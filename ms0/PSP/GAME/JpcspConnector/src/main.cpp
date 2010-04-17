/*
*	PMF Player Test
*	Copyright (c) 2006 by Sorin P. C. <magik@hypermagik.com>
*
*	Adapted for JpcspConnector
*/
#include <pspsdk.h>
#include <pspkernel.h>
#include <pspdebug.h>
#include "pmfplayer.h"

#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <malloc.h>

PSP_MODULE_INFO("JpcspConnector", 0x1000, 1, 1);
PSP_MAIN_THREAD_ATTR(0);

int pmfmain_thread(SceSize _argc, void* _argp)
{
	CPMFPlayer *MpegDecoder = new CPMFPlayer();

	if(MpegDecoder->Initialize() < 0)
	{
		pspDebugScreenInit();
		pspDebugScreenPrintf(MpegDecoder->GetLastError());
		sceKernelDelayThread(5000000);
		return -1;
	}

	if(MpegDecoder->Load("ms0:/movie.pmf") < 0)
	{
		pspDebugScreenInit();
		pspDebugScreenPrintf(MpegDecoder->GetLastError());
		sceKernelDelayThread(5000000);
		return -1;
	}

	if(MpegDecoder->Play() < 0)
	{
		pspDebugScreenInit();
		pspDebugScreenPrintf(MpegDecoder->GetLastError());
		sceKernelDelayThread(5000000);
		return -1;
	}

	MpegDecoder->Shutdown();

	return 0;
}


int main_thread(SceSize _argc, ScePVoid _argp)
{
	pspDebugScreenInit();

	debug("main_thread");
	if(pspSdkLoadStartModule("flash0:/kd/audiocodec.prx", PSP_MEMORY_PARTITION_KERNEL) < 0)
	{
		pspDebugScreenPrintf("Error loading module audiocodec.prx\n");
		return -1;
	}

	if(pspSdkLoadStartModule("flash0:/kd/videocodec.prx", PSP_MEMORY_PARTITION_KERNEL) < 0)
	{
		pspDebugScreenPrintf("Error loading module videocodec.prx\n");
		return -1;
	}

	if(pspSdkLoadStartModule("flash0:/kd/mpegbase.prx", PSP_MEMORY_PARTITION_KERNEL) < 0)
	{
		pspDebugScreenPrintf("Error loading module mpegbase.prx\n");
		return -1;
	}

	if(pspSdkLoadStartModule("flash0:/kd/mpeg_vsh.prx", PSP_MEMORY_PARTITION_USER) < 0)
	{
		pspDebugScreenPrintf("Error loading module mpeg_vsh.prx\n");
		return -1;
	}

	if(pspSdkLoadStartModule("flash0:/kd/semawm.prx", PSP_MEMORY_PARTITION_KERNEL) < 0)
	{
		pspDebugScreenPrintf("Error loading module semawm.prx\n");
		return -1;
	}

	if(pspSdkLoadStartModule("flash0:/kd/usbstor.prx", PSP_MEMORY_PARTITION_KERNEL) < 0)
	{
		pspDebugScreenPrintf("Error loading module usbstor.prx\n");
		return -1;
	}

	if(pspSdkLoadStartModule("flash0:/kd/usbstormgr.prx", PSP_MEMORY_PARTITION_KERNEL) < 0)
	{
		pspDebugScreenPrintf("Error loading module usbstormgr.prx\n");
		return -1;
	}

	if(pspSdkLoadStartModule("flash0:/kd/usbstorms.prx", PSP_MEMORY_PARTITION_KERNEL) < 0)
	{
		pspDebugScreenPrintf("Error loading module usbstorms.prx\n");
		return -1;
	}

	if(pspSdkLoadStartModule("flash0:/kd/usbstorboot.prx", PSP_MEMORY_PARTITION_KERNEL) < 0)
	{
		pspDebugScreenPrintf("Error loading module usbstorboot.prx\n");
		return -1;
	}

	SceUID T = sceKernelCreateThread("pmfplayer_thread", pmfmain_thread, 0x20, 0xFA0, THREAD_ATTR_USER, NULL);

	sceKernelStartThread(T, 0, NULL);

	sceKernelWaitThreadEnd(T, 0);

	sceKernelExitGame();

	return 0;
}

extern "C" int main(int _argc, char** _argp)
{
	SceUID thread;

	debug("main");
	thread = sceKernelCreateThread("main_thread", main_thread, 0x20, 0x10000, 0, NULL);

	if (thread >= 0)
		sceKernelStartThread(thread, sizeof(char*), _argp);	

	return 0;
}
