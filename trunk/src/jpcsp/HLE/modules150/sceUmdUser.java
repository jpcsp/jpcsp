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

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_WAIT_CANCELLED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_WAIT_STATUS_RELEASED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_WAIT_TIMEOUT;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_WAITING;
import static jpcsp.util.Utilities.readStringZ;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelCallbackInfo;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.pspUmdInfo;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.HLEStartModule;
import jpcsp.filesystems.umdiso.UmdIsoReader;

import org.apache.log4j.Logger;

public class sceUmdUser implements HLEModule, HLEStartModule {

    protected static Logger log = Modules.getLogger("sceUmdUser");

    @Override
    public String getName() {
        return "sceUmdUser";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.addFunction(0x46EBB729, sceUmdCheckMediumFunction);
            mm.addFunction(0xC6183D47, sceUmdActivateFunction);
            mm.addFunction(0xE83742BA, sceUmdDeactivateFunction);
            mm.addFunction(0x8EF08FCE, sceUmdWaitDriveStatFunction);
            mm.addFunction(0x56202973, sceUmdWaitDriveStatWithTimerFunction);
            mm.addFunction(0x4A9E5E29, sceUmdWaitDriveStatCBFunction);
            mm.addFunction(0x6AF9B50A, sceUmdCancelWaitDriveStatFunction);
            mm.addFunction(0x6B4A146C, sceUmdGetDriveStatFunction);
            mm.addFunction(0x20628E6F, sceUmdGetErrorStatFunction);
            mm.addFunction(0x340B7686, sceUmdGetDiscInfoFunction);
            mm.addFunction(0xAEE7404D, sceUmdRegisterUMDCallBackFunction);
            mm.addFunction(0xBD2BDE07, sceUmdUnRegisterUMDCallBackFunction);

        }
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

    @Override
    public void start() {
        umdActivated = false;
        umdDeactivateCalled = false;
        waitingThreads = new LinkedList<SceKernelThreadInfo>();
    }

    @Override
    public void stop() {
    }

    protected static final int PSP_UMD_INIT = 0x00;
    protected static final int PSP_UMD_NOT_PRESENT = 0x01;
    protected static final int PSP_UMD_PRESENT = 0x02;
    protected static final int PSP_UMD_CHANGED = 0x04;
    protected static final int PSP_UMD_NOT_READY = 0x08;
    protected static final int PSP_UMD_READY = 0x10;
    protected static final int PSP_UMD_READABLE = 0x20;
    protected UmdIsoReader iso;
    protected boolean umdActivated;
    protected boolean umdDeactivateCalled;
    protected List<SceKernelThreadInfo> waitingThreads;

    public void setIsoReader(UmdIsoReader iso) {
        this.iso = iso;
        if (iso == null) {
            umdActivated = false;
        } else {
            umdActivated = true;
        }
    }

    public boolean isUmdActivated() {
        return umdActivated;
    }

    public int getUmdStat() {
        int stat;
        if (iso != null) {
            stat = PSP_UMD_PRESENT;
            if (umdActivated) {
                stat |= PSP_UMD_READY;
                stat |= PSP_UMD_READABLE;
            } else {
                stat |= PSP_UMD_NOT_READY;
            }
        } else {
            stat = PSP_UMD_NOT_PRESENT;
            if (umdDeactivateCalled) {
                stat |= PSP_UMD_NOT_READY;
            }
        }
        return stat;
    }

    protected boolean checkDriveStat(int wantedStat) {
        int currentStat = getUmdStat();
        return ((currentStat & wantedStat) != 0);
    }

    protected void removeWaitingThread(SceKernelThreadInfo thread) {
        for (ListIterator<SceKernelThreadInfo> lit = waitingThreads.listIterator(); lit.hasNext();) {
            SceKernelThreadInfo waitingThread = lit.next();
            if (waitingThread.uid == thread.uid) {
                lit.remove();
                break;
            }
        }
    }

    public void onThreadWaitTimeout(SceKernelThreadInfo thread) {
        log.info("UMD stat timedout");
        // Untrack
        thread.wait.waitingOnUmd = false;
        removeWaitingThread(thread);
        // Return WAIT_TIMEOUT
        thread.cpuContext.gpr[2] = ERROR_WAIT_TIMEOUT;
    }

    public void onThreadWaitReleased(SceKernelThreadInfo thread) {
        log.info("UMD stat released");
        // Untrack
        thread.wait.waitingOnUmd = false;
        removeWaitingThread(thread);
        // Return ERROR_WAIT_STATUS_RELEASED
        thread.cpuContext.gpr[2] = ERROR_WAIT_STATUS_RELEASED;
    }

    public void onThreadDeleted(SceKernelThreadInfo thread) {
        if (thread.wait.waitingOnUmd) {
            removeWaitingThread(thread);
        }
    }

    protected void checkWaitingThreads() {
        for (ListIterator<SceKernelThreadInfo> lit = waitingThreads.listIterator(); lit.hasNext();) {
            SceKernelThreadInfo waitingThread = lit.next();
            if (waitingThread.status == SceKernelThreadInfo.PSP_THREAD_WAITING) {
                int wantedUmdStat = waitingThread.wait.wantedUmdStat;
                if (waitingThread.wait.waitingOnUmd && checkDriveStat(wantedUmdStat)) {
                    log.debug("sceUmdUser - checkWaitingThreads waking " + Integer.toHexString(waitingThread.uid) + " thread:'" + waitingThread.name + "'");
                    // Untrack
                    waitingThread.wait.waitingOnUmd = false;
                    lit.remove();
                    // Return success
                    waitingThread.cpuContext.gpr[2] = 0;
                    // Wakeup thread
                    Modules.ThreadManForUserModule.hleChangeThreadState(waitingThread, SceKernelThreadInfo.PSP_THREAD_READY);
                }
            }
        }
    }

    public void sceUmdCheckMedium(Processor processor) {
        CpuState cpu = processor.cpu;

        if(log.isDebugEnabled()) {
            log.debug("sceUmdCheckMedium (umd mounted = " + (iso != null) + ")");
        }

        cpu.gpr[2] = (iso != null) ? 1 : 0;
    }

    public void sceUmdActivate(Processor processor) {
        CpuState cpu = processor.cpu;

        int mode = cpu.gpr[4];
        String drive = readStringZ(cpu.gpr[5]);

        if(log.isDebugEnabled()) {
            log.debug("sceUmdActivate mode = " + mode + " drive = " + drive);
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        umdActivated = true;
        cpu.gpr[2] = 0;
        checkWaitingThreads();
    }

    public void sceUmdDeactivate(Processor processor) {
        CpuState cpu = processor.cpu;

        int mode = cpu.gpr[4];
        String drive = readStringZ(cpu.gpr[5]);

        if(log.isDebugEnabled()) {
            log.debug("sceUmdDeactivate mode = " + mode + " drive = " + drive);
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        umdActivated = false;
        umdDeactivateCalled = true;
        cpu.gpr[2] = 0;
        checkWaitingThreads();
    }

    protected void hleUmdWaitDriveStat(Processor processor, int wantedStat, boolean doCallbacks, boolean doTimeout, int timeout) {
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        CpuState cpu = processor.cpu;

        if (checkDriveStat(wantedStat)) {
            cpu.gpr[2] = 0;
        } else {
            SceKernelThreadInfo currentThread = threadMan.getCurrentThread();
            // Wait type.
            currentThread.waitType = SceKernelThreadInfo.PSP_WAIT_EVENTFLAG;
            // Go to wait state.
            threadMan.hleKernelThreadWait(currentThread, timeout, !doTimeout);
            // Wait on a specific umdStat.
            currentThread.wait.waitingOnUmd = true;
            currentThread.wait.wantedUmdStat = wantedStat;
            waitingThreads.add(currentThread);
            threadMan.hleChangeThreadState(currentThread, PSP_THREAD_WAITING);
        }
        threadMan.hleRescheduleCurrentThread(doCallbacks);
    }

    public void sceUmdWaitDriveStat(Processor processor) {
        CpuState cpu = processor.cpu;

        int wantedStat = cpu.gpr[4];

        if(log.isDebugEnabled()) {
            log.debug("sceUmdWaitDriveStat(stat=0x" + Integer.toHexString(wantedStat) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        hleUmdWaitDriveStat(processor, wantedStat, false, false, 0);
    }

    public void sceUmdWaitDriveStatWithTimer(Processor processor) {
        CpuState cpu = processor.cpu;

        int wantedStat = cpu.gpr[4];
        int timeout = cpu.gpr[5];

        if(log.isDebugEnabled()) {
            log.debug("sceUmdWaitDriveStatWithTimer(stat=0x" + Integer.toHexString(wantedStat)
                    + ", timeout=" + timeout + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        hleUmdWaitDriveStat(processor, wantedStat, false, true, timeout);
    }

    public void sceUmdWaitDriveStatCB(Processor processor) {
        CpuState cpu = processor.cpu;

        int wantedStat = cpu.gpr[4];
        int timeout = cpu.gpr[5];

        if(log.isDebugEnabled()) {
            log.debug("sceUmdWaitDriveStatCB(stat=0x" + Integer.toHexString(wantedStat)
                    + ",timeout=" + timeout + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        hleUmdWaitDriveStat(processor, wantedStat, true, true, timeout);
    }

    public void sceUmdCancelWaitDriveStat(Processor processor) {
        CpuState cpu = processor.cpu;
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;

        for (ListIterator<SceKernelThreadInfo> lit = waitingThreads.listIterator(); lit.hasNext();) {
            SceKernelThreadInfo waitingThread = lit.next();
            if (!waitingThread.wait.waitingOnUmd || waitingThread.status != SceKernelThreadInfo.PSP_THREAD_WAITING) {
                log.error("sceUmdCancelWaitDriveStat thread " + Integer.toHexString(waitingThread.uid)
                        + " '" + waitingThread.name + "' not waiting on umd");
            } else {
                log.debug("sceUmdCancelWaitDriveStat waking thread " + Integer.toHexString(waitingThread.uid)
                        + " '" + waitingThread.name + "'");
                // Untrack.
                waitingThread.wait.waitingOnUmd = false;
                lit.remove();
                // Return WAIT_CANCELLED.
                waitingThread.cpuContext.gpr[2] = ERROR_WAIT_CANCELLED;
                // Wakeup thread
                threadMan.hleChangeThreadState(waitingThread, SceKernelThreadInfo.PSP_THREAD_READY);
            }
        }
        cpu.gpr[2] = 0;
    }

    public void sceUmdGetDriveStat(Processor processor) {
        CpuState cpu = processor.cpu;

        if(log.isDebugEnabled()) {
            log.debug("sceUmdGetDriveStat");
        }

        cpu.gpr[2] = getUmdStat();
    }

    public void sceUmdGetErrorStat(Processor processor) {
        CpuState cpu = processor.cpu;

        if(log.isDebugEnabled()) {
            log.debug("sceUmdGetErrorStat");
        }

        cpu.gpr[2] = 0;
    }

    public void sceUmdGetDiscInfo(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int pspUmdInfoAddr = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceUmdGetDiscInfo pspUmdInfoAddr=0x%08X", pspUmdInfoAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (Memory.isAddressGood(pspUmdInfoAddr)) {
            pspUmdInfo umdInfo = new pspUmdInfo();
            umdInfo.read(mem, pspUmdInfoAddr);
            umdInfo.type = pspUmdInfo.PSP_UMD_TYPE_GAME;
            umdInfo.write(mem);
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = -1;
        }
    }

    public void sceUmdRegisterUMDCallBack(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug("sceUmdRegisterUMDCallBack SceUID=" + Integer.toHexString(uid));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        if (threadMan.hleKernelRegisterCallback(SceKernelThreadInfo.THREAD_CALLBACK_UMD, uid)) {
            Modules.ThreadManForUserModule.hleKernelNotifyCallback(SceKernelThreadInfo.THREAD_CALLBACK_UMD, getUmdStat());
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = -1;
        }
    }

    public void sceUmdUnRegisterUMDCallBack(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug("sceUmdUnRegisterUMDCallBack SceUID=" + Integer.toHexString(uid));
        }

        SceKernelCallbackInfo info = Modules.ThreadManForUserModule.hleKernelUnRegisterCallback(SceKernelThreadInfo.THREAD_CALLBACK_UMD, uid);
        if (info != null) {
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = -1;
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
}