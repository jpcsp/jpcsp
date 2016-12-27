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

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import jpcsp.Memory;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.SysMemUserForUser.SysMemInfo;

public class sceMeMemory extends HLEModule {
    public static Logger log = Modules.getLogger("sceMeMemory");
    private Map<Integer, SysMemInfo> allocated;

	@Override
	public void start() {
		allocated = new HashMap<Integer, SysMemUserForUser.SysMemInfo>();

		super.start();
	}

    @HLEFunction(nid = 0xC4EDA9F4, version = 150)
    public int sceMeCalloc(int num, int size) {
    	SysMemInfo info = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.KERNEL_PARTITION_ID, "sceMeCalloc", SysMemUserForUser.PSP_SMEM_Low, num * size, 0);
    	if (info.addr == 0) {
    		return 0;
    	}
    	Memory mem = Memory.getInstance();
    	mem.memset(info.addr, (byte) 0, info.size);

    	allocated.put(info.addr, info);

    	return info.addr;
    }

    @HLEFunction(nid = 0x92D3BAA1, version = 150)
    public int sceMeMalloc(int size) {
    	SysMemInfo info = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.KERNEL_PARTITION_ID, "sceMeCalloc", SysMemUserForUser.PSP_SMEM_Low, size, 0);
    	if (info.addr == 0) {
    		return 0;
    	}

    	allocated.put(info.addr, info);

    	return info.addr;
    }

    @HLEFunction(nid = 0x6ED69327, version = 150)
    public void sceMeFree(int addr) {
    	SysMemInfo info = allocated.remove(addr);
    	if (info != null) {
    		Modules.SysMemUserForUserModule.free(info);
    	}
    }
}
