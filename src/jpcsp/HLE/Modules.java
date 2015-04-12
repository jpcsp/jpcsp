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

import jpcsp.HLE.modules.InterruptManager;
import jpcsp.HLE.modules.IoFileMgrForUser;
import jpcsp.HLE.modules.KDebugForKernel;
import jpcsp.HLE.modules.Kernel_Library;
import jpcsp.HLE.modules.LoadCoreForKernel;
import jpcsp.HLE.modules.LoadExecForUser;
import jpcsp.HLE.modules.ModuleMgrForUser;
import jpcsp.HLE.modules.StdioForKernel;
import jpcsp.HLE.modules.StdioForUser;
import jpcsp.HLE.modules.SysMemUserForUser;
import jpcsp.HLE.modules.SysMemForKernel;
import jpcsp.HLE.modules.ThreadManForUser;
import jpcsp.HLE.modules.UtilsForUser;
import jpcsp.HLE.modules.sceAac;
import jpcsp.HLE.modules.sceAdler;
import jpcsp.HLE.modules.sceAtrac3plus;
import jpcsp.HLE.modules.sceAudio;
import jpcsp.HLE.modules.sceAudioRouting;
import jpcsp.HLE.modules.sceAudiocodec;
import jpcsp.HLE.modules.sceCcc;
import jpcsp.HLE.modules.sceCtrl;
import jpcsp.HLE.modules.sceDeflt;
import jpcsp.HLE.modules.sceDisplay;
import jpcsp.HLE.modules.sceDmac;
import jpcsp.HLE.modules.sceFont;
import jpcsp.HLE.modules.sceFpu;
import jpcsp.HLE.modules.sceGameUpdate;
import jpcsp.HLE.modules.sceGe_user;
import jpcsp.HLE.modules.sceHeap;
import jpcsp.HLE.modules.sceHprm;
import jpcsp.HLE.modules.sceHttp;
import jpcsp.HLE.modules.sceHttps;
import jpcsp.HLE.modules.sceImpose;
import jpcsp.HLE.modules.sceJpeg;
import jpcsp.HLE.modules.sceMd5;
import jpcsp.HLE.modules.sceMp3;
import jpcsp.HLE.modules.sceMp4;
import jpcsp.HLE.modules.sceMpeg;
import jpcsp.HLE.modules.sceMt19937;
import jpcsp.HLE.modules.sceNet;
import jpcsp.HLE.modules.sceNetAdhoc;
import jpcsp.HLE.modules.sceNetAdhocctl;
import jpcsp.HLE.modules.sceNetAdhocDiscover;
import jpcsp.HLE.modules.sceNetAdhocMatching;
import jpcsp.HLE.modules.sceNetApctl;
import jpcsp.HLE.modules.sceNetIfhandle;
import jpcsp.HLE.modules.sceNetInet;
import jpcsp.HLE.modules.sceNetResolver;
import jpcsp.HLE.modules.sceNp;
import jpcsp.HLE.modules.sceNpAuth;
import jpcsp.HLE.modules.sceNpService;
import jpcsp.HLE.modules.sceOpenPSID;
import jpcsp.HLE.modules.sceP3da;
import jpcsp.HLE.modules.scePauth;
import jpcsp.HLE.modules.scePower;
import jpcsp.HLE.modules.scePsmf;
import jpcsp.HLE.modules.scePsmfPlayer;
import jpcsp.HLE.modules.scePspNpDrm_user;
import jpcsp.HLE.modules.sceRtc;
import jpcsp.HLE.modules.sceSasCore;
import jpcsp.HLE.modules.sceSfmt19937;
import jpcsp.HLE.modules.sceSha1;
import jpcsp.HLE.modules.sceSha256;
import jpcsp.HLE.modules.sceSsl;
import jpcsp.HLE.modules.SystemCtrlForKernel;
import jpcsp.HLE.modules.sceSuspendForUser;
import jpcsp.HLE.modules.sceUmdUser;
import jpcsp.HLE.modules.sceParseUri;
import jpcsp.HLE.modules.sceUsb;
import jpcsp.HLE.modules.sceUsbAcc;
import jpcsp.HLE.modules.sceUsbCam;
import jpcsp.HLE.modules.sceUsbGps;
import jpcsp.HLE.modules.sceUsbMic;
import jpcsp.HLE.modules.sceUtility;
import jpcsp.HLE.modules.sceVaudio;
import jpcsp.HLE.modules.sceWlan;
import jpcsp.HLE.modules.SystemCtrlForKernel;
import jpcsp.HLE.modules.ModuleMgrForKernel;

import org.apache.log4j.Logger;
import org.lwjgl.LWJGLException;

public class Modules {
	// The modules must be named using the following convention:
	//    <module class name>Module
	// This is required by the compiler (see CompilerContext.loadModule()).
    public static IoFileMgrForUser IoFileMgrForUserModule = new IoFileMgrForUser();
    public static ThreadManForUser ThreadManForUserModule = new ThreadManForUser();
    public static SysMemUserForUser SysMemUserForUserModule = new SysMemUserForUser();
    public static SysMemForKernel SysMemForKernelModule = new SysMemForKernel();
    public static InterruptManager InterruptManagerModule = new InterruptManager();
    public static LoadExecForUser LoadExecForUserModule = new LoadExecForUser();
    public static StdioForUser StdioForUserModule = new StdioForUser();
    public static StdioForKernel StdioForKernelModule = new StdioForKernel();
    public static sceCtrl sceCtrlModule = new sceCtrl();
    public static sceDisplay sceDisplayModule;
    public static sceGe_user sceGe_userModule = new sceGe_user();
    public static scePower scePowerModule = new scePower();
    public static sceUmdUser sceUmdUserModule = new sceUmdUser();
    public static sceUtility sceUtilityModule = new sceUtility();
    public static UtilsForUser UtilsForUserModule = new UtilsForUser();
    public static sceRtc sceRtcModule = new sceRtc();
    public static Kernel_Library Kernel_LibraryModule = new Kernel_Library();
    public static ModuleMgrForUser ModuleMgrForUserModule = new ModuleMgrForUser();
    public static sceMpeg sceMpegModule = new sceMpeg();
    public static LoadCoreForKernel LoadCoreForKernelModule = new LoadCoreForKernel();
    public static sceAtrac3plus sceAtrac3plusModule = new sceAtrac3plus();
    public static sceAudio sceAudioModule = new sceAudio();
    public static sceImpose sceImposeModule = new sceImpose();
    public static sceSuspendForUser sceSuspendForUserModule = new sceSuspendForUser();
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
    public static sceNpAuth sceNpAuthModule = new sceNpAuth();
    public static sceNpService sceNpServiceModule = new sceNpService();
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
	public static SystemCtrlForKernel SystemCtrlForKernelModule = new SystemCtrlForKernel();
	public static ModuleMgrForKernel ModuleMgrForKernelModule = new ModuleMgrForKernel();

    public static Logger log = Logger.getLogger("hle");

    public static Logger getLogger(String module) {
        return Logger.getLogger("hle." + module);
    }

    static {
    	try {
			sceDisplayModule = new sceDisplay();
		} catch (LWJGLException e) {
			log.error("Error while creating sceDisplay", e);
		}
    }
}