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

package jpcsp.GUI;

import com.jidesoft.swing.FolderChooser;
import jpcsp.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.swing.JTextField;
import jpcsp.Controller.keyCode;

/**
 *
 * @author  shadow
 */
public class SettingsGUI extends javax.swing.JFrame implements KeyListener {

    private boolean getKey = false;
    private JTextField sender;
    private keyCode targetKey;
    private HashMap<Integer, keyCode> currentKeys;
    private HashMap<keyCode, Integer> revertKeys;  //kinda lame
    private MainGUI mainWindow = null;
    
    /** Creates new form SettingsGUI */
    public SettingsGUI() {
        initComponents();
        
        boolean enabled = Settings.getInstance().readBool("emu.pbpunpack");
        pbpunpackcheck.setSelected(enabled);
        
        enabled = Settings.getInstance().readBool("gui.saveWindowPos");
        saveWindowPosCheck.setSelected(enabled);
        
        enabled = Settings.getInstance().readBool("gui.openLogwindow");
        openLogwindowCheck.setSelected(enabled);
        
        enabled = Settings.getInstance().readBool("gui.snapLogwindow");
        snapConsoleCheck.setSelected(enabled);
        
        enabled = Settings.getInstance().readBool("emu.compiler");
        compilerCheck.setSelected(enabled);
        
        enabled = Settings.getInstance().readBool("emu.useshaders");
        shadersCheck.setSelected(enabled);
        
        enabled = Settings.getInstance().readBool("emu.ignoreInvalidMemoryAccess");
        invalidMemoryCheck.setSelected(enabled);
        
        enabled = Settings.getInstance().readBool("emu.debug.enablefilelogger");
        filelogCheck.setSelected(enabled);
        
        enabled = Settings.getInstance().readBool("emu.disablege");
        disableGECheck.setSelected(enabled);
        
        enabled = Settings.getInstance().readBool("emu.disablevbo");
        disableVBOCheck.setSelected(enabled);
        
        enabled = Settings.getInstance().readBool("emu.disablesceAudio");
        DisableSceAudioCheck.setSelected(enabled);
        
        enabled = Settings.getInstance().readBool("emu.ignoreaudiothreads");
        IgnoreAudioThreadsCheck.setSelected(enabled);
        
        enabled = Settings.getInstance().readBool("emu.mutesound");
        disableSoundCheck.setSelected(enabled);
        
        enabled = Settings.getInstance().readBool("emu.disableblockingaudio");
        disableBlockingAudioCheck.setSelected(enabled);
        
        enabled = Settings.getInstance().readBool("emu.umdbrowser");
        if(enabled)
            umdBrowser.setSelected(true);
        else
            ClassicOpenDialogumd.setSelected(true);
        
        umdpath.setText(Settings.getInstance().readString("emu.umdpath"));
        
        /* load current config and set the config */
        loadKeys();
        
        /* add key listerners to the text fields */
        fieldCircle.addKeyListener(this);
        fieldCross.addKeyListener(this);
        fieldDown.addKeyListener(this);
        fieldLTrigger.addKeyListener(this);
        fieldLeft.addKeyListener(this);
        fieldRTrigger.addKeyListener(this);
        fieldRight.addKeyListener(this);
        fieldSelect.addKeyListener(this);
        fieldSquare.addKeyListener(this);
        fieldStart.addKeyListener(this);
        fieldTriangle.addKeyListener(this);
        fieldUp.addKeyListener(this);
        fieldHome.addKeyListener(this);
        fieldScreen.addKeyListener(this);
        fieldMusic.addKeyListener(this);
        fieldVolPlus.addKeyListener(this);
        fieldVolMin.addKeyListener(this);
        fieldHold.addKeyListener(this);
        fieldAnalogUp.addKeyListener(this);
        fieldAnalogDown.addKeyListener(this);
        fieldAnalogLeft.addKeyListener(this);
        fieldAnalogRight.addKeyListener(this);
    }
    
    private void loadKeys() {
        currentKeys = Settings.getInstance().loadKeys();
        revertKeys = new HashMap<keyCode, Integer>(22);
        
        Iterator iter = currentKeys.entrySet().iterator();
        while(iter.hasNext()) {
            Map.Entry<Integer, keyCode> entry = (Map.Entry)iter.next();
            keyCode key = (keyCode)entry.getValue();
            int value = (Integer)entry.getKey();
            
            revertKeys.put(key, value);
            
            switch (key) {
                case DOWN:      fieldDown.setText(KeyEvent.getKeyText(value)); break;
                case UP:        fieldUp.setText(KeyEvent.getKeyText(value)); break;
                case LEFT:      fieldLeft.setText(KeyEvent.getKeyText(value)); break;
                case RIGHT:     fieldRight.setText(KeyEvent.getKeyText(value)); break;
                case ANDOWN:    fieldAnalogDown.setText(KeyEvent.getKeyText(value)); break;
                case ANUP:      fieldAnalogUp.setText(KeyEvent.getKeyText(value)); break;
                case ANLEFT:    fieldAnalogLeft.setText(KeyEvent.getKeyText(value)); break;
                case ANRIGHT:   fieldAnalogRight.setText(KeyEvent.getKeyText(value)); break;
            
                case TRIANGLE:  fieldTriangle.setText(KeyEvent.getKeyText(value)); break;
                case SQUARE:    fieldSquare.setText(KeyEvent.getKeyText(value)); break;
                case CIRCLE:    fieldCircle.setText(KeyEvent.getKeyText(value)); break;
                case CROSS:     fieldCross.setText(KeyEvent.getKeyText(value)); break;
                case L1:        fieldLTrigger.setText(KeyEvent.getKeyText(value)); break;
                case R1:        fieldRTrigger.setText(KeyEvent.getKeyText(value)); break;
                case START:     fieldStart.setText(KeyEvent.getKeyText(value)); break;
                case SELECT:    fieldSelect.setText(KeyEvent.getKeyText(value)); break;
                
                case HOME:      fieldHome.setText(KeyEvent.getKeyText(value)); break;
                case HOLD:      fieldHold.setText(KeyEvent.getKeyText(value)); break;
                case VOLMIN:    fieldVolMin.setText(KeyEvent.getKeyText(value)); break;
                case VOLPLUS:   fieldVolPlus.setText(KeyEvent.getKeyText(value)); break;
                case SCREEN:    fieldScreen.setText(KeyEvent.getKeyText(value)); break;
                case MUSIC:     fieldMusic.setText(KeyEvent.getKeyText(value)); break;
                        
                default: break;
            }
        }
    }
    
    @Override
    public void keyTyped(KeyEvent arg0) { }
    
    @Override
    public void keyReleased(KeyEvent arg0) { }

    @Override
    public void keyPressed(KeyEvent arg0) { 
        if (!getKey) {
            return;
        }
        getKey = false;
        
        int pressedKey = arg0.getKeyCode();
        keyCode k = currentKeys.get(pressedKey);
        
        // pressedKey already mapped?
        if (k != null) {
            Emulator.log.warn("Key already used for " + k);
            this.sender.setText(KeyEvent.getKeyText(revertKeys.get(this.targetKey)));
            return;
        }
        
        //Remove old key
        int oldMapping = revertKeys.get(this.targetKey);
        revertKeys.remove(this.targetKey);
        currentKeys.remove(oldMapping);
        
        //Add new mapping
        currentKeys.put(pressedKey, this.targetKey);
        revertKeys.put(this.targetKey, pressedKey);
        sender.setText(KeyEvent.getKeyText(pressedKey));
    }

    private void setKey(JTextField sender, keyCode targetKey) {
        if (getKey) {
            this.sender.setText(KeyEvent.getKeyText(revertKeys.get(this.targetKey)));
        }
        sender.setText("PressKey");
        getKey = true;
        
        this.sender = sender;
        this.targetKey = targetKey;
    }

    public void setMainGUI(MainGUI mainWindow) {
        this.mainWindow = mainWindow;
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        buttonGroup1 = new javax.swing.ButtonGroup();
        jButtonOK = new javax.swing.JButton();
        jButtonCancel = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        generalPanel = new javax.swing.JPanel();
        pbpunpackcheck = new javax.swing.JCheckBox();
        saveWindowPosCheck = new javax.swing.JCheckBox();
        openLogwindowCheck = new javax.swing.JCheckBox();
        snapConsoleCheck = new javax.swing.JCheckBox();
        compilerCheck = new javax.swing.JCheckBox();
        umdBrowser = new javax.swing.JRadioButton();
        ClassicOpenDialogumd = new javax.swing.JRadioButton();
        umdpath = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        shadersCheck = new javax.swing.JCheckBox();
        invalidMemoryCheck = new javax.swing.JCheckBox();
        filelogCheck = new javax.swing.JCheckBox();
        compatibilityPanel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        disableSoundCheck = new javax.swing.JCheckBox();
        IgnoreAudioThreadsCheck = new javax.swing.JCheckBox();
        DisableSceAudioCheck = new javax.swing.JCheckBox();
        disableBlockingAudioCheck = new javax.swing.JCheckBox();
        jPanel2 = new javax.swing.JPanel();
        disableGECheck = new javax.swing.JCheckBox();
        disableVBOCheck = new javax.swing.JCheckBox();
        keyPanel = new javax.swing.JPanel();
        fgPanel = new javax.swing.JPanel();
        fieldStart = new javax.swing.JTextField();
        fieldSelect = new javax.swing.JTextField();
        fieldCross = new javax.swing.JTextField();
        fieldCircle = new javax.swing.JTextField();
        fieldTriangle = new javax.swing.JTextField();
        fieldSquare = new javax.swing.JTextField();
        fieldRight = new javax.swing.JTextField();
        fieldUp = new javax.swing.JTextField();
        fieldLeft = new javax.swing.JTextField();
        fieldDown = new javax.swing.JTextField();
        fieldHold = new javax.swing.JTextField();
        fieldHome = new javax.swing.JTextField();
        fieldVolMin = new javax.swing.JTextField();
        fieldVolPlus = new javax.swing.JTextField();
        fieldLTrigger = new javax.swing.JTextField();
        fieldRTrigger = new javax.swing.JTextField();
        fieldScreen = new javax.swing.JTextField();
        fieldMusic = new javax.swing.JTextField();
        fieldAnalogUp = new javax.swing.JTextField();
        fieldAnalogDown = new javax.swing.JTextField();
        fieldAnalogLeft = new javax.swing.JTextField();
        fieldAnalogRight = new javax.swing.JTextField();
        bgLabel1 = new javax.swing.JLabel();

        setTitle("Settings");
        setResizable(false);

        jButtonOK.setText("OK");
        jButtonOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOKActionPerformed(evt);
            }
        });

        jButtonCancel.setText("Cancel");
        jButtonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCancelActionPerformed(evt);
            }
        });

        pbpunpackcheck.setText("Unpack PBP when loading");

        saveWindowPosCheck.setText("Save window positions on exit");

        openLogwindowCheck.setText("Open console on start-up");

        snapConsoleCheck.setText("Snap console to main window");

        compilerCheck.setText("Use compiler (dynarec)");

        buttonGroup1.add(umdBrowser);
        umdBrowser.setText("Use UMD Browser");

        buttonGroup1.add(ClassicOpenDialogumd);
        ClassicOpenDialogumd.setText("Use Classic Open Dialog for UMD");

        umdpath.setEditable(false);

        jLabel1.setText("UMD path folder:");

        jButton1.setText("...");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        shadersCheck.setText("Use shaders (highly experimental)");

        invalidMemoryCheck.setText("Ignore Invalid Memory access (not recommended)");

        filelogCheck.setText("Enable file IO logging");

        javax.swing.GroupLayout generalPanelLayout = new javax.swing.GroupLayout(generalPanel);
        generalPanel.setLayout(generalPanelLayout);
        generalPanelLayout.setHorizontalGroup(
            generalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(generalPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(generalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(generalPanelLayout.createSequentialGroup()
                        .addComponent(filelogCheck)
                        .addContainerGap())
                    .addGroup(generalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(generalPanelLayout.createSequentialGroup()
                            .addComponent(shadersCheck)
                            .addContainerGap())
                        .addGroup(generalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(generalPanelLayout.createSequentialGroup()
                                .addGroup(generalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(umdBrowser)
                                    .addComponent(ClassicOpenDialogumd))
                                .addContainerGap())
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, generalPanelLayout.createSequentialGroup()
                                .addGroup(generalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(pbpunpackcheck, javax.swing.GroupLayout.DEFAULT_SIZE, 384, Short.MAX_VALUE)
                                    .addComponent(saveWindowPosCheck, javax.swing.GroupLayout.DEFAULT_SIZE, 384, Short.MAX_VALUE)
                                    .addComponent(openLogwindowCheck, javax.swing.GroupLayout.DEFAULT_SIZE, 384, Short.MAX_VALUE)
                                    .addComponent(snapConsoleCheck, javax.swing.GroupLayout.DEFAULT_SIZE, 384, Short.MAX_VALUE)
                                    .addComponent(compilerCheck, javax.swing.GroupLayout.DEFAULT_SIZE, 384, Short.MAX_VALUE)
                                    .addComponent(invalidMemoryCheck, javax.swing.GroupLayout.DEFAULT_SIZE, 384, Short.MAX_VALUE))
                                .addGap(216, 216, 216))
                            .addGroup(generalPanelLayout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(umdpath, javax.swing.GroupLayout.PREFERRED_SIZE, 175, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(310, Short.MAX_VALUE))))))
        );
        generalPanelLayout.setVerticalGroup(
            generalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(generalPanelLayout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(pbpunpackcheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(saveWindowPosCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(openLogwindowCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(snapConsoleCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(compilerCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(shadersCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(invalidMemoryCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(filelogCheck)
                .addGap(6, 6, 6)
                .addComponent(umdBrowser)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ClassicOpenDialogumd)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(generalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(umdpath, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addContainerGap(55, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("General", generalPanel);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Audio"));

        disableSoundCheck.setText("Disable audio (mute)");

        IgnoreAudioThreadsCheck.setText("Disable audio threads");

        DisableSceAudioCheck.setText("Disable audio channels (not recommended)");

        disableBlockingAudioCheck.setText("Disable audio blocking");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(disableSoundCheck)
                    .addComponent(IgnoreAudioThreadsCheck)
                    .addComponent(DisableSceAudioCheck)
                    .addComponent(disableBlockingAudioCheck))
                .addContainerGap(40, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(disableSoundCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(IgnoreAudioThreadsCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(DisableSceAudioCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(disableBlockingAudioCheck)
                .addContainerGap(167, Short.MAX_VALUE))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Video"));

        disableGECheck.setText("Disable GE (not recommended)");

        disableVBOCheck.setText("Disable VBO (Automatically disabled if not supported)");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(disableGECheck)
            .addComponent(disableVBOCheck)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(disableGECheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(disableVBOCheck)
                .addContainerGap(213, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout compatibilityPanelLayout = new javax.swing.GroupLayout(compatibilityPanel);
        compatibilityPanel.setLayout(compatibilityPanelLayout);
        compatibilityPanelLayout.setHorizontalGroup(
            compatibilityPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, compatibilityPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, 297, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        compatibilityPanelLayout.setVerticalGroup(
            compatibilityPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, compatibilityPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(compatibilityPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Compatibility", compatibilityPanel);

        keyPanel.setMinimumSize(new java.awt.Dimension(1, 1));
        keyPanel.setLayout(new java.awt.GridBagLayout());

        fgPanel.setOpaque(false);

        fieldStart.setEditable(false);
        fieldStart.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldStart.setText("Enter");
        fieldStart.setToolTipText("select and press desired key");
        fieldStart.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 2, true));
        fieldStart.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldStartMouseClicked(evt);
            }
        });

        fieldSelect.setEditable(false);
        fieldSelect.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldSelect.setText("Space");
        fieldSelect.setToolTipText("select and press desired key");
        fieldSelect.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 2, true));
        fieldSelect.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldSelectMouseClicked(evt);
            }
        });

        fieldCross.setEditable(false);
        fieldCross.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldCross.setText("S");
        fieldCross.setToolTipText("select and press desired key");
        fieldCross.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 2, true));
        fieldCross.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldCrossMouseClicked(evt);
            }
        });

        fieldCircle.setEditable(false);
        fieldCircle.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldCircle.setText("D");
        fieldCircle.setToolTipText("select and press desired key");
        fieldCircle.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 2, true));
        fieldCircle.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldCircleMouseClicked(evt);
            }
        });

        fieldTriangle.setEditable(false);
        fieldTriangle.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldTriangle.setText("W");
        fieldTriangle.setToolTipText("select and press desired key");
        fieldTriangle.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 2, true));
        fieldTriangle.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldTriangleMouseClicked(evt);
            }
        });

        fieldSquare.setEditable(false);
        fieldSquare.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldSquare.setText("A");
        fieldSquare.setToolTipText("select and press desired key");
        fieldSquare.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 2, true));
        fieldSquare.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldSquareMouseClicked(evt);
            }
        });

        fieldRight.setEditable(false);
        fieldRight.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldRight.setText("Right");
        fieldRight.setToolTipText("select and press desired key");
        fieldRight.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 2, true));
        fieldRight.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldRightMouseClicked(evt);
            }
        });

        fieldUp.setEditable(false);
        fieldUp.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldUp.setText("Up");
        fieldUp.setToolTipText("select and press desired key");
        fieldUp.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 2, true));
        fieldUp.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldUpMouseClicked(evt);
            }
        });

        fieldLeft.setEditable(false);
        fieldLeft.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldLeft.setText("Left");
        fieldLeft.setToolTipText("select and press desired key");
        fieldLeft.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 2, true));
        fieldLeft.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldLeftMouseClicked(evt);
            }
        });

        fieldDown.setEditable(false);
        fieldDown.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldDown.setText("Down");
        fieldDown.setToolTipText("select and press desired key");
        fieldDown.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 2, true));
        fieldDown.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldDownMouseClicked(evt);
            }
        });

        fieldHold.setEditable(false);
        fieldHold.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldHold.setText("O");
        fieldHold.setToolTipText("select and press desired key");
        fieldHold.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 2, true));
        fieldHold.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldHoldMouseClicked(evt);
            }
        });

        fieldHome.setEditable(false);
        fieldHome.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldHome.setText("H");
        fieldHome.setToolTipText("select and press desired key");
        fieldHome.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 2, true));
        fieldHome.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldHomeMouseClicked(evt);
            }
        });

        fieldVolMin.setEditable(false);
        fieldVolMin.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldVolMin.setText("-");
        fieldVolMin.setToolTipText("select and press desired key");
        fieldVolMin.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 2, true));
        fieldVolMin.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldVolMinMouseClicked(evt);
            }
        });

        fieldVolPlus.setEditable(false);
        fieldVolPlus.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldVolPlus.setText("+");
        fieldVolPlus.setToolTipText("select and press desired key");
        fieldVolPlus.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 2, true));
        fieldVolPlus.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldVolPlusMouseClicked(evt);
            }
        });

        fieldLTrigger.setEditable(false);
        fieldLTrigger.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldLTrigger.setText("Q");
        fieldLTrigger.setToolTipText("select and press desired key");
        fieldLTrigger.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 2, true));
        fieldLTrigger.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldLTriggerMouseClicked(evt);
            }
        });

        fieldRTrigger.setEditable(false);
        fieldRTrigger.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldRTrigger.setText("E");
        fieldRTrigger.setToolTipText("select and press desired key");
        fieldRTrigger.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 2, true));
        fieldRTrigger.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldRTriggerMouseClicked(evt);
            }
        });

        fieldScreen.setEditable(false);
        fieldScreen.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldScreen.setText("S");
        fieldScreen.setToolTipText("select and press desired key");
        fieldScreen.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 2, true));
        fieldScreen.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldScreenMouseClicked(evt);
            }
        });

        fieldMusic.setEditable(false);
        fieldMusic.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldMusic.setText("N");
        fieldMusic.setToolTipText("select and press desired key");
        fieldMusic.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 2, true));
        fieldMusic.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldMusicMouseClicked(evt);
            }
        });

        fieldAnalogUp.setEditable(false);
        fieldAnalogUp.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldAnalogUp.setText("I");
        fieldAnalogUp.setToolTipText("select and press desired key");
        fieldAnalogUp.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 2, true));
        fieldAnalogUp.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldAnalogUpMouseClicked(evt);
            }
        });

        fieldAnalogDown.setEditable(false);
        fieldAnalogDown.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldAnalogDown.setText("K");
        fieldAnalogDown.setToolTipText("select and press desired key");
        fieldAnalogDown.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 2, true));
        fieldAnalogDown.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldAnalogDownMouseClicked(evt);
            }
        });

        fieldAnalogLeft.setEditable(false);
        fieldAnalogLeft.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldAnalogLeft.setText("J");
        fieldAnalogLeft.setToolTipText("select and press desired key");
        fieldAnalogLeft.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 2, true));
        fieldAnalogLeft.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldAnalogLeftMouseClicked(evt);
            }
        });

        fieldAnalogRight.setEditable(false);
        fieldAnalogRight.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldAnalogRight.setText("L");
        fieldAnalogRight.setToolTipText("select and press desired key");
        fieldAnalogRight.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 2, true));
        fieldAnalogRight.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldAnalogRightMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout fgPanelLayout = new javax.swing.GroupLayout(fgPanel);
        fgPanel.setLayout(fgPanelLayout);
        fgPanelLayout.setHorizontalGroup(
            fgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, fgPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fieldDown, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 484, Short.MAX_VALUE)
                .addComponent(fieldCross, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addGroup(fgPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fieldLTrigger, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 484, Short.MAX_VALUE)
                .addComponent(fieldRTrigger, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, fgPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fieldLeft, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 484, Short.MAX_VALUE)
                .addComponent(fieldCircle, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addGroup(fgPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fieldRight, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 484, Short.MAX_VALUE)
                .addComponent(fieldSquare, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, fgPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fieldUp, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 484, Short.MAX_VALUE)
                .addComponent(fieldTriangle, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, fgPanelLayout.createSequentialGroup()
                .addContainerGap(547, Short.MAX_VALUE)
                .addComponent(fieldHold, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, fgPanelLayout.createSequentialGroup()
                .addGroup(fgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(fgPanelLayout.createSequentialGroup()
                        .addGap(29, 29, 29)
                        .addComponent(fieldAnalogUp, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 71, Short.MAX_VALUE)
                        .addComponent(fieldHome, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(fieldVolPlus, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(48, 48, 48))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, fgPanelLayout.createSequentialGroup()
                        .addComponent(fieldAnalogLeft, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(fieldAnalogRight, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(76, 76, 76)
                        .addComponent(fieldVolMin, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                .addGroup(fgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(fgPanelLayout.createSequentialGroup()
                        .addGap(45, 45, 45)
                        .addComponent(fieldMusic, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addGroup(fgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(fgPanelLayout.createSequentialGroup()
                                .addGap(81, 81, 81)
                                .addComponent(fieldStart, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(fgPanelLayout.createSequentialGroup()
                                .addGap(38, 38, 38)
                                .addComponent(fieldSelect, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(fgPanelLayout.createSequentialGroup()
                        .addGap(11, 11, 11)
                        .addComponent(fieldScreen, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(43, 43, 43))
            .addGroup(fgPanelLayout.createSequentialGroup()
                .addGap(29, 29, 29)
                .addComponent(fieldAnalogDown, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(528, Short.MAX_VALUE))
        );
        fgPanelLayout.setVerticalGroup(
            fgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(fgPanelLayout.createSequentialGroup()
                .addGap(70, 70, 70)
                .addComponent(fieldTriangle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(41, 41, 41)
                .addComponent(fieldCross, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 86, Short.MAX_VALUE)
                .addComponent(fieldMusic, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(fieldScreen, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(36, 36, 36))
            .addGroup(fgPanelLayout.createSequentialGroup()
                .addGroup(fgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(fgPanelLayout.createSequentialGroup()
                        .addGap(41, 41, 41)
                        .addComponent(fieldRight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(fgPanelLayout.createSequentialGroup()
                        .addGroup(fgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(fieldLTrigger, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(fieldRTrigger, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(fieldSquare, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                .addGap(9, 9, 9)
                .addComponent(fieldUp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(11, 11, 11)
                .addGroup(fgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fieldCircle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(fieldLeft, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(24, 24, 24)
                .addGroup(fgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(fgPanelLayout.createSequentialGroup()
                        .addGap(95, 95, 95)
                        .addGroup(fgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(fieldAnalogUp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(fieldVolPlus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(fieldHome, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(fgPanelLayout.createSequentialGroup()
                        .addGap(29, 29, 29)
                        .addComponent(fieldHold, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(fieldStart, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(fieldSelect, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(fgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(fgPanelLayout.createSequentialGroup()
                        .addGroup(fgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(fieldAnalogRight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(fieldAnalogLeft, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(fieldAnalogDown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(fieldVolMin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(fgPanelLayout.createSequentialGroup()
                .addGap(126, 126, 126)
                .addComponent(fieldDown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(167, Short.MAX_VALUE))
        );

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        keyPanel.add(fgPanel, gridBagConstraints);

        bgLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        bgLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/images/controls.jpg"))); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        keyPanel.add(bgLabel1, gridBagConstraints);

        jTabbedPane1.addTab("Controls", keyPanel);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jButtonOK, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButtonCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jTabbedPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 615, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 339, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonCancel)
                    .addComponent(jButtonOK))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
public void RefreshWindow()
{
    /* (not needed?)
    boolean pbpunpack = Settings.getInstance().readBool("emu.pbpunpack");
    if (pbpunpack) pbpunpackcheck.setSelected(true);
    */
}
private void jButtonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCancelActionPerformed
   dispose();
}//GEN-LAST:event_jButtonCancelActionPerformed

private void jButtonOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonOKActionPerformed
   Settings.getInstance().writeBool("emu.pbpunpack", pbpunpackcheck.isSelected());
   Settings.getInstance().writeBool("gui.saveWindowPos", saveWindowPosCheck.isSelected());
   Settings.getInstance().writeBool("gui.openLogwindow", openLogwindowCheck.isSelected());
   Settings.getInstance().writeBool("gui.snapLogwindow", snapConsoleCheck.isSelected());
   Settings.getInstance().writeBool("emu.compiler", compilerCheck.isSelected());
   Settings.getInstance().writeBool("emu.useshaders", shadersCheck.isSelected());
   Settings.getInstance().writeBool("emu.ignoreInvalidMemoryAccess", invalidMemoryCheck.isSelected());
   Settings.getInstance().writeBool("emu.debug.enablefilelogger", filelogCheck.isSelected());
   Settings.getInstance().writeBool("emu.disablege", disableGECheck.isSelected());
   Settings.getInstance().writeBool("emu.disablevbo", disableVBOCheck.isSelected());
   Settings.getInstance().writeBool("emu.disablesceAudio", DisableSceAudioCheck.isSelected());
   Settings.getInstance().writeBool("emu.ignoreaudiothreads",IgnoreAudioThreadsCheck.isSelected());
   Settings.getInstance().writeBool("emu.mutesound",disableSoundCheck.isSelected());
   Settings.getInstance().writeBool("emu.disableblockingaudio",disableBlockingAudioCheck.isSelected());
   
   if(umdBrowser.isSelected())
      Settings.getInstance().writeBool("emu.umdbrowser", true);
   else
      Settings.getInstance().writeBool("emu.umdbrowser", false);
   Settings.getInstance().writeString("emu.umdpath", umdpath.getText());
   Settings.getInstance().writeKeys(currentKeys);
   
   State.controller.loadKeyConfig(currentKeys);
   if (snapConsoleCheck.isSelected() && mainWindow != null)
       mainWindow.snaptoMainwindow();
   dispose();
}//GEN-LAST:event_jButtonOKActionPerformed

private void fieldStartMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldStartMouseClicked
    setKey(fieldStart, keyCode.START);
}//GEN-LAST:event_fieldStartMouseClicked

private void fieldSelectMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldSelectMouseClicked
    setKey(fieldSelect, keyCode.SELECT);
}//GEN-LAST:event_fieldSelectMouseClicked

private void fieldCrossMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldCrossMouseClicked
    setKey(fieldCross, keyCode.CROSS);
}//GEN-LAST:event_fieldCrossMouseClicked

private void fieldCircleMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldCircleMouseClicked
    setKey(fieldCircle, keyCode.CIRCLE);
}//GEN-LAST:event_fieldCircleMouseClicked

private void fieldTriangleMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldTriangleMouseClicked
    setKey(fieldTriangle, keyCode.TRIANGLE);
}//GEN-LAST:event_fieldTriangleMouseClicked

private void fieldSquareMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldSquareMouseClicked
    setKey(fieldSquare, keyCode.SQUARE);
}//GEN-LAST:event_fieldSquareMouseClicked

private void fieldRightMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldRightMouseClicked
    setKey(fieldRight, keyCode.RIGHT);
}//GEN-LAST:event_fieldRightMouseClicked

private void fieldUpMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldUpMouseClicked
    setKey(fieldUp, keyCode.UP);
}//GEN-LAST:event_fieldUpMouseClicked

private void fieldLeftMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldLeftMouseClicked
    setKey(fieldLeft, keyCode.LEFT);
}//GEN-LAST:event_fieldLeftMouseClicked

private void fieldDownMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldDownMouseClicked
    setKey(fieldDown, keyCode.DOWN);
}//GEN-LAST:event_fieldDownMouseClicked

private void fieldHoldMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldHoldMouseClicked
    setKey(fieldHold, keyCode.HOLD);
}//GEN-LAST:event_fieldHoldMouseClicked

private void fieldHomeMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldHomeMouseClicked
    setKey(fieldHome, keyCode.HOME);
}//GEN-LAST:event_fieldHomeMouseClicked

private void fieldVolMinMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldVolMinMouseClicked
    setKey(fieldVolMin, keyCode.VOLMIN);
}//GEN-LAST:event_fieldVolMinMouseClicked

private void fieldVolPlusMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldVolPlusMouseClicked
    setKey(fieldVolPlus, keyCode.VOLPLUS);
}//GEN-LAST:event_fieldVolPlusMouseClicked

private void fieldLTriggerMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldLTriggerMouseClicked
    setKey(fieldLTrigger, keyCode.L1);
}//GEN-LAST:event_fieldLTriggerMouseClicked

private void fieldRTriggerMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldRTriggerMouseClicked
    setKey(fieldRTrigger, keyCode.R1);
}//GEN-LAST:event_fieldRTriggerMouseClicked

private void fieldScreenMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldScreenMouseClicked
    setKey(fieldScreen, keyCode.SCREEN);
}//GEN-LAST:event_fieldScreenMouseClicked

private void fieldMusicMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldMusicMouseClicked
    setKey(fieldMusic, keyCode.MUSIC);
}//GEN-LAST:event_fieldMusicMouseClicked

private void fieldAnalogDownMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldAnalogDownMouseClicked
    setKey(fieldAnalogDown, keyCode.ANDOWN);
}//GEN-LAST:event_fieldAnalogDownMouseClicked

private void fieldAnalogUpMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldAnalogUpMouseClicked
    setKey(fieldAnalogUp, keyCode.ANUP);
}//GEN-LAST:event_fieldAnalogUpMouseClicked

private void fieldAnalogRightMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldAnalogRightMouseClicked
    setKey(fieldAnalogRight, keyCode.ANRIGHT);
}//GEN-LAST:event_fieldAnalogRightMouseClicked

private void fieldAnalogLeftMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldAnalogLeftMouseClicked
    setKey(fieldAnalogLeft, keyCode.ANLEFT);
}//GEN-LAST:event_fieldAnalogLeftMouseClicked

private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
  FolderChooser folderChooser = new FolderChooser("select folder");
  int result = folderChooser.showSaveDialog(jButton1.getTopLevelAncestor());
  if (result == FolderChooser.APPROVE_OPTION) {
       umdpath.setText(folderChooser.getSelectedFile().getPath());
  }
}//GEN-LAST:event_jButton1ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JRadioButton ClassicOpenDialogumd;
    private javax.swing.JCheckBox DisableSceAudioCheck;
    private javax.swing.JCheckBox IgnoreAudioThreadsCheck;
    private javax.swing.JLabel bgLabel1;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JPanel compatibilityPanel;
    private javax.swing.JCheckBox compilerCheck;
    private javax.swing.JCheckBox disableBlockingAudioCheck;
    private javax.swing.JCheckBox disableGECheck;
    private javax.swing.JCheckBox disableSoundCheck;
    private javax.swing.JCheckBox disableVBOCheck;
    private javax.swing.JPanel fgPanel;
    private javax.swing.JTextField fieldAnalogDown;
    private javax.swing.JTextField fieldAnalogLeft;
    private javax.swing.JTextField fieldAnalogRight;
    private javax.swing.JTextField fieldAnalogUp;
    private javax.swing.JTextField fieldCircle;
    private javax.swing.JTextField fieldCross;
    private javax.swing.JTextField fieldDown;
    private javax.swing.JTextField fieldHold;
    private javax.swing.JTextField fieldHome;
    private javax.swing.JTextField fieldLTrigger;
    private javax.swing.JTextField fieldLeft;
    private javax.swing.JTextField fieldMusic;
    private javax.swing.JTextField fieldRTrigger;
    private javax.swing.JTextField fieldRight;
    private javax.swing.JTextField fieldScreen;
    private javax.swing.JTextField fieldSelect;
    private javax.swing.JTextField fieldSquare;
    private javax.swing.JTextField fieldStart;
    private javax.swing.JTextField fieldTriangle;
    private javax.swing.JTextField fieldUp;
    private javax.swing.JTextField fieldVolMin;
    private javax.swing.JTextField fieldVolPlus;
    private javax.swing.JCheckBox filelogCheck;
    private javax.swing.JPanel generalPanel;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButtonCancel;
    private javax.swing.JButton jButtonOK;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JPanel keyPanel;
    private javax.swing.JCheckBox openLogwindowCheck;
    private javax.swing.JCheckBox pbpunpackcheck;
    private javax.swing.JCheckBox saveWindowPosCheck;
    private javax.swing.JCheckBox shadersCheck;
    private javax.swing.JCheckBox invalidMemoryCheck;
    private javax.swing.JCheckBox snapConsoleCheck;
    private javax.swing.JRadioButton umdBrowser;
    private javax.swing.JTextField umdpath;
    // End of variables declaration//GEN-END:variables

}
