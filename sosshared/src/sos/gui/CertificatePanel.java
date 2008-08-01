package sos.gui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Scanner;

import javax.security.auth.x500.X500Principal;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class CertificatePanel extends JPanel
{
	private static final long serialVersionUID = -1109469067988004311L;

	private Certificate certificate;
	private JTextArea area;
	
	public CertificatePanel(Certificate certificate) {
		super(new BorderLayout());
		this.certificate = certificate;
		area = new JTextArea(20, 40);
		area.append(certificateToString(certificate));
		area.setEditable(false);
		add(new JScrollPane(area), BorderLayout.CENTER);
		add(new KeyPanel(certificate.getPublicKey()), BorderLayout.SOUTH);
	}
	
	public Certificate getCertificate() {
		return certificate;
	}
	
	public void setFont(Font font) {
		super.setFont(font);
		if (area != null) { area.setFont(font); }
	}

	private static String certificateToString(Certificate certificate) {
		String certText = null;
		if (certificate instanceof X509Certificate) {
			StringBuffer result = new StringBuffer();
			X509Certificate x509Cert = (X509Certificate)certificate;
			result.append("subject:\n" );
			result.append(principalToString(x509Cert.getSubjectX500Principal()));
			result.append('\n');
			result.append("issuer:\n");
			result.append(principalToString(x509Cert.getIssuerX500Principal()));
			result.append('\n');
			result.append("Not before: " + x509Cert.getNotBefore() + "\n");
			result.append("Not after: " + x509Cert.getNotAfter() + "\n");
			certText = result.toString();
		} else {
			certText = certificate.toString();
		}
		return certText;
	}

	private static String principalToString(X500Principal principal) {
		StringBuffer result = new StringBuffer();
		String subject = principal.getName(X500Principal.RFC1779);
		Scanner scanner = new Scanner(subject);
		scanner.useDelimiter(",");
		while (scanner.hasNext()) {
			String token = scanner.next().trim();
			result.append("   ");
			result.append(token);
			result.append('\n');
		}
		return result.toString();
	}
}
