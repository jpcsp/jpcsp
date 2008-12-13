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
}
