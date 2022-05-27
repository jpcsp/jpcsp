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

import static java.lang.Integer.rotateRight;
import static jpcsp.HLE.HLEModuleManager.InternalSyscallNid;
import static jpcsp.HLE.modules.sceIdStorage.idStorageKeys;
import static jpcsp.hardware.Nand.getTotalPages;
import static jpcsp.hardware.Nand.pageSize;
import static jpcsp.hardware.Nand.pagesPerBlock;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.apache.log4j.Logger;

import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer8;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.VFS.IVirtualFileSystem;
import jpcsp.HLE.VFS.compress.CompressPrxVirtualFileSystem;
import jpcsp.HLE.VFS.fat.Fat12VirtualFile;
import jpcsp.HLE.VFS.fat.FatVirtualFile;
import jpcsp.HLE.VFS.fat.FatVirtualFileSystem;
import jpcsp.HLE.VFS.local.LocalVirtualFile;
import jpcsp.HLE.VFS.local.LocalVirtualFileSystem;
import jpcsp.HLE.VFS.nand.NandVirtualFile;
import jpcsp.HLE.VFS.patch.PatchFileVirtualFileSystem;
import jpcsp.HLE.VFS.synchronize.ISynchronize;
import jpcsp.HLE.VFS.synchronize.SynchronizeMemoryToVirtualFile;
import jpcsp.HLE.VFS.synchronize.SynchronizeVirtualFileSystems;
import jpcsp.HLE.kernel.types.SceNandSpare;
import jpcsp.filesystems.SeekableRandomFile;
import jpcsp.hardware.Nand;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.IntArrayMemory;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.mmio.MMIOHandlerNand;
import jpcsp.settings.Settings;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;
import jpcsp.util.Utilities;

public class sceNand extends HLEModule {
    public static Logger log = Modules.getLogger("sceNand");
	private static final int STATE_VERSION = 0;
    private static final boolean emulateNand = true;
    private static final boolean storeNandInMemory = true;
    public static final int iplTablePpnStart = 0x80;
    public static final int iplTablePpnEnd = 0x17F;
    public static final int iplPpnStart = 0x200;
    public static final int iplPpnEnd = 0x3DF;
    public static final int idStoragePpnStart = 0x600;
    public static final int idStoragePpnEnd = 0x7FF;
    public static final int iplId = 0x6DC64A38;
    private static final int idStorageId = 0xFFFF0101;
    private byte[] dumpBlocks;
    private byte[] dumpSpares;
    private int[] dumpResults;
    private int[] ppnToLbn;
    private boolean writeProtected;
    private int scramble;
    private FatVirtualFile vFileFlash0;
    private FatVirtualFile vFileFlash1;
    private FatVirtualFile vFileFlash2;
    private FatVirtualFile vFileFlash3;
    public static final int flash0LbnStart = 0x2;
    public static int flash1LbnStart;
    public static int flash2LbnStart;
    public static int flash3LbnStart;
    public static int flash4LbnStart;
    public static int flash5LbnStart;
    private IVirtualFile vFileIpl;
    private IntArrayMemory nandMemory;
    private TPointer nandMemoryPointer;
    private IntArrayMemory nandSpareMemory;
    private TPointer nandSpareMemoryPointer;
    private boolean initNandInProgress;
    private final Object writeLock = new Object();
    private ISynchronize syncFlash0;
    private ISynchronize syncFlash1;
    private ISynchronize syncFlash2;
    private ISynchronize syncFlash3;
    private ISynchronize syncIpl;

    @Override
	public void start() {
    	reset();

		if (log.isDebugEnabled()) {
			for (int ppn = 0; ppn < ppnToLbn.length; ppn++) {
				if (ppnToLbn[ppn] == 0xFFFF) {
					int startFreePpn = ppn;
					int endFreePpn = ppn;
					for (; ppn < ppnToLbn.length; ppn++) {
						if (ppnToLbn[ppn] != 0xFFFF) {
							ppn--;
							endFreePpn = ppn;
							break;
						}
					}

					log.debug(String.format("Free blocks ppn=0x%X-0x%X", startFreePpn, endFreePpn));
				}
			}
		}

		if (log.isTraceEnabled()) {
			for (int ppn = 0; ppn < ppnToLbn.length; ppn++) {
				log.trace(String.format("ppn=0x%04X -> lbn=0x%04X", ppn, ppnToLbn[ppn]));
			}
		}

		if (!emulateNand && log.isTraceEnabled()) {
			//
			// Brute force search for boot sectors
			//
			long fuseId = Modules.sceSysregModule.sceSysregGetFuseId();
			// All possible scramble values
			final int scrambles[] = new int[7];
			int n = 0;
			scrambles[n++] = MMIOHandlerNand.getScrambleBootSector(fuseId, 0);
			scrambles[n++] = MMIOHandlerNand.getScrambleDataSector(fuseId, 0);
			scrambles[n++] = 0;
			scrambles[n++] = MMIOHandlerNand.getScrambleBootSector(fuseId, 2);
			scrambles[n++] = MMIOHandlerNand.getScrambleDataSector(fuseId, 2);
			scrambles[n++] = MMIOHandlerNand.getScrambleBootSector(fuseId, 3);
			scrambles[n++] = MMIOHandlerNand.getScrambleDataSector(fuseId, 3);

			TPointer user = Utilities.allocatePointer(pageSize);
			int maxPpn = dumpBlocks.length / pageSize;
			for (int ppn = 0; ppn < maxPpn; ppn++) {
				for (int i = 0; i < scrambles.length; i++) {
					int scramble = scrambles[i];
					if (scramble != 0) {
						sceNandSetScramble(scramble);
						descramblePage(ppn, user, dumpBlocks, ppn * pageSize);
					} else {
						Utilities.writeBytes(user, pageSize, dumpBlocks, ppn * pageSize);
					}

					// Boot sector found?
					if (user.getUnsignedValue16(510) == 0xAA55) {
						log.trace(String.format("Boot sector ppn=0x%X: %s", ppn, Utilities.getMemoryDump(user, pageSize)));
					}
				}
			}
		}

		if (!emulateNand && log.isTraceEnabled()) {
			//
			// Dump all ID storage pages
			//
			TPointer user = Utilities.allocatePointer(pageSize);
			int idStorageKeys[] = new int[0x10000];
			Arrays.fill(idStorageKeys, -1);
			int idStoragePage = 0;
			for (int ppn = idStoragePpnStart; ppn <= idStoragePpnEnd; ppn++) {
				int id = Utilities.readUnaligned32(dumpSpares, (ppn << 4) + 8);
				if (id == idStorageId) {
					int scramble = MMIOHandlerNand.getInstance().getScramble(ppn);

					if (scramble != 0) {
						sceNandSetScramble(scramble);
						descramblePage(ppn, user, dumpBlocks, ppn * pageSize);
					} else {
						Utilities.writeBytes(user, pageSize, dumpBlocks, ppn * pageSize);
					}

					for (int i = 0; i < pageSize; i += 2) {
						int n = user.getUnsignedValue16(i);
						if (n != 0xFFFF) {
							idStorageKeys[n] = (idStoragePage * pageSize + i) >> 1;
						}
					}
					idStoragePage++;
				}
			}

			for (int key = 0; key < idStorageKeys.length; key++) {
				if (idStorageKeys[key] >= 0) {
					int ppn = idStoragePpnStart + idStorageKeys[key];

					int scramble = MMIOHandlerNand.getInstance().getScramble(ppn);

					if (scramble != 0) {
						sceNandSetScramble(scramble);
						descramblePage(ppn, user, dumpBlocks, ppn * pageSize);
					} else {
						Utilities.writeBytes(user, pageSize, dumpBlocks, ppn * pageSize);
					}

					log.trace(String.format("ID Storage key=0x%X, ppn=0x%X: %s", key, ppn, isEmptyPage(user, 0) ? "all 0's" : Utilities.getMemoryDump(user, pageSize)));
				}
			}
		}

		super.start();
	}

    private void initNandInMemory() {
    	if (!storeNandInMemory || nandMemory != null || initNandInProgress) {
    		return;
    	}

    	initNandInProgress = true;

    	IntArrayMemory nandMemory = new IntArrayMemory(new int[Nand.getTotalSize() >> 2]);
		IntArrayMemory nandSpareMemory = new IntArrayMemory(new int[Nand.getTotalPages() << 2]);

		for (int ppn = 0; ppn < ppnToLbn.length; ppn += pagesPerBlock) {
			hleNandReadPages(ppn, nandMemory.getPointer(ppn * pageSize), nandSpareMemory.getPointer(ppn * 16), pagesPerBlock, true, true, true);
		}

		if (vFileFlash0 != null) {
			vFileFlash0.ioClose();
			vFileFlash0 = null;
		}
		if (vFileFlash1 != null) {
			vFileFlash1.ioClose();
			vFileFlash1 = null;
		}
		if (vFileFlash2 != null) {
			vFileFlash2.ioClose();
			vFileFlash2 = null;
		}
		if (vFileFlash3 != null) {
			vFileFlash3.ioClose();
			vFileFlash3 = null;
		}

		this.nandMemory = nandMemory;
		nandMemoryPointer = nandMemory.getPointer();
		this.nandSpareMemory = nandSpareMemory;
		nandSpareMemoryPointer = nandSpareMemory.getPointer();

		IVirtualFileSystem inputFlash0 = new FatVirtualFileSystem("flash0", new NandVirtualFile(flash0LbnStart + 1, flash1LbnStart));
		IVirtualFileSystem outputFlash0 = new LocalVirtualFileSystem(Settings.getInstance().getDirectoryMapping("flash0"), false);
		syncFlash0 = new SynchronizeVirtualFileSystems("flash0", inputFlash0, outputFlash0, writeLock);

		IVirtualFileSystem inputFlash1 = new FatVirtualFileSystem("flash1", new NandVirtualFile(flash1LbnStart + 1, flash2LbnStart));
		IVirtualFileSystem outputFlash1 = new LocalVirtualFileSystem(Settings.getInstance().getDirectoryMapping("flash1"), false);
		syncFlash1 = new SynchronizeVirtualFileSystems("flash1", inputFlash1, outputFlash1, writeLock);

		IVirtualFileSystem inputFlash2 = new FatVirtualFileSystem("flash2", new NandVirtualFile(flash2LbnStart + 1, flash3LbnStart));
		IVirtualFileSystem outputFlash2 = new LocalVirtualFileSystem(Settings.getInstance().getDirectoryMapping("flash2"), false);
		syncFlash2 = new SynchronizeVirtualFileSystems("flash2", inputFlash2, outputFlash2, writeLock);

		if (!isSmallNand()) {
			IVirtualFileSystem inputFlash3 = new FatVirtualFileSystem("flash3", new NandVirtualFile(flash3LbnStart + 1, flash4LbnStart));
			IVirtualFileSystem outputFlash3 = new LocalVirtualFileSystem(Settings.getInstance().getDirectoryMapping("flash3"), false);
			syncFlash3 = new SynchronizeVirtualFileSystems("flash3", inputFlash3, outputFlash3, writeLock);
		}

    	try {
    		TPointer inputIpl = nandMemory.getPointer(iplTablePpnStart * pageSize);
    		int inputIplSize = (iplPpnEnd - iplTablePpnStart + 1) * pageSize;
    		IVirtualFile outputIpl = new LocalVirtualFile(new SeekableRandomFile("nand.ipl.bin", "rw"));
    		syncIpl = new SynchronizeMemoryToVirtualFile("ipl", inputIpl, inputIplSize, outputIpl, writeLock);
		} catch (FileNotFoundException e) {
			log.error("initNandInMemory", e);
		}

    	initNandInProgress = false;
    }

    @Override
    public void read(StateInputStream stream) throws IOException {
    	stream.readVersion(STATE_VERSION);
    	writeProtected = stream.readBoolean();
    	scramble = stream.readInt();

    	int stateNumberPages = stream.readInt();
    	int numberPages = Math.min(stateNumberPages, ppnToLbn.length);
    	for (int i = 0; i < numberPages; i++) {
    		ppnToLbn[i] = stream.readUnsignedShort();
    	}
    	if (stateNumberPages > numberPages) {
    		stream.skipBytes(2 * (stateNumberPages - numberPages));
    	} else {
			for (int i = numberPages; i < ppnToLbn.length; i++) {
				ppnToLbn[i] = 0;
			}
    	}

    	boolean vFileFlash0Present = stream.readBoolean();
    	if (vFileFlash0Present) {
	    	openFileFlash0();
	    	vFileFlash0.read(stream);
    	}

    	boolean vFileFlash1Present = stream.readBoolean();
    	if (vFileFlash1Present) {
    		openFileFlash1();
    		vFileFlash1.read(stream);
    	}

    	boolean vFileFlash2Present = stream.readBoolean();
    	if (vFileFlash2Present) {
    		openFileFlash2();
    		vFileFlash2.read(stream);
    	}

    	boolean vFileFlash3Present = stream.readBoolean();
    	if (vFileFlash3Present) {
    		openFileFlash3();
    		vFileFlash3.read(stream);
    	}

    	boolean nandMemoryPresent = stream.readBoolean();
    	if (nandMemoryPresent) {
    		if (nandMemory == null) {
    			nandMemory = new IntArrayMemory(new int[Nand.getTotalSize() >> 2]);
    			nandMemoryPointer = nandMemory.getPointer();
    		}
    		nandMemory.read(stream);

    		if (nandSpareMemory == null) {
    			nandSpareMemory = new IntArrayMemory(new int[Nand.getTotalPages() << 2]);
    			nandSpareMemoryPointer = nandSpareMemory.getPointer();
    		}
    		nandSpareMemory.read(stream);

    		if (syncFlash0 == null) {
    			IVirtualFileSystem inputFlash0 = new FatVirtualFileSystem("flash0", new NandVirtualFile(flash0LbnStart + 1, flash1LbnStart));
    			IVirtualFileSystem outputFlash0 = new LocalVirtualFileSystem(Settings.getInstance().getDirectoryMapping("flash0"), false);
	    		syncFlash0 = new SynchronizeVirtualFileSystems("flash0", inputFlash0, outputFlash0, writeLock);
    		}
    		syncFlash0.read(stream);

    		if (syncFlash1 == null) {
    			IVirtualFileSystem inputFlash1 = new FatVirtualFileSystem("flash1", new NandVirtualFile(flash1LbnStart + 1, flash2LbnStart));
    			IVirtualFileSystem outputFlash1 = new LocalVirtualFileSystem(Settings.getInstance().getDirectoryMapping("flash1"), false);
	    		syncFlash1 = new SynchronizeVirtualFileSystems("flash1", inputFlash1, outputFlash1, writeLock);
    		}
    		syncFlash1.read(stream);

    		if (syncFlash2 == null) {
    			IVirtualFileSystem inputFlash2 = new FatVirtualFileSystem("flash2", new NandVirtualFile(flash2LbnStart + 1, flash3LbnStart));
    			IVirtualFileSystem outputFlash2 = new LocalVirtualFileSystem(Settings.getInstance().getDirectoryMapping("flash2"), false);
	    		syncFlash2 = new SynchronizeVirtualFileSystems("flash2", inputFlash2, outputFlash2, writeLock);
    		}
    		syncFlash2.read(stream);

    		boolean flash3Present = stream.readBoolean();
    		if (flash3Present) {
        		if (syncFlash3 == null) {
        			IVirtualFileSystem inputFlash3 = new FatVirtualFileSystem("flash3", new NandVirtualFile(flash3LbnStart + 1, flash4LbnStart));
        			IVirtualFileSystem outputFlash3 = new LocalVirtualFileSystem(Settings.getInstance().getDirectoryMapping("flash3"), false);
    	    		syncFlash3 = new SynchronizeVirtualFileSystems("flash3", inputFlash3, outputFlash3, writeLock);
        		}
	    		syncFlash3.read(stream);
    		}

    		if (syncIpl == null) {
    	    	try {
    	    		TPointer inputIpl = nandMemory.getPointer(iplTablePpnStart * pageSize);
    	    		int inputIplSize = (iplPpnEnd - iplTablePpnStart + 1) * pageSize;
    	    		IVirtualFile outputIpl = new LocalVirtualFile(new SeekableRandomFile("nand.ipl.bin", "rw"));
    	    		syncIpl = new SynchronizeMemoryToVirtualFile("ipl", inputIpl, inputIplSize, outputIpl, writeLock);
    			} catch (FileNotFoundException e) {
    				log.error("initNandInMemory", e);
    			}
    		}
    		syncIpl.read(stream);
    	}

    	super.read(stream);
    }

    @Override
    public void write(StateOutputStream stream) throws IOException {
    	stream.writeVersion(STATE_VERSION);
    	stream.writeBoolean(writeProtected);
    	stream.writeInt(scramble);

    	stream.writeInt(ppnToLbn.length);
    	for (int i = 0; i < ppnToLbn.length; i++) {
    		stream.writeShort(ppnToLbn[i]);
    	}

    	if (vFileFlash0 != null) {
    		stream.writeBoolean(true);
    		vFileFlash0.write(stream);
    	} else {
    		stream.writeBoolean(false);
    	}

    	if (vFileFlash1 != null) {
    		stream.writeBoolean(true);
    		vFileFlash1.write(stream);
    	} else {
    		stream.writeBoolean(false);
    	}

    	if (vFileFlash2 != null) {
    		stream.writeBoolean(true);
        	vFileFlash2.write(stream);
    	} else {
    		stream.writeBoolean(false);
    	}

    	if (vFileFlash3 != null) {
    		stream.writeBoolean(true);
        	vFileFlash3.write(stream);
    	} else {
    		stream.writeBoolean(false);
    	}

    	if (nandMemory != null) {
    		synchronizeAll();

    		stream.writeBoolean(true);
    		nandMemory.write(stream);
    		nandSpareMemory.write(stream);

    		syncFlash0.write(stream);
    		syncFlash1.write(stream);
    		syncFlash2.write(stream);
    		if (syncFlash3 != null) {
    			stream.writeBoolean(true);
	    		syncFlash3.write(stream);
    		} else {
    			stream.writeBoolean(false);
    		}
    		syncIpl.write(stream);
    	} else {
    		stream.writeBoolean(false);
    	}

    	super.write(stream);
    }

    private void synchronizeAll() {
    	if (syncFlash0 != null) {
    		syncFlash0.synchronize();
    	}
    	if (syncFlash1 != null) {
    		syncFlash1.synchronize();
    	}
    	if (syncFlash2 != null) {
    		syncFlash2.synchronize();
    	}
		if (syncFlash3 != null) {
			syncFlash3.synchronize();
		}
    	if (syncIpl != null) {
    		syncIpl.synchronize();
    	}
    }

    public void reset() {
		// Flush any pending writes before doing the reset
    	synchronizeAll();

		if (vFileFlash0 != null) {
			vFileFlash0.ioClose();
			vFileFlash0 = null;
		}
		if (vFileFlash1 != null) {
			vFileFlash1.ioClose();
			vFileFlash1 = null;
		}
		if (vFileFlash2 != null) {
			vFileFlash2.ioClose();
			vFileFlash2 = null;
		}
		if (vFileFlash3 != null) {
			vFileFlash3.ioClose();
			vFileFlash3 = null;
		}

		syncFlash0 = null;
		syncFlash1 = null;
		syncFlash2 = null;
		syncFlash3 = null;
		syncIpl = null;
		nandMemory = null;
		nandMemoryPointer = null;
		nandSpareMemory = null;
		nandSpareMemoryPointer = null;
		initNandInProgress = false;

		writeProtected = true;
		scramble = 0;

		Nand.init();
		ppnToLbn = new int[getTotalPages()];
		flash1LbnStart = flash0LbnStart + (getTotalSectorsFlash0() / pagesPerBlock) + 1; // 0x602 on PSP-1000, 0xA42 on PSP-2000
		flash2LbnStart = flash1LbnStart + (getTotalSectorsFlash1() / pagesPerBlock) + 1; // 0x702 on PSP-1000, 0xB82 on PSP-2000
		flash3LbnStart = flash2LbnStart + (getTotalSectorsFlash2() / pagesPerBlock) + 1; // 0x742 on PSP-1000, 0xC82 on PSP-2000
		flash4LbnStart = flash3LbnStart + (getTotalSectorsFlash3() / pagesPerBlock) + 1; // 0x77E on PSP-1000, 0xECA on PSP-2000
		flash5LbnStart = flash4LbnStart + (getTotalSectorsFlash4() / pagesPerBlock) + 1; // None  on PSP-1000, 0xEFE on PSP-2000

		dumpBlocks = readBytes("nand.block");
		dumpSpares = readBytes("nand.spare");
		dumpResults = readInts("nand.result");

		final int startPpnToLbn = idStoragePpnEnd + 1;
		final int numberUsedBlocks = 0x1E0;
		final int numberBlocks = isSmallNand() ? 0x1F0 : 0x1F8;
		final int numberFreePages = (numberBlocks - numberUsedBlocks) * pagesPerBlock;
		Arrays.fill(ppnToLbn, 0, startPpnToLbn, 0x0000);
		for (int ppn = startPpnToLbn, lbn = 0; ppn < ppnToLbn.length; ) {
			Arrays.fill(ppnToLbn, ppn, ppn + pagesPerBlock, lbn);
			ppn += pagesPerBlock;
			lbn++;

			// The blocks from 0x1E0 to 0x1F0/0x1F8 have to be free
			if ((lbn % numberUsedBlocks) == 0) {
				Arrays.fill(ppnToLbn, ppn, ppn + numberFreePages, 0xFFFF);
				ppn += numberFreePages;
			}
		}

		if (!emulateNand) {
			byte[] fuseId = readBytes("nand.fuseid");
			if (fuseId != null && fuseId.length == 8) {
				Modules.sceSysregModule.setFuseId(Utilities.readUnaligned64(fuseId, 0));
			}
		}
    }

    private static byte[] readBytes(String fileName) {
    	byte[] bytes = null;
    	try {
    		File file = new File(fileName);
			InputStream is = new FileInputStream(file);
			bytes = new byte[(int) file.length()];
			is.read(bytes);
			is.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}

    	return bytes;
    }

    private static int[] readInts(String fileName) {
    	byte[] bytes = readBytes(fileName);
    	if (bytes == null) {
    		return null;
    	}

    	int[] ints = new int[bytes.length / 4];
    	ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(ints);

    	return ints;
    }

    public static void scramblePage(int scramble, int ppn, int[] source, int[] destination) {
    	int scrmb = rotateRight(scramble, 21);
    	int key = rotateRight(ppn, 17) ^ (scrmb * 7);
    	int scrambleOffset = (((ppn ^ scrmb) & 0x1F) << 4) >> 2;

    	final int pageSize4 = pageSize >> 2;
    	for (int i = 0; i < pageSize4; ) {
    		int value0 = source[i++];
    		int value1 = source[i++];
    		int value2 = source[i++];
    		int value3 = source[i++];
    		if (scrambleOffset >= pageSize4) {
    			scrambleOffset -= pageSize4;
    		}
    		destination[scrambleOffset++] = value0 + key;
    		key += value0;
    		destination[scrambleOffset++] = value1 + key;
    		key ^= value1;
    		destination[scrambleOffset++] = value2 + key;
    		key -= value2;
    		destination[scrambleOffset++] = value3 + key;
    		key += value3;
    		key += scrmb;
    		key = Integer.reverse(key);
    	}
    }

    public static void descramblePage(int scramble, int ppn, int[] source, int[] destination) {
    	int scrmb = rotateRight(scramble, 21);
    	int key = rotateRight(ppn, 17) ^ (scrmb * 7);
    	int scrambleOffset = (((ppn ^ scrmb) & 0x1F) << 4) >> 2;

    	final int pageSize4 = pageSize >> 2;
    	for (int i = 0; i < pageSize4; ) {
    		int value0 = source[scrambleOffset++];
    		int value1 = source[scrambleOffset++];
    		int value2 = source[scrambleOffset++];
    		int value3 = source[scrambleOffset++];
    		if (scrambleOffset >= pageSize4) {
    			scrambleOffset -= pageSize4;
    		}
    		value0 -= key;
    		key += value0;
    		destination[i++] = value0;
    		value1 -= key;
    		key ^= value1;
    		destination[i++] = value1;
    		value2 -= key;
    		key -= value2;
    		destination[i++] = value2;
    		value3 -= key;
    		key += value3;
    		destination[i++] = value3;
    		key += scrmb;
    		key = Integer.reverse(key);
    	}
    }

    protected void descramblePage(int ppn, TPointer user, byte[] blocks, int offset) {
    	int scrmb = rotateRight(scramble, 21);
    	int key = rotateRight(ppn, 17) ^ (scrmb * 7);
    	int scrambleOffset = ((ppn ^ scrmb) & 0x1F) << 4;

    	for (int i = 0; i < pageSize; i += 16) {
    		int value0 = Utilities.readUnaligned32(blocks, offset + scrambleOffset);
    		int value1 = Utilities.readUnaligned32(blocks, offset + scrambleOffset + 4);
    		int value2 = Utilities.readUnaligned32(blocks, offset + scrambleOffset + 8);
    		int value3 = Utilities.readUnaligned32(blocks, offset + scrambleOffset + 12);
    		scrambleOffset += 16;
    		if (scrambleOffset >= pageSize) {
    			scrambleOffset -= pageSize;
    		}
    		value0 -= key;
    		key += value0;
    		user.setValue32(i, value0);
    		value1 -= key;
    		key ^= value1;
    		user.setValue32(i + 4, value1);
    		value2 -= key;
    		key -= value2;
    		user.setValue32(i + 8, value2);
    		value3 -= key;
    		key += value3;
    		user.setValue32(i + 12, value3);
    		key += scrmb;
    		key = Integer.reverse(key);
    	}
    }

    protected void descramble(int ppn, TPointer user, int len, byte[] blocks, int offset) {
    	for (int i = 0; i < len; i++) {
    		descramblePage(ppn, user, blocks, offset);
    		ppn++;
    		offset += pageSize;
    		user.add(pageSize);
    	}
    }

    private void readMasterBootRecord0(TPointer buffer) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("readMasterBootRecord0"));
    	}

    	// First partition entry
    	int partitionEntry = 446;

    	// Status of physical drive
    	buffer.setValue8(partitionEntry + 0, (byte) 0x00);
    	// CHS address of first absolute sector in partition
    	buffer.setValue8(partitionEntry + 1, (byte) 0x00);
    	buffer.setValue8(partitionEntry + 2, (byte) 0x01);
    	buffer.setValue8(partitionEntry + 3, (byte) 0x01);
    	// Partition type
    	buffer.setValue8(partitionEntry + 4, (byte) (isSmallNand() ? 0x05 : 0x0F));
    	// CHS address of last absolute sector in partition
    	buffer.setValue8(partitionEntry + 5, (byte) 0x01);
    	buffer.setValue8(partitionEntry + 6, (byte) 0xE0);
    	buffer.setValue8(partitionEntry + 7, (byte) (isSmallNand() ? 0xBE : 0xFF));
    	// LBA of first absolute sector in the partition
    	buffer.setUnalignedValue32(partitionEntry + 8, 0x40);
    	// Number of sectors in partition
    	buffer.setUnalignedValue32(partitionEntry + 12, isSmallNand() ? 0xEF80 : 0x1DF80);

    	// Boot signature
    	buffer.setValue8(510, (byte) 0x55);
    	buffer.setValue8(511, (byte) 0xAA);
    }

    private void readMasterBootRecordFlash0(TPointer buffer) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("readMasterBootRecordFlash0"));
    	}

    	// First partition entry
    	int partitionEntry = 446;

    	// Status of physical drive
    	buffer.setValue8(partitionEntry + 0, (byte) 0x00);
    	// CHS address of first absolute sector in partition
    	buffer.setValue8(partitionEntry + 1, (byte) 0x01);
    	buffer.setValue8(partitionEntry + 2, (byte) 0x01);
    	buffer.setValue8(partitionEntry + 3, (byte) 0x01);
    	// Partition type
    	buffer.setValue8(partitionEntry + 4, (byte) (isSmallNand() ? 0x01 : 0x0E)); // FAT12 or FAT16B
    	// CHS address of last absolute sector in partition
    	buffer.setValue8(partitionEntry + 5, (byte) 0x01);
    	buffer.setValue8(partitionEntry + 6, (byte) 0xE0);
    	buffer.setValue8(partitionEntry + 7, (byte) (isSmallNand() ? 0x00 : 0xFF));
    	// LBA of first absolute sector in the partition
    	buffer.setUnalignedValue32(partitionEntry + 8, 0x20);
    	// Number of sectors in partition
    	buffer.setUnalignedValue32(partitionEntry + 12, (flash1LbnStart - flash0LbnStart - 1) * pagesPerBlock);

    	// Second partition entry
    	partitionEntry += 16;

    	// Status of physical drive
    	buffer.setValue8(partitionEntry + 0, (byte) 0x00);
    	// CHS address of first absolute sector in partition
    	buffer.setValue8(partitionEntry + 1, (byte) 0x00);
    	buffer.setValue8(partitionEntry + 2, (byte) 0xC1);
    	buffer.setValue8(partitionEntry + 3, (byte) (isSmallNand() ? 0x01 : 0xFF));
    	// Partition type
    	buffer.setValue8(partitionEntry + 4, (byte) (isSmallNand() ? 0x05 : 0x0F));
    	// CHS address of last absolute sector in partition
    	buffer.setValue8(partitionEntry + 5, (byte) 0x01);
    	buffer.setValue8(partitionEntry + 6, (byte) 0xE0);
    	buffer.setValue8(partitionEntry + 7, (byte) (isSmallNand() ? 0x80 : 0xFF));
    	// LBA of first absolute sector in the partition
    	buffer.setUnalignedValue32(partitionEntry + 8, (flash1LbnStart - flash0LbnStart) * pagesPerBlock);
    	// Number of sectors in partition
    	buffer.setUnalignedValue32(partitionEntry + 12, (flash2LbnStart - flash1LbnStart) * pagesPerBlock);

    	// Boot signature
    	buffer.setValue8(510, (byte) 0x55);
    	buffer.setValue8(511, (byte) 0xAA);
    }

    private void readMasterBootRecordFlash1(TPointer buffer) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("readMasterBootRecordFlash1"));
    	}

    	// First partition entry
    	int partitionEntry = 446;

    	// Status of physical drive
    	buffer.setValue8(partitionEntry + 0, (byte) 0x00);
    	// CHS address of first absolute sector in partition
    	buffer.setValue8(partitionEntry + 1, (byte) 0x01);
    	buffer.setValue8(partitionEntry + 2, (byte) 0xC1);
    	buffer.setValue8(partitionEntry + 3, (byte) (isSmallNand() ? 0x01 : 0xFF));
    	// Partition type
    	buffer.setValue8(partitionEntry + 4, (byte) (isSmallNand() ? 0x01 : 0x0E)); // FAT12 or FAT16B
    	// CHS address of last absolute sector in partition
    	buffer.setValue8(partitionEntry + 5, (byte) 0x01);
    	buffer.setValue8(partitionEntry + 6, (byte) 0xE0);
    	buffer.setValue8(partitionEntry + 7, (byte) (isSmallNand() ? 0x80 : 0xFF));
    	// LBA of first absolute sector in the partition
    	buffer.setUnalignedValue32(partitionEntry + 8, 0x20);
    	// Number of sectors in partition
    	buffer.setUnalignedValue32(partitionEntry + 12, (flash2LbnStart - flash1LbnStart - 1) * pagesPerBlock);

    	// Second partition entry
    	partitionEntry += 16;

    	// Status of physical drive
    	buffer.setValue8(partitionEntry + 0, (byte) 0x00);
    	// CHS address of first absolute sector in partition
    	buffer.setValue8(partitionEntry + 1, (byte) 0x00);
    	buffer.setValue8(partitionEntry + 2, (byte) 0xC1);
    	buffer.setValue8(partitionEntry + 3, (byte) (isSmallNand() ? 0x81 : 0xFF));
    	// Partition type
    	buffer.setValue8(partitionEntry + 4, (byte) (isSmallNand() ? 0x05 : 0x0F));
    	// CHS address of last absolute sector in partition
    	buffer.setValue8(partitionEntry + 5, (byte) 0x01);
    	buffer.setValue8(partitionEntry + 6, (byte) 0xE0);
    	buffer.setValue8(partitionEntry + 7, (byte) (isSmallNand() ? 0xA0 : 0xFF));
    	// LBA of first absolute sector in the partition
    	buffer.setUnalignedValue32(partitionEntry + 8, (flash2LbnStart - flash0LbnStart) * pagesPerBlock);
    	// Number of sectors in partition
    	buffer.setUnalignedValue32(partitionEntry + 12, isSmallNand() ? 0x800 : 0x2000);

    	// Boot signature
    	buffer.setValue8(510, (byte) 0x55);
    	buffer.setValue8(511, (byte) 0xAA);
    }

    private void readMasterBootRecordFlash2(TPointer buffer) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("readMasterBootRecordFlash2"));
    	}

    	// First partition entry
    	int partitionEntry = 446;

    	// Status of physical drive
    	buffer.setValue8(partitionEntry + 0, (byte) 0x00);
    	// CHS address of first absolute sector in partition
    	buffer.setValue8(partitionEntry + 1, (byte) 0x01);
    	buffer.setValue8(partitionEntry + 2, (byte) 0xC1);
    	buffer.setValue8(partitionEntry + 3, (byte) (isSmallNand() ? 0x81 : 0xFF));
    	// Partition type
    	buffer.setValue8(partitionEntry + 4, (byte) (isSmallNand() ? 0x01 : 0x0E)); // FAT12 or FAT16B
    	// CHS address of last absolute sector in partition
    	buffer.setValue8(partitionEntry + 5, (byte) 0x01);
    	buffer.setValue8(partitionEntry + 6, (byte) 0xE0);
    	buffer.setValue8(partitionEntry + 7, (byte) (isSmallNand() ? 0xA0 : 0xFF));
    	// LBA of first absolute sector in the partition
    	buffer.setUnalignedValue32(partitionEntry + 8, 0x20);
    	// Number of sectors in partition
    	buffer.setUnalignedValue32(partitionEntry + 12, (flash3LbnStart - flash2LbnStart - 1) * pagesPerBlock);

    	// Second partition entry
    	partitionEntry += 16;

    	// Status of physical drive
    	buffer.setValue8(partitionEntry + 0, (byte) 0x00);
    	// CHS address of first absolute sector in partition
    	buffer.setValue8(partitionEntry + 1, (byte) 0x00);
    	buffer.setValue8(partitionEntry + 2, (byte) 0xC1);
    	buffer.setValue8(partitionEntry + 3, (byte) (isSmallNand() ? 0xA1 : 0xFF));
    	// Partition type
    	buffer.setValue8(partitionEntry + 4, (byte) (isSmallNand() ? 0x05 : 0x0F));
    	// CHS address of last absolute sector in partition
    	buffer.setValue8(partitionEntry + 5, (byte) 0x01);
    	buffer.setValue8(partitionEntry + 6, (byte) 0xE0);
    	buffer.setValue8(partitionEntry + 7, (byte) (isSmallNand() ? 0xBE : 0xFF));
    	// LBA of first absolute sector in the partition
    	buffer.setUnalignedValue32(partitionEntry + 8, (flash3LbnStart - flash0LbnStart) * pagesPerBlock);
    	// Number of sectors in partition
    	buffer.setUnalignedValue32(partitionEntry + 12, isSmallNand() ? 0x780 : 0x4900);

    	// Boot signature
    	buffer.setValue8(510, (byte) 0x55);
    	buffer.setValue8(511, (byte) 0xAA);
    }

    private void readMasterBootRecordFlash3(TPointer buffer) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("readMasterBootRecordFlash3"));
    	}

    	// First partition entry
    	int partitionEntry = 446;

    	// Status of physical drive
    	buffer.setValue8(partitionEntry + 0, (byte) 0x00);
    	// CHS address of first absolute sector in partition
    	buffer.setValue8(partitionEntry + 1, (byte) 0x01);
    	buffer.setValue8(partitionEntry + 2, (byte) 0xC1);
    	buffer.setValue8(partitionEntry + 3, (byte) (isSmallNand() ? 0xA1 : 0xFF));
    	// Partition type
    	buffer.setValue8(partitionEntry + 4, (byte) (isSmallNand() ? 0x01 : 0x0E)); // FAT12 or FAT16B
    	// CHS address of last absolute sector in partition
    	buffer.setValue8(partitionEntry + 5, (byte) 0x01);
    	buffer.setValue8(partitionEntry + 6, (byte) 0xE0);
    	buffer.setValue8(partitionEntry + 7, (byte) (isSmallNand() ? 0xBE : 0xFF));
    	// LBA of first absolute sector in the partition
    	buffer.setUnalignedValue32(partitionEntry + 8, 0x20);
    	// Number of sectors in partition
    	buffer.setUnalignedValue32(partitionEntry + 12, (flash4LbnStart - flash3LbnStart - 1) * pagesPerBlock);

    	if (!isSmallNand()) {
        	// Second partition entry
        	partitionEntry += 16;

        	// Status of physical drive
        	buffer.setValue8(partitionEntry + 0, (byte) 0x00);
        	// CHS address of first absolute sector in partition
        	buffer.setValue8(partitionEntry + 1, (byte) 0x00);
        	buffer.setValue8(partitionEntry + 2, (byte) 0xC1);
        	buffer.setValue8(partitionEntry + 3, (byte) 0xFF);
        	// Partition type
        	buffer.setValue8(partitionEntry + 4, (byte) 0x0F);
        	// CHS address of last absolute sector in partition
        	buffer.setValue8(partitionEntry + 5, (byte) 0x01);
        	buffer.setValue8(partitionEntry + 6, (byte) 0xE0);
        	buffer.setValue8(partitionEntry + 7, (byte) 0xFF);
        	// LBA of first absolute sector in the partition
        	buffer.setUnalignedValue32(partitionEntry + 8, (flash4LbnStart - flash0LbnStart) * pagesPerBlock);
        	// Number of sectors in partition
        	buffer.setUnalignedValue32(partitionEntry + 12, 0x680);
    	}

    	// Boot signature
    	buffer.setValue8(510, (byte) 0x55);
    	buffer.setValue8(511, (byte) 0xAA);
    }

    private void readMasterBootRecordFlash4(TPointer buffer) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("readMasterBootRecordFlash4"));
    	}

    	// First partition entry
    	int partitionEntry = 446;

    	// Status of physical drive
    	buffer.setValue8(partitionEntry + 0, (byte) 0x00);
    	// CHS address of first absolute sector in partition
    	buffer.setValue8(partitionEntry + 1, (byte) 0x01);
    	buffer.setValue8(partitionEntry + 2, (byte) 0xC1);
    	buffer.setValue8(partitionEntry + 3, (byte) 0xFF);
    	// Partition type
    	buffer.setValue8(partitionEntry + 4, (byte) 0x0E);
    	// CHS address of last absolute sector in partition
    	buffer.setValue8(partitionEntry + 5, (byte) 0x01);
    	buffer.setValue8(partitionEntry + 6, (byte) 0xE0);
    	buffer.setValue8(partitionEntry + 7, (byte) 0xFF);
    	// LBA of first absolute sector in the partition
    	buffer.setUnalignedValue32(partitionEntry + 8, 0x20);
    	// Number of sectors in partition
    	buffer.setUnalignedValue32(partitionEntry + 12, (flash5LbnStart - flash4LbnStart - 1) * pagesPerBlock);

    	// Boot signature
    	buffer.setValue8(510, (byte) 0x55);
    	buffer.setValue8(511, (byte) 0xAA);
    }

    private void readFile(TPointer buffer, IVirtualFile vFile, int ppn, int lbnStart) {
    	int lbn = ppnToLbn[ppn];
    	int sectorNumber = (lbn - lbnStart) * pagesPerBlock + (ppn % pagesPerBlock);
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("readFile ppn=0x%X, lbnStart=0x%X, lbn=0x%X, sectorNumber=0x%X, vFile=%s", ppn, lbnStart, lbn, sectorNumber, vFile));
    	}
    	readFile(buffer, vFile, sectorNumber);
    }

    private void readFile(TPointer buffer, IVirtualFile vFile, int sectorNumber) {
    	vFile.ioLseek(sectorNumber * pageSize);
    	vFile.ioRead(buffer, pageSize);
    }

    private int getIdStorageKey(int page) {
    	if (page < 0 || page >= idStorageKeys.length) {
    		return -1;
    	}

    	return idStorageKeys[page];
    }

    private void readIdStoragePage(TPointer buffer, int page) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("readIdStoragePage page=0x%X", page));
    	}

    	switch (page) {
    		case 0:
    			buffer.memset((byte) 0xFF, pageSize);
    			for (int i = 0; i < idStorageKeys.length; i++) {
    				buffer.setValue16(i << 1, (short) idStorageKeys[i]);
    			}
    			break;
    		case 1:
    			buffer.memset((byte) 0xFF, pageSize);
    			break;
			default:
				int key = getIdStorageKey(page);
				if (key >= 0) {
					Modules.sceIdStorageModule.hleIdStorageReadLeaf(key, buffer);
				}
				break;
    	}
    }

    private int computeEcc(SceNandSpare spare) {
    	int t0, t1, t2, t3, t4, t5, t6, t7, t9;
    	int s0, s1, s2, s3, s4, s5;
    	int a0, a3;
    	int v0, v1;

    	s3 = spare.blockFmt;
    	v0 = spare.blockStat;
    	t4 = spare.lbn >> 8;
    	t2 = spare.lbn & 0xFF;
    	s0 = spare.id & 0xFF;
    	t7 = s3 ^ v0;
    	s1 = (spare.id >> 8) & 0xFF;
    	t9 = t4 ^ t7;
    	s5 = (spare.id >> 16) & 0xFF;
    	t6 = t2 ^ t9;
    	v1 = s0 ^ t6;
    	t9 = (spare.id >> 24) & 0xFF;
    	a3 = s0 ^ s1;
    	t5 = s1 ^ v1;
    	s2 = t4 ^ t2;
    	t0 = s5 ^ t5;
    	t3 = s5 ^ a3;
    	t5 = s5 ^ s2;
    	s4 = t9 ^ t0;
    	a3 = t9 ^ t3;
    	a0 = s4 & 0xFF;
    	s2 = v0 ^ t2;
    	s4 = t6 & 0xFF;
    	t3 = s0 ^ t7;
    	t6 = a3 & 0xFF;
    	t7 = t9 ^ t5;
    	v0 = 0x6996;
    	t0 = t7 & 0xFF;
    	a3 = s4 >> 4;
    	t7 = s3 ^ t4;
    	t5 = s4 & 0x0F;
    	s3 = s1 ^ s2;
    	s4 = t6 & 0x0F;
    	s2 = s1 ^ t3;
    	t4 = t6 >> 4;
    	s1 = a0 & 0xCC;
    	t6 = v0 >> t4;
    	v1 = v0 >> s4;
    	t3 = s2 & 0xFF;
    	t4 = a0 >> 4;
    	t1 = v0 >> t5;
    	s2 = s1 >> 4;
    	t5 = v0 >> a3;
    	s1 = s0 ^ t7;
    	a3 = a0 & 0x0F;
    	s0 = t9 ^ s3;
    	t7 = a0 & 0x0C;
    	s3 = t0 >> 4;
    	s4 = t0 & 0x0F;
    	t2 = s0 & 0xFF;
    	s2 = v0 >> s2;
    	s0 = v0 >> t7;
    	t0 = v0 >> s4;
    	t7 = v0 >> a3;
    	s4 = v0 >> s3;
    	a3 = v0 >> t4;
    	t1 = t1 ^ t5;
    	t4 = s5 ^ s1;
    	t5 = v1 ^ t6;
    	s5 = t3 >> 4;
    	s3 = a0 & 0xAA;
    	t6 = a0 & 0x03;
    	s1 = (a0 >> 4) & 0x03;
    	t3 = t3 & 0x0F;
    	s1 = v0 >> s1;
    	s5 = v0 >> s5;
    	s0 = s0 ^ s2;
    	t0 = t0 ^ s4;
    	t4 = t4 & 0xFF;
    	t6 = v0 >> t6;
    	s2 = t7 & 0x01;
    	s4 = t2 >> 4;
    	t3 = v0 >> t3;
    	v1 = t5 & 0x01;
    	s3 = s3 >> 4;
    	t5 = a0 & 0x0A;
    	a3 = a3 & 0x01;
    	t1 = t1 & 0x01;
    	t2 = t2 & 0x0F;
    	t6 = t6 ^ s1;
    	t3 = t3 ^ s5;
    	s3 = v0 >> s3;
    	s5 = v0 >> t5;
    	t7 = s2 << 2;	// bit 0x0004
    	s4 = v0 >> s4;
    	a3 = a3 << 8;	// bit 0x0100
    	s2 = t4 >> 4;
    	t2 = v0 >> t2;
    	t1 = t1 << 5;	// bit 0x0020
    	s1 = a0 & 0x55;
    	s0 = s0 & 0x01;
    	t0 = t0 & 0x01;
    	v1 = v1 << 11;	// bit 0x0800
    	t4 = t4 & 0x0F;
    	t5 = s5 ^ s3;
    	t2 = t2 ^ s4;
    	s5 = a3 | t7;
    	s4 = t6 & 0x01;
    	s2 = v0 >> s2;
    	t7 = t3 & 0x01;
    	v1 = v1 | t1;
    	s1 = s1 >> 4;
    	t1 = v0 >> t4;
    	s0 = s0 << 7;	// bit 0x0080
    	t0 = t0 << 10;	// bit 0x0400
    	a0 = a0 & 0x05;
    	s3 = s5 | s0;
    	s5 = t1 ^ s2;
    	s0 = s4 << 1;	// bit 0x0002
    	s2 = v0 >> a0;
    	t1 = v1 | t0;
    	v0 = v0 >> s1;
    	t0 = t5 & 0x01;
    	s1 = t7 << 4;	// bit 0x0010
    	s4 = t2 & 0x01;
    	t7 = s2 ^ v0;
    	t6 = s5 & 0x01;
    	s2 = s3 | s0;
    	s0 = t1 | s1;
    	s3 = t0 << 6;	// bit 0x0040
    	s1 = s4 << 9;	// bit 0x0200
    	v0 = s0 | s1;
    	t3 = s2 | s3;
    	t2 = t7 & 0x01;	// bit 0x0001
    	t1 = t6 << 3;	// bit 0x0008
    	a0 = v0 | t1;
    	t0 = t3 | t2;
    	v0 = t0 | a0;

    	return v0;
    }

    private void openFileIpl() {
    	if (vFileIpl != null) {
    		return;
    	}

    	try {
			vFileIpl = new LocalVirtualFile(new SeekableRandomFile("nand.ipl.bin", "rw"));

			if (vFileIpl.length() == 0L) {
				byte[] buffer = new byte[pageSize * pagesPerBlock];
    			for (int iplPpn = iplPpnStart, offset = 0; iplPpn < iplPpnEnd; iplPpn += pagesPerBlock, offset += 2) {
    				Utilities.writeUnaligned16(buffer, offset, iplPpn / pagesPerBlock);
    			}

    			vFileIpl.ioLseek(0L);
				for (int n = iplTablePpnStart; n <= iplTablePpnEnd; n += pagesPerBlock) {
	    			vFileIpl.ioWrite(buffer, 0, buffer.length);
				}
				vFileIpl.ioLseek(0L);
			}
		} catch (FileNotFoundException e) {
			log.error("openFileIpl", e);
		}
    }

    public static boolean isSmallNand() {
    	return Nand.getTotalSizeMb() <= 32;
    }

    private static int getTotalSectorsFlash0() {
		return isSmallNand() ? 0xBFE0 : 0x147E0;
    }

    private static int getTotalSectorsFlash1() {
		return isSmallNand() ? 0x1FE0 : 0x27E0;
    }

    private static int getTotalSectorsFlash2() {
		return isSmallNand() ? 0x7E0 : 0x1FE0;
    }

    private static int getTotalSectorsFlash3() {
		return isSmallNand() ? 0x760 : 0x48E0;
    }

    private static int getTotalSectorsFlash4() {
		return isSmallNand() ? 0 : 0x660;
    }

    private void openFileFlash0() {
		if (vFileFlash0 != null) {
			return;
		}

		IVirtualFileSystem vfs = new LocalVirtualFileSystem(Settings.getInstance().getDirectoryMapping("flash0"), false);

		if (!RuntimeContextLLE.isLLEActive()) {
			// Apply patches for some files as required
			vfs = new PatchFileVirtualFileSystem(vfs);

			// All the PRX files need to be compressed so that they can fit
			// into the space available on flash0.
			vfs = new CompressPrxVirtualFileSystem(vfs);
		}

		vFileFlash0 = new Fat12VirtualFile("flash0:", vfs, getTotalSectorsFlash0());
		vFileFlash0.scan();
    }

    private void openFileFlash1() {
		if (vFileFlash1 != null) {
			return;
		}

		IVirtualFileSystem vfs = new LocalVirtualFileSystem(Settings.getInstance().getDirectoryMapping("flash1"), false);
		vFileFlash1 = new Fat12VirtualFile("flash1:", vfs, getTotalSectorsFlash1());
		vFileFlash1.scan();
    }

    private void openFileFlash2() {
		if (vFileFlash2 != null) {
			return;
		}

		IVirtualFileSystem vfs = new LocalVirtualFileSystem(Settings.getInstance().getDirectoryMapping("flash2"), false);
		vFileFlash2 = new Fat12VirtualFile("flash2:", vfs, getTotalSectorsFlash2());
		vFileFlash2.scan();
    }

    private void openFileFlash3() {
		if (vFileFlash3 != null) {
			return;
		}

		IVirtualFileSystem vfs = new LocalVirtualFileSystem(Settings.getInstance().getDirectoryMapping("flash3"), false);
		vFileFlash3 = new Fat12VirtualFile("flash3:", vfs, getTotalSectorsFlash3());
		vFileFlash3.scan();
    }

    private boolean isEmptyPage(TPointer user, int emptyValue) {
    	IMemoryReader pageReader = MemoryReader.getMemoryReader(user, pageSize, 4);
    	for (int i = 0; i < pageSize; i += 4) {
    		int value = pageReader.readNext();
    		if (value != emptyValue) {
    			return false;
    		}
    	}

    	return true;
    }

    public int hleNandReadPages(int ppn, TPointer user, TPointer spare, int len, boolean raw, boolean spareUserEcc, boolean isLLE) {
    	boolean emptyPages[] = new boolean[len];

		initNandInMemory();

		if (user.isNotNull()) {
	    	if (dumpBlocks != null && !emulateNand) {
	    		if (scramble != 0) {
	    			descramble(ppn, user, len, dumpBlocks, ppn * pageSize);
	    		} else {
	    			Utilities.writeBytes(user, len * pageSize, dumpBlocks, ppn * pageSize);
	    		}
	    	} else if (nandMemory != null) {
	    		user.memcpy(nandMemory.getPointer(ppn * pageSize), len * pageSize);
	    	} else {
	    		for (int i = 0; i < len; i++) {
	    			user.clear(pageSize);
	    			int n = ppn + i;
	    			if (n >= iplTablePpnStart && n <= iplPpnEnd) {
		    			openFileIpl();
		    			readFile(user, vFileIpl, ppn + i - iplTablePpnStart);
		    			emptyPages[i] = isEmptyPage(user, 0xFFFFFFFF);
		    		} else if (n >= idStoragePpnStart && n <= idStoragePpnEnd) {
		    			readIdStoragePage(user, ppn + i - idStoragePpnStart);
		    		} else if (ppnToLbn[n] == 0) {
		    			// Master Boot Record
		    			readMasterBootRecord0(user);
		    		} else if (ppnToLbn[n] == flash0LbnStart) {
		    			// Master Boot Record
		    			readMasterBootRecordFlash0(user);
		    		} else if (ppnToLbn[n] > flash0LbnStart && ppnToLbn[n] < flash1LbnStart) {
		    			openFileFlash0();
		    			readFile(user, vFileFlash0, n, flash0LbnStart + 1);
		    		} else if (ppnToLbn[n] == flash1LbnStart) {
		    			// Master Boot Record
		    			readMasterBootRecordFlash1(user);
		    		} else if (ppnToLbn[n] > flash1LbnStart && ppnToLbn[n] < flash2LbnStart) {
		    			openFileFlash1();
		    			readFile(user, vFileFlash1, n, flash1LbnStart + 1);
		    		} else if (ppnToLbn[n] == flash2LbnStart) {
		    			// Master Boot Record
		    			readMasterBootRecordFlash2(user);
		    		} else if (ppnToLbn[n] > flash2LbnStart && ppnToLbn[n] < flash3LbnStart) {
		    			openFileFlash2();
		    			readFile(user, vFileFlash2, n, flash2LbnStart + 1);
		    		} else if (ppnToLbn[n] == flash3LbnStart) {
		    			// Master Boot Record
		    			readMasterBootRecordFlash3(user);
		    		} else if (!isSmallNand() && ppnToLbn[n] > flash3LbnStart && ppnToLbn[n] < flash4LbnStart) {
		    			openFileFlash3();
		    			readFile(user, vFileFlash3, n, flash3LbnStart + 1);
		    		} else if (!isSmallNand() && ppnToLbn[n] == flash4LbnStart) {
		    			// Master Boot Record
		    			readMasterBootRecordFlash4(user);
		    		}
		    		user.add(pageSize);
	    		}
	    	}
    	}

    	if (spare.isNotNull()) {
        	if (dumpSpares != null && !emulateNand) {
        		if (spareUserEcc) {
        			// Write the userEcc
        			Utilities.writeBytes(spare, len * 16, dumpSpares, ppn * 16);
        		} else {
        			// Do not return the userEcc
    	    		for (int i = 0; i < len; i++) {
    	    			Utilities.writeBytes(new TPointer(spare, i * 12), 12, dumpSpares, (ppn + i) * 16 + 4);
    	    		}
        		}
        	} else if (nandSpareMemory != null) {
        		if (spareUserEcc) {
        			// Write the userEcc
        			spare.memcpy(nandSpareMemory.getPointer(ppn << 4), len << 4);
        		} else {
        			// Do not return the userEcc
    	    		for (int i = 0; i < len; i++) {
    	    			spare.memcpy(i * 12, nandSpareMemory.getPointer(((ppn + i) << 4) + 4), 12);
    	    		}
        		}
        	} else {
    	    	SceNandSpare sceNandSpare = new SceNandSpare();
    	    	for (int i = 0; i < len; i++) {
    	    		int n = ppn + i;
        			sceNandSpare.blockFmt = n < 0x800 ? 0xFF : 0x00;
    	    		sceNandSpare.blockStat = 0xFF;
        			sceNandSpare.lbn = ppnToLbn[n];
    	    		if ((n >= iplTablePpnStart && n <= iplTablePpnEnd) || (n >= iplPpnStart && n <= iplPpnEnd)) {
    	    			if (emptyPages[i]) {
    	    				sceNandSpare.id = 0xFFFFFFFF;
    	    				sceNandSpare.lbn = 0xFFFF;
    	    			} else {
    	    				sceNandSpare.id = iplId; // For IPL area
    	    			}
    	    		} else if (n >= idStoragePpnStart && n <= idStoragePpnEnd) {
    	    			sceNandSpare.id = idStorageId; // For ID Storage area
    	    			sceNandSpare.lbn = 0x7301;
    	    		} else if (n > 0x80 && n <= 0x9F) {
	    				sceNandSpare.id = 0xFFFFFFFF;
	    				sceNandSpare.lbn = 0xFFFF;
    	    		}
    	    		sceNandSpare.reserved2[0] = 0xFF;
    	    		sceNandSpare.reserved2[1] = 0xFF;

	    			sceNandSpare.spareEcc = computeEcc(sceNandSpare) | 0xF000;

	    			if (!isLLE) {
		    			// All values are set to 0xFF when the lbn is 0xFFFF
	    	    		if (sceNandSpare.lbn == 0xFFFF) {
	    	    			sceNandSpare.userEcc[0] = 0xFF;
	    	    			sceNandSpare.userEcc[1] = 0xFF;
	    	    			sceNandSpare.userEcc[2] = 0xFF;
	    	    			sceNandSpare.reserved1 = 0xFF;
	    	    			sceNandSpare.blockFmt = 0xFF;
	    	    			sceNandSpare.blockStat = 0xFF;
	    	    			sceNandSpare.id = 0xFFFFFFFF;
	    	    			sceNandSpare.spareEcc = 0xFFFF;
	    	    			sceNandSpare.reserved2[0] = 0xFF;
	    	    			sceNandSpare.reserved2[1] = 0xFF;
	    	    		}
	    			}

    	    		if (spareUserEcc) {
    	    			sceNandSpare.write(spare, i * sceNandSpare.sizeof());
    	    		} else {
    	    			sceNandSpare.writeNoUserEcc(spare, i * sceNandSpare.sizeofNoEcc());
    	    		}
    	    	}
        	}
    	}

    	int result = 0;
    	if (dumpResults != null && !emulateNand) {
    		result = dumpResults[ppn / pagesPerBlock];
    	}

    	return result;
    }

    private void writeFile(TPointer buffer, IVirtualFile vFile, int ppn, int lbnStart) {
    	int lbn = ppnToLbn[ppn];
    	int sectorNumber = (lbn - lbnStart) * pagesPerBlock + (ppn % pagesPerBlock);
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("writeFile ppn=0x%X, lbnStart=0x%X, lbn=0x%X, sectorNumber=0x%X", ppn, lbnStart, lbn, sectorNumber));
    	}
    	writeFile(buffer, vFile, sectorNumber);
    }

    private void writeFile(TPointer buffer, IVirtualFile vFile, int sectorNumber) {
    	vFile.ioLseek(sectorNumber * pageSize);
    	vFile.ioWrite(buffer, pageSize);
    }

    public int hleNandWriteSparePages(int ppn, TPointer spare, int len, boolean raw, boolean spareUserEcc, boolean isLLE) {
    	int result = 0;

		initNandInMemory();

		if (spare.isNotNull()) {
    		if (nandSpareMemory != null) {
    			nandSpareMemoryPointer.memcpy(ppn << 4, spare, len << 4);

    			SceNandSpare sceNandSpare = new SceNandSpare();
	    		for (int i = 0; i < len; i++) {
		    		if (spareUserEcc) {
			    		sceNandSpare.read(spare, i * sceNandSpare.sizeof());
		    		} else {
			    		sceNandSpare.readNoUserEcc(spare, i * sceNandSpare.sizeofNoEcc());
		    		}

	    			int n = ppn + i;
	    			if (log.isDebugEnabled()) {
	    				log.debug(String.format("hleNandWriteSparePages ppn=0x%X: changed lbn=0x%X to lbn=0x%X", n, ppnToLbn[n], sceNandSpare.lbn));
	    			}
					ppnToLbn[n] = sceNandSpare.lbn;
	    		}
    		} else {
	    		SceNandSpare sceNandSpare = new SceNandSpare();
	    		for (int i = 0; i < len; i++) {
		    		if (spareUserEcc) {
			    		sceNandSpare.read(spare, i * sceNandSpare.sizeof());
		    		} else {
			    		sceNandSpare.readNoUserEcc(spare, i * sceNandSpare.sizeofNoEcc());
		    		}

	    			int n = ppn + i;
	    			if (n >= iplTablePpnStart && n <= iplTablePpnEnd) {
	    				if (sceNandSpare.lbn != 0xFFFF) {
	    					ppnToLbn[n] = sceNandSpare.lbn;
	    				}
	    			} else if (n >= iplPpnStart && n <= iplPpnEnd) {
	    				if (sceNandSpare.lbn != 0xFFFF) {
	    					ppnToLbn[n] = sceNandSpare.lbn;
	    				}
	    			} else if (sceNandSpare.lbn != 0xFFFF && ppnToLbn[n] != sceNandSpare.lbn) {
		    			int offset = n % pagesPerBlock;
		    			for (int j = offset; j < ppnToLbn.length; j += pagesPerBlock) {
		    				if (ppnToLbn[j] == sceNandSpare.lbn) {
		    					if (log.isDebugEnabled()) {
		    						log.debug(String.format("hleNandWriteSparePages moving lbn=0x%04X from ppn=0x%X to ppn=0x%X", sceNandSpare.lbn, j, ppn + i));
		    					}
		    					ppnToLbn[j] = 0xFFFF;
		    					break;
		    				}
		    			}

		    			if (ppnToLbn[n] != 0xFFFF) {
		    				log.error(String.format("hleNandWriteSparePages moving lbn=0x%04X to ppn=0x%X not being free (currently used for lbn=0x%04X)", sceNandSpare.lbn, n, ppnToLbn[n]));
		    			}
	    				ppnToLbn[n] = sceNandSpare.lbn;
		    		}
	    		}
    		}
    	}

    	return result;
    }

    private void notifyWrite(int ppn, int len) {
		for (int i = 0; i < len; i++) {
			int n = ppn + i;
			if (n >= iplTablePpnStart && n <= iplPpnEnd) {
				syncIpl.notifyWrite();
			} else if (ppnToLbn[n] > flash0LbnStart && ppnToLbn[n] < flash1LbnStart) {
				syncFlash0.notifyWrite();
    		} else if (ppnToLbn[n] > flash1LbnStart && ppnToLbn[n] < flash2LbnStart) {
				syncFlash1.notifyWrite();
    		} else if (ppnToLbn[n] > flash2LbnStart && ppnToLbn[n] < flash3LbnStart) {
				syncFlash2.notifyWrite();
    		} else if (ppnToLbn[n] > flash3LbnStart && ppnToLbn[n] < flash4LbnStart) {
    			if (syncFlash3 != null) {
    				syncFlash3.notifyWrite();
    			}
			}
		}
    }

    public int hleNandWriteUserPages(int ppn, TPointer user, int len, boolean raw, boolean isLLE) {
    	int result = 0;

    	synchronized(writeLock) {
    		initNandInMemory();

			if (user.isNotNull()) {
	    		if (nandMemory != null) {
	    			nandMemoryPointer.memcpy(ppn * pageSize, user, len * pageSize);
	    			notifyWrite(ppn, len);
	    		} else {
		    		for (int i = 0; i < len; i++) {
		    			int n = ppn + i;
		    			if (n >= iplTablePpnStart && n <= iplTablePpnEnd) {
		    				// Ignore
		    			} else if (n >= iplPpnStart && n <= iplPpnEnd) {
		    				openFileIpl();
		    				writeFile(user, vFileIpl, n - iplPpnStart);
		    			} else if (ppnToLbn[n] > flash0LbnStart && ppnToLbn[n] < flash1LbnStart) {
		    				openFileFlash0();
			    			writeFile(user, vFileFlash0, n, flash0LbnStart + 1);
			    		} else if (ppnToLbn[n] > flash1LbnStart && ppnToLbn[n] < flash2LbnStart) {
			    			openFileFlash1();
			    			writeFile(user, vFileFlash1, n, flash1LbnStart + 1);
			    		} else if (ppnToLbn[n] > flash2LbnStart && ppnToLbn[n] < flash3LbnStart) {
			    			openFileFlash2();
			    			writeFile(user, vFileFlash2, n, flash2LbnStart + 1);
			    		} else if (!isSmallNand() && ppnToLbn[n] > flash3LbnStart && ppnToLbn[n] < flash4LbnStart) {
			    			openFileFlash3();
			    			writeFile(user, vFileFlash3, n, flash3LbnStart + 1);
		    			} else {
		    				log.error(String.format("hleNandWriteUserPages unimplemented write on ppn=0x%X, lbn=0x%X", n, ppnToLbn[n]));
		    			}
		    			user.add(pageSize);
		    		}
	    		}
			}
    	}

    	return result;
    }

    private int hleNandWritePages(int ppn, TPointer user, TPointer spare, int len, boolean raw, boolean spareUserEcc, boolean isLLE) {
    	synchronized(writeLock) {
	    	int result = hleNandWriteSparePages(ppn, spare, len, raw, spareUserEcc, isLLE);
	    	if (result != 0) {
	    		return result;
	    	}

	    	return hleNandWriteUserPages(ppn, user, len, raw, isLLE);
    	}
    }

    public int hleNandEraseBlock(int ppn, boolean isLLE) {
    	synchronized(writeLock) {
	    	if (storeNandInMemory && isLLE) {
		    	int lbn = 0xFFFF;
				for (int i = 0; i < pagesPerBlock; i++) {
					int n = ppn + i;
					if (log.isDebugEnabled()) {
						log.debug(String.format("hleNandEraseBlock ppn=0x%X: changed lbn=0x%X to lbn=0x%X", n, ppnToLbn[n], lbn));
					}
					ppnToLbn[n] = lbn;
				}
	    	}
    	}

		return 0;
    }

    public int getLbnFromPpn(int ppn) {
    	return ppnToLbn[ppn];
    }

    public int getPpnFromLbn(int lbn) {
    	for (int ppn = 0; ppn < ppnToLbn.length; ppn++) {
    		if (ppnToLbn[ppn] == lbn) {
    			return ppn;
    		}
    	}

    	return -1;
    }

    @HLEFunction(nid = 0xB07C41D4, version = 150, jumpCall = true)
    public int sceNandGetPagesPerBlock() {
    	// Has no parameters
        return pagesPerBlock;
    }

    @HLEFunction(nid = 0xCE9843E6, version = 150)
    public int sceNandGetPageSize() {
    	// Has no parameters
        return pageSize;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xAE4438C7, version = 150, jumpCall = true)
    public int sceNandLock(int mode) {
    	sceNandSetWriteProtect(mode == 0);

        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x41FFA822, version = 150, jumpCall = true)
    public int sceNandUnlock() {
    	// Has no parameters
    	sceNandSetWriteProtect(true);

        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x01F09203, version = 150, jumpCall = true)
    public int sceNandIsBadBlock(int ppn) {
    	if ((ppn % pagesPerBlock) != 0) {
    		return -1;
    	}

    	int result = 0;
    	if (dumpSpares != null) {
    		int blockStat = dumpSpares[ppn * 16 + 5] & 0xFF;
    		if (blockStat == 0xFF) {
    			result = 0;
    		} else {
    			result = 1;
    		}
    	}

    	return result;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0BEE8F36, version = 150)
    public int sceNandSetScramble(int scramble) {
    	this.scramble = scramble;

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x84EE5D76, version = 150, jumpCall = true)
    public boolean sceNandSetWriteProtect(boolean protect) {
    	boolean result = writeProtected;

    	writeProtected = protect;

    	return result;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8932166A, version = 150, jumpCall = true)
    public int sceNandWritePagesRawExtra(int ppn, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=pageSize, usage=Usage.in) TPointer user, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=16, usage=Usage.in) TPointer spare, int len) {
    	return hleNandWritePages(ppn, user, spare, len, true, false, false);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5182C394, version = 150, jumpCall = true)
    public int sceNandReadExtraOnly(int ppn, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=16, usage=Usage.out) TPointer spare, int len) {
    	hleNandReadPages(ppn, TPointer.NULL, spare, len, true, true, false);
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x89BDCA08, version = 150, jumpCall = true)
    public int sceNandReadPages(int ppn, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=pageSize, usage=Usage.out) TPointer user, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=12, usage=Usage.out) TPointer spare, int len) {
    	return hleNandReadPages(ppn, user, spare, len, false, false, false);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE05AE88D, version = 150, jumpCall = true)
    public int sceNandReadPagesRawExtra(int ppn, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=pageSize, usage=Usage.out) TPointer user, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=12, usage=Usage.out) TPointer spare, int len) {
    	return hleNandReadPages(ppn, user, spare, len, true, false, false);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC32EA051, version = 150, jumpCall = true)
    public int sceNandReadBlockWithRetry(int ppn, TPointer user, TPointer spare) {
    	return hleNandReadPages(ppn, user, spare, pagesPerBlock, false, false, false);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB2B021E5, version = 150, jumpCall = true)
    public int sceNandWriteBlockWithVerify(int ppn, TPointer user, TPointer spare) {
    	return hleNandWritePages(ppn, user, spare, pagesPerBlock, false, false, false);
    }

    @HLEFunction(nid = 0xC1376222, version = 150, jumpCall = true)
    public int sceNandGetTotalBlocks() {
    	// Has no parameters
    	return Nand.getTotalBlocks();
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE41A11DE, version = 150, jumpCall = true)
    public int sceNandReadStatus() {
    	// Has no parameters
    	int result = 0;
    	if (!writeProtected) {
    		result |= 0x80; // not write protected
    	}

    	return result;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xEB0A0022, version = 150, jumpCall = true)
    public int sceNandEraseBlock(int ppn) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7AF7B77A, version = 150)
    public int sceNandReset(@CanBeNull @BufferInfo(usage=Usage.out) TPointer8 statusAddr) {
    	statusAddr.setValue(0);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = InternalSyscallNid, version = 150)
    public boolean sceNandIsReady() {
    	return true;
    }

    @HLEUnimplemented
    @HLEFunction(nid = InternalSyscallNid, version = 150)
    public int sceNandInit2() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = InternalSyscallNid, version = 150)
    public int sceNandTransferDataToNandBuf() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = InternalSyscallNid, version = 150)
    public int sceNandIntrHandler() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = InternalSyscallNid, version = 150)
    public int sceNandTransferDataFromNandBuf() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xFCDF7610, version = 150)
    public int sceNandReadId(@CanBeNull @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.out) TPointer8 id, int len) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x766756EF, version = 150)
    public int sceNandReadAccess(int ppn, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=pageSize, usage=Usage.out) TPointer user, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=12, usage=Usage.out) TPointer spare, int len, int mode) {
    	return hleNandReadPages(ppn, user, spare, len, false, false, false);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0ADC8686, version = 150)
    public int sceNandWriteAccess(int ppn, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=pageSize, usage=Usage.in) TPointer user, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=12, usage=Usage.in) TPointer spare, int len, int mode) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8AF0AB9F, version = 150, jumpCall = true)
    public int sceNandWritePages(int ppn, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=pageSize, usage=Usage.in) TPointer user, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=12, usage=Usage.in) TPointer spare, int len) {
    	return hleNandWritePages(ppn, user, spare, len, false, false, false);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC478C1DE, version = 150)
    public int sceNandReadPagesRawAll(int ppn, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=pageSize, usage=Usage.out) TPointer user, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=12, usage=Usage.out) TPointer spare, int len) {
    	return hleNandReadPages(ppn, user, spare, len, true, false, false);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5AC02755, version = 150)
    public int sceNandVerifyBlockWithRetry(int ppn, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=pageSize, usage=Usage.out) TPointer user, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=12, usage=Usage.out) TPointer spare) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8933B2E0, version = 150)
    public int sceNandEraseBlockWithRetry(int ppn) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC29DA136, version = 150, jumpCall = true)
    public int sceNandDoMarkAsBadBlock(int ppn) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2FF6081B, version = 150)
    public int sceNandDetectChipMakersBBM(int ppn) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xBADD5D46, version = 150)
    public int sceNandWritePagesRawAll(int ppn, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=pageSize, usage=Usage.in) TPointer user, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=12, usage=Usage.in) TPointer spare, int len) {
    	return hleNandWritePages(ppn, user, spare, len, true, false, false);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD897C343, version = 150)
    public int sceNandDetectChip() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3F76BC21, version = 150)
    public int sceNandDumpWearBBMSize() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xEBA0E6C6, version = 150)
    public int sceNandCountChipMakersBBM() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2674CFFE, version = 150)
    public int sceNandEraseAllBlock() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9B2AC433, version = 150, jumpCall = true)
    public int sceNandTestBlock() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x716CD2B2, version = 150)
    public int sceNandWriteBlock() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xEF55F193, version = 150)
    public int sceNandCalcEcc(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=8, usage=Usage.in) TPointer buffer) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x18B78661, version = 150)
    public int sceNandVerifyEcc() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x88CC9F72, version = 150)
    public int sceNandCorrectEcc(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=8, usage=Usage.inout) TPointer buffer, int ecc) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB795D2ED, version = 150, jumpCall = true)
    public int sceNandCollectEcc(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=8, usage=Usage.inout) TPointer buffer, int ecc) {
    	return sceNandCorrectEcc(buffer, ecc);
    }
}
