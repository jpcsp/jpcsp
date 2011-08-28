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

import static jpcsp.util.Utilities.endianSwap32;
import static jpcsp.util.Utilities.readUnaligned32;

import jpcsp.HLE.HLEFunction;
import java.util.HashMap;

import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEStartModule;
import jpcsp.HLE.modules150.scePsmf.PSMFHeader.PSMFEntry;

import org.apache.log4j.Logger;

public class scePsmf extends HLEModule implements HLEStartModule {

    private static Logger log = Modules.getLogger("scePsmf");

    @Override
    public String getName() {
        return "scePsmf";
    }

    @Override
    public void start() {
        psmfHeaderMap = new HashMap<Integer, PSMFHeader>();
    }

    @Override
    public void stop() {
    }
    private HashMap<Integer, PSMFHeader> psmfHeaderMap;

    private PSMFHeader getPsmfHeader(int psmf) {
    	if (Memory.isAddressGood(psmf)) {
    		Memory mem = Memory.getInstance();
    		int headerAddress = mem.read32(psmf + 24);
    		return psmfHeaderMap.get(headerAddress);
    	}

    	return null;
    }

    protected int endianSwap16(int x) {
        return (x >> 8) | ((x << 8) & 0xFF00);
    }

    protected class PSMFHeader {

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
        private HashMap<Integer, PSMFStream> streamMap;
        private int currentStreamNumber;
        private int currentVideoStreamNumber;
        private int videoStreamNum;
        private int currentAudioStreamNumber;
        private int audioStreamNum;

        // Entry class for the EPMap.
        protected class PSMFEntry {

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

        // Entry class for the PSMF streams.
        protected class PSMFStream {

            private int StreamType;
            private int StreamChannel;

            public PSMFStream(int type, int channel) {
                StreamType = type;
                StreamChannel = channel;
            }

            public int getStreamType() {
                return StreamType;
            }

            public int getStreamChannel() {
                return StreamChannel;
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
                                                         
            // Stream area:
            // At offset 0x82, each 16 bytes represent one stream.
            streamMap = new HashMap<Integer, PSMFStream>();
            currentStreamNumber = -1;         // Current stream number.
            currentVideoStreamNumber = -1;    // Current video stream number.
            currentAudioStreamNumber = -1;    // Current audio stream number.
            videoStreamNum = 0;
            audioStreamNum = 0;

            // Parse the stream field and assign each one to it's type.
            for (int i = 0; i < streamNum; i++) {
                PSMFStream stream = null;
                int currentStreamAddr = (addr + 0x82 + i * 16);
                int streamID = mem.read8(currentStreamAddr);
                if ((streamID & PSMF_VIDEO_STREAM_ID) == PSMF_VIDEO_STREAM_ID) {
                    stream = new PSMFStream(PSMF_AVC_STREAM, 0);
                    stream.readMPEGVideoStreamParams(currentStreamAddr);
                    currentVideoStreamNumber++;
                    videoStreamNum++;
                } else if ((streamID & PSMF_AUDIO_STREAM_ID) == PSMF_AUDIO_STREAM_ID) {
                    stream = new PSMFStream(PSMF_ATRAC_STREAM, 1);
                    stream.readPrivateAudioStreamParams(currentStreamAddr);
                    currentAudioStreamNumber++;
                    audioStreamNum++;
                }
                if (stream != null) {
                    currentStreamNumber++;
                    streamMap.put(currentStreamNumber, stream);
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
                int index = mem.read8(EPMapOffset + i * 10);
                int picOffset = mem.read8(EPMapOffset + 1 + i * 10);
                int pts = endianSwap32(readUnaligned32(mem, EPMapOffset + 2 + i * 10));
                int offset = endianSwap32(readUnaligned32(mem, EPMapOffset + 6 + i * 10));
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
            if (streamMap.get(currentStreamNumber) != null) {
                return streamMap.get(currentStreamNumber).getStreamType();
            }
            return -1;
        }

        public int getCurrentStreamChannel() {
            if (streamMap.get(currentStreamNumber) != null) {
                return streamMap.get(currentStreamNumber).getStreamChannel();
            }
            return -1;
        }

        public int getSpecificStreamNum(int type) {
        	switch (type) {
        		case PSMF_AVC_STREAM:
        			if (currentVideoStreamNumber != -1) {
        				return 1;
        			}
        			break;
        		case PSMF_ATRAC_STREAM:
        		case PSMF_PCM_STREAM:
        		case PSMF_AUDIO_STREAM:
        			if (currentAudioStreamNumber != -1) {
        				return 1;
        			}
        			break;
                case PSMF_DATA_STREAM:
                    if (currentVideoStreamNumber != -1) {
        				return 1;
        			} else if (currentAudioStreamNumber != -1) {
        				return 1;
        			}
                    break;
    			default:
    				log.warn(String.format("scePsmfGetNumberOfSpecificStreams unknown stream type %d", type));
        	}

        	return 0;
        }

        public void setStreamNum(int id) {
            currentStreamNumber = id;
        }

        public void setStreamType(int type, int channel) {
        	switch (type) {
	    		case PSMF_AVC_STREAM:
	    			if (currentVideoStreamNumber != -1) {
	                    currentStreamNumber = currentVideoStreamNumber;
	    			}
	    			break;
	    		case PSMF_ATRAC_STREAM:
	    		case PSMF_PCM_STREAM:
	    		case PSMF_AUDIO_STREAM:
	    			if (currentAudioStreamNumber != -1) {
	                    currentStreamNumber = currentAudioStreamNumber;
	    			}
	    			break;
                case PSMF_DATA_STREAM:
                    if (currentVideoStreamNumber != -1) {
	                    currentStreamNumber = currentVideoStreamNumber;
	    			} else if (currentAudioStreamNumber != -1) {
	                    currentStreamNumber = currentAudioStreamNumber;
	    			}
                    break;
				default:
					log.warn(String.format("scePsmfSpecifyStreamWithStreamType unknown stream type %d", type));
        	}
        }

        public void setStreamTypeNum(int type, int tid) {
            currentStreamNumber = tid;
        }
    }

    @HLEFunction(nid = 0xC22C8327, version = 150)
    public void scePsmfSetPsmf(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int psmf = cpu.gpr[4];         // PSMF struct.
        int buffer_addr = cpu.gpr[5];  // Actual PMF data.

        if (log.isInfoEnabled()) {
            log.info("scePsmfSetPsmf (psmf=0x" + Integer.toHexString(psmf) + " buffer_addr=0x" + Integer.toHexString(buffer_addr) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        Modules.sceMpegModule.setCurrentMpegAnalyzed(false);
        PSMFHeader header = new PSMFHeader(buffer_addr);
        psmfHeaderMap.put(header.getHeaderOffset(), header);

        // PSMF struct:
        // This is an internal system data area which is used to store
        // several parameters of the file being handled.
        // It's size ranges from 28 bytes to 52 bytes, since when a pointer to
        // a certain PSMF area does not exist (NULL), it's omitted from the struct
        // (e.g.: no mark data or non existant EPMap).
        mem.write32(psmf, header.getVersion());                  // PSMF version.
        mem.write32(psmf + 4, header.getHeaderSize());           // The PSMF header size (0x800).
        mem.write32(psmf + 8, header.getStreamSize());           // The PSMF stream size.
        mem.write32(psmf + 12, 0);                               // Grouping Period ID.
        mem.write32(psmf + 16, 0);                               // Group ID.
        mem.write32(psmf + 20, header.getCurrentStreamNumber()); // Current stream's number.
        mem.write32(psmf + 24, header.getHeaderOffset());        // Pointer to PSMF header.
        // psmf + 28 - Pointer to current PSMF stream info (video/audio).
        // psmf + 32 - Pointer to mark data (used for chapters in UMD_VIDEO).
        // psmf + 36 - Pointer to current PSMF stream grouping period.
        // psmf + 40 - Pointer to current PSMF stream group.
        // psmf + 44 - Pointer to current PSMF stream.
        // psmf + 48 - Pointer to PSMF EPMap.

        cpu.gpr[2] = 0;
    }

    @HLEFunction(nid = 0xC7DB3A5B, version = 150)
    public void scePsmfGetCurrentStreamType(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int psmf = cpu.gpr[4];
        int type_addr = cpu.gpr[5];
        int ch_addr = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug("scePsmfGetCurrentStreamType (psmf=0x" + Integer.toHexString(psmf) + ", type_addr=0x" + Integer.toHexString(type_addr) + ", ch_addr=0x" + Integer.toHexString(ch_addr) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        PSMFHeader header = getPsmfHeader(psmf);
        if (header != null) {
            int streamType = header.getCurrentStreamType();
            int streamCh = header.getCurrentStreamChannel();
            mem.write32(type_addr, streamType);
            mem.write32(ch_addr, streamCh);
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_NOT_FOUND;
        }
    }

    @HLEFunction(nid = 0x28240568, version = 150)
    public void scePsmfGetCurrentStreamNumber(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmf = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug("scePsmfGetCurrentStreamNumber psmf=0x" + Integer.toHexString(psmf));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        PSMFHeader header = getPsmfHeader(psmf);
        if (header != null) {
            cpu.gpr[2] = header.getCurrentStreamNumber();
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_NOT_FOUND;
        }
    }

    @HLEFunction(nid = 0x1E6D9013, version = 150)
    public void scePsmfSpecifyStreamWithStreamType(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmf = cpu.gpr[4];
        int type = cpu.gpr[5];
        int ch = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug("scePsmfSpecifyStreamWithStreamType (psmf=0x" + Integer.toHexString(psmf) + ", type=" + type + ", ch=" + ch + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        PSMFHeader header = getPsmfHeader(psmf);
        if (header != null) {
            header.setStreamType(type, ch);
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_NOT_FOUND;
        }
    }

    @HLEFunction(nid = 0x4BC9BDE0, version = 150)
    public void scePsmfSpecifyStream(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmf = cpu.gpr[4];
        int streamNum = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("scePsmfSpecifyStream (psmf=0x" + Integer.toHexString(psmf) + ", streamNum=" + streamNum + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        PSMFHeader header = getPsmfHeader(psmf);
        if (header != null) {
            header.setStreamNum(streamNum);
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_NOT_FOUND;
        }
    }

    @HLEFunction(nid = 0x76D3AEBA, version = 150)
    public void scePsmfGetPresentationStartTime(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int psmf = cpu.gpr[4];
        int startTimeAddr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("scePsmfGetPresentationStartTime (psmf=0x" + Integer.toHexString(psmf) + ", startTimeAddr=0x" + Integer.toHexString(startTimeAddr) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        PSMFHeader header = getPsmfHeader(psmf);
        if (header != null) {
            int startTime = header.getPresentationStartTime();
            mem.write32(startTimeAddr, startTime);
            if (log.isDebugEnabled()) {
                log.debug(String.format("scePsmfGetPresentationStartTime startTime=%d", startTime));
            }
            cpu.gpr[2] = 0;
        } else {
            mem.write32(startTimeAddr, 0);
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_NOT_FOUND;
        }
    }

    @HLEFunction(nid = 0xBD8AE0D8, version = 150)
    public void scePsmfGetPresentationEndTime(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int psmf = cpu.gpr[4];
        int endTimeAddr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("scePsmfGetPresentationEndTime (psmf=0x" + Integer.toHexString(psmf) + ", endTimeAddr=0x" + Integer.toHexString(endTimeAddr) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        PSMFHeader header = getPsmfHeader(psmf);
        if (header != null) {
            int endTime = header.getPresentationEndTime();
            mem.write32(endTimeAddr, endTime);
            if (log.isDebugEnabled()) {
                log.debug(String.format("scePsmfGetPresentationEndTime endTime=%d", endTime));
            }
            cpu.gpr[2] = 0;
        } else {
            mem.write32(endTimeAddr, 0);
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_NOT_FOUND;
        }
    }

    @HLEFunction(nid = 0xEAED89CD, version = 150)
    public void scePsmfGetNumberOfStreams(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmf = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug("scePsmfGetNumberOfStreams psmf=0x" + Integer.toHexString(psmf));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        PSMFHeader header = getPsmfHeader(psmf);
        if (header != null) {
            cpu.gpr[2] = header.getNumberOfStreams();
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_NOT_FOUND;
        }
    }

    @HLEFunction(nid = 0x7491C438, version = 150)
    public void scePsmfGetNumberOfEPentries(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmf = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug("scePsmfGetNumberOfEPentries (psmf=0x" + Integer.toHexString(psmf) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        PSMFHeader header = getPsmfHeader(psmf);
        if (header != null) {
            cpu.gpr[2] = header.getEPMapEntriesNum();
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_NOT_FOUND;
        }
    }

    @HLEFunction(nid = 0x0BA514E5, version = 150)
    public void scePsmfGetVideoInfo(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int psmf = cpu.gpr[4];
        int videoInfoAddr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("scePsmfGetVideoInfo (psmf=0x" + Integer.toHexString(psmf) + ", videoInfoAddr=0x" + Integer.toHexString(videoInfoAddr) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        PSMFHeader header = getPsmfHeader(psmf);
        if (header != null) {
            int width = header.getVideoWidth();
            int height = header.getvideoHeigth();
            mem.write32(videoInfoAddr, width);
            mem.write32(videoInfoAddr + 4, height);
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_NOT_FOUND;
        }
    }

    @HLEFunction(nid = 0xA83F7113, version = 150)
    public void scePsmfGetAudioInfo(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int psmf = cpu.gpr[4];
        int audioInfoAddr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("scePsmfGetAudioInfo (psmf=0x" + Integer.toHexString(psmf) + ", audioInfoAddr=0x" + Integer.toHexString(audioInfoAddr) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        PSMFHeader header = getPsmfHeader(psmf);
        if (header != null) {
            int chConfig = header.getAudioChannelConfig();
            int sampleFreq = header.getAudioSampleFrequency();
            mem.write32(audioInfoAddr, chConfig);
            mem.write32(audioInfoAddr + 4, sampleFreq);
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_NOT_FOUND;
        }
    }

    @HLEFunction(nid = 0x971A3A90, version = 150)
    public void scePsmfCheckEPmap(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmf = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug("scePsmfCheckEPmap (psmf=0x" + Integer.toHexString(psmf) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        PSMFHeader header = getPsmfHeader(psmf);
        if (header != null && header.hasEPMap()) {
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_NOT_FOUND;
        }
    }

    @HLEFunction(nid = 0x4E624A34, version = 150)
    public void scePsmfGetEPWithId(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int psmf = cpu.gpr[4];
        int id = cpu.gpr[5];
        int out_addr = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug("scePsmfGetEPWithId (psmf=0x" + Integer.toHexString(psmf) + ", id=0x" + Integer.toHexString(id) + ", out_addr=0x" + Integer.toHexString(out_addr) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        PSMFHeader header = getPsmfHeader(psmf);
        if (header != null && header.hasEPMap()) {
        	PSMFEntry entry = header.getEPMapEntry(id);
        	if (entry != null) {
	            int pts = entry.getEntryPTS();
	            int offset = entry.getEntryOffset();
                int index = entry.getEntryIndex();
                int picOffset = entry.getEntryPicOffset();
	            mem.write32(out_addr, pts);
	            mem.write32(out_addr + 4, offset);
                mem.write32(out_addr + 8, index);
                mem.write32(out_addr + 12, picOffset);
	            cpu.gpr[2] = 0;
        	} else {
        		cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_INVALID_ID;
        	}
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_NOT_FOUND;
        }
    }

    @HLEFunction(nid = 0x7C0E7AC3, version = 150)
    public void scePsmfGetEPWithTimestamp(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int psmf = cpu.gpr[4];
        int ts = cpu.gpr[5];
        int entry_addr = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug("scePsmfGetEPWithTimestamp (psmf=0x" + Integer.toHexString(psmf) + ", ts=" + ts + ", entry_addr=0x" + Integer.toHexString(entry_addr) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        PSMFHeader header = getPsmfHeader(psmf);
        if (header != null && header.hasEPMap()) {
        	if (ts < header.getPresentationStartTime()) {
                cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_INVALID_TIMESTAMP;
        	} else {
	            PSMFEntry entry = header.getEPMapEntryWithTimestamp(ts);
	            if (entry == null) {
	            	cpu.gpr[2] = -1;
	            } else {
	                int pts = entry.getEntryPTS();
	                int offset = entry.getEntryOffset();
                    int index = entry.getEntryIndex();
                    int picOffset = entry.getEntryPicOffset();
	                mem.write32(entry_addr, pts);
	                mem.write32(entry_addr + 4, offset);
                    mem.write32(entry_addr + 8, index);
                    mem.write32(entry_addr + 12, picOffset);
	                cpu.gpr[2] = 0;
	            }
        	}
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_NOT_FOUND;
        }
    }

    @HLEFunction(nid = 0x5F457515, version = 150)
    public void scePsmfGetEPidWithTimestamp(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmf = cpu.gpr[4];
        int ts = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("scePsmfGetEPidWithTimestamp (psmf=0x" + Integer.toHexString(psmf) + ", ts=" + ts + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        PSMFHeader header = getPsmfHeader(psmf);
        if (header != null && header.hasEPMap()) {
        	if (ts < header.getPresentationStartTime()) {
                cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_INVALID_TIMESTAMP;
        	} else {
	            PSMFEntry entry = header.getEPMapEntryWithTimestamp(ts);
	            if (entry == null) {
	                cpu.gpr[2] = -1;
	            } else {
	            	cpu.gpr[2] = entry.getId();
	            }
        	}
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_NOT_FOUND;
        }
    }

    @HLEFunction(nid = 0x5B70FCC1, version = 150)
    public void scePsmfQueryStreamOffset(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int buffer_addr = cpu.gpr[4];
        int offset_addr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("scePsmfQueryStreamOffset (buffer_addr=0x" + Integer.toHexString(buffer_addr) + ", offset_addr=0x" + Integer.toHexString(offset_addr) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        int offset = endianSwap32(mem.read32(buffer_addr + sceMpeg.PSMF_STREAM_OFFSET_OFFSET));
        mem.write32(offset_addr, offset);

        // Always let sceMpeg handle the PSMF analysis.
        Modules.sceMpegModule.analyseMpeg(buffer_addr);

        cpu.gpr[2] = 0;
    }

    @HLEFunction(nid = 0x9553CC91, version = 150)
    public void scePsmfQueryStreamSize(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int buffer_addr = cpu.gpr[4];
        int size_addr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("scePsmfQueryStreamSize (buffer_addr=0x" + Integer.toHexString(buffer_addr) + ", size_addr=0x" + Integer.toHexString(size_addr) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        int size = endianSwap32(mem.read32(buffer_addr + sceMpeg.PSMF_STREAM_SIZE_OFFSET));
        if ((size & 0x7FF) != 0) {
            mem.write32(size_addr, 0);
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_INVALID_VALUE;
        } else {
            mem.write32(size_addr, size);
            cpu.gpr[2] = 0;
        }
        // Always let sceMpeg handle the PSMF analysis.
        Modules.sceMpegModule.analyseMpeg(buffer_addr);
    }

    @HLEFunction(nid = 0x68D42328, version = 150)
    public void scePsmfGetNumberOfSpecificStreams(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmf = cpu.gpr[4];
        int stream_type = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("scePsmfGetNumberOfSpecificStreams (psmf=0x" + Integer.toHexString(psmf) + ", stream_type=" + stream_type + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        PSMFHeader header = getPsmfHeader(psmf);
        if (header != null) {
            cpu.gpr[2] = header.getSpecificStreamNum(stream_type);
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_NOT_FOUND;
        }
        if (log.isDebugEnabled()) {
            log.debug(String.format("scePsmfGetNumberOfSpecificStreams (psmf=0x%08X, stream_type=%d) returning 0x%08X", psmf, stream_type, cpu.gpr[2]));
        }
    }

    @HLEFunction(nid = 0x0C120E1D, version = 150)
    public void scePsmfSpecifyStreamWithStreamTypeNumber(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmf = cpu.gpr[4];
        int type = cpu.gpr[5];
        int type_num = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug("scePsmfSpecifyStreamWithStreamTypeNumber (psmf=0x" + Integer.toHexString(psmf) + ", type=" + type + ", type_num=" + type_num + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        PSMFHeader header = getPsmfHeader(psmf);
        if (header != null) {
            header.setStreamTypeNum(type, type_num);
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_NOT_FOUND;
        }
    }

    @HLEFunction(nid = 0x2673646B, version = 150)
    public void scePsmfVerifyPsmf(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int buffer_addr = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug("scePsmfVerifyPsmf (buffer_addr=0x" + Integer.toHexString(buffer_addr) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (Memory.isAddressGood(buffer_addr)) {
            int version = mem.read32(buffer_addr + sceMpeg.PSMF_STREAM_VERSION_OFFSET);
            if (version > sceMpeg.PSMF_VERSION_0015) {
                cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_INVALID_PSMF;
            } else {
                cpu.gpr[2] = 0;
            }
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_INVALID_PSMF;
        }
    }

    @HLEFunction(nid = 0xB78EB9E9, version = 150)
    public void scePsmfGetHeaderSize(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int psmf = cpu.gpr[4];
        int size_addr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("scePsmfGetHeaderSize (psmf=0x" + Integer.toHexString(psmf) + ", size_addr=0x" + size_addr + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        PSMFHeader header = getPsmfHeader(psmf);
        if (header != null) {
            int size = header.getHeaderSize();
            mem.write32(size_addr, size);
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_NOT_FOUND;
        }
    }

    @HLEFunction(nid = 0xA5EBFE81, version = 150)
    public void scePsmfGetStreamSize(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int psmf = cpu.gpr[4];
        int size_addr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("scePsmfGetStreamSize (psmf=0x" + Integer.toHexString(psmf) + ", size_addr=0x" + size_addr + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        PSMFHeader header = getPsmfHeader(psmf);
        if (header != null) {
            int size = header.getStreamSize();
            mem.write32(size_addr, size);
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_NOT_FOUND;
        }
    }

    @HLEFunction(nid = 0xE1283895, version = 150)
    public void scePsmfGetPsmfVersion(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmf = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug("scePsmfGetPsmfVersion (psmf=0x" + Integer.toHexString(psmf) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        PSMFHeader header = getPsmfHeader(psmf);
        if (header != null) {
            cpu.gpr[2] = header.getVersion();
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_NOT_FOUND;
        }
    }

}