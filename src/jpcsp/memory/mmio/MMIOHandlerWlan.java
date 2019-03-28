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

import static jpcsp.HLE.kernel.managers.IntrManager.PSP_WLAN_INTR;

import jpcsp.Memory;
import jpcsp.HLE.modules.sceWlan;
import jpcsp.memory.IntArrayMemory;

/**
 * MMIO for Wlan.
 * 
 * The Wlan interface is very similar to the MemoryStick Pro interface.
 * 
 * @author gid15
 *
 */
public class MMIOHandlerWlan extends MMIOHandlerBaseMemoryStick {
	private final IntArrayMemory attributesMemory = new IntArrayMemory(new int[0x40 / 4]);
	private static final int DUMMY_ATTRIBUTE_ENTRY = 0x1234;
	private static final int WLAN_RESULT_REG_ADDRESS = 0x54;

	public MMIOHandlerWlan(int baseAddress) {
		super(baseAddress);

		log = sceWlan.log;

		reset();
	}

	@Override
	protected void reset() {
		super.reset();

		// Possible values:
		// 0x0011 0x0001 0x0001
		// 0x0011 0x0001 0x1B18
		// 0x0011 0x0002 0x1B11
		// 0x0011 0x0002 0x0B11
		attributesMemory.writeUnsigned16(0, 0x0011);
		attributesMemory.writeUnsigned16(2, 0x0001);
		attributesMemory.writeUnsigned16(4, 0x1B18);

		for (int i = 1; i < 8; i++) {
			int offset = i * 8;
			attributesMemory.writeUnsigned16(offset + 0, i); // Has to be a value in range [1..8]
			attributesMemory.writeUnsigned16(offset + 2, DUMMY_ATTRIBUTE_ENTRY); // Unknown address used for MSPRO_CMD_READ_IO_ATRB
			attributesMemory.writeUnsigned16(offset + 4, 0x0040); // Unknown size
		}
		swapData32(attributesMemory, 0, 0x40);

		// Behaves like a Memory Stick Pro.
		registers[MS_TYPE_ADDRESS] = MS_TYPE_MEMORY_STICK_PRO;
		// For now in serial mode, it will be set later by the PSP to parallel mode.
		registers[MS_SYSTEM_ADDRESS] = MS_SYSTEM_SERIAL_MODE;

		// Unknown register value
		setRegisterValue(0x22, 0x24);
	}

	static private int swap32(int value) {
		return (value >>> 16) | (value << 16);
	}

	static private void swapData32(Memory mem, int address, int length) {
		for (int i = 0; i < length; i += 4) {
			mem.write32(address + i, swap32(mem.read32(address + i)));
		}
	}

	@Override
	protected void initMsproAttributeMemory() {
		log.error(String.format("MMIOHandlerWlan.initMsproAttributeMemory not supported"));
	}

	@Override
	protected int getInterruptNumber() {
		return PSP_WLAN_INTR;
	}

	@Override
	protected int getInterruptBit() {
		return 0x0000;
	}

	private void addResultFlag(int flag) {
		registers[WLAN_RESULT_REG_ADDRESS] |= flag;
	}

	@Override
	protected void setRegisterValue(int register, int value) {
		switch (register) {
			case WLAN_RESULT_REG_ADDRESS:
				// Writing to this register seems to only have the effect to clear bits of its value
				value = registers[register] & value;
				break;
		}

		super.setRegisterValue(register, value);

		switch (register) {
			case 0x56:
				// Writing to this register seems to also have the effect of updating the result register
				addResultFlag(0x04);
				break;
			case 0x5E:
				// Writing to this register seems to also have the effect of updating the result register
				addResultFlag(0x80);
				break;
		}
	}

	@Override
	protected int readData16(int dataAddress, int pageDataIndex) {
		int value = 0;

		switch (cmd) {
			case MSPRO_CMD_READ_IO_ATRB:
				if (dataAddress == 0 && pageDataIndex < attributesMemory.getSize()) {
					value = attributesMemory.read16(pageDataIndex);
				} else if (dataAddress == DUMMY_ATTRIBUTE_ENTRY) {
					// Dummy entry
					value = 0;
				} else {
					log.error(String.format("MMIOHandlerWlan.readData16 unimplemented cmd=0x%X(%s), dataAddress=0x%X, pageDataIndex=0x%X", cmd, getCommandName(cmd), dataAddress, pageDataIndex));
				}
				break;
			default:
				log.error(String.format("MMIOHandlerWlan.readData16 unimplemented cmd=0x%X(%s), dataAddress=0x%X, pageDataIndex=0x%X", cmd, getCommandName(cmd), dataAddress, pageDataIndex));
				break;
		}

		return value;
	}

	@Override
	protected void readPageBuffer() {
		log.error(String.format("MMIOHandlerWlan.readPageBuffer unimplemented"));
	}

	@Override
	protected void writePageBuffer() {
		log.error(String.format("MMIOHandlerWlan.writePageBuffer unimplemented"));
	}
}
