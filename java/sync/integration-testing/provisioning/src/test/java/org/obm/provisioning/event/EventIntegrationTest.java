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
package org.obm.provisioning.event;

import static com.jayway.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.obm.provisioning.ProvisioningIntegrationTestUtils.commitBatch;
import static org.obm.provisioning.ProvisioningIntegrationTestUtils.startBatch;
import static org.obm.provisioning.ProvisioningIntegrationTestUtils.waitForBatchSuccess;

import java.net.URL;
import java.sql.ResultSet;

import javax.ws.rs.core.Response.Status;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.obm.SolrModuleUtils.DummyCommonsHttpSolrServer;
import org.obm.dao.utils.H2InMemoryDatabase;
import org.obm.dao.utils.H2InMemoryDatabaseTestRule;
import org.obm.guice.GuiceRule;
import org.obm.provisioning.TestingProvisioningModule;
import org.obm.server.WebServer;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.jayway.restassured.http.ContentType;
import com.linagora.obm.sync.JMSServer;

import fr.aliacom.obm.common.domain.ObmDomainUuid;

public class EventIntegrationTest {

	@Rule public TestRule chain = RuleChain
			.outerRule(new GuiceRule(this, new TestingProvisioningModule()))
			.around(new H2InMemoryDatabaseTestRule(new Provider<H2InMemoryDatabase>() {
				@Override
				public H2InMemoryDatabase get() {
					return db;
				}
			}, "dbInitialScriptEvent.sql"));

	@Inject private H2InMemoryDatabase db;
	@Inject private WebServer server;
	@Inject private JMSServer jmsServer;
	@Inject private DummyCommonsHttpSolrServer solrServer;
	
	private URL baseURL;
	
	@Before
	public void init() throws Exception {
		server.start();
		baseURL = new URL("http", "localhost", server.getHttpPort(), "/");
	}

	@After
	public void tearDown() throws Exception {
		jmsServer.stop();
		server.stop();
	}
	
	@Test
	public void testImportICSWhenTheUserIsUnknown() throws Exception {
		ObmDomainUuid obmDomainUuid = ObmDomainUuid.of("ac21bc0c-f816-4c52-8bb9-e50cfbfec5b6");
		String batchId = startBatch(baseURL, obmDomainUuid);
		
		String ics = Resources.toString(Resources.getResource("ics/simple.ics"), Charsets.UTF_8);
		String expectedICS = ics.replaceAll("\n", "\\\\n");
		
		importICS(ics, "unexisting_user@test.tlse.lng");
		commitBatch();
		waitForBatchSuccess(batchId, 1, 0);

		given()
			.auth().basic("admin0@global.virt", "admin0").
		expect()
			.statusCode(Status.OK.getStatusCode())
			.body(containsString("{"
					+ "\"id\":" + batchId + ","
					+ "\"status\":\"SUCCESS\","
					+ "\"operationCount\":1,"
					+ "\"operationDone\":0,"
					+ "\"operations\":["
						+ "{\"status\":\"ERROR\","
						+ "\"entityType\":\"EVENT\","
						+ "\"entity\":\"" + expectedICS + "\","
						+ "\"operation\":\"POST\","
						+ "\"error\":\"org.obm.provisioning.exception.ProcessingException: "
							+ "org.obm.provisioning.dao.exceptions.UserNotFoundException: "
							+ "The user with login unexisting_user@test.tlse.lng with domain id ac21bc0c-f816-4c52-8bb9-e50cfbfec5b6 was not found\"}"
					+ "]"
				+ "}")).
		when()
			.get("");
	}
	
	@Test
	public void testImportICSWhenTheEventIsUnknown() throws Exception {
		ObmDomainUuid obmDomainUuid = ObmDomainUuid.of("ac21bc0c-f816-4c52-8bb9-e50cfbfec5b6");
		String batchId = startBatch(baseURL, obmDomainUuid);
		
		String ics = Resources.toString(Resources.getResource("ics/simple.ics"), Charsets.UTF_8);
		String expectedICS = ics.replaceAll("\n", "\\\\n");
		
		importICS(ics);

		given()
			.auth().basic("admin0@global.virt", "admin0").
		expect()
			.statusCode(Status.OK.getStatusCode())
			.body(containsString("{"
					+ "\"id\":" + batchId + ","
					+ "\"status\":\"IDLE\","
					+ "\"operationCount\":1,"
					+ "\"operationDone\":0,"
					+ "\"operations\":["
						+ "{\"status\":\"IDLE\","
						+ "\"entityType\":\"EVENT\","
						+ "\"entity\":\"" + expectedICS + "\","
						+ "\"operation\":\"POST\","
						+ "\"error\":null}"
					+ "]"
				+ "}")).
		when()
			.get("");
		
		commitBatch();
		waitForBatchSuccess(batchId);
		
		given()
			.auth().basic("admin0@global.virt", "admin0").
		expect()
			.statusCode(Status.OK.getStatusCode())
			.body(containsString("{"
					+ "\"id\":" + batchId + ","
					+ "\"status\":\"SUCCESS\","
					+ "\"operationCount\":1,"
					+ "\"operationDone\":1,"
					+ "\"operations\":["
						+ "{\"status\":\"SUCCESS\","
						+ "\"entityType\":\"EVENT\","
						+ "\"entity\":\"" + expectedICS + "\","
						+ "\"operation\":\"POST\","
						+ "\"error\":null}"
					+ "]"
				+ "}")).
		when()
			.get("");
		
		ResultSet results = db.execute("select count(1) from event");
		results.next();
		assertThat(results.getInt(1)).isEqualTo(1);
		assertThat(solrServer.addCount).isEqualTo(1);
		assertThat(solrServer.commitCount).isEqualTo(1);
	}
	
	@Test
	public void testImportICSWhenTheEventIsAlreadyKnownButSameSequence() throws Exception {
		ObmDomainUuid obmDomainUuid = ObmDomainUuid.of("ac21bc0c-f816-4c52-8bb9-e50cfbfec5b6");
		String ics = Resources.toString(Resources.getResource("ics/simple.ics"), Charsets.UTF_8);
		
		// Create the event in OBM first
		String batchId1 = startBatch(baseURL, obmDomainUuid);
		importICS(ics);
		commitBatch();
		waitForBatchSuccess(batchId1);

		// Then simulate a new import with the same sequence
		String batchId2 = startBatch(baseURL, obmDomainUuid);
		importICS(ics);
		commitBatch();
		waitForBatchSuccess(batchId2);
		
		// Only the first event is expected in the db and in solr
		ResultSet results = db.execute("select count(1) from event");
		results.next();
		assertThat(results.getInt(1)).isEqualTo(1);
		assertThat(solrServer.addCount).isEqualTo(1);
		assertThat(solrServer.commitCount).isEqualTo(1);
	}
	
	@Test
	public void testImportICSWhenTheEventIsAlreadyKnownButHigherSequence() throws Exception {
		ObmDomainUuid obmDomainUuid = ObmDomainUuid.of("ac21bc0c-f816-4c52-8bb9-e50cfbfec5b6");
		String ics = Resources.toString(Resources.getResource("ics/simple.ics"), Charsets.UTF_8);
		
		// Create the event in OBM first
		String batchId1 = startBatch(baseURL, obmDomainUuid);
		importICS(ics);
		commitBatch();
		waitForBatchSuccess(batchId1);

		// Then simulate a new import with the same sequence
		String batchId2 = startBatch(baseURL, obmDomainUuid);
		importICS(ics.replace("SEQUENCE:0", "SEQUENCE:1"));
		commitBatch();
		waitForBatchSuccess(batchId2);
		
		// Only the first event is expected in the db and in solr
		ResultSet results = db.execute("select count(1) from event");
		results.next();
		assertThat(results.getInt(1)).isEqualTo(1);
		assertThat(solrServer.addCount).isEqualTo(2);
		assertThat(solrServer.commitCount).isEqualTo(2);
	}

	private void importICS(String ics) {
		importICS(ics, "user1@test.tlse.lng");
	}
	
	private void importICS(String ics, String userEmail) {
		given()
			.auth().basic("admin0@global.virt", "admin0")
			.body(ics).contentType(ContentType.TEXT).
		expect()
			.statusCode(Status.OK.getStatusCode()).
		when()
			.post("events/" + userEmail);
	}

}
