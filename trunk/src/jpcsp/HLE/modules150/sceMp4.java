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
package jpcsp.HLE.modules150;

import static jpcsp.HLE.modules150.IoFileMgrForUser.PSP_SEEK_SET;
import static jpcsp.HLE.modules150.sceAudiocodec.PSP_CODEC_AAC;
import static jpcsp.HLE.modules150.sceMpeg.mpegTimestampPerSecond;
import static jpcsp.util.Utilities.alignUp;
import static jpcsp.util.Utilities.endianSwap32;
import static jpcsp.util.Utilities.getReturnValue64;
import static jpcsp.util.Utilities.readUnaligned32;

import java.util.Arrays;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceMp4SampleInfo;
import jpcsp.HLE.kernel.types.SceMp4TrackSampleBuf;
import jpcsp.HLE.kernel.types.SceMp4TrackSampleBuf.SceMp4TrackSampleBufInfo;
import jpcsp.HLE.kernel.types.SceMpegAu;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.media.codec.CodecFactory;
import jpcsp.media.codec.ICodec;
import jpcsp.media.codec.h264.H264Utils;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

@HLELogging
public class sceMp4 extends HLEModule {
    public static Logger log = Modules.getLogger("sceMp4");
    protected int callbackParam;
	protected int callbackGetCurrentPosition;
	protected int callbackSeek;
	protected int callbackRead;
	protected int readBufferAddr;
	protected int readBufferSize;
	// Values for video track
	protected int[] videoSamplesOffset;
	protected int[] videoSamplesSize;
	protected int videoDuration;
	protected int videoTimeScale;
	// Values for audio track
	protected int[] audioSamplesOffset;
	protected int[] audioSamplesSize;
	protected int audioDuration;
	protected int audioTimeScale;

	protected int[] numberOfSamplesPerChunk;
	protected int[] samplesSize;
	protected long parseOffset;
	protected int duration;
	protected int numberOfTracks;
	protected int trackType;
	protected int[] currentAtomContent;
	protected int currentAtom;
	protected int currentAtomSize;
	protected int currentAtomOffset;
	protected int trackTimeScale;
	protected int trackDuration;
	protected int[] videoCodecExtraData;
	protected SceMp4TrackSampleBuf currentTrack;
	protected TPointer currentTracAddr;
	protected int bufferPutSamples;
	protected int bufferPutCurrentSampleRemainingBytes;
    protected ICodec audioCodec;
    protected int audioChannels;

	public static final int TRACK_TYPE_VIDEO = 0x10;
	public static final int TRACK_TYPE_AUDIO = 0x20;

	protected static final int ATOM_FTYP = 0x66747970; // "ftyp"
	protected static final int ATOM_MOOV = 0x6D6F6F76; // "moov"
	protected static final int ATOM_TRAK = 0x7472616B; // "trak"
	protected static final int ATOM_MDIA = 0x6D646961; // "mdia"
	protected static final int ATOM_MINF = 0x6D696E66; // "minf"
	protected static final int ATOM_STBL = 0x7374626C; // "stbl"
	protected static final int ATOM_STSD = 0x73747364; // "stsd"
	protected static final int ATOM_MVHD = 0x6D766864; // "mvhd"
	protected static final int ATOM_STSC = 0x73747363; // "stsc"
	protected static final int ATOM_STSZ = 0x7374737A; // "stsz"
	protected static final int ATOM_STCO = 0x7374636F; // "stco"
	protected static final int ATOM_MDHD = 0x6D646864; // "mdhd"
	protected static final int ATOM_AVCC = 0x61766343; // "avcC"
	protected static final int FILE_TYPE_MSNV = 0x4D534E56; // "MSNV"
	protected static final int FILE_TYPE_ISOM = 0x69736F6D; // "isom"
	protected static final int DATA_FORMAT_AVC1 = 0x61766331; // "avc1"
	protected static final int DATA_FORMAT_MP4A = 0x6D703461; // "mp4a"

	@Override
    public String getName() {
        return "sceMp4";
    }

	private static boolean isContainerAtom(int atom) {
		switch (atom) {
			case ATOM_MOOV:
			case ATOM_TRAK:
			case ATOM_MDIA:
			case ATOM_MINF:
			case ATOM_STBL:
				return true;
		}

		return false;
	}

	private static boolean isAtomContentRequired(int atom) {
		switch (atom) {
			case ATOM_MVHD:
			case ATOM_STSD:
			case ATOM_STSC:
			case ATOM_STSZ:
			case ATOM_STCO:
			case ATOM_MDHD:
			case ATOM_AVCC:
				return true;
		}

		return false;
	}

	private static String atomToString(int atom) {
		return String.format("%c%c%c%c", (char) (atom >>> 24), (char) ((atom >> 16) & 0xFF), (char) ((atom >> 8) & 0xFF), (char) (atom & 0xFF));
	}

	private static int read32(Memory mem, int addr) {
		return endianSwap32(readUnaligned32(mem, addr));
	}

	private static int read32(int[] content, int o) {
		return (content[o] << 24) | (content[o + 1] << 16) | (content[o + 2] << 8) | content[o + 3];
	}

	private static int read16(int[] content, int o) {
		return (content[o] << 8) | content[o + 1];
	}

	private static int[] extend(int[] array, int length) {
		if (length > 0) {
			if (array == null) {
				array = new int[length];
			} else if (array.length < length) {
				int[] newArray = new int[length];
				System.arraycopy(array,	0, newArray, 0, array.length);
				array = newArray;
			}
		}

		return array;
	}

	private void processAtom(Memory mem, int addr, int atom, int size) {
		int[] content = new int[size];
		for (int i = 0; i < size; i++, addr++) {
			content[i] = mem.read8(addr);
		}

		processAtom(atom, content, size);
	}

	private void setTrackDurationAndTimeScale() {
		if (trackType == 0) {
			return;
		}

		if (currentTrack != null && currentTracAddr != null && currentTrack.isOfType(trackType)) {
			currentTrack.timeScale = trackTimeScale;
			currentTrack.duration = trackDuration;
			currentTrack.write(currentTracAddr);
		}

		switch (trackType) {
			case TRACK_TYPE_VIDEO:
				videoTimeScale = trackTimeScale;
				videoDuration = trackDuration;
				break;
			case TRACK_TYPE_AUDIO:
				audioTimeScale = trackTimeScale;
				audioDuration = trackDuration;
				break;
			default:
				log.error(String.format("processAtom 'mdhd' unknown track type %d", trackType));
				break;
		}
	}

	private void processAtom(int atom, int[] content, int size) {
		switch (atom) {
			case ATOM_MVHD:
				if (size >= 20) {
					duration = read32(content, 16);
				}
				break;
			case ATOM_MDHD:
				if (size >= 20) {
					trackTimeScale = read32(content, 12);
					trackDuration = read32(content, 16);

					setTrackDurationAndTimeScale();
				}
				break;
			case ATOM_STSD:
				if (size >= 16) {
					int dataFormat = read32(content, 12);
					switch (dataFormat) {
						case DATA_FORMAT_AVC1:
							if (log.isDebugEnabled()) {
								log.debug(String.format("trackType video %s", atomToString(dataFormat)));
							}
							trackType = TRACK_TYPE_VIDEO;

							if (size >= 44) {
								int videoFrameWidth = read16(content, 40);
								int videoFrameHeight = read16(content, 42);
								if (log.isDebugEnabled()) {
									log.debug(String.format("Video frame size %dx%d", videoFrameWidth, videoFrameHeight));
								}

								Modules.sceMpegModule.setVideoFrameHeight(videoFrameHeight);
							}
							if (size >= 102) {
								int atomAvcC = read32(content, 98);
								int atomAvcCsize = read32(content, 94);
								if (atomAvcC == ATOM_AVCC && atomAvcCsize <= size - 94) {
									videoCodecExtraData = new int[atomAvcCsize - 8];
									System.arraycopy(content, 102, videoCodecExtraData, 0, videoCodecExtraData.length);
									Modules.sceMpegModule.setVideoCodecExtraData(videoCodecExtraData);
								}
							}
							break;
						case DATA_FORMAT_MP4A:
							if (log.isDebugEnabled()) {
								log.debug(String.format("trackType audio %s", atomToString(dataFormat)));
							}
							trackType = TRACK_TYPE_AUDIO;

							if (size >= 34) {
								audioChannels = read16(content, 32);
							}
							break;
						default:
							log.warn(String.format("Unknown track type 0x%08X(%s)", dataFormat, atomToString(dataFormat)));
							break;
					}

					setTrackDurationAndTimeScale();
				}
				break;
			case ATOM_STSC: {
				numberOfSamplesPerChunk = null;
				if (size >= 8) {
					int numberOfEntries = read32(content, 4);
					if (size >= numberOfEntries * 12 + 8) {
						int offset = 8;
						int previousChunk = 1;
						int previousSamplesPerChunk = 0;
						for (int i = 0; i < numberOfEntries; i++, offset += 12) {
							int firstChunk = read32(content, offset);
							int samplesPerChunk = read32(content, offset + 4);
							numberOfSamplesPerChunk = extend(numberOfSamplesPerChunk, firstChunk);
							for (int j = previousChunk; j < firstChunk; j++) {
								numberOfSamplesPerChunk[j - 1] = previousSamplesPerChunk;
							}
							previousChunk = firstChunk;
							previousSamplesPerChunk = samplesPerChunk;
						}
						numberOfSamplesPerChunk = extend(numberOfSamplesPerChunk, previousChunk);
						numberOfSamplesPerChunk[previousChunk - 1] = previousSamplesPerChunk;
					}
				}
				break;
			}
			case ATOM_STSZ: {
				samplesSize = null;
				if (size >= 8) {
					int sampleSize = read32(content, 4);
					if (sampleSize > 0) {
						samplesSize = new int[1];
						samplesSize[0] = sampleSize;
					} else if (size >= 12) {
						int numberOfEntries = read32(content, 8);
						samplesSize = new int[numberOfEntries];
						int offset = 12;
						for (int i = 0; i < numberOfEntries; i++, offset += 4) {
							samplesSize[i] = read32(content, offset);
						}
					}
				}

				switch (trackType) {
					case TRACK_TYPE_VIDEO:
						videoSamplesSize = samplesSize;
						break;
					case TRACK_TYPE_AUDIO:
						audioSamplesSize = samplesSize;
						break;
					default:
						log.error(String.format("processAtom 'stsz' unknown track type %d", trackType));
						break;
				}
				break;
			}
			case ATOM_STCO: {
				int[] chunksOffset = null;
				if (size >= 8) {
					int numberOfEntries = read32(content, 4);
					chunksOffset = extend(chunksOffset, numberOfEntries);
					int offset = 8;
					for (int i = 0; i < numberOfEntries; i++, offset += 4) {
						chunksOffset[i] = read32(content, offset);
					}
				}

				int[] samplesOffset = null;
				if (numberOfSamplesPerChunk != null && samplesSize != null && chunksOffset != null) {
					// numberOfSamplesPerChunk could be shorter if the last chunks all have the same length.
					// Extend numberOfSamplesPerChunk by repeating the size of the last chunk.
					int compactedChunksLength = numberOfSamplesPerChunk.length;
					numberOfSamplesPerChunk = extend(numberOfSamplesPerChunk, chunksOffset.length);
					Arrays.fill(numberOfSamplesPerChunk, compactedChunksLength, chunksOffset.length, numberOfSamplesPerChunk[compactedChunksLength - 1]);

					// Compute the total number of samples
					int numberOfSamples = 0;
					for (int i = 0; i < numberOfSamplesPerChunk.length; i++) {
						numberOfSamples += numberOfSamplesPerChunk[i];
					}

					// samplesSize could be shorter than the number of samples.
					// Extend samplesSize by repeating the size of the last sample.
					int compactedSamplesLength = samplesSize.length;
					samplesSize = extend(samplesSize, numberOfSamples);
					Arrays.fill(samplesSize, compactedSamplesLength, numberOfSamples, samplesSize[compactedSamplesLength - 1]);

					samplesOffset = new int[numberOfSamples];
					int sample = 0;
					for (int i = 0; i < chunksOffset.length; i++) {
						int offset = chunksOffset[i];
						for (int j = 0; j < numberOfSamplesPerChunk[i]; j++, sample++) {
							samplesOffset[sample] = offset;
							offset += samplesSize[sample];
						}
					}

					if (log.isTraceEnabled()) {
						for (int i = 0; i < samplesOffset.length; i++) {
							log.trace(String.format("Sample#%d offset=0x%X, size=0x%X", i, samplesOffset[i], samplesSize[i]));
						}
					}

					if (currentTrack != null && currentTracAddr != null && currentTrack.isOfType(trackType)) {
						currentTrack.totalNumberSamples = numberOfSamples;
						currentTrack.write(currentTracAddr);
					}
				}

				switch (trackType) {
					case TRACK_TYPE_VIDEO:
						videoSamplesOffset = samplesOffset;
						break;
					case TRACK_TYPE_AUDIO:
						audioSamplesOffset = samplesOffset;
						break;
					default:
						log.error(String.format("processAtom 'stco' unknown track type %d", trackType));
						break;
				}
				break;
			}
		}
	}

	private void processAtom(int atom) {
		switch (atom) {
			case ATOM_TRAK:
				// We start a new track.
				trackType = 0;
				numberOfSamplesPerChunk = null;
				samplesSize = null;
				numberOfTracks++;
				break;
		}
	}

	private void addCurrentAtomContent(Memory mem, int addr, int size) {
		for (int i = 0; i < size; i++) {
			currentAtomContent[currentAtomOffset++] = mem.read8(addr++);
		}
	}

	private void parseAtoms(Memory mem, int addr, int size) {
		int offset = 0;

		if (currentAtom != 0) {
			int length = Math.min(size, currentAtomSize - currentAtomOffset);
			addCurrentAtomContent(mem, addr, length);
			offset += length;

			if (currentAtomOffset >= currentAtomSize) {
				processAtom(currentAtom, currentAtomContent, currentAtomSize);
				currentAtom = 0;
				currentAtomContent = null;
			}
		}

		while (offset + 8 <= size) {
			int atomSize = read32(mem, addr + offset);
			int atom = read32(mem, addr + offset + 4);

			if (log.isDebugEnabled()) {
				log.debug(String.format("parseAtoms atom=0x%08X(%s), size=0x%X, offset=0x%X", atom, atomToString(atom), atomSize, parseOffset + offset));
			}

			if (atomSize <= 0) {
				break;
			}

			if (isAtomContentRequired(atom)) {
				if (offset + atomSize <= size) {
					processAtom(mem, addr + offset + 8, atom, atomSize - 8);
				} else {
					currentAtom = atom;
					currentAtomSize = atomSize - 8;
					currentAtomOffset = 0;
					currentAtomContent = new int[currentAtomSize];
					addCurrentAtomContent(mem, addr + offset + 8, size - offset - 8);
					atomSize = size - offset;
				}
			} else {
				// Process an atom without content
				processAtom(atom);
			}

			if (isContainerAtom(atom)) {
				offset += 8;
			} else {
				offset += atomSize;
			}
		}

		parseOffset += offset;
	}

	private class AfterReadHeadersRead implements IAction {
		@Override
		public void execute() {
			afterReadHeadersRead(Emulator.getProcessor().cpu._v0);
		}
	}

	private class AfterReadHeadersSeek implements IAction {
		@Override
		public void execute() {
			afterReadHeadersSeek(getReturnValue64(Emulator.getProcessor().cpu));
		}
	}

	private void afterReadHeadersRead(int readSize) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("afterReadHeadersRead: %s", Utilities.getMemoryDump(readBufferAddr, readSize)));
		}

		Memory mem = Memory.getInstance();
		if (parseOffset == 0L && readSize >= 12) {
			int header1 = read32(mem, readBufferAddr);
			int header2 = read32(mem, readBufferAddr + 4);
			int header3 = read32(mem, readBufferAddr + 8);
			if (header1 < 12 || header2 != ATOM_FTYP || (header3 != FILE_TYPE_MSNV && header3 != FILE_TYPE_ISOM)) {
				log.warn(String.format("Invalid MP4 file header 0x%08X 0x%08X 0x%08X: %s", header1, header2, header3, Utilities.getMemoryDump(readBufferAddr, Math.min(16, readSize))));
				readSize = 0;
			}
		}

		parseAtoms(mem, readBufferAddr, readSize);

		// Continue reading?
		if (readSize > 0) {
			// Seek to the next atom
			callSeekCallback(new AfterReadHeadersSeek(), parseOffset, PSP_SEEK_SET);
		} else {
			if (log.isTraceEnabled() && currentTrack != null) {
				log.trace(String.format("afterReadHeadersRead updated track %s", currentTrack));
			}
			currentTrack = null;
			currentTracAddr = null;

			Modules.sceMpegModule.setVideoFrameSizes(videoSamplesSize);
		}
	}

	private void afterReadHeadersSeek(long seek) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("afterReadHeadersSeek seek=0x%X", seek));
		}

		callReadCallback(new AfterReadHeadersRead(), readBufferAddr, readBufferSize);
	}

	protected void readHeaders() {
		if (videoSamplesOffset != null && videoSamplesSize != null) {
			return;
		}

		parseOffset = 0L;
		duration = 0;
		currentAtom = 0;
		numberOfTracks = 0;

		// Start reading all the atoms.
		// First seek to the beginning of the file.
		callSeekCallback(new AfterReadHeadersSeek(), parseOffset, PSP_SEEK_SET);
	}

	protected int getSampleOffset(int sample) {
		return getSampleOffset(currentTrack.trackType, sample);
	}

	protected int getSampleOffset(int trackType, int sample) {
		if ((trackType & TRACK_TYPE_AUDIO) != 0) {
			if (audioSamplesOffset == null || sample >= audioSamplesOffset.length) {
				return -1;
			}
			return audioSamplesOffset[sample];
		}
		if ((trackType & TRACK_TYPE_VIDEO) != 0) {
			if (videoSamplesOffset == null || sample >= videoSamplesOffset.length) {
				return -1;
			}
			return videoSamplesOffset[sample];
		}

		log.error(String.format("getSampleOffset unknown trackType=0x%X", trackType));

		return -1;
	}

	protected int getSampleSize(int sample) {
		return getSampleSize(currentTrack.trackType, sample);
	}

	protected int getSampleSize(int trackType, int sample) {
		if ((trackType & TRACK_TYPE_AUDIO) != 0) {
			if (audioSamplesSize == null || sample >= audioSamplesSize.length) {
				return -1;
			}
			return audioSamplesSize[sample];
		}
		if ((trackType & TRACK_TYPE_VIDEO) != 0) {
			if (videoSamplesSize == null || sample >= videoSamplesSize.length) {
				return -1;
			}
			return videoSamplesSize[sample];
		}

		log.error(String.format("getSampleSize unknown trackType=0x%X", trackType));

		return -1;
	}

	private class AfterBufferPutSeek implements IAction {
		@Override
		public void execute() {
			afterBufferPutSeek(getReturnValue64(Emulator.getProcessor().cpu));
		}
	}

	private class AfterBufferPutRead implements IAction {
		@Override
		public void execute() {
			afterBufferPutRead(Emulator.getProcessor().cpu._v0);
		}
	}

	private void afterBufferPutSeek(long seek) {
		currentTrack.currentFileOffset = seek;
		callReadCallback(new AfterBufferPutRead(), currentTrack.readBufferAddr, currentTrack.readBufferSize);
	}

	private void afterBufferPutRead(int size) {
		currentTrack.sizeAvailableInReadBuffer = size;
		bufferPut();
	}

	private void bufferPut(long seek) {
		// PSP is always reading in multiples of readBufferSize
		seek = Utilities.alignDown(seek, currentTrack.readBufferSize - 1);

		callSeekCallback(new AfterBufferPutSeek(), seek, PSP_SEEK_SET);
	}

	private void addBytesToTrack(Memory mem, int addr, int length) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("addBytesToTrack addr=0x%08X, length=0x%X: %s", addr, length, Utilities.getMemoryDump(addr, length)));
		}

		currentTrack.addBytesToTrack(addr, length);

		if (currentTrack.isOfType(TRACK_TYPE_VIDEO)) {
			Modules.sceMpegModule.addToVideoBuffer(mem, addr, length);
		}
	}

	private void bufferPut() {
		Memory mem = Memory.getInstance();
		while (bufferPutSamples > 0) {
			if (log.isTraceEnabled()) {
				log.trace(String.format("bufferPut samples=0x%X, remainingBytes=0x%X, currentTrack=%s", bufferPutSamples, bufferPutCurrentSampleRemainingBytes, currentTrack));
			}

			if (bufferPutCurrentSampleRemainingBytes > 0) {
				int length = Math.min(currentTrack.readBufferSize, bufferPutCurrentSampleRemainingBytes);
				addBytesToTrack(mem, currentTrack.readBufferAddr, length);
				bufferPutCurrentSampleRemainingBytes -= length;

				if (bufferPutCurrentSampleRemainingBytes > 0) {
					bufferPut(currentTrack.currentFileOffset + currentTrack.readBufferSize);
					break;
				}

				currentTrack.addSamplesToTrack(1);
				bufferPutSamples--;
			} else {
				// Read one sample
				int sample = currentTrack.currentSample;
				int sampleOffset = getSampleOffset(sample);
				int sampleSize = getSampleSize(sample);

				if (log.isTraceEnabled()) {
					log.trace(String.format("bufferPut sample=0x%X, offset=0x%X, size=0x%X, currentFilePosition=0x%X, readSize=0x%X", sample, sampleOffset, sampleSize, currentTrack.currentFileOffset, currentTrack.sizeAvailableInReadBuffer));
				}

				if (sampleOffset < 0) {
					if (log.isTraceEnabled()) {
						log.trace(String.format("bufferPut reached last frame at sample 0x%X, stopping", sample));
					}
					bufferPutSamples = 0;
					break;
				}

				if (sampleSize > currentTrack.bufBytes.getWritableSpace()) {
					if (log.isTraceEnabled()) {
						log.trace(String.format("bufferPut bufBytes full (remaining 0x%X bytes, sample size=0x%X), stopping", currentTrack.bufBytes.getWritableSpace(), sampleSize));
					}
					bufferPutSamples = 0;
					break;
				}

				if (currentTrack.isInReadBuffer(sampleOffset)) {
					int sampleReadBufferOffset = (int) (sampleOffset - currentTrack.currentFileOffset);
					int sampleAddr = currentTrack.readBufferAddr + sampleReadBufferOffset;
					if (currentTrack.isInReadBuffer(sampleOffset + sampleSize)) {
						// Sample completely available in the read buffer
						addBytesToTrack(mem, sampleAddr, sampleSize);
						currentTrack.addSamplesToTrack(1);
	
						bufferPutSamples--;
					} else {
						// Sample partially available in the read buffer
						int availableSampleLength = currentTrack.sizeAvailableInReadBuffer - sampleReadBufferOffset;
						addBytesToTrack(mem, sampleAddr, availableSampleLength);
						bufferPutCurrentSampleRemainingBytes = sampleSize - availableSampleLength;

						bufferPut(currentTrack.currentFileOffset + currentTrack.readBufferSize);
						break;
					}
				} else {
					bufferPut(sampleOffset);
					break;
				}
			}
		}

		if (bufferPutSamples <= 0) {
			// sceMp4TrackSampleBufPut is now completed, write the current track back to memory
			currentTrack.write(currentTracAddr);

			Modules.sceMpegModule.hleMpegNotifyVideoDecoderThread();
		}
	}

	protected void bufferPut(SceMp4TrackSampleBuf track, TPointer trackAddr, int samples) {
		currentTrack = track;
		currentTracAddr = trackAddr;
		bufferPutSamples = samples;
		bufferPutCurrentSampleRemainingBytes = 0;

		bufferPut();
	}

	protected void callReadCallback(IAction afterAction, int readAddr, int readBytes) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("callReadCallback readAddr=0x%08X, readBytes=0x%X", readAddr, readBytes));
		}
    	Modules.ThreadManForUserModule.executeCallback(null, callbackRead, afterAction, false, callbackParam, readAddr, readBytes);
	}

	protected void callGetCurrentPositionCallback(IAction afterAction) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("callGetCurrentPositionCallback"));
		}
		Modules.ThreadManForUserModule.executeCallback(null, callbackGetCurrentPosition, afterAction, false, callbackParam);
	}

	protected void callSeekCallback(IAction afterAction, long offset, int whence) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("callSeekCallback offset=0x%X, whence=%s", offset, IoFileMgrForUser.getWhenceName(whence)));
		}
		Modules.ThreadManForUserModule.executeCallback(null, callbackSeek, afterAction, false, callbackParam, 0, (int) (offset & 0xFFFFFFFF), (int) (offset >>> 32), whence);
	}

	protected void hleMp4Init() {
		readBufferAddr = 0;
		readBufferSize = 0;
		videoSamplesOffset = null;
		videoSamplesSize = null;
		audioSamplesOffset = null;
		audioSamplesSize = null;
		trackType = 0;

		// TODO MP4 videos seem to decode with no alpha... or does it depend on the movie data?
		H264Utils.setAlpha(0x00);
	}

	protected void readCallbacks(TPointer32 callbacks) {
    	callbackParam = callbacks.getValue(0);
    	callbackGetCurrentPosition = callbacks.getValue(4);
    	callbackSeek = callbacks.getValue(8);
    	callbackRead = callbacks.getValue(12);
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceMp4 callbacks: param=0x%08X, getCurrentPosition=0x%08X, seek=0x%08X, read=0x%08X", callbackParam, callbackGetCurrentPosition, callbackSeek, callbackRead));
    	}
	}

	@HLEUnimplemented
    @HLEFunction(nid = 0x68651CBC, version = 150, checkInsideInterrupt = true)
    public int sceMp4Init(boolean unk1, boolean unk2) {
		hleMp4Init();

		return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9042B257, version = 150, checkInsideInterrupt = true)
    public int sceMp4Finish() {
		videoSamplesOffset = null;
		videoSamplesSize = null;
		audioSamplesOffset = null;
		audioSamplesSize = null;

		return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB1221EE7, version = 150, checkInsideInterrupt = true)
    public int sceMp4Create(int mp4, TPointer32 callbacks, TPointer readBufferAddr, int readBufferSize) {
    	this.readBufferAddr = readBufferAddr.getAddress();
    	this.readBufferSize = readBufferSize;

    	Modules.sceMpegModule.hleCreateRingbuffer();

    	readCallbacks(callbacks);

    	readHeaders();

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x538C2057, version = 150)
    public int sceMp4Delete() {
		// Reset default alpha
		H264Utils.setAlpha(0xFF);

		return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x113E9E7B, version = 150)
    public int sceMp4GetNumberOfMetaData(int mp4) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7443AF1D, version = 150)
    public int sceMp4GetMovieInfo(int mp4, @CanBeNull TPointer32 movieInfo) {
    	movieInfo.setValue(0, numberOfTracks);
    	movieInfo.setValue(4, 0); // Always 0
    	movieInfo.setValue(8, duration);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5EB65F26, version = 150)
    public int sceMp4GetNumberOfSpecificTrack(int mp4, int trackType) {
    	if ((trackType & TRACK_TYPE_VIDEO) != 0) {
    		return videoSamplesOffset != null ? 1 : 0;
    	}
    	if ((trackType & TRACK_TYPE_AUDIO) != 0) {
    		return audioSamplesOffset != null ? 1 : 0;
    	}

    	log.warn(String.format("sceMp4GetNumberOfSpecificTrack unknown trackType=%X", trackType));

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7ADFD01C, version = 150)
    public int sceMp4RegistTrack(int mp4, int trackType, int unknown, TPointer32 callbacks, TPointer trackAddr) {
    	SceMp4TrackSampleBuf track = new SceMp4TrackSampleBuf();
    	track.read(trackAddr);

		track.currentSample = 0;
		track.trackType = trackType;

		if ((trackType & TRACK_TYPE_VIDEO) != 0) {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("sceMp4RegistTrack TRACK_TYPE_VIDEO"));
    		}
    		track.timeScale = videoTimeScale;
    		track.duration = videoDuration;
    		track.totalNumberSamples = videoSamplesSize != null ? videoSamplesSize.length : 0;
    	}

		if ((trackType & TRACK_TYPE_AUDIO) != 0) {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("sceMp4RegistTrack TRACK_TYPE_AUDIO"));
    		}
    		track.timeScale = audioTimeScale;
    		track.duration = audioDuration;
    		track.totalNumberSamples = audioSamplesSize != null ? audioSamplesSize.length : 0;
    	}

    	readCallbacks(callbacks);

    	track.write(trackAddr);
    	currentTrack = track;
    	currentTracAddr = trackAddr;

    	if (log.isTraceEnabled()) {
    		log.trace(String.format("sceMp4RegistTrack track %s", track));
    	}

    	readHeaders();

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xBCA9389C, version = 150)
    public int sceMp4TrackSampleBufQueryMemSize(int trackType, int numSamples, int sampleSize, int unknown, int readBufferSize) {
        int value = Math.max(numSamples * sampleSize, unknown << 1) + (numSamples << 6) + readBufferSize + 256;
        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceMp4TrackSampleBufQueryMemSize returning 0x%X", value));
        }

        return value;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9C8F4FC1, version = 150)
    public int sceMp4TrackSampleBufConstruct(int mp4, TPointer trackAddr, TPointer buffer, int sampleBufQueyMemSize, int numSamples, int sampleSize, int unknown, int readBufferSize) {
    	// sampleBufQueyMemSize is the value returned by sceMp4TrackSampleBufQueryMemSize

    	SceMp4TrackSampleBuf track = new SceMp4TrackSampleBuf();
    	track.read(trackAddr);

    	track.mp4 = mp4;
    	track.baseBufferAddr = buffer.getAddress();
    	track.samplesPut = numSamples;
    	track.sampleSize = sampleSize;
    	track.unknown = unknown;
    	track.bytesBufferAddr = alignUp(buffer.getAddress(), 63) + (numSamples << 6);
    	track.bytesBufferLength = Math.max(numSamples * sampleSize, unknown << 1);
    	track.readBufferSize = readBufferSize;
    	track.readBufferAddr = track.bytesBufferAddr + track.bytesBufferLength + 48;
    	track.currentFileOffset = -1;
    	track.sizeAvailableInReadBuffer = 0;

		track.bufBytes = new SceMp4TrackSampleBufInfo();
		track.bufBytes.totalSize = track.bytesBufferLength;
		track.bufBytes.readOffset = 0;
		track.bufBytes.writeOffset = 0;
		track.bufBytes.sizeAvailableForRead = 0;
		track.bufBytes.unknown16 = 1;
		track.bufBytes.bufferAddr = track.bytesBufferAddr;
		track.bufBytes.callback24 = 0;
		track.bufBytes.unknown28 = trackAddr.getAddress() + 184;
		track.bufBytes.unknown36 = mp4;

		track.bufSamples = new SceMp4TrackSampleBufInfo();
		track.bufSamples.totalSize = numSamples;
		track.bufSamples.readOffset = 0;
		track.bufSamples.writeOffset = 0;
		track.bufSamples.sizeAvailableForRead = 0;
		track.bufSamples.unknown16 = 64;
		track.bufSamples.bufferAddr = alignUp(buffer.getAddress(), 63);
		track.bufSamples.callback24 = 0;
		track.bufSamples.unknown28 = 0;
		track.bufSamples.unknown36 = mp4;

    	track.write(trackAddr);

    	if (log.isTraceEnabled()) {
    		log.trace(String.format("sceMp4TrackSampleBufConstruct track %s", track));
    	}

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0F0187D2, version = 150)
    public int sceMp4GetAvcTrackInfoData(int mp4, TPointer trackAddr, @CanBeNull TPointer32 infoAddr) {
    	SceMp4TrackSampleBuf track = new SceMp4TrackSampleBuf();
    	track.read(trackAddr);

    	if (log.isTraceEnabled()) {
    		log.trace(String.format("sceMp4GetAvcTrackInfoData track %s", track));
    	}

    	// Returning 3 32-bit values in infoAddr
    	infoAddr.setValue(0, 0); // Always 0
    	infoAddr.setValue(4, track.totalNumberSamples * 3600);
    	infoAddr.setValue(8, track.totalNumberSamples);

    	if (log.isTraceEnabled()) {
    		log.trace(String.format("sceMp4GetAvcTrackInfoData returning info:%s", Utilities.getMemoryDump(infoAddr.getAddress(), 12)));
    	}

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9CE6F5CF, version = 150)
    public int sceMp4GetAacTrackInfoData(int mp4, TPointer trackAddr, @CanBeNull TPointer32 infoAddr) {
    	SceMp4TrackSampleBuf track = new SceMp4TrackSampleBuf();
    	track.read(trackAddr);

    	if (log.isTraceEnabled()) {
    		log.trace(String.format("sceMp4GetAacTrackInfoData track %s", track));
    	}

    	// Returning 5 32-bit values in infoAddr
    	infoAddr.setValue(0, 0); // Always 0
    	infoAddr.setValue(4, track.totalNumberSamples * 1920);
    	infoAddr.setValue(8, track.totalNumberSamples);
    	infoAddr.setValue(12, track.timeScale);
    	infoAddr.setValue(16, audioChannels);

    	if (log.isTraceEnabled()) {
    		log.trace(String.format("sceMp4GetAacTrackInfoData returning info:%s", Utilities.getMemoryDump(infoAddr.getAddress(), 20)));
    	}

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4ED4AB1E, version = 150)
    public int sceMp4AacDecodeInitResource(int unknown) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x10EE0D2C, version = 150)
    public int sceMp4AacDecodeInit(TPointer32 aac) {
    	aac.setValue(0); // Always 0?

		audioCodec = CodecFactory.getCodec(PSP_CODEC_AAC);

		int channels = 2; // Always stereo?
		audioCodec.init(0, channels, channels, 0);

		return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x496E8A65, version = 150)
    public int sceMp4TrackSampleBufFlush(int mp4, TPointer trackAddr) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB4B400D1, version = 150)
    public int sceMp4GetSampleNumWithTimeStamp(int mp4, TPointer trackAddr, TPointer32 timestampAddr) {
    	SceMp4TrackSampleBuf track = new SceMp4TrackSampleBuf();
    	track.read(trackAddr);

    	// Only value at offset 4 is used
    	int timestamp = timestampAddr.getValue(4);

    	if (log.isTraceEnabled()) {
    		log.trace(String.format("sceMp4GetSampleNumWithTimeStamp timestamp=0x%X, track %s", timestamp, track));
    	}

    	int sample = track.currentSample;

    	return sample;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF7C51EC1, version = 150)
    public int sceMp4GetSampleInfo(int mp4, TPointer trackAddr, int sample, TPointer infoAddr) {
    	SceMp4TrackSampleBuf track = new SceMp4TrackSampleBuf();
    	track.read(trackAddr);

    	if (log.isTraceEnabled()) {
    		log.trace(String.format("sceMp4GetSampleInfo track %s", track));
    	}

    	if (sample == 0) {
    		sample = track.currentSample;
    	}

    	SceMp4SampleInfo info = new SceMp4SampleInfo();

    	info.sample = sample;
    	info.sampleOffset = getSampleOffset(track.trackType, sample);
    	info.sampleSize = getSampleSize(track.trackType, sample);
    	info.unknown1 = 0;
    	info.frameDuration = mpegTimestampPerSecond / track.timeScale;
    	info.unknown2 = 0;
    	info.timestamp1 = sample * info.frameDuration;
    	info.timestamp2 = sample * info.frameDuration;

    	info.write(infoAddr);

    	if (log.isTraceEnabled()) {
    		log.trace(String.format("sceMp4GetSampleInfo returning info=%s", info));
    	}

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x74A1CA3E, version = 150)
    public int sceMp4SearchSyncSampleNum(int mp4, TPointer trackAddr, int unknown3, int unknown4) {
    	// unknown3: value 0 or 1

        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD8250B75, version = 150)
    public int sceMp4PutSampleNum(int mp4, TPointer trackAddr, int sample) {
    	SceMp4TrackSampleBuf track = new SceMp4TrackSampleBuf();
    	track.read(trackAddr);

    	if (log.isTraceEnabled()) {
    		log.trace(String.format("sceMp4PutSampleNum track %s", track));
    	}

    	if (sample < 0 || sample > track.totalNumberSamples) {
    		return SceKernelErrors.ERROR_MP4_INVALID_SAMPLE_NUMBER;
    	}

    	track.currentSample = sample;
    	track.write(trackAddr);

    	return 0;
    }

    /**
     * Similar to sceMpegRingbufferAvailableSize.
     * 
     * @param mp4
     * @param trackAddr
     * @param writableSamplesAddr
     * @param writableBytesAddr
     * @return
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x8754ECB8, version = 150)
    public int sceMp4TrackSampleBufAvailableSize(int mp4, TPointer trackAddr, @CanBeNull TPointer32 writableSamplesAddr, @CanBeNull TPointer32 writableBytesAddr) {
    	SceMp4TrackSampleBuf track = new SceMp4TrackSampleBuf();
    	track.read(trackAddr);
    	writableSamplesAddr.setValue(track.bufSamples.getWritableSpace());
    	writableBytesAddr.setValue(track.bufBytes.getWritableSpace());

    	int result = 0;
    	if (writableSamplesAddr.getValue() < 0 || writableBytesAddr.getValue() < 0) {
    		result = SceKernelErrors.ERROR_MP4_NO_AVAILABLE_SIZE;
    	}

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceMp4TrackSampleBufAvailableSize returning writableSamples=0x%X, writableBytes=0x%X, result=0x%08X", writableSamplesAddr.getValue(), writableBytesAddr.getValue(), result));
    	}

    	return result;
    }

    /**
     * Similar to sceMpegRingbufferPut.
     * 
     * @param mp4
     * @param track
     * @param writableSamples
     * @return
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x31BCD7E0, version = 150)
    public int sceMp4TrackSampleBufPut(int mp4, TPointer trackAddr, int writableSamples) {
    	readHeaders();

    	if (writableSamples > 0) {
        	SceMp4TrackSampleBuf track = new SceMp4TrackSampleBuf();
        	track.read(trackAddr);

        	bufferPut(track, trackAddr, writableSamples);
    	}

    	return 0;
    }

    /**
     * Similar to sceMpegGetAtracAu.
     * 
     * @param mp4
     * @param trackAddr
     * @param auAddr
     * @param infoAddr
     * @return
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x5601A6F0, version = 150)
    public int sceMp4GetAacAu(int mp4, TPointer trackAddr, TPointer auAddr, @CanBeNull TPointer infoAddr) {
    	SceMp4TrackSampleBuf track = new SceMp4TrackSampleBuf();
    	track.read(trackAddr);

    	SceMpegAu au = new SceMpegAu();
    	au.read(auAddr);

    	if (log.isTraceEnabled()) {
    		log.trace(String.format("sceMp4GetAacAu track %s, au %s", track, au));
    	}

    	if (track.bufSamples.sizeAvailableForRead <= 0) {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("sceMp4GetAacAu returning ERROR_MP4_NO_MORE_DATA"));
    		}
    		return SceKernelErrors.ERROR_MP4_NO_MORE_DATA;
    	}

    	int sample = track.bufSamples.readOffset;
    	int sampleSize = getSampleSize(track.trackType, sample);

    	// Consume one frame
    	track.bufSamples.readOffset++;
    	track.bufSamples.sizeAvailableForRead--;
    	track.readBytes(au.esBuffer, sampleSize);
    	au.esSize = sampleSize;
    	au.dts = sample * ((long) mpegTimestampPerSecond) * (2048 / audioChannels) / track.timeScale;
    	au.pts = au.dts;

    	if (log.isTraceEnabled()) {
    		log.trace(String.format("sceMp4GetAacAu consuming one frame of size=0x%X, track %s", sampleSize, track));
    	}

    	au.write(auAddr);
    	track.write(trackAddr);

    	if (infoAddr.isNotNull()) {
        	SceMp4SampleInfo info = new SceMp4SampleInfo();

        	info.sample = sample;
        	info.sampleOffset = getSampleOffset(track.trackType, sample);
        	info.sampleSize = getSampleSize(track.trackType, sample);
        	info.unknown1 = 0;
        	info.frameDuration = (int) (((long) mpegTimestampPerSecond) * (2048 / audioChannels) / track.timeScale);
        	info.unknown2 = 0;
        	info.timestamp1 = (int) au.dts;
        	info.timestamp2 = info.timestamp1;

        	info.write(infoAddr);

        	if (log.isTraceEnabled()) {
        		log.trace(String.format("sceMp4GetAacAu returning info=%s", info));
        	}
    	}

    	return 0;
    }

    /**
     * Similar to sceMpegAtracDecode.
     * 
     * @param aac
     * @param auAddr
     * @param outputBufferAddr
     * @param init		1 at first call, 0 afterwards
     * @param frequency	44100
     * @return
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x7663CB5C, version = 150)
    public int sceMp4AacDecode(TPointer32 aac, TPointer auAddr, TPointer bufferAddr, int init, int frequency) {
    	SceMpegAu au = new SceMpegAu();
    	au.read(auAddr);

    	if (log.isTraceEnabled()) {
    		log.trace(String.format("sceMp4AacDecode au=%s, esBuffer:%s", au, Utilities.getMemoryDump(au.esBuffer, au.esSize)));
    	}

    	int result = audioCodec.decode(au.esBuffer, au.esSize, bufferAddr.getAddress());

    	if (result < 0) {
			log.error(String.format("sceMp4AacDecode audio codec returned 0x%08X", result));
    		result = SceKernelErrors.ERROR_MP4_AAC_DECODE_ERROR;
    	} else {
    		result = 0;
    	}

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceMp4AacDecode returning 0x%X", result));
    	}

    	return result;
    }

    /**
     * Similar to sceMpegGetAvcAu.
     * Video decoding is done by sceMpegAvcDecode.
     * 
     * @param mp4
     * @param trackAddr
     * @param auAddr
     * @param infoAddr
     * @return
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x503A3CBA, version = 150)
    public int sceMp4GetAvcAu(int mp4, TPointer trackAddr, TPointer auAddr, @CanBeNull TPointer infoAddr) {
    	SceMp4TrackSampleBuf track = new SceMp4TrackSampleBuf();
    	track.read(trackAddr);

    	if (log.isTraceEnabled()) {
    		log.trace(String.format("sceMp4GetAvcAu track %s", track));
    	}

    	if (track.bufSamples.sizeAvailableForRead <= 0) {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("sceMp4GetAvcAu returning ERROR_MP4_NO_MORE_DATA"));
    		}
    		return SceKernelErrors.ERROR_MP4_NO_MORE_DATA;
    	}

    	SceMpegAu au = new SceMpegAu();
    	au.read(auAddr);
    	Modules.sceMpegModule.setMpegAvcAu(au);

    	int sample = track.bufSamples.readOffset;
    	int sampleSize = getSampleSize(track.trackType, sample);

    	// Consume one frame
    	track.bufSamples.readOffset++;
    	track.bufSamples.sizeAvailableForRead--;
    	track.bufBytes.notifyRead(sampleSize);

    	au.dts = sample * ((long) mpegTimestampPerSecond) / track.timeScale;
    	au.pts = au.dts;

		if (log.isTraceEnabled()) {
    		log.trace(String.format("sceMp4GetAvcAu consuming one frame of size=0x%X, track %s", sampleSize, track));
    	}

    	au.write(auAddr);
    	track.write(trackAddr);

    	if (infoAddr.isNotNull()) {
        	SceMp4SampleInfo info = new SceMp4SampleInfo();

        	info.sample = sample;
        	info.sampleOffset = getSampleOffset(track.trackType, sample);
        	info.sampleSize = getSampleSize(track.trackType, sample);
        	info.unknown1 = 0;
        	info.frameDuration = mpegTimestampPerSecond / track.timeScale;
        	info.unknown2 = 0;
        	info.timestamp1 = (int) au.dts;
        	info.timestamp2 = info.timestamp1;

        	info.write(infoAddr);

        	if (log.isTraceEnabled()) {
        		log.trace(String.format("sceMp4GetAvcAu returning info=%s", info));
        	}
    	}

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x01C76489, version = 150)
    public int sceMp4TrackSampleBufDestruct(int unknown1, int unknown2) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6710FE77, version = 150)
    public int sceMp4UnregistTrack(int unknown1, int unknown2) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5D72B333, version = 150)
    public int sceMp4AacDecodeExit(int unknown) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7D332394, version = 150)
    public int sceMp4AacDecodeTermResource() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x131BDE57, version = 150)
    public int sceMp4InitAu(int mp4, TPointer bufferAddr, TPointer auAddr) {
    	SceMpegAu au = new SceMpegAu();
    	au.esBuffer = bufferAddr.getAddress();
    	au.esSize = 0;
    	au.write(auAddr);

        return 0;
    }
    
    @HLEUnimplemented
    @HLEFunction(nid = 0x17EAA97D, version = 150)
    public int sceMp4GetAvcAuWithoutSampleBuf(int mp4) {
        return 0;
    }
    
    @HLEUnimplemented
    @HLEFunction(nid = 0x28CCB940, version = 150)
    public int sceMp4GetTrackEditList(int mp4) {
        return 0;
    }
    
    @HLEUnimplemented
    @HLEFunction(nid = 0x3069C2B5, version = 150)
    public int sceMp4GetAvcParamSet(int mp4) {
        return 0;
    }
    
    @HLEUnimplemented
    @HLEFunction(nid = 0xD2AC9A7E, version = 150)
    public int sceMp4GetMetaData(int mp4) {
        return 0;
    }
    
    @HLEUnimplemented
    @HLEFunction(nid = 0x4FB5B756, version = 150)
    public int sceMp4GetMetaDataInfo(int mp4) {
        return 0;
    }
    
    @HLEUnimplemented
    @HLEFunction(nid = 0x427BEF7F, version = 150)
    public int sceMp4GetTrackNumOfEditList(int mp4) {
        return 0;
    }
    
    @HLEUnimplemented
    @HLEFunction(nid = 0x532029B8, version = 150)
    public int sceMp4GetAacAuWithoutSampleBuf(int mp4) {
        return 0;
    }
    
    @HLEUnimplemented
    @HLEFunction(nid = 0xA6C724DC, version = 150)
    public int sceMp4GetSampleNum(int mp4) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3C2183C7, version = 150)
    public int mp4msv_3C2183C7(int unknown, @CanBeNull TPointer addr) {
    	if (addr.isNotNull()) {
    		// addr is pointing to five 32-bit values (20 bytes)
    		log.warn(String.format("mp4msv_3C2183C7 unknown values: %s", Utilities.getMemoryDump(addr.getAddress(), 20, 4, 20)));
    	}

    	// mp4msv_3C2183C7 is called by sceMp4Init
    	hleMp4Init();

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9CA13D1A, version = 150)
    public int mp4msv_9CA13D1A(int unknown, @CanBeNull TPointer addr) {
    	if (addr.isNotNull()) {
    		// addr is pointing to 17 32-bit values (68 bytes)
    		log.warn(String.format("mp4msv_9CA13D1A unknown values: %s", Utilities.getMemoryDump(addr.getAddress(), 68, 4, 16)));
    	}

    	// mp4msv_9CA13D1A is called by sceMp4Init
    	hleMp4Init();

    	return 0;
    }
}