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
import jpcsp.sound.Utils;

/**
 * @author gid15
 *
 */
public class SoundMix extends AbstractNativeCodeSequence {
	static public void mixStereoInMemory(int inAddrReg, int inOutAddrReg, int countReg, int leftVolumeFReg, int rightVolumeFReg) {
		int inAddr = getRegisterValue(inAddrReg);
		int inOutAddr = getRegisterValue(inOutAddrReg);
		int count = getRegisterValue(countReg);
		float inLeftVolume = getFRegisterValue(leftVolumeFReg);
		float inRightVolume = getFRegisterValue(rightVolumeFReg);

		Utils.mixStereoInMemory(inAddr, inOutAddr, count, inLeftVolume, inRightVolume);
	}

	static public void mixStereoInMemory(int inAddrReg, int inOutAddrReg, int countReg, int maxCountAddrReg, int leftVolumeFReg, int rightVolumeFReg) {
		int inAddr = getRegisterValue(inAddrReg);
		int inOutAddr = getRegisterValue(inOutAddrReg);
		int count = getRegisterValue(countReg);
		int maxCount = getMemory().read32(getRegisterValue(maxCountAddrReg));
		float inLeftVolume = getFRegisterValue(leftVolumeFReg);
		float inRightVolume = getFRegisterValue(rightVolumeFReg);

		Utils.mixStereoInMemory(inAddr, inOutAddr, maxCount - count, inLeftVolume, inRightVolume);
	}

	static public void mixMonoToStereo(int leftChannelAddrReg, int rightChannelAddrReg, int stereoChannelAddrReg, int lengthReg, int lengthStep) {
		int leftChannelAddr = getRegisterValue(leftChannelAddrReg);
		int rightChannelAddr = getRegisterValue(rightChannelAddrReg);
		int stereoChannelAddr = getRegisterValue(stereoChannelAddrReg);
		int length = getRegisterValue(lengthReg) * lengthStep;

		IMemoryReader leftChannelReader = MemoryReader.getMemoryReader(leftChannelAddr, length, 2);
		IMemoryReader rightChannelReader = MemoryReader.getMemoryReader(rightChannelAddr, length, 2);
		IMemoryWriter stereoChannelWriter = MemoryWriter.getMemoryWriter(stereoChannelAddr, length << 1, 2);

		for (int i = 0; i < length; i += 2) {
			int left = leftChannelReader.readNext();
			int right = rightChannelReader.readNext();
			stereoChannelWriter.writeNext(left);
			stereoChannelWriter.writeNext(right);
		}
		stereoChannelWriter.flush();
	}
}
