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

public class SceKernelLMOption extends pspAbstractMemoryMappedStructureVariableLength {
	public int mpidText;
	public int mpidData;
	public int flags;
	public int position;
    public int access;
    public int creserved;

	@Override
	protected void read() {
		super.read();
		mpidText = read32();
		mpidData = read32();
		flags = read32();
		position = read8();
        access = read8();
        creserved = read16();
	}

	@Override
	protected void write() {
		super.write();
		write32(mpidText);
		write32(mpidData);
		write32(flags);
		write8((byte)position);
        write8((byte)access);
        write16((short)creserved);
	}
}