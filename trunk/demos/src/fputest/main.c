#include <pspkernel.h>

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

static int results[32];

int main(int argc, char *argv[])
{
	results[0] = (adds(1.0, 1.0) == 2.0);
	results[1] = (subs(3.0, 1.0) == 2.0);
	results[2] = (muls(2.0, 1.0) == 2.0);
	results[3] = (divs(4.0, 2.0) == 2.0);
	results[4] = (abss(+2.0) == 2.0);
	results[5] = (abss(-2.0) == 2.0);
	results[6] = (negs(negs(+2.0)) == 2.0);

	int result = 0, i;

	for (i = 0; i < 32; ++i)
		result |= 1 << i;

	return 0;
}
