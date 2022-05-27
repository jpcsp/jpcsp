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
package jpcsp.Allegrex.compiler;

import static jpcsp.HLE.kernel.managers.IntrManager.EXCEP_BP;
import static jpcsp.HLE.kernel.managers.IntrManager.EXCEP_INT;
import static jpcsp.HLE.kernel.managers.IntrManager.EXCEP_SYS;
import static jpcsp.util.Utilities.hasFlag;
import static jpcsp.util.Utilities.notHasFlag;
import static jpcsp.util.Utilities.readCompleteFile;
import static jpcsp.util.Utilities.setFlag;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Processor;
import jpcsp.Allegrex.Cp0State;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.modules.reboot;
import jpcsp.mediaengine.MEProcessor;
import jpcsp.mediaengine.METhread;
import jpcsp.memory.DebuggerMemory;
import jpcsp.memory.mmio.MMIO;
import jpcsp.memory.mmio.MMIOHandlerInterruptMan;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

/**
 * @author gid15
 *
 */
public class RuntimeContextLLE {
	public static Logger log = RuntimeContext.log;
	private static final int STATE_VERSION = 0;
	private static boolean isLLEActive;
	private static Memory mmio;
	public volatile static int pendingInterruptIPbitsMain;
	public volatile static int pendingInterruptIPbitsME;
	private static int firmwareVersion = -1;
	private final static List<IAction> exitActions = new LinkedList<IAction>();

	public static boolean isLLEActive() {
		return isLLEActive;
	}

	public static void enableLLE() {
		isLLEActive = true;
	}

	public static void start() {
		if (reboot.enableReboot) {
			isLLEActive = true;
			RuntimeContext.javaThreadScheduling = false;
		} else {
			RuntimeContext.javaThreadScheduling = true;
		}

		if (!isLLEActive()) {
			return;
		}

		createMMIO();
	}

	public static void run() {
		if (!isLLEActive()) {
			return;
		}

		MEProcessor.getInstance().sync();
	}

	public static void exit() {
		if (!isLLEActive()) {
			return;
		}

		for (IAction action : exitActions) {
			action.execute();
		}
	}

	public static void registerExitAction(IAction action) {
		if (!exitActions.contains(action)) {
			exitActions.add(action);
		}
	}

	public static void reset() {
		if (mmio != null) {
			mmio.reset();
			mmio.Initialise();
		}

		// Force a reload of the cached firmware version
		firmwareVersion = -1;
	}

	public static void createMMIO() {
		if (mmio == null) {
			Memory mem = Emulator.getMemory();

			if (mem instanceof DebuggerMemory) {
				mmio = new DebuggerMemory(new MMIO(((DebuggerMemory) mem).getDebuggedMemory()));
			} else {
				mmio = new MMIO(mem);
			}

			if (mmio.allocate()) {
				mmio.Initialise();
			} else {
				mmio = null;
			}
			markMMIO();
		}
	}

    private static void markMMIO() {
    	Compiler compiler = Compiler.getInstance();
    	compiler.addMMIORange(MemoryMap.START_KERNEL, 0x800000);
    	compiler.addMMIORange(0xBFC00C00, 0x240);

    	int firmwareVersion = getFirmwareVersion();
    	if (firmwareVersion >= 280 && firmwareVersion <= 303) {
    		compiler.addMMIORange(MemoryMap.START_USERSPACE, 0x30000);
    	}
    }

	public static boolean hasMMIO() {
		return mmio != null;
	}

	public static Memory getMMIO() {
		return mmio;
	}

	public static void triggerInterrupt(Processor processor, int interruptNumber) {
		if (!isLLEActive()) {
			Managers.intr.triggerInterrupt(interruptNumber);
			return;
		}

		MMIOHandlerInterruptMan interruptMan = MMIOHandlerInterruptMan.getInstance(processor);
		if (!interruptMan.hasInterruptTriggered(interruptNumber)) {
			if (log.isDebugEnabled()) {
				log.debug(String.format("triggerInterrupt 0x%X(%s)", interruptNumber, IntrManager.getInterruptName(interruptNumber)));
			}

			interruptMan.triggerInterrupt(interruptNumber);
		}
	}

	public static void clearInterrupt(Processor processor, int interruptNumber) {
		if (!isLLEActive()) {
			return;
		}

		MMIOHandlerInterruptMan interruptMan = MMIOHandlerInterruptMan.getInstance(processor);
		if (interruptMan.hasInterruptTriggered(interruptNumber)) {
			if (log.isDebugEnabled()) {
				log.debug(String.format("clearInterrupt 0x%X(%s)", interruptNumber, IntrManager.getInterruptName(interruptNumber)));
			}

			interruptMan.clearInterrupt(interruptNumber);
		}
	}

	/*
	 * synchronized method as it can be called from different threads (e.g. CoreThreadMMIO)
	 */
	public static synchronized void triggerInterruptException(Processor processor, int IPbits) {
		if (!isLLEActive()) {
			return;
		}

		if (processor.cp0.isMainCpu()) {
			pendingInterruptIPbitsMain |= IPbits;

			RuntimeContext.onLLEInterrupt();

			if (log.isDebugEnabled()) {
				log.debug(String.format("triggerInterruptException IPbits=0x%X, pendingInterruptIPbitsMain=0x%X", IPbits, pendingInterruptIPbitsMain));
			}
		} else if (processor.cp0.isMediaEngineCpu()) {
			getMediaEngineProcessor().triggerException(IPbits);

			if (log.isDebugEnabled()) {
				log.debug(String.format("triggerInterruptException IPbits=0x%X, pendingInterruptIPbitsME=0x%X", IPbits, pendingInterruptIPbitsME));
			}
		}
	}

	public static int triggerSyscallException(Processor processor, int syscallCode, boolean inDelaySlot) {
		processor.cp0.setSyscallCode(syscallCode << 2);
		int pc = triggerException(processor, EXCEP_SYS, inDelaySlot);

		if (log.isDebugEnabled()) {
			log.debug(String.format("Calling exception handler for Syscall at 0x%08X, epc=0x%08X", pc, processor.cp0.getEpc()));
		}

		return pc;
	}

	public static int triggerBreakException(Processor processor, boolean inDelaySlot) {
		int pc = triggerException(processor, EXCEP_BP, inDelaySlot);

		if (log.isDebugEnabled()) {
			log.debug(String.format("Calling exception handler for Break at 0x%08X, epc=0x%08X", pc, processor.cp0.getEpc()));
		}

		return pc;
	}

	public static boolean isMediaEngineCpu() {
		if (!isLLEActive()) {
			return false;
		}
		return METhread.isMediaEngine(Thread.currentThread());
	}

	public static boolean isMainCpu() {
		if (!isLLEActive()) {
			return true;
		}
		return !isMediaEngineCpu();
	}

	public static Processor getMainProcessor() {
		return Emulator.getProcessor();
	}

	public static MEProcessor getMediaEngineProcessor() {
		return MEProcessor.getInstance();
	}

	public static Processor getProcessor() {
		if (isMediaEngineCpu()) {
			return getMediaEngineProcessor();
		}
		return getMainProcessor();
	}

	public static int triggerException(Processor processor, int exceptionNumber, boolean inDelaySlot) {
		return prepareExceptionHandlerCall(processor, exceptionNumber, inDelaySlot);
	}

	/*
	 * synchronized method as it can be called from different threads (e.g. CoreThreadMMIO)
	 */
	public static synchronized void clearInterruptException(Processor processor, int IPbits) {
		if (!isLLEActive()) {
			return;
		}

		if (processor.cp0.isMainCpu()) {
			pendingInterruptIPbitsMain &= ~IPbits;
		} else if (processor.cp0.isMediaEngineCpu()) {
			pendingInterruptIPbitsME &= ~IPbits;
		}
	}

	private static boolean isInterruptExceptionAllowed(Processor processor, int IPbits) {
		if (IPbits == 0) {
			log.debug("IPbits == 0");
			return false;
		}

		if (processor.isInterruptsDisabled()) {
			return false;
		}

		int status = processor.cp0.getStatus();
		if (log.isDebugEnabled()) {
			log.debug(String.format("cp0 Status=0x%X", status));
		}

		// Is the processor already in an exception state?
		if (hasFlag(status, Cp0State.STATUS_EXL)) {
			return false;
		}

		// Is the interrupt masked?
		if (notHasFlag(status, Cp0State.STATUS_IE) || ((IPbits << 8) & status) == 0) {
			return false;
		}

		return true;
	}

	private static int prepareExceptionHandlerCall(Processor processor, int exceptionNumber, boolean inDelaySlot) {
		// Set the exception number and BD flag
		int cause = processor.cp0.getCause();
		cause = (cause & 0xFFFFFF00) | (exceptionNumber << 2);
		if (inDelaySlot) {
			cause |= 0x80000000; // Set BD flag (Branch Delay Slot)
		} else {
			cause &= ~0x80000000; // Clear BD flag (Branch Delay Slot)
		}
		processor.cp0.setCause(cause);

		int epc = processor.cpu.pc;

		if (inDelaySlot) {
			epc -= 4; // The EPC is set to the instruction having the delay slot
		}

		// Set the EPC
		processor.cp0.setEpc(epc);

		int pc;
		// BEV flag set?
		if (hasFlag(processor.cp0.getStatus(), Cp0State.STATUS_BEV)) {
			pc = 0xBFC00200;
		} else {
			pc = processor.cp0.getEbase();
		}

		// Set the EXL bit
		int status = processor.cp0.getStatus();
		status = setFlag(status, Cp0State.STATUS_EXL); // Set EXL bit
		processor.cp0.setStatus(status);

		return pc;
	}

	/*
	 * synchronized method as it is modifying pendingInterruptIPbitsXX which can be updated from different threads
	 */
	public static synchronized int checkPendingInterruptException(int returnAddress) {
		Processor processor = getProcessor();
		int IPbits = 0;
		if (processor.cp0.isMainCpu()) {
			IPbits = pendingInterruptIPbitsMain;
		} else if (processor.cp0.isMediaEngineCpu()) {
			IPbits = pendingInterruptIPbitsME;
		}

		if (isInterruptExceptionAllowed(processor, IPbits)) {
			int cause = processor.cp0.getCause();
			cause |= (IPbits << 8);
			if (processor.cp0.isMainCpu()) {
				pendingInterruptIPbitsMain = 0;
			} else if (processor.cp0.isMediaEngineCpu()) {
				pendingInterruptIPbitsME = 0;
			}
			processor.cp0.setCause(cause);

			// The compiler is only calling this function when
			// we are not in a delay slot
			int pc = prepareExceptionHandlerCall(processor, EXCEP_INT, false);

			if (log.isDebugEnabled()) {
				log.debug(String.format("Calling exception handler for %s at 0x%08X, epc=0x%08X, cause=0x%X", MMIOHandlerInterruptMan.getInstance(processor).toStringInterruptTriggered(), pc, processor.cp0.getEpc(), processor.cp0.getCause()));
			}

			return pc;
		}

		return returnAddress;
	}

	/*
	 * synchronized method as it is modifying pendingInterruptIPbitsXX which can be updated from different threads
	 */
	public static synchronized void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		pendingInterruptIPbitsMain = stream.readInt();
		pendingInterruptIPbitsME = stream.readInt();

		// Force a reload of the cached firmware version
		firmwareVersion = -1;
	}

	/*
	 * synchronized method as it is reading pendingInterruptIPbitsXX which can be updated from different threads
	 */
	public static synchronized void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(pendingInterruptIPbitsMain);
		stream.writeInt(pendingInterruptIPbitsME);
	}

	public static void onEmulatorLoad() {
		// Force a reload of the cached firmware version
		firmwareVersion = -1;
	}

	public static int getFirmwareVersion() {
		if (firmwareVersion < 0) {
			firmwareVersion = 0;
	    	String versionFileName = "flash0:/vsh/etc/version.txt";
	    	byte[] content = readCompleteFile(versionFileName);
	    	if (content != null && content.length > 0) {
	    		String contentString = new String(content);
	    		int index1 = contentString.indexOf(':');
	    		if (index1 >= 0) {
	    			int index2 = contentString.indexOf(':', index1 + 1);
	    			if (index2 >= 0) {
	    				String versionString = contentString.substring(index1 + 1, index2);
	    				versionString = versionString.replace(".", "");
	    				firmwareVersion = Integer.parseInt(versionString);
	    				if (log.isDebugEnabled()) {
	    					log.debug(String.format("firmwareVersion=%d", firmwareVersion));
	    				}
	    			}
	    		}
	    	} else if (Emulator.getInstance().isPspOfficialUpdater()) {
	    		int updateVersion = Emulator.getInstance().getPspOfficialUpdaterVersion();
	    		if (updateVersion >= 0) {
	    			firmwareVersion = updateVersion;
	    		}
	    	}
		}

    	return firmwareVersion;
	}
}
