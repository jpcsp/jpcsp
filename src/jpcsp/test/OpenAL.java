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
package jpcsp.test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import org.lwjgl.LWJGLException;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;

public class OpenAL {
	private static Set<Integer> freeBuffers = new HashSet<Integer>();
	private static boolean isRawFile = false;

	private static int read8(FileInputStream fis) throws IOException {
		byte[] buffer = new byte[1];
		fis.read(buffer);

		return ((int) buffer[0]) & 0xFF;
	}

	private static int read32(FileInputStream fis) throws IOException {
		int n1 = read8(fis);
		int n2 = read8(fis);
		int n3 = read8(fis);
		int n4 = read8(fis);

		return n1 | (n2 << 8) | (n3 << 16) | (n4 << 24);
	}

	private static void read(FileInputStream fis, int alSource) {
		try {
			byte[] buffer;
			int length;
			if (isRawFile) {
				int fileSize = read32(fis);
				read32(fis); // timestamp
				length = fileSize - 8;
				if (length > 0) {
					buffer = new byte[length];
					length = fis.read(buffer);
				} else {
					buffer = null;
				}
			} else {
				buffer = new byte[10240];
				length = fis.read(buffer);
			}

			if (length <= 0) {
				AL10.alSourceStop(alSource);
				fis.close();
				return;
			}

			ByteBuffer directBuffer = ByteBuffer.allocateDirect(length);
			directBuffer.put(buffer, 0, length);
			directBuffer.rewind();
			while (true) {
				int alBuffer = -1;
				if (freeBuffers.isEmpty()) {
					int availableBuffers = AL10.alGetSourcei(alSource, AL10.AL_BUFFERS_PROCESSED);
					if (availableBuffers > 0) {
						alBuffer = AL10.alSourceUnqueueBuffers(alSource);
					}
				} else {
					alBuffer = freeBuffers.iterator().next();
					freeBuffers.remove(alBuffer);
				}

				if (alBuffer >= 0) {
					AL10.alBufferData(alBuffer, AL10.AL_FORMAT_STEREO16, directBuffer, 48000);
					AL10.alSourceQueueBuffers(alSource, alBuffer);
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			AL.create();
			//FileInputStream fis = new FileInputStream("tmp/xxDISCIDxx/Atrac/Atrac-NNNNNNNN-NNNNNNNN.at3.decoded");
			FileInputStream fis = new FileInputStream("tmp/xxDISCIDxx/Mpeg-nnnn/AudioStream-0.raw");
			isRawFile = true;

			if (isRawFile) {
				read32(fis);
			}

			for (int i = 0; i < 5; i++) {
				freeBuffers.add(AL10.alGenBuffers());
				System.out.println("Error alGenBuffers " + AL10.alGetError());
			}
			int alSource = AL10.alGenSources();
			System.out.println("Error alGenSources " + AL10.alGetError());

//			AL10.alSource3f(alSource, AL10.AL_POSITION, 0.0f, 0.0f, 0.0f);
//			System.out.println("Error AL_POSITION " + AL10.alGetError());
//
//			AL10.alSource3f(alSource, AL10.AL_VELOCITY, 0.0f, 0.0f, 0.0f);
//			System.out.println("Error AL_VELOCITY " + AL10.alGetError());
//
//			AL10.alSource3f(alSource, AL10.AL_DIRECTION, 0.0f, 0.0f, 0.0f);
//			System.out.println("Error AL_DIRECTION " + AL10.alGetError());
//
//			AL10.alSourcei(alSource, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
//			System.out.println("Error AL_SOURCE_RELATIVE " + AL10.alGetError());
//
//			AL10.alSourcef(alSource, AL10.AL_PITCH, 1.0f);
//			System.out.println("Error AL_PITCH " + AL10.alGetError());
//
//			AL10.alSourcef(alSource, AL10.AL_GAIN, 1.0f);
//			System.out.println("Error AL_GAIN " + AL10.alGetError());

			AL10.alSourcei(alSource, AL10.AL_LOOPING, AL10.AL_FALSE);
			System.out.println("Error AL_LOOPING " + AL10.alGetError());

			read(fis, alSource);

			AL10.alSourcePlay(alSource);
			System.out.println("Error alSourcePlay " + AL10.alGetError());

			int previousState = -1;
			while (true) {
				int state = AL10.alGetSourcei(alSource, AL10.AL_SOURCE_STATE);
				if (state != previousState) {
					System.out.println("State " + state);
					previousState = state;
				}
				if (state != AL10.AL_PLAYING && state != AL10.AL_INITIAL) {
					break;
				}
				read(fis, alSource);
			}
			AL10.alSourceStop(alSource);
		} catch (LWJGLException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
