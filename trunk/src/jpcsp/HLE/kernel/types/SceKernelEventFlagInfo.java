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
public class SceKernelEventFlagInfo extends SceKernelUid {

    public int initPattern;
    public int currentPattern;
    public int numWaitThreads;//NOT sure if that should be here or merged with the semaphore waitthreads..

    public SceKernelEventFlagInfo(String name, int attr, int initPattern, int currentPattern) {
        super(name, attr);
        if (-1 < this.getUid()) { 
            this.name = name;
            this.attr = attr;
            this.initPattern = initPattern;
            this.currentPattern = currentPattern;
        }
    }
    
    @Override
    public boolean release() {
        return Managers.eventsFlags.releaseObject(this);
    }
    
    public void sceKernelDeleteEventFlag(Processor processor) {
        CpuState cpu = processor.cpu;
    }

    public void sceKernelSetEventFlag(Processor processor) {
        CpuState cpu = processor.cpu;
    }
    
    public void sceKernelClearEventFlag(Processor processor) {
        CpuState cpu = processor.cpu;
    }
    
    public void sceKernelWaitEventFlag(Processor processor) {
        CpuState cpu = processor.cpu;
        int bitPattern, waitMode, resPattern_addr, timeout_addr;
    }
    
    public void sceKernelWaitEventFlagCB(Processor processor) {
        CpuState cpu = processor.cpu;
        int bitPattern, waitMode, resPattern_addr, timeout_addr;
    }

    public void sceKernelPollEventFlag(Processor processor) {
        CpuState cpu = processor.cpu;
        int bitPattern, waitMode, resPattern_addr, timeout_addr;
    }

    public void sceKernelCancelEventFlag(Processor processor) {
        CpuState cpu = processor.cpu;
        int setPattern, numWaitedThreads_addr;
    }
    
    public void sceKernelReferEventFlagStatus(Processor processor) {
        CpuState cpu = processor.cpu;
        int info_addr;
    }
}
    
