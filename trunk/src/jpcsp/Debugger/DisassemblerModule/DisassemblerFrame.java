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

import com.jidesoft.list.StyledListCellRenderer;
import com.jidesoft.swing.StyleRange;
import com.jidesoft.swing.StyledLabel;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
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
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Settings;
import jpcsp.Debugger.MemoryViewer;
import jpcsp.Allegrex.Instructions.*;
import jpcsp.Allegrex.Decoder;
import jpcsp.Allegrex.CpuState;
import jpcsp.Allegrex.Common.Instruction;
import jpcsp.util.*;

/**
 *
 * @author  shadow
 */
public class DisassemblerFrame extends javax.swing.JFrame implements ClipboardOwner{
    private int DebuggerPC;
    private Emulator emu;
    private DefaultListModel listmodel = new DefaultListModel();
    private int opcode_address; // store the address of the opcode used for offsetdecode
    private ArrayList<Integer> breakpoints = new ArrayList<Integer>();
    private MemoryViewer memview;
    private boolean wantStep;
    private boolean haveFocus;
    private int gpi, gpo;

    /** Creates new form DisassemblerFrame */
    public DisassemblerFrame(Emulator emu) {
        this.emu=emu;
        listmodel = new DefaultListModel();
        initComponents();
        ViewTooltips.register(disasmList);
        DebuggerPC = 0;
            disasmList.setCellRenderer(new StyledListCellRenderer() {
            @Override
            protected void customizeStyledLabel(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.customizeStyledLabel(list, value, index, isSelected, cellHasFocus);
                String text = getText();
                setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
                setIcon(null);
                DisassemblerFrame.this.customizeStyledLabel(this, text);
            }
        });
        RefreshDebugger();
        
        
    }
    private void customizeStyledLabel(StyledLabel label, String text) {
        if(text.contains("<*>"))//breakpoint!
        {
            label.addStyleRange(new StyleRange(0, text.length(), Font.BOLD, Color.RED));     
        }
        if(text.startsWith("   0" + Integer.toHexString(Emulator.getProcessor().cpu.pc).toUpperCase()))
        {
             
             label.addStyleRange(new StyleRange(0, 12, Font.BOLD, Color.GREEN)); 
        }
        if(text.contains("[sce"))
        {
            int find = text.indexOf("[sce");
            label.addStyleRange(new StyleRange(find, -1, Font.PLAIN, Color.BLUE)); 
        }
        if(text.contains("<=>"))
        {
           int find = text.indexOf("<=>");
           label.addStyleRange(new StyleRange(find, -1, Font.PLAIN, Color.GRAY));  
        }
    }

    public void setMemoryViewer(MemoryViewer memview) {
        this.memview = memview;
    }

    /** Delete breakpoints and reset to PC */
    public void resetDebugger() {
        DeleteAllBreakpoints();
        DebuggerPC = 0;
        RefreshDebugger();
    }

    public void RefreshDebugger() {
        CpuState cpu = Emulator.getProcessor().cpu;
        int pc;
        int cnt;
        if (DebuggerPC == 0) {
            DebuggerPC = cpu.pc;
        }
        ViewTooltips.unregister(disasmList);
        synchronized(listmodel) {
	        listmodel.clear();
	
	        for (pc = DebuggerPC , cnt = 0; pc < (DebuggerPC + 0x00000094); pc += 0x00000004, cnt++) {
	            if (Memory.getInstance().isAddressGood(pc)) {
	                int opcode = Memory.getInstance().read32(pc);
	
	                Instruction insn = Decoder.instruction(opcode);
	
	                if(breakpoints.indexOf(pc)!=-1) {
	                    listmodel.addElement(String.format("<*>%08X:[%08X]: %s", pc, opcode, insn.disasm(pc, opcode)));
	                } else {
	                    listmodel.addElement(String.format("   %08X:[%08X]: %s", pc, opcode, insn.disasm(pc, opcode)));
	                }
	            } else {
	                listmodel.addElement(String.format("   %08x: invalid address", pc));
	            }
	        }
        }
        ViewTooltips.register(disasmList);
    //refreshregisters
        jTable1.setValueAt(Integer.toHexString(cpu.pc), 0, 1);
        jTable1.setValueAt(Integer.toHexString(cpu.getHi()), 1, 1);
        jTable1.setValueAt(Integer.toHexString(cpu.getLo()), 2, 1);
        for (int i = 0; i < 32; i++) {
            jTable1.setValueAt(Integer.toHexString(cpu.gpr[i]), 3 + i, 1);
        }
        for (int i = 0; i < 32; i++) {
            jTable3.setValueAt(cpu.fpr[i], i, 1);
        }
        VfpuFrame.getInstance().updateRegisters(cpu);
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
        RegMenu = new javax.swing.JPopupMenu();
        CopyValue = new javax.swing.JMenuItem();
        disasmList = new javax.swing.JList(listmodel);
        DisasmToolbar = new javax.swing.JToolBar();
        RunDebugger = new javax.swing.JToggleButton();
        PauseDebugger = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        StepInto = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JToolBar.Separator();
        ResetToPCbutton = new javax.swing.JButton();
        JumpToAddress = new javax.swing.JButton();
        jSeparator4 = new javax.swing.JToolBar.Separator();
        AddBreakpoint = new javax.swing.JButton();
        DeleteBreakpoint = new javax.swing.JButton();
        DeleteAllBreakpoints = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JToolBar.Separator();
        DumpCodeToText = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jTable1 = new javax.swing.JTable();
        jTable2 = new javax.swing.JTable();
        jTable3 = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        jToggleButton1 = new javax.swing.JToggleButton();
        jToggleButton2 = new javax.swing.JToggleButton();
        jToggleButton3 = new javax.swing.JToggleButton();
        jToggleButton4 = new javax.swing.JToggleButton();
        jToggleButton5 = new javax.swing.JToggleButton();
        jToggleButton6 = new javax.swing.JToggleButton();
        jToggleButton7 = new javax.swing.JToggleButton();
        jToggleButton8 = new javax.swing.JToggleButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();

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

        CopyValue.setText("Copy value");
        CopyValue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CopyValueActionPerformed(evt);
            }
        });
        RegMenu.add(CopyValue);

        setTitle("Debugger");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowDeactivated(java.awt.event.WindowEvent evt) {
                formWindowDeactivated(evt);
            }
        });

        disasmList.setFont(new java.awt.Font("Courier New", 0, 11));
        disasmList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        disasmList.setToolTipText("");
        disasmList.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                disasmListMouseWheelMoved(evt);
            }
        });
        disasmList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                disasmListMouseClicked(evt);
            }
        });
        disasmList.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                disasmListKeyPressed(evt);
            }
        });

        DisasmToolbar.setFloatable(false);
        DisasmToolbar.setRollover(true);

        RunDebugger.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/PlayIcon.png"))); // NOI18N
        RunDebugger.setText("Run");
        RunDebugger.setFocusable(false);
        RunDebugger.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        RunDebugger.setIconTextGap(2);
        RunDebugger.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        RunDebugger.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RunDebuggerActionPerformed(evt);
            }
        });
        DisasmToolbar.add(RunDebugger);

        PauseDebugger.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/PauseIcon.png"))); // NOI18N
        PauseDebugger.setText("Pause");
        PauseDebugger.setFocusable(false);
        PauseDebugger.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        PauseDebugger.setIconTextGap(2);
        PauseDebugger.setInheritsPopupMenu(true);
        PauseDebugger.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        PauseDebugger.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PauseDebuggerActionPerformed(evt);
            }
        });
        DisasmToolbar.add(PauseDebugger);
        DisasmToolbar.add(jSeparator1);

        StepInto.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/StepIntoIcon.png"))); // NOI18N
        StepInto.setText("Step Into");
        StepInto.setFocusable(false);
        StepInto.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        StepInto.setIconTextGap(2);
        StepInto.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        StepInto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StepIntoActionPerformed(evt);
            }
        });
        DisasmToolbar.add(StepInto);

        jButton2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/StepOverIcon.png"))); // NOI18N
        jButton2.setText("Step Over");
        jButton2.setFocusable(false);
        jButton2.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        jButton2.setIconTextGap(2);
        jButton2.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        DisasmToolbar.add(jButton2);

        jButton3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/StepOutIcon.png"))); // NOI18N
        jButton3.setText("Step Out");
        jButton3.setFocusable(false);
        jButton3.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        jButton3.setIconTextGap(2);
        jButton3.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        DisasmToolbar.add(jButton3);
        DisasmToolbar.add(jSeparator2);

        ResetToPCbutton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/ResetToPc.png"))); // NOI18N
        ResetToPCbutton.setText("Reset To PC");
        ResetToPCbutton.setFocusable(false);
        ResetToPCbutton.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        ResetToPCbutton.setIconTextGap(2);
        ResetToPCbutton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        ResetToPCbutton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ResetToPCbuttonActionPerformed(evt);
            }
        });
        DisasmToolbar.add(ResetToPCbutton);

        JumpToAddress.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/JumpTo.png"))); // NOI18N
        JumpToAddress.setText("Jump To");
        JumpToAddress.setFocusable(false);
        JumpToAddress.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        JumpToAddress.setIconTextGap(2);
        JumpToAddress.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        JumpToAddress.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                JumpToAddressActionPerformed(evt);
            }
        });
        DisasmToolbar.add(JumpToAddress);
        DisasmToolbar.add(jSeparator4);

        AddBreakpoint.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/NewBreakpointIcon.png"))); // NOI18N
        AddBreakpoint.setText("Add Break");
        AddBreakpoint.setFocusable(false);
        AddBreakpoint.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        AddBreakpoint.setIconTextGap(2);
        AddBreakpoint.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        AddBreakpoint.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AddBreakpointActionPerformed(evt);
            }
        });
        DisasmToolbar.add(AddBreakpoint);

        DeleteBreakpoint.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/DeleteBreakpointIcon.png"))); // NOI18N
        DeleteBreakpoint.setText("Delete Break");
        DeleteBreakpoint.setFocusable(false);
        DeleteBreakpoint.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        DeleteBreakpoint.setIconTextGap(2);
        DeleteBreakpoint.setInheritsPopupMenu(true);
        DeleteBreakpoint.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        DeleteBreakpoint.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DeleteBreakpointActionPerformed(evt);
            }
        });
        DisasmToolbar.add(DeleteBreakpoint);

        DeleteAllBreakpoints.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/DeleteAllBreakpointsIcon.png"))); // NOI18N
        DeleteAllBreakpoints.setText("DeleteAll");
        DeleteAllBreakpoints.setFocusable(false);
        DeleteAllBreakpoints.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        DeleteAllBreakpoints.setIconTextGap(2);
        DeleteAllBreakpoints.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        DeleteAllBreakpoints.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DeleteAllBreakpointsActionPerformed(evt);
            }
        });
        DisasmToolbar.add(DeleteAllBreakpoints);
        DisasmToolbar.add(jSeparator3);

        DumpCodeToText.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/Dump.png"))); // NOI18N
        DumpCodeToText.setText("Dump Code");
        DumpCodeToText.setFocusable(false);
        DumpCodeToText.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        DumpCodeToText.setIconTextGap(2);
        DumpCodeToText.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        DumpCodeToText.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DumpCodeToTextActionPerformed(evt);
            }
        });
        DisasmToolbar.add(DumpCodeToText);

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"PC", ""},
                {"HI", null},
                {"LO", null},
                {"zr", null},
                {"at", null},
                {"v0", null},
                {"v1", null},
                {"a0", null},
                {"a1", null},
                {"a2", null},
                {"a3", null},
                {"t0", null},
                {"t1", null},
                {"t2", null},
                {"t3", null},
                {"t4", null},
                {"t5", null},
                {"t6", null},
                {"t7", null},
                {"s0", null},
                {"s1", null},
                {"s2", null},
                {"s3", null},
                {"s4", null},
                {"s5", null},
                {"s6", null},
                {"s7", null},
                {"t8", null},
                {"t9", null},
                {"k0", null},
                {"k1", null},
                {"gp", null},
                {"sp", null},
                {"fp", null},
                {"ra", null}
            },
            new String [] {
                "REG", "HEX"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTable1.setColumnSelectionAllowed(true);
        jTable1.getTableHeader().setReorderingAllowed(false);
        jTable1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTable1MouseClicked(evt);
            }
        });
        jTabbedPane1.addTab("GPR", jTable1);

        jTable2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null},
                {null, null},
                {null, null},
                {null, null}
            },
            new String [] {
                "REG", "HEX"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTabbedPane1.addTab("COP0", jTable2);

        jTable3.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"FPR0", null},
                {"FPR1", null},
                {"FPR2", null},
                {"FPR3", null},
                {"FPR4", null},
                {"FPR5", null},
                {"FPR6", null},
                {"FPR7", null},
                {"FPR8", null},
                {"FPR9", null},
                {"FPR10", null},
                {"FPR11", null},
                {"FPR12", null},
                {"FPR13", null},
                {"FPR14", null},
                {"FPR15", null},
                {"FPR16", null},
                {"FPR17", null},
                {"FPR18", null},
                {"FPR19", null},
                {"FPR20", null},
                {"FPR21", null},
                {"FPR22", null},
                {"FPR23", null},
                {"FPR24", null},
                {"FPR25", null},
                {"FPR26", null},
                {"FPR27", null},
                {"FPR28", null},
                {"FPR29", null},
                {"FPR30", null},
                {"FPR31", null}
            },
            new String [] {
                "REG", "FLOAT"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Float.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTable3.setColumnSelectionAllowed(true);
        jTable3.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTable3MouseClicked(evt);
            }
        });
        jTabbedPane1.addTab("COP1", jTable3);

        jPanel1.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                onFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                onFocusLost(evt);
            }
        });

        jToggleButton1.setText("1");
        jToggleButton1.setBorder(null);
        jToggleButton1.setPreferredSize(new java.awt.Dimension(16, 16));
        jToggleButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton1ActionPerformed(evt);
            }
        });

        jToggleButton2.setText("2");
        jToggleButton2.setBorder(null);
        jToggleButton2.setPreferredSize(new java.awt.Dimension(16, 16));
        jToggleButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton2ActionPerformed(evt);
            }
        });

        jToggleButton3.setText("3");
        jToggleButton3.setBorder(null);
        jToggleButton3.setPreferredSize(new java.awt.Dimension(16, 16));
        jToggleButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton3ActionPerformed(evt);
            }
        });

        jToggleButton4.setText("4");
        jToggleButton4.setBorder(null);
        jToggleButton4.setPreferredSize(new java.awt.Dimension(16, 16));
        jToggleButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton4ActionPerformed(evt);
            }
        });

        jToggleButton5.setText("5");
        jToggleButton5.setBorder(null);
        jToggleButton5.setPreferredSize(new java.awt.Dimension(16, 16));
        jToggleButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton5ActionPerformed(evt);
            }
        });

        jToggleButton6.setText("6");
        jToggleButton6.setBorder(null);
        jToggleButton6.setPreferredSize(new java.awt.Dimension(16, 16));
        jToggleButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton6ActionPerformed(evt);
            }
        });

        jToggleButton7.setText("7");
        jToggleButton7.setBorder(null);
        jToggleButton7.setPreferredSize(new java.awt.Dimension(16, 16));
        jToggleButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton7ActionPerformed(evt);
            }
        });

        jToggleButton8.setText("8");
        jToggleButton8.setBorder(null);
        jToggleButton8.setPreferredSize(new java.awt.Dimension(16, 16));
        jToggleButton8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton8ActionPerformed(evt);
            }
        });

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/tick.gif"))); // NOI18N

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/tick.gif"))); // NOI18N

        jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/tick.gif"))); // NOI18N

        jLabel4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/tick.gif"))); // NOI18N

        jLabel5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/tick.gif"))); // NOI18N

        jLabel6.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/tick.gif"))); // NOI18N

        jLabel7.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/tick.gif"))); // NOI18N

        jLabel8.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/tick.gif"))); // NOI18N

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel8))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jToggleButton1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jToggleButton2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jToggleButton3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jToggleButton4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jToggleButton5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jToggleButton6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jToggleButton7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jToggleButton8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(20, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jLabel8)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4)
                    .addComponent(jLabel5)
                    .addComponent(jLabel6)
                    .addComponent(jLabel7))
                .addGap(11, 11, 11)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jToggleButton1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jToggleButton2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jToggleButton3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jToggleButton4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jToggleButton5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jToggleButton6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jToggleButton7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jToggleButton8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(505, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("GPIO", jPanel1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(DisasmToolbar, javax.swing.GroupLayout.DEFAULT_SIZE, 793, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(disasmList, javax.swing.GroupLayout.DEFAULT_SIZE, 533, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 223, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(19, 19, 19))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(DisasmToolbar, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(disasmList, javax.swing.GroupLayout.DEFAULT_SIZE, 588, Short.MAX_VALUE)
                    .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void disasmListKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_disasmListKeyPressed
    if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_DOWN && disasmList.getSelectedIndex() == disasmList.getLastVisibleIndex()) {
        DebuggerPC += 4;
        RefreshDebugger();
        evt.consume();
        disasmList.setSelectedIndex(disasmList.getLastVisibleIndex());
    } else if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_UP && disasmList.getSelectedIndex() == 0) {
        DebuggerPC -= 4;
        RefreshDebugger();
        evt.consume();
        disasmList.setSelectedIndex(0);
    } else if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_PAGE_UP && disasmList.getSelectedIndex() == 0) {
        DebuggerPC -= 0x00000094;
        RefreshDebugger();
        evt.consume();
        disasmList.setSelectedIndex(0);
    } else if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_PAGE_DOWN && disasmList.getSelectedIndex() == disasmList.getLastVisibleIndex()) {
        DebuggerPC += 0x00000094;
        RefreshDebugger();
        evt.consume();
        disasmList.setSelectedIndex(disasmList.getLastVisibleIndex());
    }
}//GEN-LAST:event_disasmListKeyPressed

private void disasmListMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_disasmListMouseWheelMoved
    if (evt.getWheelRotation() < 0) {
        evt.consume();
        if (disasmList.getSelectedIndex() == 0 || disasmList.getSelectedIndex() == -1) {
            DebuggerPC -= 4;
            RefreshDebugger();
            disasmList.setSelectedIndex(0);
        } else {
            disasmList.setSelectedIndex(disasmList.getSelectedIndex() - 1);
        }
    } else {
        evt.consume();
        if (disasmList.getSelectedIndex() == disasmList.getLastVisibleIndex()) {
            DebuggerPC += 4;
            RefreshDebugger();
            disasmList.setSelectedIndex(disasmList.getLastVisibleIndex());
        } else {
            disasmList.setSelectedIndex(disasmList.getSelectedIndex() + 1);
        }
    }
}//GEN-LAST:event_disasmListMouseWheelMoved

private void ResetToPCbuttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ResetToPCbuttonActionPerformed
    DebuggerPC = Emulator.getProcessor().cpu.pc;
    RefreshDebugger();
}//GEN-LAST:event_ResetToPCbuttonActionPerformed

private void JumpToAddressActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_JumpToAddressActionPerformed
    String input = (String) JOptionPane.showInputDialog(this, "Enter the address to which to jump (Hex)", "Jpcsp", JOptionPane.QUESTION_MESSAGE, null, null, String.format("%08x", Emulator.getProcessor().cpu.pc));
    if (input == null) {
        return;
    }
    int value=0;
         try {
            value = Utilities.parseAddress(input);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "The Number you enter is not correct");
            return;
        }
        DebuggerPC = value;
        RefreshDebugger();

}//GEN-LAST:event_JumpToAddressActionPerformed

private void DumpCodeToTextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DumpCodeToTextActionPerformed
    OptionPaneMultiple opt = new OptionPaneMultiple(this, Integer.toHexString(DebuggerPC), Integer.toHexString(DebuggerPC + 0x00000094));
    if(opt.completed()){
        //Here the input can be used to actually dump code
    	Emulator.log.debug("Start address: "+opt.getInput()[0]);
    	Emulator.log.debug("End address: "+opt.getInput()[1]);
    	Emulator.log.debug("File name: "+opt.getInput()[2]);

        BufferedWriter bufferedWriter = null;
        try {

            //Construct the BufferedWriter object
            bufferedWriter = new BufferedWriter(new FileWriter(opt.getInput()[2]));

            //Start writing to the output stream
            bufferedWriter.write("-------JPCSP DISASM-----------");
            bufferedWriter.newLine();
            int Start = Utilities.parseAddress(opt.getInput()[0]);
            int End = Utilities.parseAddress(opt.getInput()[1]);
            for(int i =Start; i<=End; i+=4)
            {
                if (Memory.getInstance().isAddressGood(i)) {
                    int opcode = Memory.getInstance().read32(i);

                    Instruction insn = Decoder.instruction(opcode);

                    bufferedWriter.write(String.format("%08x:[%08x]: %s", i, opcode, insn.disasm(i, opcode)));
                    bufferedWriter.newLine();
                } else {
                    // Should we even both printing these?
                    bufferedWriter.write(String.format("%08x: invalid address", i));
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
}//GEN-LAST:event_DumpCodeToTextActionPerformed



//Following methods are for the JPopmenu in Jlist
private void CopyAddressActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CopyAddressActionPerformed
    String value = (String)disasmList.getSelectedValue();
    String address = value.substring(3, 11);
    StringSelection stringSelection = new StringSelection( address);
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    clipboard.setContents(stringSelection, this);
}//GEN-LAST:event_CopyAddressActionPerformed

private void CopyAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CopyAllActionPerformed
String value = (String)disasmList.getSelectedValue();
    StringSelection stringSelection = new StringSelection( value);
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    clipboard.setContents(stringSelection, this);
}//GEN-LAST:event_CopyAllActionPerformed

private void BranchOrJumpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BranchOrJumpActionPerformed
String value = (String)disasmList.getSelectedValue();
    int address = value.indexOf("0x");
    if(address==-1)
    {
      JpcspDialogManager.showError(this, "Can't find the jump or branch address");
      return;
    }
    else
    {
      String add = value.substring(address+2,value.length());

      // Remove syscall code, if present
      int addressend = add.indexOf(" ");
      if (addressend != -1)
        add = add.substring(0, addressend);

      StringSelection stringSelection = new StringSelection(add);
      Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      clipboard.setContents(stringSelection, this);

    }
}//GEN-LAST:event_BranchOrJumpActionPerformed
    @Override
public void lostOwnership( Clipboard aClipboard, Transferable aContents) {
     //do nothing
 }
private void disasmListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_disasmListMouseClicked
      
       BranchOrJump.setEnabled(false);
       if (SwingUtilities.isRightMouseButton(evt) && !disasmList.isSelectionEmpty() && disasmList.locationToIndex(evt.getPoint()) == disasmList.getSelectedIndex())
       {
           //check if we can enable branch or jump address copy
           String line = (String)disasmList.getSelectedValue();
           int finddot = line.indexOf("]:");
           String opcode = line.substring(finddot+3,line.length());
           if(opcode.startsWith("b") || opcode.startsWith("j"))//it is definately a branch or jump opcode
           {
               BranchOrJump.setEnabled(true);
           }
            DisMenu.show(disasmList, evt.getX(), evt.getY());
       }
}//GEN-LAST:event_disasmListMouseClicked

private void AddBreakpointActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AddBreakpointActionPerformed
    String value =(String)disasmList.getSelectedValue();
    if (value != null) {
        try {
            String address = value.substring(3, 11);
            int addr = Utilities.parseAddress(address);
            if(!breakpoints.contains(addr))
            	breakpoints.add(addr);
            RefreshDebugger();
        } catch(NumberFormatException e) {
            // Ignore it, probably already a breakpoint there
        }
    } else {
        JpcspDialogManager.showInformation(this, "Breakpoint Help : " + "Select the line to add a breakpoint to.");
    }
}//GEN-LAST:event_AddBreakpointActionPerformed

private void DeleteAllBreakpointsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DeleteAllBreakpointsActionPerformed
    DeleteAllBreakpoints();

    // Move this call to DeleteAllBreakpoints()?
    RefreshDebugger();
}//GEN-LAST:event_DeleteAllBreakpointsActionPerformed

public void DeleteAllBreakpoints() {
    if (!breakpoints.isEmpty())
        breakpoints.clear();
}

private void DeleteBreakpointActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DeleteBreakpointActionPerformed
          String value =(String)disasmList.getSelectedValue();
          if(value != null)
          {
            boolean breakpointexists = value.startsWith("<*>");
            if(breakpointexists)
            {
              String address = value.substring(3, 11);
              int addr = Utilities.parseAddress(address);
              int b = breakpoints.indexOf(addr);
              breakpoints.remove(b);
              RefreshDebugger();
            }
          }
          else
          {
            JpcspDialogManager.showInformation(this, "Breakpoint Help : " + "Select the line to remove a breakpoint from.");
          }
}//GEN-LAST:event_DeleteBreakpointActionPerformed

private void StepIntoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StepIntoActionPerformed
    wantStep = true;
    emu.RunEmu();
}//GEN-LAST:event_StepIntoActionPerformed

private void RunDebuggerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RunDebuggerActionPerformed
    emu.RunEmu();
}//GEN-LAST:event_RunDebuggerActionPerformed

// Called from Emulator
public void step() {
    //check if there is a breakpoint
    if (wantStep || (breakpoints.size() > 0 && breakpoints.indexOf(Emulator.getProcessor().cpu.pc) != -1)) {
    	wantStep = false;
        Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_BREAKPOINT);

        DebuggerPC = 0;
        RefreshDebugger();
        RefreshButtons();
        if (memview != null)
            memview.RefreshMemory();
    }
}

private void PauseDebuggerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PauseDebuggerActionPerformed
    Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_PAUSE);

    DebuggerPC = 0;
    RefreshDebugger();
}//GEN-LAST:event_PauseDebuggerActionPerformed

// Called from Emulator
public void RefreshButtons() {
    RunDebugger.setSelected(emu.run && !emu.pause);
    PauseDebugger.setSelected(emu.run && emu.pause);
}

private void formWindowDeactivated(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowDeactivated
    //Called when the mainWindow is closed
    if (Settings.getInstance().readBool("gui.saveWindowPos"))
        Settings.getInstance().writeWindowPos("disassembler", getLocation());
}//GEN-LAST:event_formWindowDeactivated
private boolean isCellChecked(JTable table)
{
  for(int i=0; i<table.getRowCount(); i++)
  {
       if(table.isCellSelected(i, 1)) return true;

  }
  return false;
}
private void jTable3MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTable3MouseClicked
   if (SwingUtilities.isRightMouseButton(evt) && jTable3.isColumnSelected(1) && isCellChecked(jTable3))
   {
     RegMenu.show(jTable3, evt.getX(), evt.getY());
   }
}//GEN-LAST:event_jTable3MouseClicked

private void CopyValueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CopyValueActionPerformed
 if(jTable3.isShowing()){
    float value = (Float)jTable3.getValueAt(jTable3.getSelectedRow(),1);
    StringSelection stringSelection = new StringSelection( Float.toString(value));
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    clipboard.setContents(stringSelection, this);
 }
 else if(jTable1.isShowing())
 {
    String value = (String)jTable1.getValueAt(jTable1.getSelectedRow(),1);
    StringSelection stringSelection = new StringSelection(value);
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    clipboard.setContents(stringSelection, this);
 }
}//GEN-LAST:event_CopyValueActionPerformed

private void jTable1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTable1MouseClicked
   if (SwingUtilities.isRightMouseButton(evt) && jTable1.isColumnSelected(1) && isCellChecked(jTable1))
   {
     RegMenu.show(jTable1, evt.getX(), evt.getY());
   }
}//GEN-LAST:event_jTable1MouseClicked

public int GetGPI() {
    return gpi;
}

public void SetGPO(int gpo) {
    this.gpo = gpo;
    if (haveFocus) {
        // Refresh GPO
        for(int i = 0; i < 8; i++)
            SetGPO(i, (gpo & (1 << i)) != 0);
    }
}

private void ToggleGPI(int index) {
    gpi ^= 1 << index;

    // Refresh GPI buttons
    for(int i = 0; i < 8; i++)
        SetGPI(i, (gpi & (1 << i)) != 0);
}

private void SetGPO(int index, boolean on) {
    switch(index) {
        case 0: jLabel1.setVisible(on); break;
        case 1: jLabel2.setVisible(on); break;
        case 2: jLabel3.setVisible(on); break;
        case 3: jLabel4.setVisible(on); break;
        case 4: jLabel5.setVisible(on); break;
        case 5: jLabel6.setVisible(on); break;
        case 6: jLabel7.setVisible(on); break;
        case 7: jLabel8.setVisible(on); break;
    }
}

private void SetGPI(int index, boolean on) {
    switch(index) {
        case 0: jToggleButton1.setSelected(on); break;
        case 1: jToggleButton2.setSelected(on); break;
        case 2: jToggleButton3.setSelected(on); break;
        case 3: jToggleButton4.setSelected(on); break;
        case 4: jToggleButton5.setSelected(on); break;
        case 5: jToggleButton6.setSelected(on); break;
        case 6: jToggleButton7.setSelected(on); break;
        case 7: jToggleButton8.setSelected(on); break;
    }
}

private void jToggleButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton1ActionPerformed
    ToggleGPI(0);
}//GEN-LAST:event_jToggleButton1ActionPerformed

private void jToggleButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton2ActionPerformed
    ToggleGPI(1);
}//GEN-LAST:event_jToggleButton2ActionPerformed

private void jToggleButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton3ActionPerformed
    ToggleGPI(2);
}//GEN-LAST:event_jToggleButton3ActionPerformed

private void jToggleButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton4ActionPerformed
    ToggleGPI(3);
}//GEN-LAST:event_jToggleButton4ActionPerformed

private void jToggleButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton5ActionPerformed
    ToggleGPI(4);
}//GEN-LAST:event_jToggleButton5ActionPerformed

private void jToggleButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton6ActionPerformed
    ToggleGPI(5);
}//GEN-LAST:event_jToggleButton6ActionPerformed

private void jToggleButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton7ActionPerformed
    ToggleGPI(6);
}//GEN-LAST:event_jToggleButton7ActionPerformed

private void jToggleButton8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton8ActionPerformed
    ToggleGPI(7);
}//GEN-LAST:event_jToggleButton8ActionPerformed

private void onFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_onFocusGained
    haveFocus = true;
}//GEN-LAST:event_onFocusGained

private void onFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_onFocusLost
    haveFocus = false;
}//GEN-LAST:event_onFocusLost

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton AddBreakpoint;
    private javax.swing.JMenuItem BranchOrJump;
    private javax.swing.JMenuItem CopyAddress;
    private javax.swing.JMenuItem CopyAll;
    private javax.swing.JMenuItem CopyValue;
    private javax.swing.JButton DeleteAllBreakpoints;
    private javax.swing.JButton DeleteBreakpoint;
    private javax.swing.JPopupMenu DisMenu;
    private javax.swing.JToolBar DisasmToolbar;
    private javax.swing.JButton DumpCodeToText;
    private javax.swing.JButton JumpToAddress;
    private javax.swing.JButton PauseDebugger;
    private javax.swing.JPopupMenu RegMenu;
    private javax.swing.JButton ResetToPCbutton;
    private javax.swing.JToggleButton RunDebugger;
    private javax.swing.JButton StepInto;
    private javax.swing.JList disasmList;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar.Separator jSeparator2;
    private javax.swing.JToolBar.Separator jSeparator3;
    private javax.swing.JToolBar.Separator jSeparator4;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JTable jTable2;
    private javax.swing.JTable jTable3;
    private javax.swing.JToggleButton jToggleButton1;
    private javax.swing.JToggleButton jToggleButton2;
    private javax.swing.JToggleButton jToggleButton3;
    private javax.swing.JToggleButton jToggleButton4;
    private javax.swing.JToggleButton jToggleButton5;
    private javax.swing.JToggleButton jToggleButton6;
    private javax.swing.JToggleButton jToggleButton7;
    private javax.swing.JToggleButton jToggleButton8;
    // End of variables declaration//GEN-END:variables

}
