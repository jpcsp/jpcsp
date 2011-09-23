        .set noreorder

#include "pspimport.s"

#ifdef F_sceUtility_0000
        IMPORT_START    "sceUtility",0x40010000
#endif
#ifdef F_sceUtility_0001
        IMPORT_FUNC     "sceUtility",0xC492F751,sceUtilityGameSharingInitStart
#endif
#ifdef F_sceUtility_0002
        IMPORT_FUNC     "sceUtility",0xEFC6F80F,sceUtilityGameSharingShutdownStart
#endif
#ifdef F_sceUtility_0003
        IMPORT_FUNC     "sceUtility",0x7853182D,sceUtilityGameSharingUpdate
#endif
#ifdef F_sceUtility_0004
        IMPORT_FUNC     "sceUtility",0x946963F3,sceUtilityGameSharingGetStatus
#endif
#ifdef F_sceUtility_0005
        IMPORT_FUNC     "sceUtility",0x3AD50AE7,sceNetplayDialogInitStart
#endif
#ifdef F_sceUtility_0006
        IMPORT_FUNC     "sceUtility",0xBC6B6296,sceNetplayDialogShutdownStart
#endif
#ifdef F_sceUtility_0007
        IMPORT_FUNC     "sceUtility",0x417BED54,sceNetplayDialogUpdate
#endif
#ifdef F_sceUtility_0008
        IMPORT_FUNC     "sceUtility",0xB6CEE597,sceNetplayDialogGetStatus
#endif
#ifdef F_sceUtility_0009
        IMPORT_FUNC     "sceUtility",0x4DB1E739,sceUtilityNetconfInitStart
#endif
#ifdef F_sceUtility_0010
        IMPORT_FUNC     "sceUtility",0xF88155F6,sceUtilityNetconfShutdownStart
#endif
#ifdef F_sceUtility_0011
        IMPORT_FUNC     "sceUtility",0x91E70E35,sceUtilityNetconfUpdate
#endif
#ifdef F_sceUtility_0012
        IMPORT_FUNC     "sceUtility",0x6332AA39,sceUtilityNetconfGetStatus
#endif
#ifdef F_sceUtility_0013
        IMPORT_FUNC     "sceUtility",0x50C4CD57,sceUtilitySavedataInitStart
#endif
#ifdef F_sceUtility_0014
        IMPORT_FUNC     "sceUtility",0x9790B33C,sceUtilitySavedataShutdownStart
#endif
#ifdef F_sceUtility_0015
        IMPORT_FUNC     "sceUtility",0xD4B95FFB,sceUtilitySavedataUpdate
#endif
#ifdef F_sceUtility_0016
        IMPORT_FUNC     "sceUtility",0x8874DBE0,sceUtilitySavedataGetStatus
#endif
#ifdef F_sceUtility_0017
        IMPORT_FUNC     "sceUtility",0x2995D020,sceUtility_2995D020
#endif
#ifdef F_sceUtility_0018
        IMPORT_FUNC     "sceUtility",0xB62A4061,sceUtility_B62A4061
#endif
#ifdef F_sceUtility_0019
        IMPORT_FUNC     "sceUtility",0xED0FAD38,sceUtility_ED0FAD38
#endif
#ifdef F_sceUtility_0020
        IMPORT_FUNC     "sceUtility",0x88BC7406,sceUtility_88BC7406
#endif
#ifdef F_sceUtility_0021
        IMPORT_FUNC     "sceUtility",0x2AD8E239,sceUtilityMsgDialogInitStart
#endif
#ifdef F_sceUtility_0022
        IMPORT_FUNC     "sceUtility",0x67AF3428,sceUtilityMsgDialogShutdownStart
#endif
#ifdef F_sceUtility_0023
        IMPORT_FUNC     "sceUtility",0x95FC253B,sceUtilityMsgDialogUpdate
#endif
#ifdef F_sceUtility_0024
        IMPORT_FUNC     "sceUtility",0x9A1C91D7,sceUtilityMsgDialogGetStatus
#endif
#ifdef F_sceUtility_0025
        IMPORT_FUNC     "sceUtility",0xF6269B82,sceUtilityOskInitStart
#endif
#ifdef F_sceUtility_0026
        IMPORT_FUNC     "sceUtility",0x3DFAEBA9,sceUtilityOskShutdownStart
#endif
#ifdef F_sceUtility_0027
        IMPORT_FUNC     "sceUtility",0x4B85C861,sceUtilityOskUpdate
#endif
#ifdef F_sceUtility_0028
        IMPORT_FUNC     "sceUtility",0xF3F76017,sceUtilityOskGetStatus
#endif
#ifdef F_sceUtility_0029
        IMPORT_FUNC     "sceUtility",0x45C18506,sceUtilitySetSystemParamInt
#endif
#ifdef F_sceUtility_0030
        IMPORT_FUNC     "sceUtility",0x41E30674,sceUtilitySetSystemParamString
#endif
#ifdef F_sceUtility_0031
        IMPORT_FUNC     "sceUtility",0xA5DA2406,sceUtilityGetSystemParamInt
#endif
#ifdef F_sceUtility_0032
        IMPORT_FUNC     "sceUtility",0x34B78343,sceUtilityGetSystemParamString
#endif
#ifdef F_sceUtility_0033
        IMPORT_FUNC     "sceUtility",0x5EEE6548,sceUtilityCheckNetParam
#endif
#ifdef F_sceUtility_0034
        IMPORT_FUNC     "sceUtility",0x434D4B3A,sceUtilityGetNetParam
#endif
#ifdef F_sceUtility_0035
        IMPORT_FUNC     "sceUtility",0x1579a159,sceUtilityLoadNetModule
#endif
#ifdef F_sceUtility_0036
        IMPORT_FUNC     "sceUtility",0x64d50c56,sceUtilityUnloadNetModule
#endif
#ifdef F_sceUtility_0037
        IMPORT_FUNC     "sceUtility",0xC629AF26,sceUtilityLoadAvModule
#endif
#ifdef F_sceUtility_0038
        IMPORT_FUNC     "sceUtility",0xF7D8D092,sceUtilityUnloadAvModule
#endif
#ifdef F_sceUtility_0039
        IMPORT_FUNC     "sceUtility",0xD81957B7,sceUtilityScreenshotGetStatus
#endif

