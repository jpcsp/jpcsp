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

import jpcsp.HLE.HLEFunction;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

public class ModuleMgrForUser extends jpcsp.HLE.modules150.ModuleMgrForUser {

    // Export functions

    @HLEFunction(nid = 0xFEF27DC1, version = 271)
    public void ModuleMgrForUser_FEF27DC1(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn(String.format("UNIMPLEMENTED:ModuleMgrForUser_FEF27DC1"
            + " %08X %08X %08X", cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]));

        cpu.gpr[2] = 0;
    }

}