/* This autogenerated file is part of jpcsp. */
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

package jpcsp.HLE.modules150;

import java.io.IOException;
import java.nio.ByteBuffer;

import jpcsp.HLE.Modules;
import jpcsp.HLE.ThreadMan;
import jpcsp.HLE.pspSysMem;
import jpcsp.HLE.pspiofilemgr;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.types.SceKernelModuleInfo;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

import jpcsp.Emulator;
import jpcsp.Loader;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Processor;
import jpcsp.filesystems.SeekableDataInput;
import jpcsp.util.Utilities;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.SceModule;
import static jpcsp.HLE.kernel.types.SceKernelErrors.*;

import jpcsp.Allegrex.CpuState; // New-Style Processor



public class ModuleMgrForUser implements HLEModule {
   // String[] bannedModulesList = {};
    enum bannedModulesList {
        LIBFONT,  /*ace combat */
        sc_sascore,
        audiocodec,
        libatrac3plus,
        videocodec,
        mpegbase,
        mpeg
    }
	@Override
	public String getName() { return "ModuleMgrForUser"; }

	@Override
	public void installModule(HLEModuleManager mm, int version) {
		if (version >= 150) {

			mm.addFunction(sceKernelLoadModuleByIDFunction, 0xB7F46618);
			mm.addFunction(sceKernelLoadModuleFunction, 0x977DE386);
			mm.addFunction(sceKernelLoadModuleMsFunction, 0x710F61B5);
			mm.addFunction(sceKernelLoadModuleBufferUsbWlanFunction, 0xF9275D98);
			mm.addFunction(sceKernelStartModuleFunction, 0x50F0C1EC);
			mm.addFunction(sceKernelStopModuleFunction, 0xD1FF982A);
			mm.addFunction(sceKernelUnloadModuleFunction, 0x2E0911AA);
			mm.addFunction(sceKernelSelfStopUnloadModuleFunction, 0xD675EBB8);
			mm.addFunction(sceKernelStopUnloadSelfModuleFunction, 0xCC1D3699);
			mm.addFunction(sceKernelGetModuleIdListFunction, 0x644395E2);
			mm.addFunction(sceKernelQueryModuleInfoFunction, 0x748CBED9);
			mm.addFunction(sceKernelGetModuleIdFunction, 0xF0A26395);
			mm.addFunction(sceKernelGetModuleIdByAddressFunction, 0xD8B73127);

		}
	}

	@Override
	public void uninstallModule(HLEModuleManager mm, int version) {
		if (version >= 150) {

			mm.removeFunction(sceKernelLoadModuleByIDFunction);
			mm.removeFunction(sceKernelLoadModuleFunction);
			mm.removeFunction(sceKernelLoadModuleMsFunction);
			mm.removeFunction(sceKernelLoadModuleBufferUsbWlanFunction);
			mm.removeFunction(sceKernelStartModuleFunction);
			mm.removeFunction(sceKernelStopModuleFunction);
			mm.removeFunction(sceKernelUnloadModuleFunction);
			mm.removeFunction(sceKernelSelfStopUnloadModuleFunction);
			mm.removeFunction(sceKernelStopUnloadSelfModuleFunction);
			mm.removeFunction(sceKernelGetModuleIdListFunction);
			mm.removeFunction(sceKernelQueryModuleInfoFunction);
			mm.removeFunction(sceKernelGetModuleIdFunction);
			mm.removeFunction(sceKernelGetModuleIdByAddressFunction);

		}
	}


    public void sceKernelLoadModuleByID(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        Memory mem = Processor.memory;

        int uid = cpu.gpr[4];
        int option_addr = cpu.gpr[5] & 0x3fffffff;
        String name = pspiofilemgr.getInstance().getFileFilename(uid);

        Modules.log.debug("sceKernelLoadModuleByID(uid=0x" + Integer.toHexString(uid)
            + "('" + name + "')"
            + ",option=0x" + Integer.toHexString(option_addr) + ")");

        // TODO refactor this with sceKernelLoadModule

        String prxname = "UNKNOWN";
        int findprx = name.lastIndexOf("/");
        int endprx = name.toLowerCase().indexOf(".prx");
        if (endprx >= 0)
            prxname = name.substring(findprx+1, endprx);

        // Load module as ELF
        try {
            SeekableDataInput moduleInput = pspiofilemgr.getInstance().getFile(uid);
            if (moduleInput != null) {
                byte[] moduleBytes = new byte[(int) moduleInput.length()];
                moduleInput.readFully(moduleBytes);
                ByteBuffer moduleBuffer = ByteBuffer.wrap(moduleBytes);

                // TODO
                // We need to get a load address, we can either add getHeapBottom to pspsysmem, or we can malloc something small
                // We're going to need to write a SceModule struct somewhere, so we could malloc that, and add the size of the struct to the address
                // For now we'll just malloc 64 bytes :P (the loadBase needs to be aligned anyway)
                int loadBase = pspSysMem.getInstance().malloc(2, pspSysMem.PSP_SMEM_Low, 64, 0) + 64;
                pspSysMem.getInstance().addSysMemInfo(2, "ModuleMgr", pspSysMem.PSP_SMEM_Low, 64, loadBase);
                SceModule module = Loader.getInstance().LoadModule(name, moduleBuffer, loadBase);

                if ((module.fileFormat & Loader.FORMAT_SCE) == Loader.FORMAT_SCE ||
                    (module.fileFormat & Loader.FORMAT_PSP) == Loader.FORMAT_PSP) {
                    // Simulate a successful loading
                    Modules.log.warn("IGNORED:sceKernelLoadModuleByID(path='" + name + "') encrypted module not loaded");
                    SceModule fakeModule = new SceModule(true);
                    fakeModule.modname = prxname;
                    fakeModule.write(mem, fakeModule.address);
                    Managers.modules.addModule(fakeModule);
                    cpu.gpr[2] = fakeModule.modid;
                } else if ((module.fileFormat & Loader.FORMAT_ELF) == Loader.FORMAT_ELF) {
                    cpu.gpr[2] = module.modid;
                } else {
                    // The Loader class now manages the module's memory footprint, it won't allocate if it failed to load
                    //pspSysMem.getInstance().free(loadBase);
                    cpu.gpr[2] = -1;
                }

                moduleInput.close();
            } else {
                Modules.log.warn("sceKernelLoadModuleByID(path='" + name + "') can't find file");
                cpu.gpr[2] = -1;
            }
        } catch (IOException e) {
            Modules.log.error("sceKernelLoadModuleByID - Error while loading module " + name + ": " + e.getMessage());
            cpu.gpr[2] = -1;
        }
    }

    public void sceKernelLoadModule(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        Memory mem = Processor.memory;

        int path_addr = cpu.gpr[4] & 0x3fffffff;
        int flags = cpu.gpr[5];
        int option_addr = cpu.gpr[6] & 0x3fffffff;
        String name = Utilities.readStringZ(path_addr);
        Modules.log.debug("sceKernelLoadModule(path='" + name
            + "',flags=0x" + Integer.toHexString(flags)
            + ",option=0x" + Integer.toHexString(option_addr) + ")");

        String prxname = "UNKNOWN";
        int findprx = name.lastIndexOf("/");
        int endprx = name.toLowerCase().indexOf(".prx");
        if (endprx >= 0)
            prxname = name.substring(findprx+1, endprx);

        // Load flash0 modules as Java HLE modules
        if (name.startsWith("flash0:")) {
            // Simulate a successful loading
            Modules.log.warn("PARTIAL:sceKernelLoadModule(path='" + name + "'): module from flash0 not loaded");
            SceModule fakeModule = new SceModule(true);
            fakeModule.modname = prxname;
            fakeModule.write(mem, fakeModule.address);
            Managers.modules.addModule(fakeModule);
            cpu.gpr[2] = fakeModule.modid;
            // TODO cpu.gpr[2] = HLEModuleManager.getInstance().LoadFlash0Module(prxname);
            return;
        }

        // Ban some modules
        for (bannedModulesList bannedModuleName : bannedModulesList.values())
        {
            if (bannedModuleName.name().matches(prxname))
            {
                Modules.log.warn("IGNORED:sceKernelLoadModule(path='" + name + "'): module from banlist not loaded");
                SceModule fakeModule = new SceModule(true);
                fakeModule.modname = prxname;
                fakeModule.write(mem, fakeModule.address);
                Managers.modules.addModule(fakeModule);
                cpu.gpr[2] = fakeModule.modid;
                return;
            }
        }

        // Load module as ELF
        try {
            SeekableDataInput moduleInput = pspiofilemgr.getInstance().getFile(name, flags);
            if (moduleInput != null) {
                byte[] moduleBytes = new byte[(int) moduleInput.length()];
                moduleInput.readFully(moduleBytes);
                ByteBuffer moduleBuffer = ByteBuffer.wrap(moduleBytes);

                // TODO
                // We need to get a load address, we can either add getHeapBottom to pspsysmem, or we can malloc something small
                // We're going to need to write a SceModule struct somewhere, so we could malloc that, and add the size of the struct to the address
                // For now we'll just malloc 64 bytes :P (the loadBase needs to be aligned anyway)
                int loadBase = pspSysMem.getInstance().malloc(2, pspSysMem.PSP_SMEM_Low, 64, 0) + 64;
                pspSysMem.getInstance().addSysMemInfo(2, "ModuleMgr", pspSysMem.PSP_SMEM_Low, 64, loadBase);
                SceModule module = Loader.getInstance().LoadModule(name, moduleBuffer, loadBase);

                if ((module.fileFormat & Loader.FORMAT_SCE) == Loader.FORMAT_SCE ||
                    (module.fileFormat & Loader.FORMAT_PSP) == Loader.FORMAT_PSP) {
                    // Simulate a successful loading
                    Modules.log.warn("IGNORED:sceKernelLoadModule(path='" + name + "') encrypted module not loaded");
                    SceModule fakeModule = new SceModule(true);
                    fakeModule.modname = prxname;
                    fakeModule.write(mem, fakeModule.address);
                    Managers.modules.addModule(fakeModule);
                    cpu.gpr[2] = fakeModule.modid;
                } else if ((module.fileFormat & Loader.FORMAT_ELF) == Loader.FORMAT_ELF) {
                    cpu.gpr[2] = module.modid;
                } else {
                    // The Loader class now manages the module's memory footprint, it won't allocate if it failed to load
                    //pspSysMem.getInstance().free(loadBase);
                    cpu.gpr[2] = -1;
                }

                moduleInput.close();
            } else {
                Modules.log.warn("sceKernelLoadModule(path='" + name + "') can't find file");
                cpu.gpr[2] = pspiofilemgr.PSP_ERROR_FILE_NOT_FOUND;
            }
        } catch (IOException e) {
            Modules.log.error("sceKernelLoadModule - Error while loading module " + name + ": " + e.getMessage());
            cpu.gpr[2] = -1;
        }
    }

	public void sceKernelLoadModuleMs(Processor processor) {
		CpuState cpu = processor.cpu; // New-Style Processor
		// Processor cpu = processor; // Old-Style Processor
		Memory mem = Processor.memory;

		/* put your own code here instead */

		// int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
		// float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

		System.out.println("Unimplemented NID function sceKernelLoadModuleMs [0x710F61B5]");

		cpu.gpr[2] = 0xDEADC0DE;

		// cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result  32); cpu.fpr[0] = result;
	}

	public void sceKernelLoadModuleBufferUsbWlan(Processor processor) {
		CpuState cpu = processor.cpu; // New-Style Processor
		// Processor cpu = processor; // Old-Style Processor
		Memory mem = Processor.memory;

		/* put your own code here instead */

		// int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
		// float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

		System.out.println("Unimplemented NID function sceKernelLoadModuleBufferUsbWlan [0xF9275D98]");

		cpu.gpr[2] = 0xDEADC0DE;

		// cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result  32); cpu.fpr[0] = result;
	}

    public void sceKernelStartModule(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor
        Memory mem = Processor.memory;

        int uid = cpu.gpr[4];
        int argsize = cpu.gpr[5];
        int argp_addr = cpu.gpr[6];
        int status_addr = cpu.gpr[7]; // TODO
        int option_addr = cpu.gpr[8]; // SceKernelSMOption

        Modules.log.debug("sceKernelStartModule(uid=0x" + Integer.toHexString(uid)
            + ",argsize=" + argsize
            + ",argp=0x" + Integer.toHexString(argp_addr)
            + ",status=0x" + Integer.toHexString(status_addr)
            + ",option=0x" + Integer.toHexString(option_addr) + ")");

        SceModule sceModule = Managers.modules.getModuleByUID(uid);

        if (sceModule == null) {
            Modules.log.warn("sceKernelStartModule - unknown module UID 0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_UNKNOWN_MODULE;
        } else  if (sceModule.isFlashModule) {
            // Trying to start a module loaded from flash0:
            // Do nothing...
            Modules.log.warn("IGNORING:sceKernelStartModule flash module '" + sceModule.modname + "'");
            cpu.gpr[2] = sceModule.modid; // return the module id
        } else {
            ThreadMan threadMan = ThreadMan.getInstance();
            if (mem.isAddressGood(sceModule.entry_addr)) {
                if (mem.isAddressGood(status_addr)) {
                    mem.write32(status_addr, 0); // TODO set to return value of the thread (when it exits, of course)
                }

                SceKernelThreadInfo thread = threadMan.hleKernelCreateThread("SceModmgrStart",
                        sceModule.entry_addr, 0x20, 0x40000, sceModule.attribute, option_addr);
                // override inherited module id with the new module we are starting
                thread.moduleid = sceModule.modid;
                cpu.gpr[2] = sceModule.modid; // return the module id
                threadMan.hleKernelStartThread(thread, argsize, argp_addr, sceModule.gp_value);
            } else {
                Modules.log.warn("sceKernelStartModule - invalid entry address 0x" + Integer.toHexString(sceModule.entry_addr));
                cpu.gpr[2] = -1;
            }
        }
    }

    public void sceKernelStopModule(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor
        Memory mem = Processor.memory;

        int uid = cpu.gpr[4];
        int argsize = cpu.gpr[5];
        int argp_addr = cpu.gpr[6];
        int status_addr = cpu.gpr[7]; // TODO
        int option_addr = cpu.gpr[8]; // SceKernelSMOption

        Modules.log.warn("UNIMPLEMENTED:sceKernelStopModule(uid=0x" + Integer.toHexString(uid)
            + ",argsize=" + argsize
            + ",argp=0x" + Integer.toHexString(argp_addr)
            + ",status=0x" + Integer.toHexString(status_addr)
            + ",option=0x" + Integer.toHexString(option_addr) + ")");

        // TODO check if module_stop export exists in this module, if so:
        // - create a thread called SceKernelModmgrStop, stack 0x40000, pri 0x20, entry = module_stop
        // - start the thread using the parameters supplied to this function

        // TODO only write this if module_stop exists
        if (mem.isAddressGood(status_addr)) {
            mem.write32(status_addr, 0); // TODO set to return value of the thread (when it exits, of course)
        }

        // TODO
        // return 0 regardless of module_stop existing
        // 80020135 module already stopped
        // 8002012E not found module, ERROR_UNKNOWN_MODULE
        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceKernelUnloadModule(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor
        Memory mem = Processor.memory;

        int uid = cpu.gpr[4];

        SceModule sceModule = Managers.modules.getModuleByUID(uid);
        if (sceModule == null) {
            Modules.log.warn("sceKernelUnloadModule unknown module UID 0x" + Integer.toHexString(uid));
            cpu.gpr[2] = -1;
        } else {
            Modules.log.warn("PARTIAL:sceKernelUnloadModule(uid=" + Integer.toHexString(uid) + ") modname:'" + sceModule.modname + "'");

            // TODO terminate delete all threads that belong to this module

            pspSysMem sysMem = pspSysMem.getInstance();
            for (int i = 0; i < sceModule.nsegment; i++) {
                sysMem.free(sceModule.segmentaddr[i]);
            }

            sceModule.free();

            Managers.modules.removeModule(uid);

            cpu.gpr[2] = 0;
        }
    }

    public void sceKernelSelfStopUnloadModule(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor

        int unknown = cpu.gpr[4];
        int argsize = cpu.gpr[5];
        int argp_addr = cpu.gpr[6];

        Modules.log.debug("sceKernelSelfStopUnloadModule(unknown=0x" + Integer.toHexString(unknown)
            + ",argsize=" + argsize
            + ",argp_addr=0x" + Integer.toHexString(argp_addr) +
            ") current thread:'" + ThreadMan.getInstance().getCurrentThread().name + "'");

        // TODO see if the current thread belongs to the root module,
        // we can get root module from Emulator.getInstance().module.
        // If it is not the from root module do not pause the emulator!
        // compare ThreadMan.getInstance().getCurrentThread().modid
        // terminate delete thread?

        Modules.log.info("Program exit detected (sceKernelSelfStopUnloadModule)");
        Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_OK);

        cpu.gpr[2] = 0;
    }

	public void sceKernelStopUnloadSelfModule(Processor processor) {
		CpuState cpu = processor.cpu; // New-Style Processor
		// Processor cpu = processor; // Old-Style Processor
		Memory mem = Processor.memory;

		/* put your own code here instead */

		// int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
		// float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

		System.out.println("Unimplemented NID function sceKernelStopUnloadSelfModule [0xCC1D3699]");

		cpu.gpr[2] = 0xDEADC0DE;

		// cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result  32); cpu.fpr[0] = result;
	}

	public void sceKernelGetModuleIdList(Processor processor) {
		CpuState cpu = processor.cpu; // New-Style Processor
		// Processor cpu = processor; // Old-Style Processor
		Memory mem = Processor.memory;

		/* put your own code here instead */

		// int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
		// float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

		System.out.println("Unimplemented NID function sceKernelGetModuleIdList [0x644395E2]");

		cpu.gpr[2] = 0xDEADC0DE;

		// cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result  32); cpu.fpr[0] = result;
	}

    public void sceKernelQueryModuleInfo(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor
        Memory mem = Processor.memory;

        int uid = cpu.gpr[4];
        int info_addr = cpu.gpr[5];

        SceModule sceModule = Managers.modules.getModuleByUID(uid);
        if (sceModule == null) {
            Modules.log.warn("sceKernelQueryModuleInfo unknown module UID 0x" + Integer.toHexString(uid));
            cpu.gpr[2] = -1;
        } else if (!mem.isAddressGood(info_addr)) {
            Modules.log.warn("sceKernelQueryModuleInfo bad info pointer " + String.format("0x%08X", info_addr));
            cpu.gpr[2] = -1;
        } else {
            Modules.log.debug("sceKernelQueryModuleInfo UID 0x" + Integer.toHexString(uid)
                + " info " + String.format("0x%08X", info_addr)
                + " modname '" + sceModule.modname + "'");
            SceKernelModuleInfo moduleInfo = new SceKernelModuleInfo();
            moduleInfo.copy(sceModule);
            moduleInfo.write(mem, info_addr);
            cpu.gpr[2] = 0;
        }
    }

    public void sceKernelGetModuleId(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor

        int moduleid = ThreadMan.getInstance().getCurrentThread().moduleid;

        Modules.log.debug("sceKernelGetModuleId returning 0x" + Integer.toHexString(moduleid));

        cpu.gpr[2] = moduleid;
    }

	public void sceKernelGetModuleIdByAddress(Processor processor) {
		CpuState cpu = processor.cpu; // New-Style Processor

        int addr = cpu.gpr[4];

        SceModule module = Managers.modules.getModuleByAddress(addr);
        if (module != null) {
            Modules.log.debug("sceKernelGetModuleIdByAddress(addr=0x" + Integer.toHexString(addr) + ") returning 0x" + Integer.toHexString(module.modid));
            cpu.gpr[2] = module.modid;
        } else {
            Modules.log.warn("sceKernelGetModuleIdByAddress(addr=0x" + Integer.toHexString(addr) + ") module not found");
            cpu.gpr[2] = -1;
        }
	}

	public final HLEModuleFunction sceKernelLoadModuleByIDFunction = new HLEModuleFunction("ModuleMgrForUser", "sceKernelLoadModuleByID") {
		@Override
		public final void execute(Processor processor) {
			sceKernelLoadModuleByID(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.ModuleMgrForUserModule.sceKernelLoadModuleByID(processor);";
		}
	};

	public final HLEModuleFunction sceKernelLoadModuleFunction = new HLEModuleFunction("ModuleMgrForUser", "sceKernelLoadModule") {
		@Override
		public final void execute(Processor processor) {
			sceKernelLoadModule(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.ModuleMgrForUserModule.sceKernelLoadModule(processor);";
		}
	};

	public final HLEModuleFunction sceKernelLoadModuleMsFunction = new HLEModuleFunction("ModuleMgrForUser", "sceKernelLoadModuleMs") {
		@Override
		public final void execute(Processor processor) {
			sceKernelLoadModuleMs(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.ModuleMgrForUserModule.sceKernelLoadModuleMs(processor);";
		}
	};

	public final HLEModuleFunction sceKernelLoadModuleBufferUsbWlanFunction = new HLEModuleFunction("ModuleMgrForUser", "sceKernelLoadModuleBufferUsbWlan") {
		@Override
		public final void execute(Processor processor) {
			sceKernelLoadModuleBufferUsbWlan(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.ModuleMgrForUserModule.sceKernelLoadModuleBufferUsbWlan(processor);";
		}
	};

	public final HLEModuleFunction sceKernelStartModuleFunction = new HLEModuleFunction("ModuleMgrForUser", "sceKernelStartModule") {
		@Override
		public final void execute(Processor processor) {
			sceKernelStartModule(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.ModuleMgrForUserModule.sceKernelStartModule(processor);";
		}
	};

	public final HLEModuleFunction sceKernelStopModuleFunction = new HLEModuleFunction("ModuleMgrForUser", "sceKernelStopModule") {
		@Override
		public final void execute(Processor processor) {
			sceKernelStopModule(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.ModuleMgrForUserModule.sceKernelStopModule(processor);";
		}
	};

	public final HLEModuleFunction sceKernelUnloadModuleFunction = new HLEModuleFunction("ModuleMgrForUser", "sceKernelUnloadModule") {
		@Override
		public final void execute(Processor processor) {
			sceKernelUnloadModule(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.ModuleMgrForUserModule.sceKernelUnloadModule(processor);";
		}
	};

	public final HLEModuleFunction sceKernelSelfStopUnloadModuleFunction = new HLEModuleFunction("ModuleMgrForUser", "sceKernelSelfStopUnloadModule") {
		@Override
		public final void execute(Processor processor) {
			sceKernelSelfStopUnloadModule(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.ModuleMgrForUserModule.sceKernelSelfStopUnloadModule(processor);";
		}
	};

	public final HLEModuleFunction sceKernelStopUnloadSelfModuleFunction = new HLEModuleFunction("ModuleMgrForUser", "sceKernelStopUnloadSelfModule") {
		@Override
		public final void execute(Processor processor) {
			sceKernelStopUnloadSelfModule(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.ModuleMgrForUserModule.sceKernelStopUnloadSelfModule(processor);";
		}
	};

	public final HLEModuleFunction sceKernelGetModuleIdListFunction = new HLEModuleFunction("ModuleMgrForUser", "sceKernelGetModuleIdList") {
		@Override
		public final void execute(Processor processor) {
			sceKernelGetModuleIdList(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.ModuleMgrForUserModule.sceKernelGetModuleIdList(processor);";
		}
	};

	public final HLEModuleFunction sceKernelQueryModuleInfoFunction = new HLEModuleFunction("ModuleMgrForUser", "sceKernelQueryModuleInfo") {
		@Override
		public final void execute(Processor processor) {
			sceKernelQueryModuleInfo(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.ModuleMgrForUserModule.sceKernelQueryModuleInfo(processor);";
		}
	};

	public final HLEModuleFunction sceKernelGetModuleIdFunction = new HLEModuleFunction("ModuleMgrForUser", "sceKernelGetModuleId") {
		@Override
		public final void execute(Processor processor) {
			sceKernelGetModuleId(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.ModuleMgrForUserModule.sceKernelGetModuleId(processor);";
		}
	};

	public final HLEModuleFunction sceKernelGetModuleIdByAddressFunction = new HLEModuleFunction("ModuleMgrForUser", "sceKernelGetModuleIdByAddress") {
		@Override
		public final void execute(Processor processor) {
			sceKernelGetModuleIdByAddress(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.ModuleMgrForUserModule.sceKernelGetModuleIdByAddress(processor);";
		}
	};

};
