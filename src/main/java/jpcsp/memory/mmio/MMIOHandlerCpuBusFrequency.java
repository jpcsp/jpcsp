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
package jpcsp.memory.mmio;

import java.io.IOException;

import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

public class MMIOHandlerCpuBusFrequency extends MMIOHandlerBase {
	private static final int STATE_VERSION = 0;
	private int cpuFrequencyNumerator;
	private int cpuFrequencyDenominator;
	private int busFrequencyNumerator;
	private int busFrequencyDenominator;

	public MMIOHandlerCpuBusFrequency(int baseAddress) {
		super(baseAddress);
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		cpuFrequencyNumerator = stream.readInt();
		cpuFrequencyDenominator = stream.readInt();
		busFrequencyNumerator = stream.readInt();
		busFrequencyDenominator = stream.readInt();
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(cpuFrequencyNumerator);
		stream.writeInt(cpuFrequencyDenominator);
		stream.writeInt(busFrequencyNumerator);
		stream.writeInt(busFrequencyDenominator);
		super.write(stream);
	}

	@Override
	public void reset() {
		super.reset();

		cpuFrequencyNumerator = 0;
		cpuFrequencyDenominator = 0;
		busFrequencyNumerator = 0;
		busFrequencyDenominator = 0;
	}

	private int getFrequency(int numerator, int denominator) {
		return (numerator << 16) | denominator;
	}

	private int getNumerator(int value) {
		return (value >> 16) & 0x1FF;
	}

	private int getDenominator(int value) {
		return value & 0x1FF;
	}

	private int getCpuFrequency() {
		return getFrequency(cpuFrequencyNumerator, cpuFrequencyDenominator);
	}

	private int getBusFrequency() {
		return getFrequency(busFrequencyNumerator, busFrequencyDenominator);
	}

	private void setCpuFrequency(int value) {
		cpuFrequencyNumerator = getNumerator(value);
		cpuFrequencyDenominator = getDenominator(value);
	}

	private void setBusFrequency(int value) {
		busFrequencyNumerator = getNumerator(value);
		busFrequencyDenominator = getDenominator(value);
	}

	@Override
	public int read32(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x00: value = getCpuFrequency(); break;
			case 0x04: value = getBusFrequency(); break;
			default: value = super.read32(address); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - read32(0x%08X) returning 0x%08X", getPc(), address, value));
		}

		return value;
	}

	@Override
	public void write32(int address, int value) {
		switch (address - baseAddress) {
			case 0x00: setCpuFrequency(value); break;
			case 0x04: setBusFrequency(value); break;
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}

	@Override
	public String toString() {
		return String.format("CPU frequency=%d/%d, Bus frequency=%d/%d", cpuFrequencyNumerator, cpuFrequencyDenominator, busFrequencyNumerator, busFrequencyDenominator);
	}
}
