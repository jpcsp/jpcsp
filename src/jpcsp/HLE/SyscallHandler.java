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

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.NIDMapper;
import jpcsp.Allegrex.Common.Instruction;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.Allegrex.Common;
import jpcsp.Allegrex.CpuState;
import jpcsp.Allegrex.Decoder;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.format.DeferredStub;
import jpcsp.settings.AbstractBoolSettingsListener;
import jpcsp.settings.Settings;
import jpcsp.util.Utilities;

public class SyscallHandler {
	private static Logger log = Modules.log;
    public static boolean ignoreUnmappedImports = false;
    public static final int syscallUnmappedImport = 0xFFFFF;
    private static IgnoreUnmappedImportsSettingsListerner ignoreUnmappedImportsSettingsListerner;

	private static class IgnoreUnmappedImportsSettingsListerner extends AbstractBoolSettingsListener {
		@Override
		protected void settingsValueChanged(boolean value) {
			setEnableIgnoreUnmappedImports(value);
		}
	}

	private static boolean isEnableIgnoreUnmappedImports(){
        return ignoreUnmappedImports;
    }

    private static void setEnableIgnoreUnmappedImports(boolean enable){
        ignoreUnmappedImports = enable;
        if (enable) {
            log.info("Ignore Unmapped Imports enabled");
        }
    }

    private static void logMem(Memory mem, int address, String registerName) {
    	if (Memory.isAddressGood(address)) {
    		log.error(String.format("Memory at %s:%s", registerName, Utilities.getMemoryDump(address, 64)));
    	}
    }

    private static void unsupportedSyscall(int code) throws Exception {
    	if (ignoreUnmappedImportsSettingsListerner == null) {
    		ignoreUnmappedImportsSettingsListerner = new IgnoreUnmappedImportsSettingsListerner();
    		Settings.getInstance().registerSettingsListener("SyscallHandler", "emu.ignoreUnmappedImports", ignoreUnmappedImportsSettingsListerner);
    	}

    	NIDMapper nidMapper = NIDMapper.getInstance();

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
	            log.warn(String.format("IGNORING: Unmapped import at %s - $a0=0x%08X $a1=0x%08X $a2=0x%08X", description, cpu._a0, cpu._a1, cpu._a2));
	        } else {
		        log.error(String.format("Unmapped import at %s:", description));
		        log.error(String.format("Registers: $a0=0x%08X, $a1=0x%08X, $a2=0x%08X, $a3=0x%08X", cpu._a0, cpu._a1, cpu._a2, cpu._a3));
		        log.error(String.format("           $t0=0x%08X, $t1=0x%08X, $t2=0x%08X, $t3=0x%08X", cpu._t0, cpu._t1, cpu._t2, cpu._t3));
		        log.error(String.format("           $ra=0x%08X, $sp=0x%08X", cpu._ra, cpu._sp));
		        Memory mem = Emulator.getMemory();
		        log.error(String.format("Caller code:"));
		        for (int i = -96; i <= 40; i += 4) {
		        	int address = cpu._ra + i;
		        	int opcode = mem.read32(address);
		        	Instruction insn = Decoder.instruction(opcode);
		        	String disasm = insn.disasm(address, opcode);
		        	log.error(String.format("%c 0x%08X:[%08X]: %s", i == -8 ? '>' : ' ', address, opcode, disasm));
		        }
		        logMem(mem, cpu._a0, Common.gprNames[Common._a0]);
		        logMem(mem, cpu._a1, Common.gprNames[Common._a1]);
		        logMem(mem, cpu._a2, Common.gprNames[Common._a2]);
		        logMem(mem, cpu._a3, Common.gprNames[Common._a3]);
		        logMem(mem, cpu._t0, Common.gprNames[Common._t0]);
		        logMem(mem, cpu._t1, Common.gprNames[Common._t1]);
		        logMem(mem, cpu._t2, Common.gprNames[Common._t2]);
		        logMem(mem, cpu._t3, Common.gprNames[Common._t3]);
		        Emulator.PauseEmu();
	        }
            cpu._v0 = 0;
        } else {
        	int address = nidMapper.getAddressBySyscall(code);

        	if (address != 0) {
	        	if (log.isDebugEnabled()) {
	        		String name = nidMapper.getNameBySyscall(code);
	        		int nid = nidMapper.getNidBySyscall(code);
	        		if (name != null) {
	        			log.debug(String.format("Jumping to 0x%08X instead of overwritten syscall %s[0x%08X]", address, name, nid));
	        		} else {
	        			log.debug(String.format("Jumping to 0x%08X instead of overwritten syscall NID 0x%08X", address, nid));
	        		}
	        	}

	        	RuntimeContext.executeFunction(address);
        	} else {
	        	// Check if this is the syscall
	        	// for an HLE function currently being uninstalled
        		String name = nidMapper.getNameBySyscall(code);
        		if (name != null) {
	        		log.error(String.format("HLE Function %s not activated by default for Firmware Version %d", name, Emulator.getInstance().getFirmwareVersion()));
        		} else {
	        		int nid = nidMapper.getNidBySyscall(code);
	        		log.error(String.format("NID 0x%08X not activated by default for Firmware Version %d", nid, Emulator.getInstance().getFirmwareVersion()));
        		}
        	}
        }
    }

    public static void syscall(int code) throws Exception {
    	// All syscalls are now implemented natively in the compiler
    	unsupportedSyscall(code);
    }
}