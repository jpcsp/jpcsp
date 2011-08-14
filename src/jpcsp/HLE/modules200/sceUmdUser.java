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

import jpcsp.HLE.HLEFunction;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

public class sceUmdUser extends jpcsp.HLE.modules150.sceUmdUser {
    protected boolean umdAllowReplace;

    @Override
    public void installModule(HLEModuleManager mm, int version) { mm.installModuleWithAnnotations(this, version); }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) { mm.uninstallModuleWithAnnotations(this, version); }

    public void sceUmdReplaceProhibit(Processor processor) {
        CpuState cpu = processor.cpu;

        if(log.isDebugEnabled()) {
            log.debug("sceUmdReplaceProhibit");
        }

        umdAllowReplace = false;
        if(((getUmdStat() & PSP_UMD_READY) != PSP_UMD_READY)
                || ((getUmdStat() & PSP_UMD_READABLE) != PSP_UMD_READABLE)) {
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
    @HLEFunction(nid = 0x87533940, version = 200) public HLEModuleFunction sceUmdReplaceProhibitFunction;
    @HLEFunction(nid = 0xCBE9F02A, version = 200) public HLEModuleFunction sceUmdReplacePermitFunction;

}