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
package jpcsp.format.rco.sound;

import jpcsp.format.rco.object.BaseObject;
import jpcsp.format.rco.vsmx.interpreter.VSMXBaseObject;

public class Sound extends BaseObject {
	private int format;
	private int channels;
	private int[] channelSize;
	private int[] channelOffset;

	public Sound(int format, int channels, int[] channelSize, int[] channelOffset) {
		this.format = format;
		this.channels = channels;
		this.channelSize = channelSize;
		this.channelOffset = channelOffset;
	}

	public int getFormat() {
		return format;
	}

	public int getChannels() {
		return channels;
	}

	public int getChannelSize(int channel) {
		return channelSize[channel];
	}

	public int getChannelOffset(int channel) {
		return channelOffset[channel];
	}

	public void play(VSMXBaseObject object) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Sound.play"));
		}
	}
}
