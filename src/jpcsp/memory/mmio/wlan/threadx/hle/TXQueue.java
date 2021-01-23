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
package jpcsp.memory.mmio.wlan.threadx.hle;

/**
 * @author gid15
 *
 */
public class TXQueue {
	public static final int SIZEOF = 56;
	public int queuePtr;
	public String queueName;
	public int messageSize;
	public int capacity;
	public int enqueued;
	public int availableStorage;
	public int queueStart;
	public int queueEnd;
	public int queueRead;
	public int queueWrite;

	public void init() {
		enqueued = 0;
		availableStorage = capacity;
		queueEnd = queueStart + ((capacity * messageSize) << 2);
		queueRead = queueStart;
		queueWrite = queueStart;
	}

	@Override
	public String toString() {
		return String.format("TXQueue queuePtr=0x%08X, queueName='%s', messageSize=0x%X, queueStart=0x%08X, queueSize=0x%X", queuePtr, queueName, messageSize, queueStart, capacity);
	}
}
