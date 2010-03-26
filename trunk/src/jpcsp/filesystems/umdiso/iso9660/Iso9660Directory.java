package jpcsp.filesystems.umdiso.iso9660;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import jpcsp.filesystems.umdiso.*;

/**
 *
 * @author gigaherz
 */
public class Iso9660Directory {

    private List<Iso9660File> files;

    public Iso9660Directory(UmdIsoReader r, int directorySector, int directorySize) throws IOException
    {
        // parse directory sector
        UmdIsoFile dataStream = new UmdIsoFile(r, directorySector, directorySize, null, null);
        
        files = new ArrayList<Iso9660File>();

        byte[] b;

        while(directorySize>=1)
        {
            int entryLength = dataStream.read();

            // This is assuming that the padding bytes are always filled with 0's.
            if(entryLength==0)
            {
            	directorySize--;
                continue;
            }

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
            if(file.equalsIgnoreCase(fileName))
            {
                return i;
            }
        }

        throw new FileNotFoundException("File " + fileName + " not found in directory.");
    }
    
    public String[] getFileList() throws FileNotFoundException
    {
        String[] list = new String[files.size()];
        for(int i=0;i<files.size();i++)
        {
            list[i] = files.get(i).getFileName();
        }
        return list;
    }
    
}
