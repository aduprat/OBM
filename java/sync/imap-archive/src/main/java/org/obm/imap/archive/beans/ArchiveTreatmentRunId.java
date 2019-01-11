/* ***** BEGIN LICENSE BLOCK *****
 * Copyright (C) 2014  Linagora
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version, provided you comply with the Additional Terms applicable for OBM
 * software by Linagora pursuant to Section 7 of the GNU Affero General Public
 * License, subsections (b), (c), and (e), pursuant to which you must notably (i)
 * retain the displaying by the interactive user interfaces of the “OBM, Free
 * Communication by Linagora” Logo with the “You are using the Open Source and
 * free version of OBM developed and supported by Linagora. Contribute to OBM R&D
 * by subscribing to an Enterprise offer !” infobox, (ii) retain all hypertext
 * links between OBM and obm.org, between Linagora and linagora.com, as well as
 * between the expression “Enterprise offer” and pro.obm.org, and (iii) refrain
 * from infringing Linagora intellectual property rights over its trademarks and
 * commercial brands. Other Additional Terms apply, see
 * <http://www.linagora.com/licenses/> for more details.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License and
 * its applicable Additional Terms for OBM along with this program. If not, see
 * <http://www.gnu.org/licenses/> for the GNU Affero General   Public License
 * version 3 and <http://www.linagora.com/licenses/> for the Additional Terms
 * applicable to the OBM software.
 * ***** END LICENSE BLOCK ***** */


package org.obm.imap.archive.beans;

import java.util.UUID;

import org.obm.push.utils.UUIDFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class ArchiveTreatmentRunId {

	public static class Factory {
		
		private UUIDFactory uuidFactory;

		@Inject
		@VisibleForTesting Factory(UUIDFactory uuidFactory) {
			this.uuidFactory = uuidFactory;
		}
		
		public ArchiveTreatmentRunId randomRunId() {
			return new Builder().runId(uuidFactory.randomUUID()).build();
		}
	}

	public static ArchiveTreatmentRunId from(UUID runId) {
		return new Builder().runId(runId).build();
	}

	public static ArchiveTreatmentRunId from(String runId) {
		return new Builder().runId(UUID.fromString(runId)).build();
	}
	
	private static class Builder {

		private UUID runId;
		
		private Builder() {
		}
		
		public Builder runId(UUID runId) {
			Preconditions.checkNotNull(runId);
			this.runId = runId;
			return this;
		}
		
		public ArchiveTreatmentRunId build() {
			return new ArchiveTreatmentRunId(runId);
		}
	}
	
	private final UUID runId;
	
	private ArchiveTreatmentRunId(UUID runId) {
		this.runId = runId;
	}

	public UUID getRunId() {
		return runId;
	}
	
	public String serialize() {
		return runId.toString();
	}
	
	@Override
	public int hashCode(){
		return Objects.hashCode(runId);
	}
	
	@Override
	public boolean equals(Object object){
		if (object instanceof ArchiveTreatmentRunId) {
			ArchiveTreatmentRunId that = (ArchiveTreatmentRunId) object;
			return Objects.equal(this.runId, that.runId);
		}
		return false;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("runId", runId)
			.toString();
	}
}
