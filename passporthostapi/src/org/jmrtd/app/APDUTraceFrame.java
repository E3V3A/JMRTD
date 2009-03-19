package org.jmrtd.app;

import java.awt.Font;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.swing.*;

import sos.smartcards.APDUListener;
import sos.util.Hex;

public class APDUTraceFrame extends JFrame implements APDUListener
{
	private static final long serialVersionUID = -584060710792989841L;

	private JTextArea area;

	public APDUTraceFrame(String title) {
		super(title);
		area = new JTextArea(40, 80);
		area.setFont(new Font("Monospaced", Font.PLAIN, 9));
		getContentPane().add(new JScrollPane(area));
	}

	public APDUTraceFrame() {
		this("APDU trace");
	}

	public void exchangedAPDU(CommandAPDU capdu, ResponseAPDU rapdu) {
		area.append("C:\n" + Hex.bytesToPrettyString(capdu.getBytes()) + "\n");
		area.append("R:\n" + Hex.bytesToPrettyString(rapdu.getBytes()) + "\n");
		area.setCaretPosition(area.getDocument().getLength() - 1);
	}	
}
