/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2013  Linagora
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

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

public class ProvisioningContextListener implements ServletContextListener {

	protected Injector injector;

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		try {
			injector = createInjector(sce.getServletContext());
			SecurityManager securityManager = injector.getInstance(SecurityManager.class);
			SecurityUtils.setSecurityManager(securityManager);
		} catch (Exception e) {
			Throwables.propagate(e);
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
	}

    private Injector createInjector(ServletContext servletContext)
    		throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    	
        return Guice.createInjector(selectGuiceModule(servletContext));
    }

    @VisibleForTesting Module selectGuiceModule(ServletContext servletContext)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		
		return Objects.firstNonNull(newWebXmlModuleInstance(servletContext), new ProvisioningService(servletContext));
	}

    @VisibleForTesting Module newWebXmlModuleInstance(ServletContext servletContext)
    		throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    	
		String guiceModuleClassName = servletContext.getInitParameter("guiceModule");
		if (Strings.isNullOrEmpty(guiceModuleClassName)) {
			return null;
		}
		return (Module) Class.forName(guiceModuleClassName).newInstance();
	}
}
