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

import java.nio.ByteBuffer;

import jpcsp.HLE.modules.InterruptManager;
import jpcsp.HLE.modules.IoFileMgrForUser;
import jpcsp.HLE.modules.Kernel_Library;
import jpcsp.HLE.modules.LoadCoreForKernel;
import jpcsp.HLE.modules.LoadExecForUser;
import jpcsp.HLE.modules.ModuleMgrForUser;
import jpcsp.HLE.modules.StdioForUser;
import jpcsp.HLE.modules.SysMemUserForUser;
import jpcsp.HLE.modules.SysMemForKernel;
import jpcsp.HLE.modules.ThreadManForUser;
import jpcsp.HLE.modules.UtilsForUser;
import jpcsp.HLE.modules.sceAtrac3plus;
import jpcsp.HLE.modules.sceAudio;
import jpcsp.HLE.modules.sceCtrl;
import jpcsp.HLE.modules.sceDeflt;
import jpcsp.HLE.modules.sceDisplay;
import jpcsp.HLE.modules.sceDmac;
import jpcsp.HLE.modules.sceFont;
import jpcsp.HLE.modules.sceGe_user;
import jpcsp.HLE.modules.sceHprm;
import jpcsp.HLE.modules.sceImpose;
import jpcsp.HLE.modules.sceMp3;
import jpcsp.HLE.modules.sceMpeg;
import jpcsp.HLE.modules.scePower;
import jpcsp.HLE.modules.scePsmf;
import jpcsp.HLE.modules.scePsmfPlayer;
import jpcsp.HLE.modules.sceRtc;
import jpcsp.HLE.modules.sceSasCore;
import jpcsp.HLE.modules.sceSuspendForUser;
import jpcsp.HLE.modules.sceUmdUser;
import jpcsp.HLE.modules.sceUtility;

import org.apache.log4j.Logger;

public class Modules {

    public static IoFileMgrForUser IoFileMgrForUserModule = new IoFileMgrForUser();
    public static ThreadManForUser ThreadManForUserModule = new ThreadManForUser();
    public static SysMemUserForUser SysMemUserForUserModule = new SysMemUserForUser();
    public static SysMemForKernel SysMemForKernelModule = new SysMemForKernel();
    public static InterruptManager InterruptManagerModule = new InterruptManager();
    public static LoadExecForUser LoadExecForUserModule = new LoadExecForUser();
    public static StdioForUser StdioForUserModule = new StdioForUser();
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

    public static Logger log = Logger.getLogger("hle");

    public void step() {
    }

    public void load(ByteBuffer buffer) {
    }

    public void save(ByteBuffer buffer) {
    }
}