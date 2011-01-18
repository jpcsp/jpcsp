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

package jpcsp.HLE.modules;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.NIDMapper;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.types.SceModule;


/**
 * For backwards compatibility with the current jpcsp code, the old
 * SyscallHandler can still be used. When an unhandled syscall is found
 * HLEModuleManager.getInstance().handleSyscall(code) should be called.
 * This function will then return true if the syscall is handled, in which case
 * no error message should be printed by SyscallHandler.java.
 *
 * Modules that require stepping should implement HLEThread and call
 * mm.addThread inside installModule with a matching mm.removeThread in
 * uninstall module.
 * Example: ThreadMan, pspctrl, pspAudio, sceDisplay
 *
 * @author fiveofhearts
 */
public class HLEModuleManager {
    private static HLEModuleManager instance;

    // Store the syscallCodeToFunction map into an array to improve the performance
    // of handleSyscall().
    private HLEModuleFunction[] syscallCodeToFunction;
    private int syscallCodeAllocator;
    private boolean modulesStarted = false;

    private HashMap<String, List<HLEModule>> flash0prxMap;

    /** The current firmware version we are using
     * was supposed to be one of SysMemUserForUser.PSP_FIRMWARE_* but there's a mistake
     * in the module autogen and now its an int in this format:
     * ABB, where A = major and B = minor, for example 271 */
    private int firmwareVersion;

    /**
     * List of modules that can be loaded
     * - by default in all firmwares (or only from a given FirmwareVersion)
     * - by sceKernelLoadModule/sceUtilityLoadModule from the flash0 or from the UMD (.prx)
     */
    private enum DefaultModule {
    	// Modules loaded by default in all firmware version...
    	SysMemUserForUser(Modules.SysMemUserForUserModule),
        SysMemForKernel(Modules.SysMemForKernelModule),
    	IoFileMgrForUser(Modules.IoFileMgrForUserModule),
    	ThreadManForUser(Modules.ThreadManForUserModule),
    	InterruptManager(Modules.InterruptManagerModule),
    	LoadExecForUser(Modules.LoadExecForUserModule),
        StdioForUser(Modules.StdioForUserModule),
        sceUmdUser(Modules.sceUmdUserModule),
        scePower(Modules.scePowerModule),
        sceUtility(Modules.sceUtilityModule),
        UtilsForUser(Modules.UtilsForUserModule),
        sceDisplay(Modules.sceDisplayModule),
        sceGe_user(Modules.sceGe_userModule),
        sceRtc(Modules.sceRtcModule),
        KernelLibrary(Modules.Kernel_LibraryModule),
        ModuleMgrForUser(Modules.ModuleMgrForUserModule),
        LoadCoreForKernel(Modules.LoadCoreForKernelModule),
        sceCtrl(Modules.sceCtrlModule),
        sceAudio(Modules.sceAudioModule),
        sceImpose(Modules.sceImposeModule),
        sceSuspendForUser(Modules.sceSuspendForUserModule),
        sceDmac(Modules.sceDmacModule),
        sceHprm(Modules.sceHprmModule),		// check if loaded by default
        sceAtrac3plus(Modules.sceAtrac3plusModule, new String[] { "libatrac3plus", "PSP_AV_MODULE_ATRAC3PLUS", "PSP_MODULE_AV_ATRAC3PLUS" }),
        sceSasCore(Modules.sceSasCoreModule, new String[] { "sc_sascore", "PSP_AV_MODULE_SASCORE", "PSP_MODULE_AV_SASCORE" } ),
        sceMpeg(Modules.sceMpegModule, new String[] { "mpeg", "mpeg_vsh", "PSP_AV_MODULE_MPEGBASE", "PSP_MODULE_AV_MPEGBASE" }),
        sceFont(Modules.sceFontModule, new String[] { "libfont" }),
        scePsmfPlayer(Modules.scePsmfPlayerModule, new String[] { "libpsmfplayer" }),
        scePsmf(Modules.scePsmfModule, new String[] { "psmf" }),
        sceMp3(Modules.sceMp3Module),
        sceDeflt(Modules.sceDefltModule),
        sceWlan(Modules.sceWlanModule),
        sceNet(Modules.sceNetModule),
        sceNetAdhoc(Modules.sceNetAdhocModule),
        sceNetAdhocctl(Modules.sceNetAdhocctlModule),
        sceNetAdhocDiscover(Modules.sceNetAdhocDiscoverModule),
        sceNetAdhocMatching(Modules.sceNetAdhocMatchingModule),
        sceNetIfhandle(Modules.sceNetIfhandleModule),
        sceOpenPSID(Modules.sceOpenPSIDModule),
        sceNp(Modules.sceNpModule),
        sceNpAuth(Modules.sceNpAuthModule),
        sceNpService(Modules.sceNpServiceModule),
        scePspNpDrm_user(Modules.scePspNpDrm_userModule),
        sceVaudio(Modules.sceVaudioModule);

    	private HLEModule module;
    	private int firmwareVersionAsDefault;	// FirmwareVersion where the module is loaded by default
    	private String[] prxNames;

    	// Module loaded by default in all Firmware versions
    	DefaultModule(HLEModule module) {
    		this.module = module;
    		firmwareVersionAsDefault = 100;	// Loaded by default in all firmwares (from 1.00)
    		prxNames = null;
    	}

    	// Module only loaded as a PRX, under different names
    	DefaultModule(HLEModule module, String[] prxNames) {
    		this.module = module;
    		firmwareVersionAsDefault = 100; //Integer.MAX_VALUE;	// Never loaded by default
    		this.prxNames = prxNames;
    	}

    	public HLEModule getModule() {
    		return module;
    	}

    	public String[] getPrxNames() {
    		return prxNames;
    	}

    	public boolean isLoadedByDefault(int firmwareVersion) {
    		return firmwareVersion >= firmwareVersionAsDefault;
    	}
    };

    public static HLEModuleManager getInstance() {
        if (instance == null) {
            instance = new HLEModuleManager();
        }
        return instance;
    }

    /** (String)"2.71" to (int)271 */
    public static int psfFirmwareVersionToInt(String firmwareVersion) {
        int version = 150;

        if (firmwareVersion != null) {
        	// HACK Some games have firmwareVersion = "5.00?"
        	while (!Character.isDigit(firmwareVersion.charAt(firmwareVersion.length() - 1))) {
        		firmwareVersion = firmwareVersion.substring(0, firmwareVersion.length() - 1);
        	}

        	version = (int)(Float.parseFloat(firmwareVersion) * 100);

            // HACK we started implementing stuff under 150 even if it existed in 100
            if (version < 150)
                version = 150;
        }

        return version;
    }

    public void Initialise(int firmwareVersion) {
        // Official syscalls start at 0x2000,
        // so we'll put the HLE syscalls far away at 0x4000.
        syscallCodeAllocator = 0x4000;

        syscallCodeToFunction = new HLEModuleFunction[syscallCodeAllocator];

        this.firmwareVersion = firmwareVersion;
        installDefaultModules();
        initialiseFlash0PRXMap();
    }

    // Add modules in flash that are loaded by default on this firmwareVersion
    private void installDefaultModules() {
        Modules.log.debug("Loading HLE firmware up to version " + firmwareVersion);
        for (DefaultModule defaultModule : DefaultModule.values()) {
        	if (defaultModule.isLoadedByDefault(firmwareVersion)) {
        		defaultModule.getModule().installModule(this, firmwareVersion);
        	}
        }
    }

    private void addToFlash0PRXMap(String prxName, HLEModule module) {
    	List<HLEModule> modules = new LinkedList<HLEModule>();
    	modules.add(module);
    	flash0prxMap.put(prxName.toLowerCase(), modules);
    }

    // Add modules in flash (or on UMD) that aren't loaded by default on this firmwareVersion
    private void initialiseFlash0PRXMap() {
        flash0prxMap = new HashMap<String, List<HLEModule>>();

        for (DefaultModule defaultModule : DefaultModule.values()) {
        	if (!defaultModule.isLoadedByDefault(firmwareVersion)) {
        		String[] prxNames = defaultModule.getPrxNames();
        		for (int i = 0; prxNames != null && i < prxNames.length; i++) {
        			addToFlash0PRXMap(prxNames[i], defaultModule.getModule());
        		}
        	}
        }
    }

    public boolean hasFlash0Module(String prxname) {
    	if (prxname == null) {
    		return false;
    	}

    	return flash0prxMap.containsKey(prxname.toLowerCase());
    }

    /** @return the UID assigned to the module or negative on error
     * TODO need to figure out how the uids work when 1 prx contains several modules. */
    public int LoadFlash0Module(String prxname) {
    	if (prxname != null) {
	        List<HLEModule> modules = flash0prxMap.get(prxname.toLowerCase());
	        if (modules != null) {
	            for (HLEModule module : modules) {
	                module.installModule(this, firmwareVersion);
	            }
	        }
    	}

        SceModule fakeModule = new SceModule(true);
        fakeModule.modname = prxname;
        fakeModule.write(Memory.getInstance(), fakeModule.address);
        Managers.modules.addModule(fakeModule);

        return fakeModule.modid;
    }

    public void UnloadFlash0Module(SceModule sceModule) {
    	if (sceModule == null) {
    		return;
    	}

    	if (sceModule.modname != null) {
	    	List<HLEModule> prx = flash0prxMap.get(sceModule.modname.toLowerCase());
	        if (prx != null) {
	            for (HLEModule module : prx) {
	                module.uninstallModule(this, firmwareVersion);
	            }
	        }
    	}

        // TODO terminate delete all threads that belong to this module

        sceModule.free();

        Managers.modules.removeModule(sceModule.modid);
    }

    public int getSyscallFromNid(int nid) {
        int code = NIDMapper.getInstance().nidToSyscall(nid);
        if (code == -1) {
            // Allocate an arbitrary syscall code to the function
            code = syscallCodeAllocator;
            // Add the new code to the NIDMapper
            NIDMapper.getInstance().addSyscallNid(nid, syscallCodeAllocator);
            syscallCodeAllocator++;
        }

        return code;
    }

    private void addSyscallCodeToFunction(int code, HLEModuleFunction func) {
    	if (code >= syscallCodeToFunction.length) {
    		// Extend the array
    		HLEModuleFunction[] extendedArray = new HLEModuleFunction[code + 100];
    		System.arraycopy(syscallCodeToFunction, 0, extendedArray, 0, syscallCodeToFunction.length);
    		syscallCodeToFunction = extendedArray;
    	}
    	syscallCodeToFunction[code] = func;
    }

    public void addFunction(int nid, HLEModuleFunction func) {
        int code = getSyscallFromNid(nid);
    	if (code < syscallCodeToFunction.length && syscallCodeToFunction[code] != null) {
        	Modules.log.error(String.format("Tried to register a second handler for NID 0x%08X called %s", nid, func.getFunctionName()));
    	} else {
            func.setNid(nid);
    		func.setSyscallCode(code);
    		addSyscallCodeToFunction(code, func);
    	}
    }

    public void addHLEFunction(HLEModuleFunction func) {
        func.setNid(-1);

        // Allocate an arbitrary syscall code to the function
        int code = syscallCodeAllocator++;

        func.setSyscallCode(code);
		addSyscallCodeToFunction(code, func);
    }

    public void removeFunction(HLEModuleFunction func) {
        int syscallCode = func.getSyscallCode();
        if (syscallCode >= 0 && syscallCode < syscallCodeToFunction.length) {
        	syscallCodeToFunction[syscallCode] = null;
        }
    }

    private HLEModuleFunction getFunctionFromSyscallCode(int code) {
    	if (code < 0 || code >= syscallCodeToFunction.length) {
    		return null;
    	}

    	return syscallCodeToFunction[code];
    }

    /**
     * @param code The syscall code to try and execute.
     * @return true if handled, false if not handled.
     */
    public boolean handleSyscall(int code) {
    	HLEModuleFunction func = getFunctionFromSyscallCode(code);
        if (func == null) {
        	return false;
        }

        func.execute(Emulator.getProcessor());

        return true;
    }

    public String functionName(int code) {
    	HLEModuleFunction func = getFunctionFromSyscallCode(code);
    	if (func == null) {
    		return null;
    	}

        return func.getFunctionName();
    }

	public void startModules() {
		if (modulesStarted) {
			return;
		}

		for(DefaultModule defaultModule : DefaultModule.values()) {
			if(defaultModule.module instanceof HLEStartModule) {
				((HLEStartModule)defaultModule.module).start();
			}
		}

		modulesStarted = true;
	}

	public void stopModules() {
		if (!modulesStarted) {
			return;
		}

		for(DefaultModule defaultModule : DefaultModule.values()) {
			if(defaultModule.module instanceof HLEStartModule) {
				((HLEStartModule)defaultModule.module).stop();
			}
		}

		modulesStarted = false;
	}

	public int getMaxSyscallCode() {
		return syscallCodeAllocator;
	}
}