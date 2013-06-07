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

import java.io.File;
import java.util.List;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import jpcsp.Debugger.MemoryBreakpoints.MemoryBreakpoint.AccessType;
import jpcsp.Memory;
import jpcsp.State;
import jpcsp.memory.DebuggerMemory;

public class MemoryBreakpointsDialog extends javax.swing.JDialog {

    private List<MemoryBreakpoint> memoryBreakpoints;
    private MemoryBreakpointsModel memoryBreakpointsModel;
    private final int COL_STARTADDRESS = 0;
    private final int COL_ENDADDRESS = 1;
    private final int COL_ACCESSTYPE = 2;
    private final int COL_ACTIVE = 3;
    private final int COL_LAST = 4;

    public MemoryBreakpointsDialog(java.awt.Frame parent) {
        super(parent, true);

        memoryBreakpoints = ((DebuggerMemory) Memory.getInstance()).getMemoryBreakpoints();
        memoryBreakpointsModel = new MemoryBreakpointsModel();

        initComponents();

        TableColumn accessType = tblBreakpoints.getColumnModel().getColumn(COL_ACCESSTYPE);
        JComboBox combo = new JComboBox();
        combo.addItem("READ");
        combo.addItem("WRITE");
        combo.addItem("READWRITE");
        accessType.setCellEditor(new DefaultCellEditor(combo));

        tblBreakpoints.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                btnRemove.setEnabled(true);
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
    }

    private class MemoryBreakpointsModel extends AbstractTableModel {

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            MemoryBreakpoint mbp = memoryBreakpoints.get(rowIndex);
            switch (columnIndex) {
                case COL_STARTADDRESS:
                    try {
                        mbp.setStartAddress(Integer.decode((String) aValue));
                    } catch (NumberFormatException nfe) {
                        // do nothing - cell will revert to previous value
                        return;
                    }
                    break;
                case COL_ENDADDRESS:
                    try {
                        mbp.setEndAddress(Integer.decode((String) aValue));
                    } catch (NumberFormatException nfe) {
                        // do nothing - cell will revert to previous value
                        return;
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
                    return String.class;
                case COL_ENDADDRESS:
                    return String.class;
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
            switch (column) {
                case COL_STARTADDRESS:
                    return "Start";
                case COL_ENDADDRESS:
                    return "End";
                case COL_ACCESSTYPE:
                    return "Access";
                case COL_ACTIVE:
                    return "Active";
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
        chkPauseOnHit = new javax.swing.JCheckBox();
        jSeparator1 = new javax.swing.JSeparator();
        btnClose = new javax.swing.JButton();
        cbTraceRead = new javax.swing.JCheckBox();
        cbTraceRead8 = new javax.swing.JCheckBox();
        cbTraceRead16 = new javax.swing.JCheckBox();
        cbTraceRead32 = new javax.swing.JCheckBox();
        cbTraceWrite = new javax.swing.JCheckBox();
        cbTraceWrite8 = new javax.swing.JCheckBox();
        cbTraceWrite16 = new javax.swing.JCheckBox();
        cbTraceWrite32 = new javax.swing.JCheckBox();
        btnExport = new javax.swing.JButton();
        btnImport = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        tblBreakpoints = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Memory breakpoints");
        setLocationByPlatform(true);
        setModal(true);
        setName("dialog"); // NOI18N
        setResizable(false);

        btnAdd.setText("Add");
        btnAdd.setMaximumSize(new java.awt.Dimension(140, 25));
        btnAdd.setMinimumSize(new java.awt.Dimension(140, 25));
        btnAdd.setPreferredSize(new java.awt.Dimension(140, 25));
        btnAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddActionPerformed(evt);
            }
        });

        btnRemove.setText("Remove");
        btnRemove.setEnabled(false);
        btnRemove.setMaximumSize(new java.awt.Dimension(140, 25));
        btnRemove.setMinimumSize(new java.awt.Dimension(140, 25));
        btnRemove.setPreferredSize(new java.awt.Dimension(140, 25));
        btnRemove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRemoveActionPerformed(evt);
            }
        });

        chkPauseOnHit.setSelected(((DebuggerMemory)Memory.getInstance()).pauseEmulatorOnMemoryBreakpoint);
        chkPauseOnHit.setText("pause emulator on hit");
        chkPauseOnHit.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                chkPauseOnHitItemStateChanged(evt);
            }
        });

        btnClose.setText("Close");
        btnClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCloseActionPerformed(evt);
            }
        });

        cbTraceRead.setText("trace all reads");
        cbTraceRead.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cbTraceReadItemStateChanged(evt);
            }
        });

        cbTraceRead8.setText("trace on BYTE read");
        cbTraceRead8.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cbTraceRead8ItemStateChanged(evt);
            }
        });

        cbTraceRead16.setText("trace on WORD read");
        cbTraceRead16.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cbTraceRead16ItemStateChanged(evt);
            }
        });

        cbTraceRead32.setText("trace on DWORD read");
        cbTraceRead32.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cbTraceRead32ItemStateChanged(evt);
            }
        });

        cbTraceWrite.setText("trace all writes");
        cbTraceWrite.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cbTraceWriteItemStateChanged(evt);
            }
        });

        cbTraceWrite8.setText("trace on BYTE write");
        cbTraceWrite8.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cbTraceWrite8ItemStateChanged(evt);
            }
        });

        cbTraceWrite16.setText("trace on WORD write");
        cbTraceWrite16.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cbTraceWrite16ItemStateChanged(evt);
            }
        });

        cbTraceWrite32.setText("trace on DWORD write");
        cbTraceWrite32.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cbTraceWrite32ItemStateChanged(evt);
            }
        });

        btnExport.setText("Export");
        btnExport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnExportActionPerformed(evt);
            }
        });

        btnImport.setText("Import");
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
                .addComponent(cbTraceRead, javax.swing.GroupLayout.PREFERRED_SIZE, 215, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cbTraceWrite, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(cbTraceRead8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(cbTraceRead16, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(cbTraceRead32, javax.swing.GroupLayout.DEFAULT_SIZE, 215, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(cbTraceWrite8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(cbTraceWrite16, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(cbTraceWrite32, javax.swing.GroupLayout.DEFAULT_SIZE, 205, Short.MAX_VALUE)))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(chkPauseOnHit)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btnImport)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnExport)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnClose)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbTraceRead)
                    .addComponent(cbTraceWrite))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbTraceRead8)
                    .addComponent(cbTraceWrite8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbTraceWrite16)
                    .addComponent(cbTraceRead16))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbTraceWrite32)
                    .addComponent(cbTraceRead32))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(chkPauseOnHit)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnExport)
                    .addComponent(btnClose)
                    .addComponent(btnImport))
                .addGap(31, 31, 31))
        );

        tblBreakpoints.setModel(memoryBreakpointsModel);
        tblBreakpoints.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane2.setViewportView(tblBreakpoints);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(btnAdd, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnRemove, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(22, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnAdd, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnRemove, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, 178, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCloseActionPerformed
        dispose();
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
        final JFileChooser fc = new JFileChooser();
        final FileNameExtensionFilter flt = new FileNameExtensionFilter("Breakpoint files", "mbrk");
        fc.setDialogTitle("Export memory breakpoints to file...");
        fc.setSelectedFile(new File(State.discId + ".mbrk"));
        fc.setCurrentDirectory(new java.io.File("."));
        fc.addChoosableFileFilter(flt);
        fc.setFileFilter(flt);

        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            if (f.exists()) {
                int rc = JOptionPane.showConfirmDialog(
                        this,
                        "File '" + f + "' already exists. Do you want to overwrite?",
                        "File exists",
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
        final FileNameExtensionFilter flt = new FileNameExtensionFilter("Breakpoint files", "mbrk");
        fc.setDialogTitle("Import memory breakpoints from file...");
        fc.setSelectedFile(new File(State.discId + ".mbrk"));
        fc.setCurrentDirectory(new java.io.File("."));
        fc.addChoosableFileFilter(flt);
        fc.setFileFilter(flt);

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
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTable tblBreakpoints;
    // End of variables declaration//GEN-END:variables
}
