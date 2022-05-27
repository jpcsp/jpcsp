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

import java.util.Arrays;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.HLE.Modules;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.modules.SysMemUserForUser;
import jpcsp.HLE.modules.SysMemUserForUser.SysMemInfo;
import jpcsp.HLE.modules.sceAtrac3plus;
import jpcsp.HLE.modules.sceAtrac3plus.AtracFileInfo;
import jpcsp.hardware.Audio;
import jpcsp.media.codec.CodecFactory;
import jpcsp.media.codec.ICodec;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryWriter;
import jpcsp.util.Utilities;

public class UmdBrowserSound {
	private boolean done;
	private boolean threadExit;
	private Memory mem;
	private SysMemInfo memInfo;
	private int samplesAddr;
	private int inputAddr;
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

	public UmdBrowserSound(Memory mem, byte[] data) {
		initMemory(mem);

		if (read(data)) {
			startThread();
		} else {
			threadExit = true;
		}
	}

	public UmdBrowserSound(Memory mem, IVirtualFile vFile, int codecType, AtracFileInfo atracFileInfo) {
		initMemory(mem);

		byte[] audioData = Utilities.readCompleteFile(vFile);
		int atracBytesPerFrame = (((audioData[2] & 0x03) << 8) | ((audioData[3] & 0xFF) << 3)) + 8;
		int headerLength = 8;
		inputLength = 0;
		for (int i = 0; i < audioData.length; i += headerLength + atracBytesPerFrame) {
			write(mem, inputAddr + inputLength, audioData, i + headerLength, atracBytesPerFrame);
			inputLength += atracBytesPerFrame;
		}
		atracFileInfo.atracBytesPerFrame = atracBytesPerFrame;

		if (read(codecType, atracFileInfo)) {
			startThread();
		} else {
			threadExit = true;
		}
	}

	private void initMemory(Memory mem) {
		this.mem = mem;

		memInfo = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.KERNEL_PARTITION_ID, "UmdBrowserSound", SysMemUserForUser.PSP_SMEM_Low, 0x20000, 0);
		if (memInfo != null) {
			samplesAddr = memInfo.addr;
			inputAddr = memInfo.addr + 0x10000;
		} else {
			// PSP not yet initialized, use any memory space
			samplesAddr = MemoryMap.START_USERSPACE;
			inputAddr = MemoryMap.START_USERSPACE + 0x10000;
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

		if (memInfo != null) {
			Modules.SysMemUserForUserModule.free(memInfo);
			memInfo = null;
			samplesAddr = 0;
			inputAddr = 0;
		}
	}

	private static void write(Memory mem, int addr, byte[] data, int offset, int length) {
		length = Math.min(length, data.length - offset);
		for (int i = 0; i < length; i++) {
			mem.write8(addr + i, data[offset + i]);
		}
	}

	private void startThread() {
		Thread soundPlayThread = new SoundPlayThread();
		soundPlayThread.setDaemon(true);
		soundPlayThread.setName("Umd Browser Sound Play Thread");
		soundPlayThread.start();
	}

	private boolean read(byte[] data) {
		if (data == null || data.length == 0) {
			return false;
		}

		inputLength = data.length;
		IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(inputAddr, inputLength, 1);
		for (int i = 0; i < data.length; i++) {
			memoryWriter.writeNext(data[i] & 0xFF);
		}
		memoryWriter.flush();

		AtracFileInfo atracFileInfo = new AtracFileInfo();
		int codecType = sceAtrac3plus.analyzeRiffFile(mem, inputAddr, inputLength, atracFileInfo);
		if (codecType < 0) {
			return false;
		}

		boolean result = read(codecType, atracFileInfo);

		return result;
	}

	private boolean read(int codecType, AtracFileInfo atracFileInfo) {
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

		int result = codec.decode(mem, inputAddr + inputPosition, inputBytesPerFrame, mem, samplesAddr);
		if (result < 0) {
			return false;
		}

		inputPosition += inputBytesPerFrame;

		byte bytes[] = new byte[codec.getNumberOfSamples() * 2 * channels];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte) mem.read8(samplesAddr + i);
		}

		if (Audio.isMuted()) {
			Arrays.fill(bytes, (byte) 0);
		}

		mLine.write(bytes, 0, bytes.length);

		return true;
	}
}
