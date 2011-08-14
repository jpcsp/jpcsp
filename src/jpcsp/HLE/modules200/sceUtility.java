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

import jpcsp.HLE.HLEFunction;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;

import jpcsp.Emulator;
import jpcsp.GeneralJpcspException;
import jpcsp.Loader;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.HLE.kernel.types.SceUtilityInstallParams;
import jpcsp.HLE.kernel.types.pspAbstractMemoryMappedStructure;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.IoFileMgrForUser;
import jpcsp.filesystems.SeekableDataInput;

public class sceUtility extends jpcsp.HLE.modules150.sceUtility {
    @Override
    public void installModule(HLEModuleManager mm, int version) { mm.installModuleWithAnnotations(this, version); }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) { mm.uninstallModuleWithAnnotations(this, version); }

    protected static class InstallUtilityDialogState extends UtilityDialogState {
		protected SceUtilityInstallParams installParams;

    	public InstallUtilityDialogState(String name) {
			super(name);
		}

		@Override
		protected boolean executeUpdateVisible(Processor processor) {
			boolean keepVisible = false;

			log.warn(String.format("Partial sceUtilityInstallUpdate %s", installParams.toString()));

			// We only get the game name from the install params. Is the rest fixed?
			String fileName = String.format("ms0:/PSP/GAME/%s/EBOOT.PBP", installParams.gameName);
	        try {
	            SeekableDataInput moduleInput = Modules.IoFileMgrForUserModule.getFile(fileName, IoFileMgrForUser.PSP_O_RDONLY);
	            if (moduleInput != null) {
	                byte[] moduleBytes = new byte[(int) moduleInput.length()];
	                moduleInput.readFully(moduleBytes);
	                ByteBuffer moduleBuffer = ByteBuffer.wrap(moduleBytes);

	    			// TODO How is this module being loaded?
	                // Does it unload the current module? i.e. re-init the PSP
	                SceModule module = Emulator.getInstance().load(name, moduleBuffer, true);
	                Emulator.getClock().resume();

	                if ((module.fileFormat & Loader.FORMAT_ELF) == Loader.FORMAT_ELF) {
	                	installParams.base.result = 0;
	                	keepVisible = false;
	                } else {
	                    log.warn("sceUtilityInstall - failed, target is not an ELF");
	                    installParams.base.result = -1;
	                }
	                moduleInput.close();
	            }
	        } catch (GeneralJpcspException e) {
	            log.error("General Error : " + e.getMessage());
	            Emulator.PauseEmu();
	        } catch (IOException e) {
	            log.error(String.format("sceUtilityInstall - Error while loading module %s: %s", fileName, e.getMessage()));
	            installParams.base.result = -1;
	        }

			return keepVisible;
		}

		@Override
		protected pspAbstractMemoryMappedStructure createParams() {
			installParams = new SceUtilityInstallParams();
			return installParams;
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
    protected InstallUtilityDialogState installState;

	@Override
	public void start() {
		super.start();

		installState = new InstallUtilityDialogState("sceUtilityInstall");
	}

	private String getNetModuleName(int module) {
    	if (module < 0 || module >= utilityNetModuleNames.length) {
    		return "PSP_NET_MODULE_UNKNOWN_" + module;
    	}
    	return utilityNetModuleNames[module];
    }

    protected int hleUtilityLoadNetModule(int module, String moduleName) {
        HLEModuleManager moduleManager = HLEModuleManager.getInstance();
    	if (loadedNetModules.containsKey(module) || waitingNetModules.containsKey(module)) { // Module already loaded.
    		return SceKernelErrors.ERROR_NET_MODULE_ALREADY_LOADED;
    	} else if (!moduleManager.hasFlash0Module(moduleName)) { // Can't load flash0 module.
            waitingNetModules.put(module, moduleName); // Always save a load attempt.
            return SceKernelErrors.ERROR_NET_MODULE_BAD_ID;
    	} else {
            // Load and save it in loadedNetModules.
            int sceModuleId = moduleManager.LoadFlash0Module(moduleName);
            SceModule sceModule = Managers.modules.getModuleByUID(sceModuleId);
            loadedNetModules.put(module, sceModule);
            return 0;
        }
    }

    protected int hleUtilityUnloadNetModule(int module) {
        if (loadedNetModules.containsKey(module)) {
            // Unload the module.
            HLEModuleManager moduleManager = HLEModuleManager.getInstance();
            SceModule sceModule = loadedNetModules.remove(module);
            moduleManager.UnloadFlash0Module(sceModule);
            return 0;
        } else if (waitingNetModules.containsKey(module)) {
            // Simulate a successful unload.
            waitingNetModules.remove(module);
            return 0;
        } else {
            return SceKernelErrors.ERROR_NET_MODULE_NOT_LOADED;
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
            log.info(String.format("sceUtilityLoadNetModule(module=0x%04X) %s loaded", module, moduleName));
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
        log.info(String.format("sceUtilityUnloadNetModule(module=0x%04X) %s unloaded", module, moduleName));

        cpu.gpr[2] = hleUtilityUnloadNetModule(module);
    }

    public void sceUtilityInstallInitStart(Processor processor) {
        installState.executeInitStart(processor);
    }

    public void sceUtilityInstallShutdownStart(Processor processor) {
    	installState.executeShutdownStart(processor);
    }

    public void sceUtilityInstallUpdate(Processor processor) {
    	installState.executeUpdate(processor);
    }

    public void sceUtilityInstallGetStatus(Processor processor) {
    	installState.executeGetStatus(processor);
    }
    @HLEFunction(nid = 0x1579A159, version = 200) public HLEModuleFunction sceUtilityLoadNetModuleFunction;

    @HLEFunction(nid = 0x64D50C56, version = 200) public HLEModuleFunction sceUtilityUnloadNetModuleFunction;

    @HLEFunction(nid = 0xC4700FA3, version = 200) public HLEModuleFunction sceUtilityInstallGetStatusFunction;

    @HLEFunction(nid = 0x1281DA8E, version = 200) public HLEModuleFunction sceUtilityInstallInitStartFunction;

    @HLEFunction(nid = 0x5EF1C24A, version = 200) public HLEModuleFunction sceUtilityInstallShutdownStartFunction;

    @HLEFunction(nid = 0xA03D29BA, version = 200) public HLEModuleFunction sceUtilityInstallUpdateFunction;

}