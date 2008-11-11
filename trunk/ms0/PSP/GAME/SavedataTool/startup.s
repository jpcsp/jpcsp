# Hello World for PSP
# 2005.04.30  created by nem 

		.set noreorder

		.text

		.extern xmain


##############################################################################


		.ent _start
		.weak _start
_start:
		addiu 	$sp, -0x10
		sw		$ra, 0($sp)	
		sw		$s0, 4($sp)
		sw		$s1, 8($sp)
		
		move	$s0, $a0				# Save args
		move	$s1, $a1

		la $a0, _elfTest
		la $a1, _startAddress

#		jal	kmain
#		nop

		la  	$a0, _main_thread_name	# Main thread setup
		la		$a1, xmain
		li		$a2, 0x20				# Priority
		li		$a3, 0x40000			# Stack size
		lui		$t0, 0x8000				# Attributes
		jal		sceKernelCreateThread
		move	$t1, $0

		move	$a0, $v0				# Start thread
		move	$a1, $s0
		jal		sceKernelStartThread
		move	$a2, $s1

		lw		$ra, 0($sp)
		lw		$s0, 4($sp)
		lw		$s1, 8($sp)
		move	$v0, $0
		jr 		$ra
		addiu	$sp, 0x10

_main_thread_name:
		.asciiz	"user_main"
_elfTest:
		.asciiz	"ms0:/test.elf"
_startAddress:
		.word 0



##############################################################################


		.section	.lib.ent,"wa",@progbits
__lib_ent_top:
		.word 0
		.word 0x80000000
		.word 0x00010104
		.word __entrytable


		.section	.lib.ent.btm,"wa",@progbits
__lib_ent_bottom:
		.word	0


		.section	.lib.stub,"wa",@progbits
__lib_stub_top:


		.section	.lib.stub.btm,"wa",@progbits
__lib_stub_bottom:
		.word	0


##############################################################################

		.section	".xodata.sceModuleInfo","wa",@progbits

__moduleinfo:
#		.word	0x01011000   # kernel mode
		.byte	0,0,1,1

		.ascii	"SavedataTool"		#up to 28 char
		.align	5

		.word	_gp
		.word	__lib_ent_top
		.word	__lib_ent_bottom
		.word	__lib_stub_top
		.word	__lib_stub_bottom

##############################################################################

		.section	.rodata.entrytable,"wa",@progbits
__entrytable:
		.word 0xD632ACDB
		.word 0xF01D73A7
		.word _start
		.word __moduleinfo
		.word 0



###############################################################################

		.data


###############################################################################

		.bss


###############################################################################


	.macro	STUB_START	module,d1,d2

		.section	.rodata.stubmodulename
		.word	0
__stub_modulestr_\@:
		.asciz	"\module"
		.align	2

		.section	.lib.stub
		.word __stub_modulestr_\@
		.word \d1
		.word \d2
		.word __stub_idtable_\@
		.word __stub_text_\@

		.section	.rodata.stubidtable
__stub_idtable_\@:

		.section	.text.stub
__stub_text_\@:

	.endm


	.macro	STUB_END
	.endm


	.macro	STUB_FUNC	funcid,funcname

		.set push
		.set noreorder

		.section	.text.stub
		.weak	\funcname
\funcname:
		jr	$ra
		nop

		.section	.rodata.stubidtable
		.word	\funcid

		.set pop

	.endm


STUB_START "sceCtrl",0x40010000,0x000e0005 
  STUB_FUNC 0x6a2774f3,sceCtrlSetSamplingCycle 
  STUB_FUNC 0x02baad91,sceCtrlGetSamplingCycle 
  STUB_FUNC 0x1f4011e6,sceCtrlSetSamplingMode 
  STUB_FUNC 0xda6b76a1,sceCtrlGetSamplingMode 
  STUB_FUNC 0x3a622550,sceCtrlPeekBufferPositive 
  STUB_FUNC 0xc152080a,sceCtrlPeekBufferNegative 
  STUB_FUNC 0x1f803938,sceCtrlReadBufferPositive 
  STUB_FUNC 0x60b81f86,sceCtrlReadBufferNegative 
  STUB_FUNC 0xb1d0e5cd,sceCtrlPeekLatch 
  STUB_FUNC 0x0b588501,sceCtrlReadLatch 
  STUB_FUNC 0x348d99d4,sceCtrl_Unkonow00 
  STUB_FUNC 0xaf5960f3,sceCtrl_Unkonow01 
  STUB_FUNC 0xa68fd260,sceCtrl_Unkonow02 
  STUB_FUNC 0x6841be1a,sceCtrl_Unkonow03 
STUB_END 
STUB_START "sceDisplay",0x40010000,0x00110005 
  STUB_FUNC 0x0e20f177,sceDisplaySetMode 
  STUB_FUNC 0xdea197d4,sceDisplayGetMode 
  STUB_FUNC 0xdba6c4c4,sceDisplayGetFramePerSec 
  STUB_FUNC 0x7ed59bc4,sceDisplaySetHoldMode 
  STUB_FUNC 0xa544c486,sceDisplaySetResumeMode 
  STUB_FUNC 0x289d82fe,sceDisplaySetFrameBuf 
  STUB_FUNC 0xeeda2e54,sceDisplayGetFrameBuf 
  STUB_FUNC 0xb4f378fa,sceDisplayIsForeground 
  STUB_FUNC 0x31c4baa8,sceDisplay_Unkonow00 
  STUB_FUNC 0x9c6eaad7,sceDisplayGetVcount 
  STUB_FUNC 0x4d4e10ec,sceDisplayIsVblank 
  STUB_FUNC 0x36cdfade,sceDisplayWaitVblank 
  STUB_FUNC 0x8eb9ec49,sceDisplayWaitVblankCB 
  STUB_FUNC 0x984c27e7,sceDisplayWaitVblankStart 
  STUB_FUNC 0x46f186c3,sceDisplayWaitVblankStartCB 
  STUB_FUNC 0x773dd3a3,sceDisplayGetCurrentHcount 
  STUB_FUNC 0x210eab3a,sceDisplayGetAccumulatedHcount 
STUB_END 
STUB_START "IoFileMgrForUser",0x40010000,0x00240005 
  STUB_FUNC 0x3251ea56,sceIoPollAsync 
  STUB_FUNC 0xe23eec33,sceIoWaitAsync 
  STUB_FUNC 0x35dbd746,sceIoWaitAsyncCB 
  STUB_FUNC 0xcb05f8d6,sceIoGetAsyncStat 
  STUB_FUNC 0xb293727f,sceIoChangeAsyncPriority 
  STUB_FUNC 0xa12a0514,sceIoSetAsyncCallback 
  STUB_FUNC 0x810c4bc3,sceIoClose 
  STUB_FUNC 0xff5940b6,sceIoCloseAsync 
  STUB_FUNC 0x109f50bc,sceIoOpen 
  STUB_FUNC 0x89aa9906,sceIoOpenAsync 
  STUB_FUNC 0x6a638d83,sceIoRead 
  STUB_FUNC 0xa0b5a7c2,sceIoReadAsync 
  STUB_FUNC 0x42ec03ac,sceIoWrite 
  STUB_FUNC 0x0facab19,sceIoWriteAsync 
  STUB_FUNC 0x27eb27b8,sceIoLseek 
  STUB_FUNC 0x71b19e77,sceIoLseekAsync 
  STUB_FUNC 0x68963324,sceIoLseek32 
  STUB_FUNC 0x1b385d8f,sceIoLseek32Async 
  STUB_FUNC 0x63632449,sceIoIoctl 
  STUB_FUNC 0xe95a012b,sceIoIoctlAsync 
  STUB_FUNC 0xb29ddf9c,sceIoDopen 
  STUB_FUNC 0xe3eb004c,sceIoDread 
  STUB_FUNC 0xeb092469,sceIoDclose 
  STUB_FUNC 0xf27a9c51,sceIoRemove 
  STUB_FUNC 0x06a70004,sceIoMkdir 
  STUB_FUNC 0x1117c65f,sceIoRmdir 
  STUB_FUNC 0x55f4717d,sceIoChdir 
  STUB_FUNC 0xab96437f,sceIoSync 
  STUB_FUNC 0xace946e8,sceIoGetstat 
  STUB_FUNC 0xb8a740f4,sceIoChstat 
  STUB_FUNC 0x779103a0,sceIoRename 
  STUB_FUNC 0x54f5fb11,sceIoDevctl 
  STUB_FUNC 0x08bd7374,sceIoGetDevType 
  STUB_FUNC 0xb2a628c1,sceIoAssign 
  STUB_FUNC 0x6d08a871,sceIoUnassign 
  STUB_FUNC 0xe8bc6571,sceIoCancel 
STUB_END 
STUB_START "ThreadManForUser",0x40010000,0x007e0005 
  STUB_FUNC 0x6e9ea350,ThreadManForUser_Unkonow00 
  STUB_FUNC 0x0c106e53,sceKernelRegisterThreadEventHandler 
  STUB_FUNC 0x72f3c145,sceKernelReleaseThreadEventHandler 
  STUB_FUNC 0x369eeb6b,sceKernelReferThreadEventHandlerStatus 
  STUB_FUNC 0xe81caf8f,sceKernelCreateCallback 
  STUB_FUNC 0xedba5844,sceKernelDeleteCallback 
  STUB_FUNC 0xc11ba8c4,sceKernelNotifyCallback 
  STUB_FUNC 0xba4051d6,sceKernelCancelCallback 
  STUB_FUNC 0x2a3d44ff,sceKernelGetCallbackCount 
  STUB_FUNC 0x349d6d6c,sceKernelCheckCallback 
  STUB_FUNC 0x730ed8bc,sceKernelReferCallbackStatus 
  STUB_FUNC 0x9ace131e,sceKernelSleepThread 
  STUB_FUNC 0x82826f70,sceKernelSleepThreadCB 
  STUB_FUNC 0xd59ead2f,sceKernelWakeupThread 
  STUB_FUNC 0xfccfad26,sceKernelCancelWakeupThread 
  STUB_FUNC 0x9944f31f,sceKernelSuspendThread 
  STUB_FUNC 0x75156e8f,sceKernelResumeThread 
  STUB_FUNC 0x278c0df5,sceKernelWaitThreadEnd 
  STUB_FUNC 0x840e8133,sceKernelWaitThreadEndCB 
  STUB_FUNC 0xceadeb47,sceKernelDelayThread 
  STUB_FUNC 0x68da9e36,sceKernelDelayThreadCB 
  STUB_FUNC 0xbd123d9e,sceKernelDelaySysClockThread 
  STUB_FUNC 0x1181e963,sceKernelDelaySysClockThreadCB 
  STUB_FUNC 0xd6da4ba1,sceKernelCreateSema 
  STUB_FUNC 0x28b6489c,sceKernelDeleteSema 
  STUB_FUNC 0x3f53e640,sceKernelSignalSema 
  STUB_FUNC 0x4e3a1105,sceKernelWaitSema 
  STUB_FUNC 0x6d212bac,sceKernelWaitSemaCB 
  STUB_FUNC 0x58b1f937,sceKernelPollSema 
  STUB_FUNC 0x8ffdf9a2,sceKernelCancelSema 
  STUB_FUNC 0xbc6febc5,sceKernelReferSemaStatus 
  STUB_FUNC 0x55c20a00,sceKernelCreateEventFlag 
  STUB_FUNC 0xef9e4c70,sceKernelDeleteEventFlag 
  STUB_FUNC 0x1fb15a32,sceKernelSetEventFlag 
  STUB_FUNC 0x812346e4,sceKernelClearEventFlag 
  STUB_FUNC 0x402fcf22,sceKernelWaitEventFlag 
  STUB_FUNC 0x328c546a,sceKernelWaitEventFlagCB 
  STUB_FUNC 0x30fd48f0,sceKernelPollEventFlag 
  STUB_FUNC 0xcd203292,sceKernelCancelEventFlag 
  STUB_FUNC 0xa66b0120,sceKernelReferEventFlagStatus 
  STUB_FUNC 0x8125221d,sceKernelCreateMbx 
  STUB_FUNC 0x86255ada,sceKernelDeleteMbx 
  STUB_FUNC 0xe9b3061e,sceKernelSendMbx 
  STUB_FUNC 0x18260574,sceKernelReceiveMbx 
  STUB_FUNC 0xf3986382,sceKernelReceiveMbxCB 
  STUB_FUNC 0x0d81716a,sceKernelPollMbx 
  STUB_FUNC 0x87d4dd36,sceKernelCancelReceiveMbx 
  STUB_FUNC 0xa8e8c846,sceKernelReferMbxStatus 
  STUB_FUNC 0x7c0dc2a0,sceKernelCreateMsgPipe 
  STUB_FUNC 0xf0b7da1c,sceKernelDeleteMsgPipe 
  STUB_FUNC 0x876dbfad,sceKernelSendMsgPipe 
  STUB_FUNC 0x7c41f2c2,sceKernelSendMsgPipeCB 
  STUB_FUNC 0x884c9f90,sceKernelTrySendMsgPipe 
  STUB_FUNC 0x74829b76,sceKernelReceiveMsgPipe 
  STUB_FUNC 0xfbfa697d,sceKernelReceiveMsgPipeCB 
  STUB_FUNC 0xdf52098f,sceKernelTryReceiveMsgPipe 
  STUB_FUNC 0x349b864d,sceKernelCancelMsgPipe 
  STUB_FUNC 0x33be4024,sceKernelReferMsgPipeStatus 
  STUB_FUNC 0x56c039b5,sceKernelCreateVpl 
  STUB_FUNC 0x89b3d48c,sceKernelDeleteVpl 
  STUB_FUNC 0xbed27435,sceKernelAllocateVpl 
  STUB_FUNC 0xec0a693f,sceKernelAllocateVplCB 
  STUB_FUNC 0xaf36d708,sceKernelTryAllocateVpl 
  STUB_FUNC 0xb736e9ff,sceKernelFreeVpl 
  STUB_FUNC 0x1d371b8a,sceKernelCancelVpl 
  STUB_FUNC 0x39810265,sceKernelReferVplStatus 
  STUB_FUNC 0xc07bb470,sceKernelCreateFpl 
  STUB_FUNC 0xed1410e0,sceKernelDeleteFpl 
  STUB_FUNC 0xd979e9bf,sceKernelAllocateFpl 
  STUB_FUNC 0xe7282cb6,sceKernelAllocateFplCB 
  STUB_FUNC 0x623ae665,sceKernelTryAllocateFpl 
  STUB_FUNC 0xf6414a71,sceKernelFreeFpl 
  STUB_FUNC 0xa8aa591f,sceKernelCancelFpl 
  STUB_FUNC 0xd8199e4c,sceKernelReferFplStatus 
  STUB_FUNC 0x0e927aed,ThreadManForUser_Unkonow01 
  STUB_FUNC 0x110dec9a,ThreadManForUser_Unkonow02 
  STUB_FUNC 0xc8cd158c,ThreadManForUser_Unkonow03 
  STUB_FUNC 0xba6b92e2,ThreadManForUser_Unkonow04 
  STUB_FUNC 0xe1619d7c,ThreadManForUser_Unkonow05 
  STUB_FUNC 0xdb738f35,sceKernelGetSystemTime 
  STUB_FUNC 0x82bc5777,ThreadManForUser_Unkonow06 
  STUB_FUNC 0x369ed59d,sceKernelGetSystemTimeLow 
  STUB_FUNC 0x6652b8ca,sceKernelSetAlarm 
  STUB_FUNC 0xb2c25152,sceKernelSetSysClockAlarm 
  STUB_FUNC 0x7e65b999,sceKernelCancelAlarm 
  STUB_FUNC 0xdaa3f564,sceKernelReferAlarmStatus 
  STUB_FUNC 0x20fff560,sceKernelCreateVTimer 
  STUB_FUNC 0x328f9e52,sceKernelDeleteVTimer 
  STUB_FUNC 0xb3a59970,sceKernelGetVTimerBase 
  STUB_FUNC 0xb7c18b77,ThreadManForUser_Unkonow07 
  STUB_FUNC 0x034a921f,sceKernelGetVTimerTime 
  STUB_FUNC 0xc0b3ffd2,ThreadManForUser_Unkonow08 
  STUB_FUNC 0x542ad630,sceKernelSetVTimerTime 
  STUB_FUNC 0xfb6425c3,ThreadManForUser_Unkonow09 
  STUB_FUNC 0xc68d9437,sceKernelStartVTimer 
  STUB_FUNC 0xd0aeee87,sceKernelStopVTimer 
  STUB_FUNC 0xd8b299ae,sceKernelSetVTimerHandler 
  STUB_FUNC 0x53b00e9a,ThreadManForUser_Unkonow10 
  STUB_FUNC 0xd2d615ef,sceKernelCancelVTimerHandler 
  STUB_FUNC 0x5f32beaa,sceKernelReferVTimerStatus 
  STUB_FUNC 0x446d8de6,sceKernelCreateThread 
  STUB_FUNC 0x9fa03cd3,sceKernelDeleteThread 
  STUB_FUNC 0xf475845d,sceKernelStartThread 
  STUB_FUNC 0x532a522e,ThreadManForUser_Unkonow11 
  STUB_FUNC 0xaa73c935,sceKernelExitThread 
  STUB_FUNC 0x809ce29b,sceKernelExitDeleteThread 
  STUB_FUNC 0x616403ba,sceKernelTerminateThread 
  STUB_FUNC 0x383f7bcc,sceKernelTerminateDeleteThread 
  STUB_FUNC 0x3ad58b8c,sceKernelSuspendDispatchThread 
  STUB_FUNC 0x27e22ec2,sceKernelResumeDispatchThread 
  STUB_FUNC 0xea748e31,ThreadManForUser_Unkonow12 
  STUB_FUNC 0x71bc9871,sceKernelChangeThreadPriority 
  STUB_FUNC 0x912354a7,ThreadManForUser_Unkonow13 
  STUB_FUNC 0x2c34e053,sceKernelReleaseWaitThread 
  STUB_FUNC 0x293b45b8,sceKernelGetThreadId 
  STUB_FUNC 0x94aa61ee,sceKernelGetThreadCurrentPriority 
  STUB_FUNC 0x3b183e26,ThreadManForUser_Unkonow14 
  STUB_FUNC 0xd13bde95,sceKernelCheckThreadStack 
  STUB_FUNC 0x52089ca1,ThreadManForUser_Unkonow15 
  STUB_FUNC 0x17c1684e,ThreadManForUser_Unkonow16 
  STUB_FUNC 0xffc36a14,ThreadManForUser_Unkonow17 
  STUB_FUNC 0x627e6f3a,sceKernelReferSystemStatus 
  STUB_FUNC 0x94416130,sceKernelGetThreadmanIdList 
  STUB_FUNC 0x57cf62dd,sceKernelGetThreadmanIdType 
  STUB_FUNC 0x64d4540e,ThreadManForUser_Unkonow18 
  STUB_FUNC 0x8218b4dd,ThreadManForUser_Unkonow19 
STUB_END 
STUB_START "LoadExecForUser",0x40010000,0x00040005 
  STUB_FUNC 0xbd2f1094,sceKernelLoadExec 
  STUB_FUNC 0x2ac9954b,sceKernelExitGameWithStatus 
  STUB_FUNC 0x05572a5f,sceKernelExitGame 
  STUB_FUNC 0x4ac57943,sceKernelRegisterExitCallback 
STUB_END 
STUB_START "sceUtility",0x40010000,0x00230005 
  STUB_FUNC 0xc492f751,sceUtilityGameSharingInitStart 
  STUB_FUNC 0xefc6f80f,sceUtilityGameSharingShutdownStart 
  STUB_FUNC 0x7853182d,sceUtilityGameSharingUpdate 
  STUB_FUNC 0x946963f3,sceUtilityGameSharingGetStatus 
  STUB_FUNC 0x3ad50ae7,sceUtility_Unkonow00 
  STUB_FUNC 0xbc6b6296,sceUtility_Unkonow01 
  STUB_FUNC 0x417bed54,sceUtility_Unkonow02 
  STUB_FUNC 0xb6cee597,sceUtility_Unkonow03 
  STUB_FUNC 0x4db1e739,sceUtilityNetconfInitStart 
  STUB_FUNC 0xf88155f6,sceUtilityNetconfShutdownStart 
  STUB_FUNC 0x91e70e35,sceUtilityNetconfUpdate 
  STUB_FUNC 0x6332aa39,sceUtilityNetconfGetStatus 
  STUB_FUNC 0x50c4cd57,sceUtilitySavedataInitStart 
  STUB_FUNC 0x9790b33c,sceUtilitySavedataShutdownStart 
  STUB_FUNC 0xd4b95ffb,sceUtilitySavedataUpdate 
  STUB_FUNC 0x8874dbe0,sceUtilitySavedataGetStatus 
  STUB_FUNC 0x2995d020,sceUtility_Unkonow04 
  STUB_FUNC 0xb62a4061,sceUtility_Unkonow05 
  STUB_FUNC 0xed0fad38,sceUtility_Unkonow06 
  STUB_FUNC 0x88bc7406,sceUtility_Unkonow07 
  STUB_FUNC 0x2ad8e239,sceUtilityMsgDialogInitStart 
  STUB_FUNC 0x67af3428,sceUtilityMsgDialogShutdownStart 
  STUB_FUNC 0x95fc253b,sceUtilityMsgDialogUpdate 
  STUB_FUNC 0x9a1c91d7,sceUtilityMsgDialogGetStatus 
  STUB_FUNC 0xf6269b82,sceUtilityOskInitStart 
  STUB_FUNC 0x3dfaeba9,sceUtilityOskShutdownStart 
  STUB_FUNC 0x4b85c861,sceUtilityOskUpdate 
  STUB_FUNC 0xf3f76017,sceUtilityOskGetStatus 
  STUB_FUNC 0x45c18506,sceUtilitySetSystemParamInt 
  STUB_FUNC 0x41e30674,sceUtilitySetSystemParamString 
  STUB_FUNC 0xa5da2406,sceUtilityGetSystemParamInt 
  STUB_FUNC 0x34b78343,sceUtilityGetSystemParamString 
  STUB_FUNC 0x5eee6548,sceUtilityCheckNetParam 
  STUB_FUNC 0x434d4b3a,sceUtilityGetNetParam 
  STUB_FUNC 0x0bfb8894,sceUtilityExecFileOpen 
STUB_END 


###############################################################################

	.text

	.end _start

