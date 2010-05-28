#include <pspkernel.h>
#include <pspdebug.h>
#include <pspctrl.h>
#include <pspdisplay.h>
#include <pspgu.h>
#include <pspgum.h>
#include <psppower.h>

#include <sys/stat.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>

PSP_MODULE_INFO("Compiler Performance Test", 0, 1, 0);
PSP_MAIN_THREAD_ATTR(THREAD_ATTR_USER | PSP_THREAD_ATTR_VFPU);

int done = 0;
int cpuFreq = 222;
int startSystemTime;

float pspDurationMillis[] = { 0, 910, 1138, 1215, 1024, 989, 1229, 962, 1007, 1066, 1365, 682, 682, 819, 819, 819, 819, 1214, 1229 };
char *testNames[] = { "", "Empty loop", "Simple loop", "read32", "read16", "read8", "write32", "write16", "write8",
                      "Function call no params", "Function call with params",
					  "FPU add.s", "FPU mul.s",
					  "VFPU vadd.s", "VFPU vadd.p", "VFPU vadd.t", "VFPU vadd.q",
					  "LWC1", "SWC1"
                    };

#define KB(n)	((n) * 1024)
#define MB(n)	(KB(n) * 1024)
#define GB(n)	(MB(n) * 1024)
#define BUFFER_SIZE		MB(10)
char buffer[BUFFER_SIZE];
int dummy;


void startTest()
{
	startSystemTime = sceKernelGetSystemTimeLow();
}


void endTest(int testNumber)
{
	int endSystemTime = sceKernelGetSystemTimeLow();
	int durationMicros = endSystemTime - startSystemTime;
	int durationMillis = (durationMicros + 500) / 1000;
	float pspReference = pspDurationMillis[testNumber] / durationMillis;

	pspDebugScreenPrintf("%-25s: %4d ms (%3.0f%%) @ %d MHz\n", testNames[testNumber], durationMillis, pspReference * 100, scePowerGetCpuClockFrequencyInt());
}


void runTest1()
{
	startTest();
	int i;
	int j;
	for (j = 0; j < 50; j++)
	{
		for (i = 0; i < 1000000; i++)
		{
		}
	}
	endTest(1);
}


int runTest2()
{
	startTest();
	int i;
	int j;
	int sum = 0;
	for (j = 0; j < 50; j++)
	{
		for (i = 0; i < 1000000; i++)
		{
			sum += i;
		}
	}
	endTest(2);

	return sum;
}


int runTest3()
{
	startTest();
	int i;
	int j;
	int sum = 0;
	for (j = 0; j < 10; j++)
	{
		int *address = (int *) buffer;
		for (i = 0; i < BUFFER_SIZE / 4; i++)
		{
			sum += *address++;
		}
	}
	endTest(3);

	return sum;
}


int runTest4()
{
	startTest();
	int i;
	int j;
	int sum = 0;
	for (j = 0; j < 5; j++)
	{
		short *address = (short *) buffer;
		for (i = 0; i < BUFFER_SIZE / 2; i++)
		{
			sum += *address++;
		}
	}
	endTest(4);

	return sum;
}


int runTest5()
{
	startTest();
	int i;
	int j;
	int sum = 0;
	for (j = 0; j < 3; j++)
	{
		char *address = (char *) buffer;
		for (i = 0; i < BUFFER_SIZE; i++)
		{
			sum += *address++;
		}
	}
	endTest(5);

	return sum;
}


void runTest6()
{
	startTest();
	int i;
	int j;
	for (j = 0; j < 10; j++)
	{
		int *address = (int *) buffer;
		for (i = 0; i < BUFFER_SIZE / 4; i++)
		{
			*address++ = i;
		}
	}
	endTest(6);
}


void runTest7()
{
	startTest();
	int i;
	int j;
	for (j = 0; j < 5; j++)
	{
		short *address = (short *) buffer;
		for (i = 0; i < BUFFER_SIZE / 2; i++)
		{
			*address++ = (short) i;
		}
	}
	endTest(7);
}


void runTest8()
{
	startTest();
	int i;
	int j;
	for (j = 0; j < 3; j++)
	{
		char *address = (char *) buffer;
		for (i = 0; i < BUFFER_SIZE; i++)
		{
			*address++ = (char) i;
		}
	}
	endTest(8);
}

void runTest9c()
{
	dummy = 0;
}

void runTest9b()
{
	runTest9c();
	runTest9c();
	runTest9c();
	runTest9c();
	runTest9c();
	runTest9c();
	runTest9c();
	runTest9c();
	runTest9c();
	runTest9c();
}

void runTest9a()
{
	runTest9b();
	runTest9b();
	runTest9b();
	runTest9b();
	runTest9b();
	runTest9b();
	runTest9b();
	runTest9b();
	runTest9b();
	runTest9b();
}

void runTest9()
{
	startTest();
	int i;
	int j;
	for (j = 0; j < 30; j++)
	{
		for (i = 0; i < 10000; i++)
		{
			runTest9a();
		}
	}
	endTest(9);
}


int runTest10a(int a, int b, int c, int d)
{
	dummy = 0;
	return a;
}

void runTest10()
{
	startTest();
	int i;
	int j;
	for (j = 0; j < 20; j++)
	{
		for (i = 0; i < 1000000; i++)
		{
			runTest10a(1, 2, 3, 4);
		}
	}
	endTest(10);
}


float runTest11()
{
	startTest();
	int i;
	int j;
	float sum = 0;
	for (j = 0; j < 30; j++)
	{
		for (i = 0; i < 1000000; i++)
		{
			sum += 1;
		}
	}
	endTest(11);

	return sum;
}


float runTest12()
{
	startTest();
	int i;
	int j;
	float sum = 1;
	for (j = 0; j < 30; j++)
	{
		for (i = 0; i < 1000000; i++)
		{
			sum *= 0.999999f;
		}
	}
	endTest(12);

	return sum;
}


void runTest13()
{
	startTest();
	int i;
	int j;
	for (j = 0; j < 30; j++)
	{
		for (i = 1000000; i > 0; i--)
		{
			asm("vadd.s S000, S100, S200\n");
		}
	}
	endTest(13);
}


void runTest14()
{
	startTest();
	int i;
	int j;
	for (j = 0; j < 30; j++)
	{
		for (i = 1000000; i > 0; i--)
		{
			asm("vadd.p C000.p, C100.p, C200.p\n");
		}
	}
	endTest(14);
}


void runTest15()
{
	startTest();
	int i;
	int j;
	for (j = 0; j < 30; j++)
	{
		for (i = 1000000; i > 0; i--)
		{
			asm("vadd.t C000.t, C100.t, C200.t\n");
		}
	}
	endTest(15);
}


void runTest16()
{
	startTest();
	int i;
	int j;
	for (j = 0; j < 30; j++)
	{
		for (i = 1000000; i > 0; i--)
		{
			asm("vadd.q C000, C100, C200\n");
		}
	}
	endTest(16);
}


float runTest17()
{
	startTest();
	int i;
	int j;
	float sum = 0;
	for (j = 0; j < 10; j++)
	{
		float *address = (float *) buffer;
		for (i = 0; i < BUFFER_SIZE / 4; i++)
		{
			sum += *address++;
		}
	}
	endTest(17);

	return sum;
}


void runTest18()
{
	startTest();
	int i;
	int j;
	for (j = 0; j < 10; j++)
	{
		float *address = (float *) buffer;
		for (i = 0; i < BUFFER_SIZE / 4; i++)
		{
			*address++ = 1.f;
		}
	}
	endTest(18);
}


void runTest()
{
	runTest1();
	runTest2();
	runTest3();
	runTest4();
	runTest5();
	runTest6();
	runTest7();
	runTest8();
	runTest9();
	runTest10();
	runTest11();
	runTest12();
	runTest13();
	runTest14();
	runTest15();
	runTest16();
	runTest17();
	runTest18();
	pspDebugScreenPrintf("--- End of Tests ---\n");
}


int main(int argc, char *argv[])
{
	SceCtrlData pad;
	int oldButtons = 0;
#define SECOND	   1000000
#define REPEAT_START (1 * SECOND)
#define REPEAT_DELAY (SECOND / 5)
	struct timeval repeatStart;
	struct timeval repeatDelay;

	repeatStart.tv_sec = 0;
	repeatStart.tv_usec = 0;
	repeatDelay.tv_sec = 0;
	repeatDelay.tv_usec = 0;

	pspDebugScreenInit();
	pspDebugScreenPrintf("Press Cross to start the Performance Test\n");
	pspDebugScreenPrintf("Press Circle to change the CPU Clock\n");

	while(!done)
	{
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
			runTest();
		}

		if (buttonDown & PSP_CTRL_CIRCLE)
		{
			cpuFreq += 111;
			if (cpuFreq > 333)
			{
				cpuFreq = 111;
			}

			int result = scePowerSetCpuClockFrequency(cpuFreq);
			if (result == 0)
			{
				pspDebugScreenPrintf("CPU Clock set to %d MHz\n", cpuFreq);
			}
			else
			{
				pspDebugScreenPrintf("Could not set CPU Clock set to %d MHz\n", cpuFreq);
			}
		}

		if (buttonDown & PSP_CTRL_TRIANGLE)
		{
			done = 1;
		}

		oldButtons = pad.Buttons;
	}

	sceGuTerm();

	sceKernelExitGame();
	return 0;
}

/* Exit callback */
int exit_callback(int arg1, int arg2, void *common)
{
	done = 1;
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

