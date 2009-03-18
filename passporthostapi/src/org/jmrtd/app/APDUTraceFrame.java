package org.jmrtd.app;

import java.awt.Font;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.swing.*;

import sos.smartcards.APDUListener;
import sos.util.Hex;

public class APDUTraceFrame extends JFrame implements APDUListener
{
	private JTextArea traceArea;
	
	public APDUTraceFrame(String title) {
		super(title);
		traceArea = new JTextArea(40, 80);
		traceArea.setFont(new Font("Monospaced", Font.PLAIN, 9));
		getContentPane().add(new JScrollPane(traceArea));
	}
	
	public APDUTraceFrame() {
		this("APDU trace");
	}

	public void exchangedAPDU(CommandAPDU capdu, ResponseAPDU rapdu) {
		traceArea.append("C:\n" + Hex.bytesToPrettyString(capdu.getBytes()) + "\n");
		traceArea.append("R:\n" + Hex.bytesToPrettyString(rapdu.getBytes()) + "\n");
	}	
}
