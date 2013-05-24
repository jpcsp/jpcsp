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

import jpcsp.HLE.TPointer;

/**
 * Provide a IVirtualFile interface by reading from a part of another virtual file.
 * E.g. a part of virtual file can be considered as a virtual file itself.
 * The part of the virtual file is defined by giving a start offset and a length.
 * 
 * @author gid15
 *
 */
public class PartialVirtualFile extends AbstractProxyVirtualFile {
	private long startPosition;
	private long length;

	public PartialVirtualFile(IVirtualFile vFile, long startPosition, long length) {
		super(vFile);
		this.startPosition = startPosition;
		this.length = length;

		vFile.ioLseek(startPosition);
	}

	private int getRestLength() {
		long restLength = length() - getPosition();
		if (restLength > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}

		return (int) restLength;
	}

	@Override
	public int ioRead(TPointer outputPointer, int outputLength) {
		outputLength = Math.min(outputLength, getRestLength());
		return vFile.ioRead(outputPointer, outputLength);
	}

	@Override
	public int ioRead(byte[] outputBuffer, int outputOffset, int outputLength) {
		outputLength = Math.min(outputLength, getRestLength());
		return vFile.ioRead(outputBuffer, outputOffset, outputLength);
	}

	@Override
	public long ioLseek(long offset) {
		if (offset > length()) {
			return AbstractVirtualFileSystem.IO_ERROR;
		}
		long result = vFile.ioLseek(startPosition + offset);
		if (result == AbstractVirtualFileSystem.IO_ERROR) {
			return result;
		}

		return result - startPosition;
	}

	@Override
	public long length() {
		return length;
	}

	@Override
	public long getPosition() {
		return vFile.getPosition() - startPosition;
	}

	@Override
	public IVirtualFile duplicate() {
		IVirtualFile vFileDuplicate = vFile.duplicate();
		if (vFileDuplicate == null) {
			return null;
		}

		return new PartialVirtualFile(vFileDuplicate, startPosition, length);
	}

	@Override
	public String toString() {
		return String.format("PartialVirtualFile[%s, startPosition=0x%X, length=0x%X]", vFile, startPosition, length);
	}
}
