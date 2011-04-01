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
public class ICAOCountry extends Country {

	private static final ICAOCountry
	GBD = new ICAOCountry("GB","GBD","British Dependent territories citizen"),
	GBN = new ICAOCountry("GB","GBN","British National (Overseas)"),
	GBO = new ICAOCountry("GB","GBO","British Overseas citizen"),
	GBP = new ICAOCountry("GB","GBP","British Protected person"),
	GBS = new ICAOCountry("GB","GBS","British Subject"),
	UNO = new ICAOCountry("UN","UNO","United Nations Organization"),
	UNA = new ICAOCountry("UN","UNA","United Nations Agency"),
	UNK = new ICAOCountry("UN","UNK","United Nations Interim Administration Mission in Kosovo"),
	XOM = new ICAOCountry("XO","XOM","Sovereign Military Order of Malta"),
	XCC = new ICAOCountry("XC","XCC","Carribean Community"),
	XXA = new ICAOCountry("XX","XXA","Stateless person"),
	XXB = new ICAOCountry("XX","XXB","Refugee"),
	XXC = new ICAOCountry("XX","XXC","Refugee (other)"),
	XXX = new ICAOCountry("XX","XXX","Unspecified");

	private static ICAOCountry[] VALUES = {
		GBD, GBN, GBO, GBP, GBS,
		UNO, UNA, UNK,
		XOM, XCC, XXA, XXB, XXC, XXX
	};
	
	private String name;
	private String alpha2Code;
	private String alpha3Code;
	
	public static ICAOCountry getInstance(String alpha3Code) {
		for (ICAOCountry country: VALUES) {
			if (country.alpha3Code == alpha3Code) { return country; }
		}
		throw new IllegalArgumentException("Illegal ICAO country alpha 3 code " + alpha3Code);
	}
	
	/**
	 * Prevent caller from creating instance.
	 */
	private ICAOCountry() {
	}

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
