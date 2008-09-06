/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jpcsp.filesystems.umdiso.iso9660;

import java.io.*;

/**
 *
 * @author gigaherz
 */
public class Iso9660File {

    private int fileLBA;
    private int fileSize;
    private int fileProperties;
    // padding: byte[3]
    private String fileName; //[128+1];
    //Iso9660Date date; // byte[7]
    
    private int Ubyte(byte b)
    {
        return ((int)b)&255;
    }
    
    public Iso9660File(byte[] data, int length) throws IOException
    {

        /*
 -- 1           Length of Directory Record (LEN-DR) -- read by the Iso9660Directory
2           Extended Attribute Record Length
3 to 10     Location of Extent
11 to 18    Data Length
19 to 25    Recording Date and Time
26          File Flags
27          File Unit Size
28          Interleave Gap Size
29 to 32    Volume Sequence Number
33          Length of File Identifier (LEN_FI)
34 to (33+LEN_FI)   File Identifier
(34 + LEN_FI)   Padding Field

*/        

        fileLBA = Ubyte(data[1]) | (Ubyte(data[2])<<8) | (Ubyte(data[3])<<16) | (data[4]<<24);
        fileSize = Ubyte(data[9]) | (Ubyte(data[10])<<8) | (Ubyte(data[11])<<16) | (data[12]<<24);
        fileProperties = data[24];
        
        if((fileLBA<0)||(fileSize<0))
        {
            throw new IOException("WTF?! Size of lba < 0?!");
        }

        int fileNameLength = data[31];
        
        fileName="";
        for(int i=0;i<fileNameLength;i++)
        {
            char c =(char)(data[32+i]);
            if(c==0)
                c='.';
                
            fileName += c;
        }
        
    }

    public int getLBA()
    {
        return fileLBA;
    }
    
    public int getSize()
    {
        return fileSize;
    }
    
    public int getProperties()
    {
        return fileProperties;
    }
    
    public String getFileName()
    {
        return fileName;
    }
}
