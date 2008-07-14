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
 * $Id: FacePanel.java 206 2007-03-26 20:19:44Z martijno $
 */

package sos.mrtd.sample.newgui;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.io.InputStream;
import java.util.Collection;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import sos.mrtd.DG2File;
import sos.mrtd.FaceInfo;
import sos.util.Icons;

/**
 * GUI component for displaying the portrait.
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 *
 * @version $Revision: $
 */
public class FacePreviewPanel extends JPanel
{	
	private static final Icon IMAGE_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("picture"));
	
	private Dimension preferredSize;
	private JTabbedPane tabbedPane;

	public FacePreviewPanel(InputStream in) {
		
	}
	
	public FacePreviewPanel(InputStream in, int width, int height) {
		super(new FlowLayout());
		preferredSize = new Dimension(width, height);
		add(createFaceComponent(in, width, height));
	}
	
	public int getSelectedIndex() {
		return tabbedPane.getSelectedIndex();
	}

	/**
	 * The face image component.
	 * 
	 * @param in inputstream containing the image
	 * @param width the width of the resulting component
	 * @param height the height of the resulting component
	 * @return a component displaying the image
	 */	
	private JComponent createFaceComponent(InputStream in, int width, int height) {
		Collection<FaceInfo> faces = (new DG2File(in)).getFaces();
		return createFaceComponent(faces, width, height);
	}
	
	private JComponent createFaceComponent(Collection<FaceInfo> faces, int width, int height) {
		tabbedPane = new JTabbedPane(JTabbedPane.LEFT);
		int dw = width - 10, dh = height - 10;
		int index = 0;
		for (FaceInfo face: faces) {
			index++;
			JPanel panel = new JPanel(new FlowLayout());
			Image origImage = face.getImage();
			Image image = (origImage.getHeight(this) / dh < origImage.getWidth(this) / dw) ? origImage.getScaledInstance(dw, -1, Image.SCALE_SMOOTH) : origImage.getScaledInstance(-1, dh, Image.SCALE_SMOOTH);

			panel.add(new JLabel(new ImageIcon(image)));
			tabbedPane.addTab(Integer.toString(index), IMAGE_SMALL_ICON, add(panel));
		}
		return tabbedPane;		
	}
}
