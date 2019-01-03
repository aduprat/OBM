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


package org.obm.imap.archive.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.expect;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import org.obm.utils.ObmHelper;

public class TestingDateProviderTest {

	private IMocksControl control;
	
	private ObmHelper obmHelper;
	private TestingDateProvider testee;
	private DateFormat dateFormat;
	
	@Before
	public void setup() {
		control = createControl();
		
		dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		obmHelper = control.createMock(ObmHelper.class);
		testee = new TestingDateProviderImpl(obmHelper);
	}
	
	@Test
	public void getDateShouldReturnDateFromObmHelperWhenDefaultDuration() throws ParseException {
		Date expectedDate = dateFormat.parse("2014-08-27");
		expect(obmHelper.getDate())
			.andReturn(expectedDate);
		
		control.replay();
		Date date = testee.getDate();
		control.verify();
		
		assertThat(date).isEqualTo(expectedDate);
	}
	
	@Test
	public void getDateShouldReturnUpdatedDateWhenDurationHasBeenSet() throws ParseException {
		Date dateTime = dateFormat.parse("2014-08-17");
		expect(obmHelper.getDate())
			.andReturn(dateTime).times(2);
		
		Date expectedDate = DateUtils.addDays(dateTime, 10);
		
		control.replay();
		testee.setReferenceDate(dateFormat.parse("2014-08-27"));
		Date date = testee.getDate();
		control.verify();
		
		assertThat(date).isEqualTo(expectedDate);
	}
	
	@Test
	public void getDateShouldReturnUpdatedDateWhenDurationHasBeenSetAndIsNegative() throws ParseException {
		Date dateTime = dateFormat.parse("2014-08-17");
		expect(obmHelper.getDate())
			.andReturn(dateTime).times(2);
		
		Date expectedDate = DateUtils.addDays(dateTime, -10);
		
		control.replay();
		testee.setReferenceDate(dateFormat.parse("2014-08-07"));
		Date date = testee.getDate();
		control.verify();
		
		assertThat(date).isEqualTo(expectedDate);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void setReferenceDateShouldThrowWhenNullArgument() {
		testee.setReferenceDate(null);
	}
}
