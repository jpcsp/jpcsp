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
package jpcsp.HLE.VFS.iso;

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_ERRNO_INVALID_ARGUMENT;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_INVALID_ARGUMENT;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_FILE_READ_ERROR;
import static jpcsp.HLE.modules150.IoFileMgrForUser.PSP_SEEK_CUR;
import static jpcsp.HLE.modules150.IoFileMgrForUser.PSP_SEEK_END;
import static jpcsp.HLE.modules150.IoFileMgrForUser.PSP_SEEK_SET;

import java.io.IOException;
import java.nio.ByteBuffer;

import jpcsp.HLE.TPointer;
import jpcsp.HLE.VFS.AbstractVirtualFile;
import jpcsp.filesystems.umdiso.UmdIsoFile;
import jpcsp.filesystems.umdiso.UmdIsoReader;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryWriter;
import jpcsp.util.Utilities;

public class UmdIsoVirtualFile extends AbstractVirtualFile {
	protected final UmdIsoFile file;
	protected final boolean sectorBlockMode;
	protected final UmdIsoReader iso;

	public UmdIsoVirtualFile(UmdIsoFile file, boolean sectorBlockMode, UmdIsoReader iso) {
		super(file);
		this.file = file;
		this.sectorBlockMode = sectorBlockMode;
		this.iso = iso;
	}

	@Override
	public boolean isSectorBlockMode() {
		return sectorBlockMode;
	}

	@Override
	public int ioIoctl(int command, TPointer inputPointer, int inputLength, TPointer outputPointer, int outputLength) {
		int result;

		switch (command) {
	        // UMD file seek set.
	        case 0x01010005: {
	            if (inputPointer.isAddressGood() && inputLength >= 4) {
                    int offset = inputPointer.getValue32();
                    if (log.isDebugEnabled()) {
                    	log.debug(String.format("ioIoctl umd file seek set %d", offset));
                    }
                    setPosition(offset);
                    result = 0;
	            } else {
	                result = ERROR_INVALID_ARGUMENT;
	            }
	            break;
	        }
	        // Get UMD Primary Volume Descriptor
	        case 0x01020001: {
	            if (outputPointer.isAddressGood() && outputLength == UmdIsoFile.sectorLength) {
					try {
						byte[] primaryVolumeSector = iso.readSector(UmdIsoReader.startSector);
	                	IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(outputPointer.getAddress(), outputLength, 1);
	                	for (int i = 0; i < outputLength; i++)  {
	                		memoryWriter.writeNext(primaryVolumeSector[i] & 0xFF);
	                	}
	                	memoryWriter.flush();
	                    result = 0;
					} catch (IOException e) {
						log.error("ioIoctl", e);
						result = ERROR_KERNEL_FILE_READ_ERROR;
					}
	            } else {
	                result = ERROR_ERRNO_INVALID_ARGUMENT;
	            }
	            break;
	        }
	        // Get UMD Path Table
	        case 0x01020002: {
	            if (outputPointer.isAddressGood() && outputLength <= UmdIsoFile.sectorLength) {
	            	try {
	                	byte[] primaryVolumeSector = iso.readSector(UmdIsoReader.startSector);
	                	ByteBuffer primaryVolume = ByteBuffer.wrap(primaryVolumeSector);
	                	primaryVolume.position(140);
	                	int pathTableLocation = Utilities.readWord(primaryVolume);
	                	byte[] pathTableSector = iso.readSector(pathTableLocation);
	                	IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(outputPointer.getAddress(), outputLength, 1);
	                	for (int i = 0; i < outputLength; i++)  {
	                		memoryWriter.writeNext(pathTableSector[i] & 0xFF);
	                	}
	                	memoryWriter.flush();
	                    result = 0;
					} catch (IOException e) {
						log.error("ioIoctl", e);
						result = ERROR_KERNEL_FILE_READ_ERROR;
					}
	            } else {
	                result = ERROR_ERRNO_INVALID_ARGUMENT;
	            }
	            break;
	        }
	        // Get Sector size
	        case 0x01020003: {
	            if (outputPointer.isAddressGood() && outputLength == 4) {
                	outputPointer.setValue32(UmdIsoFile.sectorLength);
                	result = 0;
	            } else {
	                result = ERROR_ERRNO_INVALID_ARGUMENT;
	            }
	            break;
	        }
	        // Get UMD file pointer.
	        case 0x01020004: {
	            if (outputPointer.isAddressGood() && outputLength >= 4) {
	            	try {
	                    int fPointer = (int) file.getFilePointer();
	                    outputPointer.setValue32(fPointer);
	                    if (log.isDebugEnabled()) {
	                    	log.debug(String.format("ioIoctl umd file get file pointer %d", fPointer));
	                    }
	                    result = 0;
					} catch (IOException e) {
						log.error("ioIoctl", e);
						result = ERROR_KERNEL_FILE_READ_ERROR;
					}
	            } else {
	                result = ERROR_INVALID_ARGUMENT;
	            }
	            break;
	        }
	        // Get UMD file start sector.
	        case 0x01020006: {
	            if (outputPointer.isAddressGood() && outputLength >= 4) {
	                int startSector = 0;
                    startSector = file.getStartSector();
                    if (log.isDebugEnabled()) {
                    	log.debug(String.format("ioIoctl umd file get start sector %d", startSector));
                    }
                    outputPointer.setValue32(startSector);
                    result = 0;
	            } else {
	                result = ERROR_INVALID_ARGUMENT;
	            }
	            break;
	        }
	        // Get UMD file length in bytes.
	        case 0x01020007: {
	            if (outputPointer.isAddressGood() && outputLength >= 8) {
	            	long length = length();
                    outputPointer.setValue64(length);
                    if (log.isDebugEnabled()) {
                    	log.debug(String.format("ioIoctl get file size %d", length));
                    }
                    result = 0;
	            } else {
	                result = ERROR_INVALID_ARGUMENT;
	            }
	            break;
	        }
	        // Read UMD file.
	        case 0x01030008: {
	            if (inputPointer.isAddressGood() && inputLength >= 4) {
	            	int length = inputPointer.getValue32();
	            	if (length > 0) {
	            		if (outputPointer.isAddressGood() && outputLength >= length) {
	            			try {
								Utilities.readFully(file, outputPointer.getAddress(), length);
								setPosition(getPosition() + length);
	                            result = length;
	    					} catch (IOException e) {
	    						log.error("ioIoctl", e);
	    						result = ERROR_KERNEL_FILE_READ_ERROR;
	    					}
	            		} else {
	                        result = ERROR_INVALID_ARGUMENT;
	            		}
	            	} else {
	                    result = ERROR_INVALID_ARGUMENT;
	            	}
	            } else {
	                result = ERROR_INVALID_ARGUMENT;
	            }
	            break;
	        }
	        // UMD disc read sectors operation.
	        case 0x01F30003: {
	            if (inputPointer.isAddressGood() && inputLength >= 4) {
	            	int numberOfSectors = inputPointer.getValue32();
	            	if (numberOfSectors > 0) {
	            		if (outputPointer.isAddressGood() && outputLength >= numberOfSectors) {
	            			try {
	                        	int length = numberOfSectors * UmdIsoFile.sectorLength;
								Utilities.readFully(file, outputPointer.getAddress(), length);
								setPosition(getPosition() + length);
	                            result = length / UmdIsoFile.sectorLength;
	    					} catch (IOException e) {
	    						log.error("ioIoctl", e);
	    						result = ERROR_KERNEL_FILE_READ_ERROR;
	    					}
	            		} else {
	                        result = ERROR_ERRNO_INVALID_ARGUMENT;
	            		}
	            	} else {
	                    result = ERROR_ERRNO_INVALID_ARGUMENT;
	            	}
	            } else {
	                result = ERROR_ERRNO_INVALID_ARGUMENT;
	            }
	            break;
	        }
	        // UMD file seek whence.
	        case 0x01F100A6: {
	            if (inputPointer.isAddressGood() && inputLength >= 16) {
                    long offset = inputPointer.getValue64(0);
                    int whence = inputPointer.getValue32(12);
                	if (isSectorBlockMode()) {
                		offset *= UmdIsoFile.sectorLength;
                	}
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("ioIoctl UMD file seek offset %d, whence %d", offset, whence));
                    }
                    switch (whence) {
                        case PSP_SEEK_SET:
                        	setPosition(offset);
                            result = 0;
                            break;
                        case PSP_SEEK_CUR:
                        	setPosition(getPosition() + offset);
                            result = 0;
                            break;
                        case PSP_SEEK_END:
                        	setPosition(length() + offset);
                            result = 0;
                            break;
                        default:
                            log.error(String.format("ioIoctl - unhandled whence %d", whence));
                            result = ERROR_INVALID_ARGUMENT;
                            break;
                    }
	            } else {
	                result = ERROR_INVALID_ARGUMENT;
	            }
	            break;
	        }
			default:
				result = super.ioIoctl(command, inputPointer, inputLength, outputPointer, outputLength);
		}

		return result;
	}
}
