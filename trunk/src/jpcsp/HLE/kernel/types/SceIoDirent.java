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

/** http://psp.jim.sh/pspsdk-doc/structSceIoDirent.html */
public class SceIoDirent extends pspAbstractMemoryMappedStructure {
    public SceIoStat stat;
    public String filename;

    public SceIoDirent(SceIoStat stat, String filename) {
        this.stat = stat;
        this.filename = filename;
    }

	@Override
	protected void read() {
		stat = new SceIoStat();
		read(stat);
		filename = readStringNZ(256);
	}

	@Override
	protected void write() {
		write(stat);
		writeStringNZ(256, filename);
        // 2 ints reserved
		writeUnknown(8);
	}

	@Override
	public int sizeof() {
		return SceIoStat.SIZEOF + 256 + 8;
	}
}
