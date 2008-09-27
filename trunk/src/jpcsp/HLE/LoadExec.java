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

import jpcsp.Emulator;

public class LoadExec {
    private static LoadExec instance;

    public static LoadExec get_instance() {
        if (instance == null) {
            instance = new LoadExec();
        }
        return instance;
    }

    private LoadExec() {
    }

    public void sceKernelRegisterExitCallback(int uid)
    {
        // TODO
        Modules.log.warn("UNIMPLEMENTED:sceKernelRegisterExitCallback SceUID=" + Integer.toHexString(uid));

        // Fake successful return
        Emulator.getProcessor().cpu.gpr[2] = 0;
    }

    public void sceKernelExitGame()
    {
        Modules.log.info("Program exit detected (sceKernelExitGame)");
        Emulator.PauseEmu();
    }

    /* TODO
    public void sceKernelLoadExec(int a0, int a1)
    {
        Emulator.getProcessor().gpr[2] = 0;
    }
    */
}
