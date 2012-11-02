/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2012  The JMRTD team
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
 * $Id: BACEntryField.java 764 2009-02-04 13:49:38Z martijno $
 */

package org.jmrtd.app;

import java.text.ParseException;
import java.util.Date;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;

import org.jmrtd.BACKeySpec;
import org.jmrtd.app.swing.DateEntryField;
import org.jmrtd.app.swing.MRZEntryField;

/**
 * Text field for entering BAC entries.
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 *
 * @version $Revision: 764 $
 */
public class BACEntryField extends Box implements BACEntrySource {

	private static final long serialVersionUID = 6780286228880496605L;

	private static final Logger LOGGER = Logger.getLogger("org.jmrtd");
	
	private MRZEntryField docNrTF;
	private DateEntryField dateOfBirthField, dateOfExpiryField;

	/**
	 * Creates a BAC entry field.
	 */
	public BACEntryField() {
		super(BoxLayout.X_AXIS);
		docNrTF = new MRZEntryField(9);
		dateOfBirthField = new DateEntryField(DateEntryField.YEAR_MODE_2_DIGITS);
		dateOfExpiryField = new DateEntryField(DateEntryField.YEAR_MODE_2_DIGITS);
		add(new JLabel("Doc.nr.: "));
		add(Box.createHorizontalStrut(5));
		add(docNrTF);
		add(Box.createHorizontalStrut(10));
		add(new JLabel("DoB: "));
		add(Box.createHorizontalStrut(5));
		add(dateOfBirthField);
		add(Box.createHorizontalStrut(10));
		add(new JLabel("DoE: "));
		add(Box.createHorizontalStrut(5));
		add(dateOfExpiryField);
	}

	/**
	 * Creates a BAC entry field with a specific value.
	 * 
	 * @param bacEntry the value to use
	 */
	public BACEntryField(BACKeySpec bacEntry) {
		this();
		try {
			setValue(bacEntry.getDocumentNumber(), bacEntry.getDateOfBirth(), bacEntry.getDateOfExpiry());
		} catch (ParseException e) {
			/* NOTE: a parse exception at this point would indicate lack of input checking in BACKeySpec */
			LOGGER.severe("Error in entering BAC entry: " + e.getMessage());
//			throw new IllegalArgumentException(e.getMessage());
		}
	}

	/**
	 * Creates a BAC entry field with a specific value.
	 *
	 * @param documentNumber part of the BAC entry value
	 * @param dateOfBirth part of the BAC entry value
	 * @param dateOfExpiry part of the BAC entry value
	 * 
	 * @throws ParseException if the supplied arguments could not be parsed
	 */
	public BACEntryField(String documentNumber, String dateOfBirth, String dateOfExpiry) throws ParseException {
		this();
		setValue(documentNumber, dateOfBirth, dateOfExpiry);
	}

	/**
	 * Creates a BAC entry field with a specific value.
	 * 
	 * @param documentNumber part of the BAC entry value
	 * @param dateOfBirth part of the BAC entry value
	 * @param dateOfExpiry part of the BAC entry value
	 */
	public BACEntryField(String documentNumber, Date dateOfBirth, Date dateOfExpiry) {
		this();
		setValue(documentNumber, dateOfBirth, dateOfExpiry);
	}

	/**
	 * Gets the document number.
	 * 
	 * @return the document number
	 */
	public String getDocumentNumber() {
		return docNrTF.getText();
	}

	/**
	 * Gets the date of birth.
	 * 
	 * @return the date of birth
	 */
	public Date getDateOfBirth() {
		return dateOfBirthField.getDate();
	}

	/**
	 * Gets the date of expiry.
	 * 
	 * @return a date
	 */
	public Date getDateOfExpiry() {
		return dateOfExpiryField.getDate();
	}

	/**
	 * Enables or disables this entry field.
	 * 
	 * @param boolean indicates whether to enable or disable
	 */
	public void setEnabled(boolean b) {
		docNrTF.setEnabled(b);
		dateOfBirthField.setEnabled(b);
		dateOfExpiryField.setEnabled(b);
	}
	
	/* ONLY PRIVATE METHODS BELOW */
	
	private void setValue(String documentNumber, String dateOfBirth, String dateOfExpiry) throws ParseException {
		docNrTF.setText(documentNumber);
		dateOfBirthField.setDate(dateOfBirth);
		dateOfExpiryField.setDate(dateOfExpiry);
	}

	private void setValue(String documentNumber, Date dateOfBirth, Date dateOfExpiry) {
		docNrTF.setText(documentNumber);
		dateOfBirthField.setDate(dateOfBirth);
		dateOfExpiryField.setDate(dateOfExpiry);
	}
}
