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
package jpcsp.state;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.HLEModuleManager;
import jpcsp.hardware.Battery;
import jpcsp.hardware.Wlan;

public class State implements IState {
	public static Logger log = Logger.getLogger("state");
	private static final int STATE_VERSION = 0;

	public State() {
	}

	public void read(String fileName) throws IOException {
		FileInputStream fileInputStream = new FileInputStream(fileName);
		GZIPInputStream gzipInputStream = new GZIPInputStream(fileInputStream);
		BufferedInputStream bufferedInputStream = new BufferedInputStream(gzipInputStream);
		StateInputStream stream = new StateInputStream(bufferedInputStream);

		if (log.isInfoEnabled()) {
			log.info(String.format("Reading state from file '%s'", fileName));
		}

		try {
			read(stream);
			if (stream.read() >= 0) {
				log.error(String.format("State file '%s' containing too much data", fileName));
			}
		} finally {
			stream.close();
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("Done reading state from file '%s'", fileName));
		}
	}

	public void write(String fileName) throws IOException {
		FileOutputStream fileOutputStream = new FileOutputStream(fileName);
		GZIPOutputStream gzipOutputStream = new GZIPOutputStream(fileOutputStream);
		BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(gzipOutputStream);
		StateOutputStream stream = new StateOutputStream(bufferedOutputStream);

		if (log.isInfoEnabled()) {
			log.info(String.format("Writing state to file '%s'", fileName));
		}

		try {
			write(stream);
		} finally {
			stream.close();
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("Done writing state to file '%s'", fileName));
		}
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		if (stream.readBoolean()) {
			Emulator.getMainGUI().doReboot();
		}
    	Emulator.getClock().read(stream);
		Wlan.read(stream);
		Battery.read(stream);
		Emulator.getProcessor().read(stream);
		Emulator.getMemory().read(stream);
		HLEModuleManager.getInstance().read(stream);
		boolean isLLEActive = stream.readBoolean();
		if (isLLEActive) {
			RuntimeContextLLE.enableLLE();
			RuntimeContextLLE.read(stream);
			RuntimeContextLLE.createMMIO();
			RuntimeContextLLE.getMMIO().read(stream);
			RuntimeContextLLE.getMediaEngineProcessor().read(stream);
			RuntimeContextLLE.getMediaEngineProcessor().getMEMemory().read(stream);
		}
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeBoolean(Emulator.getMainGUI().isRunningReboot());
		Emulator.getClock().write(stream);
		Wlan.write(stream);
		Battery.write(stream);
		Emulator.getProcessor().write(stream);
		Emulator.getMemory().write(stream);
		HLEModuleManager.getInstance().write(stream);
		if (RuntimeContextLLE.isLLEActive()) {
			stream.writeBoolean(true);
			RuntimeContextLLE.write(stream);
			RuntimeContextLLE.createMMIO();
			RuntimeContextLLE.getMMIO().write(stream);
			RuntimeContextLLE.getMediaEngineProcessor().write(stream);
			RuntimeContextLLE.getMediaEngineProcessor().getMEMemory().write(stream);
		} else {
			stream.writeBoolean(false);
		}
	}
}
