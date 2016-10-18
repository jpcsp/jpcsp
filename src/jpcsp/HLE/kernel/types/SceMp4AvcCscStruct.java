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

public class SceMp4AvcCscStruct extends pspAbstractMemoryMappedStructure {
	public int height;
	public int width;
	public int mode0;
	public int mode1;
	public int buffer0;
	public int buffer1;
	public int buffer2;
	public int buffer3;
	public int buffer4;
	public int buffer5;
	public int buffer6;
	public int buffer7;

	@Override
	protected void read() {
		height = read32();
		width = read32();
		mode0 = read32();
		mode1 = read32();
		buffer0 = read32();
		buffer1 = read32();
		buffer2 = read32();
		buffer3 = read32();
		buffer4 = read32();
		buffer5 = read32();
		buffer6 = read32();
		buffer7 = read32();
	}

	@Override
	protected void write() {
		write32(height);
		write32(width);
		write32(mode0);
		write32(mode1);
		write32(buffer0);
		write32(buffer1);
		write32(buffer2);
		write32(buffer3);
		write32(buffer4);
		write32(buffer5);
		write32(buffer6);
		write32(buffer7);
	}

	@Override
	public int sizeof() {
		return 48;
	}

	@Override
	public String toString() {
		return String.format("heigth=%d, width=%d, mode0=%d, mode1=%d, buffer0=0x%08X, buffer1=0x%08X, buffer2=0x%08X, buffer3=0x%08X, buffer4=0x%08X, buffer5=0x%08X, buffer6=0x%08X, buffer7=0x%08X", height, width, mode0, mode1, buffer0, buffer1, buffer2, buffer3, buffer4, buffer5, buffer6, buffer7);
	}
}
