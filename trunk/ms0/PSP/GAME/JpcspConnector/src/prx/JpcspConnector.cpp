/*
 *	Jpcsp Connector
 */
#include "JpcspConnector.h"
#include <psppower.h>
#include <pspctrl.h>
#include <pspusb.h>
#include <pspusbstor.h>
#include <pspdisplay.h>
#include <pspatrac3.h>
#include <pspaudio.h>
#include <time.h>

#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#define	DAEMON	0
#define USE_USB	0
#define DEBUG_TIMING	0
#define DEBUG_ATRAC		0

#if DEBUG_ATRAC
extern "C" {
	int sceAtracGetStreamDataInfo(int atracID, u8** writePointer, u32* availableBytes, u32* readOffset);
	int sceAtracGetNextDecodePosition(int atracID, int *position);
}
#endif

JpcspConnector::JpcspConnector(void)
{
}


JpcspConnector::~JpcspConnector(void)
{
}


void JpcspConnector::initialize()
{
	createDirectories();

	for (int stream = 0; stream < NUMBER_STREAMS; stream++)
	{
		currentFileSize[stream] = 0;
		currentFd[stream] = -1;
		startFrameCount[stream] = 0;
		currentFileBuffer[stream] = NULL;
	}
	streamName[VIDEO_STREAM] = "VideoStream";
	streamName[AUDIO_STREAM] = "AudioStream";

#if USE_USB
	//setup USB drivers
	int retVal = sceUsbStart(PSP_USBBUS_DRIVERNAME, 0, 0);
	if (retVal != 0) {
        sceKernelSleepThread();
	}
	retVal = sceUsbStart(PSP_USBSTOR_DRIVERNAME, 0, 0);
	if (retVal != 0) {
        sceKernelSleepThread();
	}
	retVal = sceUsbstorBootSetCapacity(0x800000);
	if (retVal != 0) {
        sceKernelSleepThread();
	}
#endif

	pspDebugScreenInit();
#if !DAEMON
	// Create a simple default command file if no command file present
	char commandFileName[100];
	char command[1000];
	getFileName(commandFileName, "command.txt");
	SceUID fd = sceIoOpen(commandFileName, PSP_O_RDONLY, 0);
	if (fd < 0)
	{
		fd = sceIoOpen(commandFileName, PSP_O_CREAT | PSP_O_TRUNC | PSP_O_WRONLY, 0777);
		sprintf(command, "DecodeVideo\nms0:/tmp/Movie.pmf\n");
		sceIoWrite(fd, command, strlen(command));
	}
	sceIoClose(fd);
#endif

	allocateFileBuffer(VIDEO_STREAM, MB(8));
	allocateFileBuffer(AUDIO_STREAM, MB(8));

	usbActivated = 0;
	muted = false;
	atracInstructionsDisplayed = false;
	oldButtons = 0;
	buttons = 0;
}

void JpcspConnector::allocateFileBuffer(int stream, int maxSize)
{
	fileBufferSize[stream] = 0;

	for (int size = maxSize; size > 0; size -= MB(1))
	{
		currentFileBuffer[stream] = (char *) malloc(size + MEMORY_ALIGNMENT);
		if (currentFileBuffer[stream] == NULL)
		{
#if DEBUG
			char msg[100];
			sprintf(msg, "Cannot allocate buffer of size %d for stream %d, trying again with a smaller size", size, stream);
			debug(msg);
#endif
		}
		else
		{
#if DEBUG
			char msg[100];
			sprintf(msg, "Successfully allocated buffer of size %d for stream %d", size, stream);
			debug(msg);
#endif
			// Make sure memory is MEMORY_ALIGNMENT-byte aligned
			currentFileBuffer[stream] = (char *) ((((int) currentFileBuffer[stream]) + MEMORY_ALIGNMENT - 1) & (~(MEMORY_ALIGNMENT - 1)));
			fileBufferSize[stream] = size;
			break;
		}
	}
}


void JpcspConnector::run(SceSize _argc, void* _argp)
{
	int result = 1;
	initialize();

#if DEBUG
	int startTime = time(NULL);
#endif
	while (result != 2 && !isCancel())
	{
		scePowerTick(0);

		result = executeCommandFile();
		if (result == 0)
		{
			activateUsb();
			sceKernelDelayThread(1000 * 1000);
			deactivateUsb();
		}

		ctrlPeekBuffer();
	}
#if DEBUG
	int endTime = time(NULL);
	char msg[1000];
	sprintf(msg, "Duration: %d s", endTime - startTime);
	debug(msg);
#endif

	deactivateUsb();
	debugFlush();
}


char *JpcspConnector::getDirectoryName(char *name)
{
	sprintf(name, "fatms0:/tmp");

	return name;
}


char *JpcspConnector::getRawFileName(char *name, char *prefix, int frameCount)
{
	char directory[100];

	getDirectoryName(directory);
	sprintf(name, "%s/%s-%d.raw", directory, prefix, frameCount);

	return name;
}

char *JpcspConnector::getFileName(char *result, char *name)
{
	char directory[100];

	getDirectoryName(directory);
	sprintf(result, "%s/%s", directory, name);

	return name;
}

void JpcspConnector::createDirectories()
{
	char directory[100];

	getDirectoryName(directory);
	sceIoMkdir(directory, 0777);
}


int JpcspConnector::readLine(SceUID fd, char *data, SceSize size)
{
	if (size <= 0 || data == NULL || fd < 0)
	{
		return 0;
	}

	size--;	// Leave space for ending '\0'

	for (SceSize i = 0; i < size; i++)
	{
		int length = sceIoRead(fd, data + i, 1);
		if (length <= 0)
		{
			data[i] = '\0';
			return i;
		}

		if (data[i] == '\n')
		{
			data[i] = '\0';
			return i;
		}
		if (data[i] == '\r')
		{
			i--;
		}
	}

	data[size] = '\0';

	return size;
}


void JpcspConnector::refreshMemoryStick()
{
	// Invalidate the MemoryStick driver cache.
	// All the open file descriptors are being closed!
	sceIoDevctl("fatms0:", 0x0240D81E, NULL, 0, NULL, 0 ); 
}


int JpcspConnector::executeCommandFile()
{
	char commandFileName[100];
	char command[1000];
	char parameters[1000];
	int result = 0;

	refreshMemoryStick();

	// Open command file
	getFileName(commandFileName, "command.txt");
	SceUID fd = sceIoOpen(commandFileName, PSP_O_RDONLY, 0777);
	if (fd < 0)
	{
		pspDebugScreenPrintf("Waiting for %s\n", commandFileName);
		return 0;
	}

	while (result != 2)
	{
		// Read command line
		int length = readLine(fd, command, sizeof(command));
		if (length <= 0)
		{
			break;
		}

		// Read parameters line
		length = readLine(fd, parameters, sizeof(parameters));
		if (length <= 0)
		{
			// No parameters
			parameters[0] = '\0';
		}

#if DEBUG
		{
			char msg[100];
			sprintf(msg, "Command: %s", command);
			debug(msg);
			sprintf(msg, "Parameters: %s", parameters);
			debug(msg);
		}
#endif

		// Execute command
		if (strcmp(command, "DecodeVideo") == 0)
		{
			result = commandDecodeVideo(parameters);
		}
		else if (strcmp(command, "DecryptPGD") == 0)
		{
			result = commandDecryptPGD(parameters);
		}
		else if (strcmp(command, "DecodeAtrac3") == 0)
		{
			result = commandDecodeAtrac3(parameters);
		}
		else if (strcmp(command, "Exit") == 0)
		{
			result = commandExit(parameters);
		}
		else
		{
			// Unknown command
			result = 0;
		}
	}

	// Close command file
	sceIoClose(fd);

	// Delete the command file to signal that its processing is completed
	sceIoRemove(commandFileName);
#if !DAEMON
	result = 2;
#endif

	return result;
}


int JpcspConnector::commandExit(char *parameters)
{
	return 2;
}


int JpcspConnector::commandDecryptPGD(char *parameters)
{
	char *pSpace = strchr(parameters, ' ');
	char *fileName = parameters;
	*pSpace = '\0';
	char *keyHex = pSpace + 1;
	char key[16];
	int keyLength = decodeHex(keyHex, key, 16);
	char *pLastFileNamePart = strrchr(fileName, '/');

#if DEBUG
	char msg[1000];
	sprintf(msg, "FileName: %s", fileName);
	debug(msg);
	sprintf(msg, "Key in Hex: %s", keyHex);
	debug(msg);
#endif
	SceUID fdin = sceIoOpen(fileName, 0x40000000 | PSP_O_RDONLY, 0);
	int result = sceIoIoctl(fdin, 0x04100001, key, keyLength, NULL, 0);
#if DEBUG
	if (result != 0)
	{
		sprintf(msg, "sceIoIoctl 0x04100001 result: %08X", result);
		debug(msg);
	}
#endif

	char outFileName[100];
	sprintf(outFileName, "%s.decrypted", pLastFileNamePart + 1);
	char outFilePath[100];
	getFileName(outFilePath, outFileName);
#if DEBUG
	sprintf(msg, "Output FileName: %s", outFilePath);
	debug(msg);
#endif
	SceUID fdout = sceIoOpen(outFilePath, PSP_O_CREAT | PSP_O_TRUNC | PSP_O_WRONLY, 0777);

	char *buffer = currentFileBuffer[0];
	int bufferSize = fileBufferSize[0];

	pspDebugScreenPrintf("Decrypting %s...\n", fileName);
	while (fdin > 0 && fdout > 0)
	{
		int length = sceIoRead(fdin, buffer, bufferSize);
		if (length <= 0)
		{
#if DEBUG
			if (length < 0)
			{
				sprintf(msg, "sceIoRead result: %08X", length);
				debug(msg);
			}
#endif
			break;
		}
		pspDebugScreenPrintf(".");
		sceIoWrite(fdout, buffer, length);
	}
	pspDebugScreenPrintf("Decryption completed.");

	sceIoClose(fdin);
	sceIoClose(fdout);

	return 1;
}


int JpcspConnector::decodeHex(char *hex, char *buffer, int bufferLength)
{
	int hexLength = strlen(hex);
	int previous = 0;
	int bufferIndex = 0;
	for (int i = 0; i < hexLength && bufferIndex < bufferLength; i++)
	{
		char c = hex[i];
		int n = 0;
		if (c >= '0' && c <= '9')
		{
			n = c - '0';
		}
		else if (c >= 'a' && c <= 'f')
		{
			n = c - 'a' + 10;
		}
		else if (c >= 'A' && c <= 'F')
		{
			n = c - 'A' + 10;
		}

		if (i % 2 == 0)
		{
			previous = n;
		}
		else
		{
			buffer[bufferIndex] = (previous << 4) | n;
			bufferIndex++;
		}
	}

	return bufferIndex;
}


int JpcspConnector::commandDecodeVideo(char *parameters)
{
	Player = new CPMFPlayer();

	if(Player->Initialize() < 0)
	{
#if DEBUG
		char msg[1000];
		sprintf(msg, "Initialize error %s\n", Player->GetLastError());
		debug(msg);
#endif
		pspDebugScreenInit();
		pspDebugScreenPrintf(Player->GetLastError());
		sceKernelDelayThread(5000000);
		return 0;
	}

	if(Player->Load(parameters) < 0)
	{
#if DEBUG
		char msg[1000];
		sprintf(msg, "Load error %s\n", Player->GetLastError());
		debug(msg);
#endif
		pspDebugScreenInit();
		pspDebugScreenPrintf(Player->GetLastError());
		sceKernelDelayThread(5000000);
		return 0;
	}

	if(Player->Play(this) < 0)
	{
#if DEBUG
		char msg[1000];
		sprintf(msg, "Play error %s\n", Player->GetLastError());
		debug(msg);
#endif
		pspDebugScreenInit();
		pspDebugScreenPrintf(Player->GetLastError());
		sceKernelDelayThread(5000000);
		return 0;
	}

	Player->Shutdown();

	delete Player;
	Player = NULL;

	return 1;
}


int JpcspConnector::compressVideo(void *destinationBuffer, void *videoBuffer, void *previousVideoBuffer)
{
	int *dst = (int *) destinationBuffer;
	int *src = (int *) videoBuffer;
	int *previous = (int *) previousVideoBuffer;

	//
	// Writing to the MemoryStick is very slow...
	// so speed up the processing by first compressing the
	// image so that we have less information to write
	// on the MemoryStick.
	//
	// Encode the video buffer using Run-Length Encoding
	// on a pixel base.
	// Store the length in the alpha component of the pixel
	// (bits 24..31).
	//
	for (int y = 0; y < SCREEN_H; y++)
	{
		int x;
		for (x = 0; x < SCREEN_W; )
		{
			int c = PIXEL_VALUE(*src++);
			int p = (previousVideoBuffer == NULL ? c + 1 : PIXEL_VALUE(*previous++));
			x++;

			if (c == PIXEL_VALUE(*src) && x < SCREEN_W)
			{
				if (c == p)
				{
					// Both methods apply: RLE and matching previous video.
					// Choose the one matching the most pixels
					int rleFailed = 0;
					int previousFailed = 0;
					int i;
					for (i = 0; x < SCREEN_W && i < 0x7F; i++)
					{
						int rleMatch      = !rleFailed      && PIXEL_VALUE(*src) == c;
						int previousMatch = !previousFailed && PIXEL_VALUE(*src) == PIXEL_VALUE(*previous);
						if (rleMatch)
						{
							if (previousMatch)
							{
								// OK, both still matching
							}
							else
							{
								// Continue RLE, previous video no longer matching
								previousFailed = 1;
							}
						}
						else
						{
							if (previousMatch)
							{
								// Continue testing previous video, RLE no longer matching
								rleFailed = 1;
							}
							else
							{
								break;
							}
						}
						src++;
						previous++;
						x++;
					}

					// If none failed, prefer RLE encoding (because faster decoding)
					if (!rleFailed)
					{
						// Encode RLE
						*dst++ = ENCODE_RLE(c, i);
					}
					else
					{
						// Encode to match previous video
						if (x < SCREEN_W)
						{
							c = PIXEL_VALUE(*src++);
							previous++;
							x++;
						}
						else if (i > 0)
						{
							// Past screen width, take previous pixel
							c = PIXEL_VALUE(*(src-1));
							i--;
						}
						*dst++ = ENCODE_PREVIOUS(c, i);
					}
				}
				else
				{
					// Only RLE, not matching previous video
					src++;
					previous++;
					x++;
					int i;
					for (i = 1; x < SCREEN_W; i++)
					{
						if (c != PIXEL_VALUE(*src) || i >= 0x7F)
						{
							break;
						}
						else
						{
							src++;
							previous++;
							x++;
						}
					}
					*dst++ = ENCODE_RLE(c, i);
				}
			}
			else if (c == p)
			{
				// No RLE, only matching previous video
				int i;
				for (i = 0; x < SCREEN_W; i++)
				{
					c = PIXEL_VALUE(*src++);
					p = PIXEL_VALUE(*previous++);
					x++;
					if (c != p || i >= 0x7F)
					{
						break;
					}
				}

				*dst++ = ENCODE_PREVIOUS(c, i);
			}
			else
			{
				// No RLE, not matching previous video
				*dst++ = ENCODE_NONE(c);
			}
		}

		src += BUFFER_WIDTH - x;
		previous += BUFFER_WIDTH - x;
	}

	return ((char *) dst) - ((char *)destinationBuffer);
}


void JpcspConnector::writeCurrentFile(int stream, void *buffer, int length)
{
	if (length > 0 && buffer != NULL)
	{
		memcpy(currentFileBuffer[stream] + currentFileSize[stream], buffer, length);
		currentFileSize[stream] += length;
	}
}


int JpcspConnector::getMemoryStickFreeSizeKb()
{
	struct
	{
		int maxClusters;
		int freeClusters;
		int maxSectors;
		int sectorSize;
		int sectorCount;
	} msFreeSpace;
	void *input = &msFreeSpace;

	sceIoDevctl("fatms0:", 0x02425818, &input, sizeof(input), NULL, 0);

	return msFreeSpace.freeClusters * (msFreeSpace.sectorSize * msFreeSpace.sectorCount / KB(1));
}


void JpcspConnector::waitForRemoteCompletion()
{
	SceCtrlData pad;
	int oldButtons = 0;
	char completedName[100];
	getFileName(completedName, "remoteCompleted.txt");

	int usbWasActivated = usbActivated;
	int startFreeSize = getMemoryStickFreeSizeKb();
	pspDebugScreenPrintf("Move the files from ms0:/tmp/*.raw to Jpcsp/tmp/<DISC_ID>\n");
	pspDebugScreenPrintf("After moving the files, press X\n");

	// I'm trying different methods to automatically detect the file moving
	// but none is really working:
	// - wait for the creation of a file "ms0:/tmp/remoteCompleted.txt": this file is never seen
	// - wait for a decrease of the memory stick free size: this is never changing
	// The last alternative is to ask the user to press X
	while (1)
	{
		scePowerTick(0);

		SceIoStat stat;
		int result = sceIoGetstat(completedName, &stat);
		if (result >= 0)
		{
			break;
		}
		int freeSize = getMemoryStickFreeSizeKb();
		if (freeSize > startFreeSize)
		{
			break;
		}

		sceCtrlReadBufferPositive(&pad, 1);
		int buttonDown = (oldButtons ^ pad.Buttons) & pad.Buttons;
		if (buttonDown & PSP_CTRL_CROSS)
		{
			break;
		}

		activateUsb();
		refreshMemoryStick();
		sceKernelDelayThread(100 * 1000);
	}

	if (!usbWasActivated)
	{
		deactivateUsb();
	}

	sceIoRemove(completedName);
}


void JpcspConnector::closeCurrentFile(int stream)
{
	if (currentFd[stream] < 0)
	{
		return;
	}

	sceIoClose(currentFd[stream]);
	currentFd[stream] = -1;
}


int JpcspConnector::openCurrentFile(int stream, int videoFrameCount)
{
	closeCurrentFile(stream);

	if (getMemoryStickFreeSizeKb() < (int) (2 * fileBufferSize[stream] / KB(1)))
	{
#if DAEMON
		waitForRemoteCompletion();
#else
		pspDebugScreenInit();
		pspDebugScreenPrintf("Not enough space left on the MemoryStick\n");
		return 0;
#endif
	}

	char name[100];
	getRawFileName(name, streamName[stream], videoFrameCount);
	currentFd[stream] = sceIoOpen(name, PSP_O_CREAT | PSP_O_TRUNC | PSP_O_WRONLY, 0777);

	return (currentFd[stream] >= 0);
}


void JpcspConnector::flushCurrentFile(int stream)
{
	if (currentFd[stream] < 0)
	{
		if (!openCurrentFile(stream, startFrameCount[stream]))
		{
			return;
		}
	}

	sceIoWrite(currentFd[stream], currentFileBuffer[stream], currentFileSize[stream]);
	currentFileSize[stream] = 0;
}


void JpcspConnector::writeVersion(int stream)
{
	SceUInt32 version = FILE_FORMAT_VERSION;
	writeCurrentFile(stream, &version, sizeof(version));
}


int JpcspConnector::writeFrame(int stream, int frameCount, void *buffer, int length, SceInt32 timeStamp, void *additionalBuffer, int additionalLength)
{
	SceUInt32 fileSize = sizeof(fileSize) + sizeof(timeStamp) + additionalLength + length;

	if ((currentFileSize[stream] + fileSize) > fileBufferSize[stream])
	{
		// write current file
		closeCurrentFile(stream);
		flushCurrentFile(stream);
	}

	if (currentFileSize[stream] == 0)
	{
		startFrameCount[stream] = frameCount;
		writeVersion(stream);
	}

	writeCurrentFile(stream, &fileSize, sizeof(fileSize));
	writeCurrentFile(stream, &timeStamp, sizeof(timeStamp));
	writeCurrentFile(stream, additionalBuffer, additionalLength);
	writeCurrentFile(stream, buffer, length);

	return 1;
}


void JpcspConnector::sendVideoFrame(int videoFrameCount, void *videoBuffer, SceInt32 videoTimeStamp, ReaderThreadData* D, SceUInt32 packetsConsumed)
{
	struct
	{
		SceUInt32 packetsConsumed;
		SceInt32  totalBytes;
	} additionalInfo;

	additionalInfo.packetsConsumed = packetsConsumed;
	additionalInfo.totalBytes	   = D->m_TotalBytes;
	int length = compressVideo(sendBuffer, videoBuffer, previousVideoBuffer);
	writeFrame(VIDEO_STREAM, videoFrameCount, sendBuffer, length, videoTimeStamp, &additionalInfo, sizeof(additionalInfo));
	memcpy(previousVideoBuffer, videoBuffer, DRAW_BUFFER_SIZE);
}


void JpcspConnector::sendAudioFrame(int audioFrameCount, void *audioBuffer, int audioBufferLength, SceInt32 audioTimeStamp)
{
	memcpy(sendBuffer, audioBuffer, audioBufferLength);
	writeFrame(AUDIO_STREAM, audioFrameCount, sendBuffer, audioBufferLength, audioTimeStamp, NULL, 0);
}


int JpcspConnector::loadStartModule(char *path)
{
        u32 loadResult;
        u32 startResult;
        int status;

        loadResult = sceKernelLoadModule(path, 0, NULL);
        if (loadResult < 0)
		{
			return loadResult;
		}
        else
		{
			startResult = sceKernelStartModule(loadResult, 0, NULL, &status, NULL);
		}

        if (loadResult != startResult)
		{
			return startResult;
		}

        return 0;
}


void JpcspConnector::activateUsb()
{
	if (!usbActivated)
	{
#if USE_USB
		sceUsbActivate(0x1c8);
#endif
		usbActivated = 1;
	}
}


void JpcspConnector::deactivateUsb()
{
	if (usbActivated)
	{
#if USE_USB
		sceUsbDeactivate(0x1c8);
		sceIoDevctl("fatms0:", 0x0240D81E, NULL, 0, NULL, 0); //Avoid corrupted files 
#endif
		usbActivated = 0;
	}
}


void JpcspConnector::initConnector()
{
}


void JpcspConnector::exitConnector()
{
	for (int stream = 0; stream < NUMBER_STREAMS; stream++)
	{
		closeCurrentFile(stream);
		flushCurrentFile(stream);
		closeCurrentFile(stream);
	}

#if DAEMON
	waitForRemoteCompletion();
#endif

	deactivateUsb();
}


int JpcspConnector::commandDecodeAtrac3(char *parameters)
{
	char *fileName = parameters;
	SceIoStat stat;
	char *atracBuffer = currentFileBuffer[0];
	int atracBufferSize = (int) fileBufferSize[0];
	char *decodeBuffer = currentFileBuffer[1];
	int decodeBufferSize = (int) fileBufferSize[1];
#if DEBUG_TIMING
	int start;
	char s[100];
#endif
#if DEBUG_ATRAC
	char s[100];
#endif

	int result = sceIoGetstat(fileName, &stat);
	if (result < 0)
	{
		return 0;
	}
	int size = (int) stat.st_size;
	if (size > atracBufferSize)
	{
		size = atracBufferSize;
	}

	SceUID fd = sceIoOpen(fileName, PSP_O_RDONLY, 0);
	int length = sceIoRead(fd, atracBuffer, size);
	sceIoClose(fd);

	int atracID = sceAtracSetDataAndGetID(atracBuffer, length);

	int maxSamples = 0;
	result = sceAtracGetMaxSample(atracID, &maxSamples);
	int channel = sceAudioChReserve(0, maxSamples, PSP_AUDIO_FORMAT_STEREO);

	char *pLastFileNamePart = strrchr(fileName, '/');
	char outFileName[100];
	sprintf(outFileName, "%s.decoded", pLastFileNamePart + 1);
	char outFilePath[100];
	getFileName(outFilePath, outFileName);
	SceUID fdout = sceIoOpen(outFilePath, PSP_O_CREAT | PSP_O_TRUNC | PSP_O_WRONLY, 0777);

	if (!atracInstructionsDisplayed)
	{
		pspDebugScreenPrintf("Press CROSS to mute (much faster processing).\n");
		pspDebugScreenPrintf("Press CIRCLE to stop.\n");
		atracInstructionsDisplayed = true;
	}

	pspDebugScreenPrintf("Decoding Atrac Audio %s...\n", fileName);

	int end = 0;
	int remainFrame = -1;
	int decodeBufferPosition = 0;
	while (!end && !isCancel())
	{
		if (isCrossPressed())
		{
			muted = !muted;
		}

		int freeSize = decodeBufferSize - decodeBufferPosition;
		if (freeSize < maxSamples * 4)
		{
			sceIoWrite(fdout, decodeBuffer, decodeBufferPosition);
			decodeBufferPosition = 0;
		}

		int samples = 0;
#if DEBUG_TIMING
		start = sceKernelGetSystemTimeLow();
#endif
		result = sceAtracDecodeData(atracID, (u16 *) (decodeBuffer + decodeBufferPosition), &samples, &end, &remainFrame);
#if DEBUG_TIMING
		sprintf(s, "Duration sceAtracDecodeData %d, return %X", sceKernelGetSystemTimeLow() - start, result);
		debug(s);
#endif
#if DEBUG_ATRAC
		sprintf(s, "sceAtracDecodeData returning 0x%08X, samples=%d, end=%d, remainFrame=%d", result, samples, end, remainFrame);
		debug(s);
#endif
		if (result == 0 && samples > 0)
		{
			if (!muted)
			{
				sceAudioSetChannelDataLen(channel, samples);
				sceAudioOutputBlocking(channel, 0x8000, decodeBuffer + decodeBufferPosition);
			}

			decodeBufferPosition += 4 * samples;
		}
		else
		{
#if DEBUG
			char msg[100];
			sprintf(msg, "sceAtracDecodeData result %08X, samples %d, remainFrame %08X", result, samples, remainFrame);
			debug(msg);
#endif
			if (result < 0)
			{
				break;
			}
		}
#if DEBUG_ATRAC
		result = sceAtracGetRemainFrame(atracID, &remainFrame);
		sprintf(s, "sceAtracGetRemainFrame returning 0x%08X, remainFrame=%d", result, remainFrame);
		debug(s);
		u8 *inputBuffer = 0;
		u32 writableBytes = 0;
		u32 readOffset = 0;
		result = sceAtracGetStreamDataInfo(atracID, &inputBuffer, &writableBytes, &readOffset);
		sprintf(s, "sceAtracGetStreamDataInfo returning 0x%08X, inputBuffer=0x%08X, writableBytes=0x%X, readOffset=0x%X", result, (int) inputBuffer, writableBytes, readOffset);
		debug(s);
		int position;
		result = sceAtracGetNextDecodePosition(atracID, &position);
		sprintf(s, "sceAtracGetNextDecodePosition returning 0x%08X, position=%d", result, position);
		debug(s);
#endif

		ctrlPeekBuffer();
	}

	// Flush decodeBuffer
	if (decodeBufferPosition > 0)
	{
		sceIoWrite(fdout, decodeBuffer, decodeBufferPosition);
	}

	sceIoClose(fdout);
	sceAudioChRelease(channel);
	sceAtracReleaseAtracID(atracID);

	return 1;
}


void JpcspConnector::ctrlPeekBuffer()
{
	SceCtrlData pad;
	int n = sceCtrlPeekBufferPositive(&pad, 1);

	if (n > 0 && pad.Buttons != oldButtons)
	{
		buttons = pad.Buttons;
		oldButtons = pad.Buttons;
	}
}


bool JpcspConnector::isButtonPressed(SceUInt32 button)
{
	bool pressed = ((buttons & button) == button);

	if (pressed)
	{
		buttons &= ~button;
	}

	return pressed;
}


bool JpcspConnector::isCancel()
{
	return isButtonPressed(PSP_CTRL_CIRCLE);
}


bool JpcspConnector::isCrossPressed()
{
	return isButtonPressed(PSP_CTRL_CROSS);
}
