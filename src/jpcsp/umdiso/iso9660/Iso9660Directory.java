/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jpcsp.umdiso.iso9660;

import java.io.*;
import java.util.Vector;
import jpcsp.umdiso.*;

/**
 *
 * @author gigaherz
 */
public class Iso9660Directory {

    private Vector<Iso9660File> files;
    
    public Iso9660Directory(UmdIsoReader r, int directorySector, int directorySize) throws IOException
    {
        // parse directory sector
        UmdIsoFile dataStream = new UmdIsoFile(r, directorySector, directorySize);
        
        files = new Vector<Iso9660File>();
        
        byte[] b;
        
        int currentPos = 0;
        
        while(directorySize>0)
        {
            int entryLength = dataStream.read();
            
            if(entryLength==0)
                break;
            
            directorySize-=entryLength;
            
            b = new byte[entryLength-1];
            dataStream.read(b);
            
            Iso9660File file = new Iso9660File(b,b.length);
            files.add(file);
        }
        
    }
    
    public Iso9660File getEntryByIndex(int index) throws ArrayIndexOutOfBoundsException
    {
        return files.get(index);
    }
    
    public int getFileIndex(String fileName) throws FileNotFoundException
    {
        for(int i=0;i<files.size();i++)
        {
            String file = files.get(i).getFileName();
            if(file.compareToIgnoreCase(fileName)==0)
            {
                return i;
            }
        }
        
        throw new FileNotFoundException("File " + fileName + " not found in directory.");
    }

}
