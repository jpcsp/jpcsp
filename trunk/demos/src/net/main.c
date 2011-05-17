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
#include <psputility_netparam.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <sys/select.h>
#include <errno.h>
#include <pspsdk.h>

#include <sys/stat.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>

extern int sceNetApctlAddHandler(void *, int);
extern int sceNetApctlDelHandler(int);

#define printf pspDebugScreenPrintf

PSP_MODULE_INFO("Network Test", 0x1000, 1, 1);
PSP_MAIN_THREAD_ATTR(0);

struct icmp
{
	unsigned char type;
	unsigned char code;
	unsigned short checksum;
	unsigned short identifier;
	unsigned short sequence;
	char data[8];
};

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
	int start, end;

	printf("Starting Test Blocking Stream...\n");

	sock = sceNetInetSocket(PF_INET, SOCK_STREAM, 0);
	if (sock < 0)
	{
		printf("Cannot create socket 0x%08X errno=%d\n", sock, sceNetInetGetErrno());
		return;
	}
	else
	{
		printf("Socket id=%d\n", sock);
	}

	name.sin_family = AF_INET;
	name.sin_port = htons(80);
	resolve(hostname, &name.sin_addr);
	printf("Address of %s = 0x%08X\n", hostname, name.sin_addr.s_addr);

	ret = sceNetInetConnect(sock, (struct sockaddr *) &name, sizeof(name));
	if (ret < 0)
	{
		printf("Cannot sceNetInetConnect %d errno=%d\n", ret, sceNetInetGetErrno());
		return;
	}
	printf("Connected\n");

	int length = sceNetInetSend(sock, cmd, strlen(cmd), 0);
	printf("sceNetInetSend %d (errno=%d)\n", length, sceNetInetGetErrno());

	// Test with a buffer length 0
	start = sceKernelGetSystemTimeLow();
	length = sceNetInetRecv(sock, buffer, 0, 0);
	end = sceKernelGetSystemTimeLow();
	printf("sceNetInetRecv buffer length 0: %d (errno=%d), duration=%d\n", length, sceNetInetGetErrno(), end - start);

	start = sceKernelGetSystemTimeLow();
	length = sceNetInetRecv(sock, buffer, sizeof(buffer), 0);
	end = sceKernelGetSystemTimeLow();
	printf("sceNetInetRecv %d (errno=%d), duration=%d\n", length, sceNetInetGetErrno(), end - start);
	buffer[length] = '\0';
	strcpy(buffer + 100, "...");
	printf("%s\n", buffer);

	sceNetInetClose(sock);
}

void testNonBlockingStream()
{
	int sock;
	int ret;
	struct sockaddr_in name;
	char *hostname = "www.google.com";
	char *cmd = "GET /\n\n";
	char buffer[4096];
	int flag;

	printf("Starting Test Non-Blocking Stream...\n");

	sock = sceNetInetSocket(PF_INET, SOCK_STREAM, 0);
	if (sock < 0)
	{
		printf("Cannot create socket 0x%08X errno=%d\n", sock, sceNetInetGetErrno());
		return;
	}
	else
	{
		printf("Socket id=%d\n", sock);
	}

	name.sin_family = AF_INET;
	name.sin_port = htons(80);
	resolve(hostname, &name.sin_addr);
	printf("Address of %s = 0x%08X\n", hostname, name.sin_addr.s_addr);

	flag = 1;
	ret = sceNetInetSetsockopt(sock, 0xFFFF, SO_NONBLOCK, &flag, sizeof(flag));
	if (ret < 0)
	{
		printf("Cannot sceNetInetSetsockopt %d errno=%d\n", ret, sceNetInetGetErrno());
		return;
	}

	ret = sceNetInetConnect(sock, (struct sockaddr *) &name, sizeof(name));
	if (ret < 0)
	{
		if (sceNetInetGetErrno() == 119)
		{
			printf("sceNetInetConnect returned %d errno=%d\n", ret, sceNetInetGetErrno());
		}
		else
		{
			printf("Cannot sceNetInetConnect %d errno=%d\n", ret, sceNetInetGetErrno());
			return;
		}
	}
	printf("Connected\n");

	while (1)
	{
		int length = sceNetInetSend(sock, cmd, strlen(cmd), 0);
		printf("sceNetInetSend %d (errno=%d)\n", length, sceNetInetGetErrno());
		if (length < 0 && sceNetInetGetErrno() == 128)
		{
			// wait a little before polling again
			sceKernelDelayThread(500*1000); // 500ms
		}
		else
		{
			break;
		}
	}

	while (1)
	{
		int length = sceNetInetRecv(sock, buffer, sizeof(buffer), 0);
		printf("sceNetInetRecv %d (errno=%d)\n", length, sceNetInetGetErrno());
		if (length < 0 && sceNetInetGetErrno() == 11)
		{
			// wait a little before polling again
			sceKernelDelayThread(100*1000); // 100ms
		}
		else
		{
			if (length >= 0)
			{
				buffer[length] = '\0';
			}
			break;
		}
	}
	strcpy(buffer + 100, "...");
	printf("%s\n", buffer);

	sceNetInetClose(sock);
}

void testConnect()
{
	int sock;
	int ret;
	struct sockaddr_in name;
	char *hostname = "www.google.com";
	int flag;
	int soError;
	unsigned int length;
	int errno;

	printf("Starting Test sceNetInetConnect...\n");

	sock = sceNetInetSocket(PF_INET, SOCK_STREAM, 0);
	if (sock < 0)
	{
		printf("Cannot create socket 0x%08X errno=%d\n", sock, sceNetInetGetErrno());
		return;
	}
	else
	{
		printf("Socket id=%d\n", sock);
	}

	// Try to bind to any invalid address: should return errno=125
	name.sin_family = AF_INET;
	name.sin_port = htons(80);
	name.sin_addr.s_addr = 0x08E52424;
	ret = sceNetInetBind(sock, (struct sockaddr *) &name, sizeof(name));
	errno = sceNetInetGetErrno();
	length = sizeof(soError);
	sceNetInetGetsockopt(sock, 0xFFFF, SO_ERROR, &soError, &length);
	printf("sceNetInetBind to invalid addr returned %d errno=%d, SO_ERROR=%d\n", ret, errno, soError);

	name.sin_family = AF_INET;
	name.sin_port = htons(80);
	resolve(hostname, &name.sin_addr);
	printf("Address of %s = 0x%08X\n", hostname, name.sin_addr.s_addr);

	flag = 1;
	ret = sceNetInetSetsockopt(sock, 0xFFFF, SO_NONBLOCK, &flag, sizeof(flag));
	if (ret < 0)
	{
		printf("Cannot sceNetInetSetsockopt %d errno=%d\n", ret, sceNetInetGetErrno());
		return;
	}

	ret = sceNetInetConnect(sock, (struct sockaddr *) &name, sizeof(name));
	if (ret < 0)
	{
		if (sceNetInetGetErrno() == 119)
		{
			errno = sceNetInetGetErrno();
			length = sizeof(soError);
			sceNetInetGetsockopt(sock, 0xFFFF, SO_ERROR, &soError, &length);
			printf("sceNetInetConnect returned %d errno=%d, SO_ERROR=%d\n", ret, errno, soError);
		}
		else
		{
			printf("Cannot sceNetInetConnect %d errno=%d\n", ret, sceNetInetGetErrno());
			return;
		}
	}

	while (1)
	{
		ret = sceNetInetConnect(sock, (struct sockaddr *) &name, sizeof(name));
		errno = sceNetInetGetErrno();
		length = sizeof(soError);
		sceNetInetGetsockopt(sock, 0xFFFF, SO_ERROR, &soError, &length);
		printf("sceNetInetConnect again returned %d errno=%d, SO_ERROR=%d\n", ret, errno, soError);
		if (errno == 127)
		{
			break;
		}
		else
		{
			sceKernelDelayThread(100*1000); // 100ms
		}
	}

	printf("Connected\n");

	sceNetInetClose(sock);
}

unsigned short icmpChecksum(struct icmp *picmp, int length)
{
	unsigned char *b = (unsigned char *) picmp;
	int n = length;
	int sum = 0;
	unsigned short answer;

	while (n > 1)
	{
		answer = (b[0] << 8) | b[1];
		sum += answer;
		b += 2;
		n -= 2;
	}

	if (n == 1)
	{
		answer = (b[0] << 8);
		sum += answer;
	}

	sum = (sum >> 16) + (sum & 0xFFFF);
	sum += (sum >> 16);
	answer = ~sum & 0xFFFF;
	answer = htons(answer);

	return answer;
}

void testRaw()
{
	int sock;
	int ret;
	struct sockaddr_in name;
	char *hostname = "www.google.com";
	unsigned int length;
	int errno;
	unsigned char buf[1024];
	struct icmp *picmp;
	int i;

	printf("Starting RAW Socket Test (ping %s)...\n", hostname);

	sock = sceNetInetSocket(PF_INET, SOCK_RAW, 1);
	if (sock < 0)
	{
		printf("Cannot create socket 0x%08X errno=%d\n", sock, sceNetInetGetErrno());
		return;
	}

	name.sin_family = AF_INET;
	name.sin_port = htons(0);
	resolve(hostname, &name.sin_addr);

	memset(buf, 0, sizeof(buf));
	picmp = (struct icmp *) buf;
	picmp->type = 8; // echo request
	picmp->code = 0;
	picmp->identifier = 0x1234;
	picmp->sequence = 0x5678;
	picmp->checksum = 0;
	memcpy(picmp->data, "TestPing", 8);

	picmp->checksum = icmpChecksum(picmp, sizeof(*picmp));

	int start = sceKernelGetSystemTimeLow();
	ret = sceNetInetSendto(sock, picmp, sizeof(*picmp), 0, (struct sockaddr *) &name, sizeof(name));
	if (ret < 0)
	{
		printf("Cannot sceNetInetSendto %d errno=%d\n", ret, sceNetInetGetErrno());
		return;
	}

	length = sizeof(name);
	ret = sceNetInetRecvfrom(sock, buf, sizeof(buf), 0, (struct sockaddr *) &name, &length);
	if (ret < 0)
	{
		printf("Cannot sceNetInetRecvfrom %d errno=%d\n", ret, sceNetInetGetErrno());
		return;
	}
	int end = sceKernelGetSystemTimeLow();

	int headerLength = (buf[0] & 0x0F) * 4;
	if (ret < headerLength + sizeof(*picmp))
	{
		printf("Insufficient response length %d\n", ret);
		return;
	}

	int correct = 1;
	picmp = (struct icmp *) &buf[headerLength];
	if (picmp->identifier != 0x1234)
	{
		printf("Incorrect identifier 0x%04X\n", picmp->identifier);
		correct = 0;
	}
	if (picmp->sequence != 0x5678)
	{
		printf("Incorrect sequence number 0x%04X\n", picmp->sequence);
		correct = 0;
	}
	if (picmp->type != 0)
	{
		printf("Incorrect type 0x%02X\n", picmp->type);
		correct = 0;
	}
	if (memcmp(picmp->data, "TestPing", 8) != 0)
	{
		printf("Incorrect data\n");
		correct = 0;
	}
	if (correct)
	{
		printf("Correct ping response in %d ms\n", (end - start) / 1000);
	}
	else
	{
		printf("Ping response:");
		for (i = 0; i < ret; i++)
		{
			if ((i % 16) == 0)
			{
				printf("\n");
			}
			printf(" %02X", buf[i]);
		}
		printf("\n");
	}

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
	netData name;
	int i;

	int handlerId = sceNetApctlAddHandler(apctlHandler, 0x12345);

	printf("sceUtilityCheckNetParam: ");
	int first = 1;
	for (i = 0; i < 20; i++)
	{
		err = sceUtilityCheckNetParam(i);
		if (err == 0)
		{
			if (!first)
			{
				printf(", ");
			}
			first = 0;
			printf("%d", i);
			err = sceUtilityGetNetParam(i, PSP_NETPARAM_NAME, &name);
			if (err == 0)
			{
				printf("(%s)", name.asString);
			}
		}
	}
	printf("\n");

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
	pspDebugScreenPrintf("Press Circle to start test non-blocking stream\n");
	pspDebugScreenPrintf("Press Square to start test connect\n");
	pspDebugScreenPrintf("Press Left to start raw socket test (ping)\n");
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
			testNonBlockingStream();
		}

		if (buttonDown & PSP_CTRL_SQUARE)
		{
			testConnect();
		}

		if (buttonDown & PSP_CTRL_LEFT)
		{
			testRaw();
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


