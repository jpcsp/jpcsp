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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.BufferedReader;
import static jpcsp.Syscallv15.*;

public class NIDMapper {
    private static NIDMapper instance;
    private HashMap<Integer, Integer> nidToSyscall;
    private HashMap<String, HashMap<Integer, Integer>> moduleToNidTable;

    public static NIDMapper get_instance() {
        if (instance == null) {
            instance = new NIDMapper();
        }
        return instance;
    }

    public void Initialise() {
        moduleToNidTable = new HashMap<String, HashMap<Integer, Integer>>();
       nidToSyscall = new HashMap<Integer, Integer>();
        for(int i=0; i<syscalls.length; i++)
        {
               int syscall = syscalls[i][0];
               long nid = syscalls[i][1];
                nidToSyscall.put((int)nid,syscall);
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
