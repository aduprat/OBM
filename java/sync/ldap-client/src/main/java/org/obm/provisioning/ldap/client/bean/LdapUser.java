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

import java.util.List;
import java.util.Set;

import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.obm.provisioning.ldap.client.Configuration;
import org.obm.provisioning.ldap.client.samba.NTLMPasswordGenerator;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import fr.aliacom.obm.common.domain.Samba;
import fr.aliacom.obm.common.user.ObmUser;
import fr.aliacom.obm.common.user.UserPassword;

public class LdapUser {

	private final static String[] DEFAULT_OBJECT_CLASSES = {
		"posixAccount", "shadowAccount", "inetOrgPerson", "obmUser" };
	private final static String SAMBA_SAM_ACCOUNT_OBJECT_CLASS = "sambaSamAccount";

	private final static String DEFAULT_LOGIN_SHELL = "/bin/bash";
	private final static String DEFAULT_WEB_ACCESS = "REJECT";
	private final static String FORBIDDEN_EMAIL_ACCESS = "REJECT";
	private final static String PERMITTED_EMAIL_ACCESS = "PERMIT";
	private final static int DEFAULT_CYRUS_PORT = 24;
	private final static boolean DEFAULT_HIDDEN_USER = false;
	
	public static class Uid {

		private final String uid;
	
		public static Uid valueOf(String id) {
			return new Uid(id);
		}
		
		private Uid(String uid) {
			this.uid = uid;
		}
		
		public String get() {
			return uid;
		}

		@Override
		public final boolean equals(Object object){
			if (!(object instanceof Uid))
				return false;
			
			return Objects.equal(uid, ((Uid)object).uid);
		}

		@Override
		public final int hashCode(){
			return Objects.hashCode(uid);
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this)
					.add("uid", uid)
					.toString();
		}
	}
	
	public static class Builder {
		
		private String[] objectClasses;
		private Uid uid;
		private Integer uidNumber;
		private Integer gidNumber;
		private String loginShell;
		private String cn;
		private String displayName;
		private String sn;
		private String givenName;
		private String homeDirectory;
		private UserPassword userPassword;
		private String webAccess;
		private String mailBox;
		private String mailBoxServer;
		private String mailAccess;
		private String mail;
		private final ImmutableSet.Builder<String> mailAlias;
		private boolean hiddenUser;
		private LdapDomain domain;
		private boolean sambaAllowed;
		private String sambaSid;
		private String sambaPrimaryGroupSid;
		private String sambaNTPassword;
		private String sambaLMPassword;
		private String sambaHomeDrive;
		private String sambaHomeFolder;
		private String sambaLogonScript;
		
		private final Configuration configuration;

		@Inject
		private Builder(Configuration configuration) {
			this.configuration = configuration;
			this.mailAlias = ImmutableSet.builder();
		}
	
		public Builder fromObmUser(ObmUser obmUser) {
			Preconditions.checkArgument(obmUser.getUidNumber() != null,
					"The UID number is mandatory");
			Preconditions.checkArgument(obmUser.getGidNumber() != null,
					"The GID number is mandatory");
			Preconditions.checkArgument(obmUser.getDomain().getName() != null,
					"The domain name is mandatory");
			if (obmUser.getMailHost() != null) {
				Preconditions.checkArgument(
						obmUser.getMailHost().getIp() != null,
						"The IP of the mail host is mandatory");
			}

			String displayName = buildDisplayName(obmUser);
			this.objectClasses = DEFAULT_OBJECT_CLASSES;
			this.uid = new Uid(obmUser.getLogin().toLowerCase());
			this.uidNumber = obmUser.getUidNumber();
			this.gidNumber = obmUser.getGidNumber();
			this.cn = displayName;
			this.displayName = displayName;
			this.sn = Strings.isNullOrEmpty(obmUser.getLastName()) ?
					obmUser.getLogin() :
					obmUser.getLastName();
			this.givenName = obmUser.getFirstName();
			this.homeDirectory = buildHomeDirectory(obmUser);
			this.userPassword = obmUser.getPassword();
			this.webAccess = DEFAULT_WEB_ACCESS;
			this.mailBox = String.format("%s@%s",
					obmUser.getLogin().toLowerCase(),
					obmUser.getDomain().getName());
			this.mailBoxServer = buildMailboxServer(obmUser);
			this.mailAccess = buildEmailAccess(obmUser);
			this.hiddenUser = DEFAULT_HIDDEN_USER;
			this.domain = LdapDomain.valueOf(obmUser.getDomain().getName());
			this.mail = obmUser.getEmailAtDomain();
			addAllMailAliases(obmUser);
			this.loginShell = DEFAULT_LOGIN_SHELL;
			
			Optional<Samba> optional = obmUser.getDomain().getSamba();
			if (optional.isPresent()) {
				Samba samba = optional.get();
				this.sambaSid = Joiner.on("-").join(samba.getSid(), uidNumber);
				this.sambaPrimaryGroupSid = Joiner.on("-").join(samba.getSid(), gidNumber);
				
				NTLMPassword ntlmPassword = ntlmPassword(obmUser);
				this.sambaNTPassword = ntlmPassword.getNtHash();
				this.sambaLMPassword = ntlmPassword.getLmHash();
				this.sambaAllowed = obmUser.isSambaAllowed();
				this.sambaHomeDrive = obmUser.getSambaHomeDrive();
				this.sambaHomeFolder = obmUser.getSambaHomeFolder();
				this.sambaLogonScript = obmUser.getSambaLogonScript();
			}
			
			return this;
		}
		
		private NTLMPassword ntlmPassword(ObmUser obmUser) {
			try {
				return NTLMPasswordGenerator.computeNTLMPassword(obmUser.getPassword().getStringValue());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		private void addAllMailAliases(ObmUser obmUser) {
			if (mail != null) {
				mailAlias.addAll(Iterables.filter(obmUser.expandAllEmailDomainTuples(), new Predicate<String>() {
					@Override
					public boolean apply(String input) {
						return !input.equals(mail);
					}
				}));
			}
		}
	
		private String buildDisplayName(ObmUser obmUser) {
			String cn;
			if (!Strings.isNullOrEmpty(obmUser.getFirstName())
					&& !Strings.isNullOrEmpty(obmUser.getLastName())) {
				cn = String.format("%s %s", obmUser.getFirstName(), obmUser.getLastName());
			}
			else if (!Strings.isNullOrEmpty(obmUser.getLastName())) {
				cn = obmUser.getLastName();
			}
			else {
				cn = obmUser.getLogin();
			}
			return cn;
		}
	
		private String buildHomeDirectory(ObmUser obmUser) {
			return String.format("/home/%s", obmUser.getLogin().toLowerCase());
		}

		private String buildMailboxServer(ObmUser obmUser) {
			return obmUser.getEmail() != null && obmUser.getMailHost() != null ?
					String.format("lmtp:%s:%d",
							obmUser.getMailHost().getIp(),
							DEFAULT_CYRUS_PORT)
					: null;
		}

		private String buildEmailAccess(ObmUser obmUser) {
			return obmUser.getEmail() != null ?
					PERMITTED_EMAIL_ACCESS :
					FORBIDDEN_EMAIL_ACCESS;
		}
	
		public Builder objectClasses(String[] objectClasses) {
			this.objectClasses = objectClasses;
			return this;
		}
		
		public Builder uid(Uid uid) {
			this.uid = uid;
			return this;
		}
		
		public Builder uidNumber(int uidNumber) {
			this.uidNumber = uidNumber;
			return this;
		}
		
		public Builder gidNumber(int gidNumber) {
			this.gidNumber = gidNumber;
			return this;
		}
		
		public Builder loginShell(String loginShell) {
			this.loginShell = loginShell;
			return this;
		}
		
		public Builder cn(String cn) {
			this.cn = cn;
			return this;
		}
		
		public Builder displayName(String displayName) {
			this.displayName = displayName;
			return this;
		}
		
		public Builder sn(String sn) {
			this.sn = sn;
			return this;
		}
		
		public Builder givenName(String givenName) {
			this.givenName = givenName;
			return this;
		}
		
		public Builder homeDirectory(String homeDirectory) {
			this.homeDirectory = homeDirectory;
			return this;
		}
		
		public Builder userPassword(UserPassword userPassword) {
			this.userPassword = userPassword;
			return this;
		}
		
		public Builder webAccess(String webAccess) {
			this.webAccess = webAccess;
			return this;
		}
		
		public Builder mailBox(String mailBox) {
			this.mailBox = mailBox;
			return this;
		}
		
		public Builder mailBoxServer(String mailBoxServer) {
			this.mailBoxServer = mailBoxServer;
			return this;
		}
		
		public Builder mailAccess(String mailAccess) {
			this.mailAccess = mailAccess;
			return this;
		}
		
		public Builder mail(String mail) {
			this.mail = mail;
			return this;
		}
		
		public Builder mailAlias(Set<String> mailAlias) {
			this.mailAlias.addAll(mailAlias);
			return this;
		}
		
		public Builder hiddenUser(boolean hiddenUser) {
			this.hiddenUser = hiddenUser;
			return this;
		}
		
		public Builder domain(LdapDomain domain) {
			this.domain = domain;
			return this;
		}
		
		public Builder sambaAllowed(boolean sambaAllowed) {
			this.sambaAllowed = sambaAllowed;
			return this;
		}
		
		public Builder sambaHomeDrive(String sambaHomeDrive) {
			this.sambaHomeDrive = sambaHomeDrive;
			return this;
		}
		
		public Builder sambaHomeFolder(String sambaHomeFolder) {
			this.sambaHomeFolder = sambaHomeFolder;
			return this;
		}
		
		public Builder sambaLogonScript(String sambaLogonScript) {
			this.sambaLogonScript = sambaLogonScript;
			return this;
		}
		
		public Builder sambaNTPassword(String sambaNTPassword) {
			this.sambaNTPassword = sambaNTPassword;
			return this;
		}
		
		public Builder sambaLMPassword(String sambaLMPassword) {
			this.sambaLMPassword = sambaLMPassword;
			return this;
		}
		
		public LdapUser build() {
			Preconditions.checkState(uid != null, "uid should not be null");
			Preconditions.checkState(objectClasses != null && objectClasses.length > 0, "objectClasses should not be empty");
			Preconditions.checkState(uidNumber != null, "uidNumber should not be null");
			Preconditions.checkState(gidNumber != null, "gidNumber should not be null");
			Preconditions.checkState(cn != null, "cn should not be null");
			Preconditions.checkState(domain != null, "domain should not be null");
	
			return new LdapUser(configuration.getUserBaseDn(domain), objectClasses, uid, uidNumber, gidNumber, loginShell,
					cn, displayName, sn, givenName, homeDirectory, userPassword, webAccess,
					mailBox, mailBoxServer, mailAccess, mail, mailAlias.build(), hiddenUser, domain,
					sambaAllowed, sambaSid, sambaPrimaryGroupSid, sambaNTPassword, sambaLMPassword, sambaHomeDrive, sambaHomeFolder, sambaLogonScript);
		}
	}
	
	private final Dn userBaseDn;
	private final String[] objectClasses;
	private final Uid uid;
	private final int uidNumber;
	private final int gidNumber;
	private final String loginShell;
	private final String cn;
	private final String displayName;
	private final String sn;
	private final String givenName;
	private final String homeDirectory;
	private final UserPassword userPassword;
	private final String webAccess;
	private final String mailBox;
	private final String mailBoxServer;
	private final String mailAccess;
	private final String mail;
	private final Set<String> mailAlias;
	private final boolean hiddenUser;
	private final LdapDomain domain;
	private final boolean sambaAllowed;
	private final String sambaSid;
	private final String sambaPrimaryGroupSid;
	private final String sambaNTPassword;
	private final String sambaLMPassword;
	private final String sambaHomeDrive;
	private final String sambaHomeFolder;
	private final String sambaLogonScript;
	
	private LdapUser(Dn userBaseDn, String[] objectClasses, Uid uid, int uidNumber, int gidNumber, String loginShell,
			String cn, String displayName, String sn, String givenName, String homeDirectory, UserPassword userPassword, String webAccess,
			String mailBox, String mailBoxServer, String mailAccess, String mail, Set<String>mailAlias, boolean hiddenUser, LdapDomain domain,
			boolean sambaAllowed, String sambaSid, String sambaPrimaryGroupSid, String sambaNTPassword, String sambaLMPassword, String sambaHomeDrive, String sambaHomeFolder, String sambaLogonScript) {
		this.userBaseDn = userBaseDn;
		this.objectClasses = objectClasses;
		this.uid = uid;
		this.uidNumber = uidNumber;
		this.gidNumber = gidNumber;
		this.loginShell = loginShell;
		this.cn = cn;
		this.displayName = displayName;
		this.sn = sn;
		this.givenName = givenName;
		this.homeDirectory = homeDirectory;
		this.userPassword = userPassword;
		this.webAccess = webAccess;
		this.mailBox = mailBox;
		this.mailBoxServer = mailBoxServer;
		this.mailAccess = mailAccess;
		this.mail = mail;
		this.mailAlias = mailAlias;
		this.hiddenUser = hiddenUser;
		this.domain = domain;
		this.sambaAllowed = sambaAllowed;
		this.sambaSid = sambaSid;
		this.sambaPrimaryGroupSid = sambaPrimaryGroupSid;
		this.sambaNTPassword = sambaNTPassword;
		this.sambaLMPassword = sambaLMPassword;
		this.sambaHomeDrive = sambaHomeDrive;
		this.sambaHomeFolder = sambaHomeFolder;
		this.sambaLogonScript = sambaLogonScript;
	}

	public String[] getObjectClasses() {
		return objectClasses;
	}

	public Uid getUid() {
		return uid;
	}

	public int getUidNumber() {
		return uidNumber;
	}

	public int getGidNumber() {
		return gidNumber;
	}

	public String getLoginShell() {
		return loginShell;
	}

	public String getCn() {
		return cn;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getSn() {
		return sn;
	}

	public String getGivenName() {
		return givenName;
	}

	public String getHomeDirectory() {
		return homeDirectory;
	}

	public UserPassword getUserPassword() {
		return userPassword;
	}

	public String getWebAccess() {
		return webAccess;
	}

	public String getMailBox() {
		return mailBox;
	}

	public String getMailBoxServer() {
		return mailBoxServer;
	}

	public String getMailAccess() {
		return mailAccess;
	}

	public String getMail() {
		return mail;
	}
	
	public Set<String> getMailAlias() {
		return mailAlias;
	}

	public boolean isHiddenUser() {
		return hiddenUser;
	}

	public LdapDomain getDomain() {
		return domain;
	}
	
	public boolean isSambaAllowed() {
		return sambaAllowed;
	}
	
	public String getSambaSid() {
		return sambaSid;
	}
	
	public String getSambaPrimaryGroupSid() {
		return sambaPrimaryGroupSid;
	}
	
	public String getSambaNTPassword() {
		return sambaNTPassword;
	}
	
	public String getSambaLMPassword() {
		return sambaLMPassword;
	}
	
	public String getSambaHomeDrive() {
		return sambaHomeDrive;
	}
	
	public String getSambaHomeFolder() {
		return sambaHomeFolder;
	}
	
	public String getSambaLogonScript() {
		return sambaLogonScript;
	}

	public Entry buildEntry() throws LdapException {
		LdapEntry.Builder builder = LdapEntry.builder()
				.dn(buildDn());
		for (String objectClass: getObjectClasses()) {
			builder.attribute(Attribute.valueOf("objectClass", objectClass));
		}
		for (String mail: mailAlias) {
			builder.attribute(Attribute.valueOf("MAILALIAS", mail));
		}
		
		builder
			.attribute(Attribute.valueOf("uid", uid.get()))
			.attribute(Attribute.valueOf("uidNumber", uidNumber))
			.attribute(Attribute.valueOf("gidNumber", gidNumber))
			.attribute(Attribute.valueOf("loginShell", loginShell))
			.attribute(Attribute.valueOf("cn", cn))
			.attribute(Attribute.valueOf("displayName", displayName))
			.attribute(Attribute.valueOf("sn", sn))
			.attribute(Attribute.valueOf("givenName", givenName))
			.attribute(Attribute.valueOf("homeDirectory", homeDirectory))
			.attribute(Attribute.valueOf("userPassword", userPassword.getStringValue()))
			.attribute(Attribute.valueOf("webAccess", webAccess))
			.attribute(Attribute.valueOf("mailBox", mailBox))
			.attribute(Attribute.valueOf("mailBoxServer", mailBoxServer))
			.attribute(Attribute.valueOf("mailAccess", mailAccess))
			.attribute(Attribute.valueOf("mail", mail))
			.attribute(Attribute.valueOf("hiddenUser", Boolean.valueOf(hiddenUser).toString().toUpperCase()))
			.attribute(Attribute.valueOf("obmDomain", domain.get()));
		
		
		if (isSambaAllowed()) {
			builder.attribute(Attribute.valueOf("objectClass", SAMBA_SAM_ACCOUNT_OBJECT_CLASS));
			builder.attribute(Attribute.valueOf("sambaSID", sambaSid));
			builder.attribute(Attribute.valueOf("sambaPrimaryGroupSID", sambaPrimaryGroupSid));
			builder.attribute(Attribute.valueOf("sambaNTPassword", sambaNTPassword));
			builder.attribute(Attribute.valueOf("sambaLMPassword", sambaLMPassword));
			builder.attribute(Attribute.valueOf("sambaHomeDrive", sambaHomeDrive));
			builder.attribute(Attribute.valueOf("sambaHomePath", sambaHomeFolder));
			builder.attribute(Attribute.valueOf("sambaLogonScript", sambaLogonScript));
		}
		
		return builder.build().toDefaultEntry();
	}

	public Modification[] buildDiffModifications(LdapUser oldUser) {
		List<Modification> mods = Lists.newArrayList();

		if (!Objects.equal(uidNumber, oldUser.uidNumber)) {
			mods.add(buildAttributeModification("uidNumber", String.valueOf(uidNumber)));
		}
		if (!Objects.equal(gidNumber, oldUser.gidNumber)) {
			mods.add(buildAttributeModification("gidNumber", String.valueOf(gidNumber)));
		}
		if (!Objects.equal(loginShell, oldUser.loginShell)) {
			mods.add(buildAttributeModification("loginShell", loginShell));
		}
		if (!Objects.equal(cn, oldUser.cn)) {
			mods.add(buildAttributeModification("cn", cn));
		}
		if (!Objects.equal(givenName, oldUser.givenName)) {
			mods.add(buildAttributeModification("givenName", givenName));
		}
		if (!Objects.equal(sn, oldUser.sn)) {
			mods.add(buildAttributeModification("sn", sn));
		}
		if (!Objects.equal(displayName, oldUser.displayName)) {
			mods.add(buildAttributeModification("displayName", displayName));
		}
		if (!Objects.equal(homeDirectory, oldUser.homeDirectory)) {
			mods.add(buildAttributeModification("homeDirectory", homeDirectory));
		}
		if (!Objects.equal(userPassword, oldUser.userPassword)) {
			mods.add(buildAttributeModification("userPassword", userPassword.getStringValue()));
		}
		if (!Objects.equal(webAccess, oldUser.webAccess)) {
			mods.add(buildAttributeModification("webAccess", webAccess));
		}
		if (!Objects.equal(mailBox, oldUser.mailBox)) {
			mods.add(buildAttributeModification("mailBox", mailBox));
		}
		if (!Objects.equal(mailBoxServer, oldUser.mailBoxServer)) {
			mods.add(buildAttributeModification("mailBoxServer", mailBoxServer));
		}
		if (!Objects.equal(mailAccess, oldUser.mailAccess)) {
			mods.add(buildAttributeModification("mailAccess", mailAccess));
		}
		if (!Objects.equal(mail, oldUser.mail)) {
			mods.add(buildAttributeModification("mail", mail));
		}
		if (!Objects.equal(mailAlias, oldUser.mailAlias)) {
			for (String mail: oldUser.mailAlias) {
				mods.add(removeAttributeModification("MAILALIAS", mail));
			}
			for (String mail: mailAlias) {
				mods.add(addAttributeModification("MAILALIAS", mail));
			}
		}
		if (!Objects.equal(hiddenUser, oldUser.hiddenUser)) {
			mods.add(buildAttributeModification("hiddenUser", String.valueOf(hiddenUser).toUpperCase()));
		}
		if (!Objects.equal(sambaAllowed, oldUser.sambaAllowed)) {
			if (!sambaAllowed) {
				mods.add(removeAttributeModification("sambaSID", sambaSid));
				mods.add(removeAttributeModification("sambaPrimaryGroupSID", sambaPrimaryGroupSid));
				mods.add(removeAttributeModification("sambaNTPassword", sambaNTPassword));
				mods.add(removeAttributeModification("sambaLMPassword", sambaLMPassword));
				mods.add(removeAttributeModification("sambaHomeDrive", sambaHomeDrive));
				mods.add(removeAttributeModification("sambaHomePath", sambaHomeFolder));
				mods.add(removeAttributeModification("sambaLogonScript", sambaLogonScript));
			} else {
				mods.add(buildAttributeModification("sambaSID", sambaSid));
				mods.add(buildAttributeModification("sambaPrimaryGroupSID", sambaPrimaryGroupSid));
				mods.add(buildAttributeModification("sambaNTPassword", sambaNTPassword));
				mods.add(buildAttributeModification("sambaLMPassword", sambaLMPassword));
				mods.add(buildAttributeModification("sambaHomeDrive", sambaHomeDrive));
				mods.add(buildAttributeModification("sambaHomePath", sambaHomeFolder));
				mods.add(buildAttributeModification("sambaLogonScript", sambaLogonScript));
			}
		}

		return mods.toArray(new Modification[mods.size()]);
	}

	private Modification buildAttributeModification(String field, String value) {
		if (Strings.isNullOrEmpty(value)) {
			return new DefaultModification(ModificationOperation.REMOVE_ATTRIBUTE, field);
		}

		return new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, field, value);
	}
	
	private Modification removeAttributeModification(String field, String value) {
		return new DefaultModification(ModificationOperation.REMOVE_ATTRIBUTE, field, value);
	}
	
	private Modification addAttributeModification(String field, String value) {
		return new DefaultModification(ModificationOperation.ADD_ATTRIBUTE, field, value);
	}

	private org.obm.provisioning.ldap.client.bean.Dn buildDn() {
		return org.obm.provisioning.ldap.client.bean.Dn.valueOf(
				"uid=" + getUid().get() + "," + userBaseDn.getName());
	}
	
	@Override
	public final int hashCode(){
		return Objects.hashCode(uid, uidNumber, gidNumber, loginShell, cn, displayName, sn, givenName, 
				homeDirectory, userPassword, webAccess, mailBox, mailBoxServer, mailAccess, mail, mailAlias, hiddenUser, domain,
				sambaAllowed, sambaNTPassword, sambaLMPassword, sambaHomeDrive, sambaHomeFolder, sambaLogonScript);
	}
	
	@Override
	public final boolean equals(Object object){
		if (object instanceof LdapUser) {
			LdapUser that = (LdapUser) object;
			return Objects.equal(this.uid, that.uid)
				&& Objects.equal(this.uidNumber, that.uidNumber)
				&& Objects.equal(this.gidNumber, that.gidNumber)
				&& Objects.equal(this.loginShell, that.loginShell)
				&& Objects.equal(this.cn, that.cn)
				&& Objects.equal(this.displayName, that.displayName)
				&& Objects.equal(this.sn, that.sn)
				&& Objects.equal(this.givenName, that.givenName)
				&& Objects.equal(this.homeDirectory, that.homeDirectory)
				&& Objects.equal(this.userPassword, that.userPassword)
				&& Objects.equal(this.webAccess, that.webAccess)
				&& Objects.equal(this.mailBox, that.mailBox)
				&& Objects.equal(this.mailBoxServer, that.mailBoxServer)
				&& Objects.equal(this.mailAccess, that.mailAccess)
				&& Objects.equal(this.mail, that.mail)
				&& Objects.equal(this.mailAlias, that.mailAlias)
				&& Objects.equal(this.hiddenUser, that.hiddenUser)
				&& Objects.equal(this.domain, that.domain)
				&& Objects.equal(this.sambaAllowed, that.sambaAllowed)
				&& Objects.equal(this.sambaNTPassword, that.sambaNTPassword)
				&& Objects.equal(this.sambaLMPassword, that.sambaLMPassword)
				&& Objects.equal(this.sambaHomeDrive, that.sambaHomeDrive)
				&& Objects.equal(this.sambaHomeFolder, that.sambaHomeFolder)
				&& Objects.equal(this.sambaLogonScript, that.sambaLogonScript);
		}
		return false;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("uid", uid)
			.add("uidNumber", uidNumber)
			.add("gidNumber", gidNumber)
			.add("loginShell", loginShell)
			.add("cn", cn)
			.add("displayName", displayName)
			.add("sn", sn)
			.add("givenName", givenName)
			.add("homeDirectory", homeDirectory)
			.add("userPassword", userPassword)
			.add("webAccess", webAccess)
			.add("mailBox", mailBox)
			.add("mailBoxServer", mailBoxServer)
			.add("mailAccess", mailAccess)
			.add("mail", mail)
			.add("mailAlias", mailAlias)
			.add("hiddenUser", hiddenUser)
			.add("obmDomain", domain)
			.add("sambaAllowed", sambaAllowed)
			.add("sambaNTPassword", sambaNTPassword)
			.add("sambaLMPassword", sambaLMPassword)
			.add("sambaHomeDrive", sambaHomeDrive)
			.add("sambaHomeFolder", sambaHomeFolder)
			.add("sambaLogonScript", sambaLogonScript)
			.toString();
	}
}
