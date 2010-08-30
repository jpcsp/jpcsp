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
package jpcsp.HLE.modules200;

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_UMD_NOT_READY;

import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

public class sceUmdUser extends jpcsp.HLE.modules150.sceUmdUser {
    protected boolean umdAllowReplace;

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        super.installModule(mm, version);

        if (version >= 200) {

            mm.addFunction(0x87533940, sceUmdReplaceProhibitFunction);
            mm.addFunction(0xCBE9F02A, sceUmdReplacePermitFunction);

        }
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        super.uninstallModule(mm, version);

        if (version >= 200) {

            mm.removeFunction(sceUmdReplaceProhibitFunction);
            mm.removeFunction(sceUmdReplacePermitFunction);

        }
    }

    public void sceUmdReplaceProhibit(Processor processor) {
        CpuState cpu = processor.cpu;

        if(log.isDebugEnabled()) {
            log.debug("sceUmdReplaceProhibit");
        }

        umdAllowReplace = false;
        if(getUmdStat() != PSP_UMD_READY) {
            cpu.gpr[2] = ERROR_UMD_NOT_READY;
        } else {
            cpu.gpr[2] = 0;
        }
    }

    public void sceUmdReplacePermit(Processor processor) {
        CpuState cpu = processor.cpu;

        if(log.isDebugEnabled()) {
            log.debug("sceUmdReplacePermit");
        }

        umdAllowReplace = true;
        cpu.gpr[2] = 0;
    }

    public final HLEModuleFunction sceUmdReplaceProhibitFunction = new HLEModuleFunction("sceUmdUser", "sceUmdReplaceProhibit") {

        @Override
        public final void execute(Processor processor) {
            sceUmdReplaceProhibit(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUmdUserModule.sceUmdReplaceProhibit(processor);";
        }
    };
    public final HLEModuleFunction sceUmdReplacePermitFunction = new HLEModuleFunction("sceUmdUser", "sceUmdReplacePermit") {

        @Override
        public final void execute(Processor processor) {
            sceUmdReplacePermit(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUmdUserModule.sceUmdReplacePermit(processor);";
        }
    };
}