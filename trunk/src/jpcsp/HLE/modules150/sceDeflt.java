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

import jpcsp.HLE.HLEFunction;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.MemoryWriter;

import org.apache.log4j.Logger;

public class sceDeflt implements HLEModule {
    private static Logger log = Modules.getLogger("sceDeflt");

	@Override
	public String getName() { return "sceDeflt"; }
	
	@Override
	public void installModule(HLEModuleManager mm, int version) { mm.installModuleWithAnnotations(this, version); }
	
	@Override
	public void uninstallModule(HLEModuleManager mm, int version) { mm.uninstallModuleWithAnnotations(this, version); }
		
	@HLEFunction(nid = 0x2EE39A64, version = 150)
	public void sceZlibAdler32(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceZlibAdler32 [0x2EE39A64]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	@HLEFunction(nid = 0x44054E03, version = 150)
	public void sceDeflateDecompress(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceDeflateDecompress [0x44054E03]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	@HLEFunction(nid = 0x6DBCF897, version = 150)
	public void sceGzipDecompress(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceGzipDecompress [0x6DBCF897]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	@HLEFunction(nid = 0xB767F9A0, version = 150)
	public void sceGzipGetComment(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceGzipGetComment [0xB767F9A0]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	@HLEFunction(nid = 0x0BA3B9CC, version = 150)
	public void sceGzipGetCompressedData(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceGzipGetCompressedData [0x0BA3B9CC]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	@HLEFunction(nid = 0x8AA82C92, version = 150)
	public void sceGzipGetInfo(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceGzipGetInfo [0x8AA82C92]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	@HLEFunction(nid = 0x106A3552, version = 150)
	public void sceGzipGetName(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceGzipGetName [0x106A3552]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	@HLEFunction(nid = 0x1B5B82BC, version = 150)
	public void sceGzipIsValid(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceGzipIsValid [0x1B5B82BC]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	@HLEFunction(nid = 0xA9E4FB28, version = 150)
	public void sceZlibDecompress(Processor processor) {

		CpuState cpu = processor.cpu;
		
		int outBufferAddr = cpu.gpr[4];
		int outBufferLength = cpu.gpr[5];
		int inBufferAddr = cpu.gpr[6];
		int crc32Addr = cpu.gpr[7];
		
		byte inBuffer[] = new byte[4096];
		byte outBuffer[] = new byte[4096];
		int inBufferPtr = 0;
		IMemoryReader reader = MemoryReader.getMemoryReader(inBufferAddr, 1);
		IMemoryWriter writer = MemoryWriter.getMemoryWriter(outBufferAddr, outBufferLength, 1);
		CRC32 crc32 = new CRC32();
		Inflater inflater = new Inflater();
		
		while(!inflater.finished()) {
			if(inflater.needsInput()) {
				for(inBufferPtr = 0; inBufferPtr < inBuffer.length; ++inBufferPtr)
					inBuffer[inBufferPtr] = (byte) reader.readNext();
				
				inflater.setInput(inBuffer);
			}
			
			try {
				int count = inflater.inflate(outBuffer);
				
				if(inflater.getTotalOut() > outBufferLength) {
					log.warn("sceZlibDecompress : zlib decompress buffer too small inBuffer=0x" + Integer.toHexString(inBufferAddr) + " outLength=" + outBufferLength);
					cpu.gpr[2] = SceKernelErrors.ERROR_INVALID_SIZE;
					return;
				}
				crc32.update(outBuffer, 0, count);
				for(int i = 0; i < count; ++i) {
					writer.writeNext(outBuffer[i]);
				}
			} catch (DataFormatException e) {
				log.warn("sceZlibDecompress : malformed zlib stream inBuffer=0x" + Integer.toHexString(inBufferAddr));
				cpu.gpr[2] = SceKernelErrors.ERROR_INVALID_FORMAT;
				return;
			}
		}
		writer.flush();
		
		Memory.getInstance().write32(crc32Addr, (int) crc32.getValue());

		cpu.gpr[2] = inflater.getTotalOut();
	}
    
	@HLEFunction(nid = 0x6A548477, version = 150)
	public void sceZlibGetCompressedData(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceZlibGetCompressedData [0x6A548477]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	@HLEFunction(nid = 0xAFE01FD3, version = 150)
	public void sceZlibGetInfo(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceZlibGetInfo [0xAFE01FD3]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	@HLEFunction(nid = 0xE46EB986, version = 150)
	public void sceZlibIsValid(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceZlibIsValid [0xE46EB986]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
   
}