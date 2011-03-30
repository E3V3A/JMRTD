/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2010  The JMRTD team
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 * $Id: $
 */

package org.jmrtd.lds;

import net.sourceforge.scuba.data.Country;

/**
 * Contributed by Aleksandar Kamburov (wise_guybg).
 */
public enum ICAOCountry implements Country {

	GBD("GB","GBD","British Dependent territories citizen"),
	GBN("GB","GBN","British National (Overseas)"),
	GBO("GB","GBO","British Overseas citizen"),
	GBP("GB","GBP","British Protected person"),
	GBS("GB","GBS","British Subject"),
	UNO("UN","UNO","United Nations Organization"),
	UNA("UN","UNA","United Nations Agency"),
	UNK("UN","UNK","United Nations Interim Administration Mission in Kosovo"),
	XOM("XO","XOM","Sovereign Military Order of Malta"),
	XCC("XC","XCC","Carribean Community"),
	XXA("XX","XXA","Stateless person"),
	XXB("XX","XXB","Refugee"),
	XXC("XX","XXC","Refugee (other)"),
	XXX("XX","XXX","Unspecified");

	private String name;
	private String alpha2Code;
	private String alpha3Code;

	private ICAOCountry(String alpha2Code, String alpha3Code, String name) {
		this.alpha2Code = alpha2Code;
		this.alpha3Code = alpha3Code;
		this.name = name;
	}

	public int valueOf() {
		return -1;
	}

	public String getName() {
		return name;
	}

	public String toAlpha2Code() {
		return alpha2Code;
	}

	public String toAlpha3Code() {
		return alpha3Code;
	}
}
