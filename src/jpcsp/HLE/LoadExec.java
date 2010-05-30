/*
Function:
- HLE everything in http://psp.jim.sh/pspsdk-doc/group__LoadExec.html


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
package jpcsp.HLE;

import java.io.IOException;
import java.nio.ByteBuffer;

import jpcsp.Allegrex.CpuState;
import jpcsp.Emulator;
import jpcsp.GeneralJpcspException;
import jpcsp.Loader;
import jpcsp.Processor;
import jpcsp.filesystems.SeekableDataInput;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.util.Utilities;

public class LoadExec {
    private static LoadExec instance;

    public static LoadExec getInstance() {
        if (instance == null) {
            instance = new LoadExec();
        }
        return instance;
    }

    private LoadExec() {
    }

    public void sceKernelRegisterExitCallback(int uid)
    {
        Modules.log.debug("IGNORING:sceKernelRegisterExitCallback SceUID=" + Integer.toHexString(uid));

        // Fake successful return
        Emulator.getProcessor().cpu.gpr[2] = 0;
    }

    public void sceKernelExitGame()
    {
        Modules.log.info("Program exit detected (sceKernelExitGame)");
        Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_OK);
    }

    public void sceKernelLoadExec(int filename_addr, int option_addr)
    {
        Processor processor = Emulator.getProcessor();
        CpuState cpu = processor.cpu;

        String name = Utilities.readStringZ(filename_addr);

        Modules.log.debug("sceKernelLoadExec file='" + name + "' option=0x" + Integer.toHexString(option_addr));

        if (option_addr != 0)
            Modules.log.warn("UNIMPLEMENTED:sceKernelLoadExec option=0x" + Integer.toHexString(option_addr));

        try {
            SeekableDataInput moduleInput = pspiofilemgr.getInstance().getFile(name, pspiofilemgr.PSP_O_RDONLY);
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
                    cpu.gpr[2] = -1;
                }

                moduleInput.close();
            }
        } catch (GeneralJpcspException e) {
            Modules.log.error("General Error : " + e.getMessage());
            Emulator.PauseEmu();
        } catch (IOException e) {
            Modules.log.error("sceKernelLoadExec - Error while loading module " + name + ": " + e.getMessage());
            cpu.gpr[2] = -1;
        }
    }
}
