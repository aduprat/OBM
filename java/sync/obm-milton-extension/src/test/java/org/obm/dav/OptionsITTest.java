/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2013  Linagora
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
package org.obm.dav;

import static org.easymock.EasyMock.expect;
import static org.fest.assertions.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.easymock.IMocksControl;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.obm.dav.hc.PropFindResponse;
import org.obm.filter.Slow;
import org.obm.guice.GuiceModule;
import org.obm.guice.SlowGuiceRunner;
import org.obm.icalendar.Ical4jHelper;
import org.obm.sync.calendar.Event;
import org.obm.sync.calendar.EventExtId;
import org.obm.sync.calendar.EventType;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import fr.aliacom.obm.common.calendar.CalendarDao;
import fr.aliacom.obm.common.domain.DomainService;
import fr.aliacom.obm.common.domain.ObmDomain;
import fr.aliacom.obm.common.user.ObmUser;
import fr.aliacom.obm.common.user.UserService;

@Slow
@GuiceModule(AbstractObmDavIT.Env.class)
@RunWith(SlowGuiceRunner.class)
public class OptionsITTest extends AbstractObmDavIT {

	@Inject
	private DomainService domainService;
	@Inject
	private UserService userService;
	@Inject
	private CalendarDao calendarDao;
	@Inject
	private Ical4jHelper ical4jHelper;

	@Inject
	IMocksControl control;

	@Ignore
	@Test
	public void testOptions() throws Exception {
		HttpResponse response = options("/");

		assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
	}

	@Test
	public void testUsersPropFind() throws Exception {
		ObmDomain domain = ObmDomain.builder().name("my.domain").build();
		ObmUser user = ObmUser.builder().login("joe").domain(domain).build();

		expect(domainService.findDomainByName("my.domain"))
			.andReturn(domain);
		expect(userService.getUserFromLogin("joe", "my.domain"))
			.andReturn(user);

		control.replay();
		HttpResponse response = propfind("/dav/my.domain/joe", 1);
		control.verify();
		assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_MULTI_STATUS);
	}

	@Test
	public void testEventsPropFind() throws Exception {
		Date now = new Date();

		ObmDomain domain = ObmDomain.builder().name("my.domain").build();
		ObmUser user = ObmUser.builder().login("joe").domain(domain).build();

		Event event = new Event();
		event.setExtId(new EventExtId("event1"));
		event.setTimeCreate(now);
		event.setTimeUpdate(now);
		List<Event> events = ImmutableList.of(event);

		expect(domainService.findDomainByName("my.domain"))
			.andReturn(domain);
		expect(userService.getUserFromLogin("joe", "my.domain"))
			.andReturn(user);
		expect(calendarDao.findAllEvents(null, user, EventType.VEVENT))
			.andReturn(events);

		control.replay();
		HttpResponse response = propfind("/dav/my.domain/joe/calendars/default", 1);
		control.verify();

		assertThat(response.getStatusLine().getStatusCode()).isEqualTo(
				HttpStatus.SC_MULTI_STATUS);
		List<PropFindResponse> responses = PropFindResponse.parse(response, 1);
		assertThat(responses).hasSize(1);
		PropFindResponse first = responses.get(0);
		assertThat(first.getName()).isEqualTo("event1");
		assertThat(first.getModifiedDate().toString()).isEqualTo(now.toString());
		assertThat(first.getCreatedDate().toString()).isEqualTo(now.toString());
	}

	@Test
	public void testEventGet() throws Exception {
		ObmDomain domain = ObmDomain.builder().name("my.domain").build();
		ObmUser user = ObmUser.builder().login("joe").domain(domain).build();

		EventExtId eventId = new EventExtId("event1");
		Event event = new Event();
		event.setExtId(eventId);
		List<Event> events = ImmutableList.of(event);
		String ical = "FAKE ICAL";

		expect(domainService.findDomainByName("my.domain"))
			.andReturn(domain);
		expect(userService.getUserFromLogin("joe", "my.domain"))
			.andReturn(user);
		expect(calendarDao.findEventByExtId(null, user, eventId))
			.andReturn(event);
		expect(ical4jHelper.buildIcs(null, events, null))
			.andReturn(ical);

		control.replay();
		HttpResponse response = get("/dav/my.domain/joe/calendars/default/event1");
		control.verify();

		assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		IOUtils.copy(response.getEntity().getContent(), outputStream);
		String actualIcal = outputStream.toString();
		assertThat(actualIcal).isEqualTo(ical);
	}
}