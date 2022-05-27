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

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.HLE.kernel.types.IAction;

/**
 * @author gid15
 *
 */
public class HLEPointerFunction extends TPointerFunction{
	public static Logger log = Emulator.log;

	public HLEPointerFunction() {
		super();
	}

	public int executeCallback(IAction afterAction, int arg0) {
		log.error(String.format("Unimplemented HLEPointerFunction.executeCallback afterAction=%s, arg0=0x%08X", afterAction, arg0));
		return 0;
	}

	public int executeCallback(IAction afterAction, int arg0, int arg1) {
		log.error(String.format("Unimplemented HLEPointerFunction.executeCallback afterAction=%s, arg0=0x%08X, arg1=0x%08X", afterAction, arg0, arg1));
		return 0;
	}

	public int executeCallback(IAction afterAction, int arg0, int arg1, int arg2) {
		log.error(String.format("Unimplemented HLEPointerFunction.executeCallback afterAction=%s, arg0=0x%08X, arg1=0x%08X, arg2=0x%08X", afterAction, arg0, arg1, arg2));
		return 0;
	}

	public int executeCallback(IAction afterAction, int arg0, int arg1, int arg2, int arg3, int arg4, int arg5) {
		log.error(String.format("Unimplemented HLEPointerFunction.executeCallback afterAction=%s, arg0=0x%08X, arg1=0x%08X, arg2=0x%08X, arg3=0x%08X, arg4=0x%08X, arg5=0x%08X", afterAction, arg0, arg1, arg2, arg3, arg4, arg5));
		return 0;
	}

	public int executeCallback(IAction afterAction, int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6) {
		log.error(String.format("Unimplemented HLEPointerFunction.executeCallback afterAction=%s, arg0=0x%08X, arg1=0x%08X, arg2=0x%08X, arg3=0x%08X, arg4=0x%08X, arg5=0x%08X, arg6=0x%08X", afterAction, arg0, arg1, arg2, arg3, arg4, arg5, arg6));
		return 0;
	}
}
