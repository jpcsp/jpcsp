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
public class Sound extends AbstractNativeCodeSequence {
	public static void adjustVolume(int dstAddrReg, int srcAddrReg, int samplesReg, int volReg) {
		float vol = getFRegisterValue(volReg);
		if (vol != 1f) {
			int samples = getRegisterValue(samplesReg);
			int srcAddr = getRegisterValue(srcAddrReg);
			int dstAddr = getRegisterValue(dstAddrReg);
			IMemoryReader memoryReader = MemoryReader.getMemoryReader(srcAddr, samples << 1, 2);
			IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(dstAddr, samples << 1, 2);

			if (vol == .5f) {
				for (int i = 0; i < samples; i++) {
					int sample = memoryReader.readNext();
					sample = (sample << 16) >> 17;
					memoryWriter.writeNext(sample);
				}
			} else {
				for (int i = 0; i < samples; i++) {
					int sample = (short) memoryReader.readNext();
					sample = (int) (sample * vol);
					memoryWriter.writeNext(sample);
				}
			}
			memoryWriter.flush();
		}
	}

	public static void stereoToMono(int dstAddrReg, int srcAddrReg, int samplesReg) {
		int samples = getRegisterValue(samplesReg);
		int srcAddr = getRegisterValue(srcAddrReg);
		int dstAddr = getRegisterValue(dstAddrReg);
		IMemoryReader memoryReader = MemoryReader.getMemoryReader(srcAddr, samples << 2, 4);
		IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(dstAddr, samples << 1, 2);

		for (int i = 0; i < samples; i++) {
			int sample = memoryReader.readNext();
			memoryWriter.writeNext(sample);
		}
		memoryWriter.flush();
	}
}
