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
package org.obm.sync.addition;

import java.io.Serializable;
import java.util.Date;

import org.obm.sync.dao.EntityId;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public class CommitedElement implements Serializable {
	
	public static Builder builder() {
		return new Builder();
	}
	
	public static class Builder {
		
		private String clientId;
		private Date clientDate;
		private EntityId entityId;
		private Kind kind;
		
		private Builder() {
			super();
		}
		
		public Builder clientId(String clientId) {
			this.clientId = clientId;
			return this;
		}
		
		public Builder clientDate(Date clientDate) {
			this.clientDate = clientDate;
			return this;
		}
		
		public Builder entityId(EntityId entityId) {
			this.entityId = entityId;
			return this;
		}
		
		public Builder kind(Kind kind) {
			this.kind = kind;
			return this;
		}
		
		public CommitedElement build() {
			Preconditions.checkArgument(clientId != null, "clientId is required");
			Preconditions.checkArgument(entityId != null, "entityId is required");
			Preconditions.checkArgument(kind != null, "kind is required");
			return new CommitedElement(clientId, entityId, kind, Optional.fromNullable(clientDate));
		}
	}
	
	private final String clientId;
	private final Optional<Date> clientDate;
	private final EntityId entityId;
	private final Kind kind;
	
	private CommitedElement(String clientId, EntityId entityId, Kind kind, Optional<Date> clientDate) {
		this.clientId = clientId;
		this.entityId = entityId;
		this.kind = kind;
		this.clientDate = clientDate;
	}

	public String getClientId() {
		return clientId;
	}

	public Optional<Date> getClientDate() {
		return clientDate;
	}

	public EntityId getEntityId() {
		return entityId;
	}

	public Kind getKind() {
		return kind;
	}
	
	@Override
	public final int hashCode(){
		return Objects.hashCode(clientId, clientDate, entityId, kind);
	}
	
	@Override
	public final boolean equals(Object object){
		if (object instanceof CommitedElement) {
			CommitedElement that = (CommitedElement) object;
			return Objects.equal(this.clientId, that.clientId)
				&& Objects.equal(this.clientDate, that.clientDate)
				&& Objects.equal(this.entityId, that.entityId)
				&& Objects.equal(this.kind, that.kind);
		}
		return false;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("clientId", clientId)
			.add("clientDate", clientDate)
			.add("entityId", entityId)
			.add("kind", kind)
			.toString();
	}
}
