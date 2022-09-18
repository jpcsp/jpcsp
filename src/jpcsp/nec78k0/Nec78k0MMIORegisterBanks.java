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

import static jpcsp.nec78k0.Nec78k0Processor.REGISTER_ADDRESS_BANK3;
import static jpcsp.util.Utilities.u16;
import static jpcsp.util.Utilities.u8;

/**
 * @author gid15
 *
 */
public class Nec78k0MMIORegisterBanks extends Nec78k0MMIOHandlerBase {
	public Nec78k0MMIORegisterBanks() {
		super(REGISTER_ADDRESS_BANK3);
	}

	private int getRegisterNumber(int address) {
		return address & 0x7;
	}

	private int getRegisterPairNumber(int address) {
		return getRegisterNumber(address) >> 1;
	}

	private int getRegisterBankNumber(int address) {
		return 3 - ((address >> 3) & 0x3);
	}

	@Override
	public int read8(int address) {
		return processor.getRegister(getRegisterNumber(address), getRegisterBankNumber(address));
	}

	@Override
	public void write8(int address, byte value) {
		processor.setRegister(getRegisterNumber(address), getRegisterBankNumber(address), u8(value));
	}

	@Override
	public int read16(int address) {
		return processor.getRegisterPair(getRegisterPairNumber(address), getRegisterBankNumber(address));
	}

	@Override
	public void write16(int address, short value) {
		processor.setRegisterPair(getRegisterPairNumber(address), getRegisterBankNumber(address), u16(value));
	}
}
