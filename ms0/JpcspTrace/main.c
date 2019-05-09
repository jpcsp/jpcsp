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
#include <pspidstorage.h>
#include <string.h>
#include "systemctrl.h"
#include "common.h"
#include <pspnand_driver.h>
int sceNandReadExtraOnly(u32 ppn, void *spare, u32 len);
#if TEST_ATA
   void *sceAta_driver_D225B43E(int driveNumber);
   int sceAtaExecPacketCmd(void *driveStructure, int unknown1, int unknown2, int unknown3, int unknown4, int operationCode, void *unknown5);
   int sceAta_driver_C344D497(void *driveStructure, int unknown1, int unknown2, int length);
   int sceAtaSelectDevice(int unknown);
   int sceAtaScanDevice(void *driveStructure);
   int sceAtaAhbSetupBus();
   int sceAtaEnableClkIo(int unknown);
   int sceAtaWaitBusBusy1();
   void sceAta_driver_BE6261DA(int unknown);
   int sceAta_driver_4D225674();
#endif

PSP_MODULE_INFO("JpcspTrace", PSP_MODULE_KERNEL, 1, 0);

#define MAKE_CALL(f) (0x0C000000 | (((u32)(f) >> 2) & 0x03ffffff))
#define MAKE_JUMP(f) (0x08000000 | (((u32)(f) >> 2) & 0x03ffffff))
#define MAKE_SYSCALL(n) ((((n) & 0xFFFFF) << 6) | 0x0C)
#define NOP 0

#define LW(addr)   (*((volatile u32*) (addr)))
#define LW8(addr)  (*((volatile u8 *) (addr)))
#define LW16(addr) (*((volatile u16*) (addr)))
#define SW(value, addr)   (*((volatile u32*) (addr)) = (u32) (value))
#define SW8(value, addr)  (*((volatile u8 *) (addr)) = (u8 ) (value))
#define SW16(value, addr) (*((volatile u16*) (addr)) = (u16) (value))

#define DISABLE_INTR(intr)	asm("mfic %0, $0\n" : "=r"(intr)); asm("mtic $0, $0\n");
#define ENABLE_INTR(intr)	asm("mtic %0, $0\n" : : "r"(intr));

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
int sceWmd_driver_7A0E484C(void *, u32, u32 *);
int sceUtilsBufferCopyWithRange(void* outbuff, int outsize, void* inbuff, int insize, int cmd);
#if DUMP_PSAR
int sceUtilsBufferCopyByPollingWithRangeAddr = 0;
int sceUtilsBufferCopyWithRangeAddr = 0;
#endif

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

struct syscallTableHeader {
	// Pointer to the next syscall table
	struct syscallTableHeader *next;
	// Set at each power on to a random value
	u32 baseSyscallIndex;
	// Size in bytes of the syscalls table. Usually, 0x4000
	u32 tableSize;
	// Size in bytes of the total size (header and syscalls table). Usually, 0x4000
	u32 totalSize; // 0x4010
	// The syscalls table is following the header
	u32 syscalls[0];
};

int changeSyscallAddr(void *addr, void *newaddr) {
	int i;
	int found = 0;

	// Retrieve the syscall table from the cop0
	struct syscallTableHeader *header;
	asm("cfc0 %0, $12\n" : "=r"(header));

	if (header == NULL) {
		return found;
	}

	u32 _addr = ((u32) addr) & 0x3FFFFFFF;
	while (header != NULL && header->baseSyscallIndex != 0) {
		int numberSyscalls = header->tableSize >> 2;
		u32 *syscalls = header->syscalls;
		for (i = 0; i < numberSyscalls; i++, syscalls++) {
			if ((*syscalls & 0x3FFFFFFF) == _addr) {
				printLogHH("Patching syscall from ", *syscalls, " to ", (int) newaddr, "\n");
				*syscalls = ((u32) newaddr) | (*syscalls & 0xC0000000);
				found = 1;
			}
		}

		header = header->next;
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
		} else if (c == '$') {
			*pflags |= FLAG_LOG_FREEMEM;
		} else if (c == '>') {
			*pflags |= FLAG_LOG_STACK_USAGE;
		} else if (c == '%') {
			*pflags |= FLAG_LOG_FLUSH_AFTER_CALL;
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
				case 'I': paramType = TYPE_SOCK_ADDR_INTERNET; break;
				case 'B': paramType = TYPE_BUFFER_AND_LENGTH; break;
				case 'V': paramType = TYPE_VIDEOCODEC; break;
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

u64 syscallPlugin(u32 a0, u32 a1, u32 a2, u32 a3, u32 t0, u32 t1, u32 t2, u32 t3, SyscallInfo *syscallInfo, u32 ra, u32 sp, u32 gp, u32 dummy0, u32 dummy4, u32 dummy8, u32 realRa) {
	u32 parameters[8];
	int k1;
	u64 result;
	int inOut = 0;

	parameters[0] = a0;
	parameters[1] = a1;
	parameters[2] = a2;
	parameters[3] = a3;
	parameters[4] = t0;
	parameters[5] = t1;
	parameters[6] = t2;
	parameters[7] = t3;

	if (IS_sceIoOpen_NID(syscallInfo->nid)) {
		commonInfo->inWriteLog++;
	}

	ioOpen = originalIoOpen;
	ioWrite = originalIoWrite;
	ioClose = originalIoClose;

	#if DEBUG_MUTEX
	mutexPreLog(syscallInfo, parameters);
	#endif

	if (syscallInfo->flags & FLAG_LOG_BEFORE_CALL) {
		if (syscallInfo->flags & FLAG_LOG_AFTER_CALL) {
			// Display "IN" and "OUT" in front of syscall
			// when it is logged both before and after.
			inOut = 1;
		}

		commonInfo->inWriteLog++;
		k1 = pspSdkSetK1(0);
		syscallLog(syscallInfo, -inOut, parameters, 0, realRa, sp, gp);
		pspSdkSetK1(k1);
		commonInfo->inWriteLog--;

		if (syscallInfo->flags & FLAG_LOG_FLUSH_AFTER_CALL) {
			flushLogBuffer();
		}
	}

	// Remark: the stackUsage doesn't make sense here for syscalls, only for user libraries
	result = syscallInfo->originalEntry(a0, a1, a2, a3, t0, t1, t2, t3);

	if (syscallInfo->flags & FLAG_LOG_AFTER_CALL) {
		k1 = pspSdkSetK1(0);
		syscallLog(syscallInfo, inOut, parameters, result, realRa, sp, gp);
		pspSdkSetK1(k1);
	}

	if (IS_sceIoOpen_NID(syscallInfo->nid)) {
		commonInfo->inWriteLog--;
	}

	if (syscallInfo->flags & FLAG_LOG_FLUSH_AFTER_CALL) {
		flushLogBuffer();
	}

	ioOpen = userIoOpen;
	ioWrite = userIoWrite;
	ioClose = userIoClose;

	return result;
}

void *getEntryByModule(SceModule2 *module, int nid) {
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
		SceModule2 *module = (SceModule2 *) sceKernelFindModuleByUID(id[i]);
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

#if DUMP_PSAR
	if (nid == NID_sceUtilsBufferCopyByPollingWithRange && sceUtilsBufferCopyByPollingWithRangeAddr != 0) {
		printLogH("Patching 0x77E97079 before: ", LW(sceUtilsBufferCopyByPollingWithRangeAddr), "\n");
		SW(0x08000000 | ((((u32) asmblock) >> 2) & 0x03FFFFFF), sceUtilsBufferCopyByPollingWithRangeAddr);
		printLogH("Patching 0x77E97079 after: ", LW(sceUtilsBufferCopyByPollingWithRangeAddr), "\n");
		printLogH("Patching 0x77E97079 asmblock: ", (u32) asmblock, "\n");
	}
	if (nid == NID_sceUtilsBufferCopyWithRange && sceUtilsBufferCopyWithRangeAddr != 0) {
		printLogH("Patching 0x4C537C72 before: ", LW(sceUtilsBufferCopyWithRangeAddr), "\n");
		SW(0x08000000 | ((((u32) asmblock) >> 2) & 0x03FFFFFF), sceUtilsBufferCopyWithRangeAddr);
		printLogH("Patching 0x4C537C72 after: ", LW(sceUtilsBufferCopyWithRangeAddr), "\n");
		printLogH("Patching 0x4C537C72 asmblock: ", (u32) asmblock, "\n");
	}
#endif

	sceKernelDcacheWritebackAll();
	sceKernelIcacheClearAll();

	#if DEBUG
	printLogMem("Sycall stub ", (int) asmblock, memSize);
	#endif
}

void dumpMemory(u32 startAddress, u32 length, const char *fileName) {
	SceUID fd = ioOpen(fileName, PSP_O_WRONLY | PSP_O_CREAT, 0777);
	if (fd < 0) {
		printLog("dumpMemory - Cannot create file\n");
		return;
	}

	u32 buffer[256];
	u32 address = startAddress & 0xFFFFFFFC;
	while (length > 0) {
		u32 n = length;
		if (n > sizeof(buffer)) {
			n = sizeof(buffer);
		}

		u32 i;
		u32 n4 = n >> 2;
		for (i = 0; i < n4; i++, address += 4) {
			buffer[i] = _lw(address);
		}

		ioWrite(fd, buffer, n);

		length -= n;
	}

	ioClose(fd);
}

void decryptMeimg(char *fromFileName, char *toFileName) {
	SceUID me = ioOpen(fromFileName, PSP_O_RDONLY, 0777);
	if (me < 0) {
		printLog("Cannot open meimg.img file\n");
		return;
	}

	u32 bufferSize = 349200;
	u32 allocSize = bufferSize + 0x40;
	void *allocBuffer = alloc(allocSize);
	void *buffer = (void *) ((((u32) allocBuffer) + 0x3F) & ~0x3F);

	int result = sceIoRead(me, buffer, bufferSize);
	ioClose(me);
	if (result < 0) {
		printLog("Error reading meimg.img file\n");
		freeAlloc(allocBuffer, allocSize);
		return;
	}

	u32 newSize = 0;
	result = sceWmd_driver_7A0E484C(buffer, bufferSize, &newSize);
	if (result < 0) {
		printLogH("Error ", result, " decrypting meimg.img\n");
		freeAlloc(allocBuffer, allocSize);
		return;
	}

	sceIoRemove(toFileName);
	me = ioOpen(toFileName, PSP_O_WRONLY | PSP_O_CREAT, 0777);
	if (me < 0) {
		printLog("Cannot create decrypted meimg.img file\n");
		freeAlloc(allocBuffer, allocSize);
		return;
	}
	printLogH("meimg.img decrypted newSize=", newSize, "\n");
	ioWrite(me, buffer, newSize);
	ioClose(me);

	freeAlloc(allocBuffer, allocSize);
}

void executeKirkCommand(int cmd, char *outputFileName, int outputSize, char *inputFileName, int inputSize) {
	SceUID in = ioOpen(inputFileName, PSP_O_RDONLY, 0777);
	if (in < 0) {
		printLog("Cannot open input file\n");
		return;
	}

	u32 allocInputSize = inputSize + 0x40;
	void *allocInputBuffer = alloc(allocInputSize);
	memset(allocInputBuffer, 0, allocInputSize);
	void *inputBuffer = (void *) ((((u32) allocInputBuffer) + 0x3F) & ~0x3F);

	int result = sceIoRead(in, inputBuffer, inputSize);
	ioClose(in);
	if (result < 0) {
		printLog("Error reading input file\n");
		freeAlloc(allocInputBuffer, allocInputSize);
		return;
	}

	u32 allocOutputSize = outputSize + 0x40;
	void *allocOutputBuffer = alloc(allocOutputSize);
	memset(allocOutputBuffer, 0, allocOutputSize);
	void *outputBuffer = (void *) ((((u32) allocOutputBuffer) + 0x3F) & ~0x3F);

	u32 start = LW(0xBC600000);
	int kirkResult = sceUtilsBufferCopyWithRange(outputBuffer, outputSize, inputBuffer, inputSize, cmd);
	u32 end = LW(0xBC600000);
	if (kirkResult != 0) {
		printLogH("Error ", kirkResult, " received while executing kirk command\n");
		freeAlloc(allocOutputBuffer, allocOutputSize);
		freeAlloc(allocInputBuffer, allocInputSize);
		return;
	}
	printLogH("Kirk command executed in ", end - start, " us\n");

	sceIoRemove(outputFileName);
	SceUID out = ioOpen(outputFileName, PSP_O_WRONLY | PSP_O_CREAT, 0777);
	if (out < 0) {
		ioClose(in);
		printLog("Cannot open output file\n");
		return;
	}
	ioWrite(out, outputBuffer, outputSize);
	ioClose(out);

	freeAlloc(allocOutputBuffer, allocOutputSize);
	freeAlloc(allocInputBuffer, allocInputSize);
}

void executeIdStorageRead(int key, char *outputFileName) {
	char buffer[0x200];

	int result = sceIdStorageReadLeaf(key, buffer);
	if (result < 0) {
		printLogH("sceIdStorageReadLeaf returned ", result, "\n");
		return;
	}

	sceIoRemove(outputFileName);
	SceUID out = ioOpen(outputFileName, PSP_O_WRONLY | PSP_O_CREAT, 0777);
	if (out < 0) {
		printLog("Cannot open output file\n");
		return;
	}
	ioWrite(out, buffer, sizeof(buffer));
	ioClose(out);
}

void dumpNand() {
	SceUID fdFuseId = ioOpen("ms0:/nand.fuseid", PSP_O_WRONLY | PSP_O_CREAT, 0777);
	if (fdFuseId < 0) {
		printLog("dumpNand - Cannot create nand.fuseid file\n");
		return;
	}

	// sceSysregGetFuseId
	u32 fuseId0 = *((u32 *) (0xBC100090));
	u32 fuseId1 = *((u32 *) (0xBC100094));
	ioWrite(fdFuseId, &fuseId0, 4);
	ioWrite(fdFuseId, &fuseId1, 4);
	ioClose(fdFuseId);

	SceUID fdBlock = ioOpen("ms0:/nand.block", PSP_O_WRONLY | PSP_O_CREAT, 0777);
	if (fdBlock < 0) {
		printLog("dumpNand - Cannot create nand.block file\n");
		return;
	}

	SceUID fdSpare = ioOpen("ms0:/nand.spare", PSP_O_WRONLY | PSP_O_CREAT, 0777);
	if (fdSpare < 0) {
		printLog("dumpNand - Cannot create nand.spare file\n");
		return;
	}

	SceUID fdResult = ioOpen("ms0:/nand.result", PSP_O_WRONLY | PSP_O_CREAT, 0777);
	if (fdResult < 0) {
		printLog("dumpNand - Cannot create nand.result file\n");
		return;
	}

	int pageSize = sceNandGetPageSize();
	int pagesPerBlock = sceNandGetPagesPerBlock();
	int totalBlocks = sceNandGetTotalBlocks();
	printLogH("sceNandGetPageSize ", pageSize, "\n");
	printLogH("sceNandGetPagesPerBlock ", pagesPerBlock, "\n");
	printLogH("sceNandGetTotalBlocks ", totalBlocks, "\n");

	int blockBufferSize = pageSize * pagesPerBlock;
	void *blockBuffer = alloc(blockBufferSize);
	int spareBufferSize = 16 * pagesPerBlock;
	void *spareBuffer = alloc(spareBufferSize);

	int block;
	for (block = 0; block < totalBlocks; block++) {
		u32 ppn = block * pagesPerBlock;

		memset(blockBuffer, 0, blockBufferSize);
		memset(spareBuffer, 0, spareBufferSize);

		sceNandLock(0);
		int result = sceNandReadPages(ppn, blockBuffer, NULL, pagesPerBlock);
		sceNandReadExtraOnly(ppn, spareBuffer, pagesPerBlock);
		sceNandUnlock();

		ioWrite(fdBlock, blockBuffer, blockBufferSize);
		ioWrite(fdSpare, spareBuffer, spareBufferSize);
		ioWrite(fdResult, &result, 4);
	}

	ioClose(fdResult);
	ioClose(fdSpare);
	ioClose(fdBlock);
}

#if TEST_ATA
int ataWaitBusy(int addr) {
	u8 status;
	u32 start = LW(0xBC600000);
	do {
		status = LW8(addr);
		if ((status & 0x81) == 0x01) {
			return 0;
		}

		// Timeout after 0.1 second
		u32 now = LW(0xBC600000);
		if (now - start > 100000) {
			return 0;
		}
	} while (status & 0x80);

	return status & 0x01 ? 0 : 1;
}

int ataWaitBusy1() {
	return ataWaitBusy(0xBD70000E);
}

int ataWaitBusy2() {
	return ataWaitBusy(0xBD700007);
}

void wait(int us) {
	u32 start = LW(0xBC600000);
	while (us > 0) {
		u32 now = LW(0xBC600000);
		if (now - start > us) {
			break;
		}
	}
}

int ataWaitForCondition(int statusMask, int statusValue, int interruptReasonMask, int interruptReasonValue) {
	u32 start = LW(0xBC600000);
	while (1) {
		u8 status = LW8(0xBD70000E);
		u8 interruptReason = LW8(0xBD700002);
		if ((status & statusMask) == statusValue && (interruptReason & interruptReasonMask) == interruptReasonValue) {
			break;
		}

		// Timeout after 0.1 second
		u32 now = LW(0xBC600000);
		if (now - start > 100000) {
			return 0;
		}
	}

	return 1;
}

void testAtaReadCmd(int operationCode, int length, int lengthOffset, int data1, int dataOffset1, int data2, int dataOffset2) {
	int i;

	u16 bufferPacketCmd16[6];
	memset(bufferPacketCmd16, 0, sizeof(bufferPacketCmd16));
	u8 *bufferPacketCmd8 = (u8 *) bufferPacketCmd16;
	bufferPacketCmd8[0] = operationCode;
	if (lengthOffset > 0) {
		bufferPacketCmd8[lengthOffset] = length;
		if (length >= 0x100) {
			bufferPacketCmd8[lengthOffset - 1] = length >> 8;
		}
	}
	if (dataOffset1 > 0) {
		bufferPacketCmd8[dataOffset1] = data1;
		if (data1 >= 0x100) {
			bufferPacketCmd8[dataOffset1 - 1] = data1 >> 8;
		}
	}
	if (dataOffset2 > 0) {
		bufferPacketCmd8[dataOffset2] = data2;
		if (data2 >= 0x100) {
			bufferPacketCmd8[dataOffset2 - 1] = data2 >> 8;
		}
	}
	printLogMem("bufferPacketCmd ", (u32) bufferPacketCmd16, sizeof(bufferPacketCmd16));

	u16 bufferResult[128];
	memset(bufferResult, 0, sizeof(bufferResult));
	int resultLength = 0;

	printLogHH("Start: ", LW8(0xBD70000E), ", ", LW8(0xBD700002), "\n");

	u32 startResponse = 0;
	u32 endResponse = 0;
	u32 start = LW(0xBC600000);

	if (ataWaitBusy1() || (LW8(0xBD70000E) & 0x80) == 0x00) {
		// Wait for BSY=0, DRQ=0
		if (ataWaitForCondition(0x88, 0x00, 0, 0)) {
			SW8(0x0A, 0xBD70000E);
			SW8(0x00, 0xBD700001);
			SW8(0x00, 0xBD700002);
			SW8(0x00, 0xBD700003);
			SW8(0x00, 0xBD700004);
			SW8(0x40, 0xBD700005);
			SW8(0x00, 0xBD700006);
			SW8(0xA0, 0xBD700007);
			LW8(0xBD70000E);
			// Wait for CoD=1, IO=0 and BSY=0, DRQ=1
			if (ataWaitForCondition(0x88, 0x08, 0x03, 0x01)) {
				for (i = 0; i < 6; i++) {
					SW16(bufferPacketCmd16[i], 0xBD700000);
				}
				SW8(0x00, 0xBD700008);
				startResponse = LW(0xBC600000);
				endResponse = startResponse;
				ataWaitBusy1();
				if ((LW8(0xBD70000E) & 0x01) == 0x00) {
					if (LW8(0xBD70000E) & 0x08) {
						resultLength = LW8(0xBD700004) | (LW8(0xBD700005) << 8);
						int resultLength2 = (resultLength + 1) >> 1;
						for (i = 0; i < resultLength2; i++) {
							bufferResult[i] = LW16(0xBD700000);
						}
					} else {
						printLogHH("No result ", LW8(0xBD70000E), ", ", LW8(0xBD700002), "\n");
					}
					// Wait for BSY=0, DRQ=0
					if (ataWaitForCondition(0x88, 0x00, 0, 0)) {
						// Wait for CoD=1, IO=1 and DRDY=1, BSY=0, DRQ=0
						if (ataWaitForCondition(0xC8, 0x40, 0x03, 0x03)) {
							// OK
							endResponse = LW(0xBC600000);
						} else {
							printLogHH("Timeout on condition CoD=1, IO=1 and DRDY=1, BSY=0, DRQ=0: ", LW8(0xBD70000E), ", ", LW8(0xBD700002), "\n");
						}
					} else {
						printLogHH("Timeout on condition BSY=0, DRQ=0: ", LW8(0xBD70000E), ", ", LW8(0xBD700002), "\n");
					}
				} else {
					printLogHH("Error before reading result ", LW8(0xBD70000E), ", ", LW8(0xBD700002), "\n");
				}
			} else {
				printLogHH("Timeout on condition CoD=1, IO=0 and BSY=0, DRQ=1: ", LW8(0xBD70000E), ", ", LW8(0xBD700002), "\n");
			}
		} else {
			printLogHH("Timeout on condition BSY=0, DRQ=0: ", LW8(0xBD70000E), ", ", LW8(0xBD700002), "\n");
		}
	}

	u32 end = LW(0xBC600000);

	printLogHH("testAtaReadCmd end status ", LW8(0xBD70000E), " in ", end - start, " us\n");
	if (LW8(0xBD70000E) & 0x01) {
		printLogH("Error ", LW8(0xBD700001), "\n");
	}
	if (resultLength > 0) {
		printLogHH("Result length ", resultLength, " in ", endResponse - startResponse, "\n");
		printLogMem("bufferResult ", (u32) bufferResult, resultLength);
	}
}

void testAta() {
	sceKernelDelayThread(100000);

	sceAtaEnableClkIo(0x80);
	SW(0xFECC0000, 0xBD600044);
	sceAtaAhbSetupBus();
	ataWaitBusy1();
	sceAtaEnableClkIo(0x80);
	sceAtaAhbSetupBus();

	int intr;
	DISABLE_INTR(intr);

	// Perform soft reset
	SW8(0x04, 0xBD70000E);
	wait(100);
	SW8(0x00, 0xBD70000E);
	wait(10000);
	ataWaitBusy1();
	wait(100000);

	printLogHH("Start 1,2: ", LW8(0xBD700001), ", ", LW8(0xBD700002), "\n");
	printLogHH("Start 3,4: ", LW8(0xBD700003), ", ", LW8(0xBD700004), "\n");
	printLogHH("Start 5,6: ", LW8(0xBD700005), ", ", LW8(0xBD700006), "\n");
	printLogHH("Start 7,E: ", LW8(0xBD700007), ", ", LW8(0xBD70000E), "\n");

	printLog("Testing ATA_CMD_OP_TEST_UNIT_READY:\n");
	testAtaReadCmd(0x0, 0, -1, 0, -1, 0, -1);

	printLog("Testing ATA_CMD_OP_INQUIRY:\n");
	testAtaReadCmd(0x12, 0x60, 4, 0, -1, 0, -1);

	printLog("Testing ATA_CMD_OP_READ_STRUCTURE:\n");
	testAtaReadCmd(0xAD, 0x24, 9, 0, -1, 0, -1);

	printLog("Testing ATA_CMD_OP_REQUEST_SENSE:\n");
	testAtaReadCmd(0x3, 0x12, 4, 0, -1, 0, -1);

	printLog("Testing ATA_CMD_OP_MODE_SENSE_BIG:\n");
	testAtaReadCmd(0x5A, 0x8, 8, 0x81A, 2, 0, -1);

	printLog("Testing ATA_CMD_OP_MODE_SENSE_BIG:\n");
	testAtaReadCmd(0x5A, 0x1C, 8, 0x81A, 2, 0, -1);

	printLog("Testing ATA_CMD_OP_READ_BIG:\n");
	testAtaReadCmd(0x28, 0x10, 5, 0x1, 8, 0xE0, 9);

	printLog("Testing ATA_CMD_OP_UNKNOWN_F0:\n");
	testAtaReadCmd(0xF0, 0x1, 1, 0, -1, 0, -1);

	printLog("Testing ATA_CMD_OP_UNKNOWN_F1:\n");
	testAtaReadCmd(0xF1, 0x8, 7, 0x10, 2, 0, -1);

	printLog("Testing ATA_CMD_OP_UNKNOWN_F7:\n");
	testAtaReadCmd(0xF7, 0x40, 2, 0, -1, 0, -1);

	printLog("Testing ATA_CMD_OP_UNKNOWN_FC:\n");
	testAtaReadCmd(0xFC, 0x100, 8, 0, -1, 0, -1);

	ENABLE_INTR(intr);
}
#endif

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
		char *param1 = nextWord(&line);
		char *param2 = nextWord(&line);
		char *param3 = nextWord(&line);

		if (strcmp(name, "BufferLogWrites") == 0) {
			commonInfo->bufferLogWrites = 1;
		} else if (strcmp(name, "LogBufferLength") == 0) {
			commonInfo->maxLogBufferLength = parseHex(param1);
		} else if (strcmp(name, "FlushLogBuffer") == 0) {
			flushLogBuffer();
		} else if (strcmp(name, "DumpMemory") == 0) {
			u32 startAddress = parseHex(param1);
			u32 length = parseHex(param2);
			char *fileName = param3;

			dumpMemory(startAddress, length, fileName);
		} else if (strcmp(name, "write32") == 0) {
			u32 address = parseHex(param1);
			u32 value = parseHex(param2);
			SW(value, address);
		} else if (strcmp(name, "read32") == 0) {
			u32 address = parseHex(param1);
			u32 value = LW(address);
			printLogHH("read32(", address, ")=", value, "\n");
		} else if (strcmp(name, "DecryptMeimg") == 0) {
			char *fromFileName = param1;
			char *toFileName = param2;

			decryptMeimg(fromFileName, toFileName);
		} else if (strcmp(name, "ExecuteKirkCommand") == 0) {
			char *param4 = nextWord(&line);
			char *param5 = nextWord(&line);

			int cmd = parseHex(param1);
			char *outputFileName = param2;
			int outputSize = parseHex(param3);
			char *inputFileName = param4;
			int inputSize = parseHex(param5);

			executeKirkCommand(cmd, outputFileName, outputSize, inputFileName, inputSize);
		} else if (strcmp(name, "IdStorageRead") == 0) {
			int key = parseHex(param1);
			char *outputFileName = param2;

			executeIdStorageRead(key, outputFileName);
		} else if (strcmp(name, "DumpNand") == 0) {
			dumpNand();
#if TEST_ATA
		} else if (strcmp(name, "TestAta") == 0) {
			testAta();
#endif
		} else {
			u32 nid = parseHex(param1);
			u32 numParams = parseHex(param2);
			u32 flags = 0;
			u32 paramTypes = parseParamTypes(param3, &flags);

			// If no numParams specified, take maximum number of params
			if (strlen(param2) == 0) {
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
	int id[200];
	int idcount = 0;

	printLogSH("Starting module '", startingModule->modname, "' at ", startingModule->text_addr, "\n");
	u32 maxFreeMem = sceKernelMaxFreeMemSize();
	u32 totalFreeMem = sceKernelTotalFreeMemSize();
	printLogHH("TotalFreeMem=", totalFreeMem, ", MaxFreeMem=", maxFreeMem, "\n");

	// Do not patch myself...
	if (strcmp(startingModule->modname, "JpcspTraceUser") != 0) {
		// Check if the starting module is providing still missing NIDs
		int syscallInfoUpdated = 0;
		SyscallInfo **pLastSyscallInfo = &moduleSyscalls;
		for (syscallInfo = moduleSyscalls; syscallInfo != NULL; syscallInfo = syscallInfo->next) {
			if (syscallInfo->originalEntry == NULL) {
				syscallInfo->originalEntry = getEntryByModule(startingModule, syscallInfo->nid);
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

#if DUMP_PSAR
void findPatches() {
	int n, i, j;
	int id[100];
	int idcount = 0;
	struct SceLibraryEntryTable *entry;

	int result = sceKernelGetModuleIdList(id, sizeof(id), &idcount);
	if (result != 0) {
		return;
	}

	for (n = 0; n < idcount; n++) {
		SceModule *module = sceKernelFindModuleByUID(id[n]);
		for (i = 0; i < module->ent_size; i += entry->len * 4) {
			entry = (struct SceLibraryEntryTable *) (module->ent_top + i);
			if (entry->libname != NULL) {
				int numEntries = entry->stubcount + entry->vstubcount;
				u32 *entries = (u32 *) entry->entrytable;
				for (j = 0; j < entry->stubcount; j++) {
					if (entries[j] == 0x102DC8AF) {
						sceUtilsBufferCopyByPollingWithRangeAddr = entries[numEntries + j] + 0x6BFC - 0x4A0C;
						sceUtilsBufferCopyWithRangeAddr = entries[numEntries + j] + 0x6BF4 - 0x4A0C;
					}
				}
			}
		}
	}
}
#endif


int loadUserModule(SceSize args, void * argp) {
	int userModuleId = -1;

	// Make sure we are running in user mode (required by sceKernelLoadModule)
	int k1 = pspSdkSetK1(0x100000);

	// Load the user module JpcspTraceUser.prx.
	// Retry to load it several times if the module manager is currently busy.
	while (1) {
		if (userModuleId < 0) {
			// We need to copy all parameter values from kernel memory to user memory
			// (this is required by sceKernelLoadModule)
			void *mem = alloc(sizeof(SceKernelLMOption) + 40);

			SceKernelLMOption *pOptions = mem;
			memset(pOptions, 0, sizeof(*pOptions));
			// Load the user module in high memory
			pOptions->size = sizeof(*pOptions);
			pOptions->mpidtext = PSP_MEMORY_PARTITION_USER;
			pOptions->position = PSP_SMEM_High;

			char *fileName = mem + sizeof(*pOptions);
			strcpy(fileName, "ms0:/seplugins/JpcspTraceUser.prx");

			userModuleId = sceKernelLoadModule(fileName, 0, pOptions);
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

	int ent_size = module == NULL ? 0 : module->ent_size;
	for (i = 0; i < ent_size; i += entry->len * 4) {
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

	pspSdkSetK1(k1);

	sceKernelExitDeleteThread(0);

	return 0;
}

#if DUMP_MEMORYSTICK
void dumpMemoryStick() {
	SceUID fdBlock = ioOpen("ms0:/ms.block", PSP_O_WRONLY | PSP_O_CREAT, 0777);
	if (fdBlock < 0) {
		printLog("dumpMemoryStick - Cannot create ms.block file\n");
		return;
	}

	SceUID ms = ioOpen("msstor0p1:", PSP_O_RDONLY, 0777);
	if (ms < 0) {
		printLog("dumpMemoryStick - Cannot open the MemoryStick\n");
		return;
	}

	int result = sceIoLseek(ms, 0, 0);
	if (result < 0) {
		printLog("dumpMemoryStick - Cannot seek on the MemoryStick\n");
		return;
	}

	int bufferSize = 0x10000;
	void *buffer = alloc(bufferSize);

	result = sceIoIoctl(ms, 0x2125001, NULL, 0, buffer, 4);
	printLogHH("sceIoIoctl 0x2125001 returned ", result, ", out=", ((u32 *) buffer)[0], "\n");

	result = sceIoIoctl(ms, 0x2125803, NULL, 0, buffer, 0x60);
	printLogH("sceIoIoctl 0x2125803 returned ", result, "\n");
	printLogMem("   out=", (int) buffer, 0x60);
	SceUID fd = ioOpen("ms0:/ms.ioctl.0x02125803", PSP_O_WRONLY | PSP_O_CREAT, 0777);
	if (fd >= 0) {
		ioWrite(fd, buffer, 0x60);
		ioClose(fd);
	}

	result = sceIoIoctl(ms, 0x2125008, NULL, 0, buffer, 4);
	printLogHH("sceIoIoctl 0x2125008 returned ", result, ", out=", ((u32 *) buffer)[0], "\n");

	result = sceIoIoctl(ms, 0x2125009, NULL, 0, buffer, 4);
	printLogHH("sceIoIoctl 0x2125009 returned ", result, ", out=", ((u32 *) buffer)[0], "\n");

	int i;
	for (i = 0; i < 0x1000; i++) {
		result = sceIoRead(ms, buffer, bufferSize);
		if (result < 0) {
			printLog("dumpMemoryStick - Cannot read the MemoryStick\n");
			return;
		}

		ioWrite(fdBlock, buffer, bufferSize);
	}

	ioClose(ms);
	ioClose(fdBlock);
}
#endif

u32 nandDmaIntrOld = -1;
void checkNandDma() {
	u32 nandDmaIntr = LW(0xBD101038);
	if (nandDmaIntr != nandDmaIntrOld) {
		printLogH("nandDmaIntr=", nandDmaIntr, "\n");
	}
	nandDmaIntrOld = nandDmaIntr;
}

// Module Start
int module_start(SceSize args, void * argp) {
	// Find Allocator Functions in Memory
	allocFunc = (void *) sctrlHENFindFunction("sceSystemMemoryManager", "SysMemUserForUser", 0x237DBD4F);
	getHeadFunc = (void *) sctrlHENFindFunction("sceSystemMemoryManager", "SysMemUserForUser", 0x9D9A5BA1);
	originalIoOpen = (void *) sctrlHENFindFunction("sceIOFileManager", "IoFileMgrForUser", NID_sceIoOpen);
	originalIoWrite = (void *) sctrlHENFindFunction("sceIOFileManager", "IoFileMgrForUser", NID_sceIoWrite);
	originalIoClose = (void *) sctrlHENFindFunction("sceIOFileManager", "IoFileMgrForUser", NID_sceIoClose);

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
	commonInfo->bufferLogWrites = 0;

	sceIoRemove(logFilename);

	commonInfo->logKeepOpen = 1;
	openLogFile();

	printLog("JpcspTrace - module_start\n");

	#if 0
	int n = 0;
	int i;
	int count = 100000;
	int memSize = count * 4;
	u32 *timings = alloc(memSize);
	SW(0x00000007, 0xBE000008);
	SW(0x00000001, 0xBE000004);
	SW(0x00000001, 0xBE000010);
	SW(0x00000001, 0xBE000024);
	for (i = 0; i < count; i++) {
		u32 start = LW(0xBC600000);
		SW((n << 16) | n, 0xBE000060);
		u32 end = LW(0xBC600000);

		u32 interrupt = LW(0xBE00001C);
		u32 inProgress = LW(0xBE00000C);
		u32 completed = LW(0xBE000028);
		timings[i] = end - start;
		timings[i] |= (interrupt & 0xFF) << 24;
		timings[i] |= (inProgress & 0xFF) << 16;
		timings[i] |= (completed & 0xFF) << 8;

		n += 0x0010;
		if (n > 0xFFFF) {
			n = 0;
		}
	}
	SceUID fd = ioOpen("ms0:/audioTimings.dump", PSP_O_WRONLY | PSP_O_CREAT, 0777);
	ioWrite(fd, timings, memSize);
	ioClose(fd);
	freeAlloc(timings, memSize);
	#endif

	#if DUMP_MEMORYSTICK
	dumpMemoryStick();
	#endif

	int initKeyConfig = sceKernelInitKeyConfig();
	if (initKeyConfig == PSP_INIT_KEYCONFIG_VSH) {
		printLog("JpcspTrace enabled for VSH\n");
	} else if (initKeyConfig != PSP_INIT_KEYCONFIG_GAME) {
		printLogH("sceKernelInitKeyConfig returned ", initKeyConfig, "\n");
		return 1;
	}

	#if DEBUG >= 2
	printAllModules();
	printAllSyscalls();
	#endif

#if DUMP_PSAR
	findPatches();
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

	flushLogBuffer();
	closeLogFile();

	return 0;
}
