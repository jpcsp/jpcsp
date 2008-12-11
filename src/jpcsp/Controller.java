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

import java.awt.event.KeyEvent;
import java.util.HashMap;

public class Controller {
    private byte Lx = 127;
    private byte Ly = 127;
    private int Buttons = 0;
    private keyCode lastKey = keyCode.RELEASED;
    private long lastUpdate;

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

        sceCtrlModule.setButtons(Lx, Ly, Buttons);
        lastUpdate = now;
    }

    public void keyPressed(KeyEvent arg0) {
        keyCode key = keys.get(arg0.getKeyCode());
        if (key == null || key == lastKey)
            return;

        /* in digital mode, analoge keys behave as the digital keys (right?) */
        if (sceCtrlModule.isModeDigital()) {
            switch (key) {
                case ANDOWN:    key = keyCode.DOWN; break;
                case ANUP:      key = keyCode.UP; break;
                case ANLEFT:    key = keyCode.LEFT; break;
                case ANRIGHT:   key = keyCode.RIGHT; break;
            }
        }

        switch (key) {
            case DOWN:      this.Buttons |= PSP_CTRL_DOWN; break;
            case UP:        this.Buttons |= PSP_CTRL_UP; break;
            case LEFT:      this.Buttons |= PSP_CTRL_LEFT; break;
            case RIGHT:     this.Buttons |= PSP_CTRL_RIGHT; break;
            case ANDOWN:    this.Ly = 0; break;
            case ANUP:      this.Ly = (byte)255; break;
            case ANLEFT:    this.Lx = 0; break;
            case ANRIGHT:   this.Lx = (byte)255; break;

            case TRIANGLE:  this.Buttons |= PSP_CTRL_TRIANGLE; break;
            case SQUARE:    this.Buttons |= PSP_CTRL_SQUARE; break;
            case CIRCLE:    this.Buttons |= PSP_CTRL_CIRCLE; break;
            case CROSS:     this.Buttons |= PSP_CTRL_CROSS; break;
            case L1:        this.Buttons |= PSP_CTRL_LTRIGGER; break;
            case R1:        this.Buttons |= PSP_CTRL_RTRIGGER; break;
            case START:     this.Buttons |= PSP_CTRL_START; break;
            case SELECT:    this.Buttons |= PSP_CTRL_SELECT; break;

            case HOME:      this.Buttons |= PSP_CTRL_HOME; break;
            case HOLD:      this.Buttons |= PSP_CTRL_HOLD; break;
            case VOLMIN:    this.Buttons |= PSP_CTRL_VOLDOWN; break;
            case VOLPLUS:   this.Buttons |= PSP_CTRL_VOLUP; break;
            case SCREEN:    this.Buttons |= PSP_CTRL_SCREEN; break;
            case MUSIC:     this.Buttons |= PSP_CTRL_NOTE; break;

            default: return;
        }
        lastKey = key;
        //System.out.println("keyPressed!! " + this.Buttons);
    }

    public void keyReleased(KeyEvent arg0) {
        keyCode key = keys.get(arg0.getKeyCode());
        if (key == null)
            return;

        if (sceCtrlModule.isModeDigital()) {
            switch (key) {
                case ANDOWN:    key = keyCode.DOWN; break;
                case ANUP:      key = keyCode.UP; break;
                case ANLEFT:    key = keyCode.LEFT; break;
                case ANRIGHT:   key = keyCode.RIGHT; break;
            }
        }

        switch (key) {
            case DOWN:      this.Buttons &= ~PSP_CTRL_DOWN; break;
            case UP:        this.Buttons &= ~PSP_CTRL_UP; break;
            case LEFT:      this.Buttons &= ~PSP_CTRL_LEFT; break;
            case RIGHT:     this.Buttons &= ~PSP_CTRL_RIGHT; break;
            case ANDOWN:    this.Ly = (byte)128; break;
            case ANUP:      this.Ly = (byte)128; break;
            case ANLEFT:    this.Lx = (byte)128; break;
            case ANRIGHT:   this.Lx = (byte)128; break;

            case TRIANGLE:  this.Buttons &= ~PSP_CTRL_TRIANGLE; break;
            case SQUARE:    this.Buttons &= ~PSP_CTRL_SQUARE; break;
            case CIRCLE:    this.Buttons &= ~PSP_CTRL_CIRCLE; break;
            case CROSS:     this.Buttons &= ~PSP_CTRL_CROSS; break;
            case L1:        this.Buttons &= ~PSP_CTRL_LTRIGGER; break;
            case R1:        this.Buttons &= ~PSP_CTRL_RTRIGGER; break;
            case START:     this.Buttons &= ~PSP_CTRL_START; break;
            case SELECT:    this.Buttons &= ~PSP_CTRL_SELECT; break;

            case HOME:      this.Buttons &= ~PSP_CTRL_HOME; break;
            case HOLD:      this.Buttons &= ~PSP_CTRL_HOLD; break;
            case VOLMIN:    this.Buttons &= ~PSP_CTRL_VOLDOWN; break;
            case VOLPLUS:   this.Buttons &= ~PSP_CTRL_VOLUP; break;
            case SCREEN:    this.Buttons &= ~PSP_CTRL_SCREEN; break;
            case MUSIC:     this.Buttons &= ~PSP_CTRL_NOTE; break;

            default: return;
        }
        lastKey = keyCode.RELEASED;
    }
}
