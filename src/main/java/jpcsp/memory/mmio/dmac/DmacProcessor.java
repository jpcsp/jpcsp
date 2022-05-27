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

import static jpcsp.util.Utilities.clearFlag;
import static jpcsp.util.Utilities.isFallingFlag;
import static jpcsp.util.Utilities.isRaisingFlag;

import java.io.IOException;

import jpcsp.Memory;
import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.state.IState;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

public class DmacProcessor implements IState {
	private static final int STATE_VERSION = 0;
	// Dmac STATUS flags:
	public static final int DMAC_STATUS_IN_PROGRESS                = 0x00000001;
	public static final int DMAC_STATUS_DDR_VALUE                  = 0x000000F0;
	public static final int DMAC_STATUS_DDR_VALUE_SHIFT            = 4;
	public static final int DMAC_STATUS_REQUIRES_DDR               = 0x00000100;
	public static final int DMAC_STATUS_UNKNOWN                    = 0xFFFFFE0E;
	// Dmac ATTRIBUTES flags:
	public static final int DMAC_ATTRIBUTES_LENGTH                 = 0x00000FFF;
	//                                                               0x00007000
	public static final int DMAC_ATTRIBUTES_SRC_STEP_SHIFT         = 12;
	//                                                               0x00038000
	public static final int DMAC_ATTRIBUTES_DST_STEP_SHIFT         = 15;
	//                                                               0x001C0000
	public static final int DMAC_ATTRIBUTES_SRC_LENGTH_SHIFT_SHIFT = 18;
	//                                                               0x00E00000
	public static final int DMAC_ATTRIBUTES_DST_LENGTH_SHIFT_SHIFT = 21;
	public static final int DMAC_ATTRIBUTES_UNKNOWN1               = 0x01000000;
	public static final int DMAC_ATTRIBUTES_UNKNOWN2               = 0x02000000;
	public static final int DMAC_ATTRIBUTES_SRC_INCREMENT          = 0x04000000;
	public static final int DMAC_ATTRIBUTES_DST_INCREMENT          = 0x08000000;
	public static final int DMAC_ATTRIBUTES_TRIGGER_INTERRUPT      = 0x80000000;
	private final Memory memSrc;
	private final Memory memDst;
	private final IAction interruptAction;
	private final IAction completedAction;
	private final DmacThread dmacThread;
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
			// Clear the status "in progress"
			status = clearFlag(status, DMAC_STATUS_IN_PROGRESS);
		}
	}

	private class ExitAction implements IAction {
		@Override
		public void execute() {
			dmacThread.exit();
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

		RuntimeContextLLE.registerExitAction(new ExitAction());
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		dst = stream.readInt();
		src = stream.readInt();
		next = stream.readInt();
		attributes = stream.readInt();
		status = stream.readInt();
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(dst);
		stream.writeInt(src);
		stream.writeInt(next);
		stream.writeInt(attributes);
		stream.writeInt(status);
	}

	public void reset() {
		dst = 0;
		src = 0;
		next = 0;
		attributes = 0;
		status = 0;
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
		if (isFallingFlag(previousStatus, status, DMAC_STATUS_IN_PROGRESS)) {
			// Stopping...
			dmacThread.abortJob();
		} else if (isRaisingFlag(previousStatus, status, DMAC_STATUS_IN_PROGRESS)) {
			// Starting...
			dmacThread.execute(memDst, memSrc, dst, src, next, attributes, status, interruptAction, completedAction);
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
