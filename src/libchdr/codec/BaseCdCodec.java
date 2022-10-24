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

import static jpcsp.util.Utilities.read8;
import static libchdr.Cdrom.CD_FRAME_SIZE;
import static libchdr.Cdrom.CD_MAX_SECTOR_DATA;
import static libchdr.Cdrom.CD_MAX_SUBCODE_DATA;
import static libchdr.ChdHeader.ChdError.CHDERR_NONE;
import static libchdr.codec.Zlib.zlibDecompress;

import libchdr.Chd.CodecData;
import libchdr.ChdHeader.ChdError;

public abstract class BaseCdCodec extends BaseCodec {
	@Override
	public ChdError init(CodecData codec, int hunkbytes) {
		codec.buffer = new byte[hunkbytes];

		return super.init(codec, hunkbytes);
	}

	@Override
	public ChdError decompress(CodecData codec, byte[] src, int srcOffset, int complen, byte[] dest, int destOffset, int destlen) {
		// Determine header bytes
		int frames = destlen / CD_FRAME_SIZE;
		int complenBytes = destlen < 0x10000 ? 2 : 3;
		int eccBytes = (frames + 7) / 8;
		int headerBytes = eccBytes + complenBytes;

		// Extract compressed length of base
		int complenBase = read8(src, srcOffset + eccBytes) << 8;
		complenBase |= read8(src, srcOffset + eccBytes + 1);
		if (complenBytes > 2) {
			complenBase = complenBase << 8 | read8(src, srcOffset + eccBytes + 2);
		}

		byte[] buffer = codec.buffer;
		ChdError err = decompress(src, srcOffset + headerBytes, complenBase, buffer, 0, frames * CD_MAX_SECTOR_DATA);
		if (err != CHDERR_NONE) {
			return err;
		}

		err = zlibDecompress(src, srcOffset + headerBytes + complenBase, complen - complenBase - headerBytes, buffer, frames * CD_MAX_SECTOR_DATA, frames * CD_MAX_SUBCODE_DATA);
		if (err != CHDERR_NONE) {
			return err;
		}

		cdCopyFramesToDest(buffer, frames, dest, destOffset);

		return CHDERR_NONE;
	}

	public static void cdCopyFramesToDest(byte[] buffer, int frames, byte[] dest, int destOffset) {
		for (int frame = 0; frame < frames; frame++) {
			System.arraycopy(buffer, frame * CD_MAX_SECTOR_DATA, dest, destOffset + frame * CD_FRAME_SIZE, CD_MAX_SECTOR_DATA);
			System.arraycopy(buffer, frame * CD_MAX_SUBCODE_DATA + frames * CD_MAX_SECTOR_DATA, dest, destOffset + frame * CD_FRAME_SIZE + CD_MAX_SECTOR_DATA, CD_MAX_SUBCODE_DATA);
		}
	}
}
