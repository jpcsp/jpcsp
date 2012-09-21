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
package jpcsp.HLE.kernel.types;

import static jpcsp.Allegrex.Common._gp;
import static jpcsp.Allegrex.Common._k0;
import static jpcsp.Allegrex.Common._ra;
import static jpcsp.Allegrex.Common._sp;
import static jpcsp.Allegrex.Common._zr;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_THREAD_ALREADY_DORMANT;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Allegrex.Common;
import jpcsp.Allegrex.CpuState;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.modules.SysMemUserForUser;
import jpcsp.HLE.modules150.SysMemUserForUser.SysMemInfo;
import jpcsp.HLE.modules150.ThreadManForUser.Callback;

public class SceKernelThreadInfo extends pspAbstractMemoryMappedStructureVariableLength implements Comparator<SceKernelThreadInfo> {

    public static final int PSP_MODULE_USER = 0;
    public static final int PSP_MODULE_NO_STOP = 0x00000001;
    public static final int PSP_MODULE_SINGLE_LOAD = 0x00000002;
    public static final int PSP_MODULE_SINGLE_START = 0x00000004;
    public static final int PSP_MODULE_POPS = 0x00000200;
    public static final int PSP_MODULE_DEMO = 0x00000200; // same as PSP_MODULE_POPS
    public static final int PSP_MODULE_GAMESHARING = 0x00000400;
    public static final int PSP_MODULE_VSH = 0x00000800; // can only be loaded from kernel mode?
    public static final int PSP_MODULE_KERNEL = 0x00001000;
    public static final int PSP_MODULE_USE_MEMLMD_LIB = 0x00002000;
    public static final int PSP_MODULE_USE_SEMAPHORE_LIB = 0x00004000; // not kernel semaphores, but a fake name (actually security stuff)
    public static final int PSP_THREAD_ATTR_USER = 0x80000000; // module attr 0, thread attr: 0x800000FF?
    public static final int PSP_THREAD_ATTR_USBWLAN = 0xa0000000;
    public static final int PSP_THREAD_ATTR_VSH = 0xc0000000;
    public static final int PSP_THREAD_ATTR_KERNEL = 0x00001000; // module attr 0x1000, thread attr: 0?
    public static final int PSP_THREAD_ATTR_VFPU = 0x00004000;
    public static final int PSP_THREAD_ATTR_SCRATCH_SRAM = 0x00008000;
    public static final int PSP_THREAD_ATTR_NO_FILLSTACK = 0x00100000; // Disables filling the stack with 0xFF on creation.
    public static final int PSP_THREAD_ATTR_CLEAR_STACK = 0x00200000; // Clear the stack when the thread is deleted.
    // PspThreadStatus
    public static final int PSP_THREAD_RUNNING = 0x00000001;
    public static final int PSP_THREAD_READY = 0x00000002;
    public static final int PSP_THREAD_WAITING = 0x00000004;
    public static final int PSP_THREAD_SUSPEND = 0x00000008;
    public static final int PSP_THREAD_WAITING_SUSPEND = PSP_THREAD_WAITING | PSP_THREAD_SUSPEND;
    public static final int PSP_THREAD_STOPPED = 0x00000010;
    public static final int PSP_THREAD_KILLED = 0x00000020;
    // Wait types
    public static final int PSP_WAIT_NONE = 0x00;
    public static final int PSP_WAIT_SLEEP = 0x01; // Wait on sleep thread.
    public static final int PSP_WAIT_DELAY = 0x02; // Wait on delay thread.
    public static final int PSP_WAIT_SEMA = 0x03; // Wait on sema.
    public static final int PSP_WAIT_EVENTFLAG = 0x04; // Wait on event flag.
    public static final int PSP_WAIT_MBX = 0x05; // Wait on mbx.
    public static final int PSP_WAIT_VPL = 0x06; // Wait on vpl.
    public static final int PSP_WAIT_FPL = 0x07; // Wait on fpl.
    public static final int PSP_WAIT_MSGPIPE = 0x08; // Wait on msg pipe (send and receive).
    public static final int PSP_WAIT_THREAD_END = 0x09; // Wait on thread end.
    public static final int PSP_WAIT_EVENTHANDLER = 0x0a; // Wait on event handler release.
    public static final int PSP_WAIT_CALLBACK_DELETE = 0x0b; // Wait on callback delete.
    public static final int PSP_WAIT_MUTEX = 0x0c; // Wait on mutex.
    public static final int PSP_WAIT_LWMUTEX = 0x0d; // Wait on lwmutex.
    // These wait types are only used internally in Jpcsp and are not real PSP wait types.
    public static final int JPCSP_FIRST_INTERNAL_WAIT_TYPE = 0x100;
    public static final int JPCSP_WAIT_IO = JPCSP_FIRST_INTERNAL_WAIT_TYPE; // Wait on IO.
    public static final int JPCSP_WAIT_UMD = JPCSP_WAIT_IO + 1; // Wait on UMD.
    public static final int JPCSP_WAIT_BLOCKED = JPCSP_WAIT_UMD + 1; // Thread blocked.
    // SceKernelThreadInfo.
    public final String name;
    public int attr;
    public int status; // it's a bitfield but I don't think we ever use more than 1 bit at once
    public final int entry_addr;
    private int stackAddr; // using low address, no need to add stackSize to the pointer returned by malloc
    public int stackSize;
    public int gpReg_addr;
    public final int initPriority; // lower numbers mean higher priority
    public int currentPriority;
    public int waitType;
    public int waitId;  // the uid of the wait object
    public int wakeupCount; // number of sceKernelWakeupThread() calls pending
    public int exitStatus;
    public TPointer32 exitStatusAddr; // Store the exitStatus at this address if specified
    public long runClocks;
    public int intrPreemptCount;
    public int threadPreemptCount;
    public int releaseCount;
    public int notifyCallback;  // Used by sceKernelNotifyCallback to check if a callback has been called or not.
    public int errno; // used by sceNetInet
    private SysMemInfo stackSysMemInfo;
    // internal variables
    public final int uid;
    public int moduleid;
    public CpuState cpuContext;
    public boolean doDelete;
    public IAction doDeleteAction;
    public boolean doCallbacks;
    public final ThreadWaitInfo wait;
    public int displayLastWaitVcount;
    public long javaThreadId = -1;
    public long javaThreadCpuTimeNanos = -1;
    // Callbacks, only 1 of each type can be registered per thread.
    public final static int THREAD_CALLBACK_UMD = 0;
    public final static int THREAD_CALLBACK_IO = 1;
    public final static int THREAD_CALLBACK_MEMORYSTICK = 2;
    public final static int THREAD_CALLBACK_MEMORYSTICK_FAT = 3;
    public final static int THREAD_CALLBACK_POWER = 4;
    public final static int THREAD_CALLBACK_EXIT = 5;
    public final static int THREAD_CALLBACK_USER_DEFINED = 6;
    public final static int THREAD_CALLBACK_SIZE = 7;
    private RegisteredCallbacks[] registeredCallbacks;
    public Queue<Callback> pendingCallbacks = new LinkedList<Callback>();
    // Used by sceKernelExtendThreadStack
    private SysMemInfo extendedStackSysMemInfo;

    public static class RegisteredCallbacks {
    	private int type;
    	private List<SceKernelCallbackInfo> callbacks;
    	private List<SceKernelCallbackInfo> readyCallbacks;
    	// THREAD_CALLBACK_MEMORYSTICK and THREAD_CALLBACK_MEMORYSTICK_FAT have
    	// a maximum of 32 registered callbacks each.
    	// Don't know yet for the other types, assuming also 32.
    	private int maxNumberOfCallbacks = 32;
    	// Registering a new callback overwrites the previous one.
    	private boolean registerOnlyLastCallback = false;

    	public RegisteredCallbacks(int type) {
    		this.type = type;
    		callbacks = new LinkedList<SceKernelCallbackInfo>();
    		readyCallbacks = new LinkedList<SceKernelCallbackInfo>();
    	}

		public boolean hasCallbacks() {
			return !callbacks.isEmpty();
		}

		public SceKernelCallbackInfo getCallbackByUid(int cbid) {
    		for (SceKernelCallbackInfo callback : callbacks) {
    			if (callback.uid == cbid) {
    				return callback;
    			}
    		}

    		return null;
		}

		public boolean hasCallback(int cbid) {
			return getCallbackByUid(cbid) != null;
    	}

		public boolean hasCallback(SceKernelCallbackInfo callback) {
			return callbacks.contains(callback);
		}

		public boolean addCallback(SceKernelCallbackInfo callback) {
			if (hasCallback(callback)) {
				return true;
			}

			if (getNumberOfCallbacks() >= maxNumberOfCallbacks) {
				return false;
			}

			if (registerOnlyLastCallback) {
				callbacks.clear();
			}

			callbacks.add(callback);

			return true;
		}

    	public void setCallbackReady(SceKernelCallbackInfo callback) {
    		if (hasCallback(callback) && !isCallbackReady(callback)) {
				readyCallbacks.add(callback);
    		}
    	}

    	public boolean isCallbackReady(SceKernelCallbackInfo callback) {
    		return readyCallbacks.contains(callback);
    	}

    	public SceKernelCallbackInfo removeCallback(SceKernelCallbackInfo callback) {
    		if (!callbacks.remove(callback)) {
    			return null;
    		}

    		readyCallbacks.remove(callback);

    		return callback;
    	}

    	public SceKernelCallbackInfo getNextReadyCallback() {
    		if (readyCallbacks.isEmpty()) {
    			return null;
    		}

    		return readyCallbacks.remove(0);
    	}

    	public int getNumberOfCallbacks() {
    		return callbacks.size();
    	}

    	public SceKernelCallbackInfo getCallbackByIndex(int index) {
    		return callbacks.get(index);
    	}

		public void setRegisterOnlyLastCallback() {
			registerOnlyLastCallback = true;
		}

		@Override
		public String toString() {
			return String.format("RegisteredCallbacks[type %d, count %d, ready %d]", type, callbacks.size(), readyCallbacks.size());
		}
    }

    public SceKernelThreadInfo(String name, int entry_addr, int initPriority, int stackSize, int attr) {
        if (stackSize < 512) {
            // 512 byte min. (required for interrupts)
            stackSize = 512;
        } else {
            // 256 byte size alignment.
            stackSize = (stackSize + 0xFF) & ~0xFF;
        }

        this.name = name;
        this.entry_addr = entry_addr;
        this.initPriority = initPriority;
        this.stackSize = stackSize;
        this.attr = attr;
        uid = SceUidManager.getNewUid("ThreadMan-thread");
        // Setup the stack.
    	stackSysMemInfo = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.USER_PARTITION_ID, String.format("ThreadMan-Stack-0x%x-%s", uid, name), SysMemUserForUser.PSP_SMEM_High, stackSize, 0);
    	if (stackSysMemInfo == null) {
    		stackAddr = 0;
    	} else {
    		stackAddr = stackSysMemInfo.addr;
    	}

        // Inherit gpReg.
        gpReg_addr = Emulator.getProcessor().cpu.gpr[_gp];
        // Inherit context.
        cpuContext = new CpuState(Emulator.getProcessor().cpu);
        wait = new ThreadWaitInfo();
        reset();
    }

    public void reset() {
        status = PSP_THREAD_STOPPED;

        int k0 = stackAddr + stackSize - 0x100; // setup k0
        Memory mem = Memory.getInstance();
        if (stackAddr != 0 && stackSize > 0) {
            // set stack to 0xFF
            if ((attr & PSP_THREAD_ATTR_NO_FILLSTACK) != PSP_THREAD_ATTR_NO_FILLSTACK) {
                mem.memset(stackAddr, (byte) 0xFF, stackSize);
            }

            // setup k0
            mem.memset(k0, (byte) 0x0, 0x100);
            mem.write32(k0 + 0xc0, stackAddr);
            mem.write32(k0 + 0xca, uid);
            mem.write32(k0 + 0xf8, 0xffffffff);
            mem.write32(k0 + 0xfc, 0xffffffff);

            mem.write32(stackAddr, uid);
        }

        currentPriority = initPriority;
        waitType = PSP_WAIT_NONE;
        waitId = 0;
        wakeupCount = 0;
        exitStatus = ERROR_KERNEL_THREAD_ALREADY_DORMANT;  // Threads start with DORMANT and not NOT_DORMANT (tested and checked).
        exitStatusAddr = null;
        runClocks = 0;
        intrPreemptCount = 0;
        threadPreemptCount = 0;
        releaseCount = 0;
        notifyCallback = 0;

        // Thread specific registers
        cpuContext.pc = entry_addr;
        cpuContext.npc = entry_addr; // + 4;

        // Reset all the registers to DEADBEEF value
        for (int i = _ra; i > _zr; i--) {
        	cpuContext.gpr[i] = 0xDEADBEEF;
        }
        cpuContext.gpr[Common._k0] = 0;
        cpuContext.gpr[Common._k1] = 0;
        float nanValue = Float.intBitsToFloat(0x7F800001);
        for (int i = Common._f31; i >= Common._f0; i--) {
        	cpuContext.fpr[i] = nanValue;
        }
        cpuContext.hilo = 0xDEADBEEFDEADBEEFL;

        // sp, 512 byte padding at the top for user data, this will get re-jigged when we call start thread
        cpuContext.gpr[_sp] = stackAddr + stackSize - 512;
        cpuContext.gpr[_k0] = k0;

        // We'll hook "jr $ra" where $ra == address of HLE syscall hleKernelExitThread
        // when the thread is exiting
        cpuContext.gpr[_ra] = jpcsp.HLE.modules150.ThreadManForUser.THREAD_EXIT_HANDLER_ADDRESS; // $ra

        doDelete = false;
        doCallbacks = false;

        registeredCallbacks = new RegisteredCallbacks[THREAD_CALLBACK_SIZE];
        for (int i = 0; i < registeredCallbacks.length; i++) {
        	registeredCallbacks[i] = new RegisteredCallbacks(i);
        }
    	// The UMD callback registers only the last callback.
        registeredCallbacks[THREAD_CALLBACK_UMD].setRegisterOnlyLastCallback();
    }

    public void saveContext() {
        cpuContext = Emulator.getProcessor().cpu;
    }

    public void restoreContext() {
        // Assuming context switching only happens on syscall,
        // we always execute npc after a syscall,
        // so we can set pc = npc regardless of cop0.status.bd.
        cpuContext.pc = cpuContext.npc;

        Emulator.getProcessor().setCpu(cpuContext);
        RuntimeContext.update();
    }

    /** For use in the scheduler */
    @Override
    public int compare(SceKernelThreadInfo o1, SceKernelThreadInfo o2) {
        return o1.currentPriority - o2.currentPriority;
    }

    private int getPSPWaitType() {
        if (waitType >= 0x100) {
            // A blocked thread (e.g. a thread blocked due to audio output or
            // wait for vblank or sceCtrl sample reading) is implemented like
            // a "wait for Event Flag". This is the closest implementation to a real PSP,
            // as event flags are usually used by a PSP to implement these wait
            // functions.
            // Jpcsp internal wait types are best matched to PSP_WAIT_EVENTFLAG.
            return PSP_WAIT_EVENTFLAG;
        }
        return waitType;
    }

	@Override
	protected void write() {
		super.write();
		writeStringNZ(32, name);
		write32(attr);
		write32(status);
		write32(entry_addr);
		write32(stackAddr);
		write32(stackSize);
		write32(gpReg_addr);
		write32(initPriority);
		write32(currentPriority);
		write32(getPSPWaitType());
		write32(waitId);
		write32(wakeupCount);
		write32(exitStatus);
		write64(runClocks);
		write32(intrPreemptCount);
		write32(threadPreemptCount);
		write32(releaseCount);
	}

    // SceKernelThreadRunStatus.
    // Represents a smaller subset of SceKernelThreadInfo containing only the most volatile parts
    // of the thread (mostly used for debugging).
    public void writeRunStatus(Memory mem, int address) {
    	start(mem, address);
    	super.write();
    	write32(status);
    	write32(currentPriority);
    	write32(waitType);
    	write32(waitId);
    	write32(wakeupCount);
    	write64(runClocks);
    	write32(intrPreemptCount);
    	write32(threadPreemptCount);
    	write32(releaseCount);
    }

    public void setSystemStack(int stackAddr, int stackSize) {
    	freeStack();
    	this.stackAddr = stackAddr;
    	this.stackSize = stackSize;
    }

    public void freeStack() {
        if (stackSysMemInfo != null) {
            Modules.SysMemUserForUserModule.free(stackSysMemInfo);
            stackSysMemInfo = null;
            stackAddr = 0;
        }
        freeExtendedStack();
    }

    public void freeExtendedStack() {
        if (extendedStackSysMemInfo != null) {
        	Modules.SysMemUserForUserModule.free(extendedStackSysMemInfo);
        	extendedStackSysMemInfo = null;
        }
    }

    public int extendStack(int size) {
    	extendedStackSysMemInfo = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.USER_PARTITION_ID, String.format("ThreadMan-ExtendedStack-0x%x-%s", uid, name), SysMemUserForUser.PSP_SMEM_High, size, 0);

    	return extendedStackSysMemInfo.addr;
    }

    public int getStackAddr() {
    	if (extendedStackSysMemInfo != null) {
        	return extendedStackSysMemInfo.addr;
    	}

		return stackAddr;
    }

    public static String getStatusName(int status) {
        StringBuilder s = new StringBuilder();

        // A thread status is a bitfield so it could be in multiple states
        if ((status & PSP_THREAD_RUNNING) == PSP_THREAD_RUNNING) {
            s.append(" | PSP_THREAD_RUNNING");
        }

        if ((status & PSP_THREAD_READY) == PSP_THREAD_READY) {
            s.append(" | PSP_THREAD_READY");
        }

        if ((status & PSP_THREAD_WAITING) == PSP_THREAD_WAITING) {
            s.append(" | PSP_THREAD_WAITING");
        }

        if ((status & PSP_THREAD_SUSPEND) == PSP_THREAD_SUSPEND) {
            s.append(" | PSP_THREAD_SUSPEND");
        }

        if ((status & PSP_THREAD_STOPPED) == PSP_THREAD_STOPPED) {
            s.append(" | PSP_THREAD_STOPPED");
        }

        if ((status & PSP_THREAD_KILLED) == PSP_THREAD_KILLED) {
            s.append(" | PSP_THREAD_KILLED");
        }

        // Strip off leading " | "
        if (s.length() > 0) {
            s.delete(0, 3);
        } else {
            s.append("UNKNOWN");
        }

        return s.toString();
    }

    public String getStatusName() {
        return getStatusName(status);
    }

    public static String getWaitName(int waitType, ThreadWaitInfo wait, int status) {
        StringBuilder s = new StringBuilder();

        switch (waitType) {
        	case PSP_WAIT_NONE:
        		s.append(String.format("None"));
        		break;
        	case PSP_WAIT_SLEEP:
        		s.append(String.format("Sleep"));
        		break;
        	case PSP_WAIT_DELAY:
        		s.append(String.format("Delay"));
        		break;
        	case PSP_WAIT_THREAD_END:
        		s.append(String.format("ThreadEnd (0x%04X)", wait.ThreadEnd_id));
        		break;
        	case PSP_WAIT_EVENTFLAG:
        		s.append(String.format("EventFlag (0x%04X)", wait.EventFlag_id));
        		break;
        	case PSP_WAIT_SEMA:
        		s.append(String.format("Semaphore (0x%04X)", wait.Semaphore_id));
        		break;
        	case PSP_WAIT_MUTEX:
        		s.append(String.format("Mutex (0x%04X)", wait.Mutex_id));
        		break;
        	case PSP_WAIT_LWMUTEX:
        		s.append(String.format("LwMutex (0x%04X)", wait.LwMutex_id));
        		break;
        	case PSP_WAIT_MBX:
        		s.append(String.format("Mbx (0x%04X)", wait.Mbx_id));
        		break;
        	case PSP_WAIT_VPL:
        		s.append(String.format("Vpl (0x%04X)", wait.Vpl_id));
        		break;
        	case PSP_WAIT_FPL:
        		s.append(String.format("Fpl (0x%04X)", wait.Fpl_id));
        		break;
        	case PSP_WAIT_MSGPIPE:
        		s.append(String.format("MsgPipe (0x%04X)", wait.MsgPipe_id));
        		break;
        	case PSP_WAIT_EVENTHANDLER:
        		s.append(String.format("EventHandler"));
        		break;
        	case PSP_WAIT_CALLBACK_DELETE:
        		s.append(String.format("CallBackDelete"));
        		break;
        	case JPCSP_WAIT_IO:
        		s.append(String.format("Io (0x%04X)", wait.Io_id));
        		break;
        	case JPCSP_WAIT_UMD:
        		s.append(String.format("Umd (0x%02X)", wait.wantedUmdStat));
        		break;
        	case JPCSP_WAIT_BLOCKED:
        		s.append(String.format("Blocked"));
        		break;
        	default:
        		s.append(String.format("Unknown waitType=%d", waitType));
        		break;
        }

        if ((status & PSP_THREAD_WAITING) != 0) {
            if (wait.forever) {
                s.append(" (forever)");
            } else {
                int restDelay = (int) (wait.microTimeTimeout - Emulator.getClock().microTime());
                if (restDelay < 0) {
                    restDelay = 0;
                }
                s.append(String.format(" (delay %d us, rest %d us)", wait.micros, restDelay));
            }
        }

        return s.toString();
    }

    public String getWaitName() {
        return getWaitName(waitType, wait, status);
    }

    public boolean isSuspended() {
        return (status & PSP_THREAD_SUSPEND) != 0;
    }

    public boolean isWaiting() {
        return (status & PSP_THREAD_WAITING) != 0;
    }

    public boolean isWaitingForType(int waitType) {
    	// Check if the thread is still in a WAITING state, but not WAITING_SUSPEND
    	if (!isWaiting() || isSuspended()) {
    		return false;
    	}
    	return this.waitType == waitType;
    }

    public boolean isRunning() {
        return (status & PSP_THREAD_RUNNING) != 0;
    }

    public boolean isReady() {
        return (status & PSP_THREAD_READY) != 0;
    }

    public boolean isStopped() {
        return (status & PSP_THREAD_STOPPED) != 0;
    }

    public static boolean isKernelMode(int attr) {
        return (attr & PSP_THREAD_ATTR_KERNEL) != 0;
    }

    public static boolean isUserMode(int attr) {
        return (attr & PSP_THREAD_ATTR_USER) != 0;
    }

    public boolean isKernelMode() {
    	return isKernelMode(attr);
    }

    public boolean isUserMode() {
    	return isUserMode(attr);
    }

    public RegisteredCallbacks getRegisteredCallbacks(int type) {
    	return registeredCallbacks[type];
    }

    public void setExitStatus(int exitStatus) {
    	this.exitStatus = exitStatus;
    	if (exitStatusAddr != null) {
    		exitStatusAddr.setValue(exitStatus);
    	}
    }

    @Override
    public String toString() {
    	return String.format("%s(uid=0x%X, Status=%s, Wait=%s, doCallbacks=%b)", name, uid, getStatusName(), getWaitName(), doCallbacks);
    }
}