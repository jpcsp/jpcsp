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
public class EventFlagManager {

    private static HashMap<Integer, SceKernelEventFlagInfo> eventFlagMap;
    
    public boolean isUidValid(int uid) {
        return eventFlagMap.containsKey(uid);
    }
    
    public void sceKernelCreateEventFlag(Processor processor)
    {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;
        
        int name_addr = cpu.gpr[4];
        int attr = cpu.gpr[5];
        int initPattern = cpu.gpr[6];
        int option = cpu.gpr[7];
        
        String name = readStringZ(mem.mainmemory,
            (name_addr & 0x3fffffff) - MemoryMap.START_RAM);

        Modules.log.debug("sceKernelCreateEventFlag name=" + name + " attr= " + attr + " initPattern= " + initPattern+ " option= " + option);

        if (option != 0) {
            Modules.log.warn("sceKernelCreateEventFlag: UNSUPPORTED Option Value");
        }
        
        // initPattern and currentPattern should be the same at init
        SceKernelEventFlagInfo event = new SceKernelEventFlagInfo(name, attr, initPattern, initPattern);

        int uid = event.getUid();
        
        cpu.gpr[2] = uid;
        
        if (0 < uid) {
            eventFlagMap.put(uid, event);
        }
    }

    public void sceKernelDeleteEventFlag(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];
        
        if (-1 < uid) {
            SceKernelEventFlagInfo event = eventFlagMap.get(uid);
        
            event.sceKernelDeleteEventFlag(processor);
        }
    }

    public void sceKernelSetEventFlag(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];
        
        if (-1 < uid) {
            SceKernelEventFlagInfo event = eventFlagMap.get(uid);
        
            event.sceKernelSetEventFlag(processor);
        }
    }
    
    public void sceKernelClearEventFlag(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];
        
        if (-1 < uid) {
            SceKernelEventFlagInfo event = eventFlagMap.get(uid);
        
            event.sceKernelClearEventFlag(processor);
        }
    }
    
    public void sceKernelWaitEventFlag(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];
        
        if (-1 < uid) {
            SceKernelEventFlagInfo event = eventFlagMap.get(uid);
        
            event.sceKernelWaitEventFlag(processor);
        }
    }
    
    public void sceKernelWaitEventFlagCB(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];
        
        if (-1 < uid) {
            SceKernelEventFlagInfo event = eventFlagMap.get(uid);
        
            event.sceKernelWaitEventFlagCB(processor);
        }
    }

    public void sceKernelPollEventFlag(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];
        
        if (-1 < uid) {
            SceKernelEventFlagInfo event = eventFlagMap.get(uid);
        
            event.sceKernelPollEventFlag(processor);
        }
    }

    public void sceKernelCancelEventFlag(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];
        
        if (-1 < uid) {
            SceKernelEventFlagInfo event = eventFlagMap.get(uid);
        
            event.sceKernelCancelEventFlag(processor);
        }
    }
    
    public void sceKernelReferEventFlagStatus(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];
        
        if (-1 < uid) {
            SceKernelEventFlagInfo event = eventFlagMap.get(uid);
        
            event.sceKernelReferEventFlagStatus(processor);
        }
    }
    
    public boolean releaseObject(SceKernelUid object) {
        if (Managers.uids.removeObject(object)) {
            eventFlagMap.remove(object.getUid());
            return true;
        }
        return false;
    }      
    
    public void reset() {
        eventFlagMap = new HashMap<Integer, SceKernelEventFlagInfo>();
    }
    
    public static final EventFlagManager singleton;
    
    private EventFlagManager() {
    }
            
    static {
        singleton = new EventFlagManager();
        singleton.reset();
    }    
}
