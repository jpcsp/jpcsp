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

//import java.io.BufferedOutputStream;
import java.io.RandomAccessFile;
import java.io.FileNotFoundException;
import java.io.IOException;
//import java.io.PrintStream;
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
    private static HashMap<Integer, IoInfo> filelist;
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

    private final boolean debug = true; //enable/disable debug

    public static pspiofilemgr get_instance() {
        if (instance == null) {
            instance = new pspiofilemgr();
        }
        return instance;
    }

    public pspiofilemgr()
    {
      filelist = new HashMap<Integer, IoInfo>();
    }

    private String getDeviceFilePath(String pspfilename) {
        String filename = null;
        if (pspfilename.substring(0, 3).matches("ms0")) { //found on fileio demo
            int findslash = pspfilename.indexOf("/");
            filename = "ms0/" + pspfilename.substring(findslash+1,pspfilename.length());
        } else if(pspfilename.substring(0,1).matches("/")) {
            /*that absolute way will work either it is from memstrick browser
             * either if it is from openfile menu*/
            int findslash = pspfilename.indexOf("/");
            filename = filepath +"/"+ pspfilename.substring(findslash+1,pspfilename.length());
        } else {
            System.out.println("pspiofilemgr - Unsupported device '" + pspfilename + "'");
        }
        return filename;
    }

    public void sceIoOpen(int filename_addr, int flags, int permissions) {
        String filename = readStringZ(Memory.get_instance().mainmemory, (filename_addr & 0x3fffffff) - MemoryMap.START_RAM);
        if (debug) System.out.println("sceIoOpen name = " + filename + " flags = " + Integer.toHexString(flags) + " permissions = " + Integer.toOctalString(permissions));

        if (debug) {
            if ((flags & PSP_O_RDONLY) == PSP_O_RDONLY) System.out.println("PSP_O_RDONLY");
            if ((flags & PSP_O_WRONLY) == PSP_O_WRONLY) System.out.println("PSP_O_WRONLY");
            if ((flags & PSP_O_NBLOCK) == PSP_O_NBLOCK) System.out.println("PSP_O_NBLOCK");
            if ((flags & PSP_O_DIROPEN) == PSP_O_DIROPEN) System.out.println("PSP_O_DIROPEN");
            if ((flags & PSP_O_APPEND) == PSP_O_APPEND) System.out.println("PSP_O_APPEND");
            if ((flags & PSP_O_CREAT) == PSP_O_CREAT) System.out.println("PSP_O_CREAT");
            if ((flags & PSP_O_TRUNC) == PSP_O_TRUNC) System.out.println("PSP_O_TRUNC");
            if ((flags & PSP_O_EXCL) == PSP_O_EXCL) System.out.println("PSP_O_EXCL");
            if ((flags & PSP_O_NOWAIT) == PSP_O_NOWAIT) System.out.println("PSP_O_NOWAIT");
        }

        String mode;

        // PSP_O_RDWR check must come before the individual PSP_O_RDONLY and PSP_O_WRONLY checks
        if ((flags & PSP_O_RDWR) == PSP_O_RDWR) {
            mode = "rw";
        } else if ((flags & PSP_O_RDONLY) == PSP_O_RDONLY) {
            mode = "r";
        } else if ((flags & PSP_O_WRONLY) == PSP_O_WRONLY) {
            // RandomAccessFile doesn't support write only
            mode = "rw";
        } else {
            System.out.println("sceIoOpen - unhandled flags " + Integer.toHexString(flags));
            Emulator.getProcessor().gpr[2] = -1;
            return;
        }

        // TODO we may want to do something with PSP_O_CREAT and permissions
        // using java File and its setReadable/Writable/Executable.
        // Does PSP filesystem even support permissions?

        // TODO PSP_O_TRUNC flag. delete the file and recreate it?

        // This could get messy, is it even allowed?
        if ((flags & PSP_O_RDONLY) == PSP_O_RDONLY &&
            (flags & PSP_O_APPEND) == PSP_O_APPEND) {
            System.out.println("sceIoOpen - read and append flags both set!");
        }

        try {
            String pcfilename = getDeviceFilePath(filename);
            if (pcfilename != null) {
                if (debug) System.out.println("pspiofilemgr - opening file '" + pcfilename + "'");
                RandomAccessFile f = new RandomAccessFile(pcfilename, mode);
                IoInfo info = new IoInfo(f, mode, flags, permissions);
                Emulator.getProcessor().gpr[2] = info.uid;
            } else {
                Emulator.getProcessor().gpr[2] = -1;
            }
        } catch(FileNotFoundException e) {
            // To be expected under mode="r" and file doesn't exist
            if (debug) System.out.println("pspiofilemgr - file not found (ok to ignore this message, debug purpose only)");
            Emulator.getProcessor().gpr[2] = -1;
        }
    }

    public void sceIoClose(int uid) {
        if (debug) System.out.println("sceIoClose - uid " + Integer.toHexString(uid));

        try {
            SceUIDMan.get_instance().checkUidPurpose(uid, "IOFileManager-File", true);

            IoInfo info = filelist.remove(uid);
            if (info == null) {
                if (uid != 1 && uid != 2) // stdin and stderr
                    System.out.println("sceIoClose - unknown uid " + Integer.toHexString(uid));
                Emulator.getProcessor().gpr[2] = -1;
            } else {
                info.f.close();
                SceUIDMan.get_instance().releaseUid(info.uid, "IOFileManager-File");
                Emulator.getProcessor().gpr[2] = 0;
            }
        } catch(GeneralJpcspException e) {
            e.printStackTrace();
            Emulator.getProcessor().gpr[2] = -1;
        } catch(IOException e) {
            e.printStackTrace();
            Emulator.getProcessor().gpr[2] = -1;
        }
    }

    public void sceIoWrite(int uid, int data_addr, int size) {
        if (debug) System.out.println("sceIoWrite - uid " + Integer.toHexString(uid) + " data " + Integer.toHexString(data_addr) + " size " + size);
        data_addr &= 0x3fffffff; // remove kernel/cache bits

        if (uid == 1) { // stdout
            String stdout = readStringZ(Memory.get_instance().mainmemory, data_addr - MemoryMap.START_RAM);
            System.out.print(stdout);
            Emulator.getProcessor().gpr[2] = size;
        } else if (uid == 2) { // stderr
            String stderr = readStringZ(Memory.get_instance().mainmemory, data_addr - MemoryMap.START_RAM);
            System.out.print(stderr);
            Emulator.getProcessor().gpr[2] = size;
        } else {
            try {
                SceUIDMan.get_instance().checkUidPurpose(uid, "IOFileManager-File", true);
                IoInfo info = filelist.get(uid);
                if (info == null) {
                    System.out.println("sceIoWrite - unknown uid " + Integer.toHexString(uid));
                    Emulator.getProcessor().gpr[2] = -1;
                } else if ((data_addr >= MemoryMap.START_RAM ) && (data_addr <= MemoryMap.END_RAM)) {
                    if ((info.flags & PSP_O_APPEND) == PSP_O_APPEND) {
                        System.out.println("sceIoWrite - untested append operation");
                        info.f.seek(info.f.length());
                    }

                    info.f.write(
                        Memory.get_instance().mainmemory.array(),
                        Memory.get_instance().mainmemory.arrayOffset() + data_addr - MemoryMap.START_RAM,
                        size);

                    Emulator.getProcessor().gpr[2] = size;
                } else {
                    System.out.println("sceIoWrite - data is outside of ram " + Integer.toHexString(data_addr));
                    Emulator.getProcessor().gpr[2] = -1;
                }
            } catch(GeneralJpcspException e) {
                e.printStackTrace();
                Emulator.getProcessor().gpr[2] = -1;
            } catch(IOException e) {
                e.printStackTrace();
                Emulator.getProcessor().gpr[2] = -1;
            }
        }
    }

    public void sceIoRead(int uid, int data_addr, int size) {
        if (debug) System.out.println("sceIoRead - uid " + Integer.toHexString(uid) + " data " + Integer.toHexString(data_addr) + " size " + size);
        data_addr &= 0x3fffffff; // remove kernel/cache bits

        if (uid == 3) { // stdin
            // TODO
            Emulator.getProcessor().gpr[2] = 0;
        } else {
            try {
                SceUIDMan.get_instance().checkUidPurpose(uid, "IOFileManager-File", true);
                IoInfo info = filelist.get(uid);
                if (info == null) {
                    System.out.println("sceIoRead - unknown uid " + Integer.toHexString(uid));
                    Emulator.getProcessor().gpr[2] = -1;
                } else if ((data_addr >= MemoryMap.START_RAM ) && (data_addr <= MemoryMap.END_RAM)) {
                    Emulator.getProcessor().gpr[2] = info.f.read(
                        Memory.get_instance().mainmemory.array(),
                        Memory.get_instance().mainmemory.arrayOffset() + data_addr - MemoryMap.START_RAM,
                        size);
                } else {
                    System.out.println("sceIoRead - data is outside of ram " + Integer.toHexString(data_addr));
                    Emulator.getProcessor().gpr[2] = -1;
                }
            } catch(GeneralJpcspException e) {
                e.printStackTrace();
                Emulator.getProcessor().gpr[2] = -1;
            } catch(IOException e) {
                e.printStackTrace();
                Emulator.getProcessor().gpr[2] = -1;
            }
        }
    }

    public void sceIoDopen(int a0)
    {
        String name = readStringZ(Memory.get_instance().mainmemory, (a0 & 0x3fffffff) - MemoryMap.START_RAM);
        System.out.println("sceIoDopen dir = " + name);
        Emulator.getProcessor().gpr[2] = -1;

    }

    public void sceIoChdir(int a0)
    {
        String name = readStringZ(Memory.get_instance().mainmemory, (a0 & 0x3fffffff) - MemoryMap.START_RAM);
        System.out.println("sceIoChdir = " + name);
        Emulator.getProcessor().gpr[2] = -1;
    }

    //the following sets the filepath from memstick manager.
    private String filepath;
    public void setfilepath(String filepath)
    {
        this.filepath = filepath;
    }

    class IoInfo {
        // Internal settings
        final RandomAccessFile f;
        final String mode;

        // PSP settings
        final int flags;
        final int permissions;

        final int uid;

        public IoInfo(RandomAccessFile f, String mode, int flags, int permissions) {
            this.f = f;
            this.mode = mode;
            this.flags = flags;
            this.permissions = permissions;
            uid = SceUIDMan.get_instance().getNewUid("IOFileManager-File");
            filelist.put(uid, this);
        }
    }

/*
    private class IOInfo{
        private int uid;
        File f;
        public boolean fileappend=false;
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
               //that absolute way will work either it is from memstrick browser
               // either if it is from openfile menu
               int findslash = name.indexOf("/");
                String filename = name.substring(findslash+1,name.length());
                f = new File(filepath +"/"+ filename);
            }
            else
            {
             System.out.println("SceIoFilemgr - Unsupported device for open");
             uid=-1;
             return;
            }
            if((a1 & PSP_O_CREAT) ==0x0200 )
            {
               if(debug) System.out.println("sceIoOpen - create new file");
               try{
                 if(!f.createNewFile())//if already exists maybe that isn't important but do it anyway
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
                //Okay the is probably a very bad way to do this.. but works.. anyone with better idea?
                if(debug) System.out.println("sceIoOpen - Truncates");
                try
                {
                  FileOutputStream fop = new FileOutputStream(f);
                  BufferedOutputStream bos = new BufferedOutputStream(fop);
                  PrintStream b = new PrintStream(bos,true);
                  b.close();
                  bos.close();
                  fop.close();
                }
                catch(IOException e)
                {
                  e.printStackTrace();
                }

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
                  fileappend=true;
             }
             if(((a1 & PSP_O_NBLOCK)  == 0x0004))
             {
                  if(debug) System.out.println("sceIoOpen - nblock unsupported!!");
             }
             if(((a1 & PSP_O_DIROPEN)  == 0x0008))
             {
                  if(debug) System.out.println("sceIoOpen - diropen unsupported!!");
             }
             if(((a1 & PSP_O_EXCL)  == 0x0800))
             {
               if(debug) System.out.println("sceIoOpen - excl unsupported!!");
             }
             if(((a1 & PSP_O_NOWAIT)  == 0x8000))
             {
                  if(debug) System.out.println("sceIoOpen - nowait unsupported!!");
             }
            uid = SceUIDMan.get_instance().getNewUid("IOFileManager-File");
            filelist.put(uid, this);
        }

    }
    */

}
