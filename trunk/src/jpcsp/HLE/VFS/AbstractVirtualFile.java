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

import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.filesystems.SeekableDataInput;
import jpcsp.util.Utilities;

public abstract class AbstractVirtualFile implements IVirtualFile {
	protected static Logger log = AbstractVirtualFileSystem.log;
	protected final SeekableDataInput file;
	protected static final int IO_ERROR = AbstractVirtualFileSystem.IO_ERROR;

	public AbstractVirtualFile(SeekableDataInput file) {
		this.file = file;
	}

	@Override
	public long getPosition() {
		try {
			return file.getFilePointer();
		} catch (IOException e) {
			log.error("getPosition", e);
		}
		return Modules.IoFileMgrForUserModule.getPosition(this);
	}

	protected void setPosition(long position) {
		Modules.IoFileMgrForUserModule.setPosition(this, position);
		ioLseek(position);
	}

	@Override
	public int ioClose() {
		try {
			file.close();
		} catch (IOException e) {
			log.error("ioClose", e);
			return IO_ERROR;
		}

		return 0;
	}

	@Override
	public int ioRead(TPointer outputPointer, int outputLength) {
		try {
			Utilities.readFully(file, outputPointer.getAddress(), outputLength);
		} catch (IOException e) {
			log.error("ioRead", e);
			return SceKernelErrors.ERROR_KERNEL_FILE_READ_ERROR;
		}

		return outputLength;
	}

	@Override
	public int ioRead(byte[] outputBuffer, int outputOffset, int outputLength) {
		try {
			file.readFully(outputBuffer, outputOffset, outputLength);
		} catch (IOException e) {
			log.error("ioRead", e);
			return SceKernelErrors.ERROR_KERNEL_FILE_READ_ERROR;
		}

		return outputLength;
	}

	@Override
	public int ioWrite(TPointer inputPointer, int inputLength) {
		return IO_ERROR;
	}

	@Override
	public int ioWrite(byte[] inputBuffer, int inputOffset, int inputLength) {
		return IO_ERROR;
	}

	@Override
	public long ioLseek(long offset) {
		try {
			file.seek(offset);
		} catch (IOException e) {
			log.error("ioLseek", e);
			return IO_ERROR;
		}
		return offset;
	}

	@Override
	public int ioIoctl(int command, TPointer inputPointer, int inputLength, TPointer outputPointer, int outputLength) {
		if (log.isEnabledFor(Level.WARN)) {
	        log.warn(String.format("ioIoctl 0x%08X unsupported command, inlen=%d, outlen=%d", command, inputLength, outputLength));
	        if (inputPointer.isAddressGood()) {
	        	log.warn(String.format("ioIoctl indata: %s", Utilities.getMemoryDump(inputPointer.getAddress(), inputLength)));
	        }
	        if (outputPointer.isAddressGood()) {
	        	log.warn(String.format("ioIoctl outdata: %s", Utilities.getMemoryDump(outputPointer.getAddress(), outputLength)));
	        }
		}

		return IO_ERROR;
	}

	@Override
	public long length() {
		try {
			return file.length();
		} catch (IOException e) {
			log.error("length", e);
		}

		return 0;
	}

	@Override
	public boolean isSectorBlockMode() {
		return false;
	}
}
