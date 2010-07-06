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
package jpcsp.HLE.kernel.managers;

import java.util.Collection;
import java.util.HashMap;

import jpcsp.HLE.kernel.types.SceModule;

public class ModuleManager {

    private HashMap<Integer, SceModule> moduleUidToModule;
    private HashMap<String, SceModule> moduleNameToModule;

    public void reset() {
        moduleUidToModule = new HashMap<Integer, SceModule>();
        moduleNameToModule = new HashMap<String, SceModule>();
    }

    // -------------------------- helpers --------------------------

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

    public SceModule getModuleByUID(int uid) {
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

    // -------------------------- singleton --------------------------

    public static final ModuleManager singleton;

    static {
        singleton = new ModuleManager();
        singleton.reset();
    }

    private ModuleManager() {
    }
}
