package org.jmrtd.app;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import sos.mrtd.PassportManager;

public class PreferencesPanel extends JPanel
{
	public PreferencesPanel(PassportManager pm) {
		super(new BorderLayout());
		add(new JLabel("DEBUG: UNDER CONSTRUCTION!"), BorderLayout.CENTER);
	}
	
	public String getName() {
		return "Preferences";
	}
}
