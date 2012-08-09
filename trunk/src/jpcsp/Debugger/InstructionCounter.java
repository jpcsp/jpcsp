/* This file is part of jpcsp.
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
package jpcsp.Debugger;

import java.awt.Cursor;
import java.awt.Toolkit;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JFileChooser;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Resource;
import jpcsp.Allegrex.Common.Instruction;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.util.Utilities;

import com.jidesoft.utils.SwingWorker;

/**
 *
 * @author  George
 */
public class InstructionCounter extends javax.swing.JFrame implements PropertyChangeListener {

	private static final long serialVersionUID = 1L;
	private Task task;
    private SceModule module;

    /** Creates new form InstructionCounter */
    public InstructionCounter() {
        initComponents();
        // moved to setModule
        //RefreshWindow();
    }

    public void setModule(SceModule module) {
        this.module = module;
        RefreshWindow();
    }

    public void RefreshWindow() {
        if (module == null)
            return;

        resetcounts();
        areastatus.setText("");

        if (module.text_addr == 0) {
            textcheck.setEnabled(false);
            textcheck.setSelected(false);
        } else {
            textcheck.setEnabled(true);
            textcheck.setSelected(true);
            areastatus.append("Found .text section at " + Integer.toHexString(module.text_addr) + " size " + module.text_size + "\n");
        }

        if (module.initsection[0] == 0) {
            initcheck.setEnabled(false);
            initcheck.setSelected(false);
        } else {
            initcheck.setEnabled(true);
            initcheck.setSelected(true);
            areastatus.append("Found .init section at " + Integer.toHexString(module.initsection[0]) + " size " + module.initsection[1] + "\n");
        }

        if (module.finisection[0] == 0) {
            finicheck.setEnabled(false);
            finicheck.setSelected(false);
        } else {
            finicheck.setEnabled(true);
            finicheck.setSelected(true);
            areastatus.append("Found .fini section at " + Integer.toHexString(module.finisection[0]) + " size " + module.finisection[1] + "\n");
        }

        if (module.stubtextsection[0] == 0) {
            stubtextcheck.setEnabled(false);
            stubtextcheck.setSelected(false);
        } else {
            stubtextcheck.setEnabled(true);
            stubtextcheck.setSelected(true);
            areastatus.append("Found .sceStub.text at " + Integer.toHexString(module.stubtextsection[0]) + " size " + module.stubtextsection[1]);
        }

        pack();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        textcheck = new javax.swing.JCheckBox();
        initcheck = new javax.swing.JCheckBox();
        finicheck = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        startbutton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        areastatus = new javax.swing.JTextArea();
        jScrollPane2 = new javax.swing.JScrollPane();
        OpcodeTable = new javax.swing.JTable();
        stubtextcheck = new javax.swing.JCheckBox();
        Save = new javax.swing.JButton();

        setTitle(Resource.get("instructioncounter"));
        setResizable(false);

        textcheck.setText(".text");

        initcheck.setText(".init");

        finicheck.setText(".fini");

        jLabel1.setText(Resource.get("chooseSectionsCount"));

        startbutton.setText(Resource.get("startcounting"));
        startbutton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startbuttonActionPerformed(evt);
            }
        });

        areastatus.setColumns(20);
        areastatus.setFont(new java.awt.Font("Courier New", 0, 12));
        areastatus.setRows(4);
        jScrollPane1.setViewportView(areastatus);

        OpcodeTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [jpcsp.Allegrex.Common.instructions().length][3],
            new String [] {"Opcode", "Category", "Count"}) {
            
				private static final long serialVersionUID = 1L;
			@SuppressWarnings("rawtypes")
			Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            @SuppressWarnings("rawtypes")
			@Override
			public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            @Override
			public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane2.setViewportView(OpcodeTable);

        stubtextcheck.setText(".sceStub.text");

        Save.setText(Resource.get("savetofile"));
        Save.addActionListener(new java.awt.event.ActionListener() {
            @Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
                SaveActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(startbutton)
                        .addGap(18, 18, 18)
                        .addComponent(progressBar, javax.swing.GroupLayout.DEFAULT_SIZE, 216, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 8, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(stubtextcheck)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(textcheck)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(initcheck)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(finicheck))))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 337, Short.MAX_VALUE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 332, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Save, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(finicheck)
                    .addComponent(initcheck)
                    .addComponent(textcheck)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(stubtextcheck)
                .addGap(4, 4, 4)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(progressBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(startbutton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 402, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 8, Short.MAX_VALUE)
                .addComponent(Save))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void startbuttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startbuttonActionPerformed
    startbutton.setEnabled(false);
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    //Instances of javax.swing.SwingWorker are not reusuable, so
    //we create new instances as needed.
    progressBar.setIndeterminate(true);
    task = new Task();
    task.addPropertyChangeListener(this);
    task.execute();

}//GEN-LAST:event_startbuttonActionPerformed

private void SaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SaveActionPerformed
    File file;
    final JFileChooser fc = new JFileChooser();
    fc.setDialogTitle(Resource.get("saveinstructions"));
    fc.setCurrentDirectory(new java.io.File("."));
    fc.setSelectedFile(new File("instructionoutput.txt"));
    int returnvalue = fc.showSaveDialog(this);
    if (returnvalue == JFileChooser.APPROVE_OPTION) {
        file = fc.getSelectedFile();
    } else {
        return;
    }
    BufferedWriter bufferedWriter = null;
    try {

        //Construct the BufferedWriter object
        bufferedWriter = new BufferedWriter(new FileWriter(file));

        //Start writing to the output stream
        for (int i = 0; i < OpcodeTable.getRowCount(); i++) {

            OpcodeTable.getValueAt(i, 1);
            bufferedWriter.write(OpcodeTable.getValueAt(i, 0) + "  " + OpcodeTable.getValueAt(i, 1) + "  " + OpcodeTable.getValueAt(i, 2));
            bufferedWriter.newLine();
        }
    } catch (FileNotFoundException ex) {
        ex.printStackTrace();
    } catch (IOException ex) {
        ex.printStackTrace();
    } finally {
        Utilities.close(bufferedWriter);
    }

}//GEN-LAST:event_SaveActionPerformed
   /**
     * Invoked when task's progress property changes.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("progress")) {
            int progress = (Integer) evt.getNewValue();
            progressBar.setValue(progress);
        }
    }

    public void findinitsections()
    {
       for(int i =0; i< module.initsection[1]; i+=4)
       {
          int memread32 = Memory.getInstance().read32(module.initsection[0]+i);
          jpcsp.Allegrex.Decoder.instruction(memread32).increaseCount();
       }
    }
    public void findfinisections()
    {
       for(int i =0; i< module.finisection[1]; i+=4)
       {
          int memread32 = Memory.getInstance().read32(module.finisection[0]+i);
          jpcsp.Allegrex.Decoder.instruction(memread32).increaseCount();
       }
    }
    public void findtextsections()
    {
       for(int i =0; i< module.text_size; i+=4)
       {
          int memread32 = Memory.getInstance().read32(module.text_addr+i);
          jpcsp.Allegrex.Decoder.instruction(memread32).increaseCount();
       }
    }
    public void findstubtextsections()
    {
        for(int i =0; i< module.stubtextsection[1]; i+=4)
       {
          int memread32 = Memory.getInstance().read32(module.stubtextsection[0]+i);
          jpcsp.Allegrex.Decoder.instruction(memread32).increaseCount();
       }

    }
    class Task extends SwingWorker<Void, Void> {
        /*
         * Main task. Executed in background thread.
         */
        @Override
        public Void doInBackground() {
            setProgress(0);

            resetcounts();
            setProgress(20);

            if (initcheck.isSelected()) findinitsections();
            setProgress(40);

            if (textcheck.isSelected()) findtextsections();
            setProgress(60);

            if (finicheck.isSelected()) findfinisections();
            setProgress(80);

            if (stubtextcheck.isSelected()) findstubtextsections();
            setProgress(100);

            return null;
        }

        /*
         * Executed in event dispatching thread
         */
        @Override
        public void done() {
            refreshCounter();
            Toolkit.getDefaultToolkit().beep();
            startbutton.setEnabled(true);
            setCursor(null); //turn off the wait cursor
            progressBar.setIndeterminate(false);
        }
    }
    // Let's instanciate this private member so the two following methods
    // can retrieve the right opcodes.
    public static jpcsp.Allegrex.Instructions INSTRUCTIONS = new jpcsp.Allegrex.Instructions();

    public void refreshCounter()
    {
        int i = 0;
        java.util.TreeMap< String, Instruction > instructions = new java.util.TreeMap< String, Instruction >();
        for (Instruction insn : jpcsp.Allegrex.Common.instructions()) {
            if (insn != null) {
                instructions.put(insn.name(), insn);
            }
        }
        for (Instruction insn : instructions.values()) {
            if (insn != null && insn.getCount() > 0) {
                OpcodeTable.setValueAt(insn.name(), i, 0);
                OpcodeTable.setValueAt(insn.category(), i, 1);
                OpcodeTable.setValueAt(insn.getCount(), i, 2);
                i++;
            }
        }
    }
    public void resetcounts()
    {
        int i = 0;

        for (Instruction insn : jpcsp.Allegrex.Common.instructions()) {
            if (insn != null) {
                insn.resetCount();
                OpcodeTable.setValueAt("", i, 2);
                i++;
            }
        }
    }

	@Override
	public void dispose() {
		Emulator.getMainGUI().endWindowDialog();
		super.dispose();
	}

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTable OpcodeTable;
    private javax.swing.JButton Save;
    private javax.swing.JTextArea areastatus;
    private javax.swing.JCheckBox finicheck;
    private javax.swing.JCheckBox initcheck;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JButton startbutton;
    private javax.swing.JCheckBox stubtextcheck;
    private javax.swing.JCheckBox textcheck;
    // End of variables declaration//GEN-END:variables

}


