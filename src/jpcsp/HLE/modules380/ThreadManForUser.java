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

import jpcsp.HLE.HLEFunction;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

public class ThreadManForUser extends jpcsp.HLE.modules271.ThreadManForUser {
	@Override
	public void installModule(HLEModuleManager mm, int version) { mm.installModuleWithAnnotations(this, version); }

	@Override
	public void uninstallModule(HLEModuleManager mm, int version) { mm.uninstallModuleWithAnnotations(this, version); }

	@HLEFunction(nid = 0x19CFF145, version = 380)
	public void sceKernelCreateLwMutex(Processor processor) {
		int[] gpr = processor.cpu.gpr;
        if (IntrManager.getInstance().isInsideInterrupt()) {
            gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
		Managers.lwmutex.sceKernelCreateLwMutex(gpr[4], gpr[5], gpr[6], gpr[7], gpr[8]);
	}

	@HLEFunction(nid = 0x1AF94D03, version = 380)
	public void sceKernelDonateWakeupThread(Processor processor) {
		CpuState cpu = processor.cpu;

		log.debug("Unimplemented NID function sceKernelDonateWakeupThread [0x1AF94D03]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	@HLEFunction(nid = 0x31327F19, version = 380)
	public void ThreadManForUser_31327F19(Processor processor) {
		CpuState cpu = processor.cpu;

		log.debug("Unimplemented NID function ThreadManForUser_31327F19 [0x31327F19]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	@HLEFunction(nid = 0x4C145944, version = 380)
	public void sceKernelReferLwMutexStatusByID(Processor processor) {
        int[] gpr = processor.cpu.gpr;
		Managers.lwmutex.sceKernelReferLwMutexStatusByID(gpr[4], gpr[5]);
	}

	@HLEFunction(nid = 0x60107536, version = 380)
	public void sceKernelDeleteLwMutex(Processor processor) {
		int[] gpr = processor.cpu.gpr;
        if (IntrManager.getInstance().isInsideInterrupt()) {
            gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
		Managers.lwmutex.sceKernelDeleteLwMutex(gpr[4]);
	}

	@HLEFunction(nid = 0x71040D5C, version = 380)
	public void ThreadManForUser_71040D5C(Processor processor) {
		CpuState cpu = processor.cpu;

		log.debug("Unimplemented NID function ThreadManForUser_71040D5C [0x71040D5C]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	@HLEFunction(nid = 0x7CFF8CF3, version = 380)
	public void ThreadManForUser_7CFF8CF3(Processor processor) {
		CpuState cpu = processor.cpu;

		log.debug("Unimplemented NID function ThreadManForUser_7CFF8CF3 [0x7CFF8CF3]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	@HLEFunction(nid = 0xBEED3A47, version = 380)
	public void ThreadManForUser_BEED3A47(Processor processor) {
		CpuState cpu = processor.cpu;

		log.debug("Unimplemented NID function ThreadManForUser_BEED3A47 [0xBEED3A47]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

}