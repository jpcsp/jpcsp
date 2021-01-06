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

import static jpcsp.arm.ARMProcessor.REG_R0;

import org.apache.log4j.Logger;

import jpcsp.arm.ARMInterpreter;
import jpcsp.arm.ARMMemory;
import jpcsp.arm.ARMProcessor;

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

		// Required for PSP generation 1
		interpreter.registerHLECall(0xFFFF2B79, 0, new HLEInitExceptions());
		// Required for PSP generation 2 or later
		interpreter.registerHLECall(0xFFFF233D, 1, new HLEInitExceptions());
		interpreter.registerHLECall(0xFFFF4409, 0, new HLEJumpCall(REG_R0));
		interpreter.registerHLECall(0xFFFF1D79, 1, new HLENullCall(3));
		interpreter.registerHLECall(0xFFFF440D, 1, new HLENullCall(3));
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
}
