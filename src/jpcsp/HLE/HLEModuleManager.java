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
package jpcsp.HLE;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import jpcsp.AllegrexOpcodes;
import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.NIDMapper;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.HLE.VFS.IVirtualFileSystem;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.SceIoStat;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.HLE.modules.ThreadManForUser;

/**
 * Manager for the HLE modules.
 * It defines which modules are loaded by default and
 * which modules are loaded explicitly from flash0 or from a PRX.
 * 
 * @author fiveofhearts
 * @author gid15
 */
@HLELogging
public class HLEModuleManager {
	private static Logger log = Modules.log;
    private static HLEModuleManager instance;

    public static final int HLESyscallNid = -1;
    public static final int InternalSyscallNid = -1;

    private boolean modulesStarted = false;
    private boolean startFromSyscall;
	private NIDMapper nidMapper;

    private HashMap<String, List<HLEModule>> flash0prxMap;
    private Set<HLEModule> installedModules = new HashSet<HLEModule>();
    private Map<Integer, HLEModuleFunction> syscallToFunction;
    private Map<Integer, HLEModuleFunction> nidToFunction;
    private Map<HLEModule, ModuleInfo> moduleInfos;

    private HLELogging defaultHLEFunctionLogging;

    /**
     * List of PSP modules that can be loaded when they are available.
     * They will then replace the HLE equivalent.
     */
	private static final String[] moduleFileNamesToBeLoaded = {
			  "flash0:/kd/utility.prx"
			, "flash0:/kd/vshbridge.prx"
			, "flash0:/vsh/module/paf.prx"
			, "flash0:/vsh/module/common_gui.prx"
			, "flash0:/vsh/module/common_util.prx"
//			, "flash0:/kd/memlmd_01g.prx"
//			, "flash0:/kd/loadcore.prx"
//			, "flash0:/kd/loadexec_01g.prx"
//			, "flash0:/kd/modulemgr.prx"
//			, "flash0:/kd/iofilemgr.prx"
//			, "flash0:/kd/isofs.prx"
//			, "flash0:/kd/umd9660.prx"
//			, "flash0:/kd/lfatfs.prx"
//			, "flash0:/kd/fatms.prx"
//			, "flash0:/kd/codepage.prx"
	};

	private static final String[] moduleFilesNameVshOnly = {
			"flash0:/kd/vshbridge.prx"
			, "flash0:/vsh/module/paf.prx"
			, "flash0:/vsh/module/common_gui.prx"
			, "flash0:/vsh/module/common_util.prx"
	};

	/**
     * List of modules that can be loaded
     * - by default in all firmwares (or only from a given FirmwareVersion)
     * - by sceKernelLoadModule/sceUtilityLoadModule from the flash0 or from the UMD (.prx)
     */
    private enum ModuleInfo {
    	SysMemUserForUser(Modules.SysMemUserForUserModule),
    	IoFileMgrForUser(Modules.IoFileMgrForUserModule),
    	IoFileMgrForKernel(Modules.IoFileMgrForKernelModule),
    	ThreadManForUser(Modules.ThreadManForUserModule),
    	ThreadManForKernel(Modules.ThreadManForKernelModule),
        SysMemForKernel(Modules.SysMemForKernelModule), // To be loaded after ThreadManForUser
    	InterruptManager(Modules.InterruptManagerModule),
    	LoadExecForUser(Modules.LoadExecForUserModule),
    	LoadExecForKernel(Modules.LoadExecForKernelModule),
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
        sceSuspendForKernel(Modules.sceSuspendForKernelModule),
        sceDmac(Modules.sceDmacModule),
        sceHprm(Modules.sceHprmModule),		// check if loaded by default
        sceAtrac3plus(Modules.sceAtrac3plusModule, new String[] { "libatrac3plus", "PSP_AV_MODULE_ATRAC3PLUS", "PSP_MODULE_AV_ATRAC3PLUS", "sceATRAC3plus_Library" }, "flash0:/kd/libatrac3plus.prx"),
        sceSasCore(Modules.sceSasCoreModule, new String[] { "sc_sascore", "PSP_AV_MODULE_SASCORE", "PSP_MODULE_AV_SASCORE", "sceSAScore" }, "flash0:/kd/sc_sascore.prx"),
        sceMpeg(Modules.sceMpegModule, new String[] { "mpeg", "PSP_AV_MODULE_MPEGBASE", "PSP_MODULE_AV_MPEGBASE", "sceMpeg_library" }, "flash0:/kd/mpeg.prx"),
        sceMpegVsh(Modules.sceMpegModule, new String[] { "mpeg_vsh", "mpeg_vsh370", }, "flash0:/kd/mpeg_vsh.prx"),
        sceMpegbase(Modules.sceMpegbaseModule, new String[] { "PSP_AV_MODULE_AVCODEC", "PSP_MODULE_AV_AVCODEC", "avcodec", "sceMpegbase_Driver" }, "flash0:/kd/avcodec.prx"),
        sceFont(Modules.sceFontModule, new String[] { "libfont", "sceFont_Library" }),
        scePsmfPlayer(Modules.scePsmfPlayerModule, new String[] { "libpsmfplayer", "psmf_jk", "scePsmfP_library" }),
        scePsmf(Modules.scePsmfModule, new String[] { "psmf", "scePsmf_library" }),
        sceMp3(Modules.sceMp3Module, new String[] { "PSP_AV_MODULE_MP3", "PSP_MODULE_AV_MP3", "LIBMP3" }),
        sceDeflt(Modules.sceDefltModule, new String[] { "libdeflt" }),
        sceWlan(Modules.sceWlanModule),
        sceNet(Modules.sceNetModule, new String[] { "pspnet", "PSP_NET_MODULE_COMMON", "PSP_MODULE_NET_COMMON" }, "flash0:/kd/pspnet.prx"),
        sceNetAdhoc(Modules.sceNetAdhocModule, new String[] { "pspnet_adhoc", "PSP_NET_MODULE_ADHOC", "PSP_MODULE_NET_ADHOC" }, "flash0:/kd/pspnet_adhoc.prx"),
        sceNetAdhocctl(Modules.sceNetAdhocctlModule, new String[] { "pspnet_adhocctl", "PSP_NET_MODULE_ADHOC", "PSP_MODULE_NET_ADHOC" }, "flash0:/kd/pspnet_adhocctl.prx"),
        sceNetAdhocDiscover(Modules.sceNetAdhocDiscoverModule, new String[] { "pspnet_adhoc_discover", "PSP_NET_MODULE_ADHOC", "PSP_MODULE_NET_ADHOC" }, "flash0:/kd/pspnet_adhoc_discover.prx"),
        sceNetAdhocMatching(Modules.sceNetAdhocMatchingModule, new String[] { "pspnet_adhoc_matching", "PSP_NET_MODULE_ADHOC", "PSP_MODULE_NET_ADHOC" }, "flash0:/kd/pspnet_adhoc_matching.prx"),
        sceNetAdhocTransInt(Modules.sceNetAdhocTransIntModule, new String[] { "pspnet_adhoc_transfer_int" }, "flash0:/kd/pspnet_adhoc_transfer_int.prx"),
        sceNetAdhocAuth(Modules.sceNetAdhocAuthModule, new String[] { "pspnet_adhoc_auth", "sceNetAdhocAuth_Service" }, "flash0:/kd/pspnet_adhoc_auth.prx"),
        sceNetAdhocDownload(Modules.sceNetAdhocDownloadModule, new String[] { "pspnet_adhoc_download" }, "flash0:/kd/pspnet_adhoc_download.prx"),
        sceNetIfhandle(Modules.sceNetIfhandleModule, new String[] { "ifhandle", "PSP_NET_MODULE_COMMON", "PSP_MODULE_NET_COMMON", "sceNetIfhandle_Service" }, "flash0:/kd/ifhandle.prx"),
        sceNetApctl(Modules.sceNetApctlModule, new String[] { "pspnet_apctl", "PSP_NET_MODULE_COMMON", "PSP_MODULE_NET_COMMON" }, "flash0:/kd/pspnet_apctl.prx"),
        sceNetInet(Modules.sceNetInetModule, new String[] { "pspnet_inet", "PSP_NET_MODULE_INET", "PSP_MODULE_NET_INET" }, "flash0:/kd/pspnet_inet.prx"),
        sceNetResolver(Modules.sceNetResolverModule, new String[] { "pspnet_resolver", "PSP_NET_MODULE_COMMON", "PSP_MODULE_NET_COMMON" }, "flash0:/kd/pspnet_resolver.prx"),
        sceNetUpnp(Modules.sceNetUpnpModule, new String[] { "pspnet_upnp", "PSP_MODULE_NET_UPNP" }, "flash0:/kd/pspnet_upnp.prx"),
        sceOpenPSID(Modules.sceOpenPSIDModule),
        sceNp(Modules.sceNpModule, new String[] { "np", "PSP_MODULE_NP_COMMON" }, "flash0:/kd/np.prx"),
        sceNpCore(Modules.sceNpCoreModule, new String[] { "np_core" }, "flash0:/kd/np_core.prx"),
        sceNpAuth(Modules.sceNpAuthModule, new String[] { "np_auth", "PSP_MODULE_NP_COMMON" }, "flash0:/kd/np_auth.prx"),
        sceNpService(Modules.sceNpServiceModule, new String[] { "np_service", "PSP_MODULE_NP_SERVICE" }, "flash0:/kd/np_service.prx"),
        sceNpCommerce2(Modules.sceNpCommerce2Module, new String[] { "np_commerce2", "PSP_MODULE_NP_COMMERCE2" }, "flash0:/kd/np_commerce2.prx"),
        sceNpCommerce2Store(Modules.sceNpCommerce2StoreModule, new String[] { "np_commerce2_store" }, "flash0:/kd/np_commerce2_store.prx"),
        sceNpCommerce2RegCam(Modules.sceNpCommerce2RegCamModule, new String[] { "np_commerce2_regcam" }, "flash0:/kd/np_commerce2_regcam.prx"),
        sceNpMatching2(Modules.sceNpMatching2Module, new String[] { "np_matching2", "PSP_MODULE_NP_MATCHING2" }, "flash0:/kd/np_matching2.prx"),
        sceNpInstall(Modules.sceNpInstallModule, new String[] { "np_inst" }, "flash0:/kd/np_inst.prx"),
        sceNpCamp(Modules.sceNpCampModule, new String[] { "np_campaign" }, "flash0:/kd/np_campaign.prx"),
        scePspNpDrm_user(Modules.scePspNpDrm_userModule, new String[] { "PSP_MODULE_NP_DRM", "npdrm" }),
        sceVaudio(Modules.sceVaudioModule, new String[] { "PSP_AV_MODULE_VAUDIO", "PSP_MODULE_AV_VAUDIO" }),
        sceMp4(Modules.sceMp4Module, new String[] { "PSP_MODULE_AV_MP4", "libmp4" }, "flash0:/kd/libmp4.prx"),
        mp4msv(Modules.mp4msvModule, new String[] { "mp4msv" }, "flash0:/kd/mp4msv.prx"),
        sceHttp(Modules.sceHttpModule, new String[] { "libhttp", "libhttp_rfc", "PSP_NET_MODULE_HTTP", "PSP_MODULE_NET_HTTP" }, "flash0:/kd/libhttp.prx"),
        sceHttps(Modules.sceHttpsModule, new String[] { "libhttp", "libhttp_rfc", "PSP_NET_MODULE_HTTP", "PSP_MODULE_NET_HTTP" }, "flash0:/kd/libhttp.prx"),
        sceHttpStorage(Modules.sceHttpStorageModule, new String[] { "http_storage" }, "flash0:/kd/http_storage.prx"),
        sceSsl(Modules.sceSslModule, new String[] { "libssl", "PSP_NET_MODULE_SSL", "PSP_MODULE_NET_SSL" }, "flash0:/kd/libssl.prx"),
        sceP3da(Modules.sceP3daModule),
        sceGameUpdate(Modules.sceGameUpdateModule, new String[] { "libgameupdate" }),
        sceUsbCam(Modules.sceUsbCamModule, new String[] { "PSP_USB_MODULE_CAM", "PSP_MODULE_USB_CAM", "usbcam" }),
        sceJpeg(Modules.sceJpegModule, new String[] { "PSP_AV_MODULE_AVCODEC", "PSP_MODULE_AV_AVCODEC" }, "flash0:/kd/avcodec.prx"),
        sceUsb(Modules.sceUsbModule),
        sceHeap(Modules.sceHeapModule, new String[] { "libheap" }),
        KDebugForKernel(Modules.KDebugForKernelModule),
        sceCcc(Modules.sceCccModule, new String[] { "libccc" }),
        scePauth(Modules.scePauthModule),
        sceSfmt19937(Modules.sceSfmt19937Module),
        sceMd5(Modules.sceMd5Module, new String[] { "libmd5" }),
        sceParseUri(Modules.sceParseUriModule, new String[] { "libparse_uri", "libhttp_rfc", "PSP_NET_MODULE_HTTP", "PSP_MODULE_NET_HTTP", "PSP_MODULE_NET_PARSEURI" }, "flash0:/kd/libparse_uri.prx"),
        sceUsbAcc(Modules.sceUsbAccModule, new String[] { "PSP_USB_MODULE_ACC", "USBAccBaseDriver" }),
        sceMt19937(Modules.sceMt19937Module, new String[] { "libmt19937" }),
        sceAac(Modules.sceAacModule, new String[] { "libaac", "PSP_AV_MODULE_AAC", "PSP_MODULE_AV_AAC" }),
        sceFpu(Modules.sceFpuModule, new String[] { "libfpu" }),
        sceUsbMic(Modules.sceUsbMicModule, new String[] { "usbmic", "PSP_USB_MODULE_MIC", "PSP_MODULE_USB_MIC", "USBCamMicDriver" }),
        sceAudioRouting(Modules.sceAudioRoutingModule),
        sceUsbGps(Modules.sceUsbGpsModule, new String[] { "PSP_USB_MODULE_GPS", "PSP_MODULE_USB_GPS", "usbgps" }),
        sceAudiocodec(Modules.sceAudiocodecModule, new String[] { "PSP_AV_MODULE_AVCODEC", "PSP_MODULE_AV_AVCODEC", "avcodec", "sceAudiocodec_Driver" }, "flash0:/kd/avcodec.prx"),
        sceVideocodec(Modules.sceVideocodecModule, new String[] { "PSP_AV_MODULE_AVCODEC", "PSP_MODULE_AV_AVCODEC", "avcodec", "sceVideocodec_Driver" }, "flash0:/kd/avcodec.prx"),
        sceAdler(Modules.sceAdlerModule, new String[] { "libadler" }),
        sceSha1(Modules.sceSha1Module, new String[] { "libsha1" }),
        sceSha256(Modules.sceSha256Module, new String[] { "libsha256" }),
        sceMeCore(Modules.sceMeCoreModule),
        sceMeBoot(Modules.sceMeBootModule),
        KUBridge(Modules.KUBridgeModule),
        SysclibForKernel(Modules.SysclibForKernelModule),
        semaphore(Modules.semaphoreModule),
        ModuleMgrForKernel(Modules.ModuleMgrForKernelModule),
        sceReg(Modules.sceRegModule),
        sceDve(Modules.sceDveModule),
        sceSysEventForKernel(Modules.sceSysEventForKernelModule),
        sceChkreg(Modules.sceChkregModule),
        sceMsAudio_Service(Modules.sceMsAudio_ServiceModule),
        sceMePower(Modules.sceMePowerModule),
        sceResmgr(Modules.sceResmgrModule),
        UtilsForKernel(Modules.UtilsForKernelModule),
        sceLibUpdateDL(Modules.sceLibUpdateDLModule, new String[] { "libupdown" }),
        sceParseHttp(Modules.sceParseHttpModule, new String[] { "libparse_http", "PSP_MODULE_NET_PARSEHTTP" }, "flash0:/kd/libparse_http.prx"),
        sceMgr_driver(Modules.sceMgr_driverModule),
        sceChnnlsv(Modules.sceChnnlsvModule, new String[] { "chnnlsv" }),
        sceUsbstor(Modules.sceUsbstorModule),
        sceIdStorage(Modules.sceIdStorageModule),
        sceCertLoader(Modules.sceCertLoaderModule, new String[] { "cert_loader", "PSP_MODULE_NET_SSL" }, "flash0:/kd/cert_loader.prx"),
        sceDNAS(Modules.sceDNASModule, new String[] { "libdnas" }),
        sceDNASCore(Modules.sceDNASCoreModule, new String[] { "libdnas_core" }),
        sceMcctrl(Modules.sceMcctrlModule, new String[] { "mcctrl" }),
        sceNetStun(Modules.sceNetStunModule),
        sceMeMemory(Modules.sceMeMemoryModule),
        sceMeVideo(Modules.sceMeVideoModule),
        sceMeAudio(Modules.sceMeAudioModule),
        InitForKernel(Modules.InitForKernelModule),
        sceMemab(Modules.sceMemabModule, new String[] { "memab", "sceMemab" }),
        DmacManForKernel(Modules.DmacManForKernelModule),
        sceSyscon(Modules.sceSysconModule),
        sceLed(Modules.sceLedModule),
        sceSysreg(Modules.sceSysregModule),
        scePsheet(Modules.scePsheetModule),
        sceUmdMan(Modules.sceUmdManModule),
        sceCodepage(Modules.sceCodepageModule),
        sceMSstor(Modules.sceMSstorModule),
        sceAta(Modules.sceAtaModule),
        sceGpio(Modules.sceGpioModule),
        sceNand(Modules.sceNandModule),
        sceBSMan(Modules.sceBSManModule),
        memlmd(Modules.memlmdModule),
        reboot(Modules.rebootModule),
        sceI2c(Modules.sceI2cModule),
        scePwm(Modules.scePwmModule),
        sceLcdc(Modules.sceLcdcModule),
        sceDmacplus(Modules.sceDmacplusModule),
        sceDdr(Modules.sceDdrModule);

    	private HLEModule module;
    	private boolean loadedByDefault;
    	private String[] names;
    	private String prxFileName;

    	// Module loaded by default in all Firmware versions
    	ModuleInfo(HLEModule module) {
    		this.module = module;
    		loadedByDefault = true;
    		names = null;
    	}

    	// Module only loaded as a PRX, under different names
    	ModuleInfo(HLEModule module, String[] prxNames) {
    		this.module = module;
    		loadedByDefault = false;
    		this.names = prxNames;
    	}

    	// Module only loaded as a PRX, under different names
    	ModuleInfo(HLEModule module, String[] prxNames, String prxFileName) {
    		this.module = module;
    		loadedByDefault = false;
    		this.names = prxNames;
    		this.prxFileName = prxFileName;
    	}

    	public HLEModule getModule() {
    		return module;
    	}

    	public String[] getNames() {
    		return names;
    	}

    	public boolean isLoadedByDefault() {
    		return loadedByDefault;
    	}

    	public String getPrxFileName() {
    		return prxFileName;
    	}
    };

    public static HLEModuleManager getInstance() {
        if (instance == null) {
            instance = new HLEModuleManager();
        }
        return instance;
    }

    private HLEModuleManager() {
		defaultHLEFunctionLogging = HLEModuleManager.class.getAnnotation(HLELogging.class);
		nidMapper = NIDMapper.getInstance();
		syscallToFunction = new HashMap<>();
		nidToFunction = new HashMap<>();
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

    public void init() {
    	installedModules.clear();
        installDefaultModules();
        initialiseFlash0PRXMap();
    }

    /**
     * Install the modules that are loaded by default on the current firmware version.
     */
    private void installDefaultModules() {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("Loading default HLE modules"));
    	}

    	for (ModuleInfo defaultModule : ModuleInfo.values()) {
        	if (defaultModule.isLoadedByDefault()) {
        		installModuleWithAnnotations(defaultModule.getModule());
        	} else {
        		// This module is not loaded by default on this firmware version.
        		// Install and Uninstall the module to register the module syscalls
        		// so that the loader can still resolve the imports for this module.
        		installModuleWithAnnotations(defaultModule.getModule());
        		uninstallModuleWithAnnotations(defaultModule.getModule());
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
        moduleInfos = new HashMap<HLEModule, ModuleInfo>();

        for (ModuleInfo moduleInfo : ModuleInfo.values()) {
        	HLEModule hleModule = moduleInfo.getModule();

        	moduleInfos.put(hleModule, moduleInfo);

        	if (!moduleInfo.isLoadedByDefault()) {
        		String[] names = moduleInfo.getNames();
        		for (int i = 0; names != null && i < names.length; i++) {
        			addToFlash0PRXMap(names[i], hleModule);
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

    public String getModulePrxFileName(String name) {
    	if (name != null) {
	        List<HLEModule> modules = flash0prxMap.get(name.toLowerCase());
	        if (modules != null) {
	            for (HLEModule module : modules) {
	            	ModuleInfo moduleInfo = moduleInfos.get(module);
	            	if (moduleInfo != null) {
	            		return moduleInfo.getPrxFileName();
	            	}
	            }
	        }
    	}

    	return null;
    }

    /** @return the UID assigned to the module or negative on error
     * TODO need to figure out how the uids work when 1 prx contains several modules. */
    public int LoadFlash0Module(String name) {
    	if (name != null) {
	        List<HLEModule> modules = flash0prxMap.get(name.toLowerCase());
	        if (modules != null) {
	            for (HLEModule module : modules) {
	            	installModuleWithAnnotations(module);
	            }
	        }
    	}

        SceModule fakeModule = new SceModule(true);
        fakeModule.modname = name;
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
	            	uninstallModuleWithAnnotations(module);
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

    public void addFunction(int nid, HLEModuleFunction func) {
    	int syscallCode;
    	if (nid == HLESyscallNid) {
    		syscallCode = nidMapper.getNewSyscallNumber();
    	} else {
	    	if (!nidMapper.addHLENid(nid, func.getFunctionName(), func.getModuleName(), func.getFirmwareVersion())) {
				log.error(String.format("Tried to register a second handler for NID 0x%08X called %s", nid, func.getFunctionName()));
	    	}

	    	nidToFunction.put(nid, func);

	    	syscallCode = nidMapper.getSyscallByNid(nid, func.getModuleName());
    	}

    	if (syscallCode >= 0) {
    		func.setSyscallCode(syscallCode);
    		syscallToFunction.put(syscallCode, func);
    	}
    }

    public HLEModuleFunction getFunctionFromSyscallCode(int syscallCode) {
    	return syscallToFunction.get(syscallCode);
    }

    public HLEModuleFunction getFunctionFromAddress(int address) {
    	int nid = nidMapper.getNidByAddress(address);
    	if (nid == 0) {
    		// Verify if this not the address of a stub call:
    		//   J   realAddress
    		//   NOP
        	Memory mem = Memory.getInstance();
        	if ((mem.read32(address) >>> 26) == AllegrexOpcodes.J) {
        		if (mem.read32(address + 4) == ThreadManForUser.NOP()) {
        			int jumpAddress = (mem.read32(address) & 0x03FFFFFF) << 2;

        			nid = nidMapper.getNidByAddress(jumpAddress);
        		}
        	}
    	}

    	if (nid == 0) {
    		return null;
    	}

    	HLEModuleFunction func = nidToFunction.get(nid);

    	return func;
    }

    public HLEModuleFunction getFunctionFromNID(int nid) {
    	return nidToFunction.get(nid);
    }

    public void removeFunction(HLEModuleFunction func) {
    	nidMapper.unloadNid(func.getNid());
    }

    public void startModules(boolean startFromSyscall) {
		if (modulesStarted) {
			return;
		}

		this.startFromSyscall = startFromSyscall;

		for (ModuleInfo defaultModule : ModuleInfo.values()) {
			if (defaultModule.module.isStarted()) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("Module %s already started", defaultModule.module.getName()));
				}
			} else {
				if (log.isDebugEnabled()) {
					log.debug(String.format("Starting module %s", defaultModule.module.getName()));
				}

				defaultModule.module.start();

				if (log.isDebugEnabled()) {
					log.debug(String.format("Started module %s", defaultModule.module.getName()));
				}
			}
		}

		this.startFromSyscall = false;
		modulesStarted = true;
	}

	public void stopModules() {
		if (!modulesStarted) {
			return;
		}

		for (ModuleInfo defaultModule : ModuleInfo.values()) {
			defaultModule.module.stop();
		}

		modulesStarted = false;
	}

	public boolean isStartFromSyscall() {
		return startFromSyscall;
	}

	private void installFunctionWithAnnotations(HLEFunction hleFunction, Method method, HLEModule hleModule) {
		HLEUnimplemented hleUnimplemented = method.getAnnotation(HLEUnimplemented.class);
		HLELogging hleLogging = method.getAnnotation(HLELogging.class);

		// Take the module default logging if no HLELogging has been
		// defined at the function level and if the function is not
		// unimplemented (which will produce it's own logging).
		if (hleLogging == null) {
			if (hleUnimplemented != null) {
				// Take the logging level of the HLEUnimplemented class
				// as default value for unimplemented functions
				hleLogging = HLEUnimplemented.class.getAnnotation(HLELogging.class);
			} else {
				HLELogging hleModuleLogging = method.getDeclaringClass().getAnnotation(HLELogging.class);
				if (hleModuleLogging != null) {
					// Take the module default logging
					hleLogging = hleModuleLogging;
				} else {
					hleLogging = defaultHLEFunctionLogging;
				}
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

		HLEModuleFunction hleModuleFunction = new HLEModuleFunction(moduleName, functionName, hleFunction.nid(), hleModule, method, hleFunction.checkInsideInterrupt(), hleFunction.checkDispatchThreadEnabled(), hleFunction.stackUsage(), hleFunction.version());

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
	 * Iterates over an object fields searching for HLEFunction annotations and install them.
	 * 
	 * @param hleModule
	 */
	public void installModuleWithAnnotations(HLEModule hleModule) {
		if (installedModules.contains(hleModule)) {
			return;
		}

		try {
			for (Method method : hleModule.getClass().getMethods()) {
				HLEFunction hleFunction = method.getAnnotation(HLEFunction.class);
				if (hleFunction != null) {
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
	 */
	public void uninstallModuleWithAnnotations(HLEModule hleModule) {
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

	private boolean isModuleFileNameVshOnly(String moduleFileName) {
		for (int i = 0; i < moduleFilesNameVshOnly.length; i++) {
			if (moduleFilesNameVshOnly[i].equalsIgnoreCase(moduleFileName)) {
				return true;
			}
		}

		return false;
	}

	public void loadAvailableFlash0Modules(boolean fromSyscall) {
		boolean runningFromVsh = Emulator.getMainGUI().isRunningFromVsh() && !fromSyscall;

		List<String> availableModuleFileNames = new LinkedList<>();
		for (String moduleFileName : moduleFileNamesToBeLoaded) {
			if (runningFromVsh || !isModuleFileNameVshOnly(moduleFileName)) {
				StringBuilder localFileName = new StringBuilder();
				IVirtualFileSystem vfs = Modules.IoFileMgrForUserModule.getVirtualFileSystem(moduleFileName, localFileName);
				if (vfs != null && vfs.ioGetstat(localFileName.toString(), new SceIoStat()) == 0) {
					// The module is available, load it
					availableModuleFileNames.add(moduleFileName);
				}
			}
		}

		if (availableModuleFileNames.isEmpty()) {
			// No module available, do nothing
			return;
		}

		// This HLE module need to be started in order
		// to be able to load and start the available modules.
		Modules.ModuleMgrForUserModule.start();

    	int startPriority = 0x10;
    	for (String moduleFileName : availableModuleFileNames) {
        	if (log.isInfoEnabled()) {
        		log.info(String.format("Loading and starting the module '%s', it will replace the equivalent HLE functions", moduleFileName));
        	}

        	IAction onModuleStartAction = null;

        	// loadcore.prx requires start parameters
        	if ("flash0:/kd/loadcore.prx".equals(moduleFileName)) {
        		onModuleStartAction = Modules.LoadCoreForKernelModule.getModuleStartAction();
        	}

        	Modules.ModuleMgrForUserModule.hleKernelLoadAndStartModule(moduleFileName, startPriority++, onModuleStartAction);
    	}
	}
}