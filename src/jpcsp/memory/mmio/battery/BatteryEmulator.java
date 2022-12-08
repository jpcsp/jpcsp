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
package jpcsp.memory.mmio.battery;

import static jpcsp.memory.mmio.syscon.SysconBatteryEmulator.challenge1secret10;
import static jpcsp.memory.mmio.syscon.SysconBatteryEmulator.challenge1secret13;
import static jpcsp.memory.mmio.syscon.SysconBatteryEmulator.challenge1secret8;
import static jpcsp.memory.mmio.syscon.SysconBatteryEmulator.challenge2secret10;
import static jpcsp.memory.mmio.syscon.SysconBatteryEmulator.challenge2secret13;
import static jpcsp.memory.mmio.syscon.SysconBatteryEmulator.challenge2secret8;
import static jpcsp.nec78k0.Nec78k0Instructions.registerFunctionName;
import static jpcsp.nec78k0.Nec78k0Memory.BASE_RAM0;
import static jpcsp.nec78k0.Nec78k0Memory.END_RAM0;
import static jpcsp.nec78k0.Nec78k0Processor.REG_PAIR_AX;
import static jpcsp.nec78k0.Nec78k0Processor.RESET;
import static jpcsp.nec78k0.sfr.Nec78k0Sfr.INTAD;
import static jpcsp.nec78k0.sfr.Nec78k0Sfr.INTP2;
import static jpcsp.nec78k0.sfr.Nec78k0Sfr.INTSR6;
import static jpcsp.nec78k0.sfr.Nec78k0Sfr.INTST6;
import static jpcsp.nec78k0.sfr.Nec78k0Sfr.INTTM000;
import static jpcsp.nec78k0.sfr.Nec78k0Sfr.INTTMH0;
import static jpcsp.nec78k0.sfr.Nec78k0Sfr.INTTMH1;
import static jpcsp.util.Utilities.KB;
import static jpcsp.util.Utilities.readUnaligned32;
import static jpcsp.util.Utilities.writeUnaligned16;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.HLE.TPointer;
import jpcsp.hardware.Battery;
import jpcsp.hardware.Model;
import jpcsp.memory.mmio.syscon.SysconEmulator;
import jpcsp.nec78k0.INec78k0HLECall;
import jpcsp.nec78k0.Nec78k0Interpreter;
import jpcsp.nec78k0.Nec78k0Memory;
import jpcsp.nec78k0.Nec78k0Processor;
import jpcsp.nec78k0.sfr.Nec78k0Sfr;
import jpcsp.util.Utilities;

/**
 * @author gid15
 *
 */
public class BatteryEmulator {
	public static Logger log = Logger.getLogger("battery");
	public static final int BATTERY_MODEL_FAT = 0;
	public static final int BATTERY_MODEL_2000 = 1;
	public static final int BATTERY_MODEL_3000 = 2;
	public static final int BATTERY_MODEL_STREET = 3;
	public static final int BATTERY_MODEL_GO = 4;
	private static final int HLE_ADDR1 = 0xFEB8;
	private static final int HLE_ADDR2 = 0xFEBC;
	private static int batteryModel = -1;
	private static boolean isEnabled;
	private static int initializedModel = -1;
	private final BatteryMemory mem;
	private final Nec78k0Processor processor;
	private final Nec78k0Interpreter interpreter;
	private BatteryProcessorThread thread;
	private volatile boolean exit;

	private static class HLE_s32_to_s16 implements INec78k0HLECall {
		@Override
		public void call(Nec78k0Processor processor, int insn) {
			int value1 = processor.mem.read32(HLE_ADDR1);
			int result = (short) value1;
			if (log.isDebugEnabled()) {
				log.debug(String.format("HLE_s32_to_s16 0x%08X = 0x%04X", value1, result));
			}

			processor.mem.write16(HLE_ADDR1, (short) result);
		}
	}

	private static class HLE_mul16 implements INec78k0HLECall {
		@Override
		public void call(Nec78k0Processor processor, int insn) {
			int value1 = processor.mem.read16(HLE_ADDR1);
			int value2 = processor.getRegisterPair(REG_PAIR_AX);
			int result = (value1 * value2) & 0xFFFF;
			if (log.isDebugEnabled()) {
				log.debug(String.format("HLE_mul16 0x%04X * 0x%04X = 0x%04X", value1, value2, result));
			}

			processor.setRegisterPair(REG_PAIR_AX, result);;
		}
	}

	private static class HLE_mul32 implements INec78k0HLECall {
		@Override
		public void call(Nec78k0Processor processor, int insn) {
			int value1 = processor.mem.read32(HLE_ADDR1);
			int value2 = processor.mem.read32(HLE_ADDR2) | (processor.getRegisterPair(REG_PAIR_AX) << 16);
			int result = value1 * value2;
			if (log.isDebugEnabled()) {
				log.debug(String.format("HLE_mul32 0x%08X * 0x%08X = 0x%08X", value1, value2, result));
			}

			processor.mem.write32(HLE_ADDR1, result);
		}
	}

	private static class HLE_divs16 implements INec78k0HLECall {
		@Override
		public void call(Nec78k0Processor processor, int insn) {
			int value1 = (short) processor.mem.read16(HLE_ADDR1);
			int value2 = (short) processor.getRegisterPair(REG_PAIR_AX);
			int result = value2 == 0 ? value1 : value1 / value2;
			if (log.isDebugEnabled()) {
				log.debug(String.format("HLE_divs16 0x%04X / 0x%04X = 0x%04X", value1, value2, result));
			}

			processor.setRegisterPair(REG_PAIR_AX, result);;
		}
	}

	private static class HLE_div32 implements INec78k0HLECall {
		@Override
		public void call(Nec78k0Processor processor, int insn) {
			int value1 = processor.mem.read32(HLE_ADDR1);
			int value2 = processor.mem.read16(HLE_ADDR2) | (processor.getRegisterPair(REG_PAIR_AX) << 16);
			int result = value2 == 0 ? value1 : value1 / value2;
			if (log.isDebugEnabled()) {
				log.debug(String.format("HLE_div32 0x%08X / 0x%08X = 0x%08X", value1, value2, result));
			}

			processor.mem.write32(HLE_ADDR1, result);
		}
	}

	private static class HLE_div_rem implements INec78k0HLECall {
		@Override
		public void call(Nec78k0Processor processor, int insn) {
			int value1 = processor.mem.read32(HLE_ADDR1);
			int value2 = processor.mem.read16(HLE_ADDR2) | (processor.getRegisterPair(REG_PAIR_AX) << 16);
			int quotient16 = (short) (value2 == 0 ? value1 : value1 / value2);
			int remainder16 = (short) (value2 == 0 ? value1 : value1 % value2);
			if (log.isDebugEnabled()) {
				log.debug(String.format("Unknown03BD div_rem 0x%08X / 0x%08X = 0x%04X, remainder 0x%04X", value1, value2, quotient16, remainder16));
			}

			processor.mem.write16(HLE_ADDR1, (short) quotient16);
			processor.mem.write16(HLE_ADDR1 + 2, (short) remainder16);
		}
	}

	private class BatteryProcessorThread extends Thread {
		@Override
		public void run() {
			RuntimeContext.setLog4jMDC();
			load(mem, interpreter);
			processor.reset();

			while (!Emulator.pause && !exit) {
				interpreter.setHalted(false);
				interpreter.run();
			}
			thread = null;
		}
	}

	public static int getBatteryModel() {
		if (batteryModel < 0) {
			switch (Model.getModel()) {
				case Model.MODEL_PSP_FAT:
					batteryModel = BATTERY_MODEL_FAT;
					break;
				case Model.MODEL_PSP_SLIM:
					batteryModel = BATTERY_MODEL_2000;
					break;
				case Model.MODEL_PSP_BRITE:
				case Model.MODEL_PSP_BRITE2:
				case Model.MODEL_PSP_BRITE3:
				case Model.MODEL_PSP_BRITE4:
					batteryModel = BATTERY_MODEL_3000;
					break;
				case Model.MODEL_PSP_STREET:
					batteryModel = BATTERY_MODEL_STREET;
					break;
				case Model.MODEL_PSP_GO:
					batteryModel = BATTERY_MODEL_GO;
					break;
				default:
					log.error(String.format("Unknown Battery model for PSP model %s", Model.getModelName()));
					batteryModel = BATTERY_MODEL_2000;
					break;
			}
		}

		return batteryModel;
	}

	public static String getFirmwareFileName() {
		switch (getBatteryModel()) {
			case BATTERY_MODEL_2000: return "Battery-2000.bin";
			case BATTERY_MODEL_3000: return "Battery-3000.bin";
		}

		return null;
	}

	public static boolean isEnabled() {
		// Check for the firmware file only if the PSP model has changed since last call
		if (initializedModel != Model.getModel()) {
			String firmwareFileName = getFirmwareFileName();
			if (firmwareFileName == null) {
				isEnabled = false;
			} else {
				File firmwareFile = new File(firmwareFileName);
				// The firmware file must be at least 16KB
				if (firmwareFile.canRead() && firmwareFile.length() >= 16 * KB) {
					isEnabled = true;
				} else {
					isEnabled = false;
				}
			}

			initializedModel = Model.getModel();
		}

		return isEnabled;
	}

	public BatteryEmulator() {
		mem = new BatteryMemory(log);
		processor = new Nec78k0Processor(mem);
		interpreter = new Nec78k0Interpreter(processor);
	}

	public MMIOHandlerBatteryFirmwareSfr getBatterySfr() {
		return (MMIOHandlerBatteryFirmwareSfr) mem.getSfr();
	}

	public static void load(BatteryMemory mem, Nec78k0Interpreter interpreter) {
		String firmwareFileName = getFirmwareFileName();
		log.info(String.format("Loading the Battery firmware from %s for %s", firmwareFileName, Model.getModelName()));
		File inputFile = new File(firmwareFileName);
		int length = (int) inputFile.length();
		byte[] buffer = new byte[length];
		try {
			FileInputStream in = new FileInputStream(inputFile);
			length = in.read(buffer);
			in.close();
		} catch (IOException e) {
			log.error(e);
		}

		int baseAddress = BASE_RAM0;
		length = Math.min(length, END_RAM0);
		for (int i = 0; i < length; i += 4) {
			mem.write32(baseAddress + i, readUnaligned32(buffer, i));
		}

		SysconEmulator.loadBootloader(mem);

		// Because the battery dumps are missing the area 0x0000-0x03FF,
		// we need to patch it by trying to guess the missing data
		patchFirmware(mem, interpreter);
	}

	private static void patchCallt(BatteryMemory mem, int callt, int address, String name) {
		mem.setRamKnown(callt, 2);
		mem.write16(callt, (short) address);
		registerFunctionName(callt, name);
	}

	private static void patchInterrupt(BatteryMemory mem, int vectorTableAddress, int address) {
		mem.setRamKnown(vectorTableAddress, 2);
		mem.write16(vectorTableAddress, (short) address);
		registerFunctionName(address, Nec78k0Sfr.getInterruptName(vectorTableAddress));
	}

	private static void store(Nec78k0Memory mem, int addr, int[] data) {
		for (int i = 0; i < data.length; i++) {
			mem.write8(addr + i, (byte) data[i]);
		}
	}

	private static void patchFirmware(BatteryMemory mem, Nec78k0Interpreter interpreter) {
		// For callt instructions, common to all battery models
		patchCallt(mem, 0x0040, 0x0515, "allocStack");
		patchCallt(mem, 0x0042, 0x0524, "freeStack");
		patchCallt(mem, 0x0044, 0x0531, "add16");
		patchCallt(mem, 0x0046, 0x0538, "zero8");
		patchCallt(mem, 0x0048, 0x053B, "one8");
		patchCallt(mem, 0x004A, 0x053E, "u8_to_u16");
		patchCallt(mem, 0x004C, 0x0541, "zero16");
		patchCallt(mem, 0x004E, 0x0545, "u16_to_u32");
		patchCallt(mem, 0x0050, 0x0549, "one16");
		patchCallt(mem, 0x0052, 0x054D, "s8_to_s16");
		patchCallt(mem, 0x0054, 0x0552, "read16");
		patchCallt(mem, 0x0056, 0x0557, "write16");
		patchCallt(mem, 0x0058, 0x055D, "read32");
		patchCallt(mem, 0x005A, 0x056A, "read32_to_FEB8");
		patchCallt(mem, 0x005C, 0x0578, "read32_to_FEBC");
		patchCallt(mem, 0x005E, 0x0584, "write32");
		patchCallt(mem, 0x0060, 0x0592, "write32_from_FEB8");
		patchCallt(mem, 0x0062, 0x05A5, "isZero8");
		patchCallt(mem, 0x0064, 0x05A8, "isOne8");
		patchCallt(mem, 0x0066, 0x05AB, "isZeroC");
		patchCallt(mem, 0x0068, 0x05AE, "incr8");
		patchCallt(mem, 0x006A, 0x05B2, "decr8");

		registerFunctionName(0x042E, "div16_FEB8");
		registerFunctionName(0x0493, "add32_FEB8_FEBC");
		registerFunctionName(0x04A8, "sub32_FEB8_FEBC");
		registerFunctionName(0x04DF, "ror32_FEB8");
		registerFunctionName(0x04C1, "rol32_FEB8");
		registerFunctionName(0x04FE, "cmp16_FEB8");
		registerFunctionName(0x05B6, "memcpy");
		registerFunctionName(0x0CDB, "readSeedFromSecureFlash");

		// Unknown functions stored in region not yet dumped: 0x0000-0x03FF
		interpreter.registerHLECall(0x0276, new HLE_s32_to_s16());
		interpreter.registerHLECall(0x028F, new HLE_mul16());
		interpreter.registerHLECall(0x02AC, new HLE_mul32());
		interpreter.registerHLECall(0x02FB, new HLE_divs16());
		interpreter.registerHLECall(0x0363, new HLE_div32());
		interpreter.registerHLECall(0x03BD, new HLE_div_rem());

		// Some data
		mem.setRamKnown(0x0204, 17);
		new TPointer(mem, 0x0204).setStringNZ(17, "SonyEnergyDevices");

		// Table with 100 entries (in decreasing order: 0x0194 - 0x00CE)
		mem.setRamKnown(0x00CE, 100 * 2);
		mem.write16(0x0194, (short) 0x1000);
		mem.write16(0x0192, (short) 0x0800);

		// Table with 16 entries (0x0196 - 0x01B4)
		mem.setRamKnown(0x0196, 16 * 2);
		mem.write16(0x0196, (short) 0x0100);
		mem.write16(0x0198, (short) 0x0010);

		// Table with 16 entries (0x01B6 - 0x01D4)
		mem.setRamKnown(0x01B6, 16 * 2);
		mem.write16(0x01B6, (short) 0x0010);

		mem.getSfr().getSecureFlash().write8(0x07F0 + 0, challenge1secret8[0]);
		mem.getSfr().getSecureFlash().write8(0x07F0 + 2, challenge1secret10[0]);
		mem.getSfr().getSecureFlash().write8(0x07F0 + 5, challenge1secret13[0]);
		int baseChallenge1SecretAddr = 0;
		int baseChallenge2SecretAddr = 0;
		if (batteryModel == BATTERY_MODEL_3000) {
			baseChallenge1SecretAddr = 0xFCCE;
			baseChallenge2SecretAddr = 0x01D4;
		} else if (batteryModel == BATTERY_MODEL_2000) {
			baseChallenge1SecretAddr = 0xFD04;
			baseChallenge2SecretAddr = 0x01D4;
		}
		if (baseChallenge1SecretAddr != 0) {
			mem.setRamKnown(baseChallenge1SecretAddr, 8 * 8);
			store(mem, baseChallenge1SecretAddr + 0 * 8, challenge1secret8);
			store(mem, baseChallenge1SecretAddr + 2 * 8, challenge1secret10);
			store(mem, baseChallenge1SecretAddr + 5 * 8, challenge1secret13);
		}
		if (baseChallenge2SecretAddr != 0) {
			mem.setRamKnown(baseChallenge2SecretAddr, 8 * 8);
			store(mem, baseChallenge2SecretAddr + 0 * 8, challenge2secret8);
			store(mem, baseChallenge2SecretAddr + 2 * 8, challenge2secret10);
			store(mem, baseChallenge2SecretAddr + 5 * 8, challenge2secret13);
		}

		int mainLoopAddr = -1;
		int bootAddr = -1;
		if (batteryModel == BATTERY_MODEL_3000) {
			registerFunctionName(0x0623, "memset");
			registerFunctionName(0x307B, "processCommandReceivedFromSyscon");
			registerFunctionName(0x2AA5, "readBatteryEEPROMWithRetry");
			registerFunctionName(0x2931, "EEPROM_tickClock");
			registerFunctionName(0x2995, "EEPROM_send8bits");
			registerFunctionName(0x29CA, "EEPROM_receive16bits");
			registerFunctionName(0x293A, "EEPROM_EraseWriteEnable");
			registerFunctionName(0x2970, "EEPROM_EraseWriteDisable");
			registerFunctionName(0x2A4A, "EEPROM_Write");
			registerFunctionName(0x2A0C, "EEPROM_Read");
			registerFunctionName(0x2B28, "mainLoop");
			registerFunctionName(0x2EF1, "readAD");

			// For interrupts
			patchInterrupt(mem, INTSR6, 0x3D8B);
			patchInterrupt(mem, INTST6, 0x3EDC);
			patchInterrupt(mem, INTAD, 0x2F3D);
			patchInterrupt(mem, INTP2, 0x2FCF);
			// INTTM000 needs to clear 0xFC7E for the UART6 transmission (wait loop at 0x3048-0x304C): candidates are 0x3F19
			patchInterrupt(mem, INTTM000, 0x3F19);
			// INTTMH0 or INTTM000 need to increment 0xFC3C for main loop (0x2C0A: test in 0x2C93), candidates are 0x2EB1, 0x2FBE
			patchInterrupt(mem, INTTMH0, 0x2EB1);
			patchInterrupt(mem, INTTMH1, 0x2FBE);

			mainLoopAddr = 0x2B28;
			bootAddr = 0x3F68;
		} else if (batteryModel == BATTERY_MODEL_2000) {
			registerFunctionName(0x305C, "processCommandReceivedFromSyscon");
			registerFunctionName(0x2A8A, "readBatteryEEPROMWithRetry");
			registerFunctionName(0x2916, "EEPROM_tickClock");
			registerFunctionName(0x297A, "EEPROM_send8bits");
			registerFunctionName(0x29AF, "EEPROM_receive16bits");
			registerFunctionName(0x291F, "EEPROM_EraseWriteEnable");
			registerFunctionName(0x2955, "EEPROM_EraseWriteDisable");
			registerFunctionName(0x2A2F, "EEPROM_Write");
			registerFunctionName(0x29F1, "EEPROM_Read");
			registerFunctionName(0x2B0D, "mainLoop");
			registerFunctionName(0x2ED2, "readAD");

			// For interrupts
			patchInterrupt(mem, INTSR6, 0x3D51);
			patchInterrupt(mem, INTST6, 0x3EA2);
			patchInterrupt(mem, INTAD, 0x2F1E);
			patchInterrupt(mem, INTP2, 0x2FB0);
			patchInterrupt(mem, INTTM000, 0x3EDF);
			patchInterrupt(mem, INTTMH0, 0x2E92);
			patchInterrupt(mem, INTTMH1, 0x2F9F);

			mainLoopAddr = 0x2B0D;
			bootAddr = 0x3F2E;
		} else {
			log.error(String.format("Unsupported Battery model %d", batteryModel));
		}

		// Build dummy code due to boot
		final int resetAddr = 0x7F00;
		final int spAddr = 0x7E00;
		patchInterrupt(mem, RESET, resetAddr);

		int addr = resetAddr;
		// movw SP, spAddr
		writeUnaligned16(mem, addr, 0x1CEE); // Opcode for "movw SP" instruction
		addr += 2;
		writeUnaligned16(mem, addr, spAddr);
		addr += 2;
		// call bootAddr
		mem.write8(addr, (byte) 0x9A); // Opcode for "call" instruction
		addr++;
		writeUnaligned16(mem, addr, bootAddr);
		addr += 2;
		// halt
		writeUnaligned16(mem, addr, 0x0071); // Opcode for "halt" instruction
		addr += 2;
		// call mainLoopAddr
		mem.write8(addr, (byte) 0x9A); // Opcode for "call" instruction
		addr++;
		writeUnaligned16(mem, addr, mainLoopAddr);
		addr += 2;
		// ret
		mem.write8(addr, (byte) 0xAF); // Opcode for "ret" instrunction
		addr++;
	}

	public void boot() {
		if (thread == null) {
			thread = new BatteryProcessorThread();
			thread.setName("Battery NEC 78k0 Processor Thread");
			thread.setDaemon(true);
			thread.start();
		} else {
			log.error(String.format("BatteryEmulator.boot() BatteryProcessorThread already running"));
		}
	}

	public void exit() {
		exit = true;
		interpreter.exitInterpreter();

		while (thread != null) {
			Utilities.sleep(1, 0);
		}
	}

	public static int readEeprom16(int address) {
		int value = (Battery.readEeprom(address << 1) << 8) | Battery.readEeprom((address << 1) + 1);

		if (log.isDebugEnabled()) {
			log.debug(String.format("EEPROM Read address=0x%02X: 0x%04X", address, value));
		}

		return value;
	}

	public static void writeEeprom16(int address, int value) {
		value &= 0xFFFF;

		if (log.isDebugEnabled()) {
			log.debug(String.format("EEPROM Write address=0x%02X: 0x%04X", address, value));
		}

		Battery.writeEeprom((address << 1) + 0, value >> 8);
		Battery.writeEeprom((address << 1) + 1, value);
	}
}
