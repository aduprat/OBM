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

package org.obm.imap.archive.beans;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.obm.push.mail.bean.MessageSet;


public class MappedMessageSetsTest {

	@Test(expected=NullPointerException.class)
	public void originShouldNotBeNull() {
		MappedMessageSets.builder().origin(null);
	}

	@Test(expected=NullPointerException.class)
	public void destinationShouldNotBeNull() {
		MappedMessageSets.builder().destination(null);
	}

	@Test(expected=IllegalStateException.class)
	public void originIsMandatory() {
		MappedMessageSets.builder().build();
	}

	@Test(expected=IllegalStateException.class)
	public void destinationIsMandatory() {
		MappedMessageSets.builder().origin(MessageSet.empty()).build();
	}

	@Test(expected=IllegalStateException.class)
	public void originAndDestinationShouldHaveTheSameSize() {
		MappedMessageSets.builder().origin(MessageSet.empty()).destination(MessageSet.singleton(1l)).build();
	}

	@Test
	public void originAndDestinationShouldBeMapped() {
		MessageSet origin = MessageSet.builder().add(1l).add(2l).add(3l).add(4l).build();
		MessageSet destination = MessageSet.builder().add(10l).add(20l).add(30l).add(40l).build();
		
		MappedMessageSets mappedMessageSets = MappedMessageSets.builder().origin(origin).destination(destination).build();
		assertThat(mappedMessageSets.getDestinationUidFor(1l)).isEqualTo(10l);
		assertThat(mappedMessageSets.getDestinationUidFor(2l)).isEqualTo(20l);
		assertThat(mappedMessageSets.getDestinationUidFor(3l)).isEqualTo(30l);
		assertThat(mappedMessageSets.getDestinationUidFor(4l)).isEqualTo(40l);
	}
}
