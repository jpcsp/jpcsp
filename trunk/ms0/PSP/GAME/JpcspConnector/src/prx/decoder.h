/*
 *	PMF Player Module
 *	Copyright (c) 2006 by Sorin P. C. <magik@hypermagik.com>
 */
#ifndef __PMFPLAYERDECODER_H__
#define __PMFPLAYERDECODER_H__

#include <pspkernel.h>
#include "pspmpeg.h"
#include <psptypes.h>

#include "reader.h"
#include "audio.h"
#include "video.h"
#include "JpcspConnector.h"

class JpcspConnector;

typedef struct DecoderThreadData
{
	SceUID				m_ThreadID;

	ReaderThreadData*	Reader;
	VideoThreadData*	Video;
	AudioThreadData*	Audio;
	JpcspConnector*		Connector;

	SceMpeg				m_Mpeg;

	SceMpegStream*		m_MpegStreamAVC;
	SceMpegAu*			m_MpegAuAVC;
	SceMpegStream*		m_MpegStreamAtrac;
	SceMpegAu*			m_MpegAuAtrac;
	SceInt32			m_MpegAtracOutSize;
	SceMpegStream*		m_MpegStreamUserdata;
	SceMpegAu*			m_MpegAuUserdata;

	SceInt32			m_iAudioFrameDuration;
	SceInt32			m_iVideoFrameDuration;
	SceInt32			m_iLastTimeStamp;

	char*				m_LastError;

} DecoderThreadData;

#endif
