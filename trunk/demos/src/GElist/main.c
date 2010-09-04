#include <pspkernel.h>
#include <pspkernel.h>
#include <pspkernel.h>
#include <pspdebug.h>
#include <pspctrl.h>
#include <pspdisplay.h>
#include <pspgu.h>
#include <pspgum.h>
#include <pspge.h>
#include <pspiofilemgr.h>

#include <sys/stat.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>
#include <malloc.h>

PSP_MODULE_INFO("GE List Test", 0, 1, 0);
PSP_MAIN_THREAD_ATTR(THREAD_ATTR_USER);

#define GU_BEHAVIOR_BREAK	3

#define BUF_WIDTH	512
#define SCR_WIDTH	480
#define SCR_HEIGHT	272
#define FONT_HEIGHT	8

void sendCommandi(int cmd, int argument);
void sendCommandf(int cmd, float argument);
void sceGeContinue();
extern int ge_list_executed[];
typedef struct
{
        unsigned int* start;
        unsigned int* current;
        int parent_context;
} GuDisplayList;
extern GuDisplayList* gu_list;

static unsigned int __attribute__((aligned(16))) list[262144];

static unsigned int staticOffset = 0;
void* fbp0;
void* fbp1;
void* zbp;
int done = 0;

static unsigned int getMemorySize(unsigned int width, unsigned int height, unsigned int psm)
{
	switch (psm)
	{
		case GU_PSM_T4:
			return (width * height) >> 1;

		case GU_PSM_T8:
			return width * height;

		case GU_PSM_5650:
		case GU_PSM_5551:
		case GU_PSM_4444:
		case GU_PSM_T16:
			return 2 * width * height;

		case GU_PSM_8888:
		case GU_PSM_T32:
			return 4 * width * height;

		default:
			return 0;
	}
}

void* getStaticVramBuffer(unsigned int width, unsigned int height, unsigned int psm)
{
	unsigned int memSize = getMemorySize(width,height,psm);
	void* result = (void*)staticOffset;
	staticOffset += memSize;

	return result;
}

unsigned int __attribute__((aligned(16))) clutTable[] = { 0xFFFFFFFF, 0x00000000, 0x00FF0000, 0x0000FF00, 0x000000FF };
unsigned char __attribute__((aligned(16))) imageData[] =
							{ 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01
							};

struct
{
	unsigned char u, v;
	unsigned short color;
	signed char nx, ny, nz;
	signed char px, py, pz;
} vertices8[6] = { { 0, 0, 0x000F, 0, 0, 0, -80, 80, -80 },
                   { 0, 200, 0x00FF, 1, 1, 1, -80, -80, -80 },
                   { 100, 0, 0x00FF, 1, 1, 1, 0, 80, -80 },
                   { 100, 200, 0x00FF, 1, 1, 1, 0, -80, -80 },
                   { 200, 0, 0x00FF, 1, 1, 1, 80, 80, -80 },
                   { 200, 200, 0x000F, 1, 1, 1, 80, -80, -80 } };

struct
{
	unsigned short u, v;
	unsigned short color;
	signed short nx, ny, nz;
	signed short px, py, pz;
} vertices16[6] = { { 0, 0, 0x000F, 0, 0, 0, -32000, 32000, -32000 },
                   { 0, 50000, 0x00FF, 1, 1, 1, -32000, -32000, -32000 },
                   { 25000, 0, 0x00FF, 1, 1, 1, 0, 32000, -32000 },
                   { 25000, 50000, 0x00FF, 1, 1, 1, 0, -32000, -32000 },
                   { 50000, 0, 0x00FF, 1, 1, 1, 32000, 32000, -32000 },
                   { 50000, 50000, 0x000F, 1, 1, 1, 32000, -32000, -32000 } };

struct
{
	float u, v;
	unsigned int color;
	float nx, ny, nz;
	float px, py, pz;
} verticesFloat[6] = { { 0, 0, 0x000000FF, 0, 0, 0, -1.2, 1.2, -1.2 },
                       { 0, 2, 0x0000FFFF, 0, 0, 0, -1.2, -1.2, -1.2 },
                       { 1, 0, 0x0000FFFF, 0, 0, 0, 0, 1.2, -1.2 },
                       { 1, 2, 0x0000FFFF, 0, 0, 0, 0, -1.2, -1.2 },
                       { 2, 0, 0x0000FFFF, 0, 0, 0, 1.2, 1.2, -1.2 },
                       { 2, 2, 0x000000FF, 0, 0, 0, 1.2, -1.2, -1.2 } };

ScePspFVector3 translation;

void drawTest()
{
	sceKernelDcacheWritebackAll();
	sceGuClear(GU_COLOR_BUFFER_BIT);
	sceGuEnable(GU_TEXTURE_2D);
	sceGuClutMode(GU_PSM_8888, 0, 0xFF, 0);
	sceGuTexMode(GU_PSM_T8, 0, 0, GU_FALSE);
	sceGuTexEnvColor(0xFF000000);
	sceGuTexFunc(GU_TFX_DECAL, GU_TCC_RGB);
	sceGuTexImage(0, 16, 16, 16, imageData);
	sceGuClutLoad(1, clutTable);

	sceGumMatrixMode(GU_PROJECTION);
	sceGumLoadIdentity();
	sceGumPerspective(75.0f,16.0f/9.0f,0.9f,1000.0f);

	sceGumMatrixMode(GU_MODEL);
	sceGumLoadIdentity();

	sceGumMatrixMode(GU_VIEW);
	sceGumLoadIdentity();
	translation.x = -2;
	translation.y = 1.5;
	translation.z = -4;
	sceGumTranslate(&translation);
	sceGumDrawArray(GU_TRIANGLE_STRIP, GU_TEXTURE_8BIT|GU_COLOR_4444|GU_NORMAL_8BIT|GU_VERTEX_8BIT|GU_TRANSFORM_3D, 6, NULL, vertices8);

	sceGumMatrixMode(GU_VIEW);
	sceGumLoadIdentity();
	translation.x = 0;
	translation.y = 1.5;
	translation.z = -4;
	sceGumTranslate(&translation);
	sceGumDrawArray(GU_TRIANGLE_STRIP, GU_TEXTURE_16BIT|GU_COLOR_4444|GU_NORMAL_16BIT|GU_VERTEX_16BIT|GU_TRANSFORM_3D, 6, NULL, vertices16);

	sceGumMatrixMode(GU_VIEW);
	sceGumLoadIdentity();
	translation.x = 3;
	translation.y = 1.5;
	translation.z = -4;
	sceGumTranslate(&translation);
	sceGumDrawArray(GU_TRIANGLE_STRIP, GU_TEXTURE_32BITF|GU_COLOR_8888|GU_NORMAL_32BITF|GU_VERTEX_32BITF|GU_TRANSFORM_3D, 6, NULL, verticesFloat);

	sceGuDisable(GU_TEXTURE_2D);

	sceGumMatrixMode(GU_VIEW);
	sceGumLoadIdentity();
	translation.x = -2;
	translation.y = -1.5;
	translation.z = -4;
	sceGumTranslate(&translation);
	sceGumDrawArray(GU_TRIANGLE_STRIP, GU_TEXTURE_8BIT|GU_COLOR_4444|GU_NORMAL_8BIT|GU_VERTEX_8BIT|GU_TRANSFORM_3D, 6, NULL, vertices8);

	sceGumMatrixMode(GU_VIEW);
	sceGumLoadIdentity();
	translation.x = 0;
	translation.y = -1.5;
	translation.z = -4;
	sceGumTranslate(&translation);
	sceGumDrawArray(GU_TRIANGLE_STRIP, GU_TEXTURE_16BIT|GU_COLOR_4444|GU_NORMAL_16BIT|GU_VERTEX_16BIT|GU_TRANSFORM_3D, 6, NULL, vertices16);

	sceGumMatrixMode(GU_VIEW);
	sceGumLoadIdentity();
	translation.x = 3;
	translation.y = -1.5;
	translation.z = -4;
	sceGumTranslate(&translation);
	sceGumDrawArray(GU_TRIANGLE_STRIP, GU_TEXTURE_32BITF|GU_COLOR_8888|GU_NORMAL_32BITF|GU_VERTEX_32BITF|GU_TRANSFORM_3D, 6, NULL, verticesFloat);
}


void draw()
{
	sceDisplaySetMode(0, SCR_WIDTH, SCR_HEIGHT);
	sceGuStart(GU_DIRECT, list);
	sceKernelDcacheWritebackAll();

	drawTest();
	sceGuFinish();
	sceGuSync(0, 0);

	sceDisplayWaitVblank();
	fbp0 = sceGuSwapBuffers();
}


void init()
{
	pspDebugScreenInit();

	fbp0 = getStaticVramBuffer(BUF_WIDTH, SCR_HEIGHT, GU_PSM_8888);
	fbp1 = getStaticVramBuffer(BUF_WIDTH, SCR_HEIGHT, GU_PSM_8888);
	zbp  = getStaticVramBuffer(BUF_WIDTH, SCR_HEIGHT, GU_PSM_4444);
 
	sceGuInit();
	sceGuStart(GU_DIRECT, list);
	sceGuDrawBuffer(GU_PSM_8888,fbp0,BUF_WIDTH);
	sceGuDispBuffer(SCR_WIDTH,SCR_HEIGHT,fbp1,BUF_WIDTH);
	sceGuDepthBuffer(zbp,BUF_WIDTH);
	sceGuOffset(2048 - (SCR_WIDTH/2),2048 - (SCR_HEIGHT/2));
	sceGuViewport(2048,2048,SCR_WIDTH,SCR_HEIGHT);
	sceGuDepthRange(65535,0);
	sceGuScissor(0,0,SCR_WIDTH,SCR_HEIGHT);
	sceGuEnable(GU_SCISSOR_TEST);
	sceGuFrontFace(GU_CW);
	sceGuShadeModel(GU_SMOOTH);
	sceGuDisable(GU_TEXTURE_2D);
	sceGuFinish();
	sceGuSync(0,0);
 
	sceDisplayWaitVblankStart();
	sceGuDisplay(1);

	sceCtrlSetSamplingCycle(0);
	sceCtrlSetSamplingMode(PSP_CTRL_MODE_ANALOG);
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

	init();

	while(!done)
	{
		draw();

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

		if (buttonDown & PSP_CTRL_LEFT)
		{
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

		if (buttonDown & PSP_CTRL_LTRIGGER)
		{
		}

		if (buttonDown & PSP_CTRL_RTRIGGER)
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

