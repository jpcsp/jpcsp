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
import jpcsp.filesystems.*;
import jpcsp.filesystems.umdiso.*;
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
    private final boolean debug = true; //enable/disable debug

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

    private HashMap<Integer, IoInfo> filelist;
    private HashMap<Integer, IoDirInfo> dirlist;

    private String filepath; // current working directory on PC
    private UmdIsoReader iso;

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

        // on PSP: device:/path
        // on PC: device/path
        int findcolon = pspfilename.indexOf(":");
        if (findcolon != -1) {
            // Device absolute
            filename = pspfilename.substring(0, findcolon) + pspfilename.substring(findcolon + 1);
            //if (debug) System.out.println("'" + pspfilename.substring(0, findcolon) + "' '" + pspfilename.substring(findcolon + 1) + "'");
        } else if (pspfilename.startsWith("/")) {
            // Relative
            filename = filepath + pspfilename;
        } else {
            // Relative
            filename = filepath + "/" + pspfilename;
        }

        //if (debug) System.out.println("getDeviceFilePath filename = " + filename);

        return filename;
    }

    private boolean isUmdPath(String deviceFilePath) {
        return deviceFilePath.startsWith("disc0/");
    }

    public void sceIoOpen(int filename_addr, int flags, int permissions) {
        String filename = readStringZ(Memory.getInstance().mainmemory, (filename_addr & 0x3fffffff) - MemoryMap.START_RAM);
        if (debug) System.out.println("sceIoOpen filename = " + filename + " flags = " + Integer.toHexString(flags) + " permissions = " + Integer.toOctalString(permissions));

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
            // SeekableRandomFile doesn't support write only
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
                if (debug) System.out.println("sceIoOpen - opening file " + pcfilename);
                //if (debug) System.out.println("sceIoOpen - isUmdPath " + isUmdPath(pcfilename));

                if (isUmdPath(pcfilename)) {
                    // check umd is mounted
                    if (iso == null) {
                        System.out.println("sceIoOpen - no umd mounted");
                        Emulator.getProcessor().gpr[2] = -1;

                    // check flags are valid
                    } else if ((flags & PSP_O_WRONLY) == PSP_O_WRONLY ||
                        (flags & PSP_O_CREAT) == PSP_O_CREAT ||
                        (flags & PSP_O_TRUNC) == PSP_O_TRUNC) {
                        // should we refuse (return -1) or just ignore?
                        System.out.println("sceIoOpen - refusing to open umd media for write");
                        Emulator.getProcessor().gpr[2] = -1;
                    } else {
                        // open file
                        try {
                            UmdIsoFile file = iso.getFile(pcfilename.substring(6));
                            IoInfo info = new IoInfo(file, mode, flags, permissions);
                            Emulator.getProcessor().gpr[2] = info.uid;
                        } catch(IOException e) {
                            System.out.println("sceIoOpen - error opening umd media: " + e.getMessage());
                            Emulator.getProcessor().gpr[2] = -1;
                        }
                    }
                } else {
                    // First check if the file already exists
                    File file = new File(pcfilename);
                    if (file.exists() &&
                        (flags & PSP_O_CREAT) == PSP_O_CREAT &&
                        (flags & PSP_O_EXCL) == PSP_O_EXCL) {
                        // PSP_O_CREAT + PSP_O_EXCL + file already exists = error
                        if (debug) System.out.println("sceIoOpen - file already exists (PSP_O_CREAT + PSP_O_EXCL)");
                        Emulator.getProcessor().gpr[2] = -1;
                    } else {
                        if (file.exists() &&
                            (flags & PSP_O_TRUNC) == PSP_O_TRUNC) {
                            if (debug) System.out.println("sceIoOpen - file already exists, deleting UNIMPLEMENT (PSP_O_TRUNC)");
                            //file.delete();
                        }

                        SeekableRandomFile raf = new SeekableRandomFile(pcfilename, mode);
                        IoInfo info = new IoInfo(raf, mode, flags, permissions);
                        Emulator.getProcessor().gpr[2] = info.uid;
                    }
                }
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
                info.readOnlyFile.close();
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
            String stdout = readStringNZ(Memory.getInstance().mainmemory, data_addr - MemoryMap.START_RAM, size);
            System.out.print(stdout);
            Emulator.getProcessor().gpr[2] = size;
        } else if (uid == 2) { // stderr
            String stderr = readStringNZ(Memory.getInstance().mainmemory, data_addr - MemoryMap.START_RAM, size);
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
                        info.msFile.seek(info.msFile.length());
                    }

                    info.msFile.write(
                        Memory.getInstance().mainmemory.array(),
                        Memory.getInstance().mainmemory.arrayOffset() + data_addr - MemoryMap.START_RAM,
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
                    // Using readFully for ms/umd compatibility, but now we must
                    // manually make sure it doesn't read off the end of the file.
                    if (info.readOnlyFile.getFilePointer() + size > info.readOnlyFile.length())
                        size = (int)(info.readOnlyFile.length() - info.readOnlyFile.getFilePointer());

                    info.readOnlyFile.readFully(
                        Memory.getInstance().mainmemory.array(),
                        Memory.getInstance().mainmemory.arrayOffset() + data_addr - MemoryMap.START_RAM,
                        size);
                    Emulator.getProcessor().gpr[2] = size;
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

    // TODO sceIoLseek with 64-bit return value
    public void sceIoLseek(int uid, long offset, int whence) {
        if (debug) System.out.println("sceIoLseek - uid " + Integer.toHexString(uid) + " offset " + offset + " (hex=0x" + Long.toHexString(offset) + ") whence " + getWhenceName(whence));
        seek(uid, offset, whence);
    }

    public void sceIoLseek32(int uid, int offset, int whence) {
        if (debug) System.out.println("sceIoLseek32 - uid " + Integer.toHexString(uid) + " offset " + offset + " (hex=0x" + Integer.toHexString(offset) + ") whence " + getWhenceName(whence));
        seek(uid, offset, whence);
    }

    private String getWhenceName(int whence) {
        switch(whence) {
            case PSP_SEEK_SET: return "PSP_SEEK_SET";
            case PSP_SEEK_CUR: return "PSP_SEEK_CUR";
            case PSP_SEEK_END: return "PSP_SEEK_END";
            default: return "UNHANDLED " + whence;
        }
    }

    private void seek(int uid, long offset, int whence) {
        //if (debug) System.out.println("seek - uid " + Integer.toHexString(uid) + " offset " + offset + " whence " + whence);

        if (uid == 1 || uid == 2 || uid == 3) { // stdio
            System.out.println("seek - can't seek on stdio uid " + Integer.toHexString(uid));
            Emulator.getProcessor().gpr[2] = -1;
        } else {
            try {
                SceUIDMan.get_instance().checkUidPurpose(uid, "IOFileManager-File", true);
                IoInfo info = filelist.get(uid);
                if (info == null) {
                    System.out.println("seek - unknown uid " + Integer.toHexString(uid));
                    Emulator.getProcessor().gpr[2] = -1;
                } else {
                    switch(whence) {
                        case PSP_SEEK_SET:
                            info.readOnlyFile.seek(offset);
                            break;
                        case PSP_SEEK_CUR:
                            info.readOnlyFile.seek(info.readOnlyFile.getFilePointer() + offset);
                            break;
                        case PSP_SEEK_END:
                            info.readOnlyFile.seek(info.readOnlyFile.length() - offset);
                            break;
                        default:
                            System.out.println("seek - unhandled whence " + whence);
                            break;
                    }
                    Emulator.getProcessor().gpr[2] = (int)info.readOnlyFile.getFilePointer();
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
        String dir = readStringZ(Memory.getInstance().mainmemory, (dir_addr & 0x3fffffff) - MemoryMap.START_RAM);
        if (debug) System.out.println("sceIoMkdir dir = " + dir);
        //should work okay..
        String pcfilename = getDeviceFilePath(dir);
        if (pcfilename != null) {
            File f = new File(pcfilename);
            f.mkdir();
            Emulator.getProcessor().gpr[2] = 0;
        } else {
            Emulator.getProcessor().gpr[2] = -1;
        }
    }

    public void sceIoChdir(int path_addr) {
        String path = readStringZ(Memory.getInstance().mainmemory, (path_addr & 0x3fffffff) - MemoryMap.START_RAM);
        if (debug) System.out.println("(Unverified):sceIoChdir path = " + path);

        // TODO/check correctness
        String pcfilename = getDeviceFilePath(path);
        if (pcfilename != null) {
            filepath = pcfilename;
            Emulator.getProcessor().gpr[2] = 0;
        } else {
            Emulator.getProcessor().gpr[2] = -1;
        }
    }

    public void sceIoDopen(int dirname_addr) {
        String dirname = readStringZ(Memory.getInstance().mainmemory, (dirname_addr & 0x3fffffff) - MemoryMap.START_RAM);
        if (debug) System.out.println("sceIoDopen dirname = " + dirname);

        String pcfilename = getDeviceFilePath(dirname);
        if (pcfilename != null) {
            File f = new File(pcfilename);
            if (f.isDirectory()) {
                IoDirInfo info = new IoDirInfo(pcfilename, f);
                Emulator.getProcessor().gpr[2] = info.uid;
            } else {
                if (debug) System.out.println("sceIoDopen not a directory!");
                Emulator.getProcessor().gpr[2] = -1;
            }
        } else {
            Emulator.getProcessor().gpr[2] = -1;
        }
    }

    public void sceIoDread(int uid, int dirent_addr) {
        if (debug) System.out.println("sceIoDread - uid = " + Integer.toHexString(uid) + " dirent = " + Integer.toHexString(dirent_addr));

        try {
            SceUIDMan.get_instance().checkUidPurpose(uid, "IOFileManager-Directory", true);
            IoDirInfo info = dirlist.get(uid);
            if (info == null) {
                System.out.println("sceIoDread - unknown uid " + Integer.toHexString(uid));
                Emulator.getProcessor().gpr[2] = -1;
            } else if (info.hasNext()) {
                //String filename = info.path + "/" + info.next(); // TODO is the separator needed?
                String filename = info.next(); // TODO is the separator needed?
                System.out.println("sceIoDread - filename = " + filename);

                //String pcfilename = getDeviceFilePath(filename);
                //String pcfilename = getDeviceFilePath(info.path + "/" + filename);
                //SceIoStat stat = stat(pcfilename);
                SceIoStat stat = stat(info.path + filename);
                if (stat != null) {
                    SceIoDirent dirent = new SceIoDirent(stat, filename);
                    dirent.write(Memory.getInstance(), dirent_addr);
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
        if (debug) System.out.println("sceIoDclose - uid = " + Integer.toHexString(uid));

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
            if (debug) System.out.println("stat - pcfilename = " + pcfilename);
            File file = new File(pcfilename);
            if (file.exists()) {
                int mode = (file.canRead() ? 4 : 0) + (file.canWrite() ? 2 : 0) + (file.canExecute() ? 1 : 0);
                int attr = 0;
                long size = file.length();
                long mtime = file.lastModified();

                // Octal extend into user and group
                mode = mode + mode * 8 + mode * 64;
                //if (debug) System.out.println("stat - permissions = " + Integer.toOctalString(mode));

                // Set attr (dir/file) and copy into mode
                if (file.isDirectory())
                    attr |= 0x10;
                if (file.isFile())
                    attr |= 0x20;
                mode |= attr << 8;

                // Java can't see file create/access time
                stat = new SceIoStat(mode, attr, size,
                    new ScePspDateTime(0), new ScePspDateTime(0),
                    new ScePspDateTime(mtime));
            }
        }
        return stat;
    }

    public void sceIoGetstat(int file_addr, int stat_addr) {
        String filename = readStringZ(Memory.getInstance().mainmemory, (file_addr & 0x3fffffff) - MemoryMap.START_RAM);
        if (debug) System.out.println("sceIoGetstat - file = " + filename + " stat = " + Integer.toHexString(stat_addr));

        String pcfilename = getDeviceFilePath(filename);
        SceIoStat stat = stat(pcfilename);
        if (stat != null) {
            stat.write(Memory.getInstance(), stat_addr);
            Emulator.getProcessor().gpr[2] = 0;
        } else {
            Emulator.getProcessor().gpr[2] = -1;
        }
    }

    //the following sets the filepath from memstick manager.
    public void setfilepath(String filepath)
    {
        System.out.println("pspiofilemgr - filepath " + filepath);
        this.filepath = filepath;
    }

    public void setIsoReader(UmdIsoReader iso)
    {
        if (iso != null)
        {
            System.out.println("pspiofilemgr - umd mounted " + iso.getFilename());
        }
        else
        {
            System.out.println("pspiofilemgr - umd unmounted");
        }

        this.iso = iso;
    }

    class IoInfo {
        // Internal settings
        public final SeekableRandomFile msFile; // on memory stick
        public final SeekableDataInput readOnlyFile; // on memory stick or umd
        public final String mode;

        // PSP settings
        public final int flags;
        public final int permissions;

        public final int uid;

        public IoInfo(SeekableRandomFile f, String mode, int flags, int permissions) {
            this.msFile = f;
            this.readOnlyFile = f;
            this.mode = mode;
            this.flags = flags;
            this.permissions = permissions;
            uid = SceUIDMan.get_instance().getNewUid("IOFileManager-File");
            filelist.put(uid, this);
        }

        public IoInfo(SeekableDataInput f, String mode, int flags, int permissions) {
            this.msFile = null;
            this.readOnlyFile = f;
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

}
