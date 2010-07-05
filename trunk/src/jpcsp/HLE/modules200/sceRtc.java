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

package jpcsp.HLE.modules200;

import java.util.Calendar;

import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.ScePspDateTime;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

public class sceRtc extends jpcsp.HLE.modules150.sceRtc {	
	@Override
	public void installModule(HLEModuleManager mm, int version) {
		super.installModule(mm, version);
		
		if (version >= 200) {
		
			mm.addFunction(sceRtcGetAccumlativeTimeFunction, 0x029CA3B3);
			mm.addFunction(sceRtcGetLastReincarnatedTimeFunction, 0x203CEB0D);
			mm.addFunction(sceRtcGetLastAdjustedTimeFunction, 0x62685E98);
			mm.addFunction(sceRtcSetTime64_tFunction, 0x1909C99B);
			mm.addFunction(sceRtcGetTime64_tFunction, 0xE1C93E47);
			
		}
	}
	
	@Override
	public void uninstallModule(HLEModuleManager mm, int version) {
		super.uninstallModule(mm, version);
		
		if (version >= 200) {
		
			mm.removeFunction(sceRtcGetAccumlativeTimeFunction);
			mm.removeFunction(sceRtcGetLastReincarnatedTimeFunction);
			mm.removeFunction(sceRtcGetLastAdjustedTimeFunction);
			mm.removeFunction(sceRtcSetTime64_tFunction);
			mm.removeFunction(sceRtcGetTime64_tFunction);
			
		}
	}
	
	public void sceRtcGetAccumlativeTime(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceRtcGetAccumlativeTime [0x029CA3B3]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceRtcGetLastReincarnatedTime(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceRtcGetLastReincarnatedTime [0x203CEB0D]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceRtcGetLastAdjustedTime(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceRtcGetLastAdjustedTime [0x62685E98]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
       
	public void sceRtcSetTime64_t(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceRtcSetTime64_t [0x1909C99B]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
        
	public void sceRtcGetTime64_t(Processor processor) {
		CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int date_addr = cpu.gpr[4];
        int time_addr = cpu.gpr[5];

        if (mem.isAddressGood(date_addr) && mem.isAddressGood(time_addr)) {
            ScePspDateTime dateTime = new ScePspDateTime();
            dateTime.read(mem, date_addr);
            Calendar cal = Calendar.getInstance();
            cal.set(dateTime.year, dateTime.month - 1, dateTime.day, dateTime.hour, dateTime.minute, dateTime.second);
            long unixtime = (long)(cal.getTime().getTime() / 1000L);
            Modules.log.debug("sceRtcGetTime_t psptime:" + dateTime + " unixtime:" + unixtime);
            mem.write64(time_addr, unixtime);
            cpu.gpr[2] = 0;
        } else {
            Modules.log.warn("sceRtcGetTime_t bad address " + String.format("0x%08X 0x%08X", date_addr, time_addr));
            cpu.gpr[2] = -1;
        }
	}
	
	public final HLEModuleFunction sceRtcGetAccumlativeTimeFunction = new HLEModuleFunction("sceRtc", "sceRtcGetAccumlativeTime") {
		@Override
		public final void execute(Processor processor) {
			sceRtcGetAccumlativeTime(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceRtcModule.sceRtcGetAccumlativeTime(processor);";
		}
	};
    
	public final HLEModuleFunction sceRtcGetLastReincarnatedTimeFunction = new HLEModuleFunction("sceRtc", "sceRtcGetLastReincarnatedTime") {
		@Override
		public final void execute(Processor processor) {
			sceRtcGetLastReincarnatedTime(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceRtcModule.sceRtcGetLastReincarnatedTime(processor);";
		}
	};
    
	public final HLEModuleFunction sceRtcGetLastAdjustedTimeFunction = new HLEModuleFunction("sceRtc", "sceRtcGetLastAdjustedTime") {
		@Override
		public final void execute(Processor processor) {
			sceRtcGetLastAdjustedTime(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceRtcModule.sceRtcGetLastAdjustedTime(processor);";
		}
	};

	public final HLEModuleFunction sceRtcSetTime64_tFunction = new HLEModuleFunction("sceRtc", "sceRtcSetTime64_t") {
		@Override
		public final void execute(Processor processor) {
			sceRtcSetTime64_t(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceRtcModule.sceRtcSetTime64_t(processor);";
		}
	};
    
	public final HLEModuleFunction sceRtcGetTime64_tFunction = new HLEModuleFunction("sceRtc", "sceRtcGetTime64_t") {
		@Override
		public final void execute(Processor processor) {
			sceRtcGetTime64_t(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceRtcModule.sceRtcGetTime64_t(processor);";
		}
	};    
};