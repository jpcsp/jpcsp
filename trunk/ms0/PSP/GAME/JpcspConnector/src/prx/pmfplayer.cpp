/*
 *	PMF Player Module
 *	Copyright (c) 2006 by Sorin P. C. <magik@hypermagik.com>
 */
#include <pspkernel.h>
#include <pspsdk.h>

#include <stdio.h>
#include <malloc.h>
#include <string.h>

#include "pmfplayer.h"

CPMFPlayer::CPMFPlayer(void)
{
	m_RingbufferData	= NULL;
	m_MpegMemData		= NULL;

	m_FileHandle		= -1;

	m_MpegStreamAVC		= NULL;
	m_MpegStreamAtrac	= NULL;

	m_pEsBufferAVC		= NULL;
	m_pEsBufferAtrac	= NULL;
}

CPMFPlayer::~CPMFPlayer(void)
{
}

char *CPMFPlayer::GetLastError()
{
	return m_LastError;
}

SceInt32 RingbufferCallback(ScePVoid pData, SceInt32 iNumPackets, ScePVoid pParam)
{
	int retVal, iPackets;
	SceUID hFile = *(SceUID*)pParam;

	retVal = sceIoRead(hFile, pData, iNumPackets * 2048);
	if(retVal < 0)
		return -1;

	iPackets = retVal / 2048;

	return iPackets;
}

SceInt32 CPMFPlayer::Initialize(SceInt32 nPackets)
{
	int retVal = -1;

	m_RingbufferPackets = nPackets;

	retVal = sceMpegInit();
	if(retVal != 0)
	{
		sprintf(m_LastError, "sceMpegInit() failed: 0x%08X", retVal);
		goto error;
	}

	retVal = sceMpegRingbufferQueryMemSize(m_RingbufferPackets);
	if(retVal < 0)
	{
		sprintf(m_LastError, "sceMpegRingbufferQueryMemSize(%d) failed: 0x%08X", (int)nPackets, retVal);
		goto finish;
	}

	m_RingbufferSize = retVal;

	retVal = sceMpegQueryMemSize(0);
	if(retVal < 0)
	{
		sprintf(m_LastError, "sceMpegQueryMemSize() failed: 0x%08X", retVal);
		goto finish;
	}

	m_MpegMemSize = retVal;

	m_RingbufferData = malloc(m_RingbufferSize);
	if(m_RingbufferData == NULL)
	{
		sprintf(m_LastError, "malloc() failed!");
		goto finish;
	}

	m_MpegMemData = malloc(m_MpegMemSize);
	if(m_MpegMemData == NULL)
	{
		sprintf(m_LastError, "malloc() failed!");
		goto freeringbuffer;
	}

	retVal = sceMpegRingbufferConstruct(&m_Ringbuffer, m_RingbufferPackets, m_RingbufferData, m_RingbufferSize, &RingbufferCallback, &m_FileHandle);
	if(retVal != 0)
	{
		sprintf(m_LastError, "sceMpegRingbufferConstruct() failed: 0x%08X", retVal);
		goto freempeg;
	}

	retVal = sceMpegCreate(&m_Mpeg, m_MpegMemData, m_MpegMemSize, &m_Ringbuffer, BUFFER_WIDTH, 0, 0);
	if(retVal != 0)
	{
		sprintf(m_LastError, "sceMpegCreate() failed: 0x%08X", retVal);
		goto destroyringbuffer;
	}

	SceMpegAvcMode m_MpegAvcMode;
	m_MpegAvcMode.iUnk0 = -1;
	m_MpegAvcMode.iUnk1 = 3;

	sceMpegAvcDecodeMode(&m_Mpeg, &m_MpegAvcMode);

	return 0;

destroyringbuffer:
	sceMpegRingbufferDestruct(&m_Ringbuffer);

freempeg:
	free(m_MpegMemData);

freeringbuffer:
	free(m_RingbufferData);

finish:
	sceMpegFinish();

error:
	return -1;
}

SceInt32 CPMFPlayer::Load(char* pFileName)
{
	int retVal;

	m_FileHandle = sceIoOpen(pFileName, PSP_O_RDONLY, 0777);
	if(m_FileHandle < 0)
	{
		sprintf(m_LastError, "sceIoOpen() failed!");
		return -1;
	}

	if(ParseHeader() < 0)
		return -1;

	m_MpegStreamAVC = sceMpegRegistStream(&m_Mpeg, 0, 0);
	if(m_MpegStreamAVC == NULL)
	{
		sprintf(m_LastError, "sceMpegRegistStream() failed!");
		return -1;
	}

	m_MpegStreamAtrac = sceMpegRegistStream(&m_Mpeg, 1, 0);
	if(m_MpegStreamAtrac == NULL)
	{
		sprintf(m_LastError, "sceMpegRegistStream() failed!");
		return -1;
	}

	m_pEsBufferAVC = sceMpegMallocAvcEsBuf(&m_Mpeg);
	if(m_pEsBufferAVC == NULL)
	{
		sprintf(m_LastError, "sceMpegMallocAvcEsBuf() failed!");
		return -1;
	}

	retVal = sceMpegInitAu(&m_Mpeg, m_pEsBufferAVC, &m_MpegAuAVC);
	if(retVal != 0)
	{
		sprintf(m_LastError, "sceMpegInitAu() failed: 0x%08X", retVal);
		return -1;
	}

	retVal = sceMpegQueryAtracEsSize(&m_Mpeg, &m_MpegAtracEsSize, &m_MpegAtracOutSize);
	if(retVal != 0)
	{
		sprintf(m_LastError, "sceMpegQueryAtracEsSize() failed: 0x%08X", retVal);
		return -1;
	}

	m_pEsBufferAtrac = memalign(64, m_MpegAtracEsSize);
	if(m_pEsBufferAtrac == NULL)
	{
		sprintf(m_LastError, "malloc() failed!");
		return -1;
	}

	retVal = sceMpegInitAu(&m_Mpeg, m_pEsBufferAtrac, &m_MpegAuAtrac);
	if(retVal != 0)
	{
		sprintf(m_LastError, "sceMpegInitAu() failed: 0x%08X", retVal);
		return -1;
	}

	return 0;
}

SceInt32 CPMFPlayer::ParseHeader()
{
	int retVal;
	char * pHeader = new char[2048];

	sceIoLseek(m_FileHandle, 0, SEEK_SET);

	retVal = sceIoRead(m_FileHandle, pHeader, 2048);
	if(retVal < 2048)
	{
		sprintf(m_LastError, "sceIoRead() failed!");
		goto error;
	}

	retVal = sceMpegQueryStreamOffset(&m_Mpeg, pHeader, &m_MpegStreamOffset);
	if(retVal != 0)
	{
		sprintf(m_LastError, "sceMpegQueryStreamOffset() failed: 0x%08X", retVal);
		goto error;
	}

	retVal = sceMpegQueryStreamSize(pHeader, &m_MpegStreamSize);
	if(retVal != 0)
	{
		sprintf(m_LastError, "sceMpegQueryStreamSize() failed: 0x%08X", retVal);
		goto error;
	}

	m_iLastTimeStamp = *(int*)(pHeader + 80 + 12);
	m_iLastTimeStamp = SWAPINT(m_iLastTimeStamp);

	delete[] pHeader;

	sceIoLseek(m_FileHandle, m_MpegStreamOffset, SEEK_SET);

	return 0;

error:
	delete[] pHeader;
	return -1;
}

SceVoid CPMFPlayer::Shutdown()
{
	if(m_pEsBufferAtrac != NULL)
		free(m_pEsBufferAtrac);

	if(m_pEsBufferAVC != NULL)
		sceMpegFreeAvcEsBuf(&m_Mpeg, m_pEsBufferAVC);

	if(m_MpegStreamAVC != NULL)
		sceMpegUnRegistStream(&m_Mpeg, m_MpegStreamAVC);

	if(m_MpegStreamAtrac != NULL)
		sceMpegUnRegistStream(&m_Mpeg, m_MpegStreamAtrac);

	if(m_FileHandle > -1)
		sceIoClose(m_FileHandle);

	sceMpegDelete(&m_Mpeg);

	sceMpegRingbufferDestruct(&m_Ringbuffer);

	sceMpegFinish();

	if(m_RingbufferData != NULL)
		free(m_RingbufferData);

	if(m_MpegMemData != NULL)
		free(m_MpegMemData);
}

SceInt32 CPMFPlayer::Play(JpcspConnector *Connector)
{
	int retVal, fail = 0;

	retVal = InitReader();
	if(retVal < 0)
	{
		fail++;
		goto exit_reader;
	}
	
	retVal = InitVideo();
	if(retVal < 0)
	{
		fail++;
		goto exit_video;
	}

	retVal = InitAudio();
	if(retVal < 0)
	{
		fail++;
		goto exit_audio;
	}

	retVal = InitDecoder(Connector);
	if(retVal < 0)
	{
		fail++;
		goto exit_decoder;
	}

	ReaderThreadData* TDR = &Reader;
	DecoderThreadData* TDD = &Decoder;

	sceKernelStartThread(Reader.m_ThreadID,  sizeof(void*), &TDR);
	sceKernelStartThread(Audio.m_ThreadID,   sizeof(void*), &TDD);
	sceKernelStartThread(Video.m_ThreadID,   sizeof(void*), &TDD);
	sceKernelStartThread(Decoder.m_ThreadID, sizeof(void*), &TDD);

	sceKernelWaitThreadEnd(Decoder.m_ThreadID, 0);
	sceKernelWaitThreadEnd(Video.m_ThreadID, 0);
	sceKernelWaitThreadEnd(Audio.m_ThreadID, 0);
	sceKernelWaitThreadEnd(Reader.m_ThreadID, 0);

	ShutdownDecoder();
exit_decoder:
	ShutdownAudio();
exit_audio:
	ShutdownVideo();
exit_video:
	ShutdownReader();
exit_reader:

	if(fail > 0)
		return -1;

	return 0;
}
