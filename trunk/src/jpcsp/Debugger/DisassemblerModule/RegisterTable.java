/*
 This file is part of jpcsp.

 Jpcsp is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Jpcsp is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Jpcsp.  If not, see <http://www.gnu.org/licenses/>.
 */
package jpcsp.Debugger.DisassemblerModule;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

public class RegisterTable extends JTable {

    private static final long serialVersionUID = 1L;
    private static final Font tableFont = new Font("Courier new", Font.PLAIN, 12);

    private class Register {

        public Register(String name) {
            this.name = name;
            value = 0;
            changed = false;
        }
        public String name;
        public int value;
        public boolean changed;

        @Override
        public String toString() {
            return String.format("0x%08X", value);
        }
    };

    public RegisterTable(String[] regnames) {
        super();
        setFont(tableFont);
        setDefaultRenderer(Register.class, new RegisterValueRenderer());
        setDefaultEditor(Register.class, new RegisterValueEditor());

        setRegisters(regnames);
    }

    public RegisterTable() {
        super();
        setFont(tableFont);
        setDefaultRenderer(Register.class, new RegisterValueRenderer());
        setDefaultEditor(Register.class, new RegisterValueEditor());
    }

    final public void setRegisters(String[] regnames) {
        setModel(new RegisterTableModel(regnames));
    }

    @Override
    public void setModel(TableModel dataModel) {
        // needed to allow setting model property in NetBeans to null
        if (dataModel != null) {
            super.setModel(dataModel);
        }
    }

    public void resetChanges() {
        ((RegisterTableModel) getModel()).resetChanges();
    }

    public int getAddressAt(int rowIndex) {
        return ((Register) ((RegisterTableModel) getModel()).getValueAt(rowIndex, 1)).value;
    }

    class RegisterValueEditor extends DefaultCellEditor {

        private static final long serialVersionUID = 1L;

        public RegisterValueEditor() {
            super(new JTextField());
            final JTextField tf = ((JTextField) getComponent());
            tf.setFont(tableFont);
        }

        @Override
        public Object getCellEditorValue() {
            return ((JTextField) getComponent()).getText();
        }

        @Override
        public Component getTableCellEditorComponent(
                final JTable table, final Object value,
                final boolean isSelected, final int row, final int column) {
            final JTextField tf = ((JTextField) getComponent());
            tf.setText(String.format("0x%X", ((Register) table.getModel().getValueAt(row, column)).value));

            // needed for double-click to work, otherwise the second click
            // is interpreted to position the caret
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    // automatically select text after '0x'
                    tf.select(2, tf.getText().length());
                }
            });
            return tf;
        }
    }

    private class RegisterValueRenderer extends JLabel implements TableCellRenderer {

        private static final long serialVersionUID = 1L;

        public RegisterValueRenderer() {
            super();
            setFont(tableFont);
            setBackground(selectionBackground);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object color,
                boolean isSelected, boolean hasFocus,
                int row, int column) {

            Register reg = (Register) table.getModel().getValueAt(row, column);
            setText(String.format("0x%08X", reg.value));

            if (reg.changed) {
                setForeground(Color.RED);
                setFont(getFont().deriveFont(Font.BOLD));
            } else {
                setForeground(Color.BLACK);
                setFont(getFont().deriveFont(Font.PLAIN));
            }

            // handle selection highlight
            setOpaque(isSelected);

            return this;
        }
    }

    private class RegisterTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;
        private List<Register> reginfo;

        public RegisterTableModel(String[] regnames) {
            super();

            reginfo = new LinkedList<Register>();
            for (int i = 0; i < regnames.length; i++) {
                reginfo.add(new Register(regnames[i]));
            }
        }

        public void resetChanges() {
            Iterator<Register> it = reginfo.iterator();
            while (it.hasNext()) {
                (it.next()).changed = false;
            }
            fireTableDataChanged();
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return String.class;
                case 1:
                    return Register.class;
                default:
                    throw new IndexOutOfBoundsException("column index out of range");
            }
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "REG";
                case 1:
                    return "HEX";
                default:
                    throw new IndexOutOfBoundsException("column index out of range");
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            // only the values of the registers are editable
            return (columnIndex == 1);
        }

        @Override
        public int getColumnCount() {
            // REG, HEX
            return 2;
        }

        @Override
        public int getRowCount() {
            return reginfo.size();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return reginfo.get(rowIndex).name;
                case 1:
                    return reginfo.get(rowIndex);
                default:
                    throw new IndexOutOfBoundsException("column index out of range");
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            int value;
            if (aValue instanceof Integer) {
                value = (Integer) aValue;
            } else if (aValue instanceof String) {
                try {
                    value = Integer.decode((String) aValue);
                } catch (NumberFormatException nfe) {
                    // ignore - will revert to old value instead
                    return;
                }
            } else {
                throw new IllegalArgumentException("setValueAt() will only handle Integer and String objects");
            }

            reginfo.get(rowIndex).changed = value != reginfo.get(rowIndex).value;
            if (reginfo.get(rowIndex).changed) {
                reginfo.get(rowIndex).value = value;
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }
    }
}
