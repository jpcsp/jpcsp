
#include <pspkernel.h>
#include <pspdebug.h>
#include <pspctrl.h>
#include <pspdisplay.h>
#include <psputility.h>
#include <psppower.h>
#include <pspumd.h>

#include <sys/stat.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>

PSP_MODULE_INFO("waitType_test", 0, 1, 0);
PSP_MAIN_THREAD_ATTR(THREAD_ATTR_USER);

int sceUmdCancelWaitDriveStat();

int sceKernelCreateMutex(const char *name, int attr, int count, const void *option);
int sceKernelDeleteMutex(int uid);
int sceKernelLockMutex(int uid, int count, unsigned int *timeout);
int sceKernelCancelMutex(int uid);

int CallbackThread(SceSize args, void *argp);
int SetupCallbacks(void);
int createStartThread(const char *name, SceKernelThreadEntry entry);
int referThread(int thid);

/* Define printf, just to make typing easier */
#define printf	pspDebugScreenPrintf

#define MUTEX_FW 0 // for jpcsp
//#define MUTEX_FW 0x02070110

enum
{
    TEST_BEGIN,

    TEST_SLEEP_THREAD,      // waitType=1
    TEST_SUSPEND_THREAD,    // N/A
    TEST_DELAY_THREAD,      // 2
    TEST_THREAD_END,        // 9
    TEST_EVENT_FLAG,        // 4
    TEST_SEMAPHORE,         // 3
    TEST_IO,                // 4
    TEST_UMD,               // 4
    TEST_MSG_PIPE,          // 8
    TEST_MUTEX,             // 0xc
    TEST_VBLANK,            // 4? can't reliably be tested

    TEST_FINISH
};

int done = 0;
int work_thid;
int work_done = 0;
int work_idle = 0; // probably not needed
int test_status;
int test_started = 0, test_ended = 0; // use 2 flags to see if waitType changes during a wait, probably not needed
int mark = 0;
int evid = -1;
int semaid = -1;
int mppid = -1;
int mtxid = -1;

int waitthreadend_thread(SceSize args, void *argp)
{
    SceKernelThreadInfo info;
    info.size = sizeof(SceKernelThreadInfo);
    sceKernelReferThreadStatus(0, &info);
    printf("[%s] START\n", info.name);

    sceKernelDelayThread(1*1000*1000);

    printf("[%s] EXIT\n", info.name);
    sceKernelExitDeleteThread(0);
    return 0;
}

int work_thread(SceSize args, void *argp)
{
	int result;
    int waitthreadend_thid = -1;
    int fd = -1;
    SceInt64 io_res = -1;
    char mppbuf[16];
    unsigned int timeout;
    SceKernelThreadInfo info;

    info.size = sizeof(SceKernelThreadInfo);
    sceKernelReferThreadStatus(0, &info);

    printf("[%s] START\n", info.name);

    while(!work_done)
    {
        work_idle = 0;

        switch(test_status)
        {
            case TEST_SLEEP_THREAD:
                //if (!test_started)
                {
                    printf("[%s] sceKernelSleepThread...\n", info.name);
                    test_started = 1;
                    sceKernelSleepThread();
                    test_ended = 1;
                }
                break;

            /* not allowed to call sceKernelSuspendThread on yourself, error 80020197
            case TEST_SUSPEND_THREAD:
                printf("[%s] sceKernelSuspendThread\n", info.name);
                test_started = 1;
                result = sceKernelSuspendThread(work_thid);
                printf("[%s] sceKernelSuspendThread %08x\n", info.name, result);
                break;
            */

            case TEST_DELAY_THREAD:
                //if (!test_started)
                {
                    printf("[%s] sceKernelDelayThread...\n", info.name);
                    test_started = 1;
                    sceKernelDelayThread(10*16666);
                    test_ended = 1;
                }
                break;

            case TEST_THREAD_END:
                //if (!test_started)
                {
                    waitthreadend_thid = createStartThread("waitthreadend_thread", waitthreadend_thread);
                    printf("[%s] sceKernelWaitThreadEnd...\n", info.name);
                    test_started = 1;

                    // see if waitType changes during a wait
                    //timeout = 4*500*1000;
                    timeout = 1*500*1000;

                    result = sceKernelWaitThreadEnd(waitthreadend_thid, &timeout);
                    printf("[%s] sceKernelWaitThreadEnd %08x timeout:%d\n", info.name, result, timeout);
                    sceKernelDeleteThread(waitthreadend_thid);
                    waitthreadend_thid = -1;
                    test_ended = 1;
                }
                break;

            case TEST_EVENT_FLAG:
                //if (!test_started)
                {
                    evid = sceKernelCreateEventFlag("eventflag", 0, 0, 0);
                    printf("[%s] sceKernelWaitEventFlag...\n", info.name);
                    test_started = 1;
                    sceKernelWaitEventFlag(evid, 0xFFFFFFFF, PSP_EVENT_WAITOR, 0, 0);
                    sceKernelDeleteEventFlag(evid);
                    test_ended = 1;
                    evid = -1;
                }
                break;

            case TEST_SEMAPHORE:
                //if (!test_started)
                {
                    semaid = sceKernelCreateSema("sema", 0, 0, 1, 0);
                    printf("[%s] sceKernelWaitSema...\n", info.name);
                    test_started = 1;
                    sceKernelWaitSema(semaid, 1, 0);
                    sceKernelDeleteSema(semaid);
                    test_ended = 1;
                    semaid = -1;
                }
                break;

            case TEST_IO:
                //if (!test_started)
                {
                    test_started = 1;
                    fd = sceIoOpen("ms0:/tmp.bin", PSP_O_RDWR|PSP_O_CREAT, 0);
                    printf("[%s] sceIoWriteAsync(%08x)...\n", info.name, fd);
                    sceIoWriteAsync(fd, (void*)0x08800000, 0x00100000); // 1mb
                    sceIoWaitAsync(fd,  &io_res);
                    test_ended = 1;
                    sceIoClose(fd);
                    fd = -1;
                }
                break;

            case TEST_UMD:
                //if (!test_started)
                {
                    if (sceUmdCheckMedium(0))
                        printf("ERROR: remove UMD for this test\n");

                    printf("sceUmdWaitDriveStat...\n");
                    test_started = 1;
                    result = sceUmdWaitDriveStat(0x32);
                    //printf("sceUmdWaitDriveStat %08x\n", result);
                    test_ended = 1;
                }
                break;

            case TEST_MSG_PIPE:
                //if (!test_started)
                {
                    mppid = sceKernelCreateMsgPipe("MsgPipe", 2, 0, sizeof(mppbuf), 0);

#if 1
                    // test receive
                    printf("sceKernelReceiveMsgPipe...\n");
                    test_started = 1;
                    result = sceKernelReceiveMsgPipe(mppid, mppbuf, sizeof(mppbuf), 0, 0, 0);
                    //printf("sceKernelReceiveMsgPipe %08x\n", result);
                    test_ended = 1;
#else
                    // test send
                    // fill buffer so next call blocks
                    sceKernelSendMsgPipe(mppid, mppbuf, sizeof(mppbuf), 0, 0, 0);

                    printf("sceKernelSendMsgPipe...\n");
                    test_started = 1;
                    result = sceKernelSendMsgPipe(mppid, mppbuf, sizeof(mppbuf), 0, 0, 0);
                    //printf("sceKernelSendMsgPipe %08x\n", result);
                    test_ended = 1;
#endif

                    sceKernelDeleteMsgPipe(mppid);
                    mppid = -1;
                }
                break;

            case TEST_MUTEX:
                if (/*!test_started &&*/ mtxid >= 0 && sceKernelDevkitVersion() >= MUTEX_FW)
                {
                    printf("[%s] sceKernelLockMutex...\n", info.name);
                    test_started = 1;
                    result = sceKernelLockMutex(mtxid, 1, 0);
                    //printf("[%s] sceKernelLockMutex %08x\n", info.name, result);
                    test_ended = 1;
                }
                else
                {
                    sceKernelDelayThread(0);
                }
                break;

            case TEST_VBLANK:
                if (!test_started)
                {
                    test_started = 1;
                    printf("[%s] sceDisplayWaitVblankStart...\n", info.name);
                    sceDisplayWaitVblankStart();
                    printf("[%s] sceDisplayWaitVblankStart\n", info.name);
                    test_ended = 1;
                }
                break;

            default:
                work_idle = 1;
                sceKernelDelayThread(0);
        }
    }

    // child thread must end before before parent thread ends?
    // or it could have been crashing because the stack size was too small (0x1000)
    if (waitthreadend_thid > 0)
    {
        sceKernelWaitThreadEnd(waitthreadend_thid, 0);
        sceKernelDeleteThread(waitthreadend_thid);
    }

    printf("[%s] EXIT\n", info.name);
	return 0;
}

void nextTest()
{
    if (test_status != TEST_FINISH)
    {
        test_started = 0;
        test_ended = 0;
        mark = 0;
        test_status++;

        // skip this for now, it's unreliable and can make the app get stuck
        if (test_status == TEST_VBLANK)
            test_status++;
    }
}

int getWaitType()
{
    int waitType = -1;

    if (test_started /*&& !work_idle*/)
    {
        SceKernelThreadInfo info;
        info.size = sizeof(SceKernelThreadInfo);
        sceKernelReferThreadStatus(work_thid, &info);
        waitType = info.waitType;
    }

    return waitType;
}

int main(int argc, char *argv[])
{
    SceCtrlData pad;
    int result;
    int oldButtons = 0;
    int waitType;

    pspDebugScreenInit();
    if (argc > 0) {
        printf("Bootpath: %s\n", argv[0]);
    }

    printf("Triangle - Exit\n");
    printf("\n");

    SetupCallbacks();

    sceCtrlSetSamplingCycle(0);
    sceCtrlSetSamplingMode(PSP_CTRL_MODE_ANALOG);

    test_status = TEST_BEGIN;
    printf("main thid\n");
    work_thid = createStartThread("work_thread", work_thread);
    printf("main thid\n");

    referThread(0);
    referThread(work_thid);

    while(!done)
    {
        sceCtrlReadBufferPositive(&pad, 1); // context switch in here
        //sceCtrlPeekBufferPositive(&pad, 1); // no context switch in here
        int buttonDown = (oldButtons ^ pad.Buttons) & pad.Buttons;

        switch(test_status)
        {
            case TEST_BEGIN:
                nextTest();
                break;

            case TEST_SLEEP_THREAD:
                waitType = getWaitType();
                if (waitType >= 0)
                {
                    printf("TEST_SLEEP_THREAD waitType:%02x\n", waitType);
                    sceKernelWakeupThread(work_thid);
                    nextTest();

                    // special case, suspend test, can't call sceKernelSuspendThread on yourself
                    printf("sceKernelSuspendThread...\n");
                    test_started = 1;
                    mark = 0;
                    result = sceKernelSuspendThread(work_thid);
                    printf("sceKernelSuspendThread %08x\n", result);
                }
                break;

            case TEST_SUSPEND_THREAD:
                waitType = getWaitType();
                if (waitType >= 0)
                {
                    printf("TEST_SUSPEND_THREAD waitType:%02x\n", waitType);

                    if (!mark)
                    {
                        sceKernelResumeThread(work_thid);
                        test_ended = 1;
                        mark = 1;
                    }

                    if (test_ended)
                        nextTest();
                }
                break;

            case TEST_DELAY_THREAD:
                waitType = getWaitType();
                if (waitType >= 0)
                {
                    printf("TEST_DELAY_THREAD waitType:%02x\n", waitType);

                    if (test_ended)
                        nextTest();
                }
                break;

            case TEST_THREAD_END:
                waitType = getWaitType();
                if (waitType >= 0)
                {
                    printf("TEST_THREAD_END waitType:%02x\n", waitType);

                    if (test_ended)
                        nextTest();
                }
                break;

            case TEST_EVENT_FLAG:
                waitType = getWaitType();
                if (waitType >= 0)
                {
                    printf("TEST_EVENT_FLAG waitType:%02x\n", waitType);

                    if (!mark)
                    {
                        sceKernelSetEventFlag(evid, 0xFFFFFFFF);
                        mark = 1;
                    }

                    if (test_ended)
                        nextTest();
                }
                break;


            case TEST_SEMAPHORE:
                waitType = getWaitType();
                if (waitType >= 0)
                {
                    printf("TEST_SEMAPHORE waitType:%02x\n", waitType);

                    if (!mark)
                    {
                        sceKernelSignalSema(semaid, 1);
                        mark = 1;
                    }

                    if (test_ended)
                        nextTest();
                }
                break;

            case TEST_IO:
                waitType = getWaitType();
                if (waitType >= 0)
                {
                    printf("TEST_IO waitType:%02x\n", waitType);

                    if (test_ended)
                        nextTest();
                }
                break;

            case TEST_UMD:
                waitType = getWaitType();
                if (waitType >= 0)
                {
                    printf("TEST_UMD waitType:%02x\n", waitType);

                    if (!mark)
                    {
                        sceUmdCancelWaitDriveStat();
                        mark = 1;
                    }

                    if (test_ended)
                        nextTest();
                }
                break;

            case TEST_MSG_PIPE:
                waitType = getWaitType();
                if (waitType >= 0)
                {
                    printf("TEST_MSG_PIPE waitType:%02x\n", waitType);

                    if (!mark)
                    {
                        sceKernelCancelMsgPipe(mppid, 0, 0);
                        mark = 1;
                    }

                    if (test_ended)
                        nextTest();
                }
                break;

            case TEST_MUTEX:
                if (sceKernelDevkitVersion() >= MUTEX_FW)
                {
                    if (!mark)
                    {
                        mtxid = sceKernelCreateMutex("mutex", 0, 1, 0);
                        printf("sceKernelCreateMutex %08x\n", mtxid);
                        mark = 1;
                    }

                    waitType = getWaitType();
                    if (waitType >= 0)
                    {
                        printf("TEST_MUTEX waitType:%02x\n", waitType);

                        if (mark)
                        {
                            sceKernelCancelMutex(mtxid);
                            sceKernelDeleteMutex(mtxid);
                            test_ended = 1;
                            mtxid = -1;

                            nextTest();
                        }
                    }
                }
                else
                {
                    nextTest();
                }
                break;

            case TEST_VBLANK:
                waitType = getWaitType();
                if (waitType >= 0)
                {
                    printf("TEST_VBLANK waitType:%02x\n", waitType);
                }

                if (test_ended)
                    nextTest();
                break;

            default:
            case TEST_FINISH:
                //sceKernelTerminateDeleteThread(work_thid);
                work_done = 1;
                break;
        }

        if (buttonDown & PSP_CTRL_TRIANGLE)
        {
            done = 1;
            work_done = 1;
        }

        oldButtons = pad.Buttons;
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

int createStartThread(const char *name, SceKernelThreadEntry entry)
{
	int thid = sceKernelCreateThread(name, entry, 0x20, 0x2000, 0, 0);
	if(thid >= 0)
	{
		if (sceKernelStartThread(thid, 0, 0) < 0)
            printf("Failed to start thread '%s'\n", name);
	}
    else
    {
        printf("Failed to create thread '%s'\n", name);
    }
    return thid;
}

// returns the result of sceKernelReferThreadStatus
int referThread(int thid)
{
    int result;
    SceKernelThreadInfo info;
    memset(&info, 0, sizeof(info));
    info.size = sizeof(SceKernelThreadInfo);
    result = sceKernelReferThreadStatus(thid, &info);
    printf("[%s] refer pri:%02x status:%02x waitType:%02x\n", info.name, info.currentPriority, info.status, info.waitType);

    if (info.status & PSP_THREAD_RUNNING) printf("  RUNNING\n");
    if (info.status & PSP_THREAD_READY) printf("  READY\n");
    if (info.status & PSP_THREAD_WAITING) printf("  WAITING\n");
    if (info.status & PSP_THREAD_SUSPEND) printf("  SUSPEND\n");
    if (info.status & PSP_THREAD_STOPPED) printf("  STOPPED\n");
    if (info.status & PSP_THREAD_KILLED) printf("  KILLED\n");

    return result;
}
