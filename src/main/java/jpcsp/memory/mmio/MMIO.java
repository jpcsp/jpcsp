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
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.modules.memlmd;
import jpcsp.crypto.KeyVault;
import jpcsp.hardware.Model;
import jpcsp.memory.mmio.eflash.MMIOHandlerEFlash;
import jpcsp.memory.mmio.eflash.MMIOHandlerEFlashAta;
import jpcsp.memory.mmio.eflash.MMIOHandlerEFlashDma;
import jpcsp.memory.mmio.memorystick.MMIOHandlerMemoryStick;
import jpcsp.memory.mmio.syscon.MMIOHandlerSyscon;
import jpcsp.memory.mmio.uart.MMIOHandlerUart3;
import jpcsp.memory.mmio.uart.MMIOHandlerUart4;
import jpcsp.memory.mmio.uart.MMIOHandlerUartBase;
import jpcsp.memory.mmio.umd.MMIOHandlerUmdAta;
import jpcsp.memory.mmio.wlan.MMIOHandlerWlan;
import jpcsp.memory.mmio.umd.MMIOHandlerAta2;
import jpcsp.memory.mmio.umd.MMIOHandlerUmd;
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
    	Arrays.fill(validMemoryPage, START_IO_0 >>> MEMORY_PAGE_SHIFT, (MemoryMap.END_IO_1 >>> MEMORY_PAGE_SHIFT) + 1, true);

        return true;
    }

    @Override
    public void reset() {
    	for (IMMIOHandler handler : sortedHandlers.values()) {
    		handler.reset();
    	}
    }

    public Memory getBackendMemory() {
    	return mem;
    }

    @Override
	public void Initialise() {
    	handlers.clear();

    	addHandler(0xA7F00000, 0x24, new MMIOHandlerA7F00000(0xA7F00000), 4);
    	addHandlerRW(0xA7F80000, 0x2000, 4);

    	addHandler(0xBC000000, 0x80, new MMIOHandlerMemoryAccessControl(0xBC000000));
    	addHandler(MMIOHandlerSystemControl.BASE_ADDRESS, 0x104, MMIOHandlerSystemControl.getInstance());
    	addHandler(0xBC200000, 0x8, new MMIOHandlerCpuBusFrequency(0xBC200000));
    	addHandler(MMIOHandlerInterruptMan.BASE_ADDRESS, 0x30, MMIOHandlerInterruptMan.getProxyInstance());
    	addHandler(0xBC400000, 0x54, new MMIOHandlerProfiler(0xBC400000));
    	addHandler(0xBC500000, 0x10, new int[] { 0x0100 }, new MMIOHandlerTimer(0xBC500000, IntrManager.PSP_SYSTIMER0_INTR));
    	addHandler(0xBC500010, 0x10, new int[] { 0x0100 }, new MMIOHandlerTimer(0xBC500010, IntrManager.PSP_SYSTIMER1_INTR));
    	addHandler(0xBC500020, 0x10, new int[] { 0x0100 }, new MMIOHandlerTimer(0xBC500020, IntrManager.PSP_SYSTIMER2_INTR));
    	addHandler(0xBC500030, 0x10, new int[] { 0x0100 }, new MMIOHandlerTimer(0xBC500030, IntrManager.PSP_SYSTIMER3_INTR));
    	addHandler(0xBC600000, 0x14, new MMIOHandlerSystemTime(0xBC600000));
    	addHandler(0xBC800000, 0x1D4, new MMIOHandlerDmacplus(0xBC800000));
    	addHandler(0xBC900000, 0x1F4, new MMIOHandlerDmac(0xBC900000));
    	addHandler(0xBCA00000, 0x1F4, new MMIOHandlerDmac(0xBCA00000));
    	addHandler(0xBCC00000, 0x74, new MMIOHandlerMeController(0xBCC00000));
    	addHandler(MMIOHandlerDdr.BASE_ADDRESS, 0x48, MMIOHandlerDdr.getInstance());
    	addHandler(MMIOHandlerNand.BASE_ADDRESS, 0x304, MMIOHandlerNand.getInstance());
    	addHandler(MMIOHandlerMemoryStick.BASE_ADDRESS, 0x44, MMIOHandlerMemoryStick.getInstance());
    	addHandler(MMIOHandlerWlan.BASE_ADDRESS, 0x44, MMIOHandlerWlan.getInstance());
    	addHandler(MMIOHandlerGe.BASE_ADDRESS, 0xE50, MMIOHandlerGe.getInstance());
    	addHandler(0xBD500000, 0x94, new MMIOHandlerGeEdram(0xBD500000));
    	addHandler(0xBD600000, 0x50, new MMIOHandlerAta2(0xBD600000));
    	addHandler(MMIOHandlerUmdAta.BASE_ADDRESS, 0xF, MMIOHandlerUmdAta.getInstance());
    	addHandler(MMIOHandlerUsb.BASE_ADDRESS, 0x518, MMIOHandlerUsb.getInstance());
    	if (Model.getModel() == Model.MODEL_PSP_GO) {
    		addHandler(MMIOHandlerEFlash.BASE_ADDRESS, 0x48, MMIOHandlerEFlash.getInstance());
    		addHandler(MMIOHandlerEFlashAta.BASE_ADDRESS, 0x10, MMIOHandlerEFlashAta.getInstance());
    		addHandler(0xBDB00000, 0x48, new MMIOHandlerEFlashDma(0xBDB00000));
    	}
    	addHandler(MMIOHandlerKirk.BASE_ADDRESS, 0x3C, MMIOHandlerKirk.getInstance());
    	addHandler(MMIOHandlerUmd.BASE_ADDRESS, 0x98, MMIOHandlerUmd.getInstance());
    	addHandler(MMIOHandlerAudio.BASE_ADDRESS, 0x80, MMIOHandlerAudio.getInstance());
    	addHandler(0xBE140000, 0x204, new MMIOHandlerLcdc(0xBE140000));
    	addHandler(0xBE200000, 0x30, new MMIOHandlerI2c(0xBE200000));
    	addHandler(MMIOHandlerGpio.BASE_ADDRESS, 0x4C, MMIOHandlerGpio.getInstance());
    	addHandler(0xBE300000, 0x60, new MMIOHandlerPower(0xBE300000));
    	addHandler(0xBE4C0000, MMIOHandlerUartBase.SIZE_OF, new MMIOHandlerUart4(0xBE4C0000));
    	addHandler(0xBE500000, MMIOHandlerUartBase.SIZE_OF, new MMIOHandlerUart3(0xBE500000));
    	addHandler(MMIOHandlerSyscon.BASE_ADDRESS, 0x28, MMIOHandlerSyscon.getInstance());
    	addHandler(0xBE5C0000, 0x28, new MMIOHandlerLcdControllerSlim(0xBE5C0000), 2);
    	addHandler(MMIOHandlerDisplayController.BASE_ADDRESS, 0x28, MMIOHandlerDisplayController.getInstance());
    	addHandler(0xBE780000, 0x20, new MMIOHandlerDisplayControllerSlim(0xBE780000), 2);

    	// Unknown addresses being only accessed during IPL boot
    	addHandlerRW(0xB1800000, 0x4);
    	addHandlerRW(0xB0000000, 0x4);

    	// The memory at 0xBFC00000 is only visible during the IPL execution
    	addHandlerRW(0xBFC00000, 0x100000); // 1Mb embedded RAM
    	// The memory at 0xBFD00000 will be remapped to 0xBFC00000 after IPL execution
    	// making the original memory at 0xBFC00000 no longer accessible.
    	// See remapMemoryAtProcessorReset().
    	addHandlerRW(0xBFD00000, 0x100000); // 1Mb embedded RAM

    	addHandler(MMIOHandlerNandPage.BASE_ADDRESS1, 0x90C, MMIOHandlerNandPage.getInstance());
    	addHandler(MMIOHandlerNandPage.BASE_ADDRESS2, 0x90C, MMIOHandlerNandPage.getInstance());

    	initMMIO();
	}

	private void xorKey(int keyAddr, int[] key, int[] xorInts) {
		byte[] xorBytes = memlmd.getKey(xorInts);
		for (int i = 0; i < xorBytes.length; i++) {
			write8(keyAddr + i, (byte) (key[i] ^ xorBytes[i]));
		}
	}

	public static int[] getXorKeyBFD00210() {
		switch (Model.getGeneration()) {
			case 2:
				return new int[] { 0x539CBC37, 0x7CC9CD1C, 0x66128056, 0x531C4971 };
			case 3:
			case 4:
			case 7:
			case 9:
			case 11:
				return new int[] { 0xDA87A21B, 0x982BFA1D, 0xDC328074, 0x575ABEE7 };
			case 5:
				return new int[] { 0x2AFCC7CF, 0xC7C84535, 0xD6B9C66F, 0x2957A953 };
		}

		return null;
	}

	public static int[] getKeyBFD00210() {
		switch (Model.getGeneration()) {
			case 2:
				return KeyVault.keys660_k2;
			case 3:
			case 4:
			case 7:
			case 9:
			case 11:
				return KeyVault.keys660_k7;
			case 5:
				return KeyVault.keys660_k5;
		}

		return null;
	}

	private void initMMIO() {
		// Initialization of key values used to decrypt pspbtcnf.bin
    	switch (Model.getGeneration()) {
    		case 1:
    			// The 4 values stored at 0xBFD00200 will be xor-ed
    			// with the following 4 values which are hardcoded in reboot.bin:
    			//   0x24388370
    			//   0xB565FA41
    			//   0x48ADBAA4
    			//   0x53DFF0BB
    			// The result of the xor has to match KeyVault.keys660_k1:
    			//   0x0A6CF276
    			//   0x4EBA3ACA
    			//   0x40D276AC
    			//   0xF9BFC3F5
    			// which will be used to decrypt flash0:/kd/pspbtcnf.bin and PRXes
    			xorKey(0xBFD00200, KeyVault.keys660_k1, new int[] { 0x24388370, 0xB565FA41, 0x48ADBAA4, 0x53DFF0BB });
    	    	break;
    		case 2:
    			// Same key as used for generation 1, but different XOR.
    			// Will be used to decrypt (01g) PRXes.
    			xorKey(0xBFD00200, KeyVault.keys660_k1, new int[] { 0x896161FF, 0xBCB845BB, 0x6DD47B69, 0x43C76AF0 });

    			// The 4 values stored at 0xBFD00210 will be xor-ed
    			// with the following 4 values which are hardcoded in reboot_02g.bin:
    			//   0x539CBC37
    			//   0x7CC9CD1C
    			//   0x66128056
    			//   0x531C4971
    			// The result of the xor has to match KeyVault.keys660_k2:
    			//   0x75553E7A
    			//   0x4FFC6AB9
    			//   0xB3DFE33E
    			//   0x822AE86C
    			// which will be used to decrypt flash0:/kd/pspbtcnf_02g.bin and _02g PRXes
    			xorKey(0xBFD00210, getKeyBFD00210(), getXorKeyBFD00210());
    	    	break;
    		case 3:
    		case 4:
			case 7:
    		case 9:
    		case 11:
    			// Same key as used for generation 1, but different XOR.
    			// Will be used to decrypt (01g) PRXes.
    			xorKey(0xBFD00200, KeyVault.keys660_k1, new int[] { 0xA8F5FC35, 0x7F8D6DB9, 0x1B4E5413, 0x150FBA9C });

    			// The 4 values stored at 0xBFD00210 will be xor-ed
    			// with the following 4 values which are hardcoded in reboot_03g.bin and reboot_09g.bin:
    			//   0xDA87A21B
    			//   0x982BFA1D
    			//   0xDC328074
    			//   0x575ABEE7
    			// The result of the xor has to match KeyVault.keys660_k7
    			// which will be used to decrypt flash0:/kd/pspbtcnf_03g.bin or pspbtcnf_09g.bin
    			// and _03g/_09g PRXes
    			xorKey(0xBFD00210, getKeyBFD00210(), getXorKeyBFD00210());
    	    	break;
    		case 5:
    			// Same key as used for generation 1, but different XOR.
    			// Will be used to decrypt (01g) PRXes.
    			xorKey(0xBFD00200, KeyVault.keys660_k1, new int[] { 0x444D3E9C, 0x7FDE2596, 0x52F26426, 0xE09216CC });

    			// The 4 values stored at 0xBFD00210 will be xor-ed
    			// with the following 4 values which are hardcoded in reboot_05g.bin
    			//   0x2AFCC7CF
    			//   0xC7C84535
    			//   0xD6B9C66F
    			//   0x2957A953
    			// The result of the xor has to match KeyVault.keys660_k5
    			// which will be used to decrypt flash0:/kd/pspbtcnf_05g.bin and _05g PRXes
    			xorKey(0xBFD00210, getKeyBFD00210(), getXorKeyBFD00210());
    	    	break;
	    	default:
	    		log.error(String.format("Unimplemented MMIO initialization for PSP Model %s", Model.getModelName()));
	    		break;
    	}
	}

	protected void addHandler(int baseAddress, int length, IMMIOHandler handler) {
    	addHandler(baseAddress, length, null, handler);
    }

	protected void addHandler(int baseAddress, int length, IMMIOHandler handler, int minimumGeneration) {
		if (Model.getGeneration() >= minimumGeneration) {
			addHandler(baseAddress, length, handler);
		}
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

    protected void addHandlerRW(int baseAddress, int length, int minimumGeneration) {
    	if (Model.getGeneration() >= minimumGeneration) {
    		addHandlerRW(baseAddress, length);
    	}
    }

    protected void addHandlerRW(int baseAddress, int length, Logger log) {
    	MMIOHandlerReadWrite handler = new MMIOHandlerReadWrite(baseAddress, length);
    	if (log != null) {
    		handler.setLogger(log);
    	}
    	addHandler(baseAddress, length, handler);
    }

    private void removeHandler(int baseAddress, int length) {
    	for (int i = 0; i < length; i++) {
    		handlers.remove(baseAddress + i);
    	}
    }

    @Override
    public void remapMemoryAtProcessorReset() {
		// When resetting the main processor, the memory content that was accessible
		// at address 0xBFD00000 is now made available at address 0xBFC00000 and
		// the address 0xBFD00000 becomes invalid.
    	final int size = 0x100000;
		memcpy(0xBFC00000, 0xBFD00000, size);
		RuntimeContext.invalidateRange(0xBFC00000, size);
		removeHandler(0xBFD00000, size);
		RuntimeContext.removeCodeBlocks(0xBFD00000, size);
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
	public int internalRead8(int address) {
    	IMMIOHandler handler = getHandler(address);
    	if (handler != null) {
    		return handler.internalRead8(address);
    	}
		return mem.internalRead8(address);
	}

	@Override
	public int internalRead16(int address) {
    	IMMIOHandler handler = getHandler(address);
    	if (handler != null) {
    		return handler.internalRead16(address);
    	}
		return mem.internalRead16(address);
	}

	@Override
	public int internalRead32(int address) {
    	IMMIOHandler handler = getHandler(address);
    	if (handler != null) {
    		return handler.internalRead32(address);
    	}
		return mem.internalRead32(address);
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
    	IMMIOHandler handler = getHandler(address);
    	if (handler != null) {
    		for (int i = 0; i < length; i++) {
    			handler.write8(address + i, source.get());
    		}
    	} else {
    		mem.copyToMemory(address, source, length);
    	}
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
	}
}
