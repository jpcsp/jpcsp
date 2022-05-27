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
package jpcsp.mediaengine;

import java.io.IOException;

import org.apache.log4j.Logger;

import jpcsp.Memory;
import jpcsp.hardware.Model;
import jpcsp.memory.mmio.IMMIOHandler;
import jpcsp.memory.mmio.MMIO;
import jpcsp.memory.mmio.MMIOHandlerReadWrite;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

/**
 * The PSP Media Engine memory:
 * - 0x00000000 - 0x001FFFFF (2MB): ME internal RAM
 * - access to the main memory
 * - access to the MMIO
 * - access to the Media Engine DSP processor
 *
 * @author gid15
 *
 */
public class MEMemory extends MMIO {
	private static final int STATE_VERSION = 0;
	public static final int START_ME_RAM = 0x00000000;
	public static int END_ME_RAM;
	public static int SIZE_ME_RAM;
	private final IMMIOHandler meRamHandlers[] = new IMMIOHandler[8];
	private final int[] meRam;

	public MEMemory(Memory mem, Logger log) {
		super(mem);

		// The PSP Fat has 2MB ME memory, while the PSP Slim has 4MB.
		SIZE_ME_RAM = Model.getGeneration() == 1 ? 0x200000 : 0x400000;
		END_ME_RAM = START_ME_RAM + SIZE_ME_RAM - 1;

		// This array will store the contents of the ME RAM and
		// will be shared between several handlers at different addresses.
		meRam = new int[SIZE_ME_RAM >> 2];

		// The ME RAM  is visible at address range 0x00000000-0x001FFFFF
		addMeRamHandler(0x00000000, meRam, log);

		// The same memory is also visible at address range 0x40000000-0x401FFFFF
		addMeRamHandler(0x40000000, meRam, log);

		// The same memory is also visible at address range 0x80000000-0x801FFFFF
		addMeRamHandler(0x80000000, meRam, log);

		// The same memory is also visible at address range 0xA0000000-0xA01FFFFF
		addMeRamHandler(0xA0000000, meRam, log);

		addHandlerRW(0x44000000, 0x8000, log);
		addHandlerRW(0x44020000, 0x8000, log);

		addHandler(0x440F8000, 0x194, new MMIOHandlerMe0F8000(0x440F8000));
		addHandler(0x440FF000, 0x30, new MMIOHandlerMe0FF000(0x440FF000));
		addHandler(0x44100000, 0x40, new MMIOHandlerMeDecoderQuSpectra(0x44100000));
	}

	private void addMeRamHandler(int address, int[] meRam, Logger log) {
		MMIOHandlerReadWrite handler = new MMIOHandlerReadWrite(START_ME_RAM | address, SIZE_ME_RAM, meRam);
		handler.setLogger(log);
		meRamHandlers[address >>> 29] = handler;
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		// The ME RAM need to be read only once
		meRamHandlers[0].read(stream);
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		// The ME RAM need to be written only once
		meRamHandlers[0].write(stream);
		super.write(stream);
	}

	@Override
	protected IMMIOHandler getHandler(int address) {
		// Fast retrieval for the ME RAM
		if ((address & Memory.addressMask) <= END_ME_RAM) {
			return meRamHandlers[address >>> 29];
		}
		return super.getHandler(address);
	}

	@Override
	public boolean hasMemoryInt(int address) {
		if ((address & Memory.addressMask) <= END_ME_RAM) {
			return true;
		}
		return super.hasMemoryInt(address);
	}

	@Override
	public int[] getMemoryInt(int address) {
		if ((address & Memory.addressMask) <= END_ME_RAM) {
			return meRam;
		}
		return super.getMemoryInt(address);
	}
}
