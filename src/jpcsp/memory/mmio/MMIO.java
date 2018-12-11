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

import static jpcsp.MemoryMap.END_IO_1;
import static jpcsp.MemoryMap.START_IO_0;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.memory.mmio.cy27040.CY27040;
import jpcsp.memory.mmio.uart.MMIOHandlerUart3;
import jpcsp.memory.mmio.uart.MMIOHandlerUart4;
import jpcsp.memory.mmio.uart.MMIOHandlerUartBase;
import jpcsp.memory.mmio.wm8750.WM8750;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

public class MMIO extends Memory {
	private static final int STATE_VERSION = 0;
    private final Memory mem;
    private final Map<Integer, IMMIOHandler> handlers = new HashMap<Integer, IMMIOHandler>(40000);
    protected static final boolean[] validMemoryPage = new boolean[Memory.validMemoryPage.length];
    private final Map<Integer, IMMIOHandler> sortedHandlers = new TreeMap<Integer, IMMIOHandler>();

    public MMIO(Memory mem) {
    	this.mem = mem;
    }

    @Override
    public boolean allocate() {
    	System.arraycopy(Memory.validMemoryPage, 0, validMemoryPage, 0, validMemoryPage.length);
    	Arrays.fill(validMemoryPage, START_IO_0 >>> MEMORY_PAGE_SHIFT, (MemoryMap.END_EXCEPTIO_VEC >>> MEMORY_PAGE_SHIFT) + 1, true);

        return true;
    }

	@Override
	public void Initialise() {
    	handlers.clear();

    	addHandler(0xBC000000, 0x80, new MMIOHandlerMemoryAccessControl(0xBC000000));
    	addHandler(MMIOHandlerSystemControl.BASE_ADDRESS, 0x9C, MMIOHandlerSystemControl.getInstance());
    	addHandler(0xBC200000, 0x8, new MMIOHandlerCpuBusFrequency(0xBC200000));
    	addHandler(MMIOHandlerInterruptMan.BASE_ADDRESS, 0x30, MMIOHandlerInterruptMan.getProxyInstance());
    	addHandler(0xBC500000, 0x10, new int[] { 0x0100 }, new MMIOHandlerTimer(0xBC500000));
    	addHandler(0xBC500010, 0x10, new int[] { 0x0100 }, new MMIOHandlerTimer(0xBC500010));
    	addHandler(0xBC500020, 0x10, new int[] { 0x0100 }, new MMIOHandlerTimer(0xBC500020));
    	addHandler(0xBC500030, 0x10, new int[] { 0x0100 }, new MMIOHandlerTimer(0xBC500030));
    	addHandler(0xBC600000, 0x14, new MMIOHandlerSystemTime(0xBC600000));
    	addHandler(0xBC800000, 0x1D4, new MMIOHandlerDmacplus(0xBC800000));
    	addHandler(0xBC900000, 0x1F4, new MMIOHandlerDmac(0xBC900000));
    	addHandler(0xBCA00000, 0x1F4, new MMIOHandlerDmac(0xBCA00000));
    	addHandler(0xBCC00000, 0x74, new MMIOHandlerMeController(0xBCC00000));
    	addHandler(MMIOHandlerDdr.BASE_ADDRESS, 0x48, MMIOHandlerDdr.getInstance());
    	addHandler(MMIOHandlerNand.BASE_ADDRESS, 0x304, MMIOHandlerNand.getInstance());
    	addHandler(0xBD200000, 0x44, new MMIOHandlerMemoryStick(0xBD200000));
//    	addHandler(0xBD300000, 0x44, new MMIOHandlerWlan(0xBD300000));
    	addHandlerRW(0xBD300000, 0x44);
    	addHandler(MMIOHandlerGe.BASE_ADDRESS, 0xE50, MMIOHandlerGe.getInstance());
    	addHandler(0xBD500000, 0x94, new MMIOHandlerGeEdram(0xBD500000));
    	addHandler(0xBD600000, 0x50, new MMIOHandlerAta2(0xBD600000));
    	addHandler(MMIOHandlerAta.BASE_ADDRESS, 0xF, MMIOHandlerAta.getInstance());
    	addHandler(MMIOHandlerUsb.BASE_ADDRESS, 0x420, MMIOHandlerUsb.getInstance());
    	addHandler(0xBDE00000, 0x3C, new MMIOHandlerKirk(0xBDE00000));
    	addHandler(MMIOHandlerUmd.BASE_ADDRESS, 0x98, MMIOHandlerUmd.getInstance());
    	addHandler(0xBE000000, 0x80, new MMIOHandlerAudio(0xBE000000));
    	addHandler(0xBE140000, 0x204, new MMIOHandlerLcdc(0xBE140000));
    	addHandler(0xBE200000, 0x30, new MMIOHandlerI2c(0xBE200000));
    	addHandler(MMIOHandlerGpio.BASE_ADDRESS, 0x4C, MMIOHandlerGpio.getInstance());
    	addHandler(0xBE300000, 0x60, new MMIOHandlerPower(0xBE300000));
    	addHandler(0xBE4C0000, MMIOHandlerUartBase.SIZE_OF, new MMIOHandlerUart4(0xBE4C0000));
    	addHandler(0xBE500000, MMIOHandlerUartBase.SIZE_OF, new MMIOHandlerUart3(0xBE500000));
    	addHandler(MMIOHandlerSyscon.BASE_ADDRESS, 0x28, MMIOHandlerSyscon.getInstance());
    	addHandler(MMIOHandlerDisplayController.BASE_ADDRESS, 0x28, MMIOHandlerDisplayController.getInstance());
    	addHandlerRW(0xBFC00000, 0x1000);
    	write32(0xBFC00200, 0x2E547106);
    	write32(0xBFC00204, 0xFBDFC08B);
    	write32(0xBFC00208, 0x087FCC08);
    	write32(0xBFC0020C, 0xAA60334E);
    	addHandler(MMIOHandlerMeCore.BASE_ADDRESS, 0x2C, MMIOHandlerMeCore.getInstance());
    	addHandler(MMIOHandlerNandPage.BASE_ADDRESS1, 0x90C, MMIOHandlerNandPage.getInstance());
    	addHandler(MMIOHandlerNandPage.BASE_ADDRESS2, 0x90C, MMIOHandlerNandPage.getInstance());
    }

    protected void addHandler(int baseAddress, int length, IMMIOHandler handler) {
    	addHandler(baseAddress, length, null, handler);
    }

    private void addHandler(int baseAddress, int length, int[] additionalOffsets, IMMIOHandler handler) {
    	// The handlers will be kept sorted based on their baseAddress
    	sortedHandlers.put(baseAddress, handler);

    	for (int i = 0; i < length; i++) {
    		handlers.put(baseAddress + i, handler);
    	}

    	if (additionalOffsets != null) {
	    	for (int offset : additionalOffsets) {
	    		handlers.put(baseAddress + offset, handler);
	    	}
    	}
    }

    protected void addHandlerRW(int baseAddress, int length) {
    	addHandlerRW(baseAddress, length, null);
    }

    protected void addHandlerRW(int baseAddress, int length, Logger log) {
    	MMIOHandlerReadWrite handler = new MMIOHandlerReadWrite(baseAddress, length);
    	if (log != null) {
    		handler.setLogger(log);
    	}
    	addHandler(baseAddress, length, handler);
    }

    protected IMMIOHandler getHandler(int address) {
    	return handlers.get(address);
    }

    private boolean hasHandler(int address) {
    	return handlers.containsKey(address);
    }

    public static boolean isAddressGood(int address) {
        return validMemoryPage[address >>> MEMORY_PAGE_SHIFT];
    }

    @Override
    public int normalize(int address) {
    	if (hasHandler(address)) {
    		return address;
    	}
    	return mem.normalize(address);
    }

    public static int normalizeAddress(int addr) {
		// Transform address 0x1nnnnnnn into 0xBnnnnnnn
		if (addr >= (START_IO_0 & Memory.addressMask) && addr <= (END_IO_1 & Memory.addressMask)) {
			addr |= (START_IO_0 & ~Memory.addressMask);
		}

		return addr;
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
    	IMMIOHandler handler = getHandler(address);
    	if (handler != null) {
    		for (int i = 0; i < length; i++) {
    			handler.write8(address + i, data);
    		}
    	} else {
    		mem.memset(address, data, length);
    	}
	}

	@Override
	public Buffer getMainMemoryByteBuffer() {
		return mem.getMainMemoryByteBuffer();
	}

	@Override
	public Buffer getBuffer(int address, int length) {
		IMMIOHandler handler = getHandler(address);
		if (handler != null) {
			return null;
		}
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

    @Override
	public void read(StateInputStream stream) throws IOException {
    	stream.readVersion(STATE_VERSION);
    	// The handlers are kept sorted based on their base address
    	for (Integer baseAddress : sortedHandlers.keySet()) {
    		IMMIOHandler handler = sortedHandlers.get(baseAddress);
    		handler.read(stream);
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("Read State for %s at 0x%08X", handler, baseAddress));
    		}
    	}
    	CY27040.getInstance().read(stream);
    	WM8750.getInstance().read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
    	// The handlers are kept sorted based on their base address
    	for (Integer baseAddress : sortedHandlers.keySet()) {
    		IMMIOHandler handler = sortedHandlers.get(baseAddress);
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("Writing State for %s at 0x%08X", handler, baseAddress));
    		}
    		handler.write(stream);
    	}
    	CY27040.getInstance().write(stream);
    	WM8750.getInstance().write(stream);
	}
}
