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

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.EnumMap;
import java.util.Map;

import javax.swing.JTextField;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.MutableComboBoxModel;

import jpcsp.Emulator;
import jpcsp.State;
import jpcsp.Controller.keyCode;
import jpcsp.WindowPropSaver;
import jpcsp.settings.Settings;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Event;
import net.java.games.input.EventQueue;
import net.java.games.input.Component.Identifier;
import net.java.games.input.Component.Identifier.Axis;
import net.java.games.input.Component.Identifier.Button;

public class ControlsGUI extends javax.swing.JFrame implements KeyListener {

    private static final long serialVersionUID = -732715495873159718L;
    public static final String identifierForConfig = "controlsGUI";
    private boolean getKey = false;
    private JTextField sender;
    private keyCode targetKey;
    private Map<Integer, keyCode> currentKeys;
    private Map<keyCode, Integer> revertKeys;
    private Map<keyCode, String> currentController;
    private ControllerPollThread controllerPollThread;
    private static final int maxControllerFieldValueLength = 9;

    private class ControllerPollThread extends Thread {

        volatile protected boolean exit = false;

        @Override
        public void run() {
            while (!exit) {
                Controller controller = getSelectedController();
                if (controller != null && controller.poll()) {
                    EventQueue eventQueue = controller.getEventQueue();
                    Event event = new Event();
                    while (eventQueue.getNextEvent(event)) {
                        onControllerEvent(event);
                    }
                }

                // Wait a little bit before polling again...
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    // Ignore exception
                }
            }
        }
    }

    public ControlsGUI() {
        initComponents();
        loadKeys();

        Controller controller = jpcsp.Controller.getInstance().getInputController();
        if (controller != null) {
            for (int i = 0; i < controllerBox.getItemCount(); i++) {
                if (controller == controllerBox.getItemAt(i)) {
                    controllerBox.setSelectedIndex(i);
                    break;
                }
            }
        }
        setFields();

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

        controllerBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                onControllerChange();
            }
        });

        controllerPollThread = new ControllerPollThread();
        controllerPollThread.setName("Controller Poll Thread");
        controllerPollThread.setDaemon(true);
        controllerPollThread.start();

        WindowPropSaver.loadWindowProperties(this);
    }

    @Override
    public void dispose() {
        if (controllerPollThread != null) {
            controllerPollThread.exit = true;
        }
        Emulator.getMainGUI().endWindowDialog();
        super.dispose();
    }

    private void onControllerChange() {
        setFields();
    }

    private Controller getSelectedController() {
        if (controllerBox != null) {
            int controllerIndex = controllerBox.getSelectedIndex();
            ControllerEnvironment ce = ControllerEnvironment.getDefaultEnvironment();
            Controller[] controllers = ce.getControllers();
            if (controllers != null && controllerIndex >= 0 && controllerIndex < controllers.length) {
                return controllers[controllerIndex];
            }
        }

        return null;
    }

    private void loadKeys() {
        currentKeys = Settings.getInstance().loadKeys();
        revertKeys = new EnumMap<keyCode, Integer>(keyCode.class);

        for (Map.Entry<Integer, keyCode> entry : currentKeys.entrySet()) {
            revertKeys.put(entry.getValue(), entry.getKey());
        }

        currentController = Settings.getInstance().loadController();
    }

    private void setFieldValue(keyCode key, String value) {
        switch (key) {
            case DOWN:
                fieldDown.setText(value);
                break;
            case UP:
                fieldUp.setText(value);
                break;
            case LEFT:
                fieldLeft.setText(value);
                break;
            case RIGHT:
                fieldRight.setText(value);
                break;
            case LANDOWN:
                fieldAnalogDown.setText(value);
                break;
            case LANUP:
                fieldAnalogUp.setText(value);
                break;
            case LANLEFT:
                fieldAnalogLeft.setText(value);
                break;
            case LANRIGHT:
                fieldAnalogRight.setText(value);
                break;

            case TRIANGLE:
                fieldTriangle.setText(value);
                break;
            case SQUARE:
                fieldSquare.setText(value);
                break;
            case CIRCLE:
                fieldCircle.setText(value);
                break;
            case CROSS:
                fieldCross.setText(value);
                break;
            case L1:
                fieldLTrigger.setText(value);
                break;
            case R1:
                fieldRTrigger.setText(value);
                break;
            case START:
                fieldStart.setText(value);
                break;
            case SELECT:
                fieldSelect.setText(value);
                break;

            case HOME:
                fieldHome.setText(value);
                break;
            case HOLD:
                fieldHold.setText(value);
                break;
            case VOLMIN:
                fieldVolMin.setText(value);
                break;
            case VOLPLUS:
                fieldVolPlus.setText(value);
                break;
            case SCREEN:
                fieldScreen.setText(value);
                break;
            case MUSIC:
                fieldMusic.setText(value);
                break;
            case RELEASED:
                break;
        }
    }

    private void setFields() {
        if (jpcsp.Controller.isKeyboardController(getSelectedController())) {
            for (Map.Entry<Integer, keyCode> entry : currentKeys.entrySet()) {
                setFieldValue(entry.getValue(), KeyEvent.getKeyText(entry.getKey()));
            }
        } else {
            for (Map.Entry<keyCode, String> entry : currentController.entrySet()) {
                String identifierName = entry.getValue();
                setFieldValue(entry.getKey(), getControllerFieldText(identifierName));
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent arg0) {
    }

    @Override
    public void keyReleased(KeyEvent arg0) {
    }

    @Override
    public void keyPressed(KeyEvent arg0) {
        if (!getKey) {
            return;
        }
        getKey = false;

        int pressedKey = arg0.getKeyCode();
        keyCode k = currentKeys.get(pressedKey);

        if (k != null) {
            Emulator.log.warn("Key already used for " + k);
            sender.setText(KeyEvent.getKeyText(revertKeys.get(targetKey)));
            return;
        }

        int oldMapping = revertKeys.get(targetKey);
        revertKeys.remove(targetKey);
        currentKeys.remove(oldMapping);

        currentKeys.put(pressedKey, targetKey);
        revertKeys.put(targetKey, pressedKey);
        sender.setText(KeyEvent.getKeyText(pressedKey));

        getKey = false;
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

    private void setControllerMapping(keyCode targetKey, String identifierName, JTextField field) {
        currentController.put(targetKey, identifierName);
        field.setText(getControllerFieldText(identifierName));
        getKey = false;
    }

    private Component getControllerComponent(String identifierName) {
        Controller controller = getSelectedController();
        if (controller == null) {
            return null;
        }

        Component[] components = controller.getComponents();
        if (components == null) {
            return null;
        }

        for (int i = 0; i < components.length; i++) {
            if (identifierName.equals(components[i].getIdentifier().getName())) {
                return components[i];
            }
        }

        return null;
    }

    private String getControllerFieldText(String identifierName) {
        Component component = getControllerComponent(identifierName);
        if (component == null) {
            return identifierName;
        }

        String name = component.getName();
        if (name == null) {
            // Use the Identifier name if the component has no name
            name = identifierName;
        } else if (name.length() > maxControllerFieldValueLength && identifierName.length() < name.length()) {
            // Use the Identifier name if the component name is too long to fit
            // into the display field
            name = identifierName;
        }

        return name;
    }

    private void onControllerEvent(Event event) {
        if (!getKey) {
            return;
        }

        Component component = event.getComponent();
        float value = event.getValue();
        Identifier identifier = component.getIdentifier();
        String identifierName = identifier.getName();

        if (identifier instanceof Button && value == 1.f) {
            setControllerMapping(targetKey, identifierName, sender);
        } else if (identifier == Axis.POV) {
            switch (targetKey) {
                case DOWN:
                case UP:
                case LEFT:
                case RIGHT:
                    setControllerMapping(keyCode.DOWN, identifierName, fieldDown);
                    setControllerMapping(keyCode.UP, identifierName, fieldUp);
                    setControllerMapping(keyCode.LEFT, identifierName, fieldLeft);
                    setControllerMapping(keyCode.RIGHT, identifierName, fieldRight);
                    break;
                default:
                    jpcsp.Controller.log.warn(String.format("Unknown Controller POV Event on %s(%s): %f for %s", component.getName(), identifier.getName(), value, targetKey.toString()));
                    break;
            }
        } else if (identifier instanceof Axis && !jpcsp.Controller.isInDeadZone(component, value)) {
            switch (targetKey) {
                case DOWN:
                case UP:
                    setControllerMapping(keyCode.DOWN, identifierName, fieldDown);
                    setControllerMapping(keyCode.UP, identifierName, fieldUp);
                    break;
                case LEFT:
                case RIGHT:
                    setControllerMapping(keyCode.LEFT, identifierName, fieldLeft);
                    setControllerMapping(keyCode.RIGHT, identifierName, fieldRight);
                    break;
                case LANDOWN:
                case LANUP:
                    setControllerMapping(keyCode.LANDOWN, identifierName, fieldAnalogDown);
                    setControllerMapping(keyCode.LANUP, identifierName, fieldAnalogUp);
                    break;
                case LANLEFT:
                case LANRIGHT:
                    setControllerMapping(keyCode.LANLEFT, identifierName, fieldAnalogLeft);
                    setControllerMapping(keyCode.LANRIGHT, identifierName, fieldAnalogRight);
                    break;
                default:
                    setControllerMapping(targetKey, identifierName, sender);
                    break;
            }
        } else {
            if (identifier instanceof Axis && jpcsp.Controller.isInDeadZone(component, value)) {
                jpcsp.Controller.log.debug(String.format("Unknown Controller Event in DeadZone on %s(%s): %f for %s", component.getName(), identifier.getName(), value, targetKey.toString()));
            } else {
                jpcsp.Controller.log.warn(String.format("Unknown Controller Event on %s(%s): %f for %s", component.getName(), identifier.getName(), value, targetKey.toString()));
            }
        }
    }

    public ComboBoxModel makeControllerComboBoxModel() {
        MutableComboBoxModel comboBox = new DefaultComboBoxModel();
        ControllerEnvironment ce = ControllerEnvironment.getDefaultEnvironment();
        Controller[] controllers = ce.getControllers();
        for (Controller c : controllers) {
            comboBox.addElement(c);
        }
        return comboBox;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        controllerLabel = new javax.swing.JLabel();
        controllerBox = new javax.swing.JComboBox();
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
        jButtonOK = new javax.swing.JButton();
        cancelButton = new jpcsp.GUI.CancelButton();

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("jpcsp/languages/jpcsp"); // NOI18N
        setTitle(bundle.getString("ControlsGUI.title")); // NOI18N
        setResizable(false);

        controllerLabel.setText(bundle.getString("ControlsGUI.controllerLabel.text")); // NOI18N

        controllerBox.setModel(makeControllerComboBoxModel());

        keyPanel.setBackground(new java.awt.Color(255, 255, 255));
        keyPanel.setMinimumSize(new java.awt.Dimension(1, 1));
        keyPanel.setLayout(new java.awt.GridBagLayout());

        fgPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));
        fgPanel.setOpaque(false);
        fgPanel.setPreferredSize(new java.awt.Dimension(614, 312));

        fieldStart.setEditable(false);
        fieldStart.setFont(new java.awt.Font("Courier New", 1, 12)); // NOI18N
        fieldStart.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldStart.setText("Enter"); // NOI18N
        fieldStart.setToolTipText(bundle.getString("ControlsGUI.fieldPutKey.text")); // NOI18N
        fieldStart.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 1, true));
        fieldStart.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldStartMouseClicked(evt);
            }
        });

        fieldSelect.setEditable(false);
        fieldSelect.setFont(new java.awt.Font("Courier New", 1, 12)); // NOI18N
        fieldSelect.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldSelect.setText("Space"); // NOI18N
        fieldSelect.setToolTipText(bundle.getString("ControlsGUI.fieldPutKey.text")); // NOI18N
        fieldSelect.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 1, true));
        fieldSelect.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldSelectMouseClicked(evt);
            }
        });

        fieldCross.setEditable(false);
        fieldCross.setFont(new java.awt.Font("Courier New", 1, 12)); // NOI18N
        fieldCross.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldCross.setText("S"); // NOI18N
        fieldCross.setToolTipText(bundle.getString("ControlsGUI.fieldPutKey.text")); // NOI18N
        fieldCross.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 1, true));
        fieldCross.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldCrossMouseClicked(evt);
            }
        });

        fieldCircle.setEditable(false);
        fieldCircle.setFont(new java.awt.Font("Courier New", 1, 12)); // NOI18N
        fieldCircle.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldCircle.setText("D"); // NOI18N
        fieldCircle.setToolTipText(bundle.getString("ControlsGUI.fieldPutKey.text")); // NOI18N
        fieldCircle.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 1, true));
        fieldCircle.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldCircleMouseClicked(evt);
            }
        });

        fieldTriangle.setEditable(false);
        fieldTriangle.setFont(new java.awt.Font("Courier New", 1, 12)); // NOI18N
        fieldTriangle.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldTriangle.setText("W"); // NOI18N
        fieldTriangle.setToolTipText(bundle.getString("ControlsGUI.fieldPutKey.text")); // NOI18N
        fieldTriangle.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 1, true));
        fieldTriangle.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldTriangleMouseClicked(evt);
            }
        });

        fieldSquare.setEditable(false);
        fieldSquare.setFont(new java.awt.Font("Courier New", 1, 12)); // NOI18N
        fieldSquare.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldSquare.setText("A"); // NOI18N
        fieldSquare.setToolTipText(bundle.getString("ControlsGUI.fieldPutKey.text")); // NOI18N
        fieldSquare.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 1, true));
        fieldSquare.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldSquareMouseClicked(evt);
            }
        });

        fieldRight.setEditable(false);
        fieldRight.setFont(new java.awt.Font("Courier New", 1, 12)); // NOI18N
        fieldRight.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldRight.setText("Right"); // NOI18N
        fieldRight.setToolTipText(bundle.getString("ControlsGUI.fieldPutKey.text")); // NOI18N
        fieldRight.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 1, true));
        fieldRight.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldRightMouseClicked(evt);
            }
        });

        fieldUp.setEditable(false);
        fieldUp.setFont(new java.awt.Font("Courier New", 1, 12)); // NOI18N
        fieldUp.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldUp.setText("Up"); // NOI18N
        fieldUp.setToolTipText(bundle.getString("ControlsGUI.fieldPutKey.text")); // NOI18N
        fieldUp.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 1, true));
        fieldUp.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldUpMouseClicked(evt);
            }
        });

        fieldLeft.setEditable(false);
        fieldLeft.setFont(new java.awt.Font("Courier New", 1, 12)); // NOI18N
        fieldLeft.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldLeft.setText("Left"); // NOI18N
        fieldLeft.setToolTipText(bundle.getString("ControlsGUI.fieldPutKey.text")); // NOI18N
        fieldLeft.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 1, true));
        fieldLeft.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldLeftMouseClicked(evt);
            }
        });

        fieldDown.setEditable(false);
        fieldDown.setFont(new java.awt.Font("Courier New", 1, 12)); // NOI18N
        fieldDown.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldDown.setText("Down"); // NOI18N
        fieldDown.setToolTipText(bundle.getString("ControlsGUI.fieldPutKey.text")); // NOI18N
        fieldDown.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 1, true));
        fieldDown.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldDownMouseClicked(evt);
            }
        });

        fieldHold.setEditable(false);
        fieldHold.setFont(new java.awt.Font("Courier New", 1, 12)); // NOI18N
        fieldHold.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldHold.setText("O"); // NOI18N
        fieldHold.setToolTipText(bundle.getString("ControlsGUI.fieldPutKey.text")); // NOI18N
        fieldHold.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 1, true));
        fieldHold.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldHoldMouseClicked(evt);
            }
        });

        fieldHome.setEditable(false);
        fieldHome.setFont(new java.awt.Font("Courier New", 1, 12)); // NOI18N
        fieldHome.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldHome.setText("H"); // NOI18N
        fieldHome.setToolTipText(bundle.getString("ControlsGUI.fieldPutKey.text")); // NOI18N
        fieldHome.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 1, true));
        fieldHome.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldHomeMouseClicked(evt);
            }
        });

        fieldVolMin.setEditable(false);
        fieldVolMin.setFont(new java.awt.Font("Courier New", 1, 12)); // NOI18N
        fieldVolMin.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldVolMin.setText("-"); // NOI18N
        fieldVolMin.setToolTipText(bundle.getString("ControlsGUI.fieldPutKey.text")); // NOI18N
        fieldVolMin.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 1, true));
        fieldVolMin.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldVolMinMouseClicked(evt);
            }
        });

        fieldVolPlus.setEditable(false);
        fieldVolPlus.setFont(new java.awt.Font("Courier New", 1, 12)); // NOI18N
        fieldVolPlus.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldVolPlus.setText("+"); // NOI18N
        fieldVolPlus.setToolTipText(bundle.getString("ControlsGUI.fieldPutKey.text")); // NOI18N
        fieldVolPlus.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 1, true));
        fieldVolPlus.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldVolPlusMouseClicked(evt);
            }
        });

        fieldLTrigger.setEditable(false);
        fieldLTrigger.setFont(new java.awt.Font("Courier New", 1, 12)); // NOI18N
        fieldLTrigger.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldLTrigger.setText("Q"); // NOI18N
        fieldLTrigger.setToolTipText(bundle.getString("ControlsGUI.fieldPutKey.text")); // NOI18N
        fieldLTrigger.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, true));
        fieldLTrigger.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldLTriggerMouseClicked(evt);
            }
        });

        fieldRTrigger.setEditable(false);
        fieldRTrigger.setFont(new java.awt.Font("Courier New", 1, 12)); // NOI18N
        fieldRTrigger.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldRTrigger.setText("E"); // NOI18N
        fieldRTrigger.setToolTipText(bundle.getString("ControlsGUI.fieldPutKey.text")); // NOI18N
        fieldRTrigger.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 1, true));
        fieldRTrigger.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldRTriggerMouseClicked(evt);
            }
        });

        fieldScreen.setEditable(false);
        fieldScreen.setFont(new java.awt.Font("Courier New", 1, 12)); // NOI18N
        fieldScreen.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldScreen.setText("N"); // NOI18N
        fieldScreen.setToolTipText(bundle.getString("ControlsGUI.fieldPutKey.text")); // NOI18N
        fieldScreen.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 1, true));
        fieldScreen.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldScreenMouseClicked(evt);
            }
        });

        fieldMusic.setEditable(false);
        fieldMusic.setFont(new java.awt.Font("Courier New", 1, 12)); // NOI18N
        fieldMusic.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldMusic.setText("M"); // NOI18N
        fieldMusic.setToolTipText(bundle.getString("ControlsGUI.fieldPutKey.text")); // NOI18N
        fieldMusic.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 1, true));
        fieldMusic.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldMusicMouseClicked(evt);
            }
        });

        fieldAnalogUp.setEditable(false);
        fieldAnalogUp.setFont(new java.awt.Font("Courier New", 1, 12)); // NOI18N
        fieldAnalogUp.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldAnalogUp.setText("I"); // NOI18N
        fieldAnalogUp.setToolTipText(bundle.getString("ControlsGUI.fieldPutKey.text")); // NOI18N
        fieldAnalogUp.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 1, true));
        fieldAnalogUp.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldAnalogUpMouseClicked(evt);
            }
        });

        fieldAnalogDown.setEditable(false);
        fieldAnalogDown.setFont(new java.awt.Font("Courier New", 1, 12)); // NOI18N
        fieldAnalogDown.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldAnalogDown.setText("K"); // NOI18N
        fieldAnalogDown.setToolTipText(bundle.getString("ControlsGUI.fieldPutKey.text")); // NOI18N
        fieldAnalogDown.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 1, true));
        fieldAnalogDown.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldAnalogDownMouseClicked(evt);
            }
        });

        fieldAnalogLeft.setEditable(false);
        fieldAnalogLeft.setFont(new java.awt.Font("Courier New", 1, 12)); // NOI18N
        fieldAnalogLeft.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldAnalogLeft.setText("J"); // NOI18N
        fieldAnalogLeft.setToolTipText(bundle.getString("ControlsGUI.fieldPutKey.text")); // NOI18N
        fieldAnalogLeft.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 1, true));
        fieldAnalogLeft.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fieldAnalogLeftMouseClicked(evt);
            }
        });

        fieldAnalogRight.setEditable(false);
        fieldAnalogRight.setFont(new java.awt.Font("Courier New", 1, 12)); // NOI18N
        fieldAnalogRight.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldAnalogRight.setText("L"); // NOI18N
        fieldAnalogRight.setToolTipText(bundle.getString("ControlsGUI.fieldPutKey.text")); // NOI18N
        fieldAnalogRight.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 102, 102), 1, true));
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
                .addGroup(fgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(fgPanelLayout.createSequentialGroup()
                        .addComponent(fieldAnalogLeft, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(fieldAnalogRight, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(fgPanelLayout.createSequentialGroup()
                        .addGap(35, 35, 35)
                        .addGroup(fgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(fgPanelLayout.createSequentialGroup()
                                .addComponent(fieldAnalogDown, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(89, 89, 89)
                                .addComponent(fieldVolMin, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(46, 46, 46))
                            .addGroup(fgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(fieldAnalogUp, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGroup(fgPanelLayout.createSequentialGroup()
                                    .addGap(103, 103, 103)
                                    .addComponent(fieldHome, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(32, 32, 32)
                                    .addComponent(fieldVolPlus, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))))))
                .addGroup(fgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(fgPanelLayout.createSequentialGroup()
                        .addGap(35, 35, 35)
                        .addComponent(fieldScreen, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, fgPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 76, Short.MAX_VALUE)
                        .addGroup(fgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, fgPanelLayout.createSequentialGroup()
                                .addComponent(fieldMusic, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(194, 194, 194))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, fgPanelLayout.createSequentialGroup()
                                .addComponent(fieldSelect, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(72, 72, 72))))))
            .addGroup(fgPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(fgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, fgPanelLayout.createSequentialGroup()
                        .addComponent(fieldDown, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(fieldCross, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(fgPanelLayout.createSequentialGroup()
                        .addComponent(fieldLTrigger, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(fieldRTrigger, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, fgPanelLayout.createSequentialGroup()
                        .addComponent(fieldLeft, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(fieldCircle, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(fgPanelLayout.createSequentialGroup()
                        .addComponent(fieldRight, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(fieldSquare, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, fgPanelLayout.createSequentialGroup()
                        .addComponent(fieldUp, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(fieldTriangle, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, fgPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(fieldHold, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, fgPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(fieldStart, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(39, 39, 39))
        );
        fgPanelLayout.setVerticalGroup(
            fgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(fgPanelLayout.createSequentialGroup()
                .addGap(7, 7, 7)
                .addGroup(fgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fieldLTrigger, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(fieldRTrigger, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(fgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(fgPanelLayout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addComponent(fieldRight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(14, 14, 14)
                        .addComponent(fieldUp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(fgPanelLayout.createSequentialGroup()
                        .addGap(24, 24, 24)
                        .addComponent(fieldSquare, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(fieldTriangle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(fgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(fieldLeft, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(fieldCircle, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(fgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(fgPanelLayout.createSequentialGroup()
                        .addGroup(fgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(fgPanelLayout.createSequentialGroup()
                                .addGap(12, 12, 12)
                                .addComponent(fieldDown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(82, 82, 82)
                                .addComponent(fieldAnalogUp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(fgPanelLayout.createSequentialGroup()
                                .addGap(50, 50, 50)
                                .addComponent(fieldHold, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(fieldStart, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(fieldSelect, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(fgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(fgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(fieldAnalogRight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(fieldHome, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(fieldVolPlus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(fieldAnalogLeft, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(fgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(fieldAnalogDown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(fieldVolMin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(33, Short.MAX_VALUE))
                    .addGroup(fgPanelLayout.createSequentialGroup()
                        .addGap(13, 13, 13)
                        .addComponent(fieldCross, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(fieldMusic, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(fieldScreen, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(36, 36, 36))))
        );

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.ipadx = 10;
        gridBagConstraints.ipady = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        keyPanel.add(fgPanel, gridBagConstraints);

        bgLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        bgLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpcsp/images/controls.jpg"))); // NOI18N
        bgLabel1.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        keyPanel.add(bgLabel1, gridBagConstraints);

        jButtonOK.setText(bundle.getString("OkButton.text")); // NOI18N
        jButtonOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOKActionPerformed(evt);
            }
        });

        cancelButton.setText(bundle.getString("CancelButton.text")); // NOI18N
        cancelButton.setParent(this);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(158, Short.MAX_VALUE)
                .addComponent(controllerLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(controllerBox, javax.swing.GroupLayout.PREFERRED_SIZE, 209, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(193, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButtonOK, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(keyPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap()))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(controllerBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(controllerLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 343, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonOK))
                .addContainerGap())
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(46, 46, 46)
                    .addComponent(keyPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(46, 46, 46)))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void jButtonOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonOKActionPerformed
        Settings.getInstance().writeKeys(currentKeys);
        Settings.getInstance().writeController(currentController);
        String controllerName = controllerBox.getSelectedItem().toString();
        Settings.getInstance().writeString("controller.controllerName", controllerName);

        // Index when several controllers have the same name:
        // 0 refers to the first controller with the given name, 1, to the second...
        int controllerNameIndex = 0;
        int selectedIndex = controllerBox.getSelectedIndex();
        for (int i = 0; i < controllerBox.getItemCount(); i++) {
            if (controllerName.equals(controllerBox.getItemAt(i).toString())) {
                if (i < selectedIndex) {
                    controllerNameIndex++;
                } else {
                    break;
                }
            }
        }
        Settings.getInstance().writeString("controller.controllerNameIndex", String.valueOf(controllerNameIndex));

        State.controller.setInputControllerIndex(controllerBox.getSelectedIndex());
        State.controller.loadKeyConfig(currentKeys);
        State.controller.loadControllerConfig(currentController);

        setVisible(false);
}//GEN-LAST:event_jButtonOKActionPerformed

    private void fieldAnalogRightMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldAnalogRightMouseClicked
        setKey(fieldAnalogRight, keyCode.LANRIGHT);
    }//GEN-LAST:event_fieldAnalogRightMouseClicked

    private void fieldAnalogLeftMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldAnalogLeftMouseClicked
        setKey(fieldAnalogLeft, keyCode.LANLEFT);
    }//GEN-LAST:event_fieldAnalogLeftMouseClicked

    private void fieldAnalogDownMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldAnalogDownMouseClicked
        setKey(fieldAnalogDown, keyCode.LANDOWN);
    }//GEN-LAST:event_fieldAnalogDownMouseClicked

    private void fieldAnalogUpMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldAnalogUpMouseClicked
        setKey(fieldAnalogUp, keyCode.LANUP);
    }//GEN-LAST:event_fieldAnalogUpMouseClicked

    private void fieldMusicMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldMusicMouseClicked
        setKey(fieldMusic, keyCode.MUSIC);
    }//GEN-LAST:event_fieldMusicMouseClicked

    private void fieldScreenMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldScreenMouseClicked
        setKey(fieldScreen, keyCode.SCREEN);
    }//GEN-LAST:event_fieldScreenMouseClicked

    private void fieldRTriggerMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldRTriggerMouseClicked
        setKey(fieldRTrigger, keyCode.R1);
    }//GEN-LAST:event_fieldRTriggerMouseClicked

    private void fieldLTriggerMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldLTriggerMouseClicked
        setKey(fieldLTrigger, keyCode.L1);
    }//GEN-LAST:event_fieldLTriggerMouseClicked

    private void fieldVolPlusMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldVolPlusMouseClicked
        setKey(fieldVolPlus, keyCode.VOLPLUS);
    }//GEN-LAST:event_fieldVolPlusMouseClicked

    private void fieldVolMinMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldVolMinMouseClicked
        setKey(fieldVolMin, keyCode.VOLMIN);
    }//GEN-LAST:event_fieldVolMinMouseClicked

    private void fieldHomeMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldHomeMouseClicked
        setKey(fieldHome, keyCode.HOME);
    }//GEN-LAST:event_fieldHomeMouseClicked

    private void fieldHoldMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldHoldMouseClicked
        setKey(fieldHold, keyCode.HOLD);
    }//GEN-LAST:event_fieldHoldMouseClicked

    private void fieldDownMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldDownMouseClicked
        setKey(fieldDown, keyCode.DOWN);
    }//GEN-LAST:event_fieldDownMouseClicked

    private void fieldLeftMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldLeftMouseClicked
        setKey(fieldLeft, keyCode.LEFT);
    }//GEN-LAST:event_fieldLeftMouseClicked

    private void fieldUpMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldUpMouseClicked
        setKey(fieldUp, keyCode.UP);
    }//GEN-LAST:event_fieldUpMouseClicked

    private void fieldRightMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldRightMouseClicked
        setKey(fieldRight, keyCode.RIGHT);
    }//GEN-LAST:event_fieldRightMouseClicked

    private void fieldSquareMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldSquareMouseClicked
        setKey(fieldSquare, keyCode.SQUARE);
    }//GEN-LAST:event_fieldSquareMouseClicked

    private void fieldTriangleMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldTriangleMouseClicked
        setKey(fieldTriangle, keyCode.TRIANGLE);
    }//GEN-LAST:event_fieldTriangleMouseClicked

    private void fieldCircleMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldCircleMouseClicked
        setKey(fieldCircle, keyCode.CIRCLE);
    }//GEN-LAST:event_fieldCircleMouseClicked

    private void fieldCrossMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldCrossMouseClicked
        setKey(fieldCross, keyCode.CROSS);
    }//GEN-LAST:event_fieldCrossMouseClicked

    private void fieldSelectMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldSelectMouseClicked
        setKey(fieldSelect, keyCode.SELECT);
    }//GEN-LAST:event_fieldSelectMouseClicked

    private void fieldStartMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fieldStartMouseClicked
        setKey(fieldStart, keyCode.START);
    }//GEN-LAST:event_fieldStartMouseClicked

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel bgLabel1;
    private jpcsp.GUI.CancelButton cancelButton;
    private javax.swing.JComboBox controllerBox;
    private javax.swing.JLabel controllerLabel;
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
    private javax.swing.JButton jButtonOK;
    private javax.swing.JPanel keyPanel;
    // End of variables declaration//GEN-END:variables
}
