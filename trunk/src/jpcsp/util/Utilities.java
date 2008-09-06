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

import java.io.IOException;
import jpcsp.filesystems.*;
import java.nio.ByteBuffer;

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

    public static String integerToHex(int value) {
        return Integer.toHexString(0x100 | value).substring(1).toUpperCase();
    }

    public static String integerToHexShort(int value) {
        return Integer.toHexString(0x10000 | value).substring(1).toUpperCase();
    }

    public static long readUWord(SeekableRandomFile f) throws IOException {
        long l = (f.readUnsignedByte() | (f.readUnsignedByte() << 8) | (f.readUnsignedByte() << 16) | (f.readUnsignedByte() << 24));
        return (l & 0xFFFFFFFFL);
    }

    public static int readUByte(SeekableRandomFile f) throws IOException {
        return f.readUnsignedByte();
    }

    public static int readUHalf(SeekableRandomFile f) throws IOException {
        return f.readUnsignedByte() | (f.readUnsignedByte() << 8);
    }

    public static int readWord(SeekableRandomFile f) throws IOException {
        //readByte() isn't more correct? (already exists one readUWord() method to unsign values)
        return (f.readUnsignedByte() | (f.readUnsignedByte() << 8) | (f.readUnsignedByte() << 16) | (f.readUnsignedByte() << 24));
    }

    public static void readBytesToBuffer(
        SeekableRandomFile f, ByteBuffer buf,
        int offset, int size) throws IOException
    {
        f.read(buf.array(), offset + buf.arrayOffset(), size);
    }

    public static String readStringZ(SeekableRandomFile f) throws IOException {
        StringBuffer sb = new StringBuffer();
        int b;
        for (;;) {
            b = f.readUnsignedByte();
            if (b == 0) {
                break;
            }
            sb.append((char)b);
        }
        return sb.toString();
    }

    public static String readStringZ(byte[] mem, int offset) {
        StringBuffer sb = new StringBuffer();
        int b;
        for (;;) {
            b = mem[offset++];
            if (b == 0) {
                break;
            }
            sb.append((char)b);
        }
        return sb.toString();
    }

    public static String readStringZ(ByteBuffer buf, int offset) {
        StringBuffer sb = new StringBuffer();
        byte b;
        for (;;) {
            b = buf.get(offset++);
            if (b == 0)
                break;
            sb.append((char)b);
        }
        return sb.toString();
    }

    public static String readStringNZ(ByteBuffer buf, int offset, int n) {
        StringBuffer sb = new StringBuffer();
        byte b;
        for (; n > 0; n--) {
            b = buf.get(offset++);
            if (b == 0)
                break;
            sb.append((char)b);
        }
        return sb.toString();
    }
}
