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
	private static void showFile(String fileName) throws IOException {
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
	}

	private static void transcodeFile(String sourceFileName, String targetFileName) throws IOException {
		String destFormat = targetFileName.substring(targetFileName.indexOf('.') + 1, targetFileName.length());
		File file = new File(sourceFileName);
		BufferedImage image = ImageIO.read(file);
		File out = new File(targetFileName);
		ImageIO.write(image, destFormat, out);
	}

	public static void main(String[] arg) {
		try {
			switch (arg.length) {
			case 1:
				showFile(arg[0]);
				break;
			case 2:
				transcodeFile(arg[0], arg[1]);
				break;
			default:
				System.err.println("Usage:  java " + WSQTest.class.getCanonicalName() + " <file>.wsq");
				System.err.println("        java " + WSQTest.class.getCanonicalName() + " <sourcefile>.extension <targetfile>.extension");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
