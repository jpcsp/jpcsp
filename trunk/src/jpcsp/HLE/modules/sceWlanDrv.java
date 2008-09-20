/* This autogenerated file is part of jpcsp. */
package jpcsp.HLE.modules;

import jpcsp.HLE.pspSysMem;
import jpcsp.Memory;
import jpcsp.Processor;

public class sceWlanDrv implements HLEModule {

    @Override
    public final String getName() {
        return "sceWlanDrv";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) {

        mm.add(sceWlanDevIsPowerOn, 0x93440B11);

        mm.add(sceWlanGetSwitchState, 0xD7763699);

        mm.add(sceWlanGetEtherAddr, 0x0C622081);

    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {

        mm.remove(sceWlanDevIsPowerOn);

        mm.remove(sceWlanGetSwitchState);

        mm.remove(sceWlanGetEtherAddr);

    }
    public static final HLEModuleFunction sceWlanDevIsPowerOn = new HLEModuleFunction("sceWlanDrv", "sceWlanDevIsPowerOn") {

        @Override
        public void execute(Processor cpu, Memory mem) {
            System.out.println("Unimplement function sceWlanDevIsPowerOn [0x93440B11]");
        }
    };
    public static final HLEModuleFunction sceWlanGetSwitchState = new HLEModuleFunction("sceWlanDrv", "sceWlanGetSwitchState") {

        @Override
        public void execute(Processor cpu, Memory mem) {
            System.out.println("Unimplement function sceWlanGetSwitchState [0xD7763699]");
        }
    };
    public static final HLEModuleFunction sceWlanGetEtherAddr = new HLEModuleFunction("sceWlanDrv", "sceWlanGetEtherAddr") {

        @Override
        public void execute(Processor cpu, Memory mem) {
            System.out.println("Unimplement function sceWlanGetEtherAddr [0x0C622081]");
        }
    };
};
