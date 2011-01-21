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
package jpcsp.Allegrex.compiler.nativeCode.graphics;

import jpcsp.Memory;
import jpcsp.Allegrex.compiler.nativeCode.AbstractNativeCodeSequence;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.MemoryWriter;

/**
 * @author gid15
 *
 */
public class GeList extends AbstractNativeCodeSequence {
	static public void updateCommands(int base, int startReg, int endReg, int offsetReg, int stepReg) {
		Memory mem = getMemory();
		int start = getRegisterValue(startReg);
		int end = getRegisterValue(endReg);
		int offset = getRegisterValue(offsetReg);
		int step = getRegisterValue(stepReg);
		int skip = (step - 4) >> 2;
		IMemoryReader baseReader = MemoryReader.getMemoryReader(getRegisterValue(base), (end - start) << 4, 4);
		for (int i = start; i < end; i++) {
			baseReader.skip(1);
			int addr = baseReader.readNext();
			int count = baseReader.readNext();
			int dest = baseReader.readNext();
			IMemoryReader addrReader = MemoryReader.getMemoryReader(addr, count << 2, 4);
			IMemoryWriter destWriter = MemoryWriter.getMemoryWriter(dest + offset, count * step, 4);
			for (int j = 0; j < count; j++) {
				int src = addrReader.readNext();
				destWriter.writeNext(mem.read32(src));
				destWriter.skip(skip);
			}
			destWriter.flush();
		}
	}
}
