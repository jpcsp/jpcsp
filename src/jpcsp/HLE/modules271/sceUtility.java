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

public class sceUtility extends jpcsp.HLE.modules150.sceUtility {

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        super.installModule(mm, version);

        if (version >= 271) {

            mm.addFunction(sceUtilityLoadAvModuleFunction, 0xC629AF26);
            mm.addFunction(sceUtilityUnloadAvModuleFunction, 0xF7D8D092);

        }
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        super.uninstallModule(mm, version);

        if (version >= 271) {

            mm.removeFunction(sceUtilityLoadAvModuleFunction);
            mm.removeFunction(sceUtilityUnloadAvModuleFunction);

        }
    }

    public static final String[] utilityAvModuleNames = new String[] {
        "PSP_AV_MODULE_AVCODEC",
        "PSP_AV_MODULE_SASCORE",
        "PSP_AV_MODULE_ATRAC3PLUS",
        "PSP_AV_MODULE_MPEGBASE",
        "PSP_AV_MODULE_MP3",
        "PSP_AV_MODULE_VAUDIO",
        "PSP_AV_MODULE_AAC",
        "PSP_AV_MODULE_G729",
    };

    public static final int PSP_AV_MODULE_AVCODEC = 0;
    public static final int PSP_AV_MODULE_SASCORE = 1;
    public static final int PSP_AV_MODULE_ATRAC3PLUS = 2;
    public static final int PSP_AV_MODULE_MPEGBASE = 3;
    public static final int PSP_AV_MODULE_MP3 = 4;
    public static final int PSP_AV_MODULE_VAUDIO = 5;
    public static final int PSP_AV_MODULE_AAC = 6;
    public static final int PSP_AV_MODULE_G729 = 7;

    // Export functions

    public void sceUtilityLoadAvModule(Processor processor) {
        CpuState cpu = processor.cpu;

        int module = cpu.gpr[4];

        String msg = "sceUtilityLoadAvModule(module=" + module + ")";
        if (module >= 0 && module < utilityAvModuleNames.length)
            msg += " " + utilityAvModuleNames[module];

        Modules.log.warn("IGNORING:" + msg);

        cpu.gpr[2] = 0;
    }

    public void sceUtilityUnloadAvModule(Processor processor) {
        CpuState cpu = processor.cpu;

        int module = cpu.gpr[4];

        String msg = "sceUtilityUnloadAvModule(module=" + module + ")";
        if (module >= 0 && module < utilityAvModuleNames.length)
            msg += " " + utilityAvModuleNames[module];

        Modules.log.warn("IGNORING:" + msg);

        cpu.gpr[2] = 0;
    }

    public final HLEModuleFunction sceUtilityLoadAvModuleFunction = new HLEModuleFunction("sceUtility", "sceUtilityLoadAvModule") {

        @Override
        public final void execute(Processor processor) {
            sceUtilityLoadAvModule(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityLoadAvModule(processor);";
        }
    };

    public final HLEModuleFunction sceUtilityUnloadAvModuleFunction = new HLEModuleFunction("sceUtility", "sceUtilityUnloadAvModule") {

        @Override
        public final void execute(Processor processor) {
            sceUtilityUnloadAvModule(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityUnloadAvModule(processor);";
        }
    };
}
