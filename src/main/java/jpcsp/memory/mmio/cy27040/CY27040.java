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
package jpcsp.memory.mmio.cy27040;

import java.io.IOException;

import org.apache.log4j.Logger;

import jpcsp.memory.mmio.MMIOHandlerAudio;
import jpcsp.state.IState;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

public class CY27040 implements IState {
	public static Logger log = Logger.getLogger("CY27040");
	private static final int STATE_VERSION = 0;
	private static CY27040 instance;
	// In clock register, used to manage the audio frequency
	public static final int PSP_CLOCK_AUDIO_FREQ = 0x01;
	// In clock register, used to enable/disable the lepton DSP
	public static final int PSP_CLOCK_LEPTON = 0x08;
	// In clock register, used to enable/disable audio
	public static final int PSP_CLOCK_AUDIO = 0x10;
	// Possible revisions: 3, 4, 7, 8, 9, 10 or 15
	private int revision;
	private int clock;
	private int spreadSpectrum;

	public static CY27040 getInstance() {
		if (instance == null) {
			instance = new CY27040();
		}
		return instance;
	}

	private CY27040() {
		resetInternally();
	}

    @Override
	public void read(StateInputStream stream) throws IOException {
    	stream.readVersion(STATE_VERSION);
    	revision = stream.readInt();
    	clock = stream.readInt();
    	spreadSpectrum = stream.readInt();

    	updateAudioFrequency();
    }

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(revision);
		stream.writeInt(clock);
		stream.writeInt(spreadSpectrum);
	}

	private void updateAudioFrequency() {
		MMIOHandlerAudio.getInstance().updateAudioFrequency();
	}

	public void reset() {
		resetInternally();
		updateAudioFrequency();
	}

	private void resetInternally() {
		revision = 0x04; // PSP Firmware 1.50 is only supporting revisions 4 or 8
		clock = 0;
		spreadSpectrum = 0;
	}

	public void executeTransmitReceiveCommand(int[] transmitData, int[] receiveData) {
		int command = transmitData[0] & 0xFF;

		if (log.isDebugEnabled()) {
			log.debug(String.format("executeTransmitReceiveCommand command=0x%02X on %s", command, this));
		}

		switch (command) {
			case 0x00: // Retrieve all the registers
				receiveData[0] = 3; // Returning 3 register values
				receiveData[1] = revision;
				receiveData[2] = clock;
				receiveData[3] = spreadSpectrum;
				break;
			case 0x80:
				receiveData[0] = revision;
				break;
			case 0x81:
				receiveData[0] = clock;
				break;
			case 0x82:
				receiveData[0] = spreadSpectrum;
				break;
			default:
				log.error(String.format("executeTransmitReceiveCommand unknown command 0x%X", command));
				break;
		}
	}

	public void executeTransmitCommand(int[] transmitData) {
		int command = transmitData[0] & 0xFF;
		switch (command) {
			case 0x80:
				revision = transmitData[1] & 0xFF;
				break;
			case 0x81:
				clock = transmitData[1] & 0xFF;
				updateAudioFrequency();
				break;
			case 0x82:
				spreadSpectrum = transmitData[1] & 0xFF;
				break;
			default:
				log.error(String.format("executeTransmitCommand unknown command 0x%X", command));
				break;
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("executeTransmitCommand command=0x%02X on %s", command, this));
		}
	}

	public int getAudioFreq() {
		return (clock & PSP_CLOCK_AUDIO_FREQ) == 0 ? 44100 : 48000;
	}

	private boolean isAudioEnabled() {
		return (clock & PSP_CLOCK_AUDIO) != 0;
	}

	private boolean isLeptonEnabled() {
		return (clock & PSP_CLOCK_LEPTON) != 0;
	}

	private String toString(boolean enabled) {
		return enabled ? "enabled" : "disabled";
	}

	private String toStringClock() {
		return String.format("%d, audio %s, lepton %s", getAudioFreq(), toString(isAudioEnabled()), toString(isLeptonEnabled()));
	}

	@Override
	public String toString() {
		return String.format("CY27040 revision=0x%02X, clock=0x%02X(%s), spreadSpectrum=0x%02X", revision, clock, toStringClock(), spreadSpectrum);
	}
}
