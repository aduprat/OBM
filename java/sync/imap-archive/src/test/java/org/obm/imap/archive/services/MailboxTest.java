/* ***** BEGIN LICENSE BLOCK *****
 *
 * Copyright (C) 2014  Linagora
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

package org.obm.imap.archive.services;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.obm.imap.archive.exception.ImapSelectException;
import org.obm.imap.archive.exception.ImapSetAclException;
import org.obm.push.minig.imap.StoreClient;
import org.slf4j.Logger;

import pl.wkr.fluentrule.api.FluentExpectedException;


public class MailboxTest {

	private IMocksControl control;
	private Logger logger;
	private StoreClient storeClient;

	@Rule public FluentExpectedException expectedException = FluentExpectedException.none();
	
	@Before
	public void setup() {
		control = createControl();
		logger = control.createMock(Logger.class);
		
		storeClient = control.createMock(StoreClient.class);
	}
	
	@Test(expected=NullPointerException.class)
	public void nameShouldNotBeNull() {
		Mailbox.from(null, null, null);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void nameShouldNotBeEmpty() {
		Mailbox.from("", null, null);
	}
	
	@Test(expected=NullPointerException.class)
	public void loggerShouldBeProvided() {
		Mailbox.from("mailbox", null, null);
	}
	
	@Test(expected=NullPointerException.class)
	public void storeClientShouldBeProvided() {
		Mailbox.from("mailbox", logger, null);
	}
	
	@Test
	public void selectShouldNotThrowWhenSuccess() throws Exception {
		expect(storeClient.select("mailbox"))
			.andReturn(true);
		logger.debug(anyObject(String.class), anyObject());
		expectLastCall().anyTimes();
		
		control.replay();
		Mailbox mailbox = Mailbox.from("mailbox", logger, storeClient);
		mailbox.select();
		control.verify();
	}
	
	@Test
	public void selectShouldThrowWhenError() throws Exception {
		expect(storeClient.select("mailbox"))
			.andReturn(false);
		logger.error(anyObject(String.class));
		expectLastCall();
		
		expectedException.expect(ImapSelectException.class);
		
		control.replay();
		Mailbox mailbox = Mailbox.from("mailbox", logger, storeClient);
		mailbox.select();
		control.verify();
	}
	
	@Test
	public void setAclShouldReturnTrueWhenSuccess() throws Exception {
		String user = "user";
		String acl = "lr";
		expect(storeClient.setAcl("mailbox", user, acl))
			.andReturn(true);
		logger.debug(anyObject(String.class), anyObject(), anyObject(), anyObject());
		expectLastCall().anyTimes();
		
		control.replay();
		Mailbox mailbox = Mailbox.from("mailbox", logger, storeClient);
		mailbox.setAcl(user, acl);
		control.verify();
	}
	
	@Test
	public void setAclShouldReturnFalseWhenError() throws Exception {
		String user = "user";
		String acl = "lr";
		expect(storeClient.setAcl("mailbox", user, acl))
			.andReturn(false);
		logger.error(anyObject(String.class));
		expectLastCall();
		
		expectedException.expect(ImapSetAclException.class);
		
		control.replay();
		Mailbox mailbox = Mailbox.from("mailbox", logger, storeClient);
		mailbox.setAcl(user, acl);
		control.verify();
	}
}
