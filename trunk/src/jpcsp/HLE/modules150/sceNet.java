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
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.PspString;
import jpcsp.HLE.StringInfo;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.Memory;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.hardware.Wlan;
import jpcsp.network.INetworkAdapter;
import jpcsp.network.NetworkAdapterFactory;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

@HLELogging
public class sceNet extends HLEModule {
    public static Logger log = Modules.getLogger("sceNet");
    private INetworkAdapter networkAdapter;
	protected int netMemSize;

    @Override
    public String getName() {
        return "sceNet";
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

    public INetworkAdapter getNetworkAdapter() {
    	return networkAdapter;
    }

    /**
     * Convert a 6-byte MAC address into a string representation (xx:xx:xx:xx:xx:xx)
     * in lower-case.
     * The PSP always returns MAC addresses in lower-case.
     *
     * @param macAddress  MAC address
     * @return            string representation of the MAC address: xx:xx:xx:xx:xx:xx (in lower-case).
     */
    public static String convertMacAddressToString(byte[] macAddress) {
    	return String.format("%02x:%02x:%02x:%02x:%02x:%02x", macAddress[0], macAddress[1], macAddress[2], macAddress[3], macAddress[4], macAddress[5]);
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

    protected int networkSwap32(int value) {
    	return Utilities.endianSwap32(value);
    }

    protected int networkSwap16(int value) {
    	return Utilities.endianSwap16(value);
    }

    @HLELogging(level="info")
    @HLEFunction(nid = 0x39AF39A6, version = 150, checkInsideInterrupt = true)
    public int sceNetInit(int poolSize, int calloutThreadPri, int calloutThreadStack, int netinitThreadPri, int netinitThreadStack) {
        netMemSize = poolSize;
        return 0;
    }

    @HLEFunction(nid = 0x281928A9, version = 150, checkInsideInterrupt = true)
    public int sceNetTerm() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x50647530, version = 150, checkInsideInterrupt = true)
    public int sceNetFreeThreadinfo(int threadID) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xAD6844c6, version = 150, checkInsideInterrupt = true)
    public int sceNetThreadAbort(int threadID) {
    	return 0;
    }

    @HLEFunction(nid = 0x89360950, version = 150, checkInsideInterrupt = true)
    public int sceNetEtherNtostr(pspNetMacAddress macAddress, TPointer strAddr) {
        // Convert 6-byte Mac address into string representation (XX:XX:XX:XX:XX:XX).
        Utilities.writeStringZ(Memory.getInstance(), strAddr.getAddress(), convertMacAddressToString(macAddress.macAddress));

        return 0;
    }

    @HLEFunction(nid = 0xD27961C9, version = 150, checkInsideInterrupt = true)
    public int sceNetEtherStrton(@StringInfo(maxLength=17) PspString str, TPointer etherAddr) {
        // Convert string Mac address string representation (XX:XX:XX:XX:XX:XX)
    	// into 6-byte representation.
    	pspNetMacAddress macAddress = new pspNetMacAddress();
    	macAddress.setMacAddress(convertStringToMacAddress(str.getString()));
        macAddress.write(etherAddr);

        return 0;
    }

    @HLEFunction(nid = 0xF5805EFE, version = 150, checkInsideInterrupt = true)
    public int sceNetHtonl(int host32) {
        // Convert host 32-bits to network 32-bits
        return networkSwap32(host32);
    }

    @HLEFunction(nid = 0x39C1BF02, version = 150, checkInsideInterrupt = true)
    public int sceNetHtons(int host16) {
        // Convert host 16-bits to network 16-bits
        return networkSwap16(host16);
    }

    @HLEFunction(nid = 0x93C4AF7E, version = 150, checkInsideInterrupt = true)
    public int sceNetNtohl(int net32) {
        // Convert network 32-bits to host 32-bits
        return networkSwap32(net32);
    }

    @HLEFunction(nid = 0x4CE03207, version = 150, checkInsideInterrupt = true)
    public int sceNetNtohs(int net16) {
        // Convert network 16-bits to host 16-bits
        return networkSwap16(net16);
    }

    @HLEFunction(nid = 0x0BF0A3AE, version = 150, checkInsideInterrupt = true)
    public int sceNetGetLocalEtherAddr(TPointer etherAddr) {
        // Return WLAN MAC address
    	pspNetMacAddress macAddress = new pspNetMacAddress();
    	macAddress.setMacAddress(Wlan.getMacAddress());
    	macAddress.write(etherAddr);

    	return 0;
    }

    @HLEFunction(nid = 0xCC393E48, version = 150, checkInsideInterrupt = true)
    public int sceNetGetMallocStat(TPointer32 statAddr) {
        // Faking. Assume the pool is half free.
        int freeSize = netMemSize / 2;

        statAddr.setValue(0, netMemSize);             // Poolsize from sceNetInit.
        statAddr.setValue(4, netMemSize - freeSize);  // Currently in use size.
        statAddr.setValue(8, freeSize);               // Free size.

        return 0;
    }
}