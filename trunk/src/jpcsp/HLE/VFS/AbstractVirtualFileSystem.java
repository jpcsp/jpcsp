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
package jpcsp.HLE.VFS;

import org.apache.log4j.Logger;

import jpcsp.HLE.TPointer;
import jpcsp.HLE.kernel.types.SceIoDirent;
import jpcsp.HLE.kernel.types.SceIoStat;
import jpcsp.HLE.modules.IoFileMgrForUser;

public abstract class AbstractVirtualFileSystem implements IVirtualFileSystem {
	protected static Logger log = Logger.getLogger("vfs");
	protected static final int IO_ERROR = -1;
    // modeStrings indexed by [0, PSP_O_RDONLY, PSP_O_WRONLY, PSP_O_RDWR]
    // SeekableRandomFile doesn't support write only: take "rw",
    private final static String[] modeStrings = {"r", "r", "rw", "rw"};

	protected static boolean hasFlag(int mode, int flag) {
		return (mode & flag) == flag;
	}

	protected static String getMode(int mode) {
		return modeStrings[mode & IoFileMgrForUser.PSP_O_RDWR];
	}

	@Override
	public int ioRead(IVirtualFile file, TPointer outputPointer, int outputLength) {
		return file.ioRead(outputPointer, outputLength);
	}

	@Override
	public int ioWrite(IVirtualFile file, TPointer inputPointer, int inputLength) {
		return file.ioWrite(inputPointer, inputLength);
	}

	@Override
	public long ioLseek(IVirtualFile file, long offset) {
		return file.ioLseek(offset);
	}

	@Override
	public int ioIoctl(IVirtualFile file, int command, TPointer inputPointer, int inputLength, TPointer outputPointer, int outputLength) {
		return file.ioIoctl(command, inputPointer, inputLength, outputPointer, outputLength);
	}

	@Override
	public void ioInit() {
	}

	@Override
	public void ioExit() {
	}

	@Override
	public IVirtualFile ioOpen(String fileName, int flags, int mode) {
		return null;
	}

	@Override
	public int ioClose(IVirtualFile file) {
		return IO_ERROR;
	}

	@Override
	public int ioRemove(String name) {
		return IO_ERROR;
	}

	@Override
	public int ioMkdir(String name, int mode) {
		return IO_ERROR;
	}

	@Override
	public int ioRmdir(String name) {
		return IO_ERROR;
	}

	@Override
	public IVirtualFile ioDopen(String name) {
		return null;
	}

	@Override
	public int ioDclose(IVirtualFile file) {
		return file.ioClose();
	}

	@Override
	public int ioDread(IVirtualFile file, SceIoDirent dir) {
		return IO_ERROR;
	}

	@Override
	public int ioGetstat(String fileName, SceIoStat stat) {
		return IO_ERROR;
	}

	@Override
	public int ioChstat(String fileName, SceIoStat stat, int bits) {
		return IO_ERROR;
	}

	@Override
	public int ioRename(String oldFileName, String newFileName) {
		return IO_ERROR;
	}

	@Override
	public int ioChdir(String directoryName) {
		return IO_ERROR;
	}

	@Override
	public int ioMount() {
		return IO_ERROR;
	}

	@Override
	public int ioUmount() {
		return IO_ERROR;
	}

	@Override
	public int ioDevctl(String deviceName, int command, TPointer inputPointer, int inputLength, TPointer outputPointer, int outputLength) {
		return IO_ERROR;
	}
}
