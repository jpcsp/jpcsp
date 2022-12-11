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

import static jpcsp.Allegrex.Common._a2;
import static jpcsp.Allegrex.Common._a3;
import static jpcsp.Allegrex.Common._ra;
import static jpcsp.Allegrex.Common._sp;
import static jpcsp.HLE.modules.SysMemUserForUser.KERNEL_PARTITION_ID;
import static jpcsp.HLE.modules.SysMemUserForUser.PSP_SMEM_Low;
import static jpcsp.HLE.modules.SysMemUserForUser.defaultSizeAlignment;
import static jpcsp.util.HLEUtilities.ADDIU;
import static jpcsp.util.HLEUtilities.JAL;
import static jpcsp.util.HLEUtilities.JR;
import static jpcsp.util.HLEUtilities.LI;
import static jpcsp.util.HLEUtilities.LW;
import static jpcsp.util.HLEUtilities.NOP;
import static jpcsp.util.HLEUtilities.SW;
import static jpcsp.util.HLEUtilities.SYSCALL;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
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
import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.VFS.IVirtualFileSystem;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.SceIoStat;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.HLE.modules.SysMemUserForUser.SysMemInfo;
import jpcsp.hardware.Model;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryWriter;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;
import jpcsp.util.HLEUtilities;
import jpcsp.util.Utilities;

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
	private static final int STATE_VERSION = 0;

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
			, "flash0:/kd/wlan.prx"
			, "flash0:/kd/wlanfirm_01g.prx"
			, "flash0:/kd/memlmd_01g.prx"
			, "flash0:/kd/lowio.prx"
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

	private static final String[] moduleFileNamesVshOnly = {
			  "flash0:/kd/vshbridge.prx"
			, "flash0:/vsh/module/paf.prx"
			, "flash0:/vsh/module/common_gui.prx"
			, "flash0:/vsh/module/common_util.prx"
	};

	/**
	 * List of PSP modules that do require LLE emulation when loaded.
	 */
	private static final String[] moduleFileNamesLLE = {
			  "flash0:/kd/lowio.prx"
			, "flash0:/kd/audio.prx"
			, "flash0:/kd/wlan.prx"
			, "flash0:/kd/memlmd_01g.prx"
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
        KernelLibrary(Modules.Kernel_LibraryModule, new String[] { "usersystemlib", "sceKernelLibrary", "Kernel_Library" }, "flash0:/kd/usersystemlib.prx"),
        ModuleMgrForUser(Modules.ModuleMgrForUserModule),
        LoadCoreForKernel(Modules.LoadCoreForKernelModule),
        sceCtrl(Modules.sceCtrlModule, new String[] { "sceCtrl" }, "flash0:/kd/ctrl.prx"),
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
        scePsmfPlayer(Modules.scePsmfPlayerModule, new String[] { "libpsmfplayer", "scePsmfP_library", "psmf_jk", "jkPsmfP_library" }),
        scePsmf(Modules.scePsmfModule, new String[] { "psmf", "scePsmf_library" }),
        sceMp3(Modules.sceMp3Module, new String[] { "PSP_AV_MODULE_MP3", "PSP_MODULE_AV_MP3", "LIBMP3", "sceMp3_Library" }, "flash0:/kd/libmp3.prx"),
        sceDeflt(Modules.sceDefltModule, new String[] { "libdeflt", "sceDEFLATE_Library", "DEFLATE_Library" }),
        sceWlan(Modules.sceWlanModule),
        sceNet(Modules.sceNetModule, new String[] { "pspnet", "PSP_NET_MODULE_COMMON", "PSP_MODULE_NET_COMMON", "sceNet_Library" }, "flash0:/kd/pspnet.prx"),
        sceNetAdhoc(Modules.sceNetAdhocModule, new String[] { "pspnet_adhoc", "PSP_NET_MODULE_ADHOC", "PSP_MODULE_NET_ADHOC", "sceNetAdhoc_Library" }, "flash0:/kd/pspnet_adhoc.prx"),
        sceNetAdhocctl(Modules.sceNetAdhocctlModule, new String[] { "pspnet_adhocctl", "PSP_NET_MODULE_ADHOC", "PSP_MODULE_NET_ADHOC", "sceNetAdhocctl_Library" }, "flash0:/kd/pspnet_adhocctl.prx"),
        sceNetAdhocDiscover(Modules.sceNetAdhocDiscoverModule, new String[] { "pspnet_adhoc_discover", "PSP_NET_MODULE_ADHOC", "PSP_MODULE_NET_ADHOC", "sceNetAdhocDiscover_Library" }, "flash0:/kd/pspnet_adhoc_discover.prx"),
        sceNetAdhocMatching(Modules.sceNetAdhocMatchingModule, new String[] { "pspnet_adhoc_matching", "PSP_NET_MODULE_ADHOC", "PSP_MODULE_NET_ADHOC", "sceNetAdhocMatching_Library" }, "flash0:/kd/pspnet_adhoc_matching.prx"),
        sceNetAdhocTransInt(Modules.sceNetAdhocTransIntModule, new String[] { "pspnet_adhoc_transfer_int", "sceNetAdhocTransInt_Library" }, "flash0:/kd/pspnet_adhoc_transfer_int.prx"),
        sceNetAdhocAuth(Modules.sceNetAdhocAuthModule, new String[] { "pspnet_adhoc_auth", "sceNetAdhocAuth_Service", "PSP_NET_MODULE_ADHOC", "PSP_MODULE_NET_ADHOC" }, "flash0:/kd/pspnet_adhoc_auth.prx"),
        sceNetAdhocDownload(Modules.sceNetAdhocDownloadModule, new String[] { "pspnet_adhoc_download", "sceNetAdhocDownload_Library", "PSP_NET_MODULE_ADHOC", "PSP_MODULE_NET_ADHOC" }, "flash0:/kd/pspnet_adhoc_download.prx"),
        sceNetIfhandle(Modules.sceNetIfhandleModule, new String[] { "ifhandle", "PSP_NET_MODULE_COMMON", "PSP_MODULE_NET_COMMON", "sceNetIfhandle_Service" }, "flash0:/kd/ifhandle.prx"),
        sceNetApctl(Modules.sceNetApctlModule, new String[] { "pspnet_apctl", "PSP_NET_MODULE_COMMON", "PSP_MODULE_NET_COMMON", "sceNetApctl_Library" }, "flash0:/kd/pspnet_apctl.prx"),
        sceNetInet(Modules.sceNetInetModule, new String[] { "pspnet_inet", "PSP_NET_MODULE_INET", "PSP_MODULE_NET_INET", "sceNetInet_Library" }, "flash0:/kd/pspnet_inet.prx"),
        sceNetResolver(Modules.sceNetResolverModule, new String[] { "pspnet_resolver", "PSP_NET_MODULE_COMMON", "PSP_MODULE_NET_COMMON", "sceNetResolver_Library" }, "flash0:/kd/pspnet_resolver.prx"),
        sceNetUpnp(Modules.sceNetUpnpModule, new String[] { "pspnet_upnp", "PSP_MODULE_NET_UPNP", "sceNetUpnp_Library" }, "flash0:/kd/pspnet_upnp.prx"),
        sceOpenPSID(Modules.sceOpenPSIDModule),
        sceNp(Modules.sceNpModule, new String[] { "np", "PSP_MODULE_NP_COMMON" }, "flash0:/kd/np.prx"),
        sceNpCore(Modules.sceNpCoreModule, new String[] { "np_core", "PSP_MODULE_NP_COMMON" }, "flash0:/kd/np_core.prx"),
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
        mp4msv(Modules.mp4msvModule, new String[] { "mp4msv", "PSP_MODULE_AV_MP4" }, "flash0:/kd/mp4msv.prx"),
        sceHttp(Modules.sceHttpModule, new String[] { "libhttp", "libhttp_rfc", "PSP_NET_MODULE_HTTP", "PSP_MODULE_NET_HTTP" }, "flash0:/kd/libhttp.prx"),
        sceHttps(Modules.sceHttpsModule, new String[] { "libhttp", "libhttp_rfc", "PSP_NET_MODULE_HTTP", "PSP_MODULE_NET_HTTP" }, "flash0:/kd/libhttp.prx"),
        sceHttpStorage(Modules.sceHttpStorageModule, new String[] { "http_storage", "PSP_MODULE_NET_HTTPSTORAGE" }, "flash0:/kd/http_storage.prx"),
        sceSsl(Modules.sceSslModule, new String[] { "libssl", "PSP_NET_MODULE_SSL", "PSP_MODULE_NET_SSL" }, "flash0:/kd/libssl.prx"),
        sceP3da(Modules.sceP3daModule),
        sceGameUpdate(Modules.sceGameUpdateModule, new String[] { "libgameupdate" }),
        sceUsbCam(Modules.sceUsbCamModule, new String[] { "PSP_USB_MODULE_CAM", "PSP_MODULE_USB_CAM", "usbcam" }),
        sceJpeg(Modules.sceJpegModule, new String[] { "PSP_AV_MODULE_AVCODEC", "PSP_MODULE_AV_AVCODEC" }, "flash0:/kd/avcodec.prx"),
        sceUsb(Modules.sceUsbModule),
        sceHeap(Modules.sceHeapModule, new String[] { "libheap", "sceHeap_Library" }),
        KDebugForKernel(Modules.KDebugForKernelModule),
        sceCcc(Modules.sceCccModule, new String[] { "libccc", "sceCcc_Library" }),
        scePauth(Modules.scePauthModule),
        sceSfmt19937(Modules.sceSfmt19937Module, new String[] { "libsfmt19937", "sceSfmt19937_Library" }),
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
        sceMemab(Modules.sceMemabModule, new String[] { "memab", "sceMemab", "PSP_NET_MODULE_ADHOC", "PSP_MODULE_NET_ADHOC" }, "flash0:/kd/memab.prx"),
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
        sceDdr(Modules.sceDdrModule),
        sceMScm(Modules.sceMScmModule),
        sceG729(Modules.sceG729Module, new String[] { "PSP_MODULE_AV_G729", "g729" }, "flash0:/kd/g729.prx"),
        scePopsMan(Modules.scePopsManModule),
        scePaf(Modules.scePafModule),
        sceClockgen(Modules.sceClockgenModule),
        sceCodec(Modules.sceCodecModule),
        sceMesgd(Modules.sceMesgdModule),
        sceVshBridge(Modules.sceVshBridgeModule),
        SystemCtrlForKernel(Modules.SystemCtrlForKernelModule),
        sceHibari(Modules.sceHibariModule),
        sceSystimer(Modules.sceSystimerModule),
        sceUsbBus(Modules.sceUsbBusModule),
        sceUsbHost(Modules.sceUsbHostModule),
        sceEFlash(Modules.sceEFlashModule),
        sceVshCommonUtil(Modules.sceVshCommonUtilModule),
        sceLFatFs(Modules.sceLFatFsModule),
        pspvmc(Modules.pspvmcModule),
        sceAmctrl(Modules.sceAmctrlModule),
        sceNwman(Modules.sceNwmanModule),
        sceUsbPspcm(Modules.sceUsbPspcmModule, new String[] { "usbpspcm", "PSP_MODULE_USB_PSPCM", "PSP_USB_MODULE_PSPCM", "sceUSB_PSPComm_Driver" }, "flash0:/kd/usbpspcm.prx");

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

	public void read(StateInputStream stream) throws IOException {
    	stream.readVersion(STATE_VERSION);
    	for (ModuleInfo moduleInfo : ModuleInfo.values()) {
        	HLEModule hleModule = moduleInfo.getModule();
        	hleModule.read(stream);
        }
    }

    public void write(StateOutputStream stream) throws IOException {
    	stream.writeVersion(STATE_VERSION);
    	for (ModuleInfo moduleInfo : ModuleInfo.values()) {
        	HLEModule hleModule = moduleInfo.getModule();
        	hleModule.write(stream);
        }
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

    public int[] LoadFlash0Module(String name) {
    	int result = 0;
    	int[] moduleIds = null;
    	if (name != null) {
	        List<HLEModule> modules = flash0prxMap.get(name.toLowerCase());
	        if (modules != null) {
	            for (HLEModule module : modules) {
	            	ModuleInfo moduleInfo = moduleInfos.get(module);
	            	if (moduleInfo != null) {
	            		String prxFileName = moduleInfo.getPrxFileName();
	            		if (prxFileName != null) {
	            			if (log.isDebugEnabled()) {
	            				log.debug(String.format("Loading module '%s' for '%s'", prxFileName, name));
	            			}
	            			result = Modules.ModuleMgrForUserModule.hleKernelLoadAndStartModule(prxFileName, 0x10);
	            			if (result < 0) {
	            				log.error(String.format("Could not load '%s': 0x%08X", prxFileName, result));
	            			} else {
	            				moduleIds = Utilities.add(moduleIds, result);
	            			}
	            		}
	            	}
	            }
	        }
    	}

    	return moduleIds;
    }

    /** @return the UID assigned to the module or negative on error
     * TODO need to figure out how the uids work when 1 prx contains several modules. */
    public int LoadFlash0Module(String name, int moduleVersion, int moduleElfVersion, int moduleMemoryPartition, int moduleMemoryType) {
    	if (name != null) {
	        List<HLEModule> modules = flash0prxMap.get(name.toLowerCase());
	        if (modules != null) {
	            for (HLEModule module : modules) {
	            	module.setModuleVersion(moduleVersion);
	            	module.setModuleElfVersion(moduleElfVersion);
	            	module.setModuleMemoryPartition(moduleMemoryPartition);
	            	module.setModuleMemoryType(moduleMemoryType);
	            	installModuleWithAnnotations(module);
	            }
	        }
    	}

        SceModule fakeModule = new SceModule(true);
        fakeModule.modname = name;
        fakeModule.moduleVersion = moduleVersion;
        fakeModule.version = moduleElfVersion;
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

    		if (func.requiresJumpCall()) {
    			int jumpCallAddress = HLEUtilities.getInstance().allocateInternalMemory(8);
                Memory mem = Emulator.getMemory(jumpCallAddress);
                mem.write32(jumpCallAddress + 0, JR());
                mem.write32(jumpCallAddress + 4, SYSCALL(syscallCode));
    			nidMapper.setNidAddress(func.getModuleName(), nid, jumpCallAddress);
    		}
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
        	if (Memory.isAddressGood(address)) {
            	Memory mem = Memory.getInstance();
	        	if ((mem.internalRead32(address) >>> 26) == AllegrexOpcodes.J) {
	        		if (mem.internalRead32(address + 4) == NOP()) {
	        			int jumpAddress = (mem.internalRead32(address) & 0x03FFFFFF) << 2;

	        			nid = nidMapper.getNidByAddress(jumpAddress);
	        		}
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

    public int getNIDFromFunctionName(String functionName) {
    	for (HLEModuleFunction function : nidToFunction.values()) {
    		if (functionName.equals(function.getFunctionName())) {
    			return function.getNid();
    		}
    	}

    	return 0;
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

		HLEModuleFunction hleModuleFunction = new HLEModuleFunction(moduleName, functionName, hleFunction.nid(), hleModule, method, hleFunction.checkInsideInterrupt(), hleFunction.checkDispatchThreadEnabled(), hleFunction.stackUsage(), hleFunction.version(), hleFunction.jumpCall(), hleFunction.canModifyCode());

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
				HLEFunction[] hleFunctions = method.getAnnotationsByType(HLEFunction.class);
				for (HLEFunction hleFunction : hleFunctions) {
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
		for (int i = 0; i < moduleFileNamesVshOnly.length; i++) {
			if (moduleFileNamesVshOnly[i].equalsIgnoreCase(moduleFileName)) {
				return true;
			}
		}

		return false;
	}

	public void loadAvailableFlash0Modules(boolean fromSyscall) {
		boolean runningFromVsh = Emulator.getMainGUI().isRunningFromVsh() && !fromSyscall;
    	boolean requiresLLE = false;

		List<String> availableModuleFileNames = new LinkedList<>();
		for (String moduleFileNameTemplate : moduleFileNamesToBeLoaded) {
			// Replace "01g" with "0Ng" where N is the PSP model generation
			String moduleFileName = moduleFileNameTemplate;
			if (moduleFileName.contains("01g")) {
				moduleFileName = moduleFileName.replace("01g", String.format("%02dg", Model.getGeneration()));
			}

			if (runningFromVsh || !isModuleFileNameVshOnly(moduleFileName)) {
				StringBuilder localFileName = new StringBuilder();
				IVirtualFileSystem vfs = Modules.IoFileMgrForUserModule.getVirtualFileSystem(moduleFileName, localFileName);
				if (vfs != null && vfs.ioGetstat(localFileName.toString(), new SceIoStat()) == 0) {
					// The module is available, load it
					availableModuleFileNames.add(moduleFileName);

		        	// Is the module requiring LLE?
		        	for (String moduleFileNameLLE : moduleFileNamesLLE) {
		        		if (moduleFileNameTemplate.equals(moduleFileNameLLE)) {
		        			requiresLLE = true;
		        			break;
		        		}
		        	}

				}
			}
		}

		if (availableModuleFileNames.isEmpty()) {
			// No module available, do nothing
			return;
		}

		// This HLE module needs to be started in order
		// to be able to load and start the available modules.
		Modules.ModuleMgrForUserModule.start();

		// Enable the LLE if not yet done
    	if (requiresLLE && !RuntimeContextLLE.isLLEActive()) {
			RuntimeContextLLE.enableLLE();
			RuntimeContextLLE.start();
    	}

    	int startPriority = 0x10;
    	for (String moduleFileName : availableModuleFileNames) {
        	if (log.isInfoEnabled()) {
        		log.info(String.format("Loading and starting the module '%s', it will replace the equivalent HLE functions", moduleFileName));
        	}

        	IAction onModuleStartAction = null;
        	if ("flash0:/kd/loadcore.prx".equals(moduleFileName)) {
            	// loadcore.prx requires start parameters
        		onModuleStartAction = Modules.LoadCoreForKernelModule.getModuleStartAction();
        	}

        	Modules.ModuleMgrForUserModule.hleKernelLoadAndStartModule(moduleFileName, startPriority++, onModuleStartAction);
    	}
	}

	private void loadPlugins(String sepluginsFileName) {
		byte[] buffer = Utilities.readCompleteFile(sepluginsFileName);
		if (buffer == null) {
			return;
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("Loading Plug-Ins from '%s'", sepluginsFileName));
		}

		boolean firstPlugIn = true;
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buffer)));
			while (true) {
				String line = reader.readLine();
				if (line == null) {
					break;
				}
				line = line.trim();

				// Ignore lines starting with #
				if (line.startsWith("#")) {
					continue;
				}

				String[] parts = line.split("[ \\t]");
				if (parts != null && parts.length >= 2 && "1".equals(parts[1])) {
					if (firstPlugIn) {
						installFakeKernelCode();
						firstPlugIn = false;
					}

					String pluginFileName = parts[0];
					if (log.isInfoEnabled()) {
						log.info(String.format("Loading and starting the Plug-In '%s'", pluginFileName));
					}
			    	Modules.ModuleMgrForUserModule.hleKernelLoadAndStartModule(pluginFileName, 0x10, null);
				}
			}
			reader.close();
		} catch (FileNotFoundException e) {
			log.error("loadPlugins", e);
		} catch (IOException e) {
			log.error("loadPlugins", e);
		}
	}

	public void loadPlugins() {
		loadPlugins("ms0:/seplugins/game.txt");
	}

	private void installFakeKernelCode() {
		// The homebrew plug-in CheatMaster is patching the following kernel functions:
		installFakeKernelCode(getFunctionFromNID(0x9E3C6DC6)); // sceDisplaySetBrightness
		installFakeKernelCode(getFunctionFromNID(0x3A622550)); // sceCtrlPeekBufferPositive
		installFakeKernelCode(getFunctionFromNID(0xC152080A)); // sceCtrlPeekBufferNegative
		installFakeKernelCode(getFunctionFromNID(0x1F803938)); // sceCtrlReadBufferPositive
		installFakeKernelCode(getFunctionFromNID(0x60B81F86)); // sceCtrlReadBufferNegative
	}

	private void installFakeKernelCode(HLEModuleFunction hleFunction) {
		if (hleFunction == null) {
			return;
		}

		int addr = installFakeKernelCodeForSyscall(hleFunction.getSyscallCode(), hleFunction.getFunctionName());
		if (addr == 0) {
			return;
		}

		nidMapper.addFakeSycall(hleFunction.getModuleName(), hleFunction.getNid(), addr);

		if (log.isDebugEnabled()) {
			log.debug(String.format("Installed fake kernel code at 0x%08X for %s", addr, hleFunction));
		}
	}

	private int installFakeKernelCodeForSyscall(int syscallCode, String functionName) {
		SysMemInfo sysMemInfo = Modules.SysMemUserForUserModule.malloc(KERNEL_PARTITION_ID, String.format("Fake kernel code for syscall 0x%05X(%s)", syscallCode, functionName), PSP_SMEM_Low, defaultSizeAlignment, 0);
		if (sysMemInfo == null) {
			return 0;
		}

		int addr = sysMemInfo.addr;
		IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(addr, defaultSizeAlignment, 4);

		if (functionName.startsWith("sceCtrlReadBuffer") || functionName.startsWith("sceCtrlPeekBuffer")) {
			// The homebrew plug-in CheatMaster is patching these functions by searching for a "jal" instruction inside them
			int port = 0;
			int mode = (functionName.contains("Peek") ? 0 : 2) | (functionName.contains("Positive") ? 0 : 1);
	    	memoryWriter.writeNext(ADDIU(_sp, _sp, -16));
	    	memoryWriter.writeNext(SW   (_ra, _sp, 0));
			memoryWriter.writeNext(LI   (_a2, port));
			memoryWriter.writeNext(JAL  (addr + 8 * 4));
			memoryWriter.writeNext(LI   (_a3, mode));
	    	memoryWriter.writeNext(LW   (_ra, _sp, 0));
			memoryWriter.writeNext(JR   ());
	    	memoryWriter.writeNext(ADDIU(_sp, _sp, 16));

	    	// This is a HLE syscall supporting the different sceCtrlReadBuffer variants
	    	syscallCode = Modules.sceCtrlModule.getHleFunctionByName("_sceCtrlReadBuf").getSyscallCode();
		} else if ("sceDisplaySetBrightness".equals(functionName)) {
			// The homebrew plug-in CheatMaster is patching this function by searching for a "jal" instruction inside it
	    	memoryWriter.writeNext(ADDIU(_sp, _sp, -16));
	    	memoryWriter.writeNext(SW   (_ra, _sp, 0));
			memoryWriter.writeNext(JAL  (addr + 7 * 4));
			memoryWriter.writeNext(NOP  ());
	    	memoryWriter.writeNext(LW   (_ra, _sp, 0));
			memoryWriter.writeNext(JR   ());
	    	memoryWriter.writeNext(ADDIU(_sp, _sp, 16));
		}

		memoryWriter.writeNext(JR     ());
		memoryWriter.writeNext(SYSCALL(syscallCode));
		memoryWriter.flush();

		return addr;
	}
}