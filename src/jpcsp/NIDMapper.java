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
package jpcsp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import jpcsp.HLE.Modules;
import jpcsp.HLE.SyscallIgnore;
import jpcsp.HLE.kernel.types.SceModule;

public class NIDMapper {
	private static Logger log = Modules.log;
    private static NIDMapper instance;
    private HashMap<Integer, Integer> nidToSyscall;
    private HashMap<Integer, Integer> syscallToNid;
    private HashMap<String, HashMap<Integer, Integer>> moduleToNidTable;
    private Set<Integer> overwrittenSyscalls;

    public static NIDMapper getInstance() {
        if (instance == null) {
            instance = new NIDMapper();
        }
        return instance;
    }

    public void Initialise() {
        moduleToNidTable = new HashMap<String, HashMap<Integer, Integer>>();
        nidToSyscall = new HashMap<Integer, Integer>();
        syscallToNid = new HashMap<Integer, Integer>();
        overwrittenSyscalls = new HashSet<Integer>();

        for (SyscallIgnore c : SyscallIgnore.values()) {
            nidToSyscall.put(c.getNID(), c.getSyscall());
        }
    }

    private int nidToSyscallInternal(int nid) {
    	Integer code = nidToSyscall.get(nid);
        if (code == null) {
            return -1;
        }
        return code.intValue();
    }

    /** returns -1 if the nid couldn't be mapped */
    public int nidToSyscall(int nid) {
    	int syscall = nidToSyscallInternal(nid);
    	if (isOverwrittenSyscall(syscall)) {
    		// The HLE syscall has been overwritten by a prx
    		return -1;
    	}

    	return syscall;
    }

    public int syscallToNid(int code) {
    	Integer nid = syscallToNid.get(code);
    	if (nid == null) {
    		return 0;
    	}

    	return nid.intValue();
    }

    public boolean isOverwrittenSyscall(int code) {
    	return overwrittenSyscalls.contains(code);
    }

    public int overwrittenSyscallToAddress(int code) {
    	int nid = syscallToNid(code);

    	for (String moduleName : moduleToNidTable.keySet()) {
    		HashMap<Integer, Integer> nidToAddress = moduleToNidTable.get(moduleName);
    		Integer address = nidToAddress.get(nid);
    		if (address != null) {
    			return address.intValue();
    		}
    	}

    	return 0;
    }

    public int overwrittenSyscallAddressToCode(int address) {
    	for (String moduleName : moduleToNidTable.keySet()) {
    		HashMap<Integer, Integer> nidToAddress = moduleToNidTable.get(moduleName);
    		for (int nid : nidToAddress.keySet()) {
    			if (address == nidToAddress.get(nid).intValue()) {
    				return nidToSyscallInternal(nid);
    			}
    		}
    	}

    	return -1;
    }

    public int overwrittenNidToAddress(int nid) {
    	int code = nidToSyscallInternal(nid);
    	return overwrittenSyscallToAddress(code);
    }

    /**
     * This function is only for the HLE. It allows us to HLE modules, normally
     * a module would be loaded into memory, so imports would jump to the
     * function. What we are doing here is making the import a syscall, which
     * we can trap and turn into a Java function call.
     * @param code The syscall code. This must come from the unallocated set.
     * @param nid The NID the syscall will map to. */
    public void addSyscallNid(int nid, int code) {
    	syscallToNid.put(code, nid);
        nidToSyscall.put(nid, code);
    }

    /** @param modulename Example: sceRtc
     * @param address Address of export (example: start of function). */
    public void addModuleNid(SceModule module, String modulename, int nid, int address) {
        int syscall = nidToSyscall(nid);
        if (syscall != -1) {
    		// Only modules from flash0 are allowed to overwrite NIDs from syscalls
        	if (module.pspfilename == null || !module.pspfilename.startsWith("flash0:")) {
        		return;
        	}
        	if (log.isInfoEnabled()) {
        		log.info(String.format("NID 0x%08X at address 0x%08X from module '%s' overwriting an HLE syscall", nid, address, modulename));
        	}
    		overwrittenSyscalls.add(syscall);
        }

        HashMap<Integer, Integer> nidToAddress = moduleToNidTable.get(modulename);
        if (nidToAddress == null) {
            nidToAddress = new HashMap<Integer, Integer>();
            moduleToNidTable.put(modulename, nidToAddress);
            module.addModuleName(modulename);
        }

        nidToAddress.put(nid, address);
    }

    /** Use this when unloading modules */
    public void removeModuleNids(String modulename) {
         HashMap<Integer, Integer> nidToAddress = moduleToNidTable.remove(modulename);

        // The overwritten syscalls are now visible again
        if (nidToAddress != null && overwrittenSyscalls.size() > 0) {
    		for (int nid : nidToAddress.keySet()) {
    			int syscall = nidToSyscallInternal(nid);
    			if (syscall != -1) {
    				overwrittenSyscalls.remove(syscall);
    			}
    		}
    	}
    }

    /** returns -1 if the nid couldn't be mapped */
    public int moduleNidToAddress(String modulename, int nid) {
        HashMap<Integer, Integer> nidToAddress;
        Integer address;

        nidToAddress = moduleToNidTable.get(modulename);
        if (nidToAddress == null) {
            // module is not loaded
            return -1;
        }

        address = nidToAddress.get(nid);
        if (address == null) {
            // nid is not recognized
            return -1;
        }

        return address.intValue();
    }
}