  package sos.mrtd.sample.apdutest;
/*
    * Programming graphical user interfaces
    * Example: DrawImage.java
    * Jarkko Leponiemi 2003
    */
   
   import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;

import javax.swing.JPanel;
  
  public class DrawImage extends JPanel {
     
     private Image img = null;
     
     public DrawImage(String file) {
        // toolkit is an interface to the environment
        Toolkit toolkit = getToolkit();
        // create the image using the toolkit
        img = toolkit.createImage(file);
     }
     
     public DrawImage(){
    	 
     }
     
     public void paint(Graphics g) {
//        super.paint(g);
        this.setSize(45, 45);
        // the size of the component
        Dimension d = getSize();
        // the internal margins of the component
        Insets i = getInsets();
        // draw to fill the entire component
        g.drawImage(img, i.left, i.top, d.width, d.height, this);
     }
     
     public void update(String file){
    	 Toolkit toolkit = getToolkit();
    	 img = toolkit.createImage(file);
    	 repaint();
     }
  
  }
