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

import static jpcsp.Allegrex.Common.COP0_CONTROL_SYSCALL_TABLE;
import static jpcsp.Allegrex.Common.COP0_STATE_CAUSE;
import static jpcsp.Allegrex.Common.COP0_STATE_CONFIG;
import static jpcsp.Allegrex.Common.COP0_STATE_CPUID;
import static jpcsp.Allegrex.Common.COP0_STATE_EBASE;
import static jpcsp.Allegrex.Common.COP0_STATE_EPC;
import static jpcsp.Allegrex.Common.COP0_STATE_ERROR_EPC;
import static jpcsp.Allegrex.Common.COP0_STATE_SCCODE;
import static jpcsp.Allegrex.Common.COP0_STATE_STATUS;

import java.io.IOException;
import java.util.Arrays;

import jpcsp.mediaengine.MEProcessor;
import jpcsp.state.IState;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

/**
 * System Control Coprocessor 0
 *
 * @author gid15
 *
 */
public class Cp0State implements IState {
	private static final int STATE_VERSION = 0;
	public static final int STATUS_IE  = 0x00000001;
	public static final int STATUS_EXL = 0x00000002;
	public static final int STATUS_ERL = 0x00000004;
	public static final int STATUS_BEV = 0x00400000;
	private final int[] data = new int[32];
	private final int[] control = new int[32];

	public Cp0State() {
		reset();
	}

	@Override
    public void read(StateInputStream stream) throws IOException {
    	stream.readVersion(STATE_VERSION);
    	stream.readInts(data);
    	stream.readInts(control);
    }

	@Override
    public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
    	stream.writeInts(data);
    	stream.writeInts(control);
    }

	public void reset() {
		Arrays.fill(data, 0);
		Arrays.fill(control, 0);

		final int dataCacheSize = 16 * 1024; // 16KB
		final int instructionCacheSize = 16 * 1024; // 16KB

		int config = 0;
		// 3 bits to indicate the data cache size
		config |= Math.min(Integer.numberOfTrailingZeros(dataCacheSize) - 12, 7) << 6;
		// 3 bits to indicate the instruction cache size
		config |= Math.min(Integer.numberOfTrailingZeros(instructionCacheSize) - 12, 7) << 9;
		setConfig(config);

		setStatus(STATUS_IE);
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

	public int getErrorEpc() {
		return getDataRegister(COP0_STATE_ERROR_EPC);
	}

	public void setErrorEpc(int errorEpc) {
		setDataRegister(COP0_STATE_ERROR_EPC, errorEpc);
	}

	public int getStatus() {
		return getDataRegister(COP0_STATE_STATUS);
	}

	public void setStatus(int status) {
		setDataRegister(COP0_STATE_STATUS, status);
	}

	public int getCause() {
		return getDataRegister(COP0_STATE_CAUSE);
	}

	public void setCause(int cause) {
		setDataRegister(COP0_STATE_CAUSE, cause);
	}

	public int getEbase() {
		return getDataRegister(COP0_STATE_EBASE);
	}

	public void setEbase(int ebase) {
		setDataRegister(COP0_STATE_EBASE, ebase);
	}

	public void setSyscallCode(int syscallCode) {
		setDataRegister(COP0_STATE_SCCODE, syscallCode);
	}

	public int getSyscallCode() {
		return getDataRegister(COP0_STATE_SCCODE);
	}

	public void setConfig(int config) {
		setDataRegister(COP0_STATE_CONFIG, config);
	}

	public void setCpuid(int cpuid) {
		setDataRegister(COP0_STATE_CPUID, cpuid);
	}

	public int getCpuid() {
		return getDataRegister(COP0_STATE_CPUID);
	}

	public int getSyscallTable() {
		return getControlRegister(COP0_CONTROL_SYSCALL_TABLE);
	}

	public boolean isMediaEngineCpu() {
		return getCpuid() == MEProcessor.CPUID_ME;
	}

	public boolean isMainCpu() {
		return !isMediaEngineCpu();
	}
}
