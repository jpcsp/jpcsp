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

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;

import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.MemoryWriter;

import org.apache.log4j.Logger;

public class sceDeflt extends HLEModule {
    private static Logger log = Modules.getLogger("sceDeflt");

	@Override
	public String getName() { return "sceDeflt"; }

    @HLEUnimplemented
	@HLEFunction(nid = 0x2EE39A64, version = 150)
	public int sceZlibAdler32() {
		return 0;
	}
    
    @HLEUnimplemented
	@HLEFunction(nid = 0x44054E03, version = 150)
	public int sceDeflateDecompress() {
		return 0;
	}
    
    @HLEUnimplemented
	@HLEFunction(nid = 0x6DBCF897, version = 150)
	public int sceGzipDecompress() {
		return 0;
	}
    
    @HLEUnimplemented
	@HLEFunction(nid = 0xB767F9A0, version = 150)
	public int sceGzipGetComment() {
		return 0;
	}
    
    @HLEUnimplemented
	@HLEFunction(nid = 0x0BA3B9CC, version = 150)
	public int sceGzipGetCompressedData() {
		return 0;
	}
    
    @HLEUnimplemented
	@HLEFunction(nid = 0x8AA82C92, version = 150)
	public int sceGzipGetInfo() {
		return 0;
	}
    
    @HLEUnimplemented
	@HLEFunction(nid = 0x106A3552, version = 150)
	public int sceGzipGetName() {
		return 0;
	}
    
    @HLEUnimplemented
	@HLEFunction(nid = 0x1B5B82BC, version = 150)
	public int sceGzipIsValid() {
		return 0;
	}
    
	@HLEFunction(nid = 0xA9E4FB28, version = 150)
	public int sceZlibDecompress(TPointer outBufferAddr, int outBufferLength, TPointer inBufferAddr, @CanBeNull TPointer32 crc32Addr) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("sceZlibDecompress outBufferAddr=%d, outBufferLength=0x%X, inBufferAddr=%s, crc32Addr=%s", outBufferAddr, outBufferLength, inBufferAddr, crc32Addr));
		}

		byte inBuffer[] = new byte[4096];
		byte outBuffer[] = new byte[4096];
		int inBufferPtr = 0;
		IMemoryReader reader = MemoryReader.getMemoryReader(inBufferAddr.getAddress(), 1);
		IMemoryWriter writer = MemoryWriter.getMemoryWriter(outBufferAddr.getAddress(), outBufferLength, 1);
		CRC32 crc32 = new CRC32();
		Inflater inflater = new Inflater();
		
		while (!inflater.finished()) {
			if (inflater.needsInput()) {
				for (inBufferPtr = 0; inBufferPtr < inBuffer.length; ++inBufferPtr) {
					inBuffer[inBufferPtr] = (byte) reader.readNext();
				}
				inflater.setInput(inBuffer);
			}
			
			try {
				int count = inflater.inflate(outBuffer);
				
				if (inflater.getTotalOut() > outBufferLength) {
					log.warn(String.format("sceZlibDecompress : zlib decompress buffer too small inBuffer=%s, outLength=%d", inBufferAddr, outBufferLength));
					return SceKernelErrors.ERROR_INVALID_SIZE;
				}
				crc32.update(outBuffer, 0, count);
				for (int i = 0; i < count; ++i) {
					writer.writeNext(outBuffer[i]);
				}
			} catch (DataFormatException e) {
				log.warn(String.format("sceZlibDecompress : malformed zlib stream inBuffer=%s", inBufferAddr));
				return SceKernelErrors.ERROR_INVALID_FORMAT;
			}
		}
		writer.flush();
		
		crc32Addr.setValue((int) crc32.getValue());

		return inflater.getTotalOut();
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x6A548477, version = 150)
	public int sceZlibGetCompressedData() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xAFE01FD3, version = 150)
	public int sceZlibGetInfo() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xE46EB986, version = 150)
	public int sceZlibIsValid() {
		return 0;
	}
}