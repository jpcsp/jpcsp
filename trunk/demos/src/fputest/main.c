#include <pspkernel.h>
#include <pspdebug.h>
#include <pspctrl.h>
#include <pspdisplay.h>

#define printf  pspDebugScreenPrintf


PSP_MODULE_INFO("fpu test", 0, 1, 1);

PSP_MAIN_THREAD_ATTR(THREAD_ATTR_USER);

static int exit_callback(int arg1, int arg2, void *common)
{
	sceKernelExitGame();
	return 0;
}

static int update_thread(SceSize args, void *argp)
{
	int cbid = sceKernelCreateCallback("Exit Callback", exit_callback, NULL);
	sceKernelRegisterExitCallback(cbid);
	sceKernelSleepThreadCB();
	return 0;
}

static __attribute__((constructor)) void setup_callbacks()
{
	int id;

	if ((id = sceKernelCreateThread("Update Thread", update_thread, 0x11, 0xFA0, 0, 0)) >= 0)
		sceKernelStartThread(id, 0, 0);
}


static __attribute__((destructor)) void back_to_kernel()
{
	sceKernelExitGame();
}

float __attribute__((noinline)) adds(float x, float y)
{
	float result;
	asm volatile("add.s %0, %1, %2" : "=f"(result) : "f"(x), "f"(y));
	return result;
}

float __attribute__((noinline)) subs(float x, float y)
{
	float result;
	asm volatile("sub.s %0, %1, %2" : "=f"(result) : "f"(x), "f"(y));
	return result;
}

float __attribute__((noinline)) muls(float x, float y)
{
	float result;
	asm volatile("mul.s %0, %1, %2" : "=f"(result) : "f"(x), "f"(y));
	return result;
}

float __attribute__((noinline)) divs(float x, float y)
{
	float result;
	asm volatile("div.s %0, %1, %2" : "=f"(result) : "f"(x), "f"(y));
	return result;
}

float __attribute__((noinline)) sqrts(float x)
{
	float result;
	asm volatile("sqrt.s %0, %1" : "=f"(result) : "f"(x));
	return result;
}

float __attribute__((noinline)) abss(float x)
{
	float result;
	asm volatile("abs.s %0, %1" : "=f"(result) : "f"(x));
	return result;
}

float __attribute__((noinline)) negs(float x)
{
	float result;
	asm volatile("neg.s %0, %1" : "=f"(result) : "f"(x));
	return result;
}

int __attribute__((noinline)) cvtws(float x, int rm)
{
	float resultFloat;
	asm volatile("ctc1 %0, $31" : : "r"(rm));
	asm volatile("cvt.w.s %0, %1" : "=f"(resultFloat) : "f"(x));
	int result = *((int *) &resultFloat);
	return result;
}


static int results[32];

int main(int argc, char *argv[])
{
	int n = 0;
	results[n++] = (adds(1.0, 1.0) == 2.0);
	results[n++] = (subs(3.0, 1.0) == 2.0);
	results[n++] = (muls(2.0, 1.0) == 2.0);
	results[n++] = (divs(4.0, 2.0) == 2.0);
	results[n++] = (abss(+2.0) == 2.0);
	results[n++] = (abss(-2.0) == 2.0);
	results[n++] = (negs(negs(+2.0)) == 2.0);
	results[n++] = (sqrts(4.0) == 2.0);

	results[n++] = (cvtws(1.1, 0) == 1);
	results[n++] = (cvtws(1.1, 1) == 1);
	results[n++] = (cvtws(1.1, 2) == 2);
	results[n++] = (cvtws(1.1, 3) == 1);

	results[n++] = (cvtws(-1.1, 0) == -1);
	results[n++] = (cvtws(-1.1, 1) == -1);
	results[n++] = (cvtws(-1.1, 2) == -1);
	results[n++] = (cvtws(-1.1, 3) == -2);

	results[n++] = (cvtws(1.9, 0) == 2);
	results[n++] = (cvtws(1.9, 1) == 1);
	results[n++] = (cvtws(1.9, 2) == 2);
	results[n++] = (cvtws(1.9, 3) == 1);

	results[n++] = (cvtws(1.5, 0) == 2);
	results[n++] = (cvtws(1.5, 1) == 1);
	results[n++] = (cvtws(1.5, 2) == 2);
	results[n++] = (cvtws(1.5, 3) == 1);

	int i;

	pspDebugScreenInit();
	for (i = 0; i < n; ++i) {
		printf("Test#%d: %s\n", i, results[i] ? "OK" : "Failed");
	}

    SceCtrlData pad;
    int oldButtons = 0;
	int done = 0;

	printf("Press Cross to Exit\n");
	while(!done)
	{
		sceCtrlReadBufferPositive(&pad, 1);
		int buttonDown = (oldButtons ^ pad.Buttons) & pad.Buttons;

		if (buttonDown & PSP_CTRL_CROSS)
		{
			done = 1;
		}

		oldButtons = pad.Buttons;
		sceDisplayWaitVblank();
	}

	return 0;
}
