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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.SceModule;

public class NIDMapper {
	private static Logger log = Modules.log;
    private static NIDMapper instance;
    private final Map<Integer, NIDInfo> syscallMap;
    private final Map<String, Map<Integer, NIDInfo>> moduleNidMap;
    private final Map<Integer, NIDInfo> nidMap;
    private final Map<Integer, NIDInfo> addressMap;
    private final Map<String, NIDInfo> nameMap;
    private int freeSyscallNumber;

    protected static class NIDInfo {
    	private final int nid;
		private final int syscall;
    	private int address;
    	private final String name;
    	private final String moduleName;
    	private final boolean variableExport; // Is coming from a function or variable export?
    	private int firmwareVersion;
    	private boolean overwritten;
    	private boolean loaded;
    	private boolean validModuleName;

    	/**
    	 * New NIDInfo for a NID from a loaded module.
    	 *
    	 * @param nid
    	 * @param address
    	 * @param moduleName
    	 */
    	public NIDInfo(int nid, int address, String moduleName, boolean variableExport) {
			this.nid = nid;
			this.address = address;
			this.moduleName = moduleName;
			this.variableExport = variableExport;
			name = null;
			syscall = -1;
			firmwareVersion = 999;
			overwritten = false;
			loaded = true;
			validModuleName = true;
		}

    	/**
    	 * New NIDInfo for a NID from an HLE syscall.
    	 *
    	 * @param nid
    	 * @param syscall
    	 * @param name
    	 * @param moduleName
    	 * @param firmwareVersion
    	 */
    	public NIDInfo(int nid, int syscall, String name, String moduleName, int firmwareVersion) {
    		this.nid = nid;
    		this.syscall = syscall;
    		this.name = name;
    		this.moduleName = moduleName;
    		variableExport = false;
    		this.firmwareVersion = firmwareVersion;
    		address = 0;
    		overwritten = false;
			loaded = true;
			validModuleName = false; // the given moduleName is probably not the correct one...
    	}

    	public int getNid() {
			return nid;
		}

		public int getSyscall() {
			return syscall;
		}

		public boolean hasSyscall() {
			return syscall >= 0;
		}

		public int getAddress() {
			return address;
		}

		private void setAddress(int address) {
			this.address = address;
		}

		public boolean hasAddress() {
			return address != 0;
		}

		public String getName() {
			return name;
		}

		public boolean hasName() {
			return name != null && name.length() > 0;
		}

		public String getModuleName() {
			return moduleName;
		}

		public boolean isOverwritten() {
			return overwritten;
		}

		private void setOverwritten(boolean overwritten) {
			this.overwritten = overwritten;
		}

		public void overwrite(int address) {
			setOverwritten(true);
			setAddress(address);
		}

		public void undoOverwrite() {
			setOverwritten(false);
			setAddress(0);
		}

		public int getFirmwareVersion() {
			return firmwareVersion;
		}

		public void setFirmwareVersion(int firmwareVersion) {
			this.firmwareVersion = firmwareVersion;
		}

		public boolean isFromModule(String moduleName) {
			return moduleName.equals(this.moduleName);
		}

		public boolean isLoaded() {
			return loaded;
		}

		public void setLoaded(boolean loaded) {
			this.loaded = loaded;
		}

		public boolean isValidModuleName() {
			return validModuleName;
		}

		public boolean isVariableExport() {
			return variableExport;
		}

		@Override
		public String toString() {
			StringBuilder s = new StringBuilder();

			if (name != null) {
				s.append(String.format("%s(nid=0x%08X)", name, nid));
			} else {
				s.append(String.format("nid=0x%08X", nid));
			}
			s.append(String.format(", moduleName='%s'", moduleName));
			if (!isValidModuleName()) {
				s.append("(probably invalid)");
			}
			if (isVariableExport()) {
				s.append(", variable export");
			}
			s.append(String.format(", firmwareVersion=%d", firmwareVersion));
			if (isOverwritten()) {
				s.append(", overwritten");
			}
			if (hasAddress()) {
				s.append(String.format(", address=0x%08X", address));
			}
			if (hasSyscall()) {
				s.append(String.format(", syscall=0x%X", syscall));
			}

			return s.toString();
		}
    }

    public static NIDMapper getInstance() {
        if (instance == null) {
            instance = new NIDMapper();
        }
        return instance;
    }

    private NIDMapper() {
    	moduleNidMap = new HashMap<>();
    	nidMap = new HashMap<>();
    	syscallMap = new HashMap<>();
    	addressMap = new HashMap<>();
    	nameMap = new HashMap<>();
		// Official syscalls start at 0x2000,
		// so we'll put the HLE syscalls far away at 0x4000.
    	freeSyscallNumber = 0x4000;
    }

    private void addNIDInfo(NIDInfo info) {
    	Map<Integer, NIDInfo> moduleMap = moduleNidMap.get(info.getModuleName());
    	if (moduleMap == null) {
    		moduleMap = new HashMap<Integer, NIDInfo>();
    		moduleNidMap.put(info.getModuleName(), moduleMap);
    	}
    	moduleMap.put(info.getNid(), info);

    	// For HLE NID's, do not trust the module names defined in Jpcsp, use only the NID.
    	if (!info.isValidModuleName()) {
    		nidMap.put(info.getNid(), info);
    	}

    	if (info.hasAddress()) {
    		addressMap.put(info.getAddress(), info);
    	}

    	if (info.hasSyscall()) {
    		syscallMap.put(info.getSyscall(), info);
    	}

    	if (info.hasName()) {
    		nameMap.put(info.getName(), info);
    	}
    }

    private void removeNIDInfo(NIDInfo info) {
    	Map<Integer, NIDInfo> moduleMap = moduleNidMap.get(info.getModuleName());
    	if (moduleMap != null) {
    		moduleMap.remove(info.getNid());
    		if (moduleMap.isEmpty()) {
    			moduleMap.remove(info.getModuleName());
    		}
    	}

    	// For HLE NID's, do not trust the module names defined in Jpcsp, use only the NID.
    	if (!info.isValidModuleName()) {
    		nidMap.remove(info.getNid());
    	}

    	if (info.hasAddress()) {
    		addressMap.remove(info.getAddress());
    	}

    	if (info.hasSyscall()) {
    		syscallMap.remove(info.getSyscall());
    	}

    	if (info.hasName()) {
    		syscallMap.remove(info.getName());
    	}
    }

    private NIDInfo getNIDInfoByNid(String moduleName, int nid) {
    	NIDInfo info = null;

    	Map<Integer, NIDInfo> moduleMap = moduleNidMap.get(moduleName);
    	if (moduleMap != null) {
        	info = moduleMap.get(nid);
    	}

    	// For HLE NID's, do not trust the module names defined in Jpcsp, use only the NID.
    	if (info == null) {
    		info = nidMap.get(nid);
    	}

    	return info;
    }

    private NIDInfo getNIDInfoBySyscall(int syscall) {
    	return syscallMap.get(syscall);
    }

    private NIDInfo getNIDInfoByAddress(int address) {
    	return addressMap.get(address);
    }

    private NIDInfo getNIDInfoByName(String name) {
    	return nameMap.get(name);
    }

    public int getNewSyscallNumber() {
    	return freeSyscallNumber++;
    }

    /**
     * Add a NID from an HLE syscall.
     *
     * @param nid             the nid
     * @param name            the function name
     * @param moduleName      the module name
     * @param firmwareVersion the firmware version defining this nid
     * @return                true if the NID has been added
     *                        false if the NID was already added
     */
    public boolean addHLENid(int nid, String name, String moduleName, int firmwareVersion) {
    	NIDInfo info = getNIDInfoByNid(moduleName, nid);
    	if (info != null) {
    		// This NID is already added, verify that we are trying to use the same data
    		if (!name.equals(info.getName()) || !moduleName.equals(info.getModuleName()) || firmwareVersion != info.getFirmwareVersion()) {
    			return false;
    		}
    		return true;
    	}

    	int syscall = getNewSyscallNumber();
    	info = new NIDInfo(nid, syscall, name, moduleName, firmwareVersion);

    	addNIDInfo(info);

    	return true;
    }

    /**
     * Add a NID loaded from a module.
     *
     * @param module     the loaded module
     * @param moduleName the module name
     * @param nid        the nid
     * @param address    the address of the nid
     * @param variableExport coming from a function or variable export
     */
    public void addModuleNid(SceModule module, String moduleName, int nid, int address, boolean variableExport) {
    	address &= Memory.addressMask;

    	NIDInfo info = getNIDInfoByNid(moduleName, nid);
    	if (info != null) {
    		// Only modules from flash0 are allowed to overwrite NIDs from syscalls
        	if (module.pspfilename == null || !module.pspfilename.startsWith("flash0:")) {
        		return;
        	}
        	if (log.isInfoEnabled()) {
        		log.info(String.format("NID %s[0x%08X] at address 0x%08X from module '%s' overwriting an HLE syscall", info.getName(), nid, address, moduleName));
        	}
        	info.overwrite(address);
        	addressMap.put(address, info);
    	} else {
    		info = new NIDInfo(nid, address, moduleName, variableExport);

    		addNIDInfo(info);
    	}
    }

    /**
     * Remove all the NIDs that have been loaded from a module.
     *
     * @param moduleName the module name
     */
    public void removeModuleNids(String moduleName) {
    	List<NIDInfo> nidsToBeRemoved = new LinkedList<NIDInfo>();
    	List<Integer> addressesToBeRemoved = new LinkedList<Integer>();
    	for (NIDInfo info : addressMap.values()) {
    		if (info.isFromModule(moduleName)) {
    			if (info.isOverwritten()) {
    				addressesToBeRemoved.add(info.getAddress());
    				info.undoOverwrite();
    			} else {
    				nidsToBeRemoved.add(info);
    			}
    		}
    	}

    	for (NIDInfo info : nidsToBeRemoved) {
    		removeNIDInfo(info);
    	}

    	for (Integer address : addressesToBeRemoved) {
    		addressMap.remove(address);
    	}
    }

    public int getAddressByNid(int nid, String moduleName) {
    	NIDInfo info = getNIDInfoByNid(moduleName, nid);
    	if (info == null || !info.hasAddress()) {
    		return 0;
    	}

    	if (moduleName != null && !info.isFromModule(moduleName)) {
    		log.debug(String.format("Trying to resolve %s from module '%s'", info, moduleName));
    	}

    	return info.getAddress();
    }

    public int getAddressByNid(int nid) {
    	return getAddressByNid(nid, null);
    }

    public int getAddressBySyscall(int syscall) {
    	NIDInfo info = getNIDInfoBySyscall(syscall);
    	if (info == null || !info.hasAddress()) {
    		return 0;
    	}

    	return info.getAddress();
    }

    public int getAddressByName(String name) {
    	NIDInfo info = getNIDInfoByName(name);
    	if (info == null || !info.hasAddress()) {
    		return 0;
    	}

    	return info.getAddress();
    }

    public int getSyscallByNid(int nid, String moduleName) {
    	NIDInfo info = getNIDInfoByNid(moduleName, nid);
    	if (info == null || !info.hasSyscall()) {
    		return -1;
    	}

    	if (moduleName != null && !info.isFromModule(moduleName)) {
    		log.debug(String.format("Trying to resolve %s from module '%s'", info, moduleName));
    	}

    	return info.getSyscall();
    }

    public int getSyscallByNid(int nid) {
    	return getSyscallByNid(nid, null);
    }

    public String getNameBySyscall(int syscall) {
    	NIDInfo info = getNIDInfoBySyscall(syscall);
    	if (info == null) {
    		return null;
    	}

    	return info.getName();
    }

    public int getNidBySyscall(int syscall) {
    	NIDInfo info = getNIDInfoBySyscall(syscall);
    	if (info == null) {
    		return 0;
    	}

    	return info.getNid();
    }

    public int getNidByAddress(int address) {
    	NIDInfo info = getNIDInfoByAddress(address);
    	if (info == null) {
    		return 0;
    	}

    	return info.getNid();
    }

    public void unloadNid(int nid) {
    	// Search for the NID in all the modules
    	for (String moduleName : moduleNidMap.keySet()) {
        	NIDInfo info = getNIDInfoByNid(moduleName, nid);
        	if (info != null) {
            	info.setLoaded(false);
        	}
    	}
    }

    public void unloadAll() {
    	for (Map<Integer, NIDInfo> moduleMap : moduleNidMap.values()) {
        	for (NIDInfo info : moduleMap.values()) {
        		if (info.isOverwritten()) {
        			info.undoOverwrite();
        		}
        		info.setLoaded(false);
        	}
    	}
    }

    public int[] getModuleNids(String moduleName) {
    	Map<Integer, NIDInfo> moduleMap = moduleNidMap.get(moduleName);
    	if (moduleMap == null) {
    		return null;
    	}

    	Integer[] nids = moduleMap.keySet().toArray(new Integer[moduleMap.size()]);
    	if (nids == null) {
    		return null;
    	}

    	int[] result = new int[nids.length];
    	for (int i = 0; i < nids.length; i++) {
    		result[i] = nids[i].intValue();
    	}

    	return result;
    }

    public String[] getModuleNames() {
    	String[] moduleNames = moduleNidMap.keySet().toArray(new String[moduleNidMap.size()]);
    	return moduleNames;
    }

    public boolean isVariableExportByAddress(int address) {
    	NIDInfo info = getNIDInfoByAddress(address);
    	if (info == null) {
    		return false;
    	}

    	return info.isVariableExport();
    }
}