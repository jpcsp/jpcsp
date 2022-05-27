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

import jpcsp.Allegrex.compiler.nativeCode.AbstractNativeCodeSequence;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.MemoryWriter;

/**
 * @author gid15
 *
 */
public class BinkCodec extends AbstractNativeCodeSequence {

	// See ffmpeg - binkidtc.c - ff_bink_idct_add_c
	static public void binkIdctAdd(int contextAddr1, int contextAddr2) {
		int contextAddr = getRelocatedAddress(contextAddr1, contextAddr2);
		int dstAddr = getGprA0();
		int dstStep = getGprA1();
		int blockAddr = getGprA2();
		int blockOffset = getGprA3();
		int srcAddr = getGprT0();
		int srcStep = getGprT1();

		final short[] block = new short[64];
		IMemoryReader sourceReader = MemoryReader.getMemoryReader(blockAddr, 128, 2);
		for (int i = 0; i < 64; i++) {
			block[i] = (short) sourceReader.readNext();
		}

		final int[] context = new int[64];
		contextAddr += blockOffset << 8;
		IMemoryReader contextReader = MemoryReader.getMemoryReader(contextAddr, 256, 4);
		for (int i = 0; i < 64; i++) {
			context[i] = contextReader.readNext();
		}

		final int[] temp = new int[64];
		for (int i = 0; i < 8; i++) {
			// See ffmpeg - binkidtc.c - bink_idct_col
			if (block[i+8] == 0 && block[i+16] == 0 && block[i+24] == 0 && block[i+32] == 0 && block[i+40] == 0 && block[i+48] == 0 && block[i+56] == 0) {
				int src0 = (block[i] * context[i]) >> 11;
				temp[i] = src0;
				temp[i+8] = src0;
				temp[i+16] = src0;
				temp[i+24] = src0;
				temp[i+32] = src0;
				temp[i+40] = src0;
				temp[i+48] = src0;
				temp[i+56] = src0;
			} else {
				int src0 = (block[i] * context[i]) >> 11;
				int src1 = (block[i+8] * context[i+8]) >> 11;
				int src2 = (block[i+16] * context[i+16]) >> 11;
				int src6 = (block[i+48] * context[i+48]) >> 11;
				int src3 = (block[i+24] * context[i+24]) >> 11;
				int src4 = (block[i+32] * context[i+32]) >> 11;
				int src5 = (block[i+40] * context[i+40]) >> 11;
				int src7 = (block[i+56] * context[i+56]) >> 11;
				int a0 = src0 + src4;
				int a1 = src0 - src4;
				int a2 = src2 + src6;
				int a3 = (2896 * (src2 - src6)) >> 11;
				int a4 = src5 + src3;
				int a5 = src5 - src3;
				int a6 = src1 + src7;
				int a7 = src1 - src7;
				int b0 = a6 + a4;
				int b1 = ((a5 + a7) * 3784) >> 11;
				int b2 = ((a5 * -5352) >> 11) - b0 + b1;
				int b3 = (((a6 - a4) * 2896) >> 11) - b2;
				int b4 = ((a7 * 2217) >> 11) + b3 - b1;
				temp[i] = a0 + a2 + b0;
				temp[i+8] = a1 + a3 - a2 + b2;
				temp[i+16] = a1 - a3 + a2 + b3;
				temp[i+24] = a0 - a2 - b4;
				temp[i+32] = a0 - a2 + b4;
				temp[i+40] = a1 - a3 + a2 - b3;
				temp[i+48] = a1 + a3 - a2 - b2;
				temp[i+56] = a0 + a2 - b0;
			}
		}

		final int[] src = new int[8];
		final int[] dst = new int[8];
		IMemoryReader srcReader = MemoryReader.getMemoryReader(srcAddr, 8 * srcStep, 1);
		IMemoryWriter dstWriter = MemoryWriter.getMemoryWriter(dstAddr, 8 * dstStep, 1);
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				src[j] = srcReader.readNext();
			}
			srcReader.skip(srcStep - 8);

			int n = i * 8;
			int src0 = temp[n];
			int src1 = temp[n+1];
			int src2 = temp[n+2];
			int src3 = temp[n+3];
			int src4 = temp[n+4];
			int src5 = temp[n+5];
			int src6 = temp[n+6];
			int src7 = temp[n+7];
			int a0 = src0 + src4;
			int a1 = src0 - src4;
			int a2 = src2 + src6;
			int a3 = (2896 * (src2 - src6)) >> 11;
			int a4 = src5 + src3;
			int a5 = src5 - src3;
			int a6 = src1 + src7;
			int a7 = src1 - src7;
			int b0 = a6 + a4;
			int b1 = ((a5 + a7) * 3784) >> 11;
			int b2 = ((a5 * -5352) >> 11) - b0 + b1;
			int b3 = (((a6 - a4) * 2896) >> 11) - b2;
			int b4 = ((a7 * 2217) >> 11) + b3 - b1;
			dst[0] = src[0] + (((a0 + a2 + b0) + 0x7F) >> 8);
			dst[1] = src[1] + (((a1 + a3 - a2 + b2) + 0x7F) >> 8);
			dst[2] = src[2] + (((a1 - a3 + a2 + b3) + 0x7F) >> 8);
			dst[3] = src[3] + (((a0 - a2 - b4) + 0x7F) >> 8);
			dst[4] = src[4] + (((a0 - a2 + b4) + 0x7F) >> 8);
			dst[5] = src[5] + (((a1 - a3 + a2 - b3) + 0x7F) >> 8);
			dst[6] = src[6] + (((a1 + a3 - a2 - b2) + 0x7F) >> 8);
			dst[7] = src[7] + (((a0 + a2 - b0) + 0x7F) >> 8);

			for (int j = 0; j < 8; j++) {
				dstWriter.writeNext(dst[j]);
			}
			dstWriter.flush();
			dstWriter.skip(dstStep - 8);
		}
	}
}
