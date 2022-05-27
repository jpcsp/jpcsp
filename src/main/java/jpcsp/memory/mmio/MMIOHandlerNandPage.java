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
package jpcsp.memory.mmio;

import static jpcsp.hardware.Nand.pageSize;

import java.io.IOException;
import java.util.Arrays;

import org.apache.log4j.Logger;

import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

public class MMIOHandlerNandPage extends MMIOHandlerBase {
	public static Logger log = MMIOHandlerNand.log;
	private static final int STATE_VERSION = 0;
	public static final int BASE_ADDRESS1 = 0xBFF00000;
	public static final int BASE_ADDRESS2 = 0x9FF00000;
	private static MMIOHandlerNandPage instance;
	private final int[] data = new int[pageSize >> 2];
	private final int[] ecc = new int[4];

	public static MMIOHandlerNandPage getInstance() {
		if (instance == null) {
			instance = new MMIOHandlerNandPage(BASE_ADDRESS1);
		}
		return instance;
	}

	private MMIOHandlerNandPage(int baseAddress) {
		super(baseAddress);
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		stream.readInts(data);
		stream.readInts(ecc);
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInts(data);
		stream.writeInts(ecc);
		super.write(stream);
	}

	@Override
	public void reset() {
		super.reset();

		Arrays.fill(data, 0);
		Arrays.fill(ecc, 0);
	}

	public int[] getData() {
		return data;
	}

	public int[] getEcc() {
		return ecc;
	}

	@Override
	public int read32(int address) {
		int value;
		int localAddress = (address - baseAddress) & 0xFFFFF;
		switch (localAddress) {
			case 0x800: value = ecc[0]; break;
			case 0x900: value = ecc[1]; break;
			case 0x904: value = ecc[2]; break;
			case 0x908: value = ecc[3]; break;
			default:
				if (localAddress >= 0 && localAddress < 0x200) {
					value = data[localAddress >> 2];
				} else {
					value = super.read32(address);
				}
				break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - read32(0x%08X) returning 0x%08X", getPc(), address, value));
		}

		return value;
	}

	@Override
	public void write32(int address, int value) {
		int localAddress = (address - baseAddress) & 0xFFFFF;
		switch (localAddress) {
			case 0x800: ecc[0] = value; break;
			case 0x900: ecc[1] = value; break;
			case 0x904: ecc[2] = value; break;
			case 0x908: ecc[3] = value; break;
			default:
				if (localAddress >= 0 && localAddress < 0x200) {
					data[localAddress >> 2] = value;
				} else {
					super.write32(address, value);
				}
				break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}
}
