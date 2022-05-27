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
package jpcsp.HLE.VFS.local;

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_PGD_INVALID_PARAMETER;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import jpcsp.HLE.TPointer;
import jpcsp.HLE.VFS.AbstractVirtualFile;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.VFS.SeekableDataInputVirtualFile;
import jpcsp.HLE.VFS.crypto.PGDVirtualFile;
import jpcsp.HLE.modules.IoFileMgrForUser;
import jpcsp.HLE.modules.IoFileMgrForUser.IoOperation;
import jpcsp.HLE.modules.IoFileMgrForUser.IoOperationTiming;
import jpcsp.filesystems.SeekableRandomFile;
import jpcsp.util.Utilities;

public class LocalVirtualFile extends AbstractVirtualFile {
	protected SeekableRandomFile file;
	protected boolean truncateAtNextWrite;
	protected IVirtualFile proxyFile;
	protected int proxyFileOffset;

	public LocalVirtualFile(SeekableRandomFile file) {
		super(file);
		this.file = file;
	}

	@Override
	public int ioWrite(TPointer inputPointer, int inputLength) {
		try {
			Utilities.write(file, inputPointer, inputLength);
		} catch (IOException e) {
			log.error("ioWrite", e);
			return IO_ERROR;
		}

		return inputLength;
	}

	@Override
	public int ioWrite(byte[] inputBuffer, int inputOffset, int inputLength) {
		try {
			if (isTruncateAtNextWrite()) {
            	// The file was open with PSP_O_TRUNC: truncate the file at the first write
				long position = getPosition();
				if (position < file.length()) {
					file.setLength(getPosition());
				}
				setTruncateAtNextWrite(false);
			}

			file.write(inputBuffer, inputOffset, inputLength);
		} catch (IOException e) {
			log.error("ioWrite", e);
			return IO_ERROR;
		}

		return inputLength;
	}

	@Override
	public int ioIoctl(int command, TPointer inputPointer, int inputLength, TPointer outputPointer, int outputLength) {
		int result;
		switch (command) {
			case 0x00005001:
	        	if (inputLength != 0 || outputLength != 0) {
	        		result = IO_ERROR;
	        	} else {
	        		result = 0;
	        	}
				break;
            // Check if LoadExec is allowed on the file
            case 0x00208013:
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("Checking if LoadExec is allowed on '%s'", this));
            	}
            	// Result == 0: LoadExec allowed
            	// Result != 0: LoadExec prohibited
            	result = 0;
            	break;
            // Check if LoadModule is allowed on the file
            case 0x00208003:
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("Checking if LoadModule is allowed on '%s'", this));
            	}
            	// Result == 0: LoadModule allowed
            	// Result != 0: LoadModule prohibited
            	result = 0;
            	break;
            // Check if PRX type is allowed on the file
            case 0x00208081:
            case 0x00208082:
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("Checking if PRX type is allowed on '%s'", this));
            	}
            	// Result == 0: PRX type allowed
            	// Result != 0: PRX type prohibited
            	result = 0;
            	break;
            // Called from sceNpDrmEdataSetupKey, setup key?
            case 0x04100001:
	        	if (inputLength != 16 || outputLength != 0) {
	        		result = ERROR_PGD_INVALID_PARAMETER;
	        	} else {
	        		byte[] key = inputPointer.getArray8(16);
	        		key = null;
	        		PGDVirtualFile pgdVirtualFile = new PGDVirtualFile(key, new SeekableDataInputVirtualFile(file), proxyFileOffset);
	        		if (pgdVirtualFile.isValid()) {
	        			proxyFile = pgdVirtualFile;
	        			result = 0;
	        		} else {
	        			result = ERROR_PGD_INVALID_PARAMETER;
	        		}
	        	}
            	break;
            // Called from sceNpDrmEdataSetupKey, set the PGD offset
            case 0x04100002:
	        	if (inputLength != 4 || outputLength != 0) {
	        		result = ERROR_PGD_INVALID_PARAMETER;
	        	} else {
	        		proxyFileOffset = inputPointer.getValue32(0);
	        		result = 0;
	        	}
            	break;
            // Called from sceNpDrmEdataSetupKey, read PSPEDAT header
            case 0x04100005:
            	if (inputLength != 8) {
	        		result = ERROR_PGD_INVALID_PARAMETER;
            	} else {
            		int seekPosition = inputPointer.getValue32(0);
            		int readLength = inputPointer.getValue32(4);
            		result = (int) ioLseek((long) seekPosition);
            		result = ioRead(outputPointer, readLength);
            	}
            	break;
        	// Called from sceNpDrmEdataSetupKey, has the file already a key setup?
            case 0x04100006:
        		result = 1;
            	break;
        	// Used by sceNpDrmEdataGetDataSize, returning the size of the DRM file
            case 0x04100010:
            	result = (int) length();
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("Returning the size of the DRM file '%s': 0x%X", this, result));
            	}
            	break;
			default:
				result = super.ioIoctl(command, inputPointer, inputLength, outputPointer, outputLength);
				break;
		}

		return result;
	}

	public boolean isTruncateAtNextWrite() {
		return truncateAtNextWrite;
	}

	public void setTruncateAtNextWrite(boolean truncateAtNextWrite) {
		this.truncateAtNextWrite = truncateAtNextWrite;
	}

	@Override
	public IVirtualFile duplicate() {
		try {
			return new LocalVirtualFile(new SeekableRandomFile(file.getFileName(), file.getMode()));
		} catch (FileNotFoundException e) {
		}

		return super.duplicate();
	}

	@Override
	public Map<IoOperation, IoOperationTiming> getTimings() {
		return IoFileMgrForUser.noDelayTimings;
	}

	@Override
	public String toString() {
		return String.format("LocalVirtualFile %s", file);
	}

	@Override
	public long getPosition() {
		if (proxyFile != null) {
			return proxyFile.getPosition();
		}
		return super.getPosition();
	}

	@Override
	public int ioClose() {
		if (proxyFile != null) {
			IVirtualFile vFile = proxyFile;
			proxyFile = null;
			proxyFileOffset = 0;
			return vFile.ioClose();
		}
		return super.ioClose();
	}

	@Override
	public int ioRead(TPointer outputPointer, int outputLength) {
		if (proxyFile != null) {
			return proxyFile.ioRead(outputPointer, outputLength);
		}
		return super.ioRead(outputPointer, outputLength);
	}

	@Override
	public int ioRead(byte[] outputBuffer, int outputOffset, int outputLength) {
		if (proxyFile != null) {
			return proxyFile.ioRead(outputBuffer, outputOffset, outputLength);
		}
		return super.ioRead(outputBuffer, outputOffset, outputLength);
	}

	@Override
	public long ioLseek(long offset) {
		if (proxyFile != null) {
			return proxyFile.ioLseek(offset);
		}
		return super.ioLseek(offset);
	}

	@Override
	public long length() {
		if (proxyFile != null) {
			return proxyFile.length();
		}
		return super.length();
	}

	@Override
	public boolean isSectorBlockMode() {
		if (proxyFile != null) {
			return proxyFile.isSectorBlockMode();
		}
		return super.isSectorBlockMode();
	}
}
