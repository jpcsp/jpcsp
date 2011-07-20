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

import java.security.MessageDigest;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Random;

import jpcsp.Clock;
import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.State;
import jpcsp.Allegrex.CpuState;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.SystemTimeManager;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.HLEStartModule;

import org.apache.log4j.Logger;

/*
 * TODO list:
 * 1. Improve sceKernelLibcGettimeofday() result.
 *  -> Info:
 *       struct timeval {
 *           time_t tv_sec; // seconds since Jan. 1, 1970
 *           suseconds_t tv_usec; // and microseconds
 *       };
 *
 *     struct timezone {
 *          int tz_minuteswest; // of Greenwich
 *           int tz_dsttime; // type of dst correction to apply
 *      };
 */
public class UtilsForUser implements HLEModule, HLEStartModule {
    private static Logger log = Modules.getLogger("UtilsForUser");

	private HashMap<Integer, SceKernelUtilsMt19937Context> Mt19937List;
    private SceKernelUtilsMd5Context md5Ctx;
    private SceKernelUtilsSha1Context sha1Ctx;

    private static class SceKernelUtilsMt19937Context {
        private Random r;

        public SceKernelUtilsMt19937Context(int seed) {
            r = new Random(seed);
        }
    }

    private static class SceKernelUtilsMd5Context {
        // Context vars.
        private int part1;
        private int part2;
        private int part3;
        private int part4;
        private int padding;
        private short tmpBytesRemaining;
        private short tmpBytesCalculated;
        private long fullDataSize;
        private byte[] buf;

        // Internal vars.
        private byte[] input;

        public SceKernelUtilsMd5Context() {
            part1 = 0;
            part2 = 0;
            part3 = 0;
            part4 = 0;
            padding = 0;
            tmpBytesRemaining = 0;
            tmpBytesCalculated = 0;
            fullDataSize = 0;
            buf = new byte[64];
        }

        public void init (int ctx_addr) {
            Memory mem = Memory.getInstance();
            mem.write32(ctx_addr, part1);
            mem.write32(ctx_addr + 4, part2);
            mem.write32(ctx_addr + 8, part3);
            mem.write32(ctx_addr + 12, part4);
            mem.write32(ctx_addr + 16, padding);
            mem.write16(ctx_addr + 20, tmpBytesRemaining);
            mem.write16(ctx_addr + 22, tmpBytesCalculated);
            mem.write64(ctx_addr + 24, fullDataSize);
            for (int i = 0; i < 64; i++) {
                mem.write8(ctx_addr + 32 + i, buf[i]);
            }
        }

        public void update (int ctx_addr, int data_addr, int data_size) {
            Memory mem = Memory.getInstance();
            input = new byte[data_size];
            if (Memory.isAddressGood(data_addr)) {
                for (int i = 0; i < data_size; i++) {
                    input[i] = (byte) mem.read8(data_addr + i);
                }
            }
        }

        public void result (int ctx_addr, int result_addr) {
            Memory mem = Memory.getInstance();
            byte[] hash = null;
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                hash = md.digest(input);
            } catch (Exception e) {
                // Ignore...
            }
            if ((hash != null) && Memory.isAddressGood(result_addr)) {
                for (int i = 0; i < 16; i++) {
                    mem.write8(result_addr + i, hash[i]);
                }
            }
        }
    }

    private static class SceKernelUtilsSha1Context {
        // Context vars.
        private int part1;
        private int part2;
        private int part3;
        private int part4;
        private int part5;
        private short tmpBytesRemaining;
        private short tmpBytesCalculated;
        private long fullDataSize;
        private byte[] buf;

        // Internal vars.
        private byte[] input;

        public SceKernelUtilsSha1Context() {
            part1 = 0;
            part2 = 0;
            part3 = 0;
            part4 = 0;
            part5 = 0;
            tmpBytesRemaining = 0;
            tmpBytesCalculated = 0;
            fullDataSize = 0;
            buf = new byte[64];
        }

        public void init (int ctx_addr) {
            Memory mem = Memory.getInstance();
            mem.write32(ctx_addr, part1);
            mem.write32(ctx_addr + 4, part2);
            mem.write32(ctx_addr + 8, part3);
            mem.write32(ctx_addr + 12, part4);
            mem.write32(ctx_addr + 16, part5);
            mem.write16(ctx_addr + 20, tmpBytesRemaining);
            mem.write16(ctx_addr + 22, tmpBytesCalculated);
            mem.write64(ctx_addr + 24, fullDataSize);
            for (int i = 0; i < 64; i++) {
                mem.write8(ctx_addr + 32 + i, buf[i]);
            }
        }

        public void update (int ctx_addr, int data_addr, int data_size) {
            Memory mem = Memory.getInstance();
            input = new byte[data_size];
            if (Memory.isAddressGood(data_addr)) {
                for (int i = 0; i < data_size; i++) {
                    input[i] = (byte) mem.read8(data_addr + i);
                }
            }
        }

        public void result (int ctx_addr, int result_addr) {
            Memory mem = Memory.getInstance();
            byte[] hash = null;
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-1");
                hash = md.digest(input);
            } catch (Exception e) {
                // Ignore...
            }
            if ((hash != null) && Memory.isAddressGood(result_addr)) {
                for (int i = 0; i < 16; i++) {
                    mem.write8(result_addr + i, hash[i]);
                }
            }
        }
    }

	@Override
	public String getName() { return "UtilsForUser"; }

	@Override
	public void installModule(HLEModuleManager mm, int version) {
		if (version >= 150) {

			mm.addFunction(0xBFA98062, sceKernelDcacheInvalidateRangeFunction);
			mm.addFunction(0xC2DF770E, sceKernelIcacheInvalidateRangeFunction);
			mm.addFunction(0xC8186A58, sceKernelUtilsMd5DigestFunction);
			mm.addFunction(0x9E5C5086, sceKernelUtilsMd5BlockInitFunction);
			mm.addFunction(0x61E1E525, sceKernelUtilsMd5BlockUpdateFunction);
			mm.addFunction(0xB8D24E78, sceKernelUtilsMd5BlockResultFunction);
			mm.addFunction(0x840259F1, sceKernelUtilsSha1DigestFunction);
			mm.addFunction(0xF8FCD5BA, sceKernelUtilsSha1BlockInitFunction);
			mm.addFunction(0x346F6DA8, sceKernelUtilsSha1BlockUpdateFunction);
			mm.addFunction(0x585F1C09, sceKernelUtilsSha1BlockResultFunction);
			mm.addFunction(0xE860E75E, sceKernelUtilsMt19937InitFunction);
			mm.addFunction(0x06FB8A63, sceKernelUtilsMt19937UIntFunction);
			mm.addFunction(0x37FB5C42, sceKernelGetGPIFunction);
			mm.addFunction(0x6AD345D7, sceKernelSetGPOFunction);
			mm.addFunction(0x91E4F6A7, sceKernelLibcClockFunction);
			mm.addFunction(0x27CC57F0, sceKernelLibcTimeFunction);
			mm.addFunction(0x71EC4271, sceKernelLibcGettimeofdayFunction);
			mm.addFunction(0x79D1C3FA, sceKernelDcacheWritebackAllFunction);
			mm.addFunction(0xB435DEC5, sceKernelDcacheWritebackInvalidateAllFunction);
			mm.addFunction(0x3EE30821, sceKernelDcacheWritebackRangeFunction);
			mm.addFunction(0x34B9FA9E, sceKernelDcacheWritebackInvalidateRangeFunction);
			mm.addFunction(0x80001C4C, sceKernelDcacheProbeFunction);
			mm.addFunction(0x16641D70, sceKernelDcacheReadTagFunction);
			mm.addFunction(0x920F104A, sceKernelIcacheInvalidateAllFunction);
			mm.addFunction(0x4FD31C9D, sceKernelIcacheProbeFunction);
			mm.addFunction(0xFB05FAD0, sceKernelIcacheReadTagFunction);

		}
	}

	@Override
	public void uninstallModule(HLEModuleManager mm, int version) {
		if (version >= 150) {

			mm.removeFunction(sceKernelDcacheInvalidateRangeFunction);
			mm.removeFunction(sceKernelIcacheInvalidateRangeFunction);
			mm.removeFunction(sceKernelUtilsMd5DigestFunction);
			mm.removeFunction(sceKernelUtilsMd5BlockInitFunction);
			mm.removeFunction(sceKernelUtilsMd5BlockUpdateFunction);
			mm.removeFunction(sceKernelUtilsMd5BlockResultFunction);
			mm.removeFunction(sceKernelUtilsSha1DigestFunction);
			mm.removeFunction(sceKernelUtilsSha1BlockInitFunction);
			mm.removeFunction(sceKernelUtilsSha1BlockUpdateFunction);
			mm.removeFunction(sceKernelUtilsSha1BlockResultFunction);
			mm.removeFunction(sceKernelUtilsMt19937InitFunction);
			mm.removeFunction(sceKernelUtilsMt19937UIntFunction);
			mm.removeFunction(sceKernelGetGPIFunction);
			mm.removeFunction(sceKernelSetGPOFunction);
			mm.removeFunction(sceKernelLibcClockFunction);
			mm.removeFunction(sceKernelLibcTimeFunction);
			mm.removeFunction(sceKernelLibcGettimeofdayFunction);
			mm.removeFunction(sceKernelDcacheWritebackAllFunction);
			mm.removeFunction(sceKernelDcacheWritebackInvalidateAllFunction);
			mm.removeFunction(sceKernelDcacheWritebackRangeFunction);
			mm.removeFunction(sceKernelDcacheWritebackInvalidateRangeFunction);
			mm.removeFunction(sceKernelDcacheProbeFunction);
			mm.removeFunction(sceKernelDcacheReadTagFunction);
			mm.removeFunction(sceKernelIcacheInvalidateAllFunction);
			mm.removeFunction(sceKernelIcacheProbeFunction);
			mm.removeFunction(sceKernelIcacheReadTagFunction);

		}
	}

	@Override
	public void start() {
        Mt19937List = new HashMap<Integer, SceKernelUtilsMt19937Context>();
    }

	@Override
	public void stop() {
	}

    protected static final int PSP_KERNEL_ICACHE_PROBE_MISS = 0;
    protected static final int PSP_KERNEL_ICACHE_PROBE_HIT = 1;
    protected static final int PSP_KERNEL_DCACHE_PROBE_MISS = 0;
    protected static final int PSP_KERNEL_DCACHE_PROBE_HIT = 1;
    protected static final int PSP_KERNEL_DCACHE_PROBE_HIT_DIRTY = 2;

	public void sceKernelDcacheInvalidateRange(Processor processor) {
		CpuState cpu = processor.cpu;

		int addr = cpu.gpr[4];
		int size = cpu.gpr[5];

		if (log.isTraceEnabled()) {
			log.trace(String.format("IGNORING: sceKernelDcacheInvalidateRange addr=0x%08X, size=%d", addr, size));
		}

        cpu.gpr[2] = 0;
	}

	public void sceKernelIcacheInvalidateRange(Processor processor) {
        CpuState cpu = processor.cpu;

		int addr = cpu.gpr[4];
		int size = cpu.gpr[5];

		if (log.isInfoEnabled()) {
			log.info(String.format("sceKernelIcacheInvalidateRange addr=0x%08X, size=%d", addr, size));
		}

        RuntimeContext.invalidateRange(addr, size);

        cpu.gpr[2] = 0;
	}

	public void sceKernelUtilsMd5Digest(Processor processor) {
		CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int inAddr = cpu.gpr[4];
		int inSize = cpu.gpr[5];
        int outAddr = cpu.gpr[6];

        log.info("sceKernelUtilsMd5Digest (inAddr=0x" + Integer.toHexString(inAddr)
                + ", inSize=" + inSize
                + ", outAddr=0x" + Integer.toHexString(outAddr) + ")");

        byte[] input = new byte[inSize];
        byte[] hash = null;
        if (Memory.isAddressGood(inAddr)) {
            for (int i = 0; i < inSize; i++) {
                input[i] = (byte) mem.read8(inAddr + i);
            }
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            hash = md.digest(input);
        } catch (Exception e) {
            // Ignore...
        }
        if ((hash != null) && Memory.isAddressGood(outAddr)) {
            for (int i = 0; i < 16; i++) {
                mem.write8(outAddr + i, hash[i]);
            }
        }
		cpu.gpr[2] = 0;
	}

	public void sceKernelUtilsMd5BlockInit(Processor processor) {
		CpuState cpu = processor.cpu;

        int md5CtxAddr = cpu.gpr[4];

        log.info("sceKernelUtilsMd5BlockInit (md5CtxAddr=0x" + Integer.toHexString(md5CtxAddr) + ")");

        md5Ctx = new SceKernelUtilsMd5Context();
        md5Ctx.init(md5CtxAddr);

		cpu.gpr[2] = 0;
	}

	public void sceKernelUtilsMd5BlockUpdate(Processor processor) {
		CpuState cpu = processor.cpu;

        int md5CtxAddr = cpu.gpr[4];
        int inAddr = cpu.gpr[5];
        int inSize = cpu.gpr[6];

        log.info("sceKernelUtilsMd5BlockUpdate (md5CtxAddr=0x" + Integer.toHexString(md5CtxAddr)
                + ", inAddr=0x" + Integer.toHexString(inAddr)
                + ", inSize=" + inSize + ")" );

        md5Ctx.update(md5CtxAddr, inAddr, inSize);

		cpu.gpr[2] = 0;
	}

	public void sceKernelUtilsMd5BlockResult(Processor processor) {
		CpuState cpu = processor.cpu;

        int md5CtxAddr = cpu.gpr[4];
        int outAddr = cpu.gpr[5];

        log.info("sceKernelUtilsMd5BlockResult (md5CtxAddr=0x" + Integer.toHexString(md5CtxAddr)
                + ", outAddr=0x" + Integer.toHexString(outAddr) + ")" );

        md5Ctx.result(md5CtxAddr, outAddr);

        cpu.gpr[2] = 0;
	}

	public void sceKernelUtilsSha1Digest(Processor processor) {
		CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int inAddr = cpu.gpr[4];
		int inSize = cpu.gpr[5];
        int outAddr = cpu.gpr[6];

        log.info("sceKernelUtilsSha1Digest (inAddr=0x" + Integer.toHexString(inAddr)
                + ", inSize=" + inSize
                + ", outAddr=0x" + Integer.toHexString(outAddr) + ")");

        byte[] input = new byte[inSize];
        byte[] hash = null;
        if (Memory.isAddressGood(inAddr)) {
            for (int i = 0; i < inSize; i++) {
                input[i] = (byte) mem.read8(inAddr + i);
            }
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            hash = md.digest(input);
        } catch (Exception e) {
            // Ignore...
        }
        if ((hash != null) && Memory.isAddressGood(outAddr)) {
            for (int i = 0; i < 16; i++) {
                mem.write8(outAddr + i, hash[i]);
            }
        }
		cpu.gpr[2] = 0;
	}

	public void sceKernelUtilsSha1BlockInit(Processor processor) {
		CpuState cpu = processor.cpu;

        int sha1CtxAddr = cpu.gpr[4];

        log.info("sceKernelUtilsSha1BlockInit (sha1CtxAddr=0x" + Integer.toHexString(sha1CtxAddr) + ")");

        sha1Ctx = new SceKernelUtilsSha1Context();
        sha1Ctx.init(sha1CtxAddr);

		cpu.gpr[2] = 0;
	}

	public void sceKernelUtilsSha1BlockUpdate(Processor processor) {
		CpuState cpu = processor.cpu;

        int sha1CtxAddr = cpu.gpr[4];
        int inAddr = cpu.gpr[5];
        int inSize = cpu.gpr[6];

        log.info("sceKernelUtilsSha1BlockUpdate (sha1CtxAddr=0x" + Integer.toHexString(sha1CtxAddr)
                + ", inAddr=0x" + Integer.toHexString(inAddr)
                + ", inSize=" + inSize + ")" );

        sha1Ctx.update(sha1CtxAddr, inAddr, inSize);

		cpu.gpr[2] = 0;
	}

	public void sceKernelUtilsSha1BlockResult(Processor processor) {
		CpuState cpu = processor.cpu;

        int sha1CtxAddr = cpu.gpr[4];
        int outAddr = cpu.gpr[5];

        log.info("sceKernelUtilsSha1BlockResult (sha1CtxAddr=0x" + Integer.toHexString(sha1CtxAddr)
                + ", outAddr=0x" + Integer.toHexString(outAddr) + ")" );

        sha1Ctx.result(sha1CtxAddr, outAddr);

        cpu.gpr[2] = 0;
	}

	public void sceKernelUtilsMt19937Init(Processor processor) {
		CpuState cpu = processor.cpu;

		int ctx_addr = cpu.gpr[4];
		int seed = cpu.gpr[5];

		// We'll use the address of the ctx as a key
        Mt19937List.remove(ctx_addr); // Remove records of any already existing context at a0
        Mt19937List.put(ctx_addr, new SceKernelUtilsMt19937Context(seed));

        // We'll overwrite all the context memory, 628 bytes
        Memory.getInstance().memset(ctx_addr, (byte) 0xCD, 628);

        cpu.gpr[2] = 0;
	}

	public void sceKernelUtilsMt19937UInt(Processor processor) {
		CpuState cpu = processor.cpu;

		int ctx_addr = cpu.gpr[4];

		SceKernelUtilsMt19937Context ctx = Mt19937List.get(ctx_addr);
        if (ctx != null) {
            cpu.gpr[2] = ctx.r.nextInt();
        } else {
            log.warn("sceKernelUtilsMt19937UInt uninitialised context " + Integer.toHexString(ctx_addr));
            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
	}

	public void sceKernelGetGPI(Processor processor) {
		CpuState cpu = processor.cpu;

		if (State.debugger != null) {
            int gpi = State.debugger.GetGPI();
            if (log.isDebugEnabled()) {
            	log.debug("sceKernelGetGPI 0x" + String.format("%02X", gpi));
            }
            cpu.gpr[2] = gpi;
        } else {
        	if (log.isDebugEnabled()) {
        		log.debug("sceKernelGetGPI debugger not enabled");
        	}
            cpu.gpr[2] = 0;
        }
	}

	public void sceKernelSetGPO(Processor processor) {
		CpuState cpu = processor.cpu;

		int value = cpu.gpr[4];

		if (State.debugger != null) {
            State.debugger.SetGPO(value);
            if (log.isDebugEnabled()) {
            	log.debug("sceKernelSetGPO 0x" + String.format("%02X", value));
            }
        } else {
        	if (log.isDebugEnabled()) {
        		log.debug("sceKernelSetGPO debugger not enabled");
        	}
        }

        cpu.gpr[2] = 0;
	}

	public void sceKernelLibcClock(Processor processor) {
		CpuState cpu = processor.cpu;

		cpu.gpr[2] = (int) SystemTimeManager.getSystemTime();
	}

	public void sceKernelLibcTime(Processor processor) {
		CpuState cpu = processor.cpu;
		Memory mem = Processor.memory;

		int time_t_addr = cpu.gpr[4];

        int seconds = (int)(Calendar.getInstance().getTimeInMillis() / 1000);
        if (Memory.isAddressGood(time_t_addr)) {
            mem.write32(time_t_addr, seconds);
        }
        Emulator.getProcessor().cpu.gpr[2] = seconds;
	}

	public void sceKernelLibcGettimeofday(Processor processor) {
		CpuState cpu = processor.cpu;
		Memory mem = Processor.memory;

		int tp = cpu.gpr[4];
		int tzp = cpu.gpr[5];

        if (Memory.isAddressGood(tp)) {
        	Clock.TimeNanos currentTimeNano = Emulator.getClock().currentTimeNanos();
            int tv_sec = currentTimeNano.seconds;
            int tv_usec = currentTimeNano.millis * 1000 + currentTimeNano.micros;
            mem.write32(tp, tv_sec);
            mem.write32(tp + 4, tv_usec);
        }

        if (Memory.isAddressGood(tzp)) {
            int tz_minuteswest = 0;
            int tz_dsttime = 0;
            mem.write32(tzp, tz_minuteswest);
            mem.write32(tzp + 4, tz_dsttime);
        }

        cpu.gpr[2] = 0;
	}

	public void sceKernelDcacheWritebackAll(Processor processor) {
		Modules.log.trace("IGNORING: sceKernelDcacheWritebackAll");
	}

	public void sceKernelDcacheWritebackInvalidateAll(Processor processor) {
		Modules.log.trace("IGNORING: sceKernelDcacheWritebackInvalidateAll");
	}

	public void sceKernelDcacheWritebackRange(Processor processor) {
        CpuState cpu = processor.cpu;

		int addr = cpu.gpr[4];
		int size = cpu.gpr[5];

		Modules.log.trace("IGNORING: sceKernelDcacheWritebackRange addr=0x" + Integer.toHexString(addr)
                + ", size=" + size);
	}

	public void sceKernelDcacheWritebackInvalidateRange(Processor processor) {
		CpuState cpu = processor.cpu;

		int addr = cpu.gpr[4];
		int size = cpu.gpr[5];

		Modules.log.trace("IGNORING: sceKernelDcacheWritebackInvalidateRange addr=0x" + Integer.toHexString(addr)
                + ", size=" + size);
	}

	public void sceKernelDcacheProbe(Processor processor) {
        CpuState cpu = processor.cpu;

		int addr = cpu.gpr[4];

		Modules.log.trace("IGNORING: sceKernelDcacheProbe addr=0x" + Integer.toHexString(addr));

        cpu.gpr[2] = PSP_KERNEL_DCACHE_PROBE_HIT; // Dummy.
	}

	public void sceKernelDcacheReadTag(Processor processor) {
        CpuState cpu = processor.cpu;

		Modules.log.trace("UNIMPLEMENTED: sceKernelDcacheReadTag");

        cpu.gpr[2] = 0xDEADC0DE;
	}

	public void sceKernelIcacheInvalidateAll(Processor processor) {
		// Some games attempt to change compiled code at runtime
        // by calling this function.
        // Use the RuntimeContext to regenerate a compiling context
        // and restart from there.
    	// This method only works for compiled code being called by
    	//    JR   $rs
    	// or
    	//    JALR $rs, $rd
    	// but not for compiled code being called by
    	//    JAL xxxx
        RuntimeContext.invalidateAll();
        log.info("sceKernelIcacheInvalidateAll");
	}

	public void sceKernelIcacheProbe(Processor processor) {
		CpuState cpu = processor.cpu;

		int addr = cpu.gpr[4];

		Modules.log.trace("IGNORING: sceKernelIcacheProbe addr=0x" + Integer.toHexString(addr));

        cpu.gpr[2] = PSP_KERNEL_ICACHE_PROBE_HIT; // Dummy.
	}

	public void sceKernelIcacheReadTag(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.trace("UNIMPLEMENTED: sceKernelIcacheReadTag");

        cpu.gpr[2] = 0xDEADC0DE;
	}

	public final HLEModuleFunction sceKernelDcacheInvalidateRangeFunction = new HLEModuleFunction("UtilsForUser", "sceKernelDcacheInvalidateRange") {
		@Override
		public final void execute(Processor processor) {
			sceKernelDcacheInvalidateRange(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForUserModule.sceKernelDcacheInvalidateRange(processor);";
		}
	};

	public final HLEModuleFunction sceKernelIcacheInvalidateRangeFunction = new HLEModuleFunction("UtilsForUser", "sceKernelIcacheInvalidateRange") {
		@Override
		public final void execute(Processor processor) {
			sceKernelIcacheInvalidateRange(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForUserModule.sceKernelIcacheInvalidateRange(processor);";
		}
	};

	public final HLEModuleFunction sceKernelUtilsMd5DigestFunction = new HLEModuleFunction("UtilsForUser", "sceKernelUtilsMd5Digest") {
		@Override
		public final void execute(Processor processor) {
			sceKernelUtilsMd5Digest(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForUserModule.sceKernelUtilsMd5Digest(processor);";
		}
	};

	public final HLEModuleFunction sceKernelUtilsMd5BlockInitFunction = new HLEModuleFunction("UtilsForUser", "sceKernelUtilsMd5BlockInit") {
		@Override
		public final void execute(Processor processor) {
			sceKernelUtilsMd5BlockInit(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForUserModule.sceKernelUtilsMd5BlockInit(processor);";
		}
	};

	public final HLEModuleFunction sceKernelUtilsMd5BlockUpdateFunction = new HLEModuleFunction("UtilsForUser", "sceKernelUtilsMd5BlockUpdate") {
		@Override
		public final void execute(Processor processor) {
			sceKernelUtilsMd5BlockUpdate(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForUserModule.sceKernelUtilsMd5BlockUpdate(processor);";
		}
	};

	public final HLEModuleFunction sceKernelUtilsMd5BlockResultFunction = new HLEModuleFunction("UtilsForUser", "sceKernelUtilsMd5BlockResult") {
		@Override
		public final void execute(Processor processor) {
			sceKernelUtilsMd5BlockResult(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForUserModule.sceKernelUtilsMd5BlockResult(processor);";
		}
	};

	public final HLEModuleFunction sceKernelUtilsSha1DigestFunction = new HLEModuleFunction("UtilsForUser", "sceKernelUtilsSha1Digest") {
		@Override
		public final void execute(Processor processor) {
			sceKernelUtilsSha1Digest(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForUserModule.sceKernelUtilsSha1Digest(processor);";
		}
	};

	public final HLEModuleFunction sceKernelUtilsSha1BlockInitFunction = new HLEModuleFunction("UtilsForUser", "sceKernelUtilsSha1BlockInit") {
		@Override
		public final void execute(Processor processor) {
			sceKernelUtilsSha1BlockInit(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForUserModule.sceKernelUtilsSha1BlockInit(processor);";
		}
	};

	public final HLEModuleFunction sceKernelUtilsSha1BlockUpdateFunction = new HLEModuleFunction("UtilsForUser", "sceKernelUtilsSha1BlockUpdate") {
		@Override
		public final void execute(Processor processor) {
			sceKernelUtilsSha1BlockUpdate(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForUserModule.sceKernelUtilsSha1BlockUpdate(processor);";
		}
	};

	public final HLEModuleFunction sceKernelUtilsSha1BlockResultFunction = new HLEModuleFunction("UtilsForUser", "sceKernelUtilsSha1BlockResult") {
		@Override
		public final void execute(Processor processor) {
			sceKernelUtilsSha1BlockResult(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForUserModule.sceKernelUtilsSha1BlockResult(processor);";
		}
	};

	public final HLEModuleFunction sceKernelUtilsMt19937InitFunction = new HLEModuleFunction("UtilsForUser", "sceKernelUtilsMt19937Init") {
		@Override
		public final void execute(Processor processor) {
			sceKernelUtilsMt19937Init(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForUserModule.sceKernelUtilsMt19937Init(processor);";
		}
	};

	public final HLEModuleFunction sceKernelUtilsMt19937UIntFunction = new HLEModuleFunction("UtilsForUser", "sceKernelUtilsMt19937UInt") {
		@Override
		public final void execute(Processor processor) {
			sceKernelUtilsMt19937UInt(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForUserModule.sceKernelUtilsMt19937UInt(processor);";
		}
	};

	public final HLEModuleFunction sceKernelGetGPIFunction = new HLEModuleFunction("UtilsForUser", "sceKernelGetGPI") {
		@Override
		public final void execute(Processor processor) {
			sceKernelGetGPI(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForUserModule.sceKernelGetGPI(processor);";
		}
	};

	public final HLEModuleFunction sceKernelSetGPOFunction = new HLEModuleFunction("UtilsForUser", "sceKernelSetGPO") {
		@Override
		public final void execute(Processor processor) {
			sceKernelSetGPO(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForUserModule.sceKernelSetGPO(processor);";
		}
	};

	public final HLEModuleFunction sceKernelLibcClockFunction = new HLEModuleFunction("UtilsForUser", "sceKernelLibcClock") {
		@Override
		public final void execute(Processor processor) {
			sceKernelLibcClock(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForUserModule.sceKernelLibcClock(processor);";
		}
	};

	public final HLEModuleFunction sceKernelLibcTimeFunction = new HLEModuleFunction("UtilsForUser", "sceKernelLibcTime") {
		@Override
		public final void execute(Processor processor) {
			sceKernelLibcTime(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForUserModule.sceKernelLibcTime(processor);";
		}
	};

	public final HLEModuleFunction sceKernelLibcGettimeofdayFunction = new HLEModuleFunction("UtilsForUser", "sceKernelLibcGettimeofday") {
		@Override
		public final void execute(Processor processor) {
			sceKernelLibcGettimeofday(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForUserModule.sceKernelLibcGettimeofday(processor);";
		}
	};

	public final HLEModuleFunction sceKernelDcacheWritebackAllFunction = new HLEModuleFunction("UtilsForUser", "sceKernelDcacheWritebackAll") {
		@Override
		public final void execute(Processor processor) {
			sceKernelDcacheWritebackAll(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForUserModule.sceKernelDcacheWritebackAll(processor);";
		}
	};

	public final HLEModuleFunction sceKernelDcacheWritebackInvalidateAllFunction = new HLEModuleFunction("UtilsForUser", "sceKernelDcacheWritebackInvalidateAll") {
		@Override
		public final void execute(Processor processor) {
			sceKernelDcacheWritebackInvalidateAll(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForUserModule.sceKernelDcacheWritebackInvalidateAll(processor);";
		}
	};

	public final HLEModuleFunction sceKernelDcacheWritebackRangeFunction = new HLEModuleFunction("UtilsForUser", "sceKernelDcacheWritebackRange") {
		@Override
		public final void execute(Processor processor) {
			sceKernelDcacheWritebackRange(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForUserModule.sceKernelDcacheWritebackRange(processor);";
		}
	};

	public final HLEModuleFunction sceKernelDcacheWritebackInvalidateRangeFunction = new HLEModuleFunction("UtilsForUser", "sceKernelDcacheWritebackInvalidateRange") {
		@Override
		public final void execute(Processor processor) {
			sceKernelDcacheWritebackInvalidateRange(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForUserModule.sceKernelDcacheWritebackInvalidateRange(processor);";
		}
	};

	public final HLEModuleFunction sceKernelDcacheProbeFunction = new HLEModuleFunction("UtilsForUser", "sceKernelDcacheProbe") {
		@Override
		public final void execute(Processor processor) {
			sceKernelDcacheProbe(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForUserModule.sceKernelDcacheProbe(processor);";
		}
	};

	public final HLEModuleFunction sceKernelDcacheReadTagFunction = new HLEModuleFunction("UtilsForUser", "sceKernelDcacheReadTag") {
		@Override
		public final void execute(Processor processor) {
			sceKernelDcacheReadTag(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForUserModule.sceKernelDcacheReadTag(processor);";
		}
	};

	public final HLEModuleFunction sceKernelIcacheInvalidateAllFunction = new HLEModuleFunction("UtilsForUser", "sceKernelIcacheInvalidateAll") {
		@Override
		public final void execute(Processor processor) {
			sceKernelIcacheInvalidateAll(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForUserModule.sceKernelIcacheInvalidateAll(processor);";
		}
	};

	public final HLEModuleFunction sceKernelIcacheProbeFunction = new HLEModuleFunction("UtilsForUser", "sceKernelIcacheProbe") {
		@Override
		public final void execute(Processor processor) {
			sceKernelIcacheProbe(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForUserModule.sceKernelIcacheProbe(processor);";
		}
	};

	public final HLEModuleFunction sceKernelIcacheReadTagFunction = new HLEModuleFunction("UtilsForUser", "sceKernelIcacheReadTag") {
		@Override
		public final void execute(Processor processor) {
			sceKernelIcacheReadTag(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.UtilsForUserModule.sceKernelIcacheReadTag(processor);";
		}
	};
}