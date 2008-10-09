package sos.mrtd.sample.newgui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.smartcardio.CardTerminal;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;

import sos.mrtd.PassportEvent;
import sos.mrtd.PassportListener;
import sos.mrtd.PassportManager;
import sos.smartcards.CardEvent;
import sos.smartcards.CardManager;
import sos.smartcards.CardService;
import sos.smartcards.CardTerminalListener;
import sos.smartcards.TerminalCardService;
import sos.util.Icons;

public class PassportManagerPanel extends JPanel
{
	private static final Icon CM_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("computer"));
	private static final Icon TERMINAL_NOT_POLLING_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("drive"));
	private static final Icon TERMINAL_NO_CARD_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("drive_delete"));
	private static final Icon TERMINAL_OTHER_CARD_ICON  = new ImageIcon(Icons.getFamFamFamSilkIcon("drive_add"));
	private static final Icon TERMINAL_PASSPORT_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("drive_go"));

	JTree tree;

	List<CardTerminal> terminals;
	List<TerminalNode> terminalNodes;

	public PassportManagerPanel() {
		super(new BorderLayout());
		final CardManager cm = CardManager.getInstance();
		JPanel northPanel = new JPanel(new FlowLayout());
		JCheckBox cardManagerCheckBox = new JCheckBox();
		cardManagerCheckBox.setAction(getUseCardManagerAction());
		northPanel.add(cardManagerCheckBox);
		add(northPanel, BorderLayout.NORTH);
		terminalNodes = new ArrayList<TerminalNode>();
		terminals = cm.getTerminals();
		for (CardTerminal terminal: terminals) {
			terminalNodes.add(new TerminalNode(terminal));
		}
		tree = new JTree(buildTree(cm));
		TreeCellRenderer renderer = new DefaultTreeCellRenderer() {
			public Component getTreeCellRendererComponent(JTree tree,
					Object value, boolean selected, boolean expanded,
					boolean leaf, int row, boolean hasFocus) {

				if (value instanceof TerminalNode) {
					TerminalNode node = (TerminalNode)value;
					CardTerminal terminal = node.getTerminal();
					JLabel label = new JLabel(terminal.getName(), node.getIcon(), JLabel.LEFT);
					return label;
				} else {
					/* Must be root */
					JLabel label = new JLabel(value.toString(), CM_ICON, JLabel.LEFT);
					return label;
				}
			}
		};
		tree.setCellRenderer(renderer);
		add(new JScrollPane(tree), BorderLayout.CENTER);
		cm.addCardTerminalListener(new CardTerminalListener() {
			public void cardInserted(CardEvent ce) {
				CardService service = ce.getService();
				if (service instanceof TerminalCardService) {
					CardTerminal terminal = ((TerminalCardService)service).getTerminal();
					TerminalNode node = terminalNodes.get(terminals.indexOf(terminal));
					Icon icon = node.getIcon();
					if (icon == TERMINAL_NOT_POLLING_ICON || node.getIcon() == TERMINAL_NO_CARD_ICON) {
						node.setIcon(TERMINAL_OTHER_CARD_ICON);
					}
					revalidate();
				}
			}

			public void cardRemoved(CardEvent ce) {
				CardService service = ce.getService();
				if (service instanceof TerminalCardService) {
					CardTerminal terminal = ((TerminalCardService)service).getTerminal();
					TerminalNode node = terminalNodes.get(terminals.indexOf(terminal));
					node.setIcon(TERMINAL_NO_CARD_ICON);
					revalidate();
				}
			}			
		});
		PassportManager pm = PassportManager.getInstance();
		pm.addPassportListener(new PassportListener() {

			public void passportInserted(PassportEvent pe) {
				CardService service = pe.getService().getService();
				if (service instanceof TerminalCardService) {
					CardTerminal terminal = ((TerminalCardService)service).getTerminal();
					TerminalNode node = terminalNodes.get(terminals.indexOf(terminal));
					node.setIcon(TERMINAL_PASSPORT_ICON);
					revalidate();
				}

			}

			public void passportRemoved(PassportEvent pe) {
				// TODO Auto-generated method stub

			}	
		});
	}

	private TreeNode buildTree(CardManager cm) {
		return new DefaultMutableTreeNode("Card Manager", true) {

			public int getChildCount() {
				return terminalNodes.size();
			}

			public TreeNode getChildAt(int childIndex) {
				return terminalNodes.get(childIndex);
			}
		};
	}

	public Action getUseCardManagerAction() {
		Action action = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				Object src = e.getSource();
				if (src instanceof AbstractButton) {
					AbstractButton button = (AbstractButton)src;
					CardManager cm = CardManager.getInstance();
					if (button.isSelected()) {
						for (TerminalNode node: terminalNodes) {
							node.setIcon(TERMINAL_NO_CARD_ICON);
						}
						revalidate();
						cm.start();
					} else {
						cm.stop();
						for (TerminalNode node: terminalNodes) {
							node.setIcon(TERMINAL_NOT_POLLING_ICON);
						}
						revalidate();
					}
				}
			}
		};
		action.putValue(Action.SMALL_ICON, CM_ICON);
		action.putValue(Action.LARGE_ICON_KEY, CM_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Poll all terminals using the card manager");
		action.putValue(Action.NAME, "Use card manager");
		return action;
	}



	private class TerminalNode extends DefaultMutableTreeNode
	{
		CardTerminal terminal;
		Icon icon;

		public TerminalNode(CardTerminal terminal) {
			this.terminal = terminal;
			if (CardManager.getInstance().isPolling()) {
				this.icon = TERMINAL_NO_CARD_ICON;
			} else {
				this.icon = TERMINAL_NOT_POLLING_ICON;
			}
		}

		public void setIcon(Icon icon) {
			this.icon = icon;
		}

		public Icon getIcon() {
			return icon;
		}

		public String toString() {
			return terminal.getName();
		}

		public CardTerminal getTerminal() {
			return terminal;
		}
	}
	
	public void revalidate() {
		super.revalidate();
		if (tree != null) { tree.repaint(); }
		super.revalidate();
		repaint();
	}
}
