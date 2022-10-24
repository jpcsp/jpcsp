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

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.tukaani.xz.LZMAInputStream;

import libchdr.ChdHeader.ChdError;

public class Lzma extends BaseCodec {
	private static final int lc = 3;
	private static final int lp = 0;
	private static final int pb = 2;
	private static final byte propsByte = pb * (5 * 9) + lp * 9 + lc;
	private static final int dictSize = LZMAInputStream.DICT_SIZE_MAX;

	@Override
	protected ChdError decompress(byte[] src, int srcOffset, int complen, byte[] dest, int destOffset, int destlen) {
		return lzmaDecompress(src, srcOffset, complen, dest, destOffset, destlen);
	}

	public static ChdError lzmaDecompress(byte[] src, int srcOffset, int complen, byte[] dest, int destOffset, int destlen) {
		LZMAInputStream lzmaInputStream = null;
		try {
			lzmaInputStream = new LZMAInputStream(new ByteArrayInputStream(src, srcOffset, complen), destlen & 0xFFFFFFFFL, propsByte, dictSize);
			while (destlen > 0) {
				int readLength = lzmaInputStream.read(dest, destOffset, destlen);
				if (readLength <= 0) {
					return CHDERR_DECOMPRESSION_ERROR;
				}
				destOffset += readLength;
				destlen -= readLength;
			}
		} catch (IOException e) {
			return CHDERR_DECOMPRESSION_ERROR;
		} finally {
			if (lzmaInputStream != null) {
				try {
					lzmaInputStream.close();
				} catch (IOException e) {
					return CHDERR_DECOMPRESSION_ERROR;
				}
			}
		}

		return CHDERR_NONE;
	}
}
