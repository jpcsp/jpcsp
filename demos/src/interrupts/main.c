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

PSP_MODULE_INFO("Interrupts Test", 0, 1, 0);
PSP_MAIN_THREAD_ATTR(THREAD_ATTR_USER);

/* Define printf, just to make typing easier */
#define printf  pspDebugScreenPrintf

int done = 0;

char text[10000];


int main(int argc, char *argv[])
{
    SceCtrlData pad;
    int oldButtons = 0;
    int i;
#define SECOND       1000000
#define REPEAT_START (1 * SECOND)
#define REPEAT_DELAY (SECOND / 5)
    struct timeval repeatStart;
    struct timeval repeatDelay;

    repeatStart.tv_sec = 0;
    repeatStart.tv_usec = 0;
    repeatDelay.tv_sec = 0;
    repeatDelay.tv_usec = 0;

    pspDebugScreenInit();

    printf("Triangle - Exit\n");
    printf("Cross - Run sceKernel Interrupts Test\n");

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
        }

        if (buttonDown & PSP_CTRL_SQUARE)
        {
        }

        if (buttonDown & PSP_CTRL_CROSS)
        {
			int flag1 = sceKernelIsCpuIntrEnable();
			int state1 = sceKernelCpuSuspendIntr();
			int flag2 = sceKernelIsCpuIntrEnable();
			int state2 = sceKernelCpuSuspendIntr();
			int flag3 = sceKernelIsCpuIntrEnable();
			int test1 = sceKernelIsCpuIntrSuspended(1);
			int test2 = sceKernelIsCpuIntrSuspended(0);
			sceKernelCpuResumeIntr(state2);
			int flag4 = sceKernelIsCpuIntrEnable();
			sceKernelCpuResumeIntr(state1);
			int flag5 = sceKernelIsCpuIntrEnable();
			sceKernelCpuResumeIntr(state2);
			int flag6 = sceKernelIsCpuIntrEnable();
			sceKernelCpuResumeIntr(state1);
			int flag7 = sceKernelIsCpuIntrEnable();
			int test3 = sceKernelIsCpuIntrSuspended(1);
			int test4 = sceKernelIsCpuIntrSuspended(0);
			int test5 = sceKernelIsCpuIntrSuspended(2);
			sprintf(text, "flag1 %d, state1 %d, flag2 %d, state2 %d, flag3 %d, flag4 %d, flag5 %d, flag6 %d, flag7 %d\n", flag1, state1, flag2, state2, flag3, flag4, flag5, flag6, flag7);
			printf(text);
			sprintf(text, "test1 %d, test2 %d, test3 %d, test4 %d, test5 %d\n", test1, test2, test3, test4, test5);
			printf(text);
        }

        if (buttonDown & PSP_CTRL_RIGHT)
        {
        }

        if (buttonDown & PSP_CTRL_UP)
        {
        }

        if (buttonDown & PSP_CTRL_DOWN)
        {
        }


        if (buttonDown & PSP_CTRL_TRIANGLE)
        {
        	done = 1;
        }

        oldButtons = pad.Buttons;
        sceDisplayWaitVblank();
    }

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

