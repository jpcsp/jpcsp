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
package jpcsp.memory.mmio.syscon;

import static jpcsp.nec78k0.Nec78k0Memory.BASE_RAM0;
import static jpcsp.nec78k0.Nec78k0Memory.END_RAM0;
import static jpcsp.util.Utilities.KB;
import static jpcsp.util.Utilities.readUnaligned32;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;

import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.HLE.modules.sceSyscon;
import jpcsp.hardware.Model;
import jpcsp.nec78k0.Nec78k0Interpreter;
import jpcsp.nec78k0.Nec78k0Memory;
import jpcsp.nec78k0.Nec78k0Processor;
import jpcsp.util.Utilities;

/**
 * @author gid15
 *
 */
public class SysconEmulator {
    public static Logger log = sceSyscon.log;
	private final Nec78k0Memory mem;
	private final Nec78k0Processor processor;
	private final Nec78k0Interpreter interpreter;
	private SysconProcessorThread thread;
	private volatile boolean exit;
	private static boolean isEnabled;
	private static int initializedModel = -1;

	private class SysconProcessorThread extends Thread {
		@Override
		public void run() {
			RuntimeContext.setLog4jMDC();
			load(mem);
			processor.reset();

			while (!exit) {
				interpreter.run();
			}
			thread = null;
		}
	}

	public static String getFirmwareFileName() {
		switch (Model.getModel()) {
			case Model.MODEL_PSP_SLIM:   return "TA-085_Full.bin";
			case Model.MODEL_PSP_FAT:    return "TA-086_Full.bin";
			case Model.MODEL_PSP_BRITE:  return "TA-090_Full.bin";
			case Model.MODEL_PSP_GO:     return "TA-091_Full.bin";
			case Model.MODEL_PSP_BRITE2: return "TA-093_Full.bin";
			case Model.MODEL_PSP_BRITE3:
			case Model.MODEL_PSP_BRITE4: return "TA-095_Full.bin";
			case Model.MODEL_PSP_STREET: return "TA-096_Full.bin";
		}
		return String.format("syscon_%02dg.bin", Model.getGeneration());
	}

	public static void disable() {
		initializedModel = Model.getModel();
		isEnabled = false;
	}

	public static boolean isEnabled() {
		// Check for the firmware file only if the PSP model has changed since last call
		if (initializedModel != Model.getModel()) {
			File firmwareFile = new File(getFirmwareFileName());
			// The firmware file must be at least 48KB
			if (firmwareFile.canRead() && firmwareFile.length() >= 48 * KB) {
				isEnabled = true;
			} else {
				isEnabled = false;
			}

			initializedModel = Model.getModel();
		}

		return isEnabled;
	}

	public SysconEmulator() {
		mem = new Nec78k0Memory(Nec78k0Processor.log);
		processor = new Nec78k0Processor(mem);
		interpreter = new Nec78k0Interpreter(processor);
	}

	public static void load(Nec78k0Memory mem) {
		String firmwareFileName = getFirmwareFileName();
		log.info(String.format("Loading %s", firmwareFileName));

		File inputFile = new File(firmwareFileName);
		byte[] buffer = new byte[(int) inputFile.length()];
		int length = buffer.length;
		try {
			InputStream is = new FileInputStream(inputFile);
			length = is.read(buffer);
			is.close();
		} catch (IOException e) {
			log.error(e);
		}

		int baseAddress = BASE_RAM0;
		length = Math.min(length, END_RAM0);
		for (int i = 0; i < length; i += 4) {
			mem.write32(baseAddress + i, readUnaligned32(buffer, i));
		}

		if (mem.internalRead32(0x8000) == 0xFFFFFFFF) {
			try {
				InputStream is = new FileInputStream("TA-086_Full.bin");
				length = is.read(buffer);
				is.close();

				baseAddress = 0x8000;
				length = Math.min(length - 0x8000, 0x2000);
				for (int i = 0; i < length; i += 4) {
					mem.write32(baseAddress + i, readUnaligned32(buffer, baseAddress + i));
				}
			} catch (IOException e) {
				log.error(e);
			}
		}
	}

	public void boot() {
		if (log.isInfoEnabled()) {
			log.info(String.format("Using syscon firmware from '%s'", getFirmwareFileName()));
		}

		if (thread == null) {
			thread = new SysconProcessorThread();
			thread.setName("Syscon Nec 78k0 Processor Thread");
			thread.setDaemon(true);
			thread.start();
		} else {
			log.error(String.format("SysconFirmware.boot() SysconProcessorThread already running"));
		}
	}

	public void exit() {
		exit = true;
		interpreter.exitInterpreter();

		while (thread != null) {
			Utilities.sleep(1, 0);
		}
	}

	public void startSysconCmd(int[] data) {
		mem.getSysconSfr().startSysconCmd(data);
	}
}
