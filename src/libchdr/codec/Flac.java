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
import static libchdr.ChdHeader.ChdError.CHDERR_DECOMPRESSION_ERROR;
import static libchdr.ChdHeader.ChdError.CHDERR_NONE;

import java.io.IOException;

import io.nayuki.flac.decode.AbstractFlacLowLevelInput;
import io.nayuki.flac.decode.FlacDecoder;
import libchdr.Chd;
import libchdr.Chd.CodecData;
import libchdr.ChdHeader.ChdError;

public class Flac implements ICodecInterface {
	protected FlacDecoder flacDecoder;
	protected FlacInput flacInput;
	protected static final int numChannels = 2;
	private final byte[] magic = new byte[] {
			(byte) 0x66, (byte) 0x4C, (byte) 0x61, (byte) 0x43  /* 'fLaC' magic header */
	};
	private final byte[] dummyMetadata = new byte[] {
			(byte) 0x80,                                        /* metadata block type 0 (STREAMINFO), flagged as last block */
			(byte) 0x00, (byte) 0x00, (byte) 0x22,              /* metadata block length = 0x22 */
			(byte) 0x00, (byte) 0x00,                           /* minimum block size (will be updated) */
			(byte) 0x00, (byte) 0x00,                           /* maximum block size (will be updated) */
			(byte) 0x00, (byte) 0x00, (byte) 0x00,              /* minimum frame size (0 == unknown) */
			(byte) 0x00, (byte) 0x00, (byte) 0x00,              /* maximum frame size (0 == unknown) */
			(byte) 0x0A, (byte) 0xC4, (byte) 0x42,              /* sample rate (0x0ac44 == 44100), number channels = 2, sample depth = 16 */
			(byte) 0xF0,                                        /* sample depth = 16 */
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, /* number samples (0 == unknown) */
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, /* MD5 hash (0 == none) */
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
	};
	protected final int[][] samples = new int[numChannels][0x10000];

	protected static class FlacInput extends AbstractFlacLowLevelInput {
		private byte[] data;
		private int dataOffset;
		private int dataLength;
		private int position;

		@Override
		public long getLength() {
			return (long) dataLength;
		}

		@Override
		public void seekTo(long pos) throws IOException {
			position = (int) pos;
		}

		@Override
		protected int readUnderlying(byte[] buf, int off, int len) throws IOException {
			int readLength = Math.min(len, dataLength - position);
			System.arraycopy(data, dataOffset + position, buf, off, readLength);
			position += readLength;

			return readLength;
		}

		public void setData(byte[] data, int dataOffset, int dataLength) {
			this.data = data;
			this.dataOffset = dataOffset;
			this.dataLength = dataLength;
			position = 0;
			positionChanged(position);
		}
	}

	@Override
	public ChdError init(CodecData codec, int hunkbytes) {
		flacInput = new FlacInput();
		flacInput.setData(magic, 0, magic.length);
		try {
			flacDecoder = new FlacDecoder(flacInput);
		} catch (IOException e) {
			return CHDERR_DECOMPRESSION_ERROR;
		}

		return CHDERR_NONE;
	}

	@Override
	public void free(CodecData codec) {
	}

	@Override
	public ChdError decompress(CodecData codec, byte[] src, int srcOffset, int complen, byte[] dest, int destOffset, int destlen) {
		Chd.FlacCodecData flac = (Chd.FlacCodecData) codec;
		boolean endianSwap;

		if (read8(src, srcOffset + 0) == 'L') {
			endianSwap = !flac.nativeEndian;
		} else if (read8(src, srcOffset + 0) == 'B') {
			endianSwap = flac.nativeEndian;
		} else {
			return CHDERR_DECOMPRESSION_ERROR;
		}

		try {
			decompressDummyMetadata(getBlocksize(destlen));

			flacInput.setData(src, srcOffset + 1, complen - 1);
			int sampleCount = flacDecoder.readAudioBlock(samples, 0);
			storeSamples(samples, sampleCount, dest, destOffset, destlen, endianSwap);
		} catch (IOException e) {
			return CHDERR_DECOMPRESSION_ERROR;
		}

		return CHDERR_NONE;
	}

	@Override
	public ChdError config(CodecData codec, int param, Object config) {
		return CHDERR_NONE;
	}

	protected void decompressDummyMetadata(int blockSize) throws IOException {
		blockSize *= numChannels;

		// minimum block size
		dummyMetadata[4] = (byte) (blockSize >> 8);
		dummyMetadata[5] = (byte) blockSize;
		// maximum block size == minimum block size
		dummyMetadata[6] = dummyMetadata[4];
		dummyMetadata[7] = dummyMetadata[5];

		flacInput.setData(dummyMetadata, 0, dummyMetadata.length);

		while (flacDecoder.readAndHandleMetadataBlock() != null) {
		}
	}

	protected int getBlockSize(int bytes, int target) {
		// determine FLAC block size, which must be 16-65535
		int blocksize = bytes / 4;
		while (blocksize > target) {
			blocksize /= 2;
		}
		return blocksize;
	}

	private int getBlocksize(int bytes)	{
		// clamp to 2k since that's supposed to be the sweet spot
		return getBlockSize(bytes, 2048);
	}

	protected void storeSamples(int[][] samples, int sampleCount, byte[] dest, int destOffset, int destLength, boolean endianSwap) {
		int destIndex = 0;
		for (int sample = 0; sample < sampleCount; sample++) {
			for (int channel = 0; channel < numChannels; channel++) {
				int value = samples[channel][sample];
				if (destIndex < destLength) {
					if (endianSwap) {
						dest[destOffset + destIndex] = (byte) value;
						dest[destOffset + destIndex + 1] = (byte) (value >> 8);
					} else {
						dest[destOffset + destIndex] = (byte) (value >> 8);
						dest[destOffset + destIndex + 1] = (byte) value;
					}
					destIndex += 2;
				}
			}
		}
	}
}
