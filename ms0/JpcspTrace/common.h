/*
This file is part of jpcsp.

Jpcsp is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Jpcsp is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Jpcsp.  If not, see <http://www.gnu.org/licenses/>.
 */

#define DEBUG			0
#define DEBUG_MUTEX		0
#define DEBUG_UTILITY_SAVEDATA		1
#define DEBUG_UTILITY_OSK			1
#define DEBUG_UTILITY_MSG			1
#define DEFAULT_LOG_BUFFER_SIZE		8*1024

#define ALIGN_UP(n, alignment) (((n) + ((alignment) - 1)) & ~((alignment) - 1))

#define TYPE_HEX32		0
#define TYPE_INT32		1
#define TYPE_STRING		2
#define TYPE_POINTER32	3
#define TYPE_POINTER64	4
#define TYPE_VARSTRUCT	5
#define TYPE_FONT_INFO	6
#define TYPE_FONT_CHAR_INFO	7
#define TYPE_MPEG_EP	8
#define TYPE_MPEG_AU	9
#define TYPE_MP4_TRACK	10

#define FLAG_LOG_BEFORE_CALL	(1 << 0)
#define FLAG_LOG_AFTER_CALL	(1 << 1)
#define FLAG_LOG_FREEMEM	(1 << 2)
#define FLAG_LOG_STACK_USAGE	(1 << 3)

#define NID_sceIoWrite	0x42EC03AC
#define NID_sceIoOpen	0x109F50BC
#define NID_sceIoClose	0x810C4BC3
#define NID_sceIoOpen_stargate	0x7C8EFE7D
#define NID_sceIoClose_stargate	0x747A373E

#define IS_sceIoOpen_NID(nid) ((nid) == NID_sceIoOpen || (nid) == NID_sceIoOpen_stargate)
#define IS_sceIoClose_NID(nid) ((nid) == NID_sceIoClose || (nid) == NID_sceIoClose_stargate)

typedef struct {
	SceUID logFd;
	int logKeepOpen;
	char *logBuffer;
	int logBufferLength;
	int maxLogBufferLength;
	void *freeAddr;
	int freeSize;
	volatile int inWriteLog;
	int bufferLogWrites;
} CommonInfo;

typedef struct SyscallInfo {
	u64 (*originalEntry)(u32, u32, u32, u32, u32, u32, u32, u32);
	u32 nid;
	int numParams;
	u32 paramTypes;
	u32 flags;
	char *name;
	u64 (*newEntry)(u32, u32, u32, u32, u32, u32, u32, u32, struct SyscallInfo *);
	struct SyscallInfo *next;
	CommonInfo *commonInfo;
} SyscallInfo;

extern char *logFilename;
extern CommonInfo *commonInfo;
extern int (* ioOpen)(const char *s, int flags, int permissions);
extern int (* ioWrite)(SceUID id, const void *data, int size);
extern int (* ioClose)(SceUID id);

void *alloc(int size);
char *append(char *dst, const char *src);
char *appendHex(char *dst, u32 hex, int numDigits);
char *appendInt(char *dst, s32 n, int numDigits);
void openLogFile();
void closeLogFile();
void flushLogBuffer();
void writeLog(const char *s, int length);
void printLog(const char *s);
void printLogH(const char *s1, int hex, const char *s2);
void printLogS(const char *s1, const char *s2, const char *s3);
void printLogHH(const char *s1, int hex1, const char *s2, int hex2, const char *s3);
void printLogSH(const char *s1, const char *s2, const char *s3, int hex, const char *s4);
void printLogHS(const char *s1, int hex, const char *s2, const char *s3, const char *s4);
void printLogSS(const char *s1, const char *s2, const char *s3, const char *s4, const char *s5);
void printLogMem(const char *s1, int addr, int length);
void syscallLog(const SyscallInfo *syscallInfo, const u32 *parameters, u64 result, u32 ra, u32 sp, u32 gp);
int userIoOpen(const char *s, int flags, int permissions);
int userIoWrite(SceUID id, const void *data, int size);
int userIoClose(SceUID id);
void prepareStackUsage(u32 sp);
void logStackUsage(const SyscallInfo *syscallInfo);
