/*
 *	PMF Player Module
 *	Copyright (c) 2006 by Sorin P. C. <magik@hypermagik.com>
 */
#ifndef __PMFPLAYERVIDEO_H__
#define __PMFPLAYERVIDEO_H__

#include <pspkernel.h>
#include "pspmpeg.h"
#include <psptypes.h>

#define PIXEL_SIZE          4

#define SCREEN_W            480
#define SCREEN_H            272
#define DRAW_BUFFER_SIZE    512 * SCREEN_H * PIXEL_SIZE
#define DISP_BUFFER_SIZE    512 * SCREEN_H * PIXEL_SIZE

#define TEXTURE_W           512
#define TEXTURE_H           512
#define TEXTURE_SIZE        TEXTURE_W * TEXTURE_H * PIXEL_SIZE

#define BUFFER_WIDTH		512

struct Vertex
{
	short u,v;
	short x,y,z;
};

typedef struct VideoThreadData
{
	SceUID				m_SemaphoreStart;
	SceUID				m_SemaphoreWait;
	SceUID				m_SemaphoreLock;
	SceUID				m_ThreadID;

	ScePVoid			m_pVideoBuffer[2];
	SceInt32			m_iBufferTimeStamp[2];

	SceInt32			m_iNumBuffers;
	SceInt32			m_iFullBuffers;
	SceInt32			m_iPlayBuffer;

	SceInt32			m_iAbort;

	SceInt32			m_iWidth;
	SceInt32			m_iHeight;

	char*				m_LastError;

} VideoThreadData;

#endif
