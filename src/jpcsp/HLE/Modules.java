/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jpcsp.HLE;

import jpcsp.HLE.modules.*;

import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

/**
 *
 * @author hli
 */
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
    public static sceAtrac3plus sceAttrac3plusModule = new sceAtrac3plus();
    public static sceAudio sceAudio = new sceAudio();
    public static sceImpose sceImpose = new sceImpose();

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
};
