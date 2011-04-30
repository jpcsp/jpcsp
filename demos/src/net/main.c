#include <stdlib.h>
#include <unistd.h>
#include <pspkernel.h>
#include <pspdebug.h>
#include <pspctrl.h>
#include <pspdisplay.h>
#include <pspgu.h>
#include <pspgum.h>
#include <psppower.h>
#include <pspnet.h>
#include <pspnet_inet.h>
#include <pspnet_apctl.h>
#include <pspnet_resolver.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <sys/select.h>
#include <errno.h>
#include <pspsdk.h>

#include <sys/stat.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>

#define printf pspDebugScreenPrintf

PSP_MODULE_INFO("Network Test", 0x1000, 1, 1);
PSP_MAIN_THREAD_ATTR(0);

int done = 0;

void resolve(char *hostname, struct in_addr *addr)
{
	int rid = -1;
	char buf[1024];

	if (sceNetResolverCreate(&rid, buf, sizeof(buf)) < 0)
	{
		printf("Cannot create resolver\n");
		return;
	}

	int timeout = 2;
	int retry = 3;
	if (sceNetResolverStartNtoA(rid, hostname, addr, timeout, retry) < 0)
	{
		printf("Error resolving %s\n", hostname);
		return;
	}

	sceNetResolverDelete(rid);
}

void testBlockingStream()
{
	int sock;
	int ret;
	struct sockaddr_in name;
	char *hostname = "www.google.com";
	char *cmd = "GET /\n\n";
	char buffer[4096];

	printf("Starting Test Blocking Stream...\n");

	sock = sceNetInetSocket(PF_INET, SOCK_STREAM, 0);
	if (sock < 0)
	{
		printf("Cannot create socket 0x%08X errno=%d\n", sock, sceNetInetGetErrno());
		return;
	}

	name.sin_family = AF_INET;
	name.sin_port = htons(80);
	resolve(hostname, &name.sin_addr);
	printf("Address of %s = 0x%08X\n", hostname, name.sin_addr.s_addr);

	ret = sceNetInetConnect(sock, (struct sockaddr *) &name, sizeof(name));
	if (ret < 0)
	{
		printf("Cannot sceNetInetConnect 0x%08X errno=%d", ret, sceNetInetGetErrno());
		return;
	}
	printf("Connected\n");

	int length = sceNetInetSend(sock, cmd, strlen(cmd), 0);
	printf("sceNetInetSend %d (errno=%d)\n", length, sceNetInetGetErrno());

	length = sceNetInetRecv(sock, buffer, sizeof(buffer), 0);
	printf("sceNetInetRecv %d (errno=%d)\n", length, sceNetInetGetErrno());
	buffer[length] = '\0';
	printf("%s\n", buffer);

	sceNetInetClose(sock);
}

void apctlHandler(int oldState, int newState, int event, int error, void *pArg)
{
	printf("ApctlHandler oldState=%d, newState=%d, event=%d, error=%d, pArg=0x%X\n", oldState, newState, event, error, (int) pArg);
}

/* Connect to an access point */
int connect_to_apctl(int config)
{
	int err;
	int stateLast = -1;

	int handlerId = sceNetApctlAddHandler(apctlHandler, 0x12345);

	/* Connect using the first profile */
	err = sceNetApctlConnect(config);
	if (err != 0)
	{
		printf("sceNetApctlConnect returns %08X\n", err);
		return 0;
	}

	printf("Connecting...\n");
	while (1)
	{
		int state;
		err = sceNetApctlGetState(&state);
		if (err != 0)
		{
			printf("sceNetApctlGetState returns 0x%x\n", err);
			break;
		}
		if (state > stateLast)
		{
			printf("  connection state %d of 4\n", state);
			stateLast = state;
		}
		if (state == 4)
			break;  // connected with static IP

		// wait a little before polling again
		sceKernelDelayThread(50*1000); // 50ms
	}
	printf("Connected!\n");

	sceNetApctlDelHandler(handlerId);

	if(err != 0)
	{
		return 0;
	}

	return 1;
}


int net_thread(SceSize args, void *argp)
{
	SceCtrlData pad;
	int oldButtons = 0;
	int err;
#define SECOND	   1000000
#define REPEAT_START (1 * SECOND)
#define REPEAT_DELAY (SECOND / 5)
	struct timeval repeatStart;
	struct timeval repeatDelay;

	repeatStart.tv_sec = 0;
	repeatStart.tv_usec = 0;
	repeatDelay.tv_sec = 0;
	repeatDelay.tv_usec = 0;

	if ((err = pspSdkInetInit()))
	{
		printf("Error, could not initialise the network %08X\n", err);
		sceKernelSleepThread();
		sceKernelExitGame();
	}

	if (!connect_to_apctl(1))
	{
		printf("Error, could not connect to apctl\n");
		sceKernelSleepThread();
		sceKernelExitGame();
	}

	pspDebugScreenPrintf("Press Cross to start test blocking stream\n");
	pspDebugScreenPrintf("Press Triangle to exit\n");

	while(!done)
	{
		sceDisplayWaitVblankStart();
		sceCtrlReadBufferPositive(&pad, 1);
		int buttonDown = (oldButtons ^ pad.Buttons) & pad.Buttons;

		if (pad.Buttons == oldButtons)
		{
			struct timeval now;
			gettimeofday(&now, NULL);
			if (repeatStart.tv_sec == 0)
			{
				repeatStart.tv_sec = now.tv_sec;
				repeatStart.tv_usec = now.tv_usec;
				repeatDelay.tv_sec = 0;
				repeatDelay.tv_usec = 0;
			}
			else
			{
				long usec = (now.tv_sec - repeatStart.tv_sec) * SECOND;
				usec += (now.tv_usec - repeatStart.tv_usec);
				if (usec >= REPEAT_START)
				{
					if (repeatDelay.tv_sec != 0)
					{
						usec = (now.tv_sec - repeatDelay.tv_sec) * SECOND;
						usec += (now.tv_usec - repeatDelay.tv_usec);
						if (usec >= REPEAT_DELAY)
						{
							repeatDelay.tv_sec = 0;
						}
					}

					if (repeatDelay.tv_sec == 0)
					{
						buttonDown = pad.Buttons;
						repeatDelay.tv_sec = now.tv_sec;
						repeatDelay.tv_usec = now.tv_usec;
					}
				}
			}
		}
		else
		{
			repeatStart.tv_sec = 0;
		}

		if (buttonDown & PSP_CTRL_CROSS)
		{
			testBlockingStream();
		}

		if (buttonDown & PSP_CTRL_CIRCLE)
		{
		}

		if (buttonDown & PSP_CTRL_TRIANGLE)
		{
			done = 1;
		}

		oldButtons = pad.Buttons;
	}

	sceKernelExitGame();
	return 0;
}

/* Exit callback */
int exit_callback(int arg1, int arg2, void *common)
{
	sceKernelExitGame();
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

	thid = sceKernelCreateThread("update_thread", CallbackThread, 0x11, 0xFA0, PSP_THREAD_ATTR_USER, 0);
	if(thid >= 0)
	{
		sceKernelStartThread(thid, 0, 0);
	}

	return thid;
}

int main(int argc, char *argv[])
{
	SceUID thid;

	SetupCallbacks();

	pspDebugScreenInit();

	if(pspSdkLoadInetModules() < 0)
	{
		printf("Error, could not load inet modules\n");
		sceKernelSleepThread();
	}

	/* Create a user thread to do the real work */
	thid = sceKernelCreateThread("net_thread", net_thread, 0x18, 0x10000, PSP_THREAD_ATTR_USER, NULL);
	if(thid < 0)
	{
		printf("Error, could not create thread\n");
		sceKernelSleepThread();
	}

	sceKernelStartThread(thid, 0, NULL);

	sceKernelExitDeleteThread(0);

	return 0;
}


