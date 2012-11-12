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
package jpcsp.HLE.modules260;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import org.apache.log4j.Logger;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALC11;
import org.lwjgl.openal.ALCdevice;

import jpcsp.Emulator;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.pspUsbMicInputInitExParam;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules150.ThreadManForUser;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryWriter;
import jpcsp.util.Utilities;

@HLELogging
public class sceUsbMic extends HLEModule {
	public static Logger log = Modules.getLogger("sceUsbMic");
	protected ALCdevice device;
	protected ByteBuffer captureBuffer;
	protected IntBuffer samplesBuffer;

	@Override
	public String getName() {
		return "sceUsbMic";
	}

	@Override
	public void stop() {
		if (device != null) {
			ALC11.alcCaptureCloseDevice(device);
			device = null;
		}
		captureBuffer = null;

		super.stop();
	}

	protected static class MicBlockingInputAction implements IAction {
		private int threadId;
		private int addr;
		private int samples;
		private int frequency;

		public MicBlockingInputAction(int threadId, int addr, int samples, int frequency) {
			this.threadId = threadId;
			this.addr = addr;
			this.samples = samples;
			this.frequency = frequency;
		}

		@Override
		public void execute() {
			Modules.sceUsbMicModule.hleMicBlockingInput(threadId, addr, samples, frequency);
		}
	}

	public void hleMicBlockingInput(int threadId, int addr, int samples, int frequency) {
		int availableSamples = getAvailableSamples();
		if (log.isTraceEnabled()) {
			log.trace(String.format("hleMicBlockingInput available samples: %d from %d", availableSamples, samples));
		}

		if (availableSamples >= samples) {
			int bufferBytes = samples << 1;
			if (captureBuffer == null || captureBuffer.capacity() < bufferBytes) {
				captureBuffer = ByteBuffer.allocateDirect(bufferBytes).order(ByteOrder.LITTLE_ENDIAN);
			} else {
				captureBuffer.rewind();
			}

			ALC11.alcCaptureSamples(device, captureBuffer, samples);

			captureBuffer.rewind();
			IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(addr, samples, 2);
			for (int i = 0; i < samples; i++) {
				short sample = captureBuffer.getShort();
				memoryWriter.writeNext(sample & 0xFFFF);
			}

			if (log.isTraceEnabled()) {
				log.trace(String.format("hleMicBlockingInput returning %d samples: %s", samples, Utilities.getMemoryDump(addr, bufferBytes, 2, 16)));
			}
			Modules.ThreadManForUserModule.hleUnblockThread(threadId);
		} else {
			blockThreadInput(threadId, addr, samples, frequency, availableSamples);
		}
	}

	protected int getAvailableSamples() {
		if (samplesBuffer == null) {
			samplesBuffer = ByteBuffer.allocateDirect(4).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
		}

		ALC10.alcGetInteger(device, ALC11.ALC_CAPTURE_SAMPLES, samplesBuffer);

		return samplesBuffer.get(0);
	}

	protected int getUnblockInputDelayMicros(int availableSamples, int samples, int frequency) {
		if (availableSamples >= samples) {
			return 0;
		}

		int missingSamples = samples - availableSamples;
		int delayMicros = (int) (missingSamples * 1000000L / frequency);

		return delayMicros;
	}

	protected void blockThreadInput(int addr, int samples, int frequency) {
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        int threadId = threadMan.getCurrentThreadID();
		threadMan.hleBlockCurrentThread();
		blockThreadInput(threadId, addr, samples, frequency, getAvailableSamples());
	}

	protected void blockThreadInput(int threadId, int addr, int samples, int frequency, int availableSamples) {
		int delayMicros = getUnblockInputDelayMicros(availableSamples, samples, frequency);
		if (log.isTraceEnabled()) {
			log.trace(String.format("blockThreadInput waiting %d micros", delayMicros));
		}
		Emulator.getScheduler().addAction(Emulator.getClock().microTime() + delayMicros, new MicBlockingInputAction(threadId, addr, samples, frequency));
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x06128E42, version = 260)
	public int sceUsbMicPollInputEnd() {
		return 0;
	}

	@HLEFunction(nid = 0x2E6DCDCD, version = 260)
	public int sceUsbMicInputBlocking(int maxSamples, int frequency, TPointer buffer) {
		if (maxSamples <= 0 || (maxSamples & 0x3F) != 0) {
			return SceKernelErrors.ERROR_USBMIC_INVALID_MAX_SAMPLES;
		}

		if (frequency != 44100 && frequency != 22050 && frequency != 11025) {
			return SceKernelErrors.ERROR_USBMIC_INVALID_FREQUENCY;
		}

		if (device == null) {
			device = ALC11.alcCaptureOpenDevice(null, frequency, AL10.AL_FORMAT_MONO16, 10 * 1024);
			ALC11.alcCaptureStart(device);
		}

		blockThreadInput(buffer.getAddress(), maxSamples, frequency);

		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x45310F07, version = 260)
	public int sceUsbMicInputInitEx(@CanBeNull pspUsbMicInputInitExParam param) {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x5F7F368D, version = 260)
	public int sceUsbMicInput() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x63400E20, version = 260)
	public int sceUsbMicGetInputLength() {
		return getAvailableSamples();
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xB8E536EB, version = 260)
	public int sceUsbMicInputInit() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xF899001C, version = 260)
	public int sceUsbMicWaitInputEnd() {
		return 0;
	}
}
