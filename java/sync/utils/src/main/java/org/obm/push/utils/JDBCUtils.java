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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import javax.transaction.UserTransaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class JDBCUtils {

	private final static Logger logger = LoggerFactory
			.getLogger(JDBCUtils.class);

	public static final void rollback(Connection con) {
		if (con != null) {
			try {
				con.rollback();
			} catch (SQLException se) {
				logger.error(se.getMessage(), se);
			}
		}
	}

	public static final void cleanup(Connection con, Statement ps, ResultSet rs) {
		cleanup(con, ps, rs, connectionCloser);
	}
	
	public static final void cleanup(Connection con, Statement ps, ResultSet rs,
			ConnectionCloser connectionCloser) {
		
		Throwable resultSetFailure = closeResultSetThenGetFailure(rs);
		Throwable statementFailure = closeStatementThenGetFailure(ps);
		Throwable connectionFailure = connectionCloser.closeConnectionThenGetFailure(con);
		throwFirstNotNull(resultSetFailure, statementFailure, connectionFailure);
	}
	
	public static interface ConnectionCloser {
		Throwable closeConnectionThenGetFailure(Connection connection);
	}

	/*
	 * This connection closer is there only to make the static method overridable
	 */
	private static ConnectionCloser connectionCloser = new ConnectionCloser() {
		
		@Override
		public Throwable closeConnectionThenGetFailure(Connection connection) {
			return JDBCUtils.closeConnectionThenGetFailure(connection);
		}
	};
	
	public static void throwFirstNotNull(Throwable...failures) {
		for (Throwable failure : failures) {
			throwRuntimeIfNotNull(failure);
		}
	}

	public static void throwRuntimeIfNotNull(Throwable failure) {
		if (failure != null) {
			throw new RuntimeException(failure);
		}
	}

	public static Throwable closeResultSetThenGetFailure(ResultSet rs) {
		if (rs != null) {
			try {
				rs.close();
			} catch (Throwable se) {
				logger.error(se.getMessage(), se);
				return se;
			}
		}
		return null;
	}

	public static Throwable closeStatementThenGetFailure(Statement ps) {
		if (ps != null) {
			try {
				ps.close();
			} catch (Throwable se) {
				logger.error(se.getMessage(), se);
				return se;
			}
		}
		return null;
	}
	
	public static Throwable closeConnectionThenGetFailure(Connection connection) {
		if (connection != null) {
			try {
				connection.close();
			} catch (Throwable se) {
				logger.error(se.getMessage(), se);
				return se;
			}
		}
		return null;
	}

	public static void rollback(UserTransaction ut) {
		try {
			ut.rollback();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	public static Date getDate(ResultSet rs, String fieldName) throws SQLException {
		Timestamp timestamp = getTimestamp(rs, fieldName);
		if (timestamp != null) {
			return new Date(timestamp.getTime());
		} else {
			return null;
		}
	}

	public static Date getDate(ResultSet rs, Integer fieldNumber)
			throws SQLException {
		Preconditions.checkNotNull(rs);
		Preconditions.checkNotNull(fieldNumber);
		Timestamp timestamp = rs.getTimestamp(fieldNumber);
		if (timestamp != null) {
			return new Date(timestamp.getTime());
		} else {
			return null;
		}
	}

	public static ZonedDateTime getDateTime(ResultSet rs, String fieldName, ZoneId dateTimeZone) throws SQLException {
		Preconditions.checkNotNull(dateTimeZone);
		Timestamp timestamp = getTimestamp(rs, fieldName);
		if (timestamp != null) {
			return ZonedDateTime.ofInstant(timestamp.toInstant(), dateTimeZone);
		} else {
			return null;
		}
	}

	private static Timestamp getTimestamp(ResultSet rs, String fieldName) throws SQLException {
		Preconditions.checkNotNull(rs);
		Preconditions.checkNotNull(fieldName);
		return rs.getTimestamp(fieldName);
	}

	public static java.sql.Date getDateWithoutTime(Date lastSync) {
		return new java.sql.Date(lastSync.getTime());
	}

	public static int convertNegativeIntegerToZero(ResultSet rs, String fieldName) throws SQLException {
		Preconditions.checkNotNull(rs);
		Preconditions.checkNotNull(fieldName);
		return Math.max(0, rs.getInt(fieldName));
	}

	public static Integer getInteger(ResultSet rs, String fieldName) throws SQLException {
		Preconditions.checkNotNull(rs);
		Preconditions.checkNotNull(fieldName);

		int value = rs.getInt(fieldName);

		return !rs.wasNull() ? value : null;
	}
	
	public static Timestamp toTimestamp(ZonedDateTime dateTime) {
		if (dateTime == null) {
			return null;
		}
		return Timestamp.from(dateTime.toInstant());
	}

	public static boolean setOptionalDate(PreparedStatement ps, Date date, int idx) throws SQLException {
		Preconditions.checkNotNull(ps);
		Preconditions.checkArgument(idx >= 1, "indexes in prepared statements are >= 1");

		if (date != null) {
			ps.setDate(idx, getDateWithoutTime(date));
			return true;
		} else {
			ps.setNull(idx, Types.DATE);
			return false;
		}
	}

}
