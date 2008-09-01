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
//import jpcsp.graphics.PspGeCallbackData;

public class pspge {

    private static pspge instance;
    /*
    private PspGeCallbackData cbdata;
    private int cbid = -1;
    */

    public static pspge get_instance() {
        if (instance == null) {
            instance = new pspge();
        }
        return instance;
    }

    private pspge() {
    }

    public void Initialise() {
        DisplayList.Lock();
        DisplayList.Initialise();
        DisplayList.Unlock();
    }

    public void sceGeEdramGetAddr() {
        Emulator.getProcessor().gpr[2] = MemoryMap.START_VRAM;
    }

    public void sceGeListEnQueue(int list, int stall, int callbackId, int argument) {
        DisplayList.Lock();

        /*
        list 	- The head of the list to queue.
        stall 	- The stall address. If NULL then no stall address set and the list is transferred immediately.
        cbid 	- ID of the callback set by calling sceGeSetCallback
        arg 	- Probably a parameter to the callbacks (to be confirmed)
        */

        // remove uncache bit
        list &= 0x3fffffff;
        stall &= 0x3fffffff;

        DisplayList displayList = new DisplayList(list, stall, callbackId, argument);
        DisplayList.addDisplayList(displayList);
        log("The list " + displayList.toString());

        if (displayList.status == DisplayList.QUEUED)
            pspdisplay.get_instance().setDirty(true);

        Emulator.getProcessor().gpr[2] = displayList.id;
        DisplayList.Unlock();
    }

    public void sceGeListDeQueue(int qid) {
        DisplayList.Lock();
        // TODO if we render asynchronously, using another thread then we need to interupt it first
        if (DisplayList.removeDisplayList(qid)) {
            log("sceGeListDeQueue qid=" + qid);
            Emulator.getProcessor().gpr[2] = 0;
        } else {
            log("sceGeListDeQueue failed qid=" + qid);
            Emulator.getProcessor().gpr[2] = -1;
        }
        DisplayList.Unlock();
    }

    public void sceGeListUpdateStallAddr(int qid, int stallAddress) {
        DisplayList.Lock();
        DisplayList displayList = DisplayList.getDisplayList(qid);
        if (displayList != null) {
            // remove uncache bit
            stallAddress &= 0x3fffffff;

            log("sceGeListUpdateStallAddr qid=" + qid
                + " new stall addr " + String.format("%08x", stallAddress)
                + " " + ((stallAddress - displayList.stallAddress) / 4) + " new commands");

            displayList.stallAddress = stallAddress;
            if (displayList.pc != displayList.stallAddress) {
                displayList.status = DisplayList.QUEUED;
                pspdisplay.get_instance().setDirty(true);
            }

            Emulator.getProcessor().gpr[2] = 0;
        } else {
            log("sceGeListUpdateStallAddr qid="+ qid +" failed, no longer exists");
            Emulator.getProcessor().gpr[2] = -1;
        }
        DisplayList.Unlock();
    }

    /* Not sure if this is correct
    public void sceGeSetCallback(int cbdata_addr) {
        // TODO list of callbacks, for now we allow only 1
        if (cbid == -1) {
            cbdata = new PspGeCallbackData(Emulator.getMemory(), cbdata_addr);
            cbid = 1;
            Emulator.getProcessor().gpr[2] = cbid;
        } else {
            System.out.println("sceGeSetCallback failed");
            Emulator.getProcessor().gpr[2] = -1;
        }
    }

    public void sceGeUnsetCallback(int cbid) {
        // TODO list of callbacks, for now we allow only 1
        if (this.cbid == cbid) {
            cbdata = null;
            cbid = -1;
            Emulator.getProcessor().gpr[2] = 0;
        } else {
            System.out.println("sceGeUnsetCallback failed");
            Emulator.getProcessor().gpr[2] = -1;
        }
    }
    */

    private void log(String msg){
        System.out.println("sceGe DEBUG > " + msg);
    }


}
