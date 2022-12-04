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
package jpcsp.test;

import static jpcsp.HLE.modules.sceSyscon.getSysconCmdName;
import static jpcsp.memory.mmio.syscon.MMIOHandlerSysconFirmwareSfr.getInterruptName;
import static jpcsp.memory.mmio.syscon.SysconBootloaderEmulator.SYSCON_BOOTLOADER_COMMAND_CHECKSUM_SECUREFLASH;
import static jpcsp.memory.mmio.syscon.SysconBootloaderEmulator.SYSCON_BOOTLOADER_COMMAND_CHECK_BLANK_SECUREFLASH;
import static jpcsp.memory.mmio.syscon.SysconBootloaderEmulator.SYSCON_BOOTLOADER_COMMAND_COMPARE_SECUREFLASH;
import static jpcsp.memory.mmio.syscon.SysconBootloaderEmulator.SYSCON_BOOTLOADER_COMMAND_ERASE_COMPLETE_SECUREFLASH;
import static jpcsp.memory.mmio.syscon.SysconBootloaderEmulator.SYSCON_BOOTLOADER_COMMAND_ERASE_SECUREFLASH;
import static jpcsp.memory.mmio.syscon.SysconBootloaderEmulator.SYSCON_BOOTLOADER_COMMAND_GET_VERSION;
import static jpcsp.memory.mmio.syscon.SysconBootloaderEmulator.SYSCON_BOOTLOADER_COMMAND_GET_INFO;
import static jpcsp.memory.mmio.syscon.SysconBootloaderEmulator.SYSCON_BOOTLOADER_COMMAND_INIT;
import static jpcsp.memory.mmio.syscon.SysconBootloaderEmulator.SYSCON_BOOTLOADER_COMMAND_READ_SECUREFLASH_0000_to_03FF;
import static jpcsp.memory.mmio.syscon.SysconBootloaderEmulator.SYSCON_BOOTLOADER_COMMAND_SET_CONFIG_FOR_DELAYS;
import static jpcsp.memory.mmio.syscon.SysconBootloaderEmulator.SYSCON_BOOTLOADER_COMMAND_SET_TIMEOUT;
import static jpcsp.memory.mmio.syscon.SysconBootloaderEmulator.SYSCON_BOOTLOADER_COMMAND_GET_INIT_ERROR;
import static jpcsp.memory.mmio.syscon.SysconBootloaderEmulator.SYSCON_BOOTLOADER_COMMAND_SET_SECURITY;
import static jpcsp.memory.mmio.syscon.SysconBootloaderEmulator.SYSCON_BOOTLOADER_COMMAND_WRITE_SECUREFLASH;
import static jpcsp.memory.mmio.syscon.SysconEmulator.firmwareBootloader;
import static jpcsp.nec78k0.Nec78k0Instructions.getBranchAddress;
import static jpcsp.nec78k0.Nec78k0Instructions.registerFunctionName;
import static jpcsp.util.Utilities.internalReadUnaligned16;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import jpcsp.Emulator;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.autotests.AutoTestsRunner;
import jpcsp.hardware.Battery;
import jpcsp.hardware.Model;
import jpcsp.hardware.Wlan;
import jpcsp.memory.mmio.battery.BatteryEmulator;
import jpcsp.memory.mmio.syscon.MMIOHandlerSyscon;
import jpcsp.memory.mmio.syscon.MMIOHandlerSysconFirmwareSfr;
import jpcsp.memory.mmio.syscon.SysconBootloaderEmulator;
import jpcsp.memory.mmio.syscon.SysconEmulator;
import jpcsp.memory.mmio.syscon.SysconMemory;
import jpcsp.nec78k0.Nec78k0Interpreter;
import jpcsp.nec78k0.Nec78k0Memory;
import jpcsp.nec78k0.Nec78k0Processor;
import jpcsp.nec78k0.sfr.Nec78k0SerialInterfaceUART6;
import jpcsp.util.LWJGLFixer;
import jpcsp.util.Utilities;

/**
 * @author gid15
 *
 */
public class Syscon78k0Test {
	public static Logger log;
	private static final int[] bootloaderTestCommands = {
			SYSCON_BOOTLOADER_COMMAND_INIT,
			SYSCON_BOOTLOADER_COMMAND_GET_INIT_ERROR,
			SYSCON_BOOTLOADER_COMMAND_GET_VERSION,
			SYSCON_BOOTLOADER_COMMAND_GET_INFO,
			SYSCON_BOOTLOADER_COMMAND_READ_SECUREFLASH_0000_to_03FF,
			-1,
			SYSCON_BOOTLOADER_COMMAND_SET_SECURITY,
			SYSCON_BOOTLOADER_COMMAND_SET_CONFIG_FOR_DELAYS,
			SYSCON_BOOTLOADER_COMMAND_SET_TIMEOUT,
			SYSCON_BOOTLOADER_COMMAND_CHECKSUM_SECUREFLASH,
			SYSCON_BOOTLOADER_COMMAND_COMPARE_SECUREFLASH,
			SYSCON_BOOTLOADER_COMMAND_WRITE_SECUREFLASH,
			SYSCON_BOOTLOADER_COMMAND_CHECK_BLANK_SECUREFLASH,
			SYSCON_BOOTLOADER_COMMAND_ERASE_SECUREFLASH,
			SYSCON_BOOTLOADER_COMMAND_ERASE_COMPLETE_SECUREFLASH,
			-1
	};

	private static class SerialInterfaceSimulatorThread extends Thread {
		private final Nec78k0Processor processor;
		private final Nec78k0SerialInterfaceUART6 serialInterface;
		private final SysconBootloaderEmulator bootloaderEmulator;

		public SerialInterfaceSimulatorThread(Nec78k0Processor processor) {
			this.processor = processor;
			serialInterface = processor.mem.getSfr().getSerialInterfaceUART6();
			bootloaderEmulator = (SysconBootloaderEmulator) serialInterface.getConnectedSerialInterface();
		}

		private int[] getAddressRangeParameters(int start, int end) {
			int[] data = new int[6];
			data[0] = (start >> 16) & 0xFF;
			data[1] = (start >>  8) & 0xFF;
			data[2] = (start >>  0) & 0xFF;
			data[3] = (  end >> 16) & 0xFF;
			data[4] = (  end >>  8) & 0xFF;
			data[5] = (  end >>  0) & 0xFF;

			return data;
		}

		@Override
		public void run() {
			RuntimeContext.setLog4jMDC();
			Utilities.sleep(100, 0);

			int bootloaderTestCommandIndex = 0;
			while (bootloaderTestCommandIndex < bootloaderTestCommands.length) {
				if (bootloaderEmulator.isEmpty()) {
					int bootloaderTestCommand = bootloaderTestCommands[bootloaderTestCommandIndex++];
					if (bootloaderTestCommand < 0) {
						break;
					}

					int data[];
					int startSecureFlashStartAddress = 0x000800;
					int endSecureFlashStartAddress = startSecureFlashStartAddress + 0x3FF;
					switch (bootloaderTestCommand) {
						case SYSCON_BOOTLOADER_COMMAND_INIT:
							bootloaderEmulator.setCommand(SYSCON_BOOTLOADER_COMMAND_INIT);
							break;
						case SYSCON_BOOTLOADER_COMMAND_GET_INIT_ERROR:
							bootloaderEmulator.setCommand(SYSCON_BOOTLOADER_COMMAND_GET_INIT_ERROR);
							break;
						case SYSCON_BOOTLOADER_COMMAND_READ_SECUREFLASH_0000_to_03FF:
							// Data parameters must be 0x00, 0x00
							data = new int[2];
							data[0] = 0x00;
							data[1] = 0x00;
							bootloaderEmulator.setCommand(SYSCON_BOOTLOADER_COMMAND_READ_SECUREFLASH_0000_to_03FF, data);
							break;
						case SYSCON_BOOTLOADER_COMMAND_CHECKSUM_SECUREFLASH:
							bootloaderEmulator.setCommand(SYSCON_BOOTLOADER_COMMAND_CHECKSUM_SECUREFLASH, getAddressRangeParameters(startSecureFlashStartAddress, endSecureFlashStartAddress));
							break;
						case SYSCON_BOOTLOADER_COMMAND_GET_VERSION:
							bootloaderEmulator.setCommand(SYSCON_BOOTLOADER_COMMAND_GET_VERSION);
							break;
						case SYSCON_BOOTLOADER_COMMAND_GET_INFO:
							bootloaderEmulator.setCommand(SYSCON_BOOTLOADER_COMMAND_GET_INFO);
							break;
						case SYSCON_BOOTLOADER_COMMAND_SET_CONFIG_FOR_DELAYS:
							bootloaderEmulator.setCommand(SYSCON_BOOTLOADER_COMMAND_SET_CONFIG_FOR_DELAYS);
							break;
						case SYSCON_BOOTLOADER_COMMAND_SET_TIMEOUT:
							int unknownValue = 0x04; // Must be 0x04 or 0x05
							int timeoutValue = 200;; // Must be >= 200 when unknownValue == 0x04, otherwise must be <= 200
							                         // Will be multiplied by 10000, seems to be a timeout
							data = new int[4];
							data[0] = (timeoutValue / 100) % 10;
							data[1] = (timeoutValue / 10) % 10;
							data[2] = timeoutValue % 10;
							data[3] = unknownValue;
							bootloaderEmulator.setCommand(SYSCON_BOOTLOADER_COMMAND_SET_TIMEOUT, data);
							break;
						case SYSCON_BOOTLOADER_COMMAND_SET_SECURITY:
							// Data parameters must be 0x00, 0x00
							data = new int[2];
							data[0] = 0x00;
							data[1] = 0x00;
							bootloaderEmulator.setCommand(SYSCON_BOOTLOADER_COMMAND_SET_SECURITY, data);
							data = new int[2];
							data[0] = 0x01 | 0x02 | 0x04 | 0x10; // Security flags
							data[1] = 0x03; // Must be 0x03
							bootloaderEmulator.setData(data, true);
							break;
						case SYSCON_BOOTLOADER_COMMAND_WRITE_SECUREFLASH:
							bootloaderEmulator.setCommand(SYSCON_BOOTLOADER_COMMAND_WRITE_SECUREFLASH, getAddressRangeParameters(startSecureFlashStartAddress, endSecureFlashStartAddress));
							// Send data packets of max. 0x100 bytes of data
							for (int n = startSecureFlashStartAddress; n <= endSecureFlashStartAddress; n += 0x100) {
								int length = Math.min(endSecureFlashStartAddress - n + 1, 0x100);
								data = new int[length];
								for (int i = 0; i < data.length; i++) {
									data[i] = (i + 1) & 0xFF;
								}
								bootloaderEmulator.setData(data, n + length > endSecureFlashStartAddress);
							}
							break;
						case SYSCON_BOOTLOADER_COMMAND_CHECK_BLANK_SECUREFLASH:
							bootloaderEmulator.setCommand(SYSCON_BOOTLOADER_COMMAND_CHECK_BLANK_SECUREFLASH, getAddressRangeParameters(startSecureFlashStartAddress, endSecureFlashStartAddress));
							break;
						case SYSCON_BOOTLOADER_COMMAND_COMPARE_SECUREFLASH:
							bootloaderEmulator.setCommand(SYSCON_BOOTLOADER_COMMAND_COMPARE_SECUREFLASH, getAddressRangeParameters(startSecureFlashStartAddress, endSecureFlashStartAddress));
							// Send data packets of max. 0x100 bytes of data
							for (int n = startSecureFlashStartAddress; n <= endSecureFlashStartAddress; n += 0x100) {
								int length = Math.min(endSecureFlashStartAddress - n + 1, 0x100);
								data = new int[length];
								for (int i = 0; i < data.length; i++) {
									data[i] = (i + 1) & 0xFF;
								}
								bootloaderEmulator.setData(data, n + length > endSecureFlashStartAddress);
							}
							break;
						case SYSCON_BOOTLOADER_COMMAND_ERASE_SECUREFLASH:
							bootloaderEmulator.setCommand(SYSCON_BOOTLOADER_COMMAND_ERASE_SECUREFLASH, getAddressRangeParameters(startSecureFlashStartAddress, endSecureFlashStartAddress));
							break;
						case SYSCON_BOOTLOADER_COMMAND_ERASE_COMPLETE_SECUREFLASH:
							bootloaderEmulator.setCommand(SYSCON_BOOTLOADER_COMMAND_ERASE_COMPLETE_SECUREFLASH);
							break;
						default:
							log.error(String.format("Unknown bootloader command 0x%02X", bootloaderTestCommand));
							break;
					}
				}

				if (serialInterface.isReceptionEnabled() && processor.getCurrentInstructionPc() == 0x0DE1) {
					serialInterface.asyncReceive();
				}

				Utilities.sleep(1, 0);
			}
		}
	}

	public static void main(String[] args) {
        LWJGLFixer.fixOnce();
        DOMConfigurator.configure("LogSettings.xml");
        log = SysconEmulator.log;
		RuntimeContext.setLog4jMDC();
		Wlan.initialize();
		Battery.initialize();
        new Emulator(new AutoTestsRunner.DummyGUI());
        Emulator.getClock().resume();
		RuntimeContext.debugCodeBlockCalls = true;
		Emulator.run = true;

		new Syscon78k0Test().testFirmware();
	}

	public void testFirmware() {
		int model = Model.MODEL_PSP_SLIM;
		model = Model.MODEL_PSP_STREET;
		model = Model.MODEL_PSP_BRITE2;

		Model.setModel(model);

		firmwareBootloader = false;
		Nec78k0Memory mem = new SysconMemory(log);
		Nec78k0Processor processor = new Nec78k0Processor(mem);
		Nec78k0Interpreter interpreter = new Nec78k0Interpreter(processor);

		SysconEmulator.load(mem);

		if (model == Model.MODEL_PSP_STREET) {
			// The below offsets and function names are taken from
			//     https://github.com/uofw/uofw/blob/master/src/syscon_firmware/firmware_ta096.c
			registerFunctionName(0x4CE0, "do_encrypt");
			registerFunctionName(0x4D02, "aes_key_expand");
			registerFunctionName(0x5011, "memcpy");
			registerFunctionName(0x5039, "memcmp");
			registerFunctionName(0x5076, "xorloop_0x10");
			registerFunctionName(0x50A7, "memset");
			registerFunctionName(0x50C4, "generate_challenge");
			registerFunctionName(0x5103, "final_key_encryption_cbc");
			registerFunctionName(0x55CF, "read_secure_flash");
		} else if (model == Model.MODEL_PSP_BRITE2) {
			registerFunctionName(0x0723, "mainLoop");
			registerFunctionName(0x524A, "do_encrypt");
			registerFunctionName(0x526C, "aes_key_expand");
			registerFunctionName(0x557B, "memcpy");
			registerFunctionName(0x55A3, "memcmp");
			registerFunctionName(0x55E0, "xorloop_0x10");
			registerFunctionName(0x5611, "memset");
			registerFunctionName(0x566D, "final_key_encryption_cbc");
			registerFunctionName(0x5A80, "check_if_service_or_autoboot_battery");
			registerFunctionName(0x5B42, "read_secure_flash");
			registerFunctionName(0x4CAD, "aes128_encrypt_key_0xFD3C_data_0xF5D0");
			registerFunctionName(0x4916, "aes128_encrypt_key_0xFCFA_data_0xFE20");
		}

		if (mem.internalRead32(0x8100) != 0xFFFFFFFF || firmwareBootloader) {
			final int baseAddress = firmwareBootloader ? 0x0000 : 0x8000;
			// Same offsets for all models
			registerFunctionName(baseAddress + 0x0EAD, "send_response_packet_02_and_03");
			registerFunctionName(baseAddress + 0x0EA4, "send_response_packet_02_and_17");
			registerFunctionName(baseAddress + 0x0EB6, "send_response_packet_02");
			registerFunctionName(baseAddress + 0x0DCA, "send_byte");
			registerFunctionName(baseAddress + 0x0DE1, "wait_for_byte_reception");
			registerFunctionName(baseAddress + 0x0DE8, "receive_packet_01");
			registerFunctionName(baseAddress + 0x0F26, "receive_packet_01_70_and_send_response_packets_02_and_03");
			registerFunctionName(baseAddress + 0x0FE9, "prepare_one_byte_for_response_packet_02");
			registerFunctionName(baseAddress + 0x082F, "get_status_from_FFC4");
			registerFunctionName(baseAddress + 0x0F6D, "check_status_success_for_response_packet_02");
			registerFunctionName(baseAddress + 0x0E48, "receive_packet_02");
			registerFunctionName(baseAddress + 0x0F93, "prepare_byte_STATUS_BUSY_for_response_packet_02");
			registerFunctionName(baseAddress + 0x0FA7, "prepare_byte_STATUS_PARAM_ERROR_for_response_packet_02");
			registerFunctionName(baseAddress + 0x1A1F, "store_0F_bytes_of_received_packet_to_FE26");
			registerFunctionName(baseAddress + 0x10BC, "multiply_32bits_by_8bits");
			registerFunctionName(baseAddress + 0x1E66, "read32_secure_flash");
			registerFunctionName(baseAddress + 0x1E27, "write32_secure_flash");
			registerFunctionName(baseAddress + 0x05DB, "set_secure_flash_operation_read_with_delay_FE2E");
			registerFunctionName(baseAddress + 0x05E7, "set_secure_flash_operation_read_with_delay_FE2F");
			registerFunctionName(baseAddress + 0x05FE, "set_secure_flash_operation_read_with_delay_variable");
			registerFunctionName(baseAddress + 0x05F3, "set_secure_flash_operation_read_with_delay_3");
			registerFunctionName(baseAddress + 0x0638, "set_secure_flash_operation_erase_with_delay_FE2C");
			registerFunctionName(baseAddress + 0x18BC, "set_secure_flash_operation_write_with_delay_FE28");
			registerFunctionName(baseAddress + 0x0617, "set_secure_flash_operation_verify_with_delay_FE2C");
			registerFunctionName(baseAddress + 0x0623, "set_secure_flash_operation_extended_read_with_delay_FE2C");
			registerFunctionName(baseAddress + 0x062F, "set_secure_flash_operation_extended_read_with_delay_FE2D");
			registerFunctionName(baseAddress + 0x0644, "set_secure_flash_operation_05_or_06_with_delay_FE2C");
			registerFunctionName(baseAddress + 0x0658, "set_secure_flash_operation_05_or_06_with_delay_FE2D");
			registerFunctionName(baseAddress + 0x0677, "clr1_FFC5_1");
			registerFunctionName(baseAddress + 0x0680, "set1_FFC5_1");
			registerFunctionName(baseAddress + 0x0689, "clr1_set1_FFC5_1");
			registerFunctionName(baseAddress + 0x0690, "set1_clr1_FFC5_1");
			registerFunctionName(baseAddress + 0x0697, "clr1_set1_clr1_FFC5_1");
			registerFunctionName(baseAddress + 0x06A1, "set1_clr1_set1_FFC5_1");
			registerFunctionName(baseAddress + 0x06F7, "set_FFCA_to_01");
			registerFunctionName(baseAddress + 0x0708, "set_FFCA_to_81");
			registerFunctionName(baseAddress + 0x0719, "set_FFCA_to_00");
			registerFunctionName(baseAddress + 0x072A, "set_FFCA_to_80");
			registerFunctionName(baseAddress + 0x08EE, "enable_interrupts");
			registerFunctionName(baseAddress + 0x08F6, "disable_interrupts");
			registerFunctionName(baseAddress + 0x12C5, "is_using_internal_8MHz_clock");
			registerFunctionName(baseAddress + 0x0669, "small_delay");
			registerFunctionName(baseAddress + 0x078D, "read32_secure_flash_0204_to_FE3A");
			registerFunctionName(baseAddress + 0x118F, "set_secure_flash_address_range_0400_to_07FF");
			registerFunctionName(baseAddress + 0x11B8, "increase_address_range");
			registerFunctionName(baseAddress + 0x074C, "set_secure_flash_address");
			registerFunctionName(baseAddress + 0x08B9, "set_secure_flash_writeData32_to_FFFFFFFF");
			registerFunctionName(baseAddress + 0x08DE, "set_secure_flash_writeData32");
			registerFunctionName(baseAddress + 0x0FDA, "set_packet_error_code_STATUS_MRG11_ERROR");
			registerFunctionName(baseAddress + 0x0FCB, "set_packet_error_code_STATUS_MRG10_ERROR");
			registerFunctionName(baseAddress + 0x0FAD, "set_packet_error_code_STATUS_PROTECT_ERROR");
			registerFunctionName(baseAddress + 0x1635, "secure_flash_erase_or_verify_address_range");
			registerFunctionName(baseAddress + 0x1167, "set_secure_flash_address_range_0000_to_max");
			registerFunctionName(baseAddress + 0x08C9, "copy_secure_flash_readData32_to_writeData32");
			registerFunctionName(baseAddress + 0x1917, "secure_flash_verify_address_range");
			registerFunctionName(baseAddress + 0x191A, "secure_flash_verify_address_range");
			registerFunctionName(baseAddress + 0x1116, "multiply_by_0400");
			registerFunctionName(baseAddress + 0x11E1, "receive_packet_02_and_process_secure_flash_write");
			registerFunctionName(baseAddress + 0x0886, "extract_address_range_from_request_packet");
			registerFunctionName(baseAddress + 0x0834, "validate_end_address_range");
			registerFunctionName(baseAddress + 0x0850, "validate_start_address_range_before_end");
			registerFunctionName(baseAddress + 0x086A, "validate_start_and_end_address_range_aligned_on_0400");
			registerFunctionName(baseAddress + 0x0759, "read16_secure_flash_0008_to_FE3F_and_FE41");
			registerFunctionName(baseAddress + 0x1242, "read16_secure_flash_0008_to_FE3F_and_FE41");
			registerFunctionName(baseAddress + 0x1481, "secure_flash_erase_address_range");
			registerFunctionName(baseAddress + 0x1227, "read32_secure_flash_to_DE");
			registerFunctionName(baseAddress + 0x1DE0, "erase_secure_flash_address_range_0400_to_7FFF");
		}

		for (int i = 0; i < 0x40; i += 2) {
			int addr = mem.internalRead16(i);
			if (addr != 0 && addr != 0xFFFF) {
				log.info(String.format("Disassembling Vector Table entry 0x%02X(%s): 0x%04X", i, getInterruptName(i), addr));
				processor.disassemble(addr);
			}
		}

		if (mem.internalRead32(0x8100) != 0xFFFFFFFF || firmwareBootloader) {
			final int baseAddress = firmwareBootloader ? 0x0000 : 0x8000;
			if (!firmwareBootloader) {
				for (int i = 0; i < 0x3C; i += 2) {
					int addr = mem.internalRead16(baseAddress + i) | baseAddress;
					if (addr != 0 && addr != 0xFFFF) {
						log.info(String.format("Disassembling Vector Table 0x%04X entry 0x%02X(%s): 0x%04X", baseAddress, i, getInterruptName(i), addr));
						processor.disassemble(addr);
					}
				}
			}

			for (int i = 0; i < 16; i++) {
				int addr = getBranchAddress(mem.internalRead16(baseAddress + 0x0502 + i * 2), baseAddress);
				log.info(String.format("Disassembling switch table from 0x%04X: case 0x%02X at 0x%04X", baseAddress + 0x095A, i, addr));
				processor.disassemble(addr);
			}

			processor.disassemble(baseAddress + 0x0100);
			for (int i = 0; i < 10; i++) {
				int value = mem.internalRead8(baseAddress + 0x1FD2 - i);
				int addr = getBranchAddress(internalReadUnaligned16(mem, baseAddress + 0x1FD3 + (10 - 1 - i) * 2), baseAddress);
				log.info(String.format("Disassembling switch table from 0x%04X: case 0x%02X at 0x%04X", baseAddress + 0x016D, value, addr));
				processor.disassemble(addr);
			}

			for (int i = 0; i < 14; i++) {
				int value = mem.internalRead8(baseAddress + 0x052F - i);
				int addr = internalReadUnaligned16(mem, baseAddress + 0x0530 + (14 - 1 - i) * 2);
				log.info(String.format("Disassembling switch table from 0x%04X: case 0x%02X at 0x%04X", baseAddress + 0x0A1F, value, addr));
				processor.disassemble(addr);
			}
		}

		if (!firmwareBootloader) {
			if (model == Model.MODEL_PSP_SLIM) {
				// The below offsets are taken from the syscon firmware on the TA-085 motherboard
				for (int i = 0x88; i < 0xAE; i += 2) {
					int addr = mem.internalRead16(i);
					if (addr != 0 && addr != 0xFFFF) {
						log.info(String.format("Disassembling sysconCmdGetOps table 0x%02X(%s): 0x%04X", i, getSysconCmdName((i - 0x88) / 2), addr));
						processor.disassemble(addr);
					}
				}
				for (int i = 0xAE; i < 0xDC; i += 2) {
					int addr = mem.internalRead16(i);
					if (addr != 0 && addr != 0xFFFF) {
						log.info(String.format("Disassembling mainOperations table 0x%02X(%s): 0x%04X", i, getSysconCmdName((i - 0xAE) / 2 + 0x20), addr));
						processor.disassemble(addr);
					}
				}
				for (int i = 0xDC; i < 0x10A; i += 2) {
					int addr = mem.internalRead16(i);
					if (addr != 0 && addr != 0xFFFF) {
						log.info(String.format("Disassembling peripheralOperations table 0x%02X(%s): 0x%04X", i, getSysconCmdName((i - 0xDC) / 2 + 0x40), addr));
						processor.disassemble(addr);
					}
				}
				for (int i = 2; i <= 13; i++) {
					int addr = mem.internalRead16(0x4DA8 + (i - 2) * 2);
					log.info(String.format("Disassembling switch table from 0x4DA6: case 0x%02X at 0x%04X", i, addr));
					processor.disassemble(addr);
				}
				for (int i = 1; i <= 20; i++) {
					int addr = mem.internalRead16(0x3264 + (i - 1) * 2);
					log.info(String.format("Disassembling switch table from 0x3262: case 0x%02X at 0x%04X", i, addr));
					processor.disassemble(addr);
				}
			} else if (model == Model.MODEL_PSP_FAT) {
				// The below offsets are taken from the syscon firmware on the TA-086 motherboard
				for (int i = 0x88; i < 0xAE; i += 2) {
					int addr = mem.internalRead16(i);
					if (addr != 0 && addr != 0xFFFF) {
						log.info(String.format("Disassembling sysconCmdGetOps table 0x%02X(%s): 0x%04X", i, getSysconCmdName((i - 0x88) / 2), addr));
						processor.disassemble(addr);
					}
				}
				for (int i = 0xAC; i < 0xDA; i += 2) {
					int addr = mem.internalRead16(i);
					if (addr != 0 && addr != 0xFFFF) {
						log.info(String.format("Disassembling mainOperations table 0x%02X(%s): 0x%04X", i, getSysconCmdName((i - 0xAE) / 2 + 0x20), addr));
						processor.disassemble(addr);
					}
				}
				for (int i = 0xDA; i < 0x108; i += 2) {
					int addr = mem.internalRead16(i);
					if (addr != 0 && addr != 0xFFFF) {
						log.info(String.format("Disassembling peripheralOperations table 0x%02X(%s): 0x%04X", i, getSysconCmdName((i - 0xDC) / 2 + 0x40), addr));
						processor.disassemble(addr);
					}
				}
				for (int i = 1; i <= 12; i++) {
					int addr = mem.internalRead16(0x25C2 + (i - 1) * 2);
					log.info(String.format("Disassembling switch table from 0x25C0: case 0x%02X at 0x%04X", i, addr));
					processor.disassemble(addr);
				}
			} else if (model == Model.MODEL_PSP_BRITE) {
				// The below offsets are taken from the syscon firmware on the TA-090 motherboard
				for (int i = 2; i <= 13; i++) {
					int addr = internalReadUnaligned16(mem, 0x4DD1 + (i - 2) * 2);
					log.info(String.format("Disassembling switch table from 0x4DCF: case 0x%02X at 0x%04X", i, addr));
					processor.disassemble(addr);
				}
			} else if (model == Model.MODEL_PSP_STREET) {
				// The below offsets are taken from the syscon firmware on the TA-096 motherboard
				for (int i = 0x8A; i < 0xB0; i += 2) {
					int addr = mem.internalRead16(i);
					if (addr != 0 && addr != 0xFFFF) {
						log.info(String.format("Disassembling sysconCmdGetOps table 0x%02X(%s): 0x%04X", i, getSysconCmdName((i - 0x8A) / 2), addr));
						processor.disassemble(addr);
					}
				}
				for (int i = 0xB0; i < 0xDE; i += 2) {
					int addr = mem.internalRead16(i);
					if (addr != 0 && addr != 0xFFFF) {
						log.info(String.format("Disassembling mainOperations table 0x%02X(%s): 0x%04X", i, getSysconCmdName((i - 0xB0) / 2 + 0x20), addr));
						processor.disassemble(addr);
					}
				}
				for (int i = 0xE0; i < 0x10E; i += 2) {
					int addr = mem.internalRead16(i);
					if (addr != 0 && addr != 0xFFFF) {
						log.info(String.format("Disassembling peripheralOperations table 0x%02X(%s): 0x%04X", i, getSysconCmdName((i - 0xE0) / 2 + 0x40), addr));
						processor.disassemble(addr);
					}
				}

				for (int i = 1; i <= 21; i++) {
					int addr = mem.internalRead16(0x277A + (i - 1) * 2);
					log.info(String.format("Disassembling switch table from 0x2778: case 0x%02X at 0x%04X", i, addr));
					processor.disassemble(addr);
				}
				for (int i = 2; i <= 13; i++) {
					int addr = internalReadUnaligned16(mem, 0x51BA + (i - 2) * 2);
					log.info(String.format("Disassembling switch table from 0x51B8: case 0x%02X at 0x%04X", i, addr));
					processor.disassemble(addr);
				}
			}
		}

		SysconEmulator.disable();
		MMIOHandlerSyscon.getInstance().init(mem.getSfr());
		MMIOHandlerSysconFirmwareSfr.dummyTesting = true;
		Nec78k0Processor.disassembleFunctions = true;

		if (BatteryEmulator.isEnabled()) {
			// Wait for the battery firmware to boot
			Utilities.sleep(1000, 0);
		}

		if (firmwareBootloader) {
			SerialInterfaceSimulatorThread serialInterfaceSimulatorThread = new SerialInterfaceSimulatorThread(processor);
			serialInterfaceSimulatorThread.setName("Serial Interface Simulator Thread");
			serialInterfaceSimulatorThread.setDaemon(true);
			serialInterfaceSimulatorThread.start();
		}
		processor.reset();
		interpreter.run();

		long minimumDuration = 4000L; // Run for at least 4 seconds
		long start = now();
		while ((now() - start) < minimumDuration && !Emulator.pause) {
			interpreter.run();
		}
	}

	public static long now() {
		return Emulator.getClock().currentTimeMillis();
	}
}
