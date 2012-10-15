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

import static jpcsp.Allegrex.Common.gprNames;

import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Resource;
import jpcsp.State;
import jpcsp.Allegrex.CpuState;
import jpcsp.Allegrex.Decoder;
import jpcsp.Allegrex.Common.Instruction;
import jpcsp.Debugger.DumpDebugState;
import jpcsp.settings.Settings;
import jpcsp.util.JpcspDialogManager;
import jpcsp.util.OptionPaneMultiple;
import jpcsp.util.Utilities;

import com.jidesoft.list.StyledListCellRenderer;
import com.jidesoft.swing.StyleRange;
import com.jidesoft.swing.StyledLabel;

/**
 *
 * @author  shadow
 */
public class DisassemblerFrame extends javax.swing.JFrame implements ClipboardOwner{
	private static final long serialVersionUID = -8481807175706172292L;
	private int DebuggerPC;
    private int SelectedPC;
    private Emulator emu;
    private DefaultListModel listmodel = new DefaultListModel();
    private ArrayList<Integer> breakpoints = new ArrayList<Integer>();
    private volatile boolean wantStep;
    protected int gpi, gpo;

    private int selectedRegCount;
    private final Color[] selectedRegColors = new Color[] { new Color(128, 255, 255), new Color(255, 255, 128), new Color(128, 255, 128) };
    private String[] selectedRegNames = new String[selectedRegColors.length];
    private final Color selectedAddressColor = new Color(255, 128, 255);
    private String selectedAddress;

    private int srcounter;

    /** Creates new form DisassemblerFrame */
    public DisassemblerFrame(Emulator emu) {
        this.emu=emu;
        listmodel = new DefaultListModel();
        initComponents();
        ViewTooltips.register(disasmList);
        disasmList.setCellRenderer(new StyledListCellRenderer() {
			private static final long serialVersionUID = 3921020228217850610L;

			@Override
            protected void customizeStyledLabel(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.customizeStyledLabel(list, value, index, isSelected, cellHasFocus);
                String text = getText();
                setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
                setIcon(null);
                // highlight the selected line
                if (index == disasmListGetSelectedIndex()) {
                    setBackground(Color.LIGHT_GRAY);
                }
                DisassemblerFrame.this.customizeStyledLabel(this, text);
            }
        });
        disasmList.addListSelectionListener(new ListSelectionListener() {
            @Override
			public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    // this is the only place we can use disasmList.getSelectedValue(),
                    // all other places should go through disasmListGetSelectedValue()
                    String text = (String)disasmList.getSelectedValue();
                    if (text != null) {
                        // this is the only place we can use disasmList.getSelectedIndex(),
                        // all other places should go through disasmListGetSelectedIndex()
                        SelectedPC = DebuggerPC + disasmList.getSelectedIndex() * 4;
                        DisassemblerFrame.this.updateSelectedRegisters(text);
                        disasmList.clearSelection();
                        disasmList.repaint();
                    }
                }
            }
        });

        RefreshDebugger(true);
        wantStep = false;
    }

    private void customizeStyledLabel(StyledLabel label, String text) {
        // breakpoint
        if (text.startsWith("<*>")) {
            label.addStyleRange(new StyleRange(0, 3, Font.BOLD, Color.RED));
        }

        // PC line highlighting
        // TODO highlight entire line except for breakpoint highlighted registers
        // it seems the longest style overrides any shorter styles (such as the register highlighting)
        if (text.contains(String.format("%08X:", Emulator.getProcessor().cpu.pc))) {
            // highlight: entire line, except gutter
            //label.addStyleRange(new StyleRange(3, -1, Font.BOLD, Color.BLACK));

            // highlight: address, raw opcode, opcode. no operands.
            int length = 32;
            if (length > text.length() - 3)
                    length = text.length() - 3;

            label.addStyleRange(new StyleRange(3, length, Font.BOLD, Color.BLACK));
            // testing label.addStyleRange(new StyleRange(3, length, Font.PLAIN, Color.RED, Color.GREEN, 0));

            // highlight gutter if there is no breakpoint
            if(!text.startsWith("<*>"))
            {
                label.addStyleRange(new StyleRange(0, 3, Font.BOLD, Color.BLACK, Color.YELLOW, 0));
            }
        }

        // selected line highlighting
        /* moved to cell renderer, we can highlight the entire line independantly of StyleRange
        else if (text.contains(String.format("%08X:", SelectedPC))) {
            // highlight gutter if there is no breakpoint
            if(!text.startsWith("<*>"))
            {
                label.addStyleRange(new StyleRange(0, 3, Font.BOLD, Color.BLACK, Color.LIGHT_GRAY, 0));
            }
        }
        */

        // syscall highlighting
        if (text.contains(" [")) {
            int find = text.indexOf(" [");
            label.addStyleRange(new StyleRange(find, -1, Font.PLAIN, Color.BLUE));
        }

        // alias highlighting
        if (text.contains("<=>")) {
            int find = text.indexOf("<=>");
            label.addStyleRange(new StyleRange(find, -1, Font.PLAIN, Color.GRAY));
        }

        // address highlighting
        if (selectedAddress != null && text.contains("0x" + selectedAddress) && !text.contains("syscall")) {
            int find = text.indexOf("0x" + selectedAddress);
            label.addStyleRange(new StyleRange(find, 10, Font.PLAIN, Color.BLACK, selectedAddressColor, 0));
        } else if (selectedAddress != null && text.contains(selectedAddress) && !text.contains("syscall")) {
            int find = text.indexOf(selectedAddress);
            label.addStyleRange(new StyleRange(find, 8, Font.PLAIN, Color.BLACK, selectedAddressColor, 0));
        }

        // register highlighting
        int lastfind = 0;
        // find register in disassembly
        while ((lastfind = text.indexOf("$", lastfind)) != -1) {

            String regName = text.substring(lastfind);
            for (int i = 0; i < gprNames.length; i++) {
                // we still need to check every possible register because a tracked register may not be the first operand
                if (!regName.startsWith(gprNames[i])) {
                    continue;
                }
                // check for tracked register
                for (int j = 0; j < selectedRegCount; j++) {
                    if (regName.startsWith(selectedRegNames[j])) {
                        label.addStyleRange(new StyleRange(lastfind, 3, Font.PLAIN, Color.BLACK, selectedRegColors[j], 0));
                    }
                }
                break;
            }
            // move on to the remainder of the disassembled line on the next iteration
            lastfind += 3;
        }
    }

    /** Delete breakpoints and reset to PC */
    public void resetDebugger() {
        DeleteAllBreakpoints();
        RefreshDebugger(true);
    }

    public void SafeRefreshDebugger(final boolean moveToPC) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                RefreshDebugger(moveToPC);
            }
        });
    }

    public void RefreshDebugger(boolean moveToPC) {
        CpuState cpu = Emulator.getProcessor().cpu;
        int pc;

        if (moveToPC) {
            DebuggerPC = cpu.pc;
        }

        ViewTooltips.unregister(disasmList);
        synchronized(listmodel) {
            listmodel.clear();

            for (pc = DebuggerPC; pc < (DebuggerPC + 0x00000094); pc += 0x00000004) {
                if (Memory.isAddressGood(pc)) {
                    int opcode = Memory.getInstance().read32(pc);

                    Instruction insn = Decoder.instruction(opcode);

                    String line;
                    if(breakpoints.indexOf(pc) != -1) {
                        line = String.format("<*>%08X:[%08X]: %s", pc, opcode, insn.disasm(pc, opcode));
                    } else if (pc == cpu.pc) {
                        line = String.format("-->%08X:[%08X]: %s", pc, opcode, insn.disasm(pc, opcode));
                    } else {
                        line = String.format("   %08X:[%08X]: %s", pc, opcode, insn.disasm(pc, opcode));
                    }
                    listmodel.addElement(line);

                    // update register highlighting
                    if (pc == SelectedPC) {
                        updateSelectedRegisters(line);
                    }

                } else {
                    listmodel.addElement(String.format("   %08x: invalid address", pc));
                }
            }
        }
        ViewTooltips.register(disasmList);

        // refresh registers
        // gpr
        gprTable.setValueAt(Integer.toHexString(cpu.pc), 0, 1);
        gprTable.setValueAt(Integer.toHexString(cpu.getHi()), 1, 1);
        gprTable.setValueAt(Integer.toHexString(cpu.getLo()), 2, 1);
        for (int i = 0; i < 32; i++) {
            gprTable.setValueAt(Integer.toHexString(cpu.getRegister(i)), 3 + i, 1);
        }

        // fpr
        for (int i = 0; i < 32; i++) {
            cop1Table.setValueAt(cpu.fpr[i], i, 1);
        }

        // vfpu
        VfpuFrame.getInstance().updateRegisters(cpu);
    }

    private void updateSelectedRegisters(String text) {

        // selected address (highlight constant branch/jump addresses)
        selectedAddress = null;
        int find = text.indexOf(" 0x");
        if (find != -1 && (find + 11) <= text.length() && text.charAt(find + 7) != ' ') {
            selectedAddress = text.substring(find + 3, find + 3 + 8);
        }

        selectedRegCount = 0; // clear tracked registers
        int lastFind = 0;
        while ((lastFind = text.indexOf("$", lastFind)) != -1 && selectedRegCount < selectedRegColors.length) {

            // find register in disassembly
            String regName = text.substring(lastFind);
            for (int i = 0; i < gprNames.length; i++) {
                if (!regName.startsWith(gprNames[i])) {
                    continue;
                }
                // check if we are already tracking this register
                boolean found = false;
                for (int j = 0; j < selectedRegCount && !found; j++) {
                    found = regName.startsWith(selectedRegNames[j]);
                }

                // start tracking this register
                if (!found) {
                    selectedRegNames[selectedRegCount] = gprNames[i];
                    selectedRegCount++;
                }
                break;
            }
            // move on to the remainder of the disassembled line
            lastFind += 3;
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
        SetPCToCursor = new javax.swing.JMenuItem();
        RegMenu = new javax.swing.JPopupMenu();
        CopyValue = new javax.swing.JMenuItem();
        disasmList = new javax.swing.JList(listmodel);
        disasmToolbar = new javax.swing.JToolBar();
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
        DumpCodeToText = new javax.swing.JButton();
        disasmTabs = new javax.swing.JTabbedPane();
        gprTable = new javax.swing.JTable();
        cop0Table = new javax.swing.JTable();
        cop1Table = new javax.swing.JTable();
        miscPanel = new javax.swing.JPanel();
        gpiButton1 = new javax.swing.JToggleButton();
        gpiButton2 = new javax.swing.JToggleButton();
        gpiButton3 = new javax.swing.JToggleButton();
        gpiButton4 = new javax.swing.JToggleButton();
        gpiButton5 = new javax.swing.JToggleButton();
        gpiButton6 = new javax.swing.JToggleButton();
        gpiButton7 = new javax.swing.JToggleButton();
        gpiButton8 = new javax.swing.JToggleButton();
        gpoLabel1 = new javax.swing.JLabel();
        gpoLabel2 = new javax.swing.JLabel();
        gpoLabel3 = new javax.swing.JLabel();
        gpoLabel4 = new javax.swing.JLabel();
        gpoLabel5 = new javax.swing.JLabel();
        gpoLabel6 = new javax.swing.JLabel();
        gpoLabel7 = new javax.swing.JLabel();
        gpoLabel8 = new javax.swing.JLabel();
        gpioLabel = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        captureButton = new javax.swing.JButton();
        replayButton = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        dumpDebugStateButton = new javax.swing.JButton();
        SearchField = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        disasmToolbar2 = new javax.swing.JToolBar();
        AddBreakpoint = new javax.swing.JButton();
        DeleteBreakpoint = new javax.swing.JButton();
        DeleteAllBreakpoints = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JToolBar.Separator();
        ExportBreaks = new javax.swing.JButton();
        ImportBreaks = new javax.swing.JButton();
        jSeparator5 = new javax.swing.JSeparator();
        jSeparator6 = new javax.swing.JSeparator();

        CopyAddress.setText("Copy Address");
        CopyAddress.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CopyAddressActionPerformed(evt);
            }
        });
        DisMenu.add(CopyAddress);

        CopyAll.setText("Copy All");
        CopyAll.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CopyAllActionPerformed(evt);
            }
        });
        DisMenu.add(CopyAll);

        BranchOrJump.setText("Copy Branch Or Jump address");
        BranchOrJump.setEnabled(false); //disable as default
        BranchOrJump.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BranchOrJumpActionPerformed(evt);
            }
        });
        DisMenu.add(BranchOrJump);

        SetPCToCursor.setText("Set PC to Cursor");
        SetPCToCursor.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SetPCToCursorActionPerformed(evt);
            }
        });
        DisMenu.add(SetPCToCursor);

        CopyValue.setText("Copy value");
        CopyValue.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CopyValueActionPerformed(evt);
            }
        });
        RegMenu.add(CopyValue);

        setTitle("Debugger");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
			public void windowDeactivated(java.awt.event.WindowEvent evt) {
                formWindowDeactivated(evt);
            }
        });

        disasmList.setFont(new java.awt.Font("Courier New", 0, 11));
        disasmList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        disasmList.setToolTipText("");
        disasmList.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            @Override
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                disasmListMouseWheelMoved(evt);
            }
        });
        disasmList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
			public void mouseClicked(java.awt.event.MouseEvent evt) {
                disasmListMouseClicked(evt);
            }
        });
        disasmList.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
			public void keyPressed(java.awt.event.KeyEvent evt) {
                disasmListKeyPressed(evt);
            }
        });

        disasmToolbar.setFloatable(false);
        disasmToolbar.setRollover(true);

        RunDebugger.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/PlayIcon.png"))); // NOI18N
        RunDebugger.setMnemonic('R');
        RunDebugger.setText(Resource.get("run"));
        RunDebugger.setFocusable(false);
        RunDebugger.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        RunDebugger.setIconTextGap(2);
        RunDebugger.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        RunDebugger.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RunDebuggerActionPerformed(evt);
            }
        });
        disasmToolbar.add(RunDebugger);

        PauseDebugger.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/PauseIcon.png"))); // NOI18N
        PauseDebugger.setMnemonic('P');
        PauseDebugger.setText(Resource.get("pause"));
        PauseDebugger.setFocusable(false);
        PauseDebugger.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        PauseDebugger.setIconTextGap(2);
        PauseDebugger.setInheritsPopupMenu(true);
        PauseDebugger.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        PauseDebugger.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PauseDebuggerActionPerformed(evt);
            }
        });
        disasmToolbar.add(PauseDebugger);
        disasmToolbar.add(jSeparator1);

        StepInto.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/StepIntoIcon.png"))); // NOI18N
        StepInto.setText(Resource.get("stepinto"));
        StepInto.setFocusable(false);
        StepInto.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        StepInto.setIconTextGap(2);
        StepInto.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        StepInto.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StepIntoActionPerformed(evt);
            }
        });
        disasmToolbar.add(StepInto);

        jButton2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/StepOverIcon.png"))); // NOI18N
        jButton2.setText(Resource.get("stepover"));
        jButton2.setEnabled(false);
        jButton2.setFocusable(false);
        jButton2.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        jButton2.setIconTextGap(2);
        jButton2.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        disasmToolbar.add(jButton2);

        jButton3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/StepOutIcon.png"))); // NOI18N
        jButton3.setText(Resource.get("stepout"));
        jButton3.setEnabled(false);
        jButton3.setFocusable(false);
        jButton3.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        jButton3.setIconTextGap(2);
        jButton3.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });
        disasmToolbar.add(jButton3);
        disasmToolbar.add(jSeparator2);

        ResetToPCbutton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/ResetToPc.png"))); // NOI18N
        ResetToPCbutton.setMnemonic('P');
        ResetToPCbutton.setText(Resource.get("resettopc"));
        ResetToPCbutton.setFocusable(false);
        ResetToPCbutton.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        ResetToPCbutton.setIconTextGap(2);
        ResetToPCbutton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        ResetToPCbutton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ResetToPCbuttonActionPerformed(evt);
            }
        });
        disasmToolbar.add(ResetToPCbutton);

        JumpToAddress.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/JumpTo.png"))); // NOI18N
        JumpToAddress.setMnemonic('J');
        JumpToAddress.setText(Resource.get("jumpto"));
        JumpToAddress.setFocusable(false);
        JumpToAddress.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        JumpToAddress.setIconTextGap(2);
        JumpToAddress.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        JumpToAddress.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                JumpToAddressActionPerformed(evt);
            }
        });
        disasmToolbar.add(JumpToAddress);
        disasmToolbar.add(jSeparator4);

        DumpCodeToText.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/Dump.png"))); // NOI18N
        DumpCodeToText.setMnemonic('W');
        DumpCodeToText.setText(Resource.get("dumpcode"));
        DumpCodeToText.setFocusable(false);
        DumpCodeToText.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        DumpCodeToText.setIconTextGap(2);
        DumpCodeToText.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        DumpCodeToText.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DumpCodeToTextActionPerformed(evt);
            }
        });
        disasmToolbar.add(DumpCodeToText);

        gprTable.setModel(new javax.swing.table.DefaultTableModel(
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
			private static final long serialVersionUID = 4714824805211201111L;
			@SuppressWarnings("rawtypes")
			Class[] types = new Class [] {
                java.lang.String.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
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
        gprTable.setColumnSelectionAllowed(true);
        gprTable.getTableHeader().setReorderingAllowed(false);
        gprTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
			public void mouseClicked(java.awt.event.MouseEvent evt) {
                gprTableMouseClicked(evt);
            }
        });
        disasmTabs.addTab("GPR", gprTable);

        cop0Table.setModel(new javax.swing.table.DefaultTableModel(
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
			private static final long serialVersionUID = 1080691380828614427L;
			@SuppressWarnings("rawtypes")
			Class[] types = new Class [] {
                java.lang.String.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
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
        disasmTabs.addTab("COP0", cop0Table);

        cop1Table.setModel(new javax.swing.table.DefaultTableModel(
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
			private static final long serialVersionUID = -5902668243370431997L;
			@SuppressWarnings("rawtypes")
			Class[] types = new Class [] {
                java.lang.String.class, java.lang.Float.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
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
        cop1Table.setColumnSelectionAllowed(true);
        cop1Table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
			public void mouseClicked(java.awt.event.MouseEvent evt) {
                cop1TableMouseClicked(evt);
            }
        });
        disasmTabs.addTab("COP1", cop1Table);

        gpiButton1.setText("1");
        gpiButton1.setBorder(null);
        gpiButton1.setPreferredSize(new java.awt.Dimension(16, 16));
        gpiButton1.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gpiButton1ActionPerformed(evt);
            }
        });

        gpiButton2.setText("2");
        gpiButton2.setBorder(null);
        gpiButton2.setPreferredSize(new java.awt.Dimension(16, 16));
        gpiButton2.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gpiButton2ActionPerformed(evt);
            }
        });

        gpiButton3.setText("3");
        gpiButton3.setBorder(null);
        gpiButton3.setPreferredSize(new java.awt.Dimension(16, 16));
        gpiButton3.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gpiButton3ActionPerformed(evt);
            }
        });

        gpiButton4.setText("4");
        gpiButton4.setBorder(null);
        gpiButton4.setPreferredSize(new java.awt.Dimension(16, 16));
        gpiButton4.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gpiButton4ActionPerformed(evt);
            }
        });

        gpiButton5.setText("5");
        gpiButton5.setBorder(null);
        gpiButton5.setPreferredSize(new java.awt.Dimension(16, 16));
        gpiButton5.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gpiButton5ActionPerformed(evt);
            }
        });

        gpiButton6.setText("6");
        gpiButton6.setBorder(null);
        gpiButton6.setPreferredSize(new java.awt.Dimension(16, 16));
        gpiButton6.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gpiButton6ActionPerformed(evt);
            }
        });

        gpiButton7.setText("7");
        gpiButton7.setBorder(null);
        gpiButton7.setPreferredSize(new java.awt.Dimension(16, 16));
        gpiButton7.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gpiButton7ActionPerformed(evt);
            }
        });

        gpiButton8.setText("8");
        gpiButton8.setBorder(null);
        gpiButton8.setPreferredSize(new java.awt.Dimension(16, 16));
        gpiButton8.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gpiButton8ActionPerformed(evt);
            }
        });

        gpoLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/tick.gif"))); // NOI18N
        gpoLabel1.setEnabled(false);

        gpoLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/tick.gif"))); // NOI18N
        gpoLabel2.setEnabled(false);

        gpoLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/tick.gif"))); // NOI18N
        gpoLabel3.setEnabled(false);

        gpoLabel4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/tick.gif"))); // NOI18N
        gpoLabel4.setEnabled(false);

        gpoLabel5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/tick.gif"))); // NOI18N
        gpoLabel5.setEnabled(false);

        gpoLabel6.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/tick.gif"))); // NOI18N
        gpoLabel6.setEnabled(false);

        gpoLabel7.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/tick.gif"))); // NOI18N
        gpoLabel7.setEnabled(false);

        gpoLabel8.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/tick.gif"))); // NOI18N
        gpoLabel8.setEnabled(false);

        gpioLabel.setText("GPI/GPO");

        jLabel1.setText("GE Capture/Replay");

        captureButton.setText(Resource.get("capturenextframe"));
        captureButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                captureButtonActionPerformed(evt);
            }
        });

        replayButton.setText(Resource.get("replaycapturenextframe"));
        replayButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                replayButtonActionPerformed(evt);
            }
        });

        jLabel2.setText(Resource.get("dumpdebugstate"));

        dumpDebugStateButton.setText(Resource.get("dumptoconsole"));
        dumpDebugStateButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dumpDebugStateButtonActionPerformed(evt);
            }
        });

        SearchField.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SearchFieldActionPerformed(evt);
            }
        });

        jLabel3.setText(Resource.get("search"));

        javax.swing.GroupLayout miscPanelLayout = new javax.swing.GroupLayout(miscPanel);
        miscPanel.setLayout(miscPanelLayout);
        miscPanelLayout.setHorizontalGroup(
            miscPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(miscPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(miscPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(miscPanelLayout.createSequentialGroup()
                        .addComponent(SearchField, javax.swing.GroupLayout.DEFAULT_SIZE, 229, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(miscPanelLayout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addContainerGap())
                    .addGroup(miscPanelLayout.createSequentialGroup()
                        .addComponent(gpioLabel)
                        .addContainerGap(197, Short.MAX_VALUE))
                    .addGroup(miscPanelLayout.createSequentialGroup()
                        .addGroup(miscPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(gpoLabel1)
                            .addComponent(gpiButton1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(miscPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(gpiButton2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(gpoLabel2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(miscPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(gpiButton3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(gpoLabel3))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(miscPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(gpiButton4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(gpoLabel4))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(miscPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(gpiButton5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(gpoLabel5))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(miscPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(gpiButton6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(gpoLabel6))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(miscPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(gpiButton7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(gpoLabel7))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(miscPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(gpoLabel8)
                            .addComponent(gpiButton8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(46, 46, 46))
                    .addGroup(miscPanelLayout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addContainerGap(147, Short.MAX_VALUE))
                    .addGroup(miscPanelLayout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addContainerGap(173, Short.MAX_VALUE))
                    .addGroup(miscPanelLayout.createSequentialGroup()
                        .addComponent(dumpDebugStateButton)
                        .addContainerGap(140, Short.MAX_VALUE))
                    .addGroup(miscPanelLayout.createSequentialGroup()
                        .addComponent(captureButton)
                        .addContainerGap(140, Short.MAX_VALUE))
                    .addGroup(miscPanelLayout.createSequentialGroup()
                        .addComponent(replayButton)
                        .addContainerGap(140, Short.MAX_VALUE))))
        );
        miscPanelLayout.setVerticalGroup(
            miscPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(miscPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(gpioLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(miscPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(miscPanelLayout.createSequentialGroup()
                        .addGroup(miscPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(gpoLabel1)
                            .addComponent(gpoLabel2)
                            .addComponent(gpoLabel3)
                            .addComponent(gpoLabel4)
                            .addComponent(gpoLabel5)
                            .addComponent(gpoLabel6)
                            .addComponent(gpoLabel7))
                        .addGap(11, 11, 11)
                        .addGroup(miscPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                            .addComponent(gpiButton1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(gpiButton2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(gpiButton3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(gpiButton4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(gpiButton5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(gpiButton6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(gpiButton7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(gpiButton8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(captureButton))
                    .addComponent(gpoLabel8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(replayButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(dumpDebugStateButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 11, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(SearchField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(300, Short.MAX_VALUE))
        );

        disasmTabs.addTab("Misc", miscPanel);

        disasmToolbar2.setFloatable(false);
        disasmToolbar2.setRollover(true);

        AddBreakpoint.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/NewBreakpointIcon.png"))); // NOI18N
        AddBreakpoint.setMnemonic('A');
        AddBreakpoint.setText(Resource.get("addbreak"));
        AddBreakpoint.setFocusable(false);
        AddBreakpoint.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        AddBreakpoint.setIconTextGap(2);
        AddBreakpoint.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        AddBreakpoint.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AddBreakpointActionPerformed(evt);
            }
        });
        disasmToolbar2.add(AddBreakpoint);

        DeleteBreakpoint.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/DeleteBreakpointIcon.png"))); // NOI18N
        DeleteBreakpoint.setMnemonic('D');
        DeleteBreakpoint.setText(Resource.get("deletebreak"));
        DeleteBreakpoint.setFocusable(false);
        DeleteBreakpoint.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        DeleteBreakpoint.setIconTextGap(2);
        DeleteBreakpoint.setInheritsPopupMenu(true);
        DeleteBreakpoint.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        DeleteBreakpoint.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DeleteBreakpointActionPerformed(evt);
            }
        });
        disasmToolbar2.add(DeleteBreakpoint);

        DeleteAllBreakpoints.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/DeleteAllBreakpointsIcon.png"))); // NOI18N
        DeleteAllBreakpoints.setMnemonic('E');
        DeleteAllBreakpoints.setText(Resource.get("deleteall"));
        DeleteAllBreakpoints.setFocusable(false);
        DeleteAllBreakpoints.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        DeleteAllBreakpoints.setIconTextGap(2);
        DeleteAllBreakpoints.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        DeleteAllBreakpoints.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DeleteAllBreakpointsActionPerformed(evt);
            }
        });
        disasmToolbar2.add(DeleteAllBreakpoints);
        disasmToolbar2.add(jSeparator3);

        ExportBreaks.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/SaveStateIcon.png"))); // NOI18N
        ExportBreaks.setText("Export Breaks");
        ExportBreaks.setFocusable(false);
        ExportBreaks.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        ExportBreaks.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        ExportBreaks.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ExportBreaksActionPerformed(evt);
            }
        });
        disasmToolbar2.add(ExportBreaks);

        ImportBreaks.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/LoadStateIcon.png"))); // NOI18N
        ImportBreaks.setText("Import Breaks");
        ImportBreaks.setFocusable(false);
        ImportBreaks.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        ImportBreaks.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        ImportBreaks.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ImportBreaksActionPerformed(evt);
            }
        });
        disasmToolbar2.add(ImportBreaks);

        jSeparator5.setForeground(new java.awt.Color(0, 0, 0));

        jSeparator6.setForeground(new java.awt.Color(0, 0, 0));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(disasmToolbar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(disasmList, javax.swing.GroupLayout.PREFERRED_SIZE, 503, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(disasmTabs, javax.swing.GroupLayout.PREFERRED_SIZE, 254, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(disasmToolbar2, javax.swing.GroupLayout.DEFAULT_SIZE, 469, Short.MAX_VALUE)
                .addGap(326, 326, 326))
            .addComponent(jSeparator5, javax.swing.GroupLayout.DEFAULT_SIZE, 795, Short.MAX_VALUE)
            .addComponent(jSeparator6, javax.swing.GroupLayout.DEFAULT_SIZE, 795, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(disasmToolbar, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(disasmToolbar2, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator6, javax.swing.GroupLayout.PREFERRED_SIZE, 1, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator5, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(13, 13, 13)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(disasmTabs, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(disasmList, javax.swing.GroupLayout.DEFAULT_SIZE, 588, Short.MAX_VALUE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void disasmListKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_disasmListKeyPressed
    int keyCode = evt.getKeyCode();

    switch (keyCode) {
        case java.awt.event.KeyEvent.VK_DOWN:
            DebuggerPC += 4;
            RefreshDebugger(false);
            updateSelectedIndex();
            evt.consume();
            break;

        case java.awt.event.KeyEvent.VK_UP:
            DebuggerPC -= 4;
            RefreshDebugger(false);
            updateSelectedIndex();
            evt.consume();
            break;

        case java.awt.event.KeyEvent.VK_PAGE_UP:
            DebuggerPC -= 0x00000094;
            RefreshDebugger(false);
            updateSelectedIndex();
            evt.consume();
            break;

        case java.awt.event.KeyEvent.VK_PAGE_DOWN:
            DebuggerPC += 0x00000094;
            RefreshDebugger(false);
            updateSelectedIndex();
            evt.consume();
            break;
    }
}//GEN-LAST:event_disasmListKeyPressed

private void disasmListMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_disasmListMouseWheelMoved
    if (evt.getWheelRotation() < 0) {
        DebuggerPC -= 4;
        RefreshDebugger(false);
        updateSelectedIndex();
        evt.consume();
    } else {
        DebuggerPC += 4;
        RefreshDebugger(false);
        updateSelectedIndex();
        evt.consume();
    }
}//GEN-LAST:event_disasmListMouseWheelMoved

private void updateSelectedIndex() {
    if (SelectedPC >= DebuggerPC && SelectedPC < DebuggerPC + 0x00000094) {
        disasmList.setSelectedIndex((SelectedPC - DebuggerPC) / 4);
    }
}

/** replacement for disasmList.getSelectedIndex() because there is no longer a selected index,
 * we don't want the blue highlight from the operating system/look and feel, we want our own. */
private int disasmListGetSelectedIndex() {
    return (SelectedPC - DebuggerPC) / 4;
}

/** replacement for disasmList.getSelectedValue() because there is no longer a selected index,
 * we don't want the blue highlight from the operating system/look and feel, we want our own. */
private Object disasmListGetSelectedValue() {
    return disasmList.getModel().getElementAt(disasmListGetSelectedIndex());
}

private void ResetToPCbuttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ResetToPCbuttonActionPerformed
    RefreshDebugger(true);
}//GEN-LAST:event_ResetToPCbuttonActionPerformed

private void JumpToAddressActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_JumpToAddressActionPerformed
    String input = (String) JOptionPane.showInputDialog(this, Resource.get("entertojump"), "Jpcsp", JOptionPane.QUESTION_MESSAGE, null, null, String.format("%08x", Emulator.getProcessor().cpu.pc));
    if (input == null) {
        return;
    }
    int value=0;
         try {
            value = Utilities.parseAddress(input);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, Resource.get("numbernotcorrect"));
            return;
        }
        DebuggerPC = value;
        RefreshDebugger(false);

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
            for (int i = Start; i <= End; i += 4) {
                if (Memory.isAddressGood(i)) {
                    int opcode = Memory.getInstance().read32(i);
                    Instruction insn = Decoder.instruction(opcode);
                    bufferedWriter.write(String.format("%08X:[%08X]: %s", i, opcode, insn.disasm(i, opcode)));
                } else {
                    // Should we even both printing these?
                    bufferedWriter.write(String.format("%08X: invalid address", i));
                }
                bufferedWriter.newLine();
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            Utilities.close(bufferedWriter);
        }
    }
    //System.out.println("dump code dialog done");
    opt=null;
}//GEN-LAST:event_DumpCodeToTextActionPerformed



//Following methods are for the JPopmenu in Jlist
private void CopyAddressActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CopyAddressActionPerformed
    String value = (String)disasmListGetSelectedValue();
    String address = value.substring(3, 11);
    StringSelection stringSelection = new StringSelection( address);
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    clipboard.setContents(stringSelection, this);
}//GEN-LAST:event_CopyAddressActionPerformed

private void CopyAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CopyAllActionPerformed
    String value = (String)disasmListGetSelectedValue();
    StringSelection stringSelection = new StringSelection( value);
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    clipboard.setContents(stringSelection, this);
}//GEN-LAST:event_CopyAllActionPerformed

private void BranchOrJumpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BranchOrJumpActionPerformed
    String value = (String)disasmListGetSelectedValue();
    int address = value.indexOf("0x");
    if(address==-1)
    {
      JpcspDialogManager.showError(this, "Can't find the jump or branch address");
      return;
    }
	String add = value.substring(address+2,value.length());

	// Remove syscall code, if present
	int addressend = add.indexOf(" ");
	if (addressend != -1)
		add = add.substring(0, addressend);
	
	StringSelection stringSelection = new StringSelection(add);
	Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	clipboard.setContents(stringSelection, this);
}//GEN-LAST:event_BranchOrJumpActionPerformed
    @Override
public void lostOwnership( Clipboard aClipboard, Transferable aContents) {
     //do nothing
}

private void disasmListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_disasmListMouseClicked

       BranchOrJump.setEnabled(false);
       SetPCToCursor.setEnabled(false);

        if (SwingUtilities.isRightMouseButton(evt) && disasmList.locationToIndex(evt.getPoint()) == disasmListGetSelectedIndex())
       {
           //check if we can enable branch or jump address copy
           String line = (String)disasmListGetSelectedValue();
           int finddot = line.indexOf("]:");
           String opcode = line.substring(finddot+3,line.length());
           if(opcode.startsWith("b") || opcode.startsWith("j"))//it is definately a branch or jump opcode
           {
               BranchOrJump.setEnabled(true);
           }

           //check if we should enable set pc to cursor
           int addr = DebuggerPC + disasmListGetSelectedIndex() * 4;
           if (Memory.isAddressGood(addr)) {
               SetPCToCursor.setEnabled(true);
           }

           DisMenu.show(disasmList, evt.getX(), evt.getY());
       }
}//GEN-LAST:event_disasmListMouseClicked

private void AddBreakpointActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AddBreakpointActionPerformed
    String value =(String)disasmListGetSelectedValue();
    if (value != null) {
        try {
            String address = value.substring(3, 11);
            int addr = Utilities.parseAddress(address);
            if(!breakpoints.contains(addr))
                breakpoints.add(addr);
            RefreshDebugger(false);
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
    RefreshDebugger(false);
}//GEN-LAST:event_DeleteAllBreakpointsActionPerformed

public void DeleteAllBreakpoints() {
    if (!breakpoints.isEmpty())
        breakpoints.clear();
}

private void DeleteBreakpointActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DeleteBreakpointActionPerformed
          String value =(String)disasmListGetSelectedValue();
          if(value != null)
          {
            boolean breakpointexists = value.startsWith("<*>");
            if(breakpointexists)
            {
              String address = value.substring(3, 11);
              int addr = Utilities.parseAddress(address);
              int b = breakpoints.indexOf(addr);
              breakpoints.remove(b);
              RefreshDebugger(false);
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
    if (wantStep || (!breakpoints.isEmpty() && breakpoints.indexOf(Emulator.getProcessor().cpu.pc) != -1)) {
    	wantStep = false;
        Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_BREAKPOINT);

        /* done by PauseEmu
        RefreshDebugger(true);
        RefreshButtons();
        if (State.memoryViewer != null)
            State.memoryViewer.RefreshMemory();
        */
    }
}

private void PauseDebuggerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PauseDebuggerActionPerformed
    Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_PAUSE);

    /* done by PauseEmu
    RefreshDebugger(true);
    */
}//GEN-LAST:event_PauseDebuggerActionPerformed

// Called from Emulator
public void RefreshButtons() {
    RunDebugger.setSelected(Emulator.run && !Emulator.pause);
    PauseDebugger.setSelected(Emulator.run && Emulator.pause);
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
private void cop1TableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_cop1TableMouseClicked
   if (SwingUtilities.isRightMouseButton(evt) && cop1Table.isColumnSelected(1) && isCellChecked(cop1Table))
   {
     RegMenu.show(cop1Table, evt.getX(), evt.getY());
   }
}//GEN-LAST:event_cop1TableMouseClicked

private void CopyValueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CopyValueActionPerformed
 if(cop1Table.isShowing()){
    float value = (Float)cop1Table.getValueAt(cop1Table.getSelectedRow(),1);
    StringSelection stringSelection = new StringSelection( Float.toString(value));
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    clipboard.setContents(stringSelection, this);
 }
 else if(gprTable.isShowing())
 {
    String value = (String)gprTable.getValueAt(gprTable.getSelectedRow(),1);
    StringSelection stringSelection = new StringSelection(value);
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    clipboard.setContents(stringSelection, this);
 }
}//GEN-LAST:event_CopyValueActionPerformed

private void gprTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_gprTableMouseClicked
   if (SwingUtilities.isRightMouseButton(evt) && gprTable.isColumnSelected(1) && isCellChecked(gprTable))
   {
     RegMenu.show(gprTable, evt.getX(), evt.getY());
   }
}//GEN-LAST:event_gprTableMouseClicked

public int GetGPI() {
    return gpi;
}

public void SetGPO(int gpo) {
    this.gpo = gpo;
    // TODO if we want to use a visibility check here, then we need to refresh
    // gpo onFocus too otherwise it will be stale.
    //if (jPanel1.isVisible()) {
        // Refresh GPO
        for(int i = 0; i < 8; i++)
            SetGPO(i, (gpo & (1 << i)) != 0);
    //}
}

private void ToggleGPI(int index) {
    gpi ^= 1 << index;

    // Refresh GPI buttons
    for(int i = 0; i < 8; i++)
        SetGPI(i, (gpi & (1 << i)) != 0);
}

private void SetGPO(int index, boolean on) {
    switch(index) {
        case 0: gpoLabel1.setEnabled(on); break;
        case 1: gpoLabel2.setEnabled(on); break;
        case 2: gpoLabel3.setEnabled(on); break;
        case 3: gpoLabel4.setEnabled(on); break;
        case 4: gpoLabel5.setEnabled(on); break;
        case 5: gpoLabel6.setEnabled(on); break;
        case 6: gpoLabel7.setEnabled(on); break;
        case 7: gpoLabel8.setEnabled(on); break;
    }
}

private void SetGPI(int index, boolean on) {
    switch(index) {
        case 0: gpiButton1.setSelected(on); break;
        case 1: gpiButton2.setSelected(on); break;
        case 2: gpiButton3.setSelected(on); break;
        case 3: gpiButton4.setSelected(on); break;
        case 4: gpiButton5.setSelected(on); break;
        case 5: gpiButton6.setSelected(on); break;
        case 6: gpiButton7.setSelected(on); break;
        case 7: gpiButton8.setSelected(on); break;
    }
}

private void gpiButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gpiButton1ActionPerformed
    ToggleGPI(0);
}//GEN-LAST:event_gpiButton1ActionPerformed

private void gpiButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gpiButton2ActionPerformed
    ToggleGPI(1);
}//GEN-LAST:event_gpiButton2ActionPerformed

private void gpiButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gpiButton3ActionPerformed
    ToggleGPI(2);
}//GEN-LAST:event_gpiButton3ActionPerformed

private void gpiButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gpiButton4ActionPerformed
    ToggleGPI(3);
}//GEN-LAST:event_gpiButton4ActionPerformed

private void gpiButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gpiButton5ActionPerformed
    ToggleGPI(4);
}//GEN-LAST:event_gpiButton5ActionPerformed

private void gpiButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gpiButton6ActionPerformed
    ToggleGPI(5);
}//GEN-LAST:event_gpiButton6ActionPerformed

private void gpiButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gpiButton7ActionPerformed
    ToggleGPI(6);
}//GEN-LAST:event_gpiButton7ActionPerformed

private void gpiButton8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gpiButton8ActionPerformed
    ToggleGPI(7);
}//GEN-LAST:event_gpiButton8ActionPerformed

private void SetPCToCursorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SetPCToCursorActionPerformed
    int index = disasmListGetSelectedIndex();
    if (index != -1) {
        Emulator.getProcessor().cpu.pc = DebuggerPC + index * 4;
        RefreshDebugger(true);
    } else {
        System.out.println("dpc: " + Integer.toHexString(DebuggerPC));
        System.out.println("idx: " + Integer.toHexString(index));
        System.out.println("npc: " + Integer.toHexString(DebuggerPC + index * 4));
    }
}//GEN-LAST:event_SetPCToCursorActionPerformed

private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
    // TODO add your handling code here:
}//GEN-LAST:event_jButton3ActionPerformed

private void captureButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_captureButtonActionPerformed
    State.captureGeNextFrame = true;
}//GEN-LAST:event_captureButtonActionPerformed

private void replayButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_replayButtonActionPerformed
    State.replayGeNextFrame = true;
}//GEN-LAST:event_replayButtonActionPerformed

private void dumpDebugStateButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dumpDebugStateButtonActionPerformed
    DumpDebugState.dumpDebugState();
}//GEN-LAST:event_dumpDebugStateButtonActionPerformed

private void SearchFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SearchFieldActionPerformed

    //Basic text-based search function
    //TODO: Figure out an effective method of optimizing the search and avoid
    //hogging the emulator

    String text = SearchField.getText();
    String current = "";

    disasmList.setSelectedIndex(srcounter);

    while(!current.contains(text)){
        DebuggerPC += 4;
        SelectedPC = DebuggerPC;
        RefreshDebugger(false);
        updateSelectedIndex();

        current = (String)disasmListGetSelectedValue();
        srcounter++;
    }
}//GEN-LAST:event_SearchFieldActionPerformed

private void ExportBreaksActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ExportBreaksActionPerformed
JFileChooser fc = new JFileChooser();
fc.setSelectedFile(new File(State.discId + ".brk"));
fc.setDialogTitle("Export breakpoints");
fc.setCurrentDirectory(new java.io.File("."));
int returnVal = fc.showSaveDialog(this);
if (returnVal != JFileChooser.APPROVE_OPTION)
    return;

File f = fc.getSelectedFile();
BufferedWriter out = null;
try {
    if (f.exists()) {
        int res = JOptionPane.showConfirmDialog(
                this,
                "File '" + f.getName() + "' already exists! Do you want to override?",
                "Export breakpoints",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (res != JOptionPane.YES_OPTION)
            return;
    }

    out = new BufferedWriter(new FileWriter(f));

    for(int i = 0; i < breakpoints.size(); i++)
        out.write(Integer.toHexString(breakpoints.get(i)) + System.getProperty("line.separator"));

} catch (Exception ex) {
    ex.printStackTrace();
} finally {
    Utilities.close(out);
}
}//GEN-LAST:event_ExportBreaksActionPerformed

private void ImportBreaksActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ImportBreaksActionPerformed
JFileChooser fc = new JFileChooser();
fc.setDialogTitle("Import breakpoints");
fc.setCurrentDirectory(new java.io.File("."));
int returnVal = fc.showOpenDialog(this);
if (returnVal != JFileChooser.APPROVE_OPTION)
    return;

File f = fc.getSelectedFile();
BufferedReader in = null;
try {
    if(!f.getName().contains(".brk")) {
        JOptionPane.showMessageDialog(this,
                "File '" + f.getName() + "' is not a valid .brk file!",
                "Import breakpoints",
                JOptionPane.ERROR_MESSAGE);

        return;
    }

    in = new BufferedReader(new FileReader(f));
    String nextBrk = in.readLine();

    while (nextBrk != null) {
        breakpoints.add(Integer.parseInt(nextBrk, 16));
        nextBrk = in.readLine();
    }

    RefreshDebugger(true);

} catch (Exception ex) {
    ex.printStackTrace();
} finally {
    Utilities.close(in);
}
}//GEN-LAST:event_ImportBreaksActionPerformed

	@Override
	public void dispose() {
		Emulator.getMainGUI().endWindowDialog();
		super.dispose();
	}

	// Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton AddBreakpoint;
    private javax.swing.JMenuItem BranchOrJump;
    private javax.swing.JMenuItem CopyAddress;
    private javax.swing.JMenuItem CopyAll;
    private javax.swing.JMenuItem CopyValue;
    private javax.swing.JButton DeleteAllBreakpoints;
    private javax.swing.JButton DeleteBreakpoint;
    private javax.swing.JPopupMenu DisMenu;
    private javax.swing.JButton DumpCodeToText;
    private javax.swing.JButton ExportBreaks;
    private javax.swing.JButton ImportBreaks;
    private javax.swing.JButton JumpToAddress;
    private javax.swing.JButton PauseDebugger;
    private javax.swing.JPopupMenu RegMenu;
    private javax.swing.JButton ResetToPCbutton;
    private javax.swing.JToggleButton RunDebugger;
    private javax.swing.JTextField SearchField;
    private javax.swing.JMenuItem SetPCToCursor;
    private javax.swing.JButton StepInto;
    private javax.swing.JButton captureButton;
    private javax.swing.JTable cop0Table;
    private javax.swing.JTable cop1Table;
    private javax.swing.JList disasmList;
    private javax.swing.JTabbedPane disasmTabs;
    private javax.swing.JToolBar disasmToolbar;
    private javax.swing.JToolBar disasmToolbar2;
    private javax.swing.JButton dumpDebugStateButton;
    private javax.swing.JToggleButton gpiButton1;
    private javax.swing.JToggleButton gpiButton2;
    private javax.swing.JToggleButton gpiButton3;
    private javax.swing.JToggleButton gpiButton4;
    private javax.swing.JToggleButton gpiButton5;
    private javax.swing.JToggleButton gpiButton6;
    private javax.swing.JToggleButton gpiButton7;
    private javax.swing.JToggleButton gpiButton8;
    private javax.swing.JLabel gpioLabel;
    private javax.swing.JLabel gpoLabel1;
    private javax.swing.JLabel gpoLabel2;
    private javax.swing.JLabel gpoLabel3;
    private javax.swing.JLabel gpoLabel4;
    private javax.swing.JLabel gpoLabel5;
    private javax.swing.JLabel gpoLabel6;
    private javax.swing.JLabel gpoLabel7;
    private javax.swing.JLabel gpoLabel8;
    private javax.swing.JTable gprTable;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar.Separator jSeparator2;
    private javax.swing.JToolBar.Separator jSeparator3;
    private javax.swing.JToolBar.Separator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JPanel miscPanel;
    private javax.swing.JButton replayButton;
    // End of variables declaration//GEN-END:variables

}
