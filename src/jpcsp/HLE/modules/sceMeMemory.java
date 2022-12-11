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
package jpcsp.HLE.modules;

import org.apache.log4j.Logger;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.Modules;
import jpcsp.mediaengine.MEEmulator;

public class sceMeMemory extends HLEModule {
    public static Logger log = Modules.getLogger("sceMeMemory");

    public boolean isAllocated(int addr) {
    	return MEEmulator.getInstance().isAllocated(addr);
    }

    @HLEFunction(nid = 0xC4EDA9F4, version = 150)
    public int sceMeCalloc(int num, int size) {
		int totalSize = num * size;
		int addr = MEEmulator.getInstance().malloc(totalSize);
		if (addr == 0) {
			return 0;
		}

		getMEMemory().memset(addr, (byte) 0, totalSize);

		return addr;
	}

    @HLEFunction(nid = 0x92D3BAA1, version = 150)
    public int sceMeMalloc(int size) {
    	return MEEmulator.getInstance().malloc(size);
    }

    @HLEFunction(nid = 0x6ED69327, version = 150)
    public void sceMeFree(int addr) {
    	MEEmulator.getInstance().free(addr);
    }
}
