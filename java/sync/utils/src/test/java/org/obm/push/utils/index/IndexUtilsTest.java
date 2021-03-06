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
package org.obm.push.utils.index;

import static org.fest.assertions.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.fest.assertions.data.MapEntry;
import org.junit.Test;
import org.junit.runner.RunWith;


import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.obm.filter.SlowFilterRunner;

@RunWith(SlowFilterRunner.class)
public class IndexUtilsTest {

	private static class IntIndexed implements Indexed<Integer> {
		
		private final int index;

		public IntIndexed(int index) {
			this.index = index;
		}
		
		@Override
		public Integer getIndex() {
			return index;
		}
		
		@Override
		public boolean equals(Object obj) {
			return Objects.equal(this.index, ((IntIndexed)obj).index);
		}
		
		@Override
		public int hashCode() {
			return Objects.hashCode(index);
		}
	}
	
	@Test
	public void testEmptyList() {
		ImmutableList<Indexed<Integer>> emptyList = ImmutableList.of();
		ArrayList<Integer> listIndexes = IndexUtils.listIndexes(emptyList);
		assertThat(listIndexes).isEmpty();
	}
	
	@Test(expected=NullPointerException.class)
	public void testNullList() {
		@SuppressWarnings("unused")
		ArrayList<Integer> listIndexes = IndexUtils.listIndexes((List<Indexed<Integer>>)null);
	}
	
	@Test(expected=NullPointerException.class)
	public void testNullElementInList() {
		List<IntIndexed> list = Lists.newArrayList();
		list.add(null);
		@SuppressWarnings("unused")
		ArrayList<Integer> listIndexes = IndexUtils.listIndexes(list);
	}

	@Test
	public void testSimpleList() {
		ImmutableList<IntIndexed> emptyList = 
				ImmutableList.of(new IntIndexed(0), new IntIndexed(1), new IntIndexed(2));
		ArrayList<Integer> listIndexes = IndexUtils.listIndexes(emptyList);
		assertThat(listIndexes).containsOnly(0, 1, 2);
	}
	
	@Test
	public void testMapEmpty() {
		List<Indexed<Integer>> emptyList = ImmutableList.of();
		Map<Integer, Indexed<Integer>> mapByIndexes = IndexUtils.mapByIndexes(emptyList);
		assertThat(mapByIndexes).isEmpty();
	}

	@Test(expected=NullPointerException.class)
	public void testMapNull() {
		@SuppressWarnings("unused")
		Map<Integer, IntIndexed> mapByIndexes = IndexUtils.mapByIndexes((List<IntIndexed>)null);
	}

	@Test
	public void testMapWithOneElement() {
		List<IntIndexed> emptyList = ImmutableList.of(new IntIndexed(5));
		Map<Integer, IntIndexed> mapByIndexes = IndexUtils.mapByIndexes(emptyList);
		assertThat(mapByIndexes).hasSize(1).contains(
				MapEntry.entry(5, new IntIndexed(5)));
	}

	@Test
	public void testMapWithOneNegativeElement() {
		List<IntIndexed> emptyList = ImmutableList.of(new IntIndexed(-5));
		Map<Integer, IntIndexed> mapByIndexes = IndexUtils.mapByIndexes(emptyList);
		assertThat(mapByIndexes).hasSize(1).contains(
				MapEntry.entry(-5, new IntIndexed(-5)));
	}
	
	@Test
	public void testMapWithTwoElement() {
		List<IntIndexed> emptyList = ImmutableList.of(new IntIndexed(5), new IntIndexed(-5));
		Map<Integer, IntIndexed> mapByIndexes = IndexUtils.mapByIndexes(emptyList);
		assertThat(mapByIndexes).hasSize(2).contains(
				MapEntry.entry(5, new IntIndexed(5)),
				MapEntry.entry(-5, new IntIndexed(-5)));
	}
	
	@Test
	public void testMapWithManyElement() {
		List<IntIndexed> emptyList = ImmutableList.of(
				new IntIndexed(5), new IntIndexed(-5), new IntIndexed(10), new IntIndexed(20), new IntIndexed(150));
		Map<Integer, IntIndexed> mapByIndexes = IndexUtils.mapByIndexes(emptyList);
		assertThat(mapByIndexes).hasSize(5).contains(
				MapEntry.entry(5, new IntIndexed(5)),
				MapEntry.entry(-5, new IntIndexed(-5)),
				MapEntry.entry(10, new IntIndexed(10)),
				MapEntry.entry(20, new IntIndexed(20)),
				MapEntry.entry(150, new IntIndexed(150)));
	}

	@Test(expected=IllegalArgumentException.class)
	public void testMapWithTwoSameIndexes() {
		List<IntIndexed> emptyList = ImmutableList.of(new IntIndexed(5), new IntIndexed(5));
		Map<Integer, IntIndexed> mapByIndexes = IndexUtils.mapByIndexes(emptyList);
		assertThat(mapByIndexes).hasSize(1).contains(
				MapEntry.entry(5, new IntIndexed(5)));
	}
}
