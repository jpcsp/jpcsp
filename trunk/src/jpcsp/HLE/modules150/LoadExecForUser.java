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

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_PROHIBIT_LOADEXEC_DEVICE;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_ILLEGAL_LOADEXEC_FILENAME;
import jpcsp.Emulator;
import jpcsp.GeneralJpcspException;
import jpcsp.Loader;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.filesystems.SeekableDataInput;
import jpcsp.util.Utilities;

public class LoadExecForUser implements HLEModule {
	@Override
	public String getName() { return "LoadExecForUser"; }

	@Override
	public void installModule(HLEModuleManager mm, int version) {
		if (version >= 150) {

			mm.addFunction(0xBD2F1094, sceKernelLoadExecFunction);
			mm.addFunction(0x2AC9954B, sceKernelExitGameWithStatusFunction);
			mm.addFunction(0x05572A5F, sceKernelExitGameFunction);
			mm.addFunction(0x4AC57943, sceKernelRegisterExitCallbackFunction);

		}
	}

	@Override
	public void uninstallModule(HLEModuleManager mm, int version) {
		if (version >= 150) {

			mm.removeFunction(sceKernelLoadExecFunction);
			mm.removeFunction(sceKernelExitGameWithStatusFunction);
			mm.removeFunction(sceKernelExitGameFunction);
			mm.removeFunction(sceKernelRegisterExitCallbackFunction);

		}
	}

	   public void sceKernelLoadExec(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int filename_addr = cpu.gpr[4];
        int option_addr = cpu.gpr[5];

        String name = Utilities.readStringZ(filename_addr);

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
        } else {
            Modules.log.debug("sceKernelLoadExec file='" + name + "' option=0x" + Integer.toHexString(option_addr));

            // Flush system memory to mimic a real PSP reset.
            Modules.SysMemUserForUserModule.reset();

            if (option_addr != 0) {
                int optSize = mem.read32(option_addr);       // Size of the option struct.
                int argSize = mem.read32(option_addr + 4);   // Number of args (strings).
                int argAddr = mem.read32(option_addr + 8);   // Pointer to a list of strings.
                int keyAddr = mem.read32(option_addr + 12);  // Pointer to an encryption key (may not be used).

                Modules.log.debug("sceKernelLoadExec params: optSize=" + optSize + ", argSize=" + argSize
                        + ", argAddr=" + Integer.toHexString(argAddr) + ", keyAddr=" + Integer.toHexString(keyAddr));
            }

            try {
                SeekableDataInput moduleInput = Modules.IoFileMgrForUserModule.getFile(name, IoFileMgrForUser.PSP_O_RDONLY);
                if (moduleInput != null) {
                    byte[] moduleBytes = new byte[(int) moduleInput.length()];
                    moduleInput.readFully(moduleBytes);
                    ByteBuffer moduleBuffer = ByteBuffer.wrap(moduleBytes);

                    SceModule module = Emulator.getInstance().load(name, moduleBuffer, true);
                    Emulator.getClock().resume();

                    if ((module.fileFormat & Loader.FORMAT_ELF) == Loader.FORMAT_ELF) {
                        cpu.gpr[2] = 0;
                    } else {
                        Modules.log.warn("sceKernelLoadExec - failed, target is not an ELF");
                        cpu.gpr[2] = ERROR_ILLEGAL_LOADEXEC_FILENAME;
                    }
                    moduleInput.close();
                }
            } catch (GeneralJpcspException e) {
                Modules.log.error("General Error : " + e.getMessage());
                Emulator.PauseEmu();
            } catch (IOException e) {
                Modules.log.error("sceKernelLoadExec - Error while loading module " + name + ": " + e.getMessage());
                cpu.gpr[2] = ERROR_PROHIBIT_LOADEXEC_DEVICE;
            }
        }
    }

    public void sceKernelExitGameWithStatus(Processor processor) {
        CpuState cpu = processor.cpu;

        int status = cpu.gpr[4];

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
        } else {
            Modules.log.info("Program exit detected with status=" + status + " (sceKernelExitGameWithStatus)");
            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_OK);
        }
    }

    public void sceKernelExitGame(Processor processor) {
        CpuState cpu = processor.cpu;

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
        } else {
            Modules.log.info("Program exit detected (sceKernelExitGame)");
            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_OK);
        }
    }

    public void sceKernelRegisterExitCallback(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
        } else {
            Modules.log.debug("IGNORING:sceKernelRegisterExitCallback SceUID=" + Integer.toHexString(uid));
            cpu.gpr[2] = 0;
        }
    }

	public final HLEModuleFunction sceKernelLoadExecFunction = new HLEModuleFunction("LoadExecForUser", "sceKernelLoadExec") {
		@Override
		public final void execute(Processor processor) {
			sceKernelLoadExec(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.LoadExecForUserModule.sceKernelLoadExec(processor);";
		}
	};

	public final HLEModuleFunction sceKernelExitGameWithStatusFunction = new HLEModuleFunction("LoadExecForUser", "sceKernelExitGameWithStatus") {
		@Override
		public final void execute(Processor processor) {
			sceKernelExitGameWithStatus(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.LoadExecForUserModule.sceKernelExitGameWithStatus(processor);";
		}
	};

	public final HLEModuleFunction sceKernelExitGameFunction = new HLEModuleFunction("LoadExecForUser", "sceKernelExitGame") {
		@Override
		public final void execute(Processor processor) {
			sceKernelExitGame(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.LoadExecForUserModule.sceKernelExitGame(processor);";
		}
	};

	public final HLEModuleFunction sceKernelRegisterExitCallbackFunction = new HLEModuleFunction("LoadExecForUser", "sceKernelRegisterExitCallback") {
		@Override
		public final void execute(Processor processor) {
			sceKernelRegisterExitCallback(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.LoadExecForUserModule.sceKernelRegisterExitCallback(processor);";
		}
	};
}