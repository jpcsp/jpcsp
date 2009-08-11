/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jpcsp.filesystems;

import java.io.*;

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
