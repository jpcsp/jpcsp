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

public class pspIoDrvFileArg extends pspAbstractMemoryMappedStructure {
	public int unknown1;
	public int fsNum;
	public int drvArgAddr;
	public pspIoDrvArg drvArg;
	public int unknown2;
	public int arg;

	@Override
	protected void read() {
		unknown1 = read32();
		fsNum = read32();
		drvArgAddr = read32();
		unknown2 = read32();
		arg = read32();

		if (drvArgAddr == 0) {
			drvArg = null;
		} else {
			drvArg = new pspIoDrvArg();
			drvArg.read(mem, drvArgAddr);
		}
	}

	@Override
	protected void write() {
		write32(unknown1);
		write32(fsNum);
		write32(drvArgAddr);
		write32(unknown2);
		write32(arg);
	}

	@Override
	public int sizeof() {
		return 20;
	}

	@Override
	public String toString() {
		return String.format("unknown1=0x%X, fsNum=0x%X, drvArg=0x%08X(%s), unknown2=0x%X, arg=0x%08X", unknown1, fsNum, drvArgAddr, drvArg, unknown2, arg);
	}
}
