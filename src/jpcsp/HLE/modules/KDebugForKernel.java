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

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.PspString;
import jpcsp.HLE.TPointer;

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_ERROR;
import static jpcsp.util.Utilities.hasBit;

import org.apache.log4j.Logger;

import jpcsp.HLE.Modules;
import jpcsp.Allegrex.CpuState;

public class KDebugForKernel extends HLEModule {
    public static Logger log = Modules.getLogger("KDebugForKernel");
    protected static Logger kprintf = Logger.getLogger("kprintf");
    private long dispsw;
    private TPointer sm1Operations;
    private int deci2pOperations;

	@Override
	public void start() {
		dispsw = 0L;
		sm1Operations = TPointer.NULL;
		deci2pOperations = 0;

		super.start();
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xE7A3874D, version = 150)
	public int sceKernelRegisterAssertHandler() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x2FF4E9F9, version = 150)
	public int sceKernelAssert() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x9B868276, version = 150)
	public int sceKernelGetDebugPutchar() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xE146606D, version = 150)
	public int sceKernelRegisterDebugPutchar() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x7CEB2C09, version = 150)
	public int sceKernelRegisterKprintfHandler() {
		return 0;
	}

	@HLEFunction(nid = 0x84F370BC, version = 150)
	public int Kprintf(CpuState cpu, PspString formatString) {
		return Modules.SysMemUserForUserModule.hleKernelPrintf(cpu, formatString, kprintf);
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x5CE9838B, version = 150)
	@HLEFunction(nid = 0x2214799B, version = 660)
	public int sceKernelDebugWrite() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x66253C4E, version = 150)
	public int sceKernelRegisterDebugWrite() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xDBB5597F, version = 150)
	@HLEFunction(nid = 0xD4EC38C1, version = 660)
	public int sceKernelDebugRead() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xE6554FDA, version = 150)
	public int sceKernelRegisterDebugRead() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xB9C643C9, version = 150)
	@HLEFunction(nid = 0xE8FE3EE3, version = 660)
	public int sceKernelDebugEcho() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x7D1C74F0, version = 150)
	public int sceKernelDebugEchoSet() {
		return 0;
	}

	@HLEFunction(nid = 0x24C32559, version = 150)
	@HLEFunction(nid = 0x86010FCB, version = 660)
	public int sceKernelDipsw(int bit) {
		if (bit < 0 || bit >= 64) {
			return ERROR_KERNEL_ERROR;
		}

		return ((int) (dispsw >> bit)) & 0x1;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xD636B827, version = 150)
	public int sceKernelDipswAll() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x5282DD5E, version = 150)
	public int sceKernelDipswSet() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xEE75658D, version = 150)
	public int sceKernelDipswClear() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x9F8703E4, version = 150)
	public int KDebugForKernel_9F8703E4() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x333DCEC7, version = 150)
	public int KDebugForKernel_333DCEC7() {
		return 0;
	}

	@HLEFunction(nid = 0xE892D9A1, version = 150)
	public int sceKernelSm1ReferOperations() {
		return sm1Operations.getAddress();
	}

	@HLEFunction(nid = 0x02668C61, version = 150)
	public int sceKernelSm1RegisterOperations(@CanBeNull @BufferInfo(lengthInfo = LengthInfo.fixedLength, length = 60, usage = Usage.in) TPointer sm1Operations) {
    	this.sm1Operations = sm1Operations;

    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xA126F497, version = 150)
	public int KDebugForKernel_A126F497() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xB7251823, version = 150)
	public int KDebugForKernel_B7251823() {
		return 0;
	}

	@HLEFunction(nid = 0x47570AC5, version = 150)
	public boolean sceKernelIsToolMode() {
		return hasBit(dispsw, 30) || hasBit(dispsw, 28);
	}

	@HLEFunction(nid = 0x27B23800, version = 150)
	public boolean sceKernelIsUMDMode() {
		return !sceKernelIsDVDMode();
	}

	@HLEFunction(nid = 0xB41E2430, version = 150)
	public boolean sceKernelIsDVDMode() {
		return false;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xACF427DC, version = 150)
	public int sceKernelIsDevelopmentToolMode() {
		return 0;
	}

	@HLEFunction(nid = 0xF339073C, version = 150)
	public int sceKernelDeci2pReferOperations() {
		return deci2pOperations;
	}

	@HLEFunction(nid = 0x604276EB, version = 150)
	public int sceKernelDeci2pRegisterOperations(int deci2pOperations) {
    	this.deci2pOperations = deci2pOperations;

    	return 0;
	}

	@HLEFunction(nid = 0x6CB0BDA4, version = 150)
	public int sceKernelDipswHigh32() {
		return (int) (dispsw >>> 32);
	}

	@HLEFunction(nid = 0x43F0F8AB, version = 150)
	public int sceKernelDipswLow32() {
		return (int) dispsw;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x568DCD25, version = 150)
	public int sceKernelDipswCpTime() {
    	// Has no parameters
		return 0;
	}
}
