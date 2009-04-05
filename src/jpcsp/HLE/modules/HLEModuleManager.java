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
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.NIDMapper;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.HLE.Modules;
import jpcsp.HLE.pspSysMem;
import jpcsp.HLE.kernel.Managers;


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

    //private HashMap<String, List<HLEModule>> flash0prxMap;

    /** The current firmware version we are using
     * was supposed to be one of pspSysMem.PSP_FIRMWARE_* but there's a mistake
     * in the module autogen and now its an int in this format:
     * ABB, where A = major and B = minor, for example 271 */
    private int firmwareVersion;

    // TODO add more modules here
    private HLEModule[] defaultModules = new HLEModule[] {
        Modules.SampleModule, // For testing purposes
        Modules.StdioForUserModule,
        Modules.sceUmdUserModule,
        Modules.scePowerModule,
        Modules.sceUtilityModule,
        Modules.sceRtcModule,
        Modules.Kernel_LibraryModule,
        Modules.ModuleMgrForUserModule,
        Modules.sceMpegModule,
        Modules.LoadCoreForKernelModule,
        Modules.sceAttrac3plusModule,
        Modules.sceCtrlModule,
        Modules.sceAudioModule,
        Modules.sceImposeModule,
        Modules.sceSuspendForUserModule,
        Modules.sceDmacModule,
        Modules.sceHprmModule, // check if loaded by default

        // HACK: we should only load this when the game tries to load sc_sascore.prx,
        // or if the firmware version has it built in.
        Modules.sceSasCoreModule,
    };

    public static HLEModuleManager getInstance() {
        if (instance == null) {
            instance = new HLEModuleManager();
        }
        return instance;
    }

    /** (String)"2.71" to (int)271 */
    public static int psfFirmwareVersionToInt(String firmwareVersion) {
        int version = 150;

        if (firmwareVersion != null) {
            version = (int)(Float.parseFloat(firmwareVersion) * 100);

            // HACK we started implementing stuff under 150 even if it existed in 100
            if (version < 150)
                version = 150;
        }

        return version;
    }

    public void Initialise(int firmwareVersion) {
        syscallCodeToFunction = new HashMap<Integer, HLEModuleFunction>();

        // Official syscalls start at 0x2000,
        // so we'll put the HLE syscalls far away at 0x4000.
        syscallCodeAllocator = 0x4000;

        hleThreadList = new ArrayList<HLEThread>();

        this.firmwareVersion = firmwareVersion;
        installDefaultModules();
        //initialiseFlash0PRXMap();
    }

    private void installDefaultModules() {
        Modules.log.debug("Loading HLE firmware up to version " + firmwareVersion);
        for (HLEModule module : defaultModules) {
            module.installModule(this, firmwareVersion);
        }
    }

    // TODO add here modules in flash that aren't loaded by default
    // We could add all modules but I think we just need the unloaded ones (fiveofhearts)
    private void initialiseFlash0PRXMap() {
        //flash0prxMap = new HashMap<String, List<HLEModule>>();
        /* TODO
        List<HLEModule> prx;

        prx = new LinkedList<HLEModule>();
        prx.add(Modules.sceNetIfhandleModule);
        prx.add(Modules.sceNetIfhandle_libModule);
        prx.add(Modules.sceNetIfhandle_driverModule);
        flash0prxMap.put("ifhandle.prx", prx);

        prx = new LinkedList<HLEModule>();
        prx.add(Modules.sceNetModule);
        prx.add(Modules.sceNet_libModule);
        flash0prxMap.put("pspnet.prx", prx);
        */
    }

    /** @return the UID assigned to the module or negative on error */
    /* unused
    public int LoadHLEModule(String modname, HLEModule module) {
        int uid = -1;
        module.installModule(this, firmwareVersion);
        SceModule fakeModule = new SceModule(true);
        fakeModule.modname = modname;
        fakeModule.write(Memory.getInstance(), fakeModule.address);
        Managers.modules.addModule(fakeModule);
        uid = fakeModule.modid;
        return uid;
    }
    */

    /** @return the UID assigned to the module or negative on error
     * TODO need to figure out how the uids work when 1 prx contains several modules. */
    /* unused
    public int LoadFlash0Module(String prxname) {
        int uid = -1;
        List<HLEModule> prx = flash0prxMap.get(prxname);
        if (prx != null) {
            for (HLEModule module : prx) {
                module.installModule(this, firmwareVersion);
            }
            SceModule fakeModule = new SceModule(true);
            fakeModule.modname = prxname;
            fakeModule.write(Memory.getInstance(), fakeModule.address);
            Managers.modules.addModule(fakeModule);
            uid = fakeModule.modid;
        }
        return uid;
    }
    */

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
        int code = NIDMapper.getInstance().nidToSyscall(func.getNid());
        if (code == -1) {
            // Allocate an arbitrary syscall code to the function
            code = syscallCodeAllocator;
            // Add the new code to the NIDMapper
            NIDMapper.getInstance().addSyscallNid(func.getNid(), syscallCodeAllocator);
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
}
