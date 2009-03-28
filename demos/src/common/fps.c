
#include <pspkernel.h>

#include <string.h>

#include "fps.h"

static struct FpsInfo
{
    unsigned int samples[8];
    int sampleposition;
    int samplesvalid;
    unsigned int timestamp;
    unsigned int averagetime;
} fps;

void initFps()
{
    memset(&fps, 0, sizeof(fps));
}

void updateFps()
{
    unsigned int now = sceKernelGetSystemTimeLow();
    int i;

    fps.samples[fps.sampleposition] = now - fps.timestamp;
    fps.timestamp = now;
    fps.sampleposition = (fps.sampleposition + 1) % 8;

    if (fps.samplesvalid < 8)
    {
        fps.samplesvalid++;
    }

    fps.averagetime = 0;
    for (i = 0; i < 8; i++)
    {
        fps.averagetime += fps.samples[i];
    }
    fps.averagetime /= fps.samplesvalid;
}

unsigned int getFps()
{
    if (fps.averagetime != 0)
    {
        float x = 1.0f / fps.averagetime;
        float y = x * 1000000.0f;
        return (int)(y + 0.5f);
    }
    return 0;
}

unsigned int getUpf()
{
    return fps.averagetime;
}
