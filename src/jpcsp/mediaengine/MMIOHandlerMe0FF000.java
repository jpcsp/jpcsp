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

import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.kernel.managers.IntrManager;

public class MMIOHandlerMe0FF000 extends MMIOHandlerMeBase {
	private int status;
	private int power;
	private int command;
	private int unknown10;
	private int unknown14;
	private int unknown18;
	private int unknown1C;
	private int unknown20;
	private int unknown24;
	private int unknown28;
	private int unknown2C;

	public MMIOHandlerMe0FF000(int baseAddress) {
		super(baseAddress);
	}

	private void setCommand(int command) {
		this.command = command;

		switch (command) {
			case 0x02:
				if (log.isDebugEnabled()) {
					log.debug(String.format("Unknown command 0x%X, status=0x%X", command, status));
				}
				break;
			case 0x03:
				if (log.isDebugEnabled()) {
					log.debug(String.format("Unknown command 0x%X, status=0x%X", command, status));
				}
				break;
			case 0x04:
				if (log.isDebugEnabled()) {
					log.debug(String.format("Unknown command 0x%X, status=0x%X, unknown10=0x%08X, unknown14=0x%X, unknown18=0x%X", command, status, unknown10, unknown14, unknown18));
				}
				break;
			case 0x05:
				if (log.isDebugEnabled()) {
					log.debug(String.format("Unknown command 0x%X, status=0x%X, unknown10=0x%08X", command, status, unknown10));
				}
				break;
			case 0x08:
				if (log.isDebugEnabled()) {
					log.debug(String.format("Unknown command 0x%X, status=0x%X", command, status));
				}
				break;
			case 0x18:
				if (log.isDebugEnabled()) {
					log.debug(String.format("Unknown command 0x%X, status=0x%X", command, status));
				}
				break;
			case 0x1D:
				// Used at startup
				if (log.isDebugEnabled()) {
					log.debug(String.format("Unknown command 0x%X, status=0x%X, power=0x%X, unknown10=0x%08X", command, status, power, unknown10));
				}
				break;
			case 0x20:
				if (log.isDebugEnabled()) {
					log.debug(String.format("Unknown command 0x%X, status=0x%X, unknown14=0x%X, unknown18=0x%X, unknown2C", command, status, unknown14, unknown18, unknown2C));
				}
				break;
			case 0x21:
				if (log.isDebugEnabled()) {
					log.debug(String.format("Unknown command 0x%X, status=0x%X, unknown14=0x%X, unknown18=0x%X", command, status, unknown14, unknown18));
				}
				break;
			case 0x28:
				if (log.isDebugEnabled()) {
					log.debug(String.format("Unknown command 0x%X, status=0x%X, unknown14=0x%X, unknown18=0x%X", command, status, unknown14, unknown18));
				}
				break;
			case 0x40:
				if (log.isDebugEnabled()) {
					log.debug(String.format("Unknown command 0x%X, status=0x%X, unknown10=0x%08X, unknown14=0x%X, unknown18=0x%X", command, status, unknown10, unknown14, unknown18));
				}
				break;
			case 0x42:
				if (log.isDebugEnabled()) {
					log.debug(String.format("Unknown command 0x%X, status=0x%X, unknown10=0x%08X, unknown14=0x%X, unknown18=0x%X", command, status, unknown10, unknown14, unknown18));
				}
				break;
			case 0x45:
				if (log.isDebugEnabled()) {
					log.debug(String.format("Unknown command 0x%X, status=0x%X, unknown10=0x%08X, unknown14=0x%X, unknown18=0x%X", command, status, unknown10, unknown14, unknown18));
				}
				break;
			case 0x48:
				if (log.isDebugEnabled()) {
					log.debug(String.format("Unknown command 0x%X, status=0x%X, unknown10=0x%08X, unknown14=0x%X, unknown18=0x%X", command, status, unknown10, unknown14, unknown18));
				}
				break;
			case 0x4D:
				if (log.isDebugEnabled()) {
					log.debug(String.format("Unknown command 0x%X, status=0x%X, unknown10=0x%08X, unknown14=0x%X, unknown18=0x%X", command, status, unknown10, unknown14, unknown18));
				}
				break;
			case 0x50:
				if (log.isDebugEnabled()) {
					log.debug(String.format("Unknown command 0x%X, status=0x%X, unknown10=0x%08X, unknown14=0x%X, unknown18=0x%X, unknown20=0x%X, unknown24=0x%X, unknown28=0x%X", command, status, unknown10, unknown14, unknown18, unknown20, unknown24, unknown28));
				}
				break;
			case 0x52:
				if (log.isDebugEnabled()) {
					log.debug(String.format("Unknown command 0x%X, status=0x%X, unknown10=0x%08X, unknown14=0x%X, unknown18=0x%X, unknown20=0x%X, unknown24=0x%X, unknown28=0x%X", command, status, unknown10, unknown14, unknown18, unknown20, unknown24, unknown28));
				}
				break;
			case 0x54:
				if (log.isDebugEnabled()) {
					log.debug(String.format("Unknown command 0x%X, status=0x%X, unknown10=0x%08X, unknown14=0x%X, unknown18=0x%X, unknown20=0x%X, unknown24=0x%X, unknown28=0x%X", command, status, unknown10, unknown14, unknown18, unknown20, unknown24, unknown28));
				}
				break;
			case 0x58:
				if (log.isDebugEnabled()) {
					log.debug(String.format("Unknown command 0x%X, status=0x%X, unknown10=0x%08X, unknown14=0x%X, unknown18=0x%X, unknown20=0x%X, unknown24=0x%X, unknown28=0x%X", command, status, unknown10, unknown14, unknown18, unknown20, unknown24, unknown28));
				}
				break;
			case 0x5A:
				if (log.isDebugEnabled()) {
					log.debug(String.format("Unknown command 0x%X, status=0x%X, unknown10=0x%08X, unknown14=0x%X, unknown18=0x%X, unknown20=0x%X, unknown24=0x%X, unknown28=0x%X", command, status, unknown10, unknown14, unknown18, unknown20, unknown24, unknown28));
				}
				break;
			case 0x5B:
				if (log.isDebugEnabled()) {
					log.debug(String.format("Unknown command 0x%X, status=0x%X, unknown10=0x%08X, unknown14=0x%X, unknown18=0x%X, unknown20=0x%X, unknown24=0x%X, unknown28=0x%X", command, status, unknown10, unknown14, unknown18, unknown20, unknown24, unknown28));
				}
				break;
			case 0x5D:
				if (log.isDebugEnabled()) {
					log.debug(String.format("Unknown command 0x%X, status=0x%X, unknown10=0x%08X, unknown14=0x%X, unknown18=0x%X, unknown20=0x%X, unknown24=0x%X, unknown28=0x%X", command, status, unknown10, unknown14, unknown18, unknown20, unknown24, unknown28));
				}
				break;
			default:
				log.error(String.format("Unknown command 0x%X, status=0x%X", command, status));
				break;
		}

		RuntimeContextLLE.triggerInterrupt(getProcessor(), IntrManager.PSP_ATA_INTR);
	}

	@Override
	public int read32(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x00: value = status; break;
			default: value = super.read32(address); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - read32(0x%08X) returning 0x%08X", getPc() - 4, address, value));
		}

		return value;
	}

	@Override
	public void write32(int address, int value) {
		switch (address - baseAddress) {
			case 0x00: status = value; break;
			case 0x04: power = value; break;
			case 0x08: setCommand(value); break;
			case 0x10: unknown10 = value; break;
			case 0x14: unknown14 = value; break;
			case 0x18: unknown18 = value; break;
			case 0x1C: unknown1C = value; break;
			case 0x20: unknown20 = value; break;
			case 0x24: unknown24 = value; break;
			case 0x28: unknown28 = value; break;
			case 0x2C: unknown2C = value; break;
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc() - 4, address, value, this));
		}
	}

	@Override
	public String toString() {
		return String.format("unknown00=0x%X, power=0x%X, control=0x%X, unknown10=0x%X, unknown14=0x%X, unknown18=0x%X, unknown1C=0x%X, unknown20=0x%X, unknown24=0x%X, unknown28=0x%X", status, power, command, unknown10, unknown14, unknown18, unknown1C, unknown20, unknown24, unknown28);
	}
}
