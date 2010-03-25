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

	// Change this to return the number of cycles used?
    public static void syscall(int code) {
        int gpr[] = Emulator.getProcessor().cpu.gpr;
        ThreadMan.getInstance().clearSyscallFreeCycles();

        durationStatistics.start();

        // Some syscalls implementation throw GeneralJpcspException,
        // and Processor isn't setup to catch exceptions so we'll do it
        // here for now, or we could just stop throwing exceptions.
        // Also we need to decide whether to pass arguments to the functions,
        // or let them read the registers they want themselves.
        try {
            // Currently using FW1.50 codes
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
		//  case 0x2009: //_sceKernelReturnFromCallback
		//  case 0x200a: //sceKernelRegisterThreadEventHandler
		//  case 0x200b: //sceKernelReleaseThreadEventHandler
		//  case 0x200c: //sceKernelReferThreadEventHandlerStatus
                case 0x200d:
                    ThreadMan.getInstance().ThreadMan_sceKernelCreateCallback(gpr[4], gpr[5], gpr[6]);
                    break;
                case 0x200e:
                    ThreadMan.getInstance().ThreadMan_sceKernelDeleteCallback(gpr[4]);
                    break;
               // case 0x200f: //sceKernelNotifyCallback
              //  case 0x2010: //sceKernelCancelCallback
              //  case 0x2011: //sceKernelGetCallbackCount
                case 0x2012:
                    ThreadMan.getInstance().ThreadMan_sceKernelCheckCallback();
                    break;
                case 0x2013:
                    ThreadMan.getInstance().ThreadMan_sceKernelReferCallbackStatus(gpr[4], gpr[5]);
                    break;
                case 0x2014:
                    ThreadMan.getInstance().ThreadMan_sceKernelSleepThread();
                    break;
                case 0x2015:
                    ThreadMan.getInstance().ThreadMan_sceKernelSleepThreadCB();
                    break;
                case 0x2016:
                    ThreadMan.getInstance().ThreadMan_sceKernelWakeupThread(gpr[4]);
                    break;
                case 0x2017:
                	ThreadMan.getInstance().ThreadMan_sceKernelCancelWakeupThread(gpr[4]);
                	break;
                case 0x2018:
                    ThreadMan.getInstance().ThreadMan_sceKernelSuspendThread(gpr[4]);
                    break;
                case 0x2019:
                    ThreadMan.getInstance().ThreadMan_sceKernelResumeThread(gpr[4]);
                    break;
                case 0x201a:
                    ThreadMan.getInstance().ThreadMan_sceKernelWaitThreadEnd(gpr[4], gpr[5]);
                    break;
                case 0x201b:
                    ThreadMan.getInstance().ThreadMan_sceKernelWaitThreadEndCB(gpr[4], gpr[5]);
                    break;
                case 0x201c:
                    ThreadMan.getInstance().ThreadMan_sceKernelDelayThread(gpr[4]);
                    break;
                case 0x201d:
                    ThreadMan.getInstance().ThreadMan_sceKernelDelayThreadCB(gpr[4]);
                    break;
                case 0x201e:
                    ThreadMan.getInstance().ThreadMan_sceKernelDelaySysClockThread(gpr[4]);
                    break;
                case 0x201f:
                    ThreadMan.getInstance().ThreadMan_sceKernelDelaySysClockThreadCB(gpr[4]);
                    break;
                case 0x2020:
                    Managers.semas.sceKernelCreateSema(gpr[4], gpr[5], gpr[6], gpr[7], gpr[8]);
                    break;
                case 0x2021:
                    Managers.semas.sceKernelDeleteSema(gpr[4]);
                    break;
                case 0x2022:
                    Managers.semas.sceKernelSignalSema(gpr[4], gpr[5]);
                    break;
                case 0x2023:
                    Managers.semas.sceKernelWaitSema(gpr[4], gpr[5], gpr[6]);
                    break;
                case 0x2024:
                    Managers.semas.sceKernelWaitSemaCB(gpr[4], gpr[5], gpr[6]);
                    break;
                case 0x2025:
                    Managers.semas.sceKernelPollSema(gpr[4], gpr[5]);
                    break;
                case 0x2026:
                    Managers.semas.sceKernelCancelSema(gpr[4]); // not in pspsdk, params guessed
                    break;
                case 0x2027:
                    Managers.semas.sceKernelReferSemaStatus(gpr[4], gpr[5]);
                    break;

                case 0x2028:
                    Managers.eventFlags.sceKernelCreateEventFlag(gpr[4], gpr[5], gpr[6], gpr[7]);
                    break;
                case 0x2029:
                    Managers.eventFlags.sceKernelDeleteEventFlag(gpr[4]);
                    break;
                case 0x202a:
                    Managers.eventFlags.sceKernelSetEventFlag(gpr[4], gpr[5]);
                    break;
                case 0x202b:
                    Managers.eventFlags.sceKernelClearEventFlag(gpr[4], gpr[5]);
                    break;
                case 0x202c:
                    Managers.eventFlags.sceKernelWaitEventFlag(gpr[4], gpr[5], gpr[6], gpr[7], gpr[8]);
                    break;
                case 0x202d:
                    Managers.eventFlags.sceKernelWaitEventFlagCB(gpr[4], gpr[5], gpr[6], gpr[7], gpr[8]);
                    break;
                case 0x202e:
                    Managers.eventFlags.sceKernelPollEventFlag(gpr[4], gpr[5], gpr[6], gpr[7]);
                    break;
                case 0x202f:
                    Managers.eventFlags.sceKernelCancelEventFlag(gpr[4], gpr[5], gpr[6]); // not in pspsdk, params guessed
                    break;
                case 0x2030:
                    Managers.eventFlags.sceKernelReferEventFlagStatus(gpr[4], gpr[5]);
                    break;
		        case 0x2031:
                    Managers.mbx.sceKernelCreateMbx(gpr[4], gpr[5], gpr[6]);
                    break;
                case 0x2032:
                    Managers.mbx.sceKernelDeleteMbx(gpr[4]);
                    break;
                case 0x2033:
                    Managers.mbx.sceKernelSendMbx(gpr[4], gpr[5]);
                    break;
                case 0x2034:
                    Managers.mbx.sceKernelReceiveMbx(gpr[4], gpr[5], gpr[6]);
                    break;
                case 0x2035:
                    Managers.mbx.sceKernelReceiveMbxCB(gpr[4], gpr[5], gpr[6]);
                    break;
                case 0x2036:
                	Managers.mbx.sceKernelPollMbx(gpr[4], gpr[5]);
                	break;
                case 0x2037:
                    Managers.mbx.sceKernelCancelReceiveMbx(gpr[4], gpr[5]);
                    break;
                case 0x2038:
                    Managers.mbx.sceKernelReferMbxStatus(gpr[4], gpr[5]);
                    break;
                case 0x2039:
                    Managers.msgPipes.sceKernelCreateMsgPipe(gpr[4], gpr[5], gpr[6], gpr[7], gpr[8]);
                    break;
                case 0x203a:
                    Managers.msgPipes.sceKernelDeleteMsgPipe(gpr[4]);
                    break;
                case 0x203b:
                    Managers.msgPipes.sceKernelSendMsgPipe(gpr[4], gpr[5], gpr[6], gpr[7], gpr[8], gpr[9]);
                    break;
                case 0x203c:
                    Managers.msgPipes.sceKernelSendMsgPipeCB(gpr[4], gpr[5], gpr[6], gpr[7], gpr[8], gpr[9]);
                    break;
                case 0x203d:
                    Managers.msgPipes.sceKernelTrySendMsgPipe(gpr[4], gpr[5], gpr[6], gpr[7], gpr[8]);
                    break;
                case 0x203e:
                    Managers.msgPipes.sceKernelReceiveMsgPipe(gpr[4], gpr[5], gpr[6], gpr[7], gpr[8], gpr[9]);
                    break;
                case 0x203f:
                    Managers.msgPipes.sceKernelReceiveMsgPipeCB(gpr[4], gpr[5], gpr[6], gpr[7], gpr[8], gpr[9]);
                    break;
                case 0x2040:
                    Managers.msgPipes.sceKernelTryReceiveMsgPipe(gpr[4], gpr[5], gpr[6], gpr[7], gpr[8]);
                    break;
                case 0x2041:
                    Managers.msgPipes.sceKernelCancelMsgPipe(gpr[4], gpr[5], gpr[6]);
                    break;
                case 0x2042:
                    Managers.msgPipes.sceKernelReferMsgPipeStatus(gpr[4], gpr[5]);
                    break;
                case 0x2043:
                    Managers.vpl.sceKernelCreateVpl(gpr[4], gpr[5], gpr[6], gpr[7], gpr[8]);
                    break;
                case 0x2044:
                    Managers.vpl.sceKernelDeleteVpl(gpr[4]);
                    break;
                case 0x2045:
                    Managers.vpl.sceKernelAllocateVpl(gpr[4], gpr[5], gpr[6], gpr[7]);
                    break;
                case 0x2046:
                    Managers.vpl.sceKernelAllocateVplCB(gpr[4], gpr[5], gpr[6], gpr[7]);
                    break;
                case 0x2047:
                    Managers.vpl.sceKernelTryAllocateVpl(gpr[4], gpr[5], gpr[6]);
                    break;
                case 0x2048:
                    Managers.vpl.sceKernelFreeVpl(gpr[4], gpr[5]);
                    break;
                case 0x2049:
                    Managers.vpl.sceKernelCancelVpl(gpr[4], gpr[5]);
                    break;
                case 0x204a:
                    Managers.vpl.sceKernelReferVplStatus(gpr[4], gpr[5]);
                    break;
                case 0x204b:
                    Managers.fpl.sceKernelCreateFpl(gpr[4], gpr[5], gpr[6], gpr[7], gpr[8], gpr[9]);
                    break;
                case 0x204c:
                    Managers.fpl.sceKernelDeleteFpl(gpr[4]);
                    break;
                case 0x204d:
                    Managers.fpl.sceKernelAllocateFpl(gpr[4], gpr[5], gpr[6]);
                    break;
                case 0x204e:
                    Managers.fpl.sceKernelAllocateFplCB(gpr[4], gpr[5], gpr[6]);
                    break;
                case 0x204f:
                    Managers.fpl.sceKernelTryAllocateFpl(gpr[4], gpr[5]);
                    break;
                case 0x2050:
                    Managers.fpl.sceKernelFreeFpl(gpr[4], gpr[5]);
                    break;
                case 0x2051:
                    Managers.fpl.sceKernelCancelFpl(gpr[4], gpr[5]);
                    break;
                case 0x2052:
                    Managers.fpl.sceKernelReferFplStatus(gpr[4], gpr[5]);
                    break;
		// ThreadManForUser_0E927AED(0x2053),
                case 0x2054:
                    Managers.systime.sceKernelUSec2SysClock(gpr[4], gpr[5]);
                    break;
                case 0x2055:
                    Managers.systime.sceKernelUSec2SysClockWide(gpr[4]);
                    break;
                case 0x2056:
                    Managers.systime.sceKernelSysClock2USec(gpr[4], gpr[5], gpr[6]);
                    break;
                case 0x2057:
                    Managers.systime.sceKernelSysClock2USecWide(gpr[4], gpr[5], gpr[6], gpr[7]);
                    break;
                case 0x2058:
                    Managers.systime.sceKernelGetSystemTime(gpr[4]);
                    break;
                case 0x2059:
                    Managers.systime.sceKernelGetSystemTimeWide();
                    break;
                case 0x205a:
                    Managers.systime.sceKernelGetSystemTimeLow();
                    break;
		// sceKernelSetAlarm(0x205b),
		// sceKernelSetSysClockAlarm(0x205c),
		// sceKernelCancelAlarm(0x205d),
		// sceKernelReferAlarmStatus(0x205e),
		// sceKernelCreateVTimer(0x205f),
		// sceKernelDeleteVTimer(0x2060),
		// sceKernelGetVTimerBase(0x2061),
		// sceKernelGetVTimerBaseWide(0x2062),
		// sceKernelGetVTimerTime(0x2063),
		// sceKernelGetVTimerTimeWide(0x2064),
		// sceKernelSetVTimerTime(0x2065),
		// sceKernelSetVTimerTimeWide(0x2066),
		// sceKernelStartVTimer(0x2067),
		// sceKernelStopVTimer(0x2068),
		// sceKernelSetVTimerHandler(0x2069),
		// sceKernelSetVTimerHandlerWide(0x206a),
		// sceKernelCancelVTimerHandler(0x206b),
		// sceKernelReferVTimerStatus(0x206c),
                case 0x206d:
                    ThreadMan.getInstance().ThreadMan_sceKernelCreateThread(gpr[4], gpr[5], gpr[6], gpr[7], gpr[8], gpr[9]);
                    break;
                case 0x206e:
                    ThreadMan.getInstance().ThreadMan_sceKernelDeleteThread(gpr[4]);
                    break;
                case 0x206f:
                    ThreadMan.getInstance().ThreadMan_sceKernelStartThread(gpr[4], gpr[5], gpr[6]);
                    break;
                case 0x2070:
                case 0x2071:
                    ThreadMan.getInstance().ThreadMan_sceKernelExitThread(gpr[4]);
                    break;
                case 0x2072:
                    ThreadMan.getInstance().ThreadMan_sceKernelExitDeleteThread(gpr[4]);
                    break;
                case 0x2073:
                    ThreadMan.getInstance().ThreadMan_sceKernelTerminateThread(gpr[4]);
                    break;
                case 0x2074:
                    ThreadMan.getInstance().ThreadMan_sceKernelTerminateDeleteThread(gpr[4]);
                    break;
                case 0x2075:
                	ThreadMan.getInstance().ThreadMan_sceKernelSuspendDispatchThread();
                	break;
                case 0x2076:
                	ThreadMan.getInstance().ThreadMan_sceKernelResumeDispatchThread(gpr[4]);
                	break;
                case 0x2077:
                    ThreadMan.getInstance().ThreadMan_sceKernelChangeCurrentThreadAttr(gpr[4], gpr[5]);
                    break;
                case 0x2078:
                    ThreadMan.getInstance().ThreadMan_sceKernelChangeThreadPriority(gpr[4], gpr[5]);
                    break;
                case 0x2079:
                    ThreadMan.getInstance().ThreadMan_sceKernelRotateThreadReadyQueue(gpr[4]);
                    break;
                case 0x207a:
                	ThreadMan.getInstance().ThreadMan_sceKernelReleaseWaitThread(gpr[4]);
                	break;
                case 0x207b:
                    ThreadMan.getInstance().ThreadMan_sceKernelGetThreadId();
                    break;
                case 0x207c:
                    ThreadMan.getInstance().ThreadMan_sceKernelGetThreadCurrentPriority();
                    break;
                case 0x207d:
                    ThreadMan.getInstance().ThreadMan_sceKernelGetThreadExitStatus(gpr[4]);
                    break;
                case 0x207e:
                    ThreadMan.getInstance().ThreadMan_sceKernelCheckThreadStack();
                    break;
                case 0x207f:
                    ThreadMan.getInstance().ThreadMan_sceKernelGetThreadStackFreeSize(gpr[4]);
                    break;
                case 0x2080:
                    ThreadMan.getInstance().ThreadMan_sceKernelReferThreadStatus(gpr[4], gpr[5]);
                    break;
                // sceKernelReferThreadRunStatus(0x2081),
                case 0x2082:
                    ThreadMan.getInstance().ThreadMan_sceKernelReferSystemStatus(gpr[4]);
                    break;
                case 0x2083:
                    ThreadMan.getInstance().ThreadMan_sceKernelGetThreadmanIdList(gpr[4], gpr[5], gpr[6], gpr[7]);
                    break;
		// sceKernelGetThreadmanIdType(0x2084),
		// sceKernelReferThreadProfiler(0x2085),
		// sceKernelReferGlobalProfiler(0x2086),
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
		// sceKernelIcacheInvalidateRange(0x20b6),
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
		// sceKernelIcacheInvalidateAll(0x20cc),
		// sceKernelIcacheProbe(0x20cd),
		// sceKernelIcacheReadTag(0x20ce),
		// sceKernelLoadModule(0x20cf),
		// sceKernelLoadModuleByID(0x20d0),
		// sceKernelLoadModuleMs(0x20d1),
		// sceKernelLoadModuleBufferUsbWlan(0x20d2),
		// sceKernelStartModule(0x20d3),
		// sceKernelStopModule(0x20d4),
		// sceKernelUnloadModule(0x20d5),
		// sceKernelSelfStopUnloadModule(0x20d6),
		// sceKernelStopUnloadSelfModule(0x20d7),
		// sceKernelGetModuleIdList(0x20d8),
		// sceKernelQueryModuleInfo(0x20d9),
		// ModuleMgrForUser_F0A26395(0x20da),
		// ModuleMgrForUser_D8B73127(0x20db),
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
		// sceKernelPowerLock(0x20e3),
		// sceKernelPowerUnlock(0x20e4),
		// sceKernelPowerTick(0x20e5),
		// sceSuspendForUser_3E0271D3(0x20e6),
		// sceSuspendForUser_A14F40B2(0x20e7),
		// sceSuspendForUser_A569E425(0x20e8),
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
              // sceDmacMemcpy(0x20ed),
		// sceDmacTryMemcpy(0x20ee),
               case 0x20ef:
            	   pspge.getInstance().sceGeEdramGetSize();
                   break;
               case 0x20f0:
                    pspge.getInstance().sceGeEdramGetAddr();
                    break;
                //sceGeEdramSetAddrTranslation(0x20f1),
		// sceGeGetCmd(0x20f2),
		// sceGeGetMtx(0x20f3),
                case 0x20f4:
            	   pspge.getInstance().sceGeSaveContext(gpr[4]);
            	   break;
                case 0x20f5:
            	   pspge.getInstance().sceGeRestoreContext(gpr[4]);
            	   break;
                case 0x20f6:
                    pspge.getInstance().sceGeListEnQueue(gpr[4], gpr[5], gpr[6], gpr[7]);
                    break;
               // sceGeListEnQueueHead(0x20f7),
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
               /* case 0x2100:
                    psprtc.getInstance().sceRtcGetTickResolution();
                    break;
                case 0x2101:
                    psprtc.getInstance().sceRtcGetCurrentTick(gpr[4]);
                    break;
		// sceRtc_011F03C1(0x2102),
		// sceRtc_029CA3B3(0x2103),
		// sceRtcGetCurrentClock(0x2104),
                case 0x2105:
                    psprtc.getInstance().sceRtcGetCurrentClockLocalTime(gpr[4]);
                    break;*/
		// sceRtcConvertUtcToLocalTime(0x2106),
		// sceRtcConvertLocalTimeToUTC(0x2107),
		// sceRtcIsLeapYear(0x2108),
		// sceRtcGetDaysInMonth(0x2109),
		// sceRtcGetDayOfWeek(0x210a),
		// sceRtcCheckValid(0x210b),
		// sceRtcSetTime_t(0x210c),
		// sceRtcGetTime_t(0x210d),
		// sceRtcSetDosTime(0x210e),
		// sceRtcGetDosTime(0x210f),
		// sceRtcSetWin32FileTime(0x2110),
		// sceRtcGetWin32FileTime(0x2111),
		// sceRtcSetTick(0x2112),
		// sceRtcGetTick(0x2113),
		// sceRtcCompareTick(0x2114),
		// sceRtcTickAddTicks(0x2115),
		// sceRtcTickAddMicroseconds(0x2116),
		// sceRtcTickAddSeconds(0x2117),
		// sceRtcTickAddMinutes(0x2118),
		// sceRtcTickAddHours(0x2119),
		// sceRtcTickAddDays(0x211a),
		// sceRtcTickAddWeeks(0x211b),
		// sceRtcTickAddMonths(0x211c),
		// sceRtcTickAddYears(0x211d),
		// sceRtcFormatRFC2822(0x211e),
		// sceRtcFormatRFC2822LocalTime(0x211f),
		// sceRtcFormatRFC3339(0x2120),
		// sceRtcFormatRFC3339LocalTime(0x2121),
		// sceRtcParseDateTime(0x2122),
		// sceRtcParseRFC3339(0x2123),
                    /*
		case 0x2124: pspAudio.getInstance().sceAudioOutput(gpr[4], gpr[5], gpr[6]); break;
		case 0x2125: pspAudio.getInstance().sceAudioOutputBlocking(gpr[4], gpr[5], gpr[6]); break;
		case 0x2126: pspAudio.getInstance().sceAudioOutputPanned(gpr[4], gpr[5], gpr[6], gpr[7]); break;
                case 0x2127:
                    pspAudio.getInstance().sceAudioOutputPannedBlocking(gpr[4], gpr[5], gpr[6], gpr[7]);
                    break;
                case 0x2128:
                    pspAudio.getInstance().sceAudioChReserve(gpr[4], gpr[5], gpr[6]);
                    break;
		//case 0x2129: pspAudio.getInstance().sceAudioOneshotOutput(); break;
		case 0x212a: pspAudio.getInstance().sceAudioChRelease(gpr[4]); break;
		case 0x212b: pspAudio.getInstance().sceAudioGetChannelRestLength(gpr[4]); break;
		case 0x212c: pspAudio.getInstance().sceAudioSetChannelDataLen(gpr[4], gpr[5]); break;
		//case 0x212d: pspAudio.getInstance().sceAudioChangeChannelConfig(gpr[4], gpr[5]); break;
		case 0x212e: pspAudio.getInstance().sceAudioChangeChannelVolume(gpr[4], gpr[5], gpr[6]); break;
		//case 0x212f: pspAudio.getInstance().sceAudio_38553111(); break;
		//case 0x2130: pspAudio.getInstance().sceAudio_5C37C0AE(); break;
		//case 0x2131: pspAudio.getInstance().sceAudio_E0727056(); break;
		//case 0x2132: pspAudio.getInstance().sceAudioInputBlocking(gpr[4], gpr[5], gpr[6]); break;
		//case 0x2133: pspAudio.getInstance().sceAudioInput(gpr[4], gpr[5], gpr[6]); break;
		//case 0x2134: pspAudio.getInstance().sceAudioGetInputLength(); break;
		//case 0x2135: pspAudio.getInstance().sceAudioWaitInputEnd(); break;
		//case 0x2136: pspAudio.getInstance().sceAudioInputInit(gpr[4], gpr[5], gpr[6]); break;
		//case 0x2137: pspAudio.getInstance().sceAudio_E926D3FB(); break;
		//case 0x2138: pspAudio.getInstance().sceAudio_A633048E(); break;
		case 0x2139: pspAudio.getInstance().sceAudioGetChannelRestLen(gpr[4]); break;

*/
                case 0x213a:
                    pspdisplay.getInstance().sceDisplaySetMode(gpr[4], gpr[5], gpr[6]);
                    break;
                case 0x213b:
                    pspdisplay.getInstance().sceDisplayGetMode(gpr[4], gpr[5], gpr[6]);
                    break;
                case 0x213c:
                    pspdisplay.getInstance().sceDisplayGetFramePerSec();
                    break;
		// sceDisplaySetHoldMode(0x213d),
		// sceDisplaySetResumeMode(0x213e),
                case 0x213f:
                    pspdisplay.getInstance().sceDisplaySetFrameBuf(gpr[4], gpr[5], gpr[6], gpr[7]);
                    break;
                case 0x2140:
                    pspdisplay.getInstance().sceDisplayGetFrameBuf(gpr[4], gpr[5], gpr[6], gpr[7]);
                    break;
		// sceDisplayIsForeground(0x2141),
		// sceDisplayGetBrightness(0x2142),
		// sceDisplayGetVcount(0x2143),
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
		// sceDisplay_A83EF139(0x214b),
            /*    case 0x214c:
                    pspctrl.getInstance().sceCtrlSetSamplingCycle(gpr[4]);
                    break;
                case 0x214d:
                    pspctrl.getInstance().sceCtrlGetSamplingCycle(gpr[4]);
                    break;
                case 0x214e:
                    pspctrl.getInstance().sceCtrlSetSamplingMode(gpr[4]);
                    break;
                case 0x214f:
                    pspctrl.getInstance().sceCtrlGetSamplingMode(gpr[4]);
                    break;
                case 0x2150:
                    pspctrl.getInstance().sceCtrlPeekBufferPositive(gpr[4], gpr[5]);
                    break;
                case 0x2151:
                    pspctrl.getInstance().sceCtrlPeekBufferNegative(gpr[4], gpr[5]);
                    break;
                case 0x2152:
                    pspctrl.getInstance().sceCtrlReadBufferPositive(gpr[4], gpr[5]);
                    break;
                case 0x2153:
                    pspctrl.getInstance().sceCtrlReadBufferNegative(gpr[4], gpr[5]);
                    break;
                case 0x2154:
                    pspctrl.getInstance().sceCtrlPeekLatch(gpr[4]);
                    break;
                case 0x2155:
                    pspctrl.getInstance().sceCtrlReadLatch(gpr[4]);
                    break;*/
             //   sceCtrl_A7144800(0x2156),
		// sceCtrl_687660FA(0x2157),
		// sceCtrl_348D99D4(0x2158),
		// sceCtrl_AF5960F3(0x2159),
		// sceCtrl_A68FD260(0x215a),
		// sceCtrl_6841BE1A(0x215b),
	//	 sceHprmRegisterCallback(0x215c),
	//	 sceHprmUnregisterCallback(0x215d),
	//	 sceHprm_71B5FB67(0x215e),
	//	 sceHprmIsRemoteExist(0x215f),
	//	 sceHprmIsHeadphoneExist(0x2160),
	//	 sceHprmIsMicrophoneExist(0x2161),
	//	 sceHprmPeekCurrentKey(0x2162),
	//	 sceHprmPeekLatch(0x2163),
	//	 sceHprmReadLatch(0x2164),
	//	 scePower_2B51FE2F(0x2165),
	//	 scePower_442BFBAC(0x2166),
	///	 scePowerTick(0x2167),
	//	 scePowerGetIdleTimer(0x2168),
	//	 scePowerIdleTimerEnable(0x2169),
	//	 scePowerIdleTimerDisable(0x216a),
	//	 scePowerBatteryUpdateInfo(0x216b),
	//	 scePower_E8E4E204(0x216c),
	//	 scePowerGetLowBatteryCapacity(0x216d),
	//	 scePowerIsPowerOnline(0x216e),
	//	 scePowerIsBatteryExist(0x216f),
	//	 scePowerIsBatteryCharging(0x2170),
	//	 scePowerGetBatteryChargingStatus(0x2171),
	//	 scePowerIsLowBattery(0x2172),
	//	 scePower_78A1A796(0x2173),
	//	 scePowerGetBatteryRemainCapacity(0x2174),
	//	 scePower_FD18A0FF(0x2175),
	////	 scePowerGetBatteryLifePercent(0x2176),
	//	 scePowerGetBatteryLifeTime(0x2177),
	////	 scePowerGetBatteryTemp(0x2178),
	//	 scePowerGetBatteryElec(0x2179),
	//	 scePowerGetBatteryVolt(0x217a),
	//	 scePower_23436A4A(0x217b),
	//	 scePower_0CD21B1F(0x217c),
	//	 scePower_165CE085(0x217d),
	//	 scePower_23C31FFE(0x217e),
	//	 scePower_FA97A599(0x217f),
	//	 scePower_B3EDD801(0x2180),
	//	 scePowerLock(0x2181),
	//	 scePowerUnlock(0x2182),
	//	 scePowerCancelRequest(0x2183),
	//	 scePowerIsRequest(0x2184),
	//	 scePowerRequestStandby(0x2185),
	//	 scePowerRequestSuspend(0x2186),
	//	 scePower_2875994B(0x2187),
	//	 scePowerEncodeUBattery(0x2188),
	//	 scePowerGetResumeCount(0x2189),
	//	 scePowerRegisterCallback(0x218a),
	//	 scePowerUnregisterCallback(0x218b),
	//	 scePowerUnregitserCallback(0x218c),
	//	 scePowerSetCpuClockFrequency(0x218d),
	///	 scePowerSetBusClockFrequency(0x218e),
	//	 scePowerGetCpuClockFrequency(0x218f),
	//	 scePowerGetBusClockFrequency(0x2190),
	//	 scePowerGetCpuClockFrequencyInt(0x2191),
	//	 scePowerGetBusClockFrequencyInt(0x2192),
	//	 scePower_34F9C463(0x2193),
	//	 scePowerGetCpuClockFrequencyFloat(0x2194),
	//	 scePowerGetBusClockFrequencyFloat(0x2195),
	//	 scePower_EA382A27(0x2196),
	//	 scePowerSetClockFrequency(0x2197),
	//	 sceUsbStart(0x2198),
	//	 sceUsbStop(0x2199),
	//	 sceUsbGetState(0x219a),
	//	 sceUsbGetDrvList(0x219b),
	//	 sceUsbGetDrvState(0x219c),
	//	 sceUsbActivate(0x219d),
	//	 sceUsbDeactivate(0x219e),
	//	 sceUsbWaitState(0x219f),
	//	 sceUsbWaitCancel(0x21a0),
	//	 sceOpenPSIDGetOpenPSID(0x21a1),
	//	 sceSircsSend(0x21a2),
	//	 sceUmdCheckMedium(0x21a3),
	//	 sceUmdActivate(0x21a4),
	//	 sceUmdDeactivate(0x21a5),
	//	 sceUmdWaitDriveStat(0x21a6),
	//	 sceUmdWaitDriveStatWithTimer(0x21a7),
	//	 sceUmdWaitDriveStatCB(0x21a8),
	//	 sceUmdCancelWaitDriveStat(0x21a9),
	//	 sceUmdGetDriveStat(0x21aa),
	//	 sceUmdGetErrorStat(0x21ab),
	//	 sceUmdGetDiscInfo(0x21ac),
	//	 sceUmdRegisterUMDCallBack(0x21ad),
	//	 sceUmdUnRegisterUMDCallBack(0x21ae),
	//	 sceWlanDevIsPowerOn(0x21af),
	//	 sceWlanGetSwitchState(0x21b0),
	//	 sceWlanGetEtherAddr(0x21b1),
	//	 sceWlanDevAttach(0x21b2),
	//	 sceWlanDevDetach(0x21b3),
	//	 sceWlanDrv_lib_19E51F54(0x21b4),
	//	 sceWlanDevIsGameMode(0x21b5),
	//	 sceWlanGPPrevEstablishActive(0x21b6),
	//	 sceWlanGPSend(0x21b7),
	//	 sceWlanGPRecv(0x21b8),
	//	 sceWlanGPRegisterCallback(0x21b9),
	//	 sceWlanGPUnRegisterCallback(0x21ba),
	//	 sceWlanDrv_lib_81579D36(0x21bb),
	//	 sceWlanDrv_lib_5BAA1FE5(0x21bc),
	//	 sceWlanDrv_lib_4C14BACA(0x21bd),
	//	 sceWlanDrv_lib_2D0FAE4E(0x21be),
	//	 sceWlanDrv_lib_56F467CA(0x21bf),
	//	 sceWlanDrv_lib_FE8A0B46(0x21c0),
	//	 sceWlanDrv_lib_40B0AA4A(0x21c1),
	//	 sceWlanDevSetGPIO(0x21c2),
	//	 sceWlanDevGetStateGPIO(0x21c3),
	//	 sceWlanDrv_lib_8D5F551B(0x21c4),
	//	 sceVaudioOutputBlocking(0x21c5),
	//	 sceVaudioChReserve(0x21c6),
	//	 sceVaudioChRelease(0x21c7),
	//	 sceVaudio_346FBE94(0x21c8),
	//	 sceRegExit(0x21c9),
	//	 sceRegOpenRegistry(0x21ca),
	//	 sceRegCloseRegistry(0x21cb),
	//	 sceRegRemoveRegistry(0x21cc),
	//	 sceReg_1D8A762E(0x21cd),
	//	 sceReg_0CAE832B(0x21ce),
	//	 sceRegFlushRegistry(0x21cf),
	//	 sceReg_0D69BF40(0x21d0),
	//	 sceRegCreateKey(0x21d1),
	//	 sceRegSetKeyValue(0x21d2),
	//	 sceRegGetKeyInfo(0x21d3),
	//	 sceRegGetKeyValue(0x21d4),
	//	 sceRegGetKeysNum(0x21d5),
	//	 sceRegGetKeys(0x21d6),
	//	 sceReg_4CA16893(0x21d7),
		// sceRegRemoveKey(0x21d8),
		// sceRegKickBackDiscover(0x21d9),
		// sceRegGetKeyValueByName(0x21da),
		// sceUtilityGameSharingInitStart(0x21db),
		// sceUtilityGameSharingShutdownStart(0x21dc),
		// sceUtilityGameSharingUpdate(0x21dd),
		// sceUtilityGameSharingGetStatus(0x21de),
		// sceNetplayDialogInitStart(0x21df),
		// sceNetplayDialogShutdownStart(0x21e0),
		// sceNetplayDialogUpdate(0x21e1),
		// sceNetplayDialogGetStatus(0x21e2),
		// sceUtilityNetconfInitStart(0x21e3),
		// sceUtilityNetconfShutdownStart(0x21e4),
		// sceUtilityNetconfUpdate(0x21e5),
		// sceUtilityNetconfGetStatus(0x21e6),
		// sceUtilitySavedataInitStart(0x21e7),
		// sceUtilitySavedataShutdownStart(0x21e8),
		// sceUtilitySavedataUpdate(0x21e9),
		// sceUtilitySavedataGetStatus(0x21ea),
		// sceUtility_2995D020(0x21eb),
		// sceUtility_B62A4061(0x21ec),
		// sceUtility_ED0FAD38(0x21ed),
		// sceUtility_88BC7406(0x21ee),
		// sceUtilityMsgDialogInitStart(0x21ef),
		// sceUtilityMsgDialogShutdownStart(0x21f0),
		// sceUtilityMsgDialogUpdate(0x21f1),
		// sceUtilityMsgDialogGetStatus(0x21f2),
		// sceUtilityOskInitStart(0x21f3),
		// sceUtilityOskShutdownStart(0x21f4),
		// sceUtilityOskUpdate(0x21f5),
		// sceUtilityOskGetStatus(0x21f6),
		// sceUtilitySetSystemParamInt(0x21f7),
		// sceUtilitySetSystemParamString(0x21f8),
		// sceUtilityGetSystemParamInt(0x21f9),
		// sceUtilityGetSystemParamString(0x21fa),
		// sceUtilityCheckNetParam(0x21fb),
		// sceUtilityGetNetParam(0x21fc),
		// sceUtility_private_17CB4D96(0x21fd),
		// sceUtility_private_EE7AC503(0x21fe),
		// sceUtility_private_5FF96ED3(0x21ff),
		// sceUtility_private_9C9DD5BC(0x2200),
		// sceUtility_private_4405BA38(0x2201),
		// sceUtility_private_1DFA62EF(0x2202),
		// sceUtilityDialogSetStatus(0x2203),
		// sceUtilityDialogGetType(0x2204),
		// sceUtilityDialogGetParam(0x2205),
		// sceUtility_private_EF5BC2D1(0x2206),
		// sceUtilityDialogGetSpeed(0x2207),
		// sceUtility_private_19461966(0x2208),
		// sceUtilityDialogSetThreadId(0x2209),
		// sceUtilityDialogLoadModule(0x220a),
		// sceUtilityDialogPowerLock(0x220b),
		// sceUtilityDialogPowerUnlock(0x220c),
		// sceUtilityCreateNetParam(0x220d),
		 //sceUtilityDeleteNetParam(0x220e),
		 //sceUtilityCopyNetParam(0x220f),
		// sceUtilitySetNetParam(0x2210);

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

                // special codes for HLE syscalls
                case 0x6f000:
                	ThreadMan.getInstance().hleKernelExitThread();
                    break;
                case 0x6f001:
                	ThreadMan.getInstance().hleKernelExitCallback();
                    break;
                case 0x6f002:
                    ThreadMan.getInstance().hleKernelAsyncLoop();
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
                        // At least set a decent return value
                        cpu.gpr[2] = 0;
                        //Emulator.getProcessor().gpr[2] = 0xb515ca11;

                        // Display debug info
                        String params = String.format("%08x %08x %08x", cpu.gpr[4],
                            cpu.gpr[5], cpu.gpr[6]);

                        // TODO replace this enum with a dynamically generated hashmap, this way we can avoid numbering mistakes
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
