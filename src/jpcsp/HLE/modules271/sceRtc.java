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
package jpcsp.HLE.modules271;

import org.apache.log4j.Logger;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;

@HLELogging
public class sceRtc extends jpcsp.HLE.modules200.sceRtc {
	public static Logger log = jpcsp.HLE.modules150.sceRtc.log;

	@HLEUnimplemented
	@HLEFunction(nid = 0xFB3B18CD, version = 271)
	public int sceRtcRegisterCallback(int callbackId) {
		return 0;
	}
}
