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

package jpcsp.HLE.modules200;

import jpcsp.HLE.HLEFunction;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;

public class SysMemUserForUser extends jpcsp.HLE.modules150.SysMemUserForUser {
    protected int compiledSdkVersion;
    protected int compilerVersion;

	@Override
	public void start() {
		compiledSdkVersion = 0;
		super.start();
	}

	public int hleGetCompiledSdkVersion() {
		return compiledSdkVersion;
	}

	@HLEFunction(nid = 0xFC114573, version = 200)
	public void sceKernelGetCompiledSdkVersion(Processor processor) {
		CpuState cpu = processor.cpu;

		if(log.isDebugEnabled()) {
            log.debug("sceKernelGetCompiledSdkVersion");
        }

		cpu.gpr[2] = compiledSdkVersion;
	}

	@HLEFunction(nid = 0x7591C7DB, version = 200)
	public void sceKernelSetCompiledSdkVersion(Processor processor) {
		CpuState cpu = processor.cpu;

        int sdkVersion = cpu.gpr[4];

        if(log.isDebugEnabled()) {
            log.debug("sceKernelSetCompiledSdkVersion: sdkVersion=" + Integer.toHexString(sdkVersion));
        }

        compiledSdkVersion = sdkVersion;
		cpu.gpr[2] = 0;
	}

	@HLEFunction(nid = 0xF77D77CB, version = 200)
	public void sceKernelSetCompilerVersion(Processor processor) {
		CpuState cpu = processor.cpu;

		int compVersion = cpu.gpr[4];

        if(log.isDebugEnabled()) {
            log.debug("sceKernelSetCompilerVersion: compVersion=" + Integer.toHexString(compVersion));
        }

        compilerVersion = compVersion;
		cpu.gpr[2] = 0;
	}

	@HLEFunction(nid = 0xA6848DF8, version = 200)
	public void SysMemUserForUser_A6848DF8(Processor processor) {
		CpuState cpu = processor.cpu;

		log.debug("Unimplemented NID function SysMemUserForUser_A6848DF8 [0xA6848DF8]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

}