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

public interface IWaitStateChecker {
	/**
	 * Checks if a thread has to return to its wait state after the execution
	 * of a callback.
	 * 
	 * @param thread the thread
	 * @param wait   the wait state that has to be checked
	 * @return       true if the thread has to return to the wait state
	 *               false if the thread has not to return to the wait state but
	 *               continue in the READY state. In that case, the return values have
	 *               to be set in the CpuState of the thread (at least $v0).
	 */
	public boolean continueWaitState(SceKernelThreadInfo thread, ThreadWaitInfo wait);
}
