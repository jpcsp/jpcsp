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

import static jpcsp.HLE.VFS.AbstractVirtualFileSystem.IO_ERROR;

import java.util.Map;

import jpcsp.Memory;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.modules.IoFileMgrForUser;
import jpcsp.HLE.modules.IoFileMgrForUser.IoOperation;
import jpcsp.HLE.modules.IoFileMgrForUser.IoOperationTiming;

/**
 * Provide a IVirtualFile interface by reading and writing from memory.
 * 
 * @author gid15
 *
 */
public class MemoryVirtualFile implements IVirtualFile {
	private final TPointer startPtr;
	private final TPointer ptr;
	private final int length;

	public MemoryVirtualFile(int address, int length) {
		Memory mem = Memory.getInstance();
		this.startPtr = new TPointer(mem, address);
		this.ptr = new TPointer(mem, address);
		this.length = length;
	}

	public MemoryVirtualFile(TPointer ptr, int length) {
		this.startPtr = new TPointer(ptr);
		this.ptr = new TPointer(ptr);
		this.length = length;
	}

	@Override
	public int ioClose() {
		ptr.setAddress(0);

		return 0;
	}

	private int remainingLength() {
		return length - (ptr.getAddress() - startPtr.getAddress());
	}

	@Override
	public int ioRead(TPointer outputPointer, int outputLength) {
		outputLength = Math.min(outputLength, remainingLength());
		outputPointer.memcpy(ptr, outputLength);
		ptr.add(outputLength);

		return outputLength;
	}

	@Override
	public int ioRead(byte[] outputBuffer, int outputOffset, int outputLength) {
		outputLength = Math.min(outputLength, remainingLength());
		ptr.getArray8(0, outputBuffer, outputOffset, outputLength);
		ptr.add(outputLength);

		return outputLength;
	}

	@Override
	public int ioWrite(TPointer inputPointer, int inputLength) {
		inputLength = Math.min(inputLength, remainingLength());
		inputPointer.memcpy(ptr, inputLength);
		ptr.add(inputLength);

		return inputLength;
	}

	@Override
	public int ioWrite(byte[] inputBuffer, int inputOffset, int inputLength) {
		inputLength = Math.min(inputLength, remainingLength());
		ptr.setArray(0, inputBuffer, inputOffset, inputLength);
		ptr.add(inputLength);

		return inputLength;
	}

	@Override
	public long ioLseek(long offset) {
		ptr.setAddress(startPtr.getAddress() + (int) offset);

		return offset;
	}

	@Override
	public int ioIoctl(int command, TPointer inputPointer, int inputLength, TPointer outputPointer, int outputLength) {
		return IO_ERROR;
	}

	@Override
	public long length() {
		return length;
	}

	@Override
	public boolean isSectorBlockMode() {
		return false;
	}

	@Override
	public long getPosition() {
		return ptr.getAddress() - startPtr.getAddress();
	}

	@Override
	public IVirtualFile duplicate() {
		MemoryVirtualFile vFile = new MemoryVirtualFile(startPtr, length);
		vFile.ioLseek(getPosition());

		return vFile;
	}

	@Override
	public Map<IoOperation, IoOperationTiming> getTimings() {
		return IoFileMgrForUser.defaultTimings;
	}

	@Override
	public String toString() {
		return String.format("MemoryVirtualFile %s-%s (length=0x%X)", startPtr, new TPointer(startPtr, length), length);
	}
}
