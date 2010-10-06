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
package jpcsp.HLE.modules150;

import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

import org.apache.log4j.Logger;

public class sceNpAuth implements HLEModule {

    protected static Logger log = Modules.getLogger("sceNpAuth");

    @Override
    public String getName() {
        return "sceNpAuth";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.addFunction(0x58A7A48D, sceNpAuthInitFunction);
            mm.addFunction(0x9C9502AB, sceNpAuthTermFunction);
            mm.addFunction(0x3A48150F, sceNpAuthCreateStartRequestFunction);
            mm.addFunction(0xE99C0F56, sceNpAuthDestroyRequestFunction);
            mm.addFunction(0x8D07F54A, sceNpAuthAbortRequestFunction);
            mm.addFunction(0x5B6700E9, sceNpAuthGetTicketFunction);
            mm.addFunction(0x9E2B5B32, sceNpAuthGetMemoryStatFunction);

        }
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.removeFunction(sceNpAuthInitFunction);
            mm.removeFunction(sceNpAuthTermFunction);
            mm.removeFunction(sceNpAuthCreateStartRequestFunction);
            mm.removeFunction(sceNpAuthDestroyRequestFunction);
            mm.removeFunction(sceNpAuthAbortRequestFunction);
            mm.removeFunction(sceNpAuthGetTicketFunction);
            mm.removeFunction(sceNpAuthGetMemoryStatFunction);

        }
    }

    public void sceNpAuthInit(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNpAuthInit");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNpAuthTerm(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNpAuthTerm");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNpAuthCreateStartRequest(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNpAuthCreateStartRequest");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNpAuthDestroyRequest(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNpAuthDestroyRequest");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNpAuthAbortRequest(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNpAuthAbortRequest");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNpAuthGetTicket(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNpAuthGetTicket");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNpAuthGetMemoryStat(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNpAuthGetMemoryStat");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public final HLEModuleFunction sceNpAuthInitFunction = new HLEModuleFunction("sceNpAuth", "sceNpAuthInit") {

        @Override
        public final void execute(Processor processor) {
            sceNpAuthInit(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNpAuthModule.sceNpAuthInit(processor);";
        }
    };

    public final HLEModuleFunction sceNpAuthTermFunction = new HLEModuleFunction("sceNpAuth", "sceNpAuthTerm") {

        @Override
        public final void execute(Processor processor) {
            sceNpAuthTerm(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNpAuthModule.sceNpAuthTerm(processor);";
        }
    };

    public final HLEModuleFunction sceNpAuthCreateStartRequestFunction = new HLEModuleFunction("sceNpAuth", "sceNpAuthCreateStartRequest") {

        @Override
        public final void execute(Processor processor) {
            sceNpAuthCreateStartRequest(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNpAuthModule.sceNpAuthCreateStartRequest(processor);";
        }
    };

    public final HLEModuleFunction sceNpAuthDestroyRequestFunction = new HLEModuleFunction("sceNpAuth", "sceNpAuthDestroyRequest") {

        @Override
        public final void execute(Processor processor) {
            sceNpAuthDestroyRequest(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNpAuthModule.sceNpAuthDestroyRequest(processor);";
        }
    };

    public final HLEModuleFunction sceNpAuthAbortRequestFunction = new HLEModuleFunction("sceNpAuth", "sceNpAuthAbortRequest") {

        @Override
        public final void execute(Processor processor) {
            sceNpAuthAbortRequest(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNpAuthModule.sceNpAuthAbortRequest(processor);";
        }
    };

    public final HLEModuleFunction sceNpAuthGetTicketFunction = new HLEModuleFunction("sceNpAuth", "sceNpAuthGetTicket") {

        @Override
        public final void execute(Processor processor) {
            sceNpAuthGetTicket(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNpAuthModule.sceNpAuthGetTicket(processor);";
        }
    };

    public final HLEModuleFunction sceNpAuthGetMemoryStatFunction = new HLEModuleFunction("sceNpAuth", "sceNpAuthGetMemoryStat") {

        @Override
        public final void execute(Processor processor) {
            sceNpAuthGetMemoryStat(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNpAuthModule.sceNpAuthGetMemoryStat(processor);";
        }
    };
}