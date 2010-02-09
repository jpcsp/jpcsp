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

import org.apache.log4j.Logger;

import jpcsp.HLE.modules.*;

public class Modules {

    public static Sample SampleModule = new Sample();

    public static ThreadManForUser ThreadManForUserModule = new ThreadManForUser();
    public static StdioForUser StdioForUserModule = new StdioForUser();
    public static sceCtrl sceCtrlModule = new sceCtrl();
    public static sceDisplay sceDisplayModule = new sceDisplay();
    public static sceGe_user sceGE_userModule = new sceGe_user();
    public static scePower scePowerModule = new scePower();
    public static sceUmdUser sceUmdUserModule = new sceUmdUser();
    public static sceUtility sceUtilityModule = new sceUtility();
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
    public static TimerManager TimerManager = new TimerManager();


    public static Logger log = Logger.getLogger("hle");

    public void step() {
        // These three to be phased out:
        //jpcsp.HLE.pspge.getInstance().step();
        //jpcsp.HLE.ThreadMan.getInstance().step();
        //jpcsp.HLE.pspdisplay.getInstance().step();

        // This is the new design, and it can co-exist with the old design
        //HLEModuleManager.getInstance().step();
    }

    public void load(ByteBuffer buffer) {
    }

    public void save(ByteBuffer buffer) {
    }
}
