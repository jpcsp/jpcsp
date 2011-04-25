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

import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

public class sceUtility extends jpcsp.HLE.modules200.sceUtility {

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        super.installModule(mm, version);

        if (version >= 270) {

            mm.addFunction(0xC629AF26, sceUtilityLoadAvModuleFunction);
            mm.addFunction(0xF7D8D092, sceUtilityUnloadAvModuleFunction);
            mm.addFunction(0x4928BD96, sceUtilityMsgDialogAbortFunction);

            loadedAvModules = new HashMap<Integer, SceModule>();
            waitingAvModules = new HashMap<Integer, String>();
        }
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        super.uninstallModule(mm, version);

        if (version >= 270) {

            mm.removeFunction(sceUtilityLoadAvModuleFunction);
            mm.removeFunction(sceUtilityUnloadAvModuleFunction);
            mm.removeFunction(sceUtilityMsgDialogAbortFunction);

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

    protected HashMap<Integer, SceModule> loadedAvModules;
    protected HashMap<Integer, String> waitingAvModules;

    private String getAvModuleName(int module) {
    	if (module < 0 || module >= utilityAvModuleNames.length) {
    		return "PSP_AV_MODULE_UNKNOWN_" + module;
    	}
    	return utilityAvModuleNames[module];
    }

    protected int hleUtilityLoadAvModule(int module, String moduleName) {
        HLEModuleManager moduleManager = HLEModuleManager.getInstance();
    	if (loadedAvModules.containsKey(module) || waitingAvModules.containsKey(module)) { // Module already loaded.
    		return SceKernelErrors.ERROR_AV_MODULE_ALREADY_LOADED;
    	} else if (!moduleManager.hasFlash0Module(moduleName)) { // Can't load flash0 module.
            waitingAvModules.put(module, moduleName); // Always save a load attempt.
            return SceKernelErrors.ERROR_AV_MODULE_BAD_ID;
    	} else {
            // Load and save it in loadedAvModules.
            int sceModuleId = moduleManager.LoadFlash0Module(moduleName);
            SceModule sceModule = Managers.modules.getModuleByUID(sceModuleId);
            loadedAvModules.put(module, sceModule);
            return 0;
        }
    }

    protected int hleUtilityUnloadAvModule(int module) {
        if (loadedAvModules.containsKey(module)) {
            // Unload the module.
            HLEModuleManager moduleManager = HLEModuleManager.getInstance();
            SceModule sceModule = loadedAvModules.remove(module);
            moduleManager.UnloadFlash0Module(sceModule);
            return 0;
        } else if (waitingAvModules.containsKey(module)) {
            // Simulate a successful unload.
            waitingAvModules.remove(module);
            return 0;
        } else {
            return SceKernelErrors.ERROR_AV_MODULE_NOT_LOADED;
        }
    }

    public void sceUtilityLoadAvModule(Processor processor) {
        CpuState cpu = processor.cpu;

        int module = cpu.gpr[4];

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }

        String moduleName = getAvModuleName(module);
        int result = hleUtilityLoadAvModule(module, moduleName);
        if(result == SceKernelErrors.ERROR_AV_MODULE_BAD_ID) {
            log.info(String.format("IGNORING: sceUtilityLoadAvModule(module=0x%04X) %s", module, moduleName));
            result = 0;
        } else {
            log.info(String.format("sceUtilityLoadAvModule(module=0x%04X) %s loaded", module, moduleName));
        }
        cpu.gpr[2] = result;
    }

    public void sceUtilityUnloadAvModule(Processor processor) {
        CpuState cpu = processor.cpu;

        int module = cpu.gpr[4];

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }

        String moduleName = getAvModuleName(module);
        log.info(String.format("sceUtilityUnloadAvModule(module=0x%04X) %s unloaded", module, moduleName));

        cpu.gpr[2] = hleUtilityUnloadAvModule(module);
    }

    public void sceUtilityMsgDialogAbort(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("PARTIAL: sceUtilityMsgDialogAbort()");
        msgDialogState.abort();

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

    public final HLEModuleFunction sceUtilityMsgDialogAbortFunction = new HLEModuleFunction("sceUtility", "sceUtilityMsgDialogAbort") {
		@Override
		public final void execute(Processor processor) {
			sceUtilityMsgDialogAbort(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityMsgDialogAbort(processor);";
		}
	};
}