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
import jpcsp.graphics.GeCommands;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.MemoryWriter;

/**
 * @author gid15
 *
 */
public class BoneSequence extends AbstractNativeCodeSequence {
	static public void call(int baseAddressReg, int offset1, int offset2, int offset3, int destAddressReg) {
		Memory mem = getMemory();
		int baseAddress = getRegisterValue(baseAddressReg);
		int paramAddr = mem.read32(baseAddress + offset1);
		int count = mem.read16(paramAddr + offset2);
		if (count <= 0) {
			return;
		}
		int destAddr = getRegisterValue(destAddressReg);
		int srcBaseAddr = mem.read32(baseAddress + offset3);
		final float[] src1 = new float[16];
		final float[] src2 = new float[16];
		final float[] dst = new float[16];

		int length = count * 304;
		IMemoryReader src1Reader = MemoryReader.getMemoryReader(srcBaseAddr + 64, length, 4);
		IMemoryReader src2Reader = MemoryReader.getMemoryReader(srcBaseAddr + 128, length, 4);
		IMemoryReader src3Reader = MemoryReader.getMemoryReader(srcBaseAddr + 296, length, 4);
		IMemoryWriter boneWriter = MemoryWriter.getMemoryWriter(srcBaseAddr + 192, length, 4);
		final int cmdBONE = GeCommands.BONE << 24;

		for (int i = 0; i < count; i++) {
			if ((src3Reader.readNext() & 1) != 0) {
				for (int j = 0; j < 12; j++) {
					boneWriter.writeNext(cmdBONE);
				}

				src1Reader.skip(76);
				src2Reader.skip(76);
			} else {
				for (int j = 0; j < 16; j++) {
					src1[j] = Float.intBitsToFloat(src1Reader.readNext());
					src2[j] = Float.intBitsToFloat(src2Reader.readNext());
				}

				// VMMUL
				for (int n1 = 0, j = 0, k = 0; n1 < 4; n1++, k += 4) {
					// We only need a 4x3 dst matrix because the BONE matrix is only 4x3
					for (int n2 = 0; n2 < 3; n2++, j++) {
						float dot = src1[n2] * src2[k];
						dot += src1[n2 + 4] * src2[k + 1];
						dot += src1[n2 + 8] * src2[k + 2];
						dst[j] = dot + src1[n2 + 12] * src2[k + 3];
					}
					j++;
				}

				for (int n1 = 0, j = 0; n1 < 4; n1++) {
					// The BONE matrix is only 4x3
					for (int n2 = 0; n2 < 3; n2++, j++) {
						int intBits = Float.floatToRawIntBits(dst[j]);
						boneWriter.writeNext(cmdBONE | (intBits >>> 8));
					}
					j++; // Skip one column
				}

				src1Reader.skip(60);
				src2Reader.skip(60);
			}
			src3Reader.skip(75);
			boneWriter.skip(64);
		}
		boneWriter.flush();

		// This is probably not used by the application as it is overwritten
		// at each loop and only the last loop result is left...
		for (int n1 = 0, k = 0; n1 < 4; n1++, k += 4) {
			final int n2 = 3;
			float dot = src1[n2] * src2[k];
			dot += src1[n2 + 4] * src2[k + 1];
			dot += src1[n2 + 8] * src2[k + 2];
			dst[(n1 << 2) + n2] = dot + src1[n2 + 12] * src2[k + 3];
		}
		IMemoryWriter dstWriter = MemoryWriter.getMemoryWriter(destAddr, 64, 4);
		for (int n1 = 0; n1 < 4; n1++) {
			for (int n2 = 0; n2 < 4; n2++) {
				int intBits = Float.floatToRawIntBits(dst[(n2 << 2) + n1]);
				dstWriter.writeNext(intBits);
			}
		}
		dstWriter.flush();
	}

	static public void call(int matrix1Reg, int matrix2Reg, int destReg, int countReg) {
		int matrix1Addr = getRegisterValue(matrix1Reg);
		int matrix2Addr = getRegisterValue(matrix2Reg);
		int dest = getRegisterValue(destReg);
		int count = getRegisterValue(countReg);

		if (count <= 0) {
			return;
		}

		IMemoryReader matrix1Reader = MemoryReader.getMemoryReader(matrix1Addr, 48 * count, 4);
		IMemoryReader matrix2Reader = MemoryReader.getMemoryReader(matrix2Addr, 48 * count, 4);
		IMemoryWriter destWriter = MemoryWriter.getMemoryWriter(dest, 64 * count, 4);
		final float[] matrix2 = new float[12];
		int cmdBONE = GeCommands.BONE << 24;
		int cmdRET = GeCommands.RET << 24;
		float dot, m1a, m1b, m1c;
		for (int i = 0; i < count; i++) {
			for (int j = 0; j < 12; j++) {
				matrix2[j] = Float.intBitsToFloat(matrix2Reader.readNext());
			}

			for (int n1 = 0; n1 < 3; n1++) {
				m1a = Float.intBitsToFloat(matrix1Reader.readNext());
				m1b = Float.intBitsToFloat(matrix1Reader.readNext());
				m1c = Float.intBitsToFloat(matrix1Reader.readNext());
				for (int n2 = 0; n2 < 3; n2++) {
					dot = m1a * matrix2[n2];
					dot += m1b * matrix2[n2 + 3];
					dot += m1c * matrix2[n2 + 6];
					destWriter.writeNext((Float.floatToRawIntBits(dot) >>> 8) | cmdBONE);
				}
			}

			m1a = Float.intBitsToFloat(matrix1Reader.readNext());
			m1b = Float.intBitsToFloat(matrix1Reader.readNext());
			m1c = Float.intBitsToFloat(matrix1Reader.readNext());
			for (int n2 = 0; n2 < 3; n2++) {
				dot = m1a * matrix2[n2];
				dot += m1b * matrix2[n2 + 3];
				dot += m1c * matrix2[n2 + 6];
				dot += matrix2[n2 + 9];
				destWriter.writeNext((Float.floatToRawIntBits(dot) >>> 8) | cmdBONE);
			}

			destWriter.writeNext(cmdRET);
			destWriter.skip(3);
		}
		destWriter.flush();
	}
}
