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

package jpcsp.HLE.modules;

import java.lang.reflect.Method;

/**
 *
 * @author fiveofhearts
 */
public class HLEModuleFunction {
    private int syscallCode;
    private final String moduleName;
    private final String functionName;
    private int nid;
    private boolean unimplemented;
    private String loggingLevel;
	private Method hleModuleMethod;
	private boolean checkInsideInterrupt;
	private boolean checkDispatchThreadEnabled;
	private HLEModule hleModule;

    public HLEModuleFunction(String moduleName, String functionName, HLEModule hleModule, Method hleModuleMethod, boolean checkInsideInterrupt, boolean checkDispatchThreadEnabled) {
        this.moduleName = moduleName;
        this.functionName = functionName;
		this.checkInsideInterrupt = checkInsideInterrupt;
		this.checkDispatchThreadEnabled = checkDispatchThreadEnabled;
		this.hleModuleMethod = hleModuleMethod; 
		this.hleModule = hleModule;
    }

    public final void setSyscallCode(int syscallCode) {
        this.syscallCode = syscallCode;
    }

    public final int getSyscallCode() {
        return syscallCode;
    }
    
    public final String getModuleName() {
        return moduleName;
    }

    public final String getFunctionName() {
        return functionName;
    }

    public final void setNid(int nid) {
        this.nid = nid;
    }

    public final int getNid() {
        return nid;
    }
    
    public final void setUnimplemented(boolean unimplemented) {
        this.unimplemented = unimplemented;
    }

    public final boolean isUnimplemented() {
        return unimplemented;
    }

	public String getLoggingLevel() {
		return loggingLevel;
	}

	public void setLoggingLevel(String loggingLevel) {
		this.loggingLevel = loggingLevel;
	}

	public boolean checkDispatchThreadEnabled() {
		return checkDispatchThreadEnabled;
	}
	
	public boolean checkInsideInterrupt() {
		return checkInsideInterrupt;
	}

	public Method getHLEModuleMethod() {
		return hleModuleMethod;
	}

	public HLEModule getHLEModule() {
		return hleModule;
	}

    @Override
    public String toString() {
    	return String.format("HLEModuleFunction(moduleName='%s', functionName='%s', nid=0x%08X, syscallCode=%d)", moduleName, functionName, nid, syscallCode);
    }
}
