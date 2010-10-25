
        .set noreorder

#include "pspimport.s"

#ifdef F_sceDisplayMulti_0000
        IMPORT_START    "sceDisplay",0x40010000
#endif
#ifdef F_sceDisplayMulti_0001
        IMPORT_FUNC     "sceDisplay",0x40F1469C,sceDisplayWaitVblankStartMulti
#endif
