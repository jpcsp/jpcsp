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
package jpcsp.Debugger.MemoryBreakpoints;

import java.awt.Component;
import java.awt.Font;
import java.io.File;
import java.util.List;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import jpcsp.Memory;
import jpcsp.State;
import jpcsp.memory.DebuggerMemory;
import jpcsp.Debugger.MemoryBreakpoints.MemoryBreakpoint.AccessType;
import jpcsp.util.Constants;

public class MemoryBreakpointsDialog extends javax.swing.JDialog {
    
    private List<MemoryBreakpoint> memoryBreakpoints;
    private MemoryBreakpointsModel memoryBreakpointsModel;
    private final int COL_STARTADDRESS = 0;
    private final int COL_ENDADDRESS = 1;
    private final int COL_ACCESSTYPE = 2;
    private final int COL_ACTIVE = 3;
    private final int COL_LAST = 4;
    private static final Font tableFont = new Font("Courier new", Font.PLAIN, 12);
    
    public MemoryBreakpointsDialog(java.awt.Frame parent) {
        super(parent);
        
        memoryBreakpoints = ((DebuggerMemory) Memory.getInstance()).getMemoryBreakpoints();
        memoryBreakpointsModel = new MemoryBreakpointsModel();
        
        initComponents();
        
        TableColumn accessType = tblBreakpoints.getColumnModel().getColumn(COL_ACCESSTYPE);
        JComboBox combo = new JComboBox();
        combo.addItem("READ");
        combo.addItem("WRITE");
        combo.addItem("READWRITE");
        accessType.setCellEditor(new DefaultCellEditor(combo));
        
        tblBreakpoints.getColumnModel().getColumn(COL_STARTADDRESS).setCellEditor(new AddressCellEditor());
        tblBreakpoints.getColumnModel().getColumn(COL_ENDADDRESS).setCellEditor(new AddressCellEditor());
        
        tblBreakpoints.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                btnRemove.setEnabled(!((ListSelectionModel) e.getSource()).isSelectionEmpty());
            }
        });
        
        tblBreakpoints.getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent tme) {
                MemoryBreakpointsModel mbpm = (MemoryBreakpointsModel) tme.getSource();
                btnExport.setEnabled(mbpm.getRowCount() > 0);

                // validate entered addresses
                if (tme.getColumn() == COL_STARTADDRESS || tme.getColumn() == COL_ENDADDRESS) {
                    for (int i = tme.getFirstRow(); i <= tme.getLastRow(); i++) {
                        int start = Integer.decode(mbpm.getValueAt(i, COL_STARTADDRESS).toString());
                        int end = Integer.decode(mbpm.getValueAt(i, COL_ENDADDRESS).toString());
                        
                        if (tme.getColumn() == COL_STARTADDRESS && start > end) {
                            mbpm.setValueAt(new Integer(start), i, COL_ENDADDRESS);
                        }
                        if (tme.getColumn() == COL_ENDADDRESS && end < start) {
                            mbpm.setValueAt(new Integer(end), i, COL_STARTADDRESS);
                        }
                    }
                }
            }
        });

        // copy trace settings to UI
        updateTraceSettings();
    }
    
    private void updateTraceSettings() {
        DebuggerMemory dbgmem = ((DebuggerMemory) Memory.getInstance());
        
        cbTraceRead.setSelected(dbgmem.traceMemoryRead);
        cbTraceRead8.setSelected(dbgmem.traceMemoryRead8);
        cbTraceRead16.setSelected(dbgmem.traceMemoryRead16);
        cbTraceRead32.setSelected(dbgmem.traceMemoryRead32);
        
        cbTraceWrite.setSelected(dbgmem.traceMemoryWrite);
        cbTraceWrite8.setSelected(dbgmem.traceMemoryWrite8);
        cbTraceWrite16.setSelected(dbgmem.traceMemoryWrite16);
        cbTraceWrite32.setSelected(dbgmem.traceMemoryWrite32);
        
        chkPauseOnHit.setSelected(dbgmem.pauseEmulatorOnMemoryBreakpoint);
    }
    
    private class AddressCellEditor extends DefaultCellEditor {
        
        private static final long serialVersionUID = 1L;
        
        public AddressCellEditor() {
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
            tf.setText(String.format("0x%X", Integer.decode((String) table.getModel().getValueAt(row, column))));

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
    
    private class MemoryBreakpointsModel extends AbstractTableModel {
        
        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            MemoryBreakpoint mbp = memoryBreakpoints.get(rowIndex);
            switch (columnIndex) {
                case COL_STARTADDRESS:
                case COL_ENDADDRESS:
                    int address = 0;
                    if (aValue instanceof String) {
                        try {
                            address = Integer.decode((String) aValue);
                        } catch (NumberFormatException nfe) {
                            // do nothing - cell will revert to previous value
                            return;
                        }
                    } else if (aValue instanceof Integer) {
                        address = ((Integer) aValue).intValue();
                    } else {
                        throw new IllegalArgumentException("only String or Integer values allowed");
                    }
                    
                    if (columnIndex == COL_STARTADDRESS) {
                        mbp.setStartAddress(address);
                    } else if (columnIndex == COL_ENDADDRESS) {
                        mbp.setEndAddress(address);
                    }
                    break;
                case COL_ACCESSTYPE:
                    String value = ((String) aValue).toUpperCase();
                    if (value.equals("READ")) {
                        mbp.setAccess(AccessType.READ);
                    } else if (value.equals("WRITE")) {
                        mbp.setAccess(AccessType.WRITE);
                    } else if (value.equals("READWRITE")) {
                        mbp.setAccess(AccessType.READWRITE);
                    }
                    break;
                case COL_ACTIVE:
                    // TODO check if ranges overlap and prevent update
                    mbp.setEnabled((Boolean) aValue);
                    break;
                default:
                    throw new IllegalArgumentException("column out of range: " + columnIndex);
            }
            fireTableCellUpdated(rowIndex, columnIndex);
        }
        
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            // all cells are editable
            return true;
        }
        
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case COL_STARTADDRESS:
                case COL_ENDADDRESS:
                case COL_ACCESSTYPE:
                    return String.class;
                case COL_ACTIVE:
                    return Boolean.class;
                default:
                    throw new IllegalArgumentException("column out of range: " + columnIndex);
            }
        }
        
        @Override
        public String getColumnName(int column) {
            java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("jpcsp/languages/jpcsp"); // NOI18N
            switch (column) {
                case COL_STARTADDRESS:
                    return bundle.getString("MemoryBreakpointsDialog.strStartAddress.text");
                case COL_ENDADDRESS:
                    return bundle.getString("MemoryBreakpointsDialog.strEndAddress.text");
                case COL_ACCESSTYPE:
                    return bundle.getString("MemoryBreakpointsDialog.strAccess.text");
                case COL_ACTIVE:
                    return bundle.getString("MemoryBreakpointsDialog.strActive.text");
                default:
                    throw new IllegalArgumentException("column out of range: " + column);
            }
        }
        
        @Override
        public int getRowCount() {
            return memoryBreakpoints.size();
        }
        
        @Override
        public int getColumnCount() {
            return COL_LAST;
        }
        
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            MemoryBreakpoint mbp = memoryBreakpoints.get(rowIndex);
            switch (columnIndex) {
                case COL_STARTADDRESS:
                    return String.format("0x%08X", mbp.getStartAddress());
                case COL_ENDADDRESS:
                    return String.format("0x%08X", mbp.getEndAddress());
                case COL_ACCESSTYPE:
                    switch (mbp.getAccess()) {
                        case READ:
                            return "READ";
                        case WRITE:
                            return "WRITE";
                        case READWRITE:
                            return "READWRITE";
                        default:
                            throw new IllegalArgumentException("unknown access type");
                    }
                case COL_ACTIVE:
                    return (mbp.isEnabled()) ? Boolean.TRUE : Boolean.FALSE;
                default:
                    throw new IllegalArgumentException("column out of range: " + columnIndex);
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        btnAdd = new javax.swing.JButton();
        btnRemove = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jSeparator1 = new javax.swing.JSeparator();
        jPanel1 = new javax.swing.JPanel();
        cbTraceRead = new javax.swing.JCheckBox();
        cbTraceWrite = new javax.swing.JCheckBox();
        cbTraceRead8 = new javax.swing.JCheckBox();
        cbTraceWrite8 = new javax.swing.JCheckBox();
        cbTraceRead16 = new javax.swing.JCheckBox();
        cbTraceWrite16 = new javax.swing.JCheckBox();
        cbTraceRead32 = new javax.swing.JCheckBox();
        cbTraceWrite32 = new javax.swing.JCheckBox();
        chkPauseOnHit = new javax.swing.JCheckBox();
        btnClose = new javax.swing.JButton();
        btnExport = new javax.swing.JButton();
        btnImport = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        tblBreakpoints = new javax.swing.JTable();

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("jpcsp/languages/jpcsp"); // NOI18N
        setTitle(bundle.getString("MemoryBreakpointsDialog.title")); // NOI18N
        setLocationByPlatform(true);
        setName("dialog"); // NOI18N

        btnAdd.setText(bundle.getString("MemoryBreakpointsDialog.btnAdd.text")); // NOI18N
        btnAdd.setMaximumSize(new java.awt.Dimension(140, 25));
        btnAdd.setMinimumSize(new java.awt.Dimension(140, 25));
        btnAdd.setPreferredSize(new java.awt.Dimension(140, 25));
        btnAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddActionPerformed(evt);
            }
        });

        btnRemove.setText(bundle.getString("MemoryBreakpointsDialog.btnRemove.text")); // NOI18N
        btnRemove.setEnabled(false);
        btnRemove.setMaximumSize(new java.awt.Dimension(140, 25));
        btnRemove.setMinimumSize(new java.awt.Dimension(140, 25));
        btnRemove.setPreferredSize(new java.awt.Dimension(140, 25));
        btnRemove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRemoveActionPerformed(evt);
            }
        });

        jPanel1.setLayout(new java.awt.GridLayout(5, 2));

        cbTraceRead.setText(bundle.getString("MemoryBreakpointsDialog.cbTraceRead.text")); // NOI18N
        cbTraceRead.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cbTraceReadItemStateChanged(evt);
            }
        });
        jPanel1.add(cbTraceRead);

        cbTraceWrite.setText(bundle.getString("MemoryBreakpointsDialog.cbTraceWrite.text")); // NOI18N
        cbTraceWrite.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cbTraceWriteItemStateChanged(evt);
            }
        });
        jPanel1.add(cbTraceWrite);

        cbTraceRead8.setText(bundle.getString("MemoryBreakpointsDialog.cbTraceRead8.text")); // NOI18N
        cbTraceRead8.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cbTraceRead8ItemStateChanged(evt);
            }
        });
        jPanel1.add(cbTraceRead8);

        cbTraceWrite8.setText(bundle.getString("MemoryBreakpointsDialog.cbTraceWrite8.text")); // NOI18N
        cbTraceWrite8.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cbTraceWrite8ItemStateChanged(evt);
            }
        });
        jPanel1.add(cbTraceWrite8);

        cbTraceRead16.setText(bundle.getString("MemoryBreakpointsDialog.cbTraceRead16.text")); // NOI18N
        cbTraceRead16.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cbTraceRead16ItemStateChanged(evt);
            }
        });
        jPanel1.add(cbTraceRead16);

        cbTraceWrite16.setText(bundle.getString("MemoryBreakpointsDialog.cbTraceWrite16.text")); // NOI18N
        cbTraceWrite16.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cbTraceWrite16ItemStateChanged(evt);
            }
        });
        jPanel1.add(cbTraceWrite16);

        cbTraceRead32.setText(bundle.getString("MemoryBreakpointsDialog.cbTraceRead32.text")); // NOI18N
        cbTraceRead32.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cbTraceRead32ItemStateChanged(evt);
            }
        });
        jPanel1.add(cbTraceRead32);

        cbTraceWrite32.setText(bundle.getString("MemoryBreakpointsDialog.cbTraceWrite32.text")); // NOI18N
        cbTraceWrite32.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cbTraceWrite32ItemStateChanged(evt);
            }
        });
        jPanel1.add(cbTraceWrite32);

        chkPauseOnHit.setSelected(((DebuggerMemory)Memory.getInstance()).pauseEmulatorOnMemoryBreakpoint);
        chkPauseOnHit.setText(bundle.getString("MemoryBreakpointsDialog.chkPauseOnHit.text")); // NOI18N
        chkPauseOnHit.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                chkPauseOnHitItemStateChanged(evt);
            }
        });
        jPanel1.add(chkPauseOnHit);

        btnClose.setText(bundle.getString("CloseButton.text")); // NOI18N
        btnClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCloseActionPerformed(evt);
            }
        });

        btnExport.setText(bundle.getString("MemoryBreakpointsDialog.btnExport.text")); // NOI18N
        btnExport.setEnabled(false);
        btnExport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnExportActionPerformed(evt);
            }
        });

        btnImport.setText(bundle.getString("MemoryBreakpointsDialog.btnImport.text")); // NOI18N
        btnImport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnImportActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSeparator1)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(btnImport)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnExport)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnClose))
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 576, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 127, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnExport)
                    .addComponent(btnClose)
                    .addComponent(btnImport))
                .addGap(31, 31, 31))
        );

        tblBreakpoints.setFont(new java.awt.Font("Courier New", 0, 12)); // NOI18N
        tblBreakpoints.setModel(memoryBreakpointsModel);
        tblBreakpoints.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane2.setViewportView(tblBreakpoints);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btnAdd, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnRemove, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 104, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnAdd, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnRemove, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, 178, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCloseActionPerformed
        setVisible(false);
    }//GEN-LAST:event_btnCloseActionPerformed
    
    private void chkPauseOnHitItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_chkPauseOnHitItemStateChanged
        ((DebuggerMemory) Memory.getInstance()).pauseEmulatorOnMemoryBreakpoint = chkPauseOnHit.isSelected();
    }//GEN-LAST:event_chkPauseOnHitItemStateChanged
    
    private void btnAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddActionPerformed
        MemoryBreakpoint mbp = new MemoryBreakpoint();
        memoryBreakpoints.add(mbp);
        memoryBreakpointsModel.fireTableRowsInserted(memoryBreakpoints.size() - 1, memoryBreakpoints.size() - 1);
    }//GEN-LAST:event_btnAddActionPerformed
    
    private void btnRemoveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRemoveActionPerformed
        int row = tblBreakpoints.getSelectedRow();
        MemoryBreakpoint mbp = memoryBreakpoints.remove(row);

        // make sure breakpoint is uninstalled after being removed
        mbp.setEnabled(false);
        memoryBreakpointsModel.fireTableRowsDeleted(row, row);

        // after removal no item is selected - so disable the button once again
        btnRemove.setEnabled(false);
    }//GEN-LAST:event_btnRemoveActionPerformed
    
    private void cbTraceReadItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cbTraceReadItemStateChanged
        ((DebuggerMemory) Memory.getInstance()).traceMemoryRead = cbTraceRead.isSelected();
    }//GEN-LAST:event_cbTraceReadItemStateChanged
    
    private void cbTraceRead8ItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cbTraceRead8ItemStateChanged
        ((DebuggerMemory) Memory.getInstance()).traceMemoryRead8 = cbTraceRead8.isSelected();
    }//GEN-LAST:event_cbTraceRead8ItemStateChanged
    
    private void cbTraceRead16ItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cbTraceRead16ItemStateChanged
        ((DebuggerMemory) Memory.getInstance()).traceMemoryRead16 = cbTraceRead16.isSelected();
    }//GEN-LAST:event_cbTraceRead16ItemStateChanged
    
    private void cbTraceRead32ItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cbTraceRead32ItemStateChanged
        ((DebuggerMemory) Memory.getInstance()).traceMemoryRead32 = cbTraceRead32.isSelected();
    }//GEN-LAST:event_cbTraceRead32ItemStateChanged
    
    private void cbTraceWriteItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cbTraceWriteItemStateChanged
        ((DebuggerMemory) Memory.getInstance()).traceMemoryWrite = cbTraceWrite.isSelected();
    }//GEN-LAST:event_cbTraceWriteItemStateChanged
    
    private void cbTraceWrite8ItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cbTraceWrite8ItemStateChanged
        ((DebuggerMemory) Memory.getInstance()).traceMemoryWrite8 = cbTraceWrite8.isSelected();
    }//GEN-LAST:event_cbTraceWrite8ItemStateChanged
    
    private void cbTraceWrite16ItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cbTraceWrite16ItemStateChanged
        ((DebuggerMemory) Memory.getInstance()).traceMemoryWrite16 = cbTraceWrite16.isSelected();
    }//GEN-LAST:event_cbTraceWrite16ItemStateChanged
    
    private void cbTraceWrite32ItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cbTraceWrite32ItemStateChanged
        ((DebuggerMemory) Memory.getInstance()).traceMemoryWrite32 = cbTraceWrite32.isSelected();
    }//GEN-LAST:event_cbTraceWrite32ItemStateChanged
    
    private void btnExportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExportActionPerformed
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("jpcsp/languages/jpcsp");
        final JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(bundle.getString("MemoryBreakpointsDialog.dlgExport.title"));
        fc.setSelectedFile(new File(State.discId + ".mbrk"));
        fc.setCurrentDirectory(new java.io.File("."));
        fc.addChoosableFileFilter(Constants.fltMemoryBreakpointFiles);
        fc.setFileFilter(Constants.fltMemoryBreakpointFiles);
        
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            if (f.exists()) {
                int rc = JOptionPane.showConfirmDialog(
                        this,
                        bundle.getString("ConsoleWindow.strFileExists.text"),
                        bundle.getString("ConsoleWindow.strFileExistsTitle.text"),
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                
                if (rc != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            ((DebuggerMemory) Memory.getInstance()).exportBreakpoints(fc.getSelectedFile());
        }
    }//GEN-LAST:event_btnExportActionPerformed
    
    private void btnImportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnImportActionPerformed
        final JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(java.util.ResourceBundle.getBundle("jpcsp/languages/jpcsp").getString("MemoryBreakpointsDialog.dlgImport.title"));
        fc.setSelectedFile(new File(State.discId + ".mbrk"));
        fc.setCurrentDirectory(new java.io.File("."));
        fc.addChoosableFileFilter(Constants.fltMemoryBreakpointFiles);
        fc.setFileFilter(Constants.fltMemoryBreakpointFiles);
        
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            ((DebuggerMemory) Memory.getInstance()).importBreakpoints(fc.getSelectedFile());
        }
        memoryBreakpointsModel.fireTableDataChanged();
        updateTraceSettings();
    }//GEN-LAST:event_btnImportActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAdd;
    private javax.swing.JButton btnClose;
    private javax.swing.JButton btnExport;
    private javax.swing.JButton btnImport;
    private javax.swing.JButton btnRemove;
    private javax.swing.JCheckBox cbTraceRead;
    private javax.swing.JCheckBox cbTraceRead16;
    private javax.swing.JCheckBox cbTraceRead32;
    private javax.swing.JCheckBox cbTraceRead8;
    private javax.swing.JCheckBox cbTraceWrite;
    private javax.swing.JCheckBox cbTraceWrite16;
    private javax.swing.JCheckBox cbTraceWrite32;
    private javax.swing.JCheckBox cbTraceWrite8;
    private javax.swing.JCheckBox chkPauseOnHit;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTable tblBreakpoints;
    // End of variables declaration//GEN-END:variables
}
