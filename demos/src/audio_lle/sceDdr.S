        .set noreorder

#include "pspstub.s"

        STUB_START "sceDdr_driver",0x00070000,0x00010005
        STUB_FUNC  0x87D86769,sceDdrFlush
        STUB_END
