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
package jpcsp.format.psmf;

import static jpcsp.HLE.VFS.AbstractVirtualFileSystem.IO_ERROR;

import org.apache.log4j.Logger;

import jpcsp.HLE.TPointer;
import jpcsp.HLE.VFS.AbstractProxyVirtualFile;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.media.MediaEngine;

/**
 * Provides a IVirtualFile interface to read only the audio from a PSMF file.
 * 
 * @author gid15
 *
 */
public class PsmfAudioDemuxVirtualFile extends AbstractProxyVirtualFile {
	private static Logger log = MediaEngine.log;
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

    private byte[] buffer = new byte[1];
    private int audioChannel;
    private long position;
    private int mpegOffset;
    private long startPosition;

    public PsmfAudioDemuxVirtualFile(IVirtualFile vFile, int mpegOffset, int audioChannel) {
		super(vFile);
		this.mpegOffset = mpegOffset;
		this.audioChannel = audioChannel;
		startPosition = vFile.getPosition();

		if (mpegOffset > 0) {
			vFile.ioLseek(startPosition + mpegOffset);
		} else {
			this.mpegOffset = 0;
		}
	}

	private int read8() {
		if (vFile.ioRead(buffer, 0, 1) != 1) {
			return -1;
		}

		return buffer[0] & 0xFF;
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

	private void skip(int n) {
		if (n > 0) {
			vFile.ioLseek(vFile.getPosition() + n);
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

	private boolean isEOF() {
		return vFile.getPosition() >= vFile.length();
	}

	private int doRead(TPointer outputPointer, byte[] outputBuffer, int outputOffset, int outputLength) {
		if (isEOF()) {
			return IO_ERROR;
		}

		int readLength = 0;
		int readAddr = outputPointer != null ? outputPointer.getAddress() : 0;

		while (!isEOF() && readLength < outputLength) {
			long startIndex = vFile.getPosition();
			int startCode = 0xFF;
			while ((startCode & PACKET_START_CODE_MASK) != PACKET_START_CODE_PREFIX && !isEOF()) {
				startCode = (startCode << 8) | read8();
			}
			if (log.isDebugEnabled()) {
				log.debug(String.format("StartCode 0x%08X, offset %08X, skipped %d", startCode, vFile.getPosition(), vFile.getPosition() - startIndex - 4));
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
					int length = read16();
					PesHeader pesHeader = new PesHeader(audioChannel);
					length = readPesHeader(pesHeader, length, startCode);
					if (pesHeader.channel == audioChannel || audioChannel < 0) {
						int packetLength = 0;
						while (packetLength < length) {
							int maxReadLength = Math.min(length - packetLength, outputLength - readLength);
							int l;
							if (outputBuffer != null) {
								l = vFile.ioRead(outputBuffer, outputOffset, maxReadLength);
							} else if (outputPointer != null) {
								l = vFile.ioRead(new TPointer(outputPointer.getMemory(), readAddr), maxReadLength);
							} else {
								l = maxReadLength;
							}

							if (l > 0) {
								readLength += l;
								readAddr += l;
								outputOffset += l;
								packetLength += l;
								position += l;
							} else if (l < 0) {
								break;
							}
						}
					} else {
						skip(length);
					}
					break;
				}
				case 0x1E0: case 0x1E1: case 0x1E2: case 0x1E3:
				case 0x1E4: case 0x1E5: case 0x1E6: case 0x1E7:
				case 0x1E8: case 0x1E9: case 0x1EA: case 0x1EB:
				case 0x1EC: case 0x1ED: case 0x1EE: case 0x1EF: {
					// Video Stream, skipped
					int length = read16();
					skip(length);
					break;
				}
				default: {
					log.warn(String.format("Unknown StartCode 0x%08X, offset %08X", startCode, vFile.getPosition()));
				}
			}
		}

		return readLength;
	}

	@Override
	public int ioRead(TPointer outputPointer, int outputLength) {
		return doRead(outputPointer, null, 0, outputLength);
	}

	@Override
	public int ioRead(byte[] outputBuffer, int outputOffset, int outputLength) {
		return doRead(null, outputBuffer, outputOffset, outputLength);
	}

	@Override
	public long ioLseek(long offset) {
		long result = vFile.ioLseek(startPosition + mpegOffset);
		if (result < 0) {
			return result;
		}

		position = 0;
		while (getPosition() < offset) {
			int length = doRead(null, null, 0, offset < Integer.MAX_VALUE ? (int) offset : Integer.MAX_VALUE);
			if (length < 0) {
				return IO_ERROR;
			}
		}

		return getPosition();
	}

	@Override
	public long length() {
		return super.length() - startPosition - mpegOffset;
	}

	@Override
	public long getPosition() {
		return position;
	}

	@Override
	public IVirtualFile duplicate() {
		return super.duplicate();
	}
}
