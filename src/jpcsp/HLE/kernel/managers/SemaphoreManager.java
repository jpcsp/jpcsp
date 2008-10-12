/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jpcsp.HLE.kernel.managers;

import java.util.HashMap;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import static jpcsp.util.Utilities.*;

import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.types.*;
import static jpcsp.HLE.kernel.types.SceKernelErrors.*;

/**
 *
 * @author hli
 */
public class SemaphoreManager {

    private static HashMap<Integer, SceKernelSemaphoreInfo> semaMap;

    public boolean isUidValid(int uid) {
        return semaMap.containsKey(uid);
    }

    public void sceKernelCreateSema(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int name_addr = cpu.gpr[4];
        int attr = cpu.gpr[5];
        int initCount = cpu.gpr[6];
        int maxCount = cpu.gpr[7];
        int option_addr = cpu.gpr[8];

        String name = readStringZ(mem.mainmemory,
                (name_addr & 0x3fffffff) - MemoryMap.START_RAM);

        Modules.log.debug("sceKernelCreateSema name=" + name + " attr= " + attr + " initCount= " + initCount + " maxCount= " + maxCount + " option_addr= " + option_addr);

        if (option_addr != 0) {
            Modules.log.warn("sceKernelCreateSema: UNSUPPORTED Option Value");
        }
        SceKernelSemaphoreInfo sema = new SceKernelSemaphoreInfo(name, attr, initCount, initCount, maxCount);

        int uid = sema.uid;

        cpu.gpr[2] = uid;

        if (-1 < uid) {
            semaMap.put(uid, sema);
        }
    }

    public void sceKernelDeleteSema(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];

        SceKernelSemaphoreInfo semaphore = semaMap.get(uid);

        if (semaphore != null) {
            semaphore.sceKernelDeleteSema(processor);
        } else {
            Modules.log.warn("sceKernelDeleteSema - invalid semaphore id " + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_SEMAPHORE;
        }
    }

    public void sceKernelSignalSema(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];

        SceKernelSemaphoreInfo semaphore = semaMap.get(uid);

        if (semaphore != null) {
            semaphore.sceKernelSignalSema(processor);
        } else {
            Modules.log.warn("sceKernelSignalSema - invalid semaphore id " + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_SEMAPHORE;
        }
    }

    public void sceKernelWaitSema(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];

        SceKernelSemaphoreInfo semaphore = semaMap.get(uid);

        if (semaphore != null) {
            semaphore.sceKernelWaitSema(processor);
        } else {
            Modules.log.warn("sceKernelWaitSema - invalid semaphore id " + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_SEMAPHORE;
        }
    }

    public void sceKernelWaitSemaCB(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];

        SceKernelSemaphoreInfo semaphore = semaMap.get(uid);

        if (semaphore != null) {
            semaphore.sceKernelWaitSemaCB(processor);
        } else {
            Modules.log.warn("sceKernelWaitSemaCB - invalid semaphore id " + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_SEMAPHORE;
        }
    }

    public void sceKernelPollSema(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];

        SceKernelSemaphoreInfo semaphore = semaMap.get(uid);

        if (semaphore != null) {
            semaphore.sceKernelDeleteSema(processor);
        } else {
            Modules.log.warn("sceKernelPollSema - invalid semaphore id " + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_SEMAPHORE;
        }
    }

    public void sceKernelCancelSema(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];

        SceKernelSemaphoreInfo semaphore = semaMap.get(uid);

        if (semaphore != null) {
            semaphore.sceKernelCancelSema(processor);
        } else {
            Modules.log.warn("sceKernelCancelSema - invalid semaphore id " + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_SEMAPHORE;
        }
    }

    public void sceKernelReferSemaStatus(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];

        SceKernelSemaphoreInfo semaphore = semaMap.get(uid);

        if (semaphore != null) {
            semaphore.sceKernelReferSemaStatus(processor);
        } else {
            Modules.log.warn("sceKernelReferSemaStatus - invalid semaphore id " + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_SEMAPHORE;
        }
    }

    public boolean releaseObject(SceKernelUid object) {
        if (Managers.uids.removeObject(object)) {
            semaMap.remove(object.getUid());
            return true;
        }
        return false;
    }

    public void reset() {
        semaMap = new HashMap<Integer, SceKernelSemaphoreInfo>();
    }
    public static final SemaphoreManager singleton;

    private SemaphoreManager() {
    }
    

    static {
        singleton = new SemaphoreManager();
        singleton.reset();
    }
}
