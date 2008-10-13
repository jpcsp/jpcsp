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

import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import static jpcsp.util.Utilities.*;

import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.Managers;

/**
 *
 * @author hli
 */
public class SceKernelCallbackInfo extends SceKernelUid {

    public SceKernelThreadInfo thread;
    public int callback_addr;
    public int callback_arg_addr;
    public int notifyCount;
    public int notifyArg;
    public int gpreg;

    public SceKernelCallbackInfo(String name, int callback_addr, int callback_arg_addr) {
        super(name, 0);
        if (-1 < this.uid) {
            this.thread = Managers.threads.currentThread;
            this.callback_addr = callback_addr;
            this.callback_arg_addr = callback_arg_addr;
            this.notifyCount = 0;
            this.notifyArg = 0;

            // TODO
            //SceModule *mod = sceKernelFindModuleByAddress(callback_addr);
            //this.gpreg = (mod == 0) ? gpr[GP] : mod->unk_68;
        }
    }
    
    public void release() {
        Managers.callbacks.releaseObject(this);
    }    
}
