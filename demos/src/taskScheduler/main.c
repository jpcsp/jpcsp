#include <pspkernel.h>
#include <pspdebug.h>
#include <pspctrl.h>
#include <pspdisplay.h>
#include <pspgu.h>
#include <pspgum.h>

#include <sys/stat.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>

PSP_MODULE_INFO("Task Scheduler Test", 0, 1, 0);
PSP_MAIN_THREAD_ATTR(THREAD_ATTR_USER);

int done = 0;

volatile int highPrioCounter = 0;
volatile int mediumPrioCounter = 0;
volatile int lowPrioCounter = 0;
volatile int testDone = 0;
char buffer[10000];
char *msg;

int threadHighPrio(SceSize args, void *argp)
{
	while (!testDone)
	{
		sceKernelDelayThread(50);
		highPrioCounter++;
	}

	return 0;
}


int threadMediumPrio(SceSize args, void *argp)
{
	sceKernelDelayThread(1000); /* Small delay to let the Busy Thread start immediately */
	while (!testDone)
	{
		sceKernelDelayThread(1000);
		mediumPrioCounter++;
	}

	return 0;
}


int threadLowPrio(SceSize args, void *argp)
{
	while (!testDone)
	{
		sceKernelDelayThread(1000);
		lowPrioCounter++;
	}

	return 0;
}


int threadBusy(SceSize args, void *argp)
{
	int i, j;
	while (!testDone)
	{
		for (j = 0; j < 50; j++)
		{
			for (i = 0; i < 1000000; i++)
			{
			}
		}
	}

	return 0;
}

int sleepingThread(SceSize args, void *argp)
{
	sceKernelSleepThread();
	strcat(msg, "Sleeping Thread\n");
	return 0;
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
	pspDebugScreenPrintf("Press Cross to start the Task Scheduler Test\n");
	pspDebugScreenPrintf("Press Circle to start the CpuSuspendIntr/CpuResumeIntr Test\n");
	pspDebugScreenPrintf("Press Triangle to Exit\n");

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
			SceUID lowThid = sceKernelCreateThread("Low Prio Thread", threadLowPrio, 0x70, 0x1000, 0, 0);
			SceUID mediumThid = sceKernelCreateThread("Medium Prio Thread", threadMediumPrio, 0x30, 0x1000, 0, 0);
			SceUID highThid = sceKernelCreateThread("High Prio Thread", threadHighPrio, 0x10, 0x1000, 0, 0);
			SceUID busyThid = sceKernelCreateThread("Busy Thread", threadBusy, 0x30, 0x1000, 0, 0);
			testDone = 0;
			highPrioCounter = 0;
			mediumPrioCounter = 0;
			lowPrioCounter = 0;
			sceKernelStartThread(lowThid, 0, 0);
			sceKernelStartThread(mediumThid, 0, 0);
			sceKernelStartThread(busyThid, 0, 0);
			sceKernelStartThread(highThid, 0, 0);
			int totalDelay = 5000000;
			sceKernelDelayThread(totalDelay);
			testDone = 1;
			sceKernelWaitThreadEnd(busyThid, NULL);
			sceKernelWaitThreadEnd(mediumThid, NULL);
			sceKernelWaitThreadEnd(highThid, NULL);
			sceKernelWaitThreadEnd(lowThid, NULL);
			pspDebugScreenPrintf("Counters: high=%d (%d us), medium=%d, low=%d\n", highPrioCounter, (totalDelay / highPrioCounter), mediumPrioCounter, lowPrioCounter);
		}

		if (buttonDown & PSP_CTRL_CIRCLE)
		{
			msg = buffer;
			strcpy(msg, "");
			SceUID sleepingThid = sceKernelCreateThread("Sleeping Thread", sleepingThread, 0x10, 0x1000, 0, 0);
			sceKernelStartThread(sleepingThid, 0, 0);
			sceKernelDelayThread(100000);
			int intr = sceKernelCpuSuspendIntr();
			sceKernelWakeupThread(sleepingThid);
			strcat(msg, "Main Thread with disabled interrupts\n");
			sceKernelCpuResumeIntr(intr);
			strcat(msg, "Main Thread with enabled interrupts\n");
			pspDebugScreenPrintf("%s", msg);
		}

		if (buttonDown & PSP_CTRL_SQUARE)
		{
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

