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

public class scePower extends jpcsp.HLE.modules150.scePower {

    @Override
    public void installModule(HLEModuleManager mm, int version) { mm.installModuleWithAnnotations(this, version); }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) { mm.uninstallModuleWithAnnotations(this, version); }

    public void scePower_469989AD(Processor processor) {
        CpuState cpu = processor.cpu;

        // Identical to scePower_EBD177D6.
        pllClock = cpu.gpr[4];
        cpuClock = cpu.gpr[5];
        busClock = cpu.gpr[6];

        log.debug("scePower_469989AD pll:" + pllClock + " cpu:" + cpuClock + " bus:" + busClock);

        cpu.gpr[2] = 0;
    }
    @HLEFunction(nid = 0x469989AD, version = 630)
    public final HLEModuleFunction scePower_469989ADFunction = new HLEModuleFunction("scePower", "scePower_469989AD") {

        @Override
        public final void execute(Processor processor) {
            scePower_469989AD(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePowerModule.scePower_469989AD(processor);";
        }
    };
}