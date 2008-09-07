/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jpcsp.filesystems.umdiso;

import java.io.*;
import jpcsp.filesystems.*;
import jpcsp.util.Utilities.*;

/**
 *
 * @author gigaherz
 */
public class UmdIsoFile extends SeekableInputStream {

    private int startSectorNumber;
    private int currentSectorNumber;
    private long currentOffset;
    private long maxOffset;
    
    private byte[] currentSector;
    private int sectorOffset;
    
    UmdIsoReader internalReader;
    
    public UmdIsoFile(UmdIsoReader reader, int startSector, long lengthInBytes) throws IOException
    {
        startSectorNumber = startSector;
        currentSectorNumber = startSectorNumber;
        currentOffset = 0;
        currentSector = reader.readSector(startSector);
        maxOffset = lengthInBytes;
        sectorOffset = 0;
        internalReader = reader;
    }
    
    private int Ubyte(byte b)
    {
        return ((int)b)&255;
    }
    
    @Override
    public int read() throws IOException
    {
        // I hate Java. Actuall I hate whoever decided to make "byte" signed,
        // and then decided that streams would have a read() function returning a value [0..255], and -1 for EOF
        
        if(currentOffset == maxOffset)
            return -1; //throw new java.io.EOFException();
        
        if(sectorOffset==2048)
        {
            currentSectorNumber++;
            currentSector = internalReader.readSector(currentSectorNumber);
            sectorOffset = 0;
        }
        currentOffset++;
        
        int debuggingVariable = Ubyte(currentSector[sectorOffset++]); // make unsigned
        
        assert (debuggingVariable>=0);
        
        return debuggingVariable;
        
    }
    
    @Override
    public void reset() throws IOException
    {
        seek(0);
    }
    
    @Override
    public long skip(long n) throws IOException
    {
        long oldOffset = currentOffset;
        long newOffset = currentOffset + n;
        
        if(n<0)
            return n;
        
        if(newOffset>=maxOffset)
        {
            newOffset = maxOffset;
        }
        
        seek(currentOffset+n);
        
        return currentOffset-oldOffset;
    }
    
    @Override
    public long length()
    {
        return maxOffset;
    }
    
    @Override
    public void seek(long offset) throws IOException
    {
        long endOffset = offset;
 
        if(offset<0)
            throw new IOException("Seek offset " + offset + " out of bounds.");
        
        int oldSectorNumber = currentSectorNumber;
        long newOffset = endOffset;
        int newSectorNumber = startSectorNumber + (int)(newOffset / 2048);
        if(oldSectorNumber != newSectorNumber)
        {
            currentSector = internalReader.readSector(newSectorNumber);
        }
        currentOffset = newOffset;
        currentSectorNumber = newSectorNumber;
        sectorOffset = (int)(currentOffset % 2048);
    }
    
    @Override
    public long getFilePointer() throws IOException
    {
        return currentOffset;
    }
    
    
    @Override
    public byte readByte() throws IOException
    {
        if(currentOffset>=maxOffset)
            throw new EOFException();
        
        return (byte)read();
    }
    
    @Override
    public short readShort() throws IOException
    {
        return (short)(readUnsignedByte() | (((int)readByte())<<8));
    }
    
    @Override
    public int readInt() throws IOException
    {
        return (readUnsignedByte() | (((int)readUnsignedByte())<<8) | (((int)readUnsignedByte())<<16) | (((int)readByte())<<24));
    }
    
    @Override
    public int readUnsignedByte() throws IOException
    {
        if(currentOffset>=maxOffset)
            throw new EOFException();
        
        return read();
    }
    
    @Override
    public int readUnsignedShort() throws IOException
    {
        return ((int)readShort())&0xFFFF;
    }
    
    @Override
    public long readLong() throws IOException
    {
        return (((long)readInt())&0xFFFFFFFFl) | (((long)readInt())<<32);
    }
    
    @Override
    public float readFloat() throws IOException
    {
        if(currentOffset>=maxOffset)
            throw new EOFException();
        
        DataInputStream s = new DataInputStream(this);
        return s.readFloat();
    }
    
    @Override
    public double readDouble() throws IOException
    {
        if(currentOffset>=maxOffset)
            throw new EOFException();
        
        DataInputStream s = new DataInputStream(this);
        return s.readDouble();
    }
    
    @Override
    public boolean readBoolean() throws IOException
    {
        return (readUnsignedByte()!=0);
    }
    
    @Override
    public char readChar() throws IOException
    {
        if(currentOffset>=maxOffset)
            throw new EOFException();
        
        DataInputStream s = new DataInputStream(this);
        return s.readChar();
    }
    
    @Override
    public String readUTF() throws IOException
    {
        if(currentOffset>=maxOffset)
            throw new EOFException();

        DataInputStream s = new DataInputStream(this);
        return s.readUTF();
    }
    
    @Override
    public String readLine() throws IOException
    {
        if(currentOffset>=maxOffset)
            throw new EOFException();
        
        DataInputStream s = new DataInputStream(this);
        return s.readLine();
    }
    
    @Override
    public void readFully(byte[] b, int off, int len) throws IOException
    {
        if(currentOffset>=maxOffset)
            throw new EOFException();
        
        read(b, off, len);
    }
    
    @Override
    public void readFully(byte[] b) throws IOException
    {
        if(currentOffset>=maxOffset)
            throw new EOFException();

        read(b);
    }
    
    @Override
    public int skipBytes(int bytes) throws IOException
    {
        return (int)skip(bytes);
    }

}
