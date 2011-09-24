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
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;

public class ThreadManForUser extends jpcsp.HLE.modules380.ThreadManForUser {

    @HLEFunction(nid = 0xBC80EC7C, version = 630)
    public void sceKernelExtendThreadStack(Processor processor) {
        CpuState cpu = processor.cpu;

        int size = cpu.gpr[4];
        int entry_addr = cpu.gpr[5];
        int attr = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelExtendThreadStack size=0x%X, entry_addr=0x%X, attr=0x%X", size, entry_addr, attr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (size > 0x200) {
            // sceKernelExtendThreadStack copies the current thread, starts a new one with the same
            // properties, except for the ones passed as arguments for sceKernelExtendThreadStack (size,
            // entry_addr, and attr) and deletes the previous one.
            SceKernelThreadInfo currentThread = Modules.ThreadManForUserModule.getCurrentThread();
            currentThread.freeStack();
            SceKernelThreadInfo expandedThread = Modules.ThreadManForUserModule.hleKernelCreateThread(currentThread.name,
                    entry_addr, currentThread.currentPriority, size,
                    attr, 0);
            cpu.gpr[2] = 0;
            Modules.ThreadManForUserModule.hleKernelExitDeleteThread();
            Modules.ThreadManForUserModule.hleKernelStartThread(expandedThread, 0, 0, expandedThread.gpReg_addr);
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_ILLEGAL_STACK_SIZE;
        }
        
    }
}