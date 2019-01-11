/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2014 Linagora
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
package org.obm.provisioning;

import org.obm.push.utils.jvm.VMArgumentsUtils;
import org.obm.server.EmbeddedServerModule;
import org.obm.server.ServerConfiguration;
import org.obm.server.WebServer;
import org.obm.server.context.NoContext;

import com.google.common.base.MoreObjects;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class ProvisioningServerLauncher {

	private static final int DEFAULT_SERVER_PORT = 8086;
	private static final int SERVER_PORT = MoreObjects.firstNonNull(
			VMArgumentsUtils.integerArgumentValue("provisioningServerPort"),
			DEFAULT_SERVER_PORT);

	public static void main(String... args) throws Exception {
		/******************************************************************
		 * EVERY CHANGE DONE HERE CAN SILENTLY BREAK THE STARTUP *
		 ******************************************************************/
		Injector injector = Guice.createInjector(
				new ProvisioningServerService(new NoContext()), 
				new EmbeddedServerModule(
					ServerConfiguration.builder()
						.port(SERVER_PORT)
						.requestLoggerEnabled(true)
						.build()));
		
		WebServer webserver = injector.getInstance(WebServer.class);
		start(webserver).join();
	}

	public static WebServer start(WebServer server) throws Exception {
		registerSigTermHandler(server);
		server.start();
		return server;
	}

	private static void registerSigTermHandler(final WebServer server) {
		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {
				try {
					server.stop();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});
	}
}
