
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

int sceUmdGetErrorStat();
int sceUmdGetDriveStat();
int sceUmdRegisterUMDCallBack(int cbid);
int sceUmdWaitDriveStatCB(int stat, int *timeout);

int CallbackThread(SceSize args, void *argp);
int SetupCallbacks(void);

/* Define printf, just to make typing easier */
#define printf	pspDebugScreenPrintf

int done = 0;

int umd_callback(int arg1, int arg2, void *common)
{
    SceKernelThreadInfo info;
    info.size = sizeof(SceKernelThreadInfo);
    sceKernelReferThreadStatus(0, &info);

    FILE *f = fopen("dump.txt", "a");
    if (f)
    {
        fprintf(f, "umd callback\n");
        fprintf(f, "thread %s %d\n", info.name, info.currentPriority);
        fprintf(f, "arg1=0x%08x\n", arg1);
        fprintf(f, "arg2=0x%08x\n", arg2);
        fprintf(f, "common=%p\n", common);
        fclose(f);
    }

    return 0;
}

int main(int argc, char *argv[])
{
    SceCtrlData pad;
    int result;
    int oldButtons = 0;

    // clear log file
    FILE *f = fopen("dump.txt", "w");
    if (f) fclose(f);

    pspDebugScreenInit();
    if (argc > 0) {
        printf("Bootpath: %s\n", argv[0]);
    }

    printf("Triangle - Exit\n");
    printf("Left - sceUmdCheckMedium\n");
    printf("Right - sceUmdActivate\n");
    printf("Up - sceUmdGetErrorStat\n");
    printf("Down - sceUmdGetDriveStat\n");
    printf("Cross - Delay CB\n");

    SetupCallbacks();

    sceCtrlSetSamplingCycle(0);
    sceCtrlSetSamplingMode(PSP_CTRL_MODE_ANALOG);

    {
        int cbid = sceKernelCreateCallback("UMD Callback", umd_callback, (void*)0x34343434);
        sceUmdRegisterUMDCallBack(cbid);
    }

    while(!done)
    {
        sceCtrlReadBufferPositive(&pad, 1); // context switch in here
        //sceCtrlPeekBufferPositive(&pad, 1); // no context switch version
        int buttonDown = (oldButtons ^ pad.Buttons) & pad.Buttons;

        if (buttonDown & PSP_CTRL_CROSS)
        {
            printf("sceKernelDelayThreadCB ...\n");
            sceKernelDelayThreadCB(1000);
        }

        if (buttonDown & PSP_CTRL_LEFT)
        {
            result = sceUmdCheckMedium(0);
            printf("sceUmdCheckMedium result %08x\n", result);
        }

        if (buttonDown & PSP_CTRL_RIGHT)
        {
            result = sceUmdActivate(1, "disc0:");
            printf("sceUmdActivate result %08x\n", result);

            //result = sceUmdWaitDriveStatCB(0x20, NULL); // umd in
            result = sceUmdWaitDriveStatCB(0x1, NULL); // umd not in
            printf("sceUmdWaitDriveStatCB result %08x\n", result);
        }

        if (buttonDown & PSP_CTRL_UP)
        {
            result = sceUmdGetErrorStat();
            printf("sceUmdGetErrorStat result %08x\n", result);
        }

        if (buttonDown & PSP_CTRL_DOWN)
        {
            result = sceUmdGetDriveStat();
            printf("sceUmdGetDriveStat result %08x\n", result);
        }


        if (buttonDown & PSP_CTRL_TRIANGLE)
            done = 1;

        oldButtons = pad.Buttons;
        sceDisplayWaitVblank();
    }

    sceKernelExitGame();
    return 0;
}

/* Exit callback */
int exit_callback(int arg1, int arg2, void *common)
{
    SceKernelThreadInfo info;
    info.size = sizeof(SceKernelThreadInfo);
    sceKernelReferThreadStatus(0, &info);

    FILE *f = fopen("dump.txt", "a");
    if (f)
    {
        fprintf(f, "exit callback\n");
        fprintf(f, "thread %s %d\n", info.name, info.currentPriority);
        fprintf(f, "arg1=0x%08x\n", arg1);
        fprintf(f, "arg2=0x%08x\n", arg2);
        fprintf(f, "common=%p\n", common);
        fclose(f);
    }

    done = 1;
    return 0;
}

/* Callback thread */
int CallbackThread(SceSize args, void *argp)
{
    int cbid;

    cbid = sceKernelCreateCallback("Exit Callback", exit_callback, (void*)0x12121212);
    sceKernelRegisterExitCallback(cbid);

    //cbid = sceKernelCreateCallback("UMD Callback", umd_callback, (void*)0x34343434);
    //sceUmdRegisterUMDCallBack(cbid);

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
