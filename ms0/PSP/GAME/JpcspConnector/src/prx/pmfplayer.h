/*
 *	PMF Player Module
 *	Copyright (c) 2006 by Sorin P. C. <magik@hypermagik.com>
 */
#ifndef __PMFPLAYER_H__
#define __PMFPLAYER_H__

#include <pspkernel.h>
#include "pspmpeg.h"
#include <psptypes.h>

#include "decoder.h"
#include "reader.h"
#include "audio.h"
#include "video.h"
#include "debug.h"

class JpcspConnector;

class CPMFPlayer
{

public:

	CPMFPlayer(void);
	~CPMFPlayer(void);

	char*				CPMFPlayer::GetLastError();
	SceInt32			CPMFPlayer::Initialize(SceInt32 nPackets = 0x3C0);
	SceInt32			CPMFPlayer::Load(char* pFileName);
	SceInt32			CPMFPlayer::Play(JpcspConnector *Connector);
	SceVoid				CPMFPlayer::Shutdown();

private:

	SceInt32 			CPMFPlayer::ParseHeader();

	char				m_LastError[256];

	SceUID				m_FileHandle;
	SceInt32			m_MpegStreamOffset;
	SceInt32			m_MpegStreamSize;
	
	SceMpeg				m_Mpeg;
	SceInt32			m_MpegMemSize;
	ScePVoid			m_MpegMemData;

	SceInt32			m_RingbufferPackets;
	SceInt32			m_RingbufferSize;
	ScePVoid			m_RingbufferData;
	SceMpegRingbuffer	m_Ringbuffer;

	SceMpegStream*		m_MpegStreamAVC;
	ScePVoid			m_pEsBufferAVC;
	SceMpegAu			m_MpegAuAVC;

	SceMpegStream*		m_MpegStreamAtrac;
	ScePVoid			m_pEsBufferAtrac;
	SceMpegAu			m_MpegAuAtrac;

	SceInt32			m_MpegAtracEsSize;
	SceInt32			m_MpegAtracOutSize;

	SceInt32			m_iLastTimeStamp;

	DecoderThreadData	Decoder;
	SceInt32			CPMFPlayer::InitDecoder(JpcspConnector *Connector);
	SceInt32			CPMFPlayer::ShutdownDecoder();

	ReaderThreadData	Reader;
	SceInt32			CPMFPlayer::InitReader();
	SceInt32			CPMFPlayer::ShutdownReader();

	VideoThreadData		Video;
	SceInt32			CPMFPlayer::InitVideo();
	SceInt32			CPMFPlayer::ShutdownVideo();

	AudioThreadData		Audio;
	SceInt32			CPMFPlayer::InitAudio();
	SceInt32			CPMFPlayer::ShutdownAudio();
};

#define SWAPINT(x) (((x)<<24) | (((uint)(x)) >> 24) | (((x) & 0x0000FF00) << 8) | (((x) & 0x00FF0000) >> 8))

#endif
