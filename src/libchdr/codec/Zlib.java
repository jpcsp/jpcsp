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
package libchdr.codec;

import static libchdr.ChdHeader.ChdError.CHDERR_DECOMPRESSION_ERROR;
import static libchdr.ChdHeader.ChdError.CHDERR_NONE;

import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import libchdr.ChdHeader.ChdError;

public class Zlib extends BaseCodec {
	@Override
	protected ChdError decompress(byte[] src, int srcOffset, int complen, byte[] dest, int destOffset, int destlen) {
		return zlibDecompress(src, srcOffset, complen, dest, destOffset, destlen);
	}

	public static ChdError zlibDecompress(byte[] src, int srcOffset, int srcLength, byte[] dest, int destOffset, int destLength) {
		Inflater inflater = new Inflater(true); // ZLIB header and checksum fields are ignored
		inflater.setInput(src, srcOffset, srcLength);
		try {
			int resultLength = inflater.inflate(dest, destOffset, destLength);
			if (resultLength != destLength) {
				return CHDERR_DECOMPRESSION_ERROR;
			}
		} catch (DataFormatException e) {
			return CHDERR_DECOMPRESSION_ERROR;
		} finally {
			inflater.end();
		}

		return CHDERR_NONE;
	}
}
