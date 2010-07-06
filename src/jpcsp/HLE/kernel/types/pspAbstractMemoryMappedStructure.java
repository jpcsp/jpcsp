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

import java.nio.charset.Charset;

import jpcsp.Memory;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;
import jpcsp.util.Utilities;

public abstract class pspAbstractMemoryMappedStructure {
    private final static int unknown = 0x11111111;
	private final static Charset charset16 = Charset.forName("UTF-16LE");

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
        baseAddress = address;
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

    protected void read8Array(byte[] array) {
    	for (int i = 0; array != null && i < array.length; i++) {
    		array[i] = (byte) read8();
    	}
    }

    protected void align16() {
    	offset = (offset + 1) & ~1;
    }

    protected void align32() {
    	offset = (offset + 3) & ~3;
    }

    protected int read16() {
    	align16();

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
    	align32();

    	int value;
        if (offset >= maxSize) {
            value = 0;
        } else {
            value = mem.read32(baseAddress + offset);
        }
        offset += 4;

        return value;
    }

    protected long read64() {
    	align32();

    	long value;
        if (offset >= maxSize) {
            value = 0;
        } else {
            value = mem.read64(baseAddress + offset);
        }
        offset += 8;

        return value;
    }

    protected void read32Array(int[] array) {
    	for (int i = 0; array != null && i < array.length; i++) {
    		array[i] = read32();
    	}
    }

    protected boolean readBoolean() {
    	int value = read8();

    	return (value != 0);
    }

    protected void readBooleanArray(boolean[] array) {
    	for (int i = 0; array != null && i < array.length; i++) {
    		array[i] = readBoolean();
    	}
    }

    protected float readFloat() {
    	int int32 = read32();

    	return Float.intBitsToFloat(int32);
    }

    protected void readFloatArray(float[] array) {
    	for (int i = 0; array != null && i < array.length; i++) {
    		array[i] = readFloat();
    	}
    }

    protected void readFloatArray(float[][] array) {
    	for (int i = 0; array != null && i < array.length; i++) {
    		readFloatArray(array[i]);
    	}
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

    /**
     * Read a string in UTF16, until '\0\0'
     * @param addr address of the string
     * @return the string
     */
    protected String readStringUTF16Z(int addr) {
    	if (addr == 0) {
    		return null;
    	}

    	IMemoryReader memoryReader = MemoryReader.getMemoryReader(addr, 2);
    	StringBuilder s = new StringBuilder();
    	while (true) {
    		int char16 = memoryReader.readNext();
    		if (char16 == 0) {
    			break;
    		}
    		byte[] bytes = new byte[2];
    		bytes[0] = (byte) char16;
    		bytes[1] = (byte) (char16 >> 8);
    		s.append(new String(bytes, charset16));
    	}

    	return s.toString();
    }

    // Write a string in UTF16
    /**
     * Write a string in UTF16, including a trailing '\0\0'
     * @param addr address where to write the string
     * @param s the string to write
     * @return the number of bytes written (not including the trailing '\0\0')
     */
    protected int writeStringUTF16Z(int addr, String s) {
    	if (addr == 0 || s == null) {
    		return 0;
    	}

    	byte[] bytes = s.getBytes(charset16);
    	if (bytes == null) {
    		return 0;
    	}

    	for (int i = 0; i < bytes.length; i++) {
    		mem.write8(addr + i, bytes[i]);
    	}

    	// Write trailing '\0\0'
    	mem.write8(addr + bytes.length    , (byte) 0);
    	mem.write8(addr + bytes.length + 1, (byte) 0);

    	return bytes.length;
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

    protected void write8Array(byte[] array) {
    	for (int i = 0; array != null && i < array.length; i++) {
    		write8(array[i]);
    	}
    }

    protected void write16(short data) {
    	align16();
        if (offset < maxSize) {
            mem.write16(baseAddress + offset, data);
        }
        offset += 2;
    }

    protected void write32(int data) {
    	align32();
        if (offset < maxSize) {
            mem.write32(baseAddress + offset, data);
        }
        offset += 4;
    }

    protected void write64(long data) {
    	align32();
        if (offset < maxSize) {
            mem.write64(baseAddress + offset, data);
        }
        offset += 8;
    }

    protected void write32Array(int[] array) {
    	for (int i = 0; array != null && i < array.length; i++) {
    		write32(array[i]);
    	}
    }

    protected void writeBoolean(boolean data) {
    	write8(data ? (byte) 1 : (byte) 0);
    }

    protected void writeBooleanArray(boolean[] array) {
    	for (int i = 0; array != null && i < array.length; i++) {
    		writeBoolean(array[i]);
    	}
    }

    protected void writeFloat(float data) {
    	int int32 = Float.floatToIntBits(data);
    	write32(int32);
    }

    protected void writeFloatArray(float[] array) {
    	for (int i = 0; array != null && i < array.length; i++) {
    		writeFloat(array[i]);
    	}
    }

    protected void writeFloatArray(float[][] array) {
    	for (int i = 0; array != null && i < array.length; i++) {
    		writeFloatArray(array[i]);
    	}
    }

    protected void writeUnknown(int length) {
        for (int i = 0; i < length; i++) {
            write8((byte) unknown);
        }
    }

    protected void writeSkip(int length) {
        offset += length;
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

    protected int getOffset() {
    	return offset;
    }

    public int getBaseAddress() {
    	return baseAddress;
    }
}
