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
package jpcsp.HLE.modules;

import jpcsp.NIDMapper;
import jpcsp.Processor;
import jpcsp.Allegrex.Cp0State;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.TPointerFunction;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.interrupts.AbstractInterruptHandler;
import jpcsp.memory.mmio.MMIOHandlerInterruptMan;
import jpcsp.scheduler.Scheduler;
import jpcsp.util.HLEUtilities;

import static jpcsp.HLE.HLEModuleManager.HLESyscallNid;
import static jpcsp.HLE.kernel.managers.IntrManager.EXCEP_INT;
import static jpcsp.HLE.kernel.managers.IntrManager.EXCEP_SYS;
import static jpcsp.HLE.kernel.managers.IntrManager.IP2;
import static jpcsp.memory.mmio.MMIOHandlerInterruptMan.NUMBER_INTERRUPTS;
import static jpcsp.util.Utilities.clearFlag;

import org.apache.log4j.Logger;

public class InterruptManager extends HLEModule {
    public static Logger log = Modules.getLogger("InterruptManager");

	private int hleExceptionHandlerAddr;

	private static class HLEInterruptHandler extends AbstractInterruptHandler {
		private int interruptNumber;

		public HLEInterruptHandler(int interruptNumber) {
			this.interruptNumber = interruptNumber;
		}

		@Override
		protected void executeInterrupt() {
			IntrManager.getInstance().triggerInterrupt(interruptNumber);
		}
	}

	private static class HLEExceptionHandler extends AbstractInterruptHandler {
		private Processor processor;

		public HLEExceptionHandler(Processor processor) {
			this.processor = processor;
		}

		@Override
		protected void executeInterrupt() {
			int cause = processor.cp0.getCause();
			int exceptionNumber = (cause & 0xFF) >> 2;

			if (exceptionNumber == EXCEP_INT) {
				int ipBits = (cause >> 8) & 0xFF;
				if (ipBits == IP2) {
					if (log.isDebugEnabled()) {
						log.debug(String.format("hleExceptionHandler for IP2"));
					}

					MMIOHandlerInterruptMan interruptMan = MMIOHandlerInterruptMan.getInstance(processor);
					for (int interruptNumber = 0; interruptNumber < NUMBER_INTERRUPTS; interruptNumber++) {
						if (interruptMan.hasInterruptTriggered(interruptNumber)) {
							if (log.isDebugEnabled()) {
								log.debug(String.format("hleExceptionHandler for interrupt %s", IntrManager.getInterruptName(interruptNumber)));
							}
							Scheduler.getInstance().addAction(new HLEInterruptHandler(interruptNumber));
						}
					}
				} else {
					log.error(String.format("hleExceptionHandler unimplemented IP 0x%02X", ipBits));
				}
			} else if (exceptionNumber == EXCEP_SYS) {
				int syscallCode = processor.cp0.getSyscallCode() >> 2;
				log.error(String.format("hleExceptionHandler unimplemented syscall 0x%05X at 0x%08X", syscallCode, processor.cp0.getEpc()));
			} else {
				log.error(String.format("hleExceptionHandler unimplemented exceptionNumber=%d", exceptionNumber));
			}

			// Clear the EXL bit
			int status = processor.cp0.getStatus();
			status = clearFlag(status, Cp0State.STATUS_EXL); // Clear EXL bit
			processor.cp0.setStatus(status);
		}
	}

	@Override
	public void start() {
		hleExceptionHandlerAddr = HLEUtilities.getInstance().installHLEInterruptHandler(this, "hleExceptionHandler");
		super.start();
	}

	@Override
	public void stop() {
		Managers.intr.stop();
		super.stop();
	}

	@HLEFunction(nid = HLESyscallNid, version = 150)
    public void hleExceptionHandler(Processor processor) {
		Scheduler.getInstance().addAction(new HLEExceptionHandler(processor));
	}

	@HLEFunction(nid = 0xCA04A2B9, version = 150)
	@HLEFunction(nid = 0xFFA8B183, version = 660)
	public int sceKernelRegisterSubIntrHandler(int intrNumber, int subIntrNumber, TPointer handlerAddress, int handlerArgument) {
		return Managers.intr.sceKernelRegisterSubIntrHandler(intrNumber, subIntrNumber, handlerAddress, handlerArgument);
	}

	@HLEFunction(nid = 0xD61E6961, version = 150)
	public int sceKernelReleaseSubIntrHandler(int intrNumber, int subIntrNumber) {
		return Managers.intr.sceKernelReleaseSubIntrHandler(intrNumber, subIntrNumber);
	}

	@HLEFunction(nid = 0xFB8E22EC, version = 150)
	public int sceKernelEnableSubIntr(int intrNumber, int subIntrNumber) {
		return Managers.intr.sceKernelEnableSubIntr(intrNumber, subIntrNumber);
	}

	@HLEFunction(nid = 0x8A389411, version = 150)
	@HLEFunction(nid = 0x4023E1A7, version = 660)
	public int sceKernelDisableSubIntr(int intrNumber, int subIntrNumber) {
		return Managers.intr.sceKernelDisableSubIntr(intrNumber, subIntrNumber);
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x5CB5A78B, version = 150)
	public int sceKernelSuspendSubIntr() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x7860E0DC, version = 150)
	public int sceKernelResumeSubIntr() {
		return 0;
	}
    
	@HLEUnimplemented
	@HLEFunction(nid = 0xFC4374B8, version = 150)
	public int sceKernelIsSubInterruptOccurred() {
		return 0;
	}
    
	@HLEUnimplemented
	@HLEFunction(nid = 0xD2E8363F, version = 150)
	public int QueryIntrHandlerInfo() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xEEE43F47, version = 150)
	public int sceKernelRegisterUserSpaceIntrStack() {
		return 0;
	}

	@HLEFunction(nid = 0xD774BA45, version = 150)
	public int sceKernelDisableIntr(Processor processor, int intrNumber) {
		MMIOHandlerInterruptMan interruptMan = MMIOHandlerInterruptMan.getInstance(processor);
		interruptMan.disableInterrupt(intrNumber);

		return 0;
	}

	@HLEFunction(nid = 0x4D6E7305, version = 150)
	public int sceKernelEnableIntr(Processor processor, int intrNumber) {
		MMIOHandlerInterruptMan interruptMan = MMIOHandlerInterruptMan.getInstance(processor);
		interruptMan.enableInterrupt(intrNumber);
		processor.cp0.setStatus(processor.cp0.getStatus() | (IP2 << 8));
		processor.cp0.setEbase(hleExceptionHandlerAddr);

		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xDB14CBE0, version = 150)
	public int sceKernelResumeIntr() {
		return 0;
	}

	@HLEFunction(nid = 0x0C5F7AE3, version = 150)
	public int sceKernelCallSubIntrHandler(int intrNumber, int subIntrNumber, int handlerArg0, int handlerArg2) {
		return Managers.intr.sceKernelCallSubIntrHandler(intrNumber, subIntrNumber, handlerArg0, handlerArg2);
	}

	@HLEFunction(nid = 0x58DD8978, version = 150)
	public int sceKernelRegisterIntrHandler(int intrNumber, int unknown, TPointer func, int funcArg, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=12, usage=Usage.in) TPointer32 handler) {
		return Managers.intr.sceKernelRegisterIntrHandler(intrNumber, unknown, func, funcArg, handler);
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xF987B1F0, version = 150)
	public int sceKernelReleaseIntrHandler(int intrNumber) {
		return 0;
	}

	@HLEFunction(nid = 0xFE28C6D9, version = 150)
	public boolean sceKernelIsIntrContext() {
		return IntrManager.getInstance().isInsideInterrupt();
	}

	@HLEFunction(nid = 0xA0F88036, version = 150)
	public int sceKernelGetSyscallRA() {
		return RuntimeContext.syscallRa;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x14D4C61A, version = 660)
	public int sceKernelRegisterSystemCallTable(TPointer syscallTable) {
		return 0;
	}

	/*
	 * Returns the syscall number implementing the given function address
	 */
	@HLEFunction(nid = 0x8B61808B, version = 150)
	@HLEFunction(nid = 0xF153B371, version = 660)
	public int sceKernelQuerySystemCall(TPointerFunction func) {
		NIDMapper nidMapper = NIDMapper.getInstance();
		int syscall = nidMapper.getSyscallByAddress(func.getAddress());

		if (log.isDebugEnabled()) {
			log.debug(String.format("sceKernelQuerySystemCall func=%s returning syscall=0x%05X (NID 0x%08X from moduleName='%s')", func, syscall, nidMapper.getNidByAddress(func.getAddress()), nidMapper.getModuleNameByAddress(func.getAddress())));
		}

		return syscall;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x02475AAF, version = 150)
	@HLEFunction(nid = 0xF2F1E983, version = 660)
	public int sceKernelIsInterruptOccurred(int intrNumber) {
		return 0;
	}
}