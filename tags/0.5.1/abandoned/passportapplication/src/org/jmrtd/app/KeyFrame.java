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
 * $Id: KeyFrame.java 893 2009-03-23 15:43:42Z martijno $
 */

package org.jmrtd.app;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Key;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import org.jmrtd.app.swing.KeyPanel;
import org.jmrtd.app.util.FileUtil;
import org.jmrtd.app.util.IconUtil;

/**
 * Frame for displaying (and saving to file) keys.
 *
 * @author The JMRTD team (info@jmrtd.org)
 * 
 * @version $Revision: 893 $
 */
public class KeyFrame extends JMRTDFrame {

	private static final long serialVersionUID = -514612440541711549L;

	private static final Icon SAVE_AS_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("disk"));
	private static final Icon CLOSE_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("bin"));

	private ActionMap actionMap;
	private KeyPanel keyPanel;

	/**
	 * Constructs a frame.
	 * 
	 * @param key the key
	 */
	public KeyFrame(Key key) {
		this("Key", key);
	}

	/**
	 * Constructs a frame.
	 * 
	 * @param title title
	 * @param key the key
	 */
	public KeyFrame(String title, Key key) {
		super(title);
		
		actionMap = new ActionMap();

		/* Menu bar */
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(createFileMenu());
		setJMenuBar(menuBar);

		/* Frame content */
		keyPanel = new KeyPanel(key);
		Container cp = getContentPane();
		cp.add(keyPanel);
	}

	private JMenu createFileMenu() {
		JMenu fileMenu = new JMenu("File");

		/* Save As...*/
		JMenuItem saveAsItem = new JMenuItem("Save As...");
		fileMenu.add(saveAsItem);
		saveAsItem.setAction(getSaveAsAction());

		/* Close */
		JMenuItem closeItem = new JMenuItem("Close");
		fileMenu.add(closeItem);
		closeItem.setAction(getCloseAction());

		return fileMenu;
	}

	/**
	 * Saves the key in DER format.
	 * 
	 * Use <code>openssl rsa -pubin -inform DER -in &lt;file&gt;</code>
	 * to print the resulting file.
	 */
	private Action getSaveAsAction() {
		final Preferences preferences = Preferences.userNodeForPackage(getClass());
		Action action = new AbstractAction() {

			private static final long serialVersionUID = -7264665364705062205L;

			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				String directory = preferences.get(JMRTDApp.CERT_AND_KEY_FILES_DIR_KEY, null);
				if (directory != null) {
					fileChooser.setCurrentDirectory(new File(directory));
				}
				fileChooser.setFileFilter(FileUtil.KEY_FILE_FILTER);
				int choice = fileChooser.showSaveDialog(getContentPane());
				switch (choice) {
				case JFileChooser.APPROVE_OPTION:
					try {
						File file = fileChooser.getSelectedFile();
						preferences.put(JMRTDApp.CERT_AND_KEY_FILES_DIR_KEY, file.getParent());
						FileOutputStream out = new FileOutputStream(file);
						/* FIXME: This is DER encoding? */
						out.write(keyPanel.getKey().getEncoded());
						out.flush();
						out.close();
					} catch (IOException fnfe) {
						fnfe.printStackTrace();
					}
					break;
				default: break;
				}
			}			
		};
		action.putValue(Action.SMALL_ICON, SAVE_AS_ICON);
		action.putValue(Action.LARGE_ICON_KEY, SAVE_AS_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Save key to file");
		action.putValue(Action.NAME, "Save As...");
		return action;
	}

	private Action getCloseAction() {
		Action action = actionMap.get("Close");
		if (action != null) { return action; }
		action = new AbstractAction() {

			private static final long serialVersionUID = 8216910949055330269L;

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
}
