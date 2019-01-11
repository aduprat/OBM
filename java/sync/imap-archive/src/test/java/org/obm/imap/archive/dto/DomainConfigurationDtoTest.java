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

package org.obm.imap.archive.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.UUID;

import org.assertj.core.data.MapEntry;
import org.junit.Test;
import org.obm.imap.archive.beans.ArchiveRecurrence;
import org.obm.imap.archive.beans.ConfigurationState;
import org.obm.imap.archive.beans.DayOfMonth;
import org.obm.imap.archive.beans.DayOfWeek;
import org.obm.imap.archive.beans.DayOfYear;
import org.obm.imap.archive.beans.DomainConfiguration;
import org.obm.imap.archive.beans.Mailing;
import org.obm.imap.archive.beans.RepeatKind;
import org.obm.imap.archive.beans.SchedulingConfiguration;
import org.obm.imap.archive.beans.ScopeUser;
import org.obm.imap.archive.beans.SharedMailbox;
import org.obm.sync.base.EmailAddress;

import com.google.common.collect.ImmutableList;

import fr.aliacom.obm.common.domain.ObmDomain;
import fr.aliacom.obm.common.domain.ObmDomainUuid;
import fr.aliacom.obm.common.user.UserExtId;


public class DomainConfigurationDtoTest {

	@Test
	public void fromDomainConfigurationShouldCopyAllFields() {
		DomainConfiguration configuration = 
				DomainConfiguration.builder()
					.domain(ObmDomain.builder().uuid(ObmDomainUuid.of("e953d0ab-7053-4f84-b83a-abfe479d3888")).build())
					.state(ConfigurationState.DISABLE)
					.schedulingConfiguration(SchedulingConfiguration.builder()
							.recurrence(ArchiveRecurrence.builder()
								.repeat(RepeatKind.DAILY)
								.dayOfMonth(DayOfMonth.of(12))
								.dayOfWeek(DayOfWeek.FRIDAY)
								.dayOfYear(DayOfYear.of(234))
								.build())
							.time(LocalTime.parse("13:23"))
							.build())
					.archiveMainFolder("arChive")
					.excludedFolder("excluded")
					.scopeUsersIncludes(true)
					.scopeUsers(ImmutableList.of(ScopeUser.builder()
								.id(UserExtId.valueOf("08607f19-05a4-42a2-9b02-6f11f3ceff3b"))
								.login("user")
								.build()))
					.scopeSharedMailboxesIncludes(true)
					.scopeSharedMailboxes(ImmutableList.of(SharedMailbox.builder()
								.id(1)
								.name("shared")
								.build()))
					.mailing(Mailing.from(ImmutableList.of(EmailAddress.loginAtDomain("usera@mydomain.org"), EmailAddress.loginAtDomain("userb@mydomain.org"))))
					.build();
		DomainConfigurationDto dto = DomainConfigurationDto.from(configuration);
		assertThat(dto.domainId).isEqualTo(UUID.fromString("e953d0ab-7053-4f84-b83a-abfe479d3888"));
		assertThat(dto.enabled).isFalse();
		assertThat(dto.repeatKind).isEqualTo("DAILY");
		assertThat(dto.dayOfMonth).isEqualTo(12);
		assertThat(dto.dayOfWeek).isEqualTo(5);
		assertThat(dto.dayOfYear).isEqualTo(234);
		assertThat(dto.hour).isEqualTo(13);
		assertThat(dto.minute).isEqualTo(23);
		assertThat(dto.archiveMainFolder).isEqualTo("arChive");
		assertThat(dto.excludedFolder).isEqualTo("excluded");
		assertThat(dto.scopeUsersIncludes).isTrue();
		assertThat(dto.scopeUserIdToLoginMap).containsExactly(MapEntry.entry("08607f19-05a4-42a2-9b02-6f11f3ceff3b", "user"));
		assertThat(dto.scopeSharedMailboxesIncludes).isTrue();
		assertThat(dto.scopeSharedMailboxIdToNameMap).containsExactly(MapEntry.entry(1, "shared"));
		assertThat(dto.mailingEmails).containsOnly("usera@mydomain.org", "userb@mydomain.org");
	}
	
}
