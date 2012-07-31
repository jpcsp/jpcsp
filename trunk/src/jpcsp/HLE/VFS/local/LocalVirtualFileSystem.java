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

import static jpcsp.HLE.modules150.IoFileMgrForUser.PSP_O_CREAT;
import static jpcsp.HLE.modules150.IoFileMgrForUser.PSP_O_EXCL;
import static jpcsp.HLE.modules150.IoFileMgrForUser.PSP_O_TRUNC;
import static jpcsp.HLE.modules150.IoFileMgrForUser.PSP_O_WRONLY;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.VFS.AbstractVirtualFileSystem;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.kernel.types.SceIoStat;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.ScePspDateTime;
import jpcsp.filesystems.SeekableRandomFile;

public class LocalVirtualFileSystem extends AbstractVirtualFileSystem {
	protected final String localPath;

	public LocalVirtualFileSystem(String localPath) {
		this.localPath = localPath;
	}

	protected File getFile(String fileName) {
		return new File(localPath + fileName);
	}

	@Override
	public IVirtualFile ioOpen(String fileName, int flags, int mode) {
		File file = getFile(fileName);
        if (file.exists() && hasFlag(mode, PSP_O_CREAT) && hasFlag(mode, PSP_O_EXCL)) {
            if (log.isDebugEnabled()) {
                log.debug("hleIoOpen - file already exists (PSP_O_CREAT + PSP_O_EXCL)");
            }
            throw new SceKernelErrorException(SceKernelErrors.ERROR_ERRNO_FILE_ALREADY_EXISTS);
        }

        // When PSP_O_CREAT is specified, create the parent directories
    	// if they do not yet exist.
        if (!file.exists() && hasFlag(mode, PSP_O_CREAT)) {
        	String parentDir = file.getParent();
        	new File(parentDir).mkdirs();
        }

        SeekableRandomFile raf;
		try {
			raf = new SeekableRandomFile(file, getMode(mode));
		} catch (FileNotFoundException e) {
			return null;
		}

		if (hasFlag(mode, PSP_O_WRONLY) && hasFlag(mode, PSP_O_TRUNC)) {
            // When writing, PSP_O_TRUNC resets the file to be written (truncate to 0 length).
        	try {
				raf.setLength(0);
			} catch (IOException e) {
				log.error("ioOpen.setLength", e);
			}
        }

        return new LocalVirtualFile(raf);
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

		return file.delete() ? 0 : IO_ERROR;
	}
}
