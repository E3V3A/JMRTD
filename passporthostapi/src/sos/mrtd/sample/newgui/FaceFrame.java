package sos.mrtd.sample.newgui;

import java.awt.Container;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileFilter;

import sos.gui.ImagePanel;
import sos.mrtd.FaceInfo;
import sos.util.Hex;
import sos.util.Icons;
import sos.util.Images;

/* TODO: implement a frame instead of the dialog!
 *    including a menu bar with menu items:
 *       file -> save as JPEG, PNG, ...,
 *       view -> featurepoints (checkbox menu item)
 */
public class FaceFrame extends JFrame
{
	private static final Icon SAVE_AS_PNG_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("disk"));
	private static final Icon SAVE_AS_PNG_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("disk"));
	private static final Icon CLOSE_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("bin"));
	private static final Icon CLOSE_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("bin"));
	private static final Icon IMAGE_INFO_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("information"));
	private static final Icon IMAGE_INFO_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("information"));
	private static final Icon FEATURE_POINTS_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("chart_line"));
	private static final Icon FEATURE_POINTS_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("chart_line"));


	private FaceInfo info;
	private ImagePanel imagePanel;

	public FaceFrame(FaceInfo info) {
		super("Portrait");
		this.info = info;

		/* Menu bar */
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(createFileMenu());
		menuBar.add(createViewMenu());
		setJMenuBar(menuBar);

		/* Frame content */
		Image image = info.getImage();
		imagePanel = new ImagePanel();
		imagePanel.setImage(image);
		Container cp = getContentPane();
		cp.add(imagePanel);
	}

	private JMenu createFileMenu() {
		JMenu fileMenu = new JMenu("File");

		/* Save As...*/
		JMenuItem saveAsItem = new JMenuItem("Save As...");
		fileMenu.add(saveAsItem);
		saveAsItem.setAction(new SaveAsPNGAction());

		/* Close */
		JMenuItem closeItem = new JMenuItem("Close");
		fileMenu.add(closeItem);
		closeItem.setAction(new CloseAction());

		return fileMenu;
	}

	private JMenu createViewMenu() {
		JMenu viewMenu = new JMenu("View");

		/* Image Info */
		JMenuItem viewImageInfo = new JMenuItem();
		viewMenu.add(viewImageInfo);
		viewImageInfo.setAction(new ViewImageInfoAction());

		/* Feature Points */
		JCheckBoxMenuItem viewFeaturePoints = new JCheckBoxMenuItem();
		viewMenu.add(viewFeaturePoints);
		viewFeaturePoints.setAction(new ViewFeaturePointsAction());

		return viewMenu;
	}

	private class SaveAsPNGAction extends AbstractAction
	{
		public SaveAsPNGAction() {
			putValue(SMALL_ICON, SAVE_AS_PNG_SMALL_ICON);
			putValue(LARGE_ICON_KEY, SAVE_AS_PNG_LARGE_ICON);
			putValue(SHORT_DESCRIPTION, "Save image in PNG format");
			putValue(NAME, "Save As PNG...");
		}

		public void actionPerformed(ActionEvent e) {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setFileFilter(new FileFilter() {
				public boolean accept(File f) { return f.isDirectory() || f.getName().endsWith("png") || f.getName().endsWith("PNG"); }
				public String getDescription() { return "PNG files"; }				
			});
			int choice = fileChooser.showSaveDialog(getContentPane());
			switch (choice) {
			case JFileChooser.APPROVE_OPTION:
				try {
					File file = fileChooser.getSelectedFile();
					ImageIO.write(Images.toBufferedImage(imagePanel.getImage()), "png", file);  
				} catch (IOException fnfe) {
					fnfe.printStackTrace();
				}
				break;
			default: break;
			}
		}
		


	}

	private class ViewImageInfoAction extends AbstractAction
	{
		public ViewImageInfoAction() {
			putValue(SMALL_ICON, IMAGE_INFO_SMALL_ICON);
			putValue(LARGE_ICON_KEY, IMAGE_INFO_LARGE_ICON);
			putValue(SHORT_DESCRIPTION, "View Image Information");
			putValue(NAME, "Image Info...");
		}

		public void actionPerformed(ActionEvent e) {
			JTextArea area = new JTextArea();
			area.append(info.toString());
			JOptionPane.showMessageDialog(getContentPane(), new JScrollPane(area), "Image information", JOptionPane.PLAIN_MESSAGE, null);

		}
	}

	private class ViewFeaturePointsAction extends AbstractAction
	{
		public ViewFeaturePointsAction() {
			putValue(SMALL_ICON, FEATURE_POINTS_SMALL_ICON);
			putValue(LARGE_ICON_KEY, FEATURE_POINTS_LARGE_ICON);
			putValue(SHORT_DESCRIPTION, "View Feature Points");
			putValue(NAME, "Feature Points");
		}

		public void actionPerformed(ActionEvent e) {
			Object src = e.getSource();
			if (src instanceof AbstractButton) {
				AbstractButton button = (AbstractButton)src;
				if (button.isSelected()) {
					System.out.println("DEBUG: Feature Points switched on");
				} else {
					System.out.println("DEBUG: Feature Points switched off");
				}
			}
		}
	}

	private class CloseAction extends AbstractAction
	{
		public CloseAction() {
			putValue(SMALL_ICON, CLOSE_SMALL_ICON);
			putValue(LARGE_ICON_KEY, CLOSE_LARGE_ICON);
			putValue(SHORT_DESCRIPTION, "Close Window");
			putValue(NAME, "Close");
		}

		public void actionPerformed(ActionEvent e) {
			dispose();
		}
	}
}
