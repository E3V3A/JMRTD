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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;

import org.jmrtd.app.swing.ImagePanel;
import org.jmrtd.app.util.FileUtil;
import org.jmrtd.app.util.IconUtil;
import org.jmrtd.app.util.ImageUtil;
import org.jmrtd.lds.FaceImageInfo;

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
 * @version $Revision: 894 $
 */
public class PortraitFrame extends JMRTDFrame {

	private static final long serialVersionUID = -3718372037784854010L;

	private static final Icon SAVE_AS_PNG_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("disk"));
	private static final Icon CLOSE_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("bin"));
	private static final Icon IMAGE_INFO_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("page_white_text"));
	private static final Icon FEATURE_POINTS_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("chart_line"));

	private ActionMap actionMap;
	
	private FaceImageInfo info;
	private ImagePanel imagePanel;
	private JCheckBoxMenuItem viewFeaturePointsItem;

	public PortraitFrame(FaceImageInfo faceImageInfo) {
		this("Portrait", faceImageInfo);
	}

	public PortraitFrame(String title, FaceImageInfo info) {
		super(title);
		if (info == null) { throw new IllegalArgumentException("Face image cannot be null"); }
		this.info = info;
	
		actionMap = new ActionMap();
		
//		info.addImageReadUpdateListener(new ImageReadUpdateListener() {
//			public void passComplete(BufferedImage image, double percentage) {
//				imagePanel.setImage(image);
//				imagePanel.revalidate(); repaint();
//			}
//		});

		/* Menu bar */
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(createFileMenu());
		menuBar.add(createViewMenu());
		setJMenuBar(menuBar);

		JToolBar toolBar = new JToolBar();
		toolBar.add(getSaveAsAction());
//		toolBar.add(getCloseAction());
		toolBar.addSeparator();
		toolBar.add(getViewImageInfoAction());
//		toolBar.add(getViewFeaturePointsAction());
		
		/* Frame content */
		try {
			Image image = ImageUtil.read(info.getImageInputStream(), info.getImageLength(), info.getMimeType());
			imagePanel = new ImagePanel();
			imagePanel.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					super.mouseClicked(e);
					if (e.getClickCount() > 1) { toggleViewFeaturePoints(!viewFeaturePointsItem.isSelected()); }
				}
			});
			imagePanel.setImage(image);
			Container cp = getContentPane();
			cp.setLayout(new BorderLayout());
			cp.add(toolBar, BorderLayout.NORTH);
			cp.add(new JScrollPane(imagePanel), BorderLayout.CENTER);
			imagePanel.revalidate(); repaint();
		} catch (Exception e) {
			e.printStackTrace();
		}
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
		viewFeaturePointsItem = new JCheckBoxMenuItem();
		viewMenu.add(viewFeaturePointsItem);
		viewFeaturePointsItem.setAction(getViewFeaturePointsAction());

		return viewMenu;
	}

	private Action getSaveAsAction() {
		Action action = actionMap.get("SaveAs");
		if (action != null) { return action; }
		final Preferences preferences = Preferences.userNodeForPackage(getClass());
		action = new AbstractAction() {

			private static final long serialVersionUID = -4810689890241792533L;

			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				String directory = preferences.get(JMRTDApp.IMAGE_FILES_DIR_KEY, null);
				if (directory != null) {
					fileChooser.setCurrentDirectory(new File(directory));
				}
				fileChooser.setFileFilter(FileUtil.IMAGE_FILE_FILTER);
				int choice = fileChooser.showSaveDialog(getContentPane());
				switch (choice) {
				case JFileChooser.APPROVE_OPTION:
					try {
						File file = fileChooser.getSelectedFile();
						preferences.put(JMRTDApp.IMAGE_FILES_DIR_KEY, file.getParent());
						String fileName = file.getName().toLowerCase();
						Image image = imagePanel.getImage();
						FileOutputStream fileOut = new FileOutputStream(file);
						if (fileName.endsWith(".png")) {
							ImageUtil.write(image, "image/png", fileOut);
						} else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") ) {
							ImageUtil.write(image, "image/jpeg", fileOut);
						} else if (fileName.endsWith(".bmp")) {
							ImageUtil.write(image, "image/bmp", fileOut);
						} else if (fileName.endsWith(".gif")) {
							ImageUtil.write(image, "image/gif", fileOut);
						}
						fileOut.flush();
						fileOut.close();
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
		actionMap.put("SaveAs", action);
		return action;
	}

	private Action getViewImageInfoAction() {
		Action action = new AbstractAction() {

			private static final long serialVersionUID = 5741460721594325551L;

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
		Action action = actionMap.get("ViewFeaturePoints");
		if (action != null) { return action; }
		action = new AbstractAction() {

			private static final long serialVersionUID = -5878482281301204061L;

			public void actionPerformed(ActionEvent e) {
				Object src = e.getSource();
				if (src instanceof AbstractButton) {
					AbstractButton button = (AbstractButton)src;
					toggleViewFeaturePoints(button.isSelected());
				}
			}			
		};
		action.putValue(Action.SMALL_ICON, FEATURE_POINTS_ICON);
		action.putValue(Action.LARGE_ICON_KEY, FEATURE_POINTS_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "View Feature Points");
		action.putValue(Action.NAME, "Feature Points");
		actionMap.put("ViewFeaturePoints", action);
		return action;
	}
	
	private void toggleViewFeaturePoints(boolean showPoints) {
		FaceImageInfo.FeaturePoint[] featurePoints = info.getFeaturePoints();
		viewFeaturePointsItem.setSelected(showPoints);
		if (showPoints) {
			for (FaceImageInfo.FeaturePoint featurePoint: featurePoints) {
				String key = featurePoint.getMajorCode() + "." + featurePoint.getMinorCode();
				imagePanel.highlightPoint(key, featurePoint.getX(), featurePoint.getY());
			}
		} else {
			for (FaceImageInfo.FeaturePoint featurePoint: featurePoints) {
				String key = featurePoint.getMajorCode() + "." + featurePoint.getMinorCode();
				imagePanel.deHighlightPoint(key);
			}
		}
	}

	private Action getCloseAction() {
		Action action = actionMap.get("Close");
		if (action != null) { return action; }
		action = new AbstractAction() {

			private static final long serialVersionUID = 893441969514204179L;

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
