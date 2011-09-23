#include <pspkernel.h>
#include <pspsdk.h>
#include <pspdebug.h>
#include <pspctrl.h>
#include <pspdisplay.h>
#include <pspgu.h>
#include <pspgum.h>
#include <psppower.h>
#include <psputility_avmodules.h>

#include <sys/stat.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>

PSP_MODULE_INFO("sceSasCore Test", 0, 1, 0);
PSP_MAIN_THREAD_ATTR(THREAD_ATTR_USER | PSP_THREAD_ATTR_VFPU);

int main(int _argc, char** _argp)
{
	char *arg = *(char**)_argp;
	char prx[256] = { 0 };

	if (arg)
	{
		char *p = strrchr(arg, '/');
		if (p != NULL)
		{
			*(p+1) = 0;
			strcpy(prx, arg);
		}
	}

	strcat(prx, "sascore.prx");

	pspDebugScreenInit();
	SetupCallbacks();

	if (sceUtilityLoadAvModule(PSP_AV_MODULE_SASCORE) < 0) {
		pspDebugScreenPrintf("Error loading PSP_AV_MODULE_SASCORE\n");
		return -1;
	}

	if (pspSdkLoadStartModuleWithArgs(prx, PSP_MEMORY_PARTITION_USER, 0, NULL) < 0)
	{
		pspDebugScreenPrintf("Error loading module sascore.prx\n");
		return -1;
	}

//	sceKernelExitGame();
	return 0;
}


/* Exit callback */
int exit_callback(int arg1, int arg2, void *common)
{
	return 0;
}

/* Callback thread */
int CallbackThread(SceSize args, void *argp)
{
	int cbid;

	cbid = sceKernelCreateCallback("Exit Callback", exit_callback, (void*)0);
	sceKernelRegisterExitCallback(cbid);

	sceKernelSleepThreadCB();

	return 0;
}

/* Sets up the callback thread and returns its thread id */
int SetupCallbacks(void)
{
	int thid = 0;

	thid = sceKernelCreateThread("CallbackThread", CallbackThread, 0x11, 0xFA0, 0, 0);
	if(thid >= 0)
	{
		sceKernelStartThread(thid, 0, 0);
	}

	return thid;
}

