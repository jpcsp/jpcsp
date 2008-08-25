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
import java.util.HashMap;


public class Controller {
    private byte Lx = 127;
    private byte Ly = 127;
    private int Buttons = 0;
    public boolean changed = true;
    
    private HashMap<Integer, keyCode> keys;
    
    public enum keyCode { 
        UP, DOWN, LEFT, RIGHT, START, SELECT, TRIANGLE, SQUARE, CIRCLE, CROSS,
        L1, R1, HOME, HOLD };
    
    public Controller() {
        /* load the button config */
        keys = new HashMap<Integer, keyCode>(22);
        loadKeyConfig();
    }
    
    public void loadKeyConfig() {
        keys.clear();
        keys.putAll(Settings.get_instance().loadKeys());
        
        /*keys.put(KeyEvent.VK_UP, keyCode.UP);
        keys.put(KeyEvent.VK_DOWN, keyCode.DOWN);
        keys.put(KeyEvent.VK_LEFT, keyCode.LEFT);
        keys.put(KeyEvent.VK_RIGHT, keyCode.RIGHT);
        keys.put(KeyEvent.VK_ENTER, keyCode.START);
        keys.put(KeyEvent.VK_SPACE, keyCode.SELECT);
        keys.put(KeyEvent.VK_W, keyCode.TRIANGLE);
        keys.put(KeyEvent.VK_A, keyCode.SQUARE);
        keys.put(KeyEvent.VK_D, keyCode.CIRCLE);
        keys.put(KeyEvent.VK_S, keyCode.CROSS);
        keys.put(KeyEvent.VK_Q, keyCode.L1);
        keys.put(KeyEvent.VK_E, keyCode.R1);*/
    }
    
    public void checkControllerState(){
        if (!changed)
            return;
        
        jpcsp.HLE.pspctrl.get_instance().setButtons(Lx, Ly, Buttons);
        changed = false;
    }
   
    public void keyPressed(KeyEvent arg0) {
        keyCode key = keys.get(arg0.getKeyCode());
        if (key == null)
            return;
        
        switch (key) {
            case DOWN:      this.Lx = 0; break;
            case UP:        this.Lx = (byte)255; break;
            case LEFT:      this.Ly = 0; break;
            case RIGHT:     this.Ly = (byte)255; break;
            
            case TRIANGLE:  this.Buttons |= jpcsp.HLE.pspctrl.PSP_CTRL_TRIANGLE; break;
            case SQUARE:    this.Buttons |= jpcsp.HLE.pspctrl.PSP_CTRL_SQUARE; break;
            case CIRCLE:    this.Buttons |= jpcsp.HLE.pspctrl.PSP_CTRL_CIRCLE; break;
            case CROSS:     this.Buttons |= jpcsp.HLE.pspctrl.PSP_CTRL_CROSS; break;
            case L1:        this.Buttons |= jpcsp.HLE.pspctrl.PSP_CTRL_LTRIGGER; break;
            case R1:        this.Buttons |= jpcsp.HLE.pspctrl.PSP_CTRL_RTRIGGER; break;
            case START:     this.Buttons |= jpcsp.HLE.pspctrl.PSP_CTRL_START; break;
            case SELECT:    this.Buttons |= jpcsp.HLE.pspctrl.PSP_CTRL_SELECT; break;
                        
            default: return;
        }
        changed = true;
        //System.out.println("keyPressed!! " + this.Buttons);
    }

    public void keyReleased(KeyEvent arg0) {
        keyCode key = keys.get(arg0.getKeyCode());
        if (key == null)
            return;
        
        switch (key) {
            case DOWN:      this.Lx = (byte)128; break;
            case UP:        this.Lx = (byte)128; break;
            case LEFT:      this.Ly = (byte)128; break;
            case RIGHT:     this.Ly = (byte)128; break;
            
            case TRIANGLE:  this.Buttons &= ~jpcsp.HLE.pspctrl.PSP_CTRL_TRIANGLE; break;
            case SQUARE:    this.Buttons &= ~jpcsp.HLE.pspctrl.PSP_CTRL_SQUARE; break;
            case CIRCLE:    this.Buttons &= ~jpcsp.HLE.pspctrl.PSP_CTRL_CIRCLE; break;
            case CROSS:     this.Buttons &= ~jpcsp.HLE.pspctrl.PSP_CTRL_CROSS; break;
            case L1:        this.Buttons &= ~jpcsp.HLE.pspctrl.PSP_CTRL_LTRIGGER; break;
            case R1:        this.Buttons &= ~jpcsp.HLE.pspctrl.PSP_CTRL_RTRIGGER; break;
            case START:     this.Buttons &= ~jpcsp.HLE.pspctrl.PSP_CTRL_START; break;
            case SELECT:    this.Buttons &= ~jpcsp.HLE.pspctrl.PSP_CTRL_SELECT; break;
           
            default: return;
        }
        changed = true;
        //System.out.println("keyReleased!! "  + this.Buttons);
    }
}
