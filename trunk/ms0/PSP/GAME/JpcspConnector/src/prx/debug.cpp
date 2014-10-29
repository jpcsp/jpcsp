/*
 *	PMF Player Module
 *	Copyright (c) 2006 by Sorin P. C. <magik@hypermagik.com>
 */
#include "debug.h"
#include <stdio.h>
#include <string.h>

#define BUFFERED_DEBUG	1

#if DEBUG
#define DEBUG_BUFFER_LENGTH	9000
char debugBuffer[DEBUG_BUFFER_LENGTH];
int debugBufferIndex = 0;
volatile int inDebug = 0;
#endif

extern "C" void debug(char *s)
{
#if DEBUG
	while (inDebug)
	{
		sceKernelDelayThread(1000);
	}

	inDebug = 1;

	int length = strlen(s);
	if (debugBufferIndex + length >= DEBUG_BUFFER_LENGTH - 5)
	{
		debugFlush();
	}

	memcpy(debugBuffer + debugBufferIndex, s, length);
	debugBufferIndex += length;
	debugBuffer[debugBufferIndex++] = '\n';
#if !BUFFERED_DEBUG
	debugFlush();
#endif

	inDebug = 0;
#endif
}

extern "C" void debugFlush()
{
#if DEBUG
	SceUID fd = sceIoOpen("ms0:/tmp/JpcspConnector.log", PSP_O_APPEND | PSP_O_WRONLY | PSP_O_CREAT, 0777);
	sceIoWrite(fd, debugBuffer, debugBufferIndex);
	sceIoClose(fd);
	debugBufferIndex = 0;
#endif
}

void debug(SceMpegRingbuffer *ringbuffer)
{
	char s[300];
	sprintf(s, "Ringbuffer iPackets=%d, iUnk0=%d, iUnk1=%d, iUnk2=%d, iUnk3=%d, pData=0x%08X, Callback=0x%08X", ringbuffer->iPackets, ringbuffer->iUnk0, ringbuffer->iUnk1, ringbuffer->iUnk2, ringbuffer->iUnk3, u32(ringbuffer->pData), u32(ringbuffer->Callback));
	debug(s);
	sprintf(s, "           pCBparam=0x%08X, iUnk4=0x%08X, iUnk5=%d, pSceMpeg=0x%08X", u32(ringbuffer->pCBparam), ringbuffer->iUnk4, ringbuffer->iUnk5, u32(ringbuffer->pSceMpeg));
	debug(s);
}

void debug(SceMpegAu *mpegAu)
{
	char s[300];
	sprintf(s, "Au iPts=%d, iDts=%d, iEsBuffer=0x%08X, iAuSize=0x%X", mpegAu->iPts, mpegAu->iDts, mpegAu->iEsBuffer, mpegAu->iAuSize);
	debug(s);
}

