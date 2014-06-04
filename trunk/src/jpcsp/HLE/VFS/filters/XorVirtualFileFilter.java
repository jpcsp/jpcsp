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
package jpcsp.HLE.VFS.filters;

import jpcsp.HLE.TPointer;
import jpcsp.HLE.VFS.AbstractProxyVirtualFile;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.memory.IMemoryReaderWriter;
import jpcsp.memory.MemoryReaderWriter;

public class XorVirtualFileFilter extends AbstractProxyVirtualFile implements IVirtualFileFilter {
	private int xor;

	protected XorVirtualFileFilter(byte xor) {
		this.xor = xor & 0xFF;
	}

	@Override
	public int ioRead(TPointer outputPointer, int outputLength) {
		int readLength = super.ioRead(outputPointer, outputLength);
		if (readLength > 0) {
			IMemoryReaderWriter memoryReaderWriter = MemoryReaderWriter.getMemoryReaderWriter(outputPointer.getAddress(), readLength, 1);
			for (int i = 0; i < readLength; i++) {
				int value = memoryReaderWriter.readCurrent();
				value ^= xor;
				memoryReaderWriter.writeNext(value);
			}
			memoryReaderWriter.flush();
		}

		return readLength;
	}

	@Override
	public int ioRead(byte[] outputBuffer, int outputOffset, int outputLength) {
		int readLength = super.ioRead(outputBuffer, outputOffset, outputLength);
		if (readLength > 0) {
			filter(outputBuffer, outputOffset, readLength);
		}

		return readLength;
	}

	@Override
	public void filter(byte[] data, int offset, int length) {
		for (int i = 0; i < length; i++) {
			data[offset + i] ^= xor;
		}
	}

	@Override
	public void setVirtualFile(IVirtualFile vFile) {
		setProxyVirtualFile(vFile);
	}
}
