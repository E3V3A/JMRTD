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
 * $Id: BACStorePanel.java 893 2009-03-23 15:43:42Z martijno $
 */

package org.jmrtd.app;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;

import org.jmrtd.BACKey;
import org.jmrtd.BACKeySpec;
import org.jmrtd.app.util.IconUtil;

/**
 * Panel for managing the BAC store.
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 *
 * @version $Revision: 893 $
 */
public class BACStorePanel extends JPanel {

	private static final long serialVersionUID = 8209327475448864084L;

	private static final Logger LOGGER = Logger.getLogger("org.jmrtd");
	
	private static final int DOCUMENT_NUMBER_COLUMN = 0;
	private static final int DATE_OF_BIRTH_COLUMN = 1;
	private static final int DATE_OF_EXPIRY_COLUMN = 2;

	private static final Icon DOWN_SMALL_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("arrow_down"));
	private static final Icon DOWN_LARGE_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("arrow_down"));
	private static final Icon UP_SMALL_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("arrow_up"));
	private static final Icon UP_LARGE_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("arrow_up"));
	private static final Icon TABLE_ROW_DELETE_SMALL_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("table_row_delete"));
	private static final Icon TABLE_ROW_DELETE_LARGE_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("table_row_delete"));
	private static final Icon TABLE_ROW_INSERT_SMALL_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("table_row_insert"));
	private static final Icon TABLE_ROW_INSERT_LARGE_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("table_row_insert"));

	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyMMdd");

	private ActionMap actionMap;

	private final MutableBACStore store;
	private BACStoreTable table;
	private BACEntryField entryField;

	// private Action addAction, deleteAction, moveUpAction, moveDownAction;

	/**
	 * Constructs the GUI.
	 *
	 * @param arg command line arguments, are ignored for now.
	 */
	public BACStorePanel(MutableBACStore store)  {
		super(new BorderLayout());
		this.store = store;
		actionMap = new ActionMap();

		table = new BACStoreTable();
		entryField = new BACEntryField();
		add(new JScrollPane(table), BorderLayout.CENTER);

		Action moveUpAction = getMoveUpAction(),
		moveDownAction = getMoveDownAction(),
		deleteAction = getDeleteAction(),
		addAction = getAddAction();

		JToolBar toolBar = new JToolBar();
		toolBar.add(moveUpAction);
		toolBar.add(moveDownAction);
		toolBar.addSeparator();
		toolBar.add(addAction);
		toolBar.add(deleteAction);
		add(toolBar, BorderLayout.NORTH);
	}

	/**
	 * Adds a key listener (typed MRZ fragments lead to entering BAC entry)
	 * 
	 * @param l a key listener
	 */
	public void addKeyListener(KeyListener l) {
		super.addKeyListener(l);
		table.addKeyListener(l);
	}

	/**
	 * Adds a BAC entry.
	 * 
	 * @param bacKeySpec a BAC key spec
	 */
	public void addEntry(BACKey bacKeySpec) {
		store.addEntry(bacKeySpec);
		table.revalidate();
	}

	private class BACStoreTable extends JTable
	{
		private static final long serialVersionUID = -6213600681715225467L;

		public BACStoreTable() {
			super();
			setModel(new BACStoreTableModel());
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		}

		public void editingStopped(ChangeEvent e) {
			try {
				/* Old value */
				String documentNumber = (String)getValueAt(editingRow, DOCUMENT_NUMBER_COLUMN);
				Date dateOfBirth = SDF.parse((String)getValueAt(editingRow, DATE_OF_BIRTH_COLUMN));
				Date dateOfExpiry = SDF.parse((String)getValueAt(editingRow, DATE_OF_EXPIRY_COLUMN));

				/* New value */
				TableCellEditor editor = (TableCellEditor)e.getSource();
				String changedValue = (String)editor.getCellEditorValue();
				switch (editingColumn) {
				case DOCUMENT_NUMBER_COLUMN: documentNumber = changedValue.trim().toUpperCase(); break;
				case DATE_OF_BIRTH_COLUMN: dateOfBirth = SDF.parse(changedValue); break;
				case DATE_OF_EXPIRY_COLUMN: dateOfExpiry = SDF.parse(changedValue); break;
				}

				BACKey entry = new BACKey(documentNumber, dateOfBirth, dateOfExpiry);

				store.removeEntry(editingRow);
				store.addEntry(editingRow, entry);
				super.editingStopped(e);
			} catch (NumberFormatException nfe) {
				nfe.printStackTrace();
			} catch (ParseException pe) {
				pe.printStackTrace();
			} finally {
				validate(); repaint();
				super.editingStopped(e);
			}
		}
	}

	private class BACStoreTableModel extends AbstractTableModel
	{
		private static final long serialVersionUID = 2891824258471617L;

		public BACStoreTableModel() { }

		public int getColumnCount() {
			return 3;
		}

		public String getColumnName(int columnIndex) {
			switch(columnIndex) {
			case DOCUMENT_NUMBER_COLUMN: return "Document Nr.";
			case DATE_OF_BIRTH_COLUMN: return "Date of Birth";
			case DATE_OF_EXPIRY_COLUMN: return "Date of Expiry";
			default: return null;
			}
		}

		public int getRowCount() {
			return store.getEntries().size();
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			List<BACKeySpec> entries = store.getEntries();
			BACKeySpec entry = entries.get(rowIndex);
			switch(columnIndex) {
			case 0: return entry.getDocumentNumber();
			case 1: return entry.getDateOfBirth();
			case 2: return entry.getDateOfExpiry();
			default: return null;
			}
		}

		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return true;
		}
	}

	/**
	 * Add the entry in the entryField to the store (and thus to the table).
	 */
	/* package visible */ Action getAddAction() {
		Action action = actionMap.get("Add");
		if (action != null) { return action; }
		action = new AbstractAction() {

			private static final long serialVersionUID = -7053795260898666446L;

			public void actionPerformed(ActionEvent ae) {
				int choice = JOptionPane.showOptionDialog(table, entryField, "Enter BAC",
						JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
						null, null, null);
				if (choice == JOptionPane.OK_OPTION) {
					BACKey bacKeySpec = null;
					try {
						String documentNumber = entryField.getDocumentNumber();
						Date dateOfBirth = entryField.getDateOfBirth();
						Date dateOfExpiry = entryField.getDateOfExpiry();
						bacKeySpec = new BACKey(documentNumber, dateOfBirth, dateOfExpiry);
					} catch (Exception e) {
						/* FIXME: something is wrong with the entered BAC key, tell user in popup dialog. */
						LOGGER.warning("Failed to add BAC entry");
						e.printStackTrace();
					}
					if (bacKeySpec != null) { addEntry(bacKeySpec); }
				}
			}
		};
		action.putValue(Action.SMALL_ICON, TABLE_ROW_INSERT_SMALL_ICON);
		action.putValue(Action.LARGE_ICON_KEY, TABLE_ROW_INSERT_LARGE_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Add BAC Entry");
		action.putValue(Action.NAME, "Add");
		actionMap.put("Add", action);
		return action;
	}

	/**
	 * Delete the selected entry from the store (and thus from the table).
	 */
	private Action getDeleteAction() {
		Action action = actionMap.get("Delete");
		if (action != null) { return action; }
		action = new AbstractAction() {

			private static final long serialVersionUID = 8299630020852663643L;

			public void actionPerformed(ActionEvent e) {
				try {
					int entryRowIndex = table.getSelectedRow();
					store.removeEntry(entryRowIndex);
					table.revalidate();
				} catch (IndexOutOfBoundsException ioobe) {
					/* NOTE: Nothing selected, do nothing. */
				}
			}			
		};
		action.putValue(Action.SMALL_ICON, TABLE_ROW_DELETE_SMALL_ICON);
		action.putValue(Action.LARGE_ICON_KEY, TABLE_ROW_DELETE_LARGE_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Delete BAC Entry");
		action.putValue(Action.NAME, "Delete");
		actionMap.put("Delete", action);
		return action;
	}

	private Action getMoveUpAction() {
		Action action = actionMap.get("MoveUp");
		if (action != null) { return action; }
		action = new AbstractAction() {

			private static final long serialVersionUID = -4661297725569163924L;

			public void actionPerformed(ActionEvent e) {
				try {
					int entryRowIndex = table.getSelectedRow();
					if (entryRowIndex <= 0) { return; }
					BACKeySpec entry = store.getEntries().get(entryRowIndex);
					store.removeEntry(entryRowIndex);
					store.addEntry(entryRowIndex - 1, entry);
					table.removeRowSelectionInterval(entryRowIndex - 1, entryRowIndex);
					table.revalidate();
				} catch (IndexOutOfBoundsException ioobe) {
					/* NOTE: Nothing selected, do nothing. */
				}
			}
		};
		action.putValue(Action.SMALL_ICON, UP_SMALL_ICON);
		action.putValue(Action.LARGE_ICON_KEY, UP_LARGE_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Move BAC Entry Up");
		action.putValue(Action.NAME, "Up");
		actionMap.put("MoveUp", action);
		return action;
	}

	private Action getMoveDownAction() {
		Action action = actionMap.get("MoveDown");
		if (action != null) { return action; }
		action = new AbstractAction() {

			private static final long serialVersionUID = -4995808660508497602L;

			public void actionPerformed(ActionEvent e) {
				try {
					int entryRowIndex = table.getSelectedRow();
					if (entryRowIndex >= table.getRowCount()) { return; }
					BACKeySpec entry = store.getEntries().get(entryRowIndex);
					store.removeEntry(entryRowIndex);
					store.addEntry(entryRowIndex + 1, entry);
					table.removeRowSelectionInterval(entryRowIndex, entryRowIndex + 1);
					table.revalidate();
				} catch (IndexOutOfBoundsException ioobe) {
					/* NOTE: Nothing selected, do nothing. */
				}
			}
		};
		action.putValue(Action.SMALL_ICON, DOWN_SMALL_ICON);
		action.putValue(Action.LARGE_ICON_KEY, DOWN_LARGE_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Move BAC Entry Down");
		action.putValue(Action.NAME, "Down");
		actionMap.put("MoveDown", action);
		return action;
	}
}
