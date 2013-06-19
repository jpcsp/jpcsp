#include <pspkernel.h>
#include <pspdebug.h>
#include <pspctrl.h>
#include <pspdisplay.h>
#include <pspgu.h>
#include <pspgum.h>
#include <pspthreadman.h>
#include <malloc.h>

#include <sys/stat.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>

PSP_MODULE_INFO("Mbx Test", 0, 1, 0);
PSP_MAIN_THREAD_ATTR(THREAD_ATTR_USER);

int done = 0;
SceUID mbxId = -1;

typedef struct {
	SceKernelMsgPacket header;
	char text[64];
} MyMessage;

void printMbxStatus(SceUID mbxId) {
	SceKernelMbxInfo mbxInfo;
	int result;

	memset(&mbxInfo, 0, sizeof(mbxInfo));
	mbxInfo.size = sizeof(mbxInfo);
	result = sceKernelReferMbxStatus(mbxId, &mbxInfo);
	if (result != 0) {
		pspDebugScreenPrintf("sceKernelReferMbxStatus returning 0x%08X\n", result);
		return;
	}
	pspDebugScreenPrintf("sceKernelReferMbxStatus mbxId=0x%08X:\n", mbxId);
	pspDebugScreenPrintf("    name           = '%s'\n", mbxInfo.name);
	pspDebugScreenPrintf("    attr           = 0x%X\n", mbxInfo.attr);
	pspDebugScreenPrintf("    numWaitThreads = %d\n", (int) mbxInfo.numWaitThreads);
	pspDebugScreenPrintf("    numMessages    = %d\n", (int) mbxInfo.numMessages);
	pspDebugScreenPrintf("    firstMessage   = 0x%08X\n", (int) mbxInfo.firstMessage);
}

int receiveMbxThread(SceSize args, void *argp) {
	while (!done) {
		sceKernelSleepThread();

		MyMessage *msg;
		int result = sceKernelReceiveMbx(mbxId, (void **) &msg, NULL);
		pspDebugScreenPrintf("sceKernelReceiveMbx = 0x%08X\n", result);
		if (result == 0) {
			pspDebugScreenPrintf(" msg = 0x%08X\n", (int) msg);
			pspDebugScreenPrintf(" msg.header.next = 0x%08X\n", (int) msg->header.next);
			pspDebugScreenPrintf(" msg.header.msgPriority = %d\n", msg->header.msgPriority);
			pspDebugScreenPrintf(" msg.header.dummy[3] = 0x%02X 0x%02X 0x%02X\n", msg->header.dummy[0], msg->header.dummy[1], msg->header.dummy[2]);
			pspDebugScreenPrintf(" msg.text = '%s'\n", msg->text);
			free(msg);
			printMbxStatus(mbxId);
		}
	}

	return 0;
}

void printHeader() {
	pspDebugScreenInit();
	pspDebugScreenPrintf("Press Cross to create the Mbx\n");
	pspDebugScreenPrintf("Press Circle to send a message to the Mbx\n");
	pspDebugScreenPrintf("Press Square to get the Mbx status\n");
	pspDebugScreenPrintf("Press Left to receive a message from the Mbx\n");
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
	int msgCount = 0;

	repeatStart.tv_sec = 0;
	repeatStart.tv_usec = 0;
	repeatDelay.tv_sec = 0;
	repeatDelay.tv_usec = 0;

	printHeader();

	int receiveThreadId = sceKernelCreateThread("ReceiveMbx", receiveMbxThread, 0x50, 0x1000, 0, 0);
	sceKernelStartThread(receiveThreadId, 0, 0);

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
			mbxId = sceKernelCreateMbx("Mbx", 0, NULL);
			pspDebugScreenPrintf("sceKernelCreateMbx = 0x%08X\n", mbxId);
			printMbxStatus(mbxId);
		}

		if (buttonDown & PSP_CTRL_CIRCLE) {
			printHeader();
			msgCount++;
			MyMessage *msg = malloc(sizeof(MyMessage));
			msg->header.next = (void *) 0x12345678;
			msg->header.msgPriority = 1;
			msg->header.dummy[0] = 2;
			msg->header.dummy[1] = 3;
			msg->header.dummy[2] = 4;
			sprintf(msg->text, "Hello %d", msgCount);
			result = sceKernelSendMbx(mbxId, msg);
			pspDebugScreenPrintf("sceKernelSendMbx msg=0x%08X, msgCount=%d: 0x%08X\n", (int) msg, msgCount, result);
			printMbxStatus(mbxId);
		}

		if (buttonDown & PSP_CTRL_SQUARE) {
			printHeader();
			printMbxStatus(mbxId);
		}

		if (buttonDown & PSP_CTRL_LEFT) {
			sceKernelWakeupThread(receiveThreadId);
			printHeader();
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
