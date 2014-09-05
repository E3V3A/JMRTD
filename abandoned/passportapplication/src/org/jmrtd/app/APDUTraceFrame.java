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
 * $Id: $
 */

package org.jmrtd.app;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;

import net.sourceforge.scuba.smartcards.APDUEvent;
import net.sourceforge.scuba.smartcards.APDUListener;
import net.sourceforge.scuba.smartcards.CommandAPDU;
import net.sourceforge.scuba.smartcards.ResponseAPDU;
import net.sourceforge.scuba.util.Hex;

import org.jmrtd.app.util.FileUtil;
import org.jmrtd.app.util.IconUtil;

/**
 * Frame for tracing APDUs.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 * 
 * @version $Revision: $
 */
public class APDUTraceFrame extends JMRTDFrame {

	protected static final Icon CLEAR_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("paintbrush"));
	
	private static final long serialVersionUID = -584060710792989841L;

	private ActionMap actionMap;
	
	private JTextArea area;
	
	private APDUListener apduListener;

	/**
	 * Constructs an APDU trace frame.
	 */
	public APDUTraceFrame() {
		this("APDU trace");
	}
	
	/**
	 * Constructs an APDU trace frame with a specific title.
	 * 
	 * @param title the title to use
	 */
	public APDUTraceFrame(String title) {
		super(title);
		final APDUTraceFrame frame = this;
		this.apduListener = new APDUListener() {

			@Override
			public void exchangedAPDU(APDUEvent e) {
				frame.exchangedAPDU(e);
			}
			
		};
		actionMap = new ActionMap();
		area = new JTextArea(40, 80);
		area.setFont(new Font("Monospaced", Font.PLAIN, 9));
		Container cp = getContentPane();
		cp.setLayout(new BorderLayout());
		cp.add(new JScrollPane(area), BorderLayout.CENTER);
		
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		menuBar.add(createFileMenu());
		menuBar.add(createViewMenu());
		
		JToolBar toolBar = new JToolBar();
		toolBar.add(getSaveAsAction());
		toolBar.add(getClearAction());
		cp.add(toolBar, BorderLayout.NORTH);
	}

	/**
	 * Gets the APDU listener for raw (possibly encrypted APDUs).
	 * 
	 * @return an APDU listener
	 */
	public APDUListener getRawAPDUListener() {
		return apduListener;
	}

	/**
	 * Gets the APDU listener for plain text (decrypted APDUs)
	 * 
	 * @return an APDU listener
	 */
	public APDUListener getPlainTextAPDUListener() {
		return apduListener;
	}

	/* ONLY PRIVATE METHODS BELOW */
	
	/**
	 * Tell this APDU listener that an APDU was exchanged.
	 * 
	 * @param e the event indicating an APDU was exchanged
	 */
	private synchronized void exchangedAPDU(APDUEvent e) {
		CommandAPDU capdu = e.getCommandAPDU();
		ResponseAPDU rapdu = e.getResponseAPDU();
		area.append(e.getType() + ". C:\n" + Hex.bytesToPrettyString(capdu.getBytes()) + "\n");
		area.append(e.getType() + ". R:\n" + Hex.bytesToPrettyString(rapdu.getBytes()) + "\n");
		area.setCaretPosition(area.getDocument().getLength() - 1);
	}
	
	private JMenu createFileMenu() {
		JMenu menu = new JMenu("File");
		menu.add(getSaveAsAction());
		menu.add(getCloseAction());
		return menu;
	}
	
	private JMenu createViewMenu() {
		JMenu menu = new JMenu("View");
		menu.add(getClearAction());
		return menu;
	}
	
	private Action getCloseAction() {
		Action action = actionMap.get("Close");
		if (action != null) { return action; }
		action = new AbstractAction() {

			private static final long serialVersionUID = -4351062033708816679L;

			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		};
		action.putValue(Action.SMALL_ICON, CLOSE_ICON);
		action.putValue(Action.LARGE_ICON_KEY, CLOSE_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Close Window");
		action.putValue(Action.NAME, "Close");
		actionMap.put("Close", action);
		return action;
	}

	private Action getSaveAsAction() {
		Action action = actionMap.get("SaveAs");
		if (action != null) { return action; }
		final Preferences preferences = Preferences.userNodeForPackage(getClass());
		action = new AbstractAction() {

			private static final long serialVersionUID = 9113082315691234764L;

			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				String directory = preferences.get(JMRTDApp.PASSPORT_LOG_FILES_DIR_KEY, null);
				if (directory != null) {
					fileChooser.setCurrentDirectory(new File(directory));
				}
				fileChooser.setFileFilter(FileUtil.TXT_FILE_FILTER);
				int choice = fileChooser.showSaveDialog(getContentPane());
				switch (choice) {
				case JFileChooser.APPROVE_OPTION:
					try {
						File file = fileChooser.getSelectedFile();
						preferences.put(JMRTDApp.PASSPORT_LOG_FILES_DIR_KEY, file.getParent());
						FileWriter writer = new FileWriter(file);
						writer.write(area.getText());
						writer.close();
						break;
					} catch (IOException fnfe) {
						fnfe.printStackTrace();
					}
				default: break;
				}
			}
		};
		action.putValue(Action.SMALL_ICON, SAVE_AS_ICON);
		action.putValue(Action.LARGE_ICON_KEY, SAVE_AS_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Save log to file");
		action.putValue(Action.NAME, "Save As...");
		actionMap.put("SaveAs", action);
		return action;
	}
	
	private Action getClearAction() {
		Action action = actionMap.get("Clear");
		if (action != null) { return action; }
		action = new AbstractAction() {

			private static final long serialVersionUID = -4322062033708816679L;

			public void actionPerformed(ActionEvent e) {
				area.setText("");
			}
		};
		action.putValue(Action.SMALL_ICON, CLEAR_ICON);
		action.putValue(Action.LARGE_ICON_KEY, CLEAR_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Clear Console");
		action.putValue(Action.NAME, "Clear");
		actionMap.put("Clear", action);
		return action;
	}
}
