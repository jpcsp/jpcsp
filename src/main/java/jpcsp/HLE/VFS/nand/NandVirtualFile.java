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
package jpcsp.HLE.VFS.nand;

import static jpcsp.HLE.Modules.sceNandModule;
import static jpcsp.HLE.VFS.AbstractVirtualFileSystem.IO_ERROR;
import static jpcsp.hardware.Nand.pageSize;
import static jpcsp.hardware.Nand.pagesPerBlock;

import java.util.Map;

import org.apache.log4j.Logger;

import jpcsp.HLE.TPointer;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.modules.IoFileMgrForUser.IoOperation;
import jpcsp.HLE.modules.IoFileMgrForUser.IoOperationTiming;
import jpcsp.HLE.modules.sceNand;
import jpcsp.memory.ByteArrayMemory;
import jpcsp.util.Utilities;

public class NandVirtualFile implements IVirtualFile {
	private static Logger log = sceNand.log;
	private int startLbn;
	private int endLbn;
	private long position;
	private final byte[] currentPage = new byte[pageSize];
	private final ByteArrayMemory currentPageMemory = new ByteArrayMemory(currentPage);

	public NandVirtualFile(int startLbn, int endLbn) {
		this.startLbn = startLbn;
		this.endLbn = endLbn;
	}

	private int getPpn() {
		int pageNumber = (int) (position / pageSize);
		int lbn = startLbn + pageNumber / pagesPerBlock;
		int ppn = sceNandModule.getPpnFromLbn(lbn);
		ppn += pageNumber % pagesPerBlock;

		if (log.isTraceEnabled()) {
			log.trace(String.format("getPpn ppn=0x%X, lbn=0x%X", ppn, lbn));
		}

		return ppn;
	}

	private int getPageOffset() {
		return (int) (position % pageSize);
	}

	private void readPage(int ppn) {
		sceNandModule.hleNandReadPages(ppn, currentPageMemory.getPointer(), TPointer.NULL, 1, false, false, false);
		if (log.isTraceEnabled()) {
			log.trace(String.format("readPage ppn=0x%X: %s", ppn, Utilities.getMemoryDump(currentPage)));
		}
	}

	private void writePage(int ppn) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("writePage ppn=0x%X: %s", ppn, Utilities.getMemoryDump(currentPage)));
		}
		sceNandModule.hleNandWriteUserPages(ppn, currentPageMemory.getPointer(), 1, false, false);
	}

	@Override
	public int ioClose() {
		return 0;
	}

	@Override
	public int ioRead(TPointer outputPointer, int outputLength) {
		int readLength = 0;
		int outputOffset = 0;
		while (outputLength > 0) {
			int ppn = getPpn();
			readPage(ppn);
			int pageOffset = getPageOffset();
			int pageLength = pageSize - pageOffset;
			int length = Math.min(pageLength, outputLength);

			outputPointer.setArray(outputOffset, currentPage, pageOffset, length);

			outputLength -= length;
			outputOffset += length;
			position += length;
			readLength += length;
		}

		return readLength;
	}

	@Override
	public int ioRead(byte[] outputBuffer, int outputOffset, int outputLength) {
		int readLength = 0;
		while (outputLength > 0) {
			int ppn = getPpn();
			readPage(ppn);
			int pageOffset = getPageOffset();
			int pageLength = pageSize - pageOffset;
			int length = Math.min(pageLength, outputLength);

			System.arraycopy(currentPage, pageOffset, outputBuffer, outputOffset, length);

			outputLength -= length;
			outputOffset += length;
			position += length;
			readLength += length;
		}

		return readLength;
	}

	@Override
	public int ioWrite(TPointer inputPointer, int inputLength) {
		int writeLength = 0;
		int inputOffset = 0;
		while (inputLength > 0) {
			int pageOffset = getPageOffset();
			int pageLength = pageSize - pageOffset;
			int length = Math.min(pageLength, inputLength);

			if (length != pageSize) {
				// Not writing a complete page, read the current page
				int ppn = getPpn();
				readPage(ppn);
			}

			System.arraycopy(inputPointer.getArray8(inputOffset, length), 0, currentPage, pageOffset, length);

			int ppn = getPpn();
			writePage(ppn);

			inputLength -= length;
			inputOffset += length;
			position += length;
			writeLength += length;
		}

		return writeLength;
	}

	@Override
	public int ioWrite(byte[] inputBuffer, int inputOffset, int inputLength) {
		int writeLength = 0;
		while (inputLength > 0) {
			int pageOffset = getPageOffset();
			int pageLength = pageSize - pageOffset;
			int length = Math.min(pageLength, inputLength);

			if (length != pageSize) {
				// Not writing a complete page, read the current page
				int ppn = getPpn();
				readPage(ppn);
			}

			System.arraycopy(inputBuffer, inputOffset, currentPage, pageOffset, length);

			int ppn = getPpn();
			writePage(ppn);

			inputLength -= length;
			inputOffset += length;
			position += length;
			writeLength += length;
		}

		return writeLength;
	}

	@Override
	public long ioLseek(long offset) {
		position = offset;

		return position;
	}

	@Override
	public int ioIoctl(int command, TPointer inputPointer, int inputLength, TPointer outputPointer, int outputLength) {
		return IO_ERROR;
	}

	@Override
	public long length() {
		return (endLbn - startLbn) * pagesPerBlock * pageSize;
	}

	@Override
	public boolean isSectorBlockMode() {
		return false;
	}

	@Override
	public long getPosition() {
		return position;
	}

	@Override
	public IVirtualFile duplicate() {
		return null;
	}

	@Override
	public Map<IoOperation, IoOperationTiming> getTimings() {
		return null;
	}
}
