
#include <pspkernel.h>
#include <pspdebug.h>
#include <pspctrl.h>
#include <pspdisplay.h>
#include <psputility.h>
#include <psppower.h>

#include <sys/stat.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>

PSP_MODULE_INFO("event flag test", 0, 1, 0);
PSP_MAIN_THREAD_ATTR(THREAD_ATTR_USER);

int exit_callback(int arg1, int arg2, void *common);
int CallbackThread(SceSize args, void *argp);
int SetupCallbacks(void);
void createStartThread(const char *name, SceKernelThreadEntry entry);

/* Define printf, just to make typing easier */
#define printf	pspDebugScreenPrintf

#define INITIAL_PATTERN 0x00FF00FF

int done = 0;
int evid = -1;

#define AND_PATTERN 0x000000FF
int and_thread(SceSize args, void *argp)
{
	int result;
    u32 outBits;
    SceKernelThreadInfo info;
    info.size = sizeof(SceKernelThreadInfo);

    sceKernelReferThreadStatus (0, &info);
    printf("thread '%-12s' sp %p pattern %08x START\n", info.name, info.stack, AND_PATTERN);

    while(!done)
    {
        // 0x00FF00FF initPattern
        // 0x000000FF should work - ok
        // 0x00000FF0 should fail - ok
        // 0x0000FF00 should fail - ok

        outBits = 0xbaadc0de;
        result = sceKernelWaitEventFlag(evid, AND_PATTERN, PSP_EVENT_WAITAND, &outBits, 0);
        //result = sceKernelPollEventFlag(evid, AND_PATTERN, PSP_EVENT_WAITAND, &outBits);
        printf("thread '%-12s' result %08x outBits %08x\n", info.name, result, (int)outBits);

        sceKernelSleepThreadCB();
    }

	return 0;
}

#define OR_PATTERN 0x0000FF00
int or_thread(SceSize args, void *argp)
{
	int result;
    u32 outBits;
    SceKernelThreadInfo info;
    info.size = sizeof(SceKernelThreadInfo);

    sceKernelReferThreadStatus (0, &info);
    printf("thread '%-12s' sp %p pattern %08x START\n", info.name, info.stack, OR_PATTERN);

    while(!done)
    {
        // 0x00FF00FF initPattern
        // 0x000000FF should work - ok
        // 0x00000FF0 should work - ok
        // 0x0000FF00 should fail - ok

        outBits = 0xbaadc0de;
        volatile SceUInt timeout = 1000*1000*5;
        //result = sceKernelWaitEventFlag(evid, OR_PATTERN, PSP_EVENT_WAITOR, &outBits, &timeout);
        result = sceKernelWaitEventFlag(evid, OR_PATTERN, PSP_EVENT_WAITOR, &outBits, 0);
        printf("thread '%-12s' result %08x outBits %08x\n", info.name, result, (int)outBits);

        //if (timeout) timeout = *(int*)timeout;
        printf("timeout %d/%08x\n", (int)timeout, (int)timeout);


        sceKernelSleepThreadCB();
    }

	return 0;
}

#define CLEAR_PATTERN 0x00000FF0
int clear_thread(SceSize args, void *argp)
{
	int result;
    u32 outBits;
    SceKernelThreadInfo info;
    info.size = sizeof(SceKernelThreadInfo);

    sceKernelReferThreadStatus (0, &info);
    printf("thread '%-12s' sp %p pattern %08x START\n", info.name, info.stack, CLEAR_PATTERN);

    while(!done)
    {
        // 0x00FF00FF initPattern
        // 0x000000FF should work - ok
        // 0x00000FF0 should fail - ok
        // 0x0000FF00 should fail - ok

        outBits = 0xbaadc0de;
        result = sceKernelWaitEventFlag(evid, CLEAR_PATTERN, PSP_EVENT_WAITCLEAR, &outBits, 0);
        printf("thread '%-12s' result %08x outBits %08x\n", info.name, result, (int)outBits);

        sceKernelSleepThreadCB();
    }

	return 0;
}

void printSceKernelEventFlagInfo(int evid)
{
    SceKernelEventFlagInfo info;
    info.size = sizeof(SceKernelEventFlagInfo);
    memset(&info, 0xFF, sizeof(SceKernelEventFlagInfo));
    int result = sceKernelReferEventFlagStatus(evid, &info);
    printf("\nsceKernelReferEventFlagStatus result %08x\n", result);
    if (info.attr != PSP_EVENT_WAITMULTIPLE || info.initPattern != INITIAL_PATTERN)
        printf("attr %08x initPattern %08x\n", info.attr, info.initPattern);
    printf("pattern %08x wait# %d/%08x\n\n", info.currentPattern, info.numWaitThreads, info.numWaitThreads);
}

int main(int argc, char *argv[])
{
	SceCtrlData pad;
    int result;
    int oldButtons = 0;

	pspDebugScreenInit();
	if (argc > 0) {
		printf("Bootpath: %s\n", argv[0]);
	}

    printf("Triangle - Exit\n");
    printf("Square - Set lower 16-bits\n");
    printf("Circle - Clear lower 16-bits\n");
    printf("Cross - Delay + refer status\n");
    printf("R-Trigger - Cancel event flag\n\n");
    printf("L-Trigger - Delete event flag\n\n");

	SetupCallbacks();

	sceCtrlSetSamplingCycle(0);
	sceCtrlSetSamplingMode(PSP_CTRL_MODE_ANALOG);

    evid = sceKernelCreateEventFlag("test_ef", PSP_EVENT_WAITMULTIPLE, INITIAL_PATTERN, 0);
    //evid = sceKernelCreateEventFlag("test_ef", 0, INITIAL_PATTERN, 0);
    printf("EVID: %08x pattern %08x\n", evid, INITIAL_PATTERN);
    if (evid >= 0)
    {
        createStartThread("and", and_thread);
        createStartThread("or", or_thread);
        createStartThread("clear", clear_thread);

#if 0
        // testing context switch timing
        sceKernelSetEventFlag(evid, 0x0000FFFF);
        //int buf[64]; for(;;) sceCtrlReadLatch(buf); // sceCtrlReadLatch does not context switch
        for(;;) sceKernelDelayThread(0); // does not wait forever
#else
        while(!done)
        {
            sceCtrlReadBufferPositive(&pad, 1); // context switch in here
            int buttonDown = (oldButtons ^ pad.Buttons) & pad.Buttons;

            if (buttonDown & PSP_CTRL_SQUARE)
            {
                result = sceKernelSetEventFlag(evid, 0x0000FFFF);
                printf("\nsceKernelSetEventFlag result %08x\n", result);
                printSceKernelEventFlagInfo(evid);
            }

            if (buttonDown & PSP_CTRL_CIRCLE)
            {
                //result = sceKernelClearEventFlag(evid, 0x0000FFFF); // bits to clear - bad
                result = sceKernelClearEventFlag(evid, 0xFFFF0000); // bits to keep - ok
                printf("\nsceKernelClearEventFlag result %08x\n", result);
                printSceKernelEventFlagInfo(evid);
            }

            if (buttonDown & PSP_CTRL_CROSS)
            {
                printf("\nsceKernelDelayThreadCB ...\n");
                sceKernelDelayThreadCB(1000);
                printSceKernelEventFlagInfo(evid);
            }

            if (buttonDown & PSP_CTRL_RTRIGGER)
            {
                //result = sceKernelCancelEventFlag(evid, newPattern, addr);
                result = sceKernelCancelEventFlag(evid, INITIAL_PATTERN, 0);
                printf("sceKernelCancelEventFlag result %08x\n", result);
                printSceKernelEventFlagInfo(evid);
            }

            if (buttonDown & PSP_CTRL_LTRIGGER)
            {
                //result = sceKernelCancelEventFlag(evid, newPattern, addr);
                result = sceKernelDeleteEventFlag(evid);
                printf("sceKernelDeleteEventFlag result %08x\n", result);
                evid = 0;
            }

            if (buttonDown & PSP_CTRL_TRIANGLE)
                done = 1;

            oldButtons = pad.Buttons;
            //sceKernelDelayThread(0); // ok, not infinite :)
        }
#endif
    }
    else
    {
        printf("sceKernelCreateEventFlag failed %08x\n", evid);
        sceKernelSleepThreadCB();
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

    //SceKernelThreadInfo info;
    //info.size = sizeof(SceKernelThreadInfo);
    //sceKernelReferThreadStatus (0, &info);
    //printf("thread '%-12s' sp %p w %08x START\n", info.name, info.stack, info.waitId);

	cbid = sceKernelCreateCallback("Exit Callback", exit_callback, NULL);
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

void createStartThread(const char *name, SceKernelThreadEntry entry)
{
	int thid = sceKernelCreateThread(name, entry, 0x20, 0x1000, 0, 0);
	if(thid >= 0)
	{
		if (sceKernelStartThread(thid, 0, 0) < 0)
            printf("Failed to start thread '%s'\n", name);
	}
    else
    {
        printf("Failed to create thread '%s'\n", name);
    }
}
