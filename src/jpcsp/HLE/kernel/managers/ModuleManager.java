/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jpcsp.HLE.kernel.managers;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Processor;
import static jpcsp.util.Utilities.*;

import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.types.*;
import static jpcsp.HLE.kernel.types.SceKernelErrors.*;

/**
 *
 * @author hli
 */
public class ModuleManager {

    private HashMap<Integer, SceModule> moduleUidToModule;
    private HashMap<String, SceModule> moduleNameToModule;

    public void reset() {
        moduleUidToModule = new HashMap<Integer, SceModule>();
        moduleNameToModule = new HashMap<String, SceModule>();
    }

    public void addModule(SceModule module) {
        moduleUidToModule.put(module.modid, module);
        moduleNameToModule.put(module.modname, module);
    }

    public void removeModule(int uid) {
        SceModule sceModule = moduleUidToModule.remove(uid);
        if (sceModule != null)
            moduleNameToModule.remove(sceModule.modname);
    }

    // used by the loader to fixup deferred imports
    public Collection<SceModule> values() {
        return moduleUidToModule.values();
    }

    public SceModule getModule(int uid) {
        return moduleUidToModule.get(uid);
    }

    // used by sceKernelFindModuleByName
    public SceModule getModuleByName(String name) {
        return moduleNameToModule.get(name);
    }

    public SceModule getModuleByAddress(int address) {
        for (SceModule module : moduleUidToModule.values()) {
            if (address >= module.loadAddressLow && address < module.loadAddressHigh)
                return module;
        }
        return null;
    }

    // -------------------------- ModuleMgrForUser --------------------------
    // unfinished - do not use (hlide's stuff), use ModuleMgrForUser.java instead

    public boolean isUidValid(int uid) {
        return moduleUidToModule.containsKey(uid);
    }

    public void sceKernelLoadModule(Processor processor) {
        /*
        int[] gpr = processor.cpu.gpr;
        Memory mem = Processor.memory;

        int name_addr = gpr[4];
        int start_addr = gpr[5];
        int attr = gpr[6];

        String name = readStringZ(mem.mainmemory,
                (name_addr & 0x3fffffff) - MemoryMap.START_RAM);

        SceKernelModuleInfo module = new SceKernelModuleInfo(name, start_addr, attr);

        int uid = module.getUid();

        gpr[2] = uid;

        if (0 < uid) {
            moduleMap.put(uid, module);
        }
        */
    }

    public void sceKernelLoadModuleByID(Processor processor) {
    }

    public void sceKernelLoadModuleMs(Processor processor) {
    }

    public void sceKernelLoadModuleBufferUsbWlan(Processor processor) {
    }

    public void sceKernelStartModule(Processor processor) {
    }

    public void sceKernelStopModule(Processor processor) {
    }

    public void sceKernelUnloadModule(Processor processor) {
    }

    public void sceKernelSelfStopUnloadModule(Processor processor) {
    }

    public void sceKernelStopUnloadSelfModule(Processor processor) {
    }

    public void sceKernelGetModuleIdList(Processor processor) {
    }

    public void sceKernelQueryModuleInfo(Processor processor) {
    }

    public void sceKernelGetModuleId(Processor processor) {
    }

    public void sceKernelGetModuleIdByAddress(Processor processor) {
    }

    public boolean releaseObject(SceKernelUid object) {
        /*
        if (Managers.uids.removeObject(object)) {
            moduleMap.remove(object.getUid());
            return true;
        }
        */
        return false;
    }

    // -------------------------- singleton --------------------------

    public static final ModuleManager singleton;

    static {
        singleton = new ModuleManager();
        singleton.reset();
    }

    private ModuleManager() {
    }
}
