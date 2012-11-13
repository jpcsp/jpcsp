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
package jpcsp.HLE.modules150;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.modules.HLEModule;

import org.apache.log4j.Logger;

@HLELogging
public class InterruptManager extends HLEModule {
    public static Logger log = Modules.getLogger("InterruptManager");

	@Override
	public String getName() {
		return "InterruptManager";
	}

	@Override
	public void stop() {
		Managers.intr.stop();
		super.stop();
	}

	@HLEFunction(nid = 0xCA04A2B9, version = 150)
	public int sceKernelRegisterSubIntrHandler(int intrNumber, int subIntrNumber, int handlerAddress, int handlerArgument) {
		return Managers.intr.sceKernelRegisterSubIntrHandler(intrNumber, subIntrNumber, handlerAddress, handlerArgument);
	}

	@HLEFunction(nid = 0xD61E6961, version = 150)
	public int sceKernelReleaseSubIntrHandler(int intrNumber, int subIntrNumber) {
		return Managers.intr.sceKernelReleaseSubIntrHandler(intrNumber, subIntrNumber);
	}

	@HLEFunction(nid = 0xFB8E22EC, version = 150)
	public int sceKernelEnableSubIntr(int intrNumber, int subIntrNumber) {
		return Managers.intr.sceKernelEnableSubIntr(intrNumber, subIntrNumber);
	}

	@HLEFunction(nid = 0x8A389411, version = 150)
	public int sceKernelDisableSubIntr(int intrNumber, int subIntrNumber) {
		return Managers.intr.sceKernelDisableSubIntr(intrNumber, subIntrNumber);
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x5CB5A78B, version = 150)
	public int sceKernelSuspendSubIntr() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x7860E0DC, version = 150)
	public int sceKernelResumeSubIntr() {
		return 0;
	}
    
	@HLEUnimplemented
	@HLEFunction(nid = 0xFC4374B8, version = 150)
	public int sceKernelIsSubInterruptOccurred() {
		return 0;
	}
    
	@HLEUnimplemented
	@HLEFunction(nid = 0xD2E8363F, version = 150)
	public int QueryIntrHandlerInfo() {
		return 0;
	}
    
	@HLEUnimplemented
	@HLEFunction(nid = 0xEEE43F47, version = 150)
	public int sceKernelRegisterUserSpaceIntrStack() {
		return 0;
	}
}