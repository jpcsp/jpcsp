/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jpcsp.umdiso;

import java.io.FileNotFoundException;
import java.io.IOException;

import java.io.*;
import java.security.InvalidParameterException;
import jpcsp.umdiso.iso9660.*;

/**
 *
 * @author gigaherz
 */
public class UmdIsoReader {
    
    RandomAccessFile fileReader;
     
    public UmdIsoReader(String umdFilename) throws IOException, FileNotFoundException
    {
        fileReader = new RandomAccessFile(umdFilename,"r");
    }
    
    public byte[] readSector(int sectorNumber) throws IOException
    {
        byte[] bytes = new byte[2048];
        
        if(sectorNumber<0)
            throw new InvalidParameterException("Negative sector number not allowed.");
        
        fileReader.seek(2048 * sectorNumber);
        fileReader.read(bytes);
        return bytes;
    }
    
    public UmdIsoFile getFile(String filePath) throws IOException, FileNotFoundException
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
