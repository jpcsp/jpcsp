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
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.HLEStartModule;

public class scePsmf implements HLEModule, HLEStartModule {
	@Override
	public String getName() { return "scePsmf"; }

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
        return (x << 24) | ((x << 8) &  0xFF0000) | ((x >> 8) &  0xFF00) | ((x >> 24) &  0xFF);
    }

    protected int endianSwap16(int x) {
        return (x >> 8) | ((x << 8) & 0xFF00);
    }

    protected boolean isPSMFInit(int PSMFType) {
        return ((PSMFType & 0xE) == 0xE);
    }

    protected int comparePSMFVersion(int PSMFType, int versionToCompare) {
        int version = (PSMFType >> 4);
        if(version > versionToCompare) {
            return 1;
        } else if (version < versionToCompare) {
            return -1;
        } else {
            return 0;
        }
    }

    protected class PSMFHeader {
        // Common fields.
        private final int size = 2048;

        // Header vars.
        private int streamOffset;
        private int streamSize;
        private int version;
        private int type;
        private int presentationStartTime;
        private int presentationEndTime;
        private int groupingPeriodID;
        private int groupID;
        private int currentStreamType;
        private int currentStreamNumber;
        private int streamNum;
        private int streamStructSize;
        private int streamVideoType;
        private int streamAudioType;
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

        public PSMFHeader(int addr) {
            Memory mem = Memory.getInstance();

            headerOffset = addr;

            if (mem.read32(addr) != sceMpeg.PSMF_MAGIC) {
                Modules.log.warn("Invalid PSMF detected!");
            }

            version = mem.read32(addr + 4);
            streamOffset = endianSwap32(mem.read32(addr + 8));
            streamSize = endianSwap32(mem.read32(addr + 12));

            // Skip block of null data (80 bytes).

            type = endianSwap32(mem.read32(addr + 80));

            presentationStartTime = endianSwap32(mem.read32(addr + 86));  // First PTS in EPMap (90000).
            presentationEndTime = endianSwap32(mem.read32(addr + 92));    // mpegLastTimestamp.

            currentStreamType = mem.read8(addr + 104);
            currentStreamNumber = mem.read8(addr + 105);

            // Global stream:
            //  - Counts as one stream;
            //  - First two bytes (short): number of streams;
            //  - Next two bytes (short): a generic ID (0xE000);
            //  - Next two bytes (short): a generic size (0x20FB - can change);
            //  - Remaining: global audio and video settings.
            streamStructSize = endianSwap16(mem.read16(addr + 126));  // 16 bytes per stream + 2 bytes for the streamStructSize.
            streamNum = endianSwap16(mem.read16(addr + 128));

            audioSampleFrequency = mem.read16(addr + 136);
            audioChannelsSetup = mem.read16(addr + 138);

            videoWidth = (mem.read8(addr + 142) * 0x10);
            videoHeight = (mem.read8(addr + 143) * 0x10);

            // Stream area:
            // At offset 144, each 16 bytes represent one stream:
            //  - First byte: stream type;
            //  - Second byte: stream number;
            //  - Next two bytes (short): some sort of ID for the stream;
            //  - Next two bytes (short): size of the stream;
            //  - Remaining: padding.

            // EPMap Offset:
            // - Depends on the number of streams present in the header (not counting with the global stream).
            EPMapOffset = (addr + 144) + ((streamNum - 1) * 0x10);

            // Number of entry points in the EPMap.
            EPMapEntriesNum = mem.read16(EPMapOffset);

            // EPMap info:
            // - Located at EPMapOffset;
            // - Each entry is composed by a total of 10 bytes:
            //      - First 2 bytes (short): ID of the entry point;
            //      - Next 4 bytes (int): PTS of the entry point;
            //      - Last 4 bytes (int): Relative offset of the entry point in the MPEG data.
            EPMap = new HashMap<Integer, PSMFEntry>();

            for(int i = 0; i < EPMapEntriesNum; i += 10) {
                int id = mem.read16(EPMapOffset + 2 + i);
                int pts = endianSwap32(mem.read32(EPMapOffset + 4 + i));
                int offset = endianSwap32(mem.read32(EPMapOffset + 6 + i));

                PSMFEntry pEnt = new PSMFEntry(pts, offset);
                EPMap.put(id, pEnt);
            }
        }

        public int getVersion() {
            return version;
        }
        public int getType() {
            return type;
        }
        public int getHeaderSize() {
            return size;
        }
        public int getStreamSize() {
            return streamSize;
        }
        public int getCurrentStreamType() {
            return currentStreamType;
        }
        public int getCurrentStreamNumber() {
            return currentStreamNumber;
        }
        public int getNumberOfStreams() {
            return streamNum;
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
        public int getHeaderOffset() {
            return headerOffset;
        }
        public int getEPMapOffset() {
            return EPMapOffset;
        }
        public int getEPMapEntriesNum() {
            return EPMapEntriesNum;
        }
        public int getEPMapEntryPts(int id) {
            return EPMap.get(id).getEntryPTS();
        }
        public int getEPMapEntryOffset(int id) {
            return EPMap.get(id).getEntryOffset();
        }
    }

    public void scePsmfSetPsmf(Processor processor) {
	    CpuState cpu = processor.cpu;
		Memory mem = Processor.memory;

        int psmf = cpu.gpr[4];         // PSMF struct.
        int buffer_addr = cpu.gpr[5];  // Actual PMF data.

		Modules.log.warn("PARTIAL: scePsmfSetPsmf (psmf=0x" + Integer.toHexString(psmf)
                + " buffer_addr=0x" + Integer.toHexString(buffer_addr) + ")");

        PSMFHeader header = new PSMFHeader(buffer_addr);
        psmfMap.put(psmf, header);

        // Reads several parameters from the PMF file header and stores them in
        // a 48 byte struct.
        mem.write32(psmf, header.getType());                     // PMSF type (e.g.: 0x0000004E).
        mem.write32(psmf + 4, header.getStreamSize());           // The PSMF stream size.
        mem.write32(psmf + 8, header.getHeaderSize());           // The PSMF header size (0x800).
        mem.write32(psmf + 12, 0);                               // Grouping Period ID.
        mem.write32(psmf + 16, 0);                               // Group ID.
        mem.write32(psmf + 20, 0);                               // Unknown (current stream?).
        mem.write32(psmf + 24, header.getHeaderOffset());        // Pointer to PSMF header.
        mem.write32(psmf + 28, 0);                               // Unknown.
        mem.write32(psmf + 32, 0);                               // Unknown.
        mem.write32(psmf + 36, 0);                               // Unknown.
        mem.write32(psmf + 40, 0);                               // Unknown.
        mem.write32(psmf + 44, header.getEPMapOffset());         // Pointer to PSMF EPMap.

		cpu.gpr[2] = 0;
	}

    public void scePsmfGetCurrentStreamType(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int psmf = cpu.gpr[4];
        int type_addr = cpu.gpr[5];
        int num_addr = cpu.gpr[6];

        Modules.log.warn("PARTIAL: scePsmfGetCurrentStreamType (psmf=0x" + Integer.toHexString(psmf)
                + " type_addr=0x" + Integer.toHexString(type_addr)
                + " num_addr=0x" + Integer.toHexString(num_addr) + ")");

        if(psmfMap.containsKey(psmf)) {
            int streamType = psmfMap.get(psmf).getCurrentStreamType();
            int streamNum = psmfMap.get(psmf).getCurrentStreamNumber();
            mem.write32(type_addr, streamType);
            mem.write32(num_addr, streamNum);
        }

        cpu.gpr[2] = 0;
    }

    public void scePsmfGetCurrentStreamNumber(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int psmf = cpu.gpr[4];

        Modules.log.warn("PARTIAL: scePsmfGetCurrentStreamNumber psmf=0x" + Integer.toHexString(psmf));

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void scePsmfSpecifyStreamWithStreamType(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        Modules.log.warn("Unimplemented NID function scePsmfSpecifyStreamWithStreamType [0x1E6D9013]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void scePsmfSpecifyStream(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        Modules.log.warn("Unimplemented NID function scePsmfSpecifyStream [0x4BC9BDE0]");

        cpu.gpr[2] = 0;
    }

    public void scePsmfGetPresentationStartTime(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int psmf = cpu.gpr[4];
        int startTimeAddr = cpu.gpr[5];

        Modules.log.warn("PARTIAL: scePsmfGetPresentationStartTime (psmf=0x" + Integer.toHexString(psmf)
                + " startTimeAddr=0x" + Integer.toHexString(startTimeAddr) + ")");

        if(psmfMap.containsKey(psmf)) {
            int startTime = psmfMap.get(psmf).getPresentationStartTime();
            mem.write32(startTimeAddr, startTime);
        } else {
            mem.write32(startTimeAddr, 0);
        }

        cpu.gpr[2] = 0;
    }

    public void scePsmfGetPresentationEndTime(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int psmf = cpu.gpr[4];
        int endTimeAddr = cpu.gpr[5];

        Modules.log.warn("PARTIAL: scePsmfGetPresentationEndTime (psmf=0x" + Integer.toHexString(psmf)
                + " endTimeAddr=0x" + Integer.toHexString(endTimeAddr) + ")");

        if(psmfMap.containsKey(psmf)) {
            int endTime = psmfMap.get(psmf).getPresentationStartTime();
            mem.write32(endTimeAddr, endTime);
        } else {
            mem.write32(endTimeAddr, 0);
        }

        cpu.gpr[2] = 0;
    }

    public void scePsmfGetNumberOfStreams(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmf = cpu.gpr[4];

        Modules.log.warn("PARTIAL: scePsmfGetNumberOfStreams psmf=0x" + Integer.toHexString(psmf));

        int res = 0;
        if(psmfMap.containsKey(psmf)) {
            if(!isPSMFInit(psmfMap.get(psmf).getType())) {
                res = SceKernelErrors.ERROR_PSMF_NOT_INITIALIZED;
            } else if(comparePSMFVersion(psmfMap.get(psmf).getType(), 2) != 1) {  // If version > 2, it should be loaded by scePsmfPlayer.
                res = SceKernelErrors.ERROR_PSMF_BAD_VERSION;
            } else {
                res = psmfMap.get(psmf).getNumberOfStreams();
            }
        }

        cpu.gpr[2] = res;
    }

    public void scePsmfGetNumberOfEPentries(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmf = cpu.gpr[4];

		Modules.log.warn("PARTIAL: scePsmfGetNumberOfEPentries (psmf=0x" + Integer.toHexString(psmf) + ")");

        int entries = 0;
        if(psmfMap.containsKey(psmf)) {
            entries = psmfMap.get(psmf).getEPMapEntriesNum();
        }

        cpu.gpr[2] = entries;
    }

    public void scePsmfGetVideoInfo(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int psmf = cpu.gpr[4];
        int videoInfoAddr = cpu.gpr[5];

        Modules.log.warn("PARTIAL: scePsmfGetVideoInfo (psmf=0x" + Integer.toHexString(psmf)
                + " audioInfoAddr=0x" + Integer.toHexString(videoInfoAddr) + ")");

        if(psmfMap.containsKey(psmf)) {
            int width = psmfMap.get(psmf).getVideoWidth();
            int height = psmfMap.get(psmf).getVideoHeight();
            mem.write32(videoInfoAddr, width);
            mem.write32(videoInfoAddr + 4, height);
        }

        cpu.gpr[2] = 0;
    }

    public void scePsmfGetAudioInfo(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int psmf = cpu.gpr[4];
        int audioInfoAddr = cpu.gpr[5];

        Modules.log.warn("PARTIAL: scePsmfGetAudioInfo (psmf=0x" + Integer.toHexString(psmf)
                + " audioInfoAddr=0x" + Integer.toHexString(audioInfoAddr) + ")");

        if(psmfMap.containsKey(psmf)) {
            int setup = psmfMap.get(psmf).getAudioChannelsSetup();
            int freq = psmfMap.get(psmf).getAudioSampleFrequency();
            mem.write32(audioInfoAddr, setup);
            mem.write32(audioInfoAddr + 4, freq);
        }

        cpu.gpr[2] = 0;
    }

    public void scePsmfCheckEPmap(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        Modules.log.warn("Unimplemented NID function scePsmfCheckEPmap [0x971A3A90]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void scePsmfGetEPWithId(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int psmf = cpu.gpr[4];
        int id = cpu.gpr[5];
        int out_addr = cpu.gpr[6];

        Modules.log.warn("PARTIAL: scePsmfGetEPWithId (psmf=0x" + Integer.toHexString(psmf)
                + " id=0x" + Integer.toHexString(id)
                + " out_addr=0x" + Integer.toHexString(out_addr) + ")");

        if(psmfMap.containsKey(psmf)) {
            int pts = psmfMap.get(psmf).getEPMapEntryPts(id);
            int offset = psmfMap.get(psmf).getEPMapEntryOffset(id);
            mem.write32(out_addr, pts);
            mem.write32(out_addr + 4, offset);
        }

        cpu.gpr[2] = 0;
    }

    public void scePsmfGetEPWithTimestamp(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        Modules.log.warn("Unimplemented NID function scePsmfGetEPWithTimestamp [0x7C0E7AC3]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void scePsmfGetEPidWithTimestamp(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        Modules.log.warn("Unimplemented NID function scePsmfGetEPidWithTimestamp [0x5F457515]");

        cpu.gpr[2] = 0xDEADC0DE;
    }
    public void scePsmfQueryStreamOffset(Processor processor) {
	    CpuState cpu = processor.cpu;
		Memory mem = Processor.memory;

        int buffer_addr = cpu.gpr[4];
        int offset_addr = cpu.gpr[5];

		Modules.log.warn("PARTIAL: scePsmfQueryStreamOffset (buffer_addr=0x" + Integer.toHexString(buffer_addr)
                + " offset_addr=0x" + Integer.toHexString(offset_addr) + ")");

        // Same as sceMpeg. Read the offset and write it at the output address.
        int offset = endianSwap32(mem.read32(buffer_addr + 8));
        mem.write32(offset_addr, offset);

		cpu.gpr[2] = 0;
	}

    public void scePsmfQueryStreamSize(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int buffer_addr = cpu.gpr[4];
        int size_addr = cpu.gpr[5];

        Modules.log.warn("PARTIAL: scePsmfQueryStreamSize (buffer_addr=0x" + Integer.toHexString(buffer_addr)
                + " size_addr=0x" + Integer.toHexString(size_addr) + ")");

        // Same as sceMpeg. Read the size and write it at the output address.
        int size = endianSwap32(mem.read32(buffer_addr + 12));
        if((size & 0x7FF) == 0) {
            mem.write32(size_addr, 0);
            cpu.gpr[2] = SceKernelErrors.ERROR_PSMF_INVALID_VALUE;
        } else {
            mem.write32(size_addr, size);
            cpu.gpr[2] = 0;
        }
    }

    public void scePsmfGetNumberOfSpecificStreams(Processor processor) {
	    CpuState cpu = processor.cpu;

        int psmf = cpu.gpr[4];
        int stream = cpu.gpr[5];  // Stream type (same as sceMpeg).

		Modules.log.warn("PARTIAL: scePsmfGetNumberOfSpecificStreams (psmf=0x" + Integer.toHexString(psmf)
                + " stream=" + stream + ")");

        // Return at least one stream for each type (0 = video, 1 = audio).
		cpu.gpr[2] = 1;
	}

    public void scePsmfSpecifyStreamWithStreamTypeNumber(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        Modules.log.warn("Unimplemented NID function scePsmfSpecifyStreamWithStreamTypeNumber [0x0C120E1D]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

	public void scePsmfVerifyPsmf(Processor processor) {
	    CpuState cpu = processor.cpu;

        int psmf = cpu.gpr[4];

		Modules.log.warn("PARTIAL: scePsmfVerifyPsmf (psmf=0x" + Integer.toHexString(psmf) + ")");

        int res = 0;
        if(psmfMap.containsKey(psmf)) {
            int version = psmfMap.get(psmf).getVersion();
            if(version != sceMpeg.PSMF_VERSION_0012) {
                res = SceKernelErrors.ERROR_PSMF_BAD_VERSION;
            }
        } else {
            res = SceKernelErrors.ERROR_PSMF_NOT_FOUND;
        }

        cpu.gpr[2] = res;
	}

    public void scePsmfGetHeaderSize(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int psmf = cpu.gpr[4];
        int size_addr = cpu.gpr[5];

        Modules.log.warn("PARTIAL: scePsmfGetHeaderSize (psmf=0x" + Integer.toHexString(psmf)
                + " size_addr=0x" + size_addr + ")");

        int res = 0;
        if(psmfMap.containsKey(psmf)) {
            int size = psmfMap.get(psmf).getHeaderSize();
            mem.write32(size_addr, size);

            if(!isPSMFInit(psmfMap.get(psmf).getType())) {
                res = SceKernelErrors.ERROR_PSMF_NOT_INITIALIZED;
            }
        }
        cpu.gpr[2] = res;
    }

    public void scePsmfGetStreamSize(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int psmf = cpu.gpr[4];
        int size_addr = cpu.gpr[5];

        Modules.log.warn("PARTIAL: scePsmfGetStreamSize (psmf=0x" + Integer.toHexString(psmf)
                + " size_addr=0x" + size_addr + ")");

        int res = 0;
        if(psmfMap.containsKey(psmf)) {
            int size = psmfMap.get(psmf).getStreamSize();
            mem.write32(size_addr, size);

            if(!isPSMFInit(psmfMap.get(psmf).getType())) {
                res = SceKernelErrors.ERROR_PSMF_NOT_INITIALIZED;
            }
        }
        cpu.gpr[2] = res;
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

    public final HLEModuleFunction scePsmfCheckEPmapFunction = new HLEModuleFunction("scePsmf", "scePsmfGetCurrenscePsmfCheckEPmaptStreamType") {
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
}