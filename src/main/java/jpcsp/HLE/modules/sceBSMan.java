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
package jpcsp.HLE.modules;

import org.apache.log4j.Logger;

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.DebugMemory;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;

public class sceBSMan extends HLEModule {
    public static Logger log = Modules.getLogger("sceBSMan");

	@HLEUnimplemented
	@HLEFunction(nid = 0x46ACDAE3, version = 660)
	public int sceBSMan_46ACDAE3(@DebugMemory @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=11, usage=Usage.out) TPointer buffer) {
		buffer.setValue16(0, (short) 126); // Valid values are [68..126]
		buffer.setValue16(2, (short) 0); // 0 or 1

		// The following bytes are only evaluated when the value at offset 0 is 126.
		buffer.setValue16(4, (short) 5); // Only valid value is 5 (length of following structure?)

		int unknown678 = 0x080046; // Possible values: 0x080046, 0x001958
		buffer.setValue8(6, (byte) ((unknown678 >> 16) & 0xFF));
		buffer.setValue8(7, (byte) ((unknown678 >>  8) & 0xFF));
		buffer.setValue8(8, (byte) ((unknown678 >>  0) & 0xFF));

		int unknown9A = 0x0000; // Possible values: 0x0000, 0x0001, 0x0002
		buffer.setValue8(9, (byte) ((unknown9A >> 8) & 0xFF));
		buffer.setValue8(10, (byte) ((unknown9A >> 0) & 0xFF));

		return 0;
	}
}
