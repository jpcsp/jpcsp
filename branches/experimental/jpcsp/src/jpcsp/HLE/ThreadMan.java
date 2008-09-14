package jpcsp.HLE;

import static jpcsp.util.Utilities.readStringZ;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import jpcsp.GeneralJpcspException;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Processor;
import jpcsp.HLE.PSPThread.PspThreadStatus;
import jpcsp.javassist.FastProcessor;

public class ThreadMan {
	
	private Map<Integer, PSPThread> threads =  new HashMap<Integer, PSPThread> ();
	PSPThread rootThread = null;

	static class SingletonHolder {
		static ThreadMan instance = new ThreadMan();
	}

	public static ThreadMan get_instance() {
		return SingletonHolder.instance;
	}
	
	public Processor newProcessor() {
		return new FastProcessor();
	}

	public void Initialise(int entry_addr, int gp_addr, int attr) {
		
		Processor cpu = newProcessor();
		
        cpu.gpr[4] = 0; //a0
        cpu.gpr[5] = entry_addr; // argumentsPointer a1 reg
        cpu.gpr[6] = 0; //a2
        cpu.gpr[26] = 0x09F00000; //k0
        cpu.gpr[27] = 0; //k1 should probably be 0
        cpu.gpr[28] = gp_addr; //gp reg    gp register should get the GlobalPointer!!!
        cpu.gpr[29] = 0x09F00000; //sp
        cpu.gpr[31] = 0x08000004; //ra, should this be 0?
        // All other registers are uninitialised/random values

		rootThread = new PSPThread(cpu, "root", entry_addr, 0x20, 0x40000, attr, -1);
        // Set user mode bit if kernel mode bit is not present
        if ((rootThread.attr & PSPThread.PSP_THREAD_ATTR_KERNEL) != PSPThread.PSP_THREAD_ATTR_KERNEL) {
        	rootThread.attr |= PSPThread.PSP_THREAD_ATTR_USER;
        }
		threads.put(rootThread.uid, rootThread);
	}

	public void start() {
		psputils.get_instance().Initialise();
		rootThread.start();		
	}
	
	public void pause() {
		for (Iterator<PSPThread> iterator = threads.values().iterator(); iterator.hasNext();) {
			PSPThread thread = iterator.next();
			thread.status = PSPThread.PspThreadStatus.PSP_THREAD_STOPPED;
		}
	}

	public void ThreadMan_sceKernelCreateThread(int name_addr, int entry_addr,
			int initPriority, int stackSize, int attr, int option_addr) {
		String name = readStringZ(Memory.get_instance().mainmemory,
	            (name_addr & 0x3fffffff) - MemoryMap.START_RAM);
		
		Processor parentProcessor = getProcessor();
		
		Processor processor = newProcessor();

		for(int i=0; i<32; i++) {
			processor.gpr[i] = parentProcessor.gpr[i];
		}
		if ((attr & PSPThread.PSP_THREAD_ATTR_VFPU) != 0) {
			for(int i=0; i<32; i++) {
				processor.fpr[i] = parentProcessor.fpr[i];
			}
		}
		processor.hilo = parentProcessor.hilo;

		PSPThread thread = new PSPThread(processor, name, entry_addr, initPriority, stackSize, attr, option_addr);
		threads.put(thread.uid, thread);
		
		getProcessor().gpr[2] = thread.uid;
	}

	public void ThreadMan_sceKernelTerminateThread(int uid)
			throws GeneralJpcspException {
		System.out.println("sceKernelTerminateThread uid:" + uid);
		PSPThread thread = threads.get(uid);
		if (thread != null) {
            System.out.println("sceKernelTerminateThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.getName() + "'");			
			thread.status = PspThreadStatus.PSP_THREAD_STOPPED;
			getProcessor().gpr[2] = 0;
		} else {
			 getProcessor().gpr[2] = 0x80020198; //notfoundthread
		}
	}

	public void ThreadMan_sceKernelDeleteThread(int uid)
			throws GeneralJpcspException {
		System.out.println("sceKernelDeleteThread uid:" + uid);
		PSPThread thread = threads.get(uid);
		if (thread != null) {
            System.out.println("sceKernelDeleteThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.getName() + "'");			
			thread.status = PspThreadStatus.PSP_THREAD_STOPPED;
			SceUIDMan.get_instance().releaseUid(thread.uid, "ThreadMan-thread");
			threads.remove( uid );
			getProcessor().gpr[2] = 0;
		} else {
			getProcessor().gpr[2] = 0x80020198; //notfoundthread
		}
	}

	public void ThreadMan_sceKernelStartThread(int uid, int arglen, int argp)
			throws GeneralJpcspException {
		PSPThread thread = threads.get(uid);
		if (thread != null) {
	        System.out.println("sceKernelStartThread SceUID=" + Integer.toHexString(uid) + " name:'" + thread.getName() + "'");
	        
            // Copy user data to the new thread's stack, since we are not
            // starting the thread immediately, only marking it as ready,
            // the data needs to be saved somewhere safe.
            Memory mem = Memory.get_instance();
            for (int i = 0; i < arglen; i++)
                mem.write8(thread.stack_addr - arglen + i, (byte)mem.read8(argp + i));

            thread.processor.gpr[29] -= arglen; // Adjust sp for size of user data
            //thread.processor.gpr[29] = thread.stack_addr; 
            thread.processor.gpr[4] = arglen; // a0 = a1
            thread.processor.gpr[5] = thread.processor.gpr[29]; // a1 = pointer to copy of data at a2          
	        
			thread.start();
			getProcessor().gpr[2] = 0;
		} else {
			getProcessor().gpr[2] = 0x80020198; //notfoundthread
		}
	}

	public void ThreadMan_sceKernelExitThread(int exitStatus) {
		synchronized (this) {
			PSPThread current_thread = (PSPThread)Thread.currentThread();
			
			if (current_thread != null) {
		        current_thread.exitStatus = exitStatus;
	
		        System.out.println("sceKernelExitThread SceUID=" + Integer.toHexString(current_thread.uid)
		                + " name:'" + current_thread.getName() + "' exitStatus:" + exitStatus);
				
		        current_thread.exit();
				getProcessor().gpr[2] = 0;
			} else {
				getProcessor().gpr[2] = -1;//FIXME
			}
		}
	}

	public void ThreadMan_sceKernelExitDeleteThread(int exitStatus)
			throws GeneralJpcspException {
		synchronized (this) {
			PSPThread current_thread = PSPThread.currentPSPThread();
	        System.out.println("sceKernelExitDeleteThread SceUID=" + Integer.toHexString(current_thread.uid)
	            + " name:'" + current_thread.getName() + "' exitStatus:" + exitStatus);
	
	        // Exit
	        current_thread.status = PspThreadStatus.PSP_THREAD_STOPPED;
	        current_thread.exitStatus = exitStatus;
	
	        // Delete
	        // TODO cleanup thread, example: free the stack, anything else?
	        // MemoryMan.free(thread.stack_addr);
	        
	        threads.remove( current_thread.uid );
	        SceUIDMan.get_instance().releaseUid(current_thread.uid, "ThreadMan-thread");
	
	        current_thread.processor.gpr[2] = 0;
		}
	}

	public void ThreadMan_sceKernelSleepThreadCB() {
		PSPThread currentThread = PSPThread.currentPSPThread();
        System.out.println("sceKernelSleepThreadCB SceUID=" + Integer.toHexString(currentThread.uid) + " name:'" + currentThread.getName() + "'");

		currentThread.status = PspThreadStatus.PSP_THREAD_SUSPEND;

		currentThread.do_callbacks = true;
		currentThread.processor.gpr[2] = 0;
	}

	/** sleep the current thread for a certain number of microseconds */
	public void ThreadMan_sceKernelDelayThread(int microseconds) {
		if (microseconds <=0) return;
		PSPThread currentThread = PSPThread.currentPSPThread();
		currentThread.status = PspThreadStatus.PSP_THREAD_WAITING;
		currentThread.do_callbacks = false;
		currentThread.delay = (microseconds > 0 ? microseconds / 1000 : -1);
		currentThread.processor.gpr[2] = 0;
	}
	
	public Processor getProcessor() {
		return ((PSPThread)Thread.currentThread()).processor;
	}

	public void ThreadMan_sceKernelCreateCallback(int name_addr, int callback_addr, int callback_arg_addr)
			throws GeneralJpcspException {

        String name = readStringZ(Memory.get_instance().mainmemory, (name_addr & 0x3fffffff) - MemoryMap.START_RAM);
        PSPCallback callback = new PSPCallback(name, ((PSPThread)Thread.currentThread()).uid, callback_addr, callback_arg_addr);

        System.out.println("sceKernelCreateCallback SceUID=" + Integer.toHexString(callback.uid)
            + " PC=" + Integer.toHexString(callback.callback_addr) + " name:'" + callback.name + "'");

        getProcessor().gpr[2] = callback.uid;
	}

	public void ThreadMan_sceKernelGetThreadId() {
		getProcessor().gpr[2] = PSPThread.currentPSPThread().uid;
	}

	public void ThreadMan_sceKernelReferThreadStatus(int uid, int a1) {
        //Get the status information for the specified thread
		PSPThread thread = threads.get(uid);
        if (thread == null) {
        	getProcessor().gpr[2] = 0x80020198; //notfoundthread
            return;
        }

        //System.out.println("sceKernelReferThreadStatus SceKernelThreadInfo=" + Integer.toHexString(a1));

        int i, len;
        Memory mem = Memory.get_instance();
        mem.write32(a1, 106); //struct size

        //thread name max 32bytes
        len = thread.getName().length();
        if (len > 31) len = 31;
        for (i=0; i < len; i++)
            mem.write8(a1 +4 +i, (byte)thread.getName().charAt(i));
        mem.write8(a1 +4 +i, (byte)0);

        mem.write32(a1 +36, thread.attr);
        mem.write32(a1 +40, thread.status.getValue());
        mem.write32(a1 +44, thread.entry_addr);
        mem.write32(a1 +48, thread.stack_addr);
        mem.write32(a1 +52, thread.stackSize);
        mem.write32(a1 +56, thread.gpReg_addr);
        mem.write32(a1 +60, thread.initPriority);
        mem.write32(a1 +64, thread.currentPriority);
        mem.write32(a1 +68, thread.waitType);
        mem.write32(a1 +72, thread.waitId);
        mem.write32(a1 +78, thread.wakeupCount);
        mem.write32(a1 +82, thread.exitStatus);
        mem.write64(a1 +86, thread.runClocks);
        mem.write32(a1 +94, thread.intrPreemptCount);
        mem.write32(a1 +98, thread.threadPreemptCount);
        mem.write32(a1 +102, thread.releaseCount);

        getProcessor().gpr[2] = 0;
	}

	/** sleep the current thread */
	public void ThreadMan_sceKernelSleepThread() {
		PSPThread currentThread = PSPThread.currentPSPThread();
        System.out.println("sceKernelSleepThread SceUID=" + Integer.toHexString(currentThread.uid) + " name:'" + currentThread.getName() + "'");
        currentThread.status = PspThreadStatus.PSP_THREAD_SUSPEND;
        currentThread.do_callbacks = true;

        currentThread.processor.gpr[2] = 0;					
	}

}
