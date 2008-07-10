package sos.mrtd.sample.newgui;

import java.awt.event.ActionListener;
import java.util.Date;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;

import sos.gui.DateField;

public class BACEntryField extends Box
{

	
	private JButton addButton;
	private JTextField docNrTF;
	private DateField dateOfBirthField, dateOfExpiryField;

	public BACEntryField() {
		super(BoxLayout.X_AXIS);
		docNrTF = new JTextField(9);
		// dateOfBirthTF = new JTextField(6);
		dateOfBirthField = new DateField();
		dateOfExpiryField = new DateField();
		addButton = new JButton("Add");
		add(new JLabel("Document number: "));
		add(Box.createHorizontalStrut(5));
		add(docNrTF);
		add(Box.createHorizontalStrut(10));
		add(new JLabel("Date of birth: "));
		add(Box.createHorizontalStrut(5));
		add(dateOfBirthField);
		add(Box.createHorizontalStrut(10));
		add(new JLabel("Date of expiry: "));
		add(Box.createHorizontalStrut(5));
		add(dateOfExpiryField);
		add(Box.createHorizontalStrut(10));
		add(addButton);
	}
	
	public BACEntryField(String documentNumber, Date dateOfBirth, Date dateOfExpiry) {
		this();
		docNrTF.setText(documentNumber);
		dateOfBirthField.setDate(dateOfBirth);
		dateOfExpiryField.setDate(dateOfExpiry);
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

	public Date getDateOfBirth() {
		return dateOfBirthField.getDate();
	}

	public Date getDateOfExpiry() {
		return dateOfExpiryField.getDate();
	}
}
