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

import jpcsp.memory.IMemoryReader;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.MemoryWriter;

/**
 * @author gid15
 *
 */
public class MatrixOperation1 extends AbstractNativeCodeSequence {
	static public void call() {
		int a0 = getGprA0();
		int a1 = getGprA1();

		IMemoryReader memoryReader = MemoryReader.getMemoryReader(a1, 64, 4);
		int i00 = memoryReader.readNext();
		int i01 = memoryReader.readNext();
		int i02 = memoryReader.readNext();
		memoryReader.skip(1);
		int i10 = memoryReader.readNext();
		int i11 = memoryReader.readNext();
		int i12 = memoryReader.readNext();
		memoryReader.skip(1);
		int i20 = memoryReader.readNext();
		int i21 = memoryReader.readNext();
		int i22 = memoryReader.readNext();
		memoryReader.skip(1);
		int i30 = memoryReader.readNext();
		int i31 = memoryReader.readNext();
		int i32 = memoryReader.readNext();

		float a00 = Float.intBitsToFloat(i00);
		float a01 = Float.intBitsToFloat(i01);
		float a02 = Float.intBitsToFloat(i02);
		float a10 = Float.intBitsToFloat(i10);
		float a11 = Float.intBitsToFloat(i11);
		float a12 = Float.intBitsToFloat(i12);
		float a20 = Float.intBitsToFloat(i20);
		float a21 = Float.intBitsToFloat(i21);
		float a22 = Float.intBitsToFloat(i22);
		float a30 = Float.intBitsToFloat(i30);
		float a31 = Float.intBitsToFloat(i31);
		float a32 = Float.intBitsToFloat(i32);

		IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(a0, 64, 4);
		memoryWriter.writeNext(i00);
		memoryWriter.writeNext(i10);
		memoryWriter.writeNext(i20);
		memoryWriter.writeNext(0);
		memoryWriter.writeNext(i01);
		memoryWriter.writeNext(i11);
		memoryWriter.writeNext(i21);
		memoryWriter.writeNext(0);
		memoryWriter.writeNext(i02);
		memoryWriter.writeNext(i12);
		memoryWriter.writeNext(i22);
		memoryWriter.writeNext(0);
		memoryWriter.writeNext(Float.floatToRawIntBits(-(a00 * a30 + a01 * a31 + a02 * a32)));
		memoryWriter.writeNext(Float.floatToRawIntBits(-(a10 * a30 + a11 * a31 + a12 * a32)));
		memoryWriter.writeNext(Float.floatToRawIntBits(-(a20 * a30 + a21 * a31 + a22 * a32)));
		memoryWriter.writeNext(0x3F800000);
		memoryWriter.flush();
	}
}
