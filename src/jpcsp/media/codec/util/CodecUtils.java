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
package jpcsp.media.codec.util;

import static java.lang.Math.max;
import static java.lang.Math.min;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryWriter;

public class CodecUtils {
	private static int convertSampleFloatToInt16(float sample) {
		return min(max((int) (sample * 32768f + 0.5f), -32768), 32767) & 0xFFFF;
	}

	public static void writeOutput(float[][] samples, int outputAddr, int numberOfSamples, int outputChannels) {
		IMemoryWriter writer = MemoryWriter.getMemoryWriter(outputAddr, numberOfSamples * 2 * outputChannels, 2);
		switch (outputChannels) {
			case 1:
				for (int i = 0; i < numberOfSamples; i++) {
					int sample = convertSampleFloatToInt16(samples[0][i]);
					writer.writeNext(sample);
				}
				break;
			case 2:
				for (int i = 0; i < numberOfSamples; i++) {
					int lsample = convertSampleFloatToInt16(samples[0][i]);
					int rsample = convertSampleFloatToInt16(samples[1][i]);
					writer.writeNext(lsample);
					writer.writeNext(rsample);
				}
				break;
		}
		writer.flush();
	}
}
