#include <pspkernel.h>
#include <pspdebug.h>
#include <pspctrl.h>
#include <pspdisplay.h>
#include <pspgu.h>
#include <pspgum.h>
#include <pspaudio.h>

#include <sys/stat.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>

PSP_MODULE_INFO("Sound Test", 0, 1, 0);
PSP_MAIN_THREAD_ATTR(THREAD_ATTR_USER);

/* Define printf, just to make typing easier */
#define printf  pspDebugScreenPrintf

int done = 0;

int channel;
#define SAMPLE_SIZE	(64 * 20)
#define VOLUME		32000
char buffer1[SAMPLE_SIZE];
char buffer2[SAMPLE_SIZE];

char text[10000];
struct timeval previousAudioOutput;


void audioOutput(char * buffer, int blocking)
{
	struct timeval start;
	gettimeofday(&start, NULL);
	int result;

	if (blocking)
	{
		result = sceAudioOutputBlocking(channel, VOLUME, buffer);
	}
	else
	{
		result = sceAudioOutput(channel, VOLUME, buffer);
	}
	struct timeval end;
	gettimeofday(&end, NULL);
	int duration_usec = (end.tv_sec - start.tv_sec) * 1000000 + (end.tv_usec - start.tv_usec);
	int interval_usec = (start.tv_sec - previousAudioOutput.tv_sec) * 1000000 + (start.tv_usec - previousAudioOutput.tv_usec);

	char s[1000];
	sprintf(s, "Interval %d usec, result %d, duration: %d usec\n", interval_usec, result, duration_usec);

	strcat(text, s);

	gettimeofday(&previousAudioOutput, NULL);
}

void audioOutput2(char *buffer, int volume) {
	int i;

	printf("sceAudioOutput2OutputBlocking volume=0x%05X\n", volume);
	for (i = 0; i < 10; i++) {
		sceAudioOutput2OutputBlocking(volume, buffer);
	}
}


int main(int argc, char *argv[])
{
    SceCtrlData pad;
    int oldButtons = 0;
    int i;
#define SECOND       1000000
#define REPEAT_START (1 * SECOND)
#define REPEAT_DELAY (SECOND / 5)
    struct timeval repeatStart;
    struct timeval repeatDelay;

    repeatStart.tv_sec = 0;
    repeatStart.tv_usec = 0;
    repeatDelay.tv_sec = 0;
    repeatDelay.tv_usec = 0;

    for (i = 0; i < SAMPLE_SIZE; i += 2)
    {
	buffer1[i+0] = 0x00;
	buffer1[i+1] = 0x70;
	buffer2[i+0] = 0x00;
	buffer2[i+1] = 0x10;
    }

    pspDebugScreenInit();

    printf("Triangle - Exit\n");
    printf("Cross - Test Blocking Audio\n");
    printf("Square - Test non-Blocking Audio\n");
    printf("Circle - Test Audio2\n");

    channel = sceAudioChReserve(PSP_AUDIO_NEXT_CHANNEL, SAMPLE_SIZE, PSP_AUDIO_FORMAT_STEREO);
    if (channel < 0)
    {
        sceKernelExitGame();
    }

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
		strcpy(text, "Start Test:\n");
		gettimeofday(&previousAudioOutput, NULL);
		for (i = 0; i < 10; i++)
		{
			audioOutput(buffer1, 1);
			audioOutput(buffer2, 1);
		}
		strcat(text, "\n");

		printf(text);
        }

        if (buttonDown & PSP_CTRL_SQUARE)
        {
		strcpy(text, "Start Test:\n");
		gettimeofday(&previousAudioOutput, NULL);
		for (i = 0; i < 10; i++)
		{
			audioOutput(buffer1, 0);
			audioOutput(buffer2, 0);
		}
		strcat(text, "\n");

		printf(text);
        }

        if (buttonDown & PSP_CTRL_CIRCLE)
        {
		sceAudioOutput2Reserve(SAMPLE_SIZE / 2);

		audioOutput2(buffer1, 0xFFFFF);
		sceKernelDelayThread(1 * 1000000);
		audioOutput2(buffer1, 0x20000);
		sceKernelDelayThread(1 * 1000000);
		audioOutput2(buffer1, 0x10000);
		sceKernelDelayThread(1 * 1000000);
		audioOutput2(buffer1, 0x08000);
		sceKernelDelayThread(1 * 1000000);
		audioOutput2(buffer1, 0x04000);
		sceKernelDelayThread(1 * 1000000);
		audioOutput2(buffer1, 0x02000);
		sceKernelDelayThread(1 * 1000000);
		audioOutput2(buffer1, 0x01000);
		sceKernelDelayThread(1 * 1000000);
		audioOutput2(buffer1, 0x00800);
        }

        if (buttonDown & PSP_CTRL_RIGHT)
        {
        }

        if (buttonDown & PSP_CTRL_UP)
        {
        }

        if (buttonDown & PSP_CTRL_DOWN)
        {
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

