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

import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

public class ModuleMgrForUser extends jpcsp.HLE.modules150.ModuleMgrForUser {

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        super.installModule(mm, version);

        if (version >= 271) {

            mm.addFunction(0xFEF27DC1, ModuleMgrForUser_FEF27DC1Function);

        }
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        super.uninstallModule(mm, version);

        if (version >= 271) {

            mm.removeFunction(ModuleMgrForUser_FEF27DC1Function);

        }
    }

    // Export functions

    public void ModuleMgrForUser_FEF27DC1(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn(String.format("UNIMPLEMENTED:ModuleMgrForUser_FEF27DC1"
            + " %08X %08X %08X", cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]));

        cpu.gpr[2] = 0;
    }

    public final HLEModuleFunction ModuleMgrForUser_FEF27DC1Function = new HLEModuleFunction("ModuleMgrForUser", "ModuleMgrForUser_FEF27DC1") {

        @Override
        public final void execute(Processor processor) {
            ModuleMgrForUser_FEF27DC1(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ModuleMgrForUserModule.ModuleMgrForUser_FEF27DC1(processor);";
        }
    };
}