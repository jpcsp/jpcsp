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
import java.io.File;
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
    private static HashMap<Integer, IoDirInfo> dirlist;

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

    public final static int PSP_SEEK_SET  = 0;
    public final static int PSP_SEEK_CUR  = 1;
    public final static int PSP_SEEK_END  = 2;

    private final boolean debug = true; //enable/disable debug

    public static pspiofilemgr get_instance() {
        if (instance == null) {
            instance = new pspiofilemgr();
        }
        return instance;
    }

    public pspiofilemgr() {
        filelist = new HashMap<Integer, IoInfo>();
        dirlist = new HashMap<Integer, IoDirInfo>();
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
        //if (debug) System.out.println("sceIoWrite - uid " + Integer.toHexString(uid) + " data " + Integer.toHexString(data_addr) + " size " + size);
        data_addr &= 0x3fffffff; // remove kernel/cache bits

        if (uid == 1) { // stdout
            String stdout = readStringNZ(Memory.get_instance().mainmemory, data_addr - MemoryMap.START_RAM, size);
            System.out.print(stdout);
            Emulator.getProcessor().gpr[2] = size;
        } else if (uid == 2) { // stderr
            String stderr = readStringNZ(Memory.get_instance().mainmemory, data_addr - MemoryMap.START_RAM, size);
            System.out.print(stderr);
            Emulator.getProcessor().gpr[2] = size;
        } else {
            if (debug) System.out.println("sceIoWrite - uid " + Integer.toHexString(uid) + " data " + Integer.toHexString(data_addr) + " size " + size);

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

    // TODO sceIoLseek with 64-bit offset parameter and return value
    public void sceIoLseek32(int uid, int offset, int whence) {
        if (debug) System.out.println("sceIoLseek32 - uid " + Integer.toHexString(uid) + " offset " + offset + " whence " + whence);

        if (uid == 1 || uid == 2 || uid == 3) { // stdio
            System.out.println("sceIoLseek32 - can't seek on stdio uid " + Integer.toHexString(uid));
            Emulator.getProcessor().gpr[2] = -1;
        } else {
            try {
                SceUIDMan.get_instance().checkUidPurpose(uid, "IOFileManager-File", true);
                IoInfo info = filelist.get(uid);
                if (info == null) {
                    System.out.println("sceIoLseek32 - unknown uid " + Integer.toHexString(uid));
                    Emulator.getProcessor().gpr[2] = -1;
                } else {
                    switch(whence) {
                        case PSP_SEEK_SET:
                            info.f.seek(offset);
                            break;
                        case PSP_SEEK_CUR:
                            info.f.seek(info.f.getFilePointer() + offset);
                            break;
                        case PSP_SEEK_END:
                            info.f.seek(info.f.length() - offset);
                            break;
                        default:
                            System.out.println("sceIoLseek32 - unhandled whence " + whence);
                            break;
                    }
                    Emulator.getProcessor().gpr[2] = (int)info.f.getFilePointer();
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

    public void sceIoMkdir(int dir_addr, int permissions) {
        String dir = readStringZ(Memory.get_instance().mainmemory, (dir_addr & 0x3fffffff) - MemoryMap.START_RAM);
        if (debug) System.out.println("sceIoMkdir dir = " + dir);
        // TODO
        Emulator.getProcessor().gpr[2] = -1;
    }

    public void sceIoChdir(int path_addr) {
        String path = readStringZ(Memory.get_instance().mainmemory, (path_addr & 0x3fffffff) - MemoryMap.START_RAM);
        if (debug) System.out.println("sceIoChdir path = " + path);
        // TODO
        Emulator.getProcessor().gpr[2] = -1;
    }

    public void sceIoDopen(int dirname_addr) {
        String dirname = readStringZ(Memory.get_instance().mainmemory, (dirname_addr & 0x3fffffff) - MemoryMap.START_RAM);
        if (debug) System.out.println("sceIoDopen dirname = " + dirname);

        String pcfilename = getDeviceFilePath(dirname);
        if (pcfilename != null) {
            File f = new File(pcfilename);
            if (f.isDirectory()) {
                IoDirInfo info = new IoDirInfo(pcfilename, f);
                Emulator.getProcessor().gpr[2] = info.uid;
            } else {
                Emulator.getProcessor().gpr[2] = -1;
            }
        } else {
            Emulator.getProcessor().gpr[2] = -1;
        }
    }

    /** @param dir_addr address of a dirent struct */
    public void sceIoDread(int uid, int dir_addr) {
        if (debug) System.out.println("sceIoDread - uid " + Integer.toHexString(uid) + " dir " + Integer.toHexString(dir_addr));

        try {
            SceUIDMan.get_instance().checkUidPurpose(uid, "IOFileManager-Directory", true);
            IoDirInfo info = dirlist.get(uid);
            if (info == null) {
                System.out.println("sceIoDread - unknown uid " + Integer.toHexString(uid));
                Emulator.getProcessor().gpr[2] = -1;
            } else if (info.hasNext()) {
                //String filename = info.path + "/" + info.next(); // TODO is the separator needed?
                String filename = info.next(); // TODO is the separator needed?
                System.out.println("sceIoDread - filename '" + filename + "'");

                //String pcfilename = getDeviceFilePath(filename);
                //String pcfilename = getDeviceFilePath(info.path + "/" + filename);
                //SceIoStat stat = stat(pcfilename);
                SceIoStat stat = stat(info.path + "/" + filename);
                if (stat != null) {
                    SceIoDirent dirent = new SceIoDirent(stat, filename);
                    dirent.write(Memory.get_instance(), dir_addr);
                    Emulator.getProcessor().gpr[2] = 1; // TODO "> 0", so number of files remaining?
                } else {
                    System.out.println("sceIoDread - stat failed");
                    Emulator.getProcessor().gpr[2] = -1;
                }
            } else {
                System.out.println("sceIoDread - no more files");
                Emulator.getProcessor().gpr[2] = 0;
            }
        } catch(GeneralJpcspException e) {
            e.printStackTrace();
            Emulator.getProcessor().gpr[2] = -1;
        }
    }

    public void sceIoDclose(int uid) {
        if (debug) System.out.println("sceIoDclose - uid " + Integer.toHexString(uid));

        try {
            SceUIDMan.get_instance().checkUidPurpose(uid, "IOFileManager-Directory", true);

            IoDirInfo info = dirlist.remove(uid);
            if (info == null) {
                System.out.println("sceIoDclose - unknown uid " + Integer.toHexString(uid));
                Emulator.getProcessor().gpr[2] = -1;
            } else {
                SceUIDMan.get_instance().releaseUid(info.uid, "IOFileManager-Directory");
                Emulator.getProcessor().gpr[2] = 0;
            }
        } catch(GeneralJpcspException e) {
            e.printStackTrace();
            Emulator.getProcessor().gpr[2] = -1;
        }
    }

    /** @param pcfilename can be null for convenience
     * @returns null on error */
    private SceIoStat stat(String pcfilename) {
        SceIoStat stat = null;
        if (pcfilename != null) {
            File file = new File(pcfilename);
            if (file.exists()) {
                int mode = (file.canRead() ? 4 : 0) + (file.canWrite() ? 2 : 0) + (file.canExecute() ? 1 : 0);
                int attr = 0; // file/dir? posix puts that in mode
                long size = file.length();
                long mtime = file.lastModified();

                // octal extend into user and group
                mode = mode + mode * 8 + mode * 16;

                // TODO convert mtime from seconds to ScePspDateTime (see sceRtc)

                stat = new SceIoStat(mode, attr, size,
                    new ScePspDateTime(), new ScePspDateTime(),
                    new ScePspDateTime());
            }
        }
        return stat;
    }

    public void sceIoGetstat(int file_addr, int stat_addr) {
        String filename = readStringZ(Memory.get_instance().mainmemory, (file_addr & 0x3fffffff) - MemoryMap.START_RAM);
        if (debug) System.out.println("sceIoGetstat - file " + Integer.toHexString(file_addr) + " stat " + Integer.toHexString(stat_addr));

        String pcfilename = getDeviceFilePath(filename);
        SceIoStat stat = stat(pcfilename);
        if (stat != null) {
            stat.write(Memory.get_instance(), stat_addr);
            Emulator.getProcessor().gpr[2] = 0;
        } else {
            Emulator.getProcessor().gpr[2] = -1;
        }
    }

    //the following sets the filepath from memstick manager.
    private String filepath;
    public void setfilepath(String filepath)
    {
        System.out.println("pspiofilemgr - filepath " + filepath);
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

    class IoDirInfo {
        final String path;
        final String[] filenames;
        int position;
        final int uid;

        public IoDirInfo(String path, File f) {
            this.path = path;

            filenames = f.list();
            position = 0;

            uid = SceUIDMan.get_instance().getNewUid("IOFileManager-Directory");
            dirlist.put(uid, this);
        }

        public boolean hasNext() {
            return (position < filenames.length);
        }

        public String next() {
            String filename = null;
            if (position < filenames.length) {
                filename = filenames[position];
                position++;
            }
            return filename;
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
