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
package jpcsp.HLE.modules;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.PspString;
import jpcsp.HLE.StringInfo;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.Memory;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.hardware.Wlan;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;
import jpcsp.network.INetworkAdapter;
import jpcsp.network.NetworkAdapterFactory;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class sceNet extends HLEModule {
    public static Logger log = Modules.getLogger("sceNet");
    private INetworkAdapter networkAdapter;
	protected int netMemSize;
	private static final int[] look_ctype_table = new int[] {
		0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x08, 0x08, 0x08, 0x08, 0x08, 0x20, 0x20,
		0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20,
		0x18, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10,
		0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10,
		0x10, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
		0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x10, 0x10, 0x10, 0x10, 0x10,
		0x10, 0x42, 0x42, 0x42, 0x42, 0x42, 0x42, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02,
		0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x10, 0x10, 0x10, 0x10, 0x20
	};

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
    public void sceNetEtherNtostr(@CanBeNull pspNetMacAddress macAddress, @CanBeNull TPointer strAddr) {
    	// This syscall is only doing something when both parameters are not 0.
    	if (macAddress.isNotNull() && strAddr.isNotNull()) {
	        // Convert 6-byte Mac address into string representation (XX:XX:XX:XX:XX:XX).
	        Utilities.writeStringZ(Memory.getInstance(), strAddr.getAddress(), convertMacAddressToString(macAddress.macAddress));
    	}
    }

    @HLEFunction(nid = 0xD27961C9, version = 150, checkInsideInterrupt = true)
    public void sceNetEtherStrton(@StringInfo(maxLength=17) @CanBeNull PspString str, @CanBeNull TPointer etherAddr) {
    	// This syscall is only doing something when both parameters are not 0.
    	if (str.isNotNull() && etherAddr.isNotNull()) {
	        // Convert string Mac address string representation (XX:XX:XX:XX:XX:XX)
	    	// into 6-byte representation.
	    	pspNetMacAddress macAddress = new pspNetMacAddress();
	    	macAddress.setMacAddress(convertStringToMacAddress(str.getString()));
	        macAddress.write(etherAddr);
    	}
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

    @HLEFunction(nid = 0xD8722983, version = 150)
    public int sceNetStrlen(@CanBeNull TPointer srcAddr) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceNetStrlen '%s'", srcAddr.getStringZ()));
    	}
    	return Modules.SysclibForKernelModule.strlen(srcAddr);
    }

    @HLEFunction(nid = 0x80C9F02A, version = 150)
    public int sceNetStrcpy(@CanBeNull TPointer destAddr, @CanBeNull TPointer srcAddr) {
    	return Modules.SysclibForKernelModule.strcpy(destAddr, srcAddr);
    }

    @HLEFunction(nid = 0xA0F16ABD, version = 150)
    public int sceNetStrcmp(@CanBeNull TPointer src1Addr, @CanBeNull TPointer src2Addr) {
    	return Modules.SysclibForKernelModule.strcmp(src1Addr, src2Addr);
    }

    @HLEFunction(nid = 0x94DCA9F0, version = 150)
    public int sceNetStrncmp(@CanBeNull TPointer src1Addr, @CanBeNull TPointer src2Addr, int size) {
    	return Modules.SysclibForKernelModule.strncmp(src1Addr, src2Addr, size);
    }

    @HLEFunction(nid = 0xB5CE388A, version = 150)
    public int sceNetStrncpy(@CanBeNull @BufferInfo(lengthInfo=LengthInfo.nextNextParameter, usage=Usage.out) TPointer destAddr, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer srcAddr, int size) {
    	return Modules.SysclibForKernelModule.strncpy(destAddr, srcAddr, size);
    }

    @HLEFunction(nid = 0xBCBE14CF, version = 150)
    public int sceNetStrchr(@CanBeNull TPointer srcAddr, int c1) {
    	if (srcAddr.isNull()) {
    		return 0;
    	}
    	c1 = c1 & 0xFF;

    	IMemoryReader memoryReader = MemoryReader.getMemoryReader(srcAddr.getAddress(), 1);
		for (int i = 0; true; i++) {
			int c2 = memoryReader.readNext();
			if (c1 == c2) {
				// Character found
				return srcAddr.getAddress() + i;
			} else if (c2 == 0) {
				// End of string
				break;
			}
		}

    	return 0;
    }

    @HLEFunction(nid = 0x750F705D, version = 150)
    public int sceNetLook_ctype_table(int c) {
    	int ctype = look_ctype_table[c & 0xFF];

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceNetLook_ctype_table c='%c' = 0x%02X", (char) c, ctype));
    	}

    	return ctype;
    }

    @HLEFunction(nid = 0x5705F6F9, version = 150)
    public int sceNetStrcat(@CanBeNull TPointer destAddr, @CanBeNull TPointer srcAddr) {
    	return Modules.SysclibForKernelModule.strcat(destAddr, srcAddr);
    }

    @HLEFunction(nid = 0x9CFBC7E3, version = 150)
    public int sceNetStrcasecmp(@CanBeNull PspString src1Addr, @CanBeNull PspString src2Addr) {
    	if (src1Addr.isNull() || src2Addr.isNull()) {
    		if (src1Addr.getAddress() == src2Addr.getAddress()) {
    			return 0;
    		}
    		if (src1Addr.isNotNull()) {
    			return 1;
    		}
    		return -1;
    	}

    	return src1Addr.getString().compareToIgnoreCase(src2Addr.getString());
    }

    @HLEFunction(nid = 0x96EF9DA1, version = 150)
    public int sceNetTolower(int c) {
    	int ctype = look_ctype_table[c & 0xFF];
    	if ((ctype & 0x01) != 0) {
    		c += 0x20;
    	}

    	return c;
    }

    @HLEFunction(nid = 0xC13C9307, version = 150)
    public int sceNetToupper(int c) {
    	int ctype = look_ctype_table[c & 0xFF];
    	if ((ctype & 0x02) != 0) {
    		c -= 0x20;
    	}

    	return c;
    }

    @HLEFunction(nid = 0xCF705E46, version = 150)
    public int sceNetSprintf(CpuState cpu, TPointer buffer, String format) {
    	return Modules.SysclibForKernelModule.sprintf(cpu, buffer, format);
    }

    @HLEFunction(nid = 0xB9085A96, version = 150)
    public int sceNetStrncasecmp(@CanBeNull @BufferInfo(lengthInfo=LengthInfo.nextNextParameter, usage=Usage.in) TPointer src1Addr, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer src2Addr, int size) {
    	if (src1Addr.isNull() || src2Addr.isNull()) {
    		if (src1Addr.getAddress() == src2Addr.getAddress()) {
    			return 0;
    		}
    		if (src1Addr.isNotNull()) {
    			return 1;
    		}
    		return -1;
    	}

    	String s1 = src1Addr.getStringNZ(size);
    	String s2 = src2Addr.getStringNZ(size);
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceNetStrncasecmp s1='%s', s2='%s'", s1, s2));
    	}

    	return s1.compareToIgnoreCase(s2);
    }

    /**
     * Convert a string to an integer. The base is 10.
     * 
     * @param string   the string to be converted
     * @return         the integer value represented by the string
     */
    @HLEFunction(nid = 0x1FB2FDDD, version = 150)
    public long sceNetAtoi(@CanBeNull PspString string) {
    	return Integer.parseInt(string.getString());
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2A73ADDC, version = 150)
    public long sceNetStrtoul(@CanBeNull PspString string, @CanBeNull TPointer32 endString, int base) {
    	return Modules.SysclibForKernelModule.strtoul(string, endString, base);
    }

    @HLEFunction(nid = 0xE0A81C7C, version = 150)
    public int sceNetMemcmp(@CanBeNull @BufferInfo(lengthInfo=LengthInfo.nextNextParameter, usage=Usage.in) TPointer src1Addr, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer src2Addr, int size) {
    	return Modules.SysclibForKernelModule.memcmp(src1Addr, src2Addr, size);
    }

    @HLEFunction(nid = 0xF48963C6, version = 150)
    public int sceNetStrrchr(@CanBeNull TPointer srcAddr, int c1) {
    	if (srcAddr.isNull()) {
    		return 0;
    	}
    	c1 = c1 & 0xFF;

    	IMemoryReader memoryReader = MemoryReader.getMemoryReader(srcAddr.getAddress(), 1);
    	int lastOccurence = -1;
		for (int i = 0; true; i++) {
			int c2 = memoryReader.readNext();
			if (c1 == c2) {
				// Character found
				lastOccurence = i;
			} else if (c2 == 0) {
				// End of string
				break;
			}
		}

		if (lastOccurence < 0) {
			return 0;
		}

		return srcAddr.getAddress() + lastOccurence;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x384EFE14, version = 150)
    public int sceNet_lib_384EFE14(@BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer in1Addr, int in1Size, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer in2Addr, int in2Size, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=20, usage=Usage.out) TPointer outAddr) {
    	if (in2Size > 64) {
    		log.warn(String.format("sceNet_lib_384EFE14 not implemented for size=0x%X", in2Size));
    	}

    	MessageDigest md;
        try {
			md = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			log.error("sceNet_lib_384EFE14", e);
			return -1;
		}

    	byte[] in1 = in1Addr.getArray8(in1Size);
    	byte[] in2 = in2Addr.getArray8(in2Size);

    	byte[] tmp1 = new byte[64];
    	byte[] tmp2 = new byte[64];
    	System.arraycopy(in2, 0, tmp1, 0, Math.min(in2Size, tmp1.length));
    	System.arraycopy(in2, 0, tmp2, 0, Math.min(in2Size, tmp2.length));
    	for (int i = 0; i < tmp1.length; i++) {
    		tmp1[i] = (byte) (tmp1[i] ^ 0x36);
    		tmp2[i] = (byte) (tmp2[i] ^ 0x5C);
    	}

		md.update(tmp1);
		md.update(in1);
		byte[] tmp3 = md.digest();
		md.reset();
		md.update(tmp2);
		md.update(tmp3);
		byte[] result = md.digest();

		outAddr.setArray(result, 20);

		return 0;
    }

    @HLEFunction(nid = 0x4753D878, version = 150)
    public int sceNetMemmove(@CanBeNull TPointer dstAddr, TPointer srcAddr, int size) {
    	return Modules.SysclibForKernelModule.memmove(dstAddr, srcAddr, size);
    }

    @HLEFunction(nid = 0x8687B5AB, version = 150)
    public int sceNetVsprintf(CpuState cpu, TPointer buffer, String format, TPointer32 parameters) {
    	Object[] formatParameters = new Object[10]; // Assume max. 10 parameters
    	IMemoryReader memoryReader = MemoryReader.getMemoryReader(parameters.getAddress(), 4 * formatParameters.length, 4);
    	for (int i = 0; i < formatParameters.length; i++) {
    		formatParameters[i] = memoryReader.readNext();
    	}

    	String formattedString = Modules.SysMemUserForUserModule.hleKernelSprintf(cpu, format, formatParameters);
		Utilities.writeStringZ(buffer.getMemory(), buffer.getAddress(), formattedString);

		if (log.isDebugEnabled()) {
			log.debug(String.format("sceNetVsprintf returning '%s'", formattedString));
		}

		return formattedString.length();
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1858883D, version = 150)
    public int sceNetRand() {
    	// Has no parameters
    	return new Random().nextInt();
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA93A93E9, version = 150)
    public int _sce_pspnet_callout_stop(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=32, usage=Usage.inout) TPointer unknown) {
    	return 0;
    }

	@HLEUnimplemented
	@HLEFunction(nid = 0xA8B6205A, version = 150)
	public int sceNet_lib_A8B6205A(TPointer unknown1, int unknown2, TPointer unknown3, int unknown4) {
		return 0;
	}
}