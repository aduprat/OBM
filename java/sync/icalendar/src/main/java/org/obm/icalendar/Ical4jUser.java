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

import org.obm.push.utils.UserEmailParserUtils;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import fr.aliacom.obm.common.domain.ObmDomain;

public class Ical4jUser {

	@Singleton
	public static class Factory {

		private final UserEmailParserUtils userEmailParserUtils;

		@Inject
		private Factory(UserEmailParserUtils userEmailParserUtils) {
			this.userEmailParserUtils = userEmailParserUtils;
		}
		
		public static Factory create() {
			return new Factory(new UserEmailParserUtils());
		}
		
		public Ical4jUser createIcal4jUser(String email, ObmDomain obmDomain) {
			String login = userEmailParserUtils.getLogin(email);
			String domain = userEmailParserUtils.getDomain(email);
			return new Ical4jUser(login + "@" + domain, obmDomain);
		}
		
	}
	
	private final String email;
	private final ObmDomain obmDomain;
	
	private Ical4jUser(String email, ObmDomain obmDomain) {
		this.email = email;
		this.obmDomain = obmDomain;
	}
	
	public ObmDomain getObmDomain() {
		return obmDomain;
	}
	
	public String getEmail() {
		return email;
	}

	@Override
	public final int hashCode(){
		return Objects.hashCode(email, obmDomain);
	}
	
	@Override
	public final boolean equals(Object object){
		if (object instanceof Ical4jUser) {
			Ical4jUser that = (Ical4jUser) object;
			return Objects.equal(this.email, that.email)
				&& Objects.equal(this.obmDomain, that.obmDomain);
		}
		return false;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("email", email)
			.add("obmDomain", obmDomain)
			.toString();
	}
	
}
