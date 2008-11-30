	.set noreorder

#include "pspstub.s"

	STUB_START "sceUmdUser",0x00070000,0x00010005
	STUB_FUNC  0xAEE7404D,sceUmdRegisterUMDCallback
	STUB_END
