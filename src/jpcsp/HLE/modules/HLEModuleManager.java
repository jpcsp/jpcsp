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

import jpcsp.HLE.kernel.SceModule;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import jpcsp.Emulator;
import jpcsp.HLE.Modules;
import jpcsp.HLE.pspSysMem;
import jpcsp.NIDMapper;

/**
 * For backwards compatibility with the current jpcsp code, the old
 * SyscallHandler can still be used. When an unhandled syscall is found
 * HLEModuleManager.getInstance().handleSyscall(code) should be called.
 * This function will then return true if the syscall is handled, in which case
 * no error message should be printed by SyscallHandler.java.
 *
 * Modules that require stepping should implement HLEThread and call
 * mm.addThread inside installModule with a matching mm.removeThread in
 * uninstall module.
 * Example: ThreadMan, pspctrl, pspAudio, pspdisplay
 *
 * @author fiveofhearts
 */
public class HLEModuleManager {
    private static HLEModuleManager instance;

    private HashMap<Integer, HLEModuleFunction> syscallCodeToFunction;
    private int syscallCodeAllocator;

    private List<HLEThread> hleThreadList;
    private List<SceModule> sceModuleList;

    // TODO add more modules here
    private HLEModule[] defaultModules = new HLEModule[] {
        new Sample(),
        Modules.StdioForUserModule,
        Modules.sceUmdUserModule,
        Modules.scePowerModule,
        Modules.sceUtilityModule,
        Modules.sceRtcModule,
        Modules.Kernel_LibraryModule,
        Modules.ModuleMgrForUserModule,
        Modules.sceMpegModule,
        Modules.LoadCoreForKernelModule,
    };

    public static HLEModuleManager getInstance() {
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

        hleThreadList = new LinkedList<HLEThread>();
        sceModuleList = new LinkedList<SceModule>();

        installDefaultModules(pspSysMem.PSP_FIRMWARE_150);
    }

    /** @param version The firmware version of the module to load.
     * @see pspSysMem */
    private void installDefaultModules(int version) {
        for (HLEModule module : defaultModules) {
            module.installModule(this, version);
        }
    }

    /** Instead use addFunction. */
    @Deprecated
    public void add(HLEModuleFunction func, int nid) {
        addFunction(func, nid);
    }

    /** Instead use removeFunction. */
    @Deprecated
    public void remove(HLEModuleFunction func) {
        removeFunction(func);
    }

    public void addFunction(HLEModuleFunction func, int nid) {
        func.setNid(nid);

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

    public void removeFunction(HLEModuleFunction func) {
        /*
        System.out.println("HLEModuleManager - unregistering "
                + func.getModuleName() + "_"
                + String.format("%08x", func.getNid()) + "_"
                + func.getFunctionName());
        */
        int syscallCode = func.getSyscallCode();
        syscallCodeToFunction.remove(syscallCode);
    }

    public void addThread(HLEThread thread) {
        hleThreadList.add(thread);
    }

    public void removeThread(HLEThread thread) {
        hleThreadList.remove(thread);
    }

    public void step() {
        for (Iterator<HLEThread> it = hleThreadList.iterator(); it.hasNext();) {
            HLEThread thread = it.next();
            thread.step();
        }
    }

    /**
     * @param code The syscall code to try and execute.
     * @return true if handled, false if not handled.
     */
    public boolean handleSyscall(int code) {
        HLEModuleFunction func = syscallCodeToFunction.get(code);
        if (func != null) {
            func.execute(Emulator.getProcessor());
            return true;
        } else {
            return false;
        }
    }

    public void addSceModule(SceModule sceModule) {
        sceModuleList.add(sceModule);
    }

    public SceModule getSceModuleByUid(int uid) {
        for (Iterator<SceModule> it = sceModuleList.iterator(); it.hasNext();) {
            SceModule sceModule = it.next();
            if (sceModule.getUid() == uid) {
                return sceModule;
            }
        }

        return null;
    }
}
