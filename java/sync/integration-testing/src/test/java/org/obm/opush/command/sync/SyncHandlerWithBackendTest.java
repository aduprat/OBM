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
package org.obm.opush.command.sync;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.obm.DateUtils.date;
import static org.obm.opush.IntegrationPushTestUtils.mockNextGeneratedSyncKey;
import static org.obm.opush.IntegrationTestUtils.buildWBXMLOpushClient;
import static org.obm.opush.IntegrationUserAccessUtils.mockUsersAccess;
import static org.obm.opush.command.sync.EmailSyncTestUtils.getCollectionWithId;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.easymock.IMocksControl;
import org.fest.util.Files;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.obm.configuration.EmailConfiguration;
import org.obm.filter.Slow;
import org.obm.opush.ActiveSyncServletModule.OpushServer;
import org.obm.opush.ImapConnectionCounter;
import org.obm.opush.IntegrationTestUtils;
import org.obm.opush.PendingQueriesLock;
import org.obm.opush.SingleUserFixture;
import org.obm.opush.SingleUserFixture.OpushUser;
import org.obm.opush.env.Configuration;
import org.obm.push.bean.CalendarBusyStatus;
import org.obm.push.bean.CalendarSensitivity;
import org.obm.push.bean.FilterType;
import org.obm.push.bean.ItemSyncState;
import org.obm.push.bean.MSEvent;
import org.obm.push.bean.MSEventUid;
import org.obm.push.bean.ServerId;
import org.obm.push.bean.SyncCollection;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.item.ItemChangeBuilder;
import org.obm.push.exception.DaoException;
import org.obm.push.mail.MailboxService;
import org.obm.push.mail.imap.GuiceModule;
import org.obm.push.mail.imap.SlowGuiceRunner;
import org.obm.push.protocol.bean.SyncResponse;
import org.obm.push.protocol.bean.SyncResponse.SyncCollectionResponse;
import org.obm.push.service.DateService;
import org.obm.push.service.EventService;
import org.obm.push.store.CollectionDao;
import org.obm.push.store.ItemTrackingDao;
import org.obm.push.store.SyncedCollectionDao;
import org.obm.push.utils.DateUtils;
import org.obm.push.utils.collection.ClassToInstanceAgregateView;
import org.obm.sync.calendar.Event;
import org.obm.sync.calendar.EventObmId;
import org.obm.sync.items.EventChanges;
import org.obm.sync.push.client.OPClient;
import org.obm.sync.services.ICalendar;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.icegreen.greenmail.imap.ImapHostManager;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;

@GuiceModule(SyncHandlerWithBackendTestModule.class)
@RunWith(SlowGuiceRunner.class) @Slow
public class SyncHandlerWithBackendTest {

	private final static int ONE_WINDOWS_SIZE = 1;
	
	@Inject	SingleUserFixture singleUserFixture;
	@Inject	OpushServer opushServer;
	@Inject	ClassToInstanceAgregateView<Object> classToInstanceMap;
	@Inject GreenMail greenMail;
	@Inject ImapConnectionCounter imapConnectionCounter;
	@Inject PendingQueriesLock pendingQueries;
	@Inject IMocksControl mocksControl;
	@Inject Configuration configuration;
	@Inject MailboxService mailboxService;
	@Inject EventService eventService;
	
	private ItemTrackingDao itemTrackingDao;
	private CollectionDao collectionDao;
	private DateService dateService;
	private ICalendar calendarClient;

	private GreenMailUser greenMailUser;
	private ImapHostManager imapHostManager;
	private OpushUser user;
	private String mailbox;
	private String inboxCollectionPath;
	private String calendarCollectionPath;
	private int inboxCollectionId;
	private int calendarCollectionId;
	private String inboxCollectionIdAsString;
	private String calendarCollectionIdAsString;

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
		calendarCollectionPath = IntegrationTestUtils.buildCalendarCollectionPath(user);
		calendarCollectionId = 5678;
		calendarCollectionIdAsString = String.valueOf(calendarCollectionId);
		
		itemTrackingDao = classToInstanceMap.get(ItemTrackingDao.class);
		collectionDao = classToInstanceMap.get(CollectionDao.class);
		dateService = classToInstanceMap.get(DateService.class);
		calendarClient = classToInstanceMap.get(ICalendar.class);
		eventService = classToInstanceMap.get(EventService.class);

		bindCollectionIdToPath();
	}

	private void bindCollectionIdToPath() throws Exception {
		expect(collectionDao.getCollectionPath(inboxCollectionId)).andReturn(inboxCollectionPath).anyTimes();
		expect(collectionDao.getCollectionPath(calendarCollectionId)).andReturn(calendarCollectionPath).anyTimes();
		
		SyncedCollectionDao syncedCollectionDao = classToInstanceMap.get(SyncedCollectionDao.class);
		expect(syncedCollectionDao.get(user.credentials, user.device, inboxCollectionId))
			.andReturn(new SyncCollection(inboxCollectionId, inboxCollectionPath)).anyTimes();
		expect(syncedCollectionDao.get(user.credentials, user.device, calendarCollectionId))
			.andReturn(new SyncCollection(calendarCollectionId, calendarCollectionPath)).anyTimes();
		
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
	public void testInitialSyncThenRecreatesAccountOnMails() throws Exception {
		SyncKey initialSyncKey = SyncKey.INITIAL_FOLDER_SYNC_KEY;
		SyncKey firstAllocatedSyncKey = new SyncKey("132");
		SyncKey secondAllocatedSyncKey = new SyncKey("456");
		SyncKey newFirstAllocatedSyncKey = new SyncKey("789");
		SyncKey newSecondAllocatedSyncKey = new SyncKey("980");
		SyncKey newThirdAllocatedSyncKey = new SyncKey("123456");
		SyncKey newFourthAllocatedSyncKey = new SyncKey("456789");
		int firstAllocatedStateId = 3;
		int secondAllocatedStateId = 4;
		int newFirstAllocatedStateId = 5;
		int newSecondAllocatedStateId = 6;
		int newThirdAllocatedStateId = 7;
		int newFourthAllocatedStateId = 8;
		
		mockUsersAccess(classToInstanceMap, Arrays.asList(user));
		mockNextGeneratedSyncKey(classToInstanceMap, firstAllocatedSyncKey, 
				secondAllocatedSyncKey, newFirstAllocatedSyncKey, 
				newSecondAllocatedSyncKey, newThirdAllocatedSyncKey,
				newFourthAllocatedSyncKey);
		
		Date initialDate = DateUtils.getEpochPlusOneSecondCalendar().getTime();
		ItemSyncState firstAllocatedState = ItemSyncState.builder()
				.syncDate(initialDate)
				.syncKey(firstAllocatedSyncKey)
				.id(firstAllocatedStateId)
				.build();
		ItemSyncState secondAllocatedState = ItemSyncState.builder()
				.syncDate(date("2012-10-10T16:22:53"))
				.syncKey(secondAllocatedSyncKey)
				.id(secondAllocatedStateId)
				.build();
		ItemSyncState newFirstAllocatedState = ItemSyncState.builder()
				.syncDate(initialDate)
				.syncKey(newFirstAllocatedSyncKey)
				.id(newFirstAllocatedStateId)
				.build();
		ItemSyncState newSecondAllocatedState = ItemSyncState.builder()
				.syncDate(date("2012-10-10T17:22:53"))
				.syncKey(newSecondAllocatedSyncKey)
				.id(newSecondAllocatedStateId)
				.build();
		ItemSyncState newThirdAllocatedState = ItemSyncState.builder()
				.syncDate(date("2012-10-10T17:22:53"))
				.syncKey(newThirdAllocatedSyncKey)
				.id(newThirdAllocatedStateId)
				.build();
		ItemSyncState newFourthAllocatedState = ItemSyncState.builder()
				.syncDate(date("2012-10-10T17:22:53"))
				.syncKey(newFourthAllocatedSyncKey)
				.id(newFourthAllocatedStateId)
				.build();
		expect(dateService.getEpochPlusOneSecondDate()).andReturn(initialDate).anyTimes();
		
		expect(dateService.getCurrentDate()).andReturn(secondAllocatedState.getSyncDate()).once();
		expectCollectionDaoPerformInitialSync(initialSyncKey, firstAllocatedState, inboxCollectionId);
		expectCollectionDaoPerformSync(firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, inboxCollectionId);
		
		expect(dateService.getCurrentDate()).andReturn(newSecondAllocatedState.getSyncDate()).times(2);
		expectCollectionDaoPerformInitialSync(initialSyncKey, newFirstAllocatedState, inboxCollectionId);
		expectCollectionDaoPerformSync(newFirstAllocatedSyncKey, newFirstAllocatedState, newSecondAllocatedState, inboxCollectionId);
		expectCollectionDaoPerformSync(newSecondAllocatedSyncKey, newSecondAllocatedState, newThirdAllocatedState, inboxCollectionId);
		expectCollectionDaoPerformSync(newThirdAllocatedSyncKey, newThirdAllocatedState, newFourthAllocatedState, inboxCollectionId);

		itemTrackingDao.markAsSynced(anyObject(ItemSyncState.class), anyObject(Set.class));
		expectLastCall().anyTimes();
		
		mocksControl.replay();
		opushServer.start();
		OPClient opClient = buildWBXMLOpushClient(user, opushServer.getPort());
		sendTwoEmailsToImapServer();
		opClient.syncEmail(initialSyncKey, inboxCollectionIdAsString, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		SyncResponse syncResponse = opClient.syncEmail(firstAllocatedSyncKey, inboxCollectionIdAsString, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		
		opClient.syncEmail(initialSyncKey, inboxCollectionIdAsString, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		SyncResponse firstSyncResponse = opClient.syncEmail(newFirstAllocatedSyncKey, inboxCollectionIdAsString, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		SyncResponse secondSyncResponse = opClient.syncEmail(newSecondAllocatedSyncKey, inboxCollectionIdAsString, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		SyncResponse thirdSyncResponse = opClient.syncEmail(newThirdAllocatedSyncKey, inboxCollectionIdAsString, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		mocksControl.verify();

		SyncCollectionResponse collectionResponse = getCollectionWithId(syncResponse, inboxCollectionIdAsString);
		SyncCollectionResponse firstCollectionResponse = getCollectionWithId(firstSyncResponse, inboxCollectionIdAsString);
		SyncCollectionResponse secondCollectionResponse = getCollectionWithId(secondSyncResponse, inboxCollectionIdAsString);
		SyncCollectionResponse thirdCollectionResponse = getCollectionWithId(thirdSyncResponse, inboxCollectionIdAsString);

		assertThat(collectionResponse.getItemChanges()).containsOnly(new ItemChangeBuilder()
			.serverId(inboxCollectionId + ":2")
			.withNewFlag(true)
			.build());
		assertThat(firstCollectionResponse.getItemChanges()).containsOnly(new ItemChangeBuilder()
			.serverId(inboxCollectionId + ":2")
			.withNewFlag(true)
			.build());
		assertThat(secondCollectionResponse.getItemChanges()).containsOnly(new ItemChangeBuilder()
			.serverId(inboxCollectionId + ":1")
			.withNewFlag(true)
			.build());
		assertThat(thirdCollectionResponse.getItemChanges()).hasSize(0);
		
		assertEmailCountInMailbox(EmailConfiguration.IMAP_INBOX_NAME, 2);
		assertThat(pendingQueries.waitingClose(10, TimeUnit.SECONDS)).isTrue();
		assertThat(imapConnectionCounter.loginCounter.get()).isEqualTo(4);
		assertThat(imapConnectionCounter.closeCounter.get()).isEqualTo(4);
		assertThat(imapConnectionCounter.selectCounter.get()).isEqualTo(4);
		assertThat(imapConnectionCounter.listMailboxesCounter.get()).isEqualTo(0);
	}

	@Test
	public void testInitialSyncThenRecreatesAccountOnCalendars() throws Exception {
		SyncKey initialSyncKey = SyncKey.INITIAL_FOLDER_SYNC_KEY;
		SyncKey firstAllocatedSyncKey = new SyncKey("132");
		SyncKey secondAllocatedSyncKey = new SyncKey("456");
		SyncKey newFirstAllocatedSyncKey = new SyncKey("789");
		SyncKey newSecondAllocatedSyncKey = new SyncKey("980");
		SyncKey newThirdAllocatedSyncKey = new SyncKey("123456");
		SyncKey newFourthAllocatedSyncKey = new SyncKey("456789");
		int firstAllocatedStateId = 3;
		int secondAllocatedStateId = 4;
		int newFirstAllocatedStateId = 5;
		int newSecondAllocatedStateId = 6;
		int newThirdAllocatedStateId = 7;
		int newFourthAllocatedStateId = 8;
		
		mockUsersAccess(classToInstanceMap, Arrays.asList(user));
		mockNextGeneratedSyncKey(classToInstanceMap, firstAllocatedSyncKey, 
				secondAllocatedSyncKey, newFirstAllocatedSyncKey, 
				newSecondAllocatedSyncKey, newThirdAllocatedSyncKey,
				newFourthAllocatedSyncKey);
		
		Date initialDate = DateUtils.getEpochPlusOneSecondCalendar().getTime();
		ItemSyncState firstAllocatedState = ItemSyncState.builder()
				.syncDate(initialDate)
				.syncKey(firstAllocatedSyncKey)
				.id(firstAllocatedStateId)
				.build();
		Date secondDate = date("2012-10-10T16:22:53");
		ItemSyncState secondAllocatedState = ItemSyncState.builder()
				.syncDate(secondDate)
				.syncKey(secondAllocatedSyncKey)
				.id(secondAllocatedStateId)
				.build();
		ItemSyncState newFirstAllocatedState = ItemSyncState.builder()
				.syncDate(initialDate)
				.syncKey(newFirstAllocatedSyncKey)
				.id(newFirstAllocatedStateId)
				.build();
		ItemSyncState newSecondAllocatedState = ItemSyncState.builder()
				.syncDate(secondDate)
				.syncKey(newSecondAllocatedSyncKey)
				.id(newSecondAllocatedStateId)
				.build();
		ItemSyncState newThirdAllocatedState = ItemSyncState.builder()
				.syncDate(secondDate)
				.syncKey(newThirdAllocatedSyncKey)
				.id(newThirdAllocatedStateId)
				.build();
		ItemSyncState newFourthAllocatedState = ItemSyncState.builder()
				.syncDate(secondDate)
				.syncKey(newFourthAllocatedSyncKey)
				.id(newFourthAllocatedStateId)
				.build();
		expect(dateService.getEpochPlusOneSecondDate()).andReturn(initialDate).anyTimes();
		
		expectCollectionDaoPerformInitialSync(initialSyncKey, firstAllocatedState, calendarCollectionId);
		expectCollectionDaoPerformSync(firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, calendarCollectionId);

		Event event = new Event();
		event.setUid(new EventObmId(1));
		event.setTitle("event");
		Event event2 = new Event();
		event2.setUid(new EventObmId(2));
		event2.setTitle("event2");
		expect(calendarClient.getSyncEventDate(eq(user.accessToken), eq(user.user.getLogin()), anyObject(Date.class)))
			.andReturn(EventChanges.builder()
					.lastSync(secondDate)
					.updates(Lists.newArrayList(event, event2))
					.build());
		expect(calendarClient.getUserEmail(user.accessToken))
			.andReturn(user.user.getLoginAtDomain());
		
		TimeZone timeZone = TimeZone.getTimeZone("GMT");
		Calendar calendar = DateUtils.getEpochCalendar(timeZone);
		MSEvent msEvent = new MSEvent();
		msEvent.setUid(new MSEventUid("1"));
		msEvent.setSubject("event");
		msEvent.setSensitivity(CalendarSensitivity.NORMAL);
		msEvent.setBusyStatus(CalendarBusyStatus.FREE);
		msEvent.setAllDayEvent(false);
		msEvent.setDtStamp(calendar.getTime());
		msEvent.setTimeZone(timeZone);
		MSEvent msEvent2 = new MSEvent();
		msEvent2.setUid(new MSEventUid("2"));
		msEvent2.setSubject("event2");
		msEvent2.setSensitivity(CalendarSensitivity.NORMAL);
		msEvent2.setBusyStatus(CalendarBusyStatus.FREE);
		msEvent2.setAllDayEvent(false);
		msEvent2.setDtStamp(calendar.getTime());
		msEvent2.setTimeZone(timeZone);
		expect(eventService.convertEventToMSEvent(anyObject(UserDataRequest.class), eq(event)))
			.andReturn(msEvent).times(2);
		expect(eventService.convertEventToMSEvent(anyObject(UserDataRequest.class), eq(event2)))
			.andReturn(msEvent2).times(2);
		
		String serverId = calendarCollectionIdAsString + ":" + msEvent.getUid().serializeToString();
		expect(itemTrackingDao.isServerIdSynced(firstAllocatedState, 
				new ServerId(serverId)))
			.andReturn(false);
		
		expectCollectionDaoPerformInitialSync(initialSyncKey, newFirstAllocatedState, calendarCollectionId);
		expectCollectionDaoPerformSync(newFirstAllocatedSyncKey, newFirstAllocatedState, newSecondAllocatedState, calendarCollectionId);
		expectCollectionDaoPerformSync(newSecondAllocatedSyncKey, newSecondAllocatedState, newThirdAllocatedState, calendarCollectionId);
		expectCollectionDaoPerformSync(newThirdAllocatedSyncKey, newThirdAllocatedState, newFourthAllocatedState, calendarCollectionId);
		
		expect(itemTrackingDao.isServerIdSynced(newFirstAllocatedState, 
				new ServerId(serverId)))
			.andReturn(false);
		String serverId2 = calendarCollectionIdAsString + ":" + msEvent2.getUid().serializeToString();
		expect(itemTrackingDao.isServerIdSynced(newSecondAllocatedState, 
				new ServerId(serverId2)))
			.andReturn(false);
		
		expect(calendarClient.getSyncEventDate(eq(user.accessToken), eq(user.user.getLogin()), anyObject(Date.class)))
			.andReturn(EventChanges.builder()
					.lastSync(secondDate)
					.updates(Lists.newArrayList(event, event2))
					.build());
		expect(calendarClient.getUserEmail(user.accessToken))
			.andReturn(user.user.getLoginAtDomain());
		expect(calendarClient.getSyncEventDate(eq(user.accessToken), eq(user.user.getLogin()), anyObject(Date.class)))
			.andReturn(EventChanges.builder()
					.lastSync(secondDate)
					.build());
		expect(calendarClient.getUserEmail(user.accessToken))
			.andReturn(user.user.getLoginAtDomain());
		itemTrackingDao.markAsSynced(anyObject(ItemSyncState.class), anyObject(Set.class));
		expectLastCall().anyTimes();
		
		mocksControl.replay();
		opushServer.start();
		OPClient opClient = buildWBXMLOpushClient(user, opushServer.getPort());
		sendTwoEmailsToImapServer();
		opClient.syncEmail(initialSyncKey, calendarCollectionIdAsString, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		SyncResponse syncResponse = opClient.syncEmail(firstAllocatedSyncKey, calendarCollectionIdAsString, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		
		opClient.syncEmail(initialSyncKey, calendarCollectionIdAsString, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		SyncResponse firstSyncResponse = opClient.syncEmail(newFirstAllocatedSyncKey, calendarCollectionIdAsString, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		SyncResponse secondSyncResponse = opClient.syncEmail(newSecondAllocatedSyncKey, calendarCollectionIdAsString, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		SyncResponse thirdSyncResponse = opClient.syncEmail(newThirdAllocatedSyncKey, calendarCollectionIdAsString, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		mocksControl.verify();

		SyncCollectionResponse collectionResponse = getCollectionWithId(syncResponse, calendarCollectionIdAsString);
		SyncCollectionResponse firstCollectionResponse = getCollectionWithId(firstSyncResponse, calendarCollectionIdAsString);
		SyncCollectionResponse secondCollectionResponse = getCollectionWithId(secondSyncResponse, calendarCollectionIdAsString);
		SyncCollectionResponse thirdCollectionResponse = getCollectionWithId(thirdSyncResponse, calendarCollectionIdAsString);

		assertThat(collectionResponse.getItemChanges()).containsOnly(new ItemChangeBuilder()
			.serverId(serverId)
			.withNewFlag(true)
			.build());
		assertThat(firstCollectionResponse.getItemChanges()).containsOnly(new ItemChangeBuilder()
			.serverId(serverId)
			.withNewFlag(true)
			.build());
		assertThat(secondCollectionResponse.getItemChanges()).containsOnly(new ItemChangeBuilder()
			.serverId(serverId2)
			.withNewFlag(true)
			.build());
		assertThat(thirdCollectionResponse.getItemChanges()).hasSize(0);
	}

	private void expectCollectionDaoPerformSync(SyncKey requestSyncKey,
			ItemSyncState allocatedState, ItemSyncState newItemSyncState, int collectionId)
					throws DaoException {
		expect(collectionDao.findItemStateForKey(requestSyncKey)).andReturn(allocatedState).times(2);
		expect(collectionDao.updateState(user.device, collectionId, newItemSyncState.getSyncKey(), newItemSyncState.getSyncDate()))
				.andReturn(newItemSyncState);
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

	private void sendTwoEmailsToImapServer() throws InterruptedException {
		GreenMailUtil.sendTextEmail(mailbox, mailbox, "subject", "body", greenMail.getSmtp().getServerSetup());
		GreenMailUtil.sendTextEmail(mailbox, mailbox, "subject2", "body", greenMail.getSmtp().getServerSetup());
		greenMail.waitForIncomingEmail(2);
	}

	private void assertEmailCountInMailbox(String mailbox, Integer expectedNumberOfEmails) {
		MailFolder inboxFolder = imapHostManager.getFolder(greenMailUser, mailbox);
		assertThat(inboxFolder.getMessageCount()).isEqualTo(expectedNumberOfEmails);
	}
}
