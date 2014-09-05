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
 * $Id: LDSPanel.java 894 2009-03-23 15:50:46Z martijno $
 */

package sos.mrtd.sample;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.swing.HexField;
import net.sourceforge.scuba.swing.HexViewPanel;

import org.jmrtd.AAEvent;
import org.jmrtd.AuthListener;
import org.jmrtd.BACEvent;
import org.jmrtd.EACEvent;
import org.jmrtd.PassportApduService;
import org.jmrtd.PassportService;
import org.jmrtd.SecureMessagingWrapper;

/**
 * Convenient GUI component for accessing the LDS.
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision: 894 $
 */
public class LDSPanel extends JPanel
implements AuthListener
{
	private static final byte[] ZERO_DATA = new byte[256];
	private HexField fidTF, offsetTF, leTF;
	private HexViewPanel hexviewer;
	private JButton selectButton, readBinaryButton, readNextButton, saveButton;

	short offset;
	int bytesRead;

	private PassportApduService service;
	private SecureMessagingWrapper wrapper;

	public LDSPanel(PassportApduService service) {
		super(new BorderLayout());
		setService(service);
		this.wrapper = null;
		hexviewer = new HexViewPanel(ZERO_DATA);
		JPanel north = new JPanel(new FlowLayout());
		fidTF = new HexField(2);
		selectButton = new JButton("Select File");
		selectButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try { pressedSelectButton(); } catch (CardServiceException cse) { cse.printStackTrace(); }
			}			
		});
		saveButton = new JButton("Save File");
		saveButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				pressedSaveButton();
			}			
		});
		north.add(new JLabel("File: "));
		north.add(fidTF);
		north.add(selectButton);
		north.add(saveButton);
		JPanel south = new JPanel(new FlowLayout());
		leTF = new HexField(1);
		leTF.setValue(0xFF);
		offsetTF = new HexField(2);
		readBinaryButton = new JButton("Read Binary");
		readBinaryButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			   try { pressedReadBinaryButton(); } catch (CardServiceException cse) { cse.printStackTrace(); }	
			}
		});
		readNextButton = new JButton("Read Next");
		readNextButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try { pressedReadNextButton(); } catch (CardServiceException cse) { cse.printStackTrace(); }
			}
		});
		south.add(new JLabel("Offset: "));
		south.add(offsetTF);
		south.add(new JLabel("Length: "));
		south.add(leTF);
		south.add(readBinaryButton);
		south.add(readNextButton);
		add(north, BorderLayout.NORTH);
		add(hexviewer, BorderLayout.CENTER);
		add(south, BorderLayout.SOUTH);
	}

	public void setService(PassportApduService service) {
		this.service = service;
	}

	private void pressedSaveButton() {
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Save file");
		// chooser.setCurrentDirectory(currentDir);
		chooser.setFileHidingEnabled(false);
		int n = chooser.showOpenDialog(this);
		if (n != JFileChooser.APPROVE_OPTION) {
			System.out.println("DEBUG: select file canceled...");
			return;
		}
		byte[] fidBytes = fidTF.getValue();
		final short fid = (short) (((fidBytes[0] & 0x000000FF) << 8)
				| (fidBytes[1] & 0x000000FF));
		final File file = chooser.getSelectedFile();
		new Thread(new Runnable() {
			public void run() {
				try {
					PassportService s = new PassportService(service);
					s.setWrapper(wrapper);
					DataInputStream fileIn = new DataInputStream(s.readFile(fid));
					byte[] data = new byte[fileIn.available()];
					fileIn.readFully(data);
					OutputStream out = new FileOutputStream(file);
					out.write(data, 0, data.length);
					out.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	private void pressedSelectButton() throws CardServiceException {
		byte[] fid = fidTF.getValue();
		service.sendSelectFile(wrapper,
				(short) (((fid[0] & 0x000000FF) << 8) | (fid[1] & 0x000000FF)));
	}

	private void pressedReadBinaryButton() throws CardServiceException {
		bytesRead = 0;
		int le = leTF.getValue()[0] & 0x000000FF;
		byte[] offsetBytes = offsetTF.getValue();
		offset = (short)(((offsetBytes[0] & 0x000000FF) << 8)
				| (offsetBytes[1] & 0x000000FF));
		byte[] data = service.sendReadBinary(wrapper, offset, le, false);
		remove(hexviewer);
		hexviewer = new HexViewPanel(data, offset);
		add(hexviewer, BorderLayout.CENTER);
		bytesRead = data.length;
		setVisible(false);
		setVisible(true);
		// repaint();
	}

	private void pressedReadNextButton() throws CardServiceException {
		offset += bytesRead;
		offsetTF.setValue(offset & 0x000000000000FFFFL);
		pressedReadBinaryButton();
	}

	public void performedBAC(BACEvent be) {
		this.wrapper = be.getWrapper();
	}

    public void performedEAC(EACEvent ee) {
        this.wrapper = ee.getWrapper();
    }

	public void performedAA(AAEvent ae) {
	}
}

