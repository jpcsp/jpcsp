/*
 *	PMF Player Module
 *	Copyright (c) 2006 by Sorin P. C. <magik@hypermagik.com>
 */
#ifndef __PMFPLAYERAUDIO_H__
#define __PMFPLAYERAUDIO_H__

#include <pspkernel.h>
#include "pspmpeg.h"
#include <psptypes.h>

typedef struct AudioThreadData
{
	SceUID				m_SemaphoreStart;
	SceUID				m_SemaphoreLock;
	SceUID				m_ThreadID;

	SceInt32			m_AudioChannel;

	ScePVoid			m_pAudioBuffer[4];
	SceInt32			m_iBufferTimeStamp[4];

	SceInt32			m_iNumBuffers;
	SceInt32			m_iFullBuffers;
	SceInt32			m_iPlayBuffer;
	SceInt32			m_iDecodeBuffer;

	SceInt32			m_iAbort;

	char*				m_LastError;

} AudioThreadData;

#endif
