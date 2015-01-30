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

import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_WAIT_MUTEX;
import static jpcsp.HLE.modules150.IoFileMgrForUser.PSP_SEEK_SET;
import static jpcsp.HLE.modules150.sceAudiocodec.PSP_CODEC_AAC;
import static jpcsp.HLE.modules150.sceMpeg.mpegTimestampPerSecond;
import static jpcsp.util.Utilities.alignUp;
import static jpcsp.util.Utilities.endianSwap32;
import static jpcsp.util.Utilities.getReturnValue64;
import static jpcsp.util.Utilities.readUnaligned32;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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
import jpcsp.HLE.kernel.types.IWaitStateChecker;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.SceMp4SampleInfo;
import jpcsp.HLE.kernel.types.SceMp4TrackSampleBuf;
import jpcsp.HLE.kernel.types.SceMp4TrackSampleBuf.SceMp4TrackSampleBufInfo;
import jpcsp.HLE.kernel.types.SceMpegAu;
import jpcsp.HLE.kernel.types.ThreadWaitInfo;
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
	protected int[] videoSamplesDuration;
	protected int[] videoSamplesPresentationOffset;
	protected int[] videoSyncSamples;
	protected int videoDuration;
	protected int videoTimeScale;
	protected long videoCurrentTimestamp;
	// Values for audio track
	protected int[] audioSamplesOffset;
	protected int[] audioSamplesSize;
	protected int[] audioSamplesDuration;
	protected int[] audioSamplesPresentationOffset;
	protected int[] audioSyncSamples;
	protected int audioDuration;
	protected int audioTimeScale;
	protected long audioCurrentTimestamp;

	protected int[] numberOfSamplesPerChunk;
	protected int[] samplesSize;
	protected long parseOffset;
	protected int timeScale;
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
	protected boolean bufferPutInProgress;
	protected int bufferPutSamples;
	protected int bufferPutCurrentSampleRemainingBytes;
	protected int bufferPutSamplesPut;
	protected SceKernelThreadInfo bufferPutThread;
    protected ICodec audioCodec;
    protected int audioChannels;
    protected List<Integer> threadsWaitingOnBufferPut;

	public static final int TRACK_TYPE_VIDEO = 0x10;
	public static final int TRACK_TYPE_AUDIO = 0x20;

	protected static final int SEARCH_BACKWARDS = 0;
	protected static final int SEARCH_FORWARDS  = 1;

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
	protected static final int ATOM_STTS = 0x73747473; // "stts"
	protected static final int ATOM_CTTS = 0x63747473; // "ctts"
	protected static final int ATOM_STCO = 0x7374636F; // "stco"
	protected static final int ATOM_STSS = 0x73747373; // "stss"
	protected static final int ATOM_MDHD = 0x6D646864; // "mdhd"
	protected static final int ATOM_AVCC = 0x61766343; // "avcC"
	protected static final int FILE_TYPE_MSNV = 0x4D534E56; // "MSNV"
	protected static final int FILE_TYPE_ISOM = 0x69736F6D; // "isom"
	protected static final int FILE_TYPE_MP42 = 0x6D703432; // "mp42"
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
			case ATOM_STTS:
			case ATOM_CTTS:
			case ATOM_STCO:
			case ATOM_STSS:
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
					timeScale = read32(content, 12);
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
			case ATOM_STTS: {
				int[] samplesDuration = null;
				if (size >= 8) {
					int numberOfEntries = read32(content, 4);
					int offset = 8;
					int sample = 0;
					for (int i = 0; i < numberOfEntries; i++, offset += 8) {
						int sampleCount = read32(content, offset);
						int sampleDuration = read32(content, offset + 4);
						samplesDuration = extend(samplesDuration, sample + sampleCount);
						Arrays.fill(samplesDuration, sample, sample + sampleCount, sampleDuration);
						sample += sampleCount;
					}
				}

				switch (trackType) {
					case TRACK_TYPE_VIDEO:
						videoSamplesDuration = samplesDuration;
						break;
					case TRACK_TYPE_AUDIO:
						audioSamplesDuration = samplesDuration;
						break;
					default:
						log.error(String.format("processAtom 'stts' unknown track type %d", trackType));
						break;
				}
				break;
			}
			case ATOM_CTTS: {
				int samplesPresentationOffset[] = null;
				if (size >= 8) {
					int numberOfEntries = read32(content, 4);
					int offset = 8;
					int sample = 0;
					for (int i = 0; i < numberOfEntries; i++, offset += 8) {
						int sampleCount = read32(content, offset);
						int samplePresentationOffset = read32(content, offset + 4);
						samplesPresentationOffset = extend(samplesPresentationOffset, sample + sampleCount);
						Arrays.fill(samplesPresentationOffset, sample, sample + sampleCount, samplePresentationOffset);
						sample += sampleCount;
					}
				}

				switch (trackType) {
					case TRACK_TYPE_VIDEO:
						videoSamplesPresentationOffset = samplesPresentationOffset;
						break;
					case TRACK_TYPE_AUDIO:
						audioSamplesPresentationOffset = samplesPresentationOffset;
						break;
					default:
						log.error(String.format("processAtom 'ctts' unknown track type %d", trackType));
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
			case ATOM_STSS: {
				int[] syncSamples = null;
				if (size >= 8) {
					int numberOfEntries = read32(content, 4);
					syncSamples = new int[numberOfEntries];
					int offset = 8;
					for (int i = 0; i < numberOfEntries; i++, offset += 4) {
						syncSamples[i] = read32(content, offset) - 1; // Sync samples are numbered starting at 1
						if (log.isTraceEnabled()) {
							log.trace(String.format("Sync sample#%d=0x%X", i, syncSamples[i]));
						}
					}
				}

				switch (trackType) {
					case TRACK_TYPE_VIDEO:
						videoSyncSamples = syncSamples;
						break;
					case TRACK_TYPE_AUDIO:
						audioSyncSamples = syncSamples;
						break;
					default:
						log.error(String.format("processAtom 'stss' unknown track type %d", trackType));
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
			if (header1 < 12 || header2 != ATOM_FTYP || (header3 != FILE_TYPE_MSNV && header3 != FILE_TYPE_ISOM && header3 != FILE_TYPE_MP42)) {
				log.warn(String.format("Invalid MP4 file header 0x%08X 0x%08X 0x%08X: %s", header1, header2, header3, Utilities.getMemoryDump(readBufferAddr, Math.min(16, readSize))));
				readSize = 0;
			}
		}

		parseAtoms(mem, readBufferAddr, readSize);

		// Continue reading?
		if (readSize > 0) {
			// Seek to the next atom
			callSeekCallback(null, new AfterReadHeadersSeek(), parseOffset, PSP_SEEK_SET);
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

		callReadCallback(null, new AfterReadHeadersRead(), readBufferAddr, readBufferSize);
	}

	protected void readHeaders(SceMp4TrackSampleBuf track, TPointer trackAddr) {
		if (videoSamplesOffset != null && videoSamplesSize != null) {
			return;
		}

		parseOffset = 0L;
		duration = 0;
		currentAtom = 0;
		numberOfTracks = 0;
		currentTrack = track;
		currentTracAddr = trackAddr;

		// Start reading all the atoms.
		// First seek to the beginning of the file.
		callSeekCallback(null, new AfterReadHeadersSeek(), parseOffset, PSP_SEEK_SET);
	}

	protected int getSampleOffset(int sample) {
		return getSampleOffset(currentTrack.trackType, sample);
	}

	protected int getSampleOffset(int trackType, int sample) {
		if ((trackType & TRACK_TYPE_AUDIO) != 0) {
			if (audioSamplesOffset == null || sample < 0 || sample >= audioSamplesOffset.length) {
				return -1;
			}
			return audioSamplesOffset[sample];
		}
		if ((trackType & TRACK_TYPE_VIDEO) != 0) {
			if (videoSamplesOffset == null || sample < 0 || sample >= videoSamplesOffset.length) {
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
			if (audioSamplesSize == null || sample < 0 || sample >= audioSamplesSize.length) {
				return -1;
			}
			return audioSamplesSize[sample];
		}
		if ((trackType & TRACK_TYPE_VIDEO) != 0) {
			if (videoSamplesSize == null || sample < 0 || sample >= videoSamplesSize.length) {
				return -1;
			}
			return videoSamplesSize[sample];
		}

		log.error(String.format("getSampleSize unknown trackType=0x%X", trackType));

		return -1;
	}

	protected int getSampleDuration(int sample) {
		return getSampleDuration(currentTrack.trackType, sample);
	}

	protected int getSampleDuration(int trackType, int sample) {
		if ((trackType & TRACK_TYPE_AUDIO) != 0) {
			if (audioSamplesDuration == null || sample < 0 || sample >= audioSamplesDuration.length) {
				return -1;
			}
			return audioSamplesDuration[sample];
		}
		if ((trackType & TRACK_TYPE_VIDEO) != 0) {
			if (videoSamplesDuration == null || sample < 0 || sample >= videoSamplesDuration.length) {
				return -1;
			}
			return videoSamplesDuration[sample];
		}

		log.error(String.format("getSampleDuration unknown trackType=0x%X", trackType));

		return -1;
	}

	protected int getSamplePresentationOffset(int sample) {
		return getSamplePresentationOffset(currentTrack.trackType, sample);
	}

	protected int getSamplePresentationOffset(int trackType, int sample) {
		if ((trackType & TRACK_TYPE_AUDIO) != 0) {
			if (audioSamplesPresentationOffset == null || sample < 0 || sample >= audioSamplesPresentationOffset.length) {
				return 0;
			}
			return audioSamplesPresentationOffset[sample];
		}
		if ((trackType & TRACK_TYPE_VIDEO) != 0) {
			if (videoSamplesPresentationOffset == null || sample < 0 || sample >= videoSamplesPresentationOffset.length) {
				return 0;
			}
			return videoSamplesPresentationOffset[sample];
		}

		log.error(String.format("getSamplePresentationOffset unknown trackType=0x%X", trackType));

		return 0;
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
		callReadCallback(bufferPutThread, new AfterBufferPutRead(), currentTrack.readBufferAddr, currentTrack.readBufferSize);
	}

	private void afterBufferPutRead(int size) {
		currentTrack.sizeAvailableInReadBuffer = size;
		bufferPut();
	}

	private void bufferPut(long seek) {
		// PSP is always reading in multiples of readBufferSize
		seek = Utilities.alignDown(seek, currentTrack.readBufferSize - 1);

		callSeekCallback(bufferPutThread, new AfterBufferPutSeek(), seek, PSP_SEEK_SET);
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
				bufferPutSamplesPut++;
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
						bufferPutSamplesPut++;
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

		if (bufferPutSamples <= 0 && bufferPutInProgress) {
			// sceMp4TrackSampleBufPut is now completed, write the current track back to memory...
			currentTrack.write(currentTracAddr);

			// ... and return the number of samples put
			if (log.isTraceEnabled()) {
				log.trace(String.format("bufferPut returning 0x%X for thread %s", bufferPutSamplesPut, bufferPutThread));
			}
			bufferPutThread.cpuContext._v0 = bufferPutSamplesPut;

			Modules.sceMpegModule.hleMpegNotifyVideoDecoderThread();

			bufferPutInProgress = false;

			if (!threadsWaitingOnBufferPut.isEmpty()) {
				int threadUid = threadsWaitingOnBufferPut.remove(0).intValue();
				if (log.isTraceEnabled()) {
					log.trace(String.format("bufferPut unblocking thread %s", Modules.ThreadManForUserModule.getThreadById(threadUid)));
				}
				Modules.ThreadManForUserModule.hleUnblockThread(threadUid);
			}
		}
	}

	private class StartBufferPut implements IAction {
		private SceMp4TrackSampleBuf track;
		private TPointer trackAddr;
		private int samples;
		private SceKernelThreadInfo thread;

		public StartBufferPut(SceMp4TrackSampleBuf track, TPointer trackAddr, int samples, SceKernelThreadInfo thread) {
			this.track = track;
			this.trackAddr = trackAddr;
			this.samples = samples;
			this.thread = thread;
		}

		@Override
		public void execute() {
			bufferPut(thread, track, trackAddr, samples);
		}
	}

	private class BufferPutUnblock implements IAction {
		private SceMp4TrackSampleBuf track;
		private TPointer trackAddr;
		private int samples;
		private SceKernelThreadInfo thread;

		public BufferPutUnblock(SceMp4TrackSampleBuf track, TPointer trackAddr, int samples, SceKernelThreadInfo thread) {
			this.track = track;
			this.trackAddr = trackAddr;
			this.samples = samples;
			this.thread = thread;
		}

		@Override
		public void execute() {
			// Start bufferPut in the thread context when it will be scheduled
			Modules.ThreadManForUserModule.pushActionForThread(thread, new StartBufferPut(track, trackAddr, samples, thread));
		}
	}

	private class BufferPutWaitStateChecker implements IWaitStateChecker {
		@Override
		public boolean continueWaitState(SceKernelThreadInfo thread, ThreadWaitInfo wait) {
			if (log.isTraceEnabled()) {
				log.trace(String.format("BufferPutWaitStateChecker.continueWaitState for thread %s returning %b", thread, threadsWaitingOnBufferPut.contains(thread.uid)));
			}

			if (threadsWaitingOnBufferPut.contains(thread.uid)) {
				return true;
			}

			return false;
		}
	}

	protected void bufferPut(SceKernelThreadInfo thread, SceMp4TrackSampleBuf track, TPointer trackAddr, int samples) {
		if (bufferPutInProgress) {
			if (log.isTraceEnabled()) {
				log.trace(String.format("bufferPut blocking thread %s", thread));
			}
			BufferPutUnblock bufferPutUnblock = new BufferPutUnblock(track, trackAddr, samples, thread);
			threadsWaitingOnBufferPut.add(thread.uid);
			Modules.ThreadManForUserModule.hleBlockThread(thread, PSP_WAIT_MUTEX, 0, false, bufferPutUnblock, new BufferPutWaitStateChecker());
		} else {
			if (log.isTraceEnabled()) {
				log.trace(String.format("bufferPut starting samples=0x%X, thread=%s", samples, thread));
			}
			bufferPutInProgress = true;
			currentTrack = track;
			currentTracAddr = trackAddr;
			bufferPutSamples = samples;
			bufferPutCurrentSampleRemainingBytes = 0;
			bufferPutSamplesPut = 0;
			bufferPutThread = thread;

			bufferPut();
		}
	}

	protected void callReadCallback(SceKernelThreadInfo thread, IAction afterAction, int readAddr, int readBytes) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("callReadCallback readAddr=0x%08X, readBytes=0x%X", readAddr, readBytes));
		}
    	Modules.ThreadManForUserModule.executeCallback(thread, callbackRead, afterAction, false, callbackParam, readAddr, readBytes);
	}

	protected void callGetCurrentPositionCallback(SceKernelThreadInfo thread, IAction afterAction) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("callGetCurrentPositionCallback"));
		}
		Modules.ThreadManForUserModule.executeCallback(thread, callbackGetCurrentPosition, afterAction, false, callbackParam);
	}

	protected void callSeekCallback(SceKernelThreadInfo thread, IAction afterAction, long offset, int whence) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("callSeekCallback offset=0x%X, whence=%s", offset, IoFileMgrForUser.getWhenceName(whence)));
		}
		Modules.ThreadManForUserModule.executeCallback(thread, callbackSeek, afterAction, false, callbackParam, 0, (int) (offset & 0xFFFFFFFF), (int) (offset >>> 32), whence);
	}

	protected void hleMp4Init() {
		readBufferAddr = 0;
		readBufferSize = 0;
		videoSamplesOffset = null;
		videoSamplesSize = null;
		videoSamplesDuration = null;
		videoSamplesPresentationOffset = null;
		videoCurrentTimestamp = 0L;
		audioSamplesOffset = null;
		audioSamplesSize = null;
		audioSamplesDuration = null;
		audioSamplesPresentationOffset = null;
		audioCurrentTimestamp = 0L;
		trackType = 0;
		threadsWaitingOnBufferPut = new LinkedList<Integer>();
		bufferPutInProgress = false;

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

	protected long sampleToFrameDuration(long sampleDuration, SceMp4TrackSampleBuf track) {
		return sampleToFrameDuration(sampleDuration, track.timeScale);
	}

	protected long sampleToFrameDuration(long sampleDuration, int timeScale) {
		if (timeScale == 0) {
			return sampleDuration;
		}
		return sampleDuration * mpegTimestampPerSecond / timeScale;
	}

	protected long getTotalFrameDuration(SceMp4TrackSampleBuf track) {
    	long totalSampleDuration = 0L;
    	for (int sample = 0; sample < track.totalNumberSamples; sample++) {
    		int sampleDuration = getSampleDuration(track.trackType, sample);
    		totalSampleDuration += sampleDuration;
    	}

    	long totalFrameDuration = sampleToFrameDuration(totalSampleDuration, track);

    	return totalFrameDuration;
	}

    @HLEFunction(nid = 0x68651CBC, version = 150, checkInsideInterrupt = true)
    public int sceMp4Init(boolean unk1, boolean unk2) {
		hleMp4Init();

		return 0;
    }

    @HLEFunction(nid = 0x9042B257, version = 150, checkInsideInterrupt = true)
    public int sceMp4Finish() {
		videoSamplesOffset = null;
		videoSamplesSize = null;
		audioSamplesOffset = null;
		audioSamplesSize = null;
		currentTrack = null;
		currentTracAddr = null;

		return 0;
    }

    @HLELogging(level="info")
    @HLEFunction(nid = 0xB1221EE7, version = 150, checkInsideInterrupt = true)
    public int sceMp4Create(int mp4, TPointer32 callbacks, TPointer readBufferAddr, int readBufferSize) {
    	this.readBufferAddr = readBufferAddr.getAddress();
    	this.readBufferSize = readBufferSize;

    	Modules.sceMpegModule.hleCreateRingbuffer();

    	readCallbacks(callbacks);

    	readHeaders(null, null);

    	return 0;
    }

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

    @HLEFunction(nid = 0x7443AF1D, version = 150)
    public int sceMp4GetMovieInfo(int mp4, @CanBeNull TPointer32 movieInfo) {
    	movieInfo.setValue(0, numberOfTracks);
    	movieInfo.setValue(4, 0); // Always 0
    	movieInfo.setValue(8, (int) sampleToFrameDuration(duration, timeScale));

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceMp4GetMovieInfo returning numberOfTracks=%d, duration=0x%X", movieInfo.getValue(0), movieInfo.getValue(8)));
    	}

    	return 0;
    }

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

    	if (log.isTraceEnabled()) {
    		log.trace(String.format("sceMp4RegistTrack track %s", track));
    	}

    	readHeaders(track, trackAddr);

    	return 0;
    }

    @HLEFunction(nid = 0xBCA9389C, version = 150)
    public int sceMp4TrackSampleBufQueryMemSize(int trackType, int numSamples, int sampleSize, int unknown, int readBufferSize) {
        int value = Math.max(numSamples * sampleSize, unknown << 1) + (numSamples << 6) + readBufferSize + 256;
        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceMp4TrackSampleBufQueryMemSize returning 0x%X", value));
        }

        return value;
    }

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

    @HLEFunction(nid = 0x0F0187D2, version = 150)
    public int sceMp4GetAvcTrackInfoData(int mp4, TPointer trackAddr, @CanBeNull TPointer32 infoAddr) {
    	SceMp4TrackSampleBuf track = new SceMp4TrackSampleBuf();
    	track.read(trackAddr);

    	if (log.isTraceEnabled()) {
    		log.trace(String.format("sceMp4GetAvcTrackInfoData track %s", track));
    	}

    	long totalFrameDuration = getTotalFrameDuration(track);

    	// Returning 3 32-bit values in infoAddr
    	infoAddr.setValue(0, 0); // Always 0
    	infoAddr.setValue(4, (int) totalFrameDuration);
    	infoAddr.setValue(8, track.totalNumberSamples);

    	if (log.isTraceEnabled()) {
    		log.trace(String.format("sceMp4GetAvcTrackInfoData returning info:%s", Utilities.getMemoryDump(infoAddr.getAddress(), 12)));
    	}

    	return 0;
    }

    @HLEFunction(nid = 0x9CE6F5CF, version = 150)
    public int sceMp4GetAacTrackInfoData(int mp4, TPointer trackAddr, @CanBeNull TPointer32 infoAddr) {
    	SceMp4TrackSampleBuf track = new SceMp4TrackSampleBuf();
    	track.read(trackAddr);

    	if (log.isTraceEnabled()) {
    		log.trace(String.format("sceMp4GetAacTrackInfoData track %s", track));
    	}

    	long totalFrameDuration = getTotalFrameDuration(track);

    	// Returning 5 32-bit values in infoAddr
    	infoAddr.setValue(0, 0); // Always 0
    	infoAddr.setValue(4, (int) totalFrameDuration);
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

    @HLEFunction(nid = 0x10EE0D2C, version = 150)
    public int sceMp4AacDecodeInit(TPointer32 aac) {
    	aac.setValue(0); // Always 0?

		audioCodec = CodecFactory.getCodec(PSP_CODEC_AAC);

		int channels = 2; // Always stereo?
		audioCodec.init(0, channels, channels, 0);

		return 0;
    }

    @HLEFunction(nid = 0x496E8A65, version = 150)
    public int sceMp4TrackSampleBufFlush(int mp4, TPointer trackAddr) {
    	SceMp4TrackSampleBuf track = new SceMp4TrackSampleBuf();
    	track.read(trackAddr);

    	track.bufBytes.flush();
    	track.bufSamples.flush();

    	track.write(trackAddr);

		if (track.isOfType(TRACK_TYPE_VIDEO)) {
			Modules.sceMpegModule.flushVideoFrameData();
		}

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

    @HLEFunction(nid = 0xF7C51EC1, version = 150)
    public int sceMp4GetSampleInfo(int mp4, TPointer trackAddr, int sample, TPointer infoAddr) {
    	SceMp4TrackSampleBuf track = new SceMp4TrackSampleBuf();
    	track.read(trackAddr);

    	if (log.isTraceEnabled()) {
    		log.trace(String.format("sceMp4GetSampleInfo track %s", track));
    	}

    	if (sample == -1) {
    		sample = track.currentSample;
    	}

    	SceMp4SampleInfo info = new SceMp4SampleInfo();
    	int sampleDuration = getSampleDuration(track.trackType, sample);
    	long frameDuration = sampleToFrameDuration(sampleDuration, track);

    	info.sample = sample;
    	info.sampleOffset = getSampleOffset(track.trackType, sample);
    	info.sampleSize = getSampleSize(track.trackType, sample);
    	info.unknown1 = 0;
    	info.frameDuration = (int) frameDuration;
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
    public int sceMp4SearchSyncSampleNum(int mp4, TPointer trackAddr, int searchDirection, int sample) {
    	if (searchDirection != SEARCH_BACKWARDS && searchDirection != SEARCH_FORWARDS) {
    		return SceKernelErrors.ERROR_MP4_INVALID_VALUE;
    	}

    	SceMp4TrackSampleBuf track = new SceMp4TrackSampleBuf();
    	track.read(trackAddr);

    	if (log.isTraceEnabled()) {
    		log.trace(String.format("sceMp4SearchSyncSampleNum track %s", track));
    	}

    	int[] syncSamples;
    	if (track.isOfType(TRACK_TYPE_AUDIO)) {
    		syncSamples = audioSyncSamples;
    	} else if (track.isOfType(TRACK_TYPE_VIDEO)) {
    		syncSamples = videoSyncSamples;
    	} else {
    		log.error(String.format("sceMp4SearchSyncSampleNum unknown track type 0x%X", track.trackType));
    		return -1;
    	}

    	int syncSample = 0;
    	if (syncSamples != null) {
    		for (int i = 0; i < syncSamples.length; i++) {
    			if (sample > syncSamples[i]) {
    				syncSample = syncSamples[i];
    			} else if (sample == syncSamples[i] && searchDirection == SEARCH_FORWARDS) {
    				syncSample = syncSamples[i];
    			} else {
    				if (searchDirection == SEARCH_FORWARDS) {
    					syncSample = syncSamples[i];
    				}
    				break;
    			}
    		}
    	}

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceMp4SearchSyncSampleNum returning 0x%X", syncSample));
    	}

    	return syncSample;
    }

    @HLEFunction(nid = 0xD8250B75, version = 150)
    public int sceMp4PutSampleNum(int mp4, TPointer trackAddr, int sample) {
    	SceMp4TrackSampleBuf track = new SceMp4TrackSampleBuf();
    	track.read(trackAddr);

    	if (log.isTraceEnabled()) {
    		log.trace(String.format("sceMp4PutSampleNum track %s", track));
    	}

    	if (sample < 0 || sample >= track.totalNumberSamples) {
    		return SceKernelErrors.ERROR_MP4_INVALID_SAMPLE_NUMBER;
    	}

    	track.currentSample = sample;
    	track.write(trackAddr);

		if (track.isOfType(TRACK_TYPE_VIDEO)) {
			Modules.sceMpegModule.setVideoFrame(sample);
		}

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
     * @param samples
     * @return
     */
    @HLEFunction(nid = 0x31BCD7E0, version = 150)
    public int sceMp4TrackSampleBufPut(int mp4, TPointer trackAddr, int samples) {
    	SceMp4TrackSampleBuf track = new SceMp4TrackSampleBuf();
    	track.read(trackAddr);

    	readHeaders(track, trackAddr);

    	if (samples > 0) {
			// Start bufferPut in the thread context when it will be scheduled
        	SceKernelThreadInfo currentThread = Modules.ThreadManForUserModule.getCurrentThread();
			Modules.ThreadManForUserModule.pushActionForThread(currentThread, new StartBufferPut(track, trackAddr, samples, currentThread));
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

    	int sample = track.currentSample - track.bufSamples.sizeAvailableForRead;
    	int sampleSize = getSampleSize(track.trackType, sample);
    	int sampleDuration = getSampleDuration(track.trackType, sample);
    	int samplePresentationOffset = getSamplePresentationOffset(track.trackType, sample);
    	long frameDuration = sampleToFrameDuration(sampleDuration, track);
    	long framePresentationOffset = sampleToFrameDuration(samplePresentationOffset, track);

    	// Consume one frame
    	track.bufSamples.notifyRead(1);
    	track.readBytes(au.esBuffer, sampleSize);
    	au.esSize = sampleSize;
    	au.dts = audioCurrentTimestamp;
    	audioCurrentTimestamp += frameDuration;
    	au.pts = au.dts + framePresentationOffset;

    	if (log.isTraceEnabled()) {
    		log.trace(String.format("sceMp4GetAacAu consuming one frame of size=0x%X, duration=0x%X, track %s", sampleSize, frameDuration, track));
    	}

    	au.write(auAddr);
    	track.write(trackAddr);

    	if (infoAddr.isNotNull()) {
        	SceMp4SampleInfo info = new SceMp4SampleInfo();

        	info.sample = sample;
        	info.sampleOffset = getSampleOffset(track.trackType, sample);
        	info.sampleSize = getSampleSize(track.trackType, sample);
        	info.unknown1 = 0;
        	info.frameDuration = (int) frameDuration;
        	info.unknown2 = 0;
        	info.timestamp1 = (int) au.dts;
        	info.timestamp2 = (int) au.pts;

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

    	int sample = track.currentSample - track.bufSamples.sizeAvailableForRead;
    	int sampleSize = getSampleSize(track.trackType, sample);
    	int sampleDuration = getSampleDuration(track.trackType, sample);
    	int samplePresentationOffset = getSamplePresentationOffset(track.trackType, sample);
    	long frameDuration = sampleToFrameDuration(sampleDuration, track);
    	long framePresentationOffset = sampleToFrameDuration(samplePresentationOffset, track);

    	// Consume one frame
    	track.bufSamples.notifyRead(1);
    	track.bufBytes.notifyRead(sampleSize);

    	au.dts = videoCurrentTimestamp;
    	videoCurrentTimestamp += frameDuration;
    	au.pts = au.dts + framePresentationOffset;

		if (log.isTraceEnabled()) {
    		log.trace(String.format("sceMp4GetAvcAu consuming one frame of size=0x%X, duration=0x%X, track %s", sampleSize, frameDuration, track));
    	}

    	au.write(auAddr);
    	track.write(trackAddr);

    	if (infoAddr.isNotNull()) {
        	SceMp4SampleInfo info = new SceMp4SampleInfo();

        	info.sample = sample;
        	info.sampleOffset = getSampleOffset(track.trackType, sample);
        	info.sampleSize = getSampleSize(track.trackType, sample);
        	info.unknown1 = 0;
        	info.frameDuration = (int) frameDuration;
        	info.unknown2 = 0;
        	info.timestamp1 = (int) au.dts;
        	info.timestamp2 = (int) au.pts;

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