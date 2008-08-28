/*
Function:
- HLE everything in http://psp.jim.sh/pspsdk-doc/pspiofilemgr_8h.html


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

package jpcsp.HLE;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import jpcsp.Emulator;
import jpcsp.GeneralJpcspException;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import static jpcsp.util.Utilities.*;
/**
 *
 * @author George
 */
public class pspiofilemgr {
    private static pspiofilemgr  instance;
    private static HashMap<Integer, IOInfo> filelist;
    public final static int PSP_O_RDONLY  = 0x0001;
    public final static int PSP_O_WRONLY  = 0x0002;
    public final static int PSP_O_RDWR    = (PSP_O_RDONLY | PSP_O_WRONLY);
    public final static int PSP_O_NBLOCK  = 0x0004;
    public final static int PSP_O_DIROPEN = 0x0008; // Internal use for dopen
    public final static int PSP_O_APPEND  = 0x0100;
    public final static int PSP_O_CREAT   = 0x0200;
    public final static int PSP_O_TRUNC   = 0x0400;
    public final static int PSP_O_EXCL    = 0x0800;
    public final static int PSP_O_NOWAIT  = 0x8000;
    
    private boolean debug = true; //enable/disable debug
    public static pspiofilemgr  get_instance() 
    {
        if (instance == null) {
            instance = new pspiofilemgr();
        }
        return instance;
    }
    public pspiofilemgr()
    {
      filelist = new HashMap<Integer, IOInfo>();   
    }
    File f;
    public void sceIoOpen(int a0 ,int a1, int a2)
    {
       String name = readStringZ(Memory.get_instance().mainmemory, (a0 & 0x3fffffff) - MemoryMap.START_RAM);
       System.out.println("sceIoOpen name = " + name + " flags = " + Integer.toHexString(a1) + " mode = " + Integer.toOctalString(a2));
       IOInfo file = new IOInfo(name,a1,a2);
       Emulator.getProcessor().gpr[2]=file.uid;
            
    }
    public void sceIoClose(int a0)
    {
        if(debug)System.out.println("sceIoClose uid" + a0);
       
        try{
         SceUIDMan.get_instance().checkUidPurpose(a0, "IOFileManager-File");
        }
        catch(GeneralJpcspException e)//some homebrew i tested try to close a file that hasn't be opened? is that a check or something?
        { 
            System.out.println("sceIoClose - trying to close a file that hasn't be open uid=" + a0); 
            Emulator.getProcessor().gpr[2]=0;
            return;
        }
        IOInfo file = filelist.get(a0);
        try{
            SceUIDMan.get_instance().releaseUid(file.uid, "IOFileManager-File");
          }
         catch(Exception e)
         {
             e.printStackTrace();   
         }
        Emulator.getProcessor().gpr[2]=0;
    }

    public void sceIoWrite(int a0 , int a1,int a2)
    {
         System.out.println("sceIoWrite uid=" + a0 + " data = " + Integer.toHexString(a1) + " size = "+a2);
        try{
         SceUIDMan.get_instance().checkUidPurpose(a0, "IOFileManager-File");
        }
        catch(GeneralJpcspException e)
        { 
            e.printStackTrace(); 
        }
        IOInfo file = filelist.get(a0);
        try
        {
          if ((a1 >= MemoryMap.START_RAM ) && (a1 <= MemoryMap.END_VRAM)) 
          {
           FileOutputStream fop=new FileOutputStream(file.f);
           fop.write(Memory.get_instance().mainmemory.array(), a1 - MemoryMap.START_RAM, a2);
           fop.close();
          }
          else
          {
            System.out.println("sceIoWrite - Unsupported memory address for write");   
          }
        }
        catch(IOException e)
        {
           e.printStackTrace();   
        }
        Emulator.getProcessor().gpr[2]= a2;//return the number of bytes written
    }
    //the following gets the filepath from memstick manager. 
    String filepath;
    public void getfilepath(String filepath)
    {
        this.filepath=filepath;
    }
    private class IOInfo{
        private int uid;
        File f; 
        public IOInfo(String name,int a1,int a2)
        {
            
            if(name.substring(0, 3).matches("ms0"))//found on fileio demo
            {
              int findslash = name.indexOf("/");
              String filename = name.substring(findslash+1,name.length());
              f = new File("ms0/" + filename);
            }
            else if(name.substring(0,1).matches("/"))
            {
               /*that absolute way will work either it is from memstrick browser
                * either if it is from openfile menu*/
               int findslash = name.indexOf("/");
                String filename = name.substring(findslash+1,name.length());
                f = new File(filepath +"/"+ filename);        
            }
            else
            {
             System.out.println("SceIoFilemgr - Unsupported device for write");
             return;
            }
            if((a1 & PSP_O_CREAT) ==0x0200 )
            {
               if(debug) System.out.println("sceIoOpen - create new file");
               try{
                 if(!f.createNewFile())//if already exists
                 {
                   f.delete();//delete it
                   f.createNewFile(); //and recreate it 
                 }
               }
               catch(Exception e)
               {
                  e.printStackTrace();   
               }
             }
             if(((a1 & PSP_O_RDONLY) == 0x0001))
             {
                if(debug)System.out.println("sceIoOpen - readonly");
                f.setReadable(true);
             }
             if(((a1 & PSP_O_WRONLY)  == 0x0002)){
                if(debug) System.out.println("sceIoOpen - writeonly");
                f.setWritable(true);
             }
             if(((a1 & PSP_O_TRUNC)  == 0x0400))
             {
                if(debug) System.out.println("sceIoOpen - Truncates");
               /*TODO*/
             }
             if(((a1 & PSP_O_RDWR)  == 0x0003))
             {
                if(debug) System.out.println("sceIoOpen - Read/Write"); 
                f.setReadable(true);
                f.setWritable(true);
             }
             if(((a1 & PSP_O_APPEND)  == 0x0100))
             {
                  if(debug) System.out.println("sceIoOpen - Append file");
                  /*TODO */
             }
            uid = SceUIDMan.get_instance().getNewUid("IOFileManager-File");
            filelist.put(uid, this);
        }
        
    }
    
}
