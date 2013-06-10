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

public class SceMpegAu extends pspAbstractMemoryMappedStructure {
	// Presentation TimeStamp
	public long pts;
	// Decode TimeStamp
	public long dts;
	// Es buffer handle?
	public int esBuffer;
	// Es size?
	public int esSize;

	public SceMpegAu() {
		pts = -1;
		dts = -1;
		esBuffer = 0;
		esSize = 0;
	}

	protected long readTimeStamp() {
		int msb = read32();
		int lsb = read32();

		return (((long) msb) << 32) | (((long) lsb) & 0xFFFFFFFFL);
	}

	protected void writeTimeStamp(long ts) {
		int msb = (int) (ts >> 32);
		int lsb = (int) ts;

		write32(msb);
		write32(lsb);
	}

	@Override
	protected void read() {
		pts = readTimeStamp();
		dts = readTimeStamp();
		esBuffer = read32();
		esSize = read32();
	}

	@Override
	protected void write() {
		writeTimeStamp(pts);
		writeTimeStamp(dts);
		write32(esBuffer);
		write32(esSize);
	}

	@Override
	public int sizeof() {
		return 24;
	}

	@Override
	public String toString() {
		return String.format("pts=%d, dts=%d, esBuffer=0x%X, esSize=0x%X", pts, dts, esBuffer, esSize);
	}
}
