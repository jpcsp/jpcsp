/* This autogenerated file is part of jpcsp. */
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
package jpcsp.HLE.modules150;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.Modules;

import jpcsp.Memory;
import jpcsp.Processor;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_WAITING;
import static jpcsp.util.Utilities.*;

import jpcsp.Allegrex.CpuState; // New-Style Processor
import jpcsp.HLE.ThreadMan;
import jpcsp.filesystems.umdiso.UmdIsoReader;

import jpcsp.HLE.kernel.types.*;
import static jpcsp.HLE.kernel.types.SceKernelErrors.*;

public class sceUmdUser implements HLEModule {
    @Override
    public String getName() {
        return "sceUmdUser";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.addFunction(sceUmdCheckMediumFunction, 0x46EBB729);
            mm.addFunction(sceUmdActivateFunction, 0xC6183D47);
            mm.addFunction(sceUmdDeactivateFunction, 0xE83742BA);
            mm.addFunction(sceUmdWaitDriveStatFunction, 0x8EF08FCE);
            mm.addFunction(sceUmdWaitDriveStatWithTimerFunction, 0x56202973);
            mm.addFunction(sceUmdWaitDriveStatCBFunction, 0x4A9E5E29);
            mm.addFunction(sceUmdCancelWaitDriveStatFunction, 0x6AF9B50A);
            mm.addFunction(sceUmdGetDriveStatFunction, 0x6B4A146C);
            mm.addFunction(sceUmdGetErrorStatFunction, 0x20628E6F);
            mm.addFunction(sceUmdGetDiscInfoFunction, 0x340B7686);
            mm.addFunction(sceUmdRegisterUMDCallBackFunction, 0xAEE7404D);
            mm.addFunction(sceUmdUnRegisterUMDCallBackFunction, 0xBD2BDE07);

        }

        umdActivated = false;
        umdDeactivateCalled = false;
        waitingThreads = new LinkedList<SceKernelThreadInfo>();
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.removeFunction(sceUmdCheckMediumFunction);
            mm.removeFunction(sceUmdActivateFunction);
            mm.removeFunction(sceUmdDeactivateFunction);
            mm.removeFunction(sceUmdWaitDriveStatFunction);
            mm.removeFunction(sceUmdWaitDriveStatWithTimerFunction);
            mm.removeFunction(sceUmdWaitDriveStatCBFunction);
            mm.removeFunction(sceUmdCancelWaitDriveStatFunction);
            mm.removeFunction(sceUmdGetDriveStatFunction);
            mm.removeFunction(sceUmdGetErrorStatFunction);
            mm.removeFunction(sceUmdGetDiscInfoFunction);
            mm.removeFunction(sceUmdRegisterUMDCallBackFunction);
            mm.removeFunction(sceUmdUnRegisterUMDCallBackFunction);

        }
    }

    // HLE helper state

    protected final int PSP_UMD_NOT_PRESENT = 0x01;
    protected final int PSP_UMD_PRESENT = 0x02;
    protected final int PSP_UMD_CHANGED = 0x04;
    protected final int PSP_UMD_INITING = 0x08;
    protected final int PSP_UMD_INITED = 0x10;
    protected final int PSP_UMD_READY = 0x20;

    protected UmdIsoReader iso;
    protected boolean umdActivated;
    protected boolean umdDeactivateCalled;
    protected List<SceKernelThreadInfo> waitingThreads;

    // HLE helper functions

    public void setIsoReader(UmdIsoReader iso) {
        this.iso = iso;

        // MGS:PO does not contain an import for sceUmdActivate
        // this suggests sceUmdActivate/sceUmdDeactivate are completely useless
        if (iso == null) {
            umdActivated = false;
        } else {
            umdActivated = true;
        }
    }

    public boolean isUmdActivated() {
        return umdActivated;
    }

    /** note this value is NOT the same as that used in the activate/deactivate callback */
    public int getUmdStat() {
        int stat;

        if (iso != null) {
            stat = PSP_UMD_PRESENT | PSP_UMD_INITED; // return 0x12
            if (umdActivated) stat |= PSP_UMD_READY; // return 0x32
        } else {
            stat = PSP_UMD_NOT_PRESENT; // return 0x1
            if (umdDeactivateCalled)
                stat |= PSP_UMD_INITING; // return 0x9
        }

        return stat;
    }

    protected int getUmdCallbackEvent() {
        int event = 0;
        if (iso != null) {
            event = PSP_UMD_PRESENT;

            if (umdActivated) {
                // it can also return just 0x2 and 0x12, immediately after the UMD has been inserted, but we'll go straight to 0x22
                event |= PSP_UMD_READY; // return 0x22
            } else {
                event |= PSP_UMD_INITED; // return 0x12
            }
        } else {
            event = PSP_UMD_NOT_PRESENT;

            if (!umdActivated && umdDeactivateCalled)
                event = PSP_UMD_INITING;
        }

        return event;
    }

    protected boolean checkDriveStat(int wantedStat) {
        int currentStat = getUmdStat();
        return ((currentStat & wantedStat) != 0);
    }

    // Export functions

    public void sceUmdCheckMedium(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor
        Modules.log.debug("sceUmdCheckMedium (umd mounted = " + (iso != null) + ")");
        cpu.gpr[2] = (iso != null) ? 1 : 0;
    }

    protected void removeWaitingThread(SceKernelThreadInfo thread)
    {
        for (ListIterator<SceKernelThreadInfo> lit = waitingThreads.listIterator(); lit.hasNext(); ) {
            SceKernelThreadInfo waitingThread = lit.next();
            if (waitingThread.uid == thread.uid) {
                lit.remove();
                break;
            }
        }
    }

    /** Don't call this unless thread.wait.waitingOnUmd == true */
    public void onThreadWaitTimeout(SceKernelThreadInfo thread) {
        Modules.log.info("UMD stat timedout");

        // Untrack
        thread.wait.waitingOnUmd = false;
        removeWaitingThread(thread);

        // Return WAIT_TIMEOUT
        thread.cpuContext.gpr[2] = ERROR_WAIT_TIMEOUT;
    }

    public void onThreadDeleted(SceKernelThreadInfo thread) {
        if (thread.wait.waitingOnUmd) {
            removeWaitingThread(thread);
        }
    }

    protected void checkWaitingThreads() {
    	for (ListIterator<SceKernelThreadInfo> lit = waitingThreads.listIterator(); lit.hasNext(); ) {
    		SceKernelThreadInfo waitingThread = lit.next();
    		if (waitingThread.status == SceKernelThreadInfo.PSP_THREAD_WAITING) {
    			int wantedUmdStat = waitingThread.wait.wantedUmdStat;
    			if (waitingThread.wait.waitingOnUmd && checkDriveStat(wantedUmdStat)) {
                    Modules.log.debug("sceUmdUser - checkWaitingThreads waking " + Integer.toHexString(waitingThread.uid) + " thread:'" + waitingThread.name + "'");

                    // Untrack
    				waitingThread.wait.waitingOnUmd = false;
                    lit.remove();

    				// Return success
    				waitingThread.cpuContext.gpr[2] = 0;

    				// Wakeup thread
    				ThreadMan.getInstance().hleChangeThreadState(waitingThread, SceKernelThreadInfo.PSP_THREAD_READY);
    			}
    		}
    	}
    }

    public void sceUmdActivate(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor

        int unit = cpu.gpr[4]; // should be always 1
        String drive = readStringZ(cpu.gpr[5]);
        Modules.log.debug("sceUmdActivate unit = " + unit + " drive = " + drive);

        umdActivated = true;
        cpu.gpr[2] = 0;

        ThreadMan.getInstance().hleKernelNotifyCallback(SceKernelThreadInfo.THREAD_CALLBACK_UMD, getUmdCallbackEvent());
        checkWaitingThreads();
    }

    public void sceUmdDeactivate(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor

        int unit = cpu.gpr[4]; // should be always 1
        String drive = readStringZ(cpu.gpr[5]);
        Modules.log.debug("sceUmdDeactivate unit = " + unit + " drive = " + drive);
        cpu.gpr[2] = 0;

        umdActivated = false;
        umdDeactivateCalled = true;

        ThreadMan.getInstance().hleKernelNotifyCallback(SceKernelThreadInfo.THREAD_CALLBACK_UMD, getUmdCallbackEvent());
        checkWaitingThreads();
    }

    protected void hleUmdWaitDriveStat(Processor processor, int wantedStat, boolean doCallbacks, boolean doTimeout, int timeout) {
    	ThreadMan threadMan = ThreadMan.getInstance();

        if (checkDriveStat(wantedStat)) {
        	processor.cpu.gpr[2] = 0;
        } else {
            SceKernelThreadInfo currentThread = threadMan.getCurrentThread();

            // wait type
            currentThread.waitType = SceKernelThreadInfo.PSP_WAIT_MISC;

            // Go to wait state
            threadMan.hleKernelThreadWait(currentThread, timeout, !doTimeout);

            // Wait on a specific umdStat
            currentThread.wait.waitingOnUmd = true;
            currentThread.wait.wantedUmdStat = wantedStat;

            waitingThreads.add(currentThread);

            threadMan.hleChangeThreadState(currentThread, PSP_THREAD_WAITING);
        }

        threadMan.hleRescheduleCurrentThread(doCallbacks);
    }

    /** wait forever until drive stat reaches a0 */
    public void sceUmdWaitDriveStat(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor
        int wantedStat = cpu.gpr[4];
        Modules.log.debug("sceUmdWaitDriveStat(stat=0x" + Integer.toHexString(wantedStat) + ")");

        hleUmdWaitDriveStat(processor, wantedStat, false, false, 0);
    }

    /** wait until drive stat reaches a0
     * timeout parameter is literal, not a pointer. */
    public void sceUmdWaitDriveStatWithTimer(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor

        int wantedStat = cpu.gpr[4];
        int timeout = cpu.gpr[5];
        Modules.log.debug("sceUmdWaitDriveStatWithTimer(stat=0x" + Integer.toHexString(wantedStat) + ",timeout=" + timeout + ")");

        hleUmdWaitDriveStat(processor, wantedStat, false, true, timeout);
    }

    /** wait until drive stat reaches a0
     * timeout parameter is literal, not a pointer. */
    public void sceUmdWaitDriveStatCB(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor

        int wantedStat = cpu.gpr[4];
        int timeout = cpu.gpr[5];
        Modules.log.debug("sceUmdWaitDriveStatCB(stat=0x" + Integer.toHexString(wantedStat) + ",timeout=" + timeout + ")");

        hleUmdWaitDriveStat(processor, wantedStat, true, true, timeout);
    }

    public void sceUmdCancelWaitDriveStat(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor

        ThreadMan threadMan = ThreadMan.getInstance();
        boolean handled = false;

        for (ListIterator<SceKernelThreadInfo> lit = waitingThreads.listIterator(); lit.hasNext(); ) {
            SceKernelThreadInfo waitingThread = lit.next();

            if (!waitingThread.wait.waitingOnUmd ||
                waitingThread.status != SceKernelThreadInfo.PSP_THREAD_WAITING) {
                Modules.log.error("sceUmdCancelWaitDriveStat thread " + Integer.toHexString(waitingThread.uid)
                    + " '" + waitingThread.name + "' not waiting on umd");

                handled = true;
            } else {
                Modules.log.debug("sceUmdCancelWaitDriveStat waking thread " + Integer.toHexString(waitingThread.uid)
                    + " '" + waitingThread.name + "'");

                // Untrack
                waitingThread.wait.waitingOnUmd = false;
                lit.remove();

                // Return WAIT_CANCELLED
                waitingThread.cpuContext.gpr[2] = ERROR_WAIT_CANCELLED;

                // Wakeup thread
                threadMan.hleChangeThreadState(waitingThread, SceKernelThreadInfo.PSP_THREAD_READY);

                handled = true;
            }
        }

        if (!handled) {
            Modules.log.debug("sceUmdCancelWaitDriveStat - nothing to do");
        }

        cpu.gpr[2] = 0;
    }

    public void sceUmdGetDriveStat(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor

        int stat = getUmdStat();

        Modules.log.debug("sceUmdGetDriveStat return:0x" + Integer.toHexString(stat));

        cpu.gpr[2] = stat;
    }

    /** @return
     *        0 : most of the time.
     * 80210003 : umd was activated, but user ejected the disc, goes back to 0 when the disc is put back in.
     */
    public void sceUmdGetErrorStat(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor

        cpu.gpr[2] = 0;

        Modules.log.debug("sceUmdGetErrorStat ret:0x" + Integer.toHexString(cpu.gpr[2]));
    }

    public void sceUmdGetDiscInfo(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int pspUmdInfoAddr = cpu.gpr[4];
        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug(String.format("sceUmdGetDiscInfo pspUmdInfoAddr=0x%08X", pspUmdInfoAddr));
        }

        if (mem.isAddressGood(pspUmdInfoAddr)) {
        	pspUmdInfo umdInfo = new pspUmdInfo();
        	umdInfo.read(mem, pspUmdInfoAddr);
        	umdInfo.type = pspUmdInfo.PSP_UMD_TYPE_GAME;
        	umdInfo.write(mem);

        	cpu.gpr[2] = 0;
        } else {
        	cpu.gpr[2] = -1;
        }
    }

    // TODO not fully implemented yet
    public void sceUmdRegisterUMDCallBack(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor

        int uid = cpu.gpr[4];
        Modules.log.debug("sceUmdRegisterUMDCallBack SceUID=" + Integer.toHexString(uid));

        ThreadMan threadMan = ThreadMan.getInstance();
        if (threadMan.hleKernelRegisterCallback(SceKernelThreadInfo.THREAD_CALLBACK_UMD, uid)) {
            // seems to be dependant on timing, for example if you run the
            // umdcallback sample immediately after resetting psplink you will
            // get all the callbacks, if you wait a few seconds after resetting
            // psplink you won't get them.
            if (iso != null) {
                ThreadMan.getInstance().hleKernelNotifyCallback(SceKernelThreadInfo.THREAD_CALLBACK_UMD, getUmdCallbackEvent());
            }

            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = -1;
        }
    }

    public void sceUmdUnRegisterUMDCallBack(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor

        int uid = cpu.gpr[4];
        Modules.log.debug("sceUmdUnRegisterUMDCallBack SceUID=" + Integer.toHexString(uid));

        SceKernelCallbackInfo info = ThreadMan.getInstance().hleKernelUnRegisterCallback(SceKernelThreadInfo.THREAD_CALLBACK_UMD, uid);
        if (info == null) {
            cpu.gpr[2] = -1;
        } else {
            cpu.gpr[2] = 0;
        }
    }

    public final HLEModuleFunction sceUmdCheckMediumFunction = new HLEModuleFunction("sceUmdUser", "sceUmdCheckMedium") {

        @Override
        public final void execute(Processor processor) {
            sceUmdCheckMedium(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUmdUserModule.sceUmdCheckMedium(processor);";
        }
    };
    public final HLEModuleFunction sceUmdActivateFunction = new HLEModuleFunction("sceUmdUser", "sceUmdActivate") {

        @Override
        public final void execute(Processor processor) {
            sceUmdActivate(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUmdUserModule.sceUmdActivate(processor);";
        }
    };
    public final HLEModuleFunction sceUmdDeactivateFunction = new HLEModuleFunction("sceUmdUser", "sceUmdDeactivate") {

        @Override
        public final void execute(Processor processor) {
            sceUmdDeactivate(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUmdUserModule.sceUmdDeactivate(processor);";
        }
    };
    public final HLEModuleFunction sceUmdWaitDriveStatFunction = new HLEModuleFunction("sceUmdUser", "sceUmdWaitDriveStat") {

        @Override
        public final void execute(Processor processor) {
            sceUmdWaitDriveStat(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUmdUserModule.sceUmdWaitDriveStat(processor);";
        }
    };
    public final HLEModuleFunction sceUmdWaitDriveStatWithTimerFunction = new HLEModuleFunction("sceUmdUser", "sceUmdWaitDriveStatWithTimer") {

        @Override
        public final void execute(Processor processor) {
            sceUmdWaitDriveStatWithTimer(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUmdUserModule.sceUmdWaitDriveStatWithTimer(processor);";
        }
    };
    public final HLEModuleFunction sceUmdWaitDriveStatCBFunction = new HLEModuleFunction("sceUmdUser", "sceUmdWaitDriveStatCB") {

        @Override
        public final void execute(Processor processor) {
            sceUmdWaitDriveStatCB(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUmdUserModule.sceUmdWaitDriveStatCB(processor);";
        }
    };
    public final HLEModuleFunction sceUmdCancelWaitDriveStatFunction = new HLEModuleFunction("sceUmdUser", "sceUmdCancelWaitDriveStat") {

        @Override
        public final void execute(Processor processor) {
            sceUmdCancelWaitDriveStat(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUmdUserModule.sceUmdCancelWaitDriveStat(processor);";
        }
    };
    public final HLEModuleFunction sceUmdGetDriveStatFunction = new HLEModuleFunction("sceUmdUser", "sceUmdGetDriveStat") {

        @Override
        public final void execute(Processor processor) {
            sceUmdGetDriveStat(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUmdUserModule.sceUmdGetDriveStat(processor);";
        }
    };
    public final HLEModuleFunction sceUmdGetErrorStatFunction = new HLEModuleFunction("sceUmdUser", "sceUmdGetErrorStat") {

        @Override
        public final void execute(Processor processor) {
            sceUmdGetErrorStat(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUmdUserModule.sceUmdGetErrorStat(processor);";
        }
    };
    public final HLEModuleFunction sceUmdGetDiscInfoFunction = new HLEModuleFunction("sceUmdUser", "sceUmdGetDiscInfo") {

        @Override
        public final void execute(Processor processor) {
            sceUmdGetDiscInfo(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUmdUserModule.sceUmdGetDiscInfo(processor);";
        }
    };
    public final HLEModuleFunction sceUmdRegisterUMDCallBackFunction = new HLEModuleFunction("sceUmdUser", "sceUmdRegisterUMDCallBack") {

        @Override
        public final void execute(Processor processor) {
            sceUmdRegisterUMDCallBack(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUmdUserModule.sceUmdRegisterUMDCallBack(processor);";
        }
    };
    public final HLEModuleFunction sceUmdUnRegisterUMDCallBackFunction = new HLEModuleFunction("sceUmdUser", "sceUmdUnRegisterUMDCallBack") {

        @Override
        public final void execute(Processor processor) {
            sceUmdUnRegisterUMDCallBack(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUmdUserModule.sceUmdUnRegisterUMDCallBack(processor);";
        }
    };
};
