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

import org.apache.log4j.Logger;

import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;

@HLELogging
public class sceFpu extends HLEModule {
    public static Logger log = Modules.getLogger("sceFpu");

	@Override
	public String getName() {
		return "sceFpu";
	}

	private int getCfc1_31(CpuState cpu) {
		return (cpu.fcr31.fs ? (1 << 24) : 0) | (cpu.fcr31.c ? (1 << 23) : 0) | (cpu.fcr31.rm & 3);
	}

	private void setCtc1_31(CpuState cpu, int ctc1Bits) {
		cpu.fcr31.rm = ctc1Bits & 3;
		cpu.fcr31.fs = ((ctc1Bits >> 24) & 1) != 0;
		cpu.fcr31.c = ((ctc1Bits >> 23) & 1) != 0;
	}

	@HLEFunction(nid = 0x3AF6984A)
	public int sceFpu_3AF6984A(float value) {
		return Math.round(value);
	}

	@HLEFunction(nid = 0x6CF7A73F)
	public int sceFpu_6CF7A73F(CpuState cpu) {
		return getCfc1_31(cpu);
	}

	@HLEFunction(nid = 0xB9EEFCEA)
	public int sceFpu_B9EEFCEA(CpuState cpu, int ctc1Bits) {
		int result = getCfc1_31(cpu);
		setCtc1_31(cpu, ctc1Bits);

		return result;
	}
}
