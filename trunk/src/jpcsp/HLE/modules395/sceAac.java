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
package jpcsp.HLE.modules395;

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_AAC_DECODING_ERROR;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_AAC_INVALID_ADDRESS;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_AAC_INVALID_ID;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_AAC_INVALID_PARAMETER;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_AAC_RESOURCE_NOT_INITIALIZED;
import static jpcsp.HLE.modules150.sceAudiocodec.PSP_CODEC_AAC;

import org.apache.log4j.Logger;

import jpcsp.Memory;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.CheckArgument;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.pspFileBuffer;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.sceAtrac3plus;
import jpcsp.HLE.modules150.SysMemUserForUser.SysMemInfo;
import jpcsp.media.codec.CodecFactory;
import jpcsp.media.codec.ICodec;
import jpcsp.util.Utilities;

@HLELogging
public class sceAac extends HLEModule {
    public static Logger log = Modules.getLogger("sceAac");
    protected SysMemInfo resourceMem;
    protected AacInfo[] ids;

    protected static class AacInfo {
    	// The PSP is always reserving this size at the beginning of the input buffer
    	private static final int reservedBufferSize = 1600;
    	private static final int minimumInputBufferSize = reservedBufferSize;
        private boolean init;
        private pspFileBuffer inputBuffer;
        private int bufferAddr;
        private int outputAddr;
        private int outputSize;
        private int sumDecodedSamples;
        private ICodec codec;
        private int halfBufferSize;
        private int outputIndex;
        private int loopNum;
        private int startPos;

        public boolean isInit() {
            return init;
        }

        public void init(int bufferAddr, int bufferSize, int outputAddr, int outputSize, long startPos, long endPos) {
            init = true;
            this.bufferAddr = bufferAddr;
            this.outputAddr = outputAddr;
            this.outputSize = outputSize;
            this.startPos = (int) startPos;
            inputBuffer = new pspFileBuffer(bufferAddr + reservedBufferSize, bufferSize - reservedBufferSize, 0, this.startPos);
            inputBuffer.setFileMaxSize((int) endPos);
            loopNum = -1; // Looping indefinitely by default
            codec = CodecFactory.getCodec(PSP_CODEC_AAC);
            codec.init(0, 2, 2, 0); // TODO How to find out correct parameter values?

            halfBufferSize = (bufferSize - reservedBufferSize) >> 1;
        }

        public void exit() {
            init = false;
        }

        public int notifyAddStream(int bytesToAdd) {
            if (log.isTraceEnabled()) {
                log.trace(String.format("notifyAddStream: %s", Utilities.getMemoryDump(inputBuffer.getWriteAddr(), bytesToAdd)));
            }

            inputBuffer.notifyWrite(bytesToAdd);

            return 0;
        }

        public pspFileBuffer getInputBuffer() {
            return inputBuffer;
        }

        public boolean isStreamDataNeeded() {
        	int writeSize = inputBuffer.getWriteSize();

        	if (writeSize <= 0) {
        		return false;
        	}

        	if (writeSize >= halfBufferSize) {
            	return true;
            }

            if (writeSize >= inputBuffer.getFileWriteSize()) {
            	return true;
            }

            return false;
        }

        public int getSumDecodedSamples() {
            return sumDecodedSamples;
        }

        public int decode(TPointer32 outputBufferAddress) {
        	int result;
        	int decodeOutputAddr = outputAddr + outputIndex;
        	if (inputBuffer.isFileEnd() && inputBuffer.getCurrentSize() <= 0) {
        		int outputBytes = codec.getNumberOfSamples() * 4;
        		Memory mem = Memory.getInstance();
        		mem.memset(decodeOutputAddr, (byte) 0, outputBytes);
        		result = outputBytes;
        	} else {
	        	int decodeInputAddr = inputBuffer.getReadAddr();
	        	int decodeInputLength = inputBuffer.getReadSize();

	        	// Reaching the end of the input buffer (wrapping to its beginning)?
	        	if (decodeInputLength < minimumInputBufferSize && decodeInputLength < inputBuffer.getCurrentSize()) {
	        		// Concatenate the input into a temporary buffer
	        		Memory mem = Memory.getInstance();
	        		mem.memcpy(bufferAddr, decodeInputAddr, decodeInputLength);
	        		int wrapLength = Math.min(inputBuffer.getCurrentSize(), minimumInputBufferSize) - decodeInputLength;
	        		mem.memcpy(bufferAddr + decodeInputLength, inputBuffer.getAddr(), wrapLength);

	        		decodeInputAddr = bufferAddr;
	        		decodeInputLength += wrapLength;
	        	}

	        	if (log.isDebugEnabled()) {
	            	log.debug(String.format("Decoding from 0x%08X, length=0x%X to 0x%08X", decodeInputAddr, decodeInputLength, decodeOutputAddr));
	            }

	        	result = codec.decode(decodeInputAddr, decodeInputLength, decodeOutputAddr);

	            if (result < 0) {
	            	result = ERROR_AAC_DECODING_ERROR;
	            } else {
		            int readSize = result;
		            int samples = codec.getNumberOfSamples();
		            int outputBytes = samples * 4;

		            inputBuffer.notifyRead(readSize);

		            sumDecodedSamples += samples;

		            // Update index in output buffer for next decode()
		            outputIndex += outputBytes;
		            if (outputIndex + outputBytes > outputSize) {
		            	// No space enough to store the same amount of output bytes,
		            	// reset to beginning of output buffer
		            	outputIndex = 0;
		            }

		            result = outputBytes;
	            }

	            if (inputBuffer.getCurrentSize() < minimumInputBufferSize && inputBuffer.isFileEnd() && loopNum != 0) {
	            	if (log.isDebugEnabled()) {
	            		log.debug(String.format("Looping loopNum=%d", loopNum));
	            	}

	            	if (loopNum > 0) {
	            		loopNum--;
	            	}

	            	resetPlayPosition();
	            }
        	}

            outputBufferAddress.setValue(decodeOutputAddr);

            return result;
        }

        public int getWritableBytes() {
        	int writeSize = inputBuffer.getWriteSize();

        	if (writeSize >= 2 * halfBufferSize) {
        		return 2 * halfBufferSize;
        	}

        	if (writeSize >= halfBufferSize) {
        		return halfBufferSize;
        	}

        	if (writeSize >= inputBuffer.getFileWriteSize()) {
        		return halfBufferSize;
        	}

        	return 0;
        }

		public int getLoopNum() {
			return loopNum;
		}

		public void setLoopNum(int loopNum) {
			this.loopNum = loopNum;
		}

		public int resetPlayPosition() {
			inputBuffer.reset(0, startPos);
			sumDecodedSamples = 0;

			return 0;
		}
    }

    @Override
    public String getName() {
        return "sceAac";
    }

    @Override
    public void start() {
        ids = null;

        super.start();
    }

    public int checkId(int id) {
        if (ids == null || ids.length == 0) {
            throw new SceKernelErrorException(ERROR_AAC_RESOURCE_NOT_INITIALIZED);
        }
        if (id < 0 || id >= ids.length) {
            throw new SceKernelErrorException(ERROR_AAC_INVALID_ID);
        }

        return id;
    }

    public int checkInitId(int id) {
        id = checkId(id);
        if (!ids[id].isInit()) {
            throw new SceKernelErrorException(SceKernelErrors.ERROR_AAC_ID_NOT_INITIALIZED);
        }

        return id;
    }

    protected AacInfo getAacInfo(int id) {
        return ids[id];
    }

    @HLEFunction(nid = 0xE0C89ACA, version = 395)
    public int sceAacInit(@CanBeNull TPointer parameters, int unknown1, int unknown2, int unknown3) {
        if (parameters.isNull()) {
            return ERROR_AAC_INVALID_ADDRESS;
        }

        long startPos = parameters.getValue64(0);   // Audio data frame start position.
        long endPos = parameters.getValue64(8);     // Audio data frame end position.
        int bufferAddr = parameters.getValue32(16); // Input AAC data buffer.
        int bufferSize = parameters.getValue32(20); // Input AAC data buffer size.
        int outputAddr = parameters.getValue32(24); // Output PCM data buffer.
        int outputSize = parameters.getValue32(28); // Output PCM data buffer size.
        int freq = parameters.getValue32(32);       // Frequency.
        int reserved = parameters.getValue32(36);   // Always null.

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAacInit parameters: startPos=0x%X, endPos=0x%X, "
                    + "bufferAddr=0x%08X, bufferSize=0x%X, outputAddr=0x%08X, outputSize=0x%X, freq=%d, reserved=0x%08X",
                    startPos, endPos, bufferAddr, bufferSize, outputAddr, outputSize, freq, reserved));
        }

        if (bufferAddr == 0 || outputAddr == 0) {
            return ERROR_AAC_INVALID_ADDRESS;
        }
        if (startPos < 0 || startPos > endPos) {
            return ERROR_AAC_INVALID_PARAMETER;
        }
        if (bufferSize < 8192 || outputSize < 8192 || reserved != 0) {
            return ERROR_AAC_INVALID_PARAMETER;
        }
        if (freq != 44100 && freq != 32000 && freq != 48000 && freq != 24000) {
            return ERROR_AAC_INVALID_PARAMETER;
        }

        int id = -1;
        for (int i = 0; i < ids.length; i++) {
            if (!ids[i].isInit()) {
                id = i;
                break;
            }
        }
        if (id < 0) {
            return SceKernelErrors.ERROR_AAC_NO_MORE_FREE_ID;
        }

        ids[id].init(bufferAddr, bufferSize, outputAddr, outputSize, startPos, endPos);

        return id;
    }

    @HLEFunction(nid = 0x33B8C009, version = 395)
    public int sceAacExit(@CheckArgument("checkId") int id) {
        getAacInfo(id).exit();

        return 0;
    }

    @HLEFunction(nid = 0x5CFFC57C, version = 395)
    public int sceAacInitResource(int numberIds) {
        int memSize = numberIds * 0x19000;
        resourceMem = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.USER_PARTITION_ID, "SceLibAacResource", SysMemUserForUser.PSP_SMEM_Low, memSize, 0);

        if (resourceMem == null) {
            return SceKernelErrors.ERROR_AAC_NOT_ENOUGH_MEMORY;
        }

        Memory.getInstance().memset(resourceMem.addr, (byte) 0, memSize);

        ids = new AacInfo[numberIds];
        for (int i = 0; i < numberIds; i++) {
            ids[i] = new AacInfo();
        }

        return 0;
    }

    @HLEFunction(nid = 0x23D35CAE, version = 395)
    public int sceAacTermResource() {
        if (resourceMem != null) {
            Modules.SysMemUserForUserModule.free(resourceMem);
            resourceMem = null;
        }

        return 0;
    }

    @HLEFunction(nid = 0x7E4CFEE4, version = 395)
    public int sceAacDecode(@CheckArgument("checkInitId") int id, @CanBeNull TPointer32 bufferAddress) {
        int result = getAacInfo(id).decode(bufferAddress);

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAacDecode bufferAddress=%s(0x%08X) returning 0x%X", bufferAddress, bufferAddress.getValue(), result));
        }

        if (result >= 0) {
            Modules.ThreadManForUserModule.hleKernelDelayThread(sceAtrac3plus.atracDecodeDelay, false);
        }

        return result;
    }

    @HLEFunction(nid = 0x523347D9, version = 395)
    public int sceAacGetLoopNum(@CheckArgument("checkInitId") int id) {
        return getAacInfo(id).getLoopNum();
    }

    @HLEFunction(nid = 0xBBDD6403, version = 395)
    public int sceAacSetLoopNum(@CheckArgument("checkInitId") int id, int loopNum) {
        getAacInfo(id).setLoopNum(loopNum);
        return 0;
    }

    @HLEFunction(nid = 0xD7C51541, version = 395)
    public boolean sceAacCheckStreamDataNeeded(@CheckArgument("checkInitId") int id) {
        return getAacInfo(id).isStreamDataNeeded();
    }

    @HLEFunction(nid = 0xAC6DCBE3, version = 395)
    public int sceAacNotifyAddStreamData(@CheckArgument("checkInitId") int id, int bytesToAdd) {
        return getAacInfo(id).notifyAddStream(bytesToAdd);
    }

    @HLEFunction(nid = 0x02098C69, version = 395)
    public int sceAacGetInfoToAddStreamData(@CheckArgument("checkInitId") int id, @CanBeNull TPointer32 writeAddr, @CanBeNull TPointer32 writableBytesAddr, @CanBeNull TPointer32 readOffsetAddr) {
        AacInfo info = getAacInfo(id);
        writeAddr.setValue(info.getInputBuffer().getWriteAddr());
        writableBytesAddr.setValue(info.getWritableBytes());
        readOffsetAddr.setValue(info.getInputBuffer().getFilePosition());

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceAacGetInfoToAddStreamData returning writeAddr=0x%08X, writableBytes=0x%X, readOffset=0x%X", writeAddr.getValue(), writableBytesAddr.getValue(), readOffsetAddr.getValue()));
        }
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6DC7758A, version = 395)
    public int sceAacGetMaxOutputSample(@CheckArgument("checkInitId") int id) {
        return 0;
    }

    @HLEFunction(nid = 0x506BF66C, version = 395)
    public int sceAacGetSumDecodedSample(@CheckArgument("checkInitId") int id) {
        int sumDecodedSamples = getAacInfo(id).getSumDecodedSamples();
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAacGetSumDecodedSample returning 0x%X", sumDecodedSamples));
        }

        return sumDecodedSamples;
    }

    @HLEFunction(nid = 0xD2DA2BBA, version = 395)
    public int sceAacResetPlayPosition(@CheckArgument("checkInitId") int id) {
    	return getAacInfo(id).resetPlayPosition();
    }
}
