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
package jpcsp.HLE.VFS.emulator;

import jpcsp.Memory;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.VFS.AbstractVirtualFileSystem;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.autotests.AutoTestsOutput;

public class EmulatorVirtualFileSystem extends AbstractVirtualFileSystem {
	@Override
	public int ioDevctl(String deviceName, int command, TPointer inputPointer, int inputLength, TPointer outputPointer, int outputLength) {
		if (!deviceName.equals("emulator:") && !deviceName.equals("kemulator:")) {
			throw(new SceKernelErrorException(SceKernelErrors.ERROR_ERRNO_DEVICE_NOT_FOUND));
		}
		
    	switch (command) {
			// EMULATOR_DEVCTL__GET_HAS_DISPLAY
			case 0x00000001:
				
				if (!outputPointer.isAddressGood() && outputLength < 4) {
					return -1;
				}

				outputPointer.setValue32(1);
                return 0;

			// EMULATOR_DEVCTL__SEND_OUTPUT
			case 0x00000002:
				AutoTestsOutput.appendString(new String(Memory.getInstance().readChunk(inputPointer.getAddress(), inputLength).array()));
				return 0;

			// EMULATOR_DEVCTL__IS_EMULATOR
			case 0x00000003:
				return 0;

			// Unknown
			default:
				throw(new RuntimeException(String.format("Unknown emulator: cmd 0x%08X", command)));
		}
	}
}
