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
import jpcsp.HLE.TPointer;
import jpcsp.util.Utilities;

/**
 * Provide a IVirtualFile interface by reading from a byte array.
 * No write access is allowed.
 * 
 * @author gid15
 *
 */
public class ByteArrayVirtualFile implements IVirtualFile {
	private byte[] buffer;
	private int offset;
	private int length;
	private int currentIndex;

	public ByteArrayVirtualFile(byte[] buffer) {
		this.buffer = buffer;
		offset = 0;
		length = buffer.length;
	}

	public ByteArrayVirtualFile(byte[] buffer, int offset, int length) {
		this.buffer = buffer;
		this.offset = offset;
		this.length = length;
	}

	@Override
	public int ioClose() {
		buffer = null;
		return 0;
	}

	@Override
	public int ioRead(TPointer outputPointer, int outputLength) {
		outputLength = Math.min(length - (currentIndex - offset), outputLength);
		Utilities.writeBytes(outputPointer.getAddress(), outputLength, buffer, currentIndex);
		currentIndex += outputLength;

		return outputLength;
	}

	@Override
	public int ioRead(byte[] outputBuffer, int outputOffset, int outputLength) {
		outputLength = Math.min(length - (currentIndex - offset), outputLength);
		System.arraycopy(buffer, currentIndex, outputBuffer, outputOffset, outputLength);
		currentIndex += outputLength;

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
		currentIndex = this.offset + Math.min(length, (int) offset);
		return getPosition();
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
		return currentIndex - offset;
	}

	@Override
	public IVirtualFile duplicate() {
		IVirtualFile duplicate = new ByteArrayVirtualFile(buffer, offset, length);
		duplicate.ioLseek(getPosition());

		return duplicate;
	}
}
