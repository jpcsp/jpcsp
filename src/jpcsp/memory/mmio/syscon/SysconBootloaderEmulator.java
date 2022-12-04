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
package jpcsp.memory.mmio.syscon;

import java.io.IOException;

import jpcsp.nec78k0.sfr.Nec78k0SerialInterface;
import jpcsp.nec78k0.sfr.Nec78k0SerialInterfaceUART6;
import jpcsp.nec78k0.sfr.Nec78k0Sfr;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

/**
 * During the execution of the bootloader, the syscon firmware is
 * communicating with an external system using a serial interface.
 *
 * Based on some values found in
 *    https://github.com/janvdherrewegen/bootl-attacks/blob/main/78k0/glitching/renesas_fpi.py
 * 
 * The Syscon bootloader code is accepting commands in the following format:
 *    0x01 Command packet header
 *    0xNN Packet length, the count starts with the command code byte up to, but not including, the checksum byte
 *    0xNN Command code, one of 0x00, 0x13, 0x20, 0x22, 0x32, 0x40, 0x70, 0x90, 0x9E, 0xA0, 0xA4, 0xB0, 0xC0, 0xC5
 *    .... Optional command parameters
 *    0xNN Checksum byte
 *    0x03 End of packet
 *
 * After reception of such a command packet, an acknowledge packet is being sent
 *    0x02 Response packet header
 *    0x01 Packet length
 *    0xNN Response code, one of
 *             0x06 Successful acknowledge
 *             0x04 Unknown command code
 *             0x05 Parameter error
 *             0x07 Checksum error
 *             0x0F Verify error
 *             0x10 Protection error
 *             0x15 Not acknowledged
 *             0x1A MRG10 (?) error
 *             0x1B MRG11 (?) error
 *             0x1C Write error
 *             0x20 Read error
 *             0xFF Busy error
 *    0xNN Checksum byte
 *    0x03 End of packet
 *
 * Few examples:
 *    - Command : 0x01 0x01 0x00 0xFF 0x03
 *      Response: 0x02 0x01 0x06 0xF9 0x03
 *    - Command:  0x01 0x01 0x70 0x8F 0x03
 *      Response: 0x02 0x01 0x04 0xFB 0x03
 *    - Command:  0x01 0x03 0xA4 0x00 0x00 0x59 0x03
 *      Response: 0x02 0x01 0x06 0xF9 0x03
 *
 * Some commands require additional data to follow:
 *    0x02 Data packet header
 *    0xNN Packet length (0x00 means 256)
 *    ...  NN bytes of data
 *    0xNN Checksum byte
 *    0xNN Indicates if further data packets are following:
 *             0x17 means further data packet(s) are following
 *             0x03 means this was the last data packet
 *
 * After reception of such a data packet, an acknowledge packet is being sent
 *    0x02 Response packet header
 *    0x02 Packet length (contains 2 response code bytes)
 *    0xNN Response code for the data packet reception
 *             0x06 Successful acknowledge
 *             0x07 Checksum error
 *             0x15 Not acknowledged
 *    0xNN Additional response code from the command execution
 *             0x06 Successful command execution
 *             0x0F Verify error
 *             0x1C Write error
 *             0x20 Read error
 *    0xNN Checksum byte
 *    0x03 End of packet
 *
 * Example:
 *    - Command : 0x01 0x07 0x13 0x00 0x08 0x00 0x00 0x0B 0xFF 0xD4 0x3
 *      Response: 0x02 0x01 0x06 0xF9 0x03
 *      Data#1  : 0x02 0x00 ...256 data bytes... 0xNN 0x17
 *      Response: 0x02 0x02 0x06 0x06 0xF2 0x03
 *      Data#2  : 0x02 0x00 ...256 data bytes... 0xNN 0x17
 *      Response: 0x02 0x02 0x06 0x06 0xF2 0x03
 *      Data#3  : 0x02 0x00 ...256 data bytes... 0xNN 0x17
 *      Response: 0x02 0x02 0x06 0x06 0xF2 0x03
 *      Data#4  : 0x02 0x00 ...256 data bytes... 0xNN 0x03
 *      Response: 0x02 0x02 0x06 0x0F 0xE9 0x03
 *
 * @author gid15
 *
 */
public class SysconBootloaderEmulator extends Nec78k0SerialInterface {
	private static final int STATE_VERSION = 0;
	// First command to be sent before other commands can be processed
    public static final int SYSCON_BOOTLOADER_COMMAND_INIT = 0x00;
    // Compare the contents of the given address range with the passed data
    public static final int SYSCON_BOOTLOADER_COMMAND_COMPARE_SECUREFLASH = 0x13;
    // Erase the contents of the complete secure flash with 0xFF
    public static final int SYSCON_BOOTLOADER_COMMAND_ERASE_COMPLETE_SECUREFLASH = 0x20;
    // Erase the contents of the given address range with 0xFF
    public static final int SYSCON_BOOTLOADER_COMMAND_ERASE_SECUREFLASH = 0x22;
    // Check if the given address range is blank (i.e. filled with 0xFF)
    public static final int SYSCON_BOOTLOADER_COMMAND_CHECK_BLANK_SECUREFLASH = 0x32;
    // Write secure flash in the given address range
    public static final int SYSCON_BOOTLOADER_COMMAND_WRITE_SECUREFLASH = 0x40;
    // Returns any error happening during SYSCON_BOOTLOADER_COMMAND_INIT
    public static final int SYSCON_BOOTLOADER_COMMAND_GET_INIT_ERROR = 0x70;
    // Set a timeout value
    public static final int SYSCON_BOOTLOADER_COMMAND_SET_TIMEOUT = 0x90;
    // Set configuration values which mainly seem to be some delays for different operations
    public static final int SYSCON_BOOTLOADER_COMMAND_SET_CONFIG_FOR_DELAYS = 0x9E;
    // Unclear function - erase range 0x0400 - 0x07FF and init ranges 0x0600-0x060B, 0x07F4-0x07FF
    public static final int SYSCON_BOOTLOADER_COMMAND_SET_SECURITY = 0xA0;
    // Read secure flash 0x0000 - 0x03FF
    public static final int SYSCON_BOOTLOADER_COMMAND_READ_SECUREFLASH_0000_to_03FF = 0xA4;
    // Compute the 16-bit checksum of the secure flash in the given address range (must be a multiple of 0x100)
    public static final int SYSCON_BOOTLOADER_COMMAND_CHECKSUM_SECUREFLASH = 0xB0;
    // Returns a fixed value 0x10 0x7F 0x04 0x7C - 0x7F - 0x7F 0x80 - 0xC4 0x37 0x38 0x46 0xB0 0xB5 - 0xB0 - 0x31 - 0x20 - 0x20 - 0x79 - 0x03
    public static final int SYSCON_BOOTLOADER_COMMAND_GET_INFO = 0xC0;
    // Returns a fixed value 0x000000 - 0x020000
    public static final int SYSCON_BOOTLOADER_COMMAND_GET_VERSION = 0xC5;
	private final int[] buffer = new int[32 + 0x400];
	private int index;
	private int length;

    public SysconBootloaderEmulator(Nec78k0Sfr sfr, Nec78k0SerialInterfaceUART6 serialInterface) {
		super(sfr, serialInterface);
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		index = stream.readInt();
		length = stream.readInt();
		stream.readInts(buffer);
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(index);
		stream.writeInt(length);
		stream.writeInts(buffer);
		super.write(stream);
	}

	@Override
	public void reset() {
		index = 0;
		length = 0;
		super.reset();
	}

	@Override
	public void startTransmission() {
	}

	@Override
	public void startReception() {
	}

	@Override
	public void transmit(int value) {
	}

	@Override
	public int receive() {
		if (index >= length) {
			return 0x00;
		}
		return buffer[index++];
	}

	@Override
	public boolean hasReceived() {
		return !isEmpty();
	}

	public boolean isEmpty() {
		return index >= length;
	}

	public void setCommand(int command, int[] data) {
		if (isEmpty()) {
			index = 0;
			length = 0;
		}

		// Fixed start value
		buffer[length++] = 0x01;
		int startChecksum = length;

		// Data length
		buffer[length++] = data.length + 1;

		// Command
		buffer[length++] = command;

		// Data values
		for (int i = 0; i < data.length; i++) {
			buffer[length++] = data[i];
		}

		// Checksum
		int checksum = 0x00;
		for (int i = startChecksum; i < length; i++) {
			checksum = (checksum - buffer[i]) & 0xFF;
		}
		buffer[length++] = checksum;

		// Fixed end value
		buffer[length++] = 0x03;
	}

	public void setCommand(int command) {
		setCommand(command, new int[0]);
	}

	public void setData(int[] data, boolean lastDataPacket) {
		if (isEmpty()) {
			index = 0;
			length = 0;
		}

		// Fixed start value
		buffer[length++] = 0x02;
		int startChecksum = length;

		// Data length
		buffer[length++] = data.length & 0xFF;

		// Data values
		for (int i = 0; i < data.length; i++) {
			buffer[length++] = data[i];
		}

		// Checksum
		int checksum = 0x00;
		for (int i = startChecksum; i < length; i++) {
			checksum = (checksum - buffer[i]) & 0xFF;
		}
		buffer[length++] = checksum;

		// Fixed end value
		buffer[length++] = lastDataPacket ? 0x03 : 0x17;
	}
}
