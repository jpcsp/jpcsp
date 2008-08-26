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
    
    /** Creates new form SettingsGUI */
    public SettingsGUI() {
        initComponents();
        boolean pbpunpack = Settings.get_instance().readBoolEmuoptions("pbpunpack");
        if(pbpunpack) pbpunpackcheck.setSelected(true);
        
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
    }
    
    private void loadKeys() {
        currentKeys = Settings.get_instance().loadKeys();
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
        
        // pressedKey allready mapped?
        if (k != null) {
            System.out.println("Key allready used");
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
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jButtonOK = new javax.swing.JButton();
        jButtonCancel = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        pbpunpackcheck = new javax.swing.JCheckBox();
        bg = new javax.swing.JPanel();
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

        pbpunpackcheck.setText("unpack pbp when loading");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addComponent(pbpunpackcheck, javax.swing.GroupLayout.PREFERRED_SIZE, 147, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(329, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(pbpunpackcheck)
                .addContainerGap(233, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("General", jPanel1);

        bg.setMinimumSize(new java.awt.Dimension(1, 1));
        bg.setLayout(new java.awt.GridBagLayout());

        fgPanel.setOpaque(false);

        fieldStart.setEditable(false);
        fieldStart.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldStart.setText("test");
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
        fieldHold.setText("L");
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

        javax.swing.GroupLayout fgPanelLayout = new javax.swing.GroupLayout(fgPanel);
        fgPanel.setLayout(fgPanelLayout);
        fgPanelLayout.setHorizontalGroup(
            fgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, fgPanelLayout.createSequentialGroup()
                .addGap(51, 51, 51)
                .addComponent(fieldHold, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(91, 91, 91)
                .addComponent(fieldVolPlus, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(fieldScreen, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 56, Short.MAX_VALUE)
                .addComponent(fieldCross, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(82, 82, 82))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, fgPanelLayout.createSequentialGroup()
                .addContainerGap(114, Short.MAX_VALUE)
                .addGroup(fgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(fgPanelLayout.createSequentialGroup()
                        .addComponent(fieldHome, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(fgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(fgPanelLayout.createSequentialGroup()
                                .addComponent(fieldVolMin, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(46, 46, 46)
                                .addComponent(fieldSelect, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(fieldMusic, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(6, 6, 6)
                        .addComponent(fieldStart, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(114, 114, 114))
                    .addGroup(fgPanelLayout.createSequentialGroup()
                        .addComponent(fieldTriangle, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(39, 39, 39))
                    .addGroup(fgPanelLayout.createSequentialGroup()
                        .addComponent(fieldRight, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(143, 143, 143)
                        .addComponent(fieldSquare, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(103, 103, 103))))
            .addGroup(fgPanelLayout.createSequentialGroup()
                .addGap(122, 122, 122)
                .addComponent(fieldUp, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(323, Short.MAX_VALUE))
            .addGroup(fgPanelLayout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addGroup(fgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(fgPanelLayout.createSequentialGroup()
                        .addComponent(fieldDown, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
                    .addGroup(fgPanelLayout.createSequentialGroup()
                        .addComponent(fieldLeft, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 341, Short.MAX_VALUE)
                        .addComponent(fieldCircle, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(30, 30, 30))))
            .addGroup(fgPanelLayout.createSequentialGroup()
                .addGap(61, 61, 61)
                .addComponent(fieldLTrigger, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 272, Short.MAX_VALUE)
                .addComponent(fieldRTrigger, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(59, 59, 59))
        );
        fgPanelLayout.setVerticalGroup(
            fgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(fgPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fieldUp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(fgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fieldSquare, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(fieldRight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(fgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(fgPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(fieldRTrigger, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(fieldTriangle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(fgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(fieldCircle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(fieldLeft, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(fieldDown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(fgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(fgPanelLayout.createSequentialGroup()
                                .addGap(30, 30, 30)
                                .addComponent(fieldCross, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(26, 26, 26)
                                .addComponent(fieldStart, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(fgPanelLayout.createSequentialGroup()
                                .addGap(47, 47, 47)
                                .addGroup(fgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(fieldHold, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(fieldVolPlus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(fieldScreen, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(fgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(fgPanelLayout.createSequentialGroup()
                                        .addGap(17, 17, 17)
                                        .addComponent(fieldHome, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(fgPanelLayout.createSequentialGroup()
                                        .addGap(25, 25, 25)
                                        .addComponent(fieldMusic, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                        .addGap(13, 13, 13))
                    .addGroup(fgPanelLayout.createSequentialGroup()
                        .addGap(3, 3, 3)
                        .addComponent(fieldLTrigger, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(145, 145, 145)
                        .addGroup(fgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(fieldSelect, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(fieldVolMin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(37, Short.MAX_VALUE))))
        );

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        bg.add(fgPanel, gridBagConstraints);

        bgLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        bgLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/images/controls.jpg"))); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        bg.add(bgLabel1, gridBagConstraints);

        jTabbedPane1.addTab("Controls", bg);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(301, Short.MAX_VALUE)
                .addComponent(jButtonOK, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButtonCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 503, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 302, Short.MAX_VALUE)
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
    boolean pbpunpack = Settings.get_instance().readBoolEmuoptions("pbpunpack");
   if(pbpunpack) pbpunpackcheck.setSelected(true);
}
private void jButtonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCancelActionPerformed
   dispose();
}//GEN-LAST:event_jButtonCancelActionPerformed

private void jButtonOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonOKActionPerformed
   Settings.get_instance().writeBoolEmuoptions("pbpunpack", pbpunpackcheck.isSelected());
   Settings.get_instance().writeKeys(currentKeys);
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

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel bg;
    private javax.swing.JLabel bgLabel1;
    private javax.swing.JPanel fgPanel;
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
    private javax.swing.JButton jButtonCancel;
    private javax.swing.JButton jButtonOK;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JCheckBox pbpunpackcheck;
    // End of variables declaration//GEN-END:variables

}
