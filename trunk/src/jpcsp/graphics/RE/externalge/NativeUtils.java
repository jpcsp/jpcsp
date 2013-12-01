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

import jpcsp.util.DurationStatistics;

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
    private static int intArrayBaseOffset = 0;
    private static long memoryIntAddress = 0L;
	private static Object[] arrayObject = new Object[] { null };
	private static long arrayObjectBaseOffset = 0L;
    private static DurationStatistics coreInterpret = new DurationStatistics("coreInterpret");
    public static final int EVENT_GE_START_LIST        = 0;
    public static final int EVENT_GE_FINISH_LIST       = 1;
    public static final int EVENT_GE_ENQUEUE_LIST      = 2;
    public static final int EVENT_GE_UPDATE_STALL_ADDR = 3;
    public static final int EVENT_GE_WAIT_FOR_LIST     = 4;
    public static final int EVENT_DISPLAY_WAIT_VBLANK  = 5;
    public static final int EVENT_DISPLAY_VBLANK       = 6;

	public static void init() {
		if (!isInitialized) {
			try {
				System.loadLibrary("software-ge-renderer");
				initNative();
				log.info(String.format("Loaded software-ge-renderer library"));
				isAvailable = true;
			} catch (UnsatisfiedLinkError e) {
				isAvailable = false;
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
				}
			} catch (NoSuchFieldException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			unsafeInitialized = true;
    	}

    	if (unsafe == null) {
    		return 0L;
    	}

    	arrayObject[0] = memoryInt;
    	long address = unsafe.getInt(arrayObject, arrayObjectBaseOffset);
    	if (address == 0L) {
    		return address;
    	}

    	return address + intArrayBaseOffset;
    }

    public static void updateMemoryUnsafeAddr() {
    	long address = getMemoryUnsafeAddr();
    	if (memoryIntAddress != address) {
    		if (log.isDebugEnabled()) {
	    		if (memoryIntAddress == 0L) {
	        		log.debug(String.format("memoryInt at 0x%X", address));
	        	} else {
	        		log.debug(String.format("memoryInt MOVED from 0x%X to 0x%X", memoryIntAddress, address));
	    		}
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
}
