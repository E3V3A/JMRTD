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

import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collection;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import sos.mrtd.FaceInfo;
import sos.util.Icons;

/**
 * Component for displaying a preview of the portrait.
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 *
 * @version $Revision: $
 */
public class FacePreviewPanel extends JPanel
{	
	private static final Icon IMAGE_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("picture"));

	private int width, height;
	private JTabbedPane tabbedPane;

	public FacePreviewPanel(int width, int height) {
		super(new FlowLayout());
		this.width = width;
		this.height = height;
		tabbedPane = new JTabbedPane(JTabbedPane.LEFT);
		add(tabbedPane);
	}

	public int getSelectedIndex() {
		return tabbedPane.getSelectedIndex();
	}

	public void addFace(FaceInfo faceInfo) {
		try {
			Image image = faceInfo.getPreviewImage(width - 10, height - 10);
			addImage(image);
			revalidate(); repaint();
		} catch (IOException ioe) {
			/* We'll just skip this image then. */
			ioe.printStackTrace();
		}	
	}
	
	public void addFaces(Collection<FaceInfo> faces) {
		for (FaceInfo faceInfo: faces) {
			addFace(faceInfo);
		}
	}

	private void addImage(Image image) {
		JPanel panel = new JPanel(new FlowLayout());
		panel.add(new JLabel(new ImageIcon(image)));
		int index = tabbedPane.getTabCount();
		tabbedPane.addTab(Integer.toString(index), IMAGE_SMALL_ICON, panel);
	}

	private void addDummyFace(int width, int height) {
		BufferedImage dummyImage = new BufferedImage(width - 10, height - 10, BufferedImage.TYPE_INT_ARGB);
		addImage(dummyImage);
	}

	private void removeImage(int index) {
		tabbedPane.removeTabAt(index);
	}
}

