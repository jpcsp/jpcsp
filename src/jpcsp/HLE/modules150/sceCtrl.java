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

import java.util.LinkedList;
import java.util.List;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.HLEStartModule;

public class sceCtrl implements HLEModule, HLEStartModule {

	private int cycle;
    private int mode;
    private int uiMake;
    private int uiBreak;
    private int uiPress;
    private int uiRelease;

    private int TimeStamp; // microseconds
    private byte Lx;
    private byte Ly;
    private int Buttons;

    // IdleCancelThreshold
    private int idlereset;
    private int idleback;

    public final static int PSP_CTRL_SELECT = 0x000001;
    public final static int PSP_CTRL_START = 0x000008;
    public final static int PSP_CTRL_UP = 0x000010;
    public final static int PSP_CTRL_RIGHT = 0x000020;
    public final static int PSP_CTRL_DOWN = 0x000040;
    public final static int PSP_CTRL_LEFT = 0x000080;
    public final static int PSP_CTRL_LTRIGGER = 0x000100;
    public final static int PSP_CTRL_RTRIGGER = 0x000200;
    public final static int PSP_CTRL_TRIANGLE = 0x001000;
    public final static int PSP_CTRL_CIRCLE = 0x002000;
    public final static int PSP_CTRL_CROSS = 0x004000;
    public final static int PSP_CTRL_SQUARE = 0x008000;
    public final static int PSP_CTRL_HOME = 0x010000;
    public final static int PSP_CTRL_HOLD = 0x020000;
    public final static int PSP_CTRL_NOTE = 0x800000;
    public final static int PSP_CTRL_SCREEN = 0x400000;
    public final static int PSP_CTRL_VOLUP = 0x100000;
    public final static int PSP_CTRL_VOLDOWN = 0x200000;
    public final static int PSP_CTRL_WLAN_UP = 0x040000;
    public final static int PSP_CTRL_REMOTE = 0x080000;
    public final static int PSP_CTRL_DISC = 0x1000000;
    public final static int PSP_CTRL_MS = 0x2000000;

    // PspCtrlMode
    public final static int PSP_CTRL_MODE_DIGITAL = 0;
    public final static int PSP_CTRL_MODE_ANALOG = 1;
    protected IAction sampleAction = null;
    protected Sample samples[];
    protected int currentSamplingIndex;
    protected int currentReadingIndex;
    protected int latchSamplingCount;
    // PSP remembers the last 64 samples.
    protected final static int SAMPLE_BUFFER_SIZE = 64;
    protected List<ThreadWaitingForSampling> threadsWaitingForSampling;

    public boolean isModeDigital() {
        if (mode == PSP_CTRL_MODE_DIGITAL)
            return true;
        return false;
    }

    /** Need to call setButtons even if the user didn't move any fingers, otherwise we can't track "press" properly */
    public void setButtons(byte Lx, byte Ly, int Buttons)
    {
        int oldButtons = this.Buttons;

        this.TimeStamp = (((int)Emulator.getClock().currentTimeMillis()) * 1000) & 0x7FFFFFFF;
        this.Lx = Lx;
        this.Ly = Ly;
        this.Buttons = Buttons;

        if (isModeDigital())
        {
            // PSP_CTRL_MODE_DIGITAL
            // moving the analog stick has no effect and always returns 128,128
            this.Lx = (byte)128;
            this.Ly = (byte)128;
        }

        int changed = oldButtons ^ Buttons;

        uiMake = Buttons & changed;
        uiBreak = oldButtons & changed;
        uiPress = Buttons;
        uiRelease = (oldButtons - Buttons) & changed;
    }

    @Override
    public String getName() {
        return "sceCtrl";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.addFunction(0x6A2774F3, sceCtrlSetSamplingCycleFunction);
            mm.addFunction(0x02BAAD91, sceCtrlGetSamplingCycleFunction);
            mm.addFunction(0x1F4011E6, sceCtrlSetSamplingModeFunction);
            mm.addFunction(0xDA6B76A1, sceCtrlGetSamplingModeFunction);
            mm.addFunction(0x3A622550, sceCtrlPeekBufferPositiveFunction);
            mm.addFunction(0xC152080A, sceCtrlPeekBufferNegativeFunction);
            mm.addFunction(0x1F803938, sceCtrlReadBufferPositiveFunction);
            mm.addFunction(0x60B81F86, sceCtrlReadBufferNegativeFunction);
            mm.addFunction(0xB1D0E5CD, sceCtrlPeekLatchFunction);
            mm.addFunction(0x0B588501, sceCtrlReadLatchFunction);
            mm.addFunction(0xA7144800, sceCtrlSetIdleCancelThresholdFunction);
            mm.addFunction(0x687660FA, sceCtrlGetIdleCancelThresholdFunction);
            mm.addFunction(0x348D99D4, sceCtrl_348D99D4Function);
            mm.addFunction(0xAF5960F3, sceCtrl_AF5960F3Function);
            mm.addFunction(0xA68FD260, sceCtrlClearRapidFireFunction);
            mm.addFunction(0x6841BE1A, sceCtrlSetRapidFireFunction);

        }
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.removeFunction(sceCtrlSetSamplingCycleFunction);
            mm.removeFunction(sceCtrlGetSamplingCycleFunction);
            mm.removeFunction(sceCtrlSetSamplingModeFunction);
            mm.removeFunction(sceCtrlGetSamplingModeFunction);
            mm.removeFunction(sceCtrlPeekBufferPositiveFunction);
            mm.removeFunction(sceCtrlPeekBufferNegativeFunction);
            mm.removeFunction(sceCtrlReadBufferPositiveFunction);
            mm.removeFunction(sceCtrlReadBufferNegativeFunction);
            mm.removeFunction(sceCtrlPeekLatchFunction);
            mm.removeFunction(sceCtrlReadLatchFunction);
            mm.removeFunction(sceCtrlSetIdleCancelThresholdFunction);
            mm.removeFunction(sceCtrlGetIdleCancelThresholdFunction);
            mm.removeFunction(sceCtrl_348D99D4Function);
            mm.removeFunction(sceCtrl_AF5960F3Function);
            mm.removeFunction(sceCtrlClearRapidFireFunction);
            mm.removeFunction(sceCtrlSetRapidFireFunction);

            if (sampleAction != null) {
            	Managers.intr.removeVBlankAction(sampleAction);
            	sampleAction = null;
            }
        }
    }

    @Override
    public void start() {
    	uiMake = 0;
        uiBreak = 0;
        uiPress = 0;
        uiRelease = ~uiPress;

        Lx = (byte)128;
        Ly = (byte)128;
        Buttons = 0;

        idlereset = -1;
        idleback = -1;

        mode = PSP_CTRL_MODE_DIGITAL; // check initial mode
        cycle = 0;

        // Allocate 1 more entry because we always leave 1 entry free
        // for the internal management
        // (to differentiate a full buffer from an empty one).
        samples = new Sample[SAMPLE_BUFFER_SIZE + 1];
        for (int i = 0; i < samples.length; i++) {
        	samples[i] = new Sample();
        }
        currentSamplingIndex = 0;
        currentReadingIndex = 0;
        latchSamplingCount = 0;

        threadsWaitingForSampling = new LinkedList<ThreadWaitingForSampling>();

        if (sampleAction == null) {
        	sampleAction = new SamplingAction();
        	Managers.intr.addVBlankAction(sampleAction);
        }
    }

    @Override
    public void stop() {
    }

    protected class SamplingAction implements IAction {
		@Override
		public void execute() {
			hleCtrlExecuteSampling();
		}
    }

    protected static class ThreadWaitingForSampling {
    	SceKernelThreadInfo thread;
    	int readAddr;
    	int readCount;
    	boolean readPositive;

    	public ThreadWaitingForSampling(SceKernelThreadInfo thread, int readAddr, int readCount, boolean readPositive) {
    		this.thread = thread;
    		this.readAddr = readAddr;
    		this.readCount = readCount;
    		this.readPositive = readPositive;
    	}
    }

    protected static class Sample {
        public int TimeStamp; // microseconds
        public byte Lx;
        public byte Ly;
        public int Buttons;

        public int write(Memory mem, int addr, boolean positive) {
            mem.write32(addr    , TimeStamp);
            mem.write32(addr + 4, positive ? Buttons : ~Buttons);
            mem.write8 (addr + 8, Lx);
            mem.write8 (addr + 9, Ly);

            return addr + 16;
        }

        @Override
        public String toString() {
        	return String.format("TimeStamp=%d,Lx=%d,Ly=%d,Buttons=%08X", TimeStamp, Lx, Ly, Buttons);
        }
    }

    /**
     * Increment (or decrement) the given Sample Index
     * (currentSamplingIndex or currentReadingIndex).
     * 
     * @param index the current index value
     *              0 <= index < samples.length
     * @param count the increment (or decrement) value.
     *              -samples.length <= count <= samples.length
     * @return      the incremented index value
     *              0 <= returned value < samples.length
     */
    protected int incrementSampleIndex(int index, int count) {
    	index += count;
    	if (index >= samples.length) {
    		index -= samples.length;
    	} else if (index < 0) {
    		index += samples.length;
    	}

    	return index;
    }

    /**
     * Increment the given Sample Index by 1
     * (currentSamplingIndex or currentReadingIndex).
     * 
     * @param index the current index value
     *              0 <= index < samples.length
     * @return      the index value incremented by 1
     *              0 <= returned value < samples.length
     */
    protected int incrementSampleIndex(int index) {
    	return incrementSampleIndex(index, 1);
    }

    protected int getNumberOfAvailableSamples() {
    	int n = currentSamplingIndex - currentReadingIndex;
    	if (n < 0) {
    		n += samples.length;
    	}

    	return n;
    }

    protected void hleCtrlExecuteSampling() {
    	if (Modules.log.isDebugEnabled()) {
    		Modules.log.debug("hleCtrlExecuteSampling");
    	}

    	latchSamplingCount++;

    	Sample currentSampling = samples[currentSamplingIndex];
    	currentSampling.TimeStamp = TimeStamp;
    	currentSampling.Lx = Lx;
    	currentSampling.Ly = Ly;
    	currentSampling.Buttons = Buttons;

    	currentSamplingIndex = incrementSampleIndex(currentSamplingIndex);
    	if (currentSamplingIndex == currentReadingIndex) {
    		currentReadingIndex = incrementSampleIndex(currentReadingIndex);
    	}

    	if (!threadsWaitingForSampling.isEmpty()) {
    		ThreadWaitingForSampling wait = threadsWaitingForSampling.remove(0);
    		if (Modules.log.isDebugEnabled()) {
        		Modules.log.debug("hleExecuteSampling waiting up thread " + wait.thread);
        	}
			hleCtrlReadBufferImmediately(wait.thread.cpuContext, wait.readAddr, wait.readCount, wait.readPositive, false);
			Modules.ThreadManForUserModule.hleUnblockThread(wait.thread.uid);
    	}
    }

    protected void hleCtrlReadBufferImmediately(CpuState cpu, int addr, int count, boolean positive, boolean peek) {
    	Memory mem = Memory.getInstance();

        // If more samples are available than requested, read the more recent ones
        int available = getNumberOfAvailableSamples();
        int readIndex;
        if (available > count) {
        	readIndex = incrementSampleIndex(currentSamplingIndex, count - available);
        } else {
        	count = available;
        	readIndex = currentReadingIndex;
        }

        if (!peek) {
        	// Forget the remaining samples if they are not read now
        	currentReadingIndex = currentSamplingIndex;
        }

        for (int ctrlCount = 0; ctrlCount < count; ctrlCount++) {
        	addr = samples[readIndex].write(mem, addr, positive);
        	readIndex = incrementSampleIndex(readIndex);
        }

        if (Modules.log.isDebugEnabled()) {
    		Modules.log.debug(String.format("hleCtrlReadBufferImmediately(positive=%b, peek=%b) returning %d", positive, peek, count));
    	}

        cpu.gpr[2] = count;
    }

    protected void hleCtrlReadBuffer(int addr, int count, boolean positive) {
    	// Some data available in sample buffer?
    	if (getNumberOfAvailableSamples() > 0) {
    		// Yes, read immediately
    		hleCtrlReadBufferImmediately(Emulator.getProcessor().cpu, addr, count, positive, false);
    	} else {
    		// No, wait for next sampling
        	ThreadManForUser threadMan = Modules.ThreadManForUserModule;
    		SceKernelThreadInfo currentThread = threadMan.getCurrentThread();
    		ThreadWaitingForSampling threadWaitingForSampling = new ThreadWaitingForSampling(currentThread, addr, count, positive);
    		threadsWaitingForSampling.add(threadWaitingForSampling);
    		threadMan.hleBlockCurrentThread();

    		if (Modules.log.isDebugEnabled()) {
        		Modules.log.debug("hleCtrlReadBuffer waiting for sample");
        	}
    	}
    }

    public void sceCtrlSetSamplingCycle(Processor processor) {
        CpuState cpu = processor.cpu;

        int newCycle = cpu.gpr[4];

        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug("sceCtrlSetSamplingCycle(cycle=" + newCycle + ") returning " + cycle);
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
    		cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
        } else {
            cpu.gpr[2] = cycle;
            cycle = newCycle;
        }
    }

    public void sceCtrlGetSamplingCycle(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int cycleAddr = cpu.gpr[4];

        if (IntrManager.getInstance().isInsideInterrupt()) {
    		cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
        } else {
            mem.write32(cycleAddr, cycle);
            cpu.gpr[2] = 0;
        }
    }

    public void sceCtrlSetSamplingMode(Processor processor) {
        CpuState cpu = processor.cpu;

        int newMode = cpu.gpr[4];

        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug("sceCtrlSetSamplingMode(mode=" + newMode + ") returning " + mode);
        }

        cpu.gpr[2] = mode;
        mode = newMode;
    }

    public void sceCtrlGetSamplingMode(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int modeAddr = cpu.gpr[4];

        mem.write32(modeAddr, mode);
        cpu.gpr[2] = 0;
    }

    public void sceCtrlPeekBufferPositive(Processor processor) {
        CpuState cpu = processor.cpu;

        int data_addr = cpu.gpr[4];
        int numBuf = cpu.gpr[5];
        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug(String.format("sceCtrlPeekBufferPositive(0x%08X, %d)", data_addr, numBuf));
        }

        hleCtrlReadBufferImmediately(cpu, data_addr, numBuf, true, true);
    }

    public void sceCtrlPeekBufferNegative(Processor processor) {
        CpuState cpu = processor.cpu;

        int data_addr = cpu.gpr[4];
        int numBuf = cpu.gpr[5];
        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug(String.format("sceCtrlPeekBufferNegative(0x%08X, %d)", data_addr, numBuf));
        }

        hleCtrlReadBufferImmediately(cpu, data_addr, numBuf, false, true);
    }

    public void sceCtrlReadBufferPositive(Processor processor) {
        CpuState cpu = processor.cpu;

        int data_addr = cpu.gpr[4];
        int numBuf = cpu.gpr[5];

        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug(String.format("sceCtrlReadBufferPositive(0x%08X, %d)", data_addr, numBuf));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
        } else {
            hleCtrlReadBuffer(data_addr, numBuf, true);
        }
    }

    public void sceCtrlReadBufferNegative(Processor processor) {
        CpuState cpu = processor.cpu;

        int data_addr = cpu.gpr[4];
        int numBuf = cpu.gpr[5];

        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug(String.format("sceCtrlReadBufferNegative(0x%08X, %d)", data_addr, numBuf));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
        } else {
            hleCtrlReadBuffer(data_addr, numBuf, false);
        }
    }

    public void sceCtrlPeekLatch(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int latch_addr = cpu.gpr[4];

        mem.write32(latch_addr, uiMake);
        mem.write32(latch_addr + 4, uiBreak);
        mem.write32(latch_addr + 8, uiPress);
        mem.write32(latch_addr + 12, uiRelease);
        cpu.gpr[2] = latchSamplingCount;
    }

    public void sceCtrlReadLatch(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int latch_addr = cpu.gpr[4];

        mem.write32(latch_addr, uiMake);
        mem.write32(latch_addr + 4, uiBreak);
        mem.write32(latch_addr + 8, uiPress);
        mem.write32(latch_addr + 12, uiRelease);
        cpu.gpr[2] = latchSamplingCount;
        latchSamplingCount = 0;
    }

    public void sceCtrlSetIdleCancelThreshold(Processor processor) {
        CpuState cpu = processor.cpu;

        idlereset = cpu.gpr[4];
        idleback = cpu.gpr[5];

        Modules.log.debug("sceCtrlSetIdleCancelThreshold(idlereset=" + idlereset + ",idleback=" + idleback + ")");

        cpu.gpr[2] = 0;
    }

    public void sceCtrlGetIdleCancelThreshold(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int idlereset_addr = cpu.gpr[4];
        int idleback_addr = cpu.gpr[5];

        Modules.log.debug("sceCtrlGetIdleCancelThreshold(idlereset=0x" + Integer.toHexString(idlereset_addr)
            + ",idleback=0x" + Integer.toHexString(idleback_addr) + ")"
            + " returning idlereset=" + idlereset + " idleback=" + idleback);

        if (mem.isAddressGood(idlereset_addr)) {
            mem.write32(idlereset_addr, idlereset);
        }

        if (mem.isAddressGood(idleback_addr)) {
            mem.write32(idleback_addr, idleback);
        }

        cpu.gpr[2] = 0;
    }

    public void sceCtrl_348D99D4(Processor processor) {
        CpuState cpu = processor.cpu;

        Modules.log.warn("Unimplemented NID function sceCtrl_348D99D4 [0x348D99D4]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceCtrl_AF5960F3(Processor processor) {
        CpuState cpu = processor.cpu;

        Modules.log.warn("Unimplemented NID function sceCtrl_AF5960F3 [0xAF5960F3]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceCtrlClearRapidFire(Processor processor) {
        CpuState cpu = processor.cpu;

        Modules.log.warn("Unimplemented NID function sceCtrlClearRapidFire [0xA68FD260]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceCtrlSetRapidFire(Processor processor) {
        CpuState cpu = processor.cpu;

        Modules.log.warn("Unimplemented NID function sceCtrlSetRapidFire [0x6841BE1A]");

        cpu.gpr[2] = 0xDEADC0DE;
    }
    public final HLEModuleFunction sceCtrlSetSamplingCycleFunction = new HLEModuleFunction("sceCtrl", "sceCtrlSetSamplingCycle") {

        @Override
        public final void execute(Processor processor) {
            sceCtrlSetSamplingCycle(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceCtrlModule.sceCtrlSetSamplingCycle(processor);";
        }
    };
    public final HLEModuleFunction sceCtrlGetSamplingCycleFunction = new HLEModuleFunction("sceCtrl", "sceCtrlGetSamplingCycle") {

        @Override
        public final void execute(Processor processor) {
            sceCtrlGetSamplingCycle(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceCtrlModule.sceCtrlGetSamplingCycle(processor);";
        }
    };
    public final HLEModuleFunction sceCtrlSetSamplingModeFunction = new HLEModuleFunction("sceCtrl", "sceCtrlSetSamplingMode") {

        @Override
        public final void execute(Processor processor) {
            sceCtrlSetSamplingMode(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceCtrlModule.sceCtrlSetSamplingMode(processor);";
        }
    };
    public final HLEModuleFunction sceCtrlGetSamplingModeFunction = new HLEModuleFunction("sceCtrl", "sceCtrlGetSamplingMode") {

        @Override
        public final void execute(Processor processor) {
            sceCtrlGetSamplingMode(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceCtrlModule.sceCtrlGetSamplingMode(processor);";
        }
    };
    public final HLEModuleFunction sceCtrlPeekBufferPositiveFunction = new HLEModuleFunction("sceCtrl", "sceCtrlPeekBufferPositive") {

        @Override
        public final void execute(Processor processor) {
            sceCtrlPeekBufferPositive(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceCtrlModule.sceCtrlPeekBufferPositive(processor);";
        }
    };
    public final HLEModuleFunction sceCtrlPeekBufferNegativeFunction = new HLEModuleFunction("sceCtrl", "sceCtrlPeekBufferNegative") {

        @Override
        public final void execute(Processor processor) {
            sceCtrlPeekBufferNegative(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceCtrlModule.sceCtrlPeekBufferNegative(processor);";
        }
    };
    public final HLEModuleFunction sceCtrlReadBufferPositiveFunction = new HLEModuleFunction("sceCtrl", "sceCtrlReadBufferPositive") {

        @Override
        public final void execute(Processor processor) {
            sceCtrlReadBufferPositive(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceCtrlModule.sceCtrlReadBufferPositive(processor);";
        }
    };
    public final HLEModuleFunction sceCtrlReadBufferNegativeFunction = new HLEModuleFunction("sceCtrl", "sceCtrlReadBufferNegative") {

        @Override
        public final void execute(Processor processor) {
            sceCtrlReadBufferNegative(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceCtrlModule.sceCtrlReadBufferNegative(processor);";
        }
    };
    public final HLEModuleFunction sceCtrlPeekLatchFunction = new HLEModuleFunction("sceCtrl", "sceCtrlPeekLatch") {

        @Override
        public final void execute(Processor processor) {
            sceCtrlPeekLatch(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceCtrlModule.sceCtrlPeekLatch(processor);";
        }
    };
    public final HLEModuleFunction sceCtrlReadLatchFunction = new HLEModuleFunction("sceCtrl", "sceCtrlReadLatch") {

        @Override
        public final void execute(Processor processor) {
            sceCtrlReadLatch(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceCtrlModule.sceCtrlReadLatch(processor);";
        }
    };
    public final HLEModuleFunction sceCtrlSetIdleCancelThresholdFunction = new HLEModuleFunction("sceCtrl", "sceCtrlSetIdleCancelThreshold") {

        @Override
        public final void execute(Processor processor) {
            sceCtrlSetIdleCancelThreshold(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceCtrlModule.sceCtrlSetIdleCancelThreshold(processor);";
        }
    };
    public final HLEModuleFunction sceCtrlGetIdleCancelThresholdFunction = new HLEModuleFunction("sceCtrl", "sceCtrlGetIdleCancelThreshold") {

        @Override
        public final void execute(Processor processor) {
            sceCtrlGetIdleCancelThreshold(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceCtrlModule.sceCtrlGetIdleCancelThreshold(processor);";
        }
    };
    public final HLEModuleFunction sceCtrl_348D99D4Function = new HLEModuleFunction("sceCtrl", "sceCtrl_348D99D4") {

        @Override
        public final void execute(Processor processor) {
            sceCtrl_348D99D4(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceCtrlModule.sceCtrl_348D99D4(processor);";
        }
    };
    public final HLEModuleFunction sceCtrl_AF5960F3Function = new HLEModuleFunction("sceCtrl", "sceCtrl_AF5960F3") {

        @Override
        public final void execute(Processor processor) {
            sceCtrl_AF5960F3(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceCtrlModule.sceCtrl_AF5960F3(processor);";
        }
    };
    public final HLEModuleFunction sceCtrlClearRapidFireFunction = new HLEModuleFunction("sceCtrl", "sceCtrlClearRapidFire") {

        @Override
        public final void execute(Processor processor) {
            sceCtrlClearRapidFire(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceCtrlModule.sceCtrlClearRapidFire(processor);";
        }
    };
    public final HLEModuleFunction sceCtrlSetRapidFireFunction = new HLEModuleFunction("sceCtrl", "sceCtrlSetRapidFire") {

        @Override
        public final void execute(Processor processor) {
            sceCtrlSetRapidFire(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceCtrlModule.sceCtrlSetRapidFire(processor);";
        }
    };
}