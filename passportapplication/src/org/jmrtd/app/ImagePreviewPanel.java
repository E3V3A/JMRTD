/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2010  The JMRTD team
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
 * $Id: FacePreviewPanel.java 894 2009-03-23 15:50:46Z martijno $
 */

package org.jmrtd.app;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.jmrtd.app.util.IconUtil;
import org.jmrtd.app.util.ImageUtil;
import org.jmrtd.lds.DisplayedImageInfo;
import org.jmrtd.lds.ImageInfo;

/**
 * Component for displaying a preview of LDS content (typically a portrait).
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 *
 * @version $Revision: 894 $
 */
public class ImagePreviewPanel extends JPanel {

	private static final long serialVersionUID = 9113961215076977525L;

	private static final Logger LOGGER = Logger.getLogger("org.jmrtd");

	private static final Icon IMAGE_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("picture"));
	private static final Icon FACE_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("user"));
	private static final Icon FINGER_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("thumb_up"));
	private static final Icon IRIS_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("eye"));
	private static final Icon MAGNIFIER_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("magnifier"));
	private static final Icon WRITTEN_SIGNATURE_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("text_signature"));

	private int width, height;
	private JTabbedPane tabbedPane;
	private List<ImageInfo> infos;

	/**
	 * Constructs an instance of this component.
	 * 
	 * @param width width of this component
	 * @param height height of this component
	 */
	public ImagePreviewPanel(int width, int height) {
		super(new BorderLayout());
		this.width = width;
		this.height = height;
		infos = new ArrayList<ImageInfo>();
		tabbedPane = new JTabbedPane(JTabbedPane.LEFT);
		add(tabbedPane, BorderLayout.CENTER);
	}

	/**
	 * @return
	 */
	public int getSelectedIndex() {
		return tabbedPane.getSelectedIndex();
	}

	public ImageInfo getSelectedDisplayedImage() {
		return infos.get(tabbedPane.getSelectedIndex());
	}

	public void addDisplayedImage(ImageInfo info) {
		try {
			Icon icon = null;
			switch (info.getType()) {
			case DisplayedImageInfo.TYPE_PORTRAIT: icon = FACE_ICON; break;
			case DisplayedImageInfo.TYPE_SIGNATURE_OR_MARK: icon = WRITTEN_SIGNATURE_ICON; break;
			case DisplayedImageInfo.TYPE_FINGER: icon = FINGER_ICON; break;
			case DisplayedImageInfo.TYPE_IRIS: icon = IRIS_ICON; break;
			default: icon = IMAGE_ICON; break;
			}
			final int index = tabbedPane.getTabCount();
			BufferedImage image = (BufferedImage)createImage(width - 10, height - 10);
			final JLabel label = new JLabel(new ImageIcon(image));
			final JPanel panel = new JPanel(new FlowLayout());
			panel.add(label);
			infos.add(info);
			tabbedPane.addTab(Integer.toString(index + 1), icon, panel);
			revalidate(); repaint();
			//			if (info instanceof FaceImageInfo) {
			//				((FaceImageInfo)info).addImageReadUpdateListener(new ImageReadUpdateListener() {
			//					public void passComplete(BufferedImage image, double percentage) {
			//						if (image == null) { return; }
			//						BufferedImage scaledImage = scaleImage(image, calculateScale(width - 10, height - 10, image.getWidth(), image.getHeight()));
			//						label.setIcon(new ImageIcon(scaledImage));
			//						revalidate(); repaint();
			//					}
			//				});
			//			}

			synchronized(info) {
				boolean isImageDecodable = false;
				try {
					int imageLength = info.getImageLength();
					InputStream imageInputStream = info.getImageInputStream();
					String imageMimeType = info.getMimeType();					
					image = ImageUtil.read(imageInputStream, imageLength, imageMimeType);
					image = scaleImage(image, calculateScale(width - 10, height - 10, image.getWidth(), image.getHeight()));
					isImageDecodable = true;
				} catch (UnsatisfiedLinkError e) {
					/* FIXME: Our image decoders should be better behaved... */
					LOGGER.warning("Got UnsatisfiedLinkError while decoding \"" + info.getMimeType() + "\" image");
					isImageDecodable = false;
				} catch (Exception e) {
					e.printStackTrace();
					isImageDecodable = false;				
				}

				if (isImageDecodable) {
					label.setIcon(new ImageIcon(image));
				} else {
					label.setIcon(null);
				}
				revalidate(); repaint();
			}
		} catch (Exception e) {
			/* We'll just skip this image then. */
		}	
	}

	public void removeDisplayedImage(int index) {
		tabbedPane.removeTabAt(index);
		infos.remove(index);
		revalidate(); repaint();
	}

	public void addMouseListener(MouseListener l) {
		super.addMouseListener(l);
		tabbedPane.addMouseListener(l);
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
		AffineTransform affineTransform = AffineTransform.getScaleInstance(scale, scale);
		g2.drawImage(image, affineTransform, null); 
		return scaledImage;
	}
}
