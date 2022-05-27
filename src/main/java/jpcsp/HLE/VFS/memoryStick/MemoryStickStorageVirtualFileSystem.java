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

import jpcsp.HLE.Modules;
import jpcsp.HLE.PspString;
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
	private boolean init;

	private void init() {
		if (!init) {
			Modules.sceMSstorModule.hleInit();
			init = true;
		}
	}

	@Override
	public IVirtualFile ioOpen(String fileName, int flags, int mode) {
		init();

		return new MemoryStickStorageVirtualFile();
	}

	@Override
	public int ioDevctl(String deviceName, int command, TPointer inputPointer, int inputLength, TPointer outputPointer, int outputLength) {
		init();

		return Modules.sceMSstorModule.hleMSstorStorageIoDevctl(null, new PspString(deviceName), command, inputPointer, inputLength, outputPointer, outputLength);
	}
}
