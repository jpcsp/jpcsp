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
import jpcsp.format.PSPModuleInfo;

import static jpcsp.HLE.modules.ModuleMgrForUser.SCE_HEADER_LENGTH;
import static jpcsp.HLE.modules.SysMemUserForUser.KERNEL_PARTITION_ID;
import static jpcsp.HLE.modules.SysMemUserForUser.PSP_SMEM_Low;
import static jpcsp.Loader.SCE_MAGIC;
import static jpcsp.format.PSP.PSP_HEADER_SIZE;
import static jpcsp.format.PSP.PSP_MAGIC;

import org.apache.log4j.Logger;

public class LoadCoreForKernel extends HLEModule {
    public static Logger log = Modules.getLogger("LoadCoreForKernel");

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
			TPointer modBuf = allocMem(1024);
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
			modBuf.setValue32(offset + 0, PSP_MAGIC);
			modBuf.setValue16(offset + 4, (short) 0x1000); // PspHeader.mod_attr = SCE_MODULE_KERNEL
			modBuf.setValue32(offset + 52, 0x80000000); // PspHeader.modinfo_offset = IS_KERNEL_ADDR
			modBuf.setValue8(offset + 124, (byte) 2); // PspHeader.dec_mode = DECRYPT_MODE_KERNEL_MODULE
			offset += PSP_HEADER_SIZE;

			// ELF header
			modBuf.setValue32(offset + 0, Elf32Header.ELF_MAGIC);
			modBuf.setValue8(offset + 4, (byte) 1); // Elf32Header.e_class = ELFCLASS32
			modBuf.setValue8(offset + 5, (byte) 1); // Elf32Header.e_data = ELFDATA2LSB
			modBuf.setValue16(offset + 16, (short) 0xFFA0); // Elf32Header.e_type = ET_SCE_PRX
			modBuf.setValue16(offset + 18, (short) Elf32Header.E_MACHINE_MIPS); // Elf32Header.e_machine = EM_MIPS_ALLEGREX
			modBuf.setValue32(offset + 28, Elf32Header.sizeof()); // Elf32Header.e_phoff = sizeof(Elf32Header), must be > 0
			modBuf.setValue16(offset + 44, (short) 1); // Elf32Header.e_phnum, must be > 0
			offset += Elf32Header.sizeof();

			// ELF Program header
			modBuf.setValue32(offset + 0, 1); // Elf32ProgramHeader.p_type = PT_LOAD
			modBuf.setValue32(offset + 8, 0); // Elf32ProgramHeader.p_vaddr = 0
			modBuf.setValue32(offset + 12, Elf32Header.sizeof() + Elf32ProgramHeader.sizeof()); // Elf32ProgramHeader.p_paddr = sizeof(Efl32Header) + sizeof(Elf32ProgramHeader)
			offset += Elf32ProgramHeader.sizeof();

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
}