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
        if (-1 < this.getUid()) {
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

    public void sceKernelDeleteCallback(Processor processor) {
        Modules.log.debug("sceKernelDeleteCallback id=" + processor.cpu.gpr[4]);
        release();
    }
    
    public void sceKernelNotifyCallback(Processor processor) {
        int[] gpr = processor.cpu.gpr;
        
        //this.thread.callbackNotify = true;
        this.notifyArg = gpr[5];
        this.notifyCount++;

        Modules.log.debug(String.format("sceKernelNotifyCallback PARTIALLY implemented id=%s arg=0x%08x", gpr[4], notifyArg));

        //if (this.thread.isCallback)
        //{
        //  if (this.thread.status == THREAD_WAITING || this.thread.status == (THREAD_WAITING | THREAD_SUSPEND))
        //  {
        //    int s0 = sub_0000022C(this.thread);
        //
        //    this.thread.callbackStatus = KERNEL_ERROR_NOTIFY_CALLBACK;
        //    if (this.thread.waitType != 0)
        //    {
        //      s0 += sub_000005F4(thread.thread.waitType);
        //    }
        //    if (s0 != 0)
        //    {
        //      gInfo.nextThread = 0;
        //      _ReleaseWaitThread(0);
        //    }
        //  }
        //}
        //return KERNEL_ERROR_OK;
        
        processor.cpu.gpr[2] = 0;
    }
    
    public void sceKernelCancelCallback(Processor processor) {
        int[] gpr = processor.cpu.gpr;
        
        Modules.log.debug("sceKernelCancelCallback id=" + gpr[4]);
        
        this.notifyArg = 0;
        this.callback_arg_addr = 0;

        gpr[2] = 0;
    }
    
    public void sceKernelGetCallbackCount(Processor processor) {
        int[] gpr = processor.cpu.gpr;
        
        Modules.log.debug("sceKernelGetCallbackCount id=" + gpr[4]);

        gpr[2] = this.notifyCount;
    }
        
    public void sceKernelReferCallbackStatus(Processor processor) {
        int[] gpr = processor.cpu.gpr;
        Memory mem = Processor.memory;
        
        Modules.log.debug("sceKernelReferCallbackStatus id=" + gpr[4]);
        
        int addr = gpr[5];
        
        int i, len = Math.min(this.name.length(), 31);

        mem.write32(addr, 1*4+32*1+5*4); //struct size
        for (addr += 4, i = 0; i < len; i++)
            mem.write8(addr++, (byte)this.name.charAt(i));
        for (; i < 32; i++)
            mem.write8(addr++, (byte)0);
        mem.write32(addr + 0 + 0 + 0 + 0, this.thread.uid);
        mem.write32(addr + 4 + 0 + 0 + 0, this.callback_addr);
        mem.write32(addr + 4 + 4 + 0 + 0, this.callback_arg_addr);
        mem.write32(addr + 4 + 4 + 4 + 0, this.notifyCount);
        mem.write32(addr + 4 + 4 + 4 + 4, this.notifyArg);
        
        gpr[2] = 0;
    }
    
    @Override
    public boolean release() {
        return Managers.callbacks.releaseObject(this);
    }
}
