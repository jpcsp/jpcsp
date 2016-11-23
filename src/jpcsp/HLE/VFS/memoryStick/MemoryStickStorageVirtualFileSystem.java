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
import jpcsp.HLE.VFS.AbstractVirtualFileSystem;
import jpcsp.HLE.VFS.IVirtualFile;

/**
 * Virtual File System implementing the PSP device msstor0p1.
 *
 * @author gid15
 *
 */
public class MemoryStickStorageVirtualFileSystem extends AbstractVirtualFileSystem {
	@Override
	public IVirtualFile ioOpen(String fileName, int flags, int mode) {
		return new MemoryStickStorageVirtualFile();
	}

	@Override
	public int ioDevctl(String deviceName, int command, TPointer inputPointer, int inputLength, TPointer outputPointer, int outputLength) {
		int result;

		switch (command) {
			case 0x02125802:
				if (outputPointer.isNotNull() && outputLength >= 4) {
                    if (log.isDebugEnabled()) {
                    	log.debug(String.format("ioIoctl msstor cmd 0x%08X", command));
                    }
					// Output value 0x11 or 0x41: the Memory Stick is locked
					outputPointer.setValue32(0);
					result = 0;
				} else {
	                result = ERROR_INVALID_ARGUMENT;
				}
				break;
			default:
				result = super.ioDevctl(deviceName, command, inputPointer, inputLength, outputPointer, outputLength);
				break;
		}

		return result;
	}
}
