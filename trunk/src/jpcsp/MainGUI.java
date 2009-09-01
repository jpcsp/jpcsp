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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Properties;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import jpcsp.Allegrex.compiler.Profiler;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.Debugger.ElfHeaderInfo;
import jpcsp.Debugger.InstructionCounter;
import jpcsp.Debugger.MemoryViewer;
import jpcsp.Debugger.StepLogger;
import jpcsp.Debugger.DisassemblerModule.DisassemblerFrame;
import jpcsp.Debugger.DisassemblerModule.VfpuFrame;
import jpcsp.GUI.MemStickBrowser;
import jpcsp.GUI.RecentElement;
import jpcsp.GUI.SettingsGUI;
import jpcsp.GUI.UmdBrowser;
import jpcsp.HLE.ThreadMan;
import jpcsp.HLE.pspdisplay;
import jpcsp.HLE.pspiofilemgr;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.HLE.pspSysMem;
import jpcsp.filesystems.umdiso.UmdIsoFile;
import jpcsp.filesystems.umdiso.UmdIsoReader;
import jpcsp.format.PSF;
import jpcsp.graphics.VideoEngine;
import jpcsp.log.LogWindow;
import jpcsp.log.LoggingOutputStream;
import jpcsp.util.JpcspDialogManager;
import jpcsp.util.MetaInformation;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

/**
 *
 * @author  shadow
 */
public class MainGUI extends javax.swing.JFrame implements KeyListener, ComponentListener {
    private static final long serialVersionUID = -3647025845406693230L;
    public static final int MAX_RECENT = 4;
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
    private Vector<RecentElement> recentUMD = new Vector<RecentElement>();
    private Vector<RecentElement> recentFile = new Vector<RecentElement>();
    private Level rootLogLevel = null;

    /** Creates new form MainGUI */
    public MainGUI() {
        DOMConfigurator.configure("LogSettings.xml");
        System.setOut(new PrintStream(new LoggingOutputStream(Logger.getLogger("misc"), Level.INFO)));
        consolewin = new LogWindow();

        emulator = new Emulator(this);

        /*next two lines are for overlay menus over joglcanvas*/
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
        //end of

        initComponents();
        populateRecentMenu();

        int pos[] = Settings.getInstance().readWindowPos("mainwindow");
        setLocation(pos[0], pos[1]);
        State.fileLogger.setLocation(pos[0] + 488, pos[1] + 18);
        setTitle(MetaInformation.FULL_NAME);

        /*add glcanvas to frame and pack frame to get the canvas size*/
        getContentPane().add(pspdisplay.getInstance(), java.awt.BorderLayout.CENTER);
        pspdisplay.getInstance().addKeyListener(this);
        this.addComponentListener(this);
        pack();

        Insets insets = this.getInsets();
        Dimension minSize = new Dimension(
            480 + insets.left + insets.right,
            272 + insets.top + insets.bottom);
        this.setMinimumSize(minSize);

        //logging console window stuff
        snapConsole = Settings.getInstance().readBool("gui.snapLogwindow");
        if (snapConsole) {
            mainwindowPos = getLocation();
            consolewin.setLocation(mainwindowPos.x, mainwindowPos.y + getHeight());
        } else {
            pos = Settings.getInstance().readWindowPos("logwindow");
            consolewin.setLocation(pos[0], pos[1]);
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
        DebugMenu = new javax.swing.JMenu();
        EnterDebugger = new javax.swing.JMenuItem();
        EnterMemoryViewer = new javax.swing.JMenuItem();
        VfpuRegisters = new javax.swing.JMenuItem();
        ToggleConsole = new javax.swing.JMenuItem();
        ElfHeaderViewer = new javax.swing.JMenuItem();
        InstructionCounter = new javax.swing.JMenuItem();
        FileLog = new javax.swing.JMenuItem();
        ToggleDebugLog = new javax.swing.JMenuItem();
        DumpIso = new javax.swing.JMenuItem();
        ResetProfiler = new javax.swing.JMenuItem();
        HelpMenu = new javax.swing.JMenu();
        About = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(480, 272));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);

        RunButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/PlayIcon.png"))); // NOI18N
        RunButton.setText("Run");
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
        PauseButton.setText("Pause");
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
        ResetButton.setText("Reset");
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
            public void mouseExited(java.awt.event.MouseEvent evt) {
                MenuBarMouseExited(evt);
            }
        });

        FileMenu.setText("File");

        openUmd.setText("Load UMD ");
        openUmd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openUmdActionPerformed(evt);
            }
        });
        FileMenu.add(openUmd);

        OpenFile.setText("Load File");
        OpenFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                OpenFileActionPerformed(evt);
            }
        });
        FileMenu.add(OpenFile);

        OpenMemStick.setText("Load MemStick");
        OpenMemStick.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                OpenMemStickActionPerformed(evt);
            }
        });
        FileMenu.add(OpenMemStick);

        RecentMenu.setText("Load Recent");
        FileMenu.add(RecentMenu);

        ExitEmu.setText("Exit");
        ExitEmu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ExitEmuActionPerformed(evt);
            }
        });
        FileMenu.add(ExitEmu);

        MenuBar.add(FileMenu);

        EmulationMenu.setText("Emulation");

        RunEmu.setText("Run");
        RunEmu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RunEmuActionPerformed(evt);
            }
        });
        EmulationMenu.add(RunEmu);

        PauseEmu.setText("Pause");
        PauseEmu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PauseEmuActionPerformed(evt);
            }
        });
        EmulationMenu.add(PauseEmu);

        ResetEmu.setText("Reset");
        ResetEmu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ResetEmuActionPerformed(evt);
            }
        });
        EmulationMenu.add(ResetEmu);

        MenuBar.add(EmulationMenu);

        OptionsMenu.setText("Options");

        RotateItem.setText("Rotate");
        RotateItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RotateItemActionPerformed(evt);
            }
        });
        OptionsMenu.add(RotateItem);

        SetttingsMenu.setText("Settings");
        SetttingsMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SetttingsMenuActionPerformed(evt);
            }
        });
        OptionsMenu.add(SetttingsMenu);

        ShotItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F5, 0));
        ShotItem.setText("Screenshot");
        ShotItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ShotItemActionPerformed(evt);
            }
        });
        OptionsMenu.add(ShotItem);

        MenuBar.add(OptionsMenu);

        DebugMenu.setText("Debug");

        EnterDebugger.setText("Enter Debugger");
        EnterDebugger.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                EnterDebuggerActionPerformed(evt);
            }
        });
        DebugMenu.add(EnterDebugger);

        EnterMemoryViewer.setText("Memory Viewer");
        EnterMemoryViewer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                EnterMemoryViewerActionPerformed(evt);
            }
        });
        DebugMenu.add(EnterMemoryViewer);

        VfpuRegisters.setText("VFPU Registers");
        VfpuRegisters.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                VfpuRegistersActionPerformed(evt);
            }
        });
        DebugMenu.add(VfpuRegisters);

        ToggleConsole.setText("Toggle Console");
        ToggleConsole.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ToggleConsoleActionPerformed(evt);
            }
        });
        DebugMenu.add(ToggleConsole);

        ElfHeaderViewer.setText("Elf Header Info");
        ElfHeaderViewer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ElfHeaderViewerActionPerformed(evt);
            }
        });
        DebugMenu.add(ElfHeaderViewer);

        InstructionCounter.setText("Instruction Counter");
        InstructionCounter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                InstructionCounterActionPerformed(evt);
            }
        });
        DebugMenu.add(InstructionCounter);

        FileLog.setLabel("File Log");
        FileLog.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FileLogActionPerformed(evt);
            }
        });
        DebugMenu.add(FileLog);

        ToggleDebugLog.setText("Toggle Debug Logging");
        ToggleDebugLog.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ToggleDebugLogActionPerformed(evt);
            }
        });
        DebugMenu.add(ToggleDebugLog);

        DumpIso.setText("Dump ISO to iso-index.txt");
        DumpIso.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DumpIsoActionPerformed(evt);
            }
        });
        DebugMenu.add(DumpIso);

        ResetProfiler.setText("Reset Profiler Information");
        ResetProfiler.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
            	ResetProfilerActionPerformed(evt);
            }
        });
        DebugMenu.add(ResetProfiler);

        MenuBar.add(DebugMenu);

        HelpMenu.setText("Help");

        About.setText("About");
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


    public LogWindow getConsoleWindow() {
        return consolewin;
    }

    private void populateRecentMenu() {
        RecentMenu.removeAll();
        recentUMD.clear();
        recentFile.clear();

        Settings.getInstance().readRecent("umd", recentUMD);
        Settings.getInstance().readRecent("file", recentFile);

        if(recentUMD.size() > 0) {
            for(int i = 0; i < recentUMD.size(); ++i) {
                JMenuItem item = new JMenuItem(recentUMD.get(i).toString());
                //item.setFont(Settings.getInstance().getFont()); // doesn't seem to work
                item.addActionListener(new RecentElementActionListener(RecentElementActionListener.TYPE_UMD, recentUMD.get(i).path));
                RecentMenu.add(item);
            }
            if(recentFile.size() > 0)
                RecentMenu.addSeparator();
        }

        if(recentFile.size() > 0) {
            for(int i = 0; i < recentFile.size(); ++i) {
                JMenuItem item = new JMenuItem(recentFile.get(i).toString());
                item.addActionListener(new RecentElementActionListener(RecentElementActionListener.TYPE_FILE, recentFile.get(i).path));
                RecentMenu.add(item);
            }
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
        int pos[] = Settings.getInstance().readWindowPos("disassembler");
        State.debugger.setLocation(pos[0], pos[1]);
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
    // Files on memstick
    if (pcfilename.startsWith("ms0"))
        return "ms0:" + pcfilename.substring(3).replaceAll("\\\\", "/");

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

        String findpath = file.getParent();
        //System.out.println(findpath);
        pspiofilemgr.getInstance().setfilepath(findpath);
        pspiofilemgr.getInstance().setIsoReader(null);
        jpcsp.HLE.Modules.sceUmdUserModule.setIsoReader(null);

        RuntimeContext.setIsHomebrew(isHomebrew);
        State.discId = discId;

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
        JpcspDialogManager.showError(this, "General Error : " + e.getMessage());
    } catch (IOException e) {
        e.printStackTrace();
        JpcspDialogManager.showError(this, "IO Error : " + e.getMessage());
    } catch (Exception ex) {
        ex.printStackTrace();
        if (ex.getMessage() != null) {
            JpcspDialogManager.showError(this, "Critical Error : " + ex.getMessage());
        } else {
            JpcspDialogManager.showError(this, "Critical Error : Check console for details.");
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
        recentFile.insertElementAt(new RecentElement(s, title), 0);
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
        recentUMD.insertElementAt(new RecentElement(s, title), 0);
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
      int pos[] = Settings.getInstance().readWindowPos("elfheader");
      elfheader.setLocation(pos[0], pos[1]);
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
        int pos[] = Settings.getInstance().readWindowPos("memoryview");
        State.memoryViewer.setLocation(pos[0], pos[1]);
        State.memoryViewer.setVisible(true);
    }
    else
    {
        State.memoryViewer.RefreshMemory();
        State.memoryViewer.setVisible(true);
    }
}//GEN-LAST:event_EnterMemoryViewerActionPerformed

private void ToggleDebugLogActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ToggleDebugLogActionPerformed
	Logger rootLogger = Logger.getRootLogger();
	if (rootLogLevel == null)
	{
		// Enable DEBUG logging
		rootLogLevel = rootLogger.getLevel();
		rootLogger.setLevel(Level.DEBUG);
	}
	else
	{
		// Reset logging level to previous level
		rootLogger.setLevel(rootLogLevel);
		rootLogLevel = null;
	}
}//GEN-LAST:event_ToggleDebugLogActionPerformed

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
        Point mainwindow = this.getLocation();
        umdbrowser.setLocation(mainwindow.x+100, mainwindow.y+50);
        umdbrowser.setVisible(true);
    }
    else
    {
        final JFileChooser fc = makeJFileChooser();
        fc.setDialogTitle("Open umd iso");
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

            pspiofilemgr.getInstance().setfilepath("disc0/");
            //pspiofilemgr.getInstance().setfilepath("disc0/PSP_GAME/SYSDIR");

            pspiofilemgr.getInstance().setIsoReader(iso);
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
            throw new GeneralJpcspException("File format not supported!");
        }
    } catch (GeneralJpcspException e) {
        JpcspDialogManager.showError(this, "General Error : " + e.getMessage());
    } catch (IOException e) {
        e.printStackTrace();
        JpcspDialogManager.showError(this, "IO Error : " + e.getMessage());
    } catch (Exception ex) {
        ex.printStackTrace();
        if (ex.getMessage() != null) {
            JpcspDialogManager.showError(this, "Critical Error : " + ex.getMessage());
        } else {
            JpcspDialogManager.showError(this, "Critical Error : Check console for details.");
        }
    }
}

private void installCompatibilitySettings()
{
    Emulator.log.info("Loading global compatibility settings");

    boolean onlyGEGraphics = Settings.getInstance().readBool("emu.onlyGEGraphics");
    pspdisplay.getInstance().setOnlyGEGraphics(onlyGEGraphics);

    boolean disableAudio = Settings.getInstance().readBool("emu.disablesceAudio");
    jpcsp.HLE.Modules.sceAudioModule.setChReserveEnabled(!disableAudio);

    boolean audioMuted = Settings.getInstance().readBool("emu.mutesound");
    jpcsp.HLE.Modules.sceAudioModule.setAudioMuted(audioMuted);
    jpcsp.HLE.Modules.sceSasCoreModule.setAudioMuted(audioMuted);

    boolean disableBlocking = Settings.getInstance().readBool("emu.disableblockingaudio");
    jpcsp.HLE.Modules.sceAudioModule.setBlockingEnabled(!disableBlocking);

    boolean ignoreAudioThreads = Settings.getInstance().readBool("emu.ignoreaudiothreads");
    jpcsp.HLE.ThreadMan.getInstance().setThreadBanningEnabled(ignoreAudioThreads);

    boolean ignoreInvalidMemoryAccess = Settings.getInstance().readBool("emu.ignoreInvalidMemoryAccess");
    Memory.getInstance().setIgnoreInvalidMemoryAccess(ignoreInvalidMemoryAccess);

    boolean disableReservedThreadMemory = Settings.getInstance().readBool("emu.disablereservedthreadmemory");
    jpcsp.HLE.pspSysMem.getInstance().setDisableReservedThreadMemory(disableReservedThreadMemory);

    boolean enableWaitThreadEndCB = Settings.getInstance().readBool("emu.enablewaitthreadendcb");
    jpcsp.HLE.ThreadMan.getInstance().setEnableWaitThreadEndCB(enableWaitThreadEndCB);
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
    try {
        Emulator.log.info("Overriding previous settings with patch file");
        patchSettings.load(new BufferedInputStream(new FileInputStream(patchfile)));

        String disableAudio = patchSettings.getProperty("emu.disablesceAudio");
        if (disableAudio != null)
            jpcsp.HLE.Modules.sceAudioModule.setChReserveEnabled(!(Integer.parseInt(disableAudio) != 0));

        String disableBlocking = patchSettings.getProperty("emu.disableblockingaudio");
        if (disableBlocking != null)
            jpcsp.HLE.Modules.sceAudioModule.setBlockingEnabled(!(Integer.parseInt(disableBlocking) != 0));

        String ignoreAudioThreads = patchSettings.getProperty("emu.ignoreaudiothreads");
        if (ignoreAudioThreads != null)
            jpcsp.HLE.ThreadMan.getInstance().setThreadBanningEnabled(Integer.parseInt(ignoreAudioThreads) != 0);

        String ignoreInvalidMemoryAccess = patchSettings.getProperty("emu.ignoreInvalidMemoryAccess");
        if (ignoreInvalidMemoryAccess != null)
            Memory.getInstance().setIgnoreInvalidMemoryAccess(Integer.parseInt(ignoreInvalidMemoryAccess) != 0);

        String onlyGEGraphics = patchSettings.getProperty("emu.onlyGEGraphics");
        if (onlyGEGraphics != null)
            pspdisplay.getInstance().setOnlyGEGraphics(Integer.parseInt(onlyGEGraphics) != 0);

        String disableReservedThreadMemory = patchSettings.getProperty("emu.disablereservedthreadmemory");
        if (disableReservedThreadMemory != null)
            pspSysMem.getInstance().setDisableReservedThreadMemory(Integer.parseInt(disableReservedThreadMemory) != 0);

    } catch (IOException e) {
        e.printStackTrace();
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
        UmdIsoReader iso = pspiofilemgr.getInstance().getIsoReader();
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
pspdisplay.getInstance().repaint();
}//GEN-LAST:event_MenuBarMouseExited

private void ShotItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ShotItemActionPerformed
    pspdisplay.getInstance().getscreen = true;
}//GEN-LAST:event_ShotItemActionPerformed

private void RotateItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RotateItemActionPerformed
pspdisplay screen = pspdisplay.getInstance();
Object[] options = {"90 CW","90 CCW","180","Mirror","Normal"};

int jop = JOptionPane.showOptionDialog(null, "Choose the angle of rotation", "Rotate", JOptionPane.UNDEFINED_CONDITION, JOptionPane.QUESTION_MESSAGE, null, options, options[4]);

if(jop != -1)
    screen.rotate(jop);
else
    return;
}//GEN-LAST:event_RotateItemActionPerformed

private void exitEmu() {
    if (Settings.getInstance().readBool("gui.saveWindowPos"))
        Settings.getInstance().writeWindowPos("mainwindow", getLocation());

    ThreadMan.getInstance().exit();
    pspdisplay.getInstance().exit();
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
    int sub = oldtitle.indexOf("average");
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
    private javax.swing.JMenu DebugMenu;
    private javax.swing.JMenuItem DumpIso;
    private javax.swing.JMenuItem ElfHeaderViewer;
    private javax.swing.JMenu EmulationMenu;
    private javax.swing.JMenuItem EnterDebugger;
    private javax.swing.JMenuItem EnterMemoryViewer;
    private javax.swing.JMenuItem ExitEmu;
    private javax.swing.JMenuItem FileLog;
    private javax.swing.JMenu FileMenu;
    private javax.swing.JMenu HelpMenu;
    private javax.swing.JMenuItem InstructionCounter;
    private javax.swing.JMenuBar MenuBar;
    private javax.swing.JMenuItem OpenFile;
    private javax.swing.JMenuItem OpenMemStick;
    private javax.swing.JMenu OptionsMenu;
    private javax.swing.JToggleButton PauseButton;
    private javax.swing.JMenuItem PauseEmu;
    private javax.swing.JMenu RecentMenu;
    private javax.swing.JButton ResetButton;
    private javax.swing.JMenuItem ResetEmu;
    private javax.swing.JMenuItem RotateItem;
    private javax.swing.JToggleButton RunButton;
    private javax.swing.JMenuItem RunEmu;
    private javax.swing.JMenuItem SetttingsMenu;
    private javax.swing.JMenuItem ShotItem;
    private javax.swing.JMenuItem ToggleConsole;
    private javax.swing.JMenuItem ToggleDebugLog;
    private javax.swing.JMenuItem ResetProfiler;
    private javax.swing.JMenuItem VfpuRegisters;
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
