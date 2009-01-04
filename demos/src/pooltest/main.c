
#include <pspkernel.h>
#include <pspdebug.h>
#include <pspctrl.h>
#include <pspdisplay.h>

#include <sys/stat.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>

PSP_MODULE_INFO("memory pool test", 0, 1, 0);
PSP_MAIN_THREAD_ATTR(THREAD_ATTR_USER);

int CallbackThread(SceSize args, void *argp);
int SetupCallbacks(void);

/* Define printf, just to make typing easier */
#define printf	pspDebugScreenPrintf

int done = 0;
int another_done = 0;

void printMem()
{
    int max, total;

    max = sceKernelMaxFreeMemSize();
    total = sceKernelTotalFreeMemSize();

    printf("  mem max %08x total %08x\n", max, total);
}

void *fpl_wait_alloc(const char *threadname, int fpl)
{
    int result;
    void *addr = (void*)-1;

    printf("[%s] sceKernelAllocateFpl wait alloc...\n", threadname);
    result = sceKernelAllocateFpl(fpl, &addr, 0x0);
    printf("[%s] sceKernelAllocateFpl result=%08x addr=%p\n", threadname, result, addr);
    printMem();

    if (result != 0)
        addr = 0;

    return addr;
}

void *fpl_alloc(int fpl)
{
    int result;
    unsigned int timeout = 1000000;
    void *addr = (void*)-1;

    result = sceKernelAllocateFpl(fpl, &addr, &timeout);
    printf("sceKernelAllocateFpl result=%08x addr=%p timeout=%d\n", result, addr, timeout);
    printMem();

    if (result != 0)
        addr = 0;

    return addr;
}

void *fpl_try_alloc(int fpl)
{
    int result;
    void *addr = (void*)-1;

    result = sceKernelTryAllocateFpl(fpl, &addr);
    printf("sceKernelTryAllocateFpl result=%08x addr=%p\n", result, addr);
    printMem();

    if (result != 0)
        addr = 0;

    return addr;
}

void fpl_refer(int fpl)
{
    int result;
    SceKernelFplInfo info;
    memset(&info, 0xEE, sizeof(info));
    result = sceKernelReferFplStatus(fpl, &info);

    if (result == 0)
    {
        printf("size %08x %d\n", info.size, info.size);
        printf("name '%s'\n", info.name);
        printf("attr %08x\n", info.attr);
        printf("blockSize %08x %d\n", info.blockSize, info.blockSize);
        printf("freeBlocks %d\n", info.freeBlocks);
        printf("numWaitThreads %d\n", info.numWaitThreads);
    }
    else
    {
        printf("sceKernelReferFplStatus %08x\n", result);
    }
}

// 200, 400, 800, 1000, 2000, 8000 not allowed
#define FPL_ATTR_UNKNOWN1 0x00000001
#define FPL_ATTR_UNKNOWN2 0x00000002
#define FPL_ATTR_UNKNOWN3 0x00000004
#define FPL_ATTR_UNKNOWN4 0x00000008
#define FPL_ATTR_UNKNOWN5 0x00000010
#define FPL_ATTR_UNKNOWN6 0x00000020
#define FPL_ATTR_UNKNOWN7 0x00000040
#define FPL_ATTR_UNKNOWN8 0x00000080
#define FPL_ATTR_UNKNOWN9 0x00000100 // unknown
#define FPL_ATTR_ADDR_HIGH 0x00004000 // hi-mem

int another_thread(SceSize args, void *argp)
{
    int fpl = *(int*)argp;
    void *addr = fpl_wait_alloc("another_thread", fpl);

    if (addr)
    {
        printf("[another_thread] alloc success %p\n", addr);
        sceKernelFreeFpl(fpl, addr);
    }
    else
    {
        printf("[another_thread] alloc failed\n");
    }

    another_done = 1;
    return 0;
}

void fpl_test()
{
    int result;
    int fpl;
    int attr = 0x80;

    //fpl = sceKernelCreateFpl("FPL", 2, attr, 0x1000, 1, 0x0);
    fpl = sceKernelCreateFpl("FPL", 2, attr, 0x1, 1, 0x0);
    //fpl = sceKernelCreateFpl("FPL", 2, attr, 0x4000000, 1, 0x0); // not enough free mem
    printf("sceKernelCreateFpl(attr=%08x) %08x\n", attr, fpl);
    if (fpl <= 0)
        return;

    printMem(); // 256-byte aligned consumption (may be enforced by pspsysmem, not threadman/fpl)
    fpl_refer(fpl);

    void *addr;
    void *addr2;

    addr = fpl_alloc(fpl);

    if (0)
    {
        // test multiple wait
        int thid = sceKernelCreateThread("another_thread", another_thread, 0x20, 0x4000, 0, 0);
        if (thid >= 0)
        {
            sceKernelStartThread(thid, 4, &fpl);
        }

        addr2 = fpl_wait_alloc("user_main", fpl);
    }
    else
    {
        addr2 = fpl_alloc(fpl); // 32-bit aligned
        fpl_try_alloc(fpl);
        //fpl_try_alloc(fpl);
    }
    fpl_refer(fpl);

    if (addr)
    {
        result = sceKernelFreeFpl(fpl, addr); // ok
        printf("sceKernelFreeFpl(addr) result=%08x\n", result);
        printMem();

        //result = sceKernelFreeFpl(fpl, addr); // illegal mem block
        //printf("sceKernelFreeFpl(addr) result=%08x\n", result);
        //printMem();
    }

    if (addr2)
    {
        result = sceKernelFreeFpl(fpl, addr2); // ok
        printf("sceKernelFreeFpl(addr2) result=%08x\n", result);
        printMem();
    }

    //result = sceKernelFreeFpl(fpl, (void*)0x08A00000); // illegal mem block
    //printf("sceKernelFreeFpl(0x08A00000) result=%08x\n", result);
    //printMem();

    result = sceKernelDeleteFpl(fpl); // ok
    printf("sceKernelDeleteFpl %08x\n", result);
    printMem();

    result = sceKernelDeleteFpl(fpl); // not found fpl
    printf("sceKernelDeleteFpl %08x\n", result);
    printMem();

    fpl_refer(fpl); // not found fpl
}

int main(int argc, char *argv[])
{
    SceCtrlData pad;
    //int result;
    int oldButtons = 0;

    pspDebugScreenInit();
    if (argc > 0) {
        printf("Bootpath: %s\n", argv[0]);
    }

    printf("Triangle - Exit\n");
    printf("\n");

    SetupCallbacks();

    printMem();

    fpl_test();

    sceCtrlSetSamplingCycle(0);
    sceCtrlSetSamplingMode(PSP_CTRL_MODE_ANALOG);

    while(!done)
    {
        sceCtrlReadBufferPositive(&pad, 1); // context switch in here
        //sceCtrlPeekBufferPositive(&pad, 1); // no context switch version
        int buttonDown = (oldButtons ^ pad.Buttons) & pad.Buttons;

        if (buttonDown & PSP_CTRL_TRIANGLE)
            done = 1;

        oldButtons = pad.Buttons;
        sceDisplayWaitVblankStartCB();
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
