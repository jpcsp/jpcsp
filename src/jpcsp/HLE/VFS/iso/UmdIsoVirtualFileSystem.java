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

import static jpcsp.HLE.modules150.IoFileMgrForUser.PSP_O_CREAT;
import static jpcsp.HLE.modules150.IoFileMgrForUser.PSP_O_TRUNC;
import static jpcsp.HLE.modules150.IoFileMgrForUser.PSP_O_WRONLY;

import java.io.FileNotFoundException;
import java.io.IOException;

import jpcsp.HLE.SceKernelErrorException;
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

		return new UmdIsoVirtualFile(file, sectorBlockMode);
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
}
