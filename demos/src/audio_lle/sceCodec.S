        .set noreorder

#include "pspstub.s"

        STUB_START "sceCodec_driver",0x00070000,0x00020005
        STUB_FUNC  0x376399B6,sceCodec_driver_376399B6
        STUB_FUNC  0xFCA6D35B,sceCodec_driver_FCA6D35B
        STUB_END
