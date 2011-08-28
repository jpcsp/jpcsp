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
package jpcsp.HLE.modules150;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.Processor;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEStartModule;

import org.apache.log4j.Logger;

public class sceMp4 extends HLEModule implements HLEStartModule {

    protected static Logger log = Modules.getLogger("sceMp4");

    @Override
    public String getName() {
        return "sceMp4";
    }

    @Override
	public void start() {
	}

	@Override
	public void stop() {
	}

	@HLEUnimplemented(partial = true)
    @HLEFunction(nid = 0x68651CBC, version = 150, checkInsideInterrupt = true)
    public int sceMp4Init(boolean unk1, boolean unk2) {
        log.warn("PARTIAL: sceMp4Init (unk1=%s" + unk1 + ", unk2=%s" + unk2 + ")");

        return 0;
    }

    @HLEUnimplemented(partial = true)
    @HLEFunction(nid = 0x9042B257, version = 150, checkInsideInterrupt = true)
    public int sceMp4Finish(Processor processor) {
        log.warn("PARTIAL: sceMp4Finish");

        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB1221EE7, version = 150, checkInsideInterrupt = true)
    public int sceMp4Create() {
        log.warn("UNIMPLEMENTED: sceMp4Create");

        return 0;
    }

    @HLEFunction(nid = 0x538C2057, version = 150)
    public int sceMp4Delete() {
        log.warn("UNIMPLEMENTED: sceMp4Delete");

        return 0;
    }

}