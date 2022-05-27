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
package jpcsp.mediaengine;

import static jpcsp.util.Utilities.signExtend;

import java.io.IOException;

import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

public class MMIOHandlerMeDecoderQuSpectra extends MMIOHandlerMeBase {
	private static final int STATE_VERSION = 0;
	private int control;
	private int numSpecs;
	private int groupSize;
	private int numCoeffs;
	private int tabBits;
	private int vlcBits;
	private int unknown18;
	private int inputBuffer;
	private int bitIndex;
	private int vlcTableCode;
	private int vlsTableN;
	private int unknown2C;
	private int outputBuffer;
	private int outputBufferSize; // ???, always 0x2000
	private int cacheAddress;
	private int cacheValue8;

	public MMIOHandlerMeDecoderQuSpectra(int baseAddress) {
		super(baseAddress);
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		control = stream.readInt();
		numSpecs = stream.readInt();
		groupSize = stream.readInt();
		numCoeffs = stream.readInt();
		tabBits = stream.readInt();
		vlcBits = stream.readInt();
		unknown18 = stream.readInt();
		inputBuffer = stream.readInt();
		bitIndex = stream.readInt();
		vlcTableCode = stream.readInt();
		vlsTableN = stream.readInt();
		unknown2C = stream.readInt();
		outputBuffer = stream.readInt();
		outputBufferSize = stream.readInt();
		cacheAddress = stream.readInt();
		cacheValue8 = stream.readInt();
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(control);
		stream.writeInt(numSpecs);
		stream.writeInt(groupSize);
		stream.writeInt(numCoeffs);
		stream.writeInt(tabBits);
		stream.writeInt(vlcBits);
		stream.writeInt(unknown18);
		stream.writeInt(inputBuffer);
		stream.writeInt(bitIndex);
		stream.writeInt(vlcTableCode);
		stream.writeInt(vlsTableN);
		stream.writeInt(unknown2C);
		stream.writeInt(outputBuffer);
		stream.writeInt(outputBufferSize);
		stream.writeInt(cacheAddress);
		stream.writeInt(cacheValue8);
		super.write(stream);
	}

	private void setControl(int control) {
		// Flag 0x1 is "running"?
		// Flag 0x2 is "error"?
		// Flag 0x100/0x200 is signed/unsigned?
		this.control = control & ~0x3;

		if ((control & 0x1) != 0) {
			decodeQuSpectra();
		}
	}

	private boolean isSigned() {
		return (control & 0x300) == 0x100;
	}

	private boolean readBool() {
		return read1() != 0;
	}

	private int read1() {
		int address = inputBuffer + (bitIndex >> 3);
		if (address != cacheAddress) {
			cacheAddress = address;
			cacheValue8 = getMemory().read8(cacheAddress);
		}

		int bit = (cacheValue8 >> (7 - (bitIndex & 0x7))) & 0x1;

		bitIndex++;

		return bit;
	}

	private int read(int n) {
		int read = 0;
		for (; n > 0; n--) {
			read = (read << 1) + read1();
		}

		return read;
	}

	private int peek(int n) {
		int bitIndex = this.bitIndex;
		int value = read(n);
		this.bitIndex = bitIndex;

		return value;
	}

	private void skip(int n) {
		bitIndex += n;
	}

	private int getVLC2(int maxDepth) {
		int index = peek(vlcBits);
		int code = getMemory().read8(vlcTableCode + index);
		int n = (int) (byte) getMemory().read8(vlsTableN + code);

		if (maxDepth > 1 && n < 0) {
			skip(vlcBits);

			int nbBits = -n;

			index = peek(nbBits) + code;
			code = getMemory().read8(vlcTableCode + index);
			n = (int) (byte) getMemory().read8(vlsTableN + code);
			if (maxDepth > 2 && n < 0) {
				skip(nbBits);

				nbBits = -n;

				index = peek(nbBits) + code;
				code = getMemory().read8(vlcTableCode + index);
				n = (int) (byte) getMemory().read8(vlsTableN + code);
			}
		}
		skip(n);

		return code;
	}

	private int getVLC2() {
		return getVLC2(1);
	}

	// See jpcsp.media.codec.atrac3plus.ChannelUnit.decodeQuSpectra()
	private void decodeQuSpectra() {
		cacheAddress = -1;
		int mask = (1 << tabBits) - 1;
		boolean isSigned = isSigned();

		for (int pos = 0; pos < numSpecs; ) {
			if (groupSize == 1 || readBool()) {
				for (int j = 0; j < groupSize; j++) {
					int val = getVLC2();

					for (int i = 0; i < numCoeffs; i++) {
						int cf = val & mask;
						if (isSigned) {
							cf = signExtend(cf, tabBits);
						} else if (cf != 0 && readBool()) {
							cf = -cf;
						}

						getMemory().write32(outputBuffer + (pos << 2), cf);
						pos++;
						val >>= tabBits;
					}
				}
			} else {
				// Group skipped
				pos += groupSize * numCoeffs;
			}
		}
	}

	@Override
	public int read32(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x00: value = control; break;
			case 0x20: value = bitIndex; break;
			case 0x30: value = outputBuffer; break;
			default: value = super.read32(address); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - read32(0x%08X) returning 0x%08X", getPc() - 4, address, value));
		}

		return value;
	}

	@Override
	public void write32(int address, int value) {
		switch (address - baseAddress) {
			case 0x00: setControl(value); break;
			case 0x04: numSpecs = value; break;
			case 0x08: groupSize = value; break;
			case 0x0C: numCoeffs = value; break;
			case 0x10: tabBits = value; break;
			case 0x14: vlcBits = value; break;
			case 0x18: unknown18 = value; break;
			case 0x1C: inputBuffer = value; break;
			case 0x20: bitIndex = value; break;
			case 0x24: vlcTableCode = value; break;
			case 0x28: vlsTableN = value; break;
			case 0x2C: unknown2C = value; break;
			case 0x30: outputBuffer = value; break;
			case 0x3C: outputBufferSize = value; break;
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc() - 4, address, value, this));
		}
	}
}
