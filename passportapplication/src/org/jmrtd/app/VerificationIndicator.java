/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2008  The JMRTD team
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 * $Id: VerificationIndicator.java 893 2009-03-23 15:43:42Z martijno $
 */

package org.jmrtd.app;

import java.awt.Font;
import java.awt.Image;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import net.sourceforge.scuba.util.Icons;

/**
 * Component for displaying the verification status.
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision: 893 $
 */
public class VerificationIndicator extends Box
{
	private static final long serialVersionUID = -1458554034529575752L;

	private static final Font KEY_FONT = new Font("Sans-serif", Font.PLAIN, 8);
	private static final int BAC_INDICATOR = 0, AA_INDICATOR = 1, EAC_INDICATOR = 2, DS_INDICATOR = 3, CS_INDICATOR = 4;
	
	private static final int VERIFICATION_UNKNOWN = 0, VERIFICATION_SUCCEEDED = 1, VERIFICATION_FAILED = -1;

	private static final Image
	SUCCEEDED_ICON = Icons.getFamFamFamSilkIcon("tick"),
	FAILED_ICON = Icons.getFamFamFamSilkIcon("cross"),
	NOT_CHECKED_ICON = Icons.getFamFamFamSilkIcon("error");

	private static final int SPACING = 25;

	private JLabel bacLabel, dsLabel, csLabel, aaLabel, eacLabel;
	private ImageIcon bacIcon, dsIcon, csIcon, aaIcon, eacIcon;

	public VerificationIndicator() {
		super(BoxLayout.X_AXIS);
		bacIcon = new ImageIcon(NOT_CHECKED_ICON);
		aaIcon = new ImageIcon(NOT_CHECKED_ICON);
		dsIcon = new ImageIcon(NOT_CHECKED_ICON);
		csIcon = new ImageIcon(NOT_CHECKED_ICON);
        eacIcon = new ImageIcon(NOT_CHECKED_ICON);

		bacLabel = new JLabel(bacIcon);
		aaLabel = new JLabel(aaIcon);
		dsLabel = new JLabel(dsIcon);
		csLabel = new JLabel(csIcon);
        eacLabel = new JLabel(eacIcon);

		setAANotChecked();

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

        JLabel eacKeyLabel = new JLabel("EAC: ");
        eacKeyLabel.setFont(KEY_FONT);
        add(eacKeyLabel);
        add(eacLabel);

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

	public void setBACSucceeded() {
		setState(BAC_INDICATOR, VERIFICATION_SUCCEEDED, "Succeeded");
	}
	
	public void setBACFailed(String reason) {
		setState(BAC_INDICATOR, VERIFICATION_FAILED, reason);
	}
	
	public void setBACNotChecked() {
		setState(BAC_INDICATOR, VERIFICATION_UNKNOWN, null);
	}

	public void setAASucceeded() {
		setState(AA_INDICATOR, VERIFICATION_SUCCEEDED, null);
	}

	public void setAAFailed(String reason) {
		setState(AA_INDICATOR, VERIFICATION_FAILED, reason);
	}
	
	public void setAANotChecked() {
		setState(AA_INDICATOR, VERIFICATION_UNKNOWN, null);
	}

    public void setEACSucceeded() {
        setState(EAC_INDICATOR, VERIFICATION_SUCCEEDED, "Succeeded");
    }
    
    public void setEACFailed(String reason) {
        setState(EAC_INDICATOR, VERIFICATION_FAILED, reason);
    }
    
    public void setEACNotChecked() {
        setState(EAC_INDICATOR, VERIFICATION_UNKNOWN, null);
    }

	public void setDSSucceeded() {
		setState(DS_INDICATOR, VERIFICATION_SUCCEEDED, null);
	}

	public void setDSFailed(String reason) {
		setState(DS_INDICATOR, VERIFICATION_FAILED, reason);
	}

	public void setDSNotChecked() {
		setState(DS_INDICATOR, VERIFICATION_UNKNOWN, null);
	}

	public void setCSSucceeded() {
		setState(CS_INDICATOR, VERIFICATION_SUCCEEDED, null);
	}

	public void setCSFailed(String reason) {
		setState(CS_INDICATOR, VERIFICATION_FAILED, reason);
	}

	public void setCSNotChecked(String reason) {
		setState(CS_INDICATOR, VERIFICATION_UNKNOWN, reason);
	}
	
	private void setState(int indicator, int result, String reason) {
		ImageIcon icon = null;
		JLabel label = null;
		switch (indicator) {
		case BAC_INDICATOR: label= bacLabel; icon = bacIcon; break;
		case AA_INDICATOR: label = aaLabel; icon = aaIcon; break;
        case EAC_INDICATOR: label = eacLabel; icon = eacIcon; break;
		case DS_INDICATOR: label = dsLabel; icon = dsIcon; break;
		case CS_INDICATOR: label = csLabel; icon = csIcon; break;
		}
		switch (result) {
		case VERIFICATION_SUCCEEDED:
			icon.setImage(SUCCEEDED_ICON);
			label.setToolTipText(reason == null ? "Succeeded" : reason);
			break;
		case VERIFICATION_FAILED:
			icon.setImage(FAILED_ICON);
			label.setToolTipText(reason == null ? "Failed" : reason);
			break;
		default:
			icon.setImage(NOT_CHECKED_ICON);
			label.setToolTipText(reason == null ? "Not checked" : reason);
			break;
		}
		revalidate(); repaint();
	}
}
