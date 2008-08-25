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
package jpcsp.graphics;

import jpcsp.Emulator;

public class DisplayList {

    public static final int DONE = 0;
    public static final int QUEUED = 1;
    public static final int DRAWING_DONE = 2;
    public static final int STALL_REACHED = 3;
    public static final int CANCEL_DONE = 4;
    //sceGuSendList [int mode
    public static final int GU_TAIL = 0;
    public static final int GU_HEAD = 1;

    private static int ids = 0;

    public int base;
    public int pc;
    public int[] stack = new int[32];
    public int stackIndex;
    public int id;

    public int start;
    public int stallAddress;
    public int callbackId;
    public int arg;

    public DisplayList(int startList, int stall, int callbackId, int arg){
        this.start = startList;
        this.stallAddress = stall;
        this.callbackId = callbackId;
        this.arg = arg;

        base = 0x08000000;
        pc = startList;
        stackIndex = 0;
        id = ++ids;
    }

    @Override
    public String toString(){
        return "id = " + id + ", start address = " + Integer.toHexString(start)
                + ", end address = " + Integer.toHexString(stallAddress)
                + ", initial command instruction = " + Integer.toHexString(Emulator.getMemory().read32(pc));
    }
}
