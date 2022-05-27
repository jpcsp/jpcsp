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

import static jpcsp.HLE.Modules.sceMSstorModule;

import jpcsp.HLE.TPointer;
import jpcsp.HLE.VFS.AbstractVirtualFile;
import jpcsp.hardware.MemoryStick;

public class MemoryStickStorageVirtualFile extends AbstractVirtualFile {
	private long position;

	public MemoryStickStorageVirtualFile() {
		super(null);
	}

	@Override
	public int ioIoctl(int command, TPointer inputPointer, int inputLength, TPointer outputPointer, int outputLength) {
		return sceMSstorModule.hleMSstorPartitionIoIoctl(null, command, inputPointer, inputLength, outputPointer, outputLength);
	}

	@Override
	public int ioClose() {
		return 0;
	}

	@Override
	public int ioRead(TPointer outputPointer, int outputLength) {
		return sceMSstorModule.hleMSstorRawIoRead(position, outputPointer, outputLength);
	}

	@Override
	public int ioRead(byte[] outputBuffer, int outputOffset, int outputLength) {
		return sceMSstorModule.hleMSstorRawIoRead(position, outputBuffer, outputOffset, outputLength);
	}

	@Override
	public int ioWrite(TPointer inputPointer, int inputLength) {
		return sceMSstorModule.hleMSstorRawIoWrite(position, inputPointer, inputLength);
	}

	@Override
	public int ioWrite(byte[] inputBuffer, int inputOffset, int inputLength) {
		return sceMSstorModule.hleMSstorRawIoWrite(position, inputBuffer, inputOffset, inputLength);
	}

	@Override
	public long ioLseek(long offset) {
		position = offset;
		return position;
	}

	@Override
	public long length() {
		return MemoryStick.getTotalSize();
	}

	@Override
	public long getPosition() {
		return position;
	}
}
