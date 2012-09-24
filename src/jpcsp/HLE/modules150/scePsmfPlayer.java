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

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_PSMFPLAYER_NOT_INITIALIZED;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.CheckArgument;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.PspString;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.TimeZone;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceMpegAu;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.sceMpeg;
import jpcsp.filesystems.SeekableDataInput;
import jpcsp.graphics.VideoEngine;
import jpcsp.media.MediaEngine;
import jpcsp.media.PacketChannel;
import jpcsp.settings.AbstractBoolSettingsListener;
import jpcsp.util.Debug;

import org.apache.log4j.Logger;

@HLELogging
public class scePsmfPlayer extends HLEModule {
    public static Logger log = Modules.getLogger("scePsmfPlayer");

    private class EnableMediaEngineSettingsListener extends AbstractBoolSettingsListener {
		@Override
		protected void settingsValueChanged(boolean value) {
			setEnableMediaEngine(value);
		}
    }

    @Override
    public String getName() {
        return "scePsmfPlayer";
    }

	@Override
	public void start() {
		setSettingsListener("emu.useMediaEngine", new EnableMediaEngineSettingsListener());

		super.start();
	}

    // PSMF Player timing management.
    protected static final int psmfPlayerVideoTimestampStep = sceMpeg.videoTimestampStep;
    protected static final int psmfPlayerAudioTimestampStep = sceMpeg.audioTimestampStep;
    protected static final int psmfTimestampPerSecond = sceMpeg.mpegTimestampPerSecond;
    protected int psmfMaxAheadTimestamp = 40000;

    // PSMF Player timestamp vars.
    protected Date psmfPlayerLastDate;
    protected long psmfPlayerLastTimestamp;
    protected SceMpegAu psmfPlayerAvcAu;
    protected SceMpegAu psmfPlayerAtracAu;

    // PSMF Player status.
    protected static final int PSMF_PLAYER_STATUS_NONE = 0x0;
    protected static final int PSMF_PLAYER_STATUS_INIT = 0x1;
    protected static final int PSMF_PLAYER_STATUS_STANDBY = 0x2;
    protected static final int PSMF_PLAYER_STATUS_PLAYING = 0x4;
    protected static final int PSMF_PLAYER_STATUS_ERROR = 0x100;
    protected static final int PSMF_PLAYER_STATUS_PLAYING_FINISHED = 0x200;

    // PSMF Player status vars.
    protected int psmfPlayerStatus;

    // PSMF Player mode.
    protected static final int PSMF_PLAYER_MODE_PLAY = 0;
    protected static final int PSMF_PLAYER_MODE_SLOWMOTION = 1;
    protected static final int PSMF_PLAYER_MODE_STEPFRAME = 2;
    protected static final int PSMF_PLAYER_MODE_PAUSE = 3;
    protected static final int PSMF_PLAYER_MODE_FORWARD = 4;
    protected static final int PSMF_PLAYER_MODE_REWIND = 5;

    // PSMF Player stream type.
    protected static final int PSMF_PLAYER_STREAM_AVC = sceMpeg.MPEG_AVC_STREAM;
    protected static final int PSMF_PLAYER_STREAM_ATRAC = sceMpeg.MPEG_ATRAC_STREAM;
    protected static final int PSMF_PLAYER_STREAM_PCM = sceMpeg.MPEG_PCM_STREAM;
    protected static final int PSMF_PLAYER_STREAM_VIDEO = 14;
    protected static final int PSMF_PLAYER_STREAM_AUDIO = sceMpeg.MPEG_AUDIO_STREAM;

    // PSMF Player playback speed.
    protected static final int PSMF_PLAYER_SPEED_SLOW = 1;
    protected static final int PSMF_PLAYER_SPEED_NORMAL = 2;
    protected static final int PSMF_PLAYER_SPEED_FAST = 3;

    // PSMF Player config mode.
    protected static final int PSMF_PLAYER_CONFIG_MODE_LOOP = 0;
    protected static final int PSMF_PLAYER_CONFIG_MODE_PIXEL_TYPE = 1;

    // PSMF Player config loop.
    protected static final int PSMF_PLAYER_CONFIG_LOOP = 0;
    protected static final int PSMF_PLAYER_CONFIG_NO_LOOP = 1;

    // PSMF Player config pixel type.
    protected static final int PSMF_PLAYER_PIXEL_TYPE_NONE = -1;

    // PSMF Player version.
    protected static final int PSMF_PLAYER_VERSION_FULL = 0;
    protected static final int PSMF_PLAYER_VERSION_BASIC = 1;
    protected static final int PSMF_PLAYER_VERSION_NET = 2;

    // PMF file vars.
    protected String pmfFilePath;
    protected byte[] pmfFileData;

    // PMSF info.
    // TODO: Parse the right values from the PSMF file.
    protected int psmfCurrentPts = 0;
    protected int psmfAvcStreamNum = 1;
    protected int psmfAtracStreamNum = 1;
    protected int psmfPcmStreamNum = 0;
    protected int psmfPlayerVersion = PSMF_PLAYER_VERSION_FULL;

    // PSMF Player playback params.
    protected int displayBuffer;
    protected int displayBufferSize;
    protected int playbackThreadPriority;

    // PSMF Player playback info.
    protected int videoCodec;
    protected int videoStreamNum;
    protected int audioCodec;
    protected int audioStreamNum;
    protected int playMode;
    protected int playSpeed;

    // PSMF Player video data.
    protected int videoDataFrameWidth = 512;  // Default.
    protected int videoDataDisplayBuffer;
    protected int videoDataDisplayPts;

    // PSMF Player config.
    protected int videoPixelMode = TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;  // Default.
    protected int videoLoopStatus = PSMF_PLAYER_CONFIG_NO_LOOP;  // Default.

    // PSMF Player audio size
    protected final int audioSamples = 2048;  // Default.
    protected final int audioSamplesBytes = audioSamples * 4;

    // Media Engine vars.
    protected PacketChannel pmfFileChannel;
    protected MediaEngine me;
    protected boolean useMediaEngine = false;
    protected byte[] audioDecodeBuffer;

    protected boolean checkMediaEngineState() {
        return useMediaEngine;
    }

    private void setEnableMediaEngine(boolean state) {
        useMediaEngine = state;
    }

    protected Date convertPsmfTimestampToDate(long timestamp) {
        long millis = timestamp / (psmfTimestampPerSecond / 1000);
        return new Date(millis);
    }

    private void generateFakePSMFVideo(int dest_addr, int frameWidth) {
        Memory mem = Memory.getInstance();

        Random random = new Random();
        final int pixelSize = 3;
        final int bytesPerPixel = sceDisplay.getPixelFormatBytes(videoPixelMode);
        for (int y = 0; y < 272 - pixelSize + 1; y += pixelSize) {
            int address = dest_addr + y * frameWidth * bytesPerPixel;
            final int width = Math.min(480, frameWidth);
            for (int x = 0; x < width; x += pixelSize) {
                int n = random.nextInt(256);
                int color = 0xFF000000 | (n << 16) | (n << 8) | n;
                int pixelColor = Debug.getPixelColor(color, videoPixelMode);
                if (bytesPerPixel == 4) {
                    for (int i = 0; i < pixelSize; i++) {
                        for (int j = 0; j < pixelSize; j++) {
                            mem.write32(address + (i * frameWidth + j) * 4, pixelColor);
                        }
                    }
                } else if (bytesPerPixel == 2) {
                    for (int i = 0; i < pixelSize; i++) {
                        for (int j = 0; j < pixelSize; j++) {
                            mem.write16(address + (i * frameWidth + j) * 2, (short) pixelColor);
                        }
                    }
                }
                address += pixelSize * bytesPerPixel;
            }
        }

        Date currentDate = convertPsmfTimestampToDate(psmfPlayerAvcAu.pts);
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        Debug.printFramebuffer(dest_addr, frameWidth, 10, 250, 0xFFFFFFFF, 0xFF000000, videoPixelMode, 1, " This is a faked PSMF Player video. ");

        String displayedString;
        if (psmfPlayerLastDate != null) {
            displayedString = String.format(" %s / %s ", dateFormat.format(currentDate), dateFormat.format(psmfPlayerLastDate));
            Debug.printFramebuffer(dest_addr, frameWidth, 10, 10, 0xFFFFFFFF, 0xFF000000, videoPixelMode, 2, displayedString);
        }
    }

    protected int getPsmfFileDataInt8(int index) {
    	return pmfFileData[index] & 0xFF;
    }

    protected int getPsmfFileDataInt32(int index) {
        return (getPsmfFileDataInt8(index    ) << 24) |
               (getPsmfFileDataInt8(index + 1) << 16) |
               (getPsmfFileDataInt8(index + 2) <<  8) |
               (getPsmfFileDataInt8(index + 3)      );
    }

    protected void analyzePSMFLastTimestamp() {
        if (pmfFileData != null) {
            // Endian swapped inside the buffer.
            psmfPlayerLastTimestamp = getPsmfFileDataInt32(sceMpeg.PSMF_LAST_TIMESTAMP_OFFSET);
            psmfPlayerLastDate = convertPsmfTimestampToDate(psmfPlayerLastTimestamp);
        }
    }

    public int checkPlayerInitialized(int psmfPlayer) {
    	if (psmfPlayerStatus == PSMF_PLAYER_STATUS_NONE) {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("checkPlayerInitialized player not initialized (status=0x%X)", psmfPlayerStatus));
    		}
    		throw new SceKernelErrorException(ERROR_PSMFPLAYER_NOT_INITIALIZED);
    	}

    	return psmfPlayer;
    }

    public int checkPlayerPlaying(int psmfPlayer) {
    	psmfPlayer = checkPlayerInitialized(psmfPlayer);
    	if (psmfPlayerStatus != PSMF_PLAYER_STATUS_PLAYING && psmfPlayerStatus != PSMF_PLAYER_STATUS_PLAYING_FINISHED && psmfPlayerStatus != PSMF_PLAYER_STATUS_ERROR) {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("checkPlayerInitialized player not playing (status=0x%X)", psmfPlayerStatus));
    		}
    		throw new SceKernelErrorException(ERROR_PSMFPLAYER_NOT_INITIALIZED);
    	}

    	return psmfPlayer;
    }

    protected void startMediaEngine() {
    	if (checkMediaEngineState()) {
	        if (pmfFileChannel != null && me == null) {
	            me = new MediaEngine();
	            audioDecodeBuffer = new byte[audioSamplesBytes];
	            me.init(pmfFileData);
	            me.init(pmfFileChannel, true, true);
	        }
    	}
    }

    protected int hlePsmfPlayerSetPsmf(int psmfPlayer, PspString fileAddr, boolean doCallbacks) {
    	if (psmfPlayerStatus != PSMF_PLAYER_STATUS_INIT) {
    		return ERROR_PSMFPLAYER_NOT_INITIALIZED;
    	}

    	pmfFilePath = fileAddr.getString();

        // Get the file and read it to a buffer.
        try {
            SeekableDataInput psmfFile = Modules.IoFileMgrForUserModule.getFile(pmfFilePath, 0);
            pmfFileData = new byte[(int) psmfFile.length()];
            psmfFile.readFully(pmfFileData);

            if (log.isInfoEnabled()) {
            	log.info(String.format("'%s' PSMF file loaded.", pmfFilePath));
            }

            if (checkMediaEngineState()) {
                pmfFileChannel = new PacketChannel(pmfFileData);
            }
        } catch (IOException e) {
        	log.error("scePsmfPlayerSetPsmf", e);
        }

        // Switch to STANDBY.
        psmfPlayerStatus = PSMF_PLAYER_STATUS_STANDBY;

        // Delay the thread for 100ms
        Modules.ThreadManForUserModule.hleKernelDelayThread(100000, doCallbacks);

        return 0;
    }

    @HLEFunction(nid = 0x235D8787, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerCreate(int psmfPlayer, TPointer32 psmfPlayerDataAddr) {
        // The psmfDataAddr contains three fields that are manually set before
        // scePsmfPlayerCreate is called.
        displayBuffer = psmfPlayerDataAddr.getValue(0);          // The buffer allocated for scePsmf, which is ported into scePsmfPlayer.
        displayBufferSize = psmfPlayerDataAddr.getValue(4);      // The buffer's size.
        playbackThreadPriority = psmfPlayerDataAddr.getValue(8); // Priority of the "START" thread.
        if (log.isInfoEnabled()) {
        	log.info(String.format("PSMF Player Data: displayBuffer=0x%08X, displayBufferSize=0x%X, playbackThreadPriority=%d", displayBuffer, displayBufferSize, playbackThreadPriority));
        }

        psmfPlayerAtracAu = new SceMpegAu();
        psmfPlayerAvcAu = new SceMpegAu();

        // scePsmfPlayer creates a ringbuffer with 581 packets
        psmfMaxAheadTimestamp = sceMpeg.getMaxAheadTimestamp(581);

        // Start with INIT.
        psmfPlayerStatus = PSMF_PLAYER_STATUS_INIT;

        return 0;
    }

    @HLEFunction(nid = 0x9B71A274, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerDelete(@CheckArgument("checkPlayerInitialized") int psmfPlayer) {
        if (checkMediaEngineState()) {
            if (me != null) {
                me.finish();
                me = null;
            }
            if (pmfFileChannel != null) {
            	pmfFileChannel = null;
            }
        }
        VideoEngine.getInstance().resetVideoTextures();

        // Set to NONE.
        psmfPlayerStatus = PSMF_PLAYER_STATUS_NONE;

        return 0;
    }

    @HLEFunction(nid = 0x3D6D25A9, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerSetPsmf(@CheckArgument("checkPlayerInitialized") int psmfPlayer, PspString fileAddr) {
    	return hlePsmfPlayerSetPsmf(psmfPlayer, fileAddr, false);
    }

    @HLEFunction(nid = 0x58B83577, version = 150)
    public int scePsmfPlayerSetPsmfCB(@CheckArgument("checkPlayerInitialized") int psmfPlayer, PspString fileAddr) {
    	return hlePsmfPlayerSetPsmf(psmfPlayer, fileAddr, true);
    }

    @HLEFunction(nid = 0xE792CD94, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerReleasePsmf(@CheckArgument("checkPlayerInitialized") int psmfPlayer) {
    	if (psmfPlayerStatus != PSMF_PLAYER_STATUS_STANDBY) {
    		return ERROR_PSMFPLAYER_NOT_INITIALIZED;
    	}

    	if (checkMediaEngineState()) {
            if (me != null) {
                me.finish();
                me = null;
            }
            if (pmfFileChannel != null) {
            	pmfFileChannel = null;
            }
        }
        VideoEngine.getInstance().resetVideoTextures();

        // Go back to INIT, because some applications recognize that another file can be
        // loaded after scePsmfPlayerReleasePsmf has been called.
        psmfPlayerStatus = PSMF_PLAYER_STATUS_INIT;

        return 0;
    }

    @HLEFunction(nid = 0x95A84EE5, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerStart(@CheckArgument("checkPlayerInitialized") int psmfPlayer, @CanBeNull TPointer32 initPlayInfoAddr, int initPts) {
        // Read the playback parameters.
        if (initPlayInfoAddr.isNotNull()) {
	        videoCodec = initPlayInfoAddr.getValue(0);
	        videoStreamNum = initPlayInfoAddr.getValue(4);
	        audioCodec = initPlayInfoAddr.getValue(8);
	        audioStreamNum = initPlayInfoAddr.getValue(12);
	        playMode = initPlayInfoAddr.getValue(16);
	        playSpeed = initPlayInfoAddr.getValue(20);

	        if (log.isInfoEnabled()) {
	        	log.info(String.format("Found play info data: videoCodec=0x%X, videoStreamNum=%d, audioCodec=0x%X, audioStreamNum=%d, playMode=%d, playSpeed=%d", videoCodec, videoStreamNum, audioCodec, audioStreamNum, playMode, playSpeed));
	        }
        }

        // Initialize the current PTS and DTS with the given timestamp (mostly set to 0).
        psmfPlayerAtracAu.dts = initPts;
        psmfPlayerAtracAu.pts = initPts;
        psmfPlayerAvcAu.dts = initPts;
        psmfPlayerAvcAu.pts = initPts;

        analyzePSMFLastTimestamp();

        startMediaEngine();

        // Switch to PLAYING.
        psmfPlayerStatus = PSMF_PLAYER_STATUS_PLAYING;

        return 0;
    }

    @HLEFunction(nid = 0x3EA82A4B, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerGetAudioOutSize(@CheckArgument("checkPlayerInitialized") int psmfPlayer) {
        return audioSamplesBytes;
    }

    @HLEFunction(nid = 0x1078C008, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerStop(@CheckArgument("checkPlayerInitialized") int psmfPlayer) {
        if (checkMediaEngineState()) {
            if (me != null) {
                me.finish();
                me = null;
            }
            if (pmfFileChannel != null) {
            	pmfFileChannel = null;
            }
        }
        VideoEngine.getInstance().resetVideoTextures();

        // Always switch to STANDBY, because this PSMF can still be resumed.
        psmfPlayerStatus = PSMF_PLAYER_STATUS_STANDBY;

        Modules.ThreadManForUserModule.hleKernelDelayThread(100000, false);

        return 0;
    }

    @HLEFunction(nid = 0xA0B8CA55, version = 150)
    public int scePsmfPlayerUpdate(@CheckArgument("checkPlayerPlaying") int psmfPlayer) {
        // Can be called from interrupt.
        // Check playback status.
        if (psmfPlayerAvcAu.pts > 0) {
            if (psmfPlayerAvcAu.pts > psmfPlayerLastTimestamp) {
                // If we've reached the last timestamp, change the status to PLAYING_FINISHED.
                psmfPlayerStatus = PSMF_PLAYER_STATUS_PLAYING_FINISHED;
            }
        }

        return 0;
    }

    @HLEFunction(nid = 0x46F61F8B, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerGetVideoData(@CheckArgument("checkPlayerPlaying") int psmfPlayer, @CanBeNull TPointer32 videoDataAddr) {
    	int result = 0;

    	if (psmfPlayerStatus != PSMF_PLAYER_STATUS_PLAYING) {
    		return ERROR_PSMFPLAYER_NOT_INITIALIZED;
    	}

    	if (psmfPlayerAtracAu.pts != 0 && psmfPlayerAvcAu.pts > psmfPlayerAtracAu.pts + psmfMaxAheadTimestamp) {
            // If we're ahead of audio, return an error.
        	result = SceKernelErrors.ERROR_PSMFPLAYER_NO_MORE_DATA;
            if (log.isDebugEnabled()) {
                log.debug(String.format("scePsmfPlayerGetVideoData avcAu=[%s], returning 0x%08X", psmfPlayerAvcAu, result));
            }
            sceMpeg.delayThread(sceMpeg.mpegDecodeErrorDelay);
            return result;
        }

        if (videoDataAddr.isNotNull()) {
            videoDataFrameWidth = videoDataAddr.getValue(0);
            videoDataDisplayBuffer = videoDataAddr.getValue(4);
            videoDataDisplayPts = videoDataAddr.getValue(8);
            if (log.isDebugEnabled()) {
                log.debug(String.format("scePsmfPlayerGetVideoData videoDataFrameWidth=%d, videoDataDisplayBuffer=0x%08X, videoDataDisplayPts=%d", videoDataFrameWidth, videoDataDisplayBuffer, videoDataDisplayPts));
            }
        }

        // Check if there's already a valid pointer at videoDataAddr.
        // If not, use the displayBuffer from scePsmfPlayerCreate.
        if (Memory.isAddressGood(videoDataDisplayBuffer)) {
            displayBuffer = videoDataDisplayBuffer;
        } else if (videoDataAddr.isNotNull()) {
        	videoDataAddr.setValue(4, displayBuffer);
            // Valid frame width?
            if (videoDataFrameWidth <= 0 || videoDataFrameWidth > 512) {
            	videoDataFrameWidth = 512;
            	videoDataAddr.setValue(0, videoDataFrameWidth);
            }
        }

        // Do not cache the video image as a texture in the VideoEngine to allow fluid rendering
        VideoEngine.getInstance().addVideoTexture(displayBuffer, displayBuffer + 272 * videoDataFrameWidth * sceDisplay.getPixelFormatBytes(videoPixelMode));

        long startTime = Emulator.getClock().microTime();

    	// Write video data.
        if (checkMediaEngineState() && pmfFileChannel != null) {
        	Emulator.getClock().pause();
        	startMediaEngine();
            if (me.stepVideo()) {
            	me.writeVideoImage(displayBuffer, videoDataFrameWidth, videoPixelMode);
	            me.getCurrentVideoAu(psmfPlayerAvcAu);
            } else {
	        	psmfPlayerAvcAu.pts += psmfPlayerVideoTimestampStep;
	        	psmfPlayerAvcAu.dts = psmfPlayerAvcAu.pts - psmfPlayerVideoTimestampStep;
            }
        	Emulator.getClock().resume();
        } else {
        	psmfPlayerAvcAu.pts += psmfPlayerVideoTimestampStep;
        	psmfPlayerAvcAu.dts = psmfPlayerAvcAu.pts - psmfPlayerVideoTimestampStep;
            generateFakePSMFVideo(displayBuffer, videoDataFrameWidth);
        }

        // TODO Check if timestamp is returned
        if (videoDataAddr.isNotNull()) {
        	videoDataAddr.setValue(8, (int) psmfPlayerAvcAu.dts);
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("scePsmfPlayerGetVideoData avcAu=[%s], returning 0x%08X", psmfPlayerAvcAu, result));
        }

        sceMpeg.delayThread(startTime, sceMpeg.avcDecodeDelay);

        return result;
    }

    @HLEFunction(nid = 0xB9848A74, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerGetAudioData(@CheckArgument("checkPlayerPlaying") int psmfPlayer, TPointer audioDataAddr) {
    	int result = 0;

        if (psmfPlayerAvcAu.pts != 0 && psmfPlayerAtracAu.pts > psmfPlayerAvcAu.pts + psmfMaxAheadTimestamp) {
            // If we're ahead of video, return an error.
        	result = SceKernelErrors.ERROR_PSMFPLAYER_NO_MORE_DATA;
            if (log.isDebugEnabled()) {
            	log.debug(String.format("scePsmfPlayerGetAudioData atracAu=[%s], avcAu=[%s], returning 0x%08X", psmfPlayerAtracAu, psmfPlayerAvcAu, result));
            }
            sceMpeg.delayThread(sceMpeg.mpegDecodeErrorDelay);
            return result;
        }

        long startTime = Emulator.getClock().microTime();

    	// Write audio data
        Memory mem = Memory.getInstance();
    	int bytes = 0;
        if (checkMediaEngineState() && pmfFileChannel != null) {
        	Emulator.getClock().pause();
        	startMediaEngine();
        	if (me.stepAudio(audioSamplesBytes)) {
                bytes = me.getCurrentAudioSamples(audioDecodeBuffer);
                if (log.isDebugEnabled()) {
                	log.debug(String.format("scePsmfPlayerGetAudioData ME returned %d bytes (audioSamplesBytes=%d)", bytes, audioSamplesBytes));
                }
                mem.copyToMemory(audioDataAddr.getAddress(), ByteBuffer.wrap(audioDecodeBuffer, 0, bytes), bytes);
            	me.getCurrentAudioAu(psmfPlayerAtracAu);
        	} else {
        		psmfPlayerAtracAu.pts += psmfPlayerAudioTimestampStep;
        	}
        	Emulator.getClock().resume();
    	} else {
    		psmfPlayerAtracAu.pts += psmfPlayerAudioTimestampStep;
    		psmfPlayerAtracAu.dts = -1;
    	}
    	// Fill the rest of the buffer with 0's
    	audioDataAddr.clear(bytes, audioSamplesBytes - bytes);

        sceMpeg.delayThread(startTime, sceMpeg.atracDecodeDelay);

        if (log.isDebugEnabled()) {
        	log.debug(String.format("scePsmfPlayerGetAudioData atracAu=[%s], avcAu=[%s], returning 0x%08X", psmfPlayerAtracAu, psmfPlayerAvcAu, result));
        }

        return result;
    }

    @HLEFunction(nid = 0xF8EF08A6, version = 150)
    public int scePsmfPlayerGetCurrentStatus(@CheckArgument("checkPlayerInitialized") int psmfPlayer) {
        // scePsmfPlayerGetCurrentStatus can be called from an interrupt
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("scePsmfPlayerGetCurrentStatus returning status 0x%X", psmfPlayerStatus));
    	}

    	return psmfPlayerStatus;
    }

    @HLEFunction(nid = 0xDF089680, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerGetPsmfInfo(@CheckArgument("checkPlayerInitialized") int psmfPlayer, TPointer32 psmfInfoAddr) {
    	if (psmfPlayerStatus < PSMF_PLAYER_STATUS_STANDBY) {
    		return ERROR_PSMFPLAYER_NOT_INITIALIZED;
    	}

    	psmfInfoAddr.setValue(0, psmfCurrentPts);
        psmfInfoAddr.setValue(4, psmfAvcStreamNum);
        psmfInfoAddr.setValue(8, psmfAtracStreamNum);
        psmfInfoAddr.setValue(12, psmfPcmStreamNum);
        psmfInfoAddr.setValue(16, psmfPlayerVersion);

        return 0;
    }

    @HLEFunction(nid = 0x1E57A8E7, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerConfigPlayer(@CheckArgument("checkPlayerInitialized") int psmfPlayer, int configMode, int configAttr) {
    	if (psmfPlayerStatus == PSMF_PLAYER_STATUS_NONE) {
    		return ERROR_PSMFPLAYER_NOT_INITIALIZED;
    	}

    	if (configMode == PSMF_PLAYER_CONFIG_MODE_LOOP) {              // Sets if the video is looped or not.
            videoLoopStatus = configAttr;
        } else if (configMode == PSMF_PLAYER_CONFIG_MODE_PIXEL_TYPE) { // Sets the display's pixel type.
            videoPixelMode = configAttr;
        } else {
            log.warn(String.format("scePsmfPlayerConfigPlayer unknown configMode=%d, configAddr=%d", configMode, configAttr));
        }

        return 0;
    }

    @HLEFunction(nid = 0xA3D81169, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerChangePlayMode(@CheckArgument("checkPlayerInitialized") int psmfPlayer, int playMode, int playSpeed) {
        this.playMode = playMode;
        this.playSpeed = playSpeed;

        return 0;
    }

    @HLEFunction(nid = 0x68F07175, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerGetCurrentAudioStream(@CheckArgument("checkPlayerInitialized") int psmfPlayer, @CanBeNull TPointer32 audioCodecAddr, @CanBeNull TPointer32 audioStreamNumAddr) {
    	if (psmfPlayerStatus < PSMF_PLAYER_STATUS_STANDBY) {
    		return ERROR_PSMFPLAYER_NOT_INITIALIZED;
    	}

    	audioCodecAddr.setValue(audioCodec);
        audioStreamNumAddr.setValue(audioStreamNum);

        return 0;
    }

    @HLEFunction(nid = 0xF3EFAA91, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerGetCurrentPlayMode(@CheckArgument("checkPlayerInitialized") int psmfPlayer, @CanBeNull TPointer32 playModeAddr, @CanBeNull TPointer32 playSpeedAddr) {
        playModeAddr.setValue(playMode);
        playSpeedAddr.setValue(playSpeed);

        return 0;
    }

    @HLEFunction(nid = 0x3ED62233, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerGetCurrentPts(@CheckArgument("checkPlayerInitialized") int psmfPlayer, TPointer32 currentPtsAddr) {
    	if (psmfPlayerStatus < PSMF_PLAYER_STATUS_STANDBY) {
    		return ERROR_PSMFPLAYER_NOT_INITIALIZED;
    	}

        // Write our current video presentation timestamp.
        currentPtsAddr.setValue((int) psmfPlayerAvcAu.pts);

        return 0;
    }

    @HLEFunction(nid = 0x9FF2B2E7, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerGetCurrentVideoStream(@CheckArgument("checkPlayerInitialized") int psmfPlayer, @CanBeNull TPointer32 videoCodecAddr, @CanBeNull TPointer32 videoStreamNumAddr) {
    	if (psmfPlayerStatus < PSMF_PLAYER_STATUS_STANDBY) {
    		return ERROR_PSMFPLAYER_NOT_INITIALIZED;
    	}

        videoCodecAddr.setValue(videoCodec);
        videoStreamNumAddr.setValue(videoStreamNum);

        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2BEB1569, version = 150)
    public int scePsmfPlayerBreak(@CheckArgument("checkPlayerInitialized") int psmfPlayer) {
        // Can be called from interrupt.
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x76C0F4AE, version = 150)
    public int scePsmfPlayerSetPsmfOffset() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA72DB4F9, version = 150)
    public int scePsmfPlayerSetPsmfOffsetCB() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2D0E4E0A, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerSetTempBuf(int psmfPlayer) {
        return 0;
    }

    @HLEFunction(nid = 0x75F03FA2, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerSelectSpecificVideo(@CheckArgument("checkPlayerInitialized") int psmfPlayer, int videoCodec, int videoStreamNum) {
        this.videoCodec = videoCodec;
        this.videoStreamNum = videoStreamNum;

        return 0;
    }

    @HLEFunction(nid = 0x85461EFF, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerSelectSpecificAudio(@CheckArgument("checkPlayerInitialized") int psmfPlayer, int audioCodec, int audioStreamNum) {
        this.audioCodec = audioCodec;
        this.audioStreamNum = audioStreamNum;

        return 0;
    }

    @HLEFunction(nid = 0x8A9EBDCD, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerSelectVideo(@CheckArgument("checkPlayerInitialized") int psmfPlayer) {
        // Advances to the next video stream number.
        videoStreamNum++;

        return 0;
    }

    @HLEFunction(nid = 0xB8D10C56, version = 150, checkInsideInterrupt = true)
    public int scePsmfPlayerSelectAudio(@CheckArgument("checkPlayerInitialized") int psmfPlayer) {
        // Advances to the next audio stream number.
        audioStreamNum++;

        return 0;
    }
}