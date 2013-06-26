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
package jpcsp.format;

import jpcsp.AllegrexOpcodes;
import jpcsp.Memory;
import jpcsp.HLE.SyscallHandler;
import jpcsp.HLE.kernel.types.SceModule;

public class DeferredStub {
    private SceModule sourceModule;
    private String moduleName;
    private int importAddress;
    private int nid;
    private boolean savedImport;
    private int savedImport1;
    private int savedImport2;

    public DeferredStub(SceModule sourceModule, String moduleName, int importAddress, int nid) {
    	this.sourceModule = sourceModule;
        this.moduleName = moduleName;
        this.importAddress = importAddress;
        this.nid = nid;
    }

    public String getModuleName() {
        return moduleName;
    }

    public int getImportAddress() {
        return importAddress;
    }

    public int getNid() {
        return nid;
    }

    public void resolve(Memory mem, int address) {
    	if (!savedImport) {
			savedImport1 = mem.read32(importAddress);
			savedImport2 = mem.read32(importAddress + 4);
	    	savedImport = true;
    	}

		// j <address>
    	int instruction = (AllegrexOpcodes.J << 26) | ((address >> 2) & 0x03FFFFFF);

        mem.write32(importAddress, instruction);
        mem.write32(importAddress + 4, 0); // write a nop over our "unmapped import detection special syscall"
    }

    public void unresolve(Memory mem) {
    	if (savedImport) {
    		mem.write32(importAddress, savedImport1);
    		mem.write32(importAddress + 4, savedImport2);
    	} else {
        	// syscall <syscallUnmappedImport>
        	int instruction = (AllegrexOpcodes.SPECIAL << 26) | AllegrexOpcodes.SYSCALL | (SyscallHandler.syscallUnmappedImport << 6);
            mem.write32(importAddress + 4, instruction);
    	}

    	if (sourceModule != null) {
    		// Add this stub back to the list of unresolved imports from the source module
    		sourceModule.unresolvedImports.add(this);
    	}
    }

    @Override
	public String toString() {
		return String.format("0x%08X [0x%08X] Module '%s'", getImportAddress(), getNid(), getModuleName());
	}
}
