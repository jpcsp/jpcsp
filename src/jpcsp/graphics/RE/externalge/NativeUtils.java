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
package jpcsp.graphics.RE.externalge;

import static jpcsp.Allegrex.compiler.RuntimeContext.memoryInt;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import jpcsp.Memory;
import jpcsp.memory.FastMemory;
import jpcsp.util.DurationStatistics;
import jpcsp.util.NativeCpuInfo;
import jpcsp.util.Utilities;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import sun.misc.Unsafe;

/**
 * @author gid15
 *
 */
public class NativeUtils {
	public static Logger log = ExternalGE.log;
	private static boolean isInitialized = false;
	private static boolean isAvailable = false;
    private static Unsafe unsafe = null;
    private static boolean unsafeInitialized = false;
    private static long intArrayBaseOffset = 0L;
    private static long memoryIntAddress = 0L;
	private static Object[] arrayObject = new Object[] { null, 0x123456789ABCDEFL, 0x1111111122222222L };
	private static long arrayObjectBaseOffset = 0L;
	private static int arrayObjectIndexScale = 0;
	private static int addressSize = 0;
    private static DurationStatistics coreInterpret = new DurationStatistics("coreInterpret");
    public static final int EVENT_GE_START_LIST        = 0;
    public static final int EVENT_GE_FINISH_LIST       = 1;
    public static final int EVENT_GE_ENQUEUE_LIST      = 2;
    public static final int EVENT_GE_UPDATE_STALL_ADDR = 3;
    public static final int EVENT_GE_WAIT_FOR_LIST     = 4;
    public static final int EVENT_DISPLAY_WAIT_VBLANK  = 5;
    public static final int EVENT_DISPLAY_VBLANK       = 6;
	public static final int INTR_STAT_SIGNAL = 0x1;
	public static final int INTR_STAT_END    = 0x2;
	public static final int INTR_STAT_FINISH = 0x4;
	public static final int CTRL_ACTIVE      = 0x1;

	public static void init() {
		if (!isInitialized) {
			List<String> libraries = new LinkedList<String>();
			if (NativeCpuInfo.isAvailable()) {
				NativeCpuInfo.init();
				if (NativeCpuInfo.hasAVX2()) {
					libraries.add("software-ge-renderer-AVX2");
				}
				if (NativeCpuInfo.hasAVX()) {
					libraries.add("software-ge-renderer-AVX");
				}
				if (NativeCpuInfo.hasSSE41()) {
					libraries.add("software-ge-renderer-SSE41");
				}
				if (NativeCpuInfo.hasSSE3()) {
					libraries.add("software-ge-renderer-SSE3");
				}
				if (NativeCpuInfo.hasSSE2()) {
					libraries.add("software-ge-renderer-SSE2");
				}
			}
			libraries.add("software-ge-renderer");

			boolean libraryExisting = false;
			// Search for an available library in preference order
			for (String library : libraries) {
				if (Utilities.isSystemLibraryExisting(library)) {
					libraryExisting = true;
					try {
						System.loadLibrary(library);
						if (Memory.getInstance() instanceof FastMemory) {
							memoryInt = ((FastMemory) Memory.getInstance()).getAll();
						}
						initNative();
						log.info(String.format("Loaded %s library", library));
						isAvailable = true;
					} catch (UnsatisfiedLinkError e) {
						log.error(String.format("Could not load external software library %s: %s", library, e));
						isAvailable = false;
					}
					break;
				}
			}
			if (!libraryExisting) {
				log.error(String.format("Missing external software library"));
			}

			isInitialized = true;
		}
	}

	public static void exit() {
		if (DurationStatistics.collectStatistics) {
			log.info(coreInterpret.toString());
		}
	}

	public static boolean isAvailable() {
		return isAvailable;
	}

    public static void checkMemoryIntAddress() {
    	long address = getMemoryUnsafeAddr();
    	if (address == 0L) {
    		return;
    	}

		memoryInt[0] = 0x12345678;
		int x = unsafe.getInt(memoryIntAddress);
		if (x != memoryInt[0]) {
			log.error(String.format("Non matching value 0x%08X - 0x%08X", x, memoryInt[0]));
		} else {
			log.info(String.format("Matching value 0x%08X - 0x%08X", x, memoryInt[0]));
		}
		memoryInt[0] = 0;
    }

    public static long getMemoryUnsafeAddr() {
    	if (!ExternalGE.useUnsafe || memoryInt == null) {
    		return 0L;
    	}

    	if (!unsafeInitialized) {
			try {
				Field f = Unsafe.class.getDeclaredField("theUnsafe");
				if (f != null) {
			    	f.setAccessible(true);
			    	unsafe = (Unsafe) f.get(null);
			    	intArrayBaseOffset = unsafe.arrayBaseOffset(memoryInt.getClass());
			    	arrayObjectBaseOffset = unsafe.arrayBaseOffset(arrayObject.getClass());
			    	arrayObjectIndexScale = unsafe.arrayIndexScale(arrayObject.getClass());
			    	addressSize = unsafe.addressSize();

			    	if (log.isInfoEnabled()) {
			    		log.info(String.format("Unsafe address information: addressSize=%d, arrayBase=%d, indexScale=%d", addressSize, arrayObjectBaseOffset, arrayObjectIndexScale));
			    	}

			    	if (addressSize != 4 && addressSize != 8) {
			    		log.error(String.format("Unknown addressSize=%d", addressSize));
			    	}
			    	if (arrayObjectIndexScale != 4 && arrayObjectIndexScale != 8) {
			    		log.error(String.format("Unknown addressSize=%d, indexScale=%d", addressSize, arrayObjectIndexScale));
			    	}
			    	if (arrayObjectIndexScale > addressSize) {
			    		log.error(String.format("Unknown addressSize=%d, indexScale=%d", addressSize, arrayObjectIndexScale));
			    	}
				}
			} catch (NoSuchFieldException e) {
				log.error("getMemoryUnsafeAddr", e);
			} catch (SecurityException e) {
				log.error("getMemoryUnsafeAddr", e);
			} catch (IllegalArgumentException e) {
				log.error("getMemoryUnsafeAddr", e);
			} catch (IllegalAccessException e) {
				log.error("getMemoryUnsafeAddr", e);
			}
			unsafeInitialized = true;
    	}

    	if (unsafe == null) {
    		return 0L;
    	}

    	arrayObject[0] = memoryInt;
    	long address = 0L;
    	if (addressSize == 4) {
    		address = unsafe.getInt(arrayObject, arrayObjectBaseOffset);
        	address &= 0xFFFFFFFFL;
    	} else if (addressSize == 8) {
    		if (arrayObjectIndexScale == 8) {
    			// The JVM is running with the following option disabled:
    			//   -XX:-UseCompressedOops
    			// Object addresses are stored as 64-bit values.
    			address = unsafe.getLong(arrayObject, arrayObjectBaseOffset);
    		} else if (arrayObjectIndexScale == 4) {
    			// The JVM is running with the following option enabled
    			//   -XX:+UseCompressedOops
    			// Object addresses are stored as compressed 32-bit values (shifted by 3).
    			address = unsafe.getInt(arrayObject, arrayObjectBaseOffset) & 0xFFFFFFFFL;
    			address <<= 3;
    		}
    	}

    	if (address == 0L) {
    		return address;
    	}

    	if (false) {
	    	// Perform a self-test
	    	int testValue = 0x12345678;
	    	int originalValue = memoryInt[0];
	    	memoryInt[0] = testValue;
	    	int resultValue = unsafe.getInt(address + intArrayBaseOffset);
	    	memoryInt[0] = originalValue;
	    	if (resultValue != testValue) {
	    		log.error(String.format("Unsafe self-test failed: 0x%08X != 0x%08X", testValue, resultValue));
	    	}
    	}

    	return address + intArrayBaseOffset;
    }

    public static void updateMemoryUnsafeAddr() {
    	long address = getMemoryUnsafeAddr();
    	if (memoryIntAddress != address) {
    		if (log.isDebugEnabled()) {
        		log.debug(String.format("memoryInt at 0x%X", address));
    		}
    		if (log.isInfoEnabled() && memoryIntAddress != 0L) {
    			log.info(String.format("memoryInt MOVED from 0x%X to 0x%X", memoryIntAddress, address));
    		}

    		memoryIntAddress = address;
    		setMemoryUnsafeAddr(memoryIntAddress);
    	}
    }

    public static void setLogLevel() {
    	setLogLevel(log);
    }

    public static void setLogLevel(Logger log) {
    	int level = 7; // E_DEFAULT
    	switch (log.getEffectiveLevel().toInt()) {
	    	case Level.ALL_INT:
	    		level = 7; // E_FORCE
	    		break;
	    	case Level.TRACE_INT:
	    		level = 6; // E_TRACE
	    		break;
	    	case Level.DEBUG_INT:
	    		level = 5; // E_DEBUG
	    		break;
	    	case Level.INFO_INT:
	    		level = 4; // E_INFO
	    		break;
	    	case Level.WARN_INT:
	    		level = 3; // E_WARN
	    		break;
	    	case Level.ERROR_INT:
	    		level = 2; // E_ERROR
	    		break;
	    	case Level.FATAL_INT:
	    		level = 1; // E_FATAL
	    		break;
	    	case Level.OFF_INT:
	    		level = 0; // E_OFF
	    		break;
    	}

    	setLogLevel(level);
    }

    public static boolean coreInterpretWithStatistics() {
    	coreInterpret.start();
    	boolean result = coreInterpret();
    	coreInterpret.end();

    	return result;
    }

	public static boolean isCoreCtrlActive() {
		return (getCoreCtrl() & CTRL_ACTIVE) != 0;
	}

	public static native int initNative();
    public static native boolean coreInterpret();
    public static native int getCoreCtrl();
    public static native void setCoreCtrl(int ctrl);
    public static native void setCoreCtrlActive();
    public static native int getCoreStat();
    public static native void setCoreStat(int stat);
    public static native int getCoreMadr();
    public static native void setCoreMadr(int madr);
    public static native int getCoreSadr();
    public static native void setCoreSadr(int sadr);
    public static native int getCoreIntrStat();
    public static native void setCoreIntrStat(int intrStat);
    public static native int getCoreCmdArray(int cmd);
    public static native void setCoreCmdArray(int cmd, int value);
    public static native float getCoreMtxArray(int mtx);
    public static native void setCoreMtxArray(int mtx, float value);
    public static native void setLogLevel(int level);
    public static native void setMemoryUnsafeAddr(long addr);
    public static native void startEvent(int event);
    public static native void stopEvent(int event);
    public static native void notifyEvent(int event);
    public static native void setRendererAsyncRendering(boolean asyncRendering);
    public static native int getRendererIndexCount();
    public static native void rendererRender(int lineMask);
    public static native void rendererTerminate();
    public static native void setDumpFrames(boolean dumpFrames);
    public static native void setDumpTextures(boolean dumpTextures);
    public static native void saveCoreContext(int addr);
    public static native void restoreCoreContext(int addr);
    public static native void setScreenScale(int screenScale);
    public static native ByteBuffer getScaledScreen(int address, int bufferWidth, int height, int pixelFormat);
    public static native void addVideoTexture(int destinationAddress, int sourceAddress, int length);
    public static native void setMaxTextureSizeLog2(int maxTextureSizeLog2);
    public static native void setDoubleTexture2DCoords(boolean doubleTexture2DCoords);
    public static native void doTests();
}
