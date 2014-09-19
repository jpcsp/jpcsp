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
package jpcsp.HLE.modules250;

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_ATRAC_INCORRECT_READ_SIZE;
import static jpcsp.HLE.modules150.sceAudiocodec.PSP_CODEC_AT3;
import static jpcsp.HLE.modules150.sceAudiocodec.PSP_CODEC_AT3PLUS;
import jpcsp.Memory;
import jpcsp.HLE.CheckArgument;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.util.Utilities;

@HLELogging
public class sceAtrac3plus extends jpcsp.HLE.modules150.sceAtrac3plus {
	protected static final int MAX_ATRAC3_IDS = 6;
	protected static final int MAX_ATRAC3PLUS_IDS = MAX_ATRAC3_IDS / 2;

	protected static int read24(Memory mem, int address) {
		return (mem.read8(address + 0) << 16)
		     | (mem.read8(address + 1) <<  8)
		     | (mem.read8(address + 2) <<  0);
	}

	protected static int read16(Memory mem, int address) {
		return (mem.read8(address) << 8) | mem.read8(address + 1);
	}

	protected int analyzeAA3File(TPointer buffer, int fileSize, AtracFileInfo info) {
		Memory mem = buffer.getMemory();
		int address = buffer.getAddress();
		int codecType = 0;

		int magic = read24(mem, address);
		address += 3;
		if (magic != 0x656133 && magic != 0x494433) { // 3ae | 3AE
			log.error(String.format("Unknown AA3 magic 0x%06X", magic));
			return codecType;
		}

		if (mem.read8(address) != 3 || mem.read8(address + 1) != 0) {
			log.error(String.format("Unknown AA3 bytes 0x%08X 0x%08X", mem.read8(address), mem.read8(address + 1)));
			return SceKernelErrors.ERROR_AA3_INVALID_HEADER_VERSION;
		}
		address += 3;

		int headerSize = read28(mem, address);
		address += 4 + headerSize;
		if (mem.read8(address) == 0) {
			address += 16;
		}
		info.inputFileDataOffset = address - buffer.getAddress();

		magic = read24(mem, address);
		if (magic != 0x454133) { // 3AE
			log.error(String.format("Unknown AA3 magic 0x%06X", magic));
			return SceKernelErrors.ERROR_AA3_INVALID_HEADER;
		}
		address += 4;

		int dataOffset = read16(mem, address);
		if (dataOffset == 0xFFFF) {
			return SceKernelErrors.ERROR_AA3_INVALID_HEADER;
		}
		address += 2;

		int unknown2 = read16(mem, address);
		if (unknown2 != 0xFFFF) {
			return SceKernelErrors.ERROR_AA3_INVALID_HEADER;
		}
		address += 2;

		address += 24;

		switch (mem.read8(address)) {
			case 0: codecType = PSP_CODEC_AT3; break;
			case 1: codecType = PSP_CODEC_AT3PLUS; break;
			default: return SceKernelErrors.ERROR_AA3_INVALID_CODEC;
		}

		info.inputFileDataOffset += dataOffset;
		info.inputFileSize = fileSize;

		return codecType;
	}

	@HLEFunction(nid = 0xB3B5D042, version = 250, checkInsideInterrupt = true)
    public int sceAtracGetOutputChannel(@CheckArgument("checkAtracID") int atID, TPointer32 outputChannelAddr) {
    	AtracID id = atracIDs[atID];
        outputChannelAddr.setValue(id.getOutputChannels());

        return 0;
    }

    @HLEFunction(nid = 0xECA32A99, version = 250, checkInsideInterrupt = true)
    public boolean sceAtracIsSecondBufferNeeded(@CheckArgument("checkAtracID") int atID) {
        AtracID id = atracIDs[atID];
        // 0 -> Second buffer isn't needed.
        // 1 -> Second buffer is needed.
        return id.isSecondBufferNeeded();
    }

    @HLEFunction(nid = 0x132F1ECA, version = 250, checkInsideInterrupt = true)
    public int sceAtracReinit(int at3IDNum, int at3plusIDNum) {
		int result = hleAtracReinit(at3IDNum, at3plusIDNum);

    	if (result >= 0) {
    		Modules.ThreadManForUserModule.hleYieldCurrentThread();
    	}

    	return result;
    }

    @HLEFunction(nid = 0x2DD3E298, version = 250, checkInsideInterrupt = true)
    public int sceAtracGetBufferInfoForResetting(@CheckArgument("checkAtracID") int atID, int sample, TPointer32 bufferInfoAddr) {
        AtracID id = atracIDs[atID];
        return id.getBufferInfoForResetting(sample, bufferInfoAddr);
    }

    @HLEFunction(nid = 0x5CF9D852, version = 250, checkInsideInterrupt = true)
    public int sceAtracSetMOutHalfwayBuffer(@CheckArgument("checkAtracID") int atID, TPointer MOutHalfBuffer, int readSize, int MOutHalfBufferSize) {
    	return hleSetHalfwayBuffer(atID, MOutHalfBuffer, readSize, MOutHalfBufferSize, true);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF6837A1A, version = 250, checkInsideInterrupt = true)
    public int sceAtracSetMOutData(@CheckArgument("checkAtracID") int atID, int unknown2, int unknown3, int unknown4, int unknown5, int unknown6) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x472E3825, version = 250, checkInsideInterrupt = true)
    public int sceAtracSetMOutDataAndGetID(int unknown1, int unknown2, int unknown3, int unknown4, int unknown5, int unknown6) {
        return 0;
    }

    @HLEFunction(nid = 0x9CD7DE03, version = 250, checkInsideInterrupt = true)
    public int sceAtracSetMOutHalfwayBufferAndGetID(TPointer MOutHalfBuffer, int readSize, int MOutHalfBufferSize) {
    	return hleSetHalfwayBufferAndGetID(MOutHalfBuffer, readSize, MOutHalfBufferSize, true);
    }

    protected int hleSetAA3HalfwayBufferAndGetID(TPointer buffer, int readSize, int bufferSize, boolean isMonoOutput, int fileSize) {
        if (readSize > bufferSize) {
        	return ERROR_ATRAC_INCORRECT_READ_SIZE;
        }

        // readSize and bufferSize are unsigned int's.
        // Allow negative values.
        // "Tales of VS - ULJS00209" is even passing an uninitialized value bufferSize=0xDEADBEEF

        AtracFileInfo info = new AtracFileInfo();
        int codecType = analyzeAA3File(buffer, fileSize, info);
        if (codecType < 0) {
        	return codecType;
        }

        int atID = hleGetAtracID(codecType);
        if (atID < 0) {
        	return atID;
        }

        AtracID id = atracIDs[atID];
    	int result = id.setHalfwayBuffer(buffer.getAddress(), readSize, bufferSize, isMonoOutput, info);
    	if (result < 0) {
    		hleReleaseAtracID(atID);
    		return result;
    	}

        if (log.isDebugEnabled()) {
            log.debug(String.format("hleSetHalfwayBufferAndGetID returning atID=0x%X", atID));
        }

        // Reschedule
        Modules.ThreadManForUserModule.hleYieldCurrentThread();

        return atID;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5622B7C1, version = 250, checkInsideInterrupt = true)
    public int sceAtracSetAA3DataAndGetID(TPointer buffer, int bufferSize, int fileSize, int unused) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceAtracSetAA3DataAndGetID buffer:%s", Utilities.getMemoryDump(buffer.getAddress(), bufferSize)));
    	}
    	return hleSetAA3HalfwayBufferAndGetID(buffer, bufferSize, bufferSize, false, fileSize);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5DD66588, version = 250)
    public int sceAtracSetAA3HalfwayBufferAndGetID(TPointer buffer, int readSize, int bufferSize, int fileSize, int unused) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceAtracSetAA3HalfwayBufferAndGetID buffer:%s", Utilities.getMemoryDump(buffer.getAddress(), readSize)));
    	}
    	return hleSetAA3HalfwayBufferAndGetID(buffer, readSize, bufferSize, false, fileSize);
    }
}