/*
 *	PMF Player Module
 *	Copyright (c) 2006 by Sorin P. C. <magik@hypermagik.com>
 */
#include "pmfplayer.h"
#include <psppower.h>
#include <pspctrl.h>
#include <pspusb.h>
#include <pspusbstor.h>
#include "debug.h"

#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#define DEBUG_TIMING	1

SceInt32 IsRingbufferFull(ReaderThreadData* D)
{
	if(D->m_Status == ReaderThreadData::READER_EOF)
		return 1;

	if(sceMpegRingbufferAvailableSize(D->m_Ringbuffer) > 0)
		return 0;

	return 1;
}


int T_Decoder(SceSize _args, void *_argp)
{
	int retVal;
	int oldButtons = 0;
	SceCtrlData pad;
#if DEBUG_TIMING
	int start, end;
	char s[100];
#endif

	int iInitAudio = 1;
	SceInt32 iVideoStatus = 0;
	int videoFrameCount = 0;
	int audioFrameCount = 0;

	SceInt32 unknown = 0;

	int iThreadsRunning = 0;

	SceInt32 m_iAudioCurrentTimeStamp = 0;
	SceInt32 m_iVideoCurrentTimeStamp = 0;
	SceInt32 m_iVideoLastTimeStamp = 0;

	DecoderThreadData* D = *((DecoderThreadData**)_argp);

	SceUInt32 m_iLastPacketsWritten = D->Reader->m_Ringbuffer->iUnk1;
	SceInt32  m_iLastPacketsAvailable = sceMpegRingbufferAvailableSize(D->Reader->m_Ringbuffer);

	D->Connector->initConnector();

	for(;;)
	{
		sceKernelDelayThread(1);

		scePowerTick(0);

		sceCtrlReadBufferPositive(&pad, 1);
		int buttonDown = (oldButtons ^ pad.Buttons) & pad.Buttons;

		if (buttonDown & PSP_CTRL_CIRCLE)
		{
			break;
		}

		if( iThreadsRunning == 0 &&
			IsRingbufferFull(D->Reader) &&
			D->Video->m_iNumBuffers == D->Video->m_iFullBuffers)
		{
			iThreadsRunning = 1;
			sceKernelSignalSema(D->Video->m_SemaphoreStart, 1);
			sceKernelSignalSema(D->Audio->m_SemaphoreStart, 1);
		}

		if(D->Reader->m_Status == ReaderThreadData::READER_ABORT)
		{
			break;
		}
		else if(D->Reader->m_Status == ReaderThreadData::READER_EOF)
		{
			retVal = sceMpegRingbufferAvailableSize(D->Reader->m_Ringbuffer);

			if(retVal == D->Reader->m_RingbufferPackets)
				break;
		}

		if(!IsRingbufferFull(D->Reader))
		{
			sceKernelWaitSema(D->Reader->m_Semaphore, 1, 0);

			if(D->Reader->m_Status == ReaderThreadData::READER_ABORT)
				break;
		}

		if(D->Audio->m_iFullBuffers < D->Audio->m_iNumBuffers)
		{
#if DEBUG_TIMING
			start = sceKernelGetSystemTimeLow();
#endif
			retVal = sceMpegGetAtracAu(&D->m_Mpeg, D->m_MpegStreamAtrac, D->m_MpegAuAtrac, &unknown);
#if DEBUG_TIMING
			end = sceKernelGetSystemTimeLow();
			sprintf(s, "Duration sceMpegGetAtracAu %d, return %X, presentation timeStamp %d (%d), decode timeStamp %d", end - start, retVal, D->m_MpegAuAtrac->iPts, m_iAudioCurrentTimeStamp + D->m_iAudioFrameDuration, D->m_MpegAuAtrac->iDts);
			debug(s);
			debug(D->m_MpegAuAtrac);
			debug(D->Reader->m_Ringbuffer);
#endif
			if(retVal != 0)
			{
				if(!IsRingbufferFull(D->Reader))
				{
					sceKernelWaitSema(D->Reader->m_Semaphore, 1, 0);

					if(D->Reader->m_Status == ReaderThreadData::READER_ABORT)
						break;
				}
			}
			else
			{
				if(m_iAudioCurrentTimeStamp >= D->m_iLastTimeStamp - D->m_iVideoFrameDuration)
					break;

#if DEBUG_TIMING
				start = sceKernelGetSystemTimeLow();
#endif
				retVal = sceMpegAtracDecode(&D->m_Mpeg, D->m_MpegAuAtrac, D->Audio->m_pAudioBuffer[D->Audio->m_iDecodeBuffer], iInitAudio);
#if DEBUG_TIMING
				end = sceKernelGetSystemTimeLow();
				sprintf(s, "Duration sceMpegAtracDecode %d, return %X", end - start, retVal);
				debug(s);
				debug(D->Reader->m_Ringbuffer);
#endif
				if(retVal != 0)
				{
					sprintf(D->m_LastError, "sceMpegAtracDecode() failed: 0x%08X", retVal);
					break;
				}

				if(D->m_MpegAuAtrac->iPts == 0xFFFFFFFF)
					m_iAudioCurrentTimeStamp += D->m_iAudioFrameDuration;
				else
					m_iAudioCurrentTimeStamp = D->m_MpegAuAtrac->iPts;

				if(m_iAudioCurrentTimeStamp <= 0x15F90 /* video start ts */ - D->m_iAudioFrameDuration)
					iInitAudio = 1;

				D->Audio->m_iBufferTimeStamp[D->Audio->m_iDecodeBuffer] = m_iAudioCurrentTimeStamp;

				if(iInitAudio == 0)
				{
					D->Connector->sendAudioFrame(audioFrameCount, D->Audio->m_pAudioBuffer[D->Audio->m_iDecodeBuffer], D->m_MpegAtracOutSize, m_iAudioCurrentTimeStamp);
					audioFrameCount++;

					sceKernelWaitSema(D->Audio->m_SemaphoreLock, 1, 0);

					D->Audio->m_iFullBuffers++;

					sceKernelSignalSema(D->Audio->m_SemaphoreLock, 1);

					D->Audio->m_iDecodeBuffer = (D->Audio->m_iDecodeBuffer + 1) % D->Audio->m_iNumBuffers;
				}

				iInitAudio = 0;
			}
		}

		if(!IsRingbufferFull(D->Reader))
		{
			sceKernelWaitSema(D->Reader->m_Semaphore, 1, 0);

			if(D->Reader->m_Status == ReaderThreadData::READER_ABORT)
				break;
		}

		if(D->Video->m_iFullBuffers < D->Video->m_iNumBuffers)
		{
#if DEBUG_TIMING
			start = sceKernelGetSystemTimeLow();
#endif
			retVal = sceMpegGetAvcAu(&D->m_Mpeg, D->m_MpegStreamAVC, D->m_MpegAuAVC, &unknown);
#if DEBUG_TIMING
			end = sceKernelGetSystemTimeLow();
			sprintf(s, "Duration sceMpegGetAvcAu %d, return %X, presentation timeStamp %d (%d), decode timeStamp %d", end - start, retVal, D->m_MpegAuAVC->iPts, m_iVideoCurrentTimeStamp + 3004, D->m_MpegAuAVC->iDts);
			debug(s);
			debug(D->m_MpegAuAVC);
			debug(D->Reader->m_Ringbuffer);
#endif
			if((SceUInt32)retVal == 0x80618001)
			{
				if(!IsRingbufferFull(D->Reader))
				{
					sceKernelWaitSema(D->Reader->m_Semaphore, 1, 0);

					if(D->Reader->m_Status == ReaderThreadData::READER_ABORT)
						break;
				}
			}
			else if(retVal != 0)
			{
				sprintf(D->m_LastError, "sceMpegGetAvcAu() failed: 0x%08X", retVal);
				break;
			}
			else
			{
				if(m_iVideoCurrentTimeStamp >= D->m_iLastTimeStamp - D->m_iVideoFrameDuration)
					break;

#if DEBUG_TIMING
				start = sceKernelGetSystemTimeLow();
#endif
				retVal = sceMpegAvcDecode(&D->m_Mpeg, D->m_MpegAuAVC, BUFFER_WIDTH, &D->Video->m_pVideoBuffer[D->Video->m_iPlayBuffer], &iVideoStatus);
#if DEBUG_TIMING
				end = sceKernelGetSystemTimeLow();
				sprintf(s, "Duration sceMpegAvcDecode %d, return %X", end - start, retVal);
				debug(s);
				debug(D->Reader->m_Ringbuffer);
#endif
				if(retVal != 0)
				{
					sprintf(D->m_LastError, "sceMpegAvcDecode() failed: 0x%08X", retVal);
					break;
				}

				if(D->m_MpegAuAVC->iPts == 0xFFFFFFFF)
					m_iVideoCurrentTimeStamp += 0x0BBC;
				else
					m_iVideoCurrentTimeStamp = D->m_MpegAuAVC->iPts;

				if(iVideoStatus == 1)
				{
					SceUInt32 m_iPacketsWritten = D->Reader->m_Ringbuffer->iUnk1;
					SceInt32  m_iPacketsAvailable = sceMpegRingbufferAvailableSize(D->Reader->m_Ringbuffer);

					SceInt32  m_iDeltaPacketsWritten = m_iPacketsWritten - m_iLastPacketsWritten;
					if (m_iDeltaPacketsWritten < 0)
					{
						m_iDeltaPacketsWritten += D->Reader->m_Ringbuffer->iPackets;
					}
					SceInt32 m_iDeltaPacketsAvailable = m_iPacketsAvailable - m_iLastPacketsAvailable;
					SceInt32 m_iConsumedPackets = m_iDeltaPacketsAvailable + m_iDeltaPacketsWritten;

					m_iLastPacketsWritten = m_iPacketsWritten;
					m_iLastPacketsAvailable = m_iPacketsAvailable;

					D->Connector->sendVideoFrame(videoFrameCount, D->Video->m_pVideoBuffer[D->Video->m_iPlayBuffer], m_iVideoCurrentTimeStamp, D->Reader, m_iConsumedPackets);
					videoFrameCount++;

					D->Video->m_iBufferTimeStamp[D->Video->m_iPlayBuffer] = m_iVideoLastTimeStamp;

					sceKernelWaitSema(D->Video->m_SemaphoreLock, 1, 0);

					D->Video->m_iFullBuffers++;

					sceKernelSignalSema(D->Video->m_SemaphoreLock, 1);
				}

				m_iVideoLastTimeStamp = m_iVideoCurrentTimeStamp;
			}
		}

		if(!IsRingbufferFull(D->Reader))
		{
			sceKernelWaitSema(D->Reader->m_Semaphore, 1, 0);

			if(D->Reader->m_Status == ReaderThreadData::READER_ABORT)
				break;
		}

	}

	D->Connector->exitConnector();

	sceKernelSignalSema(D->Audio->m_SemaphoreStart, 1);
	sceKernelSignalSema(D->Video->m_SemaphoreStart, 1);

	D->Reader->m_Status = ReaderThreadData::READER_ABORT;

	D->Audio->m_iAbort = 1;

	while(D->Video->m_iFullBuffers > 0)
	{
		sceKernelWaitSema(D->Video->m_SemaphoreWait, 1, 0);
		sceKernelSignalSema(D->Video->m_SemaphoreLock, 1);
	}

	sceMpegAvcDecodeStop(&D->m_Mpeg, BUFFER_WIDTH, D->Video->m_pVideoBuffer, &iVideoStatus);

	if(iVideoStatus > 0)
	{
		sceKernelWaitSema(D->Video->m_SemaphoreLock, 1, 0);

		D->Video->m_iFullBuffers++;

		sceKernelSignalSema(D->Video->m_SemaphoreLock, 1);
	}

	D->Video->m_iAbort = 1;

	sceMpegFlushAllStream(&D->m_Mpeg);

	sceKernelExitThread(0);

	return 0;
}

SceInt32 CPMFPlayer::InitDecoder(JpcspConnector *Connector)
{
	Decoder.m_ThreadID	= sceKernelCreateThread("decoder_thread", T_Decoder, 0x10, 0x10000, PSP_THREAD_ATTR_USER, NULL);

	if(Decoder.m_ThreadID < 0)
	{
		sprintf(m_LastError, "sceKernelCreateThread() failed: 0x%08X", (int)Decoder.m_ThreadID);
		return -1;
	}

	Decoder.Reader				= &Reader;
	Decoder.Video				= &Video;
	Decoder.Audio				= &Audio;
	Decoder.Connector			= Connector;
	Decoder.m_Mpeg				= m_Mpeg;
	Decoder.m_MpegStreamAVC		= m_MpegStreamAVC;
	Decoder.m_MpegAuAVC			= &m_MpegAuAVC;
	Decoder.m_MpegStreamAtrac	= m_MpegStreamAtrac;
	Decoder.m_MpegAuAtrac		= &m_MpegAuAtrac;
	Decoder.m_LastError			= m_LastError;
	Decoder.m_MpegAtracOutSize  = m_MpegAtracOutSize;

	Decoder.m_iAudioFrameDuration = 4180; // ??
	Decoder.m_iVideoFrameDuration = (int)(90000 / 29.97);
	Decoder.m_iLastTimeStamp      = m_iLastTimeStamp;

	return 0;
}

SceInt32 CPMFPlayer::ShutdownDecoder()
{
	sceKernelDeleteThread(Decoder.m_ThreadID);

	return 0;
}
