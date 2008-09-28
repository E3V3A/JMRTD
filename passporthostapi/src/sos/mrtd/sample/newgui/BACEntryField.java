package sos.mrtd.sample.newgui;

import java.awt.event.ActionListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;

import sos.gui.DateEntryField;
import sos.gui.MRZEntryField;

/**
 * Text field for BAC entries.
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 *
 * @version $Revision: $
 */
public class BACEntryField extends Box implements BACEntrySource
{
	private static final SimpleDateFormat SDF = new SimpleDateFormat("ddMMyy");
	private JButton addButton;
	private MRZEntryField docNrTF;
	private DateEntryField dateOfBirthField, dateOfExpiryField;

	public BACEntryField(boolean showAddButton) {
		super(BoxLayout.X_AXIS);
		docNrTF = new MRZEntryField(9);
		// dateOfBirthTF = new JTextField(6);
		dateOfBirthField = new DateEntryField();
		dateOfExpiryField = new DateEntryField();
		addButton = new JButton("Add");
		add(new JLabel("Doc.nr.: "));
		add(Box.createHorizontalStrut(5));
		add(docNrTF);
		add(Box.createHorizontalStrut(10));
		add(new JLabel("DoB: "));
		add(Box.createHorizontalStrut(5));
		add(dateOfBirthField);
		add(Box.createHorizontalStrut(10));
		add(new JLabel("DoE: "));
		add(Box.createHorizontalStrut(5));
		add(dateOfExpiryField);
		if (showAddButton) {
			add(Box.createHorizontalStrut(10));
			add(addButton);
		}
	}

	public BACEntryField(BACEntry bacEntry, boolean showAddButton) {
		this(showAddButton);
		try {
			setValue(bacEntry.getDocumentNumber(), SDF.parse(bacEntry.getDateOfBirth()), SDF.parse(bacEntry.getDateOfExpiry()));
		} catch (ParseException pe) {
			throw new IllegalArgumentException(pe.toString());
		}
	}

	public BACEntryField(String documentNumber, Date dateOfBirth, Date dateOfExpiry, boolean showAddButton) {
		this(showAddButton);
		setValue(documentNumber, dateOfBirth, dateOfExpiry);
	}

	private void setValue(String documentNumber, Date dateOfBirth, Date dateOfExpiry) {
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

	public void setEnabled(boolean b) {
		addButton.setEnabled(b);
		docNrTF.setEnabled(b);
		dateOfBirthField.setEnabled(b);
		dateOfExpiryField.setEnabled(b);
	}
}
