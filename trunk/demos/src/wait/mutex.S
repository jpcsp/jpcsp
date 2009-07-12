	.set noreorder

#include "pspstub.s"

	STUB_START "ThreadManForUser",0x00070000,0x00040005
	STUB_FUNC  0xB7D098C6,sceKernelCreateMutex
	STUB_FUNC  0xF8170FBE,sceKernelDeleteMutex
	STUB_FUNC  0xB011B11F,sceKernelLockMutex
	STUB_FUNC  0x87D9223C,sceKernelCancelMutex
	STUB_END
