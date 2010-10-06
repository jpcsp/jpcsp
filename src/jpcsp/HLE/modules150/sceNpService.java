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

public class sceNpService implements HLEModule {

    protected static Logger log = Modules.getLogger("sceNpService");

    @Override
    public String getName() {
        return "sceNpService";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.addFunction(0xDF3F0F63, sceNpServiceInitFunction);
            mm.addFunction(0x0ABF56FB, sceNpServiceTermFunction);
            mm.addFunction(0x6177F13B, sceNpManagerSigninUpdateInitStartFunction);
            mm.addFunction(0x8303CF16, sceNpManagerSigninUpdateGetStatusFunction);
            mm.addFunction(0xB5317629, sceNpManagerSigninUpdateAbortFunction);
            mm.addFunction(0x88E1A727, sceNpManagerSigninUpdateShutdownStartFunction);
            mm.addFunction(0x75409949, sceNpServiceGetMemoryStatFunction);

        }
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.removeFunction(sceNpServiceInitFunction);
            mm.removeFunction(sceNpServiceTermFunction);
            mm.removeFunction(sceNpManagerSigninUpdateInitStartFunction);
            mm.removeFunction(sceNpManagerSigninUpdateGetStatusFunction);
            mm.removeFunction(sceNpManagerSigninUpdateAbortFunction);
            mm.removeFunction(sceNpManagerSigninUpdateShutdownStartFunction);
            mm.removeFunction(sceNpServiceGetMemoryStatFunction);

        }
    }

    public void sceNpServiceInit(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNpServiceInit");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNpServiceTerm(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNpServiceTerm");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNpManagerSigninUpdateInitStart(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNpManagerSigninUpdateInitStart");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNpManagerSigninUpdateGetStatus(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNpManagerSigninUpdateGetStatus");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNpManagerSigninUpdateAbort(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNpManagerSigninUpdateAbort");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNpManagerSigninUpdateShutdownStart(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNpManagerSigninUpdateShutdownStart");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNpServiceGetMemoryStat(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNpServiceGetMemoryStat");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public final HLEModuleFunction sceNpServiceInitFunction = new HLEModuleFunction("sceNpService", "sceNpServiceInit") {

        @Override
        public final void execute(Processor processor) {
            sceNpServiceInit(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNpServiceModule.sceNpServiceInit(processor);";
        }
    };

    public final HLEModuleFunction sceNpServiceTermFunction = new HLEModuleFunction("sceNpService", "sceNpServiceTerm") {

        @Override
        public final void execute(Processor processor) {
            sceNpServiceTerm(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNpServiceModule.sceNpServiceTerm(processor);";
        }
    };

    public final HLEModuleFunction sceNpManagerSigninUpdateInitStartFunction = new HLEModuleFunction("sceNpService", "sceNpManagerSigninUpdateInitStart") {

        @Override
        public final void execute(Processor processor) {
            sceNpManagerSigninUpdateInitStart(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNpServiceModule.sceNpManagerSigninUpdateInitStart(processor);";
        }
    };

    public final HLEModuleFunction sceNpManagerSigninUpdateGetStatusFunction = new HLEModuleFunction("sceNpService", "sceNpManagerSigninUpdateGetStatus") {

        @Override
        public final void execute(Processor processor) {
            sceNpManagerSigninUpdateGetStatus(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNpServiceModule.sceNpManagerSigninUpdateGetStatus(processor);";
        }
    };

    public final HLEModuleFunction sceNpManagerSigninUpdateAbortFunction = new HLEModuleFunction("sceNpService", "sceNpManagerSigninUpdateAbort") {

        @Override
        public final void execute(Processor processor) {
            sceNpManagerSigninUpdateAbort(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNpServiceModule.sceNpManagerSigninUpdateAbort(processor);";
        }
    };

    public final HLEModuleFunction sceNpManagerSigninUpdateShutdownStartFunction = new HLEModuleFunction("sceNpService", "sceNpManagerSigninUpdateShutdownStart") {

        @Override
        public final void execute(Processor processor) {
            sceNpManagerSigninUpdateShutdownStart(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNpServiceModule.sceNpManagerSigninUpdateShutdownStart(processor);";
        }
    };

    public final HLEModuleFunction sceNpServiceGetMemoryStatFunction = new HLEModuleFunction("sceNpService", "sceNpServiceGetMemoryStat") {

        @Override
        public final void execute(Processor processor) {
            sceNpServiceGetMemoryStat(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNpServiceModule.sceNpServiceGetMemoryStat(processor);";
        }
    };
}