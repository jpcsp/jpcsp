	.set noreorder

#include "pspstub.s"

	STUB_START "sceSasCore",0x00070000,0x001C0005
	STUB_FUNC  0x019B25EB,__sceSasSetADSR
	STUB_FUNC  0x267A6DD2,__sceSasRevParam
	STUB_FUNC  0x2C8E6AB3,__sceSasGetPauseFlag
	STUB_FUNC  0x33D4AB37,__sceSasRevType
	STUB_FUNC  0x42778A9F,__sceSasInit
	STUB_FUNC  0x440CA7D8,__sceSasSetVolume
	STUB_FUNC  0x50A14DFC,__sceSasCoreWithMix
	STUB_FUNC  0x5F9529F6,__sceSasSetSL
	STUB_FUNC  0x68A46B95,__sceSasGetEndFlag
	STUB_FUNC  0x74AE582A,__sceSasGetEnvelopeHeight
	STUB_FUNC  0x76F01ACA,__sceSasSetKeyOn
	STUB_FUNC  0x787D04D5,__sceSasSetPause
	STUB_FUNC  0x99944089,__sceSasSetVoice
	STUB_FUNC  0x9EC3676A,__sceSasSetADSRmode
	STUB_FUNC  0xA0CF2FA4,__sceSasSetKeyOff
	STUB_FUNC  0xA232CBE6,__sceSasSetTrianglarWave
	STUB_FUNC  0xA3589D81,__sceSasCore
	STUB_FUNC  0xAD84D37F,__sceSasSetPitch
	STUB_FUNC  0xB7660A23,__sceSasSetNoise
	STUB_FUNC  0xBD11B7C2,__sceSasGetGrain
	STUB_FUNC  0xCBCD4F79,__sceSasSetSimpleADSR
	STUB_FUNC  0xD1E0A01E,__sceSasSetGrain
	STUB_FUNC  0xD5A229C9,__sceSasRevEVOL
	STUB_FUNC  0xD5EBBBCD,__sceSasSetSteepWave
	STUB_FUNC  0xE175EF66,__sceSasGetOutputmode
	STUB_FUNC  0xE855BF76,__sceSasSetOutputmode
	STUB_FUNC  0xF983B186,__sceSasRevVON
	STUB_FUNC  0x07F58C24,__sceSasGetAllEnvelopeHeights
	STUB_END
