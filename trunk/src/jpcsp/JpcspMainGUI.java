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

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import jpcsp.Debugger.*;
import jpcsp.Debugger.DisassemblerModule.Disassembler;
import jpcsp.util.JpcspDialogManager;
import jpcsp.util.MetaInformation;

/**
 *
 * @author  George
 */
public class JpcspMainGUI extends javax.swing.JFrame {

    ElfHeaderInfo elfinfo;
    Disassembler dis;
    Emulator emulator;
    Registers regs;
    MemoryViewer memview;
    SettingsGUI setgui;
    final String version = MetaInformation.FULL_NAME;

    /** Creates new form JpcspMainGUI */
    public JpcspMainGUI() {
        initComponents();
        emulator = new Emulator(); //maybe pass the container drawndable

        this.setTitle(version);
    }

    private void createDisassemblerWindow() {
        //disassembler window
        if (dis != null) {
            //clear previously opened stuff
            dis.setVisible(false);
            Disasembler.setIcon(null);
            desktopPane.remove(dis);
            dis = null;
        }
        dis = new Disassembler(emulator, regs, memview);
        //dis.setLocation(300, 0);
        dis.setLocation(Settings.get_instance().readWindowPos("disassembler")[0], Settings.get_instance().readWindowPos("disassembler")[1]);
        dis.setVisible(true);
        desktopPane.add(dis);
        Disasembler.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/tick.gif")));
        try {
            dis.setSelected(true);
        } catch (java.beans.PropertyVetoException e) {
        }
    }

    private void createELFWindow() {

        //elf info window
        if (elfinfo != null) {
            //clear previously opened stuff
            elfinfo.setVisible(false);
            desktopPane.remove(elfinfo);
            elfinfo = null;
        }

        elfinfo = new ElfHeaderInfo();
        //elfinfo.setLocation(0, 0);
        elfinfo.setLocation(Settings.get_instance().readWindowPos("elfheader")[0], Settings.get_instance().readWindowPos("elfheader")[1]);
        elfinfo.setVisible(true);

        desktopPane.add(elfinfo);
        ElfInfo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/tick.gif")));
        try {
            elfinfo.setSelected(true);
        } catch (java.beans.PropertyVetoException e) {
        }
    }

    private void createMemoryViewWindow() {
        if (memview != null) {
            //clear previously opened stuff
            memview.setVisible(false);
            MemView.setIcon(null);
            desktopPane.remove(memview);
            memview = null;
        }
        memview = new MemoryViewer(emulator.getProcessor());
        // memview.setLocation(70, 150);
        memview.setLocation(Settings.get_instance().readWindowPos("memoryview")[0], Settings.get_instance().readWindowPos("memoryview")[1]);
        memview.setVisible(true);
        desktopPane.add(memview);
        MemView.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/tick.gif")));
        try {
            memview.setSelected(true);
        } catch (java.beans.PropertyVetoException e) {
        }
    }

    private void createRegistersWindow() {
        //registers window
        if (regs != null) {
            //clear previously opened stuff
            regs.setVisible(false);
            Registers.setIcon(null);
            desktopPane.remove(regs);
            regs = null;
        }
        regs = new Registers(emulator.getProcessor());
        //regs.setLocation(70, 0);
        regs.setLocation(Settings.get_instance().readWindowPos("registers")[0], Settings.get_instance().readWindowPos("registers")[1]);
        regs.setVisible(true);
        desktopPane.add(regs);
        Registers.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/tick.gif")));
        try {
            regs.setSelected(true);
        } catch (java.beans.PropertyVetoException e) {
        }
    }

    private void hideDisassemblerWindow() {
        if (dis != null) {
            //clear previously opened stuff
            dis.setVisible(false);
            Disasembler.setIcon(null);
            desktopPane.remove(dis);
            dis = null;
        }
    }

    private void hideELFWindow() {
        if (elfinfo != null) {
            //clear previously opened stuff
            elfinfo.setVisible(false);
            desktopPane.remove(elfinfo);
            elfinfo = null;
        }
    }

    private void hideMemoryViewWindow() {
        if (memview != null) {
            //clear previously opened stuff
            memview.setVisible(false);
            MemView.setIcon(null);
            desktopPane.remove(memview);
            memview = null;
        }
    }

    private void hideRegistersWindow() {
        if (regs != null) {
            //clear previously opened stuff
            regs.setVisible(false);
            Registers.setIcon(null);
            desktopPane.remove(regs);
            regs = null;
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

        desktopPane = new javax.swing.JDesktopPane();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        openMenuItem = new javax.swing.JMenuItem();
        exitMenuItem = new javax.swing.JMenuItem();
        Emulation = new javax.swing.JMenu();
        Run = new javax.swing.JMenuItem();
        Stop = new javax.swing.JMenuItem();
        Reset = new javax.swing.JMenuItem();
        Options = new javax.swing.JMenu();
        SettingsMenu = new javax.swing.JMenuItem();
        Windows = new javax.swing.JMenu();
        Disasembler = new javax.swing.JMenuItem();
        ElfInfo = new javax.swing.JMenuItem();
        Registers = new javax.swing.JMenuItem();
        MemView = new javax.swing.JMenuItem();
        WindowsPos = new javax.swing.JMenuItem();
        helpMenu = new javax.swing.JMenu();
        aboutMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setLocationByPlatform(true);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        desktopPane.setBackground(new java.awt.Color(204, 204, 255));

        fileMenu.setText("File");

        openMenuItem.setText("Open File (ELF,PBP)");
        openMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(openMenuItem);

        exitMenuItem.setText("Exit");
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        Emulation.setText("Emulation");

        Run.setText("Run");
        Emulation.add(Run);

        Stop.setText("Stop");
        Emulation.add(Stop);

        Reset.setText("Reset");
        Emulation.add(Reset);

        menuBar.add(Emulation);

        Options.setText("Options");

        SettingsMenu.setText("Settings");
        SettingsMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SettingsMenuActionPerformed(evt);
            }
        });
        Options.add(SettingsMenu);

        menuBar.add(Options);

        Windows.setText("Windows");
        Windows.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                WindowsMouseEntered(evt);
            }
        });

        Disasembler.setText("Disassembler");
        Disasembler.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DisasemblerActionPerformed(evt);
            }
        });
        Windows.add(Disasembler);

        ElfInfo.setText("Elf Info");
        ElfInfo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ElfInfoActionPerformed(evt);
            }
        });
        Windows.add(ElfInfo);

        Registers.setText("Registers");
        Registers.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RegistersActionPerformed(evt);
            }
        });
        Windows.add(Registers);

        MemView.setText("Memory");
        MemView.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MemViewActionPerformed(evt);
            }
        });
        Windows.add(MemView);

        WindowsPos.setText("Reset WindowsPos");
        WindowsPos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                WindowsPosActionPerformed(evt);
            }
        });
        Windows.add(WindowsPos);

        menuBar.add(Windows);

        helpMenu.setText("Help");

        aboutMenuItem.setText("About");
        aboutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(desktopPane, javax.swing.GroupLayout.DEFAULT_SIZE, 894, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(desktopPane, javax.swing.GroupLayout.DEFAULT_SIZE, 666, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
        saveWinPos();
        System.exit(0);
    }//GEN-LAST:event_exitMenuItemActionPerformed

    private JFileChooser makeJFileChooser() {
        final JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Open Elf/Pbp File");
        fc.setCurrentDirectory(new java.io.File("."));
        return fc;
    }

private void openMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openMenuItemActionPerformed
    boolean isloaded = false; // variable to check if user at least choose something

    saveWinPos();//same windows before open something

    final JFileChooser fc = makeJFileChooser();
    int returnVal = fc.showOpenDialog(desktopPane);

    if (userChooseSomething(returnVal)) {
        File file = fc.getSelectedFile();
        //This is where a real application would open the file.
        try {
            emulator.load(file.getPath());
           // emulator.run();
            isloaded = true; //TODO check if it a valid file
            // maybe change the name isloaded to isDebugMode

            this.setTitle(version + " - " + file.getName());
        } catch (IOException e) {
            e.printStackTrace();
            JpcspDialogManager.showError(this, "IO Error : " + e.getMessage());
       // } catch (GeneralJpcspException ex) {
          //  ex.printStackTrace();
          //  JpcspDialogManager.showError(this, "General Error : " + ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            JpcspDialogManager.showError(this, "Critical Error : " + ex.getMessage());
        }
    } else {
        return; //user cancel the action

    }
    if (isloaded) {
        createELFWindow();
        createRegistersWindow();
        createMemoryViewWindow();
        createDisassemblerWindow();
    } else {
        hideELFWindow();
        hideRegistersWindow();
        hideDisassemblerWindow();
        hideMemoryViewWindow();
    }
}//GEN-LAST:event_openMenuItemActionPerformed

private void DisasemblerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DisasemblerActionPerformed

    if (dis != null) {
        // dis.setLocation(300, 0);
        dis.setLocation(Settings.get_instance().readWindowPos("disassembler")[0], Settings.get_instance().readWindowPos("disassembler")[1]);
        if (dis.isVisible()) {
            dis.setVisible(false);
            Disasembler.setIcon(null);

        } else {
            dis.setVisible(true);
            Disasembler.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/tick.gif")));
        }

    }

}//GEN-LAST:event_DisasemblerActionPerformed

private void ElfInfoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ElfInfoActionPerformed
// TODO add your handling code here:
    if (elfinfo != null) {
        //elfinfo.setLocation(0, 0);
        elfinfo.setLocation(Settings.get_instance().readWindowPos("elfheader")[0], Settings.get_instance().readWindowPos("elfheader")[1]);
        if (elfinfo.isVisible()) {
            elfinfo.setVisible(false);
            ElfInfo.setIcon(null);
        } else {
            elfinfo.setVisible(true);
            ElfInfo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/tick.gif")));
        }
    }

}//GEN-LAST:event_ElfInfoActionPerformed

private void WindowsMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_WindowsMouseEntered
// TODO add your handling code here:
    if (elfinfo != null) {
        if (elfinfo.isVisible()) {
            ElfInfo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/tick.gif")));
        } else {
            ElfInfo.setIcon(null);
        }
    }
    if (dis != null) {
        if (dis.isVisible()) {
            Disasembler.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/tick.gif")));
        } else {
            Disasembler.setIcon(null);
        }
    }
    if (regs != null) {
        if (regs.isVisible()) {
            Registers.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/tick.gif")));
        } else {
            Registers.setIcon(null);
        }
    }
    if (memview != null) {
        if (memview.isVisible()) {
            MemView.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/tick.gif")));
        } else {
            MemView.setIcon(null);
        }
    }
}//GEN-LAST:event_WindowsMouseEntered

private void RegistersActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RegistersActionPerformed
// TODO add your handling code here:
    if (regs != null) {
        //regs.setLocation(70, 0);
        regs.setLocation(Settings.get_instance().readWindowPos("registers")[0], Settings.get_instance().readWindowPos("registers")[1]);
        if (regs.isVisible()) {
            regs.setVisible(false);
            Registers.setIcon(null);

        } else {
            regs.setVisible(true);
            Registers.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/tick.gif")));
        }

    }
}//GEN-LAST:event_RegistersActionPerformed

private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed
// TODO add your handling code here:
    StringBuilder message = new StringBuilder();
    message.append("<html>").append("<h2>" + MetaInformation.FULL_NAME + "</h2>").append("<hr/>").append("Official site      : <a href='" + MetaInformation.OFFICIAL_SITE + "'>" + MetaInformation.OFFICIAL_SITE + "</a><br/>").append("Official forum     : <a href='" + MetaInformation.OFFICIAL_FORUM + "'>" + MetaInformation.OFFICIAL_FORUM + "</a><br/>").append("Official repository: <a href='" + MetaInformation.OFFICIAL_REPOSITORY + "'>" + MetaInformation.OFFICIAL_REPOSITORY + "</a><br/>").append("<hr/>").append("<i>Team:</i> <font color='gray'>" + MetaInformation.TEAM + "</font>").append("</html>");
    JOptionPane.showMessageDialog(this, message.toString(), version, JOptionPane.INFORMATION_MESSAGE);
}//GEN-LAST:event_aboutMenuItemActionPerformed

private void MemViewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MemViewActionPerformed
    if (memview != null) {
        //memview.setLocation(70, 150);
        memview.setLocation(Settings.get_instance().readWindowPos("memoryview")[0], Settings.get_instance().readWindowPos("memoryview")[1]);
        if (memview.isVisible()) {
            memview.setVisible(false);
            MemView.setIcon(null);

        } else {
            memview.setVisible(true);
            MemView.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/icons/tick.gif")));
        }

    }
}//GEN-LAST:event_MemViewActionPerformed

private void WindowsPosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_WindowsPosActionPerformed
// TODO add your handling code here:
    //reset windows Pos to default
    if (dis != null) {
        dis.setLocation(300, 0);
    }
    if (elfinfo != null) {
        elfinfo.setLocation(0, 0);
    }
    if (memview != null) {
        memview.setLocation(70, 150);
    }
    if (regs != null) {
        regs.setLocation(70, 0);
    //write them to xml
    }
    String dispos[] = {"300", "0"};
    String elfpos[] = {"0", "0"};
    String mempos[] = {"70", "150"};
    String regpos[] = {"70", "0"};
    Settings.get_instance().writeWindowPos("elfheader", elfpos);
    Settings.get_instance().writeWindowPos("registers", regpos);
    Settings.get_instance().writeWindowPos("disassembler", dispos);
    Settings.get_instance().writeWindowPos("memoryview", mempos);

}//GEN-LAST:event_WindowsPosActionPerformed

private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
    saveWinPos();
}//GEN-LAST:event_formWindowClosing

private void SettingsMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SettingsMenuActionPerformed
    setgui = new SettingsGUI();
    setgui.setLocation(200, 50);
    desktopPane.add(setgui);
    setgui.setVisible(true);
    try {
        setgui.setSelected(true);
    } catch (java.beans.PropertyVetoException e) {
    }

}//GEN-LAST:event_SettingsMenuActionPerformed
    private void saveWinPos() {

        if (dis != null) {
            if (dis.isVisible()) {
                Point location = dis.getLocation();
                String[] coord = new String[2];
                coord[0] = Integer.toString(location.x);
                coord[1] = Integer.toString(location.y);
                Settings.get_instance().writeWindowPos("disassembler", coord);
            }
        }
        if (elfinfo != null) {
            if (elfinfo.isVisible()) {
                Point location = elfinfo.getLocation();
                String[] coord = new String[2];
                coord[0] = Integer.toString(location.x);
                coord[1] = Integer.toString(location.y);
                Settings.get_instance().writeWindowPos("elfheader", coord);
            }
        }
        if (regs != null) {
            if (regs.isVisible()) {
                Point location = regs.getLocation();
                String[] coord = new String[2];
                coord[0] = Integer.toString(location.x);
                coord[1] = Integer.toString(location.y);
                Settings.get_instance().writeWindowPos("registers", coord);
            }
        }
        if (memview != null) {
            if (memview.isVisible()) {
                Point location = memview.getLocation();
                String[] coord = new String[2];
                coord[0] = Integer.toString(location.x);
                coord[1] = Integer.toString(location.y);
                Settings.get_instance().writeWindowPos("memoryview", coord);
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
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new JpcspMainGUI().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem Disasembler;
    private javax.swing.JMenuItem ElfInfo;
    private javax.swing.JMenu Emulation;
    private javax.swing.JMenuItem MemView;
    private javax.swing.JMenu Options;
    private javax.swing.JMenuItem Registers;
    private javax.swing.JMenuItem Reset;
    private javax.swing.JMenuItem Run;
    private javax.swing.JMenuItem SettingsMenu;
    private javax.swing.JMenuItem Stop;
    private javax.swing.JMenu Windows;
    private javax.swing.JMenuItem WindowsPos;
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JDesktopPane desktopPane;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem openMenuItem;
    // End of variables declaration//GEN-END:variables

    private boolean userChooseSomething(int returnVal) {
        return returnVal == JFileChooser.APPROVE_OPTION;
    }
}
