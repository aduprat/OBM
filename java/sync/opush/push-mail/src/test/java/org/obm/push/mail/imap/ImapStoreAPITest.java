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
package org.obm.push.mail.imap;

import static org.obm.configuration.EmailConfiguration.IMAP_INBOX_NAME;
import static org.obm.push.mail.MailTestsUtils.loadEmail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.Set;

import org.fest.assertions.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.obm.opush.env.JUnitGuiceRule;
import org.obm.opush.mail.StreamMailTestsUtils;
import org.obm.push.bean.BackendSession;
import org.obm.push.bean.CollectionPathHelper;
import org.obm.push.bean.Credentials;
import org.obm.push.bean.Email;
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.User;
import org.obm.push.mail.MailEnvModule;
import org.obm.push.mail.MailException;
import org.obm.push.mail.MailTestsUtils;
import org.obm.push.mail.RandomGeneratedInputStream;
import org.obm.push.mail.ThrowingInputStream;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.icegreen.greenmail.util.GreenMail;

public class ImapStoreAPITest {

	
	@Rule
	public JUnitGuiceRule guiceBerry = new JUnitGuiceRule(MailEnvModule.class);

	@Inject ImapMailboxService mailboxService;

	@Inject CollectionPathHelper collectionPathHelper;
	@Inject ImapMailBoxUtils mailboxUtils;
	@Inject GreenMail greenMail;
	private String mailbox;
	private String password;
	private BackendSession bs;
	private String inboxPath;

	@Before
	public void setUp() {
		greenMail.start();
		mailbox = "to@localhost.com";
		password = "password";
		greenMail.setUser(mailbox, password);
		bs = new BackendSession(
    				new Credentials(User.Factory.create()
						.createUser(mailbox, mailbox, null), password), null, null, null);
		inboxPath = collectionPathHelper.buildCollectionPath(bs, PIMDataType.EMAIL, IMAP_INBOX_NAME);
	}
	
	@After
	public void tearDown() {
		greenMail.stop();
	}
	
	@Test
	public void testStoreInInboxContentAfterStoring() throws Exception {
		InputStream emailData = loadEmail("plainText.eml");
		mailboxService.storeInInbox(bs, emailData, true);
		InputStream fetchedMailStream = mailboxService.fetchMailStream(bs, inboxPath, 1);
		InputStream expectedEmailData = loadEmail("plainText.eml");
		Assertions.assertThat(fetchedMailStream).hasContentEqualTo(expectedEmailData);
	}

	@Test
	public void testStoreInInboxContentAfterStoringStream() throws Exception {
		InputStream emailData = loadEmail("plainText.eml");
		mailboxService.storeInInbox(bs, emailData, 279, true);
		InputStream fetchedMailStream = mailboxService.fetchMailStream(bs, inboxPath, 1);
		InputStream expectedEmailData = loadEmail("plainText.eml");
		Assertions.assertThat(fetchedMailStream).hasContentEqualTo(expectedEmailData);
	}

	@Test
	public void testStoreInInboxInvitation() throws Exception {
		InputStream emailData = loadEmail("androidInvit.eml");
		mailboxService.storeInInbox(bs, emailData, true);

		InputStream expected = loadEmail("androidInvit.eml");
		InputStream fetchedContent = mailboxService.fetchMailStream(bs, inboxPath, 1);
		Assertions.assertThat(fetchedContent).hasContentEqualTo(expected);
	}

	@Test
	public void testStoreInInboxInvitationStream() throws Exception {
		InputStream emailData = loadEmail("androidInvit.eml");
		mailboxService.storeInInbox(bs, emailData, 3660, true);
		InputStream expected = loadEmail("androidInvit.eml");
		InputStream fetchedContent = mailboxService.fetchMailStream(bs, inboxPath, 1);
		Assertions.assertThat(fetchedContent).hasContentEqualTo(expected);
	}

	@Test
	public void testStoreInInboxNotAnEmail() throws Exception {
		byte[] data = new byte[]{'t','e','s','t', '\r', '\n', '\r', '\n'};
		InputStream notAnEmailData = new ByteArrayInputStream(data);
		mailboxService.storeInInbox(bs, notAnEmailData, true);
		InputStream fetchedContent = mailboxService.fetchMailStream(bs, inboxPath, 1);
		Assertions.assertThat(fetchedContent).hasContentEqualTo(new ByteArrayInputStream(data));
	}

	@Test
	public void testStoreInInboxNotAnEmailStream() throws Exception {
		byte[] data = new byte[]{'t','e','s','t', '\r', '\n', '\r', '\n'};
		InputStream notAnEmailData = new ByteArrayInputStream(data);
		mailboxService.storeInInbox(bs, notAnEmailData, data.length, true);
		InputStream fetchedContent = mailboxService.fetchMailStream(bs, inboxPath, 1);
		Assertions.assertThat(fetchedContent).hasContentEqualTo(new ByteArrayInputStream(data));
	}
	
	@Test(expected=MailException.class)
	public void testStoreInInboxThrowExceptionWhenStreamFail() throws Exception {
		InputStream failingEmailStream = new ThrowingInputStream(new RandomGeneratedInputStream(1000), 50);
		mailboxService.storeInInbox(bs, failingEmailStream, true);
	}
	
	@Test(expected=MailException.class)
	public void testStoreInInboxThrowExceptionWhenStreamFailStream() throws Exception {
		InputStream failingEmailStream = new ThrowingInputStream(new RandomGeneratedInputStream(1000), 50);
		mailboxService.storeInInbox(bs, failingEmailStream, 100, true);
	}

	@Test(expected=MailException.class)
	public void testStoreInInboxRollbackWhenStreamFail() throws Exception {
		Date before = new Date(0);
		InputStream failingEmailStream = new ThrowingInputStream(new RandomGeneratedInputStream(1000), 50);
		try {
			mailboxService.storeInInbox(bs, failingEmailStream, true);
		} catch (MailException e) {
			Set<Email> emails = mailboxService.fetchEmails(bs, inboxPath, before);
			Assertions.assertThat(emails).isNotNull().hasSize(0);
			throw e;
		}
	}
	

	@Test(expected=MailException.class)
	public void testStoreInInboxRollbackWhenStreamFailStream() throws Exception {
		Date before = new Date(0);
		InputStream failingEmailStream = new ThrowingInputStream(new RandomGeneratedInputStream(1000), 50);
		try {
			mailboxService.storeInInbox(bs, failingEmailStream, 100, true);
		} catch (MailException e) {
			Set<Email> emails = mailboxService.fetchEmails(bs, inboxPath, before);
			Assertions.assertThat(emails).isNotNull().hasSize(0);
			throw e;
		}
	}
	
	@Test
	public void testStoreInInboxReadStatusTrue() throws Exception {
		Date before = new Date(0);
		InputStream emailData = loadEmail("plainText.eml");
		mailboxService.storeInInbox(bs, emailData, true);
		Set<Email> emails = mailboxService.fetchEmails(bs, inboxPath, before);
		Email element = Iterables.getOnlyElement(emails);
		Assertions.assertThat(element.isRead()).isTrue();
	}

	@Test
	public void testStoreInInboxReadStatusTrueStream() throws Exception {
		Date before = new Date(0);
		InputStream emailData = loadEmail("plainText.eml");
		mailboxService.storeInInbox(bs, emailData, 279, true);
		Set<Email> emails = mailboxService.fetchEmails(bs, inboxPath, before);
		Email element = Iterables.getOnlyElement(emails);
		Assertions.assertThat(element.isRead()).isTrue();
	}
	
	@Test
	public void testStoreInInboxReadStatusFalse() throws Exception {
		Date before = new Date(0);
		InputStream emailData = loadEmail("plainText.eml");
		mailboxService.storeInInbox(bs, emailData, false);
		Set<Email> emails = mailboxService.fetchEmails(bs, inboxPath, before);
		Email element = Iterables.getOnlyElement(emails);
		Assertions.assertThat(element.isRead()).isFalse();
	}

	@Test
	public void testStoreInInboxReadStatusFalseStream() throws Exception {
		Date before = new Date(0);
		InputStream emailData = loadEmail("plainText.eml");
		mailboxService.storeInInbox(bs, emailData, 279, false);
		Set<Email> emails = mailboxService.fetchEmails(bs, inboxPath, before);
		Email element = Iterables.getOnlyElement(emails);
		Assertions.assertThat(element.isRead()).isFalse();
	}

	
	@Test(expected=MailException.class)
	public void testStoreInInboxThrowExceptionWhenGivenMessageSizeIsShorter() throws Exception {
		int emailGivenSize = 20;
		InputStream emailStream = StreamMailTestsUtils.newInputStreamFromString("This sentence contains 36 characters");
		mailboxService.storeInInbox(bs, emailStream, emailGivenSize, true);
	}

	@Test(expected=MailException.class)
	public void testStoreInInboxRollbackWhenGivenMessageSizeIsShorter() throws Exception {
		Date before = new Date(0);
		int emailGivenSize = 20;
		InputStream emailStream = StreamMailTestsUtils.newInputStreamFromString("This sentence contains 36 characters");

		try {
			mailboxService.storeInInbox(bs, emailStream, emailGivenSize, true);
		} catch (MailException e) {
			Set<Email> emails = mailboxService.fetchEmails(bs, inboxPath, before);
			Assertions.assertThat(emails).isNotNull().hasSize(0);
			throw e;
		}
	}

	@Test(expected=MailException.class)
	public void testStoreInInboxThrowExceptionWhenGivenMessageSizeIsLonger() throws MailException {
		int emailGivenSize = 60;
		InputStream emailStream = StreamMailTestsUtils.newInputStreamFromString("This sentence contains 36 characters");
		try {
			mailboxService.storeInInbox(bs, emailStream, emailGivenSize, true);
		} catch (MailException e) {
			MailTestsUtils.assertThatIsJavaSocketTimeoutException(e);
			throw e;
		}
	}

	@Test(expected=MailException.class)
	public void testStoreInInboxRollbackWhenGivenMessageSizeIsLonger() throws Exception {
		Date before = new Date(0);
		int emailGivenSize = 60;
		InputStream emailStream = StreamMailTestsUtils.newInputStreamFromString("This sentence contains 36 characters");

		try {
			mailboxService.storeInInbox(bs, emailStream, emailGivenSize, true);
		} catch (MailException e) {
			Set<Email> emails = mailboxService.fetchEmails(bs, inboxPath, before);
			Assertions.assertThat(emails).isNotNull().hasSize(0);
			throw e;
		}
	}

	
}
