/*
 *	PMF Player Module
 *	Copyright (c) 2006 by Sorin P. C. <magik@hypermagik.com>
 */
#ifndef __PMFPLAYERREADER_H__
#define __PMFPLAYERREADER_H__

#include <pspkernel.h>
#include "pspmpeg.h"
#include <psptypes.h>

typedef struct ReaderThreadData
{
	SceUID				m_Semaphore;
	SceUID				m_ThreadID;

	SceInt32			m_StreamSize;
	SceMpegRingbuffer*	m_Ringbuffer;
	SceInt32			m_RingbufferPackets;
	SceInt32			m_Status;
	SceInt32			m_TotalBytes;

	enum
	{
		READER_OK = 0,
		READER_EOF,
		READER_ABORT
	};

	char*				m_LastError;

} ReaderThreadData;

#endif
