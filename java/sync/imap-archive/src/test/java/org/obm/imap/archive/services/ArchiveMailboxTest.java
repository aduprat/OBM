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

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.obm.imap.archive.beans.Year;
import org.obm.imap.archive.exception.ImapCreateException;
import org.obm.imap.archive.exception.MailboxFormatException;
import org.obm.push.minig.imap.StoreClient;
import org.obm.sync.base.DomainName;
import org.slf4j.Logger;

import pl.wkr.fluentrule.api.FluentExpectedException;


public class ArchiveMailboxTest {

	@Rule public FluentExpectedException expectedException = FluentExpectedException.none();
	
	private IMocksControl control;
	
	@Before
	public void setup() {
		control = createControl();
	}
	
	@Test(expected=NullPointerException.class)
	public void mailboxShouldNotBeNull() throws Exception {
		ArchiveMailbox.from((Mailbox) null, (Year) null, (DomainName) null);
	}
	
	@Test(expected=NullPointerException.class)
	public void yearShouldNotBeNull() throws Exception {
		Logger logger = control.createMock(Logger.class);
		StoreClient storeClient = control.createMock(StoreClient.class);
		
		control.replay();
		ArchiveMailbox.from(Mailbox.from("mailbox", logger, storeClient), null, null);
		control.verify();
	}
	
	@Test(expected=NullPointerException.class)
	public void domainNameShouldNotBeNull() throws Exception {
		Logger logger = control.createMock(Logger.class);
		StoreClient storeClient = control.createMock(StoreClient.class);
		
		control.replay();
		ArchiveMailbox.from(Mailbox.from("mailbox", logger, storeClient), Year.from(2015), null);
		control.verify();
	}
	
	@Test
	public void shouldBuildWhenEveryThingProvided() throws Exception {
		Logger logger = control.createMock(Logger.class);
		StoreClient storeClient = control.createMock(StoreClient.class);
		
		control.replay();
		ArchiveMailbox.from(Mailbox.from("user/usera@mydomain.org", logger, storeClient), Year.from(2015), new DomainName("mydomain.org"));
		control.verify();
	}
	
	@Test
	public void archiveMailboxShouldWorkWhenMailboxIsINBOX() throws Exception {
		String mailbox = "user/usera@mydomain.org";
		
		String archiveMailbox = ArchiveMailbox.archiveMailbox(mailbox, Year.from(2014));
		assertThat(archiveMailbox).isEqualTo("user/usera/ARCHIVE/2014/INBOX@mydomain.org");
	}
	
	@Test
	public void archiveMailboxShouldWorkWhenMailboxIsAFolder() throws Exception {
		String mailbox = "user/usera/Test@mydomain.org";
		
		String archiveMailbox = ArchiveMailbox.archiveMailbox(mailbox, Year.from(2014));
		assertThat(archiveMailbox).isEqualTo("user/usera/ARCHIVE/2014/Test@mydomain.org");
	}
	
	@Test
	public void archiveMailboxShouldWorkWhenMailboxIsASubFolder() throws Exception {
		String mailbox = "user/usera/Test/subfolder@mydomain.org";
		
		String archiveMailbox = ArchiveMailbox.archiveMailbox(mailbox, Year.from(2014));
		assertThat(archiveMailbox).isEqualTo("user/usera/ARCHIVE/2014/Test/subfolder@mydomain.org");
	}
	
	@Test
	public void archiveMailboxShouldThrowWhenBadMailbox() throws Exception {
		String mailbox = "user";
		
		expectedException.expect(MailboxFormatException.class);
		
		ArchiveMailbox.archiveMailbox(mailbox, Year.from(2014));
	}
	
	@Test
	public void getUserAtDomainShoudWorkWhenMailboxIsINBOX() throws Exception {
		String mailbox = "user/usera@mydomain.org";
		
		String userAtDomain = ArchiveMailbox.getUserAtDomain(mailbox, new DomainName("mydomain.org"));
		assertThat(userAtDomain).isEqualTo("usera@mydomain.org");
	}
	
	@Test
	public void getUserAtDomainShoudWorkWhenMailboxIsAFolder() throws Exception {
		String mailbox = "user/usera/Test@mydomain.org";
		
		String userAtDomain = ArchiveMailbox.getUserAtDomain(mailbox, new DomainName("mydomain.org"));
		assertThat(userAtDomain).isEqualTo("usera@mydomain.org");
	}
	
	@Test
	public void getUserAtDomainShoudWorkWhenMailboxIsASubFolder() throws Exception {
		String mailbox = "user/usera/Test/subfolder@mydomain.org";
		
		String userAtDomain = ArchiveMailbox.getUserAtDomain(mailbox, new DomainName("mydomain.org"));
		assertThat(userAtDomain).isEqualTo("usera@mydomain.org");
	}
	
	@Test
	public void getUserAtDomainShoudThrowWhenBadMailbox() throws Exception {
		String mailbox = "user";
		
		expectedException.expect(MailboxFormatException.class);
		
		String userAtDomain = ArchiveMailbox.getUserAtDomain(mailbox, new DomainName("mydomain.org"));
		assertThat(userAtDomain).isEqualTo("usera@mydomain.org");
	}
	
	@Test
	public void archivePartitionNameShouldWorkWhenTwoLevels() {
		assertThat(ArchiveMailbox.archivePartitionName(new DomainName("mydomain.org"))).isEqualTo("mydomain_org_archive");
	}
	
	@Test
	public void archivePartitionNameShouldWorkWhenThreeLevels() {
		assertThat(ArchiveMailbox.archivePartitionName(new DomainName("mydomain.imap.org"))).isEqualTo("mydomain_imap_org_archive");
	}
	
	@Test
	public void archivePartitionNameShouldWorkWhenOneLevel() {
		assertThat(ArchiveMailbox.archivePartitionName(new DomainName("mydomain"))).isEqualTo("mydomain_archive");
	}
	
	@Test
	public void createShouldNotThrowWhenSuccess() throws Exception {
		Logger logger = control.createMock(Logger.class);
		StoreClient storeClient = control.createMock(StoreClient.class);
		
		expect(storeClient.create("user/usera/ARCHIVE/2015/INBOX@mydomain.org", "mydomain_org_archive"))
			.andReturn(true);
		logger.debug(anyObject(String.class));
		expectLastCall().anyTimes();
		
		control.replay();
		ArchiveMailbox archiveMailbox = ArchiveMailbox.from(Mailbox.from("user/usera@mydomain.org", logger, storeClient), Year.from(2015), new DomainName("mydomain.org"));
		archiveMailbox.create();
		control.verify();
	}
	
	@Test
	public void createShouldThrowWhenError() throws Exception {
		Logger logger = control.createMock(Logger.class);
		StoreClient storeClient = control.createMock(StoreClient.class);
		
		expect(storeClient.create("user/usera/ARCHIVE/2015/INBOX@mydomain.org", "mydomain_org_archive"))
			.andReturn(false);
		logger.error(anyObject(String.class));
		expectLastCall();
		
		expectedException.expect(ImapCreateException.class);
		
		control.replay();
		ArchiveMailbox archiveMailbox = ArchiveMailbox.from(Mailbox.from("user/usera@mydomain.org", logger, storeClient), Year.from(2015), new DomainName("mydomain.org"));
		archiveMailbox.create();
		control.verify();
	}
}