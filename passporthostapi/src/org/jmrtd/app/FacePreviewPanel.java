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

import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collection;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import sos.mrtd.FaceInfo;
import sos.mrtd.ImageReadUpdateListener;
import sos.util.Icons;

/**
 * Component for displaying a preview of the portrait(s).
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 *
 * @version $Revision: $
 */
public class FacePreviewPanel extends JPanel
{	
	private static final long serialVersionUID = 9113961215076977525L;

	private static final Icon IMAGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("picture"));

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
			final int index = tabbedPane.getTabCount();
			BufferedImage image = (BufferedImage)createImage(width - 10, height - 10);
			final JLabel label = new JLabel(new ImageIcon(image));
			final JPanel panel = new JPanel(new FlowLayout());
			panel.add(label);
			tabbedPane.addTab(Integer.toString(index), IMAGE_ICON, panel);
			revalidate(); repaint();
			faceInfo.addImageReadUpdateListener(new ImageReadUpdateListener() {
				public void passComplete(BufferedImage image) {
					if (image == null) { return; }
					BufferedImage scaledImage = scaleImage(image, calculateScale(width - 10, height - 10, image.getWidth(), image.getHeight()));
					label.setIcon(new ImageIcon(scaledImage));
					revalidate(); repaint();
				}
			});
			image = faceInfo.getImage();
			image = scaleImage(image, calculateScale(width - 10, height - 10, image.getWidth(), image.getHeight()));
			label.setIcon(new ImageIcon(image));
			revalidate(); repaint();
		} catch (IOException ioe) {
			/* We'll just skip this image then. */
			System.out.println("DEBUG: ignoring " + ioe);
		}	
	}

	public void removeFace(int index) {
		tabbedPane.removeTabAt(index);
		revalidate(); repaint();
	}

	public void addFaces(Collection<FaceInfo> faces) {
		for (FaceInfo faceInfo: faces) {
			addFace(faceInfo);
		}
	}

	/**
	 * A scaling factor resulting in at most desiredWidth and desiredHeight yet
	 * that respects aspect ratio of original width and height.
	 */
	private double calculateScale(int desiredWidth, int desiredHeight, int actualWidth, int actualHeight) {
		double xScale = (double)desiredWidth / (double)actualWidth;
		double yScale = (double)desiredHeight / (double)actualHeight;
		double scale = xScale < yScale ? xScale : yScale;
		return scale;
	}

	/**
	 * Scales image.
	 * 
	 * @param image an image
	 * @param scale scaling factor
	 * @return scaled image
	 */
	private BufferedImage scaleImage(BufferedImage image, double scale) {
		BufferedImage scaledImage = new BufferedImage((int)((double)image.getWidth() * scale), (int)((double)image.getHeight() * scale), BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = scaledImage.createGraphics();
		AffineTransform at = AffineTransform.getScaleInstance(scale, scale);
		g2.drawImage(image, at, null); 
		return scaledImage;
	}
}
