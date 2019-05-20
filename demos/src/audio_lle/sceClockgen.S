        .set noreorder

#include "pspstub.s"

        STUB_START "sceClockgen_driver",0x00070000,0x00010005
        STUB_FUNC  0xDAB6E612,sceClockgenAudioClkSetFreq
        STUB_END
