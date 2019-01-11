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
package org.obm.icalendar;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import java.security.InvalidParameterException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.obm.icalendar.ical4jwrapper.EventDate;
import org.obm.sync.auth.AccessToken;
import org.obm.sync.calendar.Attendee;
import org.obm.sync.calendar.CalendarUserType;
import org.obm.sync.calendar.Event;
import org.obm.sync.calendar.EventExtId;
import org.obm.sync.calendar.EventExtId.Factory;
import org.obm.sync.calendar.EventOpacity;
import org.obm.sync.calendar.EventPrivacy;
import org.obm.sync.calendar.EventRecurrence;
import org.obm.sync.calendar.EventType;
import org.obm.sync.calendar.FreeBusy;
import org.obm.sync.calendar.FreeBusyInterval;
import org.obm.sync.calendar.FreeBusyRequest;
import org.obm.sync.calendar.Participation;
import org.obm.sync.calendar.ParticipationRole;
import org.obm.sync.calendar.RecurrenceDay;
import org.obm.sync.calendar.RecurrenceDays;
import org.obm.sync.calendar.RecurrenceKind;
import org.obm.sync.date.DateProvider;
import org.obm.sync.exception.IllegalRecurrenceKindException;
import org.obm.sync.services.AttendeeService;
import org.obm.sync.utils.RecurrenceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.BiMap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import fr.aliacom.obm.common.domain.ObmDomain;
import fr.aliacom.obm.common.domain.ObmDomainUuid;
import fr.aliacom.obm.common.resource.Resource;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.data.UnfoldingReader;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Content;
import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyFactory;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.Recur.Frequency;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.WeekDay;
import net.fortuna.ical4j.model.WeekDayList;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VFreeBusy;
import net.fortuna.ical4j.model.component.VToDo;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.CuType;
import net.fortuna.ical4j.model.parameter.FbType;
import net.fortuna.ical4j.model.parameter.PartStat;
import net.fortuna.ical4j.model.parameter.Role;
import net.fortuna.ical4j.model.parameter.Rsvp;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.Action;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Categories;
import net.fortuna.ical4j.model.property.Clazz;
import net.fortuna.ical4j.model.property.Comment;
import net.fortuna.ical4j.model.property.Created;
import net.fortuna.ical4j.model.property.DateProperty;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.Due;
import net.fortuna.ical4j.model.property.Duration;
import net.fortuna.ical4j.model.property.ExDate;
import net.fortuna.ical4j.model.property.LastModified;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.Method;
import net.fortuna.ical4j.model.property.Organizer;
import net.fortuna.ical4j.model.property.PercentComplete;
import net.fortuna.ical4j.model.property.Priority;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.RecurrenceId;
import net.fortuna.ical4j.model.property.Repeat;
import net.fortuna.ical4j.model.property.Sequence;
import net.fortuna.ical4j.model.property.Status;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.Transp;
import net.fortuna.ical4j.model.property.Trigger;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.UtcProperty;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.model.property.XProperty;
import net.fortuna.ical4j.util.TimeZones;
import net.fortuna.ical4j.validate.ValidationException;

@Singleton
public class Ical4jHelper implements RecurrenceHelper {
	
	private static final String MAILTO = "mailto:";
	private static final int MAX_FOLD_LENGTH = 74;
	private static final String X_OBM_DOMAIN = "X-OBM-DOMAIN";
	private static final String X_OBM_DOMAIN_UUID = "X-OBM-DOMAIN-UUID";
	private static final String XOBMORIGIN = "X-OBM-ORIGIN";
	private static final boolean DTSTART_WITHOUT_TIMEZONE = false;
	private static final boolean DTSTART_WITH_TIMEZONE = true;

	private static Logger logger = LoggerFactory.getLogger(Ical4jHelper.class);
	private static final BiMap<RecurrenceDay, WeekDay> RECURRENCE_DAY_TO_WEEK_DAY = new ImmutableBiMap.Builder<RecurrenceDay, WeekDay>()
			.put(RecurrenceDay.Sunday, WeekDay.SU).put(RecurrenceDay.Monday, WeekDay.MO)
			.put(RecurrenceDay.Tuesday, WeekDay.TU).put(RecurrenceDay.Wednesday, WeekDay.WE)
			.put(RecurrenceDay.Thursday, WeekDay.TH).put(RecurrenceDay.Friday, WeekDay.FR)
			.put(RecurrenceDay.Saturday, WeekDay.SA).build();
	private static final BiMap<WeekDay, RecurrenceDay> WEEK_DAY_TO_RECURRENCE_DAY = RECURRENCE_DAY_TO_WEEK_DAY.inverse();
	private static final Map<RecurrenceKind, Frequency> RECURRENCEKIND_TO_RECUR = new ImmutableMap.Builder<RecurrenceKind, Frequency>()
			.put(RecurrenceKind.daily, Frequency.DAILY).put(RecurrenceKind.weekly, Frequency.WEEKLY)
			.put(RecurrenceKind.monthlybydate, Frequency.MONTHLY).put(RecurrenceKind.monthlybyday, Frequency.MONTHLY)
			.put(RecurrenceKind.yearly, Frequency.YEARLY).put(RecurrenceKind.yearlybyday, Frequency.YEARLY).build();
	
	private static final BiMap<EventPrivacy, Clazz> PRIVACY_TO_CLASSIFICATION = new ImmutableBiMap.Builder<EventPrivacy, Clazz>()
			.put(EventPrivacy.PUBLIC, Clazz.PUBLIC)
			.put(EventPrivacy.PRIVATE, Clazz.PRIVATE)
			.put(EventPrivacy.CONFIDENTIAL, Clazz.CONFIDENTIAL)
			.build();
	private static final BiMap<Clazz, EventPrivacy> CLASSIFICATION_TO_PRIVACY = PRIVACY_TO_CLASSIFICATION.inverse();
	
	private static final ImmutableSet<String> UTC_TZID_STRINGS = ImmutableSet.of(
			TimeZones.GMT_ID, // "Etc/GMT"
			TimeZones.UTC_ID, // "Etc/UTC"
			TimeZones.IBM_UTC_ID // "GMT"
		);

	private final DateProvider dateProvider;
	private final Factory eventExtIdFactory;
	private final AttendeeService attendeeService;
	private final TimeZoneRegistry tzRegistry;
	
	@Inject
	@VisibleForTesting
	public Ical4jHelper(DateProvider obmHelper, EventExtId.Factory eventExtIdFactory, AttendeeService attendeeService) {
		this.dateProvider = obmHelper;
		this.eventExtIdFactory = eventExtIdFactory;
		this.attendeeService = attendeeService;
		this.tzRegistry = TimeZoneRegistryFactory.getInstance().createRegistry();
	}

	public String buildIcsInvitationRequest(Ical4jUser iCal4jUser, Event event, AccessToken token) {
		Calendar calendar = initCalendar();
		VEvent vEvent = buildIcsInvitationVEvent(calendar, iCal4jUser, event, token, event.isRecurrent());
		calendar.getComponents().add(vEvent);
		if (event.isRecurrent()) {
			for (Event ee : event.getRecurrence().getEventExceptions()) {
				VEvent eventExt = buildIcsInvitationVEventException(calendar, ee, DTSTART_WITH_TIMEZONE);
				appendUidToICS(eventExt.getProperties(), ee, event.getExtId());
				calendar.getComponents().add(eventExt);
			}
		}
		calendar.getProperties().add(Method.REQUEST);
		return foldingWriterToString(calendar);
	}
	
	private String foldingWriterToString(final Calendar calendar) {
		Writer writer =  new StringWriter();
		CalendarOutputter calendarOutputter = new CalendarOutputter(true, MAX_FOLD_LENGTH);
		try {
			calendarOutputter.output(calendar, writer);
			return writer.toString(); 
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		} catch (ValidationException e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	public String buildIcsInvitationReply(final Event event, final Ical4jUser replyICal4jUser, AccessToken token) {
		Method method = Method.REPLY;
		final Attendee replyAttendee = event.findAttendeeFromEmail(replyICal4jUser.getEmail());
		final Calendar calendar = buildVEvent(replyICal4jUser, event, replyAttendee,method, token, event.isRecurrent());
		calendar.getProperties().add(method);
		return foldingWriterToString(calendar);
	}
	
	public String buildIcsInvitationCancel(Ical4jUser iCal4jUser, Event event, AccessToken token) {
		Method method = Method.CANCEL;
		Calendar calendar = buildVEvent(iCal4jUser, event, null, method, token, event.isRecurrent());
		calendar.getProperties().add(method);
		return foldingWriterToString(calendar);
	}
	
	public String buildIcs(Ical4jUser iCal4jUser, Collection<Event> events, AccessToken token) {
		Calendar calendar = this.buildVEvents(iCal4jUser, events, null, null, token, DTSTART_WITHOUT_TIMEZONE);
		return foldingWriterToString(calendar);
	}
	
	public String buildIcsWithTimeZoneOnDtStart(Ical4jUser iCal4jUser, Collection<Event> events, AccessToken token) {
		Calendar calendar = this.buildVEvents(iCal4jUser, events, null, null, token, DTSTART_WITH_TIMEZONE);
		return foldingWriterToString(calendar);
	}

	private VEvent buildIcsInvitationVEventDefaultValue(Calendar calendar, Event event, boolean dtStartWithTimeZone) {
		VEvent vEvent = new VEvent();
		PropertyList<Property> prop = vEvent.getProperties();
		appendDtstamp(event, vEvent);
		appendCreated(prop, event);
		appendLastModified(prop, event);
		appendSequence(prop, event);
		appendAttendeesToICS(prop, event.getAttendees());
		appendCategoryToICS(prop, event);
		appendEventDates(calendar, prop, event, dtStartWithTimeZone);
		appendDescriptionToICS(prop, event);
		appendLocationToICS(prop, event);
		appendTranspToICS(prop, event);
		appendOrganizerToICS(prop, event);
		appendPriorityToICS(prop, event);
		appendPrivacyToICS(prop, event);
		appendSummaryToICS(prop, event);
		appendRRuleToICS(prop, event);
		appendExDateToICS(prop, event);
		appendVAlarmToICS(vEvent.getAlarms(), event);
		appendRecurenceIdToICS(prop, event);
		appendXMozLastAck(prop);
		return vEvent;
	}

	private VEvent buildIcsInvitationVEventException(Calendar calendar, Event event, boolean dtStartWithTimeZone) {
		return buildIcsInvitationVEventDefaultValue(calendar, event, dtStartWithTimeZone);
	}
	
	private VEvent buildIcsInvitationVEvent(Calendar calendar, Ical4jUser iCal4jUser, Event event, AccessToken token, boolean dtStartWithTimeZone) {
		VEvent vEvent = buildIcsInvitationVEventDefaultValue(calendar, event, dtStartWithTimeZone);
		PropertyList<Property> prop = vEvent.getProperties();
		appendUidToICS(prop, event, null);
		appendXObmDomainProperties(iCal4jUser, prop);
		appendXObmOrigin(prop, token);
		return vEvent;
	}
	
	public FreeBusyRequest parseICSFreeBusy(String ics, ObmDomain domain, Integer ownerId) 
		throws IOException, ParserException {
		CalendarBuilder builder = new CalendarBuilder();
		Calendar calendar = builder.build(new StringReader(ics));
		FreeBusyRequest freeBusy = new FreeBusyRequest();
		if (calendar != null) {
			ComponentList<?> comps = getComponents(calendar, Component.VFREEBUSY);
			if (comps.size() > 0) {
				VFreeBusy vFreeBusy = (VFreeBusy) comps.get(0);
				freeBusy = getFreeBusy(vFreeBusy, domain, ownerId);
			}
		}

		return freeBusy;
	}

	public List<Event> parseICS(String ics, Ical4jUser ical4jUser, Integer ownerId) 
		throws IOException, ParserException {
		
		Calendar calendar = buildCalendar(ics);
		Cache<String, Optional<Attendee>> cache = newAttendeeCache();

		if (calendar != null) {
			return ImmutableList.copyOf(
					Iterables.concat(
							getEvents(calendar, ical4jUser, ownerId, cache),
							getTodos(ical4jUser, calendar, ownerId, cache)));
		}
		return ImmutableList.<Event>of();
	}

	private Calendar buildCalendar(String ics) throws IOException, ParserException {
		CalendarBuilder builder = new CalendarBuilder();
		Calendar calendar = builder.build(new UnfoldingReader(new StringReader(ics), true));
		return calendar;
	}


	public List<Event> parseICSEvent(String ics, Ical4jUser ical4jUser, Integer ownerId) throws IOException, ParserException {
		Calendar calendar = buildCalendar(ics);
		Cache<String, Optional<Attendee>> cache = newAttendeeCache();

		if (calendar != null) {
			return ImmutableList.copyOf(getEvents(calendar, ical4jUser, ownerId, cache));
		}
		return ImmutableList.<Event>of();
	}

	private Cache<String, Optional<Attendee>> newAttendeeCache() {
		return CacheBuilder.newBuilder().build();
	}
	
	private Collection<Event> getTodos(Ical4jUser ical4jUser, Calendar calendar, Integer ownerId, Cache<String, Optional<Attendee>> cache) {
		List<Event> todos = Lists.newArrayList();
		ComponentList<?> comps = getComponents(calendar, Component.VTODO);
		for (Object obj: comps) {
			VToDo vTodo = (VToDo) obj;
			Event event = convertVTodoToEvent(ical4jUser, vTodo, ownerId, cache);
			todos.add(event);
		}
		return todos;
	}

	private Collection<Event> getEvents(Calendar calendar, Ical4jUser ical4jUser, Integer ownerId, Cache<String, Optional<Attendee>> cache) {
		Map<EventExtId, Event> mapEvents = Maps.newHashMap();
		Multimap<EventExtId, Event> mapExceptionEvents = HashMultimap.create();
		ComponentList<?> comps = getComponents(calendar, Component.VEVENT);
		for (Object obj: comps) {
			VEvent vEvent = (VEvent) obj;
			Event event = convertVEventToEvent(ical4jUser, vEvent, ownerId, cache);
			if(event.getRecurrenceId() == null) {
				mapEvents.put(event.getExtId(), event);
			} else {
				mapExceptionEvents.put(event.getExtId(), event);
			}
		}
		return flattenEventsAndExceptions(mapEvents, mapExceptionEvents);
	}

	private Collection<Event> flattenEventsAndExceptions(
			Map<EventExtId, Event> mapEvents,
			Multimap<EventExtId, Event> mapExceptionEvents) {

		ImmutableSet.Builder<Event> flattenedEvents = ImmutableSet.<Event>builder().addAll(mapEvents.values());

		for (Entry<EventExtId, Collection<Event>> exceptionsByExtId : mapExceptionEvents.asMap().entrySet()) {
			Event parentEvent = mapEvents.get(exceptionsByExtId.getKey());
			Collection<Event> eventsException = exceptionsByExtId.getValue();
			if (parentEvent != null) {
				addOrReplaceExceptions(parentEvent.getRecurrence(), eventsException);
			} else {
				// No parent found, add these exceptions to the "parsed events" list
				flattenedEvents.addAll(eventsException);
			}
		}
		
		return flattenedEvents.build();
	}

	private void addOrReplaceExceptions(EventRecurrence recurrenceTarget, Collection<Event> eventsToAdd) {
		for (Event eventToAdd : eventsToAdd) {
			recurrenceTarget.getExceptions().remove(eventToAdd.getRecurrenceId());
		}
		recurrenceTarget.getEventExceptions().addAll(eventsToAdd);
	}
	
	/* package */ Event convertVEventToEvent(Ical4jUser ical4jUser, VEvent vEvent, Integer ownerId, Cache<String, Optional<Attendee>> cache) {
		Event event = new Event();
		event.setType(EventType.VEVENT);
		appendSummary(event, vEvent.getSummary());
		appendDescription(event, vEvent.getDescription());
		appendUid(event, vEvent.getUid());
		appendPrivacy(event, vEvent.getClassification());
		appendOwner(event, vEvent.getOrganizer());
		appendCategory(event, vEvent.getProperty(Property.CATEGORIES));
		appendLocation(event, vEvent.getLocation());
		appendSequence(event, vEvent.getSequence());
		appendDate(event, vEvent.getStartDate());
		appendDuration(event, vEvent.getStartDate(), vEvent.getEndDate());
		appendAllDay(event, vEvent);
		appendPriority(event, vEvent.getPriority());
		appendRecurrenceId(event, vEvent.getRecurrenceId());
		appendAttendees(event, vEvent, ical4jUser.getObmDomain(), ownerId, cache);
		appendRecurence(event, vEvent);
		appendAlert(event, vEvent.getAlarms());
		appendOpacity(event, vEvent.getTransparency(), event.isAllday());
		appendIsInternal(ical4jUser, event, vEvent.getProperty(X_OBM_DOMAIN_UUID));
		
		appendCreated(event, vEvent.getCreated());
		appendLastModified(event, vEvent.getLastModified());
		return event;
	}

	/* package */ Event convertVTodoToEvent(Ical4jUser ical4jUser, VToDo vTodo, Integer ownerId, Cache<String, Optional<Attendee>> cache) {
		Event event = new Event();
		event.setType(EventType.VTODO);
		appendSummary(event, vTodo.getSummary());
		appendDescription(event, vTodo.getDescription());
		appendUid(event, vTodo.getUid());
		appendPrivacy(event, vTodo.getClassification());
		appendOwner(event, vTodo.getOrganizer());
		appendCategory(event, vTodo.getProperty(Property.CATEGORIES));
		appendLocation(event, vTodo.getLocation());
		appendDate(event, vTodo.getStartDate());
		appendDuration(event, vTodo.getStartDate(), vTodo.getDue());
		appendAllDay(event, vTodo.getStartDate(), vTodo.getDue());
		appendPriority(event, vTodo.getPriority());
		appendRecurrenceId(event, vTodo.getRecurrenceId());

		appendAttendees(event, vTodo, ical4jUser.getObmDomain(), ownerId, cache);
		appendRecurence(event, vTodo);
		appendAlert(event, vTodo.getAlarms());
		appendPercent(event, vTodo.getPercentComplete(), ical4jUser.getEmail());
		appendStatus(event, vTodo.getStatus(), ical4jUser.getEmail());
		appendOpacity(event,
				(Transp) vTodo.getProperties().getProperty(Property.TRANSP),
				event.isAllday());
		appendIsInternal(ical4jUser, event, vTodo.getProperty(X_OBM_DOMAIN_UUID));
		
		appendCreated(event, vTodo.getCreated());
		appendLastModified(event, vTodo.getLastModified());
		return event;
	}
	
	private void appendLastModified(Event event,
			LastModified lastModified) {
		if (lastModified != null) {
			event.setTimeUpdate(lastModified.getDate());
		}
	}

	private void appendCreated(Event event, Created created) {
		if (created != null) {
			event.setTimeCreate(created.getDate());
		}
	}
	
	private void appendIsInternal(Ical4jUser ical4jUser, Event event, Property obmDomain) {
		boolean eventIsInternal = false;
		if(obmDomain != null){
			eventIsInternal = ical4jUser.getObmDomain().getUuid().equals(ObmDomainUuid.of(obmDomain.getValue()));
		}
		event.setInternalEvent(eventIsInternal);
		
	}

	private void appendOpacity(Event event, Transp transp,
			boolean isAllDay) {
		if (Transp.TRANSPARENT.equals(transp)) {
			event.setOpacity(EventOpacity.TRANSPARENT);
		} else if (Transp.OPAQUE.equals(transp)) {
			event.setOpacity(EventOpacity.OPAQUE);
		} else if (isAllDay) {
			event.setOpacity(EventOpacity.TRANSPARENT);
		} else {
			event.setOpacity(EventOpacity.OPAQUE);
		}
	}

	private void appendRecurrenceId(Event event,
			RecurrenceId recurrenceId) {
		if (recurrenceId != null) {
			event.setRecurrenceId(recurrenceId.getDate());
		}
	}

	private void appendStatus(Event event, Status status, String email) {
		if (status != null) {
			for (Attendee att : event.getAttendees()) {
				if (att.getEmail().equals(email)) {

					if (Status.VTODO_NEEDS_ACTION.equals(status)) {
						att.setParticipation(Participation.needsAction());
					} else if (Status.VTODO_IN_PROCESS.equals(status)) {
						att.setParticipation(Participation.inProgress());
					} else if (Status.VTODO_COMPLETED.equals(status)) {
						att.setParticipation(Participation.completed());
					} else if (Status.VTODO_CANCELLED.equals(status)) {
						att.setParticipation(Participation.declined());
					} else {
						att.setParticipation(null);
					}
				}
			}
		}
	}

	private void appendPercent(Event event,
			PercentComplete percentComplete, String email) {
		if (percentComplete != null) {
			for (Attendee att : event.getAttendees()) {
				if (att.getEmail().equals(email)) {
					att.setPercent(percentComplete.getPercentage());
				}
			}
		}
	}

	private void appendPriority(Event event, Priority priority) {
		int value = 2;
		if (priority != null) {
			if (priority.getLevel() >= 6) {
				value = 1;
			} else if (priority.getLevel() >= 3 && priority.getLevel() < 6) {
				value = 2;
			} else if (priority.getLevel() > 0 && priority.getLevel() < 3) {
				value = 3;
			}
			event.setPriority(value);
		}
	}

	@VisibleForTesting void appendAllDay(Event event, DtStart startDate, DateProperty endDate) {
		if (endDate != null && startDate != null && startDate.getDate() != null && endDate.getDate() != null)  {
			if (startDate.getDate() instanceof DateTime || endDate.getDate() instanceof DateTime) {
				event.setAllday(false);
				return;
			}
		}
		event.setAllday(true);
	}

	@VisibleForTesting void appendAllDay(Event event, VEvent vevent) {
		event.setAllday(isVeventAllDay(vevent));
	}

	private boolean isVeventAllDay(VEvent vevent) {
		DtStart startDate = vevent.getStartDate();
		if (startDate != null) {
			Parameter dtStartValueParameter = startDate.getParameter(Parameter.VALUE);
			return Value.DATE.equals(dtStartValueParameter);
		}

		Duration duration = vevent.getDuration();
		if (duration == null) {
			return false;
		}

		TemporalAmount temporalAmount = duration.getDuration();
		if (temporalAmount instanceof java.time.Period) {
			java.time.Period period = (java.time.Period) temporalAmount;
			return period.getMonths() == 0
				&& period.getYears() == 0
				&& period.getDays() < 7;
		}
		java.time.Duration dur = (java.time.Duration) temporalAmount;
		long days = dur.toDays();
		if (days >= 7) {
			return false;
		}
		return java.time.Duration.ofDays(days)
			.equals(dur);
	}

	private void appendDuration(Event event, DtStart startDate,
			DateProperty due) {
		int duration = getDuration(startDate, due);
		event.setDuration(duration);
	}

	private void appendDate(Event event, DtStart startDate) {
		if (startDate != null) {
			event.setStartDate(startDate.getDate());
			event.setTimezoneName("Etc/GMT");
		}

	}

	private void appendLocation(Event event, Location location) {
		if (location != null) {
			event.setLocation(location.getValue());
		}
	}

	private void appendSequence(Event event, Sequence sequence) {
		if (sequence != null) {
			event.setSequence(sequence.getSequenceNo());
		}
	}
	
	private void appendCategory(Event event, Property category) {
		if (category != null) {
			event.setCategory(category.getValue());
		}
	}

	private void appendOwner(Event event, Organizer organizer) {
		if (organizer != null) {
			Parameter cn = organizer.getParameter(Parameter.CN);
			String cnOrganizer = "";
			if (cn != null) {
				cnOrganizer = cn.getValue();
			}

			if (cnOrganizer != null && !"".equals(cnOrganizer)) {
				event.setOwner(cnOrganizer);
			}

			int mailToIndex = organizer.getValue().toLowerCase()
					.indexOf(MAILTO);
			if (mailToIndex != -1) {
				event.setOwnerEmail(organizer.getValue().substring(
						mailToIndex + MAILTO.length()));
				
				if (Strings.isNullOrEmpty(event.getOwner())) {
					event.setOwner(organizer.getValue().substring(
							mailToIndex + MAILTO.length()));
				}
			}
		}
	}

	private void appendPrivacy(Event event, Clazz classification) {
		EventPrivacy eventPrivacy = CLASSIFICATION_TO_PRIVACY.get(classification);
		event.setPrivacy(MoreObjects.firstNonNull(eventPrivacy, EventPrivacy.PUBLIC));
	}

	private void appendUid(Event event, Uid uid) {
		if (uid != null && !Strings.isNullOrEmpty(uid.getValue())) {
			event.setExtId(new EventExtId(uid.getValue()));
		} else {
			event.setExtId(eventExtIdFactory.generate());
		}
		
	}

	private void appendDescription(Event event, Description description) {
		if (description != null) {
			event.setDescription(description.getValue());
		}
	}

	private void appendSummary(Event event, Summary summary) {
		if (summary != null) {
			event.setTitle(summary.getValue());
		}

	}

	public String parseEvents(Ical4jUser iCal4jUser, Collection<Event> listEvent, AccessToken token) {

		Calendar calendar = initCalendar();

		for (Event event : listEvent) {
			VEvent vEvent = getVEvent(calendar, iCal4jUser, event, null, null, token, DTSTART_WITHOUT_TIMEZONE);
			calendar.getComponents().add(vEvent);
		}
		return calendar.toString();
	}

	public String parseEvent(Event event, Ical4jUser iCal4jUser, AccessToken token) {
		if (EventType.VEVENT.equals(event.getType())) {
			Calendar c = buildVEvent(iCal4jUser, event, null, null, token, DTSTART_WITHOUT_TIMEZONE);
			return c.toString();
		} else if (EventType.VTODO.equals(event.getType())) {
			Calendar c = buildVTodo(event, iCal4jUser);
			return c.toString();
		}
		return null;
	}

	private Calendar buildVTodo(Event event, Ical4jUser iCal4jUser) {
		Calendar calendar = initCalendar();
		VToDo vTodo = getVToDo(event, iCal4jUser);
		calendar.getComponents().add(vTodo);
		if (event.isRecurrent()) {
			for (Event ee : event.getRecurrence().getEventExceptions()) {
				VToDo todoExt = getVTodo(ee, event.getExtId(), iCal4jUser, event);
				calendar.getComponents().add(todoExt);
			}
		}
		return calendar;
	}

	private Calendar buildVEvent(Ical4jUser iCal4jUser, Event event, Attendee replyAttendee, Method method, AccessToken token, boolean dtStartWithTimeZone) {
		return buildVEvents(iCal4jUser, Arrays.asList(event), replyAttendee, method, token, dtStartWithTimeZone);
	}

	private Calendar buildVEvents(Ical4jUser iCal4jUser, Collection<Event> events, Attendee replyAttendee, Method method, AccessToken token, boolean dtStartWithTimeZone) {
		Calendar calendar = initCalendar();
		for (Event event : events) {
			VEvent vEvent = getVEvent(calendar, iCal4jUser, event, replyAttendee, method, token, dtStartWithTimeZone);
			calendar.getComponents().add(vEvent);
			if (event.isRecurrent()) {
				for (Event ee : event.getRecurrence().getEventExceptions()) {
					VEvent eventExt = getVEvent(calendar, null, ee, event.getExtId(), event, replyAttendee,
							method, token, dtStartWithTimeZone);
					calendar.getComponents().add(eventExt);
				}
			}
		}
		return calendar;
	}

	private VEvent getVEvent(Calendar calendar, Ical4jUser iCal4jUser, Event event, Attendee replyAttendee, Method method, AccessToken token, boolean dtStartWithTimeZone) {
		return getVEvent(calendar, iCal4jUser, event, null, null, replyAttendee, method, token, dtStartWithTimeZone);
	}

	private VEvent getVEvent(Calendar calendar, Ical4jUser iCal4jUser, Event event, EventExtId parentExtID, Event parent, Attendee replyAttendee, Method method, AccessToken token, boolean dtStartWithTimeZone) {
		VEvent vEvent = new VEvent();
		PropertyList<Property> prop = vEvent.getProperties();

		if (Method.REPLY.equals(method)) {
			appendDtstamp(dateProvider.getDate(), vEvent);
		} else {
			appendDtstamp(event, vEvent);
		}
		appendUidToICS(prop, event, parentExtID);
		appendCreated(prop, event);
		appendLastModified(prop, event);
		appendSequence(prop, event);
		if (replyAttendee == null) {
			appendAttendeesToICS(prop, event.getAttendees());
		} else {
			appendAttendeesToICS(prop, ImmutableList.of(replyAttendee));
			appendReplyCommentToICS(prop, replyAttendee);
		}
		appendCategoryToICS(prop, event);
		appendEventDates(calendar, prop, event, dtStartWithTimeZone);
		appendDescriptionToICS(prop, event);
		appendLocationToICS(prop, event);
		appendTranspToICS(prop, event);
		if (parent != null) {
			appendOrganizerToICS(prop, parent);
		} else {
			appendOrganizerToICS(prop, event);
		}
		appendPriorityToICS(prop, event);
		appendPrivacyToICS(prop, event);
		appendSummaryToICS(prop, event);
		appendRRuleToICS(prop, event);
		appendExDateToICS(prop, event);
		if(canAddVAlarmToICS(method)){
			appendVAlarmToICS(vEvent.getAlarms(), event);
		}
		appendRecurenceIdToICS(prop, event);
		appendXMozLastAck(prop);
		if (token != null) {
			appendXObmOrigin(prop, token);
		}
		if(iCal4jUser != null){
			appendXObmDomainProperties(iCal4jUser, prop);
		}
		return vEvent;
	}

	private void appendDtstamp(Event event, VEvent vEvent) {
		Date eventTimeUpdate = event.getTimeUpdate();
		Date eventTimeCreate = event.getTimeCreate();
		if (eventTimeUpdate == null && eventTimeCreate == null) {
			return;
		}
		
		appendDtstamp(MoreObjects.firstNonNull(eventTimeUpdate, eventTimeCreate), vEvent);
	}
	
	private void appendDtstamp(Date time, VEvent vEvent) {
		vEvent.getDateStamp().setDateTime(new DateTime(time));
	}

	private boolean canAddVAlarmToICS(Method method) {
		if(method == null || Method.ADD.equals(method) || Method.COUNTER.equals(method) || Method.PUBLISH.equals(method) || Method.REQUEST.equals(method)){
			return true;
		} else {
			return false;
		}
	}

	private VToDo getVToDo(Event event, Ical4jUser iCal4jUser) {
		return getVTodo(event, null, iCal4jUser, null);
	}

	private VToDo getVTodo(Event event, EventExtId parentExtID, Ical4jUser iCal4jUser, Event pere) {
		VToDo vTodo = new VToDo();
		PropertyList<Property> prop = vTodo.getProperties();

		appendUidToICS(prop, event, parentExtID);
		appendCreated(prop, event);
		appendLastModified(prop, event);
		appendAttendeesToICS(prop, event.getAttendees());
		appendCategoryToICS(prop, event);
		appendDtStartToICS(prop, event);
		appendDuedToICS(prop, event);
		appendDescriptionToICS(prop, event);
		appendLocationToICS(prop, event);
		appendTranspToICS(prop, event);
		if (pere != null) {
			appendOrganizerToICS(prop, pere);
		} else {
			appendOrganizerToICS(prop, event);
		}
		appendPriorityToICS(prop, event);
		appendPrivacyToICS(prop, event);
		appendSummaryToICS(prop, event);
		appendRRuleToICS(prop, event);
		appendExDateToICS(prop, event);
		appendVAlarmToICS(vTodo.getAlarms(), event);
		appendRecurenceIdToICS(prop, event);
		appendPercentCompleteToICS(prop, event, iCal4jUser);
		appendStatusToICS(prop, event, iCal4jUser);
		appendXMozLastAck(prop);

		return vTodo;
	}

	private void appendDtStartToICS(PropertyList<Property> prop, Event event) {
			prop.add(getDtStart(event.getStartDate()));
	}

	private void appendXMozLastAck(PropertyList<Property> prop) {
		java.util.Calendar cal = java.util.Calendar.getInstance(TimeZone
				.getTimeZone("GMT"));
		cal.setTimeInMillis(System.currentTimeMillis());
		XMozLastAck p = new XMozLastAck();
		DateTime dateTime = new DateTime(cal.getTime());
		dateTime.setUtc(true);
		p.setDate(dateTime);
		prop.add(p);
	}
	
	private static class XMozLastAck extends UtcProperty {

		private static final String X_MOZ_LASTACK = "X-MO-LASTACK";
		private static final long serialVersionUID = -3843759478796584984L;
		
		public XMozLastAck() {
			super(X_MOZ_LASTACK, new Factory());
		}

		public XMozLastAck(ParameterList aList, String aValue) throws ParseException {
			super(X_MOZ_LASTACK, aList, new Factory());
			setValue(aValue);
		  }

		public static class Factory extends Content.Factory implements PropertyFactory<Property> {
			private static final long serialVersionUID = 1L;

			public Factory() {
				super(X_MOZ_LASTACK);
			}

			public Property createProperty(final ParameterList parameters, final String value)
					throws IOException, URISyntaxException, ParseException {
				return new XMozLastAck(parameters, value);
			}

			public Property createProperty() {
				return new XMozLastAck();
			}
		}
	}
	
	private void appendXObmDomainProperties(Ical4jUser iCal4jUser, PropertyList<Property> prop) {
		ObmDomain obmDomain = iCal4jUser.getObmDomain();
		XProperty domainProp = new XProperty(X_OBM_DOMAIN, obmDomain.getName());
		XProperty uuidDomainProp = new XProperty(X_OBM_DOMAIN_UUID, obmDomain.getUuid().get());	
		prop.add(domainProp);
		prop.add(uuidDomainProp);
	}

	private void appendXObmOrigin(PropertyList<Property> prop, AccessToken token) {
		XProperty p = new XProperty(XOBMORIGIN, token.getOrigin());
		prop.add(p);
	}

	private void appendStatusToICS(PropertyList<Property> prop, Event event, Ical4jUser iCal4jUser) {
		for (Attendee att : event.getAttendees()) {
			if (att.getEmail().equals(iCal4jUser.getEmail())) {
				if (Participation.needsAction().equals(att.getParticipation())) {
					prop.add(Status.VTODO_NEEDS_ACTION);
				} else if (Participation.inProgress().equals(att.getParticipation())) {
					prop.add(Status.VTODO_IN_PROCESS);
				} else if (Participation.completed().equals(att.getParticipation())) {
					prop.add(Status.VTODO_COMPLETED);
				} else if (Participation.declined().equals(att.getParticipation())) {
					prop.add(Status.VTODO_CANCELLED);
				} else {
					prop.add(new Status(""));
				}

			}
		}
	}

	private void appendPercentCompleteToICS(PropertyList<Property> prop, Event event, Ical4jUser iCal4jUser) {
		for (Attendee att : event.getAttendees()) {
			if (att.getEmail().equals(iCal4jUser.getEmail())) {
				prop.add(new PercentComplete(att.getPercent()));
			}
		}

	}

	private void appendDuedToICS(PropertyList<Property> prop, Event event) {
		if (event.getDuration() != 0) {
			Due dtEnd = getDue(event.getStartDate(), event.getDuration());
			if (dtEnd != null) {
				prop.add(dtEnd);
			}
		}
	}

	private void appendRecurenceIdToICS(PropertyList<Property> prop, Event event) {
		if (event.getRecurrenceId() != null) {
			prop.add(getRecurrenceId(event));
		}
	}

	private void appendVAlarmToICS(ComponentList<VAlarm> prop, Event event) {
		VAlarm vAlarm = getVAlarm(event.getAlert());
		if (vAlarm != null) {
			prop.add(vAlarm);
		}
	}

	private void appendExDateToICS(PropertyList<Property> prop, Event event) {
		ExDate exDate = getExDate(event);
		if (exDate != null) {
			prop.add(exDate);
		}
	}

	private void appendRRuleToICS(PropertyList<Property> prop, Event event) {
		if (event.getRecurrenceId() == null) {
			RRule rrule = getRRule(event);
			if (rrule != null) {
				prop.add(rrule);
			}
		}
	}

	private void appendSummaryToICS(PropertyList<Property> prop, Event event) {
		prop.add(new Summary(event.getTitle()));
	}

	private void appendReplyCommentToICS(PropertyList<Property> prop, Attendee attendee) {
		Participation status = attendee.getParticipation();

		if (status.hasDefinedComment()) {
			org.obm.sync.calendar.Comment comment = status.getComment();
			prop.add(new Comment(comment.serializeToString()));
		}
	}

	private void appendPrivacyToICS(PropertyList<Property> prop, Event event) {
		prop.add(getClazz(event.getPrivacy()));
	}

	private void appendPriorityToICS(PropertyList<Property> prop, Event event) {
		int priority = 5;
		if (event.getPriority() != null) {
			if (event.getPriority() == 1) {
				priority = 9;
			} else if (event.getPriority() == 2) {
				priority = 5;
			} else if (event.getPriority() == 3) {
				priority = 1;
			}
		}
		prop.add(new Priority(priority));
	}

	private void appendOrganizerToICS(PropertyList<Property> prop, Event event) {
		final Attendee organizer = event.findOrganizer();
		if (organizer != null) {
			prop.add(getOrganizer(organizer.getDisplayName(), organizer.getEmail()));	
		} else {
			prop.add(getOrganizer(event.getOwnerDisplayName(), event.getOwnerEmail()));
		}
	}

	private void appendTranspToICS(PropertyList<Property> prop, Event event) {
		prop.add(getTransp(event.getOpacity()));
	}

	private void appendLocationToICS(PropertyList<Property> prop, Event event) {
		if (!isEmpty(event.getLocation())) {
			prop.add(new Location(event.getLocation()));
		}
	}

	private void appendDescriptionToICS(PropertyList<Property> prop, Event event) {
		if (!isEmpty(event.getDescription())) {
			prop.add(new Description(event.getDescription()));
		}

	}

	private void appendEventDates(Calendar calendar, PropertyList<Property> prop, Event event, boolean dtStartWithTimeZone) {
		if(event.isAllday()) {
			appendDtStartAsDateToICS(prop, event);
			appendDtEndAsDateToICS(prop, event);
		} else {
			if (dtStartWithTimeZone) {
				appendDtStartAsDateTimeToICSWithTimeZone(calendar, prop, event);
			} else {
				appendDtStartAsDateTimeToICS(prop, event);
			}
			appendDurationToICS(prop, event);
		}
	}

	private void appendDtEndAsDateToICS(PropertyList<Property> prop, Event event) {
		prop.add(new DtEnd(new EventDate(event.getEndDate(), TimeZone.getTimeZone(event.getTimezoneName())), true));
	}

	private void appendDtStartAsDateToICS(PropertyList<Property> prop, Event event) {
		prop.add(new DtStart(new EventDate(event.getStartDate(), TimeZone.getTimeZone(event.getTimezoneName())), true));
	}

	private void appendDtStartAsDateTimeToICSWithTimeZone(Calendar calendar, PropertyList<Property> prop, Event event) {
		String timezoneName = MoreObjects.firstNonNull(event.getTimezoneName(), TimeZones.GMT_ID);
		net.fortuna.ical4j.model.TimeZone zone = tzRegistry.getTimeZone(timezoneName);
		DateTime icalDate = new DateTime();
		icalDate.setTime(event.getStartDate().getTime());

		if (UTC_TZID_STRINGS.contains(timezoneName)) {
			icalDate.setUtc(true);
		} else {
			icalDate.setTimeZone(zone);

			// OBMFULL-6433
			// As an attempt to be more widely compatible with external clients, we serialize the VTIMEZONE
			// component inside the ICS file, only when an actual TZID is used in the event dates.
			// ical4j handles all the TZID <-> VTIMEZONE mapping for us using the TimezoneRegistry, but
			// the probability that this change breaks something somewhere is high, though...
			if (!calendar.getComponents().contains(zone.getVTimeZone())) {
				calendar.getComponents().add(zone.getVTimeZone());
			}
		}

		DtStart dts = new DtStart(zone);
		dts.setDate(icalDate);
		prop.add(dts);
	}
	
	private void appendDtStartAsDateTimeToICS(PropertyList<Property> prop, Event event) {
		prop.add(new DtStart(new DateTime(event.getStartDate()), true));
	}


	private void appendLastModified(PropertyList<Property> prop, Event event) {
		if(event.getTimeUpdate() != null) {
			prop.add(new LastModified(new DateTime(event.getTimeUpdate().getTime())));
		}
	}

	private void appendSequence(PropertyList<Property> prop, Event event) {
		prop.add(new Sequence(event.getSequence()));
	}
	
	private void appendCreated(PropertyList<Property> prop, Event event) {
		if(event.getTimeCreate() != null){
			prop.add(new Created(new DateTime(event.getTimeCreate().getTime())));
		}
	}

	private void appendDurationToICS(PropertyList<Property> prop, Event event) {
		prop.add(new Duration(java.time.Duration.between(event.getStartDate().toInstant(), event.getEndDate().toInstant())));
	}

	private void appendCategoryToICS(PropertyList<Property> prop, Event event) {
		if (!isEmpty(event.getCategory())) {
			prop.add(new Categories(event.getCategory()));
		}
	}

	private void appendAttendeesToICS(PropertyList<Property> prop, List<Attendee> attendees) {
		for (final Attendee attendee: attendees) {
			prop.add(getAttendee(attendee));
		}
	}
	
	@VisibleForTesting CuType calendarUserTypeToCuType(CalendarUserType type) {
		return new CuType(type.name());
	}

	private net.fortuna.ical4j.model.property.Attendee getAttendee(
			Attendee attendee) {
		net.fortuna.ical4j.model.property.Attendee att = new net.fortuna.ical4j.model.property.Attendee();

		att.getParameters().add(calendarUserTypeToCuType(attendee.getCalendarUserType()));

		PartStat ps = getPartStat(attendee);
		att.getParameters().add(ps);

		att.getParameters().add(Rsvp.TRUE);

		Cn cn = getCn(attendee);
		att.getParameters().add(cn);

		Role role = getRole(attendee);
		att.getParameters().add(role);

		try {
			att.setValue(MAILTO + attendee.getEmail());
		} catch (URISyntaxException e) {
			logger.error(e.getMessage(), e);
		}
		return att;
	}

	private void appendUidToICS(PropertyList<Property> prop, Event event,
			EventExtId parentExtId) {
		if (parentExtId != null && parentExtId.getExtId() != null) {
			prop.add(new Uid(parentExtId.serializeToString()));
		} else if (event.getExtId() != null && event.getExtId().getExtId() != null) {
			prop.add(new Uid(event.getExtId().serializeToString()));
		} else {
			throw new InvalidParameterException();
		}
	}

	@Override
	public Date isInIntervalDate(Event event, Date start, Date end,
			Set<Date> dateExce) {
		return isInIntervalDate(event.getRecurrence(), event.getStartDate(), start,
				end, dateExce);
	}

	@Override
	public Date isInIntervalDate(EventRecurrence recurrence,
			Date eventDate, Date start, Date end, Set<Date> dateExce) {
		List<Date> dates = dateInInterval(recurrence, eventDate, start, end, dateExce);
		for (Date date : dates) {
			if ((date.after(start) || date.equals(start))
					&& (end == null || ((date.before(end) || date.equals(end))))) {
				return date;
			}
		}
		return null;

	}

	@Override
	public List<Date> dateInInterval(EventRecurrence recurrence,
			Date eventDate, Date start, Date end, Set<Date> dateExce) {
		List<Date> ret = new LinkedList<Date>();
		Recur recur = getRecur(recurrence, eventDate);
		if (recur == null) {
			ret.add(eventDate);
			return ret;
		}
		if (end == null) {
			if (start.before(eventDate)) {
				ret.add(eventDate);
				return ret;
			}
			return ImmutableList.of();
		}
		DateList dl = recur.getDates(new DateTime(eventDate), new DateTime(
				start), new DateTime(end), Value.DATE_TIME);
		for (Iterator<?> it = dl.iterator(); it.hasNext();) {
			Date evD = (Date) it.next();
			GregorianCalendar cal = new GregorianCalendar();
			cal.setTime(evD);
			cal.set(GregorianCalendar.MILLISECOND, 0);
			if (!dateExce.contains(cal.getTime())) {
				ret.add(evD);
			}
		}
		return ret;
	}

	@VisibleForTesting Recur getRecur(EventRecurrence eventRecurrence, Date eventStartDate) {
		Recur recur = null;
		if (eventRecurrence.isRecurrent()) {
			RecurrenceKind recurrenceKind = eventRecurrence.getKind();

			if (!RECURRENCEKIND_TO_RECUR.containsKey(recurrenceKind)) {
				throw new IllegalRecurrenceKindException(recurrenceKind);
			}

			Frequency recurFrequency = RECURRENCEKIND_TO_RECUR.get(recurrenceKind);

			recur = getRecurFrom(eventRecurrence, recurFrequency);
			if (RecurrenceKind.monthlybyday.equals(recurrenceKind)) {
				addMonthlyOffsetToRecurDayList(eventStartDate, recur);
			}
			if (eventRecurrence.getFrequence() > 0) {
				recur.setInterval(eventRecurrence.getFrequence());
			}
			setRecurDayList(eventRecurrence, recur);
		}
		return recur;
	}

	private void addMonthlyOffsetToRecurDayList(Date eventStartDate, Recur recur) {
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(eventStartDate);
		recur.getDayList().add(WeekDay.getMonthlyOffset(cal));
	}

	private void setRecurDayList(EventRecurrence eventRecurrence, Recur recur) {
		Set<WeekDay> listDay = getListDay(eventRecurrence);
		for (WeekDay weekDay : listDay) {
			recur.getDayList().add(weekDay);
		}
	}

	private Recur getRecurFrom(EventRecurrence eventRecurrence, Frequency recurFrequency) {
		if (eventRecurrence.getEnd() == null) {
			return new Recur(recurFrequency, null);
		} else {
			GregorianCalendar cal = new GregorianCalendar();
			cal.setTime(eventRecurrence.getEnd());
			cal.set(GregorianCalendar.SECOND, 0);
			return new Recur(recurFrequency, new DateTime(cal.getTime()));
		}
	}

	@VisibleForTesting Set<WeekDay> getListDay(EventRecurrence eventRecurrence) {
		Set<WeekDay> listDay = new HashSet<WeekDay>();
		RecurrenceDays recurrenceDays = eventRecurrence.getDays();
		for (RecurrenceDay recurrenceDay : recurrenceDays) {
			WeekDay weekDay = RECURRENCE_DAY_TO_WEEK_DAY.get(recurrenceDay);
			if (weekDay == null) {
				throw new IllegalArgumentException("Unknown recurrence day " + recurrenceDay);
			}
			listDay.add(weekDay);
		}
		return listDay;
	}

	private int getDuration(DtStart startDate, DateProperty endDate) {
		if (startDate != null && endDate != null) {
			long start = startDate.getDate().getTime();
			long end = endDate.getDate().getTime();
			return (int) ((end - start) / 1000);
		}
		return 0;
	}

	private void appendAlert(Event event, ComponentList<VAlarm> cl) {
		if (cl.size() > 0) {
			
			final VAlarm valarm = (VAlarm) cl.get(0);
			if (valarm != null) {

				if ((isVAlarmRepeat(valarm) && valarm.getDuration() != null)
					|| (!isVAlarmRepeat(valarm) && valarm.getDuration() == null)) {
					final Trigger trigger = valarm.getTrigger();
					
					java.time.Duration dur = (java.time.Duration) trigger.getDuration();
					java.time.Duration durZero = java.time.Duration.ofNanos(0);
					
					if (dur == null || dur.equals(durZero)) {
						event.setAlert(null);
						return;
					} else if (dur.isNegative()) {
						dur = dur.abs();
					}
					
					event.setAlert(Long.valueOf(dur.getSeconds()).intValue());	
					return;
				}
			}
		} else {
			event.setAlert(null);
		}
	}

	private boolean isVAlarmRepeat(final VAlarm valarm) {
		final Repeat repeat = valarm.getRepeat();
		if (repeat != null) {
			return true;
		}
		return false;
	}

	private void appendRecurence(Event event, CalendarComponent component) {
		EventRecurrence er = new EventRecurrence();
		RRule rrule = (RRule) component.getProperty(Property.RRULE);
		EnumSet<RecurrenceDay> recurrenceDays = EnumSet.noneOf(RecurrenceDay.class);

		if (rrule != null) {
			Recur recur = rrule.getRecur();
			Frequency frequency = recur.getFrequency();

			if (Frequency.WEEKLY.equals(frequency) || Frequency.DAILY.equals(frequency)) {
				for (Object ob : recur.getDayList()) {
					recurrenceDays.add(weekDayToRecurrenceDay((WeekDay) ob));
				}

				if (Frequency.WEEKLY.equals(frequency) && recurrenceDays.isEmpty()) {
					GregorianCalendar cal = getEventStartCalendar(event);
					WeekDay eventStartWeekDay = WeekDay.getDay(cal.get(GregorianCalendar.DAY_OF_WEEK));

					recurrenceDays.add(WEEK_DAY_TO_RECURRENCE_DAY.get(eventStartWeekDay));
				}
			}

			er.setDays(new RecurrenceDays(recurrenceDays));
			er.setEnd(recur.getUntil());
			er.setFrequence(Math.max(recur.getInterval(), 1)); // getInterval() returns -1 if no interval is defined

			if (er.getDays().isEmpty()) {
				if (Frequency.DAILY.equals(frequency)) {
					er.setKind(RecurrenceKind.daily);
				} else if (Frequency.WEEKLY.equals(frequency)) {
					er.setKind(RecurrenceKind.weekly);
				} else if (Frequency.MONTHLY.equals(frequency)) {
					WeekDayList wdl = recur.getDayList();

					if (wdl.size() > 0) {
						WeekDay day = (WeekDay) wdl.get(0);
						GregorianCalendar cal = getEventStartCalendar(event);

						er.setKind(RecurrenceKind.monthlybyday);
						cal.set(GregorianCalendar.DAY_OF_WEEK, WeekDay.getCalendarDay(day));
						cal.set(GregorianCalendar.DAY_OF_WEEK_IN_MONTH, day.getOffset());
						event.setStartDate(cal.getTime());
					} else {
						er.setKind(RecurrenceKind.monthlybydate);
					}

				} else if (Frequency.YEARLY.equals(frequency)) {
					er.setKind(RecurrenceKind.yearly);
				}
			} else {
				er.setKind(RecurrenceKind.weekly);
			}
		}

		event.setRecurrence(er);

		appendNegativeExceptions(event, component.getProperties(Property.EXDATE));
	}

	private void appendNegativeExceptions(Event event, PropertyList<Property> exdates) {
		for (Object ob : exdates) {
			for (Object date : ((ExDate) ob).getDates()) {
				event.getRecurrence().addException((Date) date);
			}
		}
	}

	private RecurrenceDay weekDayToRecurrenceDay(WeekDay weekDay) {
		RecurrenceDay recurrenceDay = WEEK_DAY_TO_RECURRENCE_DAY.get(weekDay);

		if (recurrenceDay == null) {
			throw new IllegalArgumentException("Unknown week day " + weekDay);
		}

		return recurrenceDay;
	}

	private GregorianCalendar getEventStartCalendar(Event event) {
		GregorianCalendar cal = new GregorianCalendar();

		cal.setTime(event.getStartDate());

		return cal;
	}

	private void appendAttendees(Event event, Component vEvent, ObmDomain domain, Integer ownerId, Cache<String, Optional<Attendee>> cache) {
		Map<String, Attendee> emails = new HashMap<String, Attendee>();
		
		for (Property prop : getProperties(vEvent, Property.ATTENDEE)) {
			Attendee att = convertAttendeePropertyToAttendee(prop, domain, ownerId, cache);
			
			if (att == null) {
				logger.warn("Couldn't find an attendee matching {}, skipping.", prop);
				
				continue;
			}
			
			if (att.getEmail() != null && !attendeeAlreadyExist(emails, att)) {
				emails.put(att.getEmail(), att);
			}
		}
		
		appendOrganizer(emails, vEvent, domain, ownerId, cache);
		event.addAttendees(new ArrayList<Attendee>(emails.values()));
	}

	private boolean attendeeAlreadyExist(Map<String, Attendee> emails, Attendee att) {
		return emails.containsKey(att.getEmail());
	}

	private void appendOrganizer(Map<String, Attendee> emails,
			Component vEvent, ObmDomain domain, Integer ownerId, Cache<String, Optional<Attendee>> cache) {
		Property prop = vEvent.getProperty(Property.ORGANIZER);
		if(prop != null){
			Organizer orga = (Organizer) prop;
			if(orga.getValue() != null){
				String email = removeMailto(orga);
				
				Attendee organizer = emails.get(email);
				if(organizer != null){
					organizer.setOrganizer(true);
				} else {
					organizer = convertAttendeePropertyToAttendee(prop, domain, ownerId, cache);
					
					organizer.setParticipationRole(ParticipationRole.REQ);
					organizer.setParticipation(Participation.accepted());
					organizer.setOrganizer(true);
					
					emails.put(organizer.getEmail(), organizer);
				}
			}
		}
	}

	private String removeMailto(Property prop) {
		String email = extractEmail(prop);
		
		return MoreObjects.firstNonNull(email, prop.getValue());
	}
	
	private String extractEmail(Property prop) {
		String value = prop.getValue();
		int mailIndex = value.toLowerCase().indexOf(MAILTO);
		
		if (mailIndex != -1) {
			return value.substring(mailIndex + MAILTO.length());
		}
		
		return null;
	}

	private Calendar initCalendar() {
		Calendar calendar = new Calendar();

		calendar.getProperties().add(
				new ProdId("-//Aliasource Groupe LINAGORA//OBM Calendar //FR"));
		calendar.getProperties().add(Version.VERSION_2_0);
		calendar.getProperties().add(CalScale.GREGORIAN);

		return calendar;
	}

	@VisibleForTesting ExDate getExDate(Event event) {
		if (eventHasExceptions(event)) {
			return buildExDate(event.getRecurrence());
		} else {
			return null;
		}
	}

	private boolean eventHasExceptions(Event event) {
		if (event.isRecurrent() && event.getRecurrence().hasException() || event.getRecurrence().hasEventException()) {
			return true;
		}
		return false;
	}

	private ExDate buildExDate(EventRecurrence eventRecurrence) {
		DateList exceptionDates = new DateList();
		exceptionDates.setUtc(true);

		for (Date exceptionDeleted : eventRecurrence.getExceptions()) {
			exceptionDates.add(new DateTime(exceptionDeleted));
		}
		return new ExDate(exceptionDates);
	}

	/* package */ VAlarm getVAlarm(Integer alert) {
		if (alert != null && alert != 0l) {
			VAlarm va = new VAlarm(java.time.Duration.ofSeconds(-alert));
			va.getProperties().add(Action.DISPLAY);
			va.getProperties().add(new Description("Default Obm Description"));
			Trigger ti = va.getTrigger();
			ti.getParameters().add(new Value("DURATION"));
			return va;
		}
		return null;
	}

	/* package */ Clazz getClazz(EventPrivacy privacy) {
		return MoreObjects.firstNonNull(PRIVACY_TO_CLASSIFICATION.get(privacy), Clazz.PUBLIC);
	}

	/* package */ Organizer getOrganizer(String owner, String ownerEmail) {
		Organizer orga = new Organizer();
		try {
			if (owner != null && !"".equals(owner)) {
				orga.getParameters().add(new Cn(owner));
			}
			if (ownerEmail != null && !"".equals(ownerEmail)) {
				orga.setValue(MAILTO + ownerEmail);
			}
		} catch (URISyntaxException e) {
			logger.error(e.getMessage(), e);
		}
		return orga;
	}

	/* package */ Transp getTransp(EventOpacity eo) {
		Transp transp = Transp.OPAQUE;
		if (EventOpacity.OPAQUE.equals(eo)) {
			transp = Transp.OPAQUE;
		} else if (EventOpacity.TRANSPARENT.equals(eo)) {
			transp = Transp.TRANSPARENT;
		}
		return transp;
	}

	private Due getDue(Date start, int duration) {
		if (start != null && duration >= 0) {
			int durationInMS = duration * 1000;
			DateTime dateTimeEnd = new DateTime(start.getTime() + durationInMS);
			return new Due(dateTimeEnd);
		}
		return null;
	}

	/* package */ DtEnd getDtEnd(Date start, int duration) {
		if (start != null && duration >= 0) {
			net.fortuna.ical4j.model.Date dateTimeEnd =
					new DateTime(start.getTime() + duration * 1000);
			return new DtEnd(dateTimeEnd, true);
		}
		return null;
	}

	@VisibleForTesting Duration getDuration(Date startDate, Date endDate) {
		return new Duration(startDate, endDate);
	}

	private RecurrenceId getRecurrenceId(Event event) {
		net.fortuna.ical4j.model.Date dt = null;
		dt = new DateTime(event.getRecurrenceId());
		return new RecurrenceId(dt);
	}

	/* package */ Role getRole(Attendee attendee) {
		Role role = Role.OPT_PARTICIPANT;
		if (ParticipationRole.CHAIR.equals(attendee.getParticipationRole())) {
			role = Role.CHAIR;
		} else if (ParticipationRole.NON.equals(attendee.getParticipationRole())) {
			role = Role.NON_PARTICIPANT;
		} else if (ParticipationRole.OPT.equals(attendee.getParticipationRole())) {
			role = Role.OPT_PARTICIPANT;
		} else if (ParticipationRole.REQ.equals(attendee.getParticipationRole())) {
			role = Role.REQ_PARTICIPANT;
		}

		return role;
	}

	/* package */ Cn getCn(Attendee attendee) {
		if (isEmpty(attendee.getDisplayName())) {
			return new Cn(attendee.getEmail());
		}
		return new Cn(attendee.getDisplayName());
	}

	/* package */ PartStat getPartStat(Attendee attendee) {
		PartStat partStat = PartStat.NEEDS_ACTION;
		if (Participation.accepted().equals(attendee.getParticipation())) {
			partStat = PartStat.ACCEPTED;
		} else if (Participation.completed().equals(attendee.getParticipation())) {
			partStat = PartStat.COMPLETED;
		} else if (Participation.declined().equals(attendee.getParticipation())) {
			partStat = PartStat.DECLINED;
		} else if (Participation.delegated().equals(attendee.getParticipation())) {
			partStat = PartStat.DELEGATED;
		} else if (Participation.inProgress().equals(attendee.getParticipation())) {
			partStat = PartStat.IN_PROCESS;
		} else if (Participation.needsAction().equals(attendee.getParticipation())) {
			partStat = PartStat.NEEDS_ACTION;
		} else if (Participation.tentative().equals(attendee.getParticipation())) {
			partStat = PartStat.TENTATIVE;
		}
		return partStat;
	}

	/* package */ RRule getRRule(Event event) {
		RRule rrule = null;
		Recur recur = getRecur(event.getRecurrence(), event.getStartDate());
		if (recur != null) {
			rrule = new RRule(recur);
		}
		return rrule;
	}

	/* package */ ComponentList<VEvent> getComponents(Calendar calendar,
			String component) {
		return calendar.getComponents(component);
	}

	private List<Property> getProperties(Component comp, String property) {
		List<Property> propsSet = new ArrayList<Property>();
		PropertyList<Property> propList = comp.getProperties(property);
		for (Iterator<Property> it = propList.iterator(); it.hasNext();) {
			Property prop = it.next();
			propsSet.add(prop);
		}
		return propsSet;
	}

	private boolean isEmpty(String st) {
		return st == null || "".equals(st);
	}

	private FreeBusyRequest getFreeBusy(VFreeBusy vFreeBusy, ObmDomain domain, Integer ownerId) {
		FreeBusyRequest fb = new FreeBusyRequest();
		appendOwner(fb, vFreeBusy.getOrganizer());
		fb.setUid(vFreeBusy.getUid().getValue());
		if (vFreeBusy.getStartDate() != null) {
			fb.setStart(vFreeBusy.getStartDate().getDate());
		}

		if (vFreeBusy.getEndDate() != null) {
			fb.setEnd(vFreeBusy.getEndDate().getDate());
		}

		appendAttendees(fb, vFreeBusy, domain, ownerId, CacheBuilder.newBuilder().<String, Optional<Attendee>>build());

		return fb;
	}
	
	private String getParameterValue(Parameter parameter) {
		if (parameter == null) {
			return null;
		}
		
		return parameter.getValue();
	}
	
	@VisibleForTesting Attendee findAttendeeUsingCuType(String name, String email, String cuType, ObmDomain domain, Integer ownerId) {
		Attendee attendee = null;
		
		if (cuType == null) {
			attendee = attendeeService.findAttendee(name, email, true, domain, ownerId);
		} else {
			CalendarUserType type = CalendarUserType.valueOf(cuType);
			
			switch (type) {
				case GROUP:
					break;
				case INDIVIDUAL:
					attendee = attendeeService.findUserAttendee(name, email, domain);
					
					if (attendee == null) {
						attendee = attendeeService.findContactAttendee(name, email, true, domain, ownerId);
					}
					
					break;
				case ROOM:
				case RESOURCE:
					attendee = attendeeService.findResourceAttendee(name, email, domain, ownerId);
					
					break;
				case UNKNOWN:
					attendee = attendeeService.findAttendee(name, email, true, domain, ownerId);
			}
		}
		
		return attendee;
	}

	private Attendee findAttendee(final String email, final String cn, final String cuType, final ObmDomain domain, final Integer ownerId, Cache<String, Optional<Attendee>> cache) {
		try {
			return cache.get(email, new Callable<Optional<Attendee>>() {
				@Override
				public Optional<Attendee> call() throws Exception {
					return Optional.fromNullable(findAttendeeUsingCuType(cn, email, cuType, domain, ownerId));
				}
			}).transform(new Function<Attendee, Attendee>() {
				@Override
				public Attendee apply(Attendee input) {
					return input.clone();
				}
			}).orNull();
		}
		catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	private Attendee convertAttendeePropertyToAttendee(Property prop, ObmDomain domain, Integer ownerId, Cache<String, Optional<Attendee>> cache) {
		String email = removeMailto(prop);
		String cn = getParameterValue(prop.getParameter(Parameter.CN));
		String cuType = getParameterValue(prop.getParameter(Parameter.CUTYPE));
		Attendee attendee = findAttendee(email, cn, cuType, domain, ownerId, cache);

		// Couldn't find a resource matching the attendee...
		if (attendee == null) {
			return null;
		}
		
		String role = getParameterValue(prop.getParameter(Parameter.ROLE));
		String partStat = getParameterValue(prop.getParameter(Parameter.PARTSTAT));
		
		if (cn != null) {
			attendee.setDisplayName(cn);
		}

		if (role != null) {
			int dashIndex = role.indexOf("-");
			
			if (dashIndex != -1) {
				attendee.setParticipationRole(ParticipationRole.valueOf(role.substring(0, dashIndex)));
			} else if (ParticipationRole.CHAIR.name().equalsIgnoreCase(role)) {
				attendee.setParticipationRole(ParticipationRole.CHAIR);
			}
		}

		if (partStat != null) {
			if (partStat.equals(PartStat.IN_PROCESS.getValue())) {
				attendee.setParticipation(Participation.inProgress());
			} else {
				attendee.setParticipation(Participation.getValueOf(partStat));
			}
		} else {
			//rfc5545 : 3.2.12, if PART-STAT is missing, default is NEEDS-ACTION
			attendee.setParticipation(Participation.needsAction());
		}
		
		return attendee;
	}

	private void appendAttendees(FreeBusyRequest fb, VFreeBusy vFreeBusy, ObmDomain domain, Integer ownerId, Cache<String, Optional<Attendee>> cache) {
		List<Property> props = getProperties(vFreeBusy, Property.ATTENDEE);
		
		for (Property prop : props) {
			fb.addAttendee(convertAttendeePropertyToAttendee(prop, domain, ownerId, cache));
		}
	}

	private void appendOwner(FreeBusyRequest fb, Organizer organizer) {
		if (organizer != null) {
			Parameter cn = organizer.getParameter(Parameter.CN);
			String cnOrganizer = "";
			if (cn != null) {
				cnOrganizer = cn.getValue();
			}

			if (cnOrganizer != null && !"".equals(cnOrganizer)) {
				fb.setOwner(cnOrganizer);
			} else {
				int mailToIndex = organizer.getValue().toLowerCase()
						.indexOf(MAILTO);
				if (mailToIndex != -1) {
					fb.setOwner(organizer.getValue().substring(
							mailToIndex + MAILTO.length()));
				}
			}
		}
	}

	public String parseFreeBusy(FreeBusy fb) {
		Calendar calendar = initCalendar();
		calendar.getProperties().add(Method.REPLY);
		VFreeBusy vFreeBusy = getVFreeBusy(fb, fb.getAtt(),	fb.getFreeBusyIntervals());
		calendar.getComponents().add(vFreeBusy);

		return calendar.toString();
	}

	private VFreeBusy getVFreeBusy(FreeBusy fb, Attendee att, Set<FreeBusyInterval> fbls) {
		VFreeBusy vfb = new VFreeBusy();
		Organizer orga = getOrganizer("", fb.getOwner());
		vfb.getProperties().add(orga);

		DtStart st = getDtStart(fb.getStart());
		vfb.getProperties().add(st);

		DtEnd en = getDtEnd(fb.getEnd());
		vfb.getProperties().add(en);

		net.fortuna.ical4j.model.property.Attendee at = getAttendee(att);
		vfb.getProperties().add(at);

		if (fb.getUid() != null && !"".equals(fb.getUid())) {
			vfb.getProperties().add(new Uid(fb.getUid()));
		}

		for (FreeBusyInterval line : fbls) {
			DtStart start = getDtStart(line.getStart());
			DtEnd end = getDtEnd(line.getStart(), line.getDuration());
			if (start != null && end != null) {
				net.fortuna.ical4j.model.property.FreeBusy fbics = new net.fortuna.ical4j.model.property.FreeBusy();
				Period p = new Period(new DateTime(start.getDate()), new DateTime(end.getDate()));
				fbics.getPeriods().add(p);
				fbics.getParameters().add(FbType.BUSY);
				vfb.getProperties().add(fbics);
			}
		}
		return vfb;
	}

	private DtEnd getDtEnd(Date end) {
		return new DtEnd(new DateTime(end), true);
	}

	@VisibleForTesting DtStart getDtStart(Date start) {
		return new DtStart(new DateTime(start), true);
	}
	
	@Override
	public Timestamp timestampFromDateString(String dateAsString) throws ParseException {
		return new Timestamp(new DateTime(dateAsString).getTime());
	}

	public Set<Resource> parseResources(String ics) throws IOException, ParserException {
		return FluentIterable
			.from(getComponents(buildCalendar(ics), Component.VEVENT))
			.transformAndConcat(new Function<VEvent, Iterable<Property>>() {

				@Override
				public Iterable<Property> apply(VEvent vEvent) {
					return getProperties(vEvent, Property.ATTENDEE);
				}
				
			}).filter(new Predicate<Property>() {

				@Override
				public boolean apply(Property prop) {
					String cuType = getParameterValue(prop.getParameter(Parameter.CUTYPE));
					return CalendarUserType.RESOURCE.name().equalsIgnoreCase(cuType);
				}
				
			}).transform(new Function<Property, Resource>() {

				@Override
				public Resource apply(Property prop) {
					return Resource.builder()
						.name(getParameterValue(prop.getParameter(Parameter.CN)))
						.mail(removeMailto(prop))
						.build();
				}
				
			})
			.toSet();
	}
}
