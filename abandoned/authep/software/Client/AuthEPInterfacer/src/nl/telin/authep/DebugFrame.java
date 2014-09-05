/*
 *  AuthEP - Interfacer.
 *
 *  Copyright (C) 2009  Telematica Instituut
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nl.telin.authep;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class DebugFrame extends JFrame implements ILogger {

	private static final long serialVersionUID = 3873547530119105871L;

	private JTextArea area;

	public DebugFrame()
	{
		setLayout(new BorderLayout());
		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BorderLayout());
		area = new JTextArea();
		centerPanel.add(new JScrollPane(area), BorderLayout.CENTER);

		add(centerPanel, BorderLayout.CENTER);
		this.setSize(300, 300);
	}

	public void log(String msg) {
		area.append(msg + "\n");
		System.out.println(getClass().getCanonicalName() + ": " + msg);
	}
}
