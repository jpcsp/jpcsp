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
package jpcsp.HLE.kernel.types;

import jpcsp.util.Utilities;

public class SceMp4AvcNalStruct extends pspAbstractMemoryMappedStructure {
	public int spsBuffer;
	public int spsSize;
	public int ppsBuffer;
	public int ppsSize;
	public int nalPrefixSize;
	public int nalBuffer;
	public int nalSize;
	public int mode;

	@Override
	protected void read() {
		spsBuffer = read32();
		spsSize = read32();
		ppsBuffer = read32();
		ppsSize = read32();
		nalPrefixSize = read32();
		nalBuffer = read32();
		nalSize = read32();
		mode = read32();
	}

	@Override
	protected void write() {
		write32(spsBuffer);
		write32(spsSize);
		write32(ppsBuffer);
		write32(ppsSize);
		write32(nalPrefixSize);
		write32(nalBuffer);
		write32(nalSize);
		write32(mode);
	}

	@Override
	public int sizeof() {
		return 32;
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();

		if (spsBuffer != 0 && spsSize > 0) {
			s.append(String.format("SPS Buffer: %s", Utilities.getMemoryDump(spsBuffer, spsSize)));
		}
		if (ppsBuffer != 0 && ppsSize > 0) {
			s.append(String.format(", PPS Buffer: %s", Utilities.getMemoryDump(ppsBuffer, ppsSize)));
		}
		s.append(String.format(", NAL prefix size 0x%X", nalPrefixSize));
		if (nalBuffer != 0 && nalSize > 0) {
			s.append(String.format(", NAL Buffer: %s", Utilities.getMemoryDump(nalBuffer, nalSize)));
		}
		s.append(String.format(", mode 0x%X", mode));

		return s.toString();
	}
}
