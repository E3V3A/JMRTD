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

package org.jmrtd.app.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JPanel;

/**
 * Panel for showing images.
 *
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Engelbert Hubbers (hubbers@cs.ru.nl)
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision: 183 $
 */
public class ImagePanel extends JPanel
{
	private static final long serialVersionUID = 1245196754602799575L;

	private static final int MAX_RADIUS = 40;
	private Image image;;

	Map<String, Point> highlights;
	Color highlightColor;
	int radius;

	/**
	 * Constructs a new image panel with empty image.
	 */
	public ImagePanel() {
		super(null);
		highlightColor = new Color(1, (float)0.1, (float)0.1, (float)0.7);
		radius = MAX_RADIUS;
		highlights = new HashMap<String, Point>();
		image = null;

		/*
      (new Thread(new Runnable() {
         public void run() {
            try {
               while(true) {
                  radius += 1;
                  radius %= MAX_RADIUS;
                  Thread.sleep(300);
                  repaint();
               }
            } catch (InterruptedException ie) {
            }
         }
      })).start();
		 */
	}

	/**
	 * Sets the image to <code>image</code>.
	 *
	 * @param image the image.
	 */
	public void setImage(Image image) {
		setVisible(false);
		this.image = image; // FIXME: what if image param is not a BufferedImage?
		setVisible(true);
	}

	public Image getImage() {
		return image;
	}

	/**
	 * Clears the currently displayed image.
	 */
	public void clearImage() {
		setVisible(false);
		setVisible(true);
	}
	
	public boolean isSomePointHighlighted() {
		return highlights != null && !highlights.isEmpty();
	}

	public void highlightPoint(String key, int x, int y) {
		setVisible(false);
		highlights.put(key, new Point(x, y));
		setVisible(true);
	}

	public void deHighlightPoint(String key) {
		setVisible(false);
		highlights.remove(key);
		setVisible(true);
	}

	public Dimension getPreferredSize() {
		if (image == null) {
			return super.getPreferredSize();
		} else {
			return new Dimension(image.getWidth(this), image.getHeight(this));
		}
	}

	public void paint(Graphics g) {
		super.paint(g);
		Graphics2D g2 = (Graphics2D)g;
		if (image == null) {
			return;
		}
		g2.drawImage(image, 0, 0, this);
		Iterator<String> it = highlights.keySet().iterator();
		while (it != null && it.hasNext()) {
			String key = (String)it.next();
			Point p = (Point)highlights.get(key);
			int x = (int)p.getX();
			int y = (int)p.getY();
			// Graphics g = image.getGraphics();
			g2.setColor(highlightColor);

			/* kruisje
         g2.drawLine(x, y - 5, x, y + 5);
         g2.drawLine(x - 5, y, x + 5, y);
			 */

			/* lijnen */
			g2.drawLine(x, y, image.getWidth(this), y);
			g2.drawLine(x, y, x, image.getHeight(this));

			/* bewegende cirkels
         int r0 = radius;
         int r2 = (radius + 2 * (MAX_RADIUS / 4)) % MAX_RADIUS;
         g2.drawOval(x - r0/2, y - r0/2, r0, r0);
         g2.drawOval(x - r2/2, y - r2/2, r2, r2);    
			 */

			/* Draw key label... */
			g2.setColor(Color.black);
			g2.drawString(key, x + 4, y + 14);
			g2.drawString(key, x + 6, y + 14);
			g2.drawString(key, x + 5, y + 13);
			g2.drawString(key, x + 5, y + 15);
			g2.setColor(Color.white);
			g2.drawString(key, x + 5, y + 14);
		}
	}
}
