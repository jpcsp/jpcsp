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

import static jpcsp.Allegrex.compiler.RuntimeContextLLE.getFirmwareVersion;
import static jpcsp.hardware.Wlan.MAC_ADDRESS_LENGTH;
import static jpcsp.util.Constants.charset;
import static jpcsp.util.Utilities.alignUp;
import static jpcsp.util.Utilities.read8;
import static jpcsp.util.Utilities.readCompleteFile;
import static jpcsp.util.Utilities.readStringNZ;
import static jpcsp.util.Utilities.readUnaligned16;
import static jpcsp.util.Utilities.readUnaligned32;
import static jpcsp.util.Utilities.write8;
import static jpcsp.util.Utilities.writeCompleteFile;
import static jpcsp.util.Utilities.writeStringNZ;
import static jpcsp.util.Utilities.writeUnaligned16;
import static jpcsp.util.Utilities.writeUnaligned32;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.modules.sceFont.FontRegistryEntry;
import jpcsp.hardware.Model;
import jpcsp.settings.Settings;
import jpcsp.util.Utilities;

public class sceReg extends HLEModule {
    public static Logger log = Modules.getLogger("sceReg");
    private static final String iregFileName = "flash1:/registry/system.ireg";
    private static final String dregFileName = "flash1:/registry/system.dreg";
    protected static final int DATA_BLOCK_SIZE = 512;
    protected static final int REG_TYPE_DIR = 1;
    protected static final int REG_TYPE_INT = 2;
    protected static final int REG_TYPE_STR = 3;
    protected static final int REG_TYPE_BIN = 4;
    protected static final int REG_MODE_READ_WRITE = 1;
    protected static final int REG_MODE_READ_ONLY = 2;
    private Map<Integer, RegistryHandle> registryHandles;
    private Map<Integer, CategoryHandle> categoryHandles;
    private Map<Integer, KeyHandle> keyHandles;
    private String authName;
    private String authKey;
    private int networkLatestId;
    private int wifiConnectCount;
    private int usbConnectCount;
    private int psnAccountCount;
    private int slideCount;
    private int bootCount;
    private int gameExecCount;
    private int oskVersionId;
    private int oskDispLocale;
    private int oskWritingLocale;
    private int oskInputCharMask;
    private int oskKeytopIndex;
    private String npEnv;
    private String adhocSsidPrefix;
    private int musicVisualizerMode;
    private int musicTrackInfoMode;
    private String lockPassword;
    private String browserHomeUri;
    private String npAccountId;
    private String npLoginId;
    private String npPassword;
    private int npAutoSignInEnable;
    private String ownerName;
    private Registry registry;
    private static MessageDigest sha1;

    protected static class Registry {
    	private byte[] ireg;
    	private byte[] dreg;
    	private RegistryHeader header;
    	private RegistryDirectoryHeader entries[];
    	private int nextFreeDataBlock;

    	public Registry(int numberDirectoryEntries, int numberDataBlocks) {
    		header = new RegistryHeader(numberDirectoryEntries);
    		ireg = new byte[numberDirectoryEntries * RegistryDirectoryHeader.SIZE_OF + RegistryHeader.SIZE_OF];
    		dreg = new byte[numberDataBlocks * DATA_BLOCK_SIZE];
    		entries = new RegistryDirectoryHeader[numberDirectoryEntries];
    	}

    	public Registry(byte[] ireg, byte[] dreg) {
    		this.ireg = ireg;
    		this.dreg = dreg;

    		read();
    	}

    	private void read() {
    		header = new RegistryHeader(ireg);

    		if (log.isDebugEnabled()) {
    			log.debug(String.format("RegistryHeader: %s, valid %b", header, header.isValid(ireg)));
    		}

    		if (header.isValid(ireg) ) {
    			entries = new RegistryDirectoryHeader[(ireg.length - RegistryHeader.SIZE_OF) / RegistryDirectoryHeader.SIZE_OF];
    			int offset = RegistryHeader.SIZE_OF;
    			for (int i = 0; i < entries.length; i++, offset += RegistryDirectoryHeader.SIZE_OF) {
    				RegistryDirectoryHeader entry = new RegistryDirectoryHeader(ireg, offset);
    				if (entry.numberBlocks != 0) {
    					entries[i] = entry;

    					if (log.isDebugEnabled()) {
    						log.debug(String.format("RegistryDirectoryHeader#%d: %s, valid checksum %b", i, entry, entry.isValidChecksum(dreg)));
    					}
    				}
    			}
    		}
    	}

    	public void save(String iregFileName, String dregFileName) {
			int offset = RegistryHeader.SIZE_OF;
    		for (int i = 0; i < entries.length; i++, offset += RegistryDirectoryHeader.SIZE_OF) {
    			if (entries[i] != null) {
    				entries[i].write(ireg, offset, dreg);
    			}
    		}

    		header.write(ireg);

    		writeCompleteFile(iregFileName, ireg, true);
    		writeCompleteFile(dregFileName, dreg, true);
    	}

    	public void dump() {
    		if (entries != null) {
	    		for (int i = 0; i < entries.length; i++) {
	    			if (entries[i] != null && !entries[i].hasParent()) {
	    				dumpChildren(i, 0);
	    			}
	    		}
    		}
    	}

    	private void dumpEntry(int n, int level) {
    		if (log.isDebugEnabled()) {
    			StringBuilder prefix = new StringBuilder();
    			for (int i = 0; i < level; i++) {
    				prefix.append("  ");
    			}
    			log.debug(String.format("%s%s: %s", prefix, getFullName(n), entries[n]));
    			byte[] dataBlocks = entries[n].getDataBlocks(dreg);
    			RegistryHeaderKeyEntry registryHeaderKeyEntry = new RegistryHeaderKeyEntry(dataBlocks);
    			int numberKeyEntries = Math.max(entries[n].numberChildren, registryHeaderKeyEntry.numberKeyEntries - 1);
    			for (int i = 0, offset = RegistryKeyEntry.SIZE_OF; i < numberKeyEntries; i++, offset += RegistryKeyEntry.SIZE_OF) {
    				RegistryKeyEntry keyEntry = new RegistryKeyEntry(dataBlocks, offset);

    				log.debug(String.format("%s  %s", prefix, keyEntry));
    			}
    		}
    	}

    	private void dumpChildren(int parent, int level) {
			dumpEntry(parent, level);
			for (int i = 0; i < entries.length; i++) {
				if (entries[i] != null && entries[i].parent == parent) {
					dumpChildren(i, level + 1);
				}
			}
    	}

    	private int getFreeDataBlock() {
    		return nextFreeDataBlock++;
    	}

    	private int hashName(String name) {
    		byte[] bytes = name.getBytes(charset);
    		int hash = 0;
    		if (bytes != null && bytes.length > 0) {
    			bytes = Utilities.add(bytes, (byte) 0);
    			int start = ((int) bytes[0]) * 100;
    			for (int i = 1; i < bytes.length; i += 2) {
    				int b = (int) bytes[i];
    				int n = hash + start + b;
    				hash = n - (((n + ((int) ((n * 0xFFFFFFFFD260BD6DL) >>> 32))) >> 14) + (n < 0 ? 1 : 0)) * 19937;
    			}
    		}

    		if (hash >= 0) {
    			hash %= header.numberDirectoryEntries;
    		} else {
    			hash = (int) ((hash & 0xFFFFFFFFL) % header.numberDirectoryEntries);
    		}

    		if (log.isDebugEnabled()) {
    			log.debug(String.format("hashName '%s' = 0x%X", name, hash));
    		}

    		return hash;
    	}

    	private String getFullName(int index) {
    		if (index == RegistryDirectoryHeader.NO_PARENT || entries[index] == null) {
    			return "";
    		}

    		return getFullName(entries[index].parent) + "/" + entries[index].name;
    	}

    	private String getFullName(int parent, String name) {
    		StringBuilder s = new StringBuilder();

    		if (parent != RegistryDirectoryHeader.NO_PARENT) {
    			s.append(getFullName(parent));
    		}
			s.append("/");
    		s.append(name);

    		return s.toString();
    	}

    	public int addDirectory(int parent, String name, int numberChildren, int numberBlocks) {
    		String fullName = getFullName(parent, name);
    		int nameHash = hashName(fullName);

    		// Find the next free entry if there is a collision
    		int index = nameHash;
    		while (entries[index] != null) {
    			index = (index + 1) % header.numberDirectoryEntries;
    		}

    		if (log.isDebugEnabled()) {
    			log.debug(String.format("Storing entry '%s' at index 0x%X", fullName, index));
    		}

    		RegistryDirectoryHeader registryDirectoryHeader = new RegistryDirectoryHeader(numberChildren, numberBlocks);
    		registryDirectoryHeader.parent = parent;
    		registryDirectoryHeader.name = name;
    		registryDirectoryHeader.numberBlocks = numberBlocks;
    		registryDirectoryHeader.nameHash = nameHash;
    		for (int i = 0; i < numberBlocks; i++) {
    			registryDirectoryHeader.blocks[i] = getFreeDataBlock();
    		}

    		entries[index] = registryDirectoryHeader;

    		return index;
    	}

    	private void addKey(int index, RegistryKeyEntry keyEntry, int valueSize) {
    		RegistryDirectoryHeader entry = entries[index];

    		int child = entry.getNextFreeChild();
    		int nextFreeValue = entry.getNextFreeValue(valueSize);

    		byte[] dataBlocks = entry.getDataBlocks(dreg);
    		keyEntry.write(dataBlocks, child, nextFreeValue);
    		entry.updateDataBlocks(dreg, dataBlocks);
    	}

    	public void addKey(int index, String name, int value) {
    		addKey(index, new RegistryKeyEntry(name, value), 0);
    	}

    	public void addKey(int index, String name, byte[] binValue) {
    		addKey(index, new RegistryKeyEntry(name, binValue), binValue.length);
    	}

    	public void addKey(int index, String name, byte[] binValue, int binValueLength) {
    		while (binValue.length < binValueLength) {
    			binValue = Utilities.add(binValue, (byte) 0);
    		}
    		addKey(index, name, binValue);
    	}

    	public void addKey(int index, String name, String stringValue, int maxSize) {
    		addKey(index, name, stringValue, stringValue.getBytes(charset).length + 1, maxSize);
    	}

    	public void addKey(int index, String name, String stringValue, int size, int maxSize) {
    		addKey(index, new RegistryKeyEntry(name, stringValue, size, maxSize), maxSize);
    	}

    	public void addKey(int index, String name) {
    		addKey(index, new RegistryKeyEntry(name), 0);
    	}
    }

    protected static class RegistryHeader {
    	public static final int SIZE_OF = 92;
    	public static final int MAGIC_IRF = 0x46524900; // ".IRF"
    	public int magic;
    	public int version;
    	public final byte checksum[] = new byte[20];
    	public int unknown28;
    	public int unknown32;
    	public int numberDirectoryEntries;
    	public int unknown40;
    	public int unknown44;
    	public int unknown48;
    	public int sizeDirectoryEntries;
    	public int headerSize;
    	public final byte unknown60[] = new byte[32];

    	public RegistryHeader(int numberDirectoryEntries) {
    		magic = MAGIC_IRF;
    		version = 0x10003;
    		this.numberDirectoryEntries = numberDirectoryEntries;
    		sizeDirectoryEntries = RegistryDirectoryHeader.SIZE_OF * numberDirectoryEntries;
    		headerSize = SIZE_OF;
    	}

    	public RegistryHeader(byte[] ireg) {
    		magic = readUnaligned32(ireg, 0);
    		version = readUnaligned32(ireg, 4);
    		System.arraycopy(ireg, 8, checksum, 0, checksum.length);
    		unknown28 = readUnaligned32(ireg, 28);
    		unknown32 = readUnaligned32(ireg, 32);
    		numberDirectoryEntries = readUnaligned32(ireg, 36);
    		unknown40 = readUnaligned32(ireg, 40);
    		unknown44 = readUnaligned32(ireg, 44);
    		unknown48 = readUnaligned32(ireg, 48);
    		sizeDirectoryEntries = readUnaligned32(ireg, 52);
    		headerSize = readUnaligned32(ireg, 56);
    		System.arraycopy(ireg, 60, unknown60, 0, unknown60.length);
    	}

    	public boolean isValid(byte[] ireg) {
    		return magic == MAGIC_IRF && isValidChecksum(ireg);
    	}

    	private byte[] computeChecksum(byte[] ireg) {
    		// Clear the checksum before computing the SHA-1 digest on the whole ireg
    		Arrays.fill(ireg, 8, 8 + checksum.length, (byte) 0);
    		return sha1(ireg);
    	}

    	public boolean isValidChecksum(byte[] ireg) {
    		return Arrays.equals(checksum, computeChecksum(ireg));
    	}

    	public void write(byte[] ireg) {
    		writeUnaligned32(ireg, 0, magic);
    		writeUnaligned32(ireg, 4, version);
    		writeUnaligned32(ireg, 28, unknown28);
    		writeUnaligned32(ireg, 32, unknown32);
    		writeUnaligned32(ireg, 36, numberDirectoryEntries);
    		writeUnaligned32(ireg, 40, unknown40);
    		writeUnaligned32(ireg, 44, unknown44);
    		writeUnaligned32(ireg, 48, unknown48);
    		writeUnaligned32(ireg, 52, sizeDirectoryEntries);
    		writeUnaligned32(ireg, 56, headerSize);
    		System.arraycopy(unknown60, 0, ireg, 60, unknown60.length);

    		// Compute the checksum after updating all fields
    		System.arraycopy(computeChecksum(ireg), 0, ireg, 8, checksum.length);
    	}

    	@Override
		public String toString() {
			return String.format("magic=0x%08X, version=0x%X, unknown28=0x%X, unknown32=0x%X, numberDirectoryEntries=0x%X, unknown40=0x%X, unknown44=0x%X, unknown48=0x%X, sizeDirectoryEntries=0x%X, headerSize=0x%X", magic, version, unknown28, unknown32, numberDirectoryEntries, unknown40, unknown44, unknown48, sizeDirectoryEntries, headerSize);
		}
    }

    protected static class RegistryDirectoryHeader {
    	public static final int SIZE_OF = 58;
    	// Up to firmware 1.52, top categories have a parent 0x0000.
    	// From firmware 2.00, top categories have a parent 0xFFFF.
    	public static final int NO_PARENT = getFirmwareVersion() <= 152 ? 0x0000 : 0xFFFF;
    	public int unknown0;
    	public int unknown1;
    	public int parent;
    	public int nameHash;
    	public int unknown3;
    	public int numberChildren;
    	public int numberBlocks;
    	public String name;
    	public int unknown4;
    	public final int blocks[] = new int[5];
    	public int unknown5;
    	private int nextFreeChild;
    	private RegistryHeaderKeyEntry registryHeaderKeyEntry;

    	public RegistryDirectoryHeader(int numberChildren, int numberBlocks) {
    		this.numberChildren = numberChildren;

    		registryHeaderKeyEntry = new RegistryHeaderKeyEntry(numberBlocks);
    		registryHeaderKeyEntry.numberKeyEntries = numberChildren + 1;

    		for (int i = 0; i < blocks.length; i++) {
    			blocks[i] = 0xFFFF;
    		}

    		nextFreeChild = 1;
    	}

    	public RegistryDirectoryHeader(byte[] ireg, int offset) {
    		unknown0 = readUnaligned16(ireg, offset + 0);
    		unknown1 = readUnaligned16(ireg, offset + 2);
    		parent = readUnaligned16(ireg, offset + 4);
    		nameHash = readUnaligned16(ireg, offset + 6);
    		unknown3 = readUnaligned16(ireg, offset + 8);
    		numberChildren = readUnaligned16(ireg, offset + 10);
    		numberBlocks = readUnaligned16(ireg, offset + 12);
    		name = readStringNZ(ireg, offset + 14, 28);
    		unknown4 = readUnaligned16(ireg, offset + 42);
    		for (int i = 0; i < blocks.length; i++) {
    			blocks[i] = readUnaligned16(ireg, offset + 44 + i * 2);
    		}
    		unknown5 = readUnaligned32(ireg, offset + 54);

    		registryHeaderKeyEntry = new RegistryHeaderKeyEntry(numberBlocks);
    		registryHeaderKeyEntry.numberKeyEntries = numberChildren + 1;
    		nextFreeChild = numberChildren + 1;
    	}

		public void write(byte[] ireg, int offset, byte[] dreg) {
			writeUnaligned16(ireg, offset + 0, unknown0);
			writeUnaligned16(ireg, offset + 2, unknown1);
			writeUnaligned16(ireg, offset + 4, parent);
			writeUnaligned16(ireg, offset + 6, nameHash);
			writeUnaligned16(ireg, offset + 8, unknown3);
			writeUnaligned16(ireg, offset + 10, numberChildren);
			writeUnaligned16(ireg, offset + 12, numberBlocks);
			writeStringNZ(ireg, offset + 14, 28, name);
			writeUnaligned16(ireg, offset + 42, unknown4);
			for (int i = 0; i < blocks.length; i++) {
				writeUnaligned16(ireg, offset + 44 + i * 2, blocks[i]);
			}
			writeUnaligned32(ireg, offset + 54, unknown5);

			byte[] dataBlocks = getDataBlocks(dreg);
			registryHeaderKeyEntry.write(dataBlocks);

			updateDataBlocks(dreg, dataBlocks);
		}

		public boolean isValidChecksum(byte[] dreg) {
			byte[] dataBlocks = getDataBlocks(dreg);

			return registryHeaderKeyEntry.isValidChecksum(dataBlocks);
		}

		public byte[] getDataBlocks(byte[] dreg) {
			byte[] dataBlocks = new byte[numberBlocks * DATA_BLOCK_SIZE];
			for (int i = 0; i < numberBlocks; i++) {
				System.arraycopy(dreg, blocks[i] * DATA_BLOCK_SIZE, dataBlocks, i * DATA_BLOCK_SIZE, DATA_BLOCK_SIZE);
			}

			return dataBlocks;
		}

		public void updateDataBlocks(byte[] dreg, byte[] dataBlocks) {
			for (int i = 0; i < numberBlocks; i++) {
				System.arraycopy(dataBlocks, i * DATA_BLOCK_SIZE, dreg, blocks[i] * DATA_BLOCK_SIZE, DATA_BLOCK_SIZE);
			}
		}

		public boolean hasParent() {
			return parent != NO_PARENT;
		}

		public int getNextFreeChild() {
			return nextFreeChild++;
		}

		public int getNextFreeValue(int length) {
			int index = -1;

			while (length > 0) {
				index = registryHeaderKeyEntry.getNextFreeDataKeyEntry();
				length -= RegistryKeyEntry.SIZE_OF;
			}

			return index;
		}

		@Override
		public String toString() {
			StringBuilder s = new StringBuilder();
			s.append(String.format("nameHash=0x%X, parent=0x%X, numberChildren=0x%X, numberBlocks=0x%X, name='%s'", nameHash, parent, numberChildren, numberBlocks, name));
			for (int i = 0; i < numberBlocks; i++) {
				s.append(String.format(", block[%d]=0x%X", i, blocks[i]));
			}
			return s.toString();
		}
    }

    protected static class RegistryHeaderKeyEntry {
    	public int numberKeyEntries;
    	public int firstFreeDataKey;
    	public int checksum;

    	public RegistryHeaderKeyEntry(int numberBlocks) {
    		firstFreeDataKey = numberBlocks * DATA_BLOCK_SIZE / RegistryKeyEntry.SIZE_OF - 1;
    	}

    	public RegistryHeaderKeyEntry(byte[] dataBlocks) {
    		numberKeyEntries = readUnaligned16(dataBlocks, 8);
    		firstFreeDataKey = readUnaligned16(dataBlocks, 10);
    		checksum = readUnaligned32(dataBlocks, 14);
    	}

		private static int computeChecksum(byte[] dataBlocks) {
			// Clear the checksum field before computing the SHA-1 digest
			writeUnaligned32(dataBlocks, 14, 0);

			byte[] digestBytes = sha1(dataBlocks);

			// Reduce the 20 bytes digest into one 32-bit value
			int checksum = 0;
			for (int i = 0, n = 0; i < 32; i += 8) {
				for (int j = 0; j < 5; j++, n++) {
					checksum ^= (digestBytes[n] & 0xFF) << i;
				}
			}

			return checksum;
		}

		public boolean isValidChecksum(byte[] dataBlocks) {
            return checksum == computeChecksum(dataBlocks);
		}

		public int getNextFreeDataKeyEntry() {
			return firstFreeDataKey--;
		}

		public void write(byte[] dataBlocks) {
    		writeUnaligned16(dataBlocks, 8, numberKeyEntries);
    		writeUnaligned16(dataBlocks, 10, firstFreeDataKey);

    		checksum = computeChecksum(dataBlocks);
    		writeUnaligned32(dataBlocks, 14, checksum);
    	}
    }

    protected static class RegistryKeyEntry {
    	public static final int SIZE_OF = 32;
    	public int type;
    	public String name;
    	public int intValue;
    	public int size;
    	public int maxKeyBlocks;
    	public String stringValue;
    	public byte[] binValue;

    	public RegistryKeyEntry(String name, int intValue) {
    		this.type = REG_TYPE_INT;
    		this.name = name;
    		this.intValue = intValue;
    	}

    	public RegistryKeyEntry(String name, String stringValue, int maxSize) {
    		this.type = REG_TYPE_STR;
    		this.name = name;
    		this.stringValue = stringValue;
    		maxKeyBlocks = maxSize / SIZE_OF;
			binValue = stringValue.getBytes(charset);
    		size = binValue.length + 1;
    	}

    	public RegistryKeyEntry(String name, String stringValue, int size, int maxSize) {
    		this.type = REG_TYPE_STR;
    		this.name = name;
    		this.stringValue = stringValue;
    		this.size = size;
    		maxKeyBlocks = maxSize / SIZE_OF;
			binValue = stringValue.getBytes(charset);
    	}

    	public RegistryKeyEntry(String name, byte[] binValue) {
    		this.type = REG_TYPE_BIN;
    		this.name = name;
    		this.binValue = binValue;
    		maxKeyBlocks = alignUp(binValue.length, SIZE_OF - 1) / SIZE_OF; 
    		size = binValue.length;
    	}

    	public RegistryKeyEntry(String name) {
    		this.type = REG_TYPE_DIR;
    		this.name = name;
    	}

    	public RegistryKeyEntry(byte[] dataBlocks, int offset) {
    		type = read8(dataBlocks, offset + 0);
    		name = readStringNZ(dataBlocks, offset + 1, 27);

    		int valueOffset;
    		switch (type) {
    			case REG_TYPE_INT:
    	    		intValue = readUnaligned32(dataBlocks, offset + 28);
    	    		break;
    			case REG_TYPE_BIN:
        			size = readUnaligned16(dataBlocks, offset + 28);
        			maxKeyBlocks = read8(dataBlocks, offset + 30);
        			binValue = new byte[size];
        			valueOffset = read8(dataBlocks, offset + 31) * SIZE_OF;
        			System.arraycopy(dataBlocks, valueOffset, binValue, 0, size);
    				break;
    			case REG_TYPE_STR:
        			size = readUnaligned16(dataBlocks, offset + 28);
        			maxKeyBlocks = read8(dataBlocks, offset + 30);
        			valueOffset = read8(dataBlocks, offset + 31) * SIZE_OF;
        			stringValue = readStringNZ(dataBlocks, valueOffset, size);
        			binValue = stringValue.getBytes(charset);
    				break;
    		}
    	}

    	public void write(byte[] dataBlocks, int child, int valueData) {
    		int offset = child * SIZE_OF;
    		int valueOffset = valueData * SIZE_OF;

    		write8(dataBlocks, offset + 0, type);
    		writeStringNZ(dataBlocks, offset + 1, 27, name);

    		switch (type) {
    			case REG_TYPE_INT:
    				writeUnaligned32(dataBlocks, offset + 28, intValue);
    				break;
    			case REG_TYPE_BIN:
    				writeUnaligned16(dataBlocks, offset + 28, size);
    				write8(dataBlocks, offset + 30, maxKeyBlocks);
    				write8(dataBlocks, offset + 31, valueData);
    				System.arraycopy(binValue, 0, dataBlocks, valueOffset, size);
    				break;
    			case REG_TYPE_STR:
    				writeUnaligned16(dataBlocks, offset + 28, size);
    				write8(dataBlocks, offset + 30, maxKeyBlocks);
    				write8(dataBlocks, offset + 31, valueData);
    				System.arraycopy(binValue, 0, dataBlocks, valueOffset, binValue.length);
    				Arrays.fill(dataBlocks, valueOffset + binValue.length, valueOffset + size, (byte) 0);
    				break;
    		}
    	}

    	@Override
		public String toString() {
			StringBuilder s = new StringBuilder(String.format("name='%s'", name));
			switch (type) {
				case REG_TYPE_INT:
					s.append(String.format(", value=0x%08X", intValue));
					break;
				case REG_TYPE_STR:
					s.append(String.format(", value='%s'", stringValue));
					if (size != binValue.length + 1) {
						s.append(String.format("(size=0x%X)", size));
					}
					s.append(String.format(", maxKeyBlocks=0x%X", maxKeyBlocks));
					break;
				case REG_TYPE_BIN:
					s.append(String.format(", value=%s", Utilities.getMemoryDump(binValue)));
					break;
				case REG_TYPE_DIR:
					s.append(String.format(", directory"));
					break;
				default:
					s.append(String.format(", type=0x%02X", type));
					break;
			}
			return s.toString();
		}
    }

    protected static class RegistryHandle {
    	private static final String registryHandlePurpose = "sceReg.RegistryHandle";
    	public int uid;
    	public int type;
    	public String name;
    	public int unknown1;
    	public int unknown2;

    	public RegistryHandle(int type, String name, int unknown1, int unknown2) {
			this.type = type;
			this.name = name;
			this.unknown1 = unknown1;
			this.unknown2 = unknown2;
			uid = SceUidManager.getNewUid(registryHandlePurpose);
		}

    	public void release() {
    		SceUidManager.releaseUid(uid, registryHandlePurpose);
    		uid = -1;
    	}
    }

    protected static class CategoryHandle {
    	private static final String categoryHandlePurpose = "sceReg.CategoryHandle";
    	public int uid;
    	public RegistryHandle registryHandle;
		public String name;
    	public int mode;

    	public CategoryHandle(RegistryHandle registryHandle, String name, int mode) {
			this.registryHandle = registryHandle;
			this.name = name;
			this.mode = mode;
			uid = SceUidManager.getNewUid(categoryHandlePurpose);
		}

    	public String getFullName() {
    		return registryHandle.name + name;
    	}

    	public void release() {
    		SceUidManager.releaseUid(uid, categoryHandlePurpose);
    		uid = -1;
    	}
    }

    protected static class KeyHandle {
    	private static int index = 0;
    	public int uid;
    	public String name;

    	public KeyHandle(String name) {
    		this.name = name;
			uid = index++;
    	}
    }

    private static byte[] sha1(byte[] input) {
    	return sha1(input, 0, input.length);
    }

    private static byte[] sha1(byte[] input, int offset, int length) {
    	sha1.reset();
    	sha1.update(input, offset, length);
		byte[] digestBytes = sha1.digest();

		return digestBytes;
    }

    public String getAuthName() {
		return authName;
	}

	public void setAuthName(String authName) {
		this.authName = authName;
	}

	public String getAuthKey() {
		return authKey;
	}

	public void setAuthKey(String authKey) {
		this.authKey = authKey;
	}

	public int getNetworkLatestId() {
		return networkLatestId;
	}

	public void setNetworkLatestId(int networkLatestId) {
		this.networkLatestId = networkLatestId;
	}

	public String getNpLoginId() {
		return npLoginId;
	}

	public String getNpPassword() {
		return npPassword;
	}

	private int getKey(CategoryHandle categoryHandle, String name, TPointer32 ptype, TPointer32 psize, TPointer buf, int size) {
    	String fullName = categoryHandle.getFullName();
    	fullName = fullName.replace("flash1:/registry/system", "");
    	fullName = fullName.replace("flash1/registry/system", "");
    	fullName = fullName.replace("flash2/registry/system", "");

    	Settings settings = Settings.getInstance();
    	if ("/system/DATA/FONT".equals(fullName) || "/DATA/FONT".equals(fullName)) {
    		List<sceFont.FontRegistryEntry> fontRegistry = Modules.sceFontModule.getFontRegistry();
    		if ("path_name".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(Modules.sceFontModule.getFontDirPath().length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, Modules.sceFontModule.getFontDirPath());
    			}
    		} else if ("num_fonts".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(fontRegistry.size());
    			}
    		} else {
    			log.warn(String.format("Unknown font registry entry '%s'", name));
    		}
    	} else if (fullName.startsWith("/system/DATA/FONT/PROPERTY/INFO") || fullName.startsWith("/DATA/FONT/PROPERTY/INFO")) {
    		List<sceFont.FontRegistryEntry> fontRegistry = Modules.sceFontModule.getFontRegistry();
    		int index = Integer.parseInt(fullName.substring(fullName.indexOf("INFO") + 4));
    		if (index < 0 || index >= fontRegistry.size()) {
    			return -1;
    		}
    		FontRegistryEntry entry = fontRegistry.get(index);
    		if ("h_size".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(entry.h_size);
    			}
    		} else if ("v_size".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(entry.v_size);
    			}
    		} else if ("h_resolution".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(entry.h_resolution);
    			}
    		} else if ("v_resolution".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(entry.v_resolution);
    			}
    		} else if ("extra_attributes".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(entry.extra_attributes);
    			}
    		} else if ("weight".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(entry.weight);
    			}
    		} else if ("family_code".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(entry.family_code);
    			}
    		} else if ("style".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(entry.style);
    			}
    		} else if ("sub_style".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(entry.sub_style);
    			}
    		} else if ("language_code".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(entry.language_code);
    			}
    		} else if ("region_code".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(entry.region_code);
    			}
    		} else if ("country_code".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(entry.country_code);
    			}
    		} else if ("file_name".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(entry.file_name == null ? 0 : entry.file_name.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, entry.file_name);
    			}
    		} else if ("font_name".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(entry.font_name == null ? 0 : entry.font_name.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, entry.font_name);
    			}
    		} else if ("expire_date".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(entry.expire_date);
    			}
    		} else if ("shadow_option".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(entry.shadow_option);
    			}
    		} else {
    			log.warn(String.format("Unknown font registry entry '%s'", name));
    		}
    	} else if ("/CONFIG/DATE".equals(fullName)) {
    		if ("date_format".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(settings.readInt("registry.date_format", 2));
    			}
    		} else if ("time_format".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(settings.readInt("registry.time_format", 0));
    			}
    		} else if ("time_zone_offset".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(settings.readInt("registry.time_zone_offset", 0));
    			}
    		} else if ("time_zone_area".equals(name)) {
				String timeZoneArea = settings.readString("registry.time_zone_area", "united_kingdom");
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(timeZoneArea.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, timeZoneArea);
    			}
    		} else if ("summer_time".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(settings.readInt("registry.summer_time", 0));
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/BROWSER".equals(fullName)) {
    		if ("flash_activated".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(1);
    			}
    		} else if ("home_uri".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(0x200);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, browserHomeUri);
    			}
    		} else if ("cookie_mode".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(1);
    			}
    		} else if ("proxy_mode".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(2);
    			}
    		} else if ("proxy_address".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(0x80);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, "");
    			}
    		} else if ("proxy_port".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("picture".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(1);
    			}
    		} else if ("animation".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("javascript".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(1);
    			}
    		} else if ("cache_size".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0x200); // Cache Size in KB
    			}
    		} else if ("char_size".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(1);
    			}
    		} else if ("disp_mode".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("flash_play".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(1);
    			}
    		} else if ("connect_mode".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("proxy_protect".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("proxy_autoauth".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("proxy_user".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(0x80);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, "");
    			}
    		} else if ("proxy_password".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(0x80);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, "");
    			}
    		} else if ("webpage_quality".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(1);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/BROWSER2".equals(fullName)) {
    		if ("tm_service".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("tm_ec_ttl".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else if ("tm_ec_ttl_update_time".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/NP".equals(fullName)) {
    		if ("account_id".equals(name)) {
    			ptype.setValue(REG_TYPE_BIN);
    			psize.setValue(16);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, npAccountId);
    			}
    		} else if ("login_id".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(npLoginId.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, npLoginId);
    			}
    		} else if ("password".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(npPassword.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, npPassword);
    			}
    		} else if ("auto_sign_in_enable".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(npAutoSignInEnable);
    			}
    		} else if ("nav_only".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else if ("env".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(npEnv.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, npEnv);
    			}
    		} else if ("guest_country".equals(name)) {
    			String guestCount = "";
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(guestCount.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, guestCount);
    			}
    		} else if ("view_mode".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else if ("check_drm".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/PREMO".equals(fullName)) {
    		if ("ps3_mac".equals(name)) {
    			byte ps3Mac[] = new byte[6];
    			ps3Mac[0] = 0x11;
    			ps3Mac[1] = 0x22;
    			ps3Mac[2] = 0x33;
    			ps3Mac[3] = 0x44;
    			ps3Mac[4] = 0x55;
    			ps3Mac[5] = 0x66;
    			ptype.setValue(REG_TYPE_BIN);
    			psize.setValue(ps3Mac.length);
    			if (size > 0) {
    				buf.setArray(ps3Mac, ps3Mac.length);
    			}
    		} else if ("ps3_name".equals(name)) {
    			String ps3Name = "My PS3";
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(ps3Name.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, ps3Name);
    			}
    		} else if ("guide_page".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else if ("response".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else if ("custom_video_buffer1".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else if ("custom_video_bitrate1".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else if ("setting_internet".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else if ("custom_video_buffer2".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else if ("custom_video_bitrate2".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else if ("button_assign".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else if ("ps3_keytype".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else if ("ps3_key".equals(name)) {
    			ptype.setValue(REG_TYPE_BIN);
    			psize.setValue(16);
    			if (size >= 16) {
    				buf.clear(16);
    			}
    		} else if ("flags".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else if ("account_id".equals(name)) {
    			ptype.setValue(REG_TYPE_BIN);
    			psize.setValue(16);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, npAccountId);
    			}
    		} else if ("login_id".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(npLoginId.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, npLoginId);
    			}
    		} else if ("password".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(npPassword.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, npPassword);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/SYSTEM".equals(fullName)) {
    		if ("exh_mode".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("umd_autoboot".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else if ("usb_auto_connect".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(1);
    			}
    		} else if ("owner_mob".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else if ("owner_dob".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else if ("umd_cache".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else if ("owner_name".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(ownerName.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, ownerName);
    			}
    		} else if ("slide_welcome".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(1);
    			}
    		} else if ("first_boot_tick".equals(name)) {
    			ptype.setValue(REG_TYPE_BIN);
    			String firstBootTick = "";
    			psize.setValue(firstBootTick.length());
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, firstBootTick);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/SYSTEM/SOUND".equals(fullName)) {
    		if ("dynamic_normalizer".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("operation_sound_mode".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(1);
    			}
    		} else if ("avls".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/SYSTEM/CHARACTER_SET".equals(fullName)) {
    		if ("oem".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(5);
    			}
    		} else if ("ansi".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0x13);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/SYSTEM/XMB".equals(fullName)) {
    		if ("language".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(sceUtility.getSystemParamLanguage());
    			}
    		} else if ("button_assign".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(sceUtility.getSystemParamButtonPreference());
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/SYSTEM/XMB/THEME".equals(fullName)) {
    		if ("wallpaper_mode".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(settings.readInt("registry.theme.wallpaper_mode", 0));
    			}
    		} else if ("custom_theme_mode".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(settings.readInt("registry.theme.custom_theme_mode", 0));
    			}
    		} else if ("color_mode".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(settings.readInt("registry.theme.color_mode", 0));
    			}
    		} else if ("system_color".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(settings.readInt("registry.theme.system_color", 0));
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/SYSPROFILE/RESOLUTION".equals(fullName)) {
    		if ("horizontal".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(8210);
    			}
    		} else if ("vertical".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(8210);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/ALARM".equals(fullName)) {
    		if (name.matches("alarm_\\d+_time")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(-1);
    			}
    		} else if (name.matches("alarm_\\d+_property")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/NETWORK/GO_MESSENGER".equals(fullName)) {
    		if (name.equals("auth_name")) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(authName.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, authName);
    			}
    		} else if (name.equals("auth_key")) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(authKey.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, authKey);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/NETWORK/ADHOC".equals(fullName)) {
    		if (name.equals("ssid_prefix")) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(adhocSsidPrefix.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, adhocSsidPrefix);
    			}
    		} else if (name.equals("channel")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(sceUtility.getSystemParamAdhocChannel());
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/NETWORK/INFRASTRUCTURE".equals(fullName)) {
    		if ("latest_id".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(networkLatestId);
    			}
    		} else if (name.equals("eap_md5")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if (name.equals("auto_setting")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if (name.equals("wifisvc_setting")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if (fullName.matches("/CONFIG/NETWORK/INFRASTRUCTURE/\\d+")) {
    		String indexName = fullName.replace("/CONFIG/NETWORK/INFRASTRUCTURE/", "");
    		int index = Integer.parseInt(indexName);
            if ("cnf_name".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			String cnfName = sceUtility.getNetParamName(index);
    			psize.setValue(cnfName.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, cnfName);
    			}
    		} else if ("ssid".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			String ssid = sceNetApctl.getSSID();
    			if (ssid.length() > 32) {
    				ssid = ssid.substring(0, 32);
    			}
    			psize.setValue(ssid.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, ssid);
    			}
    		} else if ("auth_proto".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
                    // 0 is no security.
                    // 1 is WEP (64bit).
                    // 2 is WEP (128bit).
                    // 3 is WPA.
    				buf.setValue32(1);
    			}
    		} else if ("wep_key".equals(name)) {
    			ptype.setValue(REG_TYPE_BIN);
    			String wepKey = "XXXXXXXXXXXXX"; // Max length is 13
    			psize.setValue(wepKey.length());
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, wepKey);
    			}
    		} else if ("how_to_set_ip".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
                    // 0 is DHCP.
                    // 1 is static.
                    // 2 is PPPOE.
    				buf.setValue32(0);
    			}
    		} else if ("dns_flag".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
                    // 0 is auto.
                    // 1 is manual.
    				buf.setValue32(0);
    			}
    		} else if ("primary_dns".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			String dns = sceNetApctl.getPrimaryDNS();
    			psize.setValue(dns.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, dns);
    			}
    		} else if ("secondary_dns".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			String dns = sceNetApctl.getSecondaryDNS();
    			psize.setValue(dns.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, dns);
    			}
    		} else if ("http_proxy_flag".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
                    // 0 is to not use proxy.
                    // 1 is to use proxy.
    				buf.setValue32(0);
    			}
    		} else if ("http_proxy_server".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			String httpProxyServer = "";
    			psize.setValue(httpProxyServer.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, httpProxyServer);
    			}
    		} else if ("http_proxy_port".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(80);
    			}
    		} else if ("version".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
                    // 0 is not used.
                    // 1 is old version.
                    // 2 is new version.
    				buf.setValue32(2);
    			}
    		} else if ("auth_8021x_type".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
                    // 0 is none.
                    // 1 is EAP (MD5).
    				buf.setValue32(0);
    			}
    		} else if ("browser_flag".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
                    // 0 is to not start the native browser.
                    // 1 is to start the native browser.
    				buf.setValue32(0);
    			}
    		} else if ("ip_address".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			String ip = sceNetApctl.getLocalHostIP();
    			psize.setValue(ip.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, ip);
    			}
    		} else if ("netmask".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			String netmask = sceNetApctl.getSubnetMask();
    			psize.setValue(netmask.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, netmask);
    			}
    		} else if ("default_route".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			String gateway = sceNetApctl.getGateway();
    			psize.setValue(gateway.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, gateway);
    			}
    		} else if (name.equals("device")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(1);
    			}
    		} else if (name.equals("auth_name")) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(authName.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, authName);
    			}
    		} else if (name.equals("auth_key")) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(authKey.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, authKey);
    			}
    		} else if (name.equals("auth_8021x_auth_name")) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(authName.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, authName);
    			}
    		} else if (name.equals("auth_8021x_auth_key")) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(authKey.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, authKey);
    			}
    		} else if ("wpa_key_type".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if (name.equals("wpa_key")) {
    			ptype.setValue(REG_TYPE_BIN);
    			String wpaKey = "";
    			psize.setValue(wpaKey.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, wpaKey);
    			}
    		} else if ("wifisvc_config".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if (fullName.matches("/CONFIG/NETWORK/INFRASTRUCTURE/\\d+/SUB1")) {
    		String indexName = fullName.replace("/CONFIG/NETWORK/INFRASTRUCTURE/", "");
    		int index = Integer.parseInt(indexName.substring(0, indexName.indexOf("/")));
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("/CONFIG/NETWORK/INFRASTRUCTURE, index=%d, SUB1", index));
    		}
    		if ("last_leased_dhcp_addr".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			String lastLeasedDhcpAddr = "";
    			psize.setValue(lastLeasedDhcpAddr.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, lastLeasedDhcpAddr);
    			}
    		} else if (name.equals("wifisvc_auth_name")) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(authName.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, authName);
    			}
    		} else if (name.equals("wifisvc_auth_key")) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(authKey.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, authKey);
    			}
    		} else if (name.equals("wifisvc_option")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if (name.equals("bt_id")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if (name.equals("at_command")) {
    			ptype.setValue(REG_TYPE_STR);
    			String atCommand = "";
    			psize.setValue(atCommand.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, atCommand);
    			}
    		} else if (name.equals("phone_number")) {
    			ptype.setValue(REG_TYPE_STR);
    			String phoneNumber = "";
    			psize.setValue(phoneNumber.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, phoneNumber);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/DATA/COUNT".equals(fullName)) {
    		if ("wifi_connect_count".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(wifiConnectCount);
    			}
    		} else if (name.equals("usb_connect_count")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(usbConnectCount);
    			}
    		} else if (name.equals("psn_access_count")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(psnAccountCount);
    			}
    		} else if (name.equals("slide_count")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(slideCount);
    			}
    		} else if (name.equals("boot_count")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(bootCount);
    			}
    		} else if (name.equals("game_exec_count")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(gameExecCount);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/SYSTEM/LOCK".equals(fullName)) {
    		if ("parental_level".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("browser_start".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("password".equals(name)) {
    			ptype.setValue(REG_TYPE_BIN);
    			psize.setValue(lockPassword.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, lockPassword);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/SYSTEM/POWER_SAVING".equals(fullName)) {
    		if ("backlight_off_interval".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("suspend_interval".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("wlan_mode".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("active_backlight_mode".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/TOOL/CONFIG".equals(fullName)) {
    		if ("np_debug".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(1);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/REGISTRY".equals(fullName)) {
    		if ("category_version".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(getRegistryCategoryVersion());
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/OSK".equals(fullName)) {
    		if ("version_id".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(oskVersionId);
    			}
    		} else if (name.equals("disp_locale")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(oskDispLocale);
    			}
    		} else if (name.equals("writing_locale")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(oskWritingLocale);
    			}
    		} else if (name.equals("input_char_mask")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(oskInputCharMask);
    			}
    		} else if (name.equals("keytop_index")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(oskKeytopIndex);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/MUSIC".equals(fullName)) {
    		if ("visualizer_mode".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(musicVisualizerMode);
    			}
    		} else if (name.equals("track_info_mode")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(musicTrackInfoMode);
    			}
    		} else if (name.equals("wma_play")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(1);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/PHOTO".equals(fullName)) {
    		if ("slideshow_speed".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(1);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/VIDEO".equals(fullName)) {
    		if ("lr_button_enable".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(1);
    			}
    		} else if ("list_play_mode".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(1);
    			}
    		} else if ("title_display_mode".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("sound_language".equals(name)) {
    			ptype.setValue(REG_TYPE_BIN);
    			psize.setValue(2);
    			if (size >= 2) {
    				buf.setValue8(0, (byte) '0');
    				buf.setValue8(1, (byte) '0');
    			}
    		} else if ("subtitle_language".equals(name)) {
    			ptype.setValue(REG_TYPE_BIN);
    			psize.setValue(2);
    			if (size >= 2) {
    				buf.setValue8(0, (byte) 'e');
    				buf.setValue8(1, (byte) 'n');
    			}
    		} else if ("menu_language".equals(name)) {
    			ptype.setValue(REG_TYPE_BIN);
    			psize.setValue(2);
    			if (size >= 2) {
    				buf.setValue8(0, (byte) 'e');
    				buf.setValue8(1, (byte) 'n');
    			}
    		} else if ("appended_volume".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/INFOBOARD".equals(fullName)) {
    		if ("locale_lang".equals(name)) {
    			String localeLang = "en/en/rss.xml";
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(localeLang.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, localeLang);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/CAMERA".equals(fullName)) {
    		if ("still_size".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(1);
    			}
    		} else if ("still_quality".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("movie_size".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(1);
    			}
    		} else if ("movie_quality".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("white_balance".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("exposure_bias".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("still_effect".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("file_folder".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0x65);
    			}
    		} else if ("file_number".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(1);
    			}
    		} else if ("movie_fps".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(1);
    			}
    		} else if ("shutter_sound_mode".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("file_number_eflash".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(1);
    			}
    		} else if ("folder_number_eflash".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0x65);
    			}
    		} else if ("msid".equals(name)) {
    			String msid = "";
    			ptype.setValue(REG_TYPE_BIN);
    			psize.setValue(16);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, msid);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/RSS".equals(fullName)) {
    		if ("download_items".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(5);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/DISPLAY".equals(fullName)) {
    		if ("color_space_mode".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("screensaver_start_time".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else {
			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    	}

    	return 0;
    }

	private int getRegistryCategoryVersion() {
		final int firmwareVersion = getFirmwareVersion();

		if (firmwareVersion <= 152) {
			// Firmware 1.50, 1.51, 1.52
			return 1;
		} else if (firmwareVersion <= 201) {
			// Firmware 2.00, 2.01
			return 6;
		} else if (firmwareVersion <= 250) {
			// Firmware 2.50
			return 19;
		} else if (firmwareVersion <= 260) {
			// Firmware 2.60
			return 20;
		} else if (firmwareVersion <= 271) {
			// Firmware 2.70, 2.71
			return 27;
		} else if (firmwareVersion <= 282) {
			// Firmware 2.80, 2.81, 2.82
			return 32;
		} else if (firmwareVersion <= 303) {
			// Firmware 3.00, 3.01, 3.02, 3.03
			return 38;
		} else if (firmwareVersion <= 311) {
			// Firmware 3.10, 3.11
			return 44;
		} else if (firmwareVersion <= 340) {
			// Firmware 3.30, 3.40
			return 49;
		} else if (firmwareVersion <= 352) {
			// Firmware 3.50, 3.51, 3.52
			return 53;
		} else if (firmwareVersion <= 360) {
			// Firmware 3.60
			return 54;
		} else if (firmwareVersion <= 373) {
			// Firmware 3.70, 3.71, 3.72, 3.73
			return 59;
		} else if (firmwareVersion <= 393) {
			// Firmware 3.80, 3.90, 3.93
			return 62;
		} else if (firmwareVersion <= 401) {
			// Firmware 3.95, 3.96, 4.00, 4.01
			return 64;
		} else if (firmwareVersion <= 405) {
			// Firmware 4.05
			return 67;
		} else if (firmwareVersion <= 505) {
			// Firmware 5.00, 5.01, 5.02, 5.03, 5.05
			return 76;
		} else if (firmwareVersion <= 555) {
			// Firmware 5.50, 5.51, 5.55
			return 88;
		} else if (firmwareVersion <= 570) {
			// Firmware 5.70
			return 97;
		} else if (firmwareVersion <= 610) {
			// Firmware 6.00, 6.10
			return 99;
		} else if (firmwareVersion <= 620) {
			// Firmware 6.20
			return 100;
		}
		// Firmware 6.30, 6.31, 6.35, 6.36, 6.37, 6.38, 6.39, 6.60, 6.61
		return 102;
	}

	private void createEmptyRegistry() {
    	Settings settings = Settings.getInstance();
		Registry registry = new Registry(256, 256);
		int index;
		int parent;

		index = registry.addDirectory(RegistryDirectoryHeader.NO_PARENT, "REGISTRY", 1, 1);
		registry.addKey(index, "category_version", getRegistryCategoryVersion());

		index = registry.addDirectory(RegistryDirectoryHeader.NO_PARENT, "CONFIG", 20, 2);
		registry.addKey(index, "VIDEO");
		registry.addKey(index, "PHOTO");
		registry.addKey(index, "MUSIC");
		registry.addKey(index, "BROWSER");
		registry.addKey(index, "LFTV");
		registry.addKey(index, "RSS");
		registry.addKey(index, "ALARM");
		registry.addKey(index, "PREMO");
		registry.addKey(index, "CAMERA");
		registry.addKey(index, "DISPLAY");
		registry.addKey(index, "NP");
		registry.addKey(index, "ONESEG");
		registry.addKey(index, "SYSTEM");
		registry.addKey(index, "DATE");
		registry.addKey(index, "NETWORK");
		registry.addKey(index, "BROWSER2");
		registry.addKey(index, "OSK");
		registry.addKey(index, "INFOBOARD");
		registry.addKey(index, "BT");
		registry.addKey(index, "GAME");

		parent = index;
		index = registry.addDirectory(parent, "BROWSER2", 4, 1);
		registry.addKey(index, "tm_service", 0);
		registry.addKey(index, "tm_ec_ttl", 0);
		registry.addKey(index, "tm_ec_ttl_update_time", new byte[8]);
		registry.addKey(index, "tm_service_sub_status", 0);

		index = registry.addDirectory(parent, "VIDEO", 9, 1);
		registry.addKey(index, "menu_language", "en".getBytes(charset));
		registry.addKey(index, "sound_language", "00".getBytes(charset));
		registry.addKey(index, "subtitle_language", "en".getBytes(charset));
		registry.addKey(index, "appended_volume", 0);
		registry.addKey(index, "lr_button_enable", 1);
		registry.addKey(index, "list_play_mode", 1);
		registry.addKey(index, "title_display_mode", 0);
		registry.addKey(index, "output_ext_menu", 0);
		registry.addKey(index, "output_ext_func", 0);

		index = registry.addDirectory(parent, "NP", 15, 2);
		registry.addKey(index, "env", npEnv, 0x20);
		registry.addKey(index, "account_id", npAccountId.getBytes(charset), 16);
		registry.addKey(index, "login_id", npLoginId, 0x60);
		registry.addKey(index, "password", npPassword, 0x20);
		registry.addKey(index, "auto_sign_in_enable", npAutoSignInEnable);
		registry.addKey(index, "nav_only", 0);
		registry.addKey(index, "np_ad_clock_diff", 0);
		registry.addKey(index, "view_mode", 0);
		registry.addKey(index, "np_geo_filtering", 0);
		registry.addKey(index, "guest_country", "", 0x20);
		registry.addKey(index, "guest_lang", "", 0x20);
		registry.addKey(index, "guest_yob", 0);
		registry.addKey(index, "guest_mob", 0);
		registry.addKey(index, "guest_dob", 0);
		registry.addKey(index, "check_drm", 0);

		index = registry.addDirectory(parent, "PHOTO", 1, 1);
		registry.addKey(index, "slideshow_speed", 1);

		index = registry.addDirectory(parent, "BT", 9, 1);
		registry.addKey(index, "connect_mode", 0);
		for (int i = 0; i < 8; i++) {
			registry.addKey(index, String.format("DEVICE%d", i));
		}
		for (int i = 0; i < 8; i++) {
			int deviceIndex = registry.addDirectory(index, String.format("DEVICE%d", i), 3, 1);
			registry.addKey(deviceIndex, "audio_type", 1);
			registry.addKey(deviceIndex, "device_type", 0);
			registry.addKey(deviceIndex, "device_name", new byte[64]);
		}

		index = registry.addDirectory(parent, "MUSIC", 3, 1);
		registry.addKey(index, "wma_play", 1);
		registry.addKey(index, "visualizer_mode", musicVisualizerMode);
		registry.addKey(index, "track_info_mode", musicTrackInfoMode);

		index = registry.addDirectory(parent, "ALARM", 20, 2);
		for (int i = 0; i < 10; i++) {
			registry.addKey(index, String.format("alarm_%d_time", i), -1);
		}
		for (int i = 0; i < 10; i++) {
			registry.addKey(index, String.format("alarm_%d_property", i), 0);
		}

		index = registry.addDirectory(parent, "PREMO", 16, 2);
		registry.addKey(index, "guide_page", 1);
		registry.addKey(index, "response", 0);
		registry.addKey(index, "ps3_name", "", 0x80);
		registry.addKey(index, "ps3_mac", new byte[MAC_ADDRESS_LENGTH]);
		registry.addKey(index, "ps3_keytype", 1);
		registry.addKey(index, "ps3_key", new byte[16]);
		registry.addKey(index, "custom_video_bitrate1", 1);
		registry.addKey(index, "custom_video_bitrate2", 1);
		registry.addKey(index, "custom_video_buffer1", 1);
		registry.addKey(index, "custom_video_buffer2", 1);
		registry.addKey(index, "setting_internet", 1);
		registry.addKey(index, "button_assign", 1);
		registry.addKey(index, "flags", 1);
		registry.addKey(index, "account_id", new byte[16]);
		registry.addKey(index, "login_id", "", 0x60);
		registry.addKey(index, "password", "", 0x20);

		index = registry.addDirectory(parent, "CAMERA", 15, 2);
		registry.addKey(index, "still_size", 1);
		registry.addKey(index, "movie_size", 1);
		registry.addKey(index, "still_quality", 0);
		registry.addKey(index, "movie_quality", 0);
		registry.addKey(index, "movie_fps", 1);
		registry.addKey(index, "white_balance", 0);
		registry.addKey(index, "exposure_bias", 0);
		registry.addKey(index, "shutter_sound_mode", 0);
		registry.addKey(index, "file_folder", 101);
		registry.addKey(index, "file_number", 1);
		registry.addKey(index, "msid", new byte[16]);
		registry.addKey(index, "still_effect", 0);
		registry.addKey(index, "medium_type", 0);
		registry.addKey(index, "file_number_eflash", 1);
		registry.addKey(index, "folder_number_eflash", 101);

		index = registry.addDirectory(parent, "ONESEG", 1, 1);
		registry.addKey(index, "schedule_data_key", new byte[16]);

		index = registry.addDirectory(parent, "RSS", 1, 1);
		registry.addKey(index, "download_items", 5);

		index = registry.addDirectory(parent, "OSK", 5, 1);
		registry.addKey(index, "version_id", oskVersionId);
		registry.addKey(index, "disp_locale", oskDispLocale);
		registry.addKey(index, "writing_locale", oskWritingLocale);
		registry.addKey(index, "input_char_mask", oskInputCharMask);
		registry.addKey(index, "keytop_index", oskKeytopIndex);

		index = registry.addDirectory(parent, "SYSTEM", 17, 2);
		registry.addKey(index, "owner_name", ownerName, 0x80);
		int backlightBrightness = 3;
		if (Model.getGeneration() == 11) {
			// The maximum backlight brightness is 2 on a PSP-E1000 (Street)
			backlightBrightness = Math.min(backlightBrightness, 2);
		}
		registry.addKey(index, "backlight_brightness", backlightBrightness);
		registry.addKey(index, "umd_autoboot", 0);
		registry.addKey(index, "usb_charge", 1);
		registry.addKey(index, "umd_cache", 1);
		registry.addKey(index, "usb_auto_connect", 1);
		registry.addKey(index, "XMB");
		registry.addKey(index, "SOUND");
		registry.addKey(index, "POWER_SAVING");
		registry.addKey(index, "LOCK");
		registry.addKey(index, "CHARACTER_SET");
		registry.addKey(index, "slide_action", 0);
		registry.addKey(index, "first_boot_tick", new byte[64]);
		registry.addKey(index, "owner_mob", 0);
		registry.addKey(index, "owner_dob", 0);
		registry.addKey(index, "slide_welcome", 0);
		registry.addKey(index, "exh_mode", 0);

		int parentSystem = index;
		index = registry.addDirectory(parentSystem, "XMB", 4, 1);
		registry.addKey(index, "theme_type", 0);
		registry.addKey(index, "language", 1);
		registry.addKey(index, "button_assign", 1);
		registry.addKey(index, "THEME");

		int parentXmb = index;
		index = registry.addDirectory(parentXmb, "THEME", 4, 1);
		registry.addKey(index, "color_mode", settings.readInt("registry.theme.color_mode", 0));
		registry.addKey(index, "wallpaper_mode", settings.readInt("registry.theme.wallpaper_mode", 0));
		registry.addKey(index, "system_color", settings.readInt("registry.theme.system_color", 0));
		registry.addKey(index, "custom_theme_mode", settings.readInt("registry.theme.custom_theme_mode", 0));

		index = registry.addDirectory(parentSystem, "POWER_SAVING", 4, 1);
		registry.addKey(index, "suspend_interval", 600);
		registry.addKey(index, "backlight_off_interval", 300);
		registry.addKey(index, "wlan_mode", 0);
		registry.addKey(index, "active_backlight_mode", 0);

		index = registry.addDirectory(parentSystem, "LOCK", 3, 1);
		registry.addKey(index, "password", lockPassword.getBytes(charset));
		registry.addKey(index, "parental_level", 0);
		registry.addKey(index, "browser_start", 0);

		index = registry.addDirectory(parentSystem, "CHARACTER_SET", 2, 1);
		registry.addKey(index, "oem", 5);
		registry.addKey(index, "ansi", 19);

		index = registry.addDirectory(parentSystem, "SOUND", 6, 1);
		registry.addKey(index, "main_volume", 22);
		registry.addKey(index, "mute", 0);
		registry.addKey(index, "avls", 0);
		registry.addKey(index, "equalizer_mode", 0);
		registry.addKey(index, "operation_sound_mode", 1);
		registry.addKey(index, "dynamic_normalizer", 0);

		index = registry.addDirectory(parent, "INFOBOARD", 2, 1);
		registry.addKey(index, "locale_lang", "en/en/rss.xml", 0x10);
		registry.addKey(index, "qa_server", 0);

		index = registry.addDirectory(parent, "DATE", 5, 1);
		registry.addKey(index, "time_format", settings.readInt("registry.time_format", 0));
		registry.addKey(index, "date_format", settings.readInt("registry.date_format", 2));
		registry.addKey(index, "summer_time", settings.readInt("registry.summer_time", 0));
		registry.addKey(index, "time_zone_offset", settings.readInt("registry.time_zone_offset", 0));
		registry.addKey(index, "time_zone_area", settings.readString("registry.time_zone_area", "united_kingdom"), 0x40);

		index = registry.addDirectory(parent, "GAME", 3, 1);
		registry.addKey(index, "hibernation_ow_guide", 1);
		registry.addKey(index, "hibernation_op_guide", 1);
		registry.addKey(index, "subs_expiration_guide", 1);

		index = registry.addDirectory(parent, "DISPLAY", 5, 1);
		registry.addKey(index, "aspect_ratio", 0);
		registry.addKey(index, "scan_mode", 0);
		registry.addKey(index, "screensaver_start_time", 600);
		registry.addKey(index, "color_space_mode", 0);
		registry.addKey(index, "pi_blending_mode", 1);

		index = registry.addDirectory(parent, "LFTV", 32, 4);
		registry.addKey(index, "easy_reg_done", 0);
		registry.addKey(index, "netav_domain_name", "", 0x100);
		registry.addKey(index, "netav_ip_address", "", 0x20);
		registry.addKey(index, "netav_port_no_home", 5021);
		registry.addKey(index, "netav_port_no_away", 5021);
		registry.addKey(index, "netav_nonce", "", 0x40);
		registry.addKey(index, "base_station_version", "0.000", 0x20);
		registry.addKey(index, "base_station_region", 0);
		registry.addKey(index, "tuner_type", 0);
		registry.addKey(index, "input_line", 0);
		registry.addKey(index, "tv_channel", 0);
		registry.addKey(index, "bitrate_home", 0);
		registry.addKey(index, "bitrate_away", 0);
		registry.addKey(index, "channel_setting_jp", new byte[24]);
		registry.addKey(index, "channel_setting_us", new byte[68]);
		registry.addKey(index, "channel_setting_us_catv", new byte[125]);
		registry.addKey(index, "overwrite_netav_setting", 1);
		registry.addKey(index, "screen_mode", 0);
		registry.addKey(index, "remocon_setting_region", 0);
		registry.addKey(index, "remocon_setting", new byte[96]);
		registry.addKey(index, "remocon_setting_revision", "0000.00", 0x20);
		registry.addKey(index, "external_tuner_channel", 0);
		registry.addKey(index, "ssid", "", 0x40);
		registry.addKey(index, "audio_gain", 0);
		registry.addKey(index, "broadcast_standard_video1", 0);
		registry.addKey(index, "broadcast_standard_video2", 0);
		registry.addKey(index, "version", 0);
		registry.addKey(index, "tv_channel_range", 0);
		registry.addKey(index, "tuner_type_no", 0);
		registry.addKey(index, "input_line_no", 0);
		registry.addKey(index, "audio_channel", 0);
		registry.addKey(index, "shared_remocon_setting", new byte[96]);

		index = registry.addDirectory(parent, "BROWSER", 19, 4);
		registry.addKey(index, "home_uri", browserHomeUri, 0x200, 0x400);
		registry.addKey(index, "cookie_mode", 1);
		registry.addKey(index, "proxy_mode", 2);
		registry.addKey(index, "proxy_address", "", 0x80, 0x80);
		registry.addKey(index, "proxy_port", 0);
		registry.addKey(index, "picture", 1);
		registry.addKey(index, "animation", 0);
		registry.addKey(index, "javascript", 1);
		registry.addKey(index, "cache_size", 0x200);
		registry.addKey(index, "char_size", 1);
		registry.addKey(index, "disp_mode", 0);
		registry.addKey(index, "connect_mode", 0);
		registry.addKey(index, "flash_activated", 1);
		registry.addKey(index, "flash_play", 1);
		registry.addKey(index, "proxy_protect", 0);
		registry.addKey(index, "proxy_autoauth", 0);
		registry.addKey(index, "proxy_user", "", 0x80, 0x80);
		registry.addKey(index, "proxy_password", "", 0x80, 0x80);
		registry.addKey(index, "webpage_quality", 1);

		index = registry.addDirectory(parent, "NETWORK", 3, 1);
		registry.addKey(index, "ADHOC");
		registry.addKey(index, "INFRASTRUCTURE");
		registry.addKey(index, "GO_MESSENGER");

		int parentNetwork = index;
		index = registry.addDirectory(parentNetwork, "INFRASTRUCTURE", 6, 1);
		registry.addKey(index, "latest_id", networkLatestId);
		registry.addKey(index, "eap_md5", 0);
		registry.addKey(index, "auto_setting", 2);
		registry.addKey(index, "wifisvc_setting", 0);
		registry.addKey(index, "btdun_warnings_check", 0);
		registry.addKey(index, "0");

		int parentInfrastructure = index;
		index = registry.addDirectory(parentInfrastructure, "0", 26, 4);
		registry.addKey(index, "SUB1");
		registry.addKey(index, "version", 5);
		registry.addKey(index, "device", 1);
		registry.addKey(index, "cnf_name", "", 0x40);
		registry.addKey(index, "ssid", "", 0x40);
		registry.addKey(index, "auth_proto", 0);
		registry.addKey(index, "wep_key", new byte[5]);
		registry.addKey(index, "how_to_set_ip", 0);
		registry.addKey(index, "ip_address", "", 0x20);
		registry.addKey(index, "netmask", "", 0x20);
		registry.addKey(index, "default_route", "", 0x20);
		registry.addKey(index, "dns_flag", 0);
		registry.addKey(index, "primary_dns", "", 0x20);
		registry.addKey(index, "secondary_dns", "", 0x20);
		registry.addKey(index, "auth_name", authName, 0x80);
		registry.addKey(index, "auth_key", authKey, 0x80);
		registry.addKey(index, "http_proxy_flag", 0);
		registry.addKey(index, "http_proxy_server", "", 0x80);
		registry.addKey(index, "http_proxy_port", 8080);
		registry.addKey(index, "auth_8021x_type", 0);
		registry.addKey(index, "auth_8021x_auth_name", authName, 0x80);
		registry.addKey(index, "auth_8021x_auth_key", authKey, 0x80);
		registry.addKey(index, "wpa_key_type", 0);
		registry.addKey(index, "wpa_key", new byte[64]);
		registry.addKey(index, "browser_flag", 0);
		registry.addKey(index, "wifisvc_config", 0);

		int parent0 = index;
		index = registry.addDirectory(parent0, "SUB1", 7, 2);
		registry.addKey(index, "wifisvc_auth_name", authName, 0x80);
		registry.addKey(index, "wifisvc_auth_key", authKey, 0x80);
		registry.addKey(index, "wifisvc_option", 0);
		registry.addKey(index, "last_leased_dhcp_addr", "", 0x20);
		registry.addKey(index, "bt_id", 0);
		registry.addKey(index, "at_command", "", 0x60);
		registry.addKey(index, "phone_number", "", 0x40);

		// Create 2 dummy infrastructure entries
		for (int i = 1; i <= 2; i++) {
			index = registry.addDirectory(parentInfrastructure, Integer.toString(i), 26, 4);
			registry.addKey(index, "SUB1");
			registry.addKey(index, "version", 5);
			registry.addKey(index, "device", 1);
			registry.addKey(index, "cnf_name", String.format("Jpcsp %d", i), 0x40);
			registry.addKey(index, "ssid", String.format("Jpcsp SSID %d", i), 0x40);
			registry.addKey(index, "auth_proto", 0);
			registry.addKey(index, "wep_key", new byte[5]);
			registry.addKey(index, "how_to_set_ip", 0);
			registry.addKey(index, "ip_address", "", 0x20);
			registry.addKey(index, "netmask", "", 0x20);
			registry.addKey(index, "default_route", "", 0x20);
			registry.addKey(index, "dns_flag", 0);
			registry.addKey(index, "primary_dns", "", 0x20);
			registry.addKey(index, "secondary_dns", "", 0x20);
			registry.addKey(index, "auth_name", authName, 0x80);
			registry.addKey(index, "auth_key", authKey, 0x80);
			registry.addKey(index, "http_proxy_flag", 0);
			registry.addKey(index, "http_proxy_server", "", 0x80);
			registry.addKey(index, "http_proxy_port", 8080);
			registry.addKey(index, "auth_8021x_type", 0);
			registry.addKey(index, "auth_8021x_auth_name", authName, 0x80);
			registry.addKey(index, "auth_8021x_auth_key", authKey, 0x80);
			registry.addKey(index, "wpa_key_type", 0);
			registry.addKey(index, "wpa_key", new byte[64]);
			registry.addKey(index, "browser_flag", 0);
			registry.addKey(index, "wifisvc_config", 0);

			int parentN = index;
			index = registry.addDirectory(parentN, "SUB1", 7, 2);
			registry.addKey(index, "wifisvc_auth_name", authName, 0x80);
			registry.addKey(index, "wifisvc_auth_key", authKey, 0x80);
			registry.addKey(index, "wifisvc_option", 0);
			registry.addKey(index, "last_leased_dhcp_addr", "", 0x20);
			registry.addKey(index, "bt_id", 0);
			registry.addKey(index, "at_command", "", 0x60);
			registry.addKey(index, "phone_number", "", 0x40);
		}

		index = registry.addDirectory(parentNetwork, "GO_MESSENGER", 2, 1);
		registry.addKey(index, "auth_name", authName, 0x40, 0x40);
		registry.addKey(index, "auth_key", authKey, 0x40, 0x40);

		index = registry.addDirectory(parentNetwork, "ADHOC", 2, 1);
		registry.addKey(index, "channel", 0);
		registry.addKey(index, "ssid_prefix", adhocSsidPrefix, 0x20);

		index = registry.addDirectory(RegistryDirectoryHeader.NO_PARENT, "DATA", 2, 1);
		registry.addKey(index, "FONT");
		registry.addKey(index, "COUNT");

		List<FontRegistryEntry> fontRegistry = Modules.sceFontModule.getFontRegistry();
		parent = index;
		index = registry.addDirectory(parent, "FONT", 3, 1);
		registry.addKey(index, "path_name", Modules.sceFontModule.getFontDirPath(), 0x40);
		registry.addKey(index, "num_fonts", fontRegistry.size());
		registry.addKey(index, "PROPERTY");

		int parentFont = index;
		index = registry.addDirectory(parentFont, "PROPERTY", fontRegistry.size(), 2);
		for (int i = 0; i < fontRegistry.size(); i++) {
			registry.addKey(index, String.format("INFO%d", i));
		}

		int parentProperty = index;
		for (int i = 0; i < fontRegistry.size(); i++) {
			FontRegistryEntry fontRegistryEntry = fontRegistry.get(i);

			index = registry.addDirectory(parentProperty, String.format("INFO%d", i), 16, 2);
			registry.addKey(index, "h_size", fontRegistryEntry.h_size);
			registry.addKey(index, "v_size", fontRegistryEntry.v_size);
			registry.addKey(index, "h_resolution", fontRegistryEntry.h_resolution);
			registry.addKey(index, "v_resolution", fontRegistryEntry.v_resolution);
			registry.addKey(index, "extra_attributes", fontRegistryEntry.extra_attributes);
			registry.addKey(index, "weight", fontRegistryEntry.weight);
			registry.addKey(index, "family_code", fontRegistryEntry.family_code);
			registry.addKey(index, "style", fontRegistryEntry.style);
			registry.addKey(index, "sub_style", fontRegistryEntry.sub_style);
			registry.addKey(index, "language_code", fontRegistryEntry.language_code);
			registry.addKey(index, "region_code", fontRegistryEntry.region_code);
			registry.addKey(index, "country_code", fontRegistryEntry.country_code);
			registry.addKey(index, "font_name", fontRegistryEntry.font_name, 0x40);
			registry.addKey(index, "file_name", fontRegistryEntry.file_name, 0x40);
			registry.addKey(index, "expire_date", fontRegistryEntry.expire_date);
			registry.addKey(index, "shadow_option", fontRegistryEntry.shadow_option);
		}

		index = registry.addDirectory(parent, "COUNT", 6, 1);
		registry.addKey(index, "boot_count", 0);
		registry.addKey(index, "game_exec_count", gameExecCount);
		registry.addKey(index, "slide_count", 0);
		registry.addKey(index, "usb_connect_count", usbConnectCount);
		registry.addKey(index, "wifi_connect_count", wifiConnectCount);
		registry.addKey(index, "psn_access_count", 0);

		index = registry.addDirectory(RegistryDirectoryHeader.NO_PARENT, "SYSPROFILE", 2, 1);
		registry.addKey(index, "sound_reduction", 0);
		registry.addKey(index, "RESOLUTION");

		parent = index;
		index = registry.addDirectory(parent, "RESOLUTION", 2, 1);
		registry.addKey(index, "horizontal", 8210);
		registry.addKey(index, "vertical", 8210);

		registry.save(iregFileName, dregFileName);
	}

	@Override
	public void start() {
		try {
			sha1 = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			log.error(e);
		}

		registryHandles = new HashMap<Integer, sceReg.RegistryHandle>();
		categoryHandles = new HashMap<Integer, sceReg.CategoryHandle>();
		keyHandles = new HashMap<Integer, sceReg.KeyHandle>();

		// TODO Read these values from the configuration file
		Settings settings = Settings.getInstance();
		authName = "";
		authKey = "";
		networkLatestId = 0;
		wifiConnectCount = 0;
		usbConnectCount = 0;
		gameExecCount = 0;
	    oskVersionId = 0x226;
	    oskDispLocale = 0x1;
	    oskWritingLocale = 0x1;
	    oskInputCharMask = 0xF;
	    oskKeytopIndex = 0x5;
	    npEnv = "np"; // Max length 8
	    adhocSsidPrefix = "PSP"; // Must be of length 3
	    musicVisualizerMode = 0;
	    musicTrackInfoMode = 1;
	    lockPassword = "0000"; // 4-digit password
	    browserHomeUri = "";
	    npAccountId = settings.readString("registry.npAccountId");
	    npLoginId = settings.readString("registry.npLoginId");
	    npPassword = settings.readString("registry.npPassword");
	    npAutoSignInEnable = settings.readInt("registry.npAutoSignInEnable");
		ownerName = sceUtility.getSystemParamNickname();

		byte[] ireg = readCompleteFile(iregFileName);
		byte[] dreg = readCompleteFile(dregFileName);
		if (ireg != null && dreg != null) {
			registry = new Registry(ireg, dreg);
			registry.dump();
		} else {
			createEmptyRegistry();
		}

		super.start();
	}

    @HLEFunction(nid = 0x92E41280, version = 150)
    @HLEFunction(nid = 0xDBA46704, version = 660)
    public int sceRegOpenRegistry(TPointer reg, int mode, @BufferInfo(usage=Usage.out) TPointer32 h) {
    	int regType = reg.getValue32(0);
    	int nameLen = reg.getValue32(260);
    	int unknown1 = reg.getValue32(264);
    	int unknown2 = reg.getValue32(268);
    	String name = Utilities.readStringNZ(reg.getAddress() + 4, nameLen);
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("RegParam: regType=%d, name='%s'(len=%d), unknown1=%d, unknown2=%d", regType, name, nameLen, unknown1, unknown2));
    	}

    	RegistryHandle registryHandle = new RegistryHandle(regType, name, unknown1, unknown2);
    	registryHandles.put(registryHandle.uid, registryHandle);

    	h.setValue(registryHandle.uid);

    	return 0;
    }

    @HLEFunction(nid = 0xFA8A5739, version = 150)
    @HLEFunction(nid = 0x49D77D65, version = 660)
    public int sceRegCloseRegistry(int h) {
    	RegistryHandle registryHandle = registryHandles.get(h);
    	if (registryHandle == null) {
    		return -1;
    	}

    	registryHandle.release();
    	registryHandles.remove(h);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xDEDA92BF, version = 150)
    public int sceRegRemoveRegistry(TPointer reg) {
    	return 0;
    }

    @HLEFunction(nid = 0x1D8A762E, version = 150)
    @HLEFunction(nid = 0x4F471457, version = 660)
    public int sceRegOpenCategory(int h, String name, int mode, @BufferInfo(usage=Usage.out) TPointer32 hd) {
    	RegistryHandle registryHandle = registryHandles.get(h);
    	if (registryHandle == null) {
    		return -1;
    	}
    	CategoryHandle categoryHandle = new CategoryHandle(registryHandle, name, mode);
    	categoryHandles.put(categoryHandle.uid, categoryHandle);
    	hd.setValue(categoryHandle.uid);

    	if (categoryHandle.getFullName().startsWith("/system/DATA/FONT/PROPERTY/INFO")) {
    		List<sceFont.FontRegistryEntry> fontRegistry = Modules.sceFontModule.getFontRegistry();
    		int index = Integer.parseInt(categoryHandle.getFullName().substring(31));
    		if (index < 0 || index >= fontRegistry.size()) {
    			if (mode != REG_MODE_READ_WRITE) {
    				return SceKernelErrors.ERROR_REGISTRY_NOT_FOUND;
    			}
    		}
    	} else if (categoryHandle.getFullName().startsWith("flash2/registry/system/CONFIG/NETWORK/INFRASTRUCTURE/")) {
    		String indexString = categoryHandle.getFullName().substring(53);
    		int sep = indexString.indexOf('/');
    		if (sep >= 0) {
    			indexString = indexString.substring(0, sep);
    		}
    		int index = Integer.parseInt(indexString);
    		// We do not return too many entries as some homebrew only support a limited number of entries.
    		if (index > sceUtility.PSP_NETPARAM_MAX_NUMBER_DUMMY_ENTRIES) {
    			return SceKernelErrors.ERROR_REGISTRY_NOT_FOUND;
    		}
    	}

    	return 0;
    }

    @HLEFunction(nid = 0x0CAE832B, version = 150)
    @HLEFunction(nid = 0xFC742751, version = 660)
    public int sceRegCloseCategory(int hd) {
    	CategoryHandle categoryHandle = categoryHandles.get(hd);
    	if (categoryHandle == null) {
    		return -1;
    	}

    	categoryHandle.release();
    	categoryHandles.remove(hd);

    	return 0;
    }

    @HLEFunction(nid = 0x39461B4D, version = 150)
    @HLEFunction(nid = 0x5FD4764A, version = 660)
    public int sceRegFlushRegistry(int h) {
    	RegistryHandle registryHandle = registryHandles.get(h);
    	if (registryHandle == null) {
    		return -1;
    	}
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0D69BF40, version = 150)
    @HLEFunction(nid = 0xD743A608, version = 660)
    public int sceRegFlushCategory(int hd) {
    	CategoryHandle categoryHandle = categoryHandles.get(hd);
    	if (categoryHandle == null) {
    		return -1;
    	}
    	return 0;
    }

    @HLEFunction(nid = 0x57641A81, version = 150)
    @HLEFunction(nid = 0x3B6CA1E6, version = 660)
    public int sceRegCreateKey(int hd, String name, int type, int size) {
    	CategoryHandle categoryHandle = categoryHandles.get(hd);
    	if (categoryHandle == null) {
    		return -1;
    	}

    	if (categoryHandle.getFullName().startsWith("/system/DATA/FONT/PROPERTY/INFO")) {
			List<sceFont.FontRegistryEntry> fontRegistry = Modules.sceFontModule.getFontRegistry();
			int index = Integer.parseInt(categoryHandle.getFullName().substring(31));
			if (index < 0 || index > fontRegistry.size()) {
				return -1;
			} else if (index == fontRegistry.size()) {
				log.info(String.format("sceRegCreateKey creating a new font entry '%s'", categoryHandle.getFullName()));
				FontRegistryEntry entry = new FontRegistryEntry();
				fontRegistry.add(entry);
	    		if ("h_size".equals(name) && size >= 4 && type == REG_TYPE_INT) {
	    			// OK
	    		} else if ("v_size".equals(name) && size >= 4 && type == REG_TYPE_INT) {
	    			// OK
	    		} else if ("h_resolution".equals(name) && size >= 4 && type == REG_TYPE_INT) {
	    			// OK
	    		} else if ("v_resolution".equals(name) && size >= 4 && type == REG_TYPE_INT) {
	    			// OK
	    		} else if ("extra_attributes".equals(name) && size >= 4 && type == REG_TYPE_INT) {
	    			// OK
	    		} else if ("weight".equals(name) && size >= 4 && type == REG_TYPE_INT) {
	    			// OK
	    		} else if ("family_code".equals(name) && size >= 4 && type == REG_TYPE_INT) {
	    			// OK
	    		} else if ("style".equals(name) && size >= 4 && type == REG_TYPE_INT) {
	    			// OK
	    		} else if ("sub_style".equals(name) && size >= 4 && type == REG_TYPE_INT) {
	    			// OK
	    		} else if ("language_code".equals(name) && size >= 4 && type == REG_TYPE_INT) {
	    			// OK
	    		} else if ("region_code".equals(name) && size >= 4 && type == REG_TYPE_INT) {
	    			// OK
	    		} else if ("country_code".equals(name) && size >= 4 && type == REG_TYPE_INT) {
	    			// OK
	    		} else if ("file_name".equals(name) && size >= 0 && type == REG_TYPE_STR) {
	    			// OK
	    		} else if ("font_name".equals(name) && size >= 0 && type == REG_TYPE_STR) {
	    			// OK
	    		} else if ("expire_date".equals(name) && size >= 4 && type == REG_TYPE_INT) {
	    			// OK
	    		} else if ("shadow_option".equals(name) && size >= 4 && type == REG_TYPE_INT) {
	    			// OK
	    		} else {
	    			log.warn(String.format("Unknown font registry entry '%s' size=0x%X, type=%d", name, size, type));
	    		}
			}
    	} else {
			log.warn(String.format("Unknown registry entry '%s/%s'", categoryHandle.getFullName(), name));
    	}

    	return 0;
    }

    @HLEFunction(nid = 0x17768E14, version = 150)
    @HLEFunction(nid = 0x49C70163, version = 660)
    public int sceRegSetKeyValue(int hd, String name, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer buf, int size) {
    	CategoryHandle categoryHandle = categoryHandles.get(hd);
    	if (categoryHandle == null) {
    		return -1;
    	}
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("buf: %s", Utilities.getMemoryDump(buf.getAddress(), size)));
    	}

    	String fullName = categoryHandle.getFullName();
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceRegSetKeyValue fullName='%s/%s'", fullName, name));
    	}
    	fullName = fullName.replace("flash1:/registry/system", "");
    	fullName = fullName.replace("flash1/registry/system", "");
    	fullName = fullName.replace("flash2/registry/system", "");

    	Settings settings = Settings.getInstance();
    	if ("/system/DATA/FONT".equals(fullName)) {
    		if ("path_name".equals(name)) {
    			String fontDirPath = buf.getStringNZ(size);
    			if (log.isInfoEnabled()) {
    				log.info(String.format("Setting font dir path to '%s'", fontDirPath));
    			}
    			Modules.sceFontModule.setFontDirPath(fontDirPath);
    		} else if ("num_fonts".equals(name) && size >= 4) {
        		List<sceFont.FontRegistryEntry> fontRegistry = Modules.sceFontModule.getFontRegistry();
    			int numFonts = buf.getValue32();
    			if (numFonts != fontRegistry.size()) {
	    			if (log.isInfoEnabled()) {
	    				log.info(String.format("Changing the number of fonts from %d to %d", fontRegistry.size(), numFonts));
	    			}
    			}
    		} else {
    			log.warn(String.format("Unknown font registry entry '%s'", name));
    		}
    	} else if (fullName.startsWith("/system/DATA/FONT/PROPERTY/INFO")) {
    		List<sceFont.FontRegistryEntry> fontRegistry = Modules.sceFontModule.getFontRegistry();
    		int index = Integer.parseInt(fullName.substring(31));
    		if (index < 0 || index >= fontRegistry.size()) {
    			return -1;
    		}
    		FontRegistryEntry entry = fontRegistry.get(index);
    		if ("h_size".equals(name) && size >= 4) {
    			entry.h_size = buf.getValue32();
    		} else if ("v_size".equals(name) && size >= 4) {
    			entry.h_size = buf.getValue32();
    		} else if ("h_resolution".equals(name) && size >= 4) {
    			entry.h_size = buf.getValue32();
    		} else if ("v_resolution".equals(name) && size >= 4) {
    			entry.h_size = buf.getValue32();
    		} else if ("extra_attributes".equals(name) && size >= 4) {
    			entry.h_size = buf.getValue32();
    		} else if ("weight".equals(name) && size >= 4) {
    			entry.h_size = buf.getValue32();
    		} else if ("family_code".equals(name) && size >= 4) {
    			entry.h_size = buf.getValue32();
    		} else if ("style".equals(name) && size >= 4) {
    			entry.h_size = buf.getValue32();
    		} else if ("sub_style".equals(name) && size >= 4) {
    			entry.h_size = buf.getValue32();
    		} else if ("language_code".equals(name) && size >= 4) {
    			entry.h_size = buf.getValue32();
    		} else if ("region_code".equals(name) && size >= 4) {
    			entry.h_size = buf.getValue32();
    		} else if ("country_code".equals(name) && size >= 4) {
    			entry.h_size = buf.getValue32();
    		} else if ("file_name".equals(name)) {
    			entry.file_name = buf.getStringNZ(size);
    		} else if ("font_name".equals(name)) {
    			entry.font_name = buf.getStringNZ(size);
    		} else if ("expire_date".equals(name) && size >= 4) {
    			entry.h_size = buf.getValue32();
    		} else if ("shadow_option".equals(name) && size >= 4) {
    			entry.h_size = buf.getValue32();
    		} else {
    			log.warn(String.format("Unknown font registry entry '%s'", name));
    		}
    	} else if ("/DATA/COUNT".equals(fullName)) {
    		if ("wifi_connect_count".equals(name) && size >= 4) {
    			wifiConnectCount = buf.getValue32();
    		} else if ("usb_connect_count".equals(name) && size >= 4) {
    			usbConnectCount = buf.getValue32();
    		} else if ("psn_access_count".equals(name) && size >= 4) {
    			psnAccountCount = buf.getValue32();
    		} else if ("slide_count".equals(name) && size >= 4) {
    			slideCount = buf.getValue32();
    		} else if ("boot_count".equals(name) && size >= 4) {
    			bootCount = buf.getValue32();
    		} else if ("game_exec_count".equals(name) && size >= 4) {
    			gameExecCount = buf.getValue32();
    		} else {
    			log.warn(String.format("Unknown registry entry '%s'", name));
    		}
    	} else if ("/CONFIG/OSK".equals(fullName)) {
    		if ("version_id".equals(name) && size >= 4) {
    			oskVersionId = buf.getValue32();
    		} else if (name.equals("disp_locale") && size >= 4) {
    			oskDispLocale = buf.getValue32();
    		} else if (name.equals("writing_locale") && size >= 4) {
    			oskWritingLocale = buf.getValue32();
    		} else if (name.equals("input_char_mask") && size >= 4) {
    			oskInputCharMask = buf.getValue32();
    		} else if (name.equals("keytop_index") && size >= 4) {
    			oskKeytopIndex = buf.getValue32();
    		} else {
    			log.warn(String.format("Unknown registry entry '%s'", name));
    		}
    	} else if ("/CONFIG/NP".equals(fullName)) {
    		if ("env".equals(name)) {
    			npEnv = buf.getStringNZ(size);
    		} else if ("account_id".equals(name)) {
    			npAccountId = buf.getStringNZ(size);
    			settings.writeString("registry.npAccountId", npAccountId);
    		} else if ("login_id".equals(name)) {
    			npLoginId = buf.getStringNZ(size);
    			settings.writeString("registry.npLoginId", npLoginId);
    		} else if ("password".equals(name)) {
    			npPassword = buf.getStringNZ(size);
    			settings.writeString("registry.npPassword", npPassword);
    		} else if ("auto_sign_in_enable".equals(name) && size >= 4) {
    			npAutoSignInEnable = buf.getValue32();
    			settings.writeInt("registry.npAutoSignInEnable", npAutoSignInEnable);
    		} else {
    			log.warn(String.format("Unknown registry entry '%s'", name));
    		}
    	} else if ("/CONFIG/NETWORK/INFRASTRUCTURE".equals(fullName)) {
    		if ("latest_id".equals(name) && size >= 4) {
    			networkLatestId = buf.getValue32();
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if (fullName.matches("/CONFIG/NETWORK/INFRASTRUCTURE/\\d+")) {
    		String indexName = fullName.replace("/CONFIG/NETWORK/INFRASTRUCTURE/", "");
    		int index = Integer.parseInt(indexName);
            if ("cnf_name".equals(name)) {
            	String cnfName = buf.getStringNZ(size);
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set cnf_name#%d='%s'", index, cnfName));
            	}
    		} else if ("ssid".equals(name)) {
            	String ssid = buf.getStringNZ(size);
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set ssid#%d='%s'", index, ssid));
            	}
    		} else if ("auth_proto".equals(name) && size >= 4) {
            	int authProto = buf.getValue32();
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set auth_proto#%d='%s'", index, authProto));
            	}
    		} else if ("wep_key".equals(name)) {
            	String wepKey = buf.getStringNZ(size);
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set wep_key#%d='%s'", index, wepKey));
            	}
    		} else if ("how_to_set_ip".equals(name) && size >= 4) {
            	int howToSetIp = buf.getValue32();
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set how_to_set_ip#%d=%d", index, howToSetIp));
            	}
    		} else if ("ip_address".equals(name)) {
            	String ipAddress = buf.getStringNZ(size);
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set ip_address#%d='%s'", index, ipAddress));
            	}
    		} else if ("netmask".equals(name)) {
            	String netmask = buf.getStringNZ(size);
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set netmask#%d='%s'", index, netmask));
            	}
    		} else if ("default_route".equals(name)) {
            	String defaultRoute = buf.getStringNZ(size);
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set default_route#%d='%s'", index, defaultRoute));
            	}
    		} else if ("dns_flag".equals(name) && size >= 4) {
            	int dnsFlag = buf.getValue32();
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set dns_flag#%d=%d", index, dnsFlag));
            	}
    		} else if ("primary_dns".equals(name)) {
            	String primaryDns = buf.getStringNZ(size);
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set primary_dns#%d='%s'", index, primaryDns));
            	}
    		} else if ("secondary_dns".equals(name)) {
            	String secondaryDns = buf.getStringNZ(size);
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set secondary_dns#%d='%s'", index, secondaryDns));
            	}
    		} else if ("auth_name".equals(name)) {
            	String authName = buf.getStringNZ(size);
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set auth_name#%d='%s'", index, authName));
            	}
    		} else if ("auth_key".equals(name)) {
            	String authKey = buf.getStringNZ(size);
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set auth_key#%d='%s'", index, authKey));
            	}
    		} else if ("http_proxy_flag".equals(name) && size >= 4) {
            	int httpProxyFlag = buf.getValue32();
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set http_proxy_flag#%d=%d", index, httpProxyFlag));
            	}
    		} else if ("http_proxy_server".equals(name)) {
            	String httpProxyServer = buf.getStringNZ(size);
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set http_proxy_server#%d='%s'", index, httpProxyServer));
            	}
    		} else if ("http_proxy_port".equals(name) && size >= 4) {
            	int httpProxyPort = buf.getValue32();
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set http_proxy_port#%d=%d", index, httpProxyPort));
            	}
    		} else if ("version".equals(name) && size >= 4) {
            	int version = buf.getValue32();
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set version#%d=%d", index, version));
            	}
    		} else if ("device".equals(name) && size >= 4) {
            	int device = buf.getValue32();
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set device#%d=%d", index, device));
            	}
    		} else if ("auth_8021x_type".equals(name) && size >= 4) {
            	int authType = buf.getValue32();
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set auth_8021x_type#%d=%d", index, authType));
            	}
    		} else if ("auth_8021x_auth_name".equals(name)) {
            	String authName = buf.getStringNZ(size);
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set auth_8021x_auth_name#%d='%s'", index, authName));
            	}
    		} else if ("auth_8021x_auth_key".equals(name)) {
            	String authKey = buf.getStringNZ(size);
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set auth_8021x_auth_key#%d='%s'", index, authKey));
            	}
    		} else if ("wpa_key_type".equals(name) && size >= 4) {
            	int wpaKeyType = buf.getValue32();
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set wpa_key_type#%d=%d", index, wpaKeyType));
            	}
    		} else if ("wpa_key".equals(name)) {
            	String wpaKey = buf.getStringNZ(size);
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set wpa_key#%d='%s'", index, wpaKey));
            	}
    		} else if ("browser_flag".equals(name) && size >= 4) {
            	int browserFlag = buf.getValue32();
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set browser_flag#%d=%d", index, browserFlag));
            	}
    		} else if ("wifisvc_config".equals(name) && size >= 4) {
            	int wifisvcConfig = buf.getValue32();
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set wifisvc_config#%d=%d", index, wifisvcConfig));
            	}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if (fullName.matches("/CONFIG/NETWORK/INFRASTRUCTURE/\\d+/SUB1")) {
    		String indexName = fullName.replace("/CONFIG/NETWORK/INFRASTRUCTURE/", "");
    		int index = Integer.parseInt(indexName.substring(0, indexName.indexOf("/")));
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("/CONFIG/NETWORK/INFRASTRUCTURE, index=%d, SUB1", index));
    		}
    		if ("wifisvc_auth_name".equals(name)) {
            	String authName = buf.getStringNZ(size);
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set wifisvc_auth_name#%d='%s'", index, authName));
            	}
    		} else if ("wifisvc_auth_key".equals(name)) {
            	String authKey = buf.getStringNZ(size);
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set wifisvc_auth_key#%d='%s'", index, authKey));
            	}
    		} else if ("wifisvc_option".equals(name)) {
            	int wifisvcOption = buf.getValue32();
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set wifisvc_option#%d=%d", index, wifisvcOption));
            	}
    		} else if ("last_leased_dhcp_addr".equals(name)) {
            	String lastLeasedDhcpAddr = buf.getStringNZ(size);
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set last_leased_dhcp_addr#%d='%s'", index, lastLeasedDhcpAddr));
            	}
    		} else if ("bt_id".equals(name)) {
            	int btId = buf.getValue32();
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set bt_id#%d=%d", index, btId));
            	}
    		} else if ("at_command".equals(name)) {
            	String atCommand = buf.getStringNZ(size);
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set at_command#%d='%s'", index, atCommand));
            	}
    		} else if ("phone_number".equals(name)) {
            	String phoneNumber = buf.getStringNZ(size);
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set phone_number#%d='%s'", index, phoneNumber));
            	}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/NETWORK/ADHOC".equals(fullName)) {
    		if ("ssid_prefix".equals(name)) {
    			adhocSsidPrefix = buf.getStringNZ(size);
    		} else {
    			log.warn(String.format("Unknown registry entry '%s'", name));
    		}
    	} else if ("/CONFIG/SYSTEM".equals(fullName)) {
    		if ("owner_name".equals(name)) {
    			ownerName = buf.getStringNZ(size);
    		}
    	} else if ("/CONFIG/SYSTEM/XMB/THEME".equals(fullName)) {
    		if ("custom_theme_mode".equals(name) && size >= 4) {
    			settings.writeInt("registry.theme.custom_theme_mode", buf.getValue32());
    		} else if ("color_mode".equals(name) && size >= 4) {
    			settings.writeInt("registry.theme.color_mode", buf.getValue32());
    		} else if ("wallpaper_mode".equals(name) && size >= 4) {
    			settings.writeInt("registry.theme.wallpaper_mode", buf.getValue32());
    		} else if ("system_color".equals(name) && size >= 4) {
    			settings.writeInt("registry.theme.system_color", buf.getValue32());
    		} else {
    			log.warn(String.format("Unknown registry entry '%s'", name));
    		}
    	} else if ("/CONFIG/MUSIC".equals(fullName)) {
    		if ("visualizer_mode".equals(name) && size >= 4) {
    			musicVisualizerMode = buf.getValue32();
    		} else if (name.equals("track_info_mode") && size >= 4) {
    			musicTrackInfoMode = buf.getValue32();
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/CAMERA".equals(fullName)) {
    		if ("msid".equals(name) && size >= 0) {
    			String msid = buf.getStringNZ(16);
    			if (log.isDebugEnabled()) {
    				log.debug(String.format("sceRegSetKeyValue msid='%s'", msid));
    			}
    		} else if (name.equals("file_folder") && size >= 4) {
    			int fileFolder = buf.getValue32();
    			if (log.isDebugEnabled()) {
    				log.debug(String.format("sceRegSetKeyValue fileFolder=0x%X", fileFolder));
    			}
    		} else if (name.equals("file_number") && size >= 4) {
    			int fileNumber = buf.getValue32();
    			if (log.isDebugEnabled()) {
    				log.debug(String.format("sceRegSetKeyValue fileNumber=0x%X", fileNumber));
    			}
    		} else if (name.equals("movie_quality") && size >= 4) {
    			int movieQuality = buf.getValue32();
    			if (log.isDebugEnabled()) {
    				log.debug(String.format("sceRegSetKeyValue movieQuality=0x%X", movieQuality));
    			}
    		} else if (name.equals("movie_size") && size >= 4) {
    			int movieSize = buf.getValue32();
    			if (log.isDebugEnabled()) {
    				log.debug(String.format("sceRegSetKeyValue movieSize=0x%X", movieSize));
    			}
    		} else if (name.equals("movie_fps") && size >= 4) {
    			int movieFps = buf.getValue32();
    			if (log.isDebugEnabled()) {
    				log.debug(String.format("sceRegSetKeyValue movieFps=0x%X", movieFps));
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/DATE".equals(fullName)) {
    		if ("date_format".equals(name)) {
    			settings.writeInt("registry.date_format", buf.getValue32());
    		} else if ("time_format".equals(name)) {
    			settings.writeInt("registry.time_format", buf.getValue32());
    		} else if ("time_zone_offset".equals(name)) {
    			settings.writeInt("registry.time_zone_offset", buf.getValue32());
    		} else if ("time_zone_area".equals(name)) {
    			settings.writeString("registry.time_zone_area", buf.getStringZ());
    		} else if ("summer_time".equals(name)) {
    			settings.writeInt("registry.summer_time", buf.getValue32());
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/SYSTEM/XMB".equals(fullName)) {
    		if ("language".equals(name)) {
    			settings.writeInt(sceUtility.SYSTEMPARAM_SETTINGS_OPTION_LANGUAGE, buf.getValue32());
    		} else if ("button_assign".equals(name)) {
    			settings.writeInt(sceUtility.SYSTEMPARAM_SETTINGS_OPTION_BUTTON_PREFERENCE, buf.getValue32());
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else {
			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    	}

    	return 0;
    }

    @HLEFunction(nid = 0xD4475AA8, version = 150)
    @HLEFunction(nid = 0x9980519F, version = 150)
    public int sceRegGetKeyInfo(int hd, String name, TPointer32 hk, @BufferInfo(usage=Usage.out) TPointer32 ptype, @BufferInfo(usage=Usage.out) TPointer32 psize) {
    	CategoryHandle categoryHandle = categoryHandles.get(hd);
    	if (categoryHandle == null) {
    		return -1;
    	}

    	KeyHandle keyHandle = new KeyHandle(name);
    	keyHandles.put(keyHandle.uid, keyHandle);

    	hk.setValue(keyHandle.uid);

    	return getKey(categoryHandle, name, ptype, psize, TPointer.NULL, 0);
    }

    @HLEFunction(nid = 0x28A8E98A, version = 150)
    @HLEFunction(nid = 0xF4A3E396, version = 660)
    public int sceRegGetKeyValue(int hd, int hk, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.out) TPointer buf, int size) {
    	CategoryHandle categoryHandle = categoryHandles.get(hd);
    	if (categoryHandle == null) {
    		return -1;
    	}

    	KeyHandle keyHandle = keyHandles.get(hk);
    	if (keyHandle == null) {
    		return -1;
    	}

    	return getKey(categoryHandle, keyHandle.name, TPointer32.NULL, TPointer32.NULL, buf, size);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2C0DB9DD, version = 150)
    public int sceRegGetKeysNum(int hd, int num) {
    	CategoryHandle categoryHandle = categoryHandles.get(hd);
    	if (categoryHandle == null) {
    		return -1;
    	}
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2D211135, version = 150)
    public int sceRegGetKeys(int hd, TPointer buf, int num) {
    	CategoryHandle categoryHandle = categoryHandles.get(hd);
    	if (categoryHandle == null) {
    		return -1;
    	}
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4CA16893, version = 150)
    @HLEFunction(nid = 0x61DB9D06, version = 660)
    public int sceRegRemoveCategory(int h, String name) {
    	return 0;
    }

    @HLEFunction(nid = 0xC5768D02, version = 150)
    @HLEFunction(nid = 0xF2619407, version = 660)
    public int sceRegGetKeyInfoByName(int hd, String name, @BufferInfo(usage=Usage.out) TPointer32 ptype, @BufferInfo(usage=Usage.out) TPointer32 psize) {
    	CategoryHandle categoryHandle = categoryHandles.get(hd);
    	if (categoryHandle == null) {
    		return -1;
    	}

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceRegGetKeyInfoByName fullName='%s/%s'", categoryHandle.getFullName(), name));
    	}

    	return getKey(categoryHandle, name, ptype, psize, TPointer.NULL, 0);
    }

    @HLEFunction(nid = 0x30BE0259, version = 150)
    @HLEFunction(nid = 0x38415B9F, version = 660)
    public int sceRegGetKeyValueByName(int hd, String name, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.out) TPointer buf, int size) {
    	CategoryHandle categoryHandle = categoryHandles.get(hd);
    	if (categoryHandle == null) {
    		return -1;
    	}

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceRegGetKeyValueByName fullName='%s/%s'", categoryHandle.getFullName(), name));
    	}

    	return getKey(categoryHandle, name, TPointer32.NULL, TPointer32.NULL, buf, size);
    }
}
