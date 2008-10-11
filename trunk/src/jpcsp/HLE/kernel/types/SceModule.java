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

package jpcsp.HLE.kernel.types;

import jpcsp.HLE.kernel.managers.*;

public class SceModule extends SceUid {

    private String name;
    private int startAddr;
    private int attr;
    public static final int flashModuleUid = 0;

    public SceModule() {
        super("SceModule", SceUidManager.getNewUid("SceModule"));
    }

    public SceModule(String name, int startAddr, int attr) {
        super("SceModule", SceUidManager.getNewUid("SceModule"));
        setName(name);
        setStartAddr(startAddr);
        setAttr(attr);
        // TODO allocate some mem, write ourself to it, add getAddress() function (for use by sceKernelFindModuleBy*)
    }

    public String getName(){
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getStartAddr() {
        return startAddr;
    }

    public void setStartAddr(int startAddr) {
        this.startAddr = startAddr;
    }

    public int getAttr() {
        return attr;
    }

    public void setAttr(int attr) {
        this.attr = attr;
    }
}
