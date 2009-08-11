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

public class sceUtility extends jpcsp.HLE.modules150.sceUtility {

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        super.installModule(mm, version);

        if (version >= 200) {

            mm.addFunction(sceUtilityLoadNetModuleFunction, 0x1579A159);
            mm.addFunction(sceUtilityUnloadNetModuleFunction, 0x64D50C56);

        }
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        super.uninstallModule(mm, version);

        if (version >= 200) {

            mm.removeFunction(sceUtilityLoadNetModuleFunction);
            mm.removeFunction(sceUtilityUnloadNetModuleFunction);

        }
    }

    public static final String[] utilityNetModuleNames = new String[] {
        "PSP_NET_MODULE_UNKNOWN(1)",
        "PSP_NET_MODULE_COMMON",
        "PSP_NET_MODULE_ADHOC",
        "PSP_NET_MODULE_INET",
        "PSP_NET_MODULE_PARSEURI",
        "PSP_NET_MODULE_PARSEHTTP",
        "PSP_NET_MODULE_HTTP",
        "PSP_NET_MODULE_SSL",
    };

    // Start at 1, yet sceUtilityLoadAvModule and sceUtilityLoadModule start at 0 ...
    public static final int PSP_NET_MODULE_COMMON = 1;
    public static final int PSP_NET_MODULE_ADHOC = 2;
    public static final int PSP_NET_MODULE_INET = 3;
    public static final int PSP_NET_MODULE_PARSEURI = 4;
    public static final int PSP_NET_MODULE_PARSEHTTP = 5;
    public static final int PSP_NET_MODULE_HTTP = 6;
    public static final int PSP_NET_MODULE_SSL = 7;

    // Export functions

    public void sceUtilityLoadNetModule(Processor processor) {
        CpuState cpu = processor.cpu;

        int module = cpu.gpr[4];

        String msg = "sceUtilityLoadNetModule(module=" + module + ")";
        if (module >= 0 && module < utilityNetModuleNames.length)
            msg += " " + utilityNetModuleNames[module];

        Modules.log.warn("IGNORING:" + msg);

        cpu.gpr[2] = 0;
        jpcsp.HLE.ThreadMan.getInstance().yieldCurrentThread();
    }

    public void sceUtilityUnloadNetModule(Processor processor) {
        CpuState cpu = processor.cpu;

        int module = cpu.gpr[4];

        String msg = "sceUtilityUnloadNetModule(module=" + module + ")";
        if (module >= 0 && module < utilityNetModuleNames.length)
            msg += " " + utilityNetModuleNames[module];

        Modules.log.warn("IGNORING:" + msg);

        cpu.gpr[2] = 0;
        jpcsp.HLE.ThreadMan.getInstance().yieldCurrentThread();
    }

    public final HLEModuleFunction sceUtilityLoadNetModuleFunction = new HLEModuleFunction("sceUtility", "sceUtilityLoadNetModule") {

        @Override
        public final void execute(Processor processor) {
            sceUtilityLoadNetModule(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityLoadNetModule(processor);";
        }
    };

    public final HLEModuleFunction sceUtilityUnloadNetModuleFunction = new HLEModuleFunction("sceUtility", "sceUtilityUnloadNetModule") {

        @Override
        public final void execute(Processor processor) {
            sceUtilityUnloadNetModule(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityUnloadNetModule(processor);";
        }
    };
}
