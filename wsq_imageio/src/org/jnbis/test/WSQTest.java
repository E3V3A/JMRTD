package org.jnbis.test;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jnbis.Bitmap;
import org.jnbis.WSQDecoder;
import org.jnbis.WSQEncoder;

public class WSQTest {

	private static final String WSQ_FILE_IN = "t:/sample_image.wsq";
//	private static final String WSQ_FILE_IN = "t:/FingerImage_1_1_right_index.wsq";
	private static final String WSQ_FILE_OUT = "t:/sample_image_out5.wsq";

	public WSQTest() {
		testDecode();
		testEncode();
	}

	public void testDecode() {
		try {
			File file = new File(WSQ_FILE_IN);
			Bitmap bitmap = WSQDecoder.decode(new FileInputStream(file));
			System.out.println("bitmap = " + bitmap);
			BufferedImage image = convert(bitmap);
			showImage(image);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void testEncode() {
		try {
			File file = new File(WSQ_FILE_IN);
			System.out.println("DEBUG: WSQ_FILE_IN length = " + file.length());
			Bitmap bitmap = WSQDecoder.decode(new FileInputStream(file));
			OutputStream outputStream = new FileOutputStream(WSQ_FILE_OUT);
			float bitrate = 0.75f;
//			int depth = 24; /* set it in bitmap */
//			int ppi = 500; /* set it in bitmap */
			String commentText = "";
			WSQEncoder.encode(outputStream, bitmap, bitrate, commentText);
			outputStream.close();
			
			file = new File(WSQ_FILE_OUT);
			System.out.println("DEBUG: WSQ_FILE_OUT length = " + file.length());
			bitmap = WSQDecoder.decode(new FileInputStream(file));
			System.out.println("bitmap = " + bitmap);
			BufferedImage image = convert(bitmap);
			showImage(image);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void showImage(BufferedImage image) {
		JFrame frame = new JFrame("Image " + image.getWidth() + " x " + image.getHeight());
		Container contentPane = frame.getContentPane();
		JPanel imgPanel = new JPanel(new BorderLayout());
		imgPanel.add(new JLabel(new ImageIcon(image)), BorderLayout.CENTER);
		contentPane.add(imgPanel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

	private static BufferedImage convert(Bitmap bitmap) {
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		byte[] data = bitmap.getPixels();
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		WritableRaster raster = image.getRaster();
		raster.setDataElements(0, 0, width, height, data);
		return image;
	}

	public static void main(String[] arg) {
		new WSQTest();
	}
}
