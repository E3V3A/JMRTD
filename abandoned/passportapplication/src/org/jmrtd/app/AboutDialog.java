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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import org.jmrtd.app.util.FileUtil;
import org.jmrtd.app.util.IconUtil;

/**
 * Dialog with information about the product and project.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Id: $
 */
public class AboutDialog extends JDialog {

	private static final long serialVersionUID = 3298613357748205655L;

	private static final Logger LOGGER = Logger.getLogger("org.jmrtd");
	private static final Dimension PREFERRED_SIZE = new Dimension(530, 570);
	private static final String ABOUT_JMRTD_DEFAULT_TEXT = "JMRTD is brought to you by the JMRTD team!\nVisit http://jmrtd.org/ for more information.";
	private static final String ABOUT_JMRTD_LOGO = "jmrtd_logo-100x100";
	private static final Object[] OPTIONS = { "Close" };

	private static final Font ABOUT_TEXT_FONT = new Font(Font.DIALOG, Font.PLAIN, 12);

	private JOptionPane optionPane; 
	private JTextPane area;

	/**
	 * Constructs the dialog.
	 * 
	 * @param frame parent/owner of the dialog
	 */
	public AboutDialog(Frame frame) {
		super(frame, false); /* NOTE: not modal */
		area = new JTextPane();
		buildDialog("JMRTD - About");
	}
	
	@Override
	public Dimension getPreferredSize() {
		return PREFERRED_SIZE;
	}
	
	private void buildDialog(String title) {
		try {
			setTitle(title);
			ImageIcon aboutJMRTDImageIcon = null;

			URL readMeFile = null;
			try {
				readMeFile = new URL(FileUtil.getBaseDir(getClass()) + "/README");
				Image aboutJMRTDImage = IconUtil.getImage(ABOUT_JMRTD_LOGO, getClass());
				if (aboutJMRTDImage != null) { aboutJMRTDImageIcon = new ImageIcon(aboutJMRTDImage); }
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			if (readMeFile == null) {
				append(ABOUT_JMRTD_DEFAULT_TEXT);
				LOGGER.warning("Could not open README file");
			} else {
				BufferedReader in = new BufferedReader(new InputStreamReader(readMeFile.openStream()));
				while (true) {
					String line = in.readLine();
					if (line == null) { break; }
					append("  " + line);
					append("\n");
				}
				in.close();
			}
			area.setCaretPosition(0);
			area.setEditable(false);
			Color c = getBackground().brighter();
			area.setBackground(c);
			area.setFont(ABOUT_TEXT_FONT);

			optionPane = new JOptionPane(
					new JScrollPane(area),
					JOptionPane.INFORMATION_MESSAGE,
					JOptionPane.OK_OPTION,
					aboutJMRTDImageIcon,
					OPTIONS,
					OPTIONS[0]);

			/* Handle close button. */
			optionPane.addPropertyChangeListener(new PropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent e) {
					String prop = e.getPropertyName();
					if (isVisible()
							&& (e.getSource() == optionPane)
							&& (JOptionPane.VALUE_PROPERTY.equals(prop) ||
									JOptionPane.INPUT_VALUE_PROPERTY.equals(prop))) {
						Object value = optionPane.getValue();
						if (value == JOptionPane.UNINITIALIZED_VALUE) {
							/* NOTE: ignore reset. */
							return;
						}
						if (OPTIONS[0].equals(value)) {
							setVisible(false);
						} else {
							setVisible(false);
						}
						optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
					}
				}
			});

			/* Handle window closing correctly. */
			setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
			addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent we) {
					/*
					 * Instead of directly closing the window,
					 * we're going to change the JOptionPane's
					 * value property.
					 */
					optionPane.setValue(new Integer(JOptionPane.CLOSED_OPTION));
				}
			});

			setContentPane(optionPane);
		} catch (Exception ex) {
			LOGGER.severe("Could not build dialog: " + ex.getMessage());
//			ex.printStackTrace();
		}
	}

	private void append(String txt) {
		area.setText(area.getText() + txt);
	}
}
