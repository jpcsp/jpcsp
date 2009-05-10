	.set noreorder

#include "pspstub.s"

	STUB_START "sceUmdUser",0x00070000,0x00020005
	STUB_FUNC  0xAEE7404D,sceUmdRegisterUMDCallBack
	STUB_FUNC  0x6AF9B50A,sceUmdCancelWaitDriveStat
	STUB_END
