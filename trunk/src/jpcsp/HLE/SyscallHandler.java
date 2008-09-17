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

public class SyscallHandler {

    // Change this to return the number of cycles used?
    public static void syscall(int code) {
        int gpr[] = Emulator.getProcessor().gpr;

        // Some syscalls implementation throw GeneralJpcspException,
        // and Processor isn't setup to catch exceptions so we'll do it
        // here for now, or we could just stop throwing exceptions.
        // Also we need to decide whether to pass arguments to the functions,
        // or let them read the registers they want themselves.
        try {
            // Currently using FW1.50 codes
            switch(code) {
              //  case 0x2000: //sceKernelRegisterSubIntrHandler
              //  case 0x2001: // sceKernelReleaseSubIntrHandler
		//  case 0x2002: //sceKernelEnableSubIntr
		//  case 0x2003: //sceKernelDisableSubIntr
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
                    ThreadMan.get_instance().ThreadMan_sceKernelCreateCallback(gpr[4], gpr[5], gpr[6]);
                    break;
               // case 0x200e: //sceKernelDeleteCallback
               // case 0x200f: //sceKernelNotifyCallback
              //  case 0x2010: //sceKernelCancelCallback
              //  case 0x2011: //sceKernelGetCallbackCount
              //  case 0x2012: //sceKernelCheckCallback
              //  case 0x2013: //sceKernelReferCallbackStatus
                case 0x2014:
                    ThreadMan.get_instance().ThreadMan_sceKernelSleepThread();
                    break;
                case 0x2015:
                    ThreadMan.get_instance().ThreadMan_sceKernelSleepThreadCB();
                    break;
                //case 0x2016: //sceKernelWakeupThread
		//case 0x2017: ///sceKernelCancelWakeupThread
		//case 0x2018: //sceKernelSuspendThread
		//case 0x2019: //sceKernelResumeThread
		//case 0x201a: //sceKernelWaitThreadEnd
		//case 0x201b: //sceKernelWaitThreadEndCB
                case 0x201c:
                    ThreadMan.get_instance().ThreadMan_sceKernelDelayThread(gpr[4]);
                    break;
                 //sceKernelDelayThreadCB(0x201d),
		 //sceKernelDelaySysClockThread(0x201e),
		// sceKernelDelaySysClockThreadCB(0x201f),
		// sceKernelCreateSema(0x2020),
		// sceKernelDeleteSema(0x2021),
		 //sceKernelSignalSema(0x2022),
		 //sceKernelWaitSema(0x2023),
		 //sceKernelWaitSemaCB(0x2024),
		// sceKernelPollSema(0x2025),
		 //sceKernelCancelSema(0x2026),
		 //sceKernelReferSemaStatus(0x2027),
		 //sceKernelCreateEventFlag(0x2028),
		// sceKernelDeleteEventFlag(0x2029),
		// sceKernelSetEventFlag(0x202a),
		// sceKernelClearEventFlag(0x202b),
		// sceKernelWaitEventFlag(0x202c),
		// sceKernelWaitEventFlagCB(0x202d),
		// sceKernelPollEventFlag(0x202e),
		// sceKernelCancelEventFlag(0x202f),
		// sceKernelReferEventFlagStatus(0x2030),
		// sceKernelCreateMbx(0x2031),
		// sceKernelDeleteMbx(0x2032),
		// sceKernelSendMbx(0x2033),
		// sceKernelReceiveMbx(0x2034),
		// sceKernelReceiveMbxCB(0x2035),
		// sceKernelPollMbx(0x2036),
		// sceKernelCancelReceiveMbx(0x2037),
		// sceKernelReferMbxStatus(0x2038),
		// sceKernelCreateMsgPipe(0x2039),
		// sceKernelDeleteMsgPipe(0x203a),
		// sceKernelSendMsgPipe(0x203b),
		// sceKernelSendMsgPipeCB(0x203c),
		// sceKernelTrySendMsgPipe(0x203d),
		// sceKernelReceiveMsgPipe(0x203e),
		// sceKernelReceiveMsgPipeCB(0x203f),
		// sceKernelTryReceiveMsgPipe(0x2040),
		// sceKernelCancelMsgPipe(0x2041),
		// sceKernelReferMsgPipeStatus(0x2042),
		// sceKernelCreateVpl(0x2043),
		// sceKernelDeleteVpl(0x2044),
		// sceKernelAllocateVpl(0x2045),
		// sceKernelAllocateVplCB(0x2046),
		// sceKernelTryAllocateVpl(0x2047),
		// sceKernelFreeVpl(0x2048),
		// sceKernelCancelVpl(0x2049),
		// sceKernelReferVplStatus(0x204a),
		// sceKernelCreateFpl(0x204b),
		// sceKernelDeleteFpl(0x204c),
		// sceKernelAllocateFpl(0x204d),
		// sceKernelAllocateFplCB(0x204e),
		// sceKernelTryAllocateFpl(0x204f),
		// sceKernelFreeFpl(0x2050),
		// sceKernelCancelFpl(0x2051),
		// sceKernelReferFplStatus(0x2052),
		// ThreadManForUser_0E927AED(0x2053),
		// sceKernelUSec2SysClock(0x2054),
		// sceKernelUSec2SysClockWide(0x2055),
		// sceKernelSysClock2USec(0x2056),
		// sceKernelSysClock2USecWide(0x2057),
		// sceKernelGetSystemTime(0x2058),
		// sceKernelGetSystemTimeWide(0x2059),
		// sceKernelGetSystemTimeLow(0x205a),
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
                    ThreadMan.get_instance().ThreadMan_sceKernelCreateThread(gpr[4], gpr[5], gpr[6], gpr[7], gpr[8], gpr[9]);
                    break;
                case 0x206e:
                    ThreadMan.get_instance().ThreadMan_sceKernelDeleteThread(gpr[4]);
                    break;
                case 0x206f:
                    ThreadMan.get_instance().ThreadMan_sceKernelStartThread(gpr[4], gpr[5], gpr[6]);
                    break;
                case 0x2070:
                case 0x2071:
                    ThreadMan.get_instance().ThreadMan_sceKernelExitThread(gpr[4]);
                    break;
                case 0x2072:
                    ThreadMan.get_instance().ThreadMan_sceKernelExitDeleteThread(gpr[4]);
                    break;
                case 0x2073:
                    ThreadMan.get_instance().ThreadMan_sceKernelTerminateThread(gpr[4]);
                    break;
                 //sceKernelTerminateDeleteThread(0x2074),
		// sceKernelSuspendDispatchThread(0x2075),
		 //sceKernelResumeDispatchThread(0x2076),
		// sceKernelChangeCurrentThreadAttr(0x2077),
		// sceKernelChangeThreadPriority(0x2078),
		// sceKernelRotateThreadReadyQueue(0x2079),
		// sceKernelReleaseWaitThread(0x207a),
                case 0x207b:
                    ThreadMan.get_instance().ThreadMan_sceKernelGetThreadId();
                    break;
                // sceKernelGetThreadCurrentPriority(0x207c),
		// sceKernelGetThreadExitStatus(0x207d),
		// sceKernelCheckThreadStack(0x207e),
		// sceKernelGetThreadStackFreeSize(0x207f),
                case 0x2080:
                    ThreadMan.get_instance().ThreadMan_sceKernelReferThreadStatus(gpr[4], gpr[5]);
                    break;
                // sceKernelReferThreadRunStatus(0x2081),
		// sceKernelReferSystemStatus(0x2082),
		// sceKernelGetThreadmanIdList(0x2083),
		// sceKernelGetThreadmanIdType(0x2084),
		// sceKernelReferThreadProfiler(0x2085),
		// sceKernelReferGlobalProfiler(0x2086),
		// sceIoPollAsync(0x2087),
		// sceIoWaitAsync(0x2088),
		// sceIoWaitAsyncCB(0x2089),
		// sceIoGetAsyncStat(0x208a),
		// sceIoChangeAsyncPriority(0x208b),
		// sceIoSetAsyncCallback(0x208c),
                case 0x208d:// sceIoClose
                    pspiofilemgr.get_instance().sceIoClose(gpr[4]);
                    break;
		// sceIoCloseAsync(0x208e),
                case 0x208f:// sceIoOpen
                    pspiofilemgr.get_instance().sceIoOpen(gpr[4], gpr[5], gpr[6]);
                    break;
		// sceIoOpenAsync(0x2090),
                case 0x2091: //sceIoRead
                    pspiofilemgr.get_instance().sceIoRead(gpr[4], gpr[5], gpr[6]);
                    break;
		// sceIoReadAsync(0x2092),
                case 0x2093:// sceIoWrite
                    pspiofilemgr.get_instance().sceIoWrite(gpr[4], gpr[5], gpr[6]);
                    break;
		// sceIoWriteAsync(0x2094),
		// sceIoLseek(0x2095),
		// sceIoLseekAsync(0x2096),
                case 0x2097:
                    pspiofilemgr.get_instance().sceIoLseek32(gpr[4], gpr[5], gpr[6]);
                    break;
		// sceIoLseek32Async(0x2098),
		// sceIoIoctl(0x2099),
		// sceIoIoctlAsync(0x209a),
            case 0x209b://sceIoDopen
                    pspiofilemgr.get_instance().sceIoDopen(gpr[4]);
                    break;
            case 0x209c:
                    pspiofilemgr.get_instance().sceIoDread(gpr[4], gpr[5]);
                    break;
            case 0x209d:
                    pspiofilemgr.get_instance().sceIoDclose(gpr[4]);
                    break;
		// sceIoRemove(0x209e),
            case 0x209f:
                    pspiofilemgr.get_instance().sceIoMkdir(gpr[4], gpr[5]);
                    break;
		// sceIoRmdir(0x20a0),
                case 0x20a1: //sceChDir
                    pspiofilemgr.get_instance().sceIoChdir(gpr[4]);
                    break;
		// sceIoSync(0x20a2),
                case 0x20a3:
                    pspiofilemgr.get_instance().sceIoGetstat(gpr[4], gpr[5]);
                    break;
		// sceIoChstat(0x20a4),
		// sceIoRename(0x20a5),
		// sceIoDevctl(0x20a6),
		// sceIoGetDevType(0x20a7),
		// sceIoAssign(0x20a8),
		// sceIoUnassign(0x20a9),
		// sceIoCancel(0x20aa),
		// IoFileMgrForUser_5C2BE2CC(0x20ab),
		// sceKernelStdioRead(0x20ac),
		// sceKernelStdioLseek(0x20ad),
		// sceKernelStdioSendChar(0x20ae),
		// sceKernelStdioWrite(0x20af),
		// sceKernelStdioClose(0x20b0),
		 //sceKernelStdioOpen(0x20b1),
                case 0x20b2:
                    pspstdio.get_instance().sceKernelStdin();
                    break;
                case 0x20b3:
                    pspstdio.get_instance().sceKernelStdout();
                    break;
                case 0x20b4:
                    pspstdio.get_instance().sceKernelStderr();
                    break;
                case 0x20b5:
                    psputils.get_instance().sceKernelDcacheInvalidateRange(gpr[4], gpr[5]);
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
                    psputils.get_instance().sceKernelUtilsMt19937Init(gpr[4], gpr[5]);
                    break;
                case 0x20c0:
                    psputils.get_instance().sceKernelUtilsMt19937UInt(gpr[4]);
                    break;
		// sceKernelGetGPI(0x20c1),
		// sceKernelSetGPO(0x20c2),
                case 0x20c3:
                    psputils.get_instance().sceKernelLibcClock();
                    break;
                case 0x20c4:
                    psputils.get_instance().sceKernelLibcTime(gpr[4]);
                    break;
		// sceKernelLibcGettimeofday(0x20c5),
                case 0x20c6:
                    psputils.get_instance().sceKernelDcacheWritebackAll();
                    break;
                case 0x20c7:
                    psputils.get_instance().sceKernelDcacheWritebackInvalidateAll();
                    break;
                case 0x20c8:
                    psputils.get_instance().sceKernelDcacheWritebackRange(gpr[4], gpr[5]);
                    break;
                case 0x20c9:
                    psputils.get_instance().sceKernelDcacheWritebackInvalidateRange(gpr[4], gpr[5]);
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
                    pspSysMem.get_instance().sceKernelMaxFreeMemSize();
                    break;
                case 0x20dd:
                    pspSysMem.get_instance().sceKernelTotalFreeMemSize();
                    break;
                case 0x20de:
                    pspSysMem.get_instance().sceKernelAllocPartitionMemory(gpr[4], gpr[5], gpr[6], gpr[7],gpr[8]);
                    break;
                case 0x20df:
                    pspSysMem.get_instance().sceKernelFreePartitionMemory(gpr[4]);
                    break;
                case 0x20e0:
                    pspSysMem.get_instance().sceKernelGetBlockHeadAddr(gpr[4]);
                    break;
		// SysMemUserForUser_13A5ABEF(0x20e1),
                case 0x20e2:
                    pspSysMem.get_instance().sceKernelDevkitVersion();
                    break;
		// sceKernelPowerLock(0x20e3),
		// sceKernelPowerUnlock(0x20e4),
		// sceKernelPowerTick(0x20e5),
		// sceSuspendForUser_3E0271D3(0x20e6),
		// sceSuspendForUser_A14F40B2(0x20e7),
		// sceSuspendForUser_A569E425(0x20e8),
                //sceKernelLoadExec(0x20e9),
		// sceKernelExitGameWithStatus(0x20ea),
                case 0x20eb:
                    LoadExec.get_instance().sceKernelExitGame();
                    break;
                case 0x20ec:
                    LoadExec.get_instance().sceKernelRegisterExitCallback(gpr[4]);
                    break;
              // sceDmacMemcpy(0x20ed),
		// sceDmacTryMemcpy(0x20ee),
		// sceGeEdramGetSize(0x20ef),
               case 0x20f0:
                    pspge.get_instance().sceGeEdramGetAddr();
                    break;
                //sceGeEdramSetAddrTranslation(0x20f1),
		// sceGeGetCmd(0x20f2),
		// sceGeGetMtx(0x20f3),
		// sceGeSaveContext(0x20f4),
		// sceGeRestoreContext(0x20f5),
                case 0x20f6:
                    pspge.get_instance().sceGeListEnQueue(gpr[4], gpr[5], gpr[6], gpr[7]);
                    break;
               // sceGeListEnQueueHead(0x20f7),
                case 0x20f8:
                    pspge.get_instance().sceGeListDeQueue(gpr[4]);
                    break;
                case 0x20f9:
                    pspge.get_instance().sceGeListUpdateStallAddr(gpr[4], gpr[5]);
                    break;
                 //sceGeListSync(0x20fa),
		// sceGeDrawSync(0x20fb),
		// sceGeBreak(0x20fc),
		// sceGeContinue(0x20fd),
               // case 0x20fe:
               //     pspge.get_instance().sceGeSetCallback(gpr[4]);
              //      break;
              //  case 0x20ff:
              //      pspge.get_instance().sceGeUnsetCallback(gpr[4]);
              //      break;
                case 0x2100:
                    psprtc.get_instance().sceRtcGetTickResolution();
                    break;
                case 0x2101:
                    psprtc.get_instance().sceRtcGetCurrentTick(gpr[4]);
                    break;
		// sceRtc_011F03C1(0x2102),
		// sceRtc_029CA3B3(0x2103),
		// sceRtcGetCurrentClock(0x2104),
                case 0x2105:
                    psprtc.get_instance().sceRtcGetCurrentClockLocalTime(gpr[4]);
                    break;
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
		case 0x2124: pspAudio.get_instance().sceAudioOutput(gpr[4], gpr[5], gpr[6]); break;
		case 0x2125: pspAudio.get_instance().sceAudioOutputBlocking(gpr[4], gpr[5], gpr[6]); break;
		case 0x2126: pspAudio.get_instance().sceAudioOutputPanned(gpr[4], gpr[5], gpr[6], gpr[7]); break;
                case 0x2127:
                    pspAudio.get_instance().sceAudioOutputPannedBlocking(gpr[4], gpr[5], gpr[6], gpr[7]);
                    break;
                case 0x2128:
                    pspAudio.get_instance().sceAudioChReserve(gpr[4], gpr[5], gpr[6]);
                    break;
		//case 0x2129: pspAudio.get_instance().sceAudioOneshotOutput(); break;
		case 0x212a: pspAudio.get_instance().sceAudioChRelease(gpr[4]); break;
		//case 0x212b: pspAudio.get_instance().sceAudio_B011922F(); break;
		case 0x212c: pspAudio.get_instance().sceAudioSetChannelDataLen(gpr[4], gpr[5]); break;
		//case 0x212d: pspAudio.get_instance().sceAudioChangeChannelConfig(gpr[4], gpr[5]); break;
		case 0x212e: pspAudio.get_instance().sceAudioChangeChannelVolume(gpr[4], gpr[5], gpr[6]); break;
		//case 0x212f: pspAudio.get_instance().sceAudio_38553111(); break;
		//case 0x2130: pspAudio.get_instance().sceAudio_5C37C0AE(); break;
		//case 0x2131: pspAudio.get_instance().sceAudio_E0727056(); break;
		//case 0x2132: pspAudio.get_instance().sceAudioInputBlocking(gpr[4], gpr[5], gpr[6]); break;
		//case 0x2133: pspAudio.get_instance().sceAudioInput(gpr[4], gpr[5], gpr[6]); break;
		//case 0x2134: pspAudio.get_instance().sceAudioGetInputLength(); break;
		//case 0x2135: pspAudio.get_instance().sceAudioWaitInputEnd(); break;
		//case 0x2136: pspAudio.get_instance().sceAudioInputInit(gpr[4], gpr[5], gpr[6]); break;
		//case 0x2137: pspAudio.get_instance().sceAudio_E926D3FB(); break;
		//case 0x2138: pspAudio.get_instance().sceAudio_A633048E(); break;
		case 0x2139: pspAudio.get_instance().sceAudioGetChannelRestLen(gpr[4]); break;
		                    
		
                case 0x213a:
                    pspdisplay.get_instance().sceDisplaySetMode(gpr[4], gpr[5], gpr[6]);
                    break;
               //  sceDisplayGetMode(0x213b),
		// sceDisplayGetFramePerSec(0x213c),
		// sceDisplaySetHoldMode(0x213d),
		// sceDisplaySetResumeMode(0x213e),
                case 0x213f:
                    pspdisplay.get_instance().sceDisplaySetFrameBuf(gpr[4], gpr[5], gpr[6], gpr[7]);
                    break;
              //  sceDisplayGetFrameBuf(0x2140),
		// sceDisplayIsForeground(0x2141),
		// sceDisplayGetBrightness(0x2142),
		// sceDisplayGetVcount(0x2143),
		// sceDisplayIsVblank(0x2144),
                case 0x2145:
                    pspdisplay.get_instance().sceDisplayWaitVblank();
                    break;
		// sceDisplayWaitVblankCB(0x2146),
                case 0x2147:
                    pspdisplay.get_instance().sceDisplayWaitVblankStart();
                    break;
		// sceDisplayWaitVblankStartCB(0x2148),
		// sceDisplayGetCurrentHcount(0x2149),
		// sceDisplayGetAccumulatedHcount(0x214a),
		// sceDisplay_A83EF139(0x214b),
                case 0x214c:
                    pspctrl.get_instance().sceCtrlSetSamplingCycle(gpr[4]);
                    break;
                case 0x214d:
                    pspctrl.get_instance().sceCtrlGetSamplingCycle(gpr[4]);
                    break;
                case 0x214e:
                    pspctrl.get_instance().sceCtrlSetSamplingMode(gpr[4]);
                    break;
                case 0x214f:
                    pspctrl.get_instance().sceCtrlGetSamplingMode(gpr[4]);
                    break;
                case 0x2150:
                    pspctrl.get_instance().sceCtrlPeekBufferPositive(gpr[4], gpr[5]);
                    break;
                case 0x2151:
                    pspctrl.get_instance().sceCtrlPeekBufferNegative(gpr[4], gpr[5]);
                    break;
                case 0x2152:
                    pspctrl.get_instance().sceCtrlReadBufferPositive(gpr[4], gpr[5]);
                    break;
                case 0x2153:
                    pspctrl.get_instance().sceCtrlReadBufferNegative(gpr[4], gpr[5]);
                    break;
              // sceCtrlPeekLatch(0x2154),
                case 0x2155:
                    pspctrl.get_instance().sceCtrlPeekLatch(gpr[4]);
                    break;
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
                default:
                {
                  for (jpcsp.Debugger.DisassemblerModule.syscallsFirm15.calls c : jpcsp.Debugger.DisassemblerModule.syscallsFirm15.calls.values()) {
                  if (c.getSyscall() == code) {
                      System.out.println("Unsupported syscall " + Integer.toHexString(code) + " " + c);
                      return;
                     }
                  }
                  System.out.println("Unsupported syscall " + Integer.toHexString(code));
                }
                break;
            }
        } catch(GeneralJpcspException e) {
            System.out.println(e.getMessage());
        }
    }
}
