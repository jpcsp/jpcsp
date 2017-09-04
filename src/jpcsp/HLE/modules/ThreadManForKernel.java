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

import static jpcsp.HLE.HLEModuleManager.HLESyscallNid;

import org.apache.log4j.Logger;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.kernel.Managers;

public class ThreadManForKernel extends HLEModule {
    public static Logger log = Modules.getLogger("ThreadManForKernel");

    @HLEUnimplemented
    @HLEFunction(nid = 0x04E72261, version = 150)
    public int sceKernelAllocateKTLS(int size, TPointer callback, int callbackArg) {
    	int id = Modules.ThreadManForUserModule.sceKernelCreateTlspl("KTLS", SysMemUserForUser.KERNEL_PARTITION_ID, SysMemUserForUser.PSP_SMEM_Low, size, 32, TPointer.NULL);

    	return id;
    }

    @HLEFunction(nid = 0x4FE44D5E, version = 150)
    public int sceKernelCheckThreadKernelStack() {
    	return 4096;
    }

    /**
     * Checks if the current thread is a usermode thread.
     * 
     * @return 0 if kernel, 1 if user, < 0 on error
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x85A2A5BF, version = 150)
    public int sceKernelIsUserModeThread() {
    	return 1;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA249EAAE, version = 150)
    public int sceKernelGetKTLS(int id) {
    	return Modules.Kernel_LibraryModule.sceKernel_FA835CDE(id);
    }

    /**
     * This HLE syscall is used when reading the hardware register 0xBC600000.
     * It is equivalent to reading the system time.
     * 
     * @return the same value as returned by sceKernelGetSystemTimeLow().
     */
    @HLEFunction(nid = HLESyscallNid, version = 150)
    public int hleKernelGetSystemTimeLow() {
    	return Managers.systime.sceKernelGetSystemTimeLow();
    }
}
