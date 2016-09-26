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

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.SysMemUserForUser.SysMemInfo;

import org.apache.log4j.Logger;

public class sceNetIfhandle extends HLEModule {
    public static Logger log = Modules.getLogger("sceNetIfhandle");
    private int netDropRate;
    private int netDropDuration;
    private HashMap<Integer, SysMemInfo> allocatedMemory;

	@Override
	public void start() {
		allocatedMemory = new HashMap<Integer, SysMemUserForUser.SysMemInfo>();

		super.start();
	}

	@HLEFunction(nid = 0xC80181A2, version = 150, checkInsideInterrupt = true)
    public int sceNetGetDropRate(@CanBeNull TPointer32 dropRateAddr, @CanBeNull TPointer32 dropDurationAddr) {
        dropRateAddr.setValue(netDropRate);
        dropDurationAddr.setValue(netDropDuration);

        return 0;
    }

    @HLEFunction(nid = 0xFD8585E1, version = 150, checkInsideInterrupt = true)
    public int sceNetSetDropRate(int dropRate, int dropDuration) {
        netDropRate = dropRate;
        netDropDuration = dropDuration;

        return 0;
    }

    @HLEFunction(nid = 0x15CFE3C0, version = 150)
    public int sceNetMallocInternal(int size) {
    	SysMemInfo info = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.USER_PARTITION_ID, "sceNetMallocInternal", SysMemUserForUser.PSP_SMEM_Low, size, 0);

    	if (info == null) {
    		return 0;
    	}

    	allocatedMemory.put(info.addr, info);

    	return info.addr;
    }

    @HLEFunction(nid = 0x76BAD213, version = 150)
    public int sceNetFreeInternal(int memory) {
    	SysMemInfo info = allocatedMemory.remove(memory);
    	if (info != null) {
    		Modules.SysMemUserForUserModule.free(info);
    	}

    	return 0;
    }
}