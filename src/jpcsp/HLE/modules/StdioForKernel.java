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

import org.apache.log4j.Logger;

import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.PspString;

public class StdioForKernel extends HLEModule {
    public static Logger log = Modules.getLogger("StdioForKernel");

    @HLEFunction(nid = 0xCAB439DF, version = 150)
    public int printf(CpuState cpu, PspString formatString) {
    	return Modules.SysMemUserForUserModule.hleKernelPrintf(cpu, formatString, log);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD97C8CB9, version = 150)
    public int puts() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x11A5127A, version = 150)
    public int fdgets() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2CCF071A, version = 150)
    public int fdprintf() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2D8551AB, version = 150)
    public int sceKernelStdoutReset() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x36B23B8B, version = 150)
    public int fdputs() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4F78930A, version = 150)
    public int fdputc() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7E338487, version = 150)
    public int getchar() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9662BF86, version = 150)
    public int sceKernelStderrReset() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x98220F3E, version = 150)
    public int sceKernelStdoutReopen() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xBFF7E760, version = 150)
    public int gets() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD2B2A2A7, version = 150)
    public int fdgetc() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD768752A, version = 150)
    public int putchar() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xFB5380C5, version = 150)
    public int sceKernelStderrReopen() {
    	return 0;
    }
}
