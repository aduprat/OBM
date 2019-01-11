/* ***** BEGIN LICENSE BLOCK *****
 * Copyright (C) 2014  Linagora
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

package org.obm.imap.archive.resources;

import javax.inject.Inject;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.glassfish.hk2.api.Factory;
import org.obm.domain.dao.DomainDao;
import org.obm.provisioning.dao.exceptions.DaoException;
import org.obm.provisioning.dao.exceptions.DomainNotFoundException;

import fr.aliacom.obm.common.domain.ObmDomain;
import fr.aliacom.obm.common.domain.ObmDomainUuid;

public class ObmDomainFactory implements Factory<ObmDomain> {

	public static final String PATH_PARAM = "treatment";
	public static final String PATH = "{" + PATH_PARAM + "}";
	
	private final DomainDao domainDao;
	
	@PathParam(PATH_PARAM)
	private String domainUuid;

	@Inject
	private ObmDomainFactory(DomainDao domainDao) {
		this.domainDao = domainDao;
	}
	
	@Override
	public void dispose(ObmDomain instance) {
	}
	
	@Override
	public ObmDomain provide() {

		if (domainUuid == null) {
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		try {
			ObmDomainUuid checkedDomainUuid = checkDomainUuid(domainUuid);
			return domainDao.findDomainByUuid(checkedDomainUuid);
		} catch (DomainNotFoundException e) {
			throw new WebApplicationException(Status.NOT_FOUND);
		} catch (DaoException e) {
			throw new RuntimeException(e);
		}
	}
	
	private ObmDomainUuid checkDomainUuid(String domainUuid) {
		try {
			return ObmDomainUuid.of(domainUuid);
		} catch (IllegalArgumentException e) {
			throw new WebApplicationException(Status.BAD_REQUEST);
		}
	}
}
