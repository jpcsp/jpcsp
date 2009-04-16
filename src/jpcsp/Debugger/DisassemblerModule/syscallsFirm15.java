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
package jpcsp.Debugger.DisassemblerModule;

/**
 *
 * @author George
 */
public class syscallsFirm15 {
     public static enum calls {
		 sceKernelRegisterSubIntrHandler(0x2000,0xca04a2b9),
		 sceKernelReleaseSubIntrHandler(0x2001,0xd61e6961),
		 sceKernelEnableSubIntr(0x2002,0xfb8e22ec),
		 sceKernelDisableSubIntr(0x2003,0x8a389411),
		 sceKernelSuspendSubIntr(0x2004,0x5cb5a78b),
		 sceKernelResumeSubIntr(0x2005,0x7860e0dc),
		 sceKernelIsSubInterruptOccurred(0x2006,0xfc4374b8),
		 QueryIntrHandlerInfo(0x2007,0xd2e8363f),
		 sceKernelRegisterUserSpaceIntrStack(0x2008,0xeee43f47),
		 _sceKernelReturnFromCallback(0x2009,0x6e9ea350),
		 sceKernelRegisterThreadEventHandler(0x200a,0x0c106e53),
		 sceKernelReleaseThreadEventHandler(0x200b,0x72f3c145),
		 sceKernelReferThreadEventHandlerStatus(0x200c,0x369eeb6b),
		 sceKernelCreateCallback(0x200d,0xe81caf8f),
		 sceKernelDeleteCallback(0x200e,0xedba5844),
		 sceKernelNotifyCallback(0x200f,0xc11ba8c4),
		 sceKernelCancelCallback(0x2010,0xba4051d6),
		 sceKernelGetCallbackCount(0x2011,0x2a3d44ff),
		 sceKernelCheckCallback(0x2012,0x349d6d6c),
		 sceKernelReferCallbackStatus(0x2013,0x730ed8bc),
		 sceKernelSleepThread(0x2014,0x9ace131e),
		 sceKernelSleepThreadCB(0x2015,0x82826f70),
		 sceKernelWakeupThread(0x2016,0xd59ead2f),
		 sceKernelCancelWakeupThread(0x2017,0xfccfad26),
		 sceKernelSuspendThread(0x2018,0x9944f31f),
		 sceKernelResumeThread(0x2019,0x75156e8f),
		 sceKernelWaitThreadEnd(0x201a,0x278c0df5),
		 sceKernelWaitThreadEndCB(0x201b,0x840e8133),
		 sceKernelDelayThread(0x201c,0xceadeb47),
		 sceKernelDelayThreadCB(0x201d,0x68da9e36),
		 sceKernelDelaySysClockThread(0x201e,0xbd123d9e),
		 sceKernelDelaySysClockThreadCB(0x201f,0x1181e963),
		 sceKernelCreateSema(0x2020,0xd6da4ba1),
		 sceKernelDeleteSema(0x2021,0x28b6489c),
		 sceKernelSignalSema(0x2022,0x3f53e640),
		 sceKernelWaitSema(0x2023,0x4e3a1105),
		 sceKernelWaitSemaCB(0x2024,0x6d212bac),
		 sceKernelPollSema(0x2025,0x58b1f937),
		 sceKernelCancelSema(0x2026,0x8ffdf9a2),
		 sceKernelReferSemaStatus(0x2027,0xbc6febc5),
		 sceKernelCreateEventFlag(0x2028,0x55c20a00),
		 sceKernelDeleteEventFlag(0x2029,0xef9e4c70),
		 sceKernelSetEventFlag(0x202a,0x1fb15a32),
		 sceKernelClearEventFlag(0x202b,0x812346e4),
		 sceKernelWaitEventFlag(0x202c,0x402fcf22),
		 sceKernelWaitEventFlagCB(0x202d,0x328c546a),
		 sceKernelPollEventFlag(0x202e,0x30fd48f0),
		 sceKernelCancelEventFlag(0x202f,0xcd203292),
		 sceKernelReferEventFlagStatus(0x2030,0xa66b0120),
		 sceKernelCreateMbx(0x2031,0x8125221d),
		 sceKernelDeleteMbx(0x2032,0x86255ada),
		 sceKernelSendMbx(0x2033,0xe9b3061e),
		 sceKernelReceiveMbx(0x2034,0x18260574),
		 sceKernelReceiveMbxCB(0x2035,0xf3986382),
		 sceKernelPollMbx(0x2036,0x0d81716a),
		 sceKernelCancelReceiveMbx(0x2037,0x87d4dd36),
		 sceKernelReferMbxStatus(0x2038,0xa8e8c846),
		 sceKernelCreateMsgPipe(0x2039,0x7c0dc2a0),
		 sceKernelDeleteMsgPipe(0x203a,0xf0b7da1c),
		 sceKernelSendMsgPipe(0x203b,0x876dbfad),
		 sceKernelSendMsgPipeCB(0x203c,0x7c41f2c2),
		 sceKernelTrySendMsgPipe(0x203d,0x884c9f90),
		 sceKernelReceiveMsgPipe(0x203e,0x74829b76),
		 sceKernelReceiveMsgPipeCB(0x203f,0xfbfa697d),
		 sceKernelTryReceiveMsgPipe(0x2040,0xdf52098f),
		 sceKernelCancelMsgPipe(0x2041,0x349b864d),
		 sceKernelReferMsgPipeStatus(0x2042,0x33be4024),
		 sceKernelCreateVpl(0x2043,0x56c039b5),
		 sceKernelDeleteVpl(0x2044,0x89b3d48c),
		 sceKernelAllocateVpl(0x2045,0xbed27435),
		 sceKernelAllocateVplCB(0x2046,0xec0a693f),
		 sceKernelTryAllocateVpl(0x2047,0xaf36d708),
		 sceKernelFreeVpl(0x2048,0xb736e9ff),
		 sceKernelCancelVpl(0x2049,0x1d371b8a),
		 sceKernelReferVplStatus(0x204a,0x39810265),
		 sceKernelCreateFpl(0x204b,0xc07bb470),
                 sceKernelDeleteFpl(0x204c,0xed1410e0),
                 sceKernelAllocateFpl(0x204d,0xd979e9bf),
                 sceKernelAllocateFplCB(0x204e,0xe7282cb6),
                 sceKernelTryAllocateFpl(0x204f,0x623ae665),
                 sceKernelFreeFpl(0x2050,0xf6414a71),
                 sceKernelCancelFpl(0x2051,0xa8aa591f),
                 sceKernelReferFplStatus(0x2052,0xd8199e4c),
                 _sceKernelReturnFromTimerHandler(0x2053,0x0e927aed),
                 sceKernelUSec2SysClock(0x2054,0x110dec9a),
                 sceKernelUSec2SysClockWide(0x2055,0xc8cd158c),
                 sceKernelSysClock2USec(0x2056,0xba6b92e2),
                 sceKernelSysClock2USecWide(0x2057,0xe1619d7c),
                 sceKernelGetSystemTime(0x2058,0xdb738f35),
                 sceKernelGetSystemTimeWide(0x2059,0x82bc5777),
                 sceKernelGetSystemTimeLow(0x205a,0x369ed59d),
                 sceKernelSetAlarm(0x205b,0x6652b8ca),
                 sceKernelSetSysClockAlarm(0x205c,0xb2c25152),
                 sceKernelCancelAlarm(0x205d,0x7e65b999),
                 sceKernelReferAlarmStatus(0x205e,0xdaa3f564),
                 sceKernelCreateVTimer(0x205f,0x20fff560),
                 sceKernelDeleteVTimer(0x2060,0x328f9e52),
                 sceKernelGetVTimerBase(0x2061,0xb3a59970),
                 sceKernelGetVTimerBaseWide(0x2062,0xb7c18b77),
                 sceKernelGetVTimerTime(0x2063,0x034a921f),
                 sceKernelGetVTimerTimeWide(0x2064,0xc0b3ffd2),
                 sceKernelSetVTimerTime(0x2065,0x542ad630),
                 sceKernelSetVTimerTimeWide(0x2066,0xfb6425c3),
                 sceKernelStartVTimer(0x2067,0xc68d9437),
                 sceKernelStopVTimer(0x2068,0xd0aeee87),
                 sceKernelSetVTimerHandler(0x2069,0xd8b299ae),
                 sceKernelSetVTimerHandlerWide(0x206a,0x53b00e9a),
                 sceKernelCancelVTimerHandler(0x206b,0xd2d615ef),
                 sceKernelReferVTimerStatus(0x206c,0x5f32beaa),
		 sceKernelCreateThread(0x206d,0x446d8de6),
		 sceKernelDeleteThread(0x206e,0x9fa03cd3),
		 sceKernelStartThread(0x206f,0xf475845d),
		 _sceKernelExitThread(0x2070,0x532a522e),
		 sceKernelExitThread(0x2071,0xaa73c935),
		 sceKernelExitDeleteThread(0x2072,0x809ce29b),
		 sceKernelTerminateThread(0x2073,0x616403ba),
		 sceKernelTerminateDeleteThread(0x2074,0x383f7bcc),
		 sceKernelSuspendDispatchThread(0x2075,0x3ad58b8c),
		 sceKernelResumeDispatchThread(0x2076,0x27e22ec2),
		 sceKernelChangeCurrentThreadAttr(0x2077,0xea748e31),
		 sceKernelChangeThreadPriority(0x2078,0x71bc9871),
		 sceKernelRotateThreadReadyQueue(0x2079,0x912354a7),
		 sceKernelReleaseWaitThread(0x207a,0x2c34e053),
		 sceKernelGetThreadId(0x207b,0x293b45b8),
		 sceKernelGetThreadCurrentPriority(0x207c,0x94aa61ee),
		 sceKernelGetThreadExitStatus(0x207d,0x3b183e26),
		 sceKernelCheckThreadStack(0x207e,0xd13bde95),
		 sceKernelGetThreadStackFreeSize(0x207f,0x52089ca1),
		 sceKernelReferThreadStatus(0x2080,0x17c1684e),
		 sceKernelReferThreadRunStatus(0x2081,0xffc36a14),
		 sceKernelReferSystemStatus(0x2082,0x627e6f3a),
		 sceKernelGetThreadmanIdList(0x2083,0x94416130),
		 sceKernelGetThreadmanIdType(0x2084,0x57cf62dd),
		 sceKernelReferThreadProfiler(0x2085,0x64d4540e),
		 sceKernelReferGlobalProfiler(0x2086,0x8218b4dd),
		 sceIoPollAsync(0x2087,0x3251ea56),
		 sceIoWaitAsync(0x2088,0xe23eec33),
		 sceIoWaitAsyncCB(0x2089,0x35dbd746),
		 sceIoGetAsyncStat(0x208a,0xcb05f8d6),
		 sceIoChangeAsyncPriority(0x208b,0xb293727f),
		 sceIoSetAsyncCallback(0x208c,0xa12a0514),
		 sceIoClose(0x208d,0x810c4bc3),
		 sceIoCloseAsync(0x208e,0xff5940b6),
		 sceIoOpen(0x208f,0x109f50bc),
		 sceIoOpenAsync(0x2090,0x89aa9906),
		 sceIoRead(0x2091,0x6a638d83),
		 sceIoReadAsync(0x2092,0xa0b5a7c2),
		 sceIoWrite(0x2093,0x42ec03ac),
		 sceIoWriteAsync(0x2094,0x0facab19),
		 sceIoLseek(0x2095,0x27eb27b8),
		 sceIoLseekAsync(0x2096,0x71b19e77),
		 sceIoLseek32(0x2097,0x68963324),
		 sceIoLseek32Async(0x2098,0x1b385d8f),
		 sceIoIoctl(0x2099,0x63632449),
		 sceIoIoctlAsync(0x209a,0xe95a012b),
		 sceIoDopen(0x209b,0xb29ddf9c),
		 sceIoDread(0x209c,0xe3eb004c),
		 sceIoDclose(0x209d,0xeb092469),
		 sceIoRemove(0x209e,0xf27a9c51),
		 sceIoMkdir(0x209f,0x06a70004),
		 sceIoRmdir(0x20a0,0x1117c65f),
		 sceIoChdir(0x20a1,0x55f4717d),
		 sceIoSync(0x20a2,0xab96437f),
		 sceIoGetstat(0x20a3,0xace946e8),
		 sceIoChstat(0x20a4,0xb8a740f4),
		 sceIoRename(0x20a5,0x779103a0),
		 sceIoDevctl(0x20a6,0x54f5fb11),
		 sceIoGetDevType(0x20a7,0x08bd7374),
		 sceIoAssign(0x20a8,0xb2a628c1),
		 sceIoUnassign(0x20a9,0x6d08a871),
		 sceIoCancel(0x20aa,0xe8bc6571),
		 sceIoGetFdList(0x20ab,0x5c2be2cc),
		 sceKernelStdioRead(0x20ac,0x3054d478),
		 sceKernelStdioLseek(0x20ad,0x0cbb0571),
		 sceKernelStdioSendChar(0x20ae,0xa46785c9),
		 sceKernelStdioWrite(0x20af,0xa3b931db),
		 sceKernelStdioClose(0x20b0,0x9d061c19),
		 sceKernelStdioOpen(0x20b1,0x924aba61),
		 sceKernelStdin(0x20b2,0x172d316e),
		 sceKernelStdout(0x20b3,0xa6bab2e9),
		 sceKernelStderr(0x20b4,0xf78ba90a),
                 sceKernelDcacheInvalidateRange(0x20b5,0xbfa98062),
                 sceKernelIcacheInvalidateRange(0x20b6,0xc2df770e),
                 sceKernelUtilsMd5Digest(0x20b7,0xc8186a58),
                 sceKernelUtilsMd5BlockInit(0x20b8,0x9e5c5086),
                 sceKernelUtilsMd5BlockUpdate(0x20b9,0x61e1e525),
                 sceKernelUtilsMd5BlockResult(0x20ba,0xb8d24e78),
                 sceKernelUtilsSha1Digest(0x20bb,0x840259f1),
                 sceKernelUtilsSha1BlockInit(0x20bc,0xf8fcd5ba),
                 sceKernelUtilsSha1BlockUpdate(0x20bd,0x346f6da8),
                 sceKernelUtilsSha1BlockResult(0x20be,0x585f1c09),
                 sceKernelUtilsMt19937Init(0x20bf,0xe860e75e),
                 sceKernelUtilsMt19937UInt(0x20c0,0x06fb8a63),
                 sceKernelGetGPI(0x20c1,0x37fb5c42),
                 sceKernelSetGPO(0x20c2,0x6ad345d7),
                 sceKernelLibcClock(0x20c3,0x91e4f6a7),
                 sceKernelLibcTime(0x20c4,0x27cc57f0),
                 sceKernelLibcGettimeofday(0x20c5,0x71ec4271),
                 sceKernelDcacheWritebackAll(0x20c6,0x79d1c3fa),
                 sceKernelDcacheWritebackInvalidateAll(0x20c7,0xb435dec5),
                 sceKernelDcacheWritebackRange(0x20c8,0x3ee30821),
                 sceKernelDcacheWritebackInvalidateRange(0x20c9,0x34b9fa9e),
                 sceKernelDcacheProbe(0x20ca,0x80001c4c),
                 sceKernelDcacheReadTag(0x20cb,0x16641d70),
                 sceKernelIcacheInvalidateAll(0x20cc,0x920f104a),
                 sceKernelIcacheProbe(0x20cd,0x4fd31c9d),
                 sceKernelIcacheReadTag(0x20ce,0xfb05fad0),
                 sceKernelLoadModule(0x20cf,0x977de386),
                 sceKernelLoadModuleByID(0x20d0,0xb7f46618),
                 sceKernelLoadModuleMs(0x20d1,0x710f61b5),
                 sceKernelLoadModuleBufferUsbWlan(0x20d2,0xf9275d98),
                 KernelStartModule(0x20d3,0x50f0c1ec),
                 KernelStopModule(0x20d4,0xd1ff982a),
                 KernelUnloadModule(0x20d5,0x2e0911aa),
                 KernelSelfStopUnloadModule(0x20d6,0xd675ebb8),
                 KernelStopUnloadSelfModule(0x20d7,0xcc1d3699),
                 KernelGetModuleIdList(0x20d8,0x644395e2),
                 KernelQueryModuleInfo(0x20d9,0x748cbed9),
                 sceKernelGetModuleId(0x20da,0xf0a26395),
                 sceKernelGetModuleIdByAddress(0x20db,0xd8b73127),
                 KernelMaxFreeMemSize(0x20dc,0xa291f107),
                 KernelTotalFreeMemSize(0x20dd,0xf919f628),
                 KernelAllocPartitionMemory(0x20de,0x237dbd4f),
                 KernelFreePartitionMemory(0x20df,0xb6d61d02),
                 KernelGetBlockHeadAddr(0x20e0,0x9d9a5ba1),
                 sceKernelPrintf(0x20e1,0x13a5abef),
                 KernelDevkitVersion(0x20e2,0x3fc9ae6a),
                 KernelPowerLock(0x20e3,0xeadb1bd7),
                 KernelPowerUnlock(0x20e4,0x3aee7261),
                 KernelPowerTick(0x20e5,0x090ccb3f),
                 sceKernelVolatileMemLock(0x20e6,0x3e0271d3),
                 sceKernelVolatileMemTryLock(0x20e7,0xa14f40b2),
                 sceKernelVolatileMemUnlock(0x20e8,0xa569e425),
                 KernelLoadExec(0x20e9,0xbd2f1094),
                 KernelExitGameWithStatus(0x20ea,0x2ac9954b),
                 KernelExitGame(0x20eb,0x05572a5f),
                 KernelRegisterExitCallback(0x20ec,0x4ac57943),
                 DmacMemcpy(0x20ed,0x617f3fe6),
                 DmacTryMemcpy(0x20ee,0xd97f94d8),
                 GeEdramGetSize(0x20ef,0x1f6752ad),
                 GeEdramGetAddr(0x20f0,0xe47e40e4),
                 GeEdramSetAddrTranslation(0x20f1,0xb77905ea),
                 GeGetCmd(0x20f2,0xdc93cfef),
                 GeGetMtx(0x20f3,0x57c8945b),
                 GeSaveContext(0x20f4,0x438a385a),
                 GeRestoreContext(0x20f5,0x0bf608fb),
                 GeListEnQueue(0x20f6,0xab49e76a),
                 GeListEnQueueHead(0x20f7,0x1c0d95a6),
                 GeListDeQueue(0x20f8,0x5fb86ab0),
                 GeListUpdateStallAddr(0x20f9,0xe0d68148),
                 GeListSync(0x20fa,0x03444eb4),
                 GeDrawSync(0x20fb,0xb287bd61),
                 GeBreak(0x20fc,0xb448ec0d),
                 GeContinue(0x20fd,0x4c06e472),
                 GeSetCallback(0x20fe,0xa4fc06a4),
                 GeUnsetCallback(0x20ff,0x05db22ce),
                 RtcGetTickResolution(0x2100,0xc41c2853),
                 RtcGetCurrentTick(0x2101,0x3f7ad767),
                 sceRtcGetAccumulativeTime(0x2102,0x011f03c1),
                 Rtc_029CA3B3(0x2103,0x029ca3b3),
                 RtcGetCurrentClock(0x2104,0x4cfa57b0),
                 RtcGetCurrentClockLocalTime(0x2105,0xe7c27d1b),
                 RtcConvertUtcToLocalTime(0x2106,0x34885e0d),
                 RtcConvertLocalTimeToUTC(0x2107,0x779242a2),
                 RtcIsLeapYear(0x2108,0x42307a17),
                 RtcGetDaysInMonth(0x2109,0x05ef322c),
                 RtcGetDayOfWeek(0x210a,0x57726bc1),
                 RtcCheckValid(0x210b,0x4b1b5e82),
                 RtcSetTime_t(0x210c,0x3a807cc8),
                 RtcGetTime_t(0x210d,0x27c4594c),
                 RtcSetDosTime(0x210e,0xf006f264),
                 RtcGetDosTime(0x210f,0x36075567),
                 RtcSetWin32FileTime(0x2110,0x7ace4c04),
                 RtcGetWin32FileTime(0x2111,0xcf561893),
                 RtcSetTick(0x2112,0x7ed29e40),
                 RtcGetTick(0x2113,0x6ff40acc),
                 RtcCompareTick(0x2114,0x9ed0ae87),
                 RtcTickAddTicks(0x2115,0x44f45e05),
                 RtcTickAddMicroseconds(0x2116,0x26d25a5d),
                 RtcTickAddSeconds(0x2117,0xf2a4afe5),
                 RtcTickAddMinutes(0x2118,0xe6605bca),
                 RtcTickAddHours(0x2119,0x26d7a24a),
                 RtcTickAddDays(0x211a,0xe51b4b7a),
                 RtcTickAddWeeks(0x211b,0xcf3a2ca8),
                 RtcTickAddMonths(0x211c,0xdbf74f1b),
                 RtcTickAddYears(0x211d,0x42842c77),
                 RtcFormatRFC2822(0x211e,0xc663b3b9),
                 RtcFormatRFC2822LocalTime(0x211f,0x7de6711b),
                 RtcFormatRFC3339(0x2120,0x0498fb3c),
                 RtcFormatRFC3339LocalTime(0x2121,0x27f98543),
                 RtcParseDateTime(0x2122,0xdfbc5f16),
                 RtcParseRFC3339(0x2123,0x28e1e988),
                 AudioOutput(0x2124,0x8c1009b2),
                 AudioOutputBlocking(0x2125,0x136caf51),
                 AudioOutputPanned(0x2126,0xe2d56b2d),
                 AudioOutputPannedBlocking(0x2127,0x13f592bc),
                 AudioChReserve(0x2128,0x5ec81c55),
                 AudioOneshotOutput(0x2129,0x41efade7),
                 AudioChRelease(0x212a,0x6fc46853),
                 sceAudioGetChannelRestLength(0x212b,0xb011922f),
                 AudioSetChannelDataLen(0x212c,0xcb2e439e),
                 AudioChangeChannelConfig(0x212d,0x95fd0c2d),
                 AudioChangeChannelVolume(0x212e,0xb7e1d8e7),
                 sceAudioSRCChReserve(0x212f,0x38553111),
                 sceAudioSRCChRelease(0x2130,0x5c37c0ae),
                 sceAudioSRCOutputBlocking(0x2131,0xe0727056),
                 AudioInputBlocking(0x2132,0x086e5895),
                 AudioInput(0x2133,0x6d4bec68),
                 AudioGetInputLength(0x2134,0xa708c6a6),
                 AudioWaitInputEnd(0x2135,0x87b2e651),
                 AudioInputInit(0x2136,0x7de61688),
                 sceAudioInputInitEx(0x2137,0xe926d3fb),
                 sceAudioPollInputEnd(0x2138,0xa633048e),
                 AudioGetChannelRestLen(0x2139,0xe9d97901),
		 sceDisplaySetMode(0x213a,0x0e20f177),
		 sceDisplayGetMode(0x213b,0xdea197d4),
		 sceDisplayGetFramePerSec(0x213c,0xdba6c4c4),
		 sceDisplaySetHoldMode(0x213d,0x7ed59bc4),
		 sceDisplaySetResumeMode(0x213e,0xa544c486),
		 sceDisplaySetFrameBuf(0x213f,0x289d82fe),
		 sceDisplayGetFrameBuf(0x2140,0xeeda2e54),
		 sceDisplayIsForeground(0x2141,0xb4f378fa),
		 sceDisplayGetBrightness(0x2142,0x31c4baa8),
		 sceDisplayGetVcount(0x2143,0x9c6eaad7),
		 sceDisplayIsVblank(0x2144,0x4d4e10ec),
		 sceDisplayWaitVblank(0x2145,0x36cdfade),
		 sceDisplayWaitVblankCB(0x2146,0x8eb9ec49),
		 sceDisplayWaitVblankStart(0x2147,0x984c27e7),
		 sceDisplayWaitVblankStartCB(0x2148,0x46f186c3),
		 sceDisplayGetCurrentHcount(0x2149,0x773dd3a3),
		 sceDisplayGetAccumulatedHcount(0x214a,0x210eab3a),
		 sceDisplayAdjustAccumulatedHcount(0x214b,0xa83ef139),
		 sceCtrlSetSamplingCycle(0x214c,0x6a2774f3),
		 sceCtrlGetSamplingCycle(0x214d,0x02baad91),
		 sceCtrlSetSamplingMode(0x214e,0x1f4011e6),
		 sceCtrlGetSamplingMode(0x214f,0xda6b76a1),
		 sceCtrlPeekBufferPositive(0x2150,0x3a622550),
		 sceCtrlPeekBufferNegative(0x2151,0xc152080a),
		 sceCtrlReadBufferPositive(0x2152,0x1f803938),
		 sceCtrlReadBufferNegative(0x2153,0x60b81f86),
		 sceCtrlPeekLatch(0x2154,0xb1d0e5cd),
		 sceCtrlReadLatch(0x2155,0x0b588501),
		 sceCtrlSetIdleCancelThreshold(0x2156,0xa7144800),
		 sceCtrlGetIdleCancelThreshold(0x2157,0x687660fa),
		 sceCtrl_348D99D4(0x2158,0x348d99d4),
		 sceCtrl_AF5960F3(0x2159,0xaf5960f3),
		 sceCtrlClearRapidFire(0x215a,0xa68fd260),
		 sceCtrlSetRapidFire(0x215b,0x6841be1a),
		 sceHprmRegisterCallback(0x215c,0xc7154136),
		 sceHprmUnregisterCallback(0x215d,0x444ed0b7),
		 sceHprm_71B5FB67(0x215e,0x71b5fb67),
		 sceHprmIsRemoteExist(0x215f,0x208db1bd),
                 HprmIsHeadphoneExist(0x2160,0x7e69eda4),
                 HprmIsMicrophoneExist(0x2161,0x219c58f1),
                 HprmPeekCurrentKey(0x2162,0x1910b327),
                 HprmPeekLatch(0x2163,0x2bcec83e),
                 HprmReadLatch(0x2164,0x40d2f9f0),
                 Power_2B51FE2F(0x2165,0x2b51fe2f),
                 Power_442BFBAC(0x2166,0x442bfbac),
                 PowerTick(0x2167,0xefd3c963),
                 PowerGetIdleTimer(0x2168,0xedc13fe5),
                 PowerIdleTimerEnable(0x2169,0x7f30b3b1),
                 PowerIdleTimerDisable(0x216a,0x972ce941),
                 PowerBatteryUpdateInfo(0x216b,0x27f3292c),
                 Power_E8E4E204(0x216c,0xe8e4e204),
                 PowerGetLowBatteryCapacity(0x216d,0xb999184c),
                 PowerIsPowerOnline(0x216e,0x87440f5e),
                 PowerIsBatteryExist(0x216f,0x0afd0d8b),
                 PowerIsBatteryCharging(0x2170,0x1e490401),
                 PowerGetBatteryChargingStatus(0x2171,0xb4432bc8),
                 PowerIsLowBattery(0x2172,0xd3075926),
                 Power_78A1A796(0x2173,0x78a1a796),
                 PowerGetBatteryRemainCapacity(0x2174,0x94f5a53f),
                 Power_FD18A0FF(0x2175,0xfd18a0ff),
                 PowerGetBatteryLifePercent(0x2176,0x2085d15d),
                 PowerGetBatteryLifeTime(0x2177,0x8efb3fa2),
                 PowerGetBatteryTemp(0x2178,0x28e12023),
                 PowerGetBatteryElec(0x2179,0x862ae1a6),
                 PowerGetBatteryVolt(0x217a,0x483ce86b),
                 Power_23436A4A(0x217b,0x23436a4a),
                 Power_0CD21B1F(0x217c,0x0cd21b1f),
                 Power_165CE085(0x217d,0x165ce085),
                 Power_23C31FFE(0x217e,0x23c31ffe),
                 Power_FA97A599(0x217f,0xfa97a599),
                 Power_B3EDD801(0x2180,0xb3edd801),
                 PowerLock(0x2181,0xd6d016ef),
                 PowerUnlock(0x2182,0xca3d34c1),
                 PowerCancelRequest(0x2183,0xdb62c9cf),
                 PowerIsRequest(0x2184,0x7fa406dd),
                 PowerRequestStandby(0x2185,0x2b7c7cf4),
                 PowerRequestSuspend(0x2186,0xac32c9cc),
                 Power_2875994B(0x2187,0x2875994b),
                 PowerEncodeUBattery(0x2188,0x3951af53),
                 PowerGetResumeCount(0x2189,0x0074ef9b),
       PowerRegisterCallback(0x218a,0x04b7766e),
       PowerUnregisterCallback(0x218b,0xdfa8baf8),
       PowerUnregitserCallback(0x218c,0xdb9d28dd),
       PowerSetCpuClockFrequency(0x218d,0x843fbf43),
       PowerSetBusClockFrequency(0x218e,0xb8d7b3fb),
       PowerGetCpuClockFrequency(0x218f,0xfee03a2f),
       PowerGetBusClockFrequency(0x2190,0x478fe6f5),
       PowerGetCpuClockFrequencyInt(0x2191,0xfdb5bfe9),
       PowerGetBusClockFrequencyInt(0x2192,0xbd681969),
       Power_34F9C463(0x2193,0x34f9c463),
       PowerGetCpuClockFrequencyFloat(0x2194,0xb1a52c83),
       PowerGetBusClockFrequencyFloat(0x2195,0x9badb3eb),
       Power_EA382A27(0x2196,0xea382a27),
       PowerSetClockFrequency(0x2197,0x737486f2),
       UsbStart(0x2198,0xae5de6af),
       UsbStop(0x2199,0xc2464fa0),
       UsbGetState(0x219a,0xc21645a4),
       UsbGetDrvList(0x219b,0x4e537366),
       UsbGetDrvState(0x219c,0x112cc951),
       UsbActivate(0x219d,0x586db82c),
       UsbDeactivate(0x219e,0xc572a9c8),
       UsbWaitState(0x219f,0x5be0e002),
       UsbWaitCancel(0x21a0,0x1c360735),
       OpenPSIDGetOpenPSID(0x21a1,0xc69bebce),
       SircsSend(0x21a2,0x71eef62d),
       UmdCheckMedium(0x21a3,0x46ebb729),
       UmdActivate(0x21a4,0xc6183d47),
       UmdDeactivate(0x21a5,0xe83742ba),
       UmdWaitDriveStat(0x21a6,0x8ef08fce),
       UmdWaitDriveStatWithTimer(0x21a7,0x56202973),
       UmdWaitDriveStatCB(0x21a8,0x4a9e5e29),
       UmdCancelWaitDriveStat(0x21a9,0x6af9b50a),
       UmdGetDriveStat(0x21aa,0x6b4a146c),
       UmdGetErrorStat(0x21ab,0x20628e6f),
       UmdGetDiscInfo(0x21ac,0x340b7686),
       UmdRegisterUMDCallBack(0x21ad,0xaee7404d),
       UmdUnRegisterUMDCallBack(0x21ae,0xbd2bde07),
       WlanDevIsPowerOn(0x21af,0x93440b11),
       WlanGetSwitchState(0x21b0,0xd7763699),
       WlanGetEtherAddr(0x21b1,0x0c622081),
       WlanDevAttach(0x21b2,0x482cae9a),
       WlanDevDetach(0x21b3,0xc9a8cab7),
       WlanDrv_lib_19E51F54(0x21b4,0x19e51f54),
       WlanDevIsGameMode(0x21b5,0x5e7c8d94),
       WlanGPPrevEstablishActive(0x21b6,0x5ed4049a),
       WlanGPSend(0x21b7,0xb4d7cb74),
       WlanGPRecv(0x21b8,0xa447103a),
       WlanGPRegisterCallback(0x21b9,0x9658c9f7),
       WlanGPUnRegisterCallback(0x21ba,0x4c7f62e0),
       WlanDrv_lib_81579D36(0x21bb,0x81579d36),
       WlanDrv_lib_5BAA1FE5(0x21bc,0x5baa1fe5),
       WlanDrv_lib_4C14BACA(0x21bd,0x4c14baca),
       WlanDrv_lib_2D0FAE4E(0x21be,0x2d0fae4e),
       WlanDrv_lib_56F467CA(0x21bf,0x56f467ca),
       WlanDrv_lib_FE8A0B46(0x21c0,0xfe8a0b46),
       WlanDrv_lib_40B0AA4A(0x21c1,0x40b0aa4a),
       WlanDevSetGPIO(0x21c2,0x7ff54bd2),
       WlanDevGetStateGPIO(0x21c3,0x05fe320c),
       WlanDrv_lib_8D5F551B(0x21c4,0x8d5f551b),
       VaudioOutputBlocking(0x21c5,0x8986295e),
       VaudioChReserve(0x21c6,0x03b6807d),
       VaudioChRelease(0x21c7,0x67585dfd),
       Vaudio_346FBE94(0x21c8,0x346fbe94),
       RegExit(0x21c9,0x9b25edf1),
       RegOpenRegistry(0x21ca,0x92e41280),
       RegCloseRegistry(0x21cb,0xfa8a5739),
       RegRemoveRegistry(0x21cc,0xdeda92bf),
       sceRegOpenCategory(0x21cd,0x1d8a762e),
       sceRegCloseCategory(0x21ce,0x0cae832b),
       RegFlushRegistry(0x21cf,0x39461b4d),
       sceRegFlushCategory(0x21d0,0x0d69bf40),
       sceRegCreateKey(0x21d1,0x57641a81),
       sceRegSetKeyValue(0x21d2,0x17768e14),
       sceRegGetKeyInfo(0x21d3,0xd4475aa8),
       sceRegGetKeyValue(0x21d4,0x28a8e98a),
       sceRegGetKeysNum(0x21d5,0x2c0db9dd),
       sceRegGetKeys(0x21d6,0x2d211135),
       sceRegRemoveCategory(0x21d7,0x4ca16893),
       sceRegRemoveKey(0x21d8,0x3615bc87),
       sceRegKickBackDiscover(0x21d9,0xc5768d02),
       sceRegGetKeyValueByName(0x21da,0x30be0259),
       sceUtilityGameSharingInitStart(0x21db,0xc492f751),
       sceUtilityGameSharingShutdownStart(0x21dc,0xefc6f80f),
       sceUtilityGameSharingUpdate(0x21dd,0x7853182d),
       sceUtilityGameSharingGetStatus(0x21de,0x946963f3),
       sceNetplayDialogInitStart(0x21df,0x3ad50ae7),
       sceNetplayDialogShutdownStart(0x21e0,0xbc6b6296),
       sceNetplayDialogUpdate(0x21e1,0x417bed54),
       sceNetplayDialogGetStatus(0x21e2,0xb6cee597),
       sceUtilityNetconfInitStart(0x21e3,0x4db1e739),
       sceUtilityNetconfShutdownStart(0x21e4,0xf88155f6),
       sceUtilityNetconfUpdate(0x21e5,0x91e70e35),
       sceUtilityNetconfGetStatus(0x21e6,0x6332aa39),
       sceUtilitySavedataInitStart(0x21e7,0x50c4cd57),
       sceUtilitySavedataShutdownStart(0x21e8,0x9790b33c),
       sceUtilitySavedataUpdate(0x21e9,0xd4b95ffb),
       sceUtilitySavedataGetStatus(0x21ea,0x8874dbe0),
       sceUtility_2995D020(0x21eb,0x2995d020),
       sceUtility_B62A4061(0x21ec,0xb62a4061),
       sceUtility_ED0FAD38(0x21ed,0xed0fad38),
       sceUtility_88BC7406(0x21ee,0x88bc7406),
       sceUtilityMsgDialogInitStart(0x21ef,0x2ad8e239),
       sceUtilityMsgDialogShutdownStart(0x21f0,0x67af3428),
       sceUtilityMsgDialogUpdate(0x21f1,0x95fc253b),
       sceUtilityMsgDialogGetStatus(0x21f2,0x9a1c91d7),
       sceUtilityOskInitStart(0x21f3,0xf6269b82),
       sceUtilityOskShutdownStart(0x21f4,0x3dfaeba9),
       sceUtilityOskUpdate(0x21f5,0x4b85c861),
       sceUtilityOskGetStatus(0x21f6,0xf3f76017),
       sceUtilitySetSystemParamInt(0x21f7,0x45c18506),
       sceUtilitySetSystemParamString(0x21f8,0x41e30674),
       sceUtilityGetSystemParamInt(0x21f9,0xa5da2406),
       sceUtilityGetSystemParamString(0x21fa,0x34b78343),
       sceUtilityCheckNetParam(0x21fb,0x5eee6548),
       sceUtilityGetNetParam(0x21fc,0x434d4b3a),
       sceUtility_private_17CB4D96(0x21fd,0x17cb4d96),
       sceUtility_private_EE7AC503(0x21fe,0xee7ac503),
       sceUtility_private_5FF96ED3(0x21ff,0x5ff96ed3),
       sceUtility_private_9C9DD5BC(0x2200,0x9c9dd5bc),
       sceUtility_private_4405BA38(0x2201,0x4405ba38),
       sceUtility_private_1DFA62EF(0x2202,0x1dfa62ef),
       sceUtilityDialogSetStatus(0x2203,0x680c0ea8),
       sceUtilityDialogGetType(0x2204,0xb222e887),
       sceUtilityDialogGetParam(0x2205,0x4f2206bc),
       sceUtility_private_EF5BC2D1(0x2206,0xef5bc2d1),
       sceUtilityDialogGetSpeed(0x2207,0xe01fe32a),
       sceUtility_private_19461966(0x2208,0x19461966),
       sceUtilityDialogSetThreadId(0x2209,0x6f923bd3),
       sceUtilityDialogLoadModule(0x220a,0xa5168a5d),
       sceUtilityDialogPowerLock(0x220b,0x3ceae1df),
       sceUtilityDialogPowerUnlock(0x220c,0x56bedca4),
       sceUtilityCreateNetParam(0x220d,0x072debf2),
       sceUtilityDeleteNetParam(0x220e,0x9ce50172),
       sceUtilityCopyNetParam(0x220f,0xfb0c4840),
       sceUtilitySetNetParam(0x2210,0xfc4516f3),

            /*added random syscalls numbers */
                   /*sceAtrac3plus  found in puzzle booble */
       sceAtracStartEntry(0x2211,0xD1F59FDB),
       sceAtracEndEntry(0x2212,0xD5C28CC0),
       sceAtracGetAtracID(0x2213,0x780F88D1),
       sceAtracReleaseAtracID(0x2214,0x61EB33F5),
       sceAtracSetData(0x2215,0x0E2A73AB),
       sceAtracSetHalfwayBuffer(0x2216,0x3F6E26B5),
       sceAtracSetDataAndGetID(0x2217,0x7A20E7AF),
       sceAtracSetHalfwayBufferAndGetID(0x2218,0x0FAE370E),
       sceAtracDecodeData(0x2219,0x6A8C3CD5),
       sceAtracGetRemainFrame(0x221a,0x9AE849A7),
       sceAtracGetStreamDataInfo(0x221b,0x5D268707),
       sceAtracAddStreamData(0x221c,0x7DB31251),
       sceAtracGetSecondBufferInfo(0x221d,0x83E85EA0),
       sceAtracSetSecondBuffer(0x221e,0x83BF7AFD),
       sceAtracGetNextDecodePosition(0x221f,0xE23E3A35),
       sceAtracGetSoundSample(0x2220,0xA2BBA8BE),
       sceAtracGetChannel(0x2221,0x31668BAA),
       sceAtracGetMaxSample(0x2222,0xD6A5F2F7),
       sceAtracGetNextSample(0x2223,0x36FAABFB),
       sceAtracGetBitrate(0x2224,0xA554A158),
       sceAtracGetLoopStatus(0x2225,0xFAA4F89B),
       sceAtracSetLoopNum(0x2226,0x868120B5),
       sceAtracGetBufferInfoForReseting(0x2227,0xCA3CA3D2),
       sceAtracResetPlayPosition(0x2228,0x644E5607),
       sceAtracGetInternalErrorInfo(0x2229,0xE88F759B),
       /*sce Net */
       sceNetInit(0x222a,0x39af39a6),
       sceNetTerm(0x222b,0x281928A9),
       sceNetFreeThreadinfo(0x222c,0x50647530),
       sceNetThreadAbort(0x222d,0xAD6844C6),
       sceNetEtherNtostr(0x222e,0x89360950),
       sceNetEtherStrton(0x222f,0xD27961C9),
       sceNetGetLocalEtherAddr(0x2230,0x0BF0A3AE),
       sceNetGetMallocStat(0x2231,0xCC393E48),
       /* InterruptManagerForKernel */
       	sceKernelCpuSuspendIntr(0x2232,0x092968F4),
        sceKernelCpuResumeIntr(0x2233,0x5F10D406),
        sceKernelCpuResumeIntrWithSync(0x2234,0x3B84732D),
        sceKernelIsIntrContext(0x2235,0xFE28C6D9),
        InterruptManagerForKernel_53991063(0x2236,0x53991063),
        sceKernelGetInterruptExitCount(0x2237,0x468BC716),
	ReturnToThread(0x2238,0x43CD40EF),
	SaveThreadContext(0x2239,0x85F7766D),
	sceKernelCpuEnableIntr(0x223a,0x02314986),
	QueryInterruptManCB(0x223b,0x00B6B0F3),
	sceKernelRegisterIntrHandler(0x223c,0x58DD8978),
	InterruptManagerForKernel_15894D0B(0x223d,0x15894D0B),
	sceKernelReleaseIntrHandler(0x223e,0xF987B1F0),
	sceKernelSetIntrLevel(0x223f,0xB5A15B30),
	InterruptManagerForKernel_43A7BBDC(0x2240,0x43A7BBDC),
	sceKernelIsInterruptOccurred(0x2241,0x02475AAF),
	sceKernelEnableIntr(0x2242,0x4D6E7305),
	sceKernelSuspendIntr(0x2243,0x750E2507),
	sceKernelDisableIntr(0x2244,0xD774BA45),
	sceKernelResumeIntr(0x2245,0x494D6D2B),
	RegisterContextHooks(0x2246,0x2CD783A1),
	ReleaseContextHooks(0x2247,0x55242A8B),
	UnSupportIntr(0x2248,0x27BC9A45),
	SupportIntr(0x2249,0x0E224D66),
	sceKernelRegisterDebuggerIntrHandler(0x224a,0x272766F8),
	sceKernelReleaseDebuggerIntrHandler(0x224b,0xB386A459),
	sceKernelCallSubIntrHandler(0x224c,0xCDC86B64),
	sceKernelGetUserIntrStack(0x224d,0xD6878EB6),
	sceKernelCallUserIntrHandler(0x224e,0xF4454E44),
	sceKernelGetCpuClockCounter(0x224f,0x30C08374),
	sceKernelGetCpuClockCounterWide(0x2250,0x35634A64),
	_sceKernelGetCpuClockCounterLow(0x2251,0x2DC9709B),
	_sceKernelGetCpuClockCounterHigh(0x2252,0xE9E652A9),
	sceKernelSetPrimarySyscallHandler(0x2253,0x0FC68A56),
	sceKernelRegisterSystemCallTable(0x2254,0xF4D443F3),
	sceKernelQuerySystemCall(0x2255,0x8B61808B),
                /*sceMpeg */
        sceMpegQueryStreamOffset(0x2256,0x21FF80E4),
	sceMpegQueryStreamSize(0x2257,0x611E9E11),
	sceMpegInit(0x2258,0x682A619B),
	sceMpegFinish(0x2259,0x874624D6),
	sceMpegQueryMemSize(0x225a,0xC132E22F),
	sceMpegCreate(0x225b,0xD8C5F121),
	sceMpegDelete(0x225c,0x606A4649),
	sceMpegRegistStream(0x225d,0x42560F23),
	sceMpegUnRegistStream(0x225e,0x591A4AA2),
	sceMpegMallocAvcEsBuf(0x225f,0xA780CF7E),
	sceMpegFreeAvcEsBuf(0x2260,0xCEB870B1),
	sceMpegQueryAtracEsSize(0x2261,0xF8DCB679),
	sceMpegQueryPcmEsSize(0x2262,0xC02CF6B5),
	sceMpegInitAu(0x2263,0x167AFD9E),
	sceMpegChangeGetAvcAuMode(0x2264,0x234586AE),
	sceMpegChangeGetAuMode(0x2265,0x9DCFB7EA),
	sceMpegGetAvcAu(0x2266,0xFE246728),
	sceMpegGetPcmAu(0x2267,0x8C1E027D),
	sceMpegGetAtracAu(0x2268,0xE1CE83A7),
	sceMpegFlushStream(0x2269,0x500F0429),
	sceMpegFlushAllStream(0x226a,0x707B7629),
	sceMpegAvcDecode(0x226b,0x0E3C2E9D),
	sceMpegAvcDecodeDetail(0x226c,0x0F6C18D7),
	sceMpegAvcDecodeMode(0x226d,0xA11C7026),
	sceMpegAvcDecodeStop(0x226e,0x740FCCD1),
	sceMpegAvcDecodeFlush(0x226f,0x4571CC64),
	sceMpegAvcQueryYCbCrSize(0x2270,0x211A057C),
	sceMpegAvcInitYCbCr(0x2271,0x67179B1B),
	sceMpegAvcDecodeYCbCr(0x2272,0xF0EB1125),
	sceMpegAvcDecodeStopYCbCr(0x2273,0xF2930C9C),
	sceMpegAvcCsc(0x2274,0x31BD0272),
	sceMpegAtracDecode(0x2275,0x800C44DF),
	sceMpegRingbufferQueryMemSize(0x2276,0xD7A29F46),
	sceMpegRingbufferConstruct(0x2277,0x37295ED8),
	sceMpegRingbufferDestruct(0x2278,0x13407F13),
	sceMpegRingbufferPut(0x2279,0xB240A59E),
	sceMpegRingbufferAvailableSize(0x227a,0xB5F6DC87),
	sceMpeg_11CAB459(0x227b,0x11CAB459),
	sceMpegNextAvcRpAu(0x227c,0x3C37A7A6),
	sceMpeg_B27711A8(0x227d,0xB27711A8),
	sceMpeg_D4DD6E75(0x227e,0xD4DD6E75),
	sceMpeg_C345DED2(0x227f,0xC345DED2),
	sceMpeg_AB0E9556(0x2280,0xAB0E9556),
	sceMpegAvcDecodeDetail2(0x2281,0xCF3547A2),
	sceMpeg_988E9E12(0x2282,0x988E9E12),
    		/*sceNetInet	*/
        sceNetInetInit(0x2283,0x17943399),
        sceNetInetTerm(0x2284,0xA9ED66B9),
        sceNetInetAccept(0x2285,0xDB094E1B),
        sceNetInetBind(0x2286,0x1A33F9AE),
        sceNetInetClose(0x2287,0x8D7284EA),
        sceNetInetCloseWithRST(0x2288,0x805502DD),
        sceNetInetConnect(0x2289,0x410B34AA),
        sceNetInetGetpeername(0x228a,0xE247B6D6),
        sceNetInetGetsockname(0x228b,0x162E6FD5),
        sceNetInetGetsockopt(0x228c,0x4A114C7C),
        sceNetInetListen(0x228d,0xD10A1A7A),
        sceNetInetPoll(0x228e,0xFAABB1DD),
        sceNetInetRecv(0x228f,0xCDA85C99),
        sceNetInetRecvfrom(0x2290,0xC91142E4),
        sceNetInetRecvmsg(0x2291,0xEECE61D2),
        sceNetInetSelect(0x2292,0x5BE8D595),
        sceNetInetSend(0x2293,0x7AA671BC),
        sceNetInetSendto(0x2294,0x05038FC7),
        sceNetInetSendmsg(0x2295,0x774E36F4),
        sceNetInetSetsockopt(0x2296,0x2FE71FE7),
        sceNetInetShutdown(0x2297,0x4CFE4E56),
        sceNetInetSocket(0x2298,0x8B7B220F),
        sceNetInetSocketAbort(0x2299,0x80A21ABD),
        sceNetInetGetErrno(0x229a,0xFBABE411),
        sceNetInetGetTcpcbstat(0x229b,0xB3888AD4),
        sceNetInetGetUdpcbstat(0x229c,0x39B0C7D3),
        sceNetInetInetAddr(0x229d,0xB75D5B0A),
        sceNetInetInetAton(0x229e,0x1BDF5D13),
        sceNetInetInetNtop(0x229f,0xD0792666),
        sceNetInetInetPton(0x22a0,0xE30B8C19),
                 /*sceImpose*/
                 sceImposeHomeButton(0x22a1,0x381BD9E7),
                 sceImposeSetHomePopup(0x22a2,0x5595A71A),
                 sceImposeGetHomePopup(0x22a3,0x0F341BE4),
                 sceImposeSetUMDPopup(0x22a4,0x72189C48),
                 sceImposeGetUMDPopup(0x22a5,0xE0887BC8),
                 sceImposeSetLanguageMode(0x22a6,0x36AA6E91),
                 sceImposeGetLanguageMode(0x22a7,0x24FD7BCF),
                 sceImposeGetBatteryIconStatus(0x22a8,0x8C943191),
                /*sceNetAdhoc*/
                 sceNetAdhocInit(0x22a9,0xE1D621D7),
                 sceNetAdhocTerm(0x22aa,0xA62C6F57),
                 sceNetAdhocPollSocket(0x22ab,0x7A662D6B),
                 sceNetAdhocSetSocketAlert(0x22ac,0x73BFD52D),
                 sceNetAdhocGetSocketAlert(0x22ad,0x4D2CE199),
                 sceNetAdhocPdpCreate(0x22ae,0x6F92741B),
                 sceNetAdhocPdpSend(0x22af,0xABED3790),
                 sceNetAdhocPdpRecv(0x22b0,0xDFE53E03),
                 sceNetAdhocPdpDelete(0x22b1,0x7F27BB5E),
                 sceNetAdhocGetPdpStat(0x22b2,0xC7C1FC57),
                 sceNetAdhocPtpOpen(0x22b3,0x877F6D66),
                 sceNetAdhocPtpConnect(0x22b4,0xFC6FC07B),
                 sceNetAdhocPtpListen(0x22b5,0xE08BDAC1),
                 sceNetAdhocPtpAccept(0x22b6,0x9DF81198),
                 sceNetAdhocPtpSend(0x22b7,0x4DA4C788),
                 sceNetAdhocPtpRecv(0x22b8,0x8BEA2B3E),
                 sceNetAdhocPtpFlush(0x22b9,0x9AC2EEAC),
                 sceNetAdhocPtpClose(0x22ba,0x157E6225),
                 sceNetAdhocGetPtpStat(0x22bb,0xB9685118),
                 sceNetAdhocGameModeCreateMaster(0x22bc,0x7F75C338),
                 sceNetAdhocGameModeCreateReplica(0x22bd,0x3278AB0C),
                 sceNetAdhocGameModeUpdateMaster(0x22be,0x98C204C8),
                 sceNetAdhocGameModeUpdateReplica(0x22bf,0xFA324B4E),
                 sceNetAdhocGameModeDeleteMaster(0x22c0,0xA0229362),
                 sceNetAdhocGameModeDeleteReplica(0x22c1,0x0B2228E9),
                 /* sceNetAdhocctl*/
                 sceNetAdhocctlInit(0x22c2,0xE26F226E),
                 sceNetAdhocctlTerm(0x22c3,0x9D689E13),
                 sceNetAdhocctlConnect(0x22c4,0x0AD043ED),
                 sceNetAdhocctlCreate(0x22c5,0xEC0635C1),
                 sceNetAdhocctlJoin(0x22c6,0x5E7F79C9),
                 sceNetAdhocctlScan(0x22c7,0x08FFF7A0),
                 sceNetAdhocctlDisconnect(0x22c8,0x34401D65),
                 sceNetAdhocctlAddHandler(0x22c9,0x20B317A0),
                 sceNetAdhocctlDelHandler(0x22ca,0x6402490B),
                 sceNetAdhocctlGetState(0x22cb,0x75ECD386),
                 sceNetAdhocctlGetAdhocId(0x22cc,0x362CBE8F),
                 sceNetAdhocctlGetPeerList(0x22cd,0xE162CB14),
                 sceNetAdhocctlGetAddrByName(0x22ce,0x99560ABE),
                 sceNetAdhocctlGetNameByAddr(0x22cf,0x8916C003),
                 sceNetAdhocctlGetParameter(0x22d0,0xDED9D28E),
                 sceNetAdhocctlGetScanInfo(0x22d1,0x81AEE1BE),
                 sceNetAdhocctlCreateEnterGameMode(0x22d2,0xA5C055CE),
                 sceNetAdhocctlJoinEnterGameMode(0x22d3,0x1FF89745),
                 sceNetAdhocctlExitGameMode(0x22d4,0xCF8E084D),
                 sceNetAdhocctlGetGameModeInfo(0x22d5,0x5A014CE0),
                 /*sceNetAdhocMatching*/
                 sceNetAdhocMatchingInit(0x22d6,0x2A2A1E07),
                 sceNetAdhocMatchingTerm(0x22d7,0x7945ECDA),
                 sceNetAdhocMatchingCreate(0x22d8,0xCA5EDA6F),
                 sceNetAdhocMatchingStart(0x22d9,0x93EF3843),
                 sceNetAdhocMatchingStop(0x22da,0x32B156B3),
                 sceNetAdhocMatchingDelete(0x22db,0xF16EAF4F),
                 sceNetAdhocMatchingSelectTarget(0x22dc,0x5E3D4B79),
                 sceNetAdhocMatchingCancelTarget(0x22de,0xEA3C6108),
                 sceNetAdhocMatchingSetHelloOpt(0x22df,0xB58E61B7),
                 sceNetAdhocMatchingGetHelloOpt(0x22e0,0xB5D96C2A),
                 sceNetAdhocMatchingGetMembers(0x22e1,0xC58BCD9E),
                 sceNetAdhocMatchingGetPoolMaxAlloc(0x22e2,0x40F8F435),
                 /*sceSasCore doesn't exist in firmware as default 2.70 and highter has that but
                  * many games ask for that so we load anyway.. */
                __sceSasCore(0x22e3, 0xA3589D81), // 2.71+
                __sceSasCoreWithMix(0x22e4, 0x50A14DFC), // 2.71+
                __sceSasGetEndFlag(0x22e5, 0x68A46B95), // 2.71+
                __sceSasSetVolume(0x22e6, 0x440CA7D8), // 2.71+
                __sceSasSetPitch(0x22e7, 0xAD84D37F), // 2.71+
                __sceSasSetVoice(0x22e8, 0x99944089), // 2.71+
                __sceSasSetNoise(0x22e9, 0xB7660A23), // 2.71+
                __sceSasSetADSR(0x22ea, 0x019B25EB), // 2.71+
                __sceSasSetADSRmode(0x22eb, 0x9EC3676A), // 2.71+
                __sceSasSetSL(0x22ec, 0x5F9529F6), // 2.71+
                __sceSasGetEnvelopeHeight(0x22ed, 0x74AE582A), // 2.71+
                __sceSasSetSimpleADSR(0x22ee, 0xCBCD4F79), // 2.71+
                __sceSasInit(0x22ef, 0x42778A9F), // 2.71+
                __sceSasSetKeyOff(0x22f0, 0xA0CF2FA4), // 2.71+
                __sceSasSetKeyOn(0x22f1, 0x76F01ACA), // 2.71+
                __sceSasRevVON(0x22f2, 0xF983B186), // 2.71+
                __sceSasRevEVOL(0x22f3, 0xD5A229C9), // 2.71+
                __sceSasRevType(0x22f4, 0x33D4AB37), // 2.71+
                __sceSasRevParam(0x22f5, 0x267A6DD2), // 2.71+
                __sceSasGetPauseFlag(0x22f6, 0x2C8E6AB3), // 2.71+
                __sceSasSetPause(0x22f7, 0x787D04D5), // 2.71+
                __sceSasSetTrianglarWave(0x22f8, 0xA232CBE6), // 2.71+
                __sceSasSetSteepWave(0x22f9, 0xD5EBBBCD), // 2.71+
                /*sceSasCore the following 4 appears to be on Firmware 3.00 and after but add it just in case */
                __sceSasGetGrain(0x22fa, 0xBD11B7C2), // 3.52+
                __sceSasSetGrain(0x22fb, 0xD1E0A01E), // 3.52+
                __sceSasGetOutputmode(0x22fc, 0xE175EF66), // 3.52+
                __sceSasSetOutputmode(0x22fd, 0xE855BF76), // 3.52+
                // where did 0x22fe go?
                sceNetSetDropRate(0x22ff, 0xFD8585E1), // 1.00+
                sceHttpsInit(0x2300, 0xE4D21302), // 1.00+
                sceNetApctlConnect(0x2301, 0xCFB957C6), // 1.00+
                sceNetResolverStartAtoN(0x2302, 0x629E2FB7), // 1.00+
                sceNetResolverStop(0x2303, 0x808F6063), // 1.00+
                sceUsbstorBootSetCapacity(0x2304, 0xE58818A8), // 1.00+
                sceKernelRegisterKprintfHandler(0x2305, 0x7CEB2C09), // 1.00+
                Kprintf(0x2306, 0x84F370BC), // 1.00+
                sceNetGetDropRate(0x2307, 0xC80181A2), // 1.00+
                sceCccSetTable(0x2308, 0xB4D1CBBF), // 1.00+
                sceNetApDialogDummyInit(0x2309, 0xBB73FF67), // 1.00+
                sceNetApDialogDummyConnect(0x230a, 0x3811281E), // 1.00+
                sceNetApDialogDummyGetState(0x230b, 0xCA9BE5BF), // 1.00+
                sceNetApDialogDummyTerm(0x230c, 0xF213BE65), // 1.00+
                sceParseHttpResponseHeader(0x230d, 0xAD7BFDEF), // 1.00+

                 /* FAKE MAPPING!! for Final Fantasy checks*/
                 ModuleMgrForUser_8F2DF740(0x3000,0x8f2df740), // fw 3.52 or less?
                 sceKernelSetCompiledSdkVersion(0x3001,0x7591c7db), // fw 2.5
                 sceKernelSetCompilerVersion(0x3002,0xf77d77cb), // fw 2.5
                 sceAtracIsSecondBufferNeeded(0x3003,0xeca32a99), // fw 2.5
                 sceUtilityLoadModule(0x3004,0x2a2b3de0), // fw 3.52 or less?
                 sceUtilityMsgDialogAbort(0x3005,0x4928bd96), // fw 3.52 or less?
                 sceUtilityUnloadModule(0x3006,0xe49bfe92), // fw 3.52 or less?
                 /* more fake mapping */
                 sceUmdReplaceProhibit(0x3007,0x87533940),//umd function 2.00 +
                 sceNetAdhocMatchingCancelTargetWithOpt(0x3008,0x8f58bedf), //2.50+
                 sceFontFindOptimumFont(0x3009,0x99ef33c),//2.00 +
                 sceFontGetFontInfo(0x300a,0xda7535e),//2.00+
                 sceFontClose(0x300b,0x3aea8cb6), //2.00+
                 sceFontDoneLib(0x300c,0x574b6fbc),//2.00+
                 sceFontNewLib(0x300d,0x67f17ed7),//2.00+
                 sceFontOpen(0x300e,0xa834319d),//2.00+
                 sceFontGetCharGlyphImage_Clip(0x300f,0xca1e6945),//2.00+
                 sceFontGetCharInfo(0x3010,0xdcc80c2f),//2.00+
                 sceUtilityLoadNetModule(0x3011,0x1579a159),//2.00+
                 sceUtilityUnloadNetModule(0x3012,0x64d50c56),//2.00+
                 sceUtilityLoadAvModule(0x3013,0xc629af26),//2.70+
                 sceFontGetCharGlyphImage(0x3014,0x980f4895),//2.00 , 2.01 and 3.00 +
                 sceKernelDeleteMutex(0x3015,0xf8170fbe),//2.70+
                 sceKernelUnlockMutex(0x3016,0x6b30100f),//2.70+
                 sceKernelLockMutex(0x3017,0xb011b11f),//2.70+

                 /*found in wipeout pure */
                 sceKernelIsCpuIntrEnable(0x3018,0xb55249d2),//1.00+
                 sceNetResolverInit(0x3019,0xf3370e61),//1.00+
                 sceNetResolverTerm(0x301a,0x6138194a),//1.00+
                 sceNetApctlInit(0x301b,0xe2f91f9b),//1.00+
                 sceNetApctlTerm(0x301c,0xb3edd0ec),//1.00+
                 sceNetApctlGetInfo(0x301d,0x2befdf23),//1.00+
                 sceNetApctlAddHandler(0x301e,0x8abadd51),//1.00+
                 sceNetApctlDelHandler(0x301f,0x5963991b),//1.00+
                 sceNetApctlDisconnect(0x3020,0x24fe91a1),//1.00+
                 sceNetApctlGetState(0x3021,0x5deac81b),//1.00+
                 sceHttpInit(0x3022,0xab1abe07),//1.00+
                 sceHttpEnd(0x3023,0xd1c8945e),//1.00+
                 sceHttpCreateTemplate(0x3024,0x9b1f1f36),//1.00+
                 sceHttpDeleteTemplate(0x3025,0xfcf8c055),//1.00+
                 sceHttpCreateConnectionWithURL(0x3026,0xcdf8ecb9),//1.00+
                 sceHttpDeleteConnection(0x3027,0x5152773b),//1.00+
                 sceHttpCreateRequestWithURL(0x3028,0xb509b09e),//1.00+
                 sceHttpDeleteRequest(0x3028,0xa5512e01),//1.00+
                 sceHttpEnableKeepAlive(0x3029,0x78a0d3ec),//1.00+
                 sceHttpDisableRedirect(0x302a,0x1a0ebb69),//1.00+
                 sceHttpDisableAuth(0x302b,0xae948fee),//1.00+
                 sceHttpDisableCookie(0x302c,0x0b12abfb),//1.00+
                 sceHttpInitCache(0x302d,0xa6800c34),//1.00+
                 sceHttpEndCache(0x302e,0x78b54c09),//1.00+
                 sceHttpDisableCache(0x302f,0xccbd167a),//1.00+
                 sceHttpSendRequest(0x3030,0xbb70706f),//1.00+
                 sceHttpAbortRequest(0x3031,0xc10b6bd9),//1.00+
                 sceHttpGetStatusCode(0x3032,0x4cc7d78f),//1.00+
                 sceHttpGetAllHeader(0x3033,0xdb266ccf),//1.00+
                 sceHttpReadData(0x3034,0xedeeb999),//1.00+
                 sceHttpSetAuthInfoCB(0x3035,0x2a6c3296),//1.00+
                 sceHttpDeleteHeader(0x3036,0x15540184),//1.00+
                 sceHttpAddExtraHeader(0x3037,0x3eaba285),//1.00+
                 sceHttpGetNetworkErrno(0x3038,0xd081ec8f),//1.00+
                 sceHttpsInitWithPath(0x3039,0x68ab0f86),//1.00+
                 sceHttpsLoadDefaultCert(0x303a,0x87797bdd),//1.00+
                 sceHttpsEnd(0x303b,0xf9d8eb63),//1.00+
                 sceParseHttpStatusLine(0x303c,0x8077a433),//1.00+
                 sceUriParse(0x303d,0x568518c9),//1.00+
                 sceSslInit(0x303e,0x957ecbe2),//1.00+
                 sceSslEnd(0x303f,0x191cdeff),//1.00+
                 /*world championship poker 2 */
                 sceNetResolverCreate(0x3040,0x244172af), //1.00+
                 sceNetResolverDelete(0x3041,0x94523e09),//1.00+
                 sceNetResolverStartNtoA(0x3042,0x224c5f44),//1.00+
                 /*vampire chronicles */
                 sceFontGetNumFontList(0x3043,0x27f6e642),//1.00+
                 sceFontGetFontList(0x3044,0xbc75d85b),//1.00+
		         sceKernelCheckPspConfigFunction(0x3045, 0xACE23476),
		         sceKernelCheckExecFileFunction(0x3046, 0x7BE1421C),
 		         sceKernelProbeExecutableObjectFunction(0x3047, 0xBF983EF2),
		         sceKernelLoadExecutableObjectFunction(0x3048, 0x7068E6BA),
		         sceKernelApplyElfRelSectionFunction(0x3049, 0xB4D6FECC),
		         sceKernelApplyPspRelSectionFunction(0x3050, 0x54AB2675),
		         sceKernelDcacheWBinvAllFunction(0x3051, 0x2952F5AC),
		         sceKernelIcacheClearAllFunction(0x3052, 0xD8779AC6),
		         sceKernelRegisterLibraryFunction(0x3053, 0x99A695F0),
		         sceKernelRegisterLibraryForUserFunction(0x3054, 0x5873A31F),
		         sceKernelReleaseLibraryFunction(0x3055, 0x0B464512),
		         sceKernelCanReleaseLibraryFunction(0x3056, 0x9BAF90F6),
		         sceKernelLinkLibraryEntriesFunction(0x3057, 0x0E760DBA),
		         sceKernelLinkLibraryEntriesForUserFunction(0x3058, 0x0DE1F600),
		         sceKernelUnLinkLibraryEntriesFunction(0x3059, 0xDA1B09AA),
		         sceKernelQueryLoadCoreCBFunction(0x3060, 0xC99DD47A),
		         sceKernelSetBootCallbackLevelFunction(0x3061, 0x616FCCCD),
		         sceKernelGetModuleFromUIDFunction(0x3062, 0x52A86C21),
		         sceKernelCreateModuleFunction(0x3063, 0xCD0F3BAC),
		         sceKernelDeleteModuleFunction(0x3064, 0x6B2371C2),
		         sceKernelAssignModuleFunction(0x3065, 0x8D8A8ACE),
		         sceKernelCreateAssignModuleFunction(0x3066, 0xAFF947D4),
		         sceKernelRegisterModuleFunction(0x3067, 0xAE7C6E76),
		         sceKernelReleaseModuleFunction(0x3068, 0x74CF001A),
		         sceKernelFindModuleByNameFunction(0x3069, 0xCF8A41B1),
		         sceKernelFindModuleByAddressFunction(0x306a, 0xFB8AE27D),
		         sceKernelFindModuleByUIDFunction(0x306b, 0xCCE4A157),
		         sceKernelGetModuleListWithAllocFunction(0x306c, 0x929B5C69),
		         sceKernelGetModuleIdListForKernelFunction(0x306d, 0x05D915DB),

                 sceKernelSetCompiledSdkVersion380_390(0x306e,0x315AD3A0), //3.90+ or lower


                // TODO remove when we support exports, MPEG.PRX
                sceVideocodecGetEDRAM(0x306f,0x2D31F5B1), //1.50+
                sceVideocodecReleaseEDRAM(0x3070,0x4F160BF4), //1.50+
                sceVideocodecOpen(0x3071,0xC01EC829), //1.50+
                sceVideocodecInit(0x3072,0x17099F0A), //1.50+
                sceVideocodecDecode(0x3073,0xDBA273FA), //1.50+
                sceVideocodecStop(0x3074,0xA2F0564E), //1.50+
                sceVideocodecDelete(0x3075,0x307E6E1C), //1.50+
                sceVideocodecSetMemory(0x3076,0x745A7B7A), //1.50+
                sceVideocodecScanHeader(0x3077,0x2F385E7F), //1.50+
                sceVideocodecGetVersion(0x3078,0x26927D19), //1.50+

                // TODO remove when we support exports, MPEG.PRX
                sceAudiocodeCheckNeedMem(0x3079,0x9D3F790C), //1.50+
                sceAudiocodecInit(0x307a,0x5B37EB1D), //1.50+
                sceAudiocodecDecode(0x307b,0x70A703F8), //1.50+
                sceAudiocodecGetInfo(0x307c,0x8ACA11D5), //1.50+
                sceAudiocodec_6CD2A861(0x307d,0x6CD2A861), //1.00 - 2.50
                sceAudiocodec_59176A0F(0x307e,0x59176A0F), //1.50+
                sceAudiocodecGetEDRAM(0x307f,0x3A20A200), //1.50+
                sceAudiocodecReleaseEDRAM(0x3080,0x29681260), //1.50+

                // serial
                sceHprmEnd(0x3081, 0x588845DA), //1.50+
                sceSysconCtrlHRPower(0x3082, 0x44439604), //1.50+
                sceSysregUartIoEnable(0x3083, 0x7FD7A631), //1.50+
                sceKernelRegisterDebugPutchar(0x3084, 0xE146606D), //1.50+
                sceKernelDipswAll(0x3085, 0xD636B827), //1.50+

                sceAudioOutput2Reserve(0x3086, 0x01562BA3), //2.50+ or lower
                sceAudioOutput2Release(0x3087, 0x43196845), //2.50+ or lower
                sceAudioOutput2OutputBlocking(0x3088, 0x2D53F36E), //2.50+ or lower
                sceAudioOutput2ChangeLength(0x3089, 0x63F2889C), //2.50+ or lower
                sceAudioOutput2GetRestSample(0x308a, 0x647CEF33), //2.50+ or lower

                scePower_EBD177D6(0x308b, 0xEBD177D6), //3.52+ or lower
                sceKernelSetCompiledSdkVersion370(0x308c, 0x342061E5), //3.72+ or lower
                __sceSasGetAllEnvelopeHeights(0x308d, 0x07F58C24), //3.72+ or lower, 3.95+ in libdoc


                sceKernelSetCompiledSdkVersion395(0x308e, 0xEBD5C3E6), // 3.95+ or lower
                sceAtracReinit(0x308f, 0x132F1ECA), // 2.50+ or lower
                sceAtrac3plus_2DD3E298(0x3090, 0x2DD3E298), // 2.50+ or lower
                sceAtracGetOutputChannel(0x3091, 0xB3B5D042), // 2.50+ or lower

                sceUtilityHtmlViewerInitStart(0x3092, 0xCDC3AA41), // 2.00+
                sceUtilityHtmlViewerUpdate(0x3093, 0x05AFB9E4), // 2.00+
                sceUtilityHtmlViewerGetStatus(0x3094, 0xBDA7D894), // 2.00+
                sceUtilityHtmlViewerShutdownStart(0x3095, 0xF5CE1134), // 2.00+
                sceHttpSaveSystemCookie(0x3096, 0x76D1363B), // 2.00+
                sceHttpLoadSystemCookie(0x3097, 0xF1657B22), // 2.00+

                sceFontOpenUserMemory(0x3095, 0xBB8E7FE6), // 2.00+
                sceUtilityUnloadAvModule(0x3096, 0xF7D8D092), // 2.71+

                sceFontSetAltCharacterCode(0x3097, 0xEE232411), // 2.00+
                sceFontGetCharImageRect(0x3098, 0x5C3E4A9E), // 2.00+ or lower
                sceNetInetGetPspError(0x3099, 0x8CA3A97E), // 2.00+
                sceNetAdhocctlGetPeerInfo(0x309a, 0x8DB83FDC), // 2.00+
                sceUtilityLoadUsbModule(0x309b, 0x0D5BC6D2), // 2.71+ or lower
                sceUtilityUnloadUsbModule(0x309c, 0xF64910F0), // 2.71+ or lower
                sceFontPointToPixelH(0x309d, 0x472694CD), // 2.00+ or lower
                sceFontGetFontInfoByIndexNumber(0x309e, 0x5333322D), // 2.00+ or lower
                sceNetAdhocMatchingAbortSendData(0x309f, 0xEC19337D), // 3.52+ or lower
                sceNetAdhocMatchingSendData(0x30a0, 0xF79472D7), // 3.52+ or lower
                sceKernelTryLockMutex(0x30a1, 0x0DDCD2C9), // 2.71+ or lower
                sceKernelLockMutexCB(0x30a2, 0x5BF4DD27), // 2.71+ or lower
                sceKernelCancelMutex(0x30a3, 0x87D9223C), // 2.71+ or lower
                sceKernelReferMutexStatus(0x30a4, 0xA9C2CB9A), // 2.71+ or lower

                sceAtracSetMOutHalfwayBuffer(0x30a5, 0x5CF9D852), // 2.50+ or lower
                sceFontSetResolution(0x30a6, 0x48293280), // 2.00+
                sceKernelCreateMutex(0x30a7,0xb7d098c6),//2.70+,

                sceMp3ReserveMp3Handle(0x30a8, 0x07EC321A), // 3.95+
                sceMp3NotifyAddStreamData(0x30a9, 0x0DB149F4), // 3.95+
                sceMp3ResetPlayPosition(0x30aa, 0x2A368661), // 3.95+
                sceMp3InitResource(0x30ab, 0x35750070), // 3.95+
                sceMp3TermResource(0x30ac, 0x3C2FA058), // 3.95+
                sceMp3SetLoopNum(0x30ad, 0x3CEF484F), // 3.95+
                sceMp3Init(0x30ae, 0x44E07129), // 3.95+
                sceMp3GetMp3ChannelNum(0x30af, 0x7F696782), // 3.95+
                sceMp3GetSamplingRate(0x30b0, 0x8F450998), // 3.95+
                sceMp3GetInfoToAddStreamData(0x30b1, 0xA703FE0F), // 3.95+
                sceMp3Decode(0x30b2, 0xD021C0FB), // 3.95+
                sceMp3CheckStreamDataNeeded(0x30b3, 0xD0A56296), // 3.95+
                sceMp3ReleaseMp3Handle(0x30b4, 0xF5478233), // 3.95+

                scePsmfPlayerCreate(0x30b5, 0x235D8787), // umd game only 2.60+
                scePsmfPlayerDelete(0x30b6, 0x9B71A274), // umd game only 2.60+
                scePsmfPlayerSetPsmf(0x30b7, 0x3D6D25A9), // umd game only 2.60+
                scePsmfPlayerReleasePsmf(0x30b8, 0xE792CD94), // umd game only 2.60+
                scePsmfPlayerStart(0x30b9, 0x95A84EE5), // umd game only 2.60+
                scePsmfPlayer_3EA82A4B(0x30ba, 0x3EA82A4B), // umd game only 2.60+
                scePsmfPlayerStop(0x30bb, 0x1078C008), // umd game only 2.60+
                scePsmfPlayerUpdate(0x30bc, 0xA0B8CA55), // umd game only 2.60+
                scePsmfPlayer_46F61F8B(0x30bd, 0x46F61F8B), // umd game only 2.60+
                scePsmfPlayer_B9848A74(0x30be, 0xB9848A74), // umd game only 2.60+
                scePsmfPlayer_F8EF08A6(0x30bf, 0xF8EF08A6), // umd game only 2.60+
                scePsmfPlayer_DF089680(0x30c0, 0xDF089680), // umd game only 2.60+
                scePsmfPlayer_1E57A8E7(0x30c1, 0x1E57A8E7), // umd game only 2.60+
                scePsmfPlayer_2BEB1569(0x30c2, 0x2BEB1569), // umd game only 2.60+

                sceKernelCreateLwMutex(0x30c3, 0x19CFF145), // 3.95+
                sceKernelReferLwMutexStatusByID(0x30c4, 0x4C145944), // 3.95+
                sceKernelDeleteLwMutex(0x30c5, 0x60107536), // 3.95+

                sceKernelUnlockLwMutex(0x30c6, 0x15B6446B), // 3.95+
                sceKernelLockLwMutexCB(0x30c7, 0x1FC64E09), // 3.95+
                sceKernelLockLwMutex(0x30c8, 0xBEA46419), // 3.95+
                sceKernelReferLwMutexStatus(0x30c9, 0xC1734599), // 3.95+
                sceKernelTryLockLwMutex(0x30ca, 0xDC692EE3), // 3.95+

                sceMp3GetSumDecodedSample(0x30cb, 0x354D27EA), // 3.95+
                sceMp3GetBitRate(0x30cc, 0x87677E40), // 3.95+
                sceMp3GetMaxOutputSample(0x30cd, 0x87C263D1), // 3.95+
                sceMp3GetLoopNum(0x30ce, 0xD8F54A51), // 3.95+

                sceMpegQueryUserdataEsSize(0x30cf, 0xC45C99CC), // 2.71+
                sceMpegQueryUserdataEsSize_vsh(0x30cf, 0x01977054), // 2.71+ fiddled so the syscall is the same as the non-vsh version
                sceUtilityGetNetParamLatestID(0x30d0, 0x4FED24D8), // 2.00+

                sceAtracSetAA3DataAndGetID(0x30d1, 0x5622B7C1), // 2.71+
                sceUtilityScreenshotInitStart(0x30d2, 0x0251B134), // 3.95+
                sceUtilityScreenshotUpdate(0x30d3, 0xAB083EA9), // 3.95+
                sceUtilityScreenshotGetStatus(0x30d4, 0xD81957B7), // 3.95+
                sceUtilityScreenshotShutdownStart(0x30d5, 0xF9E0008C), // 3.95+
                inflateEnd(0x30d6, 0x461C7724), // 2.71+
                inflateInit_(0x30d7, 0x18CB51AB), // 2.71+
                inflate(0x30d8, 0x216D1BF1), // 2.71+

                __sceSasSetVoicePCM(0x30d9, 0xE1CD9561), // 5.00+
                LoadExecForUser_362A956B(0x30da, 0x362A956B), // 5.00+
                SysMemUserForUser_FE707FDF(0x30db, 0xFE707FDF), // 3.52+
                SysMemUserForUser_50F61D8A(0x30dc, 0x50F61D8A), // 3.52+
                SysMemUserForUser_91DE343C(0x30dd, 0x91DE343C), // 5.00+ sceKernelSetCompiledSdkVersionXXX
                SysMemUserForUser_DB83A952(0x30de, 0xDB83A952), // 3.52+
                SysMemUserForUser_D8DE5C1E(0x30df, 0xD8DE5C1E), // 5.00+
                sceHttpGetContentLength(0x30e0, 0x0282A3BD), // 1.00+
                sceHttpSetResolveRetry(0x30e1, 0x03D9526F), // 2.00+
                sceHttpSetRecvTimeOut(0x30e2, 0x1F0FC3E3), // 1.00+
                sceHttpSetResolveTimeOut(0x30e3, 0x47940436), // 1.00+
                sceHttpSetConnectTimeOut(0x30e4, 0x8ACD1F73), // 1.00+
                sceHttpSetSendTimeOut(0x30e5, 0x9988172D), // 1.00+
                sceRtcGetLastAdjustedTime(0x30e6, 0x62685E98), // 2.00+

                sceKernelRegisterDefaultExceptionHandler(0x30e7, 0x565C0B0E), // 1.00 to 3.52, gone in 3.95+
                sceKernelGetModel(0x30e8, 0x6373995D), // 3.52+
                sceImposeGetParam(0x30e9, 0x531C9778), // 1.00+
                sceImposeSetParam(0x30ea, 0x810FB7FB), // 1.00+
                sceDisplaySetBrightness(0x30eb, 0x9E3C6DC6), // 1.00+
                sceDisplayEnable(0x30ec, 0x432D133F), // 1.00+
                sceDisplayDisable(0x30ed, 0x681EE6A7), // 1.00+
                sceSysregMeBusClockEnable(0x30ee, 0x44F6CDA7), // 1.00+
                sceSysregMeBusClockDisable(0x30ef, 0x158AD4FC), // 1.00+
                unknown_CC9ADCF8(0x30f0, 0xCC9ADCF8), // 3.71+? homebrew psplayer 2.0 (mp3 player)
                sceFontFlush(0x30f1, 0x02D7F94B), // 2.00+
                sceUtilityInstallInitStart(0x30f2, 0x1281DA8E), // 2.71+
                sceUtilityInstallShutdownStart(0x30f3, 0x5EF1C24A), // 2.71+
                sceUtilityInstallUpdate(0x30f4, 0xA03D29BA), // 2.71+
                sceUtilityInstallGetStatus(0x30f5, 0xC4700FA3), // 2.71+


                // We choose to start HLEModuleManager at 0x4000

                // CFW, we choose to start at 0x5000
                kuKernelLoadModule(0x5000, 0x4C25EA72),
                systemctrl_user_577AF198(0x5001, 0x577AF198),
                systemctrl_user_75643FCA(0x5002, 0x75643FCA),
                systemctrl_user_ABA7F1B0(0x5003, 0xABA7F1B0),
                systemctrl_user_16C3B7EE(0x5004, 0x16C3B7EE),
                systemctrl_user_85B520C6(0x5005, 0x85B520C6),

                // Module magic, we choose to start at 0x6000
                module_bootstart(0x6000, 0xD3744BE0),
                module_reboot_before(0x6001, 0x2F064FA6),
                module_reboot_phase(0x6002, 0xADF12745),
                module_start(0x6003, 0xD632ACDB),
                module_stop_1(0x6004, 0xCEE8593C),
                module_stop_2(0x6005, 0xF01D73A7),
                module_0F7C276C(0x6006, 0x0F7C276C),

                hleDummy(0x70000, 0xcdcdcdcd); // got bored of editing , and ; so leave this entry at the end of the enum


            //implement syscall
            private int syscall;
            private int NID;
            calls(int syscall,int NID)
            {
                this.syscall=syscall;
                this.NID=NID;
            }
            public int getSyscall()
            {
                return syscall;
            }
            public int getNID()
            {
                return NID;
            }
   }
}
