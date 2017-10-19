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

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.managers.IntrManager;

import org.apache.log4j.Logger;

public class InterruptManager extends HLEModule {
    public static Logger log = Modules.getLogger("InterruptManager");

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

	@HLEUnimplemented
	@HLEFunction(nid = 0xD774BA45, version = 150)
	public int sceKernelDisableIntr(int intrNumber) {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x4D6E7305, version = 150)
	public int sceKernelEnableIntr(int intrNumber) {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xDB14CBE0, version = 150)
	public int sceKernelResumeIntr() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x0C5F7AE3, version = 150)
	public int sceKernelCallSubIntrHandler() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x4023E1A7, version = 150)
	public int sceKernelDisableSubIntr() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x58DD8978, version = 150)
	public int sceKernelRegisterIntrHandler(int intrNumber, int unknown1, TPointer handler, TPointer unknown2, int unknown3) {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xF987B1F0, version = 150)
	public int sceKernelReleaseIntrHandler() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xFFA8B183, version = 660)
	public int sceKernelRegisterSubIntrHandler_660(int intrNumber, int subIntrNumber, int handlerAddress, int handlerArgument) {
		return sceKernelRegisterSubIntrHandler(intrNumber, subIntrNumber, handlerAddress, handlerArgument);
	}

	@HLEFunction(nid = 0xFE28C6D9, version = 150)
	public boolean sceKernelIsIntrContext() {
		return IntrManager.getInstance().isInsideInterrupt();
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xA0F88036, version = 150)
	public int sceKernelGetSyscallRA() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x14D4C61A, version = 660)
	public int sceKernelRegisterSystemCallTable_660(TPointer syscallTable) {
		return 0;
	}
}