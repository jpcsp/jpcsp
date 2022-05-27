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

import static jpcsp.util.Utilities.endianSwap16;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import jpcsp.Allegrex.CpuState;
import jpcsp.Allegrex.compiler.RuntimeContext;
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
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.SceNetIfMessage;
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
	protected Map<Integer, Integer> allocatedThreadStructures;
	protected final Random random = new Random();
	protected int readCallback;
	protected int unknownCallback1;
	protected int adhocSocketAlertCallback;
	protected int getReadContextCallback;
	protected TPointer32 readContextAddr;
	protected TPointer readMessage;
	protected Map<Integer, Integer> blockedThreads;

	private class AfterReadContextCallback implements IAction {
		@Override
		public void execute() {
			hleAfterReadContextCallback();
		}
	}

	@Override
	public void start() {
		networkAdapter = NetworkAdapterFactory.createNetworkAdapter();
		networkAdapter.start();
		allocatedThreadStructures = new HashMap<Integer, Integer>();
		readCallback = 0;
		unknownCallback1 = 0;
		adhocSocketAlertCallback = 0;
		getReadContextCallback = 0;
		blockedThreads = new HashMap<Integer, Integer>();

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
    	return pspNetMacAddress.toString(macAddress);
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

    protected void sendDummyMessage(SceKernelThreadInfo thread) {
    	if (readContextAddr == null) {
    		int mem = Modules.sceNetIfhandleModule.hleNetMallocInternal(4);
    		if (mem > 0) {
    			readContextAddr = new TPointer32(Memory.getInstance(), mem);
    		}
    	}
    	if (readContextAddr != null) {
        	Modules.ThreadManForUserModule.executeCallback(thread, getReadContextCallback, new AfterReadContextCallback(), true, 0, 0, readContextAddr.getAddress());
    	}
    }

    protected void hleAfterReadContextCallback() {
    	if (readMessage == null) {
    		int size = 256;
    		int mem = Modules.sceNetIfhandleModule.hleNetMallocInternal(size);
    		if (mem > 0) {
    			readMessage = new TPointer(Memory.getInstance(), mem);
        		readMessage.clear(size);
        		RuntimeContext.debugMemory(mem, size);
    		}
    	}

    	if (readMessage != null) {
    		// Store dummy message
    		SceNetIfMessage message = new SceNetIfMessage();
    		TPointer data = new TPointer(Memory.getInstance(), readMessage.getAddress() + message.sizeof());
    		TPointer header = new TPointer(data.getMemory(), data.getAddress());
    		TPointer content = new TPointer(data.getMemory(), data.getAddress() + 60);
    		final int contentLength = 8;
    		// Header information:
    		header.setArray(0, Wlan.getMacAddress()); // destination MAC address
    		header.setArray(6, new byte[] { 0x11,  0x22, 0x33, 0x44, 0x55, 0x66 }); // source MAC address
    		header.setValue8(48, (byte) 1); // 1 or 2
    		header.setValue8(49, (byte) 0);
    		header.setValue16(50, (short) endianSwap16(12 + contentLength)); // value must be >= 12
    		header.setValue16(52, (short) endianSwap16(0x22C)); // source port
    		header.setValue16(54, (short) endianSwap16(0x22C)); // destination port
    		header.setValue8(58, (byte) 0);
    		header.setValue8(59, (byte) 0);

    		// Real message content:
    		content.setValue8(0, (byte) 1);
    		content.setValue8(1, (byte) 1);
    		content.setValue16(2, (short) endianSwap16(contentLength - 4)); // endian-swapped value, length of following data
    		content.setValue8(4, (byte) 0); // Dummy data
    		content.setValue8(5, (byte) 0);
    		content.setValue8(6, (byte) 0);
    		content.setValue8(7, (byte) 0);

    		message.dataAddr = data.getAddress();
    		message.dataLength = 60 + contentLength;
    		message.totalDataLength = 60 + contentLength;
    		message.write(readMessage);

    		TPointer readContext = new TPointer(Memory.getInstance(), readContextAddr.getValue());
    		readContext.setValue32(0, readMessage.getAddress());
    		readContext.setValue32(8, readContext.getValue32(8) + 1);
    	}

    	SceKernelThreadInfo thread = Modules.ThreadManForUserModule.getCurrentThread();
		Modules.ThreadManForUserModule.executeCallback(thread, readCallback, null, true);
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

    @HLEFunction(nid = 0x1858883D, version = 150)
    public int sceNetRand() {
    	// Has no parameters
    	return random.nextInt();
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA93A93E9, version = 150)
    public int _sce_pspnet_callout_stop(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=36, usage=Usage.inout) TPointer unknown) {
    	return 0;
    }

	@HLEUnimplemented
	@HLEFunction(nid = 0xA8B6205A, version = 150)
	public int sceNet_lib_A8B6205A(TPointer unknown1, int unknown2, TPointer unknown3, int unknown4) {
		return 0;
	}

    @HLEUnimplemented
    @HLEFunction(nid = 0x94B44F26, version = 150)
    public int _sce_pspnet_spllock() {
    	// Has no parameters
    	return 1;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x515B2F33, version = 150)
    public int _sce_pspnet_splunlock(int resultFromLock) {
    	if (resultFromLock <= 0) {
    		return resultFromLock;
    	}

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2E005032, version = 150)
    public int sceNet_lib_2E005032(int unknownCallback) {
    	this.adhocSocketAlertCallback = unknownCallback;

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB3A48B7F, version = 150)
    public int sceNet_lib_B3A48B7F(int readCallback, int unknownCallback1) {
    	this.readCallback = readCallback;
    	this.unknownCallback1 = unknownCallback1;

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1F94AFD9, version = 150)
    public int sceNet_lib_1F94AFD9(int unknownCallback) {
    	this.getReadContextCallback = unknownCallback;

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5216CBF5, version = 150)
    public int sceNetConfigUpInterface(PspString interfaceName) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD2422E4D, version = 150)
    public int sceNetConfigDownInterface(PspString interfaceName) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xAB7DD9A5, version = 150)
    public int sceNetConfigSetIfEventFlag(PspString interfaceName, int eventFlagUid, int bitsToSet) {
    	if (eventFlagUid == 0) {
    		return 0;
    	}
    	return Modules.ThreadManForUserModule.sceKernelSetEventFlag(eventFlagUid, bitsToSet);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xDA02F383, version = 150)
    public int sceNet_lib_DA02F383(PspString interfaceName, @BufferInfo(usage=Usage.out) TPointer32 unknown) {
    	unknown.setValue(0); // Unknown possible values

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD5B64E37, version = 150)
    public int sceNet_lib_D5B64E37(PspString interfaceName, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer ssid, int ssidLength, int adhocChannel) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x05D525E4, version = 150)
    public int sceNet_lib_05D525E4() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0A5A8751, version = 150)
    public int sceNet_lib_0A5A8751() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x25CC373A, version = 150)
    public int _sce_pspnet_callout_init() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x33B230BD, version = 150)
    public int sceNet_lib_33B230BD() {
    	// Has no parameters
    	adhocSocketAlertCallback = 0;
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6B294EE4, version = 150)
    public int sceNet_lib_6B294EE4(int unknown1, int unknown2) {
    	// calls adhocSocketAlertCallback
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x757085B0, version = 150)
    public int sceNet_lib_757085B0(TPointer unknown1, int unkown2) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7574FDA1, version = 150)
    public int _sce_pspnet_wakeup(TPointer32 receivedMessage) {
    	if (blockedThreads.containsKey(receivedMessage.getAddress())) {
    		int threadUid = blockedThreads.get(receivedMessage.getAddress());
    		Modules.ThreadManForUserModule.hleUnblockThread(threadUid);
    	}

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x949F1FBB, version = 150)
    public int sceNet_lib_949F1FBB() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCA3CF5EB, version = 150)
    public int _sce_pspnet_thread_enter(int optionalErrorAddr) {
    	// The parameter errorAddr is only present in sceNet_Library 1.02 or higher
    	TPointer32 errorAddr;
    	if (getModuleElfVersion() >= 0x0102) {
    		errorAddr = new TPointer32(getMemory(), optionalErrorAddr);
    	} else {
    		errorAddr = TPointer32.NULL;
    	}

    	int currentThreadId = Modules.ThreadManForUserModule.getCurrentThreadID();
    	if (!allocatedThreadStructures.containsKey(currentThreadId)) {
        	int size = 92;
        	int allocateMem = Modules.sceNetIfhandleModule.hleNetMallocInternal(size);
        	if (allocateMem < 0) {
        		errorAddr.setValue(allocateMem);
        		return 0;
        	}

        	RuntimeContext.debugMemory(allocateMem, size);

        	Memory.getInstance().memset(allocateMem, (byte) 0, size);

        	allocatedThreadStructures.put(currentThreadId, allocateMem);
    	}

    	errorAddr.setValue(0);

    	return allocatedThreadStructures.get(currentThreadId);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD60225A3, version = 150)
    public int sceNet_lib_D60225A3(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=6, usage=Usage.in) TPointer macAddr) {
    	pspNetMacAddress macAddress = new pspNetMacAddress();
    	macAddress.read(macAddr);

    	return 0x11223344;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF6DB0A0B, version = 150)
    public int sceNet_lib_F6DB0A0B(@BufferInfo(usage=Usage.out) TPointer32 receivedMessage, int timeout) {
    	// Possible return values are 0, 4, 11
    	// 4: sceNetAdhocPdpRecv will then return ERROR_NET_ADHOC_THREAD_ABORTED = 0x80410719
    	// 11: sceNetAdhocPdpRecv will then return ERROR_NET_ADHOC_TIMEOUT = 0x80410715
    	// 5: sceNetAdhocPdpRecv will then return ERROR_NET_ADHOC_SOCKET_ALERTED = 0x80410708
    	// 32: sceNetAdhocPdpRecv will then return ERROR_NET_ADHOC_SOCKET_DELETED = 0x80410707
    	SceKernelThreadInfo thread = Modules.ThreadManForUserModule.getCurrentThread();
    	thread.wait.Semaphore_id = -1;
    	Modules.ThreadManForUserModule.hleBlockCurrentThread(SceKernelThreadInfo.PSP_WAIT_SEMA);
    	blockedThreads.put(receivedMessage.getAddress(), thread.uid);

//    	sendDummyMessage(thread);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x03164B12, version = 150)
    public int sceNet_lib_03164B12() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0D633F53, version = 150)
    public int sceNet_lib_0D633F53() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x389728AB, version = 150)
    public int sceNet_lib_389728AB() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7BA3ED91, version = 150)
    public int sceNet_lib_7BA3ED91() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA55C914F, version = 150)
    public int sceNet_lib_A55C914F() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xAFA11338, version = 150)
    public int sceNet_lib_AFA11338() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB20F84F8, version = 150)
    public int sceNet_lib_B20F84F8() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD1BE2CE9, version = 150)
    public int sceNetConfigGetIfEvent(PspString interfaceName, @BufferInfo(usage=Usage.out) TPointer32 eventAddr, @BufferInfo(usage=Usage.out) TPointer32 unknown) {
    	// Possible return values in eventAddr:
    	// - 4 (WLAN switch off / 0x80410B03)
    	// - 5 (WLAN beacon lost / 0x80410B0E)
    	// - 7 (???)

    	// Returns 0x80410184 if no event is available?
    	return SceKernelErrors.ERROR_NET_NO_EVENT;
    }

    /*
     * Same as sceNetMemmove, but with src and dst pointers swapped
     */
    @HLEFunction(nid = 0x2F305274, version = 150)
    public int sceNetBcopy(@CanBeNull TPointer srcAddr, TPointer dstAddr, int size) {
    	return sceNetMemmove(dstAddr, srcAddr, size);
    }
}