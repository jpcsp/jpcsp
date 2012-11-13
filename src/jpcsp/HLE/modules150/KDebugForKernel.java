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
package jpcsp.HLE.modules150;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.PspString;

import org.apache.log4j.Logger;

import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;

import jpcsp.Allegrex.CpuState;

@HLELogging
public class KDebugForKernel extends HLEModule {
    public static Logger log = Modules.getLogger("KDebugForKernel");
    protected static Logger kprintf = Logger.getLogger("kprintf");

    @Override
	public String getName() {
    	return "KDebugForKernel";
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
		return Modules.SysMemUserForUserModule.hleKernelPrintf(cpu, formatString, kprintf, "Kprintf");
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x5CE9838B, version = 150)
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
	public int sceKernelDebugEcho() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x7D1C74F0, version = 150)
	public int sceKernelDebugEchoSet() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x24C32559, version = 150)
	public int sceKernelDipsw() {
		return 0;
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

    @HLEUnimplemented
	@HLEFunction(nid = 0xE892D9A1, version = 150)
	public int KDebugForKernel_E892D9A1() {
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
}
