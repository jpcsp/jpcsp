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

public class SceKernelErrors {

    public final static int ERROR_UNKNOWN_UID = 0x800200cb;
    // some missing

    public final static int ERROR_ILLEGAL_THREAD = 0x80020197;
    public final static int ERROR_NOT_FOUND_THREAD = 0x80020198;
    public final static int ERROR_NOT_FOUND_SEMAPHORE = 0x80020199;
    public final static int ERROR_NOT_FOUND_EVENT_FLAG = 0x8002019a;
    public final static int ERROR_NOT_FOUND_MESSAGE_BOX = 0x8002019b;
    public final static int ERROR_NOT_FOUND_VPOOL = 0x8002019c;
    public final static int ERROR_NOT_FOUND_FPOOL = 0x8002019d;
    public final static int ERROR_NOT_FOUND_MESSAGE_PIPE = 0x8002019e;
    public final static int ERROR_NOT_FOUND_ALARM = 0x8002019f;
    public final static int ERROR_NOT_FOUND_THREAD_EVENT_HANDLER = 0x800201a0;
    public final static int ERROR_NOT_FOUND_CALLBACK = 0x800201a1;
    public final static int ERROR_THREAD_ALREADY_DORMANT = 0x800201a2;
    public final static int ERROR_THREAD_ALREADY_SUSPEND = 0x800201a3;
    public final static int ERROR_THREAD_IS_NOT_DORMANT = 0x800201a4;
    public final static int ERROR_THREAD_IS_NOT_SUSPEND = 0x800201a5;
    public final static int ERROR_THREAD_IS_NOT_WAIT = 0x800201a6;
    // 0x800201a7 missing
    public final static int ERROR_WAIT_TIMEOUT = 0x800201a8;
    public final static int ERROR_WAIT_CANCELLED = 0x800201a9;
    // some missing
    public final static int ERROR_SEMA_ZERO = 0x800201ad;
    public final static int ERROR_SEMA_OVERFLOW = 0x800201ae;
    public final static int ERROR_EVENT_FLAG_POLL_FAILED = 0x800201af;
    public final static int ERROR_EVENT_FLAG_NO_MULTI_PERM = 0x800201b0;
    // some missing
    public final static int ERROR_WAIT_DELETE = 0x800201b5;
    // some missing
    public final static int ERROR_ILLEGAL_COUNT = 0x800201bd;
    // some missing
    public final static int ERROR_MUTEX_LOCKED = 0x800201c4; // 2.71+
}
