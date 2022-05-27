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
package jpcsp.memory.mmio.wlan;

import static jpcsp.hardware.Wlan.MAC_ADDRESS_LENGTH;

import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.arm.ARMProcessor;

/**
 * @author gid15
 *
 */
public class HLEMemcpy extends BaseHLECall {
	private String getAdditionalInfo(ARMProcessor processor, int dest, int src, int size) {
		String additionalInfo = "";

		// Try to display additional information in case of copying a MAC address
		if (size == MAC_ADDRESS_LENGTH) {
			byte[] macAddress = new byte[MAC_ADDRESS_LENGTH];
			for (int i = 0; i < MAC_ADDRESS_LENGTH; i++) {
				macAddress[i] = (byte) processor.mem.internalRead8(src + i);
			}

			// Display the data as a MAC address when matching my own MAC address or the ANY MAC address
			if (pspNetMacAddress.isMyMacAddress(macAddress) || pspNetMacAddress.isAnyMacAddress(macAddress)) {
				additionalInfo = String.format("MAC Address %s", pspNetMacAddress.toString(macAddress));
			}
		}

		return additionalInfo;
	}

	@Override
	public void call(ARMProcessor processor, int imm) {
		int dest = getParameterValue(processor, 0);
		int src = getParameterValue(processor, 1);
		int size = getParameterValue(processor, 2);

		if (log.isTraceEnabled()) {
			log.trace(String.format("memcpy 0x%08X, 0x%08X, 0x%X: %s%s", dest, src, size, getAdditionalInfo(processor, dest, src, size), getMemoryDump(processor, src, size)));
		}

		// Simply continue with the normal execution of the memcpy code
	}
}
