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

import jpcsp.HLE.HLEFunction;
import org.apache.log4j.Logger;

import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.pspUsbCamSetupStillExParam;
import jpcsp.HLE.kernel.types.pspUsbCamSetupStillParam;
import jpcsp.HLE.kernel.types.pspUsbCamSetupVideoExParam;
import jpcsp.HLE.kernel.types.pspUsbCamSetupVideoParam;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

public class sceUsbCam implements HLEModule {
    protected static Logger log = Modules.getLogger("sceUsbCam");

	@Override
	public String getName() {
		return "sceUsbCam";
	}

	@Override
	public void installModule(HLEModuleManager mm, int version) { mm.installModuleWithAnnotations(this, version); }

	@Override
	public void uninstallModule(HLEModuleManager mm, int version) { mm.uninstallModuleWithAnnotations(this, version); }

	public static final int PSP_USBCAM_PID = 0x282;
	public static final String PSP_USBCAM_DRIVERNAME = "USBCamDriver";
	public static final String PSP_USBCAMMIC_DRIVERNAME = "USBCamMicDriver";

	/** Resolutions for sceUsbCamSetupStill & sceUsbCamSetupVideo
     ** DO NOT use on sceUsbCamSetupStillEx & sceUsbCamSetupVideoEx */
	public static final int PSP_USBCAM_RESOLUTION_160_120  = 0;
	public static final int PSP_USBCAM_RESOLUTION_176_144  = 1;
	public static final int PSP_USBCAM_RESOLUTION_320_240  = 2;
	public static final int PSP_USBCAM_RESOLUTION_352_288  = 3;
	public static final int PSP_USBCAM_RESOLUTION_640_480  = 4;
	public static final int PSP_USBCAM_RESOLUTION_1024_768 = 5;
	public static final int PSP_USBCAM_RESOLUTION_1280_960 = 6;
	public static final int PSP_USBCAM_RESOLUTION_480_272  = 7;
	public static final int PSP_USBCAM_RESOLUTION_360_272  = 8;

	/** Resolutions for sceUsbCamSetupStillEx & sceUsbCamSetupVideoEx
     ** DO NOT use on sceUsbCamSetupStill & sceUsbCamSetupVideo */
	public static final int PSP_USBCAM_RESOLUTION_EX_160_120  = 0;
	public static final int PSP_USBCAM_RESOLUTION_EX_176_144  = 1;
	public static final int PSP_USBCAM_RESOLUTION_EX_320_240  = 2;
	public static final int PSP_USBCAM_RESOLUTION_EX_352_288  = 3;
	public static final int PSP_USBCAM_RESOLUTION_EX_360_272  = 4;
	public static final int PSP_USBCAM_RESOLUTION_EX_480_272  = 5;
	public static final int PSP_USBCAM_RESOLUTION_EX_640_480  = 6;
	public static final int PSP_USBCAM_RESOLUTION_EX_1024_768 = 7;
	public static final int PSP_USBCAM_RESOLUTION_EX_1280_960 = 8;

	/** Flags for reverse effects. */
	public static final int PSP_USBCAM_FLIP = 1;
	public static final int PSP_USBCAM_MIRROR = 0x100;

	/** Delay to take pictures */
	public static final int PSP_USBCAM_NODELAY = 0;
	public static final int PSP_USBCAM_DELAY_10SEC = 1;
	public static final int PSP_USBCAM_DELAY_20SEC = 2;
	public static final int PSP_USBCAM_DELAY_30SEC = 3;

	/** Usbcam framerates */
	public static final int PSP_USBCAM_FRAMERATE_3_75_FPS = 0; /* 3.75 fps */
	public static final int PSP_USBCAM_FRAMERATE_5_FPS = 1;
	public static final int PSP_USBCAM_FRAMERATE_7_5_FPS = 2; /* 7.5 fps */
	public static final int PSP_USBCAM_FRAMERATE_10_FPS = 3;
	public static final int PSP_USBCAM_FRAMERATE_15_FPS = 4;
	public static final int PSP_USBCAM_FRAMERATE_20_FPS = 5;
	public static final int PSP_USBCAM_FRAMERATE_30_FPS = 6;
	public static final int PSP_USBCAM_FRAMERATE_60_FPS = 7;

	/** White balance values */
	public static final int PSP_USBCAM_WB_AUTO = 0;
	public static final int PSP_USBCAM_WB_DAYLIGHT = 1;
	public static final int PSP_USBCAM_WB_FLUORESCENT = 2;
	public static final int PSP_USBCAM_WB_INCADESCENT = 3;

	/** Effect modes */
	public static final int PSP_USBCAM_EFFECTMODE_NORMAL = 0;
	public static final int PSP_USBCAM_EFFECTMODE_NEGATIVE = 1;
	public static final int PSP_USBCAM_EFFECTMODE_BLACKWHITE = 2;
	public static final int PSP_USBCAM_EFFECTMODE_SEPIA = 3;
	public static final int PSP_USBCAM_EFFECTMODE_BLUE = 4;
	public static final int PSP_USBCAM_EFFECTMODE_RED = 5;
	public static final int PSP_USBCAM_EFFECTMODE_GREEN = 6;

	/** Exposure levels */
	public static final int PSP_USBCAM_EVLEVEL_2_0_POSITIVE = 0; // +2.0
	public static final int PSP_USBCAM_EVLEVEL_1_7_POSITIVE = 1; // +1.7
	public static final int PSP_USBCAM_EVLEVEL_1_5_POSITIVE = 2; // +1.5
	public static final int PSP_USBCAM_EVLEVEL_1_3_POSITIVE = 3; // +1.3
	public static final int PSP_USBCAM_EVLEVEL_1_0_POSITIVE = 4; // +1.0
	public static final int PSP_USBCAM_EVLEVEL_0_7_POSITIVE = 5; // +0.7
	public static final int PSP_USBCAM_EVLEVEL_0_5_POSITIVE = 6; // +0.5
	public static final int PSP_USBCAM_EVLEVEL_0_3_POSITIVE = 7; // +0.3
	public static final int PSP_USBCAM_EVLEVEL_0_0 = 8; // 0.0
	public static final int PSP_USBCAM_EVLEVEL_0_3_NEGATIVE = 9; // -0.3
	public static final int PSP_USBCAM_EVLEVEL_0_5_NEGATIVE = 10; // -0.5
	public static final int PSP_USBCAM_EVLEVEL_0_7_NEGATIVE = 11; // -0.7
	public static final int PSP_USBCAM_EVLEVEL_1_0_NEGATIVE = 12; // -1.0
	public static final int PSP_USBCAM_EVLEVEL_1_3_NEGATIVE = 13; // -1.3
	public static final int PSP_USBCAM_EVLEVEL_1_5_NEGATIVE = 14; // -1.5
	public static final int PSP_USBCAM_EVLEVEL_1_7_NEGATIVE = 15; // -1.7
	public static final int PSP_USBCAM_EVLEVEL_2_0_NEGATIVE = 16; // -2.0

	protected int workArea;
	protected int workAreaSize;
	protected int jpegBuffer;
	protected int jpegBufferSize;

	// Camera settings
	protected int resolution; // One of PSP_USBCAM_RESOLUTION_* (not PSP_USBCAM_RESOLUTION_EX_*)
	protected int frameRate;
	protected int whiteBalance;
	protected int frameSize;
	protected int saturation;
	protected int brightness;
	protected int contrast;
	protected int sharpness;
	protected int imageEffectMode;
	protected int evLevel;
	protected boolean flip;
	protected boolean mirror;
	protected int zoom;
	protected boolean autoImageReverseSW;
	protected boolean lensDirectionAtYou;

	/**
	 * Convert a value PSP_USBCAM_RESOLUTION_EX_*
	 * to the corresponding PSP_USBCAM_RESOLUTION_*
	 * 
	 * @param resolutionEx One of PSP_USBCAM_RESOLUTION_EX_*
	 * @return             The corresponding value PSP_USBCAM_RESOLUTION_*
	 */
	protected int convertResolutionExToResolution(int resolutionEx) {
		switch(resolutionEx) {
			case PSP_USBCAM_RESOLUTION_EX_160_120: return PSP_USBCAM_RESOLUTION_160_120;
			case PSP_USBCAM_RESOLUTION_EX_176_144: return PSP_USBCAM_RESOLUTION_176_144;
			case PSP_USBCAM_RESOLUTION_EX_320_240: return PSP_USBCAM_RESOLUTION_320_240;
			case PSP_USBCAM_RESOLUTION_EX_352_288: return PSP_USBCAM_RESOLUTION_352_288;
			case PSP_USBCAM_RESOLUTION_EX_360_272: return PSP_USBCAM_RESOLUTION_360_272;
			case PSP_USBCAM_RESOLUTION_EX_480_272: return PSP_USBCAM_RESOLUTION_480_272;
			case PSP_USBCAM_RESOLUTION_EX_640_480: return PSP_USBCAM_RESOLUTION_640_480;
			case PSP_USBCAM_RESOLUTION_EX_1024_768: return PSP_USBCAM_RESOLUTION_1024_768;
			case PSP_USBCAM_RESOLUTION_EX_1280_960: return PSP_USBCAM_RESOLUTION_1280_960;
		}

		return resolutionEx;
	}

	protected int readFakeVideoFrame() {
		Memory mem = Processor.memory;

		// Image has to be stored in Jpeg format in buffer
		mem.memset(jpegBuffer, (byte) 0x00, jpegBufferSize);

		return jpegBufferSize;
	}

	/**
	 * Set ups the parameters for video capture.
	 *
	 * @param param - Pointer to a pspUsbCamSetupVideoParam structure.
	 * @param workarea - Pointer to a buffer used as work area by the driver.
	 * @param wasize - Size of the work area.
	 *
	 * @return 0 on success, < 0 on error
	 */
	public void sceUsbCamSetupVideo(Processor processor) {
		CpuState cpu = processor.cpu;
		Memory mem = Processor.memory;

		int param = cpu.gpr[4];
		int workArea = cpu.gpr[5];
		int workAreaSize = cpu.gpr[6];

		pspUsbCamSetupVideoParam usbCamSetupVideoParam = new pspUsbCamSetupVideoParam();
		usbCamSetupVideoParam.read(mem, param);

		log.warn(String.format("Partial sceUsbCamSetupVideo param=0x%08X, workArea=0x%08X, workAreaSize=%d, param=%s", param, workArea, workAreaSize, usbCamSetupVideoParam.toString()));

		this.workArea = workArea;
		this.workAreaSize = workAreaSize;
		resolution = usbCamSetupVideoParam.resolution;
		frameRate = usbCamSetupVideoParam.framerate;
		whiteBalance = usbCamSetupVideoParam.wb;
		saturation = usbCamSetupVideoParam.saturation;
		brightness = usbCamSetupVideoParam.brightness;
		contrast = usbCamSetupVideoParam.contrast;
		sharpness = usbCamSetupVideoParam.sharpness;
		imageEffectMode = usbCamSetupVideoParam.effectmode;
		frameSize = usbCamSetupVideoParam.framesize;
		evLevel = usbCamSetupVideoParam.evlevel;

		cpu.gpr[2] = 0;
	}

	/**
	 * Sets if the image should be automatically reversed, depending of the position
	 * of the camera.
	 *
	 * @param on - 1 to set the automatical reversal of the image, 0 to set it off
	 *
	 * @return 0 on success, < 0 on error
	 */
	public void sceUsbCamAutoImageReverseSW(Processor processor) {
		CpuState cpu = processor.cpu;

		int on = cpu.gpr[4];

		autoImageReverseSW = (on != 1);

		log.warn(String.format("Partial sceUsbCamAutoImageReverseSW on=%d", on));

		cpu.gpr[2] = 0;
	}

	/**
	 * Starts video input from the camera.
	 *
	 * @return 0 on success, < 0 on error
	 */
	public void sceUsbCamStartVideo(Processor processor) {
		CpuState cpu = processor.cpu;

		// No parameters

		log.warn(String.format("Ignoring sceUsbCamStartVideo"));

		cpu.gpr[2] = 0;
	}

	/**
	 * Stops video input from the camera.
	 *
	 * @return 0 on success, < 0 on error
	*/
	public void sceUsbCamStopVideo(Processor processor) {
		CpuState cpu = processor.cpu;

		// No parameters

		log.warn(String.format("Ignoring sceUsbCamStopVideo"));

		cpu.gpr[2] = 0;
	}

	public void sceUsbCamSetupMic(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn(String.format("Unimplemented sceUsbCamSetupMic"));

		cpu.gpr[2] = 0;
	}

	public void sceUsbCamStartMic(Processor processor) {
		CpuState cpu = processor.cpu;

		// No parameters

		log.warn(String.format("Ignoring sceUsbCamStartMic"));

		cpu.gpr[2] = 0;
	}

	/**
	 * Reads a video frame. The function doesn't return until the frame
	 * has been acquired.
	 *
	 * @param buf - The buffer that receives the frame jpeg data
	 * @param size - The size of the buffer.
	 *
	 * @return size of acquired frame on success, < 0 on error
	 */
	public void sceUsbCamReadVideoFrameBlocking(Processor processor) {
		CpuState cpu = processor.cpu;

		int jpegBuffer = cpu.gpr[4];
		int jpegBufferSize = cpu.gpr[5];

		log.warn(String.format("Unimplemented sceUsbCamReadVideoFrameBlocking jpegBuffer=0x%08X, jpegBufferSize=%d", jpegBuffer, jpegBufferSize));

		this.jpegBuffer = jpegBuffer;
		this.jpegBufferSize = jpegBufferSize;

		cpu.gpr[2] = readFakeVideoFrame();
	}

	/**
	 * Reads a video frame. The function returns inmediately, and
	 * the completion has to be handled by calling sceUsbCamWaitReadVideoFrameEnd
	 * or sceUsbCamPollReadVideoFrameEnd.
	 *
	 * @param buf - The buffer that receives the frame jpeg data
	 * @param size - The size of the buffer.
	 *
	 * @return 0 on success, < 0 on error
	 */
	public void sceUsbCamReadVideoFrame(Processor processor) {
		CpuState cpu = processor.cpu;

		int jpegBuffer = cpu.gpr[4];
		int jpegBufferSize = cpu.gpr[5];

		log.warn(String.format("Unimplemented sceUsbCamReadVideoFrame jpegBuffer=0x%08X, jpegBufferSize=%d", jpegBuffer, jpegBufferSize));

		this.jpegBuffer = jpegBuffer;
		this.jpegBufferSize = jpegBufferSize;

		cpu.gpr[2] = 0;
	}

	/**
	 * Polls the status of video frame read completion.
	 *
	 * @return the size of the acquired frame if it has been read,
	 * 0 if the frame has not yet been read, < 0 on error.
	 */
	public void sceUsbCamPollReadVideoFrameEnd(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn(String.format("Unimplemented sceUsbCamPollReadVideoFrameEnd"));

		cpu.gpr[2] = readFakeVideoFrame();
	}

	/**
	 * Waits untils the current frame has been read.
	 *
	 * @return the size of the acquired frame on sucess, < 0 on error
	 */
	public void sceUsbCamWaitReadVideoFrameEnd(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn(String.format("Unimplemented sceUsbCamWaitReadVideoFrameEnd"));

		cpu.gpr[2] = readFakeVideoFrame();
	}

	/**
	 * Gets the direction of the camera lens
	 *
	 * @return 1 if the camera is "looking to you", 0 if the camera
	 * is "looking to the other side".
	 */
	public void sceUsbCamGetLensDirection(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn(String.format("Unimplemented sceUsbCamGetLensDirection"));

		cpu.gpr[2] = lensDirectionAtYou ? 1 : 0;
	}

	/**
	 * Setups the parameters to take a still image.
	 *
	 * @param param - pointer to a pspUsbCamSetupStillParam
	 *
	 * @return 0 on success, < 0 on error
	 */
	public void sceUsbCamSetupStill(Processor processor) {
		CpuState cpu = processor.cpu;

		int paramAddr = cpu.gpr[4];

		pspUsbCamSetupStillParam usbCamSetupStillParam = new pspUsbCamSetupStillParam();
		usbCamSetupStillParam.read(Processor.memory, paramAddr);

		log.warn(String.format("Unimplemented sceUsbCamSetupStill param=0x%08X, %s", paramAddr, usbCamSetupStillParam.toString()));

		cpu.gpr[2] = 0;
	}

	/**
	 * Setups the parameters to take a still image (with more options)
	 *
	 * @param param - pointer to a pspUsbCamSetupStillParamEx
	 *
	 * @return 0 on success, < 0 on error
	 */
	public void sceUsbCamSetupStillEx(Processor processor) {
		CpuState cpu = processor.cpu;

		int paramAddr = cpu.gpr[4];

		pspUsbCamSetupStillExParam usbCamSetupStillExParam = new pspUsbCamSetupStillExParam();
		usbCamSetupStillExParam.read(Processor.memory, paramAddr);

		log.warn(String.format("Unimplemented sceUsbCamSetupStillEx param=0x%08X, %s", paramAddr, usbCamSetupStillExParam.toString()));

		cpu.gpr[2] = 0;
	}

	/**
	 * Gets a still image. The function doesn't return until the image
	 * has been acquired.
	 *
	 * @param buf - The buffer that receives the image jpeg data
	 * @param size - The size of the buffer.
	 *
	 * @return size of acquired image on success, < 0 on error
	 */
	public void sceUsbCamStillInputBlocking(Processor processor) {
		CpuState cpu = processor.cpu;

		int buffer = cpu.gpr[4];
		int size = cpu.gpr[5];

		log.warn(String.format("Unimplemented sceUsbCamStillInputBlocking buffer=0x%08X, size=%d", buffer, size));

		cpu.gpr[2] = 0;
	}

	/**
	 * Gets a still image. The function returns immediately, and
	 * the completion has to be handled by calling ::sceUsbCamStillWaitInputEnd
	 * or ::sceUsbCamStillPollInputEnd.
	 *
	 * @param buf - The buffer that receives the image jpeg data
	 * @param size - The size of the buffer.
	 *
	 * @return 0 on success, < 0 on error
	 */
	public void sceUsbCamStillInput(Processor processor) {
		CpuState cpu = processor.cpu;

		int buffer = cpu.gpr[4];
		int size = cpu.gpr[5];

		log.warn(String.format("Unimplemented sceUsbCamStillInput buffer=0x%08X, size=%d", buffer, size));

		cpu.gpr[2] = 0;
	}

	/**
	 * Waits until still input has been finished.
	 *
	 * @return the size of the acquired image on success, < 0 on error
	 */
	public void sceUsbCamStillWaitInputEnd(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn(String.format("Unimplemented sceUsbCamStillWaitInputEnd"));

		cpu.gpr[2] = 0;
	}

	/**
	 * Polls the status of still input completion.
	 *
	 * @return the size of the acquired image if still input has ended,
	 * 0 if the input has not ended, < 0 on error.
	 */
	public void sceUsbCamStillPollInputEnd(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn(String.format("Unimplemented sceUsbCamStillPollInputEnd"));

		cpu.gpr[2] = 0;
	}

	/**
	 * Cancels the still input.
	 *
	 * @return 0 on success, < 0 on error
	 */
	public void sceUsbCamStillCancelInput(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn(String.format("Unimplemented sceUsbCamStillCancelInput"));

		cpu.gpr[2] = 0;
	}

	/**
	 * Gets the size of the acquired still image.
	 *
	 * @return the size of the acquired image on success, < 0 on error
	 */
	public void sceUsbCamStillGetInputLength(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn(String.format("Unimplemented sceUsbCamStillGetInputLength"));

		cpu.gpr[2] = 0;
	}

	/**
	 * Set ups the parameters for video capture (with more options)
	 *
	 * @param param - Pointer to a pspUsbCamSetupVideoExParam structure.
	 * @param workarea - Pointer to a buffer used as work area by the driver.
	 * @param wasize - Size of the work area.
	 *
	 * @return 0 on success, < 0 on error
	 */
	public void sceUsbCamSetupVideoEx(Processor processor) {
		CpuState cpu = processor.cpu;
		Memory mem = Processor.memory;

		int param = cpu.gpr[4];
		int workArea = cpu.gpr[5];
		int workAreaSize = cpu.gpr[6];

		pspUsbCamSetupVideoExParam usbCamSetupVideoExParam = new pspUsbCamSetupVideoExParam();
		usbCamSetupVideoExParam.read(mem, param);

		log.warn(String.format("Partial sceUsbCamSetupVideoEx param=0x%08X, workArea=0x%08X, workAreaSize=%d, param=%s", param, workArea, workAreaSize, usbCamSetupVideoExParam.toString()));

		this.workArea = workArea;
		this.workAreaSize = workAreaSize;
		resolution = convertResolutionExToResolution(usbCamSetupVideoExParam.resolution);
		frameRate = usbCamSetupVideoExParam.framerate;
		whiteBalance = usbCamSetupVideoExParam.wb;
		saturation = usbCamSetupVideoExParam.saturation;
		brightness = usbCamSetupVideoExParam.brightness;
		contrast = usbCamSetupVideoExParam.contrast;
		sharpness = usbCamSetupVideoExParam.sharpness;
		imageEffectMode = usbCamSetupVideoExParam.effectmode;
		frameSize = usbCamSetupVideoExParam.framesize;
		evLevel = usbCamSetupVideoExParam.evlevel;

		cpu.gpr[2] = 0;
	}

	/**
	 * Gets the size of the acquired frame.
	 *
	 * @return the size of the acquired frame on success, < 0 on error
	 */
	public void sceUsbCamGetReadVideoFrameSize(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn(String.format("Unimplemented sceUsbCamGetReadVideoFrameSize"));

		cpu.gpr[2] = jpegBufferSize;
	}

	/**
	 * Sets the saturation
	 *
	 * @param saturation - The saturation (0-255)
	 *
	 * @return 0 on success, < 0 on error
	 */
	public void sceUsbCamSetSaturation(Processor processor) {
		CpuState cpu = processor.cpu;

		int saturation = cpu.gpr[4];

		log.warn(String.format("Unimplemented sceUsbCamSetSaturation %d", saturation));

		this.saturation = saturation;

		cpu.gpr[2] = 0;
	}

	/**
	 * Sets the brightness
	 *
	 * @param brightness - The brightness (0-255)
	 *
	 * @return 0 on success, < 0 on error
	 */
	public void sceUsbCamSetBrightness(Processor processor) {
		CpuState cpu = processor.cpu;

		int brightness = cpu.gpr[4];

		log.warn(String.format("Unimplemented sceUsbCamSetBrightness %d", brightness));

		this.brightness = brightness;

		cpu.gpr[2] = 0;
	}

	/**
	 * Sets the contrast
	 *
	 * @param contrast - The contrast (0-255)
	 *
	 * @return 0 on success, < 0 on error
	 */
	public void sceUsbCamSetContrast(Processor processor) {
		CpuState cpu = processor.cpu;

		int contrast = cpu.gpr[4];

		log.warn(String.format("Unimplemented sceUsbCamSetContrast %d", contrast));

		this.contrast = contrast;

		cpu.gpr[2] = 0;
	}

	/**
	 * Sets the sharpness
	 *
	 * @param sharpness - The sharpness (0-255)
	 *
	 * @return 0 on success, < 0 on error
	 */
	public void sceUsbCamSetSharpness(Processor processor) {
		CpuState cpu = processor.cpu;

		int sharpness = cpu.gpr[4];

		log.warn(String.format("Unimplemented sceUsbCamSetSharpness %d", sharpness));

		this.sharpness = sharpness;

		cpu.gpr[2] = 0;
	}

	/**
	 * Sets the image effect mode
	 *
	 * @param effectmode - The effect mode, one of ::PspUsbCamEffectMode
	 *
	 * @return 0 on success, < 0 on error
	 */
	public void sceUsbCamSetImageEffectMode(Processor processor) {
		CpuState cpu = processor.cpu;

		int effectMode = cpu.gpr[4];

		log.warn(String.format("Unimplemented sceUsbCamSetImageEffectMode %d", effectMode));

		this.imageEffectMode = effectMode;

		cpu.gpr[2] = 0;
	}

	/**
	 * Sets the exposure level
	 *
	 * @param ev - The exposure level, one of ::PspUsbCamEVLevel
	 *
	 * @return 0 on success, < 0 on error
	 */
	public void sceUsbCamSetEvLevel(Processor processor) {
		CpuState cpu = processor.cpu;

		int evLevel = cpu.gpr[4];

		log.warn(String.format("Unimplemented sceUsbCamSetEvLevel %d", evLevel));

		this.evLevel = evLevel;

		cpu.gpr[2] = 0;
	}

	/**
	 * Sets the reverse mode
	 *
	 * @param reverseflags - The reverse flags, zero or more of ::PspUsbCamReverseFlags
	 *
	 * @return 0 on success, < 0 on error
	 */
	public void sceUsbCamSetReverseMode(Processor processor) {
		CpuState cpu = processor.cpu;

		int reverseMode = cpu.gpr[4];

		log.warn(String.format("Unimplemented sceUsbCamSetReverseMode %d", reverseMode));

		this.flip = (reverseMode & PSP_USBCAM_FLIP) != 0;
		this.mirror = (reverseMode & PSP_USBCAM_MIRROR) != 0;

		cpu.gpr[2] = 0;
	}

	/**
	 * Sets the zoom.
	 *
	 * @param zoom - The zoom level starting by 10. (10 = 1X, 11 = 1.1X, etc)
	 *
	 * @returns 0 on success, < 0 on error
	 */
	public void sceUsbCamSetZoom(Processor processor) {
		CpuState cpu = processor.cpu;

		int zoom = cpu.gpr[4];

		log.warn(String.format("Unimplemented sceUsbCamSetZoom %d", zoom));

		this.zoom = zoom;

		cpu.gpr[2] = 0;
	}

	/**
	 * Gets the current saturation
	 *
	 * @param saturation - pointer to a variable that receives the current saturation
	 *
	 * @return 0 on success, < 0 on error
	 */
	public void sceUsbCamGetSaturation(Processor processor) {
		CpuState cpu = processor.cpu;

		int saturationAddr = cpu.gpr[4];

		log.warn(String.format("Unimplemented sceUsbCamGetSaturation 0x%08X", saturationAddr));

		Processor.memory.write32(saturationAddr, saturation);

		cpu.gpr[2] = 0;
	}

	/**
	 * Gets the current brightness
	 *
	 * @param brightness - pointer to a variable that receives the current brightness
	 *
	 * @return 0 on success, < 0 on error
	 */
	public void sceUsbCamGetBrightness(Processor processor) {
		CpuState cpu = processor.cpu;

		int brightnessAddr = cpu.gpr[4];

		log.warn(String.format("Unimplemented sceUsbCamGetBrightness 0x%08X", brightnessAddr));

		Processor.memory.write32(brightnessAddr, brightness);

		cpu.gpr[2] = 0;
	}

	/**
	 * Gets the current contrast
	 *
	 * @param contrast - pointer to a variable that receives the current contrast
	 *
	 * @return 0 on success, < 0 on error
	 */
	public void sceUsbCamGetContrast(Processor processor) {
		CpuState cpu = processor.cpu;

		int contrastAddr = cpu.gpr[4];

		log.warn(String.format("Unimplemented sceUsbCamGetContrast 0x%08X", contrastAddr));

		Processor.memory.write32(contrastAddr, contrast);

		cpu.gpr[2] = 0;
	}

	/**
	 * Gets the current sharpness
	 *
	 * @param brightness - pointer to a variable that receives the current sharpness
	 *
	 * @return 0 on success, < 0 on error
	 */
	public void sceUsbCamGetSharpness(Processor processor) {
		CpuState cpu = processor.cpu;

		int sharpnessAddr = cpu.gpr[4];

		log.warn(String.format("Unimplemented sceUsbCamGetSharpness 0x%08X", sharpnessAddr));

		Processor.memory.write32(sharpnessAddr, sharpness);

		cpu.gpr[2] = 0;
	}

	/**
	 * Gets the current image efect mode
	 *
	 * @param effectmode - pointer to a variable that receives the current effect mode
	 *
	 * @return 0 on success, < 0 on error
	 */
	public void sceUsbCamGetImageEffectMode(Processor processor) {
		CpuState cpu = processor.cpu;

		int effectModeAddr = cpu.gpr[4];

		log.warn(String.format("Unimplemented sceUsbCamGetImageEffectMode 0x%08X", effectModeAddr));

		Processor.memory.write32(effectModeAddr, imageEffectMode);

		cpu.gpr[2] = 0;
	}

	/**
	 * Gets the current exposure level.
	 *
	 * @param ev - pointer to a variable that receives the current exposure level
	 *
	 * @return 0 on success, < 0 on error
	 */
	public void sceUsbCamGetEvLevel(Processor processor) {
		CpuState cpu = processor.cpu;

		int evLevelAddr = cpu.gpr[4];

		log.warn(String.format("Unimplemented sceUsbCamGetEvLevel 0x%08X", evLevelAddr));

		Processor.memory.write32(evLevelAddr, evLevel);

		cpu.gpr[2] = 0;
	}

	/**
	 * Gets the current reverse mode.
	 *
	 * @param reverseflags - pointer to a variable that receives the current reverse mode flags
	 *
	 * @return 0 on success, < 0 on error
	 */
	public void sceUsbCamGetReverseMode(Processor processor) {
		CpuState cpu = processor.cpu;

		int reverseModeAddr = cpu.gpr[4];

		log.warn(String.format("Unimplemented sceUsbCamGetReverseMode 0x%08X", reverseModeAddr));

		int reverseMode = 0;
		if (mirror) {
			reverseMode |= PSP_USBCAM_MIRROR;
		}
		if (flip) {
			reverseMode |= PSP_USBCAM_FLIP;
		}

		Processor.memory.write32(reverseModeAddr, reverseMode);

		cpu.gpr[2] = 0;
	}

	/**
	 * Gets the current zoom.
	 *
	 * @param zoom - pointer to a variable that receives the current zoom
	 *
	 * @return 0 on success, < 0 on error
	 */
	public void sceUsbCamGetZoom(Processor processor) {
		CpuState cpu = processor.cpu;

		int zoomAddr = cpu.gpr[4];

		log.warn(String.format("Unimplemented sceUsbCamGetZoom 0x%08X", zoomAddr));

		Processor.memory.write32(zoomAddr, zoom);

		cpu.gpr[2] = 0;
	}

	/**
	 * Gets the state of the autoreversal of the image.
	 *
	 * @return 1 if it is set to automatic, 0 otherwise
	 */
	public void sceUsbCamGetAutoImageReverseState(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn(String.format("Unimplemented sceUsbCamGetAutoImageReverseState"));

		cpu.gpr[2] = autoImageReverseSW ? 1 : 0;
	}

	public void sceUsbCamSetMicGain(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn(String.format("Unimplemented sceUsbCamSetMicGain"));

		cpu.gpr[2] = 0;
	}

	public void sceUsbCamSetupMicEx(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn(String.format("Unimplemented sceUsbCamSetupMicEx"));

		cpu.gpr[2] = 0;
	}

	public void sceUsbCamReadMicBlocking(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn(String.format("Unimplemented sceUsbCamReadMicBlocking"));

		cpu.gpr[2] = 0;
	}

	public void sceUsbCamReadMic(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn(String.format("Unimplemented sceUsbCamReadMic"));

		cpu.gpr[2] = 0;
	}

	public void sceUsbCamUnregisterLensRotationCallback(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn(String.format("Unimplemented sceUsbCamUnregisterLensRotationCallback"));

		cpu.gpr[2] = 0;
	}

	public void sceUsbCamStopMic(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn(String.format("Unimplemented sceUsbCamStopMic"));

		cpu.gpr[2] = 0;
	}

	public void sceUsbCamGetMicDataLength(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn(String.format("Unimplemented sceUsbCamGetMicDataLength"));

		cpu.gpr[2] = 0;
	}

	public void sceUsbCamSetAntiFlicker(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn(String.format("Unimplemented sceUsbCamSetAntiFlicker"));

		cpu.gpr[2] = 0;
	}

	public void sceUsbCamGetAntiFlicker(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn(String.format("Unimplemented sceUsbCamGetAntiFlicker"));

		cpu.gpr[2] = 0;
	}

	public void sceUsbCamWaitReadMicEnd(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn(String.format("Unimplemented sceUsbCamWaitReadMicEnd"));

		cpu.gpr[2] = 0;
	}

	public void sceUsbCamRegisterLensRotationCallback(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn(String.format("Unimplemented sceUsbCamRegisterLensRotationCallback"));

		cpu.gpr[2] = 0;
	}

	public void sceUsbCamPollReadMicEnd(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn(String.format("Unimplemented sceUsbCamPollReadMicEnd"));

		cpu.gpr[2] = 0;
	}

	@HLEFunction(nid = 0x99D86281, version = 271)
	public final HLEModuleFunction sceUsbCamReadVideoFrameFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamReadVideoFrame") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamReadVideoFrame(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamReadVideoFrame(processor);";
		}
	};
	@HLEFunction(nid = 0x17F7B2FB, version = 271)
	public final HLEModuleFunction sceUsbCamSetupVideoFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamSetupVideo") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamSetupVideo(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamSetupVideo(processor);";
		}
	};
	@HLEFunction(nid = 0xF93C4669, version = 271)
	public final HLEModuleFunction sceUsbCamAutoImageReverseSWFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamAutoImageReverseSW") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamAutoImageReverseSW(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamAutoImageReverseSW(processor);";
		}
	};
	@HLEFunction(nid = 0x574A8C3F, version = 271)
	public final HLEModuleFunction sceUsbCamStartVideoFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamStartVideo") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamStartVideo(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamStartVideo(processor);";
		}
	};
	@HLEFunction(nid = 0x6CF32CB9, version = 271)
	public final HLEModuleFunction sceUsbCamStopVideoFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamStopVideo") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamStopVideo(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamStopVideo(processor);";
		}
	};
	@HLEFunction(nid = 0x03ED7A82, version = 271)
	public final HLEModuleFunction sceUsbCamSetupMicFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamSetupMic") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamSetupMic(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamSetupMic(processor);";
		}
	};
	@HLEFunction(nid = 0x82A64030, version = 271)
	public final HLEModuleFunction sceUsbCamStartMicFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamStartMic") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamStartMic(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamStartMic(processor);";
		}
	};
	@HLEFunction(nid = 0x41E73E95, version = 271)
	public final HLEModuleFunction sceUsbCamPollReadVideoFrameEndFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamPollReadVideoFrameEnd") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamPollReadVideoFrameEnd(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamPollReadVideoFrameEnd(processor);";
		}
	};
	@HLEFunction(nid = 0x7DAC0C71, version = 271)
	public final HLEModuleFunction sceUsbCamReadVideoFrameBlockingFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamReadVideoFrameBlocking") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamReadVideoFrameBlocking(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamReadVideoFrameBlocking(processor);";
		}
	};
	@HLEFunction(nid = 0xF90B2293, version = 271)
	public final HLEModuleFunction sceUsbCamWaitReadVideoFrameEndFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamWaitReadVideoFrameEnd") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamWaitReadVideoFrameEnd(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamWaitReadVideoFrameEnd(processor);";
		}
	};
	@HLEFunction(nid = 0xD4876173, version = 271)
	public final HLEModuleFunction sceUsbCamSetImageEffectModeFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamSetImageEffectMode") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamSetImageEffectMode(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamSetImageEffectMode(processor);";
		}
	};
	@HLEFunction(nid = 0x4C34F553, version = 271)
	public final HLEModuleFunction sceUsbCamGetLensDirectionFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamGetLensDirection") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamGetLensDirection(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamGetLensDirection(processor);";
		}
	};
	@HLEFunction(nid = 0x08AEE98A, version = 271)
	public final HLEModuleFunction sceUsbCamSetMicGainFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamSetMicGain") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamSetMicGain(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamSetMicGain(processor);";
		}
	};
	@HLEFunction(nid = 0x09C26C7E, version = 271)
	public final HLEModuleFunction sceUsbCamSetContrastFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamSetContrast") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamSetContrast(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamSetContrast(processor);";
		}
	};
	@HLEFunction(nid = 0x0A41A298, version = 271)
	public final HLEModuleFunction sceUsbCamSetupStillExFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamSetupStillEx") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamSetupStillEx(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamSetupStillEx(processor);";
		}
	};
	@HLEFunction(nid = 0x11A1F128, version = 271)
	public final HLEModuleFunction sceUsbCamGetAutoImageReverseStateFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamGetAutoImageReverseState") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamGetAutoImageReverseState(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamGetAutoImageReverseState(processor);";
		}
	};
	@HLEFunction(nid = 0x1A46CFE7, version = 271)
	public final HLEModuleFunction sceUsbCamStillPollInputEndFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamStillPollInputEnd") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamStillPollInputEnd(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamStillPollInputEnd(processor);";
		}
	};
	@HLEFunction(nid = 0x1D686870, version = 271)
	public final HLEModuleFunction sceUsbCamSetEvLevelFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamSetEvLevel") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamSetEvLevel(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamSetEvLevel(processor);";
		}
	};
	@HLEFunction(nid = 0x2BCD50C0, version = 271)
	public final HLEModuleFunction sceUsbCamGetEvLevelFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamGetEvLevel") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamGetEvLevel(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamGetEvLevel(processor);";
		}
	};
	@HLEFunction(nid = 0x2E930264, version = 271)
	public final HLEModuleFunction sceUsbCamSetupMicExFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamSetupMicEx") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamSetupMicEx(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamSetupMicEx(processor);";
		}
	};
	@HLEFunction(nid = 0x36636925, version = 271)
	public final HLEModuleFunction sceUsbCamReadMicBlockingFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamReadMicBlocking") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamReadMicBlocking(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamReadMicBlocking(processor);";
		}
	};
	@HLEFunction(nid = 0x383E9FA8, version = 271)
	public final HLEModuleFunction sceUsbCamGetSaturationFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamGetSaturation") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamGetSaturation(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamGetSaturation(processor);";
		}
	};
	@HLEFunction(nid = 0x3DC0088E, version = 271)
	public final HLEModuleFunction sceUsbCamReadMicFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamReadMic") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamReadMic(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamReadMic(processor);";
		}
	};
	@HLEFunction(nid = 0x3F0CF289, version = 271)
	public final HLEModuleFunction sceUsbCamSetupStillFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamSetupStill") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamSetupStill(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamSetupStill(processor);";
		}
	};
	@HLEFunction(nid = 0x41EE8797, version = 271)
	public final HLEModuleFunction sceUsbCamUnregisterLensRotationCallbackFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamUnregisterLensRotationCallback") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamUnregisterLensRotationCallback(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamUnregisterLensRotationCallback(processor);";
		}
	};
	@HLEFunction(nid = 0x4F3D84D5, version = 271)
	public final HLEModuleFunction sceUsbCamSetBrightnessFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamSetBrightness") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamSetBrightness(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamSetBrightness(processor);";
		}
	};
	@HLEFunction(nid = 0x5145868A, version = 271)
	public final HLEModuleFunction sceUsbCamStopMicFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamStopMic") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamStopMic(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamStopMic(processor);";
		}
	};
	@HLEFunction(nid = 0x5778B452, version = 271)
	public final HLEModuleFunction sceUsbCamGetMicDataLengthFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamGetMicDataLength") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamGetMicDataLength(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamGetMicDataLength(processor);";
		}
	};
	@HLEFunction(nid = 0x61BE5CAC, version = 271)
	public final HLEModuleFunction sceUsbCamStillInputBlockingFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamStillInputBlocking") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamStillInputBlocking(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamStillInputBlocking(processor);";
		}
	};
	@HLEFunction(nid = 0x622F83CC, version = 271)
	public final HLEModuleFunction sceUsbCamSetSharpnessFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamSetSharpness") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamSetSharpness(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamSetSharpness(processor);";
		}
	};
	@HLEFunction(nid = 0x6784E6A8, version = 271)
	public final HLEModuleFunction sceUsbCamSetAntiFlickerFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamSetAntiFlicker") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamSetAntiFlicker(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamSetAntiFlicker(processor);";
		}
	};
	@HLEFunction(nid = 0x6E205974, version = 271)
	public final HLEModuleFunction sceUsbCamSetSaturationFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamSetSaturation") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamSetSaturation(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamSetSaturation(processor);";
		}
	};
	@HLEFunction(nid = 0x70F522C5, version = 271)
	public final HLEModuleFunction sceUsbCamGetBrightnessFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamGetBrightness") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamGetBrightness(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamGetBrightness(processor);";
		}
	};
	@HLEFunction(nid = 0x7563AFA1, version = 271)
	public final HLEModuleFunction sceUsbCamStillWaitInputEndFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamStillWaitInputEnd") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamStillWaitInputEnd(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamStillWaitInputEnd(processor);";
		}
	};
	@HLEFunction(nid = 0x951BEDF5, version = 271)
	public final HLEModuleFunction sceUsbCamSetReverseModeFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamSetReverseMode") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamSetReverseMode(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamSetReverseMode(processor);";
		}
	};
	@HLEFunction(nid = 0x994471E0, version = 271)
	public final HLEModuleFunction sceUsbCamGetImageEffectModeFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamGetImageEffectMode") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamGetImageEffectMode(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamGetImageEffectMode(processor);";
		}
	};
	@HLEFunction(nid = 0x9E8AAF8D, version = 271)
	public final HLEModuleFunction sceUsbCamGetZoomFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamGetZoom") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamGetZoom(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamGetZoom(processor);";
		}
	};
	@HLEFunction(nid = 0xA063A957, version = 271)
	public final HLEModuleFunction sceUsbCamGetContrastFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamGetContrast") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamGetContrast(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamGetContrast(processor);";
		}
	};
	@HLEFunction(nid = 0xA720937C, version = 271)
	public final HLEModuleFunction sceUsbCamStillCancelInputFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamStillCancelInput") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamStillCancelInput(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamStillCancelInput(processor);";
		}
	};
	@HLEFunction(nid = 0xAA7D94BA, version = 271)
	public final HLEModuleFunction sceUsbCamGetAntiFlickerFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamGetAntiFlicker") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamGetAntiFlicker(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamGetAntiFlicker(processor);";
		}
	};
	@HLEFunction(nid = 0xB048A67D, version = 271)
	public final HLEModuleFunction sceUsbCamWaitReadMicEndFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamWaitReadMicEnd") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamWaitReadMicEnd(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamWaitReadMicEnd(processor);";
		}
	};
	@HLEFunction(nid = 0xC484901F, version = 271)
	public final HLEModuleFunction sceUsbCamSetZoomFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamSetZoom") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamSetZoom(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamSetZoom(processor);";
		}
	};
	@HLEFunction(nid = 0xCFE9E999, version = 271)
	public final HLEModuleFunction sceUsbCamSetupVideoExFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamSetupVideoEx") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamSetupVideoEx(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamSetupVideoEx(processor);";
		}
	};
	@HLEFunction(nid = 0xD293A100, version = 271)
	public final HLEModuleFunction sceUsbCamRegisterLensRotationCallbackFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamRegisterLensRotationCallback") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamRegisterLensRotationCallback(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamRegisterLensRotationCallback(processor);";
		}
	};
	@HLEFunction(nid = 0xD5279339, version = 271)
	public final HLEModuleFunction sceUsbCamGetReverseModeFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamGetReverseMode") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamGetReverseMode(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamGetReverseMode(processor);";
		}
	};
	@HLEFunction(nid = 0xDF9D0C92, version = 271)
	public final HLEModuleFunction sceUsbCamGetReadVideoFrameSizeFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamGetReadVideoFrameSize") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamGetReadVideoFrameSize(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamGetReadVideoFrameSize(processor);";
		}
	};
	@HLEFunction(nid = 0xE5959C36, version = 271)
	public final HLEModuleFunction sceUsbCamStillGetInputLengthFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamStillGetInputLength") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamStillGetInputLength(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamStillGetInputLength(processor);";
		}
	};
	@HLEFunction(nid = 0xF8847F60, version = 271)
	public final HLEModuleFunction sceUsbCamPollReadMicEndFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamPollReadMicEnd") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamPollReadMicEnd(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamPollReadMicEnd(processor);";
		}
	};
	@HLEFunction(nid = 0xFB0A6C5D, version = 271)
	public final HLEModuleFunction sceUsbCamStillInputFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamStillInput") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamStillInput(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamStillInput(processor);";
		}
	};
	@HLEFunction(nid = 0xFDB68C23, version = 271)
	public final HLEModuleFunction sceUsbCamGetSharpnessFunction = new HLEModuleFunction("sceUsbCam", "sceUsbCamGetSharpness") {
		@Override
		public final void execute(Processor processor) {
			sceUsbCamGetSharpness(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUsbCamModule.sceUsbCamGetSharpness(processor);";
		}
	};
}
