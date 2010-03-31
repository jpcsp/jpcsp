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
import jpcsp.hardware.MemoryStick;
import jpcsp.util.Utilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import static jpcsp.util.Utilities.*;

import jpcsp.HLE.kernel.types.*;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.*;
import jpcsp.HLE.kernel.managers.*;
import jpcsp.State;

// TODO use file id's starting at around 0
// - saw one game check if it was between 0 and 31 inclusive
// - psplink allows 3 - 63 inclusive
// - without psplink its 3 - 12 inclusive
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
    public final static int PSP_O_UNKNOWN2 = 0x2000000; // seen on Puzzle Guzzle, Hammerin' Hero
    public final static int PSP_O_UNKNOWN3 = 0x40000000; // From "Kingdom Hearts: Birth by Sleep".

    //Every flag seems to be ORed with a retry count.
    //In Activision Hits Remixed, an error is produced after
    //the retry count (0xf0000/15) is over.
    public final static int PSP_O_RETRY_0   = 0x00000;
    public final static int PSP_O_RETRY_1   = 0x10000;
    public final static int PSP_O_RETRY_2   = 0x20000;
    public final static int PSP_O_RETRY_3   = 0x30000;
    public final static int PSP_O_RETRY_4   = 0x40000;
    public final static int PSP_O_RETRY_5   = 0x50000;
    public final static int PSP_O_RETRY_6   = 0x60000;
    public final static int PSP_O_RETRY_7   = 0x70000;
    public final static int PSP_O_RETRY_8   = 0x80000;
    public final static int PSP_O_RETRY_9   = 0x90000;
    public final static int PSP_O_RETRY_10  = 0xa0000;
    public final static int PSP_O_RETRY_11  = 0xb0000;
    public final static int PSP_O_RETRY_12  = 0xc0000;
    public final static int PSP_O_RETRY_13  = 0xd0000;
    public final static int PSP_O_RETRY_14  = 0xe0000;
    public final static int PSP_O_RETRY_15  = 0xf0000;

    public final static int PSP_SEEK_SET  = 0;
    public final static int PSP_SEEK_CUR  = 1;
    public final static int PSP_SEEK_END  = 2;

/* http://www.cngba.com/archiver/tid-17594723-page-2.html
0x80010001 = Operation is not permitted
0x80010002 = Associated file or directory does not exist
0x80010005 = Input/output error
0x80010007 = Argument list is too long
0x80010009 = Invalid file descriptor
0x8001000B = Resource is temporarily unavailable
0x8001000C = Not enough memory
0x8001000D = No file access permission
0x8001000E = Invalid address
0x80010010 = Mount or device is busy
0x80010011 = File exists
0x80010012 = Cross-device link
0x80010013 = Associated device was not found
0x80010014 = Not a directory
0x80010015 = Is a directory
0x80010016 = Invalid argument
0x80010018 = Too many files are open in the system
0x8001001B = File is too big
0x8001001C = No free space on device
0x8001001E = Read-only file system
0x80010024 = File name or path name is too long
0x80010047 = Protocol error
0x8001005A = Directory is not empty
0x8001005C = Too many symbolic links encountered
0x80010062 = Address is already in use
0x80010067 = Connection was aborted by software
0x80010068 = Connection was reset by communications peer
0x80010069 = Not enough free space in buffer
0x8001006E = Operation timed out
0x8001007B = No media was found
0x8001007C = Wrong medium type
0x80010084 = Quota exceeded
*/
    public final static int PSP_ERROR_FILE_NOT_FOUND = 0x80010002;
    //public final static int PSP_ERROR_FILE_OPEN_ERROR     = 0x80010003; // actual name unknown, no such device? bad format path name?
    public final static int PSP_ERROR_FILE_ALREADY_EXISTS = 0x80010011;
    public final static int PSP_ERROR_DEVICE_NOT_FOUND = 0x80010013;
    public final static int PSP_ERROR_INVALID_ARGUMENT = 0x80010016;
    public final static int PSP_ERROR_READ_ONLY = 0x8001001e;
    public final static int PSP_ERROR_NO_MEDIA = 0x8001007b;


    public final static int PSP_ERROR_FILE_READ_ERROR       = 0x80020130;
    public final static int PSP_ERROR_TOO_MANY_OPEN_FILES   = 0x80020320;
    public final static int PSP_ERROR_NO_SUCH_DEVICE   = 0x80020321; // also means device isn't available/mounted, such as ms not in
    public final static int PSP_ERROR_BAD_FILE_DESCRIPTOR   = 0x80020323;
    public final static int PSP_ERROR_UNSUPPORTED_OPERATION = 0x80020325;
    public final static int PSP_ERROR_NOCWD                 = 0x8002032c; // TODO
    public final static int PSP_ERROR_FILENAME_TOO_LONG     = 0x8002032d;
    public final static int PSP_ERROR_ASYNC_BUSY            = 0x80020329;
    public final static int PSP_ERROR_NO_ASYNC_OP           = 0x8002032a;
    public final static int PSP_ERROR_DEVCTL_BAD_PARAMS     = 0x80220081; // actual name unknown

    // modeStrings indexed by [0, PSP_O_RDONLY, PSP_O_WRONLY, PSP_O_RDWR]
    // SeekableRandomFile doesn't support write only: take "rw",
    private final static String[] modeStrings = { "r", "r", "rw", "rw" };

    private HashMap<Integer, IoInfo> filelist;
    private HashMap<Integer, IoDirInfo> dirlist;

    private String filepath; // current working directory on PC
    private UmdIsoReader iso;

    private HashMap<Integer, SceKernelThreadInfo> asyncThreadMap;
    private SceKernelThreadInfo currentAsyncThread;

    private byte[] AES128Key = new byte[16];

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
        MemoryStick.setState(MemoryStick.PSP_MEMORYSTICK_STATE_INSERTED);
        asyncThreadMap = new HashMap<Integer, SceKernelThreadInfo>();
    }

    /** To properly emulate async io we cannot allow async io operations to
     * complete immediately. For example a call to sceIoPollAsync must be
     * preceeded by at least one context switch to return success, otherwise it
     * returns async busy.
     * TODO could make async io succeed based on at least one context switch
     * (as it currently does) PLUS a certain amount of time passed. */
    public void onContextSwitch() {
        IoInfo found = null;
        int foundCount = 0;

        for (Iterator<IoInfo> it = filelist.values().iterator(); it.hasNext();) {
            IoInfo info = it.next();

            if (info.asyncPending)
                foundCount++;

            if (info.asyncPending && found == null) {
                found = info;
                // This is based on the assumption only 1 IO op can be
                // happening at a time, which is probably correct since the
                // PSP_ERROR_ASYNC_BUSY error code exists.
                //break;
            }

        }

        if (foundCount > 1)
            Modules.log.warn("more than 1 io callback waiting to enter pending state!");

        if (found != null) {
            found.asyncPending = false;

            if (found.cbid >= 0) {
                ThreadMan.getInstance().pushCallback(SceKernelThreadInfo.THREAD_CALLBACK_IO, found.cbid, found.notifyArg);
            }

            // Find threads waiting on this uid and wake them up
            // TODO If the call was sceIoWaitAsyncCB we might need to make sure
            // the callback is fully processed before waking the thread!
            for (Iterator<SceKernelThreadInfo> it = ThreadMan.getInstance().iterator(); it.hasNext(); ) {
                SceKernelThreadInfo thread = it.next();

                if (thread.wait.waitingOnIo &&
                    thread.wait.Io_id == found.uid) {
                    Modules.log.debug("pspiofilemgr - onContextSwitch waking " + Integer.toHexString(thread.uid) + " thread:'" + thread.name + "'");

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
        //Modules.log.debug("getDeviceFilePath input filepath='" + filepath + "' pspfilename='" + pspfilename + "'");
        pspfilename = pspfilename.replaceAll("\\\\", "/");
        String device = null;
        String cwd = "";
        String filename = null;

        int findcolon = pspfilename.indexOf(":");
        if (findcolon != -1) {
            // Device absolute
            // dev:path
            // dev:/path
            device = pspfilename.substring(0, findcolon);
            pspfilename = pspfilename.substring(findcolon + 1);
            //Modules.log.debug("getDeviceFilePath device='" + device + "' pspfilename='" + pspfilename + "'");
        } else {
            // Relative
            // path - relative to cwd
            // /path - relative to cwd
            int findslash = filepath.indexOf("/");
            if (findslash != -1) {
                device = filepath.substring(0, findslash);
                cwd = filepath.substring(findslash + 1);
                //Modules.log.debug("getDeviceFilePath device='" + device + "' cwd='" + cwd + "'");

                if (cwd.startsWith("/")) {
                    cwd = cwd.substring(1);
                }
                if (cwd.endsWith("/")) {
                    cwd = cwd.substring(0, cwd.length() - 1);
                }
            } else {
                device = filepath;
                //Modules.log.debug("getDeviceFilePath device='" + device + "'");
            }
        }

        // remap host0
        // - Bliss Island - ULES00616
        if (device.equals("host0")) {
            if (iso != null) {
                device = "disc0";
            } else {
                device = "ms0";
            }
        }

        // remap fatms0
        // - Wipeout Pure - UCUS98612
        if (device.equals("fatms0")) {
            device = "ms0";
        }

        // strip leading and trailing slash from supplied path
        // this step is common to absolute and relative paths
        if (pspfilename.startsWith("/")) {
            pspfilename = pspfilename.substring(1);
        }
        if (pspfilename.endsWith("/")) {
            pspfilename = pspfilename.substring(0, pspfilename.length() - 1);
        }

        // assemble final path
        // convert device to lower case here for case sensitive file systems (linux) and also for isUmdPath and trimUmdPrefix regex
        // - GTA: LCS uses upper case device DISC0
        // - The Fast and the Furious uses upper case device DISC0
        filename = device.toLowerCase();
        if (cwd.length() > 0) {
            filename += "/" + cwd;
        }
        if (pspfilename.length() > 0) {
            filename += "/" + pspfilename;
        }

        //Modules.log.debug("getDeviceFilePath output filename='" + filename + "'");
        return filename;
    }

    private final String[] umdPrefixes = new String[] {
        "disc", "umd"
    };

    private boolean isUmdPath(String deviceFilePath) {
        // Assume the device name is always lower case (ensured by getDeviceFilePath)
        // Assume there is always a device number
        for (int i = 0; i < umdPrefixes.length; i++) {
            if (deviceFilePath.matches("^" + umdPrefixes[i] + "[0-9]+.*"))
                return true;
        }

        return false;
    }

    private String trimUmdPrefix(String pcfilename) {
        // Assume the device name is always lower case (ensured by getDeviceFilePath)
        // Assume there is always a device number
        // Handle case where file path is blank so there is no slash after the device name
        for (int i = 0; i < umdPrefixes.length; i++) {
            if (pcfilename.matches("^" + umdPrefixes[i] + "[0-9]+/.*"))
                return pcfilename.substring(pcfilename.indexOf("/") + 1);
            else if (pcfilename.matches("^" + umdPrefixes[i] + "[0-9]+"))
                return "";
        }

        // Now make sure getDeviceFilePath is working properly - keep the old behaviour as a fallback
        // TODO eventually delete all this once we're sure it's working
        Modules.log.warn("trimUmdPrefix falling back to old routine, input='" + pcfilename + "'");

        if (pcfilename.toLowerCase().startsWith("disc0/"))
            return pcfilename.substring(6);
        if (pcfilename.toLowerCase().startsWith("umd0/"))
            return pcfilename.substring(5);
        if (pcfilename.toLowerCase().startsWith("umd1/"))
            return pcfilename.substring(5);

        if (pcfilename.toLowerCase().startsWith("disc0"))
            return pcfilename.substring(5);
        if (pcfilename.toLowerCase().startsWith("umd0"))
            return pcfilename.substring(4);
        if (pcfilename.toLowerCase().startsWith("umd1"))
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
            Modules.log.warn("hleIoGetAsyncStat - unknown uid " + Integer.toHexString(uid) + ", not waiting");
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_BAD_FILE_DESCRIPTOR;
        } else if (info.result == PSP_ERROR_NO_ASYNC_OP) {
            Modules.log.debug("hleIoGetAsyncStat - PSP_ERROR_NO_ASYNC_OP, not waiting");
            wait = false;
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_NO_ASYNC_OP;
        } else if (info.asyncPending && !wait) {
            // Need to wait for context switch before we can allow the good result through
            Modules.log.debug("hleIoGetAsyncStat - poll return=1(busy), not waiting");
            Emulator.getProcessor().cpu.gpr[2] = 1;
        } else {
            if (info.closePending) {
                Modules.log.debug("hleIoGetAsyncStat - file marked with closePending, calling sceIoClose, not waiting");
                sceIoClose(uid);
                wait = false;
            }

            // Deferred error reporting from sceIoOpenAsync(filenotfound)
            if (info.result == (PSP_ERROR_FILE_NOT_FOUND & 0xffffffffL)) {
                Modules.log.debug("hleIoGetAsyncStat - file not found, not waiting");
                filelist.remove(info.uid);
                SceUidManager.releaseUid(info.uid, "IOFileManager-File");
                wait = false;
            }

            // Hack: if we are in a callback, complete the Async IO immediately.
            // This is required as long as we can't context switch inside a callback.
            if (ThreadMan.getInstance().isInsideCallback()) {
            	onContextSwitch();
            }

            // This case happens when the game switches thread before calling waitAsync,
            // example: sceIoReadAsync -> sceKernelDelayThread -> sceIoWaitAsync
            // Technically we should wait at least some time, since tests show
            // a load of sceKernelDelayThread before sceIoWaitAsync won't make
            // the async io complete (maybe something to do with thread priorities).
            if (!info.asyncPending) {
                Modules.log.debug("hleIoGetAsyncStat - already context switched, not waiting");
                wait = false;
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

        if (wait) {
            State.fileLogger.logIoWaitAsync(Emulator.getProcessor().cpu.gpr[2], uid, res_addr);
        } else {
            State.fileLogger.logIoPollAsync(Emulator.getProcessor().cpu.gpr[2], uid, res_addr);
        }

        if (info != null && wait) {
            ThreadMan threadMan = ThreadMan.getInstance();
            SceKernelThreadInfo current_thread = threadMan.getCurrentThread();

            // Do callbacks?
            current_thread.do_callbacks = callbacks;

            // wait type
            current_thread.waitType = PSP_WAIT_MISC;

            // Go to wait state
            int timeout = 0;
            boolean forever = true;
            //int timeout = 1000000;
            //boolean forever = false;
            threadMan.hleKernelThreadWait(current_thread.wait, timeout, forever);

            // Wait on a specific file uid
            current_thread.wait.waitingOnIo = true;
            current_thread.wait.Io_id = info.uid;

            threadMan.changeThreadState(current_thread, PSP_THREAD_WAITING);
            threadMan.contextSwitch(threadMan.nextThread());
        } else if (callbacks && !ThreadMan.getInstance().isInsideCallback()) {
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

    private static class PatternFilter implements FilenameFilter {
    	private Pattern pattern;

    	public PatternFilter(String pattern) {
    		this.pattern = Pattern.compile(pattern);
    	}

		@Override
		public boolean accept(File dir, String name) {
			return pattern.matcher(name).matches();
		}
    };

    public String[] listFiles(String dir, String pattern) {
    	String pcfilename = getDeviceFilePath(dir);
    	if (pcfilename == null) {
    		return null;
    	}

    	File f = new File(pcfilename);
    	String[] list = f.list(new PatternFilter(pattern));

    	return list;
    }

    public SceIoStat statFile(String pspfilename) {
    	String pcfilename = getDeviceFilePath(pspfilename);
    	if (pcfilename == null) {
    		return null;
    	}

    	return stat(pcfilename);
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
    	return modeStrings[flags & PSP_O_RDWR];
    }

    public void hleIoOpen(int filename_addr, int flags, int permissions, boolean async) {
        String filename = readStringZ(filename_addr);
        if (debug) Modules.log.info("hleIoOpen filename = " + filename + " flags = " + Integer.toHexString(flags) + " permissions = 0" + Integer.toOctalString(permissions));

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
        if ((flags & PSP_O_UNKNOWN1) == PSP_O_UNKNOWN1) Modules.log.warn("UNIMPLEMENTED:hleIoOpen flags=PSP_O_UNKNOWN1 file='" + filename + "'");
        if ((flags & PSP_O_UNKNOWN2) == PSP_O_UNKNOWN2) Modules.log.warn("UNIMPLEMENTED:hleIoOpen flags=PSP_O_UNKNOWN2 file='" + filename + "'");
        if ((flags & PSP_O_UNKNOWN3) == PSP_O_UNKNOWN3) Modules.log.warn("PARTIAL:hleIoOpen flags=PSP_O_UNKNOWN3 file='" + filename + "'");

        String mode = getMode(flags);

        if (mode == null) {
            Modules.log.error("hleIoOpen - unhandled flags " + Integer.toHexString(flags));
            State.fileLogger.logIoOpen(-1, filename_addr, filename, flags, permissions, mode);
            Emulator.getProcessor().cpu.gpr[2] = -1;
            return;
        }

        //Retry count.
        int retry = (flags >> 16) & 0x000F;

        if (retry != 0) {
            Modules.log.warn("hleIoOpen - retry count is " + retry);
        }

        // TODO we may want to do something with PSP_O_CREAT and permissions
        // using java File and its setReadable/Writable/Executable.
        // Does PSP filesystem even support permissions?

        // TODO PSP_O_TRUNC flag. delete the file and recreate it?

        // This could get messy, is it even allowed?
        if ((flags & PSP_O_RDONLY) == PSP_O_RDONLY &&
            (flags & PSP_O_APPEND) == PSP_O_APPEND) {
            Modules.log.warn("hleIoOpen - read and append flags both set!");
        }

        try {
            String pcfilename = getDeviceFilePath(filename);
            if (pcfilename != null) {
                if (debug) Modules.log.debug("hleIoOpen - opening file " + pcfilename);

                if (isUmdPath(pcfilename)) {
                    // check umd is mounted
                    if (iso == null) {
                        Modules.log.error("hleIoOpen - no umd mounted");
                        Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_DEVICE_NOT_FOUND;

                    // check umd is activated
                    } else if (!Modules.sceUmdUserModule.isUmdActivated()) {
                        Modules.log.warn("hleIoOpen - umd mounted but not activated");
                        Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_NO_SUCH_DEVICE;

                    // check flags are valid
                    } else if ((flags & PSP_O_WRONLY) == PSP_O_WRONLY ||
                        (flags & PSP_O_CREAT) == PSP_O_CREAT ||
                        (flags & PSP_O_TRUNC) == PSP_O_TRUNC) {
                        // should we refuse (return -1) or just ignore?
                        Modules.log.error("hleIoOpen - refusing to open umd media for write");
                        Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_READ_ONLY;
                    } else {
                        // open file

                        //Tests revealed that apps are already prepared
                        //to wait for the retry count of each file.
                        for(int i = 0; i <= retry; i++) {
                            try {
                                String trimmedFileName = trimUmdPrefix(pcfilename);
                                UmdIsoFile file = iso.getFile(trimmedFileName);
                                IoInfo info = new IoInfo(filename, file, mode, flags, permissions);

                                if ((flags & PSP_O_UNKNOWN3) == PSP_O_UNKNOWN3)
                                    info.isEncrypted = true;

                                if (trimmedFileName != null && trimmedFileName.length() == 0) {
                                    // Opening "umd0:" is allowing to read the whole UMD per sectors.
                                    info.sectorBlockMode = true;
                                }
                                //info.result = info.uid;
                                info.result = PSP_ERROR_NO_ASYNC_OP;
                                Emulator.getProcessor().cpu.gpr[2] = info.uid;
                                if (debug) Modules.log.debug("hleIoOpen assigned uid = 0x" + Integer.toHexString(info.uid));
                            } catch(FileNotFoundException e) {
                                if (debug) Modules.log.warn("hleIoOpen - umd file not found (ok to ignore this message, debug purpose only)");
                                Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_FILE_NOT_FOUND;
                            } catch(IOException e) {
                                Modules.log.error("hleIoOpen - error opening umd media: " + e.getMessage());
                                Emulator.getProcessor().cpu.gpr[2] = -1;
                            }
                        }
                    }
                } else {
                    // First check if the file already exists
                    File file = new File(pcfilename);
                    if (file.exists() && (flags & PSP_O_CREAT) == PSP_O_CREAT &&
                        (flags & PSP_O_EXCL) == PSP_O_EXCL) {
                        // PSP_O_CREAT + PSP_O_EXCL + file already exists = error
                        if (debug) Modules.log.debug("hleIoOpen - file already exists (PSP_O_CREAT + PSP_O_EXCL)");
                        Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_FILE_ALREADY_EXISTS;
                    } else {
                        if (file.exists() && (flags & PSP_O_TRUNC) == PSP_O_TRUNC) {
                            if (debug) Modules.log.warn("hleIoOpen - file already exists, deleting UNIMPLEMENT (PSP_O_TRUNC)");
                            //file.delete();
                        }

                        SeekableRandomFile raf = new SeekableRandomFile(pcfilename, mode);
                        IoInfo info = new IoInfo(filename, raf, mode, flags, permissions);
                        //info.result = info.uid;
                        info.result = PSP_ERROR_NO_ASYNC_OP; // sceIoOpenAsync will set this properly
                        Emulator.getProcessor().cpu.gpr[2] = info.uid;
                        if (debug) Modules.log.debug("hleIoOpen assigned uid = 0x" + Integer.toHexString(info.uid));
                    }
                }
            } else {
                // something went wrong converting the pspfilename to pcfilename (maybe it was blank?)
                Emulator.getProcessor().cpu.gpr[2] = -1;
            }
        } catch(FileNotFoundException e) {
            // To be expected under mode="r" and file doesn't exist
            if (debug) Modules.log.warn("hleIoOpen - file not found (ok to ignore this message, debug purpose only)");
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_FILE_NOT_FOUND;
        }

        if(async) {
            // TODO make asyncPending a global? only allow 1 async op across all io operations regardless of the uid?
            int uid = Emulator.getProcessor().cpu.gpr[2];
            IoInfo info = filelist.get(uid);
            if (info != null) {
                info.asyncPending = true;
                info.result = Emulator.getProcessor().cpu.gpr[2];
            } else {
                Modules.log.debug("sceIoOpenAsync - file not found (ok to ignore this message, debug purpose only)");
                // For async we still need to make and return a file handle even if we couldn't open the file,
                // this is so the game can query on the handle (wait/async stat/io callback).
                info = new IoInfo(readStringZ(filename_addr), null, null, flags, permissions);
                info.result = PSP_ERROR_FILE_NOT_FOUND & 0xffffffffL;
                //info.asyncPending = true;
                Emulator.getProcessor().cpu.gpr[2] = info.uid;
            }
            if(currentAsyncThread != null)
                asyncThreadMap.put(uid, currentAsyncThread);
        }

        State.fileLogger.logIoOpen(Emulator.getProcessor().cpu.gpr[2],
                filename_addr, filename, flags, permissions, mode);
    }

     public void sceIoOpen(int filename_addr, int flags, int permissions) {
         if (debug) Modules.log.debug("sceIoOpen redirecting to hleIoOpen");
         hleIoOpen(filename_addr, flags, permissions, false);
     }

    /** allocates an fd and returns it, even if the file could not be opened.
     * on the next successful poll/wait async 0x80010002 will be saved if the
     * file could not be opened. */
    public void sceIoOpenAsync(int filename_addr, int flags, int permissions) {
        if (debug) Modules.log.debug("sceIoOpenAsync redirecting to hleIoOpen");

        //Start async thread (only 1 at a time allowed)?
        //The app should call sceIoCloseAsync to delete it.
        ThreadMan threadMan = ThreadMan.getInstance();

        //Inherit priority from current thread.
        int asyncPriority = threadMan.getCurrentThread().currentPriority;

        currentAsyncThread = threadMan.hleKernelCreateThread("SceIofileAsync",
                ThreadMan.ASYNC_LOOP_ADDRESS, asyncPriority, 0x2000,
                threadMan.getCurrentThread().attr, 0);

        threadMan.ThreadMan_sceKernelStartThread(currentAsyncThread.uid, 4, ThreadMan.ASYNC_LOOP_ADDRESS);

        hleIoOpen(filename_addr, flags, permissions, true);
    }

    public void sceIoChangeAsyncPriority(int uid, int priority) {
        SceUidManager.checkUidPurpose(uid, "IOFileManager-File", true);

        if(asyncThreadMap.get(uid) != null) {
            SceKernelThreadInfo asyncThread = asyncThreadMap.get(uid);
            asyncThread.currentPriority = priority;

            Modules.log.info("sceIoChangeAsyncPriority changing priority of dummy async thread from fd=" +
                uid + " to " + Integer.toHexString(priority));
        }
        else {
            //TODO
            //Some games call sceIoChangeAsyncPriority without
            //creating the thread first.
            //Return an error?
            Modules.log.warn("sceIoChangeAsyncPriority invalid fd=" + uid);
        }

        Emulator.getProcessor().cpu.gpr[2] = 0;
    }

    public void sceIoSetAsyncCallback(int uid, int cbid, int notifyArg) {
        if (debug) Modules.log.debug("sceIoSetAsyncCallback - uid " + Integer.toHexString(uid) + " cbid " + Integer.toHexString(cbid) + " arg 0x" + Integer.toHexString(notifyArg));

        SceUidManager.checkUidPurpose(uid, "IOFileManager-File", true);
        IoInfo info = filelist.get(uid);
        if (info == null) {
            Modules.log.warn("sceIoSetAsyncCallback - unknown uid " + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_BAD_FILE_DESCRIPTOR;
        } else {
            if (ThreadMan.getInstance().setCallback(SceKernelThreadInfo.THREAD_CALLBACK_IO, cbid)) {
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
            if (info.asyncPending) {
                Modules.log.warn("sceIoCloseAsync - uid " + Integer.toHexString(uid) + " PSP_ERROR_ASYNC_BUSY");
                Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_ASYNC_BUSY;
            } else {
                info.result = 0;
                info.closePending = true;
                info.asyncPending = true;
                Emulator.getProcessor().cpu.gpr[2] = 0;
            }
        } else {
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_BAD_FILE_DESCRIPTOR;
        }
        
        if(currentAsyncThread != null)
            ThreadMan.getInstance().ThreadMan_sceKernelDeleteThread(currentAsyncThread.uid);
    }

    // Handle returning/storing result for sync/async operations
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

    // Try to decrypt a file with a given AES-128 bit key.
    private boolean decryptAES128(IoInfo info, int addr, int size) {
        byte[] encFile = null;
        byte[] decFile = null;
        boolean res = false;
        SecretKeySpec keySpec = new SecretKeySpec(AES128Key, "AES");
        Memory mem = Memory.getInstance();

        if(AES128Key != null) {
            try {
                encFile = new byte[(int)info.readOnlyFile.length()];
                info.readOnlyFile.readFully(encFile);
                Cipher c = Cipher.getInstance("AES");
                c.init(Cipher.DECRYPT_MODE, keySpec);
                decFile = c.doFinal(encFile);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if(decFile != null) {
                for(int i = 0; i < size; i++) {
                    mem.write8(addr+i, decFile[i]);
                }
                res = true;
            }
        }

        return res;
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
                } else if ((data_addr < MemoryMap.START_RAM ) && (data_addr + size > MemoryMap.END_RAM)) {
                    Modules.log.warn("hleIoWrite - uid " + Integer.toHexString(uid)
                        + " data is outside of ram 0x" + Integer.toHexString(data_addr)
                        + " - 0x" + Integer.toHexString(data_addr + size));
                    result = -1;
                } else {
                    if ((info.flags & PSP_O_APPEND) == PSP_O_APPEND) {
                        info.msFile.seek(info.msFile.length());
                        info.position = info.msFile.length();
                    }

                    // if the position is off the end, pad with junk
                    if (info.position > info.readOnlyFile.length()) {
                        byte[] junk = new byte[512];
                        int towrite = (int)(info.position - info.readOnlyFile.length());

                        info.msFile.seek(info.msFile.length());
                        while(towrite >= 512) {
                            info.msFile.write(junk, 0, 512);
                            towrite -= 512;
                        }
                        if (towrite > 0) {
                            info.msFile.write(junk, 0, towrite);
                        }
                    }

                    info.position += size;

                    Utilities.write(info.msFile, data_addr, size);
                    result = size;
                }
            } catch(IOException e) {
                e.printStackTrace();
                result = -1;
            }
        }

        Emulator.getProcessor().cpu.gpr[2] = updateResult(info, result, async);

        State.fileLogger.logIoWrite(Emulator.getProcessor().cpu.gpr[2], uid, data_addr, Emulator.getProcessor().cpu.gpr[6], size);
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
                    // Can't execute another operation until the previous one completed
                    Modules.log.warn("hleIoRead - uid " + Integer.toHexString(uid) + " PSP_ERROR_ASYNC_BUSY");
                    result = PSP_ERROR_ASYNC_BUSY;
                } else if ((data_addr < MemoryMap.START_RAM ) && (data_addr + size > MemoryMap.END_RAM)) {
                    Modules.log.warn("hleIoRead - uid " + Integer.toHexString(uid)
                        + " data is outside of ram 0x" + Integer.toHexString(data_addr)
                        + " - 0x" + Integer.toHexString(data_addr + size));
                    result = PSP_ERROR_FILE_READ_ERROR;
                } else if (info.position >= info.readOnlyFile.length()) {
                    // Allow seeking off the end of the file, just return 0 bytes read/written
                    result = 0;
                } else {
					if (info.sectorBlockMode) {
                		// In sectorBlockMode, the size is a number of sectors
						size *= UmdIsoFile.sectorLength;
					}

					// Using readFully for ms/umd compatibility, but now we must
                    // manually make sure it doesn't read off the end of the file.
                    if (info.readOnlyFile.getFilePointer() + size > info.readOnlyFile.length()) {
                        int oldSize = size;
                        size = (int)(info.readOnlyFile.length() - info.readOnlyFile.getFilePointer());
                        Modules.log.debug("hleIoRead - clamping size old=" + oldSize + " new=" + size
                            + " fp=" + info.readOnlyFile.getFilePointer() + " len=" + info.readOnlyFile.length());
                    }

                    info.position += size; // check - use clamping or not

                    // Check for encrypted files.
                    if(info.isEncrypted)
                        Modules.log.warn("hleIoRead - encrypted file detected.");

                    Utilities.readFully(info.readOnlyFile, data_addr, size);

                    result = size;
                    if (info.sectorBlockMode) {
                    	result /= UmdIsoFile.sectorLength;
                    }
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

        State.fileLogger.logIoRead(Emulator.getProcessor().cpu.gpr[2], uid, data_addr, Emulator.getProcessor().cpu.gpr[6], size);
    }

    public void sceIoRead(int uid, int data_addr, int size) {
        hleIoRead(uid, data_addr, size, false);
    }

    public void sceIoReadAsync(int uid, int data_addr, int size) {
        hleIoRead(uid, data_addr, size, true);
    }

    public void sceIoLseek(int uid, long offset, int whence) {
        if (debug) Modules.log.debug("sceIoLseek - uid " + Integer.toHexString(uid) + " offset " + offset + " (hex=0x" + Long.toHexString(offset) + ") whence " + getWhenceName(whence));
        seek(uid, offset, whence, true, false);
    }

    public void sceIoLseekAsync(int uid, long offset, int whence) {
        if (debug) Modules.log.debug("sceIoLseekAsync - uid " + Integer.toHexString(uid) + " offset " + offset + " (hex=0x" + Long.toHexString(offset) + ") whence " + getWhenceName(whence));
        seek(uid, offset, whence, true, true);
    }

    public void sceIoLseek32(int uid, int offset, int whence) {
        if (debug) Modules.log.debug("sceIoLseek32 - uid " + Integer.toHexString(uid) + " offset " + offset + " (hex=0x" + Integer.toHexString(offset) + ") whence " + getWhenceName(whence));
        //seek(uid, ((long)offset & 0xFFFFFFFFL), whence, false, false);
        seek(uid, (long)offset, whence, false, false);
    }

    public void sceIoLseek32Async(int uid, int offset, int whence) {
        if (debug) Modules.log.debug("sceIoLseek32Async - uid " + Integer.toHexString(uid) + " offset " + offset + " (hex=0x" + Integer.toHexString(offset) + ") whence " + getWhenceName(whence));
        //seek(uid, ((long)offset & 0xFFFFFFFFL), whence, false, true);
        seek(uid, (long)offset, whence, false, true);
    }

    private String getWhenceName(int whence) {
        switch(whence) {
            case PSP_SEEK_SET: return "PSP_SEEK_SET";
            case PSP_SEEK_CUR: return "PSP_SEEK_CUR";
            case PSP_SEEK_END: return "PSP_SEEK_END";
            default: return "UNHANDLED " + whence;
        }
    }

    // TODO refactor (no "return" midway) now we know better what to do
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

                    // TODO check
                    Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_BAD_FILE_DESCRIPTOR;
                    if (resultIs64bit)
                        Emulator.getProcessor().cpu.gpr[3] = -1;
                } else if (info.asyncPending) {
                    Modules.log.warn("seek - uid " + Integer.toHexString(uid) + " PSP_ERROR_ASYNC_BUSY");

                    // TODO check
                    Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_ASYNC_BUSY;
                    if (resultIs64bit)
                        Emulator.getProcessor().cpu.gpr[3] = -1;
                } else {
                	if (info.sectorBlockMode) {
                		// In sectorBlockMode, the offset is a sector number
                		offset *= UmdIsoFile.sectorLength;
                	}

                	switch(whence) {
                        case PSP_SEEK_SET:
                            if (offset < 0) {
                                Modules.log.warn("SEEK_SET UID " + Integer.toHexString(uid) + " filename:'" + info.filename + "' offset=0x" + Long.toHexString(offset) + " (less than 0!)");
                                Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_INVALID_ARGUMENT;
                                if (resultIs64bit)
                                    Emulator.getProcessor().cpu.gpr[3] = -1;
                                State.fileLogger.logIoSeek64(PSP_ERROR_INVALID_ARGUMENT, uid, offset, whence);
                                return;
                            } else {
                                info.position = offset;

                                if (offset < info.readOnlyFile.length())
                                    info.readOnlyFile.seek(offset);
                            }
                            break;
                        case PSP_SEEK_CUR:
                            if (info.position + offset < 0) {
                                Modules.log.warn("SEEK_CUR UID " + Integer.toHexString(uid) + " filename:'" + info.filename + "' newposition=0x" + Long.toHexString(info.position + offset) + " (less than 0!)");
                                Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_INVALID_ARGUMENT;
                                if (resultIs64bit)
                                    Emulator.getProcessor().cpu.gpr[3] = -1;
                                State.fileLogger.logIoSeek64(PSP_ERROR_INVALID_ARGUMENT, uid, offset, whence);
                                return;
                            } else {
                                info.position += offset;

                                if (info.position < info.readOnlyFile.length())
                                    info.readOnlyFile.seek(info.position);
                            }
                            break;
                        case PSP_SEEK_END:
                            if (info.readOnlyFile.length() + offset < 0) {
                                Modules.log.warn("SEEK_END UID " + Integer.toHexString(uid) + " filename:'" + info.filename + "' newposition=0x" + Long.toHexString(info.position + offset) + " (less than 0!)");
                                Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_INVALID_ARGUMENT;
                                if (resultIs64bit)
                                    Emulator.getProcessor().cpu.gpr[3] = -1;
                                State.fileLogger.logIoSeek64(PSP_ERROR_INVALID_ARGUMENT, uid, offset, whence);
                                return;
                            } else {
                                info.position = info.readOnlyFile.length() + offset;

                                if (info.position < info.readOnlyFile.length())
                                    info.readOnlyFile.seek(info.position);
                            }
                            break;
                        default:
                            Modules.log.error("seek - unhandled whence " + whence);
                            break;
                    }
                    //long result = info.readOnlyFile.getFilePointer();
                    long result = info.position;
                    if (info.sectorBlockMode) {
                    	result /= UmdIsoFile.sectorLength;
                    }

                    if (async) {
                        info.result = result;
                        info.asyncPending = true;

                        // TODO check
                        Emulator.getProcessor().cpu.gpr[2] = 0;
                        if (resultIs64bit)
                            Emulator.getProcessor().cpu.gpr[3] = 0;
                    } else {
                        Emulator.getProcessor().cpu.gpr[2] = (int)(result & 0xFFFFFFFFL);
                        if (resultIs64bit)
                            Emulator.getProcessor().cpu.gpr[3] = (int)(result >> 32);
                    }
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

            Modules.log.info("pspiofilemgr - filepath " + filepath + " (going up one level)");
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
                    Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_DEVICE_NOT_FOUND;
                // check umd is activated
                } else if (!Modules.sceUmdUserModule.isUmdActivated()) {
                    Modules.log.warn("sceIoDopen - umd mounted but not activated");
                    Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_NO_SUCH_DEVICE;
                } else {
                    try {
                        if (iso.isDirectory(isofilename)) {
                            String[] filenames = iso.listDirectory(isofilename);
                            //if (debug) Modules.log.debug("sceIoDopen on umd, " + filenames.length + " files");
                            IoDirInfo info = new IoDirInfo(pcfilename, filenames);
                            Emulator.getProcessor().cpu.gpr[2] = info.uid;
                        } else {
                            if (debug) Modules.log.warn("sceIoDopen '" + isofilename + "' not a umd directory!");
                            Emulator.getProcessor().cpu.gpr[2] = -1;
                        }
                    } catch(FileNotFoundException e) {
                        Modules.log.warn("sceIoDopen - '" + isofilename + "' umd file not found");
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
                // Regular apps run from inside mstick dir or absolute path given
                if (debug) Modules.log.debug("sceIoDopen - pcfilename = " + pcfilename);
                File f = new File(pcfilename);
                if (f.isDirectory()) {
                    IoDirInfo info = new IoDirInfo(pcfilename, f.list());
                    Emulator.getProcessor().cpu.gpr[2] = info.uid;
                } else {
                    if (debug) Modules.log.warn("sceIoDopen '" + pcfilename + "' not a directory! (could be missing)");
                    Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_FILE_NOT_FOUND;
                }
            }
        } else {
            // I don't think we can get here anymore? (fiveofhearts)
            Modules.log.error("sceIoDopen something went wrong in getDeviceFilePath '" + dirname + "'");
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

            SceIoStat stat = stat(info.path + "/" + filename);
            if (stat != null) {
                SceIoDirent dirent = new SceIoDirent(stat, filename);
                dirent.write(Memory.getInstance(), dirent_addr);

                if ((stat.attr & 0x10) == 0x10) {
                    Modules.log.debug("sceIoDread uid=" + Integer.toHexString(uid)
                        + " #" + info.printableposition
                        + " dir='" + info.path
                        + "', dir='" + filename + "'");
                } else {
                    Modules.log.debug("sceIoDread uid=" + Integer.toHexString(uid)
                        + " #" + info.printableposition
                        + " dir='" + info.path
                        + "', file='" + filename + "'");
                }

                Emulator.getProcessor().cpu.gpr[2] = 1;
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
                for (int i = 0; i < inlen; i += 4) {
                    Modules.log.debug("sceIoDevctl indata[" + (i / 4) + "]=0x" + Integer.toHexString(mem.read32(indata_addr + i)));
                }
            }

            if (mem.isAddressGood(outdata_addr)) {
                for (int i = 0; i < outlen; i += 4) {
                    Modules.log.debug("sceIoDevctl outdata[" + (i / 4) + "]=0x" + Integer.toHexString(mem.read32(outdata_addr + i)));
                }
            }
        }

        switch(cmd) {
            case 0x01F20001:
            {
                // TODO yield? (on psp blocks until disc spins up)
                Modules.log.warn("sceIoDevctl " + String.format("0x%08X", cmd) + " unknown umd command (check disc type?)");
                Memory mem = Memory.getInstance();
                if (mem.isAddressGood(outdata_addr) && outlen >= 8) {
                    // 2nd field
                    // 0 = not inserted
                    // 0x10 = inserted
                    int result;

                    if (iso == null)
                        result = 0;
                    else
                        result = 0x10;

                    mem.write32(outdata_addr + 4, result);
                    Emulator.getProcessor().cpu.gpr[2] = 0;
                } else {
                    Emulator.getProcessor().cpu.gpr[2] = -1;
                }
                break;
            }

            case 0x02015804: // register memorystick insert/eject callback (mscmhc0)
            {
                Modules.log.debug("sceIoDevctl register memorystick insert/eject callback (mscmhc0)");
                Memory mem = Memory.getInstance();
                ThreadMan threadMan = ThreadMan.getInstance();

                if (!device.equals("mscmhc0:")) {
                    Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_UNSUPPORTED_OPERATION;
                } else if (mem.isAddressGood(indata_addr) && inlen == 4) {
                    int cbid = mem.read32(indata_addr);
                    if (threadMan.setCallback(SceKernelThreadInfo.THREAD_CALLBACK_MEMORYSTICK, cbid)) {
                        // Trigger callback immediately
                        threadMan.pushCallback(SceKernelThreadInfo.THREAD_CALLBACK_MEMORYSTICK, MemoryStick.getState());
                        Emulator.getProcessor().cpu.gpr[2] = 0; // Success
                    } else {

                        Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_DEVCTL_BAD_PARAMS; // No such callback
                    }
                } else {
                    Emulator.getProcessor().cpu.gpr[2] = -1; // Invalid parameters
                }
                break;
            }

            case 0x02015805: // unregister memorystick insert/eject callback (mscmhc0)
            {
                Modules.log.debug("sceIoDevctl unregister memorystick insert/eject callback (mscmhc0)");
                Memory mem = Memory.getInstance();
                ThreadMan threadMan = ThreadMan.getInstance();

                if (!device.equals("mscmhc0:")) {
                    Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_UNSUPPORTED_OPERATION;
                } else if (mem.isAddressGood(indata_addr) && inlen == 4) {
                    int cbid = mem.read32(indata_addr);
                    if (threadMan.clearCallback(SceKernelThreadInfo.THREAD_CALLBACK_MEMORYSTICK, cbid) != null) {
                        Emulator.getProcessor().cpu.gpr[2] = 0; // Success
                    } else {
                        Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_DEVCTL_BAD_PARAMS; // No such callback
                    }
                } else {
                    Emulator.getProcessor().cpu.gpr[2] = -1; // Invalid parameters
                }
                break;
            }

            case 0x02025801:
            {
                Modules.log.warn("sceIoDevctl " + String.format("0x%08X", cmd) + " unknown ms command (check fs type?)");
                Memory mem = Memory.getInstance();

                if (!device.equals("mscmhc0:")) {
                    Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_UNSUPPORTED_OPERATION;
                } else if (mem.isAddressGood(outdata_addr)) {
                    // 1 = not inserted
                    // 4 = inserted
                    mem.write32(outdata_addr, 4);
                    Emulator.getProcessor().cpu.gpr[2] = 0;
                } else {
                    Emulator.getProcessor().cpu.gpr[2] = -1;
                }
                break;
            }

            case 0x02025806:
            {
                Modules.log.debug("sceIoDevctl check ms inserted (mscmhc0)");
                Memory mem = Memory.getInstance();

                if (!device.equals("mscmhc0:")) {
                    Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_UNSUPPORTED_OPERATION;
                } else if (mem.isAddressGood(outdata_addr)) {
                    // 1 = inserted
                    // 2 = not inserted
                    mem.write32(outdata_addr, MemoryStick.getState());
                    Emulator.getProcessor().cpu.gpr[2] = 0;
                } else {
                    Emulator.getProcessor().cpu.gpr[2] = -1;
                }
                break;
            }

            case 0x02415821: // register memorystick insert/eject callback (fatms0)
            {
                Modules.log.debug("sceIoDevctl register memorystick insert/eject callback (fatms0)");
                Memory mem = Memory.getInstance();
                ThreadMan threadMan = ThreadMan.getInstance();

                if (!device.equals("fatms0:")) {
                    Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_DEVCTL_BAD_PARAMS;
                } else if (mem.isAddressGood(indata_addr) && inlen == 4) {
                    int cbid = mem.read32(indata_addr);
                    threadMan.setCallback(SceKernelThreadInfo.THREAD_CALLBACK_MEMORYSTICK, cbid);
                    // Trigger callback immediately
                    threadMan.pushCallback(SceKernelThreadInfo.THREAD_CALLBACK_MEMORYSTICK, MemoryStick.getState());
                    Emulator.getProcessor().cpu.gpr[2] = 0;  // Success
                } else {
                    Emulator.getProcessor().cpu.gpr[2] = -1; // Invalid parameters
                }
                break;
            }

            case 0x02415822: // unregister memorystick insert/eject callback (fatms0)
            {
                Modules.log.debug("sceIoDevctl unregister memorystick insert/eject callback (fatms0)");
                Memory mem = Memory.getInstance();
                ThreadMan threadMan = ThreadMan.getInstance();

                if (!device.equals("fatms0:")) {
                    Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_DEVCTL_BAD_PARAMS;
                } else if (mem.isAddressGood(indata_addr) && inlen == 4) {
                    int cbid = mem.read32(indata_addr);
                    threadMan.clearCallback(SceKernelThreadInfo.THREAD_CALLBACK_MEMORYSTICK, cbid);
                    Emulator.getProcessor().cpu.gpr[2] = 0;  // Success
                } else {
                    Emulator.getProcessor().cpu.gpr[2] = -1; // Invalid parameters
                }
                break;
            }

            // this one may be a typo by the jpcsp team :P can anyone find a game that uses it?
            case 0x02415823:
                Modules.log.warn("IGNORED: sceIoDevctl " + String.format("0x%08X", cmd) + " unhandled ms command");
                Emulator.getProcessor().cpu.gpr[2] = 0; // Fake success
                break;

            case 0x02425818: // Free space on ms
            // use PSP_ERROR_NO_SUCH_DEVICE if ms is not in
            {
                // empty formatted mem stick
                int sectorSize = 0x200;
                int sectorCount = 0x08;
                // Perform operation using long integers to avoid overflow
                int maxClusters = (int) ((MemoryStick.getFreeSize() * 95L / 100) / (sectorSize * sectorCount)); // reserve 5% for fs house keeping
                int freeClusters = maxClusters;
                int maxSectors = 512; // TODO

                Memory mem = Memory.getInstance();
                if (mem.isAddressGood(indata_addr) && inlen >= 4) {
                    int addr = mem.read32(indata_addr);
                    if (mem.isAddressGood(addr)) {
                        Modules.log.debug("sceIoDevctl refer ms free space");
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
            {
                Modules.log.debug("sceIoDevctl check ms inserted (fatms0)");
                Memory mem = Memory.getInstance();
                if (!device.equals("fatms0:")) {
                    Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_DEVCTL_BAD_PARAMS;
                } else if (mem.isAddressGood(outdata_addr)) {
                    // 0 = not inserted
                    // 1 = inserted
                    mem.write32(outdata_addr, 1);
                    Emulator.getProcessor().cpu.gpr[2] = 0;
                } else {
                    Emulator.getProcessor().cpu.gpr[2] = -1;
                }
                break;
            }

            default:
                Modules.log.warn("sceIoDevctl " + String.format("0x%08X", cmd) + " unknown command");
                Emulator.getProcessor().cpu.gpr[2] = -1; // Just fail for now
                break;
        }

        State.fileLogger.logIoDevctl(Emulator.getProcessor().cpu.gpr[2],
                device_addr, device, cmd, indata_addr, inlen, outdata_addr, outlen);
    }

    public void hleIoIoctl(int uid, int cmd, int indata_addr, int inlen, int outdata_addr, int outlen, boolean async) {
        IoInfo info = null;
        int result = -1;
        Memory mem = Memory.getInstance();

        if (debug) {
            Modules.log.debug("hleIoIoctl(uid=" + Integer.toHexString(uid)
                + ",cmd=0x" + Integer.toHexString(cmd)
                + ",indata=0x" + Integer.toHexString(indata_addr)
                + ",inlen=" + inlen
                + ",outdata=0x" + Integer.toHexString(outdata_addr)
                + ",outlen=" + outlen
                + ",async=" + async
                + ")");

            if (mem.isAddressGood(indata_addr)) {
                for (int i = 0; i < inlen; i += 4) {
                    Modules.log.debug("hleIoIoctl indata[" + (i / 4) + "]=0x" + Integer.toHexString(mem.read32(indata_addr + i)));
                }
            }

            if (mem.isAddressGood(outdata_addr)) {
                for (int i = 0; i < outlen; i += 4) {
                    Modules.log.debug("hleIoIoctl outdata[" + (i / 4) + "]=0x" + Integer.toHexString(mem.read32(outdata_addr + i)));
                }
            }
        }

        SceUidManager.checkUidPurpose(uid, "IOFileManager-File", true);
        info = filelist.get(uid);
        if (info == null) {
            Modules.log.warn("hleIoIoctl - unknown uid " + Integer.toHexString(uid));
            result = PSP_ERROR_BAD_FILE_DESCRIPTOR;
        } else if (info.asyncPending) {
            // Can't execute another operation until the previous one completed
            Modules.log.warn("hleIoIoctl - uid " + Integer.toHexString(uid) + " PSP_ERROR_ASYNC_BUSY");
            result = PSP_ERROR_ASYNC_BUSY;
        } else {
            switch (cmd) {
                // UMD file seek set
                case 0x01010005:
                {
                    if (mem.isAddressGood(indata_addr) && inlen >= 4) {
                        if (info.isUmdFile()) {
                            try {
                                int offset = mem.read32(indata_addr);
                                Modules.log.debug("hleIoIoctl umd file seek set " + offset);
                                info.readOnlyFile.seek(offset);
                                info.position = offset;
                                result = 0;
                            } catch (IOException e) {
                                // Should never happen?
                                Modules.log.warn("hleIoIoctl cmd=0x01010005 exception: " + e.getMessage());
                                result = -1;
                            }
                        } else {
                            Modules.log.warn("hleIoIoctl cmd=0x01010005 only allowed on UMD files");
                        }
                    } else {
                        Modules.log.warn("hleIoIoctl cmd=0x01010005 " + String.format("0x%08X %d", indata_addr, inlen) + " unsupported parameters");
                        result = PSP_ERROR_INVALID_ARGUMENT;
                    }
                    break;
                }

                //Get UMD file pointer
                case 0x01020004:
                {
                    if (mem.isAddressGood(outdata_addr) && outlen >= 4) {
                        if (info.isUmdFile()) {
                            try {
                                int fPointer = (int)info.readOnlyFile.getFilePointer();
                                mem.write32(outdata_addr, fPointer);
                                Modules.log.debug("hleIoIoctl umd file get file pointer " + fPointer);
                                result = 0;
                            } catch (IOException e) {
                                Modules.log.warn("hleIoIoctl cmd=0x01020004 exception: " + e.getMessage());
                            }
                        } else {
                            Modules.log.warn("hleIoIoctl cmd=0x01020004 only allowed on UMD files");
                        }
                    } else {
                        Modules.log.warn("hleIoIoctl cmd=0x01020004 " + String.format("0x%08X %d", outdata_addr, outlen) + " unsupported parameters");
                        result = PSP_ERROR_INVALID_ARGUMENT;
                    }
                    break;
                }

                // Get UMD file start sector
                case 0x01020006:
                {
                    if (mem.isAddressGood(outdata_addr) && outlen >= 4) {
                        int startSector = 0;
                        if (info.isUmdFile() && info.readOnlyFile instanceof UmdIsoFile) {
                            UmdIsoFile file = (UmdIsoFile) info.readOnlyFile;
                            startSector = file.getStartSector();
                            Modules.log.debug("hleIoIoctl umd file get start sector " + startSector);
                            mem.write32(outdata_addr, startSector);
                            result = 0;
                        } else {
                            Modules.log.warn("hleIoIoctl cmd=0x01020006 only allowed on UMD files and only implemented for UmdIsoFile");
                        }
                    } else {
                        Modules.log.warn("hleIoIoctl cmd=0x01020006 " + String.format("0x%08X %d", outdata_addr, outlen) + " unsupported parameters");
                        result = PSP_ERROR_INVALID_ARGUMENT;
                    }
                    break;
                }

                // Get UMD file length in bytes
                case 0x01020007:
                {
                    if (mem.isAddressGood(outdata_addr) && outlen >= 8) {
                        if (info.isUmdFile()) {
                            try {
                                long length = info.readOnlyFile.length();
                                mem.write64(outdata_addr, length);
                                Modules.log.debug("hleIoIoctl get file size " + length);
                                result = 0;
                            } catch (IOException e) {
                                // Should never happen?
                                Modules.log.warn("hleIoIoctl cmd=0x01020007 exception: " + e.getMessage());
                            }
                        } else {
                            Modules.log.warn("hleIoIoctl cmd=0x01020007 only allowed on UMD files");
                        }
                    } else {
                        Modules.log.warn("hleIoIoctl cmd=0x01020007 " + String.format("0x%08X %d", outdata_addr, outlen) + " unsupported parameters");
                        result = PSP_ERROR_INVALID_ARGUMENT;
                    }
                    break;
                }

                // UMD file seek whence
                case 0x01F100A6:
                {
                    if (mem.isAddressGood(indata_addr) && inlen >= 16) {
                        if (info.isUmdFile()) {
                            try {
                                long offset = mem.read64(indata_addr);
                                int whence = mem.read32(indata_addr + 12);
                                if (Modules.log.isDebugEnabled()) {
                                	Modules.log.debug("hleIoIoctl umd file seek offset " + offset + ", whence " + whence);
                                }
                            	switch (whence) {
                            		case PSP_SEEK_SET: {
                                        info.position = offset;
                            			info.readOnlyFile.seek(info.position);
                                        result = 0;
                            			break;
                            		}
                            		case PSP_SEEK_CUR: {
                                        info.position = info.position + offset;
                            			info.readOnlyFile.seek(info.position);
                                        result = 0;
                            			break;
                            		}
                            		case PSP_SEEK_END: {
                                        info.position = info.readOnlyFile.length() + offset;
                            			info.readOnlyFile.seek(info.position);
                                        result = 0;
                            			break;
                            		}
                            		default: {
                            			Modules.log.error("hleIoIoctl - unhandled whence " + whence);
                                        result = -1;
                            			break;
                            		}
                            	}
                            } catch (IOException e) {
                                // Should never happen?
                                Modules.log.warn("hleIoIoctl cmd=0x01F100A6 exception: " + e.getMessage());
                                result = -1;
                            }
                        } else {
                            Modules.log.warn("hleIoIoctl cmd=0x01F100A6 only allowed on UMD files");
                        }
                    } else {
                        Modules.log.warn("hleIoIoctl cmd=0x01F100A6 " + String.format("0x%08X %d", indata_addr, inlen) + " unsupported parameters");
                        result = PSP_ERROR_INVALID_ARGUMENT;
                    }
                    break;
                }

                // Get AES-128 bit key.
                case 0x04100001:
                {
                    if (mem.isAddressGood(indata_addr) && inlen == 16) {
                        for(int i = 0; i < inlen; i++)
                            AES128Key[i] = (byte)mem.read8(indata_addr+i);

                        Modules.log.debug("hleIoIoctl get AES key");
                        result = 0;
                    } else {
                        Modules.log.warn("hleIoIoctl cmd=0x04100001 " + String.format("0x%08X %d", indata_addr, inlen) + " unsupported parameters");
                        result = PSP_ERROR_INVALID_ARGUMENT;
                    }
                    break;
                }

                default:
                {
                    Modules.log.warn("hleIoIoctl " + String.format("0x%08X", cmd) + " unknown command");
                    break;
                }
            }
        }

        Emulator.getProcessor().cpu.gpr[2] = updateResult(info, result, async);

        State.fileLogger.logIoIoctl(Emulator.getProcessor().cpu.gpr[2], uid, cmd, indata_addr, inlen, outdata_addr, outlen);
    }

    public void sceIoIoctl(int uid, int cmd, int indata_addr, int inlen, int outdata_addr, int outlen) {
    	hleIoIoctl(uid, cmd, indata_addr, inlen, outdata_addr, outlen, false);
    }

    public void sceIoIoctlAsync(int uid, int cmd, int indata_addr, int inlen, int outdata_addr, int outlen) {
    	hleIoIoctl(uid, cmd, indata_addr, inlen, outdata_addr, outlen, true);
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
                    Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_DEVICE_NOT_FOUND;

                // check umd is activated
                } else if (!Modules.sceUmdUserModule.isUmdActivated()) {
                    Modules.log.warn("stat - umd mounted but not activated");
                    Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_NO_SUCH_DEVICE;

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
                        Modules.log.warn("stat - '" + isofilename + "' umd file not found");
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
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_FILE_NOT_FOUND;
        }

        // TODO move into stat()? that will also log on Dread
        State.fileLogger.logIoGetStat(Emulator.getProcessor().cpu.gpr[2],
                file_addr, filename, stat_addr);
    }

    /**
     * Remove directory entry
     *
     * @param file - Path to the file to remove
     * @return < 0 on error
     */
    public void sceIoRemove(int file_addr) {
        String filename = readStringZ(file_addr);
        if (debug) Modules.log.debug("sceIoRemove - file = " + filename);

        String pcfilename = getDeviceFilePath(filename);

        if (pcfilename != null) {
            if (isUmdPath(pcfilename)) {
                Emulator.getProcessor().cpu.gpr[2] = -1;	// TODO Check error code
            } else {
                File file = new File(pcfilename);
                if (file.delete()) {
                    Emulator.getProcessor().cpu.gpr[2] = 0;
                } else {
                    Emulator.getProcessor().cpu.gpr[2] = -1;	// TODO Check error code
                }
            }
        } else {
            Emulator.getProcessor().cpu.gpr[2] = -1;	// TODO Check error code
        }

        State.fileLogger.logIoRemove(Emulator.getProcessor().cpu.gpr[2], file_addr, filename);
    }

    /**
     * Change the status of a file.
     *
     * @param file - The path to the file.
     * @param stat - A pointer to an io_stat_t structure.
     * @param bits - Bitmask defining which bits to change.
     *
     * @return < 0 on error.
     */
    public void sceIoChstat(int file_addr, int stat_addr, int bits) {
        String filename = readStringZ(file_addr);
        if (debug) Modules.log.debug("sceIoChstat - file = " + filename + ", bits=0x" + Integer.toHexString(bits));

        String pcfilename = getDeviceFilePath(filename);

        if (pcfilename != null) {
            if (isUmdPath(pcfilename)) {
                Emulator.getProcessor().cpu.gpr[2] = -1;	// TODO Check error code
            } else {
                File file = new File(pcfilename);
                
                SceIoStat stat = new SceIoStat();
                stat.read(Memory.getInstance(), stat_addr);

                int mode = stat.mode;
                boolean successful = true;

                if ((bits & 0x0001) != 0) {	// Others execute permission 
                	if (!file.setExecutable((mode & 0x0001) != 0)) {
                		successful = false;
                	}
                }
                if ((bits & 0x0002) != 0) {	// Others write permission
                	if (!file.setWritable((mode & 0x0002) != 0)) {
                		successful = false;
                	}
                }
                if ((bits & 0x0004) != 0) {	// Others read permission
                	if (!file.setReadable((mode & 0x0004) != 0)) {
                		successful = false;
                	}
                }

                if ((bits & 0x0040) != 0) {	// User execute permission 
                	if (!file.setExecutable((mode & 0x0040) != 0, true)) {
                		successful = false;
                	}
                }
                if ((bits & 0x0080) != 0) {	// User write permission
                	if (!file.setWritable((mode & 0x0080) != 0, true)) {
                		successful = false;
                	}
                }
                if ((bits & 0x0100) != 0) {	// User read permission
                	if (!file.setReadable((mode & 0x0100) != 0, true)) {
                		successful = false;
                	}
                }

                if (successful) {
                	Emulator.getProcessor().cpu.gpr[2] = 0;
                } else {
                	Emulator.getProcessor().cpu.gpr[2] = -1;	// TODO Check error code
                }
            }
        } else {
            Emulator.getProcessor().cpu.gpr[2] = -1;	// TODO Check error code
        }

        State.fileLogger.logIoChstat(Emulator.getProcessor().cpu.gpr[2], file_addr, filename, stat_addr, bits);
    }

    //the following sets the filepath from memstick manager.
    public void setfilepath(String filepath)
    {
        // This could mess up... I don't think it's really needed anyway, just makes logging slightly nicer
        filepath = filepath.replaceAll("\\\\", "/");

        Modules.log.info("pspiofilemgr - filepath " + filepath);
        this.filepath = filepath;

        // test getDeviceFilePath/umd path handling
        if (false) {
            System.err.println("filepath " + filepath);

            // good: disc0 or disc0/
            System.err.println(getDeviceFilePath(""));
            System.err.println(getDeviceFilePath("disc0:"));
            System.err.println(getDeviceFilePath("disc0:/"));

            System.err.println(getDeviceFilePath("somewhere/trailing/"));
            System.err.println(getDeviceFilePath("somewhere/somewhere"));
            System.err.println(getDeviceFilePath("/somewhere/trailing/"));
            System.err.println(getDeviceFilePath("/somewhere/somewhere"));

            // good: disc0/somewhere
            System.err.println(getDeviceFilePath("somewhere"));
            System.err.println(getDeviceFilePath("disc0:somewhere"));
            System.err.println(getDeviceFilePath("disc0:/somewhere"));

            // good: disc0/trailing
            // bad:  disc0/trailing/
            System.err.println(getDeviceFilePath("trailing/"));
            System.err.println(getDeviceFilePath("disc0:trailing/"));
            System.err.println(getDeviceFilePath("disc0:/trailing/"));

            System.err.println(isUmdPath("disc0"));
            System.err.println(isUmdPath("disc0:"));
            System.err.println(isUmdPath("disc0:/"));
            System.err.println(isUmdPath("umd0:"));
            System.err.println(isUmdPath("umd1:"));
            System.err.println(isUmdPath("umd000:")); // this should pass as umd

            // these 3 should fail (but not checked on real psp)
            System.err.println(isUmdPath("/disc0:"));
            System.err.println(isUmdPath("somewheredisc0:"));
            System.err.println(isUmdPath("somewhere/disc0:"));

            // Gripshift
            System.err.println("Gripshift: " + trimUmdPrefix("disc0")); // should come out blank

            // FFCC
            System.err.println("FFCC: " + getDeviceFilePath("umd1:"));
            System.err.println("FFCC: " + trimUmdPrefix(getDeviceFilePath("umd1:"))); // should come out blank

            {
                String realfilepath = this.filepath;

                this.filepath = "disc0/somewhere";
                System.err.println(getDeviceFilePath(""));
                System.err.println(getDeviceFilePath("file"));
                System.err.println(getDeviceFilePath("/file"));

                // good: disc0/trailing/file
                // bad:  disc0/trailing//file
                this.filepath = "disc0/trailing/";
                System.err.println(getDeviceFilePath(""));
                System.err.println(getDeviceFilePath("file"));
                System.err.println(getDeviceFilePath("/file"));

                this.filepath = realfilepath;
            }
        }
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

    public UmdIsoReader getIsoReader() {
    	return iso;
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
        public long position; // virtual position, beyond the end is allowed, before the start is an error
        public boolean sectorBlockMode;
        public boolean isEncrypted; // Used to check for new encryption mechanism (PSP_O_UNKNOWN3).

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
            this.sectorBlockMode = false;
            uid = SceUidManager.getNewUid("IOFileManager-File");
            filelist.put(uid, this);
            isEncrypted = false;
        }

        /** UMD version (read only) */
        public IoInfo(String filename, SeekableDataInput f, String mode, int flags, int permissions) {
            this.filename = filename;
            this.msFile = null;
            this.readOnlyFile = f;
            this.mode = mode;
            this.flags = flags;
            this.permissions = permissions;
            this.sectorBlockMode = false;
            uid = SceUidManager.getNewUid("IOFileManager-File");
            filelist.put(uid, this);
            isEncrypted = false;
        }

        public boolean isUmdFile() {
            return (msFile == null);
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
