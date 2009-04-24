/*
 * PSP Software Development Kit - http://www.pspdev.org
 * -----------------------------------------------------------------------
 * Licensed under the BSD license, see LICENSE in PSPSDK root for details.
 *
 * Copyright (c) 2005 Jesper Svennevid
 */

#include <pspkernel.h>
#include <pspdisplay.h>
#include <pspdebug.h>
#include <stdlib.h>
#include <stdio.h>
#include <math.h>
#include <string.h>

#include <pspgu.h>
#include <pspgum.h>

#include "../common/callbacks.h"
#include "../common/vram.h"

PSP_MODULE_INFO("Cube Sample", 0, 1, 1);
PSP_MAIN_THREAD_ATTR(THREAD_ATTR_USER);

static unsigned int __attribute__((aligned(16))) list[262144];
extern unsigned char logo_start[];

/* Test of Texture VertexInfo:
 *    #define VERTEX_16BIT	1		: Test GU_TEXTURE_16BIT, GU_VERTEX_16BIT
 *    #define VERTEX_16BIT	0		: Test GU_TEXTURE_8BIT, GU_VERTEX_8BIT
 */
#define VERTEX_16BIT	1

struct Vertex
{
#if VERTEX_16BIT
	unsigned short u, v;
#else
	unsigned char u, v;
#endif
	unsigned int color;
#if VERTEX_16BIT
	unsigned short x, y, z;
#else
	unsigned char x, y, z;
#endif
};

#define TEXTURE_UV_TRANSLATE	-10.0
#define TEXTURE_UV_SCALE		20.0
#if VERTEX_16BIT
#  define TEXTURE_UV(N)	((unsigned short)((((N)-TEXTURE_UV_TRANSLATE)/TEXTURE_UV_SCALE)*0x8000))
#  define POSITION(N)	((unsigned short)((N)*0x7fff))
#else
#  define TEXTURE_UV(N)	((unsigned char)((((N)-TEXTURE_UV_TRANSLATE)/TEXTURE_UV_SCALE)*0x80))
#  define POSITION(N)	((unsigned char)((N)*0x7f))
#endif
#define TEXTURE_UV_0	TEXTURE_UV(0)
#define TEXTURE_UV_1	TEXTURE_UV(1)
#define POSITION_1		POSITION(1)
#define POSITION_M1		POSITION(-1)

struct Vertex __attribute__((aligned(16))) vertices[12*3] =
{
	{TEXTURE_UV_0, TEXTURE_UV_0, 0xff7f0000,POSITION_M1,POSITION_M1,POSITION_1 }, // 0
	{TEXTURE_UV_1, TEXTURE_UV_0, 0xff7f0000,POSITION_M1,POSITION_1 ,POSITION_1 }, // 4
	{TEXTURE_UV_1, TEXTURE_UV_1, 0xff7f0000,POSITION_1 ,POSITION_1 ,POSITION_1 }, // 5

	{TEXTURE_UV_0, TEXTURE_UV_0, 0xff7f0000,POSITION_M1,POSITION_M1,POSITION_1 }, // 0
	{TEXTURE_UV_1, TEXTURE_UV_1, 0xff7f0000,POSITION_1 ,POSITION_1 ,POSITION_1 }, // 5
	{TEXTURE_UV_0, TEXTURE_UV_1, 0xff7f0000,POSITION_1 ,POSITION_M1,POSITION_1 }, // 1

	{TEXTURE_UV_0, TEXTURE_UV_0, 0xff7f0000,POSITION_M1,POSITION_M1,POSITION_M1}, // 3
	{TEXTURE_UV_1, TEXTURE_UV_0, 0xff7f0000,POSITION_1 ,POSITION_M1,POSITION_M1}, // 2
	{TEXTURE_UV_1, TEXTURE_UV_1, 0xff7f0000,POSITION_1 ,POSITION_1 ,POSITION_M1}, // 6

	{TEXTURE_UV_0, TEXTURE_UV_0, 0xff7f0000,POSITION_M1,POSITION_M1,POSITION_M1}, // 3
	{TEXTURE_UV_1, TEXTURE_UV_1, 0xff7f0000,POSITION_1 ,POSITION_1 ,POSITION_M1}, // 6
	{TEXTURE_UV_0, TEXTURE_UV_1, 0xff7f0000,POSITION_M1,POSITION_1 ,POSITION_M1}, // 7

	{TEXTURE_UV_0, TEXTURE_UV_0, 0xff007f00,POSITION_1 ,POSITION_M1,POSITION_M1}, // 0
	{TEXTURE_UV_1, TEXTURE_UV_0, 0xff007f00,POSITION_1 ,POSITION_M1,POSITION_1 }, // 3
	{TEXTURE_UV_1, TEXTURE_UV_1, 0xff007f00,POSITION_1 ,POSITION_1 ,POSITION_1 }, // 7

	{TEXTURE_UV_0, TEXTURE_UV_0, 0xff007f00,POSITION_1 ,POSITION_M1,POSITION_M1}, // 0
	{TEXTURE_UV_1, TEXTURE_UV_1, 0xff007f00,POSITION_1 ,POSITION_1 ,POSITION_1 }, // 7
	{TEXTURE_UV_0, TEXTURE_UV_1, 0xff007f00,POSITION_1 ,POSITION_1 ,POSITION_M1}, // 4

	{TEXTURE_UV_0, TEXTURE_UV_0, 0xff007f00,POSITION_M1,POSITION_M1,POSITION_M1}, // 0
	{TEXTURE_UV_1, TEXTURE_UV_0, 0xff007f00,POSITION_M1,POSITION_1 ,POSITION_M1}, // 3
	{TEXTURE_UV_1, TEXTURE_UV_1, 0xff007f00,POSITION_M1,POSITION_1 ,POSITION_1 }, // 7

	{TEXTURE_UV_0, TEXTURE_UV_0, 0xff007f00,POSITION_M1,POSITION_M1,POSITION_M1}, // 0
	{TEXTURE_UV_1, TEXTURE_UV_1, 0xff007f00,POSITION_M1,POSITION_1 ,POSITION_1 }, // 7
	{TEXTURE_UV_0, TEXTURE_UV_1, 0xff007f00,POSITION_M1,POSITION_M1,POSITION_1 }, // 4

	{TEXTURE_UV_0, TEXTURE_UV_0, 0xff00007f,POSITION_M1,POSITION_1 ,POSITION_M1}, // 0
	{TEXTURE_UV_1, TEXTURE_UV_0, 0xff00007f,POSITION_1 ,POSITION_1 ,POSITION_M1}, // 1
	{TEXTURE_UV_1, TEXTURE_UV_1, 0xff00007f,POSITION_1 ,POSITION_1 ,POSITION_1 }, // 2

	{TEXTURE_UV_0, TEXTURE_UV_0, 0xff00007f,POSITION_M1,POSITION_1 ,POSITION_M1}, // 0
	{TEXTURE_UV_1, TEXTURE_UV_1, 0xff00007f,POSITION_1 ,POSITION_1 ,POSITION_1 }, // 2
	{TEXTURE_UV_0, TEXTURE_UV_1, 0xff00007f,POSITION_M1,POSITION_1 ,POSITION_1 }, // 3

	{TEXTURE_UV_0, TEXTURE_UV_0, 0xff00007f,POSITION_M1,POSITION_M1,POSITION_M1}, // 4
	{TEXTURE_UV_1, TEXTURE_UV_0, 0xff00007f,POSITION_M1,POSITION_M1,POSITION_1 }, // 7
	{TEXTURE_UV_1, TEXTURE_UV_1, 0xff00007f,POSITION_1 ,POSITION_M1,POSITION_1 }, // 6

	{TEXTURE_UV_0, TEXTURE_UV_0, 0xff00007f,POSITION_M1,POSITION_M1,POSITION_M1}, // 4
	{TEXTURE_UV_1, TEXTURE_UV_1, 0xff00007f,POSITION_1 ,POSITION_M1,POSITION_1 }, // 6
	{TEXTURE_UV_0, TEXTURE_UV_1, 0xff00007f,POSITION_1 ,POSITION_M1,POSITION_M1}, // 5
};

#define BUF_WIDTH (512)
#define SCR_WIDTH (480)
#define SCR_HEIGHT (272)

int main(int argc, char* argv[])
{
	setupCallbacks();

	// setup GU

	void* fbp0 = getStaticVramBuffer(BUF_WIDTH,SCR_HEIGHT,GU_PSM_8888);
	void* fbp1 = getStaticVramBuffer(BUF_WIDTH,SCR_HEIGHT,GU_PSM_8888);
	void* zbp = getStaticVramBuffer(BUF_WIDTH,SCR_HEIGHT,GU_PSM_4444);

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
	sceGuEnable(GU_DEPTH_TEST);
	sceGuFrontFace(GU_CW);
	sceGuShadeModel(GU_SMOOTH);
	sceGuEnable(GU_CULL_FACE);
	sceGuEnable(GU_TEXTURE_2D);
	sceGuEnable(GU_CLIP_PLANES);
	sceGuFinish();
	sceGuSync(0,0);

	sceDisplayWaitVblankStart();
	sceGuDisplay(GU_TRUE);

	// run sample

	int val = 0;

	while(running())
	{
		sceGuStart(GU_DIRECT,list);

		// clear screen

		sceGuClearColor(0xff554433);
		sceGuClearDepth(0);
		sceGuClear(GU_COLOR_BUFFER_BIT|GU_DEPTH_BUFFER_BIT);

		// setup matrices for cube

		sceGumMatrixMode(GU_PROJECTION);
		sceGumLoadIdentity();
		sceGumPerspective(75.0f,16.0f/9.0f,0.5f,1000.0f);

		sceGumMatrixMode(GU_VIEW);
		sceGumLoadIdentity();

		sceGumMatrixMode(GU_MODEL);
		sceGumLoadIdentity();
		{
			ScePspFVector3 pos = { 0, 0, -2.5f };
			ScePspFVector3 rot = { val * 0.79f * (GU_PI/180.0f), val * 0.98f * (GU_PI/180.0f), val * 1.32f * (GU_PI/180.0f) };
			sceGumTranslate(&pos);
			sceGumRotateXYZ(&rot);
		}

		// setup texture

		sceGuTexMode(GU_PSM_4444,0,0,0);
		sceGuTexImage(0,64,64,64,logo_start);
		sceGuTexFunc(GU_TFX_ADD,GU_TCC_RGB);
		sceGuTexEnvColor(0xffff00);
		sceGuTexFilter(GU_LINEAR,GU_LINEAR);
		sceGuTexScale(TEXTURE_UV_SCALE,TEXTURE_UV_SCALE);
		sceGuTexOffset(TEXTURE_UV_TRANSLATE,TEXTURE_UV_TRANSLATE);
		sceGuAmbientColor(0xffffffff);

		// draw cube

		int flags = GU_COLOR_8888|GU_TRANSFORM_3D;
		#if VERTEX_16BIT
			flags |= GU_TEXTURE_16BIT|GU_VERTEX_16BIT;
		#else
			flags |= GU_TEXTURE_8BIT|GU_VERTEX_8BIT;
		#endif
		sceGumDrawArray(GU_TRIANGLES,flags,12*3,0,vertices);

		sceGuFinish();
		sceGuSync(0,0);

		sceDisplayWaitVblankStart();
		sceGuSwapBuffers();

		val++;
	}

	sceGuTerm();

	sceKernelExitGame();
	return 0;
}
