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

    public static Logger log = Logger.getLogger("hle");

    public void step() {
        //jpcsp.HLE.pspge.get_instance().step();
        //jpcsp.HLE.ThreadMan.get_instance().step();
        //jpcsp.HLE.pspdisplay.get_instance().step();
        //HLEModuleManager.getInstance().step();
    }

    public void load(ByteBuffer buffer) {
    }

    public void save(ByteBuffer buffer) {
    }
};
