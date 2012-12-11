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

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;

import java.security.MessageDigest;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Random;

import jpcsp.Clock;
import jpcsp.Emulator;
import jpcsp.State;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.SystemTimeManager;
import jpcsp.HLE.modules.HLEModule;

import org.apache.log4j.Logger;

@HLELogging
public class UtilsForUser extends HLEModule {
    public static Logger log = Modules.getLogger("UtilsForUser");

	private HashMap<Integer, SceKernelUtilsMt19937Context> Mt19937List;
    private SceKernelUtilsMd5Context md5Ctx;
    private SceKernelUtilsSha1Context sha1Ctx;

    private static class SceKernelUtilsMt19937Context {
        private Random r;

        public SceKernelUtilsMt19937Context(TPointer ctxAddr, int seed) {
            r = new Random(seed);

            // Overwrite the context memory (628 bytes)
            ctxAddr.memset((byte) 0xCD, 628);
        }

        public int getInt(TPointer ctxAddr) {
        	return r.nextInt();
        }
    }

    private static class SceKernelUtilsContext {
    	private final String algorithm;
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

        protected SceKernelUtilsContext(String algorithm) {
        	this.algorithm = algorithm;
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

        public int init(TPointer ctxAddr) {
            ctxAddr.setValue32(0, part1);
            ctxAddr.setValue32(4, part2);
            ctxAddr.setValue32(8, part3);
            ctxAddr.setValue32(12, part4);
            ctxAddr.setValue32(16, part5);
            ctxAddr.setValue16(20, tmpBytesRemaining);
            ctxAddr.setValue16(22, tmpBytesCalculated);
            ctxAddr.setValue64(24, fullDataSize);
            ctxAddr.setArray(32, buf, 64);

            return 0;
        }

        public int update(TPointer ctxAddr, TPointer dataAddr, int dataSize) {
            input = dataAddr.getArray8(dataSize);

            return 0;
        }

        public int result(TPointer ctxAddr, TPointer resultAddr) {
            byte[] hash = null;
            try {
                MessageDigest md = MessageDigest.getInstance(algorithm);
                hash = md.digest(input);
            } catch (Exception e) {
                // Ignore...
            	log.warn(String.format("SceKernelUtilsContext(%s).result", algorithm), e);
            }

            if (hash != null) {
            	resultAddr.setArray(hash, 16);
            }

            return 0;
        }

        protected static int digest(TPointer inAddr, int inSize, TPointer outAddr, String algorithm) {
            byte[] input = inAddr.getArray8(inSize);
            byte[] hash = null;
            try {
                MessageDigest md = MessageDigest.getInstance(algorithm);
                hash = md.digest(input);
            } catch (Exception e) {
                // Ignore...
            	log.warn(String.format("SceKernelUtilsContext(%s).digest", algorithm), e);
            }
            if (hash != null) {
            	outAddr.setArray(hash, 16);
            }

            return 0;
        }
    }

    private static class SceKernelUtilsMd5Context extends SceKernelUtilsContext {
    	private static final String algorithm = "MD5";

    	public SceKernelUtilsMd5Context() {
        	super(algorithm);
        }

    	public static int digest(TPointer inAddr, int inSize, TPointer outAddr) {
    		return digest(inAddr, inSize, outAddr, algorithm);
    	}
    }

    private static class SceKernelUtilsSha1Context extends SceKernelUtilsContext {
    	private static final String algorithm = "SHA-1";

    	public SceKernelUtilsSha1Context() {
			super(algorithm);
		}

    	public static int digest(TPointer inAddr, int inSize, TPointer outAddr) {
    		return digest(inAddr, inSize, outAddr, algorithm);
    	}
    }

	@Override
	public String getName() {
		return "UtilsForUser";
	}

	@Override
	public void start() {
        Mt19937List = new HashMap<Integer, SceKernelUtilsMt19937Context>();

        super.start();
    }

    protected static final int PSP_KERNEL_ICACHE_PROBE_MISS = 0;
    protected static final int PSP_KERNEL_ICACHE_PROBE_HIT = 1;
    protected static final int PSP_KERNEL_DCACHE_PROBE_MISS = 0;
    protected static final int PSP_KERNEL_DCACHE_PROBE_HIT = 1;
    protected static final int PSP_KERNEL_DCACHE_PROBE_HIT_DIRTY = 2;

    @HLELogging(level="trace")
	@HLEFunction(nid = 0xBFA98062, version = 150)
	public int sceKernelDcacheInvalidateRange(TPointer addr, int size) {
        return 0;
	}

    @HLELogging(level="info")
	@HLEFunction(nid = 0xC2DF770E, version = 150)
	public int sceKernelIcacheInvalidateRange(TPointer addr, int size) {
		if (log.isInfoEnabled()) {
			log.info(String.format("sceKernelIcacheInvalidateRange addr=%s, size=%d", addr, size));
		}

        RuntimeContext.invalidateRange(addr.getAddress(), size);

        return 0;
	}

    @HLELogging(level="info")
	@HLEFunction(nid = 0xC8186A58, version = 150)
	public int sceKernelUtilsMd5Digest(TPointer inAddr, int inSize, TPointer outAddr) {
    	return SceKernelUtilsMd5Context.digest(inAddr, inSize, outAddr);
	}

    @HLELogging(level="info")
	@HLEFunction(nid = 0x9E5C5086, version = 150)
	public int sceKernelUtilsMd5BlockInit(TPointer md5CtxAddr) {
        md5Ctx = new SceKernelUtilsMd5Context();
        return md5Ctx.init(md5CtxAddr);
	}

    @HLELogging(level="info")
	@HLEFunction(nid = 0x61E1E525, version = 150)
	public int sceKernelUtilsMd5BlockUpdate(TPointer md5CtxAddr, TPointer inAddr, int inSize) {
        return md5Ctx.update(md5CtxAddr, inAddr, inSize);
	}

    @HLELogging(level="info")
	@HLEFunction(nid = 0xB8D24E78, version = 150)
	public int sceKernelUtilsMd5BlockResult(TPointer md5CtxAddr, TPointer outAddr) {
        return md5Ctx.result(md5CtxAddr, outAddr);
	}

    @HLELogging(level="info")
	@HLEFunction(nid = 0x840259F1, version = 150)
	public int sceKernelUtilsSha1Digest(TPointer inAddr, int inSize, TPointer outAddr) {
    	return SceKernelUtilsSha1Context.digest(inAddr, inSize, outAddr);
	}

    @HLELogging(level="info")
	@HLEFunction(nid = 0xF8FCD5BA, version = 150)
	public int sceKernelUtilsSha1BlockInit(TPointer sha1CtxAddr) {
        sha1Ctx = new SceKernelUtilsSha1Context();
        return sha1Ctx.init(sha1CtxAddr);
	}

    @HLELogging(level="info")
	@HLEFunction(nid = 0x346F6DA8, version = 150)
	public int sceKernelUtilsSha1BlockUpdate(TPointer sha1CtxAddr, TPointer inAddr, int inSize) {
        return sha1Ctx.update(sha1CtxAddr, inAddr, inSize);
	}

    @HLELogging(level="info")
	@HLEFunction(nid = 0x585F1C09, version = 150)
	public int sceKernelUtilsSha1BlockResult(TPointer sha1CtxAddr, TPointer outAddr) {
        return sha1Ctx.result(sha1CtxAddr, outAddr);
	}

	@HLEFunction(nid = 0xE860E75E, version = 150)
	public int sceKernelUtilsMt19937Init(TPointer ctxAddr, int seed) {
		// We'll use the address of the ctx as a key
        Mt19937List.remove(ctxAddr.getAddress()); // Remove records of any already existing context at a0
        Mt19937List.put(ctxAddr.getAddress(), new SceKernelUtilsMt19937Context(ctxAddr, seed));

        return 0;
	}

	@HLEFunction(nid = 0x06FB8A63, version = 150)
	public int sceKernelUtilsMt19937UInt(TPointer ctxAddr) {
		SceKernelUtilsMt19937Context ctx = Mt19937List.get(ctxAddr.getAddress());
        if (ctx == null) {
            log.warn(String.format("sceKernelUtilsMt19937UInt uninitialised context %s", ctxAddr));
            return 0;
        }

        return ctx.getInt(ctxAddr);
	}

	@HLEFunction(nid = 0x37FB5C42, version = 150)
	public int sceKernelGetGPI() {
		int gpi;

		if (State.debugger != null) {
            gpi = State.debugger.GetGPI();
            if (log.isDebugEnabled()) {
            	log.debug(String.format("sceKernelGetGPI returning 0x%02X", gpi));
            }
        } else {
        	gpi = 0;
        	if (log.isDebugEnabled()) {
        		log.debug("sceKernelGetGPI debugger not enabled");
        	}
        }

		return gpi;
	}

	@HLEFunction(nid = 0x6AD345D7, version = 150)
	public int sceKernelSetGPO(int value) {
		if (State.debugger != null) {
            State.debugger.SetGPO(value);
        } else {
        	if (log.isDebugEnabled()) {
        		log.debug("sceKernelSetGPO debugger not enabled");
        	}
        }

        return 0;
	}

	@HLEFunction(nid = 0x91E4F6A7, version = 150)
	public int sceKernelLibcClock() {
		return (int) SystemTimeManager.getSystemTime();
	}

	@HLEFunction(nid = 0x27CC57F0, version = 150)
	public int sceKernelLibcTime(@CanBeNull TPointer32 time_t_addr) {
        int seconds = (int)(Calendar.getInstance().getTimeInMillis() / 1000);
        time_t_addr.setValue(seconds);

        return seconds;
	}

	@HLEFunction(nid = 0x71EC4271, version = 150)
	public int sceKernelLibcGettimeofday(@CanBeNull TPointer32 tp, @CanBeNull TPointer32 tzp) {
    	Clock.TimeNanos currentTimeNano = Emulator.getClock().currentTimeNanos();
        int tv_sec = currentTimeNano.seconds;
        int tv_usec = currentTimeNano.millis * 1000 + currentTimeNano.micros;
        tp.setValue(0, tv_sec);
        tp.setValue(4, tv_usec);

        // PSP always returning 0 for these 2 values:
        int tz_minuteswest = 0;
        int tz_dsttime = 0;
        tzp.setValue(0, tz_minuteswest);
        tzp.setValue(4, tz_dsttime);

        return 0;
	}

	@HLELogging(level="trace")
	@HLEFunction(nid = 0x79D1C3FA, version = 150)
	public void sceKernelDcacheWritebackAll() {
	}

	@HLELogging(level="trace")
	@HLEFunction(nid = 0xB435DEC5, version = 150)
	public void sceKernelDcacheWritebackInvalidateAll() {
	}

	@HLELogging(level="trace")
	@HLEFunction(nid = 0x3EE30821, version = 150)
	public int sceKernelDcacheWritebackRange(TPointer addr, int size) {
		return 0;
	}

	@HLELogging(level="trace")
	@HLEFunction(nid = 0x34B9FA9E, version = 150)
	public void sceKernelDcacheWritebackInvalidateRange(TPointer addr, int size) {
	}

	@HLELogging(level="trace")
	@HLEFunction(nid = 0x80001C4C, version = 150)
	public int sceKernelDcacheProbe(TPointer addr) {
        return PSP_KERNEL_DCACHE_PROBE_HIT; // Dummy
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x16641D70, version = 150)
	public int sceKernelDcacheReadTag() {
		return 0;
	}

	@HLELogging(level="info")
	@HLEFunction(nid = 0x920F104A, version = 150)
	public void sceKernelIcacheInvalidateAll() {
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
	}

	@HLELogging(level="trace")
	@HLEFunction(nid = 0x4FD31C9D, version = 150)
	public int sceKernelIcacheProbe(TPointer addr) {
        return PSP_KERNEL_ICACHE_PROBE_HIT; // Dummy
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xFB05FAD0, version = 150)
	public void sceKernelIcacheReadTag() {
	}
}