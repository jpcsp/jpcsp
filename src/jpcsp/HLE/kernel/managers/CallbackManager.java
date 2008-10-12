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
public class CallbackManager {

    private static HashMap<Integer, SceKernelCallbackInfo> callbackMap;
    
    public boolean isUidValid(int uid) {
        return callbackMap.containsKey(uid);
    }
    
    public void sceKernelCreateCallback(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;
        
        int name_addr = cpu.gpr[4];
        int threadId = cpu.gpr[5];
        int callback_addr = cpu.gpr[6];
        int callback_arg_addr = cpu.gpr[7];
        
        String name = readStringZ(mem.mainmemory,
            (name_addr & 0x3fffffff) - MemoryMap.START_RAM);

        // initPattern and currentPattern should be the same at init
        SceKernelCallbackInfo callback = new SceKernelCallbackInfo(name, threadId, callback_addr, callback_arg_addr);

        int uid = callback.getUid();
        
        cpu.gpr[2] = uid;
        
        if (0 < uid) {
            callbackMap.put(uid, callback);
        }        
    }
    
    public void sceKernelDeleteCallback(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];
        
        if (-1 < uid) {
            SceKernelCallbackInfo event = callbackMap.get(uid);
        
            event.sceKernelDeleteCallback(processor);
        }
    }
    
    public void sceKernelNotifyCallback(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];
        
        if (-1 < uid) {
            SceKernelCallbackInfo event = callbackMap.get(uid);
        
            event.sceKernelNotifyCallback(processor);
        }
    }
    
    public void sceKernelCancelCallback(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];
        
        if (-1 < uid) {
            SceKernelCallbackInfo event = callbackMap.get(uid);
        
            event.sceKernelCancelCallback(processor);
        }
    }
    
    public void sceKernelGetCallbackCount(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];
        
        if (-1 < uid) {
            SceKernelCallbackInfo event = callbackMap.get(uid);
        
            event.sceKernelGetCallbackCount(processor);
        }
    }
    
    public void sceKernelCheckCallback(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];
        
        if (-1 < uid) {
            SceKernelCallbackInfo event = callbackMap.get(uid);
        
            event.sceKernelCheckCallback(processor);
        }
    }
    
    public void sceKernelReferCallbackStatus(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];
        
        if (-1 < uid) {
            SceKernelCallbackInfo event = callbackMap.get(uid);
        
            event.sceKernelReferCallbackStatus(processor);
        }
    }
    
    
    public boolean releaseObject(SceKernelUid object) {
        if (Managers.uids.removeObject(object)) {
            callbackMap.remove(object.getUid());
            return true;
        }
        return false;
    }      
    
    public void reset() {
        callbackMap = new HashMap<Integer, SceKernelCallbackInfo>();
    }
    
    public static final CallbackManager singleton;
    
    private CallbackManager() {
    }           
    
    static {
        singleton = new CallbackManager();
        singleton.reset();
    }    
}
