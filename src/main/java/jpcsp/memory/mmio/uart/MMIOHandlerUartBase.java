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

import java.io.IOException;

import org.apache.log4j.Logger;

import jpcsp.memory.mmio.MMIOHandlerBase;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

public class MMIOHandlerUartBase extends MMIOHandlerBase {
	public static Logger log = Logger.getLogger("uart");
	private static final int STATE_VERSION = 0;
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

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		data = stream.readInt();
		status = stream.readInt();
		baudrateDivisor = stream.readLong();
		control = stream.readInt();
		unknown04 = stream.readInt();
		unknown30 = stream.readInt();
		unknown34 = stream.readInt();
		unknown34 = stream.readInt();
		interrupt = stream.readInt();
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(data);
		stream.writeInt(status);
		stream.writeLong(baudrateDivisor);
		stream.writeInt(control);
		stream.writeInt(unknown04);
		stream.writeInt(unknown30);
		stream.writeInt(unknown34);
		stream.writeInt(unknown38);
		stream.writeInt(interrupt);
		super.write(stream);
	}

	@Override
	public void reset() {
		super.reset();

		data = 0;
		status = 0;
		baudrateDivisor = 0L;
		control = 0;
		unknown04 = 0;
		unknown30 = 0;
		unknown34 = 0;
		unknown38 = 0;
		interrupt = 0;
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
