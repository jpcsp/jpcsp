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

PSP_MODULE_INFO("Audio LLE Test", PSP_MODULE_KERNEL, 1, 0);
PSP_MAIN_THREAD_ATTR(PSP_THREAD_ATTR_VFPU);

#define UCACHED(ptr)    (void *)((u32)(void *)(ptr) & 0x1FFFFFFF)                /* KU0 - cached. */

#define LW(addr) (*((u32 *)(addr)))
#define SW(value, addr)   (*((volatile u32*) (addr)) = (u32) (value))

int channelSampleCount = 0xFFC0;
//int channelSampleCount = 0x100;

// The OFW is using 64 dmacSamples.
// When trying with 12 dmacSamples, the output is still perfect,
// but with 8 dmacSamples, the mixer is sometimes a bit too slow
// and a few audio interrupts are happening.
int dmacSamples = 64;

volatile int done = 0;
volatile int interruptHandlerCalled = 0;
volatile int callbackCalled = 0;
volatile int updateAudioBufCalled = 0;
int channelCurSampleCnt;
int audioFlags;
void *channelBuf = NULL;
void *audioBuf = NULL;
DmaOperation *dmaPtr[3];
DmaOperationLink hwBuf[12];
u32 *timings;
int timingsIndex;
int timingsSize;

SceUID evFlagId;
int channelId;
int sceCodec_driver_376399B6(int busy);
int sceCodec_driver_FCA6D35B(int frequency);
int sceClockgenAudioClkSetFreq(int frequency);

#define SYSTEM_TIME LW(0xBC600000)
#define AUDIO_SET_BUSY(busy) SW(busy, 0xBE000000)

void pspSync() {
	asm("sync\n");
}

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

int audioInterruptHandler() {
	int oldIntr;
	asm("mfic %0, $0\n" : "=r" (oldIntr));
	asm("mtic $0, $0\n");

	interruptHandlerCalled++;

	int attr = audioFlags;
	int hwAttr = LW(0xBE00001C) & attr;
	if (hwAttr != 0) {
		attr -= hwAttr;
		sceKernelSetEventFlag(evFlagId, hwAttr << 29);
		if (hwAttr & 0x1) {
			sceKernelDmaOpQuit(dmaPtr[0]);
			dmaPtr[0]->unknown32 = (u32) UCACHED(audioBuf + dmacSamples * 4);
		}

		audioFlags = attr;
	}

	SW(attr, 0xBE000024);
	if (attr == 0) {
		AUDIO_SET_BUSY(0);
		sceCodec_driver_376399B6(0);
	}

	asm("mtic %0, $0\n" : : "r" (oldIntr));
	pspSync();

	return -1;
}

int pspMin(int a, int b) {
	return a <= b ? a : b;
}

int dmaUpdate() {
	int v = audioFlags & ~0x1;
	SW(v, 0xBE000004);
	audioFlags = v;
	sceKernelDmaOpQuit(dmaPtr[0]);
	sceKernelDmaOpDeQueue(dmaPtr[0]);
	if (audioFlags == 0) {
		AUDIO_SET_BUSY(0);
		sceCodec_driver_376399B6(0);
	}

	return 0;
}

u32 previousAudioCbTime = 0;

int audioOutputDmaCb(int unused, int arg1) {
	callbackCalled++;

	u32 now = SYSTEM_TIME;
	if (previousAudioCbTime != 0 && timingsIndex < timingsSize && 1) {
		timings[timingsIndex++] = now - previousAudioCbTime;
		previousAudioCbTime = now;
	}

	sceKernelSetEventFlag(evFlagId, 0x20000000);
	if (arg1 != 0) {
		dmaUpdate();
	}

	return 0;
}

void updateAudioBuf() {
	updateAudioBufCalled++;

	if (audioFlags == 0) {
		AUDIO_SET_BUSY(1);
		sceCodec_driver_376399B6(1);
	}

	int v2 = audioFlags | 0x1;
	SW(v2 ^ 0x1, 0xBE000004);
	audioFlags = v2;

	while ((LW(0xBE00000C) & 0x1) != 0) {
	}

	sceKernelDmaOpQuit(dmaPtr[0]);
	SW(0x1 ^ 0x7, 0xBE000008);
	SW(0x1, 0xBE00002C);
	SW(0x1, 0xBE000020);

	u32 start = SYSTEM_TIME;
	int i;
	for (i = 0; i < 24; i++) {
		while ((LW(0xBE000028) & 0x10) == 0) {
		}
		SW(0x0, 0xBE000060);
	}
	u32 end = SYSTEM_TIME;
	if (timingsIndex < timingsSize && 0) {
		timings[timingsIndex++] = end - start;
	}

	if (sceKernelDmaOpAssign(dmaPtr[0], 0xFF, 0xFF, 0x0100C941) == 0) {
        if (sceKernelDmaOpSetCallback(dmaPtr[0], audioOutputDmaCb, 0) == 0) {
            if (sceKernelDmaOpSetupLink(dmaPtr[0], 0x0100C941, hwBuf[0].next == NULL ? &(hwBuf[2]) : &(hwBuf[0])) == 0) {
                sceKernelSetEventFlag(evFlagId, 0x20000000);
                sceKernelDmaOpEnQueue(dmaPtr[0]);
            }
        }
	}

	previousAudioCbTime = SYSTEM_TIME;

	SW(0x7, 0xBE000008);
	SW(audioFlags, 0xBE000004);
	SW(audioFlags & 0x3, 0xBE000010);
	SW(audioFlags, 0xBE000024);

	pspSync();
}

int audioMixerThread() {
	while (1) {
		int flag = 0;
		u32 start = SYSTEM_TIME;
		if (sceKernelWaitEventFlag(evFlagId, 0x20000000, 33, 0, 0) < 0) {
			sceKernelExitThread(0);
			break;
		}
		u32 end = SYSTEM_TIME;
		if (timingsIndex < timingsSize && 0) {
			timings[timingsIndex++] = end - start;
		}

		int playedSamples = 0;
		void *sourceBuffer = NULL;
		if (channelBuf != NULL) {
			playedSamples = pspMin(channelCurSampleCnt, dmacSamples);
			sourceBuffer = channelBuf;
			channelCurSampleCnt -= playedSamples;
			if (channelCurSampleCnt == 0) {
				flag |= 0x1;
				channelBuf = NULL;
			} else {
				channelBuf += playedSamples * 4;
			}
		}

		if (playedSamples == 0) {
			int oldIntr;
			asm("mfic %0, $0\n" : "=r" (oldIntr));
			asm("mtic $0, $0\n");

			audioFlags &= ~0x1;

			asm("mtic %0, $0\n" : : "r" (oldIntr));
		} else {
			int bytes = dmacSamples * 4;
			int step = dmaPtr[0]->unknown32 < (u32) UCACHED(audioBuf + bytes);
			void *dstBuf = audioBuf + step * bytes;
			int sourceBufferSize = playedSamples * 4;
			memcpy(dstBuf, sourceBuffer, sourceBufferSize);
			memset(dstBuf + sourceBufferSize, 0, bytes - sourceBufferSize);
			sceKernelDcacheWritebackRange(dstBuf, bytes);

			hwBuf[1 + 2 * step].next = NULL;
			hwBuf[1 + 2 * (1 - step)].next = UCACHED(&(hwBuf[step * 2]));
			sceKernelDcacheWritebackRange(hwBuf, 4 * sizeof(DmaOperationLink));
			sceDdrFlush(4);

			if ((audioFlags & 0x1) == 0) {
				hwBuf[1].next = NULL;
				hwBuf[3].next = NULL;
				sceKernelDcacheWritebackRange(hwBuf, sizeof(hwBuf));

				int oldIntr;
				asm("mfic %0, $0\n" : "=r" (oldIntr));
				asm("mtic $0, $0\n");

				updateAudioBuf();

				asm("mtic %0, $0\n" : : "r" (oldIntr));
			}
			sceKernelSetEventFlag(evFlagId, flag);
		}
	}

	return 0;
}

int audioOutput(void *buffer) {
	if (channelBuf != NULL) {
		return -1;
	}
	channelCurSampleCnt = channelSampleCount;
	channelBuf = buffer;
	if ((audioFlags & 0x1) || buffer == NULL) {
		return channelSampleCount;
	}

	int size = dmacSamples * 8;
	memset(audioBuf, 0, size);
	sceKernelDcacheWritebackRange(audioBuf, size);

	updateAudioBuf();

	return channelSampleCount;
}

int audioOutputBlocking(void *buffer) {
	int oldIntr;
	asm("mfic %0, $0\n" : "=r" (oldIntr));
	asm("mtic $0, $0\n");

	int result = audioOutput(buffer);
	while (result < 0) {
		asm("mtic %0, $0\n" : : "r" (oldIntr));
		result = sceKernelWaitEventFlag(evFlagId, 1 << channelId, 0x20, 0, 0);
		if (result < 0) {
			return result;
		}

		asm("mfic %0, $0\n" : "=r" (oldIntr));
		asm("mtic $0, $0\n");

		result = audioOutput(buffer);
	}

	asm("mtic %0, $0\n" : : "r" (oldIntr));

	return result;
}

void runTest() {
	int inputBufferSize = 0x1000000;
	int partitionId = sceKernelAllocPartitionMemory(PSP_MEMORY_PARTITION_USER, "Audio Raw", PSP_SMEM_Low, inputBufferSize, 0);
	if (partitionId < 0) {
		pspDebugScreenPrintf("sceKernelAllocPartitionMemory returned 0x%08X\n", partitionId);
		return;
	}
	u8 *inputBuffer = sceKernelGetBlockHeadAddr(partitionId);
	if (inputBuffer == NULL) {
		pspDebugScreenPrintf("sceKernelGetBlockHeadAddr returned 0x%08X\n", (u32) inputBuffer);
		return;
	}

	int audioBufPartitionId = sceKernelAllocPartitionMemory(PSP_MEMORY_PARTITION_USER, "Audio Buffer", PSP_SMEM_Low, dmacSamples << 4, 0);
	if (audioBufPartitionId < 0) {
		pspDebugScreenPrintf("sceKernelAllocPartitionMemory returned 0x%08X\n", audioBufPartitionId);
		return;
	}
	audioBuf = sceKernelGetBlockHeadAddr(audioBufPartitionId);
	if (audioBuf == NULL) {
		pspDebugScreenPrintf("sceKernelGetBlockHeadAddr returned 0x%08X\n", (u32) audioBuf);
		return;
	}

	timingsSize = 0x10000;
	timingsIndex = 0;
	int timingsPartitionId = sceKernelAllocPartitionMemory(PSP_MEMORY_PARTITION_USER, "Audio Timings", PSP_SMEM_Low, timingsSize * sizeof(timings[0]), 0);
	if (timingsPartitionId < 0) {
		pspDebugScreenPrintf("sceKernelAllocPartitionMemory returned 0x%08X\n", timingsPartitionId);
		return;
	}
	timings = sceKernelGetBlockHeadAddr(timingsPartitionId);
	if (timings == NULL) {
		pspDebugScreenPrintf("sceKernelGetBlockHeadAddr returned 0x%08X\n", (u32) timings);
		return;
	}

	SceUID fd = sceIoOpen("ms0:/audio.raw", PSP_O_RDONLY, 0);
	if (fd < 0) {
		pspDebugScreenPrintf("Error while opening 'ms0:/audio.raw': 0x%08X\n", fd);
		return;
	}

	int fileLength = sceIoLseek32(fd, 0, PSP_SEEK_END);
	if (fileLength < 0) {
		pspDebugScreenPrintf("sceIoLseek32 returned 0x%08X\n", fileLength);
		return;
	}

	sceIoLseek32(fd, 0, PSP_SEEK_SET);

	if (fileLength > inputBufferSize) {
		fileLength = inputBufferSize;
	}
	int readLength = sceIoRead(fd, inputBuffer, fileLength);
	if (readLength < 0) {
		pspDebugScreenPrintf("Error while reading input file: 0x%08X\n", readLength);
		return;
	}

	fileLength = readLength;
	sceIoClose(fd);
	pspDebugScreenPrintf("Read file of length 0x%X\n", fileLength);

	int i;
	for (i = 0; i < 3; i++) {
		dmaPtr[i] = sceKernelDmaOpAlloc();
	}
	evFlagId = sceKernelCreateEventFlag("SceAudio", 0x201, 0, 0);

	for (i = 0; i < 4; i += 2) {
		int offset = i * dmacSamples * 2;
		hwBuf[i + 0].src = UCACHED(audioBuf + offset);
		hwBuf[i + 0].dst = UCACHED(0xBE000060);
		hwBuf[i + 0].next = UCACHED(&(hwBuf[i + 1]));
		hwBuf[i + 0].attributes = 0x04489000 | ((dmacSamples - 4) & 0xFFF);

		hwBuf[i + 1].src = UCACHED(audioBuf + offset + (dmacSamples - 4) * 4);
		hwBuf[i + 1].dst = UCACHED(0xBE000060);
		hwBuf[i + 1].next = NULL;
		hwBuf[i + 1].attributes = 0x84489004;
	}
	sceKernelDcacheWritebackRange(hwBuf, sizeof(hwBuf));
	dmaPtr[0]->unknown32 = (u32) UCACHED(audioBuf);

	// Set frequency
//	SW(0x100, 0xBE000038);
//	SW(0x100, 0xBE00003C);
//	SW(0x100, 0xBE000044);
	sceClockgenAudioClkSetFreq(48000);
//	sceCodec_driver_FCA6D35B(48000);

	channelId = 0;

	SceUID id = sceKernelCreateThread("SceAudioMixer", audioMixerThread, 5, 0x1600, PSP_THREAD_ATTR_NO_FILLSTACK, 0);
    if (id < 0 || sceKernelStartThread(id, 0, 0) != 0) {
        return;
	}

	int result = sceKernelReleaseIntrHandler(10);
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

	pspDebugScreenPrintf("Press Circle to stop the test\n");

	SceCtrlData pad;

	int inputBufferIndex = 0;
	while (inputBufferIndex + channelSampleCount * 4 <= fileLength) {
		if (sceCtrlPeekBufferPositive(&pad, 1) > 0) {
			if (pad.Buttons & PSP_CTRL_CIRCLE) {
				break;
			}
		}

		pspDebugScreenPrintf(".");

		u32 start = SYSTEM_TIME;
		audioOutputBlocking(inputBuffer + inputBufferIndex);
		u32 end = SYSTEM_TIME;
		if (timingsIndex < timingsSize && 0) {
			timings[timingsIndex++] = end - start;
		}

		inputBufferIndex += channelSampleCount * 4;
	}

	pspDebugScreenPrintf("\nTest completed\n");
	pspDebugScreenPrintf("Calls: intr=%d, callbacks=%d, updateAudioBuf=%d\n", interruptHandlerCalled, callbackCalled, updateAudioBufCalled);

	sceIoRemove("ms0:/audioTimings.u16");
	fd = sceIoOpen("ms0:/audioTimings.u16", PSP_O_WRONLY | PSP_O_CREAT, 0777);
	if (fd < 0) {
		pspDebugScreenPrintf("Error while opening 'ms0:/audioTimings.u16': 0x%08X\n", fd);
	} else {
		sceIoWrite(fd, timings, timingsIndex * sizeof(timings[0]));
		sceIoClose(fd);
	}

	sceKernelFreePartitionMemory(partitionId);
	sceKernelFreePartitionMemory(audioBufPartitionId);
	sceKernelFreePartitionMemory(timingsPartitionId);
}

int main(int argc, char *argv[]) {
	SceCtrlData pad;
	int oldButtons = 0;
#define SECOND	   1000000
#define REPEAT_START (1 * SECOND)
#define REPEAT_DELAY (SECOND / 5)
	struct timeval repeatStart;
	struct timeval repeatDelay;
	int displayInstructions = 1;

	repeatStart.tv_sec = 0;
	repeatStart.tv_usec = 0;
	repeatDelay.tv_sec = 0;
	repeatDelay.tv_usec = 0;

	SetupCallbacks();

	pspDebugScreenInit();

	while (!done) {
		if (displayInstructions) {
			pspDebugScreenPrintf("Press Cross to start the Audio LLE Test\n");
			pspDebugScreenPrintf("Press Triangle to exit\n");
			displayInstructions = 0;
		}

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
			displayInstructions = 1;
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
