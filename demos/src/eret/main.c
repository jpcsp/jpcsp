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

PSP_MODULE_INFO("Allegrex ERET LLE Test", PSP_MODULE_KERNEL, 1, 0);
PSP_MAIN_THREAD_ATTR(PSP_THREAD_ATTR_VFPU);

#define LW(addr) (*((u32 *)(addr)))
#define SW(value, addr)   (*((volatile u32*) (addr)) = (u32) (value))

#define DISABLE_INTR(intr)      asm("mfic %0, $0\n" : "=r"(intr)); asm("mtic $0, $0\n");
#define ENABLE_INTR(intr)       asm("mtic %0, $0\n" : : "r"(intr));

int done = 0;

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

u32 getStatus() {
	int status;
	asm("mfc0 %0, $12\n" : "=r"(status));

	return status;
}

void runTest() {
	int eretOpcode = 0x42000018;

	u32 eretAddr = (u32) runTest;
	while (LW(eretAddr) != eretOpcode) {
		eretAddr += 4;
	}

	pspDebugScreenPrintf("Found eret at 0x%08X\n", eretAddr);

	pspDebugScreenPrintf("Status before disabling interrupts: 0x%X\n", getStatus());

	int intrEnabledBefore;
	DISABLE_INTR(intrEnabledBefore);

	pspDebugScreenPrintf("Status before eret: 0x%X\n", getStatus());

	// Load EPC
	asm("mtc0 %0, $14\n" : : "r"(eretAddr + 4));

	asm("nop\n");
	asm("nop\n");
	asm("nop\n");
	asm("eret\n");
	asm("nop\n");
	asm("nop\n");
	asm("nop\n");

	pspDebugScreenPrintf("Status after eret: 0x%X\n", getStatus());

	int intrEnabledAfter;
	DISABLE_INTR(intrEnabledAfter);

	pspDebugScreenPrintf("Status after disabling interrupts again: 0x%X\n", getStatus());

	ENABLE_INTR(intrEnabledBefore);

	pspDebugScreenPrintf("Status after enabling interrupts: 0x%X\n", getStatus());

	pspDebugScreenPrintf("Interrupts before eret: 0x%X\n", intrEnabledBefore);
	pspDebugScreenPrintf("Interrupts after eret: 0x%X\n", intrEnabledAfter);
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
			pspDebugScreenPrintf("Press Cross to start the Allegrex ERET LLE Test\n");
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
