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

import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.PspString;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.types.SceModule;

import org.apache.log4j.Logger;

public class LoadCoreForKernel extends HLEModule {
    public static Logger log = Modules.getLogger("LoadCoreForKernel");

    @HLEUnimplemented
	@HLEFunction(nid = 0xACE23476, version = 150)
	public int sceKernelCheckPspConfig() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x7BE1421C, version = 150)
	public int sceKernelCheckExecFile() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xBF983EF2, version = 150)
	public int sceKernelProbeExecutableObject() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x7068E6BA, version = 150)
	public int sceKernelLoadExecutableObject() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xB4D6FECC, version = 150)
	public int sceKernelApplyElfRelSection() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x54AB2675, version = 150)
	public int sceKernelApplyPspRelSection() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x2952F5AC, version = 150)
	public int sceKernelDcacheWBinvAll() {
		return 0;
	}

    @HLELogging(level="trace")
    @HLEFunction(nid = 0xD8779AC6, version = 150)
    public int sceKernelIcacheClearAll() {
        return 0;
    }

    @HLEUnimplemented
	@HLEFunction(nid = 0x99A695F0, version = 150)
	public int sceKernelRegisterLibrary() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x5873A31F, version = 150)
	public int sceKernelRegisterLibraryForUser() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x0B464512, version = 150)
	public int sceKernelReleaseLibrary() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x9BAF90F6, version = 150)
	public int sceKernelCanReleaseLibrary() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x0E760DBA, version = 150)
	public int sceKernelLinkLibraryEntries() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x0DE1F600, version = 150)
	public int sceKernelLinkLibraryEntriesForUser() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xDA1B09AA, version = 150)
	public int sceKernelUnLinkLibraryEntries() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xC99DD47A, version = 150)
	public int sceKernelQueryLoadCoreCB() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x616FCCCD, version = 150)
	public int sceKernelSetBootCallbackLevel() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x52A86C21, version = 150)
	public int sceKernelGetModuleFromUID() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xCD0F3BAC, version = 150)
	public int sceKernelCreateModule() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x6B2371C2, version = 150)
	public int sceKernelDeleteModule() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x8D8A8ACE, version = 150)
	public int sceKernelAssignModule() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xAFF947D4, version = 150)
	public int sceKernelCreateAssignModule() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xAE7C6E76, version = 150)
	public int sceKernelRegisterModule() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x74CF001A, version = 150)
	public int sceKernelReleaseModule() {
		return 0;
	}

    @HLEFunction(nid = 0xCF8A41B1, version = 150)
    public int sceKernelFindModuleByName(PspString moduleName) {
        SceModule module = Managers.modules.getModuleByName(moduleName.getString());
        if (module == null) {
            log.warn(String.format("sceKernelFindModuleByName not found moduleName=%s", moduleName));
            return 0; // return NULL
        }

        if (!Modules.ThreadManForUserModule.isKernelMode()) {
            log.warn("kernel mode required (sceKernelFindModuleByName)");
        }

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceKernelFindModuleByName returning 0x%08X", module.address));
        }

        return module.address;
    }

    @HLEFunction(nid = 0xFB8AE27D, version = 150)
    public int sceKernelFindModuleByAddress(TPointer address) {
        SceModule module = Managers.modules.getModuleByAddress(address.getAddress());
        if (module == null) {
            log.warn(String.format("sceKernelFindModuleByAddress not found module address=%s", address));
            return 0; // return NULL
        }

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceKernelFindModuleByAddress found module '%s'", module.modname));
        }

        if (!Modules.ThreadManForUserModule.isKernelMode()) {
            log.warn("kernel mode required (sceKernelFindModuleByAddress)");
        }

        return module.address;
	}

    @HLEFunction(nid = 0xCCE4A157, version = 150)
    public int sceKernelFindModuleByUID(int uid) {
        SceModule module = Managers.modules.getModuleByUID(uid);
        if (module == null) {
            log.warn(String.format("sceKernelFindModuleByUID not found module uid=0x%X", uid));
            return 0; // return NULL
        }

        // The pspsdk is not properly handling module exports with a size > 4.
        // See
        //    pspSdkFindExport()
        // in
        //    https://github.com/pspdev/pspsdk/blob/master/src/sdk/fixup.c
        // which is assuming that all module exports have a size==4 (i.e. 16 bytes).
        // This code is leading to an invalid memory access when processing the exports
        // from real PSP modules, which do have exports with a size==5.
        // Ban these modules in a case of the homebrew.
        if (RuntimeContext.isHomebrew()) {
        	String[] bannedModules = {
        			"sceNet_Library",
        			"sceNetInet_Library",
        			"sceNetApctl_Library",
        			"sceNetResolver_Library"
        	};
        	for (String bannedModule : bannedModules) {
        		if (bannedModule.equals(module.modname)) {
        			if (log.isDebugEnabled()) {
        				log.debug(String.format("sceKernelFindModuleByUID banning module '%s' for a homebrew", module.modname));
        			}
            		return 0; // NULL
        		}
        	}
        }

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceKernelFindModuleByUID found module '%s'", module.modname));
        }

        if (!Modules.ThreadManForUserModule.isKernelMode()) {
            log.warn("kernel mode required (sceKernelFindModuleByUID)");
        }

        return module.address;
    }

    @HLEUnimplemented
	@HLEFunction(nid = 0x929B5C69, version = 150)
	public int sceKernelGetModuleListWithAlloc() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x05D915DB, version = 150)
	public int sceKernelGetModuleIdListForKernel() {
		return 0;
	}
}