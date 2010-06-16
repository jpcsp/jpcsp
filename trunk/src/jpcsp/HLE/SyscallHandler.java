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
import jpcsp.GeneralJpcspException;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.kernel.Managers;
import jpcsp.util.DurationStatistics;
import jpcsp.Allegrex.CpuState;

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

        try {
            switch(code) {
              case 0x2000:
            	  gpr[2] = Managers.intr.sceKernelRegisterSubIntrHandler(gpr[4], gpr[5], gpr[6], gpr[7]);
            	  break;
              case 0x2001:
            	  gpr[2] = Managers.intr.sceKernelReleaseSubIntrHandler(gpr[4], gpr[5]);
            	  break;
              case 0x2002:
            	  gpr[2] = Managers.intr.sceKernelEnableSubIntr(gpr[4], gpr[5]);
            	  break;
              case 0x2003:
            	  gpr[2] = Managers.intr.sceKernelDisableSubIntr(gpr[4], gpr[5]);
            	  break;
                //  case 0x2004: //sceKernelSuspendSubIntr
                //  case 0x2005: //sceKernelResumeSubIntr
                //  case 0x2006: //sceKernelIsSubInterruptOccurred
                //  case 0x2007: //QueryIntrHandlerInfo
                //  case 0x2008: //sceKernelRegisterUserSpaceIntrStack
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
                case 0x20b5:
                    psputils.getInstance().sceKernelDcacheInvalidateRange(gpr[4], gpr[5]);
                    break;
                case 0x20b6:
                    psputils.getInstance().sceKernelIcacheInvalidateRange(gpr[4], gpr[5]);
                    break;
                    // sceKernelUtilsMd5Digest(0x20b7),
                    // sceKernelUtilsMd5BlockInit(0x20b8),
                    // sceKernelUtilsMd5BlockUpdate(0x20b9),
                    // sceKernelUtilsMd5BlockResult(0x20ba),
                    // sceKernelUtilsSha1Digest(0x20bb),
                    // sceKernelUtilsSha1BlockInit(0x20bc),
                    // sceKernelUtilsSha1BlockUpdate(0x20bd),
                    // sceKernelUtilsSha1BlockResult(0x20be),
                case 0x20bf:
                    psputils.getInstance().sceKernelUtilsMt19937Init(gpr[4], gpr[5]);
                    break;
                case 0x20c0:
                    psputils.getInstance().sceKernelUtilsMt19937UInt(gpr[4]);
                    break;
                case 0x20c1:
                    psputils.getInstance().sceKernelGetGPI();
                    break;
                case 0x20c2:
                    psputils.getInstance().sceKernelSetGPO(gpr[4]);
                    break;
                case 0x20c3:
                    psputils.getInstance().sceKernelLibcClock();
                    break;
                case 0x20c4:
                    psputils.getInstance().sceKernelLibcTime(gpr[4]);
                    break;
                case 0x20c5:
                    psputils.getInstance().sceKernelLibcGettimeofday(gpr[4], gpr[5]);
                    break;
                case 0x20c6:
                    psputils.getInstance().sceKernelDcacheWritebackAll();
                    break;
                case 0x20c7:
                    psputils.getInstance().sceKernelDcacheWritebackInvalidateAll();
                    break;
                case 0x20c8:
                    psputils.getInstance().sceKernelDcacheWritebackRange(gpr[4], gpr[5]);
                    break;
                case 0x20c9:
                    psputils.getInstance().sceKernelDcacheWritebackInvalidateRange(gpr[4], gpr[5]);
                    break;
                    // sceKernelDcacheProbe(0x20ca),
                    // sceKernelDcacheReadTag(0x20cb),
                case 0x20cc:
                    psputils.getInstance().sceKernelIcacheInvalidateAll();
                    break;
                    // sceKernelIcacheProbe(0x20cd),
                    // sceKernelIcacheReadTag(0x20ce),
                case 0x20dc:
                    pspSysMem.getInstance().sceKernelMaxFreeMemSize();
                    break;
                case 0x20dd:
                    pspSysMem.getInstance().sceKernelTotalFreeMemSize();
                    break;
                case 0x20de:
                    pspSysMem.getInstance().sceKernelAllocPartitionMemory(gpr[4], gpr[5], gpr[6], gpr[7],gpr[8]);
                    break;
                case 0x20df:
                    pspSysMem.getInstance().sceKernelFreePartitionMemory(gpr[4]);
                    break;
                case 0x20e0:
                    pspSysMem.getInstance().sceKernelGetBlockHeadAddr(gpr[4]);
                    break;
                case 0x20e1:
                    pspSysMem.getInstance().sceKernelPrintf(gpr[4]);
                    break;
                case 0x20e2:
                    pspSysMem.getInstance().sceKernelDevkitVersion();
                    break;
                case 0x30e8:
                    pspSysMem.getInstance().sceKernelGetModel();
                    break;
                case 0x20e9:
                    LoadExec.getInstance().sceKernelLoadExec(gpr[4], gpr[5]);
                    break;
                    // sceKernelExitGameWithStatus(0x20ea),
                case 0x20eb:
                    LoadExec.getInstance().sceKernelExitGame();
                    break;
                case 0x20ec:
                    LoadExec.getInstance().sceKernelRegisterExitCallback(gpr[4]);
                    break;
                case 0x20ef:
                    pspge.getInstance().sceGeEdramGetSize();
                    break;
                case 0x20f0:
                    pspge.getInstance().sceGeEdramGetAddr();
                    break;
                case 0x20f1:
                    pspge.getInstance().sceGeEdramSetAddrTranslation(gpr[4]);
                    break;
                case 0x20f2:
                    pspge.getInstance().sceGeGetCmd(gpr[4]);
                    break;
                case 0x20f3:
                    pspge.getInstance().sceGeGetMtx(gpr[4], gpr[5]);
                    break;
                case 0x20f4:
                    pspge.getInstance().sceGeSaveContext(gpr[4]);
                    break;
                case 0x20f5:
                    pspge.getInstance().sceGeRestoreContext(gpr[4]);
                    break;
                case 0x20f6:
                    pspge.getInstance().sceGeListEnQueue(gpr[4], gpr[5], gpr[6], gpr[7]);
                    break;
                case 0x20f7:
                    pspge.getInstance().sceGeListEnQueueHead(gpr[4], gpr[5], gpr[6], gpr[7]);
                    break;
                case 0x20f8:
                    pspge.getInstance().sceGeListDeQueue(gpr[4]);
                    break;
                case 0x20f9:
                    pspge.getInstance().sceGeListUpdateStallAddr(gpr[4], gpr[5]);
                    break;
                case 0x20fa:
                    pspge.getInstance().sceGeListSync(gpr[4], gpr[5]);
                    break;
                case 0x20fb:
                    pspge.getInstance().sceGeDrawSync(gpr[4]);
                    break;
                case 0x20fc:
                	pspge.getInstance().sceGeBreak();
                	break;
                case 0x20fd:
                	pspge.getInstance().sceGeContinue();
                	break;
                case 0x20fe:
                    pspge.getInstance().sceGeSetCallback(gpr[4]);
                    break;
                case 0x20ff:
                    pspge.getInstance().sceGeUnsetCallback(gpr[4]);
                    break;
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
                case 0x30db:
                    pspSysMem.getInstance().SysMemUserForUser_FE707FDF(gpr[4], gpr[5], gpr[6], gpr[7]);
                    break;
                case 0x30dc:
                    pspSysMem.getInstance().SysMemUserForUser_50F61D8A(gpr[4]);
                    break;
                case 0x30de:
                    pspSysMem.getInstance().SysMemUserForUser_DB83A952(gpr[4], gpr[5]);
                    break;
                case 0x311e:
                    pspSysMem.getInstance().sceKernelMemset(gpr[4], gpr[5], gpr[5]);
                    break;
                case 0x311f:
                    pspdisplay.getInstance().sceDisplayWaitVblankStartMulti();
                    break;

                // special codes for HLE syscalls
                case 0x6f000:
                	Modules.ThreadManForUserModule.hleKernelExitThread();
                    break;
                case 0x6f001:
                	Modules.ThreadManForUserModule.hleKernelExitCallback();
                    break;
                case 0x6f002:
                	Modules.ThreadManForUserModule.hleKernelAsyncLoop();
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

                        for (jpcsp.Debugger.DisassemblerModule.syscallsFirm15.calls c : jpcsp.Debugger.DisassemblerModule.syscallsFirm15.calls.values()) {
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
        } catch(GeneralJpcspException e) {
        	Modules.log.error(e);
        }

        durationStatistics.end();
    }
}