/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2011-2014  Linagora
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
package org.obm.sync.calendar;

import java.util.Date;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

public class SyncRange {
	
	private final Date before;
	private final Date after;

	public SyncRange(Date before, Date after) {
		super();
		this.before = before;
		this.after = after != null ? after : new Date(0);
	}

	public Date getBefore() {
		return before;
	}

	public Date getAfter() {
		return after;
	}

	@Override
	public final boolean equals(Object o) {
		if (!(o instanceof SyncRange)) {
			return false;
		}
		SyncRange otherRange = (SyncRange) o;
		return Objects.equal(before, otherRange.before) && Objects.equal(after, otherRange.after);
	}

	@Override
	public final int hashCode() {
		return Objects.hashCode(before, after);
	}

	@Override
	public final String toString() {
		return MoreObjects.toStringHelper(this.getClass())
				.add("before", before)
				.add("after", after)
				.toString();
	}
}
