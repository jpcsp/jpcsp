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

import java.util.HashMap;

import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.HLEStartModule;

import org.apache.log4j.Logger;

public class scePsmf implements HLEModule, HLEStartModule {

    private static Logger log = Modules.getLogger("scePsmf");

    @Override
    public String getName() {
        return "scePsmf";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.addFunction(0xC22C8327, scePsmfSetPsmfFunction);
            mm.addFunction(0xC7DB3A5B, scePsmfGetCurrentStreamTypeFunction);
            mm.addFunction(0x28240568, scePsmfGetCurrentStreamNumberFunction);
            mm.addFunction(0x1E6D9013, scePsmfSpecifyStreamWithStreamTypeFunction);
            mm.addFunction(0x4BC9BDE0, scePsmfSpecifyStreamFunction);
            mm.addFunction(0x76D3AEBA, scePsmfGetPresentationStartTimeFunction);
            mm.addFunction(0xBD8AE0D8, scePsmfGetPresentationEndTimeFunction);
            mm.addFunction(0xEAED89CD, scePsmfGetNumberOfStreamsFunction);
            mm.addFunction(0x7491C438, scePsmfGetNumberOfEPentriesFunction);
            mm.addFunction(0x0BA514E5, scePsmfGetVideoInfoFunction);
            mm.addFunction(0xA83F7113, scePsmfGetAudioInfoFunction);
            mm.addFunction(0x971A3A90, scePsmfCheckEPmapFunction);
            mm.addFunction(0x4E624A34, scePsmfGetEPWithIdFunction);
            mm.addFunction(0x7C0E7AC3, scePsmfGetEPWithTimestampFunction);
            mm.addFunction(0x5F457515, scePsmfGetEPidWithTimestampFunction);
            mm.addFunction(0x5B70FCC1, scePsmfQueryStreamOffsetFunction);
            mm.addFunction(0x9553CC91, scePsmfQueryStreamSizeFunction);
            mm.addFunction(0x68D42328, scePsmfGetNumberOfSpecificStreamsFunction);
            mm.addFunction(0x0C120E1D, scePsmfSpecifyStreamWithStreamTypeNumberFunction);
            mm.addFunction(0x2673646B, scePsmfVerifyPsmfFunction);
            mm.addFunction(0xB78EB9E9, scePsmfGetHeaderSizeFunction);
            mm.addFunction(0xA5EBFE81, scePsmfGetStreamSizeFunction);
            mm.addFunction(0xE1283895, scePsmfGetPsmfVersionFunction);

        }
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.removeFunction(scePsmfSetPsmfFunction);
            mm.removeFunction(scePsmfGetCurrentStreamTypeFunction);
            mm.removeFunction(scePsmfGetCurrentStreamNumberFunction);
            mm.removeFunction(scePsmfSpecifyStreamWithStreamTypeFunction);
            mm.removeFunction(scePsmfSpecifyStreamFunction);
            mm.removeFunction(scePsmfGetPresentationStartTimeFunction);
            mm.removeFunction(scePsmfGetPresentationEndTimeFunction);
            mm.removeFunction(scePsmfGetNumberOfStreamsFunction);
            mm.removeFunction(scePsmfGetNumberOfEPentriesFunction);
            mm.removeFunction(scePsmfGetVideoInfoFunction);
            mm.removeFunction(scePsmfGetAudioInfoFunction);
            mm.removeFunction(scePsmfCheckEPmapFunction);
            mm.removeFunction(scePsmfGetEPWithIdFunction);
            mm.removeFunction(scePsmfGetEPWithTimestampFunction);
            mm.removeFunction(scePsmfGetEPidWithTimestampFunction);
            mm.removeFunction(scePsmfQueryStreamOffsetFunction);
            mm.removeFunction(scePsmfQueryStreamSizeFunction);
            mm.removeFunction(scePsmfGetNumberOfSpecificStreamsFunction);
            mm.removeFunction(scePsmfSpecifyStreamWithStreamTypeNumberFunction);
            mm.removeFunction(scePsmfVerifyPsmfFunction);
            mm.removeFunction(scePsmfGetHeaderSizeFunction);
            mm.removeFunction(scePsmfGetStreamSizeFunction);
            mm.removeFunction(scePsmfGetPsmfVersionFunction);

        }
    }

    @Override
    public void start() {
        psmfMap = new HashMap<Integer, PSMFHeader>();
    }

    @Override
    public void stop() {
    }
    private HashMap<Integer, PSMFHeader> psmfMap;

    protected int endianSwap32(int x) {
        return (x << 24) | ((x << 8) & 0xFF0000) | ((x >> 8) & 0xFF00) | ((x >> 24) & 0xFF);
    }

    protected int endianSwap16(int x) {
        return (x >> 8) | ((x << 8) & 0xFF00);
    }

    protected class PSMFHeader {

        private static final int size = 2048;
        private static final int PSMF_VIDEO_STREAM_ID = 0xE000;
        private static final int PSMF_AUDIO_STREAM_ID = 0xBD00;
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
        private int streamStructSize;
        private int audioSampleFrequency;
        private int audioChannelsSetup;
        private int videoWidth;
        private int videoHeight;
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

            private int EPPts;
            private int EPOffset;

            public PSMFEntry(int pts, int offset) {
                EPPts = pts;
                EPOffset = offset;
            }

            public int getEntryPTS() {
                return EPPts;
            }

            public int getEntryOffset() {
                return EPOffset;
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
        }

        public PSMFHeader(int addr) {
            Memory mem = Memory.getInstance();

            if (mem.read32(addr) != sceMpeg.PSMF_MAGIC) {
                log.warn("Invalid PSMF detected!");
            }

            headerOffset = addr;

            version = mem.read32(addr + 4);
            streamOffset = endianSwap32(mem.read32(addr + 8));
            streamSize = endianSwap32(mem.read32(addr + 12));

            presentationStartTime = (endianSwap16(mem.read16(addr + 86)) << 16) | endianSwap16(mem.read16(addr + 88));  // First PTS in EPMap (90000).
            presentationEndTime = (endianSwap16(mem.read16(addr + 92)) << 16) | endianSwap16(mem.read16(addr + 94));    // mpegLastTimestamp.

            streamStructSize = mem.read8(addr + 127);                      // 16 bytes per stream + 2 bytes for total stream number.
            streamNum = endianSwap16(mem.read16(addr + 128));              // Number of total registered streams.

            // Stream area:
            // At offset 130, each 16 bytes represent one stream:
            //  - First two bytes (short): stream type ID;
            //  - Next two bytes (short): size of the stream?;
            //  - Remaining: padding and video/audio info.
            streamMap = new HashMap<Integer, PSMFStream>();
            currentStreamNumber = -1;         // Current stream number.
            currentVideoStreamNumber = -1;    // Current video stream number.
            currentAudioStreamNumber = -1;    // Current audio stream number.
            videoStreamNum = 0;
            audioStreamNum = 0;

            // Parse the stream field and assign each one to it's type.
            for (int i = 0; i < streamNum; i++) {
                PSMFStream stream = null;
                int streamID = endianSwap16(mem.read16(addr + 130 + i * 16));
                if ((streamID & PSMF_VIDEO_STREAM_ID) == PSMF_VIDEO_STREAM_ID) {
                    stream = new PSMFStream(PSMF_AVC_STREAM, 0);
                    currentVideoStreamNumber++;
                    videoStreamNum++;
                } else if ((streamID & PSMF_AUDIO_STREAM_ID) == PSMF_AUDIO_STREAM_ID) {
                    stream = new PSMFStream(PSMF_ATRAC_STREAM, 1);
                    currentAudioStreamNumber++;
                    audioStreamNum++;
                }
                if (stream != null) {
                    currentStreamNumber++;
                    streamMap.put(currentStreamNumber, stream);
                }
            }

            // Video stream info.
            EPMapEntriesNum = mem.read8(addr + 141);       // Number of entry points in the EPMap.
            videoWidth = (mem.read8(addr + 142) * 0x10);   // PSMF video width (bytes per line).
            videoHeight = (mem.read8(addr + 143) * 0x10);  // PSMF video heigth (bytes per line).

            // Audio stream info.
            audioChannelsSetup = mem.read8(addr + 160);    // 1 - Mono; 2 - Stereo.
            audioSampleFrequency = mem.read8(addr + 161);  // 2 - 44100kHz.

            // EPMap Offset:
            // - Depends on the number of streams present in the header.
            EPMapOffset = addr + 128 + streamStructSize;

            // EPMap info:
            // - Located at EPMapOffset;
            // - Each entry is composed by a total of 10 bytes:
            //      - First 2 bytes (short): Padding;
            //      - Next 4 bytes (int): PTS of the entry point;
            //      - Last 4 bytes (int): Relative offset of the entry point in the MPEG data.
            EPMap = new HashMap<Integer, PSMFEntry>();

            for (int i = 0; i < EPMapEntriesNum; i++) {
                int pts = endianSwap32(mem.read32(EPMapOffset + 2 + i * 10));
                int offset = endianSwap32(mem.read32(EPMapOffset + 6 + i * 10));
                PSMFEntry pEnt = new PSMFEntry(pts, offset);
                EPMap.put(currentEntryNumber++, pEnt);
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

        public int getVideoHeight() {
            return videoHeight;
        }

        public int getAudioSampleFrequency() {
            return audioSampleFrequency;
        }

        public int getAudioChannelsSetup() {
            return audioChannelsSetup;
        }

        public int getEPMapOffset() {
            return EPMapOffset;
        }

        public int getEPMapEntriesNum() {
            return EPMapEntriesNum;
        }

        public PSMFEntry getEPMapEntry(int id) {
            return EPMap.get(id);
        }

        public PSMFEntry getEPMapEntryWithTimestamp(int ts) {
            PSMFEntry entry = null;
            for (; EPMap.values().iterator().hasNext(); entry = EPMap.values().iterator().next()) {
                if (entry != null) {
                    if (entry.getEntryPTS() == ts) {
                        break;
                    }
                }
            }
            return entry;
        }

        public int getEPMapEntryIDWithTimestamp(int ts) {
            PSMFEntry entry = null;
            int id = 0;
            for (; EPMap.values().iterator().hasNext(); entry = EPMap.values().iterator().next()) {
                if (entry != null) {
                    if (entry.getEntryPTS() == ts) {
                        break;
                    }
                }
                id++;
            }
            return id;
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
        psmfMap.put(psmf, header);

        // Reads several parameters from the PMF file header and stores them in
        // a 52 byte struct.
        mem.write32(psmf, header.getVersion());                  // PSMF version.
        mem.write32(psmf + 4, header.getHeaderSize());           // The PSMF header size (0x800).
        mem.write32(psmf + 8, header.getStreamSize());           // The PSMF stream size.
        mem.write32(psmf + 12, 0);                               // Grouping Period ID.
        mem.write32(psmf + 16, 0);                               // Group ID.
        mem.write32(psmf + 20, header.getCurrentStreamNumber()); // Current stream's number.
        mem.write32(psmf + 24, header.getHeaderOffset());        // Pointer to PSMF header.
        mem.write32(psmf + 28, 0);                               // Pointer to current PSMF stream info (video/audio).
        mem.write32(psmf + 32, 0);                               // Pointer to mark data (used for chapters).
        mem.write32(psmf + 36, 0);                               // Pointer to current PSMF stream grouping period.
        mem.write32(psmf + 40, 0);                               // Pointer to current PSMF stream group.
        mem.write32(psmf + 44, header.getStreamOffset());        // Pointer to current PSMF stream.
        mem.write32(psmf + 48, header.getEPMapOffset());         // Pointer to PSMF EPMap.

        cpu.gpr[2] = 0;
    }

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
        if (psmfMap.containsKey(psmf)) {
            int streamType = psmfMap.get(psmf).getCurrentStreamType();
            int streamCh = psmfMap.get(psmf).getCurrentStreamChannel();
            mem.write32(type_addr, streamType);
            mem.write32(ch_addr, streamCh);
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_NOT_FOUND;
        }
    }

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
        if (psmfMap.containsKey(psmf)) {
            cpu.gpr[2] = psmfMap.get(psmf).getCurrentStreamNumber();
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_NOT_FOUND;
        }
    }

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
        if (psmfMap.containsKey(psmf)) {
            psmfMap.get(psmf).setStreamType(type, ch);
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_NOT_FOUND;
        }
    }

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
        if (psmfMap.containsKey(psmf)) {
            psmfMap.get(psmf).setStreamNum(streamNum);
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_NOT_FOUND;
        }
    }

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
        if (psmfMap.containsKey(psmf)) {
            int startTime = psmfMap.get(psmf).getPresentationStartTime();
            mem.write32(startTimeAddr, startTime);
            cpu.gpr[2] = 0;
        } else {
            mem.write32(startTimeAddr, 0);
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_NOT_FOUND;
        }
    }

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
        if (psmfMap.containsKey(psmf)) {
            int endTime = psmfMap.get(psmf).getPresentationStartTime();
            mem.write32(endTimeAddr, endTime);
            cpu.gpr[2] = 0;
        } else {
            mem.write32(endTimeAddr, 0);
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_NOT_FOUND;
        }
    }

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
        if (psmfMap.containsKey(psmf)) {
            cpu.gpr[2] = psmfMap.get(psmf).getNumberOfStreams();
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_NOT_FOUND;
        }
    }

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
        if (psmfMap.containsKey(psmf)) {
            cpu.gpr[2] = psmfMap.get(psmf).getEPMapEntriesNum();
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_NOT_FOUND;
        }
    }

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
        if (psmfMap.containsKey(psmf)) {
            int width = psmfMap.get(psmf).getVideoWidth();
            int height = psmfMap.get(psmf).getVideoHeight();
            mem.write32(videoInfoAddr, width);
            mem.write32(videoInfoAddr + 4, height);
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_NOT_FOUND;
        }
    }

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
        if (psmfMap.containsKey(psmf)) {
            int setup = psmfMap.get(psmf).getAudioChannelsSetup();
            int freq = psmfMap.get(psmf).getAudioSampleFrequency();
            mem.write32(audioInfoAddr, setup);
            mem.write32(audioInfoAddr + 4, freq);
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_NOT_FOUND;
        }
    }

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
        if (psmfMap.containsKey(psmf)) {
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_NOT_FOUND;
        }
    }

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
        if (psmfMap.containsKey(psmf)) {
            int pts = psmfMap.get(psmf).getEPMapEntry(id).getEntryPTS();
            int offset = psmfMap.get(psmf).getEPMapEntry(id).getEntryOffset();
            mem.write32(out_addr, pts);
            mem.write32(out_addr + 4, offset);
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_NOT_FOUND;
        }
    }

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
        if (psmfMap.containsKey(psmf)) {
            if (ts < psmfMap.get(psmf).getPresentationStartTime()) {
                cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_INVALID_TIMESTAMP;
            } else {
                PSMFHeader.PSMFEntry entry = psmfMap.get(psmf).getEPMapEntryWithTimestamp(ts);
                int pts = entry.getEntryPTS();
                int offset = entry.getEntryOffset();
                mem.write32(entry_addr, pts);
                mem.write32(entry_addr + 4, offset);
                cpu.gpr[2] = 0;
            }
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_NOT_FOUND;
        }
    }

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
        if (psmfMap.containsKey(psmf)) {
            if (ts < psmfMap.get(psmf).getPresentationStartTime()) {
                cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_INVALID_TIMESTAMP;
            } else {
                cpu.gpr[2] = psmfMap.get(psmf).getEPMapEntryIDWithTimestamp(ts);
            }
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_NOT_FOUND;
        }
    }

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
        int offset = endianSwap32(mem.read32(buffer_addr + 8));
        mem.write32(offset_addr, offset);

        // Always let sceMpeg handle the PSMF analysis.
        Modules.sceMpegModule.analyseMpeg(buffer_addr);

        cpu.gpr[2] = 0;
    }

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
        int size = endianSwap32(mem.read32(buffer_addr + 12));
        if ((size & 0x7FF) == 0) {
            mem.write32(size_addr, 0);
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_INVALID_VALUE;
        } else {
            mem.write32(size_addr, size);
            cpu.gpr[2] = 0;
        }
        // Always let sceMpeg handle the PSMF analysis.
        Modules.sceMpegModule.analyseMpeg(buffer_addr);
    }

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
        if (psmfMap.containsKey(psmf)) {
            cpu.gpr[2] = psmfMap.get(psmf).getSpecificStreamNum(stream_type);
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_NOT_FOUND;
        }
    }

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
        if (psmfMap.containsKey(psmf)) {
            psmfMap.get(psmf).setStreamTypeNum(type, type_num);
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_NOT_FOUND;
        }
    }

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
            int version = mem.read32(buffer_addr + 4);
            if (version > sceMpeg.PSMF_VERSION_0015) {
                cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_INVALID_PSMF;
            } else {
                cpu.gpr[2] = 0;
            }
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_INVALID_PSMF;
        }
    }

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
        if (psmfMap.containsKey(psmf)) {
            int size = psmfMap.get(psmf).getHeaderSize();
            mem.write32(size_addr, size);
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_NOT_FOUND;
        }
    }

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
        if (psmfMap.containsKey(psmf)) {
            int size = psmfMap.get(psmf).getStreamSize();
            mem.write32(size_addr, size);
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_NOT_FOUND;
        }
    }

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
        if (psmfMap.containsKey(psmf)) {
            cpu.gpr[2] = psmfMap.get(psmf).getVersion();
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_NOT_FOUND;
        }
    }

    public final HLEModuleFunction scePsmfSetPsmfFunction = new HLEModuleFunction("scePsmf", "scePsmfSetPsmf") {

        @Override
        public final void execute(Processor processor) {
            scePsmfSetPsmf(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfModule.scePsmfSetPsmf(processor);";
        }
    };
    public final HLEModuleFunction scePsmfGetCurrentStreamTypeFunction = new HLEModuleFunction("scePsmf", "scePsmfGetCurrentStreamType") {

        @Override
        public final void execute(Processor processor) {
            scePsmfGetCurrentStreamType(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfModule.scePsmfGetCurrentStreamType(processor);";
        }
    };
    public final HLEModuleFunction scePsmfGetCurrentStreamNumberFunction = new HLEModuleFunction("scePsmf", "scePsmfGetCurrentStreamNumber") {

        @Override
        public final void execute(Processor processor) {
            scePsmfGetCurrentStreamNumber(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfModule.scePsmfGetCurrentStreamNumber(processor);";
        }
    };
    public final HLEModuleFunction scePsmfSpecifyStreamWithStreamTypeFunction = new HLEModuleFunction("scePsmf", "scePsmfSpecifyStreamWithStreamType") {

        @Override
        public final void execute(Processor processor) {
            scePsmfSpecifyStreamWithStreamType(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfModule.scePsmfSpecifyStreamWithStreamType(processor);";
        }
    };
    public final HLEModuleFunction scePsmfSpecifyStreamFunction = new HLEModuleFunction("scePsmf", "scePsmfSpecifyStream") {

        @Override
        public final void execute(Processor processor) {
            scePsmfSpecifyStream(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfModule.scePsmfSpecifyStream(processor);";
        }
    };
    public final HLEModuleFunction scePsmfGetPresentationStartTimeFunction = new HLEModuleFunction("scePsmf", "scePsmfGetPresentationStartTime") {

        @Override
        public final void execute(Processor processor) {
            scePsmfGetPresentationStartTime(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfModule.scePsmfGetPresentationStartTime(processor);";
        }
    };
    public final HLEModuleFunction scePsmfGetPresentationEndTimeFunction = new HLEModuleFunction("scePsmf", "scePsmfGetPresentationEndTime") {

        @Override
        public final void execute(Processor processor) {
            scePsmfGetPresentationEndTime(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfModule.scePsmfGetPresentationEndTime(processor);";
        }
    };
    public final HLEModuleFunction scePsmfGetNumberOfStreamsFunction = new HLEModuleFunction("scePsmf", "scePsmfGetNumberOfStreams") {

        @Override
        public final void execute(Processor processor) {
            scePsmfGetNumberOfStreams(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfModule.scePsmfGetNumberOfStreams(processor);";
        }
    };
    public final HLEModuleFunction scePsmfGetNumberOfEPentriesFunction = new HLEModuleFunction("scePsmf", "scePsmfGetNumberOfEPentries") {

        @Override
        public final void execute(Processor processor) {
            scePsmfGetNumberOfEPentries(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfModule.scePsmfGetNumberOfEPentries(processor);";
        }
    };
    public final HLEModuleFunction scePsmfGetVideoInfoFunction = new HLEModuleFunction("scePsmf", "scePsmfGetVideoInfo") {

        @Override
        public final void execute(Processor processor) {
            scePsmfGetVideoInfo(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfModule.scePsmfGetVideoInfo(processor);";
        }
    };
    public final HLEModuleFunction scePsmfGetAudioInfoFunction = new HLEModuleFunction("scePsmf", "scePsmfGetAudioInfo") {

        @Override
        public final void execute(Processor processor) {
            scePsmfGetAudioInfo(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfModule.scePsmfGetAudioInfo(processor);";
        }
    };
    public final HLEModuleFunction scePsmfCheckEPmapFunction = new HLEModuleFunction("scePsmf", "scePsmfCheckEPmap") {

        @Override
        public final void execute(Processor processor) {
            scePsmfCheckEPmap(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfModule.scePsmfCheckEPmap(processor);";
        }
    };
    public final HLEModuleFunction scePsmfGetEPWithIdFunction = new HLEModuleFunction("scePsmf", "scePsmfGetEPWithId") {

        @Override
        public final void execute(Processor processor) {
            scePsmfGetEPWithId(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfModule.scePsmfGetEPWithId(processor);";
        }
    };
    public final HLEModuleFunction scePsmfGetEPWithTimestampFunction = new HLEModuleFunction("scePsmf", "scePsmfGetEPWithTimestamp") {

        @Override
        public final void execute(Processor processor) {
            scePsmfGetEPWithTimestamp(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfModule.scePsmfGetEPWithTimestamp(processor);";
        }
    };
    public final HLEModuleFunction scePsmfGetEPidWithTimestampFunction = new HLEModuleFunction("scePsmf", "scePsmfGetEPidWithTimestamp") {

        @Override
        public final void execute(Processor processor) {
            scePsmfGetEPidWithTimestamp(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfModule.scePsmfGetEPidWithTimestamp(processor);";
        }
    };
    public final HLEModuleFunction scePsmfQueryStreamOffsetFunction = new HLEModuleFunction("scePsmf", "scePsmfQueryStreamOffset") {

        @Override
        public final void execute(Processor processor) {
            scePsmfQueryStreamOffset(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfModule.scePsmfQueryStreamOffset(processor);";
        }
    };
    public final HLEModuleFunction scePsmfQueryStreamSizeFunction = new HLEModuleFunction("scePsmf", "scePsmfQueryStreamSize") {

        @Override
        public final void execute(Processor processor) {
            scePsmfQueryStreamSize(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfModule.scePsmfQueryStreamSize(processor);";
        }
    };
    public final HLEModuleFunction scePsmfGetNumberOfSpecificStreamsFunction = new HLEModuleFunction("scePsmf", "scePsmfGetNumberOfSpecificStreams") {

        @Override
        public final void execute(Processor processor) {
            scePsmfGetNumberOfSpecificStreams(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfModule.scePsmfGetNumberOfSpecificStreams(processor);";
        }
    };
    public final HLEModuleFunction scePsmfSpecifyStreamWithStreamTypeNumberFunction = new HLEModuleFunction("scePsmf", "scePsmfSpecifyStreamWithStreamTypeNumber") {

        @Override
        public final void execute(Processor processor) {
            scePsmfSpecifyStreamWithStreamTypeNumber(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfModule.scePsmfSpecifyStreamWithStreamTypeNumber(processor);";
        }
    };
    public final HLEModuleFunction scePsmfVerifyPsmfFunction = new HLEModuleFunction("scePsmf", "scePsmfVerifyPsmf") {

        @Override
        public final void execute(Processor processor) {
            scePsmfVerifyPsmf(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfModule.scePsmfVerifyPsmf(processor);";
        }
    };
    public final HLEModuleFunction scePsmfGetHeaderSizeFunction = new HLEModuleFunction("scePsmf", "scePsmfGetHeaderSize") {

        @Override
        public final void execute(Processor processor) {
            scePsmfGetHeaderSize(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfModule.scePsmfGetHeaderSize(processor);";
        }
    };
    public final HLEModuleFunction scePsmfGetStreamSizeFunction = new HLEModuleFunction("scePsmf", "scePsmfGetStreamSize") {

        @Override
        public final void execute(Processor processor) {
            scePsmfGetStreamSize(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfModule.scePsmfGetStreamSize(processor);";
        }
    };
    public final HLEModuleFunction scePsmfGetPsmfVersionFunction = new HLEModuleFunction("scePsmf", "scePsmfGetPsmfVersion") {

        @Override
        public final void execute(Processor processor) {
            scePsmfGetPsmfVersion(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfModule.scePsmfGetPsmfVersion(processor);";
        }
    };
}