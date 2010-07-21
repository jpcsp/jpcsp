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

import jpcsp.HLE.Modules;

import java.awt.event.KeyEvent;
import java.util.HashMap;

public class Controller {
    private byte Lx = (byte)128;
    private byte Ly = (byte)128;
    private int Buttons = 0;
    private keyCode lastKey = keyCode.RELEASED;
    private long lastUpdate;
    private static Controller instance;

    private HashMap<Integer, keyCode> keys;

    public enum keyCode {
        UP, DOWN, LEFT, RIGHT, ANUP, ANDOWN, ANLEFT, ANRIGHT, START, SELECT,
        TRIANGLE, SQUARE, CIRCLE, CROSS, L1, R1, HOME, HOLD, VOLMIN, VOLPLUS,
        SCREEN, MUSIC, RELEASED };

    public Controller() {
        /* load the button config */
        keys = new HashMap<Integer, keyCode>(22);
        loadKeyConfig();
        lastUpdate = System.currentTimeMillis();
        instance = this;
    }

    public static Controller getInstance() {
        return instance;
    }

    public void loadKeyConfig() {
        keys.clear();
        keys.putAll(Settings.getInstance().loadKeys());
    }

    public void loadKeyConfig(HashMap<Integer, keyCode> newLayout) {
        keys.clear();
        keys.putAll(newLayout);
    }

    public void checkControllerState(){
        // checkControllerState is called every cpu step,
        // so we need to delay that a bit
        long now = System.currentTimeMillis();
        if (now - lastUpdate < 1000 / 30) {
            return;
        }

        processSpecialKeys();
        sceCtrlModule.setButtons(Lx, Ly, Buttons);
        lastUpdate = now;
    }

    public void keyPressed(KeyEvent arg0) {
        keyCode key = keys.get(arg0.getKeyCode());
        if (key == null || key == lastKey)
            return;

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

    public void keyReleased(KeyEvent arg0) {
        keyCode key = keys.get(arg0.getKeyCode());
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
            Modules.sceAudioModule.setAudioVolDown();
            Modules.sceSasCoreModule.setSasVolDown();
        } else if (isSpecialKeyPressed(keyCode.VOLPLUS)) {
            Modules.sceAudioModule.setAudioVolUp();
            Modules.sceSasCoreModule.setSasVolUp();
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
}