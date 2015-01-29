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
package jpcsp.HLE.kernel.types;

import static jpcsp.HLE.modules150.sceMp4.TRACK_TYPE_AUDIO;
import static jpcsp.HLE.modules150.sceMp4.TRACK_TYPE_VIDEO;
import static jpcsp.util.Utilities.alignUp;

public class SceMp4TrackSampleBuf extends pspAbstractMemoryMappedStructure {
	public int mp4;
	public int baseBufferAddr;
	public int samplesPut;
	public int sampleSize;
	public int unknown;
	public int currentSample; // Incremented at each sceMp4TrackSamplePutBuf by the number of samples put
	public int timeScale;
	public int duration;
	public int totalNumberSamples;
	public int trackType;
	public int readBufferAddr;
	public int readBufferSize;
	public long currentFileOffset;
	public int sizeAvailableInReadBuffer;
	public int bytesBufferAddr;
	public int bytesBufferLength;
	public SceMp4TrackSampleBufInfo bufBytes;
	public SceMp4TrackSampleBufInfo bufSamples;

	public static class SceMp4TrackSampleBufInfo extends pspAbstractMemoryMappedStructure {
		public int totalSize;
		public int readOffset;
		public int writeOffset;
		public int sizeAvailableForRead;
		public int unknown16;
		public int bufferAddr;
		public int callback24;
		public int unknown28;
		public int unknown36;

		@Override
		protected void read() {
			totalSize = read32();
			readOffset = read32();
			writeOffset = read32();
			sizeAvailableForRead = read32();
			unknown16 = read32();
			bufferAddr = read32();
			callback24 = read32();
			unknown28 = read32();
			readUnknown(4);
			unknown36 = read32();
		}

		@Override
		protected void write() {
			write32(totalSize);
			write32(readOffset);
			write32(writeOffset);
			write32(sizeAvailableForRead);
			write32(unknown16);
			write32(bufferAddr);
			write32(callback24);
			write32(unknown28);
			writeUnknown(4);
			write32(unknown36);
		}

		public int getWritableSpace() {
			return totalSize - sizeAvailableForRead;
		}

		public void notifyRead(int length) {
			length = Math.min(length, sizeAvailableForRead);
			if (length > 0) {
				readOffset += length;
				if (readOffset >= totalSize) {
					readOffset -= totalSize;
				}
				sizeAvailableForRead -= length;
			}
		}

		@Override
		public int sizeof() {
			return 40;
		}

		@Override
		public String toString() {
			return String.format("SceMp4TrackSampleBufInfo[totalSize=0x%X, readOffset=0x%X, writeOffset=0x%X, sizeAvailableForRead=0x%X, bufferAddr=0x%08X]", totalSize, readOffset, writeOffset, sizeAvailableForRead, bufferAddr);
		}
	}

	@Override
	protected void read() {
		readUnknown(36);
		currentSample = read32();                      // Offset 36
		timeScale = read32();                          // Offset 40
		duration = read32();                           // Offset 44
		totalNumberSamples = read32();                 // Offset 48
		read32();                                      // Offset 52
		read32();                                      // Offset 56
		trackType = read32();                          // Offset 60
		readUnknown(4);
		baseBufferAddr = read32();                     // Offset 68
		samplesPut = read32();                         // Offset 72
		sampleSize = read32();                         // Offset 76
		unknown = read32();                            // Offset 80
		bytesBufferAddr = read32();                    // Offset 84
		bytesBufferLength = read32();                  // Offset 88
		read32();                                      // Offset 92
		read32();                                      // Offset 96
		bufBytes = new SceMp4TrackSampleBufInfo();     // Offset 100
		read(bufBytes);
		bufSamples = new SceMp4TrackSampleBufInfo();   // Offset 140
		read(bufSamples);
		read32();                                      // Offset 180
		currentFileOffset = read64();                  // Offset 184
		read32();                                      // Offset 192
		read32();                                      // Offset 196
		read32();                                      // Offset 200
		read32();                                      // Offset 204
		read32();                                      // Offset 208
		read32();                                      // Offset 212
		read32();                                      // Offset 216
		read32();                                      // Offset 220
		readBufferAddr = read32();                     // Offset 224
		readBufferSize = read32();                     // Offset 228
		sizeAvailableInReadBuffer = read32();          // Offset 232
		read32();                                      // Offset 236
	}

	@Override
	protected void write() {
		writeUnknown(36);
		write32(currentSample);                                   // Offset 36
		write32(timeScale);                                       // Offset 40
		write32(duration);                                        // Offset 44
		write32(totalNumberSamples);                              // Offset 48
		write32(getBaseAddress() + 240);                          // Offset 52
		write32(getBaseAddress() + 72);                           // Offset 56
		write32(trackType);                                       // Offset 60
		writeUnknown(4);
		write32(baseBufferAddr);                                  // Offset 68
		write32(samplesPut);                                      // Offset 72
		write32(sampleSize);                                      // Offset 76
		write32(unknown);                                         // Offset 80
		write32(bytesBufferAddr);                                 // Offset 84
		write32(bytesBufferLength);                               // Offset 88
		write32(alignUp(baseBufferAddr, 63));                     // Offset 92
		write32(samplesPut << 6);                                 // Offset 96
		write(bufBytes);                                          // Offset 100
		write(bufSamples);                                        // Offset 140
		write32(getBaseAddress() + 184);                          // Offset 180
		write64(currentFileOffset);                               // Offset 184
		write32(0);                                               // Offset 192 (callback address in libmp4 module?)
		write32(0);                                               // Offset 196
		write32(0);                                               // Offset 200
		write32(0);                                               // Offset 204
		write32(mp4);                                             // Offset 208
		write32(0);                                               // Offset 212
		write32(0);                                               // Offset 216
		write32(0);                                               // Offset 220
		write32(readBufferAddr);                                  // Offset 224
		write32(readBufferSize);                                  // Offset 228
		write32(sizeAvailableInReadBuffer);                       // Offset 232
		write32(1);                                               // Offset 236
	}

	public boolean isOfType(int trackType) {
		int mask = TRACK_TYPE_VIDEO | TRACK_TYPE_AUDIO;

		return (this.trackType & mask) == (trackType & mask);
	}

	public boolean isInReadBuffer(int offset) {
		return offset >= currentFileOffset && offset < currentFileOffset + sizeAvailableInReadBuffer;
	}

	private void addBytesToTrackSequential(int addr, int length) {
		if (length > 0) {
			mem.memcpy(bufBytes.bufferAddr + bufBytes.writeOffset, addr, length);
			bufBytes.writeOffset += length;
			bufBytes.sizeAvailableForRead += length;
		}
	}

	public void addBytesToTrack(int addr, int length) {
		int length1 = Math.min(length, bufBytes.totalSize - bufBytes.writeOffset);
		addBytesToTrackSequential(addr, length1);

		int length2 = length - length1;
		if (length2 > 0) {
			bufBytes.writeOffset = 0;
			addBytesToTrackSequential(addr + length1, length2);
		}
	}

	public void addSamplesToTrack(int samples) {
		bufSamples.sizeAvailableForRead += samples;
		currentSample += samples;
	}

	public void readBytes(int addr, int length) {
		length = Math.min(length, bufBytes.sizeAvailableForRead);
		if (length > 0) {
			int length1 = Math.min(length, bufBytes.totalSize - bufBytes.readOffset);
			mem.memcpy(addr, bufBytes.bufferAddr + bufBytes.readOffset, length1);

			int length2 = length - length1;
			if (length2 > 0) {
				mem.memcpy(addr + length1, bufBytes.bufferAddr, length2);
			}

			bufBytes.notifyRead(length);
		}
	}

	@Override
	public int sizeof() {
		return 240;
	}

	@Override
	public String toString() {
		return String.format("SceMp4TrackSampleBuf currentSample=0x%X, timeScale=0x%X, duration=0x%X, totalNumberSamples=0x%X, trackType=0x%X, baseBufferAddr=0x%08X, numSamples=0x%X, sampleSize=0x%X, unknown=0x%X, readBufferAddr=0x%08X, readBufferSize=0x%X, currentFileOffset=0x%X, sizeAvailableInReadBuffer=0x%X, bufBytes=%s, bufSamples=%s", currentSample, timeScale, duration, totalNumberSamples, trackType, baseBufferAddr, samplesPut, sampleSize, unknown, readBufferAddr, readBufferSize, currentFileOffset, sizeAvailableInReadBuffer, bufBytes, bufSamples);
	}
}
