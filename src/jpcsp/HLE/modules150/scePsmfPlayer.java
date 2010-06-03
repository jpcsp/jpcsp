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

import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.Modules;
import jpcsp.HLE.pspiofilemgr;
import jpcsp.media.MediaEngine;
import jpcsp.media.PacketChannel;

import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.filesystems.SeekableDataInput;
import jpcsp.util.Utilities;

import jpcsp.Allegrex.CpuState;

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
            mm.addFunction(scePsmfPlayer_3EA82A4BFunction, 0x3EA82A4B);
            mm.addFunction(scePsmfPlayerStopFunction, 0x1078C008);
            mm.addFunction(scePsmfPlayerUpdateFunction, 0xA0B8CA55);
            mm.addFunction(scePsmfPlayer_46F61F8BFunction, 0x46F61F8B);
            mm.addFunction(scePsmfPlayer_B9848A74Function, 0xB9848A74);
            mm.addFunction(scePsmfPlayer_F8EF08A6Function, 0xF8EF08A6);
            mm.addFunction(scePsmfPlayer_DF089680Function, 0xDF089680);
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
            mm.removeFunction(scePsmfPlayer_3EA82A4BFunction);
            mm.removeFunction(scePsmfPlayerStopFunction);
            mm.removeFunction(scePsmfPlayerUpdateFunction);
            mm.removeFunction(scePsmfPlayer_46F61F8BFunction);
            mm.removeFunction(scePsmfPlayer_B9848A74Function);
            mm.removeFunction(scePsmfPlayer_F8EF08A6Function);
            mm.removeFunction(scePsmfPlayer_DF089680Function);
            mm.removeFunction(scePsmfPlayer_1E57A8E7Function);
            mm.removeFunction(scePsmfPlayer_2BEB1569Function);

        }
    }

    protected String pmfFilePath;
    protected byte[] pmfFileData;
    protected PacketChannel pmfFileChannel;
    protected MediaEngine me;

    protected int currentStream;
    protected int streamCount;

    private boolean checkMediaEngineState() {
        return sceMpeg.isEnableMediaEngine();
    }

    public void scePsmfPlayerCreate(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int psmf = cpu.gpr[4];
        int mpeg = cpu.gpr[5];

        Modules.log.warn("PARTIAL: scePsmfPlayerCreate psmf=" + Integer.toHexString(psmf)
                + " mpeg=" + Integer.toHexString(mpeg));

        currentStream = 0;
        streamCount = 0;

        cpu.gpr[2] = 0;
    }

    public void scePsmfPlayerDelete(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmf = cpu.gpr[4];

        if(checkMediaEngineState()) {
            me.finish();
            pmfFileChannel.flush();
        }

        Modules.log.warn("IGNORING: scePsmfPlayerDelete psmf=" + Integer.toHexString(psmf));

        cpu.gpr[2] = 0;
    }

    public void scePsmfPlayerSetPsmf(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int psmf = cpu.gpr[4];
        int file_addr = cpu.gpr[5];  //PMF file path.

        pmfFilePath = Utilities.readStringZ(file_addr);
        pspiofilemgr fileManager = pspiofilemgr.getInstance();
        pmfFileChannel = new PacketChannel();
        //Get the file and read it to a buffer.
        try{
            SeekableDataInput psmfFile = fileManager.getFile(pmfFilePath, 0);
            pmfFileData = new byte[(int)psmfFile.length()];
            psmfFile.readFully(pmfFileData);
            pmfFileChannel.writeFile(pmfFileData);
        }catch (Exception e) {
            //TODO
        }

        Modules.log.warn("PARTIAL: scePsmfPlayerSetPsmf psmf=" + Integer.toHexString(psmf)
                + " file=" + pmfFilePath);

        cpu.gpr[2] = 0;
    }


    public void scePsmfPlayerReleasePsmf(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmf = cpu.gpr[4];

        Modules.log.warn("IGNORING: scePsmfPlayerReleasePsmf psmf=" + Integer.toHexString(psmf));

        cpu.gpr[2] = 0;
    }

    public void scePsmfPlayerStart(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int psmf = cpu.gpr[4];
        int unk1 = cpu.gpr[5];  // Another output address?
        int unk2 = cpu.gpr[6];  // MPEG stream to be used?

        Modules.log.warn("PARTIAL: scePsmfPlayerStart psmf=" + Integer.toHexString(psmf)
                + " unk1=" + Integer.toHexString(unk1) + " unk2=" + Integer.toHexString(unk2));

        if(checkMediaEngineState()) {
            me = new MediaEngine();
            me.decodeAndPlay(pmfFileChannel.getFilePath());
        }

        cpu.gpr[2] = 0;
    }

    public void scePsmfPlayer_3EA82A4B(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmf = cpu.gpr[4];

        // Seems to check a portion of the PSMF struct.
        // v0 can be any value.

        Modules.log.warn("IGNORING: scePsmfPlayer_3EA82A4B psmf=" + Integer.toHexString(psmf));

        cpu.gpr[2] = 0;
    }

    public void scePsmfPlayerStop(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmf = cpu.gpr[4];

        Modules.log.warn("IGNORING: scePsmfPlayerStop psmf=" + Integer.toHexString(psmf));

        cpu.gpr[2] = 0;
    }


    public void scePsmfPlayerUpdate(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmf = cpu.gpr[4];

        Modules.log.warn("IGNORING: scePsmfPlayerUpdate psmf=" + Integer.toHexString(psmf));

        cpu.gpr[2] = 0;
    }


    public void scePsmfPlayer_46F61F8B(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int psmf = cpu.gpr[4];
        int unk = cpu.gpr[5];

        Modules.log.warn("IGNORING: scePsmfPlayer_46F61F8B psmf=" + Integer.toHexString(psmf)
                + " unk=" + Integer.toHexString(unk));

        cpu.gpr[2] = 0;
    }

    public void scePsmfPlayer_B9848A74(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int psmf = cpu.gpr[4];
        int unk = cpu.gpr[5];

        Modules.log.warn("IGNORING: scePsmfPlayer_B9848A74 psmf=" + Integer.toHexString(psmf)
                + " unk=" + Integer.toHexString(unk));

        cpu.gpr[2] = 0;
    }

    public void scePsmfPlayer_F8EF08A6(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmf = cpu.gpr[4];

        Modules.log.warn("PARTIAL: scePsmfPlayer_F8EF08A6 psmf=" + Integer.toHexString(psmf));

        // Returns the current number of streams.

        cpu.gpr[2] = streamCount;
    }

    public void scePsmfPlayer_DF089680(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmf = cpu.gpr[4];
        int unk = cpu.gpr[5];

        Modules.log.warn("IGNORING: scePsmfPlayer_DF089680 psmf=" + Integer.toHexString(psmf)
                + " unk=" + Integer.toHexString(unk));

        cpu.gpr[2] = 0;
    }


    public void scePsmfPlayer_1E57A8E7(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int psmf = cpu.gpr[4];
        int stream_type = cpu.gpr[5];
        int ch = cpu.gpr[6];

        Modules.log.warn("PARTIAL: scePsmfPlayer_1E57A8E7 psmf=" + Integer.toHexString(psmf)
                + " stream_type=" + stream_type + " ch=" + ch);

        streamCount++;
        currentStream = ch;

        cpu.gpr[2] = 0;
    }

    public void scePsmfPlayer_2BEB1569(Processor processor) {
        CpuState cpu = processor.cpu;

        int psmf = cpu.gpr[4];

        Modules.log.warn("IGNORING: scePsmfPlayer_2BEB1569 psmf=" + Integer.toHexString(psmf));

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

    public final HLEModuleFunction scePsmfPlayer_3EA82A4BFunction = new HLEModuleFunction("scePsmfPlayer", "scePsmfPlayer_3EA82A4B") {

        @Override
        public final void execute(Processor processor) {
            scePsmfPlayer_3EA82A4B(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfPlayer.scePsmfPlayer_3EA82A4B(processor);";
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

    public final HLEModuleFunction scePsmfPlayer_46F61F8BFunction = new HLEModuleFunction("scePsmfPlayer", "scePsmfPlayer_46F61F8B") {

        @Override
        public final void execute(Processor processor) {
            scePsmfPlayer_46F61F8B(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfPlayer.scePsmfPlayer_46F61F8B(processor);";
        }
    };

    public final HLEModuleFunction scePsmfPlayer_B9848A74Function = new HLEModuleFunction("scePsmfPlayer", "scePsmfPlayer_B9848A74") {

        @Override
        public final void execute(Processor processor) {
            scePsmfPlayer_B9848A74(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfPlayer.scePsmfPlayer_B9848A74(processor);";
        }
    };

    public final HLEModuleFunction scePsmfPlayer_F8EF08A6Function = new HLEModuleFunction("scePsmfPlayer", "scePsmfPlayer_F8EF08A6") {

        @Override
        public final void execute(Processor processor) {
            scePsmfPlayer_F8EF08A6(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfPlayer.scePsmfPlayer_F8EF08A6(processor);";
        }
    };

    public final HLEModuleFunction scePsmfPlayer_DF089680Function = new HLEModuleFunction("scePsmfPlayer", "scePsmfPlayer_DF089680") {

        @Override
        public final void execute(Processor processor) {
            scePsmfPlayer_DF089680(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePsmfPlayer.scePsmfPlayer_DF089680(processor);";
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