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
package jpcsp.HLE.VFS.iso;

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_ERRNO_INVALID_ARGUMENT;
import static jpcsp.HLE.modules150.IoFileMgrForUser.PSP_O_CREAT;
import static jpcsp.HLE.modules150.IoFileMgrForUser.PSP_O_TRUNC;
import static jpcsp.HLE.modules150.IoFileMgrForUser.PSP_O_WRONLY;

import java.io.FileNotFoundException;
import java.io.IOException;

import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.VFS.AbstractVirtualFileSystem;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.kernel.types.SceIoStat;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.ScePspDateTime;
import jpcsp.filesystems.umdiso.UmdIsoFile;
import jpcsp.filesystems.umdiso.UmdIsoReader;

public class UmdIsoVirtualFileSystem extends AbstractVirtualFileSystem {
	protected final UmdIsoReader iso;

	public UmdIsoVirtualFileSystem(UmdIsoReader iso) {
		this.iso = iso;
	}

	@Override
	public IVirtualFile ioOpen(String fileName, int flags, int mode) {
		if (hasFlag(flags, PSP_O_WRONLY) || hasFlag(flags, PSP_O_CREAT) || hasFlag(flags, PSP_O_TRUNC)) {
			throw new SceKernelErrorException(SceKernelErrors.ERROR_ERRNO_READ_ONLY);
		}

		UmdIsoFile file;
		try {
			file = iso.getFile(fileName);
		} catch (FileNotFoundException e) {
			return null;
		} catch (IOException e) {
			log.error("ioOpen", e);
			return null;
		}

        // Opening "umd0:" is allowing to read the whole UMD per sectors.
		boolean sectorBlockMode = (fileName.length() == 0);

		return new UmdIsoVirtualFile(file, sectorBlockMode, iso);
	}

	@Override
	public int ioGetstat(String fileName, SceIoStat stat) {
        int mode = 4; // 4 = readable
        int attr = 0;
        long size = 0;
        long timestamp = 0;
        int startSector = 0;
        try {
            // Check for files first.
            UmdIsoFile file = iso.getFile(fileName);
            attr |= 0x20; // Is file
            size = file.length();
            timestamp = file.getTimestamp().getTime();
            startSector = file.getStartSector();
        } catch (FileNotFoundException fnfe) {
            // If file wasn't found, try looking for a directory.
            try {
                if (iso.isDirectory(fileName)) {
                    attr |= 0x10; // Is directory
                    mode |= 1; // 1 = executable
                }
            } catch (FileNotFoundException dnfe) {
                log.warn(String.format("ioGetstat - '%s' umd file/dir not found", fileName));
                return SceKernelErrors.ERROR_ERRNO_FILE_NOT_FOUND;
            } catch (IOException e) {
                log.warn("ioGetstat", e);
                return SceKernelErrors.ERROR_ERRNO_FILE_NOT_FOUND;
            }
        } catch (IOException e) {
            log.warn("ioGetstat", e);
            return SceKernelErrors.ERROR_ERRNO_FILE_NOT_FOUND;
        }

        // Octal extend into user and group
        mode = mode + (mode << 3) + (mode << 6);
        mode |= attr << 8;

        ScePspDateTime ctime = ScePspDateTime.fromUnixTime(timestamp);
        ScePspDateTime atime = ScePspDateTime.fromUnixTime(0);
        ScePspDateTime mtime = ScePspDateTime.fromUnixTime(timestamp);

        stat.init(mode, attr, size, ctime, atime, mtime);

        if (startSector > 0) {
            stat.setReserved(0, startSector);
        }

        return 0;
	}

	@Override
	public String[] ioDopen(String dirName) {
		String[] fileNames = null;

		try {
            if (iso.isDirectory(dirName)) {
                fileNames = iso.listDirectory(dirName);
            } else {
                log.warn(String.format("ioDopen file '%s' is not a directory", dirName));
            }
        } catch (FileNotFoundException e) {
            log.warn(String.format("ioDopen directory '%s' not found", dirName));
        } catch (IOException e) {
            log.warn("ioDopen", e);
        }

		return fileNames;
	}

	@Override
	public int ioDevctl(String deviceName, int command, TPointer inputPointer, int inputLength, TPointer outputPointer, int outputLength) {
		int result;

		switch (command) {
	        // Get UMD disc type.
	        case 0x01F20001: {
                log.debug("ioDevctl get disc type");
	            if (outputPointer.isAddressGood() && outputLength >= 8) {
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
	                outputPointer.setValue32(4, out);
	                result = 0;
	            } else {
	            	result = ERROR_ERRNO_INVALID_ARGUMENT;
	            }
	            break;
	        }
	        // Get UMD current LBA.
	        case 0x01F20002: {
                log.debug("ioDevctl get current LBA");
	            if (outputPointer.isAddressGood() && outputLength >= 4) {
	                outputPointer.setValue32(0); // Assume first sector.
	                result = 0;
	            } else {
	            	result = ERROR_ERRNO_INVALID_ARGUMENT;
	            }
	            break;
	        }
	        // Seek UMD disc (raw).
	        case 0x01F100A3: {
                log.debug("ioDevctl seek UMD disc");
	            if (inputPointer.isAddressGood() && inputLength >= 4) {
	                int sector = inputPointer.getValue32();
	                if (log.isDebugEnabled()) {
	                    log.debug(String.format("ioDevctl seek UMD disc: sector=%d", sector));
	                }
	                result = 0;
	            } else {
	            	result = ERROR_ERRNO_INVALID_ARGUMENT;
	            }
	            break;
	        }
	        // Prepare UMD data into cache.
	        case 0x01F100A4: {
                log.debug("ioDevctl prepare UMD data to cache");
	            if (inputPointer.isAddressGood() && inputLength >= 16) {
	                // UMD cache read struct (16-bytes).
	                int unk1 = inputPointer.getValue32(0);       // NULL.
	                int sector = inputPointer.getValue32(4);     // First sector of data to read.
	                int unk2 = inputPointer.getValue32(8);       // NULL.
	                int sectorNum = inputPointer.getValue32(12); // Length of data to read.
	                if (log.isDebugEnabled()) {
	                    log.debug(String.format("ioDevctl prepare UMD data to cache: sector=%d, sectorNum=%d, unk1=%d, unk2=%d", sector, sectorNum, unk1, unk2));
	                }
	                result = 0;
	            } else {
	            	result = ERROR_ERRNO_INVALID_ARGUMENT;
	            }
	            break;
	        }
	        // Prepare UMD data into cache and get status.
	        case 0x01F300A5: {
                log.debug("ioDevctl prepare UMD data to cache and get status");
	            if (inputPointer.isAddressGood() && inputLength >= 16 && outputPointer.isAddressGood() && outputLength >= 4) {
	                // UMD cache read struct (16-bytes).
	                int unk1 = inputPointer.getValue32(0);       // NULL.
	                int sector = inputPointer.getValue32(4);     // First sector of data to read.
	                int unk2 = inputPointer.getValue32(8);       // NULL.
	                int sectorNum = inputPointer.getValue32(12); // Length of data to read.
	                if (log.isDebugEnabled()) {
	                    log.debug(String.format("ioDevctl prepare UMD data to cache and get status: sector=%d, sectorNum=%d, unk1=%d, unk2=%d", sector, sectorNum, unk1, unk2));
	                }
	                outputPointer.setValue32(1); // Status (unitary index of the requested read, greater or equal to 1).
	                result = 0;
	            } else {
	            	result = ERROR_ERRNO_INVALID_ARGUMENT;
	            }
	            break;
	        }
			default:
				result = super.ioDevctl(deviceName, command, inputPointer, inputLength, outputPointer, outputLength);
		}

		return result;
	}
}
