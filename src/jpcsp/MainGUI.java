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

package jpcsp;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import jpcsp.Allegrex.compiler.Compiler;
import jpcsp.Allegrex.compiler.Profiler;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.Debugger.ElfHeaderInfo;
import jpcsp.Debugger.InstructionCounter;
import jpcsp.Debugger.MemoryViewer;
import jpcsp.Debugger.StepLogger;
import jpcsp.Debugger.DisassemblerModule.DisassemblerFrame;
import jpcsp.Debugger.DisassemblerModule.VfpuFrame;
import jpcsp.GUI.CheatsGUI;
import jpcsp.GUI.MemStickBrowser;
import jpcsp.GUI.RecentElement;
import jpcsp.GUI.SettingsGUI;
import jpcsp.GUI.UmdBrowser;
import jpcsp.HLE.Modules;
import jpcsp.HLE.SyscallHandler;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.HLE.modules.sceAtrac3plus;
import jpcsp.HLE.modules.sceDisplay;
import jpcsp.HLE.modules.sceMpeg;
import jpcsp.filesystems.umdiso.UmdIsoFile;
import jpcsp.filesystems.umdiso.UmdIsoReader;
import jpcsp.format.PSF;
import jpcsp.graphics.VideoEngine;
import jpcsp.log.LogWindow;
import jpcsp.log.LoggingOutputStream;
import jpcsp.util.JpcspDialogManager;
import jpcsp.util.MetaInformation;
import jpcsp.util.Utilities;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

/**
 *
 * @author  shadow
 */
public class MainGUI extends javax.swing.JFrame implements KeyListener, ComponentListener {
    private static final long serialVersionUID = -3647025845406693230L;
    public static final int MAX_RECENT = 10;
    LogWindow consolewin;
    ElfHeaderInfo elfheader;
    SettingsGUI setgui;
    MemStickBrowser memstick;
    Emulator emulator;
    UmdBrowser umdbrowser;
    InstructionCounter instructioncounter;
    File loadedFile;
    boolean umdLoaded;
    private Point mainwindowPos; // stores the last known window position
    private boolean snapConsole = true;
    private List<RecentElement> recentUMD = new LinkedList<RecentElement>();
    private List<RecentElement> recentFile = new LinkedList<RecentElement>();
    public final static String windowNameForSettings = "mainwindow";

    /** Creates new form MainGUI */
    public MainGUI() {
        DOMConfigurator.configure("LogSettings.xml");
        System.setOut(new PrintStream(new LoggingOutputStream(Logger.getLogger("misc"), Level.INFO)));
        consolewin = new LogWindow();

        emulator = new Emulator(this);

        Resource.add("jpcsp.languages." + Settings.getInstance().readLanguage());

        /*next two lines are for overlay menus over joglcanvas*/
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
        //end of

        initComponents();
        populateRecentMenu();

        setLocation(Settings.getInstance().readWindowPos(windowNameForSettings));
        State.fileLogger.setLocation(getLocation().x + 488, getLocation().y + 18);
        setTitle(MetaInformation.FULL_NAME);

        /*add glcanvas to frame and pack frame to get the canvas size*/
        getContentPane().add(Modules.sceDisplayModule, java.awt.BorderLayout.CENTER);
        Modules.sceDisplayModule.addKeyListener(this);
        addComponentListener(this);
        pack();

        Insets insets = getInsets();
        Dimension minSize = new Dimension(
            480 + insets.left + insets.right,
            272 + insets.top + insets.bottom);
        setMinimumSize(minSize);

        //logging console window stuff
        snapConsole = Settings.getInstance().readBool("gui.snapLogwindow");
        if (snapConsole) {
            mainwindowPos = getLocation();
            consolewin.setLocation(mainwindowPos.x, mainwindowPos.y + getHeight());
        } else {
            consolewin.setLocation(Settings.getInstance().readWindowPos("logwindow"));
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jToolBar1 = new javax.swing.JToolBar();
        RunButton = new javax.swing.JToggleButton();
        PauseButton = new javax.swing.JToggleButton();
        ResetButton = new javax.swing.JButton();
        MenuBar = new javax.swing.JMenuBar();
        FileMenu = new javax.swing.JMenu();
        openUmd = new javax.swing.JMenuItem();
        OpenFile = new javax.swing.JMenuItem();
        OpenMemStick = new javax.swing.JMenuItem();
        RecentMenu = new javax.swing.JMenu();
        ExitEmu = new javax.swing.JMenuItem();
        EmulationMenu = new javax.swing.JMenu();
        RunEmu = new javax.swing.JMenuItem();
        PauseEmu = new javax.swing.JMenuItem();
        ResetEmu = new javax.swing.JMenuItem();
        OptionsMenu = new javax.swing.JMenu();
        RotateItem = new javax.swing.JMenuItem();
        SetttingsMenu = new javax.swing.JMenuItem();
        ShotItem = new javax.swing.JMenuItem();
        SaveSnap = new javax.swing.JMenuItem();
        LoadSnap = new javax.swing.JMenuItem();
        DebugMenu = new javax.swing.JMenu();
        ToolsSubMenu = new javax.swing.JMenu();
        EnterDebugger = new javax.swing.JMenuItem();
        EnterMemoryViewer = new javax.swing.JMenuItem();
        VfpuRegisters = new javax.swing.JMenuItem();
        ElfHeaderViewer = new javax.swing.JMenuItem();
        FileLog = new javax.swing.JMenuItem();
        InstructionCounter = new javax.swing.JMenuItem();
        ToggleConsole = new javax.swing.JMenuItem();
        DumpIso = new javax.swing.JMenuItem();
        ResetProfiler = new javax.swing.JMenuItem();
        CheatsMenu = new javax.swing.JMenu();
        cwcheat = new javax.swing.JMenuItem();
        LanguageMenu = new javax.swing.JMenu();
        English = new javax.swing.JMenuItem();
        French = new javax.swing.JMenuItem();
        German = new javax.swing.JMenuItem();
        Lithuanian = new javax.swing.JMenuItem();
        Spanish = new javax.swing.JMenuItem();
        Catalan = new javax.swing.JMenuItem();
        PortugueseBR = new javax.swing.JMenuItem();
        Portuguese = new javax.swing.JMenuItem();
        Japanese = new javax.swing.JMenuItem();
        Russian = new javax.swing.JMenuItem();
        Polish = new javax.swing.JMenuItem();
        HelpMenu = new javax.swing.JMenu();
        About = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setForeground(java.awt.Color.white);
        setMinimumSize(new java.awt.Dimension(480, 272));
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
			public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);

        RunButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/PlayIcon.png"))); // NOI18N
        RunButton.setText(Resource.get("run"));
        RunButton.setFocusable(false);
        RunButton.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        RunButton.setIconTextGap(2);
        RunButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        RunButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RunButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(RunButton);

        PauseButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/PauseIcon.png"))); // NOI18N
        PauseButton.setText(Resource.get("pause"));
        PauseButton.setFocusable(false);
        PauseButton.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        PauseButton.setIconTextGap(2);
        PauseButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        PauseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PauseButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(PauseButton);

        ResetButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/StopIcon.png"))); // NOI18N
        ResetButton.setText(Resource.get("reset"));
        ResetButton.setFocusable(false);
        ResetButton.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        ResetButton.setIconTextGap(2);
        ResetButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        ResetButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ResetButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(ResetButton);

        getContentPane().add(jToolBar1, java.awt.BorderLayout.NORTH);

        MenuBar.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
			public void mouseExited(java.awt.event.MouseEvent evt) {
                MenuBarMouseExited(evt);
            }
        });

        FileMenu.setText(Resource.get("file"));

        openUmd.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        openUmd.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/LoadUmdIcon.png"))); // NOI18N
        openUmd.setText(Resource.get("loadumd"));
        openUmd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openUmdActionPerformed(evt);
            }
        });
        FileMenu.add(openUmd);

        OpenFile.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.ALT_MASK));
        OpenFile.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/LoadFileIcon.png"))); // NOI18N
        OpenFile.setText(Resource.get("loadfile"));
        OpenFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                OpenFileActionPerformed(evt);
            }
        });
        FileMenu.add(OpenFile);

        OpenMemStick.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.SHIFT_MASK));
        OpenMemStick.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/LoadMemoryStick.png"))); // NOI18N
        OpenMemStick.setText(Resource.get("loadmemstick"));
        OpenMemStick.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                OpenMemStickActionPerformed(evt);
            }
        });
        FileMenu.add(OpenMemStick);

        RecentMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/RecentIcon.png"))); // NOI18N
        RecentMenu.setText(Resource.get("loadrecent"));
        FileMenu.add(RecentMenu);

        ExitEmu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E, java.awt.event.InputEvent.CTRL_MASK));
        ExitEmu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/CloseIcon.png"))); // NOI18N
        ExitEmu.setText(Resource.get("exit"));
        ExitEmu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ExitEmuActionPerformed(evt);
            }
        });
        FileMenu.add(ExitEmu);

        MenuBar.add(FileMenu);

        EmulationMenu.setText(Resource.get("emulation"));

        RunEmu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F2, 0));
        RunEmu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/PlayIcon.png"))); // NOI18N
        RunEmu.setText(Resource.get("run"));
        RunEmu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RunEmuActionPerformed(evt);
            }
        });
        EmulationMenu.add(RunEmu);

        PauseEmu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F3, 0));
        PauseEmu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/PauseIcon.png"))); // NOI18N
        PauseEmu.setText(Resource.get("pause"));
        PauseEmu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PauseEmuActionPerformed(evt);
            }
        });
        EmulationMenu.add(PauseEmu);

        ResetEmu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, 0));
        ResetEmu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/StopIcon.png"))); // NOI18N
        ResetEmu.setText(Resource.get("reset"));
        ResetEmu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ResetEmuActionPerformed(evt);
            }
        });
        EmulationMenu.add(ResetEmu);

        MenuBar.add(EmulationMenu);

        OptionsMenu.setText(Resource.get("options"));

        RotateItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F11, 0));
        RotateItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/RotateIcon.png"))); // NOI18N
        RotateItem.setText(Resource.get("rotate"));
        RotateItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RotateItemActionPerformed(evt);
            }
        });
        OptionsMenu.add(RotateItem);

        SetttingsMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F12, 0));
        SetttingsMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/SettingsIcon.png"))); // NOI18N
        SetttingsMenu.setText(Resource.get("settings"));
        SetttingsMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SetttingsMenuActionPerformed(evt);
            }
        });
        OptionsMenu.add(SetttingsMenu);

        ShotItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F5, 0));
        ShotItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/ScreenshotIcon.png"))); // NOI18N
        ShotItem.setText(Resource.get("screenshot"));
        ShotItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ShotItemActionPerformed(evt);
            }
        });
        OptionsMenu.add(ShotItem);

        SaveSnap.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.SHIFT_MASK));
        SaveSnap.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/SaveStateIcon.png"))); // NOI18N
        SaveSnap.setText(Resource.get("savesnapshot"));
        SaveSnap.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SaveSnapActionPerformed(evt);
            }
        });
        OptionsMenu.add(SaveSnap);

        LoadSnap.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.SHIFT_MASK));
        LoadSnap.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/LoadStateIcon.png"))); // NOI18N
        LoadSnap.setText(Resource.get("loadsnapshot"));
        LoadSnap.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LoadSnapActionPerformed(evt);
            }
        });
        OptionsMenu.add(LoadSnap);

        MenuBar.add(OptionsMenu);

        DebugMenu.setText(Resource.get("debug"));

        ToolsSubMenu.setText(Resource.get("toolsmenu"));

        EnterDebugger.setText(Resource.get("enterdebugger"));
        EnterDebugger.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                EnterDebuggerActionPerformed(evt);
            }
        });
        ToolsSubMenu.add(EnterDebugger);

        EnterMemoryViewer.setText(Resource.get("memoryviewer"));
        EnterMemoryViewer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                EnterMemoryViewerActionPerformed(evt);
            }
        });
        ToolsSubMenu.add(EnterMemoryViewer);

        VfpuRegisters.setText(Resource.get("vfpuregisters"));
        VfpuRegisters.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                VfpuRegistersActionPerformed(evt);
            }
        });
        ToolsSubMenu.add(VfpuRegisters);

        ElfHeaderViewer.setText(Resource.get("elfheaderinfo"));
        ElfHeaderViewer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ElfHeaderViewerActionPerformed(evt);
            }
        });
        ToolsSubMenu.add(ElfHeaderViewer);

        FileLog.setText(Resource.get("filelog"));
        FileLog.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FileLogActionPerformed(evt);
            }
        });
        ToolsSubMenu.add(FileLog);

        InstructionCounter.setText(Resource.get("instructioncounter"));
        InstructionCounter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                InstructionCounterActionPerformed(evt);
            }
        });
        ToolsSubMenu.add(InstructionCounter);

        DebugMenu.add(ToolsSubMenu);

        ToggleConsole.setText(Resource.get("toggleconsole"));
        ToggleConsole.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ToggleConsoleActionPerformed(evt);
            }
        });
        DebugMenu.add(ToggleConsole);

        DumpIso.setText(Resource.get("dumpisotoisoindex"));
        DumpIso.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DumpIsoActionPerformed(evt);
            }
        });
        DebugMenu.add(DumpIso);

        ResetProfiler.setText(Resource.get("resetprofilerinformation"));
        ResetProfiler.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ResetProfilerActionPerformed(evt);
            }
        });
        DebugMenu.add(ResetProfiler);

        MenuBar.add(DebugMenu);

        CheatsMenu.setText(Resource.get("cheatsmenu"));

        cwcheat.setText("CWCheat");
        cwcheat.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cwcheatActionPerformed(evt);
            }
        });
        CheatsMenu.add(cwcheat);

        MenuBar.add(CheatsMenu);

        LanguageMenu.setText(Resource.get("language"));

        English.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/flags/en_EN_Icon.png"))); // NOI18N
        English.setText(Resource.get("english"));
        English.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                EnglishActionPerformed(evt);
            }
        });
        LanguageMenu.add(English);

        French.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/flags/fr_FR_Icon.png"))); // NOI18N
        French.setText(Resource.get("french"));
        French.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FrenchActionPerformed(evt);
            }
        });
        LanguageMenu.add(French);

        German.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/flags/de_DE_Icon.png"))); // NOI18N
        German.setText(Resource.get("german"));
        German.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GermanActionPerformed(evt);
            }
        });
        LanguageMenu.add(German);

        Lithuanian.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/flags/lt_LT_Icon.png"))); // NOI18N
        Lithuanian.setText(Resource.get("lithuanian"));
        Lithuanian.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LithuanianActionPerformed(evt);
            }
        });
        LanguageMenu.add(Lithuanian);

        Spanish.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/flags/es_ES_Icon.png"))); // NOI18N
        Spanish.setText(Resource.get("spanish"));
        Spanish.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SpanishActionPerformed(evt);
            }
        });
        LanguageMenu.add(Spanish);

        Catalan.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/flags/es_CA_Icon.png"))); // NOI18N
        Catalan.setText(Resource.get("catalan"));
        Catalan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CatalanActionPerformed(evt);
            }
        });
        LanguageMenu.add(Catalan);

        PortugueseBR.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/flags/pt_BR_Icon.png"))); // NOI18N
        PortugueseBR.setText(Resource.get("portuguesebr"));
        PortugueseBR.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PortugueseBRActionPerformed(evt);
            }
        });
        LanguageMenu.add(PortugueseBR);

        Portuguese.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/flags/pt_PT_Icon.png"))); // NOI18N
        Portuguese.setText(Resource.get("portuguese"));
        Portuguese.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PortugueseActionPerformed(evt);
            }
        });
        LanguageMenu.add(Portuguese);

        Japanese.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/flags/jp_JP_Icon.png"))); // NOI18N
        Japanese.setText(Resource.get("japanese"));
        Japanese.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                JapaneseActionPerformed(evt);
            }
        });
        LanguageMenu.add(Japanese);

        Russian.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/flags/ru_RU_Icon.png"))); // NOI18N
        Russian.setText(Resource.get("russian"));
        Russian.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RussianActionPerformed(evt);
            }
        });
        LanguageMenu.add(Russian);

        Polish.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/flags/pl_PL_Icon.png"))); // NOI18N
        Polish.setText(Resource.get("polish"));
        Polish.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PolishActionPerformed(evt);
            }
        });
        LanguageMenu.add(Polish);

        MenuBar.add(LanguageMenu);

        HelpMenu.setText(Resource.get("help"));

        About.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
        About.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/AboutIcon.png"))); // NOI18N
        About.setText(Resource.get("about"));
        About.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AboutActionPerformed(evt);
            }
        });
        HelpMenu.add(About);

        MenuBar.add(HelpMenu);

        setJMenuBar(MenuBar);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void changeLanguage(String language) {
         Resource.add("jpcsp.languages." + language);
         Settings.getInstance().writeLanguage(language);
         initComponents();
    }

    public LogWindow getConsoleWindow() {
        return consolewin;
    }

    private void populateRecentMenu() {
        RecentMenu.removeAll();
        recentUMD.clear();
        recentFile.clear();

        Settings.getInstance().readRecent("umd", recentUMD);
        Settings.getInstance().readRecent("file", recentFile);
       
        for (RecentElement umd : recentUMD) {
            JMenuItem item = new JMenuItem(umd.toString());
            //item.setFont(Settings.getInstance().getFont()); // doesn't seem to work
            item.addActionListener(new RecentElementActionListener(RecentElementActionListener.TYPE_UMD, umd.path));
            RecentMenu.add(item);
        }

        if (recentUMD.size() > 0 && recentFile.size() > 0) {
            RecentMenu.addSeparator();
        }
        
        for (RecentElement file : recentFile) {
            JMenuItem item = new JMenuItem(file.toString());
            item.addActionListener(new RecentElementActionListener(RecentElementActionListener.TYPE_FILE, file.path));
            RecentMenu.add(item);
        }
    }

private void ToggleConsoleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ToggleConsoleActionPerformed
    if (!consolewin.isVisible() && snapConsole) {
        mainwindowPos = this.getLocation();
        consolewin.setLocation(mainwindowPos.x, mainwindowPos.y + getHeight());
    }
    consolewin.setVisible(!consolewin.isVisible());
}//GEN-LAST:event_ToggleConsoleActionPerformed

private void EnterDebuggerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EnterDebuggerActionPerformed
    PauseEmu();
    if (State.debugger == null)
    {
        State.debugger = new DisassemblerFrame(emulator);
        State.debugger.setLocation(Settings.getInstance().readWindowPos("disassembler"));
        State.debugger.setVisible(true);
    }
    else
    {
        State.debugger.setVisible(true);
        State.debugger.RefreshDebugger(false);
    }
}//GEN-LAST:event_EnterDebuggerActionPerformed

private void RunButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RunButtonActionPerformed
            RunEmu();
}//GEN-LAST:event_RunButtonActionPerformed
 private JFileChooser makeJFileChooser() {
        final JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Open Elf/Pbp File");
        fc.setCurrentDirectory(new java.io.File("."));
        return fc;
    }

private void OpenFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_OpenFileActionPerformed
    PauseEmu();

    final JFileChooser fc = makeJFileChooser();
    int returnVal = fc.showOpenDialog(this);

    if (userChooseSomething(returnVal)) {
        File file = fc.getSelectedFile();
        loadFile(file);
    } else {
        return; //user cancel the action

    }
}//GEN-LAST:event_OpenFileActionPerformed

private String pspifyFilename(String pcfilename) {
    // Files relative to ms0 directory
    if (pcfilename.startsWith("ms0")) {
        return "ms0:" + pcfilename.substring(3).replaceAll("\\\\", "/");
    }

    // Files with absolute path but also in ms0 directory
    try {
        String ms0path = new File("ms0").getCanonicalPath();
        if (pcfilename.startsWith(ms0path)) {
            // Strip off absolute prefix
            return "ms0:" + pcfilename.substring(ms0path.length()).replaceAll("\\\\", "/");
        }
    } catch(Exception e) {
        // Required by File.getCanonicalPath
        e.printStackTrace();
    }

    // Files anywhere on user's hard drive, may not work
    // use host0:/ ?
    return pcfilename.replaceAll("\\\\", "/");
}

public void loadFile(File file) {
    //This is where a real application would open the file.
    try {
        if (consolewin != null)
            consolewin.clearScreenMessages();
        Emulator.log.info(MetaInformation.FULL_NAME);

        umdLoaded = false;
        loadedFile = file;

        // Create a read-only memory-mapped file
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        FileChannel roChannel = raf.getChannel();
        ByteBuffer readbuffer = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, (int)roChannel.size());
        SceModule module = emulator.load(pspifyFilename(file.getPath()), readbuffer);
        roChannel.close(); // doesn't seem to work properly :(
        raf.close(); // still doesn't work properly :/

        PSF psf = module.psf;
        String title;
        String discId = State.DISCID_UNKNOWN_FILE;
        boolean isHomebrew;
        if (psf != null) {
            title = psf.getPrintableString("TITLE");

            discId = psf.getString("DISC_ID");
            if (discId == null) {
                discId = State.DISCID_UNKNOWN_FILE;
            }

            isHomebrew = psf.isLikelyHomebrew();
        } else {
            title = file.getParentFile().getName();
            isHomebrew = true; // missing psf, assume homebrew
        }
        setTitle(MetaInformation.FULL_NAME + " - " + title);
        addRecentFile(file, title);

        // Strip off absolute file path if the file is inside our ms0 directory
        String filepath = file.getParent();
        String ms0path = new File("ms0").getCanonicalPath();
        if (filepath.startsWith(ms0path)) {
            filepath = filepath.substring(ms0path.length() - 3); // path must start with "ms0"
        }

        Modules.IoFileMgrForUserModule.setfilepath(filepath);
        Modules.IoFileMgrForUserModule.setIsoReader(null);
        jpcsp.HLE.Modules.sceUmdUserModule.setIsoReader(null);

        RuntimeContext.setIsHomebrew(isHomebrew);
        State.discId = discId;
        State.title = title;

        // use regular settings first
        installCompatibilitySettings();

        if (!isHomebrew && !discId.equals(State.DISCID_UNKNOWN_FILE)) {
            // override with patch file (allows incomplete patch files)
            installCompatibilityPatches(discId + ".patch");
        }

        if (instructioncounter != null)
            instructioncounter.RefreshWindow();
        StepLogger.clear();
        StepLogger.setName(file.getPath());
    } catch (GeneralJpcspException e) {
         JpcspDialogManager.showError(this, Resource.get("generalError")+" : " + e.getMessage());
     } catch (IOException e) {
         e.printStackTrace();
         JpcspDialogManager.showError(this, Resource.get("ioError")+" : " + e.getMessage());
     } catch (Exception ex) {
         ex.printStackTrace();
         if (ex.getMessage() != null) {
             JpcspDialogManager.showError(this, Resource.get("criticalError")+" : " + ex.getMessage());
         } else {
             JpcspDialogManager.showError(this, Resource.get("criticalError")+" : Check console for details.");
         }
     }
}

private void addRecentFile(File file, String title) {
    //try {
        String s = file.getPath(); //file.getCanonicalPath();
        for (int i = 0; i < recentFile.size(); ++i) {
            if (recentFile.get(i).path.equals(s))
                recentFile.remove(i--);
        }
        recentFile.add(0, new RecentElement(s, title));
        while(recentFile.size() > MAX_RECENT)
            recentFile.remove(MAX_RECENT);
        Settings.getInstance().writeRecent("file", recentFile);
        populateRecentMenu();
    //} catch (IOException e) {
    //    e.printStackTrace();
    //}
}

private void addRecentUMD(File file, String title) {
    try {
        String s = file.getCanonicalPath();
        for (int i = 0; i < recentUMD.size(); ++i) {
            if (recentUMD.get(i).path.equals(s))
                recentUMD.remove(i--);
        }
        recentUMD.add(0, new RecentElement(s, title));
        while(recentUMD.size() > MAX_RECENT)
            recentUMD.remove(MAX_RECENT);
        Settings.getInstance().writeRecent("umd", recentUMD);
        populateRecentMenu();
    } catch (IOException e) {
        e.printStackTrace();
    }
}

private void PauseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PauseButtonActionPerformed
    TogglePauseEmu();
}//GEN-LAST:event_PauseButtonActionPerformed

private void ElfHeaderViewerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ElfHeaderViewerActionPerformed
     if(elfheader==null)
     {

      elfheader = new ElfHeaderInfo();
      elfheader.setLocation(Settings.getInstance().readWindowPos("elfheader"));
      elfheader.setVisible(true);
     }
     else
     {
       elfheader.RefreshWindow();
       elfheader.setVisible(true);
     }
}//GEN-LAST:event_ElfHeaderViewerActionPerformed

private void EnterMemoryViewerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EnterMemoryViewerActionPerformed
    PauseEmu();
    if (State.memoryViewer == null)
    {
        State.memoryViewer = new MemoryViewer();
        State.memoryViewer.setLocation(Settings.getInstance().readWindowPos("memoryview"));
        State.memoryViewer.setVisible(true);
    }
    else
    {
        State.memoryViewer.RefreshMemory();
        State.memoryViewer.setVisible(true);
    }
}//GEN-LAST:event_EnterMemoryViewerActionPerformed

private void AboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AboutActionPerformed
  StringBuilder message = new StringBuilder();
    message.append("<html>").append("<h2>" + MetaInformation.FULL_NAME + "</h2>").append("<hr/>").append("Official site      : <a href='" + MetaInformation.OFFICIAL_SITE + "'>" + MetaInformation.OFFICIAL_SITE + "</a><br/>").append("Official forum     : <a href='" + MetaInformation.OFFICIAL_FORUM + "'>" + MetaInformation.OFFICIAL_FORUM + "</a><br/>").append("Official repository: <a href='" + MetaInformation.OFFICIAL_REPOSITORY + "'>" + MetaInformation.OFFICIAL_REPOSITORY + "</a><br/>").append("<hr/>").append("<i>Team:</i> <font color='gray'>" + MetaInformation.TEAM + "</font>").append("</html>");
    JOptionPane.showMessageDialog(this, message.toString(), MetaInformation.FULL_NAME, JOptionPane.INFORMATION_MESSAGE);
}//GEN-LAST:event_AboutActionPerformed

private void RunEmuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RunEmuActionPerformed
   RunEmu();
}//GEN-LAST:event_RunEmuActionPerformed

private void PauseEmuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PauseEmuActionPerformed
  PauseEmu();
}//GEN-LAST:event_PauseEmuActionPerformed

private void SetttingsMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SetttingsMenuActionPerformed

    if(setgui==null)
     {
      setgui = new SettingsGUI();
      Point mainwindow = this.getLocation();
      setgui.setLocation(mainwindow.x+100, mainwindow.y+50);
      setgui.setVisible(true);
      /* add a direct link to the main window*/
      setgui.setMainGUI(this);
     }
     else
     {
       setgui.RefreshWindow();
       setgui.setVisible(true);
     }
}//GEN-LAST:event_SetttingsMenuActionPerformed

private void ExitEmuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ExitEmuActionPerformed
    exitEmu();
}//GEN-LAST:event_ExitEmuActionPerformed

private void OpenMemStickActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_OpenMemStickActionPerformed
    PauseEmu();
    if(memstick==null)
    {
        memstick = new MemStickBrowser(this, new File("ms0/PSP/GAME"));
        Point mainwindow = this.getLocation();
        memstick.setLocation(mainwindow.x+100, mainwindow.y+50);
        memstick.setVisible(true);
    }
    else
    {
        memstick.refreshFiles();
        memstick.setVisible(true);
    }
}//GEN-LAST:event_OpenMemStickActionPerformed

private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
    exitEmu();
}//GEN-LAST:event_formWindowClosing

private void openUmdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openUmdActionPerformed
    PauseEmu();

    if (Settings.getInstance().readBool("emu.umdbrowser"))
    {
        umdbrowser = new UmdBrowser(this, new File(Settings.getInstance().readString("emu.umdpath") + "/"));
        umdbrowser.setVisible(true);
    }
    else
    {
        final JFileChooser fc = makeJFileChooser();
        fc.setDialogTitle(Resource.get("openumd"));
        int returnVal = fc.showOpenDialog(this);

        if (userChooseSomething(returnVal)) {
            File file = fc.getSelectedFile();
            loadUMD(file);
        } else {
            return; //user cancel the action
        }
    }
}//GEN-LAST:event_openUmdActionPerformed

/** Don't call this directly, see loadUMD(File file) */
private boolean loadUMD(UmdIsoReader iso, String bootPath) throws IOException {
    boolean success = false;

    try {
        UmdIsoFile bootBin = iso.getFile(bootPath);
        if (bootBin.length() != 0) {
            byte[] bootfile = new byte[(int)bootBin.length()];
            bootBin.read(bootfile);
            ByteBuffer buf = ByteBuffer.wrap(bootfile);
            emulator.load("disc0:/" + bootPath, buf);
            success = true;
        }
    } catch (FileNotFoundException e) {
        System.out.println(e.getMessage());
    } catch (GeneralJpcspException e) {
        //JpcspDialogManager.showError(this, "General Error : " + e.getMessage());
    }

    return success;
}

/** Don't call this directly, see loadUMD(File file) */
private boolean loadUnpackedUMD(String filename) throws IOException, GeneralJpcspException {
    // Load unpacked BOOT.BIN as if it came from the umd
    File file = new File(filename);
    if (file.exists()) {
        FileChannel roChannel = new RandomAccessFile(file, "r").getChannel();
        ByteBuffer readbuffer = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, (int)roChannel.size());
        emulator.load("disc0:/PSP_GAME/SYSDIR/EBOOT.BIN", readbuffer);
        roChannel.close();
        Emulator.log.info("Using unpacked UMD EBOOT.BIN image");
        return true;
    }
    return false;
}

public void loadUMD(File file) {
    try {
        if (consolewin != null)
            consolewin.clearScreenMessages();
        Emulator.log.info(MetaInformation.FULL_NAME);

        umdLoaded = true;
        loadedFile = file;

        UmdIsoReader iso = new UmdIsoReader(file.getPath());
        UmdIsoFile psfFile = iso.getFile("PSP_GAME/param.sfo");

        //Emulator.log.debug("Loading param.sfo from UMD");
        PSF psf = new PSF();
        byte[] data = new byte[(int)psfFile.length()];
        psfFile.read(data);
        psf.read(ByteBuffer.wrap(data));

        Emulator.log.info("UMD param.sfo :\n" + psf);
        String title = psf.getPrintableString("TITLE");
        String discId = psf.getString("DISC_ID");
        if (discId == null) {
            discId = State.DISCID_UNKNOWN_UMD;
        }

        setTitle(MetaInformation.FULL_NAME + " - " + title);
        addRecentUMD(file, title);

        emulator.setFirmwareVersion(psf.getString("PSP_SYSTEM_VER"));
        RuntimeContext.setIsHomebrew(psf.isLikelyHomebrew());

        if ((!discId.equals(State.DISCID_UNKNOWN_UMD) && loadUnpackedUMD(discId + ".BIN")) ||
            loadUMD(iso, "PSP_GAME/SYSDIR/BOOT.BIN") ||
            loadUMD(iso, "PSP_GAME/SYSDIR/EBOOT.BIN")) {

            State.discId = discId;
            State.title = title;

            Modules.IoFileMgrForUserModule.setfilepath("disc0/");
            //Modules.IoFileMgrForUserModule.setfilepath("disc0/PSP_GAME/SYSDIR");

            Modules.IoFileMgrForUserModule.setIsoReader(iso);
            jpcsp.HLE.Modules.sceUmdUserModule.setIsoReader(iso);

            // use regular settings first
            installCompatibilitySettings();

            // override with patch file (allows incomplete patch files)
            installCompatibilityPatches(discId + ".patch");

            if (instructioncounter != null)
                instructioncounter.RefreshWindow();
            StepLogger.clear();
            StepLogger.setName(file.getPath());
        } else {
            throw new GeneralJpcspException(Resource.get("encryptedBoot"));
        }
    } catch (GeneralJpcspException e) {
        JpcspDialogManager.showError(this, Resource.get("generalError")+" : " + e.getMessage());
    } catch (IOException e) {
        e.printStackTrace();
        JpcspDialogManager.showError(this, Resource.get("ioError")+" : " + e.getMessage());
    } catch (Exception ex) {
        ex.printStackTrace();
        if (ex.getMessage() != null) {
            JpcspDialogManager.showError(this, Resource.get("criticalError")+" : " + ex.getMessage());
        } else {
            JpcspDialogManager.showError(this, Resource.get("criticalError")+" : Check console for details.");
        }
    }
}

private void installCompatibilitySettings()
{
    Emulator.log.info("Loading global compatibility settings");

    boolean onlyGEGraphics = Settings.getInstance().readBool("emu.onlyGEGraphics");
    Modules.sceDisplayModule.setOnlyGEGraphics(onlyGEGraphics);

    boolean useViewport = Settings.getInstance().readBool("emu.useViewport");
    VideoEngine.getInstance().setUseViewport(useViewport);

    boolean useConnector = Settings.getInstance().readBool("emu.useConnector");
    sceMpeg.setEnableConnector(useConnector);
    sceAtrac3plus.setEnableConnector(useConnector);

    boolean useMediaEngine = Settings.getInstance().readBool("emu.useMediaEngine");
    sceMpeg.setEnableMediaEngine(useMediaEngine);

    boolean useVertexCache = Settings.getInstance().readBool("emu.useVertexCache");
    VideoEngine.getInstance().setUseVertexCache(useVertexCache);

    boolean disableAudio = Settings.getInstance().readBool("emu.disablesceAudio");
    Modules.sceAudioModule.setChReserveEnabled(!disableAudio);

    boolean audioMuted = Settings.getInstance().readBool("emu.mutesound");
    Modules.sceAudioModule.setAudioMuted(audioMuted);
    Modules.sceSasCoreModule.setAudioMuted(audioMuted);

    boolean disableBlocking = Settings.getInstance().readBool("emu.disableblockingaudio");
    Modules.sceAudioModule.setBlockingEnabled(!disableBlocking);

    boolean ignoreAudioThreads = Settings.getInstance().readBool("emu.ignoreaudiothreads");
    Modules.ThreadManForUserModule.setThreadBanningEnabled(ignoreAudioThreads);

    boolean ignoreInvalidMemoryAccess = Settings.getInstance().readBool("emu.ignoreInvalidMemoryAccess");
    Memory.getInstance().setIgnoreInvalidMemoryAccess(ignoreInvalidMemoryAccess);
    Compiler.setIgnoreInvalidMemory(ignoreInvalidMemoryAccess);

    boolean enableWaitThreadEndCB = Settings.getInstance().readBool("emu.enablewaitthreadendcb");
    Modules.ThreadManForUserModule.setEnableWaitThreadEndCB(enableWaitThreadEndCB);

    boolean ignoreUnmappedImports = Settings.getInstance().readBool("emu.ignoreUnmappedImports");
    SyscallHandler.setEnableIgnoreUnmappedImports(ignoreUnmappedImports);
}

/** @return true if a patch file was found */
public boolean installCompatibilityPatches(String filename)
{
    File patchfile = new File("patches/" + filename);
    if (!patchfile.exists())
    {
        Emulator.log.debug("No patch file found for this game");
        return false;
    }

    Properties patchSettings= new Properties();
    InputStream patchInputStream = null;
    try {
        Emulator.log.info("Overriding previous settings with patch file");
        patchInputStream = new BufferedInputStream(new FileInputStream(patchfile));
        patchSettings.load(patchInputStream);

        String onlyGEGraphics = patchSettings.getProperty("emu.onlyGEGraphics");
        if (onlyGEGraphics != null)
            Modules.sceDisplayModule.setOnlyGEGraphics(Integer.parseInt(onlyGEGraphics) != 0);

        String useViewport = patchSettings.getProperty("emu.useViewport");
        if (useViewport != null)
            VideoEngine.getInstance().setUseViewport(Integer.parseInt(useViewport) != 0);

        String useConnector = patchSettings.getProperty("emu.useConnector");
        if (useConnector != null) {
            sceMpeg.setEnableConnector(Integer.parseInt(useConnector) != 0);
            sceAtrac3plus.setEnableConnector(Integer.parseInt(useConnector) != 0);
        }

        String useMediaEngine = patchSettings.getProperty("emu.useMediaEngine");
        if (useMediaEngine != null)
            sceMpeg.setEnableMediaEngine(Integer.parseInt(useMediaEngine) != 0);

        String useVertexCache = patchSettings.getProperty("emu.useVertexCache");
        if (useVertexCache != null)
            VideoEngine.getInstance().setUseVertexCache(Integer.parseInt(useVertexCache) != 0);

        String disableAudio = patchSettings.getProperty("emu.disablesceAudio");
        if (disableAudio != null)
            jpcsp.HLE.Modules.sceAudioModule.setChReserveEnabled(!(Integer.parseInt(disableAudio) != 0));

        String disableBlocking = patchSettings.getProperty("emu.disableblockingaudio");
        if (disableBlocking != null)
            jpcsp.HLE.Modules.sceAudioModule.setBlockingEnabled(!(Integer.parseInt(disableBlocking) != 0));

        String ignoreAudioThreads = patchSettings.getProperty("emu.ignoreaudiothreads");
        if (ignoreAudioThreads != null)
        	Modules.ThreadManForUserModule.setThreadBanningEnabled(Integer.parseInt(ignoreAudioThreads) != 0);

        String ignoreInvalidMemoryAccess = patchSettings.getProperty("emu.ignoreInvalidMemoryAccess");
        if (ignoreInvalidMemoryAccess != null) {
            Memory.getInstance().setIgnoreInvalidMemoryAccess(Integer.parseInt(ignoreInvalidMemoryAccess) != 0);
            Compiler.setIgnoreInvalidMemory(Integer.parseInt(ignoreInvalidMemoryAccess) != 0);
        }

        String enableWaitThreadEndCB = patchSettings.getProperty("emu.enablewaitthreadendcb");
        if (enableWaitThreadEndCB != null)
        	Modules.ThreadManForUserModule.setEnableWaitThreadEndCB(Integer.parseInt(enableWaitThreadEndCB) != 0);

        String ignoreUnmappedImports = patchSettings.getProperty("emu.ignoreUnmappedImports");
        if (ignoreUnmappedImports != null)
        	SyscallHandler.setEnableIgnoreUnmappedImports(Integer.parseInt(ignoreUnmappedImports) != 0);

    } catch (IOException e) {
        e.printStackTrace();
    } finally{
        Utilities.close(patchInputStream);
    }

    return true;
}

private void ResetEmuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ResetEmuActionPerformed
    resetEmu();
}//GEN-LAST:event_ResetEmuActionPerformed

private void ResetButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ResetButtonActionPerformed
    resetEmu();
}//GEN-LAST:event_ResetButtonActionPerformed

private void resetEmu() {
    if(loadedFile != null) {
        PauseEmu();
        if(umdLoaded)
            loadUMD(loadedFile);
        else
            loadFile(loadedFile);
    }
}

private void InstructionCounterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_InstructionCounterActionPerformed
    PauseEmu();
    if (instructioncounter==null)
    {
        instructioncounter = new InstructionCounter();
        emulator.setInstructionCounter(instructioncounter);
        Point mainwindow = this.getLocation();
        instructioncounter.setLocation(mainwindow.x+100, mainwindow.y+50);
        instructioncounter.setVisible(true);
    }
    else
    {
        instructioncounter.RefreshWindow();
        instructioncounter.setVisible(true);
    }
}//GEN-LAST:event_InstructionCounterActionPerformed

private void FileLogActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FileLogActionPerformed
    PauseEmu();
    State.fileLogger.setVisible(true);
}//GEN-LAST:event_FileLogActionPerformed

private void VfpuRegistersActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_VfpuRegistersActionPerformed
    VfpuFrame.getInstance().setVisible(true);
}//GEN-LAST:event_VfpuRegistersActionPerformed

private void DumpIsoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DumpIsoActionPerformed
    if (umdLoaded) {
        UmdIsoReader iso = Modules.IoFileMgrForUserModule.getIsoReader();
        if (iso != null) {
            try {
                iso.dumpIndexFile("iso-index.txt");
            } catch (IOException e) {
                // Ignore Exception
            }
        }
    }
}//GEN-LAST:event_DumpIsoActionPerformed

private void ResetProfilerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ResetProfilerActionPerformed
	Profiler.reset();
}//GEN-LAST:event_ResetProfilerActionPerformed

private void MenuBarMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_MenuBarMouseExited
Modules.sceDisplayModule.repaint();
}//GEN-LAST:event_MenuBarMouseExited

private void ShotItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ShotItemActionPerformed
    Modules.sceDisplayModule.getscreen = true;
}//GEN-LAST:event_ShotItemActionPerformed

private void RotateItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RotateItemActionPerformed
sceDisplay screen = Modules.sceDisplayModule;
Object[] options = {"90 CW","90 CCW","180","Mirror","Normal"};

int jop = JOptionPane.showOptionDialog(null, Resource.get("chooseRotation"), "Rotate", JOptionPane.UNDEFINED_CONDITION, JOptionPane.QUESTION_MESSAGE, null, options, options[4]);

if(jop != -1)
    screen.rotate(jop);
else
    return;
}//GEN-LAST:event_RotateItemActionPerformed
private byte safeRead8(int address)
{
    byte value = 0;
    if (Memory.getInstance().isAddressGood(address))
        value = (byte)Memory.getInstance().read8(address);
    return value;
}

private void safeWrite8(byte value, int address)
{
    if (Memory.getInstance().isAddressGood(address))
        Memory.getInstance().write8(address, value);
}
private void SaveSnapActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SaveSnapActionPerformed
File f = new File("Snap_" + State.discId + ".bin");
BufferedOutputStream bOut = null;
ByteBuffer cpuBuf = ByteBuffer.allocate(1024);

Emulator.getProcessor().save(cpuBuf);

try
{
    bOut = new BufferedOutputStream( new FileOutputStream(f) );
    for(int i = 0x08000000; i<=0x09ffffff; i++)
    {
        bOut.write(safeRead8(i));
    }

    bOut.write(cpuBuf.array());
}
catch(IOException e)
{

}
finally
{
    Utilities.close(bOut);
}
}//GEN-LAST:event_SaveSnapActionPerformed

private void LoadSnapActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LoadSnapActionPerformed
File f = new File("Snap_" + State.discId + ".bin");
BufferedInputStream bIn = null;
ByteBuffer cpuBuf = ByteBuffer.allocate(1024);

try
{
    bIn = new BufferedInputStream(new FileInputStream(f));
    for(int i = 0x08000000; i<=0x09ffffff; i++ )
    {
        safeWrite8((byte)bIn.read(), i);
    }

    bIn.read(cpuBuf.array());
}

catch(IOException e)
{

}

finally
{
    Utilities.close(bIn);
}

Emulator.getProcessor().load(cpuBuf);
}//GEN-LAST:event_LoadSnapActionPerformed

private void EnglishActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EnglishActionPerformed
changeLanguage("en_EN");
}//GEN-LAST:event_EnglishActionPerformed

private void FrenchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FrenchActionPerformed
changeLanguage("fr_FR");
}//GEN-LAST:event_FrenchActionPerformed

private void GermanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GermanActionPerformed
changeLanguage("de_DE");
}//GEN-LAST:event_GermanActionPerformed

private void LithuanianActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LithuanianActionPerformed
changeLanguage("lt_LT");
}//GEN-LAST:event_LithuanianActionPerformed

private void SpanishActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SpanishActionPerformed
changeLanguage("es_ES");
}//GEN-LAST:event_SpanishActionPerformed

private void CatalanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CatalanActionPerformed
changeLanguage("es_CA");
}//GEN-LAST:event_CatalanActionPerformed

private void PortugueseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PortugueseActionPerformed
changeLanguage("pt_PT");
}//GEN-LAST:event_PortugueseActionPerformed

private void JapaneseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_JapaneseActionPerformed
changeLanguage("jp_JP");
}//GEN-LAST:event_JapaneseActionPerformed

private void PortugueseBRActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PortugueseBRActionPerformed
changeLanguage("pt_BR");
}//GEN-LAST:event_PortugueseBRActionPerformed

private void cwcheatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cwcheatActionPerformed
CheatsGUI cwCheats = new CheatsGUI("CWCheat");
cwCheats.setVisible(true);
}//GEN-LAST:event_cwcheatActionPerformed

private void RussianActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RussianActionPerformed
changeLanguage("ru_RU");
}//GEN-LAST:event_RussianActionPerformed

private void PolishActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PolishActionPerformed
changeLanguage("pl_PL");
}//GEN-LAST:event_PolishActionPerformed

private void exitEmu() {
    if (Settings.getInstance().readBool("gui.saveWindowPos"))
        Settings.getInstance().writeWindowPos("mainwindow", getLocation());

    Modules.ThreadManForUserModule.exit();
    Modules.sceDisplayModule.exit();
    VideoEngine.exit();
    Emulator.exit();

    System.exit(0);
}

public void snaptoMainwindow() {
    snapConsole = true;
    mainwindowPos = getLocation();
    consolewin.setLocation(mainwindowPos.x, mainwindowPos.y + getHeight());
}

private void RunEmu()
{
    emulator.RunEmu();
}

private void TogglePauseEmu()
{
    // This is a toggle, so can pause and unpause
    if (Emulator.run)
    {
        if (!Emulator.pause)
            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_PAUSE);
        else
            RunEmu();
    }
}

private void PauseEmu()
{
    // This will only enter pause mode
    if (Emulator.run && !Emulator.pause) {
        Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_PAUSE);
    }
}

public void RefreshButtons()
{
    RunButton.setSelected(Emulator.run && !Emulator.pause);
    PauseButton.setSelected(Emulator.run && Emulator.pause);
}

/** set the FPS portion of the title */
public void setMainTitle(String message)
{
    String oldtitle = getTitle();
    int sub = oldtitle.indexOf("FPS:");
    if(sub!=-1)
    {
        String newtitle= oldtitle.substring(0, sub-1);
        setTitle(newtitle + " " + message);
    }
    else
    {
        setTitle(oldtitle + " " + message);
    }
}

private void printUsage() {
    System.err.println("Usage: java -Xmx512m -jar jpcsp.jar [OPTIONS]");
    System.err.println();
    System.err.println("  -d, --debugger             Open debugger at start.");
    System.err.println("  -f, --loadfile FILE        Load a file.");
    System.err.println("                               Example: ms0/PSP/GAME/pspsolitaire/EBOOT.PBP");
    System.err.println("  -u, --loadumd FILE         Load a UMD. Example: umdimages/cube.iso");
    System.err.println("  -r, --run                  Run loaded file or umd. Use with -f or -u option.");
}

private void processArgs(String[] args) {
    int i = 0;
    while(i < args.length) {
        if (args[i].equals("-d") || args[i].equals("--debugger")) {
            i++;
            // hack: reuse this function
            EnterDebuggerActionPerformed(null);
        } else if (args[i].equals("-f") || args[i].equals("--loadfile")) {
            i++;
            if (i < args.length) {
                File file = new File(args[i]);
                if(file.exists())
                    loadFile(file);
                i++;
            } else {
                printUsage();
                break;
            }
        } else if (args[i].equals("-u") || args[i].equals("--loadumd")) {
            i++;
            if (i < args.length) {
                File file = new File(args[i]);
                if(file.exists())
                    loadUMD(file);
                i++;
            } else {
                printUsage();
                break;
            }
        } else if (args[i].equals("-r") || args[i].equals("--run")) {
            i++;
            RunEmu();
        } else {
            printUsage();
            break;
        }
    }
}

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // final copy of args for use in inner class
        final String[] fargs = args;

        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setName("GUI");
                MainGUI maingui = new MainGUI();
                maingui.setVisible(true);

                if (Settings.getInstance().readBool("gui.openLogwindow"))
                    maingui.consolewin.setVisible(true);

                maingui.processArgs(fargs);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem About;
    private javax.swing.JMenuItem Catalan;
    private javax.swing.JMenu CheatsMenu;
    private javax.swing.JMenu DebugMenu;
    private javax.swing.JMenuItem DumpIso;
    private javax.swing.JMenuItem ElfHeaderViewer;
    private javax.swing.JMenu EmulationMenu;
    private javax.swing.JMenuItem English;
    private javax.swing.JMenuItem EnterDebugger;
    private javax.swing.JMenuItem EnterMemoryViewer;
    private javax.swing.JMenuItem ExitEmu;
    private javax.swing.JMenuItem FileLog;
    private javax.swing.JMenu FileMenu;
    private javax.swing.JMenuItem French;
    private javax.swing.JMenuItem German;
    private javax.swing.JMenu HelpMenu;
    private javax.swing.JMenuItem InstructionCounter;
    private javax.swing.JMenuItem Japanese;
    private javax.swing.JMenu LanguageMenu;
    private javax.swing.JMenuItem Lithuanian;
    private javax.swing.JMenuItem LoadSnap;
    private javax.swing.JMenuBar MenuBar;
    private javax.swing.JMenuItem OpenFile;
    private javax.swing.JMenuItem OpenMemStick;
    private javax.swing.JMenu OptionsMenu;
    private javax.swing.JToggleButton PauseButton;
    private javax.swing.JMenuItem PauseEmu;
    private javax.swing.JMenuItem Polish;
    private javax.swing.JMenuItem Portuguese;
    private javax.swing.JMenuItem PortugueseBR;
    private javax.swing.JMenu RecentMenu;
    private javax.swing.JButton ResetButton;
    private javax.swing.JMenuItem ResetEmu;
    private javax.swing.JMenuItem ResetProfiler;
    private javax.swing.JMenuItem RotateItem;
    private javax.swing.JToggleButton RunButton;
    private javax.swing.JMenuItem RunEmu;
    private javax.swing.JMenuItem Russian;
    private javax.swing.JMenuItem SaveSnap;
    private javax.swing.JMenuItem SetttingsMenu;
    private javax.swing.JMenuItem ShotItem;
    private javax.swing.JMenuItem Spanish;
    private javax.swing.JMenuItem ToggleConsole;
    private javax.swing.JMenu ToolsSubMenu;
    private javax.swing.JMenuItem VfpuRegisters;
    private javax.swing.JMenuItem cwcheat;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JMenuItem openUmd;
    // End of variables declaration//GEN-END:variables

    private boolean userChooseSomething(int returnVal) {
        return returnVal == JFileChooser.APPROVE_OPTION;
    }

    @Override
    public void keyTyped(KeyEvent arg0) { }

    @Override
    public void keyPressed(KeyEvent arg0) {
        State.controller.keyPressed(arg0);
    }

    @Override
    public void keyReleased(KeyEvent arg0) {
        State.controller.keyReleased(arg0);
    }

    @Override
    public void componentHidden(ComponentEvent e) { }

    @Override
    public void componentMoved(ComponentEvent e) {
        if (snapConsole && consolewin.isVisible()) {
            Point newPos = this.getLocation();
            Point consolePos = consolewin.getLocation();
            Dimension mainwindowSize = this.getSize();

            if (consolePos.x == mainwindowPos.x &&
                consolePos.y == mainwindowPos.y + mainwindowSize.height) {
                consolewin.setLocation(newPos.x, newPos.y + mainwindowSize.height);
            } else {
                snapConsole = false;
            }

            mainwindowPos = newPos;
        }
    }

    @Override
    public void componentResized(ComponentEvent e) { }

    @Override
    public void componentShown(ComponentEvent e) { }

    private class RecentElementActionListener implements ActionListener {
    	public static final int TYPE_UMD = 0;
    	public static final int TYPE_FILE = 1;
    	int type;
    	String path;

    	public RecentElementActionListener(int type, String path) {
    		this.path = path;
    		this.type = type;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			File file = new File(path);
            if(file.exists()) {
            	if(type == TYPE_UMD)
            		loadUMD(file);
            	else
            		loadFile(file);
            }
		}
    }
}
