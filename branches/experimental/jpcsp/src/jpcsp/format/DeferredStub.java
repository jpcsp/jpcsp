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

public class DeferredStub {
    private String moduleName;
    private int importAddress;
    private int nid;

    public DeferredStub(String moduleName, int importAddress, int nid) {
        this.moduleName = moduleName;
        this.importAddress = importAddress;
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
}
