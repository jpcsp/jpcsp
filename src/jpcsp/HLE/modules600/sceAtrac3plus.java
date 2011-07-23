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
package jpcsp.HLE.modules600;

import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

public class sceAtrac3plus extends jpcsp.HLE.modules250.sceAtrac3plus {
    @Override
    public String getName() { return "sceAtrac3plus"; }

    @Override
    public void installModule(HLEModuleManager mm, int version) {
    	super.installModule(mm, version);

    	if (version >= 600) {
            mm.addFunction(0x231FC6B7, _sceAtracGetContextAddressFunction);
        }
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
    	super.uninstallModule(mm, version);

    	if (version >= 600) {
            mm.removeFunction(_sceAtracGetContextAddressFunction);
        }
    }

    public void _sceAtracGetContextAddress(Processor processor) {
        CpuState cpu = processor.cpu;

        int at3IDNum = cpu.gpr[4];

        log.warn(String.format("PARTIAL: _sceAtracGetContextAddress at3IDNum=%d", at3IDNum));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        // Always returns 0, but it may change the internal context address (at3IDNum).
        cpu.gpr[2] = 0;
    }

    public final HLEModuleFunction _sceAtracGetContextAddressFunction = new HLEModuleFunction("sceAtrac3plus", "_sceAtracGetContextAddress") {
        @Override
        public final void execute(Processor processor) {
        	_sceAtracGetContextAddress(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAtrac3plusModule._sceAtracGetContextAddress(processor);";
        }
    };
}