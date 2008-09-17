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

package jpcsp.HLE.modules;

import java.util.HashMap;
import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.NIDMapper;

/**
 *
 * @author fiveofhearts
 */
public class HLEModuleManager {
    private static HLEModuleManager instance;

    private HashMap<Integer, HLEModuleFunction> syscallCodeToFunction;
    private int syscallCodeAllocator;

    private HLEModule[] defaultModules = new HLEModule[] {
        new StdioForUser()
    };
    
    public static HLEModuleManager get_instance() {
        if (instance == null) {
            instance = new HLEModuleManager();
        }
        return instance;
    }

    public void Initialise() {
        syscallCodeToFunction = new HashMap<Integer, HLEModuleFunction>();
        
        // Official syscalls start at 0x2000,
        // so we'll put the HLE syscalls far away at 0x4000.
        syscallCodeAllocator = 0x4000;
        
        installDefaultModules();
    }
    
    private void installDefaultModules() {
        for (HLEModule module : defaultModules) {
            module.installModule(this);
        }
    }

    public void add(HLEModuleFunction func) {
        // See if a known syscall code exists for this NID
        int code = NIDMapper.get_instance().nidToSyscall(func.getNid());
        if (code == -1) {
            // Allocate an arbitrary syscall code to the function
            code = syscallCodeAllocator;
            // Add the new code to the NIDMapper
            NIDMapper.get_instance().addSyscallNid(func.getNid(), syscallCodeAllocator);
            syscallCodeAllocator++;
        }

        /*
        System.out.println("HLEModuleManager - registering "
                + func.getModuleName() + "_"
                + String.format("%08x", func.getNid()) + "_"
                + func.getFunctionName()
                + " to " + Integer.toHexString(code));
        */
        func.setSyscallCode(code);
        syscallCodeToFunction.put(code, func);
    }

    public void remove(HLEModuleFunction func) {
        /*
        System.out.println("HLEModuleManager - unregistering "
                + func.getModuleName() + "_"
                + String.format("%08x", func.getNid()) + "_"
                + func.getFunctionName());
        */
        int syscallCode = func.getSyscallCode();
        syscallCodeToFunction.remove(syscallCode);
    }

    /**
     * @param code The syscall code to try and execute.
     * @return true if handled, false if not handled.
     */
    public boolean handleSyscall(int code) {
        HLEModuleFunction func = syscallCodeToFunction.get(code);
        if (func != null) {
            func.execute(Emulator.getProcessor(), Memory.get_instance());
            return true;
        } else {
            return false;
        }
    }
}
