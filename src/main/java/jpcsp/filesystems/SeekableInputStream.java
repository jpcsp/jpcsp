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
package jpcsp.filesystems;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author gigaherz
 */
public abstract class SeekableInputStream extends InputStream implements SeekableDataInput {


    @Override
    abstract public void seek(long position) throws IOException;

    @Override
    abstract public long length() throws IOException;

    @Override
    abstract public int read() throws IOException;

    @Override
    abstract public byte readByte() throws IOException;

    @Override
    abstract public short readShort() throws IOException;

    @Override
    abstract public int readInt() throws IOException;

    @Override
    abstract public int readUnsignedByte() throws IOException;

    @Override
    abstract public int readUnsignedShort() throws IOException;

    @Override
    abstract public long readLong() throws IOException;

    @Override
    abstract public float readFloat() throws IOException;

    @Override
    abstract public double readDouble() throws IOException;

    @Override
    abstract public boolean readBoolean() throws IOException;

    @Override
    abstract public char readChar() throws IOException;

    @Override
    abstract public String readUTF() throws IOException;

    @Override
    abstract public String readLine() throws IOException;

    @Override
    abstract public void readFully(byte[] b, int off, int len) throws IOException;

    @Override
    abstract public void readFully(byte[] b) throws IOException;

    @Override
    abstract public int skipBytes(int bytes) throws IOException;
}