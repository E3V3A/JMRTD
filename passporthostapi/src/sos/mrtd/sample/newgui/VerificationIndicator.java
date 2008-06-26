package sos.mrtd.sample.newgui;

import java.awt.Font;
import java.awt.Image;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import sos.gui.Icons;

public class VerificationIndicator extends Box
{
	private static final Font KEY_FONT = new Font("Sans-serif", Font.PLAIN, 8);

	public static final int VERIFICATION_UNKNOWN = 0, VERIFICATION_SUCCEEDED = 1, VERIFICATION_FAILED = -1;

	public Image
	VERIFICATION_SUCCEEDED_ICON = Icons.getFamFamFamSilkIcon("tick"),
	VERIFICATION_FAILED_ICON = Icons.getFamFamFamSilkIcon("cross"),
	VERIFICATION_UNKNOWN_ICON = Icons.getFamFamFamSilkIcon("error");

	private static final int SPACING = 25;

	private JLabel bacLabel, dsLabel, csLabel, aaLabel;
	private ImageIcon bacIcon, dsIcon, csIcon, aaIcon;

	public VerificationIndicator() {
		super(BoxLayout.X_AXIS);
		bacIcon = new ImageIcon(VERIFICATION_UNKNOWN_ICON);
		aaIcon = new ImageIcon(VERIFICATION_UNKNOWN_ICON);
		dsIcon = new ImageIcon(VERIFICATION_UNKNOWN_ICON);
		csIcon = new ImageIcon(VERIFICATION_UNKNOWN_ICON);

		bacLabel = new JLabel(bacIcon);
		aaLabel = new JLabel(aaIcon);
		dsLabel = new JLabel(dsIcon);
		csLabel = new JLabel(csIcon);

		setAAState(VERIFICATION_UNKNOWN);

		JLabel bacKeyLabel = new JLabel("BAC: ");
		bacKeyLabel.setFont(KEY_FONT);
		add(bacKeyLabel);
		add(bacLabel);

		add(Box.createHorizontalStrut(SPACING));

		JLabel aaKeyLabel = new JLabel("AA: ");
		aaKeyLabel.setFont(KEY_FONT);
		add(aaKeyLabel);
		add(aaLabel);

		add(Box.createHorizontalStrut(SPACING));

		JLabel dsKeyLabel = new JLabel("DS: ");
		dsKeyLabel.setFont(KEY_FONT);
		add(dsKeyLabel);
		add(dsLabel);

		add(Box.createHorizontalStrut(SPACING));

		JLabel csKeyLabel = new JLabel("CS: ");
		csKeyLabel.setFont(KEY_FONT);
		add(csKeyLabel);
		add(csLabel);

	}

	public void setBACState(int state) {
		switch (state) {
		case VERIFICATION_SUCCEEDED: bacIcon.setImage(VERIFICATION_SUCCEEDED_ICON); bacLabel.setToolTipText("Succeeded"); break;
		case VERIFICATION_FAILED: bacIcon.setImage(VERIFICATION_FAILED_ICON); bacLabel.setToolTipText("Failed"); break;
		default: bacIcon.setImage(VERIFICATION_UNKNOWN_ICON); bacLabel.setToolTipText("Not checked"); break;
		}
	}

	public void setAAState(int state) {
		switch (state) {
		case VERIFICATION_SUCCEEDED: aaIcon.setImage(VERIFICATION_SUCCEEDED_ICON); aaLabel.setToolTipText("Succeeded"); break;
		case VERIFICATION_FAILED: aaIcon.setImage(VERIFICATION_FAILED_ICON); aaLabel.setToolTipText("Failed"); break;
		default: aaIcon.setImage(VERIFICATION_UNKNOWN_ICON); aaLabel.setToolTipText("Not checked"); break;
		}
	}

	public void setDSState(int state) {
		switch (state) {
		case VERIFICATION_SUCCEEDED: dsIcon.setImage(VERIFICATION_SUCCEEDED_ICON); dsLabel.setToolTipText("Succeeded"); break;
		case VERIFICATION_FAILED: dsIcon.setImage(VERIFICATION_FAILED_ICON); dsLabel.setToolTipText("Failed"); break;
		default: dsIcon.setImage(VERIFICATION_UNKNOWN_ICON); dsLabel.setToolTipText("Not checked"); break;
		}
	}
}
