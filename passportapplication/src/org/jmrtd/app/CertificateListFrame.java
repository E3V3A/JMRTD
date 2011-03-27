package org.jmrtd.app;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.math.BigInteger;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.security.auth.x500.X500Principal;
import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.sourceforge.scuba.swing.CertificateChainPanel;
import net.sourceforge.scuba.util.Icons;

public class CertificateListFrame extends JMRTDFrame
{
	private static final Icon CERTIFICATE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("script_key"));
	private static final Dimension PREFERRED_SIZE = new Dimension(600, 400);
	
	private static final long serialVersionUID = -1239469067988004321L;
	
	private List<Certificate> certificates;
	private ActionMap actionMap;

	public CertificateListFrame(String title, List<Certificate> certs) {
		super(title);
		Container cp = getContentPane();
		cp.setLayout(new BorderLayout());
		this.certificates = certs;
		this.actionMap = new ActionMap();
		
		final Component parent = this;
		
		final JList list = new JList(new AbstractListModel() {

			public int getSize() {
				return certificates.size();
			}

			public Object getElementAt(int index) {
				Certificate certificate = certificates.get(index);
				if (certificate instanceof X509Certificate) {
					X509Certificate x509Cert = (X509Certificate)certificate;
					X500Principal subject = x509Cert.getSubjectX500Principal();
					BigInteger serialNumber = x509Cert.getSerialNumber();
					return subject.getName() + "(" + serialNumber + ")";
				} else {
					return certificate.toString();
				}
			}
		});
		cp.add(new JScrollPane(list), BorderLayout.CENTER);
	}
	
	public List<Certificate> getCertificates() {
		return certificates;
	}
	
	public Dimension getPreferredSize() {
		return PREFERRED_SIZE;
	}
	
	private Action getShowAction(final Component parent, final X509Certificate certificate) {
		Action action = actionMap.get("showCertificate");
		if (action != null) { return action; }
		action = new AbstractAction() {
			
			private static final long serialVersionUID = 1818703839607672072L;

			public void actionPerformed(ActionEvent e) {
				JPanel certificatePanel = new CertificateChainPanel(certificate, true);
				JOptionPane.showMessageDialog(parent, certificatePanel);
			}
		};
		action.putValue(Action.SMALL_ICON, CERTIFICATE_ICON);
		action.putValue(Action.LARGE_ICON_KEY, CERTIFICATE_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Show certificate");
		action.putValue(Action.NAME, "Show...");
		actionMap.put("showCertificate", action);
		return action;
	}
}
