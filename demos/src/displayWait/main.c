#include <pspkernel.h>
#include <pspdebug.h>
#include <pspctrl.h>
#include <pspdisplay.h>
#include <pspgu.h>
#include <pspgum.h>
#include <psppower.h>

#include <sys/stat.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>

PSP_MODULE_INFO("sceDisplayWaitVblank Test", 0, 1, 0);
PSP_MAIN_THREAD_ATTR(THREAD_ATTR_USER | PSP_THREAD_ATTR_VFPU);

int done = 0;
int cpuFreq = 222;
int startSystemTime;
char buffer[10000000];

extern void sceDisplayWaitVblankStartMulti(int count);
extern int sceDisplayIsVblank();
extern int sceDisplayGetCurrentHcount();
extern int sceDisplayGetAccumulatedHcount();

void runTest()
{
	int cycles;
	int delay;
	sceDisplayWaitVblankStartMulti(1);
	for (cycles = 1; cycles <= 2; cycles++)
	{
		for (delay = 0; delay <= 40000; delay += 4000)
		{
			int n;
			int start = sceKernelGetSystemTimeLow();
			for (n = 0; n < 60; n++)
			{
				int startDelay = sceKernelGetSystemTimeLow();
				int endDelay = startDelay;
				while (endDelay < startDelay + delay)
				{
					endDelay = sceKernelGetSystemTimeLow();
				}
				sceDisplayWaitVblankStartMulti(cycles);
			}
			int end = sceKernelGetSystemTimeLow();
			int durationMicros = end - start;
			int durationMillis = (durationMicros + 500) / 1000;
			pspDebugScreenPrintf("sceDisplayWaitVblankStartMulti(%d) with %2d ms delay = %d ms\n", cycles, delay / 1000, durationMillis);
		}
	}
}

void runTestHcount()
{
	char s[100];
	strcpy(buffer, "");
	int index = 0;
	int previousIsVblank = -1;
	int previousCurrentHcount = -1;
	int previousAccumulatedHcount = -1;
	int start = sceKernelGetSystemTimeLow();
	while (1) {
		int now = sceKernelGetSystemTimeLow();
		// Run for 2 seconds
		if (now - start >= 2000000) {
			break;
		}

		int isVblank = sceDisplayIsVblank();
		int currentHcount = sceDisplayGetCurrentHcount();
		int accumulatedHcount = sceDisplayGetAccumulatedHcount();

		// Display a log line when one of the 3 values has changed
		if (isVblank != previousIsVblank || currentHcount != previousCurrentHcount || accumulatedHcount != previousAccumulatedHcount) {
			sprintf(s, "now=%d, isVblank=%d, currentHcount=%d, accumulatedHcount=%d\n", now, isVblank, currentHcount, accumulatedHcount);
			strcpy(buffer + index, s);
			index += strlen(s);

			previousIsVblank = isVblank;
			previousCurrentHcount = currentHcount;
			previousAccumulatedHcount = accumulatedHcount;
		}
	}

	// Write the result to a file
	SceUID logFd = sceIoOpen("sceDisplay.log", PSP_O_WRONLY | PSP_O_CREAT, 0777);
	sceIoWrite(logFd, buffer, index);
	sceIoClose(logFd);

	pspDebugScreenPrintf("See results in sceDisplay.log\n");
}

int main(int argc, char *argv[])
{
	SceCtrlData pad;
	int oldButtons = 0;
#define SECOND	   1000000
#define REPEAT_START (1 * SECOND)
#define REPEAT_DELAY (SECOND / 5)
	struct timeval repeatStart;
	struct timeval repeatDelay;

	repeatStart.tv_sec = 0;
	repeatStart.tv_usec = 0;
	repeatDelay.tv_sec = 0;
	repeatDelay.tv_usec = 0;

	pspDebugScreenInit();
	pspDebugScreenPrintf("Press Cross to start the Test sceDisplayWaitVblankStartMulti\n");
	pspDebugScreenPrintf("Press Circle to start the Test sceDisplayGetCurrentHcount\n");

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

		if (buttonDown & PSP_CTRL_CROSS)
		{
			runTest();
		}

		if (buttonDown & PSP_CTRL_CIRCLE)
		{
			runTestHcount();
		}

		if (buttonDown & PSP_CTRL_TRIANGLE)
		{
			done = 1;
		}

		oldButtons = pad.Buttons;
	}

	sceGuTerm();

	sceKernelExitGame();
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

