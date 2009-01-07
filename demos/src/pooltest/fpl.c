
#include "pooltest.h"

static int fpl_done = 0;

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

int fpl_thread(SceSize args, void *argp)
{
    int fpl = *(int*)argp;
    void *addr = fpl_wait_alloc("fpl_thread", fpl);

    if (addr)
    {
        printf("[fpl_thread] alloc success %p\n", addr);
        sceKernelFreeFpl(fpl, addr);
    }
    else
    {
        printf("[fpl_thread] alloc failed\n");
    }

    fpl_done = 1;
    return 0;
}

void fpl_test()
{
    int result;
    int fpl;
    int attr = 0x4000;

    //fpl = sceKernelCreateFpl("FPL", 2, attr, 0x1000, 1, 0x0);
    fpl = sceKernelCreateFpl("FPL", 2, attr, 0x1, 2, 0x0);
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
        int thid = sceKernelCreateThread("fpl_thread", fpl_thread, 0x20, 0x4000, 0, 0);
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
