/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2012  The JMRTD team
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
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import org.jmrtd.VerificationStatus;
import org.jmrtd.VerificationStatus.Verdict;
import org.jmrtd.app.util.IconUtil;

/**
 * Component for displaying the verification status.
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision: 893 $
 */
public class VerificationIndicator extends Box {

	private static final long serialVersionUID = -1458554034529575752L;

	private static final Font KEY_FONT = new Font("Sans-serif", Font.PLAIN, 8);
	private static final int BAC_INDICATOR = 0, AA_INDICATOR = 1, EAC_INDICATOR = 2, HT_INDICATOR = 3, DS_INDICATOR = 4, CS_INDICATOR = 5;

	private static final Logger LOGGER = Logger.getLogger("org.jmrtd");
	
	private static final Image
	SUCCEEDED_ICON = IconUtil.getFamFamFamSilkIcon("tick"),
	FAILED_ICON = IconUtil.getFamFamFamSilkIcon("cross"),
	UNKNOWN_ICON = IconUtil.getFamFamFamSilkIcon("error"),
	NOT_CHECKED_ICON = IconUtil.getFamFamFamSilkIcon("error_add"),
	NOT_PRESENT_ICON = IconUtil.getFamFamFamSilkIcon("error_add");
	private static final int SPACING = 25;

	private JLabel bacLabel, htLabel, dsLabel, csLabel, aaLabel, eacLabel;
	private ImageIcon bacIcon, htIcon, dsIcon, csIcon, aaIcon, eacIcon;

	public VerificationIndicator() {
		super(BoxLayout.X_AXIS);
		bacIcon = new ImageIcon(UNKNOWN_ICON);
		aaIcon = new ImageIcon(UNKNOWN_ICON);
		htIcon = new ImageIcon(UNKNOWN_ICON);
		dsIcon = new ImageIcon(UNKNOWN_ICON);
		csIcon = new ImageIcon(UNKNOWN_ICON);
        eacIcon = new ImageIcon(UNKNOWN_ICON);

		bacLabel = new JLabel(bacIcon);
		aaLabel = new JLabel(aaIcon);
		htLabel = new JLabel(htIcon);
		dsLabel = new JLabel(dsIcon);
		csLabel = new JLabel(csIcon);
        eacLabel = new JLabel(eacIcon);

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

        JLabel hashesKeyLabel = new JLabel("HT: "); // NOTE: HT = Hash Table
		hashesKeyLabel.setFont(KEY_FONT);
		add(hashesKeyLabel);
		add(htLabel);

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

	public void setStatus(VerificationStatus status) {
		setState(BAC_INDICATOR, status.getBAC(), status.getBACReason());
		setState(AA_INDICATOR, status.getAA(), status.getAAReason());
		setState(EAC_INDICATOR, status.getEAC(), status.getEACReason());
		setState(HT_INDICATOR, status.getHT(), status.getHTReason());
		setState(DS_INDICATOR, status.getDS(), status.getDSReason());
		setState(CS_INDICATOR, status.getCS(), status.getCSReason());
	}
	
	private void setState(int indicator, Verdict verdict, String reason) {
		ImageIcon icon = null;
		JLabel label = null;
		switch (indicator) {
		case BAC_INDICATOR: label= bacLabel; icon = bacIcon; break;
		case AA_INDICATOR: label = aaLabel; icon = aaIcon; break;
        case EAC_INDICATOR: label = eacLabel; icon = eacIcon; break;
        case HT_INDICATOR: label = htLabel; icon = htIcon; break;
		case DS_INDICATOR: label = dsLabel; icon = dsIcon; break;
		case CS_INDICATOR: label = csLabel; icon = csIcon; break;
		}
		if (SUCCEEDED_ICON.equals(FAILED_ICON)) {
			LOGGER.warning("Icons not loaded correctly...");
		}
		if (label == null || icon == null) {
			LOGGER.severe("Unclear which indicator component was selected");
			return;
		}
		switch (verdict) {
		case SUCCEEDED:
			icon.setImage(SUCCEEDED_ICON);
			label.setToolTipText(reason == null ? "Succeeded" : reason);
			break;
		case FAILED:
			icon.setImage(FAILED_ICON);
			label.setToolTipText(reason == null ? "Failed" : reason);
			break;
		case NOT_PRESENT:
			icon.setImage(NOT_PRESENT_ICON);
			label.setToolTipText(reason == null ? "Not present" : reason);
			break;
		case NOT_CHECKED:
			icon.setImage(NOT_CHECKED_ICON);
			label.setToolTipText(reason == null ? "Not checked" : reason);
			break;
		default:
			icon.setImage(UNKNOWN_ICON);
			label.setToolTipText(reason == null ? "Unknown" : reason);
			break;
		}
		revalidate(); repaint();
	}
}
