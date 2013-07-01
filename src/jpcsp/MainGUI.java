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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.EventQueue;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import jpcsp.Allegrex.compiler.Profiler;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.autotests.AutoTestsRunner;
import jpcsp.Debugger.ElfHeaderInfo;
import jpcsp.Debugger.ImageViewer;
import jpcsp.Debugger.InstructionCounter;
import jpcsp.Debugger.MemoryViewer;
import jpcsp.Debugger.StepLogger;
import jpcsp.Debugger.DisassemblerModule.DisassemblerFrame;
import jpcsp.Debugger.DisassemblerModule.VfpuFrame;
import jpcsp.GUI.CheatsGUI;
import jpcsp.GUI.IMainGUI;
import jpcsp.GUI.MemStickBrowser;
import jpcsp.GUI.RecentElement;
import jpcsp.GUI.SettingsGUI;
import jpcsp.GUI.ControlsGUI;
import jpcsp.GUI.LogGUI;
import jpcsp.GUI.UmdBrowser;
import jpcsp.GUI.UmdVideoPlayer;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.sceDisplay;
import jpcsp.HLE.modules.sceUtility;
import jpcsp.filesystems.umdiso.UmdIsoFile;
import jpcsp.filesystems.umdiso.UmdIsoReader;
import jpcsp.format.PSF;
import jpcsp.graphics.GEProfiler;
import jpcsp.graphics.VideoEngine;
import jpcsp.hardware.Audio;
import jpcsp.hardware.Screen;
import jpcsp.log.LogWindow;
import jpcsp.log.LoggingOutputStream;
import jpcsp.network.proonline.ProOnlineNetworkAdapter;
import jpcsp.settings.Settings;
import jpcsp.util.JpcspDialogManager;
import jpcsp.util.MetaInformation;
import jpcsp.util.Utilities;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.jidesoft.plaf.LookAndFeelFactory;
import java.util.ArrayList;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.swing.SwingUtilities;

/**
 *
 * @author shadow
 */
public class MainGUI extends javax.swing.JFrame implements KeyListener, ComponentListener, MouseListener, IMainGUI {

    private static final long serialVersionUID = -3647025845406693230L;
    public static final int MAX_RECENT = 10;
    LogWindow consolewin;
    ElfHeaderInfo elfheader;
    SettingsGUI setgui;
    ControlsGUI ctrlgui;
    LogGUI loggui;
    MemStickBrowser memstick;
    Emulator emulator;
    UmdBrowser umdbrowser;
    UmdVideoPlayer umdvideoplayer;
    InstructionCounter instructioncounter;
    File loadedFile;
    boolean umdLoaded;
    boolean useFullscreen;
    JPopupMenu fullScreenMenu;
    private List<RecentElement> recentUMD = new LinkedList<RecentElement>();
    private List<RecentElement> recentFile = new LinkedList<RecentElement>();
    public final static String windowNameForSettings = "mainwindow";
    private final static String[] userDir = {
        "ms0/PSP/SAVEDATA",
        "ms0/PSP/GAME",
        "tmp"
    };
    private static final String logConfigurationSettingLeft = "    %1$-40s [%2$s]";
    private static final String logConfigurationSettingRight = "    [%2$s] %1$s";
    private static final String logConfigurationSettingLeftPatch = "    %1$-40s [%2$s] (%3$s)";
    private static final String logConfigurationSettingRightPatch = "    [%2$s] %1$s (%3$s)";
    public static final int displayModeBitDepth = 32;
    public static final int preferredDisplayModeRefreshRate = 60; // Preferred refresh rate if 60Hz
    private DisplayMode displayMode;
    private SetLocationThread setLocationThread;
    private JComponent fillerLeft;
    private JComponent fillerRight;
    private JComponent fillerTop;
    private JComponent fillerBottom;
    private static boolean jideInitialized;

    @Override
    public DisplayMode getDisplayMode() {
        return displayMode;
    }

    /**
     * Creates new form MainGUI
     */
    public MainGUI() {
        DOMConfigurator.configure("LogSettings.xml");
        System.setOut(new PrintStream(new LoggingOutputStream(Logger.getLogger("emu"), Level.INFO)));
        consolewin = new LogWindow();

        // Create needed user directories
        for (String dirName : userDir) {
            File dir = new File(dirName);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }

        emulator = new Emulator(this);
        Screen.start();

        /*next two lines are for overlay menus over joglcanvas*/
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
        //end of

        useFullscreen = Settings.getInstance().readBool("gui.fullscreen");
        if (useFullscreen && !isDisplayable()) {
            setUndecorated(true);
            setLocation(0, 0);
            setSize(getFullScreenDimension());
            setPreferredSize(getFullScreenDimension());
        } else {
            setLocation(Settings.getInstance().readWindowPos(windowNameForSettings));
        }

        String resolution = Settings.getInstance().readString("emu.graphics.resolution");
        if (resolution != null && resolution.contains("x")) {
            int width = Integer.parseInt(resolution.split("x")[0]);
            int heigth = Integer.parseInt(resolution.split("x")[1]);
            changeScreenResolution(width, heigth);
        }

        initJide();
        createComponents();

        onUmdChange();

        State.fileLogger.setLocation(getLocation().x + 488, getLocation().y + 18);
        setTitle(MetaInformation.FULL_NAME);

        /*add glcanvas to frame and pack frame to get the canvas size*/
        getContentPane().add(Modules.sceDisplayModule.getCanvas(), java.awt.BorderLayout.CENTER);
        Modules.sceDisplayModule.getCanvas().addKeyListener(this);
        Modules.sceDisplayModule.getCanvas().addMouseListener(this);
        addComponentListener(this);
        pack();

        Insets insets = getInsets();
        Dimension minSize = new Dimension(
                Screen.width + insets.left + insets.right,
                Screen.height + insets.top + insets.bottom);
        setMinimumSize(minSize);

        //logging console window stuff
        if (Settings.getInstance().readBool("gui.snapLogwindow")) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    // if this update is not done later an initial gap is found
                    // between the main window and the logger
                    updateConsoleWinPosition();
                }
            });
        } else {
            consolewin.setLocation(Settings.getInstance().readWindowPos("logwindow"));
        }
    }

    private void initJide() {
        if (!jideInitialized) {
            LookAndFeelFactory.installJideExtension(LookAndFeelFactory.VSNET_STYLE_WITHOUT_MENU);
            jideInitialized = true;
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        filtersGroup = new javax.swing.ButtonGroup();
        resGroup = new javax.swing.ButtonGroup();
        frameSkipGroup = new javax.swing.ButtonGroup();
        clockSpeedGroup = new javax.swing.ButtonGroup();
        mainToolBar = new javax.swing.JToolBar();
        RunButton = new javax.swing.JToggleButton();
        PauseButton = new javax.swing.JToggleButton();
        ResetButton = new javax.swing.JButton();
        MenuBar = new javax.swing.JMenuBar();
        FileMenu = new javax.swing.JMenu();
        openUmd = new javax.swing.JMenuItem();
        OpenFile = new javax.swing.JMenuItem();
        OpenMemStick = new javax.swing.JMenuItem();
        RecentMenu = new javax.swing.JMenu();
        switchUmd = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        SaveSnap = new javax.swing.JMenuItem();
        LoadSnap = new javax.swing.JMenuItem();
        ExportMenu = new javax.swing.JMenu();
        ExportVisibleElements = new javax.swing.JMenuItem();
        ExportAllElements = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        ExitEmu = new javax.swing.JMenuItem();
        OptionsMenu = new javax.swing.JMenu();
        VideoOpt = new javax.swing.JMenu();
        ResizeMenu = new javax.swing.JMenu();
        oneTimeResize = new javax.swing.JCheckBoxMenuItem();
        twoTimesResize = new javax.swing.JCheckBoxMenuItem();
        threeTimesResize = new javax.swing.JCheckBoxMenuItem();
        FiltersMenu = new javax.swing.JMenu();
        noneCheck = new javax.swing.JCheckBoxMenuItem();
        anisotropicCheck = new javax.swing.JCheckBoxMenuItem();
        FrameSkipMenu = new javax.swing.JMenu();
        FrameSkipNone = new javax.swing.JCheckBoxMenuItem();
        FPS5 = new javax.swing.JCheckBoxMenuItem();
        FPS10 = new javax.swing.JCheckBoxMenuItem();
        FPS15 = new javax.swing.JCheckBoxMenuItem();
        FPS20 = new javax.swing.JCheckBoxMenuItem();
        FPS30 = new javax.swing.JCheckBoxMenuItem();
        FPS60 = new javax.swing.JCheckBoxMenuItem();
        ShotItem = new javax.swing.JMenuItem();
        RotateItem = new javax.swing.JMenuItem();
        AudioOpt = new javax.swing.JMenu();
        MuteOpt = new javax.swing.JCheckBoxMenuItem();
        ClockSpeedOpt = new javax.swing.JMenu();
        ClockSpeed50 = new javax.swing.JCheckBoxMenuItem();
        ClockSpeed75 = new javax.swing.JCheckBoxMenuItem();
        ClockSpeedNormal = new javax.swing.JCheckBoxMenuItem();
        ClockSpeed150 = new javax.swing.JCheckBoxMenuItem();
        ClockSpeed200 = new javax.swing.JCheckBoxMenuItem();
        ClockSpeed300 = new javax.swing.JCheckBoxMenuItem();
        ControlsConf = new javax.swing.JMenuItem();
        ConfigMenu = new javax.swing.JMenuItem();
        DebugMenu = new javax.swing.JMenu();
        ToolsSubMenu = new javax.swing.JMenu();
        LoggerMenu = new javax.swing.JMenu();
        ToggleLogger = new javax.swing.JCheckBoxMenuItem();
        CustomLogger = new javax.swing.JMenuItem();
        EnterDebugger = new javax.swing.JMenuItem();
        EnterMemoryViewer = new javax.swing.JMenuItem();
        EnterImageViewer = new javax.swing.JMenuItem();
        VfpuRegisters = new javax.swing.JMenuItem();
        ElfHeaderViewer = new javax.swing.JMenuItem();
        FileLog = new javax.swing.JMenuItem();
        InstructionCounter = new javax.swing.JMenuItem();
        DumpIso = new javax.swing.JMenuItem();
        ResetProfiler = new javax.swing.JMenuItem();
        ClearTextureCache = new javax.swing.JMenuItem();
        ClearVertexCache = new javax.swing.JMenuItem();
        CheatsMenu = new javax.swing.JMenu();
        cwcheat = new javax.swing.JMenuItem();
        LanguageMenu = new javax.swing.JMenu();
        SystemLocale = new javax.swing.JMenuItem();
        English = new javax.swing.JMenuItem();
        French = new javax.swing.JMenuItem();
        German = new javax.swing.JMenuItem();
        Lithuanian = new javax.swing.JMenuItem();
        Spanish = new javax.swing.JMenuItem();
        Catalan = new javax.swing.JMenuItem();
        Portuguese = new javax.swing.JMenuItem();
        PortugueseBR = new javax.swing.JMenuItem();
        Japanese = new javax.swing.JMenuItem();
        Russian = new javax.swing.JMenuItem();
        Polish = new javax.swing.JMenuItem();
        ChinesePRC = new javax.swing.JMenuItem();
        ChineseTW = new javax.swing.JMenuItem();
        Italian = new javax.swing.JMenuItem();
        Greek = new javax.swing.JMenuItem();
        HelpMenu = new javax.swing.JMenu();
        About = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setForeground(java.awt.Color.white);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        mainToolBar.setFloatable(false);
        mainToolBar.setRollover(true);

        RunButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/PlayIcon.png"))); // NOI18N
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("jpcsp/languages/jpcsp"); // NOI18N
        RunButton.setText(bundle.getString("MainGUI.RunButton.text")); // NOI18N
        RunButton.setFocusable(false);
        RunButton.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        RunButton.setIconTextGap(2);
        RunButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        RunButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RunButtonActionPerformed(evt);
            }
        });
        mainToolBar.add(RunButton);

        PauseButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/PauseIcon.png"))); // NOI18N
        PauseButton.setText(bundle.getString("MainGUI.PauseButton.text")); // NOI18N
        PauseButton.setFocusable(false);
        PauseButton.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        PauseButton.setIconTextGap(2);
        PauseButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        PauseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PauseButtonActionPerformed(evt);
            }
        });
        mainToolBar.add(PauseButton);

        ResetButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/StopIcon.png"))); // NOI18N
        ResetButton.setText(bundle.getString("MainGUI.ResetButton.text")); // NOI18N
        ResetButton.setFocusable(false);
        ResetButton.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        ResetButton.setIconTextGap(2);
        ResetButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        ResetButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ResetButtonActionPerformed(evt);
            }
        });
        mainToolBar.add(ResetButton);

        getContentPane().add(mainToolBar, java.awt.BorderLayout.NORTH);

        FileMenu.setText(bundle.getString("MainGUI.FileMenu.text")); // NOI18N

        openUmd.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        openUmd.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/LoadUmdIcon.png"))); // NOI18N
        openUmd.setText(bundle.getString("MainGUI.openUmd.text")); // NOI18N
        openUmd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openUmdActionPerformed(evt);
            }
        });
        FileMenu.add(openUmd);

        OpenFile.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.ALT_MASK));
        OpenFile.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/LoadFileIcon.png"))); // NOI18N
        OpenFile.setText(bundle.getString("MainGUI.OpenFile.text")); // NOI18N
        OpenFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                OpenFileActionPerformed(evt);
            }
        });
        FileMenu.add(OpenFile);

        OpenMemStick.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.SHIFT_MASK));
        OpenMemStick.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/LoadMemoryStick.png"))); // NOI18N
        OpenMemStick.setText(bundle.getString("MainGUI.OpenMemStick.text")); // NOI18N
        OpenMemStick.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                OpenMemStickActionPerformed(evt);
            }
        });
        FileMenu.add(OpenMemStick);

        RecentMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/RecentIcon.png"))); // NOI18N
        RecentMenu.setText(bundle.getString("MainGUI.RecentMenu.text")); // NOI18N
        FileMenu.add(RecentMenu);

        switchUmd.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/LoadUmdIcon.png"))); // NOI18N
        switchUmd.setText(bundle.getString("MainGUI.switchUmd.text")); // NOI18N
        switchUmd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                switchUmdActionPerformed(evt);
            }
        });
        FileMenu.add(switchUmd);
        FileMenu.add(jSeparator2);

        SaveSnap.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.SHIFT_MASK));
        SaveSnap.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/SaveStateIcon.png"))); // NOI18N
        SaveSnap.setText(bundle.getString("MainGUI.SaveSnap.text")); // NOI18N
        SaveSnap.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SaveSnapActionPerformed(evt);
            }
        });
        FileMenu.add(SaveSnap);

        LoadSnap.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.SHIFT_MASK));
        LoadSnap.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/LoadStateIcon.png"))); // NOI18N
        LoadSnap.setText(bundle.getString("MainGUI.LoadSnap.text")); // NOI18N
        LoadSnap.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LoadSnapActionPerformed(evt);
            }
        });
        FileMenu.add(LoadSnap);

        ExportMenu.setText(bundle.getString("MainGUI.ExportMenu.text")); // NOI18N

        ExportVisibleElements.setText(bundle.getString("MainGUI.ExportVisibleElements.text")); // NOI18N
        ExportVisibleElements.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ExportVisibleElementsActionPerformed(evt);
            }
        });
        ExportMenu.add(ExportVisibleElements);

        ExportAllElements.setText(bundle.getString("MainGUI.ExportAllElements.text")); // NOI18N
        ExportAllElements.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ExportAllElementsActionPerformed(evt);
            }
        });
        ExportMenu.add(ExportAllElements);

        FileMenu.add(ExportMenu);
        FileMenu.add(jSeparator1);

        ExitEmu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E, java.awt.event.InputEvent.CTRL_MASK));
        ExitEmu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/CloseIcon.png"))); // NOI18N
        ExitEmu.setText(bundle.getString("MainGUI.ExitEmu.text")); // NOI18N
        ExitEmu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ExitEmuActionPerformed(evt);
            }
        });
        FileMenu.add(ExitEmu);

        MenuBar.add(FileMenu);

        OptionsMenu.setText(bundle.getString("MainGUI.OptionsMenu.text")); // NOI18N

        VideoOpt.setText(bundle.getString("MainGUI.VideoOpt.text")); // NOI18N

        ResizeMenu.setText(bundle.getString("MainGUI.ResizeMenu.text")); // NOI18N

        resGroup.add(oneTimeResize);
        oneTimeResize.setSelected(true);
        oneTimeResize.setText("1x"); // NOI18N
        oneTimeResize.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                oneTimeResizeActionPerformed(evt);
            }
        });
        ResizeMenu.add(oneTimeResize);

        resGroup.add(twoTimesResize);
        twoTimesResize.setText("2x"); // NOI18N
        twoTimesResize.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                twoTimesResizeActionPerformed(evt);
            }
        });
        ResizeMenu.add(twoTimesResize);

        resGroup.add(threeTimesResize);
        threeTimesResize.setText("3x"); // NOI18N
        threeTimesResize.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                threeTimesResizeActionPerformed(evt);
            }
        });
        ResizeMenu.add(threeTimesResize);

        VideoOpt.add(ResizeMenu);

        FiltersMenu.setText(bundle.getString("MainGUI.FiltersMenu.text")); // NOI18N

        filtersGroup.add(noneCheck);
        noneCheck.setSelected(true);
        noneCheck.setText(bundle.getString("MainGUI.noneCheck.text")); // NOI18N
        noneCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                noneCheckActionPerformed(evt);
            }
        });
        FiltersMenu.add(noneCheck);

        filtersGroup.add(anisotropicCheck);
        anisotropicCheck.setSelected(Settings.getInstance().readBool("emu.graphics.filters.anisotropic"));
        anisotropicCheck.setText(bundle.getString("MainGUI.anisotropicCheck.text")); // NOI18N
        anisotropicCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                anisotropicCheckActionPerformed(evt);
            }
        });
        FiltersMenu.add(anisotropicCheck);

        VideoOpt.add(FiltersMenu);

        FrameSkipMenu.setText(bundle.getString("MainGUI.FrameSkipMenu.text")); // NOI18N

        frameSkipGroup.add(FrameSkipNone);
        FrameSkipNone.setSelected(true);
        FrameSkipNone.setText(bundle.getString("MainGUI.FrameSkipNone.text")); // NOI18N
        FrameSkipNone.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                frameSkipNoneActionPerformed(evt);
            }
        });
        FrameSkipMenu.add(FrameSkipNone);

        frameSkipGroup.add(FPS5);
        FPS5.setText("5 FPS"); // NOI18N
        FPS5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                frameSkipFPS5ActionPerformed(evt);
            }
        });
        FrameSkipMenu.add(FPS5);

        frameSkipGroup.add(FPS10);
        FPS10.setText("10 FPS"); // NOI18N
        FPS10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                frameSkipFPS10ActionPerformed(evt);
            }
        });
        FrameSkipMenu.add(FPS10);

        frameSkipGroup.add(FPS15);
        FPS15.setText("15 FPS"); // NOI18N
        FPS15.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                frameSkipFPS15ActionPerformed(evt);
            }
        });
        FrameSkipMenu.add(FPS15);

        frameSkipGroup.add(FPS20);
        FPS20.setText("20 FPS"); // NOI18N
        FPS20.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                frameSkipFPS20ActionPerformed(evt);
            }
        });
        FrameSkipMenu.add(FPS20);

        frameSkipGroup.add(FPS30);
        FPS30.setText("30 FPS"); // NOI18N
        FPS30.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                frameSkipFPS30ActionPerformed(evt);
            }
        });
        FrameSkipMenu.add(FPS30);

        frameSkipGroup.add(FPS60);
        FPS60.setText("60 FPS"); // NOI18N
        FPS60.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                frameSkipFPS60ActionPerformed(evt);
            }
        });
        FrameSkipMenu.add(FPS60);

        VideoOpt.add(FrameSkipMenu);

        ShotItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F5, 0));
        ShotItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/ScreenshotIcon.png"))); // NOI18N
        ShotItem.setText(bundle.getString("MainGUI.ShotItem.text")); // NOI18N
        ShotItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ShotItemActionPerformed(evt);
            }
        });
        VideoOpt.add(ShotItem);

        RotateItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F6, 0));
        RotateItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/RotateIcon.png"))); // NOI18N
        RotateItem.setText(bundle.getString("MainGUI.RotateItem.text")); // NOI18N
        RotateItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RotateItemActionPerformed(evt);
            }
        });
        VideoOpt.add(RotateItem);

        OptionsMenu.add(VideoOpt);

        AudioOpt.setText(bundle.getString("MainGUI.AudioOpt.text")); // NOI18N

        MuteOpt.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_M, java.awt.event.InputEvent.SHIFT_MASK));
        MuteOpt.setText(bundle.getString("MainGUI.MuteOpt.text")); // NOI18N
        MuteOpt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MuteOptActionPerformed(evt);
            }
        });
        AudioOpt.add(MuteOpt);

        OptionsMenu.add(AudioOpt);

        ClockSpeedOpt.setText(bundle.getString("MainGUI.ClockSpeedOpt.text")); // NOI18N

        clockSpeedGroup.add(ClockSpeed50);
        ClockSpeed50.setText(bundle.getString("MainGUI.ClockSpeed50.text")); // NOI18N
        ClockSpeed50.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ClockSpeed50ActionPerformed(evt);
            }
        });
        ClockSpeedOpt.add(ClockSpeed50);

        clockSpeedGroup.add(ClockSpeed75);
        ClockSpeed75.setText(bundle.getString("MainGUI.ClockSpeed75.text")); // NOI18N
        ClockSpeed75.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ClockSpeed75ActionPerformed(evt);
            }
        });
        ClockSpeedOpt.add(ClockSpeed75);

        clockSpeedGroup.add(ClockSpeedNormal);
        ClockSpeedNormal.setSelected(true);
        ClockSpeedNormal.setText(bundle.getString("MainGUI.ClockSpeedNormal.text")); // NOI18N
        ClockSpeedNormal.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ClockSpeedNormalActionPerformed(evt);
            }
        });
        ClockSpeedOpt.add(ClockSpeedNormal);

        clockSpeedGroup.add(ClockSpeed150);
        ClockSpeed150.setText(bundle.getString("MainGUI.ClockSpeed150.text")); // NOI18N
        ClockSpeed150.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ClockSpeed150ActionPerformed(evt);
            }
        });
        ClockSpeedOpt.add(ClockSpeed150);

        clockSpeedGroup.add(ClockSpeed200);
        ClockSpeed200.setText(bundle.getString("MainGUI.ClockSpeed200.text")); // NOI18N
        ClockSpeed200.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ClockSpeed200ActionPerformed(evt);
            }
        });
        ClockSpeedOpt.add(ClockSpeed200);

        clockSpeedGroup.add(ClockSpeed300);
        ClockSpeed300.setText(bundle.getString("MainGUI.ClockSpeed300.text")); // NOI18N
        ClockSpeed300.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ClockSpeed300ActionPerformed(evt);
            }
        });
        ClockSpeedOpt.add(ClockSpeed300);

        OptionsMenu.add(ClockSpeedOpt);

        ControlsConf.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F11, 0));
        ControlsConf.setText(bundle.getString("MainGUI.ControlsConf.text")); // NOI18N
        ControlsConf.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ControlsConfActionPerformed(evt);
            }
        });
        OptionsMenu.add(ControlsConf);

        ConfigMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F12, 0));
        ConfigMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/SettingsIcon.png"))); // NOI18N
        ConfigMenu.setText(bundle.getString("MainGUI.ConfigMenu.text")); // NOI18N
        ConfigMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ConfigMenuActionPerformed(evt);
            }
        });
        OptionsMenu.add(ConfigMenu);

        MenuBar.add(OptionsMenu);

        DebugMenu.setText(bundle.getString("MainGUI.DebugMenu.text")); // NOI18N

        ToolsSubMenu.setText(bundle.getString("MainGUI.ToolsSubMenu.text")); // NOI18N

        LoggerMenu.setText(bundle.getString("ConsoleWindow.title")); // NOI18N

        ToggleLogger.setText(bundle.getString("MainGUI.ToggleLogger.text")); // NOI18N
        ToggleLogger.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ToggleLoggerActionPerformed(evt);
            }
        });
        LoggerMenu.add(ToggleLogger);

        CustomLogger.setText(bundle.getString("MainGUI.CustomLogger.text")); // NOI18N
        CustomLogger.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CustomLoggerActionPerformed(evt);
            }
        });
        LoggerMenu.add(CustomLogger);

        ToolsSubMenu.add(LoggerMenu);

        EnterDebugger.setText(bundle.getString("DisassemblerFrame.title")); // NOI18N
        EnterDebugger.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                EnterDebuggerActionPerformed(evt);
            }
        });
        ToolsSubMenu.add(EnterDebugger);

        EnterMemoryViewer.setText(bundle.getString("MemoryViewer.title")); // NOI18N
        EnterMemoryViewer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                EnterMemoryViewerActionPerformed(evt);
            }
        });
        ToolsSubMenu.add(EnterMemoryViewer);

        EnterImageViewer.setText(bundle.getString("ImageViewer.title")); // NOI18N
        EnterImageViewer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                EnterImageViewerActionPerformed(evt);
            }
        });
        ToolsSubMenu.add(EnterImageViewer);

        VfpuRegisters.setText(bundle.getString("VfpuFrame.title")); // NOI18N
        VfpuRegisters.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                VfpuRegistersActionPerformed(evt);
            }
        });
        ToolsSubMenu.add(VfpuRegisters);

        ElfHeaderViewer.setText(bundle.getString("ElfHeaderInfo.title")); // NOI18N
        ElfHeaderViewer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ElfHeaderViewerActionPerformed(evt);
            }
        });
        ToolsSubMenu.add(ElfHeaderViewer);

        FileLog.setText(bundle.getString("FileLoggerFrame.title")); // NOI18N
        FileLog.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FileLogActionPerformed(evt);
            }
        });
        ToolsSubMenu.add(FileLog);

        InstructionCounter.setText(bundle.getString("InstructionCounter.title")); // NOI18N
        InstructionCounter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                InstructionCounterActionPerformed(evt);
            }
        });
        ToolsSubMenu.add(InstructionCounter);

        DebugMenu.add(ToolsSubMenu);

        DumpIso.setText(bundle.getString("MainGUI.DumpIso.text")); // NOI18N
        DumpIso.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DumpIsoActionPerformed(evt);
            }
        });
        DebugMenu.add(DumpIso);

        ResetProfiler.setText(bundle.getString("MainGUI.ResetProfiler.text")); // NOI18N
        ResetProfiler.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ResetProfilerActionPerformed(evt);
            }
        });
        DebugMenu.add(ResetProfiler);

        ClearTextureCache.setText(bundle.getString("MainGUI.ClearTextureCache.text")); // NOI18N
        ClearTextureCache.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ClearTextureCacheActionPerformed(evt);
            }
        });
        DebugMenu.add(ClearTextureCache);

        ClearVertexCache.setText(bundle.getString("MainGUI.ClearVertexCache.text")); // NOI18N
        ClearVertexCache.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ClearVertexCacheActionPerformed(evt);
            }
        });
        DebugMenu.add(ClearVertexCache);

        MenuBar.add(DebugMenu);

        CheatsMenu.setText(bundle.getString("MainGUI.CheatsMenu.text")); // NOI18N

        cwcheat.setText("CWCheat"); // NOI18N
        cwcheat.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cwcheatActionPerformed(evt);
            }
        });
        CheatsMenu.add(cwcheat);

        MenuBar.add(CheatsMenu);

        LanguageMenu.setText("Language"); // NOI18N

        SystemLocale.setText(bundle.getString("MainGUI.SystemLocale.text")); // NOI18N
        SystemLocale.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SystemLocaleActionPerformed(evt);
            }
        });
        LanguageMenu.add(SystemLocale);

        English.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/flags/en_EN.png"))); // NOI18N
        java.util.ResourceBundle bundle1 = java.util.ResourceBundle.getBundle("jpcsp/languages/common"); // NOI18N
        English.setText(bundle1.getString("english")); // NOI18N
        English.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                EnglishActionPerformed(evt);
            }
        });
        LanguageMenu.add(English);

        French.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/flags/fr_FR.png"))); // NOI18N
        French.setText(bundle1.getString("french")); // NOI18N
        French.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FrenchActionPerformed(evt);
            }
        });
        LanguageMenu.add(French);

        German.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/flags/de_DE.png"))); // NOI18N
        German.setText(bundle1.getString("german")); // NOI18N
        German.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GermanActionPerformed(evt);
            }
        });
        LanguageMenu.add(German);

        Lithuanian.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/flags/lt_LT.png"))); // NOI18N
        Lithuanian.setText(bundle1.getString("lithuanian")); // NOI18N
        Lithuanian.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LithuanianActionPerformed(evt);
            }
        });
        LanguageMenu.add(Lithuanian);

        Spanish.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/flags/es_ES.png"))); // NOI18N
        Spanish.setText(bundle1.getString("spanish")); // NOI18N
        Spanish.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SpanishActionPerformed(evt);
            }
        });
        LanguageMenu.add(Spanish);

        Catalan.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/flags/es_CA.png"))); // NOI18N
        Catalan.setText(bundle1.getString("catalan")); // NOI18N
        Catalan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CatalanActionPerformed(evt);
            }
        });
        LanguageMenu.add(Catalan);

        Portuguese.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/flags/pt_PT.png"))); // NOI18N
        Portuguese.setText(bundle1.getString("portuguese")); // NOI18N
        Portuguese.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PortugueseActionPerformed(evt);
            }
        });
        LanguageMenu.add(Portuguese);

        PortugueseBR.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/flags/pt_BR.png"))); // NOI18N
        PortugueseBR.setText(bundle1.getString("portuguesebr")); // NOI18N
        PortugueseBR.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PortugueseBRActionPerformed(evt);
            }
        });
        LanguageMenu.add(PortugueseBR);

        Japanese.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/flags/jp_JP.png"))); // NOI18N
        Japanese.setText(bundle1.getString("japanese")); // NOI18N
        Japanese.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                JapaneseActionPerformed(evt);
            }
        });
        LanguageMenu.add(Japanese);

        Russian.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/flags/ru_RU.png"))); // NOI18N
        Russian.setText(bundle1.getString("russian")); // NOI18N
        Russian.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RussianActionPerformed(evt);
            }
        });
        LanguageMenu.add(Russian);

        Polish.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/flags/pl_PL.png"))); // NOI18N
        Polish.setText(bundle1.getString("polish")); // NOI18N
        Polish.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PolishActionPerformed(evt);
            }
        });
        LanguageMenu.add(Polish);

        ChinesePRC.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/flags/cn_CN.png"))); // NOI18N
        ChinesePRC.setText(bundle1.getString("simplifiedChinese")); // NOI18N
        ChinesePRC.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ChinesePRCActionPerformed(evt);
            }
        });
        LanguageMenu.add(ChinesePRC);

        ChineseTW.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/flags/tw_TW.png"))); // NOI18N
        ChineseTW.setText(bundle1.getString("traditionalChinese")); // NOI18N
        ChineseTW.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ChineseTWActionPerformed(evt);
            }
        });
        LanguageMenu.add(ChineseTW);

        Italian.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/flags/it_IT.png"))); // NOI18N
        Italian.setText(bundle1.getString("italian")); // NOI18N
        Italian.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ItalianActionPerformed(evt);
            }
        });
        LanguageMenu.add(Italian);

        Greek.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/flags/gr_EL.png"))); // NOI18N
        Greek.setText(bundle1.getString("greek")); // NOI18N
        Greek.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GreekActionPerformed(evt);
            }
        });
        LanguageMenu.add(Greek);

        MenuBar.add(LanguageMenu);

        HelpMenu.setText(bundle.getString("MainGUI.HelpMenu.text")); // NOI18N

        About.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
        About.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/AboutIcon.png"))); // NOI18N
        About.setText(bundle.getString("MainGUI.About.text")); // NOI18N
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

    private void createComponents() {
        initComponents();

        if (useFullscreen) {
            // Hide the menu bar and the toolbar in full screen mode
            MenuBar.setVisible(false);
            mainToolBar.setVisible(false);
            getContentPane().remove(mainToolBar);

            fillerLeft = new JLabel();
            fillerRight = new JLabel();
            fillerTop = new JLabel();
            fillerBottom = new JLabel();

            fillerLeft.setBackground(Color.BLACK);
            fillerRight.setBackground(Color.BLACK);
            fillerTop.setBackground(Color.BLACK);
            fillerBottom.setBackground(Color.BLACK);

            fillerLeft.setOpaque(true);
            fillerRight.setOpaque(true);
            fillerTop.setOpaque(true);
            fillerBottom.setOpaque(true);

            getContentPane().add(fillerLeft, BorderLayout.LINE_START);
            getContentPane().add(fillerRight, BorderLayout.LINE_END);
            getContentPane().add(fillerTop, BorderLayout.NORTH);
            getContentPane().add(fillerBottom, BorderLayout.SOUTH);

            makeFullScreenMenu();
        } else {
            float viewportResizeScaleFactor = Modules.sceDisplayModule.getViewportResizeScaleFactor();
            if (viewportResizeScaleFactor <= 1.5f) {
                oneTimeResize.setSelected(true);
            } else if (viewportResizeScaleFactor <= 2.5f) {
                twoTimesResize.setSelected(true);
            } else {
                threeTimesResize.setSelected(true);
            }
        }

        populateRecentMenu();
    }

    private void changeLanguage(String language) {
        Settings.getInstance().writeString("emu.language", language);
        JpcspDialogManager.showInformation(this, "Language change will take effect after application restart.");
    }

    /**
     * Create a popup menu for use in full screen mode. In full screen mode, the
     * menu bar and the toolbar are not displayed. To keep a consistent user
     * interface, the popup menu is composed of the entries from the toolbar and
     * from the menu bar.
     *
     * Accelerators are only working when the popup menu is displayed.
     */
    private void makeFullScreenMenu() {
        fullScreenMenu = new JPopupMenu();

        JMenuItem popupMenuItemRun = new JMenuItem(java.util.ResourceBundle.getBundle("jpcsp/languages/jpcsp").getString("MainGUI.RunButton.text"));
        popupMenuItemRun.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/PlayIcon.png")));
        popupMenuItemRun.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RunButtonActionPerformed(e);
            }
        });

        JMenuItem popupMenuItemPause = new JMenuItem(java.util.ResourceBundle.getBundle("jpcsp/languages/jpcsp").getString("MainGUI.PauseButton.text"));
        popupMenuItemPause.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/PauseIcon.png")));
        popupMenuItemPause.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                PauseButtonActionPerformed(e);
            }
        });

        JMenuItem popupMenuItemReset = new JMenuItem(java.util.ResourceBundle.getBundle("jpcsp/languages/jpcsp").getString("MainGUI.ResetButton.text"));
        popupMenuItemReset.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/StopIcon.png")));
        popupMenuItemReset.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ResetButtonActionPerformed(e);
            }
        });

        fullScreenMenu.add(popupMenuItemRun);
        fullScreenMenu.add(popupMenuItemPause);
        fullScreenMenu.add(popupMenuItemReset);
        fullScreenMenu.addSeparator();

        // Add all the menu entries from the MenuBar to the full screen menu
        while (MenuBar.getMenuCount() > 0) {
            fullScreenMenu.add(MenuBar.getMenu(0));
        }

        // Move the "Exit" menu item from the File menu
        // to the end of the full screen menu for convenience.
        fullScreenMenu.addSeparator();
        fullScreenMenu.add(ExitEmu);

        // The resize menu is not relevant in full screen mode
        VideoOpt.remove(ResizeMenu);
    }

    public static Dimension getFullScreenDimension() {
        DisplayMode displayMode;
        if (Emulator.getMainGUI().getDisplayMode() != null) {
            displayMode = Emulator.getMainGUI().getDisplayMode();
        } else {
            displayMode = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode();
        }
        return new Dimension(displayMode.getWidth(), displayMode.getHeight());
//    	return GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().getSize();
    }

    @Override
    public void startWindowDialog(Window window) {
        GraphicsDevice localDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (localDevice.getFullScreenWindow() != null) {
            localDevice.setFullScreenWindow(null);
        }
        window.setVisible(true);
    }

    @Override
    public void startBackgroundWindowDialog(Window window) {
        startWindowDialog(window);
        requestFocus();
        Modules.sceDisplayModule.getCanvas().requestFocusInWindow();
    }

    @Override
    public void endWindowDialog() {
        if (displayMode != null) {
            GraphicsDevice localDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            if (localDevice.getFullScreenWindow() == null) {
                localDevice.setFullScreenWindow(this);
                setDisplayMode();
            }
            if (useFullscreen) {
                setFullScreenDisplaySize();
            }
        }
    }

    private void changeScreenResolution(int width, int height) {
        // Find the matching display mode with the preferred refresh rate
        // (or the highest refresh rate if the preferred refresh rate is not found).
        GraphicsDevice localDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        DisplayMode[] displayModes = localDevice.getDisplayModes();
        DisplayMode bestDisplayMode = null;
        for (int i = 0; displayModes != null && i < displayModes.length; i++) {
            DisplayMode displayMode = displayModes[i];
            if (displayMode.getWidth() == width && displayMode.getHeight() == height && displayMode.getBitDepth() == displayModeBitDepth) {
                if (bestDisplayMode == null || (bestDisplayMode.getRefreshRate() < displayMode.getRefreshRate() && bestDisplayMode.getRefreshRate() != preferredDisplayModeRefreshRate)) {
                    bestDisplayMode = displayMode;
                }
            }
        }

        if (bestDisplayMode != null) {
            changeScreenResolution(bestDisplayMode);
        }
    }

    private void setDisplayMode() {
        if (displayMode != null) {
            GraphicsDevice localDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            localDevice.setDisplayMode(displayMode);

            if (setLocationThread == null) {
                // Set up a thread calling setLocation() at regular intervals.
                // It seems that the window location is sometimes lost when
                // changing the DisplayMode.
                setLocationThread = new SetLocationThread();
                setLocationThread.setName("Set MainGUI Location Thread");
                setLocationThread.setDaemon(true);
                setLocationThread.start();
            }
        }
    }

    @Override
    public void setLocation() {
        if (displayMode != null && useFullscreen) {
            // FIXME When running in non-native resolution, the window is not displaying
            // if it is completely visible. It is only displaying if part of it is
            // hidden (e.g. outside screen borders).
            // This seems to be a Java bug.
            // Hack here is to move the window 1 pixel outside the screen so that
            // it gets displayed.
            if (fillerTop == null || fillerTop.getHeight() == 0) {
                if (getLocation().y != -1) {
                    setLocation(0, -1);
                }
            } else if (fillerLeft.getWidth() == 0) {
                if (getLocation().x != -1) {
                    setLocation(-1, 0);
                }
            }
        }
    }

    @Override
    public void setFullScreenDisplaySize() {
        Dimension size = new Dimension(sceDisplay.getResizedWidth(Screen.width), sceDisplay.getResizedHeight(Screen.height));
        setFullScreenDisplaySize(size);
    }

    private void setFullScreenDisplaySize(Dimension size) {
        Dimension fullScreenSize = getFullScreenDimension();

        setLocation();
        if (size.width < fullScreenSize.width) {
            fillerLeft.setSize((fullScreenSize.width - size.width) / 2, fullScreenSize.height);
            fillerRight.setSize(fullScreenSize.width - size.width - fillerLeft.getWidth(), fullScreenSize.height);
        } else {
            fillerLeft.setSize(0, 0);
            fillerRight.setSize(1, fullScreenSize.height);
            setSize(fullScreenSize.width + 1, fullScreenSize.height);
            setPreferredSize(getSize());
        }

        if (size.height < fullScreenSize.height) {
            fillerTop.setSize(fullScreenSize.width, (fullScreenSize.height - size.height) / 2);
            fillerBottom.setSize(fullScreenSize.width, fullScreenSize.height - size.height - fillerTop.getHeight());
        } else {
            fillerTop.setSize(0, 0);
            fillerBottom.setSize(fullScreenSize.width, 1);
            setSize(fullScreenSize.width, fullScreenSize.height + 1);
            setPreferredSize(getSize());
        }

        fillerLeft.setPreferredSize(fillerLeft.getSize());
        fillerRight.setPreferredSize(fillerRight.getSize());
        fillerTop.setPreferredSize(fillerTop.getSize());
        fillerBottom.setPreferredSize(fillerBottom.getSize());
    }

    private void changeScreenResolution(DisplayMode displayMode) {
        GraphicsDevice localDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (localDevice.isFullScreenSupported()) {
            this.displayMode = displayMode;
            localDevice.setFullScreenWindow(this);
            setDisplayMode();
            if (useFullscreen) {
                setSize(getFullScreenDimension());
                setPreferredSize(getFullScreenDimension());
                setLocation();
            }

            if (Emulator.log.isInfoEnabled()) {
                Emulator.log.info(String.format("Changing resolution to %dx%d, %d bits, %d Hz", displayMode.getWidth(), displayMode.getHeight(), displayMode.getBitDepth(), displayMode.getRefreshRate()));
            }
        }
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
            item.addActionListener(new RecentElementActionListener(RecentElementActionListener.TYPE_UMD, umd.path));
            RecentMenu.add(item);
        }

        if (!recentUMD.isEmpty() && !recentFile.isEmpty()) {
            RecentMenu.addSeparator();
        }

        for (RecentElement file : recentFile) {
            JMenuItem item = new JMenuItem(file.toString());
            item.addActionListener(new RecentElementActionListener(RecentElementActionListener.TYPE_FILE, file.path));
            RecentMenu.add(item);
        }
    }

private void EnterDebuggerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EnterDebuggerActionPerformed
        if (State.debugger == null) {
            State.debugger = new DisassemblerFrame(emulator);
            State.debugger.setLocation(Settings.getInstance().readWindowPos("disassembler"));
        } else {
            State.debugger.RefreshDebugger(false);
        }
        startWindowDialog(State.debugger);
}//GEN-LAST:event_EnterDebuggerActionPerformed

private void RunButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RunButtonActionPerformed
        if (umdvideoplayer != null) {
            umdvideoplayer.initVideo();
        }
        RunEmu();
}//GEN-LAST:event_RunButtonActionPerformed

    private JFileChooser makeJFileChooser() {
        final JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(java.util.ResourceBundle.getBundle("jpcsp/languages/jpcsp").getString("MainGUI.strOpenELFPBP.text"));
        fc.setCurrentDirectory(new java.io.File("."));
        return fc;
    }
private void OpenFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_OpenFileActionPerformed
        PauseEmu();

        final JFileChooser fc = makeJFileChooser();
        String lastOpenedFolder = Settings.getInstance().readString("gui.lastOpenedFileFolder");
        if (lastOpenedFolder != null) {
            fc.setCurrentDirectory(new File(lastOpenedFolder));
        }
        int returnVal = fc.showOpenDialog(this);

        if (userChooseSomething(returnVal)) {
            Settings.getInstance().writeString("gui.lastOpenedFileFolder", fc.getSelectedFile().getParent());
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
        } catch (Exception e) {
            // Required by File.getCanonicalPath
            e.printStackTrace();
        }

        // Files anywhere on user's hard drive, may not work
        // use host0:/ ?
        return pcfilename.replaceAll("\\\\", "/");
    }

    public void loadFile(File file) {
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("jpcsp/languages/jpcsp"); // NOI18N

        //This is where a real application would open the file.
        try {
            if (consolewin != null) {
                consolewin.clearScreenMessages();
            }
            Emulator.log.info(MetaInformation.FULL_NAME);

            umdLoaded = false;
            loadedFile = file;

            // Create a read-only memory-mapped file
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            ByteBuffer readbuffer;
            FileChannel roChannel = null;
            long size = raf.length();
            // Do not try to map very large files, this would raise on OutOfMemory exception.
            if (size > 1 * 1024 * 1024) {
                byte[] bytes = new byte[(int) size];
                int offset = 0;
                // Read large files by chunks.
                while (offset < bytes.length) {
                    int len = raf.read(bytes, offset, Math.min(10 * 1024, bytes.length - offset));
                    if (len < 0) {
                        break;
                    }
                    if (len > 0) {
                        offset += len;
                    }
                }
                readbuffer = ByteBuffer.wrap(bytes, 0, offset);
            } else {
                roChannel = raf.getChannel();
                readbuffer = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, (int) roChannel.size());
            }
            SceModule module = emulator.load(pspifyFilename(file.getPath()), readbuffer);
            if (roChannel != null) {
                roChannel.close();
            }
            raf.close();

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

            if (!isHomebrew) {
                Settings.getInstance().loadPatchSettings();
            }
            logConfigurationSettings();

            if (instructioncounter != null) {
                instructioncounter.RefreshWindow();
            }
            StepLogger.clear();
            StepLogger.setName(file.getPath());
        } catch (GeneralJpcspException e) {
            JpcspDialogManager.showError(this, bundle.getString("MainGUI.strGeneralError.text") + ": " + e.getLocalizedMessage());
        } catch (IOException e) {
            if (file.getName().contains("iso") || file.getName().contains("cso")) {
                JpcspDialogManager.showError(this, bundle.getString("MainGUI.strCriticalError.text") + ": " + bundle.getString("MainGUI.strWrongLoader.text"));
            } else {
                e.printStackTrace();
                JpcspDialogManager.showError(this, bundle.getString("ioError") + ": " + e.getLocalizedMessage());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            if (ex.getMessage() != null) {
                JpcspDialogManager.showError(this, bundle.getString("MainGUI.strCriticalError.text") + ": " + ex.getLocalizedMessage());
            } else {
                JpcspDialogManager.showError(this, bundle.getString("MainGUI.strCriticalError.text") + ": " + bundle.getString("MainGUI.strCheckConsole.text"));
            }
        }
    }

    private void addRecentFile(File file, String title) {
        String s = file.getPath();
        for (int i = 0; i < recentFile.size(); ++i) {
            if (recentFile.get(i).path.equals(s)) {
                recentFile.remove(i--);
            }
        }
        recentFile.add(0, new RecentElement(s, title));
        while (recentFile.size() > MAX_RECENT) {
            recentFile.remove(MAX_RECENT);
        }
        Settings.getInstance().writeRecent("file", recentFile);
        populateRecentMenu();
    }

    private void addRecentUMD(File file, String title) {
        try {
            String s = file.getCanonicalPath();
            for (int i = 0; i < recentUMD.size(); ++i) {
                if (recentUMD.get(i).path.equals(s)) {
                    recentUMD.remove(i--);
                }
            }
            recentUMD.add(0, new RecentElement(s, title));
            while (recentUMD.size() > MAX_RECENT) {
                recentUMD.remove(MAX_RECENT);
            }
            Settings.getInstance().writeRecent("umd", recentUMD);
            populateRecentMenu();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

private void PauseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PauseButtonActionPerformed
        if (umdvideoplayer != null) {
            umdvideoplayer.pauseVideo();
        }
        TogglePauseEmu();
}//GEN-LAST:event_PauseButtonActionPerformed

private void ElfHeaderViewerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ElfHeaderViewerActionPerformed
        if (elfheader == null) {
            elfheader = new ElfHeaderInfo();
            elfheader.setLocation(Settings.getInstance().readWindowPos("elfheader"));
        } else {
            elfheader.RefreshWindow();
        }
        startWindowDialog(elfheader);
}//GEN-LAST:event_ElfHeaderViewerActionPerformed

private void EnterMemoryViewerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EnterMemoryViewerActionPerformed
        if (State.memoryViewer == null) {
            State.memoryViewer = new MemoryViewer();
            State.memoryViewer.setLocation(Settings.getInstance().readWindowPos("memoryview"));
        } else {
            State.memoryViewer.RefreshMemory();
        }
        startWindowDialog(State.memoryViewer);
}//GEN-LAST:event_EnterMemoryViewerActionPerformed

private void EnterImageViewerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EnterImageViewerActionPerformed
        if (State.imageViewer == null) {
            State.imageViewer = new ImageViewer();
        } else {
            State.imageViewer.refreshImage();
        }
        startWindowDialog(State.imageViewer);
}//GEN-LAST:event_EnterImageViewerActionPerformed

private void AboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AboutActionPerformed
        StringBuilder message = new StringBuilder();
        message.append("<html>").append("<h2>" + MetaInformation.FULL_NAME + "</h2>").append("<hr/>").append("Official site      : <a href='" + MetaInformation.OFFICIAL_SITE + "'>" + MetaInformation.OFFICIAL_SITE + "</a><br/>").append("Official forum     : <a href='" + MetaInformation.OFFICIAL_FORUM + "'>" + MetaInformation.OFFICIAL_FORUM + "</a><br/>").append("Official repository: <a href='" + MetaInformation.OFFICIAL_REPOSITORY + "'>" + MetaInformation.OFFICIAL_REPOSITORY + "</a><br/>").append("<hr/>").append("<i>Team:</i> <font color='gray'>" + MetaInformation.TEAM + "</font>").append("</html>");
        JOptionPane.showMessageDialog(this, message.toString(), MetaInformation.FULL_NAME, JOptionPane.INFORMATION_MESSAGE);
}//GEN-LAST:event_AboutActionPerformed

private void ConfigMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ConfigMenuActionPerformed
        if (setgui == null) {
            setgui = new SettingsGUI();
            Point mainwindow = this.getLocation();
            setgui.setLocation(mainwindow.x + 100, mainwindow.y + 50);
        } else {
            setgui.RefreshWindow();
        }
        startWindowDialog(setgui);
}//GEN-LAST:event_ConfigMenuActionPerformed

private void ExitEmuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ExitEmuActionPerformed
        exitEmu();
}//GEN-LAST:event_ExitEmuActionPerformed

private void OpenMemStickActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_OpenMemStickActionPerformed
        PauseEmu();
        if (memstick == null) {
            memstick = new MemStickBrowser(this, new File("ms0/PSP/GAME"));
            Point mainwindow = this.getLocation();
            memstick.setLocation(mainwindow.x + 100, mainwindow.y + 50);
        } else {
            memstick.refreshFiles();
        }
        memstick.setVisible(true);
}//GEN-LAST:event_OpenMemStickActionPerformed

private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        exitEmu();
}//GEN-LAST:event_formWindowClosing

private void openUmdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openUmdActionPerformed
        PauseEmu();
        if (Settings.getInstance().readBool("emu.umdbrowser")) {
            List<File> umdPaths = new LinkedList<File>();
            umdPaths.add(new File(Settings.getInstance().readString("emu.umdpath") + "/"));
            for (int i = 1; true; i++) {
                String umdPath = Settings.getInstance().readString(String.format("emu.umdpath.%d", i), null);
                if (umdPath == null) {
                    break;
                }
                umdPaths.add(new File(umdPath + "/"));
            }
            umdbrowser = new UmdBrowser(this, umdPaths.toArray(new File[umdPaths.size()]));
            umdbrowser.setVisible(true);
        } else {
            final JFileChooser fc = makeJFileChooser();
            String lastOpenedFolder = Settings.getInstance().readString("gui.lastOpenedUmdFolder");
            if (lastOpenedFolder != null) {
                fc.setCurrentDirectory(new File(lastOpenedFolder));

            }
            fc.setDialogTitle(java.util.ResourceBundle.getBundle("jpcsp/languages/jpcsp").getString("MainGUI.strOpenUMD.text"));
            int returnVal = fc.showOpenDialog(this);

            if (userChooseSomething(returnVal)) {
                Settings.getInstance().writeString("gui.lastOpenedUmdFolder", fc.getSelectedFile().getParent());
                File file = fc.getSelectedFile();
                loadUMD(file);
                loadAndRun();
            } else {
                return;
            }
        }
}//GEN-LAST:event_openUmdActionPerformed

private void switchUmdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_switchUmdActionPerformed
        if (Settings.getInstance().readBool("emu.umdbrowser")) {
            List<File> umdPaths = new LinkedList<File>();
            umdPaths.add(new File(Settings.getInstance().readString("emu.umdpath") + "/"));
            for (int i = 1; true; i++) {
                String umdPath = Settings.getInstance().readString(String.format("emu.umdpath.%d", i), null);
                if (umdPath == null) {
                    break;
                }
                umdPaths.add(new File(umdPath + "/"));
            }
            umdbrowser = new UmdBrowser(this, umdPaths.toArray(new File[umdPaths.size()]));
            umdbrowser.setSwitchingUmd(true);
            umdbrowser.setVisible(true);
        } else {
            final JFileChooser fc = makeJFileChooser();
            String lastOpenedFolder = Settings.getInstance().readString("gui.lastOpenedUmdFolder");
            if (lastOpenedFolder != null) {
                fc.setCurrentDirectory(new File(lastOpenedFolder));
            }
            fc.setDialogTitle(java.util.ResourceBundle.getBundle("jpcsp/languages/jpcsp").getString("MainGUI.switchUMD.text"));
            int returnVal = fc.showOpenDialog(this);

            if (userChooseSomething(returnVal)) {
                Settings.getInstance().writeString("gui.lastOpenedUmdFolder", fc.getSelectedFile().getParent());
                File file = fc.getSelectedFile();
                switchUMD(file);
            }
        }
}//GEN-LAST:event_switchUmdActionPerformed

    /**
     * Don't call this directly, see loadUMD(File file)
     */
    private boolean loadUMD(UmdIsoReader iso, String bootPath) throws IOException {
        boolean success = false;
        try {
            UmdIsoFile bootBin = iso.getFile(bootPath);
            if (bootBin.length() != 0) {
                byte[] bootfile = new byte[(int) bootBin.length()];
                bootBin.read(bootfile);
                ByteBuffer buf = ByteBuffer.wrap(bootfile);
                emulator.load("disc0:/" + bootPath, buf);
                success = true;
            }
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (GeneralJpcspException e) {
        }
        return success;
    }

    /**
     * Don't call this directly, see loadUMD(File file)
     */
    private boolean loadUnpackedUMD(String filename) throws IOException, GeneralJpcspException {
        // Load unpacked BOOT.BIN as if it came from the umd
        File file = new File(filename);
        if (file.exists()) {
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            FileChannel roChannel = raf.getChannel();
            ByteBuffer readbuffer = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, (int) roChannel.size());
            emulator.load("disc0:/PSP_GAME/SYSDIR/EBOOT.BIN", readbuffer);
            raf.close();
            Emulator.log.info("Using unpacked UMD EBOOT.BIN image");
            return true;
        }
        return false;
    }

    public void loadUMD(File file) {
        try {
            // Raising an exception here means the ISO/CSO is not a PSP_GAME.
            // Try checking if it's a UMD_VIDEO or a UMD_AUDIO.
            UmdIsoReader iso = new UmdIsoReader(file.getPath());
            iso.getFile("PSP_GAME/param.sfo");
            loadUMDGame(file);
        } catch (FileNotFoundException e) {
            try {
                // Try loading it as a UMD_VIDEO.
                UmdIsoReader iso = new UmdIsoReader(file.getPath());
                iso.getFile("UMD_VIDEO/param.sfo");
                loadUMDVideo(file);
            } catch (FileNotFoundException ve) {
                try {
                    // Try loading it as a UMD_AUDIO.
                    UmdIsoReader iso = new UmdIsoReader(file.getPath());
                    iso.getFile("UMD_AUDIO/param.sfo");
                    loadUMDAudio(file);
                } catch (FileNotFoundException ae) {
                    // No more formats to check.
                } catch (IOException aioe) {
                    // Ignore.
                }
            } catch (IOException vioe) {
                // Ignore.
            }
        } catch (IOException ioe) {
            // Ignore.
        }
    }

    public void switchUMD(File file) {
        try {
            // Raising an exception here means the ISO/CSO is not a PSP_GAME.
            UmdIsoReader iso = new UmdIsoReader(file.getPath());
            iso.getFile("PSP_GAME/param.sfo");

            Emulator.log.info(String.format("Switching to the UMD %s", file));

            Modules.IoFileMgrForUserModule.setIsoReader(iso);
            Modules.sceUmdUserModule.setIsoReader(iso);

            Modules.sceUmdUserModule.hleUmdSwitch();
        } catch (FileNotFoundException e) {
            // Ignore.
        } catch (IOException ioe) {
            // Ignore.
        }
    }

    public void loadUMDGame(File file) {
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("jpcsp/languages/jpcsp"); // NOI18N
        try {
            if (consolewin != null) {
                consolewin.clearScreenMessages();
            }
            Emulator.log.info(String.format("Java version: %s (%s)", System.getProperty("java.version"), System.getProperty("java.runtime.version")));
            Emulator.log.info(String.format("Java library path: %s", System.getProperty("java.library.path")));

            Modules.SysMemUserForUserModule.reset();
            Emulator.log.info(MetaInformation.FULL_NAME);

            umdLoaded = true;
            loadedFile = file;

            UmdIsoReader iso = new UmdIsoReader(file.getPath());
            UmdIsoFile psfFile = iso.getFile("PSP_GAME/param.sfo");

            PSF psf = new PSF();
            byte[] data = new byte[(int) psfFile.length()];
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

            if (psf.isLikelyHomebrew()) {
                emulator.setFirmwareVersion(Loader.FIRMWAREVERSION_HOMEBREW);
            } else {
                emulator.setFirmwareVersion(psf.getString("PSP_SYSTEM_VER"));
            }
            RuntimeContext.setIsHomebrew(psf.isLikelyHomebrew());

            State.discId = discId;

            State.umdId = null;
            UmdIsoFile umdDataBin = iso.getFile("UMD_DATA.BIN");
            if (umdDataBin != null) {
                byte[] buffer = new byte[(int) umdDataBin.length()];
                umdDataBin.readFully(buffer);
                umdDataBin.close();
                String umdDataBinContent = new String(buffer).replace((char) 0, ' ');
                Emulator.log.info(String.format("Content of UMD_DATA.BIN: '%s'", umdDataBinContent));

                String[] parts = umdDataBinContent.split("\\|");
                if (parts != null && parts.length >= 2) {
                    State.umdId = parts[1];
                }
            }

            Settings.getInstance().loadPatchSettings();

            // Set the memory model 32MB/64MB before loading the EBOOT.BIN
            int memorySize = Settings.getInstance().readInt("memorySize", 0);
            if (memorySize > 0) {
                Emulator.log.info(String.format("Using memory size 0x%X from settings for %s", memorySize, State.discId));
                Modules.SysMemUserForUserModule.setMemorySize(memorySize);
            } else {
                boolean hasMemory64MB = psf.getNumeric("MEMSIZE") == 1;
                if (Settings.getInstance().readBool("memory64MB")) {
                    Emulator.log.info(String.format("Using 64MB memory from settings for %s", State.discId));
                    hasMemory64MB = true;
                }
                Modules.SysMemUserForUserModule.setMemory64MB(hasMemory64MB);
            }

            if ((!discId.equals(State.DISCID_UNKNOWN_UMD) && loadUnpackedUMD(discId + ".BIN"))
                    || // Try to load a previously decrypted EBOOT.BIN (faster)
                    (!discId.equals(State.DISCID_UNKNOWN_UMD) && loadUnpackedUMD(Settings.getInstance().getDiscTmpDirectory() + "EBOOT.BIN"))
                    || // Try to load the EBOOT.BIN (before the BOOT.BIN, same games have an invalid BOOT.BIN but a valid EBOOT.BIN)
                    loadUMD(iso, "PSP_GAME/SYSDIR/EBOOT.OLD")
                    || loadUMD(iso, "PSP_GAME/SYSDIR/EBOOT.BIN")
                    || // As the last chance, try to load the BOOT.BIN
                    loadUMD(iso, "PSP_GAME/SYSDIR/BOOT.BIN")) {

                State.title = title;

                logConfigurationSettings();

                Modules.IoFileMgrForUserModule.setfilepath("disc0/");

                Modules.IoFileMgrForUserModule.setIsoReader(iso);
                jpcsp.HLE.Modules.sceUmdUserModule.setIsoReader(iso);

                if (instructioncounter != null) {
                    instructioncounter.RefreshWindow();
                }
                StepLogger.clear();
                StepLogger.setName(file.getPath());
            } else {
                State.discId = State.DISCID_UNKNOWN_NOTHING_LOADED;
                throw new GeneralJpcspException(bundle.getString("MainGUI.strEncryptedBoot.text"));
            }
        } catch (GeneralJpcspException e) {
            JpcspDialogManager.showError(this, e.getLocalizedMessage());
        } catch (IOException e) {
            e.printStackTrace();
            JpcspDialogManager.showError(this, bundle.getString("MainGUI.strIOError.text") + " : " + e.getLocalizedMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            if (ex.getMessage() != null) {
                JpcspDialogManager.showError(this, bundle.getString("MainGUI.strCriticalError.text") + " : " + ex.getLocalizedMessage());
            } else {
                JpcspDialogManager.showError(this, bundle.getString("MainGUI.strCriticalError.text") + " : " + bundle.getString("MainGUI.strCheckConsole.text"));
            }
        }
    }

    public void loadUMDVideo(File file) {
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("jpcsp/languages/jpcsp"); // NOI18N
        try {
            if (consolewin != null) {
                consolewin.clearScreenMessages();
            }
            Modules.SysMemUserForUserModule.reset();
            Emulator.log.info(MetaInformation.FULL_NAME);

            umdLoaded = true;
            loadedFile = file;

            UmdIsoReader iso = new UmdIsoReader(file.getPath());
            UmdIsoFile psfFile = iso.getFile("UMD_VIDEO/param.sfo");
            UmdIsoFile umdDataFile = iso.getFile("UMD_DATA.BIN");

            PSF psf = new PSF();
            byte[] data = new byte[(int) psfFile.length()];
            psfFile.read(data);
            psf.read(ByteBuffer.wrap(data));

            Emulator.log.info("UMD param.sfo :\n" + psf);
            String title = psf.getPrintableString("TITLE");
            String discId = psf.getString("DISC_ID");
            if (discId == null) {
                byte[] umdDataId = new byte[10];
                String umdDataIdString = "";
                umdDataFile.readFully(umdDataId, 0, 9);
                umdDataIdString = new String(umdDataId);
                if (umdDataIdString.equals("")) {
                    discId = State.DISCID_UNKNOWN_UMD;
                } else {
                    discId = umdDataIdString;
                }
            }

            setTitle(MetaInformation.FULL_NAME + " - " + title);
            addRecentUMD(file, title);

            emulator.setFirmwareVersion(psf.getString("PSP_SYSTEM_VER"));
            RuntimeContext.setIsHomebrew(false);
            Modules.SysMemUserForUserModule.setMemory64MB(psf.getNumeric("MEMSIZE") == 1);

            State.discId = discId;
            State.title = title;

            logConfigurationSettings();

            UmdVideoPlayer vp = new UmdVideoPlayer(this, iso);
            umdvideoplayer = vp;
        } catch (Exception ex) {
            ex.printStackTrace();
            if (ex.getMessage() != null) {
                JpcspDialogManager.showError(this, bundle.getString("MainGUI.strCriticalError.text") + ": " + ex.getLocalizedMessage());
            } else {
                JpcspDialogManager.showError(this, bundle.getString("MainGUI.strCriticalError.text") + ": " + bundle.getString("MainGUI.strCheckConsole.text"));
            }
        }
    }

    public void loadUMDAudio(File file) {
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("jpcsp/languages/jpcsp"); // NOI18N
        try {
            if (consolewin != null) {
                consolewin.clearScreenMessages();
            }
            Modules.SysMemUserForUserModule.reset();
            Emulator.log.info(MetaInformation.FULL_NAME);

            umdLoaded = true;
            loadedFile = file;

            UmdIsoReader iso = new UmdIsoReader(file.getPath());
            UmdIsoFile psfFile = iso.getFile("UMD_AUDIO/param.sfo");

            PSF psf = new PSF();
            byte[] data = new byte[(int) psfFile.length()];
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
            RuntimeContext.setIsHomebrew(false);
            Modules.SysMemUserForUserModule.setMemory64MB(psf.getNumeric("MEMSIZE") == 1);

            State.discId = discId;
            State.title = title;

            logConfigurationSettings();
        } catch (IllegalArgumentException iae) {
            // Ignore...
        } catch (Exception ex) {
            ex.printStackTrace();
            if (ex.getMessage() != null) {
                JpcspDialogManager.showError(this, bundle.getString("MainGUI.strCriticalError.text") + ": " + ex.getLocalizedMessage());
            } else {
                JpcspDialogManager.showError(this, bundle.getString("MainGUI.strCriticalError.text") + ": " + bundle.getString("MainGUI.strCheckConsole.text"));
            }
        }
    }

    private void logConfigurationSetting(String resourceKey, String settingKey, String value, boolean textLeft) {
        boolean isSettingFromPatch = Settings.getInstance().isOptionFromPatch(settingKey);
        String format;
        if (isSettingFromPatch) {
            format = textLeft ? logConfigurationSettingLeftPatch : logConfigurationSettingRightPatch;
        } else {
            format = textLeft ? logConfigurationSettingLeft : logConfigurationSettingRight;
        }

        String text = resourceKey;
        try {
            java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("jpcsp/languages/jpcsp", new Locale("en"));
            text = bundle.getString(resourceKey);
        } catch (MissingResourceException mre) {
            // do nothing
        }
        Emulator.log.info(String.format(format, text, value, "from patch file"));
    }

    private void logConfigurationSettingBool(String resourceKey, String settingKey, boolean textLeft) {
        boolean value = Settings.getInstance().readBool(settingKey);
        logConfigurationSetting(resourceKey, settingKey, value ? "X" : " ", textLeft);
    }

    private void logConfigurationSettingInt(String resourceKey, String settingKey, boolean textLeft) {
        int value = Settings.getInstance().readInt(settingKey);
        logConfigurationSetting(resourceKey, settingKey, Integer.toString(value), textLeft);
    }

    private void logConfigurationSettingString(String resourceKey, String settingKey, boolean textLeft) {
        String value = Settings.getInstance().readString(settingKey);
        logConfigurationSetting(resourceKey, settingKey, value, textLeft);
    }

    private void logConfigurationSettingList(String resourceKey, String settingKey, String[] values, boolean textLeft) {
        int valueIndex = Settings.getInstance().readInt(settingKey);
        String value = Integer.toString(valueIndex);
        if (values != null && valueIndex >= 0 && valueIndex < values.length) {
            value = values[valueIndex];
        }
        logConfigurationSetting(resourceKey, settingKey, value, textLeft);
    }

    private void logConfigurationPanel(String resourceKey) {
        // jog here only in the English locale
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("jpcsp/languages/jpcsp", new Locale("en"));
        Emulator.log.info(String.format("%s / %s", bundle.getString("SettingsGUI.title"), bundle.getString(resourceKey)));
    }

    private void logConfigurationSettings() {
        if (!Emulator.log.isInfoEnabled()) {
            return;
        }

        Emulator.log.info("Using the following settings:");

        // Log the configuration settings
        logConfigurationPanel("SettingsGUI.RegionPanel.title");
        logConfigurationSettingList("language", sceUtility.SYSTEMPARAM_SETTINGS_OPTION_LANGUAGE, SettingsGUI.getImposeLanguages(), true);
        logConfigurationSettingList("buttonpref", sceUtility.SYSTEMPARAM_SETTINGS_OPTION_BUTTON_PREFERENCE, SettingsGUI.getImposeButtons(), true);
        logConfigurationSettingList("daylightSavings", sceUtility.SYSTEMPARAM_SETTINGS_OPTION_DAYLIGHT_SAVING_TIME, SettingsGUI.getSysparamDaylightSavings(), true);
        logConfigurationSettingInt("timezone", sceUtility.SYSTEMPARAM_SETTINGS_OPTION_TIME_ZONE, true);
        logConfigurationSettingList("timeformat", sceUtility.SYSTEMPARAM_SETTINGS_OPTION_TIME_FORMAT, SettingsGUI.getSysparamTimeFormats(), true);
        logConfigurationSettingList("dateformat", sceUtility.SYSTEMPARAM_SETTINGS_OPTION_DATE_FORMAT, SettingsGUI.getSysparamDateFormats(), true);
        logConfigurationSettingList("wlanpowersaving", sceUtility.SYSTEMPARAM_SETTINGS_OPTION_WLAN_POWER_SAVE, SettingsGUI.getSysparamWlanPowerSaves(), true);
        logConfigurationSettingList("adhocChannel", sceUtility.SYSTEMPARAM_SETTINGS_OPTION_ADHOC_CHANNEL, SettingsGUI.getSysparamAdhocChannels(), true);
        logConfigurationSettingString("nickname", sceUtility.SYSTEMPARAM_SETTINGS_OPTION_NICKNAME, true);

        logConfigurationPanel("SettingsGUI.VideoPanel.title");
        logConfigurationSettingBool("disablevbo", "emu.disablevbo", false);
        logConfigurationSettingBool("onlyGeGraphics", "emu.onlyGEGraphics", false);
        logConfigurationSettingBool("usevertex", "emu.useVertexCache", false);
        logConfigurationSettingBool("useshader", "emu.useshaders", false);
        logConfigurationSettingBool("useGeometryShader", "emu.useGeometryShader", false);
        logConfigurationSettingBool("disableubo", "emu.disableubo", false);
        logConfigurationSettingBool("enablevao", "emu.enablevao", false);
        logConfigurationSettingBool("enablegetexture", "emu.enablegetexture", false);
        logConfigurationSettingBool("enablenativeclut", "emu.enablenativeclut", false);
        logConfigurationSettingBool("enabledynamicshaders", "emu.enabledynamicshaders", false);
        logConfigurationSettingBool("enableshaderstenciltest", "emu.enableshaderstenciltest", false);
        logConfigurationSettingBool("enableshadercolormask", "emu.enableshadercolormask", false);
        logConfigurationSettingBool("disableoptimizedvertexinforeading", "emu.disableoptimizedvertexinforeading", false);
        logConfigurationSettingBool("saveStencilToMemory", "emu.saveStencilToMemory", false);
        logConfigurationSettingBool("useSoftwareRenderer", "emu.useSoftwareRenderer", false);

        logConfigurationPanel("SettingsGUI.AudioPanel.title");
        logConfigurationSettingBool("disableaudiothreads", "emu.ignoreaudiothreads", false);
        logConfigurationSettingBool("disableaudiotchannels", "emu.disablesceAudio", false);
        logConfigurationSettingBool("disableaudiotblocking", "emu.disableblockingaudio", false);

        logConfigurationPanel("SettingsGUI.MemoryPanel.title");
        logConfigurationSettingBool("ignoreinvalidmemory", "emu.ignoreInvalidMemoryAccess", false);
        logConfigurationSettingBool("ignoreUnmaped", "emu.ignoreUnmappedImports", false);

        logConfigurationPanel("SettingsGUI.MiscPanel.title");
        logConfigurationSettingBool("useMediaEngine", "emu.useMediaEngine", false);
        logConfigurationSettingBool("useConnector", "emu.useConnector", false);
        logConfigurationSettingBool("useExternalDecoder", "emu.useExternalDecoder", false);
        logConfigurationSettingBool("useDebugFont", "emu.useDebugFont", false);

        logConfigurationPanel("SettingsGUI.CompilerPanel.title");
        logConfigurationSettingBool("useCompiler", "emu.compiler", false);
        logConfigurationSettingBool("outputprofiler", "emu.profiler", false);
        logConfigurationSettingInt("methodMaxInstructions", "emu.compiler.methodMaxInstructions", false);

        logConfigurationPanel("SettingsGUI.CryptoPanel.title");
        logConfigurationSettingBool("extractEboot", "emu.extractEboot", false);
        logConfigurationSettingBool("cryptoSavedata", "emu.cryptoSavedata", false);
        logConfigurationSettingBool("extractPGD", "emu.extractPGD", false);

        logConfigurationPanel("SettingsGUI.DisplayPanel.title");
        logConfigurationSettingString("antiAliasing", "emu.graphics.antialias", true);
        logConfigurationSettingString("resolution", "emu.graphics.resolution", true);
        logConfigurationSettingBool("fullscreenMode", "gui.fullscreen", false);
    }

    public void loadAndRun() {
        if (Settings.getInstance().readBool("emu.loadAndRun")) {
            RunEmu();
        }
    }

    private void ResetButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ResetButtonActionPerformed
        resetEmu();
}//GEN-LAST:event_ResetButtonActionPerformed

    private void resetEmu() {
        if (loadedFile != null) {
            PauseEmu();
            RuntimeContext.reset();
            HLEModuleManager.getInstance().stopModules();
            if (umdLoaded) {
                loadUMD(loadedFile);
            } else {
                loadFile(loadedFile);
            }
        }
    }
private void InstructionCounterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_InstructionCounterActionPerformed
        PauseEmu();
        if (instructioncounter == null) {
            instructioncounter = new InstructionCounter();
            emulator.setInstructionCounter(instructioncounter);
            Point mainwindow = this.getLocation();
            instructioncounter.setLocation(mainwindow.x + 100, mainwindow.y + 50);
        } else {
            instructioncounter.RefreshWindow();
        }
        startWindowDialog(instructioncounter);
}//GEN-LAST:event_InstructionCounterActionPerformed

private void FileLogActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FileLogActionPerformed
        startWindowDialog(State.fileLogger);
}//GEN-LAST:event_FileLogActionPerformed

private void VfpuRegistersActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_VfpuRegistersActionPerformed
        startWindowDialog(VfpuFrame.getInstance());
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
        GEProfiler.reset();
}//GEN-LAST:event_ResetProfilerActionPerformed

private void ClearTextureCacheActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ClearTextureCacheActionPerformed
        VideoEngine.getInstance().clearTextureCache();
}//GEN-LAST:event_ClearTextureCacheActionPerformed

private void ClearVertexCacheActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ClearVertexCacheActionPerformed
        VideoEngine.getInstance().clearVertexCache();
}//GEN-LAST:event_ClearVertexCacheActionPerformed

private void ShotItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ShotItemActionPerformed
        if (umdvideoplayer != null) {
            umdvideoplayer.takeScreenshot();
        }
        Modules.sceDisplayModule.takeScreenshot();
}//GEN-LAST:event_ShotItemActionPerformed

private void RotateItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RotateItemActionPerformed
        sceDisplay screen = Modules.sceDisplayModule;
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("jpcsp/languages/jpcsp"); // NOI18N

        List<Object> options = new ArrayList<Object>();
        options.add(bundle.getString("MainGUI.strRotate90CW.text"));
        options.add(bundle.getString("MainGUI.strRotate90CCW.text"));
        options.add(bundle.getString("MainGUI.strRotate180.text"));
        options.add(bundle.getString("MainGUI.strRotateMirror.text"));
        options.add(bundle.getString("MainGUI.strRotateNormal.text"));

        int jop = JOptionPane.showOptionDialog(null, bundle.getString("MainGUI.strChooseRotation.text"), bundle.getString("MainGUI.strRotate.text"), JOptionPane.UNDEFINED_CONDITION, JOptionPane.QUESTION_MESSAGE, null, options.toArray(), options.get(4));
        if (jop != -1) {
            screen.rotate(jop);
        }
}//GEN-LAST:event_RotateItemActionPerformed

private void ExportVisibleElementsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ExportVisibleElementsActionPerformed
        State.exportGeNextFrame = true;
        State.exportGeOnlyVisibleElements = true;
}//GEN-LAST:event_ExportVisibleElementsActionPerformed

private void ExportAllElementsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ExportAllElementsActionPerformed
        State.exportGeNextFrame = true;
        State.exportGeOnlyVisibleElements = false;
}//GEN-LAST:event_ExportAllElementsActionPerformed

    private byte safeRead8(int address) {
        byte value = 0;
        if (Memory.isAddressGood(address)) {
            value = (byte) Memory.getInstance().read8(address);
        }
        return value;
    }

    private void safeWrite8(byte value, int address) {
        if (Memory.isAddressGood(address)) {
            Memory.getInstance().write8(address, value);
        }
    }
private void SaveSnapActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SaveSnapActionPerformed
        File f = new File("Snap_" + State.discId + ".bin");
        BufferedOutputStream bOut = null;
        ByteBuffer cpuBuf = ByteBuffer.allocate(1024);

        Emulator.getProcessor().save(cpuBuf);

        try {
            bOut = new BufferedOutputStream(new FileOutputStream(f));
            for (int i = 0x08000000; i <= 0x09ffffff; i++) {
                bOut.write(safeRead8(i));
            }

            bOut.write(cpuBuf.array());
        } catch (IOException e) {
        } finally {
            Utilities.close(bOut);
        }
}//GEN-LAST:event_SaveSnapActionPerformed

private void LoadSnapActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LoadSnapActionPerformed
        File f = new File("Snap_" + State.discId + ".bin");
        BufferedInputStream bIn = null;
        ByteBuffer cpuBuf = ByteBuffer.allocate(1024);

        try {
            bIn = new BufferedInputStream(new FileInputStream(f));
            for (int i = 0x08000000; i <= 0x09ffffff; i++) {
                safeWrite8((byte) bIn.read(), i);
            }

            bIn.read(cpuBuf.array());
        } catch (IOException e) {
        } finally {
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
        changeLanguage("ca_ES");
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
        CheatsGUI cwCheats = new CheatsGUI();
        startWindowDialog(cwCheats);
}//GEN-LAST:event_cwcheatActionPerformed

private void RussianActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RussianActionPerformed
        changeLanguage("ru_RU");
}//GEN-LAST:event_RussianActionPerformed

private void PolishActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PolishActionPerformed
        changeLanguage("pl_PL");
}//GEN-LAST:event_PolishActionPerformed

private void ItalianActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ItalianActionPerformed
        changeLanguage("it_IT");
}//GEN-LAST:event_ItalianActionPerformed

private void GreekActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GreekActionPerformed
        changeLanguage("gr_EL");
}//GEN-LAST:event_GreekActionPerformed

private void ControlsConfActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ControlsConfActionPerformed
        if (ctrlgui == null) {
            ctrlgui = new ControlsGUI();
            Point mainwindow = this.getLocation();
            ctrlgui.setLocation(mainwindow.x + 100, mainwindow.y + 50);
        }
        startWindowDialog(ctrlgui);
}//GEN-LAST:event_ControlsConfActionPerformed

private void MuteOptActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MuteOptActionPerformed
        Audio.setMuted(MuteOpt.isSelected());
}//GEN-LAST:event_MuteOptActionPerformed

private void ClockSpeedNormalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ClockSpeedNormalActionPerformed
        // Set clock speed to 1/1
        Emulator.setVariableSpeedClock(1, 1);
}//GEN-LAST:event_ClockSpeedNormalActionPerformed

private void ClockSpeed50ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ClockSpeed50ActionPerformed
        // Set clock speed to 1/2
        Emulator.setVariableSpeedClock(1, 2);
}//GEN-LAST:event_ClockSpeed50ActionPerformed

private void ClockSpeed75ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ClockSpeed75ActionPerformed
        // Set clock speed to 3/4
        Emulator.setVariableSpeedClock(3, 4);
}//GEN-LAST:event_ClockSpeed75ActionPerformed

private void ClockSpeed150ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ClockSpeed150ActionPerformed
        // Set clock speed to 3/2
        Emulator.setVariableSpeedClock(3, 2);
}//GEN-LAST:event_ClockSpeed150ActionPerformed

private void ClockSpeed200ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ClockSpeed200ActionPerformed
        // Set clock speed to 2/1
        Emulator.setVariableSpeedClock(2, 1);
}//GEN-LAST:event_ClockSpeed200ActionPerformed

private void ClockSpeed300ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ClockSpeed300ActionPerformed
        // Set clock speed to 3/1
        Emulator.setVariableSpeedClock(3, 1);
}//GEN-LAST:event_ClockSpeed300ActionPerformed

private void ToggleLoggerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ToggleLoggerActionPerformed
        if (!consolewin.isVisible()) {
            updateConsoleWinPosition();
        }
        consolewin.setVisible(!consolewin.isVisible());
        ToggleLogger.setSelected(consolewin.isVisible());
}//GEN-LAST:event_ToggleLoggerActionPerformed

private void CustomLoggerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CustomLoggerActionPerformed
        if (loggui == null) {
            loggui = new LogGUI(this);
        }
        startWindowDialog(loggui);
}//GEN-LAST:event_CustomLoggerActionPerformed

    private void ChinesePRCActionPerformed(java.awt.event.ActionEvent evt) {
        changeLanguage("cn_CN");
    }

private void ChineseTWActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ChinesePRCActionPerformed
        changeLanguage("tw_TW");
}//GEN-LAST:event_ChinesePRCActionPerformed

private void noneCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_noneCheckActionPerformed
        VideoEngine.getInstance().setUseTextureAnisotropicFilter(false);
        Settings.getInstance().writeBool("emu.graphics.filters.anisotropic", false);
}//GEN-LAST:event_noneCheckActionPerformed

private void anisotropicCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_anisotropicCheckActionPerformed
        VideoEngine.getInstance().setUseTextureAnisotropicFilter(true);
        Settings.getInstance().writeBool("emu.graphics.filters.anisotropic", anisotropicCheck.isSelected());
}//GEN-LAST:event_anisotropicCheckActionPerformed

private void frameSkipNoneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_frameSkipNoneActionPerformed
        Modules.sceDisplayModule.setDesiredFPS(0);
        Settings.getInstance().writeInt("emu.graphics.frameskip.desiredFPS", Modules.sceDisplayModule.getDesiredFPS());
}//GEN-LAST:event_frameSkipNoneActionPerformed

private void frameSkipFPS5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_frameSkipFPS5ActionPerformed
        Modules.sceDisplayModule.setDesiredFPS(5);
        Settings.getInstance().writeInt("emu.graphics.frameskip.desiredFPS", Modules.sceDisplayModule.getDesiredFPS());
}//GEN-LAST:event_frameSkipFPS5ActionPerformed

private void frameSkipFPS10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_frameSkipFPS10ActionPerformed
        Modules.sceDisplayModule.setDesiredFPS(10);
        Settings.getInstance().writeInt("emu.graphics.frameskip.desiredFPS", Modules.sceDisplayModule.getDesiredFPS());
}//GEN-LAST:event_frameSkipFPS10ActionPerformed

private void frameSkipFPS15ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_frameSkipFPS15ActionPerformed
        Modules.sceDisplayModule.setDesiredFPS(15);
        Settings.getInstance().writeInt("emu.graphics.frameskip.desiredFPS", Modules.sceDisplayModule.getDesiredFPS());
}//GEN-LAST:event_frameSkipFPS15ActionPerformed

private void frameSkipFPS20ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_frameSkipFPS20ActionPerformed
        Modules.sceDisplayModule.setDesiredFPS(20);
        Settings.getInstance().writeInt("emu.graphics.frameskip.desiredFPS", Modules.sceDisplayModule.getDesiredFPS());
}//GEN-LAST:event_frameSkipFPS20ActionPerformed

private void frameSkipFPS30ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_frameSkipFPS30ActionPerformed
        Modules.sceDisplayModule.setDesiredFPS(30);
        Settings.getInstance().writeInt("emu.graphics.frameskip.desiredFPS", Modules.sceDisplayModule.getDesiredFPS());
}//GEN-LAST:event_frameSkipFPS30ActionPerformed

private void frameSkipFPS60ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_frameSkipFPS60ActionPerformed
        Modules.sceDisplayModule.setDesiredFPS(60);
        Settings.getInstance().writeInt("emu.graphics.frameskip.desiredFPS", Modules.sceDisplayModule.getDesiredFPS());
}//GEN-LAST:event_frameSkipFPS60ActionPerformed

    private void setViewportResizeScaleFactor(int viewportResizeScaleFactor) {
        Modules.sceDisplayModule.setViewportResizeScaleFactor(viewportResizeScaleFactor);
        pack();
        if (umdvideoplayer != null) {
            umdvideoplayer.pauseVideo();
            umdvideoplayer.setVideoPlayerResizeScaleFactor(this, viewportResizeScaleFactor);
            umdvideoplayer.resumeVideo();
        }
    }

private void oneTimeResizeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_oneTimeResizeActionPerformed
        setViewportResizeScaleFactor(1);
}//GEN-LAST:event_oneTimeResizeActionPerformed

private void twoTimesResizeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_twoTimesResizeActionPerformed
        setViewportResizeScaleFactor(2);
}//GEN-LAST:event_twoTimesResizeActionPerformed

private void threeTimesResizeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_threeTimesResizeActionPerformed
        setViewportResizeScaleFactor(3);
}//GEN-LAST:event_threeTimesResizeActionPerformed

    private void SystemLocaleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SystemLocaleActionPerformed
        changeLanguage("systemLocale");
    }//GEN-LAST:event_SystemLocaleActionPerformed

    private void exitEmu() {
        // Save the current window location only if selected and not in full screen
        if (Settings.getInstance().readBool("gui.saveWindowPos") && !isFullScreen()) {
            Settings.getInstance().writeWindowPos("mainwindow", getLocation());
        }

        ProOnlineNetworkAdapter.exit();
        Modules.ThreadManForUserModule.exit();
        Modules.sceDisplayModule.exit();
        Modules.IoFileMgrForUserModule.exit();
        VideoEngine.exit();
        Screen.exit();
        Emulator.exit();

        System.exit(0);
    }

    public void snaptoMainwindow() {
        updateConsoleWinPosition();
    }

    private void updateConsoleWinPosition() {
        if (Settings.getInstance().readBool("gui.snapLogwindow")) {
            Point mainwindowPos = getLocation();
            consolewin.setLocation(mainwindowPos.x, mainwindowPos.y + getHeight());
        }
    }

    private void RunEmu() {
        emulator.RunEmu();
        Modules.sceDisplayModule.getCanvas().requestFocusInWindow();
    }

    private void TogglePauseEmu() {
        // This is a toggle, so can pause and unpause
        if (Emulator.run) {
            if (!Emulator.pause) {
                Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_PAUSE);
            } else {
                RunEmu();
            }
        }
    }

    private void PauseEmu() {
        // This will only enter pause mode
        if (Emulator.run && !Emulator.pause) {
            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_PAUSE);
        }
    }

    @Override
    public void RefreshButtons() {
        RunButton.setSelected(Emulator.run && !Emulator.pause);
        PauseButton.setSelected(Emulator.run && Emulator.pause);
    }

    @Override
    public void onUmdChange() {
        // Only enable the menu entry "Switch UMD" when sceUmdReplacePermit has been called by the application.
        switchUmd.setEnabled(Modules.sceUmdUserModule.isUmdAllowReplace());
    }

    /**
     * set the FPS portion of the title
     */
    @Override
    public void setMainTitle(String message) {
        String oldtitle = getTitle();
        int sub = oldtitle.indexOf("FPS:");
        if (sub != -1) {
            String newtitle = oldtitle.substring(0, sub - 1);
            setTitle(newtitle + " " + message);
        } else {
            setTitle(oldtitle + " " + message);
        }
    }

    private void printUsage() {
        System.err.println("Usage: java -Xmx512m -jar jpcsp.jar [OPTIONS]");
        System.err.println();
        System.err.println("  -d, --debugger             Open debugger at start.");
        System.err.println("  -f, --loadfile FILE        Load a file.");
        System.err.println("                             Example: ms0/PSP/GAME/pspsolitaire/EBOOT.PBP");
        System.err.println("  -u, --loadumd FILE         Load a UMD. Example: umdimages/cube.iso");
        System.err.println("  -r, --run                  Run loaded file or umd. Use with -f or -u option.");
        System.err.println("  -t, --tests                Run the automated tests.");
        System.err.println("  --netClientPortShift N     Increase Network client ports by N (when running 2 Jpcsp on the same computer)");
        System.err.println("  --netServerPortShift N     Increase Network server ports by N (when running 2 Jpcsp on the same computer)");
    }

    private void processArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            //System.err.println("Args: " + args[0]);
            if (args[i].equals("-t") || args[i].equals("--tests")) {
                //(new AutoTestsRunner()).run();
                //loadFile(new File("pspautotests/tests/rtc/rtc.elf"));
                //RunEmu();

                throw (new RuntimeException("Shouldn't get there"));
            } else if (args[i].equals("-d") || args[i].equals("--debugger")) {
                // hack: reuse this function
                EnterDebuggerActionPerformed(null);
            } else if (args[i].equals("-f") || args[i].equals("--loadfile")) {
                i++;
                if (i < args.length) {
                    File file = new File(args[i]);
                    if (file.exists()) {
                        Modules.sceDisplayModule.setCalledFromCommandLine();
                        loadFile(file);
                    }
                } else {
                    printUsage();
                    break;
                }
            } else if (args[i].equals("-u") || args[i].equals("--loadumd")) {
                i++;
                if (i < args.length) {
                    File file = new File(args[i]);
                    if (file.exists()) {
                        Modules.sceDisplayModule.setCalledFromCommandLine();
                        loadUMD(file);
                    }
                } else {
                    printUsage();
                    break;
                }
            } else if (args[i].equals("-r") || args[i].equals("--run")) {
                RunEmu();
            } else if (args[i].equals("--netClientPortShift")) {
                i++;
                if (i < args.length) {
                    int netClientPortShift = Integer.parseInt(args[i]);
                    Modules.sceNetAdhocModule.setNetClientPortShift(netClientPortShift);
                } else {
                    printUsage();
                    break;
                }
            } else if (args[i].equals("--netServerPortShift")) {
                i++;
                if (i < args.length) {
                    int netServerPortShift = Integer.parseInt(args[i]);
                    Modules.sceNetAdhocModule.setNetServerPortShift(netServerPortShift);
                } else {
                    printUsage();
                    break;
                }
            } else if (args[i].equals("--ProOnline")) {
                ProOnlineNetworkAdapter.setEnabled(true);
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
        // prepare i18n
        String locale = Settings.getInstance().readString("emu.language");
        if (!locale.equals("systemLocale")) {
            // extract language and country for Locale()
            String language = locale.substring(0, 2);
            String country = locale.substring(3, 5);

            Locale.setDefault(new Locale(language, country));
            ResourceBundle.clearCache();
        }

        if (args.length > 0) {
            if (args[0].equals("--tests")) {
                (new AutoTestsRunner()).run();
                return;
            }
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // final copy of args for use in inner class
        final String[] fargs = args;

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setName("GUI");
                MainGUI maingui = new MainGUI();
                maingui.setVisible(true);

                if (Settings.getInstance().readBool("gui.openLogwindow")) {
                    maingui.consolewin.setVisible(true);
                    maingui.ToggleLogger.setSelected(true);
                }

                maingui.processArgs(fargs);
            }
        });
    }

    @Override
    public boolean isFullScreen() {
        return useFullscreen;
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem About;
    private javax.swing.JMenu AudioOpt;
    private javax.swing.JMenuItem Catalan;
    private javax.swing.JMenu CheatsMenu;
    private javax.swing.JMenuItem ChinesePRC;
    private javax.swing.JMenuItem ChineseTW;
    private javax.swing.JMenuItem ClearTextureCache;
    private javax.swing.JMenuItem ClearVertexCache;
    private javax.swing.JCheckBoxMenuItem ClockSpeed150;
    private javax.swing.JCheckBoxMenuItem ClockSpeed200;
    private javax.swing.JCheckBoxMenuItem ClockSpeed300;
    private javax.swing.JCheckBoxMenuItem ClockSpeed50;
    private javax.swing.JCheckBoxMenuItem ClockSpeed75;
    private javax.swing.JCheckBoxMenuItem ClockSpeedNormal;
    private javax.swing.JMenu ClockSpeedOpt;
    private javax.swing.JMenuItem ConfigMenu;
    private javax.swing.JMenuItem ControlsConf;
    private javax.swing.JMenuItem CustomLogger;
    private javax.swing.JMenu DebugMenu;
    private javax.swing.JMenuItem DumpIso;
    private javax.swing.JMenuItem ElfHeaderViewer;
    private javax.swing.JMenuItem English;
    private javax.swing.JMenuItem EnterDebugger;
    private javax.swing.JMenuItem EnterImageViewer;
    private javax.swing.JMenuItem EnterMemoryViewer;
    private javax.swing.JMenuItem ExitEmu;
    private javax.swing.JMenuItem ExportAllElements;
    private javax.swing.JMenu ExportMenu;
    private javax.swing.JMenuItem ExportVisibleElements;
    private javax.swing.JCheckBoxMenuItem FPS10;
    private javax.swing.JCheckBoxMenuItem FPS15;
    private javax.swing.JCheckBoxMenuItem FPS20;
    private javax.swing.JCheckBoxMenuItem FPS30;
    private javax.swing.JCheckBoxMenuItem FPS5;
    private javax.swing.JCheckBoxMenuItem FPS60;
    private javax.swing.JMenuItem FileLog;
    private javax.swing.JMenu FileMenu;
    private javax.swing.JMenu FiltersMenu;
    private javax.swing.JMenu FrameSkipMenu;
    private javax.swing.JCheckBoxMenuItem FrameSkipNone;
    private javax.swing.JMenuItem French;
    private javax.swing.JMenuItem German;
    private javax.swing.JMenuItem Greek;
    private javax.swing.JMenu HelpMenu;
    private javax.swing.JMenuItem InstructionCounter;
    private javax.swing.JMenuItem Italian;
    private javax.swing.JMenuItem Japanese;
    private javax.swing.JMenu LanguageMenu;
    private javax.swing.JMenuItem Lithuanian;
    private javax.swing.JMenuItem LoadSnap;
    private javax.swing.JMenu LoggerMenu;
    private javax.swing.JMenuBar MenuBar;
    private javax.swing.JCheckBoxMenuItem MuteOpt;
    private javax.swing.JMenuItem OpenFile;
    private javax.swing.JMenuItem OpenMemStick;
    private javax.swing.JMenu OptionsMenu;
    private javax.swing.JToggleButton PauseButton;
    private javax.swing.JMenuItem Polish;
    private javax.swing.JMenuItem Portuguese;
    private javax.swing.JMenuItem PortugueseBR;
    private javax.swing.JMenu RecentMenu;
    private javax.swing.JButton ResetButton;
    private javax.swing.JMenuItem ResetProfiler;
    private javax.swing.JMenu ResizeMenu;
    private javax.swing.JMenuItem RotateItem;
    private javax.swing.JToggleButton RunButton;
    private javax.swing.JMenuItem Russian;
    private javax.swing.JMenuItem SaveSnap;
    private javax.swing.JMenuItem ShotItem;
    private javax.swing.JMenuItem Spanish;
    private javax.swing.JMenuItem SystemLocale;
    private javax.swing.JCheckBoxMenuItem ToggleLogger;
    private javax.swing.JMenu ToolsSubMenu;
    private javax.swing.JMenuItem VfpuRegisters;
    private javax.swing.JMenu VideoOpt;
    private javax.swing.JCheckBoxMenuItem anisotropicCheck;
    private javax.swing.ButtonGroup clockSpeedGroup;
    private javax.swing.JMenuItem cwcheat;
    private javax.swing.ButtonGroup filtersGroup;
    private javax.swing.ButtonGroup frameSkipGroup;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JToolBar mainToolBar;
    private javax.swing.JCheckBoxMenuItem noneCheck;
    private javax.swing.JCheckBoxMenuItem oneTimeResize;
    private javax.swing.JMenuItem openUmd;
    private javax.swing.ButtonGroup resGroup;
    private javax.swing.JMenuItem switchUmd;
    private javax.swing.JCheckBoxMenuItem threeTimesResize;
    private javax.swing.JCheckBoxMenuItem twoTimesResize;
    // End of variables declaration//GEN-END:variables

    private boolean userChooseSomething(int returnVal) {
        return returnVal == JFileChooser.APPROVE_OPTION;
    }

    @Override
    public void mousePressed(MouseEvent event) {
        if (useFullscreen && event.isPopupTrigger()) {
            fullScreenMenu.show(event.getComponent(), event.getX(), event.getY());
        }
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        if (useFullscreen && event.isPopupTrigger()) {
            fullScreenMenu.show(event.getComponent(), event.getX(), event.getY());
        }
    }

    @Override
    public void mouseClicked(MouseEvent event) {
    }

    @Override
    public void mouseEntered(MouseEvent event) {
    }

    @Override
    public void mouseExited(MouseEvent event) {
    }

    @Override
    public void keyTyped(KeyEvent event) {
    }

    @Override
    public void keyPressed(KeyEvent event) {
        State.controller.keyPressed(event);
    }

    @Override
    public void keyReleased(KeyEvent event) {
        State.controller.keyReleased(event);
    }

    @Override
    public void componentHidden(ComponentEvent e) {
    }

    @Override
    public void componentMoved(ComponentEvent e) {
        if (consolewin.isVisible()) {
            updateConsoleWinPosition();
        }
    }

    @Override
    public void componentResized(ComponentEvent e) {
    }

    @Override
    public void componentShown(ComponentEvent e) {
    }

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
            if (file.exists()) {
                if (type == TYPE_UMD) {
                    loadUMD(file);
                } else {
                    loadFile(file);
                }
                loadAndRun();
            }
        }
    }

    private static class SetLocationThread extends Thread {

        @Override
        public void run() {
            while (true) {
                try {
                    // Wait for 1 second
                    sleep(1000);
                } catch (InterruptedException e) {
                    // Ignore Exception
                }

                Emulator.getMainGUI().setLocation();
            }
        }
    }

    @Override
    public Rectangle getCaptureRectangle() {
        Insets insets = getInsets();
        Rectangle canvasBounds = Modules.sceDisplayModule.getCanvas().getBounds();
        Rectangle contentBounds = getContentPane().getBounds();

        return new Rectangle(getX() + insets.left + contentBounds.x + canvasBounds.x, getY() + insets.top + contentBounds.y + canvasBounds.y, canvasBounds.width, canvasBounds.height);
    }
}
