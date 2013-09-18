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
package jpcsp.HLE.VFS.emulator;

import static jpcsp.HLE.modules150.sceDisplay.getPixelFormatBytes;

import java.io.IOException;
import java.nio.Buffer;

import jpcsp.Memory;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.VFS.AbstractVirtualFileSystem;
import jpcsp.HLE.modules150.sceDisplay.BufferInfo;
import jpcsp.autotests.AutoTestsOutput;
import jpcsp.graphics.capture.CaptureImage;
import jpcsp.hardware.Screen;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;

public class EmulatorVirtualFileSystem extends AbstractVirtualFileSystem {
	public static final int EMULATOR_DEVCTL_GET_HAS_DISPLAY = 0x01;
	public static final int EMULATOR_DEVCTL_SEND_OUTPUT = 0x02;
	public static final int EMULATOR_DEVCTL_IS_EMULATOR = 0x03;
	public static final int EMULATOR_DEVCTL_SEND_CTRLDATA = 0x10;
	public static final int EMULATOR_DEVCTL_EMIT_SCREENSHOT = 0x20;
	private static String screenshotFileName = "testResult.bmp";
	private static String screenshotFormat = "bmp";

	@Override
	public int ioDevctl(String deviceName, int command, TPointer inputPointer, int inputLength, TPointer outputPointer, int outputLength) {
    	switch (command) {
			case EMULATOR_DEVCTL_GET_HAS_DISPLAY:
				if (!outputPointer.isAddressGood() || outputLength < 4) {
					return super.ioDevctl(deviceName, command, inputPointer, inputLength, outputPointer, outputLength);
				}
				outputPointer.setValue32(Screen.hasScreen());
				break;
			case EMULATOR_DEVCTL_SEND_OUTPUT:
				byte[] input = new byte[inputLength];
				IMemoryReader memoryReader = MemoryReader.getMemoryReader(inputPointer.getAddress(), inputLength, 1);
				for (int i = 0; i < inputLength; i++) {
					input[i] = (byte) memoryReader.readNext();
				}
				String outputString = new String(input);
				if (log.isDebugEnabled()) {
					log.debug(outputString);
				}
				AutoTestsOutput.appendString(outputString);
				break;
			case EMULATOR_DEVCTL_IS_EMULATOR:
				break;
			case EMULATOR_DEVCTL_EMIT_SCREENSHOT:
				BufferInfo fb = Modules.sceDisplayModule.getBufferInfoFb();
	            Buffer buffer = Memory.getInstance().getBuffer(fb.topAddr, fb.bufferWidth * fb.height * getPixelFormatBytes(fb.pixelFormat));
	            CaptureImage captureImage = new CaptureImage(fb.topAddr, 0, buffer, fb.width, fb.height, fb.bufferWidth,fb.pixelFormat, false, 0, false, true, null);
	            captureImage.setFileName(getScreenshotFileName());
	            captureImage.setFileFormat(getScreenshotFormat());
	            try {
	            	captureImage.write();
	            	if (log.isDebugEnabled()) {
	            		log.debug(String.format("Screenshot 0x%08X-0x%08X saved under '%s'", fb.topAddr, fb.bottomAddr, captureImage.getFileName()));
	            	}
	            } catch (IOException e) {
	            	log.error("Emit Screenshot", e);
	            }
				break;
			default:
				// Unknown command
				return super.ioDevctl(deviceName, command, inputPointer, inputLength, outputPointer, outputLength);
		}

    	return 0;
	}

	public static String getScreenshotFileName() {
		return screenshotFileName;
	}

	public static void setScreenshotFileName(String screenshotFileName) {
		EmulatorVirtualFileSystem.screenshotFileName = screenshotFileName;
	}

	public static String getScreenshotFormat() {
		return screenshotFormat;
	}

	public static void setScreenshotFormat(String screenshotFormat) {
		EmulatorVirtualFileSystem.screenshotFormat = screenshotFormat;
	}
}
