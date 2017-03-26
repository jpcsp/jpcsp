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
    private final Map<Integer, NIDInfo> nidMap;
    private final Map<Integer, NIDInfo> addressMap;
    private int freeSyscallNumber;

    protected static class NIDInfo {
    	private final int nid;
		private final int syscall;
    	private int address;
    	private final String name;
    	private final String moduleName;
    	private int firmwareVersion;
    	private boolean overwritten;
    	private boolean loaded;

    	/**
    	 * New NIDInfo for a NID from a loaded module.
    	 *
    	 * @param nid
    	 * @param address
    	 * @param moduleName
    	 */
    	public NIDInfo(int nid, int address, String moduleName) {
			this.nid = nid;
			this.address = address;
			this.moduleName = moduleName;
			name = null;
			syscall = -1;
			firmwareVersion = 999;
			overwritten = false;
			loaded = true;
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
    		this.firmwareVersion = firmwareVersion;
    		address = 0;
    		overwritten = false;
			loaded = true;
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

		@Override
		public String toString() {
			StringBuilder s = new StringBuilder();

			if (name != null) {
				s.append(String.format("%s(nid=0x%08X)", name, nid));
			} else {
				s.append(String.format("nid=0x%08X", nid));
			}
			s.append(String.format(", moduleName='%s', firmwareVersion=%d", moduleName, firmwareVersion));
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

		public boolean isLoaded() {
			return loaded;
		}

		public void setLoaded(boolean loaded) {
			this.loaded = loaded;
		}
    }

    public static NIDMapper getInstance() {
        if (instance == null) {
            instance = new NIDMapper();
        }
        return instance;
    }

    private NIDMapper() {
    	nidMap = new HashMap<>();
    	syscallMap = new HashMap<>();
    	addressMap = new HashMap<>();
		// Official syscalls start at 0x2000,
		// so we'll put the HLE syscalls far away at 0x4000.
    	freeSyscallNumber = 0x4000;
    }

    private void addNIDInfo(NIDInfo info) {
    	nidMap.put(info.getNid(), info);
    	if (info.hasAddress()) {
    		addressMap.put(info.getAddress(), info);
    	}
    	if (info.hasSyscall()) {
    		syscallMap.put(info.getSyscall(), info);
    	}
    }

    private void removeNIDInfo(NIDInfo info) {
    	nidMap.remove(info.getNid());
    	if (info.hasAddress()) {
    		addressMap.remove(info.getAddress());
    	}
    	if (info.hasSyscall()) {
    		syscallMap.remove(info.getSyscall());
    	}
    }

    private NIDInfo getNIDInfoByNid(int nid) {
    	return nidMap.get(nid);
    }

    private NIDInfo getNIDInfoBySyscall(int syscall) {
    	return syscallMap.get(syscall);
    }

    private NIDInfo getNIDInfoByAddress(int address) {
    	return addressMap.get(address);
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
    	if (getNIDInfoByNid(nid) != null) {
    		// This NID is already added
    		return false;
    	}

    	int syscall = getNewSyscallNumber();
    	NIDInfo info = new NIDInfo(nid, syscall, name, moduleName, firmwareVersion);

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
     */
    public void addModuleNid(SceModule module, String moduleName, int nid, int address) {
    	NIDInfo info = getNIDInfoByNid(nid);
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
    		info = new NIDInfo(nid, address, moduleName);

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
    	NIDInfo info = getNIDInfoByNid(nid);
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

    public int getSyscallByNid(int nid, String moduleName) {
    	NIDInfo info = getNIDInfoByNid(nid);
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
    	NIDInfo info = getNIDInfoByNid(nid);
    	if (info == null) {
    		return;
    	}

    	info.setLoaded(false);
    }

    public void loadNid(int nid) {
    	NIDInfo info = getNIDInfoByNid(nid);
    	if (info == null) {
    		return;
    	}

    	info.setLoaded(true);
    }
}