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
package jpcsp.format;

import static jpcsp.util.Utilities.endianSwap32;
import static jpcsp.util.Utilities.readUnaligned32;
import jpcsp.Memory;

public class PNG {
	private static final int PNG_MAGIC1 = 0x89504E47; // ".PNG"
	private static final int PNG_MAGIC2 = 0x0D0A1A0A; // "\d\n.\n"
	private static final int CHUNK_TYPE_IEND = 0x49454E44; // "IEND"

	public static int getEndOfPNG(Memory mem, int addr, int size) {
		if (Memory.isAddressGood(addr) && size >= 8) {
			int magic1 = endianSwap32(readUnaligned32(mem, addr));
			int magic2 = endianSwap32(readUnaligned32(mem, addr + 4));
			if (magic1 == PNG_MAGIC1 && magic2 == PNG_MAGIC2) {
				// Find the PNG size by looking for the IEND chunk
				int chunkAddr = addr + 8;
				while (chunkAddr + 12 <= addr + size) {
					int chunkLength = endianSwap32(readUnaligned32(mem, chunkAddr));
					int chunkType = endianSwap32(readUnaligned32(mem, chunkAddr + 4));
					if (chunkAddr + chunkLength + 12 > addr + size) {
						break;
					}
					if (chunkType == CHUNK_TYPE_IEND) {
						size = chunkAddr - addr + chunkLength + 12;
						break;
					}
					chunkAddr += chunkLength + 12;
				}
			}
		}

		return size;
	}
}
