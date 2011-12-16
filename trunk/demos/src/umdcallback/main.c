
#include <pspkernel.h>
#include <pspdebug.h>
#include <pspctrl.h>
#include <pspdisplay.h>
//#include <psputility.h>
//#include <psppower.h>
#include <pspumd.h>

#include <sys/stat.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>

PSP_MODULE_INFO("umd callback test", 0, 1, 0);
PSP_MAIN_THREAD_ATTR(THREAD_ATTR_USER);

int sceUmdDeactivate(int unit, const char *drive);
int sceUmdGetErrorStat();
int sceUmdGetDriveStat();
int sceUmdRegisterUMDCallBack(int cbid);
int sceUmdWaitDriveStatCB(int stat, int timeout);
int sceUmdCancelWaitDriveStat();

int CallbackThread(SceSize args, void *argp);
int SetupCallbacks(void);

/* Define printf, just to make typing easier */
#define printf	pspDebugScreenPrintf

int done = 0;
int printUmdInfoCount = 0;

// returns the result of sceKernelReferThreadStatus
int referThread(int thid)
{
    int result;
    SceKernelThreadInfo info;
    memset(&info, 0, sizeof(info));
    info.size = sizeof(SceKernelThreadInfo);
    result = sceKernelReferThreadStatus(thid, &info);
    printf("[%s] pri:%02x status:%02x waitType:%02x\n", info.name, info.currentPriority, info.status, info.waitType);

    if (info.status & PSP_THREAD_RUNNING) printf("  RUNNING\n");
    if (info.status & PSP_THREAD_READY) printf("  READY\n");
    if (info.status & PSP_THREAD_WAITING) printf("  WAITING\n");
    if (info.status & PSP_THREAD_SUSPEND) printf("  SUSPEND\n");
    if (info.status & PSP_THREAD_STOPPED) printf("  STOPPED\n");
    if (info.status & PSP_THREAD_KILLED) printf("  KILLED\n");

    return result;
}

void printUmdInfo()
{
    int present = sceUmdCheckMedium(0);
    int error = sceUmdGetErrorStat();
    int stat = sceUmdGetDriveStat();

    printf("[%02d] umd present=%d error=%08x stat=%02x\n", printUmdInfoCount, present, error, stat);
    printUmdInfoCount++;

    referThread(0);
}

int umd_callback(int count, int event, void *common)
{
    printf("umd_callback count=%d event=0x%02x common=0x%08X\n", count, event, (int) common);
    printUmdInfo();

    return 0;
}

struct WaitStatParams
{
    int stat;
    int timeout;
};

int waitstat_thread(SceSize args, void *argp)
{
    struct WaitStatParams *params = (struct WaitStatParams *)argp;
    int result;

    printf("sceUmdWaitDriveStatXXX stat=%02x timeout=%08x/%d ...\n",
        params->stat, params->timeout, params->timeout);

    //result = sceUmdWaitDriveStat(params->stat);
    //printf("sceUmdWaitDriveStat result %08x\n", result);

    //result = sceUmdWaitDriveStatWithTimer(params->stat, params->timeout);
    //printf("sceUmdWaitDriveStatWithTimer result %08x\n", result);

    result = sceUmdWaitDriveStatCB(params->stat, params->timeout);
    printf("sceUmdWaitDriveStatCB result %08x\n", result);

    return 0;
}

// test error codes when umd is in/out and activated/deactivated
// 0x80010013 device not found (umd not inserted)
// 0x80020321 no such device (umd not activated)
void test_io()
{
    int result;

    result = sceIoOpen("disc0:/PSP_GAME/SYSDIR/EBOOT.BIN", PSP_O_RDONLY, 0644);
    printf("sceIoOpen %08x\n", result);
    if (result >= 0)
    {
        sceIoClose(result);
    }

    result = sceIoDopen("disc0:/");
    printf("sceIoDopen %08x\n", result);
    if (result >= 0)
    {
        sceIoDclose(result);
    }

    SceIoStat stat;
    result = sceIoGetstat("disc0:/PSP_GAME/SYSDIR/EBOOT.BIN", &stat);
    printf("sceIoGetstat %08x\n", result);
}

int main(int argc, char *argv[])
{
    SceCtrlData pad;
    int result;
    int oldButtons = 0;
    int cbid = -1;
    int waitStatThid = -1;

    pspDebugScreenInit();
    if (argc > 0) {
        printf("Bootpath: %s\n", argv[0]);
    }

    printf("Triangle - Exit\n");
    printf("Left - sceUmdActivate\n");
    printf("Right - sceUmdDeactivate\n");
    printf("Cross - Delay CB\n");
    printf("Circle - Display umd info\n");
    printf("Square - Refer umd callback\n");
    printf("L-Trigger - IO test\n");
    printf("R-Trigger - Start/Stop wait stat test\n");

    SetupCallbacks();

    sceCtrlSetSamplingCycle(0);
    sceCtrlSetSamplingMode(PSP_CTRL_MODE_ANALOG);

    {
        printUmdInfo();

        // result:
        // callback events are generated if we launch from iso or immediately after psplink has reset
        cbid = sceKernelCreateCallback("UMD Callback (not active)", umd_callback, (void*)0x34343434);
        result = sceUmdRegisterUMDCallBack(cbid);
        printf("sceUmdRegisterUMDCallBack result %08X\n", result);

		// Register a second UMD callback: it will overwrite the first one.
		cbid = sceKernelCreateCallback("UMD Callback", umd_callback, (void*)0x11111111);
		result = sceUmdRegisterUMDCallBack(cbid);
        printf("sceUmdRegisterUMDCallBack result %08X\n", result);
    }

    while (!done)
    {
        sceCtrlReadBufferPositive(&pad, 1); // context switch in here
        //sceCtrlPeekBufferPositive(&pad, 1); // no context switch version
        int buttonDown = (oldButtons ^ pad.Buttons) & pad.Buttons;

        if (buttonDown & PSP_CTRL_LEFT)
        {
            result = sceUmdActivate(1, "disc0:");
            printf("sceUmdActivate result %08x\n", result);
        }

        if (buttonDown & PSP_CTRL_RIGHT)
        {
            result = sceUmdDeactivate(1, "disc0:");
            printf("sceUmdDeactivate result %08x\n", result);
        }

        if (buttonDown & PSP_CTRL_CROSS)
        {
            printf("sceKernelDelayThreadCB ...\n");
            sceKernelDelayThreadCB(10000);
        }

        if (buttonDown & PSP_CTRL_CIRCLE)
        {
            printUmdInfo();
        }

        if (buttonDown & PSP_CTRL_SQUARE)
        {
            SceKernelCallbackInfo info;
            memset(&info, 0xee, sizeof(info));
            info.size = sizeof(info);
            result = sceKernelReferCallbackStatus(cbid, &info);
            printf("sceKernelReferCallbackStatus result %08x\n", result);
            printf("  size %d (%d)\n", info.size, sizeof(info));
            printf("  name '%s'\n", info.name);
            printf("  threadId %08x (%08x)\n", info.threadId, sceKernelGetThreadId());
            printf("  callback %p common %p\n", info.callback, info.common);
            printf("  notifyCount %08x\n", info.notifyCount);
            printf("  notifyArg %08x\n", info.notifyArg);
        }

        if (buttonDown & PSP_CTRL_LTRIGGER)
        {
            test_io();
        }

        if (buttonDown & PSP_CTRL_RTRIGGER)
        {
            if (waitStatThid >= 0)
            {
                printf("Cleaning up wait stat test ...\n");

                referThread(waitStatThid);

                result = sceUmdCancelWaitDriveStat();
                printf("sceUmdCancelWaitDriveStat result %08x\n", result);

                referThread(waitStatThid);

                result = sceKernelDeleteThread(waitStatThid);
                printf("sceKernelDeleteThread result %08x\n", result);

                //result = sceKernelTerminateDeleteThread(waitStatThid);
                //printf("sceKernelTerminateDeleteThread result %08x\n", result);

                waitStatThid = -1;
            }
            else
            {
                printf("Starting wait stat test ...\n");

                // test timeout:
                // Press Right (deactivate UMD)
                // Press R-Trigger:
                // - Press R-Trigger again before 3 seconds:
                //   0x800201a9 wait cancelled
                // - Or wait 3 seconds:
                //   0x800201a8 wait timeout
                struct WaitStatParams params = { 0x20, 3000000 };

                // test internal workings:
                // - (wantStat & curStat) == wantStat
                // - (wantStat & curStat) != 0 <-- looks like this is the correct one
                //struct WaitStatParams params = { 0xFF, 3000000 };

                waitStatThid = sceKernelCreateThread("WaitUMDStat", waitstat_thread, 0x20, 0x4000, 0, 0);
                printf("sceKernelCreateThread result %08x\n", waitStatThid);
                if (waitStatThid >= 0)
                {
                    result = sceKernelStartThread(waitStatThid, sizeof(params), &params);
                    printf("sceKernelStartThread result %08x\n", result);
                }
            }
        }

        if (buttonDown & PSP_CTRL_TRIANGLE)
            done = 1;

        oldButtons = pad.Buttons;
        sceDisplayWaitVblank(); // only catch callback when we press Cross (sceKernelDelayThreadCB)
        //sceDisplayWaitVblankCB(); // catch all callback events
    }

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

    cbid = sceKernelCreateCallback("Exit Callback", exit_callback, (void*)0x12121212);
    sceKernelRegisterExitCallback(cbid);

    sceKernelSleepThreadCB();

    return 0;
}

/* Sets up the callback thread and returns its thread id */
int SetupCallbacks(void)
{
    int thid = 0;

    thid = sceKernelCreateThread("update_thread", CallbackThread,
                     0x11, 0xFA0, 0, 0);
    if(thid >= 0)
    {
        sceKernelStartThread(thid, 0, 0);
    }

    return thid;
}
