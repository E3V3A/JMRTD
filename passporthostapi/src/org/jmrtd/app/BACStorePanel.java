/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2008  The JMRTD team
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

package org.jmrtd.app;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;

import sos.util.Icons;

/**
 * Panel for managing the BAC store.
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 *
 * @version $Revision: 308 $
 */
public class BACStorePanel extends JPanel
{
	private static final long serialVersionUID = 8209327475448864084L;
	
	private static final int DOCUMENT_NUMBER_COLUMN = 0;
	private static final int DATE_OF_BIRTH_COLUMN = 1;
	private static final int DATE_OF_EXPIRY_COLUMN = 2;

	private static final Icon DOWN_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("arrow_down"));
	private static final Icon DOWN_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("arrow_down"));
	private static final Icon UP_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("arrow_up"));
	private static final Icon UP_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("arrow_up"));
	private static final Icon TABLE_ROW_DELETE_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("table_row_delete"));
	private static final Icon TABLE_ROW_DELETE_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("table_row_delete"));
	private static final Icon TABLE_ROW_INSERT_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("table_row_insert"));
	private static final Icon TABLE_ROW_INSERT_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("table_row_insert"));

	private static final SimpleDateFormat SDF = new SimpleDateFormat("dd MMM yyyy");

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
		this.store = store;

		moveUpAction = getMoveUpAction();
		moveDownAction = getMoveDownAction();
		deleteAction = getDeleteAction();
		addAction = getAddAction();

		table = new BACStoreTable();
		entryField = new BACEntryField();
		add(new JScrollPane(table), BorderLayout.CENTER);

		JToolBar toolBar = new JToolBar();
		JButton upButton = new JButton();
		toolBar.add(upButton);

		JButton downButton = new JButton();
		toolBar.add(downButton);

		toolBar.addSeparator();

		JButton addButton = new JButton();
		toolBar.add(addButton);

		JButton deleteButton = new JButton();
		toolBar.add(deleteButton);

		upButton.setAction(moveUpAction);
		downButton.setAction(moveDownAction);
		addButton.setAction(addAction);
		deleteButton.setAction(deleteAction);
		add(toolBar, BorderLayout.NORTH);
	}

	public void addKeyListener(KeyListener l) {
		super.addKeyListener(l);
		table.addKeyListener(l);
	}

	public void addEntry(BACEntry entry) {
		store.addEntry(entry);
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

				BACEntry entry = new BACEntry(documentNumber, dateOfBirth, dateOfExpiry);

				store.removeEntry(editingRow);
				store.addEntry(editingRow, entry);
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
			List<BACEntry> entries = store.getEntries();
			BACEntry entry = entries.get(rowIndex);
			switch(columnIndex) {
			case 0: return entry.getDocumentNumber();
			case 1: return SDF.format(entry.getDateOfBirth());
			case 2: return SDF.format(entry.getDateOfExpiry());
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
	private Action getAddAction() {
		Action action = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				int choice = JOptionPane.showOptionDialog(table, entryField, "Enter BAC",
						JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
						null, null, null);
				if (choice == JOptionPane.OK_OPTION) {
					String documentNumber = entryField.getDocumentNumber();
					Date dateOfBirth = entryField.getDateOfBirth();
					Date dateOfExpiry = entryField.getDateOfExpiry();
					addEntry(new BACEntry(documentNumber, dateOfBirth, dateOfExpiry));
				}
			}
		};
		action.putValue(Action.SMALL_ICON, TABLE_ROW_INSERT_SMALL_ICON);
		action.putValue(Action.LARGE_ICON_KEY, TABLE_ROW_INSERT_LARGE_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Add BAC Entry");
		action.putValue(Action.NAME, "Add");
		return action;
	}

	/**
	 * Delete the selected entry from the store (and thus from the table).
	 */
	private Action getDeleteAction() {
		Action action = new AbstractAction() {
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
		return action;
	}

	/**
	 * Delete the selected entry from the store (and thus from the table).
	 */
	private Action getMoveUpAction() {
		Action action = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				try {
					int entryRowIndex = table.getSelectedRow();
					if (entryRowIndex <= 0) { return; }
					BACEntry entry = store.getEntry(entryRowIndex);
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
		return action;
	}

	private Action getMoveDownAction() {
		Action action = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				try {
					int entryRowIndex = table.getSelectedRow();
					if (entryRowIndex >= table.getRowCount()) { return; }
					BACEntry entry = store.getEntry(entryRowIndex);
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
		return action;
	}
}
