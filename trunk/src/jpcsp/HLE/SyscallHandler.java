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

import java.util.Arrays;

import jpcsp.Emulator;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.format.DeferredStub;
import jpcsp.util.CpuDurationStatistics;
import jpcsp.util.DurationStatistics;

public class SyscallHandler {
	public static CpuDurationStatistics durationStatistics = new CpuDurationStatistics("Syscall");
    public static boolean ignoreUnmappedImports = false;
    private static CpuDurationStatistics[] syscallStatistics;
    public static final int syscallUnmappedImport = 0xFFFFF;

	public static void reset() {
		durationStatistics.reset();
		syscallStatistics = null;
	}

	public static void exit() {
		if (DurationStatistics.collectStatistics) {
	        Emulator.log.info(durationStatistics);
	        if (syscallStatistics != null) {
		        Arrays.sort(syscallStatistics);
		        int numberSyscalls = 20;
		        Emulator.log.info(numberSyscalls + " most time intensive Syscalls:");
		        for (int i = 0; i < numberSyscalls; i++) {
		        	Emulator.log.info("    " + syscallStatistics[i]);
		        }
	        }
		}
	}

	private static void initStatistics() {
		syscallStatistics = new CpuDurationStatistics[HLEModuleManager.getInstance().getMaxSyscallCode()];
		for (int i = 0; i < syscallStatistics.length; i++) {
			String name = HLEModuleManager.getInstance().functionName(i);
			if (name == null) {
				name = String.format("Syscall 0x%X", i);
			}
			syscallStatistics[i] = new CpuDurationStatistics(String.format("%-30s", name));
		}
	}

	public static boolean isEnableIgnoreUnmappedImports(){
        return ignoreUnmappedImports;
    }

    public static void setEnableIgnoreUnmappedImports(boolean enable){
        ignoreUnmappedImports = enable;
        if (enable) {
            Modules.log.info("Ignore Unmapped Imports enabled");
        }
    }

    public static void syscall(int code) {
        if (code == syscallUnmappedImport) { // special code for unmapped imports
            CpuState cpu = Emulator.getProcessor().cpu;

            String description = String.format("0x%08X", cpu.pc);
            // Search for the module & NID to provide a better description
            for (SceModule module : Managers.modules.values()) {
            	for (DeferredStub deferredStub : module.unresolvedImports) {
            		if (deferredStub.getImportAddress() == cpu.pc || deferredStub.getImportAddress() == cpu.pc - 4) {
            			description = deferredStub.toString();
            			break;
            		}
            	}
            }

            if (isEnableIgnoreUnmappedImports()) {
	            Modules.log.warn(String.format("IGNORING: Unmapped import at %s - %08x %08x %08x", description, cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]));
	        } else {
		        Modules.log.error(String.format("Unmapped import at %s - %08x %08x %08x", description, cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]));
		        Emulator.PauseEmu();
	        }
            cpu.gpr[2] = 0;
        } else {
	    	if (DurationStatistics.collectStatistics) {
		    	if (syscallStatistics == null) {
		    		initStatistics();
		    	}
	
		    	syscallStatistics[code].start();
		    	durationStatistics.start();
	    	}

	        // Try and handle as an HLE module export
	        boolean handled = HLEModuleManager.getInstance().handleSyscall(code);
	        if (!handled) {
	        	// Check if this is the syscall
	        	// for an HLE function currently being uninstalled
	        	HLEModuleFunction hleModuleFunction = HLEModuleManager.getInstance().getSyscallFunction(code);
	        	if (hleModuleFunction != null) {
	        		Modules.log.error(String.format("HLE Function %s(%s) not activated by default for Firmware Version %d", hleModuleFunction.getFunctionName(), hleModuleFunction.getModuleName(), Emulator.getInstance().getFirmwareVersion()));
	        	} else {
		            CpuState cpu = Emulator.getProcessor().cpu;
		            String name = "";
		            for (SyscallIgnore c : SyscallIgnore.values()) {
		                if (c.getSyscall() == code) {
		                	name = c.toString();
		                	break;
		                }
		            }
		            Modules.log.warn(String.format("Unsupported syscall %X %s %08X %08X %08X", code, name, cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]));
	        	}
	        }

	        if (DurationStatistics.collectStatistics) {
		    	syscallStatistics[code].end();
		        durationStatistics.end();
	        }
        }
    }
}