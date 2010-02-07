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
package jpcsp.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.HLE.Modules;
import jpcsp.filesystems.*;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Utilities {

    public static String formatString(String type, String oldstring) {
        int counter = 0;
        if (type.equals("byte")) {
            counter = 2;
        }
        if (type.equals("short")) {
            counter = 4;
        }
        if (type.equals("long")) {
            counter = 8;
        }
        int len = oldstring.length();
        StringBuilder sb = new StringBuilder();
        while (len++ < counter) {
            sb.append('0');
        }
        oldstring = sb.append(oldstring).toString();
        return oldstring;

    }

    public static String integerToBin(int value) {
        return Long.toBinaryString(0x0000000100000000L|(((long)value)&0x00000000FFFFFFFFL)).substring(1);
    }

    public static String integerToHex(int value) {
        return Integer.toHexString(0x100 | value).substring(1).toUpperCase();
    }

    public static String integerToHexShort(int value) {
        return Integer.toHexString(0x10000 | value).substring(1).toUpperCase();
    }

    public static long readUWord(SeekableDataInput f) throws IOException {
        long l = (f.readUnsignedByte() | (f.readUnsignedByte() << 8) | (f.readUnsignedByte() << 16) | (f.readUnsignedByte() << 24));
        return (l & 0xFFFFFFFFL);
    }

    public static int readUByte(SeekableDataInput f) throws IOException {
        return f.readUnsignedByte();
    }

    public static int readUHalf(SeekableDataInput f) throws IOException {
        return f.readUnsignedByte() | (f.readUnsignedByte() << 8);
    }

    public static int readWord(SeekableDataInput f) throws IOException {
        //readByte() isn't more correct? (already exists one readUWord() method to unsign values)
        return (f.readUnsignedByte() | (f.readUnsignedByte() << 8) | (f.readUnsignedByte() << 16) | (f.readUnsignedByte() << 24));
    }

    public static void readBytesToBuffer(
        SeekableDataInput f, ByteBuffer buf,
        int offset, int size) throws IOException
    {
        f.readFully(buf.array(), offset + buf.arrayOffset(), size);
    }
    public static String readStringZ(SeekableDataInput f) throws IOException {
        StringBuilder sb = new StringBuilder();
        int b;
        for (; f.getFilePointer() < f.length();) {
            b = f.readUnsignedByte();
            if (b == 0) {
                break;
            }
            sb.append((char)b);
        }
        return sb.toString();
    }

    public static String readStringZ(byte[] mem, int offset) {
        StringBuilder sb = new StringBuilder();
        int b;
        for (; offset < mem.length;) {
            b = mem[offset++];
            if (b == 0) {
                break;
            }
            sb.append((char)b);
        }
        return sb.toString();
    }

    public static String readStringZ(ByteBuffer buf) throws IOException {
        StringBuilder sb = new StringBuilder();
        byte b;
        for (; buf.position() < buf.limit();) {
              b = (byte)readUByte(buf);
            if (b == 0)
                break;
            sb.append((char)b);
        }
        return sb.toString();
    }
    public static String readStringNZ(Memory mem, int address, int n) {
        StringBuilder sb = new StringBuilder();
    	address &= Memory.addressMask;
        if (address + n > MemoryMap.END_RAM) {
        	n = MemoryMap.END_RAM - address + 1;
        }
        int b;
        for (; n > 0; n--) {
            b = mem.read8(address++);
            if (b == 0)
                break;
            sb.append((char)b);
        }
        return sb.toString();
    }
    public static String readStringZ(Memory mem, int address) {
    	address &= Memory.addressMask;
    	return readStringNZ(mem, address, MemoryMap.END_RAM - address + 1);
    }
    public static String readStringZ(int address) {
    	return readStringZ(Memory.getInstance(), address);
    }
    public static String readStringNZ(int address, int n) {
    	return readStringNZ(Memory.getInstance(), address, n);
    }
    public static void writeStringNZ(Memory mem, int address, int n, String s) {
    	int offset = 0;
    	while (offset < s.length() && offset < n) {
    		mem.write8(address + offset, (byte) s.charAt(offset));
    		offset++;
    	}
    	while (offset < n) {
    		mem.write8(address + offset, (byte) 0);
    		offset++;
    	}
    }
    public static void writeStringZ(Memory mem, int address, String s) {
    	int offset = 0;
    	while (offset < s.length()) {
    		mem.write8(address + offset, (byte) s.charAt(offset));
    		offset++;
    	}
        mem.write8(address + offset, (byte) 0);
    }
    public static void writeStringZ(ByteBuffer buf, String s) {
		buf.put(s.getBytes());
		buf.put((byte) 0);
    }
    public static short getUnsignedByte (ByteBuffer bb) throws IOException
    {
       return ((short)(bb.get() & 0xff));
    }
    public static void putUnsignedByte(ByteBuffer bb, int value)
    {
    	bb.put((byte) (value & 0xFF));
    }
    public static short readUByte(ByteBuffer buf) throws IOException
    {
    	return getUnsignedByte(buf);
    }
    public static int readUHalf(ByteBuffer buf) throws IOException
    {
    	return getUnsignedByte(buf) | (getUnsignedByte(buf) << 8);
    }
    public static long readUWord(ByteBuffer buf) throws IOException
    {
        long l = (getUnsignedByte(buf) | (getUnsignedByte(buf) << 8 ) | (getUnsignedByte(buf) << 16 ) | (getUnsignedByte(buf) << 24));
        return (l & 0xFFFFFFFFL);

    }
    public static int readWord(ByteBuffer buf) throws IOException
    {
        return (getUnsignedByte(buf) | (getUnsignedByte(buf) << 8 ) | (getUnsignedByte(buf) << 16 ) | (getUnsignedByte(buf) << 24));
    }
    public static void writeWord(ByteBuffer buf, long value)
    {
    	putUnsignedByte(buf, (int) (value >>  0));
    	putUnsignedByte(buf, (int) (value >>  8));
    	putUnsignedByte(buf, (int) (value >> 16));
    	putUnsignedByte(buf, (int) (value >> 24));
    }
    public static void writeHalf(ByteBuffer buf, int value)
    {
    	putUnsignedByte(buf, value >> 0);
    	putUnsignedByte(buf, value >> 8);
    }
    public static void writeByte(ByteBuffer buf, int value)
    {
    	putUnsignedByte(buf, value);
    }

    public static int parseAddress(String value)
    {
    	int address = 0;

    	if (value == null) {
    		return address;
    	}

        if (value.startsWith("0x"))
            value = value.substring(2);

    	if (Integer.SIZE == 32 && value.length() == 8 && value.charAt(0) >= '8') {
    		address = (int) Long.parseLong(value, 16);
    	} else {
    		address = Integer.parseInt(value, 16);
    	}

    	return address;
    }


    /**
     * Parse the string as a number and returns its value.
     * If the string starts with "0x", the number is parsed
     * in base 16, otherwise base 10.
     * 
     * @param s the string to be parsed
     * @return the numeric value represented by the string.
     */
    public static long parseLong(String s)
    {
    	long value = 0;

    	if (s == null) {
    		return value;
    	}

    	if (s.startsWith("0x")) {
    		value = Long.parseLong(s.substring(2), 16);
    	} else {
    		value = Long.parseLong(s);
    	}

    	return value;
    }

    /**
     * Parse the string as a number and returns its value.
     * The number is always parsed in base 16.
     * The string can start as an option with "0x".
     * 
     * @param s the string to be parsed in base 16
     * @return the numeric value represented by the string.
     */
    public static long parseHexLong(String s)
    {
        long value = 0;

        if (s == null) {
            return value;
        }

        if (s.startsWith("0x")) {
            s = s.substring(2);
        }
        value = Long.parseLong(s, 16);

        return value;
    }

    public static int makePow2(int n) {
        --n;
        n = (n >>  1) | n;
        n = (n >>  2) | n;
        n = (n >>  4) | n;
        n = (n >>  8) | n;
        n = (n >> 16) | n;
        return ++n;
    }

    public static void readFully(SeekableDataInput input, int address, int length) throws IOException {
        final int blockSize = 1024 * 1024;  // 1Mb
        while (length > 0) {
            int size = Math.min(length, blockSize);
            byte[] buffer = new byte[size];
            input.readFully(buffer);
            Memory.getInstance().copyToMemory(address, ByteBuffer.wrap(buffer), size);
            address += size;
            length -= size;
        }
    }

    public static void write(SeekableRandomFile output, int address, int length) throws IOException {
    	Buffer buffer = Memory.getInstance().getBuffer(address, length);
    	if (buffer instanceof ByteBuffer) {
    		output.getChannel().write((ByteBuffer) buffer);
    	} else {
    		Memory mem = Memory.getInstance();
    		for (int i = 0; i < length; i++) {
    			output.writeByte(mem.read8(address + i));
    		}
    	}
    }

    public static void bytePositionBuffer(Buffer buffer, int bytePosition) {
    	buffer.position(bytePosition / bufferElementSize(buffer));
    }

    public static int bufferElementSize(Buffer buffer) {
    	if (buffer instanceof IntBuffer) {
    		return 4;
    	}

    	return 1;
    }

    public static String stripNL(String s) {
    	if (s != null && s.endsWith("\n")) {
    		s = s.substring(0, s.length() - 1);
    	}

    	return s;
    }

    public static void putBuffer(ByteBuffer destination, Buffer source, ByteOrder sourceByteOrder) {
		// Set the destination to the desired ByteOrder
		ByteOrder order = destination.order();
		destination.order(sourceByteOrder);

		if (source instanceof IntBuffer) {
    		destination.asIntBuffer().put((IntBuffer) source);
    	} else if (source instanceof ShortBuffer) {
    		destination.asShortBuffer().put((ShortBuffer) source);
    	} else if (source instanceof ByteBuffer) {
    		destination.put((ByteBuffer) source);
    	} else if (source instanceof FloatBuffer) {
    		destination.asFloatBuffer().put((FloatBuffer) source);
    	} else {
    		Modules.log.error("Utilities.putBuffer: Unsupported Buffer type " + source.getClass().getName());
    		Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_UNIMPLEMENTED);
    	}

		// Reset the original ByteOrder of the destination
		destination.order(order);
    }

        /**
     * Reads inputstream i into a String with the UTF-8 charset
     * until the inputstream is finished (don't use with infinite streams).
     * @param inputStream to read into a string
     * @param close if true, close the inputstream
     * @return a string
     * @throws java.io.IOException if thrown on reading the stream
     * @throws java.lang.NullPointerException if the given inputstream is null
     */
    public static String toString(InputStream inputStream, boolean close) throws IOException {
        if (inputStream == null) {
            throw new NullPointerException("null inputstream");
        }
        String string;
        StringBuilder outputBuilder = new StringBuilder();

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            while (null != (string = reader.readLine())) {
                outputBuilder.append(string).append('\n');
            }
        } finally {
            if (close) {
                close(inputStream);
            }
        }
        return outputBuilder.toString();
    }

    /**
     * Close closeables. Use this in a finally clause.
     */
    public static void close(Closeable... closeables) {
        for (Closeable c : closeables) {
            if (c != null) {
                try {
                    c.close();
                } catch (Exception ex) {
                    Logger.getLogger(Utilities.class.getName()).log(Level.WARNING, "Couldn't close Closeable", ex);
}
            }
        }
    }
}
