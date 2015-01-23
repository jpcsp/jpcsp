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

public class SceMp4DecodeInfo extends pspAbstractMemoryMappedStructure {
	public int sample;
	public int sampleSize;
	public int sampleOffset;
	public int unknown1;
	public int frameDuration;
	public int unknown2;
	public int timestamp1;
	public int timestamp2;

	@Override
	protected void read() {
		sample = read32();
		sampleSize = read32();
		sampleOffset = read32();
		unknown1 = read32();
		frameDuration = read32();
		unknown2 = read32();
		readUnknown(4); // Always 0
		timestamp1 = read32();
		readUnknown(4); // Always 0
		timestamp2 = read32();
	}

	@Override
	protected void write() {
		write32(sample);
		write32(sampleSize);
		write32(sampleOffset);
		write32(unknown1);
		write32(frameDuration);
		write32(unknown2);
		write32(0); // Always 0
		write32(timestamp1);
		write32(0); // Always 0
		write32(timestamp2);
	}

	@Override
	public int sizeof() {
		return 40;
	}

	@Override
	public String toString() {
		return String.format("SceMp4DecodeInfo sample=0x%X, sampleSize=0x%X, sampleOffset=0x%X, unknown1=0x%X, frameDuration=0x%X, unknown2=0x%X, timestamp1=0x%X, timestamp2=0x%X", sample, sampleSize, sampleOffset, unknown1, frameDuration, unknown2, timestamp1, timestamp2);
	}
}
