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
package jpcsp.HLE.modules380;

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.Processor;
import jpcsp.HLE.kernel.Managers;

@HLELogging
public class Kernel_Library extends jpcsp.HLE.modules150.Kernel_Library {
	@HLEFunction(nid = 0x15B6446B, version = 380, checkInsideInterrupt = true)
	public int sceKernelUnlockLwMutex(TPointer workAreaAddr, int count) {
		return Managers.lwmutex.sceKernelUnlockLwMutex(workAreaAddr, count);
	}

	@HLEFunction(nid = 0x1FC64E09, version = 380, checkInsideInterrupt = true)
	public int sceKernelLockLwMutexCB(TPointer workAreaAddr, int count, @CanBeNull TPointer32 timeoutAddr) {
		return Managers.lwmutex.sceKernelLockLwMutexCB(workAreaAddr, count, timeoutAddr);
	}

	@HLEFunction(nid = 0xBEA46419, version = 380, checkInsideInterrupt = true)
	public int sceKernelLockLwMutex(TPointer workAreaAddr, int count, @CanBeNull TPointer32 timeoutAddr) {
		return Managers.lwmutex.sceKernelLockLwMutex(workAreaAddr, count, timeoutAddr);
	}

	@HLEFunction(nid = 0xC1734599, version = 380)
	public int sceKernelReferLwMutexStatus(TPointer workAreaAddr, TPointer addr) {
		return Managers.lwmutex.sceKernelReferLwMutexStatus(workAreaAddr, addr);
	}

	@HLEFunction(nid = 0xDC692EE3, version = 380, checkInsideInterrupt = true)
	public int sceKernelTryLockLwMutex(TPointer workAreaAddr, int count) {
		return Managers.lwmutex.sceKernelTryLockLwMutex(workAreaAddr, count);
	}

    @HLEFunction(nid = 0x37431849, version = 380, checkInsideInterrupt = true)
    public int sceKernelTryLockLwMutex_600(TPointer workAreaAddr, int count) {
		return Managers.lwmutex.sceKernelTryLockLwMutex(workAreaAddr, count);
	}

    @HLEFunction(nid = 0x1839852A, version = 380)
    public int sceKernelMemcpy(TPointer dst, TPointer src, int length) {
		Processor.memory.memcpy(dst.getAddress(), src.getAddress(), length);

		return dst.getAddress();
	}
}