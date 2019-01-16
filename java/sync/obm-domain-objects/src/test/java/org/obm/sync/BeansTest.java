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
package org.obm.sync;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.obm.provisioning.Group;
import org.obm.provisioning.Group.Id;
import org.obm.sync.addition.CommitedElement;
import org.obm.sync.addition.CommitedOperation;
import org.obm.sync.auth.Credentials;
import org.obm.sync.auth.Login;
import org.obm.sync.auth.MavenVersion;
import org.obm.sync.base.DomainName;
import org.obm.sync.base.EmailAddress;
import org.obm.sync.base.EmailLogin;
import org.obm.sync.bean.EqualsVerifierUtils;
import org.obm.sync.bean.EqualsVerifierUtils.EqualsVerifierBuilder;
import org.obm.sync.book.Address;
import org.obm.sync.book.AddressBook;
import org.obm.sync.book.AddressBookCreation;
import org.obm.sync.book.AddressBookReference;
import org.obm.sync.book.Contact;
import org.obm.sync.book.DeletedContact;
import org.obm.sync.book.InstantMessagingId;
import org.obm.sync.book.Phone;
import org.obm.sync.calendar.DeletedEvent;
import org.obm.sync.calendar.Event;
import org.obm.sync.calendar.EventExtId;
import org.obm.sync.calendar.EventObmId;
import org.obm.sync.calendar.EventRecurrence;
import org.obm.sync.calendar.RecurrenceDays;
import org.obm.sync.calendar.RecurrenceId;
import org.obm.sync.calendar.RecurrenceKind;
import org.obm.sync.calendar.SyncRange;
import org.obm.sync.dao.EntityId;
import org.obm.sync.dao.Tracking;
import org.obm.sync.host.ObmHost;
import org.obm.sync.items.EventChanges;
import org.obm.sync.items.ParticipationChanges;
import org.obm.sync.serviceproperty.ServiceProperty;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import fr.aliacom.obm.common.domain.ObmDomain;
import fr.aliacom.obm.common.domain.ObmDomainUuid;
import fr.aliacom.obm.common.domain.Samba;
import fr.aliacom.obm.common.profile.ModuleCheckBoxStates;
import fr.aliacom.obm.common.profile.Profile;
import fr.aliacom.obm.common.resource.Resource;
import fr.aliacom.obm.common.system.ObmSystemUser;
import fr.aliacom.obm.common.trust.TrustToken;
import fr.aliacom.obm.common.user.ObmUser;
import fr.aliacom.obm.common.user.UserAddress;
import fr.aliacom.obm.common.user.UserEmails;
import fr.aliacom.obm.common.user.UserExtId;
import fr.aliacom.obm.common.user.UserIdentity;
import fr.aliacom.obm.common.user.UserLogin;
import fr.aliacom.obm.common.user.UserPassword;
import fr.aliacom.obm.common.user.UserPhones;
import fr.aliacom.obm.common.user.UserWork;
import nl.jqno.equalsverifier.Warning;


public class BeansTest {

	private EqualsVerifierUtils equalsVerifierUtilsTest;
	
	@Before
	public void init() {
		equalsVerifierUtilsTest = new EqualsVerifierUtils();
	}
	
	@Test
	public void test() {
		equalsVerifierUtilsTest.test(
				Credentials.class,
				DeletedEvent.class,
				Contact.class,
				TrustToken.class,
				SyncRange.class,
				EventExtId.class,
				EventObmId.class,
				RecurrenceId.class,
				Resource.class,
				EmailLogin.class,
				DomainName.class,
				Address.class,
				Phone.class,
				ParticipationChanges.class,
				InstantMessagingId.class,
				MavenVersion.class,
				UserExtId.class,
				ServiceProperty.class,
				ObmHost.class,
				ObmDomainUuid.class,
				ObmSystemUser.class,
				AddressBookReference.class,
				ModuleCheckBoxStates.class,
				EntityId.class,
				UserLogin.class,
				UserIdentity.class,
				UserAddress.class,
				UserPhones.class,
				UserWork.class,
				UserPassword.class,
				Samba.class,
				DeletedContact.class);
	}

	@Test
	public void testTrackings() {
		EqualsVerifierUtils
			.createEqualsVerifier(Tracking.class)
			.withIgnoredFields("token")
			.verify();
	}
	
	@Test
	public void testProfiles() {
		EqualsVerifierUtils
			.createEqualsVerifier(Profile.class)
			.withIgnoredFields("timecreate", "timeupdate")
			.verify();
	}
	
	@Test
	public void testAddressBooks() {
		EqualsVerifierUtils
			.createEqualsVerifier(AddressBook.class)
			.withIgnoredFields("timecreate", "timeupdate")
			.verify();
	}
	
	@Test
	public void testEmailAddresses() {
		EqualsVerifierUtils
			.createEqualsVerifier(EmailAddress.class)
			.withIgnoredFields("loginAtDomain")
			.verify();
	}
	
	@Test
	public void testLogins() {
		EqualsVerifierUtils
			.createEqualsVerifier(Login.class)
			.withIgnoredFields("fullLogin")
			.verify();
	}
	
	@Test
	public void testObmDomains() {
		EqualsVerifierUtils
			.createEqualsVerifier(ObmDomain.class)
			.withIgnoredFields("hosts", "mailChooserHookId")
			.verify();
	}
	
	@Test
	public void testEvents() {
		EqualsVerifierUtils
			.createEqualsVerifier(Event.class)
			.withPrefabValues(EventRecurrence.class,
					new EventRecurrence(RecurrenceKind.daily),
					new EventRecurrence(RecurrenceKind.monthlybydate))
			.verify();
	}

	@Test
	public void testEventChanges() {
		EqualsVerifierUtils
			.createEqualsVerifier(EventChanges.class)
			.withPrefabValues(EventRecurrence.class,
					new EventRecurrence(RecurrenceKind.daily),
					new EventRecurrence(RecurrenceKind.monthlybydate))
			.verify();
	}

	@Test
	public void testEventRecurrences() {
		Event event = new Event();
		event.setTitle("myTitle");
		EqualsVerifierUtils
			.createEqualsVerifier(EventRecurrence.class)
			.withPrefabValues(Event.class, new Event(), event)
			.verify();
	}

	@Test
	public void testUserEmails() {
		EqualsVerifierUtils
			.createEqualsVerifier(UserEmails.class)
			.withPrefabValues(ImmutableList.class,
				ImmutableList.of("addr1", "addr2"),
				ImmutableList.of("addr3", "addr4"))
			.withIgnoredFields("domain")
			.verify();
	}
	
	@Test
	public void testObmUser() {
		ObmDomain obmDotOrgDomain = ObmDomain.builder()
			.id(3)
			.name("obm.org")
			.build();
		ObmDomain ibmDotComDomain = ObmDomain.builder()
			.id(5)
			.name("ibm.com")
			.build();
		EqualsVerifierUtils
			.createEqualsVerifier(ObmUser.class)
			.withPrefabValues(ObmUser.class, 
					ObmUser.builder()
						.login(UserLogin.valueOf("creator"))
						.uid(1)
						.emails(UserEmails.builder()
							.addAddress("createdBy@obm.org")
							.domain(obmDotOrgDomain)
							.build())
						.domain(obmDotOrgDomain)
						.build(), 
					ObmUser.builder()
						.login(UserLogin.valueOf("updater"))
						.uid(1)
						.emails(UserEmails.builder()
							.addAddress("updatedBy@ibm.com")
							.domain(ibmDotComDomain)
							.build())
						.domain(ibmDotComDomain)
						.build())
			.withPrefabValues(Group.class,
					Group.builder()
						.uid(Id.valueOf(1))
						.build(),
					Group.builder()
						.uid(Id.valueOf(2))
						.build())
			.withPrefabValues(UserEmails.class,
					UserEmails.builder()
						.addAddress("createdBy@obm.org")
						.domain(obmDotOrgDomain)
						.build(),
					UserEmails.builder()
						.addAddress("updatedBy@ibm.com")
						.domain(ibmDotComDomain)
						.build())
			.withIgnoredFields("timeCreate", "timeUpdate")
			.verify();
	}
	
	@Test
	public void testWhereNullableFields() {
		EqualsVerifierUtils
			.createEqualsVerifier(RecurrenceDays.class)
			.suppress(Warning.NULL_FIELDS)
			.verify();
	}
	
	@Test
	public void testWithOptionalDate() {
		EqualsVerifierBuilder.builder()
			.suppress(Warning.REFERENCE_EQUALITY)
			.prefabValue(Optional.class, Optional.absent(), Optional.of(new Date(54)))
			.equalsVerifiers(
				CommitedElement.class,
				CommitedOperation.class
			).verify();
	}
	
	@Test
	public void testAddressBookCreation() {
		EqualsVerifierBuilder.builder()
			.suppress(Warning.REFERENCE_EQUALITY)
			.prefabValue(Optional.class, Optional.absent(), Optional.of(new AddressBookReference("ref", "origin")))
			.equalsVerifiers(
				AddressBookCreation.class
			).verify();
	}
}
