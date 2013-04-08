#include <pspkernel.h>
#include <pspdebug.h>
#include <pspctrl.h>
#include <pspdisplay.h>
#include <pspgu.h>
#include <pspgum.h>
#include <pspthreadman.h>

#include <sys/stat.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>

PSP_MODULE_INFO("VTimer Test", 0, 1, 0);
PSP_MAIN_THREAD_ATTR(THREAD_ATTR_USER);

int done = 0;

SceUInt vtimerHandlerResult = 0;
int vtimerHandlerCount = 1;

SceUInt vtimerHandler(SceUID uid, SceKernelSysClock *schedule, SceKernelSysClock *current, void *common) {
	vtimerHandlerCount--;
	if (vtimerHandlerCount <= 0) {
		vtimerHandlerResult = 0;
	}

	pspDebugScreenPrintf("vtimerHandler uid=0x%08X:\n", uid);
	pspDebugScreenPrintf("    schedule = %d / %d\n", schedule->low, schedule->hi);
	pspDebugScreenPrintf("    current  = %d / %d\n", current->low, current->hi);
	pspDebugScreenPrintf("    common   = 0x%08X\n", (int) common);
	pspDebugScreenPrintf("    Returning %d (count=%d)\n", vtimerHandlerResult, vtimerHandlerCount);

	return vtimerHandlerResult;
}

void printVTimerStatus(SceUID vtimerId) {
	SceKernelVTimerInfo vtimerInfo;
	int result;

	memset(&vtimerInfo, 0, sizeof(vtimerInfo));
	vtimerInfo.size = sizeof(vtimerInfo);
	result = sceKernelReferVTimerStatus(vtimerId, &vtimerInfo);
	if (result != 0) {
		pspDebugScreenPrintf("sceKernelReferVTimerStatus returning 0x%08X\n", result);
		return;
	}
	pspDebugScreenPrintf("sceKernelReferVTimerStatus vtimerId=0x%08X:\n", vtimerId);
	pspDebugScreenPrintf("    name     = '%s'\n", vtimerInfo.name);
	pspDebugScreenPrintf("    active   = %d\n", vtimerInfo.active);
	pspDebugScreenPrintf("    base     = %d / %d\n", vtimerInfo.base.low, vtimerInfo.base.hi);
	pspDebugScreenPrintf("    current  = %d / %d\n", vtimerInfo.current.low, vtimerInfo.current.hi);
	pspDebugScreenPrintf("    schedule = %d / %d\n", vtimerInfo.schedule.low, vtimerInfo.schedule.hi);
	pspDebugScreenPrintf("    handler  = 0x%08X\n", (int) vtimerInfo.handler);
	pspDebugScreenPrintf("    common   = 0x%08X\n", (int) vtimerInfo.common);
}

void printHeader() {
	pspDebugScreenInit();
	pspDebugScreenPrintf("Press Cross to create the VTimer\n");
	pspDebugScreenPrintf("Press Circle to start the VTimer\n");
	pspDebugScreenPrintf("Press Square to get the VTimer status\n");
	pspDebugScreenPrintf("Press Left to stop the VTimer\n");
	pspDebugScreenPrintf("Press Right to schedule the VTimer handler in 1 sec\n");
	pspDebugScreenPrintf("Press Up to schedule the VTimer handler at 0\n");
	pspDebugScreenPrintf("Press Down to set the VTimer time\n");
	pspDebugScreenPrintf("Press Triangle to Exit\n");
}

int main(int argc, char *argv[]) {
	SceCtrlData pad;
	int oldButtons = 0;
#define SECOND	   1000000
#define REPEAT_START (1 * SECOND)
#define REPEAT_DELAY (SECOND / 5)
	struct timeval repeatStart;
	struct timeval repeatDelay;
	int result;
	SceUID vtimerId = -1;

	repeatStart.tv_sec = 0;
	repeatStart.tv_usec = 0;
	repeatDelay.tv_sec = 0;
	repeatDelay.tv_usec = 0;

	printHeader();

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
			printHeader();
			vtimerId = sceKernelCreateVTimer("VTimer", NULL);
			pspDebugScreenPrintf("sceKernelCreateVTimer = 0x%08X\n", vtimerId);
			printVTimerStatus(vtimerId);
		}

		if (buttonDown & PSP_CTRL_CIRCLE) {
			printHeader();
			result = sceKernelStartVTimer(vtimerId);
			pspDebugScreenPrintf("sceKernelStartVTimer = %d\n", result);
			printVTimerStatus(vtimerId);
		}

		if (buttonDown & PSP_CTRL_SQUARE) {
			printHeader();
			printVTimerStatus(vtimerId);
		}

		if (buttonDown & PSP_CTRL_LEFT) {
			printHeader();
			result = sceKernelStopVTimer(vtimerId);
			pspDebugScreenPrintf("sceKernelStopVTimer = %d\n", result);
			printVTimerStatus(vtimerId);
		}

		if (buttonDown & PSP_CTRL_RIGHT) {
			printHeader();
			// Schedule handler in 1 second
			SceKernelSysClock schedule;
			result = sceKernelGetVTimerTime(vtimerId, &schedule);
			if (result != 0) {
				pspDebugScreenPrintf("sceKernelGetVTimerTime = 0x%08X\n", result);
			} else {
				schedule.low += 1000000;
				vtimerHandlerResult = 1000000;
				vtimerHandlerCount = 2;
				result = sceKernelSetVTimerHandler(vtimerId, &schedule, vtimerHandler, (void *) 0x12345678);
				pspDebugScreenPrintf("sceKernelSetVTimerHandler(schedule=%d / %d) = %d\n", schedule.low, schedule.hi, result);
				printVTimerStatus(vtimerId);
			}
		}

		if (buttonDown & PSP_CTRL_UP) {
			printHeader();
			// Schedule handler in 1 second
			SceKernelSysClock schedule;
			schedule.low = 0;
			schedule.hi = 0;
			vtimerHandlerResult = 1000000;
			vtimerHandlerCount = 2;
			result = sceKernelSetVTimerHandler(vtimerId, &schedule, vtimerHandler, (void *) 0x12345678);
			sceKernelDelayThread(10000);
			pspDebugScreenPrintf("sceKernelSetVTimerHandler(schedule=%d / %d) = %d\n", schedule.low, schedule.hi, result);
			printVTimerStatus(vtimerId);
		}

		if (buttonDown & PSP_CTRL_DOWN) {
			printHeader();
			printVTimerStatus(vtimerId);
			SceKernelSysClock time;
			int timeIn = 1000000;
			time.low = timeIn;
			time.hi = 0;
			result = sceKernelSetVTimerTime(vtimerId, &time);
			pspDebugScreenPrintf("sceKernelSetVTimerTime(time in=%d) = %d, time out=%d / %d\n", timeIn, result, time.low, time.hi);
			printVTimerStatus(vtimerId);
		}

		if (buttonDown & PSP_CTRL_TRIANGLE) {
			done = 1;
		}

		oldButtons = pad.Buttons;
	}

	sceGuTerm();

	sceKernelExitGame();
	return 0;
}

/* Exit callback */
int exit_callback(int arg1, int arg2, void *common) {
	done = 1;
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
	if (thid >= 0) {
		sceKernelStartThread(thid, 0, 0);
	}

	return thid;
}
