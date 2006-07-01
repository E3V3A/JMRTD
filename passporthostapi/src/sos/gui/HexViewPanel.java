/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006  SoS group, Radboud University
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
 * $Id: HexViewPanel.java,v 1.7 2006/06/08 14:52:30 martijno Exp $
 */

package sos.gui;

import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import sos.util.Hex;

/**
 * Component for showing byte arrays.
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision: 1.7 $
 */
public class HexViewPanel extends JPanel
{
   private static final Font FONT = new Font("Monospaced",Font.PLAIN,10);
   private static final int COLUMNS = 16;
   private static final int CHAR_WIDTH = 12;

   private JTable table;

   private DefaultTableColumnModel columnModel;
   private HexViewPanelDataModel dataModel;

   /**
    * Constructs a hex view panel.
    *
    * @param data the data to view.
    */
   public HexViewPanel(byte[] data) {
      super(new FlowLayout());
      columnModel = new DefaultTableColumnModel();
      dataModel = new HexViewPanelDataModel(data);
      table = new JTable();
      table.setColumnModel(columnModel);
      table.setModel(dataModel);
      TableColumn column = columnModel.getColumn(COLUMNS + 1);
      column.setPreferredWidth(COLUMNS * CHAR_WIDTH);
      for (int i = COLUMNS; i > 0; i--) {
         column = columnModel.getColumn(i);
         column.setPreferredWidth(2 * CHAR_WIDTH);
      }
      column = columnModel.getColumn(0);
      column.setPreferredWidth(8 * CHAR_WIDTH);
      add(table);
   }

   /**
    * The font used.
    *
    * @return the font used.
    */
   public Font getFont() {
      return FONT;
   }

   private class HexViewPanelDataModel extends DefaultTableModel
   {
      private byte[][] rows;

      public HexViewPanelDataModel(byte[] data) {
         super();
         rows = Hex.split(data, COLUMNS);
      }

      public int getRowCount() {
         return rows == null ? 0 : rows.length;
      }

      public int getColumnCount() {
         return 1 + COLUMNS + 1;
      }

      public String getColumnName(int col) {
         switch (col) {
            case 0:
               return "Index";
            case COLUMNS + 1:
               return "ASCII";
            default:
               return Hex.byteToHexString((byte)(col - 1));
         }
      }

      public Object getValueAt(int row, int col) {
         switch (col) {
            case 0:
               return Hex.intToHexString(COLUMNS * row);
            case COLUMNS + 1:
               return Hex.bytesToASCIIString(rows[row]);
            default:
               if (row < 0 || row >= rows.length) {
                  return null;
               } else if ((col - 1) < 0 || (col - 1) >= rows[row].length) {
                  return null;
               } else {
                  return Hex.byteToHexString(rows[row][col - 1]);
               }
         }
      }

      public boolean isCellEditable(int row, int col) {
         return false;
      }
   }
}

