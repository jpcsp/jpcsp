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
import jpcsp.Emulator;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class LoadCoreForKernel implements HLEModule {
    private static Logger log = Modules.getLogger("LoadCoreForKernel");

	@Override
	public String getName() { return "LoadCoreForKernel"; }

	@Override
	public void installModule(HLEModuleManager mm, int version) { mm.installModuleWithAnnotations(this, version); }

	@Override
	public void uninstallModule(HLEModuleManager mm, int version) { mm.uninstallModuleWithAnnotations(this, version); }


	@HLEFunction(nid = 0xACE23476, version = 150)
	public void sceKernelCheckPspConfig(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceKernelCheckPspConfig [0xACE23476]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	@HLEFunction(nid = 0x7BE1421C, version = 150)
	public void sceKernelCheckExecFile(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceKernelCheckExecFile [0x7BE1421C]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	@HLEFunction(nid = 0xBF983EF2, version = 150)
	public void sceKernelProbeExecutableObject(Processor processor) {
		CpuState cpu = processor.cpu; 

		log.warn("Unimplemented NID function sceKernelProbeExecutableObject [0xBF983EF2]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	@HLEFunction(nid = 0x7068E6BA, version = 150)
	public void sceKernelLoadExecutableObject(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceKernelLoadExecutableObject [0x7068E6BA]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	@HLEFunction(nid = 0xB4D6FECC, version = 150)
	public void sceKernelApplyElfRelSection(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceKernelApplyElfRelSection [0xB4D6FECC]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	@HLEFunction(nid = 0x54AB2675, version = 150)
	public void sceKernelApplyPspRelSection(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceKernelApplyPspRelSection [0x54AB2675]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	@HLEFunction(nid = 0x2952F5AC, version = 150)
	public void sceKernelDcacheWBinvAll(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceKernelDcacheWBinvAll [0x2952F5AC]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

    @HLEFunction(nid = 0xD8779AC6, version = 150)
    public void sceKernelIcacheClearAll(Processor processor) {
        CpuState cpu = processor.cpu;

        log.trace("IGNORING:sceKernelIcacheClearAll");

        cpu.gpr[2] = 0;
    }

	@HLEFunction(nid = 0x99A695F0, version = 150)
	public void sceKernelRegisterLibrary(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceKernelRegisterLibrary [0x99A695F0]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	@HLEFunction(nid = 0x5873A31F, version = 150)
	public void sceKernelRegisterLibraryForUser(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceKernelRegisterLibraryForUser [0x5873A31F]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	@HLEFunction(nid = 0x0B464512, version = 150)
	public void sceKernelReleaseLibrary(Processor processor) {
		CpuState cpu = processor.cpu; 

		log.warn("Unimplemented NID function sceKernelReleaseLibrary [0x0B464512]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	@HLEFunction(nid = 0x9BAF90F6, version = 150)
	public void sceKernelCanReleaseLibrary(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceKernelCanReleaseLibrary [0x9BAF90F6]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	@HLEFunction(nid = 0x0E760DBA, version = 150)
	public void sceKernelLinkLibraryEntries(Processor processor) {
		CpuState cpu = processor.cpu; 

		log.warn("Unimplemented NID function sceKernelLinkLibraryEntries [0x0E760DBA]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	@HLEFunction(nid = 0x0DE1F600, version = 150)
	public void sceKernelLinkLibraryEntriesForUser(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceKernelLinkLibraryEntriesForUser [0x0DE1F600]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	@HLEFunction(nid = 0xDA1B09AA, version = 150)
	public void sceKernelUnLinkLibraryEntries(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceKernelUnLinkLibraryEntries [0xDA1B09AA]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	@HLEFunction(nid = 0xC99DD47A, version = 150)
	public void sceKernelQueryLoadCoreCB(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceKernelQueryLoadCoreCB [0xC99DD47A]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	@HLEFunction(nid = 0x616FCCCD, version = 150)
	public void sceKernelSetBootCallbackLevel(Processor processor) {
		CpuState cpu = processor.cpu; 

		log.warn("Unimplemented NID function sceKernelSetBootCallbackLevel [0x616FCCCD]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	@HLEFunction(nid = 0x52A86C21, version = 150)
	public void sceKernelGetModuleFromUID(Processor processor) {
		CpuState cpu = processor.cpu; 

		log.warn("Unimplemented NID function sceKernelGetModuleFromUID [0x52A86C21]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	@HLEFunction(nid = 0xCD0F3BAC, version = 150)
	public void sceKernelCreateModule(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceKernelCreateModule [0xCD0F3BAC]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	@HLEFunction(nid = 0x6B2371C2, version = 150)
	public void sceKernelDeleteModule(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceKernelDeleteModule [0x6B2371C2]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	@HLEFunction(nid = 0x8D8A8ACE, version = 150)
	public void sceKernelAssignModule(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceKernelAssignModule [0x8D8A8ACE]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	@HLEFunction(nid = 0xAFF947D4, version = 150)
	public void sceKernelCreateAssignModule(Processor processor) {
		CpuState cpu = processor.cpu; 

		log.warn("Unimplemented NID function sceKernelCreateAssignModule [0xAFF947D4]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	@HLEFunction(nid = 0xAE7C6E76, version = 150)
	public void sceKernelRegisterModule(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceKernelRegisterModule [0xAE7C6E76]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	@HLEFunction(nid = 0x74CF001A, version = 150)
	public void sceKernelReleaseModule(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceKernelReleaseModule [0x74CF001A]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

    @HLEFunction(nid = 0xCF8A41B1, version = 150)
    public void sceKernelFindModuleByName(Processor processor) {
        CpuState cpu = processor.cpu;

        int modulename_addr = cpu.gpr[4];
        String name = Utilities.readStringZ(modulename_addr);

        log.debug("sceKernelFindModuleByName name='" + name + "'");

        SceModule module = Managers.modules.getModuleByName(name);
        if (module != null) {
            cpu.gpr[2] = module.address;
        } else {
            log.warn("sceKernelFindModuleByName not found module name='" + name + "'");
            cpu.gpr[2] = 0; // return NULL
        }

        // we still execute the function normally, so user can click run again
        if (!Modules.ThreadManForUserModule.isKernelMode()) {
            log.error("kernel mode required (sceKernelFindModuleByName)");
            Emulator.PauseEmu();
        }
    }

    @HLEFunction(nid = 0xFB8AE27D, version = 150)
    public void sceKernelFindModuleByAddress(Processor processor) {
        CpuState cpu = processor.cpu;

        int address = cpu.gpr[4];

        log.debug("sceKernelFindModuleByAddress address=" + String.format("0x%08X", address));

        SceModule module = Managers.modules.getModuleByAddress(address);
        if (module != null) {
            log.debug("sceKernelFindModuleByAddress found module '" + module.modname + "'");
            cpu.gpr[2] = module.address;
        } else {
            log.warn("sceKernelFindModuleByAddress not found module address=0x" + Integer.toHexString(address));
            cpu.gpr[2] = 0; // return NULL
        }

        // we still execute the function normally, so user can click run again
        if (!Modules.ThreadManForUserModule.isKernelMode()) {
            log.error("kernel mode required (sceKernelFindModuleByAddress)");
            Emulator.PauseEmu();
        }
    }

    @HLEFunction(nid = 0xCCE4A157, version = 150)
    public void sceKernelFindModuleByUID(Processor processor) {
        CpuState cpu = processor.cpu; 

        int uid = cpu.gpr[4];

        log.debug("sceKernelFindModuleByUID uid=0x" + Integer.toHexString(uid));

        SceModule module = Managers.modules.getModuleByUID(uid);
        if (module != null) {
            log.debug("sceKernelFindModuleByUID found module '" + module.modname + "'");
            cpu.gpr[2] = module.address;
        } else {
            log.warn("sceKernelFindModuleByUID not found module uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = 0; // return NULL
        }

        // we still execute the function normally, so user can click run again
        if (!Modules.ThreadManForUserModule.isKernelMode()) {
            log.error("kernel mode required (sceKernelFindModuleByUID)");
            Emulator.PauseEmu();
        }
    }

	@HLEFunction(nid = 0x929B5C69, version = 150)
	public void sceKernelGetModuleListWithAlloc(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceKernelGetModuleListWithAlloc [0x929B5C69]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	@HLEFunction(nid = 0x05D915DB, version = 150)
	public void sceKernelGetModuleIdListForKernel(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceKernelGetModuleIdListForKernel [0x05D915DB]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

}