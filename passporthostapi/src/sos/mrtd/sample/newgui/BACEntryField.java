package sos.mrtd.sample.newgui;

import java.awt.event.ActionListener;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class BACEntryField extends Box
{
	private JButton addButton;
	private JTextField docNrTF, dateOfBirthTF, dateOfExpiryTF;

	public BACEntryField() {
		super(BoxLayout.X_AXIS);
		docNrTF = new JTextField(9);
		dateOfBirthTF = new JTextField(6);
		dateOfExpiryTF = new JTextField(6);
		addButton = new JButton("Add");
		add(new JLabel("Document number: "));
		add(Box.createHorizontalStrut(5));
		add(docNrTF);
		add(Box.createHorizontalStrut(10));
		add(new JLabel("Date of birth: "));
		add(Box.createHorizontalStrut(5));
		add(dateOfBirthTF);
		add(Box.createHorizontalStrut(10));
		add(new JLabel("Date of expiry: "));
		add(Box.createHorizontalStrut(5));
		add(dateOfExpiryTF);
		add(Box.createHorizontalStrut(10));
		add(addButton);
	}
	
	public BACEntryField(String documentNumber, String dateOfBirth, String dateOfExpiry) {
		this();
		docNrTF.setText(documentNumber);
		dateOfBirthTF.setText(dateOfBirth);
		dateOfExpiryTF.setText(dateOfExpiry);
	}

	public void setAction(Action action) {
		addButton.setAction(action);
	}
	
	public void addActionListener(ActionListener l) {
		addButton.addActionListener(l);
	}
	
	public String getDocumentNumber() {
		return docNrTF.getText();
	}

	public String getDateOfBirth() {
		return dateOfBirthTF.getText();
	}

	public String getDateOfExpiry() {
		return dateOfExpiryTF.getText();
	}
}
