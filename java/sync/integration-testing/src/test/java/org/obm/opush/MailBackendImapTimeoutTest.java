/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2011-2012  Linagora
 *
 * This program is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU Affero General Public License as 
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version, provided you comply 
 * with the Additional Terms applicable for OBM connector by Linagora 
 * pursuant to Section 7 of the GNU Affero General Public License, 
 * subsections (b), (c), and (e), pursuant to which you must notably (i) retain 
 * the “Message sent thanks to OBM, Free Communication by Linagora” 
 * signature notice appended to any and all outbound messages 
 * (notably e-mail and meeting requests), (ii) retain all hypertext links between 
 * OBM and obm.org, as well as between Linagora and linagora.com, and (iii) refrain 
 * from infringing Linagora intellectual property rights over its trademarks 
 * and commercial brands. Other Additional Terms apply, 
 * see <http://www.linagora.com/licenses/> for more details. 
 *
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details. 
 *
 * You should have received a copy of the GNU Affero General Public License 
 * and its applicable Additional Terms for OBM along with this program. If not, 
 * see <http://www.gnu.org/licenses/> for the GNU Affero General Public License version 3 
 * and <http://www.linagora.com/licenses/> for the Additional Terms applicable to 
 * OBM connectors. 
 * 
 * ***** END LICENSE BLOCK ***** */
package org.obm.opush;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.obm.DateUtils.date;
import static org.obm.opush.IntegrationPushTestUtils.mockNextGeneratedSyncKey;
import static org.obm.opush.IntegrationTestUtils.buildWBXMLOpushClient;
import static org.obm.opush.IntegrationUserAccessUtils.mockUsersAccess;

import java.util.Arrays;
import java.util.Date;
import java.util.Set;

import org.easymock.IMocksControl;
import org.fest.util.Files;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.obm.filter.Slow;
import org.obm.opush.ActiveSyncServletModule.OpushServer;
import org.obm.opush.SingleUserFixture.OpushUser;
import org.obm.opush.env.Configuration;
import org.obm.push.bean.FilterType;
import org.obm.push.bean.FolderSyncState;
import org.obm.push.bean.FolderSyncStatus;
import org.obm.push.bean.GetItemEstimateStatus;
import org.obm.push.bean.ItemOperationsStatus;
import org.obm.push.bean.ItemSyncState;
import org.obm.push.bean.MeetingResponseStatus;
import org.obm.push.bean.MoveItemsStatus;
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.PingStatus;
import org.obm.push.bean.SyncCollection;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.SyncStatus;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.hierarchy.HierarchyCollectionChanges;
import org.obm.push.bean.change.item.ItemChange;
import org.obm.push.calendar.CalendarBackend;
import org.obm.push.contacts.ContactsBackend;
import org.obm.push.exception.DaoException;
import org.obm.push.mail.imap.GuiceModule;
import org.obm.push.mail.imap.SlowGuiceRunner;
import org.obm.push.protocol.bean.FolderSyncResponse;
import org.obm.push.protocol.bean.MeetingHandlerResponse;
import org.obm.push.protocol.bean.PingResponse;
import org.obm.push.protocol.bean.SyncResponse;
import org.obm.push.service.DateService;
import org.obm.push.store.CollectionDao;
import org.obm.push.store.FolderSyncStateBackendMappingDao;
import org.obm.push.store.HearbeatDao;
import org.obm.push.store.MonitoredCollectionDao;
import org.obm.push.store.SyncedCollectionDao;
import org.obm.push.store.UnsynchronizedItemDao;
import org.obm.push.task.TaskBackend;
import org.obm.push.utils.DateUtils;
import org.obm.push.utils.collection.ClassToInstanceAgregateView;
import org.obm.sync.push.client.ItemOperationResponse;
import org.obm.sync.push.client.MoveItemsResponse;
import org.obm.sync.push.client.OPClient;
import org.obm.sync.push.client.beans.GetItemEstimateSingleFolderResponse;
import org.obm.sync.push.client.commands.MoveItemsCommand.Move;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.icegreen.greenmail.imap.ImapHostManager;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;

@RunWith(SlowGuiceRunner.class) @Slow
@GuiceModule(MailBackendImapTimeoutTestModule.class)
public class MailBackendImapTimeoutTest {

	@Inject	SingleUserFixture singleUserFixture;
	@Inject	OpushServer opushServer;
	@Inject	ClassToInstanceAgregateView<Object> classToInstanceMap;
	@Inject GreenMail greenMail;
	@Inject ImapConnectionCounter imapConnectionCounter;
	@Inject PendingQueriesLock pendingQueries;
	@Inject IMocksControl mocksControl;
	@Inject Configuration configuration;
	@Inject ContactsBackend contactsBackend;
	@Inject TaskBackend taskBackend;
	@Inject CalendarBackend calendarBackend;
	
	private CollectionDao collectionDao;
	private FolderSyncStateBackendMappingDao folderSyncStateBackendMappingDao;
	private UnsynchronizedItemDao unsynchronizedItemDao;
	private HearbeatDao hearbeatDao;
	private MonitoredCollectionDao monitoredCollectionDao;
	private DateService dateService;

	private GreenMailUser greenMailUser;
	private ImapHostManager imapHostManager;
	private OpushUser user;
	private String mailbox;
	private String inboxCollectionPath;
	private int inboxCollectionId;
	private String inboxCollectionIdAsString;
	private String trashCollectionPath;
	private int trashCollectionId;

	@Before
	public void init() throws Exception {
		user = singleUserFixture.jaures;
		greenMail.start();
		mailbox = user.user.getLoginAtDomain();
		greenMailUser = greenMail.setUser(mailbox, user.password);
		imapHostManager = greenMail.getManagers().getImapHostManager();
		imapHostManager.createMailbox(greenMailUser, "Trash");

		inboxCollectionPath = IntegrationTestUtils.buildEmailInboxCollectionPath(user);
		inboxCollectionId = 1234;
		inboxCollectionIdAsString = String.valueOf(inboxCollectionId);
		trashCollectionPath = IntegrationTestUtils.buildEmailTrashCollectionPath(user);
		trashCollectionId = 1645;
		
		collectionDao = classToInstanceMap.get(CollectionDao.class);
		folderSyncStateBackendMappingDao = classToInstanceMap.get(FolderSyncStateBackendMappingDao.class);
		unsynchronizedItemDao = classToInstanceMap.get(UnsynchronizedItemDao.class);
		hearbeatDao = classToInstanceMap.get(HearbeatDao.class);
		monitoredCollectionDao = classToInstanceMap.get(MonitoredCollectionDao.class);
		dateService = classToInstanceMap.get(DateService.class);

		bindCollectionIdToPath();
	}

	private void bindCollectionIdToPath() throws Exception {
		expect(collectionDao.getCollectionPath(inboxCollectionId)).andReturn(inboxCollectionPath).anyTimes();
		expect(collectionDao.getCollectionPath(trashCollectionId)).andReturn(trashCollectionPath).anyTimes();
		
		SyncedCollectionDao syncedCollectionDao = classToInstanceMap.get(SyncedCollectionDao.class);
		SyncCollection syncCollection = new SyncCollection(inboxCollectionId, inboxCollectionPath);
		expect(syncedCollectionDao.get(user.credentials, user.device, inboxCollectionId))
			.andReturn(syncCollection).anyTimes();
		SyncCollection trashCollection = new SyncCollection(trashCollectionId, trashCollectionPath);
		expect(syncedCollectionDao.get(user.credentials, user.device, trashCollectionId))
			.andReturn(trashCollection).anyTimes();
		
		syncedCollectionDao.put(eq(user.credentials), eq(user.device), anyObject(SyncCollection.class));
		expectLastCall().anyTimes();
	}

	@After
	public void shutdown() throws Exception {
		opushServer.stop();
		greenMail.stop();
		Files.delete(configuration.dataDir);
	}

	@Test
	public void testSyncHandler() throws Exception {
		SyncKey initialSyncKey = SyncKey.INITIAL_FOLDER_SYNC_KEY;
		SyncKey firstAllocatedSyncKey = new SyncKey("456");
		SyncKey secondAllocatedSyncKey = new SyncKey("789");
		int allocatedStateId = 3;
		int allocatedStateId2 = 4;
		
		mockUsersAccess(classToInstanceMap, Arrays.asList(user));
		mockNextGeneratedSyncKey(classToInstanceMap, firstAllocatedSyncKey, secondAllocatedSyncKey);
		
		Date initialDate = DateUtils.getEpochPlusOneSecondCalendar().getTime();
		ItemSyncState firstAllocatedState = ItemSyncState.builder()
				.syncDate(initialDate)
				.syncKey(firstAllocatedSyncKey)
				.id(allocatedStateId)
				.build();
		ItemSyncState currentAllocatedState = ItemSyncState.builder()
				.syncDate(date("2012-10-10T16:22:53"))
				.syncKey(secondAllocatedSyncKey)
				.id(allocatedStateId2)
				.build();
		
		expect(dateService.getEpochPlusOneSecondDate()).andReturn(initialDate).times(2);
		expect(dateService.getCurrentDate()).andReturn(currentAllocatedState.getSyncDate());
		expectCollectionDaoPerformInitialSync(initialSyncKey, firstAllocatedState, inboxCollectionId);
		expectCollectionDaoPerformSync(firstAllocatedSyncKey, firstAllocatedState);
		
		mocksControl.replay();
		opushServer.start();

		OPClient opClient = buildWBXMLOpushClient(singleUserFixture.jaures, opushServer.getPort());
		opClient.syncEmail(initialSyncKey, inboxCollectionIdAsString, FilterType.THREE_DAYS_BACK, 25);
		greenMail.lockGreenmailAndReleaseAfter(20);
		SyncResponse syncResponse = opClient.syncEmail(firstAllocatedSyncKey, inboxCollectionIdAsString, FilterType.THREE_DAYS_BACK, 25);

		mocksControl.verify();
		assertThat(syncResponse.getStatus()).isEqualTo(SyncStatus.SERVER_ERROR);
	}
	@Test
	public void testFolderSyncHandler() throws Exception {
		SyncKey syncKey = new SyncKey("123");
		SyncKey secondSyncKey = new SyncKey("456");
		int stateId = 3;
		int stateId2 = 4;
		
		mockUsersAccess(classToInstanceMap, Arrays.asList(user));
		mockNextGeneratedSyncKey(classToInstanceMap, syncKey);
		
		FolderSyncState folderSyncState = FolderSyncState.builder()
				.syncKey(syncKey)
				.id(stateId)
				.build();
		FolderSyncState secondFolderSyncState = FolderSyncState.builder()
				.syncKey(secondSyncKey)
				.id(stateId2)
				.build();
		
		expect(collectionDao.findFolderStateForKey(syncKey))
			.andReturn(folderSyncState);
		expect(collectionDao.allocateNewFolderSyncState(user.device, syncKey))
			.andReturn(secondFolderSyncState).anyTimes();
		expect(collectionDao.allocateNewFolderSyncState(user.device, secondSyncKey))
			.andReturn(secondFolderSyncState).anyTimes();
		
		UserDataRequest udr = new UserDataRequest(user.credentials, "FolderSync", user.device);
		expect(contactsBackend.getHierarchyChanges(udr, folderSyncState, secondFolderSyncState))
			.andReturn(HierarchyCollectionChanges.builder().build()).anyTimes();
		expect(taskBackend.getHierarchyChanges(udr, folderSyncState, secondFolderSyncState))
			.andReturn(HierarchyCollectionChanges.builder().build()).anyTimes();
		expect(calendarBackend.getHierarchyChanges(udr, folderSyncState, secondFolderSyncState))
			.andReturn(HierarchyCollectionChanges.builder().build()).anyTimes();
		expect(contactsBackend.getPIMDataType())
			.andReturn(PIMDataType.CONTACTS).anyTimes();
		expect(taskBackend.getPIMDataType())
			.andReturn(PIMDataType.TASKS).anyTimes();
		expect(calendarBackend.getPIMDataType())
			.andReturn(PIMDataType.CALENDAR).anyTimes();
		
		folderSyncStateBackendMappingDao.createMapping(anyObject(PIMDataType.class), anyObject(FolderSyncState.class));
		expectLastCall().anyTimes();
		
		mocksControl.replay();
		opushServer.start();

		OPClient opClient = buildWBXMLOpushClient(singleUserFixture.jaures, opushServer.getPort());
		greenMail.lockGreenmailAndReleaseAfter(20);
		FolderSyncResponse folderSyncResponse = opClient.folderSync(syncKey);
		
		mocksControl.verify();
		assertThat(folderSyncResponse.getStatus()).isEqualTo(FolderSyncStatus.SERVER_ERROR);
	}
	
	@Test
	public void testGetItemEstimateHandler() throws Exception {
		SyncKey syncKey = new SyncKey("123");
		int stateId = 3;
		
		mockUsersAccess(classToInstanceMap, Arrays.asList(user));
		
		Date initialDate = DateUtils.getEpochPlusOneSecondCalendar().getTime();
		ItemSyncState syncState = ItemSyncState.builder()
				.syncDate(initialDate)
				.syncKey(syncKey)
				.id(stateId)
				.build();
		
		expect(collectionDao.findItemStateForKey(syncKey))
			.andReturn(syncState);
		
		expect(unsynchronizedItemDao.listItemsToAdd(syncKey))
			.andReturn(ImmutableList.<ItemChange> of());
		
		expect(dateService.getCurrentDate()).andReturn(syncState.getSyncDate());
		
		mocksControl.replay();
		opushServer.start();

		OPClient opClient = buildWBXMLOpushClient(singleUserFixture.jaures, opushServer.getPort());
		greenMail.lockGreenmailAndReleaseAfter(20);
		GetItemEstimateSingleFolderResponse itemEstimateResponse = opClient.getItemEstimateOnMailFolder(syncKey, inboxCollectionId);
		
		mocksControl.verify();
		assertThat(itemEstimateResponse.getStatus()).isEqualTo(GetItemEstimateStatus.NEED_SYNC);
	}
	
	@Test
	public void testItemOperationsHandler() throws Exception {
		String emailId1 = ":1";
		
		mockUsersAccess(classToInstanceMap, Arrays.asList(user));
		
		mocksControl.replay();
		opushServer.start();

		OPClient opClient = buildWBXMLOpushClient(singleUserFixture.jaures, opushServer.getPort());
		greenMail.lockGreenmailAndReleaseAfter(20);
		ItemOperationResponse itemOperationResponse = opClient.itemOperationFetch(inboxCollectionId, inboxCollectionId + emailId1);
		
		mocksControl.verify();
		assertThat(itemOperationResponse.getStatus()).isEqualTo(ItemOperationsStatus.SERVER_ERROR);
	}
	
	@Test
	public void testMeetingResponseHandler() throws Exception {
		String emailId1 = ":1";
		
		mockUsersAccess(classToInstanceMap, Arrays.asList(user));
		
		mocksControl.replay();
		opushServer.start();

		OPClient opClient = buildWBXMLOpushClient(singleUserFixture.jaures, opushServer.getPort());
		greenMail.lockGreenmailAndReleaseAfter(20);
		MeetingHandlerResponse meetingHandlerResponse = opClient.meetingResponse(inboxCollectionIdAsString, inboxCollectionId + emailId1);
		
		mocksControl.verify();
		assertThat(meetingHandlerResponse.getItemChanges().iterator().next().getStatus()).isEqualTo(MeetingResponseStatus.SERVER_ERROR);
	}

	@Test
	public void testMoveItemsHandler() throws Exception {
		String emailId1 = ":1";
		
		mockUsersAccess(classToInstanceMap, Arrays.asList(user));
		
		expect(collectionDao.getCollectionPath(inboxCollectionId))
			.andReturn(inboxCollectionPath).anyTimes();
		expect(collectionDao.getCollectionPath(trashCollectionId))
			.andReturn(trashCollectionPath).anyTimes();
		expect(collectionDao.getCollectionMapping(user.device, trashCollectionPath))
			.andReturn(trashCollectionId).anyTimes();
		
		mocksControl.replay();
		opushServer.start();

		OPClient opClient = buildWBXMLOpushClient(singleUserFixture.jaures, opushServer.getPort());
		greenMail.lockGreenmailAndReleaseAfter(20);
		MoveItemsResponse moveItemsResponse = opClient.moveItems(
				new Move(inboxCollectionId + emailId1, inboxCollectionId, trashCollectionId));
		
		mocksControl.verify();
		assertThat(moveItemsResponse.getStatus()).isEqualTo(MoveItemsStatus.SERVER_ERROR);
	}
	
	@Ignore("Waiting for push mode in order to be checked")
	@Test
	public void testPingHandler() throws Exception {
		long hearbeat = 5;
		SyncKey syncKey = new SyncKey("123");
		int stateId = 3;
		
		mockUsersAccess(classToInstanceMap, Arrays.asList(user));
		
		Date initialDate = DateUtils.getEpochPlusOneSecondCalendar().getTime();
		ItemSyncState syncState = ItemSyncState.builder()
				.syncDate(initialDate)
				.syncKey(syncKey)
				.id(stateId)
				.build();
		
		hearbeatDao.updateLastHearbeat(user.device, hearbeat);
		expectLastCall();
		
		monitoredCollectionDao.put(eq(user.credentials), eq(user.device), anyObject(Set.class));
		expectLastCall();
		
		expect(collectionDao.lastKnownState(user.device, inboxCollectionId))
			.andReturn(syncState);
		
		mocksControl.replay();
		opushServer.start();

		OPClient opClient = buildWBXMLOpushClient(singleUserFixture.jaures, opushServer.getPort());
		greenMail.lockGreenmailAndReleaseAfter(20);
		PingResponse pingResponse = opClient.ping(inboxCollectionIdAsString, hearbeat);
		
		mocksControl.verify();
		assertThat(pingResponse.getPingStatus()).isEqualTo(PingStatus.SERVER_ERROR);
	}

	private void expectCollectionDaoPerformSync(SyncKey requestSyncKey,
			ItemSyncState allocatedState)
					throws DaoException {
		expect(collectionDao.findItemStateForKey(requestSyncKey)).andReturn(allocatedState).times(2);
	}

	private void expectCollectionDaoPerformInitialSync(SyncKey initialSyncKey,
			ItemSyncState itemSyncState, int collectionId)
					throws DaoException {
		
		expect(collectionDao.findItemStateForKey(initialSyncKey)).andReturn(null);
		expect(collectionDao.updateState(user.device, collectionId, itemSyncState.getSyncKey(), itemSyncState.getSyncDate()))
			.andReturn(itemSyncState);
		collectionDao.resetCollection(user.device, collectionId);
		expectLastCall();
	}
}
