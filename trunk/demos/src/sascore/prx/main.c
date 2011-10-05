#include <pspkernel.h>
#include <pspsdk.h>
#include <pspdebug.h>
#include <pspctrl.h>
#include <pspdisplay.h>
#include <pspgu.h>
#include <pspgum.h>
#include <psppower.h>
#include <psputility_avmodules.h>

#include <sys/stat.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>

#include "sascore.h"

PSP_MODULE_INFO("sceSasCore Test", 0, 1, 0);
PSP_MAIN_THREAD_ATTR(THREAD_ATTR_USER | PSP_THREAD_ATTR_VFPU);

int done = 0;
SceUID logFd;
static SasCore __attribute__((aligned(256))) sasCore;
static char __attribute__((aligned(256))) buffer[100000];
static unsigned short __attribute__((aligned(256))) sasOut[100000];
int result;
int grain;
int outputMode;
int voice;
int loopmode;
int pitch;
int volume;
int samples;
int attack;
int decay;
int sustain;
int release;
int attackCurveType;
int decayCurveType;
int sustainCurveType;
int releaseCurveType;
int sustainLevel;
int sasIndex = 0;
int dumpFlags = 0;
int dumpHeights = 0;
int startTime;
int endTime;
char s[1000];
char l[1000];


static unsigned char vagSample[] = {
// 18 lines * 28 samples = 504 (0x1F8) samples
0x00, 0x00, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77,
0x00, 0x00, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77,
0x00, 0x00, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77,
0x00, 0x00, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77,
0x00, 0x00, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77,
0x00, 0x00, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77,
0x00, 0x00, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77,
0x00, 0x00, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77,
0x00, 0x00, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77,
0x00, 0x00, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77,
0x00, 0x00, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77,
0x00, 0x00, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77,
0x00, 0x00, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77,
0x00, 0x00, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77,
0x00, 0x00, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77,
0x00, 0x00, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77,
0x00, 0x00, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77,
0x00, 0x00, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77,
0x00, 0x07, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77
};

void init()
{
	result = __sceSasInit(&sasCore, grain, PSP_SAS_VOICES_MAX, outputMode, 44100);
	if (result != 0) pspDebugScreenPrintf("__sceSasInit result 0x%08X\n", result);
}

void dumpSasOut(unsigned short *sasOut, int samples)
{
	int i;

	strcpy(l, "");
	for (i = 0; i < samples; i++)
	{
		if (i % 8 == 0)
		{
			if (i > 0)
			{
				strcat(l, "\n");
			}
			sprintf(s, "%04X: ", sasIndex);
			strcat(l, s);
			sceIoWrite(logFd, l, strlen(l));
			strcpy(l, "");
		}
		else
		{
			strcat(l, " ");
		}
		sprintf(s, "%04X-%04X", sasOut[i * 2], sasOut[i * 2 + 1]);
		strcat(l, s);

		sasIndex++;
	}
	strcat(l, "\n");
	sceIoWrite(logFd, l, strlen(l));
}

void setDefaults()
{
	grain = PSP_SAS_GRAIN_SAMPLES;
	outputMode = PSP_SAS_OUTPUTMODE_STEREO;
	voice = 0;
	loopmode = 0;
	pitch = PSP_SAS_PITCH_BASE;
	volume = PSP_SAS_VOL_MAX;
	samples = PSP_SAS_GRAIN_SAMPLES;

	attack = PSP_SAS_ENVELOPE_FREQ_MAX;
	decay = 0;
	sustain = PSP_SAS_ENVELOPE_FREQ_MAX;
	release = 0;
	attackCurveType = PSP_SAS_ADSR_CURVE_MODE_LINEAR_INCREASE;
	decayCurveType = PSP_SAS_ADSR_CURVE_MODE_LINEAR_DECREASE;
	sustainCurveType = PSP_SAS_ADSR_CURVE_MODE_LINEAR_INCREASE;
	releaseCurveType = PSP_SAS_ADSR_CURVE_MODE_LINEAR_DECREASE;
	sustainLevel = PSP_SAS_ENVELOPE_FREQ_MAX >> 1;
}

void setVoice()
{
	int vagDataSize;

	SceUID vagFile = sceIoOpen("sample.vag", PSP_O_RDONLY, 0);
	if (vagFile < 0)
	{
		// Use default sample
		vagDataSize = sizeof(vagSample);
		memcpy(buffer, vagSample, vagDataSize);
	}
	else
	{
		// Use sample.vag file contents
		vagDataSize = sceIoRead(vagFile, buffer, sizeof(buffer));
		sceIoClose(vagFile);
	}

	sasIndex = 0;

	sprintf(s, "SetVoice voice=%d, loopmode=%d, pitch=0x%X, volume=0x%X\n", voice, loopmode, pitch, volume);
	sceIoWrite(logFd, s, strlen(s));
	sprintf(s, "         attack=0x%08X(%d), decay=0x%08X(%d), sustain=0x%08X(%d), sustain level=0x%08X, release=0x%08X(%d)\n", attack, attackCurveType, decay, decayCurveType, sustain, sustainCurveType, sustainLevel, release, releaseCurveType);
	sceIoWrite(logFd, s, strlen(s));

	result = __sceSasSetVoice(&sasCore, voice, buffer, vagDataSize, loopmode);
	if (result != 0) pspDebugScreenPrintf("__sceSasSetVoice result 0x%08X\n", result);

	result = __sceSasSetPitch(&sasCore, voice, pitch);
	if (result != 0) pspDebugScreenPrintf("__sceSasSetPitch result 0x%08X\n", result);

	result = __sceSasSetADSR(&sasCore, voice, PSP_SAS_ADSR_ATTACK | PSP_SAS_ADSR_DECAY | PSP_SAS_ADSR_SUSTAIN | PSP_SAS_ADSR_RELEASE, attack, decay, sustain, release);
	if (result != 0) pspDebugScreenPrintf("__sceSasSetADSR result 0x%08X\n", result);

	result = __sceSasSetADSRmode(&sasCore, voice, PSP_SAS_ADSR_ATTACK | PSP_SAS_ADSR_DECAY | PSP_SAS_ADSR_SUSTAIN | PSP_SAS_ADSR_RELEASE, attackCurveType, decayCurveType, sustainCurveType, releaseCurveType);
	if (result != 0) pspDebugScreenPrintf("__sceSasSetADSRmode result 0x%08X\n", result);

	result = __sceSasSetSL(&sasCore, voice, sustainLevel);
	if (result != 0) pspDebugScreenPrintf("__sceSasSetSL result 0x%08X\n", result);

	result = __sceSasSetVolume(&sasCore, voice, volume, volume, volume, volume);
	if (result != 0) pspDebugScreenPrintf("__sceSasSetVolume result 0x%08X\n", result);

	result = __sceSasSetKeyOn(&sasCore, voice);
	if (result != 0) pspDebugScreenPrintf("__sceSasSetKeyOn result 0x%08X\n", result);
}


void sceSasCore(int count)
{
	int i;
	int endFlag;
	int pauseFlag;
	int height;

	for (i = 0; i < 20; i++)
	{
		if (dumpFlags)
		{
			endFlag = __sceSasGetEndFlag(&sasCore);
			pauseFlag = __sceSasGetPauseFlag(&sasCore);
			sprintf(s, "      end=0x%08X, pause=0x%08X\n", endFlag, pauseFlag);
			sceIoWrite(logFd, s, strlen(s));
		}
		if (dumpHeights)
		{
			height = __sceSasGetEnvelopeHeight(&sasCore, voice);
			sprintf(s, "      height=0x%08X\n", height);
			sceIoWrite(logFd, s, strlen(s));
		}

		memset(sasOut, 0, sizeof(sasOut));
		result = __sceSasCore(&sasCore, sasOut);

		if (i < count)
		{
			dumpSasOut(sasOut, samples);
		}

		int endFlag = __sceSasGetEndFlag(&sasCore);
		if ((endFlag & (1 << voice)) != 0)
		{
			break;
		}
	}

	if (dumpFlags)
	{
		endFlag = __sceSasGetEndFlag(&sasCore);
		pauseFlag = __sceSasGetPauseFlag(&sasCore);
		sprintf(s, "      end=0x%08X, pause=0x%08X\n", endFlag, pauseFlag);
		sceIoWrite(logFd, s, strlen(s));
	}
	if (dumpHeights)
	{
		height = __sceSasGetEnvelopeHeight(&sasCore, voice);
		sprintf(s, "      height=0x%08X\n", height);
		sceIoWrite(logFd, s, strlen(s));
	}
}


void runTestPause()
{
	int i;

	setDefaults();
	setVoice();
	for (i = 0; i < 20; i++)
	{
		if (i == 1)
		{
			result = __sceSasSetPause(&sasCore, (1 << voice), 1);
			if (result != 0) pspDebugScreenPrintf("__sceSasSetPause set pause, result 0x%08X\n", result);
		}
		else if (i == 3)
		{
			result = __sceSasSetPause(&sasCore, (1 << voice), 0);
			if (result != 0) pspDebugScreenPrintf("__sceSasSetPause reset pause, result 0x%08X\n", result);
		}

		memset(sasOut, 0, sizeof(sasOut));
		result = __sceSasCore(&sasCore, sasOut);
		pspDebugScreenPrintf("__sceSasCore result 0x%08X, pause flag 0x%08X\n", result, __sceSasGetPauseFlag(&sasCore));

		dumpSasOut(sasOut, samples);

		int endFlag = __sceSasGetEndFlag(&sasCore);
		if ((endFlag & (1 << voice)) != 0)
		{
			break;
		}
	}
}

void runTestPitch()
{
	int i;

	setDefaults();
	for (i = 0; i < 16; i++)
	{
		sprintf(s, "Pitch 0x%X\n", pitch);
		pspDebugScreenPrintf(s);
		sceIoWrite(logFd, s, strlen(s));

		setVoice();
		sceSasCore(1000);
		pitch += 0x200;
	}
}


void runTestAttack()
{
	int i;

	setDefaults();
	attack = PSP_SAS_ENVELOPE_FREQ_MAX;
	for (i = 0; i < 16; i++)
	{
		sprintf(s, "Attack rate 0x%X, curve type %d\n", attack, attackCurveType);
		pspDebugScreenPrintf(s);
		sceIoWrite(logFd, s, strlen(s));

		setVoice();
		sceSasCore(1);
		attack >>= 1;
	}
}


void runTestDecay()
{
	int i;

	setDefaults();
	decay = PSP_SAS_ENVELOPE_FREQ_MAX;
	sustain = PSP_SAS_ENVELOPE_FREQ_MAX >> 1;
	sustainLevel = PSP_SAS_ENVELOPE_HEIGHT_MAX >> 2;
	for (i = 0; i < 16; i++)
	{
		sprintf(s, "Decay rate 0x%X, curve type %d, sustain level 0x%08X\n", decay, decayCurveType, sustainLevel);
		pspDebugScreenPrintf(s);
		sceIoWrite(logFd, s, strlen(s));

		setVoice();
		sceSasCore(1);
		decay >>= 1;
	}
}


void runTestSustain()
{
	int i;

	setDefaults();
	decay = PSP_SAS_ENVELOPE_FREQ_MAX;
	sustainLevel = 0;
	for (i = 0; i < 16; i++)
	{
		sprintf(s, "Sustain rate 0x%X, curve type %d, sustain level 0x%08X\n", sustain, sustainCurveType, sustainLevel);
		pspDebugScreenPrintf(s);
		sceIoWrite(logFd, s, strlen(s));

		setVoice();
		sceSasCore(1);
		sustain >>= 1;
	}
}


void runTestRelease()
{
	int i;

	dumpFlags = 1;
	setDefaults();
	release = PSP_SAS_ENVELOPE_FREQ_MAX;
	for (i = 0; i < 16; i++)
	{
		sprintf(s, "Release rate 0x%X, curve type %d\n", release, releaseCurveType);
		pspDebugScreenPrintf(s);
		sceIoWrite(logFd, s, strlen(s));

		setVoice();
		// Call sceSasCore once then sceSasSetKeyOff to trigger the release
		__sceSasCore(&sasCore, sasOut);
		__sceSasSetKeyOff(&sasCore, voice);
		sceSasCore(release > 0x003FFFFF ? 1 : 2);
		release >>= 1;
	}
	dumpFlags = 0;
}


void runTestCurveType()
{
	setDefaults();

	attack = PSP_SAS_ENVELOPE_FREQ_MAX >> 6;
	attackCurveType = PSP_SAS_ADSR_CURVE_MODE_LINEAR_BENT;
	pspDebugScreenPrintf("Attack rate 0x%X, curve type %d\n", attack, attackCurveType);
	setVoice();
	sceSasCore(1);

	attack = PSP_SAS_ENVELOPE_FREQ_MAX >> 7;
	attackCurveType = PSP_SAS_ADSR_CURVE_MODE_LINEAR_BENT;
	pspDebugScreenPrintf("Attack rate 0x%X, curve type %d\n", attack, attackCurveType);
	setVoice();
	sceSasCore(1);

	attack = PSP_SAS_ENVELOPE_FREQ_MAX >> 0;
	attackCurveType = PSP_SAS_ADSR_CURVE_MODE_EXPONENT;
	pspDebugScreenPrintf("Attack rate 0x%X, curve type %d\n", attack, attackCurveType);
	setVoice();
	sceSasCore(1);

	attack = PSP_SAS_ENVELOPE_FREQ_MAX >> 1;
	attackCurveType = PSP_SAS_ADSR_CURVE_MODE_EXPONENT;
	pspDebugScreenPrintf("Attack rate 0x%X, curve type %d\n", attack, attackCurveType);
	setVoice();
	sceSasCore(1);

	attack = PSP_SAS_ENVELOPE_FREQ_MAX >> 2;
	attackCurveType = PSP_SAS_ADSR_CURVE_MODE_EXPONENT;
	pspDebugScreenPrintf("Attack rate 0x%X, curve type %d\n", attack, attackCurveType);
	setVoice();
	sceSasCore(1);

	attack = PSP_SAS_ENVELOPE_FREQ_MAX >> 3;
	attackCurveType = PSP_SAS_ADSR_CURVE_MODE_EXPONENT;
	pspDebugScreenPrintf("Attack rate 0x%X, curve type %d\n", attack, attackCurveType);
	setVoice();
	sceSasCore(1);

	attack = PSP_SAS_ENVELOPE_FREQ_MAX >> 4;
	attackCurveType = PSP_SAS_ADSR_CURVE_MODE_EXPONENT;
	pspDebugScreenPrintf("Attack rate 0x%X, curve type %d\n", attack, attackCurveType);
	setVoice();
	sceSasCore(2);

	attack = PSP_SAS_ENVELOPE_FREQ_MAX >> 5;
	attackCurveType = PSP_SAS_ADSR_CURVE_MODE_EXPONENT;
	pspDebugScreenPrintf("Attack rate 0x%X, curve type %d\n", attack, attackCurveType);
	setVoice();
	sceSasCore(2);

	attack = PSP_SAS_ENVELOPE_FREQ_MAX >> 6;
	attackCurveType = PSP_SAS_ADSR_CURVE_MODE_EXPONENT;
	pspDebugScreenPrintf("Attack rate 0x%X, curve type %d\n", attack, attackCurveType);
	setVoice();
	sceSasCore(3);

	setDefaults();
	sustain = 0;

	decay = PSP_SAS_ENVELOPE_FREQ_MAX >> 6;
	decayCurveType = PSP_SAS_ADSR_CURVE_MODE_DIRECT;
	sustainLevel = PSP_SAS_ENVELOPE_FREQ_MAX >> 3;
	pspDebugScreenPrintf("Decay rate 0x%X, curve type %d, sustain level 0x%08X\n", decay, decayCurveType, sustainLevel);
	setVoice();
	sceSasCore(1);

	decay = PSP_SAS_ENVELOPE_FREQ_MAX >> 6;
	decayCurveType = PSP_SAS_ADSR_CURVE_MODE_DIRECT;
	sustainLevel = PSP_SAS_ENVELOPE_FREQ_MAX >> 4;
	pspDebugScreenPrintf("Decay rate 0x%X, curve type %d, sustain level 0x%08X\n", decay, decayCurveType, sustainLevel);
	setVoice();
	sceSasCore(1);
}

void startTiming()
{
	startTime = sceKernelGetSystemTimeLow();
}

void endTiming(char *comment)
{
	endTime = sceKernelGetSystemTimeLow();
	sprintf(s, "%s: %d us\n", comment, endTime - startTime);
	pspDebugScreenPrintf(s);
	sceIoWrite(logFd, s, strlen(s));
}

void runTestTiming()
{
	int i;

	// Timing with no voice playing
	startTiming();
	result = __sceSasCore(&sasCore, sasOut);
	endTiming("No voice playing");

	// Timing with 1 voice playing
	setDefaults();
	setVoice();
	for (i = 0; i < 2; i++)
	{
		startTiming();
		result = __sceSasCore(&sasCore, sasOut);
		endTiming("One voice playing");
	}
	sceSasCore(0);

	// Timing with 2 voices playing
	setDefaults();
	setVoice();
	voice++;
	setVoice();
	for (i = 0; i < 2; i++)
	{
		startTiming();
		result = __sceSasCore(&sasCore, sasOut);
		endTiming("Two voices playing");
	}
	sceSasCore(0);

	// Timing with more samples
	setDefaults();
	grain <<= 2;
	samples <<= 2;
	init();
	setVoice();
	for (i = 0; i < 2; i++)
	{
		startTiming();
		result = __sceSasCore(&sasCore, sasOut);
		endTiming("4x samples");
	}
	sceSasCore(0);
}


int main_thread(SceSize _argc, ScePVoid _argp)
{
	SceCtrlData pad;
	int oldButtons = 0;
#define SECOND	   1000000
#define REPEAT_START (1 * SECOND)
#define REPEAT_DELAY (SECOND / 5)
	struct timeval repeatStart;
	struct timeval repeatDelay;
	int previousPauseFlag = 0;
	int previousEndFlag = -1;
	int pauseFlag;
	int endFlag;

	repeatStart.tv_sec = 0;
	repeatStart.tv_usec = 0;
	repeatDelay.tv_sec = 0;
	repeatDelay.tv_usec = 0;

	sceIoRemove("ms0:/sascore.log");
	logFd = sceIoOpen("ms0:/sascore.log", PSP_O_WRONLY | PSP_O_CREAT, 0777);

	pspDebugScreenInit();
	pspDebugScreenPrintf("Press Triangle to exit\n");
	pspDebugScreenPrintf("Press Cross to start the sceSasSetPause Test\n");
	pspDebugScreenPrintf("Press Circle to start the sceSasSetPitch Test\n");
	pspDebugScreenPrintf("Press Square to start the ADSR attack Test\n");
	pspDebugScreenPrintf("Press Left to start the ADSR decay Test\n");
	pspDebugScreenPrintf("Press Right to start the ADSR sustain Test\n");
	pspDebugScreenPrintf("Press Up to start the ADSR release Test\n");
	pspDebugScreenPrintf("Press Down to start the curve type Test\n");
	pspDebugScreenPrintf("Press Left Trigger to start the timing Test\n");

	setDefaults();
	init();
	dumpHeights = 1;

	while(!done)
	{
		sceCtrlReadBufferPositive(&pad, 1);
		int buttonDown = (oldButtons ^ pad.Buttons) & pad.Buttons;

		if (pad.Buttons == oldButtons)
		{
			struct timeval now;
			gettimeofday(&now, NULL);
			if (repeatStart.tv_sec == 0)
			{
				repeatStart.tv_sec = now.tv_sec;
				repeatStart.tv_usec = now.tv_usec;
				repeatDelay.tv_sec = 0;
				repeatDelay.tv_usec = 0;
			}
			else
			{
				long usec = (now.tv_sec - repeatStart.tv_sec) * SECOND;
				usec += (now.tv_usec - repeatStart.tv_usec);
				if (usec >= REPEAT_START)
				{
					if (repeatDelay.tv_sec != 0)
					{
						usec = (now.tv_sec - repeatDelay.tv_sec) * SECOND;
						usec += (now.tv_usec - repeatDelay.tv_usec);
						if (usec >= REPEAT_DELAY)
						{
							repeatDelay.tv_sec = 0;
						}
					}

					if (repeatDelay.tv_sec == 0)
					{
						buttonDown = pad.Buttons;
						repeatDelay.tv_sec = now.tv_sec;
						repeatDelay.tv_usec = now.tv_usec;
					}
				}
			}
		}
		else
		{
			repeatStart.tv_sec = 0;
		}

		endFlag = __sceSasGetEndFlag(&sasCore);
		pauseFlag = __sceSasGetPauseFlag(&sasCore);
		if (endFlag != previousEndFlag || pauseFlag != previousPauseFlag)
		{
			pspDebugScreenPrintf("End flag: 0x%08X, Pause flag: 0x%08X\n", endFlag, pauseFlag);
			previousEndFlag = endFlag;
			previousPauseFlag = pauseFlag;
		}

		if (buttonDown & PSP_CTRL_CROSS)
		{
			runTestPause();
		}

		if (buttonDown & PSP_CTRL_CIRCLE)
		{
			runTestPitch();
		}

		if (buttonDown & PSP_CTRL_SQUARE)
		{
			runTestAttack();
		}

		if (buttonDown & PSP_CTRL_LEFT)
		{
			runTestDecay();
		}

		if (buttonDown & PSP_CTRL_RIGHT)
		{
			runTestSustain();
		}

		if (buttonDown & PSP_CTRL_UP)
		{
			runTestRelease();
		}

		if (buttonDown & PSP_CTRL_DOWN)
		{
			runTestCurveType();
		}

		if (buttonDown & PSP_CTRL_LTRIGGER)
		{
			runTestTiming();
		}

		if (buttonDown & PSP_CTRL_TRIANGLE)
		{
			pspDebugScreenPrintf("Exiting...\n");
			done = 1;
		}

		oldButtons = pad.Buttons;
	}

	sceGuTerm();

	sceIoClose(logFd);

	return 0;
}

extern int module_start(SceSize _argc, char* _argp)
{
	char* arg = _argp + strlen(_argp) + 1;

	SceUID T = sceKernelCreateThread("main_thread", main_thread, 0x20, 0x10000, THREAD_ATTR_USER | PSP_THREAD_ATTR_VFPU, NULL);

	sceKernelStartThread(T, strlen(arg)+1, arg);

	sceKernelWaitThreadEnd(T, 0);

	return 0;
}


/* Exit callback */
int exit_callback(int arg1, int arg2, void *common)
{
	done = 1;
	return 0;
}

/* Callback thread */
int CallbackThread(SceSize args, void *argp)
{
	int cbid;

	cbid = sceKernelCreateCallback("Exit Callback", exit_callback, (void*)0);
	sceKernelRegisterExitCallback(cbid);

	sceKernelSleepThreadCB();

	return 0;
}

/* Sets up the callback thread and returns its thread id */
int SetupCallbacks(void)
{
	int thid = 0;

	thid = sceKernelCreateThread("CallbackThread", CallbackThread, 0x11, 0xFA0, 0, 0);
	if(thid >= 0)
	{
		sceKernelStartThread(thid, 0, 0);
	}

	return thid;
}

