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

import static jpcsp.HLE.Modules.sceCtrlModule;
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
import jpcsp.HLE.Modules;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import net.java.games.input.Component;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Event;
import net.java.games.input.EventQueue;
import net.java.games.input.Component.POV;
import net.java.games.input.Controller.Type;

public class Controller {
    private static Controller instance;
    private byte Lx = (byte)128;
	private byte Ly = (byte)128;
	private int Buttons = 0;
    private keyCode lastKey = keyCode.RELEASED;
    private long lastUpdate;
    private net.java.games.input.Controller inputController;
    private HashMap<Component.Identifier, Integer> buttonComponents;
    private Component.Identifier XAxis = Component.Identifier.Axis.X;
    private Component.Identifier YAxis = Component.Identifier.Axis.Y;
    private Component.Identifier Arrows = Component.Identifier.Axis.POV;

    private HashMap<String, keyCode> controllerComponents;
    private HashMap<Integer, keyCode> keys;

    public enum keyCode {
        UP, DOWN, LEFT, RIGHT, ANUP, ANDOWN, ANLEFT, ANRIGHT, START, SELECT,
        TRIANGLE, SQUARE, CIRCLE, CROSS, L1, R1, HOME, HOLD, VOLMIN, VOLPLUS,
        SCREEN, MUSIC, RELEASED };

    protected Controller(net.java.games.input.Controller inputController) {
    	this.inputController = inputController;
        keys = new HashMap<Integer, keyCode>(22);
        controllerComponents = new HashMap<String, keyCode>(22);
        loadKeyConfig();
        loadControllerConfig();
        lastUpdate = System.currentTimeMillis();
    }

    public static Controller getInstance() {
    	if (instance == null) {
    		// Disable JInput messages sent to stdout...
    		Logger.getLogger("net.java.games.input.DefaultControllerEnvironment").setLevel(Level.WARNING);

			ControllerEnvironment ce = ControllerEnvironment.getDefaultEnvironment();
			net.java.games.input.Controller[] controllers = ce.getControllers();
			net.java.games.input.Controller inputController = null;

			// Reuse the controller from the settings
			String controllerName = Settings.getInstance().readString("controller.controllerName");
            if (controllerName != null) {
            	for (int i = 0; controllers != null && i < controllers.length; i++) {
            		if (controllerName.equals(controllers[i].getName())) {
            			inputController = controllers[i];
            			break;
            		}
            	}
            }

            if (inputController == null) {
	            // Use the first KEYBOARD controller
				for (int i = 0; controllers != null && i < controllers.length; i++) {
					if (controllers[i].getType() == Type.KEYBOARD) {
            			inputController = controllers[i];
						break;
					}
				}
            }

    		instance = new Controller(inputController);
    	}

    	return instance;
    }

    public void setInputController(net.java.games.input.Controller inputController) {
    	this.inputController = inputController;
    	onInputControllerChanged();
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

    public void loadControllerConfig(HashMap<String, keyCode> newLayout) {
        controllerComponents.clear();
        controllerComponents.putAll(newLayout);

        onInputControllerChanged();
    }

    private void onInputControllerChanged() {
		buttonComponents = new HashMap<Component.Identifier, Integer>();
		for (String controllerName : controllerComponents.keySet()) {
			Component component = getControllerComponentByName(controllerName);
			if (component != null) {
				int keyCode = -1;
				switch (controllerComponents.get(controllerName)) {
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
			}
		}
    }

    public void checkControllerState(){
        // checkControllerState is called every cpu step,
        // so we need to delay that a bit
        long now = System.currentTimeMillis();
        if (now - lastUpdate < 1000 / 30) {
            return;
        }

        processSpecialKeys();
        pollController();
        sceCtrlModule.setButtons(Lx, Ly, Buttons);
        lastUpdate = now;
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
            case ANDOWN:    Ly = (byte)128; break;
            case ANUP:      Ly = (byte)128; break;
            case ANLEFT:    Lx = (byte)128; break;
            case ANRIGHT:   Lx = (byte)128; break;

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

    protected Component getControllerComponentByName(String name) {
		Component[] components = inputController.getComponents();
		if (components != null) {
			for (int i = 0; i < components.length; i++) {
				if (name.equals(components[i].getName())) {
					return components[i];
				}
			}
		}

		return null;
	}

	protected void processControllerEvent(Component component, float value) {
		if (Modules.log.isDebugEnabled()) {
			Modules.log.debug(String.format("JInput Event on %s: %f", component.getName(), value));
		}

		Component.Identifier id = component.getIdentifier();
		Integer button = buttonComponents.get(id);
		if (button != null) {
			if (value == 0.f) {
				Buttons &= ~button;
			} else if (value == 1.f) {
				Buttons |= button;
			} else {
				Modules.log.warn(String.format("Unknown Controller Button Event on %s(%s): %f", component.getName(), id.getName(), value));
			}
		} else if (id == XAxis) {
			Lx = (byte) (value * 127.f + 128.f);
		} else if (id == YAxis) {
			Ly = (byte) (value * 127.f + 128.f);
		} else if (id == Arrows) {
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
				Modules.log.warn(String.format("Unknown Controller Arrows Event on %s(%s): %f", component.getName(), id.getName(), value));
			}
		} else {
			Modules.log.warn(String.format("Unknown Controller Event on %s(%s): %f", component.getName(), id.getName(), value));
		}
	}
}