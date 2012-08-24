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
import org.apache.log4j.Logger;

import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;

import jpcsp.Allegrex.CpuState;

public class KDebugForKernel extends HLEModule {
    protected static Logger log = Modules.getLogger("KDebugForKernel");
    protected static Logger kprintf = Logger.getLogger("kprintf");

    @Override
	public String getName() { return "KDebugForKernel"; }
	
	@HLEFunction(nid = 0xE7A3874D, version = 150)
	public int sceKernelRegisterAssertHandler() {
		log.warn("Unimplemented sceKernelRegisterAssertHandler");

		return 0;
	}

	@HLEFunction(nid = 0x2FF4E9F9, version = 150)
	public int sceKernelAssert() {
		log.warn("Unimplemented sceKernelAssert");

		return 0;
	}

	@HLEFunction(nid = 0x9B868276, version = 150)
	public int sceKernelGetDebugPutchar() {
		log.warn("Unimplemented sceKernelGetDebugPutchar");

		return 0;
	}

	@HLEFunction(nid = 0xE146606D, version = 150)
	public int sceKernelRegisterDebugPutchar() {
		log.warn("Unimplemented sceKernelRegisterDebugPutchar");

		return 0;
	}

	@HLEFunction(nid = 0x7CEB2C09, version = 150)
	public int sceKernelRegisterKprintfHandler() {
		log.warn("Unimplemented sceKernelRegisterKprintfHandler");

		return 0;
	}

	@HLEFunction(nid = 0x84F370BC, version = 150)
	public int Kprintf(CpuState cpu) {
		return Modules.SysMemUserForUserModule.hleKernelPrintf(cpu, kprintf, "Kprintf");
	}

	@HLEFunction(nid = 0x5CE9838B, version = 150)
	public int sceKernelDebugWrite() {
		log.warn("Unimplemented sceKernelDebugWrite");

		return 0;
	}

	@HLEFunction(nid = 0x66253C4E, version = 150)
	public int sceKernelRegisterDebugWrite() {
		log.warn("Unimplemented sceKernelRegisterDebugWrite");

		return 0;
	}

	@HLEFunction(nid = 0xDBB5597F, version = 150)
	public int sceKernelDebugRead() {
		log.warn("Unimplemented sceKernelDebugRead");

		return 0;
	}

	@HLEFunction(nid = 0xE6554FDA, version = 150)
	public int sceKernelRegisterDebugRead() {
		log.warn("Unimplemented sceKernelRegisterDebugRead");

		return 0;
	}

	@HLEFunction(nid = 0xB9C643C9, version = 150)
	public int sceKernelDebugEcho() {
		log.warn("Unimplemented sceKernelDebugEcho");

		return 0;
	}

	@HLEFunction(nid = 0x7D1C74F0, version = 150)
	public int sceKernelDebugEchoSet() {
		log.warn("Unimplemented sceKernelDebugEchoSet");

		return 0;
	}

	@HLEFunction(nid = 0x24C32559, version = 150)
	public int sceKernelDipsw() {
		log.warn("Unimplemented sceKernelDipsw");

		return 0;
	}

	@HLEFunction(nid = 0xD636B827, version = 150)
	public int sceKernelDipswAll() {
		log.warn("Unimplemented sceKernelDipswAll");

		return 0;
	}

	@HLEFunction(nid = 0x5282DD5E, version = 150)
	public int sceKernelDipswSet() {
		log.warn("Unimplemented sceKernelDipswSet");

		return 0;
	}

	@HLEFunction(nid = 0xEE75658D, version = 150)
	public int sceKernelDipswClear() {
		log.warn("Unimplemented sceKernelDipswClear");

		return 0;
	}

	@HLEFunction(nid = 0x9F8703E4, version = 150)
	public int KDebugForKernel_9F8703E4() {
		log.warn("Unimplemented KDebugForKernel_9F8703E4");

		return 0;
	}

	@HLEFunction(nid = 0x333DCEC7, version = 150)
	public int KDebugForKernel_333DCEC7() {
		log.warn("Unimplemented KDebugForKernel_333DCEC7");

		return 0;
	}

	@HLEFunction(nid = 0xE892D9A1, version = 150)
	public int KDebugForKernel_E892D9A1() {
		log.warn("Unimplemented KDebugForKernel_E892D9A1");

		return 0;
	}

	@HLEFunction(nid = 0xA126F497, version = 150)
	public int KDebugForKernel_A126F497() {
		log.warn("Unimplemented KDebugForKernel_A126F497");

		return 0;
	}

	@HLEFunction(nid = 0xB7251823, version = 150)
	public int KDebugForKernel_B7251823() {
		log.warn("Unimplemented KDebugForKernel_B7251823");

		return 0;
	}
}
