        .set noreorder

#include "pspstub.s"

        STUB_START "DmacManForKernel",0x00070000,0x000B0005
        STUB_FUNC  0x59615199,sceKernelDmaOpAlloc
        STUB_FUNC  0x745E19EF,sceKernelDmaOpFree
        STUB_FUNC  0xF64BAB99,sceKernelDmaOpAssign
        STUB_FUNC  0x3BDEA96C,sceKernelDmaOpEnQueue
        STUB_FUNC  0x5AF32783,sceKernelDmaOpQuit
        STUB_FUNC  0x92700CCD,sceKernelDmaOpDeQueue
        STUB_FUNC  0xCE467D9B,sceKernelDmaOpSetupNormal
        STUB_FUNC  0xD0358BE9,sceKernelDmaOpSetCallback
        STUB_FUNC  0xDB286D65,sceKernelDmaOpSync
        STUB_FUNC  0x7D21A2EF,sceKernelDmaOpSetupLink
        STUB_FUNC  0x3FAD5844,sceKernelDmaOpSetupMemcpy
        STUB_END

