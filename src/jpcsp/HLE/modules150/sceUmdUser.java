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

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.PspString;
import jpcsp.HLE.TPointer;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.IWaitStateChecker;
import jpcsp.HLE.kernel.types.SceKernelCallbackInfo;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.ThreadWaitInfo;
import jpcsp.HLE.kernel.types.pspUmdInfo;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.filesystems.umdiso.UmdIsoReader;

import org.apache.log4j.Logger;

@HLELogging
public class sceUmdUser extends HLEModule {
    public static Logger log = Modules.getLogger("sceUmdUser");

    @Override
    public String getName() {
        return "sceUmdUser";
    }

    @Override
    public void start() {
    	// Remember if the UMD was activated even after a call to sceKernelLoadExec()
    	setUmdActivated();

    	umdDeactivateCalled = false;
        waitingThreads = new LinkedList<SceKernelThreadInfo>();
        umdErrorStat = 0;
        umdWaitStateChecker = new UmdWaitStateChecker();

        super.start();
    }

    protected class UmdWaitStateChecker implements IWaitStateChecker {
		@Override
		public boolean continueWaitState(SceKernelThreadInfo thread, ThreadWaitInfo wait) {
			if (checkDriveStat(wait.wantedUmdStat)) {
				waitingThreads.remove(thread);
                // Return success
				thread.cpuContext._v0 = 0;

				// Do not continue the wait state
				return false;
			}

			return true;
		}
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
    protected int umdErrorStat;
    protected UmdWaitStateChecker umdWaitStateChecker;

    public void setIsoReader(UmdIsoReader iso) {
        this.iso = iso;
        setUmdActivated();
    }
    
    public void setUmdErrorStat(int stat) {
        umdErrorStat = stat;
    }

    public int getUmdErrorStat() {
        return umdErrorStat;
    }

    private void setUmdActivated() {
        if (iso == null) {
            umdActivated = false;
        } else {
            umdActivated = true;
        }
        Modules.IoFileMgrForUserModule.registerUmdIso();
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
        thread.cpuContext._v0 = ERROR_KERNEL_WAIT_TIMEOUT;
    }

    public void onThreadWaitReleased(SceKernelThreadInfo thread) {
        log.info("UMD stat released");
        removeWaitingThread(thread);
        // Return ERROR_WAIT_STATUS_RELEASED
        thread.cpuContext._v0 = ERROR_KERNEL_WAIT_STATUS_RELEASED;
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
                    waitingThread.cpuContext._v0 = 0;
                    // Wakeup thread
                    Modules.ThreadManForUserModule.hleChangeThreadState(waitingThread, SceKernelThreadInfo.PSP_THREAD_READY);
                }
            }
        }
    }

    protected int hleUmdWaitDriveStat(int wantedStat, boolean doCallbacks, boolean doTimeout, int timeout) {
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;

        if (!checkDriveStat(wantedStat)) {
            SceKernelThreadInfo currentThread = threadMan.getCurrentThread();
            // Wait on a specific umdStat.
            currentThread.wait.wantedUmdStat = wantedStat;
            waitingThreads.add(currentThread);
            threadMan.hleKernelThreadEnterWaitState(currentThread, JPCSP_WAIT_UMD, -1, umdWaitStateChecker, timeout, !doTimeout, doCallbacks);
        }
        threadMan.hleRescheduleCurrentThread(doCallbacks);

        return 0;
    }

    @HLEFunction(nid = 0x46EBB729, version = 150)
    public boolean sceUmdCheckMedium() {
        return iso != null;
    }

    @HLEFunction(nid = 0xC6183D47, version = 150, checkInsideInterrupt = true)
    public int sceUmdActivate(int mode, PspString drive) {
        umdActivated = true;
        Modules.IoFileMgrForUserModule.registerUmdIso();

        // Notify the callback.
        // The callback will be executed at the next sceXXXXCB() syscall.
        int notifyArg;
        if (iso != null) {
        	notifyArg = PSP_UMD_PRESENT | PSP_UMD_READABLE;
        	// The PSP is returning 0x32 instead of 0x22 when
        	//     sceKernelSetCompiledSdkVersion()
        	// has been called (i.e. when sceKernelGetCompiledSdkVersion() != 0).
        	if (Modules.SysMemUserForUserModule.hleKernelGetCompiledSdkVersion() != 0) {
        		notifyArg |= PSP_UMD_READY;
        	}
        } else {
        	notifyArg = PSP_UMD_NOT_PRESENT | PSP_UMD_NOT_READY;
        }
    	Modules.ThreadManForUserModule.hleKernelNotifyCallback(SceKernelThreadInfo.THREAD_CALLBACK_UMD, notifyArg);

    	checkWaitingThreads();

    	return 0;
    }

    @HLEFunction(nid = 0xE83742BA, version = 150, checkInsideInterrupt = true)
    public int sceUmdDeactivate(int mode, PspString drive) {
        // Trigger the callback only if the UMD was already activated.
        // The callback will be executed at the next sceXXXXCB() syscall.
        boolean triggerCallback = umdActivated;
        umdActivated = false;
        Modules.IoFileMgrForUserModule.registerUmdIso();
        umdDeactivateCalled = true;
        if (triggerCallback) {
            int notifyArg;
            if (iso != null) {
            	notifyArg = PSP_UMD_PRESENT | PSP_UMD_READY;
            } else {
            	notifyArg = PSP_UMD_NOT_PRESENT | PSP_UMD_NOT_READY;
            }
        	Modules.ThreadManForUserModule.hleKernelNotifyCallback(SceKernelThreadInfo.THREAD_CALLBACK_UMD, notifyArg);
        }
        checkWaitingThreads();

        return 0;
    }

    @HLEFunction(nid = 0x8EF08FCE, version = 150, checkInsideInterrupt = true)
    public int sceUmdWaitDriveStat(int wantedStat) {
        return hleUmdWaitDriveStat(wantedStat, false, false, 0);
    }

    @HLEFunction(nid = 0x56202973, version = 150, checkInsideInterrupt = true)
    public int sceUmdWaitDriveStatWithTimer(int wantedStat, int timeout) {
        return hleUmdWaitDriveStat(wantedStat, false, true, timeout);
    }

    @HLEFunction(nid = 0x4A9E5E29, version = 150, checkInsideInterrupt = true)
    public int sceUmdWaitDriveStatCB(int wantedStat, int timeout) {
        return hleUmdWaitDriveStat(wantedStat, true, true, timeout);
    }

    @HLEFunction(nid = 0x6AF9B50A, version = 150)
    public int sceUmdCancelWaitDriveStat() {
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;

        for (ListIterator<SceKernelThreadInfo> lit = waitingThreads.listIterator(); lit.hasNext();) {
            SceKernelThreadInfo waitingThread = lit.next();
            if (!waitingThread.isWaiting() || waitingThread.waitType != JPCSP_WAIT_UMD) {
                log.warn(String.format("sceUmdCancelWaitDriveStat thread %s not waiting on umd", waitingThread));
            } else {
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("sceUmdCancelWaitDriveStat waking thread %s", waitingThread));
            	}
                lit.remove();
                // Return WAIT_CANCELLED.
                waitingThread.cpuContext._v0 = ERROR_KERNEL_WAIT_CANCELLED;
                // Wakeup thread
                threadMan.hleChangeThreadState(waitingThread, SceKernelThreadInfo.PSP_THREAD_READY);
            }
        }

        return 0;
    }

    @HLEFunction(nid = 0x6B4A146C, version = 150)
    public int sceUmdGetDriveStat() {
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceUmdGetDriveStat returning 0x%X", getUmdStat()));
        }

        return getUmdStat();
    }

    @HLEFunction(nid = 0x20628E6F, version = 150)
    public int sceUmdGetErrorStat() {
        return getUmdErrorStat();
    }

    @HLEFunction(nid = 0x340B7686, version = 150, checkInsideInterrupt = true)
    public int sceUmdGetDiscInfo(TPointer pspUmdInfoAddr) {
        pspUmdInfo umdInfo = new pspUmdInfo();
        umdInfo.read(pspUmdInfoAddr);
        umdInfo.type = pspUmdInfo.PSP_UMD_TYPE_GAME;
        umdInfo.write(pspUmdInfoAddr);

        return 0;
    }

    @HLEFunction(nid = 0xAEE7404D, version = 150, checkInsideInterrupt = true)
    public int sceUmdRegisterUMDCallBack(int uid) {
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        if (!threadMan.hleKernelRegisterCallback(SceKernelThreadInfo.THREAD_CALLBACK_UMD, uid)) {
            return -1;
        }

        return 0;
    }

    @HLEFunction(nid = 0xBD2BDE07, version = 150)
    public int sceUmdUnRegisterUMDCallBack(int uid) {
        SceKernelCallbackInfo info = Modules.ThreadManForUserModule.hleKernelUnRegisterCallback(SceKernelThreadInfo.THREAD_CALLBACK_UMD, uid);
        if (info == null) {
        	return -1;
        }

        return 0;
    }
}