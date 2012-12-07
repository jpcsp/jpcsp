// Shine's SavedataTool
//
// Usage:
//   Create a file savename.txt in the root directory of your memory stick with 3 lines:
//   - first part of game directory name (looks like this is always 9 chars)
//   - seconds part of game directory name (the rest)
//   - name of the data file
//   An example for Wipeout Pure is included
//   After starting you can load the savegame data, which is stored
//   to the root directory as "params.bin" and "data.bin".
//   After editing these files, you can update your savegame again with it.
//
// Credits:
//   based on Hello World for PSP by nem
//   Exit Game test by TyRaNiD
//   function stubs: PSPDev - Browser Api (by djhuevo & neofar, http://pspdev.ofcode.com/api.php)
//   controller function usage based on a program by skippy911
//   and last but not least, thanks to ps2dev.org!

#include <pspkernel.h>
#include <pspkernel.h>
#include <pspkernel.h>
#include <pspdebug.h>
#include <pspctrl.h>
#include <pspdisplay.h>
#include <psputility.h>
#include <string.h>
#include <stdio.h>
#include "pg.h"

unsigned char g_dataBuf[0x100000];
unsigned char g_readIcon0[100000];
unsigned char g_readIcon1[100000];
unsigned char g_readPic1[100000];
unsigned char msFreeBuffer[20];
unsigned char msDataBuffer[64];
unsigned char utilityDataBuffer[28];
unsigned char idListBuffer[12];
unsigned char idListDataBuffer[72 * 0x1f];
unsigned char fileListBuffer[40];
unsigned char sizeBuffer[184];
unsigned char saveFileEntriesAddr[100000];
unsigned char saveFileSecureEntriesAddr[100000];
unsigned char saveFileSystemEntriesAddr[100000];

char* g_gameName;
char* g_saveName;
char* g_dataName;

#define O_RDONLY	0x0001
#define O_WRONLY	0x0002
#define O_RDWR		0x0003
#define O_NBLOCK	0x0010
#define O_APPEND	0x0100
#define O_CREAT		0x0200
#define O_TRUNC		0x0400
#define O_NOWAIT	0x8000

#define SEEK_SET    0
#define SEEK_CUR    1
#define SEEK_END    2

#define FW15	0

int y = 3;

void print(char* text)
{
	pgPrint(0, y++, 0xffff, text);
}


void printHex(int value) {
	const unsigned char* digits = (unsigned char *) "0123456789abcdef";
	int i;
	int x = 0;
	for (i = 7; i >= 0; i--) {
		pgPutChar((x+i)*8, y*8, 0xffff, 0, digits[value&0xf], 1, 0, 1);
		value >>= 4;
	}
	y++;
}

int exit_callback(int arg1, int arg2, void *common)
{
	sceKernelExitGame();

	return 0;
}

int CallbackThread(SceSize args, void *argp)
{
	int cbid;

	cbid = sceKernelCreateCallback("Exit Callback", exit_callback, (void*)0);
	sceKernelRegisterExitCallback(cbid);
	sceKernelSleepThreadCB();

	return 0;
}

int SetupCallbacks(void)
{
	int thid = 0;

	thid = sceKernelCreateThread("update_thread", CallbackThread, 0x11, 0xFA0, 0, 0);
	if(thid >= 0)
	{
		sceKernelStartThread(thid, 0, 0);
	}

	return thid;
}

typedef struct
{
	int size;
	int language;
	int buttonSwap;
	int graphicsThread;
	int accessThread;
	int fontThread;
	int soundThread;
	int result;
	int unknown8;
	int unknown9;
	int unknown10;
	int unknown11;
	int mode;
	int unknown12;
	int overwrite;
	char gameNameAsciiZ[16];
	char saveNameAsciiZ[24];
	char dataNameAsciiZ[16];
	unsigned char* dataBuf;
	int sizeOfDataBuf;
	int sizeOfData;
	char paramsSfoTitle[0x80];
	char paramsSfoSavedataTitle[0x80];
	char paramsSfoDetail[0x400];
	unsigned char paramsSfoParentalLevel;
	unsigned char unknown14[3];
	unsigned char* readIcon0Buf;
	int sizeOfReadIcon0Buf;
	int sizeOfReadIcon0;
	int unknown15;
	unsigned char* readIcon1Buf;
	int sizeOfReadIcon1Buf;
	int sizeOfReadIcon1;
	int unknown16;
	unsigned char* readPic1Buf;
	int sizeOfReadPic1Buf;
	int sizeOfReadPic1;
	int unknown17;
	unsigned char* readSnd0Buf;
	int sizeOfReadSnd0Buf;
	int sizeOfReadSnd0;
	int unknown18;
	unsigned char* newData;
	int focus;
	int abortStatus;
#if FW15
	char unknown20[12];
#else
	unsigned char *msFreeAddr;
	unsigned char *msDataAddr;
	unsigned char *utilityDataAddr;
	char key[16];
	int secureVersion;
	int multiStatus;
	unsigned char *idListAddr;
	unsigned char *fileListAddr;
	unsigned char *sizeAddr;
#endif
} SceUtilitySavedataParamNew;

#define PARAMS_LEN (0x80 + 0x80 + 0x400 + 1)

/* Button bit masks */ 
#define CTRL_SQUARE     0x8000 
#define CTRL_TRIANGLE   0x1000 
#define CTRL_CIRCLE     0x2000 
#define CTRL_CROSS      0x4000 
#define CTRL_UP         0x0010 
#define CTRL_DOWN       0x0040 
#define CTRL_LEFT       0x0080 
#define CTRL_RIGHT      0x0020 
#define CTRL_START      0x0008 
#define CTRL_SELECT     0x0001 
#define CTRL_LTRIGGER   0x0100 
#define CTRL_RTRIGGER   0x0200 

void initSavedata(SceUtilitySavedataParamNew* savedata, int mode) {
	memset(savedata, 0, sizeof(SceUtilitySavedataParamNew));
	savedata->size = sizeof(SceUtilitySavedataParamNew);
#if FW15
	savedata->graphicsThread = 0x21;
	savedata->accessThread = 0x23;
	savedata->fontThread = 0x22;
	savedata->soundThread = 0x20;
#else
	savedata->graphicsThread = 0x11;
	savedata->accessThread = 0x13;
	savedata->fontThread = 0x12;
	savedata->soundThread = 0x10;
	memset(msFreeBuffer, 0, sizeof(msFreeBuffer));
	memset(msDataBuffer, 0, sizeof(msDataBuffer));
	memset(utilityDataBuffer, 0, sizeof(utilityDataBuffer));
	memset(idListBuffer, 0, sizeof(idListBuffer));
	memset(idListDataBuffer, 0, sizeof(idListDataBuffer));
	memset(fileListBuffer, 0, sizeof(fileListBuffer));
	memset(sizeBuffer, 0, sizeof(sizeBuffer));
	memset(saveFileSecureEntriesAddr, 0, sizeof(saveFileSecureEntriesAddr));
	memset(saveFileSystemEntriesAddr, 0, sizeof(saveFileSystemEntriesAddr));
	memset(saveFileEntriesAddr, 0, sizeof(saveFileEntriesAddr));
	savedata->msFreeAddr = msFreeBuffer;
	savedata->msDataAddr = msDataBuffer;
	strcpy((char *) msDataBuffer, g_gameName);
	strcpy((char *) (msDataBuffer + 16), g_saveName);
	savedata->utilityDataAddr = utilityDataBuffer;
	savedata->idListAddr = idListBuffer;
	*((int *) (idListBuffer + 0)) = sizeof(idListDataBuffer) / 72;
	*((int *) (idListBuffer + 8)) = (int) &idListDataBuffer;
	savedata->fileListAddr = fileListBuffer;
	*((int *) (fileListBuffer + 24)) = (int) &saveFileSecureEntriesAddr;
	*((int *) (fileListBuffer + 28)) = (int) &saveFileEntriesAddr;
	*((int *) (fileListBuffer + 32)) = (int) &saveFileSystemEntriesAddr;
	savedata->sizeAddr = sizeBuffer;
	memset(sizeBuffer, 0x12, sizeof(sizeBuffer));
	*((int *) (sizeBuffer + 0)) = 1;
	*((int *) (sizeBuffer + 4)) = 1;
	*((int *) (sizeBuffer + 8)) = (int) &saveFileSecureEntriesAddr;
	*((int *) (sizeBuffer + 12)) = (int) &saveFileEntriesAddr;
	int *entry = (int *) saveFileEntriesAddr;
	entry[0] = 0x70000000;
	entry[1] = 0;
	strcpy((char *) (entry + 2), "FILE1");
	entry = (int *) saveFileSecureEntriesAddr;
	entry[0] = 0x70000000;
	entry[1] = 0;
	strcpy((char *) (entry + 2), "FILE2");
	strncpy(savedata->key, "1234567890123456", 16);
#endif
	savedata->overwrite = 1;
	savedata->mode = mode;
	strcpy(savedata->gameNameAsciiZ, g_gameName);
	strcpy(savedata->saveNameAsciiZ, g_saveName);
	strcpy(savedata->dataNameAsciiZ, g_dataName);
	savedata->dataBuf = g_dataBuf;
	savedata->sizeOfDataBuf = sizeof(g_dataBuf);
	savedata->readIcon0Buf = g_readIcon0;
	savedata->sizeOfReadIcon0Buf = sizeof(g_readIcon0);
	savedata->readIcon1Buf = g_readIcon1;
	savedata->sizeOfReadIcon1Buf = sizeof(g_readIcon1);
	savedata->readPic1Buf = g_readPic1;
	savedata->sizeOfReadPic1Buf = sizeof(g_readPic1);
}

void mainImpl()
{
	int result, previousResult;
	SceUtilitySavedataParamNew savedata;

	// read info file
	int fd = sceIoOpen("ms0:/savename.txt", O_RDONLY, 0);
	if (!fd) {
		print("ms0:/savename.txt not found");
		return;
	}
	char file[1000];
	int len = sceIoRead(fd, file, 1000);
	sceIoClose(fd);
	file[len] = 0;
	
	// extract first 3 lines
	char *ptr = file;

	g_gameName = ptr;
	while (*ptr != 10 && *ptr != 13 && *ptr != 0) ptr++;
	if (*ptr == 13) *ptr++ = 0;
	if (*ptr == 10) *ptr++ = 0;

	g_saveName = ptr;
	while (*ptr != 10 && *ptr != 13 && *ptr != 0) ptr++;
	if (*ptr == 13) *ptr++ = 0;
	if (*ptr == 10) *ptr++ = 0;

	g_dataName = ptr;
	while (*ptr != 10 && *ptr != 13 && *ptr != 0) ptr++;
	if (*ptr == 13) *ptr++ = 0;
	if (*ptr == 10) *ptr++ = 0;

	// ask for load or update
	pgPrint2(0, 0, 0xffff, "Shine's SavedataTool");
	char buf[100];
	strcpy(buf, "game name: ");
	print(strcat(buf, g_gameName));
	strcpy(buf, "save name: ");
	print(strcat(buf, g_saveName));
	strcpy(buf, "data name: ");
	print(strcat(buf, g_dataName));
	y++;
	print("Press Cross    for savedata mode 0  (AUTOLOAD)");
    print("Press Circle   for savedata mode 1  (AUTOSAVE)");
	print("Press Triangle for savedata mode 8  (SIZES)");
	print("Press Square   for savedata mode 11 (LIST)");
	print("Press Left     for savedata mode 12 (FILES)");
	print("Press Up       for savedata mode 15 (READSECURE)");
	print("Press Down     for savedata mode 22 (GETSIZE)");
	y++;

	sceCtrlSetSamplingCycle(0); 
	sceCtrlSetSamplingMode(0); 
	SceCtrlData ctrl;
	int mode = -1;
	while (mode < 0) { 
		sceCtrlReadBufferPositive(&ctrl, 1); 
		if (ctrl.Buttons & CTRL_CROSS) { 
			mode = 0; // AUTOLOAD
		} else if (ctrl.Buttons & CTRL_CIRCLE) { 
			mode = 1; // AUTOSAVE
		} else if (ctrl.Buttons & CTRL_TRIANGLE) {
			mode = 8; // SIZES
		} else if (ctrl.Buttons & CTRL_SQUARE) {
			mode = 11; // LIST
		} else if (ctrl.Buttons & CTRL_UP) {
			mode = 15; // READSECURE
		} else if (ctrl.Buttons & CTRL_DOWN) {
			mode = 22; // GETSIZE
		} else if (ctrl.Buttons & CTRL_LEFT) {
			mode = 12; // FILES
		}
		sceDisplayWaitVblankStart();
	}

	if (mode == 0) {
		print("Loading savedata with mode 0 (AUTOLOAD)...");
		initSavedata(&savedata, mode);
		result = sceUtilitySavedataInitStart((SceUtilitySavedataParam *) &savedata);
		if (result) {
			print("sceUtilitySavedataInitStart failed");
			printHex(result);
			return;
		}
		previousResult = -1;
		while (1) {
			result = sceUtilitySavedataGetStatus();
			if (result != previousResult) {
				print("sceUtilitySavedataGetStatus result:");
				printHex(result);
				previousResult = result;
			}
			if (result == 3) break;
			sceUtilitySavedataUpdate(1);
			sceDisplayWaitVblankStart();
		}

		// write data	
		print("writing extracted savedata...");
		fd = sceIoOpen("ms0:/params.bin", O_CREAT | O_TRUNC | O_WRONLY, 0777);
		if (!fd) {
			print("can't open ms0:/params.bin");
			return;
		}
		sceIoWrite(fd, &savedata.paramsSfoTitle, PARAMS_LEN);
		sceIoClose(fd);
		fd = sceIoOpen("ms0:/data.bin", O_CREAT | O_TRUNC | O_WRONLY, 0777);
		if (!fd) {
			print("can't open ms0:/data.bin");
			return;
		}
		sceIoWrite(fd, g_dataBuf, savedata.sizeOfData);	
		sceIoClose(fd);
		fd = sceIoOpen("ms0:/savedata.bin", O_CREAT | O_TRUNC | O_WRONLY, 0777);
		if (!fd) {
			print("can't open ms0:/savedata.bin");
			return;
		}
		sceIoWrite(fd, &savedata, sizeof(savedata));
		sceIoClose(fd);
	} else if (mode == 1) {
		print("Loading savedata with mode 1 (AUTOSAVE)...");
		initSavedata(&savedata, mode);
		savedata.readIcon0Buf = 0;
		savedata.sizeOfReadIcon0Buf = 0;
		savedata.readIcon1Buf = 0;
		savedata.sizeOfReadIcon1Buf = 0;
		savedata.readPic1Buf = 0;
		savedata.sizeOfReadPic1Buf = 0;

		// load extracted data
		print("loading extracted savedata...");
		fd = sceIoOpen("ms0:/params.bin", O_RDONLY, 0);
		if (!fd) {
			print("can't open ms0:/params.bin");
			return;
		}
		sceIoRead(fd, &savedata.paramsSfoTitle, PARAMS_LEN);
		sceIoClose(fd);
		fd = sceIoOpen("ms0:/data.bin", O_RDONLY, 0);
		if (!fd) {
			print("can't open ms0:/data.bin");
			return;
		}
		int len = sceIoRead(fd, g_dataBuf, 0x100000);
		sceIoClose(fd);
		
		// update gamedata
		print("updating savedata...");
		savedata.sizeOfData = len;
		result = sceUtilitySavedataInitStart((SceUtilitySavedataParam *) &savedata);
		if (result) {
			print("sceUtilitySavedataInitStart failed");
			printHex(result);
			return;
		}
		previousResult = -1;
		while (1) {
			result = sceUtilitySavedataGetStatus();
			if (result != previousResult) {
				print("sceUtilitySavedataGetStatus result:");
				printHex(result);
				previousResult = result;
			}
			if (result == 3) break;
			sceUtilitySavedataUpdate(1);
			sceDisplayWaitVblankStart();
		}
	} else if (mode == 8) {
		print("loading savedata with mode 8 (SIZES)...");
		initSavedata(&savedata, mode);
		/* savedata.sizeOfDataBuf = 0x1000; */
		savedata.sizeOfData = savedata.sizeOfDataBuf;
		result = sceUtilitySavedataInitStart((SceUtilitySavedataParam *) &savedata);
		if (result) {
			print("sceUtilitySavedataInitStart failed");
			printHex(result);
			return;
		}
		previousResult = -1;
		while (1) {
			result = sceUtilitySavedataGetStatus();
			if (result != previousResult) {
				print("sceUtilitySavedataGetStatus result:");
				printHex(result);
				previousResult = result;
			}
			if (result == 3) break;
			sceUtilitySavedataUpdate(1);
			sceDisplayWaitVblankStart();
		}

		// write data
		print("writing savedata structure...");
		fd = sceIoOpen("ms0:/savedata.bin", O_CREAT | O_TRUNC | O_WRONLY, 0777);
		if (!fd) {
			print("can't open ms0:/savedata.bin");
			return;
		}
		sceIoWrite(fd, &savedata, sizeof(savedata));
		sceIoClose(fd);
#if FW15
#else
		char buffer[100];
		sprintf(buffer, "Free space: %s", msFreeBuffer + 12);
		print(buffer);
		sprintf(buffer, "Required space1: %s", utilityDataBuffer + 8);
		print(buffer);
		sprintf(buffer, "Required space2: %s", utilityDataBuffer + 20);
		print(buffer);
		fd = sceIoOpen("ms0:/msFreeBuffer.bin", O_CREAT | O_TRUNC | O_WRONLY, 0777);
		if (!fd) {
			print("can't open ms0:/msFreeBuffer.bin");
			return;
		}
		sceIoWrite(fd, msFreeBuffer, sizeof(msFreeBuffer));
		sceIoClose(fd);

		fd = sceIoOpen("ms0:/msDataBuffer.bin", O_CREAT | O_TRUNC | O_WRONLY, 0777);
		if (!fd) {
			print("can't open ms0:/msDataBuffer.bin");
			return;
		}
		sceIoWrite(fd, msDataBuffer, sizeof(msDataBuffer));
		sceIoClose(fd);

		fd = sceIoOpen("ms0:/utilityDataBuffer.bin", O_CREAT | O_TRUNC | O_WRONLY, 0777);
		if (!fd) {
			print("can't open ms0:/utilityDataBuffer.bin");
			return;
		}
		sceIoWrite(fd, utilityDataBuffer, sizeof(utilityDataBuffer));
		sceIoClose(fd);
#endif
	} else if (mode == 11) {
		print("Loading savedata with mode 11 (LIST)...");
		initSavedata(&savedata, mode);
		result = sceUtilitySavedataInitStart((SceUtilitySavedataParam *) &savedata);
		if (result) {
			print("sceUtilitySavedataInitStart failed");
			printHex(result);
			return;
		}
		previousResult = -1;
		while (1) {
			result = sceUtilitySavedataGetStatus();
			if (result != previousResult) {
				print("sceUtilitySavedataGetStatus result:");
				printHex(result);
				previousResult = result;
			}
			if (result == 3) break;
			sceUtilitySavedataUpdate(1);
			sceDisplayWaitVblankStart();
		}

		int numEntries = *((int *) (idListBuffer + 4));
		int i;
		char buffer[100];
		for (i = 0; i < numEntries; i++) {
			sprintf(buffer, "Name: %s", (char *) (idListDataBuffer + i * 72 + 52));
			print(buffer);
		}

		// write data
		print("writing savedata structure...");
		fd = sceIoOpen("ms0:/savedata.bin", O_CREAT | O_TRUNC | O_WRONLY, 0777);
		if (!fd) {
			print("can't open ms0:/savedata.bin");
			return;
		}
		sceIoWrite(fd, &savedata, sizeof(savedata));
		sceIoClose(fd);
#if FW15
#else
		fd = sceIoOpen("ms0:/idListBuffer.bin", O_CREAT | O_TRUNC | O_WRONLY, 0777);
		if (!fd) {
			print("can't open ms0:/idListBuffer.bin");
			return;
		}
		sceIoWrite(fd, idListBuffer, sizeof(idListBuffer));
		sceIoClose(fd);

		fd = sceIoOpen("ms0:/idListDataBuffer.bin", O_CREAT | O_TRUNC | O_WRONLY, 0777);
		if (!fd) {
			print("can't open ms0:/idListDataBuffer.bin");
			return;
		}
		sceIoWrite(fd, idListDataBuffer, sizeof(idListDataBuffer));
		sceIoClose(fd);

		fd = sceIoOpen("ms0:/fileListBuffer.bin", O_CREAT | O_TRUNC | O_WRONLY, 0777);
		if (!fd) {
			print("can't open ms0:/fileListBuffer.bin");
			return;
		}
		sceIoWrite(fd, fileListBuffer, sizeof(fileListBuffer));
		sceIoClose(fd);

		fd = sceIoOpen("ms0:/sizeBuffer.bin", O_CREAT | O_TRUNC | O_WRONLY, 0777);
		if (!fd) {
			print("can't open ms0:/sizeBuffer.bin");
			return;
		}
		sceIoWrite(fd, sizeBuffer, sizeof(sizeBuffer));
		sceIoClose(fd);

		fd = sceIoOpen("ms0:/saveFileSecureEntriesAddr.bin", O_CREAT | O_TRUNC | O_WRONLY, 0777);
		if (!fd) {
			print("can't open ms0:/saveFileSecureEntriesAddr.bin");
			return;
		}
		sceIoWrite(fd, saveFileSecureEntriesAddr, sizeof(saveFileSecureEntriesAddr));
		sceIoClose(fd);

		fd = sceIoOpen("ms0:/saveFileSystemEntriesAddr.bin", O_CREAT | O_TRUNC | O_WRONLY, 0777);
		if (!fd) {
			print("can't open ms0:/saveFileSystemEntriesAddr.bin");
			return;
		}
		sceIoWrite(fd, saveFileSystemEntriesAddr, sizeof(saveFileSystemEntriesAddr));
		sceIoClose(fd);
#endif
	} else if (mode == 15) {
		print("Loading savedata with mode 15 (READSECURE)...");
		initSavedata(&savedata, mode);
		result = sceUtilitySavedataInitStart((SceUtilitySavedataParam *) &savedata);
		if (result) {
			print("sceUtilitySavedataInitStart failed");
			printHex(result);
			return;
		}
		previousResult = -1;
		while (1) {
			result = sceUtilitySavedataGetStatus();
			if (result != previousResult) {
				print("sceUtilitySavedataGetStatus result:");
				printHex(result);
				previousResult = result;
			}
			if (result == 3) break;
			sceUtilitySavedataUpdate(1);
			sceDisplayWaitVblankStart();
		}

		// write data	
		print("writing extracted savedata...");
		fd = sceIoOpen("ms0:/params.bin", O_CREAT | O_TRUNC | O_WRONLY, 0777);
		if (!fd) {
			print("can't open ms0:/params.bin");
			return;
		}
		sceIoWrite(fd, &savedata.paramsSfoTitle, PARAMS_LEN);
		sceIoClose(fd);

		fd = sceIoOpen("ms0:/data.bin", O_CREAT | O_TRUNC | O_WRONLY, 0777);
		if (!fd) {
			print("can't open ms0:/data.bin");
			return;
		}
		sceIoWrite(fd, g_dataBuf, savedata.sizeOfData);	
		sceIoClose(fd);

		fd = sceIoOpen("ms0:/savedata.bin", O_CREAT | O_TRUNC | O_WRONLY, 0777);
		if (!fd) {
			print("can't open ms0:/savedata.bin");
			return;
		}
		sceIoWrite(fd, &savedata, sizeof(savedata));
		sceIoClose(fd);
	} else if (mode == 22) {
		print("Loading savedata with mode 22...");
		initSavedata(&savedata, mode);

		result = sceUtilitySavedataInitStart((SceUtilitySavedataParam *) &savedata);
		if (result) {
			print("sceUtilitySavedataInitStart failed");
			printHex(result);
			return;
		}
		previousResult = -1;
		while (1) {
			result = sceUtilitySavedataGetStatus();
			if (result != previousResult) {
				print("sceUtilitySavedataGetStatus result:");
				printHex(result);
				previousResult = result;
			}
			if (result == 3) break;
			sceUtilitySavedataUpdate(1);
			sceDisplayWaitVblankStart();
		}

		// write data	
		print("writing extracted savedata...");
		fd = sceIoOpen("ms0:/params.bin", O_CREAT | O_TRUNC | O_WRONLY, 0777);
		if (!fd) {
			print("can't open ms0:/params.bin");
			return;
		}
		sceIoWrite(fd, &savedata.paramsSfoTitle, PARAMS_LEN);
		sceIoClose(fd);

		fd = sceIoOpen("ms0:/data.bin", O_CREAT | O_TRUNC | O_WRONLY, 0777);
		if (!fd) {
			print("can't open ms0:/data.bin");
			return;
		}
		sceIoWrite(fd, g_dataBuf, savedata.sizeOfData);	
		sceIoClose(fd);

		fd = sceIoOpen("ms0:/savedata.bin", O_CREAT | O_TRUNC | O_WRONLY, 0777);
		if (!fd) {
			print("can't open ms0:/savedata.bin");
			return;
		}
		sceIoWrite(fd, &savedata, sizeof(savedata));
		sceIoClose(fd);

		fd = sceIoOpen("ms0:/sizeBuffer.bin", O_CREAT | O_TRUNC | O_WRONLY, 0777);
		if (!fd) {
			print("can't open ms0:/sizeBuffer.bin");
			return;
		}
		sceIoWrite(fd, sizeBuffer, sizeof(sizeBuffer));
		sceIoClose(fd);

		fd = sceIoOpen("ms0:/saveFileSecureEntriesAddr.bin", O_CREAT | O_TRUNC | O_WRONLY, 0777);
		if (!fd) {
			print("can't open ms0:/saveFileSecureEntriesAddr.bin");
			return;
		}
		sceIoWrite(fd, saveFileSecureEntriesAddr, sizeof(saveFileSecureEntriesAddr));
		sceIoClose(fd);

		fd = sceIoOpen("ms0:/saveFileSystemEntriesAddr.bin", O_CREAT | O_TRUNC | O_WRONLY, 0777);
		if (!fd) {
			print("can't open ms0:/saveFileSystemEntriesAddr.bin");
			return;
		}
		sceIoWrite(fd, saveFileSystemEntriesAddr, sizeof(saveFileSystemEntriesAddr));
		sceIoClose(fd);

		fd = sceIoOpen("ms0:/saveFileEntriesAddr.bin", O_CREAT | O_TRUNC | O_WRONLY, 0777);
		if (!fd) {
			print("can't open ms0:/saveFileEntriesAddr.bin");
			return;
		}
		sceIoWrite(fd, saveFileEntriesAddr, sizeof(saveFileEntriesAddr));
		sceIoClose(fd);

		fd = sceIoOpen("ms0:/saveFileSecureEntriesAddr.bin", O_CREAT | O_TRUNC | O_WRONLY, 0777);
		if (!fd) {
			print("can't open ms0:/saveFileSecureEntriesAddr.bin");
			return;
		}
		sceIoWrite(fd, saveFileSecureEntriesAddr, sizeof(saveFileSecureEntriesAddr));
		sceIoClose(fd);
	} else if (mode == 12) {
		print("Loading savedata with mode 12 (FILES)...");
		initSavedata(&savedata, mode);
		result = sceUtilitySavedataInitStart((SceUtilitySavedataParam *) &savedata);
		if (result) {
			print("sceUtilitySavedataInitStart failed");
			printHex(result);
			return;
		}
		previousResult = -1;
		while (1) {
			result = sceUtilitySavedataGetStatus();
			if (result != previousResult) {
				print("sceUtilitySavedataGetStatus result:");
				printHex(result);
				previousResult = result;
			}
			if (result == 3) break;
			sceUtilitySavedataUpdate(1);
			sceDisplayWaitVblankStart();
		}

		int numSecureEntries = *((int *) (fileListBuffer + 12));
		int numEntries = *((int *) (fileListBuffer + 16));
		int numSystemEntries = *((int *) (fileListBuffer + 20));

		int i;
		char buffer[100];
		for (i = 0; i < numSecureEntries; i++) {
			sprintf(buffer, "Secure Entry Name: '%s'", (char *) (saveFileSecureEntriesAddr + i * 80 + 64));
			print(buffer);
		}
		for (i = 0; i < numEntries; i++) {
			sprintf(buffer, "Entry Name: '%s'", (char *) (saveFileEntriesAddr + i * 80 + 64));
			print(buffer);
		}
		for (i = 0; i < numSystemEntries; i++) {
			sprintf(buffer, "System Entry Name: '%s'", (char *) (saveFileSystemEntriesAddr + i * 80 + 64));
			print(buffer);
		}

		// write data
		print("writing savedata structure...");
		fd = sceIoOpen("ms0:/savedata.bin", O_CREAT | O_TRUNC | O_WRONLY, 0777);
		if (!fd) {
			print("can't open ms0:/savedata.bin");
			return;
		}
		sceIoWrite(fd, &savedata, sizeof(savedata));
		sceIoClose(fd);

		fd = sceIoOpen("ms0:/fileListBuffer.bin", O_CREAT | O_TRUNC | O_WRONLY, 0777);
		if (!fd) {
			print("can't open ms0:/fileListBuffer.bin");
			return;
		}
		sceIoWrite(fd, fileListBuffer, sizeof(fileListBuffer));
		sceIoClose(fd);

		fd = sceIoOpen("ms0:/saveFileEntriesAddr.bin", O_CREAT | O_TRUNC | O_WRONLY, 0777);
		if (!fd) {
			print("can't open ms0:/saveFileEntriesAddr.bin");
			return;
		}
		sceIoWrite(fd, saveFileEntriesAddr, sizeof(saveFileEntriesAddr));
		sceIoClose(fd);

		fd = sceIoOpen("ms0:/saveFileSecureEntriesAddr.bin", O_CREAT | O_TRUNC | O_WRONLY, 0777);
		if (!fd) {
			print("can't open ms0:/saveFileSecureEntriesAddr.bin");
			return;
		}
		sceIoWrite(fd, saveFileSecureEntriesAddr, sizeof(saveFileSecureEntriesAddr));
		sceIoClose(fd);

		fd = sceIoOpen("ms0:/saveFileSystemEntriesAddr.bin", O_CREAT | O_TRUNC | O_WRONLY, 0777);
		if (!fd) {
			print("can't open ms0:/saveFileSystemEntriesAddr.bin");
			return;
		}
		sceIoWrite(fd, saveFileSystemEntriesAddr, sizeof(saveFileSystemEntriesAddr));
		sceIoClose(fd);
	}

	sceUtilitySavedataShutdownStart();
	previousResult = -1;
	while (1) {
		result = sceUtilitySavedataGetStatus();
		if (result != previousResult) {
			print("sceUtilitySavedataGetStatus result:");
			printHex(result);
			previousResult = result;
		}
		if (result == 4) break;
		sceUtilitySavedataUpdate(1);
		sceDisplayWaitVblankStart();
	}
	y++;
	print("operation successful");

	print("savedata.result:");
	printHex(savedata.result);
}

int xmain(int ra)
{
	SetupCallbacks();

	pgInit();
	pgScreenFrame(1, 0);
	pgFillvram(0);

	mainImpl();
	
	pgWaitVn(500);
	sceKernelExitGame();

	return 0;
}

