/*
*	PMF Player Test
*	Copyright (c) 2006 by Sorin P. C. <magik@hypermagik.com>
*/
#include <pspsdk.h>
#include <pspkernel.h>
#include <pspdebug.h>

#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <malloc.h>
#include "prx/debug.h"

PSP_MODULE_INFO("Jpcsp Connector", 0x1000, 1, 1);
PSP_MAIN_THREAD_ATTR(0);

int main_thread(SceSize _argc, ScePVoid _argp)
{
	char *arg = *(char**)_argp;
	char jpcspConnectorPrx[256] = { 0 };

	debug("Starting main_thread");
	if(arg)
	{
		char *p = strrchr(arg, '/');
		if (p != NULL)
		{
			*(p+1) = 0;
			strcpy(jpcspConnectorPrx, arg);
		}
	}

	strcat(jpcspConnectorPrx, "JpcspConnector.prx");


	
	pspDebugScreenInit();



	debug("Loading audiocodec.prx");
	if(pspSdkLoadStartModule("flash0:/kd/audiocodec.prx", PSP_MEMORY_PARTITION_KERNEL) < 0)
	{
		pspDebugScreenPrintf("Error loading module audiocodec.prx\n");
		return -1;
	}

	debug("Loading videocodec.prx");
	if(pspSdkLoadStartModule("flash0:/kd/videocodec.prx", PSP_MEMORY_PARTITION_KERNEL) < 0)
	{
		pspDebugScreenPrintf("Error loading module videocodec.prx\n");
		return -1;
	}

	debug("Loading mpegbase.prx");
	if(pspSdkLoadStartModule("flash0:/kd/mpegbase.prx", PSP_MEMORY_PARTITION_KERNEL) < 0)
	{
		pspDebugScreenPrintf("Error loading module mpegbase.prx\n");
		return -1;
	}

	debug("Loading mpeg_vsh.prx");
	if(pspSdkLoadStartModule("flash0:/kd/mpeg_vsh.prx", PSP_MEMORY_PARTITION_USER) < 0)
	{
		pspDebugScreenPrintf("Error loading module mpeg_vsh.prx\n");
		return -1;
	}

	debug("Loading semawm.prx");
	if(pspSdkLoadStartModule("flash0:/kd/semawm.prx", PSP_MEMORY_PARTITION_KERNEL) < 0)
	{
		pspDebugScreenPrintf("Error loading module semawm.prx\n");
		return -1;
	}

	debug("Loading usbstor.prx");
	if(pspSdkLoadStartModule("flash0:/kd/usbstor.prx", PSP_MEMORY_PARTITION_KERNEL) < 0)
	{
		pspDebugScreenPrintf("Error loading module usbstor.prx\n");
		return -1;
	}

	debug("Loading usbstormgr.prx");
	if(pspSdkLoadStartModule("flash0:/kd/usbstormgr.prx", PSP_MEMORY_PARTITION_KERNEL) < 0)
	{
		pspDebugScreenPrintf("Error loading module usbstormgr.prx\n");
		return -1;
	}

	debug("Loading usbstorms.prx");
	if(pspSdkLoadStartModule("flash0:/kd/usbstorms.prx", PSP_MEMORY_PARTITION_KERNEL) < 0)
	{
		pspDebugScreenPrintf("Error loading module usbstorms.prx\n");
		return -1;
	}

	debug("Loading usbstorboot.prx");
	if(pspSdkLoadStartModule("flash0:/kd/usbstorboot.prx", PSP_MEMORY_PARTITION_KERNEL) < 0)
	{
		pspDebugScreenPrintf("Error loading module usbstorboot.prx\n");
		return -1;
	}

	debug("Loading JpcspConnector.prx");
	char* file = "ms0:/movie.pmf";
	if(pspSdkLoadStartModuleWithArgs(jpcspConnectorPrx, PSP_MEMORY_PARTITION_USER, 1, &file) < 0)
	{
		pspDebugScreenPrintf("Error loading module JpcspConnector.prx\n");
		return -1;
	}

	sceKernelExitGame();

	return 0;
}

int main(int _argc, char** _argp)
{
	SceUID thread;

	thread = sceKernelCreateThread("main_thread", main_thread, 0x20, 0x10000, 0, NULL);

	if (thread >= 0)
		sceKernelStartThread(thread, sizeof(char*), _argp);	

	return 0;
}
