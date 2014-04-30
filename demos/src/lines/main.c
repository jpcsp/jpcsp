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

PSP_MODULE_INFO("Lines Test", 0, 1, 0);
PSP_MAIN_THREAD_ATTR(THREAD_ATTR_USER);

#define BUF_WIDTH	512
#define SCR_WIDTH	480
#define SCR_HEIGHT	272

#define COLOR_RED	0xFF0000FF
#define COLOR_WHITE	0xFFFFFFFF

void sendCommandi(int cmd, int argument);
void sendCommandf(int cmd, float argument);

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


struct vertex
{
	unsigned int color;
	float px, py, pz;
};

struct vertex vertices[1000];
struct vertex *pVertex;

float min(float a, float b)
{
	return a <= b ? a : b;
}

float max(float a, float b)
{
	return a >= b ? a : b;
}

void drawLine(float x1, float y1, float x2, float y2, unsigned int color)
{
	pVertex->color = color;
	pVertex->px = x1;
	pVertex->py = y1;
	pVertex->pz = 0;
	pVertex++;
	pVertex->color = color;
	pVertex->px = x2;
	pVertex->py = y2;
	pVertex->pz = 0;
	pVertex++;
}

void drawTestLine(float x1, float y1, float x2, float y2)
{
	drawLine(x1, y1, x2, y2, COLOR_RED);

	float minx = min(x1, x2);
	float maxx = max(x1, x2);
	float miny = min(y1, y2);
	float maxy = max(y1, y2);
	float border = 2;
	drawLine(minx - border, miny - border, maxx + border, miny - border, COLOR_WHITE);
	drawLine(minx - border, maxy + border, maxx + border, maxy + border, COLOR_WHITE);
	drawLine(minx - border, miny - border, minx - border, maxy + border, COLOR_WHITE);
	drawLine(maxx + border, miny - border, maxx + border, maxy + border + 1, COLOR_WHITE);
}

void drawTest()
{
	float x, y;
	float xstep, ystep, xstart, ystart;

	xstart = 10;
	ystart = 10;
	xstep = 20;
	ystep = 10;
	x = xstart;
	y = ystart;

	drawTestLine(x, y, x    , y    ); y += ystep;
	drawTestLine(x, y, x + 1, y    ); y += ystep;
	drawTestLine(x, y, x + 2, y    ); y += ystep;
	drawTestLine(x, y, x + 3, y    ); y += ystep;
	drawTestLine(x, y, x    , y + 1); y += ystep;
	drawTestLine(x, y, x    , y + 2); y += ystep;
	drawTestLine(x, y, x    , y + 3); y += ystep;

	x += xstep; y = ystart;
	drawTestLine(x, y, x + 1, y + 1); y += ystep;
	drawTestLine(x, y, x + 1, y + 2); y += ystep;
	drawTestLine(x, y, x + 1, y + 3); y += ystep;
	drawTestLine(x, y, x + 1, y + 4); y += ystep;
	drawTestLine(x, y, x + 2, y + 1); y += ystep;
	drawTestLine(x, y, x + 2, y + 2); y += ystep;
	drawTestLine(x, y, x + 2, y + 3); y += ystep;
	drawTestLine(x, y, x + 2, y + 4); y += ystep;
	drawTestLine(x, y, x + 3, y + 1); y += ystep;
	drawTestLine(x, y, x + 3, y + 2); y += ystep;
	drawTestLine(x, y, x + 3, y + 3); y += ystep;
	drawTestLine(x, y, x + 3, y + 4); y += ystep;
	drawTestLine(x, y, x + 4, y + 1); y += ystep;
	drawTestLine(x, y, x + 4, y + 2); y += ystep;
	drawTestLine(x, y, x + 4, y + 3); y += ystep;
	drawTestLine(x, y, x + 4, y + 4); y += ystep;

	x += xstep; y = ystart;
	drawTestLine(x, y, x + 5, y + 1); y += ystep;
	drawTestLine(x, y, x + 5, y + 2); y += ystep;
	drawTestLine(x, y, x + 5, y + 3); y += ystep;
	drawTestLine(x, y, x + 5, y + 4); y += ystep;
	drawTestLine(x, y, x + 6, y + 1); y += ystep;
	drawTestLine(x, y, x + 6, y + 2); y += ystep;
	drawTestLine(x, y, x + 6, y + 3); y += ystep;
	drawTestLine(x, y, x + 6, y + 4); y += ystep;
	drawTestLine(x, y, x + 7, y + 1); y += ystep;
	drawTestLine(x, y, x + 7, y + 2); y += ystep;
	drawTestLine(x, y, x + 7, y + 3); y += ystep;
	drawTestLine(x, y, x + 7, y + 4); y += ystep;
	drawTestLine(x, y, x + 8, y + 1); y += ystep;
	drawTestLine(x, y, x + 8, y + 2); y += ystep;
	drawTestLine(x, y, x + 8, y + 3); y += ystep;
	drawTestLine(x, y, x + 8, y + 4); y += ystep;

	x += xstep; y = ystart;
	drawTestLine(x, y, x + 1, y + 5); y += 2 * ystep;
	drawTestLine(x, y, x + 1, y + 6); y += 2 * ystep;
	drawTestLine(x, y, x + 1, y + 7); y += 2 * ystep;
	drawTestLine(x, y, x + 1, y + 8); y += 2 * ystep;
	drawTestLine(x, y, x + 2, y + 5); y += 2 * ystep;
	drawTestLine(x, y, x + 2, y + 6); y += 2 * ystep;
	drawTestLine(x, y, x + 2, y + 7); y += 2 * ystep;
	drawTestLine(x, y, x + 2, y + 8); y += 2 * ystep;
	x += xstep; y = ystart;
	drawTestLine(x, y, x + 3, y + 5); y += 2 * ystep;
	drawTestLine(x, y, x + 3, y + 6); y += 2 * ystep;
	drawTestLine(x, y, x + 3, y + 7); y += 2 * ystep;
	drawTestLine(x, y, x + 3, y + 8); y += 2 * ystep;
	drawTestLine(x, y, x + 4, y + 5); y += 2 * ystep;
	drawTestLine(x, y, x + 4, y + 6); y += 2 * ystep;
	drawTestLine(x, y, x + 4, y + 7); y += 2 * ystep;
	drawTestLine(x, y, x + 4, y + 8); y += 2 * ystep;

	x += xstep; y = ystart;
	drawTestLine(x, y, x + 10, y + 1); y += ystep;
	drawTestLine(x, y, x + 10, y + 2); y += ystep;
	drawTestLine(x, y, x + 10, y + 3); y += ystep;
	drawTestLine(x, y, x + 10, y + 4); y += ystep;
	drawTestLine(x, y, x + 20, y + 1); y += ystep;
	drawTestLine(x, y, x + 20, y + 2); y += ystep;
	drawTestLine(x, y, x + 20, y + 3); y += ystep;
	drawTestLine(x, y, x + 20, y + 4); y += ystep;
	drawTestLine(x, y, x + 30, y + 1); y += ystep;
	drawTestLine(x, y, x + 30, y + 2); y += ystep;
	drawTestLine(x, y, x + 30, y + 3); y += ystep;
	drawTestLine(x, y, x + 30, y + 4); y += ystep;
	drawTestLine(x, y, x + 40, y + 1); y += ystep;
	drawTestLine(x, y, x + 40, y + 2); y += ystep;
	drawTestLine(x, y, x + 40, y + 3); y += ystep;
	drawTestLine(x, y, x + 40, y + 4); y += ystep;

	x += xstep + 40; y = ystart;
	drawTestLine(x, y, x + 30, y + 30); y += ystep + 30;
	drawTestLine(x, y, x + 30, y + 29); y += ystep + 30;
	drawTestLine(x, y, x + 30, y + 28); y += ystep + 30;
	drawTestLine(x, y, x + 30, y + 27); y += ystep + 30;
	drawTestLine(x, y, x + 30, y + 26); y += ystep + 30;
	drawTestLine(x, y, x + 30, y + 25); y += ystep + 30;
	x += xstep + 30; y = ystart;
	drawTestLine(x, y, x + 30, y + 24); y += ystep + 30;
	drawTestLine(x, y, x + 30, y + 23); y += ystep + 30;
	drawTestLine(x, y, x + 30, y + 22); y += ystep + 30;
	drawTestLine(x, y, x + 30, y + 21); y += ystep + 30;
	drawTestLine(x, y, x + 30, y + 20); y += ystep + 30;
	drawTestLine(x, y, x + 30, y + 19); y += ystep + 30;
	x += xstep + 30; y = ystart;
	drawTestLine(x, y, x + 30, y + 18); y += ystep + 30;
	drawTestLine(x, y, x + 30, y + 17); y += ystep + 30;
	drawTestLine(x, y, x + 30, y + 16); y += ystep + 30;
	drawTestLine(x, y, x + 30, y + 15); y += ystep + 30;
	drawTestLine(x, y, x + 30, y + 14); y += ystep + 30;
	drawTestLine(x, y, x + 30, y + 13); y += ystep + 30;
	x += xstep + 30; y = ystart;
	drawTestLine(x, y, x + 30, y + 12); y += ystep + 30;
	drawTestLine(x, y, x + 30, y + 11); y += ystep + 30;
	drawTestLine(x, y, x + 30, y + 10); y += ystep + 30;
	drawTestLine(x, y, x + 30, y +  9); y += ystep + 30;
	drawTestLine(x, y, x + 30, y +  8); y += ystep + 30;
	drawTestLine(x, y, x + 30, y +  7); y += ystep + 30;
	x += xstep + 30; y = ystart;
	drawTestLine(x, y, x + 30, y +  6); y += ystep + 30;
	drawTestLine(x, y, x + 30, y +  5); y += ystep + 30;
	drawTestLine(x, y, x + 30, y +  4); y += ystep + 30;
	drawTestLine(x, y, x + 30, y +  3); y += ystep + 30;
	drawTestLine(x, y, x + 30, y +  2); y += ystep + 30;
	drawTestLine(x, y, x + 30, y +  1); y += ystep + 30;
}


void draw()
{
	sceDisplaySetMode(0, SCR_WIDTH, SCR_HEIGHT);
	sceGuStart(GU_DIRECT, list);
	sceKernelDcacheWritebackAll();

	sceGuClear(GU_COLOR_BUFFER_BIT);
	sceGuDisable(GU_TEXTURE_2D);

	sceGumMatrixMode(GU_PROJECTION);
	sceGumLoadIdentity();
	sceGumPerspective(75.0f,16.0f/9.0f,0.9f,1000.0f);

	sceGumMatrixMode(GU_MODEL);
	sceGumLoadIdentity();

	sceGumMatrixMode(GU_VIEW);
	sceGumLoadIdentity();

	pVertex = vertices;
	drawTest();

	sceGumDrawArray(GU_LINES, GU_COLOR_8888|GU_VERTEX_32BITF|GU_TRANSFORM_2D, pVertex - vertices, NULL, vertices);

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

