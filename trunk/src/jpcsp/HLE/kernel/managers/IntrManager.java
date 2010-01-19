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
package jpcsp.HLE.kernel.managers;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import jpcsp.Emulator;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.ThreadMan;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.hardware.Interrupts;

public class IntrManager {
	public static final int PSP_GPIO_INTR = 4;
	public static final int PSP_ATA_INTR = 5;
	public static final int PSP_UMD_INTR = 6;
	public static final int PSP_MSCM0_INTR = 7;
	public static final int PSP_WLAN_INTR = 8;
	public static final int PSP_AUDIO_INTR = 10;
	public static final int PSP_I2C_INTR = 12;
	public static final int PSP_SIRS_INTR = 14;
	public static final int PSP_SYSTIMER0_INTR = 15;
	public static final int PSP_SYSTIMER1_INTR = 16;
	public static final int PSP_SYSTIMER2_INTR = 17;
	public static final int PSP_SYSTIMER3_INTR = 18;
	public static final int PSP_THREAD0_INTR = 19;
	public static final int PSP_NAND_INTR = 20;
	public static final int PSP_DMACPLUS_INTR = 21;
	public static final int PSP_DMA0_INTR = 22;
	public static final int PSP_DMA1_INTR = 23;
	public static final int PSP_MEMLMD_INTR = 24;
	public static final int PSP_GE_INTR = 25;
	public static final int PSP_VBLANK_INTR = 30;
	public static final int PSP_MECODEC_INTR = 31;
	public static final int PSP_HPREMOTE_INTR = 36;
	public static final int PSP_MSCM1_INTR = 60;
	public static final int PSP_MSCM2_INTR = 61;
	public static final int PSP_THREAD1_INTR = 65;
	public static final int PSP_INTERRUPT_INTR = 66;
	public static final int PSP_NUMBER_INTERRUPTS = 67;

	private static final int VBLANK_SCHEDULE_MICROS = 1000000 / 60; // 1/60 second

	protected static IntrManager instance = null;
	private Vector<LinkedList<IntrHandler>> interrupts;
	private boolean someHandlerDefined;
	protected Scheduler scheduler = new Scheduler();
	protected IntrHandler[] intrHandlers;
	protected boolean insideInterrupt;

	public static IntrManager getInstance() {
		if (instance == null) {
			instance = new IntrManager();
		}

		return instance;
	}

	public void Initialize() {
		interrupts = new Vector<LinkedList<IntrHandler>>(PSP_NUMBER_INTERRUPTS);
		interrupts.setSize(PSP_NUMBER_INTERRUPTS);
		scheduler.Initialize();
		intrHandlers = new IntrHandler[IntrManager.PSP_NUMBER_INTERRUPTS];
		someHandlerDefined = false;
		installDefaultInterrupts();
	}

	private void installDefaultInterrupts() {
		// install VBLANK interrupt every 1/60 second
		scheduler.addAction(scheduler.getNow() + VBLANK_SCHEDULE_MICROS, new VblankInterruptAction());
	}

	public void step() {
		// Nothing to do if interrupts are currently disabled
		// or if no handler has been defined up to now.
		if (!someHandlerDefined || Interrupts.isInterruptsDisabled()) {
			return;
		}

		long now = scheduler.getNow();
		while (true) {
			IAction action = scheduler.getAction(now);
			if (action == null) {
				break;
			}
			action.execute(Emulator.getProcessor());
		}
	}

	public LinkedList<IntrHandler> getIntrHandlers(int intrNumber) {
		if (intrNumber < 0 || intrNumber >= PSP_NUMBER_INTERRUPTS) {
			return null;
		}

		return interrupts.get(intrNumber);
	}

	public void addIntrHanlder(int intrNumber, IntrHandler intrHandler) {
		if (intrNumber < 0 || intrNumber >= PSP_NUMBER_INTERRUPTS) {
			return;
		}

		LinkedList<IntrHandler> intrHandlers = interrupts.get(intrNumber);
		if (intrHandlers == null) {
			intrHandlers = new LinkedList<IntrHandler>();
			interrupts.set(intrNumber, intrHandlers);
			someHandlerDefined = true;
		}

		intrHandlers.add(intrHandler);
	}

	public boolean removeIntrHandler(int intrNumber, IntrHandler intrHandler) {
		if (intrNumber < 0 || intrNumber >= PSP_NUMBER_INTERRUPTS) {
			return false;
		}

		LinkedList<IntrHandler> intrHandlers = interrupts.get(intrNumber);
		if (intrHandlers == null) {
			return false;
		}
		return intrHandlers.remove(intrHandler);
	}


	protected void onEndOfInterrupt() {
		if (Modules.log.isDebugEnabled()) {
			Modules.log.debug("End of Interrupt");
		}

		// Schedule to a thread having a higher priority if one is ready to run
		ThreadMan.getInstance().rescheduleCurrentThread();
	}

	protected void continueTriggerSubIntrHandler(InterruptState interruptState, Iterator<IntrHandler> intrHandlerIterator, Iterator<SubIntrHandler> subIntrHandlerInterator, IAction continueAction) {
		boolean somethingExecuted = false;
		do {
			if (subIntrHandlerInterator != null && subIntrHandlerInterator.hasNext()) {
				SubIntrHandler subIntrHandler = subIntrHandlerInterator.next();
				if (subIntrHandler != null && subIntrHandler.isEnabled()) {
					if (Modules.log.isDebugEnabled()) {
						Modules.log.debug(String.format("Calling SubIntrHandler 0x%08X(0x%08X)", subIntrHandler.getAddress(), subIntrHandler.getArgument()));
					}
					// call: handler(int subIntrNumber, void* argument)
					CpuState cpu = Emulator.getProcessor().cpu;
					cpu.gpr[4] = subIntrHandler.getId();
					cpu.gpr[5] = subIntrHandler.getArgument();
					ThreadMan.getInstance().callAddress(subIntrHandler.getAddress(), continueAction);
					somethingExecuted = true;
				}
			} else if (intrHandlerIterator != null && intrHandlerIterator.hasNext()) {
				subIntrHandlerInterator = null;
				IntrHandler intrHandler = intrHandlerIterator.next();
				if (intrHandler.isEnabled()) {
					List<SubIntrHandler> subIntrHandlers = intrHandler.getSubIntrHandlers();
					if (subIntrHandlers != null) {
						subIntrHandlerInterator = subIntrHandlers.iterator();
					}
				}
			} else {
				break;
			}
		} while (!somethingExecuted);

		if (!somethingExecuted) {
			// No more handlers, end of interrupt
			insideInterrupt = interruptState.restore(Emulator.getProcessor().cpu);
			IAction afterAction = interruptState.getAfterAction();
			if (afterAction != null) {
				afterAction.execute(Emulator.getProcessor());
			}
			onEndOfInterrupt();
		}
	}

	public void triggerInterrupt(int intrNumber, IAction afterAction) {
		if (Modules.log.isDebugEnabled()) {
			Modules.log.debug(String.format("Triggering Interrupt 0x%X", intrNumber));
		}

		LinkedList<IntrHandler> intrHanlers = getIntrHandlers(intrNumber);
		if (intrHanlers != null) {
			InterruptState interruptState = new InterruptState();
			interruptState.save(insideInterrupt, Emulator.getProcessor().cpu, afterAction);
			insideInterrupt = true;

			Iterator<IntrHandler> intrHandlerIterator = intrHanlers.iterator();
			Iterator<SubIntrHandler> subIntrHandlerIterator = null;
			IAction continueAction = new AfterSubIntrAction(interruptState, intrHandlerIterator, subIntrHandlerIterator);

			continueTriggerSubIntrHandler(interruptState, intrHandlerIterator, subIntrHandlerIterator, continueAction);
		} else if (afterAction != null) {
			afterAction.execute(Emulator.getProcessor());
			onEndOfInterrupt();
		}
	}

	public boolean isInsideInterrupt() {
		return insideInterrupt;
	}

	public void setInsideInterrupt(boolean insideInterrupt) {
		this.insideInterrupt = insideInterrupt;
	}

	public int sceKernelRegisterSubIntrHandler(int intrNumber, int subIntrNumber, int handlerAddress, int handlerArgument) {
		if (Modules.log.isDebugEnabled()) {
			Modules.log.debug(String.format("sceKernelRegisterSubIntrHandler(%d, %d, 0x%08X, 0x%08X)", intrNumber, subIntrNumber, handlerAddress, handlerArgument));
		}

		if (intrNumber < 0 || intrNumber >= IntrManager.PSP_NUMBER_INTERRUPTS) {
			return -1;
		}

		if (intrHandlers[intrNumber] == null) {
			IntrHandler intrHandler = new IntrHandler();
			intrHandlers[intrNumber] = intrHandler;
			addIntrHanlder(intrNumber, intrHandler);
		}

		SubIntrHandler subIntrHandler = new SubIntrHandler(handlerAddress, subIntrNumber, handlerArgument);
		subIntrHandler.setEnabled(false);
		intrHandlers[intrNumber].addSubIntrHandler(subIntrNumber, subIntrHandler);

		return 0;
	}

	public int sceKernelReleaseSubIntrHandler(int intrNumber, int subIntrNumber) {
		if (Modules.log.isDebugEnabled()) {
			Modules.log.debug(String.format("sceKernelReleaseSubIntrHandler(%d, %d)", intrNumber, subIntrNumber));
		}

		if (intrNumber < 0 || intrNumber >= IntrManager.PSP_NUMBER_INTERRUPTS) {
			return -1;
		}

		if (intrHandlers[intrNumber] == null) {
			return -1;
		}

		if (!intrHandlers[intrNumber].removeSubIntrHandler(subIntrNumber)) {
			return -1;
		}

		return 0;
	}

	protected int hleKernelEnableDisableSubIntr(int intrNumber, int subIntrNumber, boolean enabled) {
		if (Modules.log.isDebugEnabled()) {
			Modules.log.debug(String.format("sceKernel%sSubIntr(%d, %d)", enabled ? "Enable" : "Disable", intrNumber, subIntrNumber));
		}

		if (intrNumber < 0 || intrNumber >= IntrManager.PSP_NUMBER_INTERRUPTS) {
			return -1;
		}

		if (intrHandlers[intrNumber] == null) {
			return -1;
		}

		SubIntrHandler subIntrHandler = intrHandlers[intrNumber].getSubIntrHandler(subIntrNumber);
		if (subIntrHandler == null) {
			return -1;
		}

		subIntrHandler.setEnabled(enabled);

		return 0;
	}

	public int sceKernelEnableSubIntr(int intrNumber, int subIntrNumber) {
		return hleKernelEnableDisableSubIntr(intrNumber, subIntrNumber, true);
	}

	public int sceKernelDisableSubIntr(int intrNumber, int subIntrNumber) {
		return hleKernelEnableDisableSubIntr(intrNumber, subIntrNumber, false);
	}

	private class Scheduler {
		private List<SchedulerAction> actions;
		private SchedulerAction nextAction;

		public void Initialize() {
			actions = new LinkedList<SchedulerAction>();
			nextAction = null;
		}

		private void addSchedulerAction(SchedulerAction schedulerAction) {
			actions.add(schedulerAction);
			updateNextAction(schedulerAction);
		}

		public void addAction(long schedule, IAction action) {
			SchedulerAction schedulerAction = new SchedulerAction(schedule, action);
			addSchedulerAction(schedulerAction);
		}

		private void updateNextAction(SchedulerAction schedulerAction) {
			if (nextAction == null || schedulerAction.getSchedule() < nextAction.getSchedule()) {
				nextAction = schedulerAction;
			}
		}

		private void updateNextAction() {
			nextAction = null;

			for (SchedulerAction schedulerAction : actions) {
				updateNextAction(schedulerAction);
			}
		}

		public IAction getAction(long now) {
			if (nextAction == null || now < nextAction.getSchedule()) {
				return null;
			}

			IAction action = nextAction.getAction();

			actions.remove(nextAction);
			updateNextAction();

			return action;
		}

		public long getNow() {
			return Emulator.getClock().microTime();
		}
	}

	private class SchedulerAction {
		private long schedule;
		private IAction action;

		public SchedulerAction(long schedule, IAction action) {
			this.schedule = schedule;
			this.action = action;
		}

		public long getSchedule() {
			return schedule;
		}

		public void setSchedule(long schedule) {
			this.schedule = schedule;
		}

		public IAction getAction() {
			return action;
		}

		public void setAction(IAction action) {
			this.action = action;
		}
	}

	public class VblankInterruptAction implements IAction {
		@Override
		public void execute(Processor processor) {
			// Re-schedule next VBLANK interrupt in 1/60 second
			scheduler.addAction(scheduler.getNow() + VBLANK_SCHEDULE_MICROS, new VblankInterruptAction());

			// Trigger VBLANK interrupt
			triggerInterrupt(IntrManager.PSP_VBLANK_INTR, null);
		}
	}

	public class IntrHandler {
		private Vector<SubIntrHandler> subInterrupts = new Vector<SubIntrHandler>();
		private int minIndex = Integer.MAX_VALUE;
		private int maxIndex = Integer.MIN_VALUE;
		private boolean enabled;

		public IntrHandler() {
			enabled = true;
		}

		public IntrHandler(boolean enabled) {
			this.enabled = enabled;
		}

		public void addSubIntrHandler(int id, SubIntrHandler subIntrHandler) {
			if (id >= subInterrupts.size()) {
				subInterrupts.setSize(id + 1);
			}

			if (id < minIndex) {
				minIndex = id;
			}
			if (id > maxIndex) {
				maxIndex = id;
			}

			subInterrupts.set(id, subIntrHandler);
		}

		public boolean removeSubIntrHandler(int id) {
			boolean removed = (subInterrupts.remove(id) != null);

			if (maxIndex >= subInterrupts.size()) {
				maxIndex = subInterrupts.size() - 1;
			}

			return removed;
		}

		public List<SubIntrHandler> getSubIntrHandlers() {
			if (minIndex > maxIndex) {
				return null;
			}

			return subInterrupts.subList(minIndex, maxIndex + 1);
		}

		public SubIntrHandler getSubIntrHandler(int id) {
			return subInterrupts.get(id);
		}

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}
	}

	public class SubIntrHandler {
		private int address;
		private int id;
		private int argument;
		private boolean enabled;

		public SubIntrHandler(int address, int id, int argument) {
			this.address = address;
			this.id = id;
			this.argument = argument;
		}

		public int getAddress() {
			return address;
		}

		public void setAddress(int address) {
			this.address = address;
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public int getArgument() {
			return argument;
		}

		public void setArgument(int argument) {
			this.argument = argument;
		}

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}
	}

	private class AfterSubIntrAction implements IAction {
		private Iterator<IntrHandler> intrHandlerIterator;
		private Iterator<SubIntrHandler> subIntrHandlerInterator;
		private InterruptState interruptState;

		public AfterSubIntrAction(InterruptState interruptState, Iterator<IntrHandler> intrHandlerIterator, Iterator<SubIntrHandler> subIntrHandlerIterator) {
			this.interruptState = interruptState;
			this.intrHandlerIterator = intrHandlerIterator;
			this.subIntrHandlerInterator = subIntrHandlerIterator;
		}

		public void execute(Processor processor) {
			continueTriggerSubIntrHandler(interruptState, intrHandlerIterator, subIntrHandlerInterator, this);
		}
	}

	private class InterruptState {
		boolean insideInterrupt;
		private CpuState savedCpu;
		private IAction afterAction;

		public void save(boolean insideInterrupt, CpuState cpu, IAction afterAction) {
			this.insideInterrupt = insideInterrupt;
			savedCpu = new CpuState(cpu);
			this.afterAction = afterAction;
		}

		public boolean restore(CpuState cpu) {
			cpu.copy(savedCpu);

			return insideInterrupt;
		}

		public IAction getAfterAction() {
			return afterAction;
		}
	}
}
