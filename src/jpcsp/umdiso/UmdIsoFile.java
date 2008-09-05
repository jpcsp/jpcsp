/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jpcsp.umdiso;

import java.io.*;
import java.util.zip.DataFormatException;

/**
 *
 * @author gigaherz
 */
public class UmdIsoFile extends InputStream {

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
    
    @Override
    public int read() throws IOException
    {
        if(currentOffset == maxOffset)
            throw new java.io.EOFException();
        
        if(sectorOffset==2048)
        {
            currentSectorNumber++;
            currentSector = internalReader.readSector(currentSectorNumber);
            sectorOffset = 0;
        }
        currentOffset++;
        return currentSector[sectorOffset++];
        
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
        
        return seek(currentOffset+n)-currentOffset;
    }
    
    public long getSize()
    {
        return maxOffset;
    }
    
    public long seek(long offset) throws IOException
    {
        long endOffset = offset;
 
        if((offset<0)||(offset>=maxOffset))
            throw new IndexOutOfBoundsException("Seek offset " + offset + " out of bounds.");
        
        try {
            int oldSectorNumber = currentSectorNumber;
            long newOffset = endOffset;
            int newSectorNumber = (int)(newOffset / 2048);
            if(oldSectorNumber != newSectorNumber)
            {
                currentSector = internalReader.readSector(newSectorNumber);
            }
            currentOffset = newOffset;
            currentSectorNumber = newSectorNumber;
            sectorOffset = (int)(currentOffset % 2048);
            return endOffset;
        }
        catch(IOException e)
        {
            return currentOffset;
        }
    }
}
