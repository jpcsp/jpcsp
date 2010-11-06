/*
 *	PMF Player Module
 *	Copyright (c) 2006 by Sorin P. C. <magik@hypermagik.com>
 */
#ifndef __JPCSPDEBUG_H__
#define __JPCSPDEBUG_H__

#include <pspkernel.h>
#include <psptypes.h>

#define DEBUG	1

#if DEBUG
#ifdef __cplusplus
extern "C"
{
#endif
void debug(char *s);
void debugFlush();
#ifdef __cplusplus
}
#endif 
#endif

#endif
