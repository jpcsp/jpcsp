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
package jpcsp.HLE.modules271;

import jpcsp.HLE.HLEFunction;
import jpcsp.Processor;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

public class ThreadManForUser extends jpcsp.HLE.modules150.ThreadManForUser {

    @Override
    public void installModule(HLEModuleManager mm, int version) { mm.installModuleWithAnnotations(this, version); }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) { mm.uninstallModuleWithAnnotations(this, version); }

    public void sceKernelTryLockMutex(Processor processor) {
        int[] gpr = processor.cpu.gpr;
        if (IntrManager.getInstance().isInsideInterrupt()) {
            gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        Managers.mutex.sceKernelTryLockMutex(gpr[4], gpr[5]);
    }

    public void sceKernelLockMutexCB(Processor processor) {
        int[] gpr = processor.cpu.gpr;

        if (IntrManager.getInstance().isInsideInterrupt()) {
            gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        Managers.mutex.sceKernelLockMutexCB(gpr[4], gpr[5], gpr[6]);
    }

    public void sceKernelUnlockMutex(Processor processor) {
        int[] gpr = processor.cpu.gpr;

        if (IntrManager.getInstance().isInsideInterrupt()) {
            gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        Managers.mutex.sceKernelUnlockMutex(gpr[4], gpr[5]);
    }

    public void sceKernelCancelMutex(Processor processor) {
        int[] gpr = processor.cpu.gpr;
        Managers.mutex.sceKernelCancelMutex(gpr[4], gpr[5], gpr[6]);
    }

    public void sceKernelReferMutexStatus(Processor processor) {
        int[] gpr = processor.cpu.gpr;
        Managers.mutex.sceKernelReferMutexStatus(gpr[4], gpr[5]);
    }

    public void sceKernelLockMutex(Processor processor) {
        int[] gpr = processor.cpu.gpr;

        if (IntrManager.getInstance().isInsideInterrupt()) {
            gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        Managers.mutex.sceKernelLockMutex(gpr[4], gpr[5], gpr[6]);
    }

    public void sceKernelCreateMutex(Processor processor) {
        int[] gpr = processor.cpu.gpr;

        if (IntrManager.getInstance().isInsideInterrupt()) {
            gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        Managers.mutex.sceKernelCreateMutex(gpr[4], gpr[5], gpr[6], gpr[7]);
    }

    public void sceKernelDeleteMutex(Processor processor) {
        int[] gpr = processor.cpu.gpr;

        if (IntrManager.getInstance().isInsideInterrupt()) {
            gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        Managers.mutex.sceKernelDeleteMutex(gpr[4]);
    }    @HLEFunction(nid = 0x0DDCD2C9, version = 271)
    public final HLEModuleFunction sceKernelTryLockMutexFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelTryLockMutex") {

        @Override
        public final void execute(Processor processor) {
            sceKernelTryLockMutex(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelTryLockMutex(processor);";
        }
    };    @HLEFunction(nid = 0x5BF4DD27, version = 271)
    public final HLEModuleFunction sceKernelLockMutexCBFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelLockMutexCB") {

        @Override
        public final void execute(Processor processor) {
            sceKernelLockMutexCB(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelLockMutexCB(processor);";
        }
    };    @HLEFunction(nid = 0x6B30100F, version = 271)
    public final HLEModuleFunction sceKernelUnlockMutexFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelUnlockMutex") {

        @Override
        public final void execute(Processor processor) {
            sceKernelUnlockMutex(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelUnlockMutex(processor);";
        }
    };    @HLEFunction(nid = 0x87D9223C, version = 271)
    public final HLEModuleFunction sceKernelCancelMutexFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelCancelMutex") {

        @Override
        public final void execute(Processor processor) {
            sceKernelCancelMutex(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelCancelMutex(processor);";
        }
    };    @HLEFunction(nid = 0xA9C2CB9A, version = 271)
    public final HLEModuleFunction sceKernelReferMutexStatusFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelReferMutexStatus") {

        @Override
        public final void execute(Processor processor) {
            sceKernelReferMutexStatus(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelReferMutexStatus(processor);";
        }
    };    @HLEFunction(nid = 0xB011B11F, version = 271)
    public final HLEModuleFunction sceKernelLockMutexFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelLockMutex") {

        @Override
        public final void execute(Processor processor) {
            sceKernelLockMutex(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelLockMutex(processor);";
        }
    };    @HLEFunction(nid = 0xB7D098C6, version = 271)
    public final HLEModuleFunction sceKernelCreateMutexFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelCreateMutex") {

        @Override
        public final void execute(Processor processor) {
            sceKernelCreateMutex(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelCreateMutex(processor);";
        }
    };    @HLEFunction(nid = 0xF8170FBE, version = 271)
    public final HLEModuleFunction sceKernelDeleteMutexFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelDeleteMutex") {

        @Override
        public final void execute(Processor processor) {
            sceKernelDeleteMutex(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelDeleteMutex(processor);";
        }
    };
}