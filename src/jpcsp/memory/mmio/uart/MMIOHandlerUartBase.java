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
package jpcsp.memory.mmio.uart;

import org.apache.log4j.Logger;

import jpcsp.memory.mmio.MMIOHandlerBase;

public class MMIOHandlerUartBase extends MMIOHandlerBase {
	public static Logger log = Logger.getLogger("uart");
	public static final int SIZE_OF = 0x48;
	public static final int UART_STATUS_RXEMPTY = 0x10;
	public static final int UART_STATUS_TXFULL = 0x20;
	private int data;
	private int status;
	private long baudrateDivisor;
	private int control;
	private int unknown04;
	private int unknown30;
	private int unknown34;
	private int unknown38;
	private int interrupt;

	public MMIOHandlerUartBase(int baseAddress) {
		super(baseAddress);

		status = UART_STATUS_RXEMPTY;
	}

	private void clearInterrupt(int mask) {
		interrupt &= ~mask;
	}

	@Override
	public int read32(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x00: value = data; break;
			case 0x04: value = unknown04; break;
			case 0x18: value = status; break;
			case 0x2C: value = control; break;
			case 0x30: value = unknown30; break;
			case 0x34: value = unknown34; break;
			case 0x38: value = unknown38; break;
			case 0x44: value = interrupt; break;
			default: value = super.read32(address); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - read32(0x%08X) returning 0x%08X", getPc(), address, value));
		}

		return value;
	}

	@Override
	public void write32(int address, int value) {
		switch (address - baseAddress) {
			case 0x00: data = value; break;
			case 0x04: unknown04 = value; break;
			case 0x18: status = value; break;
			case 0x24: baudrateDivisor = (baudrateDivisor & 0x3FL) | (((long) value) << 6); break;
			case 0x28: baudrateDivisor = (baudrateDivisor & ~0x3FL) | (value & 0x3F); break;
			case 0x2C: control = value; break;
			case 0x30: unknown30 = value; break;
			case 0x34: unknown34 = value; break;
			case 0x38: unknown38 = value; break;
			case 0x44: clearInterrupt(value); break;
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}
}
