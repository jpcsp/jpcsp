#ifndef __SASCORE_H
	#define __SASCORE_H

	typedef struct {
		unsigned int data[1024];
	} SasCore;

	#define PSP_SAS_EFFECT_TYPE_OFF   -1
	#define PSP_SAS_EFFECT_TYPE_ROOM   0
	#define PSP_SAS_EFFECT_TYPE_UNK1   1
	#define PSP_SAS_EFFECT_TYPE_UNK2   2
	#define PSP_SAS_EFFECT_TYPE_UNK3   3
	#define PSP_SAS_EFFECT_TYPE_HALL   4
	#define PSP_SAS_EFFECT_TYPE_SPACE  5
	#define PSP_SAS_EFFECT_TYPE_ECHO   6
	#define PSP_SAS_EFFECT_TYPE_DELAY  7
	#define PSP_SAS_EFFECT_TYPE_PIPE   8

	#define PSP_SAS_VOICES_MAX          32
	#define PSP_SAS_GRAIN_SAMPLES       256
	#define PSP_SAS_VOL_MAX             0x1000
	#define PSP_SAS_LOOP_MODE_OFF       0
	#define PSP_SAS_LOOP_MODE_ON        1
	#define PSP_SAS_PITCH_MIN           0x1
	#define PSP_SAS_PITCH_BASE          0x1000
	#define PSP_SAS_PITCH_MAX           0x4000
	#define PSP_SAS_NOISE_FREQ_MAX      0x3F;
	#define PSP_SAS_ENVELOPE_HEIGHT_MAX 0x40000000
	#define PSP_SAS_ENVELOPE_FREQ_MAX   0x7FFFFFFF

	// Allowed curve modes:
	// Attack:	PSP_SAS_ADSR_CURVE_MODE_LINEAR_INCREASE, PSP_SAS_ADSR_CURVE_MODE_LINEAR_BENT, PSP_SAS_ADSR_CURVE_MODE_EXPONENT
	// Decay:	PSP_SAS_ADSR_CURVE_MODE_LINEAR_DECREASE, PSP_SAS_ADSR_CURVE_MODE_EXPONENT_REV, PSP_SAS_ADSR_CURVE_MODE_DIRECT
	// Sustain:	all
	// Release:	PSP_SAS_ADSR_CURVE_MODE_LINEAR_DECREASE, PSP_SAS_ADSR_CURVE_MODE_EXPONENT_REV, PSP_SAS_ADSR_CURVE_MODE_DIRECT
	#define PSP_SAS_ADSR_CURVE_MODE_LINEAR_INCREASE 0
	#define PSP_SAS_ADSR_CURVE_MODE_LINEAR_DECREASE 1
	#define PSP_SAS_ADSR_CURVE_MODE_LINEAR_BENT     2
	#define PSP_SAS_ADSR_CURVE_MODE_EXPONENT_REV    3
	#define PSP_SAS_ADSR_CURVE_MODE_EXPONENT        4
	#define PSP_SAS_ADSR_CURVE_MODE_DIRECT          5

	#define PSP_SAS_ADSR_ATTACK  1
	#define PSP_SAS_ADSR_DECAY   2
	#define PSP_SAS_ADSR_SUSTAIN 4
	#define PSP_SAS_ADSR_RELEASE 8

	#define PSP_SAS_OUTPUTMODE_STEREO       0
	#define PSP_SAS_OUTPUTMODE_MULTICHANNEL 1

	/**
	 * Initialized a sasCore structure.
	 * Note: PSP can only handle one at a time.
	 *
	 * @example __sceSasInit(&sasCore, PSP_SAS_GRAIN_SAMPLES, PSP_SAS_VOICES_MAX, OutputMode.PSP_SAS_OUTPUTMODE_STEREO, 44100);
	 *
	 * @param   sasCore       Pointer to a SasCore structure that will contain information.
	 * @param   grainSamples  Number of grainSamples
	 * @param   maxVoices     Max number of voices
	 * @param   outMode       Out Mode
	 * @param   sampleRate    Sample Rate
	 *
	 * @return  0 on success
	 */
	int __sceSasInit(SasCore* sasCore, int grainSamples, int maxVoices, int outMode, int sampleRate);

	int __sceSasSetADSR(SasCore *sasCore, int voice, int flag, int attack, int decay, int sustain, int release);
	int __sceSasRevParam(SasCore *sasCore, int delay, int feedback);
	int __sceSasGetPauseFlag(SasCore *sasCore);
	int __sceSasRevType(SasCore *sasCore, int type);
	int __sceSasSetVolume(SasCore *sasCore, int voice, int leftVolume, int rightVolume, int effectLeftVolumne, int effectRightVolume);
	int __sceSasCoreWithMix(SasCore *sasCore, void *sasInOut, int leftVol, int rightVol);
	int __sceSasSetSL(SasCore *sasCore, int voice, int level);
	int __sceSasGetEndFlag(SasCore *sasCore);
	int __sceSasGetEnvelopeHeight(SasCore *sasCore, int voice);
	int __sceSasSetKeyOn(SasCore *sasCore, int voice);
	int __sceSasSetPause(SasCore *sasCore, int voice_bit, int setPause);
	int __sceSasSetVoice(SasCore *sasCore, int voice, void *vagAddr, int size, int loopmode);
	int __sceSasSetADSRmode(SasCore *sasCore, int voice, int flag, int attackType, int decayType, int sustainType, int releaseType);
	int __sceSasSetKeyOff(SasCore *sasCore, int voice);
	int __sceSasSetTrianglarWave(SasCore *sasCore);
	int __sceSasCore(SasCore *sasCore, void *sasOut);
	int __sceSasSetPitch(SasCore *sasCore, int voice, int pitch);
	int __sceSasSetNoise(SasCore *sasCore, int voice, int freq);
	int __sceSasGetGrain(SasCore *sasCore);
	int __sceSasSetSimpleADSR(SasCore *sasCore, int voice, int ADSREnv1, int ADSREnv2);
	int __sceSasSetGrain(SasCore *sasCore, int grain);
	int __sceSasRevEVOL(SasCore *sasCore, int leftVol, int rightVol);
	int __sceSasSetSteepWave(SasCore *sasCore);
	int __sceSasGetOutputmode(SasCore *sasCore);
	int __sceSasSetOutputmode(SasCore *sasCore, int mode);
	int __sceSasRevVON(SasCore *sasCore, int dry, int wet);
	int __sceSasGetAllEnvelopeHeights(SasCore *sasCore, int *heights);

#endif
