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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.TimeZone;

import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.filesystems.SeekableDataInput;
import jpcsp.media.MediaEngine;
import jpcsp.media.PacketChannel;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryWriter;
import jpcsp.util.Debug;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class scePsmfPlayer implements HLEModule {

    private static Logger log = Modules.getLogger("scePsmfPlayer");

    @Override
    public String getName() {
        return "scePsmfPlayer";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.addFunction(0x235D8787, scePsmfPlayerCreateFunction);
            mm.addFunction(0x9B71A274, scePsmfPlayerDeleteFunction);
            mm.addFunction(0x3D6D25A9, scePsmfPlayerSetPsmfFunction);
            mm.addFunction(0xE792CD94, scePsmfPlayerReleasePsmfFunction);
            mm.addFunction(0x95A84EE5, scePsmfPlayerStartFunction);
            mm.addFunction(0x3EA82A4B, scePsmfPlayerGetAudioOutSizeFunction);
            mm.addFunction(0x1078C008, scePsmfPlayerStopFunction);
            mm.addFunction(0xA0B8CA55, scePsmfPlayerUpdateFunction);
            mm.addFunction(0x46F61F8B, scePsmfPlayerGetVideoDataFunction);
            mm.addFunction(0xB9848A74, scePsmfPlayerGetAudioDataFunction);
            mm.addFunction(0xF8EF08A6, scePsmfPlayerGetCurrentStatusFunction);
            mm.addFunction(0xDF089680, scePsmfPlayerGetPsmfInfoFunction);
            mm.addFunction(0x1E57A8E7, scePsmfPlayerConfigPlayerFunction);
            mm.addFunction(0xA3D81169, scePsmfPlayerChangePlayModeFunction);
            mm.addFunction(0x68F07175, scePsmfPlayerGetCurrentAudioStreamFunction);
            mm.addFunction(0xF3EFAA91, scePsmfPlayerGetCurrentPlayModeFunction);
            mm.addFunction(0x3ED62233, scePsmfPlayerGetCurrentPtsFunction);
            mm.addFunction(0x9FF2B2E7, scePsmfPlayerGetCurrentVideoStreamFunction);
            mm.addFunction(0x2BEB1569, scePsmfPlayerSetTempBufFunction);
            mm.addFunction(0x58B83577, scePsmfPlayerSetPsmfCBFunction);
            mm.addFunction(0x76C0F4AE, scePsmfPlayerSetPsmfOffsetFunction);
            mm.addFunction(0xA72DB4F9, scePsmfPlayerSetPsmfOffset_A72DB4F9Function);
            mm.addFunction(0x2D0E4E0A, scePsmfPlayerSetPsmfOffset_2D0E4E0AFunction);
            mm.addFunction(0x75F03FA2, scePsmfPlayerSetPsmfOffset_75F03FA2Function);
            mm.addFunction(0x85461EFF, scePsmfPlayerSetPsmfOffset_85461EFFFunction);
            mm.addFunction(0x8A9EBDCD, scePsmfPlayerSetPsmfOffset_8A9EBDCDFunction);
            mm.addFunction(0xB8D10C56, scePsmfPlayerSetPsmfOffset_B8D10C56Function);

        }
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.removeFunction(scePsmfPlayerCreateFunction);
            mm.removeFunction(scePsmfPlayerDeleteFunction);
            mm.removeFunction(scePsmfPlayerSetPsmfFunction);
            mm.removeFunction(scePsmfPlayerReleasePsmfFunction);
            mm.removeFunction(scePsmfPlayerStartFunction);
            mm.removeFunction(scePsmfPlayerGetAudioOutSizeFunction);
            mm.removeFunction(scePsmfPlayerStopFunction);
            mm.removeFunction(scePsmfPlayerUpdateFunction);
            mm.removeFunction(scePsmfPlayerGetVideoDataFunction);
            mm.removeFunction(scePsmfPlayerGetAudioDataFunction);
            mm.removeFunction(scePsmfPlayerGetCurrentStatusFunction);
            mm.removeFunction(scePsmfPlayerGetPsmfInfoFunction);
            mm.removeFunction(scePsmfPlayerConfigPlayerFunction);
            mm.removeFunction(scePsmfPlayerChangePlayModeFunction);
            mm.removeFunction(scePsmfPlayerGetCurrentAudioStreamFunction);
            mm.removeFunction(scePsmfPlayerGetCurrentPlayModeFunction);
            mm.removeFunction(scePsmfPlayerGetCurrentPtsFunction);
            mm.removeFunction(scePsmfPlayerGetCurrentVideoStreamFunction);
            mm.removeFunction(scePsmfPlayerSetTempBufFunction);
            mm.removeFunction(scePsmfPlayerSetPsmfCBFunction);
            mm.removeFunction(scePsmfPlayerSetPsmfOffsetFunction);
            mm.removeFunction(scePsmfPlayerSetPsmfOffsetCBFunction);
            mm.removeFunction(scePsmfPlayerSetPsmfOffset_A72DB4F9Function);
            mm.removeFunction(scePsmfPlayerSetPsmfOffset_2D0E4E0AFunction);
            mm.removeFunction(scePsmfPlayerSetPsmfOffset_75F03FA2Function);
            mm.removeFunction(scePsmfPlayerSetPsmfOffset_85461EFFFunction);
            mm.removeFunction(scePsmfPlayerSetPsmfOffset_8A9EBDCDFunction);
            mm.removeFunction(scePsmfPlayerSetPsmfOffset_B8D10C56Function);

        }
    }
    // Statics.
    protected static final int psmfPlayerVideoTimestampStep = 3003;
    protected static final int psmfPlayerAudioTimestampStep = 4180;
    protected static final int psmfTimestampPerSecond = 90000;
    protected static final int psmfMaxAheadTimestamp = 100000;
    protected static final int psmfInitFlag = 0xE;

    protected static final int PSMF_PLAYER_STATUS_NONE = 0;
    protected static final int PSMF_PLAYER_STATUS_INIT = 1;
    protected static final int PSMF_PLAYER_STATUS_VISIBLE = 2;
    protected static final int PSMF_PLAYER_STATUS_QUIT = 3;
    protected static final int PSMF_PLAYER_STATUS_FINISHED = 3;
    protected static final int PSMF_PLAYER_STATUS_VIDEO_FINISHED = 512;
    protected static final int PSMF_PLAYER_STATUS_AUDIO_FINISHED = 256;

    // .PMF file vars.
    protected String pmfFilePath;
    protected byte[] pmfFileData;

    // PSMF Player status vars.
    protected Date psmfPlayerLastDate;
    protected long lastPsmfPlayerAvcSystemTime;
    protected long lastPsmfPlayerAtracSystemTime;
    protected long psmfPlayerLastTimestamp;
    protected int psmfPlayerStatus;
    protected int psmfPlayerAvcCurrentDecodingTimestamp;
    protected int psmfPlayerAtracCurrentDecodingTimestamp;
    protected int psmfPlayerAvcCurrentPresentationTimestamp;
    protected int psmfPlayerAtracCurrentPresentationTimestamp;

    // Playback settings.
    protected int displayBuffer;
    protected int unk1;
    protected int unk2;
    protected int frameWidth;
    protected int videoPixelMode;
    protected int audioChannelMode;
    protected int audioSize;

    // Media Engine vars.
    protected PacketChannel pmfFileChannel;
    protected MediaEngine me;

    private boolean checkMediaEngineState() {
        return sceMpeg.isEnableMediaEngine();
    }

    protected Date convertPsmfTimestampToDate(long timestamp) {
        long millis = timestamp / (psmfTimestampPerSecond / 1000);
        return new Date(millis);
    }

    private void writePSMFVideoImage(int dest_addr, int frameWidth) {
        final int bytesPerPixel = sceDisplay.getPixelFormatBytes(videoPixelMode);
        int width = Math.min(480, frameWidth);
        int height = 272;

        // Get the current generated image, convert it to pixels and write it
        // to memory.
        if (me != null && me.getCurrentImg() != null) {
            // Override the base dimensions with the image's real dimensions.
            width = me.getCurrentImg().getWidth();
            height = me.getCurrentImg().getHeight();

            for (int y = 0; y < height; y++) {
                int address = dest_addr + y * frameWidth * bytesPerPixel;
                IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(address, bytesPerPixel);

                for (int x = 0; x < width; x++) {
                    int colorARGB = me.getCurrentImg().getRGB(x, y);
                    // Convert from ARGB to ABGR.
                    int a = (colorARGB >>> 24) & 0xFF;
                    int r = (colorARGB >>> 16) & 0xFF;
                    int g = (colorARGB >>> 8) & 0xFF;
                    int b = colorARGB & 0xFF;
                    int colorABGR = a << 24 | b << 16 | g << 8 | r;

                    int pixelColor = Debug.getPixelColor(colorABGR, videoPixelMode);
                    memoryWriter.writeNext(pixelColor);
                }
            }
        }
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

        Date currentDate = convertPsmfTimestampToDate(psmfPlayerAvcCurrentDecodingTimestamp);
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        Debug.printFramebuffer(dest_addr, frameWidth, 10, 250, 0xFFFFFFFF, 0xFF000000, videoPixelMode, 1, " This is a faked PSMF Player video. ");

        String displayedString;
        if (psmfPlayerLastDate != null) {
            displayedString = String.format(" %s / %s ", dateFormat.format(currentDate), dateFormat.format(psmfPlayerLastDate));
            Debug.printFramebuffer(dest_addr, frameWidth, 10, 10, 0xFFFFFFFF, 0xFF000000, videoPixelMode, 2, displayedString);
        }
    }

    protected void analyzePSMFLastTimestamp() {
        if (pmfFileData != null) {
            // Endian swapped inside the buffer.
            psmfPlayerLastTimestamp = ((pmfFileData[92] << 24) | (pmfFileData[93] << 16) | (pmfFileData[94] << 8) | (pmfFileData[95]));
            psmfPlayerLastDate = convertPsmfTimestampToDate(psmfPlayerLastTimestamp);
        }
    }

    public void scePsmfPlayerCreate(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int psmfplayer = cpu.gpr[4];
        int psmfDataAddr = cpu.gpr[5];

        log.warn("PARTIAL: scePsmfPlayerCreate psmfplayer=0x" + Integer.toHexString(psmfplayer)
                + " psmfDataAddr=0x" + Integer.toHexString(psmfDataAddr));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        // The psmfDataAddr contains three fields that are manually set before
        // scePsmfPlayerCreate is called.
        displayBuffer = mem.read32(psmfDataAddr); // The buffer allocated for scePsmf, which is ported into scePsmfPlayer.
        unk1 = mem.read32(psmfDataAddr + 4);      // Always 0x300000.
        unk2 = mem.read32(psmfDataAddr + 8);      // Variable integer value.
        log.info("PSMF Data: displayBuffer=0x" + Integer.toHexString(displayBuffer)
                + ", unk1=0x" + Integer.toHexString(unk1)
                + ", unk2=" + unk2);

        // Set default values.
        frameWidth = 512;
        audioSize = 2048;
        videoPixelMode = sceDisplay.PSP_DISPLAY_PIXEL_FORMAT_8888;
        audioChannelMode = 0;

        // Start with INIT.
        psmfPlayerStatus = PSMF_PLAYER_STATUS_INIT;

        cpu.gpr[2] = 0;
    }

    public void scePsmfPlayerDelete(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmfplayer = cpu.gpr[4];

        log.warn("PARTIAL: scePsmfPlayerDelete psmfplayer=0x" + Integer.toHexString(psmfplayer));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (checkMediaEngineState()) {
            if (me != null) {
                me.finish();
            }
            if (pmfFileChannel != null) {
                pmfFileChannel.flush();
            }
        }

        cpu.gpr[2] = 0;
    }

    public void scePsmfPlayerSetPsmf(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmfplayer = cpu.gpr[4];
        int file_addr = cpu.gpr[5];  // PMF file path.

        log.warn("PARTIAL: scePsmfPlayerSetPsmf psmfplayer=0x" + Integer.toHexString(psmfplayer) + " file_addr=0x" + Integer.toHexString(file_addr));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        pmfFilePath = Utilities.readStringZ(file_addr);

        //Get the file and read it to a buffer.
        try {
            SeekableDataInput psmfFile = Modules.IoFileMgrForUserModule.getFile(pmfFilePath, 0);
            pmfFileData = new byte[(int) psmfFile.length()];
            psmfFile.readFully(pmfFileData);

            log.info("'" + pmfFilePath + "' PSMF file loaded.");

            if (checkMediaEngineState()) {
                pmfFileChannel = new PacketChannel();
                pmfFileChannel.writeFile(pmfFileData);
            }
        } catch (Exception e) {
            //TODO
        }

        // Switch to VISIBLE.
        psmfPlayerStatus = PSMF_PLAYER_STATUS_VISIBLE;

        cpu.gpr[2] = 0;
    }

    public void scePsmfPlayerReleasePsmf(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmfplayer = cpu.gpr[4];

        log.warn("PARTIAL: scePsmfPlayerReleasePsmf psmfplayer=0x" + Integer.toHexString(psmfplayer));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (checkMediaEngineState()) {
            if (me != null) {
                me.finish();
            }
            if (pmfFileChannel != null) {
                pmfFileChannel.flush();
            }
        }

        // Go back to INIT, because some applications recognize that another file can be
        // loaded after scePsmfPlayerReleasePsmf has been called.
        psmfPlayerStatus = PSMF_PLAYER_STATUS_INIT;

        cpu.gpr[2] = 0;
    }

    public void scePsmfPlayerStart(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int psmfplayer = cpu.gpr[4];
        int initFlagAddr = cpu.gpr[5];
        int initStatus = cpu.gpr[6];

        log.warn("PARTIAL: scePsmfPlayerStart psmfplayer=0x" + Integer.toHexString(psmfplayer)
                + " initFlagAddr=0x" + Integer.toHexString(initFlagAddr)
                + " initStatus=" + Integer.toHexString(initStatus));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if(mem.isAddressGood(initFlagAddr)) {
            boolean isPsmfInit = (mem.read32(initFlagAddr) == psmfInitFlag) ? true : false;

            if(!isPsmfInit) {
                log.warn("scePsmfPlayerStart is using an uninitialized PSMF (no 0xE flag found)!");
            }
        }
        psmfPlayerStatus = initStatus;
        psmfPlayerAvcCurrentDecodingTimestamp = 0;
        psmfPlayerAtracCurrentDecodingTimestamp = 0;

        analyzePSMFLastTimestamp();

        if (checkMediaEngineState()) {
            if (pmfFileChannel != null) {
                me = new MediaEngine();
                me.init(pmfFileChannel.getFilePath());
            }
        }

        cpu.gpr[2] = 0;
    }

    public void scePsmfPlayerGetAudioOutSize(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmfplayer = cpu.gpr[4];

        log.warn("PARTIAL: scePsmfPlayerGetAudioOutSize psmfplayer=0x" + Integer.toHexString(psmfplayer));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = audioSize;
    }

    public void scePsmfPlayerStop(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmfplayer = cpu.gpr[4];

        log.warn("PARTIAL: scePsmfPlayerStop psmfplayer=0x" + Integer.toHexString(psmfplayer));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (checkMediaEngineState()) {
            if (me != null) {
                me.finish();
            }
            if (pmfFileChannel != null) {
                pmfFileChannel.flush();
            }
        }

        // Always switch to VISIBLE, because this PSMF can still be resumed.
        psmfPlayerStatus = PSMF_PLAYER_STATUS_VISIBLE;

        cpu.gpr[2] = 0;
    }

    public void scePsmfPlayerUpdate(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmfplayer = cpu.gpr[4];

        log.warn("PARTIAL: scePsmfPlayerUpdate psmfplayer=0x" + Integer.toHexString(psmfplayer));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        // Update video presentation timestamp.
        psmfPlayerAvcCurrentPresentationTimestamp = psmfPlayerAvcCurrentDecodingTimestamp;
        cpu.gpr[2] = 0;
    }

    public void scePsmfPlayerGetVideoData(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int psmfplayer = cpu.gpr[4];
        int videoDataAddr = cpu.gpr[5];

        log.warn("PARTIAL: scePsmfPlayerGetVideoData psmfplayer=0x" + Integer.toHexString(psmfplayer) + " videoDataAddr=0x" + Integer.toHexString(videoDataAddr));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        // Check if there's already a valid pointer at videoDataAddr.
        // If not, use the displayBuffer from scePsmfPlayerCreate.
        if (mem.isAddressGood(mem.read32(videoDataAddr + 4))) {
            frameWidth = mem.read32(videoDataAddr);
            displayBuffer = mem.read32(videoDataAddr + 4);
        } else {
            mem.write32(videoDataAddr, frameWidth);
            mem.write32(videoDataAddr + 4, displayBuffer);
        }

        // Write video data.
        if (checkMediaEngineState()) {
            if (me != null) {
                if (me.getContainer() != null) {
                    me.step();
                    writePSMFVideoImage(displayBuffer, frameWidth);
                    psmfPlayerLastTimestamp = me.getPacketTimestamp("Video", "DTS");
                }
            }
        } else {
            generateFakePSMFVideo(displayBuffer, frameWidth);
        }

        // Update video timestamp.
        psmfPlayerAvcCurrentDecodingTimestamp += psmfPlayerVideoTimestampStep;

        // Check playback status.
        if (psmfPlayerAvcCurrentDecodingTimestamp > psmfPlayerLastTimestamp) {
            // If we've reached the last timestamp, change the status to VIDEO_FINISHED.
            psmfPlayerStatus = PSMF_PLAYER_STATUS_VIDEO_FINISHED;
            cpu.gpr[2] = 0;
        } else if (psmfPlayerAvcCurrentDecodingTimestamp > psmfPlayerAtracCurrentDecodingTimestamp + psmfMaxAheadTimestamp) {
            // If we're ahead of audio, switch to VISIBLE and return an error.
            psmfPlayerStatus = PSMF_PLAYER_STATUS_VISIBLE;
            cpu.gpr[2] = 0x8061600c;  // No more data (actual name unknown).
        } else {
            cpu.gpr[2] = 0;
        }
    }

    public void scePsmfPlayerGetAudioData(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int psmfplayer = cpu.gpr[4];
        int audioDataAddr = cpu.gpr[5];

        log.warn("PARTIAL: scePsmfPlayerGetAudioData psmfplayer=0x" + Integer.toHexString(psmfplayer) + " audioDataAddr=0x" + Integer.toHexString(audioDataAddr));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        // Update audio timestamp.
        psmfPlayerAtracCurrentDecodingTimestamp += psmfPlayerAudioTimestampStep;

        // Check playback status.
        if (psmfPlayerAtracCurrentDecodingTimestamp > psmfPlayerLastTimestamp) {
            // If we've reached the last timestamp, change the status to AUDIO_FINISHED.
            psmfPlayerStatus = PSMF_PLAYER_STATUS_AUDIO_FINISHED;
            cpu.gpr[2] = 0;
        } else if (psmfPlayerAtracCurrentDecodingTimestamp > psmfPlayerAvcCurrentDecodingTimestamp + psmfMaxAheadTimestamp) {
            // If we're ahead of video, switch to VISIBLE and return an error.
            psmfPlayerStatus = PSMF_PLAYER_STATUS_VISIBLE;
            cpu.gpr[2] = 0x8061600c;  // No more data (actual name unknown).
        } else {
            cpu.gpr[2] = 0;
        }
    }

    public void scePsmfPlayerGetCurrentStatus(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmfplayer = cpu.gpr[4];

        log.warn("PARTIAL: scePsmfPlayerGetCurrentStatus psmfplayer=0x" + Integer.toHexString(psmfplayer));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = psmfPlayerStatus;
    }

    public void scePsmfPlayerGetPsmfInfo(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int psmfplayer = cpu.gpr[4];
        int psmfInfoAddr = cpu.gpr[5];

        log.warn("IGNORING: scePsmfPlayerGetPsmfInfo psmfplayer=0x" + Integer.toHexString(psmfplayer) + " psmfInfoAddr=0x" + Integer.toHexString(psmfInfoAddr));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
    }

    public void scePsmfPlayerConfigPlayer(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int psmfplayer = cpu.gpr[4];
        int streamType = cpu.gpr[5];
        int playMode = cpu.gpr[6];

        log.warn("PARTIAL: scePsmfPlayerConfigPlayer psmfplayer=0x" + Integer.toHexString(psmfplayer)
                + " streamType=" + streamType + " playMode=" + playMode);

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (streamType == 1) {           // Video: sets the pixel mode.
            videoPixelMode = playMode;
        } else if (streamType == 0) {    // Audio: sets the channel mode.
            audioChannelMode = playMode;
        } else {
            log.warn("scePsmfPlayerConfigPlayer unknown stream type.");
        }
        cpu.gpr[2] = 0;
    }

    public void scePsmfPlayerChangePlayMode(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmfplayer = cpu.gpr[4];
        int playMode = cpu.gpr[5];
        int streamType = cpu.gpr[6];

        log.warn("PARTIAL: scePsmfPlayerChangePlayMode psmfplayer=0x" + Integer.toHexString(psmfplayer)
                + ", playMode=" + playMode + ", streamType=" + streamType);

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (streamType == 1) {           // Video.
            videoPixelMode = playMode;
        } else if (streamType == 0) {    // Audio.
            audioChannelMode = playMode;
        } else {
            log.warn("scePsmfPlayerChangePlayMode unknown stream type.");
        }
        cpu.gpr[2] = 0;
    }

    public void scePsmfPlayerGetCurrentAudioStream(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmfplayer = cpu.gpr[4];

        log.warn("UNIMPLEMENTED: scePsmfPlayerGetCurrentAudioStream psmfplayer=0x" + Integer.toHexString(psmfplayer));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
    }

    public void scePsmfPlayerGetCurrentPlayMode(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int psmfplayer = cpu.gpr[4];
        int videoPlayModeAddr = cpu.gpr[5];
        int audioPlayModeAddr = cpu.gpr[6];

        log.warn("PARTIAL: scePsmfPlayerGetCurrentPlayMode psmfplayer=0x" + Integer.toHexString(psmfplayer)
                + ", videoPlayModeAddr=0x" + Integer.toHexString(videoPlayModeAddr)
                + ", audioPlayModeAddr=0x" + Integer.toHexString(audioPlayModeAddr));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if(mem.isAddressGood(videoPlayModeAddr)) {
            mem.write32(videoPlayModeAddr, videoPixelMode);
        }
        if(mem.isAddressGood(audioPlayModeAddr)) {
            mem.write32(audioPlayModeAddr, audioChannelMode);
        }
        cpu.gpr[2] = 0;
    }

    public void scePsmfPlayerGetCurrentPts(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmfplayer = cpu.gpr[4];

        log.warn("PARTIAL: scePsmfPlayerGetCurrentPts psmfplayer=0x" + Integer.toHexString(psmfplayer));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        // Return our current video presentation timestamp.
        cpu.gpr[2] = psmfPlayerAvcCurrentPresentationTimestamp;
    }

    public void scePsmfPlayerGetCurrentVideoStream(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmfplayer = cpu.gpr[4];

        log.warn("UNIMPLEMENTED: scePsmfPlayerGetCurrentVideoStream psmfplayer=0x" + Integer.toHexString(psmfplayer));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
    }

    public void scePsmfPlayerSetTempBuf(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmfplayer = cpu.gpr[4];

        log.warn("IGNORING: scePsmfPlayerSetTempBuf psmfplayer=0x" + Integer.toHexString(psmfplayer));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
    }

    public void scePsmfPlayerSetPsmfCB(Processor processor) {
        log.warn("scePsmfPlayerSetPsmfCB redirecting to scePsmfPlayerSetPsmf");
        scePsmfPlayerSetPsmf(processor);
        Modules.ThreadManForUserModule.hleRescheduleCurrentThread(true);
    }

    public void scePsmfPlayerSetPsmfOffset(Processor processor) {
        log.warn("scePsmfPlayerSetPsmfOffset redirecting to scePsmfPlayerSetPsmf");
        scePsmfPlayerSetPsmf(processor);
    }

    public void scePsmfPlayerSetPsmfOffsetCB(Processor processor) {
        log.warn("scePsmfPlayerSetPsmfOffsetCB redirecting to scePsmfPlayerSetPsmf");
        scePsmfPlayerSetPsmf(processor);
        Modules.ThreadManForUserModule.hleRescheduleCurrentThread(true);
    }

    public void scePsmfPlayerSetPsmfOffset_A72DB4F9(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmfplayer = cpu.gpr[4];

        log.warn("IGNORING: scePsmfPlayerSetPsmfOffset_A72DB4F9 psmfplayer=0x" + Integer.toHexString(psmfplayer));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
    }

    public void scePsmfPlayerSetPsmfOffset_2D0E4E0A(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmfplayer = cpu.gpr[4];

        log.warn("IGNORING: scePsmfPlayerSetPsmfOffset_2D0E4E0A psmfplayer=0x" + Integer.toHexString(psmfplayer));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
    }

    public void scePsmfPlayerSetPsmfOffset_75F03FA2(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmfplayer = cpu.gpr[4];

        log.warn("IGNORING: scePsmfPlayerSetPsmfOffset_75F03FA2 psmfplayer=0x" + Integer.toHexString(psmfplayer));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
    }

    public void scePsmfPlayerSetPsmfOffset_85461EFF(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmfplayer = cpu.gpr[4];

        log.warn("IGNORING: scePsmfPlayerSetPsmfOffset_85461EFF psmfplayer=0x" + Integer.toHexString(psmfplayer));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
    }

    public void scePsmfPlayerSetPsmfOffset_8A9EBDCD(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmfplayer = cpu.gpr[4];

        log.warn("IGNORING: scePsmfPlayerSetPsmfOffset_8A9EBDCD psmfplayer=0x" + Integer.toHexString(psmfplayer));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
    }

    public void scePsmfPlayerSetPsmfOffset_B8D10C56(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmfplayer = cpu.gpr[4];

        log.warn("IGNORING: scePsmfPlayerSetPsmfOffset_B8D10C56 psmfplayer=0x" + Integer.toHexString(psmfplayer));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
    }

    public final HLEModuleFunction scePsmfPlayerCreateFunction = new HLEModuleFunction("scePsmfPlayer", "scePsmfPlayerCreate") {

        @Override
        public final void execute(Processor processor) {
            scePsmfPlayerCreate(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfPlayer.scePsmfPlayerCreate(processor);";
        }
    };
    public final HLEModuleFunction scePsmfPlayerDeleteFunction = new HLEModuleFunction("scePsmfPlayer", "scePsmfPlayerDelete") {

        @Override
        public final void execute(Processor processor) {
            scePsmfPlayerDelete(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfPlayer.scePsmfPlayerDelete(processor);";
        }
    };
    public final HLEModuleFunction scePsmfPlayerSetPsmfFunction = new HLEModuleFunction("scePsmfPlayer", "scePsmfPlayerSetPsmf") {

        @Override
        public final void execute(Processor processor) {
            scePsmfPlayerSetPsmf(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfPlayer.scePsmfPlayerSetPsmf(processor);";
        }
    };
    public final HLEModuleFunction scePsmfPlayerReleasePsmfFunction = new HLEModuleFunction("scePsmfPlayer", "scePsmfPlayerReleasePsmf") {

        @Override
        public final void execute(Processor processor) {
            scePsmfPlayerReleasePsmf(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfPlayer.scePsmfPlayerReleasePsmf(processor);";
        }
    };
    public final HLEModuleFunction scePsmfPlayerStartFunction = new HLEModuleFunction("scePsmfPlayer", "scePsmfPlayerStart") {

        @Override
        public final void execute(Processor processor) {
            scePsmfPlayerStart(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfPlayer.scePsmfPlayerStart(processor);";
        }
    };
    public final HLEModuleFunction scePsmfPlayerGetAudioOutSizeFunction = new HLEModuleFunction("scePsmfPlayer", "scePsmfPlayerGetAudioOutSize") {

        @Override
        public final void execute(Processor processor) {
            scePsmfPlayerGetAudioOutSize(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfPlayer.scePsmfPlayerGetAudioOutSize(processor);";
        }
    };
    public final HLEModuleFunction scePsmfPlayerStopFunction = new HLEModuleFunction("scePsmfPlayer", "scePsmfPlayerStop") {

        @Override
        public final void execute(Processor processor) {
            scePsmfPlayerStop(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfPlayer.scePsmfPlayerStop(processor);";
        }
    };
    public final HLEModuleFunction scePsmfPlayerUpdateFunction = new HLEModuleFunction("scePsmfPlayer", "scePsmfPlayerUpdate") {

        @Override
        public final void execute(Processor processor) {
            scePsmfPlayerUpdate(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfPlayer.scePsmfPlayerUpdate(processor);";
        }
    };
    public final HLEModuleFunction scePsmfPlayerGetVideoDataFunction = new HLEModuleFunction("scePsmfPlayer", "scePsmfPlayerGetVideoData") {

        @Override
        public final void execute(Processor processor) {
            scePsmfPlayerGetVideoData(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfPlayer.scePsmfPlayerGetVideoData(processor);";
        }
    };
    public final HLEModuleFunction scePsmfPlayerGetAudioDataFunction = new HLEModuleFunction("scePsmfPlayer", "scePsmfPlayerGetAudioData") {

        @Override
        public final void execute(Processor processor) {
            scePsmfPlayerGetAudioData(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfPlayer.scePsmfPlayerGetAudioData(processor);";
        }
    };
    public final HLEModuleFunction scePsmfPlayerGetCurrentStatusFunction = new HLEModuleFunction("scePsmfPlayer", "scePsmfPlayerGetCurrentStatus") {

        @Override
        public final void execute(Processor processor) {
            scePsmfPlayerGetCurrentStatus(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfPlayer.scePsmfPlayerGetCurrentStatus(processor);";
        }
    };
    public final HLEModuleFunction scePsmfPlayerGetPsmfInfoFunction = new HLEModuleFunction("scePsmfPlayer", "scePsmfPlayerGetPsmfInfo") {

        @Override
        public final void execute(Processor processor) {
            scePsmfPlayerGetPsmfInfo(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfPlayer.scePsmfPlayerGetPsmfInfo(processor);";
        }
    };
    public final HLEModuleFunction scePsmfPlayerConfigPlayerFunction = new HLEModuleFunction("scePsmfPlayer", "scePsmfPlayerConfigPlayer") {

        @Override
        public final void execute(Processor processor) {
            scePsmfPlayerConfigPlayer(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfPlayerConfigPlayer(processor);";
        }
    };
    public final HLEModuleFunction scePsmfPlayerChangePlayModeFunction = new HLEModuleFunction("scePsmfPlayer", "scePsmfPlayerChangePlayMode") {

        @Override
        public final void execute(Processor processor) {
            scePsmfPlayerChangePlayMode(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfPlayerChangePlayMode(processor);";
        }
    };
    public final HLEModuleFunction scePsmfPlayerGetCurrentAudioStreamFunction = new HLEModuleFunction("scePsmfPlayer", "scePsmfPlayerGetCurrentAudioStream") {

        @Override
        public final void execute(Processor processor) {
            scePsmfPlayerGetCurrentAudioStream(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfPlayerGetCurrentAudioStream(processor);";
        }
    };
    public final HLEModuleFunction scePsmfPlayerGetCurrentPlayModeFunction = new HLEModuleFunction("scePsmfPlayer", "scePsmfPlayerGetCurrentPlayMode") {

        @Override
        public final void execute(Processor processor) {
            scePsmfPlayerGetCurrentPlayMode(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfPlayerGetCurrentPlayMode(processor);";
        }
    };
    public final HLEModuleFunction scePsmfPlayerGetCurrentPtsFunction = new HLEModuleFunction("scePsmfPlayer", "scePsmfPlayerGetCurrentPts") {

        @Override
        public final void execute(Processor processor) {
            scePsmfPlayerGetCurrentPts(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfPlayerGetCurrentPts(processor);";
        }
    };
    public final HLEModuleFunction scePsmfPlayerGetCurrentVideoStreamFunction = new HLEModuleFunction("scePsmfPlayer", "scePsmfPlayerGetCurrentVideoStream") {

        @Override
        public final void execute(Processor processor) {
            scePsmfPlayerGetCurrentVideoStream(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfPlayerGetCurrentVideoStream(processor);";
        }
    };
    public final HLEModuleFunction scePsmfPlayerSetTempBufFunction = new HLEModuleFunction("scePsmfPlayer", "scePsmfPlayerSetTempBuf") {

        @Override
        public final void execute(Processor processor) {
            scePsmfPlayerSetTempBuf(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfPlayer.scePsmfPlayerSetTempBuf(processor);";
        }
    };
    public final HLEModuleFunction scePsmfPlayerSetPsmfCBFunction = new HLEModuleFunction("scePsmfPlayer", "scePsmfPlayerSetPsmfCB") {

        @Override
        public final void execute(Processor processor) {
            scePsmfPlayerSetPsmfCB(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfPlayer.scePsmfPlayerSetPsmfCB(processor);";
        }
    };
    public final HLEModuleFunction scePsmfPlayerSetPsmfOffsetFunction = new HLEModuleFunction("scePsmfPlayer", "scePsmfPlayerSetPsmfOffset") {

        @Override
        public final void execute(Processor processor) {
            scePsmfPlayerSetPsmfOffset(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfPlayer.scePsmfPlayerSetPsmfOffset(processor);";
        }
    };
    public final HLEModuleFunction scePsmfPlayerSetPsmfOffsetCBFunction = new HLEModuleFunction("scePsmfPlayer", "scePsmfPlayerSetPsmfOffsetCB") {

        @Override
        public final void execute(Processor processor) {
            scePsmfPlayerSetPsmfOffsetCB(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfPlayer.scePsmfPlayerSetPsmfOffsetCB(processor);";
        }
    };
    public final HLEModuleFunction scePsmfPlayerSetPsmfOffset_A72DB4F9Function = new HLEModuleFunction("scePsmfPlayer", "scePsmfPlayerSetPsmfOffset_A72DB4F9") {

        @Override
        public final void execute(Processor processor) {
            scePsmfPlayerSetPsmfOffset_A72DB4F9(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfPlayer.scePsmfPlayerSetPsmfOffset_A72DB4F9(processor);";
        }
    };
    public final HLEModuleFunction scePsmfPlayerSetPsmfOffset_2D0E4E0AFunction = new HLEModuleFunction("scePsmfPlayer", "scePsmfPlayerSetPsmfOffset_2D0E4E0A") {

        @Override
        public final void execute(Processor processor) {
            scePsmfPlayerSetPsmfOffset_2D0E4E0A(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfPlayer.scePsmfPlayerSetPsmfOffset_2D0E4E0A(processor);";
        }
    };
    public final HLEModuleFunction scePsmfPlayerSetPsmfOffset_75F03FA2Function = new HLEModuleFunction("scePsmfPlayer", "scePsmfPlayerSetPsmfOffset_75F03FA2") {

        @Override
        public final void execute(Processor processor) {
            scePsmfPlayerSetPsmfOffset_75F03FA2(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfPlayer.scePsmfPlayerSetPsmfOffset_75F03FA2(processor);";
        }
    };
    public final HLEModuleFunction scePsmfPlayerSetPsmfOffset_85461EFFFunction = new HLEModuleFunction("scePsmfPlayer", "scePsmfPlayerSetPsmfOffset_85461EFF") {

        @Override
        public final void execute(Processor processor) {
            scePsmfPlayerSetPsmfOffset_85461EFF(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfPlayer.scePsmfPlayerSetPsmfOffset_85461EFF(processor);";
        }
    };
    public final HLEModuleFunction scePsmfPlayerSetPsmfOffset_8A9EBDCDFunction = new HLEModuleFunction("scePsmfPlayer", "scePsmfPlayerSetPsmfOffset_8A9EBDCD") {

        @Override
        public final void execute(Processor processor) {
            scePsmfPlayerSetPsmfOffset_8A9EBDCD(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfPlayer.scePsmfPlayerSetPsmfOffset_8A9EBDCD(processor);";
        }
    };
    public final HLEModuleFunction scePsmfPlayerSetPsmfOffset_B8D10C56Function = new HLEModuleFunction("scePsmfPlayer", "scePsmfPlayerSetPsmfOffset_B8D10C56") {

        @Override
        public final void execute(Processor processor) {
            scePsmfPlayerSetPsmfOffset_B8D10C56(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfPlayer.scePsmfPlayerSetPsmfOffset_B8D10C56(processor);";
        }
    };
}