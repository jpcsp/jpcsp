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
package jpcsp.connector;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import jpcsp.Memory;
import jpcsp.State;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.SceMpegAu;
import jpcsp.HLE.modules150.sceDisplay;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryWriter;
import jpcsp.util.Debug;
import jpcsp.util.Utilities;

/**
 * @author gid15
 *
 */
public class MpegCodec {
	private int mpegAvcCurrentTimestamp;
	private int mpegAtracCurrentTimestamp;
	private int packetsConsumed;
	protected String id;

    protected int previousVideoBuffer;
    RawFileState videoRawFileState;
    RawFileState audioRawFileState;
    FileState mpegFileState;
    protected int mpegVersion;

	public MpegCodec() {
		videoRawFileState = new RawFileState("VideoStream");
		audioRawFileState = new RawFileState("AudioStream");
		mpegFileState = new FileState("Movie.pmf");
	}

	protected String generateID(int streamSize, long lastTimestamp) {
		return String.format("Mpeg-%d", streamSize);
	}

	public static String getMpegBaseDirectory(String id) {
		return String.format("%s%s/%s/", Connector.baseDirectory, State.discId, id);
	}

	public void init(int mpegVersion, int streamSize, long lastTimestamp) {
		id = generateID(streamSize, lastTimestamp);
		this.mpegVersion = mpegVersion;

		new File(getMpegBaseDirectory(id)).mkdirs();
		generateCommandFile();
		videoRawFileState.init();
		audioRawFileState.init();
		mpegFileState.init(id, mpegVersion);
	}

	public void finish() {
		try {
			videoRawFileState.close();
		} catch (IOException e) {
		}
		try {
			audioRawFileState.close();
		} catch (IOException e) {
		}
		mpegFileState.finish();
	}

	protected void generateCommandFile() {
		try {
			PrintWriter command = new PrintWriter(String.format("%s%s", getMpegBaseDirectory(id), Connector.commandFileName));
			command.println("DecodeVideo");
			command.println(Connector.basePSPDirectory + mpegFileState.name);
			command.println("Exit");
			command.close();
		} catch (FileNotFoundException e) {
			// Ignore Exception
		}
	}

	public void writeVideo(int address, int length) {
		mpegFileState.write(address, length);
	}

	private boolean checkRawFileState(RawFileState rawFileState, int frameCount) {
		if (rawFileState.currentInputPosition >= rawFileState.currentInputLength) {
			if (!readNextRawFile(rawFileState, frameCount)) {
				return false;
			}
		}

		return true;
	}

	public boolean readVideoAu(SceMpegAu au, int frameCount) {
		if (!checkRawFileState(videoRawFileState, frameCount)) {
			return false;
		}

		try {
			decodeVideoAu(videoRawFileState, au);
		} catch (IOException e) {
		}

		return true;
	}

	public boolean readVideoFrame(int buffer, int frameWidth, int width, int height, int pixelMode, int frameCount) {
		if (!checkRawFileState(videoRawFileState, frameCount)) {
			return false;
		}

    	try {
			decodeVideoFrame(videoRawFileState, buffer, frameWidth, width, height, pixelMode);
		} catch (IOException e) {
		}

    	return true;
	}

	public byte[] readEncodedVideoFrame(int frameCount) {
		if (!checkRawFileState(videoRawFileState, frameCount)) {
			return null;
		}

		try {
			return readEncodedVideoFrame(videoRawFileState, frameCount);
		} catch (IOException e) {
		}

		return null;
	}

	private byte[] readEncodedVideoFrame(RawFileState rawFileState, int frameCount) throws IOException {
		byte[] encodedVideoFrame = null;

		rawFileState.read(4);
		if (rawFileState.currentInputLength - rawFileState.currentInputPosition >= 4) {
			int fileSize = getInt32(rawFileState.currentInputBuffer, rawFileState.currentInputPosition);
			if (rawFileState.currentInputLength - rawFileState.currentInputPosition < fileSize) {
				fileSize = rawFileState.currentInputLength - rawFileState.currentInputPosition;
			}
			rawFileState.read(fileSize);

			encodedVideoFrame = new byte[fileSize];
			System.arraycopy(rawFileState.currentInputBuffer, rawFileState.currentInputPosition, encodedVideoFrame, 0, fileSize);

			rawFileState.currentInputPosition += fileSize;
		}

		return encodedVideoFrame;
	}

	public void decodeVideoFrame(byte[] encodedVideoFrame, int buffer, int frameWidth, int width, int height, int pixelMode, int frameCount) {
		RawBufferState rawBufferState = new RawBufferState(encodedVideoFrame);

		try {
			decodeVideoFrame(rawBufferState, buffer, frameWidth, width, height, pixelMode);
		} catch (IOException e) {
		}
	}

	public boolean readAudioAu(SceMpegAu au, int frameCount) {
		if (!checkRawFileState(audioRawFileState, frameCount)) {
			return false;
		}

		try {
			decodeAudioAu(audioRawFileState, au);
		} catch (IOException e) {
		}

		return true;
	}

	public boolean readAudioFrame(int buffer, int frameCount) {
		if (!checkRawFileState(audioRawFileState, frameCount)) {
			return false;
		}

		try {
			decodeAudioFrame(audioRawFileState, buffer);
		} catch (IOException e) {
		}

		return true;
	}

	protected boolean readNextRawFile(RawFileState rawFileState, int frameCount) {
		boolean result = false;

		File rawStreamFile = new File(String.format("%s%s-%d.raw", getMpegBaseDirectory(id), rawFileState.name, frameCount));
    	if (rawStreamFile.exists()) {
    		Modules.log.info("Reading raw stream file " + rawStreamFile);
			try {
				if (rawFileState.currentInputBuffer == null || rawStreamFile.length() > rawFileState.currentInputBuffer.length) {
					rawFileState.currentInputBuffer = new byte[(int) rawStreamFile.length()];
				}
				rawFileState.open(rawStreamFile);

				rawFileState.currentInputPosition = 0;
				rawFileState.currentInputLength = (int) rawStreamFile.length();

				rawFileState.read(4);
				rawFileState.currentFileVersion = getInt32(rawFileState.currentInputBuffer, rawFileState.currentInputPosition);
				rawFileState.currentInputPosition += 4;
				if (rawFileState.currentFileVersion <= 0 || rawFileState.currentFileVersion > 1) {
					Modules.log.warn("Unsupported raw file version " + rawFileState.currentFileVersion);
				}

				result = true;
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
			}
    	}

    	return result;
	}

	protected void decodeVideoAu(RawFileState rawFileState, SceMpegAu au) throws IOException {
		rawFileState.read(8);
		if (rawFileState.currentInputLength - rawFileState.currentInputPosition >= 8) {
			au.pts = getInt32(rawFileState.currentInputBuffer, rawFileState.currentInputPosition + 4);
		}
	}

	protected void decodeVideoFrame(RawFileState rawFileState, int buffer, int frameWidth, int width, int height, int pixelMode) throws IOException {
    	int fileSize = 0;
    	int fileStartPosition = rawFileState.currentInputPosition;
    	rawFileState.read(16);
		if (rawFileState.currentInputLength - rawFileState.currentInputPosition >= 4) {
			fileSize = getInt32(rawFileState.currentInputBuffer, rawFileState.currentInputPosition);
			rawFileState.currentInputPosition += 4;
		}

		if (rawFileState.currentInputLength - rawFileState.currentInputPosition >= 4) {
			mpegAvcCurrentTimestamp = getInt32(rawFileState.currentInputBuffer, rawFileState.currentInputPosition);
			rawFileState.currentInputPosition += 4;
		}

		if (rawFileState.currentInputLength - rawFileState.currentInputPosition >= 8) {
			packetsConsumed = getInt32(rawFileState.currentInputBuffer, rawFileState.currentInputPosition);
			rawFileState.currentInputPosition += 4;
			int totalBytes = getInt32(rawFileState.currentInputBuffer, rawFileState.currentInputPosition);
			rawFileState.currentInputPosition += 4;
			if (Modules.log.isDebugEnabled()) {
				Modules.log.debug("Raw video stream: packetsConsumed=" + packetsConsumed + ", totalBytes=" + totalBytes);
			}
		}

		rawFileState.read(fileSize - 16);
		Memory mem = Memory.getInstance();
		int dst = buffer;
        final int bytesPerPixel = sceDisplay.getPixelFormatBytes(pixelMode);
		for (int y = 0; y < height && rawFileState.currentInputPosition < rawFileState.currentInputLength; y++) {
			int x;
			for (x = 0; x < width; rawFileState.currentInputPosition += 4) {
				int c0    = rawFileState.currentInputBuffer[rawFileState.currentInputPosition + 0] & 0xFF;
				int c1    = rawFileState.currentInputBuffer[rawFileState.currentInputPosition + 1] & 0xFF;
				int c2    = rawFileState.currentInputBuffer[rawFileState.currentInputPosition + 2] & 0xFF;
				int flags = rawFileState.currentInputBuffer[rawFileState.currentInputPosition + 3] & 0xFF;
				int c = Debug.getPixelColor(c0 | (c1 << 8) | (c2 << 16) | 0xFF000000, pixelMode);
				int count = (flags & 0x7F) + 1;
				if ((flags & 0x80) == 0) {
					// Run Length Encoding (RLE): copy count time the same pixel
					IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(dst, count * bytesPerPixel, bytesPerPixel);
					for (int i = 0; i < count; i++) {
						memoryWriter.writeNext(c);
					}
					memoryWriter.flush();
					x += count;
					dst += count * bytesPerPixel;
				} else {
					// Copy count pixels from the previous video image
					if (buffer != previousVideoBuffer) {
						int offset = dst - buffer;
						mem.memcpy(dst, previousVideoBuffer + offset, count * bytesPerPixel);
					}
					dst += count * bytesPerPixel;
					x += count;

					if (bytesPerPixel == 2) {
						mem.write16(dst, (short) c);
					} else {
						mem.write32(dst, c);
					}
					dst += bytesPerPixel;
					x++;
				}
			}
			dst += (frameWidth - x) * bytesPerPixel;
		}
		previousVideoBuffer = buffer;

		if (rawFileState.currentInputPosition - fileStartPosition != fileSize) {
			// Something went wrong during compression or decompression...
			Modules.log.warn(String.format("Video decoding not using complete file: from %d to %d, size=%d", fileStartPosition, rawFileState.currentInputPosition, fileSize));

			// Correct the current position
			rawFileState.currentInputPosition = fileStartPosition + fileSize;
		}
	}

	protected void decodeAudioAu(RawFileState rawFileState, SceMpegAu au) throws IOException {
		rawFileState.read(8);
		if (rawFileState.currentInputLength - rawFileState.currentInputPosition >= 8) {
			au.pts = getInt32(rawFileState.currentInputBuffer, rawFileState.currentInputPosition + 4);
		}
	}

	protected void decodeAudioFrame(RawFileState rawFileState, int buffer) throws IOException {
    	int fileSize = 0;
    	rawFileState.read(8);
    	int fileStartPosition = rawFileState.currentInputPosition;
		if (rawFileState.currentInputLength - rawFileState.currentInputPosition >= 4) {
			fileSize = getInt32(rawFileState.currentInputBuffer, rawFileState.currentInputPosition);
			rawFileState.currentInputPosition += 4;
		}

		if (rawFileState.currentInputLength - rawFileState.currentInputPosition >= 4) {
			mpegAtracCurrentTimestamp = getInt32(rawFileState.currentInputBuffer, rawFileState.currentInputPosition);
			rawFileState.currentInputPosition += 4;
		}

		int length = Math.min(8192, rawFileState.currentInputLength - rawFileState.currentInputPosition);
		if (fileSize != 8192 + 8) {
			Modules.log.warn("Unknown fileSize " + fileSize);
		}
		rawFileState.read(length);
		ByteBuffer byteBuffer = ByteBuffer.wrap(rawFileState.currentInputBuffer, rawFileState.currentInputPosition, length);
		rawFileState.currentInputPosition += length;
		Memory.getInstance().copyToMemory(buffer, byteBuffer, length);

		if (rawFileState.currentInputPosition - fileStartPosition != fileSize) {
			// Something went wrong during compression or decompression...
			Modules.log.warn(String.format("Audio decoding not using complete file: from %d to %d, size=%d", fileStartPosition, rawFileState.currentInputPosition, fileSize));

			// Correct the current position
			rawFileState.currentInputPosition = fileStartPosition + fileSize;
		}
	}

	protected static int getInt32(byte[] buffer, int offset) {
    	int n0 = buffer[offset + 0] & 0xFF;
    	int n1 = buffer[offset + 1] & 0xFF;
    	int n2 = buffer[offset + 2] & 0xFF;
    	int n3 = buffer[offset + 3] & 0xFF;

    	return (n0 << 0) | (n1 << 8) | (n2 << 16) | (n3 << 24);
    }

	public int getMpegAvcCurrentTimestamp() {
		return mpegAvcCurrentTimestamp;
	}

	public int getMpegAtracCurrentTimestamp() {
		return mpegAtracCurrentTimestamp;
	}

	public int getPacketsConsumed() {
		return packetsConsumed;
	}

    public void postFakedVideo(int buffer, int frameWidth, int videoPixelMode) {
		int line = 0;

		// Display additional information about the Mpeg decoding on the faked video
		displayFakedVideoLine(""                                                                      , line++, buffer, frameWidth, videoPixelMode);
		displayFakedVideoLine("The real video file is being saved under"                              , line++, buffer, frameWidth, videoPixelMode);
		displayFakedVideoLine("   " + mpegFileState.getFileName()                                     , line++, buffer, frameWidth, videoPixelMode);
		displayFakedVideoLine(""                                                                      , line++, buffer, frameWidth, videoPixelMode);
		displayFakedVideoLine("Let the faked video run until the end (100%)"                          , line++, buffer, frameWidth, videoPixelMode);
		displayFakedVideoLine("and then copy the PMF file to your real PSP under"                     , line++, buffer, frameWidth, videoPixelMode);
		displayFakedVideoLine("   " + Connector.basePSPDirectory + mpegFileState.name                 , line++, buffer, frameWidth, videoPixelMode);
		displayFakedVideoLine(""                                                                      , line++, buffer, frameWidth, videoPixelMode);
		displayFakedVideoLine("Afterwards, run the '" + Connector.jpcspConnectorName + "' on your"    , line++, buffer, frameWidth, videoPixelMode);
		displayFakedVideoLine("PSP and move all the generated RAW files from your"                    , line++, buffer, frameWidth, videoPixelMode);
		displayFakedVideoLine("PSP"                                                                   , line++, buffer, frameWidth, videoPixelMode);
		displayFakedVideoLine("   " + Connector.basePSPDirectory + "*.raw"                            , line++, buffer, frameWidth, videoPixelMode);
        displayFakedVideoLine("to your computer under Jpcsp:"                                         , line++, buffer, frameWidth, videoPixelMode);
		displayFakedVideoLine("   " + getMpegBaseDirectory(id)                                        , line++, buffer, frameWidth, videoPixelMode);
		displayFakedVideoLine(""                                                                      , line++, buffer, frameWidth, videoPixelMode);
		displayFakedVideoLine("You can then delete the raw files on your PSP."                        , line++, buffer, frameWidth, videoPixelMode);
		displayFakedVideoLine("After this, the video should be displayed"                             , line++, buffer, frameWidth, videoPixelMode);
		displayFakedVideoLine("correctly in Jpcsp when you restart the game."                         , line++, buffer, frameWidth, videoPixelMode);
		displayFakedVideoLine(""                                                                      , line++, buffer, frameWidth, videoPixelMode);
	}

	protected void displayFakedVideoLine(String text, int line, int buffer, int frameWidth, int videoPixelMode) {
		final int baseOffset = 80;
		final int lineWidth = 50;
		String lineText = String.format(" %-" + lineWidth + "s ", text);
        Debug.printFramebuffer(buffer, frameWidth, 10, baseOffset + line * Debug.Font.charHeight, 0xFFFFFFFF, 0xFF000000, videoPixelMode, 1, lineText);
	}

	protected static class RawFileState {
		public String name;
		public byte[] currentInputBuffer;
		public int currentInputLength;
		public int currentInputPosition;
		public int currentFileVersion;
		private FileInputStream inputStream;
		protected int inputStreamPosition;

		public RawFileState(String name) {
			this.name = name;
		}

		public void init() {
			currentInputLength = 0;
			currentInputPosition = 0;
		}

		public void read(int length) throws IOException {
			int requiredPosition = currentInputPosition + length;
			if (inputStreamPosition < requiredPosition) {
				int readLength = requiredPosition - inputStreamPosition;
				readLength = inputStream.read(currentInputBuffer, inputStreamPosition, readLength);
				inputStreamPosition += readLength;
			}
		}

		public void open(File rawFile) throws IOException {
			close();
			inputStream = new FileInputStream(rawFile);
			inputStreamPosition = 0;
		}

		public void close() throws IOException {
			if (inputStream != null) {
				inputStream.close();
				inputStream = null;
				inputStreamPosition = 0;
			}
		}
	}

	protected static class RawBufferState extends RawFileState {
		public RawBufferState(byte[] buffer) {
			super(null);
			currentInputBuffer = buffer;
			currentInputLength = buffer.length;
			currentInputPosition = 0;
			inputStreamPosition = currentInputLength;
		}
	}

	protected static class FileState {
		public String name;
		public String id;
		protected RandomAccessFile output;
		public int mpegVersion;
		protected ByteBuffer byteBuffer;

		public FileState(String name) {
			this.name = name;
		}

		public void init(String id, int mpegVersion) {
			this.id = id;
			this.mpegVersion = mpegVersion;
			output = null;
		}

		public String getFileName() {
			return String.format("%s%s", getMpegBaseDirectory(id), name);
		}

		public void finish() {
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					Modules.log.error(e);
				}
				output = null;
			}
		}

		public void write(int address, int length) {
			if (length <= 0 || !Memory.isAddressGood(address)) {
				return;
			}

			try {
				if (output == null) {
					output = new RandomAccessFile(getFileName(), "rw");
				}

				if (byteBuffer == null || byteBuffer.capacity() < length) {
					byteBuffer = ByteBuffer.allocateDirect(length);
				}

				byteBuffer.clear();
				byteBuffer.limit(length);
				Buffer memoryBuffer = Memory.getInstance().getBuffer(address, length);
				Utilities.putBuffer(byteBuffer, memoryBuffer, ByteOrder.LITTLE_ENDIAN);
				byteBuffer.rewind();
				output.getChannel().write(byteBuffer);
			} catch (FileNotFoundException e) {
				// Ignore this exception
			} catch (IOException e) {
				Modules.log.error(e);
			}
		}
	}
}
