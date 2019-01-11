/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2011-2016 Linagora
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
package com.linagora.obm.sync;

import org.hornetq.core.config.Configuration;
import org.hornetq.jms.server.config.JMSConfiguration;
import org.hornetq.jms.server.embedded.EmbeddedJMS;
import org.obm.sync.LifecycleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class JMSServer implements LifecycleListener {

	private static final Logger logger = LoggerFactory.getLogger(JMSServer.class);
	
	private final EmbeddedJMS jmsServer;
	private boolean started;

	@Inject
	public JMSServer(Configuration configuration, JMSConfiguration jmsConfiguration) {
		super();
		jmsServer = new EmbeddedJMS();
		jmsServer.setConfiguration(configuration);
		jmsServer.setJmsConfiguration(jmsConfiguration);
		started = false;

		try {
			start();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized void start() throws Exception {
		if (started) {
			throw new IllegalStateException(this.getClass().getName() + " can't be started twice");
		}
		jmsServer.start();
		logger.info("Embedded JMS Server Started");
		started = true;
	}

	public synchronized void stop() throws Exception {
		if (started) {
			jmsServer.stop();
			logger.info("Embedded JMS Server Stopped");
			started = false;
		}
	}

	@Override
	public void shutdown() throws Exception {
		stop();
	}

}
