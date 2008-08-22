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


import jpcsp.util.OptionPaneMultiple;
import java.awt.Point;
import jpcsp.Debugger.*;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import jpcsp.Emulator;
import jpcsp.GeneralJpcspException;
import jpcsp.Memory;
import jpcsp.Settings;
import jpcsp.util.JpcspDialogManager;

/**
 *
 * @author  shadow
 */
public class Disassembler extends javax.swing.JInternalFrame implements ClipboardOwner, Runnable {
    Thread t;
    Emulator emu;
    int DebuggerPC;
    private DefaultListModel model_1 = new DefaultListModel();
    //int pcreg;
    int opcode_address; // store the address of the opcode used for offsetdecode
   // Processor c;
    Registers regs;
    MemoryViewer memview;
    DisasmOpcodes disOp = new DisasmOpcodes();

    ArrayList<Integer> breakpoints = new ArrayList<Integer>();
    /* Creates new form Disasembler */
    public Disassembler(Emulator emu, Registers regs, MemoryViewer memview) {
        //this.c = c;
        t=new Thread(this);
        emu.pause=false;
        this.regs=regs;
        this.memview=memview;
        this.emu=emu;
        DebuggerPC = 0;
        //pcreg = c.pc;
        model_1 = new DefaultListModel();
        initComponents();
        RefreshDebugger();

    }
    public void run()
    {
       try {
           if(emu.pause)//emu is paused
           {
               
               //emu.resume();
               emu.run=true;
               emu.run();
               //t.resume();
             
           }
           else
           {
               
             emu.run=true;
             emu.run();
           }
       }catch(GeneralJpcspException e)
       {
            JpcspDialogManager.showError(null, "General Error : " + e.getMessage());
       }
    }
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        DisMenu = new javax.swing.JPopupMenu();
        CopyAddress = new javax.swing.JMenuItem();
        CopyAll = new javax.swing.JMenuItem();
        BranchOrJump = new javax.swing.JMenuItem();
        jList1 = new javax.swing.JList(model_1);
        ResetToPC = new javax.swing.JButton();
        JumpTo = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        StepEmu = new javax.swing.JButton();
        RunEmu = new javax.swing.JButton();
        StopEmu = new javax.swing.JButton();
        AddBreakpoint = new javax.swing.JButton();
        RemoveBreakpoint = new javax.swing.JButton();
        ClearBreakpoints = new javax.swing.JButton();
        RunWithBreakPoints = new javax.swing.JButton();

        CopyAddress.setText("Copy Address");
        CopyAddress.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CopyAddressActionPerformed(evt);
            }
        });
        DisMenu.add(CopyAddress);

        CopyAll.setText("Copy All");
        CopyAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CopyAllActionPerformed(evt);
            }
        });
        DisMenu.add(CopyAll);

        BranchOrJump.setText("Copy Branch Or Jump address");
        BranchOrJump.setEnabled(false); //disable as default
        BranchOrJump.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BranchOrJumpActionPerformed(evt);
            }
        });
        DisMenu.add(BranchOrJump);

        setClosable(true);
        setDefaultCloseOperation(javax.swing.WindowConstants.HIDE_ON_CLOSE);
        setTitle("Disassembler");
        addInternalFrameListener(new javax.swing.event.InternalFrameListener() {
            public void internalFrameActivated(javax.swing.event.InternalFrameEvent evt) {
            }
            public void internalFrameClosed(javax.swing.event.InternalFrameEvent evt) {
            }
            public void internalFrameClosing(javax.swing.event.InternalFrameEvent evt) {
                formInternalFrameClosing(evt);
            }
            public void internalFrameDeactivated(javax.swing.event.InternalFrameEvent evt) {
            }
            public void internalFrameDeiconified(javax.swing.event.InternalFrameEvent evt) {
            }
            public void internalFrameIconified(javax.swing.event.InternalFrameEvent evt) {
            }
            public void internalFrameOpened(javax.swing.event.InternalFrameEvent evt) {
            }
        });

        jList1.setFont(new java.awt.Font("Courier New", 0, 11));
        jList1.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jList1.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                jList1MouseWheelMoved(evt);
            }
        });
        jList1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jList1MouseClicked(evt);
            }
        });
        jList1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jList1KeyPressed(evt);
            }
        });

        ResetToPC.setText("Reset to PC");
        ResetToPC.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ResetToPCActionPerformed(evt);
            }
        });

        JumpTo.setText("Jump to Address");
        JumpTo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                JumpToActionPerformed(evt);
            }
        });

        jButton3.setText("Dump code");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        StepEmu.setText("Step CPU");
        StepEmu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StepEmuActionPerformed(evt);
            }
        });

        RunEmu.setText("Run ");
        RunEmu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RunEmuActionPerformed(evt);
            }
        });

        StopEmu.setText("Stop");
        StopEmu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StopEmuActionPerformed(evt);
            }
        });

        AddBreakpoint.setText("Add Breakpoint");
        AddBreakpoint.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AddBreakpointActionPerformed(evt);
            }
        });

        RemoveBreakpoint.setText("Remove Breakpoint");
        RemoveBreakpoint.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RemoveBreakpointActionPerformed(evt);
            }
        });

        ClearBreakpoints.setText("Clear Breakpoints");
        ClearBreakpoints.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ClearBreakpointsActionPerformed(evt);
            }
        });

        RunWithBreakPoints.setText("Run With Breakpoints");
        RunWithBreakPoints.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RunWithBreakPointsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(jList1, javax.swing.GroupLayout.PREFERRED_SIZE, 456, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(RunEmu, javax.swing.GroupLayout.DEFAULT_SIZE, 135, Short.MAX_VALUE)
                    .addComponent(StopEmu, javax.swing.GroupLayout.DEFAULT_SIZE, 135, Short.MAX_VALUE)
                    .addComponent(StepEmu, javax.swing.GroupLayout.DEFAULT_SIZE, 135, Short.MAX_VALUE)
                    .addComponent(RunWithBreakPoints, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButton3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 135, Short.MAX_VALUE)
                    .addComponent(ClearBreakpoints, javax.swing.GroupLayout.DEFAULT_SIZE, 135, Short.MAX_VALUE)
                    .addComponent(RemoveBreakpoint, javax.swing.GroupLayout.DEFAULT_SIZE, 135, Short.MAX_VALUE)
                    .addComponent(AddBreakpoint, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 135, Short.MAX_VALUE)
                    .addComponent(JumpTo, javax.swing.GroupLayout.DEFAULT_SIZE, 135, Short.MAX_VALUE)
                    .addComponent(ResetToPC, javax.swing.GroupLayout.DEFAULT_SIZE, 135, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jList1, javax.swing.GroupLayout.DEFAULT_SIZE, 367, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(RunEmu)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(RunWithBreakPoints)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(StopEmu)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(StepEmu)
                        .addGap(30, 30, 30)
                        .addComponent(ResetToPC)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(JumpTo)
                        .addGap(19, 19, 19)
                        .addComponent(AddBreakpoint)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(RemoveBreakpoint)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(ClearBreakpoints)
                        .addGap(33, 33, 33)
                        .addComponent(jButton3)
                        .addGap(25, 25, 25))))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


private void jList1KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jList1KeyPressed
// TODO add your handling code here:

    if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_DOWN && jList1.getSelectedIndex() == jList1.getLastVisibleIndex()) {
        DebuggerPC += 4;
        RefreshDebugger();
        evt.consume();
        jList1.setSelectedIndex(jList1.getLastVisibleIndex());
    } else if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_UP && jList1.getSelectedIndex() == 0) {
        DebuggerPC -= 4;
        RefreshDebugger();
        evt.consume();
        jList1.setSelectedIndex(0);
    } else if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_PAGE_UP && jList1.getSelectedIndex() == 0) {
        DebuggerPC -= 0x68;
        RefreshDebugger();
        evt.consume();
        jList1.setSelectedIndex(0);
    } else if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_PAGE_DOWN && jList1.getSelectedIndex() == jList1.getLastVisibleIndex()) {
        DebuggerPC += 0x68;
        RefreshDebugger();
        evt.consume();
        jList1.setSelectedIndex(jList1.getLastVisibleIndex());
    }
}//GEN-LAST:event_jList1KeyPressed

private void jList1MouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_jList1MouseWheelMoved
// TODO add your handling code here:
    if (evt.getWheelRotation() < 0) {
        evt.consume();
        if (jList1.getSelectedIndex() == 0 || jList1.getSelectedIndex() == -1) {
            DebuggerPC -= 4;
            RefreshDebugger();
            jList1.setSelectedIndex(0);
        } else {
            jList1.setSelectedIndex(jList1.getSelectedIndex() - 1);
        }
    } else {
        evt.consume();
        if (jList1.getSelectedIndex() == jList1.getLastVisibleIndex()) {
            DebuggerPC += 4;
            RefreshDebugger();
            jList1.setSelectedIndex(jList1.getLastVisibleIndex());
        } else {
            jList1.setSelectedIndex(jList1.getSelectedIndex() + 1);
        }
    }
}//GEN-LAST:event_jList1MouseWheelMoved

private void ResetToPCActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ResetToPCActionPerformed
    DebuggerPC = emu.getProcessor().pc;//c.pc;//
    RefreshDebugger();
}//GEN-LAST:event_ResetToPCActionPerformed

private void JumpToActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_JumpToActionPerformed
    String input = (String) JOptionPane.showInternalInputDialog(this, "Enter the address to which to jump (Hex)", "Jpcsp", JOptionPane.QUESTION_MESSAGE, null, null, String.format("%08x", emu.getProcessor().pc));
    if (input == null) {
        return;
    }
    int value=0;
         try {
            value = Integer.parseInt(input, 16);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "The Number you enter is not correct");
            return;
        }
        DebuggerPC = value;
        RefreshDebugger();

}//GEN-LAST:event_JumpToActionPerformed


private void StepEmuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StepEmuActionPerformed
    try {
        emu.getProcessor().step();
        jpcsp.HLE.ThreadMan.get_instance().step();
        jpcsp.HLE.pspdisplay.get_instance().step();
    } catch(GeneralJpcspException e) {
        JpcspDialogManager.showError(this, "General Error : " + e.getMessage());
    }
    DebuggerPC = 0;
    RefreshDebugger();
    regs.RefreshRegisters();
    memview.RefreshMemory();
}//GEN-LAST:event_StepEmuActionPerformed

private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
// TODO add your handling code here:
    //System.out.println("dump code dialog created");
    OptionPaneMultiple opt=new OptionPaneMultiple(Integer.toHexString(DebuggerPC),Integer.toHexString(DebuggerPC + 0x00000068));
    if(opt.completed()){
        //Here the input can be used to actually dump code
        System.out.println("Start address: "+opt.getInput()[0]);
        System.out.println("End address: "+opt.getInput()[1]);
        System.out.println("File name: "+opt.getInput()[2]);

        BufferedWriter bufferedWriter = null;
        try {

            //Construct the BufferedWriter object
            bufferedWriter = new BufferedWriter(new FileWriter(opt.getInput()[2]));

            //Start writing to the output stream
            bufferedWriter.write("-------JPCSP DISASM-----------");
            bufferedWriter.newLine();
            int Start = Integer.parseInt(opt.getInput()[0],16);
            int End = Integer.parseInt(opt.getInput()[1],16);
            for(int i =Start; i<=End; i+=4)
            {
               int memread = Memory.get_instance().read32((int) i);
             if (memread == 0) {
                bufferedWriter.write(String.format("%08x : [%08x]: nop", i, memread));
                bufferedWriter.newLine();
             } else {
                opcode_address = i;
                bufferedWriter.write(String.format("%08x : [%08x]: %s", i, memread, disOp.disasm(memread,opcode_address)));
                bufferedWriter.newLine();
             }
            }


        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            //Close the BufferedWriter
            try {
                if (bufferedWriter != null) {
                    bufferedWriter.flush();
                    bufferedWriter.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    //System.out.println("dump code dialog done");
    opt=null;
}//GEN-LAST:event_jButton3ActionPerformed

private void CopyAddressActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CopyAddressActionPerformed
    String value = (String)jList1.getSelectedValue();
    String address;
    if(value.startsWith("<br>"))
      address = value.substring(4, 12);
    else
      address = value.substring(0, 8);
    StringSelection stringSelection = new StringSelection( address);
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    clipboard.setContents(stringSelection, this);
}//GEN-LAST:event_CopyAddressActionPerformed

private void jList1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jList1MouseClicked
       BranchOrJump.setEnabled(false);
       if (SwingUtilities.isRightMouseButton(evt) && !jList1.isSelectionEmpty() && jList1.locationToIndex(evt.getPoint()) == jList1.getSelectedIndex())
       {
           //check if we can enable branch or jump address copy
           String line = (String)jList1.getSelectedValue();
           int finddot = line.indexOf("]:");
           String opcode = line.substring(finddot+3,line.length());
           if(opcode.startsWith("b") || opcode.startsWith("j"))//it is definately a branch or jump opcode
           {
               BranchOrJump.setEnabled(true);
           }
            DisMenu.show(jList1, evt.getX(), evt.getY());
       }

}//GEN-LAST:event_jList1MouseClicked

private void CopyAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CopyAllActionPerformed
    String value = (String)jList1.getSelectedValue();
    StringSelection stringSelection = new StringSelection( value);
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    clipboard.setContents(stringSelection, this);
}//GEN-LAST:event_CopyAllActionPerformed

private void BranchOrJumpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BranchOrJumpActionPerformed
    String value = (String)jList1.getSelectedValue();
    int address = value.indexOf("0x");
    if(address==-1)
    {
      JpcspDialogManager.showError(this, "Can't find the jump or branch address");
      return;
    }
    else
    {
      String add = value.substring(address+2,value.length());
      StringSelection stringSelection = new StringSelection(add);
      Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      clipboard.setContents(stringSelection, this);

    }
}//GEN-LAST:event_BranchOrJumpActionPerformed

private void formInternalFrameClosing(javax.swing.event.InternalFrameEvent evt) {//GEN-FIRST:event_formInternalFrameClosing
   // System.out.println(this.getLocation());
    Point location = getLocation();
    //location.x
    String[] coord = new String[2];
    coord[0]=Integer.toString(location.x);
    coord[1]=Integer.toString(location.y);
    Settings.get_instance().writeWindowPos("disassembler", coord);
    //t.destroy();

}//GEN-LAST:event_formInternalFrameClosing
   final SwingWorker<Integer,Void> worker = new SwingWorker<Integer,Void>() {
   @Override
    public Integer doInBackground() { //start emulator
       try {
           if(emu.pause)//emu is paused
           {
               emu.resume();
           }
           else
           {
             emu.run=true;
             emu.run();
           }
       }catch(GeneralJpcspException e)
       {
            JpcspDialogManager.showError(null, "General Error : " + e.getMessage());
       }
        return 0;
    }

    @Override
    public void done() {
        emu.pause();
    }
  };
private void RunEmuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RunEmuActionPerformed

  //worker.execute();
   // t.start();
   
      if(emu.pause)//emu is paused
      {
          t.resume();
          
      }
      else
      {
          t.start();
          
      }

}//GEN-LAST:event_RunEmuActionPerformed

private void StopEmuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StopEmuActionPerformed
     //worker.cancel(false);
    t.suspend();
    emu.pause=true;
     DebuggerPC = 0;
    RefreshDebugger();
    regs.RefreshRegisters();
    memview.RefreshMemory();
}//GEN-LAST:event_StopEmuActionPerformed

private void AddBreakpointActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AddBreakpointActionPerformed
          String value =(String)jList1.getSelectedValue();
          if(value != null)
          {
            String address = value.substring(0, 8);
            int addr = Integer.parseInt(address,16);
            breakpoints.add(addr);
            //DebuggerPC = 0;
            RefreshDebugger();
          }
          else
          {
            JpcspDialogManager.showInformation(this, "Breakpoint Help : " + "Select the line to add a breakpoint to.");
          }
}//GEN-LAST:event_AddBreakpointActionPerformed

private void RemoveBreakpointActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RemoveBreakpointActionPerformed
          String value =(String)jList1.getSelectedValue();
          if(value != null)
          {
            boolean breakpointexists = value.startsWith("<br>");
            if(breakpointexists)
            {
              String address = value.substring(4, 12);
              int addr = Integer.parseInt(address,16);
              int b = breakpoints.indexOf(addr);
              breakpoints.remove(b);
              //DebuggerPC = 0;
              RefreshDebugger();
            }
          }
          else
          {
            JpcspDialogManager.showInformation(this, "Breakpoint Help : " + "Select the line to remove a breakpoint from.");
          }
}//GEN-LAST:event_RemoveBreakpointActionPerformed

private void ClearBreakpointsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ClearBreakpointsActionPerformed
         if(!breakpoints.isEmpty())
            breakpoints.clear();
}//GEN-LAST:event_ClearBreakpointsActionPerformed
     final SwingWorker<Integer,Void> worker2 = new SwingWorker<Integer,Void>() {
   @Override
    public Integer doInBackground() { //start emulator
       try {
           if(emu.pause)//emu is paused
           {
               emu.resume();
           }
           else
           {

             emu.run=true;
             while(emu.run)
             {
                 if(breakpoints.indexOf(emu.getProcessor().pc) != -1)
                 {
                     emu.run=false;
                          DebuggerPC = 0;
                    RefreshDebugger();
                     regs.RefreshRegisters();
                    memview.RefreshMemory();
                 }
                 else
                  emu.getProcessor().step();
             }

           }
       }catch(GeneralJpcspException e)
       {
            JpcspDialogManager.showError(null, "General Error : " + e.getMessage());
       }
        return 0;
    }

    @Override
    public void done() {
        emu.pause();
    }
  };
private void RunWithBreakPointsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RunWithBreakPointsActionPerformed
   worker2.execute();

}//GEN-LAST:event_RunWithBreakPointsActionPerformed


    public void RefreshDebugger() {
        int t;
        int cnt;
        if (DebuggerPC == 0) {
            DebuggerPC = emu.getProcessor().pc;//0x08900000;//test
        }
        model_1.clear();

        for (t = DebuggerPC          , cnt = 0; t < (DebuggerPC + 0x00000068); t += 0x00000004, cnt++) {

            int memread = Memory.get_instance().read32((int) t);

            if (memread == 0) {
                 if(breakpoints.indexOf(t)!=-1)
                      model_1.addElement(String.format("<br>%08x:[%08x]: nop", t, memread));
                 else
                  model_1.addElement(String.format("%08x:[%08x]: nop", t, memread));
            } else {
                opcode_address = t;
                if(breakpoints.indexOf(t)!=-1)
                {
                  // model_1.addElement(String.format("<br>%08x:[%08x]: %s", t, memread, disasm(memread)));
                    model_1.addElement(String.format("<br>%08x:[%08x]: %s", t, memread, disOp.disasm(memread,opcode_address)));
                }
                else
                {
                 //  model_1.addElement(String.format("%08x:[%08x]: %s", t, memread, disasm(memread)));
                    model_1.addElement(String.format("%08x:[%08x]: %s", t, memread, disOp.disasm(memread,opcode_address)));
                }
            }
        }

    }


    /**
   * Empty implementation of the ClipboardOwner interface.
   */

   public void lostOwnership( Clipboard aClipboard, Transferable aContents) {
     //do nothing
   }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton AddBreakpoint;
    private javax.swing.JMenuItem BranchOrJump;
    private javax.swing.JButton ClearBreakpoints;
    private javax.swing.JMenuItem CopyAddress;
    private javax.swing.JMenuItem CopyAll;
    private javax.swing.JPopupMenu DisMenu;
    private javax.swing.JButton JumpTo;
    private javax.swing.JButton RemoveBreakpoint;
    private javax.swing.JButton ResetToPC;
    private javax.swing.JButton RunEmu;
    private javax.swing.JButton RunWithBreakPoints;
    private javax.swing.JButton StepEmu;
    private javax.swing.JButton StopEmu;
    private javax.swing.JButton jButton3;
    private javax.swing.JList jList1;
    // End of variables declaration//GEN-END:variables
}
