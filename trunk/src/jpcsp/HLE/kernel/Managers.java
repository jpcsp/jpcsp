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
package jpcsp.HLE.kernel;

import jpcsp.HLE.kernel.managers.*;

/**
 *
 * @author hli
 */
public class Managers {
    //public static UidManager uids;
    //public static CallbackManager callbacks;
    //public static SemaphoreManager semaphores;
    public static EventFlagManager eventFlags;
    //public static ThreadManager threads;
    public static FplManager fpl;
    public static VplManager vpl;
    public static MutexManager mutex;
    public static ModuleManager modules;
    public static SystemTimeManager systime;

    /** call this when resetting the emulator */
    public static void reset() {
        // TODO add other reset calls here
        eventFlags.reset();
        fpl.reset();
        vpl.reset();
        mutex.reset();
        modules.reset();
        systime.reset();
    }

    static {
        //uids = UidManager.singleton;
        //callbacks = CallbackManager.singleton;
        //semaphores = SemaphoreManager.singleton;
        eventFlags = EventFlagManager.singleton;
        //threads = ThreadManager.singleton;
        fpl = FplManager.singleton;
        vpl = VplManager.singleton;
        mutex = MutexManager.singleton;
        modules = ModuleManager.singleton;
        systime = SystemTimeManager.singleton;
    }
}
