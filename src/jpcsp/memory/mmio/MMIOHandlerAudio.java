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

import static jpcsp.HLE.kernel.managers.IntrManager.PSP_AUDIO_INTR;

import org.apache.log4j.Logger;

import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.modules.sceAudio;
import jpcsp.memory.mmio.audio.AudioLine;

public class MMIOHandlerAudio extends MMIOHandlerBase {
	public static Logger log = sceAudio.log;
	public static final int AUDIO_HW_FREQUENCY_8000 = 0x01;
	public static final int AUDIO_HW_FREQUENCY_11025 = 0x02;
	public static final int AUDIO_HW_FREQUENCY_12000 = 0x04;
	public static final int AUDIO_HW_FREQUENCY_16000 = 0x08;
	public static final int AUDIO_HW_FREQUENCY_22050 = 0x10;
	public static final int AUDIO_HW_FREQUENCY_24000 = 0x20;
	public static final int AUDIO_HW_FREQUENCY_32000 = 0x40;
	public static final int AUDIO_HW_FREQUENCY_44100 = 0x80;
	public static final int AUDIO_HW_FREQUENCY_48000 = 0x100;
	private int busy;
	private int interrupt;
	private int inProgress;
	private int flags10;
	private int flags20;
	private int flags24;
	private int flags2C;
	private int volume;
	private int frequency0;
	private int frequency1;
	private int frequencyFlags;
	private int hardwareFrequency;
	private final int audioData0[] = new int[24 + (256/4)];
	private final int audioData1[] = new int[24 + (256/4)];
	private int audioDataIndex0;
	private int audioDataIndex1;
	private final AudioLine audioLines[] = new AudioLine[2];

	public MMIOHandlerAudio(int baseAddress) {
		super(baseAddress);

    	for (int i = 0; i < audioLines.length; i++) {
    		audioLines[i] = new AudioLine();
		}
	}

	private void setFlags24(int flags24) {
		this.flags24 = flags24;
		interrupt &= flags24;

		checkInterrupt();
	}

	private void checkInterrupt() {
		if (interrupt != 0) {
			RuntimeContextLLE.triggerInterrupt(getProcessor(), PSP_AUDIO_INTR);
		} else {
			RuntimeContextLLE.clearInterrupt(getProcessor(), PSP_AUDIO_INTR);
		}
	}

	private int sendAudioData(int value, int line, int index, int[] data, int interrupt) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("sendAudioData value=0x%08X, line=%d, index=0x%X, interrupt=%d", value, line, index, interrupt));
		}
		data[index++] = value;

		if (index >= data.length) {
			audioLines[line].writeAudioData(data, 0, index);

			if (log.isTraceEnabled()) {
				log.trace(String.format("sendAudioData line#%d:", line));
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
			index = 0;
		}

		return index;
	}

	private void startAudio(int flags) {
		if ((flags & 0x1) == 0) {
			audioDataIndex0 = 0;
			inProgress |= 1;
		}
		if ((flags & 0x2) == 0) {
			audioDataIndex1 = 0;
			inProgress |= 2;
		}
	}

	private void stopAudio(int flags) {
		if ((inProgress & 0x1) != 0 && (flags & 0x1) == 0) {
			inProgress &= ~0x1;
		}
		if ((inProgress & 0x2) != 0 && (flags & 0x2) == 0) {
			inProgress &= ~0x2;
		}
		if ((inProgress & 0x4) != 0 && (flags & 0x4) == 0) {
			inProgress &= ~0x4;
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
		updateAudioLineFrequency();
	}

	private void setFrequency1(int frequency1) {
		this.frequency1 = frequency1;
		updateAudioLineFrequency();
	}

	private void setHardwareFrequency(int hardwareFrequency) {
		this.hardwareFrequency = hardwareFrequency;
		updateAudioLineFrequency();
	}

	private void updateAudioLineFrequency() {
		// TODO In the VSH, only frequency0 is being set. When to use frequency1 and hardwareFrequency?
		int frequency = getFrequencyValue(frequency0);
		for (int i = 0; i < audioLines.length; i++) {
			audioLines[i].setFrequency(frequency);
		}
	}

	private void setVolume(int volume) {
		this.volume = volume;
		updateAudioLineVolume();
	}

	private void updateAudioLineVolume() {
		for (int i = 0; i < audioLines.length; i++) {
			audioLines[i].setVolume(volume);
		}
	}

	@Override
	public int read32(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x00: value = busy; break;
			case 0x0C: value = inProgress; break;
			case 0x1C: value = interrupt; break;
			case 0x28: value = 0x37; break; // flags when some actions are completed?
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
			case 0x24: setFlags24(value); break;
			case 0x2C: flags2C = value; break;
			case 0x38: setFrequency0(value); break;
			case 0x3C: setFrequency1(value); break;
			case 0x40: frequencyFlags = value; break;
			case 0x44: setHardwareFrequency(value); break;
			case 0x50: setVolume(value); break;
			case 0x60: audioDataIndex0 = sendAudioData(value, 0, audioDataIndex0, audioData0, 1); break;
			case 0x70: audioDataIndex1 = sendAudioData(value, 1, audioDataIndex1, audioData1, 2); break;
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}

	@Override
	public String toString() {
		return String.format("busy=0x%X, interrupt=0x%X, inProgress=0x%X, flags10=0x%X, flags20=0x%X, flags24=0x%X, flags2C=0x%X, volume=0x%X, frequency0=0x%X(%d), frequency1=0x%X(%d), frequencyFlags=0x%X, hardwareFrequency=0x%X(%d)", busy, interrupt, inProgress, flags10, flags20, flags24, flags2C, volume, frequency0, getFrequencyValue(frequency0), frequency1, getFrequencyValue(frequency1), frequencyFlags, hardwareFrequency, getFrequencyValue(hardwareFrequency));
	}
}
