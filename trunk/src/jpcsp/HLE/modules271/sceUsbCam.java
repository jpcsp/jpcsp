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

import static jpcsp.HLE.modules271.sceJpeg.clamp8bit;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;

import org.apache.log4j.Logger;

import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.MediaListenerAdapter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.mediatool.event.IVideoPictureEvent;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IContainerFormat;
import com.xuggle.xuggler.IPixelFormat.Type;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.IVideoResampler;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;

import jpcsp.Emulator;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.pspUsbCamSetupMicParam;
import jpcsp.HLE.kernel.types.pspUsbCamSetupStillExParam;
import jpcsp.HLE.kernel.types.pspUsbCamSetupStillParam;
import jpcsp.HLE.kernel.types.pspUsbCamSetupVideoExParam;
import jpcsp.HLE.kernel.types.pspUsbCamSetupVideoParam;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryWriter;

@HLELogging
public class sceUsbCam extends HLEModule {
    public static Logger log = Modules.getLogger("sceUsbCam");
    private static final boolean dumpJpeg = false;

	@Override
	public String getName() {
		return "sceUsbCam";
	}

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
	protected static final int[] resolutionWidth = new int[] {
		160,
		176,
		320,
		352,
		640,
		1024,
		1280,
		480,
		360
	};
	protected static final int[] resolutionHeight = new int[] {
		120,
		144,
		240,
		288,
		480,
		768,
		960,
		272,
		272
	};

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
	protected TPointer jpegBuffer;
	protected int jpegBufferSize;
	protected BufferedImage currentVideoImage;
	protected int currentVideoFrameCount;
	protected int lastVideoFrameCount;
	protected VideoListener videoListener;

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
	protected int micFrequency;
	protected int micGain;

	protected TPointer readMicBuffer;
	protected int readMicBufferSize;

	protected class VideoListener extends MediaListenerAdapter {
		IMediaReader reader;
		int videoStream;
		IStreamCoder videoCoder;
		IConverter videoConverter;
		IVideoResampler videoResampler;
		IVideoPicture videoPicture;
		VideoReaderThread videoReaderThread;

		public VideoListener(IMediaReader reader, int width, int height) {
			this.reader = reader;
			IContainer container = reader.getContainer();
			int numStreams = container.getNumStreams();
			for (int i = 0; i < numStreams; i++) {
				IStream stream = container.getStream(i);
				IStreamCoder coder = stream.getStreamCoder();

				if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
					videoStream = i;
					videoCoder = coder;
				}
			}

			if (videoCoder != null) {
				videoConverter = ConverterFactory.createConverter(ConverterFactory.XUGGLER_BGR_24, videoCoder.getPixelType(), videoCoder.getWidth(), videoCoder.getHeight());
				videoPicture = IVideoPicture.make(videoCoder.getPixelType(), width, height);
				videoResampler = IVideoResampler.make(width, height, videoCoder.getPixelType(), videoCoder.getWidth(), videoCoder.getHeight(), videoCoder.getPixelType());
			}

			videoReaderThread = new VideoReaderThread(reader);
			videoReaderThread.setName("Video Reader Thread");
			videoReaderThread.setDaemon(true);
			videoReaderThread.start();
		}

		public void stop() {
			videoReaderThread.end();
		}

		@Override
		public void onVideoPicture(IVideoPictureEvent event) {
			BufferedImage image = event.getImage();
			if (image == null && videoConverter != null) {
				IVideoPicture eventPicture = event.getPicture();
				videoResampler.resample(videoPicture, eventPicture);
				try {
					image = videoConverter.toImage(videoPicture);
				} catch (RuntimeException e) {
					if (videoPicture.getPixelType() == Type.YUYV422) {
						image = convertYUYV422toRGB(videoPicture.getWidth(), videoPicture.getHeight(), videoPicture.getByteBuffer());
					} else {
						log.error(String.format("VideoListener.onVideoPicture: %s", videoPicture), e);
					}
				}
			}

			if (log.isDebugEnabled()) {
				log.debug(String.format("onVideoPicture event=%s, image=%s", event, image));
			}
			if (image != null) {
				setCurrentVideoImage(image);
			}
		}
	}

	protected class VideoReaderThread extends Thread {
		private IMediaReader reader;
		private volatile boolean end;

		public VideoReaderThread(IMediaReader reader) {
			this.reader = reader;
		}

		public void end() {
			end = true;
		}

		@Override
		public void run() {
			while (!end) {
				reader.readPacket();
			}
			reader.close();
		}
	}

	// Faked video reading
	protected long lastVideoFrameMillis;
	protected static final int[] framerateFrameDurationMillis = new int[] {
		267, // PSP_USBCAM_FRAMERATE_3_75_FPS
		200, // PSP_USBCAM_FRAMERATE_5_FPS
		133, // PSP_USBCAM_FRAMERATE_7_5_FPS
		100, // PSP_USBCAM_FRAMERATE_10_FPS
		67, // PSP_USBCAM_FRAMERATE_15_FPS
		50, // PSP_USBCAM_FRAMERATE_20_FPS
		33, // PSP_USBCAM_FRAMERATE_30_FPS
		17 // PSP_USBCAM_FRAMERATE_60_FPS
	};

	protected static int convertYUVtoRGB(int y, int u, int v) {
		// based on http://en.wikipedia.org/wiki/Yuv#Y.27UV444_to_RGB888_conversion
		int c = y - 16;
		int d = u - 128;
		int e = v - 128;
		int r = clamp8bit((298 * c + 409 * e + 128) >> 8);
		int g = clamp8bit((298 * c - 100 * d - 208 * e + 128) >> 8);
		int b = clamp8bit((298 * c + 516 * d + 128) >> 8);

		return (r << 16) | (g << 8) | b;
	}

	protected BufferedImage convertYUYV422toRGB(int width, int height, ByteBuffer buffer) {
		byte[] input = new byte[width * height * 2];
		buffer.get(input);

		int[] output = new int[width * height];
		int i = 0;
		int j = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x += 2) {
				int y0 = input[i++] & 0xFF;
				int u = input[i++] & 0xFF;
				int y1 = input[i++] & 0xFF;
				int v = input[i++] & 0xFF;
				output[j++] = convertYUVtoRGB(y0, u, v);
				output[j++] = convertYUVtoRGB(y1, u, v);
			}
		}

		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		image.setRGB(0, 0, width, height, output, 0, width);

		return image;
	}

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

	protected int getFramerateFrameDurationMillis() {
		if (frameRate < 0 || frameRate > PSP_USBCAM_FRAMERATE_60_FPS) {
			return framerateFrameDurationMillis[PSP_USBCAM_FRAMERATE_60_FPS];
		}
		return framerateFrameDurationMillis[frameRate];
	}

	protected int readFakeVideoFrame() {
		if (jpegBuffer == null) {
			return 0;
		}

		// Image has to be stored in Jpeg format in buffer
		jpegBuffer.clear(jpegBufferSize);

		return jpegBufferSize;
	}

	@Override
	public void stop() {
		if (videoListener != null) {
			videoListener.stop();
			videoListener = null;
		}

		super.stop();
	}

	protected static int getResolutionWidth(int resolution) {
		if (resolution < 0 || resolution >= resolutionWidth.length) {
			return 0;
		}
		return resolutionWidth[resolution];
	}

	protected static int getResolutionHeight(int resolution) {
		if (resolution < 0 || resolution >= resolutionHeight.length) {
			return 0;
		}
		return resolutionHeight[resolution];
	}

	protected boolean setupVideo() {
		if (videoListener != null) {
			return true;
		}

		IContainer container = IContainer.make();
		IContainerFormat format = IContainerFormat.make();
		int ret = format.setInputFormat("vfwcap");
		if (ret < 0) {
			log.error(String.format("USB Cam: cannot open WebCam ('vfwcap' device)"));
			return false;
		}
		ret = container.open("0", IContainer.Type.READ, format);
		if (ret < 0) {
			log.error(String.format("USB Cam: cannot open WebCam ('0')"));
			return false;
		}
		IMediaReader reader = ToolFactory.makeReader(container);
		videoListener = new VideoListener(reader, getResolutionWidth(resolution), getResolutionHeight(resolution));
		reader.addListener(videoListener);

		return true;
	}

	protected void setCurrentVideoImage(BufferedImage image) {
		currentVideoImage = image;
		currentVideoFrameCount++;
	}

	public BufferedImage getCurrentVideoImage() {
		return currentVideoImage;
	}

	public int writeCurrentVideoImage(TPointer jpegBuffer, int jpegBufferSize) {
		lastVideoFrameCount = currentVideoFrameCount;

		if (getCurrentVideoImage() == null) {
			return readFakeVideoFrame();
		}

		int length = -1;
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream(jpegBufferSize);
			if (ImageIO.write(getCurrentVideoImage(), "jpeg", outputStream)) {
				outputStream.close();
				IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(jpegBuffer.getAddress(), jpegBufferSize, 1);
				byte[] bytes = outputStream.toByteArray();
				length = Math.min(bytes.length, jpegBufferSize);
				for (int i = 0; i < length; i++) {
					memoryWriter.writeNext(bytes[i] & 0xFF);
				}
				memoryWriter.flush();

				if (dumpJpeg) {
					FileOutputStream dumpFile = new FileOutputStream("dumpUsbCam.jpeg");
					dumpFile.write(bytes);
					dumpFile.close();
				}
			}
		} catch (IOException e) {
			log.error("writeCurrentVideoImage", e);
		}

		if (length < 0) {
			return readFakeVideoFrame();
		}

		return length;
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
	@HLEUnimplemented
	@HLEFunction(nid = 0x17F7B2FB, version = 271)
	public int sceUsbCamSetupVideo(pspUsbCamSetupVideoParam usbCamSetupVideoParam, TPointer workArea, int workAreaSize) {
		this.workArea = workArea.getAddress();
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

		return 0;
	}

	/**
	 * Sets if the image should be automatically reversed, depending of the position
	 * of the camera.
	 *
	 * @param on - 1 to set the automatical reversal of the image, 0 to set it off
	 *
	 * @return 0 on success, < 0 on error
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0xF93C4669, version = 271)
	public int sceUsbCamAutoImageReverseSW(boolean on) {
		autoImageReverseSW = on;

		return 0;
	}

	/**
	 * Starts video input from the camera.
	 *
	 * @return 0 on success, < 0 on error
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0x574A8C3F, version = 271)
	public int sceUsbCamStartVideo() {
		if (!setupVideo()) {
			log.warn(String.format("Cannot find webcam"));
		}

		return 0;
	}

	/**
	 * Stops video input from the camera.
	 *
	 * @return 0 on success, < 0 on error
	*/
	@HLEUnimplemented
	@HLEFunction(nid = 0x6CF32CB9, version = 271)
	public int sceUsbCamStopVideo() {
		// No parameters
		return 0;
	}

	@HLEFunction(nid = 0x03ED7A82, version = 271)
	public int sceUsbCamSetupMic(pspUsbCamSetupMicParam camSetupMicParam, TPointer workArea, int workAreaSize) {
		micFrequency = camSetupMicParam.frequency;
		micGain = camSetupMicParam.gain;

		return 0;
	}

	@HLELogging(level="info")
	@HLEFunction(nid = 0x82A64030, version = 271)
	public int sceUsbCamStartMic() {
		return 0;
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
	@HLEUnimplemented
	@HLEFunction(nid = 0x7DAC0C71, version = 271)
	public int sceUsbCamReadVideoFrameBlocking(TPointer jpegBuffer, int jpegBufferSize) {
		this.jpegBuffer = jpegBuffer;
		this.jpegBufferSize = jpegBufferSize;

		long now = Emulator.getClock().currentTimeMillis();
		int millisSinceLastFrame = (int) (now - lastVideoFrameMillis);
		int frameDurationMillis = getFramerateFrameDurationMillis();
		if (millisSinceLastFrame >= 0 && millisSinceLastFrame < frameDurationMillis) {
			int delayMillis = frameDurationMillis - millisSinceLastFrame;
			Modules.ThreadManForUserModule.hleKernelDelayThread(delayMillis * 1000, false);
			lastVideoFrameMillis = now + delayMillis;
		} else {
			lastVideoFrameMillis = now;
		}

		return writeCurrentVideoImage(jpegBuffer, jpegBufferSize);
	}

	/**
	 * Reads a video frame. The function returns immediately, and
	 * the completion has to be handled by calling sceUsbCamWaitReadVideoFrameEnd
	 * or sceUsbCamPollReadVideoFrameEnd.
	 *
	 * @param buf - The buffer that receives the frame jpeg data
	 * @param size - The size of the buffer.
	 *
	 * @return 0 on success, < 0 on error
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0x99D86281, version = 271)
	public int sceUsbCamReadVideoFrame(TPointer jpegBuffer, int jpegBufferSize) {
		this.jpegBuffer = jpegBuffer;
		this.jpegBufferSize = jpegBufferSize;

		return 0;
	}

	/**
	 * Polls the status of video frame read completion.
	 *
	 * @return the size of the acquired frame if it has been read,
	 * 0 if the frame has not yet been read, < 0 on error.
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0x41E73E95, version = 271)
	public int sceUsbCamPollReadVideoFrameEnd() {
		if (currentVideoFrameCount <= lastVideoFrameCount) {
			if (log.isDebugEnabled()) {
				log.debug(String.format("sceUsbCamPollReadVideoFrameEnd not frame end (%d - %d)", currentVideoFrameCount, lastVideoFrameCount));
			}
			return 0;
		}

		return writeCurrentVideoImage(jpegBuffer, jpegBufferSize);
	}

	/**
	 * Waits until the current frame has been read.
	 *
	 * @return the size of the acquired frame on success, < 0 on error
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0xF90B2293, version = 271)
	public int sceUsbCamWaitReadVideoFrameEnd() {
		return readFakeVideoFrame();
	}

	/**
	 * Gets the direction of the camera lens
	 *
	 * @return 1 if the camera is "looking to you", 0 if the camera
	 * is "looking to the other side".
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0x4C34F553, version = 271)
	public boolean sceUsbCamGetLensDirection() {
		return lensDirectionAtYou;
	}

	/**
	 * Setups the parameters to take a still image.
	 *
	 * @param param - pointer to a pspUsbCamSetupStillParam
	 *
	 * @return 0 on success, < 0 on error
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0x3F0CF289, version = 271)
	public int sceUsbCamSetupStill(pspUsbCamSetupStillParam usbCamSetupStillParam) {
		return 0;
	}

	/**
	 * Setups the parameters to take a still image (with more options)
	 *
	 * @param param - pointer to a pspUsbCamSetupStillParamEx
	 *
	 * @return 0 on success, < 0 on error
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0x0A41A298, version = 271)
	public int sceUsbCamSetupStillEx(pspUsbCamSetupStillExParam usbCamSetupStillExParam) {
		return 0;
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
	@HLEUnimplemented
	@HLEFunction(nid = 0x61BE5CAC, version = 271)
	public int sceUsbCamStillInputBlocking(TPointer buffer, int size) {
		return 0;
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
	@HLEUnimplemented
	@HLEFunction(nid = 0xFB0A6C5D, version = 271)
	public int sceUsbCamStillInput(TPointer buffer, int size) {
		return 0;
	}

	/**
	 * Waits until still input has been finished.
	 *
	 * @return the size of the acquired image on success, < 0 on error
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0x7563AFA1, version = 271)
	public int sceUsbCamStillWaitInputEnd() {
		return 0;
	}

	/**
	 * Polls the status of still input completion.
	 *
	 * @return the size of the acquired image if still input has ended,
	 * 0 if the input has not ended, < 0 on error.
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0x1A46CFE7, version = 271)
	public int sceUsbCamStillPollInputEnd() {
		return 0;
	}

	/**
	 * Cancels the still input.
	 *
	 * @return 0 on success, < 0 on error
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0xA720937C, version = 271)
	public int sceUsbCamStillCancelInput() {
		return 0;
	}

	/**
	 * Gets the size of the acquired still image.
	 *
	 * @return the size of the acquired image on success, < 0 on error
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0xE5959C36, version = 271)
	public int sceUsbCamStillGetInputLength() {
		return 0;
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
	@HLEUnimplemented
	@HLEFunction(nid = 0xCFE9E999, version = 271)
	public int sceUsbCamSetupVideoEx(pspUsbCamSetupVideoExParam usbCamSetupVideoExParam, TPointer workArea, int workAreaSize) {
		this.workArea = workArea.getAddress();
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

		return 0;
	}

	/**
	 * Gets the size of the acquired frame.
	 *
	 * @return the size of the acquired frame on success, < 0 on error
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0xDF9D0C92, version = 271)
	public int sceUsbCamGetReadVideoFrameSize() {
		return jpegBufferSize;
	}

	/**
	 * Sets the saturation
	 *
	 * @param saturation - The saturation (0-255)
	 *
	 * @return 0 on success, < 0 on error
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0x6E205974, version = 271)
	public int sceUsbCamSetSaturation(int saturation) {
		this.saturation = saturation;

		return 0;
	}

	/**
	 * Sets the brightness
	 *
	 * @param brightness - The brightness (0-255)
	 *
	 * @return 0 on success, < 0 on error
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0x4F3D84D5, version = 271)
	public int sceUsbCamSetBrightness(int brightness) {
		this.brightness = brightness;

		return 0;
	}

	/**
	 * Sets the contrast
	 *
	 * @param contrast - The contrast (0-255)
	 *
	 * @return 0 on success, < 0 on error
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0x09C26C7E, version = 271)
	public int sceUsbCamSetContrast(int contrast) {
		this.contrast = contrast;

		return 0;
	}

	/**
	 * Sets the sharpness
	 *
	 * @param sharpness - The sharpness (0-255)
	 *
	 * @return 0 on success, < 0 on error
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0x622F83CC, version = 271)
	public int sceUsbCamSetSharpness(int sharpness) {
		this.sharpness = sharpness;

		return 0;
	}

	/**
	 * Sets the image effect mode
	 *
	 * @param effectmode - The effect mode, one of ::PspUsbCamEffectMode
	 *
	 * @return 0 on success, < 0 on error
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0xD4876173, version = 271)
	public int sceUsbCamSetImageEffectMode(int imageEffectMode) {
		this.imageEffectMode = imageEffectMode;

		return 0;
	}

	/**
	 * Sets the exposure level
	 *
	 * @param ev - The exposure level, one of ::PspUsbCamEVLevel
	 *
	 * @return 0 on success, < 0 on error
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0x1D686870, version = 271)
	public int sceUsbCamSetEvLevel(int evLevel) {
		this.evLevel = evLevel;

		return 0;
	}

	/**
	 * Sets the reverse mode
	 *
	 * @param reverseflags - The reverse flags, zero or more of ::PspUsbCamReverseFlags
	 *
	 * @return 0 on success, < 0 on error
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0x951BEDF5, version = 271)
	public int sceUsbCamSetReverseMode(int reverseMode) {
		this.flip = (reverseMode & PSP_USBCAM_FLIP) != 0;
		this.mirror = (reverseMode & PSP_USBCAM_MIRROR) != 0;

		return 0;
	}

	/**
	 * Sets the zoom.
	 *
	 * @param zoom - The zoom level starting by 10. (10 = 1X, 11 = 1.1X, etc)
	 *
	 * @returns 0 on success, < 0 on error
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0xC484901F, version = 271)
	public int sceUsbCamSetZoom(int zoom) {
		this.zoom = zoom;

		return 0;
	}

	/**
	 * Gets the current saturation
	 *
	 * @param saturation - pointer to a variable that receives the current saturation
	 *
	 * @return 0 on success, < 0 on error
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0x383E9FA8, version = 271)
	public int sceUsbCamGetSaturation(TPointer32 saturationAddr) {
		saturationAddr.setValue(saturation);

		return 0;
	}

	/**
	 * Gets the current brightness
	 *
	 * @param brightness - pointer to a variable that receives the current brightness
	 *
	 * @return 0 on success, < 0 on error
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0x70F522C5, version = 271)
	public int sceUsbCamGetBrightness(TPointer32 brightnessAddr) {
		brightnessAddr.setValue(brightness);

		return 0;
	}

	/**
	 * Gets the current contrast
	 *
	 * @param contrast - pointer to a variable that receives the current contrast
	 *
	 * @return 0 on success, < 0 on error
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0xA063A957, version = 271)
	public int sceUsbCamGetContrast(TPointer32 contrastAddr) {
		contrastAddr.setValue(contrast);

		return 0;
	}

	/**
	 * Gets the current sharpness
	 *
	 * @param brightness - pointer to a variable that receives the current sharpness
	 *
	 * @return 0 on success, < 0 on error
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0xFDB68C23, version = 271)
	public int sceUsbCamGetSharpness(TPointer32 sharpnessAddr) {
		sharpnessAddr.setValue(sharpness);

		return 0;
	}

	/**
	 * Gets the current image efect mode
	 *
	 * @param effectmode - pointer to a variable that receives the current effect mode
	 *
	 * @return 0 on success, < 0 on error
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0x994471E0, version = 271)
	public int sceUsbCamGetImageEffectMode(TPointer32 imageEffectModeAddr) {
		imageEffectModeAddr.setValue(imageEffectMode);

		return 0;
	}

	/**
	 * Gets the current exposure level.
	 *
	 * @param ev - pointer to a variable that receives the current exposure level
	 *
	 * @return 0 on success, < 0 on error
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0x2BCD50C0, version = 271)
	public int sceUsbCamGetEvLevel(TPointer32 evLevelAddr) {
		evLevelAddr.setValue(evLevel);

		return 0;
	}

	/**
	 * Gets the current reverse mode.
	 *
	 * @param reverseflags - pointer to a variable that receives the current reverse mode flags
	 *
	 * @return 0 on success, < 0 on error
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0xD5279339, version = 271)
	public int sceUsbCamGetReverseMode(TPointer32 reverseModeAddr) {
		int reverseMode = 0;
		if (mirror) {
			reverseMode |= PSP_USBCAM_MIRROR;
		}
		if (flip) {
			reverseMode |= PSP_USBCAM_FLIP;
		}

		reverseModeAddr.setValue(reverseMode);

		return 0;
	}

	/**
	 * Gets the current zoom.
	 *
	 * @param zoom - pointer to a variable that receives the current zoom
	 *
	 * @return 0 on success, < 0 on error
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0x9E8AAF8D, version = 271)
	public int sceUsbCamGetZoom(TPointer32 zoomAddr) {
		zoomAddr.setValue(zoom);

		return 0;
	}

	/**
	 * Gets the state of the autoreversal of the image.
	 *
	 * @return 1 if it is set to automatic, 0 otherwise
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0x11A1F128, version = 271)
	public boolean sceUsbCamGetAutoImageReverseState() {
		return autoImageReverseSW;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x08AEE98A, version = 271)
	public int sceUsbCamSetMicGain() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x2E930264, version = 271)
	public int sceUsbCamSetupMicEx() {
		return 0;
	}

	@HLEFunction(nid = 0x36636925, version = 271)
	public int sceUsbCamReadMicBlocking(TPointer buffer, int bufferSize) {
		return Modules.sceAudioModule.hleAudioInputBlocking(bufferSize >> 1, micFrequency, buffer);
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x3DC0088E, version = 271)
	public int sceUsbCamReadMic(TPointer buffer, int bufferSize) {
		readMicBuffer = buffer;
		readMicBufferSize = bufferSize;

		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x41EE8797, version = 271)
	public int sceUsbCamUnregisterLensRotationCallback() {
		return 0;
	}

	@HLEFunction(nid = 0x5145868A, version = 271)
	public int sceUsbCamStopMic() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x5778B452, version = 271)
	public int sceUsbCamGetMicDataLength() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x6784E6A8, version = 271)
	public int sceUsbCamSetAntiFlicker() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xAA7D94BA, version = 271)
	public int sceUsbCamGetAntiFlicker() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xB048A67D, version = 271)
	public int sceUsbCamWaitReadMicEnd() {
		return Modules.sceAudioModule.hleAudioInputBlocking(readMicBufferSize >> 1, micFrequency, readMicBuffer);
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xD293A100, version = 271)
	public int sceUsbCamRegisterLensRotationCallback() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xF8847F60, version = 271)
	public int sceUsbCamPollReadMicEnd() {
		return 0;
	}
}
