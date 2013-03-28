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
#define DEFAULT_LOG_BUFFER_SIZE		1024

#define USER_PARTITION_ID	2

#define ALIGN_UP(n, alignment) (((n) + ((alignment) - 1)) & ~((alignment) - 1))

#define TYPE_HEX32		0
#define TYPE_INT32		1
#define TYPE_STRING		2
#define TYPE_POINTER32	3
#define TYPE_VARSTRUCT	4

typedef struct {
	SceUID logFd;
	int logKeepOpen;
	char *logBuffer;
	int logBufferLength;
	int maxLogBufferLength;
	void *freeAddr;
	int freeSize;
	volatile int inWriteLog;
} CommonInfo;

typedef struct SyscallInfo {
	u64 (*originalEntry)(u32, u32, u32, u32, u32, u32, u32, u32);
	u32 nid;
	int numParams;
	u32 paramTypes;
	char *name;
	u64 (*newEntry)(u32, u32, u32, u32, u32, u32, u32, u32, struct SyscallInfo *);
	struct SyscallInfo *next;
	CommonInfo *commonInfo;
} SyscallInfo;

extern CommonInfo *commonInfo;

void *alloc(int size);
char *append(char *dst, const char *src);
char *appendHex(char *dst, u32 hex, int numDigits);
char *appendInt(char *dst, s32 n, int numDigits);
void openLogFile();
void closeLogFile();
void writeLog(const char *s, int length);
void printLog(const char *s);
void printLogH(const char *s1, int hex, const char *s2);
void printLogS(const char *s1, const char *s2, const char *s3);
void printLogHH(const char *s1, int hex1, const char *s2, int hex2, const char *s3);
void printLogSH(const char *s1, const char *s2, const char *s3, int hex, const char *s4);
void printLogHS(const char *s1, int hex, const char *s2, const char *s3, const char *s4);
void printLogSS(const char *s1, const char *s2, const char *s3, const char *s4, const char *s5);
void printLogMem(const char *s1, int addr, int length);
void syscallLog(const SyscallInfo *syscallInfo, const u32 *parameters, u64 result);
