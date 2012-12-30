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

/** http://psp.jim.sh/pspsdk-doc/structSceIoStat.html */
public class SceIoStat extends pspAbstractMemoryMappedStructure {
	public static final int SIZEOF = 16 + ScePspDateTime.SIZEOF * 3 + 24;
    public int mode;
    public int attr;
    public long size;
    public ScePspDateTime ctime;
    public ScePspDateTime atime;
    public ScePspDateTime mtime;
    private int[] reserved = new int[] { 0, 0, 0, 0, 0, 0 };

    public SceIoStat() {
    }

    public SceIoStat(int mode, int attr, long size, ScePspDateTime ctime, ScePspDateTime atime, ScePspDateTime mtime) {
    	init(mode, attr, size, ctime, atime, mtime);
    }

    public void init(int mode, int attr, long size, ScePspDateTime ctime, ScePspDateTime atime, ScePspDateTime mtime) {
        this.mode = mode;
        this.attr = attr;
        this.size = size;
        this.ctime = ctime;
        this.atime = atime;
        this.mtime = mtime;
    }

    public void setReserved(int index, int value) {
    	reserved[index] = value;
    }

    public int getReserved(int index) {
    	return reserved[index];
    }

    public void setStartSector(int value) {
    	setReserved(0, value);
    }

    public int getStartSector() {
    	return getReserved(0);
    }

    @Override
	protected void read() {
        mode = read32();
        attr = read32();
        size = read64();

        ctime = new ScePspDateTime();
        atime = new ScePspDateTime();
        mtime = new ScePspDateTime();
        read(ctime);
        read(atime);
        read(mtime);

        read32Array(reserved);
	}

	@Override
	protected void write() {
		write32(mode);
		write32(attr);
		write64(size);

		write(ctime);
		write(atime);
		write(mtime);

        // 6 ints reserved
		write32Array(reserved);
	}

	@Override
	public int sizeof() {
        return SIZEOF;
	}

	@Override
	public String toString() {
		return String.format("SceIoStat[mode=0x%X, attr=0x%X, size=%d, ctime=%s, atime=%s, mtime=%s]", mode, attr, size, ctime.toString(), atime.toString(), mtime.toString());
	}
}
