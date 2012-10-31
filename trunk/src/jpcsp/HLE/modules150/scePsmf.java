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

import static jpcsp.util.Utilities.endianSwap16;
import static jpcsp.util.Utilities.endianSwap32;
import static jpcsp.util.Utilities.readUnaligned32;

import jpcsp.HLE.CheckArgument;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import jpcsp.Memory;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

@HLELogging
public class scePsmf extends HLEModule {
    public static Logger log = Modules.getLogger("scePsmf");

    @Override
    public String getName() {
        return "scePsmf";
    }

    @Override
    public void start() {
        psmfHeaderMap = new HashMap<Integer, PSMFHeader>();

        super.start();
    }

    private HashMap<Integer, PSMFHeader> psmfHeaderMap;

    public TPointer32 checkPsmf(TPointer32 psmf) {
		int headerAddress = psmf.getValue(24);
		if (!psmfHeaderMap.containsKey(headerAddress)) {
    		throw new SceKernelErrorException(SceKernelErrors.ERROR_PSMF_NOT_FOUND);
		}

		return psmf;
    }

    public TPointer32 checkPsmfWithEPMap(TPointer32 psmf) {
    	psmf = checkPsmf(psmf);
    	PSMFHeader header = getPsmfHeader(psmf);
    	if (!header.hasEPMap()) {
    		throw new SceKernelErrorException(SceKernelErrors.ERROR_PSMF_NOT_FOUND);
    	}

    	return psmf;
    }

    private PSMFHeader getPsmfHeader(TPointer32 psmf) {
		int headerAddress = psmf.getValue(24);
		return psmfHeaderMap.get(headerAddress);
    }

    // Entry class for the EPMap.
    protected static class PSMFEntry {
        private int EPIndex;
        private int EPPicOffset;
        private int EPPts;
        private int EPOffset;
        private int id;

        public PSMFEntry(int id, int index, int picOffset, int pts, int offset) {
        	this.id = id;
            EPIndex = index;
            EPPicOffset = picOffset;
            EPPts = pts;
            EPOffset = offset;
        }

        public int getEntryIndex() {
            return EPIndex;
        }

        public int getEntryPicOffset() {
            return EPPicOffset;
        }

        public int getEntryPTS() {
            return EPPts;
        }

        public int getEntryOffset() {
            return EPOffset;
        }

        public int getId() {
        	return id;
        }
    }

    protected static class PSMFHeader {
        private static final int size = 2048;
        private static final int PSMF_VIDEO_STREAM_ID = 0xE0;
        private static final int PSMF_AUDIO_STREAM_ID = 0xBD;
        private static final int PSMF_AVC_STREAM = 0;
        private static final int PSMF_ATRAC_STREAM = 1;
        private static final int PSMF_PCM_STREAM = 2;
        private static final int PSMF_DATA_STREAM = 3;
        private static final int PSMF_AUDIO_STREAM = 15;

        // Header vars.
        private int streamOffset;
        private int streamSize;
        private int version;
        private int presentationStartTime;
        private int presentationEndTime;
        private int streamNum;
        private int audioSampleFrequency;
        private int audioChannelConfig;
        private int videoWidth;
        private int videoHeigth;
        private int EPMapEntriesNum;

        // Offsets for the PSMF struct.
        private int headerOffset;
        private int EPMapOffset;

        // EPMap.
        private HashMap<Integer, PSMFEntry> EPMap;
        private int currentEntryNumber;

        // Stream map.
        private List<PSMFStream> streams;
        private int currentStreamNumber = -1;
        private int currentVideoStreamNumber = -1;
        private int currentAudioStreamNumber = -1;

        // Entry class for the PSMF streams.
        protected class PSMFStream {
            private int streamType = -1;
            private int streamChannel = -1;
            private int streamNumber;

            public PSMFStream(int streamNumber) {
            	this.streamNumber = streamNumber;
            }

            public int getStreamType() {
                return streamType;
            }

            public int getStreamChannel() {
                return streamChannel;
            }

			public int getStreamNumber() {
				return streamNumber;
			}

			public boolean isStreamOfType(int type) {
				if (streamType == type) {
					return true;
				}
				if (type == PSMF_AUDIO_STREAM) {
					// Atrac or PCM
					return streamType == PSMF_ATRAC_STREAM || streamType == PSMF_PCM_STREAM;
				}
				if (type == PSMF_DATA_STREAM) {
					// Any type
					return true;
				}

				return false;
			}

			public void readMPEGVideoStreamParams(int addr) {
                Memory mem = Memory.getInstance();
                int streamID = mem.read8(addr);                // 0xE0
                int privateStreamID = mem.read8(addr + 1);     // 0x00
                int unk1 = mem.read8(addr + 2);                // Found values: 0x20/0x21 
                int unk2 = mem.read8(addr + 3);                // Found values: 0x44/0xFB/0x75
                EPMapOffset = endianSwap32(readUnaligned32(mem, addr + 4));
                EPMapEntriesNum = endianSwap32(readUnaligned32(mem, addr + 8));
                videoWidth = (mem.read8(addr + 12) * 0x10);    // PSMF video width (bytes per line).
                videoHeigth = (mem.read8(addr + 13) * 0x10);   // PSMF video heigth (bytes per line).
                
                log.info("Found PSMF MPEG video stream data: streamID=0x" + Integer.toHexString(streamID) 
                        + ", privateStreamID=0x" + Integer.toHexString(privateStreamID)
                        + ", unk1=0x" + Integer.toHexString(unk1) 
                        + ", unk2=0x" + Integer.toHexString(unk2) 
                        + ", EPMapOffset=0x" + Integer.toHexString(EPMapOffset) 
                        + ", EPMapEntriesNum=" + EPMapEntriesNum
                        + ", videoWidth=" + videoWidth 
                        + ", videoHeigth=" + videoHeigth);

                streamType = PSMF_AVC_STREAM;
                streamChannel = streamID & 0x0F;
            }

            public void readPrivateAudioStreamParams(int addr) {
                Memory mem = Memory.getInstance();
                int streamID = mem.read8(addr);                // 0xBD
                int privateStreamID = mem.read8(addr + 1);     // 0x00
                int unk1 = mem.read8(addr + 2);                // Always 0x20
                int unk2 = mem.read8(addr + 3);                // Always 0x04
                audioChannelConfig = mem.read8(addr + 14);     // 1 - mono, 2 - stereo
                audioSampleFrequency = mem.read8(addr + 15);   // 2 - 44khz
                
                log.info("Found PSMF private audio stream data: streamID=0x" + Integer.toHexString(streamID) 
                        + ", privateStreamID=0x" + Integer.toHexString(privateStreamID)
                        + ", unk1=0x" + Integer.toHexString(unk1) 
                        + ", unk2=0x" + Integer.toHexString(unk2) 
                        + ", audioChannelConfig=" + audioChannelConfig
                        + ", audioSampleFrequency=" + audioSampleFrequency);

                streamType = ((privateStreamID & 0xF0) == 0 ? PSMF_ATRAC_STREAM : PSMF_PCM_STREAM);
                streamChannel = privateStreamID & 0x0F;
            }
        }

        public PSMFHeader(int addr) {
            Memory mem = Memory.getInstance();

            // PSMF Header Format
            if (mem.read32(addr) != sceMpeg.PSMF_MAGIC) {
                log.warn("Invalid PSMF detected!");
            }

            headerOffset = addr;

            version = mem.read32(addr + sceMpeg.PSMF_STREAM_VERSION_OFFSET);
            streamOffset = endianSwap32(mem.read32(addr + sceMpeg.PSMF_STREAM_OFFSET_OFFSET));
            streamSize = endianSwap32(mem.read32(addr + sceMpeg.PSMF_STREAM_SIZE_OFFSET));

            int streamDataTotalSize = endianSwap32(readUnaligned32(mem, addr + 0x50));
            presentationStartTime = endianSwap32(readUnaligned32(mem, addr + sceMpeg.PSMF_FIRST_TIMESTAMP_OFFSET));  // First PTS in EPMap (90000).
            presentationEndTime = endianSwap32(readUnaligned32(mem, addr + sceMpeg.PSMF_LAST_TIMESTAMP_OFFSET));     // Last PTS in EPMap.
            int unk = endianSwap32(readUnaligned32(mem, addr + 0x60));
            int streamDataNextBlockSize = endianSwap32(readUnaligned32(mem, addr + 0x6A));                           // General stream information block size.
            int streamDataNextInnerBlockSize = endianSwap32(readUnaligned32(mem, addr + 0x7C));                      // Inner stream information block size.
            streamNum = endianSwap16(mem.read16(addr + 0x80));                                                       // Number of total registered streams.

            if (log.isDebugEnabled()) {
            	log.debug(String.format("PSMFHeader: streamDataTotalSize=%d, unk=0x%08X, streamDataNextBlockSize=%d, streamDataNextInnerBlockSize=%d, streamNum=%d", streamDataTotalSize, unk, streamDataNextBlockSize, streamDataNextInnerBlockSize, streamNum));
            }

            // Stream area:
            // At offset 0x82, each 16 bytes represent one stream.
            streams = new LinkedList<PSMFStream>();

            // Parse the stream field and assign each one to it's type.
            int numberOfStreams = 0;
            for (int i = 0; i < streamNum; i++) {
                PSMFStream stream = null;
                int currentStreamAddr = (addr + 0x82 + i * 16);
                int streamID = mem.read8(currentStreamAddr);
                if ((streamID & 0xF0) == PSMF_VIDEO_STREAM_ID) {
                    stream = new PSMFStream(numberOfStreams);
                    stream.readMPEGVideoStreamParams(currentStreamAddr);
                } else if (streamID == PSMF_AUDIO_STREAM_ID) {
                    stream = new PSMFStream(numberOfStreams);
                    stream.readPrivateAudioStreamParams(currentStreamAddr);
                } else {
                	if (log.isDebugEnabled()) {
                		log.debug(String.format("Unknown stream found in header: 0x%02X", streamID));
                	}
                }
                if (stream != null) {
                    streams.add(stream);
                    numberOfStreams++;
                }
            }

            // EPMap info:
            // - Located at EPMapOffset (set by the AVC stream);
            // - Each entry is composed by a total of 10 bytes:
            //      - 1 byte: Reference picture index (RAPI);
            //      - 1 byte: Reference picture offset from the current index;
            //      - 4 bytes: PTS of the entry point;
            //      - 4 bytes: Relative offset of the entry point in the MPEG data.
            EPMap = new HashMap<Integer, PSMFEntry>();
            for (int i = 0; i < EPMapEntriesNum; i++) {
                int index = mem.read8(addr + EPMapOffset + i * 10);
                int picOffset = mem.read8(addr + EPMapOffset + 1 + i * 10);
                int pts = endianSwap32(readUnaligned32(mem, addr + EPMapOffset + 2 + i * 10));
                int offset = endianSwap32(readUnaligned32(mem, addr + EPMapOffset + 6 + i * 10));
                int id = currentEntryNumber++;
                PSMFEntry pEnt = new PSMFEntry(id, index, picOffset, pts, offset);
                EPMap.put(id, pEnt);
            }
        }

        public int getVersion() {
            return version;
        }

        public int getHeaderSize() {
            return size;
        }

        public int getHeaderOffset() {
            return headerOffset;
        }

        public int getStreamOffset() {
            return streamOffset;
        }

        public int getStreamSize() {
            return streamSize;
        }

        public int getPresentationStartTime() {
            return presentationStartTime;
        }

        public int getPresentationEndTime() {
            return presentationEndTime;
        }

        public int getVideoWidth() {
            return videoWidth;
        }

        public int getvideoHeigth() {
            return videoHeigth;
        }

        public int getAudioSampleFrequency() {
            return audioSampleFrequency;
        }

        public int getAudioChannelConfig() {
            return audioChannelConfig;
        }

        public int getEPMapOffset() {
            return EPMapOffset;
        }

        public int getEPMapEntriesNum() {
            return EPMapEntriesNum;
        }

        public boolean hasEPMap() {
        	return getEPMapEntriesNum() > 0;
        }

        public PSMFEntry getEPMapEntry(int id) {
            return EPMap.get(id);
        }

        public PSMFEntry getEPMapEntryWithTimestamp(int ts) {
        	PSMFEntry foundEntry = null;
            for (PSMFEntry entry : EPMap.values()) {
            	if (foundEntry == null || entry.getEntryPTS() <= ts) {
            		foundEntry = entry;
            	} else if (entry.getEntryPTS() > ts) {
                    break;
                }
            }
            return foundEntry;
        }

        public int getNumberOfStreams() {
            return streamNum;
        }

        public int getCurrentStreamNumber() {
            return currentStreamNumber;
        }

        public int getCurrentStreamType() {
            if (currentStreamNumber >= 0 && currentStreamNumber < streams.size()) {
                return streams.get(currentStreamNumber).getStreamType();
            }
            return -1;
        }

        public int getCurrentStreamChannel() {
            if (currentStreamNumber >= 0 && currentStreamNumber < streams.size()) {
                return streams.get(currentStreamNumber).getStreamChannel();
            }
            return -1;
        }

        public int getSpecificStreamNum(int type) {
        	int num = 0;
        	for (PSMFStream stream : streams) {
        		if (stream.isStreamOfType(type)) {
        			num++;
        		}
        	}

        	return num;
        }

        private void setVideoStreamNum(int id, int channel) {
        	if (currentVideoStreamNumber != id) {
        		Modules.sceMpegModule.setRegisteredVideoChannel(channel);
        		currentVideoStreamNumber = id;
        	}
        }

        private void setAudioStreamNum(int id, int channel) {
        	if (currentAudioStreamNumber != id) {
        		Modules.sceMpegModule.setRegisteredAudioChannel(channel);
        		currentAudioStreamNumber = id;
        	}
        }

        public void setStreamNum(int id) {
            currentStreamNumber = id;

            int type = getCurrentStreamType();
            int channel = getCurrentStreamChannel();
            switch (type) {
            	case PSMF_AVC_STREAM:
            		setVideoStreamNum(id, channel);
            		break;
            	case PSMF_PCM_STREAM:
            	case PSMF_ATRAC_STREAM:
            		setAudioStreamNum(id, channel);
            		break;
            }
        }

        private int getStreamNumber(int type, int typeNum, int channel) {
        	for (PSMFStream stream : streams) {
        		if (stream.isStreamOfType(type)) {
        			if (typeNum <= 0) {
        				if (channel < 0 || stream.getStreamChannel() == channel) {
        					return stream.getStreamNumber();
        				}
        			}
    				typeNum--;
        		}
        	}

        	return -1;
        }

        public boolean setStreamWithType(int type, int channel) {
        	int streamNumber = getStreamNumber(type, 0, channel);
        	if (streamNumber < 0) {
        		return false;
        	}
        	setStreamNum(streamNumber);

    		return true;
        }

        public boolean setStreamWithTypeNum(int type, int typeNum) {
        	int streamNumber = getStreamNumber(type, typeNum, -1);
        	if (streamNumber < 0) {
        		return false;
        	}
        	setStreamNum(streamNumber);

    		return true;
        }
    }

    @HLELogging(level="info")
    @HLEFunction(nid = 0xC22C8327, version = 150, checkInsideInterrupt = true)
    public int scePsmfSetPsmf(TPointer32 psmf, TPointer bufferAddr) {
        Modules.sceMpegModule.setCurrentMpegAnalyzed(false);
        PSMFHeader header = new PSMFHeader(bufferAddr.getAddress());
        psmfHeaderMap.put(header.getHeaderOffset(), header);

        // PSMF struct:
        // This is an internal system data area which is used to store
        // several parameters of the file being handled.
        // It's size ranges from 28 bytes to 52 bytes, since when a pointer to
        // a certain PSMF area does not exist (NULL), it's omitted from the struct
        // (e.g.: no mark data or non existant EPMap).
        psmf.setValue(0, header.getVersion());              // PSMF version.
        psmf.setValue(4, header.getHeaderSize());           // The PSMF header size (0x800).
        psmf.setValue(8, header.getStreamSize());           // The PSMF stream size.
        psmf.setValue(12, 0);                               // Grouping Period ID.
        psmf.setValue(16, 0);                               // Group ID.
        psmf.setValue(20, header.getCurrentStreamNumber()); // Current stream's number.
        psmf.setValue(24, header.getHeaderOffset());        // Pointer to PSMF header.
        // psmf + 28 - Pointer to current PSMF stream info (video/audio).
        // psmf + 32 - Pointer to mark data (used for chapters in UMD_VIDEO).
        // psmf + 36 - Pointer to current PSMF stream grouping period.
        // psmf + 40 - Pointer to current PSMF stream group.
        // psmf + 44 - Pointer to current PSMF stream.
        // psmf + 48 - Pointer to PSMF EPMap.

        return 0;
    }

    @HLEFunction(nid = 0xC7DB3A5B, version = 150, checkInsideInterrupt = true)
    public int scePsmfGetCurrentStreamType(@CheckArgument("checkPsmf") TPointer32 psmf, TPointer32 typeAddr, TPointer32 channelAddr) {
        PSMFHeader header = getPsmfHeader(psmf);
        typeAddr.setValue(header.getCurrentStreamType());
        channelAddr.setValue(header.getCurrentStreamChannel());

        if (log.isDebugEnabled()) {
            log.debug(String.format("scePsmfGetCurrentStreamType returning type=%d, channel=%d", typeAddr.getValue(), channelAddr.getValue()));
        }

        return 0;
    }

    @HLEFunction(nid = 0x28240568, version = 150, checkInsideInterrupt = true)
    public int scePsmfGetCurrentStreamNumber(@CheckArgument("checkPsmf") TPointer32 psmf) {
        PSMFHeader header = getPsmfHeader(psmf);

        return header.getCurrentStreamNumber();
    }

    @HLEFunction(nid = 0x1E6D9013, version = 150, checkInsideInterrupt = true)
    public int scePsmfSpecifyStreamWithStreamType(@CheckArgument("checkPsmf") TPointer32 psmf, int type, int ch) {
        PSMFHeader header = getPsmfHeader(psmf);
        if (!header.setStreamWithType(type, ch)) {
        	return SceKernelErrors.ERROR_PSMF_INVALID_ID;
        }

        return 0;
    }

    @HLEFunction(nid = 0x4BC9BDE0, version = 150, checkInsideInterrupt = true)
    public int scePsmfSpecifyStream(@CheckArgument("checkPsmf") TPointer32 psmf, int streamNum) {
        PSMFHeader header = getPsmfHeader(psmf);
        header.setStreamNum(streamNum);

        return 0;
    }

    @HLEFunction(nid = 0x76D3AEBA, version = 150, checkInsideInterrupt = true)
    public int scePsmfGetPresentationStartTime(@CheckArgument("checkPsmf") TPointer32 psmf, TPointer32 startTimeAddr) {
        PSMFHeader header = getPsmfHeader(psmf);
        int startTime = header.getPresentationStartTime();
        startTimeAddr.setValue(startTime);
        if (log.isDebugEnabled()) {
            log.debug(String.format("scePsmfGetPresentationStartTime startTime=%d", startTime));
        }

        return 0;
    }

    @HLEFunction(nid = 0xBD8AE0D8, version = 150, checkInsideInterrupt = true)
    public int scePsmfGetPresentationEndTime(@CheckArgument("checkPsmf") TPointer32 psmf, TPointer32 endTimeAddr) {
        PSMFHeader header = getPsmfHeader(psmf);
        int endTime = header.getPresentationEndTime();
        endTimeAddr.setValue(endTime);
        if (log.isDebugEnabled()) {
            log.debug(String.format("scePsmfGetPresentationEndTime endTime=%d", endTime));
        }

        return 0;
    }

    @HLEFunction(nid = 0xEAED89CD, version = 150, checkInsideInterrupt = true)
    public int scePsmfGetNumberOfStreams(@CheckArgument("checkPsmf") TPointer32 psmf) {
        PSMFHeader header = getPsmfHeader(psmf);

        return header.getNumberOfStreams();
    }

    @HLEFunction(nid = 0x7491C438, version = 150, checkInsideInterrupt = true)
    public int scePsmfGetNumberOfEPentries(@CheckArgument("checkPsmf") TPointer32 psmf) {
        PSMFHeader header = getPsmfHeader(psmf);

        return header.getEPMapEntriesNum();
    }

    @HLEFunction(nid = 0x0BA514E5, version = 150, checkInsideInterrupt = true)
    public int scePsmfGetVideoInfo(@CheckArgument("checkPsmf") TPointer32 psmf, TPointer32 videoInfoAddr) {
        PSMFHeader header = getPsmfHeader(psmf);
        videoInfoAddr.setValue(0, header.getVideoWidth());
        videoInfoAddr.setValue(4, header.getvideoHeigth());

        return 0;
    }

    @HLEFunction(nid = 0xA83F7113, version = 150, checkInsideInterrupt = true)
    public int scePsmfGetAudioInfo(@CheckArgument("checkPsmf") TPointer32 psmf, TPointer32 audioInfoAddr) {
        PSMFHeader header = getPsmfHeader(psmf);
        audioInfoAddr.setValue(0, header.getAudioChannelConfig());
        audioInfoAddr.setValue(4, header.getAudioSampleFrequency());

        return 0;
    }

    @HLEFunction(nid = 0x971A3A90, version = 150, checkInsideInterrupt = true)
    public int scePsmfCheckEPmap(@CheckArgument("checkPsmf") TPointer32 psmf) {
        PSMFHeader header = getPsmfHeader(psmf);

        return header.hasEPMap() ? 0 : SceKernelErrors.ERROR_PSMF_NOT_FOUND;
    }

    @HLEFunction(nid = 0x4E624A34, version = 150, checkInsideInterrupt = true)
    public int scePsmfGetEPWithId(@CheckArgument("checkPsmfWithEPMap") TPointer32 psmf, int id, TPointer32 outAddr) {
        PSMFHeader header = getPsmfHeader(psmf);
        PSMFEntry entry = header.getEPMapEntry(id);
    	if (entry == null) {
    		return SceKernelErrors.ERROR_PSMF_INVALID_ID;
    	}

        outAddr.setValue(0, entry.getEntryPTS());
        outAddr.setValue(4, entry.getEntryOffset());
        outAddr.setValue(8, entry.getEntryIndex());
        outAddr.setValue(12, entry.getEntryPicOffset());

        return 0;
    }

    @HLEFunction(nid = 0x7C0E7AC3, version = 150, checkInsideInterrupt = true)
    public int scePsmfGetEPWithTimestamp(@CheckArgument("checkPsmfWithEPMap") TPointer32 psmf, int ts, TPointer32 entryAddr) {
        PSMFHeader header = getPsmfHeader(psmf);
    	if (ts < header.getPresentationStartTime()) {
            return SceKernelErrors.ERROR_PSMF_INVALID_TIMESTAMP;
    	}

    	PSMFEntry entry = header.getEPMapEntryWithTimestamp(ts);
        if (entry == null) {
        	// Unknown error code
        	return -1;
        }

        entryAddr.setValue(0, entry.getEntryPTS());
        entryAddr.setValue(4, entry.getEntryOffset());
        entryAddr.setValue(8, entry.getEntryIndex());
        entryAddr.setValue(12, entry.getEntryPicOffset());

        return 0;
    }

    @HLEFunction(nid = 0x5F457515, version = 150, checkInsideInterrupt = true)
    public int scePsmfGetEPidWithTimestamp(@CheckArgument("checkPsmfWithEPMap") TPointer32 psmf, int ts) {
        PSMFHeader header = getPsmfHeader(psmf);
    	if (ts < header.getPresentationStartTime()) {
            return SceKernelErrors.ERROR_PSMF_INVALID_TIMESTAMP;
    	}

    	PSMFEntry entry = header.getEPMapEntryWithTimestamp(ts);
        if (entry == null) {
        	// Unknown error code
            return -1;
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("scePsmfGetEPidWithTimestamp returning id 0x%X", entry.getId()));
        }

        return entry.getId();
	}

    @HLEFunction(nid = 0x5B70FCC1, version = 150, checkInsideInterrupt = true)
    public int scePsmfQueryStreamOffset(TPointer bufferAddr, TPointer32 offsetAddr) {
        int offset = endianSwap32(bufferAddr.getValue32(sceMpeg.PSMF_STREAM_OFFSET_OFFSET));
        offsetAddr.setValue(offset);

        // Always let sceMpeg handle the PSMF analysis.
        Modules.sceMpegModule.analyseMpeg(bufferAddr.getAddress());

        return 0;
    }

    @HLEFunction(nid = 0x9553CC91, version = 150, checkInsideInterrupt = true)
    public int scePsmfQueryStreamSize(TPointer bufferAddr, TPointer32 sizeAddr) {
        int size = endianSwap32(bufferAddr.getValue32(sceMpeg.PSMF_STREAM_SIZE_OFFSET));
        sizeAddr.setValue(size);

        // Always let sceMpeg handle the PSMF analysis.
        Modules.sceMpegModule.analyseMpeg(bufferAddr.getAddress());

        return 0;
    }

    @HLEFunction(nid = 0x68D42328, version = 150, checkInsideInterrupt = true)
    public int scePsmfGetNumberOfSpecificStreams(@CheckArgument("checkPsmf") TPointer32 psmf, int streamType) {
        PSMFHeader header = getPsmfHeader(psmf);
        int streamNum = header.getSpecificStreamNum(streamType);

        if (log.isDebugEnabled()) {
            log.debug(String.format("scePsmfGetNumberOfSpecificStreams returning %d", streamNum));
        }

        return streamNum;
    }

    @HLEFunction(nid = 0x0C120E1D, version = 150, checkInsideInterrupt = true)
    public int scePsmfSpecifyStreamWithStreamTypeNumber(@CheckArgument("checkPsmf") TPointer32 psmf, int type, int typeNum) {
        PSMFHeader header = getPsmfHeader(psmf);
        if (!header.setStreamWithTypeNum(type, typeNum)) {
        	return SceKernelErrors.ERROR_PSMF_INVALID_ID;
        }

        return 0;
    }

    @HLEFunction(nid = 0x2673646B, version = 150, checkInsideInterrupt = true)
    public int scePsmfVerifyPsmf(TPointer bufferAddr) {
        if (log.isTraceEnabled()) {
            log.trace(String.format("scePsmfVerifyPsmf %s", Utilities.getMemoryDump(bufferAddr.getAddress(), sceMpeg.MPEG_HEADER_BUFFER_MINIMUM_SIZE)));
        }

        int magic = bufferAddr.getValue32(sceMpeg.PSMF_MAGIC_OFFSET);
        if (magic != sceMpeg.PSMF_MAGIC) {
        	return SceKernelErrors.ERROR_PSMF_INVALID_PSMF;
        }

        int rawVersion = bufferAddr.getValue32(sceMpeg.PSMF_STREAM_VERSION_OFFSET);
        int version = sceMpeg.getMpegVersion(rawVersion);
        if (version < 0) {
        	return SceKernelErrors.ERROR_PSMF_INVALID_PSMF;
        }

        return 0;
    }

    @HLEFunction(nid = 0xB78EB9E9, version = 150, checkInsideInterrupt = true)
    public int scePsmfGetHeaderSize(@CheckArgument("checkPsmf") TPointer32 psmf, TPointer32 sizeAddr) {
        PSMFHeader header = getPsmfHeader(psmf);
        sizeAddr.setValue(header.getHeaderSize());

        return 0;
    }

    @HLEFunction(nid = 0xA5EBFE81, version = 150, checkInsideInterrupt = true)
    public int scePsmfGetStreamSize(@CheckArgument("checkPsmf") TPointer32 psmf, TPointer32 sizeAddr) {
        PSMFHeader header = getPsmfHeader(psmf);
        sizeAddr.setValue(header.getStreamSize());

        return 0;
    }

    @HLEFunction(nid = 0xE1283895, version = 150, checkInsideInterrupt = true)
    public int scePsmfGetPsmfVersion(@CheckArgument("checkPsmf") TPointer32 psmf) {
        PSMFHeader header = getPsmfHeader(psmf);

        return header.getVersion();
    }
}