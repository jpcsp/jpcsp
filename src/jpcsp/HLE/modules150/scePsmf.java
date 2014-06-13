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

import jpcsp.HLE.CheckArgument;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;

import java.util.HashMap;

import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules150.sceMpeg.PSMFEntry;
import jpcsp.HLE.modules150.sceMpeg.PSMFHeader;
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
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("checkPsmfWithEPMap returning 0x%08X(ERROR_PSMF_NOT_FOUND)", SceKernelErrors.ERROR_PSMF_NOT_FOUND));
        	}
    		throw new SceKernelErrorException(SceKernelErrors.ERROR_PSMF_NOT_FOUND);
    	}

    	return psmf;
    }

    private PSMFHeader getPsmfHeader(TPointer32 psmf) {
		int headerAddress = psmf.getValue(24);
		return psmfHeaderMap.get(headerAddress);
    }

    @HLELogging(level="info")
    @HLEFunction(nid = 0xC22C8327, version = 150, checkInsideInterrupt = true)
    public int scePsmfSetPsmf(TPointer32 psmf, TPointer bufferAddr) {
        Modules.sceMpegModule.setCurrentMpegAnalyzed(false);
        Modules.sceMpegModule.analyseMpeg(bufferAddr.getAddress());
        PSMFHeader header = Modules.sceMpegModule.psmfHeader;
        psmfHeaderMap.put(bufferAddr.getAddress(), header);

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
        psmf.setValue(24, bufferAddr.getAddress());         // Pointer to PSMF header.
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
        	// Do not return SceKernelErrors.ERROR_PSMF_INVALID_ID, but set an invalid stream number.
        	header.setStreamNum(-1);
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
        if (!header.isValidCurrentStreamNumber()) {
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("scePsmfGetVideoInfo returning 0x%08X(ERROR_PSMF_INVALID_ID)", SceKernelErrors.ERROR_PSMF_INVALID_ID));
        	}
        	return SceKernelErrors.ERROR_PSMF_INVALID_ID;
        }
        videoInfoAddr.setValue(0, header.getVideoWidth());
        videoInfoAddr.setValue(4, header.getVideoHeigth());

        return 0;
    }

    @HLEFunction(nid = 0xA83F7113, version = 150, checkInsideInterrupt = true)
    public int scePsmfGetAudioInfo(@CheckArgument("checkPsmf") TPointer32 psmf, TPointer32 audioInfoAddr) {
        PSMFHeader header = getPsmfHeader(psmf);
        if (!header.isValidCurrentStreamNumber()) {
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("scePsmfGetAudioInfo returning 0x%08X(ERROR_PSMF_INVALID_ID)", SceKernelErrors.ERROR_PSMF_INVALID_ID));
        	}
        	return SceKernelErrors.ERROR_PSMF_INVALID_ID;
        }
        audioInfoAddr.setValue(0, header.getAudioChannelConfig());
        audioInfoAddr.setValue(4, header.getAudioSampleFrequency());

        return 0;
    }

    @HLEFunction(nid = 0x971A3A90, version = 150, checkInsideInterrupt = true)
    public int scePsmfCheckEPmap(@CheckArgument("checkPsmfWithEPMap") TPointer32 psmf) {
    	// checkPsmfWithEPMap is already returning the correct error code if no EPmap is present
        return 0;
    }

    @HLEFunction(nid = 0x4E624A34, version = 150, checkInsideInterrupt = true)
    public int scePsmfGetEPWithId(@CheckArgument("checkPsmfWithEPMap") TPointer32 psmf, int id, TPointer32 outAddr) {
        PSMFHeader header = getPsmfHeader(psmf);
        PSMFEntry entry = header.getEPMapEntry(id);
    	if (entry == null) {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("scePsmfGetEPWithId returning 0x%08X(ERROR_PSMF_INVALID_ID)", SceKernelErrors.ERROR_PSMF_INVALID_ID));
    		}
    		return SceKernelErrors.ERROR_PSMF_INVALID_ID;
    	}

        if (log.isDebugEnabled()) {
        	log.debug(String.format("scePsmfGetEPWithId returning %s", entry));
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
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("scePsmfGetEPWithTimestamp returning 0x%08X(ERROR_PSMF_INVALID_TIMESTAMP)", SceKernelErrors.ERROR_PSMF_INVALID_TIMESTAMP));
    		}
            return SceKernelErrors.ERROR_PSMF_INVALID_TIMESTAMP;
    	}

    	PSMFEntry entry = header.getEPMapEntryWithTimestamp(ts);
        if (entry == null) {
        	// Unknown error code
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("scePsmfGetEPWithTimestamp returning -1"));
    		}
        	return -1;
        }

        if (log.isDebugEnabled()) {
        	log.debug(String.format("scePsmfGetEPWithTimestamp returning %s", entry));
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
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("scePsmfGetEPidWithTimestamp returning 0x%08X(ERROR_PSMF_INVALID_TIMESTAMP)", SceKernelErrors.ERROR_PSMF_INVALID_TIMESTAMP));
    		}
            return SceKernelErrors.ERROR_PSMF_INVALID_TIMESTAMP;
    	}

        PSMFEntry entry = header.getEPMapEntryWithTimestamp(ts);
        if (entry == null) {
        	// Unknown error code
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("scePsmfGetEPidWithTimestamp returning -1"));
    		}
            return -1;
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("scePsmfGetEPidWithTimestamp returning id 0x%X", entry.getId()));
        }

        return entry.getId();
	}

    @HLEFunction(nid = 0x5B70FCC1, version = 150, checkInsideInterrupt = true)
    public int scePsmfQueryStreamOffset(TPointer bufferAddr, TPointer32 offsetAddr) {
        // Always let sceMpeg handle the PSMF analysis.
        Modules.sceMpegModule.analyseMpeg(bufferAddr.getAddress(), null);

        offsetAddr.setValue(Modules.sceMpegModule.psmfHeader.mpegOffset);

        return 0;
    }

    @HLEFunction(nid = 0x9553CC91, version = 150, checkInsideInterrupt = true)
    public int scePsmfQueryStreamSize(TPointer bufferAddr, TPointer32 sizeAddr) {
        // Always let sceMpeg handle the PSMF analysis.
        Modules.sceMpegModule.analyseMpeg(bufferAddr.getAddress(), null);

        sizeAddr.setValue(Modules.sceMpegModule.psmfHeader.mpegStreamSize);

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
            if (log.isDebugEnabled()) {
                log.debug(String.format("scePsmfSpecifyStreamWithStreamTypeNumber returning 0x%08X(ERROR_PSMF_INVALID_ID)", SceKernelErrors.ERROR_PSMF_INVALID_ID));
            }
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
            if (log.isDebugEnabled()) {
                log.debug(String.format("scePsmfVerifyPsmf returning 0x%08X(ERROR_PSMF_INVALID_PSMF)", SceKernelErrors.ERROR_PSMF_INVALID_PSMF));
            }
        	return SceKernelErrors.ERROR_PSMF_INVALID_PSMF;
        }

        int rawVersion = bufferAddr.getValue32(sceMpeg.PSMF_STREAM_VERSION_OFFSET);
        int version = sceMpeg.getMpegVersion(rawVersion);
        if (version < 0) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("scePsmfVerifyPsmf returning 0x%08X(ERROR_PSMF_INVALID_PSMF)", SceKernelErrors.ERROR_PSMF_INVALID_PSMF));
            }
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

        // Convert the header version into a number, e.g. "0015" -> 15
        int headerVersion = header.getVersion();
        int version = 0;
        for (int i = 0; i < 4; i++, headerVersion >>= 8) {
        	int digit = headerVersion & 0x0F;
        	version = (version * 10) + digit;
        }

        if (log.isDebugEnabled()) {
        	log.debug(String.format("scePsmfGetPsmfVersion returning version=%d (headerVersion=0x%08X)", version, headerVersion));
        }

        return version;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xDE78E9FC, version = 150)
    public int scePsmf_DE78E9FC(@CheckArgument("checkPsmf") TPointer32 psmf, int unknown) {
    	// Get number of Psmf Marks
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x43AC7DBB, version = 150)
    public int scePsmf_43AC7DBB(@CheckArgument("checkPsmf") TPointer32 psmf, int unknown, int markNumber, TPointer markInfoAddr) {
    	// Get Psmf Mark Information
    	int markType = 0;
    	int markTimestamp = 0;
    	int markEntryEsStream = 0;
    	int markData = 0;
    	String markName = "Test";
    	markInfoAddr.setValue32(0, markType);
    	markInfoAddr.setValue32(4, markTimestamp);
    	markInfoAddr.setValue32(8, markEntryEsStream);
    	markInfoAddr.setValue32(12, markData);
    	markInfoAddr.setValue32(16, markName.length());
    	markInfoAddr.setStringNZ(20, markName.length(), markName);

    	return 0;
    }
}