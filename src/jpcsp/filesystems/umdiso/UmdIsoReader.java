/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jpcsp.filesystems.umdiso;

import java.io.*;
import java.util.Date;

import jpcsp.filesystems.umdiso.iso9660.*;
import jpcsp.util.Utilities;

//import java.util.zip.*;
import org.bolet.jgz.*;

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
    long[] sectorOffsets;    // for CSO/DAX

    int offsetShift; // for CSO

    String fileName;

    private int Ubyte(byte b)
    {
        return ((int)b)&255;
    }

    private int BytesToInt(byte[] bytes, int offset) throws ArrayIndexOutOfBoundsException
    {
        return Ubyte(bytes[offset+0]) | (Ubyte(bytes[offset+1])<<8) | (Ubyte(bytes[offset+2])<<16) | (bytes[offset+3]<<24);
    }

    public UmdIsoReader(String umdFilename) throws IOException, FileNotFoundException
    {
        fileName = umdFilename;
        fileReader = new RandomAccessFile(umdFilename,"r");

        /*
            u32 'CISO'
            u32 0?
            u32 image size in bytes (why? it could have been sectors and make thigns simpler!)
            u32 sector size? (00000800 = 2048 = sector size)
            u32 ? (1)
            u32[] sector offsets (as many as image size / sector size, I guess)
        */

        format = FileFormat.Uncompressed;
        numSectors = (int)(fileReader.length() / 2048);

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

            //int version = Ubyte(id[20]);
            offsetShift = Ubyte(id[21]);

            numSectors = lenInbytes/sectorSize;

            sectorOffsets = new long[numSectors+1];

            byte[] offsetData = new byte[(numSectors+1)*4];

            fileReader.read(offsetData);

            for(int i=0;i<=numSectors;i++)
            {
                sectorOffsets[i] = ((long)BytesToInt(offsetData, i*4))&0xFFFFFFFFl;
                if(i>0)
                {
                    if((sectorOffsets[i]&0x7FFFFFFF)<(sectorOffsets[i-1]&0x7FFFFFFF))
                    {
                        throw new IOException("Invalid offset [" + i + "]: " + sectorOffsets[i] + "<" + sectorOffsets[i-1]);
                    }
                }
            }
        }

        // when we reach here, we assume it's either a .ISO or a .CSO
        // but we still need to make sure of that

        id = new byte[6];

        UmdIsoFile f = new UmdIsoFile(this, 16, 2048, null);
        f.read(id);

        if((((char)id[1])=='C')&&
           (((char)id[2])=='D')&&
           (((char)id[3])=='0')&&
           (((char)id[4])=='0')&&
           (((char)id[5])=='1'))
        {
            if(format == FileFormat.Uncompressed)
            {
                numSectors = (int)(fileReader.length() / 2048);
            }
            return;
        }

        format = FileFormat.Unknown;
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

        if(format==FileFormat.CompressedCSO)
        {
            long sectorOffset = sectorOffsets[sectorNumber];
            long sectorEnd    = sectorOffsets[sectorNumber+1];

            if((sectorOffset&0x80000000)!=0)
            {
                long realOffset = (sectorOffset&0x7fffffff)<<offsetShift;

                byte[] bytes = new byte[2048];

                fileReader.seek(realOffset);
                fileReader.read(bytes);
                return bytes;
            }

            sectorEnd    = (sectorEnd    & 0x7fffffff ) << offsetShift;
            sectorOffset = (sectorOffset & 0x7fffffff ) << offsetShift;

            int compressedLength = (int)(sectorEnd - sectorOffset);
            if(compressedLength<0)
            {
                return new byte[2048];
            }

            byte[] compressedData = new byte[compressedLength];

            fileReader.seek(sectorOffset);
            fileReader.read(compressedData);

            byte[] data = new byte[2048];

            try {
                Inflater inf = new Inflater();

                ByteArrayInputStream b = new ByteArrayInputStream(compressedData);
                inf.reset(b);
                //inf.setRawStream(b);
                inf.readAll(data,0,data.length);
            }
            catch(IOException e)
            {
                System.err.print("Exception while uncompressing sector from " + fileName);
                e.printStackTrace();
            }

            return data;
        }

        throw new IOException("Unsupported file format or corrupt file.");
    }

    private Iso9660File getFileEntry(String filePath) throws IOException, FileNotFoundException
    {
        Iso9660Directory dir = new Iso9660Handler(this);

        String[] path = filePath.split("[\\/]");

        Iso9660File info = null;
        
        // walk through path
        for(int i=0;i<path.length;)
        {
            if(path[i].compareTo(".")==0)
            {
                if(i==(path.length-1))
                {
                    break;
                }
                //do nothing
            }
            else if(path[i].compareTo("..")==0)
            {
                i=Math.max(0,i-1);
            }
            else
            {
                String pathName = path[i];
                int index = dir.getFileIndex(pathName);
                info = dir.getEntryByIndex(index);
                dir  = null;
                if((info.getProperties()&2)==2) // if it's a directory
                {
                    dir  = new Iso9660Directory(this, info.getLBA(), info.getSize());
                }
                i++;
            }
        }

        return info;
    }

    public UmdIsoFile getFile(String filePath) throws IOException, FileNotFoundException
    {
        int fileStart;
        long fileLength;
        Date timestamp = null;

        if (filePath != null && filePath.startsWith("sce_lbn"))
        {
            //
            // Direct sector access on UMD is using the following file name syntax:
            //     sce_lbnSSSS_sizeLLLL
            // where SSSS is the index of the first sector
            //       LLLL is the length in bytes
            // E.g.
            //       disc0:/sce_lbn0x5fa0_size0x1428
            //
    		filePath = filePath.substring(7);
    		int sep = filePath.indexOf("_size");
    		fileStart = (int) Utilities.parseLong(filePath.substring(0, sep));
    		fileLength = Utilities.parseLong(filePath.substring(sep + 5));

    		if (fileStart < 0 || fileStart >= numSectors) {
    			throw new IOException("File '" + filePath + "': Invalid Start Sector");
    		}
        }
        else
        {
	        Iso9660File info = getFileEntry(filePath);
	        if(info!=null)
	        {
	            if((info.getProperties()&2)==2) // if it's a directory
	            {
	                info=null;
	            }
	        }
	
	        if(info==null) throw new FileNotFoundException("File '" + filePath + "' not found or not a file.");

	        fileStart = info.getLBA();
	        fileLength = info.getSize();
	        timestamp = info.getTimestamp();
        }

        return new UmdIsoFile(this, fileStart, fileLength, timestamp);
    }
 
    public String[] listDirectory(String filePath) throws IOException, FileNotFoundException
    {
        Iso9660Directory dir = null;
        
        if(filePath.compareTo("")==0)
        {
            dir = new Iso9660Handler(this);
        }
        else
        {
            Iso9660File info = getFileEntry(filePath);

            if(info!=null)
            {
                if((info.getProperties()&2)==2) // if it's a directory
                {
                    dir  = new Iso9660Directory(this, info.getLBA(), info.getSize());
                }
            }
        }
        
        if(dir==null) throw new FileNotFoundException("File '" + filePath + "' not found or not a directory.");
        
        return dir.getFileList();
    }   

    public int getFileProperties(String filePath) throws IOException, FileNotFoundException
    {
        if(filePath.compareTo("")==0)
        {
            return 2;
        }

        Iso9660File info = getFileEntry(filePath);
        
        if(info==null) throw new FileNotFoundException("File '" + filePath + "' not found.");

        return info.getProperties();
    }   
    
    public boolean isDirectory(String filePath) throws IOException, FileNotFoundException
    {
        return (getFileProperties(filePath)&2)==2;
    }   
    
    public String getFilename()
    {
        return fileName;
    }
}
