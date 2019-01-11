/* ***** BEGIN LICENSE BLOCK *****
 * Copyright (C) 2011-2014  Linagora
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
package org.obm.sync.client.calendar;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.obm.sync.IntegerParameter;
import org.obm.sync.NotAllowedException;
import org.obm.sync.Parameter;
import org.obm.sync.StringParameter;
import org.obm.sync.auth.AccessToken;
import org.obm.sync.auth.EventAlreadyExistException;
import org.obm.sync.auth.EventNotFoundException;
import org.obm.sync.auth.ServerFault;
import org.obm.sync.calendar.Event;
import org.obm.sync.calendar.EventExtId;
import org.obm.sync.calendar.EventObmId;
import org.obm.sync.calendar.Participation;
import org.obm.sync.calendar.RecurrenceId;
import org.obm.sync.client.AbstractClientTest;
import org.obm.sync.client.impl.SyncClientAssert;
import org.w3c.dom.Document;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;

import fr.aliacom.obm.ToolBox;


public class AbstractEventSyncClientTest extends AbstractClientTest {

	private static String CALENDAR = "calendar";
	
	private AbstractEventSyncClient client;
	private AccessToken token;
	
	@Before
	public void setUpEventSyncClient() {
		token = ToolBox.mockAccessToken(control);
		client = new AbstractEventSyncClient("/calendar", new SyncClientAssert(), null, logger, null) {
			@Override
			protected Document execute(AccessToken token, String action, Multimap<String, Parameter> parameters) {
				return responder.execute(token, action, parameters);
			}
		};
	}
	
	@Test(expected=NotAllowedException.class)
	public void testCreateEventNotAllowed() throws Exception {
		testCreateEvent(NotAllowedException.class);
	}
	
	@Test(expected=EventAlreadyExistException.class)
	public void testCreateEventEventAlreadyExists() throws Exception {
		testCreateEvent(EventAlreadyExistException.class);
	}
	
	@SuppressWarnings("unchecked")
	private void testCreateEvent(Class<? extends Exception> exceptionClass) throws Exception {
		Event event = createEvent();
		Document document = mockErrorDocument(exceptionClass, null);
		
		expect(responder.execute(eq(token), eq("/calendar/createEvent"), isA(Multimap.class))).andReturn(document).once();
		control.replay();
		
		client.createEvent(token, CALENDAR, event, false, null);
	}
	
	@Test(expected=NotAllowedException.class)
	public void testRemoveEventByExtIdNotAllowed() throws Exception {
		testRemoveEventByExtId(NotAllowedException.class);
	}
	
	@SuppressWarnings("unchecked")
	private void testRemoveEventByExtId(Class<? extends Exception> exceptionClass) throws Exception {
		EventExtId extId = new EventExtId("ExtId");
		Document document = mockErrorDocument(exceptionClass, null);
		
		expect(responder.execute(eq(token), eq("/calendar/removeEventByExtId"), isA(Multimap.class))).andReturn(document).once();
		control.replay();
		
		client.removeEventByExtId(token, CALENDAR, extId, 0, false);
	}
	
	@Test(expected=NotAllowedException.class)
	public void testRemoveEventByIdNotAllowed() throws Exception {
		testRemoveEventById(NotAllowedException.class);
	}
	
	@Test(expected=EventNotFoundException.class)
	public void testRemoveEventByIdEventNotFound() throws Exception {
		testRemoveEventById(EventNotFoundException.class);
	}
	
	@SuppressWarnings("unchecked")
	private void testRemoveEventById(Class<? extends Exception> exceptionClass) throws Exception {
		EventObmId id = new EventObmId(1);
		Document document = mockErrorDocument(exceptionClass, null);
		
		expect(responder.execute(eq(token), eq("/calendar/removeEvent"), isA(Multimap.class))).andReturn(document).once();
		control.replay();
		
		client.removeEventById(token, CALENDAR, id, 0, false);
	}
	
	@Test(expected=NotAllowedException.class)
	public void testModifyEventNotAllowed() throws Exception {
		testModifyEvent(NotAllowedException.class);
	}
	
	@SuppressWarnings("unchecked")
	private void testModifyEvent(Class<? extends Exception> exceptionClass) throws Exception {
		Event event = createEvent();
		Document document = mockErrorDocument(exceptionClass, null);
		
		expect(responder.execute(eq(token), eq("/calendar/modifyEvent"), isA(Multimap.class))).andReturn(document).once();
		control.replay();
		
		client.modifyEvent(token, CALENDAR, event, false, false);
	}
	
	@Test(expected=NotAllowedException.class)
	public void testGetSyncInRangeNotAllowed() throws Exception {
		testGetSyncInRange(NotAllowedException.class);
	}
	
	@SuppressWarnings("unchecked")
	private void testGetSyncInRange(Class<? extends Exception> exceptionClass) throws Exception {
		Document document = mockErrorDocument(exceptionClass, null);
		
		expect(responder.execute(eq(token), eq("/calendar/getSyncInRange"), isA(Multimap.class))).andReturn(document).once();
		control.replay();
		
		client.getSyncInRange(token, CALENDAR, null, null);
	}
	
	@Test(expected=NotAllowedException.class)
	public void testGetSyncWithSortedChangesNotAllowed() throws Exception {
		testGetSyncWithSortedChanges(NotAllowedException.class);
	}
	
	@SuppressWarnings("unchecked")
	private void testGetSyncWithSortedChanges(Class<? extends Exception> exceptionClass) throws Exception {
		Document document = mockErrorDocument(exceptionClass, null);
		
		expect(responder.execute(eq(token), eq("/calendar/getSyncWithSortedChanges"), isA(Multimap.class))).andReturn(document).once();
		control.replay();
		
		client.getSyncWithSortedChanges(token, CALENDAR, null, null);
	}
	
	@Test(expected=NotAllowedException.class)
	public void testGetSyncNotAllowed() throws Exception {
		testGetSync(NotAllowedException.class);
	}
	
	@SuppressWarnings("unchecked")
	private void testGetSync(Class<? extends Exception> exceptionClass) throws Exception {
		Document document = mockErrorDocument(exceptionClass, null);
		
		expect(responder.execute(eq(token), eq("/calendar/getSync"), isA(Multimap.class))).andReturn(document).once();
		control.replay();
		
		client.getSync(token, CALENDAR, null);
	}
	
	@Test(expected=NotAllowedException.class)
	public void testGetSyncEventDateNotAllowed() throws Exception {
		testGetSyncEventDate(NotAllowedException.class);
	}
	
	@SuppressWarnings("unchecked")
	private void testGetSyncEventDate(Class<? extends Exception> exceptionClass) throws Exception {
		Document document = mockErrorDocument(exceptionClass, null);
		
		expect(responder.execute(eq(token), eq("/calendar/getSyncEventDate"), isA(Multimap.class))).andReturn(document).once();
		control.replay();
		
		client.getSyncEventDate(token, CALENDAR, null);
	}
	
	@Test(expected=NotAllowedException.class)
	public void testGetEventFromIdNotAllowed() throws Exception {
		testGetEventFromId(NotAllowedException.class);
	}
	
	@Test(expected=EventNotFoundException.class)
	public void testGetEventFromIdEventNotFound() throws Exception {
		testGetEventFromId(EventNotFoundException.class);
	}
	
	@SuppressWarnings("unchecked")
	private void testGetEventFromId(Class<? extends Exception> exceptionClass) throws Exception {
		EventObmId id = new EventObmId(1);
		Document document = mockErrorDocument(exceptionClass, null);
		
		expect(responder.execute(eq(token), eq("/calendar/getEventFromId"), isA(Multimap.class))).andReturn(document).once();
		control.replay();
		
		client.getEventFromId(token, CALENDAR, id);
	}
	
	@Test(expected=NotAllowedException.class)
	public void testGetEventObmIdFromExtIdNotAllowed() throws Exception {
		testGetEventObmIdFromExtId(NotAllowedException.class);
	}
	
	@Test(expected=EventNotFoundException.class)
	public void testGetEventObmIdFromExtIdEventNotFound() throws Exception {
		testGetEventObmIdFromExtId(EventNotFoundException.class);
	}
	
	@SuppressWarnings("unchecked")
	private void testGetEventObmIdFromExtId(Class<? extends Exception> exceptionClass) throws Exception {
		EventExtId extId = new EventExtId("ExtId");
		Document document = mockErrorDocument(exceptionClass, null);
		
		expect(responder.execute(eq(token), eq("/calendar/getEventObmIdFromExtId"), isA(Multimap.class))).andReturn(document).once();
		control.replay();
		
		client.getEventObmIdFromExtId(token, CALENDAR, extId);
	}
	
	@Test(expected=NotAllowedException.class)
	public void testGetEventTwinKeysNotAllowed() throws Exception {
		testGetEventTwinKeys(NotAllowedException.class);
	}
	
	@SuppressWarnings("unchecked")
	private void testGetEventTwinKeys(Class<? extends Exception> exceptionClass) throws Exception {
		Event event = createEvent();
		Document document = mockErrorDocument(exceptionClass, null);
		
		expect(responder.execute(eq(token), eq("/calendar/getEventTwinKeys"), isA(Multimap.class))).andReturn(document).once();
		control.replay();
		
		client.getEventTwinKeys(token, CALENDAR, event);
	}
	
	@Test(expected=NotAllowedException.class)
	public void testGetRefusedKeysNotAllowed() throws Exception {
		testGetRefusedKeys(NotAllowedException.class);
	}
	
	@SuppressWarnings("unchecked")
	private void testGetRefusedKeys(Class<? extends Exception> exceptionClass) throws Exception {
		Document document = mockErrorDocument(exceptionClass, null);
		
		expect(responder.execute(eq(token), eq("/calendar/getRefusedKeys"), isA(Multimap.class))).andReturn(document).once();
		control.replay();
		
		client.getRefusedKeys(token, CALENDAR, null);
	}
	
	@Test(expected=NotAllowedException.class)
	public void testGetEventFromExtIdNotAllowed() throws Exception {
		testGetEventFromExtId(NotAllowedException.class);
	}
	
	@Test(expected=EventNotFoundException.class)
	public void testGetEventFromExtIdEventNotFound() throws Exception {
		testGetEventFromExtId(EventNotFoundException.class);
	}
	
	@SuppressWarnings("unchecked")
	private void testGetEventFromExtId(Class<? extends Exception> exceptionClass) throws Exception {
		EventExtId extId = new EventExtId("ExtId");
		Document document = mockErrorDocument(exceptionClass, null);
		
		expect(responder.execute(eq(token), eq("/calendar/getEventFromExtId"), isA(Multimap.class))).andReturn(document).once();
		control.replay();
		
		client.getEventFromExtId(token, CALENDAR, extId);
	}
	
	@Test(expected=NotAllowedException.class)
	public void testGetListEventsFromIntervalDateNotAllowed() throws Exception {
		testGetListEventsFromIntervalDate(NotAllowedException.class);
	}
	
	@SuppressWarnings("unchecked")
	private void testGetListEventsFromIntervalDate(Class<? extends Exception> exceptionClass) throws Exception {
		Date start = new Date(123456789), end = new Date(123456780);
		Document document = mockErrorDocument(exceptionClass, null);
		
		expect(responder.execute(eq(token), eq("/calendar/getListEventsFromIntervalDate"), isA(Multimap.class))).andReturn(document).once();
		control.replay();
		
		client.getListEventsFromIntervalDate(token, CALENDAR, start, end);
	}
	
	@Test(expected=NotAllowedException.class)
	public void testGetLastUpdateNotAllowed() throws Exception {
		testGetLastUpdate(NotAllowedException.class);
	}
	
	@SuppressWarnings("unchecked")
	private void testGetLastUpdate(Class<? extends Exception> exceptionClass) throws Exception {
		Document document = mockErrorDocument(exceptionClass, null);
		
		expect(responder.execute(eq(token), eq("/calendar/getLastUpdate"), isA(Multimap.class))).andReturn(document).once();
		control.replay();
		
		client.getLastUpdate(token, CALENDAR);
	}
	
	@Test(expected=NotAllowedException.class)
	public void testIsWritableCalendarNotAllowed() throws Exception {
		testIsWritableCalendar(NotAllowedException.class);
	}
	
	@SuppressWarnings("unchecked")
	private void testIsWritableCalendar(Class<? extends Exception> exceptionClass) throws Exception {
		Document document = mockErrorDocument(exceptionClass, null);
		
		expect(responder.execute(eq(token), eq("/calendar/isWritableCalendar"), isA(Multimap.class))).andReturn(document).once();
		control.replay();
		
		client.isWritableCalendar(token, CALENDAR);
	}
	
	@Test(expected=NotAllowedException.class)
	public void testChangeParticipationStateNotAllowed() throws Exception {
		testChangeParticipationState(NotAllowedException.class);
	}
	
	@SuppressWarnings("unchecked")
	private void testChangeParticipationState(Class<? extends Exception> exceptionClass) throws Exception {
		EventExtId extId = new EventExtId("ExtId");
		Document document = mockErrorDocument(exceptionClass, null);
		
		expect(responder.execute(eq(token), eq("/calendar/changeParticipationState"), isA(Multimap.class))).andReturn(document).once();
		control.replay();
		
		client.changeParticipationState(token, CALENDAR, extId, Participation.accepted(), 0, false);
	}
	
	@Test(expected=NotAllowedException.class)
	public void testChangeParticipationStateRecNotAllowed() throws Exception {
		testChangeParticipationStateRec(NotAllowedException.class);
	}
	
	@SuppressWarnings("unchecked")
	private void testChangeParticipationStateRec(Class<? extends Exception> exceptionClass) throws Exception {
		EventExtId extId = new EventExtId("ExtId");
		RecurrenceId recId = new RecurrenceId("RecId");
		Document document = mockErrorDocument(exceptionClass, null);
		
		expect(responder.execute(eq(token), eq("/calendar/changeParticipationState"), isA(Multimap.class))).andReturn(document).once();
		control.replay();
		
		client.changeParticipationState(token, CALENDAR, extId, recId, Participation.accepted(), 0, false);
	}
	
	@Test(expected=NotAllowedException.class)
	public void testImportICalendarNotAllowed() throws Exception {
		testImportICalendar(NotAllowedException.class);
	}
	
	@SuppressWarnings("unchecked")
	private void testImportICalendar(Class<? extends Exception> exceptionClass) throws Exception {
		String ics = "ICS";
		Document document = mockErrorDocument(exceptionClass, null);
		
		expect(responder.execute(eq(token), eq("/calendar/importICalendar"), isA(Multimap.class))).andReturn(document).once();
		control.replay();
		
		client.importICalendar(token, CALENDAR, ics, null);
	}
	
	@Test(expected=NotAllowedException.class)
	public void testPurgeNotAllowed() throws Exception {
		testPurge(NotAllowedException.class);
	}

	@Test(expected = NotAllowedException.class)
	public void testStoreEventNotAllowed() throws Exception {
		storeEvent(NotAllowedException.class);
	}

	@Test(expected = ServerFault.class)
	public void testStoreEventServerFault() throws Exception {
		storeEvent(ServerFault.class);
	}

	@SuppressWarnings("unchecked")
	private void storeEvent(Class<? extends Exception> exceptionClass) throws ServerFault, NotAllowedException {
		Event event = createEvent();
		Document document = mockErrorDocument(exceptionClass, null);

		expect(responder.execute(eq(token), eq("/calendar/storeEvent"), isA(Multimap.class))).andReturn(document).once();
		control.replay();

		client.storeEvent(token, CALENDAR, event, false, null);
	}

	@SuppressWarnings("unchecked")
	private void testPurge(Class<? extends Exception> exceptionClass) throws Exception {
		Document document = mockErrorDocument(exceptionClass, null);
		
		expect(responder.execute(eq(token), eq("/calendar/purge"), isA(Multimap.class))).andReturn(document).once();
		control.replay();
		
		client.purge(token, CALENDAR);
	}

	@Test
	public void testListCalendarsSendsOnlySid() throws Exception {
		Document doc = mockEmptyCalendarInfosDocument();
		Multimap<String, Parameter> params = ImmutableListMultimap.<String, Parameter> of("sid", new StringParameter("sessionId"));

		expect(responder.execute(token, "/calendar/listCalendars", params)).andReturn(doc).once();
		control.replay();

		client.listCalendars(token);

		control.verify();
	}

	@Test
	public void testListCalendarsWithLimitAndOffsetSendsLimitAndOffset() throws Exception {
		Document doc = mockEmptyCalendarInfosDocument();
		Multimap<String, Parameter> params = ImmutableListMultimap.<String, Parameter> of("sid", new StringParameter("sessionId"), "limit", new IntegerParameter(10), "offset", new IntegerParameter(5));

		expect(responder.execute(token, "/calendar/listCalendars", params)).andReturn(doc).once();
		control.replay();

		client.listCalendars(token, 10, 5);

		control.verify();
	}

	@Test
	public void testListCalendarsWithNoLimitSendsSidOnly() throws Exception {
		Document doc = mockEmptyCalendarInfosDocument();
		Multimap<String, Parameter> params = ImmutableListMultimap.<String, Parameter> of("sid", new StringParameter("sessionId"));

		expect(responder.execute(token, "/calendar/listCalendars", params)).andReturn(doc).once();
		control.replay();

		client.listCalendars(token, null, 5);

		control.verify();
	}

	@Test
	public void testListCalendarsWithPatternSendsPatternAndSid() throws Exception {
		Document doc = mockEmptyCalendarInfosDocument();
		Multimap<String, Parameter> params = ImmutableListMultimap.<String, Parameter> of("sid", new StringParameter("sessionId"), "pattern", new StringParameter("p"));

		expect(responder.execute(token, "/calendar/listCalendars", params)).andReturn(doc).once();
		control.replay();

		client.listCalendars(token, null, 5, "p");

		control.verify();
	}

	@Test
	public void testListCalendarsWithPatternAndLimitAndOffsetSendsAll() throws Exception {
		Document doc = mockEmptyCalendarInfosDocument();
		Multimap<String, Parameter> params = ImmutableListMultimap.<String, Parameter> of("sid", new StringParameter("sessionId"), "limit", new IntegerParameter(10), "offset", new IntegerParameter(5), "pattern", new StringParameter("p"));

		expect(responder.execute(token, "/calendar/listCalendars", params)).andReturn(doc).once();
		control.replay();

		client.listCalendars(token, 10, 5, "p");

		control.verify();
	}

	@Test
	public void testListResourcesSendsOnlySid() throws Exception {
		Document doc = mockEmptyResourceInfosDocument();
		Multimap<String, Parameter> params = ImmutableListMultimap.<String, Parameter> of("sid", new StringParameter("sessionId"));

		expect(responder.execute(token, "/calendar/listResources", params)).andReturn(doc).once();
		control.replay();

		client.listResources(token);

		control.verify();
	}

	@Test
	public void testListResourcesWithLimitAndOffsetSendsLimitAndOffset() throws Exception {
		Document doc = mockEmptyResourceInfosDocument();
		Multimap<String, Parameter> params = ImmutableListMultimap.<String, Parameter> of("sid", new StringParameter("sessionId"), "limit", new IntegerParameter(10), "offset", new IntegerParameter(5));

		expect(responder.execute(token, "/calendar/listResources", params)).andReturn(doc).once();
		control.replay();

		client.listResources(token, 10, 5);

		control.verify();
	}

	@Test
	public void testListResourcesWithNoLimitSendsSidOnly() throws Exception {
		Document doc = mockEmptyResourceInfosDocument();
		Multimap<String, Parameter> params = ImmutableListMultimap.<String, Parameter> of("sid", new StringParameter("sessionId"));

		expect(responder.execute(token, "/calendar/listResources", params)).andReturn(doc).once();
		control.replay();

		client.listResources(token, null, 5);

		control.verify();
	}

	@Test
	public void testListResourcesWithPatternSendsPatternAndSid() throws Exception {
		Document doc = mockEmptyResourceInfosDocument();
		Multimap<String, Parameter> params = ImmutableListMultimap.<String, Parameter> of("sid", new StringParameter("sessionId"), "pattern", new StringParameter("p"));

		expect(responder.execute(token, "/calendar/listResources", params)).andReturn(doc).once();
		control.replay();

		client.listResources(token, null, 5, "p");

		control.verify();
	}

	@Test
	public void testListResourcesWithPatternAndLimitAndOffsetSendsAll() throws Exception {
		Document doc = mockEmptyResourceInfosDocument();
		Multimap<String, Parameter> params = ImmutableListMultimap.<String, Parameter> of("sid", new StringParameter("sessionId"), "limit", new IntegerParameter(10), "offset", new IntegerParameter(5), "pattern", new StringParameter("p"));

		expect(responder.execute(token, "/calendar/listResources", params)).andReturn(doc).once();
		control.replay();

		client.listResources(token, 10, 5, "p");

		control.verify();
	}

}
