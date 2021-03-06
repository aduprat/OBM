/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2011-2012  Linagora
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;

import javax.transaction.UserTransaction;

import org.obm.annotations.technicallogging.KindToBeLogged;
import org.obm.annotations.technicallogging.ResourceType;
import org.obm.annotations.technicallogging.TechnicalLogging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

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

	/**
	 * PostgreSQL only
	 * 
	 * @param con
	 * @return
	 * @throws SQLException
	 */
	public static int lastInsertId(Connection con) throws SQLException {
		Statement st = null;
		ResultSet rs = null;
		try {
			st = con.createStatement();
			rs = st.executeQuery("SELECT lastval()");
			rs.next();
			return rs.getInt(1);
		} finally {
			cleanup(null, st, rs);
		}
	}

	public static final void cleanup(Connection con, Statement ps, ResultSet rs) {
		Throwable resultSetFailure = closeResultSetThenGetFailure(rs);
		Throwable statementFailure = closeStatementThenGetFailure(ps);
		Throwable connectionFailure = closeConnectionThenGetFailure(con);
		throwFirstNotNull(resultSetFailure, statementFailure, connectionFailure);
	}

	@VisibleForTesting static void throwFirstNotNull(Throwable...failures) {
		for (Throwable failure : failures) {
			throwRuntimeIfNotNull(failure);
		}
	}

	@VisibleForTesting static void throwRuntimeIfNotNull(Throwable failure) {
		if (failure != null) {
			Throwables.propagate(failure);
		}
	}

	@VisibleForTesting static Throwable closeResultSetThenGetFailure(ResultSet rs) {
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

	@VisibleForTesting static Throwable closeStatementThenGetFailure(Statement ps) {
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

	@TechnicalLogging(kindToBeLogged=KindToBeLogged.RESOURCE, onEndOfMethod=true, resourceType=ResourceType.JDBC_CONNECTION)
	@VisibleForTesting static Throwable closeConnectionThenGetFailure(Connection con) {
		if (con != null) {
			try {
				con.close();
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

	public static Date getDate(ResultSet rs, String fieldName)
			throws SQLException {
		Preconditions.checkNotNull(rs);
		Preconditions.checkNotNull(fieldName);
		Timestamp timestamp = rs.getTimestamp(fieldName);
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

	public static java.sql.Date getDateWithoutTime(Date lastSync) {
		return new java.sql.Date(lastSync.getTime());
	}

	public static int convertNegativeIntegerToZero(ResultSet rs, String fieldName) throws SQLException {
		Preconditions.checkNotNull(rs);
		Preconditions.checkNotNull(fieldName);
		return Math.max(0, rs.getInt(fieldName));
	}
}
