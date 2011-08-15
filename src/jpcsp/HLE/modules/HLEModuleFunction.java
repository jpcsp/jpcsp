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

import jpcsp.Processor;

/**
 *
 * @author fiveofhearts
 */
public abstract class HLEModuleFunction {
    private int syscallCode;
    private final String moduleName;
    private final String functionName;
    private int nid;

    public HLEModuleFunction(String moduleName, String functionName) {
        this.moduleName = moduleName;
        this.functionName = functionName;
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

    public abstract void execute(Processor cpu);
    
    public abstract String compiledString();
    
    @Override
    public String toString() {
    	return "HLEModuleFunction(moduleName='" + moduleName + "', functionName='" + functionName + "', nid=" + nid + ", syscallCode=" + syscallCode + ")";
    }
}
