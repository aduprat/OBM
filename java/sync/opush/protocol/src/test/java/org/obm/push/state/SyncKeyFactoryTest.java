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
package org.obm.push.state;

import static org.fest.assertions.api.Assertions.assertThat;

import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.obm.filter.Slow;
import org.obm.filter.SlowFilterRunner;
import org.obm.push.bean.SyncKey;
import org.obm.push.utils.UUIDFactory;

import com.google.common.collect.Lists;

@RunWith(SlowFilterRunner.class)
public class SyncKeyFactoryTest {

	private SyncKeyFactory syncKeyFactory;

	@Before
	public void setUp() {
		UUIDFactory uuidFactory = new UUIDFactory() {};
		syncKeyFactory = new SyncKeyFactory(uuidFactory);
	}
	
	@Test
	public void testNotNull() {
		assertThat(syncKeyFactory.randomSyncKey()).isNotNull();
	}
	
	@Test
	public void testNotEmtpy() {
		SyncKey randomSyncKey = syncKeyFactory.randomSyncKey();
		assertThat(randomSyncKey.getSyncKey()).isNotNull();
		assertThat(randomSyncKey.getSyncKey()).isNotEmpty();
	}
	
	@Ignore("too slow : https://github.com/alexruiz/fest-assert-2.x/issues/122")
	@Test @Slow
	public void testNotSameKeyForMillionsGeneration() {
		int syncKeyGenerationCount = 1000000;

		List<SyncKey> allGeneratedSyncKeys = Lists.newArrayListWithExpectedSize(syncKeyGenerationCount);
		for (int count = 0; count < syncKeyGenerationCount; count++) {
			allGeneratedSyncKeys.add(syncKeyFactory.randomSyncKey());
		}
		
		assertThat(allGeneratedSyncKeys).doesNotHaveDuplicates();
	}
}
