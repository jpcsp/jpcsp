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

import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

import org.apache.log4j.Logger;

public class sceNp implements HLEModule {

    protected static Logger log = Modules.getLogger("sceNp");

    @Override
    public String getName() {
        return "sceNp";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            /*
             * FIXME: The sceNp module uses a different
             * NID resolve for it's functions' names.
             * The public names reversed from several applications
             * and modules are as follows:
             *  - sceNpInit (sceNp_857B47D3)
             *  - sceNpTerm
             *  - sceNpGetNpId
             *  - sceNpGetOnlineId
             *  - sceNpGetUserProfile
             *  - sceNpGetMyLanguages
             *  - sceNpGetAccountRegion
             *  - sceNpGetContentRatingFlag
             *  - sceNpGetChatRestrictionFlag
             * Since the generated NIDs do not match the names, it's necessary
             * to find which nomencalture is being used for these functions
             * (e.g.: _x_sceNpInit).
             *
             */
            mm.addFunction(0x857B47D3, sceNp_857B47D3Function);
        }
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.removeFunction(sceNp_857B47D3Function);

        }
    }

    public void sceNp_857B47D3(Processor processor) {
        CpuState cpu = processor.cpu;

        log.info("sceNp_857B47D3");

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
    }

    public final HLEModuleFunction sceNp_857B47D3Function = new HLEModuleFunction("sceNp", "sceNp_857B47D3") {

        @Override
        public final void execute(Processor processor) {
            sceNp_857B47D3(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNpModule.sceNp_857B47D3(processor);";
        }
    };
}