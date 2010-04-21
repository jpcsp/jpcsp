/*
*	PMF Player Test
*	Copyright (c) 2006 by Sorin P. C. <magik@hypermagik.com>
*/
#include <pspsdk.h>
#include <pspkernel.h>
#include <pspdebug.h>
#include <psputility_avmodules.h>

#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <malloc.h>
#include "../prx/debug.h"

PSP_MODULE_INFO("Jpcsp Connector", 0, 1, 0);
PSP_MAIN_THREAD_ATTR(THREAD_ATTR_USER | THREAD_ATTR_VFPU);

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Globals:
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
int runningFlag = 1;

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Callbacks:
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/* Exit callback */
int exit_callback(int arg1, int arg2, void *common) {
          runningFlag = 0;
          return 0;
}

/* Callback thread */
int CallbackThread(SceSize args, void *argp) {
          int cbid;

          cbid = sceKernelCreateCallback("Exit Callback", exit_callback, NULL);
          sceKernelRegisterExitCallback(cbid);

          sceKernelSleepThreadCB();

          return 0;
}

/* Sets up the callback thread and returns its thread id */
int SetupCallbacks(void) {
          int thid = 0;

          thid = sceKernelCreateThread("update_thread", CallbackThread, 0x11, 0x200, PSP_THREAD_ATTR_USER, 0);
          if(thid >= 0) {
                    sceKernelStartThread(thid, 0, 0);
          }

          return thid;
}


///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Main:
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
int main(int _argc, char** _argp)
{
	char *arg = *(char**)_argp;
	char jpcspConnectorPrx[256] = { 0 };

	if (arg)
	{
		char *p = strrchr(arg, '/');
		if (p != NULL)
		{
			*(p+1) = 0;
			strcpy(jpcspConnectorPrx, arg);
		}
	}

	strcat(jpcspConnectorPrx, "JpcspConnector.prx");
	debug(jpcspConnectorPrx);

	pspDebugScreenInit();
	SetupCallbacks();

	debug("Loading PSP_AV_MODULE_AVCODEC");
    if (sceUtilityLoadAvModule(PSP_AV_MODULE_AVCODEC) < 0){
        pspDebugScreenPrintf("Error loading PSP_AV_MODULE_AVCODEC\n");
        return -1;
    }

	debug("Loading PSP_AV_MODULE_MPEGBASE");
    if (sceUtilityLoadAvModule(PSP_AV_MODULE_MPEGBASE) < 0){
        pspDebugScreenPrintf("Error loading PSP_AV_MODULE_MPEGBASE\n");
        return -1;
    }

	debug("Loading JpcspConnector.prx");
	char* file = "ms0:/movie.pmf";
	if(pspSdkLoadStartModuleWithArgs(jpcspConnectorPrx, PSP_MEMORY_PARTITION_USER, 1, &file) < 0)
	{
		pspDebugScreenPrintf("Error loading module JpcspConnector.prx\n");
		return -1;
	}


//    while (runningFlag){
//        sceKernelDelayThread(100);
//    }
    sceKernelExitGame();
	return 0;
}
