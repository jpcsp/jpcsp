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

import java.nio.charset.Charset;

import org.apache.log4j.Logger;

import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.HLEStartModule;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.MemoryWriter;

public class sceCcc extends HLEModule {
    protected static Logger log = Modules.getLogger("sceCcc");

	@Override
	public String getName() {
		return "sceCcc";
	}

	protected static final Charset charsetUTF8 = Charset.forName("UTF-8");
	protected static final Charset charsetUTF16 = Charset.forName("UTF-16LE");

	protected static byte[] addByteToArray(byte[] array, byte b) {
		byte[] newArray;
		if (array == null) {
			newArray = new byte[1];
		} else {
			newArray = new byte[array.length + 1];
			System.arraycopy(array, 0, newArray, 0, array.length);
		}
		newArray[newArray.length - 1] = b;

		return newArray;
	}

	protected static byte[] getBytesUTF16(int addr) {
		IMemoryReader memoryReader = MemoryReader.getMemoryReader(addr, 2);
		byte[] bytes = null;
		while (true) {
			int utf16 = memoryReader.readNext();
			if (utf16 == 0) {
				break;
			}
			bytes = addByteToArray(bytes, (byte) utf16);
			bytes = addByteToArray(bytes, (byte) (utf16 >> 8));
		}

		return bytes;
	}

	protected static String getStringUTF16(int addr) {
		return new String(getBytesUTF16(addr), charsetUTF16);
	}

	protected static byte[] getBytesUTF8(int addr) {
		IMemoryReader memoryReader = MemoryReader.getMemoryReader(addr, 1);
		byte[] bytes = null;
		while (true) {
			int utf8 = memoryReader.readNext();
			if (utf8 == 0) {
				break;
			}
			bytes = addByteToArray(bytes, (byte) utf8);
		}

		return bytes;
	}

	protected static String getStringUTF8(int addr) {
		return new String(getBytesUTF8(addr), charsetUTF8);
	}

	protected void writeStringBytes(byte[] bytes, int addr, int maxSize) {
		IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(addr, 1);
		if (bytes != null) {
			int length = Math.min(bytes.length, maxSize - 1);
			for (int i = 0; i < length; i++) {
				memoryWriter.writeNext(bytes[i] & 0xFF);
			}
		}

		// write trailing '\0'
		memoryWriter.writeNext(0);
		memoryWriter.flush();
	}

	@HLEFunction(nid = 0xC6A8BEE2, version = 150)
	public void sceCccDecodeUTF8(Processor processor) {
		CpuState cpu = processor.cpu;

		int srcAddrUTF8 = cpu.gpr[4];

		log.warn(String.format("Unimplemented sceCccDecodeUTF8 0x%08X", srcAddrUTF8));

		String srcString = getStringUTF8(srcAddrUTF8);
		int codePoint = srcString.codePointAt(0);
		if (log.isDebugEnabled()) {
			log.debug(String.format("sceCccDecodeUTF8 string='%s', codePoint=0x%X", srcString, codePoint));
		}

		cpu.gpr[2] = codePoint;
	}
    
	@HLEFunction(nid = 0x8406F469, version = 150)
	public void sceCccEncodeUTF16(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceCccEncodeUTF16 [0x8406F469]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	@HLEFunction(nid = 0xB4D1CBBF, version = 150)
	public void sceCccSetTable(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceCccSetTable [0xB4D1CBBF]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	@HLEFunction(nid = 0xE0CF8091, version = 150)
	public void sceCccDecodeUTF16(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceCccDecodeUTF16 [0xE0CF8091]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	@HLEFunction(nid = 0xBEB47224, version = 150)
	public void sceCccSJIStoUTF16(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceCccSJIStoUTF16 [0xBEB47224]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	@HLEFunction(nid = 0xD9392CCB, version = 150)
	public void sceCccStrlenSJIS(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceCccStrlenSJIS [0xD9392CCB]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	@HLEFunction(nid = 0xF1B73D12, version = 150)
	public void sceCccUTF16toSJIS(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceCccUTF16toSJIS [0xF1B73D12]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	@HLEFunction(nid = 0x00D1378F, version = 150)
	public void sceCccUTF8toUTF16(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceCccUTF8toUTF16 [0x00D1378F]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	@HLEFunction(nid = 0xB7D3C112, version = 150)
	public void sceCccStrlenUTF8(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceCccStrlenUTF8 [0xB7D3C112]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	@HLEFunction(nid = 0x4BDEB2A8, version = 150)
	public void sceCccStrlenUTF16(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceCccStrlenUTF16 [0x4BDEB2A8]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	@HLEFunction(nid = 0x41B724A5, version = 150)
	public void sceCccUTF16toUTF8(Processor processor) {
		CpuState cpu = processor.cpu;

		int dstAddr = cpu.gpr[4];
		int dstSize = cpu.gpr[5];
		int srcAddr = cpu.gpr[6];

		if (log.isDebugEnabled()) {
			log.debug(String.format("sceCccUTF16toUTF8 dstAddr=0x%08X, dstSize=%d, srcAddr=0x%08X", dstAddr, dstSize, srcAddr));
		}

		String dstString = getStringUTF16(srcAddr);
		if (log.isDebugEnabled()) {
			log.debug(String.format("sceCccUTF16toUTF8 string='%s'", dstString));
		}
		byte[] dstBytes = dstString.getBytes(charsetUTF8);
		writeStringBytes(dstBytes, dstAddr, dstSize);

		cpu.gpr[2] = dstBytes.length;
	}
}
