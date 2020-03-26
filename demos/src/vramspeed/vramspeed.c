/*
 This file is part of jpcsp.

 Jpcsp is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Jpcsp is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Jpcsp.  If not, see <http://www.gnu.org/licenses/>.
 */
#include <pspkernel.h>
#include <pspdisplay.h>
#include <pspdebug.h>
#include <stdlib.h>
#include <stdio.h>
#include <math.h>
#include <string.h>
#include <pspctrl.h>

#include <pspgu.h>
#include <pspgum.h>

#include "../common/callbacks.h"
#include "../common/vram.h"

/*
 * Test results on a PSP-1000:
 * - texture 512x512: RAM   15 FPS, VRAM  90 FPS
 * - texture 256x512: RAM   28 FPS, VRAM 156 FPS
 * - texture 128x512: RAM  319 FPS, VRAM 613 FPS
 * - texture  64x512: RAM  509 FPS, VRAM 719 FPS
 *
 * - texture 512x256: RAM   16 FPS, VRAM  96 FPS
 * - texture 256x256: RAM   29 FPS, VRAM 164 FPS
 * - texture 128x256: RAM  333 FPS, VRAM 624 FPS
 * - texture  64x256: RAM  527 FPS, VRAM 729 FPS
 *
 * - texture 512x128: RAM   31 FPS, VRAM 172 FPS
 * - texture 256x128: RAM   58 FPS, VRAM 276 FPS
 * - texture 128x128: RAM  526 FPS, VRAM 727 FPS
 * - texture  64x128: RAM  741 FPS, VRAM 796 FPS
 *
 * - texture 512x 64: RAM   61 FPS, VRAM 289 FPS
 * - texture 256x 64: RAM  110 FPS, VRAM 420 FPS
 * - texture 128x 64: RAM  740 FPS, VRAM 798 FPS
 * - texture  64x 64: RAM  839 FPS, VRAM 931 FPS
 *
 * - texture 512x 32: RAM  117 FPS, VRAM 434 FPS
 * - texture 256x 32: RAM  202 FPS, VRAM 569 FPS
 * - texture 128x 32: RAM  929 FPS, VRAM 836 FPS
 * - texture  64x 32: RAM 1067 FPS, VRAM 861 FPS
 */

PSP_MODULE_INFO("VRAM Speed test", 0, 1, 1);
PSP_MAIN_THREAD_ATTR(THREAD_ATTR_USER);

#define BUF_WIDTH (512)
#define SCR_WIDTH (480)
#define SCR_HEIGHT (272)

static unsigned int __attribute__((aligned(16))) list[2048];
static unsigned int __attribute__((aligned(16))) ramTextureBuffer[512*512];
struct Vertex
{
        u16 u, v;
        s16 x, y, z;
};
struct Vertex __attribute__((aligned(16))) debugPrintVertices[2];
int val;
float sumFps;
float countFps;
int debugTextureWidth = 512;
int debugTextureHeight = 512;
int waitVblank = 0;
void *textureBuffer;

void drawDebugPrintBuffer()
{
        sceKernelDcacheWritebackAll();

        sceGuDisable(GU_ALPHA_TEST);
        sceGuDisable(GU_DEPTH_TEST);
        sceGuDisable(GU_SCISSOR_TEST);
        sceGuDisable(GU_STENCIL_TEST);
        sceGuDisable(GU_BLEND);
        sceGuDisable(GU_CULL_FACE);
        sceGuDisable(GU_DITHER);
        sceGuDisable(GU_FOG);
        sceGuDisable(GU_CLIP_PLANES);
        sceGuDisable(GU_LIGHTING);
        sceGuDisable(GU_LINE_SMOOTH);
        sceGuDisable(GU_PATCH_CULL_FACE);
        sceGuDisable(GU_COLOR_TEST);
        sceGuDisable(GU_COLOR_LOGIC_OP);
        sceGuDisable(GU_FACE_NORMAL_REVERSE);
        sceGuDisable(GU_PATCH_FACE);
        sceGuDisable(GU_FRAGMENT_2X);

        sceGuEnable(GU_TEXTURE_2D);
        sceGuTexFunc(GU_TFX_REPLACE, GU_TCC_RGBA);
        sceGuEnable(GU_ALPHA_TEST);
        sceGuAlphaFunc(GU_GREATER, 0x00, 0xFF);
        sceGuDepthMask(0);
        sceGuPixelMask(0x00000000);
        sceGuTexMode(GU_PSM_8888, 0, 0, 0);
        sceGuTexImage(0, debugTextureWidth, debugTextureHeight, BUF_WIDTH, textureBuffer);
        debugPrintVertices[0].u = 0;
        debugPrintVertices[0].v = 0;
        debugPrintVertices[0].x = 0;
        debugPrintVertices[0].y = 0;
        debugPrintVertices[0].z = 0;
        debugPrintVertices[1].u = debugTextureWidth;
        debugPrintVertices[1].v = debugTextureHeight;
        debugPrintVertices[1].x = debugTextureWidth;
        debugPrintVertices[1].y = debugTextureHeight;
        debugPrintVertices[1].z = 0;
        sceGuDrawArray(GU_SPRITES, GU_TEXTURE_16BIT | GU_VERTEX_16BIT | GU_TRANSFORM_2D, 2, NULL, debugPrintVertices);
}

void render()
{
		sceGuStart(GU_DIRECT,list);

		// clear screen (do not clear Z-Buffer)

		sceGuClearColor(0xff554433);
		sceGuClearDepth(0);
		sceGuClear(GU_COLOR_BUFFER_BIT);

		pspDebugScreenSetXY(0, 0);
		if (countFps > 0.f)
		{
			pspDebugScreenPrintf("FPS %03.1f  ", sumFps / countFps);
		}

		pspDebugScreenSetXY(0, 1);
		pspDebugScreenPrintf("%d x %d  ", debugTextureWidth, debugTextureHeight);

		pspDebugScreenSetXY(0, 2);
		if (textureBuffer == ramTextureBuffer)
		{
			pspDebugScreenPrintf("RAM texture ");
		}
		else
		{
			pspDebugScreenPrintf("VRAM texture");
		}

		pspDebugScreenSetXY(0, 3);
		if (waitVblank)
		{
			pspDebugScreenPrintf("Wait VBlank");
		}
		else
		{
			pspDebugScreenPrintf("           ");
		}

		drawDebugPrintBuffer();

		sceGuFinish();
		sceGuSync(0,0);

		if (waitVblank)
		{
			sceDisplayWaitVblankStart();
		}

		sceGuSwapBuffers();
}

void initTexture()
{
	int i;

	memset(textureBuffer, 0, 512*512*4);

	// Display 'X's on the top, on the left side and across
	// so that the texture resizing can easily be observed.
	pspDebugScreenSetBase(textureBuffer);
	pspDebugScreenSetXY(0, 0);
	pspDebugScreenPrintf("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
	for (i = 0; i < 34; i++)
	{
		int x1 = i * 2 * 7;
		int x2 = x1 + 7;
		int y1 = i * 8;
		int y2 = y1 + 4;

		// 'XX' on the top
		pspDebugScreenPutChar(x1,  0, -1, 'X');
		pspDebugScreenPutChar(x2,  0, -1, 'X');
		// 'X' on the left side
		pspDebugScreenPutChar( 0, y1, -1, 'X');
		// 'X' across
		pspDebugScreenPutChar(x1, y1, -1, 'X');
		pspDebugScreenPutChar(x2, y2, -1, 'X');
	}
}

int main(int argc, char* argv[])
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

	setupCallbacks();

	// setup GU

	void* fbp0 = getStaticVramBuffer(BUF_WIDTH,SCR_HEIGHT,GU_PSM_8888);
	void* fbp1 = getStaticVramBuffer(BUF_WIDTH,SCR_HEIGHT,GU_PSM_8888);
	void* zbp = getStaticVramBuffer(BUF_WIDTH,SCR_HEIGHT,GU_PSM_4444);
	// We are not using the Z-buffer so that we can use it for a large texture in VRAM
	void* vramTextureBuffer = zbp + 0x44000000;

	sceGuInit();

	sceGuStart(GU_DIRECT,list);
	sceGuDrawBuffer(GU_PSM_8888,fbp0,BUF_WIDTH);
	sceGuDispBuffer(SCR_WIDTH,SCR_HEIGHT,fbp1,BUF_WIDTH);
	sceGuDepthBuffer(zbp,BUF_WIDTH);
	sceGuOffset(2048 - (SCR_WIDTH/2),2048 - (SCR_HEIGHT/2));
	sceGuViewport(2048,2048,SCR_WIDTH,SCR_HEIGHT);
	sceGuDepthRange(65535,0);
	sceGuScissor(0,0,SCR_WIDTH,SCR_HEIGHT);
	sceGuEnable(GU_SCISSOR_TEST);
	sceGuDepthFunc(GU_GEQUAL);
	sceGuDisable(GU_DEPTH_TEST);
	sceGuFrontFace(GU_CW);
	sceGuShadeModel(GU_SMOOTH);
	sceGuEnable(GU_CULL_FACE);
	sceGuEnable(GU_CLIP_PLANES);
	sceGuDisable(GU_TEXTURE_2D);
	sceGuFinish();
	sceGuSync(0,0);

	sceDisplayWaitVblankStart();
	sceGuDisplay(GU_TRUE);

	// Prepare texture
	pspDebugScreenInit();
	textureBuffer = vramTextureBuffer;
	initTexture();
	textureBuffer = ramTextureBuffer;
	initTexture();

	textureBuffer = ramTextureBuffer;
	pspDebugScreenSetBase(textureBuffer);

	// run sample

	val = 0;
	u32 start = sceKernelGetSystemTimeLow();
	u32 end = sceKernelGetSystemTimeLow();
	sumFps = 0.f;
	countFps = 0.f;

	while(running())
	{
		int count = sceCtrlPeekBufferPositive(&pad, 1);
		if (count > 0)
		{
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

			// Switch between RAM & VRAM texture
			if (buttonDown & PSP_CTRL_CROSS)
			{
				if (textureBuffer == ramTextureBuffer)
				{
					textureBuffer = vramTextureBuffer;
				}
				else
				{
					textureBuffer = ramTextureBuffer;
				}
				pspDebugScreenSetBase(textureBuffer);
				sumFps = 0.f;
				countFps = 0.f;
			}

			// Reset the FPS counter
			if (buttonDown & PSP_CTRL_CIRCLE)
			{
				sumFps = 0.f;
				countFps = 0.f;
			}

			// Toggle the VBlank sync
			if (buttonDown & PSP_CTRL_SQUARE)
			{
				waitVblank = !waitVblank;
				sumFps = 0.f;
				countFps = 0.f;
			}

			// Resize the texture width/height in steps 32, 64, 128, 256, 512
			if (buttonDown & PSP_CTRL_LEFT)
			{
				if (debugTextureWidth > 32)
				{
					debugTextureWidth /= 2;
					sumFps = 0.f;
					countFps = 0.f;
				}
			}
			if (buttonDown & PSP_CTRL_RIGHT)
			{
				if (debugTextureWidth < 512)
				{
					debugTextureWidth *= 2;
					sumFps = 0.f;
					countFps = 0.f;
				}
			}
			if (buttonDown & PSP_CTRL_UP)
			{
				if (debugTextureHeight > 32)
				{
					debugTextureHeight /= 2;
					sumFps = 0.f;
					countFps = 0.f;
				}
			}
			if (buttonDown & PSP_CTRL_DOWN)
			{
				if (debugTextureHeight < 512)
				{
					debugTextureHeight *= 2;
					sumFps = 0.f;
					countFps = 0.f;
				}
			}
		}

		start = end;

		render();

		end = sceKernelGetSystemTimeLow();
		u32 duration = end - start;
		float fps = 1000000.f / duration;
		sumFps += fps;
		countFps += 1.f;

		val++;

		oldButtons = pad.Buttons;
	}

	sceGuTerm();

	sceKernelExitGame();
	return 0;
}
