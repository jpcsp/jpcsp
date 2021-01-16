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
package jpcsp.memory.mmio.wlan.threadx;

import static jpcsp.util.Utilities.setBit;

import jpcsp.arm.ARMProcessor;

/**
 * @author gid15
 *
 */
public class TXKernelEnter extends TXBaseCall {
	private int txInitializeLowLevel;
	private int txIrqHandler;
	private int txApplicationDefine;

	public TXKernelEnter(int txInitializeLowLevel, int txIrqHandler, int txApplicationDefine) {
		this.txInitializeLowLevel = txInitializeLowLevel;
		this.txIrqHandler = txIrqHandler;
		this.txApplicationDefine = txApplicationDefine;
	}

	@Override
	public void call(ARMProcessor processor, int imm) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("TXKernelEnter txInitializeLowLevel=0x%08X, txIrqHandler=0x%08X, txApplicationDefine=0x%08X", txInitializeLowLevel, txIrqHandler, txApplicationDefine));
		}

		// Execute _tx_initialize_low_level
		execute(processor, txInitializeLowLevel, "_tx_initialize_low_level");

		getTxManager().setTxIrqHandler(txIrqHandler);

		// Execute tx_application_define
		execute(processor, txApplicationDefine, "tx_application_define");

		// Return to threadSchedule
		int threadSchedule = setBit(processor.getCurrentInstructionPc() + 2, 0);
		processor.interpreter.registerHLECall(threadSchedule, imm, new TXThreadSchedule());
		jump(processor, threadSchedule);
	}
}
