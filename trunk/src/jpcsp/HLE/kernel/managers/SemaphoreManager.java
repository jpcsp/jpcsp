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

/**
 *
 * @author hli
 */
public class SemaphoreManager {

    private static HashMap<Integer, SceKernelSemaphoreInfo> semaphoreMap;
    
    public static boolean isUidValid(int uid) {
        return semaphoreMap.containsKey(uid);
    }
       
    public void sceKernelCreateSema(Processor processor) {
    }
    
    public void sceKernelDeleteSema(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];
        
        if (-1 < uid) {
            SceKernelSemaphoreInfo semaphore = semaphoreMap.get(uid);
        
            semaphore.sceKernelDeleteSema(processor);
        }
    }

    public void sceKernelSignalSema(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];
        
        if (-1 < uid) {
            SceKernelSemaphoreInfo semaphore = semaphoreMap.get(uid);
        
            semaphore.sceKernelSignalSema(processor);
        }
    }

    public void sceKernelWaitSema(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];
        
        if (-1 < uid) {
            SceKernelSemaphoreInfo semaphore = semaphoreMap.get(uid);
        
            semaphore.sceKernelWaitSema(processor);
        }
    }

    public void sceKernelWaitSemaCB(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];
        
        if (-1 < uid) {
            SceKernelSemaphoreInfo semaphore = semaphoreMap.get(uid);
        
            semaphore.sceKernelWaitSemaCB(processor);
        }
    }

    public void sceKernelPollSema(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];
        
        if (-1 < uid) {
            SceKernelSemaphoreInfo semaphore = semaphoreMap.get(uid);
        
            semaphore.sceKernelDeleteSema(processor);
        }
    }

    public void sceKernelCancelSema(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];
        
        if (-1 < uid) {
            SceKernelSemaphoreInfo semaphore = semaphoreMap.get(uid);
        
            semaphore.sceKernelCancelSema(processor);
        }
    }

    public void sceKernelReferSemaStatus(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];
        
        if (-1 < uid) {
            SceKernelSemaphoreInfo semaphore = semaphoreMap.get(uid);
        
            semaphore.sceKernelReferSemaStatus(processor);
        }
    }

    
    public boolean releaseObject(SceKernelUid object) {
        if (Managers.uids.removeObject(object)) {
            semaphoreMap.remove(object.getUid());
            return true;
        }
        return false;
    }      

    public static void reset() {
        semaphoreMap = new HashMap<Integer, SceKernelSemaphoreInfo>();
    }
    
    public static final SemaphoreManager singleton;
    
    private SemaphoreManager() {
    }
        
    static {
        singleton = new SemaphoreManager();
        reset();
    }    
}
