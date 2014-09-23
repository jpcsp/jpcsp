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

import static java.lang.System.arraycopy;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.HLE.Modules;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.filesystems.SeekableDataInput;
import jpcsp.filesystems.SeekableRandomFile;
import jpcsp.filesystems.umdiso.UmdIsoFile;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.MemoryWriter;

public class Utilities {

    private static final int[] round4 = {0, 3, 2, 1};

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
        return Long.toBinaryString(0x0000000100000000L | ((value) & 0x00000000FFFFFFFFL)).substring(1);
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

    public static void skipUnknown(ByteBuffer buf, int length) throws IOException {
        buf.position(buf.position() + length);
    }

    public static String readStringZ(ByteBuffer buf) throws IOException {
        StringBuilder sb = new StringBuilder();
        byte b;
        for (; buf.position() < buf.limit();) {
            b = (byte) readUByte(buf);
            if (b == 0) {
                break;
            }
            sb.append((char) b);
        }
        return sb.toString();
    }

    public static String readStringNZ(ByteBuffer buf, int n) throws IOException {
        StringBuilder sb = new StringBuilder();
        byte b;
        for (; n > 0; n--) {
            b = (byte) readUByte(buf);
            if (b != 0) {
                sb.append((char) b);
            }
        }
        return sb.toString();
    }

    /**
     * Read a string from memory. The string ends when the maximal length is
     * reached or a '\0' byte is found. The memory bytes are interpreted as
     * UTF-8 bytes to form the string.
     *
     * @param mem the memory
     * @param address the address of the first byte of the string
     * @param n the maximal string length
     * @return the string converted to UTF-8
     */
    public static String readStringNZ(Memory mem, int address, int n) {
        address &= Memory.addressMask;
        if (address + n > MemoryMap.END_RAM) {
            n = MemoryMap.END_RAM - address + 1;
            if (n < 0) {
            	n = 0;
            }
        }

        // Allocate a byte array to store the bytes of the string.
        // At first, allocate maximum 10000 bytes in case we don't know
        // the maximal string length. The array will be extended if required.
        byte[] bytes = new byte[Math.min(n, 10000)];

        int length = 0;
        IMemoryReader memoryReader = MemoryReader.getMemoryReader(address, n, 1);
        for (; n > 0; n--) {
            int b = memoryReader.readNext();
            if (b == 0) {
                break;
            }

            if (length >= bytes.length) {
                // Extend the bytes array
                byte[] newBytes = new byte[bytes.length + 10000];
                System.arraycopy(bytes, 0, newBytes, 0, bytes.length);
                bytes = newBytes;
            }

            bytes[length] = (byte) b;
            length++;
        }

        // Convert the bytes to UTF-8
        return new String(bytes, 0, length, Constants.charset);
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
        IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(address, n, 1);
        if (s != null) {
            byte[] bytes = s.getBytes(Constants.charset);
            while (offset < bytes.length && offset < n) {
                memoryWriter.writeNext(bytes[offset]);
                offset++;
            }
        }
        while (offset < n) {
            memoryWriter.writeNext(0);
            offset++;
        }
        memoryWriter.flush();
    }

    public static void writeStringZ(Memory mem, int address, String s) {
        // add 1 to the length to write the final '\0'
        writeStringNZ(mem, address, s.length() + 1, s);
    }

    public static void writeStringZ(ByteBuffer buf, String s) {
        buf.put(s.getBytes());
        buf.put((byte) 0);
    }

    public static int getUnsignedByte(ByteBuffer bb) throws IOException {
        return bb.get() & 0xFF;
    }

    public static void putUnsignedByte(ByteBuffer bb, int value) {
        bb.put((byte) (value & 0xFF));
    }

    public static int readUByte(ByteBuffer buf) throws IOException {
        return getUnsignedByte(buf);
    }

    public static int readUHalf(ByteBuffer buf) throws IOException {
        return getUnsignedByte(buf) | (getUnsignedByte(buf) << 8);
    }

    public static int readUWord(ByteBuffer buf) throws IOException {
    	// No difference between signed and unsigned word (32-bit value)
    	return readWord(buf);
    }

    public static int readWord(ByteBuffer buf) throws IOException {
        return getUnsignedByte(buf) | (getUnsignedByte(buf) << 8) | (getUnsignedByte(buf) << 16) | (getUnsignedByte(buf) << 24);
    }

    public static void writeWord(ByteBuffer buf, int value) {
        putUnsignedByte(buf, value >> 0);
        putUnsignedByte(buf, value >> 8);
        putUnsignedByte(buf, value >> 16);
        putUnsignedByte(buf, value >> 24);
    }

    public static void writeHalf(ByteBuffer buf, int value) {
        putUnsignedByte(buf, value >> 0);
        putUnsignedByte(buf, value >> 8);
    }

    public static void writeByte(ByteBuffer buf, int value) {
        putUnsignedByte(buf, value);
    }

    public static int parseAddress(String value) throws NumberFormatException {
        int address = 0;
        if (value == null) {
            return address;
        }

        value = value.trim();

        if (value.startsWith("0x")) {
            value = value.substring(2);
        }

        if (value.length() == 8 && value.charAt(0) >= '8') {
            address = (int) Long.parseLong(value, 16);
        } else {
            address = Integer.parseInt(value, 16);
        }

        return address;
    }

    /**
     * Parse the string as a number and returns its value. If the string starts
     * with "0x", the number is parsed in base 16, otherwise base 10.
     *
     * @param s the string to be parsed
     * @return the numeric value represented by the string.
     */
    public static long parseLong(String s) {
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
     * Parse the string as a number and returns its value. The number is always
     * parsed in base 16. The string can start as an option with "0x".
     *
     * @param s the string to be parsed in base 16
     * @return the numeric value represented by the string.
     */
    public static long parseHexLong(String s) {
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
        n = (n >> 1) | n;
        n = (n >> 2) | n;
        n = (n >> 4) | n;
        n = (n >> 8) | n;
        n = (n >> 16) | n;
        return ++n;
    }

    /**
     * Check if a value is a power of 2, i.e. a value that be can computed as (1 << x).
     * 
     * @param n      value to be checked
     * @return       true if the value is a power of 2,
     *               false otherwise.
     */
    public static boolean isPower2(int n) {
    	return (n & (n - 1)) == 0;
    }

    public static void readFully(SeekableDataInput input, int address, int length) throws IOException {
        final int blockSize = 16 * UmdIsoFile.sectorLength;  // 32Kb
        byte[] buffer = null;
        while (length > 0) {
            int size = Math.min(length, blockSize);
            if (buffer == null || size != buffer.length) {
                buffer = new byte[size];
            }
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
        } else if (length > 0) {
            byte[] bytes = new byte[length];
            IMemoryReader memoryReader = MemoryReader.getMemoryReader(address, length, 1);
            for (int i = 0; i < length; i++) {
                bytes[i] = (byte) memoryReader.readNext();
            }
            output.write(bytes);
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

    public static void putBuffer(ByteBuffer destination, Buffer source, ByteOrder sourceByteOrder, int lengthInBytes) {
        // Set the destination to the desired ByteOrder
        ByteOrder order = destination.order();
        destination.order(sourceByteOrder);

        int srcLimit = source.limit();
        if (source instanceof IntBuffer) {
            int copyLength = lengthInBytes & ~3;
            destination.asIntBuffer().put((IntBuffer) source.limit(source.position() + (copyLength >> 2)));
            int restLength = lengthInBytes - copyLength;
            if (restLength > 0) {
                // 1 to 3 bytes left to copy
                source.limit(srcLimit);
                int value = ((IntBuffer) source).get();
                int position = destination.position() + copyLength;
                do {
                    destination.put(position, (byte) value);
                    value >>= 8;
                    restLength--;
                    position++;
                } while (restLength > 0);
            }
        } else if (source instanceof ByteBuffer) {
            destination.put((ByteBuffer) source.limit(source.position() + lengthInBytes));
        } else if (source instanceof ShortBuffer) {
            int copyLength = lengthInBytes & ~1;
            destination.asShortBuffer().put((ShortBuffer) source.limit(source.position() + (copyLength >> 1)));
            int restLength = lengthInBytes - copyLength;
            if (restLength > 0) {
                // 1 byte left to copy
                source.limit(srcLimit);
                short value = ((ShortBuffer) source).get();
                destination.put(destination.position() + copyLength, (byte) value);
            }
        } else if (source instanceof FloatBuffer) {
            int copyLength = lengthInBytes & ~3;
            destination.asFloatBuffer().put((FloatBuffer) source.limit(source.position() + (copyLength >> 2)));
            int restLength = lengthInBytes - copyLength;
            if (restLength > 0) {
                // 1 to 3 bytes left to copy
                source.limit(srcLimit);
                int value = Float.floatToRawIntBits(((FloatBuffer) source).get());
                int position = destination.position() + copyLength;
                do {
                    destination.put(position, (byte) value);
                    value >>= 8;
                    restLength--;
                    position++;
                } while (restLength > 0);
            }
        } else {
            Emulator.log.error("Utilities.putBuffer: Unsupported Buffer type " + source.getClass().getName());
            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_UNIMPLEMENTED);
        }

        // Reset the original ByteOrder of the destination
        destination.order(order);
        // Reset the original limit of the source
        source.limit(srcLimit);
    }

    /**
     * Reads inputstream i into a String with the UTF-8 charset until the
     * inputstream is finished (don't use with infinite streams).
     *
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

    public static int getSizeKb(long sizeByte) {
        return (int) ((sizeByte + 1023) / 1024);
    }

    private static void addAsciiDump(StringBuilder dump, IMemoryReader charReader, int bytesPerLine) {
        dump.append("  >");
        for (int i = 0; i < bytesPerLine; i++) {
            char c = (char) charReader.readNext();
            if (c < ' ' || c > '~') {
                c = '.';
            }
            dump.append(c);
        }
        dump.append("<");
    }

    private static String getMemoryDump(int address, int length, int step, int bytesPerLine, IMemoryReader memoryReader, IMemoryReader charReader) {
        StringBuilder dump = new StringBuilder();
        String lineSeparator = System.getProperty("line.separator");

        if (length < bytesPerLine) {
            bytesPerLine = length;
        }

        String format = String.format(" %%0%dX", step * 2);
        boolean startOfLine = true;
        for (int i = 0; i < length; i += step) {
            if ((i % bytesPerLine) < step) {
                if (i > 0) {
                    // Add an ASCII representation at the end of the line
                    addAsciiDump(dump, charReader, bytesPerLine);
                }
                dump.append(lineSeparator);
                startOfLine = true;
            }
            if (startOfLine) {
                dump.append(String.format("0x%08X", address + i));
                startOfLine = false;
            }

            int value = memoryReader.readNext();
            if (length - i >= step) {
                dump.append(String.format(format, value));
            } else {
                switch (length - i) {
                    case 3:
                        dump.append(String.format(" %06X", value & 0x00FFFFFF));
                        break;
                    case 2:
                        dump.append(String.format(" %04X", value & 0x0000FFFF));
                        break;
                    case 1:
                        dump.append(String.format(" %02X", value & 0x000000FF));
                        break;
                }
            }
        }

        int lengthLastLine = length % bytesPerLine;
        if (lengthLastLine > 0) {
            for (int i = lengthLastLine; i < bytesPerLine; i++) {
                dump.append("  ");
                if ((i % step) == 0) {
                    dump.append(" ");
                }
            }
            addAsciiDump(dump, charReader, lengthLastLine);
        } else {
            addAsciiDump(dump, charReader, bytesPerLine);
        }

        return dump.toString();
    }

    public static String getMemoryDump(int address, int length) {
        // Convenience function using default step and bytesPerLine
        return getMemoryDump(address, length, 1, 16);
    }

    public static String getMemoryDump(int address, int length, int step, int bytesPerLine) {
        if (!Memory.isAddressGood(address) || length <= 0 || bytesPerLine <= 0 || step <= 0) {
            return "";
        }

        IMemoryReader memoryReader = MemoryReader.getMemoryReader(address, length, step);
        IMemoryReader charReader = MemoryReader.getMemoryReader(address, length, 1);

        return getMemoryDump(address, length, step, bytesPerLine, memoryReader, charReader);
    }

    public static String getMemoryDump(byte[] bytes, int offset, int length) {
        // Convenience function using default step and bytesPerLine
        return getMemoryDump(bytes, offset, length, 1, 16);
    }

    public static String getMemoryDump(byte[] bytes, int offset, int length, int step, int bytesPerLine) {
        if (bytes == null || length <= 0 || bytesPerLine <= 0 || step <= 0) {
            return "";
        }

        IMemoryReader memoryReader = MemoryReader.getMemoryReader(bytes, offset, length, step);
        IMemoryReader charReader = MemoryReader.getMemoryReader(bytes, offset, length, step);

        return getMemoryDump(0, length, step, bytesPerLine, memoryReader, charReader);
    }

    public static int alignUp(int value, int alignment) {
        return alignDown(value + alignment, alignment);
    }

    public static int alignDown(int value, int alignment) {
        return value & ~alignment;
    }

    public static int endianSwap32(int x) {
        return Integer.reverseBytes(x);
    }

    public static int endianSwap16(int x) {
        return ((x >> 8) & 0x00FF) | ((x << 8) & 0xFF00);
    }

    public static int readUnaligned32(Memory mem, int address) {
        switch (address & 3) {
            case 0:
                return mem.read32(address);
            case 2:
                return mem.read16(address) | (mem.read16(address + 2) << 16);
            default:
                return (mem.read8(address + 3) << 24)
                        | (mem.read8(address + 2) << 16)
                        | (mem.read8(address + 1) << 8)
                        | (mem.read8(address));
        }
    }

    public static int readUnaligned16(Memory mem, int address) {
    	if ((address & 1) == 0) {
    		return mem.read16(address);
    	}
    	return (mem.read8(address + 1) << 8) | mem.read8(address);
    }

    public static int read8(byte[] buffer, int offset) {
        return buffer[offset] & 0xFF;
    }

    public static int readUnaligned32(byte[] buffer, int offset) {
        return (read8(buffer, offset + 3) << 24)
                | (read8(buffer, offset + 2) << 16)
                | (read8(buffer, offset + 1) << 8)
                | (read8(buffer, offset));
    }

    public static int readUnaligned16(byte[] buffer, int offset) {
        return (read8(buffer, offset + 1) << 8) | read8(buffer, offset);
    }

    public static void writeUnaligned32(Memory mem, int address, int data) {
        switch (address & 3) {
            case 0:
                mem.write32(address, data);
                break;
            case 2:
                mem.write16(address, (short) data);
                mem.write16(address + 2, (short) (data >> 16));
                break;
            default:
                mem.write8(address, (byte) data);
                mem.write8(address + 1, (byte) (data >> 8));
                mem.write8(address + 2, (byte) (data >> 16));
                mem.write8(address + 3, (byte) (data >> 24));
        }
    }

    public static int min(int a, int b) {
        return Math.min(a, b);
    }

    public static float min(float a, float b) {
        return Math.min(a, b);
    }

    public static int max(int a, int b) {
        return Math.max(a, b);
    }

    public static float max(float a, float b) {
        return Math.max(a, b);
    }

    /**
     * Minimum value rounded down.
     *
     * @param a first float value
     * @param b second float value
     * @return the largest int value that is less than or equal to both
     * parameters
     */
    public static int minInt(float a, float b) {
        return floor(min(a, b));
    }

    /**
     * Minimum value rounded down.
     *
     * @param a first int value
     * @param b second float value
     * @return the largest int value that is less than or equal to both
     * parameters
     */
    public static int minInt(int a, float b) {
        return min(a, floor(b));
    }

    /**
     * Maximum value rounded up.
     *
     * @param a first float value
     * @param b second float value
     * @return the smallest int value that is greater than or equal to both
     * parameters
     */
    public static int maxInt(float a, float b) {
        return ceil(max(a, b));
    }

    /**
     * Maximum value rounded up.
     *
     * @param a first float value
     * @param b second float value
     * @return the smallest int value that is greater than or equal to both
     * parameters
     */
    public static int maxInt(int a, float b) {
        return max(a, ceil(b));
    }

    public static int min(int a, int b, int c) {
        return Math.min(a, Math.min(b, c));
    }

    public static int max(int a, int b, int c) {
        return Math.max(a, Math.max(b, c));
    }

    public static void sleep(int micros) {
        sleep(micros / 1000, micros % 1000);
    }

    public static void sleep(int millis, int micros) {
    	if (millis < 0) {
    		return;
    	}

    	try {
            if (micros <= 0) {
                Thread.sleep(millis);
            } else {
                Thread.sleep(millis, micros * 1000);
            }
        } catch (InterruptedException e) {
            // Ignore exception
        }
    }

    public static void matrixMult(final float[] result, float[] m1, float[] m2) {
        // If the result has to be stored into one of the input matrix,
        // duplicate the input matrix.
        if (result == m1) {
            m1 = m1.clone();
        }
        if (result == m2) {
            m2 = m2.clone();
        }

        int i = 0;
        for (int j = 0; j < 16; j += 4) {
            for (int x = 0; x < 4; x++) {
                result[i] = m1[x] * m2[j]
                        + m1[x + 4] * m2[j + 1]
                        + m1[x + 8] * m2[j + 2]
                        + m1[x + 12] * m2[j + 3];
                i++;
            }
        }
    }

    public static void vectorMult(final float[] result, final float[] m, final float[] v) {
        for (int i = 0; i < result.length; i++) {
            float s = v[0] * m[i];
            int k = i + 4;
            for (int j = 1; j < v.length; j++) {
                s += v[j] * m[k];
                k += 4;
            }
            result[i] = s;
        }
    }

    public static void vectorMult33(final float[] result, final float[] m, final float[] v) {
        result[0] = v[0] * m[0] + v[1] * m[4] + v[2] * m[8];
        result[1] = v[0] * m[1] + v[1] * m[5] + v[2] * m[9];
        result[2] = v[0] * m[2] + v[1] * m[6] + v[2] * m[10];
    }

    public static void vectorMult34(final float[] result, final float[] m, final float[] v) {
        result[0] = v[0] * m[0] + v[1] * m[4] + v[2] * m[8] + v[3] * m[12];
        result[1] = v[0] * m[1] + v[1] * m[5] + v[2] * m[9] + v[3] * m[13];
        result[2] = v[0] * m[2] + v[1] * m[6] + v[2] * m[10] + v[3] * m[14];
    }

    public static void vectorMult44(final float[] result, final float[] m, final float[] v) {
        result[0] = v[0] * m[0] + v[1] * m[4] + v[2] * m[8] + v[3] * m[12];
        result[1] = v[0] * m[1] + v[1] * m[5] + v[2] * m[9] + v[3] * m[13];
        result[2] = v[0] * m[2] + v[1] * m[6] + v[2] * m[10] + v[3] * m[14];
        result[3] = v[0] * m[3] + v[1] * m[7] + v[2] * m[11] + v[3] * m[15];
    }

    // This is equivalent to Math.round but faster: Math.round is using StrictMath.
    public static int round(float n) {
        return (int) (n + .5f);
    }

    public static int floor(float n) {
        return (int) Math.floor(n);
    }

    public static int ceil(float n) {
        return (int) Math.ceil(n);
    }

    public static int getPower2(int n) {
        return Integer.numberOfTrailingZeros(makePow2(n));
    }

    public static void copy(boolean[] to, boolean[] from) {
        arraycopy(from, 0, to, 0, to.length);
    }

    public static void copy(boolean[][] to, boolean[][] from) {
    	for (int i = 0; i < to.length; i++) {
    		copy(to[i], from[i]);
    	}
    }

    public static void copy(int[] to, int[] from) {
        arraycopy(from, 0, to, 0, to.length);
    }

    public static void copy(int[][] to, int[][] from) {
    	for (int i = 0; i < to.length; i++) {
    		copy(to[i], from[i]);
    	}
    }

    public static void copy(int[][][] to, int[][][] from) {
    	for (int i = 0; i < to.length; i++) {
    		copy(to[i], from[i]);
    	}
    }

    public static void copy(int[][][][] to, int[][][][] from) {
    	for (int i = 0; i < to.length; i++) {
    		copy(to[i], from[i]);
    	}
    }

    public static void copy(float[] to, float[] from) {
        arraycopy(from, 0, to, 0, to.length);
    }

    public static void copy(float[][] to, float[][] from) {
    	for (int i = 0; i < to.length; i++) {
    		copy(to[i], from[i]);
    	}
    }

    public static void copy(float[][][] to, float[][][] from) {
    	for (int i = 0; i < to.length; i++) {
    		copy(to[i], from[i]);
    	}
    }

    public static void copy(float[][][][] to, float[][][][] from) {
    	for (int i = 0; i < to.length; i++) {
    		copy(to[i], from[i]);
    	}
    }

    public static float dot3(float[] a, float[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    public static float dot3(float[] a, float x, float y, float z) {
        return a[0] * x + a[1] * y + a[2] * z;
    }

    public static float length3(float[] a) {
        return (float) Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
    }

    public static float invertedLength3(float[] a) {
        float length = length3(a);
        if (length == 0.f) {
            return 0.f;
        }
        return 1.f / length;
    }

    public static void normalize3(float[] result, float[] a) {
        float invertedLength = invertedLength3(a);
        result[0] = a[0] * invertedLength;
        result[1] = a[1] * invertedLength;
        result[2] = a[2] * invertedLength;
    }

    public static float pow(float a, float b) {
        return (float) Math.pow(a, b);
    }

    public static float clamp(float n, float minValue, float maxValue) {
        return max(minValue, min(n, maxValue));
    }

    /**
     * Invert a 3x3 matrix.
     *
     * Based on
     * http://en.wikipedia.org/wiki/Invert_matrix#Inversion_of_3.C3.973_matrices
     *
     * @param result the inverted matrix (stored as a 4x4 matrix, but only 3x3
     * is returned)
     * @param m the matrix to be inverted (stored as a 4x4 matrix, but only 3x3
     * is used)
     * @return true if the matrix could be inverted false if the matrix could
     * not be inverted
     */
    public static boolean invertMatrix3x3(float[] result, float[] m) {
        float A = m[5] * m[10] - m[6] * m[9];
        float B = m[6] * m[8] - m[4] * m[10];
        float C = m[4] * m[9] - m[5] * m[8];
        float det = m[0] * A + m[1] * B + m[2] * C;

        if (det == 0.f) {
            // Matrix could not be inverted
            return false;
        }

        float invertedDet = 1.f / det;
        result[0] = A * invertedDet;
        result[1] = (m[2] * m[9] - m[1] * m[10]) * invertedDet;
        result[2] = (m[1] * m[6] - m[2] * m[5]) * invertedDet;
        result[4] = B * invertedDet;
        result[5] = (m[0] * m[10] - m[2] * m[8]) * invertedDet;
        result[6] = (m[2] * m[4] - m[0] * m[6]) * invertedDet;
        result[8] = C * invertedDet;
        result[9] = (m[8] * m[1] - m[0] * m[9]) * invertedDet;
        result[10] = (m[0] * m[5] - m[1] * m[4]) * invertedDet;

        return true;
    }

    public static void transposeMatrix3x3(float[] result, float[] m) {
        for (int i = 0, j = 0; i < 3; i++, j += 4) {
            result[i] = m[j];
            result[i + 4] = m[j + 1];
            result[i + 8] = m[j + 2];
        }
    }

    public static boolean sameColor(float[] c1, float[] c2, float[] c3) {
        for (int i = 0; i < 4; i++) {
            if (c1[i] != c2[i] || c1[i] != c3[i]) {
                return false;
            }
        }

        return true;
    }

    public static boolean sameColor(float[] c1, float[] c2, float[] c3, float[] c4) {
        for (int i = 0; i < 4; i++) {
            if (c1[i] != c2[i] || c1[i] != c3[i] || c1[i] != c4[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * Transform a pixel coordinate (floating-point value "u" or "v") into a
     * texel coordinate (integer value to access the texture).
     *
     * The texel coordinate is calculated by truncating the floating point
     * value, not by rounding it. Otherwise transition problems occur at the
     * borders. E.g. if a texture has a width of 64, valid texel coordinates
     * range from 0 to 63. 64 is already outside of the texture and should not
     * be generated when approaching the border to the texture.
     *
     * @param coordinate the pixel coordinate
     * @return the texel coordinate
     */
    public static final int pixelToTexel(float coordinate) {
        return (int) coordinate;
    }

    /**
     * Wrap the value to the range [0..1[ (1 is excluded).
     *
     * E.g. value == 4.0 -> return 0.0 value == 4.1 -> return 0.1 value == 4.9
     * -> return 0.9 value == -4.0 -> return 0.0 value == -4.1 -> return 0.9
     * (and not 0.1) value == -4.9 -> return 0.1 (and not 0.9)
     *
     * @param value the value to be wrapped
     * @return the wrapped value in the range [0..1[ (1 is excluded)
     */
    public static float wrap(float value) {
        if (value >= 0.f) {
            // value == 4.0 -> return 0.0
            // value == 4.1 -> return 0.1
            // value == 4.9 -> return 0.9
            return value - (int) value;
        }

        // value == -4.0 -> return 0.0
        // value == -4.1 -> return 0.9
        // value == -4.9 -> return 0.1
        // value == -1e-8 -> return 0.0
        float wrappedValue = value - (float) Math.floor(value);
        if (wrappedValue >= 1.f) {
            wrappedValue -= 1.f;
        }
        return wrappedValue;
    }

    public static int wrap(float value, int valueMask) {
        return pixelToTexel(value) & valueMask;
    }

    public static void readBytes(int address, int length, byte[] bytes, int offset) {
        IMemoryReader memoryReader = MemoryReader.getMemoryReader(address, length, 1);
        for (int i = 0; i < length; i++) {
            bytes[offset + i] = (byte) memoryReader.readNext();
        }
    }

    public static void writeBytes(int address, int length, byte[] bytes, int offset) {
        IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(address, length, 1);
        for (int i = 0; i < length; i++) {
            memoryWriter.writeNext(bytes[i + offset] & 0xFF);
        }
        memoryWriter.flush();
    }

    public static int round4(int n) {
        return n + round4[n & 3];
    }

    public static int round2(int n) {
        return n + (n & 1);
    }

    public static int[] extendArray(int[] array, int extend) {
        if (array == null) {
            return new int[extend];
        }

        int[] newArray = new int[array.length + extend];
        System.arraycopy(array, 0, newArray, 0, array.length);

        return newArray;
    }

    public static byte[] readCompleteFile(IVirtualFile vFile) {
        if (vFile == null) {
            return null;
        }

        byte[] buffer;
        try {
            buffer = new byte[(int) (vFile.length() - vFile.getPosition())];
        } catch (OutOfMemoryError e) {
            Emulator.log.error("Error while reading a complete vFile", e);
            return null;
        }

        int length = 0;
        while (length < buffer.length) {
            int readLength = vFile.ioRead(buffer, length, buffer.length - length);
            if (readLength < 0) {
                break;
            }
            length += readLength;
        }

        if (length < buffer.length) {
            byte[] resizedBuffer;
            try {
                resizedBuffer = new byte[length];
            } catch (OutOfMemoryError e) {
                Emulator.log.error("Error while reading a complete vFile", e);
                return null;
            }
            System.arraycopy(buffer, 0, resizedBuffer, 0, length);
            buffer = resizedBuffer;
        }

        return buffer;
    }

    public static boolean isSystemLibraryExisting(String libraryName) {
    	String[] extensions = new String[] { ".dll", ".so" };

    	String path = System.getProperty("java.library.path");
    	if (path == null) {
    		path = "";
    	} else if (!path.endsWith("/")) {
    		path += "/";
    	}

    	for (String extension : extensions) {
        	File libraryFile = new File(String.format("%s%s%s", path, libraryName, extension));
        	if (libraryFile.canExecute()) {
        		return true;
        	}
    	}

    	return false;
    }

    public static int signExtend(int value, int bits) {
    	int shift = Integer.SIZE - bits;
    	return (value << shift) >> shift;
    }

    public static int clip(int value, int min, int max) {
    	if (value < min) {
    		return min;
    	}
    	if (value > max) {
    		return max;
    	}

    	return value;
    }

    public static float clipf(float value, float min, float max) {
    	if (value < min) {
    		return min;
    	}
    	if (value > max) {
    		return max;
    	}

    	return value;
    }

    public static void fill(int a[][], int value) {
    	for (int i = 0; i < a.length; i++) {
    		Arrays.fill(a[i], value);
    	}
    }

    public static void fill(float a[], float value) {
		Arrays.fill(a, value);
    }

    public static void fill(float a[][], float value) {
    	for (int i = 0; i < a.length; i++) {
    		Arrays.fill(a[i], value);
    	}
    }

    public static void fill(float a[][][], float value) {
    	for (int i = 0; i < a.length; i++) {
    		fill(a[i], value);
    	}
    }

    public static void fill(float a[][][][], float value) {
    	for (int i = 0; i < a.length; i++) {
    		fill(a[i], value);
    	}
    }
}
