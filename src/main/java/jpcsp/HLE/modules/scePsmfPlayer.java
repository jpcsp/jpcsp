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
package jpcsp.HLE.modules;

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_PSMFPLAYER_NO_MEMORY;
import static jpcsp.HLE.modules.scePsmf.PSMF_ATRAC_STREAM;
import static jpcsp.HLE.modules.scePsmf.PSMF_AUDIO_STREAM;
import static jpcsp.HLE.modules.scePsmf.PSMF_AVC_STREAM;
import static jpcsp.HLE.modules.scePsmf.PSMF_PCM_STREAM;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
import static jpcsp.util.Utilities.allocatePointer;
import static jpcsp.util.Utilities.allocatePointer32;

import java.io.IOException;

import static jpcsp.Allegrex.compiler.RuntimeContext.hleSyscall;
import static jpcsp.HLE.HLEModuleManager.HLESyscallNid;
import static jpcsp.HLE.kernel.managers.EventFlagManager.PSP_EVENT_WAITCLEAR;
import static jpcsp.HLE.kernel.managers.EventFlagManager.PSP_EVENT_WAITOR;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_MPEG_NO_DATA;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_PSMFPLAYER_FATAL;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_PSMFPLAYER_INVALID_CONFIG_MODE;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_PSMFPLAYER_INVALID_CONFIG_VALUE;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_PSMFPLAYER_NOT_INITIALIZED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_PSMFPLAYER_NOT_SUPPORTED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_PSMFPLAYER_NO_DATA;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.CheckArgument;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.PspString;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.kernel.managers.EventFlagManager;
import jpcsp.HLE.kernel.types.SceIoStat;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.modules.IoFileMgrForUser.IoInfo;
import jpcsp.hardware.Screen;
import jpcsp.util.HLEUtilities;
import jpcsp.util.Utilities;

/**
 * HLE implementation of the library libpsmfplayer.prx
 *
 * @author gid15
 *
 */
public class scePsmfPlayer extends HLEModule {
    public static Logger log = Modules.getLogger("scePsmfPlayer");
    private static final int ringbufferSize = 0x131800;
    private static final int frameWidth = 512;
    public static final int videoTimestampStep = 3003; // mpegTimestampPerSecond / 29.970 (fps)
    public static final int audioTimestampStep = 4180; // For audio play at 44100 Hz (2048 samples / 44100 * mpegTimestampPerSecond == 4180)
    // PSMF Player status.
    public static final int PSMF_PLAYER_STATUS_NONE = 0x0;
    public static final int PSMF_PLAYER_STATUS_INIT = 0x1;
    public static final int PSMF_PLAYER_STATUS_STANDBY = 0x2;
    public static final int PSMF_PLAYER_STATUS_PLAYING = 0x4;
    public static final int PSMF_PLAYER_STATUS_ERROR = 0x100;
    public static final int PSMF_PLAYER_STATUS_PLAYING_FINISHED = 0x200;
    // PSMF Player config mode.
    public static final int PSMF_PLAYER_CONFIG_MODE_LOOP = 0;
    public static final int PSMF_PLAYER_CONFIG_MODE_PIXEL_TYPE = 1;
    // PSMF Player loop mode.
    public static final int PSMF_PLAYER_LOOPING = 0;
    public static final int PSMF_PLAYER_NOT_LOOPING = 1;
    // PSMF Player config pixel type.
    public static final int PSMF_PLAYER_PIXEL_TYPE_UNCHANGED = -1;
    // PSMF Player filename type.
    public static final int PSMF_PLAYER_FILENAME_TYPE_HOST0 = 0;
    public static final int PSMF_PLAYER_FILENAME_TYPE_MS0 = 1;
    public static final int PSMF_PLAYER_FILENAME_TYPE_DISC0 = 2;
    public static final int PSMF_PLAYER_FILENAME_TYPE_HTTP = 3;
    public static final int PSMF_PLAYER_FILENAME_TYPE_MS0_ENCRYPTED = 4;
    // PSMF Player version.
    public static final int PSMF_PLAYER_VERSION_FULL = 0;
    public static final int PSMF_PLAYER_VERSION_BASIC = 1;
    public static final int PSMF_PLAYER_VERSION_NET = 2;
    // PSMF Player mode.
    public static final int PSMF_PLAYER_MODE_PLAY = 0;
    public static final int PSMF_PLAYER_MODE_SLOWMOTION = 1;
    public static final int PSMF_PLAYER_MODE_STEPFRAME = 2;
    public static final int PSMF_PLAYER_MODE_PAUSE = 3;
    public static final int PSMF_PLAYER_MODE_FORWARD = 4;
    public static final int PSMF_PLAYER_MODE_REWIND = 5;
    // PSMF Player playback speed.
    public static final int PSMF_PLAYER_SPEED_SLOW = 1;
    public static final int PSMF_PLAYER_SPEED_NORMAL = 2;
    public static final int PSMF_PLAYER_SPEED_FAST = 3;
    // PSMF Player ScePsmfPlayerASCReadyFlag
    public static final int PSMF_PLAYER_ASC_READY_FLAG_PLAY_CONTINUE = 0;
    public static final int PSMF_PLAYER_ASC_READY_FLAG_PAUSE_CONTINUE = 2;
    public static final int PSMF_PLAYER_ASC_READY_FLAG_SLOWMOTION_CONTINUE = 3;
    public static final int PSMF_PLAYER_ASC_READY_FLAG_FORWARD_CONTINUE = 7;
    public static final int PSMF_PLAYER_ASC_READY_FLAG_REWIND_CONTINUE = 8;
    public static final int PSMF_PLAYER_ASC_READY_FLAG_PLAY = 9;
    public static final int PSMF_PLAYER_ASC_READY_FLAG_PAUSE = 10;
    public static final int PSMF_PLAYER_ASC_READY_FLAG_FORWARD_SLOW = 11;
    public static final int PSMF_PLAYER_ASC_READY_FLAG_FORWARD_NORMAL = 12;
    public static final int PSMF_PLAYER_ASC_READY_FLAG_FORWARD_FAST = 13;
    public static final int PSMF_PLAYER_ASC_READY_FLAG_REWIND_SLOW = 14;
    public static final int PSMF_PLAYER_ASC_READY_FLAG_REWIND_NORMAL = 15;
    public static final int PSMF_PLAYER_ASC_READY_FLAG_REWIND_FAST = 16;
    public static final int PSMF_PLAYER_ASC_READY_FLAG_SLOWMOTION = 17;
    // PSMF Player ScePsmfPlayerDecodeStatus
    public static final int PSMF_PLAYER_DECODE_STATUS_START = 1;
    public static final int PSMF_PLAYER_DECODE_STATUS_PLAY = 2;
    public static final int PSMF_PLAYER_DECODE_STATUS_FORWARD = 5;
    public static final int PSMF_PLAYER_DECODE_STATUS_REWIND = 6;
    public static final int PSMF_PLAYER_DECODE_STATUS_START_PLAY = 7;
    public static final int PSMF_PLAYER_DECODE_STATUS_END = 8;
    public static final int PSMF_PLAYER_DECODE_STATUS_EXIT = 9;
    public static final int PSMF_PLAYER_DECODE_STATUS_EXITED = 10;
    private int scePsmfPlayerMCThreadEntry;
    private int scePsmfPlayerOpenControlThreadEntry;
    private int scePsmfPlayerDecodeThreadEntry;
    private int scePsmfPlayerReadThreadEntry;
    private int scePsmfPlayerAbortThreadEntry;
    private int scePsmfPlayerRingbufferPutEntry;
    private int scePsmfPlayerMemoryPool;
    private int scePsmfPlayerMemoryPoolMaxAllocatedSize;
    private int scePsmfPlayerAbortThread;
    private int scePsmfPlayerAbortThreadEvf;
    private boolean abortOpen;

    @Override
	public int getMemoryUsage() {
    	// Memory size used by libpsmfplayer.prx
		return 0xE264;
	}

    /**
     * This class is used in libpsmfplayer.prx to perform atomic operations.
     * It contains 2 entries:
     * - offset 0: a LwMutex
     * - offset 4: a 32 bit value
     * Each change to the value is protected by the LwMutex.
     * 
     * Here, in HLE, we do not need to use the LwMutex as
     * the operations are atomic from the PSP point of view
     * (i.e. no PSP thread switch will happen here).
     */
    private static class AtomicValue {
    	public static void or(TPointer addr, int value) {
    		addr.setValue32(4, addr.getValue32(4) | value);
    	}

    	public static void set(TPointer addr, int value) {
    		addr.setValue32(4, value);
    	}

    	public static void clear(TPointer addr, int value) {
    		addr.setValue32(4, addr.getValue32(4) & ~value);
    	}

    	public static int get(TPointer addr) {
    		return addr.getValue32(4);
    	}
    }

    @HLEFunction(nid = HLESyscallNid, version = 150)
    public void hlePsmfPlayerMCThread(int args, TPointer argp) {
    	TPointer psmfPlayer = argp.getPointer();
    	TPointer data = psmfPlayer.getPointer();
        TPointer playerStatusAddr = data.getPointer(24);
        TPointer decodeControlAddr = data.getPointer(36);
        TPointer avSyncControlAddr = data.getPointer(32);
        TPointer videoBufferAddr = avSyncControlAddr.getPointer(24);
        TPointer audioBufferAddr = avSyncControlAddr.getPointer(28);

        while (true) {
        	while (true) {
	    		hleSyscall(Modules.ThreadManForUserModule.sceKernelWaitEventFlag(playerStatusAddr.getValue32(24), 1, PSP_EVENT_WAITCLEAR | PSP_EVENT_WAITOR, TPointer32.NULL, TPointer32.NULL));
	    		if (data.getValue32(20) == 1) {
	    	    	Modules.ThreadManForUserModule.sceKernelExitThread(0);
	    	    	return;
	    		}

	    		int decodeStatus = AtomicValue.get(decodeControlAddr.getPointer(72)); // ScePsmfPlayerDecodeStatus
		    	if (decodeStatus == PSMF_PLAYER_DECODE_STATUS_END) {
		    		if (avSyncControlAddr.getValue32(20) == 1) {
		    			if (videoBufferAddr.getValue32(16) == 0) {
		    				if (audioBufferAddr.getValue32(12) == 0) {
		    					if (getVblankSyncCount(avSyncControlAddr) == 0) {
		    						break;
		    					}
		    				}
		    			}
		    		} else {
		    			if (videoBufferAddr.getValue32(16) == 0) {
	    					if (getVblankSyncCount(avSyncControlAddr) == 0) {
	    						break;
	    					}
		    			}
		    		}
		    	}
        	}

    		int playMode = playerStatusAddr.getValue32(4);
    		int loopingMode = playerStatusAddr.getValue32(12);
        	if (getPlayerVersion(playerStatusAddr) == PSMF_PLAYER_VERSION_NET || (playMode != PSMF_PLAYER_MODE_REWIND && loopingMode == PSMF_PLAYER_NOT_LOOPING)) {
        		if (getPlayerStatus(playerStatusAddr) == PSMF_PLAYER_STATUS_PLAYING) {
        			setPlayerStatus(playerStatusAddr, PSMF_PLAYER_STATUS_PLAYING_FINISHED);
        		}
        	} else {
        		final int newPlayMode = PSMF_PLAYER_MODE_PLAY;
        		final int newPlaySpeed = PSMF_PLAYER_SPEED_SLOW;
        		playerStatusAddr.setValue32(0, 0);
        		playerStatusAddr.setValue32(4, newPlayMode);
        		playerStatusAddr.setValue32(16, newPlaySpeed);
        		int result = updatePlayModeAndSpeed(avSyncControlAddr, newPlayMode, newPlaySpeed, 0);
        		if (result != 0) {
        			setPlayerStatusError(playerStatusAddr);
        		}
        	}
    	}
    }

    private int getFilenameType(TPointer readControlAddr) {
    	TPointer openControlType = readControlAddr.getPointer(56);
    	int type = openControlType.getValue32(0);

    	return type;
    }

    private int initOpenControl(TPointer data, String filename) {
    	TPointer readControlAddr = data.getPointer(48);
    	readControlAddr.setValue32(64, data.getValue32(68)); // psmf offset
    	TPointer openControlType = readControlAddr.getPointer(56);

    	int type;
    	if (filename.startsWith("host0:")) {
    		type = PSMF_PLAYER_FILENAME_TYPE_HOST0;
    	} else if (filename.startsWith("ms0:")) {
    		type = PSMF_PLAYER_FILENAME_TYPE_MS0;
    		if (filename.endsWith(".edat")) {
    			type = PSMF_PLAYER_FILENAME_TYPE_MS0_ENCRYPTED;
    		}
    	} else if (filename.startsWith("http://") || filename.startsWith("https://")) {
    		type = PSMF_PLAYER_FILENAME_TYPE_HTTP;
    	} else if (filename.startsWith("disc0:")) {
    		type = PSMF_PLAYER_FILENAME_TYPE_DISC0;
    	} else {
    		return SceKernelErrors.ERROR_PSMFPLAYER_NOT_SUPPORTED;
    	}
    	openControlType.setValue32(0, type);

    	switch (type) {
    		case PSMF_PLAYER_FILENAME_TYPE_HOST0:
    		case PSMF_PLAYER_FILENAME_TYPE_MS0:
    		case PSMF_PLAYER_FILENAME_TYPE_DISC0:
    		case PSMF_PLAYER_FILENAME_TYPE_MS0_ENCRYPTED:
    			openControlType.setValue32(8, 1);
    			break;
    		case PSMF_PLAYER_FILENAME_TYPE_HTTP:
    			log.error(String.format("initOpenControl unimplemented PSMF_PLAYER_FILENAME_TYPE_HTTP '%s'", filename));
    			break;
    	}

    	return 0;
    }

    private int openPsmf(TPointer data, String filename) {
    	int result = 0;

    	abortOpen = false;
    	TPointer readControlAddr = data.getPointer(48);
    	TPointer openControlType = readControlAddr.getPointer(56);
    	int type = getFilenameType(readControlAddr);
    	switch (type) {
			case PSMF_PLAYER_FILENAME_TYPE_HOST0:
			case PSMF_PLAYER_FILENAME_TYPE_MS0:
				result = hleSyscall(Modules.IoFileMgrForUserModule.sceIoOpen(new PspString(filename), IoFileMgrForUser.PSP_O_RDONLY, 0));
				openControlType.setValue32(4, result);
				break;
			case PSMF_PLAYER_FILENAME_TYPE_DISC0:
				result = hleSyscall(Modules.IoFileMgrForUserModule.sceIoOpen(new PspString(filename), IoFileMgrForUser.PSP_O_RDONLY, 0));
				openControlType.setValue32(4, result);
				break;
			case PSMF_PLAYER_FILENAME_TYPE_HTTP:
				log.error(String.format("openPsmf unimplemented PSMF_PLAYER_FILENAME_TYPE_HTTP '%s'", filename));
				break;
			case PSMF_PLAYER_FILENAME_TYPE_MS0_ENCRYPTED:
				result = hleSyscall(Modules.IoFileMgrForUserModule.sceIoOpen(new PspString(filename), IoFileMgrForUser.PSP_O_RDONLY, 0));
				openControlType.setValue32(4, result);
				if (result >= 0) {
					result = Modules.scePspNpDrm_userModule.sceNpDrmEdataSetupKey(result);
				}
				break;
    	}

    	if (abortOpen) {
    		// Too late
    	}

    	return result;
    }

    private int seekPsmf(TPointer readControlAddr, int offset) {
    	int result = 0;

    	TPointer openControlType = readControlAddr.getPointer(56);
    	int id = openControlType.getValue32(4);
    	int type = getFilenameType(readControlAddr);
    	switch (type) {
			case PSMF_PLAYER_FILENAME_TYPE_HOST0:
			case PSMF_PLAYER_FILENAME_TYPE_MS0:
			case PSMF_PLAYER_FILENAME_TYPE_DISC0:
			case PSMF_PLAYER_FILENAME_TYPE_MS0_ENCRYPTED:
				result = hleSyscall((int) Modules.IoFileMgrForUserModule.sceIoLseek(id, offset & 0xFFFFFFFFL, IoFileMgrForUser.PSP_SEEK_SET));
				break;
			case PSMF_PLAYER_FILENAME_TYPE_HTTP:
				log.error(String.format("seekPsmf unimplemented PSMF_PLAYER_FILENAME_TYPE_HTTP"));
				break;
    	}

    	return result;
    }

    private int closePsmf(TPointer readControlAddr) {
    	int result = 0;

    	TPointer openControlType = readControlAddr.getPointer(56);
    	int id = openControlType.getValue32(4);
    	int type = getFilenameType(readControlAddr);
    	switch (type) {
			case PSMF_PLAYER_FILENAME_TYPE_HOST0:
			case PSMF_PLAYER_FILENAME_TYPE_MS0:
			case PSMF_PLAYER_FILENAME_TYPE_DISC0:
			case PSMF_PLAYER_FILENAME_TYPE_MS0_ENCRYPTED:
				result = hleSyscall((int) Modules.IoFileMgrForUserModule.sceIoClose(id));
				break;
			case PSMF_PLAYER_FILENAME_TYPE_HTTP:
				log.error(String.format("closePsmf unimplemented PSMF_PLAYER_FILENAME_TYPE_HTTP"));
				break;
    	}

    	return result;
    }

    private int breakPsmf(TPointer readControlAddr) {
    	int result = 0;

    	int type = getFilenameType(readControlAddr);
    	switch (type) {
			case PSMF_PLAYER_FILENAME_TYPE_HOST0:
			case PSMF_PLAYER_FILENAME_TYPE_MS0:
			case PSMF_PLAYER_FILENAME_TYPE_DISC0:
			case PSMF_PLAYER_FILENAME_TYPE_MS0_ENCRYPTED:
		    	result = hleSyscall(Modules.ThreadManForUserModule.sceKernelSetEventFlag(scePsmfPlayerAbortThreadEvf, 0x2));
				break;
			case PSMF_PLAYER_FILENAME_TYPE_HTTP:
				log.error(String.format("breakPsmf unimplemented PSMF_PLAYER_FILENAME_TYPE_HTTP"));
				break;
    	}

    	return result;
    }

    private int getFilesize(TPointer data) {
    	int result = 0;

    	TPointer filenameAddr = data.getPointer(0);
    	TPointer readControlAddr = data.getPointer(48);
    	int type = getFilenameType(readControlAddr);
    	switch (type) {
			case PSMF_PLAYER_FILENAME_TYPE_HOST0:
			case PSMF_PLAYER_FILENAME_TYPE_MS0:
			case PSMF_PLAYER_FILENAME_TYPE_DISC0:
			case PSMF_PLAYER_FILENAME_TYPE_MS0_ENCRYPTED:
				TPointer statAddr = allocatePointer(SceIoStat.SIZEOF);
				result = hleSyscall(Modules.IoFileMgrForUserModule.sceIoGetstat(new PspString(filenameAddr.getAddress()), statAddr));
				if (result >= 0) {
					result = statAddr.getValue32(8);
				}
				break;
			case PSMF_PLAYER_FILENAME_TYPE_HTTP:
				log.error(String.format("getFilesize unimplemented PSMF_PLAYER_FILENAME_TYPE_HTTP"));
				break;
    	}

    	return result;
    }

    private int readFile(TPointer data, TPointer buffer, int size) {
    	int result = 0;

    	TPointer readControlAddr = data.getPointer(48);
    	TPointer openControlType = readControlAddr.getPointer(56);
    	int id = openControlType.getValue32(4);
    	int type = getFilenameType(readControlAddr);
    	switch (type) {
			case PSMF_PLAYER_FILENAME_TYPE_HOST0:
			case PSMF_PLAYER_FILENAME_TYPE_MS0:
			case PSMF_PLAYER_FILENAME_TYPE_DISC0:
			case PSMF_PLAYER_FILENAME_TYPE_MS0_ENCRYPTED:
				result = hleSyscall(Modules.IoFileMgrForUserModule.sceIoRead(id, buffer, size));
				if (log.isTraceEnabled()) {
					try {
						IoInfo info = Modules.IoFileMgrForUserModule.getFileIoInfo(id);
						if (info != null) {
							long fileSize;
							if (info.vFile != null) {
								fileSize = info.vFile.length();
							} else {
								fileSize = info.readOnlyFile.length();
							}
							log.trace(String.format("readFile buffer=%s, size=0x%X, position=0x%X/0x%X, result=0x%X", buffer, size, info.position, fileSize, result));
						}
					} catch (IOException e) {
						// Ignore exception
					}
				}
				break;
			case PSMF_PLAYER_FILENAME_TYPE_HTTP:
				log.error(String.format("readFile unimplemented PSMF_PLAYER_FILENAME_TYPE_HTTP"));
				break;
    	}

    	return result;
    }

    private int setPsmf(TPointer data) {
    	int result = getFilesize(data);
    	if (result < 0) {
    		return result;
    	}
    	int fileSize = result;

        TPointer psmfControlAddr = data.getPointer(28);
    	TPointer controlBuffer = psmfControlAddr.getPointer(168);
    	int controlBufferSize = psmfControlAddr.getValue32(172);
    	final int size = 2048;
    	result = readFile(data, controlBuffer, size);
    	if (result < size) {
    		return SceKernelErrors.ERROR_PSMFPLAYER_INVALID_PSMF;
    	}

    	TPointer32 offsetAddr = allocatePointer32(4);
    	result = Modules.scePsmfModule.scePsmfQueryStreamOffset(controlBuffer, offsetAddr);
    	if (result != 0) {
    		return SceKernelErrors.ERROR_PSMFPLAYER_INVALID_PSMF;
    	}
    	int streamOffset = offsetAddr.getValue();

    	if (fileSize != 0) {
    		if (streamOffset > fileSize) {
    			return SceKernelErrors.ERROR_PSMFPLAYER_TOO_BIG_OFFSET;
    		}
    	}

    	if (controlBufferSize < streamOffset) {
    		return SceKernelErrors.ERROR_PSMFPLAYER_INVALID_PSMF;
    	}
    	if (streamOffset > size) {
    		TPointer restHeaderBuffer = new TPointer(controlBuffer, size);
    		int restHeaderSize = streamOffset - size;
    		result = readFile(data, restHeaderBuffer, restHeaderSize);
    		if (result < restHeaderSize) {
    			return SceKernelErrors.ERROR_PSMFPLAYER_FAILED_READ_HEADER;
    		}
    	}

    	result = Modules.scePsmfModule.scePsmfVerifyPsmf(controlBuffer);
    	if (result != 0) {
    		return result;
    	}

    	TPointer psmf = new TPointer(psmfControlAddr, 24);
    	TPointer32 psmf32 = new TPointer32(psmf);
    	result = Modules.scePsmfModule.scePsmfSetPsmf(psmf32, controlBuffer);
    	if (result != 0) {
    		return result;
    	}

    	psmfControlAddr.memcpy(80, psmf, 56);

    	TPointer32 startTimeAddr = new TPointer32(psmfControlAddr, 4);
    	result = Modules.scePsmfModule.scePsmfGetPresentationStartTime(psmf32, startTimeAddr);
    	if (result != 0) {
    		return result;
    	}

    	TPointer32 endTimeAddr = new TPointer32(psmfControlAddr, 8);
    	result = Modules.scePsmfModule.scePsmfGetPresentationEndTime(psmf32, endTimeAddr);
    	if (result != 0) {
    		return result;
    	}

    	psmfControlAddr.setValue32(0, endTimeAddr.getValue() - startTimeAddr.getValue() - videoTimestampStep);

    	result = Modules.scePsmfModule.scePsmfGetNumberOfSpecificStreams(psmf32, PSMF_AVC_STREAM);
    	if (result < 0) {
    		return result;
    	}
    	int numberOfAvcStreams = result;
    	psmfControlAddr.setValue32(12, numberOfAvcStreams);

    	result = Modules.scePsmfModule.scePsmfGetNumberOfSpecificStreams(psmf32, PSMF_ATRAC_STREAM);
    	if (result < 0) {
    		return result;
    	}
    	int numberOfAtracStreams = result;
    	psmfControlAddr.setValue32(16, numberOfAtracStreams);

    	result = Modules.scePsmfModule.scePsmfGetNumberOfSpecificStreams(psmf32, PSMF_PCM_STREAM);
    	if (result < 0) {
    		return result;
    	}
    	int numberOfPcmStreams = result;
    	psmfControlAddr.setValue32(20, numberOfPcmStreams);

    	if (log.isTraceEnabled()) {
    		log.trace(String.format("setPsmf numberOfAvcStreams=%d, numberOfAtracStreams=%d, numberOfPcmStreams=%d", numberOfAvcStreams, numberOfAtracStreams, numberOfPcmStreams));
    	}

    	TPointer readControlAddr = data.getPointer(48);
    	readControlAddr.setValue32(32, streamOffset);
    	TPointer32 streamSizeAddr = new TPointer32(readControlAddr, 20);
    	result = Modules.scePsmfModule.scePsmfQueryStreamSize(controlBuffer, streamSizeAddr);
    	if (result != 0) {
    		return result;
    	}
    	readControlAddr.setValue32(28, streamSizeAddr.getValue());

    	TPointer buffer = allocatePointer(24);
    	buffer.memcpy(psmfControlAddr, 24);

    	TPointer psmfCopy = allocatePointer(56);
    	TPointer32 psmf32Copy = new TPointer32(psmfCopy);
    	psmfCopy.memcpy(psmf, 56);
    	int version = PSMF_PLAYER_VERSION_FULL;
    	for (int i = 0; i < numberOfAvcStreams; i++) {
    		result = Modules.scePsmfModule.scePsmfSpecifyStreamWithStreamTypeNumber(psmf32Copy, PSMF_AVC_STREAM, i);
    		if (result != 0) {
    			return result;
    		}
    		result = Modules.scePsmfModule.scePsmfCheckEPmap(psmf32Copy);
    		if (result == SceKernelErrors.ERROR_PSMF_NOT_FOUND) {
    			version = PSMF_PLAYER_VERSION_BASIC;
    			break;
    		}
    		if (result != 0) {
    			return result;
    		}
    	}
        TPointer playerStatusAddr = data.getPointer(24);
        playerStatusAddr.setValue32(8, version);

        TPointer decodeControlAddr = data.getPointer(36);
        TPointer avcDecodeAddr = decodeControlAddr.getPointer(24);
        result = registerStreams(psmf32Copy, avcDecodeAddr, PSMF_AVC_STREAM, numberOfAvcStreams);
        if (result != 0) {
        	return result;
        }

        TPointer atracDecodeAddr = decodeControlAddr.getPointer(28);
        result = registerStreams(psmf32Copy, atracDecodeAddr, PSMF_ATRAC_STREAM, numberOfAtracStreams);
        if (result != 0) {
        	return result;
        }

        TPointer pcmDecodeAddr = decodeControlAddr.getPointer(32);
        result = registerStreams(psmf32Copy, pcmDecodeAddr, PSMF_PCM_STREAM, numberOfPcmStreams);
        if (result != 0) {
        	return result;
        }

    	return 0;
    }

    @HLEFunction(nid = HLESyscallNid, version = 150)
    public void hlePsmfPlayerOpenControlThread(int args, TPointer argp) {
    	TPointer psmfPlayer = argp.getPointer();
    	TPointer data = psmfPlayer.getPointer();

    	int result = 0;

    	TPointer filenameAddr = data.getPointer(0);
    	String filename = filenameAddr.getStringNZ(512);

    	result = initOpenControl(data, filename);
    	if (result >= 0) {
    		result = openPsmf(data, filename);
    		if (result >= 0) {
    	    	TPointer readControlAddr = data.getPointer(48);
    			result = seekPsmf(readControlAddr, readControlAddr.getValue32(64));
    		}
    	}

    	if (result >= 0) {
    		result = setPsmf(data);
    	}

        TPointer strFileAddr = data.getPointer(52);
        if (strFileAddr.getValue32(0) == 3) {
        	if (strFileAddr.getValue32(8) != 1) {
                TPointer playerStatusAddr = data.getPointer(24);
                playerStatusAddr.setValue32(8, PSMF_PLAYER_VERSION_NET);
        	}
        }

    	Modules.ThreadManForUserModule.sceKernelExitThread(result);
    }

    private int getNumberFreeDisplayBuffers(TPointer videoBufferAddr) {
		int numberUsedDisplayBuffers = videoBufferAddr.getValue32(16);
		int numberDisplayBuffers = videoBufferAddr.getValue32(20);

		return numberDisplayBuffers - numberUsedDisplayBuffers;
    }

    private int getNumberFreeAudioBuffers(TPointer audioBufferAddr) {
    	int numberAudioBuffers = audioBufferAddr.getValue32(16);
    	int numberUsedAudioBuffers = audioBufferAddr.getValue32(12);

    	return numberAudioBuffers - numberUsedAudioBuffers;
    }

    private int addVideoBuffer(TPointer videoBufferAddr, TPointer buffer, int pts) {
    	int vBufChangeFlag = AtomicValue.get(videoBufferAddr.getPointer(44)); // ScePsmfPlayerVBufChangeFlag
    	if ((vBufChangeFlag & 0x2) == 0) {
    		int numberUsedDisplayBuffers = videoBufferAddr.getValue32(16);
    		int numberDisplayBuffers = videoBufferAddr.getValue32(20);
    		if (numberUsedDisplayBuffers == numberDisplayBuffers) {
    			return 1;
    		}
    		int displayBufferIndex = videoBufferAddr.getValue32(8);

    		TPointer displayDataArrayAddr = videoBufferAddr.getPointer(4);
    		displayDataArrayAddr.add(displayBufferIndex * 8);
    		displayDataArrayAddr.setPointer(0, buffer);
    		displayDataArrayAddr.setValue32(4, pts);

    		videoBufferAddr.setValue32(16, numberUsedDisplayBuffers + 1);
    		videoBufferAddr.setValue32(8, (displayBufferIndex + 1) % numberDisplayBuffers);
    	}

    	return 0;
    }

    private int addAudioBuffer(TPointer audioBufferAddr, TPointer buffer, int pts) {
		int numberUsedAudioBuffers = audioBufferAddr.getValue32(12);
		int numberAudioBuffers = audioBufferAddr.getValue32(16);
		if (numberUsedAudioBuffers == numberAudioBuffers) {
			return 1;
		}
		int audioBufferIndex = audioBufferAddr.getValue32(4);

		TPointer audioDataAddr = audioBufferAddr.getPointer(0);
		audioDataAddr.add(audioBufferIndex * 8);
		audioDataAddr.setPointer(0, buffer);
		audioDataAddr.setValue32(4, pts);

		audioBufferAddr.setValue32(12, numberUsedAudioBuffers + 1);
		audioBufferAddr.setValue32(4, (audioBufferIndex + 1) % numberAudioBuffers);

    	return 0;
    }

    private int avcDecodeStop(TPointer decodeControlAddr, boolean isRewind) {
    	TPointer videoBufferAddr = decodeControlAddr.getPointer(48);
    	TPointer avcDecodeAddr = decodeControlAddr.getPointer(24);

    	if (getNumberFreeDisplayBuffers(videoBufferAddr) == 0) {
    		return 1;
    	}
    	TPointer displayDataArrayAddr = videoBufferAddr.getPointer(4);
    	int displayBufferIndex = videoBufferAddr.getValue32(8);
    	TPointer buffer = displayDataArrayAddr.getPointer(displayBufferIndex * 8);

    	TPointer mpeg = avcDecodeAddr.getPointer(0);
    	TPointer32 mpeg32 = new TPointer32(mpeg);
    	TPointer32 bufferAddr = allocatePointer32(8);
    	TPointer32 gotFrameAddr = allocatePointer32(4);
    	bufferAddr.setPointer(0, buffer);
    	bufferAddr.setPointer(4, TPointer.NULL);
    	int result = hleSyscall(Modules.sceMpegModule.sceMpegAvcDecodeStopYCbCr(mpeg32, bufferAddr, gotFrameAddr));
    	if (log.isTraceEnabled()) {
    		log.trace(String.format("avcDecodeStop: sceMpegAvcDecodeStopYCbCr returned 0x%X", result));
    	}
    	if (result != 0) {
    		return result;
    	}

    	if (gotFrameAddr.getValue() == 1) {
    		TPointer ptsBufferAddr = avcDecodeAddr.getPointer(1104);
    		int ptsBufferIndex = avcDecodeAddr.getValue32(1112);
    		int numberPtsBuffers = avcDecodeAddr.getValue32(1120);
    		int pts = ptsBufferAddr.getValue32(ptsBufferIndex * 4);
    		avcDecodeAddr.setValue32(1112, (ptsBufferIndex + 1) % numberPtsBuffers);

    		int timestamp = decodeControlAddr.getValue32(60);
    		if (pts >= timestamp || isRewind) {
    			addVideoBuffer(videoBufferAddr, buffer, pts);
    		}
    	}

    	return 0;
    }

    private int avcDecode(TPointer decodeControlAddr, boolean isRewind) {
    	TPointer videoBufferAddr = decodeControlAddr.getPointer(48);
    	TPointer avcDecodeAddr = decodeControlAddr.getPointer(24);

    	if (getNumberFreeDisplayBuffers(videoBufferAddr) == 0) {
    		if (log.isTraceEnabled()) {
    			log.trace(String.format("avcDecode numberFreeDisplayBuffers==0"));
    		}
    		return 1;
    	}
    	TPointer displayDataArrayAddr = videoBufferAddr.getPointer(4);
    	int displayBufferIndex = videoBufferAddr.getValue32(8);
    	TPointer buffer = displayDataArrayAddr.getPointer(displayBufferIndex * 8);

    	TPointer mpeg = avcDecodeAddr.getPointer(0);
    	TPointer32 mpeg32 = new TPointer32(mpeg);
    	int numberRegisteredAvcStreams = avcDecodeAddr.getValue32(8);
    	boolean needToDecode = false;
    	int avcStreamMode0 = avcDecodeAddr.getValue32(4);
    	for (int i = 0; i < numberRegisteredAvcStreams; i++) {
    		TPointer streamAddr = avcDecodeAddr.getPointer(12 + i * 4);
    		TPointer auAddr = new TPointer(avcDecodeAddr, 76 + i * 64);
    		int result = hleSyscall(Modules.sceMpegModule.sceMpegGetAvcAu(mpeg32, streamAddr, auAddr, TPointer32.NULL));
    		if (log.isTraceEnabled()) {
    			log.trace(String.format("avcDecode: sceMpegGetAvcAu(stream#%d) returned 0x%X, au.esSize=0x%X", i, result, auAddr.getValue32(20)));
    		}
    		if (result != 0 && result != ERROR_MPEG_NO_DATA) {
    			return result;
    		}
    		if (result == 0 && i == avcStreamMode0) {
    			needToDecode = true;
    		}
    	}

    	if (!needToDecode) {
    		return ERROR_MPEG_NO_DATA;
    	}

    	TPointer auAddr = new TPointer(avcDecodeAddr, 76 + avcStreamMode0 * 64);
    	TPointer32 bufferAddr = allocatePointer32(8);
    	TPointer32 gotFrameAddr = allocatePointer32(4);
    	bufferAddr.setPointer(0, buffer);
    	bufferAddr.setPointer(4, TPointer.NULL);
    	int result = hleSyscall(Modules.sceMpegModule.sceMpegAvcDecodeYCbCr(mpeg32, auAddr, bufferAddr, gotFrameAddr));
		if (log.isTraceEnabled()) {
			log.trace(String.format("avcDecode: sceMpegAvcDecodeYCbCr(stream#%d) returned 0x%X", avcStreamMode0, result));
		}
    	if (result != 0) {
    		return result;
    	}

    	int pts = auAddr.getValue32(4);
    	if (pts == -1) {
    		pts = avcDecodeAddr.getValue32(1124) + videoTimestampStep;
    	}
    	avcDecodeAddr.setValue32(1124, pts); // Current PTS

		TPointer ptsBufferAddr = avcDecodeAddr.getPointer(1104);
    	ptsBufferAddr.setValue32(avcDecodeAddr.getValue32(1108) * 4, pts);
    	avcDecodeAddr.setValue32(1108, (avcDecodeAddr.getValue32(1108) + 1) % avcDecodeAddr.getValue32(1120));
    	avcDecodeAddr.setValue32(1116, avcDecodeAddr.getValue32(1116) + 1);

    	if (gotFrameAddr.getValue() == 1) {
    		int ptsBufferIndex = avcDecodeAddr.getValue32(1112);
    		int numberPtsBuffers = avcDecodeAddr.getValue32(1120);
    		int decodePts = ptsBufferAddr.getValue32(ptsBufferIndex * 4);
    		avcDecodeAddr.setValue32(1112, (ptsBufferIndex + 1) % numberPtsBuffers);
        	avcDecodeAddr.setValue32(1116, avcDecodeAddr.getValue32(1116) - 1);

    		int timestamp = decodeControlAddr.getValue32(60);
    		if (pts >= timestamp || isRewind) {
    			addVideoBuffer(videoBufferAddr, buffer, decodePts);
    		}
    	}

    	return 0;
    }

    private int atracDecode(TPointer atracDecodeAddr, TPointer buffer, TPointer32 ptsAddr) {
    	int result;

    	TPointer mpeg = atracDecodeAddr.getPointer(0);
    	TPointer32 mpeg32 = new TPointer32(mpeg);
    	int numberRegisteredAtracStreams = atracDecodeAddr.getValue32(8);
    	boolean needToDecode = false;
    	int atracStreamMode0 = atracDecodeAddr.getValue32(4);
    	for (int i = 0; i < numberRegisteredAtracStreams; i++) {
    		TPointer streamAddr = atracDecodeAddr.getPointer(12 + i * 4);
    		TPointer auAddr = new TPointer(atracDecodeAddr, 76 + i * 64);
    		result = hleSyscall(Modules.sceMpegModule.sceMpegGetAtracAu(mpeg32, streamAddr, auAddr, TPointer32.NULL));
    		if (log.isTraceEnabled()) {
    			log.trace(String.format("atracDecode: sceMpegGetAtracAu returned 0x%X", result));
    		}
    		if (result != 0 && result != ERROR_MPEG_NO_DATA) {
    			return result;
    		}
    		if (result == 0 && i == atracStreamMode0) {
    			needToDecode = true;
    		}
    	}

    	if (!needToDecode) {
    		return ERROR_MPEG_NO_DATA;
    	}

    	TPointer auAddr = new TPointer(atracDecodeAddr, 76 + atracStreamMode0 * 64);
    	int status = atracDecodeAddr.getValue32(1112);
    	int init = status == 1 || status == 3 ? 1 : 0;
    	result = hleSyscall(Modules.sceMpegModule.sceMpegAtracDecode(mpeg32, auAddr, buffer, init));
		if (log.isTraceEnabled()) {
			log.trace(String.format("atracDecode: sceMpegAtracDecode returned 0x%X", result));
		}
    	if (result != 0) {
    		return result;
    	}

    	int pts = auAddr.getValue32(4);
    	if (pts == -1) {
    		pts = atracDecodeAddr.getValue32(1116) + audioTimestampStep;
    	}
    	atracDecodeAddr.setValue32(1116, pts); // Current PTS
    	ptsAddr.setValue(pts);

    	if (init != 0) {
    		int atracOutputSize = atracDecodeAddr.getValue32(1108);
    		buffer.clear(atracOutputSize);
    	}
    	status = 2;
    	atracDecodeAddr.setValue32(1112, status);

    	return 1;
    }

    private int addPcmData(TPointer pcmDecodeAddr, TPointer esBuffer, int esSize, int numberSamples, int pts) {
		if (pcmDecodeAddr.getValue32(1136) - pcmDecodeAddr.getValue32(1132) < numberSamples) {
			return -1;
		}
    	TPointer pcmPoolAddr = pcmDecodeAddr.getPointer(1116);
    	int pcmDataIndex = pcmDecodeAddr.getValue32(1124);
    	pcmPoolAddr.add(pcmDataIndex * 4);
    	pcmPoolAddr.memcpy(esBuffer, esSize);

    	if (pts == -1) {
    		pts = pcmDecodeAddr.getValue32(1108) + 163;
    	}
    	TPointer pcmPtsAddr = pcmDecodeAddr.getPointer(1120);
    	pcmPtsAddr.setValue32(pcmDecodeAddr.getValue32(1124) / 80 * 4, pts);
    	pcmDecodeAddr.setValue32(1108, pts);

    	pcmDecodeAddr.setValue32(1124, (pcmDecodeAddr.getValue32(1124) + numberSamples) % pcmDecodeAddr.getValue32(1136));
    	pcmDecodeAddr.setValue32(1132, pcmDecodeAddr.getValue32(1132) + numberSamples);

    	return 0;
    }

    private int pcmDecode(TPointer pcmDecodeAddr, TPointer buffer, TPointer32 ptsAddr) {
    	int result;

    	TPointer mpeg = pcmDecodeAddr.getPointer(0);
    	TPointer32 mpeg32 = new TPointer32(mpeg);
    	int numberRegisteredPcmStreams = pcmDecodeAddr.getValue32(8);
    	boolean dataFound = false;
    	int pcmStreamMode0 = pcmDecodeAddr.getValue32(4);
    	for (int i = 0; i < numberRegisteredPcmStreams; i++) {
    		TPointer streamAddr = pcmDecodeAddr.getPointer(12 + i * 4);
    		TPointer auAddr = new TPointer(pcmDecodeAddr, 76 + i * 64);
    		for (int j = 0; j < 30; j++) {
	    		if (pcmDecodeAddr.getValue32(1136) - pcmDecodeAddr.getValue32(1132) < 80) {
	    			break;
	    		}
	    		result = Modules.sceMpegModule.sceMpegGetPcmAu(mpeg32, streamAddr, auAddr, TPointer32.NULL);
	    		if (log.isTraceEnabled()) {
	    			log.trace(String.format("pcmDecode: sceMpegGetPcmAu returned 0x%X", result));
	    		}
	    		if (result != 0 && result != ERROR_MPEG_NO_DATA) {
	    			return result;
	    		}
	    		if (result == 0 && i == pcmStreamMode0) {
	    			int pts = auAddr.getValue32(4);
	    			TPointer esBuffer = auAddr.getPointer(16);
	    			int esSize = auAddr.getValue32(20);
	    			addPcmData(pcmDecodeAddr, esBuffer, esSize, 80, pts);
	    			dataFound = true;
	    		} else if (result == ERROR_MPEG_NO_DATA) {
	    			break;
	    		}
    		}
    	}

    	final int numberSamples = 2048;
    	if (pcmDecodeAddr.getValue32(1132) < numberSamples) {
    		return dataFound ? ERROR_MPEG_NO_DATA : 0;
    	}

    	TPointer pcmPoolAddr = pcmDecodeAddr.getPointer(1116);
    	int pcmDataIndex = pcmDecodeAddr.getValue32(1128);
    	pcmPoolAddr.add(pcmDataIndex * 4);
    	buffer.memcpy(pcmPoolAddr, numberSamples * 4);

    	TPointer pcmPtsAddr = pcmDecodeAddr.getPointer(1120);
    	int pts = pcmPtsAddr.getValue32(pcmDataIndex / 80 * 4);
    	ptsAddr.setValue(pts);

    	pcmDecodeAddr.setValue32(1128, (pcmDataIndex + numberSamples) % pcmDecodeAddr.getValue32(1136));
    	pcmDecodeAddr.setValue32(1132, pcmDecodeAddr.getValue32(1132) - numberSamples);

    	pcmDecodeAddr.setValue32(1112, pts); // Current PTS

    	return 1;
    }

    private int audioDecode(TPointer decodeControlAddr) {
    	TPointer audioBufferAddr = decodeControlAddr.getPointer(52);
    	TPointer atracDecodeAddr = decodeControlAddr.getPointer(28);
    	TPointer pcmDecodeAddr = decodeControlAddr.getPointer(32);

    	if (getNumberFreeAudioBuffers(audioBufferAddr) == 0) {
    		if (log.isTraceEnabled()) {
    			log.trace(String.format("audioDecode numberFreeAudioBuffers==0"));
    		}
    		return 1;
    	}
    	TPointer audioDataAddr = audioBufferAddr.getPointer(0);
    	int audioBufferIndex = audioBufferAddr.getValue32(4);
    	TPointer buffer = audioDataAddr.getPointer(audioBufferIndex * 8);

    	TPointer32 ptsAddr = allocatePointer32(4);
    	int atracResult = atracDecode(atracDecodeAddr, buffer, ptsAddr);
    	if (atracResult == 1) {
    		int timestamp = decodeControlAddr.getValue32(60);
    		int pts = ptsAddr.getValue();
    		if (pts < timestamp - audioTimestampStep) {
    			return 0;
    		}
			addAudioBuffer(audioBufferAddr, buffer, pts);
    	} else if (atracResult != 0 && atracResult != ERROR_MPEG_NO_DATA) {
			int status = 1;
			atracDecodeAddr.setValue32(1112, status);
    	}

    	int pcmResult = pcmDecode(pcmDecodeAddr, buffer, ptsAddr);
    	if (pcmResult != 1) {
    		return pcmResult == 0 && atracResult != 0 ? atracResult : pcmResult;
    	}

		int timestamp = decodeControlAddr.getValue32(60);
		int pts = ptsAddr.getValue();
		if (pts < timestamp - audioTimestampStep) {
			return 0;
		}
		addAudioBuffer(audioBufferAddr, buffer, pts);

    	return 0;
    }

    private void executeDecodeStatusPlay(TPointer decodeControlAddr) {
    	TPointer playerStatusAddr = decodeControlAddr.getPointer(36);
    	TPointer readControlAddr = decodeControlAddr.getPointer(44);
    	int result;

    	if (!isPlayerStatusOK(playerStatusAddr)) {
        	AtomicValue.set(decodeControlAddr.getPointer(72), PSMF_PLAYER_DECODE_STATUS_END); // ScePsmfPlayerDecodeStatus
    	} else {
        	int readStatus = AtomicValue.get(readControlAddr.getPointer(4)); // ScePsmfPlayerReadStatus
        	if (log.isTraceEnabled()) {
        		log.trace(String.format("executeDecodeStatusPlay readStatus=0x%X", readStatus));
        	}
        	if (readStatus == 0x10 || readStatus == 0x8 || readStatus == 0x2) {
        		if (readStatus == 0x2) {
            		TPointer ringbufferAddr = readControlAddr.getPointer(48);
            		int ringbufferAvailableSize = Modules.sceMpegModule.sceMpegRingbufferAvailableSize(ringbufferAddr);
                	if (log.isTraceEnabled()) {
                		log.trace(String.format("executeDecodeStatusPlay: sceMpegRingbufferAvailableSize returned 0x%X", ringbufferAvailableSize));
                	}
            		int ringbufferNumPackets = readControlAddr.getValue32(52);
            		if (ringbufferAvailableSize == ringbufferNumPackets) {
            			result = avcDecodeStop(decodeControlAddr, false);
            			if (result == 0) {
                        	AtomicValue.set(decodeControlAddr.getPointer(72), PSMF_PLAYER_DECODE_STATUS_END); // ScePsmfPlayerDecodeStatus
            			}
            			return;
            		}
        		}

        		result = audioDecode(decodeControlAddr);
        		if (result < 0 && result != ERROR_MPEG_NO_DATA) {
    				setPlayerStatusError(playerStatusAddr, result);
    				return;
        		}
        		int value1 = result;

        		result = avcDecode(decodeControlAddr, false);
        		if (result < 0 && result != ERROR_MPEG_NO_DATA) {
    				setPlayerStatusError(playerStatusAddr, result);
    				return;
        		}
        		int value2 = result;

        		// Re-read the readStatus as the previous calls can switch to another thread
        		// and the value could have changed.
            	readStatus = AtomicValue.get(readControlAddr.getPointer(4)); // ScePsmfPlayerReadStatus
        		if (value1 == 0 || value2 == 0) {
        			if (readStatus == 0x8) {
    		    		setReadStatus(readControlAddr, 0x10);
        			}
        		}

        		if (readStatus == 0x2) {
        			if (value1 == ERROR_MPEG_NO_DATA && value2 == ERROR_MPEG_NO_DATA) {
                    	AtomicValue.set(decodeControlAddr.getPointer(72), PSMF_PLAYER_DECODE_STATUS_END); // ScePsmfPlayerDecodeStatus
        			}
        		}
        	}
    	}
    }

    private void executeDecodeStatusStartPlay(TPointer decodeControlAddr) {
    	TPointer playerStatusAddr = decodeControlAddr.getPointer(36);
    	TPointer readControlAddr = decodeControlAddr.getPointer(44);
    	int result;

    	if (!isPlayerStatusOK(playerStatusAddr)) {
        	AtomicValue.set(decodeControlAddr.getPointer(72), PSMF_PLAYER_DECODE_STATUS_END); // ScePsmfPlayerDecodeStatus
    	} else {
        	int readStatus = AtomicValue.get(readControlAddr.getPointer(4)); // ScePsmfPlayerReadStatus
    		TPointer ringbufferAddr = readControlAddr.getPointer(48);
    		int ringbufferAvailableSize = Modules.sceMpegModule.sceMpegRingbufferAvailableSize(ringbufferAddr);
        	if (log.isTraceEnabled()) {
        		log.trace(String.format("executeDecodeStatusStartPlay: readStatus=0x%X, sceMpegRingbufferAvailableSize returned 0x%X", readStatus, ringbufferAvailableSize));
        	}
    		if (readStatus == 0x2 || ringbufferAvailableSize < 32) {
        		result = initAus(decodeControlAddr);
        		if (result != 0) {
        			setPlayerStatusError(playerStatusAddr, result);
        		} else {
                	AtomicValue.set(decodeControlAddr.getPointer(72), PSMF_PLAYER_DECODE_STATUS_PLAY); // ScePsmfPlayerDecodeStatus
        		}
    		}
    	}
    }

    private void executeDecodeStatusForward(TPointer decodeControlAddr) {
        TPointer avcDecodeAddr = decodeControlAddr.getPointer(24);
    	TPointer playerStatusAddr = decodeControlAddr.getPointer(36);
    	TPointer readControlAddr = decodeControlAddr.getPointer(44);
    	TPointer videoBufferAddr = decodeControlAddr.getPointer(48);
		TPointer psmfControlAddr = decodeControlAddr.getPointer(40);
    	TPointer psmf = new TPointer(psmfControlAddr, 24);
    	TPointer32 psmf32 = new TPointer32(psmf);
    	int result;

    	if (!isPlayerStatusOK(playerStatusAddr)) {
        	AtomicValue.set(decodeControlAddr.getPointer(72), PSMF_PLAYER_DECODE_STATUS_END); // ScePsmfPlayerDecodeStatus
    	} else {
        	int readStatus = AtomicValue.get(readControlAddr.getPointer(4)); // ScePsmfPlayerReadStatus
    		if ((readStatus == 0x2 || readStatus == 0x4) && getNumberFreeDisplayBuffers(videoBufferAddr) >= 2) {
        		result = avcDecode(decodeControlAddr, false);
        		if (result < 0 && result != ERROR_MPEG_NO_DATA) {
        			setPlayerStatusError(playerStatusAddr, result);
        			return;
        		}
        		result = avcDecodeStop(decodeControlAddr, false);
        		if (result != 0) {
        			return;
        		}

        		int videoCodec = decodeControlAddr.getValue32(8);
        		if (videoCodec != 0 && videoCodec != 14) {
        			setPlayerStatusError(playerStatusAddr, ERROR_PSMFPLAYER_FATAL);
        			return;
        		}
        		int currentPts = avcDecodeAddr.getValue32(1124);

        		result = Modules.scePsmfModule.scePsmfGetEPidWithTimestamp(psmf32, currentPts);
        		int epId = result;
        		if (result == SceKernelErrors.ERROR_PSMF_INVALID_TIMESTAMP) {
        			epId = 0;
        		} else if (result < 0) {
        			setPlayerStatusError(playerStatusAddr, result);
        			return;
        		}

        		int playSpeed = decodeControlAddr.getValue32(64);
        		epId += playSpeed;
        		TPointer32 outAddr = allocatePointer32(16);
        		result = Modules.scePsmfModule.scePsmfGetEPWithId(psmf32, epId, outAddr);
        		if (result == SceKernelErrors.ERROR_PSMF_INVALID_TIMESTAMP) {
                	AtomicValue.set(decodeControlAddr.getPointer(72), PSMF_PLAYER_DECODE_STATUS_END); // ScePsmfPlayerDecodeStatus
        		} else if (result != 0) {
        			setPlayerStatusError(playerStatusAddr, result);
        			return;
        		} else {
        			int entryOffset = outAddr.getValue(4);
        			int entryPicOffset = outAddr.getValue(12);
        			readControlAddr.setValue32(36, entryOffset);
        	    	readControlAddr.setValue32(40, Math.max(entryPicOffset, 0x10000));
        			setReadStatus(readControlAddr, 0x40);
        		}
    		}
    	}
    }

    private void executeDecodeStatusRewind(TPointer decodeControlAddr) {
        TPointer avcDecodeAddr = decodeControlAddr.getPointer(24);
    	TPointer playerStatusAddr = decodeControlAddr.getPointer(36);
    	TPointer readControlAddr = decodeControlAddr.getPointer(44);
    	TPointer videoBufferAddr = decodeControlAddr.getPointer(48);
		TPointer psmfControlAddr = decodeControlAddr.getPointer(40);
    	TPointer psmf = new TPointer(psmfControlAddr, 24);
    	TPointer32 psmf32 = new TPointer32(psmf);
    	int result;

    	if (!isPlayerStatusOK(playerStatusAddr)) {
        	AtomicValue.set(decodeControlAddr.getPointer(72), PSMF_PLAYER_DECODE_STATUS_END); // ScePsmfPlayerDecodeStatus
    	} else {
        	int readStatus = AtomicValue.get(readControlAddr.getPointer(4)); // ScePsmfPlayerReadStatus
    		if ((readStatus == 0x2 || readStatus == 0x4) && getNumberFreeDisplayBuffers(videoBufferAddr) >= 2) {
        		result = avcDecode(decodeControlAddr, true);
        		if (result < 0 && result != ERROR_MPEG_NO_DATA) {
                	AtomicValue.set(decodeControlAddr.getPointer(72), PSMF_PLAYER_DECODE_STATUS_START_PLAY); // ScePsmfPlayerDecodeStatus
        			return;
        		}
        		result = avcDecodeStop(decodeControlAddr, true);
        		if (result != 0) {
        			return;
        		}

        		int videoCodec = decodeControlAddr.getValue32(8);
        		if (videoCodec != 0 && videoCodec != 14) {
        			setPlayerStatusError(playerStatusAddr, ERROR_PSMFPLAYER_FATAL);
        			return;
        		}
        		int currentPts = avcDecodeAddr.getValue32(1124);

        		result = Modules.scePsmfModule.scePsmfGetEPidWithTimestamp(psmf32, currentPts);
        		int epId = result;
        		if (result == SceKernelErrors.ERROR_PSMF_INVALID_TIMESTAMP) {
        			epId = 0;
        		} else if (result < 0) {
        			setPlayerStatusError(playerStatusAddr, result);
        			return;
        		}

        		int playSpeed = decodeControlAddr.getValue32(64);
        		epId -= playSpeed;
        		if (epId <= 0) {
                	AtomicValue.set(decodeControlAddr.getPointer(72), PSMF_PLAYER_DECODE_STATUS_END); // ScePsmfPlayerDecodeStatus
        		} else {
	        		TPointer32 outAddr = allocatePointer32(16);
	        		result = Modules.scePsmfModule.scePsmfGetEPWithId(psmf32, epId, outAddr);
	        		if (result != 0) {
	        			setPlayerStatusError(playerStatusAddr, result);
	        			return;
	        		}
        			int entryOffset = outAddr.getValue(4);
        			int entryPicOffset = outAddr.getValue(12);
        			readControlAddr.setValue32(36, entryOffset);
        	    	readControlAddr.setValue32(40, Math.max(entryPicOffset, 0x10000));
        			setReadStatus(readControlAddr, 0x40);
        		}
    		}
    	}
    }

    @HLEFunction(nid = HLESyscallNid, version = 150)
    public void hlePsmfPlayerDecodeThread(int args, TPointer argp) {
    	TPointer decodeControlAddr = argp.getPointer();
    	TPointer playerStatusAddr = decodeControlAddr.getPointer(36);

    	boolean exit = false;
    	while (!exit) {
    		hleSyscall(Modules.ThreadManForUserModule.sceKernelWaitEventFlag(playerStatusAddr.getValue32(24), 0x4, PSP_EVENT_WAITCLEAR | PSP_EVENT_WAITOR, TPointer32.NULL, TPointer32.NULL));
    		int decodeStatus = AtomicValue.get(decodeControlAddr.getPointer(72)); // ScePsmfPlayerDecodeStatus

    		if (log.isTraceEnabled()) {
    			log.trace(String.format("hlePsmfPlayerDecodeThread decodeStatus=%d", decodeStatus));
    		}

    		switch (decodeStatus) {
    			case PSMF_PLAYER_DECODE_STATUS_PLAY:
    				executeDecodeStatusPlay(decodeControlAddr);
					break;
    			case PSMF_PLAYER_DECODE_STATUS_FORWARD:
    				executeDecodeStatusForward(decodeControlAddr);
					break;
    			case PSMF_PLAYER_DECODE_STATUS_REWIND:
    				executeDecodeStatusRewind(decodeControlAddr);
					break;
    			case PSMF_PLAYER_DECODE_STATUS_START_PLAY:
    				executeDecodeStatusStartPlay(decodeControlAddr);
					break;
    			case PSMF_PLAYER_DECODE_STATUS_EXIT:
    				exit = true;
					break;
    		}
    	}

    	AtomicValue.set(decodeControlAddr.getPointer(72), PSMF_PLAYER_DECODE_STATUS_EXITED); // ScePsmfPlayerDecodeStatus

		Modules.ThreadManForUserModule.sceKernelExitThread(0);
    }

    private void executeRead0x10(TPointer readControlAddr) {
    	TPointer playerStatusAddr = readControlAddr.getPointer(16);
		int streamSize = readControlAddr.getValue32(20);
		int streamSizePut = readControlAddr.getValue32(24);
    	if (!isPlayerStatusOK(playerStatusAddr) || (readControlAddr.getValue32(28) != 0 && streamSize == streamSizePut)) {
    		setReadStatus(readControlAddr, 0x2);
    	} else {
    		TPointer ringbufferAddr = readControlAddr.getPointer(48);
    		int ringbufferAvailableSize = Modules.sceMpegModule.sceMpegRingbufferAvailableSize(ringbufferAddr);
        	if (log.isTraceEnabled()) {
        		log.trace(String.format("executeRead0x10: sceMpegRingbufferAvailableSize returned 0x%X", ringbufferAvailableSize));
        	}
    		if (ringbufferAvailableSize < 32) {
        		setReadStatus(readControlAddr, 0x8);
    		} else {
    			int numPackets = Math.min((streamSize - streamSizePut) >> 11, 32);

            	if (log.isTraceEnabled()) {
            		log.trace(String.format("executeRead0x10: calling sceMpegRingbufferPut numPackets=0x%X, availableSize=0x%X", numPackets, ringbufferAvailableSize));
            	}
    			int result = hleSyscall(Modules.sceMpegModule.sceMpegRingbufferPut(ringbufferAddr, numPackets, ringbufferAvailableSize));
            	if (log.isTraceEnabled()) {
            		log.trace(String.format("executeRead0x10: sceMpegRingbufferPut returned 0x%X", result));
            	}

            	if (result == 0) {
    	    		setReadStatus(readControlAddr, 0x2);
    			} else if (result < 0) {
    				setPlayerStatusError(playerStatusAddr, result);
    			} else {
    				int numPacketsPut = result;
    				streamSizePut += numPacketsPut << 11;
    				readControlAddr.setValue32(24, streamSizePut);
    				if (streamSizePut >= streamSize) {
        	    		setReadStatus(readControlAddr, 0x2);
    				}
    			}
    		}
    	}
    }

    private void executeRead0x80(TPointer readControlAddr) {
    	TPointer playerStatusAddr = readControlAddr.getPointer(16);
		int streamSize = readControlAddr.getValue32(20);
		int streamSizePut = readControlAddr.getValue32(24);
    	if (!isPlayerStatusOK(playerStatusAddr) || (readControlAddr.getValue32(28) != 0 && streamSize == streamSizePut)) {
    		setReadStatus(readControlAddr, 0x2);
    	} else {
    		TPointer ringbufferAddr = readControlAddr.getPointer(48);
    		int ringbufferAvailableSize = Modules.sceMpegModule.sceMpegRingbufferAvailableSize(ringbufferAddr);
        	if (log.isTraceEnabled()) {
        		log.trace(String.format("executeRead0x80: sceMpegRingbufferAvailableSize returned 0x%X", ringbufferAvailableSize));
        	}
    		int numPackets;
			if ((ringbufferAvailableSize << 11) < readControlAddr.getValue32(40)) {
				numPackets = 32;
			} else {
				numPackets = readControlAddr.getValue32(40) >>> 11;
			}

        	if (log.isTraceEnabled()) {
        		log.trace(String.format("executeRead0x80: calling sceMpegRingbufferPut numPackets=0x%X, availableSize=0x%X", numPackets, ringbufferAvailableSize));
        	}
			int result = hleSyscall(Modules.sceMpegModule.sceMpegRingbufferPut(ringbufferAddr, numPackets, ringbufferAvailableSize));
        	if (log.isTraceEnabled()) {
        		log.trace(String.format("executeRead0x80: sceMpegRingbufferPut returned 0x%X", result));
        	}

        	if (result == 0) {
	    		setReadStatus(readControlAddr, 0x2);
			} else if (result < 0) {
	    		setReadStatus(readControlAddr, 0x4);
				setPlayerStatusError(playerStatusAddr, result);
			} else {
				int numPacketsPut = result;
				streamSizePut += numPacketsPut << 11;
				readControlAddr.setValue32(24, streamSizePut);
				if (streamSizePut >= readControlAddr.getValue32(40)) {
    	    		setReadStatus(readControlAddr, 0x4);
				}
			}
    	}
    }

    private int executeSeek(TPointer readControlAddr, int offset, int valueUsedOnlyForHttp) {
    	int psmfOffset = readControlAddr.getValue32(64); // offset given at scePsmfPlayerSetPsmfOffset()
    	int streamOffset = readControlAddr.getValue32(32);
    	int streamSize = readControlAddr.getValue32(28);
    	readControlAddr.setValue32(20, streamSize - offset);
    	int result = seekPsmf(readControlAddr, psmfOffset + streamOffset + offset);
    	if (result < 0) {
    		return result;
    	}

    	return 0;
    }

    private void executeSeek0x40(TPointer readControlAddr) {
    	TPointer playerStatusAddr = readControlAddr.getPointer(16);
    	if (!isPlayerStatusOK(playerStatusAddr)) {
    		setReadStatus(readControlAddr, 0x2);
    	} else {
    		TPointer mpeg = readControlAddr.getPointer(44);
    		TPointer32 mpeg32 = new TPointer32(mpeg);
    		int result = Modules.sceMpegModule.sceMpegFlushAllStream(mpeg32);
        	if (log.isTraceEnabled()) {
        		log.trace(String.format("executeSeek0x40: sceMpegFlushAllStream returned 0x%X", result));
        	}
    		if (result != 0) {
    			setPlayerStatusError(playerStatusAddr, result);
    		} else {
    			result = executeSeek(readControlAddr, readControlAddr.getValue32(36), readControlAddr.getValue32(40));
    			if (result != 0) {
        			setPlayerStatusError(playerStatusAddr, result);
    			} else {
    				readControlAddr.setValue32(24, 0);
    	    		setReadStatus(readControlAddr, 0x80);
    			}
    		}
    	}
    }

    private void executeSeek0x20(TPointer readControlAddr) {
    	TPointer playerStatusAddr = readControlAddr.getPointer(16);
    	if (!isPlayerStatusOK(playerStatusAddr)) {
    		setReadStatus(readControlAddr, 0x2);
    	} else {
    		TPointer mpeg = readControlAddr.getPointer(44);
    		TPointer32 mpeg32 = new TPointer32(mpeg);
    		int result = Modules.sceMpegModule.sceMpegFlushAllStream(mpeg32);
        	if (log.isTraceEnabled()) {
        		log.trace(String.format("executeSeek0x20: sceMpegFlushAllStream returned 0x%X", result));
        	}
    		if (result != 0) {
    			setPlayerStatusError(playerStatusAddr, result);
    		} else if (getPlayerVersion(playerStatusAddr) == PSMF_PLAYER_VERSION_NET) {
				readControlAddr.setValue32(24, 0);
	    		setReadStatus(readControlAddr, 0x10);
			} else {
				result = executeSeek(readControlAddr, readControlAddr.getValue32(36), 0);
    			if (result != 0) {
        			setPlayerStatusError(playerStatusAddr, result);
    			} else {
    				readControlAddr.setValue32(24, 0);
    	    		setReadStatus(readControlAddr, 0x10);
    			}
			}
    	}
    }

    @HLEFunction(nid = HLESyscallNid, version = 150)
    public void hlePsmfPlayerReadThread(int args, TPointer argp) {
    	TPointer readControlAddr = argp.getPointer();

    	while (true) {
    		// Wait for event flag "ScePsmfPlayerReadStatus"
    		hleSyscall(Modules.ThreadManForUserModule.sceKernelWaitEventFlag(readControlAddr.getValue32(0), 0x3F0, PSP_EVENT_WAITOR, TPointer32.NULL, TPointer32.NULL));

        	int readStatus = AtomicValue.get(readControlAddr.getPointer(4)); // ScePsmfPlayerReadStatus

        	if (log.isTraceEnabled()) {
        		log.trace(String.format("hlePsmfPlayerReadThread readStatus=0x%X", readStatus));
        	}

        	if (readStatus == 0x10) {
        		executeRead0x10(readControlAddr);
        	} else if (readStatus == 0x40) {
        		executeSeek0x40(readControlAddr);
        	} else if (readStatus == 0x20) {
        		executeSeek0x20(readControlAddr);
        	} else if (readStatus == 0x80) {
        		executeRead0x80(readControlAddr);
        	} else if (readStatus == 0x100) {
        		break;
        	}
    	}

		setReadStatus(readControlAddr, 0x200);

    	Modules.ThreadManForUserModule.sceKernelExitThread(0);
    }

    @HLEFunction(nid = HLESyscallNid, version = 150)
    public void hlePsmfPlayerAbortThread() {
    	while (true) {
	    	TPointer32 outBitsAddr = allocatePointer32(4);
	    	int result = hleSyscall(Modules.ThreadManForUserModule.sceKernelWaitEventFlag(scePsmfPlayerAbortThreadEvf, 0x7, PSP_EVENT_WAITCLEAR | PSP_EVENT_WAITOR, outBitsAddr, TPointer32.NULL));
	    	if (result < 0) {
	    		break;
	    	}
	    	int abortAction = outBitsAddr.getValue();
	    	if ((abortAction & 0x4) != 0) {
	    		break;
	    	}
	    	if ((abortAction & 0x1) != 0) {
	    		abortOpen = true;
	    	}
	    	if ((abortAction & 0x2) != 0) {
	    		// This would call sceHttpAbortRequest() for HTTP/HTTPS connections and do nothing for the other type of files
	    	}
    	}

    	Modules.ThreadManForUserModule.sceKernelExitThread(0);
    }

    @HLEFunction(nid = HLESyscallNid, version = 150)
    public int hlePsmfPlayerRingbufferPutCallback(TPointer dataAddress, int numPackets, TPointer32 psmfPlayer) {
    	TPointer data = psmfPlayer.getPointer();
    	int result;

    	TPointer readBuffer = data.getPointer(64); // buffer set by scePsmfPlayerSetTempBuf()
    	int totalPacketsRead;
    	if (readBuffer.isNull()) {
    		result = readFile(data, dataAddress, numPackets << 11);
    		if (result < 0) {
    			return result;
    		}
    		if ((result & 0x7FF) != 0) {
    			return -1;
    		}
    		totalPacketsRead = result >> 11;
    	} else {
    		totalPacketsRead = 0;
    		int dataAddressOffset = 0;
    		while (numPackets > 0) {
    			int readNumPackets = Math.min(numPackets, 32);
    			result = readFile(data, readBuffer, readNumPackets << 11);
    			if (result < 0) {
    				return result;
    			}
        		if ((result & 0x7FF) != 0) {
        			return -1;
        		}
        		int readLength = result;
        		totalPacketsRead += readLength >> 11;
    			dataAddress.memcpy(dataAddressOffset, readBuffer, readLength);
    			dataAddressOffset += readLength;

    			numPackets -= readNumPackets;
    		}
    	}

    	if (log.isTraceEnabled()) {
    		log.trace(String.format("hlePsmfPlayerRingbufferPutCallback returning 0x%X packets: %s", totalPacketsRead, Utilities.getMemoryDump(dataAddress, totalPacketsRead << 11)));
    	}

    	return totalPacketsRead;
    }

    public int getPlayerStatus(TPointer playerStatusAddr) {
    	return playerStatusAddr.getValue32(20);
    }

    public int getPlayerVersion(TPointer playerStatusAddr) {
    	return playerStatusAddr.getValue32(8);
    }

    public void setPlayerStatus(TPointer playerStatusAddr, int status) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("setPlayerStatus status=0x%X", status));
		}
        playerStatusAddr.setValue32(20, status);
    }

    public void setPlayerStatusError(TPointer playerStatusAddr) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("setPlayerStatusError"));
		}
    	setPlayerStatus(playerStatusAddr, PSMF_PLAYER_STATUS_ERROR);
    }

    public void setPlayerStatusError(TPointer playerStatusAddr, int errorCode) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("setPlayerStatusError errorCode=0x%X", errorCode));
		}
    	setPlayerStatus(playerStatusAddr, PSMF_PLAYER_STATUS_ERROR);
    	playerStatusAddr.setValue32(0, errorCode);
    }

    public boolean isPlayerStatusOK(TPointer playerStatusAddr) {
    	return (getPlayerStatus(playerStatusAddr) & PSMF_PLAYER_STATUS_ERROR) == 0;
    }

    private void setReadStatus(TPointer readControlAddr, int readStatus) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("setReadStatus readStatus=0x%X", readStatus));
		}
		AtomicValue.set(readControlAddr.getPointer(4), readStatus); // ScePsmfPlayerReadStatus

		int eventFlag = readControlAddr.getValue32(0);
		hleSyscall(Modules.ThreadManForUserModule.sceKernelClearEventFlag(eventFlag, 0));
		hleSyscall(Modules.ThreadManForUserModule.sceKernelSetEventFlag(eventFlag, readStatus));
    }

    private int getVblankSyncCount(TPointer avSyncControlAddr) {
    	return avSyncControlAddr.getValue32(48);
    }

    private void setVblankSyncCount(TPointer avSyncControlAddr, int value) {
    	avSyncControlAddr.setValue32(48, value);
    }

    private void clearVblankSyncCount(TPointer avSyncControlAddr) {
    	setVblankSyncCount(avSyncControlAddr, 0);
    }

    private int getVblankSyncInterval(TPointer avSyncControlAddr) {
    	return avSyncControlAddr.getValue32(52);
    }

    private void setVblankSyncInterval(TPointer avSyncControlAddr, int value) {
    	avSyncControlAddr.setValue32(52, value);
    }

    public TPointer32 checkPlayerInitialized0(TPointer32 psmfPlayer) {
    	TPointer data = psmfPlayer.getPointer();
    	if (data.isNull()) {
    		throw new SceKernelErrorException(ERROR_PSMFPLAYER_NOT_INITIALIZED);
    	}
        TPointer playerStatusAddr = data.getPointer(24);
    	if (playerStatusAddr.isNull()) {
    		throw new SceKernelErrorException(ERROR_PSMFPLAYER_NOT_INITIALIZED);
    	}

    	return psmfPlayer;
    }

    public TPointer32 checkPlayerInitialized(TPointer32 psmfPlayer) {
    	psmfPlayer = checkPlayerInitialized0(psmfPlayer);
    	TPointer data = psmfPlayer.getPointer();
        TPointer playerStatusAddr = data.getPointer(24);
    	if (getPlayerStatus(playerStatusAddr) == PSMF_PLAYER_STATUS_NONE) {
    		throw new SceKernelErrorException(ERROR_PSMFPLAYER_NOT_INITIALIZED);
    	}

    	return psmfPlayer;
    }

    public TPointer32 checkPlayerStandby(TPointer32 psmfPlayer) {
    	psmfPlayer = checkPlayerInitialized0(psmfPlayer);
    	TPointer data = psmfPlayer.getPointer();
        TPointer playerStatusAddr = data.getPointer(24);
    	if (getPlayerStatus(playerStatusAddr) < PSMF_PLAYER_STATUS_STANDBY) {
    		throw new SceKernelErrorException(ERROR_PSMFPLAYER_NOT_INITIALIZED);
    	}

    	return psmfPlayer;
    }

    public TPointer32 checkPlayerPlaying(TPointer32 psmfPlayer) {
    	psmfPlayer = checkPlayerInitialized0(psmfPlayer);
    	TPointer data = psmfPlayer.getPointer();
        TPointer playerStatusAddr = data.getPointer(24);
    	int playerStatus = getPlayerStatus(playerStatusAddr);
    	if (playerStatus != PSMF_PLAYER_STATUS_PLAYING && playerStatus != PSMF_PLAYER_STATUS_PLAYING_FINISHED && playerStatus != PSMF_PLAYER_STATUS_ERROR) {
    		throw new SceKernelErrorException(ERROR_PSMFPLAYER_NOT_INITIALIZED);
    	}

    	return psmfPlayer;
    }

    private TPointer allocate(int size) {
    	TPointer32 dataAddr = allocatePointer32(4);
    	int result = Modules.ThreadManForUserModule.sceKernelTryAllocateVpl(scePsmfPlayerMemoryPool, size, dataAddr);
    	if (result < 0) {
    		return null;
    	}

    	int addr = dataAddr.getValue();

    	TPointer infoAddr = allocatePointer(52);
    	infoAddr.setValue32(0, 52);
    	result = Modules.ThreadManForUserModule.sceKernelReferVplStatus(scePsmfPlayerMemoryPool, infoAddr);
    	if (result == 0) {
    		int poolSize = infoAddr.getValue32(40);
    		int freeSize = infoAddr.getValue32(44);
    		int allocatedSize = poolSize - freeSize;
    		if (allocatedSize > scePsmfPlayerMemoryPoolMaxAllocatedSize) {
    			scePsmfPlayerMemoryPoolMaxAllocatedSize = allocatedSize;
    		}
    	}

    	return new TPointer(getMemory(), addr);
	}

    private void free(TPointer pointer) {
    	Modules.ThreadManForUserModule.sceKernelFreeVpl(scePsmfPlayerMemoryPool, pointer);
    }

    private TPointer allocateAtomicValue() {
    	TPointer atomicValue = allocate(8);
    	atomicValue.clear(8);

    	TPointer lwMutex = allocate(32);
    	atomicValue.setPointer(0, lwMutex);

    	int result = Modules.ThreadManForUserModule.sceKernelCreateLwMutex(lwMutex, "_ScePsmfPlayerStatusLwMutex", 0, 0, TPointer.NULL);
    	if (result < 0) {
    		free(lwMutex);
    		atomicValue.setValue32(0, 0);
    	}

    	return atomicValue;
    }

    private void freeAtomicValue(TPointer atomicValue) {
    	if (atomicValue.isNotNull()) {
	    	TPointer lwMutex = atomicValue.getPointer(0);
	    	if (lwMutex.isNotNull()) {
	    		Modules.ThreadManForUserModule.sceKernelDeleteLwMutex(lwMutex);
	    		free(lwMutex);
	    		atomicValue.setPointer(0, TPointer.NULL);
	    	}

	    	free(atomicValue);
    	}
    }

    private int initData(TPointer32 psmfPlayer, int threadPriority, TPointer ringbufferData, TPointer controlBuffer, int controlBufferSize, TPointer videoBuffer, int videoBufferSize, TPointer audioBuffer, int audioBufferSize, TPointer yuv420Buffer) {
		TPointer data = psmfPlayer.getPointer();
        data.clear(72);

        TPointer filenameAddr = allocate(513);
        if (filenameAddr == null) {
        	return ERROR_PSMFPLAYER_NO_MEMORY;
        }
        data.setPointer(0, filenameAddr);

        TPointer strFileAddr = allocate(16);
        if (strFileAddr == null) {
        	return ERROR_PSMFPLAYER_NO_MEMORY;
        }
        data.setPointer(52, strFileAddr);

        int result = Modules.ThreadManForUserModule.sceKernelCreateEventFlag("ScePsmfPlayerAbortThreadEvf", 0, 0, TPointer.NULL);
        if (result < 0) {
        	return result;
        }
        scePsmfPlayerAbortThreadEvf = result;

        result = Modules.ThreadManForUserModule.sceKernelCreateThread("ScePsmfPlayerAbortThread", scePsmfPlayerAbortThreadEntry, threadPriority, 0x3000, 0, 0);
        if (result < 0) {
        	return result;
        }
        scePsmfPlayerAbortThread = result;

        result = hleSyscall(Modules.ThreadManForUserModule.sceKernelStartThread(scePsmfPlayerAbortThread, 0, TPointer.NULL));
        if (result < 0) {
        	return result;
        }

        result = Modules.ThreadManForUserModule.sceKernelCreateThread("ScePsmfPlayerMCThread", scePsmfPlayerMCThreadEntry, threadPriority, 0x800, 0, 0);
        if (result < 0) {
        	return result;
        }
        data.setValue32(16, result);

        int ringbufferNumPackets = Modules.sceMpegModule.sceMpegRingbufferQueryPackNum(ringbufferSize);

        TPointer ringbufferAddr = allocate(128);
        if (ringbufferAddr == null) {
        	return ERROR_PSMFPLAYER_NO_MEMORY;
        }
        data.setPointer(8, ringbufferAddr);

        result = Modules.sceMpegModule.sceMpegRingbufferConstruct(ringbufferAddr, ringbufferNumPackets, ringbufferData, ringbufferSize, new TPointer(data.getMemory(), scePsmfPlayerRingbufferPutEntry), psmfPlayer.getAddress());
        if (result < 0) {
        	return result;
        }

        result = Modules.sceMpegModule.sceMpegQueryMemSize(0);
        if (result < 0) {
        	return result;
        }
        int mpegMemSize = result;

        TPointer mpegData = allocate(mpegMemSize);
        if (mpegData == null) {
        	return ERROR_PSMFPLAYER_NO_MEMORY;
        }
        data.setPointer(60, mpegData);

        TPointer mpeg = allocate(4);
        if (mpeg == null) {
        	return ERROR_PSMFPLAYER_NO_MEMORY;
        }
        data.setPointer(4, mpeg);

        result = Modules.sceMpegModule.sceMpegCreate(new TPointer32(mpeg), mpegData, mpegMemSize, ringbufferAddr, frameWidth, 0, 0);
        if (result < 0) {
        	return result;
        }

        TPointer playerStatusAddr = allocate(36);
        if (playerStatusAddr == null) {
        	return ERROR_PSMFPLAYER_NO_MEMORY;
        }
        data.setPointer(24, playerStatusAddr);

        playerStatusAddr.clear(36);
        playerStatusAddr.setValue32(8, PSMF_PLAYER_VERSION_BASIC);
        playerStatusAddr.setValue32(12, PSMF_PLAYER_NOT_LOOPING);
        playerStatusAddr.setValue32(16, 1);
        result = Modules.ThreadManForUserModule.sceKernelCreateEventFlag("ScePsmfPlayerWaitFlag", EventFlagManager.PSP_EVENT_WAITMULTIPLE, 0, TPointer.NULL);
        if (result < 0) {
        	return result;
        }
        playerStatusAddr.setValue32(24, result);

        TPointer psmfControlAddr = allocate(180);
        if (psmfControlAddr == null) {
        	return ERROR_PSMFPLAYER_NO_MEMORY;
        }
        data.setPointer(28, psmfControlAddr);

        psmfControlAddr.clear(180);
        psmfControlAddr.setValue32(136, -1);
        psmfControlAddr.setValue32(140, -1);
        psmfControlAddr.setValue32(144, -1);
        psmfControlAddr.setValue32(148, -1);
        psmfControlAddr.setValue32(152, -1);
        psmfControlAddr.setValue32(156, -1);
        psmfControlAddr.setValue32(160, -1);
        psmfControlAddr.setValue32(164, -1);
        psmfControlAddr.setPointer(168, controlBuffer);
        psmfControlAddr.setValue32(172, controlBufferSize);
        psmfControlAddr.setValue32(176, 0);

        TPointer avSyncControlAddr = allocate(56);
        if (avSyncControlAddr == null) {
        	return ERROR_PSMFPLAYER_NO_MEMORY;
        }
        data.setPointer(32, avSyncControlAddr);
        avSyncControlAddr.clear(56);
        avSyncControlAddr.setPointer(12, allocateAtomicValue()); // ScePsmfPlayerASCReadyFlag
        avSyncControlAddr.setPointer(16, allocateAtomicValue()); // ScePsmfPlayerASCAudioFlag

        TPointer decodeControlAddr = allocate(84);
        if (decodeControlAddr == null) {
        	return ERROR_PSMFPLAYER_NO_MEMORY;
        }
        data.setPointer(36, decodeControlAddr);

        decodeControlAddr.clear(84);
        decodeControlAddr.setPointer(0, mpeg);
        decodeControlAddr.setPointer(4, ringbufferAddr);
        decodeControlAddr.setValue32(68, TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888);
        decodeControlAddr.setPointer(72, allocateAtomicValue()); // ScePsmfPlayerDecodeStatus

        result = Modules.ThreadManForUserModule.sceKernelCreateThread("ScePsmfPlayerDecodeThread", scePsmfPlayerDecodeThreadEntry, threadPriority + 1, 0x800, 0, 0);
        if (result < 0) {
        	return result;
        }
        decodeControlAddr.setValue32(56, result);

        TPointer avcDecodeAddr = allocate(1128);
        if (avcDecodeAddr == null) {
        	return ERROR_PSMFPLAYER_NO_MEMORY;
        }
        decodeControlAddr.setPointer(24, avcDecodeAddr);
        avcDecodeAddr.clear(1128);
        initCommonDecode(avcDecodeAddr, mpeg);
        result = Modules.sceMpegModule.sceMpegMallocAvcEsBuf(new TPointer32(mpeg));
        if (result == 0) {
        	return SceKernelErrors.ERROR_PSMFPLAYER_FATAL;
        }
        avcDecodeAddr.setValue32(1100, result);
        final int numberPtsBuffers = 3;
        TPointer ptsBufferAddr = allocate(numberPtsBuffers * 4);
        if (ptsBufferAddr == null) {
        	return ERROR_PSMFPLAYER_NO_MEMORY;
        }
        avcDecodeAddr.setPointer(1104, ptsBufferAddr);
        avcDecodeAddr.setValue32(1120, numberPtsBuffers);

        TPointer atracDecodeAddr = allocate(1120);
        if (atracDecodeAddr == null) {
        	return ERROR_PSMFPLAYER_NO_MEMORY;
        }
        decodeControlAddr.setPointer(28, atracDecodeAddr);
        atracDecodeAddr.clear(1120);
        initCommonDecode(atracDecodeAddr, mpeg);
        TPointer32 esSizeAddr = allocatePointer32(4);
        TPointer32 outSizeAddr = allocatePointer32(4);
        result = Modules.sceMpegModule.sceMpegQueryAtracEsSize(new TPointer32(mpeg), esSizeAddr, outSizeAddr);
        if (result != 0) {
        	return result;
        }
        TPointer atracEsBufferAddr = allocate(esSizeAddr.getValue() + 64);
        atracDecodeAddr.setPointer(1100, atracEsBufferAddr);
        atracEsBufferAddr.alignUp(63);
        atracDecodeAddr.setPointer(1104, atracEsBufferAddr);
        int atracOutputSize = outSizeAddr.getValue();
        atracDecodeAddr.setValue32(1108, atracOutputSize);

        TPointer pcmDecodeAddr = allocate(1160);
        if (pcmDecodeAddr == null) {
        	return ERROR_PSMFPLAYER_NO_MEMORY;
        }
        decodeControlAddr.setPointer(32, pcmDecodeAddr);
        pcmDecodeAddr.clear(1160);
        initCommonDecode(pcmDecodeAddr, mpeg);
        TPointer pcmPoolAddr = allocate(0xA000);
        if (pcmPoolAddr == null) {
        	return ERROR_PSMFPLAYER_NO_MEMORY;
        }
        pcmDecodeAddr.setPointer(1116, pcmPoolAddr);
        TPointer pcmPtsAddr = allocate(512);
        if (pcmPtsAddr == null) {
        	return ERROR_PSMFPLAYER_NO_MEMORY;
        }
        pcmDecodeAddr.setPointer(1120, pcmPtsAddr);
        result = Modules.sceMpegModule.sceMpegQueryPcmEsSize(new TPointer32(mpeg), esSizeAddr, outSizeAddr);
        if (result != 0) {
        	return result;
        }
        TPointer pcmEsBufferAddr = allocate(esSizeAddr.getValue());
        if (pcmEsBufferAddr == null) {
        	return ERROR_PSMFPLAYER_NO_MEMORY;
        }
        pcmDecodeAddr.setPointer(1100, pcmEsBufferAddr);
        pcmDecodeAddr.setValue32(1104, outSizeAddr.getValue());

        TPointer videoBufferAddr = allocate(52);
        if (videoBufferAddr == null) {
        	return ERROR_PSMFPLAYER_NO_MEMORY;
        }
        data.setPointer(40, videoBufferAddr);
        videoBufferAddr.clear(52);
        videoBufferAddr.setPointer(0, mpeg);
        final int numberDisplayBuffers = 3;
        videoBufferAddr.setValue32(20, numberDisplayBuffers);
        videoBufferAddr.setPointer(44, allocateAtomicValue()); // ScePsmfPlayerVBufChangeFlag
        TPointer displayDataArrayAddr = allocate(numberDisplayBuffers * 8);
        if (displayDataArrayAddr == null) {
        	return ERROR_PSMFPLAYER_NO_MEMORY;
        }
        videoBufferAddr.setPointer(4, displayDataArrayAddr);
        for (int i = 0; i < numberDisplayBuffers; i++) {
        	displayDataArrayAddr.setValue32(i * 8 + 0, 0);
        }
        TPointer32 resultAddr = allocatePointer32(4);
        result = Modules.sceMpegModule.sceMpegAvcQueryYCbCrSize(new TPointer32(mpeg), 1, Screen.width, Screen.height, resultAddr);
        if (result != 0) {
        	return result;
        }
        int yCbCrSize = Utilities.alignUp(resultAddr.getValue(), 15);
        videoBufferAddr.setValue32(36, yCbCrSize);
        for (int i = 0; i < numberDisplayBuffers; i++) {
        	displayDataArrayAddr.setValue32(i * 8 + 0, videoBuffer.getAddress() + i * yCbCrSize);
        }
        int size = yCbCrSize * numberDisplayBuffers;
        videoBufferAddr.setValue32(24, videoBuffer.getAddress() + size);
        for (int i = 0; i < numberDisplayBuffers; i++) {
        	result = Modules.sceMpegModule.sceMpegAvcInitYCbCr(new TPointer32(mpeg), 1, Screen.width, Screen.height, displayDataArrayAddr.getPointer(i * 8 + 0));
        	if (result != 0) {
        		return result;
        	}
        }
    	result = Modules.sceMpegModule.sceMpegAvcInitYCbCr(new TPointer32(mpeg), 1, Screen.width, Screen.height, videoBufferAddr.getPointer(24));
    	if (result != 0) {
    		return result;
    	}

    	TPointer audioBufferAddr = allocate(32);
    	if (audioBufferAddr == null) {
    		return ERROR_PSMFPLAYER_NO_MEMORY;
    	}
    	data.setPointer(44, audioBufferAddr);
    	audioBufferAddr.clear(32);
    	final int numberAudioBuffers = 3;
    	audioBufferAddr.setValue32(16, numberAudioBuffers);
    	audioBufferAddr.setValue32(20, Utilities.alignUp(atracOutputSize, 63));
    	TPointer audioDataAddr = allocate(numberAudioBuffers * 8);
    	if (audioDataAddr == null) {
    		return ERROR_PSMFPLAYER_NO_MEMORY;
    	}
    	audioBufferAddr.setPointer(0, audioDataAddr);
    	for (int i = 0; i < numberAudioBuffers; i++) {
    		audioDataAddr.setValue32(i * 8 + 0, audioBuffer.getAddress() + i * audioBufferAddr.getValue32(20));
    	}

    	TPointer readControlAddr = allocate(68);
    	if (readControlAddr == null) {
    		return ERROR_PSMFPLAYER_NO_MEMORY;
    	}
    	data.setPointer(48, readControlAddr);
    	readControlAddr.clear(68);
    	readControlAddr.setPointer(44, mpeg);
    	readControlAddr.setPointer(48, ringbufferAddr);
    	readControlAddr.setValue32(52, ringbufferNumPackets);
    	result = Modules.ThreadManForUserModule.sceKernelCreateEventFlag("ScePsmfPlayerReadStatus", 0, 0, TPointer.NULL);
    	if (result < 0) {
    		return result;
    	}
    	readControlAddr.setValue32(0, result);
    	readControlAddr.setPointer(4, allocateAtomicValue()); // ScePsmfPlayerReadStatus
    	result = Modules.ThreadManForUserModule.sceKernelCreateThread("ScePsmfPlayerReadThread", scePsmfPlayerReadThreadEntry, threadPriority + 2, 0x3000, 0, 0);
    	if (result < 0) {
    		return result;
    	}
    	readControlAddr.setValue32(12, result);

    	avSyncControlAddr.setPointer(24, videoBufferAddr);
    	avSyncControlAddr.setPointer(28, audioBufferAddr);
    	avSyncControlAddr.setPointer(32, decodeControlAddr);
    	avSyncControlAddr.setPointer(36, readControlAddr);
    	avSyncControlAddr.setPointer(40, playerStatusAddr);
    	avSyncControlAddr.setPointer(44, psmfControlAddr);

    	decodeControlAddr.setPointer(36, playerStatusAddr);
    	decodeControlAddr.setPointer(40, psmfControlAddr);
    	decodeControlAddr.setPointer(44, readControlAddr);
    	decodeControlAddr.setPointer(48, videoBufferAddr);
    	decodeControlAddr.setPointer(52, audioBufferAddr);
    	decodeControlAddr.setPointer(80, yuv420Buffer);

    	readControlAddr.setPointer(16, playerStatusAddr);
    	readControlAddr.setPointer(56, strFileAddr);

    	psmfControlAddr.setPointer(176, strFileAddr);

    	setPlayerStatus(playerStatusAddr, PSMF_PLAYER_STATUS_INIT);

    	return 0;
	}

	private void freeData(TPointer32 psmfPlayer) {
		TPointer data = psmfPlayer.getPointer();

		TPointer filenameAddr = data.getPointer(0);
		if (filenameAddr.isNotNull()) {
			free(filenameAddr);
			data.setPointer(0, TPointer.NULL);
		}

        int scePsmfPlayerMCThread = data.getValue32(16);
		if (scePsmfPlayerMCThread > 0) {
			hleSyscall(Modules.ThreadManForUserModule.sceKernelDeleteThread(scePsmfPlayerMCThread));
			data.setValue32(16, 0);
		}

        TPointer playerStatusAddr = data.getPointer(24);
		if (playerStatusAddr.isNotNull()) {
			int scePsmfPlayerWaitFlag = playerStatusAddr.getValue32(24);
			if (scePsmfPlayerWaitFlag > 0) {
				hleSyscall(Modules.ThreadManForUserModule.sceKernelDeleteEventFlag(scePsmfPlayerWaitFlag));
				playerStatusAddr.setValue32(24, 0);
			}
			free(playerStatusAddr);
			data.setPointer(24, TPointer.NULL);
		}

        TPointer psmfControlAddr = data.getPointer(28);
        if (psmfControlAddr.isNotNull()) {
        	TPointer controlBuffer = psmfControlAddr.getPointer(168);
        	if (controlBuffer.isNotNull()) {
        		psmfControlAddr.setPointer(168, TPointer.NULL);
        		psmfControlAddr.setValue32(172, 0); // controlBufferSize
        	}
        	free(psmfControlAddr);
        	data.setPointer(28, TPointer.NULL);
        }

        TPointer avSyncControlAddr = data.getPointer(32);
        if (avSyncControlAddr.isNotNull()) {
        	freeAtomicValue(avSyncControlAddr.getPointer(12)); // ScePsmfPlayerASCReadyFlag
        	avSyncControlAddr.setPointer(12, TPointer.NULL); // ScePsmfPlayerASCReadyFlag
        	freeAtomicValue(avSyncControlAddr.getPointer(16)); // ScePsmfPlayerASCAudioFlag
        	avSyncControlAddr.setPointer(16, TPointer.NULL); // ScePsmfPlayerASCAudioFlag
        	free(avSyncControlAddr);
        	data.setPointer(32, TPointer.NULL);
        }

        TPointer decodeControlAddr = data.getPointer(36);
        if (decodeControlAddr.isNotNull()) {
        	unregisterAllStreams(data);
        	freeAtomicValue(decodeControlAddr.getPointer(72)); // ScePsmfPlayerDecodeStatus
        	decodeControlAddr.setPointer(72, TPointer.NULL); // ScePsmfPlayerDecodeStatus
            int scePsmfPlayerDecodeThread = decodeControlAddr.getValue32(56);
        	if (scePsmfPlayerDecodeThread > 0) {
        		hleSyscall(Modules.ThreadManForUserModule.sceKernelDeleteThread(scePsmfPlayerDecodeThread));
        		decodeControlAddr.setValue32(56, 0);
        	}
            TPointer avcDecodeAddr = decodeControlAddr.getPointer(24);
            if (avcDecodeAddr.isNotNull()) {
            	TPointer mpeg = avcDecodeAddr.getPointer(0);
            	TPointer32 mpeg32 = new TPointer32(mpeg);
            	int esBuffer = avcDecodeAddr.getValue32(1100);
            	if (esBuffer != 0) {
            		Modules.sceMpegModule.sceMpegFreeAvcEsBuf(mpeg32, esBuffer);
            		avcDecodeAddr.setValue32(1100, 0);
            	}
            	TPointer ptsBufferAddr = avcDecodeAddr.getPointer(1104);
            	if (ptsBufferAddr.isNotNull()) {
            		free(ptsBufferAddr);
            		avcDecodeAddr.setPointer(1104, TPointer.NULL);
            	}
            	free(avcDecodeAddr);
            	decodeControlAddr.setPointer(24, TPointer.NULL);
            }
            TPointer atracDecodeAddr = decodeControlAddr.getPointer(28);
            if (atracDecodeAddr.isNotNull()) {
                TPointer atracEsBufferAddr = atracDecodeAddr.getPointer(1100);
                if (atracEsBufferAddr.isNotNull()) {
                	free(atracEsBufferAddr);
                	atracDecodeAddr.setPointer(1100, TPointer.NULL);
                	atracDecodeAddr.setPointer(1104, TPointer.NULL);
                }
            	free(atracDecodeAddr);
            	decodeControlAddr.setPointer(28, TPointer.NULL);
            }
            TPointer pcmDecodeAddr = decodeControlAddr.getPointer(32);
            if (pcmDecodeAddr.isNotNull()) {
                TPointer pcmEsBufferAddr = pcmDecodeAddr.getPointer(1100);
                if (pcmEsBufferAddr.isNotNull()) {
                	free(pcmEsBufferAddr);
                	pcmDecodeAddr.setPointer(1100, TPointer.NULL);
                }
                TPointer pcmPoolAddr = pcmDecodeAddr.getPointer(1116);
                if (pcmPoolAddr.isNotNull()) {
                	free(pcmPoolAddr);
                	pcmDecodeAddr.setPointer(1116, TPointer.NULL);
                }
                TPointer pcmPtsAddr = pcmDecodeAddr.getPointer(1120);
                if (pcmPtsAddr.isNotNull()) {
                	free(pcmPtsAddr);
                	pcmDecodeAddr.setPointer(1120, TPointer.NULL);
                }
            	free(pcmDecodeAddr);
            	decodeControlAddr.setPointer(32, TPointer.NULL);
            }
        	free(decodeControlAddr);
        	data.setPointer(36, TPointer.NULL);
        }

        TPointer videoBufferAddr = data.getPointer(40);
        if (videoBufferAddr.isNotNull()) {
        	freeAtomicValue(videoBufferAddr.getPointer(44)); // ScePsmfPlayerVBufChangeFlag
        	videoBufferAddr.setPointer(44, TPointer.NULL); // ScePsmfPlayerVBufChangeFlag
        	TPointer displayDataArrayAddr = videoBufferAddr.getPointer(4);
        	if (displayDataArrayAddr.isNotNull()) {
        		int numberDisplayBuffers = videoBufferAddr.getValue32(20);
                for (int i = 0; i < numberDisplayBuffers; i++) {
                	displayDataArrayAddr.setPointer(i * 8 + 0, TPointer.NULL);
                }
        		free(displayDataArrayAddr);
        		videoBufferAddr.setPointer(4, TPointer.NULL);
        		videoBufferAddr.setPointer(24, TPointer.NULL);
        	}
        	free(videoBufferAddr);
        	data.setPointer(40, TPointer.NULL);
        }

        TPointer audioBufferAddr = data.getPointer(44);
        if (audioBufferAddr.isNotNull()) {
        	TPointer audioDataAddr = audioBufferAddr.getPointer(0);
        	if (audioDataAddr.isNotNull()) {
        		int numberAudioBuffers = audioBufferAddr.getValue32(16);
            	for (int i = 0; i < numberAudioBuffers; i++) {
            		audioDataAddr.setPointer(i * 8 + 0, TPointer.NULL);
            	}
            	free(audioDataAddr);
            	audioBufferAddr.setPointer(0, TPointer.NULL);
        	}
        	free(audioBufferAddr);
        	data.setPointer(44, TPointer.NULL);
        }

        TPointer readControlAddr = data.getPointer(48);
        if (readControlAddr.isNotNull()) {
        	int scePsmfPlayerReadStatus = readControlAddr.getValue32(0);
        	if (scePsmfPlayerReadStatus > 0) {
        		hleSyscall(Modules.ThreadManForUserModule.sceKernelDeleteEventFlag(scePsmfPlayerReadStatus));
        		readControlAddr.setValue32(0, 0);
        	}
        	freeAtomicValue(readControlAddr.getPointer(4)); // ScePsmfPlayerReadStatus
        	readControlAddr.setPointer(4, TPointer.NULL);
            int scePsmfPlayerReadThread = readControlAddr.getValue32(12);
            if (scePsmfPlayerReadStatus > 0) {
            	hleSyscall(Modules.ThreadManForUserModule.sceKernelDeleteThread(scePsmfPlayerReadThread));
            	readControlAddr.setValue32(12, 0);
            }
        	free(readControlAddr);
        	data.setPointer(48, TPointer.NULL);
        }

        TPointer strFileAddr = data.getPointer(52);
        if (strFileAddr.isNotNull()) {
        	hleSyscall(Modules.ThreadManForUserModule.sceKernelSetEventFlag(scePsmfPlayerAbortThreadEvf, 0x4));

        	hleSyscall(Modules.ThreadManForUserModule.sceKernelWaitThreadEnd(scePsmfPlayerAbortThread, TPointer32.NULL));

        	if (scePsmfPlayerAbortThreadEvf > 0) {
        		hleSyscall(Modules.ThreadManForUserModule.sceKernelDeleteEventFlag(scePsmfPlayerAbortThreadEvf));
        		scePsmfPlayerAbortThreadEvf = 0;
        	}
        	if (scePsmfPlayerAbortThread > 0) {
        		hleSyscall(Modules.ThreadManForUserModule.sceKernelDeleteThread(scePsmfPlayerAbortThread));
        		scePsmfPlayerAbortThread = 0;
        	}
        	free(strFileAddr);
        	data.setPointer(52, strFileAddr);
        }

        TPointer mpeg = data.getPointer(4);
        if (mpeg.isNotNull()) {
        	TPointer32 mpeg32 = new TPointer32(mpeg);
        	Modules.sceMpegModule.sceMpegDelete(mpeg32);
        	free(mpeg);
        	data.setPointer(4, TPointer.NULL);
        }

        TPointer mpegData = data.getPointer(60);
        if (mpegData.isNotNull()) {
        	free(mpegData);
        	data.setPointer(60, TPointer.NULL);
        }

        TPointer ringbufferAddr = data.getPointer(8);
        if (ringbufferAddr.isNotNull()) {
        	Modules.sceMpegModule.sceMpegRingbufferDestruct(ringbufferAddr);
        	free(ringbufferAddr);
        	data.setPointer(8, TPointer.NULL);
        }

        data.setValue32(56, 0);

        if (data.isNotNull()) {
        	free(data);
        	psmfPlayer.setPointer(TPointer.NULL);
        }

        if (scePsmfPlayerMemoryPool >= 0) {
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("scePsmfPlayerMemoryPoolMaxAllocatedSize=0x%X", scePsmfPlayerMemoryPoolMaxAllocatedSize));
        	}
        	Modules.ThreadManForUserModule.sceKernelDeleteVpl(scePsmfPlayerMemoryPool);
        	scePsmfPlayerMemoryPool = -1;
        	scePsmfPlayerMemoryPoolMaxAllocatedSize = 0;
        }
	}

	private void initHLE() {
		int moduleMemory = getModuleMemory();

		// Install HLE threads
		scePsmfPlayerMCThreadEntry = moduleMemory;
		moduleMemory += 0x10;
		HLEUtilities.getInstance().installHLEThread(scePsmfPlayerMCThreadEntry, this, "hlePsmfPlayerMCThread");

		scePsmfPlayerOpenControlThreadEntry = moduleMemory;
		moduleMemory += 0x10;
		HLEUtilities.getInstance().installHLEThread(scePsmfPlayerOpenControlThreadEntry, this, "hlePsmfPlayerOpenControlThread");

		scePsmfPlayerDecodeThreadEntry = moduleMemory;
		moduleMemory += 0x10;
		HLEUtilities.getInstance().installHLEThread(scePsmfPlayerDecodeThreadEntry, this, "hlePsmfPlayerDecodeThread");

		scePsmfPlayerReadThreadEntry = moduleMemory;
		moduleMemory += 0x10;
		HLEUtilities.getInstance().installHLEThread(scePsmfPlayerReadThreadEntry, this, "hlePsmfPlayerReadThread");

		scePsmfPlayerAbortThreadEntry = moduleMemory;
		moduleMemory += 0x10;
		HLEUtilities.getInstance().installHLEThread(scePsmfPlayerAbortThreadEntry, this, "hlePsmfPlayerAbortThread");

		// Install callback
		scePsmfPlayerRingbufferPutEntry = moduleMemory;
		moduleMemory += 0x10;
		HLEUtilities.getInstance().installHLESyscall(scePsmfPlayerRingbufferPutEntry, this, "hlePsmfPlayerRingbufferPutCallback");
	}

	private void initCommonDecode(TPointer commonDecodeAddr, TPointer mpeg) {
		commonDecodeAddr.setPointer(0, mpeg);
		commonDecodeAddr.setValue32(8, 0); // number of registered streams
        for (int i = 0; i < 16; i++) {
        	commonDecodeAddr.setValue32(12 + i * 4, 0); // registered avc streamAddr
        }
	}

	private int registerStream(TPointer commonDecodeAddr, int streamType, int streamChannelNum) {
		int numberOfRegisteredStreams = commonDecodeAddr.getValue32(8);
		if (numberOfRegisteredStreams >= 16) {
			return SceKernelErrors.ERROR_PSMFPLAYER_FATAL;
		}
		TPointer32 mpeg = new TPointer32(commonDecodeAddr.getPointer(0));
		int result = Modules.sceMpegModule.sceMpegRegistStream(mpeg, streamType, streamChannelNum);
    	if (log.isTraceEnabled()) {
    		log.trace(String.format("registerStream: sceMpegRegistStream streamType=0x%X, streamChannelNum=0x%X, numberOfRegisteredStreams=%d returned 0x%X", streamType, streamChannelNum, numberOfRegisteredStreams, result));
    	}
		if (result < 0) {
			return result;
		}

		commonDecodeAddr.setValue32(12 + numberOfRegisteredStreams * 4, result);
		commonDecodeAddr.setValue32(8, numberOfRegisteredStreams + 1);

		return 0;
	}

	private int registerStreams(TPointer32 psmf, TPointer commonDecodeAddr, int streamType, int numberOfStreams) {
		int result = 0;

		for (int i = 0; i < numberOfStreams; i++) {
    		result = Modules.scePsmfModule.scePsmfSpecifyStreamWithStreamType(psmf, streamType, i);
    		if (result != 0) {
    			return result;
    		}

    		TPointer32 typeAddr = allocatePointer32(4);
    		TPointer32 channelAddr = allocatePointer32(4);
    		result = Modules.scePsmfModule.scePsmfGetCurrentStreamType(psmf, typeAddr, channelAddr);
    		if (result != 0) {
    			return result;
    		}

    		result = registerStream(commonDecodeAddr, streamType, channelAddr.getValue());
    		if (result != 0) {
    			return result;
    		}
    	}

		return result;
	}

	private void unregisterStreams(TPointer commonDecodeAddr) {
		int numberRegisteredStreams = commonDecodeAddr.getValue32(8);
		for (int i = 0; i < numberRegisteredStreams; i++) {
			Modules.sceMpegModule.sceMpegUnRegistStream(new TPointer32(commonDecodeAddr.getPointer(0)), commonDecodeAddr.getPointer(12 + i * 4));
		}
		numberRegisteredStreams = 0;
		commonDecodeAddr.setValue32(8, numberRegisteredStreams);
	}

	private void unregisterAllStreams(TPointer data) {
        TPointer decodeControlAddr = data.getPointer(36);

        TPointer avcDecodeAddr = decodeControlAddr.getPointer(24);
        unregisterStreams(avcDecodeAddr);

        TPointer atracDecodeAddr = decodeControlAddr.getPointer(28);
        unregisterStreams(atracDecodeAddr);

        TPointer pcmDecodeAddr = decodeControlAddr.getPointer(32);
        unregisterStreams(pcmDecodeAddr);
	}

	private int commonChangeGetAuMode(TPointer commonDecodeAddr) {
    	int numberRegisteredStreams = commonDecodeAddr.getValue32(8);
    	TPointer mpeg = commonDecodeAddr.getPointer(0);
    	TPointer32 mpeg32 = new TPointer32(mpeg);
    	for (int i = 0; i < numberRegisteredStreams; i++) {
    		int result = Modules.sceMpegModule.sceMpegChangeGetAuMode(mpeg32, commonDecodeAddr.getPointer(12 + i * 4), 1);
        	if (log.isTraceEnabled()) {
        		log.trace(String.format("commonChangeGetAuMode: sceMpegChangeGetAuMode returned 0x%X", result));
        	}
    		if (result != 0) {
    			return result;
    		}
    	}
    	commonDecodeAddr.setValue32(4, -1);

    	return 0;
	}

	private void unregisterReadControl(TPointer data) {
    	TPointer readControlAddr = data.getPointer(48);

    	closePsmf(readControlAddr);
	}

	private int hlePsmfPlayerSetPsmf(TPointer32 psmfPlayer, PspString fileAddr, int offset, boolean doCallbacks) {
		TPointer data = psmfPlayer.getPointer();
        TPointer playerStatusAddr = data.getPointer(24);
		int result;

		if (getPlayerStatus(playerStatusAddr) == PSMF_PLAYER_STATUS_INIT) {
			TPointer videoBufferAddr = data.getPointer(40);
			videoBufferAddr.setValue32(8, 0);
			videoBufferAddr.setValue32(12, 0);
			videoBufferAddr.setValue32(16, 0);
			videoBufferAddr.setValue32(32, 0);
			TPointer filenameAddr = data.getPointer(0);
			Modules.SysclibForKernelModule.strncpy(filenameAddr, fileAddr.getPointer(), 512);
			filenameAddr.setValue8(512, (byte) 0);

			data.setValue32(68, offset);
			int currentThreadPriority = Modules.ThreadManForUserModule.sceKernelGetThreadCurrentPriority();
			result = Modules.ThreadManForUserModule.sceKernelCreateThread("ScePsmfPlayerOpenControlThread", scePsmfPlayerOpenControlThreadEntry, currentThreadPriority, 0x3000, 0, 0);
			if (result >= 0) {
				int scePsmfPlayerOpenControlThread = result;
				data.setValue32(12, scePsmfPlayerOpenControlThread);
				final int threadDataSize = 4;
				TPointer threadData = allocatePointer(threadDataSize);
				threadData.setPointer(psmfPlayer);
				result = hleSyscall(Modules.ThreadManForUserModule.sceKernelStartThread(scePsmfPlayerOpenControlThread, threadDataSize, threadData));
				if (result == 0) {
					if (doCallbacks) {
						result = hleSyscall(Modules.ThreadManForUserModule.sceKernelWaitThreadEndCB(scePsmfPlayerOpenControlThread, TPointer32.NULL));
					} else {
						result = hleSyscall(Modules.ThreadManForUserModule.sceKernelWaitThreadEnd(scePsmfPlayerOpenControlThread, TPointer32.NULL));
					}
					int newPlayerStatus;
					if (result >= 0) {
						result = 0;
						newPlayerStatus = PSMF_PLAYER_STATUS_STANDBY;
					} else {
						unregisterAllStreams(data);
						unregisterReadControl(data);
						newPlayerStatus = PSMF_PLAYER_STATUS_INIT;
					}
					setPlayerStatus(playerStatusAddr, newPlayerStatus);
				}
			}
		} else {
			result = ERROR_PSMFPLAYER_NOT_INITIALIZED;
		}

		int scePsmfPlayerOpenControlThread = data.getValue32(12);
		if (scePsmfPlayerOpenControlThread > 0) {
			Modules.ThreadManForUserModule.sceKernelDeleteThread(scePsmfPlayerOpenControlThread);
			data.setValue32(12, 0);
		}

		return result;
	}

	private int setTimestamp(TPointer decodeControlAddr, int timestamp) {
		decodeControlAddr.setValue32(60, timestamp);
		TPointer psmfControlAddr = decodeControlAddr.getPointer(40);
    	TPointer psmf = new TPointer(psmfControlAddr, 24);
    	TPointer32 psmf32 = new TPointer32(psmf);
    	int result = Modules.scePsmfModule.scePsmfGetEPidWithTimestamp(psmf32, timestamp);
    	int entryOffset;
    	if (result == SceKernelErrors.ERROR_PSMF_NOT_FOUND) {
    		entryOffset = 0;
    	} else {
    		int epId = result;
    		if (result == SceKernelErrors.ERROR_PSMF_INVALID_TIMESTAMP) {
    			epId = 0;
    		} else if (result < 0) {
    			return result;
    		}

    		TPointer32 outAddr = allocatePointer32(16);
    		result = Modules.scePsmfModule.scePsmfGetEPWithId(psmf32, epId, outAddr);
    		if (result != 0) {
    			return result;
    		}
    		entryOffset = outAddr.getValue(4);
    	}

    	TPointer readControlAddr = decodeControlAddr.getPointer(44);
    	readControlAddr.setValue32(36, entryOffset);
		setReadStatus(readControlAddr, 0x20);

    	decodeControlAddr.setValue32(8, psmfControlAddr.getValue32(136));
    	decodeControlAddr.setValue32(12, psmfControlAddr.getValue32(140));
    	decodeControlAddr.setValue32(16, psmfControlAddr.getValue32(152));
    	decodeControlAddr.setValue32(20, psmfControlAddr.getValue32(156));
    	AtomicValue.set(decodeControlAddr.getPointer(72), PSMF_PLAYER_DECODE_STATUS_START_PLAY); // ScePsmfPlayerDecodeStatus
    	TPointer videoBufferAddr = decodeControlAddr.getPointer(48);
    	videoBufferAddr.setValue32(40, timestamp);

    	return 0;
	}

	private int initAvcAu(TPointer avcDecodeAddr, int avcStreamMode0) {
        avcDecodeAddr.setValue32(1108, 0);
        avcDecodeAddr.setValue32(1112, 0);
        avcDecodeAddr.setValue32(1116, 0); // current PTS

        TPointer mpeg = avcDecodeAddr.getPointer(0);
        TPointer32 mpeg32 = new TPointer32(mpeg);
        int result = Modules.sceMpegModule.sceMpegAvcDecodeFlush(mpeg32);
    	if (log.isTraceEnabled()) {
    		log.trace(String.format("initAvcAu: sceMpegAvcDecodeFlush returned 0x%X", result));
    	}
        if (result != 0) {
        	return result;
        }
        int numberOfRegisteredAvcStreams = avcDecodeAddr.getValue32(8);
        for (int i = 0; i < numberOfRegisteredAvcStreams; i++) {
        	TPointer auAddr = new TPointer(avcDecodeAddr, 76 + i * 64);
        	result = Modules.sceMpegModule.sceMpegInitAu(mpeg32, avcDecodeAddr.getValue32(1100), auAddr);
        	if (log.isTraceEnabled()) {
        		log.trace(String.format("initAvcAu: sceMpegInitAu returned 0x%X", result));
        	}
        	if (result != 0) {
        		return result;
        	}
        }
        if (avcStreamMode0 >= 0) {
        	result = Modules.sceMpegModule.sceMpegChangeGetAuMode(mpeg32, avcDecodeAddr.getPointer(12 + avcStreamMode0 * 4), 0);
        	if (log.isTraceEnabled()) {
        		log.trace(String.format("initAvcAu: sceMpegChangeGetAuMode returned 0x%X", result));
        	}
        	if (result != 0) {
        		return result;
        	}
        	avcDecodeAddr.setValue32(4, avcStreamMode0);
        }

        return 0;
	}

	private int initAtracAu(TPointer atracDecodeAddr, int atracStreamMode0, int timestamp) {
		TPointer mpeg = atracDecodeAddr.getPointer(0);
		TPointer32 mpeg32 = new TPointer32(mpeg);
    	atracDecodeAddr.setValue32(1112, 1);
        int numberOfRegisteredAtracStreams = atracDecodeAddr.getValue32(8);
    	for (int i = 0; i < numberOfRegisteredAtracStreams; i++) {
        	TPointer auAddr = new TPointer(atracDecodeAddr, 76 + i * 64);
        	int result = Modules.sceMpegModule.sceMpegInitAu(mpeg32, atracDecodeAddr.getValue32(1104), auAddr);
        	if (log.isTraceEnabled()) {
        		log.trace(String.format("initAtracAu: sceMpegInitAu returned 0x%X", result));
        	}
        	if (result != 0) {
        		return result;
        	}
    	}
    	if (atracStreamMode0 >= 0) {
        	int result = Modules.sceMpegModule.sceMpegChangeGetAuMode(mpeg32, atracDecodeAddr.getPointer(12 + atracStreamMode0 * 4), 0);
        	if (log.isTraceEnabled()) {
        		log.trace(String.format("initAtracAu: sceMpegChangeGetAuMode returned 0x%X", result));
        	}
        	if (result != 0) {
        		return result;
        	}
        	atracDecodeAddr.setValue32(4, atracStreamMode0);
    	}
    	atracDecodeAddr.setValue32(1116, timestamp); // Current PTS

    	return 0;
	}

	private int initPcmAu(TPointer pcmDecodeAddr, int pcmStreamMode0, int timestamp) {
		pcmDecodeAddr.setValue32(1124, 0);
		pcmDecodeAddr.setValue32(1128, 0);
		pcmDecodeAddr.setValue32(1132, 0);
		TPointer mpeg = pcmDecodeAddr.getPointer(0);
		TPointer32 mpeg32 = new TPointer32(mpeg);
        int numberOfRegisteredPcmStreams = pcmDecodeAddr.getValue32(8);
    	for (int i = 0; i < numberOfRegisteredPcmStreams; i++) {
        	TPointer auAddr = new TPointer(pcmDecodeAddr, 76 + i * 64);
        	int result = Modules.sceMpegModule.sceMpegInitAu(mpeg32, pcmDecodeAddr.getValue32(1100), auAddr);
        	if (log.isTraceEnabled()) {
        		log.trace(String.format("initPcmAu: sceMpegInitAu returned 0x%X", result));
        	}
        	if (result != 0) {
        		return result;
        	}
    	}
    	if (pcmStreamMode0 >= 0) {
        	int result = Modules.sceMpegModule.sceMpegChangeGetAuMode(mpeg32, pcmDecodeAddr.getPointer(12 + pcmStreamMode0 * 4), 0);
        	if (log.isTraceEnabled()) {
        		log.trace(String.format("initPcmAu: sceMpegChangeGetAuMode returned 0x%X", result));
        	}
        	if (result != 0) {
        		return result;
        	}
        	pcmDecodeAddr.setValue32(4, pcmStreamMode0);
    	}
    	pcmDecodeAddr.setValue32(1108, timestamp);
    	pcmDecodeAddr.setValue32(1112, timestamp);

    	return 0;
	}

	private int initAus(TPointer decodeControlAddr) {
		int result;

        TPointer avcDecodeAddr = decodeControlAddr.getPointer(24);
        commonChangeGetAuMode(avcDecodeAddr);
        TPointer atracDecodeAddr = decodeControlAddr.getPointer(28);
        commonChangeGetAuMode(atracDecodeAddr);
        TPointer pcmDecodeAddr = decodeControlAddr.getPointer(32);
        commonChangeGetAuMode(pcmDecodeAddr);

        if (decodeControlAddr.getValue32(8) != 0) {
        	return ERROR_PSMFPLAYER_FATAL;
        }
        int avcStreamMode0 = decodeControlAddr.getValue32(12);
        result = initAvcAu(avcDecodeAddr, avcStreamMode0);
        if (result != 0) {
        	return result;
        }

        int audioStreamMode0 = decodeControlAddr.getValue32(20);
        if (audioStreamMode0 >= 0) {
	        if (decodeControlAddr.getValue32(16) == PSMF_ATRAC_STREAM) {
	        	result = initAtracAu(atracDecodeAddr, audioStreamMode0, decodeControlAddr.getValue32(60));
	            if (result != 0) {
	            	return result;
	            }
	        	result = initPcmAu(pcmDecodeAddr, -1, decodeControlAddr.getValue32(60));
	            if (result != 0) {
	            	return result;
	            }
	        } else if (decodeControlAddr.getValue32(16) == PSMF_PCM_STREAM) {
	        	result = initAtracAu(atracDecodeAddr, -1, decodeControlAddr.getValue32(60));
	            if (result != 0) {
	            	return result;
	            }
	        	result = initPcmAu(pcmDecodeAddr, audioStreamMode0, decodeControlAddr.getValue32(60));
	            if (result != 0) {
	            	return result;
	            }
	        } else {
	        	return ERROR_PSMFPLAYER_FATAL;
	        }
        }

    	TPointer videoBufferAddr = decodeControlAddr.getPointer(48);
    	AtomicValue.set(videoBufferAddr.getPointer(44), 1); // ScePsmfPlayerVBufChangeFlag
    	TPointer audioBufferAddr = decodeControlAddr.getPointer(52);
    	audioBufferAddr.setValue32(24, audioBufferAddr.getValue32(24) & ~0x1);

    	return 0;
	}

	private int setTimestampWithDirection(TPointer decodeControlAddr, int timestamp, int playSpeed, int direction) {
		TPointer psmfControlAddr = decodeControlAddr.getPointer(40);
    	TPointer psmf = new TPointer(psmfControlAddr, 24);
    	TPointer32 psmf32 = new TPointer32(psmf);

    	int result = Modules.scePsmfModule.scePsmfGetEPidWithTimestamp(psmf32, timestamp);
    	if (result < 0) {
    		return result;
    	}
		int epId = result;
		if (direction < 0 && epId <= 0) {
	    	AtomicValue.set(decodeControlAddr.getPointer(72), PSMF_PLAYER_DECODE_STATUS_END); // ScePsmfPlayerDecodeStatus
	    	return 0;
		}

		TPointer32 outAddr = allocatePointer32(16);
		result = Modules.scePsmfModule.scePsmfGetEPWithId(psmf32, epId + direction, outAddr);
		if (result != 0) {
			if (result == SceKernelErrors.ERROR_PSMF_INVALID_ID) {
		    	AtomicValue.set(decodeControlAddr.getPointer(72), PSMF_PLAYER_DECODE_STATUS_END); // ScePsmfPlayerDecodeStatus
				result = 0;
			}
			return result;
		}
		int entryOffset = outAddr.getValue(4);

    	TPointer readControlAddr = decodeControlAddr.getPointer(44);
    	readControlAddr.setValue32(36, entryOffset);
    	int entryPicOffset = outAddr.getValue(12);
    	readControlAddr.setValue32(40, Math.max(entryPicOffset, 0x10000));
		setReadStatus(readControlAddr, 0x40);

    	if (direction > 0) {
	    	decodeControlAddr.setValue32(8, psmfControlAddr.getValue32(136));
	    	decodeControlAddr.setValue32(12, psmfControlAddr.getValue32(140));
	    	decodeControlAddr.setValue32(16, psmfControlAddr.getValue32(152));
	    	decodeControlAddr.setValue32(20, psmfControlAddr.getValue32(156));
    	}

    	result = initAus(decodeControlAddr);
    	if (result != 0) {
    		return result;
    	}

    	switch (playSpeed) {
    		case PSMF_PLAYER_SPEED_SLOW:
	    	case PSMF_PLAYER_SPEED_NORMAL:
	    	case PSMF_PLAYER_SPEED_FAST:
	    		decodeControlAddr.setValue32(64, playSpeed);
	    		break;
			default:
    			decodeControlAddr.setValue32(64, PSMF_PLAYER_SPEED_SLOW);
    			break;
    	}
    	AtomicValue.set(decodeControlAddr.getPointer(72), direction < 0 ? PSMF_PLAYER_DECODE_STATUS_REWIND : PSMF_PLAYER_DECODE_STATUS_FORWARD); // ScePsmfPlayerDecodeStatus

    	return 0;
	}

	private int updatePlayModeAndSpeed(TPointer avSyncControlAddr, int playMode, int playSpeed, int pts) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("updatePlayModeAndSpeed playMode=%d, playSpeed=%d, pts=%d", playMode, playSpeed, pts));
		}

		TPointer videoBufferAddr = avSyncControlAddr.getPointer(24);
    	AtomicValue.set(videoBufferAddr.getPointer(44), 2); // ScePsmfPlayerVBufChangeFlag
    	videoBufferAddr.setValue32(8, 0);
    	videoBufferAddr.setValue32(12, 0);
    	videoBufferAddr.setValue32(16, 0);
    	TPointer audioBufferAddr = avSyncControlAddr.getPointer(28);
    	audioBufferAddr.setValue32(24, audioBufferAddr.getValue32(24) | 0x1);
    	audioBufferAddr.setValue32(4, 0);
    	audioBufferAddr.setValue32(8, 0);
    	audioBufferAddr.setValue32(12, 0);

        TPointer psmfControlAddr = avSyncControlAddr.getPointer(44);
        int startTime = psmfControlAddr.getValue32(4);
        int timestamp = startTime + pts;

        TPointer decodeControlAddr = avSyncControlAddr.getPointer(32);
    	switch (playMode) {
	    	case PSMF_PLAYER_MODE_FORWARD:
	    		setTimestampWithDirection(decodeControlAddr, timestamp, playSpeed, 1);
	    		break;
	    	case PSMF_PLAYER_MODE_REWIND:
    			setTimestampWithDirection(decodeControlAddr, timestamp, playSpeed, -1);
	    		break;
	    	case PSMF_PLAYER_MODE_SLOWMOTION:
	    	case PSMF_PLAYER_MODE_STEPFRAME:
	    	case PSMF_PLAYER_MODE_PAUSE:
	    	case PSMF_PLAYER_MODE_PLAY:
    		default:
	    		setTimestamp(decodeControlAddr, timestamp);
	    		AtomicValue.clear(avSyncControlAddr.getPointer(16), 3); // ScePsmfPlayerASCAudioFlag
	    		break;
    	}
		AtomicValue.set(avSyncControlAddr.getPointer(12), getReadyFlag(playMode, playSpeed)); // ScePsmfPlayerASCReadyFlag

    	return 0;
	}

	private int decodeImageToVideoDataAddr(TPointer avSyncControlAddr, TPointer32 videoDataAddr) {
		TPointer videoBufferAddr = avSyncControlAddr.getPointer(24);
		TPointer decodeControlAddr = avSyncControlAddr.getPointer(32);
		TPointer psmfControlAddr = avSyncControlAddr.getPointer(44);
		int frameWidth = videoDataAddr.getValue(0);
		TPointer destAddr = videoDataAddr.getPointer(4);

		if (log.isTraceEnabled()) {
			log.trace(String.format("decodeImageToVideoDataAddr frameWidth=%d, destAddr=%s, videoBufferAddr.getValue32(32)=%d", frameWidth, destAddr, videoBufferAddr.getValue32(32)));
		}

		if (videoBufferAddr.getValue32(32) == 0) {
        	return ERROR_PSMFPLAYER_NO_DATA;
        }

		TPointer buffer = videoBufferAddr.getPointer(24);
    	TPointer psmf = new TPointer(psmfControlAddr, 24);
    	TPointer32 psmf32 = new TPointer32(psmf);
    	TPointer32 videoInfoAddr = allocatePointer32(8);
    	int result = Modules.scePsmfModule.scePsmfGetVideoInfo(psmf32, videoInfoAddr);
    	if (result < 0) {
    		return result;
    	}
    	int videoWidth = videoInfoAddr.getValue(0);
    	int videoHeight = videoInfoAddr.getValue(4);
    	TPointer mpeg = decodeControlAddr.getPointer(0);
    	TPointer32 mpeg32 = new TPointer32(mpeg);
    	int pixelStorageMode = decodeControlAddr.getValue32(68);
    	if (pixelStorageMode == 4) {
    		TPointer yuv420Buffer = decodeControlAddr.getPointer(80);
    		result = Modules.sceMpegModule.sceMpegAvcConvertToYuv420(mpeg32, yuv420Buffer, buffer, 0);
        	if (log.isTraceEnabled()) {
        		log.trace(String.format("decodeImageToVideoDataAddr: sceMpegAvcConvertToYuv420 returned 0x%X", result));
        	}
    		if (result < 0) {
    			return result;
    		}
    		int widthHeight = (videoWidth << 16) + videoHeight;
    		result = Modules.sceJpegModule.sceJpegCsc(destAddr, yuv420Buffer, widthHeight, frameWidth, 0x120202);
        	if (log.isTraceEnabled()) {
        		log.trace(String.format("decodeImageToVideoDataAddr: sceJpegCsc returned 0x%X", result));
        	}
    		if (result != 0) {
    			return result;
    		}
    	} else {
    		TPointer32 rangeAddr = allocatePointer32(16);
    		rangeAddr.setValue(0, 0); // rangeX
    		rangeAddr.setValue(4, 0); // rangeY
    		rangeAddr.setValue(8, videoWidth); // rangeWidth
    		rangeAddr.setValue(12, videoHeight); // rangeHeight
    		result = Modules.sceMpegModule.sceMpegAvcCsc(mpeg32, buffer, rangeAddr, frameWidth, destAddr);
        	if (log.isTraceEnabled()) {
        		log.trace(String.format("decodeImageToVideoDataAddr: sceMpegAvcCsc returned 0x%X", result));
        	}
    		if (result != 0) {
    			return result;
    		}
    	}

    	int pts = videoBufferAddr.getValue32(28);
		int startTime = psmfControlAddr.getValue32(4);
    	videoDataAddr.setValue(8, pts - startTime);

		return 0;
	}

    private int copyYCbCr(TPointer videoBufferAddr) {
		int numberUsedDisplayBuffers = videoBufferAddr.getValue32(16);
    	if (numberUsedDisplayBuffers == 0) {
    		return -1;
    	}

    	TPointer destBuffer = videoBufferAddr.getPointer(24);
		int displayBufferIndex = videoBufferAddr.getValue32(12);
		TPointer displayDataArrayAddr = videoBufferAddr.getPointer(4);
		TPointer sourceBuffer = displayDataArrayAddr.getPointer(displayBufferIndex * 8 + 0);
		int pts = displayDataArrayAddr.getValue32(displayBufferIndex * 8 + 4);

		TPointer mpeg = videoBufferAddr.getPointer(0);
		TPointer32 mpeg32 = new TPointer32(mpeg);
		if (log.isTraceEnabled()) {
			log.trace(String.format("copyYCbCr calling sceMpegAvcCopyYCbCr destBuffer=%s, sourceBuffer=%s", destBuffer, sourceBuffer));
		}
		Modules.sceMpegModule.sceMpegAvcCopyYCbCr(mpeg32, destBuffer, sourceBuffer);

		videoBufferAddr.setValue32(28, pts);
		consumeOneDiplayBuffer(videoBufferAddr);
		videoBufferAddr.setValue32(32, 1);

		return 0;
    }

    private int consumeOneDiplayBuffer(TPointer videoBufferAddr) {
		int numberUsedDisplayBuffers = videoBufferAddr.getValue32(16);
    	if (numberUsedDisplayBuffers == 0) {
    		return -1;
    	}

		int displayBufferIndex = videoBufferAddr.getValue32(12);
		TPointer displayDataArrayAddr = videoBufferAddr.getPointer(4);
		int pts = displayDataArrayAddr.getValue32(displayBufferIndex * 8 + 4);
		videoBufferAddr.setValue32(40, pts);

		videoBufferAddr.setValue32(16, numberUsedDisplayBuffers - 1);
		int numberDisplayBuffers = videoBufferAddr.getValue32(20);
		videoBufferAddr.setValue32(12, (displayBufferIndex + 1) % numberDisplayBuffers);

		return 0;
    }

    private int consumeOneAudioBuffer(TPointer audioBufferAddr) {
		int numberUsedAudioBuffers = audioBufferAddr.getValue32(12);
		if (log.isTraceEnabled()) {
			log.trace(String.format("consumeOneAudioBuffer numberUsedAudioBuffers=%d", numberUsedAudioBuffers));
		}
		if (numberUsedAudioBuffers == 0) {
			return -1;
		}

		audioBufferAddr.setValue32(12, numberUsedAudioBuffers - 1);
		int audioBufferIndex = audioBufferAddr.getValue32(8);
		int numberAudioBuffers = audioBufferAddr.getValue32(16);
		audioBufferAddr.setValue32(8, (audioBufferIndex + 1) % numberAudioBuffers);

		return 0;
    }

    private static int getReadyFlag(int playMode, int playSpeed) {
    	switch (playMode) {
	    	case PSMF_PLAYER_MODE_SLOWMOTION:      return PSMF_PLAYER_ASC_READY_FLAG_SLOWMOTION;
	    	case PSMF_PLAYER_MODE_STEPFRAME:
	    	case PSMF_PLAYER_MODE_PAUSE:           return PSMF_PLAYER_ASC_READY_FLAG_PAUSE;
	    	case PSMF_PLAYER_MODE_PLAY:            return PSMF_PLAYER_ASC_READY_FLAG_PLAY;
	    	case PSMF_PLAYER_MODE_FORWARD:
	    		switch (playSpeed) {
		    		case PSMF_PLAYER_SPEED_NORMAL: return PSMF_PLAYER_ASC_READY_FLAG_FORWARD_NORMAL;
		    		case PSMF_PLAYER_SPEED_FAST:   return PSMF_PLAYER_ASC_READY_FLAG_FORWARD_FAST;
	    			case PSMF_PLAYER_SPEED_SLOW:
					default:                       return PSMF_PLAYER_ASC_READY_FLAG_FORWARD_SLOW;
	    		}
	    	case PSMF_PLAYER_MODE_REWIND:
	    		switch (playSpeed) {
		    		case PSMF_PLAYER_SPEED_NORMAL: return PSMF_PLAYER_ASC_READY_FLAG_REWIND_NORMAL;
		    		case PSMF_PLAYER_SPEED_FAST:   return PSMF_PLAYER_ASC_READY_FLAG_REWIND_FAST;
	    			case PSMF_PLAYER_SPEED_SLOW:
					default:                       return PSMF_PLAYER_ASC_READY_FLAG_REWIND_SLOW;
	    		}
		}
		return PSMF_PLAYER_ASC_READY_FLAG_PLAY;
    }

    private int getVideoDataPauseContinue(TPointer avSyncControlAddr, TPointer32 videoDataAddr) {
        clearVblankSyncCount(avSyncControlAddr);
        return decodeImageToVideoDataAddr(avSyncControlAddr, videoDataAddr);
    }

    private int getVideoDataPlayContinue(TPointer avSyncControlAddr, TPointer32 videoDataAddr) {
    	int result;

        TPointer videoBufferAddr = avSyncControlAddr.getPointer(24);
        TPointer audioBufferAddr = avSyncControlAddr.getPointer(28);
        TPointer readControlAddr = avSyncControlAddr.getPointer(36);
		int numberUsedAudioBuffers = audioBufferAddr.getValue32(12);
		int numberUsedDisplayBuffers = videoBufferAddr.getValue32(16);
    	int readStatus = AtomicValue.get(readControlAddr.getPointer(4)); // ScePsmfPlayerReadStatus

		if (avSyncControlAddr.getValue32(20) == 1 && readStatus != 0x2 && numberUsedAudioBuffers == 0) {
			result = getVideoDataPauseContinue(avSyncControlAddr, videoDataAddr);
            AtomicValue.clear(avSyncControlAddr.getPointer(16), 0x3); // ScePsmfPlayerASCAudioFlag
    		AtomicValue.set(avSyncControlAddr.getPointer(12), PSMF_PLAYER_ASC_READY_FLAG_PLAY); // ScePsmfPlayerASCReadyFlag
		} else if (readStatus == 0x2 || numberUsedDisplayBuffers > 0 || getVblankSyncCount(avSyncControlAddr) != 0) {
    		int displayBufferIndex = videoBufferAddr.getValue32(12);
    		TPointer displayDataArrayAddr = videoBufferAddr.getPointer(4);
    		int videoPts = displayDataArrayAddr.getValue32(displayBufferIndex * 8 + 4);

    		if (numberUsedAudioBuffers > 0) {
        		int audioBufferIndex = audioBufferAddr.getValue32(8);
        		TPointer audioDataArrayAddr = audioBufferAddr.getPointer(0);
        		int audioPts = audioDataArrayAddr.getValue32(audioBufferIndex * 8 + 4);

        		int vblankSyncCount = getVblankSyncCount(avSyncControlAddr);
        		int videoPts2 = videoPts - vblankSyncCount * videoTimestampStep / 2;
        		if (log.isTraceEnabled()) {
        			log.trace(String.format("scePsmfPlayerGetVideoData videoPts=%d, audioPts=%d, vblankSyncCount=%d, videoPts2=%d", videoPts, audioPts, vblankSyncCount, videoPts2));
        		}
        		if (videoPts2 - audioPts <= 2 * videoTimestampStep) {
	        		if (audioPts - videoPts2 > 4 * videoTimestampStep) {
	        			consumeOneDiplayBuffer(videoBufferAddr);
	        			consumeOneDiplayBuffer(videoBufferAddr);
	        			return getVideoDataPauseContinue(avSyncControlAddr, videoDataAddr);
	        		}
        		} else {
        			return getVideoDataPauseContinue(avSyncControlAddr, videoDataAddr);
        		}
    		}

			if (getVblankSyncCount(avSyncControlAddr) > 0 || numberUsedDisplayBuffers <= 0) {
				result = decodeImageToVideoDataAddr(avSyncControlAddr, videoDataAddr);
			} else {
				result = copyYCbCr(videoBufferAddr);
				if (result < 0) {
    				result = decodeImageToVideoDataAddr(avSyncControlAddr, videoDataAddr);
				} else {
					result = decodeImageToVideoDataAddr(avSyncControlAddr, videoDataAddr);
					setVblankSyncCount(avSyncControlAddr, getVblankSyncInterval(avSyncControlAddr));
				}
			}
		} else {
			result = getVideoDataPauseContinue(avSyncControlAddr, videoDataAddr);
            AtomicValue.clear(avSyncControlAddr.getPointer(16), 0x1); // ScePsmfPlayerASCAudioFlag
    		AtomicValue.set(avSyncControlAddr.getPointer(12), PSMF_PLAYER_ASC_READY_FLAG_PLAY); // ScePsmfPlayerASCReadyFlag
		}

		return result;
    }

    private int getVideoDataRewindForward(TPointer avSyncControlAddr, TPointer32 videoDataAddr, int vblankSyncInterval, int readyFlag) {
    	clearVblankSyncCount(avSyncControlAddr);
    	setVblankSyncInterval(avSyncControlAddr, vblankSyncInterval);
		AtomicValue.set(avSyncControlAddr.getPointer(12), readyFlag); // ScePsmfPlayerASCReadyFlag
		int result = getVideoDataPauseContinue(avSyncControlAddr, videoDataAddr);
		avSyncControlAddr.setValue32(20, avSyncControlAddr.getValue32(20) & ~0x1);

		return result;
    }

    @HLEFunction(nid = 0x235D8787, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerCreate(@BufferInfo(usage=Usage.out) TPointer32 psmfPlayer, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=12, usage=Usage.in) TPointer psmfPlayerDataAddr) {
    	psmfPlayer.setValue(0);

    	initHLE();

		TPointer buffer = psmfPlayerDataAddr.getPointer(0);
    	TPointer bufferAligned = new TPointer(buffer);
    	bufferAligned.alignUp(63);
        int bufferSize = psmfPlayerDataAddr.getValue32(4);
        int threadPriority = psmfPlayerDataAddr.getValue32(8);

        if (threadPriority < 16 || threadPriority > 109) {
        	return ERROR_PSMFPLAYER_INVALID_CONFIG_VALUE;
        }

        final int audioBufferSize = 0x8000;
        final int videoBufferSize = 0xCC000;
        final int controlBufferSize = 0x80000;
        int yuv420BufferSize = 0x33000;
        if (bufferSize < audioBufferSize + videoBufferSize + controlBufferSize + yuv420BufferSize + ringbufferSize + 64) {
            if (bufferSize < audioBufferSize + videoBufferSize + controlBufferSize + ringbufferSize) {
            	return ERROR_PSMFPLAYER_NO_MEMORY;
            }
            // Some versions of libpsmfplayer.prx do not use the yuv420Buffer
            yuv420BufferSize = 0;
        }

        TPointer audioBuffer = new TPointer(bufferAligned, 0);
        TPointer videoBuffer = new TPointer(audioBuffer, audioBufferSize);
        TPointer controlBuffer = new TPointer(videoBuffer, videoBufferSize);
        TPointer yuv420Buffer = new TPointer(controlBuffer, controlBufferSize);
        TPointer ringbufferData = new TPointer(yuv420Buffer, yuv420BufferSize);

        if (yuv420BufferSize == 0) {
        	yuv420Buffer = TPointer.NULL;
        }

        int result = Modules.ThreadManForUserModule.sceKernelCreateVpl(new PspString("ScePsmfPlayerMemoryPool"), SysMemUserForUser.USER_PARTITION_ID, 0, 0x40000, TPointer.NULL);
        if (result < 0) {
        	return result;
        }
        scePsmfPlayerMemoryPool = result;
        scePsmfPlayerMemoryPoolMaxAllocatedSize = 0;

        TPointer data = allocate(72);
        if (data == null) {
        	return ERROR_PSMFPLAYER_NO_MEMORY;
        }
        psmfPlayer.setPointer(data);

        result = initData(psmfPlayer, threadPriority, ringbufferData, controlBuffer, controlBufferSize, videoBuffer, videoBufferSize, audioBuffer, audioBufferSize, yuv420Buffer);
        if (result != 0) {
        	freeData(psmfPlayer);
        	return result;
        }

        return result;
    }

    @HLEFunction(nid = 0x9B71A274, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerDelete(@CheckArgument("checkPlayerInitialized") TPointer32 psmfPlayer) {
    	TPointer data = psmfPlayer.getPointer();
        TPointer playerStatusAddr = data.getPointer(24);
        TPointer decodeControlAddr = data.getPointer(36);
		TPointer videoBufferAddr = data.getPointer(40);
    	TPointer readControlAddr = data.getPointer(48);

    	int result = scePsmfPlayerBreak(psmfPlayer);
    	if (result < 0) {
    		return result;
    	}

    	int playerStatus = getPlayerStatus(playerStatusAddr);
    	if (playerStatus != PSMF_PLAYER_STATUS_PLAYING && playerStatus != PSMF_PLAYER_STATUS_INIT && playerStatus != PSMF_PLAYER_STATUS_ERROR && playerStatus != PSMF_PLAYER_STATUS_PLAYING_FINISHED) {
    		return ERROR_PSMFPLAYER_NOT_INITIALIZED;
    	}

    	AtomicValue.set(decodeControlAddr.getPointer(72), PSMF_PLAYER_DECODE_STATUS_EXIT); // ScePsmfPlayerDecodeStatus
    	setReadStatus(readControlAddr, 0x100);
    	data.setValue32(20, data.getValue32(20) | 0x1); // This will stop the "ScePsmfPlayerMCThread" thread
        hleSyscall(Modules.ThreadManForUserModule.sceKernelSetEventFlag(playerStatusAddr.getValue32(24), 0xF));

        int scePsmfPlayerReadThread = readControlAddr.getValue32(12);
        hleSyscall(Modules.ThreadManForUserModule.sceKernelWaitThreadEnd(scePsmfPlayerReadThread, TPointer32.NULL));

        int scePsmfPlayerDecodeThread = decodeControlAddr.getValue32(56);
        hleSyscall(Modules.ThreadManForUserModule.sceKernelWaitThreadEnd(scePsmfPlayerDecodeThread, TPointer32.NULL));

        int scePsmfPlayerMCThread = data.getValue32(16);
		hleSyscall(Modules.ThreadManForUserModule.sceKernelWaitThreadEnd(scePsmfPlayerMCThread, TPointer32.NULL));

		unregisterAllStreams(data);
		unregisterReadControl(data);

		videoBufferAddr.setValue32(8, 0);
		videoBufferAddr.setValue32(12, 0);
		videoBufferAddr.setValue32(16, 0);
		videoBufferAddr.setValue32(32, 0);

		setPlayerStatus(playerStatusAddr, PSMF_PLAYER_STATUS_INIT);

		freeData(psmfPlayer);

        return 0;
    }

    @HLEFunction(nid = 0x3D6D25A9, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerSetPsmf(@CheckArgument("checkPlayerInitialized0") TPointer32 psmfPlayer, PspString fileAddr) {
    	return hlePsmfPlayerSetPsmf(psmfPlayer, fileAddr, 0, false);
    }

    @HLEFunction(nid = 0x58B83577, version = 150)
    public int scePsmfPlayerSetPsmfCB(@CheckArgument("checkPlayerInitialized0") TPointer32 psmfPlayer, PspString fileAddr) {
    	return hlePsmfPlayerSetPsmf(psmfPlayer, fileAddr, 0, true);
    }

    @HLEFunction(nid = 0xE792CD94, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerReleasePsmf(@CheckArgument("checkPlayerInitialized") TPointer32 psmfPlayer) {
    	TPointer data = psmfPlayer.getPointer();
        TPointer playerStatusAddr = data.getPointer(24);
        TPointer decodeControlAddr = data.getPointer(36);
		TPointer videoBufferAddr = data.getPointer(40);
    	TPointer readControlAddr = data.getPointer(48);

    	int playerStatus = getPlayerStatus(playerStatusAddr);
    	if (playerStatus != PSMF_PLAYER_STATUS_PLAYING && playerStatus != PSMF_PLAYER_STATUS_STANDBY && playerStatus != PSMF_PLAYER_STATUS_ERROR && playerStatus != PSMF_PLAYER_STATUS_PLAYING_FINISHED) {
    		return ERROR_PSMFPLAYER_NOT_INITIALIZED;
    	}

    	AtomicValue.set(decodeControlAddr.getPointer(72), PSMF_PLAYER_DECODE_STATUS_EXIT); // ScePsmfPlayerDecodeStatus
    	setReadStatus(readControlAddr, 0x2);
    	setReadStatus(readControlAddr, 0x100);
    	data.setValue32(20, data.getValue32(20) | 0x1); // This will stop the "ScePsmfPlayerMCThread" thread
        hleSyscall(Modules.ThreadManForUserModule.sceKernelSetEventFlag(playerStatusAddr.getValue32(24), 0xF));

        int scePsmfPlayerReadThread = readControlAddr.getValue32(12);
        hleSyscall(Modules.ThreadManForUserModule.sceKernelWaitThreadEnd(scePsmfPlayerReadThread, TPointer32.NULL));

        int scePsmfPlayerDecodeThread = decodeControlAddr.getValue32(56);
        hleSyscall(Modules.ThreadManForUserModule.sceKernelWaitThreadEnd(scePsmfPlayerDecodeThread, TPointer32.NULL));

        int scePsmfPlayerMCThread = data.getValue32(16);
		hleSyscall(Modules.ThreadManForUserModule.sceKernelWaitThreadEnd(scePsmfPlayerMCThread, TPointer32.NULL));

		setPlayerStatus(playerStatusAddr, PSMF_PLAYER_STATUS_STANDBY);

		unregisterAllStreams(data);
		unregisterReadControl(data);

		videoBufferAddr.setValue32(8, 0);
		videoBufferAddr.setValue32(12, 0);
		videoBufferAddr.setValue32(16, 0);
		videoBufferAddr.setValue32(32, 0);

		setPlayerStatus(playerStatusAddr, PSMF_PLAYER_STATUS_INIT);

		return 0;
    }

    @HLEFunction(nid = 0x95A84EE5, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerStart(@CheckArgument("checkPlayerInitialized0") TPointer32 psmfPlayer, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=24, usage=Usage.in) TPointer32 initPlayInfoAddr, int initPts) {
    	int result;

    	TPointer data = psmfPlayer.getPointer();
        TPointer playerStatusAddr = data.getPointer(24);
    	int status = getPlayerStatus(playerStatusAddr);
    	int version = getPlayerVersion(playerStatusAddr);
    	if (status == PSMF_PLAYER_STATUS_PLAYING || status == PSMF_PLAYER_STATUS_PLAYING_FINISHED) {
    		if (version == PSMF_PLAYER_VERSION_NET) {
    			return ERROR_PSMFPLAYER_NOT_SUPPORTED;
    		}
    	} else if (status != PSMF_PLAYER_STATUS_STANDBY) {
    		return ERROR_PSMFPLAYER_NOT_INITIALIZED;
    	}

    	int playMode = initPlayInfoAddr.getValue(16);
    	if (version == PSMF_PLAYER_VERSION_BASIC || version == PSMF_PLAYER_VERSION_NET) {
    		if (playMode < 0 || playMode > PSMF_PLAYER_MODE_PAUSE) {
    			return ERROR_PSMFPLAYER_INVALID_CONFIG_VALUE;
    		}
    		if (initPts != 0) {
    			return ERROR_PSMFPLAYER_INVALID_CONFIG_VALUE;
    		}
    	}
        int playSpeed = initPlayInfoAddr.getValue(20);

        TPointer psmfControlAddr = data.getPointer(28);
        int avcCodec = initPlayInfoAddr.getValue(0);
        int avcStreamNum = initPlayInfoAddr.getValue(4);
        if (avcCodec != 0 && avcCodec != 14) {
        	return ERROR_PSMFPLAYER_NOT_SUPPORTED;
        }
        int numberOfAvcStreams = psmfControlAddr.getValue32(12);
        if (avcStreamNum < 0 || avcStreamNum >= numberOfAvcStreams) {
        	return ERROR_PSMFPLAYER_INVALID_CONFIG_MODE;
        }

        int audioCodec = initPlayInfoAddr.getValue(8);
        int audioStreamNum = initPlayInfoAddr.getValue(12);
        int numberOfAtracStreams = psmfControlAddr.getValue32(16);
        int numberOfPcmStreams = psmfControlAddr.getValue32(20);
        if (numberOfAtracStreams + numberOfPcmStreams == 0) {
    		audioStreamNum = -1;
        } else if (audioCodec == 1) {
        	if (audioStreamNum >= numberOfAtracStreams) {
        		return ERROR_PSMFPLAYER_INVALID_CONFIG_MODE;
        	}
        } else if (audioCodec == 2) {
        	if (audioStreamNum >= numberOfPcmStreams) {
        		return ERROR_PSMFPLAYER_INVALID_CONFIG_MODE;
        	}
        } else if (audioCodec == 15) {
        	if (audioStreamNum >= numberOfAtracStreams + numberOfPcmStreams) {
        		return ERROR_PSMFPLAYER_INVALID_CONFIG_MODE;
        	}
        } else {
        	return ERROR_PSMFPLAYER_NOT_SUPPORTED;
        }

        int startTime = psmfControlAddr.getValue32(4);
        int endTime = psmfControlAddr.getValue32(8);
        if (startTime + initPts >= endTime) {
        	return ERROR_PSMFPLAYER_INVALID_CONFIG_VALUE;
        }

        if (status == PSMF_PLAYER_STATUS_STANDBY) {
        	data.setValue32(20, 0);
        	int scePsmfPlayerMCThread = data.getValue32(16);
			final int threadDataSize = 4;
			TPointer threadData = allocatePointer(threadDataSize);
			threadData.setPointer(psmfPlayer);
        	result = hleSyscall(Modules.ThreadManForUserModule.sceKernelStartThread(scePsmfPlayerMCThread, threadDataSize, threadData));
        	if (result != 0) {
        		return result;
        	}

        	TPointer decodeControlAddr = data.getPointer(36);
        	AtomicValue.set(decodeControlAddr.getPointer(72), PSMF_PLAYER_DECODE_STATUS_START); // ScePsmfPlayerDecodeStatus
        	int scePsmfPlayerDecodeThread = decodeControlAddr.getValue32(56);
        	threadData.setPointer(decodeControlAddr);
        	result = hleSyscall(Modules.ThreadManForUserModule.sceKernelStartThread(scePsmfPlayerDecodeThread, threadDataSize, threadData));
        	if (result != 0) {
        		return result;
        	}

        	TPointer readControlAddr = data.getPointer(48);
        	setReadStatus(readControlAddr, 0x1);
        	int scePsmfPlayerReadThread = readControlAddr.getValue32(12);
        	threadData.setPointer(readControlAddr);
        	result = hleSyscall(Modules.ThreadManForUserModule.sceKernelStartThread(scePsmfPlayerReadThread, threadDataSize, threadData));
        	if (result != 0) {
        		return result;
        	}
        }

    	TPointer psmf = new TPointer(psmfControlAddr, 24);
    	TPointer32 psmf32 = new TPointer32(psmf);
    	result = Modules.scePsmfModule.scePsmfSpecifyStreamWithStreamTypeNumber(psmf32, PSMF_AVC_STREAM, avcStreamNum);
    	if (result != 0) {
    		return result;
    	}

    	TPointer32 typeAddr = allocatePointer32(4);
    	TPointer32 channelAddr = allocatePointer32(4);
    	result = Modules.scePsmfModule.scePsmfGetCurrentStreamType(psmf32, typeAddr, channelAddr);
    	if (result != 0) {
    		return result;
    	}
    	if (typeAddr.getValue() != PSMF_AVC_STREAM) {
    		return ERROR_PSMFPLAYER_FATAL;
    	}
    	psmfControlAddr.setValue32(136, 0);
    	psmfControlAddr.setValue32(140, avcStreamNum);
    	psmfControlAddr.setValue32(144, channelAddr.getValue());
    	psmfControlAddr.setValue32(148, avcStreamNum);

        TPointer avSyncControlAddr = data.getPointer(32);
    	if (audioStreamNum < 0) {
    		if (numberOfAtracStreams + numberOfPcmStreams == 0) {
    			avSyncControlAddr.setValue32(20, avSyncControlAddr.getValue32(20) & ~0x1);
    		} else {
    			avSyncControlAddr.setValue32(20, avSyncControlAddr.getValue32(20) | 0x2);
    		}
    	} else if (audioCodec == PSMF_ATRAC_STREAM || audioCodec == PSMF_PCM_STREAM) {
			result = Modules.scePsmfModule.scePsmfSpecifyStreamWithStreamTypeNumber(psmf32, audioCodec, audioStreamNum);
			if (result != 0) {
				return result;
			}

	    	result = Modules.scePsmfModule.scePsmfGetCurrentStreamType(psmf32, typeAddr, channelAddr);
	    	if (result != 0) {
	    		return result;
	    	}
	    	psmfControlAddr.setValue32(152, typeAddr.getValue());
	    	psmfControlAddr.setValue32(156, audioStreamNum);
	    	psmfControlAddr.setValue32(160, channelAddr.getValue());

	    	TPointer psmfCopy = allocatePointer(56);
	    	TPointer32 psmf32Copy = new TPointer32(psmfCopy);
	    	psmfCopy.memcpy(psmf, 56);

	    	psmfControlAddr.setValue32(164, -1);
	    	for (int i = 0; i < numberOfAtracStreams + numberOfPcmStreams; i++) {
    	    	result = Modules.scePsmfModule.scePsmfSpecifyStreamWithStreamTypeNumber(psmf32Copy, PSMF_AUDIO_STREAM, i);
    	    	if (result != 0) {
    	    		return result;
    	    	}
    	    	result = Modules.scePsmfModule.scePsmfGetCurrentStreamType(psmf32Copy, typeAddr, channelAddr);
    	    	if (result != 0) {
    	    		return result;
    	    	}
    	    	if (typeAddr.getValue() == audioCodec) {
    	    		if (channelAddr.getValue() == audioStreamNum) {
    	    	    	psmfControlAddr.setValue32(164, i);
    	    			break;
    	    		}
    	    	}
	    	}
		} else if (audioCodec == PSMF_AUDIO_STREAM) {
	    	TPointer psmfAudio = new TPointer(psmfControlAddr, 80);
	    	TPointer32 psmf32Audio = new TPointer32(psmfAudio);
	    	result = Modules.scePsmfModule.scePsmfSpecifyStreamWithStreamTypeNumber(psmf32Audio, audioCodec, audioStreamNum);
	    	if (result != 0) {
	    		return result;
	    	}

	    	result = Modules.scePsmfModule.scePsmfGetCurrentStreamType(psmf32Audio, typeAddr, channelAddr);
	    	if (result != 0) {
	    		return result;
	    	}
	    	psmfControlAddr.setValue32(152, typeAddr.getValue());
	    	psmfControlAddr.setValue32(164, audioStreamNum);
	    	psmfControlAddr.setValue32(160, channelAddr.getValue());

	    	TPointer psmfCopy = allocatePointer(56);
	    	TPointer32 psmf32Copy = new TPointer32(psmfCopy);
	    	psmfCopy.memcpy(psmf, 56);

	    	int audioCodecType = typeAddr.getValue();
	    	int audioCodecChannel = channelAddr.getValue();
	    	psmfControlAddr.setValue32(156, -1);
	    	for (int i = 0; i < numberOfAtracStreams + numberOfPcmStreams; i++) {
    	    	result = Modules.scePsmfModule.scePsmfSpecifyStreamWithStreamTypeNumber(psmf32Copy, audioCodecType, i);
    	    	if (result != 0) {
    	    		return result;
    	    	}
    	    	result = Modules.scePsmfModule.scePsmfGetCurrentStreamType(psmf32Copy, typeAddr, channelAddr);
    	    	if (result != 0) {
    	    		return result;
    	    	}
    	    	if (typeAddr.getValue() == audioCodecType) {
    	    		if (channelAddr.getValue() == audioCodecChannel) {
    	    	    	psmfControlAddr.setValue32(156, i);
    	    			break;
    	    		}
    	    	}
	    	}
		} else {
        	return ERROR_PSMFPLAYER_NOT_SUPPORTED;
		}

    	result = updatePlayModeAndSpeed(avSyncControlAddr, playMode, playSpeed, initPts);
    	if (result != 0) {
    		return result;
    	}

    	playerStatusAddr.setValue32(4, playMode);
    	playerStatusAddr.setValue32(16, playSpeed);

    	setPlayerStatus(playerStatusAddr, PSMF_PLAYER_STATUS_PLAYING);

        return 0;
    }

    @HLEFunction(nid = 0x3EA82A4B, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerGetAudioOutSize(@CheckArgument("checkPlayerInitialized") TPointer32 psmfPlayer) {
    	TPointer data = psmfPlayer.getPointer();
        TPointer playerStatusAddr = data.getPointer(24);

    	int playerStatus = getPlayerStatus(playerStatusAddr);
    	if (playerStatus != PSMF_PLAYER_STATUS_PLAYING && playerStatus != PSMF_PLAYER_STATUS_INIT && playerStatus != PSMF_PLAYER_STATUS_STANDBY && playerStatus != PSMF_PLAYER_STATUS_ERROR && playerStatus != PSMF_PLAYER_STATUS_PLAYING_FINISHED) {
    		return ERROR_PSMFPLAYER_NOT_INITIALIZED;
    	}

        TPointer decodeControlAddr = data.getPointer(36);
        TPointer atracDecodeAddr = decodeControlAddr.getPointer(28);
		int atracOutputSize = atracDecodeAddr.getValue32(1108);

		return atracOutputSize;
    }

    @HLEFunction(nid = 0x1078C008, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerStop(@CheckArgument("checkPlayerPlaying") TPointer32 psmfPlayer) {
    	TPointer data = psmfPlayer.getPointer();
        TPointer playerStatusAddr = data.getPointer(24);
        TPointer avSyncControlAddr = data.getPointer(32);
        TPointer decodeControlAddr = data.getPointer(36);
    	TPointer readControlAddr = data.getPointer(48);

    	AtomicValue.set(decodeControlAddr.getPointer(72), PSMF_PLAYER_DECODE_STATUS_EXIT); // ScePsmfPlayerDecodeStatus
    	setReadStatus(readControlAddr, 0x2);
    	setReadStatus(readControlAddr, 0x100);
    	data.setValue32(20, data.getValue32(20) | 0x1); // This will stop the "ScePsmfPlayerMCThread" thread
        hleSyscall(Modules.ThreadManForUserModule.sceKernelSetEventFlag(playerStatusAddr.getValue32(24), 0xF));

        int scePsmfPlayerReadThread = readControlAddr.getValue32(12);
        hleSyscall(Modules.ThreadManForUserModule.sceKernelWaitThreadEnd(scePsmfPlayerReadThread, TPointer32.NULL));

        int scePsmfPlayerDecodeThread = decodeControlAddr.getValue32(56);
        hleSyscall(Modules.ThreadManForUserModule.sceKernelWaitThreadEnd(scePsmfPlayerDecodeThread, TPointer32.NULL));

        int scePsmfPlayerMCThread = data.getValue32(16);
		hleSyscall(Modules.ThreadManForUserModule.sceKernelWaitThreadEnd(scePsmfPlayerMCThread, TPointer32.NULL));

		avSyncControlAddr.setValue32(20, avSyncControlAddr.getValue32(20) & ~0x2);
		setPlayerStatus(playerStatusAddr, PSMF_PLAYER_STATUS_STANDBY);

		int errorCode = playerStatusAddr.getValue32(0);

		return errorCode;
    }

    @HLEFunction(nid = 0xA0B8CA55, version = 150)
    public int scePsmfPlayerUpdate(@CheckArgument("checkPlayerPlaying") TPointer32 psmfPlayer) {
    	TPointer data = psmfPlayer.getPointer();
        TPointer playerStatusAddr = data.getPointer(24);
        TPointer avSyncControlAddr = data.getPointer(32);

        int count = getVblankSyncCount(avSyncControlAddr);
        if (count > 0) {
        	setVblankSyncCount(avSyncControlAddr,  count - 1);
        }

        hleSyscall(Modules.ThreadManForUserModule.sceKernelSetEventFlag(playerStatusAddr.getValue32(24), 0xF));

        return 0;
    }

    @HLEFunction(nid = 0x46F61F8B, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerGetVideoData(@CheckArgument("checkPlayerPlaying") TPointer32 psmfPlayer, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=12, usage=Usage.inout) TPointer32 videoDataAddr) {
    	TPointer data = psmfPlayer.getPointer();
        TPointer avSyncControlAddr = data.getPointer(32);
        TPointer videoBufferAddr = avSyncControlAddr.getPointer(24);
        TPointer audioBufferAddr = avSyncControlAddr.getPointer(28);
        TPointer decodeControlAddr = avSyncControlAddr.getPointer(32);
        TPointer readControlAddr = avSyncControlAddr.getPointer(36);
		TPointer ringbufferAddr = readControlAddr.getPointer(48);
		TPointer psmfControlAddr = avSyncControlAddr.getPointer(44);
    	int readStatus = AtomicValue.get(readControlAddr.getPointer(4)); // ScePsmfPlayerReadStatus
        int audioFlag = AtomicValue.get(avSyncControlAddr.getPointer(16)); // ScePsmfPlayerASCAudioFlag
		int decodeStatus = AtomicValue.get(decodeControlAddr.getPointer(72)); // ScePsmfPlayerDecodeStatus

        int result = 0;
		int numberUsedAudioBuffers = audioBufferAddr.getValue32(12);
		int numberUsedDisplayBuffers = videoBufferAddr.getValue32(16);
        int readyFlag = AtomicValue.get(avSyncControlAddr.getPointer(12)); // ScePsmfPlayerASCReadyFlag
        if (log.isTraceEnabled()) {
        	log.trace(String.format("scePsmfPlayerGetVideoData readStatus=0x%X, readyFlag=0x%X, audioFlag=0x%X, decodeStatus=0x%X, avSyncControlAddr.getValue32(20)=%d, numberUsedDisplayBuffers=%d, numberUsedAudioBuffers=%d", readStatus, readyFlag, audioFlag, decodeStatus, avSyncControlAddr.getValue32(20), numberUsedDisplayBuffers, numberUsedAudioBuffers));
        }
        switch (readyFlag) {
	        case PSMF_PLAYER_ASC_READY_FLAG_PLAY_CONTINUE:
	        	result = getVideoDataPlayContinue(avSyncControlAddr, videoDataAddr);
        		break;
	        case PSMF_PLAYER_ASC_READY_FLAG_PAUSE_CONTINUE:
				result = getVideoDataPauseContinue(avSyncControlAddr, videoDataAddr);
	        	break;
	        case PSMF_PLAYER_ASC_READY_FLAG_SLOWMOTION_CONTINUE:
	        case PSMF_PLAYER_ASC_READY_FLAG_FORWARD_CONTINUE:
	        case PSMF_PLAYER_ASC_READY_FLAG_REWIND_CONTINUE:
	        	if (getVblankSyncCount(avSyncControlAddr) > 0 || numberUsedDisplayBuffers <= 0 || copyYCbCr(videoBufferAddr) < 0) {
	        		result = decodeImageToVideoDataAddr(avSyncControlAddr, videoDataAddr);
	        	} else {
	        		result = decodeImageToVideoDataAddr(avSyncControlAddr, videoDataAddr);
	        		setVblankSyncCount(avSyncControlAddr, getVblankSyncInterval(avSyncControlAddr));
	        	}
	        	break;
	        case PSMF_PLAYER_ASC_READY_FLAG_PLAY:
	            if ((audioFlag & 0x1) == 0) {
	        		int ringbufferAvailableSize = Modules.sceMpegModule.sceMpegRingbufferAvailableSize(ringbufferAddr);
	            	if (log.isTraceEnabled()) {
	            		log.trace(String.format("scePsmfPlayerGetVideoData: sceMpegRingbufferAvailableSize returned 0x%X", ringbufferAvailableSize));
	            	}
		        	if (readStatus == 0x2 || (ringbufferAvailableSize < 32 && getNumberFreeDisplayBuffers(videoBufferAddr) == 0)) {
			            AtomicValue.or(avSyncControlAddr.getPointer(16), 0x1); // ScePsmfPlayerASCAudioFlag
		        	}
	            }
	            if ((audioFlag & 0x2) == 0) {
	        		if (avSyncControlAddr.getValue32(20) == 0 || (avSyncControlAddr.getValue32(20) & 0x2) != 0) {
			            AtomicValue.or(avSyncControlAddr.getPointer(16), 0x2); // ScePsmfPlayerASCAudioFlag
	        		} else {
		        		int ringbufferAvailableSize = Modules.sceMpegModule.sceMpegRingbufferAvailableSize(ringbufferAddr);
		            	if (log.isTraceEnabled()) {
		            		log.trace(String.format("scePsmfPlayerGetVideoData: sceMpegRingbufferAvailableSize returned 0x%X", ringbufferAvailableSize));
		            	}
			        	if (readStatus == 0x2 || (ringbufferAvailableSize < 32 && getNumberFreeAudioBuffers(audioBufferAddr) == 0)) {
			        		int displayBufferIndex = videoBufferAddr.getValue32(12);
			        		TPointer displayDataArrayAddr = videoBufferAddr.getPointer(4);
			        		int videoPts = displayDataArrayAddr.getValue32(displayBufferIndex * 8 + 4);

			        		int audioBufferIndex = audioBufferAddr.getValue32(8);
			        		TPointer audioDataArrayAddr = audioBufferAddr.getPointer(0);
			        		int audioPts = audioDataArrayAddr.getValue32(audioBufferIndex * 8 + 4);

			        		if (numberUsedDisplayBuffers == 0 || numberUsedAudioBuffers == 0 || videoPts - audioPts <= 2 * videoTimestampStep) {
					            AtomicValue.or(avSyncControlAddr.getPointer(16), 0x2); // ScePsmfPlayerASCAudioFlag
			        		} else {
			        			consumeOneAudioBuffer(audioBufferAddr);
			        		}
			        	}
	        		}
	            }

	            audioFlag = AtomicValue.get(avSyncControlAddr.getPointer(16)); // ScePsmfPlayerASCAudioFlag
	            if ((audioFlag & 0x3) == 0x3) {
	            	int numberOfAtracStreams = psmfControlAddr.getValue32(16);
	            	if (numberOfAtracStreams > 0) {
	            		avSyncControlAddr.setValue32(20, avSyncControlAddr.getValue32(20) | 0x1);
	            	}
	            	clearVblankSyncCount(avSyncControlAddr);
	            	setVblankSyncInterval(avSyncControlAddr, 2);
		    		AtomicValue.set(avSyncControlAddr.getPointer(12), PSMF_PLAYER_ASC_READY_FLAG_PLAY_CONTINUE); // ScePsmfPlayerASCReadyFlag
	            }
				result = getVideoDataPauseContinue(avSyncControlAddr, videoDataAddr);
	        	break;
	        case PSMF_PLAYER_ASC_READY_FLAG_PAUSE:
	            if ((audioFlag & 0x1) == 0) {
	        		int ringbufferAvailableSize = Modules.sceMpegModule.sceMpegRingbufferAvailableSize(ringbufferAddr);
	            	if (log.isTraceEnabled()) {
	            		log.trace(String.format("scePsmfPlayerGetVideoData: sceMpegRingbufferAvailableSize returned 0x%X", ringbufferAvailableSize));
	            	}
		        	if (decodeStatus == PSMF_PLAYER_DECODE_STATUS_END || ((readStatus == 0x2 || ringbufferAvailableSize < 32) && getNumberFreeDisplayBuffers(videoBufferAddr) == 0)) {
			            AtomicValue.or(avSyncControlAddr.getPointer(16), 0x1); // ScePsmfPlayerASCAudioFlag
		        	}
	            }
	            if ((audioFlag & 0x2) == 0) {
	        		if (avSyncControlAddr.getValue32(20) == 0 || (avSyncControlAddr.getValue32(20) & 0x2) != 0) {
			            AtomicValue.or(avSyncControlAddr.getPointer(16), 0x2); // ScePsmfPlayerASCAudioFlag
	        		} else {
		        		int ringbufferAvailableSize = Modules.sceMpegModule.sceMpegRingbufferAvailableSize(ringbufferAddr);
		            	if (log.isTraceEnabled()) {
		            		log.trace(String.format("scePsmfPlayerGetVideoData: sceMpegRingbufferAvailableSize returned 0x%X", ringbufferAvailableSize));
		            	}
			        	if (decodeStatus == PSMF_PLAYER_DECODE_STATUS_END || ((readStatus == 0x2 || ringbufferAvailableSize < 32) && getNumberFreeAudioBuffers(audioBufferAddr) == 0)) {
			        		int displayBufferIndex = videoBufferAddr.getValue32(12);
			        		TPointer displayDataArrayAddr = videoBufferAddr.getPointer(4);
			        		int videoPts = displayDataArrayAddr.getValue32(displayBufferIndex * 8 + 4);

			        		int audioBufferIndex = audioBufferAddr.getValue32(8);
			        		TPointer audioDataArrayAddr = audioBufferAddr.getPointer(0);
			        		int audioPts = audioDataArrayAddr.getValue32(audioBufferIndex * 8 + 4);

			        		if (numberUsedDisplayBuffers == 0 || numberUsedAudioBuffers == 0 || videoPts - audioPts <= 2 * videoTimestampStep) {
					            AtomicValue.or(avSyncControlAddr.getPointer(16), 0x2); // ScePsmfPlayerASCAudioFlag
			        		} else {
			        			consumeOneAudioBuffer(audioBufferAddr);
			        		}
			        	}
	        		}
	            }

	            audioFlag = AtomicValue.get(avSyncControlAddr.getPointer(16)); // ScePsmfPlayerASCAudioFlag
	            if ((audioFlag & 0x3) == 0x3) {
	            	if (getVblankSyncCount(avSyncControlAddr) == 0) {
	            		avSyncControlAddr.setValue32(20, avSyncControlAddr.getValue32(20) & ~0x1);
			    		AtomicValue.set(avSyncControlAddr.getPointer(12), PSMF_PLAYER_ASC_READY_FLAG_PAUSE_CONTINUE); // ScePsmfPlayerASCReadyFlag
	            	}
		        	result = getVideoDataPlayContinue(avSyncControlAddr, videoDataAddr);
	            } else {
	    			result = getVideoDataPauseContinue(avSyncControlAddr, videoDataAddr);
	            }
	        	break;
	        case PSMF_PLAYER_ASC_READY_FLAG_FORWARD_SLOW:
	        	result = getVideoDataRewindForward(avSyncControlAddr, videoDataAddr, 15, PSMF_PLAYER_ASC_READY_FLAG_FORWARD_CONTINUE);
	        	break;
	        case PSMF_PLAYER_ASC_READY_FLAG_FORWARD_NORMAL:
	        	result = getVideoDataRewindForward(avSyncControlAddr, videoDataAddr, 20, PSMF_PLAYER_ASC_READY_FLAG_FORWARD_CONTINUE);
	        	break;
	        case PSMF_PLAYER_ASC_READY_FLAG_FORWARD_FAST:
	        	result = getVideoDataRewindForward(avSyncControlAddr, videoDataAddr, 25, PSMF_PLAYER_ASC_READY_FLAG_FORWARD_CONTINUE);
	        	break;
	        case PSMF_PLAYER_ASC_READY_FLAG_REWIND_SLOW:
	        	result = getVideoDataRewindForward(avSyncControlAddr, videoDataAddr, 15, PSMF_PLAYER_ASC_READY_FLAG_REWIND_CONTINUE);
	        	break;
	        case PSMF_PLAYER_ASC_READY_FLAG_REWIND_NORMAL:
	        	result = getVideoDataRewindForward(avSyncControlAddr, videoDataAddr, 20, PSMF_PLAYER_ASC_READY_FLAG_REWIND_CONTINUE);
	        	break;
	        case PSMF_PLAYER_ASC_READY_FLAG_REWIND_FAST:
	        	result = getVideoDataRewindForward(avSyncControlAddr, videoDataAddr, 25, PSMF_PLAYER_ASC_READY_FLAG_REWIND_CONTINUE);
	        	break;
	        case PSMF_PLAYER_ASC_READY_FLAG_SLOWMOTION:
        		avSyncControlAddr.setValue32(20, avSyncControlAddr.getValue32(20) & ~0x1);
	            AtomicValue.or(avSyncControlAddr.getPointer(16), 0x2); // ScePsmfPlayerASCAudioFlag
	            audioFlag = AtomicValue.get(avSyncControlAddr.getPointer(16)); // ScePsmfPlayerASCAudioFlag
	            if ((audioFlag & 0x1) == 0) {
	        		int ringbufferAvailableSize = Modules.sceMpegModule.sceMpegRingbufferAvailableSize(ringbufferAddr);
	            	if (log.isTraceEnabled()) {
	            		log.trace(String.format("scePsmfPlayerGetVideoData: sceMpegRingbufferAvailableSize returned 0x%X", ringbufferAvailableSize));
	            	}
		        	if (decodeStatus == PSMF_PLAYER_DECODE_STATUS_END || ((readStatus == 0x2 || ringbufferAvailableSize < 32) && getNumberFreeDisplayBuffers(videoBufferAddr) == 0)) {
			            AtomicValue.or(avSyncControlAddr.getPointer(16), 0x1); // ScePsmfPlayerASCAudioFlag
		        	}
	            }
	            audioFlag = AtomicValue.get(avSyncControlAddr.getPointer(16)); // ScePsmfPlayerASCAudioFlag
	            if ((audioFlag & 0x1) != 0) {
	            	clearVblankSyncCount(avSyncControlAddr);
	            	setVblankSyncInterval(avSyncControlAddr, 6);
		    		AtomicValue.set(avSyncControlAddr.getPointer(12), PSMF_PLAYER_ASC_READY_FLAG_SLOWMOTION_CONTINUE); // ScePsmfPlayerASCReadyFlag
	            }
    			result = getVideoDataPauseContinue(avSyncControlAddr, videoDataAddr);
	        	break;
	        case 1:
	        case 4:
	        case 5:
	        case 6:
        	default:
        		videoBufferAddr.setValue32(32, 0);
        		result = ERROR_PSMFPLAYER_NO_DATA;
        		break;
        }

        if (log.isTraceEnabled()) {
        	log.trace(String.format("scePsmfPlayerGetVideoData returning 0x%X, audioFlag=0x%X, readyFlag=0x%X, avSyncControlAddr.getValue32(20)=%d", result, AtomicValue.get(avSyncControlAddr.getPointer(16)), AtomicValue.get(avSyncControlAddr.getPointer(12)), avSyncControlAddr.getValue32(20)));
        }

        return result;
    }

    @HLEFunction(nid = 0xB9848A74, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerGetAudioData(@CheckArgument("checkPlayerPlaying") TPointer32 psmfPlayer, TPointer audioDataAddr) {
    	TPointer data = psmfPlayer.getPointer();
        TPointer avSyncControlAddr = data.getPointer(32);
        TPointer videoBufferAddr = avSyncControlAddr.getPointer(24);
        TPointer audioBufferAddr = avSyncControlAddr.getPointer(28);
        int result;

        int audioFlag = AtomicValue.get(avSyncControlAddr.getPointer(16)); // ScePsmfPlayerASCAudioFlag
        if (log.isTraceEnabled()) {
        	log.trace(String.format("scePsmfPlayerGetAudioData audioFlag=0x%X", audioFlag));
        }
        if ((audioFlag & 0x3) != 0x3) {
        	return ERROR_PSMFPLAYER_NO_DATA;
        }

        int numberUsedAudioBuffers = audioBufferAddr.getValue32(12);
        if (log.isTraceEnabled()) {
        	log.trace(String.format("scePsmfPlayerGetAudioData numberUsedAudioBuffers=%d, avSyncControlAddr.getValue32(20)=%d", numberUsedAudioBuffers, avSyncControlAddr.getValue32(20)));
        }
		if (numberUsedAudioBuffers == 0) {
			return ERROR_PSMFPLAYER_NO_DATA;
		}

		if (avSyncControlAddr.getValue32(20) == 1) {
    		int audioBufferIndex = audioBufferAddr.getValue32(4);

    		TPointer audioDataArrayAddr = audioBufferAddr.getPointer(0);
    		TPointer audioBuffer = audioDataArrayAddr.getPointer(audioBufferIndex * 8 + 0);
    		int atracOutputSize = audioBufferAddr.getValue32(20);
    		audioDataAddr.memcpy(audioBuffer, atracOutputSize);

    		result = 0;
        } else {
    		int numberUsedDisplayBuffers = videoBufferAddr.getValue32(16);
            if (log.isTraceEnabled()) {
            	log.trace(String.format("scePsmfPlayerGetAudioData numberUsedDisplayBuffers=%d", numberUsedDisplayBuffers));
            }
    		if (numberUsedDisplayBuffers == 0) {
            	return ERROR_PSMFPLAYER_NO_DATA;
    		}
    		int displayBufferIndex = videoBufferAddr.getValue32(12);
    		TPointer displayDataArrayAddr = videoBufferAddr.getPointer(4);
    		int videoPts = displayDataArrayAddr.getValue32(displayBufferIndex * 8 + 4);

    		int audioBufferIndex = audioBufferAddr.getValue32(4);

    		TPointer audioDataArrayAddr = audioBufferAddr.getPointer(0);
    		int audioPts = audioDataArrayAddr.getValue32(audioBufferIndex * 8 + 4);

            if (log.isTraceEnabled()) {
            	log.trace(String.format("scePsmfPlayerGetAudioData videoPts=0x%X, audioPts=0x%X", videoPts, audioPts));
            }
    		if (videoPts - audioPts <= videoTimestampStep) {
    			return ERROR_PSMFPLAYER_NO_DATA;
    		}

    		result = ERROR_PSMFPLAYER_NO_DATA;
        }

		consumeOneAudioBuffer(audioBufferAddr);

		return result;
    }

    @HLEFunction(nid = 0xF8EF08A6, version = 150)
    public int scePsmfPlayerGetCurrentStatus(@CheckArgument("checkPlayerInitialized0") TPointer32 psmfPlayer) {
    	TPointer data = psmfPlayer.getPointer();
    	TPointer playerStatusAddr = data.getPointer(24);
    	int playerStatus = getPlayerStatus(playerStatusAddr);

    	return playerStatus;
    }

    @HLEFunction(nid = 0xDF089680, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerGetPsmfInfo(@CheckArgument("checkPlayerStandby") TPointer32 psmfPlayer, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=20, usage=Usage.out) TPointer32 psmfInfoAddr) {
    	TPointer data = psmfPlayer.getPointer();
        TPointer playerStatusAddr = data.getPointer(24);
        TPointer psmfControlAddr = data.getPointer(28);
    	psmfInfoAddr.setValue(0, psmfControlAddr.getValue32(0)); // endTimestamp - startTimestamp
    	psmfInfoAddr.setValue(4, psmfControlAddr.getValue32(12)); // numberOfAvcStreams
    	psmfInfoAddr.setValue(8, psmfControlAddr.getValue32(16)); // numberOfAtracStreams
    	psmfInfoAddr.setValue(12, psmfControlAddr.getValue32(20)); // numberOfPcmStreams
    	psmfInfoAddr.setValue(16, getPlayerVersion(playerStatusAddr)); // version

    	// The game "LocoRoco 2" is using a special version of scePsmfPlayerGetPsmfInfo()
    	// which is having 4 parameters instead of 2.
    	// This special version is detected using the module version stored into the libpsmfplayer.prx
    	// file itself.
    	if (getModuleVersion() == 0x03090510) {
        	TPointer psmf = new TPointer(psmfControlAddr, 24);
        	TPointer32 psmf32 = new TPointer32(psmf);
        	TPointer32 videoInfoBuffer = Utilities.allocatePointer32(8);
        	int result = Modules.scePsmfModule.scePsmfGetVideoInfo(psmf32, videoInfoBuffer);
        	if (result != 0) {
        		return result;
        	}

        	int videoWidthAddr = Emulator.getProcessor().cpu._a2;
        	int videoHeightAddr = Emulator.getProcessor().cpu._a3;
        	if (videoWidthAddr != 0) {
        		getMemory().write32(videoWidthAddr, videoInfoBuffer.getValue(0));
        	}
        	if (videoHeightAddr != 0) {
        		getMemory().write32(videoHeightAddr, videoInfoBuffer.getValue(4));
        	}
    	}

    	return 0;
    }

    @HLEFunction(nid = 0x1E57A8E7, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerConfigPlayer(@CheckArgument("checkPlayerInitialized") TPointer32 psmfPlayer, int configMode, int configAttr) {
    	TPointer data = psmfPlayer.getPointer();
        TPointer playerStatusAddr = data.getPointer(24);

    	int result = 0;
    	if (configMode == PSMF_PLAYER_CONFIG_MODE_LOOP) {
    		if (configAttr != PSMF_PLAYER_LOOPING && configAttr != PSMF_PLAYER_NOT_LOOPING) {
    			return ERROR_PSMFPLAYER_INVALID_CONFIG_VALUE;
    		}
            playerStatusAddr.setValue32(12, configAttr);
    	} else if (configMode == PSMF_PLAYER_CONFIG_MODE_PIXEL_TYPE) {
	        TPointer decodeControlAddr = data.getPointer(36);
	        switch (configAttr) {
	        	case PSMF_PLAYER_PIXEL_TYPE_UNCHANGED:
	        		// -1 means nothing to change
	        		break;
	        	case TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650:
	        	case TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551:
	        	case TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444:
	        	case TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888:
	        		TPointer32 modeAddr = allocatePointer32(8);
	        		modeAddr.setValue(0, -1);
	        		if (decodeControlAddr.getValue32(68) != configAttr) {
	        			decodeControlAddr.setValue32(68, configAttr);
	        			modeAddr.setValue(4, configAttr);
	        			TPointer mpeg = decodeControlAddr.getPointer(0);
	        			TPointer32 mpeg32 = new TPointer32(mpeg);
	        			result = Modules.sceMpegModule.sceMpegAvcDecodeMode(mpeg32, modeAddr);
		            	if (log.isTraceEnabled()) {
		            		log.trace(String.format("scePsmfPlayerConfigPlayer: sceMpegAvcDecodeMode returned 0x%X", result));
		            	}
	        		}
	        		break;
	        	case 4:
	        		// This config value is only supported in the versions of libpsmfplayer.prx
	        		// having provided enough memory for allocating a yuv420Buffer at scePsmfPlayerCreate().
	        		TPointer yuv420Buffer = decodeControlAddr.getPointer(80);
	        		if (yuv420Buffer.isNull()) {
	        			return ERROR_PSMFPLAYER_INVALID_CONFIG_VALUE;
	        		}

	        		if (getPlayerStatus(playerStatusAddr) != PSMF_PLAYER_STATUS_INIT) {
	    				return ERROR_PSMFPLAYER_NOT_INITIALIZED;
	        		}
	        		if (decodeControlAddr.getValue32(68) != configAttr) {
	        			decodeControlAddr.setValue32(68, configAttr);
	        		}
	                break;
	            default:
	            	return ERROR_PSMFPLAYER_INVALID_CONFIG_VALUE;
	        }
    	} else {
    		return ERROR_PSMFPLAYER_INVALID_CONFIG_MODE;
    	}

    	return result;
    }

    @HLEFunction(nid = 0xA3D81169, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerChangePlayMode(@CheckArgument("checkPlayerInitialized") TPointer32 psmfPlayer, int playMode, int playSpeed) {
    	TPointer data = psmfPlayer.getPointer();
        TPointer playerStatusAddr = data.getPointer(24);
        TPointer avSyncControlAddr = data.getPointer(32);
        TPointer decodeControlAddr = data.getPointer(36);
        TPointer psmfControlAddr = data.getPointer(28);

    	int playerStatus = getPlayerStatus(playerStatusAddr);
    	if (playerStatus != PSMF_PLAYER_STATUS_PLAYING && playerStatus != PSMF_PLAYER_STATUS_PLAYING_FINISHED) {
    		return ERROR_PSMFPLAYER_NOT_INITIALIZED;
    	}

		int oldPlayMode = playerStatusAddr.getValue32(4);
		int oldPlaySpeed = playerStatusAddr.getValue32(16);
		boolean needUpdate = false;
    	switch (playMode) {
	    	case PSMF_PLAYER_MODE_PLAY:
	    		if (oldPlayMode != playMode) {
	    			needUpdate = true;
	    			playSpeed = PSMF_PLAYER_SPEED_SLOW;
	    		}
	    		break;
	    	case PSMF_PLAYER_MODE_SLOWMOTION:
	    	case PSMF_PLAYER_MODE_STEPFRAME:
	    		needUpdate = true;
    			playSpeed = PSMF_PLAYER_SPEED_SLOW;
	    		break;
	    	case PSMF_PLAYER_MODE_PAUSE:
	    		if (oldPlayMode != playMode && oldPlayMode != PSMF_PLAYER_MODE_STEPFRAME) {
	    			needUpdate = true;
	    			playSpeed = PSMF_PLAYER_SPEED_SLOW;
	    		}
	    		break;
	    	case PSMF_PLAYER_MODE_FORWARD:
	    	case PSMF_PLAYER_MODE_REWIND:
	    		int playerVersion = getPlayerVersion(playerStatusAddr);
	    		if (playerVersion != PSMF_PLAYER_VERSION_FULL) {
	    			return ERROR_PSMFPLAYER_NOT_SUPPORTED;
	    		}
	    		if (playSpeed < PSMF_PLAYER_SPEED_SLOW || playSpeed > PSMF_PLAYER_SPEED_FAST) {
	    			return ERROR_PSMFPLAYER_INVALID_CONFIG_VALUE;
	    		}
	    		if (oldPlayMode != playMode || oldPlaySpeed != playSpeed) {
	    			needUpdate = true;
	    		}
	    		break;
    		default:
        		return ERROR_PSMFPLAYER_INVALID_CONFIG_MODE;
    	}

    	if (needUpdate) {
    		if (oldPlayMode == PSMF_PLAYER_MODE_FORWARD || oldPlayMode == PSMF_PLAYER_MODE_REWIND || playMode == PSMF_PLAYER_MODE_FORWARD || playMode == PSMF_PLAYER_MODE_REWIND) {
				TPointer videoBufferAddr = avSyncControlAddr.getPointer(24);
				int timestamp = videoBufferAddr.getValue32(40);
		    	AtomicValue.set(videoBufferAddr.getPointer(44), 2); // ScePsmfPlayerVBufChangeFlag
		    	videoBufferAddr.setValue32(8, 0);
		    	videoBufferAddr.setValue32(12, 0);
		    	videoBufferAddr.setValue32(16, 0);
		    	TPointer audioBufferAddr = avSyncControlAddr.getPointer(28);
		    	audioBufferAddr.setValue32(24, audioBufferAddr.getValue32(24) | 0x1);
		    	audioBufferAddr.setValue32(4, 0);
		    	audioBufferAddr.setValue32(8, 0);
		    	audioBufferAddr.setValue32(12, 0);
		    	if (playMode == PSMF_PLAYER_MODE_FORWARD) {
		    		setTimestampWithDirection(decodeControlAddr, timestamp, playSpeed, 1);
		    	} else if (playMode == PSMF_PLAYER_MODE_REWIND) {
		    		setTimestampWithDirection(decodeControlAddr, timestamp, playSpeed, -1);
		    	} else {
		    		setTimestamp(decodeControlAddr, timestamp);
		    		AtomicValue.clear(avSyncControlAddr.getPointer(16), 3); // ScePsmfPlayerASCAudioFlag
	            	int numberOfAtracStreams = psmfControlAddr.getValue32(16);
	            	if (numberOfAtracStreams > 0) {
	            		avSyncControlAddr.setValue32(20, avSyncControlAddr.getValue32(20) | 0x1);
	            	}
		    	}
			} else {
	    		AtomicValue.clear(avSyncControlAddr.getPointer(16), 3); // ScePsmfPlayerASCAudioFlag
			}
    		AtomicValue.set(avSyncControlAddr.getPointer(12), getReadyFlag(playMode, playSpeed)); // ScePsmfPlayerASCReadyFlag
    		playerStatusAddr.setValue32(4, playMode);
    		playerStatusAddr.setValue32(16, playSpeed);
    	}

    	return 0;
    }

    @HLEFunction(nid = 0x68F07175, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerGetCurrentAudioStream(@CheckArgument("checkPlayerStandby") TPointer32 psmfPlayer, @BufferInfo(usage=Usage.out) TPointer32 audioCodecAddr, @BufferInfo(usage=Usage.out) TPointer32 audioStreamNumAddr) {
    	TPointer data = psmfPlayer.getPointer();
        TPointer psmfControlAddr = data.getPointer(28);

        audioCodecAddr.setValue(psmfControlAddr.getValue32(152));
        audioStreamNumAddr.setValue(psmfControlAddr.getValue32(156));

        return 0;
    }

    @HLEFunction(nid = 0xF3EFAA91, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerGetCurrentPlayMode(@CheckArgument("checkPlayerInitialized0") TPointer32 psmfPlayer, @BufferInfo(usage=Usage.out) TPointer32 playModeAddr, @BufferInfo(usage=Usage.out) TPointer32 playSpeedAddr) {
    	TPointer data = psmfPlayer.getPointer();
        TPointer playerStatusAddr = data.getPointer(24);

        playModeAddr.setValue(playerStatusAddr.getValue32(4));
		playSpeedAddr.setValue(playerStatusAddr.getValue32(16));

		return 0;
    }

    @HLEFunction(nid = 0x3ED62233, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerGetCurrentPts(@CheckArgument("checkPlayerStandby") TPointer32 psmfPlayer, @BufferInfo(usage=Usage.out) TPointer32 currentPtsAddr) {
    	TPointer data = psmfPlayer.getPointer();
        TPointer avSyncControlAddr = data.getPointer(32);
        TPointer videoBufferAddr = avSyncControlAddr.getPointer(24);
        TPointer psmfControlAddr = avSyncControlAddr.getPointer(44);
        if (videoBufferAddr.getValue32(32) == 0) {
        	return ERROR_PSMFPLAYER_NO_DATA;
        }
    	int currentPts = videoBufferAddr.getValue32(28) - psmfControlAddr.getValue32(4);
    	currentPtsAddr.setValue(currentPts);

    	return 0;
    }

    @HLEFunction(nid = 0x9FF2B2E7, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerGetCurrentVideoStream(@CheckArgument("checkPlayerStandby") TPointer32 psmfPlayer, @BufferInfo(usage=Usage.out) TPointer32 videoCodecAddr, @BufferInfo(usage=Usage.out) TPointer32 videoStreamNumAddr) {
    	TPointer data = psmfPlayer.getPointer();
        TPointer psmfControlAddr = data.getPointer(28);

        videoCodecAddr.setValue(psmfControlAddr.getValue32(136));
        videoStreamNumAddr.setValue(psmfControlAddr.getValue32(140));

        return 0;
    }

    @HLEFunction(nid = 0x2BEB1569, version = 150)
    public int scePsmfPlayerBreak(@CheckArgument("checkPlayerInitialized") TPointer32 psmfPlayer) {
    	TPointer data = psmfPlayer.getPointer();
    	TPointer readControlAddr = data.getPointer(48);

    	return breakPsmf(readControlAddr);
    }

    @HLEFunction(nid = 0x76C0F4AE, version = 150)
    public int scePsmfPlayerSetPsmfOffset(@CheckArgument("checkPlayerInitialized0") TPointer32 psmfPlayer, PspString fileAddr, int offset) {
    	return hlePsmfPlayerSetPsmf(psmfPlayer, fileAddr, offset, false);
    }

    @HLEFunction(nid = 0xA72DB4F9, version = 150)
    public int scePsmfPlayerSetPsmfOffsetCB(@CheckArgument("checkPlayerInitialized0") TPointer32 psmfPlayer, PspString fileAddr, int offset) {
    	return hlePsmfPlayerSetPsmf(psmfPlayer, fileAddr, offset, true);
    }

    @HLEFunction(nid = 0x2D0E4E0A, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerSetTempBuf(TPointer32 psmfPlayer, @CanBeNull TPointer tempBufAddr, int tempBufSize) {
    	if (tempBufAddr.isNotNull() && tempBufSize < 0x10000) {
    		return ERROR_PSMFPLAYER_INVALID_CONFIG_VALUE;
    	}

    	TPointer data = psmfPlayer.getPointer();
		TPointer playerStatusAddr = data.getPointer(24);

		int playerStatus = getPlayerStatus(playerStatusAddr);
		if (playerStatus != PSMF_PLAYER_STATUS_INIT) {
			return ERROR_PSMFPLAYER_NOT_INITIALIZED;
		}

		data.setPointer(64, tempBufAddr);

		return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x75F03FA2, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerSelectSpecificVideo(@CheckArgument("checkPlayerInitialized") TPointer32 psmfPlayer, int videoCodec, int videoStreamNum) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x85461EFF, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerSelectSpecificAudio(@CheckArgument("checkPlayerInitialized") TPointer32 psmfPlayer, int audioCodec, int audioStreamNum) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8A9EBDCD, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerSelectVideo(@CheckArgument("checkPlayerInitialized") TPointer32 psmfPlayer) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB8D10C56, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerSelectAudio(@CheckArgument("checkPlayerInitialized") TPointer32 psmfPlayer) {
    	return 0;
    }
}
