/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2013-2014 Linagora
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
package org.obm.provisioning;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;

import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.Response.Status;

import org.awaitility.Duration;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import fr.aliacom.obm.common.domain.ObmDomainUuid;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

public class ProvisioningIntegrationTestUtils {

	public static String getAdminUserJson(){
		return "{\"id\":\"Admin0ExtId\",\"login\":\"admin0\",\"lastname\":\"Lastname\",\"profile\":\"admin\","
				+ "\"firstname\":\"Firstname\",\"commonname\":null,\"password\":\"admin0\","
				+ "\"kind\":null,\"title\":null,\"description\":null,\"company\":null,\"service\":null,"
				+ "\"direction\":null,\"addresses\":[],\"town\":null,\"zipcode\":null,\"business_zipcode\":null,"
				+ "\"country\":\"0\",\"phones\":[],\"mobile\":null,\"faxes\":[],\"archived\":false,\"mail_quota\":\"0\","
				+ "\"mail_server\":null,\"mails\":[\"admin0@*\"],\"effectiveMails\":[\"admin0@test.tlse.lng\"],\"hidden\":false,"
				+ "\"nomad_enabled\":false,\"nomad_mail\":null,\"nomad_allowed\":false,\"nomad_local_copy\":false,\"timecreate\":null,\"timeupdate\":null,"
				+ "\"expiration_date\":null,\"delegation\":null,\"delegation_target\":null,"
				+ "\"groups\":[],\"samba_allowed\":false,\"samba_home_drive\":null,\"samba_home_folder\":null,\"samba_logon_script\":null}";
	}
	
	public static String getAdminUserJsonWithGroup(){
		return "{\"id\":\"Admin0ExtId\",\"login\":\"admin0\",\"lastname\":\"Lastname\",\"profile\":\"admin\","
				+ "\"firstname\":\"Firstname\",\"commonname\":null,\"password\":\"admin0\",\"kind\":null,\"title\":null,"
				+ "\"description\":null,\"company\":null,\"service\":null,\"direction\":null,\"addresses\":[],\"town\":null,"
				+ "\"zipcode\":null,\"business_zipcode\":null,\"country\":\"0\",\"phones\":[],\"mobile\":null,\"faxes\":[],\"archived\":false,"
				+ "\"mail_quota\":\"0\",\"mail_server\":null,\"mails\":[\"admin0@*\"],\"effectiveMails\":[\"admin0@global.virt\"],\"hidden\":false,"
				+ "\"nomad_enabled\":false,\"nomad_mail\":null,\"nomad_allowed\":false,\"nomad_local_copy\":false,\"timecreate\":null,\"timeupdate\":null,"
				+ "\"expiration_date\":null,\"delegation\":null,\"delegation_target\":null,"
				+ "\"groups\":[{\"id\":\"GroupWithUsers\",\"url\":\"/abf7c2bc-aa84-461c-b057-ee42c5dce40a/groups/GroupWithUsers\"}],"
				+ "\"samba_allowed\":false,\"samba_home_drive\":null,\"samba_home_folder\":null,\"samba_logon_script\":null}";
	}

	public static String startBatch(URL baseURL, ObmDomainUuid obmDomainUuid) {
		RestAssured.baseURI = domainUrl(baseURL, obmDomainUuid);

		String batchId = given()
				.auth().basic("admin0@global.virt", "admin0")
				.post("/batches")
				.jsonPath()
				.getString("id");

		RestAssured.baseURI = batchUrl(baseURL, obmDomainUuid, batchId);
		return batchId;
	}

	public static void commitBatch() {
		given()
			.auth().basic("admin0@global.virt", "admin0")
			.put();
	}
	
	public static String groupUrl(URL baseURL, ObmDomainUuid domain) {
		return domainUrl(baseURL, domain) + "/groups/";
	}

	public static String userUrl(URL baseURL, ObmDomainUuid domain) {
		return domainUrl(baseURL, domain) + "/users/";
	}
	
	public static String profileUrl(URL baseURL, ObmDomainUuid domain) {
		return domainUrl(baseURL, domain) + "/profiles/";
	}
	
	public static String eventUrl(URL baseURL, ObmDomainUuid domain) {
		return domainUrl(baseURL, domain) + "/events/";
	}
	
	public static String batchUrl(URL baseURL, ObmDomainUuid domain, String batchId) {
		return domainUrl(baseURL, domain) + "/batches/" + batchId;
	}

	public static String domainUrl(URL baseURL, ObmDomainUuid domain) {
		return baseUrl(baseURL) + "/" + domain.get();
	}
	
	public static String baseUrl(URL baseURL) {
		return baseURL.toExternalForm() + ProvisioningService.PROVISIONING_ROOT_PATH;
	}

	public static void waitForBatchSuccess(final String batchId) {
		waitForBatchSuccess(batchId, 1, 1);
	}
	
	public static void waitForBatchSuccess(final String batchId, final int operationCount, final int operationDone) {
		waitForBatchSuccess(batchId, operationCount, operationDone, Matchers.any(String.class));
	}
	
	public static void waitForBatchSuccess(final String batchId, final int operationCount, final int operationDone, final Matcher<String> extraMatcher) {
        await()
	        .atMost(1, TimeUnit.MINUTES)
	        .pollDelay(Duration.ONE_SECOND)
	        .pollInterval(Duration.ONE_SECOND)
	        .until(new Callable<Boolean>() {

				@Override
				public Boolean call() throws Exception {
					try {
						given()
							.auth().basic("admin0@global.virt", "admin0").
						expect()
							.statusCode(Status.OK.getStatusCode())
							.body(Matchers.allOf(
								extraMatcher,
								containsString("{"
									+ "\"id\":" + batchId + ","
									+ "\"status\":\"SUCCESS\","
									+ "\"operationCount\":" + operationCount + ","
									+ "\"operationDone\":" + operationDone + ","
								))).
						when()
							.get("");
						return true;
					} catch (AssertionError e) {
						return false;
					}
				}
	        	
	        });
	}
	
	public static void createAddressBook(String json, String userEmail) {
		given()
			.auth().basic("admin0@global.virt", "admin0")
			.body(json).contentType(ContentType.JSON).
		expect()
			.statusCode(Status.OK.getStatusCode()).
		when()
			.post("addressbooks/" + userEmail);
	}
}
