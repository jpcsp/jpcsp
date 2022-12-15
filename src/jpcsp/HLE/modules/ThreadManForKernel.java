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
import static jpcsp.HLE.Modules.ThreadManForUserModule;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_KTLS_IS_FULL;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointerFunction;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.modules.SysMemUserForUser.SysMemInfo;

public class ThreadManForKernel extends HLEModule {
    public static Logger log = Modules.getLogger("ThreadManForKernel");
    private final KTLSEntry[] ktlsEntries = new KTLSEntry[16];
    private final Map<Integer, SysMemInfo[]> threadKTLSEntries = new HashMap<Integer, SysMemInfo[]>();

    private static class KTLSEntry {
    	private int size;
    	private TPointerFunction callback;
    	private int callbackArg;
    	private int gp;

    	public KTLSEntry(int size, TPointerFunction callback, int callbackArg) {
    		this.size = size;
    		this.callback = callback;
    		this.callbackArg = callbackArg;
    		gp = Emulator.getProcessor().cpu._gp;
		}

    	public void call(int addr) {
    		if (callback == null || callback.isNull()) {
    			return;
    		}

    		ThreadManForUserModule.executeCallback(callback.getAddress(), gp, null, addr, callbackArg);
    	}
    }

	@Override
	public void start() {
		for (int i = 0; i < ktlsEntries.length; i++) {
			ktlsEntries[i] = null;
		}
		threadKTLSEntries.clear();

		super.start();
	}

	public void freeKTLSEntries(int thid) {
		if (thid == 0) {
			thid = Modules.ThreadManForUserModule.getCurrentThreadID();
		}

		SysMemInfo[] threadKTLS = threadKTLSEntries.remove(thid);
		if (threadKTLS != null) {
			for (int i = 0; i < ktlsEntries.length; i++) {
				SysMemInfo sysMemInfo = threadKTLS[i];
				if (sysMemInfo != null) {
					if (ktlsEntries[i] != null) {
						ktlsEntries[i].call(sysMemInfo.addr);
					}
					Modules.SysMemUserForUserModule.free(sysMemInfo);
				}
			}
		}
	}

	/**
     * Setup the KTLS allocator.
     * 
     * @param size        The size of the memory to be allocated by sceKernelGetThreadKTLS
     * @param callback    The allocator callback
     * @param callbackArg User specified arg passed to the callback
     * @return            < 0 on error, allocation id on success
     */
    @HLEFunction(nid = 0x04E72261, version = 150)
    public int sceKernelAllocateKTLS(int size, TPointerFunction callback, int callbackArg) {
    	int result = ERROR_KERNEL_KTLS_IS_FULL;
    	for (int i = 0; i < ktlsEntries.length; i++) {
    		if (ktlsEntries[i] == null) {
    			ktlsEntries[i] = new KTLSEntry(size, callback, callbackArg);
    			result = i;
    			break;
    		}
    	}

    	return result;
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

    /**
     * Get the KTLS of the current thread.
     * 
     * @param id The allocation id returned from AllocateKTLS
     * @return   The current KTLS, NULL on error
     */
    @HLEFunction(nid = 0xA249EAAE, version = 150)
    public int sceKernelGetKTLS(int id) {
    	return sceKernelGetThreadKTLS(id, 0, 1);
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

    /**
     * Get the KTLS of a thread.
     * 
     * @param id   The allocation id returned from AllocateKTLS
     * @param thid The thread is, 0 for current thread
     * @param mode Perhaps? Sees to be set to 0 or 1
     * @return     The current KTLS, NULL on error
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x3AD875C3, version = 150)
    public int sceKernelGetThreadKTLS(int id, int thid, int mode) {
    	if (thid == 0) {
    		thid = Modules.ThreadManForUserModule.getCurrentThreadID();
    	}

    	KTLSEntry ktlsEntry = ktlsEntries[id];
    	SysMemInfo info = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.USER_PARTITION_ID, "KTLS", SysMemUserForUser.PSP_SMEM_Low, ktlsEntry.size, 0);
    	if (info == null) {
    		return 0;
    	}

    	getMemory().memset(info.addr, (byte) 0, ktlsEntry.size);

    	SysMemInfo[] threadKTLS = threadKTLSEntries.get(thid);
    	if (threadKTLS == null) {
    		threadKTLS = new SysMemInfo[ktlsEntries.length];
    		threadKTLSEntries.put(thid, threadKTLS);
    	}
    	threadKTLS[id] = info;

    	return info.addr;
    }

    /**
     * Free the KTLS allocator.
     * 
     * @param id The allocation id returned from AllocateKTLS
     * @return   < 0 on error
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0xD198B811, version = 150)
    public int sceKernelFreeKTLS(int id) {
    	ktlsEntries[id] = null;

    	return 0;
    }
}
