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
package jpcsp.Allegrex;

import static jpcsp.Allegrex.Common.COP0_STATE_EPC;

/**
 * System Control Coprocessor 0
 *
 * @author gid15
 *
 */
public class Cp0State {
	private final int[] data = new int[32];
	private final int[] control = new int[32];

	public Cp0State() {
		final int dataCacheSize = 16 * 1024; // 16KB
		final int instructionCacheSize = 16 * 1024; // 16KB

		int config = 0;
		// 3 bits to indicate the data cache size
		config |= Math.min(Integer.numberOfTrailingZeros(dataCacheSize) - 12, 7) << 6;
		// 3 bits to indicate the instruction cache size
		config |= Math.min(Integer.numberOfTrailingZeros(instructionCacheSize) - 12, 7) << 9;
		setDataRegister(Common.COP0_STATE_CONFIG, config);
	}

	public int getDataRegister(int n) {
		return data[n];
	}

	public void setDataRegister(int n, int value) {
		data[n] = value;
	}

	public int getControlRegister(int n) {
		return control[n];
	}

	public void setControlRegister(int n, int value) {
		control[n] = value;
	}

	public int getEpc() {
		return getDataRegister(COP0_STATE_EPC);
	}

	public void setEpc(int epc) {
		setDataRegister(COP0_STATE_EPC, epc);
	}
}
