/* ***** BEGIN LICENSE BLOCK *****
 * Copyright (C) 2013-2014  Linagora
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
package org.obm.provisioning.beans;

import org.junit.Before;
import org.junit.Test;
import org.obm.provisioning.Group;
import org.obm.provisioning.GroupExtId;
import org.obm.provisioning.ProfileId;
import org.obm.provisioning.ProfileName;
import org.obm.sync.bean.EqualsVerifierUtils;
import org.obm.sync.book.Website;

import fr.aliacom.obm.common.domain.ObmDomain;
import fr.aliacom.obm.common.domain.ObmDomainUuid;
import fr.aliacom.obm.common.user.ObmUser;
import fr.aliacom.obm.common.user.UserEmails;
import fr.aliacom.obm.common.user.UserLogin;
import fr.aliacom.obm.common.user.UserNomad;


public class BeansTest {

	private EqualsVerifierUtils equalsVerifierUtilsTest;

	@Before
	public void init() {
		equalsVerifierUtilsTest = new EqualsVerifierUtils();
	}

	@Test
	public void test() {
		equalsVerifierUtilsTest.test(
				GroupExtId.class,
				ProfileId.class,
				ProfileName.class,
				ObmDomainUuid.class,
				UserNomad.class,
				Website.class);
	}

	@Test
	public void testGroups() {
		ObmDomain obmDotOrgDomain = ObmDomain.builder()
				.id(3)
				.name("obm.org")
				.build();
			ObmDomain ibmDotComDomain = ObmDomain.builder()
				.id(5)
				.name("ibm.com")
				.build();
		EqualsVerifierUtils
			.createEqualsVerifier(Group.class)
			.withIgnoredFields("timecreate", "timeupdate")
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
					Group.builder().uid(Group.Id.valueOf(1)).build(),
					Group.builder().uid(Group.Id.valueOf(2)).build())
			.verify();
	}
}
