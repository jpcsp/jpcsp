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
package jpcsp.util;

import java.io.IOException;
import java.io.InputStream;

import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;

/**
 * @author gid15
 *
 */
public class MemoryInputStream extends InputStream {
	private IMemoryReader memoryReader;

	public MemoryInputStream(int address) {
		memoryReader = MemoryReader.getMemoryReader(address, 1);
	}

	@Override
	public int read() throws IOException {
		return memoryReader.readNext();
	}

	@Override
	public int read(byte[] buffer, int offset, int length) throws IOException {
		for (int i = 0; i < length; i++) {
			buffer[offset + i] = (byte) memoryReader.readNext();
		}

		return length;
	}
}
