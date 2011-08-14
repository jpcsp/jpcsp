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

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_WAIT_CANCELLED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_WAIT_STATUS_RELEASED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_WAIT_TIMEOUT;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.JPCSP_WAIT_UMD;
import static jpcsp.util.Utilities.readStringZ;

import jpcsp.HLE.HLEFunction;
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
    public void installModule(HLEModuleManager mm, int version) { mm.installModuleWithAnnotations(this, version); }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) { mm.uninstallModuleWithAnnotations(this, version); }

    @Override
    public void start() {
    	// Remember if the UMD was activated even after a call to sceKernelLoadExec()
    	setUmdActivated();

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
        setUmdActivated();
    }

    private void setUmdActivated() {
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
            stat = PSP_UMD_PRESENT | PSP_UMD_READY;
            if (umdActivated) {
                stat |= PSP_UMD_READABLE;
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
        removeWaitingThread(thread);
        // Return WAIT_TIMEOUT
        thread.cpuContext.gpr[2] = ERROR_KERNEL_WAIT_TIMEOUT;
    }

    public void onThreadWaitReleased(SceKernelThreadInfo thread) {
        log.info("UMD stat released");
        removeWaitingThread(thread);
        // Return ERROR_WAIT_STATUS_RELEASED
        thread.cpuContext.gpr[2] = ERROR_KERNEL_WAIT_STATUS_RELEASED;
    }

    public void onThreadDeleted(SceKernelThreadInfo thread) {
        if (thread.waitType == JPCSP_WAIT_UMD) {
            removeWaitingThread(thread);
        }
    }

    protected void checkWaitingThreads() {
        for (ListIterator<SceKernelThreadInfo> lit = waitingThreads.listIterator(); lit.hasNext();) {
            SceKernelThreadInfo waitingThread = lit.next();
            if (waitingThread.status == SceKernelThreadInfo.PSP_THREAD_WAITING) {
                int wantedUmdStat = waitingThread.wait.wantedUmdStat;
                if (waitingThread.waitType == JPCSP_WAIT_UMD &&
                        checkDriveStat(wantedUmdStat)) {
                	if (log.isDebugEnabled()) {
                		log.debug("sceUmdUser - checkWaitingThreads waking " + Integer.toHexString(waitingThread.uid) + " thread:'" + waitingThread.name + "'");
                	}
                    lit.remove();
                    // Return success
                    waitingThread.cpuContext.gpr[2] = 0;
                    // Wakeup thread
                    Modules.ThreadManForUserModule.hleChangeThreadState(waitingThread, SceKernelThreadInfo.PSP_THREAD_READY);
                }
            }
        }
    }

    @HLEFunction(nid = 0x46EBB729, version = 150)
    public void sceUmdCheckMedium(Processor processor) {
        CpuState cpu = processor.cpu;

        if(log.isDebugEnabled()) {
            log.debug("sceUmdCheckMedium (umd mounted = " + (iso != null) + ")");
        }

        cpu.gpr[2] = (iso != null) ? 1 : 0;
    }

    @HLEFunction(nid = 0xC6183D47, version = 150)
    public void sceUmdActivate(Processor processor) {
        CpuState cpu = processor.cpu;

        int mode = cpu.gpr[4];
        String drive = readStringZ(cpu.gpr[5]);

        if(log.isDebugEnabled()) {
            log.debug("sceUmdActivate mode = " + mode + " drive = " + drive);
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        umdActivated = true;
        cpu.gpr[2] = 0;

        SceKernelThreadInfo thread = Modules.ThreadManForUserModule.getCurrentThread();
    	thread.doCallbacks = true;
        if (iso != null) {
        	Modules.ThreadManForUserModule.hleKernelNotifyCallback(SceKernelThreadInfo.THREAD_CALLBACK_UMD, PSP_UMD_PRESENT | PSP_UMD_READABLE);
        } else {
        	Modules.ThreadManForUserModule.hleKernelNotifyCallback(SceKernelThreadInfo.THREAD_CALLBACK_UMD, PSP_UMD_NOT_PRESENT | PSP_UMD_NOT_READY);
        }
    	thread.doCallbacks = false;

    	checkWaitingThreads();
    }

    @HLEFunction(nid = 0xE83742BA, version = 150)
    public void sceUmdDeactivate(Processor processor) {
        CpuState cpu = processor.cpu;

        int mode = cpu.gpr[4];
        String drive = readStringZ(cpu.gpr[5]);

        if(log.isDebugEnabled()) {
            log.debug("sceUmdDeactivate mode = " + mode + " drive = " + drive);
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        // Trigger the callback only if the UMD was already activated
        boolean triggerCallback = umdActivated;
        umdActivated = false;
        umdDeactivateCalled = true;
        cpu.gpr[2] = 0;
        if (triggerCallback) {
        	SceKernelThreadInfo thread = Modules.ThreadManForUserModule.getCurrentThread();
        	thread.doCallbacks = true;
	        if (iso != null) {
	        	Modules.ThreadManForUserModule.hleKernelNotifyCallback(SceKernelThreadInfo.THREAD_CALLBACK_UMD, PSP_UMD_PRESENT | PSP_UMD_READY);
	        } else {
	        	Modules.ThreadManForUserModule.hleKernelNotifyCallback(SceKernelThreadInfo.THREAD_CALLBACK_UMD, PSP_UMD_NOT_PRESENT | PSP_UMD_NOT_READY);
	        }
        	thread.doCallbacks = false;
        }
        checkWaitingThreads();
    }

    protected void hleUmdWaitDriveStat(Processor processor, int wantedStat, boolean doCallbacks, boolean doTimeout, int timeout) {
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        CpuState cpu = processor.cpu;

        if (checkDriveStat(wantedStat)) {
            cpu.gpr[2] = 0;
        } else {
            SceKernelThreadInfo currentThread = threadMan.getCurrentThread();
            // Wait on a specific umdStat.
            currentThread.wait.wantedUmdStat = wantedStat;
            waitingThreads.add(currentThread);
            threadMan.hleKernelThreadEnterWaitState(currentThread, JPCSP_WAIT_UMD, -1, null, timeout, !doTimeout, doCallbacks);
        }
        threadMan.hleRescheduleCurrentThread(doCallbacks);
    }

    @HLEFunction(nid = 0x8EF08FCE, version = 150)
    public void sceUmdWaitDriveStat(Processor processor) {
        CpuState cpu = processor.cpu;

        int wantedStat = cpu.gpr[4];

        if(log.isDebugEnabled()) {
            log.debug("sceUmdWaitDriveStat(stat=0x" + Integer.toHexString(wantedStat) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        hleUmdWaitDriveStat(processor, wantedStat, false, false, 0);
    }

    @HLEFunction(nid = 0x56202973, version = 150)
    public void sceUmdWaitDriveStatWithTimer(Processor processor) {
        CpuState cpu = processor.cpu;

        int wantedStat = cpu.gpr[4];
        int timeout = cpu.gpr[5];

        if(log.isDebugEnabled()) {
            log.debug("sceUmdWaitDriveStatWithTimer(stat=0x" + Integer.toHexString(wantedStat)
                    + ", timeout=" + timeout + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        hleUmdWaitDriveStat(processor, wantedStat, false, true, timeout);
    }

    @HLEFunction(nid = 0x4A9E5E29, version = 150)
    public void sceUmdWaitDriveStatCB(Processor processor) {
        CpuState cpu = processor.cpu;

        int wantedStat = cpu.gpr[4];
        int timeout = cpu.gpr[5];

        if(log.isDebugEnabled()) {
            log.debug("sceUmdWaitDriveStatCB(stat=0x" + Integer.toHexString(wantedStat)
                    + ",timeout=" + timeout + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        hleUmdWaitDriveStat(processor, wantedStat, true, true, timeout);
    }

    @HLEFunction(nid = 0x6AF9B50A, version = 150)
    public void sceUmdCancelWaitDriveStat(Processor processor) {
        CpuState cpu = processor.cpu;
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;

        for (ListIterator<SceKernelThreadInfo> lit = waitingThreads.listIterator(); lit.hasNext();) {
            SceKernelThreadInfo waitingThread = lit.next();
            if (!waitingThread.isWaiting() || waitingThread.waitType != JPCSP_WAIT_UMD) {
                log.error("sceUmdCancelWaitDriveStat thread " + Integer.toHexString(waitingThread.uid)
                        + " '" + waitingThread.name + "' not waiting on umd");
            } else {
                log.debug("sceUmdCancelWaitDriveStat waking thread " + Integer.toHexString(waitingThread.uid)
                        + " '" + waitingThread.name + "'");
                lit.remove();
                // Return WAIT_CANCELLED.
                waitingThread.cpuContext.gpr[2] = ERROR_KERNEL_WAIT_CANCELLED;
                // Wakeup thread
                threadMan.hleChangeThreadState(waitingThread, SceKernelThreadInfo.PSP_THREAD_READY);
            }
        }
        cpu.gpr[2] = 0;
    }

    @HLEFunction(nid = 0x6B4A146C, version = 150)
    public void sceUmdGetDriveStat(Processor processor) {
        CpuState cpu = processor.cpu;

        if(log.isDebugEnabled()) {
            log.debug("sceUmdGetDriveStat");
        }

        cpu.gpr[2] = getUmdStat();
    }

    @HLEFunction(nid = 0x20628E6F, version = 150)
    public void sceUmdGetErrorStat(Processor processor) {
        CpuState cpu = processor.cpu;

        if(log.isDebugEnabled()) {
            log.debug("sceUmdGetErrorStat");
        }

        cpu.gpr[2] = 0;
    }

    @HLEFunction(nid = 0x340B7686, version = 150)
    public void sceUmdGetDiscInfo(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int pspUmdInfoAddr = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceUmdGetDiscInfo pspUmdInfoAddr=0x%08X", pspUmdInfoAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
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

    @HLEFunction(nid = 0xAEE7404D, version = 150)
    public void sceUmdRegisterUMDCallBack(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug("sceUmdRegisterUMDCallBack SceUID=" + Integer.toHexString(uid));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        if (threadMan.hleKernelRegisterCallback(SceKernelThreadInfo.THREAD_CALLBACK_UMD, uid)) {
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = -1;
        }
    }

    @HLEFunction(nid = 0xBD2BDE07, version = 150)
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

}