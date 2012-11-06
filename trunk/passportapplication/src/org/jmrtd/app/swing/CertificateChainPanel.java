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
 * $Id: $
 */

package org.jmrtd.app.swing;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Image;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import javax.security.auth.x500.X500Principal;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import org.jmrtd.app.util.IconUtil;

public class CertificateChainPanel extends JPanel
{
	private static final long serialVersionUID = -1109469067988004321L;

	private static final Image
	VALID_ICON = IconUtil.getFamFamFamSilkIcon("tick"),
	INVALID_ICON = IconUtil.getFamFamFamSilkIcon("cross");

	private List<Certificate> certificates;
	private JTabbedPane tabbedPane;

	public CertificateChainPanel(Certificate certificate) {
		this(Collections.singletonList(certificate));
	}

	public CertificateChainPanel(Certificate certificate, boolean isValid) {		
		this(Collections.singletonList(certificate), true, isValid);
	}

	public CertificateChainPanel(List<Certificate> certificates) {
		this(certificates, false, false);
	}
	
	public CertificateChainPanel(List<Certificate> certificates, boolean isValid) {
		this(certificates, true, isValid);
	}

	private CertificateChainPanel(List<Certificate> certificates, boolean showValidity, boolean isValid) {
		super(new BorderLayout());
		this.certificates = certificates;
		tabbedPane = new JTabbedPane();
		int i = 0;
		for (Certificate certificate: certificates) {
			JPanel panel = new JPanel(new BorderLayout());
			JTextArea area = new JTextArea(20, 40);
			area.append(certificateToString(certificate));
			area.setEditable(false);
			panel.add(new JScrollPane(area), BorderLayout.CENTER);
			panel.add(new KeyPanel(certificate.getPublicKey()), BorderLayout.SOUTH);
			if (certificates.size() == 1) {
				add(panel, BorderLayout.CENTER);				
			} else {
				tabbedPane.addTab(Integer.toString(++i), panel);
			}
		}
		if (certificates.size() > 1) {
			add(tabbedPane, BorderLayout.CENTER);
		}
		if (showValidity) {
			JLabel validLabel = new JLabel();
			validLabel.setText(isValid ? "Certificate chain trusted" : "Certificate chain untrusted");
			validLabel.setIcon(isValid ? new ImageIcon(VALID_ICON) : new ImageIcon(INVALID_ICON));
			add(validLabel, BorderLayout.SOUTH);
		}
	}	

	public Certificate getCertificate() {
		if (certificates.size() == 1) {
			return certificates.get(0);
		}
		int i = tabbedPane.getSelectedIndex();
		return certificates.get(i);
	}

	public List<Certificate> getCertificates() {
		return certificates;
	}

	public void setFont(Font font) {
		super.setFont(font);
	}

	private static String certificateToString(Certificate certificate) {
		String certText = null;
		if (certificate == null) { return null; }
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
			result.append('\n');
			result.append("Serial number: " + x509Cert.getSerialNumber() + "\n");
			certText = result.toString();
		} else {
			certText = certificate.toString();
		}
		return certText;
	}

	private static String principalToString(X500Principal principal) {
		StringBuffer result = new StringBuffer();
		String subject = principal.getName(X500Principal.CANONICAL);
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
