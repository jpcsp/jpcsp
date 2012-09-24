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
#include <stdlib.h>
#include <assert.h>
#include <errno.h>
#include "libfont.h"

PSP_MODULE_INFO("Font Test", 0, 1, 0);
PSP_MAIN_THREAD_ATTR(THREAD_ATTR_USER | PSP_THREAD_ATTR_VFPU);

int done = 0;
FontLibraryHandle libHandle;
FontHandle fontHandle;


void *fontAlloc(void *data, u32 size)
{
	pspDebugScreenPrintf("fontAlloc(0x%08X, %u)\n", (uint) data, (uint) size);
	return malloc(size);
}


void fontFree(void *data, void *p)
{
	pspDebugScreenPrintf("fontFree(0x%08X, 0x%08X)\n", (uint) data, (uint) p);
	free(p);
}


void runTest()
{
	FontNewLibParams params = {
		NULL, 4, NULL, fontAlloc, fontFree, NULL, NULL, NULL, NULL, NULL, NULL
	};
	uint errorCode;
	int result;

	pspDebugScreenPrintf("Starting Font Test\n");

	libHandle = sceFontNewLib(&params, &errorCode);
	pspDebugScreenPrintf("libHandle = 0x%08X\n", libHandle);

	fontHandle = sceFontOpen(libHandle, 0, 0777, &errorCode);
	pspDebugScreenPrintf("fontHandle = 0x%08X\n", fontHandle);

	FontInfo fontInfo;
	result = sceFontGetFontInfo(fontHandle, &fontInfo);
	pspDebugScreenPrintf("sceFontGetFontInfo returns 0x%08X\n", result);
	pspDebugScreenPrintf("   maxGlyphWidthI    =%d\n", fontInfo.maxGlyphWidthI);
	pspDebugScreenPrintf("   maxGlyphHeightI   =%d\n", fontInfo.maxGlyphHeightI);
	pspDebugScreenPrintf("   maxGlyphAscenderI =%d\n", fontInfo.maxGlyphAscenderI);
	pspDebugScreenPrintf("   maxGlyphDescenderI=%d\n", fontInfo.maxGlyphDescenderI);
	pspDebugScreenPrintf("   maxGlyphLeftXI    =%d\n", fontInfo.maxGlyphLeftXI);
	pspDebugScreenPrintf("   maxGlyphBaseYI    =%d\n", fontInfo.maxGlyphBaseYI);
	pspDebugScreenPrintf("   minGlyphCenterXI  =%d\n", fontInfo.minGlyphCenterXI);
	pspDebugScreenPrintf("   maxGlyphTopYI     =%d\n", fontInfo.maxGlyphTopYI);
	pspDebugScreenPrintf("   maxGlyphAdvanceXI =%d\n", fontInfo.maxGlyphAdvanceXI);
	pspDebugScreenPrintf("   maxGlyphDescenderI=%d\n", fontInfo.maxGlyphAdvanceYI);
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

	repeatStart.tv_sec = 0;
	repeatStart.tv_usec = 0;
	repeatDelay.tv_sec = 0;
	repeatDelay.tv_usec = 0;

	pspDebugScreenInit();
	pspDebugScreenPrintf("Press Cross to start the Font Test\n");

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
		}

		if (buttonDown & PSP_CTRL_TRIANGLE)
		{
			done = 1;
		}

		oldButtons = pad.Buttons;
	}

	sceGuTerm();

	return 0;
}

extern int module_start(SceSize _argc, char *_argp)
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

