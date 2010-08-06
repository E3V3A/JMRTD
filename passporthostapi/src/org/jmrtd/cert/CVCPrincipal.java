package org.jmrtd.cert;

import java.security.Principal;

import net.sourceforge.scuba.data.Country;
import net.sourceforge.scuba.data.ISOCountry;

public class CVCPrincipal implements Principal
{
	private Country country;
	private String mnemonic;
	private String seqNumber;

	public CVCPrincipal(String name) {
		if (name == null || name.length() < 2 + 5 || name.length() > 2 + 9 + 5) {
			throw new IllegalArgumentException("Name should be <Country(2)><Mnemonic(<=9)><SeqNum(5)> formatted");
		}
		country = ISOCountry.getInstance(name.substring(0, 2).toUpperCase());
		mnemonic = name.substring(2, name.length() - 5);
		seqNumber = name.substring(name.length() - 5 - 1, name.length());
	}
	
	public CVCPrincipal(Country country, String mnemonic, String seqNumber) {
		if (mnemonic == null || mnemonic.length() > 9) { throw new IllegalArgumentException("Wrong length mnemonic"); }
		if (seqNumber == null || seqNumber.length() != 5) { throw new IllegalArgumentException("Wrong length seqNumber"); }
		this.country = country;
		this.mnemonic = mnemonic;
		this.seqNumber = seqNumber;
	}

	/**
	 * Consists of the concatenation of
	 * country code (length 2), mnemonic (length < 9) and
	 * sequence number (length 5).
	 * 
	 * @return the name of the principal
	 */
	public String getName() {
		return country.toAlpha2Code() + mnemonic + seqNumber;
	}
	
	public String toString() {
		return country.toAlpha2Code() + "/" + mnemonic + "/" + seqNumber;
	}

	/**
	 * @return the country
	 */
	public Country getCountry() {
		return country;
	}

	/**
	 * @return the mnemonic
	 */
	public String getMnemonic() {
		return mnemonic;
	}

	/**
	 * @return the seqNumber
	 */
	public String getSeqNumber() {
		return seqNumber;
	}
}
