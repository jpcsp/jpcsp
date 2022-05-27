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
package jpcsp.memory.mmio;

import static jpcsp.Allegrex.compiler.RuntimeContext.setLog4jMDC;
import static jpcsp.Allegrex.compiler.RuntimeContextLLE.getMainProcessor;
import static jpcsp.Allegrex.compiler.RuntimeContextLLE.getMediaEngineProcessor;
import static jpcsp.Emulator.getClock;
import static jpcsp.HLE.kernel.managers.IntrManager.PSP_AUDIO_INTR;
import static jpcsp.HLE.kernel.managers.IntrManager.PSP_I2C_INTR;
import static jpcsp.memory.mmio.MMIOHandlerSystemControl.SYSREG_CLK_AUDIO_CLKOUT;
import static jpcsp.util.Utilities.clearBit;
import static jpcsp.util.Utilities.clearFlag;
import static jpcsp.util.Utilities.hasBit;
import static jpcsp.util.Utilities.isFallingBit;
import static jpcsp.util.Utilities.notHasBit;
import static jpcsp.util.Utilities.setBit;
import static jpcsp.util.Utilities.setFlag;

import java.io.IOException;

import org.apache.log4j.Logger;

import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.modules.sceAudio;
import jpcsp.hardware.Audio;
import jpcsp.memory.mmio.audio.AudioLine;
import jpcsp.memory.mmio.cy27040.CY27040;
import jpcsp.sound.SoundChannel;
import jpcsp.state.IState;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;
import jpcsp.util.Utilities;

public class MMIOHandlerAudio extends MMIOHandlerBase {
	public static Logger log = sceAudio.log;
	private static final int STATE_VERSION = 0;
	public static final int BASE_ADDRESS = 0xBE000000;
	public static final int AUDIO_HW_FREQUENCY_8000 = 0x01;
	public static final int AUDIO_HW_FREQUENCY_11025 = 0x02;
	public static final int AUDIO_HW_FREQUENCY_12000 = 0x04;
	public static final int AUDIO_HW_FREQUENCY_16000 = 0x08;
	public static final int AUDIO_HW_FREQUENCY_22050 = 0x10;
	public static final int AUDIO_HW_FREQUENCY_24000 = 0x20;
	public static final int AUDIO_HW_FREQUENCY_32000 = 0x40;
	public static final int AUDIO_HW_FREQUENCY_44100 = 0x80;
	public static final int AUDIO_HW_FREQUENCY_48000 = 0x100;
	private static final int BUFFER_SIZE_IN_MILLIS = 100;
	private static MMIOHandlerAudio instance;
	private int busy;
	private int interrupt;
	private int inProgress;
	private int flags10;
	private int flags20;
	private int interruptEnabled;
	private int flags28 = 0x37;
	private int flags2C;
	private int volume;
	private int frequency0;
	private int frequency1;
	private int frequencyFlags;
	private int hardwareFrequency;
	private final AudioLineState audioLineStates[] = new AudioLineState[2];

	private class AudioLineState implements IState {
		private static final int STATE_VERSION = 0;
		private static final int STARTUP_COUNT = 10;
		private final int lineNumber;
		private AudioLine audioLine = new AudioLine();
		private final int data[] = new int[64];
		private int dataIndex;
		private int numberBlockingBufferSamples;
		private boolean stalled;
		private int startup;
		private int frequency;

		public AudioLineState(int lineNumber) {
			this.lineNumber = lineNumber;

			setStalled();

			updateNumberBlockingBufferSamples();
		}

		@Override
		public synchronized void read(StateInputStream stream) throws IOException {
			stream.readVersion(STATE_VERSION);
			audioLine.read(stream);
			stream.readInts(data);
			dataIndex = stream.readInt();
			stalled = stream.readBoolean();
			frequency = stream.readInt();

			audioLine.setFrequency(frequency);
			updateNumberBlockingBufferSamples();

			// The audio is actually stalled when reloading a state
			setStalled();
		}

		@Override
		public synchronized void write(StateOutputStream stream) throws IOException {
			stream.writeVersion(STATE_VERSION);
			audioLine.write(stream);
			stream.writeInts(data);
			stream.writeInt(dataIndex);
			stream.writeBoolean(stalled);
			stream.writeInt(frequency);
		}

		private void setStalled() {
			dataIndex = 0;
			stalled = true;
			startup = STARTUP_COUNT;
		}

		public synchronized void poll() {
			if (hasBit(interruptEnabled, lineNumber)) {
				int waitingBufferSamples = audioLine.getWaitingBufferSamples();
				if (log.isTraceEnabled()) {
					log.trace(String.format("poll waitingBufferSamples=0x%X on %s", waitingBufferSamples, this));
				}

				if (waitingBufferSamples <= 0 && dataIndex == 0) {
					setInterruptBit(lineNumber);
					setStalled();
				}
			}
		}

		private synchronized void flushAudioData() {
			if (dataIndex <= 0) {
				return;
			}

			if (log.isTraceEnabled()) {
				log.trace(String.format("sendAudioData line#%d:", lineNumber));
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < data.length; i++) {
					if (sb.length() > 0) {
						sb.append(" ");
					} else {
						sb.append("    ");
					}

					sb.append(String.format("0x%04X 0x%04X", data[i] & 0xFFFF, data[i] >>> 16));

					if (((i + 1) % 4) == 0) {
						log.trace(sb.toString());
						sb.setLength(0);
					}
				}
			}

			audioLine.writeAudioData(data, 0, dataIndex);
			dataIndex = 0;
		}

		private synchronized void sendAudioData(int value) {
			if (log.isTraceEnabled()) {
				log.trace(String.format("sendAudioData value=0x%08X, %s", value, toString()));
			}

			if (Audio.isMuted()) {
				value = 0;
			}

			data[dataIndex++] = value;

			// When restarting in a stalled state, the PSP is writing
			// 24 audio samples with value 0. They can be ignored.
			if (stalled) {
				if (dataIndex == 24) {
					if (log.isTraceEnabled()) {
						log.trace(String.format("sendAudioData leaving stalled state after discarding %d data values", dataIndex));
					}
					stalled = false;
					dataIndex = 0;
				} else if (value != 0) {
					log.warn(String.format("sendAudioData unknown audio data 0x%08X in stalled state, %s", value, toString()));
				}
			}

			if (dataIndex >= data.length) {
				flushAudioData();
			}
		}

		public synchronized void startAudio() {
			dataIndex = 0;
		}

		private void updateNumberBlockingBufferSamples() {
			numberBlockingBufferSamples = Math.round(audioLine.getFrequency() * BUFFER_SIZE_IN_MILLIS / 1000.f);
			if (log.isDebugEnabled()) {
				log.debug(String.format("number of blocking buffer samples=0x%X", numberBlockingBufferSamples));
			}
		}

		public synchronized void setFrequency(int frequency) {
			if (this.frequency != frequency) {
				this.frequency = frequency;
				audioLine.setFrequency(frequency);
				updateNumberBlockingBufferSamples();
			}
		}

		public synchronized void setVolume(int volume) {
			audioLine.setVolume(volume);
		}

		public synchronized int getDmacSyncDelay(int size) {
			int syncDelay = 0;

			int waitingBufferSamples = audioLine.getWaitingBufferSamples();
			// Do not delay the Dmac copy when the clock is paused (e.g. when compiling)
			if (waitingBufferSamples >= numberBlockingBufferSamples && !getClock().isPaused()) {
				int numberSamples = size >> 2;
				int frequency = audioLine.getFrequency();
				// Use "long" for calculation to avoid "int" overflows
				syncDelay = (int) (1000000L * numberSamples / frequency);
			} else if (startup > 0) {
				syncDelay = 5;
				startup--;
			}

			if (log.isDebugEnabled()) {
				log.debug(String.format("syncDelay=0x%X milliseconds, waitingBufferSamples=0x%X", syncDelay, waitingBufferSamples));
			}

			return syncDelay;
		}

		public synchronized void dmacFlush() {
			flushAudioData();
		}

		public synchronized void reset() {
			setStalled();
		}

		@Override
		public String toString() {
			return String.format("line#%d, dataIndex=0x%X, stalled=%b, numberBlockingBuffers=%d, waitingBufferSamples=0x%X", lineNumber, dataIndex, stalled, numberBlockingBufferSamples, audioLine.getWaitingBufferSamples());
		}
	}

	private class PollAudioLinesThread extends Thread implements IAction {
		private volatile boolean exit;
		private volatile boolean end;

		@Override
		public void run() {
			setLog4jMDC();
			SoundChannel.setThreadInitContext();
			RuntimeContextLLE.registerExitAction(this);

			while (!exit) {
				try {
			    	for (int i = 0; i < audioLineStates.length && !exit; i++) {
			    		audioLineStates[i].poll();
					}

			    	if (!exit) {
			    		Utilities.sleep(10, 0);
			    	}
				} catch (UnsatisfiedLinkError e) {
					exit = true;
				}
			}

			SoundChannel.clearThreadInitContext();

			end = true;
		}

		public void exit() {
			exit = true;
			while (!end) {
				// Active polling as this will terminate very quickly
			}
		}

		@Override
		public void execute() {
			exit();
		}
	}

	public static MMIOHandlerAudio getInstance() {
		if (instance == null) {
			instance = new MMIOHandlerAudio(BASE_ADDRESS);
		}

		return instance;
	}

	private MMIOHandlerAudio(int baseAddress) {
		super(baseAddress);

		SoundChannel.init();
    	for (int i = 0; i < audioLineStates.length; i++) {
    		audioLineStates[i] = new AudioLineState(i);
		}

    	PollAudioLinesThread pollAudioLinesThread = new PollAudioLinesThread();
    	pollAudioLinesThread.setDaemon(true);
    	pollAudioLinesThread.setName("Poll Audio Lines Thread");
    	pollAudioLinesThread.start();
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		busy = stream.readInt();
		interrupt = stream.readInt();
		inProgress = stream.readInt();
		flags10 = stream.readInt();
		flags20 = stream.readInt();
		interruptEnabled = stream.readInt();
		flags28 = stream.readInt();
		flags2C = stream.readInt();
		volume = stream.readInt();
		frequency0 = stream.readInt();
		frequency1 = stream.readInt();
		frequencyFlags = stream.readInt();
		hardwareFrequency = stream.readInt();
		for (int i = 0; i < audioLineStates.length; i++) {
			audioLineStates[i].read(stream);
		}
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(busy);
		stream.writeInt(interrupt);
		stream.writeInt(inProgress);
		stream.writeInt(flags10);
		stream.writeInt(flags20);
		stream.writeInt(interruptEnabled);
		stream.writeInt(flags28);
		stream.writeInt(flags2C);
		stream.writeInt(volume);
		stream.writeInt(frequency0);
		stream.writeInt(frequency1);
		stream.writeInt(frequencyFlags);
		stream.writeInt(hardwareFrequency);
		for (int i = 0; i < audioLineStates.length; i++) {
			audioLineStates[i].write(stream);
		}
		super.write(stream);
	}

	@Override
	public void reset() {
		super.reset();

		busy = 0;
		interrupt = 0;
		inProgress = 0;
		flags10 = 0;
		flags20 = 0;
		interruptEnabled = 0;
		flags28 = 0x37; // flags when some actions are completed?
		flags2C = 0;
		volume = 0;
		frequency0 = 0;
		frequency1 = 0;
		frequencyFlags = 0;
		hardwareFrequency = 0;
		for (int i = 0; i < audioLineStates.length; i++) {
			audioLineStates[i].reset();
		}
	}

	public int getDmacSyncDelay(int address, int size) {
		int syncDelay;

		switch (address - baseAddress) {
			case 0x60: syncDelay = audioLineStates[0].getDmacSyncDelay(size); break;
			case 0x70: syncDelay = audioLineStates[1].getDmacSyncDelay(size); break;
			default:
				log.error(String.format("getDmacSyncDelay unimplemented address=0x%08X, size=0x%X", address, size));
				syncDelay = 0;
				break;
		}

		return syncDelay;
	}

	public void dmacFlush(int address) {
		switch (address - baseAddress) {
			case 0x60: audioLineStates[0].dmacFlush(); break;
			case 0x70: audioLineStates[1].dmacFlush(); break;
			default:
				log.error(String.format("dmacFlush unimplemented address=0x%08X", address));
				break;
		}
	}

	private void setInterruptBit(int bit) {
		if (!hasBit(interrupt, bit)) {
			interrupt = setBit(interrupt, bit);

			checkInterrupt();
		}
	}

	private void setInterruptEnabled(int interruptEnabled) {
		this.interruptEnabled = interruptEnabled;

		int oldInterrupt = interrupt;
		interrupt &= interruptEnabled;
		if (oldInterrupt != interrupt) {
			checkInterrupt();
		}
	}

	private void checkInterrupt() {
		if (interrupt != 0) {
			if (log.isDebugEnabled()) {
				log.debug("Triggering interrupt PSP_AUDIO_INTR");
			}

			RuntimeContextLLE.triggerInterrupt(getMainProcessor(), PSP_AUDIO_INTR);
			// This is triggering a different interrupt bit on the ME
			RuntimeContextLLE.triggerInterrupt(getMediaEngineProcessor(), PSP_I2C_INTR);
		} else {
			RuntimeContextLLE.clearInterrupt(getMainProcessor(), PSP_AUDIO_INTR);
			RuntimeContextLLE.clearInterrupt(getMediaEngineProcessor(), PSP_I2C_INTR);
		}
	}

	private void startAudio(int flags) {
		for (int i = 0; i < audioLineStates.length; i++) {
			if (notHasBit(flags, i)) {
				audioLineStates[i].startAudio();
				inProgress = setBit(inProgress, i);
			}
		}
	}

	private void stopAudio(int flags) {
		for (int i = 0; i < 3; i++) {
			if (isFallingBit(inProgress, flags, i)) {
				inProgress = clearBit(inProgress, i);
				interrupt = clearBit(interrupt, i);
			}
		}
	}

	private static int getFrequencyValue(int hwFrequency) {
		switch (hwFrequency) {
			case AUDIO_HW_FREQUENCY_8000 : return  8000;
			case AUDIO_HW_FREQUENCY_11025: return 11025;
			case AUDIO_HW_FREQUENCY_12000: return 12000;
			case AUDIO_HW_FREQUENCY_16000: return 16000;
			case AUDIO_HW_FREQUENCY_22050: return 22050;
			case AUDIO_HW_FREQUENCY_24000: return 24000;
			case AUDIO_HW_FREQUENCY_32000: return 32000;
			case AUDIO_HW_FREQUENCY_44100: return 44100;
			case AUDIO_HW_FREQUENCY_48000: return 48000;
		}
		return hwFrequency;
	}

	private void setFrequency0(int frequency0) {
		this.frequency0 = frequency0;
		updateAudioFrequency();
	}

	private void setFrequency1(int frequency1) {
		this.frequency1 = frequency1;
		updateAudioFrequency();
	}

	private void setHardwareFrequency(int hardwareFrequency) {
		this.hardwareFrequency = hardwareFrequency;
		updateAudioFrequency();
	}

	public void updateAudioFrequency() {
		// The frequency controlling the audio output is taken from the CY27040 controller
		int frequency = CY27040.getInstance().getAudioFreq();
		for (int i = 0; i < audioLineStates.length; i++) {
			audioLineStates[i].setFrequency(frequency);
		}
	}

	private void setVolume(int volume) {
		this.volume = volume;
		updateAudioLineVolume();
	}

	private void updateAudioLineVolume() {
		for (int i = 0; i < audioLineStates.length; i++) {
			audioLineStates[i].setVolume(volume);
		}
	}

	private void updateFlags28() {
		if (MMIOHandlerSystemControl.getInstance().isClockDeviceEnabled(SYSREG_CLK_AUDIO_CLKOUT)) {
			flags28 = clearFlag(flags28, 0x2);
		} else {
			flags28 = setFlag(flags28, 0x2);
		}
	}

	private int getFlags28() {
		updateFlags28();

		return flags28;
	}

	@Override
	public int read32(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x00: value = busy; break;
			case 0x0C: value = inProgress; break;
			case 0x1C: value = interrupt; break;
			case 0x28: value = getFlags28(); break;
			case 0x40: value = frequencyFlags; break;
			case 0x50: value = 0; break; // This doesn't seem to return the volume value
			default: value = super.read32(address); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - read32(0x%08X) returning 0x%08X", getPc(), address, value));
		}

		return value;
	}

	@Override
	public void write32(int address, int value) {
		switch (address - baseAddress) {
			case 0x00: busy = value; break;
			case 0x04: stopAudio(value); break;
			case 0x08: startAudio(value); break;
			case 0x10: flags10 = value; break;
			case 0x14: if (value != 0x1208) { super.write32(address, value); } break;
			case 0x18: if (value != 0x0) { super.write32(address, value); } break;
			case 0x20: flags20 = value; break;
			case 0x24: setInterruptEnabled(value); break;
			case 0x2C: flags2C = value; break;
			case 0x38: setFrequency0(value); break;
			case 0x3C: setFrequency1(value); break;
			case 0x40: frequencyFlags = value; break;
			case 0x44: setHardwareFrequency(value); break;
			case 0x50: setVolume(value); break;
			case 0x60: audioLineStates[0].sendAudioData(value); break;
			case 0x70: audioLineStates[1].sendAudioData(value); break;
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}

	@Override
	public String toString() {
		return String.format("busy=0x%X, interrupt=0x%X, inProgress=0x%X, flags10=0x%X, flags20=0x%X, interruptEnabled=0x%X, flags2C=0x%X, volume=0x%X, frequency0=0x%X(%d), frequency1=0x%X(%d), frequencyFlags=0x%X, hardwareFrequency=0x%X(%d)", busy, interrupt, inProgress, flags10, flags20, interruptEnabled, flags2C, volume, frequency0, getFrequencyValue(frequency0), frequency1, getFrequencyValue(frequency1), frequencyFlags, hardwareFrequency, getFrequencyValue(hardwareFrequency));
	}
}
