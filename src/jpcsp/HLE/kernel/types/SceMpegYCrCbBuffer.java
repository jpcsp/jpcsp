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

public class SceMpegYCrCbBuffer extends pspAbstractMemoryMappedStructure {
	public int frameBufferHeight16;
	public int frameBufferWidth16;
	public int unknown1;
	public int unknown2;
	public int bufferY;
	public int bufferY2;
	public int bufferCr;
	public int bufferCb;
	public int bufferCr2;
	public int bufferCb2;
	public int frameHeight;
	public int frameWidth;
	public int frameBufferWidth;
	public int unknown3[] = new int[11];

	@Override
	protected void read() {
		frameBufferHeight16 = read32();
		frameBufferWidth16 = read32();
		unknown1 = read32();
		unknown2 = read32();
		bufferY = read32();
		bufferY2 = read32();
		bufferCr = read32();
		bufferCb = read32();
		bufferCr2 = read32();
		bufferCb2 = read32();
		frameHeight = read32();
		frameWidth = read32();
		frameBufferWidth = read32();
		read32Array(unknown3);
	}

	@Override
	protected void write() {
		write32(frameBufferHeight16);
		write32(frameBufferWidth16);
		write32(unknown1);
		write32(unknown2);
		write32(bufferY);
		write32(bufferY2);
		write32(bufferCr);
		write32(bufferCb);
		write32(bufferCr2);
		write32(bufferCb2);
		write32(frameHeight);
		write32(frameWidth);
		write32(frameBufferWidth);
		write32Array(unknown3);
	}

	@Override
	public int sizeof() {
		return 96;
	}

	@Override
	public String toString() {
		return String.format("height16=%d, width16=%d, bufferY=0x%08X, bufferY2=0x%08X, bufferCr=0x%08X, bufferCb=0x%08X, bufferCr2=0x%08X, bufferCb2=0x%08X, height=%d, width=%d, frameBufferWidth=%d", frameBufferHeight16, frameBufferWidth16, bufferY, bufferY2, bufferCr, bufferCb, bufferCr2, bufferCb2, frameHeight, frameWidth, frameBufferWidth);
	}
}
