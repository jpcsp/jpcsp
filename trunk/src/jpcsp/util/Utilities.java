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
    public static void copyByteBuffertoByteBuffer(ByteBuffer src, ByteBuffer dst , int offset , int size)throws IOException
    {
        byte[] data = new byte[size];
        src.get(data);
        dst.position(offset);
        dst.put(data);
    }
    public static String readStringZ(SeekableDataInput f) throws IOException {
        StringBuffer sb = new StringBuffer();
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
        StringBuffer sb = new StringBuffer();
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

    public static String readStringZ(ByteBuffer buf, int offset) {
        StringBuffer sb = new StringBuffer();
        byte b;
        for (; offset < buf.limit();) {
            b = buf.get(offset++);
            if (b == 0)
                break;
            sb.append((char)b);
        }
        return sb.toString();
    }
    public static String readStringZ(ByteBuffer buf) throws IOException {
        StringBuffer sb = new StringBuffer();
        byte b;
        for (; buf.position() < buf.limit();) {
              b = (byte)readUByte(buf);
            if (b == 0)
                break;
            sb.append((char)b);
        }
        return sb.toString();
    }
    public static String readStringNZ(ByteBuffer buf, int offset, int n) {
        StringBuffer sb = new StringBuffer();
        byte b;
        for (; n > 0 && buf.position() < buf.limit(); n--) {
            b = buf.get(offset++);
            if (b == 0)
                break;
            sb.append((char)b);
        }
        return sb.toString();
    }
   public static short getUnsignedByte (ByteBuffer bb) throws IOException
   {
      return ((short)(bb.get() & 0xff));
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

    public static int parseAddress(String value)
    {
    	int address = 0;

    	if (value == null) {
    		return address;
    	}

        if (value.startsWith("0x"))
            value = value.substring(2);
        
    	if (Integer.SIZE == 32 && value.length() == 8 && value.startsWith("8")) {
    		address = Integer.parseInt(value.substring(1), 16);
    		address |= 0x80000000;
    	} else {
    		address = Integer.parseInt(value, 16);
    	}

    	return address;
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
}
