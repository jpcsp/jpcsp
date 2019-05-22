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

import jpcsp.Memory;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.modules.UtilsForKernel;
import jpcsp.util.Utilities;

public class sceKernelDeflateDecompress extends AbstractNativeCodeSequence {
	public static Logger log = UtilsForKernel.log;

	public static void call() {
		Memory mem = getMemory();
		TPointer dest = new TPointer(mem, getGprA0());
		int destSize = getGprA1();
		int srcAddr = getGprA2();
		TPointer src = new TPointer(mem, srcAddr);
		TPointer32 endOfDecompressedDestAddr = new TPointer32(mem, getGprA3());

		int result = Modules.UtilsForKernelModule.sceKernelDeflateDecompress(dest, destSize, src, endOfDecompressedDestAddr);

		// Preserve the higher bits of the src address
		if (endOfDecompressedDestAddr.isNotNull()) {
			endOfDecompressedDestAddr.setValue(endOfDecompressedDestAddr.getValue() | (srcAddr & ~Memory.addressMask));
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("sceKernelDeflateDecompress returning 0x%X", result));
			log.debug(String.format("dest[out]:%s", Utilities.getMemoryDump(dest, destSize)));
			log.debug(String.format("endOfDecompressedDestAddr[out]: 0x%08X", endOfDecompressedDestAddr.getValue()));
		}

		setGprV0(result);
	}
}
