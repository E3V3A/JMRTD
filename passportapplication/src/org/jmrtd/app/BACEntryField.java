/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2008  The JMRTD team
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

import java.util.Date;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;

import net.sourceforge.scuba.swing.DateEntryField;
import net.sourceforge.scuba.swing.MRZEntryField;

/**
 * Text field for entering BAC entries.
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 *
 * @version $Revision: 764 $
 */
public class BACEntryField extends Box implements BACEntrySource
{
	private static final long serialVersionUID = 6780286228880496605L;

	private MRZEntryField docNrTF;
	private DateEntryField dateOfBirthField, dateOfExpiryField;

	public BACEntryField() {
		super(BoxLayout.X_AXIS);
		docNrTF = new MRZEntryField(9);
		dateOfBirthField = new DateEntryField();
		dateOfExpiryField = new DateEntryField();
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

	public BACEntryField(BACEntry bacEntry) {
		this();
		setValue(bacEntry.getDocumentNumber(), bacEntry.getDateOfBirth(), bacEntry.getDateOfExpiry());
	}

	public BACEntryField(String documentNumber, Date dateOfBirth, Date dateOfExpiry) {
		this();
		setValue(documentNumber, dateOfBirth, dateOfExpiry);
	}

	private void setValue(String documentNumber, Date dateOfBirth, Date dateOfExpiry) {
		docNrTF.setText(documentNumber);
		dateOfBirthField.setDate(dateOfBirth);
		dateOfExpiryField.setDate(dateOfExpiry);
	}

	public String getDocumentNumber() {
		return docNrTF.getText();
	}

	public Date getDateOfBirth() {
		return dateOfBirthField.getDate();
	}

	public Date getDateOfExpiry() {
		return dateOfExpiryField.getDate();
	}

	public void setEnabled(boolean b) {
		docNrTF.setEnabled(b);
		dateOfBirthField.setEnabled(b);
		dateOfExpiryField.setEnabled(b);
	}
}
