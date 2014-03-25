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

import jpcsp.Memory;
import jpcsp.HLE.Modules;
import jpcsp.util.Utilities;

/** http://psp.jim.sh/pspsdk-doc/structSceIoDirent.html */
public class SceIoDirent extends pspAbstractMemoryMappedStructure {
    public SceIoStat stat;
    public String filename;
    public int reserved;  // This field is a pointer to user data and is set manually.
    public int dummy;     // Always 0.

    public SceIoDirent(SceIoStat stat, String filename) {
        this.stat = stat;
        this.filename = filename;
    }

	@Override
	protected void read() {
		stat = new SceIoStat();
		read(stat);
		filename = readStringNZ(256);
		reserved = read32();
		dummy = read32();
	}

	// Convert a "long" file name into a 8.3 file name.
	// TODO Implement correct 8.3 truncation.
	private static String convertFileNameTo83(String fileName) {
		if (fileName == null || fileName.length() <= 12) {
			return fileName.toUpperCase();
		}
		return fileName.substring(0, 12).toUpperCase();
	}

	@Override
	protected void write() {
		write(stat);
		writeStringNZ(256, filename);
		// NOTE: Do not overwrite the reserved field.
		// Tests confirm that sceIoDread only writes the stat and filename.

		if (reserved == 0) {
			reserved = read32();
		}
		if (Memory.isAddressGood(reserved)) {
			if (Modules.SysMemUserForUserModule.hleKernelGetCompiledSdkVersion() <= 0x0307FFFF) {
				// "reserved" is pointing to an area of unknown size
				// - [0..12] "8.3" file name (null-terminated)
				// - [13..???] long file name (null-terminated)
	            Utilities.writeStringNZ(mem, reserved + 0, 13, convertFileNameTo83(filename));
	            Utilities.writeStringZ(mem, reserved + 13, filename);
			} else {
				// "reserved" is pointing to an area of total size 1044
				// - [0..3] size of area
				// - [4..19] "8.3" file name (null-terminated)
				// - [20..???] long file name (null-terminated)
				int size = mem.read32(reserved);
				if (size >= 1044) {
					Utilities.writeStringNZ(mem, reserved + 4, 13, convertFileNameTo83(filename));
					Utilities.writeStringNZ(mem, reserved + 20, 1024, filename);
				}
			}
		}
	}

	@Override
	public int sizeof() {
		return SceIoStat.SIZEOF + 256 + 8;
	}
}
