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

public class pspUmdInfo extends pspAbstractMemoryMappedStructure {
	public static final int PSP_UMD_TYPE_GAME  = 0x10;
	public static final int PSP_UMD_TYPE_VIDEO = 0x20;
	public static final int PSP_UMD_TYPE_AUDIO = 0x40;
	public int size;
	public int type;

	@Override
	protected void read() {
		size = read32();
		setMaxSize(size);
		type = read32();
	}

	@Override
	protected void write() {
		setMaxSize(size);
		write32(size);
		write32(type);
	}

	@Override
	public int sizeof() {
		return size;
	}
}
