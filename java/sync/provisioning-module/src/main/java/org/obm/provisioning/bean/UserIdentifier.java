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
package org.obm.provisioning.bean;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import fr.aliacom.obm.common.domain.ObmDomainUuid;
import fr.aliacom.obm.common.user.UserExtId;

public class UserIdentifier {

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private UserExtId id;
		private ObmDomainUuid domainUuid;

		public Builder id(UserExtId id) {
			this.id = id;
			return this;
		}

		public Builder domainUuid(ObmDomainUuid domainUuid) {
			this.domainUuid = domainUuid;
			return this;
		}

		public UserIdentifier build() {
			Preconditions.checkState(id != null);
			Preconditions.checkState(domainUuid != null);
			
			return new UserIdentifier(id, "/" + domainUuid.get() + "/users/" + id.getExtId());
		}
	}
	
	private final UserExtId id;
	private final String url;
	
	private UserIdentifier(UserExtId id, String url) {
		this.id = id;
		this.url = url;
	}
	
	public UserExtId getId() {
		return id;
	}
	
	public String getUrl() {
		return url;
	}
	
	@Override
	public final int hashCode(){
		return Objects.hashCode(id, url);
	}
	
	@Override
	public final boolean equals(Object object){
		if (object instanceof UserIdentifier) {
			UserIdentifier that = (UserIdentifier) object;
			return Objects.equal(this.id, that.id)
				&& Objects.equal(this.url, that.url);
		}

		return false;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("id", id)
				.add("url", url)
				.toString();
	}

}
