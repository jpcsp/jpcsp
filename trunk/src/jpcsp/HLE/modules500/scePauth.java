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
package jpcsp.HLE.modules500;

import org.apache.log4j.Logger;

import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;

public class scePauth extends HLEModule {
    protected static Logger log = Modules.getLogger("scePauth");

    @Override
	public String getName() {
		return "scePauth";
	}

    @HLEFunction(nid = 0xF7AA47F6, version = 500)
    public int scePauth_F7AA47F6(int inputAddr, int inputLength, int resultLengthAddr, int workArea) {
    	// workArea is 16 bytes long
    	log.warn(String.format("Unimplemented scePauth_F7AA47F6 inputAddr=0x%08X, inputLength=%d, resultLengthAddr=0x%08X, workArea=0x%08X", inputAddr, inputLength, resultLengthAddr, workArea));

    	if (Memory.isAddressGood(resultLengthAddr)) {
    		Processor.memory.write32(resultLengthAddr, 0);
    	}

    	return 0;
    }

    @HLEFunction(nid = 0x98B83B5D, version = 500)
    public int scePauth_98B83B5D(int inputAddr, int inputLength, int resultLengthAddr, int workArea) {
    	// workArea is 16 bytes long
    	log.warn(String.format("Unimplemented scePauth_98B83B5D inputAddr=0x%08X, inputLength=%d, resultLengthAddr=0x%08X, workArea=0x%08X", inputAddr, inputLength, resultLengthAddr, workArea));

    	if (Memory.isAddressGood(resultLengthAddr)) {
    		Processor.memory.write32(resultLengthAddr, 0);
    	}

    	return 0;
    }
}
