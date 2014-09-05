/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006  SoS group, ICIS, Radboud University
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
 * $Id: BACPanel.java 297 2008-01-02 14:31:26Z martijno $
 */

package sos.mrtd.sample;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.sourceforge.scuba.smartcards.CardServiceException;

import org.jmrtd.AuthListener;
import org.jmrtd.BACKeySpec;
import org.jmrtd.PassportService;

/**
 * GUI component for step-by-step execution of the
 * BAC protocol.
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision: 297 $
 */
public class BACPanel extends JPanel
{
	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyMMdd");

	private JButton doBACButton;
	private JTextField docNrTF, dateOfBirthTF, dateOfExpiryTF;
	private BACStore bacDB = new BACStore();

	private PassportService service;

	public BACPanel(PassportService passportService) throws CardServiceException {
		super(new FlowLayout());
		this.service = passportService;
		JPanel top = new JPanel(new FlowLayout());
		docNrTF = new JTextField(9);
		dateOfBirthTF = new JTextField(6);
		dateOfExpiryTF = new JTextField(6);
		docNrTF.setText(bacDB.getDocumentNumber());
		dateOfBirthTF.setText(bacDB.getDateOfBirth());
		dateOfExpiryTF.setText(bacDB.getDateOfExpiry());
		top.add(new JLabel("Document number: "));
		top.add(docNrTF);
		top.add(new JLabel("Birth (YYMMDD): "));
		top.add(dateOfBirthTF);
		top.add(new JLabel("Expiry (YYMMDD): "));
		top.add(dateOfExpiryTF);
		doBACButton = new JButton("Authenticate");
		top.add(doBACButton);
		doBACButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					String documentNumber = docNrTF.getText().toUpperCase();
					String dateOfBirth = dateOfBirthTF.getText();
					String dateOfExpiry = dateOfExpiryTF.getText();
					service.doBAC(new BACKeySpec(documentNumber, SDF.parse(dateOfBirth), SDF.parse(dateOfExpiry)));
					bacDB.addEntry(documentNumber, dateOfBirth, dateOfExpiry);
					docNrTF.setText(documentNumber);
				} catch (CardServiceException cse) {
					System.out.println("DEBUG: BAC failed!");
				} catch (ParseException pe) {
					System.out.println("DEBUG: BAC failed!");
				}
			}         
		});
		add(top);
	}

	public void addAuthenticationListener(AuthListener l) {
		service.addAuthenticationListener(l);
	}
}
