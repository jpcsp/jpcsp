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
 * Provide a IVirtualFile interface by reading from another virtual file
 * in blocks on a given size.
 * No write access is supported.
 * 
 * @author gid15
 *
 */
public class BufferedVirtualFile extends AbstractProxyVirtualFile {
	private byte[] buffer;
	private int bufferIndex;
	private int bufferLength;

	public BufferedVirtualFile(IVirtualFile vFile, int bufferSize) {
		super(vFile);
		buffer = new byte[bufferSize];
	}

	private void copyFromBuffer(int outputAddr, int length) {
		if (length <= 0) {
			return;
		}

		Utilities.writeBytes(outputAddr, length, buffer, bufferIndex);
		bufferIndex += length;
	}

	private void copyFromBuffer(byte[] output, int offset, int length) {
		if (length <= 0) {
			return;
		}

		System.arraycopy(buffer, bufferIndex, output, offset, length);
		bufferIndex += length;
	}

	private void checkPopulateBuffer() {
		if (bufferIndex < bufferLength) {
			return;
		}

		bufferLength = vFile.ioRead(buffer, 0, buffer.length);
		bufferIndex = 0;
	}

	@Override
	public int ioRead(TPointer outputPointer, int outputLength) {
		if (bufferLength < 0) {
			return bufferLength;
		}

		int readLength = 0;

		while (bufferLength >= 0 && readLength < outputLength) {
			checkPopulateBuffer();
			int length = Math.min(bufferLength - bufferIndex, outputLength - readLength);
			copyFromBuffer(outputPointer.getAddress() + readLength, length);
			readLength += length;
		}

		return readLength;
	}

	@Override
	public int ioRead(byte[] outputBuffer, int outputOffset, int outputLength) {
		if (bufferLength < 0) {
			return bufferLength;
		}

		int readLength = 0;

		while (bufferLength >= 0 && readLength < outputLength) {
			checkPopulateBuffer();
			int length = Math.min(bufferLength - bufferIndex, outputLength - readLength);
			copyFromBuffer(outputBuffer, outputOffset + readLength, length);
			readLength += length;
		}

		return readLength;
	}

	@Override
	public int ioWrite(TPointer inputPointer, int inputLength) {
		// Write not supported
		return IO_ERROR;
	}

	@Override
	public int ioWrite(byte[] inputBuffer, int inputOffset, int inputLength) {
		// Write not supported
		return IO_ERROR;
	}

	@Override
	public long ioLseek(long offset) {
		long virtualFileOffset = (offset / buffer.length) * buffer.length;
		long result = vFile.ioLseek(virtualFileOffset);
		if (result == IO_ERROR) {
			return result;
		}

		bufferLength = 0;
		bufferIndex = 0;
		if (offset > virtualFileOffset) {
			checkPopulateBuffer();
			bufferIndex = (int) (offset - virtualFileOffset);
		}

		return offset;
	}

	@Override
	public long getPosition() {
		if (bufferLength <= 0) {
			return vFile.getPosition();
		}

		return vFile.getPosition() - bufferLength + bufferIndex;
	}

	@Override
	public IVirtualFile duplicate() {
		IVirtualFile vFileDuplicate = vFile.duplicate();
		if (vFileDuplicate == null) {
			return null;
		}

		BufferedVirtualFile dup = new BufferedVirtualFile(vFileDuplicate, buffer.length);
		dup.ioLseek(getPosition());

		return dup;
	}
}
