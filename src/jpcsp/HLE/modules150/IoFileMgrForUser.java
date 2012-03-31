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

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_ERRNO_DEVICE_NOT_FOUND;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_ERRNO_FILE_ALREADY_EXISTS;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_ERRNO_FILE_NOT_FOUND;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_ERRNO_INVALID_ARGUMENT;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_ERRNO_READ_ONLY;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_INVALID_ARGUMENT;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_ASYNC_BUSY;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_BAD_FILE_DESCRIPTOR;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_FILE_READ_ERROR;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_NO_ASYNC_OP;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_NO_SUCH_DEVICE;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_TOO_MANY_OPEN_FILES;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_UNSUPPORTED_OPERATION;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_MEMSTICK_DEVCTL_BAD_PARAMS;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.JPCSP_WAIT_IO;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_READY;
import static jpcsp.util.Utilities.readStringNZ;
import static jpcsp.util.Utilities.readStringZ;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.PspString;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Pattern;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Processor;
import jpcsp.Allegrex.Common;
import jpcsp.Allegrex.CpuState;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.IWaitStateChecker;
import jpcsp.HLE.kernel.types.SceIoDirent;
import jpcsp.HLE.kernel.types.SceIoStat;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.ScePspDateTime;
import jpcsp.HLE.kernel.types.ThreadWaitInfo;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.ThreadManForUser;
import jpcsp.autotests.AutoTestsOutput;
import jpcsp.connector.PGDFileConnector;
import jpcsp.crypto.CryptoEngine;
import jpcsp.filesystems.SeekableDataInput;
import jpcsp.filesystems.SeekableRandomFile;
import jpcsp.filesystems.umdiso.UmdIsoFile;
import jpcsp.filesystems.umdiso.UmdIsoReader;
import jpcsp.hardware.MemoryStick;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryWriter;
import jpcsp.settings.AbstractBoolSettingsListener;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

/*
 * TODO list:
 * 1. Get std out/err/in from stdio module.
 *
 * 2. The PSP's filesystem supports permissions.
 * Implement PSP_O_CREAT based on Java File and it's setReadable/Writable/Executable.
 */
public class IoFileMgrForUser extends HLEModule {

    private static Logger log = Modules.getLogger("IoFileMgrForUser");
    private static Logger stdout = Logger.getLogger("stdout");
    private static Logger stderr = Logger.getLogger("stderr");
    public final static int PSP_O_RDONLY = 0x0001;
    public final static int PSP_O_WRONLY = 0x0002;
    public final static int PSP_O_RDWR = (PSP_O_RDONLY | PSP_O_WRONLY);
    public final static int PSP_O_NBLOCK = 0x0004;
    public final static int PSP_O_DIROPEN = 0x0008;
    public final static int PSP_O_APPEND = 0x0100;
    public final static int PSP_O_CREAT = 0x0200;
    public final static int PSP_O_TRUNC = 0x0400;
    public final static int PSP_O_EXCL = 0x0800;
    public final static int PSP_O_NBUF = 0x4000;            // Used on the PSP to bypass the internal disc cache (commonly seen in media files that need to maintain a fixed bitrate).
    public final static int PSP_O_NOWAIT = 0x8000;
    public final static int PSP_O_PLOCK = 0x2000000;        // Used on the PSP to open the file inside a power lock (safe).
    public final static int PSP_O_FGAMEDATA = 0x40000000;   // Used on the PSP to handle encrypted data (used by NPDRM module).

    //Every flag seems to be ORed with a retry count.
    //In Activision Hits Remixed, an error is produced after
    //the retry count (0xf0000/15) is over.
    public final static int PSP_O_RETRY_0 = 0x00000;
    public final static int PSP_O_RETRY_1 = 0x10000;
    public final static int PSP_O_RETRY_2 = 0x20000;
    public final static int PSP_O_RETRY_3 = 0x30000;
    public final static int PSP_O_RETRY_4 = 0x40000;
    public final static int PSP_O_RETRY_5 = 0x50000;
    public final static int PSP_O_RETRY_6 = 0x60000;
    public final static int PSP_O_RETRY_7 = 0x70000;
    public final static int PSP_O_RETRY_8 = 0x80000;
    public final static int PSP_O_RETRY_9 = 0x90000;
    public final static int PSP_O_RETRY_10 = 0xa0000;
    public final static int PSP_O_RETRY_11 = 0xb0000;
    public final static int PSP_O_RETRY_12 = 0xc0000;
    public final static int PSP_O_RETRY_13 = 0xd0000;
    public final static int PSP_O_RETRY_14 = 0xe0000;
    public final static int PSP_O_RETRY_15 = 0xf0000;

    public final static int PSP_SEEK_SET = 0;
    public final static int PSP_SEEK_CUR = 1;
    public final static int PSP_SEEK_END = 2;

    // Type of device used (abstract).
    // Can simbolize physical character or block devices, logical filesystem devices
    // or devices represented by an alias or even mount point devices also represented by an alias.
    public final static int PSP_DEV_TYPE_NONE = 0x0;
    public final static int PSP_DEV_TYPE_CHARACTER = 0x1;
    public final static int PSP_DEV_TYPE_BLOCK = 0x4;
    public final static int PSP_DEV_TYPE_FILESYSTEM = 0x10;
    public final static int PSP_DEV_TYPE_ALIAS = 0x20;
    public final static int PSP_DEV_TYPE_MOUNT = 0x40;

    public final static int STDOUT_ID = 1;
    public final static int STDERR_ID = 2;
    public final static int STDIN_ID = 3;

    private final static int MIN_ID = 4;
    private final static int MAX_ID = 63;
    private final static String idPurpose = "IOFileManager-File";

    protected static enum IoOperation {
        open(5), close(1), seek, ioctl(2), remove, rename, mkdir, dread,
        // Duration of read operation: approx. 4 ms per 0x10000 bytes (tested on real PSP)
        read(4, 0x10000),
        // Duration of write operation: approx. 5 ms per 0x10000 bytes
        write(5, 0x10000);

        private int delayMillis;
        private int sizeUnit;

        IoOperation() {
            this.delayMillis = 0;
        }

        IoOperation(int delayMillis) {
            this.delayMillis = delayMillis;
            this.sizeUnit = 0;
        }

        IoOperation(int delayMillis, int sizeUnit) {
            this.delayMillis = delayMillis;
            this.sizeUnit = sizeUnit;
        }

        /**
         * Return a delay in milliseconds for the IoOperation.
         * 
         * @return       the delay in milliseconds
         */
        int getDelayMillis() {
        	return delayMillis;
        }

        /**
         * Return a delay in milliseconds based on the size of the
         * processed data.
         * 
         * @param size   size of the processed data.
         *               0 if no size is available.
         * @return       the delay in milliseconds
         */
        int getDelayMillis(int size) {
        	if (sizeUnit == 0 || size <= 0) {
        		return getDelayMillis();
        	}

        	return (int) (((long) delayMillis) * size / sizeUnit);
        }
    }

    // modeStrings indexed by [0, PSP_O_RDONLY, PSP_O_WRONLY, PSP_O_RDWR]
    // SeekableRandomFile doesn't support write only: take "rw",
    private final static String[] modeStrings = {"r", "r", "rw", "rw"};
    public HashMap<Integer, IoInfo> fileIds;
    public HashMap<Integer, IoInfo> fileUids;
    public HashMap<Integer, IoDirInfo> dirIds;
    private String filepath; // current working directory on PC
    private UmdIsoReader iso;
    private IoWaitStateChecker ioWaitStateChecker;

    private int defaultAsyncPriority;
    private final static int asyncThreadRegisterArgument = Common._s0; // $s0 is preserved across calls

    private PGDFileConnector pgdFileConnector;
    private boolean allowExtractPGD;

    // Implement the list of IIoListener as an array to improve the performance
    // when iterating over all the entries (most common action).
    private IIoListener[] ioListeners;

    public class IoInfo {
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
        public final int id;
        public final int uid;
        public long result; // The return value from the last operation on this file, used by sceIoWaitAsync
        public boolean closePending = false; // sceIoCloseAsync has been called on this file
        public boolean asyncPending; // Thread has not switched since an async operation was called on this file
        public boolean asyncResultPending; // Async IO result is available and has not yet been retrieved
        public long asyncDoneMillis; // When the async operation can be completed
        public int asyncThreadPriority = defaultAsyncPriority;
        public SceKernelThreadInfo asyncThread;
        public IAction asyncAction;

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
            id = getNewId();
            if (isValidId()) {
            	uid = getNewUid();
            	fileIds.put(id, this);
            	fileUids.put(uid, this);
            } else {
            	uid = -1;
            }
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
            id = getNewId();
            if (isValidId()) {
            	uid = getNewUid();
            	fileIds.put(id, this);
            	fileUids.put(uid, this);
            } else {
            	uid = -1;
            }
        }

        public boolean isValidId() {
        	return id != SceUidManager.INVALID_ID;
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

        public void truncate(int length) {
            try {
                // Only valid for msFile.
                if (msFile != null) {
                    msFile.setLength(length);
                }
            } catch (IOException ioe) {
                // Ignore.
            }
        }

        public IoInfo close() {
        	IoInfo info = fileIds.remove(id);
        	if (info != null) {
        		fileUids.remove(uid);
        		releaseId(id);
        		releaseUid(uid);
        	}

        	return info;
        }
    }

    public class IoDirInfo {

        final String path;
        final String[] filenames;
        int position;
        int printableposition;
        final int id;

        public IoDirInfo(String path, String[] filenames) {
            // iso reader doesn't like path//filename, so trim trailing /
            // (it's like doing cd somedir/ instead of cd somedir, makes little difference)
            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            this.path = path;
            this.filenames = filenames;
            position = 0;
            printableposition = 0;
            // Hide iso special files
            if (filenames != null) {
                if (filenames.length > position && filenames[position].equals(".")) {
                    position++;
                }
                if (filenames.length > position && filenames[position].equals("\01")) {
                    position++;
                }
            }
            id = getNewId();
            dirIds.put(id, this);
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

        public IoDirInfo close() {
        	IoDirInfo info = dirIds.remove(id);
        	if (info != null) {
        		releaseId(id);
        	}

        	return info;
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

    public static interface IIoListener {

        void sceIoSync(int result, int device_addr, String device, int unknown);

        void sceIoPollAsync(int result, int uid, int res_addr);

        void sceIoWaitAsync(int result, int uid, int res_addr);

        void sceIoOpen(int result, int filename_addr, String filename, int flags, int permissions, String mode);

        void sceIoClose(int result, int uid);

        void sceIoWrite(int result, int uid, int data_addr, int size, int bytesWritten);

        void sceIoRead(int result, int uid, int data_addr, int size, int bytesRead, long position, SeekableDataInput dataInput);

        void sceIoCancel(int result, int uid);

        void sceIoSeek32(int result, int uid, int offset, int whence);

        void sceIoSeek64(long result, int uid, long offset, int whence);

        void sceIoMkdir(int result, int path_addr, String path, int permissions);

        void sceIoRmdir(int result, int path_addr, String path);

        void sceIoChdir(int result, int path_addr, String path);

        void sceIoDopen(int result, int path_addr, String path);

        void sceIoDread(int result, int uid, int dirent_addr);

        void sceIoDclose(int result, int uid);

        void sceIoDevctl(int result, int device_addr, String device, int cmd, int indata_addr, int inlen, int outdata_addr, int outlen);

        void sceIoIoctl(int result, int uid, int cmd, int indata_addr, int inlen, int outdata_addr, int outlen);

        void sceIoAssign(int result, int dev1_addr, String dev1, int dev2_addr, String dev2, int dev3_addr, String dev3, int mode, int unk1, int unk2);

        void sceIoGetStat(int result, int path_addr, String path, int stat_addr);

        void sceIoRemove(int result, int path_addr, String path);

        void sceIoChstat(int result, int path_addr, String path, int stat_addr, int bits);

        void sceIoRename(int result, int path_addr, String path, int new_path_addr, String newpath);
    }

    private class IoWaitStateChecker implements IWaitStateChecker {

        @Override
        public boolean continueWaitState(SceKernelThreadInfo thread, ThreadWaitInfo wait) {
            IoInfo info = fileIds.get(wait.Io_id);
            if (info == null) {
                return false;
            }
            if (!info.asyncPending) {
                // Async IO is already completed
                return false;
            }

            return true;
        }
    }

    private class IOAsyncReadAction implements IAction {
    	private IoInfo info;
    	private int address;
    	private int size;
    	private int requestedSize;

    	public IOAsyncReadAction(IoInfo info, int address, int requestedSize, int size) {
    		this.info = info;
    		this.address = address;
    		this.requestedSize = requestedSize;
    		this.size = size;
    	}

        @Override
        public void execute() {
            long position = info.position;
            int result = 0;

            try {
            	Utilities.readFully(info.readOnlyFile, address, size);
            	info.position += size;
            	result = size;
            	if (info.sectorBlockMode) {
            		result /= UmdIsoFile.sectorLength;
            	}
            } catch (IOException e) {
            	log.error(e);
            	result = ERROR_KERNEL_FILE_READ_ERROR;
            }

            info.result = result;

            // Invalidate any compiled code in the read range
            RuntimeContext.invalidateRange(address, size);

            for (IIoListener ioListener : ioListeners) {
                ioListener.sceIoRead(result, info.id, address, requestedSize, size, position, info.readOnlyFile);
            }
        }
    }

	private class ExtractPGDSettingsListerner extends AbstractBoolSettingsListener {
		@Override
		protected void settingsValueChanged(boolean value) {
			setAllowExtractPGDStatus(value);
		}
	}

    @Override
    public String getName() {
        return "IoFileMgrForUser";
    }

    @Override
    public void start() {
        if (fileIds != null) {
            // Close open files
            for (Iterator<IoInfo> it = fileIds.values().iterator(); it.hasNext();) {
                IoInfo info = it.next();
                try {
                    info.readOnlyFile.close();
                } catch (IOException e) {
                    log.error("pspiofilemgr - error closing file: " + e.getMessage());
                }
            }
        }
        fileIds = new HashMap<Integer, IoInfo>();
        fileUids = new HashMap<Integer, IoInfo>();
        dirIds = new HashMap<Integer, IoDirInfo>();
        MemoryStick.setStateMs(MemoryStick.PSP_MEMORYSTICK_STATE_DRIVER_READY);
        defaultAsyncPriority = -1;
        if (ioListeners == null) {
            ioListeners = new IIoListener[0];
        }
        ioWaitStateChecker = new IoWaitStateChecker();

        setSettingsListener("emu.extractPGD", new ExtractPGDSettingsListerner());

        super.start();
    }

    private void setAllowExtractPGDStatus(boolean status) {
        allowExtractPGD = status;
    }

    private boolean getAllowExtractPGDStatus() {
        return allowExtractPGD;
    }
    
    public IoInfo getFileIoInfo(int id) {
        return fileIds.get(id);
    }

    private static int getNewUid() {
    	return SceUidManager.getNewUid(idPurpose);
    }

    private static void releaseUid(int uid) {
    	SceUidManager.releaseUid(uid, idPurpose);
    }

    private static int getNewId() {
    	return SceUidManager.getNewId(idPurpose, MIN_ID, MAX_ID);
    }

    private static void releaseId(int id) {
    	SceUidManager.releaseId(id, idPurpose);
    }

    /*
     *  Local file handling functions.
     */
    public String getDeviceFilePath(String pspfilename) {
        pspfilename = pspfilename.replaceAll("\\\\", "/");
        String device = null;
        String cwd = "";
        String filename = null;

        if (pspfilename.startsWith("flash0:")) {
        	if (pspfilename.startsWith("flash0:/")) {
        		return pspfilename.replace("flash0:", "flash0");
        	}
        	return pspfilename.replace("flash0:", "flash0/");
        }

        if (filepath == null) {
        	return pspfilename;
        }

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

        // Ignore the filename in "umd0:xxx".
        // Using umd0: is always opening the whole UMD in sector block mode,
        // ignoring the file name specified after the colon.
        if (device.startsWith("umd")) {
        	pspfilename = "";
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
            if (deviceFilePath.matches("^" + umdPrefixes[i] + "[0-9]+.*")) {
                return true;
            }
        }

        return false;
    }

    private String trimUmdPrefix(String pcfilename) {
        // Assume the device name is always lower case (ensured by getDeviceFilePath)
        // Assume there is always a device number
        // Handle case where file path is blank so there is no slash after the device name
        for (int i = 0; i < umdPrefixes.length; i++) {
            if (pcfilename.matches("^" + umdPrefixes[i] + "[0-9]+/.*")) {
                return pcfilename.substring(pcfilename.indexOf("/") + 1);
            } else if (pcfilename.matches("^" + umdPrefixes[i] + "[0-9]+")) {
                return "";
            }
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

    private boolean rmdir(File f, boolean recursive) {
    	boolean subDirResult = true;
    	if (recursive && f.isDirectory()) {
			File[] subFiles = f.listFiles();
			for (int i = 0; subFiles != null && i < subFiles.length; i++) {
				if (!rmdir(subFiles[i], recursive)) {
					subDirResult = false;
				}
			}
    	}

    	return f.delete() && subDirResult;
    }

    public boolean rmdir(String dir, boolean recursive) {
    	String pcfilename = getDeviceFilePath(dir);
    	if (pcfilename == null) {
    		return false;
    	}

    	File f = new File(pcfilename);
    	return rmdir(f, recursive);
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
                    Emulator.getProcessor().cpu.gpr[2] = ERROR_ERRNO_DEVICE_NOT_FOUND;
                // check umd is activated
                } else if (!Modules.sceUmdUserModule.isUmdActivated()) {
                    log.warn("stat - umd mounted but not activated");
                    Emulator.getProcessor().cpu.gpr[2] = ERROR_KERNEL_NO_SUCH_DEVICE;
                } else {
                    String isofilename = trimUmdPrefix(pcfilename);
                    int mode = 4; // 4 = readable
                    int attr = 0;
                    long size = 0;
                    long timestamp = 0;
                    int startSector = 0;
                    try {
                        // Check for files first.
                        UmdIsoFile file = iso.getFile(isofilename);
                        attr = 0x20;
                        size = file.length();
                        timestamp = file.getTimestamp().getTime();
                        startSector = file.getStartSector();
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
                    } catch (FileNotFoundException fnfe) {
                        // If file wasn't found, try looking for a directory.
                        try {
                            if (iso.isDirectory(isofilename)) {
                                attr |= 0x10;
                                mode |= 1; // 1 = executable
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
                        } catch (FileNotFoundException dnfe) {
                            log.warn("stat - '" + isofilename + "' umd file/dir not found");
                        } catch (IOException e) {
                            log.warn("stat - umd io error: " + e.getMessage());
                        }
                    } catch (IOException e) {
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
                    if (file.isDirectory()) {
                        attr |= 0x10;
                    }
                    if (file.isFile()) {
                        attr |= 0x20;
                    }
                    mode |= attr << 8;
                    // Java can't see file create/access time
                    stat = new SceIoStat(mode, attr, size,
                            ScePspDateTime.fromUnixTime(mtime),
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
                    } catch (FileNotFoundException e) {
                        if (log.isDebugEnabled()) {
                            log.debug("getFile - umd file not found '" + pcfilename + "' (ok to ignore this message, debug purpose only)");
                        }
                    } catch (IOException e) {
                        log.error("getFile - error opening umd media: " + e.getMessage());
                    }
                }
            } else {
                // First check if the file already exists
                File file = new File(pcfilename);
                if (file.exists() &&
                        (flags & PSP_O_CREAT) == PSP_O_CREAT &&
                        (flags & PSP_O_EXCL) == PSP_O_EXCL) {
                    if (log.isDebugEnabled()) {
                        log.debug("getFile - file already exists (PSP_O_CREAT + PSP_O_EXCL)");
                    }
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
                        if (log.isDebugEnabled()) {
                            log.debug("getFile - file not found '" + pcfilename + "' (ok to ignore this message, debug purpose only)");
                        }
                    }
                }
            }
        }

        return resultFile;
    }

    public SeekableDataInput getFile(int id) {
        IoInfo info = fileIds.get(id);
        if (info == null) {
            return null;
        }
        return info.readOnlyFile;
    }

    public String getFileFilename(int id) {
        IoInfo info = fileIds.get(id);
        if (info == null) {
            return null;
        }
        return info.filename;
    }

    private String getMode(int flags) {
        return modeStrings[flags & PSP_O_RDWR];
    }

    private long updateResult(IoInfo info, long result, boolean async, boolean resultIs64bit, IoOperation ioOperation) {
    	return updateResult(info, result, async, resultIs64bit, ioOperation, null, 0);
    }

    // Handle returning/storing result for sync/async operations
    private long updateResult(IoInfo info, long result, boolean async, boolean resultIs64bit, IoOperation ioOperation, IAction asyncAction, int size) {
        if (info != null) {
            if (async) {
                if (!info.asyncPending) {
                    startIoAsync(info, result, ioOperation, asyncAction, size);
                    result = 0;
                }
            } else {
                info.result = ERROR_KERNEL_NO_ASYNC_OP;
            }
        }

        return result;
    }

    private String getWhenceName(int whence) {
        switch (whence) {
            case PSP_SEEK_SET:
                return "PSP_SEEK_SET";
            case PSP_SEEK_CUR:
                return "PSP_SEEK_CUR";
            case PSP_SEEK_END:
                return "PSP_SEEK_END";
            default:
                return "UNHANDLED " + whence;
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

    protected void delayIoOperation(IoOperation ioOperation) {
        if (ioOperation.delayMillis > 0) {
            Modules.ThreadManForUserModule.hleKernelDelayThread(ioOperation.delayMillis * 1000, false);
        }
    }

    /*
     * Async thread functions.
     */
    public void hleAsyncThread(Processor processor) {
        CpuState cpu = processor.cpu;
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;

        int uid = cpu.gpr[asyncThreadRegisterArgument];

        IoInfo info = fileUids.get(uid);
        if (info == null) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("hleAsyncThread non-existing uid=%x", uid));
            }
            cpu.gpr[Common._v0] = 0; // Exit status
            // Exit and delete the thread to free its resources (e.g. its stack)
            threadMan.hleKernelExitDeleteThread();
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("hleAsyncThread id=%x", info.id));
            }
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
    private void startIoAsync(IoInfo info, long result, IoOperation ioOperation, IAction asyncAction, int size) {
        if (info == null) {
            return;
        }
        info.asyncPending = true;
        info.asyncResultPending = false;
        long now = Emulator.getClock().currentTimeMillis();
        info.asyncDoneMillis = now + ioOperation.getDelayMillis(size);
        info.asyncAction = asyncAction;
        info.result = result;
        if (info.asyncThread == null) {
            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
            // Inherit priority from current thread if no default priority set
            int asyncPriority = info.asyncThreadPriority;
            if (asyncPriority < 0) {
                asyncPriority = threadMan.getCurrentThread().currentPriority;
            }

            int stackSize = 0x2000;
            // On FW 1.50, the stack size for the async thread is 0x2000,
            // on FW 5.00, the stack size is 0x800.
            // When did it change?
            if (Emulator.getInstance().getFirmwareVersion() > 150) {
            	stackSize = 0x800;
            }

            info.asyncThread = threadMan.hleKernelCreateThread("SceIofileAsync",
                    ThreadManForUser.ASYNC_LOOP_ADDRESS, asyncPriority, stackSize,
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

        // Execute any pending async action and remove it
        if (info.asyncAction != null) {
        	IAction asyncAction = info.asyncAction;
        	info.asyncAction = null;
        	asyncAction.execute();
        }

        if (info.asyncPending) {
            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
            if (info.getAsyncRestMillis() > 0) {
                done = false;
            } else {
                info.asyncPending = false;
                info.asyncResultPending = true;
                if (info.cbid >= 0) {
                    // Trigger Async callback.
                    threadMan.hleKernelNotifyCallback(SceKernelThreadInfo.THREAD_CALLBACK_IO, info.cbid, info.notifyArg);
                }
                // Find threads waiting on this id and wake them up.
                for (Iterator<SceKernelThreadInfo> it = threadMan.iterator(); it.hasNext();) {
                    SceKernelThreadInfo thread = it.next();
                    if (thread.waitType == JPCSP_WAIT_IO &&
                            thread.wait.Io_id == info.id) {
                        if (log.isDebugEnabled()) {
                            log.debug("IoFileMgrForUser.doStepAsync - onContextSwitch waking " + Integer.toHexString(thread.uid) + " thread:'" + thread.name + "'");
                        }

                        // Write result
                        Memory mem = Memory.getInstance();
                        if (Memory.isAddressGood(thread.wait.Io_resultAddr)) {
                        	if (log.isDebugEnabled()) {
                        		log.debug("IoFileMgrForUser.doStepAsync - storing result 0x" + Long.toHexString(info.result));
                        	}
                            mem.write64(thread.wait.Io_resultAddr, info.result);
                        }

                        // Return error at next call to sceIoWaitAsync
                        info.result = ERROR_KERNEL_NO_ASYNC_OP;
                        info.asyncResultPending = false;

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

    public void unregisterIoListener(IIoListener ioListener) {
        if (ioListeners != null) {
            for (int i = 0; i < ioListeners.length; i++) {
                if (ioListeners[i] == ioListener) {
                    IIoListener[] newIoListeners = new IIoListener[ioListeners.length - 1];
                    System.arraycopy(ioListeners, 0, newIoListeners, 0, i);
                    System.arraycopy(ioListeners, i + 1, newIoListeners, i, ioListeners.length - i - 1);
                    ioListeners = newIoListeners;
                    break;
                }
            }
        }
    }

    public void registerIoListener(IIoListener ioListener) {
        if (ioListeners == null) {
            ioListeners = new IIoListener[1];
            ioListeners[0] = ioListener;
        } else {
            for (int i = 0; i < ioListeners.length; i++) {
                if (ioListeners[i] == ioListener) {
                    // The listener is already registered
                    return;
                }
            }

            IIoListener[] newIoListeners = new IIoListener[ioListeners.length + 1];
            System.arraycopy(ioListeners, 0, newIoListeners, 0, ioListeners.length);
            newIoListeners[ioListeners.length] = ioListener;
            ioListeners = newIoListeners;
        }
    }

    /*
     * HLE functions.
     */
    public int hleIoWaitAsync(int id, int res_addr, boolean wait, boolean callbacks) {
        if (log.isDebugEnabled()) {
            log.debug("hleIoWaitAsync(id=" + Integer.toHexString(id) + ",res=0x" + Integer.toHexString(res_addr) + ") wait=" + wait + " callbacks=" + callbacks);
        }

        IoInfo info = fileIds.get(id);

        if (info == null) {
            log.warn("hleIoWaitAsync - unknown id " + Integer.toHexString(id));
            return ERROR_KERNEL_BAD_FILE_DESCRIPTOR;
        }

        if (info.result == ERROR_KERNEL_NO_ASYNC_OP) {
            log.debug("hleIoWaitAsync - PSP_ERROR_NO_ASYNC_OP");
            return ERROR_KERNEL_NO_ASYNC_OP;
        }

        if (info.asyncPending && !wait) {
            // Polling returns 1 when async is busy.
            log.debug("hleIoWaitAsync - poll return = 1(busy)");
            if (log.isDebugEnabled()) {
            	log.debug(String.format("hleIoWaitAsync info.result=%d", info.result));
            }
            return 1;
        }

        boolean waitForAsync = false;

        // Check for the waiting condition first.
        if (wait) {
            waitForAsync = true;                
        }

        if (!info.asyncPending) {
            log.debug("hleIoWaitAsync - async already completed, not waiting");
            waitForAsync = false;
        }
        
        // The file was marked as closePending, so close it right away to avoid delays.
        if (info.closePending) {
            log.debug("hleIoWaitAsync - file marked with closePending, calling hleIoClose, not waiting");
            hleIoClose(info.id, false);
            waitForAsync = false;
        }

        // The file was not found at sceIoOpenAsync.
        if (info.result == ERROR_ERRNO_FILE_NOT_FOUND) {
            log.debug("hleIoWaitAsync - file not found, not waiting");
            info.close();
            triggerAsyncThread(info);
            waitForAsync = false;
        }

        if (waitForAsync) {
            // Call the ioListeners.
            for (IIoListener ioListener : ioListeners) {
                ioListener.sceIoWaitAsync(0, id, res_addr);
            }
            // Start the waiting mode.
            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
            SceKernelThreadInfo currentThread = threadMan.getCurrentThread();
            currentThread.wait.Io_id = info.id;
            currentThread.wait.Io_resultAddr = res_addr;
            threadMan.hleKernelThreadEnterWaitState(JPCSP_WAIT_IO, info.id, ioWaitStateChecker, callbacks);
        } else {
            // Always store the result.
            Memory mem = Memory.getInstance();
            if (Memory.isAddressGood(res_addr)) {
            	if (log.isDebugEnabled()) {
            		log.debug("hleIoWaitAsync - storing result 0x" + Long.toHexString(info.result));
            	}
                mem.write64(res_addr, info.result);
            }

            // Async result can only be retrieved once
            info.asyncResultPending = false;
            info.result = ERROR_KERNEL_NO_ASYNC_OP;

            // For sceIoPollAsync, only call the ioListeners.
            for (IIoListener ioListener : ioListeners) {
                ioListener.sceIoPollAsync(0, id, res_addr);
            }
        }

        return 0;
    }
    
    public int hleIoOpen(PspString filename, int flags, int permissions, boolean async) {
    	return hleIoOpen(filename.getAddress(), filename.getString(), flags, permissions, async);
    }

    public int hleIoOpen(int filename_addr, String filename, int flags, int permissions, boolean async) {
        if (log.isInfoEnabled()) {
            log.info("hleIoOpen filename = " + filename + " flags = " + Integer.toHexString(flags) + " permissions = 0" + Integer.toOctalString(permissions));
        }
        if (log.isDebugEnabled()) {
            if ((flags & PSP_O_RDONLY) == PSP_O_RDONLY) {
                log.debug("PSP_O_RDONLY");
            }
            if ((flags & PSP_O_WRONLY) == PSP_O_WRONLY) {
                log.debug("PSP_O_WRONLY");
            }
            if ((flags & PSP_O_NBLOCK) == PSP_O_NBLOCK) {
                log.debug("PSP_O_NBLOCK");
            }
            if ((flags & PSP_O_DIROPEN) == PSP_O_DIROPEN) {
                log.debug("PSP_O_DIROPEN");
            }
            if ((flags & PSP_O_APPEND) == PSP_O_APPEND) {
                log.debug("PSP_O_APPEND");
            }
            if ((flags & PSP_O_CREAT) == PSP_O_CREAT) {
                log.debug("PSP_O_CREAT");
            }
            if ((flags & PSP_O_TRUNC) == PSP_O_TRUNC) {
                log.debug("PSP_O_TRUNC");
            }
            if ((flags & PSP_O_EXCL) == PSP_O_EXCL) {
                log.debug("PSP_O_EXCL");
            }
            if ((flags & PSP_O_NBUF) == PSP_O_NBUF) {
                log.debug("PSP_O_NBUF");
            }
            if ((flags & PSP_O_NOWAIT) == PSP_O_NOWAIT) {
                log.debug("PSP_O_NOWAIT");
            }
            if ((flags & PSP_O_PLOCK) == PSP_O_PLOCK) {
                log.debug("PSP_O_PLOCK");
            }
            if ((flags & PSP_O_FGAMEDATA) == PSP_O_FGAMEDATA) {
                log.debug("PSP_O_FGAMEDATA");
            }
        }
        String mode = getMode(flags);
        if (mode == null) {
            log.error("hleIoOpen - unhandled flags " + Integer.toHexString(flags));
            for (IIoListener ioListener : ioListeners) {
                ioListener.sceIoOpen(-1, filename_addr, filename, flags, permissions, mode);
            }
            return -1;
        }
        //Retry count.
        int retry = (flags >> 16) & 0x000F;

        if (retry != 0) {
            log.info("hleIoOpen - retry count is " + retry);
        }
        if ((flags & PSP_O_RDONLY) == PSP_O_RDONLY &&
                (flags & PSP_O_APPEND) == PSP_O_APPEND) {
            log.warn("hleIoOpen - read and append flags both set!");
        }

        IoInfo info = null;
        int result;
        try {
            String pcfilename = getDeviceFilePath(filename);
            if (pcfilename != null) {
                if (log.isDebugEnabled()) {
                    log.debug("hleIoOpen - opening file " + pcfilename);
                }
                if (isUmdPath(pcfilename)) {
                    // Check umd is mounted.
                    if (iso == null) {
                        log.error("hleIoOpen - no umd mounted");
                        result = ERROR_ERRNO_DEVICE_NOT_FOUND;
                    // Check umd is activated.
                    } else if (!Modules.sceUmdUserModule.isUmdActivated()) {
                        log.warn("hleIoOpen - umd mounted but not activated");
                        result = ERROR_KERNEL_NO_SUCH_DEVICE;
                    // Check flags are valid.
                    } else if ((flags & PSP_O_WRONLY) == PSP_O_WRONLY ||
                            (flags & PSP_O_CREAT) == PSP_O_CREAT ||
                            (flags & PSP_O_TRUNC) == PSP_O_TRUNC) {
                        log.error("hleIoOpen - refusing to open umd media for write");
                        result = ERROR_ERRNO_READ_ONLY;
                    } else {
                        // Open file.
                        try {
                            String trimmedFileName = trimUmdPrefix(pcfilename);
                            UmdIsoFile file = iso.getFile(trimmedFileName);
                            info = new IoInfo(filename, file, mode, flags, permissions);
                            if (!info.isValidId()) {
                            	// Too many open files...
                            	log.warn(String.format("hleIoOpen - too many open files"));
                            	result = ERROR_KERNEL_TOO_MANY_OPEN_FILES;
                            	// Return immediately the error, even in async mode
                            	async = false;
                            } else {
	                            if (trimmedFileName != null && trimmedFileName.length() == 0) {
	                                // Opening "umd0:" is allowing to read the whole UMD per sectors.
	                                info.sectorBlockMode = true;
	                            }
	                            info.result = ERROR_KERNEL_NO_ASYNC_OP;
	                            result = info.id;
	                            if (log.isDebugEnabled()) {
	                                log.debug("hleIoOpen assigned id = 0x" + Integer.toHexString(info.id));
	                            }
                            }
                        } catch (FileNotFoundException e) {
                            if (log.isDebugEnabled()) {
                                log.debug("hleIoOpen - umd file not found (ok to ignore this message, debug purpose only)");
                            }
                            result = ERROR_ERRNO_FILE_NOT_FOUND;
                        } catch (IOException e) {
                            log.error("hleIoOpen - error opening umd media: " + e.getMessage());
                            result = -1;
                        }
                    }
                } else {
                    // First check if the file already exists
                    File file = new File(pcfilename);
                    if (file.exists() && (flags & PSP_O_CREAT) == PSP_O_CREAT &&
                            (flags & PSP_O_EXCL) == PSP_O_EXCL) {
                        if (log.isDebugEnabled()) {
                            log.debug("hleIoOpen - file already exists (PSP_O_CREAT + PSP_O_EXCL)");
                        }
                        result = ERROR_ERRNO_FILE_ALREADY_EXISTS;
                    } else {
                    	// When PSP_O_CREAT is specified, create the parent directories
                    	// if they do not yet exist.
                        if (!file.exists() && ((flags & PSP_O_CREAT) == PSP_O_CREAT)) {
                        	String parentDir = new File(pcfilename).getParent();
                        	new File(parentDir).mkdirs();
                        }

                        SeekableRandomFile raf = new SeekableRandomFile(pcfilename, mode);
                        info = new IoInfo(filename, raf, mode, flags, permissions);
                        if ((flags & PSP_O_WRONLY) == PSP_O_WRONLY &&
                                (flags & PSP_O_TRUNC) == PSP_O_TRUNC) {
                            // When writing, PSP_O_TRUNC resets the file to be written (truncate to 0 length).
                            info.truncate(0);
                        }
                        info.result = ERROR_KERNEL_NO_ASYNC_OP; // sceIoOpenAsync will set this properly
                        result = info.id;
                        if (log.isDebugEnabled()) {
                            log.debug("hleIoOpen assigned id = 0x" + Integer.toHexString(info.id));
                        }
                    }
                }
            } else {
                result = -1;
            }
        } catch (FileNotFoundException e) {
            // To be expected under mode="r" and file doesn't exist
            if (log.isDebugEnabled()) {
                log.debug("hleIoOpen - file not found (ok to ignore this message, debug purpose only)");
            }
            result = ERROR_ERRNO_FILE_NOT_FOUND;
        }

        for (IIoListener ioListener : ioListeners) {
            ioListener.sceIoOpen(result, filename_addr, filename, flags, permissions, mode);
        }

        if (async) {
        	int realResult = result;
            if (info == null) {
                log.debug("sceIoOpenAsync - file not found (ok to ignore this message, debug purpose only)");
                // For async we still need to make and return a file handle even if we couldn't open the file,
                // this is so the game can query on the handle (wait/async stat/io callback).
                info = new IoInfo(readStringZ(filename_addr), null, null, flags, permissions);
                result = info.id;
            }

            startIoAsync(info, realResult, IoOperation.open, null, 0);
        }

        return result;
    }

    private int hleIoClose(int id, boolean async) {
        int result;

        if (log.isDebugEnabled()) {
            log.debug("hleIoClose - id " + Integer.toHexString(id));
        }

        IoInfo info = fileIds.get(id);
        if (async) {
            if (info != null) {
                if (info.asyncPending || info.asyncResultPending) {
                    log.warn("sceIoCloseAsync - id " + Integer.toHexString(id) + " PSP_ERROR_ASYNC_BUSY");
                    result = ERROR_KERNEL_ASYNC_BUSY;
                } else {
                    info.closePending = true;
                    result = (int) updateResult(info, 0, true, false, IoOperation.close);
                }
            } else {
                result = ERROR_KERNEL_BAD_FILE_DESCRIPTOR;
            }
        } else {
            try {
                if (info == null) {
                    if (id != 1 && id != 2) { // Ignore stdout and stderr.
                        log.warn("sceIoClose - unknown id " + Integer.toHexString(id));
                    }
                    result = ERROR_KERNEL_BAD_FILE_DESCRIPTOR;
                } else {
                    if (info.readOnlyFile != null) {
                        // Can be just closing an empty handle, because hleIoOpen(async==true)
                        // generates a dummy IoInfo when the file could not be opened.
                        info.readOnlyFile.close();
                    }
                    info.close();
                    triggerAsyncThread(info);
                    info.result = 0;
                    result = 0;
                }
            } catch (IOException e) {
                log.error("pspiofilemgr - error closing file: " + e.getMessage());
                e.printStackTrace();
                result = -1;
            }
            for (IIoListener ioListener : ioListeners) {
                ioListener.sceIoClose(result, id);
            }
        }

        return result;
    }

    private int hleIoWrite(int id, int data_addr, int size, boolean async) {
        IoInfo info = null;
        int result;

        if (id == STDOUT_ID) {
            // stdout
            String message = Utilities.stripNL(readStringNZ(data_addr, size));
            stdout.info(message);
            result = size;
        } else if (id == STDERR_ID) {
            // stderr
            String message = Utilities.stripNL(readStringNZ(data_addr, size));
            stderr.info(message);
            result = size;
        } else {
            if (log.isDebugEnabled()) {
                log.debug("hleIoWrite(id=" + Integer.toHexString(id) + ",data=0x" + Integer.toHexString(data_addr) + ",size=0x" + Integer.toHexString(size) + ") async=" + async);
            }
            try {
                info = fileIds.get(id);
                if (info == null) {
                    log.warn("hleIoWrite - unknown id " + Integer.toHexString(id));
                    result = ERROR_KERNEL_BAD_FILE_DESCRIPTOR;
                } else if (info.asyncPending || info.asyncResultPending) {
                    log.warn("hleIoWrite - id " + Integer.toHexString(id) + " PSP_ERROR_ASYNC_BUSY");
                    result = ERROR_KERNEL_ASYNC_BUSY;
                } else if ((data_addr < MemoryMap.START_RAM) && (data_addr + size > MemoryMap.END_RAM)) {
                    log.warn("hleIoWrite - id " + Integer.toHexString(id) + " data is outside of ram 0x" + Integer.toHexString(data_addr) + " - 0x" + Integer.toHexString(data_addr + size));
                    result = -1;
                } else {
                    if ((info.flags & PSP_O_APPEND) == PSP_O_APPEND) {
                        info.msFile.seek(info.msFile.length());
                        info.position = info.msFile.length();
                    }

                    if (info.position > info.readOnlyFile.length()) {
                        byte[] junk = new byte[512];
                        int towrite = (int) (info.position - info.readOnlyFile.length());

                        info.msFile.seek(info.msFile.length());
                        while (towrite >= 512) {
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
            } catch (IOException e) {
                e.printStackTrace();
                result = -1;
            }
        }
        result = (int) updateResult(info, result, async, false, IoOperation.write, null, size);
        for (IIoListener ioListener : ioListeners) {
            ioListener.sceIoWrite(result, id, data_addr, size, size);
        }

        return result;
    }

    public int hleIoRead(int id, int data_addr, int size, boolean async) {
        if (log.isDebugEnabled()) {
            log.debug("hleIoRead(id=" + Integer.toHexString(id) + ",data=0x" + Integer.toHexString(data_addr) + ",size=0x" + Integer.toHexString(size) + ") async=" + async);
        }
        IoInfo info = null;
        int result;
        long position = 0;
        SeekableDataInput dataInput = null;
        int requestedSize = size;
        IAction asyncAction = null;

        if (id == STDIN_ID) { // stdin
            log.warn("UNIMPLEMENTED:hleIoRead id = stdin");
            result = 0;
        } else {
            try {
                info = fileIds.get(id);
                if (info == null) {
                    log.warn("hleIoRead - unknown id " + Integer.toHexString(id));
                    result = ERROR_KERNEL_BAD_FILE_DESCRIPTOR;
                } else if (info.asyncPending || info.asyncResultPending) {
                    log.warn("hleIoRead - id " + Integer.toHexString(id) + " PSP_ERROR_ASYNC_BUSY");
                    result = ERROR_KERNEL_ASYNC_BUSY;
                } else if ((data_addr < MemoryMap.START_RAM) && (data_addr + size > MemoryMap.END_RAM)) {
                    log.warn("hleIoRead - id " + Integer.toHexString(id) + " data is outside of ram 0x" + Integer.toHexString(data_addr) + " - 0x" + Integer.toHexString(data_addr + size));
                    result = ERROR_KERNEL_FILE_READ_ERROR;
                } else if ((info.readOnlyFile == null) || (info.position >= info.readOnlyFile.length())) {
                    // Ignore empty handles and allow seeking off the end of the file, just return 0 bytes read/written.
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
                        size = (int) (info.readOnlyFile.length() - info.readOnlyFile.getFilePointer());
                        if (log.isDebugEnabled()) {
                        	log.debug("hleIoRead - clamping size old=" + oldSize + " new=" + size + " fp=" + info.readOnlyFile.getFilePointer() + " len=" + info.readOnlyFile.length());
                        }
                    }

                    if (async) {
                    	// Execute the read operation in the IO async thread
                    	asyncAction = new IOAsyncReadAction(info, data_addr, requestedSize, size);
                    	result = 0;
                    } else {
                    	position = info.position;
                    	dataInput = info.readOnlyFile;
                    	Utilities.readFully(info.readOnlyFile, data_addr, size);
                    	info.position += size;
                    	result = size;

                    	// Invalidate any compiled code in the read range
                    	RuntimeContext.invalidateRange(data_addr, size);

                    	if (info.sectorBlockMode) {
                    		result /= UmdIsoFile.sectorLength;
                    	}
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                result = ERROR_KERNEL_FILE_READ_ERROR;
            } catch (Exception e) {
                e.printStackTrace();
                result = ERROR_KERNEL_FILE_READ_ERROR;
                log.error("hleIoRead: Check other console for exception details. Press Run to continue.");
                Emulator.PauseEmu();
            }
        }
        result = (int) updateResult(info, result, async, false, IoOperation.read, asyncAction, size);
        // Call the IO listeners (performed in the async action if one is provided, otherwise call them here)
        if (asyncAction == null) {
            for (IIoListener ioListener : ioListeners) {
                ioListener.sceIoRead(result, id, data_addr, requestedSize, size, position, dataInput);
            }
        }
        
        return result;
    }

    private long hleIoLseek(int id, long offset, int whence, boolean resultIs64bit, boolean async) {
        IoInfo info = null;
        long result;

        if (id == STDOUT_ID || id == STDERR_ID || id == STDIN_ID) { // stdio
            log.error("seek - can't seek on stdio id " + Integer.toHexString(id));
            result = -1;
        } else {
            try {
                info = fileIds.get(id);
                if (info == null) {
                    log.warn("seek - unknown id " + Integer.toHexString(id));
                    result = ERROR_KERNEL_BAD_FILE_DESCRIPTOR;
                } else if (info.asyncPending || info.asyncResultPending) {
                    log.warn("seek - id " + Integer.toHexString(id) + " PSP_ERROR_ASYNC_BUSY");
                    result = ERROR_KERNEL_ASYNC_BUSY;
                } else if (info.readOnlyFile == null) {
                    // Ignore empty handles.
                    result = 0;
                } else {
                    if (info.sectorBlockMode) {
                        // In sectorBlockMode, the offset is a sector number
                        offset *= UmdIsoFile.sectorLength;
                    }

                    switch (whence) {
                        case PSP_SEEK_SET:
                            if (offset < 0) {
                                log.warn("SEEK_SET id " + Integer.toHexString(id) + " filename:'" + info.filename + "' offset=0x" + Long.toHexString(offset) + " (less than 0!)");
                                result = ERROR_INVALID_ARGUMENT;
                                for (IIoListener ioListener : ioListeners) {
                                    ioListener.sceIoSeek64(ERROR_INVALID_ARGUMENT, id, offset, whence);
                                }
                                return result;
                            }
                            info.position = offset;

                            if (offset < info.readOnlyFile.length()) {
                                info.readOnlyFile.seek(offset);
                            }
                            break;
                        case PSP_SEEK_CUR:
                            if (info.position + offset < 0) {
                                log.warn("SEEK_CUR id " + Integer.toHexString(id) + " filename:'" + info.filename + "' newposition=0x" + Long.toHexString(info.position + offset) + " (less than 0!)");
                                result = ERROR_INVALID_ARGUMENT;
                                for (IIoListener ioListener : ioListeners) {
                                    ioListener.sceIoSeek64(ERROR_INVALID_ARGUMENT, id, offset, whence);
                                }
                                return result;
                            }
                            info.position += offset;

                            if (info.position < info.readOnlyFile.length()) {
                                info.readOnlyFile.seek(info.position);
                            }
                            break;
                        case PSP_SEEK_END:
                            if (info.readOnlyFile.length() + offset < 0) {
                                log.warn("SEEK_END id " + Integer.toHexString(id) + " filename:'" + info.filename + "' newposition=0x" + Long.toHexString(info.position + offset) + " (less than 0!)");
                                result = ERROR_INVALID_ARGUMENT;
                                for (IIoListener ioListener : ioListeners) {
                                    ioListener.sceIoSeek64(ERROR_INVALID_ARGUMENT, id, offset, whence);
                                }
                                return result;
                            }
                            info.position = info.readOnlyFile.length() + offset;

                            if (info.position < info.readOnlyFile.length()) {
                                info.readOnlyFile.seek(info.position);
                            }
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
            } catch (IOException e) {
                e.printStackTrace();
                result = -1;
            }
        }
        result = updateResult(info, result, async, resultIs64bit, IoOperation.seek);

        if (resultIs64bit) {
            for (IIoListener ioListener : ioListeners) {
                ioListener.sceIoSeek64(result, id, offset, whence);
            }
        } else {
            for (IIoListener ioListener : ioListeners) {
                ioListener.sceIoSeek32((int) result, id, (int) offset, whence);
            }
        }

        return result;
    }

    public int hleIoIoctl(int id, int cmd, int indata_addr, int inlen, int outdata_addr, int outlen, boolean async) {
        IoInfo info = null;
        int result;
        Memory mem = Memory.getInstance();

        if (log.isDebugEnabled()) {
            log.debug(String.format("hleIoIoctl(id=%x, cmd=0x%08X, indata=0x%08X, inlen=%d, outdata=0x%08X, outlen=%d, async=%b", id, cmd, indata_addr, inlen, outdata_addr, outlen, async));
            if (Memory.isAddressGood(indata_addr)) {
                for (int i = 0; i < inlen; i += 4) {
                    log.debug(String.format("hleIoIoctl indata[%d]=0x%08X", i / 4, mem.read32(indata_addr + i)));
                }
            }
            if (Memory.isAddressGood(outdata_addr)) {
                for (int i = 0; i < Math.min(outlen, 256); i += 4) {
                    log.debug(String.format("hleIoIoctl outdata[%d]=0x%08X", i / 4, mem.read32(outdata_addr + i)));
                }
            }
        }

        info = fileIds.get(id);
        if (info == null) {
            log.warn("hleIoIoctl - unknown id " + Integer.toHexString(id));
            result = ERROR_KERNEL_BAD_FILE_DESCRIPTOR;
        } else if (info.asyncPending || info.asyncResultPending) {
            // Can't execute another operation until the previous one completed
            log.warn("hleIoIoctl - id " + Integer.toHexString(id) + " PSP_ERROR_ASYNC_BUSY");
            result = ERROR_KERNEL_ASYNC_BUSY;
        } else {
            switch (cmd) {
                // UMD file seek set.
                case 0x01010005: {
                    if (Memory.isAddressGood(indata_addr) && inlen >= 4) {
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
								result = ERROR_KERNEL_FILE_READ_ERROR;
                            }
                        } else {
                            log.warn("hleIoIoctl cmd=0x01010005 only allowed on UMD files");
                            result = ERROR_INVALID_ARGUMENT;
                        }
                    } else {
                        log.warn("hleIoIoctl cmd=0x01010005 " + String.format("0x%08X %d", indata_addr, inlen) + " unsupported parameters");
                        result = ERROR_INVALID_ARGUMENT;
                    }
                    break;
                }
	            // Get UMD Primary Volume Descriptor
	            case 0x01020001: {
	                if (Memory.isAddressGood(outdata_addr) && outlen == UmdIsoFile.sectorLength) {
	                    if (info.isUmdFile() && iso != null) {
                            try {
                            	byte[] primaryVolumeSector = iso.readSector(UmdIsoReader.startSector);
                            	IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(outdata_addr, outlen, 1);
                            	for (int i = 0; i < outlen; i++)  {
                            		memoryWriter.writeNext(primaryVolumeSector[i] & 0xFF);
                            	}
                            	memoryWriter.flush();
                                result = 0;
							} catch (IOException e) {
								log.error(e);
								result = ERROR_KERNEL_FILE_READ_ERROR;
							}
	                    } else {
	                        log.warn("hleIoIoctl cmd=0x01020001 only allowed on UMD files");
	                        result = ERROR_INVALID_ARGUMENT;
	                    }
	                } else {
	                    log.warn("hleIoIoctl cmd=0x01020001 " + String.format("0x%08X %d", outdata_addr, outlen) + " unsupported parameters");
	                    result = SceKernelErrors.ERROR_ERRNO_INVALID_ARGUMENT;
	                }
	                break;
	            }
	            // Get UMD Path Table
	            case 0x01020002: {
	                if (Memory.isAddressGood(outdata_addr) && outlen <= UmdIsoFile.sectorLength) {
	                    if (info.isUmdFile() && iso != null) {
                            try {
                            	byte[] primaryVolumeSector = iso.readSector(UmdIsoReader.startSector);
                            	ByteBuffer primaryVolume = ByteBuffer.wrap(primaryVolumeSector);
                            	primaryVolume.position(140);
                            	int pathTableLocation = Utilities.readWord(primaryVolume);
                            	byte[] pathTableSector = iso.readSector(pathTableLocation);
                            	IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(outdata_addr, outlen, 1);
                            	for (int i = 0; i < outlen; i++)  {
                            		memoryWriter.writeNext(pathTableSector[i] & 0xFF);
                            	}
                            	memoryWriter.flush();
                                result = 0;
							} catch (IOException e) {
								log.error(e);
								result = ERROR_KERNEL_FILE_READ_ERROR;
							}
	                    } else {
	                        log.warn("hleIoIoctl cmd=0x01020002 only allowed on UMD files");
	                        result = ERROR_INVALID_ARGUMENT;
	                    }
	                } else {
	                    log.warn("hleIoIoctl cmd=0x01020002 " + String.format("0x%08X %d", outdata_addr, outlen) + " unsupported parameters");
	                    result = SceKernelErrors.ERROR_ERRNO_INVALID_ARGUMENT;
	                }
	                break;
	            }
                // Get Sector size
                case 0x01020003: {
                    if (Memory.isAddressGood(outdata_addr) && outlen == 4) {
	                    if (info.isUmdFile() && iso != null) {
	                    	mem.write32(outdata_addr, UmdIsoFile.sectorLength);
	                    	result = 0;
	                    } else {
	                        log.warn("hleIoIoctl cmd=0x01020003 only allowed on UMD files");
	                        result = ERROR_INVALID_ARGUMENT;
	                    }
	                } else {
	                    log.warn("hleIoIoctl cmd=0x01020003 " + String.format("0x%08X %d", outdata_addr, outlen) + " unsupported parameters");
	                    result = SceKernelErrors.ERROR_ERRNO_INVALID_ARGUMENT;
                    }
                    break;
                }
                // Get UMD file pointer.
                case 0x01020004: {
                    if (Memory.isAddressGood(outdata_addr) && outlen >= 4) {
                        if (info.isUmdFile()) {
                            try {
                                int fPointer = (int) info.readOnlyFile.getFilePointer();
                                mem.write32(outdata_addr, fPointer);
                                log.debug("hleIoIoctl umd file get file pointer " + fPointer);
                                result = 0;
                            } catch (IOException e) {
                                log.warn("hleIoIoctl cmd=0x01020004 exception: " + e.getMessage());
								result = ERROR_KERNEL_FILE_READ_ERROR;
                            }
                        } else {
                            log.warn("hleIoIoctl cmd=0x01020004 only allowed on UMD files");
                            result = ERROR_INVALID_ARGUMENT;
                        }
                    } else {
                        log.warn("hleIoIoctl cmd=0x01020004 " + String.format("0x%08X %d", outdata_addr, outlen) + " unsupported parameters");
                        result = ERROR_INVALID_ARGUMENT;
                    }
                    break;
                }
                // Get UMD file start sector.
                case 0x01020006: {
                    if (Memory.isAddressGood(outdata_addr) && outlen >= 4) {
                        int startSector = 0;
                        if (info.isUmdFile() && info.readOnlyFile instanceof UmdIsoFile) {
                            UmdIsoFile file = (UmdIsoFile) info.readOnlyFile;
                            startSector = file.getStartSector();
                            log.debug("hleIoIoctl umd file get start sector " + startSector);
                            mem.write32(outdata_addr, startSector);
                            result = 0;
                        } else {
                            log.warn("hleIoIoctl cmd=0x01020006 only allowed on UMD files and only implemented for UmdIsoFile");
                            result = ERROR_INVALID_ARGUMENT;
                        }
                    } else {
                        log.warn("hleIoIoctl cmd=0x01020006 " + String.format("0x%08X %d", outdata_addr, outlen) + " unsupported parameters");
                        result = ERROR_INVALID_ARGUMENT;
                    }
                    break;
                }
                // Get UMD file length in bytes.
                case 0x01020007: {
                    if (Memory.isAddressGood(outdata_addr) && outlen >= 8) {
                        if (info.isUmdFile()) {
                            try {
                                long length = info.readOnlyFile.length();
                                mem.write64(outdata_addr, length);
                                log.debug("hleIoIoctl get file size " + length);
                                result = 0;
                            } catch (IOException e) {
                                // Should never happen?
                                log.warn("hleIoIoctl cmd=0x01020007 exception: " + e.getMessage());
								result = ERROR_KERNEL_FILE_READ_ERROR;
                            }
                        } else {
                            log.warn("hleIoIoctl cmd=0x01020007 only allowed on UMD files");
                            result = ERROR_INVALID_ARGUMENT;
                        }
                    } else {
                        log.warn("hleIoIoctl cmd=0x01020007 " + String.format("0x%08X %d", outdata_addr, outlen) + " unsupported parameters");
                        result = ERROR_INVALID_ARGUMENT;
                    }
                    break;
                }
                // Read UMD file.
                case 0x01030008: {
                    if (Memory.isAddressGood(indata_addr) && inlen >= 4) {
                    	int length = mem.read32(indata_addr);
                    	if (length > 0) {
                    		if (Memory.isAddressGood(outdata_addr) && outlen >= length) {
                                try {
									Utilities.readFully(info.readOnlyFile, outdata_addr, length);
	                                info.position += length;
	                                result = length;
								} catch (IOException e) {
									log.error(e);
									result = ERROR_KERNEL_FILE_READ_ERROR;
								}
                    		} else {
                                log.warn(String.format("hleIoIoctl cmd=0x%08X inlen=%d unsupported output parameters 0x%08X %d", cmd, inlen, outdata_addr, outlen));
                                result = ERROR_INVALID_ARGUMENT;
                    		}
                    	} else {
                            log.warn(String.format("hleIoIoctl cmd=0x%08X unsupported input parameters 0x%08X %d, length=%d", cmd, indata_addr, inlen, length));
                            result = ERROR_INVALID_ARGUMENT;
                    	}
                    } else {
                        log.warn(String.format("hleIoIoctl cmd=0x%08X unsupported input parameters 0x%08X %d", cmd, indata_addr, inlen));
                        result = ERROR_INVALID_ARGUMENT;
                    }
                    break;
                }
                // UMD disc read sectors operation.
                case 0x01F30003: {
                    if (Memory.isAddressGood(indata_addr) && inlen >= 4) {
                    	int numberOfSectors = mem.read32(indata_addr);
                    	if (numberOfSectors > 0) {
                    		if (Memory.isAddressGood(outdata_addr) && outlen >= numberOfSectors) {
                                try {
                                	int length = numberOfSectors * UmdIsoFile.sectorLength;
									Utilities.readFully(info.readOnlyFile, outdata_addr, length);
	                                info.position += length;
	                                result = length / UmdIsoFile.sectorLength;
								} catch (IOException e) {
									log.error(e);
									result = ERROR_KERNEL_FILE_READ_ERROR;
								}
                    		} else {
                                log.warn(String.format("hleIoIoctl cmd=0x%08X inlen=%d unsupported output parameters 0x%08X %d", cmd, inlen, outdata_addr, outlen));
                                result = ERROR_ERRNO_INVALID_ARGUMENT;
                    		}
                    	} else {
                            log.warn(String.format("hleIoIoctl cmd=0x%08X unsupported input parameters 0x%08X %d numberOfSectors=%d", cmd, indata_addr, inlen, numberOfSectors));
                            result = ERROR_ERRNO_INVALID_ARGUMENT;
                    	}
                    } else {
                        log.warn(String.format("hleIoIoctl cmd=0x%08X unsupported input parameters 0x%08X %d", cmd, indata_addr, inlen));
                        result = ERROR_ERRNO_INVALID_ARGUMENT;
                    }
                    break;
                }
                // UMD file seek whence.
                case 0x01F100A6: {
                    if (Memory.isAddressGood(indata_addr) && inlen >= 16) {
                        if (info.isUmdFile()) {
                            try {
                                long offset = mem.read64(indata_addr);
                                int whence = mem.read32(indata_addr + 12);
                            	if (info.sectorBlockMode) {
                            		offset *= UmdIsoFile.sectorLength;
                            	}
                                if (log.isDebugEnabled()) {
                                    log.debug("hleIoIoctl UMD file seek offset " + offset + ", whence " + whence);
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
                            result = ERROR_INVALID_ARGUMENT;
                        }
                    } else {
                        log.warn("hleIoIoctl cmd=0x01F100A6 " + String.format("0x%08X %d", indata_addr, inlen) + " unsupported parameters");
                        result = ERROR_INVALID_ARGUMENT;
                    }
                    break;
                }
                // Define decryption key (DRM by amctrl.prx).
                case 0x04100001: {
                    if (Memory.isAddressGood(indata_addr) && inlen == 16) {
                        CryptoEngine crypto = new CryptoEngine();
                        String keyHex = "";

                        if (pgdFileConnector == null) {
                            pgdFileConnector = new PGDFileConnector();
                        }

                        // Store the key.
                        byte[] keyBuf = new byte[0x10];
                        for (int i = 0; i < 0x10; i++) {
                            keyBuf[i] = (byte) mem.read8(indata_addr + i);
                            keyHex += String.format("%02x", keyBuf[i] & 0xFF);
                        }

                        if(getAllowExtractPGDStatus()) {
                            // Extract the encrypted PGD file for external decryption.
                            pgdFileConnector.extractPGDFile(info.filename, info.readOnlyFile, keyHex);
                        }

                        // Check for an already decrypted file.
                        SeekableDataInput decInput = pgdFileConnector.loadDecryptedPGDFile(info.filename);
                        if (decInput != null) {
                            info.readOnlyFile = decInput;
                        } else {
                            // Try to decrypt this PGD file with the Crypto Engine.
                            try {
                                // Generate the necessary directories and files.
                                String pgdPath = pgdFileConnector.getBaseDirectory(pgdFileConnector.id);
                                new File(pgdPath).mkdirs();
                                String decFileName = pgdFileConnector.getCompleteFileName(PGDFileConnector.decryptedFileName);
                                SeekableRandomFile decFile = new SeekableRandomFile(decFileName, "rw");

                                // Maximum 16-byte aligned block size to use during stream read/write.
                                int maxAlignedChunkSize = 0x4EF0;

                                // PGD hash header size.
                                int pgdHeaderSize = 0xA0;

                                // Setup the buffers.
                                byte[] inBuf = new byte[maxAlignedChunkSize + pgdHeaderSize];
                                byte[] outBuf = new byte[maxAlignedChunkSize + 0x10];
                                byte[] headerBuf = new byte[0x30 + 0x10];
                                byte[] hashBuf = new byte[0x10];

                                // Read the encrypted PGD header.
                                info.readOnlyFile.readFully(inBuf, 0, pgdHeaderSize);

                                // Decrypt 0x30 bytes at offset 0x30 to expose the first header.
                                System.arraycopy(inBuf, 0x10, headerBuf, 0, 0x10);
                                System.arraycopy(inBuf, 0x30, headerBuf, 0x10, 0x30);
                                byte headerBufDec[] = crypto.DecryptPGD(headerBuf, 0x30 + 0x10, keyBuf);

                                // Extract the decrypting parameters.
                                System.arraycopy(headerBufDec, 0, hashBuf, 0, 0x10);
                                int dataSize = (headerBufDec[0x14] & 0xFF) | ((headerBufDec[0x15] & 0xFF) << 8) |
                                        ((headerBufDec[0x16] & 0xFF) << 16) | ((headerBufDec[0x17] & 0xFF) << 24);
                                int chunkSize = (headerBufDec[0x18] & 0xFF) | ((headerBufDec[0x19] & 0xFF) << 8) |
                                        ((headerBufDec[0x1A] & 0xFF) << 16) | ((headerBufDec[0x1B] & 0xFF) << 24);
                                int hashOffset = (headerBufDec[0x1C] & 0xFF) | ((headerBufDec[0x1D] & 0xFF) << 8) |
                                        ((headerBufDec[0x1E] & 0xFF) << 16) | ((headerBufDec[0x1F] & 0xFF) << 24);
                                if (log.isDebugEnabled()) {
                                	log.debug(String.format("PGD dataSize=%d, chunkSize=%d, hashOffset=%d", dataSize, chunkSize, hashOffset));
                                }

                                // Write the newly extracted hash at the top of the output buffer,
                                // locate the data hash at hashOffset and start decrypting until
                                // dataSize is reached.

                                // If the data is smaller than maxAlignedChunkSize, decrypt it right away.
                                if (dataSize <= maxAlignedChunkSize) {
                                    info.readOnlyFile.seek(hashOffset);
                                    info.readOnlyFile.readFully(inBuf, 0xA0, dataSize);

                                    System.arraycopy(hashBuf, 0, outBuf, 0, 0x10);
                                    System.arraycopy(inBuf, 0xA0, outBuf, 0x10, dataSize);
                                    decFile.write(crypto.DecryptPGD(outBuf, dataSize + 0x10, keyBuf));
                                } else {
                                    // Read and decrypt the first chunk of data.
                                    info.readOnlyFile.seek(hashOffset);
                                    info.readOnlyFile.readFully(inBuf, 0xA0, maxAlignedChunkSize);

                                    System.arraycopy(hashBuf, 0, outBuf, 0, 0x10);
                                    System.arraycopy(inBuf, 0xA0, outBuf, 0x10, maxAlignedChunkSize);
                                    decFile.write(crypto.DecryptPGD(outBuf, maxAlignedChunkSize + 0x10, keyBuf));

                                    // Keep reading and decrypting data by updating the PGD cipher.
                                    for (int i = 0; i < dataSize; i += maxAlignedChunkSize) {
                                        info.readOnlyFile.readFully(inBuf, 0xA0, maxAlignedChunkSize);

                                        System.arraycopy(hashBuf, 0, outBuf, 0, 0x10);
                                        System.arraycopy(inBuf, 0xA0, outBuf, 0x10, maxAlignedChunkSize);
                                        decFile.write(crypto.UpdatePGDCipher(outBuf, maxAlignedChunkSize + 0x10));
                                    }
                                }

                                // Finish the PGD cipher operations, set the real file length and close it.
                                crypto.FinishPGDCipher();
                                decFile.setLength(dataSize);
                                decFile.close();
                            } catch (Exception e) {
                                log.error(e);
                            }

                            try {
                                info.readOnlyFile.seek(info.position);
                            } catch (IOException e) {
                                log.error(e);
                            }

                            if (log.isDebugEnabled()) {
                                log.debug("hleIoIoctl get AES key " + keyHex);
                            }

                            // Load the manually decrypted file generated just now.
                            info.readOnlyFile = pgdFileConnector.loadDecryptedPGDFile(info.filename);
                        }

                        result = 0;
                    } else {
                        log.warn("hleIoIoctl cmd=0x04100001 " + String.format("0x%08X %d", indata_addr, inlen) + " unsupported parameters");
                        result = ERROR_INVALID_ARGUMENT;
                    }
                    break;
                }

                default: {
                	result = -1;
                    log.warn(String.format("hleIoIoctl 0x%08X unknown command, inlen=%d, outlen=%d", cmd, inlen, outlen));
                    if (Memory.isAddressGood(indata_addr)) {
                        for (int i = 0; i < inlen; i += 4) {
                            log.warn(String.format("hleIoIoctl indata[%d]=0x%08X", i / 4, mem.read32(indata_addr + i)));
                        }
                    }
                    if (Memory.isAddressGood(outdata_addr)) {
                        for (int i = 0; i < Math.min(outlen, 256); i += 4) {
                            log.warn(String.format("hleIoIoctl outdata[%d]=0x%08X", i / 4, mem.read32(outdata_addr + i)));
                        }
                    }
                    break;
                }
            }
        }
        result = (int) updateResult(info, result, async, false, IoOperation.ioctl);
        for (IIoListener ioListener : ioListeners) {
            ioListener.sceIoIoctl(result, id, cmd, indata_addr, inlen, outdata_addr, outlen);
        }

        return result;
    }

    /**
     * sceIoPollAsync
     * 
     * @param id
     * @param res_addr
     * 
     * @return
     */
    @HLEFunction(nid = 0x3251EA56, version = 150, checkInsideInterrupt = true)
    public int sceIoPollAsync(int id, int res_addr) {
        if (log.isDebugEnabled()) {
            log.debug("sceIoPollAsync redirecting to hleIoWaitAsync");
        }

        return hleIoWaitAsync(id, res_addr, false, false);
    }

    /**
     * sceIoWaitAsync
     * 
     * @param id
     * @param res_addr
     * 
     * @return
     */
    @HLEFunction(nid = 0xE23EEC33, version = 150, checkInsideInterrupt = true)
    public int sceIoWaitAsync(int id, int res_addr) {
        if (log.isDebugEnabled()) {
            log.debug("sceIoWaitAsync redirecting to hleIoWaitAsync");
        }
        
        return hleIoWaitAsync(id, res_addr, true, false);
    }

    /**
     * sceIoWaitAsyncCB
     * 
     * @param id
     * @param res_addr
     * 
     * @return
     */
    @HLEFunction(nid = 0x35DBD746, version = 150, checkInsideInterrupt = true)
    public int sceIoWaitAsyncCB(int id, int res_addr) {
        if (log.isDebugEnabled()) {
            log.debug("sceIoWaitAsyncCB redirecting to hleIoWaitAsync");
        }

        return hleIoWaitAsync(id, res_addr, true, true);
    }

    /**
     * sceIoGetAsyncStat
     * 
     * @param id
     * @param poll
     * @param res_addr
     * 
     * @return
     */
    @HLEFunction(nid = 0xCB05F8D6, version = 150, checkInsideInterrupt = true)
    public int sceIoGetAsyncStat(int id, int poll, int res_addr) {
        if (log.isDebugEnabled()) {
            log.debug("sceIoGetAsyncStat poll=0x" + Integer.toHexString(poll) + " redirecting to hleIoWaitAsync");
        }
        
        return hleIoWaitAsync(id, res_addr, (poll == 0), false);
    }

    /**
     * sceIoChangeAsyncPriority
     * 
     * @param id
     * @param priority
     * 
     * @return
     */
    @HLEFunction(nid = 0xB293727F, version = 150, checkInsideInterrupt = true)
    public int sceIoChangeAsyncPriority(int id, int priority) {
        if (log.isDebugEnabled()) {
            log.debug("sceIoChangeAsyncPriority id=0x" + Integer.toHexString(id) + ", priority=0x" + Integer.toHexString(priority));
        }
        
        int result;
        
        if (priority == -1) {
            priority = Modules.ThreadManForUserModule.getCurrentThread().currentPriority;
        }
        if (priority < 0) {
        	result = SceKernelErrors.ERROR_KERNEL_ILLEGAL_PRIORITY;
        } else if (id == -1) {
            defaultAsyncPriority = priority;
            result = 0;
        } else {
            IoInfo info = fileIds.get(id);
            if (info != null) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("sceIoChangeAsyncPriority changing priority of async thread from fd=%x to %d", info.id, priority));
                }
                info.asyncThreadPriority = priority;
                result = 0;
                Modules.ThreadManForUserModule.hleKernelChangeThreadPriority(info.asyncThread, priority);
            } else {
                log.warn("sceIoChangeAsyncPriority invalid fd=" + id);
                result = SceKernelErrors.ERROR_KERNEL_BAD_FILE_DESCRIPTOR;
            }
        }
        
        return result;
    }

    /**
     * sceIoSetAsyncCallback
     * 
     * @param id
     * @param cbid
     * @param notifyArg
     * 
     * @return
     */
    @HLEFunction(nid = 0xA12A0514, version = 150, checkInsideInterrupt = true)
    public int sceIoSetAsyncCallback(int id, int cbid, int notifyArg) {
        if (log.isDebugEnabled()) {
            log.debug("sceIoSetAsyncCallback - id " + Integer.toHexString(id) + " cbid " + Integer.toHexString(cbid) + " arg 0x" + Integer.toHexString(notifyArg));
        }
        
        IoInfo info = fileIds.get(id);
        int result;
        
        if (info == null) {
            log.warn("sceIoSetAsyncCallback - unknown id " + Integer.toHexString(id));
            result = ERROR_KERNEL_BAD_FILE_DESCRIPTOR;
        } else {
            if (Modules.ThreadManForUserModule.hleKernelRegisterCallback(SceKernelThreadInfo.THREAD_CALLBACK_IO, cbid)) {
                info.cbid = cbid;
                info.notifyArg = notifyArg;
                triggerAsyncThread(info);
                result = 0;
            } else {
                log.warn("sceIoSetAsyncCallback - not a callback id " + Integer.toHexString(id));
                result = -1;
            }
        }
        
        return result;
    }

    /**
     * sceIoClose
     * 
     * @param id
     * 
     * @return
     */
    @HLEFunction(nid = 0x810C4BC3, version = 150, checkInsideInterrupt = true)
    public int sceIoClose(int id) {
        if (log.isDebugEnabled()) {
            log.debug("sceIoClose redirecting to hleIoClose");
        }

        int result = hleIoClose(id, false);
        delayIoOperation(IoOperation.close);

        return result;
    }

    /**
     * sceIoCloseAsync
     * 
     * @param id
     * 
     * @return
     */
    @HLEFunction(nid = 0xFF5940B6, version = 150, checkInsideInterrupt = true)
    public int sceIoCloseAsync(int id) {
        if (log.isDebugEnabled()) {
            log.debug("sceIoCloseAsync redirecting to hleIoClose");
        }

        return hleIoClose(id, true);
    }

    /**
     * sceIoOpen
     * 
     * @param filename
     * @param flags
     * @param permissions
     * 
     * @return
     */
    @HLEFunction(nid = 0x109F50BC, version = 150, checkInsideInterrupt = true)
    public int sceIoOpen(PspString filename, int flags, int permissions) {
        if (log.isDebugEnabled()) {
            log.debug("sceIoOpen redirecting to hleIoOpen");
        }
        
        int result = hleIoOpen(filename, flags, permissions, /* async = */ false);
        delayIoOperation(IoOperation.open);
        return result;
    }

    /**
     * sceIoOpenAsync
     * 
     * @param filename
     * @param flags
     * @param permissions
     * 
     * @return
     */
    @HLEFunction(nid = 0x89AA9906, version = 150, checkInsideInterrupt = true)
    public int sceIoOpenAsync(PspString filename, int flags, int permissions) {
        if (log.isDebugEnabled()) {
            log.debug("sceIoOpenAsync redirecting to hleIoOpen");
        }
        
        return hleIoOpen(filename, flags, permissions, /* async = */ true);
    }

    /**
     * sceIoRead
     * 
     * @param id
     * @param data_addr
     * @param size
     * 
     * @return
     */
    @HLEFunction(nid = 0x6A638D83, version = 150, checkInsideInterrupt = true)
    public int sceIoRead(int id, int data_addr, int size) {
        int result = hleIoRead(id, data_addr, size, false);
        delayIoOperation(IoOperation.read);
        return result;
    }

    /**
     * sceIoReadAsync
     * 
     * @param id
     * @param data_addr
     * @param size
     * 
     * @return
     */
    @HLEFunction(nid = 0xA0B5A7C2, version = 150, checkInsideInterrupt = true)
    public int sceIoReadAsync(int id, int data_addr, int size) {
        return hleIoRead(id, data_addr, size, true);
    }

    /**
     * sceIoWrite
     * 
     * @param id
     * @param data_addr
     * @param size
     * 
     * @return
     */
    @HLEFunction(nid = 0x42EC03AC, version = 150, checkInsideInterrupt = true)
    public int sceIoWrite(int id, int data_addr, int size) {
        int result = hleIoWrite(id, data_addr, size, false);

        // Do not delay output on stdout/stderr
        if (id != STDOUT_ID && id != STDERR_ID) {
        	delayIoOperation(IoOperation.write);
        }
        
        return result;
    }

    /**
     * sceIoWriteAsync
     * 
     * @param id
     * @param data_addr
     * @param size
     * 
     * @return
     */
    @HLEFunction(nid = 0x0FACAB19, version = 150, checkInsideInterrupt = true)
    public int sceIoWriteAsync(int id, int data_addr, int size) {
        return hleIoWrite(id, data_addr, size, true);
    }

    /**
     * sceIoLseek
     * 
     * @param id
     * @param offset
     * @param whence
     * 
     * @return
     */
    @HLEFunction(nid = 0x27EB27B8, version = 150, checkInsideInterrupt = true)
    public long sceIoLseek(int id, long offset, int whence) {
        if (log.isDebugEnabled()) {
            log.debug("sceIoLseek - id " + Integer.toHexString(id) + " offset " + offset + " (hex=0x" + Long.toHexString(offset) + ") whence " + getWhenceName(whence));
        }

        long result = hleIoLseek(id, offset, whence, true, false);
        delayIoOperation(IoOperation.seek);
        return result;
    }

    /**
     * sceIoLseekAsync
     * 
     * @param id
     * @param offset
     * @param whence
     * 
     * @return
     */
    @HLEFunction(nid = 0x71B19E77, version = 150, checkInsideInterrupt = true)
    public int sceIoLseekAsync(int id, long offset, int whence) {
        if (log.isDebugEnabled()) {
            log.debug("sceIoLseekAsync - id " + Integer.toHexString(id) + " offset " + offset + " (hex=0x" + Long.toHexString(offset) + ") whence " + getWhenceName(whence));
        }

        return (int) hleIoLseek(id, offset, whence, true, true);
    }

    /**
     * sceIoLseek32
     * 
     * @param id
     * @param offset
     * @param whence
     * 
     * @return
     */
    @HLEFunction(nid = 0x68963324, version = 150, checkInsideInterrupt = true)
    public int sceIoLseek32(int id, int offset, int whence) {
        if (log.isDebugEnabled()) {
            log.debug("sceIoLseek32 - id " + Integer.toHexString(id) + " offset " + offset + " (hex=0x" + Integer.toHexString(offset) + ") whence " + getWhenceName(whence));
        }

        int result = (int) hleIoLseek(id, (long) offset, whence, false, false);
        delayIoOperation(IoOperation.seek);
        return result;
    }

    /**
     * sceIoLseek32Async
     * 
     * @param id
     * @param offset
     * @param whence
     */
    @HLEFunction(nid = 0x1B385D8F, version = 150, checkInsideInterrupt = true)
    public int sceIoLseek32Async(int id, int offset, int whence) {
        if (log.isDebugEnabled()) {
            log.debug("sceIoLseek32Async - id " + Integer.toHexString(id) + " offset " + offset + " (hex=0x" + Integer.toHexString(offset) + ") whence " + getWhenceName(whence));
        }

        return (int) hleIoLseek(id, (long) offset, whence, false, true);
    }

    /**
     * sceIoIoctl
     * 
     * @param id
     * @param cmd
     * @param indata_addr
     * @param inlen
     * @param outdata_addr
     * @param outlen
     * 
     * @return
     */
    @HLEFunction(nid = 0x63632449, version = 150, checkInsideInterrupt = true)
    public int sceIoIoctl(int id, int cmd, int indata_addr, int inlen, int outdata_addr, int outlen) {
        int result = hleIoIoctl(id, cmd, indata_addr, inlen, outdata_addr, outlen, false);
        delayIoOperation(IoOperation.ioctl);
        return result;
    }

    /**
     * sceIoIoctlAsync
     * 
     * @param id
     * @param cmd
     * @param indata_addr
     * @param inlen
     * @param outdata_addr
     * @param outlen
     * 
     * @return
     */
    @HLEFunction(nid = 0xE95A012B, version = 150, checkInsideInterrupt = true)
    public int sceIoIoctlAsync(int id, int cmd, int indata_addr, int inlen, int outdata_addr, int outlen) {
        return hleIoIoctl(id, cmd, indata_addr, inlen, outdata_addr, outlen, true);
    }

    /**
     * Opens a directory for listing.
     * 
     * @param dirname
     * 
     * @return
     */
    @HLEFunction(nid = 0xB29DDF9C, version = 150, checkInsideInterrupt = true)
    public int sceIoDopen(PspString dirname) {
        if (log.isDebugEnabled()) {
            log.debug("sceIoDopen dirname = " + dirname);
        }

        String pcfilename = getDeviceFilePath(dirname.getString());
        int result;
        if (pcfilename != null) {
            if (isUmdPath(pcfilename)) {
                // Files in our iso virtual file system
                String isofilename = trimUmdPrefix(pcfilename);
                if (log.isDebugEnabled()) {
                    log.debug("sceIoDopen - isofilename = " + isofilename);
                }
                if (iso == null) { // check umd is mounted
                    log.error("sceIoDopen - no umd mounted");
                    result = ERROR_ERRNO_DEVICE_NOT_FOUND;
                } else if (!Modules.sceUmdUserModule.isUmdActivated()) { // check umd is activated
                    log.warn("sceIoDopen - umd mounted but not activated");
                    result = ERROR_KERNEL_NO_SUCH_DEVICE;
                } else {
                    try {
                        if (iso.isDirectory(isofilename)) {
                            String[] filenames = iso.listDirectory(isofilename);
                            IoDirInfo info = new IoDirInfo(pcfilename, filenames);
                            result = info.id;
                        } else {
                            log.warn("sceIoDopen '" + isofilename + "' not a umd directory!");
                            result = -1;
                        }
                    } catch (FileNotFoundException e) {
                        log.warn("sceIoDopen - '" + isofilename + "' umd file not found");
                        result = -1;
                    } catch (IOException e) {
                        log.warn("sceIoDopen - umd io error: " + e.getMessage());
                        result = -1;
                    }
                }
            } else if (dirname.getString().startsWith("/") && dirname.getString().indexOf(":") != -1) {
                log.warn("sceIoDopen apps running outside of ms0 dir are not fully supported, relative child paths should still work");
                result = -1;
            } else {
                // Regular apps run from inside mstick dir or absolute path given
                if (log.isDebugEnabled()) {
                    log.debug("sceIoDopen - pcfilename = " + pcfilename);
                }
                File f = new File(pcfilename);
                if (f.isDirectory()) {
                    IoDirInfo info = new IoDirInfo(pcfilename, f.list());
                    result = info.id;
                } else {
                    log.warn("sceIoDopen '" + pcfilename + "' not a directory! (could be missing)");
                    result = ERROR_ERRNO_FILE_NOT_FOUND;
                }
            }
        } else {
        	result = -1;
        }
        for (IIoListener ioListener : ioListeners) {
            ioListener.sceIoDopen(result, dirname.getAddress(), dirname.getString());
        }
        delayIoOperation(IoOperation.open);
        return result;
    }

    /**
     * sceIoDread
     * 
     * @param id
     * @param dirent_addr
     * 
     * @return
     */
    @HLEFunction(nid = 0xE3EB004C, version = 150, checkInsideInterrupt = true)
    public int sceIoDread(int id, int dirent_addr) {
        IoDirInfo info = dirIds.get(id);
        
        int result;
        if (info == null) {
            log.warn("sceIoDread unknown id " + Integer.toHexString(id));
            result = ERROR_KERNEL_BAD_FILE_DESCRIPTOR;
        } else if (info.hasNext()) {
            String filename = info.next();

            SceIoStat stat = stat(info.path + "/" + filename);
            if (stat != null) {
                SceIoDirent dirent = new SceIoDirent(stat, filename);
                dirent.write(Memory.getInstance(), dirent_addr);

                if ((stat.attr & 0x10) == 0x10) {
                    log.debug("sceIoDread id=" + Integer.toHexString(id) + " #" + info.printableposition + " dir='" + info.path + "', dir='" + filename + "'");
                } else {
                    log.debug("sceIoDread id=" + Integer.toHexString(id) + " #" + info.printableposition + " dir='" + info.path + "', file='" + filename + "'");
                }

                result = 1;
            } else {
                log.warn("sceIoDread id=" + Integer.toHexString(id) + " stat failed (" + info.path + "/" + filename + ")");
                result = -1;
            }
        } else {
            log.debug("sceIoDread id=" + Integer.toHexString(id) + " no more files");
            result = 0;
        }
        for (IIoListener ioListener : ioListeners) {
            ioListener.sceIoDread(result, id, dirent_addr);
        }
        delayIoOperation(IoOperation.dread);
        return result;
    }

    /**
     * sceIoDclose
     * 
     * @param id
     * 
     * @return
     */
    @HLEFunction(nid = 0xEB092469, version = 150, checkInsideInterrupt = true)
    public int sceIoDclose(int id) {
        if (log.isDebugEnabled()) {
            log.debug("sceIoDclose - id = " + Integer.toHexString(id));
        }
        
        IoDirInfo info = dirIds.get(id);
        int result;
        if (info == null) {
            log.warn("sceIoDclose - unknown id " + Integer.toHexString(id));
            result = ERROR_KERNEL_BAD_FILE_DESCRIPTOR;
        } else {
        	info.close();
        	result = 0;
        }

        for (IIoListener ioListener : ioListeners) {
            ioListener.sceIoDclose(result, id);
        }
        delayIoOperation(IoOperation.close);
        return result;
    }

    /**
     * sceIoRemove
     * 
     * @param filename
     * 
     * @return
     */
    @HLEFunction(nid = 0xF27A9C51, version = 150, checkInsideInterrupt = true)
    public int sceIoRemove(PspString filename) {
        if (log.isDebugEnabled()) {
            log.debug("sceIoRemove - file = " + filename.getString());
        }
        
        String pcfilename = getDeviceFilePath(filename.getString());
        int result;

        if (pcfilename != null) {
            if (isUmdPath(pcfilename)) {
            	result = -1;
            } else {
                File file = new File(pcfilename);
                if (file.delete()) {
                	result = 0;
                } else {
                	result = -1;
                }
            }
        } else {
        	result = -1;
        }

        for (IIoListener ioListener : ioListeners) {
            ioListener.sceIoRemove(result, filename.getAddress(), filename.getString());
        }
        delayIoOperation(IoOperation.remove);
        return result;
    }

    /**
     * Creates a directory.
     * 
     * @param dirname      - Name of the directory
     * @param permissions  - 
     * 
     * @return
     */
    @HLEFunction(nid = 0x06A70004, version = 150, checkInsideInterrupt = true)
    public int sceIoMkdir(PspString dirname, int permissions) {
        if (log.isDebugEnabled()) {
            log.debug("sceIoMkdir dir = " + dirname.getString());
        }

        String pcfilename = getDeviceFilePath(dirname.getString());
        int result;
        
        if (pcfilename != null) {
            File f = new File(pcfilename);
            f.mkdir();
            result = 0;
        } else {
        	result = -1;
        }

        for (IIoListener ioListener : ioListeners) {
            ioListener.sceIoMkdir(result, dirname.getAddress(), dirname.getString(), permissions);
        }
        delayIoOperation(IoOperation.mkdir);

        return result;
    }

    /**
     * Removes a directory.
     * 
     * @param dirname
     * 
     * @return
     */
    @HLEFunction(nid = 0x1117C65F, version = 150, checkInsideInterrupt = true)
    public int sceIoRmdir(PspString dirname) {
        if (log.isDebugEnabled()) {
            log.debug("sceIoRmdir dir = " + dirname.getString());
        }

        String pcfilename = getDeviceFilePath(dirname.getString());
        int result;
        
        if (pcfilename != null) {
            File f = new File(pcfilename);
            f.delete();
            result = 0;
        } else {
        	result = -1;
        }

        for (IIoListener ioListener : ioListeners) {
            ioListener.sceIoRmdir(result, dirname.getAddress(), dirname.getString());
        }
        delayIoOperation(IoOperation.remove);

        return result;
    }

    /**
     * Changes the current directory.
     * 
     * @param path
     * 
     * @return
     */
    @HLEFunction(nid = 0x55F4717D, version = 150, checkInsideInterrupt = true)
    public int sceIoChdir(PspString path) {

    	if (log.isDebugEnabled()) {
            log.debug("sceIoChdir path = " + path.getString());
        }
    	
    	int result;

        if (path.getString().equals("..")) {
            int index = filepath.lastIndexOf("/");
            if (index != -1) {
                filepath = filepath.substring(0, index);
            }

            log.info("pspiofilemgr - filepath " + filepath + " (going up one level)");
            result = 0;
        } else {
            String pcfilename = getDeviceFilePath(path.getString());
            if (pcfilename != null) {
                filepath = pcfilename;
                log.info("pspiofilemgr - filepath " + filepath);
                result = 0;
            } else {
            	result = -1;
            }
        }
        for (IIoListener ioListener : ioListeners) {
            ioListener.sceIoChdir(result, path.getAddress(), path.getString());
        }
        return result;
    }

    /**
     * sceIoSync
     * 
     * @param devicename
     * @param flag
     * 
     * @return
     */
    @HLEFunction(nid = 0xAB96437F, version = 150, checkInsideInterrupt = true)
    public int sceIoSync(PspString devicename, int flag) {
    	if (log.isDebugEnabled()) {
            log.debug("IGNORING: sceIoSync(device='" + devicename.getString() + "', flag=0x" + Integer.toHexString(flag) + ")");
        }

        for (IIoListener ioListener : ioListeners) {
            ioListener.sceIoSync(0, devicename.getAddress(), devicename.getString(), flag);
        }

        return 0;
    }

    /**
     * sceIoGetstat
     * 
     * @param filename
     * @param stat_addr
     * 
     * @return
     */
    @HLEFunction(nid = 0xACE946E8, version = 150, checkInsideInterrupt = true)
    public int sceIoGetstat(PspString filename, int stat_addr) {
        if (log.isDebugEnabled()) {
            log.debug("sceIoGetstat - file = " + filename.getString() + " stat = " + Integer.toHexString(stat_addr));
        }

        int result;
        String pcfilename = getDeviceFilePath(filename.getString());
        SceIoStat stat = stat(pcfilename);
        if (stat != null) {
            stat.write(Memory.getInstance(), stat_addr);
            result = 0;
        } else {
        	result = ERROR_ERRNO_FILE_NOT_FOUND;
        }

        for (IIoListener ioListener : ioListeners) {
            ioListener.sceIoGetStat(result, filename.getAddress(), filename.getString(), stat_addr);
        }
        
        return result;
    }

    /**
     * sceIoChstat
     * 
     * @param filename
     * @param stat_addr
     * @param bits
     * 
     * @return
     */
    @HLEFunction(nid = 0xB8A740F4, version = 150, checkInsideInterrupt = true)
    public int sceIoChstat(PspString filename, int stat_addr, int bits) {
        if (log.isDebugEnabled()) {
            log.debug("sceIoChstat - file = " + filename.getString() + ", bits=0x" + Integer.toHexString(bits));
        }

        String pcfilename = getDeviceFilePath(filename.getString());
        int result;
        
        if (pcfilename != null) {
            if (isUmdPath(pcfilename)) {
            	result = -1;
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
                	result = 0;
                } else {
                	result = -1;
                }
            }
        } else {
        	result = -1;
        }
        for (IIoListener ioListener : ioListeners) {
            ioListener.sceIoChstat(result, filename.getAddress(), filename.getString(), stat_addr, bits);
        }
        return result;
    }

    /**
     * sceIoRename
     * 
     * @param oldfilename
     * @param newfilename
     * 
     * @return
     */
    @HLEFunction(nid = 0x779103A0, version = 150, checkInsideInterrupt = true)
    public int sceIoRename(PspString oldfilename, PspString newfilename) {
    	if (log.isDebugEnabled()) {
            log.debug("sceIoRename - file = " + oldfilename.getString() + ", new_file = " + newfilename.getString());
        }
        
        String oldpcfilename = getDeviceFilePath(oldfilename.getString());
        String newpcfilename = getDeviceFilePath(newfilename.getString());
        int result;
        
        if (oldpcfilename != null) {
            if (isUmdPath(oldpcfilename)) {
                result = -1;
            } else {
                File file = new File(oldpcfilename);
                File newfile = new File(newpcfilename);
                if (file.renameTo(newfile)) {
                	result = 0;
                } else {
                	result = -1;
                }
            }
        } else {
        	result = -1;
        }

        for (IIoListener ioListener : ioListeners) {
            ioListener.sceIoRename(result, oldfilename.getAddress(), oldfilename.getString(), newfilename.getAddress(), newfilename.getString());
        }
        delayIoOperation(IoOperation.rename);
        return result;
    }

    /**
     * sceIoDevctl
     * 
     * @param processor
     * @param devicename
     * @param cmd
     * @param indata_addr
     * @param inlen
     * @param outdata_addr
     * 
     * @param outlen
     */
    @HLEFunction(nid = 0x54F5FB11, version = 150, checkInsideInterrupt = true)
    public int sceIoDevctl(PspString devicename, int cmd, int indata_addr, int inlen, int outdata_addr, int outlen) {
        Memory mem = Processor.memory;
        int result = -1;

        if (log.isDebugEnabled()) {
            log.debug("sceIoDevctl(device='" + devicename.getString() + "',cmd=0x" + Integer.toHexString(cmd) + ",indata=0x" + Integer.toHexString(indata_addr) + ",inlen=" + inlen + ",outdata=0x" + Integer.toHexString(outdata_addr) + ",outlen=" + outlen + ")");

            if (Memory.isAddressGood(indata_addr)) {
                for (int i = 0; i < inlen; i += 4) {
                    log.debug("sceIoDevctl indata[" + (i / 4) + "]=0x" + Integer.toHexString(mem.read32(indata_addr + i)));
                }
            }
            if (Memory.isAddressGood(outdata_addr)) {
                for (int i = 0; i < outlen; i += 4) {
                    log.debug("sceIoDevctl outdata[" + (i / 4) + "]=0x" + Integer.toHexString(mem.read32(outdata_addr + i)));
                }
            }
        }
        
        if (devicename.getString().equals("emulator:") || devicename.getString().equals("kemulator:")) {
        	switch (cmd) {
        		// EMULATOR_DEVCTL__GET_HAS_DISPLAY
        		case 0x00000001: {
        			if (Memory.isAddressGood(outdata_addr) && outlen >= 4) {
                        mem.write32(outdata_addr, 1);
                        result = 0;
        			}
        		} break;
       			// EMULATOR_DEVCTL__SEND_OUTPUT
        		case 0x00000002: {
        			AutoTestsOutput.appendString(new String(mem.readChunk(indata_addr, inlen).array()));
        			result = 0;
        		} break;
       			// EMULATOR_DEVCTL__IS_EMULATOR
        		case 0x00000003: {
        			result = 0;
        		} break;
        		default:
        			throw(new RuntimeException(String.format("Unknown emulator: cmd 0x%08X", cmd)));
        	}
        	
            for (IIoListener ioListener : ioListeners) {
                ioListener.sceIoDevctl(result, devicename.getAddress(), devicename.getString(), cmd, indata_addr, inlen, outdata_addr, outlen);
            }
            delayIoOperation(IoOperation.ioctl);

        	return result;
        }

        
        switch (cmd) {
            // Get UMD disc type.
            case 0x01F20001: {
                if (log.isDebugEnabled()) {
                    log.debug("sceIoDevctl " + String.format("0x%08X", cmd) + " get disc type");
                }
                if (Memory.isAddressGood(outdata_addr) && outlen >= 8) {
                    // 0 = No disc.
                    // 0x10 = Game disc.
                    // 0x20 = Video disc.
                    // 0x40 = Audio disc.
                    // 0x80 = Cleaning disc.
                    int out;
                    if (iso == null) {
                    	out = 0;
                    } else {
                    	out = 0x10;  // Always return game disc (if present).
                    }
                    mem.write32(outdata_addr + 4, out);
                    result = 0;
                } else {
                	result = -1;
                }
                break;
            }
            // Get UMD current LBA.
            case 0x01F20002: {
                if (log.isDebugEnabled()) {
                    log.debug("sceIoDevctl " + String.format("0x%08X", cmd) + " get current LBA");
                }
                if (Memory.isAddressGood(outdata_addr) && outlen >= 4) {
                    mem.write32(outdata_addr, 0); // Assume first sector.
                    result = 0;
                } else {
                	result = -1;
                }
                break;
            }
            // Seek UMD disc (raw).
            case 0x01F100A3: {
                if (log.isDebugEnabled()) {
                    log.debug("sceIoDevctl " + String.format("0x%08X", cmd) + " seek UMD disc");
                }
                if ((Memory.isAddressGood(indata_addr) && inlen >= 4)) {
                    int sector = mem.read32(indata_addr);
                    if (log.isDebugEnabled()) {
                        log.debug("sector=" + sector);
                    }
                    result = 0;
                } else {
                	result = -1;
                }
                break;
            }
            // Prepare UMD data into cache.
            case 0x01F100A4: {
                if (log.isDebugEnabled()) {
                    log.debug("sceIoDevctl " + String.format("0x%08X", cmd) + " prepare UMD data to cache");
                }
                if ((Memory.isAddressGood(indata_addr) && inlen >= 4)) {
                    // UMD cache read struct (16-bytes).
                    int unk1 = mem.read32(indata_addr); // NULL.
                    int sector = mem.read32(indata_addr + 4);  // First sector of data to read.
                    int unk2 = mem.read32(indata_addr + 8); // NULL.
                    int sectorNum = mem.read32(indata_addr + 12);  // Length of data to read.
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("sector=%d, sectorNum=%d, unk1=%d, unk2=%d", sector, sectorNum, unk1, unk2));
                    }
                    result = 0;
                } else {
                	result = -1;
                }
                break;
            }
            // Prepare UMD data into cache and get status.
            case 0x01F300A5: {
                if (log.isDebugEnabled()) {
                    log.debug("sceIoDevctl " + String.format("0x%08X", cmd) + " prepare UMD data to cache and get status");
                }
                if ((Memory.isAddressGood(indata_addr) && inlen >= 4) && (Memory.isAddressGood(outdata_addr) && outlen >= 4)) {
                    // UMD cache read struct (16-bytes).
                    int unk1 = mem.read32(indata_addr); // NULL.
                    int sector = mem.read32(indata_addr + 4);  // First sector of data to read.
                    int unk2 = mem.read32(indata_addr + 8); // NULL.
                    int sectorNum = mem.read32(indata_addr + 12);  // Length of data to read.
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("sector=%d, sectorNum=%d, unk1=%d, unk2=%d", sector, sectorNum, unk1, unk2));
                    }
                    mem.write32(outdata_addr, 1); // Status (unitary index of the requested read, greater or equal to 1).
                    result = 0;
                } else {
                	result = -1;
                }
                break;
            }
            // Check the MemoryStick's driver status (mscmhc0).
            case 0x02025801: {
                log.debug("sceIoDevctl " + String.format("0x%08X", cmd) + " check ms driver status");
                if (!devicename.getString().equals("mscmhc0:")) {
                	result = ERROR_KERNEL_UNSUPPORTED_OPERATION;
                } else if (Memory.isAddressGood(outdata_addr)) {
                    // 0 = Driver busy.
                    // 1 = Driver ready.
                    mem.write32(outdata_addr, 4);
                    result = 0;
                } else {
                	result = -1;
                }
                break;
            }
            // Register MemoryStick's insert/eject callback (mscmhc0).
            case 0x02015804: {
                log.debug("sceIoDevctl register memorystick insert/eject callback (mscmhc0)");
                ThreadManForUser threadMan = Modules.ThreadManForUserModule;
                if (!devicename.getString().equals("mscmhc0:")) {
                	result = ERROR_KERNEL_UNSUPPORTED_OPERATION;
                } else if (Memory.isAddressGood(indata_addr) && inlen == 4) {
                    int cbid = mem.read32(indata_addr);
                    final int callbackType = SceKernelThreadInfo.THREAD_CALLBACK_MEMORYSTICK;
                    if (threadMan.hleKernelRegisterCallback(callbackType, cbid)) {
                        // Trigger the registered callback immediately.
                        threadMan.hleKernelNotifyCallback(callbackType, cbid, MemoryStick.getStateMs());
                        result = 0; // Success.
                    } else {
                    	result = SceKernelErrors.ERROR_MEMSTICK_DEVCTL_TOO_MANY_CALLBACKS;
                    }
                } else {
                	result = ERROR_MEMSTICK_DEVCTL_BAD_PARAMS;
                }
                break;
            }
            // Unregister MemoryStick's insert/eject callback (mscmhc0).
            case 0x02015805: {
                log.debug("sceIoDevctl unregister memorystick insert/eject callback (mscmhc0)");
                ThreadManForUser threadMan = Modules.ThreadManForUserModule;
                if (!devicename.getString().equals("mscmhc0:")) {
                	result = ERROR_KERNEL_UNSUPPORTED_OPERATION;
                } else if (Memory.isAddressGood(indata_addr) && inlen == 4) {
                    int cbid = mem.read32(indata_addr);
                    if (threadMan.hleKernelUnRegisterCallback(SceKernelThreadInfo.THREAD_CALLBACK_MEMORYSTICK, cbid) != null) {
                    	result = 0; // Success.
                    } else {
                    	result = ERROR_MEMSTICK_DEVCTL_BAD_PARAMS; // No such callback.
                    }
                } else {
                	result = -1; // Invalid parameters.
                }
                break;
            }
            // Check if the device is inserted (mscmhc0).
            case 0x02025806: {
                log.debug("sceIoDevctl check ms inserted (mscmhc0)");
                if (!devicename.getString().equals("mscmhc0:")) {
                	result = ERROR_KERNEL_UNSUPPORTED_OPERATION;
                } else if (Memory.isAddressGood(outdata_addr)) {
                    // 0 = Not inserted.
                    // 1 = Inserted.
                    mem.write32(outdata_addr, 1);
                    result = 0;
                } else {
                	result = -1;
                }
                break;
            }
            // Register memorystick insert/eject callback (fatms0).
            case 0x02415821: {
                log.debug("sceIoDevctl register memorystick insert/eject callback (fatms0)");
                ThreadManForUser threadMan = Modules.ThreadManForUserModule;
                if (!devicename.getString().equals("fatms0:")) {
                	result = ERROR_MEMSTICK_DEVCTL_BAD_PARAMS;
                } else if (Memory.isAddressGood(indata_addr) && inlen == 4) {
                    int cbid = mem.read32(indata_addr);
                    final int callbackType = SceKernelThreadInfo.THREAD_CALLBACK_MEMORYSTICK_FAT;
                    if (threadMan.hleKernelRegisterCallback(callbackType, cbid)) {
                        // Trigger the registered callback immediately.
                    	// Only trigger this one callback, not all the MS callbacks.
                        threadMan.hleKernelNotifyCallback(callbackType, cbid, MemoryStick.getStateFatMs());
                        result = 0;  // Success.
                    } else {
                    	result = SceKernelErrors.ERROR_ERRNO_INVALID_ARGUMENT;
                    }
                } else {
                	result = SceKernelErrors.ERROR_ERRNO_INVALID_ARGUMENT;
                }
                break;
            }
            // Unregister memorystick insert/eject callback (fatms0).
            case 0x02415822: {
                log.debug("sceIoDevctl unregister memorystick insert/eject callback (fatms0)");
                ThreadManForUser threadMan = Modules.ThreadManForUserModule;
                if (!devicename.getString().equals("fatms0:")) {
                	result = ERROR_MEMSTICK_DEVCTL_BAD_PARAMS;
                } else if (Memory.isAddressGood(indata_addr) && inlen == 4) {
                    int cbid = mem.read32(indata_addr);
                    threadMan.hleKernelUnRegisterCallback(SceKernelThreadInfo.THREAD_CALLBACK_MEMORYSTICK_FAT, cbid);
                    result = 0;  // Success.
                } else {
                	result = SceKernelErrors.ERROR_ERRNO_INVALID_ARGUMENT;
                }
                break;
            }
            // Set if the device is assigned/inserted or not (fatms0).
            case 0x02415823: {
                log.debug("sceIoDevctl set assigned device (fatms0)");
                if (!devicename.getString().equals("fatms0:")) {
                	result = ERROR_MEMSTICK_DEVCTL_BAD_PARAMS;
                } else if (Memory.isAddressGood(indata_addr) && inlen >= 4) {
                    // 0 - Device is not assigned (callback not registered).
                    // 1 - Device is assigned (callback registered).
                    MemoryStick.setStateFatMs(mem.read32(indata_addr));
                    result = 0;
                } else {
                	result = -1;
                }
                break;
            }
            // Check if the device is write protected (fatms0).
            case 0x02425824: {
                log.debug("sceIoDevctl check write protection (fatms0)");
                if (!devicename.getString().equals("fatms0:") && !devicename.getString().equals("ms0:")) { // For this command the alias "ms0:" is also supported.
                	result = ERROR_MEMSTICK_DEVCTL_BAD_PARAMS;
                } else if (Memory.isAddressGood(outdata_addr)) {
                    // 0 - Device is not protected.
                    // 1 - Device is protected.
                    mem.write32(outdata_addr, 0);
                    result = 0;
                } else {
                	result = -1;
                }
                break;
            }
            // Get MS capacity (fatms0).
            case 0x02425818: {
                log.debug("sceIoDevctl get MS capacity (fatms0)");
                int sectorSize = 0x200;
                int sectorCount = MemoryStick.getSectorSize() / sectorSize;
                int maxClusters = (int) ((MemoryStick.getFreeSize() * 95L / 100) / (sectorSize * sectorCount));
                int freeClusters = maxClusters;
                int maxSectors = maxClusters;
                if (Memory.isAddressGood(indata_addr) && inlen >= 4) {
                    int addr = mem.read32(indata_addr);
                    if (Memory.isAddressGood(addr)) {
                        log.debug("sceIoDevctl refer ms free space");
                        mem.write32(addr, maxClusters);
                        mem.write32(addr + 4, freeClusters);
                        mem.write32(addr + 8, maxSectors);
                        mem.write32(addr + 12, sectorSize);
                        mem.write32(addr + 16, sectorCount);
                        result = 0;
                    } else {
                        log.warn("sceIoDevctl 0x02425818 bad save address " + String.format("0x%08X", addr));
                        result = -1;
                    }
                } else {
                    log.warn("sceIoDevctl 0x02425818 bad param address " + String.format("0x%08X", indata_addr) + " or size " + inlen);
                    result = -1;
                }
                break;
            }
            // Check if the device is assigned/inserted (fatms0).
            case 0x02425823: {
                log.debug("sceIoDevctl check assigned device (fatms0)");
                if (!devicename.getString().equals("fatms0:")) {
                	result = ERROR_MEMSTICK_DEVCTL_BAD_PARAMS;
                } else if (Memory.isAddressGood(outdata_addr) && outlen >= 4) {
                    // 0 - Device is not assigned (callback not registered).
                    // 1 - Device is assigned (callback registered).
                    mem.write32(outdata_addr, MemoryStick.getStateFatMs());
                    result = 0;
                } else {
                	result = -1;
                }
                break;
            }
            // Register USB thread.
            case 0x03415001: {
                log.debug("sceIoDevctl register usb thread");
                if (Memory.isAddressGood(indata_addr) && inlen >= 4) {
                    // Unknown params.
                	result = 0;
                } else {
                	result = -1;
                }
                break;
            }
            // Unregister USB thread.
            case 0x03415002: {
                log.debug("sceIoDevctl unregister usb thread");
                if (Memory.isAddressGood(indata_addr) && inlen >= 4) {
                    // Unknown params.
                	result = 0;
                } else {
                	result = -1;
                }
                break;
            }
            default:
                log.warn("sceIoDevctl " + String.format("0x%08X", cmd) + " unknown command");
                if (Memory.isAddressGood(indata_addr)) {
                    for (int i = 0; i < inlen; i += 4) {
                        log.warn("sceIoDevctl indata[" + (i / 4) + "]=0x" + Integer.toHexString(mem.read32(indata_addr + i)));
                    }
                }
                result = -1;
                break;
        }
        for (IIoListener ioListener : ioListeners) {
            ioListener.sceIoDevctl(result, devicename.getAddress(), devicename.getString(), cmd, indata_addr, inlen, outdata_addr, outlen);
        }
        delayIoOperation(IoOperation.ioctl);
        return result;
    }

    /**
     * sceIoGetDevType
     * 
     * @param id
     * 
     * @return
     */
    @HLEFunction(nid = 0x08BD7374, version = 150, checkInsideInterrupt = true)
    public int sceIoGetDevType(int id) {
        if (log.isDebugEnabled()) {
            log.debug("sceIoGetDevType - id " + Integer.toHexString(id));
        }
        
        IoInfo info = fileIds.get(id);
        int result;
        if (info == null) {
            log.warn("sceIoGetDevType - unknown id " + Integer.toHexString(id));
            result = ERROR_KERNEL_BAD_FILE_DESCRIPTOR;
        } else {
            // For now, return alias type, since it's the most used.
            result = PSP_DEV_TYPE_ALIAS;
        }
        
        return result;
    }

    /**
     * sceIoAssign
     * 
     * @param alias_addr
     * @param physical_addr
     * @param filesystem_addr
     * @param mode
     * @param arg_addr
     * @param argSize
     * 
     * @return
     */
    @HLEFunction(nid = 0xB2A628C1, version = 150, checkInsideInterrupt = true)
    public int sceIoAssign(int alias_addr, int physical_addr, int filesystem_addr, int mode, int arg_addr, int argSize) {
        String alias = readStringZ(alias_addr);
        String physical_dev = readStringZ(physical_addr);
        String filesystem_dev = readStringZ(filesystem_addr);
        String perm;

        // IoAssignPerms
        switch (mode) {
            case 0:
                perm = "IOASSIGN_RDWR";
                break;
            case 1:
                perm = "IOASSIGN_RDONLY";
                break;
            default:
                perm = "unhandled " + mode;
                break;
        }

        // Mounts physical_dev on filesystem_dev and sets an alias to represent it.
        log.warn("IGNORING: sceIoAssign(alias='" + alias + "', physical_dev='" + physical_dev + "', filesystem_dev='" + filesystem_dev + "', mode=" + perm + ", arg_addr=0x" + Integer.toHexString(arg_addr) + ", argSize=" + argSize + ")");

        
        int result = 0;

        for (IIoListener ioListener : ioListeners) {
            ioListener.sceIoAssign(result, alias_addr, alias, physical_addr, physical_dev,
                    filesystem_addr, filesystem_dev, mode, arg_addr, argSize);
        }
        
        return result;
    }

    /**
     * sceIoUnassign
     * 
     * @param alias
     * 
     * @return
     */
    @HLEFunction(nid = 0x6D08A871, version = 150, checkInsideInterrupt = true)
    public int sceIoUnassign(PspString alias) {
        log.warn("IGNORING: sceIoUnassign (alias='" + alias.getString() + ")");

        return 0;
    }

    /**
     * sceIoCancel
     * 
     * @param id
     * 
     * @return
     */
    @HLEFunction(nid = 0xE8BC6571, version = 150, checkInsideInterrupt = true)
    public int sceIoCancel(int id) {
        if (log.isDebugEnabled()) {
            log.debug("sceIoCancel - id " + Integer.toHexString(id));
        }
        
        IoInfo info = fileIds.get(id);
        int result;
        
        if (info == null) {
            log.warn("sceIoCancel - unknown id " + Integer.toHexString(id));
            result = ERROR_KERNEL_BAD_FILE_DESCRIPTOR;
        } else {
            info.closePending = true;
            result = 0;
        }

        for (IIoListener ioListener : ioListeners) {
            ioListener.sceIoCancel(result, id);
        }
        
        return result;
    }

    /**
     * sceIoGetFdList
     * 
     * @param out_addr
     * @param outSize
     * @param fdNum_addr
     * 
     * @return
     */
    @HLEFunction(nid = 0x5C2BE2CC, version = 150, checkInsideInterrupt = true)
    public int sceIoGetFdList(int out_addr, int outSize, int fdNum_addr) {
        Memory mem = Processor.memory;

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceIoGetFdList out_addr=0x%08X, outSize=%d, fdNum_addr=0x%08X", out_addr, outSize, fdNum_addr));
        }

        int count = 0;
        if (Memory.isAddressGood(out_addr) && outSize > 0) {
	        IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(out_addr, 4 * outSize, 4);
	        for (Integer fd : fileIds.keySet()) {
	        	if (count >= outSize) {
	        		break;
	        	}
	        	memoryWriter.writeNext(fd.intValue());
	        }
	        memoryWriter.flush();
        }

        if (fdNum_addr != 0) {
            // Return the total number of files open
        	mem.write32(fdNum_addr, fileIds.size());
        }

        return count;
    }

}