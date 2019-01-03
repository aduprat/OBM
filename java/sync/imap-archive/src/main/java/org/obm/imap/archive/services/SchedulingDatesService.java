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

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;

import javax.inject.Inject;

import org.obm.imap.archive.beans.RepeatKind;
import org.obm.imap.archive.beans.SchedulingConfiguration;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;
import com.linagora.scheduling.DateTimeProvider;

@Singleton
public class SchedulingDatesService {

	private final DateTimeProvider dateProvider;

	@Inject
	@VisibleForTesting SchedulingDatesService(DateTimeProvider dateProvider) {
		this.dateProvider = dateProvider;
	}
	
	public ZonedDateTime nextTreatmentDate(SchedulingConfiguration schedulingConfiguration) {
		ZonedDateTime currentDateTime = dateProvider.now();
		ZonedDateTime currentDateWithScheduledTime = currentDateTime
				.withZoneSameInstant(ZoneId.of(ZoneOffset.UTC.getId()))
				.withHour(schedulingConfiguration.getHour())
				.withMinute(schedulingConfiguration.getMinute())
				.withSecond(0)
				.withNano(0);

		switch (schedulingConfiguration.getRepeatKind()) {
		case DAILY:
			return dailyNextTreatmentDate(currentDateTime, currentDateWithScheduledTime);
			
		case WEEKLY:
			return weeklyNextTreatmentDate(schedulingConfiguration, currentDateTime, currentDateWithScheduledTime);

		case MONTHLY:
			return monthlyNextTreatmentDate(schedulingConfiguration, currentDateTime, currentDateWithScheduledTime);
			
		case YEARLY:
			return yearlyNextTreatmentDate(schedulingConfiguration, currentDateTime, currentDateWithScheduledTime);
		
		default:
			throw new IllegalArgumentException("Unknown repeat kind: " + schedulingConfiguration.getRepeatKind());
		}
	}

	private ZonedDateTime dailyNextTreatmentDate(ZonedDateTime currentDateTime, ZonedDateTime currentDateWithScheduledTime) {
		if (currentDateWithScheduledTime.isAfter(currentDateTime)) {
			return currentDateWithScheduledTime;
		}
		return currentDateWithScheduledTime
				.plusDays(1);
	}

	private ZonedDateTime weeklyNextTreatmentDate(SchedulingConfiguration schedulingConfiguration, ZonedDateTime currentDateTime, ZonedDateTime currentDateWithScheduledTime) {
		ZonedDateTime dayOfWeek = currentDateWithScheduledTime
				.with(ChronoField.DAY_OF_WEEK, schedulingConfiguration.getDayOfWeek().getSpecificationValue());
		if (dayOfWeek.isAfter(currentDateTime)) {
			return dayOfWeek;
		}
		return dayOfWeek
				.plusWeeks(1);
	}

	private ZonedDateTime monthlyNextTreatmentDate(SchedulingConfiguration schedulingConfiguration, ZonedDateTime currentDateTime, ZonedDateTime currentDateWithScheduledTime) {
		if (schedulingConfiguration.isLastDayOfMonth()) {
			return nextTreatmentDateOnLastDayOfMonth(currentDateTime, currentDateWithScheduledTime);
		}
		
		return nextTreatmentDateCommonDayOfMonth(schedulingConfiguration, currentDateTime, currentDateWithScheduledTime);
	}

	private ZonedDateTime nextTreatmentDateOnLastDayOfMonth(ZonedDateTime currentDateTime, ZonedDateTime currentDateWithScheduledTime) {
		ZonedDateTime dayOfMonth = lastDayOfMonth(currentDateWithScheduledTime);
		
		if (dayOfMonth.isAfter(currentDateTime)) {
			return dayOfMonth;
		}
		// currentDateTime is at the end of the month, but on a higher time -> next month 
		return lastDayOfMonth(currentDateWithScheduledTime.plusMonths(1));
	}

	private ZonedDateTime lastDayOfMonth(ZonedDateTime currentDateWithScheduledTime) {
		return currentDateWithScheduledTime
			.plusMonths(1)
			.withDayOfMonth(1)
			.minusDays(1);
	}

	private ZonedDateTime nextTreatmentDateCommonDayOfMonth(SchedulingConfiguration schedulingConfiguration, ZonedDateTime currentDateTime, ZonedDateTime currentDateWithScheduledTime) {
		ZonedDateTime dayOfMonth = currentDateWithScheduledTime
			.withDayOfMonth(schedulingConfiguration.getDayOfMonth().getDayIndex());
		
		if (dayOfMonth.isAfter(currentDateTime)) {
			return dayOfMonth;
		}
		return dayOfMonth
				.plusMonths(1);
	}

	private ZonedDateTime yearlyNextTreatmentDate(SchedulingConfiguration schedulingConfiguration, ZonedDateTime currentDateTime, ZonedDateTime currentDateWithScheduledTime) {
		ZonedDateTime dayOfYear = currentDateWithScheduledTime
			.withDayOfYear(schedulingConfiguration.getDayOfYear().getDayOfYear());
		if (dayOfYear.isAfter(currentDateTime)) {
			return dayOfYear;
		}
		return dayOfYear
				.plusYears(1);
	}
	
	public ZonedDateTime higherBoundary(ZonedDateTime treatmentDate, RepeatKind repeatKind) {
		return treatmentDate.minus(RepeatKind.toPeriod(repeatKind, 1))
				.withHour(23)
				.withMinute(59)
				.withSecond(59)
				.withNano(999999999);
	}
}
