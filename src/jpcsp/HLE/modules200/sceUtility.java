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

import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

public class sceUtility extends jpcsp.HLE.modules150.sceUtility {

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        super.installModule(mm, version);

        if (version >= 200) {

            mm.addFunction(0x1579A159, sceUtilityLoadNetModuleFunction);
            mm.addFunction(0x64D50C56, sceUtilityUnloadNetModuleFunction);

            loadedModules = new HashMap<Integer, SceModule>();
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

    protected HashMap<Integer, SceModule> loadedModules;

    private String hleUtilityLoadNetModuleName(int module) {
    	if (module < 0 || module >= utilityNetModuleNames.length) {
    		return "PSP_NET_MODULE_UNKNOWN_" + module;
    	}

    	return utilityNetModuleNames[module];
    }

    protected boolean loadModule(int module, String moduleName) {
    	// Already loaded?
    	if (loadedModules.containsKey(module)) {
    		return false;
    	}

    	HLEModuleManager moduleManager = HLEModuleManager.getInstance();

    	// Can be loaded?
    	if (!moduleManager.hasFlash0Module(moduleName)) {
    		return false;
    	}

    	// Load it and remember the SceModule in loadedModules
    	int sceModuleId = moduleManager.LoadFlash0Module(moduleName);
        SceModule sceModule = Managers.modules.getModuleByUID(sceModuleId);

    	loadedModules.put(module, sceModule);

    	return true;
    }

    protected boolean unloadModule(int module, int sceModuleId) {
    	SceModule sceModule = loadedModules.remove(module);

    	// Has been loaded?
    	if (sceModule == null) {
    		return false;
    	}

    	HLEModuleManager moduleManager = HLEModuleManager.getInstance();
    	moduleManager.UnloadFlash0Module(sceModule);

    	return true;
    }

    // Export functions

    public void sceUtilityLoadNetModule(Processor processor) {
        CpuState cpu = processor.cpu;

        int module = cpu.gpr[4];

        String moduleName = hleUtilityLoadNetModuleName(module);
        if (loadModule(module, moduleName)) {
            Modules.log.info(String.format("sceUtilityLoadNetModule(module=0x%04X) %s loaded", module, moduleName));
        } else {
            Modules.log.info(String.format("IGNORING:sceUtilityLoadNetModule(module=0x%04X) %s", module, moduleName));
        }

        cpu.gpr[2] = 0;
        Modules.ThreadManForUserModule.hleRescheduleCurrentThread();
    }

    public void sceUtilityUnloadNetModule(Processor processor) {
        CpuState cpu = processor.cpu;

        int module = cpu.gpr[4];

        String moduleName = hleUtilityLoadNetModuleName(module);
        if (loadModule(module, moduleName)) {
            Modules.log.info(String.format("sceUtilityUnloadNetModule(module=0x%04X) %s unloaded", module, moduleName));
        } else {
            Modules.log.info(String.format("IGNORING:sceUtilityUnloadNetModule(module=0x%04X) %s", module, moduleName));
        }

        cpu.gpr[2] = 0;
        Modules.ThreadManForUserModule.hleRescheduleCurrentThread();
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
