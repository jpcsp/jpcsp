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
package jpcsp.Allegrex.compiler.nativeCode;

import jpcsp.Memory;
import jpcsp.Allegrex.CpuState;

/**
 * @author gid15
 *
 */
public class IntBitsToFloatSequence extends AbstractNativeCodeSequence {
	// lv.s       S020.s, 0($t1)
	// lv.q       C100.q, -27648($a3)
	// lv.q       C030.q, 31536($a2)
	// VC2I.s     S020.s, S020.s
	// vi2f.q     C020.q, C020.q, 24
	static public void callSequence1() {
		CpuState cpu = getCpu();
		Memory mem = getMemory();

		int t1 = getRegisterValue(9);
		cpu.setVpr(0, 2, 0, mem.read8(t1    ));
		cpu.setVpr(0, 2, 1, mem.read8(t1 + 1));
		cpu.setVpr(0, 2, 2, mem.read8(t1 + 2));
		cpu.setVpr(0, 2, 3, mem.read8(t1 + 3));

		cpu.doLVQ(4, 7, -27648);
		cpu.doLVQ(3, 6, 31536);
	}

	// MTV.s      $v0, S000.s
	// MTV.s      $v1, S001.s
	// MTV.s      $a0, S002.s
	// MTV.s      $a1, S003.s
	// lw         $v0, 92($sp)
	// sv.q       C000.q, 0($v0)
	static public void callSequence2() {
		Memory mem = getMemory();

		int v0 = mem.read32(getRegisterValue(29) + 92);

		mem.write32(v0     , getRegisterValue(2));
		mem.write32(v0 +  4, getRegisterValue(3));
		mem.write32(v0 +  8, getGprA0());
		mem.write32(v0 + 12, getGprA1());
		setRegisterValue(2, v0);
	}
}
