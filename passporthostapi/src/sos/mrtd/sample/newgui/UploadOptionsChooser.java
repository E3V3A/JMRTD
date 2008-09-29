package sos.mrtd.sample.newgui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collection;

import javax.smartcardio.CardTerminal;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.filechooser.FileFilter;

import sos.smartcards.CardManager;

public class UploadOptionsChooser extends JComponent {

	public static final int APPROVE_OPTION = 0;
	private UploadOptionsPanel panel;
	private BACEntry bacEntry;
	private PublicKey aaPublicKey;

	public UploadOptionsChooser(BACEntry bacEntry, PublicKey aaPublicKey) {
		this.bacEntry = bacEntry;
		this.aaPublicKey = aaPublicKey;
	}

	public int showOptionsDialog(Container parent) {
		panel = new UploadOptionsPanel(aaPublicKey != null);
		String[] options = { "OK", "Cancel" };
		int result =
			JOptionPane.showOptionDialog(parent, panel, "Upload options", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, "OK");
		return result;
	}

	/* TODO: make privateKey, BACEntry and chosen options available to caller... */

	public CardTerminal getSelectedTerminal() {
		return panel.getSelectedTerminal();
	}

	private class UploadOptionsPanel extends JPanel
	{
		private JComboBox terminalsComboBox;
		private JCheckBox bacCheckBox, aaCheckBox;
		private BACEntryField bacEntryField;
		private JTextField fileTF;
		private JButton browseButton;

		public UploadOptionsPanel(boolean isAASelected) {
			super(new BorderLayout());
			JPanel northPanel = new JPanel();
			terminalsComboBox = new JComboBox();
			northPanel.add(new JLabel("Terminal: "));
			northPanel.add(terminalsComboBox);

			JPanel centerPanel = new JPanel(new BorderLayout());

			JPanel bacPanel = new JPanel();
			bacCheckBox = new JCheckBox(new BACSelectedAction());
			bacPanel.add(bacCheckBox);
			bacEntryField = new BACEntryField(bacEntry, false);
			bacEntryField.setEnabled(bacCheckBox.isSelected());
			bacPanel.add(bacEntryField);

			JPanel aaPanel = new JPanel();
			aaCheckBox = new JCheckBox(new AASelectedAction());
			aaCheckBox.setSelected(isAASelected);
			aaPanel.add(aaCheckBox);
			fileTF = new JTextField(20);
			aaPanel.add(fileTF);
			browseButton = new JButton("Browse");
			aaPanel.add(browseButton);
			browseButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						File file = browseForKeyFile("Select private key for Active Authentication");
						fileTF.setText(file.getAbsolutePath());						
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			});

			centerPanel.add(bacPanel, BorderLayout.NORTH);
			centerPanel.add(aaPanel, BorderLayout.SOUTH);

			add(northPanel, BorderLayout.NORTH);
			add(centerPanel, BorderLayout.CENTER);

			Collection<CardTerminal> terminals = CardManager.getInstance().getTerminals();
			for (CardTerminal terminal: terminals) {
				terminalsComboBox.addItem(terminal);
			}
		}

		public File getAAKeyFile() {
			return new File(fileTF.getText());
		}

		public CardTerminal getSelectedTerminal() {
			return (CardTerminal)terminalsComboBox.getSelectedItem();
		}

		private class BACSelectedAction extends AbstractAction
		{
			public BACSelectedAction() {
				putValue(SHORT_DESCRIPTION, "Enable Basic Access Control");
				putValue(NAME, "BAC");
			}

			public void actionPerformed(ActionEvent e) {
				JToggleButton src = (JToggleButton)e.getSource();
				bacEntryField.setEnabled(src.isSelected());
			}
		}

		private class AASelectedAction extends AbstractAction
		{
			public AASelectedAction() {
				putValue(SHORT_DESCRIPTION, "Enable Active Authentication");
				putValue(NAME, "AA");
			}

			public void actionPerformed(ActionEvent e) {
				JToggleButton src = (JToggleButton)e.getSource();
				fileTF.setEnabled(src.isSelected());
				browseButton.setEnabled(src.isSelected());
			}
		}
	}

	private File browseForKeyFile(String title) {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileFilter(new FileFilter() {
			public boolean accept(File f) { return f.isDirectory()
				|| f.getName().endsWith("cer") || f.getName().endsWith("CER")
				|| f.getName().endsWith("der") || f.getName().endsWith("DER")
				|| f.getName().endsWith("x509") || f.getName().endsWith("X509")
				|| f.getName().endsWith("pkcs8") || f.getName().endsWith("PKCS8"); }
			public String getDescription() { return "Key files"; }				
		});
		chooser.setDialogTitle(title);
		chooser.setToolTipText(title);
		int choice = chooser.showOpenDialog(getParent());
		switch (choice) {
		case JFileChooser.APPROVE_OPTION:
			return chooser.getSelectedFile();
		}
		return null;
	}

	public boolean isBACSelected() {
		return panel.bacCheckBox.isSelected();
	}

	public boolean isAASelected() {
		return panel.aaCheckBox.isSelected();
	}

	public PrivateKey getAAPrivateKey() throws GeneralSecurityException, IOException {
		File file = panel.getAAKeyFile();
		DataInputStream fileIn = new DataInputStream(new FileInputStream(file));
		byte[] privateKeyBytes = new byte[(int)file.length()];
		fileIn.readFully(privateKeyBytes);
		PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
		KeyFactory factory = KeyFactory.getInstance("RSA");
		PrivateKey privateKey = factory.generatePrivate(privateKeySpec);
		return privateKey;
	}

	public BACEntry getBACEntry() {
		BACEntryField bef = panel.bacEntryField;
		return new BACEntry(bef.getDocumentNumber(), bef.getDateOfBirth(), bef.getDateOfExpiry());
	}
}