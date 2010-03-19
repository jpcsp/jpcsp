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
package jpcsp.HLE.kernel.types;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Allegrex.CpuState;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.util.Utilities;

import jpcsp.HLE.ThreadMan;
import jpcsp.HLE.kernel.managers.SceUidManager;

public class SceKernelCallbackInfo {

    public static final int size = 56;
    public final String name;
    public final int threadId;
    public final int callback_addr;
    public final int callback_arg_addr;
    public int notifyCount;
    public int notifyArg;

    // internal variables
    public final int uid;

    public SceKernelCallbackInfo(String name, int threadId, int callback_addr, int callback_arg_addr) {
        this.name = name;
        this.threadId = threadId;
        this.callback_addr = callback_addr;
        this.callback_arg_addr = callback_arg_addr;
        this.notifyCount = 0;
        this.notifyArg = 0;

        // internal state
        uid = SceUidManager.getNewUid("ThreadMan-callback");

        // TODO (hlide ?)
        //SceModule *mod = sceKernelFindModuleByAddress(callback_addr);
        //this.gpreg = (mod == 0) ? gpr[GP] : mod->unk_68;
    }

    public void write(Memory mem, int address) {
        mem.write32(address, size);
        Utilities.writeStringNZ(mem, address + 4, 32, name);
        mem.write32(address + 36, threadId);
        mem.write32(address + 40, callback_addr);
        mem.write32(address + 44, callback_arg_addr);
        mem.write32(address + 48, notifyCount);
        mem.write32(address + 52, notifyArg);
    }

    /** Call this to switch in the callback.
     * Sets up a copy of the parent thread's context for the callback to run in.
     * @param thread the thread this callback belongs to.
     */
    public void startContext(SceKernelThreadInfo thread) {
        CpuState cpu = new CpuState(thread.cpuContext);

        cpu.pc = cpu.npc = callback_addr;
        cpu.gpr[4] = notifyCount;
        cpu.gpr[5] = notifyArg;
        cpu.gpr[6] = callback_arg_addr;
        cpu.gpr[31] = 0; // ra

        // clear the counter and the arg
        notifyCount = 0;
        notifyArg = 0;

        Emulator.getProcessor().cpu = cpu;

        RuntimeContext.executeCallback(thread);
    }

	@Override
	public String toString() {
		return String.format("name:'%s', thread:'%s', PC:%08X, $a0:%08X, $a1: %08X, $a2: %08X", name, ThreadMan.getInstance().getThreadName(threadId), callback_addr, notifyCount, notifyArg, callback_arg_addr);
	}
}
