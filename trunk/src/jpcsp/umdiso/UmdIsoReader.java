/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jpcsp.umdiso;

import java.io.*;
import jpcsp.umdiso.iso9660.*;

import java.util.zip.*;

/**
 *
 * @author gigaherz
 */
public class UmdIsoReader {
    
    RandomAccessFile fileReader;
    
    enum FileFormat {
        Uncompressed,
        CompressedCSO,
        CompressedDAX, // not implemented yet
        //...
        Unknown
    }
    
    FileFormat format;
    
    int numSectors;         //
    int[] sectorOffsets;    // for CSO/DAX
    
    int offsetShift; // for CSO
    
    private int Ubyte(byte b)
    {
        return (256+b)&255;
    }
    
    private int BytesToInt(byte[] bytes, int offset) throws ArrayIndexOutOfBoundsException
    {
        return Ubyte(bytes[offset+0]) | (Ubyte(bytes[offset+1])<<8) | (Ubyte(bytes[offset+2])<<16) | (bytes[offset+3]<<24);
    }
     
    public UmdIsoReader(String umdFilename) throws IOException, FileNotFoundException
    {
        fileReader = new RandomAccessFile(umdFilename,"r");
      
        /*
            u32 'CISO'
            u32 0?
            u32 image size in bytes (why? it could have been sectors and make thigns simpler!)
            u32 sector size? (00000800 = 2048 = sector size)
            u32 ? (1)
            u32[] sector offsets (as many as image size / sector size, I guess)
        */
        
        format=FileFormat.Unknown;
        
        byte[] id = new byte[24];

        fileReader.seek(0);
        fileReader.read(id);
        
        if((((char)id[0])=='C')&&
           (((char)id[1])=='I')&&
           (((char)id[2])=='S')&&
           (((char)id[3])=='O'))
        {
            format = FileFormat.CompressedCSO;
            
            int lenInbytes = BytesToInt(id,8);
            int sectorSize = BytesToInt(id,16);
            
            int version = Ubyte(id[20]);
            offsetShift = Ubyte(id[21]);
            
            numSectors = lenInbytes/sectorSize;

            sectorOffsets = new int[numSectors+1];
            
            byte[] offsetData = new byte[(numSectors+1)*4];
            
            fileReader.read(offsetData);
            
            for(int i=0;i<=numSectors;i++)
            {
                sectorOffsets[i] = BytesToInt(offsetData, i*4);
            }
            
            //sectorOffsets[numSectors] = (int)(fileReader.length() - sectorOffsets[numSectors-1]);
            return;
        }
        
        id = new byte[6];

        fileReader.seek(16*2048);
        fileReader.read(id);
        
        if((((char)id[1])=='C')&&
           (((char)id[2])=='D')&&
           (((char)id[3])=='0')&&
           (((char)id[4])=='0')&&
           (((char)id[5])=='1'))
        {
             // assume uncompressed for now
            numSectors = (int)(fileReader.length() / 2048);
            format = FileFormat.Uncompressed;
            return;
        }
        
        throw new IOException("Unsupported file format or corrupt file.");
    }
    
    public byte[] readSector(int sectorNumber) throws IOException
    {
        if((sectorNumber<0)||(sectorNumber>=numSectors))
            throw new ArrayIndexOutOfBoundsException("Sector number " + sectorNumber + " out of bounds.");

        if(format==FileFormat.Uncompressed)
        {
            byte[] bytes = new byte[2048];

            fileReader.seek(2048 * sectorNumber);
            fileReader.read(bytes);
            return bytes;
        }

        try {
            if(format==FileFormat.CompressedCSO)
            {
                int sectorOffset = sectorOffsets[sectorNumber];
                int sectorEnd    = sectorOffsets[sectorNumber+1];

                if(sectorOffset<0)
                {
                    int realOffset = (sectorOffset&0x7fffffff)<<offsetShift;

                    byte[] bytes = new byte[2048];

                    fileReader.seek(realOffset);
                    fileReader.read(bytes);
                    return bytes;
                }

                sectorEnd <<= offsetShift;
                sectorOffset <<= offsetShift;

                int compressedLength = (sectorEnd - sectorOffset);

                byte[] compressedData = new byte[compressedLength];

                fileReader.seek(sectorOffset);
                fileReader.read(compressedData);

                Inflater inf = new Inflater();

                byte[] data = new byte[2048];

                inf.setInput(compressedData);
                inf.inflate(data);
                inf.end();
            }
        }
        catch(DataFormatException e) { throw new IOException(e.getMessage(),e.getCause()); }

        throw new IOException("Unsupported file format or corrupt file.");
    }
    
    public UmdIsoFile getFile(String filePath) throws IOException, FileNotFoundException, DataFormatException
    {
        Iso9660Directory dir = new Iso9660Handler(this);

        String[] path = filePath.split("[\\/]");
        
        Iso9660File info = null;
        // walk through path
        for(int i=0;i<path.length;)
        {
            if(path[i].compareToIgnoreCase(".")==0)
            {
                //do nothing
            }
            else if(path[i].compareToIgnoreCase("..")==0)
            {
                i=Math.max(0,i-1);
            }
            else
            {
                String pathName = path[i];
                int index = dir.getFileIndex(path[i]);
                info = dir.getEntryByIndex(index);
                dir  = null;
                if((info.getProperties()&2)==2) // if it's a directory
                {
                    dir  = new Iso9660Directory(this, info.getLBA(), info.getSize());
                    info = null;
                }
                i++;
            }
        }
        
        if(info==null) throw new FileNotFoundException("File '" + filePath + "' not found or not a file.");
        
        int fileStart    = info.getLBA();
        long fileLength  = info.getSize();
        
        return new UmdIsoFile(this, fileStart, fileLength);
    }
    
}
