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
package jpcsp.HLE;

import static jpcsp.Emulator.getProcessor;
import static jpcsp.HLE.Modules.ThreadManForUserModule;

import jpcsp.Memory;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;

/**
 * @author gid15
 *
 */
public class TPointerFunction {
	public static final TPointerFunction NULL = new TPointerFunction();
	private TPointer32 pointer;

    private static class CallbackReturnCode implements IAction {
    	private int returnCode;
    	private IAction nextAction;

    	public CallbackReturnCode(IAction nextAction) {
    		this.nextAction = nextAction;
    	}

    	@Override
		public void execute() {
			returnCode = getProcessor().cpu._v0;

			if (nextAction != null) {
				nextAction.execute();
			}
		}

		public int getReturnCode() {
			return returnCode;
		}
    }

	public TPointerFunction() {
		pointer = TPointer32.NULL;
	}

	public TPointerFunction(Memory mem, int address) {
		if (address == 0) {
			pointer = TPointer32.NULL;
		} else {
			pointer = new TPointer32(mem, address);
		}
	}

	public TPointer getPointer() {
		return pointer.getPointer();
	}

	public boolean isNull() {
		return pointer.isNull();
	}

	public boolean isNotNull() {
		return pointer.isNotNull();
	}

	public int getAddress() {
		if (pointer == null) {
			return 0;
		}
		return pointer.getAddress();
	}

	public int executeCallback(int arg0) {
		return executeCallback(null, arg0);
	}

	public int executeCallback(IAction afterAction, int arg0) {
		CallbackReturnCode callbackReturnCode = new CallbackReturnCode(afterAction);
		ThreadManForUserModule.executeCallback(ThreadManForUserModule.getCurrentThread(), pointer.getAddress(), callbackReturnCode, false, true, arg0);
		return callbackReturnCode.getReturnCode();
	}

	public int executeCallback(int arg0, int arg1) {
		return executeCallback(null, arg0, arg1);
	}

	public int executeCallback(IAction afterAction, int arg0, int arg1) {
		CallbackReturnCode callbackReturnCode = new CallbackReturnCode(afterAction);
		ThreadManForUserModule.executeCallback(ThreadManForUserModule.getCurrentThread(), pointer.getAddress(), callbackReturnCode, false, true, arg0, arg1);
		return callbackReturnCode.getReturnCode();
	}

	public int executeCallback(int arg0, int arg1, int arg2) {
		return executeCallback(ThreadManForUserModule.getCurrentThread(), null, arg0, arg1, arg2);
	}

	public int executeCallback(SceKernelThreadInfo thread, int arg0, int arg1, int arg2) {
		return executeCallback(thread, null, arg0, arg1, arg2);
	}

	public int executeCallback(IAction afterAction, int arg0, int arg1, int arg2) {
		return executeCallback(ThreadManForUserModule.getCurrentThread(), afterAction, arg0, arg1, arg2);
	}

	public int executeCallback(SceKernelThreadInfo thread, IAction afterAction, int arg0, int arg1, int arg2) {
		CallbackReturnCode callbackReturnCode = new CallbackReturnCode(afterAction);
		ThreadManForUserModule.executeCallback(thread, pointer.getAddress(), callbackReturnCode, false, true, arg0, arg1, arg2);
		return callbackReturnCode.getReturnCode();
	}

	public int executeCallback(int arg0, int arg1, int arg2, int arg3, int arg4, int arg5) {
		return executeCallback(null, arg0, arg1, arg2, arg3, arg4, arg5);
	}

	public int executeCallback(IAction afterAction, int arg0, int arg1, int arg2, int arg3, int arg4, int arg5) {
		CallbackReturnCode callbackReturnCode = new CallbackReturnCode(afterAction);
		ThreadManForUserModule.executeCallback(ThreadManForUserModule.getCurrentThread(), pointer.getAddress(), callbackReturnCode, false, true, arg0, arg1, arg2, arg3, arg4, arg5);
		return callbackReturnCode.getReturnCode();
	}

	public int executeCallback(int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6) {
		return executeCallback(null, arg0, arg1, arg2, arg3, arg4, arg5, arg6);
	}

	public int executeCallback(IAction afterAction, int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6) {
		CallbackReturnCode callbackReturnCode = new CallbackReturnCode(afterAction);
		ThreadManForUserModule.executeCallback(ThreadManForUserModule.getCurrentThread(), pointer.getAddress(), callbackReturnCode, false, true, arg0, arg1, arg2, arg3, arg4, arg5, arg6);
		return callbackReturnCode.getReturnCode();
	}

	@Override
	public String toString() {
		return pointer.toString();
	}
}
