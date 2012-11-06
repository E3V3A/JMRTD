package org.jmrtd.jj2000.test;

import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.jmrtd.jj2000.Bitmap;
import org.jmrtd.jj2000.JJ2000Decoder;
import org.jmrtd.jj2000.JJ2000Encoder;

public class ImageTest {

	private static final String J2K_FILE_IN = "./tmp/1.jp2";
	private static final String J2K_FILE_OUT = "./tmp/out.jp2";
	
	
	public ImageTest() {
		try {
			InputStream inputStream = new FileInputStream(J2K_FILE_IN);
			Bitmap bitmap = JJ2000Decoder.decode(inputStream);
            show(bitmap);
            
            OutputStream outputStream = new FileOutputStream(J2K_FILE_OUT);
            JJ2000Encoder.encode(outputStream, bitmap, 1.5);
            outputStream.flush();
            outputStream.close();
            
            Bitmap bitmap2 = JJ2000Decoder.decode(new FileInputStream(J2K_FILE_OUT));
            show(bitmap2);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void show(Bitmap bitmap) {
		BufferedImage image = toBufferedImage(bitmap);		
        JFrame frame = new JFrame();
        frame.setLayout(new BorderLayout());
        frame.getContentPane().add(new JLabel(new ImageIcon(image)));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
	}

	private static BufferedImage toBufferedImage(Bitmap bitmap) {
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		int[] pixels = bitmap.getPixels();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        WritableRaster raster = image.getRaster();
        raster.setDataElements(0, 0, width, height, pixels);
        return image;
	}
	
	public static void main(String[] arg) {
		new ImageTest();
	}
}
