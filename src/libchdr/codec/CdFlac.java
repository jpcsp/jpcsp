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

import static libchdr.Cdrom.CD_FRAME_SIZE;
import static libchdr.Cdrom.CD_MAX_SECTOR_DATA;
import static libchdr.Cdrom.CD_MAX_SUBCODE_DATA;
import static libchdr.ChdHeader.ChdError.CHDERR_DECOMPRESSION_ERROR;
import static libchdr.ChdHeader.ChdError.CHDERR_NONE;
import static libchdr.codec.BaseCdCodec.cdCopyFramesToDest;
import static libchdr.codec.Zlib.zlibDecompress;

import java.io.IOException;

import libchdr.Chd;
import libchdr.Chd.CodecData;
import libchdr.ChdHeader.ChdError;

public class CdFlac extends Flac {
	@Override
	public ChdError init(CodecData codec, int hunkbytes) {
		Chd.CdflCodecData cdfl = (Chd.CdflCodecData) codec;
		cdfl.buffer = new byte[hunkbytes];

		return super.init(codec, hunkbytes);
	}

	@Override
	public ChdError decompress(CodecData codec, byte[] src, int srcOffset, int complen, byte[] dest, int destOffset, int destlen) {
		Chd.CdflCodecData cdfl = (Chd.CdflCodecData) codec;

		int frames = destlen / CD_FRAME_SIZE;

		try {
			decompressDummyMetadata(getBlocksize(frames * CD_MAX_SECTOR_DATA));
			flacInput.setData(src, srcOffset, complen);
			int sampleOffset = 0;
			for (int i = 0; i < frames; i += 4) {
				int sampleCount = flacDecoder.readAudioBlock(samples, sampleOffset);
				sampleOffset += sampleCount;
			}
			byte[] buffer = cdfl.buffer;
			storeSamples(samples, sampleOffset, buffer, 0, destlen, cdfl.swapEndian);

			// Inflate the subcode data
			int subcodeOffset = (int) flacInput.getPosition();
			int subcodeLength = complen - subcodeOffset;

			ChdError err = zlibDecompress(src, srcOffset + subcodeOffset, subcodeLength, buffer, frames * CD_MAX_SECTOR_DATA, frames * CD_MAX_SUBCODE_DATA);
			if (err != CHDERR_NONE) {
				return err;
			}

			cdCopyFramesToDest(buffer, frames, dest, destOffset);
		} catch (IOException e) {
			return CHDERR_DECOMPRESSION_ERROR;
		}

		return CHDERR_NONE;
	}

	private int getBlocksize(int bytes)	{
		// for CDs it seems that CD_MAX_SECTOR_DATA is the right target
		return getBlockSize(bytes, CD_MAX_SECTOR_DATA);
	}
}
