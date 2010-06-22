/*
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
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.connector.PGDFileConnector;
import jpcsp.filesystems.*;
import jpcsp.filesystems.umdiso.*;
import jpcsp.hardware.MemoryStick;
import jpcsp.util.Utilities;
import static jpcsp.util.Utilities.*;
import static jpcsp.HLE.kernel.types.SceKernelErrors.*;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.kernel.types.*;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.*;
import jpcsp.HLE.kernel.managers.*;
import jpcsp.HLE.modules.ThreadManForUser;
import jpcsp.HLE.modules.sceMpeg;
import jpcsp.State;

/*
 * TODO list:
 * 1. Use file id's starting at around 0:
 *  - One game checks if it was between 0 and 31 inclusive.
 *  - psplink allows 3 - 63 inclusive.
 *  - Without psplink its 3 - 12 inclusive.
 *
 * 2. Get std out/err/in from stdio module.
 *
 * 3. Investigate PSP_O_NBUF in PSP.
 *
 * 4. Check if the PSP's filesystem supports permissions. If true,
 * implement PSP_O_CREAT based on Java File and it's setReadable/Writable/Executable.
 *
 * 5. Check PSP_O_TRUNC flag: delete the file and recreate it?
 *
 * 6. Log which filename was stored in sceIoDread.
 *
 * 7. Check if cmd 0x01F20001 in sceIoDevctl needs thread blocking.
 */

public class pspiofilemgr {
    private static pspiofilemgr  instance;
    private static Logger stdout = Logger.getLogger("stdout");
    private static Logger stderr = Logger.getLogger("stderr");

    public final static int PSP_O_RDONLY   = 0x0001;
    public final static int PSP_O_WRONLY   = 0x0002;
    public final static int PSP_O_RDWR     = (PSP_O_RDONLY | PSP_O_WRONLY);
    public final static int PSP_O_NBLOCK   = 0x0004;
    public final static int PSP_O_DIROPEN  = 0x0008;
    public final static int PSP_O_APPEND   = 0x0100;
    public final static int PSP_O_CREAT    = 0x0200;
    public final static int PSP_O_TRUNC    = 0x0400;
    public final static int PSP_O_EXCL     = 0x0800;
    public final static int PSP_O_NBUF     = 0x4000; // Special mode only valid for media files.
    public final static int PSP_O_NOWAIT   = 0x8000;
    public final static int PSP_O_UNKNOWN1 = 0x2000000; // seen on Puzzle Guzzle, Hammerin' Hero
    public final static int PSP_O_UNKNOWN2 = 0x40000000; // From "Kingdom Hearts: Birth by Sleep".

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

    // modeStrings indexed by [0, PSP_O_RDONLY, PSP_O_WRONLY, PSP_O_RDWR]
    // SeekableRandomFile doesn't support write only: take "rw",
    private final static String[] modeStrings = { "r", "r", "rw", "rw" };

    private HashMap<Integer, IoInfo> filelist;
    private HashMap<Integer, IoDirInfo> dirlist;

    private String filepath; // current working directory on PC
    private UmdIsoReader iso;

    private int defaultAsyncPriority;
    private final static int asyncThreadRegisterArgument = 16; // $s0 is preserved across calls

    private byte[] AES128Key = new byte[16];
    private PGDFileConnector pgdFileConnector;

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
        defaultAsyncPriority = -1;
    }

    /*
     *  Local file handling functions.
     */
    private String getDeviceFilePath(String pspfilename) {
        pspfilename = pspfilename.replaceAll("\\\\", "/");
        String device = null;
        String cwd = "";
        String filename = null;

        int findcolon = pspfilename.indexOf(":");
        if (findcolon != -1) {
            device = pspfilename.substring(0, findcolon);
            pspfilename = pspfilename.substring(findcolon + 1);
        } else {
            int findslash = filepath.indexOf("/");
            if (findslash != -1) {
                device = filepath.substring(0, findslash);
                cwd = filepath.substring(findslash + 1);

                if (cwd.startsWith("/")) {
                    cwd = cwd.substring(1);
                }
                if (cwd.endsWith("/")) {
                    cwd = cwd.substring(0, cwd.length() - 1);
                }
            } else {
                device = filepath;
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

        return filename;
    }

    private final String[] umdPrefixes = new String[] {
        "disc", "umd"
    };

    private boolean isUmdPath(String deviceFilePath) {
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
        return pcfilename;
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

    /**
     * @param pcfilename can be null for convenience
     * @returns null on error
     */
    private SceIoStat stat(String pcfilename) {
        SceIoStat stat = null;
        if (pcfilename != null) {
            if (isUmdPath(pcfilename)) {
                // check umd is mounted
                if (iso == null) {
                    Modules.log.error("stat - no umd mounted");
                    Emulator.getProcessor().cpu.gpr[2] = ERROR_DEVICE_NOT_FOUND;

                // check umd is activated
                } else if (!Modules.sceUmdUserModule.isUmdActivated()) {
                    Modules.log.warn("stat - umd mounted but not activated");
                    Emulator.getProcessor().cpu.gpr[2] = ERROR_NO_SUCH_DEVICE;

                } else {
                    String isofilename = trimUmdPrefix(pcfilename);
                    try {
                        int mode = 4; // 4 = readable
                        int attr = 0;
                        long size = 0;
                        long timestamp = 0;
                        int startSector = 0;

                        // Set attr (dir/file)
                        if (iso.isDirectory(isofilename)) {
                            attr |= 0x10;
                            mode |= 1; // 1 = executable
                        } else { // isFile
                            attr |= 0x20;
                            UmdIsoFile file = iso.getFile(isofilename);
                            size = file.length();
                            timestamp = file.getTimestamp().getTime();
                            startSector = file.getStartSector();
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
                    Modules.log.error("getFile - refusing to open umd media for write");
                    return resultFile;
                } else {
                    // open file
                    try {
                        UmdIsoFile file = iso.getFile(trimUmdPrefix(pcfilename));
                        resultFile = file;
                    } catch(FileNotFoundException e) {
                        if (Modules.log.isDebugEnabled()) Modules.log.debug("getFile - umd file not found '" + pcfilename + "' (ok to ignore this message, debug purpose only)");
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
                    if (Modules.log.isDebugEnabled()) Modules.log.debug("getFile - file already exists (PSP_O_CREAT + PSP_O_EXCL)");
                } else {
                    if (file.exists() &&
                        (flags & PSP_O_TRUNC) == PSP_O_TRUNC) {
                        Modules.log.warn("getFile - file already exists, deleting UNIMPLEMENT (PSP_O_TRUNC)");
                    }
                    String mode = getMode(flags);

                    try {
                        SeekableRandomFile raf = new SeekableRandomFile(pcfilename, mode);
                        resultFile = raf;
                    } catch (FileNotFoundException e) {
                        if (Modules.log.isDebugEnabled()) Modules.log.debug("getFile - file not found '" + pcfilename + "' (ok to ignore this message, debug purpose only)");
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

    // Handle returning/storing result for sync/async operations
    private void updateResult(CpuState cpu, IoInfo info, long result, boolean async, boolean resultIs64bit) {
        if (info != null) {
        	if (async) {
	            if (!info.asyncPending) {
                    startIoAsync(info, result);
	                result = 0;
	            }
	        } else {
	            info.result = ERROR_NO_ASYNC_OP;
	        }
        }

        cpu.gpr[2] = (int)(result & 0xFFFFFFFFL);
        if (resultIs64bit) {
            cpu.gpr[3] = (int) (result >> 32);
        }
    }

    private String getWhenceName(int whence) {
        switch(whence) {
            case PSP_SEEK_SET: return "PSP_SEEK_SET";
            case PSP_SEEK_CUR: return "PSP_SEEK_CUR";
            case PSP_SEEK_END: return "PSP_SEEK_END";
            default: return "UNHANDLED " + whence;
        }
    }

    public void setfilepath(String filepath) {
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

    public void setIsoReader(UmdIsoReader iso) {
        this.iso = iso;
    }

    public UmdIsoReader getIsoReader() {
    	return iso;
    }

    /*
     * Async thread functions.
     */
    public void hleAsyncThread() {
        CpuState cpu = Emulator.getProcessor().cpu;
    	ThreadManForUser threadMan = Modules.ThreadManForUserModule;

        int uid = cpu.gpr[asyncThreadRegisterArgument];
        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug("hleAsyncThread uid=" + Integer.toHexString(uid));
        }

        IoInfo info = filelist.get(uid);
        if (info == null) {
        	cpu.gpr[2] = 0; // Exit status
        	threadMan.hleKernelExitThread();
        } else {
        	doStepAsync(info);
        	if (threadMan.getCurrentThread() == info.asyncThread) {
        		// Wait for a new Async IO... wakeup is done by triggerAsyncThread()
        		threadMan.hleKernelSleepThread(false);
        	}
        }
    }

    /**
     * Trigger the activation of the async thread if one is defined.
     *
     * @param info the file info
     */
    private void triggerAsyncThread(IoInfo info) {
    	if (info.asyncThread != null) {
        	ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        	threadMan.hleKernelWakeupThread(info.asyncThread);
    	}
    }

    /**
     * Start the async IO thread if not yet started.
     *
     * @param info   the file
     * @param result the result the async IO should return
     */
    private void startIoAsync(IoInfo info, long result) {
    	if (info == null) {
    		return;
    	}

        info.asyncPending = true;
        info.result = result;

        if (info.asyncThread == null) {
        	ThreadManForUser threadMan = Modules.ThreadManForUserModule;

	        // Inherit priority from current thread if no default priority set
	        int asyncPriority = defaultAsyncPriority;
	        if (asyncPriority < 0) {
	        	asyncPriority = threadMan.getCurrentThread().currentPriority;
	        }

	        info.asyncThread = threadMan.hleKernelCreateThread("SceIofileAsync",
	                ThreadManForUser.ASYNC_LOOP_ADDRESS, asyncPriority, 0x2000,
	                threadMan.getCurrentThread().attr, 0);

	        // Copy uid to Async Thread argument register
	        info.asyncThread.cpuContext.gpr[asyncThreadRegisterArgument] = info.uid;

	        // This must be the last action of the hleIoXXX call because it can context-switch
	        // Inherit $gp from this process ($gp can be used by interrupts)
	        threadMan.hleKernelStartThread(info.asyncThread, 0, 0, info.asyncThread.gpReg_addr);
        } else {
        	triggerAsyncThread(info);
        }
    }

    private void doStepAsync(IoInfo info) {
    	ThreadManForUser threadMan = Modules.ThreadManForUserModule;

    	if (info == null || !info.asyncPending) {
    		return;
    	}

    	info.asyncPending = false;

        if (info.cbid >= 0) {
        	// Trigger Async callback
        	threadMan.hleKernelNotifyCallback(SceKernelThreadInfo.THREAD_CALLBACK_IO, info.cbid, info.notifyArg);
        }

        // Find threads waiting on this uid and wake them up
        // TODO If the call was sceIoWaitAsyncCB we might need to make sure
        // the callback is fully processed before waking the thread!
        for (Iterator<SceKernelThreadInfo> it = threadMan.iterator(); it.hasNext(); ) {
            SceKernelThreadInfo thread = it.next();

            if (thread.wait.waitingOnIo && thread.wait.Io_id == info.uid) {
            	if (Modules.log.isDebugEnabled()) {
            		Modules.log.debug("pspiofilemgr - onContextSwitch waking " + Integer.toHexString(thread.uid) + " thread:'" + thread.name + "'");
            	}
                // Untrack
                thread.wait.waitingOnIo = false;
                // Return success
                thread.cpuContext.gpr[2] = 0;
                // Wakeup
                threadMan.hleChangeThreadState(thread, PSP_THREAD_READY);
            }
        }
    }

    /*
     * HLE functions.
     */
    public void hleIoGetAsyncStat(int uid, int res_addr, boolean wait, boolean callbacks) {
        if (Modules.log.isDebugEnabled()) Modules.log.debug("hleIoGetAsyncStat(uid=" + Integer.toHexString(uid) + ",res=0x" + Integer.toHexString(res_addr) + ") wait=" + wait + " callbacks=" + callbacks);
        SceUidManager.checkUidPurpose(uid, "IOFileManager-File", true);
        IoInfo info = filelist.get(uid);
        if (info == null) {
            Modules.log.warn("hleIoGetAsyncStat - unknown uid " + Integer.toHexString(uid) + ", not waiting");
            Emulator.getProcessor().cpu.gpr[2] = ERROR_BAD_FILE_DESCRIPTOR;
        } else if (info.result == ERROR_NO_ASYNC_OP) {
            Modules.log.debug("hleIoGetAsyncStat - PSP_ERROR_NO_ASYNC_OP, not waiting");
            wait = false;
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NO_ASYNC_OP;
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
            if (info.result == (ERROR_FILE_NOT_FOUND & 0xffffffffL)) {
                Modules.log.debug("hleIoGetAsyncStat - file not found, not waiting");
                filelist.remove(info.uid);
                SceUidManager.releaseUid(info.uid, "IOFileManager-File");
                wait = false;
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
                info.result = ERROR_NO_ASYNC_OP;
            }

            Emulator.getProcessor().cpu.gpr[2] = 0;
        }

        if (wait) {
            State.fileLogger.logIoWaitAsync(Emulator.getProcessor().cpu.gpr[2], uid, res_addr);
        } else {
            State.fileLogger.logIoPollAsync(Emulator.getProcessor().cpu.gpr[2], uid, res_addr);
        }

    	ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        if (info != null && wait) {
            SceKernelThreadInfo currentThread = threadMan.getCurrentThread();

            // wait type
            currentThread.waitType = PSP_WAIT_MISC;

            // Go to wait state
            int timeout = 0;
            boolean forever = true;
            threadMan.hleKernelThreadWait(currentThread, timeout, forever);

            // Wait on a specific file uid
            currentThread.wait.waitingOnIo = true;
            currentThread.wait.Io_id = info.uid;

            threadMan.hleChangeThreadState(currentThread, PSP_THREAD_WAITING);
            threadMan.hleRescheduleCurrentThread(callbacks);
        }
    }

    public void hleIoOpen(int filename_addr, int flags, int permissions, boolean async) {
        String filename = readStringZ(filename_addr);
        if (Modules.log.isDebugEnabled()) Modules.log.info("hleIoOpen filename = " + filename + " flags = " + Integer.toHexString(flags) + " permissions = 0" + Integer.toOctalString(permissions));

        if (Modules.log.isDebugEnabled()) {
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

        // PSP_O_NBUF (actual name unknown).
        // This mode seems to be associated only with media files.
        // These files, when using sceMpegRingbufferPut
        // fail to properly execute the ringbuffer callback (also happens on PSP, if forced).
        if ((flags & PSP_O_NBUF) == PSP_O_NBUF) {
            Modules.log.warn("PSP_O_NBUF - " + filename + " doesn't use media buffer!");
            sceMpeg.setRingBufStatus(false);
        } else {
            // Always set to true (a valid file may be loaded after
            // an invalid one).
            sceMpeg.setRingBufStatus(true);
        }

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

        if ((flags & PSP_O_RDONLY) == PSP_O_RDONLY &&
            (flags & PSP_O_APPEND) == PSP_O_APPEND) {
            Modules.log.warn("hleIoOpen - read and append flags both set!");
        }

        IoInfo info = null;
        try {
            String pcfilename = getDeviceFilePath(filename);
            if (pcfilename != null) {
                if (Modules.log.isDebugEnabled()) Modules.log.debug("hleIoOpen - opening file " + pcfilename);

                if (isUmdPath(pcfilename)) {
                    // check umd is mounted
                    if (iso == null) {
                        Modules.log.error("hleIoOpen - no umd mounted");
                        Emulator.getProcessor().cpu.gpr[2] = ERROR_DEVICE_NOT_FOUND;

                    // check umd is activated
                    } else if (!Modules.sceUmdUserModule.isUmdActivated()) {
                        Modules.log.warn("hleIoOpen - umd mounted but not activated");
                        Emulator.getProcessor().cpu.gpr[2] = ERROR_NO_SUCH_DEVICE;

                    // check flags are valid
                    } else if ((flags & PSP_O_WRONLY) == PSP_O_WRONLY ||
                        (flags & PSP_O_CREAT) == PSP_O_CREAT ||
                        (flags & PSP_O_TRUNC) == PSP_O_TRUNC) {
                        Modules.log.error("hleIoOpen - refusing to open umd media for write");
                        Emulator.getProcessor().cpu.gpr[2] = ERROR_READ_ONLY;
                    } else {
                        // open file

                        //Tests revealed that apps are already prepared
                        //to wait for the retry count of each file.
                        for(int i = 0; i <= retry; i++) {
                            try {
                                String trimmedFileName = trimUmdPrefix(pcfilename);
                                UmdIsoFile file = iso.getFile(trimmedFileName);
                                info = new IoInfo(filename, file, mode, flags, permissions);
                                if (trimmedFileName != null && trimmedFileName.length() == 0) {
                                    // Opening "umd0:" is allowing to read the whole UMD per sectors.
                                    info.sectorBlockMode = true;
                                }
                                info.result = ERROR_NO_ASYNC_OP;
                                Emulator.getProcessor().cpu.gpr[2] = info.uid;
                                if (Modules.log.isDebugEnabled()) Modules.log.debug("hleIoOpen assigned uid = 0x" + Integer.toHexString(info.uid));
                            } catch(FileNotFoundException e) {
                                if (Modules.log.isDebugEnabled()) Modules.log.debug("hleIoOpen - umd file not found (ok to ignore this message, debug purpose only)");
                                Emulator.getProcessor().cpu.gpr[2] = ERROR_FILE_NOT_FOUND;
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
                        if (Modules.log.isDebugEnabled()) Modules.log.debug("hleIoOpen - file already exists (PSP_O_CREAT + PSP_O_EXCL)");
                        Emulator.getProcessor().cpu.gpr[2] = ERROR_FILE_ALREADY_EXISTS;
                    } else {
                        if (file.exists() && (flags & PSP_O_TRUNC) == PSP_O_TRUNC) {
                            Modules.log.warn("hleIoOpen - file already exists, deleting UNIMPLEMENT (PSP_O_TRUNC)");
                        }
                        SeekableRandomFile raf = new SeekableRandomFile(pcfilename, mode);
                        info = new IoInfo(filename, raf, mode, flags, permissions);
                        info.result = ERROR_NO_ASYNC_OP; // sceIoOpenAsync will set this properly
                        Emulator.getProcessor().cpu.gpr[2] = info.uid;
                        if (Modules.log.isDebugEnabled()) Modules.log.debug("hleIoOpen assigned uid = 0x" + Integer.toHexString(info.uid));
                    }
                }
            } else {
                Emulator.getProcessor().cpu.gpr[2] = -1;
            }
        } catch(FileNotFoundException e) {
            // To be expected under mode="r" and file doesn't exist
            if (Modules.log.isDebugEnabled()) Modules.log.debug("hleIoOpen - file not found (ok to ignore this message, debug purpose only)");
            Emulator.getProcessor().cpu.gpr[2] = ERROR_FILE_NOT_FOUND;
        }

        State.fileLogger.logIoOpen(Emulator.getProcessor().cpu.gpr[2],
                filename_addr, filename, flags, permissions, mode);

        if (async) {
            int result = Emulator.getProcessor().cpu.gpr[2];
            if (info == null) {
                Modules.log.debug("sceIoOpenAsync - file not found (ok to ignore this message, debug purpose only)");
                // For async we still need to make and return a file handle even if we couldn't open the file,
                // this is so the game can query on the handle (wait/async stat/io callback).
                info = new IoInfo(readStringZ(filename_addr), null, null, flags, permissions);
                Emulator.getProcessor().cpu.gpr[2] = info.uid;
            }

            startIoAsync(info, result);
        }
    }

    private void hleIoWrite(int uid, int data_addr, int size, boolean async) {
        IoInfo info = null;
        int result;

        if (uid == 1) {
            // stdout
            String message = Utilities.stripNL(readStringNZ(data_addr, size));
            stdout.info(Utilities.convertStringCharset(message));
            result = size;
        } else if (uid == 2) {
            // stderr
            String message = Utilities.stripNL(readStringNZ(data_addr, size));
            stderr.info(Utilities.convertStringCharset(message));
            result = size;
        } else {
            if (Modules.log.isDebugEnabled()) Modules.log.debug("hleIoWrite(uid=" + Integer.toHexString(uid) + ",data=0x" + Integer.toHexString(data_addr) + ",size=0x" + Integer.toHexString(size) + ") async=" + async);

            try {
                SceUidManager.checkUidPurpose(uid, "IOFileManager-File", true);
                info = filelist.get(uid);
                if (info == null) {
                    Modules.log.warn("hleIoWrite - unknown uid " + Integer.toHexString(uid));
                    result = ERROR_BAD_FILE_DESCRIPTOR;
                } else if (info.asyncPending) {
                    Modules.log.warn("hleIoWrite - uid " + Integer.toHexString(uid) + " PSP_ERROR_ASYNC_BUSY");
                    result = ERROR_ASYNC_BUSY;
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
        updateResult(Emulator.getProcessor().cpu, info, result, async, false);
        State.fileLogger.logIoWrite(Emulator.getProcessor().cpu.gpr[2], uid, data_addr, Emulator.getProcessor().cpu.gpr[6], size);
    }

    public void hleIoRead(int uid, int data_addr, int size, boolean async) {
        if (Modules.log.isDebugEnabled()) Modules.log.debug("hleIoRead(uid=" + Integer.toHexString(uid) + ",data=0x" + Integer.toHexString(data_addr) + ",size=0x" + Integer.toHexString(size) + ") async=" + async);
        IoInfo info = null;
        int result;

        if (uid == 3) { // stdin
            Modules.log.warn("UNIMPLEMENTED:hleIoRead uid = stdin");
            result = 0;
        } else {
            try {
                SceUidManager.checkUidPurpose(uid, "IOFileManager-File", true);
                info = filelist.get(uid);
                if (info == null) {
                    Modules.log.warn("hleIoRead - unknown uid " + Integer.toHexString(uid));
                    result = ERROR_BAD_FILE_DESCRIPTOR;
                } else if (info.asyncPending) {
                    Modules.log.warn("hleIoRead - uid " + Integer.toHexString(uid) + " PSP_ERROR_ASYNC_BUSY");
                    result = ERROR_ASYNC_BUSY;
                } else if ((data_addr < MemoryMap.START_RAM ) && (data_addr + size > MemoryMap.END_RAM)) {
                    Modules.log.warn("hleIoRead - uid " + Integer.toHexString(uid)
                        + " data is outside of ram 0x" + Integer.toHexString(data_addr)
                        + " - 0x" + Integer.toHexString(data_addr + size));
                    result = ERROR_FILE_READ_ERROR;
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
                    info.position += size;
                    Utilities.readFully(info.readOnlyFile, data_addr, size);
                    result = size;

                    if (info.sectorBlockMode) {
                    	result /= UmdIsoFile.sectorLength;
                    }
                }
            } catch(IOException e) {
                e.printStackTrace();
                result = ERROR_FILE_READ_ERROR;
            } catch(Exception e) {
                e.printStackTrace();
                result = ERROR_FILE_READ_ERROR;
                Modules.log.error("hleIoRead: Check other console for exception details. Press Run to continue.");
                Emulator.PauseEmu();
            }
        }
        updateResult(Emulator.getProcessor().cpu, info, result, async, false);
        State.fileLogger.logIoRead(Emulator.getProcessor().cpu.gpr[2], uid, data_addr, Emulator.getProcessor().cpu.gpr[6], size);
    }

    private void hleIoLseek(int uid, long offset, int whence, boolean resultIs64bit, boolean async) {
        IoInfo info = null;
    	long result = 0;

        if (uid == 1 || uid == 2 || uid == 3) { // stdio
            Modules.log.error("seek - can't seek on stdio uid " + Integer.toHexString(uid));
            result = -1;
        } else {
            try {
                SceUidManager.checkUidPurpose(uid, "IOFileManager-File", true);
                info = filelist.get(uid);
                if (info == null) {
                    Modules.log.warn("seek - unknown uid " + Integer.toHexString(uid));
                    result = ERROR_BAD_FILE_DESCRIPTOR;
                } else if (info.asyncPending) {
                    Modules.log.warn("seek - uid " + Integer.toHexString(uid) + " PSP_ERROR_ASYNC_BUSY");
                    result = ERROR_ASYNC_BUSY;
                } else {
                	if (info.sectorBlockMode) {
                		// In sectorBlockMode, the offset is a sector number
                		offset *= UmdIsoFile.sectorLength;
                	}

                	switch(whence) {
                        case PSP_SEEK_SET:
                            if (offset < 0) {
                                Modules.log.warn("SEEK_SET UID " + Integer.toHexString(uid) + " filename:'" + info.filename + "' offset=0x" + Long.toHexString(offset) + " (less than 0!)");
                                result = ERROR_INVALID_ARGUMENT;
                                State.fileLogger.logIoSeek64(ERROR_INVALID_ARGUMENT, uid, offset, whence);
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
                                result = ERROR_INVALID_ARGUMENT;
                                State.fileLogger.logIoSeek64(ERROR_INVALID_ARGUMENT, uid, offset, whence);
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
                                result = ERROR_INVALID_ARGUMENT;
                                State.fileLogger.logIoSeek64(ERROR_INVALID_ARGUMENT, uid, offset, whence);
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
                    result = info.position;
                    if (info.sectorBlockMode) {
                    	result /= UmdIsoFile.sectorLength;
                    }
                }
            } catch(IOException e) {
                e.printStackTrace();
                result = -1;
            }
        }
        updateResult(Emulator.getProcessor().cpu, info, result, async, resultIs64bit);

        if (resultIs64bit) {
            State.fileLogger.logIoSeek64(
                    (long)(Emulator.getProcessor().cpu.gpr[2] & 0xFFFFFFFFL) | ((long)Emulator.getProcessor().cpu.gpr[3] << 32),
                    uid, offset, whence);
        } else {
            State.fileLogger.logIoSeek32(Emulator.getProcessor().cpu.gpr[2], uid, (int)offset, whence);
        }
    }

    public void hleIoIoctl(int uid, int cmd, int indata_addr, int inlen, int outdata_addr, int outlen, boolean async) {
        IoInfo info = null;
        int result = -1;
        Memory mem = Memory.getInstance();

        if (Modules.log.isDebugEnabled()) {
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
            result = ERROR_BAD_FILE_DESCRIPTOR;
        } else if (info.asyncPending) {
            // Can't execute another operation until the previous one completed
            Modules.log.warn("hleIoIoctl - uid " + Integer.toHexString(uid) + " PSP_ERROR_ASYNC_BUSY");
            result = ERROR_ASYNC_BUSY;
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
                        result = ERROR_INVALID_ARGUMENT;
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
                        result = ERROR_INVALID_ARGUMENT;
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
                        result = ERROR_INVALID_ARGUMENT;
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
                        result = ERROR_INVALID_ARGUMENT;
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
                        result = ERROR_INVALID_ARGUMENT;
                    }
                    break;
                }

                // Define decryption key (Kirk / AES-128?).
                case 0x04100001:
                {
                    if (mem.isAddressGood(indata_addr) && inlen == 16) {
                        String keyHex = "";
                        for(int i = 0; i < inlen; i++) {
                            AES128Key[i] = (byte)mem.read8(indata_addr + i);
                            keyHex += String.format("%02x", AES128Key[i] & 0xFF);
                        }

                        if (Modules.log.isDebugEnabled()) {
                        	Modules.log.debug("hleIoIoctl get AES key " + keyHex);
                        }

                        if (pgdFileConnector == null) {
                        	pgdFileConnector = new PGDFileConnector();
                        }
                        info.readOnlyFile = pgdFileConnector.decryptPGDFile(info.filename, info.readOnlyFile, keyHex);

                        result = 0;
                    } else {
                        Modules.log.warn("hleIoIoctl cmd=0x04100001 " + String.format("0x%08X %d", indata_addr, inlen) + " unsupported parameters");
                        result = ERROR_INVALID_ARGUMENT;
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
        updateResult(Emulator.getProcessor().cpu, info, result, async, false);
        State.fileLogger.logIoIoctl(Emulator.getProcessor().cpu.gpr[2], uid, cmd, indata_addr, inlen, outdata_addr, outlen);
    }

    /*
     * Main functions.
     */
    public void sceIoSync(int device_addr, int unknown) {
        String device = readStringZ(device_addr);
        if (Modules.log.isDebugEnabled()) Modules.log.debug("IGNORING:sceIoSync(device='" + device + "',unknown=0x" + Integer.toHexString(unknown) + ")");
        State.fileLogger.logIoSync(0, device_addr, device, unknown);
        Emulator.getProcessor().cpu.gpr[2] = 0;
    }

    public void sceIoPollAsync(int uid, int res_addr) {
        if (Modules.log.isDebugEnabled()) Modules.log.debug("sceIoPollAsync redirecting to hleIoGetAsyncStat");
        hleIoGetAsyncStat(uid, res_addr, false, false);
    }

    public void sceIoGetAsyncStat(int uid, int poll, int res_addr) {
        if (Modules.log.isDebugEnabled()) Modules.log.debug("sceIoGetAsyncStat poll=0x" + Integer.toHexString(poll) + " redirecting to hleIoGetAsyncStat");
        hleIoGetAsyncStat(uid, res_addr, (poll == 0), false);
    }

    public void sceIoWaitAsync(int uid, int res_addr) {
        if (Modules.log.isDebugEnabled()) Modules.log.debug("sceIoWaitAsync redirecting to hleIoGetAsyncStat");
        hleIoGetAsyncStat(uid, res_addr, true, false);
    }

    public void sceIoWaitAsyncCB(int uid, int res_addr) {
        if (Modules.log.isDebugEnabled()) Modules.log.debug("sceIoWaitAsyncCB redirecting to hleIoGetAsyncStat");
        hleIoGetAsyncStat(uid, res_addr, true, true);
    }

    public void sceIoOpen(int filename_addr, int flags, int permissions) {
        if (Modules.log.isDebugEnabled()) Modules.log.debug("sceIoOpen redirecting to hleIoOpen");
        hleIoOpen(filename_addr, flags, permissions, false);
    }

    public void sceIoOpenAsync(int filename_addr, int flags, int permissions) {
        if (Modules.log.isDebugEnabled()) Modules.log.debug("sceIoOpenAsync redirecting to hleIoOpen");
        hleIoOpen(filename_addr, flags, permissions, true);
    }

    public void sceIoChangeAsyncPriority(int uid, int priority) {
    	CpuState cpu = Emulator.getProcessor().cpu;
        SceUidManager.checkUidPurpose(uid, "IOFileManager-File", true);

        if (priority < 0) {
        	cpu.gpr[2] = SceKernelErrors.ERROR_ILLEGAL_PRIORITY;
        } else if (uid == -1) {
        	defaultAsyncPriority = priority;
            cpu.gpr[2] = 0;
        } else {
            IoInfo info = filelist.get(uid);
            if (info != null && info.asyncThread != null) {
            	if (Modules.log.isDebugEnabled()) {
            		Modules.log.info(String.format("sceIoChangeAsyncPriority changing priority of async thread from fd=%x to %d", info.uid, priority));
            	}
            	info.asyncThread.currentPriority = priority;
                cpu.gpr[2] = 0;
                Modules.ThreadManForUserModule.hleRescheduleCurrentThread();
            } else {
                Modules.log.warn("sceIoChangeAsyncPriority invalid fd=" + uid);
                cpu.gpr[2] = -1;
            }
        }
    }

    public void sceIoSetAsyncCallback(int uid, int cbid, int notifyArg) {
        if (Modules.log.isDebugEnabled()) Modules.log.debug("sceIoSetAsyncCallback - uid " + Integer.toHexString(uid) + " cbid " + Integer.toHexString(cbid) + " arg 0x" + Integer.toHexString(notifyArg));
        SceUidManager.checkUidPurpose(uid, "IOFileManager-File", true);
        IoInfo info = filelist.get(uid);
        if (info == null) {
            Modules.log.warn("sceIoSetAsyncCallback - unknown uid " + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_BAD_FILE_DESCRIPTOR;
        } else {
            if (Modules.ThreadManForUserModule.hleKernelRegisterCallback(SceKernelThreadInfo.THREAD_CALLBACK_IO, cbid)) {
                info.cbid = cbid;
                info.notifyArg = notifyArg;
                triggerAsyncThread(info);
                Emulator.getProcessor().cpu.gpr[2] = 0;
            } else {
                Modules.log.warn("sceIoSetAsyncCallback - not a callback uid " + Integer.toHexString(uid));
                Emulator.getProcessor().cpu.gpr[2] = -1;
            }
        }
    }

    public void sceIoClose(int uid) {
        if (Modules.log.isDebugEnabled()) Modules.log.debug("sceIoClose - uid " + Integer.toHexString(uid));

        try {
            SceUidManager.checkUidPurpose(uid, "IOFileManager-File", true);
            IoInfo info = filelist.remove(uid);
            if (info == null) {
                if (uid != 1 && uid != 2) // ignore stdout and stderr
                    Modules.log.warn("sceIoClose - unknown uid " + Integer.toHexString(uid));
                Emulator.getProcessor().cpu.gpr[2] = ERROR_BAD_FILE_DESCRIPTOR;
            } else {
                if (info.readOnlyFile != null) {
                    // Can be just closing an empty handle, because hleIoOpen(async==true)
                    // generates a dummy IoInfo when the file could not be opened.
                    info.readOnlyFile.close();
                }
                SceUidManager.releaseUid(info.uid, "IOFileManager-File");
                triggerAsyncThread(info);
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
    	CpuState cpu = Emulator.getProcessor().cpu;
        if (Modules.log.isDebugEnabled()) Modules.log.debug("sceIoCloseAsync - uid " + Integer.toHexString(uid));

        SceUidManager.checkUidPurpose(uid, "IOFileManager-File", true);
        IoInfo info = filelist.get(uid);
        if (info != null) {
            if (info.asyncPending) {
                Modules.log.warn("sceIoCloseAsync - uid " + Integer.toHexString(uid) + " PSP_ERROR_ASYNC_BUSY");
                cpu.gpr[2] = ERROR_ASYNC_BUSY;
            } else {
                info.closePending = true;
                updateResult(cpu, info, 0, true, false);
            }
        } else {
            cpu.gpr[2] = ERROR_BAD_FILE_DESCRIPTOR;
        }
    }

    public void sceIoWrite(int uid, int data_addr, int size) {
        hleIoWrite(uid, data_addr, size, false);
    }

    public void sceIoWriteAsync(int uid, int data_addr, int size) {
        hleIoWrite(uid, data_addr, size, true);
    }

    public void sceIoRead(int uid, int data_addr, int size) {
        hleIoRead(uid, data_addr, size, false);
    }

    public void sceIoReadAsync(int uid, int data_addr, int size) {
        hleIoRead(uid, data_addr, size, true);
    }

    public void sceIoLseek(int uid, long offset, int whence) {
        if (Modules.log.isDebugEnabled()) Modules.log.debug("sceIoLseek - uid " + Integer.toHexString(uid) + " offset " + offset + " (hex=0x" + Long.toHexString(offset) + ") whence " + getWhenceName(whence));
        hleIoLseek(uid, offset, whence, true, false);
    }

    public void sceIoLseekAsync(int uid, long offset, int whence) {
        if (Modules.log.isDebugEnabled()) Modules.log.debug("sceIoLseekAsync - uid " + Integer.toHexString(uid) + " offset " + offset + " (hex=0x" + Long.toHexString(offset) + ") whence " + getWhenceName(whence));
        hleIoLseek(uid, offset, whence, true, true);
    }

    public void sceIoLseek32(int uid, int offset, int whence) {
        if (Modules.log.isDebugEnabled()) Modules.log.debug("sceIoLseek32 - uid " + Integer.toHexString(uid) + " offset " + offset + " (hex=0x" + Integer.toHexString(offset) + ") whence " + getWhenceName(whence));
        hleIoLseek(uid, (long)offset, whence, false, false);
    }

    public void sceIoLseek32Async(int uid, int offset, int whence) {
        if (Modules.log.isDebugEnabled()) Modules.log.debug("sceIoLseek32Async - uid " + Integer.toHexString(uid) + " offset " + offset + " (hex=0x" + Integer.toHexString(offset) + ") whence " + getWhenceName(whence));
        hleIoLseek(uid, (long)offset, whence, false, true);
    }

    public void sceIoMkdir(int dir_addr, int permissions) {
        String dir = readStringZ(dir_addr);
        if (Modules.log.isDebugEnabled()) Modules.log.debug("sceIoMkdir dir = " + dir);

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
        if (Modules.log.isDebugEnabled()) Modules.log.debug("sceIoChdir path = " + path);

        if (path.equals("..")) {
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
        if (Modules.log.isDebugEnabled()) Modules.log.debug("sceIoDopen dirname = " + dirname);

        String pcfilename = getDeviceFilePath(dirname);
        if (pcfilename != null) {

            if (isUmdPath(pcfilename)) {
                // Files in our iso virtual file system
                String isofilename = trimUmdPrefix(pcfilename);
                if (Modules.log.isDebugEnabled()) Modules.log.debug("sceIoDopen - isofilename = " + isofilename);
                // check umd is mounted
                if (iso == null) {
                    Modules.log.error("sceIoDopen - no umd mounted");
                    Emulator.getProcessor().cpu.gpr[2] = ERROR_DEVICE_NOT_FOUND;
                // check umd is activated
                } else if (!Modules.sceUmdUserModule.isUmdActivated()) {
                    Modules.log.warn("sceIoDopen - umd mounted but not activated");
                    Emulator.getProcessor().cpu.gpr[2] = ERROR_NO_SUCH_DEVICE;
                } else {
                    try {
                        if (iso.isDirectory(isofilename)) {
                            String[] filenames = iso.listDirectory(isofilename);
                            IoDirInfo info = new IoDirInfo(pcfilename, filenames);
                            Emulator.getProcessor().cpu.gpr[2] = info.uid;
                        } else {
                            Modules.log.warn("sceIoDopen '" + isofilename + "' not a umd directory!");
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
                Modules.log.warn("sceIoDopen apps running outside of ms0 dir are not fully supported, relative child paths should still work");
                Emulator.getProcessor().cpu.gpr[2] = -1;
            } else {
                // Regular apps run from inside mstick dir or absolute path given
                if (Modules.log.isDebugEnabled()) Modules.log.debug("sceIoDopen - pcfilename = " + pcfilename);
                File f = new File(pcfilename);
                if (f.isDirectory()) {
                    IoDirInfo info = new IoDirInfo(pcfilename, f.list());
                    Emulator.getProcessor().cpu.gpr[2] = info.uid;
                } else {
                    Modules.log.warn("sceIoDopen '" + pcfilename + "' not a directory! (could be missing)");
                    Emulator.getProcessor().cpu.gpr[2] = ERROR_FILE_NOT_FOUND;
                }
            }
        } else {
            Emulator.getProcessor().cpu.gpr[2] = -1;
        }
        State.fileLogger.logIoDopen(Emulator.getProcessor().cpu.gpr[2], dirname_addr, dirname);
    }

    public void sceIoDread(int uid, int dirent_addr) {
        SceUidManager.checkUidPurpose(uid, "IOFileManager-Directory", true);
        IoDirInfo info = dirlist.get(uid);
        if (info == null) {
            Modules.log.warn("sceIoDread unknown uid " + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_BAD_FILE_DESCRIPTOR;
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
        State.fileLogger.logIoDread(Emulator.getProcessor().cpu.gpr[2], uid, dirent_addr);
    }

    public void sceIoDclose(int uid) {
        if (Modules.log.isDebugEnabled()) Modules.log.debug("sceIoDclose - uid = " + Integer.toHexString(uid));

        SceUidManager.checkUidPurpose(uid, "IOFileManager-Directory", true);
        IoDirInfo info = dirlist.remove(uid);
        if (info == null) {
            Modules.log.warn("sceIoDclose - unknown uid " + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_BAD_FILE_DESCRIPTOR;
        } else {
            SceUidManager.releaseUid(info.uid, "IOFileManager-Directory");
            Emulator.getProcessor().cpu.gpr[2] = 0;
        }

        State.fileLogger.logIoDclose(Emulator.getProcessor().cpu.gpr[2], uid);
    }

    public void sceIoDevctl(int device_addr, int cmd, int indata_addr, int inlen, int outdata_addr, int outlen) {
        String device = readStringZ(device_addr);
        if (Modules.log.isDebugEnabled()) {
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
                ThreadManForUser threadMan = Modules.ThreadManForUserModule;

                if (!device.equals("mscmhc0:")) {
                    Emulator.getProcessor().cpu.gpr[2] = ERROR_UNSUPPORTED_OPERATION;
                } else if (mem.isAddressGood(indata_addr) && inlen == 4) {
                    int cbid = mem.read32(indata_addr);
                    if (threadMan.hleKernelRegisterCallback(SceKernelThreadInfo.THREAD_CALLBACK_MEMORYSTICK, cbid)) {
                        // Trigger callback immediately
                        threadMan.hleKernelNotifyCallback(SceKernelThreadInfo.THREAD_CALLBACK_MEMORYSTICK, MemoryStick.getState());
                        Emulator.getProcessor().cpu.gpr[2] = 0; // Success
                    } else {

                        Emulator.getProcessor().cpu.gpr[2] = ERROR_DEVCTL_BAD_PARAMS; // No such callback
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
                ThreadManForUser threadMan = Modules.ThreadManForUserModule;

                if (!device.equals("mscmhc0:")) {
                    Emulator.getProcessor().cpu.gpr[2] = ERROR_UNSUPPORTED_OPERATION;
                } else if (mem.isAddressGood(indata_addr) && inlen == 4) {
                    int cbid = mem.read32(indata_addr);
                    if (threadMan.hleKernelUnRegisterCallback(SceKernelThreadInfo.THREAD_CALLBACK_MEMORYSTICK, cbid) != null) {
                        Emulator.getProcessor().cpu.gpr[2] = 0; // Success
                    } else {
                        Emulator.getProcessor().cpu.gpr[2] = ERROR_DEVCTL_BAD_PARAMS; // No such callback
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
                    Emulator.getProcessor().cpu.gpr[2] = ERROR_UNSUPPORTED_OPERATION;
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
                    Emulator.getProcessor().cpu.gpr[2] = ERROR_UNSUPPORTED_OPERATION;
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
                ThreadManForUser threadMan = Modules.ThreadManForUserModule;

                if (!device.equals("fatms0:")) {
                    Emulator.getProcessor().cpu.gpr[2] = ERROR_DEVCTL_BAD_PARAMS;
                } else if (mem.isAddressGood(indata_addr) && inlen == 4) {
                    int cbid = mem.read32(indata_addr);
                    threadMan.hleKernelRegisterCallback(SceKernelThreadInfo.THREAD_CALLBACK_MEMORYSTICK, cbid);
                    // Trigger callback immediately
                    threadMan.hleKernelNotifyCallback(SceKernelThreadInfo.THREAD_CALLBACK_MEMORYSTICK, MemoryStick.getState());
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
                ThreadManForUser threadMan = Modules.ThreadManForUserModule;

                if (!device.equals("fatms0:")) {
                    Emulator.getProcessor().cpu.gpr[2] = ERROR_DEVCTL_BAD_PARAMS;
                } else if (mem.isAddressGood(indata_addr) && inlen == 4) {
                    int cbid = mem.read32(indata_addr);
                    threadMan.hleKernelUnRegisterCallback(SceKernelThreadInfo.THREAD_CALLBACK_MEMORYSTICK, cbid);
                    Emulator.getProcessor().cpu.gpr[2] = 0;  // Success
                } else {
                    Emulator.getProcessor().cpu.gpr[2] = -1; // Invalid parameters
                }
                break;
            }

            case 0x02415823:
                Modules.log.warn("IGNORED: sceIoDevctl " + String.format("0x%08X", cmd) + " unhandled ms command");
                Emulator.getProcessor().cpu.gpr[2] = 0;
                break;

            case 0x02425818: // Free space on ms
            {
                int sectorSize = 0x200;
                int sectorCount = 0x08;
                int maxClusters = (int) ((MemoryStick.getFreeSize() * 95L / 100) / (sectorSize * sectorCount));
                int freeClusters = maxClusters;
                int maxSectors = 512;

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
                    Emulator.getProcessor().cpu.gpr[2] = ERROR_DEVCTL_BAD_PARAMS;
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
                Emulator.getProcessor().cpu.gpr[2] = -1;
                break;
        }
        State.fileLogger.logIoDevctl(Emulator.getProcessor().cpu.gpr[2],
                device_addr, device, cmd, indata_addr, inlen, outdata_addr, outlen);
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

        Emulator.getProcessor().cpu.gpr[2] = 0;

        State.fileLogger.logIoAssign(Emulator.getProcessor().cpu.gpr[2],
                dev1_addr, dev1, dev2_addr, dev2, dev3_addr, dev3, mode, unk1, unk2);
    }

    public void sceIoGetstat(int file_addr, int stat_addr) {
        String filename = readStringZ(file_addr);
        if (Modules.log.isDebugEnabled()) Modules.log.debug("sceIoGetstat - file = " + filename + " stat = " + Integer.toHexString(stat_addr));

        String pcfilename = getDeviceFilePath(filename);
        SceIoStat stat = stat(pcfilename);
        if (stat != null) {
            stat.write(Memory.getInstance(), stat_addr);
            Emulator.getProcessor().cpu.gpr[2] = 0;
        } else {
            Emulator.getProcessor().cpu.gpr[2] = ERROR_FILE_NOT_FOUND;
        }

        State.fileLogger.logIoGetStat(Emulator.getProcessor().cpu.gpr[2],
                file_addr, filename, stat_addr);
    }

    public void sceIoRemove(int file_addr) {
        String filename = readStringZ(file_addr);
        if (Modules.log.isDebugEnabled()) Modules.log.debug("sceIoRemove - file = " + filename);

        String pcfilename = getDeviceFilePath(filename);

        if (pcfilename != null) {
            if (isUmdPath(pcfilename)) {
                Emulator.getProcessor().cpu.gpr[2] = -1;
            } else {
                File file = new File(pcfilename);
                if (file.delete()) {
                    Emulator.getProcessor().cpu.gpr[2] = 0;
                } else {
                    Emulator.getProcessor().cpu.gpr[2] = -1;
                }
            }
        } else {
            Emulator.getProcessor().cpu.gpr[2] = -1;
        }

        State.fileLogger.logIoRemove(Emulator.getProcessor().cpu.gpr[2], file_addr, filename);
    }

    public void sceIoChstat(int file_addr, int stat_addr, int bits) {
        String filename = readStringZ(file_addr);
        if (Modules.log.isDebugEnabled()) Modules.log.debug("sceIoChstat - file = " + filename + ", bits=0x" + Integer.toHexString(bits));

        String pcfilename = getDeviceFilePath(filename);

        if (pcfilename != null) {
            if (isUmdPath(pcfilename)) {
                Emulator.getProcessor().cpu.gpr[2] = -1;
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
                	Emulator.getProcessor().cpu.gpr[2] = -1;
                }
            }
        } else {
            Emulator.getProcessor().cpu.gpr[2] = -1;
        }
        State.fileLogger.logIoChstat(Emulator.getProcessor().cpu.gpr[2], file_addr, filename, stat_addr, bits);
    }

    /*
     * IOInfo.
     */
    class IoInfo {
        // PSP settings
        public final int flags;
        public final int permissions;

        // Internal settings
        public final String filename;
        public final SeekableRandomFile msFile; // on memory stick, should either be identical to readOnlyFile or null
        public SeekableDataInput readOnlyFile; // on memory stick or umd
        public final String mode;
        public long position; // virtual position, beyond the end is allowed, before the start is an error
        public boolean sectorBlockMode;

        public final int uid;
        public long result; // The return value from the last operation on this file, used by sceIoWaitAsync
        public boolean closePending = false; // sceIoCloseAsync has been called on this file
        public boolean asyncPending; // Thread has not switched since an async operation was called on this file
        public SceKernelThreadInfo asyncThread;

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