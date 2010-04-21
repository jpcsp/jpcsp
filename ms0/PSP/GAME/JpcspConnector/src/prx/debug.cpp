/*
 *	PMF Player Module
 *	Copyright (c) 2006 by Sorin P. C. <magik@hypermagik.com>
 */
#include "debug.h"
#include <stdio.h>
#include <string.h>

extern "C" void debug(char *s)
{
#if DEBUG
	SceUID fd = sceIoOpen("ms0:/tmp/JpcspConnector.log", PSP_O_APPEND | PSP_O_WRONLY | PSP_O_CREAT, 0777);
	sceIoWrite(fd, s, strlen(s));
	sceIoWrite(fd, "\n", 1);
	sceIoClose(fd);
#endif
}
