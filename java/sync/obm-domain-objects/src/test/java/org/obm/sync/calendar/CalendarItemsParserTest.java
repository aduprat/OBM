/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2011-2014  Linagora
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
package org.obm.sync.calendar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.obm.push.utils.DateUtils.dateUTC;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.xml.parsers.FactoryConfigurationError;

import org.junit.Before;
import org.junit.Test;
import org.obm.push.utils.DOMUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.google.common.collect.Iterables;


public class CalendarItemsParserTest {
	
	private CalendarItemsParser parser;
	
	@Before
	public void initCalendarParser(){
		parser = new CalendarItemsParser();
	}

	@Test
	public void testParseInternalEventTrue() throws SAXException, IOException, FactoryConfigurationError {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
		"<event allDay=\"false\" id=\"\" type=\"VEVENT\" isInternal=\"true\" sequence=\"0\" xmlns=\"http://www.obm.org/xsd/sync/event.xsd\">" +
		"<extId>2bf7db53-8820-4fe5-9a78-acc6d3262149</extId>" +
		"<opacity>OPAQUE</opacity>" +
		"<title>fake rdv</title>" +
		"<owner>john@do.fr</owner>" +
		"<tz>Europe/Paris</tz>" +
		"<date>1295258400000</date>" +
		"<duration>3600</duration>" +
		"<location>tlse</location>" +
		"<alert>60</alert>" +
		"<priority>0</priority>" +
		"<privacy>0</privacy>" +
		"<attendees>" +
		"<attendee displayName=\"John Do\" email=\"john@do.fr\" percent=\"0\" required=\"CHAIR\" state=\"NEEDS-ACTION\" isOrganizer=\"true\"/>" +
		"<attendee displayName=\"noIn TheDatabase\" email=\"notin@mydb.com\" percent=\"0\" required=\"OPT\" state=\"ACCEPTED\" isOrganizer=\"false\"/>" +
		"<attendee displayName=\"noIn TheDatabase2\" email=\"notin2@mydb.com\" percent=\"0\" required=\"OPT\" state=\"ACCEPTED\"/>" +
		"</attendees><recurrence days=\"\" freq=\"1\" kind=\"daily\">" +
		"<exceptions>" +
		"<exception>1295258400000</exception>" +
		"</exceptions><eventExceptions/>" +
		"</recurrence>" +
		"</event>";
		Document doc = DOMUtils.parse(new ByteArrayInputStream(xml.getBytes()));
		Event ev = parser.parseEvent(doc.getDocumentElement());

		Calendar cal = new GregorianCalendar();
		cal.setTimeInMillis(1295258400000L);
		
		assertThat(ev.isInternalEvent()).isTrue();
		assertThat(ev.getStartDate()).isEqualTo(cal.getTime());
		assertThat(ev.getExtId()).isEqualTo(new EventExtId("2bf7db53-8820-4fe5-9a78-acc6d3262149"));
		assertThat(ev.getTitle()).isEqualTo("fake rdv");
		assertThat(ev.getOwner()).isEqualTo("john@do.fr");
		assertThat(ev.getOwnerDisplayName()).isEqualTo("john@do.fr");
		assertThat(ev.getDuration()).isEqualTo(3600);
		assertThat(ev.getLocation()).isEqualTo("tlse");
		assertThat(ev.getAlert()).isEqualTo(60);
		assertThat(ev.getEndDate().getTime()).isEqualTo(1295262000000L);
		
		Attendee at = UnidentifiedAttendee
				.builder()
				.displayName("John Do")
				.email("john@do.fr")
				.participation(Participation.needsAction())
				.participationRole(ParticipationRole.CHAIR)
				.asOrganizer()
				.build();
		
		assertThat(ev.getAttendees()).contains(at);
		
		at = UnidentifiedAttendee
				.builder()
				.displayName("noIn TheDatabase")
				.email("notin@mydb.com")
				.participation(Participation.accepted())
				.participationRole(ParticipationRole.OPT)
				.asOrganizer()
				.build();
		
		assertThat(ev.getAttendees()).contains(at);
		
		at = UnidentifiedAttendee
				.builder()
				.displayName("noIn TheDatabase2")
				.email("notin2@mydb.com")
				.participation(Participation.accepted())
				.participationRole(ParticipationRole.OPT)
				.asOrganizer()
				.build();
		
		assertThat(ev.getAttendees()).contains(at);
		
		assertThat(ev.getRecurrence()).isNotNull();
		assertThat(ev.getRecurrence().getKind()).isEqualTo(RecurrenceKind.daily);
		assertThat(ev.getRecurrence().getFrequence()).isEqualTo(1);
		assertThat(ev.getRecurrence().getExceptions()).containsOnly(ev.getStartDate());
		assertThat(ev.getRecurrence().getEnd()).isNull();
	}
	
	@Test
	public void testParseInternalEventFalse() throws SAXException, IOException, FactoryConfigurationError {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
		"<event allDay=\"false\" id=\"\" type=\"VEVENT\" isInternal=\"false\" sequence=\"0\" xmlns=\"http://www.obm.org/xsd/sync/event.xsd\">" +
		"<extId>2bf7db53-8820-4fe5-9a78-acc6d3262149</extId>" +
		"<opacity>OPAQUE</opacity>" +
		"<title>fake rdv</title>" +
		"<owner>john@do.fr</owner>" +
		"<tz>Europe/Paris</tz>" +
		"<date>1295258400000</date>" +
		"<duration>3600</duration>" +
		"<location>tlse</location>" +
		"<alert>60</alert>" +
		"<priority>0</priority>" +
		"<privacy>0</privacy>" +
		"<attendees>" +
		"<attendee displayName=\"John Do\" email=\"john@do.fr\" percent=\"0\" required=\"CHAIR\" state=\"NEEDS-ACTION\" isOrganizer=\"true\"/>" +
		"<attendee displayName=\"noIn TheDatabase\" email=\"notin@mydb.com\" percent=\"0\" required=\"OPT\" state=\"ACCEPTED\" isOrganizer=\"false\"/>" +
		"<attendee displayName=\"noIn TheDatabase2\" email=\"notin2@mydb.com\" percent=\"0\" required=\"OPT\" state=\"ACCEPTED\"/>" +
		"</attendees><recurrence days=\"\" freq=\"1\" kind=\"daily\">" +
		"<exceptions>" +
		"<exception>1295258400000</exception>" +
		"</exceptions><eventExceptions/>" +
		"</recurrence>" +
		"</event>";
		Document doc = DOMUtils.parse(new ByteArrayInputStream(xml.getBytes()));
		Event ev = parser.parseEvent(doc.getDocumentElement());

		Calendar cal = new GregorianCalendar();
		cal.setTimeInMillis(1295258400000L);
		
		assertThat(ev.isInternalEvent()).isFalse();
		assertThat(ev.getStartDate()).isEqualTo(cal.getTime());
		assertThat(ev.getExtId()).isEqualTo(new EventExtId("2bf7db53-8820-4fe5-9a78-acc6d3262149"));
		assertThat(ev.getTitle()).isEqualTo("fake rdv");
		assertThat(ev.getOwner()).isEqualTo("john@do.fr");
		assertThat(ev.getOwnerDisplayName()).isEqualTo("john@do.fr");
		assertThat(ev.getDuration()).isEqualTo(3600);
		assertThat(ev.getLocation()).isEqualTo("tlse");
		assertThat(ev.getAlert()).isEqualTo(60);
		assertThat(ev.getEndDate().getTime()).isEqualTo(1295262000000L);
		
		Attendee at = UnidentifiedAttendee
				.builder()
				.displayName("John Do")
				.email("john@do.fr")
				.participation(Participation.needsAction())
				.participationRole(ParticipationRole.CHAIR)
				.asOrganizer()
				.build();
		
		assertThat(ev.getAttendees()).contains(at);
		
		at = UnidentifiedAttendee
				.builder()
				.displayName("noIn TheDatabase")
				.email("notin@mydb.com")
				.participation(Participation.accepted())
				.participationRole(ParticipationRole.OPT)
				.asOrganizer()
				.build();
		
		assertThat(ev.getAttendees()).contains(at);
		
		at = UnidentifiedAttendee
				.builder()
				.displayName("noIn TheDatabase2")
				.email("notin2@mydb.com")
				.participation(Participation.accepted())
				.participationRole(ParticipationRole.OPT)
				.asOrganizer()
				.build();
		
		assertThat(ev.getAttendees()).contains(at);
		
		assertThat(ev.getRecurrence()).isNotNull();
		assertThat(ev.getRecurrence().getKind()).isEqualTo(RecurrenceKind.daily);
		assertThat(ev.getRecurrence().getFrequence()).isEqualTo(1);
		assertThat(ev.getRecurrence().getExceptions()).containsOnly(ev.getStartDate());
		assertThat(ev.getRecurrence().getEnd()).isNull();
	}
	
	@Test
	public void testParseExternalEvent() throws SAXException, IOException, FactoryConfigurationError {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
		"<event allDay=\"false\" id=\"\" type=\"VEVENT\" isInternal=\"false\" sequence=\"0\" xmlns=\"http://www.obm.org/xsd/sync/event.xsd\">" +
		"<timeupdate>1292580000000</timeupdate>" +
		"<timecreate>1289988000000</timecreate>" +
		"<extId>2bf7db53-8820-4fe5-9a78-acc6d3262149</extId>" +
		"<opacity>OPAQUE</opacity>" +
		"<title>fake rdv</title>" +
		"<owner>john@do.fr</owner>" +
		"<tz>Europe/Paris</tz>" +
		"<date>1295258400000</date>" +
		"<duration>3600</duration>" +
		"<location>tlse</location>" +
		"<alert>60</alert>" +
		"<priority>0</priority>" +
		"<privacy>0</privacy>" +
		"<attendees>" +
		"<attendee displayName=\"John Do\" email=\"john@do.fr\" percent=\"0\" required=\"CHAIR\" state=\"NEEDS-ACTION\" isOrganizer=\"true\"/>" +
		"<attendee displayName=\"noIn TheDatabase\" email=\"notin@mydb.com\" percent=\"0\" required=\"OPT\" state=\"ACCEPTED\"/>" +
		"</attendees><recurrence days=\"\" freq=\"1\" kind=\"daily\">" +
		"<exceptions>" +
		"<exception>1295258400000</exception>" +
		"</exceptions><eventExceptions/>" +
		"</recurrence>" +
		"</event>";
		
		Document doc = DOMUtils.parse(new ByteArrayInputStream(xml.getBytes()));
		Event ev = parser.parseEvent(doc.getDocumentElement());

		Calendar cal = new GregorianCalendar();
		cal.setTimeInMillis(1295258400000L);
		
		assertThat(ev.isInternalEvent()).isFalse();
		assertThat(ev.getStartDate()).isEqualTo(cal.getTime());
		assertThat(ev.getExtId()).isEqualTo(new EventExtId("2bf7db53-8820-4fe5-9a78-acc6d3262149"));
		assertThat(ev.getTitle()).isEqualTo("fake rdv");
		assertThat(ev.getOwner()).isEqualTo("john@do.fr");
		assertThat(ev.getOwnerDisplayName()).isEqualTo("john@do.fr");
		assertThat(ev.getDuration()).isEqualTo(3600);
		assertThat(ev.getLocation()).isEqualTo("tlse");
		assertThat(ev.getAlert()).isEqualTo(60);
		assertThat(ev.getEndDate().getTime()).isEqualTo(1295262000000L);
		
		Attendee at = UnidentifiedAttendee
				.builder()
				.displayName("John Do")
				.email("john@do.fr")
				.participation(Participation.needsAction())
				.participationRole(ParticipationRole.CHAIR)
				.asOrganizer()
				.build();
		
		assertThat(ev.getAttendees()).contains(at);
		
		at = UnidentifiedAttendee
				.builder()
				.displayName("noIn TheDatabase")
				.email("notin@mydb.com")
				.participation(Participation.accepted())
				.participationRole(ParticipationRole.OPT)
				.asOrganizer()
				.build();

		assertThat(ev.getAttendees()).contains(at);
		
		assertThat(ev.getRecurrence()).isNotNull();
		assertThat(ev.getRecurrence().getKind()).isEqualTo(RecurrenceKind.daily);
		assertThat(ev.getRecurrence().getFrequence()).isEqualTo(1);
		assertThat(ev.getRecurrence().getExceptions()).containsOnly(ev.getStartDate());
		assertThat(ev.getRecurrence().getEnd()).isNull();
		
		assertThat(ev.getTimeCreate().getTime()).isEqualTo(1289988000000L);
		assertThat(ev.getTimeUpdate().getTime()).isEqualTo(1292580000000L);
	}

	@Test
	public void testParseExternalEventWithEmptyExtId() throws SAXException, IOException, FactoryConfigurationError {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
		"<event allDay=\"false\" id=\"\" type=\"VEVENT\" isInternal=\"false\" sequence=\"0\" xmlns=\"http://www.obm.org/xsd/sync/event.xsd\">" +
		"<timeupdate>1292580000000</timeupdate>" +
		"<timecreate>1289988000000</timecreate>" +
		"<extId></extId>" +
		"<opacity>OPAQUE</opacity>" +
		"<title>fake rdv</title>" +
		"<owner>john@do.fr</owner>" +
		"<tz>Europe/Paris</tz>" +
		"<date>1295258400000</date>" +
		"<duration>3600</duration>" +
		"<location>tlse</location>" +
		"<alert>60</alert>" +
		"<priority>0</priority>" +
		"<privacy>0</privacy>" +
		"<attendees/>" +
		"</event>";
		
		Document doc = DOMUtils.parse(new ByteArrayInputStream(xml.getBytes()));
		Event ev = parser.parseEvent(doc.getDocumentElement());

		Calendar cal = new GregorianCalendar();
		cal.setTimeInMillis(1295258400000L);
		
		
		assertThat(ev.isInternalEvent()).isFalse();
		assertThat(ev.getStartDate()).isEqualTo(cal.getTime());
		assertThat(ev.getExtId().getExtId()).isNull();
		assertThat(ev.getTitle()).isEqualTo("fake rdv");
		assertThat(ev.getOwner()).isEqualTo("john@do.fr");
		assertThat(ev.getOwnerDisplayName()).isEqualTo("john@do.fr");
		assertThat(ev.getDuration()).isEqualTo(3600);
		assertThat(ev.getLocation()).isEqualTo("tlse");
		assertThat(ev.getAlert()).isEqualTo(60);
		assertThat(ev.getEndDate().getTime()).isEqualTo(1295262000000L);
		assertThat(ev.getTimeCreate().getTime()).isEqualTo(1289988000000L);
		assertThat(ev.getTimeUpdate().getTime()).isEqualTo(1292580000000L);
	}
	
	@Test
	public void testParseNullRecurrence() throws SAXException, IOException, FactoryConfigurationError {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
		"<event allDay=\"false\" id=\"\" type=\"VEVENT\" sequence=\"0\" xmlns=\"http://www.obm.org/xsd/sync/event.xsd\">" +
		"<timeupdate>1292580000000</timeupdate>" +
		"<timecreate>1289988000000</timecreate>" +
		"<extId>2bf7db53-8820-4fe5-9a78-acc6d3262149</extId>" +
		"<opacity>OPAQUE</opacity>" +
		"<title>fake rdv</title>" +
		"<owner>john@do.fr</owner>" +
		"<tz>Europe/Paris</tz>" +
		"<date>1295258400000</date>" +
		"<duration>3600</duration>" +
		"<location>tlse</location>" +
		"<alert>60</alert>" +
		"<priority>0</priority>" +
		"<privacy>0</privacy>" +
		"<attendees>" +
		"<attendee displayName=\"John Do\" email=\"john@do.fr\" percent=\"0\" required=\"CHAIR\" state=\"NEEDS-ACTION\" isOrganizer=\"true\"/>" +
		"</attendees><recurrence days=\"\" freq=\"1\">" +
		"<exceptions>" +
		"<exception>1295258400000</exception>" +
		"</exceptions><eventExceptions/>" +
		"</recurrence>" +
		"</event>";
		
		Document doc = DOMUtils.parse(new ByteArrayInputStream(xml.getBytes()));
		Event ev = parser.parseEvent(doc.getDocumentElement());
		
		assertThat(ev.getRecurrence().getKind()).isEqualTo(RecurrenceKind.none);
		assertThat(ev.getRecurrence().getDays()).isEqualTo(new RecurrenceDays());
	}

	@Test
	public void testEventException() throws SAXException, IOException {
		String xml ="<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
			"<event id=\"316\" isInternal=\"true\" sequence=\"1\" allDay=\"false\" type=\"VEVENT\">" +
			"  <extId>a7db3cd5-adf3-42f4-95f3-d0a7a9c01aa3</extId>" +
			"  <owner>test2</owner>" +
			"  <priority>2</priority>" +
			"  <privacy>0</privacy>" +
			"  <date>1313994600000</date>" +
			"  <duration>3600</duration>" +
			"  <title>New Event from TBird</title>" +
			"  <alert>-1</alert>" +
			"  <opacity>OPAQUE</opacity>" +
			"  <ownerEmail>test2@par.lng</ownerEmail>" +
			"  <attendees>" +
			"    <attendee email=\"test2@par.lng\" displayName=\"test2 test2\" state=\"NEEDS-ACTION\" required=\"REQ\" isOrganizer=\"true\"/>" +
			"  </attendees>" +
			"  <recurrence kind=\"daily\" freq=\"1\" days=\"\">" +
			"    <eventExceptions>" +
			"      <eventException allDay=\"false\" type=\"VEVENT\">" +
			"        <recurrenceId>1314081000000</recurrenceId>" +
			"        <owner>test2</owner>" +
			"        <priority>2</priority>" +
			"        <privacy>0</privacy>" +
			"        <date>1314077400000</date>" +
			"        <duration>3600</duration>" +
			"        <title>New Event from TBird</title>" +
			"        <alert>-1</alert>" +
			"        <opacity>OPAQUE</opacity>" +
			"        <ownerEmail>test2@par.lng</ownerEmail>" +
			"        <attendees>" +
			"          <attendee email=\"test@par.lng\" displayName=\"test test\" state=\"NEEDS-ACTION\" required=\"OPT\"/>" +
			"          <attendee email=\"test2@par.lng\" displayName=\"test2 test2\" state=\"NEEDS-ACTION\" required=\"REQ\" isOrganizer=\"true\"/>" +
			"        </attendees>" +
			"        <recurrence kind=\"none\" freq=\"1\" days=\"\" end=\"1314177107089\"/>" +
			"      </eventException>" +
			"    </eventExceptions>" +
			"  </recurrence>" +
			"</event>";
		Document doc = DOMUtils.parse(new ByteArrayInputStream(xml.getBytes()));
		Event ev = parser.parseEvent(doc.getDocumentElement());

		assertThat(ev.getRecurrence().getEventExceptions()).hasSize(1);

		Event evEx = Iterables.getOnlyElement(ev.getRecurrence().getEventExceptions());
		assertThat(evEx.getObmId()).isNull();
		assertThat(evEx.getExtId()).isEqualTo(new EventExtId("a7db3cd5-adf3-42f4-95f3-d0a7a9c01aa3"));
		assertThat(evEx.isAllday()).isFalse();
		assertThat(evEx.getType()).isEqualTo(EventType.VEVENT);
		assertThat(evEx.getRecurrenceId().getTime()).isEqualTo(1314081000000L);
		assertThat(evEx.getOwner()).isEqualTo("test2");
		assertThat(evEx.getOwnerDisplayName()).isEqualTo("test2");
		assertThat(evEx.getPriority()).isEqualTo(2);
		assertThat(evEx.getPrivacy()).isEqualTo(EventPrivacy.PUBLIC);
		assertThat(evEx.getStartDate().getTime()).isEqualTo(1314077400000L);
		assertThat(evEx.getDuration()).isEqualTo(3600);
		assertThat(evEx.getAlert()).isNull();
		assertThat(evEx.getOpacity()).isEqualTo(EventOpacity.OPAQUE);
		assertThat(evEx.getOwnerEmail()).isEqualTo("test2@par.lng");
		assertThat(evEx.getSequence()).isEqualTo(1);
		assertThat(evEx.getAttendees().size()).isEqualTo(2);
		assertThat(ev.getRecurrence().getDays()).isEqualTo(new RecurrenceDays());
	}


	@Test
	public void testParseResourceInfo() throws SAXException, IOException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<resourceInfo>" +
				"    <id>42</id>" +
				"    <name>myresource</name>" +
				"    <mail>res-42@somedomain.com</mail>" +
				"    <description>mydescription</description>" +
				"    <read>true</read>" +
				"    <write>false</write>" +
				"</resourceInfo>";
		Document doc = DOMUtils.parse(new ByteArrayInputStream(xml.getBytes()));
		ResourceInfo resourceInfo = parser.parseResourceInfo(doc.getDocumentElement());
		assertThat(resourceInfo.getId()).isEqualTo(42);
		assertThat(resourceInfo.getName()).isEqualTo("myresource");
		assertThat("mydescription").isEqualTo(resourceInfo.getDescription());
		assertThat("res-42@somedomain.com").isEqualTo(resourceInfo.getMail());
		assertThat(resourceInfo.isRead()).isTrue();
		assertThat(resourceInfo.isWrite()).isFalse();
	}

	@Test
	public void testParseResourceInfoWithoutDescription() throws SAXException, IOException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<resourceInfo>" +
				"    <id>42</id>" +
				"    <name>myresource</name>" +
				"    <mail>res-42@somedomain.com</mail>" +
				"    <read>true</read>" +
				"    <write>false</write>" +
				"</resourceInfo>";
		Document doc = DOMUtils.parse(new ByteArrayInputStream(xml.getBytes()));
		ResourceInfo resourceInfo = parser.parseResourceInfo(doc.getDocumentElement());
		assertThat(42).isEqualTo(resourceInfo.getId());
		assertThat("myresource").isEqualTo(resourceInfo.getName());
		assertThat("res-42@somedomain.com").isEqualTo(resourceInfo.getMail());
		assertThat(resourceInfo.getDescription()).isNull();
		assertThat(resourceInfo.isRead()).isTrue();
		assertThat(resourceInfo.isWrite()).isFalse();
	}

@Test
	public void testParseRecurrenceWhenNone() throws SAXException, IOException {
		Document doc = DOMUtils.parse("<recurrence kind=\"none\"/>");

		Event event = new Event();
		parser.parseRecurrence(event, doc.getDocumentElement());
		
		assertThat(event.getRecurrence()).isNotNull();
		assertThat(event.getRecurrence().getExceptions()).isEmpty();
		assertThat(event.getRecurrence().getEventExceptions()).isEmpty();
		assertThat(event.getRecurrence().getDays()).isEmpty();
		assertThat(event.getRecurrence().frequencyIsSpecified()).isFalse();
		assertThat(event.getRecurrence().getKind()).isEqualTo(RecurrenceKind.none);
		assertThat(event.getRecurrence().getEnd()).isNull();
	}

	@Test
	public void testParseRecurrenceWhenWeeklyNoEnd() throws SAXException, IOException {
		Document doc = DOMUtils.parse(
			"<recurrence days=\"0001000\" freq=\"1\" kind=\"weekly\">" +
				"<exceptions/>" +
				"<eventExceptions/>" +
			"</recurrence>");
		
		Event event = new Event();
		parser.parseRecurrence(event, doc.getDocumentElement());
		
		assertThat(event.getRecurrence()).isNotNull();
		assertThat(event.getRecurrence().getExceptions()).isEmpty();
		assertThat(event.getRecurrence().getEventExceptions()).isEmpty();
		assertThat(event.getRecurrence().getDays()).containsOnly(RecurrenceDay.Wednesday);
		assertThat(event.getRecurrence().frequencyIsSpecified()).isTrue();
		assertThat(event.getRecurrence().getFrequence()).isEqualTo(1);
		assertThat(event.getRecurrence().getKind()).isEqualTo(RecurrenceKind.weekly);
		assertThat(event.getRecurrence().getEnd()).isNull();
	}

	@Test
	public void testParseRecurrenceWhenDailyWithEnd() throws SAXException, IOException {
		Document doc = DOMUtils.parse(
			"<recurrence days=\"\" end=\"1340708400000\" freq=\"2\" kind=\"daily\">" +
				"<exceptions/>" +
				"<eventExceptions/>" +
			"</recurrence>");
		
		Event event = new Event();
		parser.parseRecurrence(event, doc.getDocumentElement());
		
		assertThat(event.getRecurrence()).isNotNull();
		assertThat(event.getRecurrence().getExceptions()).isEmpty();
		assertThat(event.getRecurrence().getEventExceptions()).isEmpty();
		assertThat(event.getRecurrence().getDays()).isEmpty();
		assertThat(event.getRecurrence().frequencyIsSpecified()).isTrue();
		assertThat(event.getRecurrence().getFrequence()).isEqualTo(2);
		assertThat(event.getRecurrence().getKind()).isEqualTo(RecurrenceKind.daily);
		assertThat(event.getRecurrence().getEnd()).isEqualTo(dateUTC("2012-06-26T11:00:00"));
	}

	@Test
	public void testParseAnonymizedEvent() throws SAXException, IOException, FactoryConfigurationError {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
		"<event anonymized=\"true\" allDay=\"false\" id=\"\" type=\"VEVENT\" isInternal=\"true\" sequence=\"0\" xmlns=\"http://www.obm.org/xsd/sync/event.xsd\">" +
		"<extId>2bf7db53-8820-4fe5-9a78-acc6d3262149</extId>" +
		"<title>fake rdv</title>" +
		"<owner>john@do.fr</owner>" +
		"<date>1295258400000</date>" +
		"<duration>3600</duration>" +
		"<attendees>" +
		"<attendee displayName=\"John Do\" email=\"john@do.fr\" percent=\"0\" required=\"CHAIR\" state=\"NEEDS-ACTION\" isOrganizer=\"true\"/>" +
		"</attendees>" +
		"</event>";
		Document doc = DOMUtils.parse(new ByteArrayInputStream(xml.getBytes()));
		Event ev = parser.parseEvent(doc.getDocumentElement());

		assertThat(ev.isAnonymized()).isTrue();
	}

	@Test
	public void testParseNotAnonymizedEvent() throws SAXException, IOException, FactoryConfigurationError {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
		"<event anonymized=\"false\" allDay=\"false\" id=\"\" type=\"VEVENT\" isInternal=\"true\" sequence=\"0\" xmlns=\"http://www.obm.org/xsd/sync/event.xsd\">" +
		"<extId>2bf7db53-8820-4fe5-9a78-acc6d3262149</extId>" +
		"<title>fake rdv</title>" +
		"<owner>john@do.fr</owner>" +
		"<date>1295258400000</date>" +
		"<duration>3600</duration>" +
		"<attendees>" +
		"<attendee displayName=\"John Do\" email=\"john@do.fr\" percent=\"0\" required=\"CHAIR\" state=\"NEEDS-ACTION\" isOrganizer=\"true\"/>" +
		"</attendees>" +
		"</event>";
		Document doc = DOMUtils.parse(new ByteArrayInputStream(xml.getBytes()));
		Event ev = parser.parseEvent(doc.getDocumentElement());

		assertThat(ev.isAnonymized()).isFalse();
	}

	@Test
	public void testParseWithNoAnonymizedAttributeEvent() throws SAXException, IOException, FactoryConfigurationError {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
		"<event allDay=\"false\" id=\"\" type=\"VEVENT\" isInternal=\"true\" sequence=\"0\" xmlns=\"http://www.obm.org/xsd/sync/event.xsd\">" +
		"<extId>2bf7db53-8820-4fe5-9a78-acc6d3262149</extId>" +
		"<title>fake rdv</title>" +
		"<owner>john@do.fr</owner>" +
		"<date>1295258400000</date>" +
		"<duration>3600</duration>" +
		"<attendees>" +
		"<attendee displayName=\"John Do\" email=\"john@do.fr\" percent=\"0\" required=\"CHAIR\" state=\"NEEDS-ACTION\" isOrganizer=\"true\"/>" +
		"</attendees>" +
		"</event>";
		Document doc = DOMUtils.parse(new ByteArrayInputStream(xml.getBytes()));
		Event ev = parser.parseEvent(doc.getDocumentElement());

		assertThat(ev.isAnonymized()).isFalse();
	}
}
