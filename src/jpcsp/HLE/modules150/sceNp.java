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

public class sceNp implements HLEModule {

    protected static Logger log = Modules.getLogger("sceNp");

    @Override
    public String getName() {
        return "sceNp";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.addFunction(0x98BEF739, sceNpInitFunction);
            mm.addFunction(0x61850E3B, sceNpTermFunction);
            mm.addFunction(0x9A688BB0, sceNpGetNpIdFunction);
            mm.addFunction(0x212989E3, sceNpGetOnlineIdFunction);
            mm.addFunction(0xD355FC0F, sceNpGetUserProfileFunction);
            mm.addFunction(0xD416547F, sceNpGetMyLanguagesFunction);
            mm.addFunction(0xDB46D1F1, sceNpGetAccountRegionFunction);
            mm.addFunction(0x0D69A94B, sceNpGetContentRatingFlagFunction);
            mm.addFunction(0xCF0483E5, sceNpGetChatRestrictionFlagFunction);

        }
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.removeFunction(sceNpInitFunction);
            mm.removeFunction(sceNpTermFunction);
            mm.removeFunction(sceNpGetNpIdFunction);
            mm.removeFunction(sceNpGetOnlineIdFunction);
            mm.removeFunction(sceNpGetUserProfileFunction);
            mm.removeFunction(sceNpGetMyLanguagesFunction);
            mm.removeFunction(sceNpGetAccountRegionFunction);
            mm.removeFunction(sceNpGetContentRatingFlagFunction);
            mm.removeFunction(sceNpGetChatRestrictionFlagFunction);

        }
    }

    public void sceNpInit(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNpInit");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNpTerm(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNpTerm");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNpGetNpId(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNpGetNpId");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNpGetOnlineId(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNpGetOnlineId");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNpGetUserProfile(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNpGetUserProfile");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNpGetMyLanguages(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNpGetMyLanguages");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNpGetAccountRegion(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNpGetAccountRegion");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNpGetContentRatingFlag(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNpGetContentRatingFlag");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNpGetChatRestrictionFlag(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNpGetChatRestrictionFlag");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public final HLEModuleFunction sceNpInitFunction = new HLEModuleFunction("sceNp", "sceNpInit") {

        @Override
        public final void execute(Processor processor) {
            sceNpInit(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNpModule.sceNpInit(processor);";
        }
    };

    public final HLEModuleFunction sceNpTermFunction = new HLEModuleFunction("sceNp", "sceNpTerm") {

        @Override
        public final void execute(Processor processor) {
            sceNpTerm(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNpModule.sceNpTerm(processor);";
        }
    };

    public final HLEModuleFunction sceNpGetNpIdFunction = new HLEModuleFunction("sceNp", "sceNpGetNpId") {

        @Override
        public final void execute(Processor processor) {
            sceNpGetNpId(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNpModule.sceNpGetNpId(processor);";
        }
    };

    public final HLEModuleFunction sceNpGetOnlineIdFunction = new HLEModuleFunction("sceNp", "sceNpGetOnlineId") {

        @Override
        public final void execute(Processor processor) {
            sceNpGetOnlineId(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNpModule.sceNpGetOnlineId(processor);";
        }
    };

    public final HLEModuleFunction sceNpGetUserProfileFunction = new HLEModuleFunction("sceNp", "sceNpGetUserProfile") {

        @Override
        public final void execute(Processor processor) {
            sceNpGetUserProfile(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNpModule.sceNpGetUserProfile(processor);";
        }
    };

    public final HLEModuleFunction sceNpGetMyLanguagesFunction = new HLEModuleFunction("sceNp", "sceNpGetMyLanguages") {

        @Override
        public final void execute(Processor processor) {
            sceNpGetMyLanguages(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNpModule.sceNpGetMyLanguages(processor);";
        }
    };

    public final HLEModuleFunction sceNpGetAccountRegionFunction = new HLEModuleFunction("sceNp", "sceNpGetAccountRegion") {

        @Override
        public final void execute(Processor processor) {
            sceNpGetAccountRegion(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNpModule.sceNpGetAccountRegion(processor);";
        }
    };

    public final HLEModuleFunction sceNpGetContentRatingFlagFunction = new HLEModuleFunction("sceNp", "sceNpGetContentRatingFlag") {

        @Override
        public final void execute(Processor processor) {
            sceNpGetContentRatingFlag(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNpModule.sceNpGetContentRatingFlag(processor);";
        }
    };

    public final HLEModuleFunction sceNpGetChatRestrictionFlagFunction = new HLEModuleFunction("sceNp", "sceNpGetChatRestrictionFlag") {

        @Override
        public final void execute(Processor processor) {
            sceNpGetChatRestrictionFlag(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNpModule.sceNpGetChatRestrictionFlag(processor);";
        }
    };
}