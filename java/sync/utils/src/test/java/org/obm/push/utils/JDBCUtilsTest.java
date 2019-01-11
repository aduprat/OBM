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
package org.obm.push.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

import org.junit.Test;


public class JDBCUtilsTest {

	@Test
	public void noExceptionInResultSetClose() throws SQLException {
		ResultSet rs = createMock(ResultSet.class);
		rs.close();
		expectLastCall();
		replay(rs);
		
		Throwable failure = JDBCUtils.closeResultSetThenGetFailure(rs);
		
		verify(rs);
		assertThat(failure).isNull();
	}
	
	@Test
	public void sqlExceptionInResultSetCloseIsReturn() throws SQLException {
		ResultSet rs = createMock(ResultSet.class);
		rs.close();
		expectLastCall().andThrow(new SQLException());
		replay(rs);
		
		Throwable failure = JDBCUtils.closeResultSetThenGetFailure(rs);
		
		verify(rs);
		assertThat(failure).isExactlyInstanceOf(SQLException.class);
	}
	
	@Test
	public void runtimeExceptionInResultSetCloseIsReturn() throws SQLException {
		ResultSet rs = createMock(ResultSet.class);
		rs.close();
		expectLastCall().andThrow(new RuntimeException());
		replay(rs);
		
		Throwable failure = JDBCUtils.closeResultSetThenGetFailure(rs);
		
		verify(rs);
		assertThat(failure).isExactlyInstanceOf(RuntimeException.class);
	}

	@Test
	public void noExceptionInStatementClose() throws SQLException {
		Statement statement = createMock(Statement.class);
		statement.close();
		expectLastCall();
		replay(statement);
		
		Throwable failure = JDBCUtils.closeStatementThenGetFailure(statement);
		
		verify(statement);
		assertThat(failure).isNull();
	}
	
	@Test
	public void sqlExceptionInStatementCloseIsReturn() throws SQLException {
		Statement statement = createMock(Statement.class);
		statement.close();
		expectLastCall().andThrow(new SQLException());
		replay(statement);
		
		Throwable failure = JDBCUtils.closeStatementThenGetFailure(statement);
		
		verify(statement);
		assertThat(failure).isExactlyInstanceOf(SQLException.class);
	}
	
	@Test
	public void runtimeExceptionInStatementCloseIsReturn() throws SQLException {
		Statement statement = createMock(Statement.class);
		statement.close();
		expectLastCall().andThrow(new RuntimeException());
		replay(statement);
		
		Throwable failure = JDBCUtils.closeStatementThenGetFailure(statement);
		
		verify(statement);
		assertThat(failure).isExactlyInstanceOf(RuntimeException.class);
	}


	@Test
	public void noExceptionInConnectionClose() throws SQLException {
		Connection connection = createMock(Connection.class);
		connection.close();
		expectLastCall();
		replay(connection);
		
		Throwable failure = JDBCUtils.closeConnectionThenGetFailure(connection);
		
		verify(connection);
		assertThat(failure).isNull();
	}
	
	@Test
	public void sqlExceptionInConnectionCloseIsReturn() throws SQLException {
		Connection connection = createMock(Connection.class);
		connection.close();
		expectLastCall().andThrow(new SQLException());
		replay(connection);
		
		Throwable failure = JDBCUtils.closeConnectionThenGetFailure(connection);
		
		verify(connection);
		assertThat(failure).isExactlyInstanceOf(SQLException.class);
	}
	
	@Test
	public void runtimeExceptionInConnectionCloseIsReturn() throws SQLException {
		Connection connection = createMock(Connection.class);
		connection.close();
		expectLastCall().andThrow(new RuntimeException());
		replay(connection);
		
		Throwable failure = JDBCUtils.closeConnectionThenGetFailure(connection);
		
		verify(connection);
		assertThat(failure).isExactlyInstanceOf(RuntimeException.class);
	}

	@Test
	public void cleanupFailureInResultSetLetsCallOthers() throws SQLException {
		ResultSet resultSet = createMock(ResultSet.class);
		resultSet.close();
		expectLastCall().andThrow(new SQLException());
		
		Connection connection = createMock(Connection.class);
		connection.close();
		expectLastCall();
		
		Statement statement = createMock(Statement.class);
		statement.close();
		expectLastCall();
		
		replay(resultSet, statement, connection);
		
		try {
			JDBCUtils.cleanup(connection, statement, resultSet);
		} catch (Throwable e) {
			// there will be a failure
		}
		
		verify(resultSet, statement, connection);
	}

	@Test
	public void cleanupFailureInStatementLetsCallOthers() throws SQLException {
		ResultSet resultSet = createMock(ResultSet.class);
		resultSet.close();
		expectLastCall();
		
		Connection connection = createMock(Connection.class);
		connection.close();
		expectLastCall();
		
		Statement statement = createMock(Statement.class);
		statement.close();
		expectLastCall().andThrow(new SQLException());
		
		replay(resultSet, statement, connection);
		
		try {
			JDBCUtils.cleanup(connection, statement, resultSet);
		} catch (Throwable e) {
			// there will be a failure
		}
		
		verify(resultSet, statement, connection);
	}

	@Test
	public void cleanupFailureInConnectionLetsCallOthers() throws SQLException {
		ResultSet resultSet = createMock(ResultSet.class);
		resultSet.close();
		expectLastCall();
		
		Connection connection = createMock(Connection.class);
		connection.close();
		expectLastCall().andThrow(new SQLException());
		
		Statement statement = createMock(Statement.class);
		statement.close();
		expectLastCall();
		
		replay(resultSet, statement, connection);
		
		try {
			JDBCUtils.cleanup(connection, statement, resultSet);
		} catch (Throwable e) {
			// there will be a failure
		}
		
		verify(resultSet, statement, connection);
	}

	@Test
	public void throwRuntimeIfNotNullWhenNull() {
		JDBCUtils.throwRuntimeIfNotNull(null);
	}

	@Test(expected=RuntimeException.class)
	public void throwRuntimeIfNotNullWhenCatchedException() {
		JDBCUtils.throwRuntimeIfNotNull(new SQLException());
	}

	@Test(expected=RuntimeException.class)
	public void throwRuntimeIfNotNullWhenCatchedExceptionKeepCause() {
		try {
			JDBCUtils.throwRuntimeIfNotNull(new SQLException());
		} catch (Throwable e) {
			assertThat(e.getCause()).isExactlyInstanceOf(SQLException.class);
			throw e;
		}
	}

	@Test
	public void throwFirstNotNullWhenNothing() {
		JDBCUtils.throwFirstNotNull();
	}
	
	@Test
	public void throwFirstNotNullWhenNoFailure() {
		JDBCUtils.throwFirstNotNull(null, null, null);
	}

	@Test(expected=RuntimeException.class)
	public void throwFirstNotNullWhenLast() {
		try {
			JDBCUtils.throwFirstNotNull(null, null, new SQLException());
		} catch (Throwable e) {
			assertThat(e.getCause()).isExactlyInstanceOf(SQLException.class);
			throw e;
		}
	}

	@Test(expected=RuntimeException.class)
	public void throwFirstNotNullWhenMany() {
		try {
			JDBCUtils.throwFirstNotNull(new IOException(), new SQLException());
		} catch (Throwable e) {
			assertThat(e.getCause()).isExactlyInstanceOf(IOException.class);
			throw e;
		}
	}

	@Test
	public void testGetIntegerWhenValueIsNull() throws SQLException {
		ResultSet rs = createMock(ResultSet.class);

		expect(rs.getInt("field")).andReturn(0);
		expect(rs.wasNull()).andReturn(true);
		replay(rs);

		assertThat(JDBCUtils.getInteger(rs, "field")).isNull();

		verify(rs);
	}

	@Test
	public void testGetInteger() throws SQLException {
		ResultSet rs = createMock(ResultSet.class);

		expect(rs.getInt("field")).andReturn(1);
		expect(rs.wasNull()).andReturn(false);
		replay(rs);

		assertThat(JDBCUtils.getInteger(rs, "field")).isEqualTo(1);

		verify(rs);
	}

	@Test(expected = NullPointerException.class)
	public void testGetIntegerWithNullResultSet() throws SQLException {
		JDBCUtils.getInteger(null, "field");
	}

	@Test(expected = NullPointerException.class)
	public void testGetIntegerWithNullFieldName() throws SQLException {
		JDBCUtils.getInteger(createMock(ResultSet.class), null);
	}

	@Test(expected = NullPointerException.class)
	public void getDateTimeShouldThrowWhenNoResultSet() throws Exception {
		JDBCUtils.getDateTime(null, null, null);
	}

	@Test(expected = NullPointerException.class)
	public void getDateTimeShouldThrowWhenNoFieldName() throws Exception {
		JDBCUtils.getDateTime(createMock(ResultSet.class), null, null);
	}

	@Test(expected = NullPointerException.class)
	public void getDateTimeShouldThrowWhenNoDateTimeZone() throws Exception {
		JDBCUtils.getDateTime(createMock(ResultSet.class), "field_name", null);
	}

	@Test
	public void getDateTimeShouldReturnNullWhenNoSuchFieldName() throws Exception {
		ResultSet resultSet = createMock(ResultSet.class);
		String fieldName = "field_name";
		expect(resultSet.getTimestamp(fieldName))
			.andReturn(null);
		replay(resultSet);
		
		ZonedDateTime dateTime = JDBCUtils.getDateTime(resultSet, fieldName, ZoneId.of(ZoneOffset.UTC.getId()));
		
		verify(resultSet);
		assertThat(dateTime).isNull();
	}

	@Test
	public void getDateTimeShouldReturnDateTimeWhenEveryThingIsOk() throws Exception {
		ZonedDateTime expectedDateTime = ZonedDateTime.parse("2014-07-15T15:55:00.000Z");
		Timestamp timestamp = Timestamp.from(expectedDateTime.toInstant());
		ResultSet resultSet = createMock(ResultSet.class);
		String fieldName = "field_name";
		expect(resultSet.getTimestamp(fieldName))
			.andReturn(timestamp);
		replay(resultSet);
		
		ZonedDateTime dateTime = JDBCUtils.getDateTime(resultSet, fieldName, ZoneId.of(ZoneOffset.UTC.getId()));
		
		verify(resultSet);
		assertThat(dateTime).isEqualTo(expectedDateTime);
	}
	
	@Test
	public void toTimestampShouldReturnNullWhenNoDateTimeProvided() {
		assertThat(JDBCUtils.toTimestamp(null)).isNull();
	}
	
	@Test
	public void toTimestampShouldReturnTimestampWhenDateTimeProvided() {
		Timestamp expectedTimestamp = Timestamp.valueOf("2014-07-15 15:55:00");
		ZonedDateTime dateTime = ZonedDateTime.ofInstant(expectedTimestamp.toInstant(), ZoneId.of(ZoneOffset.UTC.getId()));
		
		assertThat(JDBCUtils.toTimestamp(dateTime)).isEqualTo(expectedTimestamp);
	}

	@Test(expected=NullPointerException.class)
	public void testSetOptionalDateShouldFailWhenPreparedStatementIsNull() throws SQLException {
		JDBCUtils.setOptionalDate(null, null, 0);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testSetOptionalDateShouldFailWhenIndexIsZero() throws SQLException {
		JDBCUtils.setOptionalDate(createNiceMock(PreparedStatement.class), null, 0);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testSetOptionalDateShouldFailWhenIndexIsNegative() throws SQLException {
		JDBCUtils.setOptionalDate(createNiceMock(PreparedStatement.class), null, -1);
	}

	@Test
	public void testSetOptionalDateAndReturnFalseShouldSetNullWhenDateIsNull() throws Exception {
		PreparedStatement ps = createMock(PreparedStatement.class);

		ps.setNull(1, Types.DATE);
		expectLastCall();
		replay(ps);

		try {
			assertThat(JDBCUtils.setOptionalDate(ps, null, 1)).isFalse();
		} finally {
			verify(ps);
		}
	}

	@Test
	public void testSetOptionalDateAndReturnTrueShouldSetDateWhenDateIsNotNull() throws Exception {
		Date date = DateUtils.date("2015-02-11");
		PreparedStatement ps = createMock(PreparedStatement.class);

		ps.setDate(1, JDBCUtils.getDateWithoutTime(date));
		expectLastCall();
		replay(ps);

		try {
			assertThat(JDBCUtils.setOptionalDate(ps, date, 1)).isTrue();
		} finally {
			verify(ps);
		}
	}

}
