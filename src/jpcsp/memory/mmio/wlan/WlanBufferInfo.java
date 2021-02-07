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
package jpcsp.memory.mmio.wlan;

import java.io.IOException;

import jpcsp.state.IState;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

/**
 * @author gid15
 *
 */
public class WlanBufferInfo implements IState {
	private static final int STATE_VERSION = 0;
	public int addr;
	public int length;
	public int readIndex;
	public int writeIndex;

	public void incrementWriteIndex() {
		writeIndex += 4;
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		addr = stream.readInt();
		length = stream.readInt();
		readIndex = stream.readInt();
		writeIndex = stream.readInt();
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(addr);
		stream.writeInt(length);
		stream.writeInt(readIndex);
		stream.writeInt(writeIndex);
	}

	@Override
	public String toString() {
		return String.format("WlanBufferInfo addr=0x%08X, length=0x%X, readIndex=0x%X, writeIndex=0x%X", addr, length, readIndex, writeIndex);
	}
}
