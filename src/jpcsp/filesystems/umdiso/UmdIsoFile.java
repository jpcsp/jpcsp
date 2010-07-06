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
package jpcsp.filesystems.umdiso;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Date;

import jpcsp.filesystems.SeekableInputStream;

/**
 *
 * @author gigaherz
 */
public class UmdIsoFile extends SeekableInputStream {
	public static final int sectorLength = 2048;
    private int startSectorNumber;
    private int currentSectorNumber;
    private long currentOffset;
    private long maxOffset;
    private Date timestamp;
    private String name;

    private byte[] currentSector;
    private int sectorOffset;

    UmdIsoReader internalReader;

    public UmdIsoFile(UmdIsoReader reader, int startSector, long lengthInBytes, Date timestamp, String name) throws IOException
    {
        startSectorNumber = startSector;
        currentSectorNumber = startSectorNumber;
        currentOffset = 0;

        // Some ISO directory entries indicate a file length past the size of the complete ISO.
        // Truncate the file length in that case to the available sectors.
        // This might be some sort of copy protection?
        // E.g. "Kamen no Maid Guy: Boyoyon Battle Royale"
        int endSectorNumber = startSectorNumber + (int) ((lengthInBytes + sectorLength - 1) / sectorLength);
        if (endSectorNumber >= reader.numSectors) {
        	endSectorNumber = reader.numSectors - 1;
        	lengthInBytes = (endSectorNumber - startSector + 1) * sectorLength;
        }

        if (lengthInBytes == 0) {
        	currentSector = null;
        } else {
        	currentSector = reader.readSector(startSector);
        }
        maxOffset = lengthInBytes;
        sectorOffset = 0;
        internalReader = reader;
        this.timestamp = timestamp;
    }

    private int Ubyte(byte b)
    {
        return (b)&255;
    }

    @Override
    public int read() throws IOException
    {
        // I hate Java. Actually I hate whoever decided to make "byte" signed,
        // and then decided that streams would have a read() function returning a value [0..255], and -1 for EOF

        if(currentOffset == maxOffset)
            return -1; //throw new java.io.EOFException();

        checkSectorAvailable();
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

        if(n<0)
            return n;

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
        int newSectorNumber = startSectorNumber + (int)(newOffset / sectorLength);
        if(oldSectorNumber != newSectorNumber)
        {
            currentSector = internalReader.readSector(newSectorNumber);
        }
        currentOffset = newOffset;
        currentSectorNumber = newSectorNumber;
        sectorOffset = (int)(currentOffset % sectorLength);
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
        return (short)(readUnsignedByte() | ((readByte())<<8));
    }

    @Override
    public int readInt() throws IOException
    {
        return (readUnsignedByte() | ((readUnsignedByte())<<8) | ((readUnsignedByte())<<16) | ((readByte())<<24));
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
        return (readShort())&0xFFFF;
    }

    @Override
    public long readLong() throws IOException
    {
        return ((readInt())&0xFFFFFFFFl) | (((long)readInt())<<32);
    }

    @Override
    public float readFloat() throws IOException
    {
        if(currentOffset>=maxOffset)
            throw new EOFException();

        return Float.intBitsToFloat(readInt());
    }

    @Override
    public double readDouble() throws IOException
    {
        if(currentOffset>=maxOffset)
            throw new EOFException();

        return Double.longBitsToDouble(readLong());
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

        int ch1 = read();
        int ch2 = read();
        if ((ch1 | ch2) < 0)
            throw new EOFException();
        return (char)((ch1 << 8) + (ch2 << 0));
    }

    @Override
    public String readUTF() throws IOException
    {
        if(currentOffset>=maxOffset)
            throw new EOFException();

        return DataInputStream.readUTF(this);
    }

    @Override
    public String readLine() throws IOException
    {
        if(currentOffset>=maxOffset)
            throw new EOFException();

        StringBuilder s = new StringBuilder();
        char c=0;
        do {
            c = readChar();

            if((c=='\n')||(c!='\r'))
            {
                break;
            }
            s.append(c);
        } while(true);

        return s.toString();
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

    public Date getTimestamp()
    {
    	return timestamp;
    }

    public int getStartSector()
    {
    	return startSectorNumber;
    }

    public String getName()
    {
    	if (name == null)
    	{
    		name = internalReader.getFileName(startSectorNumber);
    	}

    	return name;
    }

    private int readInternal(byte[] b, int off, int len) throws IOException
    {
    	if (len > 0)
    	{
    		if (len > (maxOffset - currentOffset))
    		{
    			len = (int) (maxOffset - currentOffset);
    		}
    		System.arraycopy(currentSector, sectorOffset, b, off, len);
    		sectorOffset += len;
    		currentOffset += len;
    	}

    	return len;
    }

    private void checkSectorAvailable() throws IOException
    {
    	if (sectorOffset == sectorLength && currentOffset < maxOffset) {
    		currentSectorNumber++;
    		currentSector = internalReader.readSector(currentSectorNumber);
    		sectorOffset = 0;
    	}
    }

    @Override
	public int read(byte[] b, int off, int len) throws IOException
	{
    	if (b == null)
    	{
    		throw new NullPointerException();
    	}
    	if (off < 0 || len < 0 || len > (b.length - off))
    	{
    		throw new IndexOutOfBoundsException();
    	}

		if (len > (maxOffset - currentOffset))
		{
			len = (int) (maxOffset - currentOffset);
		}

		int totalLength = 0;

		int firstSector = readInternal(b, off, Math.min(len, sectorLength - sectorOffset));
		off += firstSector;
		len -= firstSector;
		totalLength += firstSector;

		// Read whole sectors
		if (len >= sectorLength)
		{
			int numberSectors = len / sectorLength;
			internalReader.readSectors(currentSectorNumber + 1, numberSectors, b, off);
    		currentSectorNumber += numberSectors;
    		sectorOffset = sectorLength;
			int n = numberSectors * sectorLength;
    		currentOffset += n;
			checkSectorAvailable();
			off += n;
			len -= n;
			totalLength += n;
		}

		if (len > 0) {
    		checkSectorAvailable();
    		int lastSector = readInternal(b, off, len);
    		totalLength += lastSector;
		}

		return totalLength;
	}
}