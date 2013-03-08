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
package jpcsp.HLE.VFS.local;

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_MEMSTICK_DEVCTL_BAD_PARAMS;
import static jpcsp.HLE.modules150.IoFileMgrForUser.PSP_O_CREAT;
import static jpcsp.HLE.modules150.IoFileMgrForUser.PSP_O_EXCL;
import static jpcsp.HLE.modules150.IoFileMgrForUser.PSP_O_RDWR;
import static jpcsp.HLE.modules150.IoFileMgrForUser.PSP_O_TRUNC;
import static jpcsp.HLE.modules150.IoFileMgrForUser.PSP_O_WRONLY;

import java.io.File;
import java.io.FileNotFoundException;

import jpcsp.Memory;
import jpcsp.HLE.Modules;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.VFS.AbstractVirtualFileSystem;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.kernel.types.SceIoStat;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.ScePspDateTime;
import jpcsp.HLE.modules.ThreadManForUser;
import jpcsp.filesystems.SeekableRandomFile;
import jpcsp.hardware.MemoryStick;

public class LocalVirtualFileSystem extends AbstractVirtualFileSystem {
	protected final String localPath;
    // modeStrings indexed by [0, PSP_O_RDONLY, PSP_O_WRONLY, PSP_O_RDWR]
    // SeekableRandomFile doesn't support write only: take "rw",
    private final static String[] modeStrings = {"r", "r", "rw", "rw"};

	public LocalVirtualFileSystem(String localPath) {
		this.localPath = localPath;
	}

	protected File getFile(String fileName) {
		return new File(localPath + fileName);
	}

	protected static String getMode(int mode) {
		return modeStrings[mode & PSP_O_RDWR];
	}

	@Override
	public IVirtualFile ioOpen(String fileName, int flags, int mode) {
		File file = getFile(fileName);
        if (file.exists() && hasFlag(flags, PSP_O_CREAT) && hasFlag(flags, PSP_O_EXCL)) {
            if (log.isDebugEnabled()) {
                log.debug("hleIoOpen - file already exists (PSP_O_CREAT + PSP_O_EXCL)");
            }
            throw new SceKernelErrorException(SceKernelErrors.ERROR_ERRNO_FILE_ALREADY_EXISTS);
        }

        // When PSP_O_CREAT is specified, create the parent directories
    	// if they do not yet exist.
        if (!file.exists() && hasFlag(flags, PSP_O_CREAT)) {
        	String parentDir = file.getParent();
        	new File(parentDir).mkdirs();
        }

        SeekableRandomFile raf;
		try {
			raf = new SeekableRandomFile(file, getMode(flags));
		} catch (FileNotFoundException e) {
			return null;
		}

		LocalVirtualFile localVirtualFile = new LocalVirtualFile(raf);

		if (hasFlag(flags, PSP_O_WRONLY) && hasFlag(flags, PSP_O_TRUNC)) {
            // When writing, PSP_O_TRUNC truncates the file at the position of the first write.
        	// E.g.:
        	//    open(PSP_O_TRUNC)
        	//    seek(0x1000)
        	//    write()  -> truncates the file at the position 0x1000 before writing
			localVirtualFile.setTruncateAtNextWrite(true);
        }

		return localVirtualFile;
	}

	@Override
	public int ioGetstat(String fileName, SceIoStat stat) {
        File file = getFile(fileName);
        if (!file.exists()) {
        	return SceKernelErrors.ERROR_ERRNO_FILE_NOT_FOUND;
        }

        // Set attr (dir/file) and copy into mode
        int attr = 0;
        if (file.isDirectory()) {
            attr |= 0x10;
        }
        if (file.isFile()) {
            attr |= 0x20;
        }

        int mode = (file.canRead() ? 4 : 0) + (file.canWrite() ? 2 : 0) + (file.canExecute() ? 1 : 0);
        // Octal extend into user and group
        mode = mode + (mode << 3) + (mode << 6);
        mode |= attr << 8;

        // Java can't see file create/access time
        ScePspDateTime ctime = ScePspDateTime.fromUnixTime(file.lastModified());
        ScePspDateTime atime = ScePspDateTime.fromUnixTime(0);
        ScePspDateTime mtime = ScePspDateTime.fromUnixTime(file.lastModified());

        stat.init(mode, attr, file.length(), ctime, atime, mtime);

        return 0;
	}

	@Override
	public int ioRemove(String name) {
		File file = getFile(name);

		if (!file.delete()) {
			return IO_ERROR;
		}

		return 0;
	}

	@Override
	public String[] ioDopen(String dirName) {
		File file = getFile(dirName);

		if (!file.isDirectory()) {
			if (file.exists()) {
				log.warn(String.format("ioDopen file '%s' is not a directory", dirName));
			} else {
				log.warn(String.format("ioDopen directory '%s' not found", dirName));
			}
			return null;
		}

		return file.list();
	}

	@Override
	public int ioMkdir(String name, int mode) {
		File file = getFile(name);

		if (file.exists()) {
			return SceKernelErrors.ERROR_ERRNO_FILE_ALREADY_EXISTS;
		}
		if (!file.mkdir()) {
			return IO_ERROR;
		}

		return 0;
	}

	@Override
	public int ioRmdir(String name) {
		File file = getFile(name);

		if (!file.exists()) {
			return SceKernelErrors.ERROR_ERRNO_FILE_NOT_FOUND;
		}
		if (!file.delete()) {
			return IO_ERROR;
		}

		return 0;
	}

	@Override
	public int ioChstat(String fileName, SceIoStat stat, int bits) {
        File file = getFile(fileName);

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

        return successful ? 0 : IO_ERROR;
	}

	@Override
	public int ioRename(String oldFileName, String newFileName) {
		File oldFile = getFile(oldFileName);
		File newFile = getFile(newFileName);

		if (log.isDebugEnabled()) {
        	log.debug(String.format("ioRename: renaming file '%s' to '%s'", oldFileName, newFileName));
        }

		if (!oldFile.renameTo(newFile)) {
        	log.warn(String.format("ioRename failed: '%s' to '%s'", oldFileName, newFileName));
        	return IO_ERROR;
        }

        return 0;
	}

	@Override
	public int ioDevctl(String deviceName, int command, TPointer inputPointer, int inputLength, TPointer outputPointer, int outputLength) {
		int result;

		switch (command) {
	        // Register memorystick insert/eject callback (fatms0).
	        case 0x02415821: {
	            log.debug("sceIoDevctl register memorystick insert/eject callback (fatms0)");
	            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
	            if (!deviceName.equals("fatms0:")) {
	            	result = ERROR_MEMSTICK_DEVCTL_BAD_PARAMS;
	            } else if (inputPointer.isAddressGood() && inputLength == 4) {
	                int cbid = inputPointer.getValue32();
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
	            if (!deviceName.equals("fatms0:")) {
	            	result = ERROR_MEMSTICK_DEVCTL_BAD_PARAMS;
	            } else if (inputPointer.isAddressGood() && inputLength == 4) {
	                int cbid = inputPointer.getValue32();
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
	            if (!deviceName.equals("fatms0:")) {
	            	result = ERROR_MEMSTICK_DEVCTL_BAD_PARAMS;
	            } else if (inputPointer.isAddressGood() && inputLength >= 4) {
	                // 0 - Device is not assigned (callback not registered).
	                // 1 - Device is assigned (callback registered).
	                MemoryStick.setStateFatMs(inputPointer.getValue32());
	                result = 0;
	            } else {
	            	result = IO_ERROR;
	            }
	            break;
	        }
	        // Check if the device is write protected (fatms0).
	        case 0x02425824: {
	            log.debug("sceIoDevctl check write protection (fatms0)");
	            if (!deviceName.equals("fatms0:") && !deviceName.equals("ms0:")) { // For this command the alias "ms0:" is also supported.
	            	result = ERROR_MEMSTICK_DEVCTL_BAD_PARAMS;
	            } else if (outputPointer.isAddressGood()) {
	                // 0 - Device is not protected.
	                // 1 - Device is protected.
	                outputPointer.setValue32(0);
	                result = 0;
	            } else {
	            	result = IO_ERROR;
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
	            if (inputPointer.isAddressGood() && inputLength >= 4) {
	                int addr = inputPointer.getValue32();
	                if (Memory.isAddressGood(addr)) {
	                    log.debug("sceIoDevctl refer ms free space");
	                    Memory mem = Memory.getInstance();
	                    mem.write32(addr, maxClusters);
	                    mem.write32(addr + 4, freeClusters);
	                    mem.write32(addr + 8, maxSectors);
	                    mem.write32(addr + 12, sectorSize);
	                    mem.write32(addr + 16, sectorCount);
	                    result = 0;
	                } else {
	                    log.warn("sceIoDevctl 0x02425818 bad save address " + String.format("0x%08X", addr));
	                    result = IO_ERROR;
	                }
	            } else {
	                log.warn("sceIoDevctl 0x02425818 bad param address " + String.format("0x%08X", inputPointer) + " or size " + inputLength);
	                result = IO_ERROR;
	            }
	            break;
	        }
	        // Check if the device is assigned/inserted (fatms0).
	        case 0x02425823: {
	            log.debug("sceIoDevctl check assigned device (fatms0)");
	            if (!deviceName.equals("fatms0:")) {
	            	result = ERROR_MEMSTICK_DEVCTL_BAD_PARAMS;
	            } else if (outputPointer.isAddressGood() && outputLength >= 4) {
	                // 0 - Device is not assigned (callback not registered).
	                // 1 - Device is assigned (callback registered).
	                outputPointer.setValue32(MemoryStick.getStateFatMs());
	                result = 0;
	            } else {
	            	result = IO_ERROR;
	            }
	            break;
	        }
	        default: {
	        	result = super.ioDevctl(deviceName, command, inputPointer, inputLength, outputPointer, outputLength);
	        }
		}

		return result;
	}
}
