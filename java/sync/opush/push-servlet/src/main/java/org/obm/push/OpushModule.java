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
package org.obm.push;
import org.obm.configuration.VMArgumentsUtils;
import org.obm.configuration.module.LoggerModule;
import org.obm.healthcheck.HealthCheckDefaultHandlersModule;
import org.obm.healthcheck.HealthCheckModule;
import org.obm.push.java.mail.ImapModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class OpushModule extends AbstractModule {

	private final static String JAVA_MAIL_MODULE = "javaMail";
	private static final Logger logger = LoggerFactory.getLogger(LoggerModule.CONFIGURATION);
	
	@Override
	protected void configure() {
		installImapModule();
		install(new OpushImplModule());
		install(new OpushMailModule());
		install(new LoggerModule());
		install(new OpushCrashModule());
		install(new HealthCheckModule());
		install(new HealthCheckDefaultHandlersModule());
		bind(Boolean.class).annotatedWith(Names.named("enable-push")).toInstance(false);
 	}

	private void installImapModule() {
		String imapModuleName = VMArgumentsUtils.stringArgumentValue(OptionalVMArguments.BACKEND_EMAIL_NAME);
		install(imapModule(imapModuleName));
	}
	
	private AbstractModule imapModule(String imapModuleName) {
		if (Strings.isNullOrEmpty(imapModuleName)) {
			return defaultImapModule();
		}
		
		if (JAVA_MAIL_MODULE.equals(imapModuleName)) {
			logger.debug("Using java mail imap module");
			return new ImapModule();
		}
		
		return defaultImapModule();
	}
	
	private AbstractModule defaultImapModule() {
		logger.debug("Using default imap module");
		return new LinagoraImapModule();
	}
}
