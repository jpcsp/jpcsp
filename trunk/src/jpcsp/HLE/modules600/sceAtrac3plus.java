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

import jpcsp.HLE.HLEFunction;
import jpcsp.Processor;
import jpcsp.Memory;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.modules.SysMemUserForUser;
import jpcsp.HLE.modules150.SysMemUserForUser.SysMemInfo;

public class sceAtrac3plus extends jpcsp.HLE.modules250.sceAtrac3plus {
    @Override
    public String getName() { return "sceAtrac3plus"; }

    @HLEFunction(nid = 0x231FC6B7, version = 600)
    public void _sceAtracGetContextAddress(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int at3IDNum = cpu.gpr[4];

        log.warn(String.format("PARTIAL: _sceAtracGetContextAddress at3IDNum=%d", at3IDNum));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        SysMemInfo at3ctx = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.USER_PARTITION_ID, String.format("ThreadMan-AtracCtx"), SysMemUserForUser.PSP_SMEM_High, 200, 0);
        mem.write32(at3ctx.addr + 151, 1); // Unknown.

        cpu.gpr[2] = at3ctx.addr;
    }

}