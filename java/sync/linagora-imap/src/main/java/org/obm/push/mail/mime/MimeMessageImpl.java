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

package org.obm.push.mail.mime;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import org.obm.push.mail.bean.IMAPHeaders;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * Represents the mime tree of a message. The tree of a message can be obtained
 * by parsing the BODYSTRUCTURE response from the IMAP server.
 */
public class MimeMessageImpl implements MimeMessage {

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder implements MimePart.Builder<MimeMessage> {
		
		private MimePart part;
		private Integer size;
		private Long uid;

		@Override
		public Builder addChild(MimePart mimePart) {
			from(mimePart);
			return this;
		}
		
		public Builder from(MimePart mimePart) {
			this.part = mimePart;
			return this;
		}
		
		public Builder size(Integer size) {
			this.size = size;
			return this;
		}
		
		public Builder uid(Long uid) {
			this.uid = uid;
			return this;
		}
		
		public MimeMessageImpl build() {
			return new MimeMessageImpl(part, uid, size);
		}
	}
	
	private final Long uid;
	private final MimePart from;
	private final Integer size;

	private MimeMessageImpl(MimePart from, Long uid, Integer size) {
		super();
		this.from = from;
		this.uid = uid;
		this.size = size;
	}

	@Override
	public boolean isSinglePartMessage() {
		return getChildren().size() == 1 && getChildren().get(0).getChildren().isEmpty();
	}

	@Override
	public boolean hasAttachments() {
		for (MimePart part: this.listLeaves(true, false)) {
			String full = part.getFullMimeType();
			if (!("text/plain".equals(full)) && !("text/html".equals(full))) {
				return true;
			}
		}
		return false;
	}

	@Override
	public long getUid() {
		return uid;
	}

	@Override
	public MimePart getInvitation() {
		return from.getInvitation();
	}

	@Override
	public ContentType getContentType() {
		return from.getContentType();
	}

	@Override
	public String getPrimaryType() {
		return from.getPrimaryType();
	}

	@Override
	public String getSubtype() {
		return from.getSubtype();
	}

	@Override
	public List<MimePart> getChildren() {
		return from.getChildren();
	}

	@Override
	public MimeAddress getAddressInternal() {
		return from.getAddress(); 
	}
	
	@Override
	public MimeAddress getAddress() {
		return null;
	}

	@Override
	public BodyParams getBodyParams() {
		return from.getBodyParams();
	}

	@Override
	public BodyParam getBodyParam(String param) {
		return from.getBodyParam(param);
	}

	@Override
	public MimePart getParent() {
		return null;
	}

	@Override
	public void defineParent(MimePart parent, int index) {
		throw new RuntimeException("");
	}

	@Override
	public String getFullMimeType() {
		return from.getFullMimeType();
	}

	@Override
	public boolean containsCalendarMethod() {
		return from.containsCalendarMethod();
	}
	
	@Override
	public boolean isInvitation() {
		return from.isInvitation();
	}

	@Override
	public String getContentTransfertEncoding() {
		return from.getContentTransfertEncoding();
	}

	@Override
	public String getCharset() {
		return from.getCharset();
	}
	
	@Override
	public String getContentId() {
		return from.getContentId();
	}

	@Override
	public String getContentLocation() {
		return from.getContentLocation();
	}

	@Override
	public boolean isInline() {
		return false;
	}
	
	@Override
	public boolean isCancelInvitation() {
		return from.isCancelInvitation();
	}

	@Override
	public boolean isReplyInvitation() {
		return from.isReplyInvitation();
	}
	
	@Override
	public String getName() {
		return from.getName();
	}
	
	@Override
	public boolean isMultipart() {
		return from.isMultipart();
	}
	
	@Override
	public String getMultipartSubtype() {
		return from.getSubtype();
	}

	@Override
	public Collection<MimePart> listLeaves(boolean depthFirst, boolean filterNested) {
		if (from != null) {
			return from.listLeaves(depthFirst, filterNested);
		}
		return ImmutableSet.of();
	}
	
	@Override
	public boolean isAttachment() {
		return false;
	}

	@Override
	public String getAttachmentExtension() {
		return null;
	}
	
	@Override
	public List<MimePart> getSibling() {
		return ImmutableList.of();
	}

	@Override
	public boolean isNested() {
		return false;
	}

	@Override
	public MimePart getMimePart() {
		return from;
	}

	@Override
	public MimePart findRootMimePartInTree() {
		return getMimePart();
	}

	@Override
	public MimePart findMainMessage(ContentType contentType) {
		return from.findMainMessage(contentType);
	}

	@Override
	public Integer getSize() {
		if (size != null) {
			return size;
		}
		return from.getSize();
	}

	@Override
	public boolean hasMultiPartMixedParent() {
		return from.hasMultiPartMixedParent();
	}

	@Override
	public boolean isMultiPartMixed() {
		return from.isMultiPartMixed();
	}

	@Override
	public boolean isFirstElementInParent() {
		return from.isFirstElementInParent();
	}

	@Override
	public boolean hasMimePart(ContentType contentType) {
		return from.hasMimePart(contentType);
	}

	@Override
	public boolean isICSAttachment() {
		return from.isICSAttachment();
	}
	
	@Override
	public InputStream decodeMimeStream(InputStream rawStream) {
		return rawStream;
	}
	
	@Override
	public IMAPHeaders decodeHeaders(InputStream is) throws IOException {
		return from.decodeHeaders(is);
	}

	@Override
	public final int hashCode(){
		return Objects.hashCode(uid, size, from);
	}
	
	@Override
	public final boolean equals(Object object){
		if (object instanceof MimeMessageImpl) {
			MimeMessageImpl that = (MimeMessageImpl) object;
			return Objects.equal(this.uid, that.uid)
					&& Objects.equal(this.size, that.size)
					&& Objects.equal(this.from, that.from);
		}
		return false;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("uid", uid)
			.add("size", size)
			.add("tree", printTree())
			.toString();
	}

	private String printTree() {
		StringBuilder sb = new StringBuilder();
		for (MimePart part: listLeaves(true, false)) {
			sb.append(part.getAddress()).append(" ");
			printMimeDetails(sb, part);
		}
		return sb.toString();
	}

	private void printMimeDetails(StringBuilder sb, MimePart mimeTree) {
		sb.append(mimeTree.getFullMimeType());
		String name = mimeTree.getName();
		if (name != null) {
			sb.append(" " + name);
		}
	}

}