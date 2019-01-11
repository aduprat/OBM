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

package org.obm.imap.archive.scheduling;

import static org.easymock.EasyMock.expect;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.obm.imap.archive.beans.ArchiveRecurrence;
import org.obm.imap.archive.beans.ArchiveTreatmentKind;
import org.obm.imap.archive.beans.ArchiveTreatmentRunId;
import org.obm.imap.archive.beans.ConfigurationState;
import org.obm.imap.archive.beans.DomainConfiguration;
import org.obm.imap.archive.beans.SchedulingConfiguration;
import org.obm.imap.archive.dao.DomainConfigurationDao;
import org.obm.imap.archive.exception.DomainConfigurationDisableException;
import org.obm.imap.archive.exception.DomainConfigurationNotFoundException;
import org.obm.imap.archive.services.SchedulingDatesService;
import org.obm.provisioning.dao.exceptions.DaoException;
import org.obm.push.utils.UUIDFactory;

import com.linagora.scheduling.ScheduledTask;

import fr.aliacom.obm.common.domain.ObmDomain;
import fr.aliacom.obm.common.domain.ObmDomainUuid;
import pl.wkr.fluentrule.api.FluentExpectedException;

public class ArchiveSchedulingServiceTest {

	@Rule public FluentExpectedException expectedException= FluentExpectedException.none();

	ObmDomain domain;
	IMocksControl mocks;
	ArchiveScheduler scheduler;
	ArchiveDomainTaskFactory taskFactory;
	UUIDFactory uuidFactory;
	SchedulingDatesService schedulingDatesService;
	DomainConfigurationDao domainConfigDao;
	ArchiveSchedulingService testee;

	@Before
	public void setUp() {
		domain = ObmDomain.builder().uuid(ObmDomainUuid.of("f1dabddf-7da2-412b-8159-71f3428e902f")).build();
		
		mocks = EasyMock.createControl();
		scheduler = mocks.createMock(ArchiveScheduler.class);
		taskFactory = mocks.createMock(ArchiveDomainTaskFactory.class);
		uuidFactory = mocks.createMock(UUIDFactory.class);
		schedulingDatesService = mocks.createMock(SchedulingDatesService.class);
		domainConfigDao = mocks.createMock(DomainConfigurationDao.class);
		
		testee = new ArchiveSchedulingService(scheduler, taskFactory, uuidFactory, schedulingDatesService, domainConfigDao);
	}

	@Test
	public void scheduleShouldFindHigherBoundaryThenSchedule() throws Exception {
		testSchedule(
			DomainConfiguration.builder()
				.domain(domain)
				.state(ConfigurationState.ENABLE)
				.schedulingConfiguration(
					SchedulingConfiguration.builder()
						.time(LocalTime.parse("22:15"))
						.recurrence(ArchiveRecurrence.daily()).build())
				.archiveMainFolder("arChive")
				.build());
	}

	@Test
	public void scheduleShouldRaiseExceptionWhenConfigurationDisable() throws Exception {
		expectedException.expect(DomainConfigurationDisableException.class);
		
		testSchedule(
			DomainConfiguration.builder()
				.domain(domain)
				.state(ConfigurationState.DISABLE)
				.schedulingConfiguration(
					SchedulingConfiguration.builder()
						.time(LocalTime.parse("22:15"))
						.recurrence(ArchiveRecurrence.daily()).build())
				.archiveMainFolder("arChive")
				.build());
	}

	@SuppressWarnings("unchecked")
	private void testSchedule(DomainConfiguration config) throws DaoException {
		ZonedDateTime when = ZonedDateTime.parse("2024-01-01T05:04Z");
		ZonedDateTime higherBoundary = ZonedDateTime.parse("2024-02-01T05:04Z");
		UUID runUuid = UUID.fromString("ecd08c0d-70aa-4a04-8a18-57fe7afe1404");
		ArchiveTreatmentRunId runId = ArchiveTreatmentRunId.from(runUuid);
		ArchiveDomainTask task = mocks.createMock(ArchiveDomainTask.class);
		ScheduledTask<ArchiveDomainTask> scheduled = mocks.createMock(ScheduledTask.class);

		expect(domainConfigDao.get(domain)).andReturn(config);
		expect(schedulingDatesService.higherBoundary(when, config.getRepeatKind())).andReturn(higherBoundary);
		expect(uuidFactory.randomUUID()).andReturn(runUuid);
		expect(taskFactory.create(config, when, higherBoundary, runId, ArchiveTreatmentKind.REAL_RUN)).andReturn(task);
		expect(scheduler.schedule(task)).andReturn(scheduled);
		
		mocks.replay();
		testee.schedule(domain, when, ArchiveTreatmentKind.REAL_RUN);
		mocks.verify();
	}

	@Test
	public void scheduleShouldRaiseExceptionWhenConfigNotFound() throws Exception {
		ZonedDateTime when = ZonedDateTime.parse("2024-01-01T05:04Z");
		expect(domainConfigDao.get(domain)).andReturn(null);
		
		expectedException.expect(DomainConfigurationNotFoundException.class);
		
		mocks.replay();
		try {
			testee.schedule(domain, when, ArchiveTreatmentKind.REAL_RUN);
		} finally {
			mocks.verify();
		}
	}

	@Test
	public void scheduleByDomainUuidShouldGetDatesThenSchedule() throws Exception {
		DomainConfiguration config = DomainConfiguration.builder()
			.domain(domain)
			.state(ConfigurationState.ENABLE)
			.schedulingConfiguration(
				SchedulingConfiguration.builder()
					.time(LocalTime.parse("22:15"))
					.recurrence(ArchiveRecurrence.daily()).build())
			.archiveMainFolder("arChive")
			.build();
		
		expect(domainConfigDao.get(domain)).andReturn(config);
		expectScheduleByConfig(config);

		mocks.replay();
		testee.scheduleAsRecurrent(domain);
		mocks.verify();
	}

	@Test
	public void scheduleByConfigShouldGetDatesThenSchedule() {
		DomainConfiguration config = DomainConfiguration.builder()
			.domain(domain)
			.state(ConfigurationState.ENABLE)
			.schedulingConfiguration(
				SchedulingConfiguration.builder()
					.time(LocalTime.parse("22:15"))
					.recurrence(ArchiveRecurrence.daily()).build())
			.archiveMainFolder("arChive")
			.build();

		expectScheduleByConfig(config);
		
		mocks.replay();
		testee.scheduleAsRecurrent(config);
		mocks.verify();
	}

	@Test
	public void scheduleByConfigShouldNotCheckEnabledStatus() {
		DomainConfiguration config = DomainConfiguration.builder()
			.domain(domain)
			.state(ConfigurationState.DISABLE)
			.schedulingConfiguration(
				SchedulingConfiguration.builder()
					.time(LocalTime.parse("22:15"))
					.recurrence(ArchiveRecurrence.daily()).build())
			.archiveMainFolder("arChive")
			.build();
		
		expectScheduleByConfig(config);

		mocks.replay();
		testee.scheduleAsRecurrent(config);
		mocks.verify();
	}

	@SuppressWarnings("unchecked")
	private void expectScheduleByConfig(DomainConfiguration config) {
		ZonedDateTime when = ZonedDateTime.parse("2024-01-01T05:04Z");
		ZonedDateTime higherBoundary = ZonedDateTime.parse("2024-02-01T05:04Z");
		UUID runUuid = UUID.fromString("ecd08c0d-70aa-4a04-8a18-57fe7afe1404");
		ArchiveTreatmentRunId runId = ArchiveTreatmentRunId.from(runUuid);
		ArchiveDomainTask task = mocks.createMock(ArchiveDomainTask.class);
		ScheduledTask<ArchiveDomainTask> scheduled = mocks.createMock(ScheduledTask.class);
		
		expect(schedulingDatesService.nextTreatmentDate(config.getSchedulingConfiguration())).andReturn(when);
		expect(schedulingDatesService.higherBoundary(when, config.getRepeatKind())).andReturn(higherBoundary);
		expect(uuidFactory.randomUUID()).andReturn(runUuid);
		expect(taskFactory.createAsRecurrent(config, when, higherBoundary, runId)).andReturn(task);
		expect(scheduler.schedule(task)).andReturn(scheduled);
	}
}
