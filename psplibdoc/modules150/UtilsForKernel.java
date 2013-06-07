/* This autogenerated file is part of jpcsp. */
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

package jpcsp.HLE.modules150;

import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

import jpcsp.Memory;
import jpcsp.Processor;

import jpcsp.Allegrex.CpuState; // New-Style Processor

public class UtilsForKernel implements HLEModule {
	@Override
	public String getName() { return "UtilsForKernel"; }
	
	@Override
	public void installModule(HLEModuleManager mm, int version) {
		if (version >= 150) {
		
			mm.addFunction(sceUtilsKernelDcacheWBinvRangeFunction, 0x80FE032E);
			mm.addFunction(sceKernelUtilsMd5DigestFunction, 0xC8186A58);
			mm.addFunction(sceKernelUtilsMd5BlockInitFunction, 0x9E5C5086);
			mm.addFunction(sceKernelUtilsMd5BlockUpdateFunction, 0x61E1E525);
			mm.addFunction(sceKernelUtilsMd5BlockResultFunction, 0xB8D24E78);
			mm.addFunction(sceKernelUtilsSha1DigestFunction, 0x840259F1);
			mm.addFunction(sceKernelUtilsSha1BlockInitFunction, 0xF8FCD5BA);
			mm.addFunction(sceKernelUtilsSha1BlockUpdateFunction, 0x346F6DA8);
			mm.addFunction(sceKernelUtilsSha1BlockResultFunction, 0x585F1C09);
			mm.addFunction(sceKernelUtilsMt19937InitFunction, 0xE860E75E);
			mm.addFunction(sceKernelUtilsMt19937UIntFunction, 0x06FB8A63);
			mm.addFunction(sceKernelSetGPIMaskFunction, 0x193D4036);
			mm.addFunction(sceKernelSetGPOMaskFunction, 0x95035FEF);
			mm.addFunction(sceKernelGetGPIFunction, 0x37FB5C42);
			mm.addFunction(sceKernelSetGPOFunction, 0x6AD345D7);
			mm.addFunction(sceKernelRegisterLibcRtcFuncFunction, 0x7B7ED3FD);
			mm.addFunction(sceKernelReleaseLibcRtcFuncFunction, 0x6151A7C3);
			mm.addFunction(sceKernelLibcClockFunction, 0x91E4F6A7);
			mm.addFunction(sceKernelLibcTimeFunction, 0x27CC57F0);
			mm.addFunction(sceKernelLibcGettimeofdayFunction, 0x71EC4271);
			mm.addFunction(sceKernelDcacheWritebackAllFunction, 0x79D1C3FA);
			mm.addFunction(sceKernelDcacheWritebackInvalidateAllFunction, 0xB435DEC5);
			mm.addFunction(sceKernelDcacheInvalidateAllFunction, 0x864A9D72);
			mm.addFunction(sceKernelDcacheWritebackRangeFunction, 0x3EE30821);
			mm.addFunction(sceKernelDcacheWritebackInvalidateRangeFunction, 0x34B9FA9E);
			mm.addFunction(sceKernelDcacheInvalidateRangeFunction, 0xBFA98062);
			mm.addFunction(sceKernelDcacheProbeFunction, 0x80001C4C);
			mm.addFunction(sceKernelDcacheReadTagFunction, 0x16641D70);
			mm.addFunction(sceKernelIcacheInvalidateAllFunction, 0x920F104A);
			mm.addFunction(sceKernelIcacheInvalidateRangeFunction, 0xC2DF770E);
			mm.addFunction(sceKernelIcacheProbeFunction, 0x4FD31C9D);
			mm.addFunction(sceKernelIcacheReadTagFunction, 0xFB05FAD0);
			mm.addFunction(sceKernelGzipDecompressFunction, 0x78934841);
			mm.addFunction(sceKernelGzipIsValidFunction, 0xE0CE3E29);
			mm.addFunction(sceKernelGzipGetInfoFunction, 0xB0E9C31F);
			mm.addFunction(sceKernelGzipGetNameFunction, 0xE0E6BA96);
			mm.addFunction(sceKernelGzipGetCommentFunction, 0x8C1FBE04);
			mm.addFunction(sceKernelGzipGetCompressedDataFunction, 0x23FFC828);
			mm.addFunction(sceKernelDeflateDecompressFunction, 0xE8DB3CE6);
			
		}
	}
	
	@Override
	public void uninstallModule(HLEModuleManager mm, int version) {
		if (version >= 150) {
		
			mm.removeFunction(sceUtilsKernelDcacheWBinvRangeFunction);
			mm.removeFunction(sceKernelUtilsMd5DigestFunction);
			mm.removeFunction(sceKernelUtilsMd5BlockInitFunction);
			mm.removeFunction(sceKernelUtilsMd5BlockUpdateFunction);
			mm.removeFunction(sceKernelUtilsMd5BlockResultFunction);
			mm.removeFunction(sceKernelUtilsSha1DigestFunction);
			mm.removeFunction(sceKernelUtilsSha1BlockInitFunction);
			mm.removeFunction(sceKernelUtilsSha1BlockUpdateFunction);
			mm.removeFunction(sceKernelUtilsSha1BlockResultFunction);
			mm.removeFunction(sceKernelUtilsMt19937InitFunction);
			mm.removeFunction(sceKernelUtilsMt19937UIntFunction);
			mm.removeFunction(sceKernelSetGPIMaskFunction);
			mm.removeFunction(sceKernelSetGPOMaskFunction);
			mm.removeFunction(sceKernelGetGPIFunction);
			mm.removeFunction(sceKernelSetGPOFunction);
			mm.removeFunction(sceKernelRegisterLibcRtcFuncFunction);
			mm.removeFunction(sceKernelReleaseLibcRtcFuncFunction);
			mm.removeFunction(sceKernelLibcClockFunction);
			mm.removeFunction(sceKernelLibcTimeFunction);
			mm.removeFunction(sceKernelLibcGettimeofdayFunction);
			mm.removeFunction(sceKernelDcacheWritebackAllFunction);
			mm.removeFunction(sceKernelDcacheWritebackInvalidateAllFunction);
			mm.removeFunction(sceKernelDcacheInvalidateAllFunction);
			mm.removeFunction(sceKernelDcacheWritebackRangeFunction);
			mm.removeFunction(sceKernelDcacheWritebackInvalidateRangeFunction);
			mm.removeFunction(sceKernelDcacheInvalidateRangeFunction);
			mm.removeFunction(sceKernelDcacheProbeFunction);
			mm.removeFunction(sceKernelDcacheReadTagFunction);
			mm.removeFunction(sceKernelIcacheInvalidateAllFunction);
			mm.removeFunction(sceKernelIcacheInvalidateRangeFunction);
			mm.removeFunction(sceKernelIcacheProbeFunction);
			mm.removeFunction(sceKernelIcacheReadTagFunction);
			mm.removeFunction(sceKernelGzipDecompressFunction);
			mm.removeFunction(sceKernelGzipIsValidFunction);
			mm.removeFunction(sceKernelGzipGetInfoFunction);
			mm.removeFunction(sceKernelGzipGetNameFunction);
			mm.removeFunction(sceKernelGzipGetCommentFunction);
			mm.removeFunction(sceKernelGzipGetCompressedDataFunction);
			mm.removeFunction(sceKernelDeflateDecompressFunction);
			
		}
	}
	
	
	public void sceUtilsKernelDcacheWBinvRange(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceUtilsKernelDcacheWBinvRange [0x80FE032E]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelUtilsMd5Digest(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelUtilsMd5Digest [0xC8186A58]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelUtilsMd5BlockInit(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelUtilsMd5BlockInit [0x9E5C5086]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelUtilsMd5BlockUpdate(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelUtilsMd5BlockUpdate [0x61E1E525]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelUtilsMd5BlockResult(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelUtilsMd5BlockResult [0xB8D24E78]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelUtilsSha1Digest(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelUtilsSha1Digest [0x840259F1]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelUtilsSha1BlockInit(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelUtilsSha1BlockInit [0xF8FCD5BA]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelUtilsSha1BlockUpdate(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelUtilsSha1BlockUpdate [0x346F6DA8]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelUtilsSha1BlockResult(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelUtilsSha1BlockResult [0x585F1C09]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelUtilsMt19937Init(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelUtilsMt19937Init [0xE860E75E]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelUtilsMt19937UInt(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelUtilsMt19937UInt [0x06FB8A63]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelSetGPIMask(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelSetGPIMask [0x193D4036]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelSetGPOMask(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelSetGPOMask [0x95035FEF]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelGetGPI(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelGetGPI [0x37FB5C42]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelSetGPO(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelSetGPO [0x6AD345D7]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelRegisterLibcRtcFunc(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelRegisterLibcRtcFunc [0x7B7ED3FD]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelReleaseLibcRtcFunc(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelReleaseLibcRtcFunc [0x6151A7C3]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelLibcClock(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelLibcClock [0x91E4F6A7]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelLibcTime(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelLibcTime [0x27CC57F0]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelLibcGettimeofday(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelLibcGettimeofday [0x71EC4271]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelDcacheWritebackAll(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelDcacheWritebackAll [0x79D1C3FA]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelDcacheWritebackInvalidateAll(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelDcacheWritebackInvalidateAll [0xB435DEC5]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelDcacheInvalidateAll(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelDcacheInvalidateAll [0x864A9D72]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelDcacheWritebackRange(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelDcacheWritebackRange [0x3EE30821]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelDcacheWritebackInvalidateRange(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelDcacheWritebackInvalidateRange [0x34B9FA9E]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelDcacheInvalidateRange(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelDcacheInvalidateRange [0xBFA98062]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelDcacheProbe(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelDcacheProbe [0x80001C4C]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelDcacheReadTag(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelDcacheReadTag [0x16641D70]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelIcacheInvalidateAll(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelIcacheInvalidateAll [0x920F104A]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelIcacheInvalidateRange(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelIcacheInvalidateRange [0xC2DF770E]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelIcacheProbe(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelIcacheProbe [0x4FD31C9D]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelIcacheReadTag(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelIcacheReadTag [0xFB05FAD0]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelGzipDecompress(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelGzipDecompress [0x78934841]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelGzipIsValid(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelGzipIsValid [0xE0CE3E29]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelGzipGetInfo(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelGzipGetInfo [0xB0E9C31F]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelGzipGetName(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelGzipGetName [0xE0E6BA96]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelGzipGetComment(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelGzipGetComment [0x8C1FBE04]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelGzipGetCompressedData(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelGzipGetCompressedData [0x23FFC828]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelDeflateDecompress(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelDeflateDecompress [0xE8DB3CE6]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public final HLEModuleFunction sceUtilsKernelDcacheWBinvRangeFunction = new HLEModuleFunction("UtilsForKernel", "sceUtilsKernelDcacheWBinvRange") {
		@Override
		public final void execute(Processor processor) {
			sceUtilsKernelDcacheWBinvRange(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForKernelModule.sceUtilsKernelDcacheWBinvRange(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelUtilsMd5DigestFunction = new HLEModuleFunction("UtilsForKernel", "sceKernelUtilsMd5Digest") {
		@Override
		public final void execute(Processor processor) {
			sceKernelUtilsMd5Digest(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForKernelModule.sceKernelUtilsMd5Digest(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelUtilsMd5BlockInitFunction = new HLEModuleFunction("UtilsForKernel", "sceKernelUtilsMd5BlockInit") {
		@Override
		public final void execute(Processor processor) {
			sceKernelUtilsMd5BlockInit(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForKernelModule.sceKernelUtilsMd5BlockInit(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelUtilsMd5BlockUpdateFunction = new HLEModuleFunction("UtilsForKernel", "sceKernelUtilsMd5BlockUpdate") {
		@Override
		public final void execute(Processor processor) {
			sceKernelUtilsMd5BlockUpdate(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForKernelModule.sceKernelUtilsMd5BlockUpdate(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelUtilsMd5BlockResultFunction = new HLEModuleFunction("UtilsForKernel", "sceKernelUtilsMd5BlockResult") {
		@Override
		public final void execute(Processor processor) {
			sceKernelUtilsMd5BlockResult(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForKernelModule.sceKernelUtilsMd5BlockResult(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelUtilsSha1DigestFunction = new HLEModuleFunction("UtilsForKernel", "sceKernelUtilsSha1Digest") {
		@Override
		public final void execute(Processor processor) {
			sceKernelUtilsSha1Digest(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForKernelModule.sceKernelUtilsSha1Digest(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelUtilsSha1BlockInitFunction = new HLEModuleFunction("UtilsForKernel", "sceKernelUtilsSha1BlockInit") {
		@Override
		public final void execute(Processor processor) {
			sceKernelUtilsSha1BlockInit(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForKernelModule.sceKernelUtilsSha1BlockInit(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelUtilsSha1BlockUpdateFunction = new HLEModuleFunction("UtilsForKernel", "sceKernelUtilsSha1BlockUpdate") {
		@Override
		public final void execute(Processor processor) {
			sceKernelUtilsSha1BlockUpdate(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForKernelModule.sceKernelUtilsSha1BlockUpdate(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelUtilsSha1BlockResultFunction = new HLEModuleFunction("UtilsForKernel", "sceKernelUtilsSha1BlockResult") {
		@Override
		public final void execute(Processor processor) {
			sceKernelUtilsSha1BlockResult(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForKernelModule.sceKernelUtilsSha1BlockResult(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelUtilsMt19937InitFunction = new HLEModuleFunction("UtilsForKernel", "sceKernelUtilsMt19937Init") {
		@Override
		public final void execute(Processor processor) {
			sceKernelUtilsMt19937Init(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForKernelModule.sceKernelUtilsMt19937Init(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelUtilsMt19937UIntFunction = new HLEModuleFunction("UtilsForKernel", "sceKernelUtilsMt19937UInt") {
		@Override
		public final void execute(Processor processor) {
			sceKernelUtilsMt19937UInt(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForKernelModule.sceKernelUtilsMt19937UInt(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelSetGPIMaskFunction = new HLEModuleFunction("UtilsForKernel", "sceKernelSetGPIMask") {
		@Override
		public final void execute(Processor processor) {
			sceKernelSetGPIMask(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForKernelModule.sceKernelSetGPIMask(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelSetGPOMaskFunction = new HLEModuleFunction("UtilsForKernel", "sceKernelSetGPOMask") {
		@Override
		public final void execute(Processor processor) {
			sceKernelSetGPOMask(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForKernelModule.sceKernelSetGPOMask(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelGetGPIFunction = new HLEModuleFunction("UtilsForKernel", "sceKernelGetGPI") {
		@Override
		public final void execute(Processor processor) {
			sceKernelGetGPI(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForKernelModule.sceKernelGetGPI(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelSetGPOFunction = new HLEModuleFunction("UtilsForKernel", "sceKernelSetGPO") {
		@Override
		public final void execute(Processor processor) {
			sceKernelSetGPO(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForKernelModule.sceKernelSetGPO(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelRegisterLibcRtcFuncFunction = new HLEModuleFunction("UtilsForKernel", "sceKernelRegisterLibcRtcFunc") {
		@Override
		public final void execute(Processor processor) {
			sceKernelRegisterLibcRtcFunc(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForKernelModule.sceKernelRegisterLibcRtcFunc(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelReleaseLibcRtcFuncFunction = new HLEModuleFunction("UtilsForKernel", "sceKernelReleaseLibcRtcFunc") {
		@Override
		public final void execute(Processor processor) {
			sceKernelReleaseLibcRtcFunc(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForKernelModule.sceKernelReleaseLibcRtcFunc(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelLibcClockFunction = new HLEModuleFunction("UtilsForKernel", "sceKernelLibcClock") {
		@Override
		public final void execute(Processor processor) {
			sceKernelLibcClock(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForKernelModule.sceKernelLibcClock(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelLibcTimeFunction = new HLEModuleFunction("UtilsForKernel", "sceKernelLibcTime") {
		@Override
		public final void execute(Processor processor) {
			sceKernelLibcTime(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForKernelModule.sceKernelLibcTime(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelLibcGettimeofdayFunction = new HLEModuleFunction("UtilsForKernel", "sceKernelLibcGettimeofday") {
		@Override
		public final void execute(Processor processor) {
			sceKernelLibcGettimeofday(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForKernelModule.sceKernelLibcGettimeofday(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelDcacheWritebackAllFunction = new HLEModuleFunction("UtilsForKernel", "sceKernelDcacheWritebackAll") {
		@Override
		public final void execute(Processor processor) {
			sceKernelDcacheWritebackAll(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForKernelModule.sceKernelDcacheWritebackAll(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelDcacheWritebackInvalidateAllFunction = new HLEModuleFunction("UtilsForKernel", "sceKernelDcacheWritebackInvalidateAll") {
		@Override
		public final void execute(Processor processor) {
			sceKernelDcacheWritebackInvalidateAll(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForKernelModule.sceKernelDcacheWritebackInvalidateAll(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelDcacheInvalidateAllFunction = new HLEModuleFunction("UtilsForKernel", "sceKernelDcacheInvalidateAll") {
		@Override
		public final void execute(Processor processor) {
			sceKernelDcacheInvalidateAll(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForKernelModule.sceKernelDcacheInvalidateAll(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelDcacheWritebackRangeFunction = new HLEModuleFunction("UtilsForKernel", "sceKernelDcacheWritebackRange") {
		@Override
		public final void execute(Processor processor) {
			sceKernelDcacheWritebackRange(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForKernelModule.sceKernelDcacheWritebackRange(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelDcacheWritebackInvalidateRangeFunction = new HLEModuleFunction("UtilsForKernel", "sceKernelDcacheWritebackInvalidateRange") {
		@Override
		public final void execute(Processor processor) {
			sceKernelDcacheWritebackInvalidateRange(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForKernelModule.sceKernelDcacheWritebackInvalidateRange(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelDcacheInvalidateRangeFunction = new HLEModuleFunction("UtilsForKernel", "sceKernelDcacheInvalidateRange") {
		@Override
		public final void execute(Processor processor) {
			sceKernelDcacheInvalidateRange(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForKernelModule.sceKernelDcacheInvalidateRange(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelDcacheProbeFunction = new HLEModuleFunction("UtilsForKernel", "sceKernelDcacheProbe") {
		@Override
		public final void execute(Processor processor) {
			sceKernelDcacheProbe(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForKernelModule.sceKernelDcacheProbe(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelDcacheReadTagFunction = new HLEModuleFunction("UtilsForKernel", "sceKernelDcacheReadTag") {
		@Override
		public final void execute(Processor processor) {
			sceKernelDcacheReadTag(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForKernelModule.sceKernelDcacheReadTag(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelIcacheInvalidateAllFunction = new HLEModuleFunction("UtilsForKernel", "sceKernelIcacheInvalidateAll") {
		@Override
		public final void execute(Processor processor) {
			sceKernelIcacheInvalidateAll(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForKernelModule.sceKernelIcacheInvalidateAll(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelIcacheInvalidateRangeFunction = new HLEModuleFunction("UtilsForKernel", "sceKernelIcacheInvalidateRange") {
		@Override
		public final void execute(Processor processor) {
			sceKernelIcacheInvalidateRange(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForKernelModule.sceKernelIcacheInvalidateRange(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelIcacheProbeFunction = new HLEModuleFunction("UtilsForKernel", "sceKernelIcacheProbe") {
		@Override
		public final void execute(Processor processor) {
			sceKernelIcacheProbe(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForKernelModule.sceKernelIcacheProbe(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelIcacheReadTagFunction = new HLEModuleFunction("UtilsForKernel", "sceKernelIcacheReadTag") {
		@Override
		public final void execute(Processor processor) {
			sceKernelIcacheReadTag(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForKernelModule.sceKernelIcacheReadTag(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelGzipDecompressFunction = new HLEModuleFunction("UtilsForKernel", "sceKernelGzipDecompress") {
		@Override
		public final void execute(Processor processor) {
			sceKernelGzipDecompress(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForKernelModule.sceKernelGzipDecompress(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelGzipIsValidFunction = new HLEModuleFunction("UtilsForKernel", "sceKernelGzipIsValid") {
		@Override
		public final void execute(Processor processor) {
			sceKernelGzipIsValid(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForKernelModule.sceKernelGzipIsValid(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelGzipGetInfoFunction = new HLEModuleFunction("UtilsForKernel", "sceKernelGzipGetInfo") {
		@Override
		public final void execute(Processor processor) {
			sceKernelGzipGetInfo(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForKernelModule.sceKernelGzipGetInfo(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelGzipGetNameFunction = new HLEModuleFunction("UtilsForKernel", "sceKernelGzipGetName") {
		@Override
		public final void execute(Processor processor) {
			sceKernelGzipGetName(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForKernelModule.sceKernelGzipGetName(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelGzipGetCommentFunction = new HLEModuleFunction("UtilsForKernel", "sceKernelGzipGetComment") {
		@Override
		public final void execute(Processor processor) {
			sceKernelGzipGetComment(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForKernelModule.sceKernelGzipGetComment(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelGzipGetCompressedDataFunction = new HLEModuleFunction("UtilsForKernel", "sceKernelGzipGetCompressedData") {
		@Override
		public final void execute(Processor processor) {
			sceKernelGzipGetCompressedData(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForKernelModule.sceKernelGzipGetCompressedData(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelDeflateDecompressFunction = new HLEModuleFunction("UtilsForKernel", "sceKernelDeflateDecompress") {
		@Override
		public final void execute(Processor processor) {
			sceKernelDeflateDecompress(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForKernelModule.sceKernelDeflateDecompress(processor);";
		}
	};
    
};