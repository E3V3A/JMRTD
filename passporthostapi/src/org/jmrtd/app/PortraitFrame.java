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
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import sos.gui.ImagePanel;
import sos.mrtd.FaceInfo;
import sos.mrtd.ImageReadUpdateListener;
import sos.util.Files;
import sos.util.Icons;
import sos.util.Images;

/**
 * Frame for displaying and manipulating one portrait image.
 * Portrait is displayed at actual size.
 * 
 * Menu bar includes menu for saving image in alternative format,
 * displaying additional meta data, and an option to show feature
 * points.
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision: $
 */
public class PortraitFrame extends JFrame
{
	private static final long serialVersionUID = -3718372037784854010L;

	private static final Image JMRTD_ICON = Icons.getImage("jmrtd_logo-48x48");

	private static final Icon SAVE_AS_PNG_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("disk"));
	private static final Icon CLOSE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("bin"));
	private static final Icon IMAGE_INFO_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("information"));
	private static final Icon FEATURE_POINTS_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("chart_line"));

	private FaceInfo info;
	private ImagePanel imagePanel;

	public PortraitFrame(FaceInfo info) {
		this("Portrait", info);
	}

	public PortraitFrame(String title, FaceInfo info) {
		super(title);
		this.info = info;
		setIconImage(JMRTD_ICON);
		
		info.addImageReadUpdateListener(new ImageReadUpdateListener() {
			public void passComplete(BufferedImage image) {
				imagePanel.setImage(image);
				imagePanel.revalidate(); repaint();
			}
		});

		/* Menu bar */
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(createFileMenu());
		menuBar.add(createViewMenu());
		setJMenuBar(menuBar);

		/* Frame content */
		Image image = info.getImage();
		imagePanel = new ImagePanel();
		imagePanel.setImage(image);
		Container cp = getContentPane();
		cp.add(imagePanel);
		imagePanel.revalidate(); repaint();
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

	private JMenu createViewMenu() {
		JMenu viewMenu = new JMenu("View");

		/* Image Info */
		JMenuItem viewImageInfo = new JMenuItem();
		viewMenu.add(viewImageInfo);
		viewImageInfo.setAction(getViewImageInfoAction());

		/* Feature Points */
		JCheckBoxMenuItem viewFeaturePoints = new JCheckBoxMenuItem();
		viewMenu.add(viewFeaturePoints);
		viewFeaturePoints.setAction(getViewFeaturePointsAction());

		return viewMenu;
	}

	private Action getSaveAsAction() {
		Action action = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setFileFilter(Files.IMAGE_FILE_FILTER);
				int choice = fileChooser.showSaveDialog(getContentPane());
				switch (choice) {
				case JFileChooser.APPROVE_OPTION:
					try {
						File file = fileChooser.getSelectedFile();
						String fileName = file.getName().toLowerCase();
						if (fileName.endsWith(".png")) {
							ImageIO.write(Images.toBufferedImage(imagePanel.getImage()), "png", file);
						} else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") ) {
							ImageIO.write(Images.toBufferedImage(imagePanel.getImage()), "jpg", file);
						} else if (fileName.endsWith(".bmp")) {
							ImageIO.write(Images.toBufferedImage(imagePanel.getImage()), "bmp", file);
						} else if (fileName.endsWith(".gif")) {
							ImageIO.write(Images.toBufferedImage(imagePanel.getImage()), "gif", file);
						}
					} catch (IOException fnfe) {
						fnfe.printStackTrace();
					}
					break;
				default: break;
				}
			}
		};
		action.putValue(Action.SMALL_ICON, SAVE_AS_PNG_ICON);
		action.putValue(Action.LARGE_ICON_KEY, SAVE_AS_PNG_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Save image as bitmap");
		action.putValue(Action.NAME, "Save As...");
		return action;
	}

	private Action getViewImageInfoAction() {
		Action action = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				JTextArea area = new JTextArea();
				area.append(info.toString());
				JOptionPane.showMessageDialog(getContentPane(), new JScrollPane(area), "Image information", JOptionPane.PLAIN_MESSAGE, null);
			}
		};
		action.putValue(Action.SMALL_ICON, IMAGE_INFO_ICON);
		action.putValue(Action.LARGE_ICON_KEY, IMAGE_INFO_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "View Image Information");
		action.putValue(Action.NAME, "Image Info...");
		return action;
	}

	private Action getViewFeaturePointsAction() {
		Action action = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				Object src = e.getSource();
				if (src instanceof AbstractButton) {
					AbstractButton button = (AbstractButton)src;
					FaceInfo.FeaturePoint[] featurePoints = info.getFeaturePoints();
					if (button.isSelected()) {
						for (FaceInfo.FeaturePoint featurePoint: featurePoints) {
							String key = featurePoint.getMajorCode() + "." + featurePoint.getMinorCode();
							imagePanel.highlightPoint(key, featurePoint.getX(), featurePoint.getY());
						}
					} else {
						for (FaceInfo.FeaturePoint featurePoint: featurePoints) {
							String key = featurePoint.getMajorCode() + "." + featurePoint.getMinorCode();
							imagePanel.deHighlightPoint(key);
						}
					}
				}
			}			
		};
		action.putValue(Action.SMALL_ICON, FEATURE_POINTS_ICON);
		action.putValue(Action.LARGE_ICON_KEY, FEATURE_POINTS_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "View Feature Points");
		action.putValue(Action.NAME, "Feature Points");
		return action;
	}

	private Action getCloseAction() {
		Action action = new AbstractAction() {
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
