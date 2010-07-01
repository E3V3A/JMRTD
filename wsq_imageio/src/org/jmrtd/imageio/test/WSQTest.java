package org.jmrtd.imageio.test;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class WSQTest
{
	public WSQTest(String fileName) {
		try {
			File file = new File(fileName);
			BufferedImage image = ImageIO.read(file);
			JFrame frame = new JFrame("Image " + image.getWidth() + " x " + image.getHeight());
			Container contentPane = frame.getContentPane();
			JPanel imgPanel = new JPanel(new BorderLayout());
			imgPanel.add(new JLabel(new ImageIcon(image)), BorderLayout.CENTER);
			contentPane.add(imgPanel);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.pack();
			frame.setVisible(true);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public static void main(String[] arg) {
		new WSQTest(arg[0]);
	}
}
