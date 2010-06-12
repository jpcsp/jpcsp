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

    // TODO change waitingOnThreadEnd, waitingOnEventFlag, etc to waitType,
    // since we can only wait on one type of event at a time.

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

    public void copy(ThreadWaitInfo that) {
    	this.forever = that.forever;
    	this.microTimeTimeout = that.microTimeTimeout;
    	this.micros = that.micros;
    	this.waitTimeoutAction = that.waitTimeoutAction;

    	this.waitingOnThreadEnd = that.waitingOnThreadEnd;
    	this.ThreadEnd_id = that.ThreadEnd_id;

    	this.waitingOnEventFlag = that.waitingOnEventFlag;
    	this.EventFlag_id = that.EventFlag_id;
    	this.EventFlag_bits = that.EventFlag_bits;
    	this.EventFlag_wait = that.EventFlag_wait;
    	this.EventFlag_outBits_addr = that.EventFlag_outBits_addr;

    	this.waitingOnSemaphore = that.waitingOnSemaphore;
    	this.Semaphore_id = that.Semaphore_id;
    	this.Semaphore_signal = that.Semaphore_signal;

    	this.waitingOnMutex = that.waitingOnMutex;
    	this.Mutex_id = that.Mutex_id;

    	this.waitingOnIo = that.waitingOnIo;
    	this.Io_id = that.Io_id;

    	this.waitingOnUmd = that.waitingOnUmd;
    	this.wantedUmdStat = that.wantedUmdStat;

    	this.waitingOnMsgPipeSend = that.waitingOnMsgPipeSend;
    	this.waitingOnMsgPipeReceive = that.waitingOnMsgPipeReceive;
    	this.MsgPipe_id = that.MsgPipe_id;
    	this.MsgPipe_address = that.MsgPipe_address;
    	this.MsgPipe_size = that.MsgPipe_size;
    	this.MsgPipe_waitMode = that.MsgPipe_waitMode;
    	this.MsgPipe_resultSize_addr = that.MsgPipe_resultSize_addr;
    }
}
