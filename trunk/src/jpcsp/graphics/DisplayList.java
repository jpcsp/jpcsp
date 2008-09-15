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

import java.util.Iterator;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

import jpcsp.Emulator;

// Use the locks for reading/writing member variables and calling member methods
public class DisplayList {

    public static final int DONE = 0;
    public static final int QUEUED = 1;
    public static final int DRAWING_DONE = 2;
    public static final int STALL_REACHED = 3;
    public static final int CANCEL_DONE = 4;
    //sceGuSendList [int mode
    public static final int GU_TAIL = 0;
    public static final int GU_HEAD = 1;
    
    protected static Semaphore displayListLock = new Semaphore(1, true);

    private static HashMap<Integer, DisplayList> displayLists;
    private static int ids;

    public int base;
    public int pc;
    public int[] stack = new int[32];
    public int stackIndex;
    public int status;
    public int id;

    public int start;
    public int stallAddress;
    public final int callbackId;
    public final int arg;

    public DisplayList(int startList, int stall, int callbackId, int arg) {
        this.start = startList;
        this.stallAddress = stall;
        this.callbackId = callbackId;
        this.arg = arg;

        base = 0x08000000;
        pc = startList;
        stackIndex = 0;
        if (pc == stallAddress)
            status = DisplayList.STALL_REACHED;
        else
            status = DisplayList.QUEUED;
        //list.id = ids++;
    }

    public static synchronized void Initialise() {
        displayLists = new HashMap<Integer, DisplayList>();
        ids = 0;
    }

    public static synchronized void addDisplayList(DisplayList list) {
        // create the id outside of the constructor, inside a synchronized block so it is thread safe
        list.id = ids++;
        displayLists.put(list.id, list);
    }

    public static synchronized boolean removeDisplayList(int id) {
        return (displayLists.remove(id) != null);
    }

    public static synchronized DisplayList getDisplayList(int id) {
        return displayLists.get(id);
    }

    public static synchronized Iterator<DisplayList> iterator() {
        return displayLists.values().iterator();
    }
    
    public static void Lock() {
        try {
            displayListLock.acquire();
        } catch (InterruptedException e) {
        }
    }

    public static void Unlock() {
        displayListLock.release();
    }

    @Override
    public synchronized String toString() {
        return "id = " + id + ", start address = " + Integer.toHexString(start)
                + ", end address = " + Integer.toHexString(stallAddress)
                + ", initial command instruction = " + Integer.toHexString(Emulator.getMemory().read32(pc));
    }
}
