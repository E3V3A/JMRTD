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
 * $Id:  $
 */

package org.jmrtd.app;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.security.cert.Certificate;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.x500.X500Principal;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import net.sourceforge.scuba.data.Country;

import org.jmrtd.app.util.IconUtil;

/**
 * Frame to show the CSCA trust anchors, sorted by country.
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 */
public class CertificateMasterListFrame extends JMRTDFrame {

	private static final long serialVersionUID = -1239469067988004321L;

	private static final Icon CSCA_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("world"));
	private static final Icon CERTIFICATE_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("script_key"));

	private static final Dimension PREFERRED_SIZE = new Dimension(480, 280);

	private static final Font TREE_FONT = new Font("Sans-serif", Font.PLAIN, 12);

	private Map<Country, Collection<Certificate>> countryToCertificates;

	private DefaultMutableTreeNode rootNode;
	private ActionMap actionMap;

	/**
	 * Creates a certificate master list frame.
	 * 
	 * @param title the title to use
	 * @param anchors the certificates to show
	 */
	public CertificateMasterListFrame(String title, Set<TrustAnchor> anchors) {
		this(title, getAsCertificates(anchors));
	}

	/**
	 * Creates a certificate master list frame.
	 * 
	 * @param title the title to use
	 * @param certs the certificates to show
	 */
	public CertificateMasterListFrame(String title, Collection<Certificate> certs) {
		super(title);
		Container cp = getContentPane();
		cp.setLayout(new BorderLayout());
		countryToCertificates = new HashMap<Country, Collection<Certificate>>();
		for (Certificate certificate: certs) {
			if (!(certificate instanceof X509Certificate)) { continue; }
			Country country = getCountry(((X509Certificate)certificate).getIssuerX500Principal());
			Collection<Certificate> countryCertificates = countryToCertificates.get(country);
			if (countryCertificates == null) {
				countryCertificates = new HashSet<Certificate>();
				countryToCertificates.put(country, countryCertificates);
			}
			countryCertificates.add(certificate);
		}

		this.actionMap = new ActionMap();

		/* Menu bar */
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(createFileMenu());
		setJMenuBar(menuBar);

		rootNode = new DefaultMutableTreeNode("CSCA");

		List<Country> sortedCountryList = new ArrayList<Country>(countryToCertificates.keySet());
		Collections.sort(sortedCountryList, new Comparator<Country>() {
			public int compare(Country o1, Country o2) {
				return o1.toAlpha2Code().compareTo(o2.toAlpha2Code());
			}
		});

		for (Country country: sortedCountryList) {
			Collection<Certificate> certificates = countryToCertificates.get(country);
			DefaultMutableTreeNode countryNode = new DefaultMutableTreeNode(country);
			rootNode.add(countryNode);
			for (Certificate certificate: certificates) {
				DefaultMutableTreeNode certificateNode = new DefaultMutableTreeNode(certificate);
				countryNode.add(certificateNode);
			}
		}
		final JTree tree = new JTree(new DefaultTreeModel(rootNode));
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		CountryAndCertRenderer countryAndCertRenderer = new CountryAndCertRenderer();
		tree.setCellRenderer(countryAndCertRenderer);
		tree.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() <= 1) { return; }
		        TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
		        if (selPath == null) { return; }
		        DefaultMutableTreeNode node = (DefaultMutableTreeNode)selPath.getLastPathComponent();
				
				Object userObject = node.getUserObject();
				if (userObject instanceof Certificate) {
					JFrame certificateFrame = new CertificateChainFrame((Certificate)userObject);
					certificateFrame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
					certificateFrame.pack();
					certificateFrame.setVisible(true);
//					JOptionPane.showMessageDialog(tree, certificatePanel);
				}
			}
		});
		cp.add(new JScrollPane(tree), BorderLayout.CENTER);
	}

	/**
	 * Gets the preferred size of this frame.
	 * 
	 * @return a size
	 */
	public Dimension getPreferredSize() {
		return PREFERRED_SIZE;
	}

	/* ONLY PRIVATE METHODS BELOW */
	
	private JMenu createFileMenu() {
		JMenu fileMenu = new JMenu("File");

		/* Close */
		JMenuItem closeItem = new JMenuItem("Close");
		fileMenu.add(closeItem);
		closeItem.setAction(getCloseAction());

		return fileMenu;
	}

	private Action getCloseAction() {
		Action action = actionMap.get("Close");
		if (action != null) { return action; }
		action = new AbstractAction() {
			private static final long serialVersionUID = 5279413086163111656L;

			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		};
		action.putValue(Action.SMALL_ICON, CLOSE_ICON);
		action.putValue(Action.LARGE_ICON_KEY, CLOSE_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Close Window");
		action.putValue(Action.NAME, "Close");
		actionMap.put("Close", action);
		return action;
	}

	private static Collection<Certificate> getAsCertificates(Collection<TrustAnchor> anchors) {
		Collection<Certificate> certificates = new HashSet<Certificate>(anchors.size());
		for (TrustAnchor anchor: anchors) {
			X509Certificate certificate = anchor.getTrustedCert();
			if (certificate != null) {
				certificates.add(certificate);
			}
		}
		return certificates;
	}

	private static Country getCountry(X500Principal principal) {
		String issuerName = principal.getName("RFC1779");
		int startIndex = issuerName.indexOf("C=");
		if (startIndex < 0) { throw new IllegalArgumentException("Could not get country from issuer name, " + issuerName); }
		int endIndex = issuerName.indexOf(",", startIndex);
		if (endIndex < 0) { endIndex = issuerName.length(); }
		String countryCode = issuerName.substring(startIndex + 2, endIndex).trim().toUpperCase();
		return Country.getInstance(countryCode);
	}

	private class CountryAndCertRenderer extends DefaultTreeCellRenderer {

		private static final long serialVersionUID = 2836699659593250321L;

		private DefaultTreeCellRenderer defaultRenderer;
		private JLabel label;

		public CountryAndCertRenderer() {
			defaultRenderer = new DefaultTreeCellRenderer();
			label = new JLabel();
			label.setFont(TREE_FONT);
			label.setOpaque(true);
		}

		public Component getTreeCellRendererComponent(JTree tree,
				Object value, boolean selected, boolean expanded,
				boolean leaf, int row, boolean hasFocus) {
			DefaultTreeCellRenderer defaultComponent = (DefaultTreeCellRenderer)defaultRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
			if (value instanceof DefaultMutableTreeNode) {
				if (value == rootNode) {
					label.setText(rootNode.getUserObject().toString());
					label.setIcon(CSCA_ICON);
					setLabelSelected(selected);
					return label;
				}
				Object userObject = ((DefaultMutableTreeNode)value).getUserObject();
				if (userObject instanceof Country) {
					Country country = (Country)userObject;
					ImageIcon icon = new ImageIcon(IconUtil.getFlagImage(country));
					label.setText(country.toAlpha2Code() + " " + country.getName());
					label.setIcon(icon);
					setLabelSelected(selected);
					return label;
				} else if (userObject instanceof Certificate) {
					Certificate certificate = (Certificate)userObject;
					String certName = certificate.toString();
					if (certificate instanceof X509Certificate) {
						X509Certificate x509Cert = (X509Certificate)certificate;
						certName = x509Cert.getIssuerX500Principal().getName() + " (" + x509Cert.getSerialNumber() + ")";
					}
					label.setFont(TREE_FONT);
					label.setText(certName);
					label.setIcon(CERTIFICATE_ICON);
					setLabelSelected(selected);
					return label;
				}
			}
			return defaultComponent;
		}

		private void setLabelSelected(boolean selected) {
			if (selected) {
				label.setForeground(defaultRenderer.getTextSelectionColor());
				label.setBackground(defaultRenderer.getBackgroundSelectionColor());
			} else {
				label.setForeground(defaultRenderer.getTextNonSelectionColor());
				label.setBackground(defaultRenderer.getBackgroundNonSelectionColor());
			}
		}
	}
}

