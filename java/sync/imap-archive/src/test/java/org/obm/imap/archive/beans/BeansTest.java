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

package org.obm.imap.archive.beans;

import java.time.ZonedDateTime;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.junit.Test;
import org.obm.imap.archive.mailbox.MailboxPaths;
import org.obm.sync.bean.EqualsVerifierUtils.EqualsVerifierBuilder;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

public class BeansTest {

	@Test
	public void beanShouldRespectBeanContract() {
		EqualsVerifierBuilder.builder()
			.prefabValue(ZonedDateTime.class, ZonedDateTime.parse("2020-10-01T23:32Z"), ZonedDateTime.parse("2020-10-01T12:22Z"))
			.prefabValue(Logger.class, (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("1"), (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("2"))
			.prefabValue(ConcurrentLinkedDeque.class, new ConcurrentLinkedDeque<String>(), new ConcurrentLinkedDeque<String>())
			.equalsVerifiers(
				ArchiveRecurrence.class,
				DayOfMonth.class,
				DayOfYear.class,
				DomainConfiguration.class,
				PersistedResult.class,
				SchedulingConfiguration.class,
				SchedulingDates.class,
				ArchiveConfiguration.class,
				ArchiveTreatment.class,
				ArchiveScheduledTreatment.class,
				ArchiveRunningTreatment.class,
				ArchiveTerminatedTreatment.class,
				ArchiveTreatmentRunId.class,
				ImapFolder.class,
				ProcessedFolder.class,
				HigherBoundary.class,
				Year.class,
				Limit.class,
				ScopeUser.class,
				Mailing.class,
				MailboxPaths.class,
				MappedMessageSets.class,
				SharedMailbox.class
			).verify();
	}	
}
