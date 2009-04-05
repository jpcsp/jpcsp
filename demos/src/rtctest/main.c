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
void *fbp0;        // frame buffer

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

    printf("sceRtcGetDayOfWeek, 2008-01-07 is a monday (yyyy-mm-dd)\n");
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

void printSetTick()
{
    pspTime psptime;
    u64 tick;

    memset(&psptime, 0xee, sizeof(psptime));
    printf("0xee -> %04d-%02d-%02d %02d:%02d:%02d %d\n",
        psptime.year, psptime.month, psptime.day,
        psptime.hour, psptime.minutes, psptime.seconds,
        (unsigned int)psptime.microseconds);

    printf("sceRtcSetTick\n");

    tick = 4901191089332944926LL;
    memset(&psptime, 0, sizeof(psptime));
    sceRtcSetTick(&psptime, &tick);
    printf("%lld -> %04d-%02d-%02d %02d:%02d:%02d %d\n",
        tick,
        psptime.year, psptime.month, psptime.day,
        psptime.hour, psptime.minutes, psptime.seconds,
        (unsigned int)psptime.microseconds);

    tick = 63366462245000000LL;
    memset(&psptime, 0, sizeof(psptime));
    sceRtcSetTick(&psptime, &tick);
    printf("  %lld -> %04d-%02d-%02d %02d:%02d:%02d %d\n",
        tick,
        psptime.year, psptime.month, psptime.day,
        psptime.hour, psptime.minutes, psptime.seconds,
        (unsigned int)psptime.microseconds);
}

void printGetTick()
{
    pspTime psptime;
    u64 tick;

    printf("sceRtcGetTick\n");

    memset(&psptime, 0, sizeof(psptime));
    sceRtcGetTick(&psptime, &tick);
    printf("0 -> %lld\n", tick);

    psptime.year = 1970;
    psptime.month = 1;
    psptime.day = 1;
    psptime.hour = 0;
    psptime.minutes = 0;
    psptime.seconds = 0;
    psptime.microseconds = 0;
    sceRtcGetTick(&psptime, &tick);
    printf("1970-01-01 00:00:00 -> %lld\n", tick);

    psptime.seconds = 1;
    sceRtcGetTick(&psptime, &tick);
    printf("1970-01-01 00:00:01 -> %lld\n", tick);

    psptime.hour = 1;
    psptime.seconds = 0;
    sceRtcGetTick(&psptime, &tick);
    printf("1970-01-01 00:01:00 -> %lld\n", tick);

    psptime.year = 2009;
    psptime.month = 1;
    psptime.day = 2;
    psptime.hour = 3;
    psptime.minutes = 4;
    psptime.seconds = 5;
    psptime.microseconds = 0;
    sceRtcGetTick(&psptime, &tick);
    printf("2009-01-02 03:04:05 -> %lld\n", tick);
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

    printSetTick();
    printf("\n");

    printGetTick();
    printf("\n");

    printf("sceRtcGetTickResolution: %d", (int)tickResolution);

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

    sceKernelExitGame();    // Quits Application
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
