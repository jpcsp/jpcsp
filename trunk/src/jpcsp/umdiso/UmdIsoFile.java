/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jpcsp.umdiso;

import java.io.*;

/**
 *
 * @author gigaherz
 */
public class UmdIsoFile extends java.io.InputStream {

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
        currentSectorNumber = startSectorNumber;
        currentSector = internalReader.readSector(currentSectorNumber);
        currentOffset = 0;
        sectorOffset = 0;
    }
    
    @Override
    public long skip(long n)
    {
        long skipped=0;
        long oldOffset = currentOffset;
        long endOffset = currentOffset + n;
        
        if(endOffset>=maxOffset)
        {
            try {
                long newOffset = maxOffset;
                int newSectorNumber = (int)(newOffset / 2048);
                currentSector = internalReader.readSector(newSectorNumber);
                currentOffset = newOffset;
                currentSectorNumber = newSectorNumber;
                sectorOffset = (int)(currentOffset % 2048);
                return (maxOffset - oldOffset);
            }
            catch(IOException e)
            {
                return 0;
            }
        }
        
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
            return (endOffset - oldOffset);
        }
        catch(IOException e)
        {
            return 0;
        }
    }
    
    public long getSize()
    {
        return maxOffset;
    }
    
}
