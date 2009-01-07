
#include "pooltest.h"

PSP_MODULE_INFO("memory pool test", 0, 1, 0);
PSP_MAIN_THREAD_ATTR(THREAD_ATTR_USER);

int CallbackThread(SceSize args, void *argp);
int SetupCallbacks(void);

int done = 0;
int vpl_done = 0;

void printMem()
{
    int max, total;

    max = sceKernelMaxFreeMemSize();
    total = sceKernelTotalFreeMemSize();

    printf("  mem max %08x total %08x\n", max, total);
}


void *vpl_alloc(int vpl, int size)
{
    int result;
    unsigned int timeout = 1000000;
    void *addr = (void*)-1;

    result = sceKernelAllocateVpl(vpl, size, &addr, &timeout);
    printf("sceKernelAllocateVpl result=%08x addr=%p timeout=%d\n", result, addr, timeout);
    printMem();

    if (result != 0)
        addr = 0;
    else
        printf("unk1 %08x unk2 %08x\n", ((int*)addr)[-2], ((int*)addr)[-1]);

    return addr;
}

void *vpl_try_alloc(int vpl, int size)
{
    int result;
    void *addr = (void*)-1;

    result = sceKernelTryAllocateVpl(vpl, size, &addr);
    printf("sceKernelTryAllocateVpl result=%08x addr=%p\n", result, addr);
    printMem();

    if (result != 0)
        addr = 0;
    else
        printf("unk1 %08x unk2 %08x\n", ((int*)addr)[-2], ((int*)addr)[-1]);


    return addr;
}

void vpl_refer(int vpl)
{
    int result;
    SceKernelVplInfo info;
    memset(&info, 0xEE, sizeof(info));
    result = sceKernelReferVplStatus(vpl, &info);

    if (result == 0)
    {
        //printf("size     %08x %d\n", info.size, info.size); // 0x34/52
        //printf("name '%s'\n", info.name); // ok
        printf("attr     %08x\n", info.attr);
        printf("poolSize %08x %d\n", info.poolSize, info.poolSize);
        printf("freeSize %08x %d\n", info.freeSize, info.freeSize);
        printf("numWaitThreads %d\n", info.numWaitThreads);
    }
    else
    {
        printf("sceKernelReferVplStatus %08x\n", result);
    }
}

void vpl_test()
{
    int result;
    int vpl;

    //vpl = sceKernelCreateVpl("VPL", 2, 0x0, 0x3000 + 0x20 + 0x08 * 3, 0x0); // size is rounded up 8-byte aligned
    vpl = sceKernelCreateVpl("VPL", 2, 0x4000, 0x2000 + 0x20 + 0x08 * 2, 0x0); // size is rounded up 8-byte aligned
    printf("sceKernelCreateVpl %08x\n", vpl);
    if (vpl <= 0)
        return;

    printMem();

    // refer size is 32 bytes less than create size
    // create(0x2000) -> 0x1fe0 (pool and free)
    // 32 byte overhead (24-byte aligned to 32?)
    vpl_refer(vpl);

    void *addr1 = 0, *addr2 = 0, *addr3 = 0;

    // 8 byte alignment
    // 8 byte overhead per alloc
    // even if freeSize matches sceKernelAllocateVpl size param, 8 more bytes are needed for it to succeed
    // struct { void *header; int magic; } = (allocAddr - 0x8) = { firstAllocAddr - 0x20, 0x201 };
    addr1 = vpl_alloc(vpl, 0x1000);
    //vpl_refer(vpl);

    addr2 = vpl_try_alloc(vpl, 0x1000);
    //vpl_refer(vpl);

    addr3 = vpl_try_alloc(vpl, 0x1000);
    vpl_refer(vpl);

    //sceKernelFreeVpl(vpl, addr2);
    if (0)
    {
        struct
        {
            void *self1; // address "self - 1 byte", 0x881c507
            int size; // size (from sceKernelCreateVpl, 0x2028) - 8 bytes, 0x2020
            int unk1; // sceKernelCreateVpl(0x2028) -> 0x201, sceKernelCreateVpl(0x3038) -> 0x603
            void *upperBound; // approx, 0x881e520 - sceKernelCreateVpl(0x2028) -> diff 0x2018, sceKernelCreateVpl(0x3038) -> diff 0x3028
            void *unk3; // 0x881e520
            int unk4; // sceKernelCreateVpl(0x2028) -> 0x200, sceKernelCreateVpl(0x3038) -> 0x201
        } vplHeader;

        int *header = (int*)*((int*)addr1 - 2);
        int i;
        printf("header dump (int*)%p - 2 -> %p -> (header*)%p\n", addr1, ((int*)addr1 - 2), header);
        for (i = 0; i < 6; i += 2)
            printf("  %08x %08x\n", header[i + 0], header[i + 1]);
    }

    if (addr1)
    {
        result = sceKernelFreeVpl(vpl, addr1);
        printf("sceKernelFreeVpl(addr1) result=%08x\n", result);
        vpl_refer(vpl);
        //printMem();

        //result = sceKernelFreeVpl(vpl, addr1); // illegal mem block
        //printf("sceKernelFreeVpl(addr1) result=%08x\n", result);
        //printMem();
    }

    if (addr2)
    {
        result = sceKernelFreeVpl(vpl, addr2);
        printf("sceKernelFreeVpl(addr2) result=%08x\n", result);
        vpl_refer(vpl);
        //printMem();
    }

    if (addr3)
    {
        result = sceKernelFreeVpl(vpl, addr3);
        printf("sceKernelFreeVpl(addr2) result=%08x\n", result);
        //printMem();
    }

    //result = sceKernelFreeVpl(vpl, (void*)0x08A00000); // illegal mem block
    //printf("sceKernelFreeVpl(0x08A00000) result=%08x\n", result);
    //printMem();

    result = sceKernelDeleteVpl(vpl);
    printf("sceKernelDeleteVpl %08x\n", result);
    printMem();

    //result = sceKernelDeleteVpl(vpl); // not found vpool
    //printf("sceKernelDeleteVpl %08x\n", result);
    //printMem();

    //vpl_refer(vpl); // not found vpool
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

    //fpl_test();
    vpl_test();

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
