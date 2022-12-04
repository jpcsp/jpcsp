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
package jpcsp.nec78k0;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import jpcsp.Emulator;

/**
 * @author gid15
 *
 */
public class Nec78k0Interpreter {
	private final Nec78k0Processor processor;
	private final Logger log;
	private final Map<Integer, INec78k0HLECall> hleCalls = new HashMap<Integer, INec78k0HLECall>();
	public static final int PC_END_RUN = 0xFFFF;
	private boolean exitInterpreter;
	private boolean inInterpreter;
	private final Semaphore update = new Semaphore(0);
	private boolean halted;

	public Nec78k0Interpreter(Nec78k0Processor processor) {
		this.processor = processor;
		log = processor.log;
		processor.setInterpreter(this);
	}

	public void run() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Interpreting 0x%04X, halted=%b", processor.getNextInstructionPc(), isHalted()));
		}

		while (isHalted()) {
			try {
				update.tryAcquire(1, 10L, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				// Ignore exception
			}
		}

		inInterpreter = true;
		processor.checkPendingInterrupt();
		while (!Emulator.pause && !exitInterpreter && !processor.isNextInstructionPc(PC_END_RUN)) {
			processor.interpret();
		}
		inInterpreter = false;

		if (log.isDebugEnabled()) {
			log.debug(String.format("Exiting Nec78k0Interpreter loop at 0x%04X", processor.getNextInstructionPc()));
		}
		exitInterpreter = false;
	}

	public void exitInterpreter() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Request to exit Nec78k0Interpreter inInterpreter=%b", inInterpreter));
		}

		if (inInterpreter) {
			exitInterpreter = true;
		}
	}

	public boolean isHalted() {
		return halted;
	}

	public void setHalted(boolean halted) {
		this.halted = halted;

		update.release();
	}

	public INec78k0HLECall getHLECall(int addr) {
		return hleCalls.get(addr);
	}

	public void registerHLECall(int addr, INec78k0HLECall hleCall) {
		hleCalls.put(addr, hleCall);
		Nec78k0Instructions.registerFunctionName(addr, hleCall.getClass().getSimpleName());
	}
}
