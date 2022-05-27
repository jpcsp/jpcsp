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

public class pspUmdInfo extends pspAbstractMemoryMappedStructureVariableLength {
	public static final int PSP_UMD_TYPE_GAME  = 0x10;
	public static final int PSP_UMD_TYPE_VIDEO = 0x20;
	public static final int PSP_UMD_TYPE_AUDIO = 0x40;
	public int type;

	@Override
	protected void read() {
		super.read();
		type = read32();
	}

	@Override
	protected void write() {
		super.write();
		write32(type);
	}

	@Override
	public String toString() {
		return String.format("%s, type=%d", super.toString(), type);
	}
}
