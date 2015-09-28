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
}
