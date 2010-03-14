package jpcsp.filesystems.umdiso.iso9660;

import jpcsp.filesystems.umdiso.*;
import java.io.*;

/**
 *
 * @author gigaherz
 */
public class Iso9660Handler extends Iso9660Directory {

    private Iso9660Directory internalDir;
    
    public Iso9660Handler(UmdIsoReader r) throws IOException
    {
        super(r, 0, 0);
        
        byte[] sector = r.readSector(16);
        ByteArrayInputStream byteStream = new ByteArrayInputStream(sector);
        
        byteStream.skip(157); // reach rootDirTocHeader
        
        byte[] b = new byte[38];
        
        byteStream.read(b);
        Iso9660File rootDirEntry = new Iso9660File(b,b.length);
        
        int rootLBA = rootDirEntry.getLBA();
        int rootSize = rootDirEntry.getSize();
        
        internalDir = new Iso9660Directory(r, rootLBA, rootSize);
    }

    @Override
    public Iso9660File getEntryByIndex(int index) throws ArrayIndexOutOfBoundsException
    {
        return internalDir.getEntryByIndex(index);
    }
    
    @Override
    public int getFileIndex(String fileName) throws FileNotFoundException
    {
        return internalDir.getFileIndex(fileName);
    }
 
    @Override
    public String[] getFileList() throws FileNotFoundException
    {
        return internalDir.getFileList();
    }
     
}
