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
package jpcsp.HLE.modules271;

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.PspString;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelLMOption;
import jpcsp.HLE.Modules;

@HLELogging
public class ModuleMgrForUser extends jpcsp.HLE.modules150.ModuleMgrForUser {
    @HLEUnimplemented
    @HLEFunction(nid = 0xFEF27DC1, version = 271)
    // sceKernelLoadModuleDNAS
    public int ModuleMgrForUser_FEF27DC1() {
        return 0;
    }
    
    @HLEUnimplemented
    @HLEFunction(nid = 0xF2D8D1B4, version = 271)
    // sceKernelLoadModuleNpDrm
    public int ModuleMgrForUser_F2D8D1B4(PspString path, int flags, @CanBeNull TPointer optionAddr) {
        SceKernelLMOption lmOption;
        if (optionAddr.isNotNull()) {
            lmOption = new SceKernelLMOption();
            lmOption.read(optionAddr);
            if (log.isInfoEnabled()) {
                log.info(String.format("ModuleMgrForUser_F2D8D1B4 partition=%d, position=%d", lmOption.mpidText, lmOption.position));
            }
        }

        // SPRX modules can't be decrypted yet.
        if (!Modules.scePspNpDrm_userModule.getDisableDLCStatus()) {
            log.warn(String.format("ModuleMgrForUser_F2D8D1B4 detected encrypted DLC module: %s", path.getString()));
            return SceKernelErrors.ERROR_NPDRM_INVALID_PERM;
        }

        return Modules.ModuleMgrForUserModule.hleKernelLoadModule(path.getString(), flags, 0, false);
    }
}