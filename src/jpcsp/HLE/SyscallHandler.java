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

import jpcsp.Emulator;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.kernel.Managers;
import jpcsp.util.DurationStatistics;
import jpcsp.Allegrex.CpuState;
import jpcsp.Debugger.DisassemblerModule.syscallsFirm15;

public class SyscallHandler {
	public static DurationStatistics durationStatistics = new DurationStatistics("Syscall");
    public static boolean ignoreUnmappedImports = false;

	public static void reset() {
		durationStatistics.reset();
	}

    public static boolean isEnableIgnoreUnmappedImports(){
        return ignoreUnmappedImports;
    }

    public static void setEnableIgnoreUnmappedImports(boolean enable){
        ignoreUnmappedImports = enable;
        if(enable)
            Modules.log.info("Ignore Unmapped Imports enabled");
    }

    public static void syscall(int code) {
        int gpr[] = Emulator.getProcessor().cpu.gpr;
        Modules.ThreadManForUserModule.clearSyscallFreeCycles();

        durationStatistics.start();

        switch(code) {
		    case 0x2087:
		        pspiofilemgr.getInstance().sceIoPollAsync(gpr[4], gpr[5]);
		        break;
		    case 0x2088:
		        pspiofilemgr.getInstance().sceIoWaitAsync(gpr[4], gpr[5]);
		        break;
		    case 0x2089:
		        pspiofilemgr.getInstance().sceIoWaitAsyncCB(gpr[4], gpr[5]);
		        break;
		    case 0x208a:
		        pspiofilemgr.getInstance().sceIoGetAsyncStat(gpr[4], gpr[5], gpr[6]);
		        break;
		    case 0x208b:
		        pspiofilemgr.getInstance().sceIoChangeAsyncPriority(gpr[4], gpr[5]);
		        break;
		    case 0x208c:
		        pspiofilemgr.getInstance().sceIoSetAsyncCallback(gpr[4], gpr[5], gpr[6]);
		        break;
		    case 0x208d:
		        pspiofilemgr.getInstance().sceIoClose(gpr[4]);
		        break;
		    case 0x208e:
		        pspiofilemgr.getInstance().sceIoCloseAsync(gpr[4]);
		        break;
		    case 0x208f:
		        pspiofilemgr.getInstance().sceIoOpen(gpr[4], gpr[5], gpr[6]);
		        break;
		    case 0x2090:
		        pspiofilemgr.getInstance().sceIoOpenAsync(gpr[4], gpr[5], gpr[6]);
		        break;
		    case 0x2091:
		        pspiofilemgr.getInstance().sceIoRead(gpr[4], gpr[5], gpr[6]);
		        break;
		    case 0x2092:
		        pspiofilemgr.getInstance().sceIoReadAsync(gpr[4], gpr[5], gpr[6]);
		        break;
		    case 0x2093:
		        pspiofilemgr.getInstance().sceIoWrite(gpr[4], gpr[5], gpr[6]);
		        break;
		    case 0x2094:
		        pspiofilemgr.getInstance().sceIoWriteAsync(gpr[4], gpr[5], gpr[6]);
		        break;
		    case 0x2095:
		        pspiofilemgr.getInstance().sceIoLseek(
		                gpr[4],
		                ((((long)gpr[6]) & 0xFFFFFFFFL) | (((long)gpr[7])<<32)),
		                gpr[8]);
		        break;
		    case 0x2096:
		        pspiofilemgr.getInstance().sceIoLseekAsync(
		                gpr[4],
		                ((((long)gpr[6]) & 0xFFFFFFFFL) | (((long)gpr[7])<<32)),
		                gpr[8]);
		        break;
		    case 0x2097:
		        pspiofilemgr.getInstance().sceIoLseek32(
		                gpr[4],
		                gpr[5],
		                gpr[6]);
		        break;
		    case 0x2098:
		        pspiofilemgr.getInstance().sceIoLseek32Async(
		                gpr[4],
		                gpr[5],
		                gpr[6]);
		        break;
		    case 0x2099:
		        pspiofilemgr.getInstance().sceIoIoctl(gpr[4], gpr[5], gpr[6], gpr[7], gpr[8], gpr[9]);
		        break;
		    case 0x209a:
		        pspiofilemgr.getInstance().sceIoIoctlAsync(gpr[4], gpr[5], gpr[6], gpr[7], gpr[8], gpr[9]);
		        break;
		    case 0x209b:
		        pspiofilemgr.getInstance().sceIoDopen(gpr[4]);
		        break;
		    case 0x209c:
		        pspiofilemgr.getInstance().sceIoDread(gpr[4], gpr[5]);
		        break;
		    case 0x209d:
		        pspiofilemgr.getInstance().sceIoDclose(gpr[4]);
		        break;
		    case 0x209e:
				pspiofilemgr.getInstance().sceIoRemove(gpr[4]);
				break;
		    case 0x209f:
		        pspiofilemgr.getInstance().sceIoMkdir(gpr[4], gpr[5]);
		        break;
		        // sceIoRmdir(0x20a0),
		    case 0x20a1:
		        pspiofilemgr.getInstance().sceIoChdir(gpr[4]);
		        break;
		    case 0x20a2:
		        pspiofilemgr.getInstance().sceIoSync(gpr[4], gpr[5]);
		        break;
		    case 0x20a3:
		        pspiofilemgr.getInstance().sceIoGetstat(gpr[4], gpr[5]);
		        break;
		    case 0x20a4:
		    	pspiofilemgr.getInstance().sceIoChstat(gpr[4], gpr[5], gpr[6]);
		    	break;
		        // sceIoRename(0x20a5),
		    case 0x20a6:
		        pspiofilemgr.getInstance().sceIoDevctl(gpr[4], gpr[5], gpr[6], gpr[7], gpr[8], gpr[9]);
		        break;
		        // sceIoGetDevType(0x20a7),
		    case 0x20a8:
		        pspiofilemgr.getInstance().sceIoAssign(gpr[4], gpr[5], gpr[6], gpr[7], gpr[8], gpr[9]);
		        break;
		        // sceIoUnassign(0x20a9),
		        // sceIoCancel(0x20aa),
		        // IoFileMgrForUser_5C2BE2CC(0x20ab),
		        // sceKernelStdioRead(0x20ac),
		        // sceKernelStdioLseek(0x20ad),
		        // sceKernelStdioSendChar(0x20ae),
		        // sceKernelStdioWrite(0x20af),
		        // sceKernelStdioClose(0x20b0),
		         //sceKernelStdioOpen(0x20b1),
		    case 0x213a:
		        pspdisplay.getInstance().sceDisplaySetMode(gpr[4], gpr[5], gpr[6]);
		        break;
		    case 0x213b:
		        pspdisplay.getInstance().sceDisplayGetMode(gpr[4], gpr[5], gpr[6]);
		        break;
		    case 0x213c:
		        pspdisplay.getInstance().sceDisplayGetFramePerSec();
		        break;
		    case 0x213d:
		        pspdisplay.getInstance().sceDisplaySetHoldMode(gpr[4]);
		        break;
		    case 0x213e:
		        pspdisplay.getInstance().sceDisplaySetResumeMode(gpr[4]);
		        break;
		    case 0x213f:
		        pspdisplay.getInstance().sceDisplaySetFrameBuf(gpr[4], gpr[5], gpr[6], gpr[7]);
		        break;
		    case 0x2140:
		        pspdisplay.getInstance().sceDisplayGetFrameBuf(gpr[4], gpr[5], gpr[6], gpr[7]);
		        break;
		    case 0x2141:
		        pspdisplay.getInstance().sceDisplayIsForeground();
		        break;
		    case 0x2142:
		        pspdisplay.getInstance().sceDisplayGetBrightness(gpr[4], gpr[5]);
		        break;
		    case 0x2143:
		        pspdisplay.getInstance().sceDisplayGetVcount();
		        break;
		    case 0x2144:
		    	pspdisplay.getInstance().sceDisplayIsVblank();
		    	break;
		    case 0x2145:
		        pspdisplay.getInstance().sceDisplayWaitVblank();
		        break;
		    case 0x2146:
		        pspdisplay.getInstance().sceDisplayWaitVblankCB();
		        break;
		    case 0x2147:
		        pspdisplay.getInstance().sceDisplayWaitVblankStart();
		        break;
		    case 0x2148:
		        pspdisplay.getInstance().sceDisplayWaitVblankStartCB();
		        break;
		    case 0x2149:
		        pspdisplay.getInstance().sceDisplayGetCurrentHcount();
		        break;
		    case 0x214a:
		        pspdisplay.getInstance().sceDisplayGetAccumulatedHcount();
		        break;
		    case 0x214b:
		        pspdisplay.getInstance().sceDisplay_A83EF139();
		        break;
		    case 0x30a7:
		        Managers.mutex.sceKernelCreateMutex(gpr[4], gpr[5], gpr[6], gpr[7]);
		        break;
		    case 0x3015:
		        Managers.mutex.sceKernelDeleteMutex(gpr[4]);
		        break;
		    case 0x3017:
		        Managers.mutex.sceKernelLockMutex(gpr[4], gpr[5], gpr[6]);
		        break;
		    case 0x30a2:
		        Managers.mutex.sceKernelLockMutexCB(gpr[4], gpr[5], gpr[6]);
		        break;
		    case 0x30a1:
		        Managers.mutex.sceKernelTryLockMutex(gpr[4], gpr[5]);
		        break;
		    case 0x3016:
		        Managers.mutex.sceKernelUnlockMutex(gpr[4], gpr[5]);
		        break;
		    case 0x30a3:
		        Managers.mutex.sceKernelCancelMutex(gpr[4]);
		        break;
		    case 0x30a4:
		        Managers.mutex.sceKernelReferMutexStatus(gpr[4], gpr[5]);
		        break;
		    case 0x30c3:
		        Managers.mutex.sceKernelCreateLwMutex(gpr[4], gpr[5], gpr[6], gpr[7], gpr[8]);
		        break;
		    case 0x30c4:
		        Managers.mutex.sceKernelReferLwMutexStatusByID();
		        break;
		    case 0x30c5:
		        Managers.mutex.sceKernelDeleteLwMutex(gpr[4]);
		        break;
		    case 0x30c6:
		        Managers.mutex.sceKernelUnlockLwMutex(gpr[4], gpr[5]);
		        break;
		    case 0x30c7:
		        Managers.mutex.sceKernelLockLwMutexCB(gpr[4], gpr[5], gpr[6]);
		        break;
		    case 0x30c8:
		        Managers.mutex.sceKernelLockLwMutex(gpr[4], gpr[5], gpr[6]);
		        break;
		    case 0x30c9:
		        Managers.mutex.sceKernelReferLwMutexStatus(gpr[4], gpr[5]);
		        break;
		    case 0x30ca:
		        Managers.mutex.sceKernelTryLockLwMutex(gpr[4], gpr[5]);
		        break;
		    case 0x311f:
		        pspdisplay.getInstance().sceDisplayWaitVblankStartMulti();
		        break;

		    case 0xfffff: { // special code for unmapped imports
		        CpuState cpu = Emulator.getProcessor().cpu;
		        if(isEnableIgnoreUnmappedImports()) {
		            Modules.log.warn(String.format("IGNORING: Unmapped import @ 0x%08X - %08x %08x %08x",
		            cpu.pc, cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]));
		        }
		        else {
		        Modules.log.error(String.format("Unmapped import @ 0x%08X - %08x %08x %08x",
		            cpu.pc, cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]));
		        Emulator.PauseEmu();
		        }
		        break;
		    }

		    default:
		    {
		        // Try and handle as an HLE module export
		        boolean handled = HLEModuleManager.getInstance().handleSyscall(code);
		        if (!handled) {
		            CpuState cpu = Emulator.getProcessor().cpu;
		            cpu.gpr[2] = 0;

		            String params = String.format("%08x %08x %08x", cpu.gpr[4],
		                cpu.gpr[5], cpu.gpr[6]);

		            for (syscallsFirm15.calls c : syscallsFirm15.calls.values()) {
		                if (c.getSyscall() == code) {
		                    Modules.log.warn("Unsupported syscall " + Integer.toHexString(code) + " " + c + " " + params);
		                    return;
		                }
		            }
		            Modules.log.warn("Unsupported syscall " + Integer.toHexString(code) + " " + params);
		        }
		    }
		    break;
		}

        durationStatistics.end();
    }
}