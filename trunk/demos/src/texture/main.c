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

PSP_MODULE_INFO("Texture Test", 0, 1, 0);
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

unsigned int __attribute__((aligned(16))) clutTable[] = { 0xFFFFFFFF, 0x00000000, 0x00FF0000, 0x0000FF00, 0x000000FF, 0x00FF00FF };
unsigned char __attribute__((aligned(16))) imageData[] =
{ 0x01, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x01,
  0x05, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x03,
  0x05, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03,
  0x05, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03,
  0x05, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03,
  0x05, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03,
  0x05, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03,
  0x05, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03,
  0x05, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03,
  0x05, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03,
  0x05, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x03,
  0x05, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x03,
  0x05, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x03,
  0x05, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x03,
  0x05, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x03,
  0x01, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x01
};


struct vertex
{
	float tu, tv;
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

void drawVertex(float u, float v, float x, float y)
{
	pVertex->tu = u;
	pVertex->tv = v;
	pVertex->px = x;
	pVertex->py = y;
	pVertex->pz = 0.0f;
	pVertex++;
}

void drawRectangle(float x1, float y1, float x2, float y2, float u1, float v1, float u2, float v2)
{
	struct vertex *pVertexBase = pVertex;
	drawVertex(u1, v1, x1, y1);
	drawVertex(u2, v2, x2, y2);
	sceGumDrawArray(GU_SPRITES, GU_TEXTURE_32BITF|GU_VERTEX_32BITF|GU_TRANSFORM_2D, pVertex - pVertexBase, NULL, pVertexBase);
	sceGumDrawArray(GU_SPRITES, GU_TEXTURE_32BITF|GU_VERTEX_32BITF|GU_TRANSFORM_3D, pVertex - pVertexBase, NULL, pVertexBase);

	pVertexBase = pVertex;
	drawVertex(u1, v1, x1 + SCR_WIDTH / 4, y1);
	drawVertex(u1, v2, x1 + SCR_WIDTH / 4, y2);
	drawVertex(u2, v1, x2 + SCR_WIDTH / 4, y1);
	sceGumDrawArray(GU_TRIANGLE_STRIP, GU_TEXTURE_32BITF|GU_VERTEX_32BITF|GU_TRANSFORM_2D, pVertex - pVertexBase, NULL, pVertexBase);
	sceGumDrawArray(GU_TRIANGLE_STRIP, GU_TEXTURE_32BITF|GU_VERTEX_32BITF|GU_TRANSFORM_3D, pVertex - pVertexBase, NULL, pVertexBase);
	pVertexBase = pVertex;
	drawVertex(u1, v2, x1 + SCR_WIDTH / 4, y2);
	drawVertex(u2, v1, x2 + SCR_WIDTH / 4, y1);
	drawVertex(u2, v2, x2 + SCR_WIDTH / 4, y2);
	sceGumDrawArray(GU_TRIANGLE_STRIP, GU_TEXTURE_32BITF|GU_VERTEX_32BITF|GU_TRANSFORM_2D, pVertex - pVertexBase, NULL, pVertexBase);
	sceGumDrawArray(GU_TRIANGLE_STRIP, GU_TEXTURE_32BITF|GU_VERTEX_32BITF|GU_TRANSFORM_3D, pVertex - pVertexBase, NULL, pVertexBase);
}

void drawTest()
{
	float x, y;
	float xstep, ystep, xstart, ystart;

	xstart = 2;
	ystart = 10;
	xstep = 18;
	ystep = 20;
	x = xstart;
	y = ystart;

	sceGuTexFilter(GU_NEAREST, GU_NEAREST);
	drawRectangle(x, y, x + 16, y + 16, 0, 0, 15.0f, 15.0f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0, 0, 15.1f, 15.1f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0, 0, 15.5f, 15.5f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0, 0, 15.6f, 15.6f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0, 0, 15.9f, 15.9f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0, 0, 16.0f, 16.0f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0, 0, 16.1f, 16.1f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0, 0, 16.4f, 16.4f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0, 0, 16.5f, 16.5f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0, 0, 16.6f, 16.6f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0, 0, 16.9f, 16.9f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0, 0, 17.0f, 17.0f); y += ystep;

	x += xstep; y = ystart;
	sceGuTexFilter(GU_NEAREST, GU_NEAREST);
	drawRectangle(x, y, x + 16, y + 16, 0.0f, 0.0f, 15.6f, 15.6f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.1f, 0.1f, 15.6f, 15.6f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.2f, 0.2f, 15.6f, 15.6f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.3f, 0.3f, 15.6f, 15.6f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.4f, 0.4f, 15.6f, 15.6f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.5f, 0.5f, 15.6f, 15.6f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.6f, 0.6f, 15.6f, 15.6f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.7f, 0.7f, 15.6f, 15.6f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.8f, 0.8f, 15.6f, 15.6f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.9f, 0.9f, 15.6f, 15.6f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 1.0f, 1.0f, 15.6f, 15.6f); y += ystep;

	x += 25; y = ystart;
	sceGuTexFilter(GU_NEAREST, GU_NEAREST);
	drawRectangle(x, y, x + 16, y + 16, 0.0f, 0.0f, 1.0f, 1.0f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.0f, 0.0f, 1.1f, 1.1f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.0f, 0.0f, 1.2f, 1.2f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.0f, 0.0f, 1.3f, 1.3f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.0f, 0.0f, 1.4f, 1.4f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.0f, 0.0f, 1.5f, 1.5f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.0f, 0.0f, 1.6f, 1.6f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.0f, 0.0f, 1.7f, 1.7f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.0f, 0.0f, 1.8f, 1.8f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.0f, 0.0f, 1.9f, 1.9f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.0f, 0.0f, 2.0f, 2.0f); y += ystep;

	x += xstep; y = ystart;
	sceGuTexFilter(GU_NEAREST, GU_NEAREST);
	drawRectangle(x, y, x + 16, y + 16, 0.0f, 0.0f, 1.0f, 1.0f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.1f, 0.1f, 1.1f, 1.1f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.2f, 0.2f, 1.2f, 1.2f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.3f, 0.3f, 1.3f, 1.3f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.4f, 0.4f, 1.4f, 1.4f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.5f, 0.5f, 1.5f, 1.5f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.6f, 0.6f, 1.6f, 1.6f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.7f, 0.7f, 1.7f, 1.7f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.8f, 0.8f, 1.8f, 1.8f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.9f, 0.9f, 1.9f, 1.9f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 1.0f, 1.0f, 2.0f, 2.0f); y += ystep;

	x += xstep; y = ystart;
	sceGuTexFilter(GU_LINEAR, GU_LINEAR);
	drawRectangle(x, y, x + 16, y + 16, 0.0f, 0.0f, 1.0f, 1.0f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.0f, 0.0f, 1.1f, 1.1f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.0f, 0.0f, 1.2f, 1.2f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.0f, 0.0f, 1.3f, 1.3f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.0f, 0.0f, 1.4f, 1.4f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.0f, 0.0f, 1.5f, 1.5f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.0f, 0.0f, 1.6f, 1.6f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.0f, 0.0f, 1.7f, 1.7f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.0f, 0.0f, 1.8f, 1.8f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.0f, 0.0f, 1.9f, 1.9f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.0f, 0.0f, 2.0f, 2.0f); y += ystep;

	x += xstep; y = ystart;
	sceGuTexFilter(GU_LINEAR, GU_LINEAR);
	drawRectangle(x, y, x + 16, y + 16, 0.0f, 0.0f, 1.0f, 1.0f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.1f, 0.1f, 1.1f, 1.1f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.2f, 0.2f, 1.2f, 1.2f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.3f, 0.3f, 1.3f, 1.3f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.4f, 0.4f, 1.4f, 1.4f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.5f, 0.5f, 1.5f, 1.5f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.6f, 0.6f, 1.6f, 1.6f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.7f, 0.7f, 1.7f, 1.7f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.8f, 0.8f, 1.8f, 1.8f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 0.9f, 0.9f, 1.9f, 1.9f); y += ystep;
	drawRectangle(x, y, x + 16, y + 16, 1.0f, 1.0f, 2.0f, 2.0f); y += ystep;
}


void draw()
{
	sceDisplaySetMode(0, SCR_WIDTH, SCR_HEIGHT);
	sceGuStart(GU_DIRECT, list);
	sceKernelDcacheWritebackAll();

	sceGuClear(GU_COLOR_BUFFER_BIT);
	sceGuDepthRange(0, 65535);

	sceGuEnable(GU_TEXTURE_2D);
	sceGuClutMode(GU_PSM_8888, 0, 0xFF, 0);
	sceGuTexMode(GU_PSM_T8, 0, 0, GU_FALSE);
	sceGuTexMapMode(GU_TEXTURE_COORDS, 0, 0);
	sceGuTexScale(1.f / 16.f, 1.f / 16.f);
	sceGuTexOffset(0, 0);
	sceGuTexEnvColor(0xFF000000);
	sceGuTexFunc(GU_TFX_DECAL, GU_TCC_RGB);
	sceGuTexWrap(GU_REPEAT,GU_REPEAT);
	sceGuTexImage(0, 16, 16, 16, imageData);
	sceGuClutLoad(1, clutTable);

	sceGumMatrixMode(GU_PROJECTION);
	sceGumLoadIdentity();
	sceGumPerspective(75.0f,16.0f/9.0f,0.9f,1000.0f);

	sceGumMatrixMode(GU_MODEL);
	sceGumLoadIdentity();

	sceGumMatrixMode(GU_VIEW);
	sceGumLoadIdentity();
	ScePspFVector3 scale;
	scale.x = 1.0f;
	scale.y = -1.0f;
	scale.z = 1.0f;
	sceGumScale(&scale); // Render 3D upside-down
	ScePspFVector3 translation;
	translation.x = 0.0f;
	translation.y = -138.0f;
	translation.z = -176.0f;
	sceGumTranslate(&translation);

	pVertex = vertices;
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
	sceGuDepthOffset(0);
	sceGuDepthRange(10000,50000);
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

