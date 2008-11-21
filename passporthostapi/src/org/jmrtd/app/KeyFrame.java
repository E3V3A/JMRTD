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
 * $Id: $
 */

package org.jmrtd.app;

import java.awt.Container;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Key;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import sos.gui.KeyPanel;
import sos.util.Files;
import sos.util.Icons;

/**
 * Frame for displaying (and saving to file) keys.
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 */
public class KeyFrame extends JFrame
{
	private static final Image JMRTD_ICON = Icons.getImage("jmrtd_logo-48x48");

	private static final Icon SAVE_AS_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("disk"));
	private static final Icon SAVE_AS_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("disk"));
	private static final Icon CLOSE_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("bin"));
	private static final Icon CLOSE_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("bin"));

	private KeyPanel keyPanel;

	public KeyFrame(Key key) {
		this("Key", key);
	}

	public KeyFrame(String title, Key key) {
		super(title);
		setIconImage(JMRTD_ICON);

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
		Action action = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setFileFilter(Files.KEY_FILE_FILTER);
				int choice = fileChooser.showSaveDialog(getContentPane());
				switch (choice) {
				case JFileChooser.APPROVE_OPTION:
					try {
						File file = fileChooser.getSelectedFile();
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
		action.putValue(Action.SMALL_ICON, SAVE_AS_SMALL_ICON);
		action.putValue(Action.LARGE_ICON_KEY, SAVE_AS_LARGE_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Save key to file");
		action.putValue(Action.NAME, "Save As...");
		return action;
	}

	private Action getCloseAction() {
		Action action = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}	
		};
		action.putValue(Action.SMALL_ICON, CLOSE_SMALL_ICON);
		action.putValue(Action.LARGE_ICON_KEY, CLOSE_LARGE_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Close Window");
		action.putValue(Action.NAME, "Close");
		return action;
	}
}
