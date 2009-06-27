#include <pspkernel.h>
#include <pspdebug.h>
#include <pspctrl.h>
#include <pspkernel.h>
#include <pspdisplay.h>
#include <pspgu.h>

PSP_MODULE_INFO("vfpu test", 0, 1, 1);

PSP_MAIN_THREAD_ATTR(THREAD_ATTR_USER | PSP_THREAD_ATTR_VFPU);

/* Define printf, just to make typing easier */
#define printf  pspDebugScreenPrintf

#define BUF_WIDTH	512
#define SCR_WIDTH	480
#define SCR_HEIGHT	272
#define FONT_HEIGHT	8

int done = 0;

void __attribute__((noinline)) vtfm4(ScePspFVector4 *v0, ScePspFMatrix4 *m0, ScePspFVector4 *v1)
{
	asm volatile (
   "lv.q   C100, 0x00+%1\n"
   "lv.q   C110, 0x10+%1\n"
   "lv.q   C120, 0x20+%1\n"
   "lv.q   C130, 0x30+%1\n"

   "lv.q   C200, %2\n"

   "lv.q   C000, %0\n"

   "vtfm4.q C000, E100, C200\n"

   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*m0) ,"m" (*v1) );
}

void __attribute__((noinline)) vhtfm4(ScePspFVector4 *v0, ScePspFMatrix4 *m0, ScePspFVector4 *v1)
{
	asm volatile (
   "lv.q   C100, 0x00+%1\n"
   "lv.q   C110, 0x10+%1\n"
   "lv.q   C120, 0x20+%1\n"
   "lv.q   C130, 0x30+%1\n"

   "lv.q   C200, %2\n"

   "lv.q   C000, %0\n"

   "vhtfm4.q C000, E100, C200\n"

   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*m0) ,"m" (*v1) );
}

void __attribute__((noinline)) vtfm3(ScePspFVector4 *v0, ScePspFMatrix4 *m0, ScePspFVector4 *v1)
{
	asm volatile (
   "lv.q   C100, 0x00+%1\n"
   "lv.q   C110, 0x10+%1\n"
   "lv.q   C120, 0x20+%1\n"
   "lv.q   C130, 0x30+%1\n"

   "lv.q   C200, %2\n"

   "lv.q   C000, %0\n"

   "vtfm3.t C000, E100, C200\n"

   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*m0) ,"m" (*v1) );
}

void __attribute__((noinline)) vhtfm3(ScePspFVector4 *v0, ScePspFMatrix4 *m0, ScePspFVector4 *v1)
{
	asm volatile (
   "lv.q   C100, 0x00+%1\n"
   "lv.q   C110, 0x10+%1\n"
   "lv.q   C120, 0x20+%1\n"
   "lv.q   C130, 0x30+%1\n"

   "lv.q   C200, %2\n"

   "lv.q   C000, %0\n"

   "vhtfm3.t C000, E100, C200\n"

   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*m0) ,"m" (*v1) );
}

void __attribute__((noinline)) vtfm2(ScePspFVector4 *v0, ScePspFMatrix4 *m0, ScePspFVector4 *v1)
{
	asm volatile (
   "lv.q   C100, 0x00+%1\n"
   "lv.q   C110, 0x10+%1\n"
   "lv.q   C120, 0x20+%1\n"
   "lv.q   C130, 0x30+%1\n"

   "lv.q   C200, %2\n"

   "lv.q   C000, %0\n"

   "vtfm2.p C000, E100, C200\n"

   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*m0) ,"m" (*v1) );
}

void __attribute__((noinline)) vhtfm2(ScePspFVector4 *v0, ScePspFMatrix4 *m0, ScePspFVector4 *v1)
{
	asm volatile (
   "lv.q   C100, 0x00+%1\n"
   "lv.q   C110, 0x10+%1\n"
   "lv.q   C120, 0x20+%1\n"
   "lv.q   C130, 0x30+%1\n"

   "lv.q   C200, %2\n"

   "lv.q   C000, %0\n"

   "vhtfm2.p C000, E100, C200\n"

   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*m0) ,"m" (*v1) );
}

ScePspFVector4 v0;
ScePspFMatrix4 m0;
ScePspFVector4 v1;

void initValues()
{
	// Reset output values
	v0.x = 0;
	v0.y = 0;
	v0.z = 0;
	v0.w = 0;

	// Some random input values...
	v1.x = 17;
	v1.y = 13;
	v1.z = -5;
	v1.w = 11;

	m0.x.x = -23;
	m0.x.y = -9;
	m0.x.z = 17;
	m0.x.w = -13;

	m0.y.x = 12;
	m0.y.y = 26;
	m0.y.z = -11;
	m0.y.w = -7;

	m0.z.x = 8;
	m0.z.y = -3;
	m0.z.z = 7;
	m0.z.w = 17;

	m0.w.x = 31;
	m0.w.y = -7;
	m0.w.z = 5;
	m0.w.w = 11;
}

int main(int argc, char *argv[])
{
    SceCtrlData pad;
    int oldButtons = 0;
#define SECOND       1000000
#define REPEAT_START (1 * SECOND)
#define REPEAT_DELAY (SECOND / 5)
    struct timeval repeatStart;
    struct timeval repeatDelay;

    repeatStart.tv_sec = 0;
    repeatStart.tv_usec = 0;
    repeatDelay.tv_sec = 0;
    repeatDelay.tv_usec = 0;

	pspDebugScreenInit();
	printf("Press Cross to start test\n");
	printf("Press Triangle to exit\n");

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
			initValues();
			vtfm4(&v0, &m0, &v1);
			printf("vtfm4 : %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			vhtfm4(&v0, &m0, &v1);
			printf("vhtfm4: %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			vtfm3(&v0, &m0, &v1);
			printf("vtfm3 : %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			vhtfm3(&v0, &m0, &v1);
			printf("vhtfm3: %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			vtfm2(&v0, &m0, &v1);
			printf("vtfm2 : %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			vhtfm2(&v0, &m0, &v1);
			printf("vhtfm2: %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);
		}

		if (buttonDown & PSP_CTRL_TRIANGLE)
		{
			done = 1;
		}

		oldButtons = pad.Buttons;
		sceDisplayWaitVblank();
	}

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
