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
package jpcsp.memory.mmio.syscon;

import jpcsp.HLE.Modules;
import jpcsp.nec78k0.sfr.Nec78k0I2c;

/**
 * @author gid15
 *
 */
public class SysconI2c extends Nec78k0I2c {
	// I2c slave addresses
	public static final int I2C_SLAVE_ADDRESS_POLESTAR = 0xAA;
	public static final int I2C_SLAVE_ADDRESS_POMMEL   = 0xD0;

	public SysconI2c(MMIOHandlerSysconFirmwareSfr sfr) {
		super(sfr);
	}

	@Override
	protected void performRead() {
		int registerNumber;
		int registerValue;
		switch (getSlaveAddress()) {
			case I2C_SLAVE_ADDRESS_POMMEL:
				registerNumber = buffer[0];
				registerValue = Modules.sceSysconModule.readPommelRegister(registerNumber);
				if (log.isDebugEnabled()) {
					log.debug(String.format("readPommelRegister(0x%02X)=0x%04X", registerNumber, registerValue));
				}
				buffer[0] = (registerValue     ) & 0xFF;
				buffer[1] = (registerValue >> 8) & 0xFF;
				break;
			case I2C_SLAVE_ADDRESS_POLESTAR:
				registerNumber = buffer[0];
				registerValue = Modules.sceSysconModule.readPolestarRegister(registerNumber);
				if (log.isDebugEnabled()) {
					log.debug(String.format("readPolestarRegister(0x%02X)=0x%04X", registerNumber, registerValue));
				}
				buffer[0] = (registerValue     ) & 0xFF;
				buffer[1] = (registerValue >> 8) & 0xFF;
				break;
			default:
				log.error(String.format("I2c unimplemented read from slaveAddress 0x%02X", getSlaveAddress()));
				break;
		}
	}
}
