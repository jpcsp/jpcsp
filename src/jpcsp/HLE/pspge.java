/*
Function:
- HLE everything in http://psp.jim.sh/pspsdk-doc/pspge_8h.html


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
package jpcsp.HLE;

import jpcsp.Emulator;
import jpcsp.MemoryMap;
import jpcsp.graphics.DisplayList;
import jpcsp.graphics.VideoEngine;

public class pspge {

    private static pspge instance;

    public static pspge get_instance() {
        if (instance == null) {
            instance = new pspge();
        }
        return instance;
    }

    private pspge() {
    }

    public void Initialise() {
    }

    public void sceGeEdramGetAddr() {
        Emulator.getProcessor().gpr[2] = MemoryMap.START_VRAM;
    }

    public int sceGeListEnQueue(int list, int stall, int callbackId, int argument) {
        
        /*
        list 	- The head of the list to queue.
	stall 	- The stall address. If NULL then no stall address set and the list is transferred immediately.
	cbid 	- ID of the callback set by calling sceGeSetCallback
	arg 	- Probably a parameter to the callbacks (to be confirmed)
        */
        
        DisplayList displayList = new DisplayList(list, stall, callbackId, argument);
        VideoEngine ve = VideoEngine.getEngine(null, true, true);
        /**
         * 
         * reading more, i saw that here we just put the display list on quee 
         * after that we draw [execute list]...
         * 
         * so this code is just to debug stuffs until we discovery how
         * things goes.... 
         */
        log("The list " + displayList.toString());
        ve.executeList(displayList);
        return displayList.id;
    }
    
    private void log(String msg){
        System.out.println("sceGe DEBUG > " + msg);
    }
    
    
    
    
    
}
