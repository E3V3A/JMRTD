
package sos.mrtd.sample.newgui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

import sos.util.Icons;

/**
 * GUI for managing the BAC store.
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision: 308 $
 */
public class BACStorePanel extends JPanel
{  
	private static final SimpleDateFormat SDF =
		new SimpleDateFormat("yyMMdd");
	
	private static final Icon DOWN_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("arrow_down"));
	private static final Icon DOWN_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("arrow_down"));
	private static final Icon UP_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("arrow_up"));
	private static final Icon UP_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("arrow_up"));
	private static final Icon TABLE_ROW_DELETE_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("table_row_delete"));
	private static final Icon TABLE_ROW_DELETE_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("table_row_delete"));
	private static final Icon TABLE_ROW_INSERT_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("table_row_insert"));
	private static final Icon TABLE_ROW_INSERT_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("table_row_insert"));
	
	private final BACStore store;
	private BACStoreTable table;
	private BACEntryField entryField;
	
	private Action addAction, deleteAction, moveUpAction, moveDownAction;

	/**
	 * Constructs the GUI.
	 *
	 * @param arg command line arguments, are ignored for now.
	 */
	public BACStorePanel(BACStore store)  {
		super(new BorderLayout());
		this.store =  store;
		
		moveUpAction = new MoveUpAction();
		moveDownAction = new MoveDownAction();
		deleteAction = new DeleteAction();
		addAction = new AddAction();
		
		table = new BACStoreTable();
		entryField = new BACEntryField();
		entryField.setAction(addAction);
		add(new JScrollPane(table), BorderLayout.CENTER);
		add(entryField, BorderLayout.SOUTH);

		JToolBar toolBar = new JToolBar();
		JButton upButton = new JButton();
		toolBar.add(upButton);
		
		JButton downButton = new JButton();
		toolBar.add(downButton);
		
		toolBar.addSeparator();
		
		JButton deleteButton = new JButton();
		toolBar.add(deleteButton);
				
		upButton.setAction(moveUpAction);
		downButton.setAction(moveDownAction);
		deleteButton.setAction(deleteAction);
		add(toolBar, BorderLayout.NORTH);
	}

	private class BACStoreTable extends JTable
	{
		public BACStoreTable() {
			super();
			setModel(new BACStoreTableModel());
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		}
	}

	private class BACStoreTableModel extends AbstractTableModel
	{
		public BACStoreTableModel() { }

		public int getColumnCount() {
			return 3;
		}

		public String getColumnName(int columnIndex) {
			switch(columnIndex) {
			case 0: return "Document Nr.";
			case 1: return "Date of Birth";
			case 2: return "Date of Expiry";
			default: return null;
			}
		}

		public int getRowCount() {
			return store.getEntries().size();
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			List<BACStore.BACStoreEntry> entries = store.getEntries();
			BACStore.BACStoreEntry entry = entries.get(rowIndex);
			switch(columnIndex) {
			case 0: return entry.getDocumentNumber();
			case 1: return entry.getDateOfBirth();
			case 2: return entry.getDateOfExpiry();
			default: return null;
			}
		}

		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return false;
		}
	}

	/**
	 * Add the entry in the entryField to the store (and thus to the table).
	 */
	private class AddAction extends AbstractAction
	{
		public AddAction() {
			putValue(SMALL_ICON, TABLE_ROW_INSERT_SMALL_ICON);
			putValue(LARGE_ICON_KEY, TABLE_ROW_INSERT_LARGE_ICON);
			putValue(SHORT_DESCRIPTION, "Add BAC Entry");
			putValue(NAME, "Add");
		}
		
		public void actionPerformed(ActionEvent e) {
			String documentNumber = entryField.getDocumentNumber();
			Date dateOfBirth = entryField.getDateOfBirth();
			Date dateOfExpiry = entryField.getDateOfExpiry();
			store.addEntry(documentNumber, SDF.format(dateOfBirth), SDF.format(dateOfExpiry));
			table.revalidate();
		} 
	}
	
	/**
	 * Delete the selected entry from the store (and thus from the table).
	 */
	private class DeleteAction extends AbstractAction
	{
		public DeleteAction() {
			putValue(SMALL_ICON, TABLE_ROW_DELETE_SMALL_ICON);
			putValue(LARGE_ICON_KEY, TABLE_ROW_DELETE_LARGE_ICON);
			putValue(SHORT_DESCRIPTION, "Delete BAC Entry");
			putValue(NAME, "Delete");
		}
		
		public void actionPerformed(ActionEvent e) {
			try {
				int entryRowIndex = table.getSelectedRow();
				store.removeEntry(entryRowIndex);
				table.revalidate();
			} catch (IndexOutOfBoundsException ioobe) {
				/* NOTE: Nothing selected, do nothing. */
			}
		}
	}
	
	/**
	 * Delete the selected entry from the store (and thus from the table).
	 */
	private class MoveUpAction extends AbstractAction
	{
		public MoveUpAction() {
			putValue(SMALL_ICON, UP_SMALL_ICON);
			putValue(LARGE_ICON_KEY, UP_LARGE_ICON);
			putValue(SHORT_DESCRIPTION, "Move BAC Entry Up");
			putValue(NAME, "Up");
		}
		
		public void actionPerformed(ActionEvent e) {
			try {
				int entryRowIndex = table.getSelectedRow();
				if (entryRowIndex <= 0) { return; }
				BACStore.BACStoreEntry entry = store.getEntry(entryRowIndex);
				store.removeEntry(entryRowIndex);
				store.addEntry(entryRowIndex - 1, entry);
				table.removeRowSelectionInterval(entryRowIndex - 1, entryRowIndex);
				table.revalidate();
			} catch (IndexOutOfBoundsException ioobe) {
				/* NOTE: Nothing selected, do nothing. */
			}
		}
	}
	
	private class MoveDownAction extends AbstractAction
	{
		public MoveDownAction() {
			putValue(SMALL_ICON, DOWN_SMALL_ICON);
			putValue(LARGE_ICON_KEY, DOWN_LARGE_ICON);
			putValue(SHORT_DESCRIPTION, "Move BAC Entry Down");
			putValue(NAME, "Down");
		}
		
		public void actionPerformed(ActionEvent e) {
			try {
				int entryRowIndex = table.getSelectedRow();
				if (entryRowIndex >= table.getRowCount()) { return; }
				BACStore.BACStoreEntry entry = store.getEntry(entryRowIndex);
				store.removeEntry(entryRowIndex);
				store.addEntry(entryRowIndex + 1, entry);
				table.removeRowSelectionInterval(entryRowIndex, entryRowIndex + 1);
				table.revalidate();
			} catch (IndexOutOfBoundsException ioobe) {
				/* NOTE: Nothing selected, do nothing. */
			}
		}
	}
	
	public JMenu getBACMenu() {
		JMenu result = new JMenu("BACs");
		JMenuItem addItem = new JMenuItem();
		addItem.setAction(addAction);
		result.add(addItem);
		JMenuItem deleteItem = new JMenuItem();
		deleteItem.setAction(deleteAction);
		result.add(deleteItem);
		result.addSeparator();
		JMenuItem moveUpItem = new JMenuItem();
		moveUpItem.setAction(moveUpAction);
		result.add(moveUpItem);
		JMenuItem moveDownItem = new JMenuItem();
		moveDownItem.setAction(moveDownAction);
		result.add(moveDownItem);
		return result;
	}
}

