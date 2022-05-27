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

import java.util.Arrays;

import jpcsp.memory.IMemoryReader;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.MemoryWriter;

/**
 * @author gid15
 *
 */
public class Sort extends AbstractNativeCodeSequence {
	private static class Float8ObjectReverse implements Comparable<Float8ObjectReverse> {
		private int n1;
		private int n2;
		private float f;

		public Float8ObjectReverse(IMemoryReader memoryReader) {
			n1 = memoryReader.readNext();
			n2 = memoryReader.readNext();
			f = Float.intBitsToFloat(n2);
		}

		public void write(IMemoryWriter memoryWriter) {
			memoryWriter.writeNext(n1);
			memoryWriter.writeNext(n2);
		}

		@Override
		public int compareTo(Float8ObjectReverse o) {
			return Float.compare(o.f, f);
		}
	}

	public static void sortFloatArray8Reverse() {
		int addr = getGprA0();
		int size = getGprA1();

		if (size < 2) {
			return;
		}

		// Read the objects from memory
		Float8ObjectReverse[] objects = new Float8ObjectReverse[size];
		IMemoryReader memoryReader = MemoryReader.getMemoryReader(addr, size << 3, 4);
		for (int i = 0; i < size; i++) {
			objects[i] = new Float8ObjectReverse(memoryReader);
		}

		// Sort the objects
		Arrays.sort(objects);

		// Write back the objects to memory
		IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(addr, size << 3, 4);
		for (int i = 0; i < size; i++) {
			objects[i].write(memoryWriter);
		}
		memoryWriter.flush();
	}
}
