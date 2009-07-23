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
unsigned char buffer1[20];
unsigned char buffer2[64];
unsigned char buffer3[28];
unsigned char buffer4[12];
unsigned char buffer5[72 * 0x1f];
unsigned char buffer6[40];
unsigned char buffer7[184];
unsigned char buffer8[100000];
unsigned char buffer9[100000];

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
	unsigned char unknown19[4];
#if FW15
	char unknown20[12];
#else
	unsigned char *ptr1;
	unsigned char *ptr2;
	unsigned char *ptr3;
	char key[16];
	char unknown20[8];
	unsigned char *ptr4;
	unsigned char *ptr5;
	unsigned char *ptr6;
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
	memset(buffer1, 0, sizeof(buffer1));
	memset(buffer2, 0, sizeof(buffer2));
	memset(buffer3, 0, sizeof(buffer3));
	memset(buffer4, 0, sizeof(buffer4));
	memset(buffer5, 0, sizeof(buffer5));
	savedata->ptr1 = buffer1;
	savedata->ptr2 = buffer2;
	strcpy((char *) buffer2, g_gameName);
	strcpy((char *) (buffer2 + 16), g_saveName);
	savedata->ptr3 = buffer3;
	savedata->ptr4 = buffer4;
	*((int *) (buffer4 + 0)) = sizeof(buffer5) / 72;
	*((int *) (buffer4 + 8)) = (int) &buffer5;
	savedata->ptr5 = buffer6;
	savedata->ptr6 = buffer7;
	*((int *) (buffer6 + 24)) = (int) &buffer8;
	*((int *) (buffer6 + 32)) = (int) &buffer9;
	strncpy(savedata->key, "1234567890123456", 16);
#endif
	savedata->overwrite = 1;
	savedata->mode = mode;
	strcpy(savedata->gameNameAsciiZ, g_gameName);
	strcpy(savedata->saveNameAsciiZ, g_saveName);
	strcpy(savedata->dataNameAsciiZ, g_dataName);
	savedata->dataBuf = g_dataBuf;
	savedata->sizeOfDataBuf = sizeof(g_dataBuf);
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
	g_gameName = file;
	g_saveName = g_gameName;
	while (*g_saveName >= 32) g_saveName++;
	*g_saveName = 0;
	while (*g_saveName < 32) g_saveName++;
	g_dataName = g_saveName;
	while (*g_dataName >= 32) g_dataName++;
	*g_dataName = 0;
	while (*g_dataName < 32) g_dataName++;
	char* tmp = g_dataName;
	while (*tmp >= 32) tmp++;
	*tmp = 0;

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
	print("press 'x' for load or 'o' for update savedata,");
	print("press triangle for savedata mode 8");
	print("press square for savedata mode 11");
	print("press up for savedata mode 15");
	y++;

	sceCtrlSetSamplingCycle(0); 
	sceCtrlSetSamplingMode(0); 
	SceCtrlData ctrl;
	int update;
	while(1) { 
		sceCtrlReadBufferPositive(&ctrl, 1); 
		if (ctrl.Buttons & CTRL_CROSS) { 
			update = 0;
			break;
		} else if (ctrl.Buttons & CTRL_CIRCLE) { 
			update = 1;
			break;
		} else if (ctrl.Buttons & CTRL_TRIANGLE) {
			update = 2;
			break;
		} else if (ctrl.Buttons & CTRL_SQUARE) {
			update = 3;
			break;
		} else if (ctrl.Buttons & CTRL_UP) {
			update = 4;
			break;
		}
		sceDisplayWaitVblankStart();
	}

	// write savedata or update
	if (update == 1) {
		initSavedata(&savedata, 1);

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
	} else if (update == 0) {
		// load savedata
		print("loading savedata...");
		initSavedata(&savedata, 0);
		savedata.readIcon0Buf = g_readIcon0;
		savedata.sizeOfReadIcon0Buf = sizeof(g_readIcon0);
		savedata.readIcon1Buf = g_readIcon1;
		savedata.sizeOfReadIcon1Buf = sizeof(g_readIcon1);
		savedata.readPic1Buf = g_readPic1;
		savedata.sizeOfReadPic1Buf = sizeof(g_readPic1);
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
	} else if (update == 2) {
		// Test savedata mode 8
		print("loading savedata with mode 8...");
		initSavedata(&savedata, 8);
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
		sprintf(buffer, "Free space: %s", buffer1 + 12);
		print(buffer);
		sprintf(buffer, "Required space1: %s", buffer3 + 8);
		print(buffer);
		sprintf(buffer, "Required space2: %s", buffer3 + 20);
		print(buffer);
		fd = sceIoOpen("ms0:/buffer1.bin", O_CREAT | O_TRUNC | O_WRONLY, 0777);
		if (!fd) {
			print("can't open ms0:/buffer1.bin");
			return;
		}
		sceIoWrite(fd, buffer1, sizeof(buffer1));
		sceIoClose(fd);

		fd = sceIoOpen("ms0:/buffer2.bin", O_CREAT | O_TRUNC | O_WRONLY, 0777);
		if (!fd) {
			print("can't open ms0:/buffer2.bin");
			return;
		}
		sceIoWrite(fd, buffer2, sizeof(buffer2));
		sceIoClose(fd);

		fd = sceIoOpen("ms0:/buffer3.bin", O_CREAT | O_TRUNC | O_WRONLY, 0777);
		if (!fd) {
			print("can't open ms0:/buffer3.bin");
			return;
		}
		sceIoWrite(fd, buffer3, sizeof(buffer3));
		sceIoClose(fd);
#endif
	} else if (update == 3) {
		// Test savedata mode 11
		print("loading savedata with mode 11...");
		initSavedata(&savedata, 11);
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

		int numEntries = *((int *) (buffer4 + 4));
		int i;
		char buffer[100];
		for (i = 0; i < numEntries; i++) {
			sprintf(buffer, "Name: %s", (char *) (buffer5 + i * 72 + 52));
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
		fd = sceIoOpen("ms0:/buffer4.bin", O_CREAT | O_TRUNC | O_WRONLY, 0777);
		if (!fd) {
			print("can't open ms0:/buffer4.bin");
			return;
		}
		sceIoWrite(fd, buffer4, sizeof(buffer4));
		sceIoClose(fd);

		fd = sceIoOpen("ms0:/buffer5.bin", O_CREAT | O_TRUNC | O_WRONLY, 0777);
		if (!fd) {
			print("can't open ms0:/buffer5.bin");
			return;
		}
		sceIoWrite(fd, buffer5, sizeof(buffer5));
		sceIoClose(fd);

		fd = sceIoOpen("ms0:/buffer6.bin", O_CREAT | O_TRUNC | O_WRONLY, 0777);
		if (!fd) {
			print("can't open ms0:/buffer6.bin");
			return;
		}
		sceIoWrite(fd, buffer6, sizeof(buffer6));
		sceIoClose(fd);

		fd = sceIoOpen("ms0:/buffer7.bin", O_CREAT | O_TRUNC | O_WRONLY, 0777);
		if (!fd) {
			print("can't open ms0:/buffer7.bin");
			return;
		}
		sceIoWrite(fd, buffer7, sizeof(buffer7));
		sceIoClose(fd);

		fd = sceIoOpen("ms0:/buffer8.bin", O_CREAT | O_TRUNC | O_WRONLY, 0777);
		if (!fd) {
			print("can't open ms0:/buffer8.bin");
			return;
		}
		sceIoWrite(fd, buffer8, sizeof(buffer8));
		sceIoClose(fd);

		fd = sceIoOpen("ms0:/buffer9.bin", O_CREAT | O_TRUNC | O_WRONLY, 0777);
		if (!fd) {
			print("can't open ms0:/buffer9.bin");
			return;
		}
		sceIoWrite(fd, buffer9, sizeof(buffer9));
		sceIoClose(fd);
#endif
	} else if (update == 4) {
		// Test savedata mode 15
		print("loading savedata with mode 15...");
		initSavedata(&savedata, 15);
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

