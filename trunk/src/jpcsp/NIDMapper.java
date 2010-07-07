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

import jpcsp.HLE.SyscallIgnore;

public class NIDMapper {
    private static NIDMapper instance;
    private HashMap<Integer, Integer> nidToSyscall;
    private HashMap<String, HashMap<Integer, Integer>> moduleToNidTable;

    public static NIDMapper getInstance() {
        if (instance == null) {
            instance = new NIDMapper();
        }
        return instance;
    }

    public void Initialise() {
        moduleToNidTable = new HashMap<String, HashMap<Integer, Integer>>();
        nidToSyscall = new HashMap<Integer, Integer>();

        for (SyscallIgnore c : SyscallIgnore.values()) {
            nidToSyscall.put(c.getNID(), c.getSyscall());
        }
    }

    /** returns -1 if the nid couldn't be mapped */
    public int nidToSyscall(int nid) {
        Integer code = nidToSyscall.get(nid);
        if (code == null) {
            return -1;
        }
        return code.intValue();
    }

    /**
     * This function is only for the HLE. It allows us to HLE modules, normally
     * a module would be loaded into memory, so imports would jump to the
     * function. What we are doing here is making the import a syscall, which
     * we can trap and turn into a Java function call.
     * @param code The syscall code. This must come from the unallocated set.
     * @param nid The NID the syscall will map to. */
    public void addSyscallNid(int nid, int code) {
        nidToSyscall.put(nid, code);
    }

    /** @param modulename Example: sceRtc
     * @param address Address of export (example: start of function). */
    public void addModuleNid(String modulename, int nid, int address) {
        HashMap<Integer, Integer> nidToAddress;

        nidToAddress = moduleToNidTable.get(modulename);
        if (nidToAddress == null) {
            nidToAddress = new HashMap<Integer, Integer>();
            moduleToNidTable.put(modulename, nidToAddress);
        }

        nidToAddress.put(nid, address);
    }

    /** Use this when unloading modules */
    public void removeModuleNids(String modulename) {
        moduleToNidTable.remove(modulename);
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