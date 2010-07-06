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

import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

public class sceUtility extends jpcsp.HLE.modules200.sceUtility {

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        super.installModule(mm, version);

        // HACK using 270 instead of 271 because some 270 games use it...
        // TODO move to a 270 directory if it becomes a problem
        if (version >= 270) {

            mm.addFunction(sceUtilityLoadAvModuleFunction, 0xC629AF26);
            mm.addFunction(sceUtilityUnloadAvModuleFunction, 0xF7D8D092);

        }
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        super.uninstallModule(mm, version);

        if (version >= 270) {

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

    private String hleUtilityLoadAvModuleName(int module) {
    	if (module < 0 || module >= utilityAvModuleNames.length) {
    		return "PSP_AV_MODULE_UNKNOWN_" + module;
    	}

    	return utilityAvModuleNames[module];
    }

    // Export functions

    public void sceUtilityLoadAvModule(Processor processor) {
        CpuState cpu = processor.cpu;

        int module = cpu.gpr[4];

        String moduleName = hleUtilityLoadAvModuleName(module);
        if (loadModule(module, moduleName)) {
            Modules.log.info(String.format("sceUtilityLoadAvModule(module=0x%04X) %s loaded", module, moduleName));
        } else {
            Modules.log.info(String.format("IGNORING:sceUtilityLoadAvModule(module=0x%04X) %s", module, moduleName));
        }

        cpu.gpr[2] = 0;
        Modules.ThreadManForUserModule.hleRescheduleCurrentThread();
    }

    public void sceUtilityUnloadAvModule(Processor processor) {
        CpuState cpu = processor.cpu;

        int module = cpu.gpr[4];

        String moduleName = hleUtilityLoadAvModuleName(module);
        if (loadModule(module, moduleName)) {
            Modules.log.info(String.format("sceUtilityUnloadAvModule(module=0x%04X) %s unloaded", module, moduleName));
        } else {
            Modules.log.info(String.format("IGNORING:sceUtilityUnloadAvModule(module=0x%04X) %s", module, moduleName));
        }

        cpu.gpr[2] = 0;
        Modules.ThreadManForUserModule.hleRescheduleCurrentThread();
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
