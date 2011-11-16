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
package jpcsp.HLE.kernel.types;

public class ThreadWaitInfo {
    public boolean forever;
    public long microTimeTimeout; // when Clock.microTime() reaches microTimeTimeout the wait has expired
    public int micros; // time period specified by the game, just stored here for logging/debugging purposes
    public IAction waitTimeoutAction; // execute this action when the timeout is reached
    public IWaitStateChecker waitStateChecker; // checks if the wait condition still applies

    // Thread End
    public int ThreadEnd_id;

    // Event Flag
    public int EventFlag_id;
    public int EventFlag_bits;
    public int EventFlag_wait;
    public int EventFlag_outBits_addr;

    // Semaphore
    public int Semaphore_id;
    public int Semaphore_signal;

    // Mutex
    public int Mutex_id;
    public int Mutex_count;

    // LwMutex
    public int LwMutex_id;
    public int LwMutex_count;

    // IO
    public int Io_id;
    public int Io_resultAddr;

    // UMD
    public int wantedUmdStat;

    // MsgPipe
    public int MsgPipe_id;
    public int MsgPipe_address;
    public int MsgPipe_size;
    public int MsgPipe_waitMode;
    public int MsgPipe_resultSize_addr;
    public boolean MsgPipe_isSend; // true if send, false if receive

    //Mbx
    public int Mbx_id;
    public int Mbx_resultAddr;

    //FPL
    public int Fpl_id;
    public int Fpl_dataAddr;

    //VPL
    public int Vpl_id;
    public int Vpl_size;
    public int Vpl_dataAddr;

    // Thread blocked (used internally)
    public IAction onUnblockAction;

    public void copy(ThreadWaitInfo that) {
    	forever = that.forever;
    	microTimeTimeout = that.microTimeTimeout;
    	micros = that.micros;
    	waitTimeoutAction = that.waitTimeoutAction;
    	waitStateChecker = that.waitStateChecker;

    	ThreadEnd_id = that.ThreadEnd_id;

    	EventFlag_id = that.EventFlag_id;
    	EventFlag_bits = that.EventFlag_bits;
    	EventFlag_wait = that.EventFlag_wait;
    	EventFlag_outBits_addr = that.EventFlag_outBits_addr;

    	Semaphore_id = that.Semaphore_id;
    	Semaphore_signal = that.Semaphore_signal;

    	Mutex_id = that.Mutex_id;
    	Mutex_count = that.Mutex_count;

    	LwMutex_id = that.LwMutex_id;
    	LwMutex_count = that.LwMutex_count;

    	Io_id = that.Io_id;
    	Io_resultAddr = that.Io_resultAddr;

    	wantedUmdStat = that.wantedUmdStat;

    	MsgPipe_id = that.MsgPipe_id;
    	MsgPipe_address = that.MsgPipe_address;
    	MsgPipe_size = that.MsgPipe_size;
    	MsgPipe_waitMode = that.MsgPipe_waitMode;
    	MsgPipe_resultSize_addr = that.MsgPipe_resultSize_addr;
    	MsgPipe_isSend = that.MsgPipe_isSend;

    	Mbx_id = that.Mbx_id;
    	Mbx_resultAddr = that.Mbx_resultAddr;

        Fpl_id = that.Fpl_id;
        Fpl_dataAddr = that.Fpl_dataAddr;

        Vpl_id = that.Vpl_id;
        Vpl_size = that.Vpl_size;
        Vpl_dataAddr = that.Vpl_dataAddr;

        onUnblockAction = that.onUnblockAction;
    }

	@Override
	public String toString() {
		return SceKernelThreadInfo.getWaitName(0, this, SceKernelThreadInfo.PSP_THREAD_WAITING);
	}
}