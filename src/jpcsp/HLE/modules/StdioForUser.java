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
        // Here we can change the NID.
		// We also choose which implementation of the function to use here.
        if (version >= pspSysMem.PSP_FIRMWARE_271)
            mm.add(sceKernelStdin271, 0x172D316E);
        else
            mm.add(sceKernelStdin150, 0x172D316E);

        mm.add(sceKernelStdout, 0xa6bab2e9);
        mm.add(sceKernelStderr, 0xf78ba90a);
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
            new HLEModuleFunction("StdioForUser", "sceKernelStdin") {
        @Override
        public void execute(Processor cpu, Memory mem) {
            cpu.gpr[2] = 3;
        }
    };

    // Here we can change the implementation of the function (but it's identical for this example)
    public static final HLEModuleFunction sceKernelStdin271 =
            new HLEModuleFunction("StdioForUser", "sceKernelStdin") {
        @Override
        public void execute(Processor cpu, Memory mem) {
            cpu.gpr[2] = 3;
        }
    };

    public static final HLEModuleFunction sceKernelStdout =
            new HLEModuleFunction("StdioForUser", "sceKernelStdout") {
        @Override
        public void execute(Processor cpu, Memory mem) {
            cpu.gpr[2] = 1;
        }
    };

    public static final HLEModuleFunction sceKernelStderr =
            new HLEModuleFunction("StdioForUser", "sceKernelStderr") {
        @Override
        public void execute(Processor cpu, Memory mem) {
            cpu.gpr[2] = 2;
        }
    };
}
