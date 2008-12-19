/*
Function:
- HLE everything in http://psp.jim.sh/pspsdk-doc/pspiofilemgr_8h.html
Notes:
- Redirecting the xxxAsync calls to xxx and using yieldCB

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

import jpcsp.filesystems.*;
import jpcsp.filesystems.umdiso.*;
import jpcsp.util.Utilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import static jpcsp.util.Utilities.*;

import jpcsp.HLE.kernel.types.*;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.*;
import jpcsp.HLE.kernel.managers.*;
import jpcsp.State;

// TODO use file id's 3 to 31 inclusive
// TODO get std out/err/in from stdio module, it can be any number not just 1/2/3 BUT some old homebrew may expect it to be 1/2/3 (such as some versions of psplinkusb)
public class pspiofilemgr {
    private static pspiofilemgr  instance;
    private final boolean debug = true; //enable/disable debug
    //private final boolean debug = false; //enable/disable debug
    private static Logger stdout = Logger.getLogger("stdout");
    private static Logger stderr = Logger.getLogger("stderr");

    public final static int PSP_O_RDONLY   = 0x0001;
    public final static int PSP_O_WRONLY   = 0x0002;
    public final static int PSP_O_RDWR     = (PSP_O_RDONLY | PSP_O_WRONLY);
    public final static int PSP_O_NBLOCK   = 0x0004;
    public final static int PSP_O_DIROPEN  = 0x0008; // Internal use for dopen
    public final static int PSP_O_APPEND   = 0x0100;
    public final static int PSP_O_CREAT    = 0x0200;
    public final static int PSP_O_TRUNC    = 0x0400;
    public final static int PSP_O_EXCL     = 0x0800;
    public final static int PSP_O_UNKNOWN1 = 0x4000; // something async?
    public final static int PSP_O_NOWAIT   = 0x8000;
    public final static int PSP_O_UNKNOWN2 = 0xf0000; // seen on Wipeout Pure

    public final static int PSP_SEEK_SET  = 0;
    public final static int PSP_SEEK_CUR  = 1;
    public final static int PSP_SEEK_END  = 2;


    public final static int PSP_ERROR_ASYNC_FILE_OPEN_ERROR = 0x80010002; // actual name unknown
    //public final static int PSP_ERROR_FILE_OPEN_ERROR     = 0x80010003; // actual name unknown, no such device? bad format path name?

    public final static int PSP_ERROR_FILE_READ_ERROR     = 0x80020130;
    public final static int PSP_ERROR_TOO_MANY_OPEN_FILES = 0x80020320;
    public final static int PSP_ERROR_BAD_FILE_DESCRIPTOR = 0x80020323;
    public final static int PSP_ERROR_NOCWD               = 0x8002032c; // TODO
    public final static int PSP_ERROR_FILENAME_TOO_LONG   = 0x8002032d;
    public final static int PSP_ERROR_ASYNC_BUSY          = 0x80020329;
    public final static int PSP_ERROR_NO_ASYNC_OP         = 0x8002032a;


    private HashMap<Integer, IoInfo> filelist;
    private HashMap<Integer, IoDirInfo> dirlist;

    private String filepath; // current working directory on PC
    private UmdIsoReader iso;

    private boolean asyncBusy;

    public static pspiofilemgr getInstance() {
        if (instance == null) {
            instance = new pspiofilemgr();
        }
        return instance;
    }

    private pspiofilemgr() {
    }

    public void Initialise() {
        if (filelist != null) {
            // Close open files
            for (Iterator<IoInfo> it = filelist.values().iterator(); it.hasNext();) {
                IoInfo info = it.next();
                try {
                    info.readOnlyFile.close();
                } catch(IOException e) {
                    Modules.log.error("pspiofilemgr - error closing file: " + e.getMessage());
                }
            }
        }

        filelist = new HashMap<Integer, IoInfo>();
        dirlist = new HashMap<Integer, IoDirInfo>();

        asyncBusy = false;
    }

    /** To properly emulate async io we cannot allow async io operations to
     * complete immediately. For example a call to sceIoPollAsync must be
     * preceeded by at least one context switch to return success, otherwise it
     * returns async busy.
     * TODO could make async io succeed based on at least one context switch
     * (as it currently does) PLUS a certain amount of time passed. */
    public void onContextSwitch() {
        IoInfo found = null;

        // This is all based on the assumption only 1 io op can be happening at a time
        for (Iterator<IoInfo> it = filelist.values().iterator(); it.hasNext();) {
            IoInfo info = it.next();
            if (info.asyncPending) {
                found = info;
                break;
            }
        }

        if (found != null) {
            found.asyncPending = false;

            if (found.cbid >= 0) {
                ThreadMan.getInstance().pushIoCallback(found.cbid, found.notifyArg);
            }

            // Find threads waiting on this uid and wake them up
            // TODO If the call was sceIoWaitAsyncCB we might need to make sure
            // the callback is fully processed before waking the thread!
            for (Iterator<SceKernelThreadInfo> it = ThreadMan.getInstance().iterator(); it.hasNext(); ) {
                SceKernelThreadInfo thread = it.next();

                if (thread.wait.waitingOnIo &&
                    thread.wait.Io_id == found.uid) {
                    // Untrack
                    thread.wait.waitingOnIo = false;

                    // Return success
                    thread.cpuContext.gpr[2] = 0;

                    // Wakeup
                    ThreadMan.getInstance().changeThreadState(thread, PSP_THREAD_READY);
                }
            }
        }
    }

    private String getDeviceFilePath(String pspfilename) {
        //Modules.log.debug("getDeviceFilePath filepath='" + filepath + "' pspfilename='" + pspfilename + "'");
        String device = filepath; // must not end with /
        String path = pspfilename;
        String filename = null;

        // on PSP
        // path - relative to cwd
        // /path - relative to cwd
        // dev:path
        // dev:/path

        // on PSP: device:path
        // on PC: device/path
        int findcolon = pspfilename.indexOf(":");
        if (findcolon != -1) {
            // Device absolute
            device = pspfilename.substring(0, findcolon);
            path = pspfilename.substring(findcolon + 1);
            //Modules.log.debug("getDeviceFilePath split device='" + device + "' path='" + path + "'");
        }

        // removing trailing / on paths, but only if the path isn't just "/"
        if (path.endsWith("/") && path.length() != 1) {
            path = path.substring(0, path.length() - 1);
        }

        if (path.startsWith("/")) {
            if (path.length() == 1) {
                filename = device;
            } else {
                filename = device + path;
            }
        } else {
            filename = device + "/" + path;
        }

        if (device.equals("host0")) {
            // If an iso is loaded, remap host0 to disc0
            // If an iso is not loaded, assume running an unpacked iso, remap to file system
            if (iso != null) {
                Modules.log.warn("pspiofilemgr - remapping host0 to disc0");
                filename = filename.replace("host0", "disc0");
            } else {
                Modules.log.warn("pspiofilemgr - remapping host0 to " + filepath);
                filename = filename.replace("host0", filepath);
            }
        }

        //if (filename != null)
        //    Modules.log.debug("getDeviceFilePath filename='" + filename + "'");

        return filename;
    }

    private boolean isUmdPath(String deviceFilePath) {
        //return deviceFilePath.toLowerCase().startsWith("disc0/"); // old
        return deviceFilePath.toLowerCase().startsWith("disc0") ||
            deviceFilePath.toLowerCase().startsWith("umd0");
    }

    // TODO fix this slash thing properly, must be caused by poor handling in some other function
    private String trimUmdPrefix(String pcfilename) {
        if (pcfilename.toLowerCase().startsWith("disc0/"))
            return pcfilename.substring(6);
        if (pcfilename.toLowerCase().startsWith("disc0"))
            return pcfilename.substring(5);
        if (pcfilename.toLowerCase().startsWith("umd0/"))
            return pcfilename.substring(5);
        if (pcfilename.toLowerCase().startsWith("umd0"))
            return pcfilename.substring(4);
        return pcfilename;
    }

    public void sceIoSync(int device_addr, int unknown) {
        String device = readStringZ(device_addr);
        if (debug) Modules.log.debug("IGNORING:sceIoSync(device='" + device + "',unknown=0x" + Integer.toHexString(unknown) + ")");
        State.fileLogger.logIoSync(0, device_addr, device, unknown);
        Emulator.getProcessor().cpu.gpr[2] = 0; // Fake success
        // TODO "block"/yield?
    }

    /** if operation is still in progress return 1 and do not write to res.
     * also calls to read/write/close will return PSP_ERROR_ASYNC_BUSY.
     * if operation is done return 0, write to res and flush out the saved result. */
    public void hleIoGetAsyncStat(int uid, int res_addr, boolean wait, boolean callbacks) {
        if (debug) Modules.log.debug("hleIoGetAsyncStat(uid=" + Integer.toHexString(uid) + ",res=0x" + Integer.toHexString(res_addr) + ") wait=" + wait + " callbacks=" + callbacks);

        SceUidManager.checkUidPurpose(uid, "IOFileManager-File", true);
        IoInfo info = filelist.get(uid);
        if (info == null) {
            Modules.log.warn("hleIoGetAsyncStat - unknown uid " + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_BAD_FILE_DESCRIPTOR;
        } else if (info.result == PSP_ERROR_NO_ASYNC_OP) {
            Modules.log.debug("hleIoGetAsyncStat - PSP_ERROR_NO_ASYNC_OP");
            wait = false;
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_NO_ASYNC_OP;
        } else if (info.asyncPending) {
            // Need to wait for context switch before we can allow the good result through
            Modules.log.debug("hleIoGetAsyncStat - 1 (busy)");
            Emulator.getProcessor().cpu.gpr[2] = 1;
        } else {
            if (info.closePending) {
                Modules.log.debug("hleIoGetAsyncStat - file marked with closePending, calling sceIoClose");
                sceIoClose(uid);
            }

            // Deferred error reporting from sceIoOpenAsync(filenotfound)
            if (info.result == PSP_ERROR_ASYNC_FILE_OPEN_ERROR) {
                filelist.remove(info.uid);
                SceUidManager.releaseUid(info.uid, "IOFileManager-File");
            }

            Memory mem = Memory.getInstance();
            if (mem.isAddressGood(res_addr)) {
                Modules.log.debug("hleIoGetAsyncStat - storing result 0x" + Long.toHexString(info.result));
                mem.write32(res_addr, (int)(info.result & 0xffffffffL));
                mem.write32(res_addr + 4, (int)((info.result >> 32) & 0xffffffffL));

                // flush out result after writing it once
                info.result = PSP_ERROR_NO_ASYNC_OP;
            }

            Emulator.getProcessor().cpu.gpr[2] = 0;
        }

        // TODO refactor so we can get logIoWaitAsync
        State.fileLogger.logIoPollAsync(Emulator.getProcessor().cpu.gpr[2], uid, res_addr);

        if (info != null && wait) {
            ThreadMan threadMan = ThreadMan.getInstance();
            SceKernelThreadInfo current_thread = threadMan.getCurrentThread();

            // Do callbacks?
            current_thread.do_callbacks = callbacks;

            // Go to wait state
            int timeout = 0;
            boolean forever = true;
            threadMan.hleKernelThreadWait(current_thread.wait, timeout, forever);

            // Wait on a specific file uid
            current_thread.wait.waitingOnIo = true;
            current_thread.wait.Io_id = info.uid;

            threadMan.changeThreadState(current_thread, PSP_THREAD_WAITING);
            threadMan.contextSwitch(threadMan.nextThread());
        } else if (callbacks) {
            ThreadMan.getInstance().yieldCurrentThreadCB();
        }
    }

    public void sceIoPollAsync(int uid, int res_addr) {
        if (debug) Modules.log.debug("sceIoPollAsync redirecting to hleIoGetAsyncStat");
        hleIoGetAsyncStat(uid, res_addr, false, false);
    }

    public void sceIoGetAsyncStat(int uid, int poll, int res_addr) {
        if (debug) Modules.log.debug("sceIoGetAsyncStat poll=0x" + Integer.toHexString(poll) + " redirecting to hleIoGetAsyncStat");
        hleIoGetAsyncStat(uid, res_addr, (poll == 0), false);
    }

    public void sceIoWaitAsync(int uid, int res_addr) {
        if (debug) Modules.log.debug("sceIoWaitAsync redirecting to hleIoGetAsyncStat");
        hleIoGetAsyncStat(uid, res_addr, true, false);
    }

    public void sceIoWaitAsyncCB(int uid, int res_addr) {
        if (debug) Modules.log.debug("sceIoWaitAsyncCB redirecting to hleIoGetAsyncStat");
        hleIoGetAsyncStat(uid, res_addr, true, true);
    }

    public void mkdirs(String dir) {
        String pcfilename = getDeviceFilePath(dir);
        if (pcfilename != null) {
            File f = new File(pcfilename);
            f.mkdirs();
        }
    }

    public SeekableDataInput getFile(String filename, int flags) {
        SeekableDataInput resultFile = null;

        String pcfilename = getDeviceFilePath(filename);
        if (pcfilename != null) {
            if (isUmdPath(pcfilename)) {
                // check umd is mounted
                if (iso == null) {
                    Modules.log.error("getFile - no umd mounted");
                    return resultFile;
                // check flags are valid
                } else if ((flags & PSP_O_WRONLY) == PSP_O_WRONLY ||
                    (flags & PSP_O_CREAT) == PSP_O_CREAT ||
                    (flags & PSP_O_TRUNC) == PSP_O_TRUNC) {
                    // should we refuse (return -1) or just ignore?
                    Modules.log.error("getFile - refusing to open umd media for write");
                    return resultFile;
                } else {
                    // open file
                    try {
                        UmdIsoFile file = iso.getFile(trimUmdPrefix(pcfilename));
                        resultFile = file;
                    } catch(FileNotFoundException e) {
                        if (debug) Modules.log.debug("getFile - umd file not found '" + pcfilename + "' (ok to ignore this message, debug purpose only)");
                    } catch(IOException e) {
                        Modules.log.error("getFile - error opening umd media: " + e.getMessage());
                    }
                }
            } else {
                // First check if the file already exists
                File file = new File(pcfilename);
                if (file.exists() &&
                    (flags & PSP_O_CREAT) == PSP_O_CREAT &&
                    (flags & PSP_O_EXCL) == PSP_O_EXCL) {
                    // PSP_O_CREAT + PSP_O_EXCL + file already exists = error
                    if (debug) Modules.log.debug("getFile - file already exists (PSP_O_CREAT + PSP_O_EXCL)");
                } else {
                    if (file.exists() &&
                        (flags & PSP_O_TRUNC) == PSP_O_TRUNC) {
                        if (debug) Modules.log.warn("getFile - file already exists, deleting UNIMPLEMENT (PSP_O_TRUNC)");
                        //file.delete();
                    }
                    String mode = getMode(flags);

                    try {
                        SeekableRandomFile raf = new SeekableRandomFile(pcfilename, mode);
                        resultFile = raf;
                    } catch (FileNotFoundException e) {
                        if (debug) Modules.log.debug("getFile - file not found '" + pcfilename + "' (ok to ignore this message, debug purpose only)");
                    }
                }
            }
        }

        return resultFile;
    }

    public SeekableDataInput getFile(int uid) {
        SceUidManager.checkUidPurpose(uid, "IOFileManager-File", true);
        IoInfo info = filelist.get(uid);
        if (info == null) {
            return null;
        } else {
            return info.readOnlyFile;
        }
    }

    public String getFileFilename(int uid) {
        SceUidManager.checkUidPurpose(uid, "IOFileManager-File", true);
        IoInfo info = filelist.get(uid);
        if (info == null) {
            return null;
        } else {
            return info.filename;
        }
    }

    private String getMode(int flags) {
        String mode = null;

        // PSP_O_RDWR check must come before the individual PSP_O_RDONLY and PSP_O_WRONLY checks
        if ((flags & PSP_O_RDWR) == PSP_O_RDWR) {
            mode = "rw";
        } else if ((flags & PSP_O_RDONLY) == PSP_O_RDONLY || flags == 0) {
            mode = "r";
        } else if ((flags & PSP_O_WRONLY) == PSP_O_WRONLY) {
            // SeekableRandomFile doesn't support write only
            mode = "rw";
        }

        return mode;
    }

    public void sceIoOpen(int filename_addr, int flags, int permissions) {
        String filename = readStringZ(filename_addr);
        if (debug) Modules.log.debug("sceIoOpen filename = " + filename + " flags = " + Integer.toHexString(flags) + " permissions = " + Integer.toOctalString(permissions));

        if (debug) {
            if ((flags & PSP_O_RDONLY) == PSP_O_RDONLY) Modules.log.debug("PSP_O_RDONLY");
            if ((flags & PSP_O_WRONLY) == PSP_O_WRONLY) Modules.log.debug("PSP_O_WRONLY");
            if ((flags & PSP_O_NBLOCK) == PSP_O_NBLOCK) Modules.log.debug("PSP_O_NBLOCK");
            if ((flags & PSP_O_DIROPEN) == PSP_O_DIROPEN) Modules.log.debug("PSP_O_DIROPEN");
            if ((flags & PSP_O_APPEND) == PSP_O_APPEND) Modules.log.debug("PSP_O_APPEND");
            if ((flags & PSP_O_CREAT) == PSP_O_CREAT) Modules.log.debug("PSP_O_CREAT");
            if ((flags & PSP_O_TRUNC) == PSP_O_TRUNC) Modules.log.debug("PSP_O_TRUNC");
            if ((flags & PSP_O_EXCL) == PSP_O_EXCL) Modules.log.debug("PSP_O_EXCL");
            if ((flags & PSP_O_NOWAIT) == PSP_O_NOWAIT) Modules.log.debug("PSP_O_NOWAIT");
        }

        String mode = getMode(flags);

        if (mode == null) {
            Modules.log.error("sceIoOpen - unhandled flags " + Integer.toHexString(flags));
            State.fileLogger.logIoOpen(-1, filename_addr, filename, flags, permissions, mode);
            Emulator.getProcessor().cpu.gpr[2] = -1;
            return;
        }

        // TODO we may want to do something with PSP_O_CREAT and permissions
        // using java File and its setReadable/Writable/Executable.
        // Does PSP filesystem even support permissions?

        // TODO PSP_O_TRUNC flag. delete the file and recreate it?

        // This could get messy, is it even allowed?
        if ((flags & PSP_O_RDONLY) == PSP_O_RDONLY &&
            (flags & PSP_O_APPEND) == PSP_O_APPEND) {
            Modules.log.warn("sceIoOpen - read and append flags both set!");
        }

        try {
            String pcfilename = getDeviceFilePath(filename);
            if (pcfilename != null) {
                if (debug) Modules.log.debug("sceIoOpen - opening file " + pcfilename);
                //if (debug) Modules.log.debug("sceIoOpen - isUmdPath " + isUmdPath(pcfilename));

                if (isUmdPath(pcfilename)) {
                    // check umd is mounted
                    if (iso == null) {
                        Modules.log.error("sceIoOpen - no umd mounted");
                        Emulator.getProcessor().cpu.gpr[2] = -1;

                    // check flags are valid
                    } else if ((flags & PSP_O_WRONLY) == PSP_O_WRONLY ||
                        (flags & PSP_O_CREAT) == PSP_O_CREAT ||
                        (flags & PSP_O_TRUNC) == PSP_O_TRUNC) {
                        // should we refuse (return -1) or just ignore?
                        Modules.log.error("sceIoOpen - refusing to open umd media for write");
                        Emulator.getProcessor().cpu.gpr[2] = -1;
                    } else {
                        // open file
                        try {
                            UmdIsoFile file = iso.getFile(trimUmdPrefix(pcfilename));
                            IoInfo info = new IoInfo(filename, file, mode, flags, permissions);
                            //info.result = info.uid;
                            info.result = PSP_ERROR_NO_ASYNC_OP;
                            Emulator.getProcessor().cpu.gpr[2] = info.uid;
                            if (debug) Modules.log.debug("sceIoOpen assigned uid = 0x" + Integer.toHexString(info.uid));
                        } catch(FileNotFoundException e) {
                            if (debug) Modules.log.debug("sceIoOpen - umd file not found (ok to ignore this message, debug purpose only)");
                            Emulator.getProcessor().cpu.gpr[2] = -1;
                        } catch(IOException e) {
                            Modules.log.error("sceIoOpen - error opening umd media: " + e.getMessage());
                            Emulator.getProcessor().cpu.gpr[2] = -1;
                        }
                    }
                } else {
                    // First check if the file already exists
                    File file = new File(pcfilename);
                    if (file.exists() &&
                        (flags & PSP_O_CREAT) == PSP_O_CREAT &&
                        (flags & PSP_O_EXCL) == PSP_O_EXCL) {
                        // PSP_O_CREAT + PSP_O_EXCL + file already exists = error
                        if (debug) Modules.log.debug("sceIoOpen - file already exists (PSP_O_CREAT + PSP_O_EXCL)");
                        Emulator.getProcessor().cpu.gpr[2] = -1;
                    } else {
                        if (file.exists() &&
                            (flags & PSP_O_TRUNC) == PSP_O_TRUNC) {
                            if (debug) Modules.log.warn("sceIoOpen - file already exists, deleting UNIMPLEMENT (PSP_O_TRUNC)");
                            //file.delete();
                        }

                        SeekableRandomFile raf = new SeekableRandomFile(pcfilename, mode);
                        IoInfo info = new IoInfo(filename, raf, mode, flags, permissions);
                        //info.result = info.uid;
                        info.result = PSP_ERROR_NO_ASYNC_OP;
                        Emulator.getProcessor().cpu.gpr[2] = info.uid;
                        if (debug) Modules.log.debug("sceIoOpen assigned uid = 0x" + Integer.toHexString(info.uid));
                    }
                }
            } else {
                Emulator.getProcessor().cpu.gpr[2] = -1;
            }
        } catch(FileNotFoundException e) {
            // To be expected under mode="r" and file doesn't exist
            if (debug) Modules.log.debug("sceIoOpen - file not found (ok to ignore this message, debug purpose only)");
            Emulator.getProcessor().cpu.gpr[2] = -1;
        }

        State.fileLogger.logIoOpen(Emulator.getProcessor().cpu.gpr[2],
                filename_addr, filename, flags, permissions, mode);
    }

    /** allocates an fd and returns it, even if the file could not be opened.
     * on the next successful poll/wait async 0x80010002 will be saved if the
     * file could not be opened. */
    public void sceIoOpenAsync(int filename_addr, int flags, int permissions) {
        if (debug) Modules.log.debug("sceIoOpenAsync redirecting to sceIoOpen");
        sceIoOpen(filename_addr, flags, permissions);
        asyncBusy = true;

        // TODO refactor sceIoOpen into hleIoOpen and add more parameters
        int uid = Emulator.getProcessor().cpu.gpr[2];
        IoInfo info = filelist.get(uid);
        if (info != null) {
            info.asyncPending = true;
            info.result = Emulator.getProcessor().cpu.gpr[2];
        } else {
            Modules.log.warn("sceIoOpenAsync file not found");
            // HACK
            info = new IoInfo(null, null, null, 0, 0);
            info.result = PSP_ERROR_ASYNC_FILE_OPEN_ERROR;
            info.asyncPending = true;
            Emulator.getProcessor().cpu.gpr[2] = info.uid;
        }
    }

    public void sceIoSetAsyncCallback(int uid, int cbid, int notifyArg) {
        if (debug) Modules.log.debug("sceIoSetAsyncCallback - uid " + Integer.toHexString(uid) + " cbid " + Integer.toHexString(cbid) + " arg 0x" + Integer.toHexString(notifyArg));

        SceUidManager.checkUidPurpose(uid, "IOFileManager-File", true);
        IoInfo info = filelist.get(uid);
        if (info == null) {
            Modules.log.warn("sceIoSetAsyncCallback - unknown uid " + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_BAD_FILE_DESCRIPTOR;
        } else {
            if (ThreadMan.getInstance().setIOCallback(cbid)) {
                info.cbid = cbid;
                info.notifyArg = notifyArg;
                Emulator.getProcessor().cpu.gpr[2] = 0;
            } else {
                Modules.log.warn("sceIoSetAsyncCallback - not a callback uid " + Integer.toHexString(uid));
                Emulator.getProcessor().cpu.gpr[2] = -1;
            }
        }
    }

    public void sceIoClose(int uid) {
        if (debug) Modules.log.debug("sceIoClose - uid " + Integer.toHexString(uid));

        try {
            SceUidManager.checkUidPurpose(uid, "IOFileManager-File", true);
            IoInfo info = filelist.remove(uid);
            if (info == null) {
                if (uid != 1 && uid != 2) // ignore stdout and stderr
                    Modules.log.warn("sceIoClose - unknown uid " + Integer.toHexString(uid));
                Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_BAD_FILE_DESCRIPTOR;
            } else {
                info.readOnlyFile.close();
                SceUidManager.releaseUid(info.uid, "IOFileManager-File");
                info.result = 0;
                Emulator.getProcessor().cpu.gpr[2] = 0;
            }
        } catch(IOException e) {
            Modules.log.error("pspiofilemgr - error closing file: " + e.getMessage());
            e.printStackTrace();
            Emulator.getProcessor().cpu.gpr[2] = -1;
        }

        State.fileLogger.logIoClose(Emulator.getProcessor().cpu.gpr[2], uid);
    }

    public void sceIoCloseAsync(int uid) {
        if (debug) Modules.log.debug("sceIoCloseAsync - uid " + Integer.toHexString(uid));

        SceUidManager.checkUidPurpose(uid, "IOFileManager-File", true);
        IoInfo info = filelist.get(uid);
        if (info != null) {
            info.result = 0;
            info.closePending = true;
            info.asyncPending = true;
            Emulator.getProcessor().cpu.gpr[2] = 0;
        } else {
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_BAD_FILE_DESCRIPTOR;
        }
    }

    private int updateResult(IoInfo info, int result, boolean async) {
        int newResult;

        if (info == null) {
            newResult = result;
        } else if (async) {
            if (info.asyncPending) {
                // if result isn't PSP_ERROR_ASYNC_BUSY we probably continued
                // with the function when we should have aborted early, this
                // will be a programmer error.
                if (result != PSP_ERROR_ASYNC_BUSY)
                    throw new RuntimeException("oops HLE wasn't coded right");
                newResult = result;
            } else {
                info.result = result;
                info.asyncPending = true;
                newResult = 0;
            }
        } else {
            info.result = PSP_ERROR_NO_ASYNC_OP;
            newResult = result;
        }

        return newResult;
    }

    private void hleIoWrite(int uid, int data_addr, int size, boolean async) {
        IoInfo info = null;
        int result;

        if (uid == 1) {
            // stdout
            String message = Utilities.stripNL(readStringNZ(data_addr, size));
            stdout.info(message);
            result = size;
        } else if (uid == 2) {
            // stderr
            String message = Utilities.stripNL(readStringNZ(data_addr, size));
            stderr.info(message);
            result = size;
        } else {
            if (debug) Modules.log.debug("hleIoWrite(uid=" + Integer.toHexString(uid) + ",data=0x" + Integer.toHexString(data_addr) + ",size=0x" + Integer.toHexString(size) + ") async=" + async);

            try {
                SceUidManager.checkUidPurpose(uid, "IOFileManager-File", true);
                info = filelist.get(uid);
                if (info == null) {
                    Modules.log.warn("hleIoWrite - unknown uid " + Integer.toHexString(uid));
                    result = PSP_ERROR_BAD_FILE_DESCRIPTOR;
                } else if (info.asyncPending) {
                    Modules.log.warn("hleIoWrite - uid " + Integer.toHexString(uid) + " PSP_ERROR_ASYNC_BUSY");
                    result = PSP_ERROR_ASYNC_BUSY;
                } else if ((data_addr >= MemoryMap.START_RAM ) && (data_addr + size <= MemoryMap.END_RAM)) {
                    if ((info.flags & PSP_O_APPEND) == PSP_O_APPEND) {
                        info.msFile.seek(info.msFile.length());
                    }

                    Utilities.write(info.msFile, data_addr, size);
                    result = size;
                } else {
                    Modules.log.warn("hleIoWrite - uid " + Integer.toHexString(uid)
                        + " data is outside of ram 0x" + Integer.toHexString(data_addr)
                        + " - 0x" + Integer.toHexString(data_addr + size));
                    result = -1;
                }
            } catch(IOException e) {
                e.printStackTrace();
                result = -1;
            }
        }

        Emulator.getProcessor().cpu.gpr[2] = updateResult(info, result, async);

        State.fileLogger.logIoWrite(Emulator.getProcessor().cpu.gpr[2], uid, data_addr, Emulator.getProcessor().cpu.gpr[6]);
    }

    public void sceIoWrite(int uid, int data_addr, int size) {
        hleIoWrite(uid, data_addr, size, false);
    }

    public void sceIoWriteAsync(int uid, int data_addr, int size) {
        hleIoWrite(uid, data_addr, size, true);
    }

    public void hleIoRead(int uid, int data_addr, int size, boolean async) {
        if (debug) Modules.log.debug("hleIoRead(uid=" + Integer.toHexString(uid) + ",data=0x" + Integer.toHexString(data_addr) + ",size=0x" + Integer.toHexString(size) + ") async=" + async);
        IoInfo info = null;
        int result;

        if (uid == 3) { // stdin
            // TODO?
            Modules.log.warn("UNIMPLEMENTED:hleIoRead uid = stdin");
            result = 0; // Fake
        } else {
            try {
                SceUidManager.checkUidPurpose(uid, "IOFileManager-File", true);
                info = filelist.get(uid);
                if (info == null) {
                    Modules.log.warn("hleIoRead - unknown uid " + Integer.toHexString(uid));
                    result = PSP_ERROR_BAD_FILE_DESCRIPTOR;
                } else if (info.asyncPending) {
                    Modules.log.warn("hleIoRead - uid " + Integer.toHexString(uid) + " PSP_ERROR_ASYNC_BUSY");
                    result = PSP_ERROR_ASYNC_BUSY;
                } else if ((data_addr >= MemoryMap.START_RAM ) && (data_addr + size <= MemoryMap.END_RAM)) {
                    // Using readFully for ms/umd compatibility, but now we must
                    // manually make sure it doesn't read off the end of the file.
                    if (info.readOnlyFile.getFilePointer() + size > info.readOnlyFile.length()) {
                        int oldSize = size;
                        size = (int)(info.readOnlyFile.length() - info.readOnlyFile.getFilePointer());
                        Modules.log.debug("hleIoRead - clamping size old=" + oldSize + " new=" + size
                            + " fp=" + info.readOnlyFile.getFilePointer() + " len=" + info.readOnlyFile.length());
                    }

                    Utilities.readFully(info.readOnlyFile, data_addr, size);
                    result = size;
                } else {
                    Modules.log.warn("hleIoRead - uid " + Integer.toHexString(uid)
                        + " data is outside of ram 0x" + Integer.toHexString(data_addr)
                        + " - 0x" + Integer.toHexString(data_addr + size));
                    result = PSP_ERROR_FILE_READ_ERROR;
                }
            } catch(IOException e) {
                e.printStackTrace();
                result = PSP_ERROR_FILE_READ_ERROR;
            } catch(Exception e) {
                e.printStackTrace();
                result = PSP_ERROR_FILE_READ_ERROR;

                Modules.log.error("hleIoRead: Check other console for exception details. Press Run to continue.");
                Emulator.PauseEmu();
            }
        }

        Emulator.getProcessor().cpu.gpr[2] = updateResult(info, result, async);

        State.fileLogger.logIoRead(Emulator.getProcessor().cpu.gpr[2], uid, data_addr, Emulator.getProcessor().cpu.gpr[6]);
    }

    public void sceIoRead(int uid, int data_addr, int size) {
        hleIoRead(uid, data_addr, size, false);
    }

    public void sceIoReadAsync(int uid, int data_addr, int size) {
        hleIoRead(uid, data_addr, size, true);
    }

    // TODO sceIoLseek with 64-bit return value
    public void sceIoLseek(int uid, long offset, int whence) {
        if (debug) Modules.log.debug("sceIoLseek - uid " + Integer.toHexString(uid) + " offset " + offset + " (hex=0x" + Long.toHexString(offset) + ") whence " + getWhenceName(whence));
        seek(uid, offset, whence, true, false);
    }

    public void sceIoLseekAsync(int uid, long offset, int whence) {
        if (debug) Modules.log.debug("sceIoLseekAsync - uid " + Integer.toHexString(uid) + " offset " + offset + " (hex=0x" + Long.toHexString(offset) + ") whence " + getWhenceName(whence));
        seek(uid, offset, whence, true, true);
        Emulator.getProcessor().cpu.gpr[2] = 0;
    }

    public void sceIoLseek32(int uid, int offset, int whence) {
        if (debug) Modules.log.debug("sceIoLseek32 - uid " + Integer.toHexString(uid) + " offset " + offset + " (hex=0x" + Integer.toHexString(offset) + ") whence " + getWhenceName(whence));
        seek(uid, ((long)offset & 0xFFFFFFFFL), whence, false, false);
    }

    public void sceIoLseek32Async(int uid, int offset, int whence) {
        if (debug) Modules.log.debug("sceIoLseek32Async - uid " + Integer.toHexString(uid) + " offset " + offset + " (hex=0x" + Integer.toHexString(offset) + ") whence " + getWhenceName(whence));
        seek(uid, ((long)offset & 0xFFFFFFFFL), whence, false, true);
        Emulator.getProcessor().cpu.gpr[2] = 0;
    }

    private String getWhenceName(int whence) {
        switch(whence) {
            case PSP_SEEK_SET: return "PSP_SEEK_SET";
            case PSP_SEEK_CUR: return "PSP_SEEK_CUR";
            case PSP_SEEK_END: return "PSP_SEEK_END";
            default: return "UNHANDLED " + whence;
        }
    }

    // TODO check what happens with PSP_ERROR_BAD_FILE_DESCRIPTOR 32bit error code on 64bit mode
    private void seek(int uid, long offset, int whence, boolean resultIs64bit, boolean async) {
        //if (debug) Modules.log.debug("seek - uid " + Integer.toHexString(uid) + " offset " + offset + " whence " + whence);

        if (uid == 1 || uid == 2 || uid == 3) { // stdio
            Modules.log.error("seek - can't seek on stdio uid " + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = -1;
            if (resultIs64bit)
                Emulator.getProcessor().cpu.gpr[3] = -1;
        } else {
            try {
                SceUidManager.checkUidPurpose(uid, "IOFileManager-File", true);
                IoInfo info = filelist.get(uid);
                if (info == null) {
                    Modules.log.warn("seek - unknown uid " + Integer.toHexString(uid));
                    Emulator.getProcessor().cpu.gpr[2] = -1;
                    if (resultIs64bit)
                        Emulator.getProcessor().cpu.gpr[3] = -1;
                } else {
                    switch(whence) {
                        case PSP_SEEK_SET:
                            if (offset > info.readOnlyFile.length()) {
                                Modules.log.warn("seek - offset (0x" + Long.toHexString(offset) + ") longer than file length (0x" + Long.toHexString(info.readOnlyFile.length()) + ")!");
                                Emulator.getProcessor().cpu.gpr[2] = -1;
                                if (resultIs64bit)
                                    Emulator.getProcessor().cpu.gpr[3] = -1;
                                State.fileLogger.logIoSeek64(-1, uid, offset, whence);
                                return;
                            } else if (offset < 0) {
                                Modules.log.warn("SEEK_SET UID " + Integer.toHexString(uid) + " filename:'" + info.filename + "' offset=0x" + Long.toHexString(offset) + " (less than 0!)");
                                Emulator.getProcessor().cpu.gpr[2] = -1;
                                if (resultIs64bit)
                                    Emulator.getProcessor().cpu.gpr[3] = -1;
                                State.fileLogger.logIoSeek64(-1, uid, offset, whence);
                                return;
                            }
                            info.readOnlyFile.seek(offset);
                            break;
                        case PSP_SEEK_CUR:
                            info.readOnlyFile.seek(info.readOnlyFile.getFilePointer() + offset);
                            break;
                        case PSP_SEEK_END:
                            info.readOnlyFile.seek(info.readOnlyFile.length() + offset);
                            break;
                        default:
                            Modules.log.error("seek - unhandled whence " + whence);
                            break;
                    }
                    long result = info.readOnlyFile.getFilePointer();

                    if (async) {
                        info.result = result;
                        info.asyncPending = true;
                    }

                    Emulator.getProcessor().cpu.gpr[2] = (int)(result & 0xFFFFFFFFL);
                    if (resultIs64bit)
                        Emulator.getProcessor().cpu.gpr[3] = (int)(result >> 32);
                }
            } catch(IOException e) {
                e.printStackTrace();
                Emulator.getProcessor().cpu.gpr[2] = -1;
                if (resultIs64bit)
                    Emulator.getProcessor().cpu.gpr[3] = -1;
            }
        }

        if (resultIs64bit) {
            State.fileLogger.logIoSeek64(
                    (long)(Emulator.getProcessor().cpu.gpr[2] & 0xFFFFFFFFL) | ((long)Emulator.getProcessor().cpu.gpr[3] << 32),
                    uid, offset, whence);
        } else {
            State.fileLogger.logIoSeek32(Emulator.getProcessor().cpu.gpr[2], uid, (int)offset, whence);
        }
    }

    public void sceIoMkdir(int dir_addr, int permissions) {
        String dir = readStringZ(dir_addr);
        if (debug) Modules.log.debug("sceIoMkdir dir = " + dir);
        //should work okay..
        String pcfilename = getDeviceFilePath(dir);
        if (pcfilename != null) {
            File f = new File(pcfilename);
            f.mkdir();
            Emulator.getProcessor().cpu.gpr[2] = 0;
        } else {
            Emulator.getProcessor().cpu.gpr[2] = -1;
        }

        State.fileLogger.logIoMkdir(Emulator.getProcessor().cpu.gpr[2], dir_addr, dir, permissions);
    }

    public void sceIoChdir(int path_addr) {
        String path = readStringZ(path_addr);
        if (debug) Modules.log.debug("sceIoChdir path = " + path);

        if (path.equals("..")) {
            // Go up one level
            int index = filepath.lastIndexOf("/");
            if (index != -1)
                filepath = filepath.substring(0, index);

            Modules.log.info("pspiofilemgr - filepath " + filepath);
            Emulator.getProcessor().cpu.gpr[2] = 0;
        } else {
            String pcfilename = getDeviceFilePath(path);
            if (pcfilename != null) {
                filepath = pcfilename;

                Modules.log.info("pspiofilemgr - filepath " + filepath);
                Emulator.getProcessor().cpu.gpr[2] = 0;
            } else {
                Emulator.getProcessor().cpu.gpr[2] = -1;
            }
        }

        State.fileLogger.logIoChdir(Emulator.getProcessor().cpu.gpr[2], path_addr, path);
    }

    public void sceIoDopen(int dirname_addr) {
        String dirname = readStringZ(dirname_addr);
        if (debug) Modules.log.debug("sceIoDopen dirname = " + dirname);

        String pcfilename = getDeviceFilePath(dirname);
        if (pcfilename != null) {

            if (isUmdPath(pcfilename)) {
                // Files in our iso virtual file system
                String isofilename = trimUmdPrefix(pcfilename);
                if (debug) Modules.log.debug("sceIoDopen - isofilename = " + isofilename);
                // check umd is mounted
                if (iso == null) {
                    Modules.log.error("sceIoDopen - no umd mounted");
                    Emulator.getProcessor().cpu.gpr[2] = -1;
                } else {
                    try {
                        if (iso.isDirectory(isofilename)) {
                            String[] filenames = iso.listDirectory(isofilename);
                            //if (debug) Modules.log.debug("sceIoDopen on umd, " + filenames.length + " files");
                            IoDirInfo info = new IoDirInfo(pcfilename, filenames);
                            Emulator.getProcessor().cpu.gpr[2] = info.uid;
                        } else {
                            if (debug) Modules.log.warn("sceIoDopen not a umd directory!");
                            Emulator.getProcessor().cpu.gpr[2] = -1;
                        }
                    } catch(FileNotFoundException e) {
                        Modules.log.warn("sceIoDopen - umd file not found");
                        Emulator.getProcessor().cpu.gpr[2] = -1;
                    } catch(IOException e) {
                        Modules.log.warn("sceIoDopen - umd io error: " + e.getMessage());
                        Emulator.getProcessor().cpu.gpr[2] = -1;
                    }
                }
            } else if (dirname.startsWith("/") && dirname.indexOf(":") != -1) {
                // Detect paths outside of our emulated mstick dir and show a helpful message
                // It is unsafe to try and support this, as an app could access any part of your computer instead of being limited to the ms0 dir
                Modules.log.warn("sceIoDopen apps running outside of ms0 dir are not fully supported, relative child paths should still work");
                Emulator.getProcessor().cpu.gpr[2] = -1;
            } else {
                // Regular apps inside mstick dir
                if (debug) Modules.log.debug("sceIoDopen - pcfilename = " + pcfilename);
                File f = new File(pcfilename);
                if (f.isDirectory()) {
                    IoDirInfo info = new IoDirInfo(pcfilename, f.list());
                    Emulator.getProcessor().cpu.gpr[2] = info.uid;
                } else {
                    if (debug) Modules.log.warn("sceIoDopen '" + pcfilename + "' not a directory! (could be missing)");
                    Emulator.getProcessor().cpu.gpr[2] = -1;
                }
            }
        } else {
            Emulator.getProcessor().cpu.gpr[2] = -1;
        }

        State.fileLogger.logIoDopen(Emulator.getProcessor().cpu.gpr[2], dirname_addr, dirname);
    }

    public void sceIoDread(int uid, int dirent_addr) {
        //if (debug) Modules.log.debug("sceIoDread - uid = " + Integer.toHexString(uid) + " dirent = " + Integer.toHexString(dirent_addr));

        SceUidManager.checkUidPurpose(uid, "IOFileManager-Directory", true);
        IoDirInfo info = dirlist.get(uid);
        if (info == null) {
            Modules.log.warn("sceIoDread unknown uid " + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_BAD_FILE_DESCRIPTOR;
        } else if (info.hasNext()) {
            String filename = info.next();
            Modules.log.debug("sceIoDread uid=" + Integer.toHexString(uid)
                + " #" + info.printableposition
                + " dir='" + info.path
                + "', file='" + filename + "'");

            SceIoStat stat = stat(info.path + "/" + filename);
            if (stat != null) {
                SceIoDirent dirent = new SceIoDirent(stat, filename);
                dirent.write(Memory.getInstance(), dirent_addr);
                Emulator.getProcessor().cpu.gpr[2] = 1; // TODO "> 0", so number of files remaining or 1 is ok?
            } else {
                Modules.log.warn("sceIoDread uid=" + Integer.toHexString(uid) + " stat failed (" + info.path + "/" + filename + ")");
                Emulator.getProcessor().cpu.gpr[2] = -1;
            }
        } else {
            Modules.log.debug("sceIoDread uid=" + Integer.toHexString(uid) + " no more files");
            Emulator.getProcessor().cpu.gpr[2] = 0;
        }

        // TODO would be nice to log which filename was stored
        State.fileLogger.logIoDread(Emulator.getProcessor().cpu.gpr[2], uid, dirent_addr);
    }

    public void sceIoDclose(int uid) {
        if (debug) Modules.log.debug("sceIoDclose - uid = " + Integer.toHexString(uid));

        SceUidManager.checkUidPurpose(uid, "IOFileManager-Directory", true);
        IoDirInfo info = dirlist.remove(uid);
        if (info == null) {
            Modules.log.warn("sceIoDclose - unknown uid " + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_BAD_FILE_DESCRIPTOR;
        } else {
            SceUidManager.releaseUid(info.uid, "IOFileManager-Directory");
            Emulator.getProcessor().cpu.gpr[2] = 0;
        }

        State.fileLogger.logIoDclose(Emulator.getProcessor().cpu.gpr[2], uid);
    }

    public void sceIoDevctl(int device_addr, int cmd, int indata_addr, int inlen, int outdata_addr, int outlen) {
        String device = readStringZ(device_addr);
        if (debug) {
            Modules.log.debug("sceIoDevctl(device='" + device
                + "',cmd=0x" + Integer.toHexString(cmd)
                + ",indata=0x" + Integer.toHexString(indata_addr)
                + ",inlen=" + inlen
                + ",outdata=0x" + Integer.toHexString(outdata_addr)
                + ",outlen=" + outlen + ")");

            Memory mem = Memory.getInstance();
            if (mem.isAddressGood(indata_addr)) {
                Modules.log.debug("sceIoDevctl *indata=0x" + Integer.toHexString(mem.read32(indata_addr)));
            }

            if (mem.isAddressGood(outdata_addr)) {
                Modules.log.debug("sceIoDevctl *outdata=0x" + Integer.toHexString(mem.read32(outdata_addr)));
            }
        }

        switch(cmd) {
            case 0x01F20001:
                Modules.log.warn("IGNORED: sceIoDevctl unhandled umd command " + String.format("0x%08X", cmd));
                Emulator.getProcessor().cpu.gpr[2] = 0; // Fake success
                break;

            case 0x02015804:
            case 0x02025801:
                Modules.log.warn("IGNORED: sceIoDevctl unhandled ms command " + String.format("0x%08X", cmd));
                Emulator.getProcessor().cpu.gpr[2] = 0; // Fake success
                break;

            case 0x02025806:
            {
                Modules.log.debug("sceIoDevctl check ms inserted");
                Memory mem = Memory.getInstance();
                if (mem.isAddressGood(outdata_addr)) {
                    // 0 = not inserted
                    // 1 = inserted
                    mem.write32(outdata_addr, 1);
                    Emulator.getProcessor().cpu.gpr[2] = 0;
                } else {
                    Emulator.getProcessor().cpu.gpr[2] = -1;
                }
                break;
            }

            case 0x02415821: // register ms eject callback
                Modules.log.debug("UNIMPLEMENTED: sceIoDevctl register ms eject callback");
                Emulator.getProcessor().cpu.gpr[2] = 0; // Fake success
                break;

            case 0x02415822: // unregister ms eject callback
                Modules.log.debug("UNIMPLEMENTED: sceIoDevctl unregister ms eject callback");
                Emulator.getProcessor().cpu.gpr[2] = 0; // Fake success
                break;


            case 0x02415823:
                Modules.log.warn("IGNORED: sceIoDevctl unhandled ms command " + String.format("0x%08X", cmd));
                Emulator.getProcessor().cpu.gpr[2] = 0; // Fake success
                break;

            case 0x02425818: // Free space on ms
            {
                int maxClusters = 512; // TODO fix these settings
                int freeClusters = 512;
                int maxSectors = 512;
                int sectorSize = 512;
                int sectorCount = 512;
                Memory mem = Memory.getInstance();
                if (mem.isAddressGood(indata_addr) && inlen >= 4) {
                    int addr = mem.read32(indata_addr);
                    if (mem.isAddressGood(addr)) {
                        mem.write32(addr, maxClusters);
                        mem.write32(addr + 4, freeClusters);
                        mem.write32(addr + 8, maxSectors);
                        mem.write32(addr + 12, sectorSize);
                        mem.write32(addr + 16, sectorCount);
                        Emulator.getProcessor().cpu.gpr[2] = 0;
                    } else {
                        Modules.log.warn("sceIoDevctl 0x02425818 bad save address " + String.format("0x%08X", addr));
                        Emulator.getProcessor().cpu.gpr[2] = -1;
                    }
                } else {
                    Modules.log.warn("sceIoDevctl 0x02425818 bad param address " + String.format("0x%08X", indata_addr) + " or size " + inlen);
                    Emulator.getProcessor().cpu.gpr[2] = -1;
                }
                break;
            }

            case 0x02425823:
                Modules.log.warn("IGNORED: sceIoDevctl unhandled ms command " + String.format("0x%08X", cmd));
                Emulator.getProcessor().cpu.gpr[2] = 0; // Fake success
                break;

            default:
                Modules.log.warn("sceIoDevctl unknown command " + String.format("0x%08X", cmd));
                Emulator.getProcessor().cpu.gpr[2] = -1; // Just fail for now
                break;
        }

        State.fileLogger.logIoDevctl(Emulator.getProcessor().cpu.gpr[2],
                device_addr, device, cmd, indata_addr, inlen, outdata_addr, outlen);
    }

    public void sceIoAssign(int dev1_addr, int dev2_addr, int dev3_addr, int mode, int unk1, int unk2) {
        String dev1 = readStringZ(dev1_addr);
        String dev2 = readStringZ(dev2_addr);
        String dev3 = readStringZ(dev3_addr);
        String perm;

        // IoAssignPerms
        switch(mode) {
        case 0: perm = "IOASSIGN_RDWR"; break;
        case 1: perm = "IOASSIGN_RDONLY"; break;
        default: perm = "unhandled " + mode; break;
        }

        Modules.log.warn("IGNORING:sceIoAssign(dev1='" + dev1
            + "',dev2='" + dev2
            + "',dev3='" + dev3
            + "',mode=" + perm
            + ",unk1=0x" + Integer.toHexString(unk1)
            + ",unk2=0x" + Integer.toHexString(unk2) + ")");

        Emulator.getProcessor().cpu.gpr[2] = 0; // Fake success
        //Emulator.getProcessor().cpu.gpr[2] = -1;

        State.fileLogger.logIoAssign(Emulator.getProcessor().cpu.gpr[2],
                dev1_addr, dev1, dev2_addr, dev2, dev3_addr, dev3, mode, unk1, unk2);
    }

    /** @param pcfilename can be null for convenience
     * @returns null on error */
    private SceIoStat stat(String pcfilename) {
        SceIoStat stat = null;
        if (pcfilename != null) {
            //if (debug) Modules.log.debug("stat - pcfilename = " + pcfilename);
            if (isUmdPath(pcfilename)) {
                // check umd is mounted
                if (iso == null) {
                    Modules.log.error("stat - no umd mounted");
                    Emulator.getProcessor().cpu.gpr[2] = -1;
                } else {
                    String isofilename = trimUmdPrefix(pcfilename);
                    try {
                        int mode = 4; // 4=readable
                        int attr = 0;
                        long size = 0;
                        long timestamp = 0;
                        int startSector = 0;

                        // Set attr (dir/file)
                        if (iso.isDirectory(isofilename)) {
                            attr |= 0x10;
                            mode |= 1; // 1=executable
                        } else { // isFile
                            attr |= 0x20;
                            UmdIsoFile file = iso.getFile(isofilename);
                            size = file.length();
                            timestamp = file.getTimestamp().getTime();
                            startSector = file.getStartSector();
                            //Modules.log.debug("stat - UMD File " + isofilename + ", StartSector=0x" + Integer.toHexString(startSector));
                        }

                        // Octal extend into user and group
                        mode = mode + mode * 8 + mode * 64;
                        // Copy attr into mode
                        mode |= attr << 8;

                        stat = new SceIoStat(mode, attr, size,
                            ScePspDateTime.fromUnixTime(timestamp),
                            ScePspDateTime.fromUnixTime(0),
                            ScePspDateTime.fromUnixTime(timestamp));
                        if (startSector > 0) {
                            stat.setReserved(0, startSector);
                        }
                    } catch(FileNotFoundException e) {
                        Modules.log.warn("stat - umd file not found");
                    } catch(IOException e) {
                        Modules.log.warn("stat - umd io error: " + e.getMessage());
                    }
                }
            } else {
                File file = new File(pcfilename);
                if (file.exists()) {
                    int mode = (file.canRead() ? 4 : 0) + (file.canWrite() ? 2 : 0) + (file.canExecute() ? 1 : 0);
                    int attr = 0;
                    long size = file.length();
                    long mtime = file.lastModified();

                    // Octal extend into user and group
                    mode = mode + mode * 8 + mode * 64;
                    //if (debug) Modules.log.debug("stat - permissions = " + Integer.toOctalString(mode));

                    // Set attr (dir/file) and copy into mode
                    if (file.isDirectory())
                        attr |= 0x10;
                    if (file.isFile())
                        attr |= 0x20;
                    mode |= attr << 8;

                    // Java can't see file create/access time
                    stat = new SceIoStat(mode, attr, size,
                        ScePspDateTime.fromUnixTime(0),
                        ScePspDateTime.fromUnixTime(0),
                        ScePspDateTime.fromUnixTime(mtime));
                }
            }
        }
        return stat;
    }

    public void sceIoGetstat(int file_addr, int stat_addr) {
        String filename = readStringZ(file_addr);
        if (debug) Modules.log.debug("sceIoGetstat - file = " + filename + " stat = " + Integer.toHexString(stat_addr));

        String pcfilename = getDeviceFilePath(filename);
        SceIoStat stat = stat(pcfilename);
        if (stat != null) {
            stat.write(Memory.getInstance(), stat_addr);
            Emulator.getProcessor().cpu.gpr[2] = 0;
        } else {
            Emulator.getProcessor().cpu.gpr[2] = -1;
        }

        // TODO move into stat()? that will also log on Dread
        State.fileLogger.logIoGetStat(Emulator.getProcessor().cpu.gpr[2],
                file_addr, filename, stat_addr);
    }

    //the following sets the filepath from memstick manager.
    public void setfilepath(String filepath)
    {
        // This could mess up... I don't think it's really needed anyway, just makes logging slightly nicer
        filepath = filepath.replaceAll("\\\\", "/");

        Modules.log.info("pspiofilemgr - filepath " + filepath);
        this.filepath = filepath;
    }

    public void setIsoReader(UmdIsoReader iso)
    {
        /* debug
        if (iso != null)
        {
            Modules.log.debug("pspiofilemgr - umd mounted " + iso.getFilename());
        }
        else
        {
            Modules.log.debug("pspiofilemgr - umd unmounted");
        }
        */
        this.iso = iso;

        // testing remapping of host0
        //getDeviceFilePath("host0:modules/module.cnf");
    }

    class IoInfo {
        // PSP settings
        public final int flags;
        public final int permissions;

        // Internal settings
        public final String filename;
        public final SeekableRandomFile msFile; // on memory stick, should either be identical to readOnlyFile or null
        public final SeekableDataInput readOnlyFile; // on memory stick or umd
        public final String mode;


        public final int uid;
        public long result; // The return value from the last operation on this file, used by sceIoWaitAsync
        public boolean closePending = false; // sceIoCloseAsync has been called on this file
        public boolean asyncPending; // Thread has not switched since an async operation was called on this file

        // Async callback
        public int cbid = -1;
        public int notifyArg = 0;

        /** Memory stick version */
        public IoInfo(String filename, SeekableRandomFile f, String mode, int flags, int permissions) {
            this.filename = filename;
            this.msFile = f;
            this.readOnlyFile = f;
            this.mode = mode;
            this.flags = flags;
            this.permissions = permissions;
            uid = SceUidManager.getNewUid("IOFileManager-File");
            filelist.put(uid, this);
        }

        /** UMD version (read only) */
        public IoInfo(String filename, SeekableDataInput f, String mode, int flags, int permissions) {
            this.filename = filename;
            this.msFile = null;
            this.readOnlyFile = f;
            this.mode = mode;
            this.flags = flags;
            this.permissions = permissions;
            uid = SceUidManager.getNewUid("IOFileManager-File");
            filelist.put(uid, this);
        }
    }

    class IoDirInfo {
        final String path;
        final String[] filenames;
        int position;
        int printableposition;
        final int uid;

        public IoDirInfo(String path, String[] filenames) {
            // iso reader doesn't like path//filename, so trim trailing /
            // (it's like doing cd somedir/ instead of cd somedir, makes little difference)
            if (path.endsWith("/"))
                path = path.substring(0, path.length() - 1);

            this.path = path;

            this.filenames = filenames;
            position = 0;
            printableposition = 0;

            // Hide iso special files
            if (filenames.length > position && filenames[position].equals("."))
                position++;
            if (filenames.length > position && filenames[position].equals("\01"))
                position++;

            uid = SceUidManager.getNewUid("IOFileManager-Directory");
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
                printableposition++;
            }
            return filename;
        }
    }

}
