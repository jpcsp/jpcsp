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

import jpcsp.HLE.TPointer;
import jpcsp.HLE.VFS.AbstractVirtualFileSystem;
import jpcsp.autotests.AutoTestsOutput;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;

public class EmulatorVirtualFileSystem extends AbstractVirtualFileSystem {
	public static final int EMULATOR_DEVCTL_GET_HAS_DISPLAY = 1;
	public static final int EMULATOR_DEVCTL_SEND_OUTPUT = 2;
	public static final int EMULATOR_DEVCTL_IS_EMULATOR = 3;

	@Override
	public int ioDevctl(String deviceName, int command, TPointer inputPointer, int inputLength, TPointer outputPointer, int outputLength) {
    	switch (command) {
			case EMULATOR_DEVCTL_GET_HAS_DISPLAY:
				if (!outputPointer.isAddressGood() || outputLength < 4) {
					return super.ioDevctl(deviceName, command, inputPointer, inputLength, outputPointer, outputLength);
				}
				outputPointer.setValue32(1);
				break;
			case EMULATOR_DEVCTL_SEND_OUTPUT:
				byte[] input = new byte[inputLength];
				IMemoryReader memoryReader = MemoryReader.getMemoryReader(inputPointer.getAddress(), inputLength, 1);
				for (int i = 0; i < inputLength; i++) {
					input[i] = (byte) memoryReader.readNext();
				}
				AutoTestsOutput.appendString(new String(input));
				break;
			case EMULATOR_DEVCTL_IS_EMULATOR:
				break;
			default:
				// Unknown command
				return super.ioDevctl(deviceName, command, inputPointer, inputLength, outputPointer, outputLength);
		}

    	return 0;
	}
}
