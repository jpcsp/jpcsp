#include <pspkernel.h>
#include <pspdebug.h>
#include <pspctrl.h>
#include <pspdisplay.h>
#include <pspgu.h>
#include <pspgum.h>
#include <pspsdk.h>

#include <stdio.h>
#include <string.h>
#include <assert.h>
#include "DmacManForKernel.h"
#include "sceDdr.h"

PSP_MODULE_INFO("DmacManForKernel Test", PSP_MODULE_KERNEL, 1, 0);
PSP_MAIN_THREAD_ATTR(PSP_THREAD_ATTR_VFPU);

#define K1_BASE 0xA0000000 /* uncached - kernel */
#define KUNCACHED(ptr) (void *)(K1_BASE | ((u32)(void *)(ptr) & 0x1FFFFFFF)) /* K1 - uncached */

#define LW(addr) (*((u32 *)(addr)))
#define SW(value, addr)   (*((volatile u32*) (addr)) = (u32) (value))

volatile int done = 0;
volatile int interruptHandlerCalled = 0;
volatile int callbackCalled = 0;
volatile int callbackArg0;
volatile int callbackArg1;
volatile int callbackDisplayDmacRegisters = 0;
volatile u32 systemTimeStart;
volatile u32 systemTimeEnd;
u8 __attribute__((aligned(64))) srcBuffer[32768];
u8 __attribute__((aligned(64))) dstBuffer[32768];
u8 *inputBuffer = NULL;
int inputBufferSize = 0;
int inputBufferIndex = 0;
int fileLength = 0;
SceUID partitionId = -1;
int sceCodec_driver_376399B6(int busy);

#define SYSTEM_TIME LW(0xBC600000)
#define AUDIO_SET_BUSY(busy) SW(busy, 0xBE000000)

/* Exit callback */
int exit_callback(int arg1, int arg2, void *common) {
	done = 1;
	sceKernelExitGame();
	return 0;
}

/* Callback thread */
int CallbackThread(SceSize args, void *argp) {
	int cbid;

	cbid = sceKernelCreateCallback("Exit Callback", exit_callback, (void*)0);
	sceKernelRegisterExitCallback(cbid);

	sceKernelSleepThreadCB();

	return 0;
}

/* Sets up the callback thread and returns its thread id */
int SetupCallbacks(void) {
	int thid = 0;

	thid = sceKernelCreateThread("CallbackThread", CallbackThread, 0x11, 0xFA0, 0, 0);
	if(thid >= 0) {
		sceKernelStartThread(thid, 0, 0);
	}

	return thid;
}

void setAttribute(DmaOperationLink *dmaOp, int length, int srcStep, int dstStep, int srcLengthShift, int dstLengthShift, int srcIncrement, int dstIncrement, int last) {
	dmaOp->attributes  = 0;
	dmaOp->attributes |= (length >> srcLengthShift) & 0xFFF;
	dmaOp->attributes |= (srcStep & 0x7) << 12;
	dmaOp->attributes |= (dstStep & 0x7) << 15;
	dmaOp->attributes |= (srcLengthShift & 0x7) << 18;
	dmaOp->attributes |= (dstLengthShift & 0x7) << 21;
	if (srcIncrement) {
		dmaOp->attributes |= 0x04000000;
	}
	if (dstIncrement) {
		dmaOp->attributes |= 0x08000000;
	}
	if (last) {
		dmaOp->attributes |= 0x80000000;
	}
}

void init() {
	int i;

	u8 *ptr = KUNCACHED(srcBuffer);
	memset(ptr, 0, sizeof(srcBuffer));
	for (i = 0; i < sizeof(srcBuffer); i++) {
		ptr[i] = (u8) (i + 1);
	}

	memset(KUNCACHED(dstBuffer), 0, sizeof(dstBuffer));

	callbackCalled = 0;
	callbackArg0 = 0;
	callbackArg1 = 0;
	callbackDisplayDmacRegisters = 0;
}

void printBuffersWithOffset(int offset, int length) {
	int i;

	u8 *ptr = KUNCACHED(dstBuffer);
	for (i = 0; i < length; i++) {
		if (ptr[offset + i] != srcBuffer[offset + i]) {
			pspDebugScreenPrintf("0x%X bytes copied from srcBuffer to dstBuffer\n", i);
			break;
		}
	}

	if (i == length) {
		pspDebugScreenPrintf("Successfully copied 0x%X bytes\n", i);
	}

	for (; i < length; i++) {
		if (ptr[offset + i] != 0) {
			pspDebugScreenPrintf("dstBuffer[0x%03X]=0x%02X\n", offset + i, ptr[offset + i]);
		}
	}

	if (offset == 0) {
		if (callbackCalled) {
			pspDebugScreenPrintf("dmaCallback(0x%08X, %d) called %d times\n", callbackArg0, callbackArg1, callbackCalled);
		}
	}
}

void printBuffers() {
	printBuffersWithOffset(0, sizeof(dstBuffer));
}

int dmaCallback(int unknown1, int error) {
	systemTimeEnd = SYSTEM_TIME;
	callbackArg0 = unknown1;
	callbackArg1 = error;
	callbackCalled++;

	if (callbackDisplayDmacRegisters) {
		pspDebugScreenPrintf("Callback 0xBC900100 = 0x%08X\n", LW(0xBC900100));
		pspDebugScreenPrintf("Callback 0xBC900104 = 0x%08X\n", LW(0xBC900104));
		pspDebugScreenPrintf("Callback 0xBC900108 = 0x%08X\n", LW(0xBC900108));
		pspDebugScreenPrintf("Callback 0xBC90010C = 0x%08X\n", LW(0xBC90010C));
		pspDebugScreenPrintf("Callback 0xBC900110 = 0x%08X\n", LW(0xBC900110));
	}

	return 0;
}

int doTest(DmaOperation *dmaOp, int length, int srcStep, int dstStep, int srcLengthShift, int dstLengthShift, int srcIncrement, int dstIncrement, int last) {
	DmaOperationLink dmaOperationLink;
	int result;

	init();
	dmaOperationLink.src = DMACPTR(srcBuffer);
	dmaOperationLink.dst = DMACPTR(dstBuffer);
	dmaOperationLink.next = NULL;
	setAttribute(&dmaOperationLink, length, srcStep, dstStep, srcLengthShift, dstLengthShift, srcIncrement, dstIncrement, last);

	result = sceKernelDmaOpAssign(dmaOp, -1, -1, 0);
	if (result != 0) {
		pspDebugScreenPrintf("sceKernelDmaOpAssign = 0x%08X\n", result);
		return result;
	}

	result = sceKernelDmaOpSetupLink(dmaOp, 0x0000C001, (DmaOperationLink *) (((u32) &dmaOperationLink) + 1));
	if (result != 0) {
		pspDebugScreenPrintf("sceKernelDmaOpSetupLink = 0x%08X\n", result);
		return result;
	}

	result = sceKernelDmaOpEnQueue(dmaOp);
	if (result != 0) {
		pspDebugScreenPrintf("sceKernelDmaOpEnQueue = 0x%08X\n", result);
		return result;
	}

	result = sceKernelDmaOpSync(dmaOp, 2, 100000);
	if (result != 0) {
		pspDebugScreenPrintf("sceKernelDmaOpSync = 0x%08X\n", result);
		return result;
	}

	printBuffers();

	result = sceKernelDmaOpQuit(dmaOp);
	result = sceKernelDmaOpDeQueue(dmaOp);

	return result;
}

void runTest1() {
	int result;
	int i;
	DmaOperationLink dmaOperationLink;

	init();
	dmaOperationLink.src = DMACPTR(srcBuffer);
	dmaOperationLink.dst = DMACPTR(dstBuffer);
	dmaOperationLink.next = NULL;
	setAttribute(&dmaOperationLink, 16, 0, 0, 1, 1, 1, 1, 1);
	setAttribute(&dmaOperationLink, 16, 7, 7, 2, 2, 1, 1, 1);
	//pspDebugScreenPrintf("dmaOperationLink.src  = 0x%08X\n", (u32) dmaOperationLink.src);
	//pspDebugScreenPrintf("dmaOperationLink.dst  = 0x%08X\n", (u32) dmaOperationLink.dst);
	//pspDebugScreenPrintf("dmaOperationLink.next = 0x%08X\n", (u32) dmaOperationLink.next);
	//pspDebugScreenPrintf("dmaOperationLink.attributes = 0x%08X\n", dmaOperationLink.attributes);

	DmaOperation *dmaOp = sceKernelDmaOpAlloc();
	pspDebugScreenPrintf("sceKernelDmaOpAlloc = 0x%08X\n", (int) dmaOp);

	result = sceKernelDmaOpAssign(dmaOp, -1, -1, 0);
	if (result != 0) {
		pspDebugScreenPrintf("sceKernelDmaOpAssign = 0x%08X\n", result);
	}

	result = sceKernelDmaOpSetCallback(dmaOp, dmaCallback, 0);
	if (result != 0) {
		pspDebugScreenPrintf("sceKernelDmaOpSetCallback = 0x%08X\n", result);
	}

	result = sceKernelDmaOpSetupLink(dmaOp, 0x0100C003, (DmaOperationLink *) (((u32) &dmaOperationLink) + 1));
	if (result != 0) {
		pspDebugScreenPrintf("sceKernelDmaOpSetupLink = 0x%08X\n", result);
	}

	result = sceKernelDmaOpEnQueue(dmaOp);
	if (result != 0) {
		pspDebugScreenPrintf("sceKernelDmaOpEnQueue = 0x%08X\n", result);
	}

	for (i = 0; i < 100 && !callbackCalled; i++) {
		sceKernelDelayThread(10000);
	}

	if (callbackCalled) {
		pspDebugScreenPrintf("dmaCallback(0x%08X, %d)\n", callbackArg0, callbackArg1);
	} else {
		pspDebugScreenPrintf("dmaCallback not called after 1 second!\n");
	}
	printBuffers();

	result = sceKernelDmaOpSync(dmaOp, 0, 0);
	if (result != 0) {
		pspDebugScreenPrintf("sceKernelDmaOpSync = 0x%08X\n", result);
	}

	if (result < 0) {
		result = sceKernelDmaOpQuit(dmaOp);
		if (result != 0) {
			pspDebugScreenPrintf("sceKernelDmaOpQuit = 0x%08X\n", result);
		}

		result = sceKernelDmaOpDeQueue(dmaOp);
		if (result != 0) {
			pspDebugScreenPrintf("sceKernelDmaOpDeQueue = 0x%08X\n", result);
		}
	}

	result = sceKernelDmaOpFree(dmaOp);
	if (result != 0) {
		pspDebugScreenPrintf("sceKernelDmaOpFree = 0x%08X\n", result);
	}
}

void runTest2() {
	int result;

	DmaOperation *dmaOp = sceKernelDmaOpAlloc();

	result = sceKernelDmaOpAssign(dmaOp, 0xC0, 0xC0, 0);
	if (result != 0) {
		pspDebugScreenPrintf("sceKernelDmaOpAssign = 0x%08X\n", result);
	}

	result = sceKernelDmaOpSetupMemcpy(dmaOp, DMACPTR(dstBuffer), DMACPTR(srcBuffer), 16 >> 2);
	if (result != 0) {
		pspDebugScreenPrintf("sceKernelDmaOpSetupMemcpy = 0x%08X\n", result);
	}

	result = sceKernelDmaOpEnQueue(dmaOp);
	if (result != 0) {
		pspDebugScreenPrintf("sceKernelDmaOpEnQueue = 0x%08X\n", result);
	}

	result = sceKernelDmaOpSync(dmaOp, 1, 0);
	if (result != 0) {
		pspDebugScreenPrintf("sceKernelDmaOpSync = 0x%08X\n", result);
	}

	pspDebugScreenPrintf("Test sceKernelDmaOpSetupMemcpy: ");
	printBuffers();

	result = sceKernelDmaOpFree(dmaOp);
	if (result != 0) {
		pspDebugScreenPrintf("sceKernelDmaOpFree = 0x%08X\n", result);
	}
}

void runTest3() {
	int i;

	DmaOperation *dmaOp = sceKernelDmaOpAlloc();
	for (i = 0; i <= 7; i++) {
		pspDebugScreenPrintf("step=%d: ", i);
		doTest(dmaOp, 64, i, i, 2, 2, 1, 1, 1);
	}
	sceKernelDmaOpFree(dmaOp);
}

void runTest4() {
	DmaOperation *dmaOp = sceKernelDmaOpAlloc();
	pspDebugScreenPrintf("No srcIncrement: ");
	doTest(dmaOp, 8, 0, 0, 2, 2, 0, 1, 1);

	pspDebugScreenPrintf("No dstIncrement: ");
	doTest(dmaOp, 8, 0, 0, 2, 2, 1, 0, 1);

	pspDebugScreenPrintf("No srcIncrement and dstIncrement: ");
	doTest(dmaOp, 8, 0, 0, 2, 2, 0, 0, 1);

	sceKernelDmaOpFree(dmaOp);
}

void runTest5() {
	int result;
	DmaOperationLink dmaOperationLink1;
	DmaOperationLink dmaOperationLink2;
	DmaOperationLink dmaOperationLink3;

	init();
	dmaOperationLink1.src = DMACPTR(srcBuffer);
	dmaOperationLink1.dst = DMACPTR(dstBuffer);
	dmaOperationLink1.next = DMACPTR(&dmaOperationLink2);
	setAttribute(&dmaOperationLink1, 16, 7, 7, 2, 2, 1, 1, 0);
	sceKernelDcacheWritebackRange(&dmaOperationLink1, sizeof(dmaOperationLink1));

	dmaOperationLink2.src = DMACPTR(srcBuffer + 16);
	dmaOperationLink2.dst = DMACPTR(dstBuffer + 16);
	dmaOperationLink2.next = DMACPTR(&dmaOperationLink3);
	setAttribute(&dmaOperationLink2, 16, 6, 6, 2, 2, 1, 1, 1);
	sceKernelDcacheWritebackRange(&dmaOperationLink2, sizeof(dmaOperationLink2));

	dmaOperationLink3.src = DMACPTR(srcBuffer + 32);
	dmaOperationLink3.dst = DMACPTR(dstBuffer + 32);
	dmaOperationLink3.next = NULL;
	setAttribute(&dmaOperationLink3, 16, 5, 5, 2, 2, 1, 1, 0);
	sceKernelDcacheWritebackRange(&dmaOperationLink3, sizeof(dmaOperationLink3));

	pspDebugScreenPrintf("srcBuffer = 0x%08X\n", (u32) srcBuffer);
	pspDebugScreenPrintf("dstBuffer = 0x%08X\n", (u32) dstBuffer);
	pspDebugScreenPrintf("dmaOperationLink1 = 0x%08X\n", (u32) &dmaOperationLink1);
	pspDebugScreenPrintf("dmaOperationLink2 = 0x%08X\n", (u32) &dmaOperationLink2);
	pspDebugScreenPrintf("dmaOperationLink3 = 0x%08X\n", (u32) &dmaOperationLink3);
	pspDebugScreenPrintf("dmaOperationLink1.attributes = 0x%08X\n", dmaOperationLink1.attributes);
	pspDebugScreenPrintf("dmaOperationLink2.attributes = 0x%08X\n", dmaOperationLink2.attributes);
	pspDebugScreenPrintf("dmaOperationLink3.attributes = 0x%08X\n", dmaOperationLink3.attributes);
	callbackDisplayDmacRegisters = 1;

	DmaOperation *dmaOp = sceKernelDmaOpAlloc();

	result = sceKernelDmaOpAssign(dmaOp, -1, -1, 0);
	if (result != 0) {
		pspDebugScreenPrintf("sceKernelDmaOpAssign = 0x%08X\n", result);
	}

	result = sceKernelDmaOpSetCallback(dmaOp, dmaCallback, 0);
	if (result != 0) {
		pspDebugScreenPrintf("sceKernelDmaOpSetCallback = 0x%08X\n", result);
	}

	result = sceKernelDmaOpSetupLink(dmaOp, 0x0100C001, (DmaOperationLink *) (((u32) &dmaOperationLink1) + 1));
	if (result != 0) {
		pspDebugScreenPrintf("sceKernelDmaOpSetupLink = 0x%08X\n", result);
	}

	result = sceKernelDmaOpEnQueue(dmaOp);
	if (result != 0) {
		pspDebugScreenPrintf("sceKernelDmaOpEnQueue = 0x%08X\n", result);
	}

	result = sceKernelDmaOpSync(dmaOp, 2, 100000);
	if (result != 0) {
		pspDebugScreenPrintf("sceKernelDmaOpSync = 0x%08X\n", result);
	}
	printBuffers();

	if (result < 0) {
		result = sceKernelDmaOpQuit(dmaOp);
		if (result != 0) {
			pspDebugScreenPrintf("sceKernelDmaOpQuit = 0x%08X\n", result);
		}

		result = sceKernelDmaOpDeQueue(dmaOp);
		if (result != 0) {
			pspDebugScreenPrintf("sceKernelDmaOpDeQueue = 0x%08X\n", result);
		}
	}

	result = sceKernelDmaOpFree(dmaOp);
	if (result != 0) {
		pspDebugScreenPrintf("sceKernelDmaOpFree = 0x%08X\n", result);
	}
}

void runTest6() {
	int result;
	DmaOperationLink dmaOperationLink1;
	DmaOperationLink dmaOperationLink2;
	DmaOperationLink dmaOperationLink3;
	DmaOperationLink dmaOperationLink4;

	init();
	dmaOperationLink1.src = DMACPTR(srcBuffer);
	dmaOperationLink1.dst = DMACPTR(dstBuffer);
	dmaOperationLink1.next = DMACPTR(&dmaOperationLink2);
	setAttribute(&dmaOperationLink1, 16, 7, 7, 2, 2, 1, 1, 0);
	sceKernelDcacheWritebackRange(&dmaOperationLink1, sizeof(dmaOperationLink1));

	dmaOperationLink2.src = DMACPTR(srcBuffer + 32);
	dmaOperationLink2.dst = DMACPTR(dstBuffer + 32);
	dmaOperationLink2.next = NULL;
	setAttribute(&dmaOperationLink2, 16, 7, 7, 2, 2, 1, 1, 1);
	sceKernelDcacheWritebackRange(&dmaOperationLink2, sizeof(dmaOperationLink2));

	dmaOperationLink3.src = DMACPTR(srcBuffer + 64);
	dmaOperationLink3.dst = DMACPTR(dstBuffer + 64);
	dmaOperationLink3.next = DMACPTR(&dmaOperationLink4);
	setAttribute(&dmaOperationLink3, 16, 7, 7, 2, 2, 1, 1, 0);
	sceKernelDcacheWritebackRange(&dmaOperationLink3, sizeof(dmaOperationLink3));

	dmaOperationLink4.src = DMACPTR(srcBuffer + 96);
	dmaOperationLink4.dst = DMACPTR(dstBuffer + 96);
	dmaOperationLink4.next = NULL;
	setAttribute(&dmaOperationLink4, 16, 7, 7, 2, 2, 1, 1, 1);
	sceKernelDcacheWritebackRange(&dmaOperationLink4, sizeof(dmaOperationLink4));

	AUDIO_SET_BUSY(1);
	sceCodec_driver_376399B6(1);
	SW(0x0, 0xBE000004);
	SW(0x6, 0xBE000008);
	SW(0x1, 0xBE00002C);
	SW(0x1, 0xBE000020);

	DmaOperation *dmaOp = sceKernelDmaOpAlloc();

	int flags = 0x100C941;
	result = sceKernelDmaOpAssign(dmaOp, -1, -1, flags);
	if (result != 0) {
		pspDebugScreenPrintf("sceKernelDmaOpAssign = 0x%08X\n", result);
	}

	result = sceKernelDmaOpSetCallback(dmaOp, dmaCallback, 0);
	if (result != 0) {
		pspDebugScreenPrintf("sceKernelDmaOpSetCallback = 0x%08X\n", result);
	}

	result = sceKernelDmaOpSetupLink(dmaOp, flags, &dmaOperationLink1);
	if (result != 0) {
		pspDebugScreenPrintf("sceKernelDmaOpSetupLink = 0x%08X\n", result);
	}

	result = sceKernelDmaOpEnQueue(dmaOp);
	if (result != 0) {
		pspDebugScreenPrintf("sceKernelDmaOpEnQueue = 0x%08X\n", result);
	}

	SW(0x7, 0xBE000008);
	SW(0x1, 0xBE000004);
	SW(0x1, 0xBE000010);
	SW(0x1, 0xBE000024);

	sceKernelDelayThread(1000);

	pspDebugScreenPrintf("Before sceDdrFlush(4): ");
	printBuffersWithOffset(0, 32);
	printBuffersWithOffset(32, 32);

	dmaOperationLink2.next = DMACPTR(&dmaOperationLink3);
	sceKernelDcacheWritebackRange(&dmaOperationLink2, sizeof(dmaOperationLink2));
	systemTimeStart = SYSTEM_TIME;
	systemTimeEnd = SYSTEM_TIME;
	sceDdrFlush(4);

	sceKernelDelayThread(1000);

	pspDebugScreenPrintf("Duration=%d microseconds\n", systemTimeEnd - systemTimeStart);
	pspDebugScreenPrintf("After sceDdrFlush(4): ");
	printBuffersWithOffset(0, 32);
	printBuffersWithOffset(32, 32);
	printBuffersWithOffset(64, 32);
	printBuffersWithOffset(96, 32);

	result = sceKernelDmaOpSync(dmaOp, 0, 0);
	if (result != 0) {
		pspDebugScreenPrintf("sceKernelDmaOpSync = 0x%08X\n", result);
	}

	if (result < 0) {
		result = sceKernelDmaOpQuit(dmaOp);
		if (result != 0) {
			pspDebugScreenPrintf("sceKernelDmaOpQuit = 0x%08X\n", result);
		}

		result = sceKernelDmaOpDeQueue(dmaOp);
		if (result != 0) {
			pspDebugScreenPrintf("sceKernelDmaOpDeQueue = 0x%08X\n", result);
		}
	}

	result = sceKernelDmaOpFree(dmaOp);
	if (result != 0) {
		pspDebugScreenPrintf("sceKernelDmaOpFree = 0x%08X\n", result);
	}

	AUDIO_SET_BUSY(0);
	sceCodec_driver_376399B6(0);
}

int copyInputBuffer(int offset, int length, int inputBufferIndex, int fileLength) {
	int copyLength = 2 * length;
	if (inputBufferIndex + copyLength > fileLength) {
		copyLength = fileLength - inputBufferIndex;
	}
	if (copyLength < 0) {
		copyLength = 0;
	}
	memcpy(srcBuffer + offset, inputBuffer + inputBufferIndex, copyLength);

	return inputBufferIndex + copyLength;
}

int audioInterruptHandler() {
	int oldIntr;
	asm("mfic %0, $0\n" : "=r" (oldIntr));
	asm("mtic $0, $0\n");

	interruptHandlerCalled++;

	int hwAttr = LW(0xBE00001C) & 0x1;
	if (hwAttr != 0) {
	}

	SW(0x0, 0xBE000024);
	AUDIO_SET_BUSY(0);
	sceCodec_driver_376399B6(0);

	asm("mtic %0, $0\n" : : "r" (oldIntr));

	return -1;
}

void runTest7() {
	int result;
	DmaOperationLink dmaOperationLink1;
	DmaOperationLink dmaOperationLink2;
	DmaOperationLink dmaOperationLink3;
	DmaOperationLink dmaOperationLink4;
	int length = 0x1000;
	int i;
	u32 test;

	if (inputBufferSize == 0) {
		inputBufferSize = 0x1000000;
		partitionId = sceKernelAllocPartitionMemory(PSP_MEMORY_PARTITION_USER, "AudioRaw", PSP_SMEM_Low, inputBufferSize, 0);
		if (partitionId < 0) {
			pspDebugScreenPrintf("sceKernelAllocPartitionMemory returned 0x%08X\n", partitionId);
			inputBufferSize = 0;
		} else {
			inputBuffer = sceKernelGetBlockHeadAddr(partitionId);
			if (inputBuffer == NULL) {
				pspDebugScreenPrintf("sceKernelGetBlockHeadAddr returned 0x%08X\n", (u32) inputBuffer);
				inputBufferSize = 0;
			}
		}

		SceUID fd = sceIoOpen("ms0:/audio.raw", PSP_O_RDONLY, 0);
		if (fd < 0) {
			pspDebugScreenPrintf("Error while opening 'ms0:/audio.raw': 0x%08X\n", fd);
			fileLength = 0;
		} else {
			fileLength = sceIoLseek32(fd, 0, PSP_SEEK_END);
			if (fileLength > 0) {
				sceIoLseek32(fd, 0, PSP_SEEK_SET);

				if (fileLength > inputBufferSize) {
					fileLength = inputBufferSize;
				}
				int readLength = sceIoRead(fd, inputBuffer, fileLength);
				if (readLength < 0) {
					pspDebugScreenPrintf("Error while reading input file: 0x%08X\n", readLength);
				} else {
					fileLength = readLength;
				}
			}
			sceIoClose(fd);
		}

		result = sceKernelReleaseIntrHandler(10);
		if (result < 0) {
			pspDebugScreenPrintf("sceKernelReleaseIntrHandler returned 0x%08X\n", result);
		}
		result = sceKernelRegisterIntrHandler(10, 2, audioInterruptHandler, 0, 0);
		if (result < 0) {
			pspDebugScreenPrintf("sceKernelRegisterIntrHandler returned 0x%08X\n", result);
		}
		result = sceKernelEnableIntr(10);
		if (result < 0) {
			pspDebugScreenPrintf("sceKernelEnableIntr returned 0x%08X\n", result);
		}
	}

	init();
	dmaOperationLink1.src = DMACPTR(srcBuffer + 0 * length);
	dmaOperationLink1.dst = DMACPTR(0xBE000060);
	dmaOperationLink1.next = DMACPTR(&dmaOperationLink2);
	setAttribute(&dmaOperationLink1, length, 3, 3, 2, 2, 1, 0, 0);
	sceKernelDcacheWritebackRange(&dmaOperationLink1, sizeof(dmaOperationLink1));

	dmaOperationLink2.src = DMACPTR(srcBuffer + 1 * length);
	dmaOperationLink2.dst = DMACPTR(0xBE000060);
	dmaOperationLink2.next = NULL;
	setAttribute(&dmaOperationLink2, length, 3, 3, 2, 2, 1, 0, 1);
	sceKernelDcacheWritebackRange(&dmaOperationLink2, sizeof(dmaOperationLink2));

	dmaOperationLink3.src = DMACPTR(srcBuffer + 2 * length);
	dmaOperationLink3.dst = DMACPTR(0xBE000060);
	dmaOperationLink3.next = DMACPTR(&dmaOperationLink4);
	setAttribute(&dmaOperationLink3, length, 3, 3, 2, 2, 1, 0, 0);
	sceKernelDcacheWritebackRange(&dmaOperationLink3, sizeof(dmaOperationLink3));

	dmaOperationLink4.src = DMACPTR(srcBuffer + 3 * length);
	dmaOperationLink4.dst = DMACPTR(0xBE000060);
	dmaOperationLink4.next = NULL;
	setAttribute(&dmaOperationLink4, length, 3, 3, 2, 2, 1, 0, 1);
	sceKernelDcacheWritebackRange(&dmaOperationLink4, sizeof(dmaOperationLink4));

	inputBufferIndex = copyInputBuffer(0, length, inputBufferIndex, fileLength);

	int oldIntr;
	asm("mfic %0, $0\n" : "=r" (oldIntr));
	asm("mtic $0, $0\n");

	AUDIO_SET_BUSY(1);
	sceCodec_driver_376399B6(1);
	SW(0x0, 0xBE000004);

	u32 startLoop = SYSTEM_TIME;
	while (LW(0xBE00000C) & 0x1) {
		u32 now = SYSTEM_TIME;
		if (now - startLoop > 10000) {
			pspDebugScreenPrintf("Timeout while initializing audio!\n");
			break;
		}
	}

	SW(0x6, 0xBE000008);
	SW(0x1, 0xBE00002C);
	SW(0x1, 0xBE000020);

	for (i = 0; i < 24; i++) {
		u32 startLoop = SYSTEM_TIME;
		while ((LW(0xBE000028) & 0x10) == 0) {
			u32 now = SYSTEM_TIME;
			if (now - startLoop > 1000) {
				pspDebugScreenPrintf("Timeout while initializing audio index#%d!\n", i);
				break;
			}
		}
		SW(0x0, 0xBE000060);
	}

	test = LW(0xBE000028);
	if (test & 0x1) {
		pspDebugScreenPrintf("Completion flag 0x%X already set before starting!\n", test);
	}

	DmaOperation *dmaOp = sceKernelDmaOpAlloc();

	int flags = 0x100C941;
	result = sceKernelDmaOpAssign(dmaOp, -1, -1, flags);
	if (result != 0) {
		pspDebugScreenPrintf("sceKernelDmaOpAssign = 0x%08X\n", result);
	}

	result = sceKernelDmaOpSetCallback(dmaOp, dmaCallback, 0);
	if (result != 0) {
		pspDebugScreenPrintf("sceKernelDmaOpSetCallback = 0x%08X\n", result);
	}

	result = sceKernelDmaOpSetupLink(dmaOp, flags, &dmaOperationLink1);
	if (result != 0) {
		pspDebugScreenPrintf("sceKernelDmaOpSetupLink = 0x%08X\n", result);
	}

	result = sceKernelDmaOpEnQueue(dmaOp);
	if (result != 0) {
		pspDebugScreenPrintf("sceKernelDmaOpEnQueue = 0x%08X\n", result);
	}

	systemTimeStart = SYSTEM_TIME;
	systemTimeEnd = systemTimeStart;
	int count = 0x10000 / (2 * (length >> 2));

	SW(0x7, 0xBE000008);
	SW(0x1, 0xBE000004);
	SW(0x1, 0xBE000010);
	SW(0x1, 0xBE000024);

	asm("mtic %0, $0\n" : : "r" (oldIntr));

	int timeout = 0;
	while ((inputBufferIndex + 2 * length) <= fileLength && !timeout) {
		for (i = 0; i < count && !timeout; i++) {
			int step = i & 0x1;
			inputBufferIndex = copyInputBuffer((1 - step) * 2 * length, length, inputBufferIndex, fileLength);

			int currentCallbackCalled = callbackCalled;
			u32 ddrFlushStart = SYSTEM_TIME;
			if (step) {
				dmaOperationLink2.next = NULL;
				dmaOperationLink4.next = DMACPTR(&dmaOperationLink1);
			} else {
				dmaOperationLink2.next = DMACPTR(&dmaOperationLink3);
				dmaOperationLink4.next = NULL;
			}
			sceKernelDcacheWritebackRange(&dmaOperationLink2, sizeof(dmaOperationLink2));
			sceKernelDcacheWritebackRange(&dmaOperationLink4, sizeof(dmaOperationLink4));
			sceDdrFlush(4);

			while (callbackCalled == currentCallbackCalled) {
					u32 now = SYSTEM_TIME;
					if (now - ddrFlushStart > 100000) {
						timeout = 1;
						break;
					}
			}

			if (callbackCalled == currentCallbackCalled) {
				pspDebugScreenPrintf("Callback not called after sceDdrFlush!\n");
			}
		}

		int currentInterruptHandlerCalled = interruptHandlerCalled;
		u32 startLoop = SYSTEM_TIME;
		while ((LW(0xBE00000C) & 0x1) && !timeout) {
			u32 now = SYSTEM_TIME;
			if (now - startLoop > 1000000) {
				pspDebugScreenPrintf("Timeout while waiting for end of inProgress!\n");
				timeout = 1;
				break;
			}
			if (interruptHandlerCalled != currentInterruptHandlerCalled) {
				break;
			}
		}
	}

	if (inputBufferIndex >= fileLength) {
		if (partitionId >= 0) {
			sceKernelFreePartitionMemory(partitionId);
			partitionId = -1;
		}
		inputBufferSize = 0;
		inputBufferIndex = 0;
		inputBuffer = NULL;
		fileLength = 0;
	}

	sceKernelDelayThread(100000);

	if (callbackCalled) {
		pspDebugScreenPrintf("Callback called %d times\n", callbackCalled);
		pspDebugScreenPrintf("Interrupt handler called %d times\n", interruptHandlerCalled);
		pspDebugScreenPrintf("Duration=%d microseconds for %d audio samples\n", systemTimeEnd - systemTimeStart, 2 * count * (length >> 2));
		// Results:
		//      11 microseconds for    8 audio samples
		//      11 microseconds for   16 audio samples
		//      35 microseconds for   32 audio samples
		//     206 microseconds for   64 audio samples
		//     571 microseconds for  128 audio samples
		//    1297 microseconds for  256 audio samples
		//    2748 microseconds for  512 audio samples
		//    5650 microseconds for 1024 audio samples
		//   11091 microseconds for 1984 audio samples
		// ==> approx. 179000 audio samples per second
		// The rate does not depend on the frequency values set at 0xBE000038, 0xBE00003C and 0xBE000044.
	} else {
		pspDebugScreenPrintf("Callback not called!\n");
	}

	result = sceKernelDmaOpSync(dmaOp, 0, 0);
	if (result != 0) {
		pspDebugScreenPrintf("sceKernelDmaOpSync = 0x%08X\n", result);
	}

	if (result < 0) {
		result = sceKernelDmaOpQuit(dmaOp);
		if (result != 0) {
			pspDebugScreenPrintf("sceKernelDmaOpQuit = 0x%08X\n", result);
		}

		result = sceKernelDmaOpDeQueue(dmaOp);
		if (result != 0) {
			pspDebugScreenPrintf("sceKernelDmaOpDeQueue = 0x%08X\n", result);
		}
	}

	result = sceKernelDmaOpFree(dmaOp);
	if (result != 0) {
		pspDebugScreenPrintf("sceKernelDmaOpFree = 0x%08X\n", result);
	}

	AUDIO_SET_BUSY(0);
	sceCodec_driver_376399B6(0);
}

void runTest() {
#if 0
	runTest1();
	runTest2();
	runTest3();
	runTest4();
	runTest5();
	runTest6();
#endif
	runTest7();
}

int main(int argc, char *argv[]) {
	SceCtrlData pad;
	int oldButtons = 0;
#define SECOND	   1000000
#define REPEAT_START (1 * SECOND)
#define REPEAT_DELAY (SECOND / 5)
	struct timeval repeatStart;
	struct timeval repeatDelay;

	repeatStart.tv_sec = 0;
	repeatStart.tv_usec = 0;
	repeatDelay.tv_sec = 0;
	repeatDelay.tv_usec = 0;

	SetupCallbacks();

	pspDebugScreenInit();
	pspDebugScreenPrintf("Press Cross to start the DmacManForKernel Test\n");

	while (!done) {
		sceCtrlReadBufferPositive(&pad, 1);
		int buttonDown = (oldButtons ^ pad.Buttons) & pad.Buttons;

		if (pad.Buttons == oldButtons) {
			struct timeval now;
			gettimeofday(&now, NULL);
			if (repeatStart.tv_sec == 0) {
				repeatStart.tv_sec = now.tv_sec;
				repeatStart.tv_usec = now.tv_usec;
				repeatDelay.tv_sec = 0;
				repeatDelay.tv_usec = 0;
			} else {
				long usec = (now.tv_sec - repeatStart.tv_sec) * SECOND;
				usec += (now.tv_usec - repeatStart.tv_usec);
				if (usec >= REPEAT_START) {
					if (repeatDelay.tv_sec != 0) {
						usec = (now.tv_sec - repeatDelay.tv_sec) * SECOND;
						usec += (now.tv_usec - repeatDelay.tv_usec);
						if (usec >= REPEAT_DELAY) {
							repeatDelay.tv_sec = 0;
						}
					}

					if (repeatDelay.tv_sec == 0) {
						buttonDown = pad.Buttons;
						repeatDelay.tv_sec = now.tv_sec;
						repeatDelay.tv_usec = now.tv_usec;
					}
				}
			}
		} else {
			repeatStart.tv_sec = 0;
		}

		if (buttonDown & PSP_CTRL_CROSS) {
			runTest();
		}

		if (buttonDown & PSP_CTRL_TRIANGLE) {
			pspDebugScreenPrintf("Exiting...\n");
			done = 1;
		}

		oldButtons = pad.Buttons;
	}

	sceGuTerm();

	pspSdkSetK1(0x100000);
	sceKernelExitGame();

	return 0;
}
