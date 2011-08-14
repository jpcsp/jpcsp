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
package jpcsp.HLE.modules352;

import jpcsp.HLE.HLEFunction;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

public class sceMpeg extends jpcsp.HLE.modules150.sceMpeg {

	@Override
	public String getName() {
        return "sceMpeg";
	}

	@Override
	public void installModule(HLEModuleManager mm, int version) { mm.installModuleWithAnnotations(this, version); }

	@Override
	public void uninstallModule(HLEModuleManager mm, int version) { mm.uninstallModuleWithAnnotations(this, version); }

    @HLEFunction(nid = 0x769BEBB6, version = 352)
    public void sceMpegRingbufferQueryPackNum(Processor processor) {
        CpuState cpu = processor.cpu;

        int memorySize = cpu.gpr[4];

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceMpegRingbufferQueryPackNum memorySize=0x%08X", memorySize));
        }

        cpu.gpr[2] = getPacketsFromSize(memorySize);
    }

}
