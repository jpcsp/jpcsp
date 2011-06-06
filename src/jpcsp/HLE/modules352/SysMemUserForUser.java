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

package jpcsp.HLE.modules352;

import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.Memory;
import static jpcsp.util.Utilities.readStringNZ;

public class SysMemUserForUser extends jpcsp.HLE.modules280.SysMemUserForUser {
	@Override
	public void installModule(HLEModuleManager mm, int version) {
		super.installModule(mm, version);
		
		if (version >= 352) {
		
			mm.addFunction(0x50F61D8A, SysMemUserForUser_50F61D8AFunction);
			mm.addFunction(0xACBD88CA, SysMemUserForUser_ACBD88CAFunction);
			mm.addFunction(0xDB83A952, SysMemUserForUser_DB83A952Function);
			mm.addFunction(0xFE707FDF, SysMemUserForUser_FE707FDFFunction);
			
		}
	}
	
	@Override
	public void uninstallModule(HLEModuleManager mm, int version) {
		super.uninstallModule(mm, version);
		
		if (version >= 352) {
		
			mm.removeFunction(SysMemUserForUser_50F61D8AFunction);
			mm.removeFunction(SysMemUserForUser_ACBD88CAFunction);
			mm.removeFunction(SysMemUserForUser_DB83A952Function);
			mm.removeFunction(SysMemUserForUser_FE707FDFFunction);
			
		}
	}

    // sceKernelFreeMemoryBlock (internal name)
	public void SysMemUserForUser_50F61D8A(Processor processor) {
		CpuState cpu = processor.cpu;
		
		int uid = cpu.gpr[4];

		SysMemInfo info = blockList.remove(uid);
        if (info == null) {
            log.warn("SysMemUserForUser_50F61D8A(uid=0x" + Integer.toHexString(uid) + ") unknown uid");
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_UNKNOWN_UID;
        } else {
            log.debug("SysMemUserForUser_50F61D8A(uid=0x" + Integer.toHexString(uid) + ")");
            free(info);
            cpu.gpr[2] = 0;
        }
	}
    
	public void SysMemUserForUser_ACBD88CA(Processor processor) {
		CpuState cpu = processor.cpu;

		log.debug("Unimplemented NID function SysMemUserForUser_ACBD88CA [0xACBD88CA]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

    // sceKernelGetMemoryBlockAddr (internal name)
	public void SysMemUserForUser_DB83A952(Processor processor) {
		CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();
		
		int uid = cpu.gpr[4];
		int addr = cpu.gpr[5];

		SysMemInfo info = blockList.get(uid);
        if (info == null) {
            log.warn("SysMemUserForUser_DB83A952(uid=0x" + Integer.toHexString(uid)
                    + ", addr=0x" + Integer.toHexString(addr) + ") unknown uid");
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_UNKNOWN_UID;
        } else if (!Memory.isAddressGood(addr)) {
            log.warn("SysMemUserForUser_DB83A952(uid=0x" + Integer.toHexString(uid)
                    + ", addr=0x" + Integer.toHexString(addr) + ") bad addr");
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_ILLEGAL_ADDR;
        } else {
            if (log.isDebugEnabled()) {
        		log.debug("SysMemUserForUser_DB83A952(uid=0x" + Integer.toHexString(uid)
                    + ", addr=0x" + Integer.toHexString(addr) + ") addr 0x" + Integer.toHexString(info.addr));
        	}
            mem.write32(addr, info.addr);
            cpu.gpr[2] = 0;
        }
	}

	// sceKernelAllocMemoryBlock (internal name)
	public void SysMemUserForUser_FE707FDF(Processor processor) {
		CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();
		
		int pname = cpu.gpr[4];
		int type = cpu.gpr[5];
		int size = cpu.gpr[6];
		int paramsAddr = cpu.gpr[7];

		String name = readStringNZ(pname, 32);
		if (log.isDebugEnabled()) {
	        log.debug(String.format("SysMemUserForUser_FE707FDF(name='%s', type=%s, size=0x%X, paramsAddr=0x%08X", name, getTypeName(type), size, paramsAddr));
		}
        if (paramsAddr != 0) {
        	int length = mem.read32(paramsAddr);
        	if (length != 4) {
        		log.warn("SysMemUserForUser_FE707FDF: unknown parameters with length=" + length);
        	}
        }
        if (type < PSP_SMEM_Low || type > PSP_SMEM_High) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_ILLEGAL_MEMBLOCK_ALLOC_TYPE;
        } else {
            // Always allocate memory in user area (partitionid == 2).
            SysMemInfo info = malloc(2, name, type, size, 0);
            if (info != null) {
                cpu.gpr[2] = info.uid;
            } else {
                cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_FAILED_ALLOC_MEMBLOCK;
            }
        }
	}
    
	public final HLEModuleFunction SysMemUserForUser_50F61D8AFunction = new HLEModuleFunction("SysMemUserForUser", "SysMemUserForUser_50F61D8A") {
		@Override
		public final void execute(Processor processor) {
			SysMemUserForUser_50F61D8A(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.SysMemUserForUserModule.SysMemUserForUser_50F61D8A(processor);";
		}
	};
    
	public final HLEModuleFunction SysMemUserForUser_ACBD88CAFunction = new HLEModuleFunction("SysMemUserForUser", "SysMemUserForUser_ACBD88CA") {
		@Override
		public final void execute(Processor processor) {
			SysMemUserForUser_ACBD88CA(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.SysMemUserForUserModule.SysMemUserForUser_ACBD88CA(processor);";
		}
	};
    
	public final HLEModuleFunction SysMemUserForUser_DB83A952Function = new HLEModuleFunction("SysMemUserForUser", "SysMemUserForUser_DB83A952") {
		@Override
		public final void execute(Processor processor) {
			SysMemUserForUser_DB83A952(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.SysMemUserForUserModule.SysMemUserForUser_DB83A952(processor);";
		}
	};
    
	public final HLEModuleFunction SysMemUserForUser_FE707FDFFunction = new HLEModuleFunction("SysMemUserForUser", "SysMemUserForUser_FE707FDF") {
		@Override
		public final void execute(Processor processor) {
			SysMemUserForUser_FE707FDF(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.SysMemUserForUserModule.SysMemUserForUser_FE707FDF(processor);";
		}
	};
}