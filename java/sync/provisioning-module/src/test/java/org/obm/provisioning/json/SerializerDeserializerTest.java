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
package org.obm.provisioning.json;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.StringContains.containsString;

import javax.ws.rs.core.Response.Status;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.obm.guice.GuiceModule;
import org.obm.guice.GuiceRunner;
import org.obm.provisioning.CommonDomainEndPointEnvTest;
import org.obm.provisioning.dao.exceptions.DaoException;
import org.obm.provisioning.dao.exceptions.DomainNotFoundException;

import io.restassured.http.ContentType;

@RunWith(GuiceRunner.class)
@GuiceModule(CommonDomainEndPointEnvTest.Env.class)
public class SerializerDeserializerTest extends CommonDomainEndPointEnvTest {
	
	@Test
	public void testObmUserDeserializerAndSerializer() throws DaoException, DomainNotFoundException {
		expectDomain();
		expectSuccessfulAuthentication("user", "password");
		mocksControl.replay();
		
		given()
			.auth().basic("user@domain", "password")
			.body(obmUserToJsonString()).contentType(ContentType.JSON).
		expect()
			.statusCode(Status.OK.getStatusCode())
			.body(containsString(obmUserToJsonStringWithoutGroup())).
		when()
			.post("/do/tests/on/serialization/of/user");
		
		mocksControl.verify();
	}
	
	@Test
	public void testObmUserSerializerWithNullValue() throws DaoException, DomainNotFoundException {
		expectDomain();
		expectSuccessfulAuthentication("user", "password");
		mocksControl.replay();
		
		given()
			.auth().basic("user@domain", "password")
			.body(obmUserJsonStringWithNullValue()).contentType(ContentType.JSON).
		expect()
			.statusCode(Status.OK.getStatusCode())
			.body(containsString(obmUserJsonStringWithNullValueAfterDeserialization())).
		when()
			.post("/do/tests/on/serialization/of/user");
		
		mocksControl.verify();
	}

	@Test
	public void testObmUserDeserializerWithMinimalRepresentation() throws DaoException, DomainNotFoundException {
		expectDomain();
		expectSuccessfulAuthentication("user", "password");
		mocksControl.replay();

		given()
			.auth().basic("user@domain", "password")
			.body(minimalObmUserJsonString()).contentType(ContentType.JSON).
		expect()
			.statusCode(Status.OK.getStatusCode()).
		when()
			.post("/do/tests/on/serialization/of/user");

		mocksControl.verify();
	}

	@Test
	public void testObmUserDeserializerWhenNoLastName() throws DaoException, DomainNotFoundException {
		expectDomain();
		expectSuccessfulAuthentication("user", "password");
		mocksControl.replay();

		given()
			.auth().basic("user@domain", "password")
			.body(obmUserJsonStringWithoutLastName()).contentType(ContentType.JSON).
		expect()
			.statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).
		when()
			.post("/do/tests/on/serialization/of/user");

		mocksControl.verify();
	}

	@Test
	public void testObmUserDeserializerWhenNoProfile() throws DaoException, DomainNotFoundException {
		expectDomain();
		expectSuccessfulAuthentication("user", "password");
		mocksControl.replay();

		given()
			.auth().basic("user@domain", "password")
			.body(obmUserJsonStringWithoutProfile()).contentType(ContentType.JSON).
		expect()
			.statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).
		when()
			.post("/do/tests/on/serialization/of/user");

		mocksControl.verify();
	}

	@Test
	public void testGroupDeserializerAndSerializerWithFullJsonAndNullValue() throws DaoException, DomainNotFoundException {
		expectDomain();
		expectSuccessfulAuthentication("user", "password");
		mocksControl.replay();
		
		given()
			.auth().basic("user@domain", "password")
			.body(
					"{" +
						"\"id\":\"groupExtId\"," +
						"\"name\":\"null\"," +
						"\"email\":\"group1@domain\"," +
						"\"description\":null" +
					"}")
			.contentType(ContentType.JSON).
		expect()
			.statusCode(Status.OK.getStatusCode())
			.body(containsString(
					"{" +
						"\"id\":\"groupExtId\"," +
						"\"name\":\"null\"," +
						"\"email\":\"group1@domain\"," +
						"\"description\":null," +
						"\"members\":" +
							"{" +
								"\"users\":[]," +
								"\"subgroups\":[]" +	
							"}" +
					"}")).
		when()
			.post("/do/tests/on/serialization/of/group");
		
		mocksControl.verify();
	}
	
	@Test
	public void testGroupDeserializerAndSerializerWithFullJson() throws DaoException, DomainNotFoundException {
		expectDomain();
		expectSuccessfulAuthentication("user", "password");
		mocksControl.replay();
		
		given()
			.auth().basic("user@domain", "password")
			.body(
					"{" +
						"\"id\":\"groupExtId\"," +
						"\"name\":\"group1\"," +
						"\"email\":\"group1@domain\"," +
						"\"description\":\"description\"" +
					"}")
			.contentType(ContentType.JSON).
		expect()
			.statusCode(Status.OK.getStatusCode())
			.body(containsString(
					"{" +
						"\"id\":\"groupExtId\"," +
						"\"name\":\"group1\"," +
						"\"email\":\"group1@domain\"," +
						"\"description\":\"description\"," +
						"\"members\":" +
							"{" +
								"\"users\":[]," +
								"\"subgroups\":[]" +	
							"}" +
					"}")).
		when()
			.post("/do/tests/on/serialization/of/group");
		
		mocksControl.verify();
	}
	
	@Test
	public void testGroupDeserializerAndSerializerWithPartialJson() throws DaoException, DomainNotFoundException {
		expectDomain();
		expectSuccessfulAuthentication("user", "password");
		mocksControl.replay();
		
		given()
			.auth().basic("user@domain", "password")
			.body(
					"{" +
						"\"id\":\"groupExtId\"" +
					"}")
			.contentType(ContentType.JSON).
		expect()
			.statusCode(Status.OK.getStatusCode())
			.body(containsString(
					"{" +
						"\"id\":\"groupExtId\"," +
						"\"name\":null," +
						"\"email\":null," +
						"\"description\":null," +
						"\"members\":" +
							"{" +
								"\"users\":[]," +
								"\"subgroups\":[]" +	
							"}" +
					"}")).
		when()
			.post("/do/tests/on/serialization/of/group");
		
		mocksControl.verify();
	}
	
	@Test
	public void testRuntimeExceptionSerializer() throws DaoException, DomainNotFoundException {
		expectDomain();
		expectSuccessfulAuthentication("user", "password");
		mocksControl.replay();
		
		given()
			.auth().basic("user@domain", "password").
		expect()
			.statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
			.body(containsString(
					"{" +
						"\"message\":\"foo\"," +
						"\"type\":\"java.lang.IllegalStateException\"" +
					"}")).
		when()
			.get("/do/tests/on/serialization/of/runtimeException");
		
		mocksControl.verify();
	}
	
	@Test
	public void testExceptionSerializer() throws DaoException, DomainNotFoundException {
		expectDomain();
		expectSuccessfulAuthentication("user", "password");
		mocksControl.replay();
		
		given()
			.auth().basic("user@domain", "password").
		expect()
			.statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
			.body(containsString(
					"{" +
						"\"message\":\"foo\"," +
						"\"type\":\"java.lang.Exception\"" +
					"}")).
		when()
			.get("/do/tests/on/serialization/of/exception");
		
		mocksControl.verify();
	}
	
	@Test
	public void testAddressBookCreationDeserializerAndSerializerWhenNoReference() throws DaoException, DomainNotFoundException {
		expectDomain();
		expectSuccessfulAuthentication("user", "password");
		mocksControl.replay();
		
		given()
			.auth().basic("user@domain", "password")
			.body(
					"{"
						+ "\"name\":\"the name\","
						+ "\"role\":\"PRIMARY\""
					+ "}")
			.contentType(ContentType.JSON).
		expect()
			.statusCode(Status.OK.getStatusCode())
			.body(containsString(
					"{"
						+ "\"name\":\"the name\","
						+ "\"role\":\"PRIMARY\""
					+ "}")).
		when()
			.post("/do/tests/on/serialization/of/addressbookcreation");
		
		mocksControl.verify();
	}
	
	@Test
	public void testAddressBookCreationDeserializerAndSerializerWhenFull() throws DaoException, DomainNotFoundException {
		expectDomain();
		expectSuccessfulAuthentication("user", "password");
		mocksControl.replay();
		
		given()
			.auth().basic("user@domain", "password")
			.body(
					"{"
						+ "\"name\":\"the name\","
						+ "\"role\":\"collected\","
						+ "\"reference\":{"
							+ "\"value\":\"1234\","
							+ "\"origin\":\"exchange\""
						+ "}"
					+ "}")
			.contentType(ContentType.JSON).
		expect()
			.statusCode(Status.OK.getStatusCode())
			.body(containsString(
					"{"
						+ "\"name\":\"the name\","
						+ "\"role\":\"COLLECTED\","
						+ "\"reference\":{"
							+ "\"value\":\"1234\","
							+ "\"origin\":\"exchange\""
						+ "}"
					+ "}")).
		when()
			.post("/do/tests/on/serialization/of/addressbookcreation");
		
		mocksControl.verify();
	}
}
