/* This autogenerated file is part of jpcsp. */
package jpcsp.HLE.modules;

import jpcsp.HLE.pspSysMem;
import jpcsp.Memory;
import jpcsp.Processor;

public class sceVideocodec_driver implements HLEModule {

    @Override
    public final String getName() {
        return "sceVideocodec_driver";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) {

        mm.add(sceVideocodecStartEntry, 0xE76D65FE);

        mm.add(sceVideocodecEndEntry, 0xCB3312D1);

    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {

        mm.remove(sceVideocodecStartEntry);

        mm.remove(sceVideocodecEndEntry);

    }
    public static final HLEModuleFunction sceVideocodecStartEntry = new HLEModuleFunction("sceVideocodec_driver", "sceVideocodecStartEntry") {

        @Override
        public void execute(Processor cpu, Memory mem) {
            System.out.println("Unimplement function sceVideocodecStartEntry [0xE76D65FE]");
        }
    };
    public static final HLEModuleFunction sceVideocodecEndEntry = new HLEModuleFunction("sceVideocodec_driver", "sceVideocodecEndEntry") {

        @Override
        public void execute(Processor cpu, Memory mem) {
            System.out.println("Unimplement function sceVideocodecEndEntry [0xCB3312D1]");
        }
    };
};
