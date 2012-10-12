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

float pspDurationMillis[] = { 0, 910, 1137, 1214, 1023, 1125, 1227, 962, 1007, 1066, 1364, 682, 682, 819, 819, 819, 819, 682, 1214, 1227,
                              866, 1072, 1360, 792, 770, 920, 238, 323 };
char *testNames[] = { "", "Empty loop", "Simple loop", "read32", "read16", "read8", "write32", "write16", "write8",
                      "Function call no params", "Function call with params",
					  "FPU add.s", "FPU mul.s",
					  "VFPU vadd.s", "VFPU vadd.p", "VFPU vadd.t", "VFPU vadd.q", "VFPU vadd.q sequence",
					  "LWC1", "SWC1",
					  "memcpy (native)", "memset (native)", "strcpy (native)",
					  "memcpy (non-native)", "memset (non-native)", "strcpy (non-native)",
					  "syscall, fast, no params", "syscall, fast, one param"
                    };

#define KB(n)	((n) * 1024)
#define MB(n)	(KB(n) * 1024)
#define GB(n)	(MB(n) * 1024)
#define BUFFER_SIZE		MB(10)
char __attribute__((aligned(16))) buffer[BUFFER_SIZE];
int dummy;
int sumDurationMillis;
float sumPspDurationMillis;

SceUID logFd;


void printResult(char *name, int durationMillis, float pspDurationMillis)
{
	char s[1000];
	sprintf(s, "%-25s: %5d ms (%5.0f%%) @ %d MHz\n", name, durationMillis, pspDurationMillis / durationMillis * 100, scePowerGetCpuClockFrequencyInt());
	pspDebugScreenPrintf("%s", s);
	sceIoWrite(logFd, s, strlen(s));
}


void startTest()
{
	startSystemTime = sceKernelGetSystemTimeLow();
}


void endTest(int testNumber)
{
	int endSystemTime = sceKernelGetSystemTimeLow();
	int durationMicros = endSystemTime - startSystemTime;
	int durationMillis = (durationMicros + 500) / 1000;
	sumDurationMillis += durationMillis;
	sumPspDurationMillis += pspDurationMillis[testNumber];

	printResult(testNames[testNumber], durationMillis, pspDurationMillis[testNumber]);
}


void runTest1()
{
	startTest();
	int i;
	int j;
	for (j = 50; j > 0; j--)
	{
		for (i = 1000000; i > 0; i--)
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
	for (j = 50; j > 0; j--)
	{
		for (i = 1000000; i > 0; i--)
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
		for (i = BUFFER_SIZE / 4; i > 0; i--)
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
		for (i = BUFFER_SIZE / 2; i > 0; i--)
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
		for (i = BUFFER_SIZE; i > 0; i--)
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
		for (i = BUFFER_SIZE / 4; i > 0; i--)
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
		for (i = BUFFER_SIZE / 2; i > 0; i--)
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
		for (i = BUFFER_SIZE; i > 0; i--)
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
		for (i = 10000; i > 0; i--)
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
		for (i = 1000000; i > 0; i--)
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
		for (i = 1000000; i > 0; i--)
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
		for (i = 1000000; i > 0; i--)
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


void runTest17()
{
	startTest();
	int i;
	int j;
	for (j = 0; j < 10; j++)
	{
		for (i = 1000000; i > 0; i--)
		{
			asm("vadd.q C000, C100, C200\n");
			asm("vadd.q C000, C100, C200\n");
			asm("vadd.q C000, C100, C200\n");
			asm("vadd.q C000, C100, C200\n");
			asm("vadd.q C000, C100, C200\n");
			asm("vadd.q C000, C100, C200\n");
			asm("vadd.q C000, C100, C200\n");
			asm("vadd.q C000, C100, C200\n");
			asm("vadd.q C000, C100, C200\n");
			asm("vadd.q C000, C100, C200\n");
		}
	}
	endTest(17);
}


float runTest18()
{
	memset(buffer, 0, BUFFER_SIZE);
	startTest();
	int i;
	int j;
	float sum = 0;
	for (j = 0; j < 10; j++)
	{
		float *address = (float *) buffer;
		for (i = BUFFER_SIZE / 4; i > 0; i--)
		{
			sum += *address++;
		}
	}
	endTest(18);

	return sum;
}


void runTest19()
{
	startTest();
	int i;
	int j;
	for (j = 0; j < 10; j++)
	{
		float *address = (float *) buffer;
		for (i = BUFFER_SIZE / 4; i > 0; i--)
		{
			*address++ = 1.f;
		}
	}
	endTest(19);
}


void runTest20()
{
	startTest();
	int i;
	int length = BUFFER_SIZE / 2;
	for (i = 0; i < 10; i++)
	{
		memcpy(buffer, buffer + length, length);
	}
	endTest(20);
}


void runTest21()
{
	startTest();
	int i;
	int length = BUFFER_SIZE;
	for (i = 0; i < 10; i++)
	{
		memset(buffer, 0, length);
	}
	endTest(21);
}


void runTest22()
{
	startTest();
	int i;
	int length = BUFFER_SIZE / 2 - 16;
	char *s = buffer + BUFFER_SIZE / 2;
	memset(s, 'a', length);
	s[length] = '\0';

	for (i = 0; i < 10; i++)
	{
		strcpy(buffer, s);
	}
	endTest(22);
}


int runTest23()
{
	startTest();
	int i;
	int j;
	int length = BUFFER_SIZE / 2;
	int sum = 0;
	for (i = 0; i < 3; i++)
	{
		for (j = length - 1; j >= 0; j--)
		{
			buffer[j] = buffer[j + length];
			// Fake sum to avoid a native code sequence
			sum += j;
		}
	}
	endTest(23);

	return sum;
}


int runTest24()
{
	startTest();
	int i;
	int j;
	int length = BUFFER_SIZE;
	int sum = 0;
	for (i = 0; i < 2; i++)
	{
		for (j = length - 1; j >= 0; j--)
		{
			buffer[j] = '\0';
			// Fake sum to avoid a native code sequence
			sum += j;
		}
	}
	endTest(24);

	return sum;
}


int runTest25()
{
	startTest();
	int i;
	int j;
	char c;
	int length = BUFFER_SIZE / 2 - 16;
	char *s = buffer + BUFFER_SIZE / 2;
	memset(s, 'a', length);
	s[length] = '\0';

	int sum = 0;
	for (i = 0; i < 3; i++)
	{
		for (j = 0; 1; j++)
		{
			c = s[j];
			buffer[j] = c;
			if (c == 0)
			{
				break;
			}
			// Fake sum to avoid a native code sequence
			sum += j;
		}
	}
	endTest(25);

	return sum;
}


int runTest26()
{
	startTest();
	int i;
	int sum = 0;
	for (i = 200000; i > 0; i--)
	{
		sum += sceKernelGetSystemTimeLow();
	}
	endTest(26);

	return sum;
}


int runTest27()
{
	startTest();
	int i;
	int sum = 0;
	SceKernelSysClock time;
	for (i = 200000; i > 0; i--)
	{
		sum += sceKernelGetSystemTime(&time);
	}
	endTest(27);

	return sum;
}


void runTest()
{
	sumDurationMillis = 0;
	sumPspDurationMillis = 0;
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
	runTest19();
	runTest20();
	runTest21();
	runTest22();
	runTest23();
	runTest24();
	runTest25();
	runTest26();
	runTest27();

	printResult("Overall performance index", sumDurationMillis, sumPspDurationMillis);
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

	logFd = sceIoOpen("compilerPerf.log", PSP_O_WRONLY | PSP_O_CREAT, 0777);

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

	sceIoClose(logFd);

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

