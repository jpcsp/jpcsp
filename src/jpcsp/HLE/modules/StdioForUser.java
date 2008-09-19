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

import jpcsp.HLE.pspSysMem;
import jpcsp.Memory;
import jpcsp.Processor;

/**
 *
 * @author fiveofhearts
 */
public class StdioForUser implements HLEModule {
    @Override public final String getName() { return "StdioForUser"; };

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        if (version >= pspSysMem.PSP_FIRMWARE_271)
            mm.add(sceKernelStdin271);
        else
            mm.add(sceKernelStdin150);

        mm.add(sceKernelStdout);
        mm.add(sceKernelStderr);
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        if (version >= pspSysMem.PSP_FIRMWARE_271)
            mm.remove(sceKernelStdin271);
        else
            mm.remove(sceKernelStdin150);

        mm.remove(sceKernelStdout);
        mm.remove(sceKernelStderr);
    }

    public static final HLEModuleFunction sceKernelStdin150 =
            new HLEModuleFunction("StdioForUser", "sceKernelStdin", 0x172D316E) {
        @Override
        public void execute(Processor cpu, Memory mem) {
            cpu.gpr[2] = 3;
        }
    };

    // The code is exactly the same as 150, but it's an example of how different
    // firmware can be handled.
    // Here we can change the functionality and the NID.
    public static final HLEModuleFunction sceKernelStdin271 =
            new HLEModuleFunction("StdioForUser", "sceKernelStdin", 0x172D316E) {
        @Override
        public void execute(Processor cpu, Memory mem) {
            cpu.gpr[2] = 3;
        }
    };

    public static final HLEModuleFunction sceKernelStdout =
            new HLEModuleFunction("StdioForUser", "sceKernelStdout", 0xa6bab2e9) {
        @Override
        public void execute(Processor cpu, Memory mem) {
            cpu.gpr[2] = 1;
        }
    };

    public static final HLEModuleFunction sceKernelStderr =
            new HLEModuleFunction("StdioForUser", "sceKernelStderr", 0xf78ba90a) {
        @Override
        public void execute(Processor cpu, Memory mem) {
            cpu.gpr[2] = 2;
        }
    };
}
