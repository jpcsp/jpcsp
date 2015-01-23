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
#include "common.h"

PSP_MODULE_INFO("JpcspTrace", PSP_MODULE_KERNEL, 1, 0);

#define MAKE_CALL(f) (0x0C000000 | (((u32)(f) >> 2) & 0x03ffffff))
#define MAKE_JUMP(f) (0x08000000 | (((u32)(f) >> 2) & 0x03ffffff))
#define MAKE_SYSCALL(n) ((((n) & 0xFFFFF) << 6) | 0x0C)
#define NOP 0

#define SYSCALL_PLUGIN_NID	0xADB83469

// ASM Redirect Patch
#define REDIRECT_FUNCTION(new_func, original) \
	do { \
		_sw(MAKE_JUMP((u32)new_func), ((u32)original)); \
		_sw(NOP, ((u32)original)+4); \
	} while ( 0 )

// Allocator Functions
int (* allocFunc)(u32, char *, u32, u32, u32);
void * (* getHeadFunc)(u32);
STMOD_HANDLER nextStartModuleHandler = NULL;
int syscallPluginUser;
int callSyscallPluginOffset;
int (* originalIoOpen)(const char *s, int flags, int permissions);
int (* originalIoWrite)(SceUID id, const void *data, int size);
int (* originalIoClose)(SceUID id);


SyscallInfo *moduleSyscalls = NULL;

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

int changeSyscallAddr(void *addr, void *newaddr) {
	void *ptr;
	u32 *syscalls;
	int i;
	u32 _addr = (u32)addr;
	int found = 0;

	// Retrieve the syscall arrays from the cop0
	asm("cfc0 %0, $12\n" : "=r"(ptr));

	if (ptr == NULL) {
		return 0;
	}

	syscalls = (u32*) (ptr + 0x10);

	for (i = 0; i < 0xFF4; ++i) {
		if ((syscalls[i] & 0x3FFFFFFF) == (_addr & 0x3FFFFFFF)) {
			printLogHH("Patching syscall from ", syscalls[i], " to ", (int) newaddr, "\n");
			syscalls[i] = (u32)newaddr | (syscalls[i] & 0xC0000000);
			found = 1;
		}
	}

	sceKernelDcacheWritebackAll();
	sceKernelIcacheClearAll();

	return found;
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

u32 parseParamTypes(const char *s, u32 *pflags) {
	u32 paramTypes = 0;
	int paramType;
	int i;

	*pflags = FLAG_LOG_AFTER_CALL;

	for (i = 0; *s != '\0' && i < 32;) {
		char c = *s++;
		if (c == '!') {
			*pflags |= FLAG_LOG_BEFORE_CALL;
		} else {
			paramType = TYPE_HEX32;
			switch (c) {
				case 'x': paramType = TYPE_HEX32; break;
				case 'd': paramType = TYPE_INT32; break;
				case 's': paramType = TYPE_STRING; break;
				case 'p': paramType = TYPE_POINTER32; break;
				case 'P': paramType = TYPE_POINTER64; break;
				case 'v': paramType = TYPE_VARSTRUCT; break;
				case 'F': paramType = TYPE_FONT_INFO; break;
				case 'f': paramType = TYPE_FONT_CHAR_INFO; break;
				case 'e': paramType = TYPE_MPEG_EP; break;
				case 'a': paramType = TYPE_MPEG_AU; break;
				case 't': paramType = TYPE_MP4_TRACK; break;
			}
			paramTypes |= paramType << i;
			i += 4;
		}
	}

	return paramTypes;
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

u64 syscallPlugin(u32 a0, u32 a1, u32 a2, u32 a3, u32 t0, u32 t1, u32 t2, u32 t3, SyscallInfo *syscallInfo, u32 ra, u32 sp, u32 gp) {
	u32 parameters[8];
	int k1;
	u64 result;

	parameters[0] = a0;
	parameters[1] = a1;
	parameters[2] = a2;
	parameters[3] = a3;
	parameters[4] = t0;
	parameters[5] = t1;
	parameters[6] = t2;
	parameters[7] = t3;

	if (syscallInfo->nid == 0x109F50BC) {
		commonInfo->inWriteLog++;
	}

	ioOpen = originalIoOpen;
	ioWrite = originalIoWrite;
	ioClose = originalIoClose;

	#if DEBUG_MUTEX
	mutexPreLog(syscallInfo, parameters);
	#endif

	if (syscallInfo->flags & FLAG_LOG_BEFORE_CALL) {
		commonInfo->inWriteLog++;
		k1 = pspSdkSetK1(0);
		syscallLog(syscallInfo, parameters, 0, ra, sp, gp);
		pspSdkSetK1(k1);
		commonInfo->inWriteLog--;
	}

	// Remark: the stackUsage doesn't make sense here for syscalls, only for user libraries
	result = syscallInfo->originalEntry(a0, a1, a2, a3, t0, t1, t2, t3);

	if (syscallInfo->flags & FLAG_LOG_AFTER_CALL) {
		k1 = pspSdkSetK1(0);
		syscallLog(syscallInfo, parameters, result, ra, sp, gp);
		pspSdkSetK1(k1);
	}

	ioOpen = userIoOpen;
	ioWrite = userIoWrite;
	ioClose = userIoClose;

	if (syscallInfo->nid == 0x109F50BC) {
		commonInfo->inWriteLog--;
	}

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

void patchSyscall(char *module, char *library, const char *name, u32 nid, int numParams, u32 paramTypes, u32 flags) {
	int asmBlocks = 11;

	// Allocate memory for the patch code and SyscallInfo
	int memSize = asmBlocks * 4 + sizeof(SyscallInfo) + strlen(name) + 1;
	uint32_t *asmblock = alloc(memSize);
	if (asmblock == NULL) {
		return;
	}

	// Link to Syscall
	SyscallInfo *syscallInfo = (SyscallInfo *) (asmblock + asmBlocks);
	char *nameCopy = (char *) (syscallInfo + 1);
	append(nameCopy, name);

	// Prepare loading of syscallInfo address using lui/ori
	int syscallInfoAddr = (int) syscallInfo;
	int syscallInfoAddrHi = (syscallInfoAddr >> 16) & 0xFFFF;
	int syscallInfoAddrLo = syscallInfoAddr & 0xFFFF;

	int i = 0;
	asmblock[i++] = 0x27BDFFF0; // addiu $sp, $sp, -16
	asmblock[i++] = 0xAFBF0004; // sw $ra, 4($sp)
	asmblock[i++] = 0xAFBD0008; // sw $sp, 8($sp)
	asmblock[i++] = 0xAFBC000C; // sw $gp, 12($sp)
	asmblock[i++] = 0x3C0C0000 | syscallInfoAddrHi; // lui $t4, syscallInfoAddr
	asmblock[i++] = 0x358C0000 | syscallInfoAddrLo; // ori $t4, $t4, syscallInfoAddr
	callSyscallPluginOffset = i * 4;
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
	syscallInfo->flags = flags;
	syscallInfo->paramTypes = paramTypes;
	syscallInfo->name = nameCopy;
	syscallInfo->next = NULL;
	syscallInfo->newEntry = (void *) asmblock;
	syscallInfo->commonInfo = commonInfo;

	if (!changeSyscallAddr(syscallInfo->originalEntry, asmblock)) {
		// This function has to be patched when starting a new module
		syscallInfo->next = moduleSyscalls;
		moduleSyscalls = syscallInfo;
	}

	sceKernelDcacheWritebackAll();
	sceKernelIcacheClearAll();

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
		if (c == '\r') {
			continue;
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
		char *hexNumParams = nextWord(&line);
		char *strParamTypes = nextWord(&line);

		u32 nid = parseHex(hexNid);
		u32 numParams = parseHex(hexNumParams);
		u32 flags = 0;
		u32 paramTypes = parseParamTypes(strParamTypes, &flags);

		if (strcmp(name, "LogBufferLength") == 0) {
			commonInfo->maxLogBufferLength = nid;
		} else {
			// If no numParams specified, take maximum number of params
			if (strlen(hexNumParams) == 0) {
				numParams = 8;
			}

			patchSyscall(NULL, NULL, name, nid, numParams, paramTypes, flags);
		}
	}

	sceIoClose(fd);
}

void patchModule(SceModule *module) {
	SyscallInfo *syscallInfo;
	int i, j;

	if (module != NULL && module->modname != NULL) {
		char *name = module->modname;
		if ((name[0] == 'S' || name[0] == 's') && name[1] == 'c' && name[2] == 'e') {
			// Do not patch PSP modules ("Sce" or "sce")
//			return;
		}
	}

	for (syscallInfo = moduleSyscalls; syscallInfo != NULL; syscallInfo = syscallInfo->next) {
		if (syscallInfo->originalEntry != NULL) {
			PspModuleImport *entry;
			for (i = 0; i < module->stub_size; i += entry->entLen * 4) {
				entry = module->stub_top + i;
				if (entry->name != NULL) {
					for (j = 0; j < entry->funcCount; j++) {
						if (entry->fnids[j] == syscallInfo->nid) {
							printLogSS("Patching module ", module->modname, ": ", syscallInfo->name, "\n");
							#if DEBUG
							printLogHH("Patching from ", (int) syscallInfo->originalEntry, " to ", (int) syscallInfo->newEntry, "\n");
							#endif
							void *addr = &entry->funcs[j * 2];

							#if DEBUG
							printLogMem("Before patch ", (int) addr, 8);
							#endif
							REDIRECT_FUNCTION(syscallInfo->newEntry, addr);
							#if DEBUG
							printLogMem("After patch ", (int) addr, 8);
							#endif

							sceKernelDcacheWritebackInvalidateRange(addr, 8);
							sceKernelIcacheInvalidateRange(addr, 8);
						}
					}
				}
			}
		}
	}
}

int startModuleHandler(SceModule2 *startingModule) {
	SyscallInfo *syscallInfo;
	int i;
	int id[100];
	int idcount = 0;

	#if DEBUG
	commonInfo->logKeepOpen = 1;
	openLogFile();
	printLogS("Starting module ", startingModule->modname, "\n");
	#endif

	// Do not patch myself...
	if (strcmp(startingModule->modname, "JpcspTraceUser") != 0) {
		// Check if the starting module is providing still missing NIDs
		int syscallInfoUpdated = 0;
		SyscallInfo **pLastSyscallInfo = &moduleSyscalls;
		for (syscallInfo = moduleSyscalls; syscallInfo != NULL; syscallInfo = syscallInfo->next) {
			if (syscallInfo->originalEntry == NULL) {
				syscallInfo->originalEntry = getEntryByModule((SceModule *) startingModule, syscallInfo->nid);
				if (syscallInfo->originalEntry != NULL) {
					sceKernelDcacheWritebackInvalidateRange(&(syscallInfo->originalEntry), 4);
					sceKernelIcacheInvalidateRange(&(syscallInfo->originalEntry), 4);

					if (changeSyscallAddr(syscallInfo->originalEntry, syscallInfo->newEntry)) {
						// This syscallInfo is now implemented by a syscall, unlink the entry.
						*pLastSyscallInfo = syscallInfo->next;
					} else {
						if (syscallPluginUser != 0) {
							int addr = ((int) syscallInfo->newEntry) + callSyscallPluginOffset;
							#if DEBUG
							printLogH("Changing syscallPluginUser call at ", addr, "\n");
							#endif
							_sw(MAKE_CALL(syscallPluginUser), addr);
							sceKernelDcacheWritebackInvalidateRange((void *) addr, 4);
							sceKernelIcacheInvalidateRange((void *) addr, 4);
						}

						// Some NID has been resolved by the starting module
						syscallInfoUpdated = 1;
					}

					#if DEBUG
					printLogMem("Updated SyscallInfo ", (int) syscallInfo->newEntry, 36 + sizeof(SyscallInfo));
					#endif
				}
			}

			if (*pLastSyscallInfo == syscallInfo) {
				pLastSyscallInfo = &syscallInfo->next;
			}

			#if DEBUG
			printLogH("moduleSyscalls ", (int) moduleSyscalls, "\n");
			#endif
		}

		if (syscallInfoUpdated) {
			// At least one pending SyscallInfo has been updated,
			// patch all the modules.
			sceKernelGetModuleIdList(id, sizeof(id), &idcount);
			for (i = 0; i < idcount; i++) {
				SceModule *module = sceKernelFindModuleByUID(id[i]);
				patchModule(module);
			}
		} else {
			// No SyscallInfo has been updated,
			// we just need to patch the starting module.
			patchModule((SceModule *) startingModule);
		}
	}

	#if DEBUG
	commonInfo->logKeepOpen = 0;
	closeLogFile();
	#endif

	if (nextStartModuleHandler == NULL) {
		return 0;
	}

	return nextStartModuleHandler(startingModule);
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

int loadUserModule(SceSize args, void * argp) {
	int userModuleId = -1;

	// Load the user module JpcspTraceUser.prx.
	// Retry to load it several times if the module manager is currently busy.
	while (1) {
		if (userModuleId < 0) {
			// Load the user module in high memory
			SceKernelLMOption loadModuleOptions;
			memset(&loadModuleOptions, 0, sizeof(loadModuleOptions));
			loadModuleOptions.size = sizeof(loadModuleOptions);
			loadModuleOptions.mpidtext = PSP_MEMORY_PARTITION_USER;
			loadModuleOptions.position = PSP_SMEM_High;

			userModuleId = sceKernelLoadModule("ms0:/seplugins/JpcspTraceUser.prx", 0, &loadModuleOptions);
			#if DEBUG
			printLogH("JpcspTraceUser moduleId ", userModuleId, "\n");
			#endif
		}
		int result = sceKernelStartModule(userModuleId, 0, NULL, NULL, NULL);
		#if DEBUG
		printLogH("JpcspTraceUser module start ", result, "\n");
		#endif

		if (userModuleId >= 0 && result >= 0) {
			break;
		}

		sceKernelDelayThread(1000);
	}

	SceModule *module = sceKernelFindModuleByUID(userModuleId);
	struct SceLibraryEntryTable *entry;
	int i;
	int j;

	for (i = 0; i < module->ent_size; i += entry->len * 4) {
		entry = (struct SceLibraryEntryTable *) (module->ent_top + i);
		int numEntries = entry->stubcount + entry->vstubcount;
		u32 *entries = (u32 *) entry->entrytable;
		for (j = 0; j < entry->stubcount; j++) {
			if (entries[j] == SYSCALL_PLUGIN_NID) {
				syscallPluginUser = entries[numEntries + j];
				#if DEBUG
				printLogH("Found syscallPluginUser at ", syscallPluginUser, "\n");
				#endif
				break;
			}
		}
	}

	sceKernelExitDeleteThread(0);

	return 0;
}

// Module Start
int module_start(SceSize args, void * argp) {
	// Find Allocator Functions in Memory
	allocFunc = (void *) sctrlHENFindFunction("sceSystemMemoryManager", "SysMemUserForUser", 0x237DBD4F);
	getHeadFunc = (void *) sctrlHENFindFunction("sceSystemMemoryManager", "SysMemUserForUser", 0x9D9A5BA1);
	originalIoOpen = (void *) sctrlHENFindFunction("sceIOFileManager", "IoFileMgrForUser", 0x109F50BC);
	originalIoWrite = (void *) sctrlHENFindFunction("sceIOFileManager", "IoFileMgrForUser", 0x42EC03AC);
	originalIoClose = (void *) sctrlHENFindFunction("sceIOFileManager", "IoFileMgrForUser", 0x810C4BC3);

	syscallPluginUser = 0;
	callSyscallPluginOffset = -1;
	commonInfo = NULL;
	commonInfo = alloc(sizeof(CommonInfo));
	commonInfo->logFd = -1;
	commonInfo->logKeepOpen = 0;
	commonInfo->logBufferLength = 0;
	commonInfo->logBuffer = NULL;
	commonInfo->maxLogBufferLength = DEFAULT_LOG_BUFFER_SIZE;
	commonInfo->freeAddr = NULL;
	commonInfo->freeSize = 0;
	commonInfo->inWriteLog = 0;

	sceIoRemove("ms0:/log.txt");

	commonInfo->logKeepOpen = 1;
	openLogFile();

	printLog("JpcspTrace - module_start\n");

	if (sceKernelInitKeyConfig() != PSP_INIT_KEYCONFIG_GAME) {
		return 1;
	}

	#if DEBUG >= 2
	printAllModules();
	printAllSyscalls();
	#endif

	patchSyscalls("ms0:/seplugins/JpcspTrace.config");

	// Load only JpcspTraceUser.prx if it is required
	if (moduleSyscalls != NULL) {
		// Load JpcspTraceUser.prx in a separate thread as at this point,
		// the module mgr is busy (sceKernelLoadModule would return ERROR_KERNEL_MODULE_MANAGER_BUSY).
		int loadUserModuleThread = sceKernelCreateThread("LoadUserModule", loadUserModule, 0x10, 4096, 0, NULL);
		sceKernelStartThread(loadUserModuleThread, 0, NULL);
	}

	nextStartModuleHandler = sctrlHENSetStartModuleHandler(startModuleHandler);

	commonInfo->logKeepOpen = 0;
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
