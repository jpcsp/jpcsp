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

import jpcsp.HLE.kernel.Managers;

/**
 *
 * @author hli
 */
public class SceKernelCallbackInfo extends SceKernelUid {

    public int threadId;
    public int callback_addr;
    public int callback_arg_addr;
    public int notifyCount;
    public int notifyArg;

    public SceKernelCallbackInfo(String name, int threadId, int callback_addr, int callback_arg_addr) {
        super(name, 0);
        if (0 < this.getUid()) {
            this.threadId = threadId;
            this.callback_addr = callback_addr;
            this.callback_arg_addr = callback_arg_addr;
            
            notifyCount = 0; // ?
            notifyArg = 0; // ?
        }
    }

    public void sceKernelDeleteCallback(Processor processor) {
        CpuState cpu = processor.cpu;
        
        release();
    }
    
    public void sceKernelNotifyCallback(Processor processor) {
        CpuState cpu = processor.cpu;
    }
    
    public void sceKernelCancelCallback(Processor processor) {
        CpuState cpu = processor.cpu;
    }
    
    public void sceKernelGetCallbackCount(Processor processor) {
        CpuState cpu = processor.cpu;
    }
    
    public void sceKernelCheckCallback(Processor processor) {
        CpuState cpu = processor.cpu;
    }
    
    public void sceKernelReferCallbackStatus(Processor processor) {
        CpuState cpu = processor.cpu;
    }
    
    @Override
    public boolean release() {
        return Managers.callbacks.releaseObject(this);
    }
}
