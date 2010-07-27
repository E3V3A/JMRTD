package org.jmrtd.cert;

import java.security.Principal;

import net.sourceforge.scuba.data.Country;

public class CVCPrincipal implements Principal
{
	private Country country;
	private String mnemonic;
	private String seqNumber;

	public CVCPrincipal(Country country, String mnemonic, String seqNumber) {
		if (mnemonic == null || mnemonic.length() > 9) { throw new IllegalArgumentException("Wrong length mnemonic"); }
		if (seqNumber == null || seqNumber.length() != 5) { throw new IllegalArgumentException("Wrong length seqNumber"); }
		this.country = country;
		this.mnemonic = mnemonic;
		this.seqNumber = seqNumber;
	}

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
