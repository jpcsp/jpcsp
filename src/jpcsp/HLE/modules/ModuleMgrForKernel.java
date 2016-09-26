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

import org.apache.log4j.Logger;

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.PspString;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.kernel.types.SceKernelLMOption;

public class ModuleMgrForKernel extends HLEModule {
	public static Logger log = Modules.getLogger("ModuleMgrForKernel");

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

        return Modules.ModuleMgrForUserModule.hleKernelLoadModule(buffer.toString(), flags, 0, buffer.getAddress(), bufSize, lmOption, false, true);
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

        return Modules.ModuleMgrForUserModule.hleKernelLoadModule(path.getString(), flags, 0, 0, 0, lmOption, false, true);
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xD86DD11B, version = 150)
	public int sceKernelSearchModuleByName(PspString name) {
		return 0;
	}
}
