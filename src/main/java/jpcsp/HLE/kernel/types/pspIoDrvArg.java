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

public class pspIoDrvArg extends pspAbstractMemoryMappedStructure {
	public int drvAddr;
	public pspIoDrv drv;
	public int arg;

	@Override
	protected void read() {
		drvAddr = read32();
		arg = read32();

		if (drvAddr == 0) {
			drv = null;
		} else {
			drv = new pspIoDrv();
			drv.read(mem, drvAddr);
		}
	}

	@Override
	protected void write() {
		write32(drvAddr);
		write32(arg);
	}

	@Override
	public int sizeof() {
		return 8;
	}

	@Override
	public String toString() {
		return String.format("drv=0x%08X(%s), arg=0x%08X", drvAddr, drv, arg);
	}
}
