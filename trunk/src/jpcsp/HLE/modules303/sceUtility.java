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
package jpcsp.HLE.modules303;

import jpcsp.State;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.Modules;

import java.util.HashMap;

import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.ModuleMgrForUser;

@HLELogging
public class sceUtility extends jpcsp.HLE.modules271.sceUtility {
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

    protected HashMap<Integer, SceModule> loadedModules = new HashMap<Integer, SceModule>();
    protected HashMap<Integer, String> waitingModules = new HashMap<Integer, String>();

	@Override
	public void stop() {
		loadedModules.clear();
		waitingModules.clear();
		super.stop();
	}

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

    @HLEFunction(nid = 0x2A2B3DE0, version = 303, checkInsideInterrupt = true)
    public int sceUtilityLoadModule(int module) {
        String moduleName = getModuleName(module);
        int result = hleUtilityLoadModule(module, moduleName);
        if (result == SceKernelErrors.ERROR_MODULE_BAD_ID) {
            log.info(String.format("IGNORING: sceUtilityLoadModule(module=0x%04X) %s", module, moduleName));
            result = 0;

            if (module == UtilityModule.PSP_MODULE_NET_HTTPSTORAGE.id && "ULJS00331".equals(State.discId)) {
            	// The game "Kamen Rider Climax Heroes OOO - ULJS00331" is checking that the return value of
            	//     sceUtilityLoadModule(PSP_MODULE_NET_HTTPSTORAGE)
            	// has a defined value.
            	// The return value must match: result * 100 / 6532 == 0x00B99F84
            	result = 0x2F5CE6C3;
            }

        	Modules.ThreadManForUserModule.hleKernelDelayThread(ModuleMgrForUser.loadHLEModuleDelay, false);

        	return result;
        }

        log.info(String.format("sceUtilityLoadModule(module=0x%04X) %s loaded", module, moduleName));

        if (result >= 0) {
        	Modules.ThreadManForUserModule.hleKernelDelayThread(ModuleMgrForUser.loadHLEModuleDelay, false);
        }

        return result;
    }

    @HLEFunction(nid = 0xE49BFE92, version = 303, checkInsideInterrupt = true)
    public int sceUtilityUnloadModule(int module) {
        String moduleName = getModuleName(module);
        log.info(String.format("sceUtilityUnloadModule(module=0x%04X) %s unloaded", module, moduleName));

        return hleUtilityUnloadModule(module);
    }
}