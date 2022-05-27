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

import jpcsp.HLE.kernel.managers.EventFlagManager;
import jpcsp.HLE.kernel.managers.FplManager;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.managers.MbxManager;
import jpcsp.HLE.kernel.managers.ModuleManager;
import jpcsp.HLE.kernel.managers.MsgPipeManager;
import jpcsp.HLE.kernel.managers.MutexManager;
import jpcsp.HLE.kernel.managers.LwMutexManager;
import jpcsp.HLE.kernel.managers.SemaManager;
import jpcsp.HLE.kernel.managers.SystemTimeManager;
import jpcsp.HLE.kernel.managers.VplManager;

/**
 *
 * @author hli
 */
public class Managers {
    public static SemaManager semas;
    public static EventFlagManager eventFlags;
    public static FplManager fpl;
    public static VplManager vpl;
    public static MutexManager mutex;
    public static LwMutexManager lwmutex;
    public static MsgPipeManager msgPipes;
    public static ModuleManager modules;
    public static SystemTimeManager systime;
    public static MbxManager mbx;
    public static IntrManager intr;

    /** call this when resetting the emulator */
    public static void reset() {
        semas.reset();
        eventFlags.reset();
        fpl.reset();
        vpl.reset();
        mutex.reset();
        lwmutex.reset();
        msgPipes.reset();
        modules.reset();
        systime.reset();
        mbx.reset();
        intr.reset();
    }

    static {
        semas = SemaManager.singleton;
        eventFlags = EventFlagManager.singleton;
        fpl = FplManager.singleton;
        vpl = VplManager.singleton;
        mutex = MutexManager.singleton;
        lwmutex = LwMutexManager.singleton;
        msgPipes = MsgPipeManager.singleton;
        modules = ModuleManager.singleton;
        systime = SystemTimeManager.singleton;
        mbx = MbxManager.singleton;
        intr = IntrManager.getInstance();
    }
}