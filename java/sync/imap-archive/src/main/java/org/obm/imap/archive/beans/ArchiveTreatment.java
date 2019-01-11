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

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import fr.aliacom.obm.common.domain.ObmDomainUuid;

public class ArchiveTreatment {

	public static Builder<ArchiveTreatment> builder(ObmDomainUuid domainUuid) {
		return new Builder<ArchiveTreatment>(domainUuid);
	}
	
	public static class Builder<T extends ArchiveTreatment> {
		
		protected final ObmDomainUuid domainUuid;
		protected ArchiveTreatmentRunId runId;
		protected ArchiveStatus status;
		protected Boolean recurrent;
		protected ZonedDateTime scheduledTime;
		protected ZonedDateTime startTime;
		protected ZonedDateTime endTime;
		protected ZonedDateTime higherBoundary;

		protected Builder(ObmDomainUuid domainUuid) {
			Preconditions.checkNotNull(domainUuid);
			this.domainUuid = domainUuid;
		}
		
		public Builder<T> runId(String runId) {
			return runId(ArchiveTreatmentRunId.from(runId));
		}
		
		public Builder<T> runId(ArchiveTreatmentRunId runId) {
			Preconditions.checkNotNull(runId);
			this.runId = runId;
			return this;
		}
		
		public Builder<T> scheduledAt(ZonedDateTime scheduledTime) {
			Preconditions.checkNotNull(scheduledTime);
			this.scheduledTime = scheduledTime;
			return this;
		}
		
		public Builder<T> startedAt(ZonedDateTime startTime) {
			this.startTime = startTime;
			return this;
		}
		
		public Builder<T> terminatedAt(ZonedDateTime endTime) {
			this.endTime = endTime;
			return this;
		}
		
		public Builder<T> higherBoundary(ZonedDateTime higherBoundary) {
			Preconditions.checkNotNull(higherBoundary);
			this.higherBoundary = higherBoundary;
			return this;
		}
		
		public Builder<T> status(ArchiveStatus status) {
			Preconditions.checkNotNull(status);
			this.status = status;
			return this;
		}

		public Builder<T> recurrent(boolean recurrent) {
			this.recurrent = recurrent;
			return this;
		}
		
		@SuppressWarnings("unchecked")
		public T build() {
			Preconditions.checkState(runId != null);
			Preconditions.checkState(scheduledTime != null);
			Preconditions.checkState(higherBoundary != null);
			Preconditions.checkState(status != null);
			Preconditions.checkState(recurrent != null);
			return (T) new ArchiveTreatment(runId, domainUuid, status, scheduledTime, startTime, endTime, higherBoundary, recurrent);
		}
	}
	
	public static final ZonedDateTime FAILED_AT_UNKOWN_DATE = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneOffset.UTC);
	public static final ZonedDateTime NO_DATE = null;
	
	protected final ArchiveTreatmentRunId runId;
	protected final ObmDomainUuid domainUuid;
	protected final ArchiveStatus archiveStatus;
	protected final ZonedDateTime scheduledTime;
	protected final ZonedDateTime startTime;
	protected final ZonedDateTime endTime;
	protected final ZonedDateTime higherBoundary;
	protected final boolean recurrent;

	protected ArchiveTreatment(ArchiveTreatmentRunId runId, ObmDomainUuid  domainUuid, ArchiveStatus archiveStatus, 
			ZonedDateTime scheduledTime, ZonedDateTime startTime, ZonedDateTime endTime, ZonedDateTime higherBoundary, boolean recurrent) {
		this.runId = runId;
		this.domainUuid = domainUuid;
		this.archiveStatus = archiveStatus;
		this.scheduledTime = scheduledTime;
		this.startTime = startTime;
		this.endTime = endTime;
		this.higherBoundary = higherBoundary;
		this.recurrent = recurrent;
	}

	public ArchiveTreatmentRunId getRunId() {
		return runId;
	}

	public ObmDomainUuid getDomainUuid() {
		return domainUuid;
	}

	public ArchiveStatus getArchiveStatus() {
		return archiveStatus;
	}

	public ZonedDateTime getScheduledTime() {
		return scheduledTime;
	}

	public ZonedDateTime getStartTime() {
		return startTime;
	}

	public ZonedDateTime getEndTime() {
		return endTime;
	}

	public ZonedDateTime getHigherBoundary() {
		return higherBoundary;
	}

	public boolean isRecurrent() {
		return recurrent;
	}

	public ArchiveTerminatedTreatment asSuccess(ZonedDateTime endTime) {
		return asTerminatedBuilder(endTime).status(ArchiveStatus.SUCCESS).build();
	}

	public ArchiveTerminatedTreatment asError(ZonedDateTime endTime) {
		return asTerminatedBuilder(endTime).status(ArchiveStatus.ERROR).build();
	}
	
	private ArchiveTerminatedTreatment.Builder<ArchiveTerminatedTreatment> asTerminatedBuilder(ZonedDateTime endTime) {
		return ArchiveTerminatedTreatment
				.forDomain(domainUuid)
				.runId(runId)
				.recurrent(recurrent)
				.scheduledAt(scheduledTime)
				.startedAt(startTime)
				.higherBoundary(higherBoundary)
				.terminatedAt(endTime);
	}

	@Override
	public int hashCode(){
		return Objects.hashCode(runId, domainUuid, archiveStatus,
				startTime, endTime, higherBoundary, recurrent);
	}
	
	@Override
	public boolean equals(Object object){
		if (object instanceof ArchiveTreatment) {
			ArchiveTreatment that = (ArchiveTreatment) object;
			return Objects.equal(this.runId, that.runId)
				&& Objects.equal(this.domainUuid, that.domainUuid)
				&& Objects.equal(this.recurrent, that.recurrent)
				&& Objects.equal(this.archiveStatus, that.archiveStatus)
				&& Objects.equal(this.startTime, that.startTime)
				&& Objects.equal(this.endTime, that.endTime)
				&& Objects.equal(this.higherBoundary, that.higherBoundary);
		}
		return false;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("runId", runId)
			.add("domainUuid", domainUuid)
			.add("recurrent", recurrent)
			.add("archiveStatus", archiveStatus)
			.add("startTime", startTime)
			.add("endTime", endTime)
			.add("higherBoundary", higherBoundary)
			.toString();
	}
}
