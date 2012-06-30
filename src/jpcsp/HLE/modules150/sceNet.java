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
import jpcsp.HLE.TPointer;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.hardware.Wlan;
import jpcsp.network.INetworkAdapter;
import jpcsp.network.NetworkAdapterFactory;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class sceNet extends HLEModule {
    protected static Logger log = Modules.getLogger("sceNet");
    private INetworkAdapter networkAdapter;

    @Override
    public String getName() {
        return "sceNet";
    }

    protected int netMemSize;

    public INetworkAdapter getNetworkAdapter() {
    	return networkAdapter;
    }

    /**
     * Convert a 6-byte MAC address into a string representation (xx:xx:xx:xx:xx:xx)
     * in lowercase.
     * The PSP always returns MAC addresses in lowercase.
     *
     * @param macAddress  MAC address
     * @return            string representation of the MAC address: x:xx:xx:xx:xx:xx (in lowercase).
     */
    public static String convertMacAddressToString(byte[] macAddress) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < macAddress.length; i++) {
            if (i > 0) {
            	str.append(":");
            }
            str.append(String.format("%02x", macAddress[i]));
        }

        return str.toString();
    }

    /**
     * Convert a string MAC address representation (xx:xx:xx:xx:xx:x)
     * into a 6-byte representation.
     * Both lower and uppercase representations are accepted.
     *
     * @param str    String representation in format xx:xx:xx:xx:xx:xx (in lower or uppercase)
     * @return       6-byte representation
     */
    public static byte[] convertStringToMacAddress(String str) {
    	byte[] macAddress = new byte[Wlan.MAC_ADDRESS_LENGTH];
        for (int i = 0, n = 0; i < macAddress.length; i++) {
        	int n1 = parseHexDigit(str.charAt(n++));
        	int n2 = parseHexDigit(str.charAt(n++));
        	n++; // skip ':'
        	macAddress[i] = (byte) ((n1 << 4) + n2);
        }

        return macAddress;
    }

    @HLEFunction(nid = 0x39AF39A6, version = 150, checkInsideInterrupt = true)
    public void sceNetInit(Processor processor) {
        CpuState cpu = processor.cpu;

        int poolSize = cpu.gpr[4];
        int calloutThreadPri = cpu.gpr[5];
        int calloutThreadStack = cpu.gpr[6];
        int netinitThreadPri = cpu.gpr[7];
        int netinitThreadStack = cpu.gpr[8];

        log.warn("IGNORING: sceNetInit (poolsize=0x" + Integer.toHexString(poolSize) + ", calloutThreadPri=0x" + Integer.toHexString(calloutThreadPri)
                + ", calloutThreadStack=0x" + Integer.toHexString(calloutThreadStack) + ", netinitThreadPri=0x" + Integer.toHexString(netinitThreadPri)
                + ", netinitThreadStack=0x" + Integer.toHexString(netinitThreadStack) + ")");

        
        netMemSize = poolSize;
        cpu.gpr[2] = 0;
    }

    @HLEFunction(nid = 0x281928A9, version = 150, checkInsideInterrupt = true)
    public void sceNetTerm(Processor processor) {
        CpuState cpu = processor.cpu;
        
        log.warn("IGNORING: sceNetTerm");
        
        
        cpu.gpr[2] = 0;
    }

    @HLEFunction(nid = 0x50647530, version = 150, checkInsideInterrupt = true)
    public void sceNetFreeThreadinfo(Processor processor) {
        CpuState cpu = processor.cpu;
        
        int thID = cpu.gpr[4];

        log.warn("IGNORING: sceNetFreeThreadinfo (thID=0x" + Integer.toHexString(thID) + ")");

        
        cpu.gpr[2] = 0;
    }

    @HLEFunction(nid = 0xAD6844c6, version = 150, checkInsideInterrupt = true)
    public void sceNetThreadAbort(Processor processor) {
        CpuState cpu = processor.cpu;

        int thID = cpu.gpr[4];

        log.warn("IGNORING: sceNetThreadAbort (thID=0x" + Integer.toHexString(thID) + ")");

        
        cpu.gpr[2] = 0;
    }

    @HLEFunction(nid = 0x89360950, version = 150, checkInsideInterrupt = true)
    public int sceNetEtherNtostr(TPointer etherAddr, TPointer strAddr) {
        Memory mem = Processor.memory;

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceNetEtherNtostr etherAddr=%s, strAddr=%s: %s", etherAddr, strAddr, Utilities.getMemoryDump(etherAddr.getAddress(), Wlan.MAC_ADDRESS_LENGTH, 1, Wlan.MAC_ADDRESS_LENGTH)));
        }

        // Convert 6-byte Mac address into string representation (XX:XX:XX:XX:XX:XX).
        pspNetMacAddress macAddress = new pspNetMacAddress();
        macAddress.read(mem, etherAddr.getAddress());
        Utilities.writeStringZ(mem, strAddr.getAddress(), convertMacAddressToString(macAddress.macAddress));

        return 0;
    }

    protected static int parseHexDigit(char c) {
    	if (c >= '0' && c <= '9') {
    		return c - '0';
    	} else if (c >= 'A' && c <= 'F') {
    		return c - 'A' + 10;
    	} else if (c >= 'a' && c <= 'f') {
    		return c - 'a' + 10;
    	} else {
    		log.error(String.format("Cannot parse hex digit '%c'", c));
    		return 0;
    	}
    }

    @HLEFunction(nid = 0xD27961C9, version = 150, checkInsideInterrupt = true)
    public int sceNetEtherStrton(TPointer strAddr, TPointer etherAddr) {
    	String str = Utilities.readStringNZ(strAddr.getAddress(), 17);
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceNetEtherStrton strAddr=%s(%s), etherAddr=%s", strAddr, str, etherAddr));
        }

        // Convert string Mac address string representation (XX:XX:XX:XX:XX:XX)
    	// into 6-byte representation.
    	pspNetMacAddress macAddress = new pspNetMacAddress();
    	macAddress.setMacAddress(convertStringToMacAddress(str));
        macAddress.write(Memory.getInstance(), etherAddr.getAddress());

        return 0;
    }

    protected int networkSwap32(int value) {
    	return Integer.reverseBytes(value);
    }

    protected int networkSwap16(int value) {
    	return Integer.reverseBytes(value) >>> 16;
    }

    @HLEFunction(nid = 0xF5805EFE, version = 150, checkInsideInterrupt = true)
    public void sceNetHtonl(Processor processor) {
        CpuState cpu = processor.cpu;

        int host32 = cpu.gpr[4];

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceNetHtonl(%08X)", host32));
        }

        // Convert host 32-bits to network 32-bits
        cpu.gpr[2] = networkSwap32(host32);
    }

    @HLEFunction(nid = 0x39C1BF02, version = 150, checkInsideInterrupt = true)
    public void sceNetHtons(Processor processor) {
        CpuState cpu = processor.cpu;

        int host16 = cpu.gpr[4];

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceNetHtons(%04X)", host16));
        }

        // Convert host 16-bits to network 16-bits
        cpu.gpr[2] = networkSwap16(host16);
    }

    @HLEFunction(nid = 0x93C4AF7E, version = 150, checkInsideInterrupt = true)
    public void sceNetNtohl(Processor processor) {
        CpuState cpu = processor.cpu;

        int net32 = cpu.gpr[4];

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceNetNtohl(%08X)", net32));
        }

        // Convert network 32-bits to host 32-bits
        cpu.gpr[2] = networkSwap32(net32);
    }

    @HLEFunction(nid = 0x4CE03207, version = 150, checkInsideInterrupt = true)
    public void sceNetNtohs(Processor processor) {
        CpuState cpu = processor.cpu;

        int net16 = cpu.gpr[4];

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceNetNtohs(%04X)", net16));
        }

        // Convert network 16-bits to host 16-bits
        cpu.gpr[2] = networkSwap16(net16);
    }

    @HLEFunction(nid = 0x0BF0A3AE, version = 150, checkInsideInterrupt = true)
    public void sceNetGetLocalEtherAddr(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int etherAddr = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug("sceNetGetLocalEtherAddr (etherAddr=0x" + Integer.toHexString(etherAddr) + ")");
        }

        

        if (Memory.isAddressGood(etherAddr)) {
            // Return WLAN MAC address
        	pspNetMacAddress macAddress = new pspNetMacAddress();
        	macAddress.setMacAddress(Wlan.getMacAddress());
        	macAddress.write(mem, etherAddr);
            cpu.gpr[2] = 0;
        } else {
        	cpu.gpr[2] = -1;
        }
    }

    @HLEFunction(nid = 0xCC393E48, version = 150, checkInsideInterrupt = true)
    public int sceNetGetMallocStat(TPointer statAddr) {
        Memory mem = Processor.memory;

        log.warn(String.format("PARTIAL: sceNetGetMallocStat statAddr=%s", statAddr));

        // Faking. Assume the pool is half free.
        int freeSize = netMemSize / 2;

        int addr = statAddr.getAddress();
        mem.write32(addr, netMemSize);                 // Poolsize from sceNetInit.
        mem.write32(addr + 4, netMemSize - freeSize);  // Currently in use size.
        mem.write32(addr + 8, freeSize);               // Free size.

        return 0;
    }

	@Override
	public void start() {
		networkAdapter = NetworkAdapterFactory.createNetworkAdapter();
		networkAdapter.start();

		super.start();
	}

	@Override
	public void stop() {
		networkAdapter.stop();
		networkAdapter = null;

		super.stop();
	}
}