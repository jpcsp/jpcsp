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

import java.util.HashMap;
import java.util.Iterator;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.Modules;

import jpcsp.Emulator;
import jpcsp.MemoryMap;
import jpcsp.Memory;
import jpcsp.Processor;
import static jpcsp.util.Utilities.*;

import jpcsp.Allegrex.CpuState; // New-Style Processor
import jpcsp.HLE.ThreadMan;
import jpcsp.filesystems.umdiso.UmdIsoReader;

import jpcsp.HLE.kernel.types.*;
import jpcsp.HLE.kernel.managers.*;

public class sceUmdUser extends jpcsp.HLE.modules150.sceUmdUser {

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        super.installModule(mm, version);

        if (version >= 200) {

            mm.addFunction(sceUmdReplaceProhibitFunction, 0x87533940);
            mm.addFunction(sceUmdReplacePermitFunction, 0xCBE9F02A);

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

    // Export functions

    public void sceUmdReplaceProhibit(Processor processor) {
        CpuState cpu = processor.cpu;

        Modules.log.warn(String.format("UNIMPLEMENTED:sceUmdReplaceProhibit"
            + " %08X %08X %08X", cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]));

        cpu.gpr[2] = 0;
    }

    public void sceUmdReplacePermit(Processor processor) {
        CpuState cpu = processor.cpu;

        Modules.log.warn(String.format("UNIMPLEMENTED:sceUmdReplacePermit"
            + " %08X %08X %08X", cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]));

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
