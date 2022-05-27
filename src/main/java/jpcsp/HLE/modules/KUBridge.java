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
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.Modules;
import jpcsp.HLE.PspString;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.kernel.types.SceKernelLMOption;
import jpcsp.HLE.modules.ModuleMgrForUser.LoadModuleContext;
import jpcsp.hardware.Model;

public class KUBridge extends HLEModule {
    public static Logger log = Modules.getLogger("KUBridge");

    /*
     * Equivalent to sceKernelLoadModule()
     */
    @HLEFunction(nid = 0x4C25EA72, version = 150)
    public int kuKernelLoadModule(PspString path, int flags, @CanBeNull TPointer optionAddr) {
        SceKernelLMOption lmOption = null;
        if (optionAddr.isNotNull()) {
            lmOption = new SceKernelLMOption();
            lmOption.read(optionAddr);
            if (log.isInfoEnabled()) {
            	log.info(String.format("kuKernelLoadModule options: %s", lmOption));
            }
        }

        LoadModuleContext loadModuleContext = new LoadModuleContext();
        loadModuleContext.fileName = path.getString();
        loadModuleContext.flags = flags;
        loadModuleContext.lmOption = lmOption;
        loadModuleContext.needModuleInfo = true;
        loadModuleContext.allocMem = true;
        loadModuleContext.isSignChecked = Modules.ModuleMgrForUserModule.isSignChecked(path.getString());

        return Modules.ModuleMgrForUserModule.hleKernelLoadModule(loadModuleContext);
    }

    /*
     * Equivalent to sceKernelGetModel()
     */
    @HLEFunction(nid = 0x24331850, version = 150)
    public int kuKernelGetModel() {
		int result = Model.getGeneration() - 1;

		if (log.isDebugEnabled()) {
			log.debug(String.format("kuKernelGetModel returning %d(%s)", result, Model.getModelName()));
		}

		return result;
    }
}
