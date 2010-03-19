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

PSP_MODULE_INFO("GE Test", 0, 1, 0);
PSP_MAIN_THREAD_ATTR(THREAD_ATTR_USER);

#define GU_BEHAVIOR_BREAK	3

#define BUF_WIDTH	512
#define SCR_WIDTH	480
#define SCR_HEIGHT	272
#define FONT_HEIGHT	8

//#define USE_VERTEX_8BIT	1
//#define USE_VERTEX_16BIT	1
#define USE_VERTEX_32BITF	1

//#define USE_TEXTURE_8BIT	1
//#define USE_TEXTURE_16BIT	1
#define USE_TEXTURE_32BITF	1

extern int sceDisplayIsVblank();
extern float sceDisplayGetFramePerSec();
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
static unsigned int __attribute__((aligned(16))) list2[1024];
static unsigned int __attribute__((aligned(16))) list3[1024];

static unsigned int staticOffset = 0;
void* fbp0;
void* fbp1;
void* zbp;
int done = 0;
int doDrawSync = 1;

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

unsigned int __attribute__((aligned(16))) clutTable10[] = { 0xFFFFFFFF, 0x00000000 };
unsigned int __attribute__((aligned(16))) clutTable11[] = { 0xFFFFFFFF, 0x00000000 };
unsigned int __attribute__((aligned(16))) clutTable12[] = { 0xFFFFFFFF, 0x00000000 };
unsigned int __attribute__((aligned(16))) clutTable13[] = { 0xFFFFFFFF, 0x00000000 };
unsigned int __attribute__((aligned(16))) clutTable14[] = { 0xFFFFFFFF, 0x00000000 };
unsigned int __attribute__((aligned(16))) clutTable15[] = { 0xFFFFFFFF, 0x00000000 };
unsigned char __attribute__((aligned(16))) imageData10[] =
							{ 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
							};
unsigned char __attribute__((aligned(16))) imageData11[] =
							{ 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
							};

struct
{
	unsigned short a, b;
	unsigned short x, y, z;
} vertices10[2] = { {0, 0, 10, 10, 0}, {1, 1, 30, 20, 0} };

struct
{
	unsigned short a, b;
	unsigned short x, y, z;
} vertices11[2] = { {0, 0, 10, 20, 0}, {1, 1, 30, 30, 0} };

struct
{
	unsigned short a, b;
	unsigned short x, y, z;
} vertices12[2] = { {0, 0, 50, 10, 0}, {1, 1, 70, 20, 0} };

struct
{
	unsigned short a, b;
	unsigned short x, y, z;
} vertices13[2] = { {0, 0, 50, 20, 0}, {1, 1, 70, 30, 0} };

struct
{
	unsigned short a, b;
	unsigned short x, y, z;
} vertices14[2] = { {0, 0, 90, 10, 0}, {1, 1, 110, 20, 0} };

struct
{
	unsigned short a, b;
	unsigned short x, y, z;
} vertices15[2] = { {0, 0, 90, 20, 0}, {1, 1, 110, 30, 0} };

int x = 0;

char message[100000];

struct
{
	int id;
	int status;
} listStatus[10000];

int currentListStatus = 0;


void debugGeListSync(char *context)
{
	char s[1000];
	int i;

	sprintf(s, "%s\n", context);
	strcat(message, s);

	for (i = 0; i < currentListStatus; i++)
	{
		int status = sceGeListSync(listStatus[i].id, 1);
		int previousStatus = listStatus[i].status;

		if (status != previousStatus)
		{
			if (previousStatus == -1)
			{
				sprintf(s, "List #%3d/%3d: id %08X, status   -> %01X\n", i, currentListStatus, listStatus[i].id, status);
			}
			else
			{
				sprintf(s, "List #%3d/%3d: id %08X, status %01X -> %01X\n", i, currentListStatus, listStatus[i].id, previousStatus, status);
			}

			strcat(message, s);

			listStatus[i].status = status;
		}
	}
}

void addListStatus(int id)
{
	listStatus[currentListStatus].id = id;
	listStatus[currentListStatus].status = -1;

	currentListStatus++;
}


void delay()
{
	x++;
}

void delayLoop(int count)
{
	int i;
	for (i = 0; i < count; i++)
	{
		delay();
	}
}

void geCallback(int signal)
{
	debugGeListSync("Start GE Callback");

	delayLoop(100000);

	if (signal == 11)
	{
		clutTable11[0] = 0xFF00FF00;	// Green
		sceKernelDcacheWritebackAll();
	}
	if (signal == 13)
	{
		clutTable13[0] = 0xFF00FF00;	// Green
		sceKernelDcacheWritebackAll();
	}
	if (signal == 15)
	{
		clutTable15[0] = 0xFF00FF00;	// Green
		sceKernelDcacheWritebackAll();
		sceGeContinue();
	}
	if (signal >= 21 && signal <= 26)
	{
		char s[1000];
		sprintf(s, "Signal %d\n", signal);
		strcat(message, s);
	}

	debugGeListSync("End GE Callback");
}

void drawTest()
{
	debugGeListSync("Start drawTest");

	sceGuSetCallback(GU_CALLBACK_SIGNAL, geCallback);
	clutTable10[0] = 0xFFFFFFFF;	// White
	clutTable11[0] = 0xFFFF0000;	// Blue
	clutTable12[0] = 0xFFFFFFFF;	// White
	clutTable13[0] = 0xFFFF0000;	// Blue
	clutTable14[0] = 0xFFFFFFFF;	// White
	clutTable15[0] = 0xFFFF0000;	// Blue
	sceKernelDcacheWritebackAll();
	sceGuClear(GU_COLOR_BUFFER_BIT);
	sceGuEnable(GU_TEXTURE_2D);
	sceGuMaterial(GU_AMBIENT, 0x00FFFFFF);
	sendCommandi(88, 0x00);
	sceGuMaterial(GU_DIFFUSE, 0x00FFFFFF);
	sceGuMaterial(GU_SPECULAR, 0x00FFFFFF);
	sceGuClutMode(GU_PSM_8888, 0, 0xFF, 0);
	sceGuTexMode(GU_PSM_T8, 0, 0, GU_FALSE);
	sceGuTexEnvColor(0xFF000000);
	sceGuTexFunc(GU_TFX_DECAL, GU_TCC_RGB);
	sceGuTexImage(0, 16, 16, 16, imageData10);

	debugGeListSync("Before 1st SPRITE");

	sceGuClutLoad(1, clutTable10);
	sceGuDrawArray(GU_SPRITES, GU_TEXTURE_16BIT|GU_VERTEX_16BIT|GU_TRANSFORM_2D, 2, NULL, vertices10);
	sceGuSignal(GU_BEHAVIOR_SUSPEND, 11);
	sceGuClutLoad(1, clutTable11);
	sceGuDrawArray(GU_SPRITES, GU_TEXTURE_16BIT|GU_VERTEX_16BIT|GU_TRANSFORM_2D, 2, NULL, vertices11);

	debugGeListSync("After 1st SPRITE");

	sceGuClutLoad(1, clutTable12);
	sceGuDrawArray(GU_SPRITES, GU_TEXTURE_16BIT|GU_VERTEX_16BIT|GU_TRANSFORM_2D, 2, NULL, vertices12);
	sceGuSignal(GU_BEHAVIOR_CONTINUE, 13);
	sceGuClutLoad(1, clutTable13);
	sceGuDrawArray(GU_SPRITES, GU_TEXTURE_16BIT|GU_VERTEX_16BIT|GU_TRANSFORM_2D, 2, NULL, vertices13);

	debugGeListSync("After 2nd SPRITE");

	sceGuClutLoad(1, clutTable14);
	sceGuDrawArray(GU_SPRITES, GU_TEXTURE_16BIT|GU_VERTEX_16BIT|GU_TRANSFORM_2D, 2, NULL, vertices14);
	sceGuSignal(GU_BEHAVIOR_BREAK, 15);
	sceGuClutLoad(1, clutTable15);
	sceGuDrawArray(GU_SPRITES, GU_TEXTURE_16BIT|GU_VERTEX_16BIT|GU_TRANSFORM_2D, 2, NULL, vertices15);

	debugGeListSync("After 3rd SPRITE");
}

void drawTest2()
{
	sceGuStart(GU_DIRECT, list2);
	sceGuSignal(GU_BEHAVIOR_SUSPEND, 21);
	unsigned int *backupStart = gu_list->start;
	unsigned int *backupCurrent = gu_list->current;
	int backupList = ge_list_executed[0];

	sceGuStart(GU_DIRECT, list3);
	sceGuSignal(GU_BEHAVIOR_SUSPEND, 22);
	sceGuFinish();

	gu_list->start = backupStart;
	gu_list->current = backupCurrent;
	ge_list_executed[0] = backupList;
	sceGuSignal(GU_BEHAVIOR_SUSPEND, 23);

//	sceGuStart(GU_DIRECT, list2);
//	sceGuSignal(GU_BEHAVIOR_SUSPEND, 24);
//	sceGuSignal(GU_BEHAVIOR_SUSPEND, 25);
//	sceGuSignal(GU_BEHAVIOR_SUSPEND, 26);
	sceGuFinish();
}


void draw()
{
	sceDisplaySetMode(0, SCR_WIDTH, SCR_HEIGHT);
	sceGuStart(GU_DIRECT, list);
	addListStatus(ge_list_executed[0]);
	sceKernelDcacheWritebackAll();

	debugGeListSync("Start");

	drawTest();
	sceGuFinish();
	if (doDrawSync)
	{
		sceGuSync(0, 0);

		debugGeListSync("After DrawSync");
	}
	else
	{
		debugGeListSync("After Finish (no DrawSync)");
	}

	drawTest2();
	sceGuSync(0, 0);

	pspDebugScreenSetBackColor(0x00000000);
	pspDebugScreenSetXY(0, 10);
	pspDebugScreenPuts(message);

	int fd = sceIoOpen("ms0:/GEtest.log", PSP_O_APPEND | PSP_O_CREAT | PSP_O_WRONLY, 0777);
	sceIoWrite(fd, message, strlen(message));
	sceIoClose(fd);
	strcpy(message, "");

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

	sceIoRemove("ms0:/GEtest.log");

	int vblankCount = 0;
	int notVblankCount = 0;
	int i;
	for (i = 0; i < 1000000; i++)
	{
		if (sceDisplayIsVblank())
		{
			vblankCount++;
		}
		else
		{
			notVblankCount++;
		}
	}

	char s[1000];
	sprintf(s, "Vblank: %d, not Vblank: %d\n", vblankCount, notVblankCount);
	strcat(message, s);
	sprintf(s, "sceDisplayGetFramePerSec: %f\n", sceDisplayGetFramePerSec());
	strcat(message, s);
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
			doDrawSync = !doDrawSync;
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

