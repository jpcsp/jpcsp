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
#include <pspsdk.h>
#include <pspkernel.h>
#include <pspinit.h>
#include <psploadcore.h>
#include <psputilsforkernel.h>
#include <pspsysmem_kernel.h>
#include <pspsysmem.h>
#include <psprtc.h>
#include <string.h>
#include "common.h"

char *hexDigits = "0123456789ABCDEF";
int logTimestamp = 1;
int logThreadName = 1;
int logRa = 0;
CommonInfo *commonInfo;
void *freeAddr = NULL;
int freeSize = 0;
char *logFilename = "ms0:/log.txt";

#if DEBUG_MUTEX
typedef struct {
	SceSize size;
	char name[32];
	u32 attr;
	u32 initCount;
	u32 lockedCount;
	u32 threadid;
	u32 numWaitThreads;
} SceKernelMutexInfo;

int (* referMutex)(SceUID, SceKernelMutexInfo *) = NULL;
SceKernelMutexInfo mutexInfo;
#endif

#if DUMP_VIDEOCODEC_FRAMES
int videocodecFrame = 0;
#endif

#if DUMP_sceMpegBaseCscAvc_CALLS
int sceMpegBaseCscAvcCall = 0;
#endif

#if DUMP_sceMpegBaseYCrCbCopy_CALLS
int sceMpegBaseYCrCbCopyCall = 0;
#endif

int (* ioOpen)(const char *s, int flags, int permissions) = userIoOpen;
int (* ioWrite)(SceUID id, const void *data, int size) = userIoWrite;
int (* ioClose)(SceUID id) = userIoClose;

int userIoOpen(const char *s, int flags, int permissions) {
	return sceIoOpen(s, flags, permissions);
}

int userIoWrite(SceUID id, const void *data, int size) {
	return sceIoWrite(id, data, size);
}

int userIoClose(SceUID id) {
	return sceIoClose(id);
}

void *alloc(int size) {
	void *allocAddr;

	size = ALIGN_UP(size, 4);

	if (commonInfo != NULL && commonInfo->freeAddr != NULL) {
		freeAddr = commonInfo->freeAddr;
		freeSize = commonInfo->freeSize;
	}

	if (freeSize >= size) {
		freeSize -= size;
		allocAddr = freeAddr + freeSize;
	} else {
		int allocSize = ALIGN_UP(size, 256);
		int result = sceKernelAllocPartitionMemory(PSP_MEMORY_PARTITION_USER, "JpcspTrace", PSP_SMEM_High, allocSize, 0);
		if (result >= 0) {
			void *newFreeAddr = sceKernelGetBlockHeadAddr(result);
			if (newFreeAddr + allocSize != freeAddr) {
				// Can't merge new allocated memory to previously allocated memory
				freeSize = 0;
			}
			freeAddr = newFreeAddr;
			freeSize += allocSize - size;
			allocAddr = freeAddr + freeSize;
		} else {
			allocAddr = NULL;
		}
	}

	if (commonInfo != NULL) {
		commonInfo->freeAddr = freeAddr;
		commonInfo->freeSize = freeSize;
	}

	return allocAddr;
}

void freeAlloc(void *buffer, int size) {
	if (freeAddr + freeSize == buffer) {
		freeSize += size;
	}
}

char *append(char *dst, const char *src) {
	while ((*dst = *src) != '\0') {
		src++;
		dst++;
	}

	return dst;
}

char *appendHexNoPrefix(char *dst, u32 hex, int numDigits) {
	int i;
	int leadingZero = 1;
	if (numDigits <= 0) {
		// Display at least 1 digit
		numDigits = 1;
	}
	numDigits <<= 2;
	for (i = 28; i >= 0; i -= 4) {
		int digit = (hex >> i) & 0xF;
		if (digit > 0 || !leadingZero || i < numDigits) {
			*dst++ = hexDigits[digit];
			leadingZero = 0;
		}
	}
	*dst = '\0';

	return dst;
}

char *appendHex(char *dst, u32 hex, int numDigits) {
	*dst++ = '0';
	*dst++ = 'x';
	return appendHexNoPrefix(dst, hex, numDigits);
}

char *appendInt(char *dst, s32 n, int numDigits) {
	if (n == 0) {
		*dst++ = '0';
		for (numDigits--; numDigits > 0; numDigits--) {
			*dst++ = '0';
		}
	} else {
		if (n < 0) {
			*dst++ = '-';
			n = -n;
		}

		int factor = 1000000000;
		int leadingZero = 1;
		int factorDigits = 10;
		while (factor > 0) {
			int digit = n / factor;
			if (digit > 0 || !leadingZero || factorDigits <= numDigits) {
				*dst++ = '0' + digit;
				n -= digit * factor;
				leadingZero = 0;
			}
			factor /= 10;
			factorDigits--;
		}
	}
	*dst = '\0';

	return dst;
}

void openLogFile() {
	if (commonInfo->logFd < 0) {
		commonInfo->logFd = ioOpen(logFilename, PSP_O_WRONLY | PSP_O_CREAT | PSP_O_APPEND, 0777);
	}
}

void closeLogFile() {
	ioClose(commonInfo->logFd);
	commonInfo->logFd = -1;
}

void allocLogBuffer() {
	// Allocate a buffer if not yet allocated
	if (commonInfo->logBuffer == NULL) {
		commonInfo->logBuffer = alloc(commonInfo->maxLogBufferLength);
	}
}

void appendToLogBuffer(const char *s, int length) {
	allocLogBuffer();

	if (commonInfo->logBuffer != NULL) {
		int restLength = commonInfo->maxLogBufferLength - commonInfo->logBufferLength;
		int truncated = 0;
		if (length > restLength) {
			length = restLength;
			truncated = 1;
		}

		if (length > 0) {
			memcpy(commonInfo->logBuffer + commonInfo->logBufferLength, s, length);
			commonInfo->logBufferLength += length;

			// If we have truncated the string to be logged,
			// set "...\n" at the end of the log buffer.
			if (truncated) {
				char *addr = commonInfo->logBuffer + commonInfo->logBufferLength - 4;
				*addr++ = '.';
				*addr++ = '.';
				*addr++ = '.';
				*addr++ = '\n';
			}
		}
	}
}

void flushLogBuffer() {
	while (commonInfo->logBufferLength > 0) {
		// Try to write pending output.
		// This will succeed as soon as the interrupts are enabled again.
		int length = ioWrite(commonInfo->logFd, commonInfo->logBuffer, commonInfo->logBufferLength);
		if (length <= 0) {
			break;
		}

		commonInfo->logBufferLength -= length;
		if (commonInfo->logBufferLength > 0) {
			memcpy(commonInfo->logBuffer, commonInfo->logBuffer + length, commonInfo->logBufferLength);
		}
	}
}

void writeLog(const char *s, int length) {
	int bufferLogWrites = commonInfo->bufferLogWrites;
	int restLogBufferLength = commonInfo->maxLogBufferLength - commonInfo->logBufferLength;
	if (restLogBufferLength < length) {
		bufferLogWrites = 0;
	}

	if (commonInfo->inWriteLog || bufferLogWrites) {
		appendToLogBuffer(s, length);
		return;
	}

	commonInfo->inWriteLog++;

	if (!commonInfo->logKeepOpen) {
		openLogFile();
	}

	flushLogBuffer();

	if (ioWrite(commonInfo->logFd, s, length) < 0) {
		// Can't write to the log file right now, probably because the interrupts are disabled.
		// Save the log string for later output.
		appendToLogBuffer(s, length);
	} else {
		flushLogBuffer();
	}

	if (!commonInfo->logKeepOpen) {
		closeLogFile();
	}

	commonInfo->inWriteLog--;
}

void printLog(const char *s) {
	writeLog(s, strlen(s));
}

void printLogH(const char *s1, int hex, const char *s2) {
	char buffer[200];
	char *s = buffer;

	s = append(s, s1);
	s = appendHex(s, hex, 0);
	s = append(s, s2);
	writeLog(buffer, s - buffer);
}

void printLogS(const char *s1, const char *s2, const char *s3) {
	char buffer[200];
	char *s = buffer;

	s = append(s, s1);
	s = append(s, s2);
	s = append(s, s3);
	writeLog(buffer, s - buffer);
}

void printLogHH(const char *s1, int hex1, const char *s2, int hex2, const char *s3) {
	char buffer[200];
	char *s = buffer;

	s = append(s, s1);
	s = appendHex(s, hex1, 0);
	s = append(s, s2);
	s = appendHex(s, hex2, 0);
	s = append(s, s3);
	writeLog(buffer, s - buffer);
}

void printLogSH(const char *s1, const char *s2, const char *s3, int hex, const char *s4) {
	char buffer[200];
	char *s = buffer;

	s = append(s, s1);
	s = append(s, s2);
	s = append(s, s3);
	s = appendHex(s, hex, 0);
	s = append(s, s4);
	writeLog(buffer, s - buffer);
}

void printLogHS(const char *s1, int hex, const char *s2, const char *s3, const char *s4) {
	char buffer[200];
	char *s = buffer;

	s = append(s, s1);
	s = appendHex(s, hex, 0);
	s = append(s, s2);
	s = append(s, s3);
	s = append(s, s4);
	writeLog(buffer, s - buffer);
}

void printLogSS(const char *s1, const char *s2, const char *s3, const char *s4, const char *s5) {
	char buffer[200];
	char *s = buffer;

	s = append(s, s1);
	s = append(s, s2);
	s = append(s, s3);
	s = append(s, s4);
	s = append(s, s5);
	writeLog(buffer, s - buffer);
}

char *flushBuffer(char *buffer, char *s) {
	*s++ = '\n';
	writeLog(buffer, s - buffer);

	return buffer;
}

void printLogMem(const char *s1, int addr, int length) {
	int i, j;
	int lineStart;
	char buffer[100];
	char *s = buffer;

	s = append(s, s1);
	s = appendHex(s, addr, 8);
	s = append(s, ":\n");
	if (addr != 0) {
		lineStart = 0;
		for (i = 0; i < length; i++) {
			if (i > 0) {
				if ((i % 16) == 0) {
					s = append(s, "  >");
					for (j = lineStart; j < i; j++) {
						char c = _lb(addr + j);
						if (c < ' ' || c > '~') {
							c = '.';
						}
						*s++ = c;
					}
					*s++ = '<';
					s = flushBuffer(buffer, s);
					lineStart = i;
				} else {
					s = append(s, " ");
				}
			}
			s = appendHexNoPrefix(s, _lb(addr + i), 2);
		}
	}
	flushBuffer(buffer, s);
}

#ifdef DEBUG_UTILITY_SAVEDATA
void *utilitySavedataParams = NULL;

void utilitySavedataLog(char *buffer, const SyscallInfo *syscallInfo, u32 param) {
	char *s = buffer;
	int fd;

	if (syscallInfo->nid != 0x50C4CD57 && syscallInfo->nid != 0x9790B33C) {
		return;
	}

	if (syscallInfo->nid == 0x50C4CD57) {
		utilitySavedataParams = (void *) param;
	}
	if (utilitySavedataParams == NULL) {
		return;
	}

	int mode = _lw((int) utilitySavedataParams + 48);
	s = append(s, "mode=");
	s = appendInt(s, mode, 0);
	s = append(s, ", gameName=");
	s = append(s, utilitySavedataParams + 60);
	s = append(s, ", saveName=");
	s = append(s, utilitySavedataParams + 76);
	s = append(s, ", fileName=");
	s = append(s, utilitySavedataParams + 100);
	if (syscallInfo->nid == 0x9790B33C) {
		s = append(s, ", result=");
		s = appendHex(s, _lw((int) utilitySavedataParams + 28), 8);
	}
	s = append(s, "\n");
	writeLog(buffer, s - buffer);
	s = buffer;

	printLogMem("Data ", _lw((int) utilitySavedataParams + 116), 16);

	int fileListAddr = _lw((int) utilitySavedataParams + 1528);
	if (fileListAddr != 0 && mode == 12) { // MODE_FILES
		printLogMem("FileList ", fileListAddr, 36);
		if (syscallInfo->nid == 0x9790B33C) { // sceUtilitySavedataShutdownStart
			int saveFileSecureNumEntries = _lw(fileListAddr + 12);
			int saveFileNumEntries = _lw(fileListAddr + 16);
			int systemFileNumEntries = _lw(fileListAddr + 20);
			int saveFileSecureEntriesAddr = _lw(fileListAddr + 24);
			int saveFileEntriesAddr = _lw(fileListAddr + 28);
			int systemEntriesAddr = _lw(fileListAddr + 32);
			printLogMem("SecureEntries ", saveFileSecureEntriesAddr, saveFileSecureNumEntries * 80);
			printLogMem("NormalEntries ", saveFileEntriesAddr, saveFileNumEntries * 80);
			printLogMem("SystemEntries ", systemEntriesAddr, systemFileNumEntries * 80);
		}
	}

	printLogMem("Params ", (int) utilitySavedataParams, _lw((int) utilitySavedataParams + 0));

	if (syscallInfo->nid == 0x9790B33C) {
		fd = ioOpen("ms0:/SavedataStruct.bin", PSP_O_WRONLY | PSP_O_CREAT | PSP_O_APPEND, 0777);
		ioWrite(fd, utilitySavedataParams, _lw((int) utilitySavedataParams + 0));
		ioClose(fd);

		fd = ioOpen("ms0:/SavedataData.bin", PSP_O_WRONLY | PSP_O_CREAT | PSP_O_APPEND, 0777);
		ioWrite(fd, (void *) _lw((int) utilitySavedataParams + 116), _lw((int) utilitySavedataParams + 124));
		ioClose(fd);
	}
}
#endif

#ifdef DEBUG_UTILITY_OSK
void *utilityOskParams = NULL;

void utilityOskLog(char *buffer, const SyscallInfo *syscallInfo, u32 param) {
	char *s = buffer;

	if (syscallInfo->nid != 0xF6269B82 && syscallInfo->nid != 0x3DFAEBA9) {
		return;
	}

	if (syscallInfo->nid == 0xF6269B82) {
		utilityOskParams = (void *) param;
	}

	int oskDataAddr = _lw((int) utilityOskParams + 52);
	if (oskDataAddr != 0) {
		s = append(s, "inputMode=");
		s = appendHex(s, _lw(oskDataAddr + 0), 0);
		s = append(s, ", inputAttr=");
		s = appendHex(s, _lw(oskDataAddr + 4), 0);
	}
	if (syscallInfo->nid == 0x3DFAEBA9) {
		s = append(s, ", result=");
		s = appendHex(s, _lw((int) utilityOskParams + 28), 8);
	}
	s = flushBuffer(buffer, s);

	printLogMem("Params ", (int) utilityOskParams, _lw((int) utilityOskParams + 0));
}
#endif

#ifdef DEBUG_UTILITY_MSG
void *utilityMsgParams = NULL;

void utilityMsgLog(char *buffer, const SyscallInfo *syscallInfo, u32 param) {
	char *s = buffer;

	if (syscallInfo->nid != 0x2AD8E239 && syscallInfo->nid != 0x67AF3428) {
		return;
	}

	if (syscallInfo->nid == 0x2AD8E239) {
		utilityMsgParams = (void *) param;
	}

	s = append(s, "result=");
	s = appendHex(s, _lw((int) utilityMsgParams + 48), 0);
	s = append(s, ", mode=");
	s = appendHex(s, _lw((int) utilityMsgParams + 52), 0);
	s = append(s, ", errorValue=");
	s = appendHex(s, _lw((int) utilityMsgParams + 56), 0);
	s = append(s, ", options=");
	s = appendHex(s, _lw((int) utilityMsgParams + 572), 0);
	s = append(s, ", buttonPressed=");
	s = appendHex(s, _lw((int) utilityMsgParams + 576), 0);
	if (syscallInfo->nid == 0x67AF3428) {
		s = append(s, ", result=");
		s = appendHex(s, _lw((int) utilityMsgParams + 28), 8);
	}
	flushBuffer(buffer, s);

#if 0
	s = buffer;
	s = append(s, "message=");
	s = append(s, utilityMsgParams + 60);
	s = append(s, "\n");
	writeLog(buffer, s - buffer);

	printLogMem("Params ", (int) utilityMsgParams, _lw((int) utilityMsgParams + 0));
#endif
}
#endif

char *syscallLogMem(char *buffer, char *s, int addr, int length) {
	int i, j;
	int lineStart;
	int value;

	s = appendHex(s, addr, 8);
	if (addr != 0) {
		*s++ = ':';
		*s++ = '\n';
		writeLog(buffer, s - buffer);
		s = buffer;
		lineStart = 0;
		for (i = 0; i < length; i++) {
			if (i > 0) {
				if ((i % 16) == 0) {
					s = append(s, " >");
					for (j = lineStart; j < i; j++) {
						char c = _lb(addr + j);
						if (c < ' ' || c > '~') {
							c = '.';
						}
						*s++ = c;
					}
	 				*s++ = '<';
					s = flushBuffer(buffer, s);
					lineStart = i;
				} else {
					*s++ = ' ';
				}
			}

			value = _lb(addr + i);
			*s++ = hexDigits[value >> 4];
			*s++ = hexDigits[value & 0x0F];
		}
	}

	return s;
}

int maxStackUsage = 0x1000;
int stackValue = 0xABCD1234;
int stackBase;

void prepareStackUsage(u32 sp) {
	int i;
	stackBase = (u32) &i;

	for (i = 4; i <= maxStackUsage; i += 4) {
		_sw(stackValue, stackBase - i);
	}
}

void logStackUsage(const SyscallInfo *syscallInfo) {
	int stackUsage = 0;
	int i;
	for (i = maxStackUsage; i > 0; i -= 4) {
		if (_lw(stackBase - i) != stackValue) {
			stackUsage = i;
			break;
		} else {
			_sw(0, stackBase - i);
		}
	}

	printLogSH("Stack usage ", syscallInfo->name, ": ", stackUsage, "\n");
}


void syscallLog(const SyscallInfo *syscallInfo, int inOut, const u32 *parameters, u64 result, u32 ra, u32 sp, u32 gp) {
	char buffer[250];
	char *s = buffer;
	int i;
	int length;
	int videocodec = 0;
	int videocodecType = -1;

	// Don't log our own sceIoWrite and sceIoClose
	if ((syscallInfo->nid == NID_sceIoWrite || IS_sceIoClose_NID(syscallInfo->nid)) && parameters[0] == commonInfo->logFd) {
		return;
	}
	// Don't log our own sceIoOpen
	if (IS_sceIoOpen_NID(syscallInfo->nid) && strcmp((const char *) parameters[0], logFilename) == 0) {
		return;
	}

#if DUMP_MMIO
#	define LW(a) (*((volatile u32*)(a)))
#	define SW(a, n) (*((volatile u32*)(a))=(n))

	u32 i0 = LW(0xBC300004);
	SW(0xBC300004, 0xFFFFFFFF);
	u32 i1 = LW(0xBC300004);
	SW(0xBC300004, i0);
	printLogHH("0xBC300004 write32: ", i0, " -> ", i1, "\n");

	u32 i2 = LW(0xBC300008);
	SW(0xBC300008, 0xFFFFFFFF);
	u32 i3 = LW(0xBC300008);
	SW(0xBC300008, i2);
	printLogHH("0xBC300008 write32: ", i2, " -> ", i3, "\n");

	u32 fuseConfig = LW(0xBC100098);
	printLogH("fuseConfig: ", fuseConfig, "\n");

	int n = 0;
	u32 n4 = _lw(0xBE740004);
	u32 n8 = _lw(0xBE740008);
	while (n < 20) {
		u32 i4 = _lw(0xBE740004);
		u32 i8 = _lw(0xBE740008);
		if (i4 != n4 || i8 != n8) {
			printLogHH("0xBE740004: ", i4, " ", i8, "\n");
			printLogHH("0xBC300000: ", _lw(0xBC300000), " ", _lw(0xBC300010), "\n");
			n++;
			n4 = i4;
			n8 = i8;
		}
	}
#endif

	if (syscallInfo->flags & FLAG_LOG_FREEMEM) {
		// Allocate the logBuffer first to report a proper free mem size
		allocLogBuffer();

		u32 maxFreeMem = sceKernelMaxFreeMemSize();
		u32 totalFreeMem = sceKernelTotalFreeMemSize();

		printLogHH("TotalFreeMem=", totalFreeMem, ", MaxFreeMem=", maxFreeMem, "\n");
	}

	if (logTimestamp) {
		pspTime time;
		if (sceRtcGetCurrentClockLocalTime(&time) == 0) {
			s = appendInt(s, time.hour, 2);
			*s++ = ':';
			s = appendInt(s, time.minutes, 2);
			*s++ = ':';
			s = appendInt(s, time.seconds, 2);
			*s++ = '.';
			s = appendInt(s, time.microseconds / 1000, 3); // Log only milliseconds
			*s++ = ' ';
		}
	}

	if (logThreadName) {
		SceKernelThreadInfo currentThreadInfo;
		currentThreadInfo.size = sizeof(currentThreadInfo);
		currentThreadInfo.name[0] = '\0';
		sceKernelReferThreadStatus(0, &currentThreadInfo);

		s = append(s, currentThreadInfo.name);
		*s++ = ' ';
		*s++ = '-';
		*s++ = ' ';
	}

	if (inOut < 0) {
		s = append(s, "IN  ");
	} else if (inOut > 0) {
		s = append(s, "OUT ");
	}

	if (logRa) {
		s = appendHex(s, ra, 0);
		*s++ = ' ';
	}

	s = append(s, syscallInfo->name);
	int types = syscallInfo->paramTypes;
	for (i = 0; i < syscallInfo->numParams; i++, types >>= 4) {
		if (i > 0) {
			*s++ = ',';
		}
		*s++ = ' ';
		int parameter = parameters[i];
		switch (types & 0xF) {
			case TYPE_HEX32:
				s = appendHex(s, parameter, 0);
				break;
			case TYPE_INT32:
				s = appendInt(s, parameter, 0);
				break;
			case TYPE_STRING:
				s = appendHex(s, parameter, 8);
				if (parameter != 0) {
					*s++ = '(';
					*s++ = '\'';
					s = append(s, (char *) parameter);
					*s++ = '\'';
					*s++ = ')';
				}
				break;
			case TYPE_POINTER32:
				s = appendHex(s, parameter, 8);
				if (parameter != 0) {
					*s++ = '(';
					s = appendHex(s, _lw(parameter), 0);
					*s++ = ')';
				}
				break;
			case TYPE_POINTER64:
				s = appendHex(s, parameter, 8);
				if (parameter != 0) {
					*s++ = '(';
					s = appendHex(s, _lw(parameter), 8);
					*s++ = ' ';
					s = appendHex(s, _lw(parameter + 4), 8);
					*s++ = ')';
				}
				break;
			case TYPE_VARSTRUCT:
				length = parameter != 0 ? _lw(parameter) : 0;
				s = syscallLogMem(buffer, s, parameter, length);
				break;
			case TYPE_FONT_INFO:
				s = syscallLogMem(buffer, s, parameter, 264);
				break;
			case TYPE_FONT_CHAR_INFO:
				s = syscallLogMem(buffer, s, parameter, 60);
				break;
			case TYPE_MPEG_EP:
				s = syscallLogMem(buffer, s, parameter, 16);
				break;
			case TYPE_MPEG_AU:
				s = syscallLogMem(buffer, s, parameter, 24);
				break;
			case TYPE_MP4_TRACK:
				s = syscallLogMem(buffer, s, parameter, 384);
				break;
			case TYPE_SOCK_ADDR_INTERNET:
				// the address is not 32-bit aligned
				s = appendHex(s, parameter, 8);
				if (parameter != 0) {
					*s++ = '(';
					s = append(s, "len=");
					s = appendInt(s, _lb(parameter), 0);
					s = append(s, ", family=");
					s = appendInt(s, _lb(parameter + 1), 0);
					s = append(s, ", port=");
					s = appendInt(s, (_lb(parameter + 2) << 8) | _lb(parameter + 3), 0);
					s = append(s, ", addr=");
					s = appendInt(s, _lb(parameter + 4), 0);
					s = append(s, ".");
					s = appendInt(s, _lb(parameter + 5), 0);
					s = append(s, ".");
					s = appendInt(s, _lb(parameter + 6), 0);
					s = append(s, ".");
					s = appendInt(s, _lb(parameter + 7), 0);
					*s++ = ')';
				}
#if 0
// sceNetInetConnect on port 80 ?
if (syscallInfo->nid == 0x410B34AA && _lb(parameter + 2) == 0 && _lb(parameter + 3) == 80) {
	_sb(192, parameter + 4);
	_sb(168, parameter + 5);
	_sb(  1, parameter + 6);
	_sb(  3, parameter + 7);
}
#endif
				break;
			case TYPE_BUFFER_AND_LENGTH:
				if (i + 1 < syscallInfo->numParams) {
					s = syscallLogMem(buffer, s, parameter, parameters[i + 1]);
				}
				break;
			case TYPE_VIDEOCODEC:
				s = appendHex(s, parameter, 8);
				videocodec = parameter;
				if (i + 1 < syscallInfo->numParams) {
					videocodecType = parameters[i + 1];
				}
				break;
		}
	}

	// Do not log the result at "IN" when logging before and after the syscall
	if (inOut >= 0) {
		*s++ = ' ';
		*s++ = '=';
		*s++ = ' ';
		s = appendHex(s, (int) result, 0);
	}

	#if DEBUG_MUTEX
	s = mutexLog(s, syscallInfo, parameters, result);
	#endif

	s = flushBuffer(buffer, s);

#if DUMP_sceMpegBaseCscAvc_CALLS
	if (syscallInfo->nid == NID_sceMpegBaseCscAvc) {
		char *fileName = buffer;
		s = append(buffer, "ms0:/tmp/sceMpegBaseCscAvc.");
		s = appendInt(s, sceMpegBaseCscAvcCall, 0);
		SceUID fd = ioOpen(fileName, PSP_O_WRONLY | PSP_O_CREAT, 0777);
		if (fd >= 0) {
			int bufferWidth = parameters[2];
			int height = _lw(parameters[3] + 0);
			int size = (bufferWidth * (height << 4)) << 2;
			int addr = parameters[0] | 0x40000000; // Uncached video memory
			if ((addr & 0xFFFFF000) == 0x44110000) {
				addr &= 0xFFFFF000;
			}
			while (size > 0) {
				int length = commonInfo->maxLogBufferLength;
				if (length > size) {
					length = size;
				}
				memcpy(commonInfo->logBuffer, (void *) addr, length);
				length = ioWrite(fd, commonInfo->logBuffer, length);
				if (length <= 0) {
					break;
				}
				size -= length;
				addr += length;
			}
			ioClose(fd);
		}
		s = flushBuffer(buffer, s);
		sceMpegBaseCscAvcCall++;
	}
#endif

#if DUMP_sceMpegBaseYCrCbCopy_CALLS
	if (syscallInfo->nid == NID_sceMpegBaseYCrCbCopy) {
		char *fileName = buffer;
		s = append(buffer, "ms0:/tmp/sceMpegBaseYCrCbCopy.");
		s = appendInt(s, sceMpegBaseYCrCbCopyCall, 0);
		SceUID fd = ioOpen(fileName, PSP_O_WRONLY | PSP_O_CREAT, 0777);
		if (fd >= 0) {
			int flags = parameters[2];
			int h = _lw(parameters[1] + 0);
			int w = _lw(parameters[1] + 4);
			int size1 = ((w + 16) >> 5) * (h >> 1) * 16;
			int size2 = (w >> 5) * (h >> 1) * 16;
			ioWrite(fd, &flags, 4);
			ioWrite(fd, &h, 4);
			ioWrite(fd, &w, 4);
			ioWrite(fd, &size1, 4);
			ioWrite(fd, &size2, 4);
			if (flags & 1) {
				ioWrite(fd, (void *) _lw(parameters[0] + 16), size1);
				ioWrite(fd, (void *) _lw(parameters[0] + 20), size2);
				ioWrite(fd, (void *) _lw(parameters[0] + 32), size1 >> 1);
				ioWrite(fd, (void *) _lw(parameters[0] + 36), size2 >> 1);
			}
			if (flags & 2) {
				ioWrite(fd, (void *) _lw(parameters[0] + 24), size1);
				ioWrite(fd, (void *) _lw(parameters[0] + 28), size2);
				ioWrite(fd, (void *) _lw(parameters[0] + 40), size1 >> 1);
				ioWrite(fd, (void *) _lw(parameters[0] + 44), size2 >> 1);
			}
			ioClose(fd);
		}
		s = flushBuffer(buffer, s);
		sceMpegBaseYCrCbCopyCall++;
	}
#endif

	if (videocodec != 0) {
		s = syscallLogMem(buffer, s, videocodec, 96);
		s = flushBuffer(buffer, s);

		int buffer2 = _lw(videocodec + 16);
		if (buffer2 != 0) {
			s = append(s, "Buffer2 Offset 16 ");
			s = syscallLogMem(buffer, s, buffer2, 40);
			s = flushBuffer(buffer, s);
		}

		if (videocodecType == 0) {
			int yuv = _lw(videocodec + 44);
			if (yuv != 0) {
				s = append(s, "Yuv Offset 44 ");
				s = syscallLogMem(buffer, s, yuv, 44);
				s = flushBuffer(buffer, s);

#if DUMP_VIDEOCODEC_FRAMES
				// Has video frame?
				if (syscallInfo->nid == NID_sceVideocodecDecode && _lw(yuv + 32)) {
					char *fileName = buffer;
					s = append(buffer, "ms0:/tmp/videocodecFrame.");
					s = appendInt(s, videocodecFrame, 0);
					SceUID fd = ioOpen(fileName, PSP_O_WRONLY | PSP_O_CREAT, 0777);
					if (fd >= 0) {
						int size = _lw(yuv + 28) + (_lw(yuv + 28) - _lw(yuv + 20)) - _lw(yuv + 0);
						ioWrite(fd, (void *) (_lw(yuv + 0) | 0x80000000), size);
						ioClose(fd);
					}
					s = buffer;
					videocodecFrame++;
				}
#endif

				int buffer1 = _lw(yuv + 36);
				if (buffer1 != 0) {
					s = append(s, "Yuv.buffer1 Offset 36 ");
					s = syscallLogMem(buffer, s, buffer1, 36);
					s = flushBuffer(buffer, s);
				}

				int buffer2 = _lw(yuv + 40);
				if (buffer2 != 0) {
					s = append(s, "Yuv.buffer2 Offset 40 ");
					s = syscallLogMem(buffer, s, buffer2, 32);
					s = flushBuffer(buffer, s);
				}
			}

			int buffer3 = _lw(videocodec + 48);
			if (buffer3 != 0) {
				s = append(s, "Buffer3 Offset 48 ");
				s = syscallLogMem(buffer, s, buffer3, 40);
				s = flushBuffer(buffer, s);
			}

			int decodeSEI = _lw(videocodec + 80);
			if (decodeSEI != 0) {
				s = append(s, "DecodeSEI Offset 80 ");
				s = syscallLogMem(buffer, s, decodeSEI, 36);
				s = flushBuffer(buffer, s);
			}
		}
	}

	#if DEBUG_UTILITY_SAVEDATA
	utilitySavedataLog(buffer, syscallInfo, parameters[0]);
	#endif
	#if DEBUG_UTILITY_OSK
	utilityOskLog(buffer, syscallInfo, parameters[0]);
	#endif
	#if DEBUG_UTILITY_MSG
	utilityMsgLog(buffer, syscallInfo, parameters[0]);
	#endif
}
