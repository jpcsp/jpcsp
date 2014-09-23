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
package jpcsp.GUI;

import static jpcsp.HLE.modules150.IoFileMgrForUser.PSP_O_RDONLY;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.VFS.IVirtualFileSystem;
import jpcsp.HLE.VFS.iso.UmdIsoVirtualFileSystem;
import jpcsp.HLE.modules150.sceAtrac3plus;
import jpcsp.HLE.modules150.sceAtrac3plus.AtracFileInfo;
import jpcsp.filesystems.umdiso.UmdIsoReader;
import jpcsp.media.codec.CodecFactory;
import jpcsp.media.codec.ICodec;
import jpcsp.util.Utilities;

public class UmdBrowserSound {
	private boolean done;
	private boolean threadExit;
	private Memory mem;
	private int samplesAddr = MemoryMap.START_USERSPACE;
	private int inputAddr = MemoryMap.START_USERSPACE + 0x10000;
	private int inputLength;
	private int inputPosition;
	private int inputOffset;
	private int inputBytesPerFrame;
	private int channels;
	private ICodec codec;
    private SourceDataLine mLine;

	private class SoundPlayThread extends Thread {
		@Override
		public void run() {
			while (!done) {
				stepSound();
			}
			threadExit = true;
		}
	}

	public UmdBrowserSound(Memory mem, UmdIsoReader iso, String fileName) {
		this.mem = mem;

		if (read(iso, fileName)) {
			Thread soundPlayThread = new SoundPlayThread();
			soundPlayThread.setDaemon(true);
			soundPlayThread.setName("Umd Browser Sound Play Thread");
			soundPlayThread.start();
		} else {
			threadExit = true;
		}
	}

	public void stopSound() {
		done = true;
		while (!threadExit) {
			Utilities.sleep(1, 0);
		}

		if (mLine != null) {
			mLine.close();
		}
	}

	private boolean read(UmdIsoReader iso, String fileName) {
		IVirtualFileSystem vfs = new UmdIsoVirtualFileSystem(iso);
		IVirtualFile vFile = vfs.ioOpen(fileName, PSP_O_RDONLY, 0);
		if (vFile == null) {
			return false;
		}

		inputLength = (int) vFile.length();
		inputLength = vFile.ioRead(new TPointer(mem, inputAddr), inputLength);
		vfs.ioClose(vFile);
		if (inputLength <= 0) {
			return false;
		}

		AtracFileInfo atracFileInfo = new AtracFileInfo();
		int codecType = sceAtrac3plus.analyzeRiffFile(mem, inputAddr, inputLength, atracFileInfo);
		if (codecType < 0) {
			return false;
		}

		codec = CodecFactory.getCodec(codecType);
		if (codec == null) {
			return false;
		}

		int result = codec.init(atracFileInfo.atracBytesPerFrame, atracFileInfo.atracChannels, atracFileInfo.atracChannels, atracFileInfo.atracCodingMode);
		if (result < 0) {
			return false;
		}

        AudioFormat audioFormat = new AudioFormat(44100,
                16,
                atracFileInfo.atracChannels,
                true,
                false);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
		try {
			mLine = (SourceDataLine) AudioSystem.getLine(info);
	        mLine.open(audioFormat);
		} catch (LineUnavailableException e) {
			return false;
		}
        mLine.start();

		inputOffset = atracFileInfo.inputFileDataOffset;
		inputPosition = inputOffset;
		inputBytesPerFrame = atracFileInfo.atracBytesPerFrame;
		channels = atracFileInfo.atracChannels;

        return true;
	}

	private boolean stepSound() {
		if (inputPosition + inputBytesPerFrame >= inputLength) {
			// Loop sound
			inputPosition = inputOffset;
		}

		int result = codec.decode(inputAddr + inputPosition, inputBytesPerFrame, samplesAddr);
		if (result < 0) {
			return false;
		}

		inputPosition += inputBytesPerFrame;

		byte bytes[] = new byte[codec.getNumberOfSamples() * 2 * channels];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte) mem.read8(samplesAddr + i);
		}
		mLine.write(bytes, 0, bytes.length);

		return true;
	}
}
