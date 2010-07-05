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
import jpcsp.HLE.Modules;
import jpcsp.HLE.pspdisplay;
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

/*
 * TODO list:
 * 1. Figure out the full PSMF Player struct.
 *
 * 2. Check the meaning of the status' codes.
 *
 * 3. Resolve functions scePsmfPlayer_1E57A8E7 and scePsmfPlayer_2BEB1569's names.
 *
 * 4. Retrieve the correct playback settings (instead of default values).
 */

public class scePsmfPlayer implements HLEModule {

    @Override
    public String getName() {
        return "scePsmfPlayer";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        if (version >= 150) {
            mm.addFunction(scePsmfPlayerCreateFunction, 0x235D8787);
            mm.addFunction(scePsmfPlayerDeleteFunction, 0x9B71A274);
            mm.addFunction(scePsmfPlayerSetPsmfFunction, 0x3D6D25A9);
            mm.addFunction(scePsmfPlayerReleasePsmfFunction, 0xE792CD94);
            mm.addFunction(scePsmfPlayerStartFunction, 0x95A84EE5);
            mm.addFunction(scePsmfPlayerGetAudioOutSizeFunction, 0x3EA82A4B);
            mm.addFunction(scePsmfPlayerStopFunction, 0x1078C008);
            mm.addFunction(scePsmfPlayerUpdateFunction, 0xA0B8CA55);
            mm.addFunction(scePsmfPlayerGetVideoDataFunction, 0x46F61F8B);
            mm.addFunction(scePsmfPlayerGetAudioDataFunction, 0xB9848A74);
            mm.addFunction(scePsmfPlayerGetCurrentStatusFunction, 0xF8EF08A6);
            mm.addFunction(scePsmfPlayerGetPsmfInfoFunction, 0xDF089680);
            mm.addFunction(scePsmfPlayer_1E57A8E7Function, 0x1E57A8E7);
            mm.addFunction(scePsmfPlayer_2BEB1569Function, 0x2BEB1569);

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
            mm.removeFunction(scePsmfPlayer_1E57A8E7Function);
            mm.removeFunction(scePsmfPlayer_2BEB1569Function);

        }
    }
    // Statics.
    protected static final int psmfPlayerVideoTimestampStep = 3003;
    protected static final int psmfPlayerAudioTimestampStep = 4180;
    protected static final int psmfTimestampPerSecond = 90000;
    protected static final int pmsfMaxAheadTimestamp = 100000;
    protected static final int PSMF_PLAYER_STATUS_STOPPED = 2;
    protected static final int PSMF_PLAYER_STATUS_FINISHED = 4;

    // .PMF file vars.
    protected String pmfFilePath;
    protected byte[] pmfFileData;

    // PSMF Player status vars.
    protected Date psmfPlayerLastDate;
    protected long lastPsmfPlayerAvcSystemTime;
    protected long lastPsmfPlayerAtracSystemTime;
    protected long psmfPlayerLastTimestamp;
    protected int psmfPlayerStatus;
    protected int psmfPlayerVideoStatus;
    protected int psmfPlayerAudioStatus;
    protected int psmfPlayerAvcCurrentDecodingTimestamp;
    protected int psmfPlayerAtracCurrentDecodingTimestamp;
    protected int psmfPlayerAvcCurrentPresentationTimestamp;
    protected int psmfPlayerAtracCurrentPresentationTimestamp;

    // Playback settings.
    protected int displayBuffer;
    protected int frameWidth;
    protected int videoPixelMode;
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
        final int bytesPerPixel = pspdisplay.getPixelFormatBytes(videoPixelMode);
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
        final int bytesPerPixel = pspdisplay.getPixelFormatBytes(videoPixelMode);
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

        Modules.log.warn("PARTIAL: scePsmfPlayerCreate psmfplayer=0x" + Integer.toHexString(psmfplayer)
                + " buffer_addr=0x" + Integer.toHexString(buffer_addr));

        displayBuffer = mem.read32(buffer_addr);
        frameWidth = 512;

        audioSize = -1;  // Faking (no audio).
        videoPixelMode = pspdisplay.PSP_DISPLAY_PIXEL_FORMAT_8888;

        cpu.gpr[2] = 0;
    }

    public void scePsmfPlayerDelete(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmfplayer = cpu.gpr[4];

        Modules.log.warn("PARTIAL: scePsmfPlayerDelete psmfplayer=0x" + Integer.toHexString(psmfplayer));

        if(checkMediaEngineState()) {
            if(me != null) me.finish();
            if(pmfFileChannel != null) pmfFileChannel.flush();
        }

        psmfPlayerStatus = PSMF_PLAYER_STATUS_FINISHED;

        cpu.gpr[2] = 0;
    }

    public void scePsmfPlayerSetPsmf(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int psmfplayer = cpu.gpr[4];
        int file_addr = cpu.gpr[5];  //PMF file path.

        Modules.log.warn("PARTIAL: scePsmfPlayerSetPsmf psmfplayer=0x" + Integer.toHexString(psmfplayer)
                + " file_addr=0x" + Integer.toHexString(file_addr));

        pmfFilePath = Utilities.readStringZ(file_addr);

        //Get the file and read it to a buffer.
        try{
            SeekableDataInput psmfFile = Modules.IoFileMgrForUserModule.getFile(pmfFilePath, 0);
            pmfFileData = new byte[(int)psmfFile.length()];
            psmfFile.readFully(pmfFileData);

            Modules.log.info("'" + pmfFilePath + "' PSMF file loaded.");

            if(checkMediaEngineState()) {
                pmfFileChannel = new PacketChannel();
                pmfFileChannel.writeFile(pmfFileData);
            }
        }catch (Exception e) {
            //TODO
        }

        cpu.gpr[2] = 0;
    }


    public void scePsmfPlayerReleasePsmf(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmfplayer = cpu.gpr[4];

        Modules.log.warn("PARTIAL: scePsmfPlayerReleasePsmf psmfplayer=0x" + Integer.toHexString(psmfplayer));

        if(checkMediaEngineState()) {
            if(me != null) me.finish();
            if(pmfFileChannel != null) pmfFileChannel.flush();
        }

        psmfPlayerStatus = PSMF_PLAYER_STATUS_FINISHED;

        cpu.gpr[2] = 0;
    }

    public void scePsmfPlayerStart(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int psmfplayer = cpu.gpr[4];
        int unk = cpu.gpr[5];
        int init_status = cpu.gpr[6];

        Modules.log.warn("PARTIAL: scePsmfPlayerStart psmfplayer=0x" + Integer.toHexString(psmfplayer)
                + " unk=0x" + Integer.toHexString(unk) + " init_status=" + Integer.toHexString(init_status));

        psmfPlayerStatus = init_status;
        psmfPlayerAvcCurrentDecodingTimestamp = 0;
        psmfPlayerAtracCurrentDecodingTimestamp = 0;

        analyzePSMFLastTimestamp();

        if(checkMediaEngineState()) {
            me = new MediaEngine();
            me.init(pmfFileChannel.getFilePath());
        }

        cpu.gpr[2] = 0;
    }

    public void scePsmfPlayerGetAudioOutSize(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmfplayer = cpu.gpr[4];

        Modules.log.warn("PARTIAL: scePsmfPlayerGetAudioOutSize psmfplayer=0x" + Integer.toHexString(psmfplayer));

        cpu.gpr[2] = audioSize;
    }

    public void scePsmfPlayerStop(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmfplayer = cpu.gpr[4];

        Modules.log.warn("PARTIAL: scePsmfPlayerStop psmfplayer=0x" + Integer.toHexString(psmfplayer));

        if(checkMediaEngineState()) {
            if(me != null) me.finish();
            if(pmfFileChannel != null) pmfFileChannel.flush();
        }

        psmfPlayerStatus = PSMF_PLAYER_STATUS_FINISHED;

        cpu.gpr[2] = 0;
    }


    public void scePsmfPlayerUpdate(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmfplayer = cpu.gpr[4];

        Modules.log.warn("PARTIAL: scePsmfPlayerUpdate psmfplayer=0x" + Integer.toHexString(psmfplayer));

        // Change the current status to video or audio accordingly.
        if ((psmfPlayerAtracCurrentDecodingTimestamp > psmfPlayerAvcCurrentDecodingTimestamp + pmsfMaxAheadTimestamp)) {
            psmfPlayerStatus = psmfPlayerVideoStatus;
        } else if ((psmfPlayerAvcCurrentDecodingTimestamp > psmfPlayerAtracCurrentDecodingTimestamp + pmsfMaxAheadTimestamp)) {
            psmfPlayerStatus = psmfPlayerAudioStatus;
        }

        cpu.gpr[2] = 0;
    }


    public void scePsmfPlayerGetVideoData(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int psmfplayer = cpu.gpr[4];
        int videoDataAddr = cpu.gpr[5];

        Modules.log.warn("PARTIAL: scePsmfPlayerGetVideoData psmfplayer=0x" + Integer.toHexString(psmfplayer)
                + " videoDataAddr=0x" + Integer.toHexString(videoDataAddr));

        // Check if there's already a valid pointer at videoDataAddr.
        if(mem.isAddressGood(mem.read32(videoDataAddr + 4))) {
            frameWidth = mem.read32(videoDataAddr);
            displayBuffer = mem.read32(videoDataAddr + 4);
        } else {
            mem.write32(videoDataAddr, frameWidth);
            mem.write32(videoDataAddr + 4, displayBuffer);
        }

        // Write video data.
        if(checkMediaEngineState()) {
            if(me.getContainer() != null) {
                me.step();
                writePSMFVideoImage(displayBuffer, frameWidth);
                psmfPlayerLastTimestamp = me.getPacketTimestamp("Video", "DTS");
            }
        } else {
            generateFakePSMFVideo(displayBuffer, frameWidth);
        }

        // Update video timestamp.
        psmfPlayerAvcCurrentDecodingTimestamp += psmfPlayerVideoTimestampStep;

        // Check if we've reached the last video timestamp.
        if (psmfPlayerAvcCurrentDecodingTimestamp > psmfPlayerLastTimestamp) {
            psmfPlayerStatus = PSMF_PLAYER_STATUS_STOPPED;
        }

        cpu.gpr[2] = 0;
    }

    public void scePsmfPlayerGetAudioData(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int psmfplayer = cpu.gpr[4];
        int audioDataAddr = cpu.gpr[5];

        Modules.log.warn("PARTIAL: scePsmfPlayerGetAudioData psmfplayer=0x" + Integer.toHexString(psmfplayer)
                + " audioDataAddr=0x" + Integer.toHexString(audioDataAddr));

        // Update audio timestamp.
        psmfPlayerAtracCurrentDecodingTimestamp += psmfPlayerAudioTimestampStep;

        cpu.gpr[2] = 0;
    }

    public void scePsmfPlayerGetCurrentStatus(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmfplayer = cpu.gpr[4];

        Modules.log.warn("PARTIAL: scePsmfPlayerGetCurrentStatus psmfplayer=0x" + Integer.toHexString(psmfplayer));

        cpu.gpr[2] = psmfPlayerStatus;
    }

    public void scePsmfPlayerGetPsmfInfo(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int psmfplayer = cpu.gpr[4];
        int psmfInfoAddr = cpu.gpr[5];

        Modules.log.warn("IGNORING: scePsmfPlayerGetPsmfInfo psmfplayer=0x" + Integer.toHexString(psmfplayer)
                + " psmfInfoAddr=0x" + Integer.toHexString(psmfInfoAddr));

        cpu.gpr[2] = 0;
    }


    public void scePsmfPlayer_1E57A8E7(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int psmfplayer = cpu.gpr[4];
        int stream_type = cpu.gpr[5];
        int status = cpu.gpr[6];

        Modules.log.warn("PARTIAL: scePsmfPlayer_1E57A8E7 psmfplayer=0x" + Integer.toHexString(psmfplayer)
                + " stream_type=" + stream_type + " status=" + status);

        if(stream_type == 0) {           // Video.
            psmfPlayerVideoStatus = status;
        } else if (stream_type == 1) {   // Audio.
            psmfPlayerAudioStatus = status;
        } else {
            Modules.log.warn("scePsmfPlayer_1E57A8E7 unknown stream type.");
        }

        cpu.gpr[2] = 0;
    }

    public void scePsmfPlayer_2BEB1569(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmfplayer = cpu.gpr[4];

        Modules.log.warn("IGNORING: scePsmfPlayer_2BEB1569 psmfplayer=0x" + Integer.toHexString(psmfplayer));

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

    public final HLEModuleFunction scePsmfPlayer_1E57A8E7Function = new HLEModuleFunction("scePsmfPlayer", "scePsmfPlayer_1E57A8E7") {

        @Override
        public final void execute(Processor processor) {
            scePsmfPlayer_1E57A8E7(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfPlayer.scePsmfPlayer_1E57A8E7(processor);";
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
}