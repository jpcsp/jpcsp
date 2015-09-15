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
import org.bolet.jgz.Adler32;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;
import jpcsp.util.Utilities;

public class sceAdler extends HLEModule {
    public static Logger log = Modules.getLogger("sceAdler");
    // Do not use the JDK Adler32 implementation as we need to specify the initial checksum value.
    // This value is always forced to 1 in the JDK Adler32 implementation.
    protected Adler32 adler32;

	@Override
	public void start() {
		adler32 = new Adler32();

		super.start();
	}

	@Override
	public void stop() {
		adler32 = null;

		super.stop();
	}

	@HLEFunction(nid = 0x9702EF11, version = 150)
    public int sceAdler32(int adler, TPointer data, int length) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("sceAdler32 data:%s", Utilities.getMemoryDump(data.getAddress(), length)));
		}

		byte[] b = new byte[length];
		IMemoryReader memoryReader = MemoryReader.getMemoryReader(data.getAddress(), length, 1);
		for (int i = 0; i < length; i++) {
			b[i] = (byte) memoryReader.readNext();
		}

		adler32.init(adler);
		adler32.update(b);
		int result = adler32.getSum();

		if (log.isDebugEnabled()) {
			log.debug(String.format("sceAdler32 returning 0x%08X", result));
		}

		return result;
	}
}
