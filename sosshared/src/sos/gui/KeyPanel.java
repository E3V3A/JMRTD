package sos.gui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.security.Key;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Scanner;

import javax.security.auth.x500.X500Principal;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class KeyPanel extends JPanel
{
	private Key key;
	private JTextArea area;
	
	public KeyPanel(Key key) {
		super(new BorderLayout());
		this.key = key;
		area = new JTextArea(5, 40);
		area.append(keyToString(key));
		area.setEditable(false);
		add(new JScrollPane(area), BorderLayout.CENTER);
	}
	
	public Key getKey() {
		return key;
	}
	
	public void setFont(Font font) {
		super.setFont(font);
		if (area != null) { area.setFont(font); }
	}

	private static String keyToString(Key key) {
		if (key instanceof RSAPublicKey) {
			RSAPublicKey rsaPubKey = (RSAPublicKey)key;
			StringBuffer result = new StringBuffer();
			result.append("RSA Public Key\n");
			result.append("   public exponent = " + rsaPubKey.getPublicExponent() + "\n");
			result.append("   modulus = " + rsaPubKey.getModulus() + "\n");
			return result.toString();
		} else if (key instanceof RSAPrivateKey) {
			RSAPrivateKey rsaPubKey = (RSAPrivateKey)key;
			StringBuffer result = new StringBuffer();
			result.append("RSA Private Key\n");
			result.append("   private exponent = " + rsaPubKey.getPrivateExponent() + "\n");
			result.append("   modulus = " + rsaPubKey.getModulus() + "\n");
			return result.toString();
		} else {
			return key.toString();
		}
	}
}
