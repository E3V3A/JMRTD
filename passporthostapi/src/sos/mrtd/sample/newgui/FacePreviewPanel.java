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
import java.io.InputStream;
import java.util.Collection;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import sos.mrtd.DG2File;
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

	private JTabbedPane tabbedPane;

	public FacePreviewPanel(InputStream in, int width, int height) {
		super(new FlowLayout());
		tabbedPane = new JTabbedPane(JTabbedPane.LEFT);
		add(tabbedPane);
		Collection<FaceInfo> faces = (new DG2File(in)).getFaces();
		addFaces(faces, width, height);
	}

	public int getSelectedIndex() {
		return tabbedPane.getSelectedIndex();
	}

	private void addFaces(Collection<FaceInfo> faces, int width, int height) {
		int index = 0;
		for (FaceInfo faceInfo: faces) {
			index++;
			JPanel panel = new JPanel(new FlowLayout());
			Image image = faceInfo.getPreviewImage(width - 10, height - 10);
			panel.add(new JLabel(new ImageIcon(image)));
			tabbedPane.addTab(Integer.toString(index), IMAGE_SMALL_ICON, add(panel));
		}	
	}
}
