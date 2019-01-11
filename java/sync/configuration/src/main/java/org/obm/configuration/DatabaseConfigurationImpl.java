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

package org.obm.configuration;

import org.obm.configuration.utils.IniFile;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class DatabaseConfigurationImpl implements DatabaseConfiguration {

	private static final int DB_MAX_POOL_SIZE_DEFAULT = 10;

	private final IniFile iniFile;

	public static class Factory {
		
		protected IniFile.Factory iniFileFactory;

		public Factory() {
			iniFileFactory = new IniFile.Factory();
		}
		
		public DatabaseConfigurationImpl create(String globalConfigurationFile) {
			return new DatabaseConfigurationImpl(iniFileFactory, globalConfigurationFile);
		}
	}
	
	@Inject
	DatabaseConfigurationImpl(IniFile.Factory iniFileFactory, @Named("globalConfigurationFile") String globalConfigurationFile) {
		this(iniFileFactory.build(globalConfigurationFile));
	}
	
	@VisibleForTesting DatabaseConfigurationImpl(IniFile iniFile) {
		this.iniFile = iniFile;
	}

	@Override
	public Integer getDatabaseMaxConnectionPoolSize() {
		return iniFile.getIntValue(DB_MAX_POOL_SIZE_KEY, DB_MAX_POOL_SIZE_DEFAULT);
	}

	@Override
	public DatabaseFlavour getDatabaseSystem() {
		return DatabaseFlavour.valueOf(iniFile.getStringValue(DB_TYPE_KEY).trim());
	}

	@Override
	public String getDatabaseName() {
		return iniFile.getStringValue(DB_NAME_KEY);
	}

	@Override
	public String getDatabaseHost() {
		return iniFile.getStringValue(DB_HOST_KEY);
	}

	@Override
	public Integer getDatabasePort() {
		return iniFile.getIntegerValue(DB_PORT_KEY, null);
	}

	@Override
	public String getDatabaseLogin() {
		return iniFile.getStringValue(DB_USER_KEY);
	}

	@Override
	public String getDatabasePassword() {
		return IniFile.removeEnclosingQuotes(iniFile.getStringValue(DB_PASSWORD_KEY));
	}

	@Override
	public boolean isPostgresSSLEnabled() {
		return iniFile.getBooleanValue(DB_PG_SSL, false);
	}

	@Override
	public boolean isPostgresSSLNonValidating() {
		return iniFile.getBooleanValue(DB_PG_SSL_NON_VALIDATING, false);
	}

	@Override
	public String getJdbcOptions() {
		return NO_JDBC_OPTION;
	}

	@Override
	public Integer getDatabaseMinConnectionPoolSize() {
		return null;
	}

	@Override
	public boolean isReadOnly() {
		return false;
	}
	
	@Override
	public boolean isAutoTruncateEnabled() {
		return iniFile.getBooleanValue(DB_AUTO_TRUNCATE_PARAMETER, DB_AUTO_TRUNCATE_DEFAULT_VALUE);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(getDatabaseHost(), getDatabasePort(), getDatabaseSystem(), getDatabaseName(),
				getDatabaseLogin(), isReadOnly(), getDatabaseMinConnectionPoolSize(), getDatabaseMaxConnectionPoolSize(), getJdbcOptions());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof IniFileSectionDatabaseConfiguration) {
			IniFileSectionDatabaseConfiguration other = (IniFileSectionDatabaseConfiguration) obj;

			return Objects.equal(getDatabaseHost(), other.getDatabaseHost())
					&& Objects.equal(getDatabasePort(), other.getDatabasePort())
					&& Objects.equal(getDatabaseSystem(), other.getDatabaseSystem())
					&& Objects.equal(getDatabaseName(), other.getDatabaseName())
					&& Objects.equal(getDatabaseLogin(), other.getDatabaseLogin())
					&& Objects.equal(isReadOnly(), other.isReadOnly())
					&& Objects.equal(getDatabaseMinConnectionPoolSize(), other.getDatabaseMinConnectionPoolSize())
					&& Objects.equal(getDatabaseMaxConnectionPoolSize(), other.getDatabaseMaxConnectionPoolSize())
					&& Objects.equal(getJdbcOptions(), other.getJdbcOptions());
		}

		return false;
	}

	@Override
	public String toString() {
		return MoreObjects
				.toStringHelper(getClass())
				.add("host", getDatabaseHost())
				.add("port", getDatabasePort())
				.add("dbType", getDatabaseSystem())
				.add("dbName", getDatabaseName())
				.add("login", getDatabaseLogin())
				.add("readOnly", isReadOnly())
				.add("minPoolSize", getDatabaseMinConnectionPoolSize())
				.add("maxPoolSize", getDatabaseMaxConnectionPoolSize())
				.add("jdbcOptions", getJdbcOptions())
				.toString();
	}

}
