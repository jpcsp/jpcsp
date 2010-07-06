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

/*
 * TODO list:
 * 1. Change waitingOnThreadEnd, waitingOnEventFlag, etc to waitType
 * (can only wait on one type of event at a time).
 */

public class ThreadWaitInfo {
    public boolean forever;
    public long microTimeTimeout; // when Clock.microTime() reaches microTimeTimeout the wait has expired
    public int micros; // time period specified by the game, just stored here for logging/debugging purposes
    public IAction waitTimeoutAction; // execute this action when the timeout is reached
    public IWaitStateChecker waitStateChecker; // checks if the wait condition still applies

    // Thread End
    public boolean waitingOnThreadEnd;
    public int ThreadEnd_id;

    // Event Flag
    public boolean waitingOnEventFlag;
    public int EventFlag_id;
    public int EventFlag_bits;
    public int EventFlag_wait;
    public int EventFlag_outBits_addr;

    // Semaphore
    public boolean waitingOnSemaphore;
    public int Semaphore_id;
    public int Semaphore_signal;

    // Mutex
    public boolean waitingOnMutex;
    public int Mutex_id;
    public int Mutex_count;

    // IO
    public boolean waitingOnIo;
    public int Io_id;

    // UMD
    public boolean waitingOnUmd;
    public int wantedUmdStat;

    // MsgPipe
    public boolean waitingOnMsgPipeSend;
    public boolean waitingOnMsgPipeReceive;
    public int MsgPipe_id;
    public int MsgPipe_address;
    public int MsgPipe_size;
    public int MsgPipe_waitMode;
    public int MsgPipe_resultSize_addr;

     //Mbx
    public boolean waitingOnMbxReceive;
    public int Mbx_id;
    public int Mbx_resultAddr;

    public void copy(ThreadWaitInfo that) {
    	forever = that.forever;
    	microTimeTimeout = that.microTimeTimeout;
    	micros = that.micros;
    	waitTimeoutAction = that.waitTimeoutAction;
    	waitStateChecker = that.waitStateChecker;

    	waitingOnThreadEnd = that.waitingOnThreadEnd;
    	ThreadEnd_id = that.ThreadEnd_id;

    	waitingOnEventFlag = that.waitingOnEventFlag;
    	EventFlag_id = that.EventFlag_id;
    	EventFlag_bits = that.EventFlag_bits;
    	EventFlag_wait = that.EventFlag_wait;
    	EventFlag_outBits_addr = that.EventFlag_outBits_addr;

    	waitingOnSemaphore = that.waitingOnSemaphore;
    	Semaphore_id = that.Semaphore_id;
    	Semaphore_signal = that.Semaphore_signal;

    	waitingOnMutex = that.waitingOnMutex;
    	Mutex_id = that.Mutex_id;
    	Mutex_count = that.Mutex_count;

    	waitingOnIo = that.waitingOnIo;
    	Io_id = that.Io_id;

    	waitingOnUmd = that.waitingOnUmd;
    	wantedUmdStat = that.wantedUmdStat;

    	waitingOnMsgPipeSend = that.waitingOnMsgPipeSend;
    	waitingOnMsgPipeReceive = that.waitingOnMsgPipeReceive;
    	MsgPipe_id = that.MsgPipe_id;
    	MsgPipe_address = that.MsgPipe_address;
    	MsgPipe_size = that.MsgPipe_size;
    	MsgPipe_waitMode = that.MsgPipe_waitMode;
    	MsgPipe_resultSize_addr = that.MsgPipe_resultSize_addr;

    	waitingOnMbxReceive = that.waitingOnMbxReceive;
    	Mbx_id = that.Mbx_id;
    	Mbx_resultAddr = that.Mbx_resultAddr;
    }
}