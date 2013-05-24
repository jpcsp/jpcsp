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
 * Provide a IVirtualFile interface by reading from memory.
 * Write access is allowed.
 * 
 * @author gid15
 *
 */
public class MemoryVirtualFile implements IVirtualFile {
	private final int startAddress;
	private int address;
	private final int length;

	public MemoryVirtualFile(int address, int length) {
		this.startAddress = address;
		this.address = address;
		this.length = length;
	}

	@Override
	public int ioClose() {
		address = 0;

		return 0;
	}

	@Override
	public int ioRead(TPointer outputPointer, int outputLength) {
		outputLength = Math.min(outputLength, length - (address - startAddress));
		outputPointer.getMemory().memcpy(outputPointer.getAddress(), address, outputLength);
		address += outputLength;

		return outputLength;
	}

	@Override
	public int ioRead(byte[] outputBuffer, int outputOffset, int outputLength) {
		outputLength = Math.min(outputLength, length - (address - startAddress));
		Utilities.readBytes(address, outputLength, outputBuffer, outputOffset);
		address += outputLength;

		return outputLength;
	}

	@Override
	public int ioWrite(TPointer inputPointer, int inputLength) {
		inputLength = Math.min(inputLength, length - (address - startAddress));
		inputPointer.getMemory().memcpy(address, inputPointer.getAddress(), inputLength);
		address += inputLength;

		return inputLength;
	}

	@Override
	public int ioWrite(byte[] inputBuffer, int inputOffset, int inputLength) {
		inputLength = Math.min(inputLength, length - (address - startAddress));
		Utilities.writeBytes(address, inputLength, inputBuffer, inputOffset);
		address += inputLength;

		return inputLength;
	}

	@Override
	public long ioLseek(long offset) {
		address = startAddress + (int) offset;

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
		return address - startAddress;
	}

	@Override
	public IVirtualFile duplicate() {
		MemoryVirtualFile vFile = new MemoryVirtualFile(startAddress, length);
		vFile.ioLseek(getPosition());

		return vFile;
	}
}
