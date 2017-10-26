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
package jpcsp.memory.mmio;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import jpcsp.Memory;
import jpcsp.hardware.Screen;

public class MMIO extends Memory {
    private final Memory mem;
    private final Map<Integer, IMMIOHandler> handlers = new HashMap<Integer, IMMIOHandler>();

    public MMIO(Memory mem) {
    	this.mem = mem;
    }

    @Override
	public void Initialise() {
    	handlers.clear();

    	addHandlerRW(0xBC000000, 0x54); // Memory interface
    	addHandler(MMIOHandlerSystemControl.BASE_ADDRESS, MMIOHandlerSystemControl.SIZE_OF, MMIOHandlerSystemControl.getInstance());
    	addHandlerRW(0xBC200000, 0x4); // sysreg
    	addHandler(MMIOHandlerInterruptMan.BASE_ADDRESS, 0x30, MMIOHandlerInterruptMan.getInstance());
    	addHandler(0xBC500000, 0x10, new int[] { 0x0100 }, new MMIOHandlerTimer(0xBC500000));
    	addHandler(0xBC500010, 0x10, new int[] { 0x0100 }, new MMIOHandlerTimer(0xBC500010));
    	addHandler(0xBC500020, 0x10, new int[] { 0x0100 }, new MMIOHandlerTimer(0xBC500020));
    	addHandler(0xBC500030, 0x10, new int[] { 0x0100 }, new MMIOHandlerTimer(0xBC500030));
    	addHandler(0xBC600000, 0x14, new MMIOHandlerSystemTime(0xBC600000));
    	addHandlerRW(0xBC800000, 0x164); // DMA control (dmacplus)
    	addHandlerRW(0xBD100000, 0x1204); // NAND flash
    	addHandler(0xBD200000, 0x40, new MMIOHandlerReadWrite16(0xBD200000, 0x40)); // Memory stick
    	addHandlerRW(0xBD300000, 0x44); // Wlan
    	addHandlerRW(0xBD400000, 0x8F0); // Graphics engine (ge)
    	addHandlerRO(0xBD400100, 0x4);
    	addHandlerRW(0xBD500000, 0x94); // Graphics engine (ge)
    	addHandlerRO(0xBD500010, 0x4);
    	addHandlerRW(0xBD600000, 0x50); // Ata
    	addHandler(0xBD700000, 0xF, new MMIOHandlerReadWrite8(0xBD700000, 0xF)); // Ata
    	addHandler(0xBDE00000, 0x3C, new MMIOHandlerKirk(0xBDE00000)); // Kirk
    	addHandlerRW(0xBDF00000, 0x90); // UMD
    	addHandlerRW(0xBE000000, 0x54); // Audio
    	write32(0xBE000028, 0x30);
    	addHandlerRW(0xBE140000, 0x204); // LCDC / display
    	write32(0xBE140010, 0x29);
    	write32(0xBE140014, 0x02);
    	write32(0xBE140018, 0x02);
    	write32(0xBE14001C, Screen.width);
    	write32(0xBE140020, 0x0A);
    	write32(0xBE140024, 0x02);
    	write32(0xBE140028, 0x02);
    	write32(0xBE14002C, Screen.height);
    	write32(0xBE140048, Screen.width);
    	write32(0xBE14004C, Screen.height);
    	write32(0xBE140050, 0x01);
    	addHandler(MMIOHandlerGpio.BASE_ADDRESS, 0x4C, MMIOHandlerGpio.getInstance());
    	addHandlerRW(0xBE300000, 0x48); // Power management
    	addHandlerRW(0xBE4C0000, 0x48); // UART4 Uart4/kernel debug(?) UART (IPL, uart4, reboot)
    	addHandlerRW(0xBE500000, 0x3C); // UART3 headphone remote SIO (hpremote)
    	addHandler(MMIOHandlerSyscon.BASE_ADDRESS, 0x28, MMIOHandlerSyscon.getInstance());
    	addHandler(MMIOHandlerDisplayController.BASE_ADDRESS, 0x28, MMIOHandlerDisplayController.getInstance());
    	addHandlerRW(0xBFC00000, 0x1000);
    }

    private void addHandler(int baseAddress, int length, IMMIOHandler handler) {
    	addHandler(baseAddress, length, null, handler);
    }

    private void addHandler(int baseAddress, int length, int[] additionalOffsets, IMMIOHandler handler) {
    	for (int i = 0; i < length; i++) {
    		handlers.put(baseAddress + i, handler);
    	}

    	if (additionalOffsets != null) {
	    	for (int offset : additionalOffsets) {
	    		handlers.put(baseAddress + offset, handler);
	    	}
    	}
    }

    private void addHandlerRW(int baseAddress, int length) {
    	addHandler(baseAddress, length, new MMIOHandlerReadWrite(baseAddress, length));
    }

    private void addHandlerRO(int baseAddress, int length) {
    	addHandler(baseAddress, length, new MMIOHandlerReadOnly(baseAddress, length));
    }

    private IMMIOHandler getHandler(int address) {
    	return handlers.get(address);
    }

    public static boolean isAddressGood(int address) {
    	if (Memory.isAddressGood(address)) {
    		return true;
    	}
    	return address >= 0xBC000000 && address < 0xBFC01000;
    }

    @Override
	public int read8(int address) {
    	IMMIOHandler handler = getHandler(address);
    	if (handler != null) {
    		return handler.read8(address);
    	}
		return mem.read8(address);
	}

	@Override
	public int read16(int address) {
    	IMMIOHandler handler = getHandler(address);
    	if (handler != null) {
    		return handler.read16(address);
    	}
		return mem.read16(address);
	}

	@Override
	public int read32(int address) {
    	IMMIOHandler handler = getHandler(address);
    	if (handler != null) {
    		return handler.read32(address);
    	}
		return mem.read32(address);
	}

	@Override
	public void write8(int address, byte data) {
    	IMMIOHandler handler = getHandler(address);
    	if (handler != null) {
    		handler.write8(address, data);
    	} else {
    		mem.write8(address, data);
    	}
	}

	@Override
	public void write16(int address, short data) {
    	IMMIOHandler handler = getHandler(address);
    	if (handler != null) {
    		handler.write16(address, data);
    	} else {
    		mem.write16(address, data);
    	}
	}

	@Override
	public void write32(int address, int data) {
    	IMMIOHandler handler = getHandler(address);
    	if (handler != null) {
    		handler.write32(address, data);
    	} else {
    		mem.write32(address, data);
    	}
	}

	@Override
	public void memset(int address, byte data, int length) {
		mem.memset(address, data, length);
	}

	@Override
	public Buffer getMainMemoryByteBuffer() {
		return mem.getMainMemoryByteBuffer();
	}

	@Override
	public Buffer getBuffer(int address, int length) {
		return mem.getBuffer(address, length);
	}

	@Override
	public void copyToMemory(int address, ByteBuffer source, int length) {
		mem.copyToMemory(address, source, length);
	}

	@Override
	protected void memcpy(int destination, int source, int length, boolean checkOverlap) {
		if (((destination | source | length) & 0x3) == 0 && !checkOverlap) {
			for (int i = 0; i < length; i += 4) {
				write32(destination + i, read32(source + i));
			}
		} else {
			if (checkOverlap) {
				mem.memmove(destination, source, length);
			} else {
				mem.memcpy(destination, source, length);
			}
		}
	}
}
