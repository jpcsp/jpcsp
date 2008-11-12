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
import jpcsp.util.Utilities;

public abstract class pspAbstractMemoryMappedStructure {
    private final static int unknown = 0x11111111;

    private int baseAddress;
    private int maxSize = Integer.MAX_VALUE;
    private int offset;
    protected Memory mem;

    public abstract int sizeof();
    protected abstract void read();
    protected abstract void write();

    private void start(Memory mem) {
        this.mem = mem;
        offset = 0;
    }

    private void start(Memory mem, int address) {
        start(mem);
        this.baseAddress = address;
    }

    private void start(Memory mem, int address, int maxSize) {
        start(mem, address);
        this.maxSize = maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public void read(Memory mem, int address) {
        start(mem, address);
        read();
    }

    public void write(Memory mem, int address) {
        start(mem, address);
        write();
    }

    public void write(Memory mem) {
        start(mem);
        write();
    }

    protected int read8() {
        int value;
        if (offset >= maxSize) {
            value = 0;
        } else {
            value = mem.read8(baseAddress + offset);
        }
        offset += 1;

        return value;
    }

    protected int read16() {
        int value;
        if (offset >= maxSize) {
            value = 0;
        } else {
            value = mem.read16(baseAddress + offset);
        }
        offset += 2;

        return value;
    }

    protected int read32() {
        int value;
        if (offset >= maxSize) {
            value = 0;
        } else {
            value = mem.read32(baseAddress + offset);
        }
        offset += 4;

        return value;
    }

    protected void readUnknown(int length) {
        offset += length;
    }

    protected String readStringNZ(int n) {
        String s;
        if (offset >= maxSize) {
            s = null;
        } else {
            s = Utilities.readStringNZ(mem, baseAddress + offset, n);
        }
        offset += n;

        return s;
    }

    protected void read(pspAbstractMemoryMappedStructure object) {
        if (object == null) {
            return;
        }

        if (offset < maxSize) {
            object.start(mem, baseAddress + offset, maxSize - offset);
            object.read();
        }
        offset += object.sizeof();
    }

    protected void write8(byte data) {
        if (offset < maxSize) {
            mem.write8(baseAddress + offset, data);
        }
        offset += 1;
    }

    protected void write16(short data) {
        if (offset < maxSize) {
            mem.write16(baseAddress + offset, data);
        }
        offset += 2;
    }

    protected void write32(int data) {
        if (offset < maxSize) {
            mem.write32(baseAddress + offset, data);
        }
        offset += 4;
    }

    protected void writeUnknown(int length) {
        for (int i = 0; i < length; i++) {
            write8((byte) unknown);
        }
    }

    protected void writeStringNZ(int n, String s) {
        if (offset < maxSize) {
            Utilities.writeStringNZ(mem, baseAddress + offset, n, s);
        }
        offset += n;
    }

    protected void write(pspAbstractMemoryMappedStructure object) {
        if (object == null) {
            return;
        }

        if (offset < maxSize) {
            object.start(mem, baseAddress + offset, maxSize - offset);
            object.write();
        }
        offset += object.sizeof();
    }
}
