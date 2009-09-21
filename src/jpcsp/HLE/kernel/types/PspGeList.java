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
package jpcsp.HLE.kernel.types;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import jpcsp.HLE.pspge;
import jpcsp.graphics.VideoEngine;
import static jpcsp.HLE.pspge.*;

public class PspGeList
{
    public int list_addr;
    public int stall_addr;
    public int cbid;
    public int arg_addr;
    public int context_addr; // pointer to 2k buffer for storing GE context, used as a paramater for the callbacks?

    public int base;
    public int pc;
    public int[] stack = new int[32];
    public int stackIndex;
    public int currentStatus;
    public int syncStatus;
    public int id;

    public int thid; // the thread we are blocking
    public boolean listHasFinished;

    public PspGeList(int list_addr, int stall_addr, int cbid, int arg_addr) {
        this.list_addr = list_addr;
        this.stall_addr = stall_addr;
        this.cbid = cbid;
        this.arg_addr = arg_addr;

        context_addr = (arg_addr != 0) ? arg_addr + 4 : 0;

        // nice spam
        //if (context_addr != 0)
        //    VideoEngine.log.warn("UNIMPLEMENTED: PspGeList GE context at 0x" + Integer.toHexString(context_addr));

        // check
        //base = 0x08000000; // old
        //base = 0x0;

        pc = list_addr;

        currentStatus = (pc == stall_addr) ? PSP_GE_LIST_STALL_REACHED : PSP_GE_LIST_QUEUED;
        syncStatus = currentStatus;
    }

    public void pushSignalCallback(int arg) {
        pspge.getInstance().triggerSignalCallback(cbid, arg);
    }

    public void pushFinishCallback(int arg) {
        pspge.getInstance().triggerFinishCallback(cbid, arg);
    }
}
