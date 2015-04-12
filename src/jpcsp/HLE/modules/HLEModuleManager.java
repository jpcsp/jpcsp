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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.NIDMapper;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.types.SceModule;

/**
 * Manager for the HLE modules.
 * It defines which modules are loaded by default and
 * which modules are loaded explicitly from flash0 or from a PRX.
 * 
 * @author fiveofhearts
 */
public class HLEModuleManager {
	private static Logger log = Modules.log;
    private static HLEModuleManager instance;

    public static final int HLESyscallNid = -1;

    /**
     * Remember all the allocated syscalls, even when they are uninstalled
     * so that SyscallHandler can output an appropriate message when trying
     * to execute an uninstalled syscall.
     */
    private HLEModuleFunction[] syscallCodeToFunction;
    private int syscallCodeAllocator;
    private boolean modulesStarted = false;
    private boolean startFromSyscall;

    private HashMap<String, List<HLEModule>> flash0prxMap;
    private Set<HLEModule> installedModules = new HashSet<HLEModule>();

    /**
     * The current firmware version as an integer value in this format:
     *   ABB
     * where A = major and B = minor, for example 271.
     */
    private int firmwareVersion;

    /**
     * List of modules that can be loaded
     * - by default in all firmwares (or only from a given FirmwareVersion)
     * - by sceKernelLoadModule/sceUtilityLoadModule from the flash0 or from the UMD (.prx)
     */
    private enum DefaultModule {
    	SysMemUserForUser(Modules.SysMemUserForUserModule),
        SysMemForKernel(Modules.SysMemForKernelModule),
    	IoFileMgrForUser(Modules.IoFileMgrForUserModule),
    	ThreadManForUser(Modules.ThreadManForUserModule),
    	InterruptManager(Modules.InterruptManagerModule),
    	LoadExecForUser(Modules.LoadExecForUserModule),
        StdioForUser(Modules.StdioForUserModule),
        StdioForKernel(Modules.StdioForKernelModule),
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
        sceAtrac3plus(Modules.sceAtrac3plusModule, new String[] { "libatrac3plus", "PSP_AV_MODULE_ATRAC3PLUS", "PSP_MODULE_AV_ATRAC3PLUS", "sceATRAC3plus_Library" }),
        sceSasCore(Modules.sceSasCoreModule, new String[] { "sc_sascore", "PSP_AV_MODULE_SASCORE", "PSP_MODULE_AV_SASCORE", "sceSAScore" } ),
        sceMpeg(Modules.sceMpegModule, new String[] { "mpeg", "mpeg_vsh", "PSP_AV_MODULE_MPEGBASE", "PSP_MODULE_AV_MPEGBASE", "sceMpeg_library" }),
        sceFont(Modules.sceFontModule, new String[] { "libfont", "sceFont_Library" }),
        scePsmfPlayer(Modules.scePsmfPlayerModule, new String[] { "libpsmfplayer", "psmf_jk", "scePsmfP_library" }),
        scePsmf(Modules.scePsmfModule, new String[] { "psmf", "scePsmf_library" }),
        sceMp3(Modules.sceMp3Module, new String[] { "PSP_AV_MODULE_MP3", "PSP_MODULE_AV_MP3", "LIBMP3" }),
        sceDeflt(Modules.sceDefltModule, new String[] { "libdeflt" }),
        sceWlan(Modules.sceWlanModule),
        sceNet(Modules.sceNetModule, new String[] { "pspnet", "PSP_NET_MODULE_COMMON", "PSP_MODULE_NET_COMMON" }),
        sceNetAdhoc(Modules.sceNetAdhocModule, new String[] { "pspnet_adhoc", "PSP_NET_MODULE_ADHOC", "PSP_MODULE_NET_ADHOC" }),
        sceNetAdhocctl(Modules.sceNetAdhocctlModule, new String[] { "pspnet_adhocctl", "PSP_NET_MODULE_ADHOC", "PSP_MODULE_NET_ADHOC" }),
        sceNetAdhocDiscover(Modules.sceNetAdhocDiscoverModule, new String[] { "pspnet_adhoc_discover", "PSP_NET_MODULE_ADHOC", "PSP_MODULE_NET_ADHOC" }),
        sceNetAdhocMatching(Modules.sceNetAdhocMatchingModule, new String[] { "pspnet_adhoc_matching", "PSP_NET_MODULE_ADHOC", "PSP_MODULE_NET_ADHOC" }),
        sceNetIfhandle(Modules.sceNetIfhandleModule, new String[] { "ifhandle", "PSP_NET_MODULE_COMMON", "PSP_MODULE_NET_COMMON" }),
        sceNetApctl(Modules.sceNetApctlModule, new String[] { "pspnet_apctl", "PSP_NET_MODULE_COMMON", "PSP_MODULE_NET_COMMON" }),
        sceNetInet(Modules.sceNetInetModule, new String[] { "pspnet_inet", "PSP_NET_MODULE_INET", "PSP_MODULE_NET_INET" }),
        sceNetResolver(Modules.sceNetResolverModule, new String[] { "pspnet_resolver", "PSP_NET_MODULE_COMMON", "PSP_MODULE_NET_COMMON" }),
        sceOpenPSID(Modules.sceOpenPSIDModule),
        sceNp(Modules.sceNpModule, new String[] { "PSP_MODULE_NP_COMMON" }),
        sceNpAuth(Modules.sceNpAuthModule, new String[] { "PSP_MODULE_NP_COMMON" }),
        sceNpService(Modules.sceNpServiceModule, new String[] { "PSP_MODULE_NP_SERVICE" }),
        scePspNpDrm_user(Modules.scePspNpDrm_userModule, new String[] { "PSP_MODULE_NP_DRM" }),
        sceVaudio(Modules.sceVaudioModule, new String[] { "PSP_AV_MODULE_VAUDIO", "PSP_MODULE_AV_VAUDIO" }),
        sceMp4(Modules.sceMp4Module, new String[] { "PSP_MODULE_AV_MP4" }),
        sceHttp(Modules.sceHttpModule, new String[] { "libhttp_rfc", "PSP_NET_MODULE_HTTP", "PSP_MODULE_NET_HTTP" }),
        sceHttps(Modules.sceHttpsModule),
        sceSsl(Modules.sceSslModule, new String[] { "libssl", "PSP_NET_MODULE_SSL", "PSP_MODULE_NET_SSL" }),
        sceP3da(Modules.sceP3daModule),
        sceGameUpdate(Modules.sceGameUpdateModule),
        sceUsbCam(Modules.sceUsbCamModule, new String[] { "PSP_USB_MODULE_CAM", "PSP_MODULE_USB_CAM", "USBCamDriver" }),
        sceJpeg(Modules.sceJpegModule, new String[] { "PSP_AV_MODULE_AVCODEC", "PSP_MODULE_AV_AVCODEC" }),
        sceUsb(Modules.sceUsbModule),
        sceHeap(Modules.sceHeapModule, new String[] { "libheap" }),
        KDebugForKernel(Modules.KDebugForKernelModule),
        sceCcc(Modules.sceCccModule, new String[] { "libccc" }),
        scePauth(Modules.scePauthModule),
        sceSfmt19937(Modules.sceSfmt19937Module),
        sceMd5(Modules.sceMd5Module, new String[] { "libmd5" }),
        sceParseUri(Modules.sceParseUriModule, new String[] { "libhttp_rfc", "PSP_NET_MODULE_HTTP", "PSP_MODULE_NET_HTTP" }),
        sceUsbAcc(Modules.sceUsbAccModule, new String[] { "PSP_USB_MODULE_ACC", "USBAccBaseDriver" }),
        sceMt19937(Modules.sceMt19937Module, new String[] { "libmt19937" }),
        sceAac(Modules.sceAacModule, new String[] { "libaac", "PSP_AV_MODULE_AAC", "PSP_MODULE_AV_AAC" }),
        sceFpu(Modules.sceFpuModule, new String[] { "libfpu" }),
        sceUsbMic(Modules.sceUsbMicModule, new String[] { "usbmic", "PSP_USB_MODULE_MIC", "PSP_MODULE_USB_MIC", "USBCamMicDriver" }),
        sceAudioRouting(Modules.sceAudioRoutingModule),
        sceUsbGps(Modules.sceUsbGpsModule, new String[] { "PSP_USB_MODULE_GPS", "PSP_MODULE_USB_GPS" }),
        sceAudiocodec(Modules.sceAudiocodecModule, new String[] { "PSP_AV_MODULE_AVCODEC", "PSP_MODULE_AV_AVCODEC", "avcodec" }),
        sceAdler(Modules.sceAdlerModule, new String[] { "libadler" }),
        sceSha1(Modules.sceSha1Module, new String[] { "libsha1" }),
        sceSha256(Modules.sceSha256Module, new String[] { "libsha256" }),
		SystemCtrlForKernel(Modules.SystemCtrlForKernelModule),
		ModuleMgrForKernel(Modules.ModuleMgrForKernelModule);

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
    		firmwareVersionAsDefault = Integer.MAX_VALUE;	// Never loaded by default
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
        int version = Emulator.getInstance().getFirmwareVersion();

        if (firmwareVersion != null) {
        	// Some games have firmwareVersion = "5.00?", keep only the digits
        	while (!Character.isDigit(firmwareVersion.charAt(firmwareVersion.length() - 1))) {
        		firmwareVersion = firmwareVersion.substring(0, firmwareVersion.length() - 1);
        	}

        	version = (int)(Float.parseFloat(firmwareVersion) * 100);

            // We started implementing stuff under 150 even if it existed in 100
            if (version < 150) {
                version = 150;
            }
        }

        return version;
    }

    public void Initialise(int firmwareVersion) {
    	if (syscallCodeToFunction == null) {
    		// Official syscalls start at 0x2000,
    		// so we'll put the HLE syscalls far away at 0x4000.
    		syscallCodeAllocator = 0x4000;

    		syscallCodeToFunction = new HLEModuleFunction[syscallCodeAllocator];
    	} else {
    		// Remove all the functions.
    		// Do not reset the syscall codes, they still might be in use in
    		// already loaded modules.
    		for (int i = 0; i < syscallCodeToFunction.length; i++) {
    			syscallCodeToFunction[i] = null;
    		}
    	}

    	installedModules.clear();
        this.firmwareVersion = firmwareVersion;
        installDefaultModules();
        initialiseFlash0PRXMap();
    }

    /**
     * Install the modules that are loaded by default on the current firmware version.
     */
    private void installDefaultModules() {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("Loading HLE firmware up to version %d", firmwareVersion));
    	}

    	for (DefaultModule defaultModule : DefaultModule.values()) {
        	if (defaultModule.isLoadedByDefault(firmwareVersion)) {
        		installModuleWithAnnotations(defaultModule.getModule(), firmwareVersion);
        	} else {
        		// This module is not loaded by default on this firmware version.
        		// Install and Uninstall the module to register the module syscalls
        		// so that the loader can still resolve the imports for this module.
        		installModuleWithAnnotations(defaultModule.getModule(), firmwareVersion);
        		uninstallModuleWithAnnotations(defaultModule.getModule(), firmwareVersion);
        	}
        }
    }

    private void addToFlash0PRXMap(String prxName, HLEModule module) {
    	prxName = prxName.toLowerCase();
    	if (!flash0prxMap.containsKey(prxName)) {
    		flash0prxMap.put(prxName, new LinkedList<HLEModule>());
    	}
    	List<HLEModule> modules = flash0prxMap.get(prxName);
    	modules.add(module);
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
	            	installModuleWithAnnotations(module, firmwareVersion);
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
	            	uninstallModuleWithAnnotations(module, firmwareVersion);
	            }
	        }
    	}

        // TODO terminate delete all threads that belong to this module

        sceModule.free();

        Managers.modules.removeModule(sceModule.modid);

        if (!sceModule.isFlashModule) {
        	// Invalidate the compiled code from the unloaded module
        	RuntimeContext.invalidateAll();
        }
    }

    public int getSyscallFromNid(int nid) {
    	// Is this an HLE syscall?
    	if (nid == HLESyscallNid) {
    		return syscallCodeAllocator++;
    	}

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
    		if (func != syscallCodeToFunction[code]) {
    			log.error(String.format("Tried to register a second handler for NID 0x%08X called %s", nid, func.getFunctionName()));
    		} else {
    			func.setNid(nid);
    			func.setSyscallCode(code);
    		}
    	} else {
            func.setNid(nid);
    		func.setSyscallCode(code);
    		addSyscallCodeToFunction(code, func);
    	}
    }

    public void removeFunction(HLEModuleFunction func) {
        int syscallCode = func.getSyscallCode();
        if (syscallCode >= 0 && syscallCode < syscallCodeToFunction.length) {
        	syscallCodeToFunction[syscallCode] = null;
        }
    }

    public HLEModuleFunction getFunctionFromSyscallCode(int code) {
    	if (code < 0 || code >= syscallCodeToFunction.length) {
    		return null;
    	}

    	return syscallCodeToFunction[code];
    }

    public String functionName(int code) {
    	HLEModuleFunction func = getFunctionFromSyscallCode(code);
    	if (func == null) {
    		return null;
    	}

        return func.getFunctionName();
    }

	public void startModules(boolean startFromSyscall) {
		if (modulesStarted) {
			return;
		}

		this.startFromSyscall = startFromSyscall;

		for (DefaultModule defaultModule : DefaultModule.values()) {
			defaultModule.module.start();
		}

		this.startFromSyscall = false;
		modulesStarted = true;
	}

	public void stopModules() {
		if (!modulesStarted) {
			return;
		}

		for (DefaultModule defaultModule : DefaultModule.values()) {
			defaultModule.module.stop();
		}

		modulesStarted = false;
	}

	public boolean isStartFromSyscall() {
		return startFromSyscall;
	}

	public int getMaxSyscallCode() {
		return syscallCodeAllocator;
	}

	private void installFunctionWithAnnotations(HLEFunction hleFunction, Method method, HLEModule hleModule) {
		HLEUnimplemented hleUnimplemented = method.getAnnotation(HLEUnimplemented.class);
		HLELogging hleLogging = method.getAnnotation(HLELogging.class);

		// Take the module default logging if no HLELogging has been
		// defined at the function level and if the function is not
		// unimplemented (which will produce it's own logging).
		if (hleLogging == null && hleUnimplemented == null) {
			HLELogging hleModuleLogging = method.getDeclaringClass().getAnnotation(HLELogging.class);
			if (hleModuleLogging != null) {
				// Take the module default logging
				hleLogging = hleModuleLogging;
			}
		}

		String moduleName = hleFunction.moduleName();
		String functionName = hleFunction.functionName();

		if (moduleName.length() == 0) {
			moduleName = hleModule.getName();
		}

		if (functionName.length() == 0) {
			functionName = method.getName();
		}

		HLEModuleFunction hleModuleFunction = new HLEModuleFunction(moduleName, functionName, hleModule, method, hleFunction.checkInsideInterrupt(), hleFunction.checkDispatchThreadEnabled(), hleFunction.stackUsage());

		if (hleUnimplemented != null) {
			hleModuleFunction.setUnimplemented(true);
		}

		if (hleLogging != null) {
			hleModuleFunction.setLoggingLevel(hleLogging.level());
		}

		hleModule.installedHLEModuleFunctions.put(functionName, hleModuleFunction);

		addFunction(hleFunction.nid(), hleModuleFunction);
	}

	/**
	 * Iterates over an object fields searching for HLEFunction annotations and if the specified
	 * version is greater than the required version for that HLEFunction, it will install it.
	 * 
	 * @param hleModule
	 * @param version
	 */
	public void installModuleWithAnnotations(HLEModule hleModule, int version) {
		if (installedModules.contains(hleModule)) {
			return;
		}

		try {
			for (Method method : hleModule.getClass().getMethods()) {
				HLEFunction hleFunction = method.getAnnotation(HLEFunction.class);
				if (hleFunction != null && version >= hleFunction.version()) {
					installFunctionWithAnnotations(hleFunction, method, hleModule);
				}
			}
		} catch (Exception e) {
			log.error("installModuleWithAnnotations", e);
		}

		installedModules.add(hleModule);
		hleModule.load();
	}
	
	/**
	 * Same as installModuleWithAnnotations but uninstalling.
	 * 
	 * @param hleModule
	 * @param version
	 */
	public void uninstallModuleWithAnnotations(HLEModule hleModule, int version) {
		try {
			for (HLEModuleFunction hleModuleFunction : hleModule.installedHLEModuleFunctions.values()) {
				this.removeFunction(hleModuleFunction);
			}
		} catch (Exception e) {
			log.error("uninstallModuleWithAnnotations", e);
		}

		installedModules.remove(hleModule);
		hleModule.unload();
	}
}