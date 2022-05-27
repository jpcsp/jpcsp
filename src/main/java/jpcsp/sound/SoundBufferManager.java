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

import static jpcsp.sound.SoundChannel.alCheckError;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import jpcsp.HLE.modules.sceAudio;

import org.apache.log4j.Logger;
import org.lwjgl.openal.AL10;

public class SoundBufferManager {
	private static Logger log = sceAudio.log;
	private static SoundBufferManager instance;
	private Stack<Integer> freeBuffers;
	private List<ByteBuffer> freeDirectBuffers;

	public static SoundBufferManager getInstance() {
		if (instance == null) {
			instance = new SoundBufferManager();
		}

		return instance;
	}

	private SoundBufferManager() {
		freeBuffers = new Stack<Integer>();
		freeDirectBuffers = new LinkedList<ByteBuffer>();
	}

	public int getBuffer() {
		synchronized (freeBuffers) {
			if (freeBuffers.isEmpty()) {
				int alBuffer = AL10.alGenBuffers();
				alCheckError("alGenBuffers");
				freeBuffers.push(alBuffer);
			}

			return freeBuffers.pop();
		}
	}

	public void checkFreeBuffers(int alSource) {
        while (true) {
        	int alBuffer = checkFreeBuffer(alSource);
        	if (alBuffer < 0) {
        		break;
        	}
        }
	}

	public int checkFreeBuffer(int alSource) {
    	int processedBuffers = AL10.alGetSourcei(alSource, AL10.AL_BUFFERS_PROCESSED);
		alCheckError("alGetSourcei AL_BUFFERS_PROCESSED");
    	if (processedBuffers <= 0) {
    		return -1;
    	}
		int alBuffer = AL10.alSourceUnqueueBuffers(alSource);
		alCheckError("alSourceUnqueueBuffers");
		if (log.isTraceEnabled()) {
			log.trace(String.format("free buffer %d", alBuffer));
		}

		synchronized (freeBuffers) {
    		freeBuffers.push(alBuffer);
		}

		return alBuffer;
	}

	public ByteBuffer getDirectBuffer(int size) {
		synchronized (freeDirectBuffers) {
			for (int i = 0; i < freeDirectBuffers.size(); i++) {
				ByteBuffer directBuffer = freeDirectBuffers.get(i);
				if (directBuffer.capacity() >= size) {
					freeDirectBuffers.remove(i);
					return directBuffer;
				}
			}
		}

		ByteBuffer directBuffer = ByteBuffer.allocateDirect(size);
		return directBuffer;
	}

	public void releaseDirectBuffer(ByteBuffer directBuffer) {
		synchronized (freeDirectBuffers) {
			if (freeDirectBuffers.isEmpty()) {
				freeDirectBuffers.add(directBuffer);
			} else {
				freeDirectBuffers.add(0, directBuffer);
			}
		}
	}
}
