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
package jpcsp.HLE.modules630;

import jpcsp.HLE.HLEFunction;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

public class sceAtrac3plus extends jpcsp.HLE.modules600.sceAtrac3plus {
    @Override
    public String getName() { return "sceAtrac3plus"; }

    @HLEFunction(nid = 0x0C116E1B, version = 630)
    public void sceAtracLowLevelDecode(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceAtracLowLevelDecode");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x1575D64B, version = 630)
    public void sceAtracLowLevelInitDecoder(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceAtracLowLevelInitDecoder");

        cpu.gpr[2] = 0xDEADC0DE;
    }

}