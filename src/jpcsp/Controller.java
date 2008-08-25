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


public class Controller {
    private byte Lx = 127;
    private byte Ly = 127;
    private int Buttons = 0;
    public boolean changed = true;
    
    public void checkControllerState(){
        if (!changed)
            return;
        
        jpcsp.HLE.pspctrl.get_instance().setButtons(Lx, Ly, Buttons);
        changed = false;
    }
   
    public void keyPressed(KeyEvent arg0) {
        switch (arg0.getKeyCode()) {
            case KeyEvent.VK_DOWN:  this.Lx = 0; break;
            case KeyEvent.VK_UP:    this.Lx = (byte)255; break;
            
            case KeyEvent.VK_LEFT:  this.Ly = 0; break;
            case KeyEvent.VK_RIGHT: this.Ly = (byte)255; break;
            
            case KeyEvent.VK_ENTER: this.Buttons |= jpcsp.HLE.pspctrl.PSP_CTRL_START; break;
            case KeyEvent.VK_SPACE: this.Buttons |= jpcsp.HLE.pspctrl.PSP_CTRL_SELECT; break;
            
            default: return;
        }
        changed = true;
        //System.out.println("keyPressed!! " + this.Buttons);
    }

    public void keyReleased(KeyEvent arg0) {
        switch (arg0.getKeyCode()) {
            case KeyEvent.VK_DOWN:  this.Lx = (byte)128; break;
            case KeyEvent.VK_UP:    this.Lx = (byte)128; break;
            
            case KeyEvent.VK_LEFT:  this.Ly = (byte)128; break;
            case KeyEvent.VK_RIGHT: this.Ly = (byte)128; break;
            
            case KeyEvent.VK_ENTER: this.Buttons &= ~jpcsp.HLE.pspctrl.PSP_CTRL_START; break;
            case KeyEvent.VK_SPACE: this.Buttons &= ~jpcsp.HLE.pspctrl.PSP_CTRL_SELECT; break;
            
            default: return;
        }
        changed = true;
        //System.out.println("keyReleased!! "  + this.Buttons);
    }
}
