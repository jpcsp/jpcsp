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

import static jpcsp.Emulator.getProcessor;
import static jpcsp.HLE.kernel.managers.IntrManager.PSP_MECODEC_INTR;

import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.Modules;

public class MMIOHandlerSystemControl extends MMIOHandlerReadWrite {
	public static final int BASE_ADDRESS = 0xBC100000;
	public static final int SIZE_OF = 0x9C;
	private static MMIOHandlerSystemControl instance;

	public static MMIOHandlerSystemControl getInstance() {
		if (instance == null) {
			instance = new MMIOHandlerSystemControl(BASE_ADDRESS, SIZE_OF);
		}
		return instance;
	}

	private MMIOHandlerSystemControl(int baseAddress, int length) {
		super(baseAddress, length);
    	write32(BASE_ADDRESS + 0x40, Modules.sceSysregModule.sceSysregGetTachyonVersion() << 8);
    	write32(BASE_ADDRESS + 0x98, Modules.sceSysregModule.sceSysregGetFuseConfig());
	}

	private void sysregInterruptToOther(int value) {
		if (value != 0) {
			RuntimeContextLLE.triggerInterrupt(getProcessor(), PSP_MECODEC_INTR);
		}
	}

	@Override
	public void write32(int address, int value) {
		switch (address - baseAddress) {
			case 0x44: sysregInterruptToOther(value); break;
			default: super.write32(address, value); break;
		}
	}
}
