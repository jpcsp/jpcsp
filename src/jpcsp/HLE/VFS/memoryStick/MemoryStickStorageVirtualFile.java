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
package jpcsp.HLE.VFS.memoryStick;

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_INVALID_ARGUMENT;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.VFS.AbstractVirtualFile;
import jpcsp.hardware.MemoryStick;

public class MemoryStickStorageVirtualFile extends AbstractVirtualFile {
	public MemoryStickStorageVirtualFile() {
		super(null);
	}

	@Override
	public int ioIoctl(int command, TPointer inputPointer, int inputLength, TPointer outputPointer, int outputLength) {
		int result;

		switch (command) {
			case 0x02125009:
				// Is the memory stick locked?
				if (outputPointer.isNotNull() && outputLength >= 4) {
                    if (log.isDebugEnabled()) {
                    	log.debug(String.format("ioIoctl msstor cmd 0x%08X", command));
                    }
					outputPointer.setValue32(MemoryStick.isLocked());
					result = 0;
				} else {
	                result = ERROR_INVALID_ARGUMENT;
				}
				break;
			case 0x02125008:
				// Is the memory stick inserted?
				if (outputPointer.isNotNull() && outputLength >= 4) {
                    if (log.isDebugEnabled()) {
                    	log.debug(String.format("ioIoctl msstor cmd 0x%08X", command));
                    }
                    // Unknown output value
					outputPointer.setValue32(MemoryStick.isInserted());
					result = 0;
				} else {
	                result = ERROR_INVALID_ARGUMENT;
				}
				break;
			case 0x02125803:
				if (outputPointer.isNotNull() && outputLength >= 96) {
                    if (log.isDebugEnabled()) {
                    	log.debug(String.format("ioIoctl msstor cmd 0x%08X", command));
                    }
                    // Unknown output values
                    outputPointer.clear(96);
                    outputPointer.setStringNZ(12, 16, ""); // This value will be set in registry as /CONFIG/CAMERA/msid
					result = 0;
				} else {
	                result = ERROR_INVALID_ARGUMENT;
				}
				break;
			default:
				result = super.ioIoctl(command, inputPointer, inputLength, outputPointer, outputLength);
				break;
		}

		return result;
	}

	@Override
	public int ioClose() {
		return 0;
	}

	@Override
	public int ioRead(TPointer outputPointer, int outputLength) {
		return IO_ERROR;
	}

	@Override
	public int ioRead(byte[] outputBuffer, int outputOffset, int outputLength) {
		return IO_ERROR;
	}

	@Override
	public long ioLseek(long offset) {
		return IO_ERROR;
	}

	@Override
	public long length() {
		return 0L;
	}
}
