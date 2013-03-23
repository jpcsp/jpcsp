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
#include <psprtc.h>
#include <string.h>
#include "systemctrl.h"

#define DEBUG			0
#define DEBUG_MUTEX		0
#define LOG_BUFFER_SIZE	1024

PSP_MODULE_INFO("JpcspTrace", PSP_MODULE_KERNEL, 1, 0);

#define USER_PARTITION_ID	2

#define MAKE_CALL(f) (0x0C000000 | (((u32)(f) >> 2) & 0x03ffffff))
#define MAKE_JUMP(f) (0x08000000 | (((u32)(f) >> 2) & 0x03ffffff))
#define MAKE_SYSCALL(n) ((n<<6)|12)
#define NOP 0

// ASM Redirect Patch
#define REDIRECT_FUNCTION(new_func, original) \
	do { \
		_sw(MAKE_JUMP((u32)new_func), ((u32)original)); \
		_sw(NOP, ((u32)original)+4); \
	} while ( 0 )


SceUID logFd = -1;
int logKeepOpen = 0;
char *hexDigits = "0123456789ABCDEF";
int logTimestamp = 1;
int logThreadName = 1;
char *logBuffer;
int logBufferLength;
// Allocator Functions
int (* alloc)(u32, char *, u32, u32, u32);
void * (* gethead)(u32);


typedef struct {
	u64 (*originalEntry)(u32, u32, u32, u32, u32, u32, u32, u32);
	u32 nid;
	int numParams;
	char *name;
} SyscallInfo;

typedef struct {
        const char *name;
        unsigned short version;
        unsigned short attribute;
        unsigned char entLen;
        unsigned char varCount;
        unsigned short funcCount;
        unsigned int *fnids;
        unsigned int *funcs;
        unsigned int *vnids;
        unsigned int *vars;
} PspModuleImport;

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

char *append(char *dst, const char *src) {
	while ((*dst = *src) != '\0') {
		src++;
		dst++;
	}

	return dst;
}

char *appendHex(char *dst, u32 hex) {
	int i;
	*dst++ = '0';
	*dst++ = 'x';
	for (i = 28; i >= 0; i -= 4) {
		*dst++ = hexDigits[(hex >> i) & 0xF];
	}
	*dst = '\0';

	return dst;
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
	if (logFd < 0) {
		logFd = sceIoOpen("ms0:/log.txt", PSP_O_WRONLY | PSP_O_CREAT | PSP_O_APPEND, 0777);
	}
}

void closeLogFile() {
	sceIoClose(logFd);
	logFd = -1;
}

void writeLog(const char *s, int length) {
	if (!logKeepOpen) {
		openLogFile();
	}

	if (logBufferLength > 0) {
		// Try to write pending output.
		// This will succeed as soon as the interrupts are enabled again.
		if (sceIoWrite(logFd, logBuffer, logBufferLength) > 0) {
			logBufferLength = 0;
		}
	}

	if (sceIoWrite(logFd, s, length) < 0) {
		// Can't write to the log file right now, probably because the interrupts are disabled.
		// Save the log string for later output.

		// Allocate a buffer if not yet allocated
		if (logBuffer == NULL) {
			int result = alloc(USER_PARTITION_ID, "LogBuffer", PSP_SMEM_High, LOG_BUFFER_SIZE, 0);
			if (result >= 0) {
				logBuffer = gethead(result);
			}
		}
		if (logBuffer != NULL) {
			int restLength = LOG_BUFFER_SIZE - logBufferLength;
			if (length > restLength) {
				length = restLength;
			}
			memcpy(logBuffer + logBufferLength, s, length);
			logBufferLength += length;
		}
	}

	if (!logKeepOpen) {
		closeLogFile();
	}
}

void printLog(const char *s) {
	writeLog(s, strlen(s));
}

void printLogH(const char *s1, int hex, const char *s2) {
	char buffer[200];
	char *s = buffer;

	s = append(s, s1);
	s = appendHex(s, hex);
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
	s = appendHex(s, hex1);
	s = append(s, s2);
	s = appendHex(s, hex2);
	s = append(s, s3);
	writeLog(buffer, s - buffer);
}

void printLogSH(const char *s1, const char *s2, const char *s3, int hex, const char *s4) {
	char buffer[200];
	char *s = buffer;

	s = append(s, s1);
	s = append(s, s2);
	s = append(s, s3);
	s = appendHex(s, hex);
	s = append(s, s4);
	writeLog(buffer, s - buffer);
}

void printLogHS(const char *s1, int hex, const char *s2, const char *s3, const char *s4) {
	char buffer[200];
	char *s = buffer;

	s = append(s, s1);
	s = appendHex(s, hex);
	s = append(s, s2);
	s = append(s, s3);
	s = append(s, s4);
	writeLog(buffer, s - buffer);
}

void printLogMem(const char *s1, int addr, int length) {
	int i;
	char buffer[100];
	char *s = buffer;

	s = append(s, s1);
	s = appendHex(s, addr);
	s = append(s, ":\n");
	for (i = 0; i < length; i += 4) {
		if (i > 0) {
			if ((i % 16) == 0) {
				s = append(s, "\n");
				writeLog(buffer, s - buffer);
				s = buffer;
			} else {
				s = append(s, ", ");
			}
		}
		s = appendHex(s, _lw(addr + i));
	}
	s = append(s, "\n");
	writeLog(buffer, s - buffer);
}

void changeSyscallAddr(void *addr, void *newaddr) {
	void *ptr;
	u32 *syscalls;
	int i;
	u32 _addr = (u32)addr;

	// Retrieve the syscall arrays from the cop0
	asm("cfc0 %0, $12\n" : "=r"(ptr));

	if (ptr == NULL) {
		return;
	}

	syscalls = (u32*) (ptr + 0x10);

	for (i = 0; i < 0xFF4; ++i) {
		if ((syscalls[i] & 0x0FFFFFFF) == (_addr & 0x0FFFFFFF)) {
			printLogHH("Patching syscall from ", syscalls[i], " to ", (int) newaddr, "\n");
			syscalls[i] = (u32)newaddr;
		}
	}

	sceKernelDcacheWritebackAll();
	sceKernelIcacheClearAll();
}


int parseHexDigit(char hex) {
	if (hex >= '0' && hex <= '9') {
		return hex - '0';
	}
	if (hex >= 'A' && hex <= 'F') {
		return hex - 'A' + 10;
	}
	if (hex >= 'a' && hex <= 'f') {
		return hex - 'a' + 10;
	}

	// Invalid character
	return 0;
}

u32 parseHex(const char *s) {
	u32 hex = 0;

	// Skip leading "0x"
	if (s[0] == '0' && s[1] == 'x') {
		s += 2;
	}

	while (*s != '\0') {
		hex = (hex << 4) + parseHexDigit(*s++);
	}

	return hex;
}

#if DEBUG_MUTEX
char *mutexLog(char *s, const SyscallInfo *syscallInfo, const u32 *parameters, u64 result) {
	// Additional logging for sceKernelLockMutex, sceKernelUnlockMutex, sceKernelCreateMutex
	if (syscallInfo->nid == 0xB011B11F || syscallInfo->nid == 0x6B30100F || syscallInfo->nid == 0xB7D098C6) {
		if (referMutex == NULL) {
			referMutex = (void *)sctrlHENFindFunction("sceThreadManager", "ThreadManForKernel", 0xA9C2CB9A);
		}
		if (referMutex != NULL) {
			SceUID mutexId;
			// sceKernelCreateMutex
			if (syscallInfo->nid == 0xB7D098C6) {
				// The mutexId is the return value of sceKernelCreateMutex
				mutexId = (SceUID) result;
			} else {
				// Log the mutex values before the syscall
				s = append(s, ", before l=");
				s = appendInt(s, mutexInfo.lockedCount, 0);
				s = append(s, ", before t=");
				s = appendHex(s, mutexInfo.threadid);
				s = append(s, ", before w=");
				s = appendInt(s, mutexInfo.numWaitThreads, 0);
				// The mutexId is the first parameter of sceKernelLockMutex, ...
				mutexId = parameters[0];
			}

			SceKernelMutexInfo mutexInfo;
			mutexInfo.size = sizeof(mutexInfo);
			if (referMutex(mutexId, &mutexInfo) == 0) {
				s = append(s, ", l=");
				s = appendInt(s, mutexInfo.lockedCount, 0);
				s = append(s, ", t=");
				s = appendHex(s, mutexInfo.threadid);
				s = append(s, ", w=");
				s = appendInt(s, mutexInfo.numWaitThreads, 0);
			}
		}
	}

	return s;
}

void mutexPreLog(const SyscallInfo *syscallInfo, const u32 *parameters) {
	// Additional logging for sceKernelLockMutex, sceKernelUnlockMutex
	if (syscallInfo->nid == 0xB011B11F || syscallInfo->nid == 0x6B30100F) {
		int k1 = pspSdkSetK1(0);
		if (referMutex == NULL) {
			referMutex = (void *)sctrlHENFindFunction("sceThreadManager", "ThreadManForKernel", 0xA9C2CB9A);
		}
		if (referMutex != NULL) {
			SceUID mutexId = parameters[0];
			mutexInfo.size = sizeof(mutexInfo);
			referMutex(mutexId, &mutexInfo);
		}
		pspSdkSetK1(k1);
	}
}
#endif

void syscallLog(const SyscallInfo *syscallInfo, const u32 *parameters, u64 result) {
	char buffer[200];
	char *s = buffer;
	int i;

	if (logTimestamp) {
		pspTime time;
		if (sceRtcGetCurrentClockLocalTime(&time) == 0) {
			s = appendInt(s, time.hour, 2);
			*s++ = ':';
			s = appendInt(s, time.minutes, 2);
			*s++ = ':';
			s = appendInt(s, time.seconds, 2);
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

	s = append(s, syscallInfo->name);
	for (i = 0; i < syscallInfo->numParams; i++) {
		if (i > 0) {
			*s++ = ',';
		}
		*s++ = ' ';
		s = appendHex(s, parameters[i]);
	}
	*s++ = ' ';
	*s++ = '=';
	*s++ = ' ';
	s = appendHex(s, (int) result);

	#if DEBUG_MUTEX
	s = mutexLog(s, syscallInfo, parameters, result);
	#endif

	*s++ = '\n';
	writeLog(buffer, s - buffer);
}

u64 syscallPlugin(u32 a0, u32 a1, u32 a2, u32 a3, u32 t0, u32 t1, u32 t2, u32 t3, SyscallInfo *syscallInfo) {
	u32 parameters[8];
	int k1;

	parameters[0] = a0;
	parameters[1] = a1;
	parameters[2] = a2;
	parameters[3] = a3;
	parameters[4] = t0;
	parameters[5] = t1;
	parameters[6] = t2;
	parameters[7] = t3;

	#if DEBUG_MUTEX
	mutexPreLog(syscallInfo, parameters);
	#endif

	u64 result = syscallInfo->originalEntry(a0, a1, a2, a3, t0, t1, t2, t3);

	k1 = pspSdkSetK1(0);
	syscallLog(syscallInfo, parameters, result);
	pspSdkSetK1(k1);

	return result;
}

void *getEntryByModule(SceModule *module, int nid) {
	struct SceLibraryEntryTable *entry;
	int i;
	int j;

	for (i = 0; i < module->ent_size; i += entry->len * 4) {
		entry = (struct SceLibraryEntryTable *) (module->ent_top + i);
		if (entry->libname != NULL) {
			int numEntries = entry->stubcount + entry->vstubcount;
			u32 *entries = (u32 *) entry->entrytable;
			// Search only in the function stubs, not in the variable stubs.
			for (j = 0; j < entry->stubcount; j++) {
				if (entries[j] == nid) {
					return (void *) entries[numEntries + j];
				}
			}
		}
	}

	return NULL;
}

void *getEntryByNID(int nid) {
	int i;
	int id[100];
	int idcount = 0;

	int result = sceKernelGetModuleIdList(id, sizeof(id), &idcount);
	if (result != 0) {
		return NULL;
	}

	for (i = 0; i < idcount; i++) {
		SceModule *module = sceKernelFindModuleByUID(id[i]);
		void *entry = getEntryByModule(module, nid);
		if (entry != NULL) {
			return entry;
		}
	}

	return NULL;
}

void patchSyscall(char *module, char *library, const char *name, u32 nid, int numParams) {
	int asmBlocks = 9;
	int memSize = asmBlocks * 4 + sizeof(SyscallInfo) + strlen(name) + 1;

	// Allocate Memory
	int result = alloc(USER_PARTITION_ID, "SyscallStub", PSP_SMEM_High, memSize, 0);

	// Allocated Memory
	if (result < 0) {
		return;
	}

	// Get Memory Block
	uint32_t *asmblock = gethead(result);

	// Got Memory Block
	if (asmblock == NULL) {
		return;
	}

	// Link to Syscall
	SyscallInfo *syscallInfo = (SyscallInfo *) (asmblock + asmBlocks);
	int syscallInfoAddr = (int) syscallInfo;
	char *nameCopy = (char *) (syscallInfo + 1);
	append(nameCopy, name);

	int i = 0;
	asmblock[i++] = 0x27BDFFF0; // addiu $sp, $sp, -16
	asmblock[i++] = 0xAFBF0004; // sw $ra, 4($sp)
	asmblock[i++] = 0x3C0C0000 | ((syscallInfoAddr >> 16) & 0xFFFF); // lui $t4, syscallInfoAddr
	asmblock[i++] = 0x358C0000 | (syscallInfoAddr & 0xFFFF); // ori $t4, $t4, syscallInfoAddr
	asmblock[i++] = MAKE_CALL(syscallPlugin);
	asmblock[i++] = 0xAFAC0000; // sw $t4, 0($sp)
	asmblock[i++] = 0x8FBF0004; // lw $ra, 4($sp)
	asmblock[i++] = 0x03E00008; // jr $ra
	asmblock[i++] = 0x27BD0010; // addiu $sp, $sp, 16

	if (numParams > 8) {
		numParams = 8;
	}
	if (module == NULL || library == NULL) {
		syscallInfo->originalEntry = getEntryByNID(nid);
	} else {
		syscallInfo->originalEntry = (void *) sctrlHENFindFunction(module, library, nid);
	}
	syscallInfo->nid = nid;
	syscallInfo->numParams = numParams;
	syscallInfo->name = nameCopy;

	sceKernelDcacheWritebackAll();
	sceKernelIcacheClearAll();

	changeSyscallAddr(syscallInfo->originalEntry, asmblock);

	#if DEBUG
	printLogMem("Sycall stub ", (int) asmblock, memSize);
	#endif
}

int readChar(SceUID fd) {
	char c;
	int length = sceIoRead(fd, &c, 1);
	if (length < 1) {
		return -1;
	}

	return c & 0xFF;
}

int readLine(SceUID fd, char *line) {
	int c;
	char *start = line;

	while ((c = readChar(fd)) >= 0) {
		if (c == '\n') {
			break;
		}
		*line++ = (char) c;
	}
	if (c < 0 && start == line) {
		return -1;
	}
	*line = '\0';

	return line - start;
}

char *skipSpaces(char *s) {
	while (*s == ' ' || *s == '\t') {
		s++;
	}

	return s;
}

char *nextWord(char **ps) {
	char *s = *ps;

	s = skipSpaces(s);

	char *start = s;
	while (*s != ' ' && *s != '\t' && *s != '\0') {
		s++;
	}
	if (*s != '\0') {
		*s++ = '\0';
	}
	*ps = s;

	return start;
}

void patchSyscalls(char *filePath) {
	char lineBuffer[200];
	char *line;

	SceUID fd = sceIoOpen(filePath, PSP_O_RDONLY, 0);
	if (fd < 0) {
		printLogSH("sceIoOpen '", filePath, "' = ", fd, "\n");
		return;
	}

	printLog("Config file:\n");
	while (readLine(fd, lineBuffer) >= 0) {
		line = lineBuffer;

		printLogS("> ", line, "\n");

		// Skip leading spaces & tabs
		line = skipSpaces(line);

		// Comment or empty line
		if (*line == '#' || *line == '\0') {
			continue;
		}

		char *name = nextWord(&line);
		char *hexNid = nextWord(&line);
		u32 nid = parseHex(hexNid);
		char *hexNumParams = nextWord(&line);
		u32 numParams = parseHex(hexNumParams);
		// If no numParams specified, take maximum number of params
		if (strlen(hexNumParams) == 0) {
			numParams = 8;
		}

		patchSyscall(NULL, NULL, name, nid, numParams);
	}

	sceIoClose(fd);
}

#if DEBUG
void printAllSyscalls() {
	void *ptr;
	u32 *syscalls;
	int i;

	// get syscall struct from cop0
	asm("cfc0 %0, $12\n" : "=r"(ptr));

	if (ptr == NULL) {
		return;
	}

	syscalls = (u32*) (ptr + 0x10);

	for (i = 0; i < 0xFF4; ++i) {
		printLogHH("Syscall[", i, "] = ", syscalls[i], "\n");
	}
}

void printModuleEntries(SceModule *module) {
	struct SceLibraryEntryTable *entry;
	int i;
	int j;

	for (i = 0; i < module->ent_size; i += entry->len * 4) {
		entry = (struct SceLibraryEntryTable *) (module->ent_top + i);
		if (entry->libname != NULL) {
			printLogS("  Library name='", entry->libname, "'\n");

			int numEntries = entry->stubcount + entry->vstubcount;
			u32 *entries = (u32 *) entry->entrytable;
			for (j = 0; j < entry->stubcount; j++) {
				printLogHH("    Function NID ", entries[j], ", addr=", entries[numEntries + j], "\n");
			}
			for (j = entry->stubcount; j < numEntries; j++) {
				printLogHH("    Variable NID ", entries[j], ", addr=", entries[numEntries + j], "\n");
			}
		}
	}
}

void printAllModules() {
	int i;
	int id[100];
	int idcount = 0;

	int result = sceKernelGetModuleIdList(id, sizeof(id), &idcount);
	if (result != 0) {
		printLogH("sceKernelGetModuleIdList=", result, "\n");
		return;
	}

	for (i = 0; i < idcount; i++) {
		SceModule *module = sceKernelFindModuleByUID(id[i]);
		printLogHS("Module id=", id[i], ", name='", module->modname, "'\n");
		printModuleEntries(module);
	}
}
#endif

// Module Start
int module_start(SceSize args, void * argp) {
	// Find Allocator Functions in Memory
	alloc = (void *) sctrlHENFindFunction("sceSystemMemoryManager", "SysMemUserForUser", 0x237DBD4F);
	gethead = (void *) sctrlHENFindFunction("sceSystemMemoryManager", "SysMemUserForUser", 0x9D9A5BA1);

	logBufferLength = 0;
	logBuffer = NULL;

	logKeepOpen = 1;
	openLogFile();

	printLog("JpcspTrace - module_start\n");

	if (sceKernelInitKeyConfig() != PSP_INIT_KEYCONFIG_GAME) {
		return 1;
	}

	#if DEBUG
	printAllModules();
	printAllSyscalls();
	#endif

	patchSyscalls("ms0:/seplugins/JpcspTrace.config");

	logKeepOpen = 0;
	closeLogFile();

	return 0;
}

// Module Stop
int module_stop(SceSize args, void * argp) {
	openLogFile();

	printLog("JpcspTrace - module_stop\n");

	closeLogFile();

	return 0;
}
