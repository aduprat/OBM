/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2013-2014 Linagora
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
package org.obm.provisioning.ldap.client.bean;

import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.obm.provisioning.Group;
import org.obm.provisioning.ldap.client.Configuration;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import fr.aliacom.obm.common.user.ObmUser;

public class LdapUserMembership {

	public static class Builder {
		private String memberUid;
		private String mailBox;
		private LdapDomain domain;
		private Boolean targetGroupHasEmail;
		
		private final Configuration configuration;

		@Inject
		private Builder(Configuration configuration) {
			this.configuration = configuration;
		}
		
		public Builder fromObmUser(ObmUser obmUser) {
			this.memberUid = obmUser.getLogin();
			this.mailBox = obmUser.getLoginAtDomain();
			this.domain = LdapDomain.valueOf(obmUser.getDomain().getName()); 

			return this;
		}

		public Builder targetGroupHasEmail(boolean targetGroupHasEmail) {
			this.targetGroupHasEmail = targetGroupHasEmail;
			return this;
		}

		public Builder forGroup(Group group) {
			return targetGroupHasEmail(!Strings.isNullOrEmpty(group.getEmail()));
		}

		public Builder memberUid(String memberUid) {
			this.memberUid = memberUid;
			return this;
		}
		
		public Builder mailBox(String mailBox) {
			this.mailBox = mailBox;
			return this;
		}
	
		public Builder domain(LdapDomain domain) {
			this.domain = domain;
			return this;
		}

		public LdapUserMembership build() {
			Preconditions.checkState(memberUid != null, "memberUid should not be null");
			Preconditions.checkState(domain != null, "domain should not be null");
			Preconditions.checkState(mailBox != null, "mailBox should not be null");

			Boolean groupHasEmail = MoreObjects.firstNonNull(targetGroupHasEmail, false);

			return new LdapUserMembership(memberUid, buildMember(domain), mailBox, groupHasEmail);
		}

		private String buildMember(LdapDomain domain) {
			return "uid=" + memberUid + "," + configuration.getUserBaseDn(domain).getName();
		}
	}
	
	private final String memberUid;
	private final String member;
	private final String mailBox;
	private final boolean targetGroupHasEmail;
	
	private LdapUserMembership(String memberUid, String member, String mailBox, boolean targetGroupHasEmail) {
		this.memberUid = memberUid;
		this.member = member;
		this.mailBox = mailBox;
		this.targetGroupHasEmail = targetGroupHasEmail;
	}
	
	public String getMemberUid() {
		return memberUid;
	}

	public String getMember() {
		return member;
	}

	public String getMailBox() {
		return mailBox;
	}

	public boolean isTargetGroupHasEmail() {
		return targetGroupHasEmail;
	}

	public Modification[] buildAddModifications() {
		return buildModifications(ModificationOperation.ADD_ATTRIBUTE);
	}

	public Modification[] buildRemoveModifications() {
		return buildModifications(ModificationOperation.REMOVE_ATTRIBUTE);
	}

	private Modification[] buildModifications(ModificationOperation operation) {
		ImmutableList.Builder<Modification> modifications = ImmutableList
				.<Modification>builder()
				.add(new DefaultModification(operation, "memberUid", getMemberUid()))
				.add(new DefaultModification(operation, "member", getMember()));

		if (targetGroupHasEmail) {
			modifications.add(new DefaultModification(operation, "mailBox", getMailBox()));
		}

		ImmutableList<Modification> list = modifications.build();

		return list.toArray(new Modification[list.size()]);
	}

	@Override
	public final int hashCode(){
		return Objects.hashCode(memberUid, member, mailBox, targetGroupHasEmail);
	}
	
	@Override
	public final boolean equals(Object object){
		if (object instanceof LdapUserMembership) {
			LdapUserMembership that = (LdapUserMembership) object;
			return Objects.equal(this.memberUid, that.memberUid)
				&& Objects.equal(this.member, that.member)
				&& Objects.equal(this.mailBox, that.mailBox)
				&& Objects.equal(this.targetGroupHasEmail, that.targetGroupHasEmail);
		}
		return false;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("memberUid", memberUid)
			.add("member", member)
			.add("mailBox", mailBox)
			.add("targetGroupHasEmail", targetGroupHasEmail)
			.toString();
	}
}
