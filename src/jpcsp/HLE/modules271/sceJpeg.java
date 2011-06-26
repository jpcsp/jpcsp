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
package jpcsp.HLE.modules271;

import org.apache.log4j.Logger;

import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.sceDisplay;
import jpcsp.HLE.modules.sceMpeg;
import jpcsp.graphics.VideoEngine;
import jpcsp.hardware.Screen;

public class sceJpeg implements HLEModule {
    protected static Logger log = Modules.getLogger("sceJpeg");

	@Override
	public String getName() {
		return "sceJpeg";
	}

	@Override
	public void installModule(HLEModuleManager mm, int version) {
		if (version >= 271) {
			mm.addFunction(sceJpegMJpegCscFunction, 0x04B5AE02);
			mm.addFunction(sceJpegDeleteMJpegFunction, 0x48B602B7);
			mm.addFunction(sceJpegFinishMJpegFunction, 0x7D2F3D7F);
			mm.addFunction(sceJpegDecodeMJpegYCbCrFunction, 0x91EED83C);
			mm.addFunction(sceJpegCreateMJpegFunction, 0x9D47469C);
			mm.addFunction(sceJpegInitMJpegFunction, 0xAC9E70E6);
			mm.addFunction(sceJpegDecodeMJpegFunction, 0x04B93CEF);

			jpegWidth = Screen.width;
			jpegHeight = Screen.height;
		}
	}

	@Override
	public void uninstallModule(HLEModuleManager mm, int version) {
		if (version >= 271) {
			mm.removeFunction(sceJpegMJpegCscFunction);
			mm.removeFunction(sceJpegDeleteMJpegFunction);
			mm.removeFunction(sceJpegFinishMJpegFunction);
			mm.removeFunction(sceJpegDecodeMJpegYCbCrFunction);
			mm.removeFunction(sceJpegCreateMJpegFunction);
			mm.removeFunction(sceJpegInitMJpegFunction);
			mm.removeFunction(sceJpegDecodeMJpegFunction);
		}
	}

	protected int jpegWidth;
	protected int jpegHeight;

	public void sceJpegMJpegCsc(Processor processor) {
		CpuState cpu = processor.cpu;

		int imageBuffer = cpu.gpr[4];
		int yCbCrBuffer = cpu.gpr[5];
		int widthHeight = cpu.gpr[6];
		int width = widthHeight & 0xFFF;
		int height = (widthHeight >> 16) & 0xFFF;
		int bufferWidth = cpu.gpr[7];
		int pixelFormat = sceDisplay.PSP_DISPLAY_PIXEL_FORMAT_8888;

		log.warn(String.format("Unimplemented sceJpegMJpegCsc imageBuffer=0x%08X, yCbCrBuffer=0x%08X, widthHeight=0x%08X(width=%d, height=%d), bufferWidth=%d", imageBuffer, yCbCrBuffer, widthHeight, width, height, bufferWidth));

		// Return a fake image
		sceMpeg.generateFakeImage(imageBuffer, bufferWidth, width, height, pixelFormat);
        VideoEngine.getInstance().addVideoTexture(imageBuffer, imageBuffer + bufferWidth * height * sceDisplay.getPixelFormatBytes(pixelFormat));

		cpu.gpr[2] = 0;
	}

	/**
	 * Deletes the current decoder context.
	 *
	 * @return 0 on success, < 0 on error
	 */
	public void sceJpegDeleteMJpeg(Processor processor) {
		CpuState cpu = processor.cpu;

		// No parameters

		log.warn(String.format("Ignoring sceJpegDeleteMJpeg"));

		cpu.gpr[2] = 0;
	}

	/**
	 * Finishes the MJpeg library
	 *
	 * @return 0 on success, < 0 on error
	 */
	public void sceJpegFinishMJpeg(Processor processor) {
		CpuState cpu = processor.cpu;

		// No parameters

		log.warn(String.format("Ignoring sceJpegFinishMJpeg"));

		cpu.gpr[2] = 0;
	}

	public void sceJpegDecodeMJpegYCbCr(Processor processor) {
		CpuState cpu = processor.cpu;

		int jpegBuffer = cpu.gpr[4];
		int jpegBufferSize = cpu.gpr[5];
		int yCbCrBuffer = cpu.gpr[6];
		int yCbCrBufferSize = cpu.gpr[7];
		int unknown = cpu.gpr[8]; // 0

		log.warn(String.format("Unimplemented sceJpegDecodeMJpegYCbCr jpegBuffer=0x%08X, jpegBufferSize=%d, yCbCrBuffer=0x%08X, yCbCrBufferSize=%d, unknown=%d", jpegBuffer, jpegBufferSize, yCbCrBuffer, yCbCrBufferSize, unknown));

		int width = jpegWidth;
		int height = jpegHeight;

		// Return size of image
		cpu.gpr[2] = (height << 16) | width;
	}

	/**
	 * Creates the decoder context.
	 *
	 * @param width - The width of the frame
	 * @param height - The height of the frame
	 *
	 * @return 0 on success, < 0 on error
	 */
	public void sceJpegCreateMJpeg(Processor processor) {
		CpuState cpu = processor.cpu;

		int width = cpu.gpr[4];
		int height = cpu.gpr[5];

		log.warn(String.format("Unimplemented sceJpegCreateMJpeg width=%d, height=%d", width, height));

		jpegWidth = width;
		jpegHeight = height;

		cpu.gpr[2] = 0;
	}

	/**
	 * Inits the MJpeg library
	 *
	 * @return 0 on success, < 0 on error
	 */
	public void sceJpegInitMJpeg(Processor processor) {
		CpuState cpu = processor.cpu;

		// No parameters

		log.warn(String.format("Ignoring sceJpegInitMJpeg"));

		cpu.gpr[2] = 0;
	}

	/**
	 * Decodes a mjpeg frame.
	 *
	 * @param jpegbuf - the buffer with the mjpeg frame
	 * @param size - size of the buffer pointed by jpegbuf
	 * @param rgba - buffer where the decoded data in RGBA format will be stored.
	 *                                     It should have a size of (width * height * 4).
	 * @param unk - Unknown, pass 0
	 *
	 * @return (width * 65536) + height on success, < 0 on error
	 */
	public void sceJpegDecodeMJpeg(Processor processor) {
		CpuState cpu = processor.cpu;

		int jpegBuffer = cpu.gpr[4];
		int jpegBufferSize = cpu.gpr[5];
		int imageBuffer = cpu.gpr[6];
		int unknown = cpu.gpr[7];

		log.warn(String.format("Unimplemented sceJpegDecodeMJpeg jpegBuffer=0x%08X, jpegBufferSize=%d, imageBuffer=0x%08X, unknown=%d", jpegBuffer, jpegBufferSize, imageBuffer, unknown));

		int width = jpegWidth;
		int height = jpegHeight;
		int bufferWidth = width;
		int pixelFormat = sceDisplay.PSP_DISPLAY_PIXEL_FORMAT_8888;

		// Return a fake image
		sceMpeg.generateFakeImage(imageBuffer, bufferWidth, width, height, pixelFormat);
        VideoEngine.getInstance().addVideoTexture(imageBuffer, imageBuffer + bufferWidth * height * sceDisplay.getPixelFormatBytes(pixelFormat));

		// Return size of image
		cpu.gpr[2] = (height << 16) | width;
	}


	public final HLEModuleFunction sceJpegMJpegCscFunction = new HLEModuleFunction("sceJpeg", "sceJpegMJpegCsc") {
		@Override
		public final void execute(Processor processor) {
			sceJpegMJpegCsc(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceJpegModule.sceJpegMJpegCsc(processor);";
		}
	};

	public final HLEModuleFunction sceJpegDeleteMJpegFunction = new HLEModuleFunction("sceJpeg", "sceJpegDeleteMJpeg") {
		@Override
		public final void execute(Processor processor) {
			sceJpegDeleteMJpeg(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceJpegModule.sceJpegDeleteMJpeg(processor);";
		}
	};

	public final HLEModuleFunction sceJpegFinishMJpegFunction = new HLEModuleFunction("sceJpeg", "sceJpegFinishMJpeg") {
		@Override
		public final void execute(Processor processor) {
			sceJpegFinishMJpeg(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceJpegModule.sceJpegFinishMJpeg(processor);";
		}
	};

	public final HLEModuleFunction sceJpegDecodeMJpegYCbCrFunction = new HLEModuleFunction("sceJpeg", "sceJpegDecodeMJpegYCbCr") {
		@Override
		public final void execute(Processor processor) {
			sceJpegDecodeMJpegYCbCr(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceJpegModule.sceJpegDecodeMJpegYCbCr(processor);";
		}
	};

	public final HLEModuleFunction sceJpegCreateMJpegFunction = new HLEModuleFunction("sceJpeg", "sceJpegCreateMJpeg") {
		@Override
		public final void execute(Processor processor) {
			sceJpegCreateMJpeg(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceJpegModule.sceJpegCreateMJpeg(processor);";
		}
	};

	public final HLEModuleFunction sceJpegInitMJpegFunction = new HLEModuleFunction("sceJpeg", "sceJpegInitMJpeg") {
		@Override
		public final void execute(Processor processor) {
			sceJpegInitMJpeg(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceJpegModule.sceJpegInitMJpeg(processor);";
		}
	};

	public final HLEModuleFunction sceJpegDecodeMJpegFunction = new HLEModuleFunction("sceJpeg", "sceJpegDecodeMJpeg") {
		@Override
		public final void execute(Processor processor) {
			sceJpegDecodeMJpeg(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceJpegModule.sceJpegDecodeMJpeg(processor);";
		}
	};
}
