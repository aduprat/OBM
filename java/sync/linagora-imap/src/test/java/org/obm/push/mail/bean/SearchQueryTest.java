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

package org.obm.push.mail.bean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;

import org.junit.Test;
import org.obm.push.utils.DateUtils;


public class SearchQueryTest {

	@Test
	public void builderShoudBuildWhenNoMessageSet() {
		SearchQuery searchQuery = SearchQuery.builder()
			.build();
		assertThat(searchQuery.getMessageSet()).isNull();
	}

	@Test
	public void builderShoudBuildWhenMessageSet() {
		MessageSet expectedMessageSet = MessageSet.singleton(1l);
		SearchQuery searchQuery = SearchQuery.builder()
			.messageSet(expectedMessageSet)
			.build();
		assertThat(searchQuery.getMessageSet()).isEqualTo(expectedMessageSet);
	}

	@Test(expected=IllegalStateException.class)
	public void builderShoudBuildWhenEmptyMessageSet() {
		MessageSet expectedMessageSet = MessageSet.empty();
		SearchQuery searchQuery = SearchQuery.builder()
			.messageSet(expectedMessageSet)
			.build();
		assertThat(searchQuery.getMessageSet()).isEqualTo(expectedMessageSet);
	}
	
	@Test(expected=IllegalStateException.class)
	public void builderShouldThwoWhenBetweenAndNullBefore() {
		SearchQuery.builder()
			.between(true)
			.build();
	}
	
	@Test(expected=IllegalStateException.class)
	public void builderShouldThwoWhenBetweenAndNullAfter() {
		SearchQuery.builder()
			.between(true)
			.beforeExclusive(DateUtils.date("2014-01-01T00:00:00Z"))
			.build();
	}
	
	@Test
	public void builderShouldBuildWhenBetween() {
		SearchQuery searchQuery = SearchQuery.builder()
			.between(true)
			.beforeExclusive(DateUtils.date("2014-01-01T00:00:00Z"))
			.afterInclusive(DateUtils.date("2015-01-01T00:00:00Z"))
			.build();
		assertThat(searchQuery.isBetween()).isTrue();
	}
	
	@Test
	public void builderShouldBuildWhenNoMatchingFlag() {
		SearchQuery searchQuery = SearchQuery.builder()
				.build();
		assertThat(searchQuery.getMatchingFlag()).isAbsent();
	}
	
	@Test
	public void builderShouldBuildWhenMatchingFlag() {
		SearchQuery searchQuery = SearchQuery.builder()
				.matchingFlag(Flag.from("myFlag"))
				.build();
		assertThat(searchQuery.getMatchingFlag()).isPresent();
	}
	
	@Test
	public void builderShouldBuildWhenNoUnmatchingFlag() {
		SearchQuery searchQuery = SearchQuery.builder()
				.build();
		assertThat(searchQuery.getUnmatchingFlag()).isAbsent();
	}
	
	@Test
	public void builderShouldBuildWhenUnmatchingFlag() {
		SearchQuery searchQuery = SearchQuery.builder()
				.unmatchingFlag(Flag.from("myFlag"))
				.build();
		assertThat(searchQuery.getUnmatchingFlag()).isPresent();
	}
}
