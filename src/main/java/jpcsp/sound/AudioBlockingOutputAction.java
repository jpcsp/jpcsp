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
package jpcsp.sound;

import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.IAction;

public class AudioBlockingOutputAction implements IAction {
	private int threadId;
	private SoundChannel soundChannel;
	private int addr;
	private int leftVolume;
	private int rightVolume;

	public AudioBlockingOutputAction(int threadId, SoundChannel soundChannel, int addr, int leftVolume, int rightVolume) {
		this.threadId = threadId;
		this.soundChannel = soundChannel;
		this.addr = addr;
		this.leftVolume = leftVolume;
		this.rightVolume = rightVolume;
	}

	@Override
	public void execute() {
		Modules.sceAudioModule.hleAudioBlockingOutput(threadId, soundChannel, addr, leftVolume, rightVolume);
	}
}
