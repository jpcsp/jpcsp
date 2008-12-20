// usage: place a copy of test.txt with some text in it in the same dir as the binary

#include <pspkernel.h>
#include <pspdebug.h>
#include <pspctrl.h>
#include <pspdisplay.h>
//#include <psputility.h>
//#include <psppower.h>
//#include <pspumd.h>

#include <sys/stat.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>

PSP_MODULE_INFO("io async test", 0, 1, 0);
PSP_MAIN_THREAD_ATTR(THREAD_ATTR_USER);

int CallbackThread(SceSize args, void *argp);
int SetupCallbacks(void);

void save_cwd(const char *bootpath, char *buf, int buflen);
void set_cwd(const char *cwd);

/* Define printf, just to make typing easier */
#define printf	pspDebugScreenPrintf

int done = 0;
int io_done = 0;

char g_cwd[256];
char g_read_buf[0x2F000];

// Burnout
void doubly_check_sync(int fd)
{
    int result;
    SceInt64 async;

    async = -1;
    result = sceIoPollAsync(fd, &async);
    printf("sceIoPollAsync result %08x async %08x (%lld)\n", result, (int)(async & 0xFFFFFFFF), async);

    if (result == 1)
    {
        async = -1;
        result = sceIoWaitAsyncCB(fd, &async);
        printf("sceIoWaitAsyncCB result %08x async %08x (%lld)\n", result, (int)(async & 0xFFFFFFFF), async);
    }
}

// TOE
void wait_frame_check_sync(int fd)
{
    int result;
    SceInt64 async;

    do
    {
        sceKernelDelayThreadCB(16666);

        async = -1;
        result = sceIoPollAsync(fd, &async);
        printf("sceIoPollAsync result %08x async %08x (%lld)\n", result, (int)(async & 0xFFFFFFFF), async);
    } while(result == 1);
}

void test_burnout()
{
    int result;
    int fd;

    printf("SIMULATING BURNOUT\n");

    fd = sceIoOpen("test.txt", PSP_O_RDONLY, 0);
    printf("sceIoOpen 0x%08x\n", fd);

    if (fd >= 0)
    {
        doubly_check_sync(fd);

        result = sceIoReadAsync(fd, g_read_buf, 0x2e2e2);
        printf("sceIoReadAsync result %08x\n", result);

        doubly_check_sync(fd);

        result = sceIoClose(fd);
        printf("sceIoClose result %08x\n", result);
    }
}

void test_toe()
{
    int result;
    int fd;

    printf("SIMULATING TOE\n");

    fd = sceIoOpenAsync("test.txt", PSP_O_RDONLY, 0);
    printf("sceIoOpenAsync 0x%08x\n", fd);

    if (fd >= 0)
    {
        wait_frame_check_sync(fd);

        result = sceIoReadAsync(fd, g_read_buf, 0x1428);
        printf("sceIoReadAsync result %08x\n", result);

        wait_frame_check_sync(fd);

        result = sceIoCloseAsync(fd);
        printf("sceIoCloseAsync result %08x\n", result);

        wait_frame_check_sync(fd);
    }
}

void test_pt2()
{
    int result;
    SceInt64 async;
    int fd;

    printf("SIMULATING PT2\n");

    fd = sceIoOpenAsync("test.txt", 0x4001, 0);
    printf("sceIoOpenAsync 0x%08x\n", fd);

    if (fd >= 0)
    {
        async = -1;
        result = sceIoPollAsync(fd, &async);
        printf("sceIoPollAsync result %08x async %08x (%lld)\n", result, (int)(async & 0xFFFFFFFF), async);

        async = -1;
        result = sceIoPollAsync(fd, &async);
        printf("sceIoPollAsync result %08x async %08x (%lld)\n", result, (int)(async & 0xFFFFFFFF), async);

        // extra
        result = sceIoCloseAsync(fd);
        printf("sceIoCloseAsync result %08x\n", result);
    }
}

void test_mercury()
{
    int result;
    SceInt64 async;
    int fd;

    printf("SIMULATING MERCURY\n");

    // for this test don't create this file! it's supposed to be missing
    // 644 = 01204 = 0x284
    fd = sceIoOpenAsync("filenotfound.txt", 1, 644);
    printf("sceIoOpenAsync 0x%08x\n", fd);

    if (fd >= 0)
    {
        async = -1;
        result = sceIoWaitAsyncCB(fd, &async); // result = 0x80010016
        printf("sceIoWaitAsyncCB result %08x async %08x (%lld)\n", result, (int)(async & 0xFFFFFFFF), async);

        result = sceIoLseekAsync(fd, 0x0, PSP_SEEK_END);
        printf("sceIoLseekAsync result %08x\n", result);

        async = -1;
        result = sceIoWaitAsyncCB(fd, &async);
        printf("sceIoWaitAsyncCB result %08x async %08x (%lld)\n", result, (int)(async & 0xFFFFFFFF), async);

        result = sceIoLseekAsync(fd, 0x0, PSP_SEEK_SET);
        printf("sceIoLseekAsync result %08x\n", result);

        async = -1;
        result = sceIoWaitAsyncCB(fd, &async);
        printf("sceIoWaitAsyncCB result %08x async %08x (%lld)\n", result, (int)(async & 0xFFFFFFFF), async);

        async = -1;
        result = sceIoWaitAsyncCB(fd, &async);
        printf("sceIoWaitAsyncCB result %08x async %08x (%lld)\n", result, (int)(async & 0xFFFFFFFF), async);

        // extra
        result = sceIoCloseAsync(fd);
        printf("sceIoCloseAsync result %08x\n", result);
    }
}

// see what happens to the async result when an async is op is followed by a non-async op
void test_asyncresult() {
    int result;
    SceInt64 async;
    int fd;

    fd = sceIoOpenAsync("test.txt", PSP_O_RDONLY, 0);
    printf("sceIoOpenAsync 0x%08x\n", fd);

    if (fd >= 0)
    {
        doubly_check_sync(fd);

        result = sceIoReadAsync(fd, g_read_buf, 0x1428);
        printf("sceIoReadAsync result %08x\n", result); // result = 0

        // wait for the async to finish WITHOUT using poll/wait async functions
        /* 10x 16666, still not enough waits...
        sceKernelDelayThreadCB(16666);
        sceKernelDelayThreadCB(16666);
        sceKernelDelayThreadCB(16666);
        sceKernelDelayThreadCB(16666);
        sceKernelDelayThreadCB(16666);
        sceKernelDelayThreadCB(16666);
        sceKernelDelayThreadCB(16666);
        sceKernelDelayThreadCB(16666);
        sceKernelDelayThreadCB(16666);
        sceKernelDelayThreadCB(16666);
        */

        result = sceIoReadAsync(fd, g_read_buf, 0x1428);
        printf("sceIoReadAsync result %08x\n", result); // result = pending

        result = sceIoRead(fd, g_read_buf, 0x1428);
        printf("sceIoRead result %08x\n", result); // result = pending

        async = -1;
        result = sceIoPollAsync(fd, &async);
        printf("sceIoPollAsync result %08x async %08x (%lld)\n", result, (int)(async & 0xFFFFFFFF), async);

        result = sceIoClose(fd);
        printf("sceIoClose result %08x\n", result);
    }
}

//int sceIoSetAsyncCallback(int fd, int cbid, void *unk1);

char callback_buf[16];
int callback_done;
int callback_arg1;
int callback_arg2;
void *callback_common;

int async_callback(int arg1, int arg2, void *common)
{
    callback_arg1 = arg1;
    callback_arg2 = arg2;
    callback_common = common;
    callback_done = 1;
    return 0;
}

void test_callback() {
    int result;
    SceInt64 async;
    SceInt64 *async2;
    int fd;

    int cbid = sceKernelCreateCallback("Async Callback", async_callback, (void*)0x12121212);

    fd = sceIoOpenAsync("test.txt", PSP_O_RDONLY, 0);
    printf("sceIoOpenAsync 0x%08x\n", fd);

    if (fd >= 0)
    {
        result = sceIoSetAsyncCallback(fd, cbid, callback_buf);
        printf("sceIoSetAsyncCallback buf=%p 0x%08x\n", callback_buf, result);

        memset(callback_buf, 0xee, sizeof(callback_buf));
        callback_done = 0;

        async = -1;
        result = sceIoWaitAsyncCB(fd, &async);
        printf("sceIoWaitAsyncCB result %08x async %08x (%lld)\n", result, (int)(async & 0xFFFFFFFF), async);

        if (callback_done)
        {
            printf("callback arg1 %08x\n", callback_arg1);
            printf("callback arg2 %08x\n", callback_arg2);
            printf("callback common %p\n", callback_common);

            async2 = (SceInt64*)callback_buf;
            printf("callback async %08x (%lld)\n", (int)(*async2 & 0xFFFFFFFF), *async2);
        }


        memset(callback_buf, 0xee, sizeof(callback_buf));
        callback_done = 0;

        result = sceIoReadAsync(fd, g_read_buf, 0x100);
        printf("sceIoReadAsync result %08x\n", result);

        sceKernelDelayThreadCB(1000000);

        if (callback_done)
        {
            printf("callback arg1 %08x\n", callback_arg1);
            printf("callback arg2 %08x\n", callback_arg2);
            printf("callback common %p\n", callback_common);

            async2 = (SceInt64*)callback_buf;
            printf("callback async %08x (%lld)\n", (int)(*async2 & 0xFFFFFFFF), *async2);
        }

        result = sceIoClose(fd);
        printf("sceIoClose result %08x\n", result);
    }
}

void test_general()
{
    int result;
    SceInt64 async;
    int fd;

    // perm = 0x777 or 0x0, doesn't matter
    //fd = sceIoOpen("test.txt", PSP_O_RDONLY, 0);
    fd = sceIoOpenAsync("test.txt", PSP_O_RDONLY, 0);
    printf("sceIoOpenAsync 0x%08x\n", fd);

    if (fd >= 0)
    {
        //sceKernelDelayThread(1000);
        sceKernelDelayThreadCB(16666);

        async = -1;
        result = sceIoPollAsync(fd, &async);
        printf("sceIoPollAsync result %08x async %08x (%lld)\n", result, (int)(async & 0xFFFFFFFF), async);

        async = -1;
        result = sceIoWaitAsyncCB(fd, &async);
        printf("sceIoWaitAsyncCB result %08x async %08x (%lld)\n", result, (int)(async & 0xFFFFFFFF), async);

        async = -1;
        result = sceIoPollAsync(fd, &async);
        printf("sceIoPollAsync result %08x async %08x (%lld)\n", result, (int)(async & 0xFFFFFFFF), async);

        result = sceIoCloseAsync(fd);
        printf("sceIoCloseAsync result %08x\n", result);

        sceKernelDelayThreadCB(16666);

        async = -1;
        result = sceIoPollAsync(fd, &async);
        printf("sceIoPollAsync result %08x async %08x (%lld)\n", result, (int)(async & 0xFFFFFFFF), async);

        sceKernelDelayThreadCB(16666);

        async = -1;
        result = sceIoPollAsync(fd, &async);
        printf("sceIoPollAsync result %08x async %08x (%lld)\n", result, (int)(async & 0xFFFFFFFF), async);
    }
}

int io_thread(SceSize args, void *argp)
{
    set_cwd(g_cwd);

    //test_burnout();
    //test_toe();
    //test_pt2();
    //test_mercury();
    //test_asyncresult();
    test_callback();
    //test_general();

    io_done = 1;
    return 0;
}

int main(int argc, char *argv[])
{
    SceCtrlData pad;
    int result;
    int oldButtons = 0;

    pspDebugScreenInit();
    if (argc > 0) {
        printf("Bootpath: %s\n", argv[0]);
        save_cwd(argv[0], g_cwd, sizeof(g_cwd));
    }

    printf("Triangle - Exit\n");
    printf("Cross - Check io_thread status\n");
    printf("Circle - Delete io_thread\n");
    printf("\n");

    SetupCallbacks();

    int thid = sceKernelCreateThread("io_thread", io_thread, 0x6f, 0x4000, 0, 0);
    if (thid >= 0)
    {
        sceKernelStartThread(thid, 0, 0x0);
        //set_cwd(argv[0], thid);

        /*
        result = sceKernelWaitThreadEnd(thid, 0);
        printf("[user_main] sceKernelWaitThreadEndCB result %08x\n", result);

        result = sceKernelWaitThreadEnd(thid, 0);
        printf("[user_main] sceKernelWaitThreadEndCB result %08x\n", result);

        result = sceKernelDeleteThread(thid);
        printf("[user_main] sceKernelDeleteThread result %08x\n", result);
        */
    }

    sceCtrlSetSamplingCycle(0);
    sceCtrlSetSamplingMode(PSP_CTRL_MODE_ANALOG);

    while(!done)
    {
        sceCtrlReadBufferPositive(&pad, 1); // context switch in here
        //sceCtrlPeekBufferPositive(&pad, 1); // no context switch version
        int buttonDown = (oldButtons ^ pad.Buttons) & pad.Buttons;

        if (buttonDown & PSP_CTRL_TRIANGLE)
            done = 1;

        if ((buttonDown & PSP_CTRL_CROSS) ||
            io_done == 1)
        {
            SceKernelThreadInfo info;
            memset(&info, 0, sizeof(SceKernelThreadInfo));
            info.size = sizeof(SceKernelThreadInfo);
            result = sceKernelReferThreadStatus(thid, &info);

            if (io_done == 1)
            {
                printf("[user_main] io done\n");
                io_done = 2;
            }

            printf("[user_main] refer io thread result:%08x status:%02x exitStatus:%02x\n", result, info.status, info.exitStatus);
        }

        if (buttonDown & PSP_CTRL_CIRCLE)
        {
            result = sceKernelDeleteThread(thid);
            printf("[user_main] sceKernelDeleteThread result %08x\n", result);
        }


        oldButtons = pad.Buttons;
        sceDisplayWaitVblankStartCB();
    }

    sceKernelExitGame();
    return 0;
}

void save_cwd(const char *bootpath, char *buf, int buflen)
{
    char *t;

    strncpy(buf, bootpath, buflen);
    buf[buflen - 1] = 0;
    t = strrchr(buf, '/');
    if (t) *t = 0;
}

void set_cwd(const char *cwd)
{
    int result;
    int uid = sceIoDopen(cwd);
    //printf("sceIoDopen uid 0x%08x\n", uid);
    if (uid >= 0)
    {
        result = sceIoDclose(uid);
        //printf("sceIoDclose result 0x%08x\n", result);
        result = sceIoChdir(cwd);
        printf("sceIoChdir(%s) result 0x%08x\n", cwd, result);

        // kernel mode
        //if (thid > 0)
        //{
        //    result = sceIoChangeThreadCwd(0, cwd);
        //    printf("sceIoChangeThreadCwd(%s) result 0x%08x\n", cwd, result);
        //}
    }
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

    cbid = sceKernelCreateCallback("Exit Callback", exit_callback, 0);
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
