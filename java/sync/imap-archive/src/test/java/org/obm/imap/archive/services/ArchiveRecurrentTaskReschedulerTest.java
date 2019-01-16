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

package org.obm.imap.archive.services;

import static org.easymock.EasyMock.expect;

import java.time.ZonedDateTime;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import org.obm.imap.archive.beans.ArchiveConfiguration;
import org.obm.imap.archive.beans.ArchiveTreatmentRunId;
import org.obm.imap.archive.scheduling.ArchiveDomainTask;
import org.obm.imap.archive.scheduling.ArchiveSchedulerBus.Events.RealRunTaskStatusChanged;
import org.obm.imap.archive.scheduling.ArchiveSchedulingService;
import org.obm.provisioning.dao.exceptions.DaoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linagora.scheduling.ScheduledTask.State;
import com.linagora.scheduling.Scheduler;

import fr.aliacom.obm.common.domain.ObmDomain;
import fr.aliacom.obm.common.domain.ObmDomainUuid;

public class ArchiveRecurrentTaskReschedulerTest {

	Logger logger;
	ArchiveTreatmentRunId runId;
	ObmDomain domain;
	ZonedDateTime scheduledTime;
	
	IMocksControl mocks;
	ArchiveSchedulingService schedulingService;
	Scheduler<ArchiveDomainTask> scheduler;
	ArchiveDomainTask task;
	ArchiveRecurrentTaskRescheduler testee;
	ArchiveConfiguration archiveConfiguration;

	@Before
	public void setUp() {
		logger = LoggerFactory.getLogger(ArchiveRecurrentTaskRescheduler.class);
		runId = ArchiveTreatmentRunId.from("38efaa5c-6d46-419c-97e6-6e6c6d9cbed3");
		domain = ObmDomain.builder().uuid(ObmDomainUuid.of("f7d9e710-1863-48dc-af78-bdd59cf6d82f")).build();
		scheduledTime = ZonedDateTime.parse("2024-11-01T01:04Z");
		
		mocks = EasyMock.createControl();
		schedulingService = mocks.createMock(ArchiveSchedulingService.class);
		scheduler = mocks.createMock(Scheduler.class);
		task = mocks.createMock(ArchiveDomainTask.class);
		archiveConfiguration = mocks.createMock(ArchiveConfiguration.class);
		expect(task.getArchiveConfiguration()).andReturn(archiveConfiguration).anyTimes();
		testee = new ArchiveRecurrentTaskRescheduler(logger, schedulingService);
	}

	@Test
	public void onChangeShouldDoNothingWhenNew() {
		mocks.replay();
		testee.onTreatmentStateChange(new RealRunTaskStatusChanged(State.NEW, task));
		mocks.verify();
	}
	
	@Test
	public void onChangeShouldDoNothingWhenWaiting() {
		mocks.replay();
		testee.onTreatmentStateChange(new RealRunTaskStatusChanged(State.WAITING, task));
		mocks.verify();
	}
	
	@Test
	public void onChangeShouldDoNothingWhenRunning() {
		mocks.replay();
		testee.onTreatmentStateChange(new RealRunTaskStatusChanged(State.RUNNING, task));
		mocks.verify();
	}
	
	@Test
	public void onChangeShouldDoNothingWhenCancel() {
		mocks.replay();
		testee.onTreatmentStateChange(new RealRunTaskStatusChanged(State.CANCELED, task));
		mocks.verify();
	}
	
	@Test
	public void onChangeShouldDoNothingWhenFailedAndNotRecurrent() {
		expect(archiveConfiguration.isRecurrent()).andReturn(false);
		
		mocks.replay();
		testee.onTreatmentStateChange(new RealRunTaskStatusChanged(State.FAILED, task));
		mocks.verify();
	}
	
	@Test
	public void onChangeShouldDoNothingWhenTerminatedAndNotRecurrent() {
		expect(archiveConfiguration.isRecurrent()).andReturn(false);
		
		mocks.replay();
		testee.onTreatmentStateChange(new RealRunTaskStatusChanged(State.TERMINATED, task));
		mocks.verify();
	}
	
	@Test
	public void onChangeShouldRescheduleWhenFailedAndRecurrent() throws Exception {
		expect(archiveConfiguration.isRecurrent()).andReturn(true);
		expect(archiveConfiguration.getDomain()).andReturn(domain);
		expect(schedulingService.scheduleAsRecurrent(domain)).andReturn(runId);
		
		mocks.replay();
		testee.onTreatmentStateChange(new RealRunTaskStatusChanged(State.FAILED, task));
		mocks.verify();
	}
	
	@Test
	public void onChangeShouldRescheduleWhenTerminatedAndRecurrent() throws Exception {
		expect(archiveConfiguration.isRecurrent()).andReturn(true);
		expect(archiveConfiguration.getDomain()).andReturn(domain);
		expect(schedulingService.scheduleAsRecurrent(domain)).andReturn(runId);
		
		mocks.replay();
		testee.onTreatmentStateChange(new RealRunTaskStatusChanged(State.TERMINATED, task));
		mocks.verify();
	}
	
	@Test
	public void onChangeShouldNotPropagateExceptionWhenDaoException() throws Exception {
		expect(archiveConfiguration.isRecurrent()).andReturn(true);
		expect(archiveConfiguration.getDomain()).andReturn(domain);
		expect(schedulingService.scheduleAsRecurrent(domain)).andThrow(new DaoException("error"));
		
		mocks.replay();
		testee.onTreatmentStateChange(new RealRunTaskStatusChanged(State.TERMINATED, task));
		mocks.verify();
	}
}
