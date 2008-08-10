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

import java.util.HashMap;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.BufferedReader;

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

    private NIDMapper() {
        //Initialise("syscalls.txt", "FW 1.50");
    }

    /**
     * @filename "syscalls.txt"
     * @param firmwareversion, one of: "FW 1.00", "FW 1.50", "FW 1.52", "FW 2.00"
     * or any other supported FW in syscalls.txt. */
    public void Initialise(String filename, String firmwareversion) {
        //System.out.println("NIDMapper: Initialise");

        //nidToSyscall  =...; // assigned inside loadSyscallsTxt
        moduleToNidTable = new HashMap<String, HashMap<Integer, Integer>>();

        // Load syscalls.txt (3-way syscall/nid/function name mappings)
        loadSyscallsTxt(filename, firmwareversion);
    }

    public void loadSyscallsTxt(String filename, String firmwareversion) {
        final int STAGE_SEARCH = 0;
        final int STAGE_PARSE = 1;
        final int STAGE_DONE = 2;
        int stage = STAGE_SEARCH;
        int count = 0;

        nidToSyscall = new HashMap<Integer, Integer>();

        try {
            FileReader fr = new FileReader(filename);
            BufferedReader br = new BufferedReader(fr);
            String line;

            while((line = br.readLine()) != null &&
                stage != STAGE_DONE) {
                switch(stage) {
                    case STAGE_SEARCH:
                        if (line.equals(firmwareversion)) {
                            stage = STAGE_PARSE;
                        }
                        break;

                    case STAGE_PARSE:
                        if (line.startsWith("FW ")) {
                            stage = STAGE_DONE;
                        } else {
                            // lines are in this format: syscall,nid,name
                            String[] part = line.split(",");
                            if (part.length == 3) {
                                int syscall = Integer.parseInt(part[0].substring(2), 16);
                                long nid = Long.parseLong(part[1].substring(2), 16);
                                nidToSyscall.put((int)nid, syscall);
                                count++;
                            }
                        }
                        break;

                    default:
                        break;
                }
            }

            br.close();
            fr.close();
        } catch(FileNotFoundException e) {
            System.out.println("File not found: " + filename);
        } catch(Exception e) {
            e.printStackTrace();
        }

        //System.out.println("Loaded " + count + " syscalls");
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
