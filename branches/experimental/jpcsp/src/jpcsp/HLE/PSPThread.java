package jpcsp.HLE;

import jpcsp.Memory;
import jpcsp.Processor;

public class PSPThread extends Thread {
	
    static final int PSP_THREAD_ATTR_USER = 0x80000000;
    static final int PSP_THREAD_ATTR_USBWLAN = 0xa0000000;
    static final int PSP_THREAD_ATTR_VSH = 0xc0000000;
    static final int PSP_THREAD_ATTR_KERNEL = 0x00001000; // TODO are module/thread attr interchangeable?
    static final int PSP_THREAD_ATTR_VFPU = 0x00004000;
    static final int PSP_THREAD_ATTR_SCRATCH_SRAM = 0x00008000;
    static final int PSP_THREAD_ATTR_NO_FILLSTACK = 0x00100000; // Disables filling the stack with 0xFF on creation.
    static final int PSP_THREAD_ATTR_CLEAR_STACK = 0x00200000; // Clear the stack when the thread is deleted.	
	
    enum PspThreadStatus {
        PSP_THREAD_RUNNING(1), PSP_THREAD_READY(2),
        PSP_THREAD_WAITING(4), PSP_THREAD_SUSPEND(8),
        PSP_THREAD_STOPPED(16), PSP_THREAD_KILLED(32);
        private int value;
        private PspThreadStatus(int value) {
            this.value = value;
        }
        public int getValue() {
            return value;
        }
    }
    
    protected PspThreadStatus status = PspThreadStatus.PSP_THREAD_READY;
	
	protected int entry_addr;
	protected int stack_addr;
	protected int stackSize;
	protected int initPriority = 0;
	protected int currentPriority = 0;
	protected int attr;
    protected int gpReg_addr = 0; // ?
    protected int waitType = 0; // ?
    protected int waitId = 0; // ?
    protected int wakeupCount = 0;	
    protected int exitStatus = 0x800201a4; // thread is not DORMANT
    protected int runClocks = 0;
    protected int intrPreemptCount = 0;
    protected int threadPreemptCount = 0;
    protected int releaseCount = 0;
    protected boolean do_callbacks = false;
    
    protected int delay = 0;


	public Processor processor;
	protected int uid = 0;
	
    // stack allocation
    private static int stackAllocated;	
	
    private int mallocStack(int size) {
        return pspSysMem.get_instance().malloc(2, pspSysMem.PSP_SMEM_High, size, 0);
    }
    
    private void memset(int address, byte c, int length) {
        Memory mem = Memory.get_instance();
        for (int i = 0; i < length; i++) {
            mem.write8(address + i, c);
        }
    }    

	public PSPThread(Processor processor, String name, int entry_addr, int initPriority, int stackSize, int attr, int option_addr) {
		super(name);
		uid = SceUIDMan.get_instance().getNewUid("ThreadMan-thread");
		this.entry_addr = entry_addr;
		this.processor = processor;
		processor.pc = entry_addr;
		processor.npc = entry_addr;
		
		this.initPriority = initPriority;
		this.stackSize = stackSize;
		this.currentPriority = initPriority;
		this.attr = attr;
		
		// setPriority( initPriority / 3 );

        // TODO use option_addr/SceKernelThreadOptParam?
        if (option_addr != 0)
            System.out.println("sceKernelCreateThread unhandled SceKernelThreadOptParam");		

        System.out.println("sceKernelCreateThread SceUID=" + Integer.toHexString(uid)
                + " name:'" + getName() + "' PC=" + Integer.toHexString(processor.pc)
                + " initPriority=0x" + Integer.toHexString(initPriority) + " attr:" + Integer.toHexString(attr));

        Thread currentThread = Thread.currentThread();

        if (currentThread instanceof PSPThread) {
        	PSPThread currentPSPThread = (PSPThread) currentThread;

	        // Inherit kernel mode if user mode bit is not set
	        if ((currentPSPThread.attr & PSP_THREAD_ATTR_KERNEL) == PSP_THREAD_ATTR_KERNEL &&
	            (attr & PSP_THREAD_ATTR_USER) != PSP_THREAD_ATTR_USER) {
	            System.out.println("sceKernelCreateThread inheriting kernel mode");
	            attr |= PSP_THREAD_ATTR_KERNEL;
	        }
	        // Inherit user mode
	        if ((currentPSPThread.attr & PSP_THREAD_ATTR_USER) == PSP_THREAD_ATTR_USER) {
	            if ((attr & PSP_THREAD_ATTR_USER) != PSP_THREAD_ATTR_USER)
	                System.out.println("sceKernelCreateThread inheriting user mode");
	            attr |= PSP_THREAD_ATTR_USER;
	            // Always remove kernel mode bit
	            attr &= ~PSP_THREAD_ATTR_KERNEL;
	        }
        }
        
		
        status = PspThreadStatus.PSP_THREAD_SUSPEND;
				
        stack_addr = mallocStack(stackSize); // TODO MemoryMan.mallocFromEnd(stackSize);
        if ((attr & PSP_THREAD_ATTR_NO_FILLSTACK) != PSP_THREAD_ATTR_NO_FILLSTACK)
            memset(stack_addr - stackSize + 1, (byte)0xFF, stackSize);
        
		processor.gpr[29] = stack_addr;		

        processor.gpr[31] = 0; // ra	
	}
		
	@Override
	public synchronized void start() {
		status = PspThreadStatus.PSP_THREAD_READY;
		super.start();
	}

	@Override
	public void run() {
		
		while( true ) {
		
			while( status == PspThreadStatus.PSP_THREAD_READY || status == PspThreadStatus.PSP_THREAD_RUNNING ) {
				status = PspThreadStatus.PSP_THREAD_RUNNING;

				processor.step();
													
				if (processor.pc == 0 && processor.gpr[31] == 0) {
	                System.out.println("Thread exit detected SceUID=" + Integer.toHexString(uid)
	                        + " name:'" + getName() + "' return:" + processor.gpr[2]);
	                exitStatus = processor.gpr[2]; // v0
	                status = PspThreadStatus.PSP_THREAD_KILLED;
				}				
			}
			
			if (status == PspThreadStatus.PSP_THREAD_KILLED || getName().equals("root")) {
				// RIP
				return;
			}
			
			if (delay > 0) {
				
				try {
					sleep(delay);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				status = PspThreadStatus.PSP_THREAD_READY;
				
			}
			
			while( status != PspThreadStatus.PSP_THREAD_READY && status != PspThreadStatus.PSP_THREAD_RUNNING ) {
	
				// idle loop
				try {
					sleep( 150 );
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public void exit() {
		status = PspThreadStatus.PSP_THREAD_STOPPED;
	}
	
	public static PSPThread currentPSPThread() {
		return (PSPThread) Thread.currentThread();
	}

}
