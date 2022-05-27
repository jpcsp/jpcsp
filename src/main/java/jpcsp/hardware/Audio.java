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
package jpcsp.hardware;

import jpcsp.settings.AbstractBoolSettingsListener;
import jpcsp.settings.Settings;

public class Audio {
	public static final int PSP_AUDIO_VOLUME_MIN = 0;
	public static final int PSP_AUDIO_VOLUME_MAX = 0x8000;
	public static final int PSP_AUDIO_VOLUME_STEP = 0x100;
	private static int volume = PSP_AUDIO_VOLUME_MAX;
	private static boolean muted;
	private static AudioMutedSettingsListerner audioMutedSettingsListerner;

	private static class AudioMutedSettingsListerner extends AbstractBoolSettingsListener {
		@Override
		protected void settingsValueChanged(boolean value) {
			setMuted(value);
		}
	}

	public static int getVolume() {
		return volume;
	}

	public static void setVolume(int volume) {
		if (volume > PSP_AUDIO_VOLUME_MAX) {
			volume = PSP_AUDIO_VOLUME_MAX;
		} else if (volume < PSP_AUDIO_VOLUME_MIN) {
			volume = PSP_AUDIO_VOLUME_MIN;
		}

		Audio.volume = volume;
	}

	private static void init() {
		if (audioMutedSettingsListerner == null) {
			audioMutedSettingsListerner = new AudioMutedSettingsListerner();
			Settings.getInstance().registerSettingsListener("HardwareAudio", "emu.mutesound", audioMutedSettingsListerner);
		}
	}

	public static boolean isMuted() {
		init();
		return muted;
	}

	public static void setMuted(boolean muted) {
		init();
		Audio.muted = muted;
	}

	public static void setVolumeUp() {
		setVolume(volume + PSP_AUDIO_VOLUME_STEP);
	}

	public static void setVolumeDown() {
		setVolume(volume - PSP_AUDIO_VOLUME_STEP);
	}

	public static int getVolume(int volume) {
		if (isMuted()) {
			volume = 0;
		} else {
			volume = volume * getVolume() / PSP_AUDIO_VOLUME_MAX;
			if (volume < PSP_AUDIO_VOLUME_MIN) {
				volume = PSP_AUDIO_VOLUME_MIN;
			} else if (volume > PSP_AUDIO_VOLUME_MAX) {
				volume = PSP_AUDIO_VOLUME_MAX;
			}
		}

		return volume;
	}
}
