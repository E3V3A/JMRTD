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
 * $Id: PassportGUI.java 894 2009-03-23 15:50:46Z martijno $
 */

package sos.mrtd.sample;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.security.Provider;
import java.security.Security;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import net.sourceforge.scuba.smartcards.CardEvent;
import net.sourceforge.scuba.smartcards.CardManager;
import net.sourceforge.scuba.smartcards.CardService;

import org.jmrtd.PassportEvent;
import org.jmrtd.PassportListener;
import org.jmrtd.PassportManager;
import org.jmrtd.PassportService;

import sos.mrtd.sample.apdutest.APDUTestPanel;

/**
 * Simple graphical application to demonstrate the
 * JMRTD passport host API.
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision: 894 $
 */
public class PassportGUI extends JPanel implements PassportListener
{  
	public static final File JMRTD_USER_DIR = getApplicationDataDir();

	private static final String APPLICATION_NAME = "JMRTD (jmrtd.sourceforge.net)";

	private static final Dimension PREFERRED_SIZE = new Dimension(800, 600);

	private static final Provider PROVIDER =
		new org.bouncycastle.jce.provider.BouncyCastleProvider();

	private APDULogPanel log;
	private JTabbedPane tabbedPane;

	/**
	 * Constructs the GUI.
	 *
	 * @param arg command line arguments, are ignored for now.
	 */
	public PassportGUI(String[] arg) {
		super(new BorderLayout());
		try {
			Security.insertProviderAt(PROVIDER, 4);
			// service = new PassportApduService(new PCSCCardService());
			JPanel northPanel = new JPanel(new FlowLayout());
			JLabel blockSizeLabel = new JLabel("   Max. read file block:");
			JTextField blockSizeText = new JTextField("255");
			blockSizeText.setEditable(true);
			blockSizeText.setEnabled(true);
			blockSizeText.addCaretListener(new CaretListener(){
				public void caretUpdate(CaretEvent e) {
					JTextField f = (JTextField)e.getSource();
					try {
						int n = Integer.parseInt(f.getText());
						PassportService.maxBlockSize = n;
					} catch(NumberFormatException nfe) {
					}
				}             
			});
			northPanel.add(blockSizeLabel);
			northPanel.add(blockSizeText);
			add(northPanel, BorderLayout.NORTH);
			log = new APDULogPanel();
			add(log, BorderLayout.SOUTH);

			tabbedPane = new JTabbedPane();
			PassportManager pm = PassportManager.getInstance();
			pm.addPassportListener(this);
			CardManager cm = CardManager.getInstance();
			
			/* DEBUG: For debugging! */
//			CardManager cm = CardManager.getInstance();
//			cm.addCardTerminalListener(new CardTerminalListener() {
//				public void cardInserted(CardEvent ce) {
//					System.out.println("DEBUG: " + ce);	
//				}
//
//				public void cardRemoved(CardEvent ce) {
//					System.out.println("DEBUG: " + ce);
//				}
//			});
			
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void passportInserted(PassportEvent ce) {
		try {
			PassportService service = ce.getService();
			service.addAPDUListener(log);
			service.open();

			BACPanel bacPanel = new BACPanel(service);
			MRZPanel mrzPanel = new MRZPanel(service);
			FacePanel facePanel = new FacePanel(service);
			PAPanel paPanel = new PAPanel(service);
			AAPanel aaPanel = new AAPanel(service);
			APDUTestPanel apduPanel = new APDUTestPanel(service);
			CopyAndBurnPanel ccPanel = new CopyAndBurnPanel(service);
			bacPanel.addAuthenticationListener(mrzPanel);
			bacPanel.addAuthenticationListener(facePanel);
			bacPanel.addAuthenticationListener(paPanel);
			bacPanel.addAuthenticationListener(aaPanel);
			bacPanel.addAuthenticationListener(ccPanel);
			tabbedPane.setPreferredSize(new Dimension(600, 400));
			tabbedPane.addTab("BAC", bacPanel);
			tabbedPane.addTab("MRZ", mrzPanel);
			tabbedPane.addTab("Face", facePanel);
			tabbedPane.addTab("PA", paPanel);
			tabbedPane.addTab("AA", aaPanel);
			tabbedPane.addTab("APDU test", apduPanel);
			tabbedPane.addTab("Clone & Copy", ccPanel);
            //ClonePanel clonePanel = new ClonePanel(service, this);
            //bacPanel.addAuthenticationListener(clonePanel);
            //tabbedPane.addTab("Clone Passport", clonePanel);            
			add(tabbedPane, BorderLayout.CENTER);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		setEnabled(true);
		revalidate();
	}

	public void passportRemoved(PassportEvent ce) {
		CardService service = ce.getService();
		if (service != null) {
			service.close();
		}
		tabbedPane.removeAll();
		setEnabled(false);
		revalidate();
	}
	
	public void cardInserted(CardEvent ce) {
		/* Do nothing. */
	}
	
	public void cardRemoved(CardEvent ce) {
		/* Do nothing. */
	}

	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		tabbedPane.setEnabled(enabled);
	}

	public Dimension getPreferredSize() {
		return PREFERRED_SIZE;
	}

	private static File getApplicationDataDir() {
		String osName = System.getProperty("os.name").toLowerCase();
		String userHomeName = System.getProperty("user.home");
		if (osName.indexOf("windows") > -1) {
			String appDataDirName = System.getenv("APPDATA");   
			File appDataDir = appDataDirName != null ? new File(appDataDirName) : new File (userHomeName, "Application Data");
			File jmrtdDir = new File(appDataDir, "JMRTD");
			if (!jmrtdDir.isDirectory()) { jmrtdDir.mkdirs(); }
			return jmrtdDir;
		} else {
			File jmrtdDir = new File(userHomeName, ".jmrtd");
			if (!jmrtdDir.isDirectory()) {
				jmrtdDir.mkdirs();
			}
			return jmrtdDir;
		}
	}

	/**
	 * Main method creates a GUI instance and puts it in a frame.
	 *
	 * @param arg command line arguments.
	 */
	public static void main(String[] arg) {
		try {
			PassportGUI gui = new PassportGUI(arg);
			JFrame frame = new JFrame(APPLICATION_NAME);
			frame.getContentPane().add(gui);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.pack();
			frame.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
