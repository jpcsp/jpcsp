/*
 *	Jpcsp Connector
 */
#ifndef __JPCSPCONNECTOR_H__
#define __JPCSPCONNECTOR_H__

#include <pspkernel.h>
#include <psptypes.h>
#include "pmfplayer.h"
#include "video.h"
#include "reader.h"

#define KB(n)	((n) * 1024)
#define MB(n)	(KB(n) * 1024)
#define GB(n)	(MB(n) * 1024)
#define MAX_FILE_SIZE	MB(8)

#define FILE_FORMAT_VERSION		1

#define NUMBER_STREAMS	2
#define VIDEO_STREAM	0
#define AUDIO_STREAM	1

#define ENCODE_PREVIOUS_FLAG	0x80000000
#define ENCODE_RLE_FLAG			0x00000000
#define ENCODE_PREVIOUS(c, i)	(c | (i << 24) | ENCODE_PREVIOUS_FLAG)
#define ENCODE_RLE(c, i)		(c | (i << 24) | ENCODE_RLE_FLAG     )
#define ENCODE_NONE(c)			ENCODE_RLE(c, 0)
#define PIXEL_VALUE(c)			((c) & 0x00FFFFFF)

class CPMFPlayer;

class JpcspConnector
{
public:

	JpcspConnector(void);
	~JpcspConnector(void);
	void run(SceSize _argc, void* _argp);
	void initConnector();
	void exitConnector();
	void sendVideoFrame(int videoFrameCount, void *videoBuffer, SceInt32 videoTimeStamp, ReaderThreadData* D, SceUInt32 packetsConsumed);
	void sendAudioFrame(int audioFrameCount, void *audioBuffer, int audioBufferLength, SceInt32 audioTimeStamp);

private:
	int currentFileSize[NUMBER_STREAMS];
	SceUID currentFd[NUMBER_STREAMS];
	char *currentFileBuffer[NUMBER_STREAMS];
	char *streamName[NUMBER_STREAMS];
	int startFrameCount[NUMBER_STREAMS];
	char sendBuffer[DRAW_BUFFER_SIZE];
	char previousVideoBuffer[DRAW_BUFFER_SIZE];
	CPMFPlayer *Player;
	int usbActivated;

	void initialize();
	char *getDirectoryName(char *name);
	char *getRawFileName(char *name, char *prefix, int frameCount);
	char *getFileName(char *result, char *name);
	void createDirectories();
	int compressVideo(void *destinationBuffer, void *videoBuffer, void *previousVideoBuffer);
	void writeCurrentFile(int stream, void *buffer, int length);
	void closeCurrentFile(int stream);
	int openCurrentFile(int stream, int videoFrameCount);
	void flushCurrentFile(int stream);
	void writeVersion(int stream);
	int writeFrame(int stream, int frameCount, void *buffer, int length, SceInt32 timeStamp, void *additionalBuffer, int additionalLength);
	int loadStartModule(char *path);
	int readLine(SceUID fd, char *data, SceSize size);
	int executeCommand();
	int commandDecodeVideo(char *parameters);
	int commandExit(char *parameters);
	void activateUsb();
	void deactivateUsb();
	void waitForRemoteCompletion();
	void refreshMemoryStick();
	int getMemoryStickFreeSizeKb();
};

#endif
