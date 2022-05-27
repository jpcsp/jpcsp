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
package jpcsp.memory.mmio.wlan;

import org.apache.log4j.Logger;

import jpcsp.arm.ARMInterpreter;
import jpcsp.arm.ARMMemory;
import jpcsp.arm.ARMProcessor;
import jpcsp.memory.mmio.wlan.threadx.hle.TXManager;

/**
 * Emulator for the Wlan firmware
 * 
 * @author gid15
 *
 */
public class WlanEmulator {
	public static Logger log = Logger.getLogger("arm");
	private static WlanEmulator instance;
	private final ARMMemory mem; 
	private final ARMProcessor processor;
	private final ARMInterpreter interpreter;
	private final TXManager txManager;
	private ARMProcessorThread thread;

	private class ARMProcessorThread extends Thread {
		@Override
		public void run() {
			bootFromThread();
			thread = null;
		}
	}

	public static WlanEmulator getInstance() {
		if (instance == null) {
			instance = new WlanEmulator();
		}
		return instance;
	}

	private WlanEmulator() {
		mem = new ARMMemory(log);
		processor = new ARMProcessor(mem);
		interpreter = new ARMInterpreter(processor);
		txManager = new TXManager();
	}

	public ARMInterpreter getInterpreter() {
		return interpreter;
	}

	public ARMProcessor getProcessor() {
		return processor;
	}

	public ARMMemory getMemory() {
		return mem;
	}

	public TXManager getTxManager() {
		return txManager;
	}

	public void bootFromThread() {
		txManager.installHLECalls(interpreter);
		processor.resetException();
		interpreter.run();
	}

	public void boot() {
		if (thread == null) {
			thread = new ARMProcessorThread();
			thread.setName("ARM Processor Thread");
			thread.setDaemon(true);
			thread.start();
		} else {
			log.error(String.format("WlanEmulator.boot() ARMProcessorThread already running"));
		}
	}
}
