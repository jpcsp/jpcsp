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

import static jpcsp.HLE.modules150.sceCtrl.PSP_CTRL_CIRCLE;
import static jpcsp.HLE.modules150.sceCtrl.PSP_CTRL_CROSS;
import static jpcsp.HLE.modules150.sceCtrl.PSP_CTRL_DOWN;
import static jpcsp.HLE.modules150.sceCtrl.PSP_CTRL_HOLD;
import static jpcsp.HLE.modules150.sceCtrl.PSP_CTRL_HOME;
import static jpcsp.HLE.modules150.sceCtrl.PSP_CTRL_LEFT;
import static jpcsp.HLE.modules150.sceCtrl.PSP_CTRL_LTRIGGER;
import static jpcsp.HLE.modules150.sceCtrl.PSP_CTRL_NOTE;
import static jpcsp.HLE.modules150.sceCtrl.PSP_CTRL_RIGHT;
import static jpcsp.HLE.modules150.sceCtrl.PSP_CTRL_RTRIGGER;
import static jpcsp.HLE.modules150.sceCtrl.PSP_CTRL_SCREEN;
import static jpcsp.HLE.modules150.sceCtrl.PSP_CTRL_SELECT;
import static jpcsp.HLE.modules150.sceCtrl.PSP_CTRL_SQUARE;
import static jpcsp.HLE.modules150.sceCtrl.PSP_CTRL_START;
import static jpcsp.HLE.modules150.sceCtrl.PSP_CTRL_TRIANGLE;
import static jpcsp.HLE.modules150.sceCtrl.PSP_CTRL_UP;
import static jpcsp.HLE.modules150.sceCtrl.PSP_CTRL_VOLDOWN;
import static jpcsp.HLE.modules150.sceCtrl.PSP_CTRL_VOLUP;

import jpcsp.hardware.Audio;
import jpcsp.settings.Settings;
import jpcsp.HLE.Modules;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.JOptionPane;

import org.apache.log4j.Logger;

import net.java.games.input.Component;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Event;
import net.java.games.input.EventQueue;
import net.java.games.input.Component.Identifier;
import net.java.games.input.Component.POV;
import net.java.games.input.Component.Identifier.Axis;
import net.java.games.input.Component.Identifier.Button;
import net.java.games.input.Controller.Type;

public class Controller {
    public static Logger log = Logger.getLogger("controller");
    private static Controller instance;
    public static final byte analogCenter = (byte) 128;
    private byte Lx = analogCenter;
	private byte Ly = analogCenter;
	private int Buttons = 0;
    private keyCode lastKey = keyCode.RELEASED;
    private net.java.games.input.Controller inputController;
    private HashMap<Component.Identifier, Integer> buttonComponents;
    private Component.Identifier analogXAxis = Component.Identifier.Axis.X;
    private Component.Identifier analogYAxis = Component.Identifier.Axis.Y;
    private Component.Identifier digitalXAxis = null;
    private Component.Identifier digitalYAxis = null;
    private Component.Identifier povArrows = Component.Identifier.Axis.POV;
    private static final float minimumDeadZone = 0.1f;

    private HashMap<keyCode, String> controllerComponents;
    private HashMap<Integer, keyCode> keys;

    public enum keyCode {
        UP, DOWN, LEFT, RIGHT, ANUP, ANDOWN, ANLEFT, ANRIGHT, START, SELECT,
        TRIANGLE, SQUARE, CIRCLE, CROSS, L1, R1, HOME, HOLD, VOLMIN, VOLPLUS,
        SCREEN, MUSIC, RELEASED };

    protected Controller(net.java.games.input.Controller inputController) {
    	this.inputController = inputController;
        keys = new HashMap<Integer, keyCode>(22);
        controllerComponents = new HashMap<keyCode, String>(22);
        loadKeyConfig();
        loadControllerConfig();
    }

    public static boolean isKeyboardController(net.java.games.input.Controller inputController) {
    	return inputController == null || inputController.getType() == Type.KEYBOARD;
    }

    public static Controller getInstance() {
    	if (instance == null) {
    		// Disable JInput messages sent to stdout...
    		java.util.logging.Logger.getLogger("net.java.games.input.DefaultControllerEnvironment").setLevel(Level.WARNING);

			ControllerEnvironment ce = ControllerEnvironment.getDefaultEnvironment();
			net.java.games.input.Controller[] controllers = ce.getControllers();
			net.java.games.input.Controller inputController = null;

			// Reuse the controller from the settings
			String controllerName = Settings.getInstance().readString("controller.controllerName");
			// The controllerNameIndex is the index when several controllers have
			// the same name. 0 to use the first controller with the given name,
			// 1, to use the second...
			int controllerNameIndex = Settings.getInstance().readInt("controller.controllerNameIndex", 0);
            if (controllerName != null) {
            	for (int i = 0; controllers != null && i < controllers.length; i++) {
            		if (controllerName.equals(controllers[i].getName())) {
        				inputController = controllers[i];
            			if (controllerNameIndex <= 0) {
            				break;
            			}
        				controllerNameIndex--;
            		}
            	}
            }

            if (inputController == null) {
	            // Use the first KEYBOARD controller
				for (int i = 0; controllers != null && i < controllers.length; i++) {
					if (isKeyboardController(controllers[i])) {
            			inputController = controllers[i];
						break;
					}
				}
            }

            if (inputController == null) {
            	log.info(String.format("No KEYBOARD controller found"));
				for (int i = 0; controllers != null && i < controllers.length; i++) {
					log.info(String.format("    Controller: '%s'", controllers[i].getName()));
				}
            } else {
            	log.info(String.format("Using default controller '%s'", inputController.getName()));
            }
    		instance = new Controller(inputController);
    	}

    	return instance;
    }

    public void setInputController(net.java.games.input.Controller inputController) {
    	if (inputController != null) {
    		log.info(String.format("Using controller '%s'", inputController.getName()));
    	}
    	this.inputController = inputController;
    	onInputControllerChanged();
    }

    public net.java.games.input.Controller getInputController() {
    	return inputController;
    }

    public void setInputControllerIndex(int index) {
		ControllerEnvironment ce = ControllerEnvironment.getDefaultEnvironment();
		net.java.games.input.Controller[] controllers = ce.getControllers();
		if (controllers != null && index >= 0 && index < controllers.length) {
			setInputController(controllers[index]);
		}
    }

    public void loadKeyConfig() {
    	loadKeyConfig(Settings.getInstance().loadKeys());
    }

    public void loadKeyConfig(HashMap<Integer, keyCode> newLayout) {
        keys.clear();
        keys.putAll(newLayout);
    }

    public void loadControllerConfig() {
    	loadControllerConfig(Settings.getInstance().loadController());
    }

    public void loadControllerConfig(HashMap<keyCode, String> newLayout) {
        controllerComponents.clear();
        controllerComponents.putAll(newLayout);

        onInputControllerChanged();
    }

    private void onInputControllerChanged() {
		buttonComponents = new HashMap<Component.Identifier, Integer>();
		for (Map.Entry<keyCode, String> entry : controllerComponents.entrySet()) {
			keyCode key = entry.getKey();
			String controllerName = entry.getValue();
			Component component = getControllerComponentByName(controllerName);
			if (component != null) {
				Identifier identifier = component.getIdentifier();
				boolean isButton = identifier instanceof Button;
				boolean isAxis = identifier instanceof Axis;

				if (isButton) {
					int keyCode = -1;
					switch (key) {
			            case DOWN:     keyCode = PSP_CTRL_DOWN; break;
			            case UP:       keyCode = PSP_CTRL_UP; break;
			            case LEFT:     keyCode = PSP_CTRL_LEFT; break;
			            case RIGHT:    keyCode = PSP_CTRL_RIGHT; break;
			            case TRIANGLE: keyCode = PSP_CTRL_TRIANGLE; break;
			            case SQUARE:   keyCode = PSP_CTRL_SQUARE; break;
			            case CIRCLE:   keyCode = PSP_CTRL_CIRCLE; break;
			            case CROSS:    keyCode = PSP_CTRL_CROSS; break;
			            case L1:       keyCode = PSP_CTRL_LTRIGGER; break;
			            case R1:       keyCode = PSP_CTRL_RTRIGGER; break;
			            case START:    keyCode = PSP_CTRL_START; break;
			            case SELECT:   keyCode = PSP_CTRL_SELECT; break;
			            case HOME:     keyCode = PSP_CTRL_HOME; break;
			            case HOLD:     keyCode = PSP_CTRL_HOLD; break;
			            case VOLMIN:   keyCode = PSP_CTRL_VOLDOWN; break;
			            case VOLPLUS:  keyCode = PSP_CTRL_VOLUP; break;
			            case SCREEN:   keyCode = PSP_CTRL_SCREEN; break;
			            case MUSIC:    keyCode = PSP_CTRL_NOTE; break;
					}
					if (keyCode != -1) {
						buttonComponents.put(component.getIdentifier(), keyCode);
					}
				} else if (isAxis && identifier == Axis.POV) {
					povArrows = identifier;
				} else if (isAxis) {
					switch (key) {
						case DOWN:
						case UP:       digitalYAxis = identifier; break;
						case LEFT:
						case RIGHT:    digitalXAxis = identifier; break;
						case ANDOWN:
						case ANUP:     analogYAxis = identifier; break;
						case ANLEFT:
						case ANRIGHT:  analogXAxis = identifier; break;
					}
				}
			}
		}
    }

    /**
     * Called by sceCtrl at every VBLANK interrupt.
     */
    public void hleControllerPoll() {
        processSpecialKeys();
        pollController();
    }

    private void pollController() {
        if (inputController != null && inputController.poll()) {
			EventQueue eventQueue = inputController.getEventQueue();
			Event event = new Event();
			while (eventQueue.getNextEvent(event)) {
				Component component = event.getComponent();
				float value = event.getValue();
				processControllerEvent(component, value);
			}
		}
    }

    public void keyPressed(KeyEvent keyEvent) {
        keyCode key = keys.get(keyEvent.getKeyCode());
        if (key == null || key == lastKey) {
            return;
        }

        switch (key) {
            case DOWN:      Buttons |= PSP_CTRL_DOWN; break;
            case UP:        Buttons |= PSP_CTRL_UP; break;
            case LEFT:      Buttons |= PSP_CTRL_LEFT; break;
            case RIGHT:     Buttons |= PSP_CTRL_RIGHT; break;
            case ANDOWN:    Ly = (byte)255; break;
            case ANUP:      Ly = 0; break;
            case ANLEFT:    Lx = 0; break;
            case ANRIGHT:   Lx = (byte)255; break;

            case TRIANGLE:  Buttons |= PSP_CTRL_TRIANGLE; break;
            case SQUARE:    Buttons |= PSP_CTRL_SQUARE; break;
            case CIRCLE:    Buttons |= PSP_CTRL_CIRCLE; break;
            case CROSS:     Buttons |= PSP_CTRL_CROSS; break;
            case L1:        Buttons |= PSP_CTRL_LTRIGGER; break;
            case R1:        Buttons |= PSP_CTRL_RTRIGGER; break;
            case START:     Buttons |= PSP_CTRL_START; break;
            case SELECT:    Buttons |= PSP_CTRL_SELECT; break;

            case HOME:      Buttons |= PSP_CTRL_HOME; break;
            case HOLD:      Buttons |= PSP_CTRL_HOLD; break;
            case VOLMIN:    Buttons |= PSP_CTRL_VOLDOWN; break;
            case VOLPLUS:   Buttons |= PSP_CTRL_VOLUP; break;
            case SCREEN:    Buttons |= PSP_CTRL_SCREEN; break;
            case MUSIC:     Buttons |= PSP_CTRL_NOTE; break;

            default: return;
        }
        lastKey = key;
    }

    public void keyReleased(KeyEvent keyEvent) {
        keyCode key = keys.get(keyEvent.getKeyCode());
        if (key == null)
            return;

        switch (key) {
            case DOWN:      Buttons &= ~PSP_CTRL_DOWN; break;
            case UP:        Buttons &= ~PSP_CTRL_UP; break;
            case LEFT:      Buttons &= ~PSP_CTRL_LEFT; break;
            case RIGHT:     Buttons &= ~PSP_CTRL_RIGHT; break;
            case ANDOWN:    Ly = analogCenter; break;
            case ANUP:      Ly = analogCenter; break;
            case ANLEFT:    Lx = analogCenter; break;
            case ANRIGHT:   Lx = analogCenter; break;

            case TRIANGLE:  Buttons &= ~PSP_CTRL_TRIANGLE; break;
            case SQUARE:    Buttons &= ~PSP_CTRL_SQUARE; break;
            case CIRCLE:    Buttons &= ~PSP_CTRL_CIRCLE; break;
            case CROSS:     Buttons &= ~PSP_CTRL_CROSS; break;
            case L1:        Buttons &= ~PSP_CTRL_LTRIGGER; break;
            case R1:        Buttons &= ~PSP_CTRL_RTRIGGER; break;
            case START:     Buttons &= ~PSP_CTRL_START; break;
            case SELECT:    Buttons &= ~PSP_CTRL_SELECT; break;

            case HOME:      Buttons &= ~PSP_CTRL_HOME; break;
            case HOLD:      Buttons &= ~PSP_CTRL_HOLD; break;
            case VOLMIN:    Buttons &= ~PSP_CTRL_VOLDOWN; break;
            case VOLPLUS:   Buttons &= ~PSP_CTRL_VOLUP; break;
            case SCREEN:    Buttons &= ~PSP_CTRL_SCREEN; break;
            case MUSIC:     Buttons &= ~PSP_CTRL_NOTE; break;

            default: return;
        }
        lastKey = keyCode.RELEASED;
    }

    private void processSpecialKeys() {
        if (isSpecialKeyPressed(keyCode.VOLMIN)) {
            Audio.setVolumeDown();
        } else if (isSpecialKeyPressed(keyCode.VOLPLUS)) {
        	Audio.setVolumeUp();
        } else if (isSpecialKeyPressed(keyCode.HOME)) {
            Buttons &= ~PSP_CTRL_HOME;    // Release the HOME button to avoid dialog spamming.
            int opt = JOptionPane.showOptionDialog(null, "Exit the current application?", "HOME", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null, null, null);
            if (opt == JOptionPane.YES_OPTION) {
                Modules.LoadExecForUserModule.triggerExitCallback();
            }
        }
    }

    // Check if a certain special key is pressed.
    private boolean isSpecialKeyPressed(keyCode key) {
        boolean res = false;
        switch (key) {
            case HOME:
                if ((Buttons & PSP_CTRL_HOME) == PSP_CTRL_HOME) {
                    res = true;
                }
                break;
            case HOLD:
                if ((Buttons & PSP_CTRL_HOLD) == PSP_CTRL_HOLD) {
                    res = true;
                }
                break;
            case VOLMIN:
                if ((Buttons & PSP_CTRL_VOLDOWN) == PSP_CTRL_VOLDOWN) {
                    res = true;
                }
                break;
            case VOLPLUS:
                if ((Buttons & PSP_CTRL_VOLUP) == PSP_CTRL_VOLUP) {
                    res = true;
                }
                break;
            case SCREEN:
                if ((Buttons & PSP_CTRL_SCREEN) == PSP_CTRL_SCREEN) {
                    res = true;
                }
                break;
            case MUSIC:
                if ((Buttons & PSP_CTRL_NOTE) == PSP_CTRL_NOTE) {
                    res = true;
                }
                break;
        }
        return res;
    }

    private Component getControllerComponentByName(String name) {
		Component[] components = inputController.getComponents();
		if (components != null) {
			// First search for the identifier name
			for (int i = 0; i < components.length; i++) {
				if (name.equals(components[i].getIdentifier().getName())) {
					return components[i];
				}
			}

			// Second search for the component name
			for (int i = 0; i < components.length; i++) {
				if (name.equals(components[i].getName())) {
					return components[i];
				}
			}
		}

		return null;
	}

    public static float getDeadZone(Component component) {
    	float deadZone = component.getDeadZone();
    	if (deadZone < minimumDeadZone) {
    		deadZone = minimumDeadZone;
    	}

    	return deadZone;
    }

    public static boolean isInDeadZone(Component component, float value) {
    	return Math.abs(value) <= getDeadZone(component);
    }

    private void processControllerEvent(Component component, float value) {
		Component.Identifier id = component.getIdentifier();
		if (log.isDebugEnabled()) {
			log.debug(String.format("Controller Event on %s(%s): %f", component.getName(), id.getName(), value));
		}

		Integer button = buttonComponents.get(id);
		if (button != null) {
			if (value == 0.f) {
				Buttons &= ~button;
			} else if (value == 1.f) {
				Buttons |= button;
			} else {
				log.warn(String.format("Unknown Controller Button Event on %s(%s): %f", component.getName(), id.getName(), value));
			}
		} else if (id == analogXAxis) {
			if (isInDeadZone(component, value)) {
				Lx = analogCenter;
			} else {
				Lx = (byte) (value * 127.f + 128.f);
			}
		} else if (id == analogYAxis) {
			if (isInDeadZone(component, value)) {
				Ly = analogCenter;
			} else {
				Ly = (byte) (value * 127.f + 128.f);
			}
		} else if (id == digitalXAxis) {
			if (isInDeadZone(component, value)) {
				Buttons &= ~(PSP_CTRL_LEFT | PSP_CTRL_RIGHT);
			} else if (value < 0.f) {
				Buttons |= PSP_CTRL_LEFT;
			} else {
				Buttons |= PSP_CTRL_RIGHT;
			}
		} else if (id == digitalYAxis) {
			if (isInDeadZone(component, value)) {
				Buttons &= ~(PSP_CTRL_DOWN | PSP_CTRL_UP);
			} else if (value < 0.f) {
				Buttons |= PSP_CTRL_UP;
			} else {
				Buttons |= PSP_CTRL_DOWN;
			}
		} else if (id == povArrows) {
			if (value == POV.CENTER) {
				Buttons &= ~(PSP_CTRL_RIGHT | PSP_CTRL_LEFT | PSP_CTRL_DOWN | PSP_CTRL_UP);
			} else if (value == POV.UP) {
				Buttons |= PSP_CTRL_UP;
			} else if (value == POV.RIGHT) {
				Buttons |= PSP_CTRL_RIGHT;
			} else if (value == POV.DOWN) {
				Buttons |= PSP_CTRL_DOWN;
			} else if (value == POV.LEFT) {
				Buttons |= PSP_CTRL_LEFT;
			} else if (value == POV.DOWN_LEFT) {
				Buttons |= PSP_CTRL_DOWN | PSP_CTRL_LEFT;
			} else if (value == POV.DOWN_RIGHT) {
				Buttons |= PSP_CTRL_DOWN | PSP_CTRL_RIGHT;
			} else if (value == POV.UP_LEFT) {
				Buttons |= PSP_CTRL_UP | PSP_CTRL_LEFT;
			} else if (value == POV.UP_RIGHT) {
				Buttons |= PSP_CTRL_UP | PSP_CTRL_RIGHT;
			} else {
				log.warn(String.format("Unknown Controller Arrows Event on %s(%s): %f", component.getName(), id.getName(), value));
			}
		} else {
			// Unknown Axis components are allowed to move inside their dead zone
			// (e.g. due to small vibrations)
			if (id instanceof Axis && (isInDeadZone(component, value) || id == Axis.Z || id == Axis.RZ)) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("Unknown Controller Event in DeadZone on %s(%s): %f", component.getName(), id.getName(), value));
				}
			} else if (isKeyboardController(inputController)) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("Unknown Keyboard Controller Event on %s(%s): %f", component.getName(), id.getName(), value));
				}
			} else {
				if (log.isInfoEnabled()) {
					log.warn(String.format("Unknown Controller Event on %s(%s): %f", component.getName(), id.getName(), value));
				}
			}
		}
	}

    public byte getLx() {
    	return Lx;
    }

    public byte getLy() {
    	return Ly;
    }

    public int getButtons() {
    	return Buttons;
    }
}