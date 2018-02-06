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
package jpcsp.memory.mmio.dmac;

import jpcsp.Memory;
import jpcsp.HLE.kernel.types.IAction;

public class DmacProcessor {
	private static final int STATUS_IN_PROGRESS = 0x1;
	private Memory memSrc;
	private Memory memDst;
	private IAction interruptAction;
	private IAction completedAction;
	private DmacThread dmacThread;
	private int dst;
	private int src;
	private int next;
	private int attributes;
	private int status;

	private class CompletedAction implements IAction {
		public CompletedAction() {
		}

		@Override
		public void execute() {
			// Clear the flag STATUS_IN_PROGRESS
			status &= ~STATUS_IN_PROGRESS;
		}
	}

	public DmacProcessor(Memory memSrc, Memory memDst, int baseAddress, IAction interruptAction) {
		this.memSrc = memSrc;
		this.memDst = memDst;
		this.interruptAction = interruptAction;
		this.completedAction = new CompletedAction();

		dmacThread = new DmacThread(this);
		dmacThread.setName(String.format("Dmac Thread for 0x%08X", baseAddress));
		dmacThread.setDaemon(true);
		dmacThread.start();
	}

	public int getDst() {
		return dst;
	}

	public void setDst(int dst) {
		this.dst = dst;
	}

	public int getSrc() {
		return src;
	}

	public void setSrc(int src) {
		this.src = src;
	}

	public int getNext() {
		return next;
	}

	public void setNext(int next) {
		this.next = next;
	}

	public int getAttributes() {
		return attributes;
	}

	public void setAttributes(int attributes) {
		this.attributes = attributes;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		int previousStatus = this.status;
		this.status = status;

		// Status "in progress" changed?
		if ((previousStatus & STATUS_IN_PROGRESS) != (status & STATUS_IN_PROGRESS)) {
			if ((status & STATUS_IN_PROGRESS) != 0) {
				// Starting...
				dmacThread.execute(memDst, memSrc, dst, src, next, attributes, status, interruptAction, completedAction);
			} else {
				// Stopping...
				dmacThread.abortJob();
			}
		}
	}

	public void write32(int offset, int value) {
		switch (offset) {
			case 0x00: setSrc(value); break;
			case 0x04: setDst(value); break;
			case 0x08: setNext(value); break;
			case 0x0C: setAttributes(value); break;
			case 0x10: setStatus(value); break;
		}
	}

	public int read32(int offset) {
		switch (offset) {
			case 0x00: return getSrc();
			case 0x04: return getDst();
			case 0x08: return getNext();
			case 0x0C: return getAttributes();
			case 0x10: return getStatus();
		}

		return 0;
	}
}
