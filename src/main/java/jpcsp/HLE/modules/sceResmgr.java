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

import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
import static jpcsp.util.Utilities.readCompleteFile;
import static jpcsp.util.Utilities.write8;
import static jpcsp.util.Utilities.writeCompleteFile;
import static jpcsp.util.Utilities.writeStringNZ;
import static jpcsp.util.Utilities.writeUnaligned32;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.crypto.CryptoEngine;
import jpcsp.crypto.PRX;
import jpcsp.format.PBP;
import jpcsp.format.PSAR;
import jpcsp.graphics.capture.CaptureImage;
import jpcsp.hardware.Model;
import jpcsp.settings.Settings;

public class sceResmgr extends HLEModule {
    public static Logger log = Modules.getLogger("sceResmgr");
	// Fake a version 2.00 so that the PSP Updates can be executed
	public static final String dummyIndexDatContent = "release:2.00:\n" +
			"build:0000,0,3,1,0:builder@vsh-build6\n" +
			"system:57716@release_660,0x06060010:\n" +
			"vsh:p6616@release_660,v58533@release_660,20110727:\n" +
			"target:1:WorldWide\n";
	private static final String system_plugin_bg_rco = "flash0:/vsh/resource/system_plugin_bg.rco";
	private static final String system_plugin_fg_rco = "flash0:/vsh/resource/system_plugin_fg.rco";

    @Override
	public void start() {
    	createDummyIndexDat();
    	createDummyBackgroundImages();
    	createRco();

    	super.start();
	}

    private static void createRco() {
    	//
    	// The PSP Update for firmware 1.50 and 1.51 are expecting to find the following 2 files:
    	//      flash0:/vsh/resource/system_plugin_bg.rco
    	//      flash0:/vsh/resource/system_plugin_fg.rco
    	// before starting the installation of the files on flash0.
    	//
    	// As those files are not yet available, simply extract those 2 files
    	// from the EBOOT.PBP of the updater itself (PSAR format).
    	//
    	if (Emulator.getInstance().isPspOfficialUpdater()) {
    		int updaterVersion = Emulator.getInstance().getPspOfficialUpdaterVersion();
    		if (updaterVersion == 150 || updaterVersion == 151) {
    			SceModule module = Emulator.getInstance().getModule();
    			byte[] buffer = readCompleteFile(module.pspfilename);
    			if (buffer != null) {
	    			try {
						PBP pbp = new PBP(ByteBuffer.wrap(buffer));
						if (pbp.isValid()) {
							PSAR psar = new PSAR(buffer, pbp.getOffsetPsarData(), pbp.getSizePsarData());
							if (psar.readHeader() == 0) {
								// Buffer large enough the store the longest of both files
								final byte[] content = new byte[50000];

								int result = psar.extractFile(content, 0, system_plugin_bg_rco);
								if (result >= 0) {
									writeCompleteFile(system_plugin_bg_rco, content, 0, result, true);
								}

								result = psar.extractFile(content, 0, system_plugin_fg_rco);
								if (result >= 0) {
									writeCompleteFile(system_plugin_fg_rco, content, 0, result, true);
								}
							}
						}
					} catch (IOException e) {
						if (log.isDebugEnabled()) {
							log.debug("createRco", e);
						}
					}
    			}
    		}
    	}
    }

    private static void createDummyIndexDat() {
    	int firmwareVersion = RuntimeContextLLE.getFirmwareVersion();
    	if (firmwareVersion <= 0) {
    		firmwareVersion = Emulator.getInstance().getFirmwareVersion();
    	}

    	String fileNameFormat;
    	if (firmwareVersion == 0 || firmwareVersion >= 500) {
    		// Starting with FW 5.00, the filename includes the PSP generation
    		fileNameFormat = "flash0:/vsh/etc/index_%02dg.dat";
    	} else {
    		// Before FW 5.00, the filename does not include the PSP generation
    		fileNameFormat = "flash0:/vsh/etc/index.dat";
    	}
    	String indexDatFileName = String.format(fileNameFormat, Model.getGeneration());
    	byte[] content = readCompleteFile(indexDatFileName);
    	if (content != null && content.length > 0) {
    		// File already exists
    		return;
    	}

    	if (firmwareVersion == 0 || firmwareVersion >= 250) {
	    	// A few entries in PreDecrypt.xml will allow the decryption of this dummy file
	    	byte[] buffer = new byte[0x1F0];
	    	write8(buffer, 0x7C, PRX.DECRYPT_MODE_NO_EXEC); // decryptMode
	    	writeUnaligned32(buffer, 0xB0, 0x9F); // dataSize
	    	writeUnaligned32(buffer, 0xB4, 0x80); // dataOffset
	    	int tag;
	    	switch (Model.getGeneration()) {
	    		case 1:  tag = 0x0B2B90F0; break;
	    		case 2:  tag = 0x0B2B91F0; break;
	    		default: tag = 0x0B2B92F0; break;
	    	}
	    	writeUnaligned32(buffer, 0xD0, tag); // tag

	    	writeStringNZ(buffer, 0x150, buffer.length - 0x150, dummyIndexDatContent);

	    	writeCompleteFile(indexDatFileName, buffer, true);
    	} else {
    		// Before FW 2.50, index.dat is not encrypted
    		byte[] buffer = dummyIndexDatContent.getBytes();
    		writeCompleteFile(indexDatFileName, buffer, true);
    	}
    }

    /**
     * Create 12 dummy background images under flash0:/vsh/resource
     *     01.bmp
     *     02.bmp
     *     03.bmp
     *     04.bmp
     *     05.bmp
     *     06.bmp
     *     07.bmp
     *     08.bmp
     *     09.bmp
     *     10.bmp
     *     11.bmp
     *     12.bmp
     * The dummy images try to match the colors from a real PSP.
     */
    private void createDummyBackgroundImages() {
    	String directory = String.format("%s/vsh/resource", Settings.getInstance().getDirectoryMapping("flash0"));
    	File baseDirectory = new File(directory);
    	if (baseDirectory.isDirectory()) {
    		String[] fileNames = baseDirectory.list();
    		if (fileNames != null && fileNames.length >= 12) {
    			return;
    		}
    	} else {
    		baseDirectory.mkdirs();
    	}

    	final int imageWidth = 60;
    	final int imageHeight = 34;
    	final int[] buffer = new int[imageWidth * imageHeight];
    	final int[] monthColors = new int[] { // Colors are in ABGR format
    			0xE3E3E3,
    			0x49E5F5,
    			0x19E094,
    			0xA186F2,
    			0x0A950A,
    			0xAC5E9D,
    			0x90AB00,
    			0x99360A,
    			0xDB5BCA,
    			0x19BCEE,
    			0x184B77,
    			0x171BEC
    	};

    	// The PSP has one image per month
    	for (int month = 1; month <= 12; month++) {
    		// Each month has a different color
    		int monthColor = monthColors[month - 1];

    		// Fill the whole image with a fixed color
    		Arrays.fill(buffer, monthColor);

    		// Save the NN.bmp file
    		CaptureImage image = new CaptureImage(0, 0, IntBuffer.wrap(buffer), imageWidth, imageHeight, imageWidth, TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888, false, 0, false, true, null);
	    	String fileName = String.format("%s/%02d.bmp", directory, month);
	    	image.setFileFormat("bmp");
	    	image.setFileName(fileName);
	    	try {
				image.write();
			} catch (IOException e) {
				log.error("write BMP", e);
			}
    	}
    }

    @HLEFunction(nid = 0x9DC14891, version = 150)
    public int sceResmgr_9DC14891(@BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.inout) TPointer buffer, int bufferSize, @BufferInfo(usage=Usage.out) TPointer32 resultLengthAddr) {
    	int resultLength;

    	// Nothing to do if the buffer is already decrypted
    	if ("release:".equals(buffer.getStringNZ(0, 8))) {
    		resultLength = bufferSize;
    	} else {
    		byte[] buf = buffer.getArray8(bufferSize);

	    	int result = new CryptoEngine().getPRXEngine().DecryptPRX(buf, bufferSize, 9, null, null);
	    	if (result < 0) {
	    		log.error(String.format("sceResmgr_9DC14891 returning error 0x%08X", result));
	    		return result;
	    	}

	    	resultLength = result;
	    	buffer.setArray(buf, resultLength);
    	}

    	resultLengthAddr.setValue(resultLength);

    	return 0;
    }
}
