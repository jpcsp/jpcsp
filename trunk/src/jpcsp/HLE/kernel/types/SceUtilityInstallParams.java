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

public class SceUtilityInstallParams extends pspUtilityBaseDialog {
	public int unknown1;
	public String gameName; // DISCID
	byte[] key = new byte[0x10];

	@Override
	protected void read() {
		base = new pspUtilityDialogCommon();
		read(base);
		setMaxSize(base.totalSizeof());

		unknown1 = read32();
		gameName = readStringNZ(13);
		readUnknown(3);
		readUnknown(16);
		read8Array(key);
	}

	@Override
	protected void write() {
	    write(base);
        setMaxSize(base.totalSizeof());

	    write32(unknown1);
	    writeStringNZ(13, gameName);
	    writeUnknown(3);
	    writeUnknown(16);
	    write8Array(key);
	}

	@Override
	public int sizeof() {
		return base.totalSizeof();
	}

	@Override
	public String toString() {
		return String.format("Address 0x%08X, unknown1=%d, gameName=%s", getBaseAddress(), unknown1, gameName);
	}
}
