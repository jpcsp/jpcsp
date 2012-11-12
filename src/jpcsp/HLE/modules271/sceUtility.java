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

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;

import java.util.HashMap;

import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.HLE.modules.HLEModuleManager;

@HLELogging
public class sceUtility extends jpcsp.HLE.modules260.sceUtility {
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

    public static final String[] utilityUsbModuleNames = new String[] {
        "PSP_USB_MODULE_UNKNOWN_0",
        "PSP_USB_MODULE_PSPCM",
        "PSP_USB_MODULE_ACC",
        "PSP_USB_MODULE_MIC",
        "PSP_USB_MODULE_CAM",
        "PSP_USB_MODULE_GPS"
    };

    public static final int PSP_AV_MODULE_AVCODEC = 0;
    public static final int PSP_AV_MODULE_SASCORE = 1;
    public static final int PSP_AV_MODULE_ATRAC3PLUS = 2;
    public static final int PSP_AV_MODULE_MPEGBASE = 3;
    public static final int PSP_AV_MODULE_MP3 = 4;
    public static final int PSP_AV_MODULE_VAUDIO = 5;
    public static final int PSP_AV_MODULE_AAC = 6;
    public static final int PSP_AV_MODULE_G729 = 7;

    public static final int PSP_USB_MODULE_PSPCM = 1;
    public static final int PSP_USB_MODULE_ACC = 2;
    public static final int PSP_USB_MODULE_MIC = 3;
    public static final int PSP_USB_MODULE_CAM = 4;
    public static final int PSP_USB_MODULE_GPS = 5;

    protected HashMap<Integer, SceModule> loadedAvModules = new HashMap<Integer, SceModule>();
    protected HashMap<Integer, String> waitingAvModules = new HashMap<Integer, String>();
    protected HashMap<Integer, SceModule> loadedUsbModules = new HashMap<Integer, SceModule>();
    protected HashMap<Integer, String> waitingUsbModules = new HashMap<Integer, String>();

	@Override
	public void stop() {
		loadedAvModules.clear();
		waitingAvModules.clear();
		loadedUsbModules.clear();
		waitingUsbModules.clear();
		super.stop();
	}

	private String getAvModuleName(int module) {
    	if (module < 0 || module >= utilityAvModuleNames.length) {
    		return "PSP_AV_MODULE_UNKNOWN_" + module;
    	}
    	return utilityAvModuleNames[module];
    }

    private String getUsbModuleName(int module) {
    	if (module < 0 || module >= utilityUsbModuleNames.length) {
    		return "PSP_USB_MODULE_UNKNOWN_" + module;
    	}
    	return utilityUsbModuleNames[module];
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

    protected int hleUtilityLoadUsbModule(int module, String moduleName) {
        HLEModuleManager moduleManager = HLEModuleManager.getInstance();
    	if (loadedUsbModules.containsKey(module) || waitingUsbModules.containsKey(module)) { // Module already loaded.
    		return SceKernelErrors.ERROR_AV_MODULE_ALREADY_LOADED;
    	} else if (!moduleManager.hasFlash0Module(moduleName)) { // Can't load flash0 module.
            waitingUsbModules.put(module, moduleName); // Always save a load attempt.
            return SceKernelErrors.ERROR_AV_MODULE_BAD_ID;
    	} else {
            // Load and save it in loadedAvModules.
            int sceModuleId = moduleManager.LoadFlash0Module(moduleName);
            SceModule sceModule = Managers.modules.getModuleByUID(sceModuleId);
            loadedUsbModules.put(module, sceModule);
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

    @HLELogging(level="info")
    @HLEFunction(nid = 0xC629AF26, version = 270, checkInsideInterrupt = true)
    public int sceUtilityLoadAvModule(int module) {
        String moduleName = getAvModuleName(module);
        int result = hleUtilityLoadAvModule(module, moduleName);
        if (result == SceKernelErrors.ERROR_AV_MODULE_BAD_ID) {
            log.info(String.format("IGNORING: sceUtilityLoadAvModule(module=0x%04X) %s", module, moduleName));
            return 0;
        }

        return result;
    }

    @HLELogging(level="info")
    @HLEFunction(nid = 0xF7D8D092, version = 270, checkInsideInterrupt = true)
    public int sceUtilityUnloadAvModule(int module) {
        return hleUtilityUnloadAvModule(module);
    }

    @HLELogging(level="info")
    @HLEFunction(nid = 0x0D5BC6D2, version = 270, checkInsideInterrupt = true)
    public int sceUtilityLoadUsbModule(int module) {
        String moduleName = getUsbModuleName(module);
        int result = hleUtilityLoadUsbModule(module, moduleName);
        if (result == SceKernelErrors.ERROR_AV_MODULE_BAD_ID) {
            log.info(String.format("IGNORING: sceUtilityLoadUsbModule(module=0x%04X) %s", module, moduleName));
            return 0;
        }

        return result;
    }
}