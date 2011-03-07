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
package jpcsp.media;

import java.nio.ByteBuffer;

import jpcsp.HLE.Modules;

import org.apache.log4j.Logger;

public class MpegDemux {
	private static Logger log = Modules.log;
    public static final int PACKET_START_CODE_MASK   = 0xffffff00;
    public static final int PACKET_START_CODE_PREFIX = 0x00000100;

    public static final int SEQUENCE_START_CODE      = 0x000001b3;
    public static final int EXT_START_CODE           = 0x000001b5;
    public static final int SEQUENCE_END_CODE        = 0x000001b7;
    public static final int GOP_START_CODE           = 0x000001b8;
    public static final int ISO_11172_END_CODE       = 0x000001b9;
    public static final int PACK_START_CODE          = 0x000001ba;
    public static final int SYSTEM_HEADER_START_CODE = 0x000001bb;
    public static final int PROGRAM_STREAM_MAP       = 0x000001bc;
    public static final int PRIVATE_STREAM_1         = 0x000001bd;
    public static final int PADDING_STREAM           = 0x000001be;
    public static final int PRIVATE_STREAM_2         = 0x000001bf;

    private byte[] buffer;
	private int index;
	private int length;
	private ByteBuffer videoStream;
	private ByteBuffer audioStream;

	private static class PesHeader {
		public long pts;
		public long dts;
		public int channel;

		public PesHeader(int channel) {
			pts = 0;
			dts = 0;
			this.channel = channel;
		}

		@Override
		public String toString() {
			return String.format("PesHeader(channel=%d, pts=%d, dts=%d)", channel, pts, dts);
		}
	}

	public MpegDemux(byte[] buffer, int offset) {
		this.buffer = buffer;
		this.index = offset;
		this.length = buffer.length;
	}

	private int read8() {
		return buffer[index++] & 0xFF;
	}

	private int read16() {
		return (read8() << 8) | read8();
	}

	private long readPts() {
		return readPts(read8());
	}

	private long readPts(int c) {
		return (((long) (c & 0x0E)) << 29) | ((read16() >> 1) << 15) | (read16() >> 1);
	}

	private boolean isEOF() {
		return index >= length;
	}

	private void skip(int n) {
		if (n > 0) {
			index += n;
		}
	}

	private int readPesHeader(PesHeader pesHeader, int length, int startCode) {
		int c = 0;
		while (length > 0) {
			c = read8();
			length--;
			if (c != 0xFF) {
				break;
			}
		}

		if ((c & 0xC0) == 0x40) {
			read8();
			c = read8();
			length -= 2;
		}
		pesHeader.pts = 0;
		pesHeader.dts = 0;
		if ((c & 0xE0) == 0x20) {
			pesHeader.dts = pesHeader.pts = readPts(c);
			length -= 4;
			if ((c & 0x10) != 0) {
				pesHeader.dts = readPts();
				length -= 5;
			}
		} else if ((c & 0xC0) == 0x80) {
			int flags = read8();
			int headerLength = read8();
			length -= 2;
			length -= headerLength;
			if ((flags & 0x80) != 0) {
				pesHeader.dts = pesHeader.pts = readPts();
				headerLength -= 5;
				if ((flags & 0x40) != 0) {
					pesHeader.dts = readPts();
					headerLength -= 5;
				}
			}
			if ((flags & 0x3F) != 0 && headerLength == 0) {
				flags &= 0xC0;
			}
			if ((flags & 0x01) != 0) {
				int pesExt = read8();
				headerLength--;
				int skip = (pesExt >> 4) & 0x0B;
				skip += skip & 0x09;
				if ((pesExt & 0x40) != 0 || skip > headerLength) {
					pesExt = skip = 0;
				}
				skip(skip);
				headerLength -= skip;
				if ((pesExt & 0x01) != 0) {
					int ext2Length = read8();
					headerLength--;
					 if ((ext2Length & 0x7F) != 0) {
						 int idExt = read8();
						 headerLength--;
						 if ((idExt & 0x80) == 0) {
							 startCode = ((startCode & 0xFF) << 8) | idExt;
						 }
					 }
				}
			}
			skip(headerLength);
		}
		if (startCode == PRIVATE_STREAM_1) {
			int channel = read8();
			pesHeader.channel = channel;
			length--;
			if (channel >= 0x80 && channel <= 0xCF) {
				// Skip audio header
				skip(3);
				length -= 3;
				if (channel >= 0xB0 && channel <= 0xBF) {
					skip(1);
					length--;
				}
			} else {
				// PSP audio has additional 3 bytes in header
				skip(3);
				length -= 3;
			}
		}

		return length;
	}

	private void demuxStream(boolean demuxStream, int startCode, int channel, ByteBuffer byteStream, boolean unescape) {
		int length = read16();
		if (demuxStream) {
			PesHeader pesHeader = new PesHeader(channel);
			length = readPesHeader(pesHeader, length, startCode);
			if (unescape) {
				MpegStream mpegStream = new MpegStream(buffer, index, length);
				while (!mpegStream.isEmpty()) {
					int b = mpegStream.read8();
					byteStream.put((byte) b);
				}
			} else {
				ByteBuffer mpegStream = ByteBuffer.wrap(buffer, index, length);
				byteStream.put(mpegStream);
			}
			skip(length);
			if (log.isDebugEnabled()) {
				log.debug(String.format("Stream %X, channel %d, length %X", startCode, pesHeader.channel, length));
			}
		} else {
			skip(length);
		}
	}

	public void demux(boolean demuxVideo, boolean demuxAudio) {
		if (demuxVideo) {
			videoStream = ByteBuffer.allocate(length - index);
		}
		if (demuxAudio) {
			audioStream = ByteBuffer.allocate(length - index);
		}

		while (index < length) {
			// Search for start code
			int startIndex = index;
			int startCode = 0xFF;
			while ((startCode & PACKET_START_CODE_MASK) != PACKET_START_CODE_PREFIX && !isEOF()) {
				startCode = (startCode << 8) | read8();
			}
			if (log.isDebugEnabled()) {
				log.debug(String.format("StartCode 0x%08X, offset %08X, skipped %d", startCode, index, index - startIndex - 4));
			}

			switch (startCode) {
				case PACK_START_CODE: {
					skip(10);
					break;
				}
				case SYSTEM_HEADER_START_CODE: {
					skip(14);
					break;
				}
				case PADDING_STREAM:
				case PRIVATE_STREAM_2: {
					int length = read16();
					skip(length);
					break;
				}
				case PRIVATE_STREAM_1: {
					// Audio stream
					demuxStream(demuxAudio, startCode, startCode, audioStream, false);
					break;
				}
				case 0x1E0: case 0x1E1: case 0x1E2: case 0x1E3:
				case 0x1E4: case 0x1E5: case 0x1E6: case 0x1E7:
				case 0x1E8: case 0x1E9: case 0x1EA: case 0x1EB:
				case 0x1EC: case 0x1ED: case 0x1EE: case 0x1EF: {
					// Video Stream
					demuxStream(demuxVideo, startCode, startCode & 0x0F, videoStream, true);
					break;
				}
				default: {
					log.warn(String.format("Unknown StartCode 0x%08X, offset %08X", startCode, index));
				}
			}
		}

		if (demuxAudio) {
			if (audioStream.position() > 0) {
				audioStream.limit(audioStream.position());
				audioStream.rewind();
			} else {
				// No audio present
				audioStream = null;
			}
		}

		if (demuxVideo) {
			if (videoStream.position() > 0) {
				videoStream.limit(videoStream.position());
				videoStream.rewind();
			} else {
				// No video present
				videoStream = null;
			}
		}
	}

	public ByteBuffer getAudioStream() {
		return audioStream;
	}

	public ByteBuffer getVideoStream() {
		return videoStream;
	}
}
