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
int totalAlloc = 0;


void *fontAlloc(void *data, u32 size)
{
	totalAlloc += size;
	pspDebugScreenPrintf("fontAlloc(0x%08X, %u) - total %d\n", (uint) data, (uint) size, totalAlloc);

	// Alloc 4 bytes more to remember to memory size so that we can keep track to totalAlloc in "fontFree" callback.
	u32 *mem = malloc(size + 4);
	mem[0] = size;

	return mem + 1;
}


void fontFree(void *data, void *p)
{
	u32 *mem = ((u32 *) p) - 1;
	u32 size = mem[0];

	totalAlloc -= size;
	pspDebugScreenPrintf("fontFree(0x%08X, 0x%08X, size=%u) - total %d\n", (uint) data, (uint) p, size, totalAlloc);

	free(mem);
}


void printFontStyle(FontStyle fontStyle)
{
	pspDebugScreenPrintf("   H/V                       : %.3f / %.3f\n", fontStyle.fontH, fontStyle.fontV);
	pspDebugScreenPrintf("   H/VRes                    : %.1f / %.1f\n", fontStyle.fontHRes, fontStyle.fontVRes);
	pspDebugScreenPrintf("   Weight                    : %f\n", fontStyle.fontWeight);
	pspDebugScreenPrintf("   Family / Style / StyleSub : %d / %d / %d\n", fontStyle.fontFamily, fontStyle.fontStyle, fontStyle.fontStyleSub);
	pspDebugScreenPrintf("   Language/ Region / Country: %d / %d / %d\n", fontStyle.fontLanguage, fontStyle.fontRegion, fontStyle.fontCountry);
	pspDebugScreenPrintf("   Name / FileName           : '%s'(%s)\n", fontStyle.fontName, fontStyle.fontFileName);
}

void sceFontNewLibTest()
{
	FontNewLibParams params = {
		NULL, 4, NULL, fontAlloc, fontFree, NULL, NULL, NULL, NULL, NULL, NULL
	};
	uint errorCode = -1;
	libHandle = sceFontNewLib(&params, &errorCode);
	pspDebugScreenPrintf("libHandle = 0x%08X, errorCode=0x%08X\n", libHandle, errorCode);
}

void sceFontOpenTest()
{
	uint errorCode = -1;
	fontHandle = sceFontOpen(libHandle, 0, 0777, &errorCode);
	pspDebugScreenPrintf("fontHandle = 0x%08X, errorCode=0x%08X\n", fontHandle, errorCode);
}

void sceFontGetFontInfoTest()
{
	FontInfo fontInfo;
	int result = sceFontGetFontInfo(fontHandle, &fontInfo);
	pspDebugScreenPrintf("sceFontGetFontInfo returns 0x%08X\n", result);
	pspDebugScreenPrintf("   maxGlyphWidth       : %d(%f)\n", fontInfo.maxGlyphWidthI, fontInfo.maxGlyphWidthF);
	pspDebugScreenPrintf("   maxGlyphHeight      : %d(%f)\n", fontInfo.maxGlyphHeightI, fontInfo.maxGlyphHeightF);
	pspDebugScreenPrintf("   maxGlyphAscender    : %d(%f)\n", fontInfo.maxGlyphAscenderI, fontInfo.maxGlyphAscenderF);
	pspDebugScreenPrintf("   maxGlyphDescender   : %d(%f)\n", fontInfo.maxGlyphDescenderI, fontInfo.maxGlyphDescenderF);
	pspDebugScreenPrintf("   maxGlyphLeftX       : %d(%f)\n", fontInfo.maxGlyphLeftXI, fontInfo.maxGlyphLeftXF);
	pspDebugScreenPrintf("   maxGlyphBaseY       : %d(%f)\n", fontInfo.maxGlyphBaseYI, fontInfo.maxGlyphBaseYF);
	pspDebugScreenPrintf("   minGlyphCenterX     : %d(%f)\n", fontInfo.minGlyphCenterXI, fontInfo.minGlyphCenterXF);
	pspDebugScreenPrintf("   maxGlyphTopY        : %d(%f)\n", fontInfo.maxGlyphTopYI, fontInfo.maxGlyphTopYF);
	pspDebugScreenPrintf("   maxGlyphAdvanceX    : %d(%f)\n", fontInfo.maxGlyphAdvanceXI, fontInfo.maxGlyphAdvanceXF);
	pspDebugScreenPrintf("   maxGlyphAdvanceY    : %d(%f)\n", fontInfo.maxGlyphAdvanceYI, fontInfo.maxGlyphAdvanceYF);
	pspDebugScreenPrintf("   maxGlyphWidth/Height: %d / %d\n", fontInfo.maxGlyphWidth, fontInfo.maxGlyphHeight);
	pspDebugScreenPrintf("   char/shadowMapLength: %d / %d\n", fontInfo.charMapLength, fontInfo.shadowMapLength);
	printFontStyle(fontInfo.fontStyle);
	pspDebugScreenPrintf("   BPP               : %d\n", fontInfo.BPP);
}

void sceFontGetCharInfoTest(int charCode)
{
	CharInfo charInfo;
	int result = sceFontGetCharInfo(fontHandle, charCode, &charInfo);
	pspDebugScreenPrintf("sceFontGetCharInfo returns 0x%08X\n", result);
	pspDebugScreenPrintf("   bitmap %dx%d at (%d,%d)\n", charInfo.bitmapWidth, charInfo.bitmapHeight, charInfo.bitmapLeft, charInfo.bitmapTop);
	pspDebugScreenPrintf("   spf26Width/Height      : %d / %d\n", charInfo.spf26Width, charInfo.spf26Height);
	pspDebugScreenPrintf("   spf26Ascender/Descender: %d / %d\n", charInfo.spf26Ascender, charInfo.spf26Ascender);
	pspDebugScreenPrintf("   spf26BearingHX/Y       : %d / %d\n", charInfo.spf26BearingHX, charInfo.spf26BearingHY);
	pspDebugScreenPrintf("   spf26BearingVX/Y       : %d / %d\n", charInfo.spf26BearingVX, charInfo.spf26BearingVY);
	pspDebugScreenPrintf("   spf26AdvanceH/V        : %d / %d\n", charInfo.spf26AdvanceH, charInfo.spf26AdvanceV);
}

void printBuffer(u8 *buffer, int bufWidth)
{
	char line[100];
	int x, y, i;
	char *pixels = ".123456789ABCDEF";

	for (y = 0; y < 20; y++) {
		for (x = 0, i = 0; x < 20; x++, i++) {
			int pixelColor = buffer[y * bufWidth + x] >> 4;
			line[i] = pixels[pixelColor];
		}
		line[i] = '\0';
		pspDebugScreenPrintf(">%s<\n", line);
	}
}

void sceFontGetCharGlyphImageTest(int charCode)
{
	GlyphImage glyphImage;
	int bufWidth = 64;
	int bufHeight = 64;
	u8 *buffer = malloc(bufWidth * bufHeight);

	glyphImage.pixelFormat = PSP_FONT_PIXELFORMAT_8;
	glyphImage.xPos64 = 2 << 6;
	glyphImage.yPos64 = 2 << 6;
	glyphImage.bufWidth = bufWidth;
	glyphImage.bufHeight = bufHeight;
	glyphImage.bytesPerLine = bufWidth;
	glyphImage.pad = 0;
	glyphImage.buffer = buffer;

	memset(buffer, 0, bufWidth * bufHeight);
	int result = sceFontGetCharGlyphImage(fontHandle, charCode, &glyphImage);
	pspDebugScreenPrintf("sceFontGetCharGlyphImage returns 0x%08X\n", result);
	printBuffer(buffer, bufWidth);

	free(buffer);
}

void sceFontGetCharGlyphImage_ClipTest(int charCode)
{
	GlyphImage glyphImage;
	int bufWidth = 64;
	int bufHeight = 64;
	u8 *buffer = malloc(bufWidth * bufHeight);

	glyphImage.pixelFormat = PSP_FONT_PIXELFORMAT_8;
	glyphImage.xPos64 = 2 << 6;
	glyphImage.yPos64 = 2 << 6;
	glyphImage.bufWidth = bufWidth;
	glyphImage.bufHeight = bufHeight;
	glyphImage.bytesPerLine = bufWidth;
	glyphImage.pad = 0;
	glyphImage.buffer = buffer;

	memset(buffer, 0, bufWidth * bufHeight);
	int result = sceFontGetCharGlyphImage_Clip(fontHandle, charCode, &glyphImage, 3, 3, 11, 7);
	pspDebugScreenPrintf("sceFontGetCharGlyphImage_Clip returns 0x%08X\n", result);
	printBuffer(buffer, bufWidth);

	free(buffer);
}


void printInstructions()
{
	pspDebugScreenPrintf("Press Cross to test sceFontNewLib\n");
	pspDebugScreenPrintf("Press Square to test sceFontOpen\n");
	pspDebugScreenPrintf("Press Circle to test sceFontGetFontInfo\n");
	pspDebugScreenPrintf("Press Left to test sceFontGetCharInfo('R')\n");
	pspDebugScreenPrintf("Press Right to test sceFontGetCharGlyphImage('R')\n");
	pspDebugScreenPrintf("Press Down to test sceFontGetCharGlyphImage_Clip('R')\n");
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
	printInstructions();

	while (!done)
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
			sceFontNewLibTest();
		}

		if (buttonDown & PSP_CTRL_SQUARE)
		{
			sceFontOpenTest();
		}

		if (buttonDown & PSP_CTRL_CIRCLE)
		{
			pspDebugScreenClear();
			printInstructions();
			sceFontGetFontInfoTest();
		}

		if (buttonDown & PSP_CTRL_LEFT)
		{
			pspDebugScreenClear();
			printInstructions();
			sceFontGetCharInfoTest('R');
		}

		if (buttonDown & PSP_CTRL_RIGHT)
		{
			pspDebugScreenClear();
			printInstructions();
			sceFontGetCharGlyphImageTest('R');
		}

		if (buttonDown & PSP_CTRL_DOWN)
		{
			pspDebugScreenClear();
			printInstructions();
			sceFontGetCharGlyphImage_ClipTest('R');
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

