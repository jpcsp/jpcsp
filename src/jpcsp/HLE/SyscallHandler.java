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
package jpcsp.HLE;

import jpcsp.Emulator;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.util.DurationStatistics;
import jpcsp.Allegrex.CpuState;
import jpcsp.Debugger.DisassemblerModule.syscallsFirm15;

public class SyscallHandler {
	public static DurationStatistics durationStatistics = new DurationStatistics("Syscall");
    public static boolean ignoreUnmappedImports = false;

	public static void reset() {
		durationStatistics.reset();
	}

    public static boolean isEnableIgnoreUnmappedImports(){
        return ignoreUnmappedImports;
    }

    public static void setEnableIgnoreUnmappedImports(boolean enable){
        ignoreUnmappedImports = enable;
        if(enable)
            Modules.log.info("Ignore Unmapped Imports enabled");
    }

    public static void syscall(int code) {
        int gpr[] = Emulator.getProcessor().cpu.gpr;
        Modules.ThreadManForUserModule.clearSyscallFreeCycles();

        durationStatistics.start();

        switch(code) {
		    case 0x213a:
		        pspdisplay.getInstance().sceDisplaySetMode(gpr[4], gpr[5], gpr[6]);
		        break;
		    case 0x213b:
		        pspdisplay.getInstance().sceDisplayGetMode(gpr[4], gpr[5], gpr[6]);
		        break;
		    case 0x213c:
		        pspdisplay.getInstance().sceDisplayGetFramePerSec();
		        break;
		    case 0x213d:
		        pspdisplay.getInstance().sceDisplaySetHoldMode(gpr[4]);
		        break;
		    case 0x213e:
		        pspdisplay.getInstance().sceDisplaySetResumeMode(gpr[4]);
		        break;
		    case 0x213f:
		        pspdisplay.getInstance().sceDisplaySetFrameBuf(gpr[4], gpr[5], gpr[6], gpr[7]);
		        break;
		    case 0x2140:
		        pspdisplay.getInstance().sceDisplayGetFrameBuf(gpr[4], gpr[5], gpr[6], gpr[7]);
		        break;
		    case 0x2141:
		        pspdisplay.getInstance().sceDisplayIsForeground();
		        break;
		    case 0x2142:
		        pspdisplay.getInstance().sceDisplayGetBrightness(gpr[4], gpr[5]);
		        break;
		    case 0x2143:
		        pspdisplay.getInstance().sceDisplayGetVcount();
		        break;
		    case 0x2144:
		    	pspdisplay.getInstance().sceDisplayIsVblank();
		    	break;
		    case 0x2145:
		        pspdisplay.getInstance().sceDisplayWaitVblank();
		        break;
		    case 0x2146:
		        pspdisplay.getInstance().sceDisplayWaitVblankCB();
		        break;
		    case 0x2147:
		        pspdisplay.getInstance().sceDisplayWaitVblankStart();
		        break;
		    case 0x2148:
		        pspdisplay.getInstance().sceDisplayWaitVblankStartCB();
		        break;
		    case 0x2149:
		        pspdisplay.getInstance().sceDisplayGetCurrentHcount();
		        break;
		    case 0x214a:
		        pspdisplay.getInstance().sceDisplayGetAccumulatedHcount();
		        break;
		    case 0x214b:
		        pspdisplay.getInstance().sceDisplay_A83EF139();
		        break;
		    case 0x311f:
		        pspdisplay.getInstance().sceDisplayWaitVblankStartMulti();
		        break;

		    case 0xfffff: { // special code for unmapped imports
		        CpuState cpu = Emulator.getProcessor().cpu;
		        if(isEnableIgnoreUnmappedImports()) {
		            Modules.log.warn(String.format("IGNORING: Unmapped import @ 0x%08X - %08x %08x %08x",
		            cpu.pc, cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]));
		        }
		        else {
		        Modules.log.error(String.format("Unmapped import @ 0x%08X - %08x %08x %08x",
		            cpu.pc, cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]));
		        Emulator.PauseEmu();
		        }
		        break;
		    }

		    default:
		    {
		        // Try and handle as an HLE module export
		        boolean handled = HLEModuleManager.getInstance().handleSyscall(code);
		        if (!handled) {
		            CpuState cpu = Emulator.getProcessor().cpu;
		            cpu.gpr[2] = 0;

		            String params = String.format("%08x %08x %08x", cpu.gpr[4],
		                cpu.gpr[5], cpu.gpr[6]);

		            for (syscallsFirm15.calls c : syscallsFirm15.calls.values()) {
		                if (c.getSyscall() == code) {
		                    Modules.log.warn("Unsupported syscall " + Integer.toHexString(code) + " " + c + " " + params);
		                    return;
		                }
		            }
		            Modules.log.warn("Unsupported syscall " + Integer.toHexString(code) + " " + params);
		        }
		    }
		    break;
		}

        durationStatistics.end();
    }
}