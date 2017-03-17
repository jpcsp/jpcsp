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

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.Modules;
import jpcsp.HLE.PspString;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelLMOption;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.HLE.modules.SysMemUserForUser.SysMemInfo;

public class ModuleMgrForKernel extends HLEModule {
	public static Logger log = Modules.getLogger("ModuleMgrForKernel");
	private Set<String> modulesWithMemoryAllocated;

	@Override
	public void start() {
		modulesWithMemoryAllocated = new HashSet<>();

		super.start();
	}

	public boolean isMemoryAllocatedForModule(String moduleName) {
		if (modulesWithMemoryAllocated == null) {
			return false;
		}
		return modulesWithMemoryAllocated.contains(moduleName);
	}

	@HLEFunction(nid = 0xBA889C07, version = 150)
    public int sceKernelLoadModuleBuffer(TPointer buffer, int bufSize, int flags, @CanBeNull TPointer optionAddr) {
        SceKernelLMOption lmOption = null;
        if (optionAddr.isNotNull()) {
            lmOption = new SceKernelLMOption();
            lmOption.read(optionAddr);
            if (log.isInfoEnabled()) {
                log.info(String.format("sceKernelLoadModuleBuffer options: %s", lmOption));
            }
        }

        return Modules.ModuleMgrForUserModule.hleKernelLoadModule(buffer.toString(), flags, 0, buffer.getAddress(), bufSize, lmOption, false, true, true, 0);
    }

	/**
	 * Load a module with the VSH apitype.
	 *
	 * @param path        The path to the module to load.
	 * @param flags       Unused, always 0 . 
	 * @param optionAddr  Pointer to a mod_param_t structure. Can be NULL.
	 * @return
	 */
	@HLELogging(level = "info")
	@HLEFunction(nid = 0xD5DDAB1F, version = 150)
	public int sceKernelLoadModuleVSH(PspString path, int flags, @CanBeNull TPointer optionAddr) {
        SceKernelLMOption lmOption = null;
        if (optionAddr.isNotNull()) {
            lmOption = new SceKernelLMOption();
            lmOption.read(optionAddr);
            if (log.isInfoEnabled()) {
                log.info(String.format("sceKernelLoadModuleVSH options: %s", lmOption));
            }
        }

        return Modules.ModuleMgrForUserModule.hleKernelLoadModule(path.getString(), flags, 0, 0, 0, lmOption, false, true, true, 0);
	}

	@HLEFunction(nid = 0xD86DD11B, version = 150)
	public int sceKernelSearchModuleByName(PspString name) {
		SceModule module = Managers.modules.getModuleByName(name.getString());
		if (module == null) {
			return SceKernelErrors.ERROR_KERNEL_UNKNOWN_MODULE;
		}

		return module.modid;
	}

    @HLEFunction(nid = 0x939E4270, version = 150, checkInsideInterrupt = true)
    public int sceKernelLoadModule_660(PspString path, int flags, @CanBeNull TPointer optionAddr) {
    	return Modules.ModuleMgrForUserModule.sceKernelLoadModule(path, flags, optionAddr);
    }

    @HLEFunction(nid = 0x387E3CA9, version = 150, checkInsideInterrupt = true)
    public int sceKernelUnloadModule_660(int uid) {
    	return Modules.ModuleMgrForUserModule.sceKernelUnloadModule(uid);
    }

    @HLEFunction(nid = 0x3FF74DF1, version = 150, checkInsideInterrupt = true)
    public int sceKernelStartModule_660(int uid, int argSize, @CanBeNull TPointer argp, @CanBeNull TPointer32 statusAddr, @CanBeNull TPointer optionAddr) {
    	return Modules.ModuleMgrForUserModule.sceKernelStartModule(uid, argSize, argp, statusAddr, optionAddr);
    }

    @HLEFunction(nid = 0xE5D6087B, version = 150, checkInsideInterrupt = true)
    public int sceKernelStopModule_660(int uid, int argSize, @CanBeNull TPointer argp, @CanBeNull TPointer32 statusAddr, @CanBeNull TPointer optionAddr) {
    	return Modules.ModuleMgrForUserModule.sceKernelStopModule(uid, argSize, argp, statusAddr, optionAddr);
    }

    @HLEFunction(nid = 0xD4EE2D26, version = 150, checkInsideInterrupt = true)
    public int sceKernelLoadModuleToBlock(PspString path, int blockId, TPointer32 unknown1, int unknown2, @CanBeNull TPointer optionAddr) {
        SceKernelLMOption lmOption = null;
        if (optionAddr.isNotNull()) {
            lmOption = new SceKernelLMOption();
            lmOption.read(optionAddr);
            if (log.isInfoEnabled()) {
            	log.info(String.format("sceKernelLoadModuleToBlock options: %s", lmOption));
            }
        }

        SysMemInfo sysMemInfo = Modules.SysMemUserForUserModule.getSysMemInfo(blockId);
        if (sysMemInfo == null) {
        	return -1;
        }

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceKernelLoadModuleToBlock sysMemInfo=%s", sysMemInfo));
        }

        modulesWithMemoryAllocated.add(path.getString());

        return Modules.ModuleMgrForUserModule.hleKernelLoadModule(path.getString(), 0, 0, 0, 0, lmOption, false, true, false, sysMemInfo.addr);
    }
}
