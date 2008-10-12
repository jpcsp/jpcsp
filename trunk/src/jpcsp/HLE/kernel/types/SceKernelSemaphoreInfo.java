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

import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.Managers;

import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;

/**
 *
 * @author hli
 */
public class SceKernelSemaphoreInfo extends SceKernelUid {

    public int initCount;
    public int currentCount;
    public int maxCount;

    public SceKernelSemaphoreInfo(String name, int attr, int initCount, int currentCount, int maxCount) {
        super(name, attr);
        if (-1 < this.getUid()) {
            this.initCount = initCount;
            this.currentCount = currentCount;
            this.maxCount = maxCount;
        }
    }

    public void sceKernelDeleteSema(Processor processor) {
        Modules.log.debug("sceKernelDeleteSema id=" + processor.cpu.gpr[4]);
        release();
    }

    public void sceKernelSignalSema(Processor processor) {
        CpuState cpu = processor.cpu;
        int[] gpr = cpu.gpr;

        int id = gpr[4];
        int count = gpr[5];

        Modules.log.debug("sceKernelSignalSema id=" + id + " count=" + count);
        
        currentCount += count;

        gpr[2] = 0;
    }

    public void sceKernelWaitSema(Processor processor) {
        CpuState cpu = processor.cpu;
        int[] gpr = cpu.gpr;

        int id = gpr[4];
        int count = gpr[5];
        int timeout_addr = gpr[6];

        Modules.log.debug(String.format("sceKernelWaitSema id=%d count=%d timeout_addr=0x%08x", id, count, timeout_addr));

        gpr[2] = 0;
        if (currentCount >= count) {
            currentCount -= count;
        } else {
            Modules.log.debug(Managers.threads.getCurrentThreadID());
            Managers.threads.setCurrentThreadWaiting();
        }
    }

    public void sceKernelWaitSemaCB(Processor processor) {
        Modules.log.debug("sceKernelWaitSemaCB redirecting to sceKernelWaitSema");
        sceKernelWaitSema(processor);
    }

    public void sceKernelPollSema(Processor processor) {
        Modules.log.debug("sceKernelPollSema not implemented");
    }

    public void sceKernelCancelSema(Processor processor) {
        Modules.log.debug("sceKernelCancelSema not implemented");
    }

    public void sceKernelReferSemaStatus(Processor processor) {
        Modules.log.debug("sceKernelReferSemaStatus not implemented");
    }

    @Override
    public boolean release() {
        return Managers.sempahores.releaseObject(this);
    }
}
