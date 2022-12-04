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
package jpcsp.nec78k0.sfr;

import static jpcsp.nec78k0.sfr.Nec78k0Sfr.SRIF6;

import java.io.IOException;

import org.apache.log4j.Logger;

import jpcsp.HLE.kernel.types.IAction;
import jpcsp.state.IState;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;
import jpcsp.util.RingBuffer;

/**
 * Base implementation for the sub-system connected to a NEC 78k0 processor serial interface.
 *
 * @author gid15
 *
 */
public class Nec78k0SerialInterface implements IState {
	private static final int STATE_VERSION = 0;
	protected final Nec78k0Sfr sfr;
	protected final Nec78k0SerialInterfaceUART6 serialInterface;
	protected Logger log;
	protected final RingBuffer receptionBuffer = new RingBuffer(21);
	protected final RingBuffer transmissionBuffer = new RingBuffer(21);
	private boolean hasReadReceived;

	private class OnReceiveInterruptAction implements IAction {
		@Override
		public void execute() {
			if (log.isTraceEnabled()) {
				log.trace(String.format("UART6 OnReceiveInterruptAction %s, hasReadReceived=%b", receptionBuffer, hasReadReceived));
			}

			if (!receptionBuffer.isEmpty()) {
				if (hasReadReceived) {
					receptionBuffer.read();
					hasReadReceived = false;
				}

				if (hasReceived()) {
					sfr.setOnInterruptAction(SRIF6, this);
				}
			}
		}
	}

	public Nec78k0SerialInterface(Nec78k0Sfr sfr, Nec78k0SerialInterfaceUART6 serialInterface) {
		this.sfr = sfr;
		this.serialInterface = serialInterface;

		if (sfr != null) {
			log = sfr.log;

			sfr.setOnInterruptAction(SRIF6, new OnReceiveInterruptAction());
		}
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		receptionBuffer.read(stream);
		transmissionBuffer.read(stream);
		hasReadReceived = stream.readBoolean();
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		receptionBuffer.write(stream);
		transmissionBuffer.write(stream);
		stream.writeBoolean(hasReadReceived);
	}

	public void setLogger(Logger log) {
		this.log = log;
	}

	public synchronized void reset() {
		receptionBuffer.clear();
		transmissionBuffer.clear();
		hasReadReceived = false;
		if (log.isDebugEnabled()) {
			log.debug(String.format("UART6 reset"));
		}
	}

	public static int computeChecksum(int[] buffer, int offset, int size) {
		int checksum = 0;
		for (int i = 0; i < size; i++) {
			checksum += buffer[offset + i];
		}

		return (checksum & 0xFF) ^ 0xFF;
	}

	public static int computeChecksum(RingBuffer buffer, int size) {
		int checksum = 0;
		for (int i = 0; i < size; i++) {
			checksum += buffer.peek(i);
		}

		return (checksum & 0xFF) ^ 0xFF;
	}

	public static int computeChecksum(RingBuffer buffer) {
		return computeChecksum(buffer, buffer.size());
	}

	protected static boolean isValidChecksum(RingBuffer buffer, int checksum, int size) {
		return computeChecksum(buffer, size) == checksum;
	}

	public synchronized void startReceptionBuffer(int dataLength) {
		startReceptionBuffer(dataLength, 0x06);
	}

	public synchronized void startReceptionBuffer(int dataLength, int errorCode) {
		receptionBuffer.clear();
		// Start with fixed byte 0xA5
		receptionBuffer.write(0xA5);
		// Followed by the data length
		receptionBuffer.write(dataLength + 2);
		// Followed by error code byte
		receptionBuffer.write(errorCode);
	}

	public synchronized void endReceptionBuffer() {
		// Add checksum as the last received byte
		int checksum = computeChecksum(receptionBuffer);
		receptionBuffer.write(checksum);

		if (log.isDebugEnabled()) {
			log.debug(String.format("UART6 prepared reception buffer: %s", receptionBuffer.toString(8)));
		}

		if (serialInterface != null && serialInterface.isReceptionEnabled()) {
			sfr.setInterruptRequest(SRIF6);
		}
	}

	public synchronized void received(int data8) {
		addReceptionBufferData8(data8);

		if (serialInterface != null && serialInterface.isReceptionEnabled()) {
			sfr.setInterruptRequest(SRIF6);
		}
	}

	public synchronized void addReceptionBufferData8(int data8) {
		receptionBuffer.write(data8 & 0xFF);
	}

	public synchronized void addReceptionBufferData16(int data16) {
		addReceptionBufferData8(data16);
		addReceptionBufferData8(data16 >> 8);
	}

	public synchronized void addReceptionBufferData32(int data32) {
		addReceptionBufferData8(data32);
		addReceptionBufferData8(data32 >> 8);
		addReceptionBufferData8(data32 >> 16);
		addReceptionBufferData8(data32 >> 24);
	}

	public synchronized void addReceptionBufferData8(int[] data, int offset, int length) {
		if (data != null) {
			for (int i = 0; i < length; i++) {
				addReceptionBufferData8(data[offset + i]);
			}
		}
	}

	public synchronized void startTransmission() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("UART6 startTransmission"));
		}
	}

	public synchronized void endTransmission() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("UART6 endTransmission"));
		}
	}

	public synchronized void transmit(int value) {
		transmissionBuffer.write(value);
	}

	public synchronized void transmitChecksum() {
		transmit(computeChecksum(transmissionBuffer));
	}

	public synchronized void startReception() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("UART6 startReception"));
		}

		if (hasReceived()) {
			sfr.setInterruptRequest(SRIF6);
		}
	}

	public synchronized void endReception() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("UART6 endReception"));
		}

		receptionBuffer.clear();
		hasReadReceived = false;
	}

	public synchronized boolean isReceptionBufferEmpty() {
		return receptionBuffer.isEmpty();
	}

	public synchronized boolean hasReceived() {
		return receptionBuffer.size() > 1;
	}

	public synchronized int receive() {
		int value = 0x00;

		if (!receptionBuffer.isEmpty()) {
			value = receptionBuffer.peek();
			hasReadReceived = true;
		} else {
			log.error(String.format("UART6 receive reception buffer empty"));
		}

		return value;
	}
}
