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
package jpcsp.HLE.modules;

import jpcsp.Memory;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.PspString;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.types.SceLoadCoreBootInfo;
import jpcsp.HLE.kernel.types.SceLoadCoreBootModuleInfo;
import jpcsp.HLE.kernel.types.SceLoadCoreExecFileInfo;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.HLE.kernel.types.SysMemThreadConfig;
import jpcsp.HLE.modules.SysMemUserForUser.SysMemInfo;
import jpcsp.format.Elf32Header;
import jpcsp.format.Elf32ProgramHeader;
import jpcsp.format.Elf32SectionHeader;
import jpcsp.format.PSPModuleInfo;

import static jpcsp.Allegrex.Common._v0;
import static jpcsp.Allegrex.Common._zr;
import static jpcsp.HLE.modules.ModuleMgrForUser.SCE_HEADER_LENGTH;
import static jpcsp.HLE.modules.SysMemUserForUser.KERNEL_PARTITION_ID;
import static jpcsp.HLE.modules.SysMemUserForUser.PSP_SMEM_Low;
import static jpcsp.HLE.modules.ThreadManForUser.ADDIU;
import static jpcsp.HLE.modules.ThreadManForUser.JR;
import static jpcsp.Loader.SCE_MAGIC;
import static jpcsp.format.Elf32SectionHeader.SHF_ALLOCATE;
import static jpcsp.format.Elf32SectionHeader.SHF_EXECUTE;
import static jpcsp.format.Elf32SectionHeader.SHT_PROGBITS;
import static jpcsp.format.PSP.PSP_HEADER_SIZE;
import static jpcsp.format.PSP.PSP_MAGIC;

import org.apache.log4j.Logger;

public class LoadCoreForKernel extends HLEModule {
    public static Logger log = Modules.getLogger("LoadCoreForKernel");
    private int dummyModuleData;

    private TPointer allocMem(int size) {
		SysMemInfo memInfo = Modules.SysMemUserForUserModule.malloc(KERNEL_PARTITION_ID, "LoadCore-StartModuleParameters", PSP_SMEM_Low, size, 0);
		if (memInfo == null) {
			log.error(String.format("Cannot allocate memory for loadcore.prx start parameters"));
			return TPointer.NULL;
		}

		TPointer pointer = new TPointer(Memory.getInstance(), memInfo.addr);
		pointer.clear(size);

		return pointer;
    }

    public boolean decodeDummyModuleData(TPointer buffer, int size, TPointer32 resultSize) {
    	if (buffer.getAddress() != dummyModuleData) {
    		return false;
    	}

    	buffer.memmove(buffer.getAddress() + PSP_HEADER_SIZE, size - PSP_HEADER_SIZE);
    	resultSize.setValue(size - PSP_HEADER_SIZE);

    	return true;
    }

    public TPointer prepareModuleStart() {
		SceLoadCoreBootInfo sceLoadCoreBootInfo = new SceLoadCoreBootInfo();
		SysMemThreadConfig sysMemThreadConfig = new SysMemThreadConfig();
		SceLoadCoreExecFileInfo loadCoreExecInfo = new SceLoadCoreExecFileInfo();
		SceLoadCoreExecFileInfo sysMemExecInfo = new SceLoadCoreExecFileInfo();
		PSPModuleInfo loadCoreModuleInfo = new PSPModuleInfo();
		PSPModuleInfo sysMemModuleInfo = new PSPModuleInfo();
    	// TODO

		// Computing a checksum on the first 64 bytes of the first segment
		int dummySegmentSize = 64;
		TPointer dummySegmentAddr = allocMem(dummySegmentSize);
		loadCoreExecInfo.segmentAddr[0] = dummySegmentAddr;
		loadCoreExecInfo.segmentSize[0] = dummySegmentSize;
		loadCoreExecInfo.numSegments = 1;
		sysMemExecInfo.segmentAddr[0] = dummySegmentAddr;
		sysMemExecInfo.segmentSize[0] = dummySegmentSize;
		sysMemExecInfo.numSegments = 1;

		// At least 1 module is required
		sceLoadCoreBootInfo.numModules = 1;
		SceLoadCoreBootModuleInfo sceLoadCoreBootModuleInfo = new SceLoadCoreBootModuleInfo();
		sceLoadCoreBootInfo.modules = allocMem(sceLoadCoreBootInfo.numModules * sceLoadCoreBootModuleInfo.sizeof());
		for (int i = 0; i < sceLoadCoreBootInfo.numModules; i++) {
			// Dummy module data
			final int moduleInfoSizeof = new PSPModuleInfo().sizeof();
			final int dummyCodeSize = 8;
			int totalSize = SCE_HEADER_LENGTH + PSP_HEADER_SIZE + Elf32Header.sizeof() + Elf32ProgramHeader.sizeof() + Elf32SectionHeader.sizeof() + moduleInfoSizeof + dummyCodeSize;
			TPointer modBuf = allocMem(totalSize);
			if (log.isTraceEnabled()) {
				log.trace(String.format("Allocated dummy module buffer %s", modBuf));
			}
			sceLoadCoreBootModuleInfo.modBuf = modBuf;

			int offset = 0;
			// SCE header
			modBuf.setValue32(offset + 0, SCE_MAGIC);
			modBuf.setValue32(offset + 4, SCE_HEADER_LENGTH); // SceHeader.size
			offset += SCE_HEADER_LENGTH;

			// PSP header
			dummyModuleData = modBuf.getAddress() + offset;
			modBuf.setValue32(offset + 0, PSP_MAGIC);
			modBuf.setValue16(offset + 4, (short) 0x1000); // PspHeader.mod_attr = SCE_MODULE_KERNEL
			modBuf.setValue32(offset + 44, totalSize - SCE_HEADER_LENGTH); // PspHeader.psp_size, must be > 0
			modBuf.setValue32(offset + 48, 0); // PspHeader.boot_entry = 0
			modBuf.setValue32(offset + 52, 0x80000000 + Elf32Header.sizeof() + Elf32ProgramHeader.sizeof() + Elf32SectionHeader.sizeof()); // PspHeader.modinfo_offset = IS_KERNEL_ADDR
			modBuf.setValue8(offset + 124, (byte) 2); // PspHeader.dec_mode = DECRYPT_MODE_KERNEL_MODULE
			modBuf.setValue32(offset + 208, 0); // PspHeader.tag = 0
			offset += PSP_HEADER_SIZE;

			// ELF header
			modBuf.setValue32(offset + 0, Elf32Header.ELF_MAGIC);
			modBuf.setValue8(offset + 4, (byte) 1); // Elf32Header.e_class = ELFCLASS32
			modBuf.setValue8(offset + 5, (byte) 1); // Elf32Header.e_data = ELFDATA2LSB
			modBuf.setValue16(offset + 16, (short) 0xFFA0); // Elf32Header.e_type = ET_SCE_PRX
			modBuf.setValue16(offset + 18, (short) Elf32Header.E_MACHINE_MIPS); // Elf32Header.e_machine = EM_MIPS_ALLEGREX
			modBuf.setValue32(offset + 24, moduleInfoSizeof); // Elf32Header.e_entry = dummy entry point, must be != 0
			modBuf.setValue32(offset + 28, Elf32Header.sizeof()); // Elf32Header.e_phoff = sizeof(Elf32Header)
			modBuf.setValue32(offset + 32, Elf32Header.sizeof() + Elf32ProgramHeader.sizeof()); // Elf32Header.e_shoff = sizeof(Elf32Header) + sizeof(Elf32ProgramHeader)
			modBuf.setValue16(offset + 44, (short) 1); // Elf32Header.e_phnum, must be > 0
			modBuf.setValue16(offset + 48, (short) 1); // Elf32Header.e_shnum, must be > 0
			modBuf.setValue16(offset + 50, (short) 0); // Elf32Header.e_shstrndx = 0
			offset += Elf32Header.sizeof();

			// ELF Program header
			modBuf.setValue32(offset + 0, 1); // Elf32ProgramHeader.p_type = PT_LOAD
			modBuf.setValue32(offset + 4, Elf32Header.sizeof() + Elf32ProgramHeader.sizeof() + Elf32SectionHeader.sizeof()); // Elf32ProgramHeader.p_offset
			modBuf.setValue32(offset + 8, 0); // Elf32ProgramHeader.p_vaddr = 0
			modBuf.setValue32(offset + 12, Elf32Header.sizeof() + Elf32ProgramHeader.sizeof() + Elf32SectionHeader.sizeof()); // Elf32ProgramHeader.p_paddr = sizeof(Efl32Header) + sizeof(Elf32ProgramHeader)
			modBuf.setValue32(offset + 16, moduleInfoSizeof + dummyCodeSize); // Elf32ProgramHeader.p_filesz
			modBuf.setValue32(offset + 20, moduleInfoSizeof + dummyCodeSize); // Elf32ProgramHeader.p_memsz
			offset += Elf32ProgramHeader.sizeof();

			// ELF Section header
			modBuf.setValue32(offset + 4, SHT_PROGBITS); // Elf32SectionHeader.sh_type = SHT_PROGBITS
			modBuf.setValue32(offset + 8, SHF_ALLOCATE | SHF_EXECUTE); // Elf32SectionHeader.sh_flags = SHF_ALLOCATE | SHF_EXECUTE
			modBuf.setValue32(offset + 16, 1); // Elf32SectionHeader.sh_offset, must be > 0
			modBuf.setValue32(offset + 20, 0); // Elf32SectionHeader.sh_size = 0
			offset += Elf32SectionHeader.sizeof();

			// PSP Module Info
			modBuf.setStringNZ(offset + 4, 28, "sceInit"); // PSPModuleInfo.m_name = "sceInit"
			offset += new PSPModuleInfo().sizeof();

			// Dummy entry point
			// Code for "return SCE_KERNEL_NO_RESIDENT"
			modBuf.setValue32(offset + 0, JR());
			modBuf.setValue32(offset + 4, ADDIU(_v0, _zr, 1));
			offset += dummyCodeSize;

			TPointer sceLoadCoreBootModuleInfoAddr = new TPointer(sceLoadCoreBootInfo.modules, i * sceLoadCoreBootModuleInfo.sizeof());
			sceLoadCoreBootModuleInfo.write(sceLoadCoreBootModuleInfoAddr);
		}

		TPointer loadCoreModuleInfoAddr = allocMem(loadCoreModuleInfo.sizeof());
		loadCoreModuleInfo.write(loadCoreModuleInfoAddr);
		loadCoreExecInfo.moduleInfo = loadCoreModuleInfoAddr;

		TPointer sysMemModuleInfoAddr = allocMem(sysMemModuleInfo.sizeof());
		sysMemModuleInfo.write(sysMemModuleInfoAddr);
		sysMemExecInfo.moduleInfo = sysMemModuleInfoAddr;

		TPointer sysMemExecInfoAddr = allocMem(sysMemExecInfo.sizeof());
		sysMemExecInfo.write(sysMemExecInfoAddr);
		sysMemThreadConfig.sysMemExecInfo = sysMemExecInfoAddr;

		TPointer loadCoreExecInfoAddr = allocMem(loadCoreExecInfo.sizeof());
		loadCoreExecInfo.write(loadCoreExecInfoAddr);
		sysMemThreadConfig.loadCoreExecInfo = loadCoreExecInfoAddr;

		TPointer sceLoadCoreBootInfoAddr = allocMem(sceLoadCoreBootInfo.sizeof());
		sceLoadCoreBootInfo.write(sceLoadCoreBootInfoAddr);

		TPointer sysMemThreadConfigAddr = allocMem(sysMemThreadConfig.sizeof());
		sysMemThreadConfig.write(sysMemThreadConfigAddr);

    	TPointer argp = allocMem(8);
		argp.setPointer(0, sceLoadCoreBootInfoAddr);
		argp.setPointer(4, sysMemThreadConfigAddr);

		if (log.isTraceEnabled()) {
			log.trace(String.format("prepareModuleStart sceLoadCoreBootInfoAddr=%s, sysMemThreadConfigAddr=%s, loadCoreExecInfoAddr=%s, sysMemExecInfoAddr=%s", sceLoadCoreBootInfoAddr, sysMemThreadConfigAddr, loadCoreExecInfoAddr, sysMemExecInfoAddr));
		}

		return argp;
    }

    @HLEUnimplemented
	@HLEFunction(nid = 0xACE23476, version = 150)
	public int sceKernelCheckPspConfig() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x7BE1421C, version = 150)
	public int sceKernelCheckExecFile() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xBF983EF2, version = 150)
	public int sceKernelProbeExecutableObject() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x7068E6BA, version = 150)
	public int sceKernelLoadExecutableObject() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xB4D6FECC, version = 150)
	public int sceKernelApplyElfRelSection() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x54AB2675, version = 150)
	public int sceKernelApplyPspRelSection() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x2952F5AC, version = 150)
	public int sceKernelDcacheWBinvAll() {
		return 0;
	}

    @HLELogging(level="trace")
    @HLEFunction(nid = 0xD8779AC6, version = 150)
    public int sceKernelIcacheClearAll() {
        return 0;
    }

    @HLEUnimplemented
	@HLEFunction(nid = 0x99A695F0, version = 150)
	public int sceKernelRegisterLibrary() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x5873A31F, version = 150)
	public int sceKernelRegisterLibraryForUser() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x0B464512, version = 150)
	public int sceKernelReleaseLibrary() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x9BAF90F6, version = 150)
	public int sceKernelCanReleaseLibrary() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x0E760DBA, version = 150)
	public int sceKernelLinkLibraryEntries() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x0DE1F600, version = 150)
	public int sceKernelLinkLibraryEntriesForUser() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xDA1B09AA, version = 150)
	public int sceKernelUnLinkLibraryEntries() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xC99DD47A, version = 150)
	public int sceKernelQueryLoadCoreCB() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x616FCCCD, version = 150)
	public int sceKernelSetBootCallbackLevel() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x52A86C21, version = 150)
	public int sceKernelGetModuleFromUID() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xCD0F3BAC, version = 150)
	public int sceKernelCreateModule() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x6B2371C2, version = 150)
	public int sceKernelDeleteModule() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x8D8A8ACE, version = 150)
	public int sceKernelAssignModule() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xAFF947D4, version = 150)
	public int sceKernelCreateAssignModule() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xAE7C6E76, version = 150)
	public int sceKernelRegisterModule() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x74CF001A, version = 150)
	public int sceKernelReleaseModule() {
		return 0;
	}

    @HLEFunction(nid = 0xCF8A41B1, version = 150)
    public int sceKernelFindModuleByName(PspString moduleName) {
        SceModule module = Managers.modules.getModuleByName(moduleName.getString());
        if (module == null) {
            log.warn(String.format("sceKernelFindModuleByName not found moduleName=%s", moduleName));
            return 0; // return NULL
        }

        if (!Modules.ThreadManForUserModule.isKernelMode()) {
            log.warn("kernel mode required (sceKernelFindModuleByName)");
        }

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceKernelFindModuleByName returning 0x%08X", module.address));
        }

        return module.address;
    }

    @HLEFunction(nid = 0xFB8AE27D, version = 150)
    public int sceKernelFindModuleByAddress(TPointer address) {
        SceModule module = Managers.modules.getModuleByAddress(address.getAddress());
        if (module == null) {
            log.warn(String.format("sceKernelFindModuleByAddress not found module address=%s", address));
            return 0; // return NULL
        }

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceKernelFindModuleByAddress found module '%s'", module.modname));
        }

        if (!Modules.ThreadManForUserModule.isKernelMode()) {
            log.warn("kernel mode required (sceKernelFindModuleByAddress)");
        }

        return module.address;
	}

    @HLEFunction(nid = 0xCCE4A157, version = 150)
    public int sceKernelFindModuleByUID(int uid) {
        SceModule module = Managers.modules.getModuleByUID(uid);
        if (module == null) {
            log.warn(String.format("sceKernelFindModuleByUID not found module uid=0x%X", uid));
            return 0; // return NULL
        }

        // The pspsdk is not properly handling module exports with a size > 4.
        // See
        //    pspSdkFindExport()
        // in
        //    https://github.com/pspdev/pspsdk/blob/master/src/sdk/fixup.c
        // which is assuming that all module exports have a size==4 (i.e. 16 bytes).
        // This code is leading to an invalid memory access when processing the exports
        // from real PSP modules, which do have exports with a size==5.
        // Ban these modules in a case of the homebrew.
        if (RuntimeContext.isHomebrew()) {
        	String[] bannedModules = {
        			"sceNet_Library",
        			"sceNetInet_Library",
        			"sceNetApctl_Library",
        			"sceNetResolver_Library"
        	};
        	for (String bannedModule : bannedModules) {
        		if (bannedModule.equals(module.modname)) {
        			if (log.isDebugEnabled()) {
        				log.debug(String.format("sceKernelFindModuleByUID banning module '%s' for a homebrew", module.modname));
        			}
            		return 0; // NULL
        		}
        	}
        }

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceKernelFindModuleByUID found module '%s'", module.modname));
        }

        if (!Modules.ThreadManForUserModule.isKernelMode()) {
            log.warn("kernel mode required (sceKernelFindModuleByUID)");
        }

        return module.address;
    }

    @HLEUnimplemented
	@HLEFunction(nid = 0x929B5C69, version = 150)
	public int sceKernelGetModuleListWithAlloc() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x05D915DB, version = 150)
	public int sceKernelGetModuleIdListForKernel() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xB27CC244, version = 150)
	public int sceKernelLoadRebootBin(@BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer fileData, int fileSize) {
		return 0;
	}

    /**
     * Load a module. This function is used to boot modules during the start of Loadcore. In order for 
     * a module to be loaded, it has to be a kernel module.
     * 
     * @param bootModInfo Pointer to module information (including the file content of the module, 
     *                    its size,...) used to boot the module.
     * @param execInfo Pointer an allocated execInfo structure used to handle load-checks against the 
     *                 program module.
     *                 Furthermore, it collects various information about the module, such as its elfType, 
     *                 its segments (.text, .data, .bss), the locations of its exported functions.
     * @param modMemId The memory id of the allocated kernelPRX memory block used for the program module 
     *                 sections. The memory block specified by the ID holds the .text segment of the module. 
     * 
     * @return 0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x493EE781, version = 660)
	public int sceKernelLoadModuleBootLoadCore(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=32, usage=Usage.in) TPointer bootModInfo, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=192, usage=Usage.out) TPointer execInfo, @BufferInfo(usage=Usage.out) TPointer32 modMemId) {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xD3353EC4, version = 660)
	public int sceKernelCheckExecFile(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=256, usage=Usage.in) TPointer buf, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=192, usage=Usage.inout) TPointer execInfo) {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x41D10899, version = 660)
	public int sceKernelProbeExecutableObject(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=256, usage=Usage.in) TPointer buf, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=192, usage=Usage.inout) TPointer execInfo) {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x1C394885, version = 660)
	public int sceKernelLoadExecutableObject(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=256, usage=Usage.in) TPointer buf, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=192, usage=Usage.inout) TPointer execInfo) {
		return 0;
	}

    /**
     * Register a resident library's entry table in the system. A resident module can register any 
     * number of resident libraries. Note that this function is only meant to register kernel mode 
     * resident libraries. In order to register user mode libraries, use sceKernelRegisterLibraryForUser().
     * 
     * @param libEntryTable Pointer to the resident library's entry table.
     * 
     * @return 0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x48AF96A9, version = 660)
	public int sceKernelRegisterLibrary(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=20, usage=Usage.in) TPointer libEntryTable) {
		return 0;
	}

    /**
     * Check if a resident library can be released. This check returns "true" when all corresponding stub
     * libraries at the time of the check have one the following status:
     *      a) unlinked
     *      b) have the the attribute SCE_LIB_WEAK_IMPORT (they can exist without the resident library 
     *         being registered).
     * 
     * @param libEntryTable Pointer to the resident library's entry table.
     * 
     * @return 0 indicates the library can be released.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x538129F8, version = 660)
	public int sceKernelCanReleaseLibrary(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=20, usage=Usage.in) TPointer libEntryTable) {
		return 0;
	}

    /**
     * Link kernel mode stub libraries with the corresponding registered resident libraries. Note that 
     * this function assumes that the resident libraries linked with reside in kernel memory. Linking 
     * with user mode resident libraries will result in failure.
     * 
     * @param libStubTable Pointer to a stub library's entry table. If you want to link an array of 
     *                     entry tables, make libStubTable a pointer to the first element of that array.
     * @param size         The number of entry tables to link.
     * 
     * @return 0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x8EAE9534, version = 660)
	public int sceKernelLinkLibraryEntries_660(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=26, usage=Usage.in) TPointer libEntryTable, int size) {
		return 0;
	}

    /**
     * Unlink stub libraries from their corresponding registered resident libraries. 
     * 
     * @param libStubTable Pointer to a stub library's entry table. If you want to unlink an array of 
     *                     entry tables, make libStubTable a pointer to the first element of that array.
     * @param size The number of entry tables to unlink.
     * @return 
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x0295CFCE, version = 660)
	public int sceKernelUnLinkLibraryEntries_660(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=26, usage=Usage.in) TPointer libEntryTable, int size) {
		return 0;
	}

    /**
     * Save interrupts state and disable all interrupts.
     * 
     * @return The current state of the interrupt controller. Use sceKernelLoadCoreUnlock() to return 
     *         to that state.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x1999032F, version = 660)
	public int sceKernelLoadCoreLock() {
    	// Has no parameters
		return 0;
	}

    /**
     * Return interrupt state.
     * 
     * @param intrState The state acquired by sceKernelLoadCoreLock().
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0xB6C037EA, version = 660)
	public int sceKernelLoadCoreUnlock(int intrState) {
		return 0;
	}

    /**
     * Register a user mode resident library's entry table in the system. A resident module can register 
     * any number of resident libraries. In order to register kernel mode libraries, use 
     * sceKernelRegisterLibrary().
     * 
     * Restrictions on user mode resident libraries:
     *    1) The resident library has to live in user memory.
     *    2) Functions cannot be exported via the SYSCALL technique.
     *    3) The resident library cannot be linked with stub libraries living in kernel memory.
     * 
     * @param libEntryTable Pointer to the resident library's entry table.
     * 
     * @return 0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x2C60CCB8, version = 660)
	public int sceKernelRegisterLibraryForUser_660(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=26, usage=Usage.in) TPointer libEntryTable) {
		return 0;
	}

    /**
     * Delete a registered resident library from the system. Deletion cannot be performed if there are 
     * loaded modules using the resident library. These modules must be deleted first.
     * 
     * @param libEntryTable Pointer to the resident library's entry table.
     * 
     * @return 0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0xCB636A90, version = 660)
	public int sceKernelReleaseLibrary(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=26, usage=Usage.in) TPointer libEntryTable) {
		return 0;
	}

    /**
     * 
     * @param libStubTable Pointer to a stub library's entry table. If you want to link an array of entry 
     *                     tables, make libStubTable a pointer to the first element of that array.
     * @param size The number of entry tables to link.
     * 
     * @return 0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x6ECFFFBA, version = 660)
	public int sceKernelLinkLibraryEntriesForUser(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=26, usage=Usage.in) TPointer libStubTable, int size) {
		return 0;
	}

    /**
     * 
     * @param libStubTable Pointer to a stub library's entry table. If you want to link an array of entry 
     *                     tables, make libStubTable a pointer to the first element of that array.
     * @param size The number of entry tables to link.
     * 
     * @return 0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0xA481E30E, version = 660)
	public int sceKernelLinkLibraryEntriesWithModule(TPointer mod, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=26, usage=Usage.in) TPointer libStubTable, int size) {
		return 0;
	}

    /**
     * Does nothing but a simple return.
     * 
     * @return 0.
     */
	@HLEFunction(nid = 0x1915737F, version = 660)
	public int sceKernelMaskLibraryEntries() {
    	// Has no parameters
		return 0;
	}

	/**
	 * Delete a module from the system. The module has to be stopped and released before.
	 * 
	 * @param mod The module to delete.
	 * 
	 * @return 0 on success.
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0x001B57BB, version = 660)
	public int sceKernelDeleteModule(TPointer mod) {
		return 0;
	}

	/**
	 * Allocate memory for a new SceModule structure and fill it with default values. This function is 
	 * called during the loading process of a module.
	 * 
	 * @return A pointer to the allocated SceModule structure on success, otherwise NULL.
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0x2C44F793, version = 660)
	public int sceKernelCreateModule_660() {
		// Has no parameters
		return 0;
	}

	/**
	 * Receive a list of UIDs of loaded modules.
	 * 
	 * @param modIdList Pointer to a SceUID array which will receive the UIDs of the loaded modules.
	 * @param size Size of modIdList. Specifies the number of entries that can be stored into modIdList.
	 * @param modCount A pointer which will receive the total number of loaded modules.
	 * @param userModsOnly Set to 1 to only receive UIDs from user mode modules. Set to 0 to receive UIDs 
	 *                     from all loaded modules.
	 * 
	 * @return 0 on success.
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0x37E6F41B, version = 660)
	public int sceKernelGetModuleIdListForKernel(TPointer32 modIdList, int size, TPointer32 modCount, boolean userModsOnly) {
		return 0;
	}

	/**
	 * Receive a list of UIDs of all loaded modules.
	 * 
	 * @param modCount A pointer which will receive the total number of loaded modules.
	 * 
	 * @return The UID of the allocated array containing UIDs of the loaded modules on success. It should 
	 *        be greater than 0.
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0x3FE631F0, version = 660)
	public int sceKernelGetModuleListWithAlloc(TPointer32 modCount) {
		return 0;
	}

	/**
	 * Find a loaded module by its UID.
	 * 
	 * @param uid The UID of the module to find.
	 * 
	 * @return Pointer to the found SceModule structure on success, otherwise NULL.
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0x40972E6E, version = 660)
	public int sceKernelFindModuleByUID_660(int uid) {
		return 0;
	}

	/**
	 * Get the global pointer value of a module.
	 * 
	 * @param addr Memory address belonging to the module, i.e. the address of a function/global variable 
	 *             within the module.
	 * 
	 * @return The global pointer value (greater than 0) of the found module on success.
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0x410084F9, version = 660)
	public int sceKernelGetModuleGPByAddressForKernel(int addr) {
		return 0;
	}

	/**
	 * Compute a checksum of every segment of a module.
	 * 
	 * @param mod The module to create the checksum for.
	 * 
	 * @return The checksum. Shouldn't be 0.
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0x5FDDB07A, version = 660)
	public int sceKernelSegmentChecksum(TPointer mod) {
		return 0;
	}

	/**
	 * Unlink a module from the internal loaded-modules-linked-list. The module has to be stopped before.
	 * 
	 * @param mod The module to release.
	 * 
	 * @return 0 on success.
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0xB17F5075, version = 660)
	public int sceKernelReleaseModule(TPointer mod) {
		return 0;
	}

	/**
	 * Find a loaded module containing the specified address.
	 * 
	 * @param addr Memory address belonging to the module, i.e. the address of a function/global variable 
	 *             within the module.
	 * 
	 * @return Pointer to the found SceModule structure on success, otherwise NULL.
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0xBC99C625, version = 660)
	public int sceKernelFindModuleByAddress(int addr) {
		return 0;
	}

	/**
	 * Find a loaded module by its name. If more than one module with the same name is loaded, return 
	 * the module which was loaded last.
	 * 
	 * @param name The name of the module to find. 
	 * 
	 * @return Pointer to the found SceModule structure on success, otherwise NULL.
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0xF6B1BF0F, version = 660)
	public int sceKernelFindModuleByName(String name) {
		return 0;
	}

	/**
	 * Register a module in the system and link it into the internal loaded-modules-linked-list.
	 * 
	 * @param mod The module to register.
	 * 
	 * @return 0.
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0xBF2E388C, version = 660)
	public int sceKernelRegisterModule(TPointer mod) {
		return 0;
	}

	/**
	 * Get a loaded module from its UID.
	 * 
	 * @param uid The UID (of a module) to check for.
	 * 
	 * @return Pointer to the found SceModule structure on success, otherwise NULL.
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0xCD26E0CA, version = 660)
	public int sceKernelGetModuleFromUID(int uid) {
		return 0;
	}

	/**
	 * Assign a module and check if it can be loaded, is a valid module and copy the moduleInfo section 
	 * of the execution file over to the SceModule structure.
	 * 
	 * @param mod The module to receive the moduleInfo section data based on the provided execution file 
	 *            information.
	 * @param execFileInfo The execution file information used to copy over the moduleInfo section for 
	 *        the specified module.
	 * 
	 * @return 0 on success.
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0xF3DD4808, version = 660)
	public int sceKernelAssignModule(TPointer mod, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=192, usage=Usage.inout) TPointer execFileInfo) {
		return 0;
	}
}