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

import java.util.Arrays;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.Managers;

/**
 *
 * @author hli
 */
public class SceKernelUid {

    public int uid;
    public String name;
    public int attr;

    public SceKernelUid(String name, int attr) {
        if (-1 < (this.uid = Managers.uids.addObject(this))) { 
            this.name = name;
            this.attr = attr;
        }
    }

    public int getUid() {
        return uid;
    }

    public String getName(){
        return name;
    }
    
    public int getAttr(){
        return attr;
    }
    
    protected void memset(int address, byte c, int length) {
        Memory mem = Emulator.getMemory();
        byte[] all = mem.mainmemory.array();
        int offset = address - MemoryMap.START_RAM + mem.mainmemory.arrayOffset();
        Arrays.fill(all, offset, offset + length, c);
    }
}
