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

import jpcsp.HLE.HLEFunction;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.HLEStartModule;

import org.apache.log4j.Logger;

public class sceHprm implements HLEModule, HLEStartModule {
    private static Logger log = Modules.getLogger("sceHprm");

    @Override
    public String getName() { return "sceHprm"; }

    @Override
    public void installModule(HLEModuleManager mm, int version) { mm.installModuleWithAnnotations(this, version); }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) { mm.uninstallModuleWithAnnotations(this, version); }
    
    @Override
    public void start() {
    	hprmWarningLogged = false;
    }

    @Override
    public void stop() {
    }

    private boolean enableRemote = false;
    private boolean enableHeadphone = false;
    private boolean enableMicrophone = false;

    private boolean hprmWarningLogged;

    @HLEFunction(nid = 0xC7154136, version = 150)
    public void sceHprmRegisterCallback(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function sceHprmRegisterCallback [0xC7154136]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x444ED0B7, version = 150)
    public void sceHprmUnregisterCallback(Processor processor) {
        CpuState cpu = processor.cpu; 

        log.warn("Unimplemented NID function sceHprmUnregisterCallback [0x444ED0B7]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x71B5FB67, version = 150)
    public void sceHprmGetHpDetect(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function sceHprmGetHpDetect [0x71B5FB67]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x208DB1BD, version = 150)
    public void sceHprmIsRemoteExist(Processor processor) {
        CpuState cpu = processor.cpu;

        int result = enableRemote ? 1 : 0;
        log.debug("sceHprmIsRemoteExist ret:" + result);

        cpu.gpr[2] = result;
    }

    @HLEFunction(nid = 0x7E69EDA4, version = 150)
    public void sceHprmIsHeadphoneExist(Processor processor) {
        CpuState cpu = processor.cpu;

        int result = enableHeadphone ? 1 : 0;
        log.debug("sceHprmIsHeadphoneExist ret:" + result);

        cpu.gpr[2] = result;
    }

    @HLEFunction(nid = 0x219C58F1, version = 150)
    public void sceHprmIsMicrophoneExist(Processor processor) {
        CpuState cpu = processor.cpu;

        int result = enableMicrophone ? 1 : 0;
        log.debug("sceHprmIsMicrophoneExist ret:" + result);

        cpu.gpr[2] = result;
    }

    @HLEFunction(nid = 0x1910B327, version = 150)
    public void sceHprmPeekCurrentKey(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int key_addr = cpu.gpr[4];

        if (Memory.isAddressGood(key_addr)) {
            if (hprmWarningLogged) {
                if (log.isDebugEnabled()) {
                    log.debug("IGNORING: sceHprmPeekCurrentKey(key_addr=0x" + Integer.toHexString(key_addr) + ")");
                }
            } else {
                log.warn("IGNORING: sceHprmPeekCurrentKey(key_addr=0x" + Integer.toHexString(key_addr) + ") future calls will only appear in TRACE log");
                hprmWarningLogged = true;
            }
            mem.write32(key_addr, 0); // fake
            cpu.gpr[2] = 0; // check
        } else {
            log.warn("sceHprmPeekCurrentKey(key_addr=0x" + Integer.toHexString(key_addr) + ") invalid address");
            cpu.gpr[2] = -1; // check
        }
    }

    @HLEFunction(nid = 0x2BCEC83E, version = 150)
    public void sceHprmPeekLatch(Processor processor) {
        CpuState cpu = processor.cpu;

        int latchAddr = cpu.gpr[4];

        if (hprmWarningLogged) {
            if (log.isDebugEnabled()) {
            	log.debug(String.format("IGNORING: sceHprmPeekLatch 0x%08X", latchAddr));
            }
        } else {
        	log.warn(String.format("IGNORING: sceHprmPeekLatch 0x%08X", latchAddr));
        	hprmWarningLogged = true;
        }

        cpu.gpr[2] = 0;
    }

    @HLEFunction(nid = 0x40D2F9F0, version = 150)
    public void sceHprmReadLatch(Processor processor) {
        CpuState cpu = processor.cpu;

        int latchAddr = cpu.gpr[4];

        if (hprmWarningLogged) {
            if (log.isDebugEnabled()) {
            	log.debug(String.format("IGNORING: sceHprmReadLatch 0x%08X", latchAddr));
            }
        } else {
        	log.warn(String.format("IGNORING: sceHprmReadLatch 0x%08X", latchAddr));
        	hprmWarningLogged = true;
        }

        cpu.gpr[2] = 0;
    }

}