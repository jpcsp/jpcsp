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
        int[] gpr = processor.cpu.gpr;
        Memory mem = Processor.memory;

        int name_addr = gpr[4];
        int attr = gpr[5];
        int initCount = gpr[6];
        int maxCount = gpr[7];
        int option_addr = gpr[8];

        String name = readStringZ(mem.mainmemory,
                (name_addr & 0x3fffffff) - MemoryMap.START_RAM);

        Modules.log.debug("sceKernelCreateSema name=" + name + " attr= " + attr + " initCount= " + initCount + " maxCount= " + maxCount + " option_addr= " + option_addr);

        if (option_addr != 0) {
            Modules.log.warn("sceKernelCreateSema: UNSUPPORTED Option Value");
        }
        SceKernelSemaphoreInfo sema = new SceKernelSemaphoreInfo(name, attr, initCount, initCount, maxCount);

        int uid = sema.uid;

        gpr[2] = uid;

        if (-1 < uid) {
            semaMap.put(uid, sema);
        }
    }

    public void sceKernelDeleteSema(Processor processor) {
        int[] gpr = processor.cpu.gpr;
        
        int uid = gpr[4];

        Modules.log.debug("sceKernelDeleteSema id=" + uid);

        SceKernelSemaphoreInfo semaphore = semaMap.get(uid);

        if (semaphore != null) {
            semaphore.release();
            gpr[2] = 0;
        } else {
            Modules.log.warn("sceKernelDeleteSema - invalid id=" + uid);
            gpr[2] = ERROR_NOT_FOUND_SEMAPHORE;
        }
    }

    public void sceKernelSignalSema(Processor processor) {
        int[] gpr = processor.cpu.gpr;
        
        int uid = gpr[4];
        int count = gpr[5];

        Modules.log.debug("sceKernelSignalSema id=" + uid + " count=" + count);

        SceKernelSemaphoreInfo semaphore = semaMap.get(uid);

        if (semaphore != null) {

            semaphore.currentCount += count;
            
            gpr[2] = 0;
        } else {
            Modules.log.warn("sceKernelSignalSema - invalid id=" + uid);
            gpr[2] = ERROR_NOT_FOUND_SEMAPHORE;
        }
    }

    public void sceKernelWaitSema(Processor processor) {
        int[] gpr = processor.cpu.gpr;
        
        int uid = gpr[4];
        int count = gpr[5];
        int timeout_addr = gpr[6];

        Modules.log.debug(String.format("sceKernelWaitSema id=%d count=%d timeout_addr=0x%08x", uid, count, timeout_addr));

        SceKernelSemaphoreInfo semaphore = semaMap.get(uid);

        if (semaphore != null) {
            gpr[2] = 0;
            if (semaphore.currentCount >= count) {
                semaphore.currentCount -= count;
            } else {
                Modules.log.debug(Managers.threads.getCurrentThreadID());
                Managers.threads.setCurrentThreadWaiting();
            }
        } else {
            Modules.log.warn("sceKernelWaitSema - invalid id=" + uid);
            gpr[2] = ERROR_NOT_FOUND_SEMAPHORE;
        }
    }

    public void sceKernelWaitSemaCB(Processor processor) {
        int[] gpr = processor.cpu.gpr;

        int uid = gpr[4];

        SceKernelSemaphoreInfo semaphore = semaMap.get(uid);

        if (semaphore != null) {
            Modules.log.debug("sceKernelWaitSemaCB redirecting to sceKernelWaitSema");
            sceKernelWaitSema(processor);
        } else {
            Modules.log.warn("sceKernelWaitSemaCB - invalid id=" + uid);
            gpr[2] = ERROR_NOT_FOUND_SEMAPHORE;
        }
    }

    public void sceKernelPollSema(Processor processor) {
        int[] gpr = processor.cpu.gpr;

        int uid = gpr[4];

        Modules.log.debug("sceKernelPollSema id=" + uid);

        SceKernelSemaphoreInfo semaphore = semaMap.get(uid);

        if (semaphore != null) {
            Modules.log.debug("sceKernelPollSema - not implemented");
        } else {
            Modules.log.warn("sceKernelPollSema - invalid id=" + uid);
            gpr[2] = ERROR_NOT_FOUND_SEMAPHORE;
        }
    }

    public void sceKernelCancelSema(Processor processor) {
        int[] gpr = processor.cpu.gpr;

        int uid = gpr[4];

        Modules.log.debug("sceKernelCancelSema id=" + uid);

        SceKernelSemaphoreInfo semaphore = semaMap.get(uid);

        if (semaphore != null) {
            Modules.log.debug("sceKernelCancelSema - not implemented");
        } else {
            Modules.log.warn("sceKernelCancelSema - invalid id=" + uid);
            gpr[2] = ERROR_NOT_FOUND_SEMAPHORE;
        }
    }

    public void sceKernelReferSemaStatus(Processor processor) {
        int[] gpr = processor.cpu.gpr;

        int uid = gpr[4];

        Modules.log.debug("sceKernelReferSemaStatus id=" + uid);

        SceKernelSemaphoreInfo semaphore = semaMap.get(uid);

        if (semaphore != null) {
            Modules.log.debug("sceKernelReferSemaStatus - not implemented");
        } else {
            Modules.log.warn("sceKernelReferSemaStatus - invalid id=" + uid);
            gpr[2] = ERROR_NOT_FOUND_SEMAPHORE;
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
