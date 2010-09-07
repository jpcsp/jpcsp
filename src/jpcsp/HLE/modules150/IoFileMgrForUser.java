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

package jpcsp.HLE.modules150;

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_ASYNC_BUSY;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_BAD_FILE_DESCRIPTOR;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_DEVCTL_BAD_PARAMS;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_DEVICE_NOT_FOUND;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_FILE_ALREADY_EXISTS;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_FILE_NOT_FOUND;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_FILE_READ_ERROR;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_INVALID_ARGUMENT;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_NO_ASYNC_OP;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_NO_SUCH_DEVICE;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_READ_ONLY;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_UNSUPPORTED_OPERATION;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_READY;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_WAITING;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_WAIT_EVENTFLAG;
import static jpcsp.util.Utilities.readStringNZ;
import static jpcsp.util.Utilities.readStringZ;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Pattern;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Processor;
import jpcsp.State;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.types.SceIoDirent;
import jpcsp.HLE.kernel.types.SceIoStat;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.ScePspDateTime;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.HLEStartModule;
import jpcsp.HLE.modules.ThreadManForUser;
import jpcsp.connector.PGDFileConnector;
import jpcsp.filesystems.SeekableDataInput;
import jpcsp.filesystems.SeekableRandomFile;
import jpcsp.filesystems.umdiso.UmdIsoFile;
import jpcsp.filesystems.umdiso.UmdIsoReader;
import jpcsp.hardware.MemoryStick;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

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
 * 4. The PSP's filesystem supports permissions.
 * Implement PSP_O_CREAT based on Java File and it's setReadable/Writable/Executable.
 */
public class IoFileMgrForUser implements HLEModule, HLEStartModule {
    private static Logger log = Modules.getLogger("IoFileMgrForUser");
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
    public final static int PSP_O_NBUF     = 0x4000;     // Used on the PSP to bypass the internal disc cache (commonly seen in media files that need to maintain a fixed bitrate).
    public final static int PSP_O_NOWAIT   = 0x8000;
    public final static int PSP_O_PLOCK    = 0x2000000;  // Used on the PSP to open the file inside a power lock (safe).
    public final static int PSP_O_PGD      = 0x40000000; // From "Kingdom Hearts: Birth by Sleep".

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

    // Type of device used (abstract).
    // Can simbolize physical character or block devices, logical filesystem devices
    // or devices represented by an alias or even mount point devices also represented by an alias.
    public final static int PSP_DEV_TYPE_NONE        = 0x0;
    public final static int PSP_DEV_TYPE_CHARACTER   = 0x1;
    public final static int PSP_DEV_TYPE_BLOCK       = 0x4;
    public final static int PSP_DEV_TYPE_FILESYSTEM  = 0x10;
    public final static int PSP_DEV_TYPE_ALIAS       = 0x20;
    public final static int PSP_DEV_TYPE_MOUNT       = 0x40;

    // One async operation takes at least 10 millis to complete
    private final static int ASYNC_DELAY_MILLIS = 10;

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
        public long asyncDoneMillis; // When the async operation can be completed
        public SceKernelThreadInfo asyncThread;

        // Async callback
        public int cbid = -1;
        public int notifyArg = 0;

        /** Memory stick version */
        public IoInfo(String filename, SeekableRandomFile f, String mode, int flags, int permissions) {
            this.filename = filename;
            msFile = f;
            readOnlyFile = f;
            this.mode = mode;
            this.flags = flags;
            this.permissions = permissions;
            sectorBlockMode = false;
            uid = SceUidManager.getNewUid("IOFileManager-File");
            filelist.put(uid, this);
        }

        /** UMD version (read only) */
        public IoInfo(String filename, SeekableDataInput f, String mode, int flags, int permissions) {
            this.filename = filename;
            msFile = null;
            readOnlyFile = f;
            this.mode = mode;
            this.flags = flags;
            this.permissions = permissions;
            sectorBlockMode = false;
            uid = SceUidManager.getNewUid("IOFileManager-File");
            filelist.put(uid, this);
        }

        public boolean isUmdFile() {
            return (msFile == null);
        }

        public int getAsyncRestMillis() {
        	long now = Emulator.getClock().currentTimeMillis();
        	if (now >= asyncDoneMillis) {
        		return 0;
        	}

        	return (int) (asyncDoneMillis - now);
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

	@Override
	public String getName() { return "IoFileMgrForUser"; }

	@Override
	public void installModule(HLEModuleManager mm, int version) {
		if (version >= 150) {

			mm.addFunction(0x3251EA56, sceIoPollAsyncFunction);
			mm.addFunction(0xE23EEC33, sceIoWaitAsyncFunction);
			mm.addFunction(0x35DBD746, sceIoWaitAsyncCBFunction);
			mm.addFunction(0xCB05F8D6, sceIoGetAsyncStatFunction);
			mm.addFunction(0xB293727F, sceIoChangeAsyncPriorityFunction);
			mm.addFunction(0xA12A0514, sceIoSetAsyncCallbackFunction);
			mm.addFunction(0x810C4BC3, sceIoCloseFunction);
			mm.addFunction(0xFF5940B6, sceIoCloseAsyncFunction);
			mm.addFunction(0x109F50BC, sceIoOpenFunction);
			mm.addFunction(0x89AA9906, sceIoOpenAsyncFunction);
			mm.addFunction(0x6A638D83, sceIoReadFunction);
			mm.addFunction(0xA0B5A7C2, sceIoReadAsyncFunction);
			mm.addFunction(0x42EC03AC, sceIoWriteFunction);
			mm.addFunction(0x0FACAB19, sceIoWriteAsyncFunction);
			mm.addFunction(0x27EB27B8, sceIoLseekFunction);
			mm.addFunction(0x71B19E77, sceIoLseekAsyncFunction);
			mm.addFunction(0x68963324, sceIoLseek32Function);
			mm.addFunction(0x1B385D8F, sceIoLseek32AsyncFunction);
			mm.addFunction(0x63632449, sceIoIoctlFunction);
			mm.addFunction(0xE95A012B, sceIoIoctlAsyncFunction);
			mm.addFunction(0xB29DDF9C, sceIoDopenFunction);
			mm.addFunction(0xE3EB004C, sceIoDreadFunction);
			mm.addFunction(0xEB092469, sceIoDcloseFunction);
			mm.addFunction(0xF27A9C51, sceIoRemoveFunction);
			mm.addFunction(0x06A70004, sceIoMkdirFunction);
			mm.addFunction(0x1117C65F, sceIoRmdirFunction);
			mm.addFunction(0x55F4717D, sceIoChdirFunction);
			mm.addFunction(0xAB96437F, sceIoSyncFunction);
			mm.addFunction(0xACE946E8, sceIoGetstatFunction);
			mm.addFunction(0xB8A740F4, sceIoChstatFunction);
			mm.addFunction(0x779103A0, sceIoRenameFunction);
			mm.addFunction(0x54F5FB11, sceIoDevctlFunction);
			mm.addFunction(0x08BD7374, sceIoGetDevTypeFunction);
			mm.addFunction(0xB2A628C1, sceIoAssignFunction);
			mm.addFunction(0x6D08A871, sceIoUnassignFunction);
			mm.addFunction(0xE8BC6571, sceIoCancelFunction);
			mm.addFunction(0x5C2BE2CC, sceIoGetFdListFunction);

		}
	}

	@Override
	public void uninstallModule(HLEModuleManager mm, int version) {
		if (version >= 150) {

			mm.removeFunction(sceIoPollAsyncFunction);
			mm.removeFunction(sceIoWaitAsyncFunction);
			mm.removeFunction(sceIoWaitAsyncCBFunction);
			mm.removeFunction(sceIoGetAsyncStatFunction);
			mm.removeFunction(sceIoChangeAsyncPriorityFunction);
			mm.removeFunction(sceIoSetAsyncCallbackFunction);
			mm.removeFunction(sceIoCloseFunction);
			mm.removeFunction(sceIoCloseAsyncFunction);
			mm.removeFunction(sceIoOpenFunction);
			mm.removeFunction(sceIoOpenAsyncFunction);
			mm.removeFunction(sceIoReadFunction);
			mm.removeFunction(sceIoReadAsyncFunction);
			mm.removeFunction(sceIoWriteFunction);
			mm.removeFunction(sceIoWriteAsyncFunction);
			mm.removeFunction(sceIoLseekFunction);
			mm.removeFunction(sceIoLseekAsyncFunction);
			mm.removeFunction(sceIoLseek32Function);
			mm.removeFunction(sceIoLseek32AsyncFunction);
			mm.removeFunction(sceIoIoctlFunction);
			mm.removeFunction(sceIoIoctlAsyncFunction);
			mm.removeFunction(sceIoDopenFunction);
			mm.removeFunction(sceIoDreadFunction);
			mm.removeFunction(sceIoDcloseFunction);
			mm.removeFunction(sceIoRemoveFunction);
			mm.removeFunction(sceIoMkdirFunction);
			mm.removeFunction(sceIoRmdirFunction);
			mm.removeFunction(sceIoChdirFunction);
			mm.removeFunction(sceIoSyncFunction);
			mm.removeFunction(sceIoGetstatFunction);
			mm.removeFunction(sceIoChstatFunction);
			mm.removeFunction(sceIoRenameFunction);
			mm.removeFunction(sceIoDevctlFunction);
			mm.removeFunction(sceIoGetDevTypeFunction);
			mm.removeFunction(sceIoAssignFunction);
			mm.removeFunction(sceIoUnassignFunction);
			mm.removeFunction(sceIoCancelFunction);
			mm.removeFunction(sceIoGetFdListFunction);

		}
	}

	@Override
	public void start() {
        if (filelist != null) {
            // Close open files
            for (Iterator<IoInfo> it = filelist.values().iterator(); it.hasNext();) {
                IoInfo info = it.next();
                try {
                    info.readOnlyFile.close();
                } catch(IOException e) {
                    log.error("pspiofilemgr - error closing file: " + e.getMessage());
                }
            }
        }

        filelist = new HashMap<Integer, IoInfo>();
        dirlist = new HashMap<Integer, IoDirInfo>();
        MemoryStick.setState(MemoryStick.PSP_MEMORYSTICK_STATE_INSERTED);
        defaultAsyncPriority = -1;
    }

	@Override
	public void stop() {
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

    public String[] listFiles(String dir, String pattern) {
    	String pcfilename = getDeviceFilePath(dir);
    	if (pcfilename == null) {
    		return null;
    	}

    	File f = new File(pcfilename);

    	return pattern == null ? f.list() : f.list(new PatternFilter(pattern));
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
                    log.error("stat - no umd mounted");
                    Emulator.getProcessor().cpu.gpr[2] = ERROR_DEVICE_NOT_FOUND;

                // check umd is activated
                } else if (!Modules.sceUmdUserModule.isUmdActivated()) {
                    log.warn("stat - umd mounted but not activated");
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
                        log.warn("stat - '" + isofilename + "' umd file not found");
                    } catch(IOException e) {
                        log.warn("stat - umd io error: " + e.getMessage());
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
                    log.error("getFile - no umd mounted");
                    return resultFile;
                // check flags are valid
                } else if ((flags & PSP_O_WRONLY) == PSP_O_WRONLY ||
                    (flags & PSP_O_CREAT) == PSP_O_CREAT ||
                    (flags & PSP_O_TRUNC) == PSP_O_TRUNC) {
                    log.error("getFile - refusing to open umd media for write");
                    return resultFile;
                } else {
                    // open file
                    try {
                        UmdIsoFile file = iso.getFile(trimUmdPrefix(pcfilename));
                        resultFile = file;
                    } catch(FileNotFoundException e) {
                        if (log.isDebugEnabled()) log.debug("getFile - umd file not found '" + pcfilename + "' (ok to ignore this message, debug purpose only)");
                    } catch(IOException e) {
                        log.error("getFile - error opening umd media: " + e.getMessage());
                    }
                }
            } else {
                // First check if the file already exists
                File file = new File(pcfilename);
                if (file.exists() &&
                    (flags & PSP_O_CREAT) == PSP_O_CREAT &&
                    (flags & PSP_O_EXCL) == PSP_O_EXCL) {
                    if (log.isDebugEnabled()) log.debug("getFile - file already exists (PSP_O_CREAT + PSP_O_EXCL)");
                } else {
                    if (file.exists() &&
                        (flags & PSP_O_TRUNC) == PSP_O_TRUNC) {
                        log.warn("getFile - file already exists, deleting UNIMPLEMENT (PSP_O_TRUNC)");
                    }
                    String mode = getMode(flags);

                    try {
                        SeekableRandomFile raf = new SeekableRandomFile(pcfilename, mode);
                        resultFile = raf;
                    } catch (FileNotFoundException e) {
                        if (log.isDebugEnabled()) log.debug("getFile - file not found '" + pcfilename + "' (ok to ignore this message, debug purpose only)");
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
        }
		return info.readOnlyFile;
    }

    public String getFileFilename(int uid) {
        SceUidManager.checkUidPurpose(uid, "IOFileManager-File", true);
        IoInfo info = filelist.get(uid);
        if (info == null) {
            return null;
        }
		return info.filename;
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
        log.info("pspiofilemgr - filepath " + filepath);
        this.filepath = filepath;
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
    public void hleAsyncThread(Processor processor) {
        CpuState cpu = processor.cpu;
    	ThreadManForUser threadMan = Modules.ThreadManForUserModule;

        int uid = cpu.gpr[asyncThreadRegisterArgument];
        if (log.isDebugEnabled()) {
        	log.debug("hleAsyncThread uid=" + Integer.toHexString(uid));
        }

        IoInfo info = filelist.get(uid);
        if (info == null) {
        	cpu.gpr[2] = 0; // Exit status
        	// Exit and delete the thread to free its resources (e.g. its stack)
        	threadMan.hleKernelExitDeleteThread();
        } else {
        	boolean asyncCompleted = doStepAsync(info);
        	if (threadMan.getCurrentThread() == info.asyncThread) {
        		if (asyncCompleted) {
	        		// Wait for a new Async IO... wakeup is done by triggerAsyncThread()
	        		threadMan.hleKernelSleepThread(false);
        		} else {
        			// Wait for the Async IO to complete...
        			threadMan.hleKernelDelayThread(info.getAsyncRestMillis() * 1000, false);
        		}
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
        info.asyncDoneMillis = Emulator.getClock().currentTimeMillis() + ASYNC_DELAY_MILLIS;
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

    private boolean doStepAsync(IoInfo info) {
    	boolean done = true;

    	if (info != null && info.asyncPending) {
        	ThreadManForUser threadMan = Modules.ThreadManForUserModule;
    		if (info.getAsyncRestMillis() > 0) {
    			done = false;
    		} else {
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
		            	if (log.isDebugEnabled()) {
		            		log.debug("pspiofilemgr - onContextSwitch waking " + Integer.toHexString(thread.uid) + " thread:'" + thread.name + "'");
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
    	}

        return done;
    }

    /*
     * HLE functions.
     */
    public void hleIoGetAsyncStat(int uid, int res_addr, boolean wait, boolean callbacks) {
        if (log.isDebugEnabled()) log.debug("hleIoGetAsyncStat(uid=" + Integer.toHexString(uid) + ",res=0x" + Integer.toHexString(res_addr) + ") wait=" + wait + " callbacks=" + callbacks);
        SceUidManager.checkUidPurpose(uid, "IOFileManager-File", true);
        IoInfo info = filelist.get(uid);
        if (info == null) {
            log.warn("hleIoGetAsyncStat - unknown uid " + Integer.toHexString(uid) + ", not waiting");
            Emulator.getProcessor().cpu.gpr[2] = ERROR_BAD_FILE_DESCRIPTOR;
        } else if (info.result == ERROR_NO_ASYNC_OP) {
            log.debug("hleIoGetAsyncStat - PSP_ERROR_NO_ASYNC_OP, not waiting");
            wait = false;
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NO_ASYNC_OP;
        } else if (info.asyncPending && !wait) {
            // Need to wait for context switch before we can allow the good result through
            log.debug("hleIoGetAsyncStat - poll return=1(busy), not waiting");
            Emulator.getProcessor().cpu.gpr[2] = 1;
        } else {
            if (info.closePending) {
                log.debug("hleIoGetAsyncStat - file marked with closePending, calling sceIoClose, not waiting");
                hleIoClose(uid);
                wait = false;
            }

            // Deferred error reporting from sceIoOpenAsync(filenotfound)
            if (info.result == (ERROR_FILE_NOT_FOUND & 0xffffffffL)) {
                log.debug("hleIoGetAsyncStat - file not found, not waiting");
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
                log.debug("hleIoGetAsyncStat - already context switched, not waiting");
                wait = false;
            }

            Memory mem = Memory.getInstance();
            if (mem.isAddressGood(res_addr)) {
                log.debug("hleIoGetAsyncStat - storing result 0x" + Long.toHexString(info.result));
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
            currentThread.waitType = PSP_WAIT_EVENTFLAG;

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
        if (log.isInfoEnabled()) log.info("hleIoOpen filename = " + filename + " flags = " + Integer.toHexString(flags) + " permissions = 0" + Integer.toOctalString(permissions));

        if (log.isDebugEnabled()) {
            if ((flags & PSP_O_RDONLY) == PSP_O_RDONLY) log.debug("PSP_O_RDONLY");
            if ((flags & PSP_O_WRONLY) == PSP_O_WRONLY) log.debug("PSP_O_WRONLY");
            if ((flags & PSP_O_NBLOCK) == PSP_O_NBLOCK) log.debug("PSP_O_NBLOCK");
            if ((flags & PSP_O_DIROPEN) == PSP_O_DIROPEN) log.debug("PSP_O_DIROPEN");
            if ((flags & PSP_O_APPEND) == PSP_O_APPEND) log.debug("PSP_O_APPEND");
            if ((flags & PSP_O_CREAT) == PSP_O_CREAT) log.debug("PSP_O_CREAT");
            if ((flags & PSP_O_TRUNC) == PSP_O_TRUNC) log.debug("PSP_O_TRUNC");
            if ((flags & PSP_O_EXCL) == PSP_O_EXCL) log.debug("PSP_O_EXCL");
            if ((flags & PSP_O_NBUF) == PSP_O_NBUF) log.debug("PSP_O_NBUF");
            if ((flags & PSP_O_NOWAIT) == PSP_O_NOWAIT) log.debug("PSP_O_NOWAIT");
            if ((flags & PSP_O_PLOCK) == PSP_O_PLOCK) log.debug("PSP_O_PLOCK");
            if ((flags & PSP_O_PGD) == PSP_O_PGD) log.debug("PSP_O_PGD");
        }

        String mode = getMode(flags);

        if (mode == null) {
            log.error("hleIoOpen - unhandled flags " + Integer.toHexString(flags));
            State.fileLogger.logIoOpen(-1, filename_addr, filename, flags, permissions, mode);
            Emulator.getProcessor().cpu.gpr[2] = -1;
            return;
        }

        //Retry count.
        int retry = (flags >> 16) & 0x000F;

        if (retry != 0) {
            log.warn("hleIoOpen - retry count is " + retry);
        }

        if ((flags & PSP_O_RDONLY) == PSP_O_RDONLY &&
            (flags & PSP_O_APPEND) == PSP_O_APPEND) {
            log.warn("hleIoOpen - read and append flags both set!");
        }

        IoInfo info = null;
        try {
            String pcfilename = getDeviceFilePath(filename);
            if (pcfilename != null) {
                if (log.isDebugEnabled()) log.debug("hleIoOpen - opening file " + pcfilename);

                if (isUmdPath(pcfilename)) {
                    // check umd is mounted
                    if (iso == null) {
                        log.error("hleIoOpen - no umd mounted");
                        Emulator.getProcessor().cpu.gpr[2] = ERROR_DEVICE_NOT_FOUND;

                    // check umd is activated
                    } else if (!Modules.sceUmdUserModule.isUmdActivated()) {
                        log.warn("hleIoOpen - umd mounted but not activated");
                        Emulator.getProcessor().cpu.gpr[2] = ERROR_NO_SUCH_DEVICE;

                    // check flags are valid
                    } else if ((flags & PSP_O_WRONLY) == PSP_O_WRONLY ||
                        (flags & PSP_O_CREAT) == PSP_O_CREAT ||
                        (flags & PSP_O_TRUNC) == PSP_O_TRUNC) {
                        log.error("hleIoOpen - refusing to open umd media for write");
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
                                if (log.isDebugEnabled()) log.debug("hleIoOpen assigned uid = 0x" + Integer.toHexString(info.uid));
                            } catch(FileNotFoundException e) {
                                if (log.isDebugEnabled()) log.debug("hleIoOpen - umd file not found (ok to ignore this message, debug purpose only)");
                                Emulator.getProcessor().cpu.gpr[2] = ERROR_FILE_NOT_FOUND;
                            } catch(IOException e) {
                                log.error("hleIoOpen - error opening umd media: " + e.getMessage());
                                Emulator.getProcessor().cpu.gpr[2] = -1;
                            }
                        }
                    }
                } else {
                    // First check if the file already exists
                    File file = new File(pcfilename);
                    if (file.exists() && (flags & PSP_O_CREAT) == PSP_O_CREAT &&
                        (flags & PSP_O_EXCL) == PSP_O_EXCL) {
                        if (log.isDebugEnabled()) log.debug("hleIoOpen - file already exists (PSP_O_CREAT + PSP_O_EXCL)");
                        Emulator.getProcessor().cpu.gpr[2] = ERROR_FILE_ALREADY_EXISTS;
                    } else {
                        if (file.exists() && (flags & PSP_O_TRUNC) == PSP_O_TRUNC) {
                            log.warn("hleIoOpen - file already exists, deleting UNIMPLEMENT (PSP_O_TRUNC)");
                        }
                        SeekableRandomFile raf = new SeekableRandomFile(pcfilename, mode);
                        info = new IoInfo(filename, raf, mode, flags, permissions);
                        info.result = ERROR_NO_ASYNC_OP; // sceIoOpenAsync will set this properly
                        Emulator.getProcessor().cpu.gpr[2] = info.uid;
                        if (log.isDebugEnabled()) log.debug("hleIoOpen assigned uid = 0x" + Integer.toHexString(info.uid));
                    }
                }
            } else {
                Emulator.getProcessor().cpu.gpr[2] = -1;
            }
        } catch(FileNotFoundException e) {
            // To be expected under mode="r" and file doesn't exist
            if (log.isDebugEnabled()) log.debug("hleIoOpen - file not found (ok to ignore this message, debug purpose only)");
            Emulator.getProcessor().cpu.gpr[2] = ERROR_FILE_NOT_FOUND;
        }

        State.fileLogger.logIoOpen(Emulator.getProcessor().cpu.gpr[2],
                filename_addr, filename, flags, permissions, mode);

        if (async) {
            int result = Emulator.getProcessor().cpu.gpr[2];
            if (info == null) {
                log.debug("sceIoOpenAsync - file not found (ok to ignore this message, debug purpose only)");
                // For async we still need to make and return a file handle even if we couldn't open the file,
                // this is so the game can query on the handle (wait/async stat/io callback).
                info = new IoInfo(readStringZ(filename_addr), null, null, flags, permissions);
                Emulator.getProcessor().cpu.gpr[2] = info.uid;
            }

            startIoAsync(info, result);
        }
    }

    private void hleIoClose(int uid) {
		CpuState cpu = Emulator.getProcessor().cpu;

		if (log.isDebugEnabled()) log.debug("sceIoClose - uid " + Integer.toHexString(uid));

        try {
            SceUidManager.checkUidPurpose(uid, "IOFileManager-File", true);
            IoInfo info = filelist.remove(uid);
            if (info == null) {
                if (uid != 1 && uid != 2) // ignore stdout and stderr
                    log.warn("sceIoClose - unknown uid " + Integer.toHexString(uid));
                cpu.gpr[2] = ERROR_BAD_FILE_DESCRIPTOR;
            } else {
                if (info.readOnlyFile != null) {
                    // Can be just closing an empty handle, because hleIoOpen(async==true)
                    // generates a dummy IoInfo when the file could not be opened.
                    info.readOnlyFile.close();
                }
                SceUidManager.releaseUid(info.uid, "IOFileManager-File");
                triggerAsyncThread(info);
                info.result = 0;
                cpu.gpr[2] = 0;
            }
        } catch(IOException e) {
            log.error("pspiofilemgr - error closing file: " + e.getMessage());
            e.printStackTrace();
            cpu.gpr[2] = -1;
        }

        State.fileLogger.logIoClose(cpu.gpr[2], uid);
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
            if (log.isDebugEnabled()) log.debug("hleIoWrite(uid=" + Integer.toHexString(uid) + ",data=0x" + Integer.toHexString(data_addr) + ",size=0x" + Integer.toHexString(size) + ") async=" + async);

            try {
                SceUidManager.checkUidPurpose(uid, "IOFileManager-File", true);
                info = filelist.get(uid);
                if (info == null) {
                    log.warn("hleIoWrite - unknown uid " + Integer.toHexString(uid));
                    result = ERROR_BAD_FILE_DESCRIPTOR;
                } else if (info.asyncPending) {
                    log.warn("hleIoWrite - uid " + Integer.toHexString(uid) + " PSP_ERROR_ASYNC_BUSY");
                    result = ERROR_ASYNC_BUSY;
                } else if ((data_addr < MemoryMap.START_RAM ) && (data_addr + size > MemoryMap.END_RAM)) {
                    log.warn("hleIoWrite - uid " + Integer.toHexString(uid)
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
        if (log.isDebugEnabled()) log.debug("hleIoRead(uid=" + Integer.toHexString(uid) + ",data=0x" + Integer.toHexString(data_addr) + ",size=0x" + Integer.toHexString(size) + ") async=" + async);
        IoInfo info = null;
        int result;

        if (uid == 3) { // stdin
            log.warn("UNIMPLEMENTED:hleIoRead uid = stdin");
            result = 0;
        } else {
            try {
                SceUidManager.checkUidPurpose(uid, "IOFileManager-File", true);
                info = filelist.get(uid);
                if (info == null) {
                    log.warn("hleIoRead - unknown uid " + Integer.toHexString(uid));
                    result = ERROR_BAD_FILE_DESCRIPTOR;
                } else if (info.asyncPending) {
                    log.warn("hleIoRead - uid " + Integer.toHexString(uid) + " PSP_ERROR_ASYNC_BUSY");
                    result = ERROR_ASYNC_BUSY;
                } else if ((data_addr < MemoryMap.START_RAM ) && (data_addr + size > MemoryMap.END_RAM)) {
                    log.warn("hleIoRead - uid " + Integer.toHexString(uid)
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
                        log.debug("hleIoRead - clamping size old=" + oldSize + " new=" + size
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
                log.error("hleIoRead: Check other console for exception details. Press Run to continue.");
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
            log.error("seek - can't seek on stdio uid " + Integer.toHexString(uid));
            result = -1;
        } else {
            try {
                SceUidManager.checkUidPurpose(uid, "IOFileManager-File", true);
                info = filelist.get(uid);
                if (info == null) {
                    log.warn("seek - unknown uid " + Integer.toHexString(uid));
                    result = ERROR_BAD_FILE_DESCRIPTOR;
                } else if (info.asyncPending) {
                    log.warn("seek - uid " + Integer.toHexString(uid) + " PSP_ERROR_ASYNC_BUSY");
                    result = ERROR_ASYNC_BUSY;
                } else {
                	if (info.sectorBlockMode) {
                		// In sectorBlockMode, the offset is a sector number
                		offset *= UmdIsoFile.sectorLength;
                	}

                	switch(whence) {
                    case PSP_SEEK_SET:
                        if (offset < 0) {
                            log.warn("SEEK_SET UID " + Integer.toHexString(uid) + " filename:'" + info.filename + "' offset=0x" + Long.toHexString(offset) + " (less than 0!)");
                            result = ERROR_INVALID_ARGUMENT;
                            State.fileLogger.logIoSeek64(ERROR_INVALID_ARGUMENT, uid, offset, whence);
                            return;
                        }
						info.position = offset;

						if (offset < info.readOnlyFile.length())
						    info.readOnlyFile.seek(offset);
                        break;
                    case PSP_SEEK_CUR:
                        if (info.position + offset < 0) {
                            log.warn("SEEK_CUR UID " + Integer.toHexString(uid) + " filename:'" + info.filename + "' newposition=0x" + Long.toHexString(info.position + offset) + " (less than 0!)");
                            result = ERROR_INVALID_ARGUMENT;
                            State.fileLogger.logIoSeek64(ERROR_INVALID_ARGUMENT, uid, offset, whence);
                            return;
                        }
						info.position += offset;

						if (info.position < info.readOnlyFile.length())
						    info.readOnlyFile.seek(info.position);
                        break;
                    case PSP_SEEK_END:
                        if (info.readOnlyFile.length() + offset < 0) {
                            log.warn("SEEK_END UID " + Integer.toHexString(uid) + " filename:'" + info.filename + "' newposition=0x" + Long.toHexString(info.position + offset) + " (less than 0!)");
                            result = ERROR_INVALID_ARGUMENT;
                            State.fileLogger.logIoSeek64(ERROR_INVALID_ARGUMENT, uid, offset, whence);
                            return;
                        }
						info.position = info.readOnlyFile.length() + offset;

						if (info.position < info.readOnlyFile.length())
						    info.readOnlyFile.seek(info.position);
                        break;
                    default:
                        log.error("seek - unhandled whence " + whence);
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

        if (log.isDebugEnabled()) {
            log.debug("hleIoIoctl(uid=" + Integer.toHexString(uid) + ",cmd=0x" + Integer.toHexString(cmd) + ",indata=0x" + Integer.toHexString(indata_addr) + ",inlen=" + inlen + ",outdata=0x" + Integer.toHexString(outdata_addr) + ",outlen=" + outlen + ",async=" + async + ")");

            if (mem.isAddressGood(indata_addr)) {
                for (int i = 0; i < inlen; i += 4) {
                    log.debug("hleIoIoctl indata[" + (i / 4) + "]=0x" + Integer.toHexString(mem.read32(indata_addr + i)));
                }
            }
            if (mem.isAddressGood(outdata_addr)) {
                for (int i = 0; i < outlen; i += 4) {
                    log.debug("hleIoIoctl outdata[" + (i / 4) + "]=0x" + Integer.toHexString(mem.read32(outdata_addr + i)));
                }
            }
        }

        SceUidManager.checkUidPurpose(uid, "IOFileManager-File", true);
        info = filelist.get(uid);
        if (info == null) {
            log.warn("hleIoIoctl - unknown uid " + Integer.toHexString(uid));
            result = ERROR_BAD_FILE_DESCRIPTOR;
        } else if (info.asyncPending) {
            // Can't execute another operation until the previous one completed
            log.warn("hleIoIoctl - uid " + Integer.toHexString(uid) + " PSP_ERROR_ASYNC_BUSY");
            result = ERROR_ASYNC_BUSY;
        } else {
            switch (cmd) {
                // UMD file seek set.
                case 0x01010005: {
                    if (mem.isAddressGood(indata_addr) && inlen >= 4) {
                        if (info.isUmdFile()) {
                            try {
                                int offset = mem.read32(indata_addr);
                                log.debug("hleIoIoctl umd file seek set " + offset);
                                info.readOnlyFile.seek(offset);
                                info.position = offset;
                                result = 0;
                            } catch (IOException e) {
                                // Should never happen?
                                log.warn("hleIoIoctl cmd=0x01010005 exception: " + e.getMessage());
                                result = -1;
                            }
                        } else {
                            log.warn("hleIoIoctl cmd=0x01010005 only allowed on UMD files");
                        }
                    } else {
                        log.warn("hleIoIoctl cmd=0x01010005 " + String.format("0x%08X %d", indata_addr, inlen) + " unsupported parameters");
                        result = ERROR_INVALID_ARGUMENT;
                    }
                    break;
                }
                //Get UMD file pointer.
                case 0x01020004: {
                    if (mem.isAddressGood(outdata_addr) && outlen >= 4) {
                        if (info.isUmdFile()) {
                            try {
                                int fPointer = (int) info.readOnlyFile.getFilePointer();
                                mem.write32(outdata_addr, fPointer);
                                log.debug("hleIoIoctl umd file get file pointer " + fPointer);
                                result = 0;
                            } catch (IOException e) {
                                log.warn("hleIoIoctl cmd=0x01020004 exception: " + e.getMessage());
                            }
                        } else {
                            log.warn("hleIoIoctl cmd=0x01020004 only allowed on UMD files");
                        }
                    } else {
                        log.warn("hleIoIoctl cmd=0x01020004 " + String.format("0x%08X %d", outdata_addr, outlen) + " unsupported parameters");
                        result = ERROR_INVALID_ARGUMENT;
                    }
                    break;
                }
                // Get UMD file start sector.
                case 0x01020006: {
                    if (mem.isAddressGood(outdata_addr) && outlen >= 4) {
                        int startSector = 0;
                        if (info.isUmdFile() && info.readOnlyFile instanceof UmdIsoFile) {
                            UmdIsoFile file = (UmdIsoFile) info.readOnlyFile;
                            startSector = file.getStartSector();
                            log.debug("hleIoIoctl umd file get start sector " + startSector);
                            mem.write32(outdata_addr, startSector);
                            result = 0;
                        } else {
                            log.warn("hleIoIoctl cmd=0x01020006 only allowed on UMD files and only implemented for UmdIsoFile");
                        }
                    } else {
                        log.warn("hleIoIoctl cmd=0x01020006 " + String.format("0x%08X %d", outdata_addr, outlen) + " unsupported parameters");
                        result = ERROR_INVALID_ARGUMENT;
                    }
                    break;
                }
                // Get UMD file length in bytes.
                case 0x01020007: {
                    if (mem.isAddressGood(outdata_addr) && outlen >= 8) {
                        if (info.isUmdFile()) {
                            try {
                                long length = info.readOnlyFile.length();
                                mem.write64(outdata_addr, length);
                                log.debug("hleIoIoctl get file size " + length);
                                result = 0;
                            } catch (IOException e) {
                                // Should never happen?
                                log.warn("hleIoIoctl cmd=0x01020007 exception: " + e.getMessage());
                            }
                        } else {
                            log.warn("hleIoIoctl cmd=0x01020007 only allowed on UMD files");
                        }
                    } else {
                        log.warn("hleIoIoctl cmd=0x01020007 " + String.format("0x%08X %d", outdata_addr, outlen) + " unsupported parameters");
                        result = ERROR_INVALID_ARGUMENT;
                    }
                    break;
                }
                // UMD file seek whence.
                case 0x01F100A6: {
                    if (mem.isAddressGood(indata_addr) && inlen >= 16) {
                        if (info.isUmdFile()) {
                            try {
                                long offset = mem.read64(indata_addr);
                                int whence = mem.read32(indata_addr + 12);
                                if (log.isDebugEnabled()) {
                                    log.debug("hleIoIoctl umd file seek offset " + offset + ", whence " + whence);
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
                                        log.error("hleIoIoctl - unhandled whence " + whence);
                                        result = -1;
                                        break;
                                    }
                                }
                            } catch (IOException e) {
                                // Should never happen?
                                log.warn("hleIoIoctl cmd=0x01F100A6 exception: " + e.getMessage());
                                result = -1;
                            }
                        } else {
                            log.warn("hleIoIoctl cmd=0x01F100A6 only allowed on UMD files");
                        }
                    } else {
                        log.warn("hleIoIoctl cmd=0x01F100A6 " + String.format("0x%08X %d", indata_addr, inlen) + " unsupported parameters");
                        result = ERROR_INVALID_ARGUMENT;
                    }
                    break;
                }
                // Define decryption key (Kirk / AES-128?).
                case 0x04100001: {
                    if (mem.isAddressGood(indata_addr) && inlen == 16) {
                        String keyHex = "";
                        for (int i = 0; i < inlen; i++) {
                            AES128Key[i] = (byte) mem.read8(indata_addr + i);
                            keyHex += String.format("%02x", AES128Key[i] & 0xFF);
                        }

                        if (log.isDebugEnabled()) {
                            log.debug("hleIoIoctl get AES key " + keyHex);
                        }

                        if (pgdFileConnector == null) {
                            pgdFileConnector = new PGDFileConnector();
                        }
                        info.readOnlyFile = pgdFileConnector.decryptPGDFile(info.filename, info.readOnlyFile, keyHex);

                        result = 0;
                    } else {
                        log.warn("hleIoIoctl cmd=0x04100001 " + String.format("0x%08X %d", indata_addr, inlen) + " unsupported parameters");
                        result = ERROR_INVALID_ARGUMENT;
                    }
                    break;
                }

                default: {
                    log.warn("hleIoIoctl " + String.format("0x%08X", cmd) + " unknown command");
                    break;
                }
            }
        }
        updateResult(Emulator.getProcessor().cpu, info, result, async, false);
        State.fileLogger.logIoIoctl(Emulator.getProcessor().cpu.gpr[2], uid, cmd, indata_addr, inlen, outdata_addr, outlen);
    }

	public void sceIoPollAsync(Processor processor) {
		CpuState cpu = processor.cpu;

		int uid = cpu.gpr[4];
		int res_addr = cpu.gpr[5];

		if (log.isDebugEnabled()) log.debug("sceIoPollAsync redirecting to hleIoGetAsyncStat");
        hleIoGetAsyncStat(uid, res_addr, false, false);
	}

	public void sceIoWaitAsync(Processor processor) {
		CpuState cpu = processor.cpu;

		int uid = cpu.gpr[4];
		int res_addr = cpu.gpr[5];

		if (log.isDebugEnabled()) log.debug("sceIoWaitAsync redirecting to hleIoGetAsyncStat");
        hleIoGetAsyncStat(uid, res_addr, true, false);
	}

	public void sceIoWaitAsyncCB(Processor processor) {
		CpuState cpu = processor.cpu;

		int uid = cpu.gpr[4];
		int res_addr = cpu.gpr[5];

		if (log.isDebugEnabled()) log.debug("sceIoWaitAsyncCB redirecting to hleIoGetAsyncStat");
        hleIoGetAsyncStat(uid, res_addr, true, true);
	}

	public void sceIoGetAsyncStat(Processor processor) {
		CpuState cpu = processor.cpu;

		int uid = cpu.gpr[4];
		int poll = cpu.gpr[5];
		int res_addr = cpu.gpr[6];

		if (log.isDebugEnabled()) log.debug("sceIoGetAsyncStat poll=0x" + Integer.toHexString(poll) + " redirecting to hleIoGetAsyncStat");
        hleIoGetAsyncStat(uid, res_addr, (poll == 0), false);
	}

	public void sceIoChangeAsyncPriority(Processor processor) {
		CpuState cpu = processor.cpu;

		int uid = cpu.gpr[4];
		int priority = cpu.gpr[5];

		SceUidManager.checkUidPurpose(uid, "IOFileManager-File", true);

        if (priority < 0) {
        	cpu.gpr[2] = SceKernelErrors.ERROR_ILLEGAL_PRIORITY;
        } else if (uid == -1) {
        	defaultAsyncPriority = priority;
            cpu.gpr[2] = 0;
        } else {
            IoInfo info = filelist.get(uid);
            if (info != null && info.asyncThread != null) {
            	if (log.isDebugEnabled()) {
            		log.info(String.format("sceIoChangeAsyncPriority changing priority of async thread from fd=%x to %d", info.uid, priority));
            	}
            	info.asyncThread.currentPriority = priority;
                cpu.gpr[2] = 0;
                Modules.ThreadManForUserModule.hleRescheduleCurrentThread();
            } else {
                log.warn("sceIoChangeAsyncPriority invalid fd=" + uid);
                cpu.gpr[2] = -1;
            }
        }
	}

	public void sceIoSetAsyncCallback(Processor processor) {
		CpuState cpu = processor.cpu;

		int uid = cpu.gpr[4];
		int cbid = cpu.gpr[5];
		int notifyArg = cpu.gpr[6];

		if (log.isDebugEnabled()) log.debug("sceIoSetAsyncCallback - uid " + Integer.toHexString(uid) + " cbid " + Integer.toHexString(cbid) + " arg 0x" + Integer.toHexString(notifyArg));
        SceUidManager.checkUidPurpose(uid, "IOFileManager-File", true);
        IoInfo info = filelist.get(uid);
        if (info == null) {
            log.warn("sceIoSetAsyncCallback - unknown uid " + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_BAD_FILE_DESCRIPTOR;
        } else {
            if (Modules.ThreadManForUserModule.hleKernelRegisterCallback(SceKernelThreadInfo.THREAD_CALLBACK_IO, cbid)) {
                info.cbid = cbid;
                info.notifyArg = notifyArg;
                triggerAsyncThread(info);
                cpu.gpr[2] = 0;
            } else {
                log.warn("sceIoSetAsyncCallback - not a callback uid " + Integer.toHexString(uid));
                cpu.gpr[2] = -1;
            }
        }
	}

	public void sceIoClose(Processor processor) {
		CpuState cpu = processor.cpu;

		int uid = cpu.gpr[4];

		hleIoClose(uid);
	}

	public void sceIoCloseAsync(Processor processor) {
		CpuState cpu = processor.cpu;

		int uid = cpu.gpr[4];

		if (log.isDebugEnabled()) log.debug("sceIoCloseAsync - uid " + Integer.toHexString(uid));

        SceUidManager.checkUidPurpose(uid, "IOFileManager-File", true);
        IoInfo info = filelist.get(uid);
        if (info != null) {
            if (info.asyncPending) {
                log.warn("sceIoCloseAsync - uid " + Integer.toHexString(uid) + " PSP_ERROR_ASYNC_BUSY");
                cpu.gpr[2] = ERROR_ASYNC_BUSY;
            } else {
                info.closePending = true;
                updateResult(cpu, info, 0, true, false);
            }
        } else {
            cpu.gpr[2] = ERROR_BAD_FILE_DESCRIPTOR;
        }
	}

	public void sceIoOpen(Processor processor) {
		CpuState cpu = processor.cpu;

		int filename_addr = cpu.gpr[4];
		int flags = cpu.gpr[5];
		int permissions = cpu.gpr[6];

		if (log.isDebugEnabled()) log.debug("sceIoOpen redirecting to hleIoOpen");
        hleIoOpen(filename_addr, flags, permissions, false);
	}

	public void sceIoOpenAsync(Processor processor) {
		CpuState cpu = processor.cpu;

		int filename_addr = cpu.gpr[4];
		int flags = cpu.gpr[5];
		int permissions = cpu.gpr[6];

		if (log.isDebugEnabled()) log.debug("sceIoOpenAsync redirecting to hleIoOpen");
        hleIoOpen(filename_addr, flags, permissions, true);
	}

	public void sceIoRead(Processor processor) {
		CpuState cpu = processor.cpu;

		int uid = cpu.gpr[4];
		int data_addr = cpu.gpr[5];
		int size = cpu.gpr[6];

		hleIoRead(uid, data_addr, size, false);
	}

	public void sceIoReadAsync(Processor processor) {
		CpuState cpu = processor.cpu;

		int uid = cpu.gpr[4];
		int data_addr = cpu.gpr[5];
		int size = cpu.gpr[6];

		hleIoRead(uid, data_addr, size, true);
	}

	public void sceIoWrite(Processor processor) {
		CpuState cpu = processor.cpu;

		int uid = cpu.gpr[4];
		int data_addr = cpu.gpr[5];
		int size = cpu.gpr[6];

		hleIoWrite(uid, data_addr, size, false);
	}

	public void sceIoWriteAsync(Processor processor) {
        CpuState cpu = processor.cpu;

		int uid = cpu.gpr[4];
		int data_addr = cpu.gpr[5];
		int size = cpu.gpr[6];

		hleIoWrite(uid, data_addr, size, true);
	}

	public void sceIoLseek(Processor processor) {
		CpuState cpu = processor.cpu;

		int uid = cpu.gpr[4];
		long offset = ((long)cpu.gpr[6] & 0xFFFFFFFFL) | ((long)cpu.gpr[7] << 32);
        int whence = cpu.gpr[8];

		if (log.isDebugEnabled()) log.debug("sceIoLseek - uid " + Integer.toHexString(uid) + " offset " + offset + " (hex=0x" + Long.toHexString(offset) + ") whence " + getWhenceName(whence));
        hleIoLseek(uid, offset, whence, true, false);
	}

	public void sceIoLseekAsync(Processor processor) {
		CpuState cpu = processor.cpu;

		int uid = cpu.gpr[4];
		long offset = ((long)cpu.gpr[6] & 0xFFFFFFFFL) | ((long)cpu.gpr[7] << 32);
        int whence = cpu.gpr[8];

        if (log.isDebugEnabled()) log.debug("sceIoLseekAsync - uid " + Integer.toHexString(uid) + " offset " + offset + " (hex=0x" + Long.toHexString(offset) + ") whence " + getWhenceName(whence));
        hleIoLseek(uid, offset, whence, true, true);
	}

	public void sceIoLseek32(Processor processor) {
		CpuState cpu = processor.cpu;

		int uid = cpu.gpr[4];
		int offset = cpu.gpr[5];
        int whence = cpu.gpr[6];

		if (log.isDebugEnabled()) log.debug("sceIoLseek32 - uid " + Integer.toHexString(uid) + " offset " + offset + " (hex=0x" + Integer.toHexString(offset) + ") whence " + getWhenceName(whence));
        hleIoLseek(uid, (long)offset, whence, false, false);
	}

	public void sceIoLseek32Async(Processor processor) {
		CpuState cpu = processor.cpu;

		int uid = cpu.gpr[4];
		int offset = cpu.gpr[5];
        int whence = cpu.gpr[6];

		if (log.isDebugEnabled()) log.debug("sceIoLseek32Async - uid " + Integer.toHexString(uid) + " offset " + offset + " (hex=0x" + Integer.toHexString(offset) + ") whence " + getWhenceName(whence));
        hleIoLseek(uid, (long)offset, whence, false, true);
	}

	public void sceIoIoctl(Processor processor) {
		CpuState cpu = processor.cpu;

		int uid = cpu.gpr[4];
		int cmd = cpu.gpr[5];
		int indata_addr = cpu.gpr[6];
		int inlen = cpu.gpr[7];
		int outdata_addr = cpu.gpr[8];
		int outlen = cpu.gpr[9];

		hleIoIoctl(uid, cmd, indata_addr, inlen, outdata_addr, outlen, false);
	}

	public void sceIoIoctlAsync(Processor processor) {
		CpuState cpu = processor.cpu;

		int uid = cpu.gpr[4];
		int cmd = cpu.gpr[5];
		int indata_addr = cpu.gpr[6];
		int inlen = cpu.gpr[7];
		int outdata_addr = cpu.gpr[8];
		int outlen = cpu.gpr[9];

		hleIoIoctl(uid, cmd, indata_addr, inlen, outdata_addr, outlen, true);
	}

	public void sceIoDopen(Processor processor) {
		CpuState cpu = processor.cpu;

		int dirname_addr = cpu.gpr[4];

		String dirname = readStringZ(dirname_addr);
        if (log.isDebugEnabled()) log.debug("sceIoDopen dirname = " + dirname);

        String pcfilename = getDeviceFilePath(dirname);
        if (pcfilename != null) {
            if (isUmdPath(pcfilename)) {
                // Files in our iso virtual file system
                String isofilename = trimUmdPrefix(pcfilename);
                if (log.isDebugEnabled()) log.debug("sceIoDopen - isofilename = " + isofilename);

                if (iso == null) { // check umd is mounted
                    log.error("sceIoDopen - no umd mounted");
                    cpu.gpr[2] = ERROR_DEVICE_NOT_FOUND;
                } else if (!Modules.sceUmdUserModule.isUmdActivated()) { // check umd is activated
                    log.warn("sceIoDopen - umd mounted but not activated");
                    cpu.gpr[2] = ERROR_NO_SUCH_DEVICE;
                } else {
                    try {
                        if (iso.isDirectory(isofilename)) {
                            String[] filenames = iso.listDirectory(isofilename);
                            IoDirInfo info = new IoDirInfo(pcfilename, filenames);
                            cpu.gpr[2] = info.uid;
                        } else {
                            log.warn("sceIoDopen '" + isofilename + "' not a umd directory!");
                            cpu.gpr[2] = -1;
                        }
                    } catch(FileNotFoundException e) {
                        log.warn("sceIoDopen - '" + isofilename + "' umd file not found");
                        cpu.gpr[2] = -1;
                    } catch(IOException e) {
                        log.warn("sceIoDopen - umd io error: " + e.getMessage());
                        cpu.gpr[2] = -1;
                    }
                }
            } else if (dirname.startsWith("/") && dirname.indexOf(":") != -1) {
                log.warn("sceIoDopen apps running outside of ms0 dir are not fully supported, relative child paths should still work");
                cpu.gpr[2] = -1;
            } else {
                // Regular apps run from inside mstick dir or absolute path given
                if (log.isDebugEnabled()) log.debug("sceIoDopen - pcfilename = " + pcfilename);
                File f = new File(pcfilename);
                if (f.isDirectory()) {
                    IoDirInfo info = new IoDirInfo(pcfilename, f.list());
                    cpu.gpr[2] = info.uid;
                } else {
                    log.warn("sceIoDopen '" + pcfilename + "' not a directory! (could be missing)");
                    cpu.gpr[2] = ERROR_FILE_NOT_FOUND;
                }
            }
        } else {
            cpu.gpr[2] = -1;
        }
        State.fileLogger.logIoDopen(Emulator.getProcessor().cpu.gpr[2], dirname_addr, dirname);
	}

	public void sceIoDread(Processor processor) {
		CpuState cpu = processor.cpu;

		int uid = cpu.gpr[4];
		int dirent_addr = cpu.gpr[5];

		SceUidManager.checkUidPurpose(uid, "IOFileManager-Directory", true);
        IoDirInfo info = dirlist.get(uid);
        if (info == null) {
            log.warn("sceIoDread unknown uid " + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_BAD_FILE_DESCRIPTOR;
        } else if (info.hasNext()) {
            String filename = info.next();

            SceIoStat stat = stat(info.path + "/" + filename);
            if (stat != null) {
                SceIoDirent dirent = new SceIoDirent(stat, filename);
                dirent.write(Memory.getInstance(), dirent_addr);

                if ((stat.attr & 0x10) == 0x10) {
                    log.debug("sceIoDread uid=" + Integer.toHexString(uid)
                        + " #" + info.printableposition
                        + " dir='" + info.path
                        + "', dir='" + filename + "'");
                } else {
                    log.debug("sceIoDread uid=" + Integer.toHexString(uid)
                        + " #" + info.printableposition
                        + " dir='" + info.path
                        + "', file='" + filename + "'");
                }

                cpu.gpr[2] = 1;
            } else {
                log.warn("sceIoDread uid=" + Integer.toHexString(uid) + " stat failed (" + info.path + "/" + filename + ")");
                cpu.gpr[2] = -1;
            }
        } else {
            log.debug("sceIoDread uid=" + Integer.toHexString(uid) + " no more files");
            cpu.gpr[2] = 0;
        }
        State.fileLogger.logIoDread(cpu.gpr[2], uid, dirent_addr);
	}

	public void sceIoDclose(Processor processor) {
		CpuState cpu = processor.cpu;

		int uid = cpu.gpr[4];

		if (log.isDebugEnabled()) log.debug("sceIoDclose - uid = " + Integer.toHexString(uid));

        SceUidManager.checkUidPurpose(uid, "IOFileManager-Directory", true);
        IoDirInfo info = dirlist.remove(uid);
        if (info == null) {
            log.warn("sceIoDclose - unknown uid " + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_BAD_FILE_DESCRIPTOR;
        } else {
            SceUidManager.releaseUid(info.uid, "IOFileManager-Directory");
            cpu.gpr[2] = 0;
        }

        State.fileLogger.logIoDclose(cpu.gpr[2], uid);
	}

	public void sceIoRemove(Processor processor) {
		CpuState cpu = processor.cpu;

		int file_addr = cpu.gpr[4];

		String filename = readStringZ(file_addr);
        if (log.isDebugEnabled()) log.debug("sceIoRemove - file = " + filename);

        String pcfilename = getDeviceFilePath(filename);

        if (pcfilename != null) {
            if (isUmdPath(pcfilename)) {
                cpu.gpr[2] = -1;
            } else {
                File file = new File(pcfilename);
                if (file.delete()) {
                    cpu.gpr[2] = 0;
                } else {
                    cpu.gpr[2] = -1;
                }
            }
        } else {
            cpu.gpr[2] = -1;
        }

        State.fileLogger.logIoRemove(cpu.gpr[2], file_addr, filename);
	}

	public void sceIoMkdir(Processor processor) {
		CpuState cpu = processor.cpu;

		int dir_addr = cpu.gpr[4];
		int permissions = cpu.gpr[5];

		String dir = readStringZ(dir_addr);
        if (log.isDebugEnabled()) log.debug("sceIoMkdir dir = " + dir);

        String pcfilename = getDeviceFilePath(dir);
        if (pcfilename != null) {
            File f = new File(pcfilename);
            f.mkdir();
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = -1;
        }

        State.fileLogger.logIoMkdir(cpu.gpr[2], dir_addr, dir, permissions);
	}

	public void sceIoRmdir(Processor processor) {
		CpuState cpu = processor.cpu;

		int dir_addr = cpu.gpr[4];

		String dir = readStringZ(dir_addr);
        if (log.isDebugEnabled()) log.debug("sceIoRmdir dir = " + dir);

        String pcfilename = getDeviceFilePath(dir);
        if (pcfilename != null) {
            File f = new File(pcfilename);
            f.delete();
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = -1;
        }

        State.fileLogger.logIoRmdir(cpu.gpr[2], dir_addr, dir);
	}

	public void sceIoChdir(Processor processor) {
		CpuState cpu = processor.cpu;

		int path_addr = cpu.gpr[4];

		String path = readStringZ(path_addr);
        if (log.isDebugEnabled()) log.debug("sceIoChdir path = " + path);

        if (path.equals("..")) {
            int index = filepath.lastIndexOf("/");
            if (index != -1)
                filepath = filepath.substring(0, index);

            log.info("pspiofilemgr - filepath " + filepath + " (going up one level)");
            cpu.gpr[2] = 0;
        } else {
            String pcfilename = getDeviceFilePath(path);
            if (pcfilename != null) {
                filepath = pcfilename;
                log.info("pspiofilemgr - filepath " + filepath);
                cpu.gpr[2] = 0;
            } else {
                cpu.gpr[2] = -1;
            }
        }
        State.fileLogger.logIoChdir(cpu.gpr[2], path_addr, path);
	}

	public void sceIoSync(Processor processor) {
		CpuState cpu = processor.cpu;

		int device_addr = cpu.gpr[4];
		int unknown = cpu.gpr[5];

		String device = readStringZ(device_addr);
        if (log.isDebugEnabled()) log.debug("IGNORING:sceIoSync(device='" + device + "',unknown=0x" + Integer.toHexString(unknown) + ")");
        State.fileLogger.logIoSync(0, device_addr, device, unknown);
        cpu.gpr[2] = 0;
	}

	public void sceIoGetstat(Processor processor) {
		CpuState cpu = processor.cpu;

		int file_addr = cpu.gpr[4];
		int stat_addr = cpu.gpr[5];

		String filename = readStringZ(file_addr);
        if (log.isDebugEnabled()) log.debug("sceIoGetstat - file = " + filename + " stat = " + Integer.toHexString(stat_addr));

        String pcfilename = getDeviceFilePath(filename);
        SceIoStat stat = stat(pcfilename);
        if (stat != null) {
            stat.write(Memory.getInstance(), stat_addr);
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = ERROR_FILE_NOT_FOUND;
        }

        State.fileLogger.logIoGetStat(cpu.gpr[2], file_addr, filename, stat_addr);
	}

	public void sceIoChstat(Processor processor) {
		CpuState cpu = processor.cpu;

		int file_addr = cpu.gpr[4];
		int stat_addr = cpu.gpr[5];
		int bits = cpu.gpr[6];

		String filename = readStringZ(file_addr);
        if (log.isDebugEnabled()) log.debug("sceIoChstat - file = " + filename + ", bits=0x" + Integer.toHexString(bits));

        String pcfilename = getDeviceFilePath(filename);

        if (pcfilename != null) {
            if (isUmdPath(pcfilename)) {
                cpu.gpr[2] = -1;
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
                	cpu.gpr[2] = 0;
                } else {
                	cpu.gpr[2] = -1;
                }
            }
        } else {
            cpu.gpr[2] = -1;
        }
        State.fileLogger.logIoChstat(cpu.gpr[2], file_addr, filename, stat_addr, bits);
	}

	public void sceIoRename(Processor processor) {
		CpuState cpu = processor.cpu;

		int file_addr = cpu.gpr[4];
        int new_file_addr = cpu.gpr[4];

		String filename = readStringZ(file_addr);
        String newfilename = readStringZ(new_file_addr);
        if (log.isDebugEnabled()) log.debug("sceIoRename - file = " + filename + ", new_file = " + newfilename);

        String pcfilename = getDeviceFilePath(filename);
        String newpcfilename = getDeviceFilePath(newfilename);

        if (pcfilename != null) {
            if (isUmdPath(pcfilename)) {
                cpu.gpr[2] = -1;
            } else {
                File file = new File(pcfilename);
                File newfile = new File(newpcfilename);
                if (file.renameTo(newfile)) {
                    cpu.gpr[2] = 0;
                } else {
                    cpu.gpr[2] = -1;
                }
            }
        } else {
            cpu.gpr[2] = -1;
        }

        State.fileLogger.logIoRename(cpu.gpr[2], file_addr, filename, new_file_addr, newfilename);
	}

	public void sceIoDevctl(Processor processor) {
		CpuState cpu = processor.cpu;
		Memory mem = Processor.memory;

		int device_addr = cpu.gpr[4];
		int cmd = cpu.gpr[5];
		int indata_addr = cpu.gpr[6];
		int inlen = cpu.gpr[7];
		int outdata_addr = cpu.gpr[8];
		int outlen = cpu.gpr[9];

		String device = readStringZ(device_addr);
        if (log.isDebugEnabled()) {
            log.debug("sceIoDevctl(device='" + device
                + "',cmd=0x" + Integer.toHexString(cmd)
                + ",indata=0x" + Integer.toHexString(indata_addr)
                + ",inlen=" + inlen
                + ",outdata=0x" + Integer.toHexString(outdata_addr)
                + ",outlen=" + outlen + ")");

            if (mem.isAddressGood(indata_addr)) {
                for (int i = 0; i < inlen; i += 4) {
                    log.debug("sceIoDevctl indata[" + (i / 4) + "]=0x" + Integer.toHexString(mem.read32(indata_addr + i)));
                }
            }
            if (mem.isAddressGood(outdata_addr)) {
                for (int i = 0; i < outlen; i += 4) {
                    log.debug("sceIoDevctl outdata[" + (i / 4) + "]=0x" + Integer.toHexString(mem.read32(outdata_addr + i)));
                }
            }
        }

        switch (cmd) {
            // Get UMD disc type.
            case 0x01F20001: {
                log.debug("sceIoDevctl " + String.format("0x%08X", cmd) + " get disc type");
                if (mem.isAddressGood(outdata_addr) && outlen >= 8) {
                    // 0 = No disc.
                    // 0x10 = Game disc.
                    // 0x20 = Video disc.
                    // 0x40 = Audio disc.
                    // 0x80 = Cleaning disc.
                    int result;
                    if (iso == null) {
                        result = 0;
                    } else {
                        result = 0x10;  // Always return game disc (if present).
                    }
                    mem.write32(outdata_addr + 4, result);
                    cpu.gpr[2] = 0;
                } else {
                    cpu.gpr[2] = -1;
                }
                break;
            }
            // Seek UMD disc (raw).
            case 0x01F100A4: {
                log.debug("sceIoDevctl " + String.format("0x%08X", cmd) + " seek UMD disc");
                if ((mem.isAddressGood(indata_addr) && inlen >= 4)) {
                    int sector = mem.read32(indata_addr);
                    cpu.gpr[2] = 0;
                } else {
                    cpu.gpr[2] = -1;
                }
                break;
            }
            // Prepare UMD data into cache.
            case 0x01F300A5: {
                log.debug("sceIoDevctl " + String.format("0x%08X", cmd) + " prepare UMD data to cache");
                if ((mem.isAddressGood(indata_addr) && inlen >= 4) && (mem.isAddressGood(outdata_addr) && outlen >= 4)) {
                    int sector = mem.read32(indata_addr + 4);  // First sector of data to read.
                    int sectorNum = mem.read32(indata_addr + 12);  // Length of data to read.

                    mem.write32(outdata_addr, 1); // Status (unitary index with unknown meaning).
                    cpu.gpr[2] = 0;
                } else {
                    cpu.gpr[2] = -1;
                }
                break;
            }
            // Register memorystick insert/eject callback (mscmhc0).
            case 0x02015804: {
                log.debug("sceIoDevctl register memorystick insert/eject callback (mscmhc0)");
                ThreadManForUser threadMan = Modules.ThreadManForUserModule;

                if (!device.equals("mscmhc0:")) {
                    cpu.gpr[2] = ERROR_UNSUPPORTED_OPERATION;
                } else if (mem.isAddressGood(indata_addr) && inlen == 4) {
                    int cbid = mem.read32(indata_addr);
                    if (threadMan.hleKernelRegisterCallback(SceKernelThreadInfo.THREAD_CALLBACK_MEMORYSTICK, cbid)) {
                        // Trigger callback immediately
                        threadMan.hleKernelNotifyCallback(SceKernelThreadInfo.THREAD_CALLBACK_MEMORYSTICK, MemoryStick.getState());
                        cpu.gpr[2] = 0; // Success
                    } else {
                        cpu.gpr[2] = ERROR_DEVCTL_BAD_PARAMS; // No such callback
                    }
                } else {
                    cpu.gpr[2] = -1; // Invalid parameters
                }
                break;
            }
            // Unregister memorystick insert/eject callback (mscmhc0).
            case 0x02015805:  {
                log.debug("sceIoDevctl unregister memorystick insert/eject callback (mscmhc0)");
                ThreadManForUser threadMan = Modules.ThreadManForUserModule;

                if (!device.equals("mscmhc0:")) {
                    cpu.gpr[2] = ERROR_UNSUPPORTED_OPERATION;
                } else if (mem.isAddressGood(indata_addr) && inlen == 4) {
                    int cbid = mem.read32(indata_addr);
                    if (threadMan.hleKernelUnRegisterCallback(SceKernelThreadInfo.THREAD_CALLBACK_MEMORYSTICK, cbid) != null) {
                        cpu.gpr[2] = 0; // Success
                    } else {
                        cpu.gpr[2] = ERROR_DEVCTL_BAD_PARAMS; // No such callback
                    }
                } else {
                    cpu.gpr[2] = -1; // Invalid parameters
                }
                break;
            }
            // Unknown (mscmhc0).
            case 0x02025801: {
                log.warn("sceIoDevctl " + String.format("0x%08X", cmd) + " unknown ms command (check fs type?)");

                if (!device.equals("mscmhc0:")) {
                    cpu.gpr[2] = ERROR_UNSUPPORTED_OPERATION;
                } else if (mem.isAddressGood(outdata_addr)) {
                    // 1 = not inserted
                    // 4 = inserted
                    mem.write32(outdata_addr, 4);
                    cpu.gpr[2] = 0;
                } else {
                    cpu.gpr[2] = -1;
                }
                break;
            }
            // Check if the device is assigned/inserted (mscmhc0).
            case 0x02025806: {
                log.debug("sceIoDevctl check ms inserted (mscmhc0)");

                if (!device.equals("mscmhc0:")) {
                    cpu.gpr[2] = ERROR_UNSUPPORTED_OPERATION;
                } else if (mem.isAddressGood(outdata_addr)) {
                    // 1 = inserted
                    // 2 = not inserted
                    mem.write32(outdata_addr, MemoryStick.getState());
                    cpu.gpr[2] = 0;
                } else {
                    cpu.gpr[2] = -1;
                }
                break;
            }
            // Register memorystick insert/eject callback (fatms0).
            case 0x02415821:  {
                log.debug("sceIoDevctl register memorystick insert/eject callback (fatms0)");
                ThreadManForUser threadMan = Modules.ThreadManForUserModule;

                if (!device.equals("fatms0:")) {
                    cpu.gpr[2] = ERROR_DEVCTL_BAD_PARAMS;
                } else if (mem.isAddressGood(indata_addr) && inlen == 4) {
                    int cbid = mem.read32(indata_addr);
                    threadMan.hleKernelRegisterCallback(SceKernelThreadInfo.THREAD_CALLBACK_MEMORYSTICK, cbid);
                    // Trigger callback immediately
                    threadMan.hleKernelNotifyCallback(SceKernelThreadInfo.THREAD_CALLBACK_MEMORYSTICK, MemoryStick.getState());
                    cpu.gpr[2] = 0;  // Success
                } else {
                    cpu.gpr[2] = -1; // Invalid parameters
                }
                break;
            }
            // Unregister memorystick insert/eject callback (fatms0).
            case 0x02415822:  {
                log.debug("sceIoDevctl unregister memorystick insert/eject callback (fatms0)");
                ThreadManForUser threadMan = Modules.ThreadManForUserModule;

                if (!device.equals("fatms0:")) {
                    cpu.gpr[2] = ERROR_DEVCTL_BAD_PARAMS;
                } else if (mem.isAddressGood(indata_addr) && inlen == 4) {
                    int cbid = mem.read32(indata_addr);
                    threadMan.hleKernelUnRegisterCallback(SceKernelThreadInfo.THREAD_CALLBACK_MEMORYSTICK, cbid);
                    cpu.gpr[2] = 0;  // Success
                } else {
                    cpu.gpr[2] = -1; // Invalid parameters
                }
                break;
            }
            // Set if the device is assigned/inserted or not (fatms0).
            case 0x02415823: {
                log.debug("sceIoDevctl set assigned device (fatms0)");

                if (!device.equals("fatms0:")) {
                    cpu.gpr[2] = ERROR_DEVCTL_BAD_PARAMS;
                } else if (mem.isAddressGood(indata_addr) && inlen >= 4) {
                    // 0 - Device is not assigned (callback not registered).
                    // 1 - Device is assigned (callback registered).
                    MemoryStick.setState(mem.read32(indata_addr));
                    cpu.gpr[2] = 0;
                } else {
                    cpu.gpr[2] = -1;
                }
                break;
            }
            // Check if the device is write protected (fatms0).
            case 0x02425824: {
                log.debug("sceIoDevctl check write protection (fatms0)");

                if (!device.equals("fatms0:")) {
                    cpu.gpr[2] = ERROR_DEVCTL_BAD_PARAMS;
                } else if (mem.isAddressGood(outdata_addr)) {
                    // 0 - Device is not protected.
                    // 1 - Device is protected.
                    mem.write32(outdata_addr, 0);
                    cpu.gpr[2] = 0;
                } else {
                    cpu.gpr[2] = -1;
                }
                break;
            }
            // Get MS capacity (fatms0).
            case 0x02425818: {
                int sectorSize = 0x200;
                int sectorCount = 0x08;
                int maxClusters = (int) ((MemoryStick.getFreeSize() * 95L / 100) / (sectorSize * sectorCount));
                int freeClusters = maxClusters;
                int maxSectors = 512;

                if (mem.isAddressGood(indata_addr) && inlen >= 4) {
                    int addr = mem.read32(indata_addr);
                    if (mem.isAddressGood(addr)) {
                        log.debug("sceIoDevctl refer ms free space");
                        mem.write32(addr, maxClusters);
                        mem.write32(addr + 4, freeClusters);
                        mem.write32(addr + 8, maxSectors);
                        mem.write32(addr + 12, sectorSize);
                        mem.write32(addr + 16, sectorCount);
                        cpu.gpr[2] = 0;
                    } else {
                        log.warn("sceIoDevctl 0x02425818 bad save address " + String.format("0x%08X", addr));
                        cpu.gpr[2] = -1;
                    }
                } else {
                    log.warn("sceIoDevctl 0x02425818 bad param address " + String.format("0x%08X", indata_addr) + " or size " + inlen);
                    cpu.gpr[2] = -1;
                }
                break;
            }
            // Check if the device is assigned/inserted (fatms0).
            case 0x02425823:{
                log.debug("sceIoDevctl check assigned device (fatms0)");

                if (!device.equals("fatms0:")) {
                    cpu.gpr[2] = ERROR_DEVCTL_BAD_PARAMS;
                } else if (mem.isAddressGood(outdata_addr) && outlen >= 4) {
                    // 0 - Device is not assigned (callback not registered).
                    // 1 - Device is assigned (callback registered).
                    mem.write32(outdata_addr, MemoryStick.getState());
                    cpu.gpr[2] = 0;
                } else {
                    cpu.gpr[2] = -1;
                }
                break;
            }
            // Register USB thread.
            case 0x03415001:{
                log.debug("sceIoDevctl register usb thread");

                if (mem.isAddressGood(indata_addr) && inlen >= 4) {
                    // Unknown params.
                    cpu.gpr[2] = 0;
                } else {
                    cpu.gpr[2] = -1;
                }
                break;
            }
            // Unregister USB thread.
            case 0x03415002:{
                log.debug("sceIoDevctl unregister usb thread");

                if (mem.isAddressGood(indata_addr) && inlen >= 4) {
                    // Unknown params.
                    cpu.gpr[2] = 0;
                } else {
                    cpu.gpr[2] = -1;
                }
                break;
            }

            default:
                log.warn("sceIoDevctl " + String.format("0x%08X", cmd) + " unknown command");
                if (mem.isAddressGood(indata_addr)) {
                    for (int i = 0; i < inlen; i += 4) {
                        log.warn("sceIoDevctl indata[" + (i / 4) + "]=0x" + Integer.toHexString(mem.read32(indata_addr + i)));
                    }
                }
                cpu.gpr[2] = -1;
                break;
        }

        State.fileLogger.logIoDevctl(cpu.gpr[2], device_addr, device, cmd, indata_addr, inlen, outdata_addr, outlen);
    }

	public void sceIoGetDevType(Processor processor) {
		CpuState cpu = processor.cpu;

		int uid = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug("sceIoGetDevType - uid " + Integer.toHexString(uid));
        }
        SceUidManager.checkUidPurpose(uid, "IOFileManager-File", true);
        IoInfo info = filelist.get(uid);
        if (info == null) {
            log.warn("sceIoGetDevType - unknown uid " + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_BAD_FILE_DESCRIPTOR;
        } else {
            // For now, return alias type, since it's the most used.
            cpu.gpr[2] = PSP_DEV_TYPE_ALIAS;
        }
	}

	public void sceIoAssign(Processor processor) {
		CpuState cpu = processor.cpu;

		int alias_addr = cpu.gpr[4];
		int physical_addr = cpu.gpr[5];
		int filesystem_addr = cpu.gpr[6];
		int mode = cpu.gpr[7];
		int arg_addr = cpu.gpr[8];
		int argSize = cpu.gpr[9];

		String alias = readStringZ(alias_addr);
        String physical_dev = readStringZ(physical_addr);
        String filesystem_dev = readStringZ(filesystem_addr);
        String perm;

        // IoAssignPerms
        switch(mode) {
            case 0: perm = "IOASSIGN_RDWR"; break;
            case 1: perm = "IOASSIGN_RDONLY"; break;
            default: perm = "unhandled " + mode; break;
        }

        // Mounts physical_dev on filesystem_dev and sets an alias to represent it.
        log.warn("IGNORING: sceIoAssign(alias='" + alias
            + "',physical_dev='" + physical_dev
            + "',filesystem_dev='" + filesystem_dev
            + "',mode=" + perm
            + ",arg_addr=0x" + Integer.toHexString(arg_addr)
            + ",argSize=" + argSize + ")");

        cpu.gpr[2] = 0;

        State.fileLogger.logIoAssign(cpu.gpr[2], alias_addr, alias, physical_addr, physical_dev,
                filesystem_addr, filesystem_dev, mode, arg_addr, argSize);
	}

	public void sceIoUnassign(Processor processor) {
		CpuState cpu = processor.cpu;

        int alias_addr = cpu.gpr[4];

        String alias = readStringZ(alias_addr);

        log.warn("IGNORING: sceIoUnassign (alias='" + alias + ")");

		cpu.gpr[2] = 0;
	}

	public void sceIoCancel(Processor processor) {
		CpuState cpu = processor.cpu;

		int uid = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug("sceIoCancel - uid " + Integer.toHexString(uid));
        }
        SceUidManager.checkUidPurpose(uid, "IOFileManager-File", true);
        IoInfo info = filelist.get(uid);
        if (info == null) {
            log.warn("sceIoCancel - unknown uid " + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_BAD_FILE_DESCRIPTOR;
        } else {
            info.closePending = true;
            cpu.gpr[2] = 0;
        }

        State.fileLogger.logIoCancel(cpu.gpr[2], uid);
	}

	public void sceIoGetFdList(Processor processor) {
		CpuState cpu = processor.cpu;

        int out_addr = cpu.gpr[4];
		int outSize = cpu.gpr[5];
		int fdNum_addr = cpu.gpr[6];

		log.warn("IGNORING: sceIoGetFdList (out_addr=0x" + out_addr
                + ", outSize=" + outSize
                + ", fdNum_addr=" + fdNum_addr + ")");

		cpu.gpr[2] = 0;
	}

	public final HLEModuleFunction sceIoPollAsyncFunction = new HLEModuleFunction("IoFileMgrForUser", "sceIoPollAsync") {
		@Override
		public final void execute(Processor processor) {
			sceIoPollAsync(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.IoFileMgrForUserModule.sceIoPollAsync(processor);";
		}
	};

	public final HLEModuleFunction sceIoWaitAsyncFunction = new HLEModuleFunction("IoFileMgrForUser", "sceIoWaitAsync") {
		@Override
		public final void execute(Processor processor) {
			sceIoWaitAsync(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.IoFileMgrForUserModule.sceIoWaitAsync(processor);";
		}
	};

	public final HLEModuleFunction sceIoWaitAsyncCBFunction = new HLEModuleFunction("IoFileMgrForUser", "sceIoWaitAsyncCB") {
		@Override
		public final void execute(Processor processor) {
			sceIoWaitAsyncCB(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.IoFileMgrForUserModule.sceIoWaitAsyncCB(processor);";
		}
	};

	public final HLEModuleFunction sceIoGetAsyncStatFunction = new HLEModuleFunction("IoFileMgrForUser", "sceIoGetAsyncStat") {
		@Override
		public final void execute(Processor processor) {
			sceIoGetAsyncStat(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.IoFileMgrForUserModule.sceIoGetAsyncStat(processor);";
		}
	};

	public final HLEModuleFunction sceIoChangeAsyncPriorityFunction = new HLEModuleFunction("IoFileMgrForUser", "sceIoChangeAsyncPriority") {
		@Override
		public final void execute(Processor processor) {
			sceIoChangeAsyncPriority(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.IoFileMgrForUserModule.sceIoChangeAsyncPriority(processor);";
		}
	};

	public final HLEModuleFunction sceIoSetAsyncCallbackFunction = new HLEModuleFunction("IoFileMgrForUser", "sceIoSetAsyncCallback") {
		@Override
		public final void execute(Processor processor) {
			sceIoSetAsyncCallback(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.IoFileMgrForUserModule.sceIoSetAsyncCallback(processor);";
		}
	};

	public final HLEModuleFunction sceIoCloseFunction = new HLEModuleFunction("IoFileMgrForUser", "sceIoClose") {
		@Override
		public final void execute(Processor processor) {
			sceIoClose(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.IoFileMgrForUserModule.sceIoClose(processor);";
		}
	};

	public final HLEModuleFunction sceIoCloseAsyncFunction = new HLEModuleFunction("IoFileMgrForUser", "sceIoCloseAsync") {
		@Override
		public final void execute(Processor processor) {
			sceIoCloseAsync(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.IoFileMgrForUserModule.sceIoCloseAsync(processor);";
		}
	};

	public final HLEModuleFunction sceIoOpenFunction = new HLEModuleFunction("IoFileMgrForUser", "sceIoOpen") {
		@Override
		public final void execute(Processor processor) {
			sceIoOpen(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.IoFileMgrForUserModule.sceIoOpen(processor);";
		}
	};

	public final HLEModuleFunction sceIoOpenAsyncFunction = new HLEModuleFunction("IoFileMgrForUser", "sceIoOpenAsync") {
		@Override
		public final void execute(Processor processor) {
			sceIoOpenAsync(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.IoFileMgrForUserModule.sceIoOpenAsync(processor);";
		}
	};

	public final HLEModuleFunction sceIoReadFunction = new HLEModuleFunction("IoFileMgrForUser", "sceIoRead") {
		@Override
		public final void execute(Processor processor) {
			sceIoRead(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.IoFileMgrForUserModule.sceIoRead(processor);";
		}
	};

	public final HLEModuleFunction sceIoReadAsyncFunction = new HLEModuleFunction("IoFileMgrForUser", "sceIoReadAsync") {
		@Override
		public final void execute(Processor processor) {
			sceIoReadAsync(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.IoFileMgrForUserModule.sceIoReadAsync(processor);";
		}
	};

	public final HLEModuleFunction sceIoWriteFunction = new HLEModuleFunction("IoFileMgrForUser", "sceIoWrite") {
		@Override
		public final void execute(Processor processor) {
			sceIoWrite(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.IoFileMgrForUserModule.sceIoWrite(processor);";
		}
	};

	public final HLEModuleFunction sceIoWriteAsyncFunction = new HLEModuleFunction("IoFileMgrForUser", "sceIoWriteAsync") {
		@Override
		public final void execute(Processor processor) {
			sceIoWriteAsync(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.IoFileMgrForUserModule.sceIoWriteAsync(processor);";
		}
	};

	public final HLEModuleFunction sceIoLseekFunction = new HLEModuleFunction("IoFileMgrForUser", "sceIoLseek") {
		@Override
		public final void execute(Processor processor) {
			sceIoLseek(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.IoFileMgrForUserModule.sceIoLseek(processor);";
		}
	};

	public final HLEModuleFunction sceIoLseekAsyncFunction = new HLEModuleFunction("IoFileMgrForUser", "sceIoLseekAsync") {
		@Override
		public final void execute(Processor processor) {
			sceIoLseekAsync(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.IoFileMgrForUserModule.sceIoLseekAsync(processor);";
		}
	};

	public final HLEModuleFunction sceIoLseek32Function = new HLEModuleFunction("IoFileMgrForUser", "sceIoLseek32") {
		@Override
		public final void execute(Processor processor) {
			sceIoLseek32(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.IoFileMgrForUserModule.sceIoLseek32(processor);";
		}
	};

	public final HLEModuleFunction sceIoLseek32AsyncFunction = new HLEModuleFunction("IoFileMgrForUser", "sceIoLseek32Async") {
		@Override
		public final void execute(Processor processor) {
			sceIoLseek32Async(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.IoFileMgrForUserModule.sceIoLseek32Async(processor);";
		}
	};

	public final HLEModuleFunction sceIoIoctlFunction = new HLEModuleFunction("IoFileMgrForUser", "sceIoIoctl") {
		@Override
		public final void execute(Processor processor) {
			sceIoIoctl(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.IoFileMgrForUserModule.sceIoIoctl(processor);";
		}
	};

	public final HLEModuleFunction sceIoIoctlAsyncFunction = new HLEModuleFunction("IoFileMgrForUser", "sceIoIoctlAsync") {
		@Override
		public final void execute(Processor processor) {
			sceIoIoctlAsync(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.IoFileMgrForUserModule.sceIoIoctlAsync(processor);";
		}
	};

	public final HLEModuleFunction sceIoDopenFunction = new HLEModuleFunction("IoFileMgrForUser", "sceIoDopen") {
		@Override
		public final void execute(Processor processor) {
			sceIoDopen(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.IoFileMgrForUserModule.sceIoDopen(processor);";
		}
	};

	public final HLEModuleFunction sceIoDreadFunction = new HLEModuleFunction("IoFileMgrForUser", "sceIoDread") {
		@Override
		public final void execute(Processor processor) {
			sceIoDread(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.IoFileMgrForUserModule.sceIoDread(processor);";
		}
	};

	public final HLEModuleFunction sceIoDcloseFunction = new HLEModuleFunction("IoFileMgrForUser", "sceIoDclose") {
		@Override
		public final void execute(Processor processor) {
			sceIoDclose(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.IoFileMgrForUserModule.sceIoDclose(processor);";
		}
	};

	public final HLEModuleFunction sceIoRemoveFunction = new HLEModuleFunction("IoFileMgrForUser", "sceIoRemove") {
		@Override
		public final void execute(Processor processor) {
			sceIoRemove(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.IoFileMgrForUserModule.sceIoRemove(processor);";
		}
	};

	public final HLEModuleFunction sceIoMkdirFunction = new HLEModuleFunction("IoFileMgrForUser", "sceIoMkdir") {
		@Override
		public final void execute(Processor processor) {
			sceIoMkdir(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.IoFileMgrForUserModule.sceIoMkdir(processor);";
		}
	};

	public final HLEModuleFunction sceIoRmdirFunction = new HLEModuleFunction("IoFileMgrForUser", "sceIoRmdir") {
		@Override
		public final void execute(Processor processor) {
			sceIoRmdir(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.IoFileMgrForUserModule.sceIoRmdir(processor);";
		}
	};

	public final HLEModuleFunction sceIoChdirFunction = new HLEModuleFunction("IoFileMgrForUser", "sceIoChdir") {
		@Override
		public final void execute(Processor processor) {
			sceIoChdir(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.IoFileMgrForUserModule.sceIoChdir(processor);";
		}
	};

	public final HLEModuleFunction sceIoSyncFunction = new HLEModuleFunction("IoFileMgrForUser", "sceIoSync") {
		@Override
		public final void execute(Processor processor) {
			sceIoSync(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.IoFileMgrForUserModule.sceIoSync(processor);";
		}
	};

	public final HLEModuleFunction sceIoGetstatFunction = new HLEModuleFunction("IoFileMgrForUser", "sceIoGetstat") {
		@Override
		public final void execute(Processor processor) {
			sceIoGetstat(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.IoFileMgrForUserModule.sceIoGetstat(processor);";
		}
	};

	public final HLEModuleFunction sceIoChstatFunction = new HLEModuleFunction("IoFileMgrForUser", "sceIoChstat") {
		@Override
		public final void execute(Processor processor) {
			sceIoChstat(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.IoFileMgrForUserModule.sceIoChstat(processor);";
		}
	};

	public final HLEModuleFunction sceIoRenameFunction = new HLEModuleFunction("IoFileMgrForUser", "sceIoRename") {
		@Override
		public final void execute(Processor processor) {
			sceIoRename(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.IoFileMgrForUserModule.sceIoRename(processor);";
		}
	};

	public final HLEModuleFunction sceIoDevctlFunction = new HLEModuleFunction("IoFileMgrForUser", "sceIoDevctl") {
		@Override
		public final void execute(Processor processor) {
			sceIoDevctl(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.IoFileMgrForUserModule.sceIoDevctl(processor);";
		}
	};

	public final HLEModuleFunction sceIoGetDevTypeFunction = new HLEModuleFunction("IoFileMgrForUser", "sceIoGetDevType") {
		@Override
		public final void execute(Processor processor) {
			sceIoGetDevType(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.IoFileMgrForUserModule.sceIoGetDevType(processor);";
		}
	};

	public final HLEModuleFunction sceIoAssignFunction = new HLEModuleFunction("IoFileMgrForUser", "sceIoAssign") {
		@Override
		public final void execute(Processor processor) {
			sceIoAssign(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.IoFileMgrForUserModule.sceIoAssign(processor);";
		}
	};

	public final HLEModuleFunction sceIoUnassignFunction = new HLEModuleFunction("IoFileMgrForUser", "sceIoUnassign") {
		@Override
		public final void execute(Processor processor) {
			sceIoUnassign(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.IoFileMgrForUserModule.sceIoUnassign(processor);";
		}
	};

	public final HLEModuleFunction sceIoCancelFunction = new HLEModuleFunction("IoFileMgrForUser", "sceIoCancel") {
		@Override
		public final void execute(Processor processor) {
			sceIoCancel(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.IoFileMgrForUserModule.sceIoCancel(processor);";
		}
	};

	public final HLEModuleFunction sceIoGetFdListFunction = new HLEModuleFunction("IoFileMgrForUser", "sceIoGetFdList") {
		@Override
		public final void execute(Processor processor) {
			sceIoGetFdList(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.IoFileMgrForUserModule.sceIoGetFdList(processor);";
		}
	};
}