package org.jmrtd.app;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.security.cert.Certificate;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.x500.X500Principal;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import net.sourceforge.scuba.data.Country;
import net.sourceforge.scuba.data.ISOCountry;
import net.sourceforge.scuba.swing.CertificateChainPanel;
import net.sourceforge.scuba.util.Icons;

public class CertificateMasterListFrame extends JMRTDFrame
{
	private static final Icon CSCA_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("world"));
	private static final Icon CERTIFICATE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("script_key"));
	
	private static final Dimension PREFERRED_SIZE = new Dimension(480, 280);

	private static final long serialVersionUID = -1239469067988004321L;

	private List<Certificate> certificates;

	private Map<Country, List<Certificate>> countryToCertificates;

	private DefaultMutableTreeNode rootNode;
	private List<String> countryNodes;

	private ActionMap actionMap;

	public CertificateMasterListFrame(String title, Set<TrustAnchor> anchors) {
		this(title, getAsCertificates(anchors));
	}

	public CertificateMasterListFrame(String title, Collection<Certificate> certs) {
		super(title);
		Container cp = getContentPane();
		cp.setLayout(new BorderLayout());
		this.certificates = new ArrayList<Certificate>(certs);
		countryToCertificates = new HashMap<Country, List<Certificate>>();
		for (Certificate certificate: certificates) {
			if (!(certificate instanceof X509Certificate)) { continue; }
			Country country = getIssuerCountry((X509Certificate)certificate);
			List<Certificate> countryCertificates = countryToCertificates.get(country);
			if (countryCertificates == null) {
				countryCertificates = new ArrayList<Certificate>();
				countryToCertificates.put(country, countryCertificates);
			}
			countryCertificates.add(certificate);
		}

		this.actionMap = new ActionMap();

		final Component parent = this;

		rootNode = new DefaultMutableTreeNode("CSCA");

		for (Map.Entry<Country, List<Certificate>> entry: countryToCertificates.entrySet()) {
			Country country = entry.getKey();
			List<Certificate> certificates = entry.getValue();
			DefaultMutableTreeNode countryNode = new DefaultMutableTreeNode(country);
			rootNode.add(countryNode);
			for (Certificate certificate: certificates) {
				DefaultMutableTreeNode certificateNode = new DefaultMutableTreeNode(certificate);
				countryNode.add(certificateNode);
			}
		}
		final JTree tree = new JTree(new DefaultTreeModel(rootNode));
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		final DefaultTreeCellRenderer certRenderer = new DefaultTreeCellRenderer();
		DefaultTreeCellRenderer countryAndCertRenderer = new DefaultTreeCellRenderer() {

			private JLabel label = new JLabel();
			
			private static final long serialVersionUID = 2836699659593250321L;

			public Component getTreeCellRendererComponent(JTree tree,
					Object value, boolean selected, boolean expanded,
					boolean leaf, int row, boolean hasFocus) {
				DefaultTreeCellRenderer defaultComponent = (DefaultTreeCellRenderer)certRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
				if (value instanceof DefaultMutableTreeNode) {
					if (value == rootNode) {
						label.setText(rootNode.getUserObject().toString());
						label.setIcon(CSCA_ICON);
						return label;
					}
					Object userObject = ((DefaultMutableTreeNode)value).getUserObject();
					if (userObject instanceof Country) {
						Country country = (Country)userObject;
						ImageIcon icon = new ImageIcon(Icons.getFlagImage(country));
						label.setText(country.getName());
						label.setIcon(icon);
						return label;
					} else if (userObject instanceof Certificate) {
						Certificate certificate = (Certificate)userObject;
						String certName = certificate.toString();
						if (certificate instanceof X509Certificate) {
							certName = ((X509Certificate)certificate).getIssuerX500Principal().getName();
						}
						label.setText(certName);
						label.setIcon(CERTIFICATE_ICON);
						return label;
					}
				}
				return defaultComponent;
			}	
		};
		tree.setCellRenderer(countryAndCertRenderer);

		cp.add(new JScrollPane(tree), BorderLayout.CENTER);
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

	private static Collection<Certificate> getAsCertificates(Collection<TrustAnchor> anchors) {
		Collection<Certificate> certificates = new ArrayList<Certificate>(anchors.size());
		for (TrustAnchor anchor: anchors) {
			X509Certificate certificate = anchor.getTrustedCert();
			if (certificate != null) {
				certificates.add(certificate);
			}
		}
		return certificates;
	}

	private static Country getIssuerCountry(X509Certificate certificate) {
		X500Principal issuer = certificate.getIssuerX500Principal();
		String issuerName = issuer.getName("RFC1779");
		int startIndex = issuerName.indexOf("C=");
		if (startIndex < 0) { throw new IllegalArgumentException("Could not get country from issuer name, " + issuerName); }
		int endIndex = issuerName.indexOf(",", startIndex);
		if (endIndex < 0) { endIndex = issuerName.length(); }
		String countryCode = issuerName.substring(startIndex + 2, endIndex).trim().toUpperCase();
		return ISOCountry.getInstance(countryCode);
	}
}
