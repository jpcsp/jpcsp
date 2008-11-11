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

#include "pg.h"

unsigned char* g_dataBuf = (unsigned char*)0x9100000;
unsigned char* g_readIcon0 = (unsigned char*)0x9200000;
unsigned char* g_readIcon1 = (unsigned char*)0x9300000;
unsigned char* g_readPic1 = (unsigned char*)0x9400000;

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

int y = 3;

void print(char* text)
{
	pgPrint(0, y++, 0xffff, text);
}


void printHex(int value) {
	const unsigned char* digits = "0123456789abcdef";
	int i;
	int x = 0;
	for (i = 7; i >= 0; i--) {
		pgPutChar((x+i)*8, y*8, 0xffff, 0, digits[value&0xf], 1, 0, 1);
		value >>= 4;
	}
	y++;
}

void *memcpy(void* dest, void* src, int size)
{
	void* save = dest;
	int i;
	for (i = 0; i < size; i++) {
		*((unsigned char*)dest) = *((unsigned char*)src);
		dest++;
		src++;
	}
	return save;
}

char *strcpy(char *strDestination, const char *strSource)
{
	char* save = strDestination;
	while (*strSource) {
		*strDestination = *strSource;
		strSource++;
		strDestination++;
	}
	*strDestination = *strSource;
	return save;
}

char *strcat(char *strDestination, const char *strSource) {
	char* save = strDestination;
	while (*strDestination) strDestination++;
	strcpy(strDestination, strSource);
	return save;
}

void *memset(void* dest, int c, int count)
{
	int i;
	void* save = dest;
	for (i = 0; i < count; i++) {
		*((unsigned char*)dest) = c;
		dest++;
	}
	return save;
}

int exit_callback(void)
{
	sceKernelExitGame();

	return 0;
}

int CallbackThread(void *arg)
{
	int cbid;

	cbid = sceKernelCreateCallback("Exit Callback", exit_callback);
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
	int unknown1;
	int unknown2;
	int unknown3;
	int unknown4;
	int unknown5;
	int unknown6;
	int unknown7;
	int unknown8;
	int unknown9;
	int unknown10;
	int unknown11;
	int mode;
	int unknown12;
	int unknown13;
	char gameNameAsciiZ[16];
	char saveNameAsciiZ[24];
	char dataDotBinAsciiZ[16];
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
	unsigned char unknown17[0x18];
} SceUtilitySavedataParam;

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

/* Returned control data */ 
typedef struct _ctrl_data 
{ 
	int frame; 
	int buttons; 
	unsigned char analog[4]; 
	int unused; 
} ctrl_data_t; 

void initSavedata(SceUtilitySavedataParam* savedata, int mode) {
	memset(savedata, 0, sizeof(SceUtilitySavedataParam));
	savedata->size = sizeof(SceUtilitySavedataParam);
	savedata->unknown3 = 0x11;
	savedata->unknown4 = 0x13;
	savedata->unknown5 = 0x12;
	savedata->unknown6 = 0x10;
	savedata->unknown13 = 1;
	savedata->mode = mode;
	strcpy(savedata->gameNameAsciiZ, g_gameName);
	strcpy(savedata->saveNameAsciiZ, g_saveName);
	strcpy(savedata->dataDotBinAsciiZ, g_dataName);
	savedata->dataBuf = g_dataBuf;
	savedata->sizeOfDataBuf = 0x100000;
}

void mainImpl()
{
	int i, result;

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
	print("press 'x' for load or 'o' for update savedata");
	y++;

	sceCtrlSetSamplingCycle(0); 
	sceCtrlSetSamplingMode(0); 
	ctrl_data_t ctrl;
	int update;
	while(1) { 
		sceCtrlReadBufferPositive(&ctrl, 1); 
		if (ctrl.buttons & CTRL_CROSS) { 
			update = 0;
			break;
		} else if (ctrl.buttons & CTRL_CIRCLE) { 
			update = 1;
			break;
		} 
		sceDisplayWaitVblankStart();
	}

	// write savedata or update
	if (update) {
		SceUtilitySavedataParam savedata;
		initSavedata(&savedata, 1);

		// load extracted data
		print("loading extracted savedata...");
		fd = sceIoOpen("ms0:/params.bin", O_RDONLY, 0);
		if (!fd) {
			print("can't open ms0:/params.bin");
			return;
		}
		int r = sceIoRead(fd, &savedata.paramsSfoTitle, PARAMS_LEN);
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
		result = sceUtilitySavedataInitStart(savedata);
		if (result) {
			print("sceUtilitySavedataInitStart failed");
			printHex(result);
			return;
		}
		while (1) {
			result = sceUtilitySavedataGetStatus();
			if (result == 3) break;
			sceUtilitySavedataUpdate(1);
			sceDisplayWaitVblankStart();
		}
	} else {
		// load savedata
		print("loading savedata...");
		SceUtilitySavedataParam savedata;
		initSavedata(&savedata, 0);
		savedata.readIcon0Buf = g_readIcon0;
		savedata.sizeOfReadIcon0Buf = 0x100000;
		savedata.readIcon1Buf = g_readIcon1;
		savedata.sizeOfReadIcon1Buf = 0x100000;
		savedata.readPic1Buf = g_readPic1;
		savedata.sizeOfReadPic1Buf = 0x100000;
		result = sceUtilitySavedataInitStart(savedata);
		if (result) {
			print("sceUtilitySavedataInitStart failed");
			printHex(result);
			return;
		}
		while (1) {
			result = sceUtilitySavedataGetStatus();
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
	}
	sceUtilitySavedataShutdownStart();
	while (1) {
		result = sceUtilitySavedataGetStatus();
		if (result == 4) break;
		sceUtilitySavedataUpdate(1);
		sceDisplayWaitVblankStart();
	}
	y++;
	print("operation successful");
}

int xmain(int ra)
{
	SetupCallbacks();

	pgInit();
	pgScreenFrame(1, 0);
	pgFillvram(0);

	mainImpl();
	
	pgWaitVn(200);
	sceKernelExitGame();

	return 0;
}
