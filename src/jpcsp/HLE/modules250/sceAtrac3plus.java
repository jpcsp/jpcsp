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

	protected int parseAA3(TPointer buffer) {
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

		magic = read24(mem, address);
		if (magic != 0x454133) { // 3AE
			log.error(String.format("Unknown AA3 magic 0x%06X", magic));
			return SceKernelErrors.ERROR_AA3_INVALID_HEADER;
		}
		address += 4;

		int unknown1 = read16(mem, address);
		if (unknown1 == 0xFFFF) {
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
			case 0: codecType = PSP_MODE_AT_3; break;
			case 1: codecType = PSP_MODE_AT_3_PLUS; break;
			default: return SceKernelErrors.ERROR_AA3_INVALID_CODEC;
		}

		return codecType;
	}

	@HLEFunction(nid = 0xB3B5D042, version = 250, checkInsideInterrupt = true)
    public int sceAtracGetOutputChannel(@CheckArgument("checkAtracID") int atID, TPointer32 outputChannelAddr) {
    	AtracID id = atracIDs.get(atID);
        outputChannelAddr.setValue(id.getAtracOutputChannels());

        return 0;
    }

    @HLEFunction(nid = 0xECA32A99, version = 250, checkInsideInterrupt = true)
    public boolean sceAtracIsSecondBufferNeeded(@CheckArgument("checkAtracID") int atID) {
        AtracID id = atracIDs.get(atID);
        // 0 -> Second buffer isn't needed.
        // 1 -> Second buffer is needed.
        return id.isSecondBufferNeeded();
    }

    @HLEFunction(nid = 0x132F1ECA, version = 250, checkInsideInterrupt = true)
    public int sceAtracReinit(int at3IDNum, int at3plusIDNum) {
    	int result = 0;
		boolean delay = false;

    	if (atrac3Num > 0 || atrac3plusNum > 0) {
    		result = SceKernelErrors.ERROR_BUSY;
    	} else {
    		if (at3IDNum == 0 && at3plusIDNum == 0) {
    			// Both parameters set to 0 reschedule the current thread)
    			delay = true;
    		}

    		if (at3plusIDNum > MAX_ATRAC3PLUS_IDS) {
	    		// Can't create more than 3 AT3+ IDs
	    		at3plusIDNum = MAX_ATRAC3PLUS_IDS;
	    		result = SceKernelErrors.ERROR_OUT_OF_MEMORY;
	    	} else if (at3plusIDNum < 0) {
	    		at3plusIDNum = 0;
	    	}

	    	if (at3plusIDNum * 2 + at3IDNum > MAX_ATRAC3_IDS) {
	    		// Can't create more than 6 AT3 IDs (where each AT3+ ID takes 2 AT3 IDs)
	    		at3IDNum = MAX_ATRAC3_IDS - at3plusIDNum * 2;
	    		result = SceKernelErrors.ERROR_OUT_OF_MEMORY;
	    	} else if (at3IDNum < 0) {
	    		at3IDNum = 0;
	    	}

	    	hleAtracReinit(at3IDNum, at3plusIDNum);
    	}

    	if (delay) {
    		Modules.ThreadManForUserModule.hleYieldCurrentThread();
    	}

    	return result;
    }

    @HLEFunction(nid = 0x2DD3E298, version = 250, checkInsideInterrupt = true)
    public int sceAtracGetBufferInfoForResetting(@CheckArgument("checkAtracID") int atID, int sample, TPointer32 bufferInfoAddr) {
        AtracID id = atracIDs.get(atID);
        return id.getBufferInfoForResetting(sample, bufferInfoAddr);
    }

    @HLEFunction(nid = 0x5CF9D852, version = 250, checkInsideInterrupt = true)
    public int sceAtracSetMOutHalfwayBuffer(@CheckArgument("checkAtracID") int atID, TPointer MOutHalfBuffer, int readSize, int MOutHalfBufferSize) {
    	return hleSetHalfwayBuffer(atID, MOutHalfBuffer, readSize, MOutHalfBufferSize, true, true, 0);
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

    @HLEUnimplemented
    @HLEFunction(nid = 0x5622B7C1, version = 250, checkInsideInterrupt = true)
    public int sceAtracSetAA3DataAndGetID(TPointer buffer, int bufferSize, int fileSize, int unused) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceAtracSetAA3DataAndGetID buffer:%s", Utilities.getMemoryDump(buffer.getAddress(), bufferSize)));
    	}
    	return hleSetHalfwayBufferAndGetID(buffer, bufferSize, bufferSize, false, parseAA3(buffer), fileSize);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5DD66588, version = 250)
    public int sceAtracSetAA3HalfwayBufferAndGetID(TPointer buffer, int readSize, int bufferSize, int fileSize, int unused) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceAtracSetAA3HalfwayBufferAndGetID buffer:%s", Utilities.getMemoryDump(buffer.getAddress(), readSize)));
    	}
    	return hleSetHalfwayBufferAndGetID(buffer, readSize, bufferSize, false, parseAA3(buffer), fileSize);
    }
}