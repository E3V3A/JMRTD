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
	private static final int BAC_INDICATOR = 0, AA_INDICATOR = 1, DS_INDICATOR = 2, CS_INDICATOR = 3;
	
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

	public void setBACState(int state) { setState(BAC_INDICATOR, state); }

	public void setAAState(int state) { setState(AA_INDICATOR, state);	}

	public void setDSState(int state) { setState(DS_INDICATOR, state); }
	
	public void setCSState(int state) {	setState(CS_INDICATOR, state); }
	
	private void setState(int indicator, int result) {
		ImageIcon icon = null;
		JLabel label = null;
		switch (indicator) {
		case BAC_INDICATOR: label= bacLabel; icon = bacIcon; break;
		case AA_INDICATOR: label = aaLabel; icon = aaIcon; break;
		case DS_INDICATOR: label = dsLabel; icon = dsIcon; break;
		case CS_INDICATOR: label = csLabel; icon = csIcon; break;
		}
		switch (result) {
		case VERIFICATION_SUCCEEDED:
			icon.setImage(VERIFICATION_SUCCEEDED_ICON);
			label.setToolTipText("Succeeded");
			break;
		case VERIFICATION_FAILED:
			icon.setImage(VERIFICATION_FAILED_ICON);
			label.setToolTipText("Failed");
			break;
		default:
			icon.setImage(VERIFICATION_UNKNOWN_ICON);
			label.setToolTipText("Not checked");
			break;
		}
	}
}
