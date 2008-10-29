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

import java.util.List;
import java.util.LinkedList;
import jpcsp.Loader;
import jpcsp.format.DeferredStub;
import jpcsp.format.PSF;
import jpcsp.format.PSPModuleInfo;

public class ModuleContext {

    // See Loader class for valid formats
    public int fileFormat;
    public String pspfilename; // boot path
    public PSF psf;

    // The space consumed by the program image
    public int loadAddressLow, loadAddressHigh;
    public int baseAddress; // should in theory be the same as loadAddressLow
    public int entryAddress; // this will be the relocated entry address

    // address/size pairs
    // used by the debugger/instruction counter
    public int[] textsection;
    public int[] initsection;
    public int[] finisection;
    public int[] stubtextsection;

    // .rodata.sceModuleInfo
    public PSPModuleInfo moduleInfo;

    public List<DeferredStub> unresolvedImports;
    public int importFixupAttempts;

    public ModuleContext() {
        fileFormat = Loader.FORMAT_UNKNOWN;
        pspfilename = "";

        loadAddressLow = 0;
        loadAddressHigh = 0;
        baseAddress = 0;
        entryAddress = 0;

        textsection = new int[2];
        initsection = new int[2];
        finisection = new int[2];
        stubtextsection = new int[2];

        moduleInfo = new PSPModuleInfo();

        unresolvedImports = new LinkedList<DeferredStub>();
        importFixupAttempts = 0;
    }
}
