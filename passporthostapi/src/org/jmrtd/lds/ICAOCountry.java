/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2013  The JMRTD team
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
 * Special ICAO countries not covered in {@link net.sourceforge.scuba.data.ISOCountry}.
 * Contributed by Aleksandar Kamburov (wise_guybg).
 * 
 * @author Aleksandar Kamburov (wise_guybg)
 * 
 * @version $Revision: $
 */
public class ICAOCountry extends Country {
	
	public static final ICAOCountry
	DE = new ICAOCountry("DE", "D<<", "Germany"),
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
		DE,
		GBD, GBN, GBO, GBP, GBS,
		UNO, UNA, UNK,
		XOM, XCC, XXA, XXB, XXC, XXX
	};
	
	private String name;
	private String alpha2Code;
	private String alpha3Code;
	
	/**
	 * Gets an ICAO country instance.
	 * 
	 * @param alpha3Code a three-digit ICAO country code
	 * 
	 * @return an ICAO country
	 */
	public static Country getInstance(String alpha3Code) {
		for (ICAOCountry country: VALUES) {
			if (country.alpha3Code.equals(alpha3Code)) { return country; }
		}
		try {
			return Country.getInstance(alpha3Code);
		} catch (Exception e) {
			/* NOTE: ignore this exception if it's not a legal 3 digit code. */
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

	/**
	 * Gets the full name of the country.
	 * 
	 * @return a country name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the two-digit country code.
	 * 
	 * @return a two-digit country code
	 */
	public String toAlpha2Code() {
		return alpha2Code;
	}

	/**
	 * Gets the three-digit country code.
	 * 
	 * @return a three-digit country code
	 */
	public String toAlpha3Code() {
		return alpha3Code;
	}
}
