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

import static jpcsp.HLE.modules150.IoFileMgrForUser.PSP_SEEK_END;
import static jpcsp.HLE.modules150.IoFileMgrForUser.PSP_SEEK_SET;
import jpcsp.Emulator;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.sceMpeg;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

@HLELogging
public class sceMp4 extends HLEModule {
    public static Logger log = Modules.getLogger("sceMp4");
    protected int callbackParam;
	protected int callbackGetCurrentPosition;
	protected int callbackSeek;
	protected int callbackRead;
	protected AfterReadAction afterReadAction = new AfterReadAction();
	protected int readBufferAddr;
	protected int readBufferSize;
	protected int readSize;
	protected boolean fileReadingCompleted;
	protected boolean fileReadingInProgress;
	private static final int audioFirstTimestamp = 0;
	private static final int videoFirstTimestamp = 0;
	private static final int audioBufferSize = 4096;

	protected class FileReadingAction implements IAction {
		private int currentPosition = -1;
		private int fileSize = -1;
		private boolean reading = false;
		private int recursion = 0;

		@Override
		public void execute() {
			recursion++;
			//
			// Execute the MP4 callbacks in the following sequence:
			//
			//   1. currentPosition = callbackGetCurrentPosition();
			//   2. fileSize = callbackSeek(0, PSP_SEEK_END);
			//   3. callbackSeek(currentPosition, PSP_SEEK_SET);
			//   4. while (readSize < fileSize) {
			//        callbackRead(readBufferAddr, readBufferSize);
			//      }
			//
			int callbackReturnValue = Emulator.getProcessor().cpu._v0;
			if (currentPosition < 0) {
				currentPosition = callbackReturnValue;
				if (log.isDebugEnabled()) {
					log.debug(String.format("After GetCurrentPosition currentPosition=0x%X", currentPosition));
				}
				callSeekCallback(this, 0, PSP_SEEK_END);
			} else if (fileSize < 0) {
				fileSize = callbackReturnValue;
				if (log.isDebugEnabled()) {
					log.debug(String.format("After SeekEnd fileSize=0x%X, currentPosition=0x%X", fileSize, currentPosition));
				}
				callSeekCallback(this, currentPosition, PSP_SEEK_SET);
			} else if (!reading) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("Starting Reading readSize=0x%X, fileSize=0x%X", readSize, fileSize));
				}
				reading = true;
				callReadCallback(this, readBufferAddr, readBufferSize);
			} else {
				hleAfterReadCallback();
				if (callbackReturnValue > 0 && readSize < fileSize) {
					if (log.isDebugEnabled()) {
						log.debug(String.format("Continue Reading readSize=0x%X, fileSize=0x%X", readSize, fileSize));
					}
					if (recursion > 1000) {
						recursion = 0;
						// This exception will be catch in readFile().
						throw new StackOverflowError();
					}
					callReadCallback(this, readBufferAddr, readBufferSize);
				} else {
					if (log.isDebugEnabled()) {
						log.debug(String.format("Completed Reading readSize=0x%X, fileSize=0x%X", readSize, fileSize));
					}
					Modules.IoFileMgrForUserModule.hleSetNoDelayIoOperation(false);
					fileReadingCompleted = true;
					fileReadingInProgress = false;
					initMpegModule(fileSize);
				}
			}
		}
	}

	protected class AfterReadAction implements IAction {
		@Override
		public void execute() {
			hleAfterReadCallback();
		}
	}

	protected void hleAfterReadCallback() {
        CpuState cpu = Emulator.getProcessor().cpu;

        int bytesRead = cpu._v0;

        if (bytesRead > 0) {
        	readSize += bytesRead;
        	Modules.sceMpegModule.hleAddVideoData(readBufferAddr, bytesRead);

	        if (log.isDebugEnabled()) {
	        	log.debug(String.format("hleAfterReadCallback bytesRead=0x%X, totalRead=0x%X", bytesRead, readSize));
		        if (log.isTraceEnabled()) {
		        	log.trace(String.format("hleAfterReadCallback: %s", Utilities.getMemoryDump(readBufferAddr, bytesRead)));
		        }
	        }
        }
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

	protected void callSeekCallback(IAction afterAction, int offset, int whence) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("callSeekCallback offset=0x%X, whence=%s", offset, IoFileMgrForUser.getWhenceName(whence)));
		}
		Modules.ThreadManForUserModule.executeCallback(null, callbackSeek, afterAction, false, callbackParam, 0, offset, 0, whence);
	}

	protected void initMpegModule(int mp4FileSize) {
		sceMpeg sceMpegModule = Modules.sceMpegModule;
		sceMpegModule.hleSetTotalStreamSize(mp4FileSize);
		sceMpegModule.hleSetChannelBufferLength(mp4FileSize);
		sceMpegModule.hleCreateRingbuffer();
	}

	protected int readFile() {
    	if (!fileReadingCompleted) {
    		if (!fileReadingInProgress) {
    			// Read the complete MP4 file into the PacketChannel.
    			// Xuggle/ffmpeg requires the whole MP4 stream to be available
    			// at the beginning of the decoding.
    			// It is parsing something at the very end of the file while opening the MP4 stream.
    			fileReadingInProgress = true;
    			// Read as quickly as possible, do not delay the IO operations.
    			Modules.IoFileMgrForUserModule.hleSetNoDelayIoOperation(true);
    			// Start reading by calling the GetCurrentPosition callback
    			FileReadingAction fileReadingAction = new FileReadingAction();

    			boolean firstCall = true;
    			while (fileReadingInProgress) {
    				try {
    					if (firstCall) {
        					callGetCurrentPositionCallback(fileReadingAction);
    					} else {
    						callReadCallback(fileReadingAction, readBufferAddr, readBufferSize);
    					}
    				} catch (StackOverflowError stackOverflowError) {
    					// If the MP4 file is very large, we could get a StackOverflowError
    					// as the callbacks are called recursively.
    					// Just ignore this error and continue reading...
    					firstCall = false;
    					if (log.isDebugEnabled()) {
    						log.debug(String.format("readFile catching %s - can be ignored", stackOverflowError));
    					}
    				}
    			}
    		}
    	}

    	return 0;
	}

	@Override
    public String getName() {
        return "sceMp4";
    }

	protected void hleMp4Init() {
		readBufferAddr = 0;
		readBufferSize = 0;
		readSize = 0;
		fileReadingInProgress = false;
		fileReadingCompleted = false;
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
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB1221EE7, version = 150, checkInsideInterrupt = true)
    public int sceMp4Create(int mp4, int unknown2, TPointer readBufferAddr, int readBufferSize) {
    	this.readBufferAddr = readBufferAddr.getAddress();
    	this.readBufferSize = readBufferSize;

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x538C2057, version = 150)
    public int sceMp4Delete() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x113E9E7B, version = 150)
    public int sceMp4_113E9E7B(int unknown) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7443AF1D, version = 150)
    public int sceMp4_7443AF1D(int mp4, TPointer unknown2) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5EB65F26, version = 150)
    public int sceMp4_5EB65F26(int unknown1, int unknown2) {
        // Application expects return value > 0
        return 1;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7ADFD01C, version = 150)
    public int sceMp4_7ADFD01C(int mp4, int unknown2, int unknown3, TPointer32 callbacks, int unknown5) {
    	callbackParam = callbacks.getValue(0);
    	callbackGetCurrentPosition = callbacks.getValue(4);
    	callbackSeek = callbacks.getValue(8);
    	callbackRead = callbacks.getValue(12);
    	log.warn(String.format("sceMp4_7ADFD01C callbacks: param=0x%08X, getCurrentPosition=0x%08X, seek=0x%08X, read=0x%08X", callbackParam, callbackGetCurrentPosition, callbackSeek, callbackRead));

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xBCA9389C, version = 150)
    public int sceMp4_BCA9389C(int unknown1, int unknown2, int unknown3, int unknown4, int unknown5) {
        int value = Math.max(unknown2 * unknown3, unknown4 << 1) + (unknown2 << 6) + unknown5 + 256;
        log.warn(String.format("sceMp4_BCA9389C returning 0x%X", value));

        return value;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9C8F4FC1, version = 150)
    public int sceMp4_9C8F4FC1(int mp4, int unknown2, int unknown3, int unknown4, int unknown5, int unknown6, int unknown7, int unknown8) {
    	// unknown4 == value returned by sceMp4_BCA9389C
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0F0187D2, version = 150)
    public int sceMp4_0F0187D2(int unknown1, int unknown2, int unknown3) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9CE6F5CF, version = 150)
    public int sceMp4_9CE6F5CF(int unknown1, int unknown2, @CanBeNull TPointer32 resultAddr) {
    	// Returning 5 32-bit values in resultAddr
    	resultAddr.setValue(0, 0);
    	resultAddr.setValue(4, 0);
    	resultAddr.setValue(8, 0);
    	resultAddr.setValue(12, 44100); // Frequency
    	resultAddr.setValue(16, 0);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4ED4AB1E, version = 150)
    public int sceMp4_4ED4AB1E(int unknown) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x10EE0D2C, version = 150)
    public int sceMp4_10EE0D2C(int unknown) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x496E8A65, version = 150)
    public int sceMp4_496E8A65(int unknown1, int unknown2) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB4B400D1, version = 150)
    public int sceMp4_B4B400D1(int unknown1, int unknown2) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF7C51EC1, version = 150)
    public int sceMp4_F7C51EC1(int unknown1, int unknown2, int unknown3, TPointer unknown4) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x74A1CA3E, version = 150)
    public int sceMp4_74A1CA3E(int unknown1, int unknown2, int unknown3, int unknown4) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD8250B75, version = 150)
    public int sceMp4_D8250B75(int unknown1, int unknown2, int unknown3) {
        return 0;
    }

    /**
     * Similar to sceMpegRingbufferAvailableSize.
     * 
     * @param mp4
     * @param unknown2
     * @param writableBytesAddr
     * @param unknown4
     * @return
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x8754ECB8, version = 150)
    public int sceMp4_8754ECB8(int mp4, int unknown2, @CanBeNull TPointer32 writableBytesAddr, @CanBeNull TPointer32 unknown4) {
    	if (fileReadingInProgress || fileReadingCompleted) {
    		// No need to read more if the complete file has been read (or is in reading).
    		writableBytesAddr.setValue(0);
    		unknown4.setValue(0);
    	} else {
    		writableBytesAddr.setValue(readBufferSize);
    		unknown4.setValue(1);
    	}

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceMp4_8754ECB8 returning writableBytes=0x%X, unknown4=0x%X", writableBytesAddr.getValue(), unknown4.getValue()));
    	}
    	return 0;
    }

    /**
     * Similar to sceMpegRingbufferPut.
     * 
     * @param mp4
     * @param unknown2
     * @param writableBytes
     * @return
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x31BCD7E0, version = 150)
    public int sceMp4_31BCD7E0(int mp4, int unknown2, int writableBytes) {
    	writableBytes = Math.min(writableBytes, readBufferSize);
    	callReadCallback(afterReadAction, readBufferAddr, writableBytes);

    	return 0;
    }

    /**
     * Similar to sceMpegGetAtracAu.
     * 
     * @param mp4
     * @param unknown2
     * @param auAddr
     * @param unknown4
     * @return
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x5601A6F0, version = 150)
    public int sceMp4_5601A6F0(int mp4, int unknown2, TPointer auAddr, TPointer unknown4) {
    	if (!fileReadingCompleted) {
    		return readFile();
    	}

    	// unknown4: pointer to a 40-bytes structure
        return Modules.sceMpegModule.hleMpegGetAtracAu(auAddr, audioFirstTimestamp);
    }

    /**
     * Similar to sceMpegAtracDecode.
     * 
     * @param unknown1
     * @param auAddr
     * @param outputBufferAddr
     * @param init
     * @param frequency
     * @return
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x7663CB5C, version = 150)
    public int sceMp4_7663CB5C(int unknown1, TPointer auAddr, TPointer bufferAddr, int init, int frequency) {
    	// Decode audio:
    	// - init: 1 at first call, 0 afterwards
    	// - frequency: 44100
    	return Modules.sceMpegModule.hleMpegAtracDecode(bufferAddr, audioBufferSize);
    }

    /**
     * Similar to sceMpegGetAvcAu.
     * Video decoding is done by sceMpegAvcDecode.
     * 
     * @param mp4
     * @param unknown2
     * @param auAddr
     * @param unknown4
     * @return
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x503A3CBA, version = 150)
    public int sceMp4_503A3CBA(int mp4, int unknown2, TPointer auAddr, TPointer unknown4) {
    	// unknown4: pointer to a 40-bytes structure
    	if (!fileReadingCompleted) {
    		return readFile();
    	}

    	return Modules.sceMpegModule.hleMpegGetAvcAu(auAddr, videoFirstTimestamp, SceKernelErrors.ERROR_MP4_NO_MORE_DATA);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x01C76489, version = 150)
    public int sceMp4_01C76489(int unknown1, int unknown2) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6710FE77, version = 150)
    public int sceMp4_6710FE77(int unknown1, int unknown2) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5D72B333, version = 150)
    public int sceMp4_5D72B333(int unknown) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7D332394, version = 150)
    public int sceMp4_7D332394() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x131BDE57, version = 150)
    public int sceMp4_131BDE57(int mp4, int unknown2, TPointer auAddr) {
    	// unknown2 = return value of sceMpegAvcResourceGetAvcEsBuf()
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