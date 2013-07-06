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

import static jpcsp.Allegrex.Common._ra;
import static jpcsp.Allegrex.Common.gprNames;

import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.State;
import jpcsp.Allegrex.CpuState;
import jpcsp.Allegrex.Decoder;
import jpcsp.Allegrex.GprState;
import jpcsp.Allegrex.Instructions;
import jpcsp.Allegrex.Common.Instruction;
import jpcsp.Allegrex.compiler.Compiler;
import jpcsp.Debugger.DumpDebugState;
import jpcsp.settings.Settings;
import jpcsp.util.JpcspDialogManager;
import jpcsp.util.Utilities;

import com.jidesoft.list.StyledListCellRenderer;
import com.jidesoft.swing.StyleRange;
import com.jidesoft.swing.StyledLabel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import jpcsp.Debugger.MemoryBreakpoints.MemoryBreakpointsDialog;
import jpcsp.memory.DebuggerMemory;
import jpcsp.util.Constants;
import org.apache.log4j.Logger;

/**
 *
 * @author shadow
 */
public class DisassemblerFrame extends javax.swing.JFrame implements ClipboardOwner {

    private static final long serialVersionUID = -8481807175706172292L;
    private int DebuggerPC;
    private int SelectedPC;
    private Emulator emu;
    private DefaultListModel listmodel = new DefaultListModel();
    private ArrayList<Integer> breakpoints = new ArrayList<Integer>();
    private int temporaryBreakpoint1;
    private int temporaryBreakpoint2;
    private boolean stepOut;
    protected int gpi, gpo;
    private int selectedRegCount;
    private final Color[] selectedRegColors = new Color[]{new Color(128, 255, 255), new Color(255, 255, 128), new Color(128, 255, 128)};
    private String[] selectedRegNames = new String[selectedRegColors.length];
    private final Color selectedAddressColor = new Color(255, 128, 255);
    private String selectedAddress;
    private int srcounter;
    private MemoryBreakpointsDialog mbpDialog;

    /**
     * Creates new form DisassemblerFrame
     */
    public DisassemblerFrame(Emulator emu) {
        this.emu = emu;
        listmodel = new DefaultListModel();

        initComponents();

        // calculate the fixed cell height and width based on a dummy string
        disasmList.setPrototypeCellValue("PROTOTYPE");

        gprTable.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if ("tableCellEditor".equals(evt.getPropertyName())) {
                    if (!gprTable.isEditing()) {
                        // editor finished editing the cell
                        int row = gprTable.getEditingRow();
                        int value = gprTable.getAddressAt(row);
                        boolean changedPC = false;

                        CpuState cpu = Emulator.getProcessor().cpu;
                        switch (row) {
                            case 0:
                                cpu.pc = value;
                                DebuggerPC = value;
                                changedPC = true;
                                break;
                            case 1:
                                cpu.setHi(value);
                                break;
                            case 2:
                                cpu.setLo(value);
                                break;
                            default:
                                cpu.setRegister(row - 3, value);
                                break;
                        }

                        if (changedPC) {
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    RefreshDebuggerDisassembly(true);
                                }
                            });
                        }
                    }
                }
            }
        });

        addKeyAction(btnStepInto, "F5");
        addKeyAction(btnStepOver, "F6");
        addKeyAction(btnStepOut, "F7");

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
                    String text = (String) disasmList.getSelectedValue();
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
    }

    private void addKeyAction(JButton button, String key) {
        final String actionName = "click";
        button.getInputMap(JButton.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), actionName);
        button.getActionMap().put(actionName, new ClickAction(button));
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
            if (length > text.length() - 3) {
                length = text.length() - 3;
            }

            label.addStyleRange(new StyleRange(3, length, Font.BOLD, Color.BLACK));
            // testing label.addStyleRange(new StyleRange(3, length, Font.PLAIN, Color.RED, Color.GREEN, 0));

            // highlight gutter if there is no breakpoint
            if (!text.startsWith("<*>")) {
                label.addStyleRange(new StyleRange(0, 3, Font.BOLD, Color.BLACK, Color.YELLOW, 0));
            }
        }

        // selected line highlighting
        // moved to cell renderer, we can highlight the entire line independantly of StyleRange

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

    /**
     * Delete breakpoints and reset to PC
     */
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

    private void RefreshDebuggerDisassembly(boolean moveToPC) {
        CpuState cpu = Emulator.getProcessor().cpu;
        int pc;

        if (moveToPC) {
            DebuggerPC = cpu.pc;
        }

        ViewTooltips.unregister(disasmList);
        synchronized (listmodel) {
            listmodel.clear();

            // compute the number of visible rows, based on the widget's size
            int numVisibleRows = disasmList.getHeight() / disasmList.getFixedCellHeight();
            for (pc = DebuggerPC; pc < (DebuggerPC + numVisibleRows * 0x00000004); pc += 0x00000004) {
                if (Memory.isAddressGood(pc)) {
                    int opcode = Memory.getInstance().read32(pc);

                    Instruction insn = Decoder.instruction(opcode);

                    String line;
                    if (breakpoints.indexOf(pc) != -1) {
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
    }

    private void RefreshDebuggerRegisters() {
        CpuState cpu = Emulator.getProcessor().cpu;

        // refresh registers
        // gpr
        gprTable.resetChanges();
        gprTable.setValueAt(cpu.pc, 0, 1);
        gprTable.setValueAt(cpu.getHi(), 1, 1);
        gprTable.setValueAt(cpu.getLo(), 2, 1);
        for (int i = 0; i < GprState.NUMBER_REGISTERS; i++) {
            gprTable.setValueAt(cpu.getRegister(i), 3 + i, 1);
        }

        // fpr
        for (int i = 0; i < cpu.fpr.length; i++) {
            cop1Table.setValueAt(cpu.fpr[i], i, 1);
        }

        // vfpu
        VfpuFrame.getInstance().updateRegisters(cpu);
    }

    final public void RefreshDebugger(boolean moveToPC) {
        RefreshDebuggerDisassembly(moveToPC);
        RefreshDebuggerRegisters();

        // enable memory breakpoint manager if debugger memory is available
        ManageMemBreaks.setEnabled(Memory.getInstance() instanceof DebuggerMemory);
        miManageMemoryBreakpoints.setEnabled(Memory.getInstance() instanceof DebuggerMemory);
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

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
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
        tbDisasm = new javax.swing.JToolBar();
        RunDebugger = new javax.swing.JToggleButton();
        PauseDebugger = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        btnStepInto = new javax.swing.JButton();
        btnStepOver = new javax.swing.JButton();
        btnStepOut = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JToolBar.Separator();
        ResetToPCbutton = new javax.swing.JButton();
        JumpToAddress = new javax.swing.JButton();
        jSeparator4 = new javax.swing.JToolBar.Separator();
        DumpCodeToText = new javax.swing.JButton();
        tbBreakpoints = new javax.swing.JToolBar();
        AddBreakpoint = new javax.swing.JButton();
        DeleteBreakpoint = new javax.swing.JButton();
        DeleteAllBreakpoints = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JToolBar.Separator();
        ManageMemBreaks = new javax.swing.JButton();
        jSeparator7 = new javax.swing.JToolBar.Separator();
        ImportBreaks = new javax.swing.JButton();
        ExportBreaks = new javax.swing.JButton();
        disasmList = new javax.swing.JList(listmodel);
        disasmTabs = new javax.swing.JTabbedPane();
        gprTable = new jpcsp.Debugger.DisassemblerModule.RegisterTable();
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
        lblCaptureReplay = new javax.swing.JLabel();
        btnCapture = new javax.swing.JToggleButton();
        btnReplay = new javax.swing.JToggleButton();
        lblDumpState = new javax.swing.JLabel();
        btnDumpDebugState = new javax.swing.JButton();
        txtSearch = new javax.swing.JTextField();
        lblSearch = new javax.swing.JLabel();
        mbMain = new javax.swing.JMenuBar();
        mFile = new javax.swing.JMenu();
        miClose = new javax.swing.JMenuItem();
        mDebug = new javax.swing.JMenu();
        miRun = new javax.swing.JMenuItem();
        miPause = new javax.swing.JMenuItem();
        jSeparator9 = new javax.swing.JPopupMenu.Separator();
        miStepInto = new javax.swing.JMenuItem();
        miStepOver = new javax.swing.JMenuItem();
        miStepOut = new javax.swing.JMenuItem();
        jSeparator10 = new javax.swing.JPopupMenu.Separator();
        miResetToPC = new javax.swing.JMenuItem();
        miJumpTo = new javax.swing.JMenuItem();
        mBreakpoints = new javax.swing.JMenu();
        miNewBreakpoint = new javax.swing.JMenuItem();
        miDeleteBreakpoint = new javax.swing.JMenuItem();
        miDeleteAllBreakpoints = new javax.swing.JMenuItem();
        miImportBreakpoints = new javax.swing.JMenuItem();
        miExportBreakpoints = new javax.swing.JMenuItem();
        jSeparator11 = new javax.swing.JPopupMenu.Separator();
        miManageMemoryBreakpoints = new javax.swing.JMenuItem();
        mDisassembler = new javax.swing.JMenu();
        miDumpCode = new javax.swing.JMenuItem();

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("jpcsp/languages/jpcsp"); // NOI18N
        CopyAddress.setText(bundle.getString("DisassemblerFrame.CopyAddress.text")); // NOI18N
        CopyAddress.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CopyAddressActionPerformed(evt);
            }
        });
        DisMenu.add(CopyAddress);

        CopyAll.setText(bundle.getString("DisassemblerFrame.CopyAll.text")); // NOI18N
        CopyAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CopyAllActionPerformed(evt);
            }
        });
        DisMenu.add(CopyAll);

        BranchOrJump.setText(bundle.getString("DisassemblerFrame.CopyBranchOrJump.text")); // NOI18N
        BranchOrJump.setEnabled(false); //disable as default
        BranchOrJump.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BranchOrJumpActionPerformed(evt);
            }
        });
        DisMenu.add(BranchOrJump);

        SetPCToCursor.setText(bundle.getString("DisassemblerFrame.SetPCToCursor.text")); // NOI18N
        SetPCToCursor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SetPCToCursorActionPerformed(evt);
            }
        });
        DisMenu.add(SetPCToCursor);

        CopyValue.setText(bundle.getString("DisassemblerFrame.CopyValue.text")); // NOI18N
        CopyValue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CopyValueActionPerformed(evt);
            }
        });
        RegMenu.add(CopyValue);

        setTitle(bundle.getString("DisassemblerFrame.title")); // NOI18N
        setMinimumSize(new java.awt.Dimension(800, 700));
        setName("frmDebugger"); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowDeactivated(java.awt.event.WindowEvent evt) {
                formWindowDeactivated(evt);
            }
        });

        tbDisasm.setFloatable(false);
        tbDisasm.setRollover(true);
        tbDisasm.setOpaque(false);

        RunDebugger.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/PlayIcon.png"))); // NOI18N
        RunDebugger.setMnemonic('R');
        RunDebugger.setToolTipText(bundle.getString("DisassemblerFrame.miRun.text")); // NOI18N
        RunDebugger.setFocusable(false);
        RunDebugger.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        RunDebugger.setIconTextGap(2);
        RunDebugger.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        RunDebugger.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RunDebuggerActionPerformed(evt);
            }
        });
        tbDisasm.add(RunDebugger);

        PauseDebugger.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/PauseIcon.png"))); // NOI18N
        PauseDebugger.setMnemonic('P');
        PauseDebugger.setToolTipText(bundle.getString("DisassemblerFrame.miPause.text")); // NOI18N
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
        tbDisasm.add(PauseDebugger);
        tbDisasm.add(jSeparator1);

        btnStepInto.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/StepIntoIcon.png"))); // NOI18N
        btnStepInto.setToolTipText(bundle.getString("DisassemblerFrame.miStepInto.text")); // NOI18N
        btnStepInto.setFocusable(false);
        btnStepInto.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        btnStepInto.setIconTextGap(2);
        btnStepInto.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnStepInto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StepIntoActionPerformed(evt);
            }
        });
        tbDisasm.add(btnStepInto);

        btnStepOver.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/StepOverIcon.png"))); // NOI18N
        btnStepOver.setToolTipText(bundle.getString("DisassemblerFrame.miStepOver.text")); // NOI18N
        btnStepOver.setFocusable(false);
        btnStepOver.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        btnStepOver.setIconTextGap(2);
        btnStepOver.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnStepOver.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StepOverActionPerformed(evt);
            }
        });
        tbDisasm.add(btnStepOver);

        btnStepOut.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/StepOutIcon.png"))); // NOI18N
        btnStepOut.setToolTipText(bundle.getString("DisassemblerFrame.miStepOut.text")); // NOI18N
        btnStepOut.setFocusable(false);
        btnStepOut.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        btnStepOut.setIconTextGap(2);
        btnStepOut.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnStepOut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StepOutActionPerformed(evt);
            }
        });
        tbDisasm.add(btnStepOut);
        tbDisasm.add(jSeparator2);

        ResetToPCbutton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/ResetToPc.png"))); // NOI18N
        ResetToPCbutton.setMnemonic('P');
        ResetToPCbutton.setToolTipText(bundle.getString("DisassemblerFrame.miResetToPC.text")); // NOI18N
        ResetToPCbutton.setFocusable(false);
        ResetToPCbutton.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        ResetToPCbutton.setIconTextGap(2);
        ResetToPCbutton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        ResetToPCbutton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ResetToPCActionPerformed(evt);
            }
        });
        tbDisasm.add(ResetToPCbutton);

        JumpToAddress.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/JumpTo.png"))); // NOI18N
        JumpToAddress.setMnemonic('J');
        JumpToAddress.setToolTipText(bundle.getString("DisassemblerFrame.miJumpTo.text")); // NOI18N
        JumpToAddress.setFocusable(false);
        JumpToAddress.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        JumpToAddress.setIconTextGap(2);
        JumpToAddress.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        JumpToAddress.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                JumpToAddressActionPerformed(evt);
            }
        });
        tbDisasm.add(JumpToAddress);
        tbDisasm.add(jSeparator4);

        DumpCodeToText.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/Dump.png"))); // NOI18N
        DumpCodeToText.setMnemonic('W');
        DumpCodeToText.setToolTipText(bundle.getString("DisassemblerFrame.miDumpCode.text")); // NOI18N
        DumpCodeToText.setFocusable(false);
        DumpCodeToText.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        DumpCodeToText.setIconTextGap(2);
        DumpCodeToText.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        DumpCodeToText.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DumpCodeToTextActionPerformed(evt);
            }
        });
        tbDisasm.add(DumpCodeToText);

        tbBreakpoints.setFloatable(false);
        tbBreakpoints.setRollover(true);
        tbBreakpoints.setOpaque(false);

        AddBreakpoint.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/NewBreakpointIcon.png"))); // NOI18N
        AddBreakpoint.setMnemonic('A');
        AddBreakpoint.setToolTipText(bundle.getString("DisassemblerFrame.miNewBreakpoint.text")); // NOI18N
        AddBreakpoint.setFocusable(false);
        AddBreakpoint.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        AddBreakpoint.setIconTextGap(2);
        AddBreakpoint.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        AddBreakpoint.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AddBreakpointActionPerformed(evt);
            }
        });
        tbBreakpoints.add(AddBreakpoint);

        DeleteBreakpoint.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/DeleteBreakpointIcon.png"))); // NOI18N
        DeleteBreakpoint.setMnemonic('D');
        DeleteBreakpoint.setToolTipText(bundle.getString("DisassemblerFrame.miDeleteBreakpoint.text")); // NOI18N
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
        tbBreakpoints.add(DeleteBreakpoint);

        DeleteAllBreakpoints.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/DeleteAllBreakpointsIcon.png"))); // NOI18N
        DeleteAllBreakpoints.setMnemonic('E');
        DeleteAllBreakpoints.setToolTipText(bundle.getString("DisassemblerFrame.miDeleteAllBreakpoints.text")); // NOI18N
        DeleteAllBreakpoints.setFocusable(false);
        DeleteAllBreakpoints.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        DeleteAllBreakpoints.setIconTextGap(2);
        DeleteAllBreakpoints.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        DeleteAllBreakpoints.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DeleteAllBreakpointsActionPerformed(evt);
            }
        });
        tbBreakpoints.add(DeleteAllBreakpoints);
        tbBreakpoints.add(jSeparator3);

        ManageMemBreaks.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/MemoryBreakpointsIcon.png"))); // NOI18N
        ManageMemBreaks.setToolTipText(bundle.getString("DisassemblerFrame.miManageMemoryBreakpoints.text")); // NOI18N
        ManageMemBreaks.setFocusable(false);
        ManageMemBreaks.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        ManageMemBreaks.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        ManageMemBreaks.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ManageMemBreaksActionPerformed(evt);
            }
        });
        tbBreakpoints.add(ManageMemBreaks);
        tbBreakpoints.add(jSeparator7);

        ImportBreaks.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/LoadStateIcon.png"))); // NOI18N
        ImportBreaks.setToolTipText(bundle.getString("DisassemblerFrame.miImportBreakpoints.text")); // NOI18N
        ImportBreaks.setFocusable(false);
        ImportBreaks.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        ImportBreaks.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        ImportBreaks.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ImportBreaksActionPerformed(evt);
            }
        });
        tbBreakpoints.add(ImportBreaks);

        ExportBreaks.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/SaveStateIcon.png"))); // NOI18N
        ExportBreaks.setToolTipText(bundle.getString("DisassemblerFrame.miExportBreakpoints.text")); // NOI18N
        ExportBreaks.setFocusable(false);
        ExportBreaks.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        ExportBreaks.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        ExportBreaks.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ExportBreaksActionPerformed(evt);
            }
        });
        tbBreakpoints.add(ExportBreaks);

        disasmList.setFont(new java.awt.Font("Courier New", 0, 12)); // NOI18N
        disasmList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        disasmList.setMinimumSize(new java.awt.Dimension(500, 50));
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
        disasmList.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                disasmListComponentResized(evt);
            }
        });
        disasmList.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                disasmListKeyPressed(evt);
            }
        });

        disasmTabs.setMinimumSize(new java.awt.Dimension(280, 587));
        disasmTabs.setPreferredSize(new java.awt.Dimension(280, 587));

        gprTable.setModel(null);
        gprTable.setRegisters(new String[] {"PC", "HI", "LO", "zr", "at", "v0", "v1", "a0", "a1", "a2", "a3", "t0", "t1", "t2", "t3", "t4", "t5", "t6", "t7", "s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7", "t8", "t9", "k0", "k1", "gp", "sp", "fp", "ra"});
        disasmTabs.addTab(bundle.getString("DisassemblerFrame.gprTable.TabConstraints.tabTitle"), gprTable); // NOI18N

        cop0Table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

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
        disasmTabs.addTab(bundle.getString("DisassemblerFrame.cop0Table.TabConstraints.tabTitle"), cop0Table); // NOI18N

        cop1Table.setFont(new java.awt.Font("Courier New", 0, 12)); // NOI18N
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
        cop1Table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                cop1TableMouseClicked(evt);
            }
        });
        disasmTabs.addTab(bundle.getString("DisassemblerFrame.cop1Table.TabConstraints.tabTitle"), cop1Table); // NOI18N

        gpiButton1.setText("1"); // NOI18N
        gpiButton1.setBorder(null);
        gpiButton1.setPreferredSize(new java.awt.Dimension(16, 16));
        gpiButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gpiButton1ActionPerformed(evt);
            }
        });

        gpiButton2.setText("2"); // NOI18N
        gpiButton2.setBorder(null);
        gpiButton2.setPreferredSize(new java.awt.Dimension(16, 16));
        gpiButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gpiButton2ActionPerformed(evt);
            }
        });

        gpiButton3.setText("3"); // NOI18N
        gpiButton3.setBorder(null);
        gpiButton3.setPreferredSize(new java.awt.Dimension(16, 16));
        gpiButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gpiButton3ActionPerformed(evt);
            }
        });

        gpiButton4.setText("4"); // NOI18N
        gpiButton4.setBorder(null);
        gpiButton4.setPreferredSize(new java.awt.Dimension(16, 16));
        gpiButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gpiButton4ActionPerformed(evt);
            }
        });

        gpiButton5.setText("5"); // NOI18N
        gpiButton5.setBorder(null);
        gpiButton5.setPreferredSize(new java.awt.Dimension(16, 16));
        gpiButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gpiButton5ActionPerformed(evt);
            }
        });

        gpiButton6.setText("6"); // NOI18N
        gpiButton6.setBorder(null);
        gpiButton6.setPreferredSize(new java.awt.Dimension(16, 16));
        gpiButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gpiButton6ActionPerformed(evt);
            }
        });

        gpiButton7.setText("7"); // NOI18N
        gpiButton7.setBorder(null);
        gpiButton7.setPreferredSize(new java.awt.Dimension(16, 16));
        gpiButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gpiButton7ActionPerformed(evt);
            }
        });

        gpiButton8.setText("8"); // NOI18N
        gpiButton8.setBorder(null);
        gpiButton8.setPreferredSize(new java.awt.Dimension(16, 16));
        gpiButton8.addActionListener(new java.awt.event.ActionListener() {
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

        gpioLabel.setText(bundle.getString("DisassemblerFrame.gpioLabel.text")); // NOI18N

        lblCaptureReplay.setText(bundle.getString("DisassemblerFrame.lblCaptureReplay.text")); // NOI18N

        btnCapture.setText(bundle.getString("DisassemblerFrame.btnCapture.text")); // NOI18N
        btnCapture.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCaptureActionPerformed(evt);
            }
        });

        btnReplay.setText(bundle.getString("DisassemblerFrame.btnReplay.text")); // NOI18N
        btnReplay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnReplayActionPerformed(evt);
            }
        });

        lblDumpState.setText(bundle.getString("DisassemblerFrame.lblDebugState.text")); // NOI18N

        btnDumpDebugState.setText(bundle.getString("DisassemblerFrame.btnDumpDebugState.text")); // NOI18N
        btnDumpDebugState.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDumpDebugStateActionPerformed(evt);
            }
        });

        txtSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtSearchActionPerformed(evt);
            }
        });

        lblSearch.setText(bundle.getString("DisassemblerFrame.lblSearch.text")); // NOI18N

        javax.swing.GroupLayout miscPanelLayout = new javax.swing.GroupLayout(miscPanel);
        miscPanel.setLayout(miscPanelLayout);
        miscPanelLayout.setHorizontalGroup(
            miscPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(miscPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(miscPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
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
                            .addComponent(gpiButton8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(txtSearch)
                    .addComponent(lblSearch)
                    .addComponent(gpioLabel)
                    .addComponent(lblCaptureReplay)
                    .addComponent(lblDumpState)
                    .addComponent(btnDumpDebugState, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnCapture, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnReplay, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
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
                        .addComponent(lblCaptureReplay))
                    .addComponent(gpoLabel8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnCapture)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnReplay)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblDumpState)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnDumpDebugState)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 11, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(288, Short.MAX_VALUE))
        );

        disasmTabs.addTab(bundle.getString("DisassemblerFrame.miscPanel.TabConstraints.tabTitle"), miscPanel); // NOI18N

        mFile.setText(bundle.getString("DisassemblerFrame.mFile.text")); // NOI18N

        miClose.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/CloseIcon.png"))); // NOI18N
        miClose.setText(bundle.getString("CloseButton.text")); // NOI18N
        miClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CloseActionPerformed(evt);
            }
        });
        mFile.add(miClose);

        mbMain.add(mFile);

        mDebug.setText(bundle.getString("DisassemblerFrame.mDebug.text")); // NOI18N
        mDebug.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RunDebuggerActionPerformed(evt);
            }
        });

        miRun.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/PlayIcon.png"))); // NOI18N
        miRun.setText(bundle.getString("DisassemblerFrame.miRun.text")); // NOI18N
        mDebug.add(miRun);

        miPause.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/PauseIcon.png"))); // NOI18N
        miPause.setText(bundle.getString("DisassemblerFrame.miPause.text")); // NOI18N
        miPause.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PauseDebuggerActionPerformed(evt);
            }
        });
        mDebug.add(miPause);
        mDebug.add(jSeparator9);

        miStepInto.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F5, 0));
        miStepInto.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/StepIntoIcon.png"))); // NOI18N
        miStepInto.setText(bundle.getString("DisassemblerFrame.miStepInto.text")); // NOI18N
        miStepInto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StepIntoActionPerformed(evt);
            }
        });
        mDebug.add(miStepInto);

        miStepOver.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F6, 0));
        miStepOver.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/StepOverIcon.png"))); // NOI18N
        miStepOver.setText(bundle.getString("DisassemblerFrame.miStepOver.text")); // NOI18N
        miStepOver.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StepOverActionPerformed(evt);
            }
        });
        mDebug.add(miStepOver);

        miStepOut.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F7, 0));
        miStepOut.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/StepOutIcon.png"))); // NOI18N
        miStepOut.setText(bundle.getString("DisassemblerFrame.miStepOut.text")); // NOI18N
        mDebug.add(miStepOut);
        mDebug.add(jSeparator10);

        miResetToPC.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/ResetToPc.png"))); // NOI18N
        miResetToPC.setText(bundle.getString("DisassemblerFrame.miResetToPC.text")); // NOI18N
        miResetToPC.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ResetToPCActionPerformed(evt);
            }
        });
        mDebug.add(miResetToPC);

        miJumpTo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/JumpTo.png"))); // NOI18N
        miJumpTo.setText(bundle.getString("DisassemblerFrame.miJumpTo.text")); // NOI18N
        miJumpTo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                JumpToAddressActionPerformed(evt);
            }
        });
        mDebug.add(miJumpTo);

        mbMain.add(mDebug);

        mBreakpoints.setText(bundle.getString("DisassemblerFrame.mBreakpoints.text")); // NOI18N

        miNewBreakpoint.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/NewBreakpointIcon.png"))); // NOI18N
        miNewBreakpoint.setText(bundle.getString("DisassemblerFrame.miNewBreakpoint.text")); // NOI18N
        miNewBreakpoint.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AddBreakpointActionPerformed(evt);
            }
        });
        mBreakpoints.add(miNewBreakpoint);

        miDeleteBreakpoint.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/DeleteBreakpointIcon.png"))); // NOI18N
        miDeleteBreakpoint.setText(bundle.getString("DisassemblerFrame.miDeleteBreakpoint.text")); // NOI18N
        miDeleteBreakpoint.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DeleteBreakpointActionPerformed(evt);
            }
        });
        mBreakpoints.add(miDeleteBreakpoint);

        miDeleteAllBreakpoints.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/DeleteAllBreakpointsIcon.png"))); // NOI18N
        miDeleteAllBreakpoints.setText(bundle.getString("DisassemblerFrame.miDeleteAllBreakpoints.text")); // NOI18N
        miDeleteAllBreakpoints.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DeleteAllBreakpointsActionPerformed(evt);
            }
        });
        mBreakpoints.add(miDeleteAllBreakpoints);

        miImportBreakpoints.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/LoadStateIcon.png"))); // NOI18N
        miImportBreakpoints.setText(bundle.getString("DisassemblerFrame.miImportBreakpoints.text")); // NOI18N
        miImportBreakpoints.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ImportBreaksActionPerformed(evt);
            }
        });
        mBreakpoints.add(miImportBreakpoints);

        miExportBreakpoints.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/SaveStateIcon.png"))); // NOI18N
        miExportBreakpoints.setText(bundle.getString("DisassemblerFrame.miExportBreakpoints.text")); // NOI18N
        miExportBreakpoints.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ExportBreaksActionPerformed(evt);
            }
        });
        mBreakpoints.add(miExportBreakpoints);
        mBreakpoints.add(jSeparator11);

        miManageMemoryBreakpoints.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/SettingsIcon.png"))); // NOI18N
        miManageMemoryBreakpoints.setText(bundle.getString("DisassemblerFrame.miManageMemoryBreakpoints.text")); // NOI18N
        miManageMemoryBreakpoints.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ManageMemBreaksActionPerformed(evt);
            }
        });
        mBreakpoints.add(miManageMemoryBreakpoints);

        mbMain.add(mBreakpoints);

        mDisassembler.setText(bundle.getString("DisassemblerFrame.mDisassembler.text")); // NOI18N

        miDumpCode.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/Dump.png"))); // NOI18N
        miDumpCode.setText(bundle.getString("DisassemblerFrame.miDumpCode.text")); // NOI18N
        miDumpCode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DumpCodeToTextActionPerformed(evt);
            }
        });
        mDisassembler.add(miDumpCode);

        mbMain.add(mDisassembler);

        setJMenuBar(mbMain);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(disasmList, javax.swing.GroupLayout.DEFAULT_SIZE, 505, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(tbDisasm, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(tbBreakpoints, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(disasmTabs, javax.swing.GroupLayout.PREFERRED_SIZE, 265, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(tbDisasm, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tbBreakpoints, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(disasmTabs, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(disasmList, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void disasmListKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_disasmListKeyPressed
        int keyCode = evt.getKeyCode();

        switch (keyCode) {
            case java.awt.event.KeyEvent.VK_DOWN:
                DebuggerPC += 4;
                RefreshDebuggerDisassembly(false);
                updateSelectedIndex();
                evt.consume();
                break;

            case java.awt.event.KeyEvent.VK_UP:
                DebuggerPC -= 4;
                RefreshDebuggerDisassembly(false);
                updateSelectedIndex();
                evt.consume();
                break;

            case java.awt.event.KeyEvent.VK_PAGE_UP:
                DebuggerPC -= 0x00000094;
                RefreshDebuggerDisassembly(false);
                updateSelectedIndex();
                evt.consume();
                break;

            case java.awt.event.KeyEvent.VK_PAGE_DOWN:
                DebuggerPC += 0x00000094;
                RefreshDebuggerDisassembly(false);
                updateSelectedIndex();
                evt.consume();
                break;
        }
}//GEN-LAST:event_disasmListKeyPressed

private void disasmListMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_disasmListMouseWheelMoved
        if (evt.getWheelRotation() < 0) {
            DebuggerPC -= 4;
            RefreshDebuggerDisassembly(false);
            updateSelectedIndex();
            evt.consume();
        } else {
            DebuggerPC += 4;
            RefreshDebuggerDisassembly(false);
            updateSelectedIndex();
            evt.consume();
        }
}//GEN-LAST:event_disasmListMouseWheelMoved

    private void updateSelectedIndex() {
        if (SelectedPC >= DebuggerPC && SelectedPC < DebuggerPC + 0x00000094) {
            disasmList.setSelectedIndex((SelectedPC - DebuggerPC) / 4);
        }
    }

    /**
     * replacement for disasmList.getSelectedIndex() because there is no longer
     * a selected index, we don't want the blue highlight from the operating
     * system/look and feel, we want our own.
     */
    private int disasmListGetSelectedIndex() {
        return (SelectedPC - DebuggerPC) / 4;
    }

    /**
     * replacement for disasmList.getSelectedValue() because there is no longer
     * a selected index, we don't want the blue highlight from the operating
     * system/look and feel, we want our own.
     */
    private Object disasmListGetSelectedValue() {
        if (disasmListGetSelectedIndex() < 0) {
            return null;
        }
        return disasmList.getModel().getElementAt(disasmListGetSelectedIndex());
    }

private void ResetToPCActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ResetToPCActionPerformed
        RefreshDebuggerDisassembly(true);
}//GEN-LAST:event_ResetToPCActionPerformed

private void JumpToAddressActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_JumpToAddressActionPerformed
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("jpcsp/languages/jpcsp"); // NOI18N
        String input = (String) JOptionPane.showInputDialog(this, bundle.getString("DisassemblerFrame.strEnterToJump.text"), "Jpcsp", JOptionPane.QUESTION_MESSAGE, null, null, String.format("%08x", Emulator.getProcessor().cpu.pc)); // NOI18N
        if (input == null) {
            return;
        }
        try {
            int value = Utilities.parseAddress(input);
            DebuggerPC = value;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, bundle.getString("MemoryViewer.strInvalidAddress.text"));
            return;
        }

        RefreshDebuggerDisassembly(false);
}//GEN-LAST:event_JumpToAddressActionPerformed

private void DumpCodeToTextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DumpCodeToTextActionPerformed
        DumpCodeDialog dlgDC = new DumpCodeDialog(this, DebuggerPC);
        dlgDC.setVisible(true);

        if (dlgDC.getReturnValue() != DumpCodeDialog.DUMPCODE_APPROVE) {
            return;
        }

        Logger.getRootLogger().debug("Start address: " + dlgDC.getStartAddress());
        Logger.getRootLogger().debug("End address: " + dlgDC.getEndAddress());
        Logger.getRootLogger().debug("File name: " + dlgDC.getFilename());

        BufferedWriter bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(dlgDC.getFilename()));
            bufferedWriter.write("------- JPCSP DISASM -------");
            bufferedWriter.newLine();
            for (int i = dlgDC.getStartAddress(); i <= dlgDC.getEndAddress(); i += 4) {
                if (Memory.isAddressGood(i)) {
                    int opcode = Memory.getInstance().read32(i);
                    Instruction insn = Decoder.instruction(opcode);
                    String disasm;
                    try {
                        disasm = insn.disasm(i, opcode);
                    } catch (Exception e) {
                        disasm = "???";
                    }
                    bufferedWriter.write(String.format("%08X:[%08X]: %s", i, opcode, disasm));
                } else {
                    // should we even both printing these?
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
}//GEN-LAST:event_DumpCodeToTextActionPerformed

// following methods are for the JPopmenu in Jlist
private void CopyAddressActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CopyAddressActionPerformed
        String value = (String) disasmListGetSelectedValue();
        String address = value.substring(3, 11);
        StringSelection stringSelection = new StringSelection(address);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, this);
}//GEN-LAST:event_CopyAddressActionPerformed

private void CopyAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CopyAllActionPerformed
        String value = (String) disasmListGetSelectedValue();
        StringSelection stringSelection = new StringSelection(value);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, this);
}//GEN-LAST:event_CopyAllActionPerformed

private void BranchOrJumpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BranchOrJumpActionPerformed
        String value = (String) disasmListGetSelectedValue();
        int address = value.indexOf("0x");
        if (address == -1) {
            JpcspDialogManager.showError(this, "Can't find the jump or branch address");
            return;
        }
        String add = value.substring(address + 2, value.length());

        // Remove syscall code, if present
        int addressend = add.indexOf(" ");
        if (addressend != -1) {
            add = add.substring(0, addressend);
        }

        StringSelection stringSelection = new StringSelection(add);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, this);
}//GEN-LAST:event_BranchOrJumpActionPerformed

    @Override
    public void lostOwnership(Clipboard aClipboard, Transferable aContents) {
        //do nothing
    }

private void disasmListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_disasmListMouseClicked

        BranchOrJump.setEnabled(false);
        SetPCToCursor.setEnabled(false);

        if (SwingUtilities.isRightMouseButton(evt) && disasmList.locationToIndex(evt.getPoint()) == disasmListGetSelectedIndex()) {
            //check if we can enable branch or jump address copy
            String line = (String) disasmListGetSelectedValue();
            int finddot = line.indexOf("]:");
            String opcode = line.substring(finddot + 3, line.length());
            if (opcode.startsWith("b") || opcode.startsWith("j"))//it is definately a branch or jump opcode
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
        String value = (String) disasmListGetSelectedValue();
        if (value != null) {
            try {
                String address = value.substring(3, 11);
                int addr = Utilities.parseAddress(address);
                if (!breakpoints.contains(addr)) {
                    breakpoints.add(addr);
                }
                RefreshDebuggerDisassembly(false);
            } catch (NumberFormatException e) {
                // Ignore it, probably already a breakpoint there
            }
        } else {
            JpcspDialogManager.showInformation(this, "Breakpoint Help : " + "Select the line to add a breakpoint to.");
        }
}//GEN-LAST:event_AddBreakpointActionPerformed

private void DeleteAllBreakpointsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DeleteAllBreakpointsActionPerformed
        DeleteAllBreakpoints();
}//GEN-LAST:event_DeleteAllBreakpointsActionPerformed

    public void DeleteAllBreakpoints() {
        if (!breakpoints.isEmpty()) {
            breakpoints.clear();
            RefreshDebuggerDisassembly(false);
        }
    }

private void DeleteBreakpointActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DeleteBreakpointActionPerformed
        String value = (String) disasmListGetSelectedValue();
        if (value != null) {
            boolean breakpointexists = value.startsWith("<*>");
            if (breakpointexists) {
                String address = value.substring(3, 11);
                int addr = Utilities.parseAddress(address);
                int b = breakpoints.indexOf(addr);
                breakpoints.remove(b);
                RefreshDebuggerDisassembly(false);
            }
        } else {
            JpcspDialogManager.showInformation(this, "Breakpoint Help : " + "Select the line to remove a breakpoint from.");
        }
}//GEN-LAST:event_DeleteBreakpointActionPerformed

    private void removeTemporaryBreakpoints() {
        if (temporaryBreakpoint1 != 0) {
            breakpoints.remove(new Integer(temporaryBreakpoint1));
            temporaryBreakpoint1 = 0;
        }
        if (temporaryBreakpoint2 != 0) {
            breakpoints.remove(new Integer(temporaryBreakpoint2));
            temporaryBreakpoint2 = 0;
        }
    }

    private void addTemporaryBreakpoints() {
        if (temporaryBreakpoint1 != 0) {
            breakpoints.add(new Integer(temporaryBreakpoint1));
        }
        if (temporaryBreakpoint2 != 0) {
            breakpoints.add(new Integer(temporaryBreakpoint2));
        }
    }

    private void setTemporaryBreakpoints(boolean stepOver) {
        removeTemporaryBreakpoints();

        int pc = Emulator.getProcessor().cpu.pc;
        int opcode = Emulator.getMemory().read32(pc);
        Instruction insn = Decoder.instruction(opcode);
        if (insn != null) {
            int branchingTo = 0;
            boolean isBranching = false;
            int npc = pc + 4;
            if (stepOver && insn.hasFlags(Instruction.FLAG_STARTS_NEW_BLOCK)) {
                // Stepping over new blocks
            } else if (insn.hasFlags(Instruction.FLAG_IS_JUMPING)) {
                branchingTo = Compiler.jumpTarget(npc, opcode);
                isBranching = true;
            } else if (insn.hasFlags(Instruction.FLAG_IS_BRANCHING)) {
                branchingTo = Compiler.branchTarget(npc, opcode);
                isBranching = true;
            } else if (insn == Instructions.JR) {
                int rs = (opcode >> 21) & 31;
                branchingTo = Emulator.getProcessor().cpu.getRegister(rs);
                isBranching = true;
                // End of stepOut when reaching "jr $ra"
                if (stepOut && rs == _ra) {
                    stepOut = false;
                }
            } else if (insn == Instructions.JALR && !stepOver) {
                int rs = (opcode >> 21) & 31;
                branchingTo = Emulator.getProcessor().cpu.getRegister(rs);
                isBranching = true;
            }

            if (!isBranching) {
                temporaryBreakpoint1 = npc;
            } else if (branchingTo != 0) {
                temporaryBreakpoint1 = branchingTo;
                if (insn.hasFlags(Instruction.FLAG_IS_CONDITIONAL)) {
                    temporaryBreakpoint2 = npc;
                    if (insn.hasFlags(Instruction.FLAG_HAS_DELAY_SLOT)) {
                        // Also skip the delay slot instruction
                        temporaryBreakpoint2 += 4;
                    }
                }
            }
        }

        addTemporaryBreakpoints();
        emu.RunEmu();
    }

private void StepIntoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StepIntoActionPerformed
        setTemporaryBreakpoints(false);
}//GEN-LAST:event_StepIntoActionPerformed

private void StepOverActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StepOverActionPerformed
        stepOut = false;
        setTemporaryBreakpoints(true);
}//GEN-LAST:event_StepOverActionPerformed

private void StepOutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StepOutActionPerformed
        stepOut = true;
        setTemporaryBreakpoints(true);
}//GEN-LAST:event_StepOutActionPerformed

private void RunDebuggerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RunDebuggerActionPerformed
        stepOut = false;
        removeTemporaryBreakpoints();
        emu.RunEmu();
}//GEN-LAST:event_RunDebuggerActionPerformed

// Called from Emulator
    public void step() {
        // Fast check (most common case): nothing to do if there are no breakpoints at all.
        if (breakpoints.isEmpty()) {
            return;
        }

        // Check if we have reached a breakpoint
        if (breakpoints.contains(Emulator.getProcessor().cpu.pc)) {
            if (stepOut) {
                // When stepping out, step over all instructions
                // until we reach "jr $ra".
                setTemporaryBreakpoints(true);
            } else {
                removeTemporaryBreakpoints();

                Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_BREAKPOINT);
            }
        }
    }

private void PauseDebuggerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PauseDebuggerActionPerformed
        Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_PAUSE);
}//GEN-LAST:event_PauseDebuggerActionPerformed

    public void RefreshButtons() {
        // Called from Emulator
        RunDebugger.setSelected(Emulator.run && !Emulator.pause);
        PauseDebugger.setSelected(Emulator.run && Emulator.pause);

        btnCapture.setSelected(State.captureGeNextFrame);
        btnReplay.setSelected(State.replayGeNextFrame);
    }

private void formWindowDeactivated(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowDeactivated
        //Called when the mainWindow is closed
        if (Settings.getInstance().readBool("gui.saveWindowPos")) {
            Settings.getInstance().writeWindowPos("disassembler", getLocation());
        }
}//GEN-LAST:event_formWindowDeactivated

    private boolean isCellChecked(JTable table) {
        for (int i = 0; i < table.getRowCount(); i++) {
            if (table.isCellSelected(i, 1)) {
                return true;
            }

        }
        return false;
    }

private void CopyValueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CopyValueActionPerformed
        if (cop1Table.isShowing()) {
            float value = (Float) cop1Table.getValueAt(cop1Table.getSelectedRow(), 1);
            StringSelection stringSelection = new StringSelection(Float.toString(value));
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, this);
        }
}//GEN-LAST:event_CopyValueActionPerformed

    public int GetGPI() {
        return gpi;
    }

    public void SetGPO(int gpo) {
        this.gpo = gpo;
        // TODO if we want to use a visibility check here, then we need to refresh
        // gpo onFocus too otherwise it will be stale.
        //if (jPanel1.isVisible()) {
        // Refresh GPO
        for (int i = 0; i < 8; i++) {
            SetGPO(i, (gpo & (1 << i)) != 0);
        }
        //}
    }

    private void ToggleGPI(int index) {
        gpi ^= 1 << index;

        // Refresh GPI buttons
        for (int i = 0; i < 8; i++) {
            SetGPI(i, (gpi & (1 << i)) != 0);
        }
    }

    private void SetGPO(int index, boolean on) {
        switch (index) {
            case 0:
                gpoLabel1.setEnabled(on);
                break;
            case 1:
                gpoLabel2.setEnabled(on);
                break;
            case 2:
                gpoLabel3.setEnabled(on);
                break;
            case 3:
                gpoLabel4.setEnabled(on);
                break;
            case 4:
                gpoLabel5.setEnabled(on);
                break;
            case 5:
                gpoLabel6.setEnabled(on);
                break;
            case 6:
                gpoLabel7.setEnabled(on);
                break;
            case 7:
                gpoLabel8.setEnabled(on);
                break;
        }
    }

    private void SetGPI(int index, boolean on) {
        switch (index) {
            case 0:
                gpiButton1.setSelected(on);
                break;
            case 1:
                gpiButton2.setSelected(on);
                break;
            case 2:
                gpiButton3.setSelected(on);
                break;
            case 3:
                gpiButton4.setSelected(on);
                break;
            case 4:
                gpiButton5.setSelected(on);
                break;
            case 5:
                gpiButton6.setSelected(on);
                break;
            case 6:
                gpiButton7.setSelected(on);
                break;
            case 7:
                gpiButton8.setSelected(on);
                break;
        }
    }

private void SetPCToCursorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SetPCToCursorActionPerformed
        int index = disasmListGetSelectedIndex();
        if (index != -1) {
            Emulator.getProcessor().cpu.pc = DebuggerPC + index * 4;
            RefreshDebuggerDisassembly(true);
        } else {
            System.out.println("dpc: " + Integer.toHexString(DebuggerPC));
            System.out.println("idx: " + Integer.toHexString(index));
            System.out.println("npc: " + Integer.toHexString(DebuggerPC + index * 4));
        }
}//GEN-LAST:event_SetPCToCursorActionPerformed

private void ExportBreaksActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ExportBreaksActionPerformed
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("jpcsp/languages/jpcsp");
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(State.discId + ".brk"));
        fc.setDialogTitle(bundle.getString("DisassemblerFrame.miExportBreakpoints.text"));
        fc.setCurrentDirectory(new java.io.File("."));
        fc.addChoosableFileFilter(Constants.fltBreakpointFiles);
        fc.setFileFilter(Constants.fltBreakpointFiles);

        int returnVal = fc.showSaveDialog(this);
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File f = fc.getSelectedFile();
        BufferedWriter out = null;
        try {
            if (f.exists()) {
                int res = JOptionPane.showConfirmDialog(
                        this,
                        bundle.getString("ConsoleWindow.strFileExists.text"),
                        bundle.getString("DisassemblerFrame.dlgExportBP.title"),
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);

                if (res != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            out = new BufferedWriter(new FileWriter(f));

            for (int i = 0; i < breakpoints.size(); i++) {
                out.write(Integer.toHexString(breakpoints.get(i)) + System.getProperty("line.separator"));
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            Utilities.close(out);
        }
}//GEN-LAST:event_ExportBreaksActionPerformed

private void ImportBreaksActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ImportBreaksActionPerformed
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("jpcsp/languages/jpcsp");
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(bundle.getString("DisassemblerFrame.miImportBreakpoints.text"));
        fc.setCurrentDirectory(new java.io.File("."));
        fc.addChoosableFileFilter(Constants.fltBreakpointFiles);
        fc.setFileFilter(Constants.fltBreakpointFiles);

        int returnVal = fc.showOpenDialog(this);
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File f = fc.getSelectedFile();
        BufferedReader in = null;
        try {
            // TODO check content instead of ending
            if (!f.getName().contains(".brk")) {
                JOptionPane.showMessageDialog(
                        this,
                        bundle.getString("DisassemblerFrame.strInvalidBRKFile.text"),
                        bundle.getString("DisassemblerFrame.dlgImportBP.title"),
                        JOptionPane.ERROR_MESSAGE);

                return;
            }

            in = new BufferedReader(new FileReader(f));
            String nextBrk = in.readLine();

            while (nextBrk != null) {
                breakpoints.add(Integer.parseInt(nextBrk, 16));
                nextBrk = in.readLine();
            }

            RefreshDebuggerDisassembly(false);

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            Utilities.close(in);
        }
}//GEN-LAST:event_ImportBreaksActionPerformed

    private void ManageMemBreaksActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ManageMemBreaksActionPerformed
        if (mbpDialog == null) {
            mbpDialog = new MemoryBreakpointsDialog(this);
        }
        mbpDialog.setVisible(true);
    }//GEN-LAST:event_ManageMemBreaksActionPerformed

    private void CloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CloseActionPerformed
        setVisible(false);
    }//GEN-LAST:event_CloseActionPerformed

    private void txtSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtSearchActionPerformed

        //Basic text-based search function
        //TODO: Figure out an effective method of optimizing the search and avoid
        //hogging the emulator

        String text = txtSearch.getText();
        String current = "";

        disasmList.setSelectedIndex(srcounter);

        while (!current.contains(text)) {
            DebuggerPC += 4;
            SelectedPC = DebuggerPC;
            RefreshDebugger(false);
            updateSelectedIndex();

            current = (String) disasmListGetSelectedValue();
            srcounter++;
        }
    }//GEN-LAST:event_txtSearchActionPerformed

    private void btnDumpDebugStateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDumpDebugStateActionPerformed
        DumpDebugState.dumpDebugState();
    }//GEN-LAST:event_btnDumpDebugStateActionPerformed

    private void gpiButton8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gpiButton8ActionPerformed
        ToggleGPI(7);
    }//GEN-LAST:event_gpiButton8ActionPerformed

    private void gpiButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gpiButton7ActionPerformed
        ToggleGPI(6);
    }//GEN-LAST:event_gpiButton7ActionPerformed

    private void gpiButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gpiButton6ActionPerformed
        ToggleGPI(5);
    }//GEN-LAST:event_gpiButton6ActionPerformed

    private void gpiButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gpiButton5ActionPerformed
        ToggleGPI(4);
    }//GEN-LAST:event_gpiButton5ActionPerformed

    private void gpiButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gpiButton4ActionPerformed
        ToggleGPI(3);
    }//GEN-LAST:event_gpiButton4ActionPerformed

    private void gpiButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gpiButton3ActionPerformed
        ToggleGPI(2);
    }//GEN-LAST:event_gpiButton3ActionPerformed

    private void gpiButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gpiButton2ActionPerformed
        ToggleGPI(1);
    }//GEN-LAST:event_gpiButton2ActionPerformed

    private void gpiButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gpiButton1ActionPerformed
        ToggleGPI(0);
    }//GEN-LAST:event_gpiButton1ActionPerformed

    private void cop1TableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_cop1TableMouseClicked
        if (SwingUtilities.isRightMouseButton(evt) && cop1Table.isColumnSelected(1) && isCellChecked(cop1Table)) {
            RegMenu.show(cop1Table, evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_cop1TableMouseClicked

    private void disasmListComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_disasmListComponentResized
        RefreshDebuggerDisassembly(false);
    }//GEN-LAST:event_disasmListComponentResized

    private void btnCaptureActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCaptureActionPerformed
        State.captureGeNextFrame = btnCapture.isSelected();
    }//GEN-LAST:event_btnCaptureActionPerformed

    private void btnReplayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnReplayActionPerformed
        State.replayGeNextFrame = btnReplay.isSelected();
    }//GEN-LAST:event_btnReplayActionPerformed

    @Override
    public void dispose() {
        if (mbpDialog != null) {
            mbpDialog.dispose();
        }
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
    private javax.swing.JButton ManageMemBreaks;
    private javax.swing.JButton PauseDebugger;
    private javax.swing.JPopupMenu RegMenu;
    private javax.swing.JButton ResetToPCbutton;
    private javax.swing.JToggleButton RunDebugger;
    private javax.swing.JMenuItem SetPCToCursor;
    private javax.swing.JToggleButton btnCapture;
    private javax.swing.JButton btnDumpDebugState;
    private javax.swing.JToggleButton btnReplay;
    private javax.swing.JButton btnStepInto;
    private javax.swing.JButton btnStepOut;
    private javax.swing.JButton btnStepOver;
    private javax.swing.JTable cop0Table;
    private javax.swing.JTable cop1Table;
    private javax.swing.JList disasmList;
    private javax.swing.JTabbedPane disasmTabs;
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
    private jpcsp.Debugger.DisassemblerModule.RegisterTable gprTable;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator10;
    private javax.swing.JPopupMenu.Separator jSeparator11;
    private javax.swing.JToolBar.Separator jSeparator2;
    private javax.swing.JToolBar.Separator jSeparator3;
    private javax.swing.JToolBar.Separator jSeparator4;
    private javax.swing.JToolBar.Separator jSeparator7;
    private javax.swing.JPopupMenu.Separator jSeparator9;
    private javax.swing.JLabel lblCaptureReplay;
    private javax.swing.JLabel lblDumpState;
    private javax.swing.JLabel lblSearch;
    private javax.swing.JMenu mBreakpoints;
    private javax.swing.JMenu mDebug;
    private javax.swing.JMenu mDisassembler;
    private javax.swing.JMenu mFile;
    private javax.swing.JMenuBar mbMain;
    private javax.swing.JMenuItem miClose;
    private javax.swing.JMenuItem miDeleteAllBreakpoints;
    private javax.swing.JMenuItem miDeleteBreakpoint;
    private javax.swing.JMenuItem miDumpCode;
    private javax.swing.JMenuItem miExportBreakpoints;
    private javax.swing.JMenuItem miImportBreakpoints;
    private javax.swing.JMenuItem miJumpTo;
    private javax.swing.JMenuItem miManageMemoryBreakpoints;
    private javax.swing.JMenuItem miNewBreakpoint;
    private javax.swing.JMenuItem miPause;
    private javax.swing.JMenuItem miResetToPC;
    private javax.swing.JMenuItem miRun;
    private javax.swing.JMenuItem miStepInto;
    private javax.swing.JMenuItem miStepOut;
    private javax.swing.JMenuItem miStepOver;
    private javax.swing.JPanel miscPanel;
    private javax.swing.JToolBar tbBreakpoints;
    private javax.swing.JToolBar tbDisasm;
    private javax.swing.JTextField txtSearch;
    // End of variables declaration//GEN-END:variables

    private static class ClickAction extends AbstractAction {

        private static final long serialVersionUID = -6595335927462915819L;
        private JButton button;

        public ClickAction(JButton button) {
            this.button = button;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            button.doClick();
        }
    }
}
