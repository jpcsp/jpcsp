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
package jpcsp.Allegrex.compiler.nativeCode;

import jpcsp.Memory;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.MemoryWriter;

/**
 * @author gid15
 *
 */
public class NanodesktopRenderWithBlend extends AbstractNativeCodeSequence {
	static public void call(int posPixelXreg, int maxPixelXreg, int windowsDataAddressReg, int addr1BaseReg, int addr1BaseXreg, int addr2BaseReg, int addr2BaseYreg, int addr3BaseReg) {
		Memory mem = getMemory();
		int posPixelX = getRegisterValue(posPixelXreg);
		int maxPixelX = getRegisterValue(maxPixelXreg);
		int startAddress1 = getRegisterValue(addr1BaseReg) + ((getRegisterValue(addr1BaseXreg) + posPixelX) << 1);
		int windowsDataAddress = getRegisterValue(windowsDataAddressReg);
		int startAddress2 = getRegisterValue(addr2BaseReg) + (((getRegisterValue(addr2BaseYreg) - mem.read16(74 + windowsDataAddress)) * mem.read16(84 + windowsDataAddress) + (posPixelX - mem.read16(72 + windowsDataAddress))) << 1);
		int startAddress3 = getRegisterValue(addr3BaseReg) + (posPixelX << 1);
		IMemoryReader memoryReader1 = MemoryReader.getMemoryReader(startAddress1, 2);
		IMemoryReader memoryReader2 = MemoryReader.getMemoryReader(startAddress2, 2);
		IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(startAddress3, 2);
		for (int x = posPixelX; x <= maxPixelX; x++) {
			int color1 = memoryReader1.readNext();
			int color2 = memoryReader2.readNext();
			int r = ((color1 >> 3) & 0x03) + (color2 & 0x1F);
			int g = ((color1 >> 8) & 0x03) + ((color2 >> 5) & 0x1F);
			int b = ((color1 >> 13) & 0x03) + ((color2 >> 10) & 0x1F);
			if (r > 0x1F) r = 0x1F;
			if (g > 0x1F) g = 0x1F;
			if (b > 0x1F) b = 0x1F;
			int color = (b << 10) + (g << 5) + r + 0x8000;
			memoryWriter.writeNext(color);
		}
		memoryWriter.flush();
	}
}
