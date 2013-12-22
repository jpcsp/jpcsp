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
package jpcsp.graphics.RE.externalge;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import org.apache.log4j.Logger;

import jpcsp.Memory;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.util.DurationStatistics;
import jpcsp.util.Hash;
import jpcsp.util.Utilities;

/**
 * @author gid15
 *
 */
public class NativeCallbacks {
	private static final Logger log = Logger.getLogger("NativeCallbacks");
	private static DurationStatistics read32 = new DurationStatistics("read32");
	private static DurationStatistics readByteBuffer = new DurationStatistics("readByteBuffer");
	private static DurationStatistics writeByteBuffer = new DurationStatistics("writeByteBuffer");
	private static DurationStatistics getHashCode = new DurationStatistics("getHashCode");

	// Array indexed by the log category
	private static final Logger[] logs = new Logger[] {
		log,
		Logger.getLogger("externalge")
	};

	public static void exit() {
		if (DurationStatistics.collectStatistics) {
			log.info(read32.toString());
			log.info(readByteBuffer.toString());
			log.info(writeByteBuffer.toString());
			log.info(getHashCode.toString());
		}
	}

	private static Memory getMemory() {
		return Memory.getInstance();
	}

	public static int read32(int address) {
		if (DurationStatistics.collectStatistics) {
			read32.start();
			int value = getMemory().read32(address);
			read32.end();
			return value;
		}
		return getMemory().read32(address);
	}

	public static int read16(int address) {
		return getMemory().read16(address);
	}

	public static int read8(int address) {
		return getMemory().read8(address);
	}

	public static int readByteBuffer(int address, ByteBuffer destination, int length) {
		readByteBuffer.start();
		Buffer source = getMemory().getBuffer(address, length);
		int offset = 0;
		if (source != null) {
			if (source instanceof IntBuffer) {
				offset = address & 3;
			}
			Utilities.putBuffer(destination, source, ByteOrder.LITTLE_ENDIAN, length + offset);
		}
		readByteBuffer.end();
		return offset;
	}

	public static void write32(int address, int value) {
		getMemory().write32(address, value);
	}

	public static void write16(int address, short value) {
		getMemory().write16(address, value);
	}

	public static void write8(int address, byte value) {
		getMemory().write8(address, value);
	}

	public static void copy(int destination, int source, int length) {
		getMemory().memcpy(destination, source, length);
	}

	public static void writeByteBuffer(int address, ByteBuffer source, int length) {
		writeByteBuffer.start();
		if (RuntimeContext.memoryInt != null && (address & 3) == 0 && (length & 3) == 0) {
			IntBuffer destination = IntBuffer.wrap(RuntimeContext.memoryInt, address >> 2, length >> 2);
			source.order(ByteOrder.nativeOrder());
			destination.put(source.asIntBuffer());
		} else {
			getMemory().copyToMemory(address, source, length);
		}
		writeByteBuffer.end();
	}

	public static int getHashCode(int hashCode, int addr, int lengthInBytes, int strideInBytes) {
		getHashCode.start();
		int value = Hash.getHashCode(hashCode, addr, lengthInBytes, strideInBytes);
		getHashCode.end();
		return value;
	}

	public static void log(int category, int level, String message) {
		Logger log;
		if (category >= 0 && category < logs.length) {
			log = logs[category];
		} else {
			log = NativeCallbacks.log;
		}

		// Values matching jpcsp::log::Level defined in jpcsp.log.h
		switch (level) {
			case 0: // E_OFF
				break;
			case 1: // E_FATAL
				log.fatal(message);
				break;
			case 2: // E_ERROR
				log.error(message);
				break;
			case 3: // E_WARN
				log.warn(message);
				break;
			case 4: // E_INFO
				log.info(message);
				break;
			case 5: // E_DEBUG
				log.debug(message);
				break;
			case 6: // E_TRACE
				log.trace(message);
				break;
			case 7: // E_FORCE
				log.info(message);
				break;
			default:
				log.error(String.format("Unknown log level %d: %s", level, message));
				break;
		}
	}
}
