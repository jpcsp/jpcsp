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
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
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

            loadedNetModules = new HashMap<Integer, SceModule>();
            waitingNetModules = new HashMap<Integer, String>();
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

    public static final int PSP_NET_MODULE_COMMON = 1;
    public static final int PSP_NET_MODULE_ADHOC = 2;
    public static final int PSP_NET_MODULE_INET = 3;
    public static final int PSP_NET_MODULE_PARSEURI = 4;
    public static final int PSP_NET_MODULE_PARSEHTTP = 5;
    public static final int PSP_NET_MODULE_HTTP = 6;
    public static final int PSP_NET_MODULE_SSL = 7;

    protected HashMap<Integer, SceModule> loadedNetModules;
    protected HashMap<Integer, String> waitingNetModules;

    private String getNetModuleName(int module) {
    	if (module < 0 || module >= utilityNetModuleNames.length) {
    		return "PSP_NET_MODULE_UNKNOWN_" + module;
    	}
    	return utilityNetModuleNames[module];
    }

    protected int hleUtilityLoadNetModule(int module, String moduleName) {
        HLEModuleManager moduleManager = HLEModuleManager.getInstance();
        waitingNetModules.put(module, moduleName); // Always save a load attempt.

    	if (waitingNetModules.containsKey(module) || loadedNetModules.containsKey(module)) { // Module already loaded.
    		return SceKernelErrors.ERROR_NET_MODULE_ALREADY_LOADED;
    	} else if (!moduleManager.hasFlash0Module(moduleName)) { // Invalid flash0 module.
            return SceKernelErrors.ERROR_NET_MODULE_BAD_ID;
    	} else {
            // Load and save it in loadedNetModules.
            int sceModuleId = moduleManager.LoadFlash0Module(moduleName);
            SceModule sceModule = Managers.modules.getModuleByUID(sceModuleId);
            waitingNetModules.remove(module);
            loadedNetModules.put(module, sceModule);
            return 0;
        }
    }

    protected int hleUtilityUnloadNetModule(int module) {
    	SceModule sceModule = loadedNetModules.remove(module);
    	if (sceModule == null) { // Module was not loaded.
    		return SceKernelErrors.ERROR_NET_MODULE_NOT_LOADED;
    	} else {
            // Unload the module.
            HLEModuleManager moduleManager = HLEModuleManager.getInstance();
            moduleManager.UnloadFlash0Module(sceModule);
            return 0;
        }
    }

    public void sceUtilityLoadNetModule(Processor processor) {
        CpuState cpu = processor.cpu;

        int module = cpu.gpr[4];

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }

        String moduleName = getNetModuleName(module);
        int result = hleUtilityLoadNetModule(module, moduleName);
        if(result == SceKernelErrors.ERROR_NET_MODULE_BAD_ID) {
            log.info(String.format("IGNORING: sceUtilityLoadNetModule(module=0x%04X) %s", module, moduleName));
            result = 0;
        } else {
            log.info(String.format("sceUtilityLoadNetModule(module=0x%04X) %s", module, moduleName));
        }
        cpu.gpr[2] = result;
    }

    public void sceUtilityUnloadNetModule(Processor processor) {
        CpuState cpu = processor.cpu;

        int module = cpu.gpr[4];

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }

        String moduleName = getNetModuleName(module);
        int result = hleUtilityUnloadNetModule(module);
        if (result == SceKernelErrors.ERROR_NET_MODULE_NOT_LOADED) {
            log.info(String.format("IGNORING: sceUtilityUnloadNetModule(module=0x%04X) %s", module, moduleName));
            result = 0;
        } else {
            log.info(String.format("sceUtilityUnloadNetModule(module=0x%04X) %s unloaded", module, moduleName));
        }
        cpu.gpr[2] = result;
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