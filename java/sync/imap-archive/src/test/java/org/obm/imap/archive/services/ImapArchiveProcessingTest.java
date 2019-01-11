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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.io.IOException;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Set;

import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.obm.domain.dao.SharedMailboxDao;
import org.obm.imap.archive.beans.ArchiveConfiguration;
import org.obm.imap.archive.beans.ArchiveRecurrence;
import org.obm.imap.archive.beans.ArchiveStatus;
import org.obm.imap.archive.beans.ArchiveTerminatedTreatment;
import org.obm.imap.archive.beans.ArchiveTreatment;
import org.obm.imap.archive.beans.ArchiveTreatmentRunId;
import org.obm.imap.archive.beans.ConfigurationState;
import org.obm.imap.archive.beans.DomainConfiguration;
import org.obm.imap.archive.beans.HigherBoundary;
import org.obm.imap.archive.beans.ImapFolder;
import org.obm.imap.archive.beans.Limit;
import org.obm.imap.archive.beans.ProcessedFolder;
import org.obm.imap.archive.beans.RepeatKind;
import org.obm.imap.archive.beans.SchedulingConfiguration;
import org.obm.imap.archive.beans.Year;
import org.obm.imap.archive.configuration.ImapArchiveConfigurationService;
import org.obm.imap.archive.configuration.ImapArchiveConfigurationServiceImpl;
import org.obm.imap.archive.dao.ArchiveTreatmentDao;
import org.obm.imap.archive.dao.ProcessedFolderDao;
import org.obm.imap.archive.exception.ImapArchiveProcessingException;
import org.obm.imap.archive.logging.LoggerAppenders;
import org.obm.imap.archive.mailbox.Mailbox;
import org.obm.imap.archive.mailbox.MailboxImpl;
import org.obm.imap.archive.mailbox.TemporaryMailbox;
import org.obm.push.exception.ImapTimeoutException;
import org.obm.push.exception.MailboxNotFoundException;
import org.obm.push.mail.bean.AnnotationEntry;
import org.obm.push.mail.bean.AttributeValue;
import org.obm.push.mail.bean.Flag;
import org.obm.push.mail.bean.FlagsList;
import org.obm.push.mail.bean.InternalDate;
import org.obm.push.mail.bean.ListInfo;
import org.obm.push.mail.bean.ListResult;
import org.obm.push.mail.bean.MessageSet;
import org.obm.push.mail.bean.SearchQuery;
import org.obm.push.minig.imap.StoreClient;
import org.obm.sync.base.DomainName;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import com.linagora.scheduling.DateTimeProvider;

import ch.qos.logback.classic.Logger;
import fr.aliacom.obm.common.domain.ObmDomain;
import fr.aliacom.obm.common.domain.ObmDomainUuid;
import fr.aliacom.obm.common.system.ObmSystemUser;
import pl.wkr.fluentrule.api.FluentExpectedException;

public class ImapArchiveProcessingTest {

	@Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();
	@Rule public FluentExpectedException expectedException = FluentExpectedException.none();
	
	private IMocksControl control;
	private DateTimeProvider dateTimeProvider;
	private SchedulingDatesService schedulingDatesService;
	private StoreClientFactory storeClientFactory;
	private ArchiveTreatmentDao archiveTreatmentDao;
	private SharedMailboxDao sharedMailboxDao;
	private ProcessedFolderDao processedFolderDao;
	private ImapArchiveConfigurationService imapArchiveConfigurationService;
	private Logger logger;
	private LoggerAppenders loggerAppenders;
	
	private ImapArchiveProcessing imapArchiveProcessing;
	private MailboxProcessing mailboxProcessing;

	@Before
	public void setup() throws IOException {
		control = createControl();
		dateTimeProvider = control.createMock(DateTimeProvider.class);
		schedulingDatesService = control.createMock(SchedulingDatesService.class);
		storeClientFactory = control.createMock(StoreClientFactory.class);
		archiveTreatmentDao = control.createMock(ArchiveTreatmentDao.class);
		processedFolderDao = control.createMock(ProcessedFolderDao.class);
		sharedMailboxDao = control.createMock(SharedMailboxDao.class);
		imapArchiveConfigurationService = control.createMock(ImapArchiveConfigurationService.class);
		expect(imapArchiveConfigurationService.getCyrusPartitionSuffix())
			.andReturn("archive").anyTimes();
		expect(imapArchiveConfigurationService.getProcessingBatchSize())
			.andReturn(20).anyTimes();
		expect(imapArchiveConfigurationService.getQuotaMaxSize())
			.andReturn(ImapArchiveConfigurationServiceImpl.DEFAULT_QUOTA_MAX_SIZE).anyTimes();
		logger = (Logger) LoggerFactory.getLogger(temporaryFolder.newFile().getAbsolutePath());
		loggerAppenders = control.createMock(LoggerAppenders.class);

		mailboxProcessing = new MailboxProcessing(dateTimeProvider, processedFolderDao, imapArchiveConfigurationService);
		imapArchiveProcessing = new ImapArchiveProcessing(dateTimeProvider, 
				schedulingDatesService, storeClientFactory, archiveTreatmentDao, mailboxProcessing,
				ImmutableSet.of(new UserMailboxesProcessor(storeClientFactory),
						new SharedMailboxesProcessor(storeClientFactory, sharedMailboxDao)));
	}

	@Test
	public void archiveShouldWork() throws Exception {
		String archiveMainFolder = "arChive";
		ObmDomainUuid domainId = ObmDomainUuid.of("fc2f915e-9df4-4560-b141-7b4c7ddecdd6");
		ObmDomain domain = ObmDomain.builder().uuid(domainId).name("mydomain.org").build();
		DomainConfiguration domainConfiguration = DomainConfiguration.builder()
				.domain(domain)
				.state(ConfigurationState.ENABLE)
				.schedulingConfiguration(SchedulingConfiguration.builder()
						.recurrence(ArchiveRecurrence.daily())
						.time(LocalTime.parse("13:23"))
						.build())
				.archiveMainFolder(archiveMainFolder)
				.moveEnabled(false)
				.build();
		expect(archiveTreatmentDao.findLastTerminated(domainId, Limit.from(1)))
			.andReturn(ImmutableList.<ArchiveTreatment> of());
		
		ZonedDateTime treatmentDate = ZonedDateTime.parse("2014-08-27T12:18:00.000Z");
		expect(dateTimeProvider.now())
			.andReturn(treatmentDate).times(4);
		
		ZonedDateTime higherBoundary = ZonedDateTime.parse("2014-08-26T12:18:00.000Z");
		expect(schedulingDatesService.higherBoundary(treatmentDate, RepeatKind.DAILY))
			.andReturn(higherBoundary);
		
		ListResult inboxListResult = getUserMailboxList("usera", "");
		ListResult listResult = getUserMailboxList("usera", "/Drafts", "/SPAM");
		StoreClient storeClient = control.createMock(StoreClient.class);
		
		storeClient.login(false);
		expectLastCall();
		expect(storeClient.listAll(UserMailboxesProcessor.USERS_REFERENCE_NAME, UserMailboxesProcessor.INBOX_MAILBOX_NAME))
			.andReturn(inboxListResult);
		storeClient.login(false);
		expectLastCall();
		expectListImapFolders(storeClient, "usera", listResult);
		
		ArchiveTreatmentRunId runId = ArchiveTreatmentRunId.from("ae7e9726-4d00-4259-a89e-2dbdb7b65a77");
		expectImapCommandsOnMailboxProcessing("user/usera@mydomain.org", "user/usera/" + archiveMainFolder + "/2014/INBOX@mydomain.org", "user/usera/TEMPORARY_ARCHIVE_FOLDER/INBOX@mydomain.org", 
				ImmutableSet.of(Range.closed(1l, 10l)), higherBoundary, treatmentDate, runId, false, storeClient);
		expectImapCommandsOnMailboxProcessing("user/usera/Drafts@mydomain.org", "user/usera/" + archiveMainFolder + "/2014/Drafts@mydomain.org", "user/usera/TEMPORARY_ARCHIVE_FOLDER/Drafts@mydomain.org", 
				ImmutableSet.of(Range.closed(3l, 22l), Range.closed(23l, 42l), Range.closed(43l, 62l), Range.closed(63l, 82l), Range.closed(83l, 100l)), 
				higherBoundary, treatmentDate, runId, false, storeClient);
		expectImapCommandsOnMailboxProcessing("user/usera/SPAM@mydomain.org", "user/usera/" + archiveMainFolder + "/2014/SPAM@mydomain.org", "user/usera/TEMPORARY_ARCHIVE_FOLDER/SPAM@mydomain.org", 
				ImmutableSet.of(Range.singleton(1230l)), higherBoundary, treatmentDate, runId, false, storeClient);
		
		expectSharedMailboxes(storeClient, domain, 1);

		storeClient.close();
		expectLastCall().times(2);
		
		expect(storeClientFactory.create(domain.getName()))
			.andReturn(storeClient);
		expect(storeClientFactory.createOnUserBackend("usera", domain))
			.andReturn(storeClient).times(4);
		
		control.replay();
		imapArchiveProcessing.archive(new ArchiveConfiguration(domainConfiguration, null, null, runId, logger, loggerAppenders, false));
		control.verify();
	}
	
	@Test
	public void givenAnyArchiveRunShouldDeleteEmailsWhenMoveIsEnabled() throws Exception {
		String archiveMainFolder = "arChive";
		ObmDomainUuid domainId = ObmDomainUuid.of("fc2f915e-9df4-4560-b141-7b4c7ddecdd6");
		ObmDomain domain = ObmDomain.builder().uuid(domainId).name("mydomain.org").build();
		DomainConfiguration domainConfiguration = DomainConfiguration.builder()
				.domain(domain)
				.state(ConfigurationState.ENABLE)
				.schedulingConfiguration(SchedulingConfiguration.builder()
						.recurrence(ArchiveRecurrence.daily())
						.time(LocalTime.parse("13:23"))
						.build())
				.archiveMainFolder(archiveMainFolder)
				.moveEnabled(true)
				.build();
		expect(archiveTreatmentDao.findLastTerminated(domainId, Limit.from(1)))
			.andReturn(ImmutableList.<ArchiveTreatment> of());
		
		ZonedDateTime treatmentDate = ZonedDateTime.parse("2014-08-27T12:18:00.000Z");
		expect(dateTimeProvider.now())
			.andReturn(treatmentDate).times(4);
		
		ZonedDateTime higherBoundary = ZonedDateTime.parse("2014-08-26T12:18:00.000Z");
		expect(schedulingDatesService.higherBoundary(treatmentDate, RepeatKind.DAILY))
			.andReturn(higherBoundary);
		
		ListResult inboxListResult = getUserMailboxList("usera", "");
		ListResult listResult = getUserMailboxList("usera", "/Drafts", "/SPAM");
		
		StoreClient storeClient = control.createMock(StoreClient.class);
		
		storeClient.login(false);
		expectLastCall();
		expect(storeClient.listAll(UserMailboxesProcessor.USERS_REFERENCE_NAME, UserMailboxesProcessor.INBOX_MAILBOX_NAME))
			.andReturn(inboxListResult);
		storeClient.login(false);
		expectLastCall();
		expectListImapFolders(storeClient, "usera", listResult);
		
		ArchiveTreatmentRunId runId = ArchiveTreatmentRunId.from("ae7e9726-4d00-4259-a89e-2dbdb7b65a77");
		MessageSet archived1 = expectImapCommandsOnMailboxProcessing("user/usera@mydomain.org", "user/usera/" + archiveMainFolder + "/2014/INBOX@mydomain.org", "user/usera/TEMPORARY_ARCHIVE_FOLDER/INBOX@mydomain.org", 
				ImmutableSet.of(Range.closed(1l, 10l)), higherBoundary, treatmentDate, runId, true, storeClient);
		MessageSet archived2 = expectImapCommandsOnMailboxProcessing("user/usera/Drafts@mydomain.org", "user/usera/" + archiveMainFolder + "/2014/Drafts@mydomain.org", "user/usera/TEMPORARY_ARCHIVE_FOLDER/Drafts@mydomain.org", 
				ImmutableSet.of(Range.closed(3l, 22l), Range.closed(23l, 42l), Range.closed(43l, 62l), Range.closed(63l, 82l), Range.closed(83l, 100l)), 
				higherBoundary, treatmentDate, runId, true, storeClient);
		MessageSet archived3 = expectImapCommandsOnMailboxProcessing("user/usera/SPAM@mydomain.org", "user/usera/" + archiveMainFolder + "/2014/SPAM@mydomain.org", "user/usera/TEMPORARY_ARCHIVE_FOLDER/SPAM@mydomain.org", 
				ImmutableSet.of(Range.singleton(1230l)), higherBoundary, treatmentDate, runId, true, storeClient);
		
		expectMove("user/usera@mydomain.org", storeClient, archived1);
		expectMove("user/usera/Drafts@mydomain.org", storeClient, archived2);
		expectMove("user/usera/SPAM@mydomain.org", storeClient, archived3);
		
		expectSharedMailboxes(storeClient, domain, 1);

		storeClient.close();
		expectLastCall().times(2);
		
		expect(storeClientFactory.create(domain.getName()))
			.andReturn(storeClient);
		expect(storeClientFactory.createOnUserBackend("usera", domain))
			.andReturn(storeClient).times(4);
		
		control.replay();
		imapArchiveProcessing.archive(new ArchiveConfiguration(domainConfiguration, null, null, runId, logger, loggerAppenders, false));
		control.verify();
	}

	private void expectMove(String folder, StoreClient storeClient, MessageSet messages) throws Exception {
		expect(storeClient.select(folder)).andReturn(true);
		expect(storeClient.uidStore(messages, new FlagsList(ImmutableList.of(Flag.DELETED)), true)).andReturn(true);
	}
	
	@Test
	public void uidNextShouldBeEqualsToMaxPlusOneWhenNoPreviousTreatment() throws Exception {
		String archiveMainFolder = "arChive";
		ObmDomainUuid domainId = ObmDomainUuid.of("fc2f915e-9df4-4560-b141-7b4c7ddecdd6");
		ObmDomain domain = ObmDomain.builder().uuid(domainId).name("mydomain.org").build();
		DomainConfiguration domainConfiguration = DomainConfiguration.builder()
				.domain(domain)
				.state(ConfigurationState.ENABLE)
				.schedulingConfiguration(SchedulingConfiguration.builder()
						.recurrence(ArchiveRecurrence.daily())
						.time(LocalTime.parse("13:23"))
						.build())
				.archiveMainFolder(archiveMainFolder)
				.build();
		expect(archiveTreatmentDao.findLastTerminated(domainId, Limit.from(1)))
			.andReturn(ImmutableList.<ArchiveTreatment> of());
		
		ZonedDateTime treatmentDate = ZonedDateTime.parse("2014-08-27T12:18:00.000Z");
		expect(dateTimeProvider.now())
			.andReturn(treatmentDate).times(2);
		
		ZonedDateTime higherBoundary = ZonedDateTime.parse("2014-08-26T12:18:00.000Z");
		expect(schedulingDatesService.higherBoundary(treatmentDate, RepeatKind.DAILY))
			.andReturn(higherBoundary);
		
		ListResult listResult = getUserMailboxList("usera", "");
		StoreClient storeClient = control.createMock(StoreClient.class);
		
		storeClient.login(false);
		expectLastCall();
		expect(storeClient.listAll(UserMailboxesProcessor.USERS_REFERENCE_NAME, UserMailboxesProcessor.INBOX_MAILBOX_NAME))
			.andReturn(listResult);
		storeClient.login(false);
		expectLastCall();
		expectListImapFolders(storeClient, "usera", new ListResult());
		
		ArchiveTreatmentRunId runId = ArchiveTreatmentRunId.from("ae7e9726-4d00-4259-a89e-2dbdb7b65a77");
		expectImapCommandsOnMailboxProcessing("user/usera@mydomain.org", "user/usera/" + archiveMainFolder + "/2014/INBOX@mydomain.org", "user/usera/TEMPORARY_ARCHIVE_FOLDER/INBOX@mydomain.org", 
				ImmutableSet.of(Range.closed(1l, 10l)), higherBoundary, treatmentDate, runId, false, storeClient);
		
		expectSharedMailboxes(storeClient, domain, 1);

		storeClient.close();
		expectLastCall().times(2);
		
		expect(storeClientFactory.create(domain.getName()))
			.andReturn(storeClient);
		expect(storeClientFactory.createOnUserBackend("usera", domain))
			.andReturn(storeClient).times(2);
		
		control.replay();
		imapArchiveProcessing.archive(new ArchiveConfiguration(domainConfiguration, null, null, runId, logger, loggerAppenders, false));
		control.verify();
	}
	
	@Test
	public void archiveShouldWorkWhenNoNewMails() throws Exception {
		ObmDomainUuid domainId = ObmDomainUuid.of("fc2f915e-9df4-4560-b141-7b4c7ddecdd6");
		ObmDomain domain = ObmDomain.builder().uuid(domainId).name("mydomain.org").build();
		DomainConfiguration domainConfiguration = DomainConfiguration.builder()
				.domain(domain)
				.state(ConfigurationState.ENABLE)
				.schedulingConfiguration(SchedulingConfiguration.builder()
						.recurrence(ArchiveRecurrence.daily())
						.time(LocalTime.parse("13:23"))
						.build())
				.archiveMainFolder("arChive")
				.build();
		
		ZonedDateTime previousTreatmentDate = ZonedDateTime.parse("2014-08-25T12:18:00.000Z");
		ArchiveTreatmentRunId previousRunId = ArchiveTreatmentRunId.from("800de692-f977-446b-8fee-978cf5b3d7c1");
		expect(archiveTreatmentDao.findLastTerminated(domainId, Limit.from(1)))
			.andReturn(ImmutableList.<ArchiveTreatment> of(ArchiveTerminatedTreatment.builder(domainId)
					.runId(previousRunId)
					.status(ArchiveStatus.SUCCESS)
					.recurrent(true)
					.scheduledAt(previousTreatmentDate)
					.startedAt(previousTreatmentDate)
					.terminatedAt(previousTreatmentDate)
					.higherBoundary(previousTreatmentDate)
					.build()));
		
		ImapFolder imapFolder = ImapFolder.from("user/usera@mydomain.org");
		
		ZonedDateTime treatmentDate = ZonedDateTime.parse("2014-08-27T12:18:00.000Z");
		expect(dateTimeProvider.now())
			.andReturn(treatmentDate).times(2);
		
		ZonedDateTime higherBoundary = ZonedDateTime.parse("2014-08-26T12:18:00.000Z");
		expect(schedulingDatesService.higherBoundary(treatmentDate, RepeatKind.DAILY))
			.andReturn(higherBoundary);
		
		ListResult listResult = getUserMailboxList("usera", "");
		StoreClient storeClient = control.createMock(StoreClient.class);
		
		storeClient.login(false);
		expectLastCall();
		expect(storeClient.listAll(UserMailboxesProcessor.USERS_REFERENCE_NAME, UserMailboxesProcessor.INBOX_MAILBOX_NAME))
			.andReturn(listResult);
		storeClient.login(false);
		expectLastCall();
		expectListImapFolders(storeClient, "usera", new ListResult());
		
		ArchiveTreatmentRunId runId = ArchiveTreatmentRunId.from("ae7e9726-4d00-4259-a89e-2dbdb7b65a77");
		storeClient.login(false);
		expectLastCall();
		expect(storeClient.select("user/usera@mydomain.org")).andReturn(true);
		expect(storeClient.uidSearch(SearchQuery.builder()
				.beforeExclusive(Date.from(higherBoundary.toInstant()))
				.includeDeleted(false)
				.unmatchingFlag(MailboxProcessing.IMAP_ARCHIVE_FLAG)
				.build()))
			.andReturn(MessageSet.empty());
		
		expect(dateTimeProvider.now())
			.andReturn(treatmentDate);
		processedFolderDao.insert(ProcessedFolder.builder()
				.runId(runId)
				.folder(imapFolder)
				.start(treatmentDate)
				.end(treatmentDate)
				.status(ArchiveStatus.SUCCESS)
				.build());
		expectLastCall();
		
		expectSharedMailboxes(storeClient, domain, 1);

		storeClient.close();
		expectLastCall().times(3);
		
		expect(storeClientFactory.create(domain.getName()))
			.andReturn(storeClient);
		expect(storeClientFactory.createOnUserBackend("usera", domain))
			.andReturn(storeClient).times(2);
		
		control.replay();
		imapArchiveProcessing.archive(new ArchiveConfiguration(domainConfiguration, null, null, runId, logger, loggerAppenders, false));
		control.verify();
	}
	
	@Test
	public void archiveShouldContinueWhenAnExceptionIsThrownByAFolderProcessing() throws Exception {
		String archiveMainFolder = "arChive";
		ObmDomainUuid domainId = ObmDomainUuid.of("fc2f915e-9df4-4560-b141-7b4c7ddecdd6");
		ObmDomain domain = ObmDomain.builder().uuid(domainId).name("mydomain.org").build();
		DomainConfiguration domainConfiguration = DomainConfiguration.builder()
				.domain(domain)
				.state(ConfigurationState.ENABLE)
				.schedulingConfiguration(SchedulingConfiguration.builder()
						.recurrence(ArchiveRecurrence.daily())
						.time(LocalTime.parse("13:23"))
						.build())
				.archiveMainFolder(archiveMainFolder)
				.build();
		
		expect(archiveTreatmentDao.findLastTerminated(domainId, Limit.from(1)))
			.andReturn(ImmutableList.<ArchiveTreatment> of());
		
		ZonedDateTime treatmentDate = ZonedDateTime.parse("2014-08-27T12:18:00.000Z");
		expect(dateTimeProvider.now())
			.andReturn(treatmentDate).times(3);
		
		ZonedDateTime higherBoundary = ZonedDateTime.parse("2014-08-26T12:18:00.000Z");
		expect(schedulingDatesService.higherBoundary(treatmentDate, RepeatKind.DAILY))
			.andReturn(higherBoundary);
		
		String failingMailbox = "user/usera/Drafts@mydomain.org";
		ListResult inboxListResult = getUserMailboxList("usera", "");
		ListResult listResult = getUserMailboxList("usera", "/Drafts", "/SPAM");
		StoreClient storeClient = control.createMock(StoreClient.class);
		
		storeClient.login(false);
		expectLastCall();
		expect(storeClient.listAll(UserMailboxesProcessor.USERS_REFERENCE_NAME, UserMailboxesProcessor.INBOX_MAILBOX_NAME))
			.andReturn(inboxListResult);
		storeClient.login(false);
		expectLastCall();
		expectListImapFolders(storeClient, "usera", listResult);
		
		ArchiveTreatmentRunId runId = ArchiveTreatmentRunId.from("ae7e9726-4d00-4259-a89e-2dbdb7b65a77");
		expectImapCommandsOnMailboxProcessing("user/usera@mydomain.org", "user/usera/" + archiveMainFolder + "/2014/INBOX@mydomain.org", "user/usera/TEMPORARY_ARCHIVE_FOLDER/INBOX@mydomain.org", 
				ImmutableSet.of(Range.closed(1l, 10l)), higherBoundary, treatmentDate, runId, false, storeClient);
		
		storeClient.login(false);
		expectLastCall();
		expect(storeClient.select(failingMailbox)).andReturn(false);
		expect(storeClient.setAcl(failingMailbox, ObmSystemUser.CYRUS, MailboxImpl.ALL_IMAP_RIGHTS)).andReturn(false);
		storeClient.close();
		expectLastCall();
		expect(dateTimeProvider.now())
			.andReturn(treatmentDate).times(2);
		ImapFolder failingImapFolder = ImapFolder.from(failingMailbox);
		processedFolderDao.insert(ProcessedFolder.builder()
			.runId(runId)
			.folder(failingImapFolder)
			.start(treatmentDate)
			.end(treatmentDate)
			.status(ArchiveStatus.ERROR)
			.build());
		expectLastCall();
		
		expectImapCommandsOnMailboxProcessing("user/usera/SPAM@mydomain.org", "user/usera/" + archiveMainFolder + "/2014/SPAM@mydomain.org", "user/usera/TEMPORARY_ARCHIVE_FOLDER/SPAM@mydomain.org",
				ImmutableSet.of(Range.closed(3l, 22l), Range.closed(23l, 42l), Range.closed(43l, 62l), Range.closed(63l, 82l), Range.closed(83l, 100l)),
				higherBoundary, treatmentDate, runId, false, storeClient);
		
		expectSharedMailboxes(storeClient, domain, 1);

		storeClient.close();
		expectLastCall().times(2);
		
		expect(storeClientFactory.create(domain.getName()))
			.andReturn(storeClient);
		expect(storeClientFactory.createOnUserBackend("usera", domain))
			.andReturn(storeClient).times(4);
		
		expectedException.expect()
			.hasCauseInstanceOf(ImapArchiveProcessingException.class);
		
		control.replay();
		imapArchiveProcessing.archive(new ArchiveConfiguration(domainConfiguration, null, null, runId, logger, loggerAppenders, false));
		control.verify();
	}
	
	@Test
	public void archiveShouldContinuePreviousTreatmentWhenPreviousWasInError() throws Exception {
		// First launch
		String archiveMainFolder = "arChive";
		ObmDomainUuid domainId = ObmDomainUuid.of("fc2f915e-9df4-4560-b141-7b4c7ddecdd6");
		ObmDomain domain = ObmDomain.builder().uuid(domainId).name("mydomain.org").build();
		DomainConfiguration domainConfiguration = DomainConfiguration.builder()
				.domain(domain)
				.state(ConfigurationState.ENABLE)
				.schedulingConfiguration(SchedulingConfiguration.builder()
						.recurrence(ArchiveRecurrence.daily())
						.time(LocalTime.parse("13:23"))
						.build())
				.archiveMainFolder(archiveMainFolder)
				.build();
		
		expect(archiveTreatmentDao.findLastTerminated(domainId, Limit.from(1)))
			.andReturn(ImmutableList.<ArchiveTreatment> of());
		
		ZonedDateTime treatmentDate = ZonedDateTime.parse("2014-08-27T12:18:00.000Z");
		expect(dateTimeProvider.now())
			.andReturn(treatmentDate).times(3);
		
		ZonedDateTime higherBoundary = ZonedDateTime.parse("2014-08-26T12:18:00.000Z");
		expect(schedulingDatesService.higherBoundary(treatmentDate, RepeatKind.DAILY))
			.andReturn(higherBoundary);
		
		ListResult inboxListResult = getUserMailboxList("usera", "");
		ListResult listResult = getUserMailboxList("usera", "/Drafts", "/SPAM");
		StoreClient storeClient = control.createMock(StoreClient.class);
		
		storeClient.login(false);
		expectLastCall();
		expect(storeClient.listAll(UserMailboxesProcessor.USERS_REFERENCE_NAME, UserMailboxesProcessor.INBOX_MAILBOX_NAME))
			.andReturn(inboxListResult);
		storeClient.login(false);
		expectLastCall();
		expectListImapFolders(storeClient, "usera", listResult);
		
		ArchiveTreatmentRunId runId = ArchiveTreatmentRunId.from("ae7e9726-4d00-4259-a89e-2dbdb7b65a77");
		expectImapCommandsOnMailboxProcessing("user/usera@mydomain.org", "user/usera/" + archiveMainFolder + "/2014/INBOX@mydomain.org", "user/usera/TEMPORARY_ARCHIVE_FOLDER/INBOX@mydomain.org", 
				ImmutableSet.of(Range.closed(1l, 10l)), higherBoundary, treatmentDate, runId, false, storeClient);
		
		expectImapCommandsOnMailboxProcessingFails("user/usera/Drafts@mydomain.org", "user/usera/" + archiveMainFolder + "/2014/Drafts@mydomain.org", "user/usera/TEMPORARY_ARCHIVE_FOLDER/Drafts@mydomain.org",
				ImmutableSet.of(Range.closed(2l, 21l), Range.closed(22l, 41l), Range.closed(42l, 61l)), higherBoundary, treatmentDate, runId, storeClient);
		
		expectImapCommandsOnMailboxProcessing("user/usera/SPAM@mydomain.org", "user/usera/" + archiveMainFolder + "/2014/SPAM@mydomain.org", "user/usera/TEMPORARY_ARCHIVE_FOLDER/SPAM@mydomain.org",
				ImmutableSet.of(Range.closed(3l, 22l), Range.closed(23l, 42l), Range.closed(43l, 62l), Range.closed(63l, 82l), Range.closed(83l, 100l)),
				higherBoundary, treatmentDate, runId, false, storeClient);

		expectSharedMailboxes(storeClient, domain, 1);

		storeClient.close();
		expectLastCall().times(2);
		
		expect(storeClientFactory.create(domain.getName()))
			.andReturn(storeClient);
		expect(storeClientFactory.createOnUserBackend("usera", domain))
			.andReturn(storeClient).times(4);
		
		// Continuing previous treatment
		expect(archiveTreatmentDao.findLastTerminated(domainId, Limit.from(1)))
			.andReturn(ImmutableList.<ArchiveTreatment> of(ArchiveTerminatedTreatment.builder(domainId)
					.runId(runId)
					.status(ArchiveStatus.ERROR)
					.recurrent(false)
					.scheduledAt(treatmentDate)
					.startedAt(treatmentDate)
					.terminatedAt(treatmentDate)
					.higherBoundary(higherBoundary)
					.build()));
	
		ArchiveTreatmentRunId secondRunId = ArchiveTreatmentRunId.from("70044a54-1269-49dd-8e17-991b83816c72");
		expectImapCommandsOnAlreadyProcessedMailbox("user/usera@mydomain.org", treatmentDate, higherBoundary, secondRunId, storeClient);
		
		expectImapCommandsOnMailboxProcessing("user/usera/Drafts@mydomain.org", "user/usera/" + archiveMainFolder + "/2014/Drafts@mydomain.org", "user/usera/TEMPORARY_ARCHIVE_FOLDER/Drafts@mydomain.org", 
				ImmutableSet.of(Range.closed(2l, 21l), Range.closed(22l, 41l), Range.closed(42l, 61l)), 
				higherBoundary, treatmentDate, secondRunId, false, storeClient);
		
		expectImapCommandsOnAlreadyProcessedMailbox("user/usera/SPAM@mydomain.org", treatmentDate, higherBoundary, secondRunId, storeClient);
		
		expect(storeClientFactory.create(domain.getName()))
			.andReturn(storeClient);
		expect(storeClientFactory.createOnUserBackend("usera", domain))
			.andReturn(storeClient).times(4);
		storeClient.login(false);
		expectLastCall();
		expect(storeClient.listAll(UserMailboxesProcessor.USERS_REFERENCE_NAME, UserMailboxesProcessor.INBOX_MAILBOX_NAME))
			.andReturn(inboxListResult);
		storeClient.login(false);
		expectLastCall();
		expectListImapFolders(storeClient, "usera", listResult);
		
		storeClient.close();
		expectLastCall().times(2);

		try {
			control.replay();
			imapArchiveProcessing.archive(new ArchiveConfiguration(domainConfiguration, null, null, runId, logger, loggerAppenders, false));
		} catch (Exception e) {
			imapArchiveProcessing.archive(new ArchiveConfiguration(domainConfiguration, null, null, secondRunId, logger, loggerAppenders, false));
		} catch (Throwable t) {
			System.out.println(t);
		} finally {
			control.verify();
		}
	}
	
	private void expectSharedMailboxes(StoreClient storeClient, ObmDomain domain, int expectingTimes) throws Exception {
		expect(storeClientFactory.create(domain.getName()))
			.andReturn(storeClient).times(expectingTimes);
		storeClient.login(false);
		expectLastCall().times(expectingTimes);
		expect(storeClient.listAll("", UserMailboxesProcessor.ALL_MAILBOXES_NAME))
			.andReturn(new ListResult()).times(expectingTimes);
		storeClient.close();
		expectLastCall().times(expectingTimes);
	}

	@Test
	public void archiveShouldCopyInCorrespondingYearFolder() throws Exception {
		String archiveMainFolder = "arChive";
		ObmDomainUuid domainId = ObmDomainUuid.of("fc2f915e-9df4-4560-b141-7b4c7ddecdd6");
		ObmDomain domain = ObmDomain.builder().uuid(domainId).name("mydomain.org").build();
		DomainConfiguration domainConfiguration = DomainConfiguration.builder()
				.domain(domain)
				.state(ConfigurationState.ENABLE)
				.schedulingConfiguration(SchedulingConfiguration.builder()
						.recurrence(ArchiveRecurrence.daily())
						.time(LocalTime.parse("13:23"))
						.build())
				.archiveMainFolder(archiveMainFolder)
				.build();
		expect(archiveTreatmentDao.findLastTerminated(domainId, Limit.from(1)))
			.andReturn(ImmutableList.<ArchiveTreatment> of());
		
		ZonedDateTime treatmentDate = ZonedDateTime.parse("2014-08-27T12:18:00.000Z");
		expect(dateTimeProvider.now())
			.andReturn(treatmentDate).times(2);
		
		ZonedDateTime higherBoundary = ZonedDateTime.parse("2014-08-26T12:18:00.000Z");
		expect(schedulingDatesService.higherBoundary(treatmentDate, RepeatKind.DAILY))
			.andReturn(higherBoundary);
		
		ListResult listResult = getUserMailboxList("usera", "");
		StoreClient storeClient = control.createMock(StoreClient.class);
		
		storeClient.login(false);
		expectLastCall();
		expect(storeClient.listAll(UserMailboxesProcessor.USERS_REFERENCE_NAME, UserMailboxesProcessor.INBOX_MAILBOX_NAME))
			.andReturn(listResult);
		storeClient.login(false);
		expectLastCall();
		expectListImapFolders(storeClient, "usera", new ListResult(0));
		
		ArchiveTreatmentRunId runId = ArchiveTreatmentRunId.from("ae7e9726-4d00-4259-a89e-2dbdb7b65a77");
		expectImapCommandsOnMailboxProcessingWhenTwoYearsInRange("user/usera@mydomain.org", "user/usera/" + archiveMainFolder + "/2014/INBOX@mydomain.org", "user/usera/" + archiveMainFolder + "/2015/INBOX@mydomain.org", "user/usera/TEMPORARY_ARCHIVE_FOLDER/INBOX@mydomain.org", 
				Range.closed(1l, 10l), Range.closed(11l, 15l), higherBoundary, treatmentDate, runId, storeClient);
		
		expectSharedMailboxes(storeClient, domain, 1);

		storeClient.close();
		expectLastCall().times(2);
		
		expect(storeClientFactory.create(domain.getName()))
			.andReturn(storeClient);
		expect(storeClientFactory.createOnUserBackend("usera", domain))
			.andReturn(storeClient).times(2);
		
		control.replay();
		imapArchiveProcessing.archive(new ArchiveConfiguration(domainConfiguration, null, null, runId, logger, loggerAppenders, false));
		control.verify();
	}
	
	private void expectImapCommandsOnMailboxProcessingWhenTwoYearsInRange(String mailboxName, String firstYearArchiveMailboxName, String secondYearArchiveMailboxName, String temporaryMailboxName, Range<Long> firstYearRange, Range<Long> secondYearRange,
				ZonedDateTime higherBoundary, ZonedDateTime treatmentDate, ArchiveTreatmentRunId runId, StoreClient storeClient) 
			throws Exception {
		
		MessageSet.Builder messageSetBuilder = MessageSet.builder();
		messageSetBuilder.add(firstYearRange);
		messageSetBuilder.add(secondYearRange);
		MessageSet messageSet = messageSetBuilder.build();
		
		storeClient.login(false);
		expectLastCall();
		expect(storeClient.select(mailboxName)).andReturn(true).times(2);
		expect(storeClient.uidSearch(SearchQuery.builder()
				.beforeExclusive(Date.from(higherBoundary.toInstant()))
				.includeDeleted(false)
				.unmatchingFlag(MailboxProcessing.IMAP_ARCHIVE_FLAG)
				.build()))
			.andReturn(messageSet);
		
		expectCreateMailbox(temporaryMailboxName, storeClient);
		
		expect(storeClient.select(mailboxName)).andReturn(true);
		MessageSet secondYearMessageSet = MessageSet.builder().add(secondYearRange).build();
		expect(storeClient.uidSearch(SearchQuery.builder()
				.between(true)
				.beforeExclusive(Date.from(Year.from(2014).toDate().toInstant()))
				.afterInclusive(Date.from(Year.from(2014).next().toDate().toInstant()))
				.messageSet(messageSet)
				.build()))
			.andReturn(secondYearMessageSet);
		
		// first Year
		long firstUid = messageSet.first().get();
		expect(storeClient.uidFetchInternalDate(MessageSet.singleton(firstUid)))
				.andReturn(ImmutableList.of(new InternalDate(firstUid, "3-Dec-2014 11:53:00 +0000")));
		
		expect(storeClient.select(temporaryMailboxName)).andReturn(true);
		expect(storeClient.uidCopy(messageSet, temporaryMailboxName)).andReturn(messageSet);
		
		MessageSet firstYearMessageSet = MessageSet.builder().add(firstYearRange).build();
		expectCreateMailbox(firstYearArchiveMailboxName, storeClient);
		expect(storeClient.uidCopy(firstYearMessageSet, firstYearArchiveMailboxName)).andReturn(firstYearMessageSet);
		expect(storeClient.select(firstYearArchiveMailboxName)).andReturn(true);
		expect(storeClient.uidStore(firstYearMessageSet, new FlagsList(ImmutableSet.of(Flag.SEEN)), true)).andReturn(true);
		
		expect(storeClient.select(mailboxName)).andReturn(true);
		expect(storeClient.uidStore(firstYearMessageSet, new FlagsList(ImmutableList.of(MailboxProcessing.IMAP_ARCHIVE_FLAG)), true))
			.andReturn(true);
		
		// second Year
		ImmutableList.Builder<InternalDate> internalDates = ImmutableList.builder();
		for (long uid : secondYearMessageSet.asDiscreteValues()) {
			internalDates.add(new InternalDate(uid, "3-Jan-2015 11:53:00 +0000"));
		}
		expect(storeClient.uidFetchInternalDate(secondYearMessageSet))
			.andReturn(internalDates.build());
		
		expectCreateMailbox(secondYearArchiveMailboxName, storeClient);
		expect(storeClient.select(temporaryMailboxName)).andReturn(true);
		expect(storeClient.uidCopy(secondYearMessageSet, secondYearArchiveMailboxName)).andReturn(secondYearMessageSet);
		
		expect(storeClient.select(secondYearArchiveMailboxName)).andReturn(true);
		expect(storeClient.uidStore(secondYearMessageSet, new FlagsList(ImmutableSet.of(Flag.SEEN)), true)).andReturn(true);
		
		expect(storeClient.select(mailboxName)).andReturn(true);
		expect(storeClient.uidStore(secondYearMessageSet, new FlagsList(ImmutableList.of(MailboxProcessing.IMAP_ARCHIVE_FLAG)), true))
			.andReturn(true);
		
		expect(storeClient.delete(temporaryMailboxName)).andReturn(true);
		
		storeClient.close();
		expectLastCall();
		
		expect(dateTimeProvider.now())
			.andReturn(treatmentDate);
		processedFolderDao.insert(ProcessedFolder.builder()
				.runId(runId)
				.folder(ImapFolder.from(mailboxName))
				.start(treatmentDate)
				.end(treatmentDate)
				.status(ArchiveStatus.SUCCESS)
				.build());
		expectLastCall();
	}
	
	@Test
	public void archiveShouldCopyInCorrespondingYearFolderWhenThreeYearsInABatch() throws Exception {
		String archiveMainFolder = "arChive";
		ObmDomainUuid domainId = ObmDomainUuid.of("fc2f915e-9df4-4560-b141-7b4c7ddecdd6");
		ObmDomain domain = ObmDomain.builder().uuid(domainId).name("mydomain.org").build();
		DomainConfiguration domainConfiguration = DomainConfiguration.builder()
				.domain(domain)
				.state(ConfigurationState.ENABLE)
				.schedulingConfiguration(SchedulingConfiguration.builder()
						.recurrence(ArchiveRecurrence.daily())
						.time(LocalTime.parse("13:23"))
						.build())
				.archiveMainFolder(archiveMainFolder)
				.build();
		expect(archiveTreatmentDao.findLastTerminated(domainId, Limit.from(1)))
			.andReturn(ImmutableList.<ArchiveTreatment> of());
		
		ZonedDateTime treatmentDate = ZonedDateTime.parse("2014-08-27T12:18:00.000Z");
		expect(dateTimeProvider.now())
			.andReturn(treatmentDate).times(2);
		
		ZonedDateTime higherBoundary = ZonedDateTime.parse("2014-08-26T12:18:00.000Z");
		expect(schedulingDatesService.higherBoundary(treatmentDate, RepeatKind.DAILY))
			.andReturn(higherBoundary);

		String mailboxName = "user/usera@mydomain.org";
		ListResult listResult = getUserMailboxList("usera", "");
		StoreClient storeClient = control.createMock(StoreClient.class);
		
		storeClient.login(false);
		expectLastCall();
		expect(storeClient.listAll(UserMailboxesProcessor.USERS_REFERENCE_NAME, UserMailboxesProcessor.INBOX_MAILBOX_NAME))
			.andReturn(listResult);
		storeClient.login(false);
		expectLastCall();
		expectListImapFolders(storeClient, "usera", new ListResult());
		
		Range<Long> currentYearRange = Range.closed(6l, 10l);
		Range<Long> previousYearRange = Range.closed(1l, 5l);
		Range<Long> nextYearRange = Range.closed(11l, 15l);
		MessageSet.Builder messageSetBuilder = MessageSet.builder();
		messageSetBuilder.add(currentYearRange);
		messageSetBuilder.add(previousYearRange);
		messageSetBuilder.add(nextYearRange);
		MessageSet messageSet = messageSetBuilder.build();
		
		storeClient.login(false);
		expectLastCall();
		expect(storeClient.select(mailboxName)).andReturn(true).times(2);
		expect(storeClient.uidSearch(SearchQuery.builder()
				.beforeExclusive(Date.from(higherBoundary.toInstant()))
				.includeDeleted(false)
				.unmatchingFlag(MailboxProcessing.IMAP_ARCHIVE_FLAG)
				.build()))
			.andReturn(messageSet);
		
		String temporaryMailboxName = "user/usera/TEMPORARY_ARCHIVE_FOLDER/INBOX@mydomain.org";
		expectCreateMailbox(temporaryMailboxName, storeClient);
		
		MessageSet previousYearMessageSet = MessageSet.builder().add(previousYearRange).build();
		MessageSet nextYearMessageSet = MessageSet.builder().add(nextYearRange).build();
		expect(storeClient.uidSearch(SearchQuery.builder()
				.between(true)
				.beforeExclusive(Date.from(Year.from(2014).toDate().toInstant()))
				.afterInclusive(Date.from(Year.from(2014).next().toDate().toInstant()))
				.messageSet(messageSet)
				.build()))
			.andReturn(MessageSet.builder().add(previousYearMessageSet).add(nextYearMessageSet).build());
		
		// current Year
		long firstUid = messageSet.first().get();
		expect(storeClient.uidFetchInternalDate(MessageSet.singleton(firstUid)))
				.andReturn(ImmutableList.of(new InternalDate(firstUid, "3-Dec-2014 11:53:00 +0000")));
		
		expect(storeClient.select(mailboxName)).andReturn(true);
		expect(storeClient.select(temporaryMailboxName)).andReturn(true);
		expect(storeClient.uidCopy(messageSet, temporaryMailboxName)).andReturn(messageSet);
		
		MessageSet currentYearRangeCopiedUids = MessageSet.builder().add(Range.closed(3l, 7l)).build();
		MessageSet firstYearMessageSet = MessageSet.builder().add(currentYearRange).build();
		String currentYearArchiveMailboxName = "user/usera/" + archiveMainFolder + "/2014/INBOX@mydomain.org";
		expectCreateMailbox(currentYearArchiveMailboxName, storeClient);
		expect(storeClient.uidCopy(firstYearMessageSet, currentYearArchiveMailboxName)).andReturn(currentYearRangeCopiedUids);
		expect(storeClient.select(currentYearArchiveMailboxName)).andReturn(true);
		expect(storeClient.uidStore(currentYearRangeCopiedUids, new FlagsList(ImmutableSet.of(Flag.SEEN)), true)).andReturn(true);
		expect(storeClient.select(mailboxName)).andReturn(true);
		expect(storeClient.uidStore(firstYearMessageSet, new FlagsList(ImmutableSet.of(MailboxProcessing.IMAP_ARCHIVE_FLAG)), true))
			.andReturn(true);
		
		ImmutableList.Builder<InternalDate> otherYearsInternalDates = ImmutableList.builder();
		for (long uid : previousYearMessageSet.asDiscreteValues()) {
			otherYearsInternalDates.add(new InternalDate(uid, "3-Jan-2013 11:53:00 +0000"));
		}
		for (long uid : nextYearMessageSet.asDiscreteValues()) {
			otherYearsInternalDates.add(new InternalDate(uid, "3-Jan-2015 11:53:00 +0000"));
		}
		expect(storeClient.uidFetchInternalDate(MessageSet.builder().add(previousYearMessageSet).add(nextYearMessageSet).build()))
			.andReturn(otherYearsInternalDates.build());
		
		// previous Year
		String previousYearArchiveMailboxName = "user/usera/" + archiveMainFolder + "/2013/INBOX@mydomain.org";
		expectCreateMailbox(previousYearArchiveMailboxName, storeClient);
		expect(storeClient.select(temporaryMailboxName)).andReturn(true);
		MessageSet previousYearRangeCopiedUids = MessageSet.builder().add(Range.closed(8l, 12l)).build();
		expect(storeClient.uidCopy(previousYearMessageSet, previousYearArchiveMailboxName)).andReturn(previousYearRangeCopiedUids);
		
		expect(storeClient.select(previousYearArchiveMailboxName)).andReturn(true);
		expect(storeClient.uidStore(previousYearRangeCopiedUids, new FlagsList(ImmutableSet.of(Flag.SEEN)), true)).andReturn(true);
		expect(storeClient.select(mailboxName)).andReturn(true);
		expect(storeClient.uidStore(previousYearMessageSet, new FlagsList(ImmutableSet.of(MailboxProcessing.IMAP_ARCHIVE_FLAG)), true))
			.andReturn(true);
		
		// next Year
		String nextYearArchiveMailboxName = "user/usera/" + archiveMainFolder + "/2015/INBOX@mydomain.org";
		expectCreateMailbox(nextYearArchiveMailboxName, storeClient);
		expect(storeClient.select(temporaryMailboxName)).andReturn(true);
		MessageSet nextYearRangeCopiedUids = MessageSet.builder().add(Range.closed(13l, 17l)).build();
		expect(storeClient.uidCopy(nextYearMessageSet, nextYearArchiveMailboxName)).andReturn(nextYearRangeCopiedUids);
		
		expect(storeClient.select(nextYearArchiveMailboxName)).andReturn(true);
		expect(storeClient.uidStore(nextYearRangeCopiedUids, new FlagsList(ImmutableSet.of(Flag.SEEN)), true)).andReturn(true);
		expect(storeClient.select(mailboxName)).andReturn(true);
		expect(storeClient.uidStore(nextYearMessageSet, new FlagsList(ImmutableSet.of(MailboxProcessing.IMAP_ARCHIVE_FLAG)), true))
			.andReturn(true);
		
		expect(storeClient.delete(temporaryMailboxName)).andReturn(true);
		
		expectSharedMailboxes(storeClient, domain, 1);

		storeClient.close();
		expectLastCall();
		
		expect(dateTimeProvider.now())
			.andReturn(treatmentDate);
		ArchiveTreatmentRunId runId = ArchiveTreatmentRunId.from("ae7e9726-4d00-4259-a89e-2dbdb7b65a77");
		processedFolderDao.insert(ProcessedFolder.builder()
				.runId(runId)
				.folder(ImapFolder.from(mailboxName))
				.start(treatmentDate)
				.end(treatmentDate)
				.status(ArchiveStatus.SUCCESS)
				.build());
		expectLastCall();
		
		storeClient.close();
		expectLastCall().times(2);
		
		expect(storeClientFactory.create(domain.getName()))
			.andReturn(storeClient);
		expect(storeClientFactory.createOnUserBackend("usera", domain))
			.andReturn(storeClient).times(2);
		
		control.replay();
		imapArchiveProcessing.archive(new ArchiveConfiguration(domainConfiguration, null, null, runId, logger, loggerAppenders, false));
		control.verify();
	}

	private void expectImapCommandsOnAlreadyProcessedMailbox(String mailbox, ZonedDateTime treatmentDate, ZonedDateTime higherBoundary, 
			ArchiveTreatmentRunId secondRunId, StoreClient storeClient) throws Exception {
		
		storeClient.login(false);
		expectLastCall();
		
		expect(storeClient.select(mailbox)).andReturn(true);
		expect(dateTimeProvider.now())
			.andReturn(treatmentDate).times(2);
		ImapFolder imapFolder = ImapFolder.from(mailbox);
		expect(dateTimeProvider.now())
			.andReturn(treatmentDate);
		
		expect(storeClient.uidSearch(SearchQuery.builder()
				.beforeExclusive(Date.from(higherBoundary.toInstant()))
				.includeDeleted(false)
				.unmatchingFlag(MailboxProcessing.IMAP_ARCHIVE_FLAG)
				.build()))
			.andReturn(MessageSet.empty());
		processedFolderDao.insert(ProcessedFolder.builder()
				.runId(secondRunId)
				.folder(imapFolder)
				.start(treatmentDate)
				.end(treatmentDate)
				.status(ArchiveStatus.SUCCESS)
				.build());
		expectLastCall();
		storeClient.close();
		expectLastCall();
	}
	
	private void expectImapCommandsOnMailboxProcessingFails(String mailboxName, String archiveMailboxName, String temporaryMailboxName, Set<Range<Long>> uids,
				ZonedDateTime higherBoundary, ZonedDateTime treatmentDate, ArchiveTreatmentRunId runId, StoreClient storeClient) 
			throws Exception {
		
		MessageSet.Builder messageSetBuilder = MessageSet.builder();
		for (Range<Long> range : uids) {
			messageSetBuilder.add(range);
		}
		MessageSet messageSet = messageSetBuilder.build();
		
		storeClient.login(false);
		expectLastCall();
		expect(storeClient.select(mailboxName)).andReturn(true).times(2);
		expect(storeClient.uidSearch(SearchQuery.builder()
				.beforeExclusive(Date.from(higherBoundary.toInstant()))
				.includeDeleted(false)
				.unmatchingFlag(MailboxProcessing.IMAP_ARCHIVE_FLAG)
				.build()))
			.andReturn(messageSet);
		
		expectCreateMailbox(archiveMailboxName, storeClient);
		
		expectCreateMailbox(temporaryMailboxName, storeClient);
		expect(storeClient.uidCopy(messageSet, temporaryMailboxName)).andReturn(messageSet);
		
		expectCopyPartitionFailsOnSecond(mailboxName, archiveMailboxName, temporaryMailboxName, uids, storeClient);
		
		expect(storeClient.delete(temporaryMailboxName)).andReturn(true);
		
		storeClient.close();
		expectLastCall();
		
		expect(dateTimeProvider.now())
			.andReturn(treatmentDate).times(2);
		processedFolderDao.insert(ProcessedFolder.builder()
				.runId(runId)
				.folder(ImapFolder.from(mailboxName))
				.start(treatmentDate)
				.end(treatmentDate)
				.status(ArchiveStatus.ERROR)
				.build());
		expectLastCall();
	}
	
	private MessageSet expectImapCommandsOnMailboxProcessing(String mailboxName, String archiveMailboxName, String temporaryMailboxName, Set<Range<Long>> uids,
				ZonedDateTime higherBoundary, ZonedDateTime treatmentDate, ArchiveTreatmentRunId runId, boolean isMoveEnabled, StoreClient storeClient) 
			throws Exception {
		
		MessageSet.Builder messageSetBuilder = MessageSet.builder();
		for (Range<Long> range : uids) {
			messageSetBuilder.add(range);
		}
		MessageSet messageSet = messageSetBuilder.build();
		
		storeClient.login(false);
		expectLastCall();
		expect(storeClient.select(mailboxName)).andReturn(true);
		expect(storeClient.uidSearch(SearchQuery.builder()
				.beforeExclusive(Date.from(higherBoundary.toInstant()))
				.includeDeleted(false)
				.unmatchingFlag(MailboxProcessing.IMAP_ARCHIVE_FLAG)
				.build()))
			.andReturn(messageSet);
		
		expect(storeClient.select(mailboxName)).andReturn(true);

		expectCreateMailbox(temporaryMailboxName, storeClient);
		
		expectCopyPartition(mailboxName, archiveMailboxName, temporaryMailboxName, uids, isMoveEnabled, storeClient);
		
		expect(storeClient.uidCopy(messageSet, temporaryMailboxName)).andReturn(messageSet);
		expect(storeClient.delete(temporaryMailboxName)).andReturn(true);
		
		storeClient.close();
		expectLastCall();
		
		expect(dateTimeProvider.now())
			.andReturn(treatmentDate);
		processedFolderDao.insert(ProcessedFolder.builder()
				.runId(runId)
				.folder(ImapFolder.from(mailboxName))
				.start(treatmentDate)
				.end(treatmentDate)
				.status(ArchiveStatus.SUCCESS)
				.build());
		expectLastCall();
		
		return messageSet;
	}

	private void expectCreateMailbox(String archiveMailboxName, StoreClient storeClient) throws MailboxNotFoundException {
		expect(storeClient.select(archiveMailboxName)).andReturn(false);
		expect(storeClient.create(archiveMailboxName, "mydomain_org_archive")).andReturn(true);
		expect(storeClient.setAcl(archiveMailboxName, ObmSystemUser.CYRUS, MailboxImpl.ALL_IMAP_RIGHTS)).andReturn(true);
		expect(storeClient.setAcl(archiveMailboxName, "usera@mydomain.org", MailboxImpl.READ_SEENFLAG_IMAP_RIGHTS)).andReturn(true);
		expect(storeClient.setQuota(archiveMailboxName, ImapArchiveConfigurationServiceImpl.DEFAULT_QUOTA_MAX_SIZE)).andReturn(true);
		expect(storeClient.setAnnotation(archiveMailboxName, AnnotationEntry.SHAREDSEEN, AttributeValue.sharedValue("true"))).andReturn(true);
		expect(storeClient.select(archiveMailboxName)).andReturn(true);
	}

	private void expectCopyPartitionFailsOnSecond(String mailboxName, String archiveMailboxName, String temporaryMailboxName, Set<Range<Long>> uids, StoreClient storeClient) throws MailboxNotFoundException {
		Range<Long> first = Iterables.get(uids, 0);
		expect(storeClient.uidSearch(SearchQuery.builder()
				.between(true)
				.beforeExclusive(Date.from(Year.from(2014).toDate().toInstant()))
				.afterInclusive(Date.from(Year.from(2014).next().toDate().toInstant()))
				.messageSet(MessageSet.builder().add(first).build())
				.build()))
			.andReturn(MessageSet.empty());
		
		MessageSet firstMessageSet = MessageSet.builder()
				.add(Iterables.get(uids, 0))
				.build();
		long firstRangeLowerEndpoint = first.lowerEndpoint();
		expect(storeClient.select(mailboxName)).andReturn(true);
		expect(storeClient.uidFetchInternalDate(MessageSet.singleton(firstRangeLowerEndpoint)))
				.andReturn(ImmutableList.of(new InternalDate(firstRangeLowerEndpoint, "3-Dec-2014 11:53:00 +0000")));
		
		expect(storeClient.select(temporaryMailboxName)).andReturn(true);
		expect(storeClient.uidCopy(firstMessageSet, archiveMailboxName)).andReturn(firstMessageSet);
		expect(storeClient.select(archiveMailboxName)).andReturn(true);
		expect(storeClient.uidStore(firstMessageSet, new FlagsList(ImmutableSet.of(Flag.SEEN)), true)).andReturn(true);
		expect(storeClient.select(mailboxName)).andReturn(true);
		expect(storeClient.uidStore(firstMessageSet, new FlagsList(ImmutableList.of(MailboxProcessing.IMAP_ARCHIVE_FLAG)), true))
			.andReturn(true);
		
		Range<Long> second = Iterables.get(uids, 1);
		expect(storeClient.uidSearch(SearchQuery.builder()
				.between(true)
				.beforeExclusive(Date.from(Year.from(2014).toDate().toInstant()))
				.afterInclusive(Date.from(Year.from(2014).next().toDate().toInstant()))
				.messageSet(MessageSet.builder().add(second).build())
				.build()))
			.andReturn(MessageSet.empty());
		
		long secondRangeLowerEndpoint = second.lowerEndpoint();
		expect(storeClient.select(archiveMailboxName)).andReturn(true);
		expect(storeClient.select(mailboxName)).andReturn(true);
		expect(storeClient.uidFetchInternalDate(MessageSet.singleton(secondRangeLowerEndpoint)))
				.andReturn(ImmutableList.of(new InternalDate(secondRangeLowerEndpoint, "3-Dec-2014 11:53:00 +0000")));
		
		MessageSet secondMessageSet = MessageSet.builder()
				.add(Iterables.get(uids, 1))
				.build();
		expect(storeClient.select(temporaryMailboxName)).andReturn(true);
		expect(storeClient.uidCopy(secondMessageSet, archiveMailboxName)).andThrow(new ImapTimeoutException());
	}

	private void expectCopyPartition(String mailboxName, String archiveMailboxName, String temporaryMailboxName, Set<Range<Long>> uids, boolean isMoveEnabled, StoreClient storeClient) throws MailboxNotFoundException {
		boolean first = true;
		for (Range<Long> partition : uids) {
			if (first) {
				expectCreateMailbox(archiveMailboxName, storeClient);
				first = false;
			} else {
				expect(storeClient.select(archiveMailboxName)).andReturn(true);
			}
			
			expect(storeClient.select(mailboxName)).andReturn(true);
			long firstUid = partition.lowerEndpoint();
			expect(storeClient.uidFetchInternalDate(MessageSet.singleton(firstUid)))
				.andReturn(ImmutableList.of(new InternalDate(firstUid, "3-Dec-2014 11:53:00 +0000")));
			
			expect(storeClient.uidSearch(SearchQuery.builder()
					.between(true)
					.beforeExclusive(Date.from(Year.from(2014).toDate().toInstant()))
					.afterInclusive(Date.from(Year.from(2014).next().toDate().toInstant()))
					.messageSet(MessageSet.builder().add(partition).build())
					.build()))
					.andReturn(MessageSet.empty());
			
			MessageSet messageSet = MessageSet.builder()
					.add(partition)
					.build();
			expect(storeClient.select(temporaryMailboxName)).andReturn(true);
			expect(storeClient.uidCopy(messageSet, archiveMailboxName)).andReturn(messageSet);
			expect(storeClient.select(archiveMailboxName)).andReturn(true);
			expect(storeClient.uidStore(messageSet, new FlagsList(ImmutableSet.of(Flag.SEEN)), true)).andReturn(true);
			
			if (!isMoveEnabled) {
				expect(storeClient.select(mailboxName)).andReturn(true);
				expect(storeClient.uidStore(messageSet, new FlagsList(ImmutableList.of(MailboxProcessing.IMAP_ARCHIVE_FLAG)), true))
					.andReturn(true);
			}
		}
	}
	
	@Test
	public void previousArchiveTreatmentShouldBeAbsentWhenNone() throws Exception {
		ObmDomainUuid domainId = ObmDomainUuid.of("fc2f915e-9df4-4560-b141-7b4c7ddecdd6");
		expect(archiveTreatmentDao.findLastTerminated(domainId, Limit.from(1)))
			.andReturn(ImmutableList.<ArchiveTreatment> of());
		
		control.replay();
		Optional<ArchiveTreatment> previousArchiveTreatment = imapArchiveProcessing.previousArchiveTreatment(domainId);
		control.verify();
		assertThat(previousArchiveTreatment).isAbsent();
	}
	
	@Test
	public void previousArchiveTreatmentShouldReturnPrevious() throws Exception {
		ObmDomainUuid domainId = ObmDomainUuid.of("fc2f915e-9df4-4560-b141-7b4c7ddecdd6");
		expect(archiveTreatmentDao.findLastTerminated(domainId, Limit.from(1)))
			.andReturn(ImmutableList.<ArchiveTreatment> of(ArchiveTreatment.builder(domainId)
					.runId(ArchiveTreatmentRunId.from("ae7e9726-4d00-4259-a89e-2dbdb7b65a77"))
					.recurrent(true)
					.scheduledAt(ZonedDateTime.parse("2014-08-26T08:46:00.000Z"))
					.higherBoundary(ZonedDateTime.parse("2014-09-26T08:46:00.000Z"))
					.status(ArchiveStatus.SCHEDULED)
					.build()));
		
		control.replay();
		Optional<ArchiveTreatment> previousArchiveTreatment = imapArchiveProcessing.previousArchiveTreatment(domainId);
		control.verify();
		assertThat(previousArchiveTreatment).isPresent();
	}
	
	@Test
	public void calculateHigherBoundaryWhenNoPreviousArchiveTreatment() {
		ZonedDateTime start = ZonedDateTime.parse("2014-08-26T08:46:00.000Z");
		ZonedDateTime higherBoundary = ZonedDateTime.parse("2014-07-26T08:46:00.000Z");
		expect(schedulingDatesService.higherBoundary(start, RepeatKind.MONTHLY))
			.andReturn(higherBoundary);
		
		control.replay();
		HigherBoundary boundary = imapArchiveProcessing.calculateHigherBoundary(start, RepeatKind.MONTHLY, Optional.<ArchiveTreatment> absent(), logger);
		control.verify();
		assertThat(boundary.getHigherBoundary()).isEqualTo(higherBoundary);
	}
	
	@Test
	public void calculateHigherBoundaryShouldContinueWhenPreviousArchiveTreatmentIsInError() {
		ZonedDateTime start = ZonedDateTime.parse("2014-08-26T08:46:00.000Z");
		ZonedDateTime higherBoundary = ZonedDateTime.parse("2014-07-26T08:46:00.000Z");
		
		ObmDomainUuid domainId = ObmDomainUuid.of("fc2f915e-9df4-4560-b141-7b4c7ddecdd6");
		ArchiveTreatment archiveTreatment = ArchiveTreatment.builder(domainId)
			.runId(ArchiveTreatmentRunId.from("ae7e9726-4d00-4259-a89e-2dbdb7b65a77"))
			.recurrent(true)
			.scheduledAt(start)
			.higherBoundary(higherBoundary)
			.status(ArchiveStatus.ERROR)
			.build();
		
		control.replay();
		HigherBoundary boundary = imapArchiveProcessing.calculateHigherBoundary(start, RepeatKind.MONTHLY, Optional.fromNullable(archiveTreatment), logger);
		control.verify();
		assertThat(boundary.getHigherBoundary()).isEqualTo(higherBoundary);
	}
	
	@Test
	public void calculateHigherBoundaryShouldBeNextWhenPreviousArchiveTreatmentIsSuccess() {
		ZonedDateTime start = ZonedDateTime.parse("2014-08-26T08:46:00.000Z");
		ZonedDateTime previousHigherBoundary = ZonedDateTime.parse("2014-06-26T08:46:00.000Z");
		
		ObmDomainUuid domainId = ObmDomainUuid.of("fc2f915e-9df4-4560-b141-7b4c7ddecdd6");
		ArchiveTreatment archiveTreatment = ArchiveTreatment.builder(domainId)
			.runId(ArchiveTreatmentRunId.from("ae7e9726-4d00-4259-a89e-2dbdb7b65a77"))
			.recurrent(true)
			.scheduledAt(start)
			.higherBoundary(previousHigherBoundary)
			.status(ArchiveStatus.SUCCESS)
			.build();
		
		ZonedDateTime higherBoundary = ZonedDateTime.parse("2014-07-26T08:46:00.000Z");
		expect(schedulingDatesService.higherBoundary(start, RepeatKind.MONTHLY))
			.andReturn(higherBoundary);
		
		control.replay();
		HigherBoundary boundary = imapArchiveProcessing.calculateHigherBoundary(start, RepeatKind.MONTHLY, Optional.fromNullable(archiveTreatment), logger);
		control.verify();
		assertThat(boundary.getHigherBoundary()).isEqualTo(higherBoundary);
	}
	
	@Test
	public void continuePreviousShouldBeFalseWhenPreviousArchiveTreatmentIsAbsent() {
		
		control.replay();
		boolean continuePrevious = imapArchiveProcessing.continuePrevious(Optional.<ArchiveTreatment> absent(), ZonedDateTime.now());
		control.verify();
		assertThat(continuePrevious).isFalse();
	}
	
	@Test
	public void continuePreviousShouldBeFalseWhenPreviousArchiveTreatmentIsPresentAndHigherBoundaryDoesntMatch() {
		ObmDomainUuid domainId = ObmDomainUuid.of("b1a32567-05de-4d06-b699-ad94a7c59744");
		ArchiveTreatmentRunId runId = ArchiveTreatmentRunId.from("011ce5f5-b56a-44b3-a2e6-e19942684d45");
		
		ZonedDateTime previousHigherBoundary = ZonedDateTime.parse("2014-07-23T23:59:59.999Z");
		Optional<ArchiveTreatment> previousArchiveTreatment = Optional.<ArchiveTreatment> of(ArchiveTreatment.builder(domainId)
			.runId(runId)
			.recurrent(true)
			.status(ArchiveStatus.ERROR)
			.scheduledAt(ZonedDateTime.now())
			.higherBoundary(previousHigherBoundary)
			.build());
		
		control.replay();
		boolean continuePrevious = imapArchiveProcessing.continuePrevious(previousArchiveTreatment, ZonedDateTime.now());
		control.verify();
		assertThat(continuePrevious).isFalse();
	}
	
	@Test
	public void continuePreviousShouldBeTrueWhenPreviousArchiveTreatmentIsPresentAndHigherBoundaryMatch() {
		ObmDomainUuid domainId = ObmDomainUuid.of("b1a32567-05de-4d06-b699-ad94a7c59744");
		ArchiveTreatmentRunId runId = ArchiveTreatmentRunId.from("011ce5f5-b56a-44b3-a2e6-e19942684d45");
		
		ZonedDateTime previousHigherBoundary = ZonedDateTime.parse("2014-07-23T23:59:59.999Z");
		Optional<ArchiveTreatment> previousArchiveTreatment = Optional.<ArchiveTreatment> of(ArchiveTreatment.builder(domainId)
			.runId(runId)
			.recurrent(true)
			.status(ArchiveStatus.ERROR)
			.scheduledAt(ZonedDateTime.now())
			.higherBoundary(previousHigherBoundary)
			.build());
		
		control.replay();
		boolean continuePrevious = imapArchiveProcessing.continuePrevious(previousArchiveTreatment, previousHigherBoundary);
		control.verify();
		assertThat(continuePrevious).isTrue();
	}
	
	@Test
	public void continuePreviousShouldBeFalseWhenPreviousArchiveTreatmentIsSuccess() {
		ObmDomainUuid domainId = ObmDomainUuid.of("b1a32567-05de-4d06-b699-ad94a7c59744");
		ArchiveTreatmentRunId runId = ArchiveTreatmentRunId.from("011ce5f5-b56a-44b3-a2e6-e19942684d45");
		
		ZonedDateTime previousHigherBoundary = ZonedDateTime.parse("2014-07-23T23:59:59.999Z");
		Optional<ArchiveTreatment> previousArchiveTreatment = Optional.<ArchiveTreatment> of(ArchiveTreatment.builder(domainId)
			.runId(runId)
			.recurrent(true)
			.status(ArchiveStatus.SUCCESS)
			.scheduledAt(ZonedDateTime.now())
			.higherBoundary(previousHigherBoundary)
			.build());
		
		control.replay();
		boolean continuePrevious = imapArchiveProcessing.continuePrevious(previousArchiveTreatment, previousHigherBoundary);
		control.verify();
		assertThat(continuePrevious).isFalse();
	}
	
	@Test(expected=IllegalStateException.class)
	public void processingImapCopyShouldThrowOriginException() throws Exception {
		StoreClient storeClient = control.createMock(StoreClient.class);
		MessageSet messageSet = MessageSet.builder().add(Range.closed(1l, 100l)).build();
		ObmDomain domain = ObmDomain.builder().name("mydomain.org").build();
		Mailbox mailbox = MailboxImpl.from("user/usera@mydomain.org", logger, storeClient, false);
		TemporaryMailbox temporaryMailbox = TemporaryMailbox.builder()
				.from(mailbox)
				.domainName(new DomainName(domain.getName()))
				.cyrusPartitionSuffix("archive")
				.build();
		
		String mailboxName = mailbox.getName();
		expect(storeClient.select(mailboxName))
			.andReturn(true);
		// Throws IllegalStateException
		expect(storeClient.uidCopy(messageSet, temporaryMailbox.getName()))
			.andReturn(MessageSet.empty());
		expect(storeClient.select(temporaryMailbox.getName()))
			.andReturn(true);
		// Returning false throws ImapDeleteException in finally 
		expect(storeClient.delete(temporaryMailbox.getName()))
			.andReturn(false);
		
		DomainConfiguration domainConfiguration = DomainConfiguration.builder()
				.domain(domain)
				.state(ConfigurationState.ENABLE)
				.schedulingConfiguration(SchedulingConfiguration.builder()
					.recurrence(ArchiveRecurrence.daily())
					.time(LocalTime.parse("13:23"))
					.build())
				.archiveMainFolder("arChive")
				.build();
		
		ArchiveConfiguration archiveConfiguration = new ArchiveConfiguration(
				domainConfiguration, null, null, ArchiveTreatmentRunId.from("259ef5d1-9dfd-4fdb-84b0-09d33deba1b7"), logger, null, false);
		
		ProcessedTask processedTask = ProcessedTask.builder()
				.archiveConfiguration(archiveConfiguration)
				.higherBoundary(HigherBoundary.builder()
						.higherBoundary(ZonedDateTime.parse("2014-07-26T08:46:00.000Z"))
						.build())
				.previousArchiveTreatment(Optional.<ArchiveTreatment> absent())
				.build();

		try {
			control.replay();
			mailboxProcessing.processingImapCopy(mailbox, messageSet, processedTask);
		} finally {
			control.verify();
		}
	}

	private void expectListImapFolders(StoreClient storeClient, String user, ListResult subFolderslistResult) {
		expect(storeClient.listAll(UserMailboxesProcessor.USERS_REFERENCE_NAME + "/" + user + "/", UserMailboxesProcessor.ALL_MAILBOXES_NAME))
			.andReturn(subFolderslistResult);
	}

	private ListResult getUserMailboxList(String user, String... mailboxes) {
		ListResult listResult = new ListResult();

		for (String mailbox : mailboxes) {
			listResult.add(new ListInfo("user/" + user + mailbox + "@mydomain.org", true, false));
		}

		return listResult;
	}
}
