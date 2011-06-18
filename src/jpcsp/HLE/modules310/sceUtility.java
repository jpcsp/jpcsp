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

package jpcsp.HLE.modules310;

import java.util.HashMap;

import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

public class sceUtility extends jpcsp.HLE.modules271.sceUtility {

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        super.installModule(mm, version);

        if (version >= 310) {

            mm.addFunction(0x2A2B3DE0, sceUtilityLoadModuleFunction);
            mm.addFunction(0xE49BFE92, sceUtilityUnloadModuleFunction);

            loadedModules = new HashMap<Integer, SceModule>();
            waitingModules = new HashMap<Integer, String>();
        }
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        super.uninstallModule(mm, version);

        if (version >= 310) {

            mm.removeFunction(sceUtilityLoadModuleFunction);
            mm.removeFunction(sceUtilityUnloadModuleFunction);

        }
    }

    public static enum UtilityModule {
        PSP_MODULE_NET_COMMON(0x0100),
        PSP_MODULE_NET_ADHOC(0x0101),
        PSP_MODULE_NET_INET(0x0102),
        PSP_MODULE_NET_PARSEURI(0x0103),
        PSP_MODULE_NET_PARSEHTTP(0x0104),
        PSP_MODULE_NET_HTTP(0x0105),
        PSP_MODULE_NET_SSL(0x0106),
        PSP_MODULE_NET_HTTPSTORAGE(0x0108),
        PSP_MODULE_USB_PSPCM(0x0200),
        PSP_MODULE_USB_MIC(0x0201),
        PSP_MODULE_USB_CAM(0x0202),
        PSP_MODULE_USB_GPS(0x0203),
        PSP_MODULE_AV_AVCODEC(0x0300),
        PSP_MODULE_AV_SASCORE(0x0301),
        PSP_MODULE_AV_ATRAC3PLUS(0x0302),
        PSP_MODULE_AV_MPEGBASE(0x0303),
        PSP_MODULE_AV_MP3(0x0304),
        PSP_MODULE_AV_VAUDIO(0x0305),
        PSP_MODULE_AV_AAC(0x0306),
        PSP_MODULE_AV_G729(0x0307),
        PSP_MODULE_AV_MP4(0x0308),
        PSP_MODULE_NP_COMMON(0x0400),
        PSP_MODULE_NP_SERVICE(0x0401),
        PSP_MODULE_NP_MATCHING2(0x0402),
        PSP_MODULE_NP_DRM(0x0500),
        PSP_MODULE_IRDA(0x0600);

        private int id;

        private UtilityModule(int id) {
            this.id = id;
        }

        public int getID() {
            return id;
        }
    }

    protected HashMap<Integer, SceModule> loadedModules;
    protected HashMap<Integer, String> waitingModules;

    protected String getModuleName(int module) {
        for (UtilityModule m : UtilityModule.values()) {
            if (m.getID() == module) {
                return m.toString();
            }
        }
        return "PSP_MODULE_UNKNOWN_" + Integer.toHexString(module);
    }

    protected int hleUtilityLoadModule(int module, String moduleName) {
        HLEModuleManager moduleManager = HLEModuleManager.getInstance();
    	if (loadedModules.containsKey(module) || waitingModules.containsKey(module)) { // Module already loaded.
    		return SceKernelErrors.ERROR_MODULE_ALREADY_LOADED;
    	} else if (!moduleManager.hasFlash0Module(moduleName)) { // Can't load flash0 module.
            waitingModules.put(module, moduleName); // Always save a load attempt.
            return SceKernelErrors.ERROR_MODULE_BAD_ID;
    	} else {
            // Load and save it in loadedModules.
            int sceModuleId = moduleManager.LoadFlash0Module(moduleName);
            SceModule sceModule = Managers.modules.getModuleByUID(sceModuleId);
            loadedModules.put(module, sceModule);
            return 0;
        }
    }

    protected int hleUtilityUnloadModule(int module) {
        if (loadedModules.containsKey(module)) {
            // Unload the module.
            HLEModuleManager moduleManager = HLEModuleManager.getInstance();
            SceModule sceModule = loadedModules.remove(module);
            moduleManager.UnloadFlash0Module(sceModule);
            return 0;
        } else if (waitingModules.containsKey(module)) {
            // Simulate a successful unload.
            waitingModules.remove(module);
            return 0;
        } else {
            return SceKernelErrors.ERROR_MODULE_NOT_LOADED;
        }
    }

    public void sceUtilityLoadModule(Processor processor) {
        CpuState cpu = processor.cpu;

        int module = cpu.gpr[4];

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }

        String moduleName = getModuleName(module);
        int result = hleUtilityLoadModule(module, moduleName);
        if (result == SceKernelErrors.ERROR_MODULE_BAD_ID) {
            log.info(String.format("IGNORING: sceUtilityLoadModule(module=0x%04X) %s", module, moduleName));
            result = 0;
        } else {
            log.info(String.format("sceUtilityLoadModule(module=0x%04X) %s loaded", module, moduleName));
        }
        cpu.gpr[2] = result;
    }

    public void sceUtilityUnloadModule(Processor processor) {
        CpuState cpu = processor.cpu;

        int module = cpu.gpr[4];


        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }

        String moduleName = getModuleName(module);
        log.info(String.format("sceUtilityUnloadModule(module=0x%04X) %s unloaded", module, moduleName));

        cpu.gpr[2] = hleUtilityUnloadModule(module);
    }

    public final HLEModuleFunction sceUtilityLoadModuleFunction = new HLEModuleFunction("sceUtility", "sceUtilityLoadModule") {

        @Override
        public final void execute(Processor processor) {
            sceUtilityLoadModule(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityLoadModule(processor);";
        }
    };

    public final HLEModuleFunction sceUtilityUnloadModuleFunction = new HLEModuleFunction("sceUtility", "sceUtilityUnloadModule") {

        @Override
        public final void execute(Processor processor) {
            sceUtilityUnloadModule(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityUnloadModule(processor);";
        }
    };
}