	.set noreorder

#include "pspstub.s"

	STUB_START "sceUmdUser",0x00070000,0x00010005
	STUB_FUNC  0x6AF9B50A,sceUmdCancelWaitDriveStat
	STUB_END
