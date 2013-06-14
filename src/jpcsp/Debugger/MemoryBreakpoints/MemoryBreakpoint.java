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
package jpcsp.Debugger.MemoryBreakpoints;

import jpcsp.Memory;
import jpcsp.memory.DebuggerMemory;

public class MemoryBreakpoint {

    public enum AccessType {
        READ, WRITE, READWRITE
    };
    private int start_address;
    private int end_address;
    private AccessType access;
    private boolean installed;

    public MemoryBreakpoint() {
        start_address = 0x00000000;
        end_address = 0x00000000;
        access = AccessType.READ;
        installed = false;
    }

    public MemoryBreakpoint(DebuggerMemory debuggerMemory, int start_address, int end_address, AccessType access) {
        setStartAddress(start_address);
        setEndAddress(end_address);
        this.access = access;

        install(debuggerMemory);
    }

    public MemoryBreakpoint(DebuggerMemory debuggerMemory, int address, AccessType access) {
        setStartAddress(address);
        setEndAddress(address);
        this.access = access;

        install(debuggerMemory);
    }

    final public int getStartAddress() {
        return start_address;
    }

    final public void setStartAddress(int start_address) {
        this.start_address = Memory.normalizeAddress(start_address);
    }

    final public int getEndAddress() {
        return end_address;
    }

    final public void setEndAddress(int end_address) {
        this.end_address = Memory.normalizeAddress(end_address);
    }

    private static DebuggerMemory getDebuggerMemory() {
    	Memory mem = Memory.getInstance();
    	if (mem instanceof DebuggerMemory) {
    		return (DebuggerMemory) mem;
    	}

    	return null;
    }

    final public void setEnabled(boolean enabled) {
        if (installed & !enabled) {
            uninstall(getDebuggerMemory());
        } else if (!installed & enabled) {
            install(getDebuggerMemory());
        }
    }

    final public boolean isEnabled() {
        return installed;
    }

    public AccessType getAccess() {
        return access;
    }

    public void setAccess(AccessType access) {
        this.access = access;
    }

    private void install(DebuggerMemory debuggerMemory) {
    	if (debuggerMemory != null) {
	        switch (access) {
	            case READ:
	            	debuggerMemory.addRangeReadBreakpoint(start_address, end_address);
	                break;
	            case WRITE:
	            	debuggerMemory.addRangeWriteBreakpoint(start_address, end_address);
	                break;
	            case READWRITE:
	            	debuggerMemory.addRangeReadWriteBreakpoint(start_address, end_address);
	                break;
	        }
	        installed = true;
    	}
    }

    private void uninstall(DebuggerMemory debuggerMemory) {
    	if (debuggerMemory != null) {
	        switch (access) {
	            case READ:
	            	debuggerMemory.removeRangeReadBreakpoint(start_address, end_address);
	                break;
	            case WRITE:
	            	debuggerMemory.removeRangeWriteBreakpoint(start_address, end_address);
	                break;
	            case READWRITE:
	            	debuggerMemory.removeRangeReadWriteBreakpoint(start_address, end_address);
	                break;
	        }
	        installed = false;
    	}
    }
}
