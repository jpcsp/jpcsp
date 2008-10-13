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
            Modules.log.warn("sceKernelCreateEventFlag - UNSUPPORTED Option Value");
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
        int[] gpr = processor.cpu.gpr;

        int uid = gpr[4];
       
        SceKernelEventFlagInfo event = eventFlagMap.get(uid);

        Modules.log.debug("sceKernelDeleteEventFlag id=" + uid);
        
        if (event != null) {
            event.release();
            gpr[2] = 0;
        } else {
            Modules.log.warn("sceKernelDeleteEventFlag - invalid id=" + uid);
            gpr[2] = ERROR_NOT_FOUND_CALLBACK;
        }
    }

    public void sceKernelSetEventFlag(Processor processor) {
        int[] gpr = processor.cpu.gpr;

        int uid = gpr[4];
       
        Modules.log.debug("sceKernelSetEventFlag id=" + uid);
        
        SceKernelEventFlagInfo event = eventFlagMap.get(uid);
       
        if (event != null) {
            Modules.log.warn("sceKernelSetEventFlag - Not implemented");
        } else {
            Modules.log.warn("sceKernelSetEventFlag - invalid id=" + uid);
            gpr[2] = ERROR_NOT_FOUND_CALLBACK;
        }
    }
    
    public void sceKernelClearEventFlag(Processor processor) {
        int[] gpr = processor.cpu.gpr;

        int uid = gpr[4];
       
        Modules.log.debug("sceKernelClearEventFlag id=" + uid);
        
        SceKernelEventFlagInfo event = eventFlagMap.get(uid);
       
        if (event != null) {
            Modules.log.warn("sceKernelClearEventFlag - Not implemented");
        } else {
            Modules.log.warn("sceKernelClearEventFlag - invalid id=" + uid);
            gpr[2] = ERROR_NOT_FOUND_CALLBACK;
        }
    }
    
    public void sceKernelWaitEventFlag(Processor processor) {
        int[] gpr = processor.cpu.gpr;

        int uid = gpr[4];
       
        Modules.log.debug("sceKernelWaitEventFlag id=" + uid);
        
        SceKernelEventFlagInfo event = eventFlagMap.get(uid);
       
        if (event != null) {
            Modules.log.warn("sceKernelWaitEventFlag - Not implemented");
        } else {
            Modules.log.warn("sceKernelWaitEventFlag - invalid id=" + uid);
            gpr[2] = ERROR_NOT_FOUND_CALLBACK;
        }
    }
    
    public void sceKernelWaitEventFlagCB(Processor processor) {
        int[] gpr = processor.cpu.gpr;

        int uid = gpr[4];
       
        Modules.log.debug("sceKernelWaitEventFlagCB id=" + uid);
        
        SceKernelEventFlagInfo event = eventFlagMap.get(uid);
       
        if (event != null) {
            Modules.log.warn("sceKernelWaitEventFlagCB - Not implemented");
        } else {
            Modules.log.warn("sceKernelWaitEventFlagCB - invalid id=" + uid);
            gpr[2] = ERROR_NOT_FOUND_CALLBACK;
        }
    }

    public void sceKernelPollEventFlag(Processor processor) {
        int[] gpr = processor.cpu.gpr;

        int uid = gpr[4];
       
        Modules.log.debug("sceKernelPollEventFlag id=" + uid);
        
        SceKernelEventFlagInfo event = eventFlagMap.get(uid);
       
        if (event != null) {
            Modules.log.warn("sceKernelPollEventFlag - Not implemented");
        } else {
            Modules.log.warn("sceKernelPollEventFlag - invalid id=" + uid);
            gpr[2] = ERROR_NOT_FOUND_CALLBACK;
        }
    }

    public void sceKernelCancelEventFlag(Processor processor) {
        int[] gpr = processor.cpu.gpr;

        int uid = gpr[4];
       
        SceKernelEventFlagInfo event = eventFlagMap.get(uid);
       
        Modules.log.debug("sceKernelCancelEventFlag id=" + uid);
        
        if (event != null) {
            Modules.log.warn("sceKernelCancelEventFlag - Not implemented");
        } else {
            Modules.log.warn("sceKernelCancelEventFlag - invalid id=" + uid);
            gpr[2] = ERROR_NOT_FOUND_CALLBACK;
        }
    }
    
    public void sceKernelReferEventFlagStatus(Processor processor) {
        int[] gpr = processor.cpu.gpr;

        int uid = gpr[4];
       
        SceKernelEventFlagInfo event = eventFlagMap.get(uid);

        Modules.log.debug("sceKernelReferEventFlagStatus id=" + uid);
        
        if (event != null) {
            Modules.log.warn("sceKernelReferEventFlagStatus - Not implemented");
        } else {
            Modules.log.warn("sceKernelReferEventFlagStatus - invalid id=" + uid);
            gpr[2] = ERROR_NOT_FOUND_CALLBACK;
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
