/* ***** BEGIN LICENSE BLOCK *****
 * Copyright (C) 2014  Linagora
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version, provided you comply with the Additional Terms applicable for OBM
 * software by Linagora pursuant to Section 7 of the GNU Affero General Public
 * License, subsections (b), (c), and (e), pursuant to which you must notably (i)
 * retain the displaying by the interactive user interfaces of the “OBM, Free
 * Communication by Linagora” Logo with the “You are using the Open Source and
 * free version of OBM developed and supported by Linagora. Contribute to OBM R&D
 * by subscribing to an Enterprise offer !” infobox, (ii) retain all hypertext
 * links between OBM and obm.org, between Linagora and linagora.com, as well as
 * between the expression “Enterprise offer” and pro.obm.org, and (iii) refrain
 * from infringing Linagora intellectual property rights over its trademarks and
 * commercial brands. Other Additional Terms apply, see
 * <http://www.linagora.com/licenses/> for more details.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License and
 * its applicable Additional Terms for OBM along with this program. If not, see
 * <http://www.gnu.org/licenses/> for the GNU Affero General   Public License
 * version 3 and <http://www.linagora.com/licenses/> for the Additional Terms
 * applicable to the OBM software.
 * ***** END LICENSE BLOCK ***** */

package org.obm.imap.archive.startup;

import static com.jayway.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.obm.imap.archive.DBData.admin;
import static org.obm.imap.archive.DBData.domain;
import static org.obm.imap.archive.DBData.domainId;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;

import javax.ws.rs.core.Response.Status;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.obm.dao.utils.H2Destination;
import org.obm.dao.utils.H2InMemoryDatabase;
import org.obm.dao.utils.H2InMemoryDatabaseTestRule;
import org.obm.guice.GuiceRule;
import org.obm.imap.archive.Expectations;
import org.obm.imap.archive.TestImapArchiveModules;
import org.obm.imap.archive.TestImapArchiveModules.WithTestingMonitor.TestingOnlyOnePerDomainMonitorFactory;
import org.obm.imap.archive.beans.ArchiveRecurrence;
import org.obm.imap.archive.beans.ArchiveRunningTreatment;
import org.obm.imap.archive.beans.ArchiveScheduledTreatment;
import org.obm.imap.archive.beans.ArchiveStatus;
import org.obm.imap.archive.beans.ArchiveTerminatedTreatment;
import org.obm.imap.archive.beans.ArchiveTreatment;
import org.obm.imap.archive.beans.ArchiveTreatmentRunId;
import org.obm.imap.archive.beans.ConfigurationState;
import org.obm.imap.archive.beans.DayOfMonth;
import org.obm.imap.archive.beans.DayOfWeek;
import org.obm.imap.archive.beans.DayOfYear;
import org.obm.imap.archive.beans.DomainConfiguration;
import org.obm.imap.archive.beans.Limit;
import org.obm.imap.archive.beans.RepeatKind;
import org.obm.imap.archive.beans.SchedulingConfiguration;
import org.obm.imap.archive.dao.ArchiveTreatmentDao;
import org.obm.imap.archive.dao.DomainConfigurationDao;
import org.obm.imap.archive.scheduling.ArchiveDomainTask;
import org.obm.imap.archive.scheduling.ArchiveDomainTaskFactory;
import org.obm.server.WebServer;

import com.github.restdriver.clientdriver.ClientDriverRule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.linagora.scheduling.ScheduledTask;
import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.Operations;
import com.ninja_squad.dbsetup.operation.Operation;

import fr.aliacom.obm.common.domain.ObmDomain;
import fr.aliacom.obm.common.domain.ObmDomainUuid;

public class RestoreOnStartUpTest {

	private ClientDriverRule driver = new ClientDriverRule();

	@Rule public TestRule chain = RuleChain
			.outerRule(driver)
			.around(new TemporaryFolder())
			.around(new GuiceRule(this, new TestImapArchiveModules.WithTestingMonitor(driver, new Provider<TemporaryFolder>() {

				@Override
				public TemporaryFolder get() {
					return temporaryFolder;
				}
				
			})))
			.around(new H2InMemoryDatabaseTestRule(new Provider<H2InMemoryDatabase>() {
				@Override
				public H2InMemoryDatabase get() {
					return db;
				}
			}, "sql/initial.sql"));
	
	@Inject TemporaryFolder temporaryFolder;
	@Inject H2InMemoryDatabase db;
	@Inject WebServer server;
	@Inject ArchiveDomainTaskFactory taskFactory;
	@Inject TestingOnlyOnePerDomainMonitorFactory monitor;
	@Inject ArchiveTreatmentDao archiveTreatmentDao;
	@Inject DomainConfigurationDao domainConfigurationDao;

	Expectations expectations;
	
	@Before
	public void setUp() {
		expectations = new Expectations(driver)
			.expectTrustedLogin(domain);

		Operation operation = Operations.deleteAllFrom("mail_archive_run");
		new DbSetup(H2Destination.from(db), operation).launch();
	}
	
	@After
	public void tearDown() throws Exception {
		server.stop();
	}

	@Test
	public void testTasksAreWellRestoredOrMovedAsFailed() throws Exception {
		ArchiveTreatmentRunId expectedScheduledRunId = ArchiveTreatmentRunId.from("45896372-cc9f-4ee9-9efd-8df63e2da8c3");
		ObmDomain expectedScheduledDomain = ObmDomain.builder()
				.id(1)
				.uuid(domainId)
				.name("mydomain.org")
				.label("mydomain.org")
				.build();
		ZonedDateTime expectedScheduledHigherBoundary = ZonedDateTime.parse("2026-11-02T01:04Z");
		ZonedDateTime expectedScheduledTime = ZonedDateTime.parse("2026-11-02T03:04Z");
		DomainConfiguration expectedScheduledDomainConfiguration = DomainConfiguration.builder()
				.domain(expectedScheduledDomain)
				.state(ConfigurationState.ENABLE)
				.schedulingConfiguration(SchedulingConfiguration.builder()
						.recurrence(ArchiveRecurrence.builder()
								.repeat(RepeatKind.DAILY)
								.dayOfWeek(DayOfWeek.MONDAY)
								.dayOfMonth(DayOfMonth.last())
								.dayOfYear(DayOfYear.of(1))
								.build())
						.time(LocalTime.parse("03:04"))
						.build())
				.archiveMainFolder("arChive")
				.build();
		
		ArchiveTreatmentRunId expectedFailedRunId = ArchiveTreatmentRunId.from("c6eb4f70-2304-4bb4-aa38-441935dc6a47");
		ObmDomain expectedFailedDomain = ObmDomain.builder()
				.uuid(ObmDomainUuid.of("b9de411c-5375-4100-aedf-8e4d827c0a2c"))
				.name("mydomain2.org")
				.label("mydomain2.org")
				.build();
		ZonedDateTime expectedFailedHigherBoundary = ZonedDateTime.parse("2026-10-02T01:04Z");
		ZonedDateTime expectedFailedScheduleTime = ZonedDateTime.parse("2026-11-02T03:04Z");
		ZonedDateTime expectedFailedStartTime = ZonedDateTime.parse("2026-10-01T01:01Z");
		DomainConfiguration expectedFailedDomainConfiguration = DomainConfiguration.builder()
				.domain(expectedFailedDomain)
				.state(ConfigurationState.ENABLE)
				.schedulingConfiguration(SchedulingConfiguration.builder()
						.recurrence(ArchiveRecurrence.builder()
								.repeat(RepeatKind.DAILY)
								.dayOfWeek(DayOfWeek.MONDAY)
								.dayOfMonth(DayOfMonth.last())
								.dayOfYear(DayOfYear.of(1))
								.build())
						.time(LocalTime.parse("03:04"))
						.build())
				.archiveMainFolder("ARcHIVE")
				.build();
		

		domainConfigurationDao.create(expectedScheduledDomainConfiguration);
		domainConfigurationDao.create(expectedFailedDomainConfiguration);
		
		archiveTreatmentDao.insert(ArchiveScheduledTreatment
				.forDomain(expectedScheduledDomain.getUuid())
				.runId(expectedScheduledRunId)
				.recurrent(true)
				.higherBoundary(expectedScheduledHigherBoundary)
				.scheduledAt(expectedScheduledTime)
				.build());
		
		archiveTreatmentDao.insert(ArchiveRunningTreatment
				.forDomain(expectedFailedDomain.getUuid())
				.runId(expectedFailedRunId)
				.recurrent(true)
				.higherBoundary(expectedFailedHigherBoundary)
				.scheduledAt(expectedFailedScheduleTime)
				.startedAt(expectedFailedStartTime)
				.build());
		
		server.start();
		
		given()
			.port(server.getHttpPort())
			.auth().basic(admin.getLogin() + "@" + domain.getName(), admin.getPassword().getStringValue()).
		expect()
			.statusCode(Status.OK.getStatusCode()).
		when()
			.get("/imap-archive/service/v1/status");
		
		List<ScheduledTask<ArchiveDomainTask>> tasks = monitor.get().all();
		assertThat(tasks).hasSize(1);
		assertThat(tasks.get(0).task()).isEqualTo(taskFactory.createAsRecurrent(
			expectedScheduledDomainConfiguration,
			expectedScheduledTime,
			expectedScheduledHigherBoundary,
			expectedScheduledRunId));
		
		List<ArchiveTreatment> failedTreatments = archiveTreatmentDao.findByScheduledTime(expectedFailedDomain.getUuid(), Limit.from(5));
		assertThat(failedTreatments).containsExactly(ArchiveTerminatedTreatment
			.forDomain(expectedFailedDomain.getUuid())
			.runId(expectedFailedRunId)
			.recurrent(true)
			.scheduledAt(expectedFailedScheduleTime)
			.startedAt(expectedFailedStartTime)
			.higherBoundary(expectedFailedHigherBoundary)
			.terminatedAt(ArchiveTreatment.FAILED_AT_UNKOWN_DATE)
			.status(ArchiveStatus.ERROR)
			.build());
	}
}
