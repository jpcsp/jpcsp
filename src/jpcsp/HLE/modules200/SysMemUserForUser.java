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
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;

@HLELogging
public class SysMemUserForUser extends jpcsp.HLE.modules150.SysMemUserForUser {
    private int compiledSdkVersion;
    protected int compilerVersion;

	@Override
	public void start() {
		compiledSdkVersion = 0;
		super.start();
	}

	public int hleKernelGetCompiledSdkVersion() {
		return compiledSdkVersion;
	}

	protected void hleSetCompiledSdkVersion(int sdkVersion) {
		compiledSdkVersion = sdkVersion;
	}

	@HLEFunction(nid = 0xFC114573, version = 200)
	public int sceKernelGetCompiledSdkVersion() {
		return compiledSdkVersion;
	}

	@HLEFunction(nid = 0x7591C7DB, version = 200)
	public int sceKernelSetCompiledSdkVersion(int sdkVersion) {
        hleSetCompiledSdkVersion(sdkVersion);

        return 0;
	}

	@HLEFunction(nid = 0xF77D77CB, version = 200)
	public int sceKernelSetCompilerVersion(int compilerVersion) {
        this.compilerVersion = compilerVersion;

        return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xA6848DF8, version = 200)
	public int SysMemUserForUser_A6848DF8() {
		return 0;
	}
}