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
package jpcsp.Allegrex.compiler.nativeCode;

import org.apache.log4j.Logger;

import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.modules.UtilsForUser;
import jpcsp.util.Utilities;

public class sceKernelUtilsSha1Digest extends AbstractNativeCodeSequence {
	public static Logger log = UtilsForUser.log;

	public static void call() {
		TPointer inAddr = getPointer(getGprA0());
		int inSize = getGprA1();
		TPointer outAddr = getPointer(getGprA2());

		int result = Modules.UtilsForUserModule.sceKernelUtilsSha1Digest(inAddr, inSize, outAddr);

		if (log.isDebugEnabled()) {
			log.debug(String.format("sceKernelUtilsSha1Digest returning 0x%X", result));
			log.debug(String.format("outAddr[out]:%s", Utilities.getMemoryDump(outAddr, 20)));
		}

		setGprV0(result);
	}
}
