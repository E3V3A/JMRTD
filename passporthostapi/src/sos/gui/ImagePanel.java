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
 * $Id$
 */

package sos.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Panel for showing images.
 *
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Engelbert Hubbers (hubbers@cs.ru.nl)
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision$
 */
public class ImagePanel extends JPanel
{  
   private BufferedImage image;;

   Map highlights;

   /**
    * Constructs a new image panel with empty image.
    */
   public ImagePanel() {
      super(null);
      highlights = new HashMap();
      image = null;
   }

   /**
    * Sets the image to <code>image</code>.
    *
    * @param image the image.
    */
   public void setImage(BufferedImage image) {
      setVisible(false);
      this.image = image;
      setVisible(true);
   }

   /**
    * Clears the currently displayed image.
    */
   public void clearImage() {
      setVisible(false);
      setVisible(true);
   }

   private void clearImage(BufferedImage image) {
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
         return new Dimension(image.getWidth(), image.getHeight());
      }
   }
   
   public void paint(Graphics g) {
      super.paint(g);
      if (image == null) {
         return;
      }
      g.drawImage(image, 0, 0, this);
      Iterator it = highlights.keySet().iterator();
      while (it.hasNext()) {
         String key = (String)it.next();
         Point p = (Point)highlights.get(key);
         int x = (int)p.getX();
         int y = (int)p.getY();
         // Graphics g = image.getGraphics();
         g.setColor(Color.red);
         g.drawLine(x, y - 25, x, y + 25);
         g.drawLine(x - 25, y, x + 25, y);
         g.setColor(Color.black);
         g.drawString(key, x + 4, y + 14);
         g.drawString(key, x + 6, y + 14);
         g.drawString(key, x + 5, y + 13);
         g.drawString(key, x + 5, y + 15);
         g.setColor(Color.white);
         g.drawString(key, x + 5, y + 14);
      }
   }
}

