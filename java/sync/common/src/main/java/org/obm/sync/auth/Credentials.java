/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2011-2012  Linagora
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
package org.obm.sync.auth;

import com.google.common.base.Preconditions;
import com.google.common.base.Objects;

public class Credentials {

	public static Builder builder() {
		return new Builder();
	}
	
	public static class Builder {
		
		private Login.Builder loginBuilder;
		private String password;
		private boolean hashedPassword;

		private Builder() {
			hashedPassword = false;
			loginBuilder = Login.builder();
		}
		
		public Builder login(Login login) {
			loginBuilder.from(login);
			return this;
		}
		
		public Builder login(String login) {
			loginBuilder.login(login);
			return this;
		}

		public Builder domain(String domain) {
			loginBuilder.domain(domain);
			return this;
		}
		
		public Builder password(String password) {
			this.password = password;
			this.hashedPassword = false;
			return this;
		}
		
		public Builder hashedPassword(String password) {
			this.password = password;
			this.hashedPassword = true;
			return this;
		}
		
		public Credentials build() {
			Preconditions.checkState(password != null, "'password' is mandatory");
			return new Credentials(loginBuilder.build(), password, hashedPassword);
		}

	}

	private final Login login;
	private final String password;
	private final boolean hashedPassword;

	public Credentials(Login login, String password, boolean hashedPassword) {
		this.login = login;
		this.password = password;
		this.hashedPassword = hashedPassword;
	}

	public Login getLogin() {
		return login;
	}
	
	public String getPassword() {
		return password;
	}
	
	public boolean isPasswordHashed() {
		return hashedPassword;
	}

	@Override
	public int hashCode(){
		return Objects.hashCode(login, password, hashedPassword);
	}
	
	@Override
	public boolean equals(Object object){
		if (object instanceof Credentials) {
			Credentials that = (Credentials) object;
			return Objects.equal(this.login, that.login)
				&& Objects.equal(this.password, that.password)
				&& Objects.equal(this.hashedPassword, that.hashedPassword);
		}
		return false;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
			.add("login", login)
			.add("password", "XXXXXXXX")
			.add("hashedPassword", hashedPassword)
			.toString();
	}
	
	
}
