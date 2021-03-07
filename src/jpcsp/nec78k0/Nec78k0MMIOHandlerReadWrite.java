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

import jpcsp.memory.mmio.MMIOHandlerReadWrite;

/**
 * @author gid15
 *
 */
public class Nec78k0MMIOHandlerReadWrite extends MMIOHandlerReadWrite {
	private Nec78k0Processor processor;

	public Nec78k0MMIOHandlerReadWrite(int baseAddress, int length, int[] memory) {
		super(baseAddress, length, memory);
	}

	public void setProcessor(Nec78k0Processor processor) {
		this.processor = processor;
	}

	@Override
	protected int getPc() {
		return processor.getCurrentInstructionPc();
	}

	@Override
	protected String getTraceFormatRead32() {
		return "0x%04X - read32(0x%04X)=0x%08X";
	}

	@Override
	protected String getTraceFormatRead16() {
		return "0x%04X - read16(0x%04X)=0x%04X";
	}

	@Override
	protected String getTraceFormatRead8() {
		return "0x%04X - read8(0x%04X)=0x%02X";
	}

	@Override
	protected String getTraceFormatWrite32() {
		return "0x%04X - write32(0x%04X, 0x%08X)";
	}

	@Override
	protected String getTraceFormatWrite16() {
		return "0x%04X - write16(0x%04X, 0x%04X)";
	}

	@Override
	protected String getTraceFormatWrite8() {
		return "0x%04X - write8(0x%04X, 0x%02X)";
	}
}
