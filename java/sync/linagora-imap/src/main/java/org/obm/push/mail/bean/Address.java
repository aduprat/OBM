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

package org.obm.push.mail.bean;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public final class Address {

	private static final String ICS_MAILTO = "MAILTO:"; 
	
	private final String mail;
	private final String displayName;

	public Address() {
		this(null, null);
	}
	
	public Address(String mail) {
		this(null, mail);
	}
	
	public Address(String displayName, String mail) {
		this.displayName = formatString(displayName);
		this.mail = formatMail(mail);
	}

	private String formatString(String str) {
		if (str != null) {
			return str.replace("\"", "").replace("<", "").replace(">", "");
		}
		return str;
	}
	
	private String formatMail(String mail) {
		if (mail != null && mail.contains("@")) {
			return formatString(mail);
		}
		return mail;
	}
	
	public String getMail() {
		return mail;
	}

	public String getDisplayName() {
		return displayName;
	}

	public boolean isDefined() {
		return !Strings.isNullOrEmpty(mail);
	}
	
	public String asICSAttendee() {
		Preconditions.checkState(isDefined(), "this address cannot be serialized as ICS");
		return ICS_MAILTO + mail;
	}

	@Override
	public int hashCode(){
		return Objects.hashCode(mail, displayName);
	}
	
	@Override
	public boolean equals(Object object){
		if (object instanceof Address) {
			Address that = (Address) object;
			return Objects.equal(this.mail, that.mail)
				&& Objects.equal(this.displayName, that.displayName);
		}
		return false;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("mail", mail)
			.add("displayName", displayName)
			.toString();
	}

}
