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

import static jpcsp.HLE.SyscallHandler.syscallUnmappedImport;
import static jpcsp.util.HLEUtilities.J;
import static jpcsp.util.HLEUtilities.SYSCALL;

import jpcsp.Memory;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.HLE.kernel.types.SceModule;

public class DeferredStub {
    protected SceModule sourceModule;
    private String moduleName;
    private int importAddress;
    private int nid;
    private boolean savedImport;
    private int savedImport1;
    private int savedImport2;

    public DeferredStub(SceModule sourceModule, String moduleName, int importAddress, int nid) {
    	this.sourceModule = sourceModule;
        this.moduleName = moduleName;
        this.importAddress = importAddress & Memory.addressMask;
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

    protected void invalidate(int address, int size) {
    	RuntimeContext.invalidateRange(address, size);
    }

    public void resolve(Memory mem, int address) {
    	if (!savedImport) {
			savedImport1 = mem.read32(importAddress);
			savedImport2 = mem.read32(importAddress + 4);
	    	savedImport = true;
    	}

		// j <address>
        mem.write32(importAddress, J(address));
        mem.write32(importAddress + 4, 0); // write a nop over our "unmapped import detection special syscall"
        invalidate(importAddress, 8);
    }

    public void unresolve(Memory mem) {
    	if (savedImport) {
    		mem.write32(importAddress, savedImport1);
    		mem.write32(importAddress + 4, savedImport2);
    	} else {
        	// syscall <syscallUnmappedImport>
            mem.write32(importAddress + 4, SYSCALL(syscallUnmappedImport));
    	}
        invalidate(importAddress, 8);

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
