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
	public void installModule(HLEModuleManager mm, int version) {
		if (version >= 150) {

			mm.addFunction(0xACE23476, sceKernelCheckPspConfigFunction);
			mm.addFunction(0x7BE1421C, sceKernelCheckExecFileFunction);
			mm.addFunction(0xBF983EF2, sceKernelProbeExecutableObjectFunction);
			mm.addFunction(0x7068E6BA, sceKernelLoadExecutableObjectFunction);
			mm.addFunction(0xB4D6FECC, sceKernelApplyElfRelSectionFunction);
			mm.addFunction(0x54AB2675, sceKernelApplyPspRelSectionFunction);
			mm.addFunction(0x2952F5AC, sceKernelDcacheWBinvAllFunction);
			mm.addFunction(0xD8779AC6, sceKernelIcacheClearAllFunction);
			mm.addFunction(0x99A695F0, sceKernelRegisterLibraryFunction);
			mm.addFunction(0x5873A31F, sceKernelRegisterLibraryForUserFunction);
			mm.addFunction(0x0B464512, sceKernelReleaseLibraryFunction);
			mm.addFunction(0x9BAF90F6, sceKernelCanReleaseLibraryFunction);
			mm.addFunction(0x0E760DBA, sceKernelLinkLibraryEntriesFunction);
			mm.addFunction(0x0DE1F600, sceKernelLinkLibraryEntriesForUserFunction);
			mm.addFunction(0xDA1B09AA, sceKernelUnLinkLibraryEntriesFunction);
			mm.addFunction(0xC99DD47A, sceKernelQueryLoadCoreCBFunction);
			mm.addFunction(0x616FCCCD, sceKernelSetBootCallbackLevelFunction);
			mm.addFunction(0x52A86C21, sceKernelGetModuleFromUIDFunction);
			mm.addFunction(0xCD0F3BAC, sceKernelCreateModuleFunction);
			mm.addFunction(0x6B2371C2, sceKernelDeleteModuleFunction);
			mm.addFunction(0x8D8A8ACE, sceKernelAssignModuleFunction);
			mm.addFunction(0xAFF947D4, sceKernelCreateAssignModuleFunction);
			mm.addFunction(0xAE7C6E76, sceKernelRegisterModuleFunction);
			mm.addFunction(0x74CF001A, sceKernelReleaseModuleFunction);
			mm.addFunction(0xCF8A41B1, sceKernelFindModuleByNameFunction);
			mm.addFunction(0xFB8AE27D, sceKernelFindModuleByAddressFunction);
			mm.addFunction(0xCCE4A157, sceKernelFindModuleByUIDFunction);
			mm.addFunction(0x929B5C69, sceKernelGetModuleListWithAllocFunction);
			mm.addFunction(0x05D915DB, sceKernelGetModuleIdListForKernelFunction);

		}
	}

	@Override
	public void uninstallModule(HLEModuleManager mm, int version) {
		if (version >= 150) {

			mm.removeFunction(sceKernelCheckPspConfigFunction);
			mm.removeFunction(sceKernelCheckExecFileFunction);
			mm.removeFunction(sceKernelProbeExecutableObjectFunction);
			mm.removeFunction(sceKernelLoadExecutableObjectFunction);
			mm.removeFunction(sceKernelApplyElfRelSectionFunction);
			mm.removeFunction(sceKernelApplyPspRelSectionFunction);
			mm.removeFunction(sceKernelDcacheWBinvAllFunction);
			mm.removeFunction(sceKernelIcacheClearAllFunction);
			mm.removeFunction(sceKernelRegisterLibraryFunction);
			mm.removeFunction(sceKernelRegisterLibraryForUserFunction);
			mm.removeFunction(sceKernelReleaseLibraryFunction);
			mm.removeFunction(sceKernelCanReleaseLibraryFunction);
			mm.removeFunction(sceKernelLinkLibraryEntriesFunction);
			mm.removeFunction(sceKernelLinkLibraryEntriesForUserFunction);
			mm.removeFunction(sceKernelUnLinkLibraryEntriesFunction);
			mm.removeFunction(sceKernelQueryLoadCoreCBFunction);
			mm.removeFunction(sceKernelSetBootCallbackLevelFunction);
			mm.removeFunction(sceKernelGetModuleFromUIDFunction);
			mm.removeFunction(sceKernelCreateModuleFunction);
			mm.removeFunction(sceKernelDeleteModuleFunction);
			mm.removeFunction(sceKernelAssignModuleFunction);
			mm.removeFunction(sceKernelCreateAssignModuleFunction);
			mm.removeFunction(sceKernelRegisterModuleFunction);
			mm.removeFunction(sceKernelReleaseModuleFunction);
			mm.removeFunction(sceKernelFindModuleByNameFunction);
			mm.removeFunction(sceKernelFindModuleByAddressFunction);
			mm.removeFunction(sceKernelFindModuleByUIDFunction);
			mm.removeFunction(sceKernelGetModuleListWithAllocFunction);
			mm.removeFunction(sceKernelGetModuleIdListForKernelFunction);

		}
	}


	public void sceKernelCheckPspConfig(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceKernelCheckPspConfig [0xACE23476]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void sceKernelCheckExecFile(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceKernelCheckExecFile [0x7BE1421C]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void sceKernelProbeExecutableObject(Processor processor) {
		CpuState cpu = processor.cpu; 

		log.warn("Unimplemented NID function sceKernelProbeExecutableObject [0xBF983EF2]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void sceKernelLoadExecutableObject(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceKernelLoadExecutableObject [0x7068E6BA]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void sceKernelApplyElfRelSection(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceKernelApplyElfRelSection [0xB4D6FECC]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void sceKernelApplyPspRelSection(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceKernelApplyPspRelSection [0x54AB2675]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void sceKernelDcacheWBinvAll(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceKernelDcacheWBinvAll [0x2952F5AC]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

    public void sceKernelIcacheClearAll(Processor processor) {
        CpuState cpu = processor.cpu;

        log.trace("IGNORING:sceKernelIcacheClearAll");

        cpu.gpr[2] = 0;
    }

	public void sceKernelRegisterLibrary(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceKernelRegisterLibrary [0x99A695F0]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void sceKernelRegisterLibraryForUser(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceKernelRegisterLibraryForUser [0x5873A31F]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void sceKernelReleaseLibrary(Processor processor) {
		CpuState cpu = processor.cpu; 

		log.warn("Unimplemented NID function sceKernelReleaseLibrary [0x0B464512]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void sceKernelCanReleaseLibrary(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceKernelCanReleaseLibrary [0x9BAF90F6]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void sceKernelLinkLibraryEntries(Processor processor) {
		CpuState cpu = processor.cpu; 

		log.warn("Unimplemented NID function sceKernelLinkLibraryEntries [0x0E760DBA]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void sceKernelLinkLibraryEntriesForUser(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceKernelLinkLibraryEntriesForUser [0x0DE1F600]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void sceKernelUnLinkLibraryEntries(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceKernelUnLinkLibraryEntries [0xDA1B09AA]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void sceKernelQueryLoadCoreCB(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceKernelQueryLoadCoreCB [0xC99DD47A]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void sceKernelSetBootCallbackLevel(Processor processor) {
		CpuState cpu = processor.cpu; 

		log.warn("Unimplemented NID function sceKernelSetBootCallbackLevel [0x616FCCCD]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void sceKernelGetModuleFromUID(Processor processor) {
		CpuState cpu = processor.cpu; 

		log.warn("Unimplemented NID function sceKernelGetModuleFromUID [0x52A86C21]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void sceKernelCreateModule(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceKernelCreateModule [0xCD0F3BAC]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void sceKernelDeleteModule(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceKernelDeleteModule [0x6B2371C2]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void sceKernelAssignModule(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceKernelAssignModule [0x8D8A8ACE]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void sceKernelCreateAssignModule(Processor processor) {
		CpuState cpu = processor.cpu; 

		log.warn("Unimplemented NID function sceKernelCreateAssignModule [0xAFF947D4]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void sceKernelRegisterModule(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceKernelRegisterModule [0xAE7C6E76]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void sceKernelReleaseModule(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceKernelReleaseModule [0x74CF001A]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

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

	public void sceKernelGetModuleListWithAlloc(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceKernelGetModuleListWithAlloc [0x929B5C69]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void sceKernelGetModuleIdListForKernel(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceKernelGetModuleIdListForKernel [0x05D915DB]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public final HLEModuleFunction sceKernelCheckPspConfigFunction = new HLEModuleFunction("LoadCoreForKernel", "sceKernelCheckPspConfig") {
		@Override
		public final void execute(Processor processor) {
			sceKernelCheckPspConfig(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.LoadCoreForKernelModule.sceKernelCheckPspConfig(processor);";
		}
	};

	public final HLEModuleFunction sceKernelCheckExecFileFunction = new HLEModuleFunction("LoadCoreForKernel", "sceKernelCheckExecFile") {
		@Override
		public final void execute(Processor processor) {
			sceKernelCheckExecFile(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.LoadCoreForKernelModule.sceKernelCheckExecFile(processor);";
		}
	};

	public final HLEModuleFunction sceKernelProbeExecutableObjectFunction = new HLEModuleFunction("LoadCoreForKernel", "sceKernelProbeExecutableObject") {
		@Override
		public final void execute(Processor processor) {
			sceKernelProbeExecutableObject(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.LoadCoreForKernelModule.sceKernelProbeExecutableObject(processor);";
		}
	};

	public final HLEModuleFunction sceKernelLoadExecutableObjectFunction = new HLEModuleFunction("LoadCoreForKernel", "sceKernelLoadExecutableObject") {
		@Override
		public final void execute(Processor processor) {
			sceKernelLoadExecutableObject(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.LoadCoreForKernelModule.sceKernelLoadExecutableObject(processor);";
		}
	};

	public final HLEModuleFunction sceKernelApplyElfRelSectionFunction = new HLEModuleFunction("LoadCoreForKernel", "sceKernelApplyElfRelSection") {
		@Override
		public final void execute(Processor processor) {
			sceKernelApplyElfRelSection(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.LoadCoreForKernelModule.sceKernelApplyElfRelSection(processor);";
		}
	};

	public final HLEModuleFunction sceKernelApplyPspRelSectionFunction = new HLEModuleFunction("LoadCoreForKernel", "sceKernelApplyPspRelSection") {
		@Override
		public final void execute(Processor processor) {
			sceKernelApplyPspRelSection(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.LoadCoreForKernelModule.sceKernelApplyPspRelSection(processor);";
		}
	};

	public final HLEModuleFunction sceKernelDcacheWBinvAllFunction = new HLEModuleFunction("LoadCoreForKernel", "sceKernelDcacheWBinvAll") {
		@Override
		public final void execute(Processor processor) {
			sceKernelDcacheWBinvAll(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.LoadCoreForKernelModule.sceKernelDcacheWBinvAll(processor);";
		}
	};

	public final HLEModuleFunction sceKernelIcacheClearAllFunction = new HLEModuleFunction("LoadCoreForKernel", "sceKernelIcacheClearAll") {
		@Override
		public final void execute(Processor processor) {
			sceKernelIcacheClearAll(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.LoadCoreForKernelModule.sceKernelIcacheClearAll(processor);";
		}
	};

	public final HLEModuleFunction sceKernelRegisterLibraryFunction = new HLEModuleFunction("LoadCoreForKernel", "sceKernelRegisterLibrary") {
		@Override
		public final void execute(Processor processor) {
			sceKernelRegisterLibrary(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.LoadCoreForKernelModule.sceKernelRegisterLibrary(processor);";
		}
	};

	public final HLEModuleFunction sceKernelRegisterLibraryForUserFunction = new HLEModuleFunction("LoadCoreForKernel", "sceKernelRegisterLibraryForUser") {
		@Override
		public final void execute(Processor processor) {
			sceKernelRegisterLibraryForUser(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.LoadCoreForKernelModule.sceKernelRegisterLibraryForUser(processor);";
		}
	};

	public final HLEModuleFunction sceKernelReleaseLibraryFunction = new HLEModuleFunction("LoadCoreForKernel", "sceKernelReleaseLibrary") {
		@Override
		public final void execute(Processor processor) {
			sceKernelReleaseLibrary(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.LoadCoreForKernelModule.sceKernelReleaseLibrary(processor);";
		}
	};

	public final HLEModuleFunction sceKernelCanReleaseLibraryFunction = new HLEModuleFunction("LoadCoreForKernel", "sceKernelCanReleaseLibrary") {
		@Override
		public final void execute(Processor processor) {
			sceKernelCanReleaseLibrary(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.LoadCoreForKernelModule.sceKernelCanReleaseLibrary(processor);";
		}
	};

	public final HLEModuleFunction sceKernelLinkLibraryEntriesFunction = new HLEModuleFunction("LoadCoreForKernel", "sceKernelLinkLibraryEntries") {
		@Override
		public final void execute(Processor processor) {
			sceKernelLinkLibraryEntries(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.LoadCoreForKernelModule.sceKernelLinkLibraryEntries(processor);";
		}
	};

	public final HLEModuleFunction sceKernelLinkLibraryEntriesForUserFunction = new HLEModuleFunction("LoadCoreForKernel", "sceKernelLinkLibraryEntriesForUser") {
		@Override
		public final void execute(Processor processor) {
			sceKernelLinkLibraryEntriesForUser(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.LoadCoreForKernelModule.sceKernelLinkLibraryEntriesForUser(processor);";
		}
	};

	public final HLEModuleFunction sceKernelUnLinkLibraryEntriesFunction = new HLEModuleFunction("LoadCoreForKernel", "sceKernelUnLinkLibraryEntries") {
		@Override
		public final void execute(Processor processor) {
			sceKernelUnLinkLibraryEntries(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.LoadCoreForKernelModule.sceKernelUnLinkLibraryEntries(processor);";
		}
	};

	public final HLEModuleFunction sceKernelQueryLoadCoreCBFunction = new HLEModuleFunction("LoadCoreForKernel", "sceKernelQueryLoadCoreCB") {
		@Override
		public final void execute(Processor processor) {
			sceKernelQueryLoadCoreCB(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.LoadCoreForKernelModule.sceKernelQueryLoadCoreCB(processor);";
		}
	};

	public final HLEModuleFunction sceKernelSetBootCallbackLevelFunction = new HLEModuleFunction("LoadCoreForKernel", "sceKernelSetBootCallbackLevel") {
		@Override
		public final void execute(Processor processor) {
			sceKernelSetBootCallbackLevel(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.LoadCoreForKernelModule.sceKernelSetBootCallbackLevel(processor);";
		}
	};

	public final HLEModuleFunction sceKernelGetModuleFromUIDFunction = new HLEModuleFunction("LoadCoreForKernel", "sceKernelGetModuleFromUID") {
		@Override
		public final void execute(Processor processor) {
			sceKernelGetModuleFromUID(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.LoadCoreForKernelModule.sceKernelGetModuleFromUID(processor);";
		}
	};

	public final HLEModuleFunction sceKernelCreateModuleFunction = new HLEModuleFunction("LoadCoreForKernel", "sceKernelCreateModule") {
		@Override
		public final void execute(Processor processor) {
			sceKernelCreateModule(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.LoadCoreForKernelModule.sceKernelCreateModule(processor);";
		}
	};

	public final HLEModuleFunction sceKernelDeleteModuleFunction = new HLEModuleFunction("LoadCoreForKernel", "sceKernelDeleteModule") {
		@Override
		public final void execute(Processor processor) {
			sceKernelDeleteModule(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.LoadCoreForKernelModule.sceKernelDeleteModule(processor);";
		}
	};

	public final HLEModuleFunction sceKernelAssignModuleFunction = new HLEModuleFunction("LoadCoreForKernel", "sceKernelAssignModule") {
		@Override
		public final void execute(Processor processor) {
			sceKernelAssignModule(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.LoadCoreForKernelModule.sceKernelAssignModule(processor);";
		}
	};

	public final HLEModuleFunction sceKernelCreateAssignModuleFunction = new HLEModuleFunction("LoadCoreForKernel", "sceKernelCreateAssignModule") {
		@Override
		public final void execute(Processor processor) {
			sceKernelCreateAssignModule(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.LoadCoreForKernelModule.sceKernelCreateAssignModule(processor);";
		}
	};

	public final HLEModuleFunction sceKernelRegisterModuleFunction = new HLEModuleFunction("LoadCoreForKernel", "sceKernelRegisterModule") {
		@Override
		public final void execute(Processor processor) {
			sceKernelRegisterModule(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.LoadCoreForKernelModule.sceKernelRegisterModule(processor);";
		}
	};

	public final HLEModuleFunction sceKernelReleaseModuleFunction = new HLEModuleFunction("LoadCoreForKernel", "sceKernelReleaseModule") {
		@Override
		public final void execute(Processor processor) {
			sceKernelReleaseModule(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.LoadCoreForKernelModule.sceKernelReleaseModule(processor);";
		}
	};

	public final HLEModuleFunction sceKernelFindModuleByNameFunction = new HLEModuleFunction("LoadCoreForKernel", "sceKernelFindModuleByName") {
		@Override
		public final void execute(Processor processor) {
			sceKernelFindModuleByName(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.LoadCoreForKernelModule.sceKernelFindModuleByName(processor);";
		}
	};

	public final HLEModuleFunction sceKernelFindModuleByAddressFunction = new HLEModuleFunction("LoadCoreForKernel", "sceKernelFindModuleByAddress") {
		@Override
		public final void execute(Processor processor) {
			sceKernelFindModuleByAddress(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.LoadCoreForKernelModule.sceKernelFindModuleByAddress(processor);";
		}
	};

	public final HLEModuleFunction sceKernelFindModuleByUIDFunction = new HLEModuleFunction("LoadCoreForKernel", "sceKernelFindModuleByUID") {
		@Override
		public final void execute(Processor processor) {
			sceKernelFindModuleByUID(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.LoadCoreForKernelModule.sceKernelFindModuleByUID(processor);";
		}
	};

	public final HLEModuleFunction sceKernelGetModuleListWithAllocFunction = new HLEModuleFunction("LoadCoreForKernel", "sceKernelGetModuleListWithAlloc") {
		@Override
		public final void execute(Processor processor) {
			sceKernelGetModuleListWithAlloc(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.LoadCoreForKernelModule.sceKernelGetModuleListWithAlloc(processor);";
		}
	};

	public final HLEModuleFunction sceKernelGetModuleIdListForKernelFunction = new HLEModuleFunction("LoadCoreForKernel", "sceKernelGetModuleIdListForKernel") {
		@Override
		public final void execute(Processor processor) {
			sceKernelGetModuleIdListForKernel(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.LoadCoreForKernelModule.sceKernelGetModuleIdListForKernel(processor);";
		}
	};
}