/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006  SoS group, Radboud University
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
 * $Id: ImagePanel.java 12 2006-07-05 10:11:46Z martijno $
 */

package sos.gui;

import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
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
   /** Holds the image. */
   private ImageIcon icon;

   /**
    * Constructs a new image panel with empty image.
    */
   public ImagePanel() {
      super(new FlowLayout());
      BufferedImage image =
         new BufferedImage(480, 640, BufferedImage.TYPE_INT_ARGB);
      icon = new ImageIcon(image);
      add(new JLabel(icon));
      clearImage(image);
   }

   /**
    * Sets the image to <code>image</code>.
    *
    * @param image the image.
    */
   public void setImage(BufferedImage image) {
      setVisible(false);
      icon.setImage(image);
      setVisible(true);
   }

   /**
    * Clears the currently displayed image.
    */
   public void clearImage() {
      clearImage((BufferedImage)icon.getImage());
   }

   private void clearImage(BufferedImage image) {
      setVisible(false);
      int w = image.getWidth();
      int h = image.getHeight();
      Graphics g = image.getGraphics();
      g.setColor(getBackground());
      g.fillRect(0, 0, w, h);
      setVisible(true);
   }
}

