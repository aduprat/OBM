/* ***** BEGIN LICENSE BLOCK *****
 * Copyright (C) 2011-2014  Linagora
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
package org.obm.service.solr.jms;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.obm.service.solr.SolrDocumentIndexer;
import org.obm.service.solr.SolrRequest;
import org.obm.sync.base.EmailAddress;
import org.obm.sync.book.Address;
import org.obm.sync.book.Contact;
import org.obm.sync.book.InstantMessagingId;
import org.obm.utils.ObmHelper;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import fr.aliacom.obm.common.domain.ObmDomain;

public class ContactUpdateCommand extends ContactCommand {
	
	@Singleton
	public static class Factory {
		
		private ObmHelper obmHelper;

		@Inject
		private Factory(ObmHelper obmHelper) {
			this.obmHelper = obmHelper;
		}

		public ContactUpdateCommand create(ObmDomain domain, String login, Contact data) {
			return new ContactUpdateCommand(domain, login, data, obmHelper);
		}
		
	}
	
	private final ObmHelper obmHelper;

	private ContactUpdateCommand(ObmDomain domain, String login, Contact data, ObmHelper obmHelper) {
		super(domain, login, data);
		this.obmHelper = obmHelper;
	}

	@Override
	public SolrRequest asSolrRequest() {
		try {
			return new SolrDocumentIndexer(getDomain(), getSolrService(), dataToDocument());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private SolrInputDocument dataToDocument() throws SQLException {
		Contact contact = getObject();
		Connection con = null;
		
		Statement st = null;
		try {
			con = obmHelper.getConnection();
			st = con.createStatement();

			ResultSet rsContact = loadIndexableContactFromDatabase(st);
			if (rsContact == null) {
				throw new IllegalStateException("contact with id " + contact.getUid() + " not found.");
			}
			SolrInputDocument document = buildDocumentFromResultSet(rsContact);
			
			ResultSet rsCategory = st.executeQuery("SELECT categorylink_category_id FROM CategoryLink WHERE categorylink_entity_id=" + getObject().getEntityId());
			appendCategories(document, rsCategory);
			return document;
		} finally {
			obmHelper.cleanup(con, st, null);
		}
	}
	

	private void appendCategories(SolrInputDocument document, ResultSet rs) throws SQLException {
		List<Integer> catId = Lists.newArrayList();
		while (rs.next()) {
			catId.add(rs.getInt(1));
		}
		appendField(document, "categoryId", catId);
	}

	private ResultSet loadIndexableContactFromDatabase(Statement st)
			throws SQLException {
		ResultSet rs;
		rs = st.executeQuery("SELECT "
				+ "c.*, ab.*, k.*, cf.*, Website.website_url, Website.website_label, "
				+ "bd.event_id as bd_id, bd.event_date as bd_date, an.event_id as an_id, an.event_date as an_date "
				+ "FROM Contact c "
				+ "INNER JOIN AddressBook ab ON c.contact_addressbook_id=ab.id "
				+ "LEFT JOIN Kind k ON c.contact_kind_id=k.kind_id "
				+ "LEFT JOIN ContactFunction cf ON c.contact_function_id=cf.contactfunction_id "
				+ "LEFT JOIN Event bd ON c.contact_birthday_id=bd.event_id "
				+ "LEFT JOIN Event an ON c.contact_anniversary_id=an.event_id "
				+ "LEFT JOIN ContactEntity ce ON c.contact_id=ce.contactentity_contact_id "	
				+ "LEFT JOIN Website ON ce.contactentity_entity_id=Website.website_entity_id "
				+ "WHERE c.contact_id=" + getObject().getUid());
		if (!rs.next()) {
			return null;
		}
		return rs;
	}

	private SolrInputDocument buildDocumentFromResultSet(ResultSet rs) throws SQLException {
		Contact contact = getObject();
		SolrInputDocument sid = new SolrInputDocument();
		appendField(sid, "id", contact.getUid());
		appendField(sid, "timecreate", rs.getDate("contact_timecreate"));
		appendField(sid, "timeupdate", rs.getDate("contact_timeupdate"));
		appendField(sid, "usercreate", rs.getInt("contact_usercreate"));
		appendField(sid, "usercreate", rs.getInt("contact_userupdate"));
		appendField(sid, "datasource", rs.getInt("contact_datasource_id"));
		appendField(sid, "domain", rs.getInt("contact_domain_id"));
		appendField(sid, "in", rs.getString("name"));
		appendField(sid, "addressbookId", rs.getInt("id"));
		appendField(sid, "company", rs.getString("contact_company"));
		appendField(sid, "companyId", rs.getInt("contact_company_id"));
		appendField(sid, "commonname", rs.getString("contact_commonname"));
		appendField(sid, "lastname", rs.getString("contact_lastname"));
		appendField(sid, "firstname", rs.getString("contact_firstname"));
		appendField(sid, "middlename", rs.getString("contact_middlename"));
		appendField(sid, "suffix", rs.getString("contact_suffix"));
		appendField(sid, "aka", rs.getString("contact_aka"));
		appendField(sid, "kind", rs.getString("kind_minilabel"),
				rs.getString("kind_header"));
		appendField(sid, "manager", rs.getString("contact_manager"));
		appendField(sid, "assistant", rs.getString("contact_assistant"));
		appendField(sid, "spouse", rs.getString("contact_spouse"));
		appendField(sid, "category", rs.getString("contact_category"));
		appendField(sid, "service", rs.getString("contact_service"));
		appendField(sid, "function", rs.getString("contactfunction_label"));
		appendField(sid, "title", rs.getString("contact_title"));
		appendField(sid, "is", (rs.getBoolean("contact_archive") ? "archive" : null),
				(rs.getBoolean("contact_collected") ? "collected" : null),
				(rs.getBoolean("contact_mailing_ok") ? "mailing" : null),
				(rs.getBoolean("contact_newsletter") ? "newsletter" : null));

		appendField(sid, "date", rs.getDate("contact_date"));
		appendField(sid, "birthday", rs.getDate("bd_date"));
		appendField(sid, "birthdayId", rs.getInt("bd_id"));
		appendField(sid, "anniversary", rs.getDate("an_date"));
		appendField(sid, "anniversaryId", rs.getInt("an_id"));

		appendField(sid, "comment1", rs.getString("contact_comment"));
		appendField(sid, "comment2", rs.getString("contact_comment2"));
		appendField(sid, "comment3", rs.getString("contact_comment3"));

		appendField(sid, "from", rs.getString("contact_origin"));
		appendField(sid, "hasACalendar", hasCaluri(rs.getString("website_label"), rs.getString("website_url")));
		rs.close();
		rs = null;

		List<String> mails = Lists.newArrayList();
		for (EmailAddress e : contact.getEmails().values()) {
			mails.add(e.get());
		}
		appendField(sid, "email", mails);

		List<String> phones = Lists.newArrayList();
		List<String> fax = Lists.newArrayList();
		for (String kind : contact.getPhones().keySet()) {
			if (kind.contains("FAX")) {
				fax.add(contact.getPhones().get(kind).getNumber());
			} else {
				phones.add(contact.getPhones().get(kind).getNumber());
			}
		}
		appendField(sid, "phone", phones);
		appendField(sid, "fax", fax);

		List<String> jab = Lists.newArrayList();
		for (InstantMessagingId e : contact.getImIdentifiers().values()) {
			jab.add(e.getId());
		}
		appendField(sid, "jabber", jab);

		List<String> street = Lists.newArrayList();
		List<String> zip = Lists.newArrayList();
		List<String> express = Lists.newArrayList();
		List<String> town = Lists.newArrayList();
		List<String> country = Lists.newArrayList();
		for (Address a : contact.getAddresses().values()) {
			if (a.getStreet() != null) {
				street.add(a.getStreet());
			}
			if (a.getZipCode() != null) {
				zip.add(a.getZipCode());
			}
			if (a.getExpressPostal() != null) {
				express.add(a.getExpressPostal());
			}
			if (a.getTown() != null) {
				town.add(a.getTown());
			}
			if (a.getCountry() != null) {
				country.add(a.getCountry());
			}
		}
		appendField(sid, "street", street);
		appendField(sid, "zipcode", zip);
		appendField(sid, "expresspostal", express);
		appendField(sid, "town", town);
		appendField(sid, "country", country);

		StringBuilder sortable = new StringBuilder();
		if (contact.getLastname() != null) {
			sortable.append(contact.getLastname());
			sortable.append(' ');
		}
		if (contact.getFirstname() != null) {
			sortable.append(contact.getFirstname());
		}
		appendField(sid, "sortable", sortable.toString().trim());
		
		return sid;
	}
	
	
	
	private boolean hasCaluri(String websiteLabel, String websiteUrl) {
		if(websiteUrl != null && websiteLabel != null){
			StringTokenizer websiteTokenizer = new StringTokenizer(websiteLabel, ";");
			try{
				String websiteCategory = websiteTokenizer.nextToken();
				if(websiteCategory.equals("CALURI")){
					return true;
				}
			}
			catch (NoSuchElementException e){
				return false;
			}
		}
		return false;
	}

	private void f(SolrInputDocument sid, String field,
			Collection<Object> values) {
		if (values != null && !values.isEmpty()) {
			SolrInputField sif = new SolrInputField(field);
			for (Object v : values) {
				sif.addValue(v, 1);
			}
			sid.put(field, sif);
		}
	}

	private void appendField(SolrInputDocument sid, String field, Object... values) {
		LinkedList<Object> l = new LinkedList<Object>();
		for (Object o : values) {
			if (o != null) {
				l.add(o);
			}
		}
		f(sid, field, l);
	}
}
