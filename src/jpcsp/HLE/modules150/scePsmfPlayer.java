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

/*
 * TODO list:
 * 1. Resolve functions scePsmfPlayer_2BEB1569 and scePsmfPlayer_58B83577's name.
 */

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
            mm.addFunction(0x2BEB1569, scePsmfPlayer_2BEB1569Function);
            mm.addFunction(0x58B83577, scePsmfPlayer_58B83577Function);

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
            mm.removeFunction(scePsmfPlayer_2BEB1569Function);
            mm.removeFunction(scePsmfPlayer_58B83577Function);

        }
    }
    // Statics.
    protected static final int psmfPlayerVideoTimestampStep = 3003;
    protected static final int psmfPlayerAudioTimestampStep = 4180;
    protected static final int psmfTimestampPerSecond = 90000;
    protected static final int pmsfMaxAheadTimestamp = 100000;
    protected static final int PSMF_PLAYER_STATUS_NONE = 0;
    protected static final int PSMF_PLAYER_STATUS_READY = 1;
    protected static final int PSMF_PLAYER_STATUS_RUNNING = 2;

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
        if(pmfFileData != null) {
            // Endian swapped inside the buffer.
            psmfPlayerLastTimestamp = ((pmfFileData[92] << 24) | (pmfFileData[93] << 16) | (pmfFileData[94] << 8) | (pmfFileData[95]));
            psmfPlayerLastDate = convertPsmfTimestampToDate(psmfPlayerLastTimestamp);
        }
    }

    public void scePsmfPlayerCreate(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int psmfplayer = cpu.gpr[4];
        int buffer_addr = cpu.gpr[5];

        log.warn("PARTIAL: scePsmfPlayerCreate psmfplayer=0x" + Integer.toHexString(psmfplayer) + " buffer_addr=0x" + Integer.toHexString(buffer_addr));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
        } else {
            displayBuffer = mem.read32(buffer_addr);
            frameWidth = 512;
            audioSize = 2048;
            videoPixelMode = sceDisplay.PSP_DISPLAY_PIXEL_FORMAT_8888;

            psmfPlayerStatus = PSMF_PLAYER_STATUS_READY;

            cpu.gpr[2] = 0;
        }
    }

    public void scePsmfPlayerDelete(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmfplayer = cpu.gpr[4];

        log.warn("PARTIAL: scePsmfPlayerDelete psmfplayer=0x" + Integer.toHexString(psmfplayer));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
        } else {
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
    }

    public void scePsmfPlayerSetPsmf(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmfplayer = cpu.gpr[4];
        int file_addr = cpu.gpr[5];  // PMF file path.

        log.warn("PARTIAL: scePsmfPlayerSetPsmf psmfplayer=0x" + Integer.toHexString(psmfplayer) + " file_addr=0x" + Integer.toHexString(file_addr));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
        } else {
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

            psmfPlayerStatus = PSMF_PLAYER_STATUS_RUNNING;

            cpu.gpr[2] = 0;
        }
    }

    public void scePsmfPlayerReleasePsmf(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmfplayer = cpu.gpr[4];

        log.warn("PARTIAL: scePsmfPlayerReleasePsmf psmfplayer=0x" + Integer.toHexString(psmfplayer));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
        } else {
            if (checkMediaEngineState()) {
                if (me != null) {
                    me.finish();
                }
                if (pmfFileChannel != null) {
                    pmfFileChannel.flush();
                }
            }

            psmfPlayerStatus = PSMF_PLAYER_STATUS_READY;

            cpu.gpr[2] = 0;
        }
    }

    public void scePsmfPlayerStart(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmfplayer = cpu.gpr[4];
        int unk = cpu.gpr[5];
        int init_status = cpu.gpr[6];

        log.warn("PARTIAL: scePsmfPlayerStart psmfplayer=0x" + Integer.toHexString(psmfplayer) + " unk=0x" + Integer.toHexString(unk) + " init_status=" + Integer.toHexString(init_status));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
        } else {
            psmfPlayerStatus = init_status;
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
    }

    public void scePsmfPlayerGetAudioOutSize(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmfplayer = cpu.gpr[4];

        log.warn("PARTIAL: scePsmfPlayerGetAudioOutSize psmfplayer=0x" + Integer.toHexString(psmfplayer));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
        } else {
            cpu.gpr[2] = audioSize;
        }
    }

    public void scePsmfPlayerStop(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmfplayer = cpu.gpr[4];

        log.warn("PARTIAL: scePsmfPlayerStop psmfplayer=0x" + Integer.toHexString(psmfplayer));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
        } else {
            if (checkMediaEngineState()) {
                if (me != null) {
                    me.finish();
                }
                if (pmfFileChannel != null) {
                    pmfFileChannel.flush();
                }
            }

            psmfPlayerStatus = PSMF_PLAYER_STATUS_RUNNING;

            cpu.gpr[2] = 0;
        }
    }

    public void scePsmfPlayerUpdate(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmfplayer = cpu.gpr[4];

        log.warn("PARTIAL: scePsmfPlayerUpdate psmfplayer=0x" + Integer.toHexString(psmfplayer));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
        } else {
            cpu.gpr[2] = 0;
        }
    }

    public void scePsmfPlayerGetVideoData(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int psmfplayer = cpu.gpr[4];
        int videoDataAddr = cpu.gpr[5];

        log.warn("PARTIAL: scePsmfPlayerGetVideoData psmfplayer=0x" + Integer.toHexString(psmfplayer) + " videoDataAddr=0x" + Integer.toHexString(videoDataAddr));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
        } else {
            // Check if there's already a valid pointer at videoDataAddr.
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

            // Check if we've reached the last timestamp.
            if (psmfPlayerAvcCurrentDecodingTimestamp > psmfPlayerLastTimestamp) {
                psmfPlayerStatus = PSMF_PLAYER_STATUS_READY;
                cpu.gpr[2] = 0x8061600c;  // No more data (actual name unknown).
            } else {
                cpu.gpr[2] = 0;
            }
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
        } else {
            // Update audio timestamp.
            psmfPlayerAtracCurrentDecodingTimestamp += psmfPlayerAudioTimestampStep;

            cpu.gpr[2] = 0;
        }
    }

    public void scePsmfPlayerGetCurrentStatus(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmfplayer = cpu.gpr[4];

        log.warn("PARTIAL: scePsmfPlayerGetCurrentStatus psmfplayer=0x" + Integer.toHexString(psmfplayer));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
        } else {
            cpu.gpr[2] = psmfPlayerStatus;
        }
    }

    public void scePsmfPlayerGetPsmfInfo(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int psmfplayer = cpu.gpr[4];
        int psmfInfoAddr = cpu.gpr[5];

        log.warn("IGNORING: scePsmfPlayerGetPsmfInfo psmfplayer=0x" + Integer.toHexString(psmfplayer) + " psmfInfoAddr=0x" + Integer.toHexString(psmfInfoAddr));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
        } else {
            cpu.gpr[2] = 0;
        }
    }

    public void scePsmfPlayerConfigPlayer(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int psmfplayer = cpu.gpr[4];
        int stream_type = cpu.gpr[5];
        int setting = cpu.gpr[6];

        log.warn("PARTIAL: scePsmfPlayerConfigPlayer psmfplayer=0x" + Integer.toHexString(psmfplayer) + " stream_type=" + stream_type + " setting=" + setting);

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
        } else {
            if (stream_type == 1) {           // Video.
                videoPixelMode = setting;
            } else if (stream_type == 0) {   // Audio.
                audioChannelMode = setting;
            } else {
                log.warn("scePsmfPlayerConfigPlayer unknown stream type.");
            }

            cpu.gpr[2] = 0;
        }
    }

    public void scePsmfPlayer_2BEB1569(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmfplayer = cpu.gpr[4];

        log.warn("IGNORING: scePsmfPlayer_2BEB1569 psmfplayer=0x" + Integer.toHexString(psmfplayer));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
        } else {
            cpu.gpr[2] = 0;
        }
    }

    public void scePsmfPlayer_58B83577(Processor processor) {
        log.warn("scePsmfPlayer_58B83577 redirecting to scePsmfPlayerSetPsmf");
        scePsmfPlayerSetPsmf(processor);
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

    public final HLEModuleFunction scePsmfPlayer_2BEB1569Function = new HLEModuleFunction("scePsmfPlayer", "scePsmfPlayer_2BEB1569") {

        @Override
        public final void execute(Processor processor) {
            scePsmfPlayer_2BEB1569(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfPlayer.scePsmfPlayer_2BEB1569(processor);";
        }
    };

    public final HLEModuleFunction scePsmfPlayer_58B83577Function = new HLEModuleFunction("scePsmfPlayer", "scePsmfPlayer_58B83577") {

        @Override
        public final void execute(Processor processor) {
            scePsmfPlayer_58B83577(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfPlayer.scePsmfPlayer_58B83577(processor);";
        }
    };
}