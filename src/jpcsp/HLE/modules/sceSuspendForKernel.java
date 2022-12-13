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

import static jpcsp.HLE.Modules.ThreadManForUserModule;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointerFunction;

public class sceSuspendForKernel extends HLEModule {
    public static Logger log = Modules.getLogger("sceSuspendForKernel");
    private static final int NUMBER_HANDLERS = 32;
    private final Handler suspendHandlers[] = new Handler[NUMBER_HANDLERS];
    private final Handler resumeHandlers[] = new Handler[NUMBER_HANDLERS];

    private static class Handler {
    	private TPointerFunction handler;
    	private int param;
    	private int gp;

    	public Handler(TPointerFunction handler, int param) {
			this.handler = handler;
			this.param = param;
			gp = Emulator.getProcessor().cpu._gp;
		}

    	public void call(int unknown) {
    		if (handler == null || handler.isNull()) {
    			return;
    		}

    		ThreadManForUserModule.executeCallback(handler.getAddress(), gp, null, unknown, param);
    	}
    }

	@Override
	public void start() {
		for (int i = 0; i < NUMBER_HANDLERS; i++) {
			suspendHandlers[i] = null;
			resumeHandlers[i] = null;
		}

		super.start();
	}

	@HLEUnimplemented
    @HLEFunction(nid = 0x98A1D061, version = 150)
    public int sceKernelPowerRebootStart(int unknown) {
    	return 0;
    }

    @HLEFunction(nid = 0x91A77137, version = 150)
    public int sceKernelRegisterSuspendHandler(int reg, TPointerFunction handler, int param) {
    	if (reg < 0 || reg >= NUMBER_HANDLERS) {
    		return -1;
    	}

    	suspendHandlers[reg] = new Handler(handler, param);

    	return 0;
    }

    @HLEFunction(nid = 0xB43D1A8C, version = 150)
    public int sceKernelRegisterResumeHandler(int reg, TPointerFunction handler, int param) {
    	if (reg < 0 || reg >= NUMBER_HANDLERS) {
    		return -1;
    	}

    	resumeHandlers[reg] = new Handler(handler, param);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB53B2147, version = 150)
    public int sceKernelPowerLockForUser() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC7C928C7, version = 150)
    public int sceKernelPowerUnlockForUser() {
    	return 0;
    }

    @HLEFunction(nid = 0x8F58B1EC, version = 150)
    public int sceKernelDispatchSuspendHandlers(int unknown) {
    	for (int i = 0; i < NUMBER_HANDLERS; i++) {
    		if (suspendHandlers[i] != null) {
    			suspendHandlers[i].call(unknown);
    		}
    	}

    	return 0;
    }

    @HLEFunction(nid = 0x0AB0C6F3, version = 150)
    public int sceKernelDispatchResumeHandlers(int unknown) {
    	for (int i = 0; i < NUMBER_HANDLERS; i++) {
    		if (resumeHandlers[i] != null) {
    			resumeHandlers[i].call(unknown);
    		}
    	}

    	return 0;
    }
}
