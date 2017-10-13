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
package jpcsp.HLE.kernel.managers;

public class ExceptionManager {
	public static final int EXCEP_INT = 0; // Interrupt
	public static final int EXCEP_ADEL = 4; // Load of instruction fetch exception
	public static final int EXCEP_ADES = 5; // Address store exception
	public static final int EXCEP_IBE = 6; // Instruction fetch bus error
	public static final int EXCEP_DBE = 7; // Load/store bus error
	public static final int EXCEP_SYS = 8; // Syscall
	public static final int EXCEP_BP = 9; // Breakpoint
	public static final int EXCEP_RI = 10; // Reserved instruction
	public static final int EXCEP_CPU = 11; // Coprocessor unusable
	public static final int EXCEP_OV = 12; // Arithmetic overflow
	public static final int EXCEP_FPE = 15; // Floating-point exception
	public static final int EXCEP_WATCH = 23; // Watch (reference to WatchHi/WatchLo)
	public static final int EXCEP_VCED = 31; // "Virtual Coherency Exception Data" (used for NMI handling apparently)
}
