#include <pspkernel.h>
#include <pspdisplay.h>
#include <pspdebug.h>

#include <pspctrl.h>
#include <pspgu.h>
#include <psprtc.h>

#include <stdlib.h>
#include <stdio.h>
#include <math.h>
#include <string.h>

PSP_MODULE_INFO("rtctest", 0, 1, 1);
PSP_MAIN_THREAD_ATTR(THREAD_ATTR_USER);

#define printf pspDebugScreenPrintf

int exit_callback(int arg1, int arg2, void *common);
int CallbackThread(SceSize args, void *argp);
int SetupCallbacks(void);

int done = 0;
void *fbp0;		// frame buffer

unsigned int frames = 0;
unsigned int totalfps = 0, totalseconds = 0;

int lastfps, averagefps, lastfpskip;

u32 tickResolution;
u64 fpsTickNow;
u64 fpsTickLast;

void updateDrawFPS()
{
    u64 fpsTickNow;
    u64 TickDelta;

    frames++;
    totalfps++;

    sceRtcGetCurrentTick( &fpsTickNow );

    TickDelta = fpsTickNow - fpsTickLast;
    if (TickDelta >= tickResolution)
    {
        fpsTickLast = fpsTickNow;
        totalseconds++;

        // fps from last "second"
        lastfps = frames;

        // average for entire program running time
        averagefps = totalfps / totalseconds;

        // fps from last second, or from last call to this function, which ever gives the lower fps
        lastfpskip = (u64)(frames * tickResolution) / TickDelta;

        pspDebugScreenSetOffset( (int)fbp0 );
        pspDebugScreenSetXY( 0, 32 );
        printf("FPS last:%05d/%05d ave:%05d \n", lastfps, lastfpskip, averagefps);
        printf("delta ticks: %lld", TickDelta);

        frames = 0;
    }
}

void printGetDayOfWeek()
{
    int i;

    printf("sceRtcGetDayOfWeek\n");
    for (i = 0; i < 7; i++)
    {
        int number = sceRtcGetDayOfWeek(2008, 1, 7 + i);
        printf("2008-01-%02d: day = %d\n", (7 + i), number);
    }
}

void printGetDaysInMonth()
{
    int i;
    int days[12];

    for (i = 0; i < 12; i++)
    {
        days[i] = sceRtcGetDaysInMonth(2008, 1 + i);
    }

    printf("sceRtcGetDaysInMonth\n");
    printf("%d, %d, %d, %d\n", days[0], days[1], days[2], days[3]);
    printf("%d, %d, %d, %d\n", days[4], days[5], days[6], days[7]);
    printf("%d, %d, %d, %d\n", days[8], days[9], days[10], days[11]);
}

int main(int argc, char **argv)
{
	SceCtrlData pad;
    int oldButtons = 0;
    int useVblank = 1;

	pspDebugScreenInit();
	if (argc > 0) {
		printf("Bootpath: %s\n", argv[0]);
	}

    printf("Triangle - Exit\n");
    printf("Square - Toggle vblank (60 fps limit)\n");
    printf("\n");

	SetupCallbacks();

	sceCtrlSetSamplingCycle(0);
	sceCtrlSetSamplingMode(PSP_CTRL_MODE_ANALOG);

    sceRtcGetCurrentTick( &fpsTickLast );
	tickResolution = sceRtcGetTickResolution();

    printGetDayOfWeek();
    printf("\n");
    printGetDaysInMonth();
    printf("\n");
    printf("tickResolution: %d", (int)tickResolution);

    while(!done)
    {
        sceCtrlPeekBufferPositive(&pad, 1);
        int buttonDown = (oldButtons ^ pad.Buttons) & pad.Buttons;

        if (buttonDown & PSP_CTRL_SQUARE)
        {
            useVblank ^= 1;
        }

        if (buttonDown & PSP_CTRL_TRIANGLE)
            done = 1;

        oldButtons = pad.Buttons;

        updateDrawFPS();

        if (useVblank)
            sceDisplayWaitVblankStart();
        fbp0 = sceGuSwapBuffers();
    }

	sceKernelExitGame();	// Quits Application
	return 0;
}

int exit_callback(int arg1, int arg2, void *common)
{
    done = 1;
    return 0;
}

// Callback thread
int CallbackThread(SceSize args, void *argp)
{
    int cbid;

    cbid = sceKernelCreateCallback("Exit Callback", exit_callback, NULL);
    sceKernelRegisterExitCallback(cbid);

    sceKernelSleepThreadCB();

    return 0;
}

// Sets up the callback thread and returns its thread id
int SetupCallbacks(void)
{
    int thid = 0;

    thid = sceKernelCreateThread("update_thread", CallbackThread, 0x11, 0xFA0, 0, 0);
    if(thid >= 0) {
        sceKernelStartThread(thid, 0, 0);
    }

    return thid;
}
