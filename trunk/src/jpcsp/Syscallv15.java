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
package jpcsp;

/**
 *
 * @author George
 */
public class Syscallv15 {
   static int syscalls[][] = {
       {0x2000,0xca04a2b9}, //sceKernelRegisterSubIntrHandler
       {0x2001,0xd61e6961}, //sceKernelReleaseSubIntrHandler
       {0x2002,0xfb8e22ec}, //sceKernelEnableSubIntr
       {0x2003,0x8a389411}, //sceKernelDisableSubIntr
       {0x2004,0x5cb5a78b}, //sceKernelSuspendSubIntr
       {0x2005,0x7860e0dc}, //sceKernelResumeSubIntr
       {0x2006,0xfc4374b8},//sceKernelIsSubInterruptOccurred
       {0x2007,0xd2e8363f},//QueryIntrHandlerInfo
       {0x2008,0xeee43f47},//sceKernelRegisterUserSpaceIntrStack
       {0x2009,0x6e9ea350},//_sceKernelReturnFromCallback
       {0x200a,0x0c106e53},//sceKernelRegisterThreadEventHandler
       {0x200b,0x72f3c145},//sceKernelReleaseThreadEventHandler
       {0x200c,0x369eeb6b},//sceKernelReferThreadEventHandlerStatus
       {0x200d,0xe81caf8f},//sceKernelCreateCallback
       {0x200e,0xedba5844},//sceKernelDeleteCallback
       {0x200f,0xc11ba8c4},//sceKernelNotifyCallback
       {0x2010,0xba4051d6},//sceKernelCancelCallback
       {0x2011,0x2a3d44ff},//sceKernelGetCallbackCount
       {0x2012,0x349d6d6c},//sceKernelCheckCallback
       {0x2013,0x730ed8bc},//sceKernelReferCallbackStatus
       {0x2014,0x9ace131e},//sceKernelSleepThread
       {0x2015,0x82826f70},//sceKernelSleepThreadCB
       {0x2016,0xd59ead2f},//sceKernelWakeupThread
       {0x2017,0xfccfad26},//sceKernelCancelWakeupThread
       {0x2018,0x9944f31f},//sceKernelSuspendThread
       {0x2019,0x75156e8f},//sceKernelResumeThread
       {0x201a,0x278c0df5},//sceKernelWaitThreadEnd
       {0x201b,0x840e8133},//sceKernelWaitThreadEndCB
       {0x201c,0xceadeb47},//sceKernelDelayThread
       {0x201d,0x68da9e36},//sceKernelDelayThreadCB
       {0x201e,0xbd123d9e},//sceKernelDelaySysClockThread
       {0x201f,0x1181e963},//sceKernelDelaySysClockThreadCB
       {0x2020,0xd6da4ba1},//sceKernelCreateSema
       {0x2021,0x28b6489c},//sceKernelDeleteSema
       {0x2022,0x3f53e640},//sceKernelSignalSema
       {0x2023,0x4e3a1105},//sceKernelWaitSema
       {0x2024,0x6d212bac},//sceKernelWaitSemaCB
       {0x2025,0x58b1f937},//sceKernelPollSema
       {0x2026,0x8ffdf9a2},//sceKernelCancelSema
       {0x2027,0xbc6febc5},//sceKernelReferSemaStatus
       {0x2028,0x55c20a00},//sceKernelCreateEventFlag
       {0x2029,0xef9e4c70},//sceKernelDeleteEventFlag
       {0x202a,0x1fb15a32},//sceKernelSetEventFlag
       {0x202b,0x812346e4},//sceKernelClearEventFlag
       {0x202c,0x402fcf22},//sceKernelWaitEventFlag
       {0x202d,0x328c546a},//sceKernelWaitEventFlagCB
       {0x202e,0x30fd48f0},//sceKernelPollEventFlag
       {0x202f,0xcd203292},//sceKernelCancelEventFlag
       {0x2030,0xa66b0120},//sceKernelReferEventFlagStatus
       {0x2031,0x8125221d},//sceKernelCreateMbx
       {0x2032,0x86255ada},//sceKernelDeleteMbx
       {0x2033,0xe9b3061e},//sceKernelSendMbx
       {0x2034,0x18260574},//sceKernelReceiveMbx
       {0x2035,0xf3986382},//sceKernelReceiveMbxCB
       {0x2036,0x0d81716a},//sceKernelPollMbx
       {0x2037,0x87d4dd36},//sceKernelCancelReceiveMbx
       {0x2038,0xa8e8c846},//sceKernelReferMbxStatus
       {0x2039,0x7c0dc2a0},//sceKernelCreateMsgPipe
       {0x203a,0xf0b7da1c},//sceKernelDeleteMsgPipe
       {0x203b,0x876dbfad},//sceKernelSendMsgPipe
       {0x203c,0x7c41f2c2},//sceKernelSendMsgPipeCB
       {0x203d,0x884c9f90},//sceKernelTrySendMsgPipe
       {0x203e,0x74829b76},//sceKernelReceiveMsgPipe
       {0x203f,0xfbfa697d},//sceKernelReceiveMsgPipeCB
       {0x2040,0xdf52098f},//sceKernelTryReceiveMsgPipe
       {0x2041,0x349b864d},//sceKernelCancelMsgPipe
       {0x2042,0x33be4024},//sceKernelReferMsgPipeStatus
       {0x2043,0x56c039b5},//sceKernelCreateVpl
       {0x2044,0x89b3d48c},//sceKernelDeleteVpl
       {0x2045,0xbed27435},//sceKernelAllocateVpl
       {0x2046,0xec0a693f},//sceKernelAllocateVplCB
       {0x2047,0xaf36d708},//sceKernelTryAllocateVpl
       {0x2048,0xb736e9ff},//sceKernelFreeVpl
       {0x2049,0x1d371b8a},//sceKernelCancelVpl
       {0x204a,0x39810265},//sceKernelReferVplStatus
       {0x204b,0xc07bb470},//sceKernelCreateFpl
       {0x204c,0xed1410e0},//sceKernelDeleteFpl
       {0x204d,0xd979e9bf},//sceKernelAllocateFpl
       {0x204e,0xe7282cb6},//sceKernelAllocateFplCB
       {0x204f,0x623ae665},//sceKernelTryAllocateFpl
       {0x2050,0xf6414a71},//sceKernelFreeFpl
       {0x2051,0xa8aa591f},//sceKernelCancelFpl
       {0x2052,0xd8199e4c},//sceKernelReferFplStatus
       {0x2053,0x0e927aed},//ThreadManForUser_0E927AED
       {0x2054,0x110dec9a},//sceKernelUSec2SysClock
       {0x2055,0xc8cd158c},//sceKernelUSec2SysClockWide
       {0x2056,0xba6b92e2},//sceKernelSysClock2USec
       {0x2057,0xe1619d7c},//sceKernelSysClock2USecWide
       {0x2058,0xdb738f35},//sceKernelGetSystemTime
       {0x2059,0x82bc5777},//sceKernelGetSystemTimeWide
       {0x205a,0x369ed59d},//sceKernelGetSystemTimeLow
       {0x205b,0x6652b8ca},//sceKernelSetAlarm
       {0x205c,0xb2c25152},//sceKernelSetSysClockAlarm
       {0x205d,0x7e65b999},//sceKernelCancelAlarm
       {0x205e,0xdaa3f564},//sceKernelReferAlarmStatus
       {0x205f,0x20fff560},//sceKernelCreateVTimer
       {0x2060,0x328f9e52},//sceKernelDeleteVTimer
       {0x2061,0xb3a59970},//sceKernelGetVTimerBase
       {0x2062,0xb7c18b77},//sceKernelGetVTimerBaseWide
       {0x2063,0x034a921f},//sceKernelGetVTimerTime
       {0x2064,0xc0b3ffd2},//sceKernelGetVTimerTimeWide
       {0x2065,0x542ad630},//sceKernelSetVTimerTime
       {0x2066,0xfb6425c3},//sceKernelSetVTimerTimeWide
       {0x2067,0xc68d9437},//sceKernelStartVTimer
       {0x2068,0xd0aeee87},//sceKernelStopVTimer
       {0x2069,0xd8b299ae},//sceKernelSetVTimerHandler
       {0x206a,0x53b00e9a},//sceKernelSetVTimerHandlerWide
       {0x206b,0xd2d615ef},//sceKernelCancelVTimerHandler
       {0x206c,0x5f32beaa},//sceKernelReferVTimerStatus
       {0x206d,0x446d8de6},//sceKernelCreateThread
       {0x206e,0x9fa03cd3},//sceKernelDeleteThread
       {0x206f,0xf475845d},//sceKernelStartThread
       {0x2070,0x532a522e},//_sceKernelExitThread
       {0x2071,0xaa73c935},//sceKernelExitThread
       {0x2072,0x809ce29b},//sceKernelExitDeleteThread
       {0x2073,0x616403ba},//sceKernelTerminateThread
       {0x2074,0x383f7bcc},//sceKernelTerminateDeleteThread
       {0x2075,0x3ad58b8c},//sceKernelSuspendDispatchThread
       {0x2076,0x27e22ec2},//sceKernelResumeDispatchThread
       {0x2077,0xea748e31},//sceKernelChangeCurrentThreadAttr
       {0x2078,0x71bc9871},//sceKernelChangeThreadPriority
       {0x2079,0x912354a7},//sceKernelRotateThreadReadyQueue
       {0x207a,0x2c34e053},//sceKernelReleaseWaitThread
       {0x207b,0x293b45b8},//sceKernelGetThreadId
       {0x207c,0x94aa61ee},//sceKernelGetThreadCurrentPriority
       {0x207d,0x3b183e26},//sceKernelGetThreadExitStatus
       {0x207e,0xd13bde95},//sceKernelCheckThreadStack
       {0x207f,0x52089ca1},//sceKernelGetThreadStackFreeSize
       {0x2080,0x17c1684e},//sceKernelReferThreadStatus
       {0x2081,0xffc36a14},//sceKernelReferThreadRunStatus
       {0x2082,0x627e6f3a},//sceKernelReferSystemStatus
       {0x2083,0x94416130},//sceKernelGetThreadmanIdList
       {0x2084,0x57cf62dd},//sceKernelGetThreadmanIdType
       {0x2085,0x64d4540e},//sceKernelReferThreadProfiler
       {0x2086,0x8218b4dd},//sceKernelReferGlobalProfiler
       {0x2087,0x3251ea56},//sceIoPollAsync
       {0x2088,0xe23eec33},//sceIoWaitAsync
       {0x2089,0x35dbd746},//sceIoWaitAsyncCB
       {0x208a,0xcb05f8d6},//sceIoGetAsyncStat
       {0x208b,0xb293727f},//sceIoChangeAsyncPriority
       {0x208c,0xa12a0514},//sceIoSetAsyncCallback
       {0x208d,0x810c4bc3},//sceIoClose
       {0x208e,0xff5940b6},//sceIoCloseAsync
       {0x208f,0x109f50bc},//sceIoOpen
       {0x2090,0x89aa9906},//sceIoOpenAsync
       {0x2091,0x6a638d83},//sceIoRead
       {0x2092,0xa0b5a7c2},//sceIoReadAsync
       {0x2093,0x42ec03ac},//sceIoWrite
       {0x2094,0x0facab19},//sceIoWriteAsync
       {0x2095,0x27eb27b8},//sceIoLseek
       {0x2096,0x71b19e77},//sceIoLseekAsync
       {0x2097,0x68963324},//sceIoLseek32
       {0x2098,0x1b385d8f},//sceIoLseek32Async
       {0x2099,0x63632449},//sceIoIoctl
       {0x209a,0xe95a012b},//sceIoIoctlAsync
       {0x209b,0xb29ddf9c},//sceIoDopen
       {0x209c,0xe3eb004c},//sceIoDread
       {0x209d,0xeb092469},//sceIoDclose
       {0x209e,0xf27a9c51},//sceIoRemove
       {0x209f,0x06a70004},//sceIoMkdir
       {0x20a0,0x1117c65f},//sceIoRmdir
       {0x20a1,0x55f4717d},//sceIoChdir
       {0x20a2,0xab96437f},//sceIoSync
       {0x20a3,0xace946e8},//sceIoGetstat
       {0x20a4,0xb8a740f4},//sceIoChstat
       {0x20a5,0x779103a0},//sceIoRename
       {0x20a6,0x54f5fb11},//sceIoDevctl
       {0x20a7,0x08bd7374},//sceIoGetDevType
       {0x20a8,0xb2a628c1},//sceIoAssign
       {0x20a9,0x6d08a871},//sceIoUnassign
       {0x20aa,0xe8bc6571},//sceIoCancel
       {0x20ab,0x5c2be2cc},//IoFileMgrForUser_5C2BE2CC
       {0x20ac,0x3054d478},//sceKernelStdioRead
       {0x20ad,0x0cbb0571},//sceKernelStdioLseek
       {0x20ae,0xa46785c9},//sceKernelStdioSendChar
       {0x20af,0xa3b931db},//sceKernelStdioWrite
       {0x20b0,0x9d061c19},//sceKernelStdioClose
       {0x20b1,0x924aba61},//sceKernelStdioOpen
       {0x20b2,0x172d316e},//sceKernelStdin
       {0x20b3,0xa6bab2e9},//sceKernelStdout
       {0x20b4,0xf78ba90a},//sceKernelStderr
       {0x20b5,0xbfa98062},//sceKernelDcacheInvalidateRange
       {0x20b6,0xc2df770e},//sceKernelIcacheInvalidateRange
       {0x20b7,0xc8186a58},//sceKernelUtilsMd5Digest
       {0x20b8,0x9e5c5086},//sceKernelUtilsMd5BlockInit
       {0x20b9,0x61e1e525},//sceKernelUtilsMd5BlockUpdate
       {0x20ba,0xb8d24e78},//sceKernelUtilsMd5BlockResult
       {0x20bb,0x840259f1},//sceKernelUtilsSha1Digest
       {0x20bc,0xf8fcd5ba},//sceKernelUtilsSha1BlockInit
       {0x20bd,0x346f6da8},//sceKernelUtilsSha1BlockUpdate
       {0x20be,0x585f1c09},//sceKernelUtilsSha1BlockResult
       {0x20bf,0xe860e75e},//sceKernelUtilsMt19937Init
       {0x20c0,0x06fb8a63},//sceKernelUtilsMt19937UInt
       {0x20c1,0x37fb5c42},//sceKernelGetGPI
       {0x20c2,0x6ad345d7},//sceKernelSetGPO
       {0x20c3,0x91e4f6a7},//sceKernelLibcClock
       {0x20c4,0x27cc57f0},//sceKernelLibcTime
       {0x20c5,0x71ec4271},//sceKernelLibcGettimeofday
       {0x20c6,0x79d1c3fa},//sceKernelDcacheWritebackAll
       {0x20c7,0xb435dec5},//sceKernelDcacheWritebackInvalidateAll
       {0x20c8,0x3ee30821},//sceKernelDcacheWritebackRange
       {0x20c9,0x34b9fa9e},//sceKernelDcacheWritebackInvalidateRange
       {0x20ca,0x80001c4c},//sceKernelDcacheProbe
       {0x20cb,0x16641d70},//sceKernelDcacheReadTag
       {0x20cc,0x920f104a},//sceKernelIcacheInvalidateAll
       {0x20cd,0x4fd31c9d},//sceKernelIcacheProbe
       {0x20ce,0xfb05fad0},//sceKernelIcacheReadTag
       {0x20cf,0x977de386},//sceKernelLoadModule
       {0x20d0,0xb7f46618},//sceKernelLoadModuleByID
       {0x20d1,0x710f61b5},//sceKernelLoadModuleMs
       {0x20d2,0xf9275d98},//sceKernelLoadModuleBufferUsbWlan
       {0x20d3,0x50f0c1ec}, //KernelStartModule
       {0x20d4,0xd1ff982a}, //KernelStopModule
       {0x20d5,0x2e0911aa}, //KernelUnloadModule
       {0x20d6,0xd675ebb8}, //KernelSelfStopUnloadModule
       {0x20d7,0xcc1d3699}, //KernelStopUnloadSelfModule
       {0x20d8,0x644395e2}, //KernelGetModuleIdList?
       {0x20d9,0x748cbed9}, //KernelQueryModuleInfo
       {0x20da,0xf0a26395},//ModuleMgrForUser_F0A26395
       {0x20db,0xd8b73127},//ModuleMgrForUser_D8B73127
       {0x20dc,0xa291f107}, //KernelMaxFreeMemSize
       {0x20dd,0xf919f628}, //KernelTotalFreeMemSize
       {0x20de,0x237dbd4f}, //KernelAllocPartitionMemory
       {0x20df,0xb6d61d02}, //KernelFreePartitionMemory
       {0x20e0,0x9d9a5ba1}, //KernelGetBlockHeadAddr
       {0x20e1,0x13a5abef},//SysMemUserForUser_13A5ABEF
       {0x20e2,0x3fc9ae6a}, //KernelDevkitVersion
       {0x20e3,0xeadb1bd7}, //KernelPowerLock
       {0x20e4,0x3aee7261}, //KernelPowerUnlock
       {0x20e5,0x090ccb3f}, //KernelPowerTick
       {0x20e6,0x3e0271d3}, //SuspendForUser_3E0271D3
       {0x20e7,0xa14f40b2}, //SuspendForUser_A14F40B2
       {0x20e8,0xa569e425}, //SuspendForUser_A569E425
       {0x20e9,0xbd2f1094}, //KernelLoadExec
       {0x20ea,0x2ac9954b}, //KernelExitGameWithStatus
       {0x20eb,0x05572a5f}, //KernelExitGame
       {0x20ec,0x4ac57943}, //KernelRegisterExitCallback
       {0x20ed,0x617f3fe6}, //DmacMemcpy
       {0x20ee,0xd97f94d8}, //DmacTryMemcpy
       {0x20ef,0x1f6752ad}, //GeEdramGetSize
       {0x20f0,0xe47e40e4}, //GeEdramGetAddr
       {0x20f1,0xb77905ea}, //GeEdramSetAddrTranslation
       {0x20f2,0xdc93cfef}, //GeGetCmd
       {0x20f3,0x57c8945b}, //GeGetMtx
       {0x20f4,0x438a385a}, //GeSaveContext
       {0x20f5,0x0bf608fb}, //GeRestoreContext
       {0x20f6,0xab49e76a}, //GeListEnQueue
       {0x20f7,0x1c0d95a6}, //GeListEnQueueHead
       {0x20f8,0x5fb86ab0}, //GeListDeQueue
       {0x20f9,0xe0d68148}, //GeListUpdateStallAddr
       {0x20fa,0x03444eb4}, //GeListSync
       {0x20fb,0xb287bd61}, //GeDrawSync
       {0x20fc,0xb448ec0d}, //GeBreak
       {0x20fd,0x4c06e472}, //GeContinue
       {0x20fe,0xa4fc06a4}, //GeSetCallback
       {0x20ff,0x05db22ce}, //GeUnsetCallback
       {0x2100,0xc41c2853}, //RtcGetTickResolution
       {0x2101,0x3f7ad767}, //RtcGetCurrentTick
       {0x2102,0x011f03c1}, //Rtc_011F03C1
       {0x2103,0x029ca3b3}, //Rtc_029CA3B3
       {0x2104,0x4cfa57b0}, //RtcGetCurrentClock
       {0x2105,0xe7c27d1b}, //RtcGetCurrentClockLocalTime
       {0x2106,0x34885e0d}, //RtcConvertUtcToLocalTime
       {0x2107,0x779242a2}, //RtcConvertLocalTimeToUTC
       {0x2108,0x42307a17}, //RtcIsLeapYear
       {0x2109,0x05ef322c}, //RtcGetDaysInMonth
       {0x210a,0x57726bc1}, //RtcGetDayOfWeek
       {0x210b,0x4b1b5e82}, //RtcCheckValid
       {0x210c,0x3a807cc8}, //RtcSetTime_t
       {0x210d,0x27c4594c}, //RtcGetTime_t
       {0x210e,0xf006f264}, //RtcSetDosTime
       {0x210f,0x36075567}, //RtcGetDosTime
       {0x2110,0x7ace4c04}, //RtcSetWin32FileTime
       {0x2111,0xcf561893}, //RtcGetWin32FileTime
       {0x2112,0x7ed29e40}, //RtcSetTick
       {0x2113,0x6ff40acc}, //RtcGetTick
       {0x2114,0x9ed0ae87}, //RtcCompareTick
       {0x2115,0x44f45e05}, //RtcTickAddTicks
       {0x2116,0x26d25a5d}, //RtcTickAddMicroseconds
       {0x2117,0xf2a4afe5}, //RtcTickAddSeconds
       {0x2118,0xe6605bca}, //RtcTickAddMinutes
       {0x2119,0x26d7a24a}, //RtcTickAddHours
       {0x211a,0xe51b4b7a}, //RtcTickAddDays
       {0x211b,0xcf3a2ca8}, //RtcTickAddWeeks
       {0x211c,0xdbf74f1b}, //RtcTickAddMonths
       {0x211d,0x42842c77}, //RtcTickAddYears
       {0x211e,0xc663b3b9}, //RtcFormatRFC2822
       {0x211f,0x7de6711b}, //RtcFormatRFC2822LocalTime
       {0x2120,0x0498fb3c}, //RtcFormatRFC3339
       {0x2121,0x27f98543}, //RtcFormatRFC3339LocalTime
       {0x2122,0xdfbc5f16}, //RtcParseDateTime
       {0x2123,0x28e1e988}, //RtcParseRFC3339
       {0x2124,0x8c1009b2}, //AudioOutput
       {0x2125,0x136caf51}, //AudioOutputBlocking
       {0x2126,0xe2d56b2d}, //AudioOutputPanned
       {0x2127,0x13f592bc}, //AudioOutputPannedBlocking
       {0x2128,0x5ec81c55}, //AudioChReserve
       {0x2129,0x41efade7}, //AudioOneshotOutput
       {0x212a,0x6fc46853}, //AudioChRelease
       {0x212b,0xb011922f}, //Audio_B011922F
       {0x212c,0xcb2e439e}, //AudioSetChannelDataLen
       {0x212d,0x95fd0c2d}, //AudioChangeChannelConfig
       {0x212e,0xb7e1d8e7}, //AudioChangeChannelVolume
       {0x212f,0x38553111}, //Audio_38553111
       {0x2130,0x5c37c0ae}, //Audio_5C37C0AE
       {0x2131,0xe0727056}, //Audio_E0727056
       {0x2132,0x086e5895}, //AudioInputBlocking
       {0x2133,0x6d4bec68}, //AudioInput
       {0x2134,0xa708c6a6}, //AudioGetInputLength
       {0x2135,0x87b2e651}, //AudioWaitInputEnd
       {0x2136,0x7de61688}, //AudioInputInit
       {0x2137,0xe926d3fb}, //Audio_E926D3FB
       {0x2138,0xa633048e}, //Audio_A633048E
       {0x2139,0xe9d97901}, //AudioGetChannelRestLen
       {0x213a,0x0e20f177},//sceDisplaySetMode
       {0x213b,0xdea197d4},//sceDisplayGetMode
       {0x213c,0xdba6c4c4},//sceDisplayGetFramePerSec
       {0x213d,0x7ed59bc4},//sceDisplaySetHoldMode
       {0x213e,0xa544c486},//sceDisplaySetResumeMode
       {0x213f,0x289d82fe},//sceDisplaySetFrameBuf
       {0x2140,0xeeda2e54},//sceDisplayGetFrameBuf
       {0x2141,0xb4f378fa},//sceDisplayIsForeground
       {0x2142,0x31c4baa8},//sceDisplayGetBrightness
       {0x2143,0x9c6eaad7},//sceDisplayGetVcount
       {0x2144,0x4d4e10ec},//sceDisplayIsVblank
       {0x2145,0x36cdfade},//sceDisplayWaitVblank
       {0x2146,0x8eb9ec49},//sceDisplayWaitVblankCB
       {0x2147,0x984c27e7},//sceDisplayWaitVblankStart
       {0x2148,0x46f186c3},//sceDisplayWaitVblankStartCB
       {0x2149,0x773dd3a3},//sceDisplayGetCurrentHcount
       {0x214a,0x210eab3a},//sceDisplayGetAccumulatedHcount
       {0x214b,0xa83ef139},//sceDisplay_A83EF139
       {0x214c,0x6a2774f3},//sceCtrlSetSamplingCycle
       {0x214d,0x02baad91},//sceCtrlGetSamplingCycle
       {0x214e,0x1f4011e6},//sceCtrlSetSamplingMode
       {0x214f,0xda6b76a1},//sceCtrlGetSamplingMode          
       {0x2150,0x3a622550}, //CtrlPeekBufferPositive
       {0x2151,0xc152080a}, //CtrlPeekBufferNegative
       {0x2152,0x1f803938}, //CtrlReadBufferPositive
       {0x2153,0x60b81f86}, //CtrlReadBufferNegative
       {0x2154,0xb1d0e5cd}, //CtrlPeekLatch
       {0x2155,0x0b588501}, //CtrlReadLatch
       {0x2156,0xa7144800}, //Ctrl_A7144800
       {0x2157,0x687660fa}, //Ctrl_687660FA
       {0x2158,0x348d99d4}, //Ctrl_348D99D4
       {0x2159,0xaf5960f3}, //Ctrl_AF5960F3
       {0x215a,0xa68fd260}, //Ctrl_A68FD260
       {0x215b,0x6841be1a}, //Ctrl_6841BE1A
       {0x215c,0xc7154136}, //HprmRegisterCallback
       {0x215d,0x444ed0b7}, //HprmUnregisterCallback
       {0x215e,0x71b5fb67}, //Hprm_71B5FB67
       {0x215f,0x208db1bd}, //HprmIsRemoteExist
       {0x2160,0x7e69eda4}, //HprmIsHeadphoneExist
       {0x2161,0x219c58f1}, //HprmIsMicrophoneExist
       {0x2162,0x1910b327}, //HprmPeekCurrentKey
       {0x2163,0x2bcec83e}, //HprmPeekLatch
       {0x2164,0x40d2f9f0}, //HprmReadLatch
       {0x2165,0x2b51fe2f}, //Power_2B51FE2F
       {0x2166,0x442bfbac}, //Power_442BFBAC
       {0x2167,0xefd3c963}, //PowerTick
       {0x2168,0xedc13fe5}, //PowerGetIdleTimer
       {0x2169,0x7f30b3b1}, //PowerIdleTimerEnable
       {0x216a,0x972ce941}, //PowerIdleTimerDisable
       {0x216b,0x27f3292c}, //PowerBatteryUpdateInfo
       {0x216c,0xe8e4e204}, //Power_E8E4E204
       {0x216d,0xb999184c}, //PowerGetLowBatteryCapacity
       {0x216e,0x87440f5e}, //PowerIsPowerOnline
       {0x216f,0x0afd0d8b}, //PowerIsBatteryExist
       {0x2170,0x1e490401}, //PowerIsBatteryCharging
       {0x2171,0xb4432bc8}, //PowerGetBatteryChargingStatus
       {0x2172,0xd3075926}, //PowerIsLowBattery
       {0x2173,0x78a1a796}, //Power_78A1A796
       {0x2174,0x94f5a53f}, //PowerGetBatteryRemainCapacity
       {0x2175,0xfd18a0ff}, //Power_FD18A0FF
       {0x2176,0x2085d15d}, //PowerGetBatteryLifePercent
       {0x2177,0x8efb3fa2}, //PowerGetBatteryLifeTime
       {0x2178,0x28e12023}, //PowerGetBatteryTemp
       {0x2179,0x862ae1a6}, //PowerGetBatteryElec
       {0x217a,0x483ce86b}, //PowerGetBatteryVolt
       {0x217b,0x23436a4a}, //Power_23436A4A
       {0x217c,0x0cd21b1f}, //Power_0CD21B1F
       {0x217d,0x165ce085}, //Power_165CE085
       {0x217e,0x23c31ffe}, //Power_23C31FFE
       {0x217f,0xfa97a599}, //Power_FA97A599
       {0x2180,0xb3edd801}, //Power_B3EDD801
       {0x2181,0xd6d016ef}, //PowerLock
       {0x2182,0xca3d34c1}, //PowerUnlock
       {0x2183,0xdb62c9cf}, //PowerCancelRequest
       {0x2184,0x7fa406dd}, //PowerIsRequest
       {0x2185,0x2b7c7cf4}, //PowerRequestStandby
       {0x2186,0xac32c9cc}, //PowerRequestSuspend
       {0x2187,0x2875994b}, //Power_2875994B
       {0x2188,0x3951af53}, //PowerEncodeUBattery
       {0x2189,0x0074ef9b}, //PowerGetResumeCount
       {0x218a,0x04b7766e}, //PowerRegisterCallback
       {0x218b,0xdfa8baf8}, //PowerUnregisterCallback
       {0x218c,0xdb9d28dd}, //PowerUnregitserCallback
       {0x218d,0x843fbf43}, //PowerSetCpuClockFrequency
       {0x218e,0xb8d7b3fb}, //PowerSetBusClockFrequency
       {0x218f,0xfee03a2f}, //PowerGetCpuClockFrequency
       {0x2190,0x478fe6f5}, //PowerGetBusClockFrequency
       {0x2191,0xfdb5bfe9}, //PowerGetCpuClockFrequencyInt
       {0x2192,0xbd681969}, //PowerGetBusClockFrequencyInt
       {0x2193,0x34f9c463}, //Power_34F9C463
       {0x2194,0xb1a52c83}, //PowerGetCpuClockFrequencyFloat
       {0x2195,0x9badb3eb}, //PowerGetBusClockFrequencyFloat
       {0x2196,0xea382a27}, //Power_EA382A27
       {0x2197,0x737486f2}, //PowerSetClockFrequency
       {0x2198,0xae5de6af}, //UsbStart
       {0x2199,0xc2464fa0}, //UsbStop
       {0x219a,0xc21645a4}, //UsbGetState
       {0x219b,0x4e537366}, //UsbGetDrvList
       {0x219c,0x112cc951}, //UsbGetDrvState
       {0x219d,0x586db82c}, //UsbActivate
       {0x219e,0xc572a9c8}, //UsbDeactivate
       {0x219f,0x5be0e002}, //UsbWaitState
       {0x21a0,0x1c360735}, //UsbWaitCancel
       {0x21a1,0xc69bebce}, //OpenPSIDGetOpenPSID
       {0x21a2,0x71eef62d}, //SircsSend
       {0x21a3,0x46ebb729}, //UmdCheckMedium
       {0x21a4,0xc6183d47}, //UmdActivate
       {0x21a5,0xe83742ba}, //UmdDeactivate
       {0x21a6,0x8ef08fce}, //UmdWaitDriveStat
       {0x21a7,0x56202973}, //UmdWaitDriveStatWithTimer
       {0x21a8,0x4a9e5e29}, //UmdWaitDriveStatCB
       {0x21a9,0x6af9b50a}, //UmdCancelWaitDriveStat?
       {0x21aa,0x6b4a146c}, //UmdGetDriveStat
       {0x21ab,0x20628e6f}, //UmdGetErrorStat
       {0x21ac,0x340b7686}, //UmdGetDiscInfo
       {0x21ad,0xaee7404d}, //UmdRegisterUMDCallBack
       {0x21ae,0xbd2bde07}, //UmdUnRegisterUMDCallBack
       {0x21af,0x93440b11}, //WlanDevIsPowerOn
       {0x21b0,0xd7763699}, //WlanGetSwitchState
       {0x21b1,0x0c622081}, //WlanGetEtherAddr
       {0x21b2,0x482cae9a}, //WlanDevAttach
       {0x21b3,0xc9a8cab7}, //WlanDevDetach
       {0x21b4,0x19e51f54}, //WlanDrv_lib_19E51F54
       {0x21b5,0x5e7c8d94}, //WlanDevIsGameMode
       {0x21b6,0x5ed4049a}, //WlanGPPrevEstablishActive
       {0x21b7,0xb4d7cb74}, //WlanGPSend
       {0x21b8,0xa447103a}, //WlanGPRecv
       {0x21b9,0x9658c9f7}, //WlanGPRegisterCallback
       {0x21ba,0x4c7f62e0}, //WlanGPUnRegisterCallback
       {0x21bb,0x81579d36}, //WlanDrv_lib_81579D36
       {0x21bc,0x5baa1fe5}, //WlanDrv_lib_5BAA1FE5
       {0x21bd,0x4c14baca}, //WlanDrv_lib_4C14BACA
       {0x21be,0x2d0fae4e}, //WlanDrv_lib_2D0FAE4E
       {0x21bf,0x56f467ca}, //WlanDrv_lib_56F467CA
       {0x21c0,0xfe8a0b46}, //WlanDrv_lib_FE8A0B46
       {0x21c1,0x40b0aa4a}, //WlanDrv_lib_40B0AA4A
       {0x21c2,0x7ff54bd2}, //WlanDevSetGPIO
       {0x21c3,0x05fe320c}, //WlanDevGetStateGPIO
       {0x21c4,0x8d5f551b}, //WlanDrv_lib_8D5F551B
       {0x21c5,0x8986295e}, //VaudioOutputBlocking
       {0x21c6,0x03b6807d}, //VaudioChReserve
       {0x21c7,0x67585dfd}, //VaudioChRelease
       {0x21c8,0x346fbe94}, //Vaudio_346FBE94
       {0x21c9,0x9b25edf1}, //RegExit
       {0x21ca,0x92e41280}, //RegOpenRegistry
       {0x21cb,0xfa8a5739}, //RegCloseRegistry
       {0x21cc,0xdeda92bf}, //RegRemoveRegistry
       {0x21cd,0x1d8a762e}, //Reg_1D8A762E
       {0x21ce,0x0cae832b}, //Reg_0CAE832B
       {0x21cf,0x39461b4d}, //RegFlushRegistry
       {0x21d0,0x0d69bf40},//sceReg_0D69BF40
       {0x21d1,0x57641a81},//sceRegCreateKey
       {0x21d2,0x17768e14},//sceRegSetKeyValue
       {0x21d3,0xd4475aa8},//sceRegGetKeyInfo
       {0x21d4,0x28a8e98a},//sceRegGetKeyValue
       {0x21d5,0x2c0db9dd},//sceRegGetKeysNum
       {0x21d6,0x2d211135},//sceRegGetKeys
       {0x21d7,0x4ca16893},//sceReg_4CA16893
       {0x21d8,0x3615bc87},//sceRegRemoveKey
       {0x21d9,0xc5768d02},//sceRegKickBackDiscover
       {0x21da,0x30be0259},//sceRegGetKeyValueByName
       {0x21db,0xc492f751},//sceUtilityGameSharingInitStart
       {0x21dc,0xefc6f80f},//sceUtilityGameSharingShutdownStart
       {0x21dd,0x7853182d},//sceUtilityGameSharingUpdate
       {0x21de,0x946963f3},//sceUtilityGameSharingGetStatus
       {0x21df,0x3ad50ae7},//sceNetplayDialogInitStart
       {0x21e0,0xbc6b6296},//sceNetplayDialogShutdownStart
       {0x21e1,0x417bed54},//sceNetplayDialogUpdate
       {0x21e2,0xb6cee597},//sceNetplayDialogGetStatus
       {0x21e3,0x4db1e739},//sceUtilityNetconfInitStart
       {0x21e4,0xf88155f6},//sceUtilityNetconfShutdownStart
       {0x21e5,0x91e70e35},//sceUtilityNetconfUpdate
       {0x21e6,0x6332aa39},//sceUtilityNetconfGetStatus
       {0x21e7,0x50c4cd57},//sceUtilitySavedataInitStart
       {0x21e8,0x9790b33c},//sceUtilitySavedataShutdownStart
       {0x21e9,0xd4b95ffb},//sceUtilitySavedataUpdate
       {0x21ea,0x8874dbe0},//sceUtilitySavedataGetStatus
       {0x21eb,0x2995d020},//sceUtility_2995D020
       {0x21ec,0xb62a4061},//sceUtility_B62A4061
       {0x21ed,0xed0fad38},//sceUtility_ED0FAD38
       {0x21ee,0x88bc7406},//sceUtility_88BC7406
       {0x21ef,0x2ad8e239},//sceUtilityMsgDialogInitStart
       {0x21f0,0x67af3428},//sceUtilityMsgDialogShutdownStart
       {0x21f1,0x95fc253b},//sceUtilityMsgDialogUpdate
       {0x21f2,0x9a1c91d7},//sceUtilityMsgDialogGetStatus
       {0x21f3,0xf6269b82},//sceUtilityOskInitStart
       {0x21f4,0x3dfaeba9},//sceUtilityOskShutdownStart
       {0x21f5,0x4b85c861},//sceUtilityOskUpdate
       {0x21f6,0xf3f76017},//sceUtilityOskGetStatus
       {0x21f7,0x45c18506},//sceUtilitySetSystemParamInt
       {0x21f8,0x41e30674},//sceUtilitySetSystemParamString
       {0x21f9,0xa5da2406},//sceUtilityGetSystemParamInt
       {0x21fa,0x34b78343},//sceUtilityGetSystemParamString
       {0x21fb,0x5eee6548},//sceUtilityCheckNetParam
       {0x21fc,0x434d4b3a},//sceUtilityGetNetParam
       {0x21fd,0x17cb4d96},//sceUtility_private_17CB4D96
       {0x21fe,0xee7ac503},//sceUtility_private_EE7AC503
       {0x21ff,0x5ff96ed3},//sceUtility_private_5FF96ED3
       {0x2200,0x9c9dd5bc},//sceUtility_private_9C9DD5BC
       {0x2201,0x4405ba38},//sceUtility_private_4405BA38
       {0x2202,0x1dfa62ef},//sceUtility_private_1DFA62EF
       {0x2203,0x680c0ea8},//sceUtilityDialogSetStatus
       {0x2204,0xb222e887},//sceUtilityDialogGetType
       {0x2205,0x4f2206bc},//sceUtilityDialogGetParam
       {0x2206,0xef5bc2d1},//sceUtility_private_EF5BC2D1
       {0x2207,0xe01fe32a},//sceUtilityDialogGetSpeed
       {0x2208,0x19461966},//sceUtility_private_19461966
       {0x2209,0x6f923bd3},//sceUtilityDialogSetThreadId
       {0x220a,0xa5168a5d},//sceUtilityDialogLoadModule
       {0x220b,0x3ceae1df},//sceUtilityDialogPowerLock
       {0x220c,0x56bedca4},//sceUtilityDialogPowerUnlock
       {0x220d,0x072debf2},//sceUtilityCreateNetParam
       {0x220e,0x9ce50172},//sceUtilityDeleteNetParam
       {0x220f,0xfb0c4840},//sceUtilityCopyNetParam
       {0x2210,0xfc4516f3},//sceUtilitySetNetParam

       /*added random syscalls numbers */
       /*found on Jazz homebrew game */
       {0x2211,0x565C0B0E}, //sceKernelRegisterDefaultExceptionHandler
       {0x2212,0xD8779AC6},//sceKernelIcacheClearAll
       {0x2213,0xcf8a41b1},//sceKernelFindModuleByName
       //end of jazz
       /*found on NesterJ Nes emu v1.20 */
       {0x2214,0x39af39a6},//sceNetInit
       {0x2215,0x281928A9},//sceNetTerm
       {0x2216,0x50647530},//sceNetFreeThreadinfo
       {0x2217,0xAD6844C6},//sceNetThreadAbort
       {0x2218,0x89360950},//sceNetEtherNtostr
       {0x2219,0xD27961C9},//sceNetEtherStrton
       {0x221a,0x0BF0A3AE},//sceNetGetLocalEtherAddr
       {0x221b,0xCC393E48}//sceNetGetMallocStat

   };
   
}
