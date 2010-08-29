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

import java.util.HashMap;

import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.HLEStartModule;

import org.apache.log4j.Logger;

public class sceMp3 implements HLEModule, HLEStartModule {

    private static Logger log = Modules.getLogger("sceMp3");

    @Override
    public String getName() {
        return "sceMp3";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.addFunction(0x07EC321A, sceMp3ReserveMp3HandleFunction);
            mm.addFunction(0x0DB149F4, sceMp3NotifyAddStreamDataFunction);
            mm.addFunction(0x2A368661, sceMp3ResetPlayPositionFunction);
            mm.addFunction(0x35750070, sceMp3InitResourceFunction);
            mm.addFunction(0x3C2FA058, sceMp3TermResourceFunction);
            mm.addFunction(0x3CEF484F, sceMp3SetLoopNumFunction);
            mm.addFunction(0x44E07129, sceMp3InitFunction);
            mm.addFunction(0x7F696782, sceMp3GetMp3ChannelNumFunction);
            mm.addFunction(0x8F450998, sceMp3GetSamplingRateFunction);
            mm.addFunction(0xA703FE0F, sceMp3GetInfoToAddStreamDataFunction);
            mm.addFunction(0xD021C0FB, sceMp3DecodeFunction);
            mm.addFunction(0xD0A56296, sceMp3CheckStreamDataNeededFunction);
            mm.addFunction(0xF5478233, sceMp3ReleaseMp3HandleFunction);
            mm.addFunction(0x354D27EA, sceMp3GetSumDecodedSampleFunction);
            mm.addFunction(0x87677E40, sceMp3GetBitRateFunction);
            mm.addFunction(0x87C263D1, sceMp3GetMaxOutputSampleFunction);
            mm.addFunction(0xD8F54A51, sceMp3GetLoopNumFunction);

        }
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.removeFunction(sceMp3ReserveMp3HandleFunction);
            mm.removeFunction(sceMp3NotifyAddStreamDataFunction);
            mm.removeFunction(sceMp3ResetPlayPositionFunction);
            mm.removeFunction(sceMp3InitResourceFunction);
            mm.removeFunction(sceMp3TermResourceFunction);
            mm.removeFunction(sceMp3SetLoopNumFunction);
            mm.removeFunction(sceMp3InitFunction);
            mm.removeFunction(sceMp3GetMp3ChannelNumFunction);
            mm.removeFunction(sceMp3GetSamplingRateFunction);
            mm.removeFunction(sceMp3GetInfoToAddStreamDataFunction);
            mm.removeFunction(sceMp3DecodeFunction);
            mm.removeFunction(sceMp3CheckStreamDataNeededFunction);
            mm.removeFunction(sceMp3ReleaseMp3HandleFunction);
            mm.removeFunction(sceMp3GetSumDecodedSampleFunction);
            mm.removeFunction(sceMp3GetBitRateFunction);
            mm.removeFunction(sceMp3GetMaxOutputSampleFunction);
            mm.removeFunction(sceMp3GetLoopNumFunction);

        }
    }

    @Override
    public void start() {
        mp3Map = new HashMap<Integer, Mp3Stream>();
    }

    @Override
    public void stop() {
    }
    protected int mp3HandleCount;
    protected HashMap<Integer, Mp3Stream> mp3Map;

    protected int makeFakeMp3StreamHandle() {
        // The stream can't be negative.
        return 0x0000A300 | (mp3HandleCount++ & 0xFFFF);
    }

    protected class Mp3Stream {

        private int mp3StreamStart;
        private int mp3Unk1;
        private int mp3StreamEnd;
        private int mp3Unk2;
        private int mp3Buf;
        private int mp3BufSize;
        private int mp3PcmBuf;
        private int mp3PcmBufSize;
        private int mp3Handle;
        private int mp3LoopNum;
        private int mp3BufCurrentPos;
        private int mp3SampleRate;
        private int mp3BitRate;
        private int mp3DecodedBytes;
        private int mp3DecodedSamples;
        private int mp3MaxSamples;
        private int mp3Channels;

        public Mp3Stream(int args) {
            Memory mem = Memory.getInstance();

            // SceMp3InitArg struct.
            mp3StreamStart = mem.read32(args);
            mp3Unk1 = mem.read32(args + 4);
            mp3StreamEnd = mem.read32(args + 8);
            mp3Unk2 = mem.read32(args + 12);
            mp3Buf = mem.read32(args + 16);
            mp3BufSize = mem.read32(args + 20);
            mp3PcmBuf = mem.read32(args + 24);
            mp3PcmBufSize = mem.read32(args + 28);

            mp3Handle = makeFakeMp3StreamHandle();
            mp3BufCurrentPos = mp3StreamStart;

            // Dummy values.
            // TODO: Parse the real values from the stream.
            mp3SampleRate = 44100;
            mp3BitRate = 128;
            mp3DecodedSamples = 0;
            mp3MaxSamples = 0;
            mp3Channels = 2;
            mp3LoopNum = 0;
            mp3DecodedBytes = 0;
        }

        public int decode() {
            // Faking.
            // Decode at 320 kbps.
            mp3DecodedBytes += 320;
            mp3DecodedSamples++;
            mp3BufCurrentPos += mp3DecodedBytes;

            return mp3DecodedBytes;
        }

        public int getMp3Handle() {
            return mp3Handle;
        }

        public int getMp3LoopNum() {
            return mp3LoopNum;
        }

        public int getMp3BufAddr() {
            return mp3Buf;
        }

        public int getMp3PcmBufAddr() {
            return mp3PcmBuf;
        }

        public int getMp3PcmBufSize() {
            return mp3PcmBufSize;
        }

        public int getMp3BufCurrentPos() {
            return mp3BufCurrentPos;
        }

        public int getMp3BufSize() {
            return mp3BufSize;
        }

        public int getMp3RemainingBytes() {
            return (mp3BufSize - mp3BufCurrentPos);
        }

        public int getMp3DecodedSamples() {
            return mp3DecodedSamples;
        }

        public int getMp3MaxSamples() {
            return mp3MaxSamples;
        }

        public int getMp3BitRate() {
            return mp3BitRate;
        }

        public int getMp3ChannelNum() {
            return mp3Channels;
        }

        public int getMp3SamplingRate() {
            return mp3SampleRate;
        }

        public int getMp3DecodedBytes() {
            return mp3DecodedBytes;
        }

        public void setMp3LoopNum(int n) {
            mp3LoopNum = n;
        }

        public void setMp3BufCurrentPos(int pos) {
            mp3BufCurrentPos = pos;
        }

        public int isStreamDataNeeded() {
            return ((mp3BufSize < mp3StreamEnd) ? 1 : 0);
        }

        public void addMp3StreamData(int size) {
            mp3BufSize += size;
        }
    }

    public void sceMp3ReserveMp3Handle(Processor processor) {
        CpuState cpu = processor.cpu;

        int mp3args = cpu.gpr[4];

        log.warn("PARTIAL: sceMp3ReserveMp3Handle " + String.format("mp3args=0x%08x", cpu.gpr[4]));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        Mp3Stream stream = new Mp3Stream(mp3args);
        int streamHandle = stream.getMp3Handle();
        mp3Map.put(streamHandle, stream);

        cpu.gpr[2] = streamHandle;
    }

    public void sceMp3NotifyAddStreamData(Processor processor) {
        CpuState cpu = processor.cpu;

        int mp3handle = cpu.gpr[4];
        int size = cpu.gpr[5];

        log.warn("PARTIAL: sceMp3NotifyAddStreamData " + String.format("mp3handle=0x%08x, size=%d", mp3handle, size));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        // New data has been written by the application.
        if (mp3Map.containsKey(mp3handle)) {
            mp3Map.get(mp3handle).addMp3StreamData(size);
        }

        cpu.gpr[2] = 0;
    }

    public void sceMp3ResetPlayPosition(Processor processor) {
        CpuState cpu = processor.cpu;

        int mp3handle = cpu.gpr[4];

        log.warn("PARTIAL: sceMp3ResetPlayPosition " + String.format("mp3handle=0x%08x", mp3handle));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (mp3Map.containsKey(mp3handle)) {
            mp3Map.get(mp3handle).setMp3BufCurrentPos(0);
        }

        cpu.gpr[2] = 0;
    }

    public void sceMp3InitResource(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("IGNORING: sceMp3InitResource");

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
    }

    public void sceMp3TermResource(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("IGNORING: sceMp3TermResource");

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
    }

    public void sceMp3SetLoopNum(Processor processor) {
        CpuState cpu = processor.cpu;

        int mp3handle = cpu.gpr[4];
        int loopNbr = cpu.gpr[5];

        log.warn("PARTIAL: sceMp3SetLoopNum " + String.format("mp3handle=0x%08x, loopNbr=%d", mp3handle, loopNbr));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (mp3Map.containsKey(mp3handle)) {
            mp3Map.get(mp3handle).setMp3LoopNum(loopNbr);
        }

        cpu.gpr[2] = 0;
    }

    public void sceMp3Init(Processor processor) {
        CpuState cpu = processor.cpu;

        int mp3handle = cpu.gpr[4];

        log.warn("IGNORING: sceMp3Init " + String.format("mp3handle=0x%08x", mp3handle));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
    }

    public void sceMp3GetMp3ChannelNum(Processor processor) {
        CpuState cpu = processor.cpu;

        int mp3handle = cpu.gpr[4];

        log.warn("PARTIAL: sceMp3GetMp3ChannelNum " + String.format("mp3handle=0x%08x", mp3handle));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        int chNum = 0;
        if (mp3Map.containsKey(mp3handle)) {
            chNum = mp3Map.get(mp3handle).getMp3ChannelNum();
        }

        cpu.gpr[2] = chNum;
    }

    public void sceMp3GetSamplingRate(Processor processor) {
        CpuState cpu = processor.cpu;

        int mp3handle = cpu.gpr[4];

        log.warn("PARTIAL: sceMp3GetSamplingRate " + String.format("mp3handle=0x%08x", mp3handle));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        int sampleRate = 0;
        if (mp3Map.containsKey(mp3handle)) {
            sampleRate = mp3Map.get(mp3handle).getMp3SamplingRate();
        }

        cpu.gpr[2] = sampleRate;
    }

    public void sceMp3GetInfoToAddStreamData(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int mp3handle = cpu.gpr[4];
        int mp3BufAddr = cpu.gpr[5];
        int mp3BufToWriteAddr = cpu.gpr[6];
        int mp3PosAddr = cpu.gpr[7];

        log.warn("PARTIAL: sceMp3GetInfoToAddStreamData " + String.format("mp3handle=0x%08x, mp3BufAddr=0x%08x, mp3BufToWriteAddr=0x%08x, mp3PosAddr=0x%08x", mp3handle, mp3BufAddr, mp3BufToWriteAddr, mp3PosAddr));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        int bufAddr = 0;
        int bufToWrite = 0;
        int bufPos = 0;
        Mp3Stream stream = null;

        if (mp3Map.containsKey(mp3handle)) {
            stream = mp3Map.get(mp3handle);
            bufAddr = stream.getMp3BufAddr();
            bufToWrite = stream.getMp3RemainingBytes();
            bufPos = stream.getMp3BufCurrentPos();
        }

        mem.write32(mp3BufAddr, bufAddr);
        mem.write32(mp3BufToWriteAddr, bufToWrite);
        mem.write32(mp3PosAddr, bufPos);

        cpu.gpr[2] = 0;
    }

    public void sceMp3Decode(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int mp3handle = cpu.gpr[4];
        int outPcmAddr = cpu.gpr[5];

        log.warn("PARTIAL: sceMp3Decode " + String.format("mp3handle=0x%08x, outPcmAddr=0x%08x", mp3handle, outPcmAddr));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        // Should decode a portion of this mp3 stream.
        // TODO: Implement real mp3 to pcm decoding.
        int pcmBytes = 0;
        Mp3Stream stream = null;

        if (mp3Map.containsKey(mp3handle)) {
            stream = mp3Map.get(mp3handle);
            pcmBytes = stream.decode();
            mem.write32(outPcmAddr, stream.getMp3PcmBufAddr());
        }

        cpu.gpr[2] = pcmBytes;
    }

    public void sceMp3CheckStreamDataNeeded(Processor processor) {
        CpuState cpu = processor.cpu;

        int mp3handle = cpu.gpr[4];

        log.warn("PARTIAL: sceMp3CheckStreamDataNeeded " + String.format("mp3handle=0x%08x", mp3handle));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        // 1 - Needs more data.
        // 0 - Doesn't need more data.
        int needsData = 0;
        if (mp3Map.containsKey(mp3handle)) {
            needsData = mp3Map.get(mp3handle).isStreamDataNeeded();
        }

        cpu.gpr[2] = needsData;
    }

    public void sceMp3ReleaseMp3Handle(Processor processor) {
        CpuState cpu = processor.cpu;

        int mp3handle = cpu.gpr[4];

        log.warn("IGNORING: sceMp3ReleaseMp3Handle " + String.format("mp3handle=0x%08x", mp3handle));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
    }

    public void sceMp3GetSumDecodedSample(Processor processor) {
        CpuState cpu = processor.cpu;

        int mp3handle = cpu.gpr[4];

        log.warn("PARTIAL: sceMp3GetSumDecodedSample " + String.format("mp3handle=0x%08x", mp3handle));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        int samples = 0;
        if (mp3Map.containsKey(mp3handle)) {
            samples = mp3Map.get(mp3handle).getMp3DecodedSamples();
        }

        cpu.gpr[2] = samples;
    }

    public void sceMp3GetBitRate(Processor processor) {
        CpuState cpu = processor.cpu;

        int mp3handle = cpu.gpr[4];

        log.warn("PARTIAL: sceMp3GetBitRate " + String.format("mp3handle=0x%08x", mp3handle));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        int bitRate = 0;
        if (mp3Map.containsKey(mp3handle)) {
            bitRate = mp3Map.get(mp3handle).getMp3BitRate();
        }

        cpu.gpr[2] = bitRate;
    }

    public void sceMp3GetMaxOutputSample(Processor processor) {
        CpuState cpu = processor.cpu;

        int mp3handle = cpu.gpr[4];

        log.warn("PARTIAL: sceMp3GetMaxOutputSample " + String.format("mp3handle=0x%08x", mp3handle));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        int maxSamples = 0;
        if (mp3Map.containsKey(mp3handle)) {
            maxSamples = mp3Map.get(mp3handle).getMp3MaxSamples();
        }

        cpu.gpr[2] = maxSamples;
    }

    public void sceMp3GetLoopNum(Processor processor) {
        CpuState cpu = processor.cpu;

        int mp3handle = cpu.gpr[4];

        log.warn("PARTIAL: sceMp3GetLoopNum " + String.format("mp3handle=0x%08x", mp3handle));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        int loopNum = 0;
        if (mp3Map.containsKey(mp3handle)) {
            loopNum = mp3Map.get(mp3handle).getMp3LoopNum();
        }

        cpu.gpr[2] = loopNum;
    }
    public final HLEModuleFunction sceMp3ReserveMp3HandleFunction = new HLEModuleFunction("sceMp3", "sceMp3ReserveMp3Handle") {

        @Override
        public final void execute(Processor processor) {
            sceMp3ReserveMp3Handle(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMp3Module.sceMp3ReserveMp3Handle(processor);";
        }
    };
    public final HLEModuleFunction sceMp3NotifyAddStreamDataFunction = new HLEModuleFunction("sceMp3", "sceMp3NotifyAddStreamData") {

        @Override
        public final void execute(Processor processor) {
            sceMp3NotifyAddStreamData(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMp3Module.sceMp3NotifyAddStreamData(processor);";
        }
    };
    public final HLEModuleFunction sceMp3ResetPlayPositionFunction = new HLEModuleFunction("sceMp3", "sceMp3ResetPlayPosition") {

        @Override
        public final void execute(Processor processor) {
            sceMp3ResetPlayPosition(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMp3Module.sceMp3ResetPlayPosition(processor);";
        }
    };
    public final HLEModuleFunction sceMp3InitResourceFunction = new HLEModuleFunction("sceMp3", "sceMp3InitResource") {

        @Override
        public final void execute(Processor processor) {
            sceMp3InitResource(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMp3Module.sceMp3InitResource(processor);";
        }
    };
    public final HLEModuleFunction sceMp3TermResourceFunction = new HLEModuleFunction("sceMp3", "sceMp3TermResource") {

        @Override
        public final void execute(Processor processor) {
            sceMp3TermResource(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMp3Module.sceMp3TermResource(processor);";
        }
    };
    public final HLEModuleFunction sceMp3SetLoopNumFunction = new HLEModuleFunction("sceMp3", "sceMp3SetLoopNum") {

        @Override
        public final void execute(Processor processor) {
            sceMp3SetLoopNum(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMp3Module.sceMp3SetLoopNum(processor);";
        }
    };
    public final HLEModuleFunction sceMp3InitFunction = new HLEModuleFunction("sceMp3", "sceMp3Init") {

        @Override
        public final void execute(Processor processor) {
            sceMp3Init(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMp3Module.sceMp3Init(processor);";
        }
    };
    public final HLEModuleFunction sceMp3GetMp3ChannelNumFunction = new HLEModuleFunction("sceMp3", "sceMp3GetMp3ChannelNum") {

        @Override
        public final void execute(Processor processor) {
            sceMp3GetMp3ChannelNum(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMp3Module.sceMp3GetMp3ChannelNum(processor);";
        }
    };
    public final HLEModuleFunction sceMp3GetSamplingRateFunction = new HLEModuleFunction("sceMp3", "sceMp3GetSamplingRate") {

        @Override
        public final void execute(Processor processor) {
            sceMp3GetSamplingRate(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMp3Module.sceMp3GetSamplingRate(processor);";
        }
    };
    public final HLEModuleFunction sceMp3GetInfoToAddStreamDataFunction = new HLEModuleFunction("sceMp3", "sceMp3GetInfoToAddStreamData") {

        @Override
        public final void execute(Processor processor) {
            sceMp3GetInfoToAddStreamData(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMp3Module.sceMp3GetInfoToAddStreamData(processor);";
        }
    };
    public final HLEModuleFunction sceMp3DecodeFunction = new HLEModuleFunction("sceMp3", "sceMp3Decode") {

        @Override
        public final void execute(Processor processor) {
            sceMp3Decode(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMp3Module.sceMp3Decode(processor);";
        }
    };
    public final HLEModuleFunction sceMp3CheckStreamDataNeededFunction = new HLEModuleFunction("sceMp3", "sceMp3CheckStreamDataNeeded") {

        @Override
        public final void execute(Processor processor) {
            sceMp3CheckStreamDataNeeded(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMp3Module.sceMp3CheckStreamDataNeeded(processor);";
        }
    };
    public final HLEModuleFunction sceMp3ReleaseMp3HandleFunction = new HLEModuleFunction("sceMp3", "sceMp3ReleaseMp3Handle") {

        @Override
        public final void execute(Processor processor) {
            sceMp3ReleaseMp3Handle(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMp3Module.sceMp3ReleaseMp3Handle(processor);";
        }
    };
    public final HLEModuleFunction sceMp3GetSumDecodedSampleFunction = new HLEModuleFunction("sceMp3", "sceMp3GetSumDecodedSample") {

        @Override
        public final void execute(Processor processor) {
            sceMp3GetSumDecodedSample(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMp3Module.sceMp3GetSumDecodedSample(processor);";
        }
    };
    public final HLEModuleFunction sceMp3GetBitRateFunction = new HLEModuleFunction("sceMp3", "sceMp3GetBitRate") {

        @Override
        public final void execute(Processor processor) {
            sceMp3GetBitRate(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMp3Module.sceMp3GetBitRate(processor);";
        }
    };
    public final HLEModuleFunction sceMp3GetMaxOutputSampleFunction = new HLEModuleFunction("sceMp3", "sceMp3GetMaxOutputSample") {

        @Override
        public final void execute(Processor processor) {
            sceMp3GetMaxOutputSample(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMp3Module.sceMp3GetMaxOutputSample(processor);";
        }
    };
    public final HLEModuleFunction sceMp3GetLoopNumFunction = new HLEModuleFunction("sceMp3", "sceMp3GetLoopNum") {

        @Override
        public final void execute(Processor processor) {
            sceMp3GetLoopNum(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMp3Module.sceMp3GetLoopNum(processor);";
        }
    };
}