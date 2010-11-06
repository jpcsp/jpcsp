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
int inDebug = 0;
#endif

extern "C" void debug(char *s)
{
#if DEBUG
    while (inDebug)
	{
	}

	inDebug = 1;

#if BUFFERED_DEBUG
	int length = strlen(s);
	if (debugBufferIndex + length >= DEBUG_BUFFER_LENGTH - 5)
	{
		debugFlush();
	}

	strcpy(debugBuffer + debugBufferIndex, s);
	debugBufferIndex += length;
	strcpy(debugBuffer + debugBufferIndex, "\n");
	debugBufferIndex += 1;
#else
	strcpy(debugBuffer, s);
	strcat(debugBuffer, "\n");
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
