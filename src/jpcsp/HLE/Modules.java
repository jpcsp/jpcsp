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

import jpcsp.HLE.modules.*;

import org.apache.log4j.Logger;

public class Modules {
	// The modules must be named using the following convention:
	//    <module class name>Module
	// This is required by the compiler (see CompilerContext.loadModule()).
    public static IoFileMgrForUser IoFileMgrForUserModule = new IoFileMgrForUser();
    public static IoFileMgrForKernel IoFileMgrForKernelModule = new IoFileMgrForKernel();
    public static ThreadManForUser ThreadManForUserModule = new ThreadManForUser();
    public static ThreadManForKernel ThreadManForKernelModule = new ThreadManForKernel();
    public static SysMemUserForUser SysMemUserForUserModule = new SysMemUserForUser();
    public static SysMemForKernel SysMemForKernelModule = new SysMemForKernel();
    public static InterruptManager InterruptManagerModule = new InterruptManager();
    public static LoadExecForUser LoadExecForUserModule = new LoadExecForUser();
    public static LoadExecForKernel LoadExecForKernelModule = new LoadExecForKernel();
    public static StdioForUser StdioForUserModule = new StdioForUser();
    public static StdioForKernel StdioForKernelModule = new StdioForKernel();
    public static sceCtrl sceCtrlModule = new sceCtrl();
    public static sceDisplay sceDisplayModule = new sceDisplay();
    public static sceGe_user sceGe_userModule = new sceGe_user();
    public static scePower scePowerModule = new scePower();
    public static sceUmdUser sceUmdUserModule = new sceUmdUser();
    public static sceUtility sceUtilityModule = new sceUtility();
    public static UtilsForUser UtilsForUserModule = new UtilsForUser();
    public static sceRtc sceRtcModule = new sceRtc();
    public static Kernel_Library Kernel_LibraryModule = new Kernel_Library();
    public static ModuleMgrForUser ModuleMgrForUserModule = new ModuleMgrForUser();
    public static sceMpeg sceMpegModule = new sceMpeg();
    public static sceMpegbase sceMpegbaseModule = new sceMpegbase();
    public static LoadCoreForKernel LoadCoreForKernelModule = new LoadCoreForKernel();
    public static sceAtrac3plus sceAtrac3plusModule = new sceAtrac3plus();
    public static sceAudio sceAudioModule = new sceAudio();
    public static sceImpose sceImposeModule = new sceImpose();
    public static sceSuspendForUser sceSuspendForUserModule = new sceSuspendForUser();
    public static sceSuspendForKernel sceSuspendForKernelModule = new sceSuspendForKernel();
    public static sceDmac sceDmacModule = new sceDmac();
    public static sceSasCore sceSasCoreModule = new sceSasCore();
    public static sceHprm sceHprmModule = new sceHprm();
    public static sceFont sceFontModule = new sceFont();
    public static scePsmfPlayer scePsmfPlayerModule = new scePsmfPlayer();
    public static scePsmf scePsmfModule = new scePsmf();
    public static sceMp3 sceMp3Module = new sceMp3();
    public static sceDeflt sceDefltModule = new sceDeflt();
    public static sceWlan sceWlanModule = new sceWlan();
    public static sceNet sceNetModule = new sceNet();
    public static sceNetAdhoc sceNetAdhocModule = new sceNetAdhoc();
    public static sceNetAdhocctl sceNetAdhocctlModule = new sceNetAdhocctl();
    public static sceNetAdhocDiscover sceNetAdhocDiscoverModule = new sceNetAdhocDiscover();
    public static sceNetAdhocMatching sceNetAdhocMatchingModule = new sceNetAdhocMatching();
    public static sceNetIfhandle sceNetIfhandleModule = new sceNetIfhandle();
    public static sceNetInet sceNetInetModule = new sceNetInet();
    public static sceNetApctl sceNetApctlModule = new sceNetApctl();
    public static sceNetResolver sceNetResolverModule = new sceNetResolver();
    public static sceOpenPSID sceOpenPSIDModule = new sceOpenPSID();
    public static sceNp sceNpModule = new sceNp();
    public static sceNpCore sceNpCoreModule = new sceNpCore();
    public static sceNpAuth sceNpAuthModule = new sceNpAuth();
    public static sceNpService sceNpServiceModule = new sceNpService();
    public static sceNpCommerce2 sceNpCommerce2Module = new sceNpCommerce2();
    public static sceNpCommerce2Store sceNpCommerce2StoreModule = new sceNpCommerce2Store();
    public static sceNpCommerce2RegCam sceNpCommerce2RegCamModule = new sceNpCommerce2RegCam();
    public static sceNpMatching2 sceNpMatching2Module = new sceNpMatching2();
    public static scePspNpDrm_user scePspNpDrm_userModule = new scePspNpDrm_user();
    public static sceVaudio sceVaudioModule = new sceVaudio();
    public static sceMp4 sceMp4Module = new sceMp4();
    public static sceHttp sceHttpModule = new sceHttp();
    public static sceHttps sceHttpsModule = new sceHttps();
    public static sceSsl sceSslModule = new sceSsl();
    public static sceP3da sceP3daModule = new sceP3da();
    public static sceGameUpdate sceGameUpdateModule = new sceGameUpdate();
    public static sceUsbCam sceUsbCamModule = new sceUsbCam();
    public static sceJpeg sceJpegModule = new sceJpeg();
    public static sceUsb sceUsbModule = new sceUsb();
    public static sceHeap sceHeapModule = new sceHeap();
    public static KDebugForKernel KDebugForKernelModule = new KDebugForKernel();
    public static sceCcc sceCccModule = new sceCcc();
    public static scePauth scePauthModule = new scePauth();
    public static sceSfmt19937 sceSfmt19937Module = new sceSfmt19937();
    public static sceMd5 sceMd5Module = new sceMd5();
    public static sceParseUri sceParseUriModule = new sceParseUri();
    public static sceUsbAcc sceUsbAccModule = new sceUsbAcc();
    public static sceMt19937 sceMt19937Module = new sceMt19937();
    public static sceAac sceAacModule = new sceAac();
    public static sceFpu sceFpuModule = new sceFpu();
    public static sceUsbMic sceUsbMicModule = new sceUsbMic();
    public static sceAudioRouting sceAudioRoutingModule = new sceAudioRouting();
    public static sceUsbGps sceUsbGpsModule = new sceUsbGps();
    public static sceAudiocodec sceAudiocodecModule = new sceAudiocodec();
    public static sceAdler sceAdlerModule = new sceAdler();
    public static sceSha1 sceSha1Module = new sceSha1();
    public static sceSha256 sceSha256Module = new sceSha256();
    public static sceMeCore sceMeCoreModule = new sceMeCore();
    public static KUBridge KUBridgeModule = new KUBridge();
    public static SysclibForKernel SysclibForKernelModule = new SysclibForKernel();
    public static semaphore semaphoreModule = new semaphore();
    public static ModuleMgrForKernel ModuleMgrForKernelModule = new ModuleMgrForKernel();
    public static sceReg sceRegModule = new sceReg();
    public static sceDve sceDveModule = new sceDve();
    public static sceNetUpnp sceNetUpnpModule = new sceNetUpnp();
    public static sceSysEventForKernel sceSysEventForKernelModule = new sceSysEventForKernel();
    public static sceChkreg sceChkregModule = new sceChkreg();
    public static sceMsAudio_Service sceMsAudio_ServiceModule = new sceMsAudio_Service();
    public static sceMePower sceMePowerModule = new sceMePower();
    public static sceResmgr sceResmgrModule = new sceResmgr();
    public static UtilsForKernel UtilsForKernelModule = new UtilsForKernel();
    public static sceLibUpdateDL sceLibUpdateDLModule = new sceLibUpdateDL();
    public static sceParseHttp sceParseHttpModule = new sceParseHttp();
    public static sceMgr_driver sceMgr_driverModule = new sceMgr_driver();
    public static sceChnnlsv sceChnnlsvModule = new sceChnnlsv();
    public static sceNetAdhocTransInt sceNetAdhocTransIntModule = new sceNetAdhocTransInt();
    public static sceUsbstor sceUsbstorModule = new sceUsbstor();
    public static sceIdStorage sceIdStorageModule = new sceIdStorage();
    public static sceCertLoader sceCertLoaderModule = new sceCertLoader();
    public static sceDNAS sceDNASModule = new sceDNAS();
    public static sceDNASCore sceDNASCoreModule = new sceDNASCore();
    public static sceMcctrl sceMcctrlModule = new sceMcctrl();
    public static sceNpInstall sceNpInstallModule = new sceNpInstall();
    public static sceNpCamp sceNpCampModule = new sceNpCamp();
    public static sceNetAdhocAuth sceNetAdhocAuthModule = new sceNetAdhocAuth();
    public static sceNetAdhocDownload sceNetAdhocDownloadModule = new sceNetAdhocDownload();
    public static sceHttpStorage sceHttpStorageModule = new sceHttpStorage();
    public static sceVideocodec sceVideocodecModule = new sceVideocodec();
    public static sceNetStun sceNetStunModule = new sceNetStun();
    public static sceMeMemory sceMeMemoryModule = new sceMeMemory();
    public static sceMeBoot sceMeBootModule = new sceMeBoot();
    public static sceMeVideo sceMeVideoModule = new sceMeVideo();
    public static sceMeAudio sceMeAudioModule = new sceMeAudio();
    public static InitForKernel InitForKernelModule = new InitForKernel();
    public static sceMemab sceMemabModule = new sceMemab();
    public static DmacManForKernel DmacManForKernelModule = new DmacManForKernel();
    public static sceSyscon sceSysconModule = new sceSyscon();
    public static sceLed sceLedModule = new sceLed();
    public static sceSysreg sceSysregModule = new sceSysreg();
    public static scePsheet scePsheetModule = new scePsheet();
    public static sceUmdMan sceUmdManModule = new sceUmdMan();
    public static sceCodepage sceCodepageModule = new sceCodepage();
    public static sceMSstor sceMSstorModule = new sceMSstor();
    public static sceAta sceAtaModule = new sceAta();
    public static sceGpio sceGpioModule = new sceGpio();
    public static sceNand sceNandModule = new sceNand();
    public static sceBSMan sceBSManModule = new sceBSMan();
    public static mp4msv mp4msvModule = new mp4msv();
    public static memlmd memlmdModule = new memlmd();
    public static reboot rebootModule = new reboot();
    public static sceI2c sceI2cModule = new sceI2c();
    public static scePwm scePwmModule = new scePwm();
    public static sceLcdc sceLcdcModule = new sceLcdc();
    public static sceDmacplus sceDmacplusModule = new sceDmacplus();
    public static sceDdr sceDdrModule = new sceDdr();
    public static sceMScm sceMScmModule = new sceMScm();
    public static sceG729 sceG729Module = new sceG729();
    public static scePopsMan scePopsManModule = new scePopsMan();
    public static scePaf scePafModule = new scePaf();
    public static sceClockgen sceClockgenModule = new sceClockgen();
    public static sceCodec sceCodecModule = new sceCodec();
    public static sceMesgd sceMesgdModule = new sceMesgd();
    public static sceVshBridge sceVshBridgeModule = new sceVshBridge();
    public static SystemCtrlForKernel SystemCtrlForKernelModule = new SystemCtrlForKernel();
    public static sceHibari sceHibariModule = new sceHibari();
    public static sceSystimer sceSystimerModule = new sceSystimer();
    public static sceUsbBus sceUsbBusModule = new sceUsbBus();
    public static sceUsbHost sceUsbHostModule = new sceUsbHost();
    public static sceEFlash sceEFlashModule = new sceEFlash();
    public static sceVshCommonUtil sceVshCommonUtilModule = new sceVshCommonUtil();
    public static sceLFatFs sceLFatFsModule = new sceLFatFs();
    public static pspvmc pspvmcModule = new pspvmc();
    public static sceAmctrl sceAmctrlModule = new sceAmctrl();
    public static sceNwman sceNwmanModule = new sceNwman();
    public static sceUsbPspcm sceUsbPspcmModule = new sceUsbPspcm();
    public static sceAvcodec sceAvcodecModule = new sceAvcodec();

    public static Logger log = Logger.getLogger("hle");

    public static Logger getLogger(String module) {
        return Logger.getLogger("hle." + module);
    }
}