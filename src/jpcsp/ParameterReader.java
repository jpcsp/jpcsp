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
package jpcsp;

import static jpcsp.Allegrex.Common._a0;
import static jpcsp.Allegrex.Common._f0;
import static jpcsp.Allegrex.Common._f12;

import jpcsp.Allegrex.CpuState;

public class ParameterReader {
	private CpuState cpu;
	final private Memory memory;
	private int parameterIndex = 0;
	private int parameterIndexFloat = 0;
	protected static final int maxParameterInGprRegisters = 8;
	protected static final int maxParameterInFprRegisters = 8;
	protected static final int firstParameterInGpr = _a0;
	protected static final int firstParameterInFpr = _f12;

	public ParameterReader(CpuState cpu, Memory memory) {
		this.cpu = cpu;
		this.memory = memory;
	}

    public void setCpu(CpuState cpu) {
    	this.cpu = cpu;
    }

	public void resetReading() {
		parameterIndex = 0;
		parameterIndexFloat = 0;
	}

	private int getParameterIntAt(int index) {
		if (index >= maxParameterInGprRegisters) {
			return memory.read32(cpu._sp + (index - maxParameterInGprRegisters) * 4);
		}
		return cpu.getRegister(firstParameterInGpr + index);
	}

	private float getParameterFloatAt(int index) {
		if (index >= maxParameterInFprRegisters) {
			throw(new UnsupportedOperationException());
		}
		return cpu.fpr[firstParameterInFpr + index];
	}

	private long getParameterLongAt(int index) {
		if ((index % 2) != 0) {
			throw(new RuntimeException("Parameter misalignment"));
		}
		return (long)getParameterIntAt(index) + (long)getParameterIntAt(index + 1) << 32;
	}

	protected int moveParameterIndex(int size) {
		while ((parameterIndex % size) != 0) {
			parameterIndex++;
		}
		int retParameterIndex = parameterIndex;
		parameterIndex += size;
		return retParameterIndex;
	}

	protected int moveParameterIndexFloat(int size) {
		while ((parameterIndexFloat % size) != 0) {
			parameterIndexFloat++;
		}
		int retParameterIndexFloat = parameterIndexFloat;
		parameterIndexFloat += size;
		return retParameterIndexFloat;
	}

	public int getNextInt() {
		return getParameterIntAt(moveParameterIndex(1));
	}

	public long getNextLong() {
		return getParameterLongAt(moveParameterIndex(2));
	}

	public float getNextFloat() {
		return getParameterFloatAt(moveParameterIndexFloat(1));
	}

	public void setReturnValueInt(int value) {
		cpu._v0 = value;
	}

	public void setReturnValueFloat(float value) {
		// Float value is returned in $f0 register
		cpu.fpr[_f0] = value;
	}

	public void setReturnValueLong(long value) {
		cpu._v0 = (int)(                0xFFFFFFFF);
		cpu._v1 = (int)((value >> 32) & 0xFFFFFFFF);
	}
}
