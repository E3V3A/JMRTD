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
 * $Id: PortraitFrame.java 894 2009-03-23 15:50:46Z martijno $
 */

package org.jmrtd.app;

import java.awt.Container;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import org.jmrtd.app.swing.ImagePanel;
import org.jmrtd.app.util.IconUtil;
import org.jmrtd.app.util.ImageUtil;
import org.jmrtd.lds.FingerImageInfo;
import org.jmrtd.lds.FingerInfo;

/**
 * Frame for displaying and manipulating one fingerprint image.
 * Portrait is displayed at actual size.
 * 
 * Menu bar includes menu for saving image in alternative format,
 * displaying additional meta data, and an option to show feature
 * points.
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision: 894 $
 */
public class FingerPrintFrame extends JMRTDFrame {

	private static final long serialVersionUID = -3718372037784454010L;

	private static final Icon IMAGE_INFO_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("information"));

	private List<FingerInfo> fingerPrints;
	// private ImagePanel imagePanel;

	public FingerPrintFrame(List<FingerInfo> fingerPrints) {
		this("FingerPrints", fingerPrints);
	}

	public FingerPrintFrame(String title, List<FingerInfo> fingerInfos) {
		super(title);
		this.fingerPrints = new ArrayList<FingerInfo>(fingerInfos);

		/* Menu bar */
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(createFileMenu());
		menuBar.add(createViewMenu());
		setJMenuBar(menuBar);

		/* Frame content */
		try {
			JTabbedPane tabbedPane = new JTabbedPane();
			for (FingerInfo fingerInfo: fingerInfos) {
				List<FingerImageInfo> fingerImageInfos = fingerInfo.getFingerImageInfos();
				for (FingerImageInfo fingerImageInfo: fingerImageInfos) {
					Image image = ImageUtil.read(fingerImageInfo.getImageInputStream(), fingerImageInfo.getImageLength(), fingerImageInfo.getMimeType());
					ImagePanel imagePanel = new ImagePanel();
					imagePanel.setImage(image);
					tabbedPane.addTab("f", imagePanel);
				}
			}
			Container cp = getContentPane();
			cp.add(tabbedPane);
			tabbedPane.revalidate(); repaint();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private JMenu createFileMenu() {
		JMenu fileMenu = new JMenu("File");

		/* Close */
		JMenuItem closeItem = new JMenuItem("Close");
		fileMenu.add(closeItem);
		closeItem.setAction(getCloseAction());

		return fileMenu;
	}

	private JMenu createViewMenu() {
		JMenu viewMenu = new JMenu("View");

		/* Image Info */
		JMenuItem viewImageInfo = new JMenuItem();
		viewMenu.add(viewImageInfo);
		viewImageInfo.setAction(getViewImageInfoAction());

		return viewMenu;
	}

	private Action getViewImageInfoAction() {
		Action action = new AbstractAction() {

			private static final long serialVersionUID = 5741460721594325551L;

			public void actionPerformed(ActionEvent e) {
				JTextArea area = new JTextArea();
				area.append(fingerPrints.toString());
				JOptionPane.showMessageDialog(getContentPane(), new JScrollPane(area), "Image information", JOptionPane.PLAIN_MESSAGE, null);
			}
		};
		action.putValue(Action.SMALL_ICON, IMAGE_INFO_ICON);
		action.putValue(Action.LARGE_ICON_KEY, IMAGE_INFO_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "View Image Information");
		action.putValue(Action.NAME, "Image Info...");
		return action;
	}

	private Action getCloseAction() {
		Action action = new AbstractAction() {

			private static final long serialVersionUID = 893441969514204179L;

			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		};
		action.putValue(Action.SMALL_ICON, CLOSE_ICON);
		action.putValue(Action.LARGE_ICON_KEY, CLOSE_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Close Window");
		action.putValue(Action.NAME, "Close");
		return action;
	}
}
